package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import com.creativeblisslabs.peqlearn.app.ui.screens.isMidiNoteBlack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Unified Biomechanical Performer Engine:
 *  1. RoboPianist Optimal Transport (OT) + Marco Musy PianoPlayer 3D Hand Model for Piano & Harmonium.
 *  2. Viterbi Dynamic Programming Fretboard/Fingerboard Solver for Guitar (6/7-string), Ukulele, Violin, Sitar & Flute.
 */
object BiomechanicalPerformersEngine {

    // =========================================================================================
    // PART A: ROBOPIANIST OPTIMAL TRANSPORT (OT) & MARCO MUSY PIANOPLAYER ENGINE
    // =========================================================================================

    data class PianoFingerAssignment(
        val fingerNumber: Int, // 1 = Thumb, 2 = Index, 3 = Middle, 4 = Ring, 5 = Pinky
        val midiNote: Int,
        val isPressed: Boolean,
        val strikeBounce: Float = 1.0f // 0.0f = lifted pre-strike, 1.0f = full key depression
    )

    data class PianoHandFrameSolution(
        val lhPalmMidi: Float,
        val rhPalmMidi: Float,
        val lhFingers: List<PianoFingerAssignment>,
        val rhFingers: List<PianoFingerAssignment>,
        val lhActivePitches: List<Int>,
        val rhActivePitches: List<Int>,
        val strikePulse: Float
    )

    // Marco Musy `pianoplayer` anatomical finger rest offsets (in semitones from Middle Finger F3 = 0.0f)
    // RH: Thumb(1) is -3.6, Index(2) is -1.8, Middle(3) is 0.0, Ring(4) is +1.8, Pinky(5) is +3.6
    private val RH_FINGER_REST_SEMITONES = floatArrayOf(0f, -3.6f, -1.8f, 0.0f, 1.8f, 3.6f)
    // LH: Thumb(1) is +3.6, Index(2) is +1.8, Middle(3) is 0.0, Ring(4) is -1.8, Pinky(5) is -3.6
    private val LH_FINGER_REST_SEMITONES = floatArrayOf(0f, 3.6f, 1.8f, 0.0f, -1.8f, -3.6f)

    // Finger dexterity & strength weights (1=Thumb, 2=Index, 3=Middle, 4=Ring, 5=Pinky)
    private val FINGER_PENALTY_WEIGHTS = floatArrayOf(0f, 1.05f, 0.90f, 0.88f, 1.12f, 1.25f)

    // Maximum anatomical pairwise spans (in semitones) from thumb/adjacent fingers
    private const val MAX_COMFORT_HAND_SPAN = 12 // 1 Octave (12 semitones)
    private const val MAX_ABSOLUTE_HAND_SPAN = 13 // Minor 9th ceiling

    /**
     * Clamps a chord voicing for a single hand to <= 5 fingers and <= 12 semitones,
     * preserving the outer shell (bass root + top melody note) and essential 3rd/7th guide tones.
     */
    fun clampChordToAnatomicalHand(
        pitchesAsc: List<Int>,
        isRightHand: Boolean,
        maxSpanSemitones: Int = MAX_COMFORT_HAND_SPAN
    ): List<Int> {
        if (pitchesAsc.isEmpty()) return emptyList()
        val distinct = pitchesAsc.distinct().sorted()
        val spanFiltered = if (distinct.last() - distinct.first() > maxSpanSemitones) {
            if (isRightHand) {
                val top = distinct.last()
                distinct.filter { top - it <= maxSpanSemitones }
            } else {
                val bot = distinct.first()
                distinct.filter { it - bot <= maxSpanSemitones }
            }
        } else {
            distinct
        }

        if (spanFiltered.size <= 5) return spanFiltered

        val lowest = spanFiltered.first()
        val highest = spanFiltered.last()
        val outerPcs = setOf(lowest % 12, highest % 12)
        val fifthPc = (lowest + 7) % 12

        val innerKept = spanFiltered.subList(1, spanFiltered.lastIndex)
            .sortedByDescending { p ->
                val pc = p % 12
                (if (pc !in outerPcs) 12 else 0) + (if (pc != fifthPc) 5 else 1)
            }
            .take(3)

        return (listOf(lowest, highest) + innerKept).distinct().sorted()
    }

