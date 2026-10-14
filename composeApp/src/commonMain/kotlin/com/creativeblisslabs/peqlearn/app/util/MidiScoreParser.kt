package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote

/**
 * Robust MIDI file parser that parses actual MIDI events, sequence title, tempo BPM,
 * time signature, key signature, and places notes accurately into measures.
 */
object MidiScoreParser {

    private data class RawMidiNote(
        val midiNote: Int,
        val startTick: Long,
        val durationTicks: Long,
        val channel: Int
    )

    fun parse(bytes: ByteArray, defaultTitle: String = "Imported Score"): Score? {
        if (bytes.size < 14) return null
        if (bytes[0] != 'M'.code.toByte() || bytes[1] != 'T'.code.toByte() ||
            bytes[2] != 'h'.code.toByte() || bytes[3] != 'd'.code.toByte()
        ) {
            return null
        }

        val format = read16(bytes, 8)
        val numTracks = read16(bytes, 10)
        val division = read16(bytes, 12).toFloat().coerceAtLeast(1f)

        var tempoBpm = 120f
        var timeNumerator = 4
        var timeDenominator = 4
        var keySignature = KeySignature.C_MAJOR
        var detectedTitle = ""

        val rawNotes = mutableListOf<RawMidiNote>()

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
            var runningStatus = 0
            val activeNotes = mutableMapOf<Pair<Int, Int>, Long>() // (channel, note) -> startTick

            while (offset < trackEnd) {
                val vlq = readVlq(bytes, offset)
                currentTick += vlq.first
                offset += vlq.second

                if (offset >= trackEnd) break
                var status = bytes[offset].toInt() and 0xFF

                if ((status and 0x80) != 0) {
                    offset++
                    if (status < 0xF0) {
                        runningStatus = status
                    }
                } else {
                    status = runningStatus
                }

                if (status == 0xFF) {
                    // Meta event
                    if (offset >= trackEnd) break
                    val metaType = bytes[offset].toInt() and 0xFF
                    offset++
                    val metaLenVlq = readVlq(bytes, offset)
                    offset += metaLenVlq.second
                    val metaLen = metaLenVlq.first.toInt()
                    val metaEnd = (offset + metaLen).coerceAtMost(trackEnd)

                    when (metaType) {
                        0x01, 0x03 -> { // Text / Track name
                            if (detectedTitle.isBlank() && metaLen > 0 && offset + metaLen <= bytes.size) {
                                val text = bytes.copyOfRange(offset, offset + metaLen).decodeToString().trim()
                                if (text.isNotBlank()) detectedTitle = text
                            }
                        }
                        0x51 -> { // Set Tempo
                            if (metaLen >= 3 && offset + 3 <= bytes.size) {
                                val micros = ((bytes[offset].toInt() and 0xFF) shl 16) or
                                        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                                        (bytes[offset + 2].toInt() and 0xFF)
                                if (micros > 0) {
                                    tempoBpm = 60_000_000f / micros.toFloat()
                                }
                            }
                        }
                        0x58 -> { // Time Signature
                            if (metaLen >= 2 && offset + 2 <= bytes.size) {
                                val num = bytes[offset].toInt() and 0xFF
                                val denPow = bytes[offset + 1].toInt() and 0xFF
                                if (num in 1..32) timeNumerator = num
                                if (denPow in 0..6) timeDenominator = 1 shl denPow
                            }
                        }
                        0x59 -> { // Key Signature
                            if (metaLen >= 2 && offset + 2 <= bytes.size) {
                                val sf = bytes[offset].toInt() // -7 (flats) to +7 (sharps)
                                val mi = bytes[offset + 1].toInt() // 0 = Major, 1 = Minor
                                keySignature = matchKeySignature(sf, mi)
                            }
                        }
                    }
                    offset = metaEnd
                } else if (status in 0xF0..0xF7) {
                    // SysEx
                    val sysexLenVlq = readVlq(bytes, offset)
                    offset += sysexLenVlq.second + sysexLenVlq.first.toInt()
                } else {
                    val messageType = status and 0xF0
                    val channel = status and 0x0F

                    when (messageType) {
                        0x80 -> { // Note Off
                            if (offset + 1 >= trackEnd) break
                            val note = bytes[offset].toInt() and 0x7F
                            offset += 2
                            val key = channel to note
                            val startTick = activeNotes.remove(key)
                            if (startTick != null) {
                                val dur = (currentTick - startTick).coerceAtLeast(1L)
                                rawNotes.add(RawMidiNote(note, startTick, dur, channel))
                            }
                        }
                        0x90 -> { // Note On
                            if (offset + 1 >= trackEnd) break
                            val note = bytes[offset].toInt() and 0x7F
                            val vel = bytes[offset + 1].toInt() and 0x7F
                            offset += 2
                            val key = channel to note
                            if (vel > 0) {
                                val prevStart = activeNotes.put(key, currentTick)
                                if (prevStart != null) {
                                    val dur = (currentTick - prevStart).coerceAtLeast(1L)
                                    rawNotes.add(RawMidiNote(note, prevStart, dur, channel))
                                }
                            } else {
                                val startTick = activeNotes.remove(key)
                                if (startTick != null) {
                                    val dur = (currentTick - startTick).coerceAtLeast(1L)
                                    rawNotes.add(RawMidiNote(note, startTick, dur, channel))
                                }
                            }
                        }
                        0xA0, 0xB0, 0xE0 -> { // Aftertouch, CC, Pitch Bend: 2 data bytes
                            offset += 2
                        }
                        0xC0, 0xD0 -> { // Program Change, Channel Pressure: 1 data byte
                            offset += 1
                        }
                    }
                }
            }
        }

