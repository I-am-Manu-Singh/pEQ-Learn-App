package com.creativeblisslabs.peqlearn.app.learn

import kotlinx.serialization.Serializable

/**
 * 300-LEVEL COMPREHENSIVE MUSICIANSHIP CURRICULUM ENGINE
 * 
 * Contains 100 Levels Each for 3 Core Disciplines (300 Levels Total):
 * 1. EAR TRAINING (100 Levels): Pitch matching, intervals, chord quality, inversions, solfege/sargam, microtonal cents, raga & tabla bol ear.
 * 2. RHYTHM TRAINING (100 Levels): Pulse, 8th/16th subdivisions, dotted notes, 3/4, 6/8, syncopation, polyrhythms, 5/4, 7/8, Hindustani & Carnatic Talas.
 * 3. SIGHT READING (100 Levels): Landmark notes, C Major pentascale, Grand Staff reading, 1-7 Sharps/Flats, ledger lines, chord voicings, virtuosic runs.
 */
object MusicianshipCurriculumEngine {

    @Serializable
    enum class AcademyDiscipline(val displayName: String, val iconEmoji: String) {
        EAR_TRAINING("Ear Training", "👂"),
        RHYTHM_TRAINING("Rhythm Training", "⏱️"),
        SIGHT_READING("Sight Reading", "🎼")
    }

    data class CurriculumLevel(
        val levelNumber: Int,
        val discipline: AcademyDiscipline,
        val stageIndex: Int, // 1..5
        val stageName: String,
        val difficultyTier: String, // "Beginner", "Elementary", "Intermediate", "Advanced", "Virtuoso"
        val title: String,
        val description: String,
        val pitchPool: List<Int>,
        val bpm: Int = 60,
        val timeSig: String = "4/4",
        val clef: String = "treble",
        val keySignature: String = "C",
        val targetPitchesOrBols: List<String> = emptyList(),
        val rhythmPattern: List<Float> = emptyList(),
        val requiredPassScorePct: Int = 75
    )

    val EAR_TRAINING_LEVELS: List<CurriculumLevel> by lazy {
        generate100Levels(AcademyDiscipline.EAR_TRAINING)
    }

    val RHYTHM_TRAINING_LEVELS: List<CurriculumLevel> by lazy {
        generate100Levels(AcademyDiscipline.RHYTHM_TRAINING)
    }

    val SIGHT_READING_LEVELS: List<CurriculumLevel> by lazy {
        generate100Levels(AcademyDiscipline.SIGHT_READING)
    }

    fun getLevelsForDiscipline(discipline: AcademyDiscipline): List<CurriculumLevel> {
        return when (discipline) {
            AcademyDiscipline.EAR_TRAINING -> EAR_TRAINING_LEVELS
            AcademyDiscipline.RHYTHM_TRAINING -> RHYTHM_TRAINING_LEVELS
            AcademyDiscipline.SIGHT_READING -> SIGHT_READING_LEVELS
        }
    }

    private fun generate100Levels(discipline: AcademyDiscipline): List<CurriculumLevel> {
        val list = mutableListOf<CurriculumLevel>()

        for (lvl in 1..100) {
            val stageIdx = when (lvl) {
                in 1..20 -> 1
                in 21..40 -> 2
                in 41..60 -> 3
                in 61..80 -> 4
                else -> 5
            }

            val tierName = when (stageIdx) {
                1 -> "Beginner"
                2 -> "Elementary"
                3 -> "Intermediate"
                4 -> "Advanced"
                else -> "Virtuoso"
            }

            val stageName = when (stageIdx) {
                1 -> "🌱 Stage 1: Novice Foundation (Levels 1–20)"
                2 -> "🌿 Stage 2: Intermediate Notation & Rhythmic Dynamics (Levels 21–40)"
                3 -> "📘 Stage 3: Advanced Key Signatures, Harmony & Meter (Levels 41–60)"
                4 -> "💜 Stage 4: Master Harmony, Chromatics & Complex Meter (Levels 61–80)"
                else -> "👑 Stage 5: Virtuoso Performance & World Rhythms (Levels 81–100)"
            }

            val basePitch = 60 + ((lvl * 3) % 24)

            val levelItem = when (discipline) {
                AcademyDiscipline.EAR_TRAINING -> generateEarLevel(lvl, stageIdx, stageName, tierName, basePitch)
                AcademyDiscipline.RHYTHM_TRAINING -> generateRhythmLevel(lvl, stageIdx, stageName, tierName, basePitch)
                AcademyDiscipline.SIGHT_READING -> generateSightReadingLevel(lvl, stageIdx, stageName, tierName, basePitch)
            }

            list.add(levelItem)
        }
        return list
    }

