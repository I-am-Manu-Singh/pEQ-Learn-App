package com.creativeblisslabs.peqlearn.app.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LrcLibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

@Serializable
data class SyncedLyricLine(
    val timestampMs: Long,
    val text: String
)

object LrcLibClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLyrics(
        httpClient: HttpClient,
        trackName: String,
        artistName: String? = null,
        albumName: String? = null,
        durationSeconds: Int? = null
    ): LrcLibResponse? = withContext(Dispatchers.IO) {
        val cleanTrack = trackName
            .replace(Regex("(?i)\\b(official|audio|video|lyrics|ft\\..*|feat\\..*|remix|hd|4k)\\b"), "")
            .replace(Regex("[\\[\\(].*?[\\]\\)]"), "")
            .trim()

        if (cleanTrack.isEmpty()) return@withContext null

        // 1. Try exact match /api/get
        try {
            val response: HttpResponse = httpClient.get("https://lrclib.net/api/get") {
                parameter("track_name", cleanTrack)
                artistName?.takeIf { it.isNotBlank() }?.let { parameter("artist_name", it) }
                albumName?.takeIf { it.isNotBlank() }?.let { parameter("album_name", it) }
                durationSeconds?.takeIf { it > 0 }?.let { parameter("duration", it) }
                header("User-Agent", "pEQLearnMusicApp/2.0 (https://github.com/I-am-Manu-Singh/pEQ-Learn-App)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                return@withContext json.decodeFromString<LrcLibResponse>(body)
            }
        } catch (_: Exception) {}

        // 2. Try search /api/search fallback
        try {
            val query = listOfNotNull(cleanTrack, artistName?.takeIf { it.isNotBlank() }).joinToString(" ")
            val response: HttpResponse = httpClient.get("https://lrclib.net/api/search") {
                parameter("q", query)
                header("User-Agent", "pEQLearnMusicApp/2.0 (https://github.com/I-am-Manu-Singh/pEQ-Learn-App)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val list = json.decodeFromString<List<LrcLibResponse>>(body)
                val match = list.firstOrNull { !it.syncedLyrics.isNullOrBlank() } ?: list.firstOrNull()
                if (match != null) return@withContext match
            }
        } catch (_: Exception) {}

        null
    }

    fun parseSyncedLyrics(rawLrc: String): List<SyncedLyricLine> {
        val lines = mutableListOf<SyncedLyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\](.*)")
        for (line in rawLrc.lines()) {
            val match = regex.find(line.trim()) ?: continue
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fracStr = match.groupValues[3]
            val millis = when (fracStr.length) {
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                3 -> fracStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val totalMs = (minutes * 60000L) + (seconds * 1000L) + millis
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                lines.add(SyncedLyricLine(totalMs, text))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}

