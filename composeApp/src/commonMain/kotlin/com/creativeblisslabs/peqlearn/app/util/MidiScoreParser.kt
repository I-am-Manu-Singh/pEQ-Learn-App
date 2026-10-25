package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.Accidental
import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.MidiTrackInfo
import com.creativeblisslabs.peqlearn.app.ui.screens.PedalEvent
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import kotlin.math.abs
import kotlin.math.round

/**
 * Robust MIDI file parser that parses actual MIDI events, sequence title, tempo BPM,
 * time signature, key signature, quantizes human timing jitter into clean musical notation grids,
 * detects dotted notes & triplets, and places notes accurately into measures with cross-barline ties.
 */
object MidiScoreParser {

    private data class RawMidiNote(
        val midiNote: Int,
        val startTick: Long,
        val durationTicks: Long,
        val velocity: Int,
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

        val headerLen = read32(bytes, 4).coerceAtLeast(6)
        val numTracks = read16(bytes, 10)
        val rawDivision = read16(bytes, 12)
        val division = if ((rawDivision and 0x8000) != 0) {
            // SMPTE timecode division fallback to 480 PPQ
            480f
        } else {
            rawDivision.toFloat().coerceAtLeast(1f)
        }

        var tempoBpm = 120f
        var timeNumerator = 4
        var timeDenominator = 4
        var keySignature = KeySignature.C_MAJOR
        var detectedTitle = ""
        var initialTempoParsed = false

        val rawNotes = mutableListOf<RawMidiNote>()
        val rawPedals = mutableListOf<RawPedalEvent>()

        val firstTrackOffset = 8 + headerLen

        // Pass 1: Quick inspection of track names to intelligently map Tracks to Hands / Voices
        val trackVoiceMap = mutableMapOf<Int, Int>()
        val trackInfoList = mutableListOf<MidiTrackInfo>()
        var noteBearingTrackCount = 0
        run {
            var scanOffset = firstTrackOffset
            for (t in 0 until numTracks) {
                if (scanOffset + 8 > bytes.size) break
                if (bytes[scanOffset] != 'M'.code.toByte() || bytes[scanOffset + 1] != 'T'.code.toByte() ||
                    bytes[scanOffset + 2] != 'r'.code.toByte() || bytes[scanOffset + 3] != 'k'.code.toByte()
                ) {
                    break
                }
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
                            if (name.isNotBlank() && trackName.isBlank()) trackName = name
                        }
                        curT = (curT + mLen).coerceAtMost(tEnd)
                    } else if (st in 0xF0..0xF7) {
                        val sVlq = readVlq(bytes, curT)
                        curT = (curT + sVlq.second + sVlq.first.toInt()).coerceAtMost(tEnd)
                    } else {
                        val mType = st and 0xF0
                        val ch = st and 0x0F
                        trackChannel = ch
                        when (mType) {
                            0x90 -> {
                                if (curT + 1 < tEnd && (bytes[curT + 1].toInt() and 0x7F) > 0) {
                                    noteCount++
                                }
                                curT += 2
                            }
                            0x80, 0xA0, 0xB0, 0xE0 -> curT += 2
                            0xC0 -> {
                                if (curT < tEnd) trackProgram = bytes[curT].toInt() and 0x7F
                                curT += 1
                            }
                            0xD0 -> curT += 1
                            else -> curT += 1
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
                        MidiTrackInfo(
                            trackIndex = voice,
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

        // Pass 2: Full Event Extraction
        var offset = firstTrackOffset
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
            val activeNotes = mutableMapOf<Pair<Int, Int>, Pair<Long, Int>>() // (channel, note) -> (startTick, velocity)
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
                    if (offset >= trackEnd) break
                    val metaType = bytes[offset].toInt() and 0xFF
                    offset++
                    val metaLenVlq = readVlq(bytes, offset)
                    offset += metaLenVlq.second
                    val metaLen = metaLenVlq.first.toInt()
                    val metaEnd = (offset + metaLen).coerceAtMost(trackEnd)

                    when (metaType) {
                        0x01, 0x03 -> {
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
                        0x51 -> {
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
                        0x58 -> {
                            if (metaLen >= 2 && offset + 2 <= bytes.size) {
                                val num = bytes[offset].toInt() and 0xFF
                                val denPow = bytes[offset + 1].toInt() and 0xFF
                                if (num in 1..32) timeNumerator = num
                                if (denPow in 0..6) timeDenominator = 1 shl denPow
                            }
                        }
                        0x59 -> {
                            if (metaLen >= 2 && offset + 2 <= bytes.size) {
                                val sf = bytes[offset].toInt() // signed byte -7..+7
                                val mi = bytes[offset + 1].toInt() and 0xFF
                                if (keySignature == KeySignature.C_MAJOR || currentTick == 0L) {
                                    keySignature = matchKeySignature(sf, mi)
                                }
                            }
                        }
                    }
                    offset = metaEnd
                } else if (status in 0xF0..0xF7) {
                    val sysexLenVlq = readVlq(bytes, offset)
                    offset = (offset + sysexLenVlq.second + sysexLenVlq.first.toInt()).coerceAtMost(trackEnd)
                } else {
                    val messageType = status and 0xF0
                    val channel = status and 0x0F

                    when (messageType) {
                        0x80 -> {
                            if (offset + 1 >= trackEnd) break
                            val note = bytes[offset].toInt() and 0x7F
                            offset += 2
                            val key = channel to note
                            val startPair = activeNotes.remove(key)
                            if (startPair != null) {
                                val dur = (currentTick - startPair.first).coerceAtLeast(1L)
                                rawNotes.add(RawMidiNote(note, startPair.first, dur, startPair.second, channel, trackVoice))
                            }
                        }
                        0x90 -> {
                            if (offset + 1 >= trackEnd) break
                            val note = bytes[offset].toInt() and 0x7F
                            val vel = bytes[offset + 1].toInt() and 0x7F
                            offset += 2
                            val key = channel to note
                            if (vel > 0) {
                                val prevPair = activeNotes.put(key, currentTick to vel)
                                if (prevPair != null) {
                                    val dur = (currentTick - prevPair.first).coerceAtLeast(1L)
                                    rawNotes.add(RawMidiNote(note, prevPair.first, dur, prevPair.second, channel, trackVoice))
                                }
                            } else {
                                val startPair = activeNotes.remove(key)
                                if (startPair != null) {
                                    val dur = (currentTick - startPair.first).coerceAtLeast(1L)
                                    rawNotes.add(RawMidiNote(note, startPair.first, dur, startPair.second, channel, trackVoice))
                                }
                            }
                        }
                        0xB0 -> {
                            if (offset + 1 >= trackEnd) break
                            val ctrl = bytes[offset].toInt() and 0x7F
                            val cVal = bytes[offset + 1].toInt() and 0x7F
                            offset += 2
                            if (ctrl in listOf(64, 66, 67)) {
                                rawPedals.add(RawPedalEvent(currentTick, ctrl, cVal >= 64))
                            }
                        }
                        0xA0, 0xE0 -> offset += 2
                        0xC0, 0xD0 -> offset += 1
                        else -> offset += 1
                    }
                }
            }

            // Flush any dangling unclosed Note-On events at end of track
            activeNotes.forEach { (key, startPair) ->
                val dur = (currentTick - startPair.first).coerceAtLeast((division * 0.5f).toLong())
                rawNotes.add(RawMidiNote(key.second, startPair.first, dur, startPair.second, key.first, trackVoice))
            }
            offset = trackEnd
        }

        if (rawNotes.isEmpty()) return null

        rawNotes.sortWith(compareBy({ it.startTick }, { it.midiNote }))

        val beatsPerMeasure = timeNumerator.toFloat() * (4f / timeDenominator.toFloat())
        val ticksPerBeat = division
        val ticksPerMeasure = (beatsPerMeasure * ticksPerBeat).toLong().coerceAtLeast(1L)

        val totalTicks = rawNotes.maxOf { it.startTick + it.durationTicks }
        val numMeasures = ((totalTicks + ticksPerMeasure - 1) / ticksPerMeasure).toInt().coerceAtLeast(1)

        val measureNotes = Array(numMeasures) { mutableListOf<ScoreNote>() }
        val measurePedals = Array(numMeasures) { mutableListOf<PedalEvent>() }

        rawPedals.forEach { p ->
            val mIdx = (p.tick / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)
            val tickInMeasure = p.tick % ticksPerMeasure
            val posBeats = quantizeBeatPosition(tickInMeasure.toFloat() / ticksPerBeat).coerceIn(0f, beatsPerMeasure - 0.05f)
            measurePedals[mIdx].add(
                PedalEvent(
                    position = posBeats,
                    pedalType = p.pedalType,
                    isPressed = p.isPressed
                )
            )
        }

        val isSingleTrackScore = noteBearingTrackCount <= 1
        var noteIdCounter = 0

        rawNotes.forEach { raw ->
            val effectiveVoice = if (isSingleTrackScore) {
                if (raw.midiNote < 60) 1 else 0
            } else {
                raw.voice
            }

            val startMeasure = (raw.startTick / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)
            val endTick = raw.startTick + raw.durationTicks
            val endMeasure = ((endTick - 1).coerceAtLeast(raw.startTick) / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)
            val dynamicMark = velocityToDynamic(raw.velocity)
            val accidental = resolveAccidentalForPitch(raw.midiNote, keySignature)

            if (startMeasure == endMeasure) {
                val tickInMeasure = raw.startTick % ticksPerMeasure
                val rawPos = tickInMeasure.toFloat() / ticksPerBeat
                val rawDur = raw.durationTicks.toFloat() / ticksPerBeat
                val (qPos, qDur, isDotted, tupletVal) = quantizePositionAndDuration(rawPos, rawDur, beatsPerMeasure)

                measureNotes[startMeasure].add(
                    ScoreNote(
                        id = ++noteIdCounter,
                        midiNote = raw.midiNote,
                        position = qPos,
                        duration = qDur,
                        voice = effectiveVoice,
                        accidental = accidental,
                        dotted = isDotted,
                        isDotted = isDotted,
                        tuplet = tupletVal,
                        dynamic = dynamicMark
                    )
                )
            } else {
                var curNoteTick = raw.startTick
                for (m in startMeasure..endMeasure) {
                    val mStartTick = m * ticksPerMeasure
                    val mEndTick = (m + 1) * ticksPerMeasure
                    val segmentStart = maxOf(curNoteTick, mStartTick)
                    val segmentEnd = minOf(endTick, mEndTick)
                    val segmentDuration = segmentEnd - segmentStart
                    if (segmentDuration > 0) {
                        val tickInM = segmentStart - mStartTick
                        val rawPos = tickInM.toFloat() / ticksPerBeat
                        val rawDur = segmentDuration.toFloat() / ticksPerBeat
                        val (qPos, qDur, isDotted, tupletVal) = quantizePositionAndDuration(rawPos, rawDur, beatsPerMeasure)
                        val isFirstSeg = (m == startMeasure)
                        val isLastSeg = (m == endMeasure)
                        measureNotes[m].add(
                            ScoreNote(
                                id = ++noteIdCounter,
                                midiNote = raw.midiNote,
                                position = qPos,
                                duration = qDur,
                                voice = effectiveVoice,
                                accidental = accidental,
                                dotted = isDotted,
                                isDotted = isDotted,
                                tuplet = tupletVal,
                                tieStart = !isLastSeg,
                                tieEnd = !isFirstSeg,
                                dynamic = if (isFirstSeg) dynamicMark else ""
                            )
                        )
                    }
                    curNoteTick = segmentEnd
                }
            }
        }

        // Deduplicate any identical (position, midiNote, voice) notes produced by quantization and assign intra-measure ties if needed
        val measures = List(numMeasures) { mIdx ->
            val cleanedNotes = measureNotes[mIdx]
                .distinctBy { "${it.position}_${it.midiNote}_${it.voice}" }
                .sortedWith(compareBy({ it.position }, { it.voice }, { it.midiNote }))

            Measure(
                measureIndex = mIdx + 1,
                timeSignatureNumerator = timeNumerator,
                timeSignatureDenominator = timeDenominator,
                keySignature = keySignature,
                notes = cleanedNotes,
                pedalEvents = measurePedals[mIdx].distinctBy { "${it.position}_${it.pedalType}_${it.isPressed}" },
                barline = if (mIdx == numMeasures - 1) "final" else "single"
            )
        }

        val cleanTitle = if (detectedTitle.isNotBlank()) {
            detectedTitle
        } else {
            defaultTitle.substringBeforeLast(".").replace("_", " ")
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

    private data class QuantizedRhythm(
        val position: Float,
        val duration: Float,
        val isDotted: Boolean,
        val tuplet: Int
    )

    private fun quantizeBeatPosition(rawPos: Float): Float {
        val grid16 = round(rawPos * 4f) / 4f
        val gridTriplet = round(rawPos * 3f) / 3f
        val grid32 = round(rawPos * 8f) / 8f
        return when {
            abs(rawPos - grid16) <= 0.045f -> grid16
            abs(rawPos - gridTriplet) <= 0.035f -> gridTriplet
            else -> grid32
        }
    }

    private fun quantizePositionAndDuration(rawPos: Float, rawDur: Float, beatsPerMeasure: Float): QuantizedRhythm {
        val qPos = quantizeBeatPosition(rawPos).coerceIn(0f, (beatsPerMeasure - 0.125f).coerceAtLeast(0f))

        // Check triplet duration (1/3 beat = 0.333f, 2/3 beat = 0.667f)
        val gridTriplet = round(rawPos * 3f) / 3f
        val isTripletPos = abs(rawPos - gridTriplet) <= 0.035f && abs(gridTriplet - (round(gridTriplet * 4f) / 4f)) > 0.04f
        if (isTripletPos || abs(rawDur - 0.333f) <= 0.045f) {
            return QuantizedRhythm(
                position = qPos,
                duration = 0.333f,
                isDotted = false,
                tuplet = 3
            )
        }

        // Standard + Dotted durations in quarter-note beats
        val canonicalDurations = floatArrayOf(
            8.0f, 6.0f, 4.0f, 3.0f, 2.0f, 1.5f, 1.0f, 0.75f, 0.5f, 0.375f, 0.25f, 0.125f
        )
        val matchedDur = canonicalDurations.minByOrNull { abs(it - rawDur) } ?: 0.5f
        val isDotted = matchedDur == 6.0f || matchedDur == 3.0f || matchedDur == 1.5f || matchedDur == 0.75f || matchedDur == 0.375f

        return QuantizedRhythm(
            position = qPos,
            duration = matchedDur.coerceAtMost((beatsPerMeasure - qPos).coerceAtLeast(0.125f)),
            isDotted = isDotted,
            tuplet = 0
        )
    }

    internal fun resolveAccidentalForPitch(midiNote: Int, keySignature: KeySignature): Accidental {
        val pc = ((midiNote % 12) + 12) % 12
        val isBlackKey = pc in intArrayOf(1, 3, 6, 8, 10)
        if (!isBlackKey) return Accidental.NATURAL
        return if (keySignature.accidentalsCount > 0 && !keySignature.isSharp) {
            Accidental.FLAT
        } else {
            Accidental.SHARP
        }
    }

    private fun velocityToDynamic(velocity: Int): String {
        return when {
            velocity <= 0 -> ""
            velocity < 35 -> "pp"
            velocity < 55 -> "p"
            velocity < 75 -> "mp"
            velocity < 95 -> "mf"
            velocity < 112 -> "f"
            else -> "ff"
        }
    }

    internal fun matchKeySignature(sf: Int, mi: Int = 0): KeySignature {
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