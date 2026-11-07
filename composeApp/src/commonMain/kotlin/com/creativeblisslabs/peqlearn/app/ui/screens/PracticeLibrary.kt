package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.runtime.mutableStateMapOf

data class PracticeScaleItem(
    val name: String,
    val category: String,
    val rootPitch: Int = 60,
    val intervals: List<Int>,
    val fingeringRightHand: List<Int>,
    val fingeringLeftHand: List<Int>,
    val description: String
)

object PracticeLibrary {

    // Tracks user's best accuracy (0..100%) per tutorial ID for progressive method-book mastery
    val exerciseMasteryMap = mutableStateMapOf<String, Int>()

    fun recordExerciseScore(tutorialId: String, accuracyPercent: Int) {
        val prev = exerciseMasteryMap[tutorialId] ?: 0
        if (accuracyPercent > prev) {
            exerciseMasteryMap[tutorialId] = accuracyPercent.coerceIn(0, 100)
        }
    }

    val items = listOf(
        PracticeScaleItem(
            name = "C Major Scale",
            category = "Scales",
            rootPitch = 60,
            intervals = listOf(0, 2, 4, 5, 7, 9, 11, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "Natural major scale without sharps or flats. Essential foundation for classical and contemporary piano."
        ),
        PracticeScaleItem(
            name = "G Major Scale",
            category = "Scales",
            rootPitch = 67,
            intervals = listOf(0, 2, 4, 5, 7, 9, 11, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "One sharp (F#). Perfect practice for cross-under thumb technique."
        ),
        PracticeScaleItem(
            name = "D Major Scale",
            category = "Scales",
            rootPitch = 62,
            intervals = listOf(0, 2, 4, 5, 7, 9, 11, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "Two sharps (F#, C#). Builds symmetrical two-black-key hand alignment."
        ),
        PracticeScaleItem(
            name = "F Major Scale",
            category = "Scales",
            rootPitch = 65,
            intervals = listOf(0, 2, 4, 5, 7, 9, 10, 12),
            fingeringRightHand = listOf(1, 2, 3, 4, 1, 2, 3, 4),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "One flat (Bb). Teaches the classic 4th-finger-on-Bb right-hand crossover rule."
        ),
        PracticeScaleItem(
            name = "A Natural Minor",
            category = "Scales",
            rootPitch = 57,
            intervals = listOf(0, 2, 3, 5, 7, 8, 10, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "Relative minor of C Major, creating a solemn, contemplative tone."
        ),
        PracticeScaleItem(
            name = "A Harmonic Minor",
            category = "Scales",
            rootPitch = 57,
            intervals = listOf(0, 2, 3, 5, 7, 8, 11, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "Raised 7th degree (G#) creates the classic augmented-second exotic pull to the tonic."
        ),
        PracticeScaleItem(
            name = "C Minor Pentatonic & Blues",
            category = "Scales",
            rootPitch = 60,
            intervals = listOf(0, 3, 5, 6, 7, 10, 12),
            fingeringRightHand = listOf(1, 2, 3, 4, 1, 3, 4),
            fingeringLeftHand = listOf(4, 3, 2, 1, 3, 2, 1),
            description = "The fundamental blues scale with the blue note (Gb/F#) for improvisation."
        ),
        PracticeScaleItem(
            name = "Chromatic Scale (1 Octave)",
            category = "Scales",
            rootPitch = 60,
            intervals = (0..12).toList(),
            fingeringRightHand = listOf(1, 3, 1, 3, 1, 2, 3, 1, 3, 1, 3, 1, 2),
            fingeringLeftHand = listOf(1, 3, 1, 3, 2, 1, 3, 1, 3, 1, 3, 2, 1),
            description = "Standard French chromatic fingering using thumb (1) on white keys and middle finger (3) on black keys."
        ),
        PracticeScaleItem(
            name = "C Major Arpeggio (2 Octaves)",
            category = "Arpeggios",
            rootPitch = 60,
            intervals = listOf(0, 4, 7, 12, 16, 19, 24),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 5),
            fingeringLeftHand = listOf(5, 4, 2, 1, 4, 2, 1),
            description = "Ascending two-octave major triad arpeggio for wrist flexibility and lateral thumb under-reach."
        ),
        PracticeScaleItem(
            name = "A Minor Arpeggio (2 Octaves)",
            category = "Arpeggios",
            rootPitch = 57,
            intervals = listOf(0, 3, 7, 12, 15, 19, 24),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 5),
            fingeringLeftHand = listOf(5, 4, 2, 1, 4, 2, 1),
            description = "Minor triad two-octave arpeggio building finger independence."
        ),
        PracticeScaleItem(
            name = "G Dominant 7th Arpeggio",
            category = "Arpeggios",
            rootPitch = 55,
            intervals = listOf(0, 4, 7, 10, 12, 16, 19, 22, 24),
            fingeringRightHand = listOf(1, 2, 3, 4, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 4, 3, 2, 1),
            description = "Four-note dominant 7th arpeggio essential for Classical cadences and Jazz soloing."
        ),
        PracticeScaleItem(
            name = "ii - V - I Jazz Cadence",
            category = "Chords",
            rootPitch = 60,
            intervals = listOf(2, 5, 9, 12, 7, 11, 14, 17, 0, 4, 7, 11),
            fingeringRightHand = listOf(1, 2, 3, 5, 1, 2, 3, 5, 1, 2, 3, 5),
            fingeringLeftHand = listOf(5, 3, 2, 1, 5, 3, 2, 1, 5, 3, 2, 1),
            description = "The timeless jazz cadence: Dmin7 to G7 to Cmaj7."
        ),
        PracticeScaleItem(
            name = "I - V - vi - IV Pop Progression",
            category = "Chords",
            rootPitch = 60,
            intervals = listOf(0, 4, 7, 12, 7, 11, 14, 19, 9, 12, 16, 21, 5, 9, 12, 17),
            fingeringRightHand = listOf(1, 3, 5, 5, 1, 3, 5, 5, 1, 3, 5, 5, 1, 3, 5, 5),
            fingeringLeftHand = listOf(5, 3, 1, 1, 5, 3, 1, 1, 5, 3, 1, 1, 5, 3, 1, 1),
            description = "The most popular four chords in modern music (C - G - Am - F)."
        )
    ) + com.creativeblisslabs.peqlearn.app.util.RealProgressionsDb.list.map { prog ->
        val intervals = mutableListOf<Int>()
        prog.degrees.forEach { deg ->
            val rootOffset = when (deg.uppercase().replace("7", "").replace("M", "")) {
                "I" -> 0; "II" -> 2; "III" -> 4; "IV" -> 5; "V" -> 7; "VI" -> 9; "VII" -> 11; else -> 0
            }
            val isMinor = deg.firstOrNull()?.isLowerCase() == true
            val third = if (isMinor) 3 else 4
            intervals.addAll(listOf(rootOffset, rootOffset + third, rootOffset + 7, rootOffset + 12))
        }
        PracticeScaleItem(
            name = prog.name,
            category = "Tutorial: ${prog.category}",
            rootPitch = 60,
            intervals = intervals,
            fingeringRightHand = List(intervals.size) { (it % 4) + 1 },
            fingeringLeftHand = List(intervals.size) { 5 - (it % 4) },
            description = "${prog.romanDisplay} • ${prog.description} (e.g. ${prog.examples})"
        )
    }

    fun createScoreForPracticeItem(item: PracticeScaleItem): Score {
        val notes = mutableListOf<ScoreNote>()
        var currentBeat = 0.0f

        item.intervals.forEachIndexed { index, semitoneOffset ->
            val pitch = item.rootPitch + semitoneOffset
            val rhFinger = item.fingeringRightHand.getOrNull(index)
            val isRh = pitch >= 60

            notes.add(
                ScoreNote(
                    id = index + 1,
                    midiNote = pitch,
                    position = currentBeat,
                    duration = 1.0f,
                    voice = if (isRh) 0 else 1,
                    fingering = rhFinger,
                    chordSymbol = if (index == 0) item.name else ""
                )
            )
            currentBeat += 1.0f
        }

        val measures = mutableListOf<Measure>()
        val beatsPerMeasure = 4
        var measureIdx = 1
        var noteIdx = 0

        while (noteIdx < notes.size) {
            val chunk = mutableListOf<ScoreNote>()
            val chunkStartBeat = (measureIdx - 1) * beatsPerMeasure.toFloat()
            val chunkEndBeat = chunkStartBeat + beatsPerMeasure

            while (noteIdx < notes.size && notes[noteIdx].position < chunkEndBeat) {
                val n = notes[noteIdx]
                chunk.add(n.copy(position = n.position - chunkStartBeat))
                noteIdx++
            }

            measures.add(
                Measure(
                    notes = chunk,
                    measureIndex = measureIdx,
                    timeSignatureNumerator = 4,
                    timeSignatureDenominator = 4,
                    barline = if (noteIdx >= notes.size) "final" else "single"
                )
            )
            measureIdx++
        }

        return Score(
            title = item.name,
            composer = "pEQ Learn Studio",
            tempo = 80f,
            tempoText = "Andante",
            measures = measures
        )
    }

    data class BuiltinTutorialMidi(
        val id: String,
        val fileName: String,
        val title: String,
        val composer: String,
        val era: String,
        val difficulty: String, // "Beginner", "Intermediate", "Advanced"
        val timeSignature: String,
        val tempo: Int,
        val totalMeasures: Int,
        val description: String,
        val focusAreas: List<String>,
        val bookCategory: String = "Masterworks", // "Beyer Op. 101", "Burgmüller Op. 100", "Czerny Op. 599", "Czerny Op. 777", "Czerny Op. 299", "Hanon Virtuoso", "MusicXML Scores", "Masterworks"
        val exerciseNumber: Int = 0
    )

    private val burgmullerOp100Titles = mapOf(
        1 to "La Candeur (Candor)",
        2 to "L'Arabesque (Arabesque)",
        3 to "La Pastorale (Pastoral)",
        4 to "La Petite Réunion (The Little Party)",
        5 to "Innocence",
        6 to "Progrès (Progress)",
        7 to "Le Courant Limpide (The Limpid Stream)",
        8 to "La Gracieuse (Gracefulness)",
        9 to "La Chasse (The Chase)",
        10 to "Tendre Fleur (Tender Flower)",
        11 to "La Bergeronnette (The Young Shepherdess)",
        12 to "L'Adieu (The Farewell)",
        13 to "Consolation",
        14 to "La Styrienne",
        15 to "Ballade",
        16 to "Douce Plainte (Gentle Lament)",
        17 to "La Babillarde (The Chatterbox)",
        18 to "Inquiétude (Restlessness)",
        19 to "Ave Maria",
        20 to "Tarentelle",
        21 to "L'Harmonie des Anges (Angelic Harmony)",
        22 to "Barcarolle",
        23 to "Le Retour (The Return)",
        24 to "L'Hirondelle (The Swallow)",
        25 to "La Chevaleresque (The KnightErrant)"
    )

    val curatedTutorials = listOf(
        BuiltinTutorialMidi(
            id = "bach_prelude",
            fileName = "midi/scores/bach-prelude-in-c.mid",
            title = "Prelude in C Major (BWV 846)",
            composer = "J.S. Bach",
            era = "Baroque",
            difficulty = "Beginner",
            timeSignature = "4/4",
            tempo = 68,
            totalMeasures = 35,
            description = "Iconic arpeggiated keyboard prelude. Ideal for developing consistent tempo, smooth finger independence, and fluid hand balancing.",
            focusAreas = listOf("Arpeggio Fluency", "Hand Coordination", "LH Bass Hold"),
            bookCategory = "Masterworks",
            exerciseNumber = 1
        ),
        BuiltinTutorialMidi(
            id = "satie_gnossienne",
            fileName = "midi/scores/satie-gnossienne-1.mid",
            title = "Gnossienne No. 1",
            composer = "Erik Satie",
            era = "Impressionist",
            difficulty = "Beginner",
            timeSignature = "4/4",
            tempo = 92,
            totalMeasures = 82,
            description = "Mesmerizing modal piece with rhythmic left-hand ostinato chords and expressive right-hand phrases.",
            focusAreas = listOf("LH Chord Jumps", "Modal Harmony", "Expressive Touch"),
            bookCategory = "Masterworks",
            exerciseNumber = 2
        ),
        BuiltinTutorialMidi(
            id = "chopin_nocturne",
            fileName = "midi/scores/chopin-nocturne-op9-2.mid",
            title = "Nocturne in E-flat Major (Op. 9 No. 2)",
            composer = "Frédéric Chopin",
            era = "Romantic",
            difficulty = "Intermediate",
            timeSignature = "12/8",
            tempo = 66,
            totalMeasures = 38,
            description = "Chopin's most beloved nocturne. Features lyrical right-hand bel canto ornamentation over rich left-hand waltz chords.",
            focusAreas = listOf("Rubato & Phrasing", "LH Stride Jumps", "12/8 Compound Meter"),
            bookCategory = "Masterworks",
            exerciseNumber = 3
        ),
        BuiltinTutorialMidi(
            id = "mozart_rondo_alla_turca",
            fileName = "midi/scores/marche-turque.mid",
            title = "Rondo alla Turca (Turkish March)",
            composer = "W.A. Mozart",
            era = "Classical",
            difficulty = "Intermediate",
            timeSignature = "2/4",
            tempo = 120,
            totalMeasures = 126,
            description = "Vibrant 3rd movement of Mozart's Sonata K. 331 featuring crisp 16th-note runs and broken-octave ornaments.",
            focusAreas = listOf("Rapid 16th Runs", "Staccato Articulation", "Octave Jumps"),
            bookCategory = "Masterworks",
            exerciseNumber = 4
        ),
        BuiltinTutorialMidi(
            id = "chopin_fantaisie",
            fileName = "midi/scores/chopin-fantaisie-impromptu-op66.mid",
            title = "Fantaisie-Impromptu in C# Minor (Op. 66)",
            composer = "Frédéric Chopin",
            era = "Romantic",
            difficulty = "Advanced",
            timeSignature = "4/4",
            tempo = 132,
            totalMeasures = 138,
            description = "Famous for its cross-rhythm: 4 sixteenth notes in the right hand against 3 triplet eighth notes in the left hand (4:3 polyrhythm).",
            focusAreas = listOf("4:3 Polyrhythm", "Virtuoso Velocity", "LH Arpeggio Stamina"),
            bookCategory = "Masterworks",
            exerciseNumber = 5
        )
    )

    val builtInTutorials: List<BuiltinTutorialMidi>
        get() = curatedTutorials

    fun loadTutorialScore(
        tutorial: BuiltinTutorialMidi,
        readBytes: (String) -> ByteArray?,
        trainingMode: String = "FULL",
        targetTempoBpm: Float? = null
    ): Score? {
        val bytes = readBytes(tutorial.fileName)
            ?: readBytes(tutorial.fileName.substringAfterLast("/"))
            ?: return null
        val baseScore = com.creativeblisslabs.peqlearn.app.util.MidiFileParser.parse(bytes, tutorial.title) ?: return null

        val effectiveTempo = targetTempoBpm ?: when (trainingMode) {
            "SLOW_TEMPO" -> (baseScore.tempo * 0.7f).coerceAtLeast(40f)
            else -> baseScore.tempo
        }

        val filteredMeasures = baseScore.measures.map { measure ->
            val notes = when (trainingMode) {
                "RH_ONLY" -> measure.notes.filter { it.voice == 0 || it.midiNote >= 60 }
                "LH_ONLY" -> measure.notes.filter { it.voice == 1 || it.midiNote < 60 }
                else -> measure.notes
            }
            measure.copy(notes = notes)
        }

        val titleSuffix = when (trainingMode) {
            "RH_ONLY" -> " (Right Hand Study)"
            "LH_ONLY" -> " (Left Hand Study)"
            "SLOW_TEMPO" -> " (Practice Tempo)"
            else -> ""
        }

        return baseScore.copy(
            title = "${tutorial.title}$titleSuffix",
            composer = tutorial.composer,
            tempo = effectiveTempo,
            measures = filteredMeasures
        )
    }

    /**
     * Ultra-fast, non-blocking catalog builder for all 8 asset directories:
     * - assets/musicxml/scores
     * - assets/midi/scores
     * - assets/midi/beyer_op101 (003.mid .. 106.mid)
     * - assets/midi/burgmuller_op100 (burgmuller_op100_1.mid .. 25.mid)
     * - assets/midi/czerny_op299 (czerny_op299-01.mid .. 20.mid)
     * - assets/midi/czerny_op599 (Op_599-001.mid .. 100.mid)
     * - assets/midi/czerny_op777 (Cz777-01.mid .. 09.mid)
     * - assets/midi/hanon_the_virtuoso_pianist (Exercise_01_Hanon.mid .. 60.mid)
     */
    fun buildTutorialCatalog(
        assetFileNames: List<String>,
        readBytes: (String) -> ByteArray? = { null }
    ): List<BuiltinTutorialMidi> {
        val curatedByBareName = curatedTutorials.associateBy {
            it.fileName.substringAfterLast("/").lowercase()
        }

        val catalog = assetFileNames
            .filter {
                it.endsWith(".mid", true) ||
                        it.endsWith(".midi", true) ||
                        it.endsWith(".xml", true) ||
                        it.endsWith(".musicxml", true) ||
                        it.endsWith(".mxl", true)
            }
            .distinctBy { it.lowercase() }
            .map { fullPath ->
                val bareFile = fullPath.substringAfterLast("/")
                val lowerPath = fullPath.lowercase()
                val lowerBare = bareFile.lowercase()

                curatedByBareName[lowerBare]?.let { cur ->
                    return@map cur.copy(fileName = fullPath)
                }

                val extractedNum = Regex("""(\d+)""").findAll(bareFile.substringBeforeLast("."))
                    .lastOrNull()?.groupValues?.get(1)?.toIntOrNull() ?: 0

                when {
                    lowerPath.contains("beyer_op101") || lowerBare.matches(Regex("""^\d{3}\.mid$""")) -> {
                        val exNum = extractedNum.coerceAtLeast(1)
                        val diff = if (exNum <= 45) "Beginner" else "Intermediate"
                        BuiltinTutorialMidi(
                            id = "beyer_op101_$exNum",
                            fileName = fullPath,
                            title = "Beyer Op. 101 – No. $exNum",
                            composer = "Ferdinand Beyer",
                            era = "Classical Pedagogy",
                            difficulty = diff,
                            timeSignature = "4/4",
                            tempo = if (exNum <= 35) 72 else 88,
                            totalMeasures = 16,
                            description = "Progressive Elementary Piano School (Vorschule im Klavierspiel Op. 101), Exercise No. $exNum. Builds five-finger position reading and hand independence.",
                            focusAreas = listOf("Sight Reading", "Five-Finger Position", "Hand Independence"),
                            bookCategory = "Beyer Op. 101",
                            exerciseNumber = exNum
                        )
                    }

                    lowerPath.contains("burgmuller_op100") || lowerBare.contains("burgmuller") -> {
                        val exNum = extractedNum.coerceIn(1, 25)
                        val pieceName = burgmullerOp100Titles[exNum] ?: "Étude No. $exNum"
                        val diff = if (exNum <= 7) "Beginner" else "Intermediate"
                        BuiltinTutorialMidi(
                            id = "burgmuller_op100_$exNum",
                            fileName = fullPath,
                            title = "Op. 100 No. $exNum – $pieceName",
                            composer = "Friedrich Burgmüller",
                            era = "Romantic",
                            difficulty = diff,
                            timeSignature = if (exNum in listOf(3, 6, 8, 20)) "6/8" else "4/4",
                            tempo = if (exNum in listOf(2, 9, 14, 20, 25)) 116 else 88,
                            totalMeasures = 24,
                            description = "25 Easy and Progressive Studies (Op. 100), No. $exNum: $pieceName. Develops poetic phrasing, agile fingerwork, and Romantic expression.",
                            focusAreas = listOf("Poetic Phrasing", "Articulation", "Voicing & Dynamics"),
                            bookCategory = "Burgmüller Op. 100",
                            exerciseNumber = exNum
                        )
                    }

                    lowerPath.contains("czerny_op599") || lowerBare.contains("op_599") || lowerBare.contains("599") -> {
                        val exNum = extractedNum.coerceIn(1, 100)
                        val diff = if (exNum <= 40) "Beginner" else "Intermediate"
                        BuiltinTutorialMidi(
                            id = "czerny_op599_$exNum",
                            fileName = fullPath,
                            title = "Czerny Op. 599 – Practical Study No. $exNum",
                            composer = "Carl Czerny",
                            era = "Classical",
                            difficulty = diff,
                            timeSignature = "4/4",
                            tempo = if (exNum <= 30) 80 else 100,
                            totalMeasures = 16,
                            description = "Practical Method for Beginners (Op. 599), Study No. $exNum. Trains evenness of touch, Alberti bass accompaniment, and crisp right-hand passagework.",
                            focusAreas = listOf("Evenness of Touch", "Alberti Bass", "Scale Passages"),
                            bookCategory = "Czerny Op. 599",
                            exerciseNumber = exNum
                        )
                    }

                    lowerPath.contains("czerny_op777") || lowerBare.contains("cz777") || lowerBare.contains("777") -> {
                        val exNum = extractedNum.coerceIn(1, 24)
                        BuiltinTutorialMidi(
                            id = "czerny_op777_$exNum",
                            fileName = fullPath,
                            title = "Czerny Op. 777 – Five-Finger Study No. $exNum",
                            composer = "Carl Czerny",
                            era = "Classical",
                            difficulty = "Beginner",
                            timeSignature = "4/4",
                            tempo = 76,
                            totalMeasures = 16,
                            description = "24 Five-Finger Melodious Exercises (Op. 777), No. $exNum. Right hand stays in a fixed 5-note compass while developing lyrical touch.",
                            focusAreas = listOf("5-Note Compass", "Beginner Melodic Touch", "Steady Rhythm"),
                            bookCategory = "Czerny Op. 777",
                            exerciseNumber = exNum
                        )
                    }

                    lowerPath.contains("czerny_op299") || lowerBare.contains("299") -> {
                        val exNum = extractedNum.coerceIn(1, 40)
                        BuiltinTutorialMidi(
                            id = "czerny_op299_$exNum",
                            fileName = fullPath,
                            title = "Czerny Op. 299 – School of Velocity No. $exNum",
                            composer = "Carl Czerny",
                            era = "Classical / Virtuoso",
                            difficulty = "Advanced",
                            timeSignature = "4/4",
                            tempo = 128,
                            totalMeasures = 32,
                            description = "School of Velocity (Schule der Geläufigkeit Op. 299), Étude No. $exNum. High-velocity 16th-note runs, brilliant arpeggios, and finger dexterity.",
                            focusAreas = listOf("High Velocity", "Rapid 16th Scales", "Finger Dexterity"),
                            bookCategory = "Czerny Op. 299",
                            exerciseNumber = exNum
                        )
                    }

                    lowerPath.contains("hanon") -> {
                        val exNum = extractedNum.coerceIn(1, 60)
                        val partLabel = when {
                            exNum <= 20 -> "Part I (Preparatory)"
                            exNum <= 43 -> "Part II (Transcendental)"
                            else -> "Part III (Virtuoso)"
                        }
                        val diff = when {
                            exNum <= 20 -> "Beginner"
                            exNum <= 43 -> "Intermediate"
                            else -> "Advanced"
                        }
                        BuiltinTutorialMidi(
                            id = "hanon_ex_$exNum",
                            fileName = fullPath,
                            title = "Hanon Exercise No. $exNum – $partLabel",
                            composer = "Charles-Louis Hanon",
                            era = "Technical Pedagogy",
                            difficulty = diff,
                            timeSignature = "2/4",
                            tempo = 108,
                            totalMeasures = 30,
                            description = "The Virtuoso Pianist in 60 Exercises, No. $exNum ($partLabel). Builds equal strength in fingers 4 & 5, wrist suppleness, and parallel-hand precision.",
                            focusAreas = listOf("Fingers 4 & 5 Strength", "Parallel Hands", "Metronomic Precision"),
                            bookCategory = "Hanon Virtuoso",
                            exerciseNumber = exNum
                        )
                    }

                    else -> {
                        val isXml = lowerBare.endsWith(".xml") || lowerBare.endsWith(".musicxml") || lowerBare.endsWith(".mxl")
                        val cleanTitle = bareFile
                            .substringBeforeLast(".")
                            .replace(Regex("[_-]+"), " ")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                            .replaceFirstChar { it.uppercase() }
                        val comp = detectComposer(cleanTitle)
                        val diff = inferRepertoireDifficulty(cleanTitle, comp)
                        BuiltinTutorialMidi(
                            id = bareFile.substringBeforeLast(".").lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                            fileName = fullPath,
                            title = cleanTitle,
                            composer = comp,
                            era = detectEra(comp),
                            difficulty = diff,
                            timeSignature = "4/4",
                            tempo = 96,
                            totalMeasures = 32,
                            description = "Concert repertoire study: $cleanTitle by $comp. Practice with hand isolation, A–B bar looping, and guided USB MIDI play-along.",
                            focusAreas = listOf("Sight Reading", "Hand Coordination", "Musical Phrasing"),
                            bookCategory = if (isXml) "MusicXML Scores" else "Masterworks",
                            exerciseNumber = extractedNum
                        )
                    }
                }
            }

        return catalog.sortedWith(
            compareBy<BuiltinTutorialMidi> { it.bookCategory }
                .thenBy { if (it.exerciseNumber > 0) it.exerciseNumber else Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )
    }

    private fun inferRepertoireDifficulty(title: String, composer: String): String {
        val low = title.lowercase()
        return when {
            low.contains("minuet") || low.contains("prelude in c") || low.contains("gymnopedie") ||
                    low.contains("gnossienne") || low.contains("fur elise") || low.contains("easy") ||
                    low.contains("sonatina") -> "Beginner"
            low.contains("etude") || low.contains("fantaisie") || low.contains("ballade") ||
                    low.contains("rhapsody") || low.contains("scherzo") || low.contains("concerto") ||
                    composer in listOf("Franz Liszt", "Sergei Rachmaninoff") -> "Advanced"
            else -> "Intermediate"
        }
    }

    private fun detectComposer(title: String): String {
        val composers = linkedMapOf(
            "rachmaninoff" to "Sergei Rachmaninoff",
            "tchaikovsky" to "Pyotr Ilyich Tchaikovsky",
            "beethoven" to "Ludwig van Beethoven",
            "burgmuller" to "Friedrich Burgmüller",
            "beyer" to "Ferdinand Beyer",
            "czerny" to "Carl Czerny",
            "hanon" to "Charles-Louis Hanon",
            "chopin" to "Frédéric Chopin",
            "mozart" to "W.A. Mozart",
            "debussy" to "Claude Debussy",
            "schubert" to "Franz Schubert",
            "brahms" to "Johannes Brahms",
            "schumann" to "Robert Schumann",
            "mendelssohn" to "Felix Mendelssohn",
            "handel" to "G.F. Handel",
            "grieg" to "Edvard Grieg",
            "liszt" to "Franz Liszt",
            "satie" to "Erik Satie",
            "bach" to "J.S. Bach",
            "pachelbel" to "Johann Pachelbel",
            "clementi" to "Muzio Clementi"
        )
        val normalized = title.lowercase()
        return composers.entries.firstOrNull { normalized.contains(it.key) }?.value ?: "Classical Repertoire"
    }

    private fun detectEra(composer: String): String {
        return when (composer) {
            "J.S. Bach", "G.F. Handel", "Johann Pachelbel" -> "Baroque"
            "W.A. Mozart", "Ludwig van Beethoven", "Muzio Clementi", "Carl Czerny", "Ferdinand Beyer" -> "Classical"
            "Frédéric Chopin", "Franz Liszt", "Pyotr Ilyich Tchaikovsky", "Franz Schubert",
            "Johannes Brahms", "Robert Schumann", "Felix Mendelssohn", "Edvard Grieg", "Friedrich Burgmüller" -> "Romantic"
            "Claude Debussy", "Erik Satie" -> "Impressionist"
            else -> "Classical Studio"
        }
    }
}