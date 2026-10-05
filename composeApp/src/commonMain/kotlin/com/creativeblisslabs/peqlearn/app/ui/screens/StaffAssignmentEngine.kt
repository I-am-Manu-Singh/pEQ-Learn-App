package com.creativeblisslabs.peqlearn.app.ui.screens

import kotlin.math.abs

/**
 * 100% faithful port of StaffAssignmentEngine from original recovered sources.
 * Computes optimal staff splitting between Bass (false) and Treble (true) clef using cost scoring.
 */
object StaffAssignmentEngine {

    fun assign(notes: List<ScoreNote>): Map<ScoreNote, Boolean> {
        val result = LinkedHashMap<ScoreNote, Boolean>()
        if (notes.isEmpty()) return result

        val sortedNotes = notes.sortedBy { it.midiNote }
        val size = sortedNotes.size

        if (size <= 1) {
            for (note in sortedNotes) {
                result[note] = note.midiNote >= 60
            }
            return result
        }

        val allHigh = sortedNotes.all { it.midiNote >= 60 }
        val allLow = sortedNotes.all { it.midiNote < 60 }

        if (allHigh) {
            for (note in sortedNotes) {
                result[note] = true
            }
        } else if (allLow) {
            for (note in sortedNotes) {
                result[note] = false
            }
        } else {
            val best = chooseSplit(sortedNotes)
            for (note in sortedNotes) {
                if (best.treble.contains(note)) {
                    result[note] = true
                } else if (best.bass.contains(note)) {
                    result[note] = false
                } else {
                    result[note] = note.midiNote >= 60
                }
            }
        }
        return result
    }

    private fun chooseSplit(notes: List<ScoreNote>): StaffAssignment {
        var bestScore = Float.MAX_VALUE
        var best: StaffAssignment? = null
        val size = notes.size

        for (split in 1 until size) {
            val bass = notes.take(split).toSet()
            val treble = notes.drop(split).toSet()
            val score = scoreSplit(bass, treble)
            if (score < bestScore) {
                bestScore = score
                best = StaffAssignment(treble = treble, bass = bass)
            }
        }
        return best ?: StaffAssignment(treble = notes.toSet(), bass = emptySet())
    }

    private fun scoreSplit(bass: Set<ScoreNote>, treble: Set<ScoreNote>): Float {
        var score = 0.0f

        for (it in bass) {
            if (it.midiNote > 60) {
                score += (it.midiNote - 60) * 2.0f
            }
            if (it.midiNote > 55) {
                score += (it.midiNote - 55) * 3.0f
            }
            if (it.midiNote > 52) {
                score += (it.midiNote - 52) * 6.0f
            }
        }

        for (it in treble) {
            if (it.midiNote < 60) {
                score += (60 - it.midiNote) * 2.0f
            }
            if (it.midiNote < 64) {
                score += (64 - it.midiNote) * 3.0f
            }
            if (it.midiNote < 67) {
                score += (67 - it.midiNote) * 6.0f
            }
        }

        if (bass.isNotEmpty()) {
            val maxBass = bass.maxOf { it.midiNote }
            val minBass = bass.minOf { it.midiNote }
            score += (maxBass - minBass) * 0.25f

            val bassCenter = bass.map { it.midiNote }.average()
            score += (abs(bassCenter - 48.0) * 0.8).toFloat()
        }

        if (treble.isNotEmpty()) {
            val maxTreble = treble.maxOf { it.midiNote }
            val minTreble = treble.minOf { it.midiNote }
            score += (maxTreble - minTreble) * 0.25f

            val trebleCenter = treble.map { it.midiNote }.average()
            score += (abs(trebleCenter - 72.0) * 0.8).toFloat()
        }

        if (bass.size == 1 && treble.size >= 3) {
            score += 25.0f
        }
        if (treble.size == 1 && bass.size >= 3) {
            score += 25.0f
        }

        val maxBassMidi = bass.maxOfOrNull { it.midiNote } ?: 0
        val minTrebleMidi = treble.minOfOrNull { it.midiNote } ?: 127
        if (maxBassMidi > minTrebleMidi) {
            score += 10000.0f
        }

        return score
    }
}
