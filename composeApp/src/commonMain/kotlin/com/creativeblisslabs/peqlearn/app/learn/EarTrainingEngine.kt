package com.creativeblisslabs.peqlearn.app.learn

import kotlin.random.Random

/**
 * EAR TRAINING & INTERVAL ENGINE
 * Inspired by midee's interval ear-training pedagogy.
 * Provides canonical musical intervals, constrained root picking, 
 * question synthesis, and accuracy feedback.
 */
object EarTrainingEngine {

    data class Interval(
        val id: String,
        val shortName: String,
        val fullName: String,
        val semitones: Int,
        val category: IntervalCategory = IntervalCategory.BASIC
    )

    enum class IntervalCategory {
        BASIC,
        INTERMEDIATE,
        ADVANCED
    }

    val ALL_INTERVALS = listOf(
        Interval("P1", "P1", "Unison", 0, IntervalCategory.BASIC),
        Interval("m2", "m2", "Minor 2nd", 1, IntervalCategory.INTERMEDIATE),
        Interval("M2", "M2", "Major 2nd", 2, IntervalCategory.BASIC),
        Interval("m3", "m3", "Minor 3rd", 3, IntervalCategory.BASIC),
        Interval("M3", "M3", "Major 3rd", 4, IntervalCategory.BASIC),
        Interval("P4", "P4", "Perfect 4th", 5, IntervalCategory.BASIC),
        Interval("TT", "TT", "Tritone", 6, IntervalCategory.ADVANCED),
        Interval("P5", "P5", "Perfect 5th", 7, IntervalCategory.BASIC),
        Interval("m6", "m6", "Minor 6th", 8, IntervalCategory.INTERMEDIATE),
        Interval("M6", "M6", "Major 6th", 9, IntervalCategory.INTERMEDIATE),
        Interval("m7", "m7", "Minor 7th", 10, IntervalCategory.INTERMEDIATE),
        Interval("M7", "M7", "Major 7th", 11, IntervalCategory.ADVANCED),
        Interval("P8", "P8", "Octave", 12, IntervalCategory.BASIC)
    )

    data class IntervalMnemonic(
        val ascendingSong: String,
        val descendingSong: String
    )

    val INTERVAL_MNEMONICS = mapOf(
        "P1" to IntervalMnemonic("Unison Identity", "Unison Identity"),
        "m2" to IntervalMnemonic("Jaws Theme", "Für Elise (Opening)"),
        "M2" to IntervalMnemonic("Happy Birthday", "Mary Had a Little Lamb"),
        "m3" to IntervalMnemonic("Greensleeves", "Hey Jude"),
        "M3" to IntervalMnemonic("Oh When the Saints", "Swing Low, Sweet Chariot"),
        "P4" to IntervalMnemonic("Here Comes the Bride", "Eine kleine Nachtmusik"),
        "TT" to IntervalMnemonic("The Simpsons Theme", "Even Flow (Pearl Jam)"),
        "P5" to IntervalMnemonic("Star Wars Theme", "The Flintstones Theme"),
        "m6" to IntervalMnemonic("The Entertainer (3rd note)", "Love Story Theme"),
        "M6" to IntervalMnemonic("My Bonnie Lies Over the Ocean", "Nobody Knows the Trouble I've Seen"),
        "m7" to IntervalMnemonic("Somewhere (West Side Story)", "Watermelon Man"),
        "M7" to IntervalMnemonic("Take On Me (Chorus)", "I Love You (Cole Porter)"),
        "P8" to IntervalMnemonic("Somewhere Over the Rainbow", "Willow Weep for Me")
    )

    // High-contrast beginner set: Major 3rd (bright), Perfect 4th (open), Perfect 5th (stable), Octave (identity)
    val BEGINNER_SET = listOf("M3", "P4", "P5", "P8")
    val INTERMEDIATE_SET = listOf("M2", "m3", "M3", "P4", "P5", "m6", "M6", "P8")
    val ADVANCED_SET = ALL_INTERVALS.map { it.id }

    private const val ROOT_LOW = 48  // C3
    private const val ROOT_HIGH = 72 // C5

    fun pickRootPitch(semitones: Int, random: Random = Random.Default): Int {
        val max = maxOf(ROOT_LOW, ROOT_HIGH - semitones)
        val span = max - ROOT_LOW
        return if (span <= 0) ROOT_LOW else ROOT_LOW + random.nextInt(span + 1)
    }

