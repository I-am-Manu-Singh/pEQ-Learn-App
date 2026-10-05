package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import kotlinx.serialization.Serializable

data class DrumKitPreset(
    val id: String,
    val name: String = id,
    val kitType: String = id,
    val bpm: Int = 120,
    val volume: Float = 1.0f,
    val labels: List<String> = emptyList()
) {
    constructor(id: String, labels: List<String>) : this(
        id = id,
        name = id,
        kitType = id,
        bpm = 120,
        volume = 1.0f,
        labels = labels
    )
}

data class SongBar(
    val barNumber: Int = 1,
    val patternId: String = "",
    val lengthSteps: Int = 16,
    val isSolo: Boolean = false,
    val isMuted: Boolean = false,
    val stableId: String = java.util.UUID.randomUUID().toString(),
    var patternName: String = "",
    var isFiller: Boolean = false
) {
    constructor(id: String, patternName: String, isFiller: Boolean = false) : this(
        barNumber = id.toIntOrNull() ?: 1,
        patternId = patternName,
        patternName = patternName,
        isFiller = isFiller
    )
}

@Serializable
data class DrumTutorial(
    val name: String,
    val steps: List<List<String>> = emptyList(),
    val id: String = name,
    val title: String = name,
    val difficulty: String = "Beginner", // Beginner, Intermediate, Advanced
    val bpm: Int = 120,
    val patternId: String = name,
    val lessonNotes: String = ""
)
