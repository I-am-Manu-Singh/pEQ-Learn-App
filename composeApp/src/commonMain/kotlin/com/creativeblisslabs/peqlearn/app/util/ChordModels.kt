package com.creativeblisslabs.peqlearn.app.util

import androidx.compose.ui.graphics.Color

enum class ChordInstrument(val displayName: String, val stringCount: Int) {
    GUITAR("Guitar (6-String)", 6),
    UKULELE("Ukulele (4-String)", 4),
    PIANO("Piano / Keyboard", 88);

    val label: String get() = when (this) {
        GUITAR -> "Guitar"
        PIANO -> "Piano"
        UKULELE -> "Ukulele"
    }
}

enum class InversionStyle(val label: String, val bassDegree: Int = 0) {
    ROOT("Root Position", 0),
    FIRST("1st Inversion (3rd in Bass)" , 1),
    SECOND("2nd Inversion (5th in Bass)", 2),
    THIRD("3rd Inversion (7th in Bass)", 3),
    INV1("1st Inversion", 1),
    INV2("2nd Inversion", 2),
    INV3("3rd Inversion", 3)
}

enum class GuitarVoicingStyle(val label: String) {
    OPEN("Open / Folk Voicings"),
    BARRE_E("E-Shape Barre"),
    BARRE_A("A-Shape Barre"),
    DROP_2("Drop 2 (Jazz / Studio)"),
    DROP_3("Drop 3 (Rich Bass)"),
    TRIAD("Close Triad")
}

enum class PianoVoicingStyle(val label: String) {
    CLOSE("Close Position"),
    OPEN("Open Position"),
    SHELL("Shell Voicing (Root-3-7)"),
    TWO_HAND("Two-Hand Spread"),
    ROOTLESS("Rootless"),
    CLOSED("Closed"),
    TENSION("Tension")
}

enum class UkeVoicingStyle(val label: String) {
    STANDARD("Standard Open"),
    BARRE("Barre Shape"),
    UPPER_FRET("Upper Neck")
}
enum class ChordDiff(val label: String, val color: Color) {
    EASY("Easy",     Color(0xFF4CAF50)),
    MEDIUM("Medium", Color(0xFFFF9800)),
    HARD("Advanced", Color(0xFFF44336))
}

data class GuitarFingering(
    val stringIndex: Int = 0,
    val fret: Int = 0,
    val finger: Int = 0, // 1=Index, 2=Middle, 3=Ring, 4=Pinky, 0=Open
    val frets: List<Int> = emptyList(),
    val fingers: List<Int> = emptyList()
) {
    constructor(frets: List<Int>, fingers: List<Int>) : this(
        stringIndex = 0,
        fret = frets.firstOrNull() ?: 0,
        finger = fingers.firstOrNull() ?: 0,
        frets = frets,
        fingers = fingers
    )
}

data class GuitarShape(
    val frets: List<Int>,              // -1 means muted, 0 means open
    val fingers: List<Int>,            // 0=open/mute, 1–4=finger number
    val baseFret: Int = 1,
    val barre: Int? = null,            // absolute fret of the barre bar
    val diff: ChordDiff? = null,       // Made optional with a default to avoid breaking instances without a diff
    val tip: String = "",
    
    // Legacy mapping properties preserved for backwards compatibility
    val fingerings: List<Int> = fingers,
    val barres: List<Int> = barre?.let { listOf(it) } ?: emptyList(),
    
    // Dynamic property for generating MIDI values based on standard EADGBE tuning
    val soundingMidiNotes: List<Int> = frets.mapIndexedNotNull { idx, f ->
        if (f >= 0) {
            val standardTuning = listOf(40, 45, 50, 55, 59, 64) // E2, A2, D3, G3, B3, E4
            val openPitch = standardTuning.getOrElse(idx) { 40 + idx * 5 }
            openPitch + (if (f == 0) 0 else f + baseFret - 1)
        } else null
    }
)
data class UkuShape(
    val frets: List<Int>,
    val fingers: List<Int> = emptyList(),
    val baseFret: Int = 1,
    val diff: ChordDiff? = null // Made optional with a default to avoid breaking existing instances
)


