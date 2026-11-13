package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt

data class PlayAlongEvaluationNote(
    val id: Int,
    val midiNote: Int,
    val measureIndex: Int,
    val beatPosition: Float,
    val expectedTimestampMs: Long,
    val durationMs: Long,
    var status: NoteEvaluationStatus = NoteEvaluationStatus.PENDING,
    var hitTimestampMs: Long? = null,
    var timingDeltaMs: Long? = null
)

enum class NoteEvaluationStatus {
    PENDING,
    HIT_PERFECT,  // within +/- 70ms
    HIT_GOOD,     // within +/- 180ms
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

    val practiceHandMode = mutableStateOf("BOTH") // "BOTH", "RIGHT", "LEFT"
    val loopStartMeasure = mutableIntStateOf(-1) // -1 = disabled
    val loopEndMeasure = mutableIntStateOf(-1)

    val expectedNotes = mutableStateListOf<PlayAlongEvaluationNote>()
    val scorecard = mutableStateOf(PlayAlongScorecard())

    // Connected USB MIDI feedback: Green (Hit), Red (Wrong), Gold (Wait-For-Note Target)
    val liveKeyFeedbacks = mutableStateMapOf<Int, Long>() // midiNote -> expiryTimeMs
    val liveKeyColors = mutableStateMapOf<Int, Color>()
    val activeStepTargetNotes = mutableStateListOf<Int>()

    val streak = mutableIntStateOf(0)
    val bestStreak = mutableIntStateOf(0)
    val hitCount = mutableIntStateOf(0)
    val missCount = mutableIntStateOf(0)
    val wrongCount = mutableIntStateOf(0)
    private val timingErrors = mutableListOf<Long>()

