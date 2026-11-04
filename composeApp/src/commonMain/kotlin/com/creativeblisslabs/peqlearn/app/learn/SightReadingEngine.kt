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

    fun getTier(id: String): TierConfig {
        return TIERS.find { it.id == id } ?: TIERS[0]
    }

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

