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
