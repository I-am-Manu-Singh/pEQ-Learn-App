package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import com.creativeblisslabs.peqlearn.app.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Extension for AppViewModel to handle Metadata, Lyrics, and Subtitles logic.
 * This helps reduce the main AppViewModel file size for industrial maintenance.
 */


private fun parseTimeToMs(time: String): Long {
    val parts = time.split(':', '.').map { it.toLongOrNull() ?: 0L }
    return when (parts.size) {
        4 -> (parts[0] * 3600000) + (parts[1] * 60000) + (parts[2] * 1000) + parts[3]
        3 -> (parts[0] * 60000) + (parts[1] * 1000) + parts[2]
        2 -> (parts[0] * 60000) + (parts[1] * 1000)
        else -> 0L
    }
}

fun AppViewModel.readFileContent(path: String): String? {
    return scanner?.readFileContent(path)
}
