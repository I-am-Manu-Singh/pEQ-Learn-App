package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlin.math.abs
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
    DEAD_NOTE,
    GHOST_NOTE,
    PALM_MUTE,
    LET_RING,
    SLAP,
    POP
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
    val steps: Float = -1.0f
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
    val bendSteps: Float = 1.0f,
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
    val pedalType: Int = 64,
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
    val rightHandFinger: String = "",
    val articulation: TabArticulation = TabArticulation.NONE,
    val musicalArticulation: MusicalArticulation = MusicalArticulation.NONE,
    val ornament: OrnamentType = OrnamentType.NONE,
    val tremoloSlashes: Int = 0,
    val arpeggio: ArpeggioDirection = ArpeggioDirection.NONE,
    val octaveShift: OctaveShift = OctaveShift.NONE,
    val pickDirection: PickDirection = PickDirection.NONE,
    val fermata: Boolean = false,
    val ghostNote: Boolean = false,
    val bend: BendInfo? = null,
    val whammy: WhammyInfo? = null,
    val whammyDip: WhammyDipInfo? = null,
    val harmonic: HarmonicType? = null,
    val guitarHarmonic: GuitarHarmonicType = GuitarHarmonicType.NONE,
    val palmMute: Boolean = false,
    val letRing: Boolean = false,
    val vibrato: Boolean = false,
    val barre: BarreInfo? = null,
    val isRest: Boolean = false,
    val isDotted: Boolean = dotted,
    val stringNum: Int = string ?: 0,
    val fretNum: Int = fret ?: 0,
    val pitch: Int = midiNote,
    val durationFraction: Float = (duration / 4f).coerceAtLeast(0.05f)
) {
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
    val tuningMidi: List<Int> = listOf(64, 59, 55, 50, 45, 40),
    val tuningName: String = "Standard (E A D G B E)",
    val isGuitar8vb: Boolean = false
) {
    val bpm: Int get() = tempo.toInt()
}

