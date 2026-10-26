package com.t3codes.mobile

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Shared notification entry points. Completion and input alerts deliberately
 * use different channels so a user can mute run results without muting
 * questions that block an agent.
 */
object T3Notifications {
    const val EXTRA_THREAD_ID = "com.t3codes.mobile.extra.THREAD_ID"

    const val CHANNEL_SERVICE = "t3_background_sync"
    const val CHANNEL_COMPLETION = "t3_run_completions"
    const val CHANNEL_INPUT = "t3_input_requests"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                "Background sync",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps T3 connected while the app is closed"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COMPLETION,
                "Run completions",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Tells you when an agent finishes a run"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INPUT,
                "Questions and approvals",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Tells you when an agent is waiting for you"
            },
        )
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun contentIntent(context: Context, threadId: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!threadId.isNullOrBlank()) putExtra(EXTRA_THREAD_ID, threadId)
        }
        return PendingIntent.getActivity(
            context,
            threadId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Whether a visible activity is in the foreground. */
object AppVisibility {
    @Volatile
    var foreground: Boolean = false
}