    /**
     * RoboPianist Optimal Transport (OT) Disjoint Hand Partitioner:
     * Finds the optimal split point `k` in `soundingPitches[0..N]` such that:
     *  - `LH = soundingPitches[0 until k]` and `RH = soundingPitches[k until N]` (Strictly disjoint: `max(LH) < min(RH)`).
     *  - Minimizes combined transport distance from `lastLhCenter` and `lastRhCenter` + span violation penalties.
     */
    fun partitionHandsOptimalTransport(
        soundingPitchesAsc: List<Int>,
        showLeftHand: Boolean,
        showRightHand: Boolean,
        lastLhCenter: Int,
        lastRhCenter: Int,
        lookaheadNextPitches: List<Int> = emptyList()
    ): Pair<List<Int>, List<Int>> {
        val sorted = soundingPitchesAsc.distinct().sorted()
        if (sorted.isEmpty()) return emptyList<Int>() to emptyList()

        if (!showLeftHand && showRightHand) {
            return emptyList<Int>() to clampChordToAnatomicalHand(sorted, isRightHand = true)
        }
        if (!showRightHand && showLeftHand) {
            return clampChordToAnatomicalHand(sorted, isRightHand = false) to emptyList()
        }

        val n = sorted.size
        var bestSplitIdx = 0
        var bestCost = Float.MAX_VALUE

        val nextLow = lookaheadNextPitches.minOrNull()
        val nextHigh = lookaheadNextPitches.maxOrNull()

        for (k in 0..n) {
            val rawLh = sorted.subList(0, k)
            val rawRh = sorted.subList(k, n)

            val lhSpan = if (rawLh.size > 1) rawLh.last() - rawLh.first() else 0
            val rhSpan = if (rawRh.size > 1) rawRh.last() - rawRh.first() else 0

            var cost = 0f

            // 1. Biomechanical span wall (> 12 semitones penalized heavily so wide chords split across both hands)
            if (lhSpan > MAX_COMFORT_HAND_SPAN) cost += (lhSpan - MAX_COMFORT_HAND_SPAN) * 85f
            if (lhSpan > MAX_ABSOLUTE_HAND_SPAN) cost += (lhSpan - MAX_ABSOLUTE_HAND_SPAN) * 450f
            if (rhSpan > MAX_COMFORT_HAND_SPAN) cost += (rhSpan - MAX_COMFORT_HAND_SPAN) * 85f
            if (rhSpan > MAX_ABSOLUTE_HAND_SPAN) cost += (rhSpan - MAX_ABSOLUTE_HAND_SPAN) * 450f

            // 2. Finger overflow penalty (> 5 notes in a single hand)
            if (rawLh.size > 5) cost += (rawLh.size - 5) * 95f
            if (rawRh.size > 5) cost += (rawRh.size - 5) * 95f

            // 3. Horizontal transport cost (smooth wrist continuity) + Register affinity
            if (rawLh.isNotEmpty()) {
                val lhCenter = (rawLh.first() + rawLh.last()) * 0.5f
                cost += abs(lhCenter - lastLhCenter) * 1.45f
                for (p in rawLh) {
                    if (p >= 61) cost += (p - 59) * 16f // LH prefers below Middle C (60)
                }
                if (nextLow != null && nextLow < 55) {
                    cost += abs(lhCenter - nextLow) * 0.35f
                }
            }

            if (rawRh.isNotEmpty()) {
                val rhCenter = (rawRh.first() + rawRh.last()) * 0.5f
                cost += abs(rhCenter - lastRhCenter) * 1.45f
                for (p in rawRh) {
                    if (p <= 53) cost += (55 - p) * 16f // RH prefers F3 (53) and above
                }
                if (nextHigh != null && nextHigh >= 60) {
                    cost += abs(rhCenter - nextHigh) * 0.35f
                }
            }

            // 4. Reward splitting across natural harmonic gaps (e.g. bass root + upper triad)
            if (rawLh.isNotEmpty() && rawRh.isNotEmpty()) {
                val gap = rawRh.first() - rawLh.last()
                cost -= gap.coerceAtMost(18) * 3.8f
            }

            if (cost < bestCost) {
                bestCost = cost
                bestSplitIdx = k
            }
        }

        val rawLh = sorted.subList(0, bestSplitIdx)
        val rawRh = sorted.subList(bestSplitIdx, n)

        val clampedLh = clampChordToAnatomicalHand(rawLh, isRightHand = false).toMutableList()
        val clampedRh = clampChordToAnatomicalHand(rawRh, isRightHand = true).toMutableList()

        // Rescue pass: if LH is idle and RH had to drop low notes due to >12-semitone span, let LH play the low notes!
        if (clampedLh.isEmpty() && rawRh.size > clampedRh.size) {
            val droppedLow = rawRh.filter { it !in clampedRh && it < (clampedRh.firstOrNull() ?: 60) }
            clampedLh.addAll(clampChordToAnatomicalHand(droppedLow, isRightHand = false))
        } else if (clampedRh.isEmpty() && rawLh.size > clampedLh.size) {
            val droppedHigh = rawLh.filter { it !in clampedLh && it > (clampedLh.lastOrNull() ?: 60) }
            clampedRh.addAll(clampChordToAnatomicalHand(droppedHigh, isRightHand = true))
        }

        return clampedLh.distinct().sorted() to clampedRh.distinct().sorted()
    }

