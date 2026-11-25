package com.project.shadow.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.project.shadow.databinding.ActivityCaptureBinding

/**
 * SHADOW 3.0 - CaptureActivity
 * Camera preview + ML Kit text recognition (OCR)
 * Captured text is sent back to main chat
 */
class CaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaptureBinding
    private var imageCapture: ImageCapture? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnCapture.setOnClickListener { captureAndRecognize() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun captureAndRecognize() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val mediaImage = image.image
                    if (mediaImage == null) {
                        image.close()
                        return
                    }
                    val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text
                            if (text.isNotBlank()) {
                                // Return text to MainActivity
                                setResult(RESULT_OK, android.content.Intent().apply {
                                    putExtra("scanned_text", text)
                                })
                                finish()
                            } else {
                                Toast.makeText(this@CaptureActivity, "No text found in image", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@CaptureActivity, "Recognition failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { image.close() }
                }
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CaptureActivity, "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.close()
    }
}
