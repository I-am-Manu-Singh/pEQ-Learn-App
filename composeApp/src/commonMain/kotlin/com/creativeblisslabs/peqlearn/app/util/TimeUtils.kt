package com.creativeblisslabs.peqlearn.app.util

/**
 * pEQ TIME UTILS: TEMPORAL PRECISION HUB
 * Centralized formatting for cinematic and audiophile timestamps.
 */
object TimeUtils {
    fun formatMs(ms: Long): String {
        val sec = (ms / 1000) % 60
        val min = (ms / (1000 * 60)) % 60
        val hr = (ms / (1000 * 60 * 60))
        return if (hr > 0) {
            val h = hr.toString().padStart(2, '0')
            val m = min.toString().padStart(2, '0')
            val s = sec.toString().padStart(2, '0')
            "$h:$m:$s"
        } else {
            val m = min.toString().padStart(2, '0')
            val s = sec.toString().padStart(2, '0')
            "$m:$s"
        }
    }
}
