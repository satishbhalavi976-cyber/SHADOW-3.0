package com.project.shadow.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * SHADOW 3.0 - Boot Receiver
 * Restarts SHADOW notification service when device boots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(
                Intent(context, ShadowNotificationService::class.java)
            )
        }
    }
}
