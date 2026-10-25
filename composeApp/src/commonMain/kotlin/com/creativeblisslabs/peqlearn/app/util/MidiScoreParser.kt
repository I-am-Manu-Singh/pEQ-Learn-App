package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.PedalEvent
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
        val channel: Int,
        val voice: Int
    )

    private data class RawPedalEvent(
        val tick: Long,
        val pedalType: Int,
        val isPressed: Boolean
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
        var initialTempoParsed = false

        val rawNotes = mutableListOf<RawMidiNote>()
        val rawPedals = mutableListOf<RawPedalEvent>()

        // Pass 1: Quick inspection of track names to intelligently map Tracks to Hands / Voices
        val trackVoiceMap = mutableMapOf<Int, Int>()
        val trackInfoList = mutableListOf<com.creativeblisslabs.peqlearn.app.ui.screens.MidiTrackInfo>()
        run {
            var scanOffset = 14
            var noteBearingTrackCount = 0
            for (t in 0 until numTracks) {
                if (scanOffset + 8 > bytes.size) break
                val tLen = read32(bytes, scanOffset + 4)
                scanOffset += 8
                val tEnd = (scanOffset + tLen).coerceAtMost(bytes.size)
                var curT = scanOffset
                var running = 0
                var trackName = ""
                var noteCount = 0
                var trackChannel = 0
                var trackProgram = 0

                while (curT < tEnd) {
                    val vlq = readVlq(bytes, curT)
                    curT += vlq.second
                    if (curT >= tEnd) break
                    var st = bytes[curT].toInt() and 0xFF
                    if ((st and 0x80) != 0) {
                        curT++
                        if (st < 0xF0) running = st
                    } else {
                        st = running
                    }
                    if (st == 0xFF) {
                        if (curT >= tEnd) break
                        val mType = bytes[curT].toInt() and 0xFF
                        curT++
                        val mVlq = readVlq(bytes, curT)
                        curT += mVlq.second
                        val mLen = mVlq.first.toInt()
                        if ((mType == 0x01 || mType == 0x03) && mLen > 0 && curT + mLen <= bytes.size) {
                            val name = bytes.copyOfRange(curT, curT + mLen).decodeToString().trim()
                            if (name.isNotBlank()) trackName = name
                        }
                        curT = (curT + mLen).coerceAtMost(tEnd)
                    } else if (st in 0xF0..0xF7) {
                        val sVlq = readVlq(bytes, curT)
                        curT += sVlq.second + sVlq.first.toInt()
                    } else {
                        val mType = st and 0xF0
                        val ch = st and 0x0F
                        trackChannel = ch
                        if (mType == 0x90) {
                            if (curT + 1 < tEnd && (bytes[curT + 1].toInt() and 0x7F) > 0) {
                                noteCount++
                            }
                            curT += 2
                        } else if (mType in listOf(0x80, 0xA0, 0xB0, 0xE0)) {
                            curT += 2
                        } else if (mType == 0xC0) {
                            if (curT < tEnd) trackProgram = bytes[curT].toInt() and 0x7F
                            curT += 1
                        } else if (mType == 0xD0) {
                            curT += 1
                        }
                    }
                }

                if (noteCount > 0) {
                    val lower = trackName.lowercase()
                    val voice = when {
                        lower.contains("right") || lower.contains("upper") || lower.contains("treble") || lower.contains("rh") -> 0
                        lower.contains("left") || lower.contains("lower") || lower.contains("bass") || lower.contains("lh") -> 1
                        noteBearingTrackCount == 0 -> 0
                        noteBearingTrackCount == 1 -> 1
                        else -> noteBearingTrackCount
                    }
                    trackVoiceMap[t] = voice
                    val cleanTrackName = if (trackName.isNotBlank()) trackName else "Track ${t + 1}"
                    trackInfoList.add(
                        com.creativeblisslabs.peqlearn.app.ui.screens.MidiTrackInfo(
                            trackIndex = t,
                            trackName = cleanTrackName,
                            channel = trackChannel,
                            noteCount = noteCount,
                            program = trackProgram,
                            isEnabled = true
                        )
                    )
                    noteBearingTrackCount++
                }
                scanOffset = tEnd
            }
        }

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
            val trackVoice = trackVoiceMap[t] ?: 0

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
                                val lower = text.lowercase()
                                val isMetadata = lower.contains("piano") ||
                                        lower.contains("track") ||
                                        lower.contains("creator:") ||
                                        lower.contains("lilypond") ||
                                        lower.contains("copyright") ||
                                        lower.contains("produced") ||
                                        lower.contains("upper") ||
                                        lower.contains("lower")
                                if (text.isNotBlank() && !isMetadata) {
                                    detectedTitle = text
                                }
                            }
                        }
                        0x51 -> { // Set Tempo
                            if (metaLen >= 3 && offset + 3 <= bytes.size) {
                                val micros = ((bytes[offset].toInt() and 0xFF) shl 16) or
                                        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                                        (bytes[offset + 2].toInt() and 0xFF)
                                if (micros > 0 && (!initialTempoParsed || currentTick == 0L)) {
                                    tempoBpm = 60_000_000f / micros.toFloat()
                                    initialTempoParsed = true
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
                                if (keySignature == KeySignature.C_MAJOR || currentTick == 0L) {
                                    keySignature = matchKeySignature(sf, mi)
                                }
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
                                rawNotes.add(RawMidiNote(note, startTick, dur, channel, trackVoice))
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
                                    rawNotes.add(RawMidiNote(note, prevStart, dur, channel, trackVoice))
                                }
                            } else {
                                val startTick = activeNotes.remove(key)
                                if (startTick != null) {
                                    val dur = (currentTick - startTick).coerceAtLeast(1L)
                                    rawNotes.add(RawMidiNote(note, startTick, dur, channel, trackVoice))
                                }
                            }
                        }
                        0xB0 -> { // Control Change: Sustain (64), Sostenuto (66), Soft (67)
                            if (offset + 1 >= trackEnd) break
                            val ctrl = bytes[offset].toInt() and 0x7F
                            val cVal = bytes[offset + 1].toInt() and 0x7F
                            offset += 2
                            if (ctrl in listOf(64, 66, 67)) {
                                rawPedals.add(RawPedalEvent(currentTick, ctrl, cVal >= 64))
                            }
                        }
                        0xA0, 0xE0 -> { // Aftertouch, Pitch Bend: 2 data bytes
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
        val measurePedals = Array(numMeasures) { mutableListOf<PedalEvent>() }

        // Route pedal events into corresponding measures
        rawPedals.forEach { p ->
            val mIdx = (p.tick / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)
            val tickInMeasure = p.tick % ticksPerMeasure
            val posBeats = (tickInMeasure.toFloat() / ticksPerBeat).coerceIn(0f, beatsPerMeasure)
            measurePedals[mIdx].add(
                PedalEvent(
                    position = posBeats,
                    pedalType = p.pedalType,
                    isPressed = p.isPressed
                )
            )
        }

        var noteIdCounter = 0
        rawNotes.forEach { raw ->
            val startMeasure = (raw.startTick / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)
            val endTick = raw.startTick + raw.durationTicks
            val endMeasure = ((endTick - 1) / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)

            if (startMeasure == endMeasure) {
                val tickInMeasure = raw.startTick % ticksPerMeasure
                val posBeats = (tickInMeasure.toFloat() / ticksPerBeat).coerceIn(0f, beatsPerMeasure - 0.05f)
                val durBeats = (raw.durationTicks.toFloat() / ticksPerBeat).coerceAtLeast(0.125f)
                measureNotes[startMeasure].add(
                    ScoreNote(
                        id = ++noteIdCounter,
                        midiNote = raw.midiNote,
                        position = posBeats,
                        duration = durBeats,
                        voice = raw.voice
                    )
                )
            } else {
                // Note crosses measure boundaries: split note cleanly with tieStart and tieEnd across measures
                var curNoteTick = raw.startTick
                for (m in startMeasure..endMeasure) {
                    val mStartTick = m * ticksPerMeasure
                    val mEndTick = (m + 1) * ticksPerMeasure
                    val segmentStart = maxOf(curNoteTick, mStartTick)
                    val segmentEnd = minOf(endTick, mEndTick)
                    val segmentDuration = segmentEnd - segmentStart
                    if (segmentDuration > 0) {
                        val tickInM = segmentStart - mStartTick
                        val posBeats = (tickInM.toFloat() / ticksPerBeat).coerceIn(0f, beatsPerMeasure - 0.05f)
                        val durBeats = (segmentDuration.toFloat() / ticksPerBeat).coerceAtLeast(0.125f)
                        val isFirstSeg = (m == startMeasure)
                        val isLastSeg = (m == endMeasure)
                        measureNotes[m].add(
                            ScoreNote(
                                id = ++noteIdCounter,
                                midiNote = raw.midiNote,
                                position = posBeats,
                                duration = durBeats,
                                voice = raw.voice,
                                tieStart = !isLastSeg,
                                tieEnd = !isFirstSeg
                            )
                        )
                    }
                    curNoteTick = segmentEnd
                }
            }
        }

        val measures = List(numMeasures) { mIdx ->
            Measure(
                measureIndex = mIdx + 1,
                timeSignatureNumerator = timeNumerator,
                timeSignatureDenominator = timeDenominator,
                keySignature = keySignature,
                notes = measureNotes[mIdx],
                pedalEvents = measurePedals[mIdx],
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
            measures = measures,
            tracks = trackInfoList
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
