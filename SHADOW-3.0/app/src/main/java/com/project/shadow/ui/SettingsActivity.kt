package com.project.shadow.ui

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.project.shadow.R
import com.project.shadow.ShadowApplication
import com.project.shadow.data.ShadowRepository
import com.project.shadow.databinding.ActivitySettingsBinding

/**
 * SHADOW 3.0 Settings Activity
 * Theme toggle (Black/White), voice speed, wake word, etc.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs get() = ShadowApplication.instance.prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSettings()
    }

    private fun setupSettings() {
        // Theme display
        val currentTheme = prefs.getString(ShadowApplication.PREF_THEME, ShadowApplication.THEME_DARK)
        binding.tvCurrentTheme.text = when (currentTheme) {
            ShadowApplication.THEME_DARK -> getString(R.string.settings_theme_dark)
            ShadowApplication.THEME_LIGHT -> getString(R.string.settings_theme_light)
            else -> getString(R.string.settings_theme_system)
        }

        // Theme selection dialog
        binding.rowTheme.setOnClickListener {
            val options = arrayOf(
                getString(R.string.settings_theme_dark),
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_system)
            )
            val values = arrayOf(
                ShadowApplication.THEME_DARK,
                ShadowApplication.THEME_LIGHT,
                ShadowApplication.THEME_SYSTEM
            )
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_theme))
                .setItems(options) { _, which ->
                    ShadowApplication.instance.setTheme(values[which])
                    binding.tvCurrentTheme.text = options[which]
                    recreate()
                }
                .show()
        }

        // Wake word switch
        val wakeEnabled = prefs.getBoolean(ShadowApplication.PREF_WAKE_WORD, true)
        binding.switchWakeWord.isChecked = wakeEnabled
        binding.switchWakeWord.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(ShadowApplication.PREF_WAKE_WORD, checked).apply()
        }

        // TTS Speed seekbar (0.5x to 2.5x, stored as float)
        val speed = prefs.getFloat(ShadowApplication.PREF_TTS_SPEED, 1.0f)
        val progress = ((speed - 0.5f) / 0.1f).toInt().coerceIn(0, 20)
        binding.seekBarSpeed.progress = progress
        binding.tvSpeedValue.text = "${speed}x"
        binding.seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val newSpeed = 0.5f + p * 0.1f
                binding.tvSpeedValue.text = String.format("%.1fx", newSpeed)
                prefs.edit().putFloat(ShadowApplication.PREF_TTS_SPEED, newSpeed).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Set as default assistant
        binding.rowSetDefault.setOnClickListener {
            val intents = listOf(
                "android.settings.MANAGE_DEFAULT_APPS_SETTINGS",
                "android.settings.VOICE_INPUT_SETTINGS",
                "android.settings.SETTINGS"
            )
            var launched = false
            for (action in intents) {
                try {
                    startActivity(android.content.Intent(action))
                    launched = true
                    break
                } catch (e: Exception) {
                    // try next
                }
            }
            if (!launched) {
                Toast.makeText(this, "Couldn't open Settings on this device.", Toast.LENGTH_LONG).show()
            }
        }

        // Clear history
        binding.rowClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("This will delete all conversation history. This cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    ShadowRepository.clearAllHistory()
                    Toast.makeText(this, getString(R.string.chat_cleared), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