        if (rawNotes.isEmpty()) return null

        rawNotes.sortBy { it.startTick }

        val beatsPerMeasure = timeNumerator.toFloat() * (4f / timeDenominator.toFloat())
        val ticksPerBeat = division
        val ticksPerMeasure = (beatsPerMeasure * ticksPerBeat).toLong().coerceAtLeast(1L)

        val totalTicks = rawNotes.maxOf { it.startTick + it.durationTicks }
        val numMeasures = ((totalTicks + ticksPerMeasure - 1) / ticksPerMeasure).toInt().coerceAtLeast(1)

        val measureNotes = Array(numMeasures) { mutableListOf<ScoreNote>() }

        rawNotes.forEachIndexed { idx, raw ->
            val mIdx = (raw.startTick / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)
            val tickInMeasure = raw.startTick % ticksPerMeasure
            val posBeats = (tickInMeasure.toFloat() / ticksPerBeat).coerceIn(0f, beatsPerMeasure - 0.05f)
            val durBeats = (raw.durationTicks.toFloat() / ticksPerBeat).coerceIn(0.25f, beatsPerMeasure)

            measureNotes[mIdx].add(
                ScoreNote(
                    id = idx,
                    midiNote = raw.midiNote,
                    position = posBeats,
                    duration = durBeats
                )
            )
        }

        val measures = List(numMeasures) { mIdx ->
            Measure(
                measureIndex = mIdx + 1,
                timeSignatureNumerator = timeNumerator,
                timeSignatureDenominator = timeDenominator,
                keySignature = keySignature,
                notes = measureNotes[mIdx],
                barline = if (mIdx == numMeasures - 1) "final" else "single"
            )
        }

        val cleanTitle = if (detectedTitle.isNotBlank()) {
            detectedTitle
        } else {
            defaultTitle.removeSuffix(".mid").removeSuffix(".midi").replace("_", " ")
        }

        return Score(
            title = cleanTitle,
            tempo = tempoBpm,
            timeSignature = "$timeNumerator/$timeDenominator",
            keySignature = keySignature,
            measures = measures
        )
    }

    private fun matchKeySignature(sf: Int, mi: Int): KeySignature {
        return when (sf) {
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
}
