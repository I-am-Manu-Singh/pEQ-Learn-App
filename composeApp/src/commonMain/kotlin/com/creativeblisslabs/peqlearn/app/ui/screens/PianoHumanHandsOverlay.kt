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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private data class HandFingerAssignment(
    val fingerNumber: Int, // 1 = Thumb, 2 = Index, 3 = Middle, 4 = Ring, 5 = Pinky
    val midiNote: Int,
    val isPressed: Boolean
)

/**
 * FIXED: Resolves all playable melodic/harmonic tracks for the Piano Human Hands Overlay.
 * - Strictly excludes GM Channel 10 Drums (channel == 9 or 10, or percussion programs 112..119).
 * - Does NOT falsely match "🎸 Bass (prog 33)" as a "Bass Clef Piano" track and drop Guitar/Vocal tracks!
 * - Includes all enabled non-drum tracks so multi-instrument scores (Guitar + Bass + Voice)
 *   are seamlessly arranged across Left Hand and Right Hand without skipping 4,400+ notes.
 */
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

    // Fallback if user soloed or all tracks had custom channels: allow all non-channel-9 tracks
    return tracks.filter { it.channel != 9 }.map { it.trackIndex }.toSet()
}

/**
 * Formats a MIDI pitch and finger assignment cleanly for forensic dumps without leaking '♯' onto the finger label.
 * Fixes the "A#1→F3♯" bug -> produces "A♯1→F5".
 */
internal fun formatCleanHandOverlayDiagnostic(midiNote: Int, fingerNumber: Int): String {
    val pitchNames = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    val octave = (midiNote / 12) - 1
    val pitchName = "${pitchNames[(midiNote % 12 + 12) % 12]}$octave"
    return "$pitchName→F${fingerNumber.coerceIn(1, 5)}"
}

/**
 * Clamps a single hand's chord to at most 5 anatomically playable notes within a 12-semitone span (max 14).
 * Preserves the outer shell (lowest bass root + highest melody note) and essential 3rd/7th guide tones,
 * dropping redundant inner octave/fifth doublings from 6-string guitar voicings.
 */
