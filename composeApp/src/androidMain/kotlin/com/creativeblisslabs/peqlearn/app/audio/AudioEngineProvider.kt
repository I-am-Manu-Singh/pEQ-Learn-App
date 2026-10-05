package com.creativeblisslabs.peqlearn.app.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs
import androidx.media3.exoplayer.DefaultLoadControl
/**
 * pEQ ENGINE PROVIDER
 *
 * Shared singleton player across:
 * - UI
 * - MediaSessionService
 * - Widgets
 * - Notifications
 *
 * Prevents:
 * - multiple ExoPlayer instances
 * - duplicated audio sessions
 * - random volume jitter
 * - player recreation on Activity restart
 *
 * Humanity keeps reinventing the same bugs in new architectures.
 */
object AudioEngineProvider {

    @Volatile
    private var exoPlayer: ExoPlayer? = null

    @Volatile
    private var lastVolume = -1f

    // =========================
    // GET OR CREATE
    // =========================

    @OptIn(UnstableApi::class)
    fun getExoPlayer(
        context: Context
    ): ExoPlayer {

        exoPlayer?.let {
            return it
        }

        return synchronized(this) {

            exoPlayer?.let {
                return@synchronized it
            }

            ExoPlayer.Builder(
                context.applicationContext
            )

                .setLoadControl(

                    DefaultLoadControl.Builder()

                        .setBufferDurationsMs(

                            3000,   // minBufferMs
                            15000,  // maxBufferMs
                            1000,   // bufferForPlaybackMs
                            2000    // bufferForPlaybackAfterRebufferMs
                        )

                        .build()
                )

                .build()

                .also { player ->

                    player.volume = 1f

                    exoPlayer = player
                }
        }
    }

    // =========================
    // SAFE VOLUME
    // =========================

    fun setVolume(volume: Float) {

        val player =
            exoPlayer ?: return

        val safeVolume =
            volume.coerceIn(0f, 1f)

        // prevent spam/jitter
        if (
            abs(
                safeVolume - lastVolume
            ) < 0.01f
        ) {
            return
        }

        lastVolume = safeVolume

        try {

            player.volume = safeVolume

        } catch (e: Exception) {

            println(
                "pEQControl: [ENGINE_PROVIDER] Volume Failed: ${e.message}"
            )
        }
    }

    // =========================
    // REPLACE PLAYER
    // =========================

    fun replacePlayer(
        player: ExoPlayer
    ) {

        synchronized(this) {

            if (exoPlayer === player) {
                return
            }

            runCatching {
                exoPlayer?.release()
            }

            exoPlayer = player
        }
    }

    /**
     * Compatibility alias.
     * Because deleting old APIs apparently causes emotional damage.
     */
    fun setExoPlayer(
        player: ExoPlayer
    ) {
        replacePlayer(player)
    }

    // =========================
    // PEEK
    // =========================

    fun peekPlayer(): ExoPlayer? {
        return exoPlayer
    }

    // =========================
    // RELEASE
    // =========================

    fun release() {

        synchronized(this) {

            runCatching {
                exoPlayer?.release()
            }

            exoPlayer = null

            lastVolume = -1f
        }
    }
}