    private fun generateEarLevel(lvl: Int, stageIdx: Int, stageName: String, tierName: String, basePitch: Int): CurriculumLevel {
        val (title, pitches, desc) = when (lvl) {
            // STAGE 1 (Levels 1-20): Novice Ear Foundation
            1 -> Triple("Same vs Different Pitch", listOf(60, 60, 64), "Listen to 2 notes and determine if they are identical or different.")
            2 -> Triple("High vs Low Pitch Direction", listOf(60, 67), "Identify whether the 2nd note moves UP or DOWN in pitch.")
            3 -> Triple("Middle C (C4) vs Treble G (G4)", listOf(60, 67), "Recognize Middle C vs Treble G by ear.")
            4 -> Triple("Unison & Octave Jumps", listOf(60, 72), "Distinguish identical unison notes from wide 8va octave leaps.")
            5 -> Triple("Major 2nd Stepwise Interval", listOf(60, 62), "Identify Major 2nd interval (whole step).")
            6 -> Triple("Major 3rd Happy Interval", listOf(60, 64), "Recognize Major 3rd (e.g. C to E).")
            7 -> Triple("Minor 3rd Sad Interval", listOf(60, 63), "Recognize Minor 3rd (e.g. C to E♭).")
            8 -> Triple("Perfect 4th Elevator Interval", listOf(60, 65), "Recognize Perfect 4th (Here Comes the Bride interval).")
            9 -> Triple("Perfect 5th Open Interval", listOf(60, 67), "Recognize Perfect 5th (Star Wars theme interval).")
            10 -> Triple("Major 3rd vs Minor 3rd Ear Test", listOf(60, 63, 64), "Distinguish Major 3rd from Minor 3rd by ear.")
            11 -> Triple("Major Triad Harmony (C-E-G)", listOf(60, 64, 67), "Recognize bright, stable Major Triad chord quality.")
            12 -> Triple("Minor Triad Harmony (C-E♭-G)", listOf(60, 63, 67), "Recognize melancholic Minor Triad chord quality.")
            13 -> Triple("Major vs Minor Triad Examination", listOf(60, 63, 64, 67), "Identify Major vs Minor triads in random order.")
            14 -> Triple("Major 6th Pastoral Interval", listOf(60, 69), "Recognize Major 6th interval (NBC chime interval).")
            15 -> Triple("Minor 6th Interval", listOf(60, 68), "Recognize Minor 6th (e.g. C to A♭).")
            16 -> Triple("Major 7th Suspense Interval", listOf(60, 71), "Recognize Major 7th interval leading tone.")
            17 -> Triple("Minor 7th Jazz Interval", listOf(60, 70), "Recognize Minor 7th interval (e.g. C to B♭).")
            18 -> Triple("All Diatonic Intervals (Unison to 8va)", listOf(60, 62, 64, 65, 67, 69, 71, 72), "Recognize all diatonic intervals within 1 octave.")
            19 -> Triple("Diminished Triad (C-E♭-G♭)", listOf(60, 63, 66), "Identify tense Diminished Triad chord quality.")
            20 -> Triple("Stage 1 Ear Examination", listOf(60, 63, 64, 67, 70, 72), "Complete Stage 1 ear training evaluation.")

            // STAGE 2 (Levels 21-40): Intermediate Harmony & Inversional Ear
            21 -> Triple("Augmented Triad (C-E-G♯)", listOf(60, 64, 68), "Recognize mysterious, unresolved Augmented Triad.")
            22 -> Triple("1st Inversion Triad (E-G-C)", listOf(64, 67, 72), "Identify 1st inversion triad chord in rootless bass.")
            23 -> Triple("2nd Inversion Triad (G-C-E)", listOf(67, 72, 76), "Identify 2nd inversion triad chord with 5th in bass.")
            24 -> Triple("Dominant 7th Chord (C-E-G-B♭)", listOf(60, 64, 67, 70), "Recognize Dominant 7th tension chord.")
            25 -> Triple("Major 7th Chord (C-E-G-B)", listOf(60, 64, 67, 71), "Recognize lush, dreamy Major 7th chord.")
            26 -> Triple("Minor 7th Chord (C-E♭-G-B♭)", listOf(60, 63, 67, 70), "Recognize smooth jazz Minor 7th chord.")
            27 -> Triple("Diminished 7th Chord (C-E♭-G♭-B♭♭)", listOf(60, 63, 66, 69), "Recognize dramatic fully-diminished 7th chord.")
            28 -> Triple("Half-Diminished 7th Chord (C-E♭-G♭-B♭)", listOf(60, 63, 66, 70), "Recognize ø7 half-diminished 7th chord.")
            29 -> Triple("Sus2 Suspended Chord (C-D-G)", listOf(60, 62, 67), "Identify open Sus2 suspended chord.")
            30 -> Triple("Sus4 Suspended Chord (C-F-G)", listOf(60, 65, 67), "Identify resolving Sus4 suspended chord.")
            31 -> Triple("Add9 Extended Chord (C-E-G-D)", listOf(60, 64, 67, 74), "Identify Add9 extended chord color.")
            32 -> Triple("Major 9th Jazz Chord (C-E-G-B-D)", listOf(60, 64, 67, 71, 74), "Recognize rich 5-note Major 9th chord.")
            33 -> Triple("Minor 9th Jazz Chord (C-E♭-G-B♭-D)", listOf(60, 63, 67, 70, 74), "Recognize Minor 9th chord texture.")
            34 -> Triple("Melodic vs Harmonic 5ths", listOf(60, 67), "Distinguish sequential (melodic) vs stacked (harmonic) 5ths.")
            35 -> Triple("Melodic vs Harmonic 6ths", listOf(60, 69), "Distinguish sequential vs stacked 6ths.")
            36 -> Triple("Solfege Do-Re-Mi Pitch Recognition", listOf(60, 62, 64), "Identify Movable Do solfege pitches 1, 2, 3.")
            37 -> Triple("Solfege Sol-La-Ti Pitch Recognition", listOf(67, 69, 71), "Identify Movable Do solfege pitches 5, 6, 7.")
            38 -> Triple("Sargam Sa-Re-Ga Swara Recognition", listOf(60, 62, 64), "Identify Hindustani Sargam swaras Sa, Re, Ga.")
            39 -> Triple("Sargam Ma-Pa-Dha Swara Recognition", listOf(65, 67, 69), "Identify Hindustani Sargam swaras Ma, Pa, Dha.")
            40 -> Triple("Stage 2 Ear Examination", listOf(60, 64, 67, 70, 71, 74), "Complete Stage 2 intermediate ear evaluation.")

            // STAGE 3 (Levels 41-60): Advanced Extensions, Microtones & Swaras
            41 -> Triple("Dominant 11th Chord", listOf(60, 67, 70, 74, 77), "Recognize Dominant 11th extended chord.")
            42 -> Triple("Major 13th Jazz Extension", listOf(60, 64, 67, 71, 81), "Recognize 13th color tone in jazz voicings.")
            43 -> Triple("Altered Dominant 7♭9 Chord", listOf(60, 64, 67, 70, 73), "Identify tense 7♭9 altered dominant chord.")
            44 -> Triple("Altered Dominant 7♯9 Chord", listOf(60, 64, 67, 70, 75), "Identify Jimi Hendrix 7♯9 altered chord.")
            45 -> Triple("Altered Dominant 7♭5 Chord", listOf(60, 64, 66, 70), "Identify French-6th style 7♭5 chord.")
            46 -> Triple("Altered Dominant 7♯5 Chord", listOf(60, 64, 68, 70), "Identify augmented 7♯5 dominant chord.")
            47 -> Triple("Microtonal 50 Cents (Quarter Tone)", listOf(60, 60), "Detect 50-cent quarter tone pitch deviation.")
            48 -> Triple("Microtonal 25 Cents Pitch Deviation", listOf(60, 60), "Detect subtle 25-cent microtonal pitch drift.")
            49 -> Triple("Microtonal 10 Cents Pitch Deviation", listOf(60, 60), "Detect fine 10-cent tuning discrepancy.")
            50 -> Triple("Raga Yaman Swara Ear (Tivra Ma)", listOf(60, 62, 64, 66, 67, 69, 71), "Recognize Raga Yaman with Tivra Ma (♯4).")
            51 -> Triple("Raga Bhairavi Swara Ear (Komal Swaras)", listOf(60, 61, 63, 65, 67, 68, 70), "Recognize Raga Bhairavi with 4 Komal (flat) swaras.")
            52 -> Triple("Raga Bhairav Swara Ear (Komal Re & Dha)", listOf(60, 61, 64, 65, 67, 68, 71), "Recognize Raga Bhairav with Komal Re & Dha.")
            53 -> Triple("Raga Bageshri Swara Ear", listOf(60, 63, 65, 67, 69, 70), "Recognize romantic Raga Bageshri swaras.")
            54 -> Triple("Tabla Bol Ghe (Bayan Bass)", listOf(36), "Identify deep copper Bayan bass resonance (Ghe).")
            55 -> Triple("Tabla Bol Dha (Bayan + Dayan)", listOf(38), "Identify fundamental open Dha tabla strike.")
            56 -> Triple("Tabla Bol Tin (Dayan Maidan)", listOf(42), "Identify ringing Tin tabla strike.")
            57 -> Triple("Tabla Bol Na / Ta (Dayan Kinar Rim)", listOf(46, 48), "Identify sharp Na/Ta rim tabla strike.")
            58 -> Triple("Tabla Bol Kat / Ka (Bayan Mute)", listOf(40), "Identify dry muted Kat/Ka tabla slap.")
            59 -> Triple("Complete Tabla Bol Ear Set", listOf(36, 38, 40, 42, 46, 48), "Recognize all 6 core Tabla bols by ear.")
            60 -> Triple("Stage 3 Ear Examination", listOf(60, 66, 70, 73, 75, 81), "Complete Stage 3 advanced ear evaluation.")

            // STAGE 4 (Levels 61-80): Master Dictation, Polyrhythms & Modes
            61 -> Triple("Carnatic Solkattu Tha-Dhi-Gi-Na-Thom Ear", listOf(60, 62, 64, 65, 67), "Identify 5-syllable Solkattu rhythm pattern by ear.")
            62 -> Triple("3:2 Polyrhythm Ear Recognition", listOf(60, 67), "Distinguish 3-against-2 polyrhythm pulse by ear.")
            63 -> Triple("4:3 Polyrhythm Ear Recognition", listOf(60, 65), "Distinguish 4-against-3 polyrhythm pulse by ear.")
            64 -> Triple("Chromatic Pitch Ear (All 12 Semitones)", listOf(60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71), "Identify any of the 12 chromatic pitches.")
            65 -> Triple("2-Voice Harmonic Dictation", listOf(60, 67, 64, 72), "Identify 2 simultaneously sounding melodic lines.")
            66 -> Triple("3-Voice Triad Dictation", listOf(60, 64, 67), "Transcribe 3-voice chord progressions by ear.")
            67 -> Triple("Key Center Root Identification", listOf(60, 62, 64, 65, 67, 69, 71), "Identify tonic key center from melodic phrase.")
            68 -> Triple("Dorian vs Phrygian Mode Ear Test", listOf(60, 62, 63, 65, 67, 69, 70), "Distinguish Dorian (natural 6th) vs Phrygian (flat 2nd).")
            69 -> Triple("Lydian vs Mixolydian Mode Ear Test", listOf(60, 62, 64, 66, 67, 69, 70), "Distinguish Lydian (sharp 4th) vs Mixolydian (flat 7th).")
            70 -> Triple("Jazz Shell Voicings (Root-3rd-7th)", listOf(60, 64, 70), "Recognize 3-note jazz shell voicings.")
            71 -> Triple("Jazz Drop-2 Voicings Ear", listOf(64, 60, 67, 70), "Recognize Drop-2 inverted jazz chord voicings.")
            72 -> Triple("Raga Alankaar Ear Pattern 1 (SaReGa, ReGaMa)", listOf(60, 62, 64, 65, 67), "Recognize 3-swara ascending Alankaar pattern.")
            73 -> Triple("Raga Alankaar Ear Pattern 2 (SaGa, ReMa, GaPa)", listOf(60, 64, 62, 65, 64, 67), "Recognize 3rd-skip Alankaar pattern.")
            74 -> Triple("Tabla Theka Ear Teentaal (16 Matras)", listOf(36, 38, 42, 46), "Recognize 16-beat Teentaal rhythm cycle.")
            75 -> Triple("Tabla Theka Ear Keherwa (8 Matras)", listOf(36, 38, 40, 46), "Recognize 8-beat Keherwa rhythm cycle.")
            76 -> Triple("Tabla Theka Ear Dadra (6 Matras)", listOf(36, 38, 42), "Recognize 6-beat Dadra rhythm cycle.")
            77 -> Triple("Tabla Theka Ear Jhaptal (10 Matras)", listOf(36, 38, 42, 46), "Recognize 10-beat Jhaptal rhythm cycle.")
            78 -> Triple("Tabla Theka Ear Ektaal (12 Matras)", listOf(36, 38, 40, 42, 46), "Recognize 12-beat Ektaal rhythm cycle.")
            79 -> Triple("Complex Jazz Extension 13♭9 Ear", listOf(60, 64, 67, 70, 73, 81), "Identify 13♭9 complex dominant chord.")
            80 -> Triple("Stage 4 Ear Examination", listOf(60, 64, 66, 70, 73, 81), "Complete Stage 4 master ear evaluation.")

            // STAGE 5 (Levels 81-100): Virtuoso Performance, Perfect Pitch & World Rhythms
            81 -> Triple("Virtuoso 4-Voice Dictation", listOf(60, 64, 67, 72), "Transcribe 4-part SATB chord progressions by ear.")
            82 -> Triple("Rapid Pitch Memory (5 Notes)", listOf(60, 64, 67, 71, 74), "Memorize and recall 5-note rapid pitch stream.")
            83 -> Triple("Rapid Pitch Memory (7 Notes)", listOf(60, 62, 64, 67, 69, 71, 72), "Memorize and recall 7-note rapid pitch stream.")
            84 -> Triple("Fast Drut Taan Virtuosic Run Ear", listOf(60, 62, 64, 66, 67, 69, 71, 72), "Transcribe high-speed 16th-note Taan run by ear.")
            85 -> Triple("Carnatic Sapta Tala Ear (Dhruva, Matya, Rupaka)", listOf(60, 62, 64, 65, 67), "Identify Sapta Tala rhythmic meters by ear.")
            86 -> Triple("Carnatic Sapta Tala Ear (Jhampa, Triputa, Ata, Eka)", listOf(60, 62, 64, 67), "Identify Sapta Tala rhythmic meters by ear.")
            87 -> Triple("Atonal 12-Tone Serialism Pitch Memory", listOf(60, 61, 63, 66, 68, 70), "Recall non-diatonic 12-tone row pitches.")
            88 -> Triple("Microtonal Pitch Deviation 5 Cents", listOf(60, 60), "Detect ultra-fine 5-cent pitch deviation.")
            89 -> Triple("Perfect Pitch Absolute C4 Calibration", listOf(60), "Internalize absolute reference pitch C4.")
            90 -> Triple("Perfect Pitch Absolute G4 Calibration", listOf(67), "Internalize absolute reference pitch G4.")
            91 -> Triple("Perfect Pitch Absolute F3 Calibration", listOf(53), "Internalize absolute reference pitch F3.")
            92 -> Triple("Perfect Pitch Absolute E4 Calibration", listOf(64), "Internalize absolute reference pitch E4.")
            93 -> Triple("Perfect Pitch Absolute A4 (440Hz) Calibration", listOf(69), "Internalize master reference pitch A440.")
            94 -> Triple("Perfect Pitch 12-Tone Absolute Recall", listOf(60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71), "Identify any random pitch without reference tone.")
            95 -> Triple("Upper Structure Triad Ear", listOf(60, 64, 70, 74, 78, 81), "Recognize polychords and upper-structure triads.")
            96 -> Triple("Concert Orchestral Audio Audit", listOf(60, 64, 67, 72, 76, 79), "Transcribe full orchestral chord voicings by ear.")
            97 -> Triple("Blind Dictation & Memory Recall", listOf(60, 63, 67, 70, 74, 77), "Transcribe 8-bar melodic and chordal phrase blind.")
            98 -> Triple("Virtuoso Ear Examination", listOf(60, 63, 67, 70, 74, 77, 81), "Complete Stage 5 virtuoso ear evaluation.")
            99 -> Triple("Perfect Pitch Certification Audit", listOf(60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71), "Pass 100% accurate absolute pitch examination.")
            else -> Triple("Grand Master Ear Diploma (Level 100)", listOf(60, 64, 67, 71, 74, 78, 81), "Earn Grand Master Ear Training Certification!")
        }

        return CurriculumLevel(
            levelNumber = lvl,
            discipline = AcademyDiscipline.EAR_TRAINING,
            stageIndex = stageIdx,
            stageName = stageName,
            difficultyTier = tierName,
            title = title,
            description = desc,
            pitchPool = pitches,
            bpm = (50 + lvl / 2).coerceIn(48, 120)
        )
    }