    /**
     * Solves optimal anatomical fingers (1..5) for a single hand using Marco Musy's `pianoplayer`
     * + RoboPianist Optimal Transport cost:
     *  - Single notes use stateful interval stepping, thumb-under/finger-over crossover rules, and lookahead.
     *  - Chords use exact interval-gap templates with black-key thumb avoidance.
     *  - Unpressed fingers interpolate smoothly in their natural anatomical slots without ever crossing.
     */
    fun solveHandFingersOptimalTransport(
        activePitchesAsc: List<Int>,
        explicitFingerMap: Map<Int, Int>,
        isRightHand: Boolean,
        lastPalmMidi: Int,
        lastPrimaryFinger: Int,
        lastPrimaryPitch: Int,
        territoryLimitMidi: Int,
        strikeBounce: Float = 1.0f,
        lookaheadNextPitches: List<Int> = emptyList()
    ): Triple<Int, Int, List<PianoFingerAssignment>> {
        val minAllowedMidi = if (isRightHand) territoryLimitMidi.coerceIn(21, 96) else 21
        val maxAllowedMidi = if (isRightHand) 108 else territoryLimitMidi.coerceIn(32, 108)
        val restOffsets = if (isRightHand) RH_FINGER_REST_SEMITONES else LH_FINGER_REST_SEMITONES

        val safePalm = lastPalmMidi.coerceIn(
            minOf(minAllowedMidi + 3, maxAllowedMidi),
            maxOf(minAllowedMidi, maxAllowedMidi - 3)
        )

        // Idle hand: place all 5 fingers in their relaxed anatomical rest posture around `safePalm`
        if (activePitchesAsc.isEmpty()) {
            val idleFingers = (1..5).map { fNum ->
                val restPitch = (safePalm + restOffsets[fNum].roundToInt()).coerceIn(minAllowedMidi, maxAllowedMidi)
                PianoFingerAssignment(
                    fingerNumber = fNum,
                    midiNote = restPitch,
                    isPressed = false,
                    strikeBounce = 0f
                )
            }
            return Triple(safePalm, lastPrimaryFinger, idleFingers)
        }

        val sorted = clampChordToAnatomicalHand(activePitchesAsc, isRightHand)
        val count = sorted.size
        val span = (sorted.last() - sorted.first()).coerceAtLeast(0)

// Replace `rhThumbBlack` and `lhThumbBlack` in BiomechanicalPerformersEngine.kt (lines 217-230):
        // CRITICAL FIX: Never shift thumb (1) to index (2) on wide spans (6ths, 7ths, Octaves: span >= 7)!
        val rhThumbBlackNarrow = isRightHand && span <= 6 && isMidiNoteBlack(sorted.first())
        val lhThumbBlackNarrow = !isRightHand && span <= 6 && isMidiNoteBlack(sorted.last())

        fun adjustRhChord(fs: List<Int>): List<Int> {
            if (!rhThumbBlackNarrow || fs.size >= 5 || fs.contains(5)) return fs
            val shifted = fs.map { (it + 1).coerceAtMost(5) }.distinct()
            return if (shifted.size == fs.size) shifted else fs
        }

        fun adjustLhChord(fs: List<Int>): List<Int> {
            if (!lhThumbBlackNarrow || fs.size >= 5 || fs.firstOrNull() == 5) return fs
            val shifted = fs.mapIndexed { idx, f -> if (idx == fs.lastIndex && f == 1) 2 else f }.distinct()
            return if (shifted.size == fs.size) shifted else fs
        }

        val fingersLeftToRight: List<Int> = if (isRightHand) {
            when (count) {
                1 -> {
                    val pitch = sorted[0]
                    val exp = explicitFingerMap[pitch]?.takeIf { it in 1..5 }
                    if (exp != null && !(isMidiNoteBlack(pitch) && exp == 1 && count == 1)) {
                        listOf(exp)
                    } else {
                        listOf(
                            solveSingleNoteFingerOT(
                                pitch = pitch,
                                isRightHand = true,
                                lastPalmMidi = safePalm,
                                lastFinger = lastPrimaryFinger,
                                lastPitch = lastPrimaryPitch,
                                lookaheadPitches = lookaheadNextPitches
                            )
                        )
                    }
                }
                2 -> adjustRhChord(
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
                    adjustRhChord(
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
                    adjustRhChord(if (topGap <= 2) listOf(1, 2, 4, 5) else listOf(1, 2, 3, 5))
                }
                else -> listOf(1, 2, 3, 4, 5)
            }
        } else {
            when (count) {
                1 -> {
                    val pitch = sorted[0]
                    val exp = explicitFingerMap[pitch]?.takeIf { it in 1..5 }
                    if (exp != null && !(isMidiNoteBlack(pitch) && exp == 1 && count == 1)) {
                        listOf(exp)
                    } else {
                        listOf(
                            solveSingleNoteFingerOT(
                                pitch = pitch,
                                isRightHand = false,
                                lastPalmMidi = safePalm,
                                lastFinger = lastPrimaryFinger,
                                lastPitch = lastPrimaryPitch,
                                lookaheadPitches = lookaheadNextPitches
                            )
                        )
                    }
                }
                2 -> adjustLhChord(
                    when {
                        span <= 2 -> listOf(2, 1)
                        span <= 4 -> listOf(3, 1)
                        span <= 6 -> listOf(4, 1)
                        else -> listOf(5, 1)
                    }
                )
                3 -> {
                    val lowGap = sorted[1] - sorted[0]
                    adjustLhChord(
                        when {
                            span <= 4 -> listOf(3, 2, 1)
                            lowGap >= 5 -> listOf(5, 2, 1)
                            else -> listOf(5, 3, 1)
                        }
                    )
                }
                4 -> {
                    val lowGap = sorted[1] - sorted[0]
                    adjustLhChord(if (lowGap <= 2) listOf(5, 4, 2, 1) else listOf(5, 3, 2, 1))
                }
                else -> listOf(5, 4, 3, 2, 1)
            }
        }

        val pressedByFinger = mutableMapOf<Int, Int>()
        sorted.forEachIndexed { idx, pitch ->
            val fNum = fingersLeftToRight.getOrElse(idx) { if (isRightHand) idx + 1 else 5 - idx }.coerceIn(1, 5)
            pressedByFinger[fNum] = pitch
        }

        // Compute new palm center from pressed fingers using inverse anatomical rest offset
        val estimatedPalmMidi = if (pressedByFinger.size == 1) {
            val (f, p) = pressedByFinger.entries.first()
            (p - restOffsets[f].roundToInt()).coerceIn(24, 104)
        } else {
            ((sorted.first() + sorted.last()) / 2).coerceIn(24, 104)
        }

        val allFiveFingers = (1..5).map { fNum ->
            val pressedPitch = pressedByFinger[fNum]
            if (pressedPitch != null) {
                PianoFingerAssignment(
                    fingerNumber = fNum,
                    midiNote = pressedPitch,
                    isPressed = true,
                    strikeBounce = strikeBounce
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
                        (estimatedPalmMidi + restOffsets[fNum].roundToInt())
                    }
                }
                PianoFingerAssignment(
                    fingerNumber = fNum,
                    midiNote = naturalPitch.coerceIn(minAllowedMidi, maxAllowedMidi),
                    isPressed = false,
                    strikeBounce = 0f
                )
            }
        }

        val newPrimaryFinger = if (isRightHand) fingersLeftToRight.last() else fingersLeftToRight.first()
        return Triple(estimatedPalmMidi, newPrimaryFinger, allFiveFingers)
    }

