package com.creativeblisslabs.peqlearn.app.ai

import com.creativeblisslabs.peqlearn.app.network.LrcLibResponse
import kotlinx.serialization.Serializable

@Serializable
data class MusicSegment(
    val start: Float,
    val end: Float,
    val label: String
)

@Serializable
data class AllInOneAnalysisResult(
    val path: String = "",
    val bpm: Float = 120f,
    val beats: List<Float> = emptyList(),
    val downbeats: List<Float> = emptyList(),
    val beatPositions: List<Int> = emptyList(),
    val segments: List<MusicSegment> = emptyList(),
    val harmonicAnalysis: SongWritingHarmonicAnalysis? = null,
    val lrcLyrics: LrcLibResponse? = null
) {
    val durationSeconds: Float
        get() = segments.lastOrNull()?.end ?: beats.lastOrNull() ?: 0f

    fun getSegmentAt(timeSeconds: Float): MusicSegment? {
        return segments.find { timeSeconds >= it.start && timeSeconds < it.end }
            ?: segments.lastOrNull()?.takeIf { timeSeconds >= it.start }
    }

    fun getChordAt(timeSeconds: Float): DetectedChord? {
        return harmonicAnalysis?.detectedChords?.find { timeSeconds >= it.timestampSeconds && timeSeconds < (it.timestampSeconds + it.durationSeconds) }
    }
}
