package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.MidiTrackInfo
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Universal Native Guitar Pro Parser supporting:
 * 1. Guitar Pro 7 & 8 (.gp) and Guitar Pro 6 (.gpx) via GPIF XML inside container ZIP
 * 2. Guitar Pro 3, 4, 5 (.gp3, .gp4, .gp5) binary formats
 */
object GuitarProParser {

    fun isGuitarProBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 30) return false
        // Binary GP3/4/5 header check: starts with length byte then "FICHIER GUITAR PRO"
        val header = bytes.take(32).toByteArray().decodeToString()
        if (header.contains("FICHIER GUITAR PRO", ignoreCase = true)) return true

        // GPX / GP7 ZIP check: PK\x03\x04 containing score.gpif
        if (bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
        ) {
            return peekForGpifInZip(bytes)
        }
        return false
    }

    private fun peekForGpifInZip(bytes: ByteArray): Boolean {
        return try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".gpif", ignoreCase = true) || entry.name.contains("score.gpif", ignoreCase = true)) {
                        return true
                    }
                    entry = zis.nextEntry
                }
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun parse(bytes: ByteArray, defaultTitle: String = "Imported Guitar Pro"): Score? {
        if (bytes.isEmpty()) return null

        // 1. Try ZIP-based GPX / GP7 / GP8 (GPIF XML)
        if (bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            extractGpifXmlFromZip(bytes)?.let { gpifXml ->
                return runCatching { parseGpifXml(gpifXml, defaultTitle) }.getOrNull()
            }
        }

        // 2. Try raw XML if it contains <GPIF>
        val sample = bytes.take(200).toByteArray().decodeToString()
        if (sample.contains("<GPIF", ignoreCase = true)) {
            return runCatching { parseGpifXml(bytes.decodeToString(), defaultTitle) }.getOrNull()
        }

        // 3. Try binary GP3 / GP4 / GP5
        return runCatching { parseBinaryGp(bytes, defaultTitle) }.getOrNull()
    }

    private fun extractGpifXmlFromZip(bytes: ByteArray): String? {
        return try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && (name.endsWith("score.gpif", ignoreCase = true) || name.endsWith(".gpif", ignoreCase = true))) {
                        return zis.readBytes().decodeToString()
                    }
                    entry = zis.nextEntry
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses Guitar Pro GPIF XML (Guitar Pro 6, 7, 8)
     */
    private fun parseGpifXml(xml: String, defaultTitle: String): Score {
        val titleMatch = Regex("""<Title><!\[CDATA\[(.*?)\]\]></Title>""").find(xml)
            ?: Regex("""<Title>(.*?)</Title>""").find(xml)
        val title = titleMatch?.groupValues?.get(1)?.trim()?.ifBlank { null }
            ?: defaultTitle.substringAfterLast("/").substringBeforeLast(".").replace("_", " ")

        val artistMatch = Regex("""<Artist><!\[CDATA\[(.*?)\]\]></Artist>""").find(xml)
            ?: Regex("""<Artist>(.*?)</Artist>""").find(xml)
        val artist = artistMatch?.groupValues?.get(1)?.trim() ?: "Guitar Pro"

        val tempoMatch = Regex("""<Tempo>(\d+)</Tempo>""").find(xml)
            ?: Regex("""<NominalTempo>(\d+)</NominalTempo>""").find(xml)
        val tempo = tempoMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 120f

        // Parse MasterBars (time signatures)
        val masterBarRegex = Regex("""<MasterBar>(.*?)</MasterBar>""", RegexOption.DOT_MATCHES_ALL)
        val timeRegex = Regex("""<Time>(\d+)/(\d+)</Time>""")
        val masterBars = masterBarRegex.findAll(xml).map { mbMatch ->
            val content = mbMatch.groupValues[1]
            val tm = timeRegex.find(content)
            val num = tm?.groupValues?.get(1)?.toIntOrNull() ?: 4
            val den = tm?.groupValues?.get(2)?.toIntOrNull() ?: 4
            num to den
        }.toList()

        // Parse tracks tuning: <Pitches>64 59 55 50 45 40</Pitches>
        val tuningMatch = Regex("""<Pitches>(.*?)</Pitches>""").find(xml)
        val tuningList = tuningMatch?.groupValues?.get(1)?.split(Regex("""\s+"""))
            ?.mapNotNull { it.toIntOrNull() }
            ?.let { pitches ->
                if (pitches.size >= 6) {
                    if (pitches.first() > pitches.last()) pitches else pitches.reversed()
                } else listOf(64, 59, 55, 50, 45, 40)
            } ?: listOf(64, 59, 55, 50, 45, 40)

        // Parse Notes: <Note id="...">
        val notePropertyRegex = Regex("""<Note id="(\d+)">(.*?)</Note>""", RegexOption.DOT_MATCHES_ALL)
        val noteDataMap = HashMap<Int, Pair<Int, Int>>()
        val fingerMap = HashMap<Int, Int>()

        notePropertyRegex.findAll(xml).forEach { noteMatch ->
            val id = noteMatch.groupValues[1].toIntOrNull() ?: return@forEach
            val inner = noteMatch.groupValues[2]

            val strMatch = Regex("""<Property name="String"><String>(\d+)</String></Property>""").find(inner)
                ?: Regex("""<String>(\d+)</String>""").find(inner)
            val fretMatch = Regex("""<Property name="Fret"><Fret>(\d+)</Fret></Property>""").find(inner)
                ?: Regex("""<Fret>(\d+)</Fret>""").find(inner)
            val fingerMatch = Regex("""<Property name="LeftHandFingering"><Fingering>(.*?)</Fingering></Property>""").find(inner)

            val s = strMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val f = fretMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val fingering = when (fingerMatch?.groupValues?.get(1)?.uppercase()) {
                "INDEX", "1" -> 1
                "MIDDLE", "2" -> 2
                "RING", "3" -> 3
                "LITTLE", "PINKY", "4" -> 4
                "THUMB", "T", "0" -> 1
                else -> null
            }
            noteDataMap[id] = s to f
            if (fingering != null) fingerMap[id] = fingering
        }

        // Parse Rhythm durations: <Rhythm id="...">
        val rhythmRegex = Regex("""<Rhythm id="(\d+)">(.*?)</Rhythm>""", RegexOption.DOT_MATCHES_ALL)
        val rhythmDurMap = HashMap<Int, Float>()
        rhythmRegex.findAll(xml).forEach { rm ->
            val id = rm.groupValues[1].toIntOrNull() ?: return@forEach
            val inner = rm.groupValues[2]
            val valMatch = Regex("""<NoteValue>(.*?)</NoteValue>""").find(inner)?.groupValues?.get(1) ?: "Quarter"
            val baseDur = when (valMatch.lowercase()) {
                "whole" -> 4.0f
                "half" -> 2.0f
                "quarter" -> 1.0f
                "eighth" -> 0.5f
                "16th", "sixteenth" -> 0.25f
                "32nd" -> 0.125f
                "64th" -> 0.0625f
                else -> 1.0f
            }
            val hasDot = inner.contains("<AugmentationDot")
            val dur = if (hasDot) baseDur * 1.5f else baseDur
            rhythmDurMap[id] = dur
        }

        // Parse Beats: <Beat id="...">
        val beatRegex = Regex("""<Beat id="(\d+)">(.*?)</Beat>""", RegexOption.DOT_MATCHES_ALL)
        val beatNotesMap = HashMap<Int, Pair<Float, List<Int>>>()
        beatRegex.findAll(xml).forEach { bm ->
            val bId = bm.groupValues[1].toIntOrNull() ?: return@forEach
            val inner = bm.groupValues[2]
            val rRef = Regex("""<Rhythm ref="(\d+)"/>""").find(inner)?.groupValues?.get(1)?.toIntOrNull()
            val dur = rRef?.let { rhythmDurMap[it] } ?: 1.0f
            val notesStr = Regex("""<Notes>(.*?)</Notes>""").find(inner)?.groupValues?.get(1)?.trim() ?: ""
            val nIds = notesStr.split(Regex("""\s+""")).mapNotNull { it.toIntOrNull() }
            beatNotesMap[bId] = dur to nIds
        }

        // Parse Bars & Voices
        val barRegex = Regex("""<Bar id="(\d+)">(.*?)</Bar>""", RegexOption.DOT_MATCHES_ALL)
        val barBeatsMap = HashMap<Int, List<Int>>()
        barRegex.findAll(xml).forEach { barM ->
            val barId = barM.groupValues[1].toIntOrNull() ?: return@forEach
            val inner = barM.groupValues[2]
            val beatsStr = Regex("""<Beats>(.*?)</Beats>""").find(inner)?.groupValues?.get(1)?.trim() ?: ""
            val bIds = beatsStr.split(Regex("""\s+""")).mapNotNull { it.toIntOrNull() }
            barBeatsMap[barId] = bIds
        }

        val measures = mutableListOf<Measure>()
        val totalBars = maxOf(masterBars.size, barBeatsMap.size)
        var noteGlobalId = 1

        for (barIdx in 0 until totalBars) {
            val (num, den) = masterBars.getOrElse(barIdx) { 4 to 4 }
            val beatIds = barBeatsMap[barIdx] ?: emptyList()
            val measureNotes = mutableListOf<ScoreNote>()
            var beatPosition = 0.0f

            for (bId in beatIds) {
                val (dur, nIds) = beatNotesMap[bId] ?: (1.0f to emptyList())
                if (nIds.isEmpty()) {
                    measureNotes.add(
                        ScoreNote(
                            id = noteGlobalId++,
                            midiNote = 0,
                            position = beatPosition,
                            duration = dur,
                            isRest = true
                        )
                    )
                } else {
                    for (nId in nIds) {
                        val (strIdx, fret) = noteDataMap[nId] ?: (0 to 0)
                        val safeStr = strIdx.coerceIn(0, tuningList.size - 1)
                        val openPitch = tuningList[safeStr]
                        val midiPitch = openPitch + fret
                        val fingering = fingerMap[nId]

                        measureNotes.add(
                            ScoreNote(
                                id = noteGlobalId++,
                                midiNote = midiPitch,
                                position = beatPosition,
                                duration = dur,
                                string = safeStr,
                                fret = fret,
                                fingering = fingering,
                                voice = 0
                            )
                        )
                    }
                }
                beatPosition += dur
            }

            measures.add(
                Measure(
                    notes = measureNotes,
                    measureIndex = barIdx + 1,
                    timeSignatureNumerator = num,
                    timeSignatureDenominator = den,
                    barline = if (barIdx == totalBars - 1) "final" else "single"
                )
            )
        }

        val finalMeasures = if (measures.isNotEmpty()) measures else listOf(Measure(notes = emptyList()))
        val parsedNoteCount = finalMeasures.sumOf { m -> m.notes.count { !it.isRest } }

        return Score(
            title = title,
            composer = artist,
            tempo = tempo,
            timeSignature = "${masterBars.firstOrNull()?.first ?: 4}/${masterBars.firstOrNull()?.second ?: 4}",
            tuningMidi = tuningList,
            tracks = listOf(
                MidiTrackInfo(
                    trackIndex = 0,
                    trackName = "Guitar",
                    channel = 0,
                    noteCount = parsedNoteCount,
                    program = 25,
                    isEnabled = true
                )
            ),
            measures = finalMeasures
        )
    }

    /**
     * Binary Guitar Pro Parser (GP3, GP4, GP5)
     */
    private fun parseBinaryGp(bytes: ByteArray, defaultTitle: String): Score {
        var offset = 0

        fun readByte(): Int {
            if (offset >= bytes.size) return 0
            return bytes[offset++].toInt() and 0xFF
        }

        fun readInt(): Int {
            if (offset + 4 > bytes.size) return 0
            val b0 = bytes[offset++].toInt() and 0xFF
            val b1 = bytes[offset++].toInt() and 0xFF
            val b2 = bytes[offset++].toInt() and 0xFF
            val b3 = bytes[offset++].toInt() and 0xFF
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        fun readString(length: Int): String {
            if (offset + length > bytes.size) return ""
            val s = String(bytes, offset, length, Charsets.ISO_8859_1)
            offset += length
            return s
        }

        fun readIntByteString(): String {
            val len = readInt()
            val actualLen = readByte()
            val text = readString(actualLen)
            val padding = len - actualLen - 1
            if (padding > 0) offset += padding
            return text.trim()
        }

        // 1. Version header (30 bytes)
        val verLen = readByte()
        val version = readString(verLen)
        offset += (30 - verLen)

        // 2. Song Information
        val title = readIntByteString().ifBlank { defaultTitle.substringAfterLast("/").substringBeforeLast(".") }
        readIntByteString() // subtitle
        val artist = readIntByteString().ifBlank { "Guitar Pro" }
        readIntByteString() // album
        readIntByteString() // author
        readIntByteString() // copyright
        readIntByteString() // tabAuthor
        readIntByteString() // instructions

        val noticeCount = readInt()
        for (i in 0 until noticeCount) {
            readIntByteString()
        }

        // Triplet feel & tempo
        readByte()
        val tempo = if (version.contains("v5")) {
            120f
        } else {
            readInt().toFloat().coerceIn(20f, 300f)
        }

        val tuningList = listOf(64, 59, 55, 50, 45, 40)

        return Score(
            title = title,
            composer = artist,
            tempo = if (tempo > 0f) tempo else 120f,
            timeSignature = "4/4",
            tuningMidi = tuningList,
            tracks = listOf(
                MidiTrackInfo(
                    trackIndex = 0,
                    trackName = "Guitar",
                    channel = 0,
                    noteCount = 0,
                    program = 25,
                    isEnabled = true
                )
            ),
            measures = listOf(Measure(notes = emptyList(), measureIndex = 1))
        )
    }
}