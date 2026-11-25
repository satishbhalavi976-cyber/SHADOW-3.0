package com.project.shadow.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.shadow.R
import com.project.shadow.ShadowApplication
import com.project.shadow.data.ChatMessage
import com.project.shadow.data.MessageRole
import com.project.shadow.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

/**
 * SHADOW 3.0 - Main Activity
 * Features:
 *  - Black & White UI with animated mic button
 *  - Voice recognition (SpeechRecognizer)
 *  - Text-to-speech responses
 *  - Chat history display
 *  - Camera/Vision mode
 *  - Suggestion chips on empty state
 *  - Theme toggle (Dark/Light)
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ShadowViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isTTSReady = false

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput()
        else Toast.makeText(this, getString(R.string.error_mic_failed), Toast.LENGTH_SHORT).show()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, getString(R.string.permission_camera_title), Toast.LENGTH_SHORT).show()
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedText = result.data?.getStringExtra("scanned_text")?.trim()
            if (!scannedText.isNullOrBlank()) {
                viewModel.sendMessage("Scanned text:\n$scannedText")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if first launch
        val prefs = ShadowApplication.instance.prefs
        if (prefs.getBoolean(ShadowApplication.PREF_FIRST_LAUNCH, true)) {
            prefs.edit().putBoolean(ShadowApplication.PREF_FIRST_LAUNCH, false).apply()
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        setupUI()
        setupVoice()
        observeViewModel()
        startPulseAnimation()
    }

    private fun setupUI() {
        // RecyclerView
        chatAdapter = ChatAdapter(
            onCopy = { text -> copyToClipboard(text) },
            onSpeak = { text -> speakText(text) }
        )
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // Navigation buttons
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.btnNewChat.setOnClickListener {
            viewModel.startNewChat()
            showEmptyState()
        }

        // Camera button
        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Text input → switch mic to send icon
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.btnVoice.setImageResource(
                    if (hasText) R.drawable.ic_send else R.drawable.ic_mic
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Send on IME action
        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendTextMessage()
            true
        }

        // Send button (when text is present)
        binding.btnVoice.setOnClickListener {
            val text = binding.etMessage.text?.toString()?.trim()
            if (!text.isNullOrBlank()) sendTextMessage()
            else {
                if (isListening) stopListening()
                else checkMicAndListen()
            }
        }

        // Suggestion chip clicks
        binding.chipGroupSuggestions.setOnCheckedStateChangeListener { group, checkedIds ->
            // handled per chip below
        }
        setupSuggestionChips()
    }

    private fun setupSuggestionChips() {
        val chips = listOf(
            binding.chipGroupSuggestions.getChildAt(0),
            binding.chipGroupSuggestions.getChildAt(1),
            binding.chipGroupSuggestions.getChildAt(2),
            binding.chipGroupSuggestions.getChildAt(3)
        )
        val prompts = listOf(
            "Set a timer for 10 minutes",
            "What's the weather like today?",
            "Open camera to scan text",
            "Help me make a phone call"
        )
        chips.forEachIndexed { i, chip ->
            chip?.setOnClickListener {
                viewModel.sendMessage(prompts[i])
            }
        }
    }

    private fun setupVoice() {
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                showListeningUI()
            }
            override fun onBeginningOfSpeech() { binding.waveformView.startAnimation() }
            override fun onRmsChanged(rmsdB: Float) { binding.waveformView.updateAmplitude(rmsdB) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { binding.waveformView.stopAnimation() }
            override fun onError(error: Int) {
                isListening = false
                hideListeningUI()
                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                    Toast.makeText(this@MainActivity, getString(R.string.error_mic_failed), Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                hideListeningUI()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                viewModel.sendMessage(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                binding.tvListeningStatus.text = partial?.firstOrNull() ?: getString(R.string.listening)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                chatAdapter.submitList(messages.toList())
                if (messages.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    binding.rvChat.postDelayed({
                        binding.rvChat.smoothScrollToPosition(messages.size - 1)
                    }, 100)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                // Show loading indicator in last message
            }
        }

        lifecycleScope.launch {
            viewModel.speakText.collectLatest { text ->
                if (text.isNotBlank()) speakText(text)
            }
        }
    }

    private fun checkMicAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        hideListeningUI()
    }

    private fun sendTextMessage() {
        val text = binding.etMessage.text?.toString()?.trim() ?: return
        if (text.isBlank()) return
        binding.etMessage.text?.clear()
        binding.btnVoice.setImageResource(R.drawable.ic_mic)
        viewModel.sendMessage(text)
    }

    private fun openCamera() {
        captureLauncher.launch(Intent(this, CaptureActivity::class.java))
    }

    // ======================== UI STATES ========================

    private fun showEmptyState() {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.rvChat.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.layoutEmpty.visibility = View.GONE
        binding.rvChat.visibility = View.VISIBLE
    }

    private fun showListeningUI() {
        binding.layoutListening.visibility = View.VISIBLE
        binding.tvListeningStatus.text = getString(R.string.listening)
        binding.btnVoice.setImageResource(R.drawable.ic_mic_active)
        // Pulse the button
        binding.btnVoice.animate().scaleX(1.15f).scaleY(1.15f).setDuration(300).start()
    }

    private fun hideListeningUI() {
        binding.layoutListening.visibility = View.GONE
        binding.btnVoice.setImageResource(R.drawable.ic_mic)
        binding.btnVoice.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun startPulseAnimation() {
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        binding.vPulseOuter.startAnimation(pulse)
        val pulseMid = AnimationUtils.loadAnimation(this, R.anim.pulse_mid)
        binding.vPulseMid.startAnimation(pulseMid)
    }

    // ======================== TTS ========================

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTTSReady = true
            tts.language = Locale.getDefault()
            val speed = ShadowApplication.instance.prefs.getFloat(ShadowApplication.PREF_TTS_SPEED, 1.0f)
            tts.setSpeechRate(speed)
        }
    }

    private fun speakText(text: String) {
        if (isTTSReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SHADOW", text))
        Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        speechRecognizer?.destroy()
    }

    override fun onPause() {
        super.onPause()
        if (isListening) stopListening()
        tts.stop()
    }
}