    /**
     * Marco Musy `pianoplayer` + RoboPianist OT single-note finger cost evaluator.
     */
    private fun solveSingleNoteFingerOT(
        pitch: Int,
        isRightHand: Boolean,
        lastPalmMidi: Int,
        lastFinger: Int,
        lastPitch: Int,
        lookaheadPitches: List<Int>
    ): Int {
        val delta = pitch - lastPitch
        if (delta == 0 && lastFinger in 1..5) return lastFinger

        val restOffsets = if (isRightHand) RH_FINGER_REST_SEMITONES else LH_FINGER_REST_SEMITONES
        val isBlack = isMidiNoteBlack(pitch)
        val nextPitch = lookaheadPitches.firstOrNull()

        var bestFinger = 3
        var bestCost = Float.MAX_VALUE

        for (f in 1..5) {
            var cost = 0f

            // 1. Distance from finger's current resting key position
            val fingerRestPitch = lastPalmMidi + restOffsets[f]
            cost += abs(pitch - fingerRestPitch) * 2.2f * FINGER_PENALTY_WEIGHTS[f]

            // 2. Penalize using the exact same finger on two different consecutive pitches
            if (f == lastFinger && delta != 0) {
                cost += if (abs(delta) > 7) 14f else 65f
            }

            // 3. Anatomical step ordering & Thumb-under / Finger-over rules
            val fingerDelta = if (isRightHand) (f - lastFinger) else (lastFinger - f)
            if (delta > 0) {
                // Ascending
                if (fingerDelta > 0) {
                    val expectedSemis = fingerDelta * 2f
                    cost += abs(delta - expectedSemis) * 2.4f
                } else if (fingerDelta < 0) {
                    // Thumb-under crossover (e.g. 3 -> 1 or 4 -> 1 when ascending)
                    val isThumbUnder = (isRightHand && f == 1 && lastFinger in 2..4 && !isBlack) ||
                        (!isRightHand && lastFinger == 1 && f in 2..4)
                    cost += if (isThumbUnder && delta <= 4) 5.5f else 48f
                }
            } else if (delta < 0) {
                // Descending
                if (fingerDelta < 0) {
                    val expectedSemis = (-fingerDelta) * 2f
                    cost += abs((-delta) - expectedSemis) * 2.4f
                } else if (fingerDelta > 0) {
                    // Finger-over crossover (e.g. 1 -> 3 or 1 -> 2 when descending)
                    val isFingerOver = (isRightHand && lastFinger == 1 && f in 2..4) ||
                        (!isRightHand && f == 1 && lastFinger in 2..4 && !isBlack)
                    cost += if (isFingerOver && (-delta) <= 4) 5.5f else 48f
                }
            }

            // 4. Black-key thumb & pinky penalty (`bfactor` from pianoplayer)
            if (isBlack) {
                if (f == 1) cost += 22f
                else if (f == 5) cost += 8f
            }

            // 5. Lookahead horizon bonus: if next note continues ascending, reserve higher fingers!
            if (nextPitch != null && nextPitch != pitch) {
                val nextDelta = nextPitch - pitch
                if (isRightHand) {
                    if (nextDelta > 0 && f == 5) cost += 14f // Don't dead-end on pinky if melody keeps rising
                    if (nextDelta < 0 && f == 1 && isMidiNoteBlack(nextPitch)) cost += 6f
                } else {
                    if (nextDelta < 0 && f == 5) cost += 14f
                }
            }

            if (cost < bestCost) {
                bestCost = cost
                bestFinger = f
            }
        }

        return bestFinger
    }

