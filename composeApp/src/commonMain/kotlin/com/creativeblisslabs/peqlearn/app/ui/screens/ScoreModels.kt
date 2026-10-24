package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlin.math.pow

@Serializable
enum class NoteDuration(val beats: Float) {
    LONGA(16.0f),
    BREVE(8.0f),
    WHOLE(4.0f),
    HALF(2.0f),
    QUARTER(1.0f),
    EIGHTH(0.5f),
    SIXTEENTH(0.25f),
    THIRTY_SECOND(0.125f)
}

@Serializable
enum class MusicalArticulation {
    NONE, STACCATO, ACCENT, TENUTO, MARCATO
}

@Serializable
enum class TabArticulation {
    NONE,
    HAMMER_ON,
    PULL_OFF,
    SLIDE_UP,
    SLIDE_DOWN,
    TAP,
    RELEASE,
    DEAD_NOTE
}

@Serializable
data class BarreInfo(
    val fret: Int = 0,
    val startString: Int = 0,
    val endString: Int = 5
)

@Serializable
data class BendInfo(
    val stringIndex: Int = 0,
    val fret: Int = 0,
    val bendSteps: Float = 1.0f // 1.0 = full bend, 0.5 = half bend
)

@Serializable
data class BeamInfo(
    val primaryBeam: Boolean = true,
    val secondaryBeams: Boolean = false,
    val stemDirectionUp: Boolean = true,
    val beamStartY: Float = 0f,
    val beamEndY: Float = 0f,
    val startIndex: Int = 0,
    val count: Int = 0
)

@Serializable
data class PedalEvent(
    val position: Float = 0.0f,
    val pedalType: Int = 64, // 64: Sustain / Damper, 66: Sostenuto, 67: Soft
    val isPressed: Boolean = true
)

@Serializable
data class RestSymbol(
    val position: Float = 0.0f,
    val duration: Float = 1.0f,
    val voice: Int = 0,
    val symbolChar: String = "𝄽"
)

@Serializable
data class ScoreNote(
    val id: Int = 0,
    val midiNote: Int = 60,
    val position: Float = 0.0f,
    val duration: Float = 1.0f,
    val string: Int? = null,
    val fret: Int? = null,
    val voice: Int = 0,
    val stemUp: Boolean? = null,
    val beamGroup: Int = 0,
    val accidental: Accidental = Accidental.NATURAL,
    val courtesyAccidental: Boolean = false,
    val dotted: Boolean = false,
    val tuplet: Int = 0,
    val grace: Boolean = false,
    val tieStart: Boolean = false,
    val tieEnd: Boolean = false,
    val slurStart: Boolean = false,
    val slurEnd: Boolean = false,
    val crossStaff: Boolean = false,
    val dynamic: String = "",
    val chordSymbol: String = "",
    val lyric: String = "",
    val fingering: Int? = null,
    val articulation: TabArticulation = TabArticulation.NONE,
    val musicalArticulation: MusicalArticulation = MusicalArticulation.NONE,
    val bend: BendInfo? = null,
    val harmonic: HarmonicType? = null,
    val palmMute: Boolean = false,
    val letRing: Boolean = false,
    val vibrato: Boolean = false,
    val barre: BarreInfo? = null,
    // Convenience backward-compatibility properties
    val isRest: Boolean = false,
    val isDotted: Boolean = dotted,
    val stringNum: Int = string ?: 0,
    val fretNum: Int = fret ?: 0,
    val pitch: Int = midiNote,
    val durationFraction: Float = (duration / 4f).coerceAtLeast(0.05f)
) {
    // Secondary constructor for existing helper invocations
    constructor(
        midiNote: Int,
        duration: NoteDuration = NoteDuration.QUARTER,
        isRest: Boolean = false,
        isDotted: Boolean = false,
        stringNum: Int = 0,
        fretNum: Int = 0,
        lyric: String? = null
    ) : this(
        id = 0,
        midiNote = midiNote,
        position = 0.0f,
        duration = duration.beats,
        string = stringNum,
        fret = fretNum,
        lyric = lyric ?: "",
        isRest = isRest,
        isDotted = isDotted,
        stringNum = stringNum,
        fretNum = fretNum
    )
}

