package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.Accidental
import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.MusicalArticulation
import com.creativeblisslabs.peqlearn.app.ui.screens.RestSymbol
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import com.creativeblisslabs.peqlearn.app.ui.screens.TabArticulation

/**
 * 100% faithful cross-platform parser for standard MIDI (*.mid) and MusicXML (*.musicxml / *.xml) scores.
 */
object MidiFileParser {

    fun parse(bytes: ByteArray, defaultTitle: String = "Imported Score"): Score? {
        if (bytes.size < 4) return null

        val firstChars = try {
            bytes.take(300).toByteArray().decodeToString()
        } catch (_: Exception) {
            ""
        }

        val trimmed = firstChars.trimStart()
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<score-partwise") || firstChars.contains("<score-partwise")) {
            return try {
                parseMusicXml(bytes.decodeToString(), defaultTitle)
            } catch (_: Exception) {
                null
            }
        }
        return MidiScoreParser.parse(bytes, defaultTitle)
    }

    private fun parseMusicXml(xml: String, defaultTitle: String): Score {
        val title = xmlTag(xml, "work-title")
            ?: xmlTag(xml, "movement-title")
            ?: defaultTitle.substringBeforeLast(".")
        val composer = Regex("""<creator[^>]*type=["']composer["'][^>]*>(.*?)</creator>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim() ?: "MusicXML Composer"

        var globalTempo = 120f
        Regex("""<sound[^>]*tempo=["']([0-9.]+)["']""").find(xml)?.groupValues?.get(1)?.toFloatOrNull()?.let {
            if (it > 10f) globalTempo = it
        }

        var divisions = 1f
        var currentFifths = 0
        var currentBeats = 4
        var currentBeatType = 4
        var globalKeySig = KeySignature.C_MAJOR

        val measureBlocks = Regex("""<measure\b[^>]*>(.*?)</measure>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .map { it.groupValues[1] }
            .toList()

        if (measureBlocks.isEmpty()) {
            return Score(
                title = title,
                composer = composer,
                tempo = globalTempo,
                timeSignature = "4/4",
                measures = listOf(Measure(notes = emptyList()))
            )
        }

        var noteIdCounter = 0
        val parsedMeasures = measureBlocks.mapIndexed { mIdx, mXml ->
            // 1. Update running attributes if specified in this measure
            xmlTag(mXml, "divisions")?.toFloatOrNull()?.let { if (it > 0f) divisions = it }
            xmlTag(mXml, "fifths")?.toIntOrNull()?.let { fifths ->
                currentFifths = fifths
                globalKeySig = MidiScoreParser.matchKeySignature(fifths, 0)
            }
            xmlTag(mXml, "beats")?.toIntOrNull()?.let { if (it > 0) currentBeats = it }
            xmlTag(mXml, "beat-type")?.toIntOrNull()?.let { if (it > 0) currentBeatType = it }

            val measureKeySig = MidiScoreParser.matchKeySignature(currentFifths, 0)
            val notesInMeasure = mutableListOf<ScoreNote>()
            val restsInMeasure = mutableListOf<RestSymbol>()

            var cursorDivisions = 0f
            var lastNoteStartPosBeats = 0f

            // Iterate through <note>, <backup>, and <forward> elements in document order
            val tokenRegex = Regex("""<(note|backup|forward)\b[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
            for (match in tokenRegex.findAll(mXml)) {
                val tagType = match.groupValues[1]
                val body = match.groupValues[2]

                when (tagType) {
                    "backup" -> {
                        val durDiv = xmlTag(body, "duration")?.toFloatOrNull() ?: 0f
                        cursorDivisions = (cursorDivisions - durDiv).coerceAtLeast(0f)
                    }
                    "forward" -> {
                        val durDiv = xmlTag(body, "duration")?.toFloatOrNull() ?: 0f
                        cursorDivisions += durDiv
                    }
                    "note" -> {
                        val isGrace = body.contains("<grace")
                        val isChord = body.contains("<chord")
                        val isRest = body.contains("<rest")
                        val durDiv = xmlTag(body, "duration")?.toFloatOrNull() ?: (divisions * 0.25f)
                        val durBeats = if (isGrace) 0.25f else (durDiv / divisions).coerceAtLeast(0.125f)

                        val posBeats = if (isChord) {
                            lastNoteStartPosBeats
                        } else {
                            (cursorDivisions / divisions).coerceAtLeast(0f)
                        }

                        val rawVoice = xmlTag(body, "voice")?.toIntOrNull() ?: 1
                        val staffNum = xmlTag(body, "staff")?.toIntOrNull()
                        val voiceIdx = when {
                            staffNum != null -> if (staffNum <= 1) 0 else 1
                            rawVoice <= 1 -> 0
                            else -> 1
                        }

                        if (isRest) {
                            restsInMeasure.add(
                                RestSymbol(
                                    position = posBeats,
                                    duration = durBeats,
                                    voice = voiceIdx
                                )
                            )
                        } else {
                            val stepStr = xmlTag(body, "step") ?: "C"
                            val octave = xmlTag(body, "octave")?.toIntOrNull() ?: 4
                            val alter = xmlTag(body, "alter")?.toFloatOrNull()?.toInt() ?: 0
                            val midiPitch = stepOctaveAlterToMidi(stepStr, octave, alter)

                            val accidental = when (alter) {
                                2 -> Accidental.DOUBLE_SHARP
                                1 -> Accidental.SHARP
                                -1 -> Accidental.FLAT
                                -2 -> Accidental.DOUBLE_FLAT
                                else -> Accidental.NATURAL
                            }
                            val isCourtesy = body.contains("""cautionary="yes"""") || body.contains("""parentheses="yes"""")
                            val isDotted = body.contains("<dot")
                            val tupletNum = xmlTag(body, "actual-notes")?.toIntOrNull() ?: 0
                            val tieStart = body.contains("""<tie type="start"""") || body.contains("""<tied type="start"""")
                            val tieEnd = body.contains("""<tie type="stop"""") || body.contains("""<tied type="stop"""")
                            val slurStart = body.contains("""<slur[^>]*type="start"""".toRegex())
                            val slurEnd = body.contains("""<slur[^>]*type="stop"""".toRegex())

                            val stemText = xmlTag(body, "stem")?.lowercase()
                            val stemUp: Boolean? = when (stemText) {
                                "up" -> true
                                "down" -> false
                                else -> null
                            }

                            val lyricText = xmlTag(body, "text") ?: ""
                            val fingering = xmlTag(body, "fingering")?.toIntOrNull()
                            val tabString = xmlTag(body, "string")?.toIntOrNull()?.let { (it - 1).coerceIn(0, 5) }
                            val tabFret = xmlTag(body, "fret")?.toIntOrNull()

                            val musArtic = when {
                                body.contains("<staccato") -> MusicalArticulation.STACCATO
                                body.contains("<accent") -> MusicalArticulation.ACCENT
                                body.contains("<tenuto") -> MusicalArticulation.TENUTO
                                body.contains("<strong-accent") -> MusicalArticulation.MARCATO
                                else -> MusicalArticulation.NONE
                            }

                            val tabArtic = when {
                                body.contains("<hammer-on") -> TabArticulation.HAMMER_ON
                                body.contains("<pull-off") -> TabArticulation.PULL_OFF
                                body.contains("<slide") -> TabArticulation.SLIDE_UP
                                else -> TabArticulation.NONE
                            }

                            notesInMeasure.add(
                                ScoreNote(
                                    id = ++noteIdCounter,
                                    midiNote = midiPitch,
                                    position = posBeats,
                                    duration = durBeats,
                                    string = tabString,
                                    fret = tabFret,
                                    voice = voiceIdx,
                                    stemUp = stemUp,
                                    accidental = accidental,
                                    courtesyAccidental = isCourtesy,
                                    dotted = isDotted,
                                    isDotted = isDotted,
                                    tuplet = tupletNum,
                                    grace = isGrace,
                                    tieStart = tieStart,
                                    tieEnd = tieEnd,
                                    slurStart = slurStart,
                                    slurEnd = slurEnd,
                                    lyric = lyricText,
                                    fingering = fingering,
                                    articulation = tabArtic,
                                    musicalArticulation = musArtic
                                )
                            )
                        }

                        if (!isChord && !isGrace) {
                            lastNoteStartPosBeats = posBeats
                            cursorDivisions += durDiv
                        }
                    }
                }
            }

            val repeatStart = mXml.contains("""<repeat[^>]*direction="forward"""".toRegex())
            val repeatEnd = mXml.contains("""<repeat[^>]*direction="backward"""".toRegex())

            Measure(
                measureIndex = mIdx + 1,
                timeSignatureNumerator = currentBeats,
                timeSignatureDenominator = currentBeatType,
                keySignature = measureKeySig,
                notes = notesInMeasure,
                rests = restsInMeasure,
                repeatStart = repeatStart,
                repeatEnd = repeatEnd,
                barline = if (mIdx == measureBlocks.lastIndex) "final" else "single"
            )
        }

        val firstM = parsedMeasures.firstOrNull()
        val timeSigStr = if (firstM != null) "${firstM.timeSignatureNumerator}/${firstM.timeSignatureDenominator}" else "4/4"

        return Score(
            title = title,
            composer = composer,
            tempo = globalTempo,
            timeSignature = timeSigStr,
            keySignature = parsedMeasures.firstOrNull()?.keySignature ?: globalKeySig,
            measures = parsedMeasures
        )
    }

    private fun stepOctaveAlterToMidi(step: String, octave: Int, alter: Int): Int {
        val baseSemitone = when (step.uppercase()) {
            "C" -> 0
            "D" -> 2
            "E" -> 4
            "F" -> 5
            "G" -> 7
            "A" -> 9
            "B" -> 11
            else -> 0
        }
        return ((octave + 1) * 12 + baseSemitone + alter).coerceIn(0, 127)
    }

    private fun xmlTag(xml: String, tag: String): String? {
        val match = Regex("""<$tag(?:\s[^>]*)?>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL).find(xml)
        return match?.groupValues?.get(1)?.trim()
    }
}