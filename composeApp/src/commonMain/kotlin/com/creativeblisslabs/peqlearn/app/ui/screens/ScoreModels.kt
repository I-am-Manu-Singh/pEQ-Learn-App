package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

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