@Serializable
data class Measure(
    val notes: List<ScoreNote> = emptyList(),
    val rests: List<RestSymbol> = emptyList(),
    val pedalEvents: List<PedalEvent> = emptyList(),
    val repeatStart: Boolean = false,
    val repeatEnd: Boolean = false,
    val endingNumber: Int? = null,
    val rehearsalMark: String = "",
    val segno: Boolean = false,
    val coda: Boolean = false,
    val fine: Boolean = false,
    val daCapo: Boolean = false,
    val dalSegno: Boolean = false,
    val toCoda: Boolean = false,
    val barline: String = "single",
    val measureIndex: Int = 1,
    val timeSignatureNumerator: Int = 4,
    val timeSignatureDenominator: Int = 4,
    val keySignature: KeySignature = KeySignature.C_MAJOR
)

@Serializable
data class Score(
    val title: String = "Untitled Study",
    val composer: String = "pEQ Learn Studio",
    val lyricist: String = "",
    val copyright: String = "",
    val tempo: Float = 120.0f,
    val tempoText: String = "Moderato",
    val keySignature: KeySignature = KeySignature.C_MAJOR,
    val timeSignature: String = "4/4",
    val pickupMeasure: Boolean = false,
    val measures: List<Measure> = emptyList(),
    val clef: String = "TREBLE",
    val pdfUri: String? = null,
    val isPdfScore: Boolean = false
) {
    val bpm: Int get() = tempo.toInt()
}

/**
 * Generates an ultra-detailed, human-readable forensic representation of the parsed score.
 * Exhaustively analyzes every dimension:
 * - Score & global layout metadata
 * - Measure timing budgets, total duration vs nominal capacity, pickup offsets, barlines
 * - Note-by-note audit: ID, pitch name, scientific octave, MIDI number, frequency (Hz),
 *   clef assignment (Treble / Bass), hand, voice, duration in beats & ticks (assuming 480 PPQ),
 *   beat position, fraction, stem direction, accidental & courtesy state, tuplet, grace,
 *   ties, slurs, dynamics, chord voicings, guitar tab fret/string, articulations, bends, harmonics
 * - Distance analysis: Inter-note delta (beats), horizontal timing density, polyphonic overlaps
 * - Rhythmic beamed-cluster analysis & beamgroup assignment
 * - Rests & silence gaps
 * - Pedal controller events
 */