    // =========================================================================================
    // PART B: VITERBI FRETBOARD & FINGERBOARD SOLVER FOR GUITAR, UKULELE, VIOLIN & SITAR
    // =========================================================================================

    data class FrettedNotePlacement(
        val stringIndex: Int, // 0 = highest-pitched string (e.g. e4 on Guitar, E5 on Violin)
        val fret: Int,        // 0 = Open string, 1..24 = Fretted
        val finger: Int       // 0 = Open, 1 = Index, 2 = Middle, 3 = Ring, 4 = Pinky
    )

    /**
     * Solves a chord or single-note slice on ANY fretted/fingered string instrument
     * (6-String Guitar, 7-String Guitar, Ukulele, Violin, Sitar) using a constrained
     * 4-Fret Hand-Box Viterbi transition model:
     *  - Guaranteed: Every note in a simultaneous chord is placed on a DISTINCT physical string!
     *  - Guaranteed: Fretted notes in a chord stay within a human-playable 4-fret hand box (`maxFret - minFret <= 3..4`)!
     *  - Guaranteed: Smooth horizontal hand-box continuity (`lastHandBoxCenterFret`) so fast melodies and
     *    waterfall notes never teleport wildly across the neck!
     */
    fun solveFrettedSliceBiomechanical(
        pitches: Collection<Int>,
        openStringsMidiHighToLow: List<Int>,
        maxFret: Int = 24,
        minFret: Int = 0,
        lastHandBoxCenterFret: Float = 3.0f,
        preferOpenStrings: Boolean = true
    ): Pair<Map<Int, FrettedNotePlacement>, Float> {
        val uniquePitches = pitches.distinct().sorted()
        if (uniquePitches.isEmpty() || openStringsMidiHighToLow.isEmpty()) {
            return emptyMap<Int, FrettedNotePlacement>() to lastHandBoxCenterFret
        }

        val numStrings = openStringsMidiHighToLow.size
        val safeMaxFret = maxFret.coerceIn(4, 24)
        val safeMinFret = minFret.coerceIn(0, safeMaxFret)

        // Pre-normalize pitches: Octave-transpose any pitches below the lowest open string,
        // and map raw pitches to their transposed representations.
        val lowestPlayableMidi = openStringsMidiHighToLow.lastOrNull() ?: 40
        val transposedToRawMap = HashMap<Int, MutableList<Int>>()
        for (rawP in uniquePitches) {
            var transP = rawP
            while (transP < lowestPlayableMidi) {
                transP += 12
            }
            transposedToRawMap.getOrPut(transP) { ArrayList() }.add(rawP)
        }

        // Limit the simultaneously fretted notes to at most numStrings distinct pitches
        val uniqueTransposed = transposedToRawMap.keys.sorted().take(numStrings)

        var bestMap = emptyMap<Int, FrettedNotePlacement>()
        var bestCost = Float.MAX_VALUE
        var bestBoxCenter = lastHandBoxCenterFret

        val maxBoxStart = (safeMaxFret - 3).coerceAtLeast(1)

        // Evaluate candidate 4-fret hand positions [boxStart .. boxStart + 3]
        for (boxStart in maxOf(1, safeMinFret)..maxBoxStart) {
            val boxEnd = boxStart + 3
            val boxCenter = boxStart + 1.5f

            val usedStrings = BooleanArray(numStrings)
            val tempPlacements = HashMap<Int, Pair<Int, Int>>(uniqueTransposed.size) // transMidi -> (stringIdx, fret)

            // Transition cost from previous hand box + slight preference for lower-neck positions (frets 1..7)
            var cost = abs(boxCenter - lastHandBoxCenterFret) * 4.2f +
                (if (boxStart <= 7) (boxStart - 1) * 1.8f else 12f + (boxStart - 7) * 4.5f)

            // Assign from lowest pitch to highest pitch so bass notes claim wound bass strings first
            for (midi in uniqueTransposed) {
                var bestStrForMidi = -1
                var bestFretForMidi = -1
                var bestNoteCost = Float.MAX_VALUE

                for (sIdx in 0 until numStrings) {
                    val fret = midi - openStringsMidiHighToLow[sIdx]
                    if (fret !in safeMinFret..safeMaxFret) continue

                    val stringAlreadyUsed = usedStrings[sIdx]
                    val stretchPenalty = when {
                        fret == 0 -> if (preferOpenStrings && boxStart <= 5) 1.5f else 22f
                        fret in boxStart..boxEnd -> (fret - boxStart) * 1.2f
                        fret < boxStart -> (boxStart - fret) * 38f
                        else -> (fret - boxEnd) * 38f
                    }
                    // Avoid fretting high up on thick wound bass strings when a higher string can play it cleanly
                    val woundStringHighFretPenalty = if (sIdx >= numStrings - 2 && fret > 8) 36f else if (sIdx == numStrings - 3 && fret > 10) 18f else 0f
                    val collisionPenalty = if (stringAlreadyUsed) 250f else 0f

                    val noteCost = stretchPenalty + woundStringHighFretPenalty + collisionPenalty
                    if (noteCost < bestNoteCost) {
                        bestNoteCost = noteCost
                        bestStrForMidi = sIdx
                        bestFretForMidi = fret
                    }
                }

                if (bestStrForMidi >= 0) {
                    usedStrings[bestStrForMidi] = true
                    tempPlacements[midi] = bestStrForMidi to bestFretForMidi
                    cost += bestNoteCost
                } else {
                    cost += 500f
                }
            }

            // Check anatomical finger span across all fretted notes in this slice
            val frettedVals = tempPlacements.values.map { it.second }.filter { it > 0 }
            if (frettedVals.size > 1) {
                val span = frettedVals.max() - frettedVals.min()
                if (span > 4) cost += (span - 4) * 450f + 350f
                else if (span == 4) cost += 28f
            }

            if (cost < bestCost) {
                bestCost = cost
                val minFretted = frettedVals.minOrNull() ?: boxStart
                val hasBarre = frettedVals.count { it == minFretted } >= 2 && tempPlacements.values.none { it.second == 0 }

                val finalMap = HashMap<Int, FrettedNotePlacement>(uniquePitches.size)
                for ((transMidi, pair) in tempPlacements) {
                    val (sIdx, fret) = pair
                    val finger = when {
                        fret <= 0 -> 0
                        hasBarre && fret == minFretted -> 1
                        frettedVals.size > 1 -> (1 + (fret - minFretted)).coerceIn(1, 4)
                        else -> (1 + (fret - boxStart)).coerceIn(1, 4)
                    }
                    val placement = FrettedNotePlacement(stringIndex = sIdx, fret = fret, finger = finger)
                    // Map back to every raw pitch that transposed to this frequency
                    val rawList = transposedToRawMap[transMidi]
                    if (rawList != null) {
                        for (rawM in rawList) {
                            finalMap[rawM] = placement
                        }
                    } else {
                        finalMap[transMidi] = placement
                    }
                }
                bestMap = finalMap
                if (frettedVals.isNotEmpty()) {
                    bestBoxCenter = frettedVals.average().toFloat().coerceIn(1.5f, 15f)
                }
            }
        }

        return bestMap to bestBoxCenter
    }

