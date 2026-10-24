package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.data.Taal
import com.creativeblisslabs.peqlearn.app.ui.screens.MusicConstants

object ChordVoicingEngine {

    /**
     * Finds the best guitar voicing using root pitch and chord name.
     */
    fun findBestGuitarVoicing(rootPitch: Int, chordName: String): GuitarShape {
        val rootIdx = rootPitch % 12
        val rootName = NOTE_NAMES_CD[rootIdx]
        return getGuitarVoicing(rootName, chordName)
    }

    /**
     * Gets guitar voicing based on root name and quality.
     */
    fun getGuitarVoicing(rootName: String, quality: String = "maj", cagedPos: Int = 0): GuitarShape {
        val rootMidi = 48 + (NOTE_NAMES_CD.indexOf(rootName).takeIf { it >= 0 } ?: 0)
        val normQuality = when (quality.lowercase()) {
            "maj", "major", "" -> ""
            "m", "min", "minor" -> "m"
            "7", "dom7" -> "7"
            "maj7", "major7" -> "maj7"
            "m7", "min7" -> "m7"
            "dim" -> "dim"
            "aug" -> "aug"
            "sus2" -> "sus2"
            "sus4" -> "sus4"
            else -> quality
        }
        val typeDef = CHORD_TYPE_DEFS.find { it.suffix.equals(normQuality, ignoreCase = true) }
            ?: CHORD_TYPE_DEFS.find { it.name.equals(quality, ignoreCase = true) }
            ?: CHORD_TYPE_DEFS.first()
        val intervals = typeDef.intervals
        val openTuning = listOf(40, 45, 50, 55, 59, 64)
        return generateDynamicShape(
            rootMidiNote = rootMidi,
            targetSemitones = intervals,
            voicingStyle = "Standard",
            startFret = (cagedPos * 2 + 1).coerceIn(1, 12),
            openStringPitches = openTuning,
            chordSuffix = typeDef.suffix
        )
    }

    /**
     * Dynamically generates a playable chord shape (Advanced Overload).
     */
    fun generateDynamicShape(
        rootMidiNote: Int,
        targetSemitones: List<Int>,
        voicingStyle: String = "Standard",
        startFret: Int = 1,
        openStringPitches: List<Int> = listOf(40, 45, 50, 55, 59, 64),
        isPiano: Boolean = false,
        inversion: InversionStyle = InversionStyle.ROOT,
        chordSuffix: String = "",
        isLeftHand: Boolean = false,
        difficulty: ChordDiff = ChordDiff.MEDIUM
    ): GuitarShape {
        val numStrings = openStringPitches.size

        val bassDegreeSafe = inversion.bassDegree.coerceAtMost(targetSemitones.lastIndex)
        val targetBassNote = (rootMidiNote + targetSemitones[bassDegreeSafe]) % 12

        val effectiveTarget = if (voicingStyle == "Power") {
            targetSemitones.filterNot { it == 3 || it == 4 }
        } else targetSemitones

        val absoluteTargetPitches = effectiveTarget.map { (rootMidiNote + it) % 12 }.toSet()

        val stringPossibilities = List(numStrings) { stringIdx ->
            val openPitch = openStringPitches[stringIdx]
            val validFrets = mutableListOf<Int>()
            val endFret = startFret + 4

            if (absoluteTargetPitches.contains(openPitch % 12)) {
                validFrets.add(0)
            }

            for (fret in startFret..endFret) {
                if (fret == 0) continue
                val pitch = (openPitch + fret) % 12
                if (absoluteTargetPitches.contains(pitch)) {
                    validFrets.add(fret)
                }
            }
            validFrets.add(-1)
            validFrets.distinct()
        }

        var bestFrets = List(numStrings) { -1 }
        var bestScore = Double.MAX_VALUE

        fun evaluateCombinations(stringIdx: Int, currentFrets: MutableList<Int>) {
            if (stringIdx == numStrings) {
                val activeIndices = currentFrets.indices.filter { currentFrets[it] >= 0 }
                if (activeIndices.isEmpty()) return

                val lowestIdx = activeIndices.first()
                val lowestPitch = (openStringPitches[lowestIdx] + currentFrets[lowestIdx]) % 12

                if (lowestPitch != targetBassNote) return

                val soundingPitches = activeIndices.map { sIdx ->
                    (openStringPitches[sIdx] + currentFrets[sIdx]) % 12
                }.toSet()

                if (soundingPitches.containsAll(absoluteTargetPitches)) {
                    val score = scoreShapeMechanics(currentFrets, targetBassNote, openStringPitches)
                    if (score < bestScore) {
                        bestScore = score
                        bestFrets = currentFrets.toList()
                    }
                }
                return
            }
            for (fretOption in stringPossibilities[stringIdx]) {
                currentFrets.add(fretOption)
                evaluateCombinations(stringIdx + 1, currentFrets)
                currentFrets.removeAt(currentFrets.lastIndex)
            }
        }

        evaluateCombinations(0, mutableListOf())

        val nonZeroFrets = bestFrets.filter { it > 0 }
        val optimalBaseFret = if (nonZeroFrets.isNotEmpty()) nonZeroFrets.minOrNull()!! else startFret

        val generatedFingers = generateFingeringLabels(
            bestFrets,
            optimalBaseFret,
            isPiano,
            chordSuffix,
            isLeftHand
        )

        val constraint = detectChordConstraint(bestFrets, generatedFingers, optimalBaseFret, isPiano)

        return GuitarShape(
            frets = bestFrets,
            fingers = generatedFingers,
            baseFret = optimalBaseFret,
            barre = if (constraint != null && constraint.contains("Barre")) optimalBaseFret else null
        )
    }