data class ScaleBoxPosition(
    val boxIndex: Int = 0,
    val rootString: Int = 0,
    val startFret: Int = 0,
    val endFret: Int = 0,
    val label: String = "",
    val stringRanges: List<IntRange> = emptyList()
) {
    constructor(label: String, stringRanges: List<IntRange>) : this(
        boxIndex = 0,
        rootString = 0,
        startFret = stringRanges.firstOrNull()?.first ?: 0,
        endFret = stringRanges.firstOrNull()?.last ?: 0,
        label = label,
        stringRanges = stringRanges
    )
}

data class MidiNote(
    val pitch: Int,
    val startTick: Long,
    val endTick: Long,
    val velocity: Int = 80,
    val channel: Int = 0
)

data class ChordTypeDef(
    val name: String,
    val suffix: String,            // e.g. "m", "7", "maj7"
    val description: String = "",  // description from music theory
    val intervals: List<Int> = emptyList() // semitone offsets
) {
    // Exposes 'label' as an alias for 'name' to satisfy both data class shapes
    val label: String get() = name
}
internal val GUITAR_SHAPES_DB: Map<Pair<String, ChordDiff>, GuitarShape> = buildMap {
    // --- C Major Family ---
    put("C" to ChordDiff.EASY, GuitarShape(listOf(-1,3,2,0,1,0), listOf(0,3,2,0,1,0), diff=ChordDiff.EASY, tip="Standard open C chord."))
    put("C" to ChordDiff.MEDIUM, GuitarShape(listOf(-1,3,5,5,5,3), listOf(0,1,2,3,4,1), baseFret=3, barre=3, diff=ChordDiff.MEDIUM, tip="A-form barre shape on the 3rd fret."))
    put("C" to ChordDiff.HARD, GuitarShape(listOf(8,10,10,9,8,8), listOf(1,3,4,2,1,1), baseFret=8, barre=8, diff=ChordDiff.HARD, tip="E-form barre shape on the 8th fret."))
    put("Cm" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,1,0,1,3), listOf(0,0,1,0,2,4), diff=ChordDiff.EASY, tip="Easy Cm shape on top strings."))
    put("Cm" to ChordDiff.MEDIUM, GuitarShape(listOf(-1,3,5,5,4,3), listOf(0,1,3,4,2,1), baseFret=3, barre=3, diff=ChordDiff.MEDIUM, tip="A-form minor barre shape at the 3rd fret."))
    put("Cm" to ChordDiff.HARD, GuitarShape(listOf(8,10,10,8,8,8), listOf(1,3,4,1,1,1), baseFret=8, barre=8, diff=ChordDiff.HARD, tip="E-form minor barre shape at the 8th fret."))
    put("C7" to ChordDiff.EASY, GuitarShape(listOf(-1,3,2,3,1,0), listOf(0,3,2,4,1,0), diff=ChordDiff.EASY, tip="Open C layout adding pinky flat note."))
    put("Cmaj7" to ChordDiff.EASY, GuitarShape(listOf(-1,3,2,0,0,0), listOf(0,3,2,0,0,0), diff=ChordDiff.EASY, tip="C chord without index. Dreamy sound."))

    // --- D Major Family ---
    put("D" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,0,2,3,2), listOf(0,0,0,1,3,2), diff=ChordDiff.EASY, tip="Triangle shape. Mute the two bass strings."))
    put("D" to ChordDiff.MEDIUM, GuitarShape(listOf(-1,5,7,7,7,5), listOf(0,1,3,4,2,1), baseFret=5, barre=5, diff=ChordDiff.MEDIUM, tip="A-form barre at 5th fret."))
    put("D" to ChordDiff.HARD, GuitarShape(listOf(10,12,12,11,10,10), listOf(1,3,4,2,1,1), baseFret=10, barre=10, diff=ChordDiff.HARD, tip="E-form barre at 10th fret."))
    put("Dm" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,0,2,3,1), listOf(0,0,0,2,3,1), diff=ChordDiff.EASY, tip="Like D major but flatten the high e string."))
    put("Dm" to ChordDiff.MEDIUM, GuitarShape(listOf(-1,5,7,7,6,5), listOf(0,1,3,4,2,1), baseFret=5, barre=5, diff=ChordDiff.MEDIUM, tip="A-form minor barre at 5th fret."))
    put("Dm" to ChordDiff.HARD, GuitarShape(listOf(10,12,12,10,10,10), listOf(1,3,4,1,1,1), baseFret=10, barre=10, diff=ChordDiff.HARD, tip="E-form minor barre at 10th fret."))
    put("D7" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,0,2,1,2), listOf(0,0,0,2,1,3), diff=ChordDiff.EASY, tip="Open classic triangle shape variant."))

    // --- E Major Family ---
    put("E" to ChordDiff.EASY, GuitarShape(listOf(0,2,2,1,0,0), listOf(0,2,3,1,0,0), diff=ChordDiff.EASY, tip="Stack ring/middle on A-D, index on G."))
    put("E" to ChordDiff.MEDIUM, GuitarShape(listOf(7,7,9,9,9,7), listOf(1,1,2,3,4,1), baseFret=7, barre=7, diff=ChordDiff.MEDIUM, tip="A-form barre at 7th fret."))
    put("E" to ChordDiff.HARD, GuitarShape(listOf(12,14,14,13,12,12), listOf(1,3,4,2,1,1), baseFret=12, barre=12, diff=ChordDiff.HARD, tip="High octave E at 12th fret."))
    put("Em" to ChordDiff.EASY, GuitarShape(listOf(0,2,2,0,0,0), listOf(0,2,3,0,0,0), diff=ChordDiff.EASY, tip="Classic 2-finger chord. Great starter."))
    put("Em" to ChordDiff.MEDIUM, GuitarShape(listOf(7,7,9,9,8,7), listOf(1,1,3,4,2,1), baseFret=7, barre=7, diff=ChordDiff.MEDIUM, tip="A-form minor barre at 7th fret."))
    put("Em" to ChordDiff.HARD, GuitarShape(listOf(12,14,14,12,12,12), listOf(1,3,4,1,1,1), baseFret=12, barre=12, diff=ChordDiff.HARD, tip="High octave minor barre at 12th fret."))
    put("E7" to ChordDiff.EASY, GuitarShape(listOf(0,2,0,1,0,0), listOf(0,2,0,1,0,0), diff=ChordDiff.EASY, tip="Open E major dropping the ring finger."))

    // --- F Major Family ---
    put("F" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,3,2,1,1), listOf(0,0,3,2,1,1), diff=ChordDiff.EASY, tip="Four string beginner F chord."))
    put("F" to ChordDiff.MEDIUM, GuitarShape(listOf(1,3,3,2,1,1), listOf(1,3,4,2,1,1), baseFret=1, barre=1, diff=ChordDiff.MEDIUM, tip="Index finger barre across all 6 strings."))
    put("F" to ChordDiff.HARD, GuitarShape(listOf(-1,8,10,10,10,8), listOf(0,1,2,3,4,1), baseFret=8, barre=8, diff=ChordDiff.HARD, tip="A-form barre at 8th fret."))

    // --- G Major Family ---
    put("G" to ChordDiff.EASY, GuitarShape(listOf(3,2,0,0,0,3), listOf(2,1,0,0,0,3), diff=ChordDiff.EASY, tip="Standard open G chord."))
    put("G" to ChordDiff.MEDIUM, GuitarShape(listOf(3,5,5,4,3,3), listOf(1,3,4,2,1,1), baseFret=3, barre=3, diff=ChordDiff.MEDIUM, tip="E-form barre shape on 3rd fret."))
    put("G" to ChordDiff.HARD, GuitarShape(listOf(-1,10,12,12,12,10), listOf(0,1,2,3,4,1), baseFret=10, barre=10, diff=ChordDiff.HARD, tip="A-form barre shape on 10th fret."))
    put("Gm" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,5,3,3,3), listOf(0,0,3,1,1,1), baseFret=3, barre=3, diff=ChordDiff.EASY, tip="Partial minor barre at the 3rd fret."))
    put("Gm" to ChordDiff.MEDIUM, GuitarShape(listOf(3,5,5,3,3,3), listOf(1,3,4,1,1,1), baseFret=3, barre=3, diff=ChordDiff.MEDIUM, tip="E-form minor barre shape on the 3rd fret."))
    put("Gm" to ChordDiff.HARD, GuitarShape(listOf(-1,10,12,12,11,10), listOf(0,1,3,4,2,1), baseFret=10, barre=10, diff=ChordDiff.HARD, tip="A-form minor barre shape on the 10th fret."))
    put("G7" to ChordDiff.EASY, GuitarShape(listOf(3,2,0,0,0,1), listOf(3,2,0,0,0,1), diff=ChordDiff.EASY, tip="Standard open G Dominant 7th."))

    // --- A Major Family ---
    put("A" to ChordDiff.EASY, GuitarShape(listOf(-1,0,2,2,2,0), listOf(0,0,1,2,3,0), diff=ChordDiff.EASY, tip="Fingers squeezed on 2nd fret—keep upright."))
    put("A" to ChordDiff.MEDIUM, GuitarShape(listOf(5,7,7,6,5,5), listOf(1,3,4,2,1,1), baseFret=5, barre=5, diff=ChordDiff.MEDIUM, tip="E-form barre at 5th fret."))
    put("A" to ChordDiff.HARD, GuitarShape(listOf(-1,12,14,14,14,12), listOf(0,1,2,3,4,1), baseFret=12, barre=12, diff=ChordDiff.HARD, tip="High octave A at 12th fret."))
    put("Am" to ChordDiff.EASY, GuitarShape(listOf(-1,0,2,2,1,0), listOf(0,0,2,3,1,0), diff=ChordDiff.EASY, tip="Same shape as E, shifted one string over."))
    put("Am" to ChordDiff.MEDIUM, GuitarShape(listOf(5,7,7,5,5,5), listOf(1,3,4,1,1,1), baseFret=5, barre=5, diff=ChordDiff.MEDIUM, tip="E-form minor barre at 5th fret."))
    put("Am" to ChordDiff.HARD, GuitarShape(listOf(-1,12,14,14,13,12), listOf(0,1,3,4,2,1), baseFret=12, barre=12, diff=ChordDiff.HARD, tip="High octave Am shape at 12th fret."))
    put("Am7" to ChordDiff.EASY, GuitarShape(listOf(-1,0,2,0,1,0), listOf(0,0,2,0,1,0), diff=ChordDiff.EASY, tip="Am without ring finger. Soulful."))

    // --- B Major Family ---
    put("B" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,4,4,4,2), listOf(0,0,2,3,4,1), baseFret=2, diff=ChordDiff.EASY, tip="Simple 4-string voicing for B."))
    put("B" to ChordDiff.MEDIUM, GuitarShape(listOf(-1,2,4,4,4,2), listOf(0,1,2,3,4,1), baseFret=2, barre=2, diff=ChordDiff.MEDIUM, tip="A-form barre major. Squeeze fingers."))
    put("B" to ChordDiff.HARD, GuitarShape(listOf(7,9,9,8,7,7), listOf(1,3,4,2,1,1), baseFret=7, barre=7, diff=ChordDiff.HARD, tip="E-form barre at 7th fret."))
    put("Bm" to ChordDiff.EASY, GuitarShape(listOf(-1,-1,4,4,3,2), listOf(0,0,3,4,2,1), baseFret=2, diff=ChordDiff.EASY, tip="Simple 4-string voicing for Bm."))
    put("Bm" to ChordDiff.MEDIUM, GuitarShape(listOf(-1,2,4,4,3,2), listOf(0,1,3,4,2,1), baseFret=2, barre=2, diff=ChordDiff.MEDIUM, tip="A-form barre at 2nd fret."))
    put("Bm" to ChordDiff.HARD, GuitarShape(listOf(7,9,9,7,7,7), listOf(1,3,4,1,1,1), baseFret=7, barre=7, diff=ChordDiff.HARD, tip="E-form minor barre at 7th fret."))
}