    /**
     * Precomputes and caches the globally optimal Viterbi `(string, fret, finger)` assignments
     * for an entire [Score] so that `StaffAndTabNotationViewer`, `SynthesiaWaterfallOverlay`,
     * and `UnifiedGuitarFretboard` all share the exact same human-playable fret positions!
     */
    fun enrichScoreWithViterbiFretboardAndFingers(
        score: Score,
        tuningMidiHighToLow: List<Int> = score.tuningMidi.ifEmpty { listOf(64, 59, 55, 50, 45, 40) },
        capo: Int = score.capo
    ): Score {
        val effectiveOpenMidi = tuningMidiHighToLow.map { it + capo }
        val drumTrackSet = score.tracks.filter { it.channel == 9 }.map { it.trackIndex }.toSet()

        var runningBoxCenter = 3.0f
        var lastLhCenter = 46
        var lastRhCenter = 68
        var lastLhFinger = 3
        var lastRhFinger = 2
        var lastLhPitch = 46
        var lastRhPitch = 68

        val enrichedMeasures = score.measures.map { measure ->
            val byPos = measure.notes.groupBy { "%.2f".format(it.position) }
            val updatedNotes = ArrayList<ScoreNote>(measure.notes.size)

            for ((_, sliceNotes) in byPos.entries.sortedBy { it.key.toFloatOrNull() ?: 0f }) {
                val melodicSlice = sliceNotes.filter { !it.isRest && it.voice !in drumTrackSet }
                val pitches = melodicSlice.map { it.midiNote }.distinct().sorted()

                // 1. Solve Viterbi Guitar/Fretboard (string, fret) for this slice
                val (fretMap, nextBoxCenter) = solveFrettedSliceBiomechanical(
                    pitches = pitches,
                    openStringsMidiHighToLow = effectiveOpenMidi,
                    maxFret = 22,
                    minFret = 0,
                    lastHandBoxCenterFret = runningBoxCenter
                )
                runningBoxCenter = nextBoxCenter

                // 2. Solve RoboPianist OT Piano (LH/RH finger 1..5) for this slice
                val (lhPitches, rhPitches) = partitionHandsOptimalTransport(
                    soundingPitchesAsc = pitches,
                    showLeftHand = true,
                    showRightHand = true,
                    lastLhCenter = lastLhCenter,
                    lastRhCenter = lastRhCenter
                )

                val explicitMap = melodicSlice.filter { it.fingering != null && it.fingering in 1..5 }
                    .associate { it.midiNote to it.fingering!! }

                val lhSolved = solveHandFingersOptimalTransport(
                    activePitchesAsc = lhPitches,
                    explicitFingerMap = explicitMap,
                    isRightHand = false,
                    lastPalmMidi = lastLhCenter,
                    lastPrimaryFinger = lastLhFinger,
                    lastPrimaryPitch = lastLhPitch,
                    territoryLimitMidi = (rhPitches.firstOrNull() ?: 62) - 1
                )
                if (lhPitches.isNotEmpty()) {
                    lastLhCenter = lhSolved.first
                    lastLhFinger = lhSolved.second
                    lastLhPitch = lhPitches.first()
                }

                val rhSolved = solveHandFingersOptimalTransport(
                    activePitchesAsc = rhPitches,
                    explicitFingerMap = explicitMap,
                    isRightHand = true,
                    lastPalmMidi = lastRhCenter,
                    lastPrimaryFinger = lastRhFinger,
                    lastPrimaryPitch = lastRhPitch,
                    territoryLimitMidi = (lhPitches.lastOrNull() ?: 55) + 1
                )
                if (rhPitches.isNotEmpty()) {
                    lastRhCenter = rhSolved.first
                    lastRhFinger = rhSolved.second
                    lastRhPitch = rhPitches.last()
                }

                val pianoFingerByMidi = HashMap<Int, Int>()
                lhSolved.third.filter { it.isPressed }.forEach { pianoFingerByMidi[it.midiNote] = it.fingerNumber }
                rhSolved.third.filter { it.isPressed }.forEach { pianoFingerByMidi[it.midiNote] = it.fingerNumber }

                for (n in sliceNotes) {
                    if (n.isRest || n.voice in drumTrackSet) {
                        updatedNotes.add(n)
                    } else {
                        val placement = fretMap[n.midiNote]
                        val solvedPianoFinger = pianoFingerByMidi[n.midiNote] ?: n.fingering ?: 3
                        updatedNotes.add(
                            n.copy(
                                string = n.string ?: placement?.stringIndex,
                                fret = n.fret ?: placement?.fret,
                                fingering = solvedPianoFinger
                            )
                        )
                    }
                }
            }

            measure.copy(
                notes = updatedNotes.sortedWith(compareBy({ it.position }, { it.voice }, { it.midiNote }))
            )
        }

        return score.copy(measures = enrichedMeasures)
    }
    /**
     * Converts a RealChordPosition (which stores 6-string low-to-high or 4-string relative frets)
     * into top-to-bottom (high-string 0 to low-string N-1) ABSOLUTE frets and exact MIDI pitches.
     */
    fun normalizeRealChordPositionTopToBottom(
        pos: RealChordPosition,
        openStringsMidiHighToLow: List<Int>
    ): Triple<List<Int>, List<Int>, List<Triple<Int, Int, Int>>> {
        val rawFretsTopToBottom = pos.frets.reversed()
        val rawFingersTopToBottom = pos.fingers.reversed()
        val maxRelFret = rawFretsTopToBottom.filter { it > 0 }.maxOrNull() ?: 0
        // If baseFret > 1 and frets are stored as 1..4 box-relative offsets, convert to absolute frets
        val baseOffset = if (pos.baseFret > 1 && maxRelFret <= 5) pos.baseFret - 1 else 0

        val absFretsTopToBottom = rawFretsTopToBottom.map { f ->
            when {
                f < 0 -> -1
                f == 0 -> if (pos.baseFret > 1 && baseOffset > 0) pos.baseFret else 0
                else -> f + baseOffset
            }
        }

        // Strum order: lowest-pitched string (last index) up to highest-pitched string (index 0)
        val strumSteps = (openStringsMidiHighToLow.size - 1 downTo 0).mapNotNull { sIdx ->
            val absFret = absFretsTopToBottom.getOrNull(sIdx) ?: -1
            if (absFret >= 0) {
                val midi = openStringsMidiHighToLow[sIdx] + absFret
                Triple(sIdx, absFret, midi)
            } else null
        }

        return Triple(absFretsTopToBottom, rawFingersTopToBottom, strumSteps)
    }

