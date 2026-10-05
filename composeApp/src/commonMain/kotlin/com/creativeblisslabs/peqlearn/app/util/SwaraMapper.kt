package com.creativeblisslabs.peqlearn.app.util

import kotlin.math.pow
import kotlin.math.ln

object SwaraMapper {
    // Standard chromatic semitones from Sa (C = 0)
    private val SWARA_NAMES_HINDUSTANI = listOf(
        "Sa", "re (komal)", "Re (shuddha)", "ga (komal)", "Ga (shuddha)",
        "ma (shuddha)", "Ma (teevra)", "Pa", "dha (komal)", "Dha (shuddha)",
        "ni (komal)", "Ni (shuddha)"
    )

    private val SWARA_NAMES_CARNATIC = listOf(
        "S", "R1", "R2 / G1", "R3 / G2", "G3",
        "M1", "M2", "P", "D1", "D2 / N1", "D3 / N2", "N3"
    )

    fun getHindustaniSwara(semitoneFromRoot: Int): String {
        return SWARA_NAMES_HINDUSTANI[(semitoneFromRoot % 12 + 12) % 12]
    }

    fun getCarnaticSwara(semitoneFromRoot: Int): String {
        return SWARA_NAMES_CARNATIC[(semitoneFromRoot % 12 + 12) % 12]
    }

    fun midiToFrequency(midiNote: Int, concertPitchA4: Double = 440.0): Double {
        return concertPitchA4 * 2.0.pow((midiNote - 69) / 12.0)
    }

    fun frequencyToMidi(freq: Double, concertPitchA4: Double = 440.0): Double {
        return 69.0 + 12.0 * (kotlin.math.ln(freq / concertPitchA4) / kotlin.math.ln(2.0))
    }

    fun sargamToSemitones(sargamList: List<String>, system: String = "Hindustani"): List<Int> {
        val mapping = mapOf(
            "S" to 0, "Sa" to 0,
            "r" to 1, "re" to 1, "R1" to 1,
            "R" to 2, "Re" to 2, "R2" to 2, "G1" to 2,
            "g" to 3, "ga" to 3, "R3" to 3, "G2" to 3,
            "G" to 4, "Ga" to 4, "G3" to 4,
            "m" to 5, "ma" to 5, "M1" to 5,
            "M" to 6, "Ma" to 6, "M2" to 6,
            "P" to 7, "Pa" to 7,
            "d" to 8, "dha" to 8, "D1" to 8,
            "D" to 9, "Dha" to 9, "D2" to 9, "N1" to 9,
            "n" to 10, "ni" to 10, "D3" to 10, "N2" to 10,
            "N" to 11, "Ni" to 11, "N3" to 11,
            "S'" to 12, "Sa'" to 12
        )
        return sargamList.map { mapping[it.trim()] ?: 0 }
    }

}
