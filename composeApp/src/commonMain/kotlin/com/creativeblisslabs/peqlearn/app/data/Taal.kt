package com.creativeblisslabs.peqlearn.app.data

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
    val description: String
) {
    val khaliBeat: Int get() = khali
    val tempoRange: String get() = tempo
}
