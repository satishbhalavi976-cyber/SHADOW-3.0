package com.project.shadow.service

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * SHADOW 3.0 - Recognition Service
 * System recognition service binding required by interaction_service metadata.
 */
class ShadowRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        try {
            listener.readyForSpeech(Bundle())
            listener.beginningOfSpeech()
        } catch (_: Exception) {
            try {
                listener.error(SpeechRecognizer.ERROR_CLIENT)
            } catch (_: Exception) {
            }
        }
    }

    override fun onStopListening(listener: Callback) {
        try {
            listener.endOfSpeech()
        } catch (_: Exception) {
        }
    }

    override fun onCancel(listener: Callback) {
        // No active recording session to cancel
    }
}
