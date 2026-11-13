package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private data class HandFingerAssignment(
    val fingerNumber: Int, // 1 = Thumb, 2 = Index, 3 = Middle, 4 = Ring, 5 = Pinky
    val midiNote: Int,
    val isPressed: Boolean,
    val isCrossingUnder: Boolean = false,
    val isCrossingOver: Boolean = false
)

// Anatomical resting offsets (in semitones) relative to Middle Finger (F3 = 0.0f)
// Left-to-Right on Keyboard:
//   LH: Pinky(5) [-3.5] -> Ring(4) [-1.8] -> Middle(3) [0.0] -> Index(2) [+1.8] -> Thumb(1) [+3.5]
//   RH: Thumb(1) [-3.5] -> Index(2) [-1.8] -> Middle(3) [0.0] -> Ring(4) [+1.8] -> Pinky(5) [+3.5]
private val LH_ANATOMICAL_REST_SEMITONES = floatArrayOf(0f, +3.5f, +1.8f, 0.0f, -1.8f, -3.5f)
private val RH_ANATOMICAL_REST_SEMITONES = floatArrayOf(0f, -3.5f, -1.8f, 0.0f, +1.8f, +3.5f)

// Relative anatomical bone proportions & widths for Fingers 1..5 (index 0 unused)
private val FINGER_LENGTH_MULT = floatArrayOf(0f, 0.64f, 0.91f, 1.00f, 0.94f, 0.76f)
private val FINGER_WIDTH_MULT = floatArrayOf(0f, 1.28f, 0.96f, 1.00f, 0.93f, 0.80f)
private val MCP_ARCH_Y_OFFSET = floatArrayOf(0f, 0.105f, 0.012f, 0.000f, 0.016f, 0.042f)

