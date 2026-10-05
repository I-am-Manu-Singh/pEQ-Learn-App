package com.creativeblisslabs.peqlearn.app.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.creativeblisslabs.peqlearn.app.data.pEQConstants

@Serializable
data class OSSubtitleResponse(
    val data: List<OSSubtitleData>
)

@Serializable
data class OSSubtitleData(
    val id: String,
    val attributes: OSSubtitleAttributes
)

@Serializable
data class OSSubtitleAttributes(
    val language: String,
    val release: String,
    val download_count: Int,
    val url: String? = null,
    val files: List<OSSubtitleFile> = emptyList()
)

@Serializable
data class OSSubtitleFile(
    val file_id: Int,
    val file_name: String
)

@Serializable
data class OSDownloadResponse(
    val link: String
)


class OpenSubtitlesClient(apiKey: String) {
    private val effectiveApiKey = apiKey.ifBlank { pEQConstants.OPEN_SUBTITLES_INTERNAL_KEY }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun searchSubtitles(query: String, lang: String = "en"): List<OSSubtitleAttributes> {
        if (effectiveApiKey.isBlank()) return emptyList()
        return try {
            val response: OSSubtitleResponse = client.get("https://api.opensubtitles.com/api/v1/subtitles") {
                header("Api-Key", effectiveApiKey)
                header("User-Agent", "pEQ Neural Workstation v1.0")
                parameter("query", query)
                parameter("languages", lang)
            }.body()
            response.data.map { it.attributes }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDownloadLink(fileId: Int): String? {
        if (effectiveApiKey.isBlank()) return null
        return try {
            val response: OSDownloadResponse = client.post("https://api.opensubtitles.com/api/v1/download") {
                header("Api-Key", effectiveApiKey)
                header("User-Agent", "pEQ Neural Workstation v1.0")
                header("Content-Type", "application/json")
                setBody(mapOf("file_id" to fileId))
            }.body()
            response.link
        } catch (e: Exception) {
            null
        }
    }
}
