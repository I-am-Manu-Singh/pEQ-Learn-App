package com.creativeblisslabs.peqlearn.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.creativeblisslabs.peqlearn.MainActivity

class pEQVideoExportService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val CHANNEL_ID = "peq_video_export_channel"
        private const val NOTIFICATION_ID = 8801
        private const val ACTION_UPDATE_PROGRESS = "com.creativeblisslabs.peqlearn.ACTION_UPDATE_VIDEO_PROGRESS"
        private const val ACTION_STOP_EXPORT = "com.creativeblisslabs.peqlearn.ACTION_STOP_VIDEO_EXPORT"

        private const val EXTRA_PROGRESS = "extra_progress"
        private const val EXTRA_FRAME_TEXT = "extra_frame_text"
        private const val EXTRA_ETA_TEXT = "extra_eta_text"

        fun startExportService(context: Context) {
            runCatching {
                val intent = Intent(context, pEQVideoExportService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun updateProgress(context: Context, progress: Float, frameText: String, etaText: String) {
            runCatching {
                val intent = Intent(context, pEQVideoExportService::class.java).apply {
                    action = ACTION_UPDATE_PROGRESS
                    putExtra(EXTRA_PROGRESS, progress)
                    putExtra(EXTRA_FRAME_TEXT, frameText)
                    putExtra(EXTRA_ETA_TEXT, etaText)
                }
                context.startService(intent)
            }
        }

        fun stopExportService(context: Context) {
            runCatching {
                val intent = Intent(context, pEQVideoExportService::class.java).apply {
                    action = ACTION_STOP_EXPORT
                }
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        startInForeground(0f, "Initializing Export...", "Preparing Encoders")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_PROGRESS -> {
                val progress = intent.getFloatExtra(EXTRA_PROGRESS, 0f)
                val frameText = intent.getStringExtra(EXTRA_FRAME_TEXT) ?: ""
                val etaText = intent.getStringExtra(EXTRA_ETA_TEXT) ?: ""
                updateNotification(progress, frameText, etaText)
            }
            ACTION_STOP_EXPORT -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        runCatching {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pEQLearn:VideoExportWakeLock").apply {
                acquire(30 * 60 * 1000L) // 30 minutes max timeout
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        }
    }

    private fun startInForeground(progress: Float, frameText: String, etaText: String) {
        val notification = buildNotification(progress, frameText, etaText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            if (type != 0) {
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(progress: Float, frameText: String, etaText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(progress, frameText, etaText))
    }

    private fun buildNotification(progress: Float, frameText: String, etaText: String): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressPercent = (progress.coerceIn(0f, 1f) * 100).toInt()
        val contentTitle = "Rendering Video ($progressPercent%)"
        val contentText = if (etaText.isNotBlank()) "$frameText • $etaText" else "Export in progress..."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent, false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "pEQ Video Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing video export rendering progress"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }
}
