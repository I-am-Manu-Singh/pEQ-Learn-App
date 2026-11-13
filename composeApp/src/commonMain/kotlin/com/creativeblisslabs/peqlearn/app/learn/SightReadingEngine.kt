package com.creativeblisslabs.peqlearn.app.learn

import kotlin.random.Random

/**
 * SIGHT-READING GENERATOR
 * Generates note streams for interactive sight-reading practice.
 * 
 * Implements:
 * 1. Stepwise voice leading bias (70% stepwise ±2 diatonic notes, 30% jump)
 * 2. Consecutive duplicate elimination
 * 3. Weak-note frequency weighting (3x multiplier on missed notes)
 * 4. Distinct pedagogical tiers (Landmark, C-Major, Grand Staff, Key Signatures, Arcade)
 */
object SightReadingEngine {

    data class TierConfig(
        val id: String,
        val name: String,
        val pitchPool: List<Int>,
        val defaultBpm: Int,
        val clef: String = "treble",
        val keySignature: String = "C"
    )

    val TIERS = listOf(
        TierConfig(
            id = "landmark",
            name = "Landmark Notes",
            pitchPool = listOf(60, 64, 67, 72), // C4, E4, G4, C5
            defaultBpm = 48
        ),
        TierConfig(
            id = "c_major_1",
            name = "C Major - 1 Octave",
            pitchPool = listOf(60, 62, 64, 65, 67, 69, 71, 72), // C4-C5
            defaultBpm = 54
        ),
        TierConfig(
            id = "c_major_2",
            name = "C Major - 2 Octaves",
            pitchPool = listOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84), // C4-C6
            defaultBpm = 60
        ),
        TierConfig(
            id = "grand_staff",
            name = "Grand Staff (Bass + Treble)",
            pitchPool = listOf(
                48, 50, 52, 53, 55, 57, 59, // Bass C3-B3
                60, 62, 64, 65, 67, 69, 71, // Middle C4-B4
                72, 74, 76, 77, 79, 81, 83, 84 // Treble C5-C6
            ),
            defaultBpm = 56,
            clef = "both"
        ),
        TierConfig(
            id = "key_signatures",
            name = "Key Signatures (G Major)",
            pitchPool = listOf(55, 57, 59, 60, 62, 64, 66, 67, 69, 71, 72, 74, 76, 78, 79),
            defaultBpm = 60,
            keySignature = "G"
        ),
        TierConfig(
            id = "arcade",
            name = "Arcade Survival",
            pitchPool = listOf(60, 62, 64, 65, 67, 69, 71, 72),
            defaultBpm = 60
        )
    )

    // Pattern Collections from ftrain/sightreading repository
    val STEPWISE_PATTERNS = listOf(
        listOf(1, 2, 3), listOf(3, 2, 1), listOf(1, 2, 3, 2, 1),
        listOf(5, 4, 3), listOf(3, 4, 5), listOf(1, 2, 3, 4, 5),
        listOf(5, 4, 3, 2, 1), listOf(1, 2, 1), listOf(3, 2, 3), listOf(5, 6, 5)
    )

    val TRIADIC_PATTERNS = listOf(
        listOf(1, 3, 5), listOf(5, 3, 1), listOf(1, 3, 5, 3),
        listOf(5, 3, 1, 3), listOf(1, 5, 3), listOf(3, 1, 5)
    )

    val FOLK_PATTERNS = listOf(
        listOf(3, 2, 1, 2, 3, 3, 3),        // Mary Had a Little Lamb
        listOf(1, 1, 5, 5, 6, 6, 5),        // Twinkle Twinkle
        listOf(3, 2, 1),                    // Hot Cross Buns
        listOf(3, 3, 4, 5, 5, 4, 3, 2),     // Ode to Joy
        listOf(1, 1, 1, 2, 3, 2, 1, 3, 2),  // Au Clair de la Lune
        listOf(1, 2, 3, 1),                 // Frère Jacques
        listOf(1, 1, 1, 2, 3),              // Row Row Row Your Boat
        listOf(5, 4, 3, 4, 5, 5, 5)         // London Bridge
    )

    val CLASSICAL_PATTERNS = listOf(
        listOf(1, 5, 3, 5),              // Bach Bass
        listOf(1, 3, 5, 3),              // Bach Motif
        listOf(1, 5, 3, 5, 1, 5, 3, 5),  // Alberti Bass
        listOf(5, 4, 5, 4, 5, 2, 4, 3),  // Für Elise Theme
        listOf(1, 5, 3),                 // Chopin Nocturne Bass
        listOf(5, 4, 3, 2, 1)            // Cadential Run
    )

    val WIDE_INTERVAL_PATTERNS = listOf(
        listOf(1, 6), listOf(6, 1), listOf(1, 6, 1), listOf(3, 8), listOf(8, 3),
        listOf(1, 7), listOf(7, 1), listOf(2, 8), listOf(1, 7, 5, 3, 1),
        listOf(1, 8), listOf(8, 1), listOf(1, 8, 1), listOf(5, 5, 8, 8),
        listOf(1, 5, 8), listOf(8, 5, 3, 1), listOf(1, 3, 5, 8, 5, 3, 1)
    )

    fun scaleDegreeToMidi(degree: Int, keySignature: String = "C", isRightHand: Boolean = true): Int {
        val keyOffsets = mapOf(
            "C" to 0, "G" to 7, "F" to 5, "D" to 2, "Bb" to 10, "A" to 9,
            "Eb" to 3, "E" to 4, "Ab" to 8, "B" to 11, "Db" to 1, "F#" to 6, "Gb" to 6, "C#" to 1
        )
        val rootOffset = keyOffsets[keySignature] ?: 0
        val baseMidi = if (isRightHand) 60 + rootOffset else 48 + rootOffset
        val intervals = listOf(0, 2, 4, 5, 7, 9, 11, 12)
        val idx = (degree - 1).coerceIn(0, 7)
        return baseMidi + intervals[idx]
    }

    data class SightReadingSubLevel(
        val levelNumber: Int,
        val subLevelChar: Char, // 'a', 'b', 'c', 'd'
        val title: String,
        val hand: String, // "right", "left", "both"
        val keySignature: String,
        val timeSig: String,
        val bpm: Int,
        val restProbability: Float,
        val maxInterval: Int,
        val durations: List<Int>, // 4 = Whole, 3 = Dotted Half, 2 = Half, 1 = Quarter
        val pitches: List<Int>
    )

    fun getSubLevelsForLevel(levelNumber: Int): List<SightReadingSubLevel> {
        val keySig = when (levelNumber) {
            1, 2, 3, 4, 5, 6, 7 -> "C"
            8 -> "G"
            9 -> "F"
            10 -> "D"
            11 -> "Bb"
            12 -> "A"
            13 -> "Eb"
            14 -> "E"
            15 -> "Ab"
            16 -> "B"
            17 -> "Db"
            18 -> "F#"
            19 -> "Gb"
            else -> "C#"
        }

        val baseBpm = (30 + (levelNumber - 1) * 3).coerceIn(30, 100)
        val hands = listOf("right", "left", "right", "both")
        val subChars = listOf('a', 'b', 'c', 'd')
        val handNames = listOf("Right Hand (RH)", "Left Hand (LH)", "Right Hand Varied (RH)", "Both Hands Dual Clef (BH)")

        return (0..3).map { subIdx ->
            val h = hands[subIdx]
            val subChar = subChars[subIdx]
            val title = "Level ${levelNumber}$subChar: ${handNames[subIdx]} in $keySig Major"

            val pitches = generateFtrainSequence(
                levelNumber = levelNumber,
                subLevelIndex = subIdx,
                length = 4,
                keySignature = keySig,
                hand = h
            )

            val durs = when {
                levelNumber <= 3 -> listOf(4)
                levelNumber == 4 && subIdx < 2 -> listOf(2)
                levelNumber == 4 -> listOf(4, 2)
                levelNumber == 5 && subIdx < 2 -> listOf(1)
                levelNumber == 5 -> listOf(1, 2)
                levelNumber == 6 -> listOf(1, 2)
                levelNumber == 7 -> listOf(3, 2, 1)
                else -> if (subIdx < 2) listOf(4) else listOf(1, 2)
            }

            val restProb = when {
                levelNumber <= 5 -> 0.0f
                levelNumber == 6 && subIdx < 2 -> 0.15f
                levelNumber == 6 -> 0.10f
                subIdx >= 2 -> 0.10f
                else -> 0.0f
            }

            SightReadingSubLevel(
                levelNumber = levelNumber,
                subLevelChar = subChar,
                title = title,
                hand = h,
                keySignature = keySig,
                timeSig = if (levelNumber == 6 && subIdx >= 2) "3/4" else "4/4",
                bpm = baseBpm + (subIdx * 5),
                restProbability = restProb,
                maxInterval = if (levelNumber >= 14) 5 else if (levelNumber >= 12) 4 else 2,
                durations = durs,
                pitches = pitches
            )
        }
    }

    /**
     * Generates a procedural pitch sequence based on the ftrain/sightreading algorithm & motifs.
     */
    fun generateFtrainSequence(
        levelNumber: Int,
        subLevelIndex: Int = 0,
        length: Int = 4,
        keySignature: String = "C",
        hand: String = "right"
    ): List<Int> {
        val patternList = when {
            levelNumber <= 2 && subLevelIndex == 0 -> listOf(listOf(1, 5), listOf(5, 1), listOf(1, 1, 5))
            levelNumber <= 2 && subLevelIndex == 1 -> TRIADIC_PATTERNS
            levelNumber <= 2 -> STEPWISE_PATTERNS
            levelNumber <= 5 -> STEPWISE_PATTERNS + TRIADIC_PATTERNS + FOLK_PATTERNS
            levelNumber <= 7 -> FOLK_PATTERNS + WIDE_INTERVAL_PATTERNS
            else -> FOLK_PATTERNS + CLASSICAL_PATTERNS + STEPWISE_PATTERNS
        }

        val selectedMotif = patternList.random()
        val isRH = hand != "left"
        return selectedMotif.take(length).map { degree ->
            scaleDegreeToMidi(degree, keySignature, isRH)
        }
    }

    // --- LEAFO/SIGHTREADING.TRAINING ALGORITHMS & GENERATORS ---

    /**
     * Leafo MiniSteps Generator: Generates 2-3 note stepwise motion up or down.
     */
    fun generateLeafoMiniSteps(
        startPitch: Int = 60,
        stepCount: Int = 3,
        ascending: Boolean = true,
        keySignature: String = "C"
    ): List<Int> {
        val result = mutableListOf(startPitch)
        var current = startPitch
        val step = if (ascending) 1 else -1
        repeat(stepCount - 1) {
            val scaleDegree = ((current - 60) % 12 + 12) % 12
            val nextDegree = (scaleDegree + step).coerceIn(1, 8)
            current = scaleDegreeToMidi(nextDegree, keySignature, isRightHand = current >= 60)
            result.add(current)
        }
        return result
    }

    /**
     * Leafo PositionGenerator: 5-finger pattern placement generator with finger landmarking.
     */
    fun generateLeafoPositionSequence(
        rootPitch: Int = 60,
        length: Int = 5,
        keySignature: String = "C"
    ): List<Int> {
        val pentascaleDegrees = listOf(1, 2, 3, 4, 5)
        val shuffled = pentascaleDegrees.shuffled()
        return shuffled.take(length).map { degree ->
            scaleDegreeToMidi(degree, keySignature, isRightHand = rootPitch >= 60)
        }
    }

    /**
     * Leafo IntervalGenerator: Generates leaps bounded by maxInterval semitones with direction reversing at limits.
     */
    fun generateLeafoIntervalSequence(
        startPitch: Int = 60,
        maxInterval: Int = 5,
        length: Int = 4,
        keySignature: String = "C"
    ): List<Int> {
        val result = mutableListOf(startPitch)
        var current = startPitch
        var dir = 1
        val minP = startPitch - 12
        val maxP = startPitch + 12

        repeat(length - 1) {
            val step = (1..maxInterval).random() * dir
            val nextP = current + step
            if (nextP !in minP..maxP) {
                dir = -dir
                current = (current - step).coerceIn(minP, maxP)
            } else {
                current = nextP
            }
            result.add(current)
        }
        return result
    }

    /**
     * Leafo MultiKeyChordGenerator: Modulates across major keys in the Circle of Fifths with shared pivot chords.
     */
    fun generateLeafoModulatingProgression(
        startKey: String = "C",
        numChords: Int = 4
    ): List<Pair<String, List<Int>>> {
        val circleOfFifths = listOf("C", "G", "D", "A", "E", "B", "F#", "Db", "Ab", "Eb", "Bb", "F")
        var currentKeyIdx = circleOfFifths.indexOf(startKey).coerceAtLeast(0)

        val result = mutableListOf<Pair<String, List<Int>>>()
        repeat(numChords) { step ->
            val key = circleOfFifths[currentKeyIdx]
            val chordDegrees = listOf(1, 4, 5, 6).random() // I, IV, V, vi
            val rootMidi = scaleDegreeToMidi(chordDegrees, key, isRightHand = true)
            val triadPitches = listOf(rootMidi, rootMidi + 4, rootMidi + 7)

            result.add(key to triadPitches)

            // 20% chance to modulate to adjacent key in Circle of Fifths
            if (Random.nextFloat() < 0.20f) {
                currentKeyIdx = (currentKeyIdx + listOf(-1, 1).random() + 12) % 12
            }
        }
        return result
    }

    fun getTier(id: String): TierConfig {
        return TIERS.find { it.id == id } ?: TIERS[0]
    }

    data class MusicianshipLevel(
        val levelNumber: Int,
        val stageIndex: Int, // 1..5
        val stageName: String,
        val title: String,
        val category: String, // "Sight Reading", "Ear Training", "Rhythm", "Piano Technique", "World Rhythms"
        val description: String,
        val pitchPool: List<Int>,
        val bpm: Int = 60,
        val clef: String = "treble",
        val keySignature: String = "C",
        val timeSig: String = "4/4"
    )

    val ALL_LEVELS: List<MusicianshipLevel> by lazy {
        val list = mutableListOf<MusicianshipLevel>()
        val categories = listOf("Sight Reading", "Ear Training", "Rhythm", "Piano Technique", "World Rhythms")

        for (lvl in 1..100) {
            val stageIdx = when (lvl) {
                in 1..20 -> 1
                in 21..40 -> 2
                in 41..60 -> 3
                in 61..80 -> 4
                else -> 5
            }
            val stageName = when (stageIdx) {
                1 -> "Stage 1: Novice Foundation"
                2 -> "Stage 2: Intermediate Notation & Rhythmic Dynamics"
                3 -> "Stage 3: Advanced Key Signatures & Harmony"
                4 -> "Stage 4: Master Harmony & Complex Meter"
                else -> "Stage 5: Virtuoso Musicianship & World Rhythms"
            }
            val category = categories[(lvl - 1) % categories.size]

            val (title, pool, clef, keySig, bpm, timeSig, desc) = when (lvl) {
                1 -> Tuple7("Middle C Landmark", listOf(60), "treble", "C", 48, "4/4", "Identify and strike Middle C (C4) on the Grand Staff.")
                2 -> Tuple7("Treble G Landmark", listOf(67), "treble", "C", 50, "4/4", "Identify Treble Clef G4 landmark note.")
                3 -> Tuple7("Bass F Landmark", listOf(53), "bass", "C", 48, "4/4", "Identify Bass Clef F3 landmark note.")
                4 -> Tuple7("3-Note Pentascale (C-D-E)", listOf(60, 62, 64), "treble", "C", 52, "4/4", "Read stepwise motion between C4, D4, and E4.")
                5 -> Tuple7("C Major Pentascale (C-G)", listOf(60, 62, 64, 65, 67), "treble", "C", 56, "4/4", "5-finger C major pentascale reading.")
                6 -> Tuple7("Bass C Pentascale (C3-G3)", listOf(48, 50, 52, 53, 55), "bass", "C", 52, "4/4", "Left hand 5-finger bass pentascale.")
                7 -> Tuple7("Quarter Note Pulse Tapper", listOf(60, 64, 67), "treble", "C", 60, "4/4", "Tap steady quarter note beats on time.")
                8 -> Tuple7("C Major Full Octave (C4-C5)", listOf(60, 62, 64, 65, 67, 69, 71, 72), "treble", "C", 60, "4/4", "Read 8 diatonic notes of C Major.")
                9 -> Tuple7("Unison & 2nd Intervals", listOf(60, 62, 64), "treble", "C", 56, "4/4", "Recognize melodic seconds (stepwise notes).")
                10 -> Tuple7("3rd Intervals (Melodic C-E-G)", listOf(60, 64, 67), "treble", "C", 58, "4/4", "Read 3rd skip intervals.")
                11 -> Tuple7("Half & Whole Note Rhythms", listOf(60, 64, 67, 72), "treble", "C", 64, "4/4", "Sustain half notes (2 beats) and whole notes (4 beats).")
                12 -> Tuple7("Treble High Notes (C5-G5)", listOf(72, 74, 76, 77, 79), "treble", "C", 60, "4/4", "Read upper ledger and line notes in Treble clef.")
                13 -> Tuple7("Bass Low Notes (C2-C3)", listOf(36, 38, 40, 41, 43, 45, 47, 48), "bass", "C", 54, "4/4", "Read low bass notes in Bass clef.")
                14 -> Tuple7("Harmonic 3rds (C+E Double Hit)", listOf(60, 64, 67), "treble", "C", 58, "4/4", "Recognize 2 stacked notes played together.")
                15 -> Tuple7("8th Note Subdivisions (1-and-2-and)", listOf(60, 62, 64, 65, 67), "treble", "C", 64, "4/4", "Subdivide 8th notes accurately.")
                16 -> Tuple7("Grand Staff Both Hands (C3-C5)", listOf(48, 52, 55, 60, 64, 67, 72), "both", "C", 56, "4/4", "Dual clef reading across Bass & Treble.")
                17 -> Tuple7("3/4 Waltz Time & Dotted Halves", listOf(60, 64, 67), "treble", "C", 66, "3/4", "Read 3 beats per measure with dotted half notes.")
                18 -> Tuple7("G Major Scale (F♯ Accidental)", listOf(55, 57, 59, 60, 62, 64, 66, 67), "treble", "G", 60, "4/4", "Read G Major scale with F♯ key signature.")
                19 -> Tuple7("4th & 5th Intervals (Perfect 4/5)", listOf(60, 65, 67, 72), "treble", "C", 60, "4/4", "Recognize wide skips of 4ths and 5ths.")
                20 -> Tuple7("Novice Stage Checkpoint", listOf(48, 55, 60, 64, 67, 72), "both", "G", 64, "4/4", "Complete Stage 1 foundation examination.")
                
                21 -> Tuple7("F Major Scale (B♭ Accidental)", listOf(53, 55, 57, 58, 60, 62, 64, 65), "treble", "F", 60, "4/4", "Read F Major with B♭ key signature.")
                22 -> Tuple7("D Major Scale (2 Sharps: F♯, C♯)", listOf(62, 64, 66, 67, 69, 71, 73, 74), "treble", "D", 62, "4/4", "D Major 2-sharp key signature.")
                23 -> Tuple7("Major vs Minor Triads Ear & Sight", listOf(60, 63, 64, 67), "treble", "C", 60, "4/4", "Distinguish major (C-E-G) vs minor (C-E♭-G) triads.")
                24 -> Tuple7("2/4 Marching Time Signature", listOf(60, 62, 64, 67), "treble", "C", 72, "2/4", "Read 2 beats per measure marching feel.")
                25 -> Tuple7("16th Note Subdivisions (1-e-and-a)", listOf(60, 62, 64, 65, 67), "treble", "C", 58, "4/4", "Subdivide 16th notes with steady pulse.")
                26 -> Tuple7("Dotted Quarter + 8th Rhythms", listOf(60, 64, 67, 72), "treble", "C", 64, "4/4", "Read syncopated dotted quarter note figures.")
                27 -> Tuple7("A Natural Minor Scale (A3-A4)", listOf(57, 59, 60, 62, 64, 65, 67, 69), "treble", "C", 60, "4/4", "Read relative minor scale without accidentals.")
                28 -> Tuple7("Major & Minor 6th Intervals", listOf(60, 68, 69, 72), "treble", "C", 60, "4/4", "Recognize 6th interval skips.")
                29 -> Tuple7("B♭ Major Scale (2 Flats: B♭, E♭)", listOf(58, 60, 62, 63, 65, 67, 69, 70), "treble", "Bb", 60, "4/4", "Read B♭ Major key signature.")
                30 -> Tuple7("Ledger Lines Above & Below Staff", listOf(57, 59, 60, 72, 74, 76, 77, 79, 81), "treble", "C", 56, "4/4", "Read 2 lines above/below treble staff.")
                31 -> Tuple7("D Minor & E Minor Scales", listOf(62, 64, 65, 67, 69, 70, 72, 74), "treble", "F", 60, "4/4", "Read natural minor key signatures.")
                32 -> Tuple7("6/8 Compound Duple Meter", listOf(60, 64, 67, 72), "treble", "C", 72, "6/8", "Read 6/8 meter with 2 dotted-quarter beats.")
                33 -> Tuple7("Major & Minor 7th Intervals", listOf(60, 70, 71, 72), "treble", "C", 58, "4/4", "Recognize 7th interval leaps.")
                34 -> Tuple7("Triad Inversions (Root & 1st Inv)", listOf(60, 64, 67, 72, 76), "treble", "C", 60, "4/4", "Read 1st inversion triad voicings (E-G-C).")
                35 -> Tuple7("A Major Scale (3 Sharps: F♯, C♯, G♯)", listOf(57, 59, 61, 62, 64, 66, 68, 69), "treble", "A", 62, "4/4", "Read 3-sharp key signature.")
                36 -> Tuple7("E♭ Major Scale (3 Flats: B♭, E♭, A♭)", listOf(63, 65, 67, 68, 70, 72, 74, 75), "treble", "Eb", 62, "4/4", "Read 3-flat key signature.")
                37 -> Tuple7("Syncopation & Off-Beat Accents", listOf(60, 64, 67, 72), "treble", "C", 68, "4/4", "Tap syncopated off-beat 8th note accents.")
                38 -> Tuple7("Octave Leaps (C4 to C5)", listOf(48, 60, 72, 84), "both", "C", 60, "4/4", "Read wide 8va octave leaps.")
                39 -> Tuple7("Triad Inversions (2nd Inversion)", listOf(64, 67, 72, 76, 79), "treble", "C", 60, "4/4", "Read 2nd inversion triad voicings (G-C-E).")
                40 -> Tuple7("Intermediate Stage Checkpoint", listOf(48, 55, 60, 65, 67, 72, 77), "both", "D", 64, "4/4", "Complete Stage 2 intermediate examination.")
                
                else -> {
                    val p = 48 + ((lvl * 3) % 36)
                    val kSig = listOf("C", "G", "D", "A", "F", "Bb", "Eb")[(lvl) % 7]
                    val cF = if (lvl % 3 == 0) "both" else if (lvl % 2 == 0) "bass" else "treble"
                    val tSig = listOf("4/4", "3/4", "6/8", "5/4", "7/8")[(lvl) % 5]
                    Tuple7(
                        "Level $lvl: ${category} Mastery",
                        listOf(p, p + 2, p + 4, p + 5, p + 7, p + 9, p + 11, p + 12),
                        cF,
                        kSig,
                        (60 + (lvl / 2)).coerceIn(48, 140),
                        tSig,
                        "Advanced Level $lvl curriculum challenge covering $category."
                    )
                }
            }

            list.add(
                MusicianshipLevel(
                    levelNumber = lvl,
                    stageIndex = stageIdx,
                    stageName = stageName,
                    title = title,
                    category = category,
                    description = desc,
                    pitchPool = pool,
                    bpm = bpm,
                    clef = clef,
                    keySignature = keySig,
                    timeSig = timeSig
                )
            )
        }
        list
    }

    private data class Tuple7<A, B, C, D, E, F, G>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
    )

    /**
     * Generates next note given the previous note and player's weak note map.
     */
    fun nextNote(
        tier: TierConfig,
        lastPitch: Int? = null,
        weakNotes: Set<Int> = emptySet(),
        random: Random = Random.Default
    ): Int {
        val pool = tier.pitchPool
        if (pool.isEmpty()) return 60
        if (pool.size == 1) return pool[0]

        // Candidate pool with duplicate excluded
        val available = if (lastPitch != null) pool.filter { it != lastPitch } else pool
        if (available.isEmpty()) return pool.first()

        // 70% chance to pick stepwise within +/- 4 semitones of last pitch
        val stepwise = if (lastPitch != null) {
            available.filter { kotlin.math.abs(it - lastPitch) <= 4 }
        } else emptyList()

        val pickFrom = if (stepwise.isNotEmpty() && random.nextFloat() < 0.70f) {
            stepwise
        } else {
            available
        }

        // Apply 3x weight to weak notes
        val weightedList = mutableListOf<Int>()
        for (pitch in pickFrom) {
            val weight = if (weakNotes.contains(pitch)) 3 else 1
            repeat(weight) { weightedList.add(pitch) }
        }

        return weightedList.random(random)
    }

    /**
     * Pitch to readable notation name (e.g. 60 -> "C4", 61 -> "C#4")
     */
    fun pitchToName(midiPitch: Int): String {
        val noteNames = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
        val octave = (midiPitch / 12) - 1
        val note = noteNames[midiPitch % 12]
        return "$note$octave"
    }
}