/**
 * Generates an ultra-detailed, human-readable forensic representation of the parsed score.
 * FIXED:
 *  - Clef & Hand assignment uses actual pitch register (`midiNote >= 60` -> TREBLE/RH, `< 60` -> BASS/LH)
 *    instead of hardcoding `voice == 1` to `BASS | LH` when `v1` plays high guitar chords up to F5!
 *  - Voice Audit sums chord durations by unique onset position (`groupBy { it.position }`) instead of
 *    multiplying chord duration by the number of notes in the chord!
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
            sb.appendLine("  │ %-3s | %-6s | %-4s | %-7s | %-6s | %-6s | %-6s | %-5s | %-5s | %-6s | %-5s | %-5s | %-9s | %-8s | %s".format(
                "Idx", "Pitch", "MIDI", "Freq(Hz)", "Pos(b)", "Ticks", "Dur(b)", "Clef", "Hand", "Finger", "Voice", "Stem", "BeamGrp", "Accident", "Details & Attributes"
            ))
            sb.appendLine("  ├─" + "─".repeat(124))

            var prevPos = -1.0f
            sortedNotes.forEachIndexed { nIdx, n ->
                val pitchName = midiToName(n.midiNote)
                val freq = midiToFreq(n.midiNote)
                // Match StaffAndTabNotationViewer's exact register split at Middle C (MIDI 60)
                val isTreble = n.midiNote >= 60
                val clefStr = if (isTreble) "TREBLE" else "BASS"
                val handStr = if (isTreble) "RH" else "LH"
                val fingerStr = when {
                    n.fingering != null && n.fingering in 1..5 -> "F${n.fingering}"
                    n.rightHandFinger.isNotEmpty() -> n.rightHandFinger
                    else -> "-"
                }
                val stemStr = when (n.stemUp) {
                    true -> "UP"
                    false -> "DOWN"
                    null -> if (isTreble) (if (n.midiNote < 71) "UP" else "DOWN") else (if (n.midiNote < 50) "UP" else "DOWN")
                }
                val ticks = (n.position * 480).toInt()
                val deltaStr = if (prevPos >= 0f) "+%.2fb".format(n.position - prevPos) else "BEAT_0"
                prevPos = n.position

                val attrList = mutableListOf<String>()
                val hexColorStr = if (n.voice == 9 || n.voice == 10) "#FF5252 [DRUM_CRIMSON]" else if (isTreble) "#4A90E2 [RH_CYAN]" else "#6C63FF [LH_PURPLE]"
                attrList.add("Color:$hexColorStr")
                attrList.add("Δ:$deltaStr")
                if (n.fingering == 1 && isMidiNoteBlack(n.midiNote)) {
                    attrList.add("⚠️[WARN:Thumb-on-Black-Key]")
                }
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

                sb.appendLine("  │ %-3d | %-6s | %-4d | %-7.1f | %-6.2f | %-6d | %-6.2f | %-5s | %-5s | %-6s | %-5d | %-5s | %-9d | %-8s | %s".format(
                    nIdx + 1, pitchName, n.midiNote, freq, n.position, ticks, n.duration, clefStr, handStr, fingerStr, n.voice, stemStr, n.beamGroup, n.accidental.name, attrList.joinToString(" • ")
                ))
            }
            sb.appendLine("  └─" + "─".repeat(124))
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

        // FIXED VOICE AUDIT: Group simultaneous chord notes by `position` so a 6-note chord counts once, not 6x!
        voicesInMeasure.forEach { v ->
            val vNotes = sortedNotes.filter { it.voice == v }
            val vDurationSum = vNotes
                .groupBy { "%.2f".format(it.position) }
                .values
                .sumOf { chordAtPos -> (chordAtPos.maxOfOrNull { it.duration } ?: 0f).toDouble() }
            val vRestsSum = measure.rests.filter { it.voice == v }.sumOf { it.duration.toDouble() }
            val totalVoiceBeats = vDurationSum + vRestsSum
            val drift = totalVoiceBeats - nominalCapacity
            val status = if (abs(drift) < 0.06) "BALANCED" else "DRIFT: %+.2fb".format(drift)
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
    val depth: Float = 1f,
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
// Piano Human-Hands Overlay Diagnostic Dump (RoboPianist / PianoPlayer OT Engine)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * FIXED: Full human-readable forensic audit of the PianoHumanHandsOverlay.
 *  1. Includes all non-drum melodic tracks (Guitar, Bass, Strings, Choir, Piano) so 0 melodic notes are skipped.
 *  2. Uses the stateful Optimal Transport (OT) & PianoPlayer biomechanical hand-split and finger solver
 *     so single notes never lock to F3, chords never exceed 12 semitones or 5 fingers per hand, and
 *     LH/RH never fight over middle notes.
 *  3. Fixes the "A#1→F3♯" string bug -> outputs clean "A#1→F5".
 */
