package com.creativeblisslabs.peqlearn.app

import java.io.File

actual fun clock_time_nanos(): Long = System.nanoTime()

actual fun deleteLocalFile(path: String): Boolean {
    return try {
        File(path).delete()
    } catch (e: Exception) {
        false
    }
}
