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

object MidiScoreParser {

    private data class RawMidiNote(
        val midiNote: Int,
        val startTick: Long,
        val durationTicks: Long,
        val velocity: Int,
        val channel: Int,
        val rawTrackIdx: Int,
        val program: Int
    )

    private data class RawPedalEvent(
        val tick: Long,
        val pedalType: Int,
        val isPressed: Boolean
    )

    private data class RawTextEvent(
        val tick: Long,
        val text: String,
        val isLyric: Boolean,
        val isMarker: Boolean
    )

    val TRACK_COLOR_PALETTE_HEX = longArrayOf(
        0xFF4A90E2, // Track 0: Right Hand / Lead (Electric Blue)
        0xFF6C63FF, // Track 1: Left Hand / Accompaniment (Royal Indigo)
        0xFFFF9800, // Track 2: Guitar / Sitar / Plucked (Amber Gold)
        0xFF00E676, // Track 3: Bass / Synth Bass (Emerald Green)
        0xFFE040FB, // Track 4: Violin / Strings / Ensemble (Orchid Magenta)
        0xFF00E5FF, // Track 5: Flute / Winds / Brass (Cyan)
        0xFFFF5252, // Track 6: Drums / Percussion Ch 10 (Crimson Flame)
        0xFFFFD700, // Track 7: Harmonium / Organ / Keys 2 (Sunburst Yellow)
        0xFF26A69A, // Track 8: Pad / Choir (Teal)
        0xFFFF7043, // Track 9: Synth Lead (Coral)
        0xFFAB47BC, // Track 10: Ethnic / World (Purple)
        0xFF78909C  // Track 11: Aux / FX (Slate)
    )

    fun getGeneralMidiInstrumentName(program: Int, channel: Int): String {
        if (channel == 9) return "Drum Kit (Percussion)"
        return when (program.coerceIn(0, 127)) {
            in 0..7 -> "Acoustic Piano"
            in 8..15 -> "Chromatic Percussion"
            in 16..23 -> "Harmonium / Organ"
            in 24..31 -> "Guitar"
            in 32..39 -> "Bass"
            in 40..47 -> "Violin / Strings"
            in 48..55 -> "String Ensemble / Choir"
            in 56..63 -> "Brass Section"
            in 64..71 -> "Sax / Reed"
            in 72..79 -> "Flute / Pipe"
            in 80..87 -> "Synth Lead"
            in 88..95 -> "Synth Pad"
            in 96..103 -> "Synth Effects"
            104 -> "Sitar"
            in 105..111 -> "World / Ethnic"
            in 112..119 -> "Melodic Percussion / Tabla"
            else -> "Sound Effects"
        }
    }

    fun getGeneralMidiEmoji(program: Int, channel: Int): String {
        if (channel == 9) return "🥁"
        return when (program.coerceIn(0, 127)) {
            in 0..7 -> "🎹"
            in 16..23 -> "🪗"
            in 24..31 -> "🎸"
            in 32..39 -> "🎸"
            in 40..55 -> "🎻"
            in 56..71 -> "🎷"
            in 72..79 -> "🪈"
            104 -> "🪕"
            in 112..119 -> "🥁"
            else -> "🎵"
        }
    }

    private fun decodeMidiText(bytes: ByteArray, start: Int, len: Int): String {
        if (len <= 0 || start + len > bytes.size) return ""
        val slice = bytes.copyOfRange(start, start + len)
        val raw = try {
            val utf8 = slice.decodeToString()
            if (utf8.contains('\uFFFD')) String(slice, Charsets.ISO_8859_1) else utf8
        } catch (_: Exception) {
            String(slice, Charsets.ISO_8859_1)
        }
        return raw.replace("\r", " ").replace("\n", " ").replace("\u0000", "").trim()
    }

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
        val division = if ((rawDivision and 0x8000) != 0) 480f else rawDivision.toFloat().coerceAtLeast(1f)

        var tempoBpm = 120f
        var timeNumerator = 4
        var timeDenominator = 4
        var keySignature = KeySignature.C_MAJOR
        var detectedTitle = ""
        var detectedComposer = ""
        var initialTempoParsed = false

        val rawNotes = mutableListOf<RawMidiNote>()
        val rawPedals = mutableListOf<RawPedalEvent>()
        val rawTexts = mutableListOf<RawTextEvent>()
        val rawTrackNames = mutableMapOf<Int, String>()
        val channelPrograms = IntArray(16) { 0 }

