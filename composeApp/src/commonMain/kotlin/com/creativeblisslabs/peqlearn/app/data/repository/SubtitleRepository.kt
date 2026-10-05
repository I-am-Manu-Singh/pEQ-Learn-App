package com.creativeblisslabs.peqlearn.app.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.creativeblisslabs.peqlearn.app.network.createHttpClient

class SubtitleRepository(
    private val apiKeyProvider: () -> String
) {
    private val apiKey get() = apiKeyProvider().ifBlank { com.creativeblisslabs.peqlearn.app.data.pEQConstants.OPEN_SUBTITLES_INTERNAL_KEY }
    private val client = createHttpClient().config {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }

    private val cache = mutableMapOf<String, List<SubtitleMatch>>()


private val activeRequests =

    mutableMapOf<String, List<SubtitleMatch>?>()
    suspend fun searchSubtitles(
        title: String,
        artist: String? = null,
        season: Int? = null,
        episode: Int? = null,
        language: String = "en"
    ): List<SubtitleMatch> {
        if (apiKey.isBlank()) {
            println("pEQControl: [SUBTITLES] No API Key provided")
            return emptyList()
        }

        val normalizedTitle = normalizeTitle(title)
        val cacheKey = "$normalizedTitle-$language-$season-$episode"
        cache[cacheKey]?.let { return it }

        return try {
            val query = buildQuery(normalizedTitle, artist, season, episode)
            val response: OSSubtitleResponse = client.get("https://api.opensubtitles.com/api/v1/subtitles") {
                header("Api-Key", apiKey)
                header("User-Agent", "pEQ Neural Workstation v1.0")
                parameter("query", query)
                parameter("languages", language)
            }.body()

            val ranked = response.data.map { data ->
                val attr = data.attributes
                val release = attr.release
                val cleanedRelease = normalizeRelease(release)
                
                val score = calculateScore(
                    localTitle = normalizedTitle,
                    apiRelease = cleanedRelease,
                    localArtist = artist,
                    season = season,
                    episode = episode,
                    release = release,
                    movieHashMatched = attr.moviehashMatch == true
                )

                SubtitleMatch(
                    id = data.id,
                    fileName = attr.files.firstOrNull()?.fileName ?: release,
                    language = attr.language,
                    fileId = attr.files.firstOrNull()?.fileId ?: 0,
                    score = score
                )
            }
                .sortedBy { it.score }
                .take(20)

            cache[cacheKey] = ranked
            ranked
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDownloadLink(fileId: Int): String? {
        if (apiKey.isBlank() || fileId == 0) return null
        return try {
            val response: OSDownloadResponse = client.post("https://api.opensubtitles.com/api/v1/download") {
                header("Api-Key", apiKey)
                header("User-Agent", "pEQ Neural Workstation v1.0")
                contentType(ContentType.Application.Json)
                setBody(mapOf("file_id" to fileId))
            }.body()
            response.link
        } catch (e: Exception) {
            null
        }
    }

    private fun buildQuery(title: String, artist: String?, season: Int?, episode: Int?): String {
        val builder = StringBuilder()
        if (!artist.isNullOrBlank()) builder.append("$artist ")
        builder.append(title)
        if (season != null && episode != null) {
            builder.append(" S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}")
        }
        return builder.toString()
    }

    private fun calculateScore(
        localTitle: String,
        apiRelease: String,
        localArtist: String?,
        season: Int?,
        episode: Int?,
        release: String,
        movieHashMatched: Boolean
    ): Int {
        var score = levenshtein(localTitle, apiRelease)
        val releaseLower = release.lowercase()
        
        if (releaseLower.contains(localTitle.lowercase())) score -= 20
        if (!localArtist.isNullOrBlank() && releaseLower.contains(localArtist.lowercase())) score -= 10
        
        if (season != null && episode != null) {
            val pattern = "s${season.toString().padStart(2, '0')}e${episode.toString().padStart(2, '0')}"
            if (releaseLower.contains(pattern)) score -= 30
        }
        
        if (movieHashMatched) score -= 50
        return maxOf(score, 0)
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("feat\\..*"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeRelease(release: String): String {
        return release.lowercase()
            .replace(Regex("\\b(2160p|1080p|720p|x264|x265|h264|h265|bluray|webrip|web-dl|hdr|dvdrip|aac|dts|hevc|10bit)\\b"), "")
            .replace(Regex("-[A-Za-z0-9]+$"), "")
            .replace(Regex("[._]"), " ")
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshtein(lhs: String, rhs: String): Int {
        val dp = Array(lhs.length + 1) { IntArray(rhs.length + 1) }
        for (i in 0..lhs.length) dp[i][0] = i
        for (j in 0..rhs.length) dp[0][j] = j
        for (i in 1..lhs.length) {
            for (j in 1..rhs.length) {
                val cost = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[lhs.length][rhs.length]
    }
}

@Serializable
data class OSSubtitleResponse(val data: List<OSSubtitleData>)

@Serializable
data class OSSubtitleData(val id: String, val attributes: OSSubtitleAttributes)

@Serializable
data class OSSubtitleAttributes(
    val language: String,
    val release: String,
    val files: List<OSSubtitleFile> = emptyList(),
    @SerialName("moviehash_match") val moviehashMatch: Boolean? = null
)

@Serializable
data class OSSubtitleFile(
    @SerialName("file_id") val fileId: Int,
    @SerialName("file_name") val fileName: String
)

@Serializable
data class OSDownloadResponse(val link: String)

data class SubtitleMatch(
    val id: String,
    val fileName: String,
    val language: String,
    val fileId: Int,
    val score: Int
)