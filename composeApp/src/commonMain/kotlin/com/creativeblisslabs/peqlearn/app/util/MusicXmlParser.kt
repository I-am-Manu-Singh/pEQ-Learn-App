package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.*

/**
 * Native, cross-platform MusicXML 3.1 & 4.0 parser for Score models.
 * Parses:
 * - Parts, measures, divisions, tempo directions
 * - Pitches (step, alter, octave) converted to MIDI note numbers
 * - Rests, durations, voices, stems
 * - Accidentals (sharp, flat, natural, double-sharp, double-flat)
 * - Articulations (staccato, accent, tenuto, fermata)
 * - Tablature string & fret numbers
 * - Dynamics (p, mp, mf, f, etc.)
 * - Lyrics & syllables
 * - Measure barlines, repeats, segnos, codas
 */
object MusicXmlParser {

    fun parse(xmlString: String, defaultTitle: String = "Imported MusicXML"): Score? {
        if (!xmlString.contains("<score-partwise", ignoreCase = true) &&
            !xmlString.contains("<score-timewise", ignoreCase = true)
        ) {
            return null
        }
        return runCatching {
            MidiFileParser.parseMusicXml(xmlString, defaultTitle)
        }.getOrNull()
    }

    private fun stepAlterOctaveToMidi(step: String, alter: Int, octave: Int): Int {
        val basePitch = when (step.uppercase()) {
            "C" -> 0
            "D" -> 2
            "E" -> 4
            "F" -> 5
            "G" -> 7
            "A" -> 9
            "B" -> 11
            else -> 0
        }
        return ((octave + 1) * 12) + basePitch + alter
    }

    private fun matchKeySignature(fifths: Int, mode: String): KeySignature {
        return when (fifths) {
            0 -> KeySignature.C_MAJOR
            1 -> KeySignature.G_MAJOR
            2 -> KeySignature.D_MAJOR
            3 -> KeySignature.A_MAJOR
            4 -> KeySignature.E_MAJOR
            5 -> KeySignature.B_MAJOR
            6, 7 -> KeySignature.F_SHARP_MAJOR
            -1 -> KeySignature.F_MAJOR
            -2 -> KeySignature.B_FLAT_MAJOR
            -3 -> KeySignature.E_FLAT_MAJOR
            -4 -> KeySignature.A_FLAT_MAJOR
            -5 -> KeySignature.D_FLAT_MAJOR
            -6, -7 -> KeySignature.G_FLAT_MAJOR
            else -> KeySignature.C_MAJOR
        }
    }

    private fun extractTagValue(xml: String, tag: String, attrFilter: String = ""): String {
        val pattern = if (attrFilter.isNotBlank()) {
            "<$tag\\b[^>]*$attrFilter[^>]*>(.*?)</$tag>"
        } else {
            "<$tag\\b[^>]*>(.*?)</$tag>"
        }
        val match = Regex(pattern, RegexOption.DOT_MATCHES_ALL).find(xml)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
}
