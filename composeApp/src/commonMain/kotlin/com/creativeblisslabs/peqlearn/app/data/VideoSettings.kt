package com.creativeblisslabs.peqlearn.app.data

/**
 * CINEMATIC VIDEO CONFIGURATION
 * Defines the physical viewport and orientation for video mastering.
 */
data class VideoSettings(
    val rotation: Int = 0, // 0, 90, 180, 270
    val scaleMode: VideoScaleMode = VideoScaleMode.FIT,
    val aspectRatio: Float? = null,
    val isRotationLocked: Boolean = false,
    val zoomLevel: Float = 1.0f
)

enum class VideoScaleMode {
    FIT, FILL, ZOOM, FIT_WIDTH, FIT_HEIGHT
}

/**
 * VIDEO FILE METADATA
 */
data class VideoFile(
    val id: String,
    val name: String,
    val title: String? = null,
    val path: String,
    val duration: Long,
    val sizeBytes: Long,
    val resolution: String = "1080p",
    val codec: String = "AVC",
    val fps: Int = 0,
    val bitrate: Int = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val hasSubtitles: Boolean = false,
    val hasMultiAudio: Boolean = false,
    val hasChapters: Boolean = false,
    val thumbnail: String? = null
)
