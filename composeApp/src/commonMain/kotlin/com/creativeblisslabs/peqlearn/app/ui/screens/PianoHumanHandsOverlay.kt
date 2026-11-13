package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private data class HandFingerAssignment(
    val fingerNumber: Int, // 1 = Thumb, 2 = Index, 3 = Middle, 4 = Ring, 5 = Pinky
    val midiNote: Int,
    val isPressed: Boolean
)

// Anatomical resting offsets (in semitones) relative to Middle Finger (F3 = 0.0f)
// Left-to-Right on Keyboard:
//   LH: Pinky(5) [-3.5] -> Ring(4) [-1.8] -> Middle(3) [0.0] -> Index(2) [+1.8] -> Thumb(1) [+3.5]
//   RH: Thumb(1) [-3.5] -> Index(2) [-1.8] -> Middle(3) [0.0] -> Ring(4) [+1.8] -> Pinky(5) [+3.5]
private val LH_ANATOMICAL_REST_SEMITONES = floatArrayOf(0f, +3.5f, +1.8f, 0.0f, -1.8f, -3.5f)
private val RH_ANATOMICAL_REST_SEMITONES = floatArrayOf(0f, -3.5f, -1.8f, 0.0f, +1.8f, +3.5f)

internal fun resolvePlayableMelodicTrackIndices(
    score: Score?,
    enabledTrackIndices: Map<Int, Boolean> = emptyMap()
): Set<Int> {
    val tracks = score?.tracks
    if (tracks.isNullOrEmpty()) return (0..15).toSet()

    val nonDrumTracks = tracks.filter { tr ->
        val nameLow = tr.trackName.lowercase()
        val isDrumChannel = tr.channel == 9 || tr.channel == 10
        val isPercussionProg = tr.program in 112..119
        val isExplicitDrumName = nameLow.contains("drum") || nameLow.contains("percussion") || nameLow.contains("kit")
        !isDrumChannel && !isPercussionProg && !isExplicitDrumName &&
            (enabledTrackIndices[tr.trackIndex] ?: tr.isEnabled)
    }

    if (nonDrumTracks.isNotEmpty()) {
        return nonDrumTracks.map { it.trackIndex }.toSet()
    }
    return tracks.filter { it.channel != 9 }.map { it.trackIndex }.toSet()
}

internal fun formatCleanHandOverlayDiagnostic(
    midiNote: Int,
    fingerNumber: Int,
    keySignature: String = "C"
): String {
    val isFlatKey = keySignature.contains("FLAT", true) || keySignature in listOf("Db", "Bb", "Eb", "Ab", "F", "Gb", "Cb")
    val pitchNames = if (isFlatKey) {
        arrayOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")
    } else {
        arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    }
    val octave = (midiNote / 12) - 1
    val pitchName = "${pitchNames[(midiNote % 12 + 12) % 12]}$octave"
    return "$pitchName→F${fingerNumber.coerceIn(1, 5)}"
}

private fun clampChordToAnatomicalHand(
    sortedAsc: List<Int>,
    isRightHand: Boolean,
    maxSpanSemitones: Int = 12
): List<Int> {
    if (sortedAsc.isEmpty()) return emptyList()
    val distinctAsc = sortedAsc.distinct().sorted()

    val spanFiltered = if (distinctAsc.last() - distinctAsc.first() > maxSpanSemitones) {
        if (isRightHand) {
            val top = distinctAsc.last()
            distinctAsc.filter { top - it <= maxSpanSemitones }
        } else {
            val bot = distinctAsc.first()
            distinctAsc.filter { it - bot <= maxSpanSemitones }
        }
    } else {
        distinctAsc
    }

    if (spanFiltered.size <= 5) return spanFiltered

    val lowest = spanFiltered.first()
    val highest = spanFiltered.last()
    val outerPitchClasses = setOf(lowest % 12, highest % 12)
    val fifthPitchClass = (lowest + 7) % 12

    val keptInner = spanFiltered.subList(1, spanFiltered.lastIndex)
        .sortedByDescending { pitch ->
            val pc = pitch % 12
            val uniqueBonus = if (pc !in outerPitchClasses) 10 else 0
            val guideToneBonus = if (pc != fifthPitchClass) 4 else 1
            uniqueBonus + guideToneBonus
        }
        .take(3)

    return (listOf(lowest, highest) + keptInner).distinct().sorted()
}

private data class HandPartitionResult(
    val lhPitchesAsc: List<Int>,
    val rhPitchesAsc: List<Int>,
    val explicitFingers: Map<Int, Int>,
    val lookaheadLhPitch: Int?,
    val lookaheadRhPitch: Int?,
    val melodicTracks: Set<Int>
)