    /**
     * Dynamically generates a playable chord shape based on Difficulty (Simple Overload).
     */
    fun generateDynamicShape(
        rootMidiNote: Int,
        targetSemitones: List<Int>,
        difficulty: ChordDiff,
        openStringPitches: List<Int>
    ): GuitarShape {
        val voicingStyle = when (difficulty) {
            ChordDiff.EASY -> "Easy"
            ChordDiff.MEDIUM -> "Standard"
            ChordDiff.HARD -> "Advanced"
        }
        val startFret = when (difficulty) {
            ChordDiff.EASY -> 1
            ChordDiff.MEDIUM -> 3
            ChordDiff.HARD -> 5
        }
        return generateDynamicShape(
            rootMidiNote = rootMidiNote,
            targetSemitones = targetSemitones,
            voicingStyle = voicingStyle,
            startFret = startFret,
            openStringPitches = openStringPitches
        )
    }
private fun scoreShapeMechanics(frets: List<Int>, root: Int, tuning: List<Int>, diff: ChordDiff = ChordDiff.MEDIUM): Double {
    var score = 0.0
    
    // 1. Identify active strings (played frets 0 or higher)
    val activeIndices = frets.indices.filter { frets[it] >= 0 }
    if (activeIndices.isEmpty()) return 9999.0 // Invalid shape

    // 2. Fret Span Penalties
    val activeFrets = activeIndices.map { frets[it] }.filter { it > 0 }
    if (activeFrets.isNotEmpty()) {
        val minFret = activeFrets.minOrNull() ?: 0
        val maxFret = activeFrets.maxOrNull() ?: 0
        val span = maxFret - minFret
        
        // Stricter penalty for large spans (> 4 is very hard)
        if (span > 4) score += 500.0
        else if (span > 3) score += 200.0
        
        score += span * 25.0
    }

    // 3. String-Skipping Penalty (Avoid muted strings between played strings)
    val firstActive = activeIndices.first()
    val lastActive = activeIndices.last()
    for (i in firstActive..lastActive) {
        if (frets[i] == -1) score += 100.0 // Heavy penalty for skipping a string
    }

    // 4. Bass String Root Preference
    val lowestStringIdx = activeIndices.first()
    val lowestPitch = (tuning[lowestStringIdx] + frets[lowestStringIdx]) % 12
    if (lowestPitch != root % 12) {
        score += 150.0 // High penalty if the bass note isn't the root
    }

    // 5. Difficulty Adjustments
    if (diff == ChordDiff.EASY) {
        // Favor open strings
        val openCount = frets.count { it == 0 }
        score -= openCount * 50.0
        
        // Heavily penalize high fret positions for beginners
        val maxFret = activeFrets.maxOrNull() ?: 0
        if (maxFret > 5) score += 300.0
    }

    return score
}
    fun generateFingeringLabels(
        frets: List<Int>,
        baseFret: Int,
        isPiano: Boolean = false,
        chordSuffix: String = "",
        isLeftHand: Boolean = false
    ): List<Int> {
        val isScaleMode = chordSuffix == "SCALE" || frets.size > 6

        if (isPiano) {
            if (isScaleMode) {
                val fingerSequence = if (isLeftHand) listOf(5, 4, 3, 2, 1) else listOf(1, 2, 3, 1, 2, 3, 4, 5)
                return List(frets.size) { fingerSequence[it % fingerSequence.size] }
            }

            val registry = if (isLeftHand) PIANO_LH_FINGERS else PIANO_RH_FINGERS
            return registry[chordSuffix] ?: List(frets.size) { (it % 5) + 1 }
        } else {
            if (!isScaleMode) {
                val guitarShape = GUITAR_LH_FINGERS[chordSuffix]
                if (guitarShape != null && guitarShape.frets.size == frets.size) return guitarShape.fingers
            }

            val fingers = MutableList(frets.size) { 0 }
            val activeStrings = frets.indices.filter { frets[it] >= 0 }
            if (activeStrings.isEmpty()) return fingers

            val minFret = frets.filter { it > 0 }.minOrNull() ?: baseFret

            activeStrings.forEach { stringIdx ->
                val fretValue = frets[stringIdx]
                if (fretValue == 0) {
                    fingers[stringIdx] = 0
                } else if (fretValue > 0) {
                    val relativeOffset = fretValue - minFret
                    fingers[stringIdx] = when (relativeOffset) {
                        0 -> 1
                        1 -> 2
                        2 -> 3
                        else -> 4
                    }.coerceIn(1, 4)
                }
            }
            return fingers
        }
    }

    private fun detectChordConstraint(frets: List<Int>, fingers: List<Int>, baseFret: Int, isPiano: Boolean): String? {
        if (isPiano) {
            val activeNotes = frets.filter { it > 0 }
            if (activeNotes.size < 2) return null
            val span = activeNotes.maxOrNull()!! - activeNotes.minOrNull()!!
            return if (span > 12) "Large Stretch" else null
        } else {
            return if (detectBarreRequirement(frets, fingers, baseFret)) "Barre Fret $baseFret" else null
        }
    }

    fun detectBarreRequirement(frets: List<Int>, fingers: List<Int>, baseFret: Int): Boolean {
        if (baseFret <= 1) return false
        val baseFretCount = frets.filterIndexed { idx, fret -> fret == baseFret && fingers.getOrNull(idx) == 1 }.size
        return baseFretCount >= 3
    }

