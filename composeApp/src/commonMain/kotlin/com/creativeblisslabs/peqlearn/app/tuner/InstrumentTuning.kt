package com.creativeblisslabs.peqlearn.app.tuner

import kotlinx.serialization.Serializable

/**
 * INSTRUMENT TUNING DATA MODEL & CATALOG
 * Matches user screenshots (Chrome Tuner):
 * - Auto vs Manual string selection
 * - Favorite tunings with pin icon
 * - Custom tunings with add/delete string & pitch buttons
 * - Categorization (Common, Power, Open, Extended, Drop)
 */
@Serializable
data class InstrumentTuning(
    val id: String,
    val name: String,
    val instrument: String, // "Guitar", "Bass", "Ukulele", "Violin", "Chromatic"
    val stringNotes: List<String>, // Top to bottom / High to Low (e.g. ["E4", "B3", "G3", "D3", "A2", "E2"])
    val category: String = "Common", // "Common", "Power", "Open", "Extended", "Favorites"
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
)

object TuningsCatalog {

    val BUILT_IN_TUNINGS = listOf(
        // GUITAR 6-STRING
        InstrumentTuning(
            id = "guitar_standard",
            name = "Standard",
            instrument = "Guitar",
            stringNotes = listOf("E4", "B3", "G3", "D3", "A2", "E2"),
            category = "Common",
            isFavorite = true
        ),
        InstrumentTuning(
            id = "guitar_drop_d",
            name = "Drop D",
            instrument = "Guitar",
            stringNotes = listOf("E4", "B3", "G3", "D3", "A2", "D2"),
            category = "Power",
            isFavorite = true
        ),
        InstrumentTuning(
            id = "guitar_half_step_down",
            name = "Half Step Down",
            instrument = "Guitar",
            stringNotes = listOf("D#4", "A#3", "F#3", "C#3", "G#2", "D#2"),
            category = "Common"
        ),
        InstrumentTuning(
            id = "guitar_whole_step_down",
            name = "Whole Step Down",
            instrument = "Guitar",
            stringNotes = listOf("D4", "A3", "F3", "C3", "G2", "D2"),
            category = "Common"
        ),
        InstrumentTuning(
            id = "guitar_open_d",
            name = "Open D",
            instrument = "Guitar",
            stringNotes = listOf("D4", "A3", "F#3", "D3", "A2", "D2"),
            category = "Open"
        ),
        InstrumentTuning(
            id = "guitar_open_g",
            name = "Open G",
            instrument = "Guitar",
            stringNotes = listOf("D4", "B3", "G3", "D3", "G2", "D2"),
            category = "Open"
        ),
        InstrumentTuning(
            id = "guitar_dadgad",
            name = "DADGAD",
            instrument = "Guitar",
            stringNotes = listOf("D4", "A3", "G3", "D3", "A2", "D2"),
            category = "Open"
        ),

        // 7-STRING GUITAR
        InstrumentTuning(
            id = "guitar_7_standard",
            name = "7-String Standard",
            instrument = "Guitar",
            stringNotes = listOf("E4", "B3", "G3", "D3", "A2", "E2", "B1"),
            category = "Extended"
        ),
        InstrumentTuning(
            id = "guitar_7_drop_a",
            name = "7-String Drop A",
            instrument = "Guitar",
            stringNotes = listOf("E4", "B3", "G3", "D3", "A2", "E2", "A1"),
            category = "Extended"
        ),

        // BASS (4 & 5 STRING)
        InstrumentTuning(
            id = "bass_standard",
            name = "Bass Standard (4-String)",
            instrument = "Bass",
            stringNotes = listOf("G2", "D2", "A1", "E1"),
            category = "Common",
            isFavorite = true
        ),
        InstrumentTuning(
            id = "bass_5_standard",
            name = "Bass Standard (5-String)",
            instrument = "Bass",
            stringNotes = listOf("G2", "D2", "A1", "E1", "B0"),
            category = "Extended"
        ),
        InstrumentTuning(
            id = "bass_drop_d",
            name = "Bass Drop D",
            instrument = "Bass",
            stringNotes = listOf("G2", "D2", "A1", "D1"),
            category = "Power"
        ),

        // UKULELE
        InstrumentTuning(
            id = "ukulele_standard",
            name = "Ukulele Standard (High G)",
            instrument = "Ukulele",
            stringNotes = listOf("A4", "E4", "C4", "G4"),
            category = "Common",
            isFavorite = true
        ),
        InstrumentTuning(
            id = "ukulele_low_g",
            name = "Ukulele Low G",
            instrument = "Ukulele",
            stringNotes = listOf("A4", "E4", "C4", "G3"),
            category = "Common"
        ),

        // VIOLIN
        InstrumentTuning(
            id = "violin_standard",
            name = "Violin Standard",
            instrument = "Violin",
            stringNotes = listOf("E5", "A4", "D4", "G3"),
            category = "Common",
            isFavorite = true
        )
    )

    // Helper to step a note up or down by semitones (e.g. "E4" + 1 -> "F4", "E4" - 1 -> "D#4")
    private val NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun shiftNote(note: String, deltaSemitones: Int): String {
        try {
            val letter = note.dropLast(1)
            val oct = note.takeLast(1).toIntOrNull() ?: 4
            val idx = NOTES.indexOf(letter)
            if (idx == -1) return note

            val totalSemis = (oct * 12) + idx + deltaSemitones
            val newOct = totalSemis / 12
            val newIdx = ((totalSemis % 12) + 12) % 12
            return "${NOTES[newIdx]}$newOct"
        } catch (_: Exception) {
            return note
        }
    }
}

