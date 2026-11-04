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
}