fun Score.dumpAsText(): String {
    val sb = StringBuilder()
    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    fun midiToName(midi: Int): String {
        if (midi < 0 || midi > 127) return "N/A"
        val name = noteNames[midi % 12]
        val oct = (midi / 12) - 1
        return "$name$oct"
    }
    fun midiToFreq(midi: Int): Float {
        return (440.0 * 2.0.pow((midi - 69).toDouble() / 12.0)).toFloat()
    }

    val totalNotes = measures.sumOf { it.notes.size }
    val totalRests = measures.sumOf { it.rests.size }
    val totalPedals = measures.sumOf { it.pedalEvents.size }
    val minMidi = measures.flatMap { it.notes }.map { it.midiNote }.minOrNull() ?: 60
    val maxMidi = measures.flatMap { it.notes }.map { it.midiNote }.maxOrNull() ?: 60

    sb.appendLine("========================================================================================================================")
    sb.appendLine("                        pEQ-LEARN SCORE & NOTATION PARSER ULTRA-FORENSIC AUDIT DUMP")
    sb.appendLine("========================================================================================================================")
    sb.appendLine("SCORE METADATA:")
    sb.appendLine("  • Title              : $title")
    sb.appendLine("  • Composer           : $composer")
    if (lyricist.isNotEmpty()) sb.appendLine("  • Lyricist           : $lyricist")
    if (copyright.isNotEmpty()) sb.appendLine("  • Copyright          : $copyright")
    sb.appendLine("  • Primary Clef       : $clef")
    sb.appendLine("  • Key Signature      : ${keySignature.keyName} (${keySignature.accidentalsCount} ${if (keySignature.isSharp) "sharps" else "flats"})")
    sb.appendLine("  • Global Time Sig    : $timeSignature (Pickup measure: $pickupMeasure)")
    sb.appendLine("  • Global Tempo       : $tempo BPM ($tempoText)")
    sb.appendLine("  • Pitch Extents      : Min MIDI $minMidi (${midiToName(minMidi)}) -> Max MIDI $maxMidi (${midiToName(maxMidi)})")
    sb.appendLine("  • Total Measures     : ${measures.size}")
    sb.appendLine("  • Total Notes        : $totalNotes")
    sb.appendLine("  • Total Rests        : $totalRests")
    sb.appendLine("  • Total Pedal Events : $totalPedals")
    sb.appendLine("========================================================================================================================")
    sb.appendLine()

    measures.forEachIndexed { mIdx, measure ->
        val mNum = if (measure.measureIndex > 0) measure.measureIndex else mIdx + 1
        val num = measure.timeSignatureNumerator
        val den = measure.timeSignatureDenominator
        val nominalCapacity = (num.toFloat() / (den.toFloat() / 4.0f)) // in beats
        val timeSigStr = "$num/$den"

        sb.appendLine("########################################################################################################################")
        sb.appendLine("MEASURE #$mNum  |  TimeSig: $timeSigStr (Nominal Capacity: %.2f beats)  |  Key: ${measure.keySignature.name}  |  Barline: ${measure.barline}".format(nominalCapacity))
        sb.appendLine("########################################################################################################################")

        val measureFlags = mutableListOf<String>()
        if (measure.repeatStart) measureFlags.add("REPEAT_START (|:)")
        if (measure.repeatEnd) measureFlags.add("REPEAT_END (:|)")
        if (measure.endingNumber != null) measureFlags.add("VOLTA_ENDING #${measure.endingNumber}")
        if (measure.rehearsalMark.isNotEmpty()) measureFlags.add("REHEARSAL_MARK [${measure.rehearsalMark}]")
        if (measure.segno) measureFlags.add("SEGNO (𝄋)")
        if (measure.coda) measureFlags.add("CODA (𝄌)")
        if (measure.fine) measureFlags.add("FINE")
        if (measure.daCapo) measureFlags.add("D.C.")
        if (measure.dalSegno) measureFlags.add("D.S.")
        if (measure.toCoda) measureFlags.add("TO_CODA")

        if (measureFlags.isNotEmpty()) {
            sb.appendLine("  Flags & Navigation : " + measureFlags.joinToString(" | "))
        }

        if (measure.pedalEvents.isNotEmpty()) {
            sb.appendLine("  Pedal Controllers  :")
            measure.pedalEvents.forEachIndexed { pIdx, p ->
                val typeName = when (p.pedalType) {
                    64 -> "Sustain/Damper (CC#64)"
                    66 -> "Sostenuto (CC#66)"
                    67 -> "Soft/UnaCorda (CC#67)"
                    else -> "CC#${p.pedalType}"
                }
                sb.appendLine("    #${pIdx + 1}: Pos=%.2fb | %s | State=%s".format(p.position, typeName, if (p.isPressed) "DOWN (PRESSED)" else "UP (RELEASED)"))
            }
        }

        val sortedNotes = measure.notes.sortedWith(compareBy({ it.position }, { it.voice }, { it.midiNote }))
        val voicesInMeasure = sortedNotes.map { it.voice }.distinct().sorted()

        sb.appendLine("  Summary: ${sortedNotes.size} Notes, ${measure.rests.size} Rests, Voices=[${voicesInMeasure.joinToString()}]")
        sb.appendLine()

        if (sortedNotes.isNotEmpty()) {
            sb.appendLine("  ┌─ NOTES TABLE (${sortedNotes.size} elements):")
            sb.appendLine("  │ %-3s | %-6s | %-4s | %-7s | %-6s | %-6s | %-6s | %-5s | %-5s | %-6s | %-5s | %-9s | %-8s | %s".format(
                "Idx", "Pitch", "MIDI", "Freq(Hz)", "Pos(b)", "Ticks", "Dur(b)", "Clef", "Hand", "Voice", "Stem", "BeamGrp", "Accident", "Details & Attributes"
            ))
            sb.appendLine("  ├─" + "─".repeat(116))

            var prevPos = -1.0f
            sortedNotes.forEachIndexed { nIdx, n ->
                val pitchName = midiToName(n.midiNote)
                val freq = midiToFreq(n.midiNote)
                // If voice is explicitly set (0 for RH, 1 for LH), prioritize voice/hand over arbitrary pitch threshold
                val isTreble = when (n.voice) {
                    0 -> true
                    1 -> false
                    else -> n.midiNote >= 60
                }
                val clefStr = if (isTreble) "TREBLE" else "BASS"
                val handStr = if (n.voice == 0) "RH" else if (n.voice == 1) "LH" else if (n.midiNote >= 60) "RH" else "LH"
                val stemStr = when (n.stemUp) {
                    true -> "UP"
                    false -> "DOWN"
                    null -> "AUTO"
                }
                val ticks = (n.position * 480).toInt()
                val deltaStr = if (prevPos >= 0f) "+%.2fb".format(n.position - prevPos) else "BEAT_0"
                prevPos = n.position

                val attrList = mutableListOf<String>()
                attrList.add("Δ:$deltaStr")
                if (n.string != null && n.fret != null) attrList.add("Tab(Str:${n.string},Fret:${n.fret})")
                if (n.dotted || n.isDotted) attrList.add("Dotted")
                if (n.tuplet > 0) attrList.add("Tuplet:${n.tuplet}")
                if (n.grace) attrList.add("Grace")
                if (n.tieStart) attrList.add("TieStart")
                if (n.tieEnd) attrList.add("TieEnd")
                if (n.slurStart) attrList.add("SlurStart")
                if (n.slurEnd) attrList.add("SlurEnd")
                if (n.crossStaff) attrList.add("CrossStaff")
                if (n.courtesyAccidental) attrList.add("CourtesyAccidental")
                if (n.chordSymbol.isNotEmpty()) attrList.add("Chord:[${n.chordSymbol}]")
                if (n.dynamic.isNotEmpty()) attrList.add("Dynamic:${n.dynamic}")
                if (n.lyric.isNotEmpty()) attrList.add("Lyric:\"${n.lyric}\"")
                if (n.fingering != null) attrList.add("Finger:${n.fingering}")
                if (n.articulation != TabArticulation.NONE) attrList.add("TabArt:${n.articulation.name}")
                if (n.musicalArticulation != MusicalArticulation.NONE) attrList.add("MusArt:${n.musicalArticulation.name}")
                if (n.bend != null) attrList.add("Bend(${n.bend.bendSteps} steps)")
                if (n.harmonic != null) attrList.add("Harmonic:${n.harmonic.name}")
                if (n.palmMute) attrList.add("P.M.")
                if (n.letRing) attrList.add("LetRing")
                if (n.vibrato) attrList.add("Vibrato")

                sb.appendLine("  │ %-3d | %-6s | %-4d | %-7.1f | %-6.2f | %-6d | %-6.2f | %-5s | %-5s | %-6d | %-5s | %-9d | %-8s | %s".format(
                    nIdx + 1, pitchName, n.midiNote, freq, n.position, ticks, n.duration, clefStr, handStr, n.voice, stemStr, n.beamGroup, n.accidental.name, attrList.joinToString(" • ")
                ))
            }
            sb.appendLine("  └─" + "─".repeat(116))
        } else {
            sb.appendLine("  (No notes in this measure)")
        }

        if (measure.rests.isNotEmpty()) {
            sb.appendLine("  ┌─ RESTS TABLE (${measure.rests.size} elements):")
            sb.appendLine("  │ %-3s | %-6s | %-6s | %-6s | %-5s".format("Idx", "Glyph", "Pos(b)", "Dur(b)", "Voice"))
            sb.appendLine("  ├─" + "─".repeat(45))
            measure.rests.forEachIndexed { rIdx, r ->
                sb.appendLine("  │ %-3d | %-6s | %-6.2f | %-6.2f | %-5d".format(rIdx + 1, r.symbolChar, r.position, r.duration, r.voice))
            }
            sb.appendLine("  └─" + "─".repeat(45))
        }

        // Voice rhythmic density audit
        voicesInMeasure.forEach { v ->
            val vNotes = sortedNotes.filter { it.voice == v }
            val vDurationSum = vNotes.sumOf { it.duration.toDouble() }
            val vRestsSum = measure.rests.filter { it.voice == v }.sumOf { it.duration.toDouble() }
            val totalVoiceBeats = vDurationSum + vRestsSum
            val drift = totalVoiceBeats - nominalCapacity
            val status = if (kotlin.math.abs(drift) < 0.05) "BALANCED" else "DRIFT: %+.2fb".format(drift)
            sb.appendLine("  [Voice $v Audit]: Notes=${vNotes.size} | NotesDur=%.2fb | RestsDur=%.2fb | Total=%.2fb / %.2fb [$status]".format(vDurationSum, vRestsSum, totalVoiceBeats, nominalCapacity))
        }

        sb.appendLine()
    }

    sb.appendLine("========================================================================================================================")
    sb.appendLine("                                           END OF ULTRA-FORENSIC SCORE DUMP")
    sb.appendLine("========================================================================================================================")
    return sb.toString()
}

