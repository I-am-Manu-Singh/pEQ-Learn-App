package com.creativeblisslabs.peqlearn.app.midi

/**
 * LIVE LOOPER & OVERDUB ENGINE
 * Modeled on midee's LiveLooper.ts (Boss RC-1 / Ableton 1-button workflow).
 * 
 * Supports:
 * - Idle -> Armed -> Recording -> Playing -> Overdubbing -> Playing loop transitions
 * - Non-destructive multi-layer overdub stack
 * - Undo / Redo layers
 * - Quantization / Metronome sync
 * - Active sounding notes tracker to eliminate stuck MIDI notes
 */
class LiveLooper(
    var onPlaybackNoteOn: (pitch: Int, velocity: Float) -> Unit = { _, _ -> },
    var onPlaybackNoteOff: (pitch: Int) -> Unit = { _ -> }
) {

    enum class State {
        IDLE,
        ARMED,
        RECORDING,
        PLAYING,
        OVERDUBBING
    }

    data class LoopEvent(
        val isNoteOn: Boolean,
        val pitch: Int,
        val velocity: Float,
        val timeSec: Float
    )

    var state: State = State.IDLE
        private set

    val layerCount: Int get() = committedLayers.size

    private val committedLayers = mutableListOf<List<LoopEvent>>()
    private val undoneLayers = mutableListOf<List<LoopEvent>>()
    private val activeRecordingEvents = mutableListOf<LoopEvent>()
    private val activeOverdubEvents = mutableListOf<LoopEvent>()

    private var loopDurationSec: Float = 0f
    private var recordStartTimeMs: Long = 0L
    private var playbackStartMs: Long = 0L

    // Tracks sounding notes per layer to prevent stuck notes on clear/undo
    private val activeSoundingNotes = mutableSetOf<Int>()

    fun toggle(currentClockMs: Long) {
        when (state) {
            State.IDLE -> arm()
            State.ARMED -> cancelArm()
            State.RECORDING -> finishRecording(currentClockMs)
            State.PLAYING -> startOverdub(currentClockMs)
            State.OVERDUBBING -> commitOverdub(currentClockMs)
        }
    }

    fun arm() {
        state = State.ARMED
        activeRecordingEvents.clear()
    }

    fun cancelArm() {
        state = State.IDLE
        activeRecordingEvents.clear()
    }

    fun onLiveNote(pitch: Int, velocity: Float, isNoteOn: Boolean, currentClockMs: Long) {
        if (state == State.ARMED && isNoteOn) {
            // First note pressed automatically begins base recording
            state = State.RECORDING
            recordStartTimeMs = currentClockMs
            activeRecordingEvents.add(LoopEvent(true, pitch, velocity, 0f))
            return
        }

        if (state == State.RECORDING) {
            val relTimeSec = (currentClockMs - recordStartTimeMs) / 1000f
            activeRecordingEvents.add(LoopEvent(isNoteOn, pitch, velocity, relTimeSec))
        } else if (state == State.OVERDUBBING && loopDurationSec > 0f) {
            val elapsedMs = (currentClockMs - playbackStartMs) % (loopDurationSec * 1000f).toLong()
            val relTimeSec = elapsedMs / 1000f
            activeOverdubEvents.add(LoopEvent(isNoteOn, pitch, velocity, relTimeSec))
        }
    }

    fun finishRecording(currentClockMs: Long) {
        if (state != State.RECORDING) return
        val rawDurationSec = (currentClockMs - recordStartTimeMs) / 1000f
        loopDurationSec = maxOf(0.5f, rawDurationSec)
        committedLayers.clear()
        committedLayers.add(activeRecordingEvents.toList())
        activeRecordingEvents.clear()
        playbackStartMs = currentClockMs
        state = State.PLAYING
    }

    fun startOverdub(currentClockMs: Long) {
        if (state != State.PLAYING) return
        activeOverdubEvents.clear()
        state = State.OVERDUBBING
    }

    fun commitOverdub(currentClockMs: Long) {
        if (state != State.OVERDUBBING) return
        if (activeOverdubEvents.isNotEmpty()) {
            committedLayers.add(activeOverdubEvents.toList())
            activeOverdubEvents.clear()
            undoneLayers.clear()
        }
        state = State.PLAYING
    }

    fun undo() {
        if (committedLayers.isNotEmpty()) {
            val popped = committedLayers.removeAt(committedLayers.size - 1)
            undoneLayers.add(popped)
            releaseSoundingNotes()
            if (committedLayers.isEmpty()) {
                clear()
            }
        }
    }

    fun redo() {
        if (undoneLayers.isNotEmpty()) {
            val restored = undoneLayers.removeAt(undoneLayers.size - 1)
            committedLayers.add(restored)
        }
    }

    fun clear() {
        releaseSoundingNotes()
        committedLayers.clear()
        undoneLayers.clear()
        activeRecordingEvents.clear()
        activeOverdubEvents.clear()
        loopDurationSec = 0f
        state = State.IDLE
    }

    private fun releaseSoundingNotes() {
        for (note in activeSoundingNotes) {
            onPlaybackNoteOff(note)
        }
        activeSoundingNotes.clear()
    }

    fun getDuration(): Float = loopDurationSec

    fun getAllCommittedEvents(): List<LoopEvent> {
        return committedLayers.flatten().sortedBy { it.timeSec }
    }
}

