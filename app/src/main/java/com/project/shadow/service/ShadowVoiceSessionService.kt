package com.project.shadow.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.project.shadow.ui.MainActivity

/**
 * SHADOW 3.0 - Voice Interaction Session Service
 * Launches SHADOW assistant UI when triggered via system assist gesture.
 */
class ShadowVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return ShadowVoiceSession(this)
    }
}

class ShadowVoiceSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "com.project.shadow.ACTION_VOICE_TRIGGER"
        }
        context.startActivity(intent)
        hide()
    }
}