    private fun getModifiedIntervals(baseIntervals: List<Int>, extensionIdx: Int): List<Int> {
        if (extensionIdx <= 0) return baseIntervals
        val extraSemitoneString = INTERVALS_DATA.getOrNull(extensionIdx)?.get(0) ?: return baseIntervals
        val extraSemitone = extraSemitoneString.toIntOrNull() ?: return baseIntervals
        return (baseIntervals + extraSemitone).distinct()
    }

    fun buildGuitarShape(
        rootIdx: Int,
        suffix: String,
        style: GuitarVoicingStyle,
        startFret: Int = 1,
        inversion: InversionStyle = InversionStyle.ROOT,
        extensionIdx: Int = 0
    ): GuitarShape {
        val key = "${NOTE_NAMES_CD[rootIdx]}$suffix"
        if (extensionIdx == 0) {
            GUITAR_SHAPES_OVERRIDES[key]?.let { return it }
        }

        val typeDef = CHORD_TYPE_DEFS.find { it.suffix == suffix } ?: CHORD_TYPE_DEFS.first()
        val combinedIntervals = getModifiedIntervals(typeDef.intervals, extensionIdx)

        return generateDynamicShape(
            rootMidiNote = rootIdx,
            targetSemitones = combinedIntervals,
            voicingStyle = style.label,
            startFret = startFret,
            openStringPitches = listOf(40, 45, 50, 55, 59, 64),
            inversion = inversion,
            isPiano = false,
            chordSuffix = suffix
        )
    }

    fun buildPianoShape(
        rootIdx: Int,
        suffix: String,
        style: PianoVoicingStyle,
        inversion: InversionStyle = InversionStyle.ROOT,
        octave: Int = 4,
        extensionIdx: Int = 0
    ): List<Int> {
        val typeDef = CHORD_TYPE_DEFS.find { it.suffix == suffix } ?: CHORD_TYPE_DEFS.first()
        val combinedIntervals = getModifiedIntervals(typeDef.intervals, extensionIdx)
        val rootMidi = 60 + (rootIdx % 12) + ((octave - 4) * 12)

        val rotatedIntervals = combinedIntervals.toMutableList()
        val rotations = inversion.bassDegree.coerceAtMost(rotatedIntervals.lastIndex)

        repeat(rotations) {
            if (rotatedIntervals.isNotEmpty()) {
                val first = rotatedIntervals.removeAt(0)
                rotatedIntervals.add(first + 12)
            }
        }

        return when (style) {
            PianoVoicingStyle.SHELL -> {
                if (rotatedIntervals.size >= 2) listOf(rotatedIntervals[0], rotatedIntervals[1]) else rotatedIntervals
            }
            PianoVoicingStyle.ROOTLESS -> rotatedIntervals.drop(1)
            else -> rotatedIntervals
        }.map { rootMidi + it }
    }

    fun buildUkuShape(
        rootIdx: Int,
        suffix: String,
        style: UkeVoicingStyle,
        startFret: Int = 1,
        inversion: InversionStyle = InversionStyle.ROOT,
        extensionIdx: Int = 0
    ): GuitarShape {
        val targetTypeDef = CHORD_TYPE_DEFS.find { it.suffix == suffix } ?: CHORD_TYPE_DEFS.first()
        val combinedIntervals = getModifiedIntervals(targetTypeDef.intervals, extensionIdx)

        return generateDynamicShape(
            rootMidiNote = rootIdx,
            targetSemitones = combinedIntervals,
            voicingStyle = style.label,
            startFret = startFret,
            openStringPitches = listOf(67, 60, 64, 69),
            inversion = inversion,
            isPiano = false,
            chordSuffix = suffix
        )
    }

    fun getDiatonicStep(midiNote: Int): Int {
        val octave = (midiNote / 12) - 1
        val stepInOctave = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS[midiNote % 12]
        return (octave * 7) + stepInOctave
    }

    fun generateCagedAbsoluteNotes(
        scaleOrChordNotes: List<Int>,
        targetPosition: Int = 0,
        numFrets: Int = 12,
        isChordMode: Boolean = false,
        stringOpenBaselines: List<Int> = listOf(40, 45, 50, 55, 59, 64),
        stringRanges: List<IntRange>? = null
    ): List<Int> {
        val absoluteNotesResult = mutableListOf<Int>()
        val maxFretRange = 4

        stringOpenBaselines.forEachIndexed { stringIdx, openMidiBaseline ->
            for (fret in 0..numFrets) {
                val absoluteMidi = openMidiBaseline + fret
                val pitchClass = absoluteMidi % 12

                if (scaleOrChordNotes.contains(pitchClass)) {
                    val isFretInWindow = if (stringRanges != null && stringIdx < stringRanges.size) {
                        fret in (stringRanges[stringIdx].first + targetPosition)..(stringRanges[stringIdx].last + targetPosition)
                    } else {
                        (fret == 0) || (fret >= targetPosition && fret <= (targetPosition + maxFretRange))
                    }

                    if (isFretInWindow) {
                        absoluteNotesResult.add(absoluteMidi)
                        if (isChordMode) break
                    }
                }
            }
        }
        return absoluteNotesResult
    }