internal val UKU_SHAPES_DB: Map<Pair<String, ChordDiff>, UkuShape> = buildMap {
    put("C" to ChordDiff.EASY, UkuShape(listOf(0,0,0,3), listOf(0,0,0,3), diff = ChordDiff.EASY))
    put("C" to ChordDiff.MEDIUM, UkuShape(listOf(0,4,3,3), listOf(0,2,1,1), diff = ChordDiff.MEDIUM))
    put("C" to ChordDiff.HARD, UkuShape(listOf(5,4,3,3), listOf(3,2,1,1), diff = ChordDiff.HARD))
    put("Cm" to ChordDiff.EASY, UkuShape(listOf(0,3,3,3), listOf(0,1,2,3), diff = ChordDiff.EASY))

    put("G" to ChordDiff.EASY, UkuShape(listOf(0,2,3,2), listOf(0,1,3,2), diff = ChordDiff.EASY))
    put("G" to ChordDiff.MEDIUM, UkuShape(listOf(4,2,3,2), listOf(3,1,2,1), diff = ChordDiff.MEDIUM))
    put("G" to ChordDiff.HARD, UkuShape(listOf(7,7,7,10), listOf(1,1,1,4), diff = ChordDiff.HARD))

    put("Am" to ChordDiff.EASY, UkuShape(listOf(2,0,0,0), listOf(1,0,0,0), diff = ChordDiff.EASY))
    put("Am" to ChordDiff.MEDIUM, UkuShape(listOf(2,0,0,3), listOf(1,0,0,2), diff = ChordDiff.MEDIUM))
    put("Am" to ChordDiff.HARD, UkuShape(listOf(9,9,8,7), listOf(3,4,2,1), diff = ChordDiff.HARD))
}

