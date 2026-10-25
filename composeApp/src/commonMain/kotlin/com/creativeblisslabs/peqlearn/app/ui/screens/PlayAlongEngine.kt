package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlin.math.roundToInt

data class PlayAlongEvaluationNote(
    val id: Int,
    val midiNote: Int,
    val expectedTimestampMs: Long,
    val durationMs: Long,
    var status: NoteEvaluationStatus = NoteEvaluationStatus.PENDING,
    var hitTimestampMs: Long? = null,
    var timingDeltaMs: Long? = null
)

enum class NoteEvaluationStatus {
    PENDING,
    HIT_PERFECT,  // within +/- 60ms
    HIT_GOOD,     // within +/- 150ms
    MISSED,       // window passed without key press
    WRONG_PITCH   // wrong key pressed during window
}

data class PlayAlongScorecard(
    val totalExpectedNotes: Int = 0,
    val hitNotesCount: Int = 0,
    val missedNotesCount: Int = 0,
    val wrongNotesCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val averageTimingErrorMs: Float = 0f,
    val accuracyPercentage: Float = 0f,
    val grade: String = "N/A"
)

class PlayAlongEngine {
    val isEvaluating = mutableStateOf(false)
    val isPracticeMode = mutableStateOf(false)
    val waitForUserMode = mutableStateOf(false)

    // Notes tracking
    val expectedNotes = mutableStateListOf<PlayAlongEvaluationNote>()
    val scorecard = mutableStateOf(PlayAlongScorecard())

    // Connected USB MIDI feedback: Map of midiNote -> Color status
    // Green (Hit), Red (Wrong), Gold (Expected right now)
    val liveKeyFeedbacks = androidx.compose.runtime.mutableStateMapOf<Int, Long>() // midiNote -> expiryTimeMs
    val liveKeyColors = androidx.compose.runtime.mutableStateMapOf<Int, androidx.compose.ui.graphics.Color>()

    // Current hit streak
    val streak = mutableIntStateOf(0)
    val bestStreak = mutableIntStateOf(0)
    val hitCount = mutableIntStateOf(0)
    val missCount = mutableIntStateOf(0)
    val wrongCount = mutableIntStateOf(0)
    private val timingErrors = mutableListOf<Long>()

    fun startEvaluation(score: Score, currentTempo: Float) {
        expectedNotes.clear()
        timingErrors.clear()
        streak.intValue = 0
        bestStreak.intValue = 0
        hitCount.intValue = 0
        missCount.intValue = 0
        wrongCount.intValue = 0
        liveKeyFeedbacks.clear()
        liveKeyColors.clear()

        var currentTotalMs = 0L
        val secondsPerBeat = 60f / currentTempo

        var noteIdCounter = 0
        score.measures.forEach { measure ->
            val beatsPerMeasure = measure.timeSignatureNumerator.toFloat().coerceAtLeast(1f)
            val measureDurMs = (beatsPerMeasure * secondsPerBeat * 1000).toLong()

            measure.notes.filter { !it.isRest }.forEach { note ->
                val noteStartMs = currentTotalMs + (note.position * secondsPerBeat * 1000).toLong()
                val noteDurMs = (note.duration * secondsPerBeat * 1000).toLong().coerceAtLeast(80L)
                expectedNotes.add(
                    PlayAlongEvaluationNote(
                        id = noteIdCounter++,
                        midiNote = note.midiNote,
                        expectedTimestampMs = noteStartMs,
                        durationMs = noteDurMs
                    )
                )
            }
            currentTotalMs += measureDurMs
        }

        isEvaluating.value = true
        updateScorecard()
    }

    fun stopEvaluation() {
        isEvaluating.value = false
        updateScorecard()
    }