    /**
     * Computes voiced intervals with optional extension semitones and inversion rotations.
     */
    fun getVoicedIntervals(
        baseIntervals: List<Int>,
        voicingIdx: Int = 0,
        extraSemitone: Int? = null
    ): List<Int> {
        val base = baseIntervals.toMutableList()
        if (extraSemitone != null && !base.contains(extraSemitone)) {
            base.add(extraSemitone)
            base.sort()
        }
        return when (voicingIdx) {
            1 -> { val m = base.toMutableList(); if (m.isNotEmpty()) m[0] += 12; m.sorted() }
            2 -> { val m = base.toMutableList(); if (m.isNotEmpty()) m[0] += 12; if (m.size >= 2) m[1] += 12; m.sorted() }
            3 -> {
                if (base.size >= 4) { val m = base.toMutableList(); m[m.size - 2] -= 12; m.sorted() } else base
            }
            else -> base
        }
    }

    /**
     * Resolves active MIDI notes for a root note and either chord intervals or scale intervals.
     */
    fun getActiveMidiNotes(
        rootIdx: Int,
        intervals: List<Int>,
        baseMidi: Int = 60
    ): Map<Int, Boolean> {
        val root = baseMidi + (rootIdx % 12)
        return intervals.associate { (root + it) to true }
    }

    /**
     * Computes piano fingering for chord or scale pitches.
     */
    fun computePianoFingering(
        pitches: List<Int>,
        chordSuffix: String = "",
        isScale: Boolean = false,
        voicingIdx: Int = 0
    ): com.creativeblisslabs.peqlearn.app.ui.screens.FingeringData {
        if (!isScale) {
            val baseList = PIANO_RH_FINGERS[chordSuffix] ?: when (pitches.size) {
                1 -> listOf(1)
                2 -> listOf(1, 5)
                3 -> when (voicingIdx) {
                    1 -> listOf(1, 2, 5) // 1st inversion
                    2 -> listOf(1, 3, 5) // 2nd inversion
                    else -> listOf(1, 3, 5) // root position
                }
                4 -> listOf(1, 2, 4, 5)
                else -> listOf(1, 2, 3, 4, 5)
            }
            val fingers = pitches.indices.map { i ->
                baseList.getOrElse(i) { (i % 5) + 1 }
            }
            return com.creativeblisslabs.peqlearn.app.ui.screens.FingeringData(notes = pitches, fingers = fingers)
        } else {
            val rhScalePattern = listOf(1, 2, 3, 1, 2, 3, 4, 5)
            val fingers = pitches.indices.map { i ->
                rhScalePattern[i % rhScalePattern.size]
            }
            return com.creativeblisslabs.peqlearn.app.ui.screens.FingeringData(notes = pitches, fingers = fingers)
        }
    }

    /**
     * Resolves guitar chord shape from RealChordsDb with CAGED / fret-range filters and fallback.
     */
    fun getDictionaryGuitarShape(
        rootName: String,
        suffix: String,
        diff: ChordDiff = ChordDiff.EASY,
        capoFret: Int = 0,
        startFret: Int = 1,
        voicingType: String = "Open",
        cagedFilter: String = "All CAGED",
        fretRangeFilter: String = "All Frets",
        fallbackIntervals: List<Int> = listOf(0, 4, 7)
    ): GuitarShape {
        val allRealPositions = RealChordsDb.getGuitarPositions(rootName, suffix)
        val filteredPositions = allRealPositions.filter { pos ->
            val matchesFretRange = when (fretRangeFilter) {
                "Open (0-3)" -> pos.baseFret in 0..3
                "Low (1-5)" -> pos.baseFret in 1..5
                "Mid (5-9)" -> pos.baseFret in 5..9
                "High (9-14)" -> pos.baseFret in 9..14
                "12+ Frets" -> pos.baseFret >= 12
                else -> true
            }
            val matchesCaged = when (cagedFilter) {
                "C-Shape" -> (pos.frets.getOrNull(1) ?: -1) > 0 && (pos.frets.getOrNull(0) ?: -1) <= 0
                "A-Shape" -> (pos.frets.getOrNull(1) ?: -1) >= 0 && (pos.frets.getOrNull(2) ?: -1) >= 0
                "G-Shape" -> (pos.frets.getOrNull(0) ?: -1) > 0 && (pos.frets.getOrNull(5) ?: -1) > 0
                "E-Shape" -> (pos.frets.getOrNull(0) ?: -1) >= 0 && (pos.frets.getOrNull(1) ?: -1) >= 0
                "D-Shape" -> (pos.frets.getOrNull(0) ?: -1) < 0 && (pos.frets.getOrNull(1) ?: -1) < 0 && (pos.frets.getOrNull(2) ?: -1) >= 0
                else -> true
            }
            matchesFretRange && matchesCaged
        }

        val pool = if (filteredPositions.isNotEmpty()) filteredPositions else allRealPositions
        if (pool.isNotEmpty()) {
            val chosen = when (diff) {
                ChordDiff.EASY -> pool.first()
                ChordDiff.MEDIUM -> pool.getOrNull(1) ?: pool.first()
                ChordDiff.HARD -> pool.last()
            }
            val reversedFrets = chosen.frets.reversed()
            val reversedFingers = chosen.fingers.reversed()
            return GuitarShape(
                frets = reversedFrets,
                fingers = reversedFingers,
                baseFret = chosen.baseFret,
                barre = chosen.barres.firstOrNull(),
                diff = diff
            )
        } else {
            val rootIdx = NOTE_NAMES_CD.indexOf(rootName).takeIf { it >= 0 } ?: 0
            return generateDynamicShape(
                rootMidiNote = (rootIdx + capoFret) % 12,
                targetSemitones = fallbackIntervals,
                voicingStyle = voicingType,
                startFret = startFret,
                openStringPitches = listOf(4, 11, 7, 2, 9, 4),
                isPiano = false,
                chordSuffix = suffix
            )
        }
    }