// ─────────────────────────────────────────────────────────────────────────────
// SHAPE LOOKUP & DYNAMIC GENERATOR
// ─────────────────────────────────────────────────────────────────────────────

internal fun buildGuitarShape(
    rootIdx: Int, 
    suffix: String, 
    diff: ChordDiff = ChordDiff.EASY,
    voicingStyle: String = "Open", // Default value
    startFret: Int = 1             // Default value
): GuitarShape {
    val rootName = NOTE_NAMES_CD[rootIdx]
    val fullChordKey = "$rootName$suffix"

    if (diff == ChordDiff.EASY && GUITAR_SHAPES_OVERRIDES.containsKey(fullChordKey)) {
        return GUITAR_SHAPES_OVERRIDES[fullChordKey]!!
    }

    val targetTypeDef = CHORD_TYPE_DEFS.find { it.suffix == suffix } ?: CHORD_TYPE_DEFS.first()

    return com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine.generateDynamicShape(
        rootMidiNote = rootIdx,
        targetSemitones = targetTypeDef.intervals,
        difficulty = diff,
        voicingStyle = voicingStyle, // Pass new param
        startFret = startFret,       // Pass new param
        openStringPitches = listOf(4, 9, 2, 7, 11, 4),
        isPiano = false
    )
}