private fun partitionPianoNotesStrictDisjoint(
    activeMidiNotes: Map<Int, Boolean>,
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    activeNoteColors: Map<Int, Color>,
    leftHandColor: Color,
    rightHandColor: Color,
    showLeftHand: Boolean,
    showRightHand: Boolean,
    lastLhCenter: Int,
    lastRhCenter: Int,
    transposition: Int = 0
): HandPartitionResult {
    val melodicTrackSet = resolvePlayableMelodicTrackIndices(score)
    val currentMeasure = score?.measures?.getOrNull(playheadMeasure)
    val explicitFingers = mutableMapOf<Int, Int>()
    val validScorePitchesAtBeat = mutableSetOf<Int>()
    val drumPitchesAtBeat = mutableSetOf<Int>()
    var lookaheadLhPitch: Int? = null
    var lookaheadRhPitch: Int? = null

    if (currentMeasure != null) {
        val beatsPerMeasure = (currentMeasure.timeSignatureNumerator.toFloat() *
            (4f / currentMeasure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
        val curBeat = playheadOffset * beatsPerMeasure

        val sortedByProximity = currentMeasure.notes
            .filter { !it.isRest }
            .sortedBy { abs(it.position - curBeat) }

        for (n in sortedByProximity) {
            val isActiveAtBeat = curBeat >= (n.position - 0.16f) &&
                curBeat <= (n.position + n.duration.coerceAtLeast(0.125f) + 0.12f)
            val transposedDrumPitch = (n.midiNote + transposition).coerceIn(21, 108)
            if (n.voice !in melodicTrackSet) {
                if (isActiveAtBeat) drumPitchesAtBeat.add(transposedDrumPitch)
                continue
            }
            val transposedPitch = transposedDrumPitch
            if (isActiveAtBeat) {
                validScorePitchesAtBeat.add(transposedPitch)
            } else if (n.position > curBeat && n.position <= curBeat + 1.25f) {
                // Capture immediate upcoming note for thumb-crossing preparation
                if (transposedPitch < 60 && lookaheadLhPitch == null) lookaheadLhPitch = transposedPitch
                if (transposedPitch >= 60 && lookaheadRhPitch == null) lookaheadRhPitch = transposedPitch
            }
            if (n.fingering != null && n.fingering in 1..5 && !explicitFingers.containsKey(transposedPitch)) {
                explicitFingers[transposedPitch] = n.fingering
            }
        }
    }

    val soundingPitches = activeMidiNotes.entries
        .filter { (pitch, isOn) ->
            isOn && pitch in 21..108 && (pitch in validScorePitchesAtBeat || pitch !in drumPitchesAtBeat)
        }
        .map { it.key }
        .distinct()
        .sorted()

    if (soundingPitches.isEmpty()) {
        return HandPartitionResult(
            lhPitchesAsc = emptyList(),
            rhPitchesAsc = emptyList(),
            explicitFingers = explicitFingers,
            lookaheadLhPitch = lookaheadLhPitch,
            lookaheadRhPitch = lookaheadRhPitch,
            melodicTracks = melodicTrackSet
        )
    }

    if (!showLeftHand && showRightHand) {
        return HandPartitionResult(
            lhPitchesAsc = emptyList(),
            rhPitchesAsc = clampChordToAnatomicalHand(soundingPitches, isRightHand = true, maxSpanSemitones = 12),
            explicitFingers = explicitFingers,
            lookaheadLhPitch = lookaheadLhPitch,
            lookaheadRhPitch = lookaheadRhPitch,
            melodicTracks = melodicTrackSet
        )
    }
    if (!showRightHand && showLeftHand) {
        return HandPartitionResult(
            lhPitchesAsc = clampChordToAnatomicalHand(soundingPitches, isRightHand = false, maxSpanSemitones = 12),
            rhPitchesAsc = emptyList(),
            explicitFingers = explicitFingers,
            lookaheadLhPitch = lookaheadLhPitch,
            lookaheadRhPitch = lookaheadRhPitch,
            melodicTracks = melodicTrackSet
        )
    }

    val n = soundingPitches.size
    var bestSplitIndex = 0
    var bestCost = Float.MAX_VALUE

    for (k in 0..n) {
        val candidateLh = soundingPitches.subList(0, k)
        val candidateRh = soundingPitches.subList(k, n)

        val lhSpan = if (candidateLh.size > 1) candidateLh.last() - candidateLh.first() else 0
        val rhSpan = if (candidateRh.size > 1) candidateRh.last() - candidateRh.first() else 0

        var cost = 0f

        if (lhSpan > 12) cost += (lhSpan - 12) * 75f
        if (lhSpan > 13) cost += (lhSpan - 13) * 420f
        if (rhSpan > 12) cost += (rhSpan - 12) * 75f
        if (rhSpan > 13) cost += (rhSpan - 13) * 420f

        if (candidateLh.size > 5) cost += (candidateLh.size - 5) * 95f
        if (candidateRh.size > 5) cost += (candidateRh.size - 5) * 95f

        if (candidateLh.isNotEmpty()) {
            val lhCenter = (candidateLh.first() + candidateLh.last()) * 0.5f
            cost += abs(lhCenter - lastLhCenter) * 1.35f
            for (p in candidateLh) {
                if (p >= 62) cost += (p - 60) * 16f
                if (activeNoteColors[p] == rightHandColor && p >= 57) cost += 14f
            }
        }

        if (candidateRh.isNotEmpty()) {
            val rhCenter = (candidateRh.first() + candidateRh.last()) * 0.5f
            cost += abs(rhCenter - lastRhCenter) * 1.35f
            for (p in candidateRh) {
                if (p <= 55) cost += (56 - p) * 35f
                if (activeNoteColors[p] == leftHandColor && p <= 60) cost += 40f
            }
        }

        if (candidateLh.isNotEmpty() && candidateRh.isNotEmpty()) {
            val gap = candidateRh.first() - candidateLh.last()
            cost -= gap.coerceAtMost(18) * 3.6f
        }

        if (cost < bestCost) {
            bestCost = cost
            bestSplitIndex = k
        }
    }

    val rawLh = soundingPitches.subList(0, bestSplitIndex)
    val rawRh = soundingPitches.subList(bestSplitIndex, n)

    val clampedLh = clampChordToAnatomicalHand(rawLh, isRightHand = false, maxSpanSemitones = 12).toMutableList()
    val clampedRh = clampChordToAnatomicalHand(rawRh, isRightHand = true, maxSpanSemitones = 12).toMutableList()

    if (clampedLh.isEmpty() && rawRh.size > clampedRh.size) {
        val droppedFromRh = rawRh.filter { it !in clampedRh && it < (clampedRh.firstOrNull() ?: 60) }
        clampedLh.addAll(clampChordToAnatomicalHand(droppedFromRh, isRightHand = false, maxSpanSemitones = 12))
    } else if (clampedRh.isEmpty() && rawLh.size > clampedLh.size) {
        val droppedFromLh = rawLh.filter { it !in clampedLh && it > (clampedLh.lastOrNull() ?: 60) }
        clampedRh.addAll(clampChordToAnatomicalHand(droppedFromLh, isRightHand = true, maxSpanSemitones = 12))
    }

    return HandPartitionResult(
        lhPitchesAsc = clampedLh.distinct().sorted(),
        rhPitchesAsc = clampedRh.distinct().sorted(),
        explicitFingers = explicitFingers,
        lookaheadLhPitch = lookaheadLhPitch,
        lookaheadRhPitch = lookaheadRhPitch,
        melodicTracks = melodicTrackSet
    )
}

/**
 * Biomechanical Single-Note & Chord Finger Solver:
 *  - Fixes LH/RH Octave Spanning: Any interval >= 8 semitones (Major 6th, 7th, Octave) ALWAYS uses
 *    Pinky(5)-to-Thumb(1) for Left Hand `[5, 1]` and Thumb(1)-to-Pinky(5) for Right Hand `[1, 5]`,
 *    NEVER shifting thumb to index finger on black-key octaves!
 *  - Fixes Scale Thumb-Under & Finger-Over Crossing: Uses genuine crossover rules (`3 -> 1`, `4 -> 1`,
 *    `1 -> 3`, `1 -> 4`) instead of clamping with `.coerceIn(1, 5)`.
 *  - Tracks anatomical Palm Center (Middle Finger F3) so single notes played by Thumb(1) or Pinky(5)
 *    don't yank the wrist horizontally to the outer finger!
 */
private data class HandSolvedState(
    val palmCenterMidi: Float,
    val lastMelodicPitch: Int,
    val lastMelodicFinger: Int,
    val assignments: List<HandFingerAssignment>
)

private fun solveSingleNoteWithThumbCrossing(
    pitch: Int,
    isRightHand: Boolean,
    lastPalmMidi: Float,
    lastPitch: Int,
    lastFinger: Int,
    lookaheadPitch: Int?
): Int {
    val delta = pitch - lastPitch
    if (delta == 0 && lastFinger in 1..5) return lastFinger

    val isBlack = isMidiNoteBlack(pitch)
    val restOffsets = if (isRightHand) RH_ANATOMICAL_REST_SEMITONES else LH_ANATOMICAL_REST_SEMITONES

    // Wide leap (>= 7 semitones): land on anatomically natural outer finger (5 or 1)
    if (abs(delta) >= 7) {
        return if (isRightHand) {
            if (delta > 0) 5 else 1
        } else {
            if (delta > 0) 1 else 5
        }
    }

    var bestFinger = 3
    var bestCost = Float.MAX_VALUE

    for (f in 1..5) {
        var cost = 0f

        // 1. Proximity to current resting finger slot
        val fingerNeutralPitch = lastPalmMidi + restOffsets[f]
        cost += abs(pitch - fingerNeutralPitch) * 1.9f

        // 2. Avoid repeating the exact same finger or 1-2 finger hopping on different pitches
        if (f == lastFinger && delta != 0) {
            cost += 75f
        }
        if (delta != 0 && ((lastFinger == 2 && f == 1) || (lastFinger == 1 && f == 2))) {
            // Heavily penalize unnatural 1-2-1-2 repetitive finger hopping on melodic runs!
            cost += 65f
        }

        // 3. Directional stepping & Thumb-Under / Finger-Over Crossover Mechanics
        // In RH: ascending pitch (+delta) normally increases finger number (1 -> 2 -> 3 -> 4 -> 5)
        // In LH: ascending pitch (+delta) normally decreases finger number (5 -> 4 -> 3 -> 2 -> 1)
        val anatomicalStep = if (isRightHand) (f - lastFinger) else (lastFinger - f)

        if (delta > 0) {
            // Ascending pitch (+delta)
            if (isRightHand) {
                if (anatomicalStep in 1..2) {
                    cost -= 28f // Strong anatomical bonus for sequential finger stepping (1->2->3->4->5)
                } else if (f == 1 && lastFinger in 2..4 && !isBlack && delta in 1..4) {
                    // RH Ascending Thumb-Under Crossover (e.g. 1-2-3 -> 1-2-3-4-5)
                    val continuesUp = lookaheadPitch != null && lookaheadPitch > pitch
                    cost += if (continuesUp || lastFinger >= 3) -15f else 5.0f
                } else {
                    cost += 55f
                }
            } else {
                // LH Ascending (5 -> 4 -> 3 -> 2 -> 1, then Finger-Over 1 -> 3 or 1 -> 4!)
                if (anatomicalStep in 1..2) {
                    cost -= 28f // Strong bonus for sequential finger stepping
                } else if (lastFinger == 1 && f in 2..4 && delta in 1..4) {
                    // LH Ascending Finger-Over Crossover (Thumb 1 -> Middle 3 or Ring 4)
                    cost -= 15f
                } else {
                    cost += 55f
                }
            }
        } else {
            // Descending pitch (delta < 0)
            if (isRightHand) {
                // RH Descending (5 -> 4 -> 3 -> 2 -> 1, then Finger-Over 1 -> 3 or 1 -> 4!)
                if (anatomicalStep in -2..-1) {
                    cost -= 28f // Strong bonus for sequential finger stepping
                } else if (lastFinger == 1 && f in 2..4 && -delta in 1..4) {
                    // RH Descending Finger-Over Crossover (Thumb 1 -> Middle 3 or Ring 4)
                    cost -= 15f
                } else {
                    cost += 55f
                }
            } else {
                // LH Descending (1 -> 2 -> 3 -> 4 -> 5, or Thumb-Under 3 -> 1 / 4 -> 1!)
                if (anatomicalStep in -2..-1) {
                    cost -= 28f // Strong bonus for sequential finger stepping
                } else if (f == 1 && lastFinger in 2..4 && !isBlack && -delta in 1..4) {
                    // LH Descending Thumb-Under Crossover (3 -> 1 or 4 -> 1)
                    val continuesDown = lookaheadPitch != null && lookaheadPitch < pitch
                    cost += if (continuesDown || lastFinger >= 3) -15f else 5.0f
                } else {
                    cost += 55f
                }
            }
        }

        // 4. Black-key thumb avoidance on single notes
        if (isBlack && f == 1) cost += 18f

        // 5. Lookahead dead-end prevention: don't land on outer finger 5/1 if scale continues in same direction
        if (lookaheadPitch != null && lookaheadPitch != pitch) {
            val nextDelta = lookaheadPitch - pitch
            if (isRightHand) {
                if (nextDelta > 0 && f == 5) cost += 14f // Leave room to ascend or trigger thumb-under earlier
                if (nextDelta < 0 && f == 1 && delta > 0) cost -= 2f
            } else {
                if (nextDelta < 0 && f == 5) cost += 14f
                if (nextDelta > 0 && f == 1 && delta < 0) cost -= 2f
            }
        }

        if (cost < bestCost) {
            bestCost = cost
            bestFinger = f
        }
    }

    return bestFinger
}

private fun solveHumanChordAndMelodyFingers(
    activePitchesAsc: List<Int>,
    explicitFingerMap: Map<Int, Int>,
    isRightHand: Boolean,
    lastPalmMidi: Float,
    lastMelodicPitch: Int,
    lastMelodicFinger: Int,
    territoryLimitMidi: Int,
    lookaheadPitch: Int?
): HandSolvedState {
    val minAllowedMidi = if (isRightHand) territoryLimitMidi.coerceIn(21, 96) else 21
    val maxAllowedMidi = if (isRightHand) 108 else territoryLimitMidi.coerceIn(32, 108)
    val restOffsets = if (isRightHand) RH_ANATOMICAL_REST_SEMITONES else LH_ANATOMICAL_REST_SEMITONES

    val safePalm = lastPalmMidi.coerceIn(
        minOf(minAllowedMidi + 3f, maxAllowedMidi.toFloat()),
        maxOf(minAllowedMidi.toFloat(), maxAllowedMidi - 3f)
    )

    if (activePitchesAsc.isEmpty()) {
        val idleFingers = (1..5).map { fNum ->
            val restMidi = (safePalm + restOffsets[fNum]).roundToInt().coerceIn(minAllowedMidi, maxAllowedMidi)
            HandFingerAssignment(
                fingerNumber = fNum,
                midiNote = restMidi,
                isPressed = false
            )
        }
        return HandSolvedState(
            palmCenterMidi = safePalm,
            lastMelodicPitch = lastMelodicPitch,
            lastMelodicFinger = lastMelodicFinger,
            assignments = idleFingers
        )
    }

    val sorted = clampChordToAnatomicalHand(activePitchesAsc, isRightHand, maxSpanSemitones = 12)
    val count = sorted.size
    val span = (sorted.last() - sorted.first()).coerceAtLeast(0)

    // CRITICAL FIX: Only shift thumb off a black key for narrow intervals (span <= 6)!
    // Wide intervals (7ths & Octaves, span >= 7) MUST use Thumb(1) and Pinky(5)!
    val rhThumbIsBlackNarrow = isRightHand && span <= 6 && isMidiNoteBlack(sorted.first())
    val lhThumbIsBlackNarrow = !isRightHand && span <= 6 && isMidiNoteBlack(sorted.last())

    fun adjustRhNarrowChord(base: List<Int>): List<Int> {
        if (!rhThumbIsBlackNarrow || base.size >= 5 || base.contains(5)) return base
        val shifted = base.map { (it + 1).coerceAtMost(5) }.distinct()
        return if (shifted.size == base.size) shifted else base
    }

    fun adjustLhNarrowChord(base: List<Int>): List<Int> {
        if (!lhThumbIsBlackNarrow || base.size >= 5 || base.firstOrNull() == 5) return base
        val shifted = base.mapIndexed { idx, f -> if (idx == base.lastIndex && f == 1) 2 else f }.distinct()
        return if (shifted.size == base.size) shifted else base
    }

    // `fingersLeftToRight` maps `sorted[idx]` (ascending pitch from left to right on keyboard) to finger 1..5
    val fingersLeftToRight: List<Int> = if (isRightHand) {
        // Right Hand: Left-to-right on keyboard is Thumb(1) -> Index(2) -> Middle(3) -> Ring(4) -> Pinky(5)
        when (count) {
            1 -> {
                val n = sorted[0]
                val exp = explicitFingerMap[n]?.takeIf { it in 1..5 }
                val f = exp ?: solveSingleNoteWithThumbCrossing(
                    pitch = n,
                    isRightHand = true,
                    lastPalmMidi = safePalm,
                    lastPitch = lastMelodicPitch,
                    lastFinger = lastMelodicFinger,
                    lookaheadPitch = lookaheadPitch
                )
                listOf(f)
            }
            2 -> when {
                span >= 8 -> listOf(1, 5) // 6th, 7th, Octave: strictly Thumb(1) to Pinky(5)
                span >= 6 -> adjustRhNarrowChord(listOf(1, 4))
                span >= 4 -> adjustRhNarrowChord(listOf(1, 3))
                else -> adjustRhNarrowChord(listOf(1, 2))
            }
            3 -> {
                val lowGap = sorted[1] - sorted[0]
                val highGap = sorted[2] - sorted[1]
                when {
                    span >= 9 -> if (lowGap >= highGap) listOf(1, 3, 5) else listOf(1, 2, 5) // Octave triad
                    span <= 4 -> adjustRhNarrowChord(listOf(1, 2, 3))
                    lowGap <= 3 && highGap >= 5 -> listOf(1, 2, 5)
                    lowGap >= 5 && highGap <= 3 -> listOf(1, 4, 5)
                    else -> listOf(1, 3, 5)
                }
            }
            4 -> {
                val topGap = sorted[3] - sorted[2]
                if (topGap <= 2) listOf(1, 2, 4, 5) else listOf(1, 2, 3, 5)
            }
            else -> listOf(1, 2, 3, 4, 5)
        }
    } else {
        // Left Hand: Left-to-right on keyboard is Pinky(5) -> Ring(4) -> Middle(3) -> Index(2) -> Thumb(1)
        when (count) {
            1 -> {
                val n = sorted[0]
                val exp = explicitFingerMap[n]?.takeIf { it in 1..5 }
                val f = exp ?: solveSingleNoteWithThumbCrossing(
                    pitch = n,
                    isRightHand = false,
                    lastPalmMidi = safePalm,
                    lastPitch = lastMelodicPitch,
                    lastFinger = lastMelodicFinger,
                    lookaheadPitch = lookaheadPitch
                )
                listOf(f)
            }
            2 -> when {
                span >= 8 -> listOf(5, 1) // 6th, 7th, Octave: strictly Pinky(5) to Thumb(1)
                span >= 6 -> adjustLhNarrowChord(listOf(4, 1))
                span >= 4 -> adjustLhNarrowChord(listOf(3, 1))
                else -> adjustLhNarrowChord(listOf(2, 1))
            }
            3 -> {
                val lowGap = sorted[1] - sorted[0]
                val highGap = sorted[2] - sorted[1]
                when {
                    span >= 9 -> if (lowGap >= highGap) listOf(5, 2, 1) else listOf(5, 3, 1) // Octave shell + 5th/3rd
                    span <= 4 -> adjustLhNarrowChord(listOf(3, 2, 1))
                    lowGap >= 5 -> listOf(5, 2, 1)
                    highGap >= 5 -> listOf(5, 4, 1)
                    else -> listOf(5, 3, 1)
                }
            }
            4 -> {
                val lowGap = sorted[1] - sorted[0]
                if (lowGap <= 2) listOf(5, 4, 2, 1) else listOf(5, 3, 2, 1)
            }
            else -> listOf(5, 4, 3, 2, 1)
        }
    }

    val pressedByFinger = mutableMapOf<Int, Int>()
    sorted.forEachIndexed { idx, pitch ->
        val fNum = fingersLeftToRight.getOrElse(idx) { if (isRightHand) idx + 1 else 5 - idx }.coerceIn(1, 5)
        pressedByFinger[fNum] = pitch
    }

    // Implied Palm Center (Middle Finger F3 position) averaged across all pressed fingers
    // This prevents playing a single note with Thumb(1) or Pinky(5) from jerking the entire palm center!
    val impliedPalmCenters = pressedByFinger.map { (fNum, pitch) ->
        pitch - restOffsets[fNum]
    }
    val rawTargetPalm = if (impliedPalmCenters.isNotEmpty()) {
        impliedPalmCenters.average().toFloat()
    } else {
        safePalm
    }

    // Lookahead wrist pre-shift during single-note scale thumb crossings so the wrist glides smoothly
    val lookaheadAdjustedPalm = if (count == 1 && lookaheadPitch != null) {
        val curF = fingersLeftToRight.first()
        val curP = sorted.first()
        val deltaNext = lookaheadPitch - curP
        val isCrossingPrep = (isRightHand && ((curF in 3..4 && deltaNext in 1..4) || (curF == 1 && deltaNext in -4..-1))) ||
            (!isRightHand && ((curF in 3..4 && deltaNext in -4..-1) || (curF == 1 && deltaNext in 1..4)))
        if (isCrossingPrep) rawTargetPalm * 0.72f + lookaheadPitch * 0.28f else rawTargetPalm
    } else {
        rawTargetPalm
    }.coerceIn(24f, 104f)

    // Stretch factor for unpressed fingers when spanning an octave
    val handStretchFactor = if (count >= 2) {
        val fFirst = fingersLeftToRight.first()
        val fLast = fingersLeftToRight.last()
        val naturalRestSpan = abs(restOffsets[fLast] - restOffsets[fFirst]).coerceAtLeast(1.8f)
        (span.toFloat() / naturalRestSpan).coerceIn(0.85f, 1.65f)
    } else {
        1.0f
    }

    val allFiveFingers = (1..5).map { fNum ->
        val pressedPitch = pressedByFinger[fNum]
        if (pressedPitch != null) {
            HandFingerAssignment(fingerNumber = fNum, midiNote = pressedPitch, isPressed = true)
        } else {
            val lowerPressed = pressedByFinger.keys.filter { it < fNum }.maxOrNull()
            val higherPressed = pressedByFinger.keys.filter { it > fNum }.minOrNull()

            val naturalPitch = when {
                lowerPressed != null && higherPressed != null -> {
                    val pLow = pressedByFinger[lowerPressed]!!
                    val pHigh = pressedByFinger[higherPressed]!!
                    val frac = (fNum - lowerPressed).toFloat() / (higherPressed - lowerPressed).toFloat()
                    (pLow + (pHigh - pLow) * frac).roundToInt()
                }
                else -> {
                    (lookaheadAdjustedPalm + restOffsets[fNum] * handStretchFactor).roundToInt()
                }
            }
            HandFingerAssignment(
                fingerNumber = fNum,
                midiNote = naturalPitch.coerceIn(minAllowedMidi, maxAllowedMidi),
                isPressed = false
            )
        }
    }

    val newMelodicPitch = if (isRightHand) sorted.last() else sorted.first()
    val newMelodicFinger = if (isRightHand) fingersLeftToRight.last() else fingersLeftToRight.first()

    return HandSolvedState(
        palmCenterMidi = lookaheadAdjustedPalm,
        lastMelodicPitch = newMelodicPitch,
        lastMelodicFinger = newMelodicFinger,
        assignments = allFiveFingers
    )
}

private fun computeRepetitiveStrikePulse(
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    melodicTracks: Set<Int>
): Float {
    val measure = score?.measures?.getOrNull(playheadMeasure) ?: return 1f
    val beatsPerMeasure = (measure.timeSignatureNumerator.toFloat() *
        (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
    val curBeat = playheadOffset * beatsPerMeasure

    val activeAtBeat = measure.notes.filter { n ->
        !n.isRest && (melodicTracks.isEmpty() || n.voice in melodicTracks) &&
            curBeat >= n.position - 0.04f && curBeat <= n.position + n.duration.coerceAtLeast(0.125f)
    }
    if (activeAtBeat.isEmpty()) return 0.35f

    val latestOnset = activeAtBeat.maxOf { it.position }
    val nearestNoteDur = activeAtBeat.filter { abs(it.position - latestOnset) < 0.02f }
        .minOfOrNull { it.duration.coerceAtLeast(0.125f) } ?: 0.38f

    val elapsedInNote = (curBeat - latestOnset).coerceAtLeast(0f)
    val phase = (elapsedInNote / nearestNoteDur).coerceIn(0f, 1f)

    return when {
        phase < 0.12f -> (phase / 0.12f).coerceIn(0.30f, 1f)
        phase > 0.85f -> (1f - ((phase - 0.85f) / 0.15f) * 0.65f).coerceIn(0.30f, 1f)
        else -> 1f
    }
}

@Composable
fun PianoHumanHandsOverlay(
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    activeMidiNotes: Map<Int, Boolean>,
    activeNoteColors: Map<Int, Color>,
    startNote: Int = 21,
    endNote: Int = 108,
    showLeftHand: Boolean = true,
    showRightHand: Boolean = true,
    leftHandColor: Color = Color(0xFF6C63FF),
    rightHandColor: Color = Color(0xFF4A90E2),
    transposition: Int = 0,
    modifier: Modifier = Modifier
) {
    if (!showLeftHand && !showRightHand) return

    val safeStart = minOf(startNote, endNote).coerceIn(21, 108)
    val safeEnd = maxOf(startNote, endNote).coerceIn(21, 108)
    val whiteNotesInRange = remember(safeStart, safeEnd) {
        (safeStart..safeEnd).filter { !isMidiNoteBlack(it) }
    }
    val whiteCount = whiteNotesInRange.size.coerceAtLeast(1)

    // Persistent biomechanical hand state across frames
    val lastLhPalmRef = remember { mutableFloatStateOf(48f) } // C3 default LH palm center
    val lastRhPalmRef = remember { mutableFloatStateOf(67f) } // G4 default RH palm center
    val lastLhPitchRef = remember { mutableIntStateOf(48) }
    val lastRhPitchRef = remember { mutableIntStateOf(67) }
    val lastLhFingerRef = remember { mutableIntStateOf(3) }
    val lastRhFingerRef = remember { mutableIntStateOf(3) }

    val activeNotesSnapshot = activeMidiNotes.toMap()
    val activeColorsSnapshot = activeNoteColors.toMap()
    val activeNotesSignature = activeNotesSnapshot.entries
        .filter { it.value }
        .map { it.key }
        .sorted()

    val partitioned = remember(
        score,
        playheadMeasure,
        playheadOffset,
        activeNotesSignature,
        activeColorsSnapshot,
        showLeftHand,
        showRightHand,
        transposition
    ) {
        partitionPianoNotesStrictDisjoint(
            activeMidiNotes = activeNotesSnapshot,
            score = score,
            playheadMeasure = playheadMeasure,
            playheadOffset = playheadOffset,
            activeNoteColors = activeColorsSnapshot,
            leftHandColor = leftHandColor,
            rightHandColor = rightHandColor,
            showLeftHand = showLeftHand,
            showRightHand = showRightHand,
            lastLhCenter = lastLhPalmRef.floatValue.roundToInt(),
            lastRhCenter = lastRhPalmRef.floatValue.roundToInt(),
            transposition = transposition
        )
    }

    val disjointLh = partitioned.lhPitchesAsc
    val disjointRh = partitioned.rhPitchesAsc
    val explicitFingers = partitioned.explicitFingers

    val rhLowestBound = disjointRh.firstOrNull() ?: (lastRhPalmRef.floatValue.roundToInt() - 4).coerceAtLeast(60)
    val lhHighestBound = disjointLh.lastOrNull() ?: (lastLhPalmRef.floatValue.roundToInt() + 4).coerceAtMost(59)
    val lhTerritoryCeiling = if (showRightHand) (rhLowestBound - 1).coerceAtLeast(38) else 108
    val rhTerritoryFloor = if (showLeftHand) (lhHighestBound + 1).coerceAtMost(84) else 21

    val lhSolution = remember(disjointLh, explicitFingers, lhTerritoryCeiling, partitioned.lookaheadLhPitch) {
        solveHumanChordAndMelodyFingers(
            activePitchesAsc = disjointLh,
            explicitFingerMap = explicitFingers,
            isRightHand = false,
            lastPalmMidi = lastLhPalmRef.floatValue,
            lastMelodicPitch = lastLhPitchRef.intValue,
            lastMelodicFinger = lastLhFingerRef.intValue,
            territoryLimitMidi = lhTerritoryCeiling,
            lookaheadPitch = partitioned.lookaheadLhPitch
        ).also { solved ->
            if (disjointLh.isNotEmpty()) {
                lastLhPalmRef.floatValue = solved.palmCenterMidi.coerceAtMost(lhTerritoryCeiling.toFloat())
                lastLhPitchRef.intValue = solved.lastMelodicPitch
                lastLhFingerRef.intValue = solved.lastMelodicFinger
            }
        }
    }

    val rhSolution = remember(disjointRh, explicitFingers, rhTerritoryFloor, partitioned.lookaheadRhPitch) {
        solveHumanChordAndMelodyFingers(
            activePitchesAsc = disjointRh,
            explicitFingerMap = explicitFingers,
            isRightHand = true,
            lastPalmMidi = lastRhPalmRef.floatValue,
            lastMelodicPitch = lastRhPitchRef.intValue,
            lastMelodicFinger = lastRhFingerRef.intValue,
            territoryLimitMidi = rhTerritoryFloor,
            lookaheadPitch = partitioned.lookaheadRhPitch
        ).also { solved ->
            if (disjointRh.isNotEmpty()) {
                lastRhPalmRef.floatValue = solved.palmCenterMidi.coerceAtLeast(rhTerritoryFloor.toFloat())
                lastRhPitchRef.intValue = solved.lastMelodicPitch
                lastRhFingerRef.intValue = solved.lastMelodicFinger
            }
        }
    }

    // Critically-damped horizontal wrist glide (no aggressive snapping or overshoot!)
    val targetLhPalmMidi = lhSolution.palmCenterMidi.coerceAtMost(lhTerritoryCeiling.toFloat())
    val targetRhPalmMidi = rhSolution.palmCenterMidi.coerceAtLeast(rhTerritoryFloor.toFloat())

    val animatedLhPalmMidi by animateFloatAsState(
        targetValue = targetLhPalmMidi,
        animationSpec = spring(dampingRatio = 0.96f, stiffness = 360f),
        label = "lh_palm_glide"
    )

    val animatedRhPalmMidi by animateFloatAsState(
        targetValue = targetRhPalmMidi,
        animationSpec = spring(dampingRatio = 0.96f, stiffness = 360f),
        label = "rh_palm_glide"
    )

    // Subtle, anatomically realistic wrist tilt in degrees (strictly clamped to [-8.5°, +8.5°])
    val targetLhTiltDeg = remember(disjointLh, targetLhPalmMidi, animatedLhPalmMidi) {
        val velocityTilt = ((targetLhPalmMidi - animatedLhPalmMidi) * 0.85f).coerceIn(-5.5f, 5.5f)
        val thumbUnderBias = if (disjointLh.size == 1 && lhSolution.lastMelodicFinger == 1) -2.2f else 0f
        // Natural slight inward ulnar angle for Left Hand (-2.0°)
        (-2.0f + velocityTilt + thumbUnderBias).coerceIn(-8.5f, 8.5f)
    }

    val targetRhTiltDeg = remember(disjointRh, targetRhPalmMidi, animatedRhPalmMidi) {
        val velocityTilt = ((targetRhPalmMidi - animatedRhPalmMidi) * 0.85f).coerceIn(-5.5f, 5.5f)
        val thumbUnderBias = if (disjointRh.size == 1 && rhSolution.lastMelodicFinger == 1) +2.2f else 0f
        // Natural slight inward ulnar angle for Right Hand (+2.0°)
        (+2.0f + velocityTilt + thumbUnderBias).coerceIn(-8.5f, 8.5f)
    }

    val animatedLhTiltDeg by animateFloatAsState(
        targetValue = targetLhTiltDeg,
        animationSpec = spring(dampingRatio = 0.94f, stiffness = 280f),
        label = "lh_wrist_tilt"
    )

    val animatedRhTiltDeg by animateFloatAsState(
        targetValue = targetRhTiltDeg,
        animationSpec = spring(dampingRatio = 0.94f, stiffness = 280f),
        label = "rh_wrist_tilt"
    )

    // Compute strike pulse ONCE per frame (instead of 10 times inside the finger loop)
    val strikePulse = remember(score, playheadMeasure, playheadOffset, partitioned.melodicTracks) {
        computeRepetitiveStrikePulse(score, playheadMeasure, playheadOffset, partitioned.melodicTracks)
    }

    val fingerBadgePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }
    val wristBadgePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val totalW = size.width
        val totalH = size.height
        val whiteKeyW = totalW / whiteCount.toFloat()
        val blackKeyH = totalH * 0.60f

        fun midiToExactX(midi: Int): Float {
            val clamped = midi.coerceIn(safeStart, safeEnd)
            val startGeom = WaterfallKeyGeometryLookup.keyGeometries[safeStart]
            return if (!isMidiNoteBlack(clamped)) {
                val geom = WaterfallKeyGeometryLookup.keyGeometries[clamped]
                val relWhiteIdx = geom.whiteKeyIndex - startGeom.whiteKeyIndex
                (relWhiteIdx + 0.5f) * whiteKeyW
            } else {
                val lowerWhiteMidi = (clamped - 1).coerceAtLeast(safeStart)
                val lowerGeom = WaterfallKeyGeometryLookup.keyGeometries[lowerWhiteMidi]
                val relWhiteIdx = lowerGeom.whiteKeyIndex - startGeom.whiteKeyIndex
                val pc = (clamped % 12 + 12) % 12
                val seamBias = when (pc) {
                    1 -> -0.04f
                    3 -> 0.04f
                    6 -> -0.05f
                    8 -> 0.0f
                    10 -> 0.05f
                    else -> 0f
                }
                (relWhiteIdx + 1.0f + seamBias) * whiteKeyW
            }
        }

        fun interpolateMidiX(fractionalMidi: Float): Float {
            val fl = fractionalMidi.toInt().coerceIn(safeStart, safeEnd)
            val cl = (fl + 1).coerceIn(safeStart, safeEnd)
            val frac = (fractionalMidi - fl).coerceIn(0f, 1f)
            return midiToExactX(fl) * (1f - frac) + midiToExactX(cl) * frac
        }

        val territoryMidMidi = ((lhTerritoryCeiling + rhTerritoryFloor) * 0.5f).coerceIn(40f, 80f)
        val territoryBorderX = interpolateMidiX(territoryMidMidi)

        fun drawOrganicHumanHand(
            assignments: List<HandFingerAssignment>,
            animatedPalmMidi: Float,
            wristTiltDeg: Float,
            handAccentColor: Color,
            isRightHand: Boolean
        ) {
            // Left-to-right anatomical finger order on keyboard:
            //   RH: 1(Thumb) -> 2(Index) -> 3(Middle) -> 4(Ring) -> 5(Pinky)
            //   LH: 5(Pinky) -> 4(Ring) -> 3(Middle) -> 2(Index) -> 1(Thumb)
            val orderedLeftToRight = if (isRightHand) {
                assignments.sortedBy { it.fingerNumber }
            } else {
                assignments.sortedByDescending { it.fingerNumber }
            }

            val pressedIndices = orderedLeftToRight.indices.filter { orderedLeftToRight[it].isPressed }
            val hasPressedNotes = pressedIndices.isNotEmpty()
            val anyBlackPressed = orderedLeftToRight.any { it.isPressed && isMidiNoteBlack(it.midiNote) }
            val palmShiftY = if (anyBlackPressed) -totalH * 0.045f else 0f

            // FIXED: Always use the spring-smoothed `animatedPalmMidi` for horizontal wrist translation
            // (blended 75% smooth spring + 25% pressed centroid when active) so the hand NEVER teleports!
            val smoothPalmX = interpolateMidiX(animatedPalmMidi)
            val rawTargetPalmX = if (hasPressedNotes) {
                val firstPressedX = midiToExactX(orderedLeftToRight[pressedIndices.first()].midiNote)
                val lastPressedX = midiToExactX(orderedLeftToRight[pressedIndices.last()].midiNote)
                val pressedCentroidX = (firstPressedX + lastPressedX) * 0.5f
                smoothPalmX * 0.72f + pressedCentroidX * 0.28f
            } else {
                if (showLeftHand && showRightHand) {
                    if (isRightHand) smoothPalmX.coerceAtLeast(territoryBorderX + whiteKeyW * 1.3f)
                    else smoothPalmX.coerceAtMost(territoryBorderX - whiteKeyW * 1.3f)
                } else smoothPalmX
            }

            val palmX = rawTargetPalmX.coerceIn(whiteKeyW * 1.5f, totalW - whiteKeyW * 1.5f)
            val palmY = (totalH * 0.89f + palmShiftY).coerceIn(totalH * 0.72f, totalH * 0.94f)
            val wristY = totalH + 10.dp.toPx()
            val wristCenterX = palmX + (if (isRightHand) whiteKeyW * 0.28f else -whiteKeyW * 0.28f)

            data class OrganicFinger(
                val item: HandFingerAssignment,
                val baseLeft: Offset,
                val baseRight: Offset,
                val tipCenter: Offset,
                val halfWidth: Float
            )

            val baseFingerHalfW = (whiteKeyW * 0.27f).coerceIn(4.2f, 10.5.dp.toPx())
            val fingerGeoms = ArrayList<OrganicFinger>(5)

            val rawTipXs = FloatArray(5)
            val naturalSlotStepPx = whiteKeyW * 0.84f

            if (!hasPressedNotes) {
                for (i in 0 until 5) {
                    rawTipXs[i] = palmX + (i - 2f) * naturalSlotStepPx
                }
            } else {
                // 1. Lock all pressed fingers to their exact physical key X
                for (idx in pressedIndices) {
                    rawTipXs[idx] = midiToExactX(orderedLeftToRight[idx].midiNote)
                }
                // 2. Extrapolate unpressed fingers to the LEFT of the leftmost pressed finger
                val firstP = pressedIndices.first()
                for (i in (firstP - 1) downTo 0) {
                    val assignedX = midiToExactX(orderedLeftToRight[i].midiNote)
                    rawTipXs[i] = minOf(assignedX, rawTipXs[i + 1] - naturalSlotStepPx * 0.78f)
                }
                // 3. Extrapolate unpressed fingers to the RIGHT of the rightmost pressed finger
                val lastP = pressedIndices.last()
                for (i in (lastP + 1) until 5) {
                    val assignedX = midiToExactX(orderedLeftToRight[i].midiNote)
                    rawTipXs[i] = maxOf(assignedX, rawTipXs[i - 1] + naturalSlotStepPx * 0.78f)
                }
                // 4. Linearly interpolate unpressed fingers BETWEEN pressed fingers (e.g. F2..F4 during a 5-1 octave span)
                for (pIdx in 0 until pressedIndices.size - 1) {
                    val leftSlot = pressedIndices[pIdx]
                    val rightSlot = pressedIndices[pIdx + 1]
                    if (rightSlot - leftSlot > 1) {
                        val startX = rawTipXs[leftSlot]
                        val endX = rawTipXs[rightSlot]
                        val step = (endX - startX) / (rightSlot - leftSlot).toFloat()
                        for (slot in (leftSlot + 1) until rightSlot) {
                            rawTipXs[slot] = startX + step * (slot - leftSlot)
                        }
                    }
                }
            }

            // Compute current span stretch ratio so the knuckle arch widens naturally during octaves
            val currentOuterSpreadPx = (rawTipXs[4] - rawTipXs[0]).coerceAtLeast(whiteKeyW * 2.8f)
            val archSpreadScale = (currentOuterSpreadPx / (whiteKeyW * 3.4f)).coerceIn(0.88f, 1.45f)

            orderedLeftToRight.forEachIndexed { slotIdx, item ->
                val fNum = item.fingerNumber
                val tipX = rawTipXs[slotIdx].coerceIn(baseFingerHalfW, totalW - baseFingerHalfW)
                val isBlack = isMidiNoteBlack(item.midiNote)

                val fingerReachFrac = when (fNum) {
                    1 -> 0.22f // Thumb (Anatomically lower opposition reach)
                    2 -> 0.44f // Index
                    3 -> 0.50f // Middle
                    4 -> 0.46f // Ring
                    else -> 0.37f // Pinky
                }

                val pressCurlY = if (item.isPressed) totalH * 0.024f else 0f

                val tipY = if (isBlack && item.isPressed) {
                    blackKeyH * 0.78f
                } else if (isBlack) {
                    blackKeyH * 0.88f
                } else {
                    (palmY - totalH * fingerReachFrac) + pressCurlY
                }.coerceIn(totalH * 0.14f, totalH * 0.78f)

                val slotFromCenter = (slotIdx - 2f)
                // Gentle knuckle abduction that follows finger tips without shearing the palm
                val archBaseX = palmX + (slotFromCenter * whiteKeyW * 0.54f * archSpreadScale) + ((tipX - palmX) * 0.26f)
                val archBaseY = (palmY - totalH * 0.15f) + (slotFromCenter * slotFromCenter * totalH * 0.012f) +
                    (if (fNum == 1) totalH * 0.085f else 0f)

                val fHalfW = when (fNum) {
                    1 -> baseFingerHalfW * 1.12f
                    5 -> baseFingerHalfW * 0.85f
                    else -> baseFingerHalfW
                }

                fingerGeoms.add(
                    OrganicFinger(
                        item = item,
                        baseLeft = Offset(archBaseX - fHalfW, archBaseY),
                        baseRight = Offset(archBaseX + fHalfW, archBaseY),
                        tipCenter = Offset(tipX, tipY),
                        halfWidth = fHalfW
                    )
                )
            }

            val handOutlinePath = Path().apply {
                val firstF = fingerGeoms.first()
                val lastF = fingerGeoms.last()
                val wristHalfW = whiteKeyW * 1.32f

                moveTo(wristCenterX - wristHalfW, wristY)
                cubicTo(
                    palmX - whiteKeyW * 1.55f * archSpreadScale.coerceAtMost(1.22f),
                    (wristY + palmY) * 0.5f,
                    firstF.baseLeft.x - whiteKeyW * 0.22f,
                    firstF.baseLeft.y + totalH * 0.08f,
                    firstF.baseLeft.x,
                    firstF.baseLeft.y
                )

                for (i in fingerGeoms.indices) {
                    val fg = fingerGeoms[i]
                    val tip = fg.tipCenter
                    val hw = fg.halfWidth
                    val tipHw = hw * 0.88f

                    cubicTo(
                        fg.baseLeft.x * 0.58f + (tip.x - tipHw) * 0.42f,
                        fg.baseLeft.y * 0.58f + tip.y * 0.42f,
                        tip.x - tipHw,
                        tip.y + tipHw * 1.3f,
                        tip.x - tipHw,
                        tip.y
                    )
                    cubicTo(
                        tip.x - tipHw,
                        tip.y - tipHw * 1.25f,
                        tip.x + tipHw,
                        tip.y - tipHw * 1.25f,
                        tip.x + tipHw,
                        tip.y
                    )
                    cubicTo(
                        tip.x + tipHw,
                        tip.y + tipHw * 1.3f,
                        fg.baseRight.x * 0.58f + (tip.x + tipHw) * 0.42f,
                        fg.baseRight.y * 0.58f + tip.y * 0.42f,
                        fg.baseRight.x,
                        fg.baseRight.y
                    )

                    if (i < fingerGeoms.lastIndex) {
                        val nextFg = fingerGeoms[i + 1]
                        val webMidX = (fg.baseRight.x + nextFg.baseLeft.x) * 0.5f
                        val webMidY = maxOf(fg.baseRight.y, nextFg.baseLeft.y) + totalH * 0.024f
                        quadraticBezierTo(webMidX, webMidY, nextFg.baseLeft.x, nextFg.baseLeft.y)
                    }
                }

                cubicTo(
                    lastF.baseRight.x + whiteKeyW * 0.22f,
                    lastF.baseRight.y + totalH * 0.08f,
                    palmX + whiteKeyW * 1.55f * archSpreadScale.coerceAtMost(1.22f),
                    (wristY + palmY) * 0.5f,
                    wristCenterX + wristHalfW,
                    wristY
                )
                close()
            }

            val skinLight = Color(
                red = 0.96f * 0.82f + handAccentColor.red * 0.18f,
                green = 0.82f * 0.82f + handAccentColor.green * 0.18f,
                blue = 0.72f * 0.82f + handAccentColor.blue * 0.18f,
                alpha = if (hasPressedNotes) 0.84f else 0.52f
            )
            val skinMid = Color(
                red = 0.88f * 0.78f + handAccentColor.red * 0.22f,
                green = 0.71f * 0.78f + handAccentColor.green * 0.22f,
                blue = 0.60f * 0.78f + handAccentColor.blue * 0.22f,
                alpha = if (hasPressedNotes) 0.78f else 0.46f
            )
            val skinShadow = Color(
                red = 0.68f * 0.75f + handAccentColor.red * 0.25f,
                green = 0.50f * 0.75f + handAccentColor.green * 0.25f,
                blue = 0.42f * 0.75f + handAccentColor.blue * 0.25f,
                alpha = if (hasPressedNotes) 0.72f else 0.40f
            )

            // Apply subtle wrist rotation only to the palm silhouette around the wrist pivot,
            // while keeping pressed finger tips locked 1:1 on their target keys!
            withTransform({
                rotate(degrees = wristTiltDeg * 0.45f, pivot = Offset(palmX, palmY))
            }) {
                drawPath(
                    path = handOutlinePath,
                    color = Color.Black.copy(alpha = 0.28f),
                    style = Stroke(width = 4.dp.toPx())
                )

                drawPath(
                    path = handOutlinePath,
                    brush = Brush.radialGradient(
                        colors = listOf(skinLight, skinMid, skinShadow),
                        center = Offset(palmX, palmY - totalH * 0.12f),
                        radius = (whiteKeyW * 4.4f * archSpreadScale).coerceAtLeast(32f)
                    )
                )

                drawPath(
                    path = handOutlinePath,
                    color = handAccentColor.copy(alpha = 0.88f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            fingerBadgePaint.textSize = (baseFingerHalfW * 1.45f).coerceIn(8.5.dp.toPx(), 13.dp.toPx())

            fingerGeoms.forEach { fg ->
                val tip = fg.tipCenter
                val hw = fg.halfWidth
                val isPressed = fg.item.isPressed

                val midX = (fg.baseLeft.x + fg.baseRight.x) * 0.25f + tip.x * 0.5f
                val midY = (fg.baseLeft.y + fg.baseRight.y) * 0.25f + tip.y * 0.5f
                drawLine(
                    color = skinShadow.copy(alpha = 0.45f),
                    start = Offset(midX - hw * 0.55f, midY),
                    end = Offset(midX + hw * 0.55f, midY),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val nailW = hw * 1.18f
                val nailH = hw * 1.25f
                val nailTopY = tip.y - hw * 0.65f
                drawRoundRect(
                    color = if (isPressed) Color(0xFFFFF9C4) else Color(0xFFFFF0E6).copy(alpha = 0.72f),
                    topLeft = Offset(tip.x - nailW * 0.5f, nailTopY),
                    size = Size(nailW, nailH),
                    cornerRadius = CornerRadius(nailW * 0.42f, nailW * 0.42f)
                )
                drawRoundRect(
                    color = if (isPressed) handAccentColor else skinShadow.copy(alpha = 0.5f),
                    topLeft = Offset(tip.x - nailW * 0.5f, nailTopY),
                    size = Size(nailW, nailH),
                    cornerRadius = CornerRadius(nailW * 0.42f, nailW * 0.42f),
                    style = Stroke(width = if (isPressed) 1.6.dp.toPx() else 0.8.dp.toPx())
                )

                val badgeY = tip.y + hw * 1.25f
                val badgeRad = (hw * (if (isPressed) 1.08f else 0.80f)).coerceAtLeast(6.5f.dp.toPx())

                if (isPressed) {
                    drawCircle(
                        color = handAccentColor.copy(alpha = 0.58f),
                        radius = badgeRad * 1.48f,
                        center = Offset(tip.x, badgeY)
                    )
                }

                drawCircle(
                    color = if (isPressed) handAccentColor else Color(0xFF141820).copy(alpha = 0.75f),
                    radius = badgeRad,
                    center = Offset(tip.x, badgeY)
                )
                drawCircle(
                    color = if (isPressed) Color.White else handAccentColor.copy(alpha = 0.75f),
                    radius = badgeRad,
                    center = Offset(tip.x, badgeY),
                    style = Stroke(width = if (isPressed) 1.8.dp.toPx() else 1.0.dp.toPx())
                )

                fingerBadgePaint.color = if (isPressed) {
                    android.graphics.Color.WHITE
                } else {
                    android.graphics.Color.argb(195, 235, 240, 255)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    fg.item.fingerNumber.toString(),
                    tip.x,
                    badgeY + fingerBadgePaint.textSize * 0.36f,
                    fingerBadgePaint
                )
            }

            val cuffW = (whiteKeyW * 1.55f).coerceIn(24.dp.toPx(), 40.dp.toPx())
            val cuffH = (cuffW * 0.48f).coerceAtLeast(13.dp.toPx())
            val cuffX = (palmX * 0.65f + wristCenterX * 0.35f)
            val cuffY = (palmY - cuffH * 0.1f).coerceAtMost(totalH - cuffH * 0.55f - 2f)

            drawRoundRect(
                color = handAccentColor.copy(alpha = 0.92f),
                topLeft = Offset(cuffX - cuffW * 0.5f, cuffY - cuffH * 0.5f),
                size = Size(cuffW, cuffH),
                cornerRadius = CornerRadius(cuffH * 0.5f, cuffH * 0.5f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(cuffX - cuffW * 0.5f, cuffY - cuffH * 0.5f),
                size = Size(cuffW, cuffH),
                cornerRadius = CornerRadius(cuffH * 0.5f, cuffH * 0.5f),
                style = Stroke(width = 1.2.dp.toPx())
            )
            wristBadgePaint.textSize = (cuffH * 0.62f).coerceIn(8.dp.toPx(), 11.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                if (isRightHand) "RH" else "LH",
                cuffX,
                cuffY + wristBadgePaint.textSize * 0.36f,
                wristBadgePaint
            )
        }

        if (showLeftHand) {
            drawOrganicHumanHand(
                assignments = lhSolution.assignments,
                animatedPalmMidi = animatedLhPalmMidi,
                wristTiltDeg = animatedLhTiltDeg,
                handAccentColor = leftHandColor,
                isRightHand = false
            )
        }
        if (showRightHand) {
            drawOrganicHumanHand(
                assignments = rhSolution.assignments,
                animatedPalmMidi = animatedRhPalmMidi,
                wristTiltDeg = animatedRhTiltDeg,
                handAccentColor = rightHandColor,
                isRightHand = true
            )
        }
    }
}