    data class IntervalQuestion(
        val interval: Interval,
        val rootPitch: Int,
        val targetPitch: Int,
        val choices: List<Interval>
    )

    fun generateQuestion(
        tier: String = "Beginner",
        random: Random = Random.Default
    ): IntervalQuestion {
        val activePoolIds = when (tier.lowercase()) {
            "intermediate" -> INTERMEDIATE_SET
            "advanced" -> ADVANCED_SET
            else -> BEGINNER_SET
        }
        val pool = ALL_INTERVALS.filter { activePoolIds.contains(it.id) }
        val pickedInterval = pool.random(random)
        val root = pickRootPitch(pickedInterval.semitones, random)
        val target = root + pickedInterval.semitones

        // Build 4 distinct multiple-choice options including correct answer
        val wrongChoices = ALL_INTERVALS.filter { it.id != pickedInterval.id }.shuffled(random).take(3)
        val allChoices = (wrongChoices + pickedInterval).shuffled(random)

        return IntervalQuestion(
            interval = pickedInterval,
            rootPitch = root,
            targetPitch = target,
            choices = allChoices
        )
    }

    // --- RYANATKN/EARBETTER & TYPESCRIPT EAR TRAINING FEATURES ---

    val EARBETTER_REALMS = listOf(
        "Major Pentatonic", "Major Scale (Ionian)", "Minor Scale (Aeolian)",
        "Dorian Mode", "Phrygian Mode", "Lydian Mode", "Mixolydian Mode", "Locrian Mode", "Chromatic"
    )

    data class SolfegeSyllable(
        val syllable: String,
        val scaleDegree: String,
        val semitones: Int
    )

    val SOLFEGE_MAP = listOf(
        SolfegeSyllable("Do", "1", 0),
        SolfegeSyllable("Ra", "♭2", 1),
        SolfegeSyllable("Re", "2", 2),
        SolfegeSyllable("Me", "♭3", 3),
        SolfegeSyllable("Mi", "3", 4),
        SolfegeSyllable("Fa", "4", 5),
        SolfegeSyllable("Fi", "♯4", 6),
        SolfegeSyllable("Sol", "5", 7),
        SolfegeSyllable("Le", "♭6", 8),
        SolfegeSyllable("La", "6", 9),
        SolfegeSyllable("Te", "♭7", 10),
        SolfegeSyllable("Ti", "7", 11)
    )

    data class SargamSwara(
        val swaraName: String,
        val shortLabel: String,
        val semitones: Int
    )

    val SARGAM_MAP = listOf(
        SargamSwara("Shuddha Sa", "Sa", 0),
        SargamSwara("Komal Re", "re", 1),
        SargamSwara("Shuddha Re", "Re", 2),
        SargamSwara("Komal Ga", "ga", 3),
        SargamSwara("Shuddha Ga", "Ga", 4),
        SargamSwara("Shuddha Ma", "Ma", 5),
        SargamSwara("Tivra Ma", "Ma'", 6),
        SargamSwara("Shuddha Pa", "Pa", 7),
        SargamSwara("Komal Dha", "dha", 8),
        SargamSwara("Shuddha Dha", "Dha", 9),
        SargamSwara("Komal Ni", "ni", 10),
        SargamSwara("Shuddha Ni", "Ni", 11)
    )

    fun getChromaColorHue(midiNote: Int): Float {
        val pc = ((midiNote % 12) + 12) % 12
        return (pc / 12.0f)
    }

    /**
     * Generates a 2-note, 4-note, or 8-note pitch sequence trial for working memory ear training.
     */
    fun generateSequenceTrial(
        length: Int = 4,
        octaves: Int = 1,
        allowedSemitones: List<Int> = listOf(2, 4, 5, 7, 9, 11, 12),
        rootPitch: Int = 60
    ): List<Int> {
        val rangeSpan = 12 * octaves
        val result = mutableListOf(rootPitch)
        var current = rootPitch

        repeat(length - 1) {
            val step = allowedSemitones.random() * listOf(-1, 1).random()
            val nextP = (current + step).coerceIn(rootPitch - rangeSpan, rootPitch + rangeSpan)
            current = nextP
            result.add(current)
        }
        return result
    }
}

