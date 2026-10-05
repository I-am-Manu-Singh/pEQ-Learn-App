package com.creativeblisslabs.peqlearn.app

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import com.creativeblisslabs.peqlearn.app.data.initPersistenceManager

class pEQApplication : Application() {
    companion object {
        lateinit var instance: pEQApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initPersistenceManager(this)
        try {
            FFmpeg.getInstance().init(this)
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= 60) {
            try { cacheDir.deleteRecursively() } catch (_: Exception) {}
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try { cacheDir.deleteRecursively() } catch (_: Exception) {}
    }
}
