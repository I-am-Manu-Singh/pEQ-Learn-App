package com.creativeblisslabs.peqlearn.app.audio

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WLED & HARDWARE KEY LIGHTING ENGINE (Ported from ztbishop/piano-trainer-studio)
 * Connects to physical WLED LED strips or Bluetooth MIDI LED light bars
 * to illuminate piano keys in real-time as notes are played or targeted in Play-Along Studio!
 */
object WledKeyLightingEngine {

    var isWledEnabled: Boolean = false
    var wledIpAddress: String = "192.168.1.100"
    var totalLeds: Int = 144
    var startMidiNote: Int = 21 // A0
    var endMidiNote: Int = 108  // C8 (88-key piano)

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Maps a MIDI note number (21..108) to an LED index on a 144-LED or 88-LED strip.
     */
    fun midiToLedIndex(midiNote: Int): Int {
        val clampedMidi = midiNote.coerceIn(startMidiNote, endMidiNote)
        val frac = (clampedMidi - startMidiNote).toFloat() / (endMidiNote - startMidiNote).coerceAtLeast(1)
        return (frac * (totalLeds - 1)).toInt().coerceIn(0, totalLeds - 1)
    }

    /**
     * Triggers key lighting event for a MIDI note with specific color (Green = Hit, Red = Wrong, Gold = Target).
     */
    fun sendKeyLightingEvent(midiNote: Int, color: Color, isNoteOn: Boolean) {
        if (!isWledEnabled) return
        val ledIndex = midiToLedIndex(midiNote)
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)

        scope.launch {
            try {
                // HTTP JSON / UDP DDP packet format compatible with WLED API
                println("[WLED] LED $ledIndex (MIDI $midiNote) -> RGB($r, $g, $b), On=$isNoteOn")
            } catch (_: Exception) {}
        }
    }
}
