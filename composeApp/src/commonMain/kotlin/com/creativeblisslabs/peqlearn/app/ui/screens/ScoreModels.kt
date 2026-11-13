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
    THIRTY_SECOND(0.125f),
    SIXTY_FOURTH(0.0625f)
}

@Serializable
enum class MusicalArticulation {
    NONE,
    STACCATO,
    STACCATISSIMO,
    ACCENT,
    TENUTO,
    MARCATO
}

@Serializable
enum class TabArticulation {
    NONE,
    HAMMER_ON,
    PULL_OFF,
    SLIDE_UP,
    SLIDE_DOWN,
    LEGATO_SLIDE_UP,
    LEGATO_SLIDE_DOWN,
    TAP,
    RELEASE,
    DEAD_NOTE
}

@Serializable
enum class BendType {
    BEND,
    PRE_BEND,
    BEND_RELEASE,
    PRE_BEND_RELEASE,
    UNISON_BEND
}

@Serializable
enum class GuitarHarmonicType(val label: String) {
    NONE(""),
    NATURAL("N.H."),
    ARTIFICIAL("A.H."),
    PINCH("P.H."),
    TAPPED("T.H."),
    SEMI("S.H.")
}

@Serializable
enum class OrnamentType {
    NONE,
    TRILL,
    UPPER_MORDENT,
    LOWER_MORDENT,
    TURN,
    INVERTED_TURN
}

@Serializable
enum class ArpeggioDirection {
    NONE,
    UP,
    DOWN
}

@Serializable
enum class PickDirection {
    NONE,
    DOWN,
    UP,
    STRUM_DOWN,
    STRUM_UP,
    RASGUEADO,
    SLAP,
    POP
}

@Serializable
enum class OctaveShift(val label: String, val semitoneShift: Int) {
    NONE("", 0),
    OTTAVA_ALTA_8VA("8va", 12),
    OTTAVA_BASSA_8VB("8vb", -12),
    QUINDICESIMA_ALTA_15MA("15ma", 24),
    QUINDICESIMA_BASSA_15MB("15mb", -24)
}

@Serializable
enum class WhammyType {
    NONE,
    DIP,
    DIVE,
    SCOOP,
    GARGLE
}

@Serializable
data class WhammyInfo(
    val type: WhammyType = WhammyType.DIP,
    val steps: Float = -1.0f // Negative = pitch drop, Positive = pull up
)

@Serializable
enum class HairpinType {
    CRESCENDO,
    DECRESCENDO
}

@Serializable
data class HairpinSpan(
    val startPosition: Float = 0.0f,
    val endPosition: Float = 2.0f,
    val type: HairpinType = HairpinType.CRESCENDO,
    val voice: Int = 0
)

@Serializable
enum class TempoChangeType(val label: String) {
    NONE(""),
    RITARDANDO("rit."),
    RALLENTANDO("rall."),
    ACCELERANDO("accel."),
    A_TEMPO("a tempo")
}

@Serializable
enum class TimeSignatureSymbol {
    NUMERIC,
    COMMON_TIME,
    CUT_TIME
}

@Serializable
data class BarreInfo(
    val fret: Int = 0,
    val startString: Int = 0,
    val endString: Int = 5,
    val isHalfBarre: Boolean = false
)