data class DrawingConfig(
    val lineSpacing: Float = 10.0f,
    val trebleBaseline: Float = 48.0f,
    val bassBaseline: Float = 118.0f,
    val primaryColor: Color = Color(0xFF64B5F6),
    val leftHandColor: Color = Color(0xFF6C63FF),
    val rightHandColor: Color = Color(0xFF4A90E2),
    val isLeftHandHidden: Boolean = false,
    val isRightHandHidden: Boolean = false,
    val isDark: Boolean = true,
    val isTab: Boolean = false,
    val tabBaseline: Float = 195.0f,
    val isPedalPressed: Boolean = false,
    val strokeWidth: Float = 1.5f,
    val showLabels: Boolean = true,
    val showLyrics: Boolean = true,
    val isDarkMode: Boolean = isDark,
    val colorScheme: ColorScheme? = null
)

@Serializable
data class NotationOptions(
    val showNoteNames: Boolean = true,
    val hideLeftHand: Boolean = false,
    val hideRightHand: Boolean = false,
    val showLyrics: Boolean = true,
    val transposition: Int = 0,
    val shiftNotes: Int = 0,
    val scrollVertically: Boolean = true,
    val isPedalPressed: Boolean = false,
    val keySignatureOverride: String = "C",
    val timeSignatureOverride: String = "4/4",
    val useOsmdRenderer: Boolean = false,
    val useVexflow: Boolean = useOsmdRenderer
)


