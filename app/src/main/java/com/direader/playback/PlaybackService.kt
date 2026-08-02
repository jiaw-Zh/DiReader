package com.direader.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service to keep playback running when app is in the background.
 */
class PlaybackService : Service() {
    
    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }
    
    private val binder = LocalBinder()
    
    companion object {
        const val CHANNEL_ID = "direader_playback"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    /**
     * Start the service in the foreground and show initial notification.
     */
    fun startForegroundPlayback(bookTitle: String) {
        startForeground(NOTIFICATION_ID, createNotification(bookTitle, "", false))
    }

    /**
     * Update the notification with the current chapter and playback state.
     */
    fun updateNotification(bookTitle: String, chapterTitle: String, isPlaying: Boolean) {
        val notification = createNotification(bookTitle, chapterTitle, isPlaying)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(bookTitle: String, chapterTitle: String, isPlaying: Boolean): Notification {
        // Intent to launch the main activity when clicking the notification
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(bookTitle.ifEmpty { "DiReader" })
            .setContentText(chapterTitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Playback"
            val descriptionText = "Reading playback controls"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
