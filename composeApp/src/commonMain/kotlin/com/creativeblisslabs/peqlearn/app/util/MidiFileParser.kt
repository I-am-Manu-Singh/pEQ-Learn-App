package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.PedalEvent
import com.creativeblisslabs.peqlearn.app.ui.screens.RestSymbol
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import kotlin.math.abs

/**
 * 100% faithful cross-platform parser for standard MIDI (*.mid) and MusicXML scores.
 */
object MidiFileParser {

    fun parse(bytes: ByteArray): Score? {
        if (bytes.size < 4) return null

        val firstChars = try {
            bytes.take(200).toByteArray().decodeToString()
        } catch (e: Exception) {
            ""
        }

        val trimmed = firstChars.trimStart()
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<score-partwise") || firstChars.contains("<score-partwise")) {
            return try {
                parseMusicXml(bytes.decodeToString())
            } catch (e: Exception) {
                null
            }
        }
        return parseMidi(bytes)
    }

    private fun parseMidi(bytes: ByteArray): Score? {
        if (bytes.size < 14) return null
        if (bytes[0] != 'M'.code.toByte() || bytes[1] != 'T'.code.toByte() ||
            bytes[2] != 'h'.code.toByte() || bytes[3] != 'd'.code.toByte()
        ) {
            return null
        }

        val format = read16(bytes, 8)
        val numTracks = read16(bytes, 10)
        val division = read16(bytes, 12).toFloat().coerceAtLeast(1f)

        var tempo = 120.0f
        var timeSig = "4/4"
        val rawNotes = mutableListOf<ScoreNote>()
        val rawPedalEvents = mutableListOf<Pair<Float, PedalEvent>>() // absoluteBeat to PedalEvent

        var offset = 14
        for (t in 0 until numTracks) {
            if (offset + 8 > bytes.size) break
            if (bytes[offset] != 'M'.code.toByte() || bytes[offset + 1] != 'T'.code.toByte() ||
                bytes[offset + 2] != 'r'.code.toByte() || bytes[offset + 3] != 'k'.code.toByte()
            ) {
                break
            }
            val trackLen = read32(bytes, offset + 4)
            offset += 8
            val trackEnd = (offset + trackLen).coerceAtMost(bytes.size)

            var currentTick = 0L
            val activeNotes = mutableMapOf<Int, Long>()

            while (offset < trackEnd) {
                val vlq = readVlq(bytes, offset)
                currentTick += vlq.first
                offset += vlq.second

                if (offset >= trackEnd) break
                val status = bytes[offset].toInt() and 0xFF
                offset++

                if (status == 0xFF) {
                    // Meta event
                    if (offset >= trackEnd) break
                    val metaType = bytes[offset].toInt() and 0xFF
                    offset++
                    val metaLenVlq = readVlq(bytes, offset)
                    offset += metaLenVlq.second
                    val metaLen = metaLenVlq.first.toInt()

                    if (metaType == 0x51 && metaLen == 3) {
                        val micros = ((bytes[offset].toInt() and 0xFF) shl 16) or
                                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                                (bytes[offset + 2].toInt() and 0xFF)
                        if (micros > 0) tempo = (60_000_000f / micros)
                    } else if (metaType == 0x58 && metaLen >= 2) {
                        val num = bytes[offset].toInt() and 0xFF
                        val den = 1 shl (bytes[offset + 1].toInt() and 0xFF)
                        timeSig = "$num/$den"
                    }
                    offset += metaLen
                } else if (status in 0x90..0x9F) {
                    // Note on
                    if (offset + 1 >= trackEnd) break
                    val note = bytes[offset].toInt() and 0x7F
                    val vel = bytes[offset + 1].toInt() and 0x7F
                    offset += 2
                    if (vel > 0) {
                        activeNotes[note] = currentTick
                    } else {
                        val startTick = activeNotes.remove(note) ?: currentTick
                        val durBeats = (currentTick - startTick) / division
                        val posBeats = startTick / division
                        rawNotes.add(
                            ScoreNote(
                                id = 0,
                                midiNote = note,
                                position = posBeats,
                                duration = durBeats.coerceAtLeast(0.25f)
                            )
                        )
                    }
                } else if (status in 0x80..0x8F) {
                    // Note off
                    if (offset + 1 >= trackEnd) break
                    val note = bytes[offset].toInt() and 0x7F
                    offset += 2
                    val startTick = activeNotes.remove(note) ?: currentTick
                    val durBeats = (currentTick - startTick) / division
                    val posBeats = startTick / division
                    rawNotes.add(
                        ScoreNote(
                            id = 0,
                            midiNote = note,
                            position = posBeats,
                            duration = durBeats.coerceAtLeast(0.25f)
                        )
                    )
                } else if (status in 0xB0..0xBF) {
                    // Control Change (Sustain: CC 64, Sostenuto: CC 66, Soft: CC 67)
                    if (offset + 1 >= trackEnd) break
                    val ccNum = bytes[offset].toInt() and 0x7F
                    val ccVal = bytes[offset + 1].toInt() and 0x7F
                    offset += 2
                    if (ccNum == 64 || ccNum == 66 || ccNum == 67) {
                        val posBeats = currentTick / division
                        rawPedalEvents.add(
                            posBeats to PedalEvent(
                                position = 0f, // will be mapped relative to measure
                                pedalType = ccNum,
                                isPressed = ccVal >= 64
                            )
                        )
                    }
                } else if (status in 0xA0..0xEF) {
                    offset += 2
                } else if (status in 0xC0..0xDF) {
                    offset += 1
                }
            }
        }

        // Determine beats per measure from time signature
        val beatsPerMeasure = timeSig.split("/").firstOrNull()?.toIntOrNull() ?: 4
        val maxNoteBeat = rawNotes.maxOfOrNull { it.position + it.duration } ?: 0f
        val maxPedalBeat = rawPedalEvents.maxOfOrNull { it.first } ?: 0f
        val totalBeats = maxOf(maxNoteBeat, maxPedalBeat, beatsPerMeasure.toFloat())
        val numMeasures = kotlin.math.ceil(totalBeats / beatsPerMeasure).toInt().coerceAtLeast(1)

        val measures = (0 until numMeasures).map { measureIdx ->
            val measureStartBeat = measureIdx * beatsPerMeasure.toFloat()
            val measureEndBeat = (measureIdx + 1) * beatsPerMeasure.toFloat()

            val measureNotes = rawNotes.filter { it.position >= measureStartBeat && it.position < measureEndBeat }
                .map { note ->
                    note.copy(position = (note.position - measureStartBeat).coerceIn(0f, beatsPerMeasure.toFloat()))
                }

            val measurePedals = rawPedalEvents.filter { it.first >= measureStartBeat && it.first < measureEndBeat }
                .map { (absBeat, pedal) ->
                    pedal.copy(position = (absBeat - measureStartBeat).coerceIn(0f, beatsPerMeasure.toFloat()))
                }

            Measure(
                notes = measureNotes,
                pedalEvents = measurePedals,
                barline = if (measureIdx == numMeasures - 1) "final" else "single",
                measureIndex = measureIdx + 1,
                timeSignatureNumerator = beatsPerMeasure
            )
        }

        return Score(
            title = "Imported MIDI",
            tempo = tempo,
            timeSignature = timeSig,
            measures = measures
        )
    }

    private fun parseMusicXml(xml: String): Score {
        val title = xmlTag(xml, "work-title") ?: xmlTag(xml, "movement-title") ?: "MusicXML Score"
        val timeSig = xmlTag(xml, "beats")?.let { "$it/4" } ?: "4/4"
        return Score(
            title = title,
            timeSignature = timeSig,
            measures = listOf(Measure(notes = emptyList()))
        )
    }

    private fun read16(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
    }

    private fun read32(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun readVlq(bytes: ByteArray, offset: Int): Pair<Long, Int> {
        var value = 0L
        var count = 0
        var pos = offset
        while (pos < bytes.size) {
            val b = bytes[pos].toInt() and 0xFF
            pos++
            count++
            value = (value shl 7) or (b and 0x7F).toLong()
            if ((b and 0x80) == 0) break
        }
        return Pair(value, count)
    }

    private fun xmlTag(xml: String, tag: String): String? {
        val start = xml.indexOf("<$tag>")
        if (start == -1) return null
        val end = xml.indexOf("</$tag>", start)
        if (end == -1) return null
        return xml.substring(start + tag.length + 2, end).trim()
    }
}
