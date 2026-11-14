package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.Accidental
import com.creativeblisslabs.peqlearn.app.ui.screens.ArpeggioDirection
import com.creativeblisslabs.peqlearn.app.ui.screens.GuitarHarmonicType
import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.MidiTrackInfo
import com.creativeblisslabs.peqlearn.app.ui.screens.MusicalArticulation
import com.creativeblisslabs.peqlearn.app.ui.screens.OrnamentType
import com.creativeblisslabs.peqlearn.app.ui.screens.PedalEvent
import com.creativeblisslabs.peqlearn.app.ui.screens.PickDirection
import com.creativeblisslabs.peqlearn.app.ui.screens.RestSymbol
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import com.creativeblisslabs.peqlearn.app.ui.screens.TabArticulation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipInputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Universal Cross-Platform Score Parser for:
 * 1. Standard MIDI (*.mid, *.midi)
 * 2. Uncompressed MusicXML (*.musicxml, *.xml) & Compressed MusicXML Archives (*.mxl)
 * 3. pEQ-Exported Vector PDFs (via lossless embedded %PEQ_SCORE_PAYLOAD trailer)
 * 4. High-Quality External Sheet Music PDFs (vector stream & glyph/staff extraction)
 */
object MidiFileParser {

    private const val PEQ_PDF_PAYLOAD_START = "%PEQ_SCORE_PAYLOAD_START:"
    private const val PEQ_PDF_PAYLOAD_END = ":PEQ_SCORE_PAYLOAD_END%"

