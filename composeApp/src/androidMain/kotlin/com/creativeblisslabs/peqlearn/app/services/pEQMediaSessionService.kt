package com.creativeblisslabs.peqlearn.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.creativeblisslabs.peqlearn.app.audio.AudioEngineProvider
import com.creativeblisslabs.peqlearn.app.data.getPersistenceManager
import com.creativeblisslabs.peqlearn.app.data.initPersistenceManager
import kotlinx.coroutines.*

class pEQMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        const val ACTION_PLAY_PAUSE = "com.creativeblisslabs.peqlearn.app.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.creativeblisslabs.peqlearn.app.ACTION_NEXT"
        const val ACTION_PREV = "com.creativeblisslabs.peqlearn.app.ACTION_PREV"
        private const val CHANNEL_ID = "peq_media_session_channel"
    }

    override fun onCreate() {
        super.onCreate()
        initPersistenceManager(applicationContext)
        player = AudioEngineProvider.getExoPlayer(this)
        createNotificationChannel()
        buildMediaSession()
    }

    @OptIn(UnstableApi::class)
    private fun buildMediaSession() {
        mediaSession = MediaSession.Builder(this, object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(COMMAND_SEEK_TO_NEXT)
                    .add(COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }
        }).build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            ACTION_NEXT -> player.seekToNext()
            ACTION_PREV -> player.seekToPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "pEQ Media Session", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