private fun clampChordToAnatomicalHand(
    sortedAsc: List<Int>,
    isRightHand: Boolean,
    maxSpanSemitones: Int = 12
): List<Int> {
    if (sortedAsc.isEmpty()) return emptyList()
    val distinctAsc = sortedAsc.distinct().sorted()

    // 1. Enforce biomechanical span ceiling anchored on melody top (RH) or bass root (LH)
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

    // 2. Keep lowest and highest notes; score inner notes to drop redundant octave/fifth doublings
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

/**
 * Strictly partitions all active notes into Disjoint Left Hand (LH) and Right Hand (RH) sets
 * using a Dynamic Programming split-point solver:
 * - Guaranteed: max(LH) < min(RH) whenever both hands are enabled (zero middle-note fighting).
 * - Guaranteed: Neither hand stretches > 12 semitones when the other hand can share the inner notes.
 * - Guaranteed: Drum channel notes (Track #4 / ch 9) are completely stripped.
 */
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
    lastRhCenter: Int
): Triple<List<Int>, List<Int>, Map<Int, Int>> {
    val melodicTrackSet = resolvePlayableMelodicTrackIndices(score)
    val currentMeasure = score?.measures?.getOrNull(playheadMeasure)
    val explicitFingers = mutableMapOf<Int, Int>()
    val validScorePitchesAtBeat = mutableSetOf<Int>()
    val drumPitchesAtBeat = mutableSetOf<Int>()

    if (currentMeasure != null) {
        val beatsPerMeasure = (currentMeasure.timeSignatureNumerator.toFloat() *
            (4f / currentMeasure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
        val curBeat = playheadOffset * beatsPerMeasure

        val sortedByProximity = currentMeasure.notes
            .filter { !it.isRest }
            .sortedBy { abs(it.position - curBeat) }

        for (n in sortedByProximity) {
            val isActiveAtBeat = curBeat >= (n.position - 0.18f) &&
                curBeat <= (n.position + n.duration.coerceAtLeast(0.125f) + 0.15f)
            if (n.voice !in melodicTrackSet) {
                if (isActiveAtBeat) drumPitchesAtBeat.add(n.midiNote)
                continue
            }
            if (isActiveAtBeat) {
                validScorePitchesAtBeat.add(n.midiNote)
            }
            if (n.fingering != null && n.fingering in 1..5 && !explicitFingers.containsKey(n.midiNote)) {
                explicitFingers[n.midiNote] = n.fingering
            }
        }
    }

    // Include all active MIDI notes except those that belong ONLY to a drum track at this beat
    val soundingPitches = activeMidiNotes.entries
        .filter { (pitch, isOn) ->
            isOn && pitch in 21..108 && (pitch in validScorePitchesAtBeat || pitch !in drumPitchesAtBeat)
        }
        .map { it.key }
        .distinct()
        .sorted()

    if (soundingPitches.isEmpty()) {
        return Triple(emptyList(), emptyList(), explicitFingers)
    }

    if (!showLeftHand && showRightHand) {
        return Triple(
            emptyList(),
            clampChordToAnatomicalHand(soundingPitches, isRightHand = true, maxSpanSemitones = 13),
            explicitFingers
        )
    }
    if (!showRightHand && showLeftHand) {
        return Triple(
            clampChordToAnatomicalHand(soundingPitches, isRightHand = false, maxSpanSemitones = 13),
            emptyList(),
            explicitFingers
        )
    }

    // Dynamic Programming Split Solver:
    // Test every split index k in 0..N where LH takes soundingPitches[0 until k] and RH takes soundingPitches[k until N]
    val n = soundingPitches.size
    var bestSplitIndex = 0
    var bestCost = Float.MAX_VALUE

    for (k in 0..n) {
        val candidateLh = soundingPitches.subList(0, k)
        val candidateRh = soundingPitches.subList(k, n)

        val lhSpan = if (candidateLh.size > 1) candidateLh.last() - candidateLh.first() else 0
        val rhSpan = if (candidateRh.size > 1) candidateRh.last() - candidateRh.first() else 0

        var cost = 0f

        // 1. Biomechanical span penalty (comfortable <= 12 semitones, hard wall > 14 semitones)
        if (lhSpan > 12) cost += (lhSpan - 12) * 65f
        if (lhSpan > 14) cost += (lhSpan - 14) * 400f
        if (rhSpan > 12) cost += (rhSpan - 12) * 65f
        if (rhSpan > 14) cost += (rhSpan - 14) * 400f

        // 2. Finger count overflow penalty (> 5 notes in one hand)
        if (candidateLh.size > 5) cost += (candidateLh.size - 5) * 90f
        if (candidateRh.size > 5) cost += (candidateRh.size - 5) * 90f

        // 3. Register affinity (LH prefers < 60, RH prefers >= 58) & smooth hand-center continuity
        if (candidateLh.isNotEmpty()) {
            val lhCenter = (candidateLh.first() + candidateLh.last()) * 0.5f
            cost += abs(lhCenter - lastLhCenter) * 1.4f
            for (p in candidateLh) {
                if (p >= 62) cost += (p - 60) * 16f
                if (activeNoteColors[p] == rightHandColor && p >= 57) cost += 12f
            }
        }

        if (candidateRh.isNotEmpty()) {
            val rhCenter = (candidateRh.first() + candidateRh.last()) * 0.5f
            cost += abs(rhCenter - lastRhCenter) * 1.4f
            for (p in candidateRh) {
                if (p < 53) cost += (55 - p) * 16f
                if (activeNoteColors[p] == leftHandColor && p <= 60) cost += 12f
            }
        }

        // 4. Reward splitting across a wide interval gap (e.g., bass root A#1 + guitar chord A#2..A#4)
        if (candidateLh.isNotEmpty() && candidateRh.isNotEmpty()) {
            val gap = candidateRh.first() - candidateLh.last()
            cost -= gap.coerceAtMost(18) * 3.5f
        }

        if (cost < bestCost) {
            bestCost = cost
            bestSplitIndex = k
        }
    }

    var rawLh = soundingPitches.subList(0, bestSplitIndex).toMutableList()
    var rawRh = soundingPitches.subList(bestSplitIndex, n).toMutableList()

    // Rescue pass: if RH still spans > 13 semitones (e.g. a 2-octave guitar chord A#2..A#4 while LH has A#1),
    // and LH cannot reach A#2 without stretching > 13 semitones from A#1, clamp each hand to its primary 12-semitone octave
    val clampedLh = clampChordToAnatomicalHand(rawLh, isRightHand = false, maxSpanSemitones = 12).toMutableList()
    val clampedRh = clampChordToAnatomicalHand(rawRh, isRightHand = true, maxSpanSemitones = 12).toMutableList()

    // If LH was empty and RH dropped low chord notes during clamping, hand those low chord notes to LH!
    if (clampedLh.isEmpty() && rawRh.size > clampedRh.size) {
        val droppedFromRh = rawRh.filter { it !in clampedRh && it < (clampedRh.firstOrNull() ?: 60) }
        clampedLh.addAll(clampChordToAnatomicalHand(droppedFromRh, isRightHand = false, maxSpanSemitones = 12))
    }

    return Triple(clampedLh.distinct().sorted(), clampedRh.distinct().sorted(), explicitFingers)
}

/**
 * Stateful biomechanical finger solver (1..5) for chords and melodic lines.
 * - Eliminates the "always F3" single-note lock by using directional interval stepping & thumb-crossing rules.
 * - Guarantees strict anatomical finger ordering so fingers never cross over each other.
 */
private fun solveHumanChordAndMelodyFingers(
    activePitchesAsc: List<Int>,
    explicitFingerMap: Map<Int, Int>,
    isRightHand: Boolean,
    lastAnchorMidi: Int,
    lastAnchorFinger: Int,
    territoryLimitMidi: Int
): Triple<Int, Int, List<HandFingerAssignment>> {
    val minAllowedMidi = if (isRightHand) territoryLimitMidi.coerceIn(21, 96) else 21
    val maxAllowedMidi = if (isRightHand) 108 else territoryLimitMidi.coerceIn(32, 108)

    val defaultHome = if (isRightHand) 67 else 48
    val safeAnchor = lastAnchorMidi.coerceIn(
        minOf(minAllowedMidi + 2, maxAllowedMidi),
        maxOf(minAllowedMidi, maxAllowedMidi - 2)
    )

    if (activePitchesAsc.isEmpty()) {
        val thumbMidi = if (isRightHand) {
            (safeAnchor - 4).coerceAtLeast(minAllowedMidi)
        } else {
            (safeAnchor + 4).coerceAtMost(maxAllowedMidi)
        }
        val idleFingers = (1..5).map { fNum ->
            val offsetSemis = when (fNum) {
                1 -> 0
                2 -> 2
                3 -> 4
                4 -> 5
                else -> 7
            } * (if (isRightHand) 1 else -1)
            HandFingerAssignment(
                fingerNumber = fNum,
                midiNote = (thumbMidi + offsetSemis).coerceIn(minAllowedMidi, maxAllowedMidi),
                isPressed = false
            )
        }
        return Triple(safeAnchor, lastAnchorFinger, idleFingers)
    }

    val sorted = clampChordToAnatomicalHand(activePitchesAsc, isRightHand, maxSpanSemitones = 12)
    val count = sorted.size
    val span = (sorted.last() - sorted.first()).coerceAtLeast(0)

    val rhThumbIsBlack = isRightHand && sorted.isNotEmpty() && isMidiNoteBlack(sorted.first())
    val lhThumbIsBlack = !isRightHand && sorted.isNotEmpty() && isMidiNoteBlack(sorted.last())

    // Safe black-key adjustment that preserves strictly distinct finger numbers
    fun adjustRhChordFingers(base: List<Int>): List<Int> {
        if (!rhThumbIsBlack || base.size >= 5) return base
        val shifted = base.map { (it + 1).coerceAtMost(5) }.distinct()
        return if (shifted.size == base.size) shifted else base
    }

    fun adjustLhChordFingers(base: List<Int>): List<Int> {
        if (!lhThumbIsBlack || base.size >= 5) return base
        // Base is ordered left-to-right (high finger num to low finger num, e.g. [5, 3, 1] -> [5, 3, 2])
        val shifted = base.mapIndexed { idx, f -> if (idx == base.lastIndex && f == 1) 2 else f }.distinct()
        return if (shifted.size == base.size) shifted else base
    }

    val fingersLeftToRight: List<Int> = if (isRightHand) {
        when (count) {
            1 -> {
                val n = sorted[0]
                val exp = explicitFingerMap[n]?.takeIf { it in 1..5 }
                if (exp != null) {
                    listOf(exp)
                } else {
                    val delta = n - safeAnchor
                    val black = isMidiNoteBlack(n)
                    val solved = when {
                        delta == 0 -> lastAnchorFinger.coerceIn(1, 5)
                        delta > 0 -> {
                            val step = ((delta + 1) / 2).coerceIn(1, 4)
                            val candidate = (lastAnchorFinger + step).coerceIn(1, 5)
                            if (black && candidate == 1) 2 else candidate
                        }
                        else -> {
                            val step = ((-delta + 1) / 2).coerceIn(1, 4)
                            val candidate = (lastAnchorFinger - step).coerceIn(1, 5)
                            if (black && candidate == 1) 2 else if (black && candidate == 5 && delta > -6) 4 else candidate
                        }
                    }
                    listOf(solved)
                }
            }
            2 -> adjustRhChordFingers(
                when {
                    span <= 2 -> listOf(1, 2)
                    span <= 4 -> listOf(1, 3)
                    span <= 6 -> listOf(1, 4)
                    else -> listOf(1, 5)
                }
            )
            3 -> {
                val lowGap = sorted[1] - sorted[0]
                val highGap = sorted[2] - sorted[1]
                adjustRhChordFingers(
                    when {
                        span <= 4 -> listOf(1, 2, 3)
                        lowGap <= 3 && highGap >= 5 -> listOf(1, 2, 5)
                        lowGap >= 5 && highGap <= 3 -> listOf(1, 4, 5)
                        else -> listOf(1, 3, 5)
                    }
                )
            }
            4 -> {
                val topGap = sorted[3] - sorted[2]
                adjustRhChordFingers(if (topGap <= 2) listOf(1, 2, 4, 5) else listOf(1, 2, 3, 5))
            }
            else -> listOf(1, 2, 3, 4, 5)
        }
    } else {
        when (count) {
            1 -> {
                val n = sorted[0]
                val exp = explicitFingerMap[n]?.takeIf { it in 1..5 }
                if (exp != null) {
                    listOf(exp)
                } else {
                    val delta = n - safeAnchor
                    val black = isMidiNoteBlack(n)
                    // For LH: ascending pitch (+delta) moves from pinky (5) toward thumb (1)
                    val solved = when {
                        delta == 0 -> lastAnchorFinger.coerceIn(1, 5)
                        delta > 0 -> {
                            val step = ((delta + 1) / 2).coerceIn(1, 4)
                            val candidate = (lastAnchorFinger - step).coerceIn(1, 5)
                            if (black && candidate == 1) 2 else candidate
                        }
                        else -> {
                            val step = ((-delta + 1) / 2).coerceIn(1, 4)
                            val candidate = (lastAnchorFinger + step).coerceIn(1, 5)
                            if (black && candidate == 5 && n >= 42) 4 else candidate
                        }
                    }
                    listOf(solved)
                }
            }
            2 -> adjustLhChordFingers(
                when {
                    span <= 2 -> listOf(2, 1)
                    span <= 4 -> listOf(3, 1)
                    span <= 6 -> listOf(4, 1)
                    else -> listOf(5, 1)
                }
            )
            3 -> {
                val lowGap = sorted[1] - sorted[0]
                adjustLhChordFingers(
                    when {
                        span <= 4 -> listOf(3, 2, 1)
                        lowGap >= 5 -> listOf(5, 2, 1)
                        else -> listOf(5, 3, 1)
                    }
                )
            }
            4 -> {
                val lowGap = sorted[1] - sorted[0]
                adjustLhChordFingers(if (lowGap <= 2) listOf(5, 4, 2, 1) else listOf(5, 3, 2, 1))
            }
            else -> listOf(5, 4, 3, 2, 1)
        }
    }

    val pressedByFinger = mutableMapOf<Int, Int>()
    sorted.forEachIndexed { idx, pitch ->
        val fNum = fingersLeftToRight.getOrElse(idx) { if (isRightHand) idx + 1 else 5 - idx }.coerceIn(1, 5)
        pressedByFinger[fNum] = pitch
    }

    // Compute all 5 fingers in anatomical order and interpolate unpressed fingers cleanly between pressed anchors
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
                    (pLow + (pHigh - pLow) * frac).toInt()
                }
                lowerPressed != null -> {
                    val pLow = pressedByFinger[lowerPressed]!!
                    val diff = fNum - lowerPressed
                    pLow + (if (isRightHand) diff * 2 else -diff * 2)
                }
                higherPressed != null -> {
                    val pHigh = pressedByFinger[higherPressed]!!
                    val diff = higherPressed - fNum
                    pHigh - (if (isRightHand) diff * 2 else -diff * 2)
                }
                else -> defaultHome
            }
            HandFingerAssignment(
                fingerNumber = fNum,
                midiNote = naturalPitch.coerceIn(minAllowedMidi, maxAllowedMidi),
                isPressed = false
            )
        }
    }

    val centerMidi = ((sorted.first() + sorted.last()) / 2).coerceIn(24, 104)
    val primaryFinger = fingersLeftToRight.firstOrNull() ?: 3
    return Triple(centerMidi, primaryFinger, allFiveFingers)
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
    modifier: Modifier = Modifier
) {
    if (!showLeftHand && !showRightHand) return

    val safeStart = minOf(startNote, endNote).coerceIn(21, 108)
    val safeEnd = maxOf(startNote, endNote).coerceIn(21, 108)
    val whiteNotesInRange = remember(safeStart, safeEnd) {
        (safeStart..safeEnd).filter { !isMidiNoteBlack(it) }
    }
    val whiteCount = whiteNotesInRange.size.coerceAtLeast(1)

    // Use non-reactive refs for historical hand state to prevent 1-frame LaunchedEffect race conditions
    val lastLhCenterRef = remember { mutableIntStateOf(46) } // A#2 / C3 default LH home
    val lastRhCenterRef = remember { mutableIntStateOf(68) } // G#4 / A4 default RH home
    val lastLhFingerRef = remember { mutableIntStateOf(3) }
    val lastRhFingerRef = remember { mutableIntStateOf(2) }

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
        showRightHand
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
            lastLhCenter = lastLhCenterRef.intValue,
            lastRhCenter = lastRhCenterRef.intValue
        )
    }

    val disjointLh = partitioned.first
    val disjointRh = partitioned.second
    val explicitFingers = partitioned.third

    // Dynamic territory boundary: only restrict LH ceiling by RH's actual active notes (or a high fallback when RH is idle)
    val rhLowestBound = disjointRh.firstOrNull() ?: (lastRhCenterRef.intValue - 3).coerceAtLeast(62)
    val lhHighestBound = disjointLh.lastOrNull() ?: (lastLhCenterRef.intValue + 3).coerceAtMost(57)
    val lhTerritoryCeiling = if (showRightHand) (rhLowestBound - 1).coerceAtLeast(40) else 108
    val rhTerritoryFloor = if (showLeftHand) (lhHighestBound + 1).coerceAtMost(84) else 21

    val lhSolution = remember(disjointLh, explicitFingers, lhTerritoryCeiling) {
        solveHumanChordAndMelodyFingers(
            activePitchesAsc = disjointLh,
            explicitFingerMap = explicitFingers,
            isRightHand = false,
            lastAnchorMidi = lastLhCenterRef.intValue,
            lastAnchorFinger = lastLhFingerRef.intValue,
            territoryLimitMidi = lhTerritoryCeiling
        ).also { (newCenter, newFinger, _) ->
            if (disjointLh.isNotEmpty()) {
                lastLhCenterRef.intValue = newCenter.coerceAtMost(lhTerritoryCeiling)
                lastLhFingerRef.intValue = newFinger
            }
        }
    }

    val rhSolution = remember(disjointRh, explicitFingers, rhTerritoryFloor) {
        solveHumanChordAndMelodyFingers(
            activePitchesAsc = disjointRh,
            explicitFingerMap = explicitFingers,
            isRightHand = true,
            lastAnchorMidi = lastRhCenterRef.intValue,
            lastAnchorFinger = lastRhFingerRef.intValue,
            territoryLimitMidi = rhTerritoryFloor
        ).also { (newCenter, newFinger, _) ->
            if (disjointRh.isNotEmpty()) {
                lastRhCenterRef.intValue = newCenter.coerceAtLeast(rhTerritoryFloor)
                lastRhFingerRef.intValue = newFinger
            }
        }
    }

    val lhAssignments = lhSolution.third
    val rhAssignments = rhSolution.third

    val animatedLhCenterMidi by animateFloatAsState(
        targetValue = (if (disjointLh.isNotEmpty()) lhSolution.first else lastLhCenterRef.intValue)
            .coerceAtMost(lhTerritoryCeiling).toFloat(),
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 1200f),
        label = "lh_fast_glide"
    )

    val animatedRhCenterMidi by animateFloatAsState(
        targetValue = (if (disjointRh.isNotEmpty()) rhSolution.first else lastRhCenterRef.intValue)
            .coerceAtLeast(rhTerritoryFloor).toFloat(),
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 1200f),
        label = "rh_fast_glide"
    )

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

        /**
         * FIXED: Computes the exact physical X center for BOTH White Keys and Black Keys!
         * Previously, black keys used `(relWhiteIdx + 0.5f) * whiteKeyW`, which drew every black key
         * on top of the white key to its left/right instead of on the boundary between white keys.
         */
        fun midiToExactX(midi: Int): Float {
            val clamped = midi.coerceIn(safeStart, safeEnd)
            val startGeom = WaterfallKeyGeometryLookup.keyGeometries[safeStart]
            if (!isMidiNoteBlack(clamped)) {
                val geom = WaterfallKeyGeometryLookup.keyGeometries[clamped]
                val relWhiteIdx = geom.whiteKeyIndex - startGeom.whiteKeyIndex
                return (relWhiteIdx + 0.5f) * whiteKeyW
            } else {
                // Black key sits on the seam between lowerWhite (clamped - 1) and upperWhite (clamped + 1)
                val lowerWhiteMidi = (clamped - 1).coerceAtLeast(safeStart)
                val lowerGeom = WaterfallKeyGeometryLookup.keyGeometries[lowerWhiteMidi]
                val relWhiteIdx = lowerGeom.whiteKeyIndex - startGeom.whiteKeyIndex
                val pc = (clamped % 12 + 12) % 12
                val seamBias = when (pc) {
                    1 -> -0.04f  // C# slightly left
                    3 -> 0.04f   // D# slightly right
                    6 -> -0.05f  // F# slightly left
                    8 -> 0.0f    // G# centered
                    10 -> 0.05f  // A# slightly right
                    else -> 0f
                }
                return (relWhiteIdx + 1.0f + seamBias) * whiteKeyW
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
            handAccentColor: Color,
            isRightHand: Boolean
        ) {
            val orderedLeftToRight = if (isRightHand) {
                assignments.sortedBy { it.fingerNumber } // RH: 1 (Thumb) on Left -> 5 (Pinky) on Right
            } else {
                assignments.sortedByDescending { it.fingerNumber } // LH: 5 (Pinky) on Left -> 1 (Thumb) on Right
            }

            val pressedIndices = orderedLeftToRight.indices.filter { orderedLeftToRight[it].isPressed }
            val hasPressedNotes = pressedIndices.isNotEmpty()
            val anyBlackPressed = orderedLeftToRight.any { it.isPressed && isMidiNoteBlack(it.midiNote) }
            val palmShiftY = if (anyBlackPressed) -totalH * 0.05f else 0f

            // Anchor the palm X directly to the centroid of pressed keys when playing so the wrist never lags behind fast chords
            val targetPalmX = if (hasPressedNotes) {
                val firstPressedX = midiToExactX(orderedLeftToRight[pressedIndices.first()].midiNote)
                val lastPressedX = midiToExactX(orderedLeftToRight[pressedIndices.last()].midiNote)
                (firstPressedX + lastPressedX) * 0.5f
            } else {
                val rawPalmX = interpolateMidiX(animatedPalmMidi)
                if (showLeftHand && showRightHand) {
                    if (isRightHand) rawPalmX.coerceAtLeast(territoryBorderX + whiteKeyW * 1.5f)
                    else rawPalmX.coerceAtMost(territoryBorderX - whiteKeyW * 1.5f)
                } else rawPalmX
            }

            val palmX = targetPalmX.coerceIn(whiteKeyW * 1.5f, totalW - whiteKeyW * 1.5f)
            val palmY = (totalH * 0.89f + palmShiftY).coerceIn(totalH * 0.72f, totalH * 0.94f)
            val wristY = totalH + 10.dp.toPx()
            val wristCenterX = palmX + (if (isRightHand) whiteKeyW * 0.50f else -whiteKeyW * 0.50f)

            data class OrganicFinger(
                val item: HandFingerAssignment,
                val baseLeft: Offset,
                val baseRight: Offset,
                val tipCenter: Offset,
                val halfWidth: Float
            )

            val baseFingerHalfW = (whiteKeyW * 0.27f).coerceIn(4.2f, 10.5.dp.toPx())
            val fingerGeoms = ArrayList<OrganicFinger>(5)

            // FIXED: Compute strictly monotonic left-to-right finger tip X positions.
            // Pressed fingers lock to their exact key X; unpressed fingers interpolate smoothly between pressed anchors!
            val rawTipXs = FloatArray(5)
            val naturalSlotStepPx = whiteKeyW * 0.82f

            if (!hasPressedNotes) {
                for (i in 0 until 5) {
                    rawTipXs[i] = palmX + (i - 2f) * naturalSlotStepPx
                }
            } else {
                // 1. Lock all pressed finger slots to their exact physical key centers
                for (idx in pressedIndices) {
                    rawTipXs[idx] = midiToExactX(orderedLeftToRight[idx].midiNote)
                }
                // 2. Extrapolate unpressed fingers to the LEFT of the first pressed finger
                val firstP = pressedIndices.first()
                for (i in (firstP - 1) downTo 0) {
                    rawTipXs[i] = rawTipXs[i + 1] - naturalSlotStepPx
                }
                // 3. Extrapolate unpressed fingers to the RIGHT of the last pressed finger
                val lastP = pressedIndices.last()
                for (i in (lastP + 1) until 5) {
                    rawTipXs[i] = rawTipXs[i - 1] + naturalSlotStepPx
                }
                // 4. Linearly interpolate any unpressed fingers BETWEEN two pressed fingers
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

            orderedLeftToRight.forEachIndexed { slotIdx, item ->
                val fNum = item.fingerNumber
                val tipX = rawTipXs[slotIdx].coerceIn(baseFingerHalfW, totalW - baseFingerHalfW)
                val isBlack = isMidiNoteBlack(item.midiNote)

                val fingerReachFrac = when (fNum) {
                    1 -> 0.27f // Thumb
                    2 -> 0.44f // Index
                    3 -> 0.50f // Middle (longest)
                    4 -> 0.46f // Ring
                    else -> 0.36f // Pinky
                }

                val pressCurlY = if (item.isPressed) totalH * 0.03f else 0f
                val tipY = if (isBlack && item.isPressed) {
                    blackKeyH * 0.78f
                } else if (isBlack) {
                    blackKeyH * 0.88f
                } else {
                    (palmY - totalH * fingerReachFrac) + pressCurlY
                }.coerceIn(totalH * 0.14f, totalH * 0.78f)

                val slotFromCenter = (slotIdx - 2f)
                val archBaseX = palmX + (slotFromCenter * whiteKeyW * 0.56f) + ((tipX - palmX) * 0.38f)
                val archBaseY = (palmY - totalH * 0.15f) + (slotFromCenter * slotFromCenter * totalH * 0.014f) +
                    (if (fNum == 1) totalH * 0.07f else 0f)

                val fHalfW = when (fNum) {
                    1 -> baseFingerHalfW * 1.12f
                    5 -> baseFingerHalfW * 0.84f
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

            // Build Unified Continuous Organic Hand Silhouette Path
            val handOutlinePath = Path().apply {
                val firstF = fingerGeoms.first()
                val lastF = fingerGeoms.last()
                val wristHalfW = whiteKeyW * 1.35f

                moveTo(wristCenterX - wristHalfW, wristY)
                cubicTo(
                    palmX - whiteKeyW * 1.65f,
                    (wristY + palmY) * 0.5f,
                    firstF.baseLeft.x - whiteKeyW * 0.25f,
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
                        fg.baseLeft.x * 0.55f + (tip.x - tipHw) * 0.45f,
                        fg.baseLeft.y * 0.55f + tip.y * 0.45f,
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
                        fg.baseRight.x * 0.55f + (tip.x + tipHw) * 0.45f,
                        fg.baseRight.y * 0.55f + tip.y * 0.45f,
                        fg.baseRight.x,
                        fg.baseRight.y
                    )

                    if (i < fingerGeoms.lastIndex) {
                        val nextFg = fingerGeoms[i + 1]
                        val webMidX = (fg.baseRight.x + nextFg.baseLeft.x) * 0.5f
                        val webMidY = maxOf(fg.baseRight.y, nextFg.baseLeft.y) + totalH * 0.025f
                        quadraticBezierTo(webMidX, webMidY, nextFg.baseLeft.x, nextFg.baseLeft.y)
                    }
                }

                cubicTo(
                    lastF.baseRight.x + whiteKeyW * 0.25f,
                    lastF.baseRight.y + totalH * 0.08f,
                    palmX + whiteKeyW * 1.65f,
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
                    radius = (whiteKeyW * 4.2f).coerceAtLeast(32f)
                )
            )

            drawPath(
                path = handOutlinePath,
                color = handAccentColor.copy(alpha = 0.88f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

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
                assignments = lhAssignments,
                animatedPalmMidi = animatedLhCenterMidi,
                handAccentColor = leftHandColor,
                isRightHand = false
            )
        }
        if (showRightHand) {
            drawOrganicHumanHand(
                assignments = rhAssignments,
                animatedPalmMidi = animatedRhCenterMidi,
                handAccentColor = rightHandColor,
                isRightHand = true
            )
        }
    }
}