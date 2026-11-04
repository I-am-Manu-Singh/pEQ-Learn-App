package com.creativeblisslabs.peqlearn.app.midi

/**
 * MIDI PEDAL & CC64 SUSTAIN RESOLVER
 * Ported from midee's core/midi/sustain.ts.
 * 
 * Re-strike cut algorithm:
 * When a note is sustained by pedal (CC64) and re-struck at the same pitch,
 * this cuts the tail of the previous ringing note so voices do not accumulate
 * or muddy the audio output.
 */
object MidiSustainResolver {

    data class PedalSpan(
        val startSec: Float,
        val endSec: Float
    )

    data class ResolvedNote(
        val pitch: Int,
        val startSec: Float,
        val notatedDurationSec: Float,
        val sustainedReleaseSec: Float,
        val velocity: Float
    )

    const val PEDAL_DOWN_THRESHOLD = 0.5f // CC >= 64
    const val MAX_SUSTAIN_EXTENSION_SEC = 15.0f

    /**
     * Converts a stream of raw CC64 events into disjoint PedalSpan intervals.
     */
    fun buildPedalSpans(
        ccEvents: List<Pair<Float, Float>>, // (timeSec, value 0.0..1.0)
        trackEndTimeSec: Float
    ): List<PedalSpan> {
        val sorted = ccEvents.sortedBy { it.first }
        val spans = mutableListOf<PedalSpan>()
        var downAt: Float? = null

        for ((time, value) in sorted) {
            if (value >= PEDAL_DOWN_THRESHOLD) {
                if (downAt == null) downAt = time
            } else if (downAt != null) {
                if (time > downAt) spans.add(PedalSpan(downAt, time))
                downAt = null
            }
        }
        if (downAt != null && trackEndTimeSec > downAt) {
            spans.add(PedalSpan(downAt, trackEndTimeSec))
        }
        return spans
    }

    /**
     * Applies pedal spans and re-strike cuts to a list of raw notes.
     */
    fun resolveSustain(
        notes: List<ResolvedNote>,
        pedalSpans: List<PedalSpan>
    ): List<ResolvedNote> {
        if (pedalSpans.isEmpty() || notes.isEmpty()) return notes

        val sortedNotes = notes.sortedBy { it.startSec }
        val outNotes = sortedNotes.toMutableList()

        // Map pitch to index in outNotes for re-strike truncation
        val lastRingingNoteIndex = mutableMapOf<Int, Int>()

        for (i in outNotes.indices) {
            val note = outNotes[i]
            val notatedEnd = note.startSec + note.notatedDurationSec

            // Check if previous note of the same pitch is still ringing and truncate it (re-strike cut)
            val prevIndex = lastRingingNoteIndex[note.pitch]
            if (prevIndex != null) {
                val prev = outNotes[prevIndex]
                if (prev.sustainedReleaseSec > note.startSec) {
                    outNotes[prevIndex] = prev.copy(sustainedReleaseSec = note.startSec)
                }
            }

            // Find pedal span covering notatedEnd
            val coveringSpan = pedalSpans.firstOrNull { it.startSec <= notatedEnd && it.endSec > notatedEnd }
            val releaseSec = if (coveringSpan != null) {
                minOf(coveringSpan.endSec, notatedEnd + MAX_SUSTAIN_EXTENSION_SEC)
            } else {
                notatedEnd
            }

            outNotes[i] = note.copy(sustainedReleaseSec = releaseSec)
            lastRingingNoteIndex[note.pitch] = i
        }

        return outNotes
    }
}