private fun makeEForm(fret: Int, suffix: String, diff: ChordDiff): GuitarShape {
    val bf = maxOf(fret, 1)
    val calculatedDiff = when {
        bf >= 7 -> ChordDiff.HARD
        bf >= 3 -> ChordDiff.MEDIUM
        else -> diff
    }
    return when (suffix) {
        "m"  -> GuitarShape(listOf(bf,bf+2,bf+2,bf,bf,bf),  listOf(1,3,4,1,1,1), bf, bf, calculatedDiff)
        "7"  -> GuitarShape(listOf(bf,bf+2,bf,bf+1,bf,bf),  listOf(1,3,1,2,1,1), bf, bf, calculatedDiff)
        "m7" -> GuitarShape(listOf(bf,bf+2,bf,bf,bf,bf),    listOf(1,3,1,1,1,1), bf, bf, calculatedDiff)
        else -> GuitarShape(listOf(bf,bf+2,bf+2,bf+1,bf,bf),listOf(1,3,4,2,1,1), bf, bf, calculatedDiff)
    }
}

private fun makeAForm(fret: Int, suffix: String, diff: ChordDiff): GuitarShape {
    val bf = maxOf(fret, 1)
    val calculatedDiff = when {
        bf >= 7 -> ChordDiff.HARD
        bf >= 3 -> ChordDiff.MEDIUM
        else -> diff
    }
    return when (suffix) {
        "m"  -> GuitarShape(listOf(-1,bf,bf+2,bf+2,bf+1,bf),listOf(0,1,3,4,2,1), bf, bf, calculatedDiff)
        "7"  -> GuitarShape(listOf(-1,bf,bf+2,bf,bf+2,bf),  listOf(0,1,3,1,4,1), bf, bf, calculatedDiff)
        "m7" -> GuitarShape(listOf(-1,bf,bf+2,bf+1,bf,bf),  listOf(0,1,3,2,1,1), bf, bf, calculatedDiff)
        else -> GuitarShape(listOf(-1,bf,bf+2,bf+2,bf+2,bf),listOf(0,1,2,3,4,1), bf, bf, calculatedDiff)
    }
}

