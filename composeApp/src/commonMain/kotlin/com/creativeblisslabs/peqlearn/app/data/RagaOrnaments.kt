package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class OrnamentType {
    MEEND,      // Glissando / continuous pitch glide
    KAN,        // Grace note
    ANDOLAN,    // Slow oscillation
    GAMAK,      // Heavy oscillation
    MURKI,      // Quick turn of 3-4 notes
    KHATKA      // Crisp cluster of notes
}

@Serializable
data class RagaOrnament(
    val type: OrnamentType,
    val fromNote: String,
    val toNote: String,
    val durationMs: Long = 200,
    val description: String = ""
)
