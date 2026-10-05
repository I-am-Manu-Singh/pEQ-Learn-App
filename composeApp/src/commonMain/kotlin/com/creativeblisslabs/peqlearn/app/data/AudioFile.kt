package com.creativeblisslabs.peqlearn.app.data

/**
 * INDUSTRIAL AUDIO FILE METADATA Defines high-fidelity attributes for audiophile and
 * production-grade stems.
 */
data class AudioFile(
    val id: String,
    val name: String,
    val title: String? = null,
    val path: String,
    val duration: Long,
    val sizeBytes: Long,
    val bitDepth: Int = 16,
    val sampleRate: Int = 44100,
    val format: String = "MP3",
    val isHiRes: Boolean = false,
    val isLossless: Boolean = false,
    val isVideo: Boolean = false,
    val hasLyrics: Boolean = false,
    val artist: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val genre: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val bitrate: Int = 0,
    val channels: Int = 2,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val thumbnail: String? = null,
    val lyrics: String? = null
)