@Serializable
data class RhythmColumn(
    val stepIndex: Int,
    val activePitches: List<Int> = emptyList(),
    val isAccent: Boolean = false
)

data class FallingNoteBlock(
    val midiNote: Int,
    var currentY: Float = 0f,
    val targetY: Float = 200f,
    val speed: Float = 4f,
    val isHit: Boolean = false
)


data class StaffAssignment(
    val trebleNotes: Set<ScoreNote> = emptySet(),
    val bassNotes: Set<ScoreNote> = emptySet(),
    val treble: Set<ScoreNote> = trebleNotes,
    val bass: Set<ScoreNote> = bassNotes
)

enum class PdfPageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("Letter (8.5 × 11 in)", 612, 792),
    LEGAL("Legal (8.5 × 14 in)", 612, 1008),
    A3("A3 Conductor (297 × 420 mm)", 842, 1191)
}

enum class PdfOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

@Serializable
data class PdfExportOptions(
    val title: String = "Untitled Score",
    val composer: String = "pEQ Learn Studio",
    val pageSize: String = "A4",
    val orientation: String = "Portrait",
    val includeTitleHeader: Boolean = true,
    val includeMeasureNumbers: Boolean = true,
    val includeChordLabels: Boolean = true,
    val includeLyrics: Boolean = true,
    val staffFormat: String = "Both", // "Staff", "Tab", "Both"
    val colorTheme: String = "Monochrome", // "Monochrome", "Color (LH/RH)", "Inverted Dark"
    val staffLineSpacing: Float = 14f,
    val measuresPerSystem: Int = 4,
    val authorNotes: String = ""
)

