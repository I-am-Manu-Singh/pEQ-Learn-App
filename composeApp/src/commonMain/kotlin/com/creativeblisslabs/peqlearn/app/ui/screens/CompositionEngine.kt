package com.creativeblisslabs.peqlearn.app.ui.screens

import com.creativeblisslabs.peqlearn.app.util.NOTE_NAMES_CD

data class HarmonicSuggestion(
    val name: String,
    val quality: String,
    val roman: String
)

object CompositionEngine {
    fun getSuggestions(rootIdx: Int): List<HarmonicSuggestion> {
        val root = NOTE_NAMES_CD[rootIdx % 12]
        val dominant = NOTE_NAMES_CD[(rootIdx + 7) % 12]
        val subdominant = NOTE_NAMES_CD[(rootIdx + 5) % 12]
        val submediant = NOTE_NAMES_CD[(rootIdx + 9) % 12]

        return listOf(
            HarmonicSuggestion(name = dominant, quality = "", roman = "V"),
            HarmonicSuggestion(name = subdominant, quality = "", roman = "IV"),
            HarmonicSuggestion(name = submediant, quality = "m", roman = "vi"),
            HarmonicSuggestion(name = root, quality = "", roman = "I")
        )
    }
}