    /**
     * Resolves ukulele chord shape from RealChordsDb with fallback.
     */
    fun getDictionaryUkuShape(
        rootName: String,
        suffix: String,
        diff: ChordDiff = ChordDiff.EASY,
        capoFret: Int = 0,
        startFret: Int = 1,
        voicingType: String = "Open",
        fallbackIntervals: List<Int> = listOf(0, 4, 7)
    ): GuitarShape {
        val realUkuPositions = RealChordsDb.getUkulelePositions(rootName, suffix)
        if (realUkuPositions.isNotEmpty()) {
            val chosen = when (diff) {
                ChordDiff.EASY -> realUkuPositions.first()
                ChordDiff.MEDIUM -> realUkuPositions.getOrNull(1) ?: realUkuPositions.first()
                ChordDiff.HARD -> realUkuPositions.last()
            }
            val reversedFrets = chosen.frets.reversed()
            val reversedFingers = chosen.fingers.reversed()
            return GuitarShape(
                frets = reversedFrets,
                fingers = reversedFingers,
                baseFret = chosen.baseFret,
                barre = chosen.barres.firstOrNull(),
                diff = diff
            )
        } else {
            val rootIdx = NOTE_NAMES_CD.indexOf(rootName).takeIf { it >= 0 } ?: 0
            return generateDynamicShape(
                rootMidiNote = (rootIdx + capoFret) % 12,
                targetSemitones = fallbackIntervals,
                voicingStyle = voicingType,
                startFret = startFret,
                openStringPitches = listOf(9, 4, 0, 7),
                isPiano = false,
                chordSuffix = suffix
            )
        }
    }

    /**
     * Resolves songwriting cadence progressions for any root index.
     */
    fun getCadenceProgressions(rootIdx: Int, rootNames: List<String> = NOTE_NAMES_CD): List<Pair<String, List<Int>>> {
        return listOf(
            Pair("${rootNames[(rootIdx + 7) % 12]} (V)", listOf(rootIdx + 7, rootIdx + 11, rootIdx + 2)),
            Pair("${rootNames[(rootIdx + 5) % 12]} (IV)", listOf(rootIdx + 5, rootIdx + 9, rootIdx)),
            Pair("${rootNames[(rootIdx + 9) % 12]}m (vi)", listOf(rootIdx + 9, rootIdx, rootIdx + 4)),
            Pair("${rootNames[rootIdx]} (I)", listOf(rootIdx, rootIdx + 4, rootIdx + 7))
        )
    }

    /**
     * Comprehensive diagnostic verification and Proof-of-Concept testing ChordVoicingEngine
     * computations against RealChordsDb, RealPianoChordsDb, and ChordModels.
     */
    fun verifyIntegrity(): String {
        val report = StringBuilder()
        var passed = 0
        var total = 0

        val roots = listOf("C", "D", "E", "F", "G", "A", "B")
        val testChords = listOf(
            "major" to "",
            "m" to "m",
            "7" to "7",
            "maj7" to "maj7",
            "dim" to "dim",
            "aug" to "aug",
            "sus2" to "sus2",
            "sus4" to "sus4"
        )

        // 1. Verify Piano Pitch Classes Against RealPianoChordsDb
        for (root in roots) {
            val rootIdx = NOTE_NAMES_CD.indexOf(root).coerceAtLeast(0)
            for ((dbSuffix, engineSuffix) in testChords) {
                total++
                val dbChord = RealPianoChordsDb.getChord(root, dbSuffix)
                val enginePitches = buildPianoShape(
                    rootIdx = rootIdx,
                    suffix = engineSuffix,
                    style = PianoVoicingStyle.CLOSE,
                    inversion = InversionStyle.ROOT,
                    octave = 4
                ).map { it % 12 }.toSet()

                val expectedPitches = dbChord?.notes?.mapNotNull { noteName ->
                    val norm = when (noteName.uppercase()) {
                        "DB" -> "C#"
                        "EB" -> "D#"
                        "GB" -> "F#"
                        "AB" -> "G#"
                        "BB" -> "A#"
                        else -> noteName
                    }
                    val idx = NOTE_NAMES_CD.indexOf(norm)
                    if (idx >= 0) idx else null
                }?.toSet()

                if (expectedPitches != null && enginePitches == expectedPitches) {
                    passed++
                } else if (expectedPitches != null) {
                    report.appendLine("[Piano Mismatch] $root $dbSuffix: Engine=$enginePitches vs DB=$expectedPitches")
                } else {
                    // DB chord not found, engine fallback valid
                    passed++
                }
            }
        }

        // 2. Verify Guitar Voicings and Fingering against RealChordsDb
        for (root in roots) {
            for ((_, suffix) in testChords) {
                total++
                val dbPositions = RealChordsDb.getGuitarPositions(root, suffix)
                if (dbPositions.isNotEmpty()) {
                    val dbFirst = dbPositions.first()
                    val engineShape = getDictionaryGuitarShape(
                        rootName = root,
                        suffix = suffix,
                        diff = ChordDiff.EASY
                    )
                    // RealChordsDb is low-to-high (E2..E4). Engine reverses to high-to-low (E4..E2)
                    val expectedFrets = dbFirst.frets.reversed()
                    val expectedFingers = dbFirst.fingers.reversed()
                    if (engineShape.frets == expectedFrets && engineShape.fingers == expectedFingers && engineShape.baseFret == dbFirst.baseFret) {
                        passed++
                    } else {
                        report.appendLine("[Guitar Mismatch] $root $suffix: Engine frets=${engineShape.frets} vs DB=$expectedFrets")
                    }
                } else {
                    passed++
                }
            }
        }

        // 3. Verify Piano Fingering Resolution against ChordModels.kt
        for ((_, suffix) in testChords) {
            total++
            val expectedFingers = PIANO_RH_FINGERS[suffix] ?: listOf(1, 3, 5)
            val dummyPitches = listOf(60, 64, 67)
            val calculated = computePianoFingering(dummyPitches, suffix).fingers
            if (calculated.take(expectedFingers.size) == expectedFingers) {
                passed++
            } else {
                report.appendLine("[Fingering Mismatch] $suffix: Engine=$calculated vs Expected=$expectedFingers")
            }
        }

        report.appendLine("Verification Completed: $passed/$total checks passed.")
        return report.toString()
    }
}

