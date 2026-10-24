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
        val context = pEQApplication.instance
        val dumpDir = File(context.cacheDir, "dumps")
        if (!dumpDir.exists()) {
            dumpDir.mkdirs()
        }
        val safeFileName = if (fileName.endsWith(".txt")) fileName else "$fileName.txt"
        val file = File(dumpDir, safeFileName)
        file.writeText(content)

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, safeFileName)
            putExtra(android.content.Intent.EXTRA_TEXT, "Score Notation Parser Dump ($safeFileName)")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = android.content.Intent.createChooser(intent, "Share Score Parser Dump").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
