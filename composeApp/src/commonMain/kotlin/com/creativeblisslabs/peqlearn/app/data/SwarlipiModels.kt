package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable

/**
 * Representation of an individual Swara in a Matra (beat).
 * Follows classical Bhatkhande and Vishnu Digambar notation standards.
 *
 * @param swar The core note symbol: S, R, G, M, P, D, N (or s, r, g, m, p, d, n / Sa, Re, Ga...)
 * @param saptak Octave: MANDRA (lower, dot below), MADHYA (middle, normal), TAAR (upper, dot above)
 * @param variation Quality: SHUDDHA (natural), KOMAL (flat, line below in Bhatkhande), TEEVRA (sharp, vertical tick M' in Bhatkhande)
 * @param kan Optional grace note (Kan swar / sparsh swar) placed in superscript before/above the note
 * @param meendEnd Optional destination swar if this note is the start of a slurred glide (meend curve)
 * @param lyric Corresponding lyric syllable (geet uccharan) under this swar
 * @param isAvagraha True if this is an elongation/rest beat ("-", "S", "ऽ")
 */
@Serializable
enum class SwaraSaptak {
    MANDRA, // Dot below note (e.g. Ṇ, Ḍ, Ṗ)
    MADHYA, // Middle octave
    TAAR    // Dot above note (e.g. Ṡ, Ṙ, Ġ)
}

@Serializable
enum class SwaraVariation {
    SHUDDHA, // Natural
    KOMAL,   // Flat: horizontal underline below note (R̲, G̲, D̲, N̲)
    TEEVRA   // Sharp: vertical stroke / prime above M (M' or Ḿ)
}

@Serializable
data class SwarlipiNote(
    val swar: String,
    val saptak: SwaraSaptak = SwaraSaptak.MADHYA,
    val variation: SwaraVariation = SwaraVariation.SHUDDHA,
    val kan: String? = null,
    val meendEnd: String? = null,
    val lyric: String = "",
    val stroke: String = "", // Mizrab / Instrumental stroke: ";" (Da), "'" (Ra), "\" (Dir)
    val isAvagraha: Boolean = false,
    val isKhatka: Boolean = false
)

/**
 * Single Matra (beat) in a Taal Vibhag.
 * Can hold 1 swar (1 matra), 2 swars (1/2 matra each with bottom cup tie),
 * 3 swars (1/3 matra each), 4 swars (1/4 matra each), or 6 swars.
 */
@Serializable
data class SwarlipiMatra(
    val notes: List<SwarlipiNote> = emptyList(),
    val matraNumber: Int = 1,
    val taalMark: String = "", // "X" (Sam), "O" (Khali), "2", "3", "4" (Taali numbers)
    val bol: String = ""       // Accompanying tabla bol for this matra
)

/**
 * A Vibhag (measure / division) of a Taal cycle.
 * Contains a fixed count of matras (e.g., 4 matras in Teentaal, 2 in Jhaptal).
 */
@Serializable
data class SwarlipiVibhag(
    val matras: List<SwarlipiMatra> = emptyList(),
    val vibhagIndex: Int = 0
)

/**
 * An Avartan (one full rhythmic cycle of the Taal).
 */
@Serializable
data class SwarlipiAvartan(
    val vibhags: List<SwarlipiVibhag> = emptyList(),
    val avartanNumber: Int = 1
)

/**
 * Full Bandish Movement Notation (e.g., Sthai or Antara).
 */
@Serializable
data class SwarlipiMovement(
    val title: String, // "Sthai" or "Antara" or "Pallavi" or "Charanam"
    val avartans: List<SwarlipiAvartan> = emptyList(),
    val rawNotationText: String = ""
)

