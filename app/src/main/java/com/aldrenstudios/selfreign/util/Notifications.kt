package com.aldrenstudios.selfreign.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Lightweight notification helpers. Channels must exist before any notification
 * can be posted on Android 8.0+ (API 26).
 */
object Notifications {

    const val CHANNEL_ID = "encouragement"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val encouragement = NotificationChannel(
                CHANNEL_ID,
                "Encouragement",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Gentle reminders to stay on track" }
            manager.createNotificationChannel(encouragement)
        }
    }
}
