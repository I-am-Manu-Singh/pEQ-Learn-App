package com.creativeblisslabs.peqlearn.app.ui.screens

enum class Accidental(val symbol: String, val semitoneShift: Int) {
    NATURAL("", 0),
    SHARP("♯", 1),
    FLAT("♭", -1),
    DOUBLE_SHARP("𝄪", 2),
    DOUBLE_FLAT("𝄫", -2)
}
