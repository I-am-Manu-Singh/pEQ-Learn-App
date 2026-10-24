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

actual fun shareTextFile(fileName: String, content: String): Boolean {
    return try {
        val userHome = System.getProperty("user.home") ?: "."
        val downloads = File(userHome, "Downloads")
        val targetDir = if (downloads.exists()) downloads else File(userHome)
        val file = File(targetDir, fileName)
        file.writeText(content)
        true
    } catch (e: Exception) {
        false
    }
}