internal fun buildUkuShape(
    rootIdx: Int, 
    suffix: String, 
    diff: ChordDiff = ChordDiff.EASY,
    voicingStyle: String = "Open",
    startFret: Int = 1
): GuitarShape {
    val targetTypeDef = CHORD_TYPE_DEFS.find { it.suffix == suffix } ?: CHORD_TYPE_DEFS.first()

    return com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine.generateDynamicShape(
        rootMidiNote = rootIdx,
        targetSemitones = targetTypeDef.intervals,
        difficulty = diff,
        voicingStyle = voicingStyle, // Pass new param
        startFret = startFret,       // Pass new param
        openStringPitches = listOf(7, 0, 4, 9),
        isPiano = false
    )
}
val NOTE_NAMES_CD = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")

val CHORD_TYPE_DEFS: List<ChordTypeDef> = listOf(
    ChordTypeDef("Major",     "",      "Bright & stable",   listOf(0, 4, 7)),
    ChordTypeDef("Minor",     "m",     "Dark & moody",      listOf(0, 3, 7)),
    ChordTypeDef("Dom 7th",   "7",     "Bluesy tension",    listOf(0, 4, 7, 10)),
    ChordTypeDef("Major 7th", "maj7",  "Lush & jazzy",      listOf(0, 4, 7, 11)),
    ChordTypeDef("Minor 7th", "m7",    "Smooth & soulful",  listOf(0, 3, 7, 10)),
    ChordTypeDef("Sus2",      "sus2",  "Open & floating",   listOf(0, 2, 7)),
    ChordTypeDef("Sus4",      "sus4",  "Unresolved",        listOf(0, 5, 7)),
    ChordTypeDef("Add9",      "add9",  "Open & modern",     listOf(0, 4, 7, 2)),
    ChordTypeDef("Dim",       "dim",   "Tense & unstable",  listOf(0, 3, 6)),
    ChordTypeDef("Aug",       "aug",   "Dreamy tension",    listOf(0, 4, 8)),
    ChordTypeDef("9th",       "9",     "Rich & expansive",  listOf(0, 4, 7, 10, 2)),
    ChordTypeDef("min 9th",   "m9",    "Neo-soul & jazz",   listOf(0, 3, 7, 10, 2)),
    ChordTypeDef("Major 9th", "maj9",  "Ethereal & sweet",  listOf(0, 4, 7, 11, 2)),
    ChordTypeDef("6th",       "6",     "Classic vintage",   listOf(0, 4, 7, 9)),
    ChordTypeDef("Minor 6th", "m6",    "Bitter-sweet mystery", listOf(0, 3, 7, 9)),
    ChordTypeDef("6/9",       "69",    "Soft jazz finish",  listOf(0, 4, 7, 9, 2)),
    ChordTypeDef("7sus4",     "7sus4", "Funk & soul delay", listOf(0, 5, 7, 10)),
    ChordTypeDef("11th",      "11",    "Suspended fusion",  listOf(0, 4, 7, 10, 2, 5)),
    ChordTypeDef("13th",      "13",    "Full jazz palette", listOf(0, 4, 7, 10, 2, 9))
)

