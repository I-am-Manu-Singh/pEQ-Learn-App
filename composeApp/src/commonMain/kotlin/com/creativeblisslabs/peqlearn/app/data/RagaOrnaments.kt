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
data class Alankaar(
    val number: Int,
    val name: String,
    val pattern: String,
    val midiPattern: List<Int>,
    val description: String,
    val arohan: String = "",
    val avarohan: String = "",
    val type: String = "Shuddha Swar Alankaar", // "Shuddha Swar" | "Vakra" | "Palta" | "Ornamental"
    val technique: String = "Sargam", // "Meend" | "Kan-swar" | "Andolan" | "Khatka" | "Murki" | "Palta" | "Sargam"
    val devanagariArohan: String = "",
    val devanagariAvarohan: String = ""
)
data class Thaat(
    val name: String,
    val semitones: List<Int>,
    val description: String
) {
    val swaras: List<String>
        get() = description.substringBefore("—").trim().split(" ")
}
/**
 * Taal (rhythmic cycle) data class.
 * Matches the original Java Taal constructor signature:
 * Taal(name, beats, vibhags, bolSequence, khali, tempo, description)
 */
data class Taal(
    val name: String,
    val beats: Int,
    val vibhags: List<Int>,
    val bolSequence: List<String>,
    val khali: Int,
    val tempo: String,
    val description: String,
    val taliIndices: List<Int> = emptyList(),
    val khaliIndices: List<Int> = emptyList(),
    val jati: String = "",
    val devanagariTheka: String = "",
    val notationGrid: String = "",
    val variations: List<String> = emptyList(),
    val origin: String = "Tabla", // "Tabla" | "Pakhawaj" | "Carnatic"
    val layakaris: Map<String, String> = emptyMap()
) {
    val khaliBeat: Int get() = khali
    val tempoRange: String get() = tempo
}