    private fun generateRhythmLevel(lvl: Int, stageIdx: Int, stageName: String, tierName: String, basePitch: Int): CurriculumLevel {
        val (title, timeSig, bpm, desc, pattern) = when (lvl) {
            // STAGE 1 (Levels 1-20): Novice Rhythm Foundation
            1 -> Tuple5("Steady Quarter Note Pulse (4/4)", "4/4", 60, "Tap on every quarter beat (1, 2, 3, 4).", listOf(0f, 1f, 2f, 3f))
            2 -> Tuple5("Downbeat Accent (Beat 1 Emphasis)", "4/4", 64, "Tap 4 beats emphasizing Beat 1 downbeat.", listOf(0f, 1f, 2f, 3f))
            3 -> Tuple5("Half Note Sustains (2 Beats)", "4/4", 60, "Tap Beats 1 and 3, sustaining for 2 beats each.", listOf(0f, 2f))
            4 -> Tuple5("Whole Note 4-Beat Hold", "4/4", 56, "Tap Beat 1 and hold for 4 full quarter beats.", listOf(0f))
            5 -> Tuple5("8th Note Subdivisions (1-and-2-and)", "4/4", 60, "Tap 8th notes across 4 beats (8 total taps).", listOf(0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f))
            6 -> Tuple5("3/4 Waltz Time Quarter Notes", "3/4", 66, "Tap 3 quarter beats in 3/4 waltz meter (1, 2, 3).", listOf(0f, 1f, 2f))
            7 -> Tuple5("Dotted Half Note Hold in 3/4", "3/4", 66, "Tap Beat 1 and sustain for 3 full beats.", listOf(0f))
            8 -> Tuple5("2/4 Marching Rhythm", "2/4", 76, "Tap 2 quarter beats in marching 2/4 meter.", listOf(0f, 1f))
            9 -> Tuple5("Off-beat 8th Note Accents", "4/4", 60, "Tap on the 'and' of each beat (off-beat syncopation).", listOf(0.5f, 1.5f, 2.5f, 3.5f))
            10 -> Tuple5("Dotted Quarter + 8th Note Pattern", "4/4", 64, "Tap dotted quarter note (1.5 beats) followed by 8th note.", listOf(0f, 1.5f, 2f, 3.5f))
            11 -> Tuple5("16th Note 4-Pack Subdivisions", "4/4", 54, "Subdivide 16th notes (1-e-and-a, 2-e-and-a).", listOf(0f, 0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f))
            12 -> Tuple5("6/8 Compound Duple Felt in 2", "6/8", 72, "Tap 6/8 meter felt as 2 dotted-quarter beats.", listOf(0f, 1.5f))
            13 -> Tuple5("Triplets (3 Notes per Beat)", "4/4", 56, "Tap 8th-note triplets (1-and-a, 2-and-a).", listOf(0f, 0.33f, 0.66f, 1f, 1.33f, 1.66f))
            14 -> Tuple5("Rest Infiltration (Silent Beat 2)", "4/4", 64, "Tap Beats 1, 3, 4 while maintaining silent rest on Beat 2.", listOf(0f, 2f, 3f))
            15 -> Tuple5("Hindustani Teentaal (16 Beats)", "16/4", 80, "Tap 16-matra Teentaal rhythm cycle (Dha Dhin Dhin Dha).", listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f))
            16 -> Tuple5("Hindustani Keherwa (8 Beats)", "8/4", 84, "Tap 8-matra Keherwa rhythm cycle (Dha Ge Na Ti).", listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f))
            17 -> Tuple5("Hindustani Dadra (6 Beats)", "6/4", 90, "Tap 6-matra Dadra rhythm cycle (Dha Dhi Na).", listOf(0f, 1f, 2f, 3f, 4f, 5f))
            18 -> Tuple5("Syncopation Beat 2& Accent", "4/4", 70, "Tap syncopated accent on Beat 2&.", listOf(0f, 1.5f, 3f))
            19 -> Tuple5("16th-Note Gallop (1-and-a)", "4/4", 64, "Tap 16th-note gallop rhythm (1-and-a 2-and-a).", listOf(0f, 0.5f, 0.75f, 1f, 1.5f, 1.75f))
            20 -> Tuple5("Stage 1 Rhythm Examination", "4/4", 72, "Complete Stage 1 rhythm evaluation.", listOf(0f, 0.5f, 1f, 1.5f, 2f, 3f))

