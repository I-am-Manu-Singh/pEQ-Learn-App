package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TheoryPart(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    CHORDS("Chord Dict.", Icons.Default.MusicNote, Color(0xFFFFD700)),
    REFERENCE("Reference", Icons.Default.MenuBook, Color(0xFF64B5F6)),
    TOOLS("Tools", Icons.Default.Build, Color(0xFF4CAF50)),
    PROGRESSIONS("Progressions", Icons.Default.AltRoute, Color(0xFFAB47BC)),
    CONCEPTS("Theory Concepts", Icons.Default.AutoStories, Color(0xFF29B6F6)),
    GROOVE("Drums", Icons.Default.PlayArrow, Color(0xFF00E676)),
    STRUCTURE("Song Str.", Icons.AutoMirrored.Filled.List, Color(0xFFFF7043)),
    INDIAN_CLASSICAL("Ind. Class.", Icons.Default.Home, Color(0xFFFF9800)),
    KARNATIC("Karnatic", Icons.Default.Info, Color(0xFF00BCD4))
}

data class StructureBlock(
    val label: String,
    val bars: String
)

data class TheoryConcept(
    val emoji: String,
    val title: String,
    val shortDef: String,
    val longDef: String,
    val part: TheoryPart,
    val highlightNotes: List<Int> = emptyList(),
    val structureBlocks: List<StructureBlock> = emptyList(),
    val formula: String = "",
    val noteHighlights: List<String> = emptyList()
) {
    val id: String get() = title
    val subtitle: String get() = shortDef
    val category: String get() = part.label
    val description: String get() = longDef
}

object MusicSymbols {
    const val RELEASE = "R"
    const val PULL_OFF = "P"
    const val HAMMER_ON = "H"
    const val SLIDE_UP = "/"
    const val SLIDE_DOWN = "\\"
    const val VIBRATO = "∿∿∿"
    const val PALM_MUTE = "P.M."
    const val LET_RING = "let ring"
    const val DEAD_NOTE = "X"
    const val TAP = "T"
    const val SLAP = "S"
    const val POP = "P"

    // Picking & Strumming
    const val PICK_DOWN = "⊓"
    const val PICK_UP = "∨"
    const val STRUM_DOWN = "↓"
    const val STRUM_UP = "↑"

    // Accidentals & Clefs
    const val SHARP = "♯"
    const val FLAT = "♭"
    const val NATURAL = "♮"
    const val DOUBLE_SHARP = "𝄪"
    const val DOUBLE_FLAT = "𝄫"
    const val TREBLE_CLEF = "𝄞"
    const val BASS_CLEF = "𝄢"
    const val ALTO_CLEF = "𝄡"
    const val COMMON_TIME = "𝄴"
    const val CUT_TIME = "𝄵"

    // Articulations & Ornaments
    const val FERMATA = "𝄐"
    const val STACCATO = "•"
    const val STACCATISSIMO = "▾"
    const val TENUTO = "—"
    const val ACCENT = ">"
    const val MARCATO = "^"
    const val TRILL = "tr"
    const val MORDENT = "ffe"
    const val TURN = "𝄗"
    const val ARPEGGIO = "𝆃"

    // Octave Shifts & Pedal
    const val OCTAVE_8VA = "8va"
    const val OCTAVE_8VB = "8vb"
    const val OCTAVE_15MA = "15ma"
    const val OCTAVE_15MB = "15mb"
    const val PEDAL_DOWN = "Ped."
    const val PEDAL_UP = "*"

    // Rests
    const val SEMIBREVE_REST = "𝄻"
    const val MINIM_REST = "𝄼"
    const val CROTCHET_REST = "𝄽"
    const val QUAVER_REST = "𝄾"
    const val SEMIQUAVER_REST = "𝄿"

    // Dynamics
    const val PIANO = "𝆏"
    const val FORTE = "𝆑"
    const val MEZZO = "𝆐"
    const val RINFORZANDO = "𝆒"
    const val SFORZANDO = "𝆔"

    // Navigation & Repeats
    const val SEGNO = "𝄋"
    const val CODA = "𝄌"
    const val REPEAT_DOT = "𝄇"
}

data class Suggestion(
    val title: String,
    val subtitle: String,
    val targetTab: String = "Learn"
)

enum class HarmonicType {
    TRIAD,
    SEVENTH,
    EXTENDED,
    ALTERED,
    SUSPENDED,
    // Guitar / String Harmonic Articulation Types (Backward & Forward Compatible)
    NATURAL,
    ARTIFICIAL,
    PINCH,
    TAPPED,
    SEMI
}

enum class FretboardViewMode {
    SCALE_MAP,
    CAGED_BOX,
    CHORD_VOICING,
    SCALE_DEGREES,
    TRIAD_INVERSIONS,
    ARPEGGIO_SHAPES
}

data class UnifiedShape(
    val name: String,
    val pitches: List<Int>,
    val instrumentType: String = "GUITAR",
    val guitarShape: com.creativeblisslabs.peqlearn.app.util.GuitarShape? = null,
    val pianoNotes: List<Int>? = null
)

data class SparkParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val alpha: Float = 1.0f,
    val colorHex: Long = 0xFFFFD700
)

data class VisualizerModeItem(
    val id: String,
    val label: String,
    val isEnabled: Boolean = true
)