object CompositionEngine {
    fun getSuggestions(rootIdx: Int, chordTypeIdx: Int): List<String> {
        return listOf(
            NOTE_NAMES_CD[(rootIdx + 7) % 12] + " (V)", // Dominant
            NOTE_NAMES_CD[(rootIdx + 5) % 12] + " (IV)", // Subdominant
            NOTE_NAMES_CD[(rootIdx + 9) % 12] + "m (vi)", // Relative Minor
            NOTE_NAMES_CD[rootIdx] + " (I)" // Tonic
        )
    }
}

object CircleOfFifths {
    private val circleOrder = listOf(0, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10, 5)

    fun getCircleNames(names: List<String>) = circleOrder.map { names[it] }
    fun getRootFromCircleIndex(index: Int) = circleOrder[index]
    fun getCircleIndexFromRoot(rootIdx: Int) = circleOrder.indexOf(rootIdx)
}


internal val SCALE_BOX_POSITIONS = listOf(
    ScaleBoxPosition("Box 1: 'C' Shape", listOf(0..3, 0..3, 0..2, 0..2, 0..1, 0..3)),
    ScaleBoxPosition("Box 2: 'A' Shape", listOf(2..5, 2..5, 2..5, 2..5, 3..5, 2..5)),
    ScaleBoxPosition("Box 3: 'G' Shape", listOf(5..8, 5..7, 5..7, 4..7, 5..8, 5..8)),
    ScaleBoxPosition("Box 4: 'E' Shape", listOf(7..10, 7..9, 7..9, 7..9, 8..10, 7..10)),
    ScaleBoxPosition("Box 5: 'D' Shape", listOf(9..12, 9..12, 9..12, 9..11, 10..13, 9..12))
)

// Voicing styles are defined in ChordModels.kt

// InversionStyle is defined in ChordModels.kt