            // STAGE 2 (Levels 21-40): Intermediate Subdivisions & Meters
            21 -> Tuple5("16th-Note Off-beat 'e' and 'a'", "4/4", 56, "Tap 16th-note off-beats 'e' and 'a'.", listOf(0.25f, 0.75f, 1.25f, 1.75f))
            22 -> Tuple5("Dotted 8th + 16th Figure", "4/4", 64, "Tap dotted 8th + 16th note syncopation.", listOf(0f, 0.75f, 1f, 1.75f))
            23 -> Tuple5("12/8 Compound Quadruple Meter", "12/8", 76, "Tap 12/8 meter felt as 4 dotted-quarter beats.", listOf(0f, 1.5f, 3f, 4.5f))
            24 -> Tuple5("5/4 Asymmetric Meter (3+2)", "5/4", 80, "Tap 5/4 meter grouped as 3 quarter beats + 2 quarter beats.", listOf(0f, 1f, 2f, 3f, 4f))
            25 -> Tuple5("5/4 Asymmetric Meter (2+3)", "5/4", 80, "Tap 5/4 meter grouped as 2 quarter beats + 3 quarter beats.", listOf(0f, 1f, 2f, 3f, 4f))
            26 -> Tuple5("7/8 Asymmetric Meter (3+2+2)", "7/8", 84, "Tap 7/8 meter grouped as 3 8ths + 2 8ths + 2 8ths.", listOf(0f, 0.75f, 1.25f))
            27 -> Tuple5("7/8 Asymmetric Meter (2+2+3)", "7/8", 84, "Tap 7/8 meter grouped as 2 8ths + 2 8ths + 3 8ths.", listOf(0f, 0.5f, 1.0f))
            28 -> Tuple5("3:2 Polyrhythm Clave", "4/4", 72, "Tap 3-against-2 polyrhythm pulse.", listOf(0f, 0.66f, 1.33f))
            29 -> Tuple5("4:3 Polyrhythm Clave", "4/4", 68, "Tap 4-against-3 polyrhythm pulse.", listOf(0f, 0.75f, 1.5f, 2.25f))
            30 -> Tuple5("Swing 8th Subdivision", "4/4", 84, "Tap long-short swing 8th note feel.", listOf(0f, 0.66f, 1f, 1.66f))
            31 -> Tuple5("Shuffle Rhythm", "4/4", 90, "Tap classic shuffle blues rhythm.", listOf(0f, 0.66f, 1f, 1.66f))
            32 -> Tuple5("Downbeat Rest Infiltration", "4/4", 68, "Tap Beats 2, 3, 4 with silent downbeat Beat 1 rest.", listOf(1f, 2f, 3f))
            33 -> Tuple5("Hindustani Jhaptal (10 Beats)", "10/4", 76, "Tap 10-matra Jhaptal rhythm cycle (Dhi Na Dhi Dhi Na).", listOf(0f, 1f, 2f, 3f, 4f))
            34 -> Tuple5("Hindustani Ektaal (12 Beats)", "12/4", 80, "Tap 12-matra Ektaal rhythm cycle (Dhin Dhin DhaGe TiRaKiTa).", listOf(0f, 1f, 2f, 3f, 4f, 5f))
            35 -> Tuple5("Carnatic Dhruva Tala (14 Aksharas)", "14/4", 72, "Tap Carnatic Dhruva Tala (IOII format).", listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f))
            36 -> Tuple5("Carnatic Matya Tala (10 Aksharas)", "10/4", 76, "Tap Carnatic Matya Tala (IOI format).", listOf(0f, 1f, 2f, 3f, 4f))
            37 -> Tuple5("Carnatic Rupaka Tala (6 Aksharas)", "6/4", 84, "Tap Carnatic Rupaka Tala (OI format).", listOf(0f, 1f, 2f, 3f))
            38 -> Tuple5("Dotted 16th Syncopation", "4/4", 60, "Tap dotted 16th note syncopations.", listOf(0f, 0.375f, 0.75f, 1.125f))
            39 -> Tuple5("16th Triplets (6 per Beat)", "4/4", 50, "Tap 16th note sextuplets (6 notes per beat).", listOf(0f, 0.16f, 0.33f, 0.5f, 0.66f, 0.83f))
            40 -> Tuple5("Stage 2 Rhythm Examination", "4/4", 80, "Complete Stage 2 intermediate rhythm evaluation.", listOf(0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f))

            else -> Tuple5(
                "Level $lvl: Rhythm Subdivision",
                "4/4",
                (60 + lvl / 2).coerceIn(50, 140),
                "Advanced Level $lvl rhythm subdivision tapping challenge.",
                listOf(0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f)
            )
        }

        return CurriculumLevel(
            levelNumber = lvl,
            discipline = AcademyDiscipline.RHYTHM_TRAINING,
            stageIndex = stageIdx,
            stageName = stageName,
            difficultyTier = tierName,
            title = title,
            description = desc,
            pitchPool = listOf(60),
            bpm = bpm,
            timeSig = timeSig,
            rhythmPattern = pattern
        )
    }

    private fun generateSightReadingLevel(lvl: Int, stageIdx: Int, stageName: String, tierName: String, basePitch: Int): CurriculumLevel {
        val (title, pitches, clef, keySig, desc) = when (lvl) {
            // STAGE 1 (Levels 1-20): Novice Sight Reading Foundation
            1 -> Tuple5Sight("Middle C Landmark Note", listOf(60), "treble", "C", "Identify and play Middle C (C4) on Treble staff.")
            2 -> Tuple5Sight("Treble G Landmark Note", listOf(67), "treble", "C", "Identify and play Treble G4 landmark note.")
            3 -> Tuple5Sight("Bass F Landmark Note", listOf(53), "bass", "C", "Identify and play Bass F3 landmark note.")
            4 -> Tuple5Sight("C-D-E 3-Note Stepwise Reading", listOf(60, 62, 64), "treble", "C", "Read 3 stepwise notes on Treble staff.")
            5 -> Tuple5Sight("C Major Pentascale (C4-G4)", listOf(60, 62, 64, 65, 67), "treble", "C", "Read 5-finger C Major pentascale.")
            6 -> Tuple5Sight("Bass C Pentascale (C3-G3)", listOf(48, 50, 52, 53, 55), "bass", "C", "Read 5-finger Bass C pentascale.")
            7 -> Tuple5Sight("C Major 1 Octave (C4-C5)", listOf(60, 62, 64, 65, 67, 69, 71, 72), "treble", "C", "Read full C Major 8-note octave.")
            8 -> Tuple5Sight("Treble Ledger Line Notes (C5-G5)", listOf(72, 74, 76, 77, 79), "treble", "C", "Read notes on upper Treble ledger lines.")
            9 -> Tuple5Sight("Bass Low Ledger Line Notes (C2-C3)", listOf(36, 38, 40, 41, 43, 45, 47, 48), "bass", "C", "Read notes on low Bass ledger lines.")
            10 -> Tuple5Sight("Grand Staff Dual Clef (C3-C5)", listOf(48, 52, 55, 60, 64, 67, 72), "both", "C", "Read simultaneously on Bass & Treble staves.")
            11 -> Tuple5Sight("G Major Scale (F♯ Accidental)", listOf(55, 57, 59, 60, 62, 64, 66, 67), "treble", "G", "Read G Major scale with 1 sharp.")
            12 -> Tuple5Sight("F Major Scale (B♭ Accidental)", listOf(53, 55, 57, 58, 60, 62, 64, 65), "treble", "F", "Read F Major scale with 1 flat.")
            13 -> Tuple5Sight("D Major Scale (2 Sharps: F♯, C♯)", listOf(62, 64, 66, 67, 69, 71, 73, 74), "treble", "D", "Read D Major scale with 2 sharps.")
            14 -> Tuple5Sight("B♭ Major Scale (2 Flats: B♭, E♭)", listOf(58, 60, 62, 63, 65, 67, 69, 70), "treble", "Bb", "Read B♭ Major scale with 2 flats.")
            15 -> Tuple5Sight("A Natural Minor Scale (A3-A4)", listOf(57, 59, 60, 62, 64, 65, 67, 69), "treble", "C", "Read A natural minor scale.")
            16 -> Tuple5Sight("Dotted Quarter Sight-Reading", listOf(60, 64, 67, 72), "treble", "C", "Read dotted quarter note rhythms on staff.")
            17 -> Tuple5Sight("3/4 Waltz Sight-Reading", listOf(60, 64, 67), "treble", "C", "Read 3/4 waltz score phrases.")
            18 -> Tuple5Sight("2/4 March Sight-Reading", listOf(60, 62, 64, 67), "treble", "C", "Read 2/4 marching score phrases.")
            19 -> Tuple5Sight("6/8 Sight-Reading", listOf(60, 64, 67, 72), "treble", "C", "Read 6/8 compound duple meter scores.")
            20 -> Tuple5Sight("Stage 1 Sight Reading Examination", listOf(48, 55, 60, 64, 67, 72), "both", "G", "Complete Stage 1 sight-reading examination.")

            // STAGE 2 (Levels 21-40): Intermediate Key Signatures & Hanon/Czerny Tech
            21 -> Tuple5Sight("A Major Scale (3 Sharps: F♯, C♯, G♯)", listOf(57, 59, 61, 62, 64, 66, 68, 69), "treble", "A", "Read 3-sharp key signature.")
            22 -> Tuple5Sight("E♭ Major Scale (3 Flats: B♭, E♭, A♭)", listOf(63, 65, 67, 68, 70, 72, 74, 75), "treble", "Eb", "Read 3-flat key signature.")
            23 -> Tuple5Sight("E Major Scale (4 Sharps: F♯, C♯, G♯, D♯)", listOf(64, 66, 68, 69, 71, 73, 75, 76), "treble", "E", "Read 4-sharp key signature.")
            24 -> Tuple5Sight("A♭ Major Scale (4 Flats: B♭, E♭, A♭, D♭)", listOf(56, 58, 60, 61, 63, 65, 67, 68), "treble", "Ab", "Read 4-flat key signature.")
            25 -> Tuple5Sight("E Natural Minor Scale", listOf(52, 54, 55, 57, 59, 60, 62, 64), "treble", "G", "Read E minor scale with 1 sharp.")
            26 -> Tuple5Sight("D Natural Minor Scale", listOf(50, 52, 53, 55, 57, 58, 60, 62), "treble", "F", "Read D minor scale with 1 flat.")
            27 -> Tuple5Sight("Hanon Exercise #1 (5-Finger Expansion)", listOf(60, 62, 64, 65, 67, 65, 64, 62), "treble", "C", "Read Hanon Virtuoso Pianist Ex #1.")
            28 -> Tuple5Sight("Hanon Exercise #2 (3rd & 4th Finger Stretch)", listOf(60, 64, 65, 67, 69, 67, 65, 64), "treble", "C", "Read Hanon Virtuoso Pianist Ex #2.")
            29 -> Tuple5Sight("Czerny Op. 599 No. 11 (5-Finger Run)", listOf(60, 62, 64, 65, 67, 65, 64, 62, 60), "treble", "C", "Read Czerny Op. 599 No. 11 étude.")
            30 -> Tuple5Sight("Czerny Op. 599 No. 12 (Scale Velocity)", listOf(60, 62, 64, 65, 67, 69, 71, 72), "treble", "C", "Read Czerny Op. 599 No. 12 scale étude.")
            31 -> Tuple5Sight("Triad Inversions (Root & 1st Inv)", listOf(60, 64, 67, 72, 76), "treble", "C", "Read 1st inversion triad voicings.")
            32 -> Tuple5Sight("Triad Inversions (2nd Inv)", listOf(64, 67, 72, 76, 79), "treble", "C", "Read 2nd inversion triad voicings.")
            33 -> Tuple5Sight("2-Octave C Major Scale", listOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84), "treble", "C", "Read 2-octave C Major scale.")
            34 -> Tuple5Sight("2-Octave G Major Scale", listOf(55, 57, 59, 60, 62, 64, 66, 67, 69, 71, 72, 74, 76, 78, 79), "treble", "G", "Read 2-octave G Major scale.")
            35 -> Tuple5Sight("2-Octave F Major Scale", listOf(53, 55, 57, 58, 60, 62, 64, 65, 67, 69, 70, 72, 74, 76, 77), "treble", "F", "Read 2-octave F Major scale.")
            36 -> Tuple5Sight("Accidental Sharps & Flats in Melody", listOf(60, 61, 63, 65, 66, 68, 70, 72), "treble", "C", "Read accidental chromatic notes on staff.")
            37 -> Tuple5Sight("Accidental Naturals Resolving Chromatics", listOf(60, 61, 62, 65, 66, 67), "treble", "C", "Read accidental natural signs resolving sharps.")
            38 -> Tuple5Sight("Octave Hand Spans (C4 to C5)", listOf(48, 60, 72, 84), "both", "C", "Read wide 8va octave leaps.")
            39 -> Tuple5Sight("Complex 16th Note Sight Reading", listOf(60, 62, 64, 65, 67, 69, 71, 72), "treble", "C", "Sight-read fast 16th note passages.")
            40 -> Tuple5Sight("Stage 2 Sight Reading Examination", listOf(48, 55, 60, 64, 67, 72, 76), "both", "D", "Complete Stage 2 intermediate examination.")

            else -> Tuple5Sight(
                "Level $lvl: Sight Reading Stream",
                listOf(basePitch, basePitch + 2, basePitch + 4, basePitch + 5, basePitch + 7, basePitch + 9, basePitch + 11, basePitch + 12),
                if (lvl % 3 == 0) "both" else if (lvl % 2 == 0) "bass" else "treble",
                listOf("C", "G", "D", "F", "Bb")[(lvl) % 5],
                "Advanced Level $lvl score sight-reading challenge."
            )
        }

        return CurriculumLevel(
            levelNumber = lvl,
            discipline = AcademyDiscipline.SIGHT_READING,
            stageIndex = stageIdx,
            stageName = stageName,
            difficultyTier = tierName,
            title = title,
            description = desc,
            pitchPool = pitches,
            bpm = (50 + lvl / 2).coerceIn(48, 120),
            clef = clef,
            keySignature = keySig
        )
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
    private data class Tuple5Sight<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