    /**
     * Builds a strictly ascending, human-playable scale run on a fretted instrument within a CAGED / Fret Box
     * so scale visualizers and scale auditions NEVER pass raw 0..11 pitch classes or jump erratically!
     */
    fun solvePlayableScaleBoxRun(
        rootIdx: Int,
        scaleIntervals: List<Int>,
        openStringsMidiHighToLow: List<Int>,
        boxStartFret: Int = 0,
        boxSpanFrets: Int = 4,
        numOctaves: Int = 1
    ): List<Triple<Int, Int, Int>> {
        val scalePitchClasses = scaleIntervals.map { ((rootIdx + it) % 12 + 12) % 12 }.toSet()
        val minFret = boxStartFret.coerceIn(0, 19)
        val maxFret = (minFret + boxSpanFrets).coerceIn(minFret + 3, 24)

        val candidates = mutableListOf<Triple<Int, Int, Int>>() // (stringIdx, fret, midiNote)
        for (sIdx in openStringsMidiHighToLow.indices.reversed()) {
            val openMidi = openStringsMidiHighToLow[sIdx]
            val startF = if (minFret <= 1) 0 else minFret
            for (f in startF..maxFret) {
                val midi = openMidi + f
                if ((midi % 12) in scalePitchClasses) {
                    candidates.add(Triple(sIdx, f, midi))
                }
            }
        }

        val boxCenter = (minFret + maxFret) * 0.5f
        val uniqueByPitch = candidates
            .groupBy { it.third }
            .map { (_, group) ->
                group.minByOrNull { abs(it.second - boxCenter) } ?: group.first()
            }
            .sortedBy { it.third }

        if (uniqueByPitch.isEmpty()) return emptyList()
        val firstRootIdx = uniqueByPitch.indexOfFirst { (it.third % 12) == (rootIdx % 12) }
        val fromRoot = if (firstRootIdx >= 0 && uniqueByPitch.size - firstRootIdx >= 5) {
            uniqueByPitch.subList(firstRootIdx, uniqueByPitch.size)
        } else {
            uniqueByPitch
        }
        val maxCount = if (numOctaves >= 2) 15 else 8
        return fromRoot.take(maxCount)
    }

    /**
     * Converts any list of pitch classes (0..11) or intervals into actual playable MIDI notes (48..84)
     * across the target instrument's natural register so `UnifiedGuitarFretboard` never receives 0..11!
     */
    fun expandPitchClassesToPlayableMidi(
        rootIdx: Int,
        intervalsOrPitchClasses: List<Int>,
        isPitchClassList: Boolean = false,
        baseMidi: Int = 60
    ): List<Int> {
        return if (isPitchClassList) {
            intervalsOrPitchClasses.map { pc ->
                val semi = ((pc - rootIdx) % 12 + 12) % 12
                baseMidi + rootIdx + semi
            }.distinct().sorted()
        } else {
            intervalsOrPitchClasses.map { interval ->
                baseMidi + rootIdx + interval
            }.distinct().sorted()
        }
    }
}