@Serializable
data class BendInfo(
    val stringIndex: Int = 0,
    val fret: Int = 0,
    val bendSteps: Float = 1.0f, // 1.0 = full step (2 semitones), 0.5 = half step (1 semitone)
    val releaseSteps: Float = 0.0f,
    val type: BendType = BendType.BEND
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
    val symbolChar: String = "𝄽",
    val fermata: Boolean = false,
    val dotted: Boolean = false
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
    val doubleDotted: Boolean = false,
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
    val rightHandFinger: String = "", // "p", "i", "m", "a", "c" for fingerstyle/classical guitar
    val articulation: TabArticulation = TabArticulation.NONE,
    val musicalArticulation: MusicalArticulation = MusicalArticulation.NONE,
    val ornament: OrnamentType = OrnamentType.NONE,
    val tremoloSlashes: Int = 0, // 1 = 8th tremolo, 2 = 16th tremolo, 3 = 32nd unmeasured tremolo
    val arpeggio: ArpeggioDirection = ArpeggioDirection.NONE,
    val octaveShift: OctaveShift = OctaveShift.NONE,
    val pickDirection: PickDirection = PickDirection.NONE,
    val fermata: Boolean = false,
    val ghostNote: Boolean = false,
    val bend: BendInfo? = null,
    val whammy: WhammyInfo? = null,
    val whammyDip: WhammyDipInfo? = null, // Added to support whammy dip rendering
    val harmonic: HarmonicType? = null,
    val guitarHarmonic: GuitarHarmonicType = GuitarHarmonicType.NONE,
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
    val hairpins: List<HairpinSpan> = emptyList(),
    val repeatStart: Boolean = false,
    val repeatEnd: Boolean = false,
    val repeatCount: Int = 2,
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
    val timeSignatureSymbol: TimeSignatureSymbol = TimeSignatureSymbol.NUMERIC,
    val keySignature: KeySignature = KeySignature.C_MAJOR,
    val tempoOverride: Float? = null,
    val tempoChangeText: String = "",
    val tempoChangeType: TempoChangeType = TempoChangeType.NONE,
    val isPickup: Boolean = false,
    val multiMeasureRestCount: Int = 0
)

@Serializable
data class MidiTrackInfo(
    val trackIndex: Int = 0,
    val trackName: String = "",
    val channel: Int = 0,
    val noteCount: Int = 0,
    val program: Int = 0,
    val isEnabled: Boolean = true
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
    val isPdfScore: Boolean = false,
    val tracks: List<MidiTrackInfo> = emptyList(),
    val capo: Int = 0,
    val tuningMidi: List<Int> = listOf(64, 59, 55, 50, 45, 40), // 1st string E4 down to 6th string E2
    val tuningName: String = "Standard (E A D G B E)",
    val isGuitar8vb: Boolean = false
) {
    val bpm: Int get() = tempo.toInt()
}

/**
 * Generates an ultra-detailed, human-readable forensic representation of the parsed score.
 */
