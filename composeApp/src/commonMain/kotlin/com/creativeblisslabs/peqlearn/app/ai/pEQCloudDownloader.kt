package com.creativeblisslabs.peqlearn.app.ai

/**
 * EXPERIMENTAL NEURAL BRIDGE: pEQ CLOUD DOWNLOADER
 * Interface for high-fidelity stream extraction.
 */
import kotlinx.serialization.Serializable

@Serializable
data class FormatInfo(
    val formatId: String,
    val ext: String,
    val note: String,
    val acodec: String? = null,
    val vcodec: String? = null,
    val isAudio: Boolean = false,
    val isVideo: Boolean = false,
    val sizeBytes: Long = 0L
)

interface pEQCloudDownloader {
    fun downloadAudio(url: String, onProgress: (Float, Long) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit)
    fun downloadMedia(url: String, formatId: String?, isAudioOnly: Boolean, onProgress: (Float, Long) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit)
    suspend fun getStreamUrl(url: String): String?
    suspend fun getFormats(url: String): List<FormatInfo>
}