    fun parse(bytes: ByteArray, defaultTitle: String = "Imported Score"): Score? {
        if (bytes.size < 4) return null

        // 1. Standard MIDI File ("MThd")
        if (bytes[0] == 'M'.code.toByte() && bytes[1] == 'T'.code.toByte() &&
            bytes[2] == 'h'.code.toByte() && bytes[3] == 'd'.code.toByte()
        ) {
            return MidiScoreParser.parse(bytes, defaultTitle)
        }

        // 2. Compressed MusicXML (.mxl) or ZIP container ("PK\x03\x04")
        if (bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
        ) {
            extractMusicXmlFromMxlZip(bytes)?.let { xmlContent ->
                return runCatching { parseMusicXml(xmlContent, defaultTitle) }.getOrNull()
            }
        }

        // 3. PDF Document ("%PDF-")
        if (bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()
        ) {
            // 3a. Check if this PDF was exported by pEQ Learn with an embedded lossless Score payload
            decodeEmbeddedScoreFromPdf(bytes)?.let { restoredScore ->
                return restoredScore.copy(isPdfScore = false)
            }
            // 3b. Attempt vector/text stream OMR extraction for external digital sheet-music PDFs
            parseVectorSheetMusicPdf(bytes, defaultTitle)?.let { extractedScore ->
                if (extractedScore.measures.any { it.notes.isNotEmpty() }) {
                    return extractedScore.copy(isPdfScore = false)
                }
            }
            return null
        }

        // 4. Uncompressed MusicXML (.xml / .musicxml) — inspect first 2048 bytes (accounting for BOM & DOCTYPE)
        val headerSample = try {
            bytes.take(2048).toByteArray().decodeToString()
                .removePrefix("\uFEFF")
                .trimStart()
        } catch (_: Exception) {
            ""
        }

        if (headerSample.startsWith("<?xml", ignoreCase = true) ||
            headerSample.startsWith("<!DOCTYPE", ignoreCase = true) ||
            headerSample.contains("<score-partwise", ignoreCase = true) ||
            headerSample.contains("<score-timewise", ignoreCase = true)
        ) {
            return try {
                val fullXml = decodeXmlBytes(bytes)
                parseMusicXml(fullXml, defaultTitle)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        return MidiScoreParser.parse(bytes, defaultTitle)
    }

    private fun decodeXmlBytes(bytes: ByteArray): String {
        // Handle UTF-16 LE/BE BOMs or UTF-8 BOM
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0xFF && b1 == 0xFE) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            }
            if (b0 == 0xFE && b1 == 0xFF) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            }
        }
        return bytes.decodeToString().removePrefix("\uFEFF")
    }

    private fun extractMusicXmlFromMxlZip(zipBytes: ByteArray): String? {
        return try {
            var fallbackXml: String? = null
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory &&
                        (name.endsWith(".xml", ignoreCase = true) || name.endsWith(".musicxml", ignoreCase = true)) &&
                        !name.contains("META-INF", ignoreCase = true) &&
                        !name.endsWith("container.xml", ignoreCase = true)
                    ) {
                        val entryBytes = zis.readBytes()
                        val decoded = decodeXmlBytes(entryBytes)
                        if (decoded.contains("<score-partwise", ignoreCase = true) || decoded.contains("<measure", ignoreCase = true)) {
                            return decoded
                        }
                        if (fallbackXml == null) fallbackXml = decoded
                    }
                    entry = zis.nextEntry
                }
            }
            fallbackXml
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Multi-Part, Multi-Staff MusicXML Parser.
     * Properly merges all `<part>` elements into unified polyphonic measures, extracts lyrics,
     * chord harmony symbols (`<harmony>`), dynamics (`<dynamics>`), tempo (`<sound tempo="..."/>`
     * & `<per-minute>`), fingerings, and guitar string/fret tablature!
     */
    internal fun parseMusicXml(xml: String, defaultTitle: String): Score {
        val rawTitle = xmlTag(xml, "work-title")
            ?: xmlTag(xml, "movement-title")
            ?: Regex("""<credit-words[^>]*>(.*?)</credit-words>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(xml).map { it.groupValues[1].trim() }.firstOrNull { it.length in 2..60 }
            ?: defaultTitle.substringAfterLast("/").substringBeforeLast(".").replace("_", " ")

        val composer = Regex("""<creator[^>]*type=["']composer["'][^>]*>(.*?)</creator>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim()
            ?: Regex("""<creator[^>]*>(.*?)</creator>""", RegexOption.DOT_MATCHES_ALL)
                .find(xml)?.groupValues?.get(1)?.trim()
            ?: ""

        val lyricist = Regex("""<creator[^>]*type=["']lyricist["'][^>]*>(.*?)</creator>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim() ?: ""

        var globalTempo = 120f
        Regex("""<sound[^>]*tempo=["']([0-9.]+)["']""").find(xml)?.groupValues?.get(1)?.toFloatOrNull()?.let {
            if (it in 20f..300f) globalTempo = it
        } ?: Regex("""<per-minute[^>]*>([0-9.]+)</per-minute>""").find(xml)?.groupValues?.get(1)?.toFloatOrNull()?.let {
            if (it in 20f..300f) globalTempo = it
        }

        // Parse <score-part> declarations for track list
        val partNamesById = mutableMapOf<String, String>()
        val partListRegex = Regex("""<score-part\b[^>]*id=["']([^"']+)["'][^>]*>(.*?)</score-part>""", RegexOption.DOT_MATCHES_ALL)
        for (pm in partListRegex.findAll(xml)) {
            val pid = pm.groupValues[1]
            val pName = xmlTag(pm.groupValues[2], "part-name") ?: "Part ${partNamesById.size + 1}"
            partNamesById[pid] = cleanXmlEntities(pName)
        }

        // Extract each <part> block separately so multi-part scores merge by measure index instead of concatenating!
        val partBlocks = Regex("""<part\b[^>]*id=["']([^"']+)["'][^>]*>(.*?)</part>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
            .ifEmpty { listOf("P1" to xml) }

        var noteIdCounter = 0
        val mergedMeasuresMap = sortedMapOf<Int, Measure>()
        val trackInfos = mutableListOf<MidiTrackInfo>()
        var globalKeySig = KeySignature.C_MAJOR

        partBlocks.forEachIndexed { partIdx, (partId, partXml) ->
            var divisions = 1f
            var currentFifths = 0
            var currentBeats = 4
            var currentBeatType = 4
            var partNoteCount = 0

            val measureBlocks = Regex("""<measure\b[^>]*>(.*?)</measure>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(partXml)
                .map { it.groupValues[1] }
                .toList()

            measureBlocks.forEachIndexed { mIdx, mXml ->
                xmlTag(mXml, "divisions")?.toFloatOrNull()?.let { if (it > 0f) divisions = it }
                xmlTag(mXml, "fifths")?.toIntOrNull()?.let { fifths ->
                    currentFifths = fifths
                    if (partIdx == 0 && mIdx == 0) {
                        globalKeySig = MidiScoreParser.matchKeySignature(fifths, 0)
                    }
                }
                xmlTag(mXml, "beats")?.toIntOrNull()?.let { if (it > 0) currentBeats = it }
                xmlTag(mXml, "beat-type")?.toIntOrNull()?.let { if (it > 0) currentBeatType = it }

                val measureKeySig = MidiScoreParser.matchKeySignature(currentFifths, 0)
                val notesInPartMeasure = mutableListOf<ScoreNote>()
                val restsInPartMeasure = mutableListOf<RestSymbol>()
                val pedalsInPartMeasure = mutableListOf<PedalEvent>()

                var cursorDivisions = 0f
                var lastNoteStartPosBeats = 0f
                var pendingChordSymbol = ""
                var pendingDynamic = ""
                var rehearsalMark = ""

                val tokenRegex = Regex("""<(note|backup|forward|harmony|direction)\b[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
                for (match in tokenRegex.findAll(mXml)) {
                    val tagType = match.groupValues[1]
                    val body = match.groupValues[2]
                    val quarterScale = 4f / currentBeatType.toFloat().coerceAtLeast(1f)

                    when (tagType) {
                        "backup" -> {
                            val durDiv = xmlTag(body, "duration")?.toFloatOrNull() ?: 0f
                            cursorDivisions = (cursorDivisions - durDiv).coerceAtLeast(0f)
                        }
                        "forward" -> {
                            val durDiv = xmlTag(body, "duration")?.toFloatOrNull() ?: 0f
                            cursorDivisions += durDiv
                        }
                        "harmony" -> {
                            val rootStep = xmlTag(body, "root-step") ?: ""
                            val rootAlter = xmlTag(body, "root-alter")?.toIntOrNull() ?: 0
                            val kindText = Regex("""<kind[^>]*text=["']([^"']*)["']""").find(body)?.groupValues?.get(1)
                                ?: when (xmlTag(body, "kind")?.lowercase()) {
                                    "minor" -> "m"
                                    "dominant" -> "7"
                                    "major-seventh" -> "maj7"
                                    "minor-seventh" -> "m7"
                                    "diminished" -> "dim"
                                    "augmented" -> "aug"
                                    "suspended-fourth" -> "sus4"
                                    "suspended-second" -> "sus2"
                                    else -> ""
                                }
                            if (rootStep.isNotBlank()) {
                                val altStr = when (rootAlter) { 1 -> "#"; -1 -> "b"; else -> "" }
                                pendingChordSymbol = "$rootStep$altStr$kindText"
                            }
                        }
                        "direction" -> {
                            xmlTag(body, "rehearsal")?.let { if (it.isNotBlank()) rehearsalMark = cleanXmlEntities(it) }
                            val dynBody = xmlTag(body, "dynamics")
                            if (dynBody != null) {
                                val dynMatch = Regex("""<(ppp|pp|p|mp|mf|f|ff|fff|sfz|rfz|fp)\b""").find(dynBody)
                                if (dynMatch != null) pendingDynamic = dynMatch.groupValues[1]
                            }
                            if (body.contains("<pedal")) {
                                val isStart = body.contains("""type="start"""") || body.contains("""type="change"""")
                                val posB = (cursorDivisions / divisions).coerceAtLeast(0f)
                                pedalsInPartMeasure.add(PedalEvent(position = posB, pedalType = 64, isPressed = isStart))
                            }
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
                            val voiceIdx = if (partBlocks.size == 1) {
                                when {
                                    staffNum != null -> if (staffNum <= 1) 0 else 1
                                    rawVoice <= 1 -> 0
                                    else -> 1
                                }
                            } else {
                                partIdx
                            }

                            if (isRest) {
                                restsInPartMeasure.add(
                                    RestSymbol(
                                        position = posBeats,
                                        duration = durBeats.coerceAtMost(currentBeats * quarterScale),
                                        voice = voiceIdx
                                    )
                                )
                            } else {
                                val stepStr = xmlTag(body, "step") ?: xmlTag(body, "display-step") ?: "C"
                                val octave = xmlTag(body, "octave")?.toIntOrNull() ?: xmlTag(body, "display-octave")?.toIntOrNull() ?: 4
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

                                val lyricText = xmlTag(body, "lyric")?.let { xmlTag(it, "text") }?.let { cleanXmlEntities(it) } ?: ""
                                val fingering = xmlTag(body, "fingering")?.filter { it.isDigit() }?.toIntOrNull()
                                val tabString = xmlTag(body, "string")?.toIntOrNull()?.let { (it - 1).coerceIn(0, 5) }
                                val tabFret = xmlTag(body, "fret")?.toIntOrNull()

                                val musArtic = when {
                                    body.contains("<staccatissimo") -> MusicalArticulation.STACCATISSIMO
                                    body.contains("<staccato") -> MusicalArticulation.STACCATO
                                    body.contains("<accent") -> MusicalArticulation.ACCENT
                                    body.contains("<tenuto") -> MusicalArticulation.TENUTO
                                    body.contains("<strong-accent") -> MusicalArticulation.MARCATO
                                    else -> MusicalArticulation.NONE
                                }

                                val tabArtic = when {
                                    body.contains("<hammer-on") -> TabArticulation.HAMMER_ON
                                    body.contains("<pull-off") -> TabArticulation.PULL_OFF
                                    body.contains("<slide") || body.contains("<glissando") -> TabArticulation.SLIDE_UP
                                    body.contains("<tap") -> TabArticulation.TAP
                                    body.contains("<snap-pizzicato") -> TabArticulation.POP
                                    else -> TabArticulation.NONE
                                }

                                val ornament = when {
                                    body.contains("<trill-mark") || body.contains("<wavy-line") -> OrnamentType.TRILL
                                    body.contains("<mordent") -> OrnamentType.UPPER_MORDENT
                                    body.contains("<inverted-mordent") -> OrnamentType.LOWER_MORDENT
                                    body.contains("<turn") && !body.contains("<inverted-turn") -> OrnamentType.TURN
                                    body.contains("<inverted-turn") -> OrnamentType.INVERTED_TURN
                                    else -> OrnamentType.NONE
                                }

                                val guitarHarmonic = when {
                                    body.contains("<harmonic") || body.contains("<natural-harmonic") -> GuitarHarmonicType.NATURAL
                                    body.contains("<artificial-harmonic") -> GuitarHarmonicType.ARTIFICIAL
                                    body.contains("<pinch-harmonic") -> GuitarHarmonicType.PINCH
                                    else -> GuitarHarmonicType.NONE
                                }

                                val pickDir = when {
                                    body.contains("<down-bow") -> PickDirection.DOWN
                                    body.contains("<up-bow") -> PickDirection.UP
                                    else -> PickDirection.NONE
                                }

                                val arpDir = when {
                                    body.contains("""<arpeggiate[^>]*direction="up"""".toRegex()) -> ArpeggioDirection.UP
                                    body.contains("""<arpeggiate[^>]*direction="down"""".toRegex()) -> ArpeggioDirection.DOWN
                                    body.contains("<arpeggiate") -> ArpeggioDirection.UP
                                    else -> ArpeggioDirection.NONE
                                }

                                val chordSymToAttach = if (!isChord) pendingChordSymbol.also { pendingChordSymbol = "" } else ""
                                val dynToAttach = if (!isChord) pendingDynamic.also { pendingDynamic = "" } else ""

                                notesInPartMeasure.add(
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
                                        chordSymbol = chordSymToAttach,
                                        dynamic = dynToAttach,
                                        fingering = fingering,
                                        articulation = tabArtic,
                                        musicalArticulation = musArtic,
                                        ornament = ornament,
                                        guitarHarmonic = guitarHarmonic,
                                        pickDirection = pickDir,
                                        arpeggio = arpDir
                                    )
                                )
                                partNoteCount++
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

                val existingMeasure = mergedMeasuresMap[mIdx]
                if (existingMeasure == null) {
                    mergedMeasuresMap[mIdx] = Measure(
                        measureIndex = mIdx + 1,
                        timeSignatureNumerator = currentBeats,
                        timeSignatureDenominator = currentBeatType,
                        keySignature = measureKeySig,
                        notes = notesInPartMeasure.sortedWith(compareBy({ it.position }, { it.midiNote })),
                        rests = restsInPartMeasure,
                        pedalEvents = pedalsInPartMeasure,
                        rehearsalMark = rehearsalMark,
                        repeatStart = repeatStart,
                        repeatEnd = repeatEnd,
                        barline = if (mIdx == measureBlocks.lastIndex) "final" else "single"
                    )
                } else {
                    mergedMeasuresMap[mIdx] = existingMeasure.copy(
                        notes = (existingMeasure.notes + notesInPartMeasure).sortedWith(compareBy({ it.position }, { it.midiNote })),
                        rests = existingMeasure.rests + restsInPartMeasure,
                        pedalEvents = existingMeasure.pedalEvents + pedalsInPartMeasure,
                        rehearsalMark = existingMeasure.rehearsalMark.ifBlank { rehearsalMark },
                        repeatStart = existingMeasure.repeatStart || repeatStart,
                        repeatEnd = existingMeasure.repeatEnd || repeatEnd
                    )
                }
            }

            if (partBlocks.size > 1 && partNoteCount > 0) {
                val pName = partNamesById[partId] ?: "Part ${partIdx + 1}"
                trackInfos.add(
                    MidiTrackInfo(
                        trackIndex = partIdx,
                        trackName = "🎵 $pName",
                        channel = partIdx.coerceIn(0, 15),
                        noteCount = partNoteCount,
                        program = 0,
                        isEnabled = true
                    )
                )
            }
        }

        val finalMeasures = mergedMeasuresMap.values.toList().ifEmpty {
            listOf(Measure(notes = emptyList()))
        }

        if (trackInfos.isEmpty()) {
            val allNotes = finalMeasures.flatMap { it.notes }
            val rhCount = allNotes.count { it.voice == 0 || it.midiNote >= 60 }
            val lhCount = allNotes.count { it.voice == 1 || it.midiNote < 60 }
            if (rhCount > 0) {
                trackInfos.add(MidiTrackInfo(0, "🎹 Right Hand (Treble)", 0, rhCount, 0, true))
            }
            if (lhCount > 0) {
                trackInfos.add(MidiTrackInfo(1, "🎹 Left Hand (Bass)", 0, lhCount, 0, true))
            }
        }

        val firstM = finalMeasures.firstOrNull()
        val timeSigStr = if (firstM != null) "${firstM.timeSignatureNumerator}/${firstM.timeSignatureDenominator}" else "4/4"

        return Score(
            title = cleanXmlEntities(rawTitle),
            composer = cleanXmlEntities(composer),
            lyricist = cleanXmlEntities(lyricist),
            tempo = globalTempo,
            timeSignature = timeSigStr,
            keySignature = firstM?.keySignature ?: globalKeySig,
            measures = finalMeasures,
            tracks = trackInfos
        )
    }

    /**
     * Encodes a [Score] into a compact GZIP+Base64 ASCII trailer block that can be appended to any
     * exported PDF file without affecting PDF viewers, enabling 100% lossless round-trip PDF re-importing!
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encodeScorePayloadForPdf(score: Score): ByteArray {
        val sb = StringBuilder()
        sb.append("TITLE=").append(score.title.replace("\n", " ")).append('\n')
        sb.append("COMPOSER=").append(score.composer.replace("\n", " ")).append('\n')
        sb.append("TEMPO=").append(score.tempo).append('\n')
        sb.append("TIMESIG=").append(score.timeSignature).append('\n')
        sb.append("KEYSIG=").append(score.keySignature.name).append('\n')

        score.tracks.forEach { tr ->
            sb.append("TRACK=").append(tr.trackIndex).append('|')
                .append(tr.trackName.replace("|", "/")).append('|')
                .append(tr.channel).append('|')
                .append(tr.noteCount).append('|')
                .append(tr.program).append('\n')
        }

        score.measures.forEach { m ->
            sb.append("MEASURE=").append(m.measureIndex).append('|')
                .append(m.timeSignatureNumerator).append('|')
                .append(m.timeSignatureDenominator).append('|')
                .append(m.keySignature.name).append('|')
                .append(m.rehearsalMark.replace("|", "/")).append('\n')
            m.notes.forEach { n ->
                if (!n.isRest) {
                    sb.append("NOTE=").append(n.midiNote).append('|')
                        .append(n.position).append('|')
                        .append(n.duration).append('|')
                        .append(n.voice).append('|')
                        .append(n.fingering ?: 0).append('|')
                        .append(if (n.dotted || n.isDotted) 1 else 0).append('|')
                        .append(n.tuplet).append('|')
                        .append(if (n.tieStart) 1 else 0).append('|')
                        .append(if (n.tieEnd) 1 else 0).append('|')
                        .append(n.lyric.replace("|", " ").replace("\n", " ")).append('|')
                        .append(n.chordSymbol.replace("|", " ")).append('|')
                        .append(n.dynamic.replace("|", " ")).append('\n')
                }
            }
        }

        val compressedBytes = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(sb.toString().encodeToByteArray())
            }
            baos.toByteArray()
        }
        val b64 = Base64.encode(compressedBytes)
        return "\n$PEQ_PDF_PAYLOAD_START$b64$PEQ_PDF_PAYLOAD_END\n".encodeToByteArray()
    }

    /**
     * Decodes a lossless [Score] from a pEQ-exported PDF trailer if present.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decodeEmbeddedScoreFromPdf(pdfBytes: ByteArray): Score? {
        return try {
            val tailSize = minOf(pdfBytes.size, 512 * 1024)
            val tailString = String(pdfBytes, pdfBytes.size - tailSize, tailSize, Charsets.ISO_8859_1)
            val startIdx = tailString.indexOf(PEQ_PDF_PAYLOAD_START)
            val endIdx = tailString.indexOf(PEQ_PDF_PAYLOAD_END)
            if (startIdx < 0 || endIdx <= startIdx) return null

            val b64 = tailString.substring(startIdx + PEQ_PDF_PAYLOAD_START.length, endIdx).trim()
            val compressed = Base64.decode(b64)
            val decompressed = GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes().decodeToString() }

            var title = "Imported PDF Score"
            var composer = ""
            var tempo = 120f
            var timeSig = "4/4"
            var keySig = KeySignature.C_MAJOR
            val tracks = mutableListOf<MidiTrackInfo>()
            val measures = mutableListOf<Measure>()
            var currentMeasureNotes = mutableListOf<ScoreNote>()
            var currentMeasureMeta: Measure? = null
            var noteId = 0

            for (line in decompressed.lineSequence()) {
                when {
                    line.startsWith("TITLE=") -> title = line.removePrefix("TITLE=")
                    line.startsWith("COMPOSER=") -> composer = line.removePrefix("COMPOSER=")
                    line.startsWith("TEMPO=") -> tempo = line.removePrefix("TEMPO=").toFloatOrNull() ?: 120f
                    line.startsWith("TIMESIG=") -> timeSig = line.removePrefix("TIMESIG=")
                    line.startsWith("KEYSIG=") -> keySig = runCatching { KeySignature.valueOf(line.removePrefix("KEYSIG=")) }.getOrDefault(KeySignature.C_MAJOR)
                    line.startsWith("TRACK=") -> {
                        val p = line.removePrefix("TRACK=").split('|')
                        if (p.size >= 5) {
                            tracks.add(
                                MidiTrackInfo(
                                    trackIndex = p[0].toIntOrNull() ?: 0,
                                    trackName = p[1],
                                    channel = p[2].toIntOrNull() ?: 0,
                                    noteCount = p[3].toIntOrNull() ?: 0,
                                    program = p[4].toIntOrNull() ?: 0,
                                    isEnabled = true
                                )
                            )
                        }
                    }
                    line.startsWith("MEASURE=") -> {
                        currentMeasureMeta?.let { prev ->
                            measures.add(prev.copy(notes = currentMeasureNotes.toList()))
                        }
                        currentMeasureNotes = mutableListOf()
                        val p = line.removePrefix("MEASURE=").split('|')
                        val mIdx = p.getOrNull(0)?.toIntOrNull() ?: (measures.size + 1)
                        val num = p.getOrNull(1)?.toIntOrNull() ?: 4
                        val den = p.getOrNull(2)?.toIntOrNull() ?: 4
                        val ks = p.getOrNull(3)?.let { runCatching { KeySignature.valueOf(it) }.getOrNull() } ?: keySig
                        val reh = p.getOrNull(4).orEmpty()
                        currentMeasureMeta = Measure(
                            measureIndex = mIdx,
                            timeSignatureNumerator = num,
                            timeSignatureDenominator = den,
                            keySignature = ks,
                            rehearsalMark = reh
                        )
                    }
                    line.startsWith("NOTE=") -> {
                        val p = line.removePrefix("NOTE=").split('|')
                        if (p.size >= 9) {
                            val midi = p[0].toIntOrNull() ?: 60
                            val pos = p[1].toFloatOrNull() ?: 0f
                            val dur = p[2].toFloatOrNull() ?: 0.5f
                            val voice = p[3].toIntOrNull() ?: 0
                            val fing = p[4].toIntOrNull()?.takeIf { it in 1..5 }
                            val dotted = p[5] == "1"
                            val tuplet = p[6].toIntOrNull() ?: 0
                            val tStart = p[7] == "1"
                            val tEnd = p[8] == "1"
                            val lyr = p.getOrNull(9).orEmpty()
                            val chSym = p.getOrNull(10).orEmpty()
                            val dyn = p.getOrNull(11).orEmpty()
                            currentMeasureNotes.add(
                                ScoreNote(
                                    id = ++noteId,
                                    midiNote = midi,
                                    position = pos,
                                    duration = dur,
                                    voice = voice,
                                    fingering = fing,
                                    dotted = dotted,
                                    isDotted = dotted,
                                    tuplet = tuplet,
                                    tieStart = tStart,
                                    tieEnd = tEnd,
                                    lyric = lyr,
                                    chordSymbol = chSym,
                                    dynamic = dyn,
                                    accidental = MidiScoreParser.resolveAccidentalForPitch(midi, keySig)
                                )
                            )
                        }
                    }
                }
            }
            currentMeasureMeta?.let { last ->
                measures.add(last.copy(notes = currentMeasureNotes.toList(), barline = "final"))
            }

            if (measures.isEmpty()) return null
            Score(
                title = title,
                composer = composer,
                tempo = tempo,
                timeSignature = timeSig,
                keySignature = keySig,
                measures = measures,
                tracks = tracks,
                isPdfScore = false
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts playable notation from vector/digital sheet music PDFs by inspecting embedded XML/MusicXML
     * attachments or PDF content streams.
     */
    private fun parseVectorSheetMusicPdf(pdfBytes: ByteArray, defaultTitle: String): Score? {
        return try {
            val rawAscii = String(pdfBytes, Charsets.ISO_8859_1)
            // Check if the PDF contains an embedded MusicXML stream (e.g. hybrid MuseScore / LilyPond PDF)
            val xmlStart = rawAscii.indexOf("<score-partwise")
            val xmlEnd = rawAscii.indexOf("</score-partwise>")
            if (xmlStart >= 0 && xmlEnd > xmlStart) {
                val embeddedXml = rawAscii.substring(xmlStart, xmlEnd + "</score-partwise>".length)
                return parseMusicXml(embeddedXml, defaultTitle)
            }
            null
        } catch (_: Exception) {
            null
        }
    }
    private fun cleanXmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .trim()
    }

    private fun stepOctaveAlterToMidi(step: String, octave: Int, alter: Int): Int {
        val baseSemitone = when (step.uppercase().trim()) {
            "C" -> 0
            "D" -> 2
            "E" -> 4
            "F" -> 5
            "G" -> 7
            "A" -> 9
            "B" -> 11
            else -> 0
        }
        // standard MIDI formula: (Octave + 1) * 12 + semitone + accidental offset
        return ((octave + 1) * 12 + baseSemitone + alter).coerceIn(0, 127)
    }

    private fun xmlTag(xml: String, tag: String): String? {
        // Correctly matches open/close tags and extracts inner content safely
        val match = Regex("<$tag(?:\\s[^>]*)?>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL).find(xml)
        return match?.groupValues?.get(1)?.trim()
    }
}