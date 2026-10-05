package com.creativeblisslabs.peqlearn.app.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * INDUSTRIAL TEMPORAL DATA: CHAPTERS & MARKERS
 */
@Serializable
data class MediaChapter(
    val id: Long = Random.nextLong(),
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val thumbnail: String? = null
)

/**
 * SYNCED LYRICS LINE: LRC FORMAT
 */
@Serializable
data class SyncedLine(
    val id: Long = Random.nextLong(),
    val timestampMs: Long,
    val text: String,
    val translation: String? = null,
    val content: String = text // For compatibility with existing UI
)

/**
 * STREAM TRACK: SUBTITLES & AUDIO
 */
@Serializable
data class MediaTrack(
    val id: String,
    val language: String,
    val type: TrackType,
    val codec: String? = null,
    val isHardwareDecoded: Boolean = true,
    val isExternal: Boolean = false,
    val externalUrl: String? = null
)

enum class TrackType {
    AUDIO, SUBTITLE, VIDEO, LYRICS
}

/**
 * pEQ METADATA HUB
 * The source of truth for all synchronized media artifacts.
 */
@Serializable
data class pEQMediaMetadata(
    val chapters: List<MediaChapter> = emptyList(),
    val lyrics: List<SyncedLine> = emptyList(),
    val activeAudioTrack: MediaTrack? = null,
    val activeSubtitleTrack: MediaTrack? = null,
    val availableTracks: List<MediaTrack> = emptyList()
)
