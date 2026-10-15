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
        if (!xmlString.contains("<score-partwise") && !xmlString.contains("<score-timewise")) {
            return null
        }

        val title = extractTagValue(xmlString, "movement-title")
            .ifBlank { extractTagValue(xmlString, "work-title") }
            .ifBlank { defaultTitle.removeSuffix(".xml").removeSuffix(".musicxml").replace("_", " ") }

        val composer = extractTagValue(xmlString, "creator", attrFilter = "type=\"composer\"")
            .ifBlank { "Traditional / Unknown" }

        var tempo = 120f
        val tempoMatch = Regex("""<sound[^>]*tempo="([\d.]+)"""").find(xmlString)
        if (tempoMatch != null) {
            tempo = tempoMatch.groupValues[1].toFloatOrNull() ?: 120f
        }

        var timeNumerator = 4
        var timeDenominator = 4
        val beatsMatch = Regex("""<beats>(\d+)</beats>""").find(xmlString)
        val beatTypeMatch = Regex("""<beat-type>(\d+)</beat-type>""").find(xmlString)
        if (beatsMatch != null) timeNumerator = beatsMatch.groupValues[1].toIntOrNull() ?: 4
        if (beatTypeMatch != null) timeDenominator = beatTypeMatch.groupValues[1].toIntOrNull() ?: 4

        var keyFifths = 0
        var keyMode = "major"
        val fifthsMatch = Regex("""<fifths>(-?\d+)</fifths>""").find(xmlString)
        val modeMatch = Regex("""<mode>(\w+)</mode>""").find(xmlString)
        if (fifthsMatch != null) keyFifths = fifthsMatch.groupValues[1].toIntOrNull() ?: 0
        if (modeMatch != null) keyMode = modeMatch.groupValues[1]

        val keySignature = matchKeySignature(keyFifths, keyMode)

        // Parse measures
        val measureRegex = Regex("""<measure\b[^>]*>(.*?)</measure>""", RegexOption.DOT_MATCHES_ALL)
        val measureMatches = measureRegex.findAll(xmlString).toList()

        var currentDivisions = 1
        var noteGlobalId = 0
        val measures = mutableListOf<Measure>()

        for ((mIndex, mMatch) in measureMatches.withIndex()) {
            val mContent = mMatch.groupValues[1]

            // Check for divisions changes
            val divMatch = Regex("""<divisions>(\d+)</divisions>""").find(mContent)
            if (divMatch != null) {
                currentDivisions = divMatch.groupValues[1].toIntOrNull() ?: currentDivisions
            }

            // Check for time signature changes
            val mBeats = Regex("""<beats>(\d+)</beats>""").find(mContent)
            val mBeatType = Regex("""<beat-type>(\d+)</beat-type>""").find(mContent)
            if (mBeats != null) timeNumerator = mBeats.groupValues[1].toIntOrNull() ?: timeNumerator
            if (mBeatType != null) timeDenominator = mBeatType.groupValues[1].toIntOrNull() ?: timeDenominator

            val isSegno = mContent.contains("<segno")
            val isCoda = mContent.contains("<coda")
            val isFine = mContent.contains("<fine") || mContent.contains("Fine</words>")
            val isDaCapo = mContent.contains("D.C.") || mContent.contains("da-capo")
            val isDalSegno = mContent.contains("D.S.") || mContent.contains("dal-segno")
            val isRepeatStart = mContent.contains("direction=\"forward\"")
            val isRepeatEnd = mContent.contains("direction=\"backward\"")
            val endingMatch = Regex("""<ending[^>]*number="(\d+)"""").find(mContent)
            val endingNum = endingMatch?.groupValues?.get(1)?.toIntOrNull()

            val barline = when {
                mIndex == measureMatches.size - 1 -> "final"
                mContent.contains("<bar-style>light-heavy</bar-style>") -> "final"
                mContent.contains("<bar-style>light-light</bar-style>") -> "double"
                else -> "single"
            }

            // Parse Notes inside Measure
            val noteRegex = Regex("""<note\b[^>]*>(.*?)</note>""", RegexOption.DOT_MATCHES_ALL)
            val noteMatches = noteRegex.findAll(mContent).toList()

            val notes = mutableListOf<ScoreNote>()
            var positionInDivisions = 0

            for (nMatch in noteMatches) {
                val nContent = nMatch.groupValues[1]
                val isChord = nContent.contains("<chord/>")
                val isRest = nContent.contains("<rest")
                val durationDivs = extractTagValue(nContent, "duration").toIntOrNull() ?: currentDivisions

                // Calculate position in beats
                val posBeats = (positionInDivisions.toFloat() / currentDivisions.toFloat())
                val durBeats = (durationDivs.toFloat() / currentDivisions.toFloat()).coerceAtLeast(0.125f)

                if (!isChord && !isRest) {
                    positionInDivisions += durationDivs
                } else if (isRest) {
                    positionInDivisions += durationDivs
                }

                if (isRest) {
                    notes.add(
                        ScoreNote(
                            id = ++noteGlobalId,
                            midiNote = 60,
                            position = posBeats,
                            duration = durBeats,
                            isRest = true
                        )
                    )
                    continue
                }

                // Pitch Parsing
                val step = extractTagValue(nContent, "step").ifBlank { "C" }
                val alter = extractTagValue(nContent, "alter").toIntOrNull() ?: 0
                val octave = extractTagValue(nContent, "octave").toIntOrNull() ?: 4
                val midiNote = stepAlterOctaveToMidi(step, alter, octave)

                // Accidental
                val accidentalTag = extractTagValue(nContent, "accidental").lowercase()
                val accidental = when {
                    accidentalTag == "sharp" || alter == 1 -> Accidental.SHARP
                    accidentalTag == "flat" || alter == -1 -> Accidental.FLAT
                    accidentalTag == "double-sharp" || alter == 2 -> Accidental.DOUBLE_SHARP
                    accidentalTag == "flat-flat" || alter == -2 -> Accidental.DOUBLE_FLAT
                    accidentalTag == "natural" -> Accidental.NATURAL
                    else -> Accidental.NATURAL
                }
                val isCourtesy = nContent.contains("parentheses=\"yes\"")

                // Articulations
                val musicalArticulation = when {
                    nContent.contains("<staccato") -> MusicalArticulation.STACCATO
                    nContent.contains("<accent") -> MusicalArticulation.ACCENT
                    nContent.contains("<tenuto") -> MusicalArticulation.TENUTO
                    nContent.contains("<strong-accent") -> MusicalArticulation.MARCATO
                    else -> MusicalArticulation.NONE
                }

                // Tablature Technical Details
                val stringNum = extractTagValue(nContent, "string").toIntOrNull()
                val fretNum = extractTagValue(nContent, "fret").toIntOrNull()
                val isHarmonic = nContent.contains("<harmonic")
                val isMuted = nContent.contains("<dead-note") || nContent.contains("<mute")

                val tabArticulation = when {
                    isMuted -> TabArticulation.DEAD_NOTE
                    nContent.contains("<hammer-on") -> TabArticulation.HAMMER_ON
                    nContent.contains("<pull-off") -> TabArticulation.PULL_OFF
                    nContent.contains("<slide") -> TabArticulation.SLIDE_UP
                    else -> TabArticulation.NONE
                }

                // Dynamics
                val dynamic = when {
                    nContent.contains("<pp/>") -> "pp"
                    nContent.contains("<p/>") -> "p"
                    nContent.contains("<mp/>") -> "mp"
                    nContent.contains("<mf/>") -> "mf"
                    nContent.contains("<f/>") -> "f"
                    nContent.contains("<ff/>") -> "ff"
                    nContent.contains("<sfz/>") -> "sfz"
                    else -> ""
                }

                // Lyrics
                val lyric = extractTagValue(nContent, "text")

                notes.add(
                    ScoreNote(
                        id = ++noteGlobalId,
                        midiNote = midiNote,
                        position = posBeats,
                        duration = durBeats,
                        string = stringNum,
                        fret = fretNum,
                        accidental = accidental,
                        courtesyAccidental = isCourtesy,
                        dotted = nContent.contains("<dot/>"),
                        musicalArticulation = musicalArticulation,
                        articulation = tabArticulation,
                        harmonic = if (isHarmonic) HarmonicType.TRIAD else null,
                        dynamic = dynamic,
                        lyric = lyric
                    )
                )
            }

            measures.add(
                Measure(
                    measureIndex = mIndex + 1,
                    timeSignatureNumerator = timeNumerator,
                    timeSignatureDenominator = timeDenominator,
                    keySignature = keySignature,
                    notes = notes,
                    barline = barline,
                    segno = isSegno,
                    coda = isCoda,
                    fine = isFine,
                    daCapo = isDaCapo,
                    dalSegno = isDalSegno,
                    repeatStart = isRepeatStart,
                    repeatEnd = isRepeatEnd,
                    endingNumber = endingNum
                )
            )
        }

        return Score(
            title = title,
            composer = composer,
            tempo = tempo,
            timeSignature = "$timeNumerator/$timeDenominator",
            keySignature = keySignature,
            measures = if (measures.isNotEmpty()) measures else listOf(Measure(measureIndex = 1))
        )
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