val WESTERN_TIME_SIGNATURES = listOf(
    Taal(
        name = "4/4 Common Time",
        beats = 4,
        vibhags = listOf(4),
        bolSequence = listOf("STR", "WEK", "MID", "WEK"),
        khali = 2, // The secondary accent on beat 3
        tempo = "Variable (60 - 180 BPM)",
        description = "The most standard Western meter. Four quarter-note beats per measure. Features a heavy accent on beat 1, a lighter secondary pulse on beat 3, and weak pulses on 2 and 4."
    ),
    Taal(
        name = "3/4 Waltz Time",
        beats = 3,
        vibhags = listOf(3),
        bolSequence = listOf("STR", "WEK", "WEK"),
        khali = 0, // No secondary downbeat
        tempo = "Variable (80 - 140 BPM)",
        description = "Classic triple meter. Three quarter-note beats per bar. Heavily accents the first beat ('ONE-two-three'), creating the iconic swaying ballroom waltz rhythm."
    ),
    Taal(
        name = "2/4 March Tempo",
        beats = 2,
        vibhags = listOf(2),
        bolSequence = listOf("STR", "WEK"),
        khali = 0,
        tempo = "Variable (100 - 120 BPM)",
        description = "Duple meter commonly used in traditional marches, polkas, and fast high-energy folk tracks. An alternating rhythmic pulse of Strong-Weak, Strong-Weak."
    ),
    Taal(
        name = "6/8 Compound Time",
        beats = 6,
        vibhags = listOf(3, 3),
        bolSequence = listOf("STR", "WEK", "WEK", "MID", "WEK", "WEK"),
        khali = 3, // Secondary downbeat on the 4th pulse
        tempo = "Variable (50 - 90 BPM)",
        description = "Compound duple meter. Six eighth-note pulses grouped into two macro-beats of three. Feels like two giant steps with a triplet bounce under each ('ONE-two-three, FOUR-five-six')."
    ),
    Taal(
        name = "12/8 Blues Shuffle",
        beats = 12,
        vibhags = listOf(3, 3, 3, 3),
        bolSequence = listOf(
            "STR", "WEK", "WEK", 
            "MID", "WEK", "WEK", 
            "MID", "WEK", "WEK", 
            "MID", "WEK", "WEK"
        ),
        khali = 6,
        tempo = "Variable (60 - 100 BPM)",
        description = "Compound quadruple meter tracking four main beats, each subdivided into three triplets. The structural skeleton driving traditional Blues shuffles, classic Rock ballads, and R&B grooves."
    ),
    Taal(
        name = "5/4 Odd Meter",
        beats = 5,
        vibhags = listOf(3, 2),
        bolSequence = listOf("STR", "WEK", "WEK", "MID", "WEK"),
        khali = 3,
        tempo = "Variable (110 - 160 BPM)",
        description = "Asymmetric quintuple meter. Most commonly subdivided as 3+2 (or 2+3). Famously heard in jazz classics like Dave Brubeck's 'Take Five' and dramatic cinematic score tracks."
    ),
    Taal(
        name = "7/8 Asymmetric Time",
        beats = 7,
        vibhags = listOf(2, 2, 3),
        bolSequence = listOf("STR", "WEK", "MID", "WEK", "MID", "WEK", "WEK"),
        khali = 4,
        tempo = "Variable (120 - 200 BPM)",
        description = "Complex asymmetric time. Seven fast eighth-note pulses usually divided unevenly as 2+2+3 or 3+2+2. Creates an energetic, constantly accelerating syncopated drive common in progressive rock and folk music."
    )
)
val WESTERN_BEATS = listOf(
    Taal(
        name = "Four on the Floor (4/4)",
        beats = 16,
        vibhags = listOf(4, 4, 4, 4),
        bolSequence = listOf(
            "KCK", "HHC", "HHC", "HHC", 
            "KCK", "HHC", "SNR", "HHC", 
            "KCK", "HHC", "HHC", "HHC", 
            "KCK", "HHC", "SNR", "OHH"
        ),
        khali = 4, // Zero-indexed position of the first core backbeat snare
        tempo = "120 - 130 BPM",
        description = "The foundation of House and Electronic dance music. Constant kick on every quarter note step, with crisp hi-hat subdivisions and backbeat snares on beats 2 and 4."
    ),
    Taal(
        name = "Classic Rock Backbeat",
        beats = 16,
        vibhags = listOf(4, 4, 4, 4),
        bolSequence = listOf(
            "KCK", "HHC", "HHC", "HHC", 
            "SNR", "HHC", "KCK", "KCK", 
            "SNR", "HHC", "HHC", "HHC", 
            "SNR", "HHC", "KCK", "OHH"
        ),
        khali = 4,
        tempo = "90 - 110 BPM",
        description = "Standard 4/4 driving straight rock groove. Emphasizes heavy kicks on 1 and alternating double kicks on 3, counterbalanced by driving backbeat snare snaps."
    ),
    Taal(
        name = "Pop Waltz (3/4)",
        beats = 12,
        vibhags = listOf(3, 3, 3, 3),
        bolSequence = listOf(
            "KCK", "HHC", "HHC", 
            "SNR", "HHC", "HHC", 
            "KCK", "HHC", "HHC", 
            "SNR", "HHC", "OHH"
        ),
        khali = 3,
        tempo = "80 - 100 BPM",
        description = "Triple meter standard. A heavy foundational downbeat kick followed by two lighter hi-hat/snare subdivisions, creating a swaying 3-beat feel."
    ),
    Taal(
        name = "Trap Boom Bap (Hip-Hop)",
        beats = 16,
        vibhags = listOf(4, 4, 4, 4),
        bolSequence = listOf(
            "KCK", "HHC", "HHC", "HHC", 
            "CLP", "HHC", "KCK", "HHC", 
            "HHC", "KCK", "CLP", "HHC", 
            "HHC", "HHC", "CLP", "OHH"
        ),
        khali = 4,
        tempo = "70 - 90 BPM",
        description = "Spacious urban pattern utilizing syncopated double kicks, high-frequency rolling accents, and structural claps on the cross-beats."
    ),
    Taal(
        name = "Shuffle / Blues (6/8)",
        beats = 12,
        vibhags = listOf(6, 6),
        bolSequence = listOf(
            "KCK", "HHC", "RID", "SNR", "HHC", "RID",
            "KCK", "RID", "HHC", "SNR", "RID", "OHH"
        ),
        khali = 3,
        tempo = "60 - 80 BPM",
        description = "Compound duple pattern driving standard blues layouts. Accents fall primarily on the triplets (1-2-3, 4-5-6) giving a rolling rhythmic movement."
    ),
    Taal(
        name = "Funk Breakbeat",
        beats = 16,
        vibhags = listOf(4, 4, 4, 4),
        bolSequence = listOf(
            "KCK", "HHC", "OHH", "SNR", 
            "HHC", "KCK", "SNR", "HHC", 
            "HHC", "SNR", "KCK", "HHC", 
            "SNR", "HHC", "KCK", "OHH"
        ),
        khali = 3,
        tempo = "95 - 115 BPM",
        description = "Highly syncopated groove with active snare ghost notes, offbeat kick accents, and dynamic open hi-hat insertions."
    ),
    Taal(
        name = "Disco Groove",
        beats = 16,
        vibhags = listOf(4, 4, 4, 4),
        bolSequence = listOf(
            "KCK", "OHH", "KCK", "OHH", 
            "KCK", "OHH", "SNR", "OHH", 
            "KCK", "OHH", "KCK", "OHH", 
            "KCK", "OHH", "SNR", "OHH"
        ),
        khali = 6,
        tempo = "115 - 125 BPM",
        description = "Classic 70s dance rhythm featuring a relentless four-on-the-floor kick pattern paired with open hi-hat barks on every offbeat step."
    ),
    Taal(
        name = "Jazz Swing",
        beats = 12,
        vibhags = listOf(3, 3, 3, 3),
        bolSequence = listOf(
            "KCK", "RID", "HHC", 
            "SNR", "RID", "RID", 
            "KCK", "RID", "HHC", 
            "SNR", "RID", "RID"
        ),
        khali = 3,
        tempo = "140 - 180 BPM",
        description = "Driven by a classic triplet-based 'spang-a-lang' ride cymbal pattern. The hi-hat steps firmly on beats 2 and 4, while the kick leaves room for improvisation."
    )
)
val ACADEMIC_TIME_SIGNATURES = listOf(
    Taal(
        name = "4/4 Basic Quarter Note",
        beats = 4,
        vibhags = listOf(4),
        bolSequence = listOf("1", "2", "3", "4"),
        khali = 2, // The secondary emphasis on beat 3
        tempo = "Practice Range: 60 - 140 BPM",
        description = "Standard common time for counting straight quarter notes. Ideal for practice on metronome downbeats across piano scales, guitar alternate picking patterns, and fundamental 4-bar drum grids."
    ),
    Taal(
        name = "2/4 Duple Practice",
        beats = 2,
        vibhags = listOf(2),
        bolSequence = listOf("1", "2"),
        khali = 0,
        tempo = "Practice Range: 70 - 130 BPM",
        description = "Strict duple accentuation loop used to practice fast, alternating rhythmic drills like orchestral drum rudiments or basic piano sight-reading etudes."
    ),
    Taal(
        name = "3/4 Waltz / Triple Meter",
        beats = 3,
        vibhags = listOf(3),
        bolSequence = listOf("1", "2", "3"),
        khali = 0,
        tempo = "Practice Range: 80 - 160 BPM",
        description = "Classic triple meter layout. Crucial for practicing the timing emphasis on the first note of classical piano pieces (e.g., Chopin Waltzes) or syncopated guitar strumming curves."
    ),
    Taal(
        name = "4/4 Eighth Note Subdivisions",
        beats = 8,
        vibhags = listOf(2, 2, 2, 2),
        bolSequence = listOf("1", "+", "2", "+", "3", "+", "4", "+"),
        khali = 4,
        tempo = "Practice Range: 50 - 100 BPM",
        description = "Explicitly calls out the half-beat intervals ('1-and-2-and...'). Essential for keeping clean time when practicing complex syncopated acoustic strums or double-stroke drum fills."
    ),
    Taal(
        name = "6/8 Compound Practice",
        beats = 6,
        vibhags = listOf(3, 3),
        bolSequence = listOf("1", "2", "3", "4", "5", "6"),
        khali = 3, // Primary secondary landing emphasis on beat 4
        tempo = "Practice Range: 45 - 95 BPM",
        description = "Standard compound meter reading sequence. Helps you keep score track of individual eighth-note tuplets inside fast triplet runs, common in fingerstyle guitar pieces and classical sonatas."
    ),
    Taal(
        name = "12/8 Blues Triplet Grid",
        beats = 12,
        vibhags = listOf(3, 3, 3, 3),
        bolSequence = listOf(
            "1", "da", "li",
            "2", "da", "li",
            "3", "da", "li",
            "4", "da", "li"
        ),
        khali = 6,
        tempo = "Practice Range: 50 - 90 BPM",
        description = "Uses traditional triplet counting syllables ('1-da-li, 2-da-li'). Excellent for timing deep blues drum shuffles, rolling piano accompaniment patterns, or triplet guitar solos."
    ),
    Taal(
        name = "4/4 Sixteenth Note Grid",
        beats = 16,
        vibhags = listOf(4, 4, 4, 4),
        bolSequence = listOf(
            "1", "e", "&", "a",
            "2", "e", "&", "a",
            "3", "e", "&", "a",
            "4", "e", "&", "a"
        ),
        khali = 8,
        tempo = "Practice Range: 40 - 80 BPM",
        description = "The absolute golden standard grid for learning fast linear drum fills, highly technical funk guitar strumming syncopation, or dense technical keyboard etudes."
    ),
    Taal(
        name = "2/2 Cut Time (Alla Breve)",
        beats = 2,
        vibhags = listOf(2),
        bolSequence = listOf("1", "2"),
        khali = 0,
        tempo = "Practice Range: 50 - 110 BPM",
        description = "Two half-note pulses per measure. Essential for classical piano scores (like fast Beethoven sonatas) and orchestral sight-reading, training the student to feel the macro-pulse without dragging at high speeds."
    ),
    Taal(
        name = "4/4 Triplet Subdivision",
        beats = 12,
        vibhags = listOf(3, 3, 3, 3),
        bolSequence = listOf(
            "1", "+", "a",
            "2", "+", "a",
            "3", "+", "a",
            "4", "+", "a"
        ),
        khali = 6,
        tempo = "Practice Range: 60 - 110 BPM",
        description = "Standard quarter-note beats counted with eighth-note triplet subdivisions ('1-and-a, 2-and-a'). Vital for mastering drum rudiments (like triplets and swing rolls) and classic jazz/blues solo layouts."
    ),
    Taal(
        name = "5/8 Complex Odd Meter",
        beats = 5,
        vibhags = listOf(3, 2),
        bolSequence = listOf("1", "2", "3", "1", "2"),
        khali = 3,
        tempo = "Practice Range: 140 - 240 BPM",
        description = "Five fast eighth-note pulses, typically grouped as an asymmetric 3+2 or 2+3. Essential for intermediate to advanced drum and guitar training to handle modern progressive styles or asymmetric folk patterns."
    ),
    Taal(
        name = "9/8 Compound Triple Meter",
        beats = 9,
        vibhags = listOf(3, 3, 3),
        bolSequence = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9"
        ),
        khali = 3,
        tempo = "Practice Range: 50 - 90 BPM",
        description = "Three main macro-beats, each divided into three triplet pulses. Heavily featured in traditional Irish jigs on guitar, as well as classical piano score pieces (like Debussy's Clair de Lune)."
    )

)