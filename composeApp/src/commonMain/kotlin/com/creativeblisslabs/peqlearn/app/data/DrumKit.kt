package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class GroovePattern(
    val id: String,
    val name: String,
    val genre: String,
    val bpm: Int = 120,
    val stepsPerBar: Int = 16,
    val barsCount: Int = 1,
    val events: List<BeatEvent> = emptyList(),
    val description: String = ""
)

@Serializable
data class GrooveLibrary(
    val version: Int = 1,
    val genres: List<String> = emptyList(),
    val patterns: List<GroovePattern> = emptyList()
)

data class DrumKit(
    val id: String,
    val name: String,
    val soundfontPath: String = "",
    val samplePads: Map<Int, String> = emptyMap(),
    val description: String = ""
)
@Serializable
data class BeatEvent(
    val step: Int,
    val padIndex: Int,
    val velocity: Float = 0.8f,
    val isAccent: Boolean = false
)
@Serializable
data class PartComposition(
    val partName: String,
    val patternId: String,
    val repeatCount: Int = 1,
    val muteChannels: List<Int> = emptyList()
)