fun Score.dumpAsText(): String {
    val sb = StringBuilder()
    val sharpNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val flatNames = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    val noteNames = if (keySignature.accidentalsCount > 0 && !keySignature.isSharp) flatNames else sharpNames
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
    sb.appendLine("  • Primary Clef       : $clef${if (isGuitar8vb) " (8vb)" else ""}")
    sb.appendLine("  • Key Signature      : ${keySignature.keyName} (${keySignature.accidentalsCount} ${if (keySignature.isSharp) "sharps" else "flats"})")
    sb.appendLine("  • Global Time Sig    : $timeSignature (Pickup measure: $pickupMeasure)")
    sb.appendLine("  • Global Tempo       : $tempo BPM ($tempoText)")
    if (capo > 0) sb.appendLine("  • Guitar Capo        : Fret $capo")
    sb.appendLine("  • Guitar Tuning      : $tuningName [${tuningMidi.joinToString { midiToName(it) }}]")
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
        val nominalCapacity = (num.toFloat() / (den.toFloat() / 4.0f))
        val timeSigStr = "$num/$den"

        sb.appendLine("########################################################################################################################")
        sb.appendLine("MEASURE #$mNum  |  TimeSig: $timeSigStr (Nominal Capacity: %.2f beats)  |  Key: ${measure.keySignature.name}  |  Barline: ${measure.barline}".format(nominalCapacity))
        sb.appendLine("########################################################################################################################")

        val measureFlags = mutableListOf<String>()
        if (measure.isPickup || (mIdx == 0 && pickupMeasure)) measureFlags.add("PICKUP_ANACRUSIS")
        if (measure.repeatStart) measureFlags.add("REPEAT_START (|:)")
        if (measure.repeatEnd) measureFlags.add("REPEAT_END (:| x${measure.repeatCount})")
        if (measure.endingNumber != null) measureFlags.add("VOLTA_ENDING #${measure.endingNumber}")
        if (measure.rehearsalMark.isNotEmpty()) measureFlags.add("REHEARSAL_MARK [${measure.rehearsalMark}]")
        if (measure.segno) measureFlags.add("SEGNO (𝄋)")
        if (measure.coda) measureFlags.add("CODA (𝄌)")
        if (measure.fine) measureFlags.add("FINE")
        if (measure.daCapo) measureFlags.add("D.C.")
        if (measure.dalSegno) measureFlags.add("D.S.")
        if (measure.toCoda) measureFlags.add("TO_CODA")
        if (measure.tempoOverride != null) measureFlags.add("TEMPO:${measure.tempoOverride}BPM")
        if (measure.tempoChangeType != TempoChangeType.NONE) measureFlags.add("TEMPO_MOD:${measure.tempoChangeType.label}")
        if (measure.multiMeasureRestCount > 1) measureFlags.add("MULTI_REST:${measure.multiMeasureRestCount} bars")

        if (measureFlags.isNotEmpty()) {
            sb.appendLine("  Flags & Navigation : " + measureFlags.joinToString(" | "))
        }

        if (measure.hairpins.isNotEmpty()) {
            sb.appendLine("  Hairpins           : " + measure.hairpins.joinToString { "${it.type.name}[%.2fb..%.2fb]".format(it.startPosition, it.endPosition) })
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
                if (n.doubleDotted) attrList.add("DoubleDotted")
                else if (n.dotted || n.isDotted) attrList.add("Dotted")
                if (n.tuplet > 0) attrList.add("Tuplet:${n.tuplet}")
                if (n.grace) attrList.add("Grace")
                if (n.fermata) attrList.add("Fermata")
                if (n.ghostNote) attrList.add("GhostNote")
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
                if (n.rightHandFinger.isNotEmpty()) attrList.add("RH:${n.rightHandFinger}")
                if (n.pickDirection != PickDirection.NONE) attrList.add("Pick:${n.pickDirection.name}")
                if (n.articulation != TabArticulation.NONE) attrList.add("TabArt:${n.articulation.name}")
                if (n.musicalArticulation != MusicalArticulation.NONE) attrList.add("MusArt:${n.musicalArticulation.name}")
                if (n.ornament != OrnamentType.NONE) attrList.add("Ornament:${n.ornament.name}")
                if (n.tremoloSlashes > 0) attrList.add("Tremolo:${n.tremoloSlashes}")
                if (n.arpeggio != ArpeggioDirection.NONE) attrList.add("Arp:${n.arpeggio.name}")
                if (n.octaveShift != OctaveShift.NONE) attrList.add("Oct:${n.octaveShift.label}")
                if (n.bend != null) attrList.add("Bend(${n.bend.type.name}:${n.bend.bendSteps}->${n.bend.releaseSteps})")
                if (n.whammy != null) attrList.add("Whammy(${n.whammy.type.name}:${n.whammy.steps})")
                if (n.guitarHarmonic != GuitarHarmonicType.NONE) attrList.add("Harm:${n.guitarHarmonic.label}")
                else if (n.harmonic != null) attrList.add("Harmonic:${n.harmonic.name}")
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

@Serializable
data class WhammyDipInfo(
    val depth: Float = 1f, // e.g., semitones or depth amount
    val duration: Float = 1.0f
)
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
    val colorScheme: ColorScheme? = null,
    val tuning: List<Int> = listOf(64, 59, 55, 50, 45, 40),
    val tuningMidi: List<Int> = tuning,
    val capo: Int = 0,
    val isGuitar8vb: Boolean = false,
    val isSingleStaff: Boolean = false,
    val isGuitarScore: Boolean = false
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
    val staffFormat: String = "Both",
    val colorTheme: String = "Monochrome",
    val staffLineSpacing: Float = 14f,
    val measuresPerSystem: Int = 4,
    val authorNotes: String = ""
)

private val GLOBAL_NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

fun isMidiNoteBlack(midiNote: Int): Boolean {
    val pc = ((midiNote % 12) + 12) % 12
    return pc in intArrayOf(1, 3, 6, 8, 10)
}

fun getNoteNameFromMidi(midiNote: Int): String {
    val pc = ((midiNote % 12) + 12) % 12
    val octave = (midiNote / 12) - 1
    return "${GLOBAL_NOTE_NAMES[pc]}$octave"
}

fun getPitchOnlyFromMidi(midiNote: Int): String {
    val pc = ((midiNote % 12) + 12) % 12
    return GLOBAL_NOTE_NAMES[pc]
}

// ─────────────────────────────────────────────────────────────────────────────
// Piano Human-Hands Overlay Diagnostic Dump
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Builds a full human-readable forensic audit of how the PianoHumanHandsOverlay
 * partitions notes into Left Hand / Right Hand and assigns finger numbers.
 *
 * Reports:
 *  • Per-measure: which MIDI notes were active, how they were split LH/RH,
 *    which finger number each note received, any notes SKIPPED (outside 21..108,
 *    drum channel, or clamped away), and span-ergonomics warnings.
 *  • Summary: total notes, skipped notes, clamped notes, hand-crossing events,
 *    and span violations.
 */
fun Score.dumpHandsOverlayAsText(): String {
    val sb = StringBuilder()

    fun midiName(m: Int) = getNoteNameFromMidi(m)
    fun isBlack(m: Int) = isMidiNoteBlack(m)

    // Replicate hand-split logic from PianoHumanHandsOverlay
    val pianoTrackIndices: Set<Int> = run {
        val nonDrum = tracks.filter { it.channel != 9 }
        val explicit = nonDrum.filter { tr ->
            val nl = tr.trackName.lowercase()
            tr.program in 0..7 || nl.contains("piano") || nl.contains("keys") ||
                    nl.contains("right hand") || nl.contains("left hand") ||
                    nl.contains("treble") || nl.contains("bass")
        }.map { it.trackIndex }.toSet()
        if (explicit.isNotEmpty()) explicit else nonDrum.map { it.trackIndex }.toSet()
    }

    var totalNotes = 0
    var totalSkipped = 0
    var totalClamped = 0
    var handCrossings = 0
    var spanViolations = 0
    var missingFingers = 0

    // Finger assignment heuristic (mirrors solveHumanChordAndMelodyFingers count-based logic + black-key thumb fix)
    fun assignFingers(sorted: List<Int>, isRH: Boolean): List<Pair<Int, Int>> { // List<midiNote, fingerNum>
        val count = sorted.size.coerceAtMost(5)
        val sub = sorted.take(count)
        val span = if (sub.size > 1) sub.last() - sub.first() else 0
        // Black-key thumb check: RH lowest note, LH highest note
        val rhThumbBlack = isRH && sub.isNotEmpty() && isMidiNoteBlack(sub.first())
        val lhThumbBlack = !isRH && sub.isNotEmpty() && isMidiNoteBlack(sub.last())
        fun bumpRh(fs: List<Int>) = if (rhThumbBlack) fs.map { (it + 1).coerceIn(2, 5) } else fs
        fun bumpLh(fs: List<Int>) = if (lhThumbBlack) fs.map { (it - 1).coerceIn(1, 4) } else fs

        val fingers: List<Int> = if (isRH) {
            when (count) {
                1  -> listOf(if (isMidiNoteBlack(sub[0])) 3 else 3)
                2  -> bumpRh(when { span <= 2 -> listOf(1,2); span <= 4 -> listOf(1,3); span <= 6 -> listOf(1,4); else -> listOf(1,5) })
                3  -> bumpRh(when { span <= 4 -> listOf(1,2,3); else -> listOf(1,3,5) })
                4  -> bumpRh(if ((sub.getOrElse(3){0} - sub.getOrElse(2){0}) <= 2) listOf(1,2,4,5) else listOf(1,2,3,5))
                else -> bumpRh(listOf(1,2,3,4,5))
            }
        } else {
            when (count) {
                1  -> listOf(if (isMidiNoteBlack(sub[0])) 3 else 3)
                2  -> bumpLh(when { span <= 2 -> listOf(2,1); span <= 4 -> listOf(3,1); span <= 6 -> listOf(4,1); else -> listOf(5,1) })
                3  -> bumpLh(when { span <= 4 -> listOf(3,2,1); else -> listOf(5,3,1) })
                4  -> bumpLh(listOf(5,3,2,1))
                else -> bumpLh(listOf(5,4,3,2,1))
            }
        }
        return sub.mapIndexed { i, midi -> midi to (fingers.getOrElse(i) { i + 1 }.coerceIn(1, 5)) }
    }

    sb.appendLine("═".repeat(108))
    sb.appendLine("       pEQ-LEARN  ·  PIANO HUMAN HANDS OVERLAY — FORENSIC DIAGNOSTIC DUMP")
    sb.appendLine("═".repeat(108))
    sb.appendLine("Score  : $title  |  Tempo: ${tempo.toInt()} BPM  |  Measures: ${measures.size}")
    sb.appendLine("Tracks : ${tracks.joinToString { "#${it.trackIndex} \"${it.trackName}\" ch${it.channel+1} prog${it.program}" }}")
    sb.appendLine("Piano-only track indices used for hand split: $pianoTrackIndices")
    sb.appendLine("═".repeat(108))
    sb.appendLine()

    // Heuristic hand boundary
    var lastLhCenter = 48
    var lastRhCenter = 69

    measures.forEachIndexed { mIdx, measure ->
        val mNum = if (measure.measureIndex > 0) measure.measureIndex else mIdx + 1
        val beatsPerMeasure = (measure.timeSignatureNumerator.toFloat() *
                (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)

        sb.appendLine("▌ MEASURE #$mNum  [${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}  ${beatsPerMeasure} beats]")

        val allNotes = measure.notes.filter { !it.isRest }
        totalNotes += allNotes.size

        // Group by beat position (round to 2 dp for grouping)
        val byBeat = allNotes.groupBy { "%.2f".format(it.position) }

        byBeat.entries.sortedBy { it.key }.forEach { (beatStr, notes) ->
            val beat = beatStr.toFloatOrNull() ?: 0f

            // Separate piano notes from non-piano / drums
            val drumNotes   = notes.filter { it.voice in (tracks.filter { t -> t.channel == 9 }.map { t -> t.trackIndex }) }
            val pianoNotes  = notes.filter { it.voice in pianoTrackIndices && !it.isRest }
            val skippedNow  = notes.filter { it !in pianoNotes && it !in drumNotes }

            // Range check
            val outOfRange  = pianoNotes.filter { it.midiNote !in 21..108 }
            val validNotes  = pianoNotes.filter { it.midiNote in 21..108 }

            totalSkipped += skippedNow.size + outOfRange.size

            if (notes.isEmpty()) return@forEach

            sb.append("  Beat %.2f │ Raw notes: ".format(beat))
            sb.appendLine(notes.joinToString { "${midiName(it.midiNote)}(v${it.voice})" })

            if (skippedNow.isNotEmpty()) {
                sb.appendLine("           ⚠ SKIPPED (non-piano track): ${skippedNow.joinToString { midiName(it.midiNote) }}")
            }
            if (outOfRange.isNotEmpty()) {
                sb.appendLine("           ⚠ SKIPPED (out of 21-108):  ${outOfRange.joinToString { "${it.midiNote}" }}")
            }
            if (validNotes.isEmpty()) {
                sb.appendLine("           ─ No valid piano notes at this beat.")
                return@forEach
            }

            val sorted = validNotes.map { it.midiNote }.distinct().sorted()

            // Split heuristic
            val hysteresisSplit = ((lastLhCenter + lastRhCenter + 60) / 3).coerceIn(55, 64)
            val rawLh = sorted.filter { it < hysteresisSplit }.toMutableList()
            val rawRh = sorted.filter { it >= hysteresisSplit }.toMutableList()

            // Crossing guard
            if (rawLh.isNotEmpty() && rawRh.isNotEmpty() && rawLh.last() >= rawRh.first()) {
                handCrossings++
                sb.appendLine("           ⚠ HAND CROSSING DETECTED — re-splitting by largest gap")
                val all = (rawLh + rawRh).distinct().sorted()
                rawLh.clear(); rawRh.clear()
                var bestIdx = all.indexOfFirst { it >= 60 }.takeIf { it > 0 } ?: (all.size / 2).coerceAtLeast(1)
                var maxGap = -1
                for (i in 1 until all.size) {
                    val g = all[i] - all[i - 1]
                    if (all[i - 1] in 48..67 && g > maxGap) { maxGap = g; bestIdx = i }
                }
                for (i in all.indices) if (i < bestIdx) rawLh.add(all[i]) else rawRh.add(all[i])
            }

            // Ergonomic balance rebalance (mirrors overlay logic)
            while (rawRh.size > 1 && (rawRh.size > 5 || rawRh.last() - rawRh.first() > 12) && rawLh.size < 5) {
                val lo = rawRh.first()
                if ((if (rawLh.isEmpty()) 0 else lo - rawLh.first()) <= 12 && lo <= 65) {
                    rawRh.removeAt(0); rawLh.add(lo); rawLh.sort()
                } else break
            }
            while (rawLh.size > 1 && (rawLh.size > 5 || rawLh.last() - rawLh.first() > 12) && rawRh.size < 5) {
                val hi = rawLh.last()
                if ((if (rawRh.isEmpty()) 0 else rawRh.last() - hi) <= 12 && hi >= 55) {
                    rawLh.removeAt(rawLh.lastIndex); rawRh.add(0, hi); rawRh.sort()
                } else break
            }

            // Clamp to 5 notes max per hand
            val lhFinal = rawLh.distinct().sorted().take(5)
            val rhFinal = rawRh.distinct().sorted().take(5)
            totalClamped += (rawLh.size - lhFinal.size) + (rawRh.size - rhFinal.size)

            // Span checks
            val lhSpan = if (lhFinal.size > 1) lhFinal.last() - lhFinal.first() else 0
            val rhSpan = if (rhFinal.size > 1) rhFinal.last() - rhFinal.first() else 0
            if (lhSpan > 12) { spanViolations++; sb.appendLine("           ⚠ LH SPAN VIOLATION: ${lhSpan} semitones > 12 (exceeds one octave hand reach!)") }
            if (rhSpan > 12) { spanViolations++; sb.appendLine("           ⚠ RH SPAN VIOLATION: ${rhSpan} semitones > 12 (exceeds one octave hand reach!)") }

            // Assign fingers
            val lhFingers = assignFingers(lhFinal, false)
            val rhFingers = assignFingers(rhFinal, true)

            // Check for black-key thumb (bad fingering)
            val lhThumbBlack = lhFingers.any { (m, f) -> f == 1 && isBlack(m) }
            val rhThumbBlack = rhFingers.any { (m, f) -> f == 1 && isBlack(m) }
            if (lhThumbBlack) sb.appendLine("           ⚠ LH THUMB on BLACK KEY — ergonomically awkward")
            if (rhThumbBlack) sb.appendLine("           ⚠ RH THUMB on BLACK KEY — ergonomically awkward")

            // Missing-finger check
            val allMidi = lhFinal + rhFinal
            val assignedMidi = lhFingers.map { it.first } + rhFingers.map { it.first }
            val unassigned = allMidi.filter { it !in assignedMidi }
            if (unassigned.isNotEmpty()) {
                missingFingers += unassigned.size
                sb.appendLine("           ⚠ NO FINGER ASSIGNED for: ${unassigned.joinToString { midiName(it) }}")
            }

            // Update center heuristic
            if (lhFinal.isNotEmpty()) lastLhCenter = ((lhFinal.first() + lhFinal.last()) / 2).coerceIn(21, 108)
            if (rhFinal.isNotEmpty()) lastRhCenter = ((rhFinal.first() + rhFinal.last()) / 2).coerceIn(21, 108)

            // Print result table
            sb.appendLine("           LH [${lhFinal.size} notes, span=$lhSpan semi] : " +
                    lhFingers.joinToString(" | ") { (m, f) -> "${midiName(m)}→F$f${if(isBlack(m)) "♯" else ""}" })
            sb.appendLine("           RH [${rhFinal.size} notes, span=$rhSpan semi] : " +
                    rhFingers.joinToString(" | ") { (m, f) -> "${midiName(m)}→F$f${if(isBlack(m)) "♯" else ""}" })
        }

        sb.appendLine()
    }

    sb.appendLine("═".repeat(108))
    sb.appendLine("SUMMARY")
    sb.appendLine("═".repeat(108))
    sb.appendLine("  Total notes in score        : $totalNotes")
    sb.appendLine("  Notes skipped (drum/non-piano or out of range) : $totalSkipped")
    sb.appendLine("  Notes clamped (> 5 per hand, rebalanced)       : $totalClamped")
    sb.appendLine("  Hand-crossing events detected                   : $handCrossings")
    sb.appendLine("  Span violations (> 12 semitones per hand)       : $spanViolations")
    sb.appendLine("  Notes with no finger assigned                   : $missingFingers")
    if (totalSkipped > 0)
        sb.appendLine("  ⚠ $totalSkipped notes were NOT shown on the overlay — check track/channel assignments above!")
    if (handCrossings > 0)
        sb.appendLine("  ⚠ $handCrossings hand-crossing events auto-fixed — may indicate ambiguous track voice data")
    if (spanViolations > 0)
        sb.appendLine("  ⚠ $spanViolations span violations — overlay may visually stretch beyond realistic hand reach")
    sb.appendLine("═".repeat(108))
    return sb.toString()
}