    /**
     * FIXED:
     * 1. Uses exact quarter-note beats `(numerator * (4f / denominator))` so 6/8, 3/8, 12/8 never drift!
     * 2. Excludes GM Channel 10 Drum tracks (`resolvePlayableMelodicTrackIndices`) and respects LH/RH hide flags.
     * 3. Applies `transposition` so transposed playback grades accurately against USB MIDI input.
     */
    fun startEvaluation(
        score: Score,
        currentTempo: Float,
        enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
        isLeftHandHidden: Boolean = false,
        isRightHandHidden: Boolean = false,
        transposition: Int = 0
    ) {
        expectedNotes.clear()
        timingErrors.clear()
        streak.intValue = 0
        bestStreak.intValue = 0
        hitCount.intValue = 0
        missCount.intValue = 0
        wrongCount.intValue = 0
        liveKeyFeedbacks.clear()
        liveKeyColors.clear()
        activeStepTargetNotes.clear()

        val melodicTrackSet = resolvePlayableMelodicTrackIndices(score, enabledTrackIndices)
        val safeTempo = currentTempo.coerceAtLeast(20f)
        val secondsPerQuarterBeat = 60f / safeTempo
        var currentTotalMs = 0L
        var noteIdCounter = 0

        score.measures.forEachIndexed { mIdx, measure ->
            val quarterBeatsPerMeasure = (measure.timeSignatureNumerator.toFloat() *
                (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
            val measureDurMs = (quarterBeatsPerMeasure * secondsPerQuarterBeat * 1000f).toLong()
            val hasDifferentiatedVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }

            measure.notes
                .filter { !it.isRest && it.voice in melodicTrackSet }
                .sortedWith(compareBy({ it.position }, { it.midiNote }))
                .forEach { note ->
                    val shiftedMidi = (note.midiNote + transposition).coerceIn(21, 108)
                    val isRightHand = if (hasDifferentiatedVoices) note.voice == 0 else shiftedMidi >= 60
                    if ((isRightHand && isRightHandHidden) || (!isRightHand && isLeftHandHidden)) {
                        return@forEach
                    }

                    val noteStartMs = currentTotalMs + (note.position * secondsPerQuarterBeat * 1000f).toLong()
                    val noteDurMs = (note.duration.coerceAtLeast(0.125f) * secondsPerQuarterBeat * 1000f).toLong().coerceAtLeast(80L)

                    expectedNotes.add(
                        PlayAlongEvaluationNote(
                            id = noteIdCounter++,
                            midiNote = shiftedMidi,
                            measureIndex = mIdx,
                            beatPosition = note.position,
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
        activeStepTargetNotes.clear()
        liveKeyColors.clear()
        liveKeyFeedbacks.clear()
        updateScorecard()
    }

    /**
     * Returns true if playback clock should hold at `currentPlayheadMs` in `WAIT_FOR_NOTE` mode
     * until the user presses all pending notes in the current chord slice, and highlights target keys in Gold.
     */
    fun shouldHoldForStepWait(currentPlayheadMs: Long): Boolean {
        if (!isEvaluating.value || !waitForUserMode.value) {
            activeStepTargetNotes.clear()
            return false
        }

        val nextPending = expectedNotes.firstOrNull { it.status == NoteEvaluationStatus.PENDING } ?: run {
            activeStepTargetNotes.clear()
            return false
        }

        if (currentPlayheadMs >= nextPending.expectedTimestampMs - 25L) {
            // Collect all simultaneous chord notes within 35ms of this onset
            val chordSlice = expectedNotes.filter {
                it.status == NoteEvaluationStatus.PENDING &&
                    abs(it.expectedTimestampMs - nextPending.expectedTimestampMs) <= 35L
            }
            activeStepTargetNotes.clear()
            chordSlice.forEach { n ->
                activeStepTargetNotes.add(n.midiNote)
                if (!liveKeyFeedbacks.containsKey(n.midiNote)) {
                    liveKeyColors[n.midiNote] = Color(0xFFFFD700) // Gold target guide
                }
            }
            return true
        }
        return false
    }

    fun onUserMidiNoteOn(pressedNote: Int, currentPlayheadMs: Long) {
        if (!isEvaluating.value) return

        val toleranceWindowMs = if (waitForUserMode.value) 2_500L else 280L
        val matchingNote = expectedNotes.firstOrNull { note ->
            note.status == NoteEvaluationStatus.PENDING &&
                note.midiNote == pressedNote &&
                abs(note.expectedTimestampMs - currentPlayheadMs) <= toleranceWindowMs
        }

        if (matchingNote != null) {
            val delta = if (waitForUserMode.value) 0L else (currentPlayheadMs - matchingNote.expectedTimestampMs)
            matchingNote.hitTimestampMs = currentPlayheadMs
            matchingNote.timingDeltaMs = delta
            matchingNote.status = if (abs(delta) <= 80L) NoteEvaluationStatus.HIT_PERFECT else NoteEvaluationStatus.HIT_GOOD
            hitCount.intValue++
            streak.intValue++
            if (streak.intValue > bestStreak.intValue) {
                bestStreak.intValue = streak.intValue
            }
            timingErrors.add(abs(delta))
            activeStepTargetNotes.remove(pressedNote)

            val hitColor = if (matchingNote.status == NoteEvaluationStatus.HIT_PERFECT) Color(0xFF00E676) else Color(0xFF69F0AE)
            liveKeyColors[pressedNote] = hitColor
            liveKeyFeedbacks[pressedNote] = System.currentTimeMillis() + 420L
            com.creativeblisslabs.peqlearn.app.audio.WledKeyLightingEngine.sendKeyLightingEvent(pressedNote, hitColor, isNoteOn = true)
        } else {
            val anyExpectedNote = expectedNotes.firstOrNull { note ->
                note.status == NoteEvaluationStatus.PENDING &&
                    abs(note.expectedTimestampMs - currentPlayheadMs) <= toleranceWindowMs
            }
            if (anyExpectedNote != null) {
                wrongCount.intValue++
                streak.intValue = 0
                liveKeyColors[pressedNote] = Color(0xFFFF3D00)
                liveKeyFeedbacks[pressedNote] = System.currentTimeMillis() + 500L
            }
        }
        updateScorecard()
    }

    fun updatePlayheadPosition(currentPlayheadMs: Long) {
        if (!isEvaluating.value) return

        val now = System.currentTimeMillis()
        val iter = liveKeyFeedbacks.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (now >= entry.value) {
                liveKeyColors.remove(entry.key)
                iter.remove()
            }
        }

        // Do not mark notes as MISSED while holding for user input in Step-Wait mode
        if (waitForUserMode.value) return

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