val PIANO_RH_FINGERS: Map<String, List<Int>> = mapOf(
    ""      to listOf(1, 3, 5), "m" to listOf(1, 3, 5), "7" to listOf(1, 2, 4, 5),
    "maj7"  to listOf(1, 2, 4, 5), "m7" to listOf(1, 2, 4, 5), "sus2" to listOf(1, 2, 5),
    "sus4"  to listOf(1, 2, 5), "add9" to listOf(1, 2, 3, 5), "dim" to listOf(1, 3, 5),
    "aug"   to listOf(1, 3, 5), "9" to listOf(1, 2, 3, 4, 5), "m9" to listOf(1, 2, 3, 4, 5),
    "maj9"  to listOf(1, 2, 3, 4, 5), "6" to listOf(1, 2, 3, 5), "m6" to listOf(1, 2, 3, 5),
    "69"    to listOf(1, 2, 3, 4, 5), "7sus4" to listOf(1, 2, 4, 5), "11th" to listOf(1, 2, 3, 4, 5),
    "13th"  to listOf(1, 2, 3, 4, 5)
)

val PIANO_LH_FINGERS: Map<String, List<Int>> = mapOf(
    ""      to listOf(5, 3, 1), "m" to listOf(5, 3, 1), "7" to listOf(5, 4, 2, 1),
    "maj7"  to listOf(5, 4, 2, 1), "m7" to listOf(5, 4, 2, 1), "sus2" to listOf(5, 3, 1),
    "sus4"  to listOf(5, 2, 1), "add9" to listOf(5, 3, 2, 1), "dim" to listOf(5, 3, 1),
    "aug"   to listOf(5, 3, 1), "9" to listOf(5, 4, 3, 2, 1), "m9" to listOf(5, 4, 3, 2, 1),
    "maj9"  to listOf(5, 4, 3, 2, 1), "6" to listOf(5, 4, 2, 1), "m6" to listOf(5, 4, 2, 1),
    "69"    to listOf(5, 4, 3, 2, 1), "7sus4" to listOf(5, 3, 2, 1), "11th" to listOf(5, 4, 3, 2, 1),
    "13th"  to listOf(5, 4, 3, 2, 1)
)