        var offset = 8 + headerLen
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
            val activeNotes = mutableMapOf<Pair<Int, Int>, Triple<Long, Int, Int>>()

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
                        0x01 -> { // Text Event (often composer, tempo text, or lyrics at tick > 0)
                            val text = decodeMidiText(bytes, offset, metaLen)
                            if (text.isNotBlank()) {
                                val lower = text.lowercase()
                                if (currentTick == 0L && (lower.startsWith("by ") || lower.startsWith("composer:") || lower.startsWith("arranged by"))) {
                                    if (detectedComposer.isBlank()) {
                                        detectedComposer = text.replace(Regex("^(by|composer:|arranged by)\\s*", RegexOption.IGNORE_CASE), "").trim()
                                    }
                                } else if (currentTick > 0L && text.length <= 24 && !lower.contains("http") && !lower.contains("midi")) {
                                    rawTexts.add(RawTextEvent(currentTick, text, isLyric = true, isMarker = false))
                                }
                            }
                        }
                        0x02 -> { // Copyright / Composer Notice
                            val text = decodeMidiText(bytes, offset, metaLen)
                            if (text.isNotBlank() && detectedComposer.isBlank() && text.length <= 60) {
                                detectedComposer = text.replace(Regex("^(copyright|\\(c\\)|©)\\s*[0-9]*\\s*", RegexOption.IGNORE_CASE), "").trim()
                            }
                        }
                        0x03 -> { // Sequence / Track Name
                            val text = decodeMidiText(bytes, offset, metaLen)
                            if (text.isNotBlank()) {
                                if (!rawTrackNames.containsKey(t)) {
                                    rawTrackNames[t] = text
                                }
                                val lower = text.lowercase()
                                val isGenericPartName = lower in setOf(
                                    "piano", "acoustic grand piano", "track 1", "track 2", "track 3",
                                    "upper", "lower", "right", "left", "rh", "lh", "right hand", "left hand",
                                    "guitar", "bass", "drums", "strings", "melody", "vocal", "voice"
                                ) || lower.startsWith("creator:") || lower.startsWith("lilypond") || lower.startsWith("untitled")
                                if (detectedTitle.isBlank() && !isGenericPartName) {
                                    detectedTitle = text
                                }
                            }
                        }
                        0x04 -> { // Instrument Name
                            val text = decodeMidiText(bytes, offset, metaLen)
                            if (text.isNotBlank() && !rawTrackNames.containsKey(t)) {
                                rawTrackNames[t] = text
                            }
                        }
                        0x05 -> { // Official MIDI Lyric Event!
                            val lyric = decodeMidiText(bytes, offset, metaLen)
                                .replace("/", "").replace("\\", "").trim()
                            if (lyric.isNotBlank()) {
                                rawTexts.add(RawTextEvent(currentTick, lyric, isLyric = true, isMarker = false))
                            }
                        }
                        0x06, 0x07 -> { // Marker / Cue Point (e.g. Intro, Verse, Chorus, Bridge, Coda)
                            val marker = decodeMidiText(bytes, offset, metaLen)
                            if (marker.isNotBlank() && marker.length <= 28) {
                                rawTexts.add(RawTextEvent(currentTick, marker, isLyric = false, isMarker = true))
                            }
                        }
                        0x51 -> {
                            if (metaLen >= 3 && offset + 3 <= bytes.size) {
                                val micros = ((bytes[offset].toInt() and 0xFF) shl 16) or
                                        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                                        (bytes[offset + 2].toInt() and 0xFF)
                                if (micros > 0 && (!initialTempoParsed || currentTick == 0L)) {
                                    tempoBpm = (60_000_000f / micros.toFloat()).coerceIn(24f, 280f)
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
                                val sf = bytes[offset].toInt()
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
                            val startTriple = activeNotes.remove(key)
                            if (startTriple != null) {
                                val dur = (currentTick - startTriple.first).coerceAtLeast(1L)
                                rawNotes.add(RawMidiNote(note, startTriple.first, dur, startTriple.second, channel, t, startTriple.third))
                            }
                        }
                        0x90 -> {
                            if (offset + 1 >= trackEnd) break
                            val note = bytes[offset].toInt() and 0x7F
                            val vel = bytes[offset + 1].toInt() and 0x7F
                            offset += 2
                            val key = channel to note
                            if (vel > 0) {
                                val prevTriple = activeNotes.put(key, Triple(currentTick, vel, channelPrograms[channel]))
                                if (prevTriple != null) {
                                    val dur = (currentTick - prevTriple.first).coerceAtLeast(1L)
                                    rawNotes.add(RawMidiNote(note, prevTriple.first, dur, prevTriple.second, channel, t, prevTriple.third))
                                }
                            } else {
                                val startTriple = activeNotes.remove(key)
                                if (startTriple != null) {
                                    val dur = (currentTick - startTriple.first).coerceAtLeast(1L)
                                    rawNotes.add(RawMidiNote(note, startTriple.first, dur, startTriple.second, channel, t, startTriple.third))
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
                        0xC0 -> {
                            if (offset < trackEnd) {
                                channelPrograms[channel] = bytes[offset].toInt() and 0x7F
                            }
                            offset += 1
                        }
                        0xA0, 0xE0 -> offset += 2
                        0xD0 -> offset += 1
                        else -> offset += 1
                    }
                }
            }

            activeNotes.forEach { (key, startTriple) ->
                val dur = (currentTick - startTriple.first).coerceAtLeast((division * 0.5f).toLong())
                rawNotes.add(RawMidiNote(key.second, startTriple.first, dur, startTriple.second, key.first, t, startTriple.third))
            }
            offset = trackEnd
        }

        if (rawNotes.isEmpty()) return null

        val groupedStreams = rawNotes.groupBy { it.rawTrackIdx to it.channel }
        val trackInfoList = mutableListOf<MidiTrackInfo>()
        val streamToVoiceIndex = mutableMapOf<Pair<Int, Int>, Int>()

        if (groupedStreams.size == 1) {
            val (onlyKey, notesInStream) = groupedStreams.entries.first()
            val ch = onlyKey.second
            val prog = notesInStream.firstOrNull()?.program ?: 0
            val instLabel = getGeneralMidiInstrumentName(prog, ch)
            val emoji = getGeneralMidiEmoji(prog, ch)

            if (ch == 9) {
                streamToVoiceIndex[onlyKey] = 0
                trackInfoList.add(
                    MidiTrackInfo(
                        trackIndex = 0,
                        trackName = "$emoji Drum Kit (Ch 10)",
                        channel = 9,
                        noteCount = notesInStream.size,
                        program = prog,
                        isEnabled = true
                    )
                )
            } else {
                val rhNotesCount = notesInStream.count { it.midiNote >= 60 }
                val lhNotesCount = notesInStream.count { it.midiNote < 60 }
                if (rhNotesCount > 0) {
                    trackInfoList.add(
                        MidiTrackInfo(
                            trackIndex = 0,
                            trackName = "$emoji $instLabel (Right Hand / Treble)",
                            channel = ch,
                            noteCount = rhNotesCount,
                            program = prog,
                            isEnabled = true
                        )
                    )
                }
                if (lhNotesCount > 0) {
                    trackInfoList.add(
                        MidiTrackInfo(
                            trackIndex = 1,
                            trackName = "$emoji $instLabel (Left Hand / Bass)",
                            channel = ch,
                            noteCount = lhNotesCount,
                            program = prog,
                            isEnabled = true
                        )
                    )
                }
                streamToVoiceIndex[onlyKey] = -1
            }
        } else {
            var nextVoiceIdx = 0
            val sortedStreams = groupedStreams.entries.sortedWith(
                compareBy({ it.key.second == 9 }, { it.key.first }, { it.key.second })
            )
            for ((streamKey, notesInStream) in sortedStreams) {
                val (rawT, ch) = streamKey
                val prog = notesInStream.groupingBy { it.program }.eachCount().maxByOrNull { it.value }?.key ?: 0
                val rawName = rawTrackNames[rawT]?.trim().orEmpty()
                val gmName = getGeneralMidiInstrumentName(prog, ch)
                val emoji = getGeneralMidiEmoji(prog, ch)

                val displayTrackName = when {
                    ch == 9 -> "$emoji ${if (rawName.isNotBlank() && !rawName.equals("Track 1", true)) "$rawName (Drums)" else "Drum Kit (Ch 10)"}"
                    rawName.isNotBlank() && !rawName.startsWith("Track", ignoreCase = true) -> "$emoji $rawName ($gmName)"
                    else -> "$emoji $gmName (Ch ${ch + 1})"
                }

                val assignedIdx = nextVoiceIdx++
                streamToVoiceIndex[streamKey] = assignedIdx
                trackInfoList.add(
                    MidiTrackInfo(
                        trackIndex = assignedIdx,
                        trackName = displayTrackName,
                        channel = ch,
                        noteCount = notesInStream.size,
                        program = prog,
                        isEnabled = true
                    )
                )
            }
        }

        rawNotes.sortWith(compareBy({ it.startTick }, { it.midiNote }))

        val beatsPerMeasure = timeNumerator.toFloat() * (4f / timeDenominator.toFloat())
        val ticksPerBeat = division
        val ticksPerMeasure = (beatsPerMeasure * ticksPerBeat).toLong().coerceAtLeast(1L)

        val totalTicks = rawNotes.maxOf { it.startTick + it.durationTicks }
        val numMeasures = ((totalTicks + ticksPerMeasure - 1) / ticksPerMeasure).toInt().coerceAtLeast(1)

        val measureNotes = Array(numMeasures) { mutableListOf<ScoreNote>() }
        val measurePedals = Array(numMeasures) { mutableListOf<PedalEvent>() }
        val measureMarkers = Array(numMeasures) { "" }
        val measureLyricsByBeat = Array(numMeasures) { mutableMapOf<Float, String>() }

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

        rawTexts.forEach { txt ->
            val mIdx = (txt.tick / ticksPerMeasure).toInt().coerceIn(0, numMeasures - 1)
            if (txt.isMarker) {
                if (measureMarkers[mIdx].isBlank()) {
                    measureMarkers[mIdx] = txt.text
                }
            } else if (txt.isLyric) {
                val tickInMeasure = txt.tick % ticksPerMeasure
                val posBeats = quantizeBeatPosition(tickInMeasure.toFloat() / ticksPerBeat).coerceIn(0f, beatsPerMeasure - 0.05f)
                val existing = measureLyricsByBeat[mIdx][posBeats]
                measureLyricsByBeat[mIdx][posBeats] = if (existing.isNullOrBlank()) txt.text else "$existing ${txt.text}"
            }
        }

        var noteIdCounter = 0

        rawNotes.forEach { raw ->
            val streamVoice = streamToVoiceIndex[raw.rawTrackIdx to raw.channel] ?: 0
            val effectiveVoice = if (streamVoice == -1) {
                if (raw.midiNote < 60) 1 else 0
            } else {
                streamVoice
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

        var lastAssignedDynamic = ""
        val measures = List(numMeasures) { mIdx ->
            val rawList = measureNotes[mIdx]
                .distinctBy { "${it.position}_${it.midiNote}_${it.voice}" }
                .sortedWith(compareBy({ it.position }, { it.voice }, { it.midiNote }))

            val lyricsForMeasure = measureLyricsByBeat[mIdx]
            val usedLyricPositions = mutableSetOf<Float>()

            val cleanedNotes = ArrayList<ScoreNote>(rawList.size)
            var lastPos = -1f
            for (n in rawList) {
                val shouldShowDynamic = n.dynamic.isNotEmpty() &&
                        n.dynamic != lastAssignedDynamic &&
                        (cleanedNotes.isEmpty() || n.position != lastPos)
                val dynToUse = if (shouldShowDynamic) {
                    lastAssignedDynamic = n.dynamic
                    lastPos = n.position
                    n.dynamic
                } else ""

                // Attach lyric to the first melodic note at (or within 0.25 beats of) the lyric timestamp
                var matchedLyric = ""
                if (lyricsForMeasure.isNotEmpty()) {
                    val closestBeat = lyricsForMeasure.keys
                        .filter { it !in usedLyricPositions && abs(it - n.position) <= 0.26f }
                        .minByOrNull { abs(it - n.position) }
                    if (closestBeat != null) {
                        matchedLyric = lyricsForMeasure[closestBeat].orEmpty()
                        usedLyricPositions.add(closestBeat)
                    }
                }

                cleanedNotes.add(
                    n.copy(
                        dynamic = dynToUse,
                        lyric = matchedLyric.ifBlank { n.lyric }
                    )
                )
            }

            Measure(
                measureIndex = mIdx + 1,
                timeSignatureNumerator = timeNumerator,
                timeSignatureDenominator = timeDenominator,
                keySignature = keySignature,
                notes = cleanedNotes,
                pedalEvents = measurePedals[mIdx].distinctBy { "${it.position}_${it.pedalType}_${it.isPressed}" },
                rehearsalMark = measureMarkers[mIdx],
                barline = if (mIdx == numMeasures - 1) "final" else "single"
            )
        }

        val cleanTitle = if (detectedTitle.isNotBlank()) {
            detectedTitle
        } else {
            defaultTitle.substringAfterLast("/").substringBeforeLast(".").replace("_", " ")
        }

        return Score(
            title = cleanTitle,
            composer = detectedComposer,
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