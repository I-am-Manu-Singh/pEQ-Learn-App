package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.Accidental
import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.MidiTrackInfo
import com.creativeblisslabs.peqlearn.app.ui.screens.MusicalArticulation
import com.creativeblisslabs.peqlearn.app.ui.screens.PedalEvent
import com.creativeblisslabs.peqlearn.app.ui.screens.PickDirection
import com.creativeblisslabs.peqlearn.app.ui.screens.RestSymbol
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import com.creativeblisslabs.peqlearn.app.ui.screens.TabArticulation
import kotlin.math.abs
import kotlin.math.floor
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
        0xFF00E676, // Track 3: Low End / Contrabass (Emerald Green)
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
            in 32..39 -> "Low Strings" // Avoids triggering "bass" piano-clef filters on multi-instrument MIDIs
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
        var explicitKeyParsed = false
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
                        0x01 -> {
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
                        0x02 -> {
                            val text = decodeMidiText(bytes, offset, metaLen)
                            if (text.isNotBlank() && detectedComposer.isBlank() && text.length <= 60) {
                                detectedComposer = text.replace(Regex("^(copyright|\\(c\\)|©)\\s*[0-9]*\\s*", RegexOption.IGNORE_CASE), "").trim()
                            }
                        }
                        0x03 -> {
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
                        0x04 -> {
                            val text = decodeMidiText(bytes, offset, metaLen)
                            if (text.isNotBlank() && !rawTrackNames.containsKey(t)) {
                                rawTrackNames[t] = text
                            }
                        }
                        0x05 -> {
                            val lyric = decodeMidiText(bytes, offset, metaLen)
                                .replace("/", "").replace("\\", "").trim()
                            if (lyric.isNotBlank()) {
                                rawTexts.add(RawTextEvent(currentTick, lyric, isLyric = true, isMarker = false))
                            }
                        }
                        0x06, 0x07 -> {
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
                                if (!explicitKeyParsed || currentTick == 0L) {
                                    keySignature = matchKeySignature(sf, mi)
                                    if (sf != 0 || mi != 0) explicitKeyParsed = true
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

        if (!explicitKeyParsed && keySignature == KeySignature.C_MAJOR) {
            keySignature = inferKeySignatureFromNotes(rawNotes)
        }

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
                // Sanitize raw track name so a single track named "Bass" doesn't trick legacy "contains('bass')" piano filters
                // into dropping all other melodic tracks!
                val rawName = rawTrackNames[rawT]?.trim().orEmpty()
                    .replace(Regex("\\bBass\\b", RegexOption.IGNORE_CASE), "Low-End")
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
                        isEnabled = ch != 9 // Disable drum kit track on pitched staff/piano by default
                    )
                )
            }
        }

        val hasMelodicStreams = rawNotes.any { it.channel != 9 }
        // Exclude GM Channel 10 Drum hits from pitched staff notation when melodic tracks exist!
        val pitchedRawNotes = if (hasMelodicStreams) {
            rawNotes.filter { it.channel != 9 }
        } else {
            rawNotes
        }

        val deoverlappedRawNotes = trimOverlappingPolyphonicNotes(pitchedRawNotes, division)

        val beatsPerMeasure = timeNumerator.toFloat() * (4f / timeDenominator.toFloat())
        val ticksPerBeat = division
        val ticksPerMeasure = (beatsPerMeasure * ticksPerBeat).toLong().coerceAtLeast(1L)

        val totalTicks = deoverlappedRawNotes.maxOf { it.startTick + it.durationTicks }
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

        val melodicVelocities = deoverlappedRawNotes.map { it.velocity }
        val meanVelocity = if (melodicVelocities.isNotEmpty()) melodicVelocities.average().toFloat() else 85f

        var noteIdCounter = 0

        deoverlappedRawNotes.forEach { raw ->
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

                val artic = resolveMusicalArticulation(rawDur, qDur, raw.velocity, meanVelocity, false)

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
                        dynamic = dynamicMark,
                        musicalArticulation = artic
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

        // Post-process measure notes to assign guitar string, fret, strumming directions & TAB articulations
        var runningTabBoxCenter = 3.0f
        val standardGuitarTuning = listOf(64, 59, 55, 50, 45, 40)
        measureNotes.forEach { mNotes ->
            val notesByBeat = mNotes.groupBy { it.position }.toSortedMap()
            notesByBeat.forEach { (_, chordNotes) ->
                val isStrum = chordNotes.size >= 2
                val isDownStrum = isStrum && chordNotes.first().midiNote <= chordNotes.last().midiNote
                val melodicPitches = chordNotes.filter { !it.isRest }.map { it.midiNote }.distinct().sorted()

                val (fretMap, nextBoxCenter) = if (melodicPitches.isNotEmpty()) {
                    BiomechanicalPerformersEngine.solveFrettedSliceBiomechanical(
                        pitches = melodicPitches,
                        openStringsMidiHighToLow = standardGuitarTuning,
                        maxFret = 22,
                        minFret = 0,
                        lastHandBoxCenterFret = runningTabBoxCenter
                    )
                } else emptyMap<Int, BiomechanicalPerformersEngine.FrettedNotePlacement>() to runningTabBoxCenter
                runningTabBoxCenter = nextBoxCenter

                chordNotes.forEachIndexed { cIdx, note ->
                    val placement = fretMap[note.midiNote]
                    val (strIdx, fretNum) = if (placement != null) {
                        (placement.stringIndex + 1) to placement.fret
                    } else {
                        computeGuitarStringAndFret(note.midiNote, runningTabBoxCenter)
                    }
                    val updatedPick = if (isStrum && cIdx == 0) (if (isDownStrum) PickDirection.DOWN else PickDirection.UP) else note.pickDirection
                    val updatedArtic = if (note.duration <= 0.12f) TabArticulation.DEAD_NOTE else note.articulation
                    val updatedNote = note.copy(
                        string = strIdx,
                        fret = fretNum,
                        pickDirection = updatedPick,
                        articulation = updatedArtic
                    )
                    val nIdx = mNotes.indexOf(note)
                    if (nIdx >= 0) {
                        mNotes[nIdx] = updatedNote
                    }
                }
            }
        }

        var lastAssignedDynamic = ""
        var lastDynamicMeasureIdx = -10
        var lastChordSymbol = ""
        val useFlatsForChords = keySignature.accidentalsCount > 0 && !keySignature.isSharp

        // Stateful finger tracking across measures so melodic lines step smoothly through fingers 1..5
        val handState = StatefulHandFingerTracker()

        val measures = List(numMeasures) { mIdx ->
            val normalizedMeasureNotes = normalizeAndClampMeasureVoices(
                rawList = measureNotes[mIdx],
                beatsPerMeasure = beatsPerMeasure
            )

            val lyricsForMeasure = measureLyricsByBeat[mIdx]
            val usedLyricPositions = mutableSetOf<Float>()

            val melodicNotesAtPos = normalizedMeasureNotes.groupBy { it.position }.toSortedMap()

            val chordSymByPos = sortedMapOf<Float, String>()
            for ((pos, notesAtBeat) in melodicNotesAtPos) {
                val isMainBeat = abs(pos - round(pos)) <= 0.15f || pos <= 0.26f
                val distinctPitchClasses = notesAtBeat.map { ((it.midiNote % 12) + 12) % 12 }.distinct()
                // Require at least 3 distinct pitch classes (a true triad/7th chord, never a 2-note dyad interval!)
                if (isMainBeat && distinctPitchClasses.size >= 3) {
                    val detected = LiveChordDetector.detect(notesAtBeat.map { it.midiNote })
                    if (detected != null &&
                        detected.chordQuality != "Note" &&
                        !detected.fullName.contains("(") // Reject dyad interval descriptions like "(Whole Tone / 2nd)"
                    ) {
                        val spelledChord = spellChordForKeySignature(detected.fullName, useFlatsForChords)
                        if (spelledChord != lastChordSymbol) {
                            chordSymByPos[pos] = spelledChord
                            lastChordSymbol = spelledChord
                        }
                    }
                }
            }

            val fingerByNoteId = handState.assignMeasureFingerings(normalizedMeasureNotes)

            val cleanedNotes = ArrayList<ScoreNote>(normalizedMeasureNotes.size)
            var lastPosForDyn = -1f
            val attachedChordPositions = mutableSetOf<Float>()

            for (n in normalizedMeasureNotes) {
                val shouldShowDynamic = n.dynamic.isNotEmpty() &&
                    (n.dynamic != lastAssignedDynamic || (mIdx - lastDynamicMeasureIdx >= 8 && n.position <= 0.25f)) &&
                    (cleanedNotes.isEmpty() || n.position != lastPosForDyn)

                val dynToUse = if (shouldShowDynamic) {
                    lastAssignedDynamic = n.dynamic
                    lastDynamicMeasureIdx = mIdx
                    lastPosForDyn = n.position
                    n.dynamic
                } else ""

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

                val chordSym = if (n.position !in attachedChordPositions && chordSymByPos.containsKey(n.position)) {
                    attachedChordPositions.add(n.position)
                    chordSymByPos[n.position].orEmpty()
                } else ""

                cleanedNotes.add(
                    n.copy(
                        dynamic = dynToUse,
                        lyric = matchedLyric.ifBlank { n.lyric },
                        chordSymbol = chordSym.ifBlank { n.chordSymbol },
                        fingering = fingerByNoteId[n.id] ?: n.fingering
                    )
                )
            }

            val synthesizedRests = synthesizeMeasureRests(
                notes = cleanedNotes,
                beatsPerMeasure = beatsPerMeasure
            )

            // Non-overlapping sustain pedal events: each DOWN event releases cleanly before the next DOWN event!
            val effectivePedals = if (measurePedals[mIdx].isNotEmpty()) {
                measurePedals[mIdx].distinctBy { "${it.position}_${it.pedalType}_${it.isPressed}" }
            } else if (chordSymByPos.isNotEmpty()) {
                val chordPositions = chordSymByPos.keys.toList()
                val pedals = mutableListOf<PedalEvent>()
                for (i in chordPositions.indices) {
                    val downPos = chordPositions[i]
                    val nextDownPos = if (i + 1 < chordPositions.size) chordPositions[i + 1] else beatsPerMeasure
                    val upPos = minOf(downPos + 1.75f, nextDownPos - 0.05f).coerceAtLeast(downPos + 0.25f)
                    if (upPos <= beatsPerMeasure - 0.04f) {
                        pedals.add(PedalEvent(position = NumberFormatUtil.round2(downPos), pedalType = 64, isPressed = true))
                        pedals.add(PedalEvent(position = NumberFormatUtil.round2(upPos), pedalType = 64, isPressed = false))
                    }
                }
                pedals
            } else emptyList()

            Measure(
                measureIndex = mIdx + 1,
                timeSignatureNumerator = timeNumerator,
                timeSignatureDenominator = timeDenominator,
                keySignature = keySignature,
                notes = cleanedNotes,
                rests = synthesizedRests,
                pedalEvents = effectivePedals,
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

    /**
     * Converts sharp chord names (e.g. "A♯m", "D♯m/F♯", "G♯6") into flat equivalents ("Bbm", "Ebm/Gb", "Ab6")
     * when the score's KeySignature uses flats.
     */
    private fun spellChordForKeySignature(chordName: String, useFlats: Boolean): String {
        if (!useFlats) return chordName
        return chordName
            .replace("C♯", "Db").replace("C#", "Db")
            .replace("D♯", "Eb").replace("D#", "Eb")
            .replace("F♯", "Gb").replace("F#", "Gb")
            .replace("G♯", "Ab").replace("G#", "Ab")
            .replace("A♯", "Bb").replace("A#", "Bb")
    }

    /**
     * Stateful biomechanical finger tracker across measures.
     * Fixes:
     * 1. Single-note lock on Finger:5 / Finger:1 (now steps smoothly through 1, 2, 3, 4, 5 by interval).
     * 2. Duplicate finger numbers inside the same chord on the same hand/staff.
     */
    private class StatefulHandFingerTracker {
        private var lastLhPitch = 46 // Bb2
        private var lastLhFinger = 3
        private var lastRhPitch = 65 // F4
        private var lastRhFinger = 2

        fun assignMeasureFingerings(notes: List<ScoreNote>): Map<Int, Int> {
            val result = mutableMapOf<Int, Int>()
            val byPos = notes.filter { !it.isRest }.groupBy { it.position }.toSortedMap()

            for ((_, slice) in byPos) {
                // Group by the staff/hand they belong to:
                // Voice 0 is RH, Voice >= 2 is LH, Voice 1 splits at Middle C (60)
                val rhSlice = slice.filter { it.voice == 0 || (it.voice != 2 && it.midiNote >= 60) }.sortedBy { it.midiNote }
                val lhSlice = slice.filter { it !in rhSlice }.sortedBy { it.midiNote }

                if (lhSlice.isNotEmpty()) {
                    assignHandCluster(lhSlice, isRightHand = false, result)
                }
                if (rhSlice.isNotEmpty()) {
                    assignHandCluster(rhSlice, isRightHand = true, result)
                }
            }
            return result
        }

        private fun assignHandCluster(
            sortedAsc: List<ScoreNote>,
            isRightHand: Boolean,
            dest: MutableMap<Int, Int>
        ) {
            val distinctPitches = sortedAsc.map { it.midiNote }.distinct().sorted()
            val count = distinctPitches.size

            val pitchToFinger = mutableMapOf<Int, Int>()
            if (count == 1) {
                val p = distinctPitches[0]
                val prevP = if (isRightHand) lastRhPitch else lastLhPitch
                val prevF = if (isRightHand) lastRhFinger else lastLhFinger
                val delta = p - prevP
                val isBlack = (p % 12) in intArrayOf(1, 3, 6, 8, 10)

                val f = if (delta == 0) {
                    prevF
                } else if (isRightHand) {
                    val step = (delta / 2).coerceIn(-4, 4)
                    val raw = (prevF + (if (step == 0) (if (delta > 0) 1 else -1) else step)).coerceIn(1, 5)
                    if (isBlack && raw == 1) 2 else raw
                } else {
                    // Left hand: ascending pitch moves from 5 -> 1
                    val step = (delta / 2).coerceIn(-4, 4)
                    val raw = (prevF - (if (step == 0) (if (delta > 0) 1 else -1) else step)).coerceIn(1, 5)
                    if (isBlack && raw == 1) 2 else if (isBlack && raw == 5 && p >= 42) 4 else raw
                }
                pitchToFinger[p] = f
                if (isRightHand) {
                    lastRhPitch = p
                    lastRhFinger = f
                } else {
                    lastLhPitch = p
                    lastLhFinger = f
                }
            } else {
                val span = distinctPitches.last() - distinctPitches.first()
                val fingers: List<Int> = if (isRightHand) {
                    when (count) {
                        2 -> when {
                            span <= 2 -> listOf(1, 2)
                            span <= 4 -> listOf(1, 3)
                            span <= 6 -> listOf(1, 4)
                            else -> listOf(1, 5)
                        }
                        3 -> {
                            val g1 = distinctPitches[1] - distinctPitches[0]
                            val g2 = distinctPitches[2] - distinctPitches[1]
                            if (g1 <= 3 && g2 >= 5) listOf(1, 2, 5)
                            else if (g1 >= 5 && g2 <= 3) listOf(1, 4, 5)
                            else listOf(1, 3, 5)
                        }
                        4 -> {
                            val topGap = distinctPitches[3] - distinctPitches[2]
                            if (topGap <= 2) listOf(1, 2, 4, 5) else listOf(1, 2, 3, 5)
                        }
                        else -> {
                            // 5+ notes in a guitar chord: distribute 1..5 monotonically from bottom to top
                            distinctPitches.indices.map { i ->
                                (1 + ((i * 4) / (count - 1).coerceAtLeast(1))).coerceIn(1, 5)
                            }
                        }
                    }
                } else {
                    when (count) {
                        2 -> when {
                            span <= 2 -> listOf(2, 1)
                            span <= 4 -> listOf(3, 1)
                            span <= 6 -> listOf(4, 1)
                            else -> listOf(5, 1)
                        }
                        3 -> {
                            val g1 = distinctPitches[1] - distinctPitches[0]
                            if (g1 >= 5) listOf(5, 2, 1) else listOf(5, 3, 1)
                        }
                        4 -> listOf(5, 3, 2, 1)
                        else -> {
                            distinctPitches.indices.map { i ->
                                (5 - ((i * 4) / (count - 1).coerceAtLeast(1))).coerceIn(1, 5)
                            }
                        }
                    }
                }

                distinctPitches.forEachIndexed { idx, p ->
                    pitchToFinger[p] = fingers[idx]
                }
                if (isRightHand) {
                    lastRhPitch = distinctPitches.last()
                    lastRhFinger = fingers.last()
                } else {
                    lastLhPitch = distinctPitches.first()
                    lastLhFinger = fingers.first()
                }
            }

            for (n in sortedAsc) {
                dest[n.id] = pitchToFinger[n.midiNote] ?: 3
            }
        }
    }

    private fun trimOverlappingPolyphonicNotes(rawNotes: List<RawMidiNote>, division: Float): List<RawMidiNote> {
        val byStream = rawNotes.groupBy { it.rawTrackIdx to it.channel }
        val result = ArrayList<RawMidiNote>(rawNotes.size)
        val onsetClusterTol = (division * 0.06f).toLong().coerceAtLeast(10L)
        val minDurTicks = (division * 0.125f).toLong().coerceAtLeast(20L)

        for ((_, streamNotes) in byStream) {
            val sorted = streamNotes.sortedWith(compareBy({ it.startTick }, { it.midiNote }))
            val clusters = mutableListOf<MutableList<RawMidiNote>>()
            for (n in sorted) {
                if (clusters.isEmpty() || n.startTick - clusters.last().first().startTick > onsetClusterTol) {
                    clusters.add(mutableListOf(n))
                } else {
                    clusters.last().add(n)
                }
            }

            for (cIdx in clusters.indices) {
                val cluster = clusters[cIdx]
                val canonicalStart = cluster.first().startTick
                val nextClusterStart = if (cIdx + 1 < clusters.size) clusters[cIdx + 1].first().startTick else Long.MAX_VALUE
                val maxAllowedDur = (nextClusterStart - canonicalStart).coerceAtLeast(minDurTicks)

                // Unify all notes in the same homophonic chord cluster to the same duration so two notes at
                // the same onset in the same voice don't cause polyphonic voice-audit drift!
                val byPitch = cluster.groupBy { it.midiNote }
                val unifiedClusterDur = byPitch.values
                    .maxOf { samePitch -> samePitch.maxOf { it.durationTicks } }
                    .coerceAtMost(maxAllowedDur)
                    .coerceAtLeast(minDurTicks)

                for ((_, samePitchNotes) in byPitch) {
                    val best = samePitchNotes.maxByOrNull { it.durationTicks }!!
                    result.add(best.copy(startTick = canonicalStart, durationTicks = unifiedClusterDur))
                }
            }
        }
        return result.sortedWith(compareBy({ it.startTick }, { it.midiNote }))
    }

    private fun normalizeAndClampMeasureVoices(
        rawList: List<ScoreNote>,
        beatsPerMeasure: Float
    ): List<ScoreNote> {
        if (rawList.isEmpty()) return emptyList()
        val deduped = rawList.distinctBy { "${it.position}_${it.midiNote}_${it.voice}" }
        val byVoice = deduped.groupBy { it.voice }
        val normalized = ArrayList<ScoreNote>(deduped.size)

        for ((_, vNotes) in byVoice) {
            val byPos = vNotes.groupBy { it.position }.toSortedMap()
            val positions = byPos.keys.toList()

            for (i in positions.indices) {
                val pos = positions[i]
                val chord = byPos[pos] ?: continue
                val nextPos = if (i + 1 < positions.size) positions[i + 1] else beatsPerMeasure
                val maxSpan = (nextPos - pos).coerceAtLeast(0.125f)

                // Use a single unified duration for all notes in the same voice at the same beat position
                val rawChordDur = chord.maxOf { it.duration }
                val clampedDur = minOf(rawChordDur, maxSpan).coerceAtLeast(0.125f)
                val (cleanDur, isDotted, tupletVal) = refineQuantizedDuration(pos, clampedDur, maxSpan)
                val reachesNext = abs((pos + cleanDur) - nextPos) <= 0.05f

                for (n in chord) {
                    normalized.add(
                        n.copy(
                            duration = cleanDur,
                            dotted = isDotted,
                            isDotted = isDotted,
                            tuplet = tupletVal,
                            tieStart = n.tieStart && reachesNext
                        )
                    )
                }
            }
        }

        return normalized.sortedWith(compareBy({ it.position }, { it.voice }, { it.midiNote }))
    }

    private fun synthesizeMeasureRests(
        notes: List<ScoreNote>,
        beatsPerMeasure: Float
    ): List<RestSymbol> {
        if (notes.isEmpty()) {
            return listOf(RestSymbol(position = 0f, duration = beatsPerMeasure, voice = 0))
        }
        val rests = mutableListOf<RestSymbol>()
        val standardRestDurs = floatArrayOf(4.0f, 2.0f, 1.0f, 0.5f, 0.25f, 0.13f)

        fun emitRestsForGap(start: Float, gap: Float, voice: Int) {
            var cur = start
            var rem = gap
            while (rem >= 0.11f) {
                val d = standardRestDurs.firstOrNull { it <= rem + 0.03f } ?: 0.13f
                rests.add(RestSymbol(position = NumberFormatUtil.round2(cur), duration = d, voice = voice))
                cur += d
                rem -= d
            }
        }

        val byVoice = notes.groupBy { it.voice }
        for ((voice, vNotes) in byVoice) {
            val byPos = vNotes.groupBy { it.position }.toSortedMap()
            var cursor = 0f
            for ((pos, chord) in byPos) {
                if (pos > cursor + 0.11f) {
                    emitRestsForGap(cursor, pos - cursor, voice)
                }
                val chordDur = chord.maxOfOrNull { it.duration } ?: 0.25f
                cursor = maxOf(cursor, pos + chordDur)
            }
            if (cursor < beatsPerMeasure - 0.11f) {
                emitRestsForGap(cursor, beatsPerMeasure - cursor, voice)
            }
        }
        return rests
    }

    private fun resolveMusicalArticulation(
        rawDurBeats: Float,
        quantizedDurBeats: Float,
        velocity: Int,
        meanVelocity: Float,
        isDrum: Boolean
    ): MusicalArticulation {
        if (isDrum) return MusicalArticulation.NONE
        return when {
            rawDurBeats <= 0.18f && quantizedDurBeats <= 0.25f -> MusicalArticulation.STACCATO
            velocity >= meanVelocity + 20f && velocity >= 105 -> MusicalArticulation.ACCENT
            rawDurBeats >= quantizedDurBeats * 0.96f && quantizedDurBeats >= 0.5f -> MusicalArticulation.TENUTO
            else -> MusicalArticulation.NONE
        }
    }

    private fun inferKeySignatureFromNotes(rawNotes: List<RawMidiNote>): KeySignature {
        val melodic = rawNotes.filter { it.channel != 9 }
        if (melodic.isEmpty()) return KeySignature.C_MAJOR

        val pcWeights = DoubleArray(12)
        for (n in melodic) {
            val pc = ((n.midiNote % 12) + 12) % 12
            pcWeights[pc] += n.durationTicks.toDouble().coerceAtLeast(60.0)
        }

        val majorProfile = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
        val minorProfile = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

        var bestFifths = 0
        var bestScore = Double.NEGATIVE_INFINITY

        val majorFifthsByTonic = intArrayOf(0, -5, 2, -3, 4, -1, 6, 1, -4, 3, -2, 5)
        val minorFifthsByTonic = intArrayOf(-3, 4, -1, 6, 1, -4, 3, -2, 5, 0, -5, 2)

        for (tonic in 0..11) {
            var majScore = 0.0
            var minScore = 0.0
            for (i in 0..11) {
                val w = pcWeights[(tonic + i) % 12]
                majScore += w * majorProfile[i]
                minScore += w * minorProfile[i]
            }
            if (majScore > bestScore) {
                bestScore = majScore
                bestFifths = majorFifthsByTonic[tonic]
            }
            if (minScore > bestScore) {
                bestScore = minScore
                bestFifths = minorFifthsByTonic[tonic]
            }
        }

        return matchKeySignature(bestFifths, 0)
    }

    private data class QuantizedRhythm(
        val position: Float,
        val duration: Float,
        val isDotted: Boolean,
        val tuplet: Int
    )

    /**
     * Snaps beat positions strictly to the 16th-note (0.25b) or 32nd-note (0.125b) binary grid
     * unless the note is unmistakably part of a triplet run. This eliminates the 0.33b vs 0.38b drift!
     */
    private fun computeGuitarStringAndFret(midiNote: Int, targetBoxCenter: Float = 3.0f): Pair<Int, Int> {
        val tuningMidi = listOf(64, 59, 55, 50, 45, 40) // High e4 (string 1) to Low E2 (string 6)
        var transP = midiNote
        while (transP < 40) {
            transP += 12
        }
        val candidates = tuningMidi.mapIndexedNotNull { sIdx, openMidi ->
            val fret = transP - openMidi
            if (fret in 0..24) (sIdx + 1) to fret else null
        }
        return candidates.minByOrNull { abs(it.second - targetBoxCenter) } ?: (1 to (transP - 64).coerceIn(0, 24))
    }

    private fun quantizeBeatPosition(rawPos: Float): Float {
        val grid16 = round(rawPos * 4f) / 4f
        val grid32 = round(rawPos * 8f) / 8f
        return if (abs(rawPos - grid16) <= 0.065f) {
            NumberFormatUtil.round2(grid16)
        } else {
            NumberFormatUtil.round2(grid32)
        }
    }

    private fun quantizePositionAndDuration(rawPos: Float, rawDur: Float, beatsPerMeasure: Float): QuantizedRhythm {
        val qPos = quantizeBeatPosition(rawPos).coerceIn(0f, (beatsPerMeasure - 0.125f).coerceAtLeast(0f))
        val maxRem = (beatsPerMeasure - qPos).coerceAtLeast(0.125f)
        val (cleanDur, isDotted, tupletVal) = refineQuantizedDuration(qPos, rawDur.coerceAtMost(maxRem), maxRem)
        return QuantizedRhythm(
            position = qPos,
            duration = cleanDur,
            isDotted = isDotted,
            tuplet = tupletVal
        )
    }

    private fun refineQuantizedDuration(qPos: Float, rawDur: Float, maxAllowed: Float = 4.0f): Triple<Float, Boolean, Int> {
        val canonicalDurations = floatArrayOf(
            4.0f, 3.0f, 2.0f, 1.5f, 1.0f, 0.75f, 0.5f, 0.38f, 0.25f, 0.13f
        )
        val cappedRaw = minOf(rawDur, maxAllowed)
        val matchedDur = canonicalDurations
            .filter { it <= maxAllowed + 0.04f }
            .minByOrNull { abs(it - cappedRaw) } ?: 0.25f
        val isDotted = matchedDur == 3.0f || matchedDur == 1.5f || matchedDur == 0.75f || matchedDur == 0.38f
        return Triple(matchedDur, isDotted, 0)
    }

    private object NumberFormatUtil {
        fun round2(v: Float): Float = round(v * 100f) / 100f
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