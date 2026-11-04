package com.creativeblisslabs.peqlearn.app.learn

import kotlin.math.abs

/**
 * ARTICULATION & WAIT-MODE SCORING
 * Parallels midee's core/scoring.ts.
 * 
 * Provides deterministic precision windows for:
 * - Real-time follow grading (PERFECT, GOOD, EARLY, LATE, MISS)
 * - Chord arpeggiation window (80ms tolerance)
 * - Held-note grace periods (50ms)
 */
object PracticeScoringEngine {

    const val PERFECT_WINDOW_SEC = 0.050f  // +/- 50ms
    const val GOOD_WINDOW_SEC = 0.150f     // +/- 150ms
    const val LATE_HIT_WINDOW_SEC = 0.300f // Up to 300ms catch-up
    const val CHORD_ARPEGGIO_WINDOW_SEC = 0.080f // 80ms polyphonic tolerance
    const val HELD_GRACE_SEC = 0.050f      // 50ms sustain buffer

    enum class TimingVerdict {
        PERFECT,
        GOOD,
        EARLY,
        LATE,
        MISS
    }

    data class ScoreStats(
        val totalNotes: Int = 0,
        val perfectHits: Int = 0,
        val goodHits: Int = 0,
        val earlyHits: Int = 0,
        val lateHits: Int = 0,
        val misses: Int = 0,
        val currentStreak: Int = 0,
        val maxStreak: Int = 0
    ) {
        val totalHits: Int get() = perfectHits + goodHits + earlyHits + lateHits
        val accuracyPercent: Float get() = if (totalNotes > 0) (totalHits.toFloat() / totalNotes) * 100f else 0f
        val isCleanPass: Boolean get() = totalNotes > 10 && misses == 0 && accuracyPercent >= 95f
    }

    fun classifyTiming(
        actualTimeSec: Float,
        scheduledTimeSec: Float,
        perfectWindowSec: Float = PERFECT_WINDOW_SEC,
        goodWindowSec: Float = GOOD_WINDOW_SEC,
        lateWindowSec: Float = LATE_HIT_WINDOW_SEC
    ): TimingVerdict {
        val delta = actualTimeSec - scheduledTimeSec
        val absDelta = abs(delta)

        return when {
            absDelta <= perfectWindowSec -> TimingVerdict.PERFECT
            absDelta <= goodWindowSec -> TimingVerdict.GOOD
            delta < -goodWindowSec && absDelta <= lateWindowSec -> TimingVerdict.EARLY
            delta > goodWindowSec && delta <= lateWindowSec -> TimingVerdict.LATE
            else -> TimingVerdict.MISS
        }
    }
}

