package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Raaga(
    val name: String,
    val thaat: String,
    val system: String = "Hindustani",
    val aroha: String = "",
    val avaroha: String = "",
    val vadi: String = "",
    val samvadi: String = "",
    val time: String = "",
    val mood: String = "",
    val semitones: List<Int> = emptyList(),
    val description: String = "",
    val melakarta: Int? = null,
    val chakra: String? = null,
    val pakad: String = "",
    val chalan: String = "",
    val jati: String = "",
    val alaap: List<String> = emptyList(),
    val bandish: String = "",
    val sthai: String = "",
    val antara: String = "",
    val taal: String = "",
    val bolTaal: String = "",
    val taansPaltas: List<String> = emptyList(),
    val jhala: String = "",
    val streamUrl: String? = null,
    val accentColor: Int? = null,
    val videoUrl: String? = null,
    val sthaiNotation: String = "",
    val antaraNotation: String = "",
    val bandishNotation: String = "",
    val sthaiMeaning: String = "",
    val antaraMeaning: String = "",
    val bandishMeaning: String = "",
    val sthaiTransliteration: String = "",
    val antaraTransliteration: String = "",
    val bandishTransliteration: String = "",
    // Extended fields verified from Saraswati & Classical Music Theory treatises:
    val sthayiAlaap: List<String> = emptyList(),
    val antaraAlaap: List<String> = emptyList(),
    val sargamSthayi: List<String> = emptyList(),
    val sargamAntara: List<String> = emptyList(),
    val taaneSthayi: List<String> = emptyList(),
    val taaneAntara: List<String> = emptyList(),
    val geetTaal: String = ""
) {
    // Backward-compatible computed accessors
    val meaning: String get() = bandishMeaning.ifEmpty { sthaiMeaning }
    val transliteration: String get() = bandishTransliteration.ifEmpty { sthaiTransliteration }
}