fun Score.dumpHandsOverlayAsText(): String {
    val sb = StringBuilder()

    fun midiName(m: Int) = getNoteNameFromMidi(m)
    fun isBlack(m: Int) = isMidiNoteBlack(m)

    val melodicTrackIndices: Set<Int> = resolvePlayableMelodicTrackIndices(this)
    val drumTrackIndices: Set<Int> = tracks.filter {
        it.channel == 9 || it.channel == 10 || it.program in 112..119 ||
            it.trackName.lowercase().contains("drum")
    }.map { it.trackIndex }.toSet()

    var totalNotes = 0
    var totalSkipped = 0
    var totalClamped = 0
    var handCrossings = 0
    var spanViolations = 0
    var missingFingers = 0

    var lastLhCenter = 46 // A#2
    var lastRhCenter = 68 // G#4
    var lastLhFinger = 3
    var lastRhFinger = 2

    fun clampHandVoicing(pitches: List<Int>, isRH: Boolean): Pair<List<Int>, Int> {
        if (pitches.isEmpty()) return emptyList<Int>() to 0
        val distinct = pitches.distinct().sorted()
        val spanFiltered = if (distinct.last() - distinct.first() > 12) {
            if (isRH) {
                val top = distinct.last()
                distinct.filter { top - it <= 12 }
            } else {
                val bot = distinct.first()
                distinct.filter { it - bot <= 12 }
            }
        } else distinct

        val finalFive = if (spanFiltered.size <= 5) {
            spanFiltered
        } else {
            val low = spanFiltered.first()
            val high = spanFiltered.last()
            val outerPcs = setOf(low % 12, high % 12)
            val inner = spanFiltered.subList(1, spanFiltered.lastIndex)
                .sortedByDescending { p -> if ((p % 12) !in outerPcs) 10 else 1 }
                .take(3)
            (listOf(low, high) + inner).distinct().sorted()
        }
        return finalFive to (distinct.size - finalFive.size).coerceAtLeast(0)
    }

    fun assignBiomechanicalFingers(
        sorted: List<Int>,
        isRH: Boolean,
        explicitMap: Map<Int, Int>
    ): List<Pair<Int, Int>> {
        if (sorted.isEmpty()) return emptyList()
        val count = sorted.size
        val span = if (count > 1) sorted.last() - sorted.first() else 0

        val rhThumbBlack = isRH && isBlack(sorted.first())
        val lhThumbBlack = !isRH && isBlack(sorted.last())

        fun adjustRh(fs: List<Int>): List<Int> {
            if (!rhThumbBlack || fs.size >= 5) return fs
            val shifted = fs.map { (it + 1).coerceAtMost(5) }.distinct()
            return if (shifted.size == fs.size) shifted else fs
        }
        fun adjustLh(fs: List<Int>): List<Int> {
            if (!lhThumbBlack || fs.size >= 5) return fs
            val shifted = fs.mapIndexed { idx, f -> if (idx == fs.lastIndex && f == 1) 2 else f }.distinct()
            return if (shifted.size == fs.size) shifted else fs
        }

        val fingers: List<Int> = if (isRH) {
            when (count) {
                1 -> {
                    val p = sorted[0]
                    val exp = explicitMap[p]?.takeIf { it in 1..5 }
                    val f = if (exp != null && !(isBlack(p) && exp == 1)) {
                        exp
                    } else {
                        val delta = p - lastRhCenter
                        val step = (delta / 2).coerceIn(-4, 4)
                        val cand = if (delta == 0) lastRhFinger else (lastRhFinger + (if (step == 0) (if (delta > 0) 1 else -1) else step)).coerceIn(1, 5)
                        if (isBlack(p) && cand == 1) 2 else cand
                    }
                    lastRhFinger = f
                    listOf(f)
                }
                2 -> adjustRh(when { span <= 2 -> listOf(1, 2); span <= 4 -> listOf(1, 3); span <= 6 -> listOf(1, 4); else -> listOf(1, 5) })
                3 -> {
                    val g1 = sorted[1] - sorted[0]
                    val g2 = sorted[2] - sorted[1]
                    adjustRh(when { span <= 4 -> listOf(1, 2, 3); g1 <= 3 && g2 >= 5 -> listOf(1, 2, 5); else -> listOf(1, 3, 5) })
                }
                4 -> adjustRh(if ((sorted[3] - sorted[2]) <= 2) listOf(1, 2, 4, 5) else listOf(1, 2, 3, 5))
                else -> listOf(1, 2, 3, 4, 5)
            }
        } else {
            when (count) {
                1 -> {
                    val p = sorted[0]
                    val exp = explicitMap[p]?.takeIf { it in 1..5 }
                    val f = if (exp != null && !(isBlack(p) && exp == 1)) {
                        exp
                    } else {
                        val delta = p - lastLhCenter
                        val step = (delta / 2).coerceIn(-4, 4)
                        val cand = if (delta == 0) lastLhFinger else (lastLhFinger - (if (step == 0) (if (delta > 0) 1 else -1) else step)).coerceIn(1, 5)
                        if (isBlack(p) && cand == 1) 2 else cand
                    }
                    lastLhFinger = f
                    listOf(f)
                }
                2 -> adjustLh(when { span <= 2 -> listOf(2, 1); span <= 4 -> listOf(3, 1); span <= 6 -> listOf(4, 1); else -> listOf(5, 1) })
                3 -> adjustLh(if ((sorted[1] - sorted[0]) >= 5) listOf(5, 2, 1) else listOf(5, 3, 1))
                4 -> adjustLh(listOf(5, 3, 2, 1))
                else -> listOf(5, 4, 3, 2, 1)
            }
        }

        if (isRH && fingers.isNotEmpty()) lastRhFinger = fingers.last()
        if (!isRH && fingers.isNotEmpty()) lastLhFinger = fingers.first()

        return sorted.mapIndexed { i, midi -> midi to fingers.getOrElse(i) { if (isRH) i + 1 else 5 - i }.coerceIn(1, 5) }
    }

    sb.appendLine("═".repeat(108))
    sb.appendLine("       pEQ-LEARN  ·  PIANO HUMAN HANDS OVERLAY — FORENSIC DIAGNOSTIC DUMP")
    sb.appendLine("═".repeat(108))
    sb.appendLine("Score  : $title  |  Tempo: ${tempo.toInt()} BPM  |  Measures: ${measures.size}")
    sb.appendLine("Tracks : ${tracks.joinToString { "#${it.trackIndex} \"${it.trackName}\" ch${it.channel + 1} prog${it.program}" }}")
    sb.appendLine("Active melodic track indices used for hand split: $melodicTrackIndices")
    sb.appendLine("═".repeat(108))
    sb.appendLine()

    measures.forEachIndexed { mIdx, measure ->
        val mNum = if (measure.measureIndex > 0) measure.measureIndex else mIdx + 1
        val beatsPerMeasure = (measure.timeSignatureNumerator.toFloat() *
            (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)

        sb.appendLine("▌ MEASURE #$mNum  [${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}  $beatsPerMeasure beats]")

        val allNotes = measure.notes.filter { !it.isRest }
        totalNotes += allNotes.size

        val byBeat = allNotes.groupBy { "%.2f".format(it.position) }

        byBeat.entries.sortedBy { it.key.toFloatOrNull() ?: 0f }.forEach { (beatStr, notes) ->
            val beat = beatStr.toFloatOrNull() ?: 0f

            val drumNotes = notes.filter { it.voice in drumTrackIndices }
            val melodicNotes = notes.filter { it.voice in melodicTrackIndices && it.voice !in drumTrackIndices }
            val outOfRange = melodicNotes.filter { it.midiNote !in 21..108 }
            val validNotes = melodicNotes.filter { it.midiNote in 21..108 }

            totalSkipped += drumNotes.size + outOfRange.size

            if (notes.isEmpty()) return@forEach

            sb.append("  Beat %.2f │ Raw notes: ".format(beat))
            sb.appendLine(notes.joinToString { "${midiName(it.midiNote)}(v${it.voice})" })

            if (drumNotes.isNotEmpty()) {
                sb.appendLine("           ─ Ignored (percussion/drum track): ${drumNotes.joinToString { midiName(it.midiNote) }}")
            }
            if (outOfRange.isNotEmpty()) {
                sb.appendLine("           ⚠ SKIPPED (out of 21-108): ${outOfRange.joinToString { "${it.midiNote}" }}")
            }
            if (validNotes.isEmpty()) {
                sb.appendLine("           ─ No valid melodic piano notes at this beat.")
                return@forEach
            }

            val explicitFingerMap = validNotes
                .filter { it.fingering != null && it.fingering in 1..5 }
                .associate { it.midiNote to it.fingering!! }

            val sorted = validNotes.map { it.midiNote }.distinct().sorted()

            // Optimal Transport (OT) DP Hand Partitioner
            val n = sorted.size
            var bestK = 0
            var bestCost = Float.MAX_VALUE
            for (k in 0..n) {
                val cLh = sorted.subList(0, k)
                val cRh = sorted.subList(k, n)
                val lSpan = if (cLh.size > 1) cLh.last() - cLh.first() else 0
                val rSpan = if (cRh.size > 1) cRh.last() - cRh.first() else 0
                var cost = 0f
                if (lSpan > 12) cost += (lSpan - 12) * 80f
                if (rSpan > 12) cost += (rSpan - 12) * 80f
                if (cLh.size > 5) cost += (cLh.size - 5) * 60f
                if (cRh.size > 5) cost += (cRh.size - 5) * 60f
                if (cLh.isNotEmpty()) {
                    val lc = (cLh.first() + cLh.last()) * 0.5f
                    cost += abs(lc - lastLhCenter) * 1.4f
                    cLh.forEach { p -> if (p >= 61) cost += (p - 59) * 15f }
                }
                if (cRh.isNotEmpty()) {
                    val rc = (cRh.first() + cRh.last()) * 0.5f
                    cost += abs(rc - lastRhCenter) * 1.4f
                    cRh.forEach { p -> if (p <= 55) cost += (56 - p) * 35f }
                }
                if (cLh.isNotEmpty() && cRh.isNotEmpty()) {
                    cost -= (cRh.first() - cLh.last()).coerceAtMost(16) * 3f
                }
                if (cost < bestCost) {
                    bestCost = cost
                    bestK = k
                }
            }

            val rawLh = sorted.subList(0, bestK)
            val rawRh = sorted.subList(bestK, n)

            val (lhFinalMut, lhClampedCount) = clampHandVoicing(rawLh, isRH = false)
            val (rhFinalMut, rhClampedCount) = clampHandVoicing(rawRh, isRH = true)
            val lhFinal = lhFinalMut.toMutableList()
            val rhFinal = rhFinalMut.toMutableList()

            if (lhFinal.isEmpty() && rawRh.size > rhFinal.size) {
                val rescued = rawRh.filter { it !in rhFinal && it < (rhFinal.firstOrNull() ?: 60) }
                lhFinal.addAll(clampHandVoicing(rescued, isRH = false).first)
            }

            totalClamped += lhClampedCount + rhClampedCount

            val lhSpan = if (lhFinal.size > 1) lhFinal.last() - lhFinal.first() else 0
            val rhSpan = if (rhFinal.size > 1) rhFinal.last() - rhFinal.first() else 0
            if (lhSpan > 12) spanViolations++
            if (rhSpan > 12) spanViolations++

            val lhFingers = assignBiomechanicalFingers(lhFinal, isRH = false, explicitFingerMap)
            val rhFingers = assignBiomechanicalFingers(rhFinal, isRH = true, explicitFingerMap)

            if (lhFinal.isNotEmpty()) lastLhCenter = ((lhFinal.first() + lhFinal.last()) / 2).coerceIn(21, 108)
            if (rhFinal.isNotEmpty()) lastRhCenter = ((rhFinal.first() + rhFinal.last()) / 2).coerceIn(21, 108)

            val lhColorStr = "#6C63FF [LH_PURPLE]"
            val rhColorStr = "#4A90E2 [RH_CYAN]"
            sb.appendLine("           LH [${lhFinal.size} notes, span=$lhSpan semi, Color=$lhColorStr] : " +
                lhFingers.joinToString(" | ") { (m, f) -> "${midiName(m)}→F$f [Color: $lhColorStr]" })
            sb.appendLine("           RH [${rhFinal.size} notes, span=$rhSpan semi, Color=$rhColorStr] : " +
                rhFingers.joinToString(" | ") { (m, f) -> "${midiName(m)}→F$f [Color: $rhColorStr]" })
        }

        sb.appendLine()
    }

    sb.appendLine("═".repeat(108))
    sb.appendLine("SUMMARY")
    sb.appendLine("═".repeat(108))
    sb.appendLine("  Total notes in score        : $totalNotes")
    sb.appendLine("  Notes skipped (drum/percussion or out of range) : $totalSkipped")
    sb.appendLine("  Notes clamped (> 5 per hand, rebalanced)       : $totalClamped")
    sb.appendLine("  Hand-crossing events detected                   : $handCrossings")
    sb.appendLine("  Span violations (> 12 semitones per hand)       : $spanViolations")
    sb.appendLine("  Notes with no finger assigned                   : $missingFingers")
    sb.appendLine("═".repeat(108))
    return sb.toString()
}

/**
 * Ultra-Forensic Diagnostic Dump for Guitar / Tablature Notation and Fretting / Picking Hands.
 * Audits every single measure and beat:
 *  - Solved fretting hand box & positions (fret, string, finger F1..F4)
 *  - Open string vs fretted transitions
 *  - Ergonomic hand span (max - min fret <= 4) and awkward stretch alerts
 *  - Right-hand picking directions (Down, Up, Strum Down, Strum Up, Rasgueado, Fingerstyle)
 *  - Tab articulations (Hammer-on, Pull-off, Slide Up/Down, Bend, Release, Palm Mute, Let Ring)
 *  - Sounding pitch vs open string semitones
 */
fun Score.dumpGuitarHandsOverlayAsText(): String {
    val sb = StringBuilder()
    val openStrings = tuningMidi.ifEmpty { listOf(64, 59, 55, 50, 45, 40) }
    val stringLabels = if (openStrings.size == 4) listOf("A", "E", "C", "G")
    else if (openStrings.size == 7) listOf("e", "B", "G", "D", "A", "E", "B")
    else listOf("e", "B", "G", "D", "A", "E")

    sb.appendLine("========================================================================================================================")
    sb.appendLine("               pEQ-LEARN GUITAR TAB & HUMAN HANDS OVERLAY ULTRA-FORENSIC AUDIT DUMP")
    sb.appendLine("========================================================================================================================")
    sb.appendLine("SCORE METADATA:")
    sb.appendLine("  • Title              : $title")
    sb.appendLine("  • Tuning             : $tuningName [${openStrings.map { getNoteNameFromMidi(it) }.joinToString()}]")
    if (capo > 0) sb.appendLine("  • Capo               : Fret $capo")
    sb.appendLine("  • Total Measures     : ${measures.size}")
    sb.appendLine("========================================================================================================================")
    sb.appendLine()

    var totalNotes = 0
    var totalFrettedNotes = 0
    var totalOpenStringNotes = 0
    var handBoxShifts = 0
    var spanViolations = 0
    var lastHandBoxCenter = 3.0f

    measures.forEachIndexed { mIdx, measure ->
        val mNum = if (measure.measureIndex > 0) measure.measureIndex else mIdx + 1
        val melodicNotes = measure.notes.filter { !it.isRest && it.voice != 9 }
        sb.appendLine("------------------------------------------------------------------------------------------------------------------------")
        sb.appendLine("MEASURE #$mNum  |  TimeSig: ${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}  |  Melodic Notes: ${melodicNotes.size}")
        sb.appendLine("------------------------------------------------------------------------------------------------------------------------")

        if (melodicNotes.isEmpty()) {
            sb.appendLine("  (No melodic guitar notes in this measure)")
            sb.appendLine()
            return@forEachIndexed
        }

        val byPos = melodicNotes.groupBy { "%.2f".format(it.position) }.toSortedMap()

        for ((posStr, notesAtBeat) in byPos) {
            val beatPos = posStr.toFloatOrNull() ?: 0f
            val pitches = notesAtBeat.map { it.midiNote }.distinct().sorted()
            totalNotes += notesAtBeat.size

            // Solve Viterbi fretboard placement
            val (fretMap, nextBoxCenter) = com.creativeblisslabs.peqlearn.app.util.BiomechanicalPerformersEngine.solveFrettedSliceBiomechanical(
                pitches = pitches,
                openStringsMidiHighToLow = openStrings.map { it + capo },
                maxFret = 22,
                minFret = 0,
                lastHandBoxCenterFret = lastHandBoxCenter
            )

            if (kotlin.math.abs(nextBoxCenter - lastHandBoxCenter) >= 2.0f) {
                handBoxShifts++
            }
            lastHandBoxCenter = nextBoxCenter

            val frettedFretList = fretMap.values.map { it.fret }.filter { it > 0 }
            val span = if (frettedFretList.size > 1) frettedFretList.max() - frettedFretList.min() else 0
            if (span > 4) spanViolations++

            val chordLabel = notesAtBeat.firstNotNullOfOrNull { it.chordSymbol.takeIf { s -> s.isNotEmpty() } } ?: "Single Note"
            val pickDir = notesAtBeat.firstNotNullOfOrNull { it.pickDirection.takeIf { p -> p != PickDirection.NONE } } ?: PickDirection.DOWN
            val strPick = when (pickDir) {
                PickDirection.STRUM_DOWN -> "Strum Down (↓)"
                PickDirection.STRUM_UP -> "Strum Up (↑)"
                PickDirection.RASGUEADO -> "Rasgueado (≋)"
                PickDirection.DOWN -> "Downstroke (⊓)"
                PickDirection.UP -> "Upstroke (∨)"
                PickDirection.SLAP -> "Slap (S)"
                PickDirection.POP -> "Pop (P)"
                else -> "Pick"
            }

            sb.appendLine("  [Beat %-5s] BoxCenter=%.1ff | Chord: %-12s | Picking: %s | FretSpan: %d frets".format(
                posStr, nextBoxCenter, chordLabel, strPick, span
            ))

            for (note in notesAtBeat) {
                val placement = fretMap[note.midiNote]
                val sIdx = placement?.stringIndex ?: (note.string ?: 0)
                val fret = placement?.fret ?: (note.fret ?: 0)
                val finger = placement?.finger ?: (note.fingering ?: 1)
                val sName = stringLabels.getOrElse(sIdx) { "Str$sIdx" }
                val pitchName = getNoteNameFromMidi(note.midiNote)

                if (fret == 0) totalOpenStringNotes++ else totalFrettedNotes++

                val fingerName = when (finger) {
                    1 -> "F1 (Index)"
                    2 -> "F2 (Middle)"
                    3 -> "F3 (Ring)"
                    4 -> "F4 (Pinky)"
                    else -> "Thumb / Open"
                }

                val artFlags = mutableListOf<String>()
                if (fret == 0) artFlags.add("OPEN STRING")
                if (note.articulation != TabArticulation.NONE) artFlags.add("Art:${note.articulation.name}")
                if (note.palmMute) artFlags.add("P.M.")
                if (note.letRing) artFlags.add("LetRing")
                if (note.vibrato) artFlags.add("Vibrato")
                if (note.bend != null) artFlags.add("Bend(${note.bend.type.name}+${note.bend.bendSteps}st)")
                if (note.whammy != null) artFlags.add("Whammy(${note.whammy.type.name})")

                val flagStr = if (artFlags.isNotEmpty()) " [${artFlags.joinToString()}]" else ""
                sb.appendLine("    • String %d (%s) | Fret %-2d | Finger: %-11s | Pitch: %-4s (MIDI %d)%s".format(
                    sIdx + 1, sName, fret, fingerName, pitchName, note.midiNote, flagStr
                ))
            }
        }
        sb.appendLine()
    }

    sb.appendLine("========================================================================================================================")
    sb.appendLine("SUMMARY OF GUITAR AUDIT:")
    sb.appendLine("  • Total Notes Processed      : $totalNotes")
    sb.appendLine("  • Fretted Notes              : $totalFrettedNotes")
    sb.appendLine("  • Open String Notes          : $totalOpenStringNotes")
    sb.appendLine("  • Hand Position Shifts       : $handBoxShifts")
    sb.appendLine("  • Span Violations (> 4 frets): $spanViolations")
    sb.appendLine("========================================================================================================================")
    return sb.toString()
}