val GUITAR_LH_FINGERS: Map<String, GuitarFingering> = mapOf(
    "" to GuitarFingering(listOf(0, 3, 2, 0, 1, 0), listOf(0, 3, 2, 0, 1, 0)),
    "m" to GuitarFingering(listOf(0, 2, 2, 0, 0, 0), listOf(0, 2, 3, 0, 0, 0)),
    "7" to GuitarFingering(listOf(0, 2, 0, 1, 0, 0), listOf(0, 2, 0, 1, 0, 0)),
    "maj7" to GuitarFingering(listOf(0, 3, 2, 0, 0, 0), listOf(0, 3, 2, 0, 0, 0)),
    "m7" to GuitarFingering(listOf(0, 0, 2, 0, 1, 0), listOf(0, 0, 2, 0, 1, 0)),
    "sus2" to GuitarFingering(listOf(-1, 0, 2, 2, 0, 0), listOf(0, 0, 1, 2, 0, 0)),
    "sus4" to GuitarFingering(listOf(0, 2, 2, 2, 0, 0), listOf(0, 1, 2, 3, 0, 0))
)

val GUITAR_SHAPES_OVERRIDES: Map<String, GuitarShape> = buildMap {
    GUITAR_SHAPES_DB.forEach { (key, shape) ->
        put(key.first, shape)
    }
}

val INTERVALS_DATA = listOf(
    listOf("0", "Unison",       "P1",  "Same note",           "0"),
    listOf("1", "Minor 2nd",    "m2",  "Half step up",        "1"),
    listOf("2", "Major 2nd",    "M2",  "Whole step up",       "2"),
    listOf("3", "Minor 3rd",    "m3",  "Minor chord middle",  "3"),
    listOf("4", "Major 3rd",    "M3",  "Major chord middle",  "4"),
    listOf("5", "Perfect 4th",  "P4",  "Sus4 note",           "5"),
    listOf("6", "Tritone",      "A4/d5","Dissonant centre",   "6"),
    listOf("7", "Perfect 5th",  "P5",  "Power chord",         "7"),
    listOf("8", "Minor 6th",    "m6",  "Augmented chord",     "8"),
    listOf("9", "Major 6th",    "M6",  "Added 6th flavour",   "9"),
    listOf("10","Minor 7th",    "m7",  "Dominant 7th note",   "10"),
    listOf("11","Major 7th",    "M7",  "Maj7 jazzy note",     "11"),
    listOf("12","Octave",       "P8",  "Same note up high",   "12")
)

val SCALE_MAJOR_PATTERN        = listOf(0, 2, 4, 5, 7, 9, 11)
val SCALE_MINOR_NAT_PATTERN    = listOf(0, 2, 3, 5, 7, 8, 10)
val SCALE_MINOR_HAR_PATTERN    = listOf(0, 2, 3, 5, 7, 8, 11)
val SCALE_MINOR_MEL_PATTERN    = listOf(0, 2, 3, 5, 7, 9, 11)
val SCALE_PENTATONIC_M_PATTERN = listOf(0, 2, 4, 7, 9)
val SCALE_PENTATONIC_N_PATTERN = listOf(0, 3, 5, 7, 10)
val SCALE_BLUES_PATTERN        = listOf(0, 3, 5, 6, 7, 10)
val SCALE_DORIAN_PATTERN       = listOf(0, 2, 3, 5, 7, 9, 10)
val SCALE_MIXOLYDIAN_PATTERN   = listOf(0, 2, 4, 5, 7, 9, 10)
val SCALE_LYDIAN_PATTERN        = listOf(0, 2, 4, 6, 7, 9, 11)
val SCALE_PHRYGIAN_PATTERN      = listOf(0, 1, 3, 5, 7, 8, 10)
val SCALE_BHAIRAVI_PATTERN      = listOf(0, 1, 3, 5, 7, 8, 10)
val SCALE_BHAIRAV_PATTERN       = listOf(0, 1, 4, 5, 7, 8, 11)
val SCALE_KALYAN_PATTERN        = listOf(0, 2, 4, 6, 7, 9, 11)
val SCALE_MARWA_PATTERN         = listOf(0, 1, 4, 6, 7, 9, 11)
val SCALE_POORVI_PATTERN        = listOf(0, 1, 4, 6, 7, 8, 11)
val SCALE_TODI_PATTERN          = listOf(0, 1, 3, 6, 7, 8, 11)


