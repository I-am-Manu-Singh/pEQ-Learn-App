package com.creativeblisslabs.peqlearn.app.midi

/**
 * COMPUTER & EXTERNAL KEYBOARD INPUT FALLBACK
 * Modeled after midee's ComputerKeyboardInput.ts (FL Studio 2-octave keyboard mapping).
 * 
 * Maps QWERTY keyboard character inputs to MIDI note numbers with octave transposition.
 */
object ComputerKeyboardInput {

    private const val DEFAULT_OCTAVE = 4

    // Map character/keys to semitone offset from current base octave C
    // Lower octave (Z, S, X, D, C, V, G, B, H, N, J, M)
    // Upper octave (Q, 2, W, 3, E, R, 5, T, 6, Y, 7, U, I, 9, O, 0, P)
    private val KEY_SEMITONE_MAP = mapOf(
        // Lower octave (Octave N)
        'z' to 0,   // C
        's' to 1,   // C#
        'x' to 2,   // D
        'd' to 3,   // D#
        'c' to 4,   // E
        'v' to 5,   // F
        'g' to 6,   // F#
        'b' to 7,   // G
        'h' to 8,   // G#
        'n' to 9,   // A
        'j' to 10,  // A#
        'm' to 11,  // B
        ',' to 12,  // High C

        // Upper octave (Octave N+1)
        'q' to 12,  // C
        '2' to 13,  // C#
        'w' to 14,  // D
        '3' to 15,  // D#
        'e' to 16,  // E
        'r' to 17,  // F
        '5' to 18,  // F#
        't' to 19,  // G
        '6' to 20,  // G#
        'y' to 21,  // A
        '7' to 22,  // A#
        'u' to 23,  // B
        'i' to 24,  // High C
        '9' to 25,  // High C#
        'o' to 26,  // High D
        '0' to 27,  // High D#
        'p' to 28   // High E
    )

    fun keyToMidiPitch(char: Char, octave: Int = DEFAULT_OCTAVE): Int? {
        val lower = char.lowercaseChar()
        val offset = KEY_SEMITONE_MAP[lower] ?: return null
        val basePitch = (octave + 1) * 12 // e.g. Octave 4 -> C4 = 60
        return basePitch + offset
    }
}

