package com.creativeblisslabs.peqlearn.app.data.lyrics

import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.creativeblisslabs.peqlearn.app.data.SyncedLine
import com.creativeblisslabs.peqlearn.app.network.createHttpClient
import kotlin.math.abs

class LyricsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client =
        createHttpClient().config {

            install(HttpTimeout) {

                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            install(DefaultRequest) {

                header(
                    io.ktor.http.HttpHeaders.UserAgent,
                    "pEQ-Workstation/1.0"
                )
            }
        }

    /*
     * MEMORY CACHE
     */
    private val cache =
        mutableMapOf<String, List<SyncedLine>>()

    suspend fun fetchLyrics(
        artist: String?,
        title: String,
        durationMs: Long? = null
    ): List<SyncedLine>? {

        println(
            "pEQControl: [LYRICS_REPO] fetchLyrics entry: artist=$artist, title=$title"
        )

        /*
         * SKIP VIDEO FILES
         */
        val lowerTitle =
            title.lowercase()

        if (
            lowerTitle.endsWith(".mp4") ||
            lowerTitle.endsWith(".mkv") ||
            lowerTitle.endsWith(".avi") ||
            lowerTitle.endsWith(".mov") ||
            lowerTitle.endsWith(".webm") ||
            lowerTitle.endsWith(".3gp")
        ) {

            println(
                "pEQControl: [LYRICS_REPO] Skipping video file"
            )

            return null
        }

        /*
         * CLEAN TITLE
         */
        val cleanedTitle =
            title
                .substringBeforeLast(".")
                .replace(
                    Regex(
                        "(?i)\\b(official|video|audio|lyrics|instrumental|cover|guitar cover|live|remix|edit|4k|hd|hq)\\b"
                    ),
                    ""
                )
                .replace(
                    Regex("\\(.*?\\)|\\[.*?\\]"),
                    ""
                )
                .replace("_", " ")
                .replace("-", " ")
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        if (cleanedTitle.isBlank()) {

            println(
                "pEQControl: [LYRICS_REPO] Empty cleaned title"
            )

            return null
        }

        val normalizedArtist =
            normalizeArtist(artist ?: "")

        val normalizedTitle =
            normalizeTitle(cleanedTitle)

        val cacheKey =
            "$normalizedArtist-$normalizedTitle"

        /*
         * CACHE HIT
         */
        cache[cacheKey]?.let {

            println(
                "pEQControl: [LYRICS_REPO] Cache hit"
            )

            return it
        }

        val artistIsGeneric =
            normalizedArtist.isBlank() ||
            normalizedArtist.contains(
                "unknown",
                ignoreCase = true
            )

        val titleIsGeneric =
            normalizedTitle.isBlank() ||
            normalizedTitle.contains(
                "unknown",
                ignoreCase = true
            )

        if (titleIsGeneric) {

            println(
                "pEQControl: [LYRICS_REPO] Invalid normalized title"
            )

            return null
        }

        val query =
            if (artistIsGeneric) {

                normalizedTitle

            } else {

                "$normalizedArtist $normalizedTitle"
            }.trim()

        if (query.isBlank()) {
            return null
        }

        val url =
            "https://lrclib.net/api/search?q=${
                query.encodeURLPathPart()
            }"

        println(
            "pEQControl: [LYRICS_REPO] Searching: $url"
        )

        return try {

            val response: String =
                client.get(url).body()

            val results =
                json.decodeFromString<List<LrclibSearchResult>>(
                    response
                )

            println(
                "pEQControl: [LYRICS_REPO] Found ${results.size} matches"
            )

            if (results.isEmpty()) {
                return null
            }

            val best =
                results.minByOrNull {

                    calculateScore(
                        localArtist = normalizedArtist,
                        localTitle = normalizedTitle,
                        apiArtist = normalizeArtist(it.artistName),
                        apiTitle = normalizeTitle(it.trackName),
                        localDuration = durationMs,
                        apiDuration = it.duration
                            ?.toLong()
                            ?.times(1000L)
                    )
                }

            val lyrics =
                best?.syncedLyrics
                    ?: best?.plainLyrics

            if (lyrics.isNullOrBlank()) {
                return null
            }

            val parsed =
                if (best?.syncedLyrics != null) {

                    parseLrc(lyrics)

                } else {

                    lyrics.lines()
                        .filter {
                            it.isNotBlank()
                        }
                        .map {

                            SyncedLine(
                                timestampMs = 0L,
                                text = it
                            )
                        }
                }

            /*
             * SAVE CACHE
             */
            cache[cacheKey] = parsed

            parsed

        } catch (e: Exception) {

    if (e.message?.contains("cancelled", true) != true) {

        println(
            "pEQControl: [LYRICS_REPO] Error: ${e.message}"
        )
    }

    null
}
    }

    private fun calculateScore(
        localArtist: String,
        localTitle: String,
        apiArtist: String,
        apiTitle: String,
        localDuration: Long?,
        apiDuration: Long?
    ): Int {

        val titleScore =
            levenshtein(localTitle, apiTitle)

        val artistScore =
            levenshtein(localArtist, apiArtist)

        val durationScore =
            if (
                localDuration != null &&
                apiDuration != null
            ) {
                (abs(localDuration - apiDuration) / 1000).toInt()
            } else {
                0
            }

        return (
            (titleScore * 70) +
                    (artistScore * 20) +
                    (durationScore * 10)
                )
    }

    private fun normalizeTitle(title: String): String {
        return title
            .lowercase()
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("(?i)\\b(official|music|video|audio|lyrics|hd|4k|hq|full song|original|mix|remix)\\b"), "")
            .replace(Regex("(?i)feat\\..*"), "")
            .replace(Regex("(?i)ft\\..*"), "")
            .replace(Regex("(?i)- live.*"), "")
            .replace(Regex("(?i)- remaster.*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeArtist(artist: String): String {
        return artist
            .lowercase()
            .replace(
                Regex("feat\\..*", RegexOption.IGNORE_CASE),
                ""
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    private fun parseLrc(
        lrc: String
    ): List<SyncedLine> {

        val lines =
            mutableListOf<SyncedLine>()

        val timeRegex =
            Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]")

        lrc.lines().forEach { rawLine ->

            val matches =
                timeRegex.findAll(rawLine).toList()

            if (matches.isEmpty()) {
                return@forEach
            }

            val text =
                rawLine.replace(timeRegex, "")
                    .trim()

            matches.forEach { match ->

                val min =
                    match.groupValues[1].toLong()

                val sec =
                    match.groupValues[2].toLong()

                val ms =
                    match.groupValues[3]
                        .padEnd(3, '0')
                        .toLong()

                val time =
                    (min * 60_000L) +
                            (sec * 1000L) +
                            ms

                lines.add(
                    SyncedLine(
                        timestampMs = time,
                        text = text
                    )
                )
            }
        }

        return lines.sortedBy {
            it.timestampMs
        }
    }

    /*
     * Simple Levenshtein Distance
     * Tiny + good enough
     */

    private fun levenshtein(
        lhs: String,
        rhs: String
    ): Int {

        val dp =
            Array(lhs.length + 1) {
                IntArray(rhs.length + 1)
            }

        for (i in 0..lhs.length) {
            dp[i][0] = i
        }

        for (j in 0..rhs.length) {
            dp[0][j] = j
        }

        for (i in 1..lhs.length) {

            for (j in 1..rhs.length) {

                val cost =
                    if (lhs[i - 1] == rhs[j - 1]) {
                        0
                    } else {
                        1
                    }

                dp[i][j] =
                    minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
            }
        }

        return dp[lhs.length][rhs.length]
    }
}

@Serializable
data class LrclibSearchResult(

    @SerialName("id")
    val id: Int? = null,

    @SerialName("trackName")
    val trackName: String,

    @SerialName("artistName")
    val artistName: String,

    @SerialName("duration")
    val duration: Double? = null,

    @SerialName("plainLyrics")
    val plainLyrics: String? = null,

    @SerialName("syncedLyrics")
    val syncedLyrics: String? = null
)

