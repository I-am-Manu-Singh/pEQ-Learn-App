package com.creativeblisslabs.peqlearn.app.util

/**
 * Real-Time Polyphonic Chord Detector for MIDI input and Waterfall Playback.
 * Parallels MIDIVisualizer's ChordDetector.cpp.
 *
 * Identifies chords, qualities (Major, Minor, 7th, 9th, sus, dim, aug),
 * and inversions / slash chords from a set of sounding MIDI note numbers.
 */
object LiveChordDetector {

    data class DetectedChord(
        val rootName: String,
        val chordQuality: String,
        val fullName: String,
        val inversionText: String,
        val bassNoteName: String,
        val isSlashChord: Boolean = false
    )

    private val NOTE_NAMES = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

    // Common chord formulas defined by semitone offsets from root (0)
    private val CHORD_TEMPLATES = listOf(
        Triple("Major", "", setOf(0, 4, 7)),
        Triple("Minor", "m", setOf(0, 3, 7)),
        Triple("Dominant 7th", "7", setOf(0, 4, 7, 10)),
        Triple("Major 7th", "maj7", setOf(0, 4, 7, 11)),
        Triple("Minor 7th", "m7", setOf(0, 3, 7, 10)),
        Triple("Minor Major 7th", "m(maj7)", setOf(0, 3, 7, 11)),
        Triple("Diminished", "dim", setOf(0, 3, 6)),
        Triple("Diminished 7th", "dim7", setOf(0, 3, 6, 9)),
        Triple("Half-Diminished", "m7♭5", setOf(0, 3, 6, 10)),
        Triple("Augmented", "aug", setOf(0, 4, 8)),
        Triple("Augmented 7th", "aug7", setOf(0, 4, 8, 10)),
        Triple("Suspended 4th", "sus4", setOf(0, 5, 7)),
        Triple("Suspended 2nd", "sus2", setOf(0, 2, 7)),
        Triple("7th Suspended 4th", "7sus4", setOf(0, 5, 7, 10)),
        Triple("Major 6th", "6", setOf(0, 4, 7, 9)),
        Triple("Minor 6th", "m6", setOf(0, 3, 7, 9)),
        Triple("Add 9", "add9", setOf(0, 2, 4, 7)),
        Triple("9th", "9", setOf(0, 2, 4, 7, 10)),
        Triple("Major 9th", "maj9", setOf(0, 2, 4, 7, 11)),
        Triple("Minor 9th", "m9", setOf(0, 2, 3, 7, 10)),
        Triple("Power Chord", "5", setOf(0, 7))
    )

    fun detect(notes: Collection<Int>): DetectedChord? {
        if (notes.isEmpty()) return null
        val distinctPitches = notes.map { it % 12 }.distinct()
        if (distinctPitches.isEmpty()) return null

        val lowestMidi = notes.minOrNull() ?: return null
        val bassPitch = lowestMidi % 12
        val bassNoteName = NOTE_NAMES[bassPitch]

        // Single note / Unison
        if (distinctPitches.size == 1) {
            return DetectedChord(
                rootName = bassNoteName,
                chordQuality = "Note",
                fullName = bassNoteName,
                inversionText = "Single Note",
                bassNoteName = bassNoteName
            )
        }

        // Test each pitch in the set as the prospective root note
        for (rootPitch in distinctPitches) {
            val relativeIntervals = distinctPitches.map { (it - rootPitch + 12) % 12 }.toSet()

            for ((qualityName, suffix, template) in CHORD_TEMPLATES) {
                if (relativeIntervals == template) {
                    val rootName = NOTE_NAMES[rootPitch]
                    val isSlash = bassPitch != rootPitch
                    val inversion = when {
                        !isSlash -> "Root Position"
                        relativeIntervals.contains(4) && bassPitch == (rootPitch + 4) % 12 -> "1st Inversion (3rd in Bass)"
                        relativeIntervals.contains(3) && bassPitch == (rootPitch + 3) % 12 -> "1st Inversion (♭3 in Bass)"
                        relativeIntervals.contains(7) && bassPitch == (rootPitch + 7) % 12 -> "2nd Inversion (5th in Bass)"
                        relativeIntervals.contains(10) && bassPitch == (rootPitch + 10) % 12 -> "3rd Inversion (7th in Bass)"
                        relativeIntervals.contains(11) && bassPitch == (rootPitch + 11) % 12 -> "3rd Inversion (maj7 in Bass)"
                        else -> "Slash Chord (/$bassNoteName)"
                    }

                    val fullChordName = if (isSlash) "$rootName$suffix/$bassNoteName" else "$rootName$suffix"
                    return DetectedChord(
                        rootName = rootName,
                        chordQuality = qualityName,
                        fullName = fullChordName,
                        inversionText = inversion,
                        bassNoteName = bassNoteName,
                        isSlashChord = isSlash
                    )
                }
            }
        }

        // Partial match / subset match if 3+ notes sounding
        if (distinctPitches.size >= 3) {
            for (rootPitch in distinctPitches) {
                val relativeIntervals = distinctPitches.map { (it - rootPitch + 12) % 12 }.toSet()
                for ((qualityName, suffix, template) in CHORD_TEMPLATES) {
                    if (template.size >= 3 && template.all { relativeIntervals.contains(it) }) {
                        val rootName = NOTE_NAMES[rootPitch]
                        val isSlash = bassPitch != rootPitch
                        val fullChordName = if (isSlash) "$rootName$suffix/$bassNoteName" else "$rootName$suffix"
                        return DetectedChord(
                            rootName = rootName,
                            chordQuality = qualityName,
                            fullName = fullChordName,
                            inversionText = if (isSlash) "Inverted / Slash" else "Root Position",
                            bassNoteName = bassNoteName,
                            isSlashChord = isSlash
                        )
                    }
                }
            }
        }

        // Fallback for dyad / intervals
        if (distinctPitches.size == 2) {
            val semitoneDist = (distinctPitches[1] - distinctPitches[0] + 12) % 12
            val intervalName = when (semitoneDist) {
                1, 11 -> "Semitone / Minor 2nd"
                2, 10 -> "Whole Tone / 2nd"
                3, 9 -> "Minor 3rd"
                4, 8 -> "Major 3rd"
                5, 7 -> "Fourth / Fifth"
                6 -> "Tritone"
                else -> "Dyad"
            }
            return DetectedChord(
                rootName = bassNoteName,
                chordQuality = intervalName,
                fullName = "$bassNoteName ($intervalName)",
                inversionText = "Harmonic Interval",
                bassNoteName = bassNoteName
            )
        }

        return null
    }
}
