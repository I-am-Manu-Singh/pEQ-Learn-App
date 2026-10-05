package com.creativeblisslabs.peqlearn.app.ui.screens

enum class KeySignature(val keyName: String, val accidentalsCount: Int, val isSharp: Boolean) {
    C_MAJOR("C Major / A Minor", 0, true),
    G_MAJOR("G Major / E Minor", 1, true),
    D_MAJOR("D Major / B Minor", 2, true),
    A_MAJOR("A Major / F# Minor", 3, true),
    E_MAJOR("E Major / C# Minor", 4, true),
    B_MAJOR("B Major / G# Minor", 5, true),
    F_SHARP_MAJOR("F# Major / D# Minor", 6, true),
    F_MAJOR("F Major / D Minor", 1, false),
    B_FLAT_MAJOR("Bb Major / G Minor", 2, false),
    E_FLAT_MAJOR("Eb Major / C Minor", 3, false),
    A_FLAT_MAJOR("Ab Major / F Minor", 4, false),
    D_FLAT_MAJOR("Db Major / Bb Minor", 5, false),
    G_FLAT_MAJOR("Gb Major / Eb Minor", 6, false)
}