    fun onUserMidiNoteOn(pressedNote: Int, currentPlayheadMs: Long) {
        if (!isEvaluating.value) return

        val toleranceWindowMs = 280L
        // Find nearest pending note matching pitch
        val matchingNote = expectedNotes.firstOrNull { note ->
            note.status == NoteEvaluationStatus.PENDING &&
            note.midiNote == pressedNote &&
            kotlin.math.abs(note.expectedTimestampMs - currentPlayheadMs) <= toleranceWindowMs
        }

        if (matchingNote != null) {
            val delta = currentPlayheadMs - matchingNote.expectedTimestampMs
            matchingNote.hitTimestampMs = currentPlayheadMs
            matchingNote.timingDeltaMs = delta
            matchingNote.status = if (kotlin.math.abs(delta) <= 80L) NoteEvaluationStatus.HIT_PERFECT else NoteEvaluationStatus.HIT_GOOD
            hitCount.intValue++
            streak.intValue++
            if (streak.intValue > bestStreak.intValue) {
                bestStreak.intValue = streak.intValue
            }
            timingErrors.add(kotlin.math.abs(delta))

            // Live Green Feedback for correct note
            val hitColor = if (matchingNote.status == NoteEvaluationStatus.HIT_PERFECT) androidx.compose.ui.graphics.Color(0xFF00E676) else androidx.compose.ui.graphics.Color(0xFF69F0AE)
            liveKeyColors[pressedNote] = hitColor
            liveKeyFeedbacks[pressedNote] = System.currentTimeMillis() + 400L
        } else {
            // Check if there was an expected note around this time with a different pitch
            val anyExpectedNote = expectedNotes.firstOrNull { note ->
                note.status == NoteEvaluationStatus.PENDING &&
                kotlin.math.abs(note.expectedTimestampMs - currentPlayheadMs) <= toleranceWindowMs
            }
            if (anyExpectedNote != null) {
                wrongCount.intValue++
                streak.intValue = 0
                // Live Red Feedback for wrong pitch
                liveKeyColors[pressedNote] = androidx.compose.ui.graphics.Color(0xFFFF3D00)
                liveKeyFeedbacks[pressedNote] = System.currentTimeMillis() + 500L
            }
        }
        updateScorecard()
    }

    fun updatePlayheadPosition(currentPlayheadMs: Long) {
        if (!isEvaluating.value) return

        val now = System.currentTimeMillis()
        liveKeyFeedbacks.entries.removeIf { (note, expiry) ->
            if (now >= expiry) {
                liveKeyColors.remove(note)
                true
            } else false
        }

        val missCutoff = currentPlayheadMs - 280L
        var hasNewMisses = false
        expectedNotes.forEach { note ->
            if (note.status == NoteEvaluationStatus.PENDING && note.expectedTimestampMs < missCutoff) {
                note.status = NoteEvaluationStatus.MISSED
                missCount.intValue++
                streak.intValue = 0
                hasNewMisses = true
            }
        }

        if (hasNewMisses) {
            updateScorecard()
        }
    }

    private fun updateScorecard() {
        val total = expectedNotes.size.coerceAtLeast(1)
        val hits = hitCount.intValue
        val misses = missCount.intValue
        val wrongs = wrongCount.intValue
        val accuracy = ((hits.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
        val avgDelta = if (timingErrors.isNotEmpty()) timingErrors.average().toFloat() else 0f

        val grade = when {
            accuracy >= 95f -> "S"
            accuracy >= 85f -> "A"
            accuracy >= 75f -> "B"
            accuracy >= 60f -> "C"
            accuracy >= 45f -> "D"
            else -> "F"
        }

        scorecard.value = PlayAlongScorecard(
            totalExpectedNotes = expectedNotes.size,
            hitNotesCount = hits,
            missedNotesCount = misses,
            wrongNotesCount = wrongs,
            currentStreak = streak.intValue,
            bestStreak = bestStreak.intValue,
            averageTimingErrorMs = (avgDelta * 10f).roundToInt() / 10f,
            accuracyPercentage = (accuracy * 10f).roundToInt() / 10f,
            grade = grade
        )
    }
}
