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
    CONCEPTS("Theory Concepts", Icons.Default.MenuBook, Color(0xFF64B5F6)),
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
    const val DEAD_NOTE = "X"

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
    const val FERMATA = "𝄐"
    const val STACCATO = "•"
    const val TENUTO = "—"
    const val ACCENT = ">"
    const val MARCATO = "^"

    // Rests
    const val SEMIBREVE_REST = "𝄻"
    const val MINIM_REST = "𝄼"
    const val CROTCHET_REST = "𝄽"
    const val QUAVER_REST = "𝄾"
    const val SEMIQUAVER_REST = "𝄿"

    // Dynamics
    const val PIANO = "𝆏"
    const val FORTE = "𝆑"

    // Articulation/Repeats
    const val SEGNO = "𝄋"
    const val CODA = "𝄊"
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
    SUSPENDED
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

// DrawingConfig is defined in ScoreModels.kt

data class VisualizerModeItem(
    val id: String,
    val label: String,
    val isEnabled: Boolean = true
)