internal fun resolvePlayableMelodicTrackIndices(
    score: Score?,
    enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
    preferGuitarTracks: Boolean = false
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

    if (preferGuitarTracks && nonDrumTracks.isNotEmpty()) {
        val guitarTracks = nonDrumTracks.filter { tr ->
            val nameLow = tr.trackName.lowercase()
            tr.program in 24..31 || nameLow.contains("guitar") || nameLow.contains("gtr") || nameLow.contains("acoustic") || nameLow.contains("electric")
        }
        if (guitarTracks.isNotEmpty()) {
            return guitarTracks.map { it.trackIndex }.toSet()
        }
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
    val isFlatKey = keySignature.contains("FLAT", true) ||
        keySignature in listOf("Db", "Bb", "Eb", "Ab", "F", "Gb", "Cb")
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

private data class HandSolvedState(
    val palmCenterMidi: Float,
    val lastMelodicPitch: Int,
    val lastMelodicFinger: Int,
    val assignments: List<HandFingerAssignment>,
    val chordLabel: String
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

        val fingerNeutralPitch = lastPalmMidi + restOffsets[f]
        cost += abs(pitch - fingerNeutralPitch) * 1.85f

        if (f == lastFinger && delta != 0) {
            cost += 75f
        }
        if (delta != 0 && ((lastFinger == 2 && f == 1) || (lastFinger == 1 && f == 2))) {
            cost += 65f
        }

        val anatomicalStep = if (isRightHand) (f - lastFinger) else (lastFinger - f)

        if (delta > 0) {
            if (isRightHand) {
                if (anatomicalStep in 1..2) {
                    cost -= 28f
                } else if (f == 1 && lastFinger in 2..4 && !isBlack && delta in 1..4) {
                    val continuesUp = lookaheadPitch != null && lookaheadPitch > pitch
                    cost += if (continuesUp || lastFinger >= 3) -16f else 4.0f
                } else {
                    cost += 55f
                }
            } else {
                if (anatomicalStep in 1..2) {
                    cost -= 28f
                } else if (lastFinger == 1 && f in 2..4 && delta in 1..4) {
                    cost -= 16f
                } else {
                    cost += 55f
                }
            }
        } else {
            if (isRightHand) {
                if (anatomicalStep in -2..-1) {
                    cost -= 28f
                } else if (lastFinger == 1 && f in 2..4 && -delta in 1..4) {
                    cost -= 16f
                } else {
                    cost += 55f
                }
            } else {
                if (anatomicalStep in -2..-1) {
                    cost -= 28f
                } else if (f == 1 && lastFinger in 2..4 && !isBlack && -delta in 1..4) {
                    val continuesDown = lookaheadPitch != null && lookaheadPitch < pitch
                    cost += if (continuesDown || lastFinger >= 3) -16f else 4.0f
                } else {
                    cost += 55f
                }
            }
        }

        if (isBlack && f == 1) cost += 18f

        if (lookaheadPitch != null && lookaheadPitch != pitch) {
            val nextDelta = lookaheadPitch - pitch
            if (isRightHand) {
                if (nextDelta > 0 && f == 5) cost += 14f
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

private fun buildCompactPitchLabel(midiNote: Int): String {
    val names = arrayOf("C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")
    val octave = (midiNote / 12) - 1
    return "${names[(midiNote % 12 + 12) % 12]}$octave"
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
            assignments = idleFingers,
            chordLabel = if (isRightHand) "RH" else "LH"
        )
    }

    val sorted = clampChordToAnatomicalHand(activePitchesAsc, isRightHand, maxSpanSemitones = 12)
    val count = sorted.size
    val span = (sorted.last() - sorted.first()).coerceAtLeast(0)

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

    val fingersLeftToRight: List<Int> = if (isRightHand) {
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
                span >= 8 -> listOf(1, 5)
                span >= 6 -> adjustRhNarrowChord(listOf(1, 4))
                span >= 4 -> adjustRhNarrowChord(listOf(1, 3))
                else -> adjustRhNarrowChord(listOf(1, 2))
            }
            3 -> {
                val lowGap = sorted[1] - sorted[0]
                val highGap = sorted[2] - sorted[1]
                when {
                    span >= 9 -> if (lowGap >= highGap) listOf(1, 3, 5) else listOf(1, 2, 5)
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
                span >= 8 -> listOf(5, 1)
                span >= 6 -> adjustLhNarrowChord(listOf(4, 1))
                span >= 4 -> adjustLhNarrowChord(listOf(3, 1))
                else -> adjustLhNarrowChord(listOf(2, 1))
            }
            3 -> {
                val lowGap = sorted[1] - sorted[0]
                val highGap = sorted[2] - sorted[1]
                when {
                    span >= 9 -> if (lowGap >= highGap) listOf(5, 2, 1) else listOf(5, 3, 1)
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

    // Detect active scale thumb-under or finger-over crossover for 3D visual layering
    val singleFinger = if (count == 1) fingersLeftToRight.first() else -1
    val singleDelta = if (count == 1) sorted.first() - lastMelodicPitch else 0
    val isThumbCrossingUnder = count == 1 && singleFinger == 1 && lastMelodicFinger in 2..4 &&
        ((isRightHand && singleDelta in 1..4) || (!isRightHand && singleDelta in -4..-1))
    val isFingerCrossingOver = count == 1 && singleFinger in 2..4 && lastMelodicFinger == 1 &&
        ((isRightHand && singleDelta in -4..-1) || (!isRightHand && singleDelta in 1..4))

    val impliedPalmCenters = pressedByFinger.map { (fNum, pitch) ->
        pitch - restOffsets[fNum]
    }
    val rawTargetPalm = if (impliedPalmCenters.isNotEmpty()) {
        impliedPalmCenters.average().toFloat()
    } else {
        safePalm
    }

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
            HandFingerAssignment(
                fingerNumber = fNum,
                midiNote = pressedPitch,
                isPressed = true,
                isCrossingUnder = isThumbCrossingUnder && fNum == 1,
                isCrossingOver = isFingerCrossingOver && fNum == singleFinger
            )
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

    val handPrefix = if (isRightHand) "RH" else "LH"
    val chordLabel = when (count) {
        1 -> "$handPrefix • ${buildCompactPitchLabel(sorted[0])}→F${fingersLeftToRight[0]}"
        2 -> "$handPrefix • ${buildCompactPitchLabel(sorted[0])}-${buildCompactPitchLabel(sorted[1])}"
        else -> "$handPrefix • ${buildCompactPitchLabel(sorted.first())}..${buildCompactPitchLabel(sorted.last())}"
    }

    return HandSolvedState(
        palmCenterMidi = lookaheadAdjustedPalm,
        lastMelodicPitch = newMelodicPitch,
        lastMelodicFinger = newMelodicFinger,
        assignments = allFiveFingers,
        chordLabel = chordLabel
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

private data class ArticulatedFingerBoneChain(
    val assignment: HandFingerAssignment,
    val pressProgress: Float,
    val mcpJoint: Offset,
    val pipJoint: Offset,
    val dipJoint: Offset,
    val tipCenter: Offset,
    val baseHalfWidth: Float,
    val pipHalfWidth: Float,
    val dipHalfWidth: Float,
    val tipHalfWidth: Float,
    val axisAngleRad: Float,
    val isBlackKey: Boolean
)

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

    val targetLhPalmMidi = lhSolution.palmCenterMidi.coerceAtMost(lhTerritoryCeiling.toFloat())
    val targetRhPalmMidi = rhSolution.palmCenterMidi.coerceAtLeast(rhTerritoryFloor.toFloat())

    val animatedLhPalmMidi by animateFloatAsState(
        targetValue = targetLhPalmMidi,
        animationSpec = spring(dampingRatio = 0.94f, stiffness = 340f),
        label = "lh_palm_glide"
    )

    val animatedRhPalmMidi by animateFloatAsState(
        targetValue = targetRhPalmMidi,
        animationSpec = spring(dampingRatio = 0.94f, stiffness = 340f),
        label = "rh_palm_glide"
    )

    // 10-Finger Independent Spring Animation Channels (MIDI pitch & Press Articulation)
    val lhByFinger = remember(lhSolution.assignments) { lhSolution.assignments.associateBy { it.fingerNumber } }
    val rhByFinger = remember(rhSolution.assignments) { rhSolution.assignments.associateBy { it.fingerNumber } }

    val lhF1Midi by animateFloatAsState(lhByFinger[1]?.midiNote?.toFloat() ?: 52f, spring(0.92f, 520f), label = "lhF1M")
    val lhF2Midi by animateFloatAsState(lhByFinger[2]?.midiNote?.toFloat() ?: 50f, spring(0.92f, 520f), label = "lhF2M")
    val lhF3Midi by animateFloatAsState(lhByFinger[3]?.midiNote?.toFloat() ?: 48f, spring(0.92f, 520f), label = "lhF3M")
    val lhF4Midi by animateFloatAsState(lhByFinger[4]?.midiNote?.toFloat() ?: 46f, spring(0.92f, 520f), label = "lhF4M")
    val lhF5Midi by animateFloatAsState(lhByFinger[5]?.midiNote?.toFloat() ?: 45f, spring(0.92f, 520f), label = "lhF5M")

    val rhF1Midi by animateFloatAsState(rhByFinger[1]?.midiNote?.toFloat() ?: 64f, spring(0.92f, 520f), label = "rhF1M")
    val rhF2Midi by animateFloatAsState(rhByFinger[2]?.midiNote?.toFloat() ?: 65f, spring(0.92f, 520f), label = "rhF2M")
    val rhF3Midi by animateFloatAsState(rhByFinger[3]?.midiNote?.toFloat() ?: 67f, spring(0.92f, 520f), label = "rhF3M")
    val rhF4Midi by animateFloatAsState(rhByFinger[4]?.midiNote?.toFloat() ?: 69f, spring(0.92f, 520f), label = "rhF4M")
    val rhF5Midi by animateFloatAsState(rhByFinger[5]?.midiNote?.toFloat() ?: 71f, spring(0.92f, 520f), label = "rhF5M")

    val lhF1Press by animateFloatAsState(if (lhByFinger[1]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "lhF1P")
    val lhF2Press by animateFloatAsState(if (lhByFinger[2]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "lhF2P")
    val lhF3Press by animateFloatAsState(if (lhByFinger[3]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "lhF3P")
    val lhF4Press by animateFloatAsState(if (lhByFinger[4]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "lhF4P")
    val lhF5Press by animateFloatAsState(if (lhByFinger[5]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "lhF5P")

    val rhF1Press by animateFloatAsState(if (rhByFinger[1]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "rhF1P")
    val rhF2Press by animateFloatAsState(if (rhByFinger[2]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "rhF2P")
    val rhF3Press by animateFloatAsState(if (rhByFinger[3]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "rhF3P")
    val rhF4Press by animateFloatAsState(if (rhByFinger[4]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "rhF4P")
    val rhF5Press by animateFloatAsState(if (rhByFinger[5]?.isPressed == true) 1f else 0f, spring(0.85f, 680f), label = "rhF5P")

    val targetLhTiltDeg = remember(disjointLh, targetLhPalmMidi, animatedLhPalmMidi) {
        val velocityTilt = ((targetLhPalmMidi - animatedLhPalmMidi) * 0.85f).coerceIn(-5.5f, 5.5f)
        val thumbUnderBias = if (disjointLh.size == 1 && lhSolution.lastMelodicFinger == 1) -2.4f else 0f
        (-2.2f + velocityTilt + thumbUnderBias).coerceIn(-8.5f, 8.5f)
    }

    val targetRhTiltDeg = remember(disjointRh, targetRhPalmMidi, animatedRhPalmMidi) {
        val velocityTilt = ((targetRhPalmMidi - animatedRhPalmMidi) * 0.85f).coerceIn(-5.5f, 5.5f)
        val thumbUnderBias = if (disjointRh.size == 1 && rhSolution.lastMelodicFinger == 1) +2.4f else 0f
        (+2.2f + velocityTilt + thumbUnderBias).coerceIn(-8.5f, 8.5f)
    }

    val animatedLhTiltDeg by animateFloatAsState(
        targetValue = targetLhTiltDeg,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 280f),
        label = "lh_wrist_tilt"
    )

    val animatedRhTiltDeg by animateFloatAsState(
        targetValue = targetRhTiltDeg,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 280f),
        label = "rh_wrist_tilt"
    )

    val strikePulse = remember(score, playheadMeasure, playheadOffset, partitioned.melodicTracks) {
        computeRepetitiveStrikePulse(score, playheadMeasure, playheadOffset, partitioned.melodicTracks)
    }

    val fingerBadgePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(3.5f, 0f, 1.2f, android.graphics.Color.argb(200, 0, 0, 0))
        }
    }
    val fingerNoteSubPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(2.5f, 0f, 1f, android.graphics.Color.argb(210, 0, 0, 0))
        }
    }
    val wristBadgePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
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

        fun drawUltrarealisticHumanHand(
            assignments: List<HandFingerAssignment>,
            animatedFingerMidi: FloatArray, // index 1..5
            animatedFingerPress: FloatArray, // index 1..5
            animatedPalmMidi: Float,
            wristTiltDeg: Float,
            handAccentColor: Color,
            chordTelemetryLabel: String,
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
            val blackForwardShiftY = if (anyBlackPressed) -totalH * 0.052f else 0f

            val smoothPalmX = interpolateMidiX(animatedPalmMidi)
            val rawTargetPalmX = if (hasPressedNotes) {
                val firstPressedX = midiToExactX(orderedLeftToRight[pressedIndices.first()].midiNote)
                val lastPressedX = midiToExactX(orderedLeftToRight[pressedIndices.last()].midiNote)
                val pressedCentroidX = (firstPressedX + lastPressedX) * 0.5f
                smoothPalmX * 0.74f + pressedCentroidX * 0.26f
            } else {
                if (showLeftHand && showRightHand) {
                    if (isRightHand) smoothPalmX.coerceAtLeast(territoryBorderX + whiteKeyW * 1.25f)
                    else smoothPalmX.coerceAtMost(territoryBorderX - whiteKeyW * 1.25f)
                } else smoothPalmX
            }

            val palmX = rawTargetPalmX.coerceIn(whiteKeyW * 1.5f, totalW - whiteKeyW * 1.5f)
            val palmY = (totalH * 0.885f + blackForwardShiftY).coerceIn(totalH * 0.70f, totalH * 0.94f)
            val wristY = totalH + 14.dp.toPx()
            val ulnarBiasX = if (isRightHand) whiteKeyW * 0.26f else -whiteKeyW * 0.26f
            val wristCenterX = palmX + ulnarBiasX

            val baseFingerHalfW = (whiteKeyW * 0.275f).coerceIn(4.4f, 11.dp.toPx())
            val naturalSlotStepPx = whiteKeyW * 0.85f

            // Compute left-to-right target tip X positions using spring-animated MIDI channels
            // while locking actively pressed fingers 1:1 to their target key center
            val rawTipXs = FloatArray(5)
            for (slotIdx in 0 until 5) {
                val item = orderedLeftToRight[slotIdx]
                val fNum = item.fingerNumber
                rawTipXs[slotIdx] = if (item.isPressed) {
                    midiToExactX(item.midiNote)
                } else {
                    interpolateMidiX(animatedFingerMidi[fNum])
                }
            }

            // Enforce anatomical non-crossing spacing for unpressed fingers unless a thumb-under / finger-over is active
            if (!hasPressedNotes) {
                for (i in 0 until 5) {
                    val restTarget = palmX + (i - 2f) * naturalSlotStepPx
                    rawTipXs[i] = rawTipXs[i] * 0.65f + restTarget * 0.35f
                }
            } else {
                val firstP = pressedIndices.first()
                for (i in (firstP - 1) downTo 0) {
                    rawTipXs[i] = minOf(rawTipXs[i], rawTipXs[i + 1] - naturalSlotStepPx * 0.76f)
                }
                val lastP = pressedIndices.last()
                for (i in (lastP + 1) until 5) {
                    rawTipXs[i] = maxOf(rawTipXs[i], rawTipXs[i - 1] + naturalSlotStepPx * 0.76f)
                }
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

            val currentOuterSpreadPx = abs(rawTipXs[4] - rawTipXs[0]).coerceAtLeast(whiteKeyW * 2.7f)
            val archSpreadScale = (currentOuterSpreadPx / (whiteKeyW * 3.4f)).coerceIn(0.86f, 1.48f)

            // Build 3-Phalanx Inverse Kinematics Bone Chains for all 5 fingers
            val articulatedFingers = ArrayList<ArticulatedFingerBoneChain>(5)

            orderedLeftToRight.forEachIndexed { slotIdx, item ->
                val fNum = item.fingerNumber
                val pressAnim = (animatedFingerPress[fNum] * (0.65f + 0.35f * strikePulse)).coerceIn(0f, 1f)
                val isBlack = isMidiNoteBlack(item.midiNote)

                val fHalfW = baseFingerHalfW * FINGER_WIDTH_MULT[fNum]
                val tipX = rawTipXs[slotIdx].coerceIn(fHalfW, totalW - fHalfW)

                val reachMult = FINGER_LENGTH_MULT[fNum]
                val fullReachPx = totalH * 0.51f * reachMult

                // Pressing a key curls the PIP/DIP joints forward and slightly compresses top-down visible length
                val flexShorteningPx = pressAnim * totalH * 0.026f

                val tipY = if (isBlack) {
                    val blackTargetY = if (item.isPressed) blackKeyH * 0.77f else blackKeyH * 0.87f
                    blackTargetY + pressAnim * totalH * 0.012f
                } else {
                    (palmY - fullReachPx + flexShorteningPx)
                }.coerceIn(totalH * 0.13f, totalH * 0.81f)

                val slotFromCenter = (slotIdx - 2f)
                // MCP Knuckle Arch Position
                val mcpX = if (fNum == 1) {
                    // Thumb originates lower and more laterally from the CMC/MCP opposition joint
                    val thumbSideDir = if (isRightHand) -1f else 1f
                    palmX + thumbSideDir * whiteKeyW * 1.18f * archSpreadScale.coerceAtMost(1.25f) +
                        (tipX - palmX) * 0.22f
                } else {
                    palmX + (slotFromCenter * whiteKeyW * 0.52f * archSpreadScale) + ((tipX - palmX) * 0.25f)
                }

                val mcpY = (palmY - totalH * 0.155f) + (totalH * MCP_ARCH_Y_OFFSET[fNum])

                val mcpPos = Offset(mcpX, mcpY)
                val tipPos = Offset(tipX, tipY)

                // Compute natural lateral bow for thumb opposition and 3D joint articulation
                val dx = tipPos.x - mcpPos.x
                val dy = tipPos.y - mcpPos.y
                val axisAngle = atan2(dy, dx)

                val thumbBowX = if (fNum == 1) {
                    val outwardSign = if (isRightHand) -1f else 1f
                    if (item.isCrossingUnder) -outwardSign * whiteKeyW * 0.18f
                    else outwardSign * whiteKeyW * 0.20f
                } else {
                    dx * 0.06f
                }

                val pipPos = Offset(
                    x = mcpPos.x + dx * 0.46f + thumbBowX,
                    y = mcpPos.y + dy * 0.46f - pressAnim * 2.5.dp.toPx()
                )
                val dipPos = Offset(
                    x = mcpPos.x + dx * 0.76f + thumbBowX * 0.45f,
                    y = mcpPos.y + dy * 0.76f - pressAnim * 1.5.dp.toPx()
                )

                articulatedFingers.add(
                    ArticulatedFingerBoneChain(
                        assignment = item,
                        pressProgress = pressAnim,
                        mcpJoint = mcpPos,
                        pipJoint = pipPos,
                        dipJoint = dipPos,
                        tipCenter = tipPos,
                        baseHalfWidth = fHalfW,
                        pipHalfWidth = fHalfW * 0.92f,
                        dipHalfWidth = fHalfW * 0.84f,
                        tipHalfWidth = fHalfW * 0.78f,
                        axisAngleRad = axisAngle,
                        isBlackKey = isBlack
                    )
                )
            }

            // Realistic Multi-Layer Dermal Palette blended with Hand Accent Color
            val activeAlphaBoost = if (hasPressedNotes) 1.0f else 0.76f
            val skinHighlight = Color(
                red = (0.98f * 0.85f + handAccentColor.red * 0.15f).coerceIn(0f, 1f),
                green = (0.85f * 0.85f + handAccentColor.green * 0.15f).coerceIn(0f, 1f),
                blue = (0.76f * 0.85f + handAccentColor.blue * 0.15f).coerceIn(0f, 1f),
                alpha = 0.94f * activeAlphaBoost
            )
            val skinMidtone = Color(
                red = (0.90f * 0.82f + handAccentColor.red * 0.18f).coerceIn(0f, 1f),
                green = (0.73f * 0.82f + handAccentColor.green * 0.18f).coerceIn(0f, 1f),
                blue = (0.62f * 0.82f + handAccentColor.blue * 0.18f).coerceIn(0f, 1f),
                alpha = 0.92f * activeAlphaBoost
            )
            val skinSubsurface = Color(
                red = (0.83f * 0.78f + handAccentColor.red * 0.22f).coerceIn(0f, 1f),
                green = (0.58f * 0.78f + handAccentColor.green * 0.22f).coerceIn(0f, 1f),
                blue = (0.50f * 0.78f + handAccentColor.blue * 0.22f).coerceIn(0f, 1f),
                alpha = 0.90f * activeAlphaBoost
            )
            val skinDeepShadow = Color(
                red = (0.58f * 0.74f + handAccentColor.red * 0.26f).coerceIn(0f, 1f),
                green = (0.40f * 0.74f + handAccentColor.green * 0.26f).coerceIn(0f, 1f),
                blue = (0.35f * 0.74f + handAccentColor.blue * 0.26f).coerceIn(0f, 1f),
                alpha = 0.88f * activeAlphaBoost
            )

            // 1. Key-Bed Contact Shadow & Acoustic Force Ripples under pressed fingertips
            articulatedFingers.forEach { fg ->
                if (fg.assignment.isPressed) {
                    val tip = fg.tipCenter
                    val rippleRadius = fg.tipHalfWidth * (1.55f + 0.35f * fg.pressProgress)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                handAccentColor.copy(alpha = 0.52f * fg.pressProgress),
                                handAccentColor.copy(alpha = 0.18f * fg.pressProgress),
                                Color.Transparent
                            ),
                            center = Offset(tip.x, tip.y + fg.tipHalfWidth * 0.15f),
                            radius = rippleRadius * 1.45f
                        ),
                        radius = rippleRadius * 1.45f,
                        center = Offset(tip.x, tip.y + fg.tipHalfWidth * 0.15f)
                    )
                    drawCircle(
                        color = handAccentColor.copy(alpha = 0.75f),
                        radius = rippleRadius * 0.98f,
                        center = tip,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }

            // Build Dorsal Palm & Articulated Fingers Silhouette Path
            val handSilhouettePath = Path().apply {
                val firstF = articulatedFingers.first()
                val lastF = articulatedFingers.last()
                val wristHalfW = whiteKeyW * 1.28f

                val leftMcpEdge = Offset(firstF.mcpJoint.x - firstF.baseHalfWidth, firstF.mcpJoint.y)
                val rightMcpEdge = Offset(lastF.mcpJoint.x + lastF.baseHalfWidth, lastF.mcpJoint.y)

                moveTo(wristCenterX - wristHalfW, wristY)
                // Outer radial/ulnar contour from wrist to leftmost MCP knuckle
                cubicTo(
                    palmX - whiteKeyW * 1.62f * archSpreadScale.coerceAtMost(1.25f),
                    (wristY + palmY) * 0.52f,
                    leftMcpEdge.x - whiteKeyW * 0.20f,
                    leftMcpEdge.y + totalH * 0.09f,
                    leftMcpEdge.x,
                    leftMcpEdge.y
                )

                // Trace each 3-joint articulated finger
                for (i in articulatedFingers.indices) {
                    val fg = articulatedFingers[i]
                    val mcp = fg.mcpJoint
                    val pip = fg.pipJoint
                    val dip = fg.dipJoint
                    val tip = fg.tipCenter

                    val bw = fg.baseHalfWidth
                    val pw = fg.pipHalfWidth
                    val dw = fg.dipHalfWidth
                    val tw = fg.tipHalfWidth

                    // Left side of phalanx chain: MCP -> PIP -> DIP -> Tip
                    quadraticBezierTo(
                        (mcp.x - bw + pip.x - pw) * 0.5f,
                        (mcp.y + pip.y) * 0.5f,
                        pip.x - pw,
                        pip.y
                    )
                    quadraticBezierTo(
                        (pip.x - pw + dip.x - dw) * 0.5f,
                        (pip.y + dip.y) * 0.5f,
                        dip.x - dw,
                        dip.y
                    )
                    quadraticBezierTo(
                        dip.x - dw,
                        (dip.y + tip.y) * 0.5f,
                        tip.x - tw,
                        tip.y
                    )

                    // Anatomical rounded distal fingertip dome
                    cubicTo(
                        tip.x - tw,
                        tip.y - tw * 1.28f,
                        tip.x + tw,
                        tip.y - tw * 1.28f,
                        tip.x + tw,
                        tip.y
                    )

                    // Right side of phalanx chain: Tip -> DIP -> PIP -> MCP
                    quadraticBezierTo(
                        dip.x + dw,
                        (dip.y + tip.y) * 0.5f,
                        dip.x + dw,
                        dip.y
                    )
                    quadraticBezierTo(
                        (pip.x + pw + dip.x + dw) * 0.5f,
                        (pip.y + dip.y) * 0.5f,
                        pip.x + pw,
                        pip.y
                    )
                    quadraticBezierTo(
                        (mcp.x + bw + pip.x + pw) * 0.5f,
                        (mcp.y + pip.y) * 0.5f,
                        mcp.x + bw,
                        mcp.y
                    )

                    // Interdigital skinfold web between adjacent fingers
                    if (i < articulatedFingers.lastIndex) {
                        val nextFg = articulatedFingers[i + 1]
                        val nextLeftX = nextFg.mcpJoint.x - nextFg.baseHalfWidth
                        val nextLeftY = nextFg.mcpJoint.y
                        val isThumbWeb = fg.assignment.fingerNumber == 1 || nextFg.assignment.fingerNumber == 1
                        val webDepth = if (isThumbWeb) totalH * 0.048f else totalH * 0.024f
                        val webMidX = (mcp.x + bw + nextLeftX) * 0.5f
                        val webMidY = maxOf(mcp.y, nextLeftY) + webDepth
                        quadraticBezierTo(webMidX, webMidY, nextLeftX, nextLeftY)
                    }
                }

                // Outer contour from rightmost MCP knuckle back down to wrist
                cubicTo(
                    rightMcpEdge.x + whiteKeyW * 0.20f,
                    rightMcpEdge.y + totalH * 0.09f,
                    palmX + whiteKeyW * 1.62f * archSpreadScale.coerceAtMost(1.25f),
                    (wristY + palmY) * 0.52f,
                    wristCenterX + wristHalfW,
                    wristY
                )
                close()
            }

            // 2. Render Shadowed Dorsal Hand Silhouette & Internal Anatomical Shading
            withTransform({
                rotate(degrees = wristTiltDeg * 0.38f, pivot = Offset(palmX, palmY))
            }) {
                // Soft key-surface drop shadow beneath the entire hand
                val shadowOffsetLeft = if (isRightHand) with(this) { 2.5.dp.toPx() } else with(this) { -2.5.dp.toPx() }
                val shadowOffsetTop = with(this) { 4.dp.toPx() }
                withTransform({
                    translate(left = shadowOffsetLeft, top = shadowOffsetTop)
                }) {
                    drawPath(
                        path = handSilhouettePath,
                        color = Color.Black.copy(alpha = if (hasPressedNotes) 0.36f else 0.22f)
                    )
                }

                // Primary Subsurface-Scattered Dermal Fill
                drawPath(
                    path = handSilhouettePath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            skinHighlight,
                            skinMidtone,
                            skinSubsurface,
                            skinDeepShadow
                        ),
                        center = Offset(
                            x = palmX - (if (isRightHand) whiteKeyW * 0.32f else -whiteKeyW * 0.32f),
                            y = palmY - totalH * 0.16f
                        ),
                        radius = (whiteKeyW * 4.6f * archSpreadScale).coerceAtLeast(36f)
                    )
                )

                // Clip internal anatomical details (tendons, metacarpal valleys, knuckle dimples) to hand contour
                clipPath(handSilhouettePath) {
                    // Inter-metacarpal shadow valleys between tendons
                    for (i in 0 until articulatedFingers.lastIndex) {
                        val fA = articulatedFingers[i]
                        val fB = articulatedFingers[i + 1]
                        val valleyStart = Offset(
                            (fA.mcpJoint.x + fB.mcpJoint.x) * 0.5f,
                            (fA.mcpJoint.y + fB.mcpJoint.y) * 0.5f + totalH * 0.015f
                        )
                        val valleyEnd = Offset(
                            (valleyStart.x + wristCenterX) * 0.5f,
                            (valleyStart.y + palmY) * 0.52f
                        )
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    skinDeepShadow.copy(alpha = 0.34f),
                                    Color.Transparent
                                ),
                                start = valleyStart,
                                end = valleyEnd
                            ),
                            start = valleyStart,
                            end = valleyEnd,
                            strokeWidth = baseFingerHalfW * 0.65f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Extensor digitorum tendons radiating from carpal tunnel to MCP knuckles
                    articulatedFingers.forEach { fg ->
                        val fNum = fg.assignment.fingerNumber
                        val tendonOrigin = Offset(
                            x = wristCenterX + (fg.mcpJoint.x - wristCenterX) * 0.18f,
                            y = palmY + totalH * 0.03f
                        )
                        val tendonAlpha = if (fg.assignment.isPressed) 0.42f else 0.22f
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    skinHighlight.copy(alpha = tendonAlpha),
                                    skinHighlight.copy(alpha = tendonAlpha * 1.25f)
                                ),
                                start = tendonOrigin,
                                end = fg.mcpJoint
                            ),
                            start = tendonOrigin,
                            end = fg.mcpJoint,
                            strokeWidth = if (fNum == 1) 2.2.dp.toPx() else 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // MCP Knuckle prominence highlight & lateral dimple shadow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    skinHighlight.copy(alpha = if (fg.assignment.isPressed) 0.55f else 0.32f),
                                    Color.Transparent
                                ),
                                center = fg.mcpJoint,
                                radius = fg.baseHalfWidth * 0.95f
                            ),
                            radius = fg.baseHalfWidth * 0.95f,
                            center = fg.mcpJoint
                        )
                    }
                }

                // Subtle rim-light & accent contour along hand perimeter
                drawPath(
                    path = handSilhouettePath,
                    color = handAccentColor.copy(alpha = if (hasPressedNotes) 0.90f else 0.62f),
                    style = Stroke(
                        width = if (hasPressedNotes) 1.6.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Render Detailed Phalanges, PIP/DIP Joint Skin Creases, 3D Fingernails & HUD Badges
            // Sort so any finger crossing OVER is drawn last (on top), and thumb crossing UNDER is drawn first
            val depthSortedFingers = articulatedFingers.sortedBy { fg ->
                when {
                    fg.assignment.isCrossingUnder -> 0
                    fg.assignment.isCrossingOver -> 2
                    fg.assignment.isPressed -> 1
                    else -> 1
                }
            }

            fingerBadgePaint.textSize = (baseFingerHalfW * 1.38f).coerceIn(8.5.dp.toPx(), 12.5.dp.toPx())
            fingerNoteSubPaint.textSize = (baseFingerHalfW * 0.95f).coerceIn(6.5.dp.toPx(), 9.2.dp.toPx())

            depthSortedFingers.forEach { fg ->
                val fNum = fg.assignment.fingerNumber
                val isPressed = fg.assignment.isPressed
                val press = fg.pressProgress
                val mcp = fg.mcpJoint
                val pip = fg.pipJoint
                val dip = fg.dipJoint
                val tip = fg.tipCenter

                // Cylindrical 3D specular ridge along proximal & middle phalanges
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            skinHighlight.copy(alpha = 0.42f + 0.20f * press),
                            skinHighlight.copy(alpha = 0.25f)
                        ),
                        start = mcp,
                        end = dip
                    ),
                    start = mcp,
                    end = dip,
                    strokeWidth = fg.pipHalfWidth * 0.52f,
                    cap = StrokeCap.Round
                )

                // PIP Joint Double Skin Crease Wrinkles
                val pipCreaseW = fg.pipHalfWidth * 0.64f
                drawLine(
                    color = skinDeepShadow.copy(alpha = 0.48f + 0.18f * press),
                    start = Offset(pip.x - pipCreaseW, pip.y - 1.dp.toPx()),
                    end = Offset(pip.x + pipCreaseW, pip.y - 1.dp.toPx()),
                    strokeWidth = 1.0.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = skinDeepShadow.copy(alpha = 0.34f + 0.15f * press),
                    start = Offset(pip.x - pipCreaseW * 0.82f, pip.y + 1.2.dp.toPx()),
                    end = Offset(pip.x + pipCreaseW * 0.82f, pip.y + 1.2.dp.toPx()),
                    strokeWidth = 0.85.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // DIP Joint Skin Crease Wrinkle
                val dipCreaseW = fg.dipHalfWidth * 0.56f
                drawLine(
                    color = skinDeepShadow.copy(alpha = 0.44f + 0.18f * press),
                    start = Offset(dip.x - dipCreaseW, dip.y),
                    end = Offset(dip.x + dipCreaseW, dip.y),
                    strokeWidth = 0.9.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Ultrarealistic Fingernail Plate (Nail Bed + Lunula + Free Edge + Specular Glint)
                val tw = fg.tipHalfWidth
                val nailW = tw * (if (fNum == 1) 1.26f else 1.22f)
                val nailH = tw * (1.32f - 0.14f * press) // Foreshortens slightly when finger curls on press
                val nailTopY = tip.y - tw * 0.76f
                val nailLeftX = tip.x - nailW * 0.5f
                val nailCorner = CornerRadius(nailW * 0.42f, nailW * 0.42f)

                // Cuticle & lateral nail fold shadow
                drawRoundRect(
                    color = skinDeepShadow.copy(alpha = 0.52f),
                    topLeft = Offset(nailLeftX - 0.6.dp.toPx(), nailTopY - 0.5.dp.toPx()),
                    size = Size(nailW + 1.2.dp.toPx(), nailH + 1.2.dp.toPx()),
                    cornerRadius = nailCorner
                )

                // Pinkish keratin nail bed gradient (flushes warmer when pressed onto key)
                val nailBedTop = if (isPressed) Color(0xFFFFF4EE) else Color(0xFFFFEDE4).copy(alpha = 0.86f)
                val nailBedBot = if (isPressed) Color(0xFFFFCDD2) else Color(0xFFF5C6CB).copy(alpha = 0.82f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(nailBedTop, nailBedBot),
                        startY = nailTopY,
                        endY = nailTopY + nailH
                    ),
                    topLeft = Offset(nailLeftX, nailTopY),
                    size = Size(nailW, nailH),
                    cornerRadius = nailCorner
                )

                // Distal white free-edge crescent at top of nail
                val freeEdgeH = (nailH * 0.22f).coerceAtLeast(1.5.dp.toPx())
                drawRoundRect(
                    color = Color(0xFFFFFBF7).copy(alpha = if (isPressed) 0.95f else 0.78f),
                    topLeft = Offset(nailLeftX + nailW * 0.06f, nailTopY),
                    size = Size(nailW * 0.88f, freeEdgeH),
                    cornerRadius = CornerRadius(nailW * 0.38f, nailW * 0.38f)
                )

                // Proximal half-moon Lunula at bottom cuticle of nail
                val lunulaW = nailW * 0.58f
                val lunulaH = nailH * 0.24f
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isPressed) 0.68f else 0.45f),
                    topLeft = Offset(tip.x - lunulaW * 0.5f, nailTopY + nailH - lunulaH * 1.15f),
                    size = Size(lunulaW, lunulaH),
                    cornerRadius = CornerRadius(lunulaW * 0.5f, lunulaW * 0.5f)
                )

                // Glossy vertical specular highlight on nail plate
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isPressed) 0.72f else 0.42f),
                    topLeft = Offset(nailLeftX + nailW * 0.22f, nailTopY + nailH * 0.14f),
                    size = Size(nailW * 0.18f, nailH * 0.58f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Nail border outline (glows with hand accent when pressed)
                drawRoundRect(
                    color = if (isPressed) handAccentColor else skinDeepShadow.copy(alpha = 0.55f),
                    topLeft = Offset(nailLeftX, nailTopY),
                    size = Size(nailW, nailH),
                    cornerRadius = nailCorner,
                    style = Stroke(width = if (isPressed) 1.5.dp.toPx() else 0.8.dp.toPx())
                )

                // Glassmorphic Finger Number Badge & Pitch Telemetry
                val badgeY = tip.y + tw * 1.48f
                val badgeRad = (tw * (if (isPressed) 1.16f else 0.88f)).coerceAtLeast(6.8.dp.toPx())

                if (isPressed) {
                    // Outer neon halo on active finger badge
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                handAccentColor.copy(alpha = 0.65f),
                                handAccentColor.copy(alpha = 0.0f)
                            ),
                            center = Offset(tip.x, badgeY),
                            radius = badgeRad * 1.75f
                        ),
                        radius = badgeRad * 1.75f,
                        center = Offset(tip.x, badgeY)
                    )
                }

                // Badge body
                drawCircle(
                    brush = if (isPressed) {
                        Brush.verticalGradient(
                            colors = listOf(
                                handAccentColor,
                                handAccentColor.copy(
                                    red = handAccentColor.red * 0.72f,
                                    green = handAccentColor.green * 0.72f,
                                    blue = handAccentColor.blue * 0.72f
                                )
                            ),
                            startY = badgeY - badgeRad,
                            endY = badgeY + badgeRad
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E2430).copy(alpha = 0.82f),
                                Color(0xFF10141D).copy(alpha = 0.86f)
                            ),
                            startY = badgeY - badgeRad,
                            endY = badgeY + badgeRad
                        )
                    },
                    radius = badgeRad,
                    center = Offset(tip.x, badgeY)
                )

                // Crisp metallic/white rim on finger badge
                drawCircle(
                    color = if (isPressed) Color.White else handAccentColor.copy(alpha = 0.78f),
                    radius = badgeRad,
                    center = Offset(tip.x, badgeY),
                    style = Stroke(width = if (isPressed) 1.8.dp.toPx() else 1.0.dp.toPx())
                )

                fingerBadgePaint.color = if (isPressed) {
                    android.graphics.Color.WHITE
                } else {
                    android.graphics.Color.argb(215, 235, 242, 255)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    fNum.toString(),
                    tip.x,
                    badgeY + fingerBadgePaint.textSize * 0.36f,
                    fingerBadgePaint
                )

                // Floating Note Name Callout above pressed fingertip
                if (isPressed) {
                    val noteText = buildCompactPitchLabel(fg.assignment.midiNote)
                    val pillW = (badgeRad * 2.35f).coerceAtLeast(22.dp.toPx())
                    val pillH = (badgeRad * 1.25f).coerceAtLeast(12.dp.toPx())
                    val pillCenterY = (nailTopY - pillH * 0.72f).coerceAtLeast(pillH * 0.6f)

                    drawRoundRect(
                        color = Color(0xFF0E131F).copy(alpha = 0.88f),
                        topLeft = Offset(tip.x - pillW * 0.5f, pillCenterY - pillH * 0.5f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH * 0.5f, pillH * 0.5f)
                    )
                    drawRoundRect(
                        color = handAccentColor.copy(alpha = 0.92f),
                        topLeft = Offset(tip.x - pillW * 0.5f, pillCenterY - pillH * 0.5f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH * 0.5f, pillH * 0.5f),
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        noteText,
                        tip.x,
                        pillCenterY + fingerNoteSubPaint.textSize * 0.35f,
                        fingerNoteSubPaint
                    )
                }
            }

            // 4. Biometric Glassmorphic Wrist Telemetry Cuff
            val cuffW = (whiteKeyW * 2.45f).coerceIn(44.dp.toPx(), 86.dp.toPx())
            val cuffH = (whiteKeyW * 0.76f).coerceIn(15.dp.toPx(), 22.dp.toPx())
            val cuffX = (palmX * 0.68f + wristCenterX * 0.32f)
            val cuffY = (palmY - cuffH * 0.08f).coerceAtMost(totalH - cuffH * 0.58f - 2f)

            // Outer ambient glow on wrist cuff when playing
            if (hasPressedNotes) {
                drawRoundRect(
                    color = handAccentColor.copy(alpha = 0.32f),
                    topLeft = Offset(cuffX - cuffW * 0.5f - 2.dp.toPx(), cuffY - cuffH * 0.5f - 2.dp.toPx()),
                    size = Size(cuffW + 4.dp.toPx(), cuffH + 4.dp.toPx()),
                    cornerRadius = CornerRadius(cuffH * 0.55f, cuffH * 0.55f)
                )
            }

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF121726).copy(alpha = 0.92f),
                        handAccentColor.copy(alpha = 0.88f),
                        Color(0xFF121726).copy(alpha = 0.92f)
                    ),
                    startX = cuffX - cuffW * 0.5f,
                    endX = cuffX + cuffW * 0.5f
                ),
                topLeft = Offset(cuffX - cuffW * 0.5f, cuffY - cuffH * 0.5f),
                size = Size(cuffW, cuffH),
                cornerRadius = CornerRadius(cuffH * 0.5f, cuffH * 0.5f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = if (hasPressedNotes) 0.90f else 0.65f),
                topLeft = Offset(cuffX - cuffW * 0.5f, cuffY - cuffH * 0.5f),
                size = Size(cuffW, cuffH),
                cornerRadius = CornerRadius(cuffH * 0.5f, cuffH * 0.5f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            wristBadgePaint.textSize = (cuffH * 0.54f).coerceIn(8.dp.toPx(), 10.8.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                chordTelemetryLabel,
                cuffX,
                cuffY + wristBadgePaint.textSize * 0.36f,
                wristBadgePaint
            )
        }

        if (showLeftHand) {
            val lhMidiArr = floatArrayOf(0f, lhF1Midi, lhF2Midi, lhF3Midi, lhF4Midi, lhF5Midi)
            val lhPressArr = floatArrayOf(0f, lhF1Press, lhF2Press, lhF3Press, lhF4Press, lhF5Press)
            drawUltrarealisticHumanHand(
                assignments = lhSolution.assignments,
                animatedFingerMidi = lhMidiArr,
                animatedFingerPress = lhPressArr,
                animatedPalmMidi = animatedLhPalmMidi,
                wristTiltDeg = animatedLhTiltDeg,
                handAccentColor = leftHandColor,
                chordTelemetryLabel = lhSolution.chordLabel,
                isRightHand = false
            )
        }

        if (showRightHand) {
            val rhMidiArr = floatArrayOf(0f, rhF1Midi, rhF2Midi, rhF3Midi, rhF4Midi, rhF5Midi)
            val rhPressArr = floatArrayOf(0f, rhF1Press, rhF2Press, rhF3Press, rhF4Press, rhF5Press)
            drawUltrarealisticHumanHand(
                assignments = rhSolution.assignments,
                animatedFingerMidi = rhMidiArr,
                animatedFingerPress = rhPressArr,
                animatedPalmMidi = animatedRhPalmMidi,
                wristTiltDeg = animatedRhTiltDeg,
                handAccentColor = rightHandColor,
                chordTelemetryLabel = rhSolution.chordLabel,
                isRightHand = true
            )
        }
    }
}