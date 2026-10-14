package com.creativeblisslabs.peqlearn.app.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.ref.WeakReference

private var activityReference: WeakReference<Activity>? = null

fun initSystemWindowManager(activity: Activity) {
    activityReference = WeakReference(activity)
}

private fun getActivity(): Activity? {

    val activity = activityReference?.get()

    return if (
        activity != null &&
        !activity.isFinishing &&
        !activity.isDestroyed
    ) {
        activity
    } else {
        null
    }
}

actual fun setScreenOrientation(orientation: Int) {

    val activity = getActivity() ?: return

    val androidOrientation =
        when (orientation) {

            pEQOrientation.LANDSCAPE -> {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            pEQOrientation.PORTRAIT -> {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            pEQOrientation.USER -> {
                ActivityInfo.SCREEN_ORIENTATION_USER
            }
            
            pEQOrientation.LOCKED -> {
                ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }

            else -> {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

    runCatching {

        if (activity.requestedOrientation != androidOrientation) {
            activity.requestedOrientation = androidOrientation
        }

    }.onFailure {
        println(
            "pEQLearn: Orientation change failed -> ${it.message}"
        )
    }
}

actual fun setSystemBarsVisibility(visible: Boolean) {

    val activity = getActivity() ?: return

    runCatching {

        val window = activity.window

        val controller =
            WindowInsetsControllerCompat(
                window,
                window.decorView
            )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (visible) {

            controller.show(
                WindowInsetsCompat.Type.systemBars()
            )

        } else {

            controller.hide(
                WindowInsetsCompat.Type.systemBars()
            )
        }

    }.onFailure {
        println(
            "pEQLearn: System bars visibility update failed -> ${it.message}"
        )
    }
}
actual fun setBrightness(brightness: Float) {
    val activity = getActivity() ?: return
    runCatching {
        val window = activity.window
        val layoutParams = window.attributes
        if (brightness < 0f) {
            layoutParams.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            layoutParams.screenBrightness = brightness.coerceIn(0f, 1f)
        }
        window.attributes = layoutParams
    }.onFailure {
        println("pEQLearn: Brightness update failed -> ${it.message}")
    }
}

fun releaseBrightness() {
    setBrightness(-1f)
}

actual fun setSystemVolume(volume: Float) {
    val activity = getActivity() ?: return
    runCatching {
        val audioManager = activity.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val targetVolume = (volume * maxVolume).toInt()
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVolume, 0)
    }.onFailure {
        println("pEQLearn: Volume update failed -> ${it.message}")
    }
}

actual fun getSystemVolume(): Float {
    val activity = getActivity() ?: return 0.5f
    return runCatching {
        val audioManager = activity.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        currentVolume.toFloat() / maxVolume.toFloat()
    }.getOrDefault(0.5f)
}
