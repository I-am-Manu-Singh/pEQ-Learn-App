package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.foundation.Canvas
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import com.creativeblisslabs.peqlearn.app.ui.components.VideoExportStudioSection
import com.creativeblisslabs.peqlearn.app.ui.components.PdfExportStudioSection
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedTablaVisualizer
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.zIndex
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
import com.creativeblisslabs.peqlearn.app.ui.components.pEQInstrumentDropdown
import com.creativeblisslabs.peqlearn.app.ui.components.pEQDynamicInstrumentVisualizer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Hearing
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.launch
import com.creativeblisslabs.peqlearn.app.ui.components.FullscreenDialog
import com.creativeblisslabs.peqlearn.app.ui.components.setScreenOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.setSystemBarsVisibility
import com.creativeblisslabs.peqlearn.app.ui.components.pEQOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedGuitarFretboard
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedHarmoniumVisualizer
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedViolinFingerboard
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedFluteVisualizer
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.playMediaItem
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.persistSession
import com.creativeblisslabs.peqlearn.app.util.LiveChordDetector
import androidx.compose.ui.graphics.drawscope.Stroke
import com.creativeblisslabs.peqlearn.app.ui.components.LiveLooperFloatingControl
import kotlinx.coroutines.IO
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class ExportTimelineNote(
    val midiNote: Int,
    val startSec: Float,
    val endSec: Float,
    val voice: Int,
    val isRightHand: Boolean
)

data class FingeringData(
    val notes: List<Int> = emptyList(),
    val fingers: List<Int> = emptyList()
)

enum class PianoKeyboardMode(val label: String) {
    FULL_88("88 Keys"),
    EXPANDED_61("61 Keys"),
    STUDIO_49("49 Keys"),
    COMPACT_25("25 Keys")
}
class VisualizerStageCaptureController {
    var isExportingVideo by mutableStateOf(false)
    var exportProgress by mutableFloatStateOf(0f)
    var exportTimeSec by mutableStateOf<Float?>(null)
    var graphicsLayer: GraphicsLayer? = null

    suspend fun stepAndCaptureFrame(
        viewModel: AppViewModel,
        timeSec: Float,
        measureStartTimes: FloatArray,
        measureDurations: FloatArray,
        flatNotes: List<ExportTimelineNote>
    ): androidx.compose.ui.graphics.ImageBitmap? {
        // 1. Step playhead to exact measure and beat offset for this video frame
        var mIdx = 0
        for (i in measureStartTimes.indices) {
            if (timeSec >= measureStartTimes[i]) mIdx = i else break
        }
        val mStart = measureStartTimes[mIdx]
        val mDur = measureDurations[mIdx].coerceAtLeast(0.001f)
        val offsetInMeasure = ((timeSec - mStart) / mDur).coerceIn(0f, 1f)

        viewModel.scorePlayheadMeasure.value = mIdx
        viewModel.scorePlayheadOffset.value = offsetInMeasure

        // 2. Highlight active MIDI keys / frets / notation notes for this frame
        val activeNow = HashMap<Int, Boolean>()
        var leadPlayingNote: Int? = null
        var sustainDown = false
        for (fn in flatNotes) {
            if (fn.startSec > timeSec + 0.015f) break
            if (timeSec >= fn.startSec - 0.005f && timeSec <= fn.endSec) {
                activeNow[fn.midiNote] = true
                if (leadPlayingNote == null || fn.midiNote > leadPlayingNote) {
                    leadPlayingNote = fn.midiNote
                }
                sustainDown = true
            }
        }
        viewModel.midiActiveNotes.clear()
        viewModel.midiActiveNotes.putAll(activeNow)
        viewModel.midiPlayingNote.value = leadPlayingNote
        viewModel.sustainPedalActive.value = sustainDown

        // 3. Advance deterministic waterfall particle/flash clock
        exportTimeSec = timeSec

        // 4. Yield execution so Compose recomposes and draws into stageGraphicsLayer
        kotlinx.coroutines.yield()

        val layer = graphicsLayer ?: return null
        return runCatching {
            layer.toImageBitmap()
        }.getOrNull()
    }
}
private class PianoFallingNoteBlock(
    val midiNote: Int,
    var startY: Float = 0f,
    var currentY: Float = 0f,
    val color: Color,
    val isAscending: Boolean = false,
    var alpha: Float = 1.0f
)

data class PianoWaterfallBlock(
    val midiNote: Int,
    var startY: Float = 0f,
    var currentY: Float = 24f,
    val color: Color,
    val isAscending: Boolean = false,
    var alpha: Float = 1.0f
)

data class PianoSpark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    var size: Float,
    val maxLife: Float,
    var currentLife: Float,
    val color: Color
)

data class PianoKeyFlash(
    val midiNote: Int,
    val centerX: Float,
    val centerY: Float,
    val color: Color,
    val velocity: Int = 90,
    var alpha: Float = 1.0f,
    var radius: Float = 36f,
    val isChordStrike: Boolean = false,
    val chordName: String = ""
)

/**
 * Computes the horizontal center X, block width, display label, and string/lane color for a MIDI note
 * so that falling waterfall notes align 1:1 with the active instrument visualizer below it!
 */
data class InstrumentWaterfallLane(
    val centerX: Float,
    val widthPx: Float,
    val label: String,
    val laneColor: Color?,
    val isOpenStringPulse: Boolean = false
)
fun resolveInstrumentWaterfallGeometry(
    midiNote: Int,
    selectedInstrument: String,
    canvasWidth: Float,
    canvasHeight: Float,
    preassignedString: Int? = null,
    preassignedFret: Int? = null
): InstrumentWaterfallLane {
    val inst = selectedInstrument.trim()

    // 1. GUITAR HERO / FRETBOARD MODE (6-String Guitar, 7-String Guitar, Ukulele)
    if (inst.contains("Guitar", ignoreCase = true) || inst.equals("Ukulele", ignoreCase = true)) {
        val isUke = inst.equals("Ukulele", ignoreCase = true)
        val isSeven = inst.contains("7", ignoreCase = true)
        val numFrets = if (isUke) 15 else 24
        val openStringsMidi = when {
            isUke -> intArrayOf(69, 64, 60, 67) // A4, E4, C4, G4
            isSeven -> intArrayOf(64, 59, 55, 50, 45, 40, 35) // e4..B1
            else -> intArrayOf(64, 59, 55, 50, 45, 40) // e4, B3, G3, D3, A2, E2
        }
        val stringNames = when {
            isUke -> arrayOf("A", "E", "C", "G")
            isSeven -> arrayOf("e", "B", "G", "D", "A", "E", "B")
            else -> arrayOf("e", "B", "G", "D", "A", "E")
        }
        val stringColors = arrayOf(
            Color(0xFF00E5FF), // String 1 (High e): Electric Cyan
            Color(0xFFE040FB), // String 2 (B): Neon Magenta
            Color(0xFFFF9100), // String 3 (G): Bright Amber Orange
            Color(0xFF2979FF), // String 4 (D): Cobalt Blue
            Color(0xFFFFEA00), // String 5 (A): Sunburst Yellow
            Color(0xFFFF1744), // String 6 (Low E): Crimson Red
            Color(0xFF00E676)  // String 7 (Low B): Emerald Green
        )

        // Match UnifiedGuitarFretboard's exact horizontal viewport & Rule-of-17.817 logarithmic fret spacing
        val safeW = (canvasWidth * 0.98f).coerceAtLeast(1f)
        val leftOffset = (canvasWidth - safeW) * 0.5f
        val leftPad = leftOffset + 44f.coerceIn(28f, safeW * 0.16f)
        val rightPad = leftOffset + 16f.coerceIn(10f, safeW * 0.06f)
        val playableW = (canvasWidth - leftPad - rightPad).coerceAtLeast(100f)

        val r = 0.056125687f
        var scaleSum = 0f
        for (f in 1..numFrets) {
            scaleSum += (1f - r).toDouble().pow(f.toDouble()).toFloat() * r
        }
        scaleSum = scaleSum.coerceAtLeast(0.001f)

        // Use Viterbi-solved (string, fret) if available so chords NEVER collide on the same string or teleport!
        val bestString: Int
        val bestFret: Int
        if (preassignedString != null && preassignedFret != null &&
            preassignedString in openStringsMidi.indices && preassignedFret in 0..numFrets
        ) {
            bestString = preassignedString
            bestFret = preassignedFret
        } else {
            val result =
                com.creativeblisslabs.peqlearn.app.util.BiomechanicalPerformersEngine.solveFrettedSliceBiomechanical(
                    pitches = listOf(midiNote),
                    openStringsMidiHighToLow = openStringsMidi.toList(),
                    maxFret = numFrets
                )
            val placement = result.first[midiNote]
            bestString = placement?.stringIndex ?: 0
            bestFret = placement?.fret ?: 0
        }

        val strColor = stringColors[bestString % stringColors.size]
        val strName = stringNames[bestString]

        if (bestFret == 0) {
            // Offset open-string pulses slightly by string index so multiple open strings in a chord never overlap 100%!
            val nutX = (leftPad - 12f) + (bestString - 2.5f) * 3.5f
            return InstrumentWaterfallLane(
                centerX = nutX.coerceAtLeast(12f),
                widthPx = 22f,
                label = "0•$strName",
                laneColor = strColor,
                isOpenStringPulse = true
            )
        }

        var accPrev = 0f
        var accCur = 0f
        for (f in 1..bestFret) {
            accPrev = accCur
            val lenFrac = (1f - r).toDouble().pow(f.toDouble()).toFloat() * r
            accCur += (lenFrac / scaleSum) * playableW
        }
        val fretStartX = leftPad + accPrev
        val fretEndX = leftPad + accCur
        val fretCenterX = (fretStartX + fretEndX) * 0.5f
        val fretW = (fretEndX - fretStartX).coerceIn(16f, 54f)

        return InstrumentWaterfallLane(
            centerX = fretCenterX,
            widthPx = fretW * 0.82f,
            label = "$bestFret•$strName",
            laneColor = strColor
        )
    }

    // 2. SITAR / INDIAN CLASSICAL STRING INSTRUMENT
    if (inst.equals("Sitar", ignoreCase = true)) {
        val safeW = (canvasWidth * 0.98f).coerceAtLeast(1f)
        val leftPad = (canvasWidth - safeW) * 0.5f + 36f
        val playableW = (canvasWidth - leftPad - 16f).coerceAtLeast(100f)
        val pc = ((midiNote % 12) + 12) % 12
        val laneX = leftPad + (pc / 11f) * playableW
        return InstrumentWaterfallLane(
            centerX = laneX,
            widthPx = 20f,
            label = getPitchOnlyFromMidi(midiNote),
            laneColor = Color(0xFFFFB300)
        )
    }

    // 3. VIOLIN FINGERBOARD
    if (inst.equals("Violin", ignoreCase = true)) {
        val safeW = (canvasWidth * 0.98f).coerceAtLeast(1f)
        val leftPad = (canvasWidth - safeW) * 0.5f + 30f
        val playableW = (canvasWidth - leftPad - 16f).coerceAtLeast(100f)
        val pc = ((midiNote % 12) + 12) % 12
        val laneX = leftPad + (pc / 11f) * playableW
        return InstrumentWaterfallLane(
            centerX = laneX,
            widthPx = 18f,
            label = getPitchOnlyFromMidi(midiNote),
            laneColor = Color(0xFFE040FB)
        )
    }

    // 4. FLUTE TONE HOLES
    if (inst.equals("Flute", ignoreCase = true)) {
        val safeW = (canvasWidth * 0.98f).coerceAtLeast(1f)
        val leftPad = (canvasWidth - safeW) * 0.5f + 24f
        val playableW = (canvasWidth - leftPad - 16f).coerceAtLeast(100f)
        val pc = ((midiNote % 12) + 12) % 12
        val laneX = leftPad + (pc / 11f) * playableW
        return InstrumentWaterfallLane(
            centerX = laneX,
            widthPx = 16f,
            label = getPitchOnlyFromMidi(midiNote),
            laneColor = Color(0xFF00E5FF)
        )
    }

    // 5. DEFAULT PIANO / HARMONIUM (88-key or 37-key 1:1 geometry)
    val isHarmonium = inst.equals("Harmonium", ignoreCase = true)
    val startMidi = if (isHarmonium) 48 else 21
    val totalKeys = if (isHarmonium) 37 else 88
    val whiteNotes = (startMidi..(startMidi + totalKeys - 1)).filter { !isMidiNoteBlack(it) }
    val whiteKeyW = canvasWidth / whiteNotes.size.coerceAtLeast(1)
    
    val isBlack = isMidiNoteBlack(midiNote)
    val wIdx = getWhiteKeyIndexForMidi(midiNote)
    val centerX = (wIdx + 0.5f) * whiteKeyW
    val laneW = if (isBlack) whiteKeyW * 0.58f else whiteKeyW * 0.82f

    return InstrumentWaterfallLane(
        centerX = centerX,
        widthPx = laneW,
        label = getNoteNameFromMidi(midiNote),
        laneColor = null
    )
}
    @Suppress("SuspiciousIndentation")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PianoKeyboard88Keys(
        viewModel: AppViewModel,
        primaryColor: Color = MaterialTheme.colorScheme.primary,
    ) {
        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()

        val whiteKeyCount = 52
        val whiteKeyWidth = 28.dp
        val blackKeyWidth = 18.dp
        val whiteKeyHeight = 150.dp
        val blackKeyHeight = 90.dp
        val keyboardWidth = whiteKeyWidth * whiteKeyCount

        var keyboardMode by rememberSaveable { mutableStateOf(PianoKeyboardMode.FULL_88) }
        var notationFormat by viewModel.notationFormat
        val showNoteLabels = viewModel.showPianoKeyLabels.value
        val showWaterfall = viewModel.showWaterfall.value
        var currentOctaveOffset by remember { mutableStateOf(0) }
        val pressedKeys = remember { mutableStateListOf<Int>() }
        var showExportDialog by rememberSaveable { mutableStateOf(false) }
        var showFullscreenNotation by rememberSaveable { mutableStateOf(false) }

        val (startMidi, totalKeys) = when (keyboardMode) {
            PianoKeyboardMode.FULL_88 -> Pair(21, 88)
            PianoKeyboardMode.EXPANDED_61 -> Pair(36, 61)
            PianoKeyboardMode.STUDIO_49 -> Pair(36, 49)
            PianoKeyboardMode.COMPACT_25 -> Pair(48 + currentOctaveOffset * 12, 25)
        }

        val allNotes = remember(startMidi, totalKeys) {
            (startMidi until (startMidi + totalKeys)).toList()
        }
        val whiteNotes = remember(allNotes) {
            allNotes.filter { !isMidiNoteBlack(it) }
        }

        val activeNotesSnapshot = remember {
            derivedStateOf { viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key } }
        }

        val liveDetectedChord by remember {
            derivedStateOf {
                val combined = (activeNotesSnapshot.value + pressedKeys).distinct()
                LiveChordDetector.detect(combined)
            }
        }

        val combinedActiveNotes by remember {
            derivedStateOf {
                (activeNotesSnapshot.value + pressedKeys).distinct()
            }
        }

        var internalKeyboardModeName by rememberSaveable { mutableStateOf("PIANO") }
        LaunchedEffect(internalKeyboardModeName) {
            if (internalKeyboardModeName == "PIANO") {
                viewModel.audioEngine.setInstrument("PIANO")
            }
        }


        val isFullscreen = viewModel.isFullscreenKeyboard.value
        // Automatically sync enabledTrackIndices whenever a new Score is loaded (works in both Inline & Fullscreen modes!)
        val currentLoadedScoreForTracks = viewModel.currentScore.value as? Score
        LaunchedEffect(currentLoadedScoreForTracks) {
            if (currentLoadedScoreForTracks != null) {
                viewModel.enabledTrackIndices.clear()
                if (currentLoadedScoreForTracks.tracks.isNotEmpty()) {
                    currentLoadedScoreForTracks.tracks.forEach { tr ->
                        viewModel.enabledTrackIndices[tr.trackIndex] = tr.isEnabled
                    }
                } else {
                    viewModel.enabledTrackIndices[0] = true
                    viewModel.enabledTrackIndices[1] = true
                }
            }
        }

        // Snapshot copy of enabledTrackIndices so any Switch toggle triggers immediate recomposition in Staff, Tab & Waterfall
        val enabledTracksSnapshot = viewModel.enabledTrackIndices.toMap()

        val activeMidiNoteSet by remember {
            derivedStateOf {
                viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key }.toSet()
            }
        }

        var showDumpDialog by rememberSaveable { mutableStateOf(false) }
        var showHandsDumpDialog by rememberSaveable { mutableStateOf(false) }
        var showPlayAlongStatsDialog by rememberSaveable { mutableStateOf(false) }
        var showLearnModeDialog by rememberSaveable { mutableStateOf(false) }
        var showAudioToMidiScreen by rememberSaveable { mutableStateOf(false) }
        var showMusicianshipAcademyScreen by rememberSaveable { mutableStateOf(false) }
        var showImportMenu by remember { mutableStateOf(false) }

        var allTutorials by remember {
            mutableStateOf<List<PracticeLibrary.BuiltinTutorialMidi>>(emptyList())
        }

        var isLoadingMidiLibrary by remember { mutableStateOf(false) }
        var midiSearchQuery by remember { mutableStateOf("") }
        var selectedMidiFilter by remember { mutableStateOf("All") }

        val totalTranspositionSemis =
            viewModel.transposition.intValue + (viewModel.octaveShift.intValue * 12)

        
        val transposedActiveMidiNotesMap by remember(totalTranspositionSemis) {
            derivedStateOf {
                if (totalTranspositionSemis == 0) {
                    viewModel.midiActiveNotes.toMap()
                } else {
                    viewModel.midiActiveNotes.entries
                        .filter { it.value }
                        .associate { (midi, isOn) ->
                            (midi + totalTranspositionSemis).coerceIn(
                                0,
                                127
                            ) to isOn
                        }
                }
            }
        }

        val transposedActiveMidiNoteSet by remember(totalTranspositionSemis) {
            derivedStateOf {
                transposedActiveMidiNotesMap.entries.filter { it.value }.map { it.key }.toSet()
            }
        }

        val transposedActiveList by remember(totalTranspositionSemis) {
            derivedStateOf {
                transposedActiveMidiNoteSet.toList().sorted()
            }
        }

        val transposedPlayingNote by remember(totalTranspositionSemis) {
            derivedStateOf {
                viewModel.midiPlayingNote.value?.let {
                    (it + totalTranspositionSemis).coerceIn(
                        0,
                        127
                    )
                }
            }
        }

        val transposedActiveNoteColors by remember(totalTranspositionSemis) {
            derivedStateOf {
                val rawMap =
                    viewModel.activeNoteTrackColors + viewModel.playAlongEngine.liveKeyColors
                if (totalTranspositionSemis == 0) rawMap
                else rawMap.entries.associate { (m, c) ->
                    (m + totalTranspositionSemis).coerceIn(
                        0,
                        127
                    ) to c
                }
            }
        }
        val playAlongAndActiveNotesMap by remember(totalTranspositionSemis) {
    derivedStateOf {
        val base = transposedActiveMidiNotesMap.toMutableMap()
        if (viewModel.isGuidedPlayAlongActive.value) {
            for (targetMidi in viewModel.guidedTargetNotes) {
                val shifted = (targetMidi + totalTranspositionSemis).coerceIn(0, 127)
                base[shifted] = true
            }
        }
        base
    }
}
        LaunchedEffect(showImportMenu, showLearnModeDialog) {
            if ((showImportMenu || showLearnModeDialog) && allTutorials.isEmpty()) {
                isLoadingMidiLibrary = true
                try {
                    val tutorials: List<PracticeLibrary.BuiltinTutorialMidi> =
                        withContext(Dispatchers.IO) {
                            val files: List<String> =
                                viewModel.onListMidiAssetsRequested?.invoke()
                                    ?: viewModel.onListMidiAssets?.invoke()
                                    ?: emptyList()

                            val catalog = PracticeLibrary.buildTutorialCatalog(
                                assetFileNames = files,
                                readBytes = { name: String ->
                                    viewModel.onReadAssetBytes?.invoke(name)
                                }
                            )
                            if (catalog.isNotEmpty()) catalog else PracticeLibrary.curatedTutorials
                        }
                    allTutorials = tutorials
                } catch (e: Exception) {
                    allTutorials = PracticeLibrary.curatedTutorials
                } finally {
                    isLoadingMidiLibrary = false
                }
            }
        }
        // CRITICAL FIX: Do NOT render or tick the inline Notation + Waterfall + Keyboard when Fullscreen mode is open!
        if (!isFullscreen) {
            LearnSectionCard(title = "Notation & Piano Viewer", icon = Icons.Filled.List) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        // 1. Changes alignment to align elements to the end (right side)
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { viewModel.onPickScoreFileRequested?.invoke() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(
                                Icons.Default.FileOpen,
                                contentDescription = "Open Score",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Import",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { viewModel.isFullscreenKeyboard.value = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    // Only mount StaffAndTabNotationViewer if layout mode includes Notation
                    val layoutMode = viewModel.visualizerLayoutMode.value
                    if (layoutMode == "Split" || layoutMode == "Notation") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.15f))
                        ) {
                            StaffAndTabNotationViewer(
                                score = viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score,
                                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                playheadOffset = viewModel.scorePlayheadOffset.value,
                                activeMidiNotes = viewModel.midiActiveNotes,
                                playingNote = viewModel.midiPlayingNote.value,
                                primaryColor = primaryColor,
                                showScoreHeader = false,
                                currentFormat = notationFormat,
                                onFormatChange = { notationFormat = it },
                                showLetters = viewModel.showNotationNoteNames.value,
                                showFingerings = viewModel.showSuggestedFingerings.value,
                                isSplitStaff = viewModel.isSplitStaff.value,
                                isVerticalScroll = viewModel.isVerticalScroll.value,
                                leftHandColor = viewModel.leftHandColor.value,
                                rightHandColor = viewModel.rightHandColor.value,
                                isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                isRightHandHidden = viewModel.isRightHandHidden.value,
                                transposition = totalTranspositionSemis,
                                modifier = Modifier.fillMaxSize(),
                                enabledTrackIndices = enabledTracksSnapshot
                            )
                        }
                    }

                    // PIANO KEYBOARD & WATERFALL ON BOTTOM
                    if (layoutMode != "Notation") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(0.3f))
                            ) {
                                if (showWaterfall && layoutMode != "Keyboard") {
                                    SynthesiaWaterfallOverlay(
                                        activeMidiNotes = activeMidiNoteSet,
                                        manualPressedNotes = pressedKeys.toSet(),
                                        score = viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score,
                                        playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                        playheadOffset = viewModel.scorePlayheadOffset.value,
                                        isPlaying = viewModel.isScorePlaying.value,
                                        fallingSpeedSetting = viewModel.fallingNotesSpeed.value,
                                        modifier = Modifier
                                            .width(keyboardWidth)
                                            .height(140.dp)
                                            .background(Color.Black.copy(0.25f)),
                                        primaryColor = primaryColor,
                                        glowEffect = true,
                                        particlesEnabled = viewModel.showWaterfallParticles.value,
                                        particleCount = viewModel.particleCount.intValue,
                                        particleSpeed = viewModel.particleSpeed.floatValue,
                                        particleScale = viewModel.particleScale.floatValue,
                                        particleExpansion = viewModel.particleExpansion.floatValue,
                                        lockParticleColor = viewModel.lockParticleColor.value,
                                        particleCustomColor = viewModel.particleCustomColor.value,
                                        flashSize = viewModel.flashSize.floatValue,
                                        flashHaloIntensity = viewModel.flashHaloIntensity.floatValue,
                                        noteCornerRadius = viewModel.noteCornerRadius.floatValue,
                                        noteEdgeThickness = viewModel.noteEdgeThickness.floatValue,
                                        noteStyleOval = viewModel.noteStyleOval.value,
                                        leftHandColor = viewModel.leftHandColor.value,
                                        rightHandColor = viewModel.rightHandColor.value,
                                        isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                        isRightHandHidden = viewModel.isRightHandHidden.value,
                                        enabledTrackIndices = viewModel.enabledTrackIndices,
//                                    exportTimeSec = stageCaptureController.exportTimeSec,
                                        playbackSpeed = viewModel.playbackSpeed.value,
                                        selectedInstrument = viewModel.selectedInstrument.value,
                                        activeNoteColors = viewModel.activeNoteTrackColors,
                                        transposition = totalTranspositionSemis
                                    )
                                }

                                if (layoutMode != "Waterfall") {
                                    Box(
                                        modifier = Modifier
                                            .width(keyboardWidth)
                                            .height(whiteKeyHeight)
                                            .padding(vertical = 4.dp)
                                    ) {
                                        UnifiedPianoKeyboard(
                                            activeMidiNotes = playAlongAndActiveNotesMap,
                                            customKeyColors = transposedActiveNoteColors,
                                            accentColor = primaryColor,
                                            activeCoordinate = viewModel.midiPlayingNote.value,
                                            isPlaying = viewModel.isScorePlaying.value,
                                            startNote = startMidi,
                                            endNote = startMidi + totalKeys - 1,
                                            leftHandColor = viewModel.leftHandColor.value,
                                            rightHandColor = viewModel.rightHandColor.value,
                                            onKeyTapped = null,
                                            onKeyReleased = null,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        PianoHumanHandsOverlay(
                                            activeMidiNotes = playAlongAndActiveNotesMap,
                                            score = viewModel.currentScore.value as? Score,
                                            playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                            playheadOffset = viewModel.scorePlayheadOffset.value,
                                            activeNoteColors = viewModel.activeNoteTrackColors +
                                                    viewModel.playAlongEngine.liveKeyColors,
                                            startNote = startMidi,
                                            endNote = startMidi + totalKeys - 1,
                                            showLeftHand = viewModel.showLeftHandGuide.value && !viewModel.isLeftHandHidden.value,
                                            showRightHand = viewModel.showRightHandGuide.value && !viewModel.isRightHandHidden.value,
                                            leftHandColor = viewModel.leftHandColor.value,
                                            rightHandColor = viewModel.rightHandColor.value,
                                            transposition = totalTranspositionSemis,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            PianoThreePedalUnit(
                                sustainPressed = viewModel.sustainPedalActive.value,
                                sostenutoPressed = viewModel.sostenutoPedalActive.value,
                                softPressed = viewModel.softPedalActive.value,
                                onPedalClick = { type, pressed ->
                                    viewModel.onMidiPedalEventReceived(type, pressed)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 8.dp, top = 6.dp)
                            )

                            // Real-Time Floating Chord Readout Overlay (Ported from midee's ChordOverlay)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 2.dp, top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LiveChordHud(
                                    detectedChord = liveDetectedChord,
                                    primaryColor = primaryColor
                                )
                                LiveLooperFloatingControl(
                                    looper = viewModel.liveLooper,
                                    primaryColor = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        }


        // Real-Time MIDI Connection & Disconnection Prompt Dialog
        viewModel.midiConnectionPrompt.value?.let { prompt ->
            MidiConnectionPromptDialog(
                prompt = prompt,
                onDismiss = { viewModel.dismissMidiPrompt() }
            )
        }

        // Score / MIDI / PDF Import Computation Loading Dialog
        if (viewModel.isImportingPDF.value) {
            Dialog(
                onDismissRequest = { /* Modal processing */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF140F24),
                    border = BorderStroke(1.dp, Color(0xFF3D2D5E)),
                    shadowElevation = 16.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = primaryColor,
                                strokeWidth = 3.dp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Importing & Parsing Score",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Analyzing MIDI measures, voices, and notation layouts...",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }

                        val progress = viewModel.pdfImportProgress.value
                        if (progress > 0f) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = primaryColor,
                                    trackColor = Color(0xFF281E38)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
        // Centered High-Resolution Sheet Music PDF Export Progress Dialog
        if (viewModel.isExportingPDF.value) {
            Dialog(
                onDismissRequest = { /* Modal during vector PDF compilation */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF12131A),
                    border = BorderStroke(1.5.dp, Color(0xFF00E676).copy(alpha = 0.7f)),
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Compiling Vector Sheet Music PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        val opts = viewModel.pdfExportOptions.value
                        Text(
                            text = "${opts.pageSize} ${opts.orientation} • ${opts.staffFormat} • ${opts.colorTheme}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(104.dp).padding(6.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { viewModel.pdfExportProgress.value },
                                modifier = Modifier.fillMaxSize(),
                                color = Color(0xFF00E676),
                                trackColor = Color(0xFF282A36),
                                strokeWidth = 8.dp
                            )
                            Text(
                                text = "${(viewModel.pdfExportProgress.value * 100f).toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        LinearProgressIndicator(
                            progress = { viewModel.pdfExportProgress.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color(0xFF00E676),
                            trackColor = Color(0xFF282A36)
                        )

                        Text(
                            text = "Engraving staves, beams, fingerings, and lossless score metadata to /Documents/pEQ_Scores...",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        if (showDumpDialog) {
            val clipboardManager = LocalClipboardManager.current
            var copiedMessage by remember { mutableStateOf(false) }
            val currentScoreObj = viewModel.currentScore.value as? Score
            val dumpText = remember(currentScoreObj) {
                currentScoreObj?.dumpAsText() ?: "No active score loaded to inspect."
            }

            ModalBottomSheet(
                onDismissRequest = { showDumpDialog = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF0F1117)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Explicitly uses the baseline core Menu icon as a safe layout anchor
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6)
                            )
                            Text(
                                "Score Notation Parser Dump",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val titleSlug =
                                        currentScoreObj?.title?.replace(
                                            Regex("[^a-zA-Z0-9_-]"),
                                            "_"
                                        )
                                            ?: "score"
                                    com.creativeblisslabs.peqlearn.app.shareTextFile(
                                        "${titleSlug}_parser_dump.txt",
                                        dumpText
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6A1B9A
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Share,
                                    contentDescription = "Share File",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                                Text(
                                    "Share File",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            IconButton(onClick = { showDumpDialog = false }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Text(
                        "This parser dump displays all measures, note timings, voices, beam groups, rests, and note intervals. You can copy it or save it as a .txt file to compare with your MIDI file.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF06080C),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        val verticalScroll = rememberScrollState()
                        val horizontalScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = dumpText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF81C784),
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        // ── Hands Overlay Diagnostic Dump Dialog ───────────────────────────────────
        if (showHandsDumpDialog) {
            val clipboardManager = LocalClipboardManager.current
            var handsCopied by remember { mutableStateOf(false) }
            val scoreObj = viewModel.currentScore.value as? Score
            val handsDumpText = remember(scoreObj) {
                scoreObj?.dumpHandsOverlayAsText()
                    ?: "No active score loaded.\n\nLoad a MIDI file first, then tap 'Hands TXT' to audit the hand/finger overlay."
            }
            val titleSlugH = scoreObj?.title?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "score"

            ModalBottomSheet(
                onDismissRequest = { showHandsDumpDialog = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF0A0D14)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Piano,
                                contentDescription = null,
                                tint = Color(0xFFCE93D8)
                            )
                            Text(
                                "Piano Hands Overlay — Diagnostic Dump",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share file button
                            Button(
                                onClick = {
                                    com.creativeblisslabs.peqlearn.app.shareTextFile(
                                        "${titleSlugH}_hands_overlay_dump.txt",
                                        handsDumpText
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6A1B9A
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share, contentDescription = null,
                                    modifier = Modifier.size(16.dp), tint = Color.White
                                )
                                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                                Text(
                                    "Share File",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { showHandsDumpDialog = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Text(
                        "Per-beat audit: which notes go LH vs RH, finger number assigned (F1=thumb … F5=pinky), " +
                                "skipped/clamped notes, hand-crossing events, and span violations. " +
                                "Share the .txt file so it can be reviewed for placement correctness.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    // Legend row
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf(
                            "F1=Thumb" to Color(0xFFFFB74D),
                            "F2=Index" to Color(0xFF81C784),
                            "F3=Middle" to Color(0xFF64B5F6),
                            "F4=Ring" to Color(0xFFBA68C8),
                            "F5=Pinky" to Color(0xFFFF8A65),
                            "⚠=Issue" to Color(0xFFEF5350)
                        ).forEach { (label, color) ->
                            Text(
                                label,
                                fontSize = 10.sp,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Monospace dump text area
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF06080C),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        val vertScroll = rememberScrollState()
                        val horizScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(vertScroll)
                                .horizontalScroll(horizScroll)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = handsDumpText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = Color(0xFFCE93D8),
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

// Play-Along Evaluation & Live Accuracy Scorecard Studio Dialog
        if (showPlayAlongStatsDialog) {
            val scorecard = viewModel.playAlongEngine.scorecard.value
            val isEvaluating = viewModel.playAlongEngine.isEvaluating.value
            val currentLoadedScore = viewModel.currentScore.value as? Score
            var selectedSkillTier by rememberSaveable { mutableStateOf("Beginner") } // Beginner, Intermediate, Advanced
            var waitForNoteEnabled by rememberSaveable { mutableStateOf(true) }
            var autoSpeedUpLoop by rememberSaveable { mutableStateOf(false) }

            FullscreenDialog(
                onDismissRequest = { showPlayAlongStatsDialog = false }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0B1E)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Live Play-Along Performance Studio",
                                    color = Color(0xFFFFD700),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isEvaluating) "Listening to USB MIDI / Touch Keyboard Input..." else "Select your pianist tier & start evaluation",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = { showPlayAlongStatsDialog = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Pianist Skill Level Preset Bar (Adapts tempo, timing tolerance & practice aids)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF161226),
                            border = BorderStroke(1.dp, Color(0xFF2D234E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "SELECT PIANIST PRACTICE TIER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple(
                                            "Beginner",
                                            "0.65x Speed • Note Labels • Step Wait",
                                            0.65f
                                        ),
                                        Triple(
                                            "Intermediate",
                                            "0.85x Speed • Hand Split • Tempo Ramp",
                                            0.85f
                                        ),
                                        Triple(
                                            "Advanced",
                                            "1.0x Concert Speed • Strict Timing",
                                            1.0f
                                        )
                                    ).forEach { (tierName, desc, defaultSpeed) ->

                                        val isSelected = selectedSkillTier == tierName
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0xFF5C6BC0) else Color(
                                                0xFF211A38
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFFFFD700) else Color.Transparent
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedSkillTier = tierName
                                                    viewModel.playbackSpeed.value = defaultSpeed

                                                    when (tierName) {
                                                        "Beginner" -> {
                                                            viewModel.showNotationNoteNames.value =
                                                                true
                                                            viewModel.showPianoKeyLabels.value =
                                                                true
                                                            waitForNoteEnabled = true
                                                            autoSpeedUpLoop = false
                                                        }

                                                        "Intermediate" -> {
                                                            viewModel.showNotationNoteNames.value =
                                                                true
                                                            viewModel.showPianoKeyLabels.value =
                                                                false
                                                            waitForNoteEnabled = false
                                                            autoSpeedUpLoop = true
                                                        }

                                                        "Advanced" -> {
                                                            viewModel.showNotationNoteNames.value =
                                                                false
                                                            viewModel.showPianoKeyLabels.value =
                                                                false
                                                            waitForNoteEnabled = false
                                                            autoSpeedUpLoop = false
                                                        }
                                                    }
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    tierName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    desc,
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.75f),
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Grade & Accuracy Banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1638),
                            border = BorderStroke(1.dp, Color(0xFF382966)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "OVERALL ACCURACY",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${scorecard.accuracyPercentage}%",
                                        color = Color(0xFF00E676),
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Avg timing offset: ${scorecard.averageTimingErrorMs} ms",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                    val feedbackTip = when {
                                        scorecard.accuracyPercentage >= 95 -> "Concert Mastery! Ready to increase tempo by +10%."
                                        scorecard.accuracyPercentage >= 80 -> "Solid run! Practice missed transitions with A–B Bar Loop."
                                        scorecard.averageTimingErrorMs > 65 -> "Tip: Enable Metronome Click & practice at 0.75x tempo."
                                        else -> "Tip: Isolate Right Hand (RH) first, then Left Hand (LH)."
                                    }
                                    Text(
                                        feedbackTip,
                                        color = Color(0xFF81D4FA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(38.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFFFD700),
                                                    Color(0xFFFF9100)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        scorecard.grade,
                                        color = Color.Black,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Detailed Statistics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161226),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "HIT NOTES",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${scorecard.hitNotesCount}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161226),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "MISSED",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${scorecard.missedNotesCount}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5252)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161226),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "STREAK",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${scorecard.currentStreak}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD700)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161226),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "BEST",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${scorecard.bestStreak}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF80D8FF)
                                    )
                                }
                            }
                        }

                        // Smart Practice Aids (Wait-for-Note, Progressive Tempo Ramp, Metronome)
                        // Interactive Guided Play-Along Mode Selector
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF161226),
                            border = BorderStroke(1.dp, Color(0xFF2D234E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "GUIDED KEY-FLASHING PLAY-ALONG MODE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD700)
                                    )
                                    Switch(
                                        checked = viewModel.isGuidedPlayAlongActive.value,
                                        onCheckedChange = {
                                            viewModel.isGuidedPlayAlongActive.value = it
                                        }
                                    )
                                }

                                val modes = listOf(
                                    Triple(
                                        "WAIT_FOR_NOTE",
                                        "1. Step-by-Step Wait (Flashing Target Keys)",
                                        "Flashes the next chord/note on the visualizer and pauses until you press the matching key(s) on your USB MIDI keyboard."
                                    ),
                                    Triple(
                                        "MEASURE_FOLLOWUP",
                                        "2. Measure Call & Response (Bar-by-Bar Follow-Up)",
                                        "Plays & flashes 1 full measure for you to watch, then rewinds that measure and flashes each note as you play it back before advancing to the next measure."
                                    ),
                                    Triple(
                                        "TEMPO_RAMP",
                                        "3. Progressive Tempo Speed Ramp (+5% per Bar)",
                                        "Loops the active section and automatically increases tempo by +5% every time you achieve >= 85% accuracy."
                                    ),
                                    Triple(
                                        "CONTINUOUS",
                                        "4. Live Continuous Concert Evaluation",
                                        "Plays continuously at your chosen tempo while highlighting score keys and grading your timing offset in milliseconds."
                                    )
                                )

                                modes.forEach { (modeId, title, desc) ->
                                    val selected = viewModel.playAlongPracticeMode.value == modeId
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) Color(0xFF2E2252) else Color(
                                            0xFF1E1833
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) Color(0xFF00E5FF) else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.playAlongPracticeMode.value = modeId
                                                viewModel.isGuidedPlayAlongActive.value = true
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) Color(0xFF00E5FF) else Color.White
                                            )
                                            Text(
                                                desc,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.72f),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Control Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isRunning = isEvaluating || viewModel.isGuidedPlayAlongActive.value
                            Button(
                                onClick = {
                                    if (isRunning && viewModel.isScorePlaying.value) {
                                        viewModel.isGuidedPlayAlongActive.value = false
                                        viewModel.guidedTargetNotes.clear()
                                        viewModel.playAlongEngine.liveKeyColors.clear()
                                        viewModel.playAlongEngine.stopEvaluation()
                                        viewModel.stopScorePlayback()
                                    } else {
                                        if (currentLoadedScore != null) {
                                            viewModel.isUsbMidiInputEnabled.value = true
                                            viewModel.isGuidedPlayAlongActive.value = true
                                            viewModel.playAlongEngine.startEvaluation(
                                                currentLoadedScore,
                                                currentLoadedScore.tempo * viewModel.playbackSpeed.value
                                            )
                                            if (!viewModel.isScorePlaying.value) {
                                                viewModel.startScorePlayback()
                                            }
                                            showPlayAlongStatsDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRunning && viewModel.isScorePlaying.value) Color(
                                        0xFFD32F2F
                                    ) else Color(0xFF00E676),
                                    contentColor = if (isRunning && viewModel.isScorePlaying.value) Color.White else Color.Black
                                )
                            ) {
                                Text(
                                    if (isRunning && viewModel.isScorePlaying.value) "Stop Guided Play-Along" else "Start Guided Play-Along",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { showPlayAlongStatsDialog = false },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Learn Mode (Masterworks, A-B Measure Range Drill, Smart Difficulty Analyzer & Scales/Chords)
        if (showLearnModeDialog) {
            FullscreenDialog(
                onDismissRequest = { showLearnModeDialog = false }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0E0B1A)
                ) {
                    var practiceStudioTab by remember { mutableIntStateOf(0) } // 0 = Masterworks, 1 = Score Bar-by-Bar & A-B Loop, 2 = Scales & Chords
                    var selectedTrainingMode by remember { mutableStateOf("FULL") }
                    var selectedDifficultyFilter by remember { mutableStateOf("ALL") } // "ALL", "Easy", "Medium", "Hard"
                    val currentLoadedScore = viewModel.currentScore.value as? Score
                    val totalMeasures = currentLoadedScore?.measures?.size ?: 0
                    var loopStartBar by remember(totalMeasures) { mutableIntStateOf(1) }
                    var loopEndBar by remember(totalMeasures) {
                        mutableIntStateOf(
                            minOf(
                                4,
                                totalMeasures.coerceAtLeast(1)
                            )
                        )
                    }
                    val emptyTutorials = PracticeLibrary.curatedTutorials.take(0)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Library & Practice Studio",
                                    color = Color(0xFFFFD700),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Curriculum for Beginners, Intermediate & Advanced Pianists",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = { showLearnModeDialog = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Tab Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF130E24))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(
                                    0,
                                    "Song Library (${PracticeLibrary.curatedTutorials.size})",
                                    Icons.Default.LibraryMusic
                                ),
                                Triple(
                                    1,
                                    "Score A–B Drill ($totalMeasures Bars)",
                                    Icons.Default.Tune
                                )
                            ).forEach { (tabIdx, tabTitle, tabIcon) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (practiceStudioTab == tabIdx) Color(
                                                0xFF5C6BC0
                                            ) else Color.Transparent
                                        )
                                        .clickable { practiceStudioTab = tabIdx }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            tabIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            tabTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (practiceStudioTab == 0) {
                            var selectedBookCategory by rememberSaveable { mutableStateOf("All Library") }
                            val bookCategories = remember {
                                listOf(
                                    "All Library",
                                    "Masterworks",
                                    "MusicXML Scores",
                                    "Beyer Op. 101",
                                    "Burgmüller Op. 100",
                                    "Czerny Op. 599",
                                    "Czerny Op. 777",
                                    "Czerny Op. 299",
                                    "Hanon Virtuoso"
                                )
                            }

                            // 1. Classical Method Book & Score Collection Selector Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bookCategories.forEach { cat ->
                                    val active = selectedBookCategory == cat
                                    val countInCat =
                                        if (cat == "All Library") allTutorials.size else allTutorials.count { it.bookCategory == cat }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (active) Color(0xFF5C6BC0) else Color(0xFF19132E),
                                        border = BorderStroke(
                                            1.dp,
                                            if (active) Color(0xFFFFD700) else Color(0xFF2C2250)
                                        ),
                                        modifier = Modifier.clickable { selectedBookCategory = cat }
                                    ) {
                                        Text(
                                            text = if (countInCat > 0) "$cat ($countInCat)" else cat,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }
                                }
                            }

                            // 2. Search Input + Difficulty Tier Filter + Hand Mode Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = midiSearchQuery,
                                    onValueChange = { midiSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search piece, opus, or exercise #...",
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                                    modifier = Modifier.weight(1f).height(62.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        "ALL",
                                        "Beginner",
                                        "Intermediate",
                                        "Advanced"
                                    ).forEach { diff ->
                                        val active = selectedDifficultyFilter == diff
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (active) Color(0xFFFFD700) else Color(
                                                        0xFF19132E
                                                    )
                                                )
                                                .clickable { selectedDifficultyFilter = diff }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                diff,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) Color.Black else Color.White.copy(
                                                    alpha = 0.75f
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF19132E))
                                        .padding(2.dp)
                                ) {
                                    listOf(
                                        "FULL" to "Both",
                                        "RH_ONLY" to "RH",
                                        "LH_ONLY" to "LH",
                                        "SLOW_TEMPO" to "0.7x"
                                    ).forEach { (modeKey, label) ->
                                        val isSelected = selectedTrainingMode == modeKey
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) Color(0xFF6C63FF) else Color.Transparent)
                                                .clickable { selectedTrainingMode = modeKey }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color.White.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Trainer Practice Mode Selector (Realtime, Wait-For-Me, Follow-Me) + WLED Hardware Lighting
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "REALTIME" to "⚡ Realtime",
                                        "WAIT" to "🛑 Wait-For-Me",
                                        "FOLLOW" to "🤝 Follow-Me"
                                    ).forEach { (mKey, mLabel) ->
                                        val isSelected = (mKey == "WAIT" && viewModel.playAlongEngine.waitForUserMode.value) ||
                                                (mKey == "FOLLOW" && viewModel.playAlongEngine.isPracticeMode.value) ||
                                                (mKey == "REALTIME" && !viewModel.playAlongEngine.waitForUserMode.value && !viewModel.playAlongEngine.isPracticeMode.value)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                when (mKey) {
                                                    "WAIT" -> {
                                                        viewModel.playAlongEngine.waitForUserMode.value = true
                                                        viewModel.playAlongEngine.isPracticeMode.value = false
                                                    }
                                                    "FOLLOW" -> {
                                                        viewModel.playAlongEngine.waitForUserMode.value = false
                                                        viewModel.playAlongEngine.isPracticeMode.value = true
                                                    }
                                                    else -> {
                                                        viewModel.playAlongEngine.waitForUserMode.value = false
                                                        viewModel.playAlongEngine.isPracticeMode.value = false
                                                    }
                                                }
                                            },
                                            label = { Text(mLabel, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                                        )
                                    }
                                }

                                FilterChip(
                                    selected = com.creativeblisslabs.peqlearn.app.audio.WledKeyLightingEngine.isWledEnabled,
                                    onClick = {
                                        com.creativeblisslabs.peqlearn.app.audio.WledKeyLightingEngine.isWledEnabled =
                                            !com.creativeblisslabs.peqlearn.app.audio.WledKeyLightingEngine.isWledEnabled
                                    },
                                    label = { Text(if (com.creativeblisslabs.peqlearn.app.audio.WledKeyLightingEngine.isWledEnabled) "💡 WLED Hardware: ON" else "💡 WLED Hardware: OFF", fontSize = 10.sp) }
                                )
                            }

                            val filteredTutorials = remember(
                                allTutorials,
                                selectedBookCategory,
                                midiSearchQuery,
                                selectedDifficultyFilter
                            ) {
                                val sourceList =
                                    if (allTutorials.isNotEmpty()) allTutorials else PracticeLibrary.curatedTutorials
                                sourceList.filter { tutorial ->
                                    val matchesBook =
                                        selectedBookCategory == "All Library" || tutorial.bookCategory == selectedBookCategory
                                    val matchesSearch = midiSearchQuery.isBlank() ||
                                            tutorial.title.contains(
                                                midiSearchQuery,
                                                ignoreCase = true
                                            ) ||
                                            tutorial.composer.contains(
                                                midiSearchQuery,
                                                ignoreCase = true
                                            ) ||
                                            tutorial.bookCategory.contains(
                                                midiSearchQuery,
                                                ignoreCase = true
                                            )
                                    val matchesDifficulty = selectedDifficultyFilter == "ALL" ||
                                            tutorial.difficulty.contains(
                                                selectedDifficultyFilter,
                                                ignoreCase = true
                                            )
                                    matchesBook && matchesSearch && matchesDifficulty
                                }
                            }

                            // Curriculum Progress Summary Bar when viewing a specific Classical Method Book
                            if (selectedBookCategory != "All Library" && filteredTutorials.isNotEmpty()) {
                                val completedCount = filteredTutorials.count {
                                    (PracticeLibrary.exerciseMasteryMap[it.id] ?: 0) >= 80
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF151C33),
                                    border = BorderStroke(1.dp, Color(0xFF2E3D66)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 8.dp
                                        ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$selectedBookCategory Curriculum • Exercises in Sequential Order (1 → ${filteredTutorials.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF81D4FA)
                                        )
                                        Text(
                                            text = "Mastered: $completedCount / ${filteredTutorials.size} ★",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(filteredTutorials, key = { idx, tutorial -> "${tutorial.id}_$idx" }) { _, tutorial ->
                                    val masteryPct =
                                        PracticeLibrary.exerciseMasteryMap[tutorial.id] ?: 0
                                    val diffBadgeColor = when {
                                        tutorial.difficulty.contains("Beginner", true) -> Color(
                                            0xFF69F0AE
                                        )

                                        tutorial.difficulty.contains("Advanced", true) -> Color(
                                            0xFFFF5252
                                        )

                                        else -> Color(0xFFFFD700)
                                    }

                                    fun launchExercise(enablePlayAlong: Boolean) {
                                        scope.launch {
                                            val loadedScore = withContext(Dispatchers.IO) {
                                                PracticeLibrary.loadTutorialScore(
                                                    tutorial = tutorial,
                                                    readBytes = { name ->
                                                        viewModel.onReadAssetBytes?.invoke(
                                                            name
                                                        )
                                                    },
                                                    trainingMode = selectedTrainingMode
                                                )
                                            }
                                            if (loadedScore != null) {
                                                viewModel.currentScore.value = loadedScore
                                                viewModel.syncScoreTracks(loadedScore)
                                                viewModel.resetScorePlayhead()
                                                when (selectedTrainingMode) {
                                                    "RH_ONLY" -> viewModel.setHandPracticeMode("RIGHT")
                                                    "LH_ONLY" -> viewModel.setHandPracticeMode("LEFT")
                                                    else -> viewModel.setHandPracticeMode("BOTH")
                                                }
                                                if (enablePlayAlong) {
                                                    viewModel.isUsbMidiInputEnabled.value = true
                                                    viewModel.isGuidedPlayAlongActive.value = true
                                                    viewModel.playAlongPracticeMode.value =
                                                        "WAIT_FOR_NOTE"
                                                    viewModel.playAlongEngine.startEvaluation(
                                                        loadedScore,
                                                        loadedScore.tempo
                                                    )
                                                    viewModel.startScorePlayback()
                                                }
                                                showLearnModeDialog = false
                                            }
                                        }
                                    }

                                    val estDurSec = ((tutorial.totalMeasures * 4f * (60f / tutorial.tempo.coerceAtLeast(30))).toInt()).coerceIn(10, 300)
                                    val rangeLabel = if (tutorial.difficulty.contains("Beginner", true)) "C3–C5" else if (tutorial.difficulty.contains("Advanced", true)) "E2–E5" else "G2–G5"

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF161226),
                                        border = BorderStroke(
                                            1.dp,
                                            if (masteryPct >= 80) Color(0xFF00E676).copy(alpha = 0.5f) else Color(0xFF2E244A)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Column 1 (Left): Title, Composer · Collection, Progress Bar
                                                Column(modifier = Modifier.weight(1.3f)) {
                                                    Text(
                                                        text = tutorial.title,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = "${tutorial.composer} · ${tutorial.bookCategory}",
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.65f)
                                                    )
                                                    Spacer(Modifier.height(6.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        LinearProgressIndicator(
                                                            progress = { (masteryPct.toFloat() / 100f).coerceIn(0f, 1f) },
                                                            modifier = Modifier.width(110.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                            color = if (masteryPct >= 80) Color(0xFF00E676) else Color(0xFF7986CB),
                                                            trackColor = Color(0xFF2E244A)
                                                        )
                                                        Text(
                                                            text = "$masteryPct%",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }

                                                // Column 2 (Middle): Pill Badges & Practice Status
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.weight(1.1f)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = diffBadgeColor.copy(alpha = 0.18f),
                                                            border = BorderStroke(1.dp, diffBadgeColor.copy(alpha = 0.4f))
                                                        ) {
                                                            Text(
                                                                text = tutorial.difficulty,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = diffBadgeColor,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color.White.copy(alpha = 0.08f)
                                                        ) {
                                                            Text(
                                                                text = "$estDurSec sec",
                                                                fontSize = 10.sp,
                                                                color = Color.White.copy(alpha = 0.8f),
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color.White.copy(alpha = 0.08f)
                                                        ) {
                                                            Text(
                                                                text = "${tutorial.tempo} BPM",
                                                                fontSize = 10.sp,
                                                                color = Color.White.copy(alpha = 0.8f),
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color.White.copy(alpha = 0.08f)
                                                        ) {
                                                            Text(
                                                                text = rangeLabel,
                                                                fontSize = 10.sp,
                                                                color = Color.White.copy(alpha = 0.8f),
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = if (masteryPct > 0) "Mastery: $masteryPct% · Completed" else "Not practiced yet",
                                                        fontSize = 10.sp,
                                                        color = Color.White.copy(alpha = 0.5f)
                                                    )
                                                }

                                                // Column 3 (Right): Preview & Practice Action Buttons
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { launchExercise(enablePlayAlong = false) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Preview", fontSize = 11.sp, color = Color.White)
                                                    }

                                                    Button(
                                                        onClick = { launchExercise(enablePlayAlong = true) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Practice", tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Practice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }

                                                    IconButton(
                                                        onClick = { /* Bookmark toggle */ },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = "Bookmark",
                                                            tint = Color.White.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (practiceStudioTab == 1) {
                            // Score A–B Multi-Bar Loop Drill & Automatic Hardest-Measures Finder
                            if (currentLoadedScore == null || currentLoadedScore.measures.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Import or select a score first to unlock Measure-by-Measure A–B Drills.",
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                // Identify the 3 most complex bars in the score (highest note density + polyphony)
                                val hardestBars = remember(currentLoadedScore) {
                                    currentLoadedScore.measures.mapIndexed { idx, m ->
                                        idx + 1 to m.notes.count { !it.isRest }
                                    }.sortedByDescending { it.second }.take(4)
                                }

                                Column(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // A-B Range Loop & Hand Separation Card
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF19132E),
                                        border = BorderStroke(1.dp, Color(0xFF2C2250)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        "HANDS:",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                    listOf(
                                                        "BOTH" to "Both",
                                                        "RIGHT" to "RH Only",
                                                        "LEFT" to "LH Only"
                                                    ).forEach { (hKey, hLabel) ->
                                                        val isSelected =
                                                            viewModel.activeHandMode.value == hKey
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(
                                                                    if (isSelected) Color(
                                                                        0xFF6C63FF
                                                                    ) else Color(0xFF251A42)
                                                                )
                                                                .clickable {
                                                                    viewModel.setHandPracticeMode(
                                                                        hKey
                                                                    )
                                                                }
                                                                .padding(
                                                                    horizontal = 8.dp,
                                                                    vertical = 4.dp
                                                                )
                                                        ) {
                                                            Text(
                                                                hLabel,
                                                                fontSize = 10.sp,
                                                                color = Color.White,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        "SPEED:",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                    listOf(0.5f, 0.75f, 1.0f).forEach { spd ->
                                                        val active =
                                                            viewModel.playbackSpeed.value == spd
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(
                                                                    if (active) Color(0xFF00E676) else Color(
                                                                        0xFF251A42
                                                                    )
                                                                )
                                                                .clickable {
                                                                    viewModel.playbackSpeed.value =
                                                                        spd
                                                                }
                                                                .padding(
                                                                    horizontal = 7.dp,
                                                                    vertical = 4.dp
                                                                )
                                                        ) {
                                                            Text(
                                                                "${spd}x",
                                                                fontSize = 10.sp,
                                                                color = if (active) Color.Black else Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // A-B Loop Range Selector
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        "A–B BAR LOOP:",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                    OutlinedButton(
                                                        onClick = {
                                                            loopStartBar =
                                                                (loopStartBar - 1).coerceAtLeast(1)
                                                        },
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.size(24.dp)
                                                    ) { Text("-", color = Color.White) }
                                                    Text(
                                                        "Bar $loopStartBar",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    OutlinedButton(
                                                        onClick = {
                                                            loopStartBar =
                                                                (loopStartBar + 1).coerceAtMost(
                                                                    totalMeasures
                                                                )
                                                            if (loopEndBar < loopStartBar) loopEndBar =
                                                                loopStartBar
                                                        },
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.size(24.dp)
                                                    ) { Text("+", color = Color.White) }

                                                    Text(
                                                        "to",
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.6f)
                                                    )

                                                    OutlinedButton(
                                                        onClick = {
                                                            loopEndBar =
                                                                (loopEndBar - 1).coerceAtLeast(
                                                                    loopStartBar
                                                                )
                                                        },
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.size(24.dp)
                                                    ) { Text("-", color = Color.White) }
                                                    Text(
                                                        "Bar $loopEndBar",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    OutlinedButton(
                                                        onClick = {
                                                            loopEndBar =
                                                                (loopEndBar + 1).coerceAtMost(
                                                                    totalMeasures
                                                                )
                                                        },
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.size(24.dp)
                                                    ) { Text("+", color = Color.White) }
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.isPracticeLoopEnabled.value = true
                                                        val sIdx = (loopStartBar - 1).coerceIn(
                                                            0,
                                                            totalMeasures - 1
                                                        )
                                                        val eIdx = (loopEndBar - 1).coerceIn(
                                                            sIdx,
                                                            totalMeasures - 1
                                                        )
                                                        viewModel.practiceMeasureRange.value =
                                                            sIdx to eIdx
                                                        viewModel.seekToPosition(sIdx, 0f)
                                                        viewModel.startScorePlayback()
                                                        showLearnModeDialog = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(
                                                            0xFFFFD700
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(
                                                        horizontal = 12.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text(
                                                        "Loop Bars $loopStartBar–$loopEndBar",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.Black
                                                    )
                                                }
                                            }

                                            // Smart Challenging Bars Quick-Jump
                                            if (hardestBars.isNotEmpty()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        "CHALLENGING BARS:",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFF8A80)
                                                    )
                                                    hardestBars.forEach { (barNum, count) ->
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF3B1D2E),
                                                            border = BorderStroke(
                                                                1.dp,
                                                                Color(0xFFFF5252).copy(alpha = 0.5f)
                                                            ),
                                                            modifier = Modifier.clickable {
                                                                loopStartBar = barNum
                                                                loopEndBar =
                                                                    minOf(totalMeasures, barNum + 1)
                                                            }
                                                        ) {
                                                            Text(
                                                                "Bar $barNum ($count notes)",
                                                                fontSize = 9.5.sp,
                                                                color = Color(0xFFFFCDD2),
                                                                modifier = Modifier.padding(
                                                                    horizontal = 7.dp,
                                                                    vertical = 3.dp
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Measure Grid Picker
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val measureRows = (1..totalMeasures).chunked(4)
                                        items(measureRows) { rowIndices ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowIndices.forEach { barNum ->
                                                    val isInLoopRange =
                                                        barNum in loopStartBar..loopEndBar
                                                    val measureObj =
                                                        currentLoadedScore.measures.getOrNull(barNum - 1)
                                                    val noteCount =
                                                        measureObj?.notes?.count { !it.isRest } ?: 0
                                                    val rhCount =
                                                        measureObj?.notes?.count { !it.isRest && it.midiNote >= 60 }
                                                            ?: 0
                                                    val lhCount =
                                                        measureObj?.notes?.count { !it.isRest && it.midiNote < 60 }
                                                            ?: 0
                                                    val difficultyColor = when {
                                                        noteCount >= 16 -> Color(0xFFFF5252) // Hard bar
                                                        noteCount >= 8 -> Color(0xFFFFD700)  // Medium bar
                                                        else -> Color(0xFF69F0AE)            // Easy bar
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isInLoopRange) Color(0xFF392B68) else Color(
                                                            0xFF19132E
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isInLoopRange) Color(0xFFFFD700) else Color(
                                                                0xFF2C2250
                                                            )
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                loopStartBar = barNum
                                                                loopEndBar = barNum
                                                                viewModel.seekToPosition(
                                                                    barNum - 1,
                                                                    0f
                                                                )
                                                                if (viewModel.isPracticeLoopEnabled.value) {
                                                                    viewModel.practiceMeasureRange.value =
                                                                        (barNum - 1) to (barNum - 1)
                                                                }
                                                                viewModel.startScorePlayback()
                                                                showLearnModeDialog = false
                                                            }
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                4.dp
                                                            )
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    "Bar $barNum",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    fontSize = 13.sp
                                                                )
                                                                Box(
                                                                    modifier = Modifier.size(7.dp)
                                                                        .clip(CircleShape)
                                                                        .background(difficultyColor)
                                                                )
                                                            }
                                                            Text(
                                                                "$noteCount notes",
                                                                color = Color.White.copy(alpha = 0.75f),
                                                                fontSize = 10.sp
                                                            )
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(
                                                                    6.dp
                                                                )
                                                            ) {
                                                                Text(
                                                                    "RH:$rhCount",
                                                                    fontSize = 9.sp,
                                                                    color = Color(0xFF81D4FA)
                                                                )
                                                                Text(
                                                                    "LH:$lhCount",
                                                                    fontSize = 9.sp,
                                                                    color = Color(0xFFCE93D8)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (rowIndices.size < 4) {
                                                    repeat(4 - rowIndices.size) {
                                                        Spacer(Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Scales, Chords & Technical Exercises
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(PracticeLibrary.items) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF19132E),
                                        border = BorderStroke(1.dp, Color(0xFF2C2250)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val practiceScore =
                                                    PracticeLibrary.createScoreForPracticeItem(item)
                                                viewModel.currentScore.value = practiceScore
                                                viewModel.resetScorePlayhead()
                                                showLearnModeDialog = false
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    item.name,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                AssistChip(
                                                    onClick = {},
                                                    label = {
                                                        Text(
                                                            item.category,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                )
                                            }
                                            Text(
                                                item.description,
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "RH Fingering: ${
                                                        item.fingeringRightHand.joinToString(
                                                            "-"
                                                        )
                                                    }", fontSize = 11.sp, color = Color(0xFF81D4FA)
                                                )
                                                Text(
                                                    "LH Fingering: ${
                                                        item.fingeringLeftHand.joinToString(
                                                            "-"
                                                        )
                                                    }", fontSize = 11.sp, color = Color(0xFFCE93D8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        // AI Audio-to-Score Screen
        if (showAudioToMidiScreen) {
            FullscreenDialog(
                onDismissRequest = { showAudioToMidiScreen = false }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0B1E)
                ) {
                    com.creativeblisslabs.peqlearn.app.ai.AudioToMidiScreen(
                        viewModel = viewModel,
                        onScoreGenerated = { score ->
                            viewModel.currentScore.value = score
                            viewModel.scorePlayheadMeasure.value = 0
                            viewModel.scorePlayheadOffset.value = 0f
                            showAudioToMidiScreen = false
                            viewModel.toastMessage.value =
                                "Loaded ${score.title} with ${score.measures.size} measures!"
                        },
                        onBack = { showAudioToMidiScreen = false }
                    )
                }
            }
        }

        // Ear Training Academy Screen
        if (showMusicianshipAcademyScreen) {
            FullscreenDialog(
                onDismissRequest = { showMusicianshipAcademyScreen = false }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0B1E)
                ) {
                    MusicianshipAcademyScreen(
                        viewModel = viewModel,
                        onBack = { showMusicianshipAcademyScreen = false }
                    )
                }
            }
        }
        // Open Fullscreen Stage when either user opens Fullscreen OR Video Export is running!
        val shouldMountFullscreenStage =
            viewModel.isFullscreenKeyboard.value || viewModel.isVideoExporting.value

        if (shouldMountFullscreenStage) {
            val stageCaptureController = remember { VisualizerStageCaptureController() }
            val stageGraphicsLayer = rememberGraphicsLayer()

            SideEffect {
                stageCaptureController.graphicsLayer = stageGraphicsLayer
                viewModel.visualizerStageCaptureController = stageCaptureController
            }

            DisposableEffect(stageCaptureController) {
                viewModel.visualizerStageCaptureController = stageCaptureController
                onDispose {
                    if (viewModel.visualizerStageCaptureController === stageCaptureController) {
                        viewModel.visualizerStageCaptureController = null
                    }
                }
            }

            FullscreenDialog(
                onDismissRequest = {
                    if (!viewModel.isVideoExporting.value) {
                        viewModel.isFullscreenKeyboard.value = false
                    }
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                DisposableEffect(Unit) {
                    setScreenOrientation(pEQOrientation.LANDSCAPE)
                    setSystemBarsVisibility(false)
                    onDispose {
                        setScreenOrientation(pEQOrientation.USER)
                        setSystemBarsVisibility(true)
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
                    var fsZoomFactor by rememberSaveable { mutableStateOf(1.0f) }
                    var panOffset by remember { mutableStateOf(Offset.Zero) }
                    var areControlsVisible by remember { mutableStateOf(true) }
                    var fsShowLetters by rememberSaveable { mutableStateOf(showNoteLabels) }
                    var fsShowLyrics by rememberSaveable { mutableStateOf(true) }
                    var fsShowWaterfall by rememberSaveable { mutableStateOf(viewModel.showWaterfall.value) }
                    // Layout Mode: "Split" (Notation on Top + Waterfall), "KeyboardWaterfall", "Waterfall", "Notation", or "Keyboard"
                    var fsLayoutMode by viewModel.visualizerLayoutMode
                    var fsLayoutMenuExpanded by remember { mutableStateOf(false) }
                    var fsNotationMenuExpanded by remember { mutableStateOf(false) }
                    var fsInstrumentMenuExpanded by remember { mutableStateOf(false) }
                    val fsManualPressedKeys = remember { mutableStateListOf<Int>() }
                    var showSpeedOptimizationDialog by remember { mutableStateOf(false) }

                    val isPlaying = viewModel.isScorePlaying.value

                    val currentScore =
                        viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score


                    val effectiveTempo = (currentScore?.tempo
                        ?: 120f).coerceAtLeast(20f) * viewModel.playbackSpeed.value.coerceIn(
                        0.25f,
                        2.5f
                    )

                    val totalSeconds = remember(currentScore, effectiveTempo) {
                        if (currentScore == null) 0.0
                        else {
                            currentScore.measures.sumOf { m ->
                                val quarterBeats =
                                    m.timeSignatureNumerator.toDouble() * (4.0 / m.timeSignatureDenominator.coerceAtLeast(
                                        1
                                    ).toDouble())
                                (60.0 / effectiveTempo.toDouble()) * quarterBeats
                            }
                        }
                    }

                    val currentMeasure = viewModel.scorePlayheadMeasure.value
                    val currentOffset = viewModel.scorePlayheadOffset.value

                    val currentSeconds =
                        remember(currentScore, currentMeasure, currentOffset, effectiveTempo) {
                            if (currentScore == null) 0.0
                            else {
                                var elapsed = 0.0
                                for (i in 0 until currentMeasure) {
                                    val m = currentScore.measures.getOrNull(i) ?: break
                                    val quarterBeats =
                                        m.timeSignatureNumerator.toDouble() * (4.0 / m.timeSignatureDenominator.coerceAtLeast(
                                            1
                                        ).toDouble())
                                    elapsed += (60.0 / effectiveTempo.toDouble()) * quarterBeats
                                }
                                val m = currentScore.measures.getOrNull(currentMeasure)
                                if (m != null) {
                                    val quarterBeats =
                                        m.timeSignatureNumerator.toDouble() * (4.0 / m.timeSignatureDenominator.coerceAtLeast(
                                            1
                                        ).toDouble())
                                    elapsed += (currentOffset.toDouble() * (60.0 / effectiveTempo.toDouble()) * quarterBeats)
                                }
                                elapsed
                            }
                        }

                    val timelineProgress =
                        if (totalSeconds > 0) (currentSeconds / totalSeconds).toFloat() else 0f
                    val currentMin = currentSeconds.toInt() / 60
                    val currentSec = currentSeconds.toInt() % 60
                    val currentTimeStr =
                        "$currentMin:${if (currentSec < 10) "0$currentSec" else currentSec}"

                    val totalMin = totalSeconds.toInt() / 60
                    val totalSec = totalSeconds.toInt() % 60
                    val totalTimeStr = "$totalMin:${if (totalSec < 10) "0$totalSec" else totalSec}"

                    val fsScrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    areControlsVisible = !areControlsVisible
                                })
                            }
                            .pointerInput(fsZoomFactor) {
                                detectTransformGestures { _, pan, _, _ ->
                                    if (fsZoomFactor > 1.0f) {
                                        val maxPanX = (fsZoomFactor - 1f) * size.width / 2f
                                        val maxPanY = (fsZoomFactor - 1f) * size.height / 2f
                                        panOffset = Offset(
                                            x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                            y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                        )
                                    } else {
                                        panOffset = Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {


                    }
                    val isExportingNow =
                        stageCaptureController.isExportingVideo || viewModel.isVideoExporting.value
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (isExportingNow) 1f else fsZoomFactor,
                                scaleY = if (isExportingNow) 1f else fsZoomFactor,
                                translationX = if (isExportingNow) 0f else panOffset.x,
                                translationY = if (isExportingNow) 0f else panOffset.y
                            )
                            .then(
                                if (isExportingNow) {
                                    Modifier.drawWithContent {
                                        stageGraphicsLayer.record {
                                            this@drawWithContent.drawContent()
                                        }
                                        drawLayer(stageGraphicsLayer)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .background(Color.Black)
                    ) {
                        val availableW = maxWidth
                        val isNotationOnly = fsLayoutMode == "Notation"
                        val isLandscapeWide = availableW >= 600.dp
                        // Piano & waterfall width: in landscape wide or notation mode it fits availableW; in portrait split/waterfall/keyboard it expands for 88 playable keys
                        val keyboardWidth = if (isLandscapeWide) availableW else maxOf(
                            availableW,
                            30.dp * whiteKeyCount
                        )

                        val activeMidiNoteSet by remember {
                            derivedStateOf {
                                viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key }
                                    .toSet()
                            }
                        }

                        // If in "Split" mode, structure layout so top Notation fits screen width (availableW),
                        // while the lower Waterfall + Keyboard scroll horizontally to accommodate the 88 keys in portrait.
                        if (fsLayoutMode == "Split") {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // 1. Notation Rendering on Top - Pinned 100% to screen width (availableW)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.52f)
                                        .background(Color(0xFF0D0B14))
                                        .padding(bottom = 8.dp)
                                ) {
                                    StaffAndTabNotationViewer(
                                        score = currentScore,
                                        playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                        playheadOffset = viewModel.scorePlayheadOffset.value,
                                        activeMidiNotes = viewModel.midiActiveNotes,
                                        playingNote = viewModel.midiPlayingNote.value,
                                        primaryColor = primaryColor,
                                        currentFormat = notationFormat,
                                        onFormatChange = { newFormat ->
                                            notationFormat = newFormat
                                        },
                                        showLetters = viewModel.showNotationNoteNames.value,
                                        showLyrics = fsShowLyrics,
                                        showFingerings = viewModel.showSuggestedFingerings.value,
                                        isSplitStaff = viewModel.isSplitStaff.value,
                                        isVerticalScroll = viewModel.isVerticalScroll.value,
                                        leftHandColor = viewModel.leftHandColor.value,
                                        rightHandColor = viewModel.rightHandColor.value,
                                        isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                        isRightHandHidden = viewModel.isRightHandHidden.value,
                                        enabledTrackIndices = viewModel.enabledTrackIndices,
                                        showScoreHeader = false,
                                        compactStaffMode = true,
                                        transposition = totalTranspositionSemis,
                                        fallbackTitle = viewModel.currentMediaItem.value?.title
                                            ?: viewModel.selectedMixerItem.value?.displayName
                                            ?: "", // <-- PASSED HERE
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Clean separator divider between notation and waterfall
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(Color(0x33FFFFFF))
                                    )
                                }

                                // 2. Waterfall + Keyboard on Bottom - Horizontally scrollable in portrait for 88 keys
                                val bottomScrollModifier = if (isLandscapeWide) {
                                    Modifier.fillMaxWidth().weight(0.48f)
                                } else {
                                    Modifier.fillMaxWidth().weight(0.48f)
                                        .horizontalScroll(fsScrollState)
                                }

                                Box(modifier = bottomScrollModifier) {
                                    Column(
                                        modifier = Modifier
                                            .width(keyboardWidth)
                                            .fillMaxHeight()
                                    ) {
                                        // Waterfall Falling Notes (fills upper portion of bottom section)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .background(Color.Black)
                                        ) {
                                            SynthesiaWaterfallOverlay(
                                                activeMidiNotes = activeMidiNoteSet,
                                                manualPressedNotes = fsManualPressedKeys.toSet(),
                                                score = currentScore,
                                                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                                playheadOffset = viewModel.scorePlayheadOffset.value,
                                                isPlaying = isPlaying,
                                                fallingSpeedSetting = viewModel.fallingNotesSpeed.value,
                                                modifier = Modifier.fillMaxSize(),
                                                primaryColor = primaryColor,
                                                glowEffect = true,
                                                particlesEnabled = viewModel.showWaterfallParticles.value,
                                                particleCount = viewModel.particleCount.intValue,
                                                particleSpeed = viewModel.particleSpeed.floatValue,
                                                particleScale = viewModel.particleScale.floatValue,
                                                particleExpansion = viewModel.particleExpansion.floatValue,
                                                lockParticleColor = viewModel.lockParticleColor.value,
                                                particleCustomColor = viewModel.particleCustomColor.value,
                                                flashSize = viewModel.flashSize.floatValue,
                                                flashHaloIntensity = viewModel.flashHaloIntensity.floatValue,
                                                noteCornerRadius = viewModel.noteCornerRadius.floatValue,
                                                noteEdgeThickness = viewModel.noteEdgeThickness.floatValue,
                                                noteStyleOval = viewModel.noteStyleOval.value,
                                                leftHandColor = viewModel.leftHandColor.value,
                                                rightHandColor = viewModel.rightHandColor.value,
                                                isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                isRightHandHidden = viewModel.isRightHandHidden.value,
                                                playbackSpeed = viewModel.playbackSpeed.value,
                                                onFpsDropDetected = {
                                                    if (viewModel.fallingNotesSpeed.value != "Slow" || viewModel.playbackSpeed.value > 0.75f) {
                                                        showSpeedOptimizationDialog = true
                                                    }
                                                },
                                                enabledTrackIndices = viewModel.enabledTrackIndices.toMap(),
                                                exportTimeSec = stageCaptureController.exportTimeSec,
                                                selectedInstrument = viewModel.selectedInstrument.value,
                                                activeNoteColors = viewModel.activeNoteTrackColors,
                                                transposition = totalTranspositionSemis
                                            )
                                        }

                                        // Bottom Instrument Visualizer: UnifiedPianoKeyboard, UnifiedGuitarFretboard, Harmonium, Violin, Flute
                                        val currentInst = viewModel.selectedInstrument.value
                                        val isGuitarFamily =
                                            currentInst.contains("Guitar") || currentInst == "Ukulele"
                                        val keyboardModifier = Modifier
                                            .fillMaxWidth()
                                            .height(viewModel.keyboardHeightFactor.floatValue.dp)
                                            .background(Color(0xFF161616))

                                        Box(modifier = keyboardModifier) {
                                            if (isGuitarFamily) {
                                                val isUkulele = currentInst == "Ukulele"
                                                val isSevenString = currentInst == "7-String Guitar"
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                UnifiedGuitarFretboard(
                                                    numFrets = if (isUkulele) 15 else 24,
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    accentColor = primaryColor,
                                                    showNoteNames = fsShowLetters,
                                                    stringLabels = when {
                                                        isUkulele -> listOf("A", "E", "C", "G")
                                                        isSevenString -> listOf(
                                                            "e",
                                                            "B",
                                                            "G",
                                                            "D",
                                                            "A",
                                                            "E",
                                                            "B"
                                                        )

                                                        else -> listOf("e", "B", "G", "D", "A", "E")
                                                    },
                                                    openStringsSemis = when {
                                                        isUkulele -> listOf(9, 4, 0, 7)
                                                        isSevenString -> listOf(
                                                            4,
                                                            11,
                                                            7,
                                                            2,
                                                            9,
                                                            4,
                                                            11
                                                        )

                                                        else -> listOf(4, 11, 7, 2, 9, 4)
                                                    },
                                                    onFretTapped = { _, _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    onFretReleased = { _, _, midi ->
                                                        fsManualPressedKeys.remove(midi)
                                                        viewModel.onMidiNoteOff(midi)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Realistic Anatomical Human Hand & Celluloid Pick Overlay for Guitar & Ukulele
                                                GuitarHumanHandsOverlay(
                                                    activeMidiNotes = playAlongAndActiveNotesMap,
                                                    score = currentScore,
                                                    playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                                    playheadOffset = viewModel.scorePlayheadOffset.value,
                                                    numStrings = if (isUkulele) 4 else if (isSevenString) 7 else 6,
                                                    numFrets = if (isUkulele) 15 else 24,
                                                    baseFret = 1,
                                                    openStringsMidi = when {
                                                        isUkulele -> listOf(69, 64, 60, 67)
                                                        isSevenString -> listOf(64, 59, 55, 50, 45, 40, 35)
                                                        else -> listOf(64, 59, 55, 50, 45, 40)
                                                    },
                                                    showFrettingHand = viewModel.showLeftHandGuide.value && !viewModel.isLeftHandHidden.value,
                                                    showPickingHand = viewModel.showRightHandGuide.value && !viewModel.isRightHandHidden.value,
                                                    handAccentColor = primaryColor,
                                                    pickColor = Color(0xFFFFB347),
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Sitar") {
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                com.creativeblisslabs.peqlearn.app.ui.components.UnifiedSitarVisualizer(
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    accentColor = Color(0xFFFFB300),
                                                    showNoteNames = fsShowLetters,
                                                    onFretTapped = { _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Tabla") {
                                                val activeTablaBol = remember(
                                                    viewModel.midiActiveNotes.keys,
                                                    viewModel.midiPlayingNote.value
                                                ) {
                                                    val activeKey = viewModel.midiPlayingNote.value
                                                        ?: viewModel.midiActiveNotes.keys.firstOrNull()
                                                    activeKey?.let { midi ->
                                                        when (midi % 12) {
                                                            0, 1 -> "GHE"
                                                            2, 3 -> "DHA"
                                                            4, 5 -> "TIN"
                                                            6, 7 -> "NA"
                                                            8, 9 -> "TA"
                                                            else -> "KAT"
                                                        }
                                                    }
                                                }
                                                UnifiedTablaVisualizer(
                                                    activeBol = activeTablaBol,
                                                    accentColor = Color(0xFFFFB300),
                                                    onBolTapped = { bol ->
                                                        val midi = when (bol) {
                                                            "GHE" -> 36
                                                            "DHA" -> 38
                                                            "TIN" -> 42
                                                            "NA" -> 46
                                                            "TA" -> 48
                                                            else -> 40
                                                        }
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    onBolReleased = { bol ->
                                                        val midi = when (bol) {
                                                            "GHE" -> 36
                                                            "DHA" -> 38
                                                            "TIN" -> 42
                                                            "NA" -> 46
                                                            "TA" -> 48
                                                            else -> 40
                                                        }
                                                        fsManualPressedKeys.remove(midi)
                                                        viewModel.onMidiNoteOff(midi)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Harmonium") {
                                                UnifiedHarmoniumVisualizer(
                                                    activeMidiNotes = transposedActiveMidiNotesMap,
                                                    accentColor = Color(0xFFFFB300),
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    onKeyTapped = { midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    onKeyReleased = { midi ->
                                                        fsManualPressedKeys.remove(midi)
                                                        viewModel.onMidiNoteOff(midi)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Violin") {
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                UnifiedViolinFingerboard(
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    accentColor = Color(0xFFE040FB),
                                                    showNoteNames = fsShowLetters,
                                                    onPositionTapped = { _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Flute") {
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                UnifiedFluteVisualizer(
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    accentColor = Color(0xFF00E5FF),
                                                    onToneHoleTapped = { _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst.contains(
                                                    "Drum",
                                                    ignoreCase = true
                                                )
                                            ) {
                                                com.creativeblisslabs.peqlearn.app.ui.components.ThreeDDrumVisualizer(
                                                    viewModel = viewModel,
                                                    primaryColor = primaryColor,
                                                    currentKitLabels = viewModel.drumPadLabels,
                                                    showOnlyPartLabel = true, // Set to true for raw labels ("SNARE", "KICK"), or false for full names ("MAIN SNARE", "BASS DRUM 1")
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                UnifiedPianoKeyboard(
                                                    activeMidiNotes = playAlongAndActiveNotesMap,
                                                    customKeyColors = transposedActiveNoteColors,
                                                    accentColor = primaryColor,
                                                    activeCoordinate = viewModel.midiPlayingNote.value,
                                                    isPlaying = isPlaying,
                                                    startNote = 21,
                                                    endNote = 108,
                                                    fillWidth = true,
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    onKeyTapped = null,
                                                    onKeyReleased = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Articulated Moving Left & Right Human Hands with Finger Numbers (1..5)
                                                PianoHumanHandsOverlay(
                                                    activeMidiNotes = playAlongAndActiveNotesMap,
                                                    score = currentScore,
                                                    playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                                    playheadOffset = viewModel.scorePlayheadOffset.value,
                                                    activeNoteColors = viewModel.activeNoteTrackColors +
                                                            viewModel.playAlongEngine.liveKeyColors,
                                                    startNote = 21,
                                                    endNote = 108,
                                                    showLeftHand = viewModel.showLeftHandGuide.value && !viewModel.isLeftHandHidden.value,
                                                    showRightHand = viewModel.showRightHandGuide.value && !viewModel.isRightHandHidden.value,
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    transposition = totalTranspositionSemis,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Studio Red Velvet Cloth Strip
                                                if (viewModel.keyboardShowVelvetCloth.value) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(4.dp)
                                                            .align(Alignment.TopCenter)
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    listOf(
                                                                        viewModel.keyboardClothColor.value,
                                                                        viewModel.keyboardClothColor.value.copy(
                                                                            alpha = 0.85f
                                                                        ),
                                                                        Color(0xFF330000)
                                                                    )
                                                                )
                                                            )
                                                    )

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // For Non-Split modes ("Notation", "Waterfall", "Keyboard")
                            val scrollModifier = if (isLandscapeWide || isNotationOnly) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(fsScrollState)
                            }
                            val finalContentWidth =
                                if (isNotationOnly || isLandscapeWide) availableW else keyboardWidth

                            Box(modifier = scrollModifier) {
                                Column(
                                    modifier = Modifier
                                        .width(finalContentWidth)
                                        .fillMaxHeight()
                                ) {
                                    when (fsLayoutMode) {
                                        "Notation" -> {
                                            // Full height notation viewer fitting screen width
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .background(Color(0xFF0D0B14))
                                            ) {
                                                StaffAndTabNotationViewer(
                                                    score = currentScore,
                                                    playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                                    playheadOffset = viewModel.scorePlayheadOffset.value,
                                                    activeMidiNotes = viewModel.midiActiveNotes,
                                                    playingNote = viewModel.midiPlayingNote.value,
                                                    primaryColor = primaryColor,
                                                    currentFormat = notationFormat,
                                                    onFormatChange = { newFormat ->
                                                        notationFormat = newFormat
                                                    },
                                                    showLetters = viewModel.showNotationNoteNames.value,
                                                    showLyrics = fsShowLyrics,
                                                    showFingerings = viewModel.showSuggestedFingerings.value,
                                                    isSplitStaff = viewModel.isSplitStaff.value,
                                                    isVerticalScroll = viewModel.isVerticalScroll.value,
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    showScoreHeader = false,
                                                    compactStaffMode = true,
                                                    enabledTrackIndices = enabledTracksSnapshot,
                                                    transposition = totalTranspositionSemis,
                                                    fallbackTitle = viewModel.currentMediaItem.value?.title
                                                        ?: viewModel.selectedMixerItem.value?.displayName
                                                        ?: "", // <-- PASSED HERE
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        "Keyboard" -> {
                                            // Keyboard Only: Spacer for upper visual area so instrument dominates
                                            Spacer(modifier = Modifier.weight(1f))
                                        }

                                        else -> {
                                            // Waterfall Only full height
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .background(Color.Black)
                                            ) {
                                                SynthesiaWaterfallOverlay(
                                                    activeMidiNotes = activeMidiNoteSet,
                                                    manualPressedNotes = fsManualPressedKeys.toSet(),
                                                    score = currentScore,
                                                    playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                                    playheadOffset = viewModel.scorePlayheadOffset.value,
                                                    isPlaying = isPlaying,
                                                    fallingSpeedSetting = viewModel.fallingNotesSpeed.value,
                                                    modifier = Modifier.fillMaxSize(),
                                                    primaryColor = primaryColor,
                                                    glowEffect = true,
                                                    particlesEnabled = viewModel.showWaterfallParticles.value,
                                                    particleCount = viewModel.particleCount.intValue,
                                                    particleSpeed = viewModel.particleSpeed.floatValue,
                                                    particleScale = viewModel.particleScale.floatValue,
                                                    particleExpansion = viewModel.particleExpansion.floatValue,
                                                    lockParticleColor = viewModel.lockParticleColor.value,
                                                    particleCustomColor = viewModel.particleCustomColor.value,
                                                    flashSize = viewModel.flashSize.floatValue,
                                                    flashHaloIntensity = viewModel.flashHaloIntensity.floatValue,
                                                    noteCornerRadius = viewModel.noteCornerRadius.floatValue,
                                                    noteEdgeThickness = viewModel.noteEdgeThickness.floatValue,
                                                    noteStyleOval = viewModel.noteStyleOval.value,
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    playbackSpeed = viewModel.playbackSpeed.value,
                                                    onFpsDropDetected = {
                                                        if (viewModel.fallingNotesSpeed.value != "Slow" || viewModel.playbackSpeed.value > 0.75f) {
                                                            showSpeedOptimizationDialog = true
                                                        }
                                                    },
                                                    enabledTrackIndices = viewModel.enabledTrackIndices.toMap(),
                                                    exportTimeSec = stageCaptureController.exportTimeSec,
                                                    selectedInstrument = viewModel.selectedInstrument.value,
                                                    activeNoteColors = viewModel.activeNoteTrackColors,
                                                    transposition = totalTranspositionSemis
                                                )
                                            }
                                        }
                                    }

                                    // Bottom Instrument Visualizer for Keyboard / Waterfall+Keyboard modes
                                    if (fsLayoutMode != "Notation" && fsLayoutMode != "Waterfall") {
                                        val currentInst = viewModel.selectedInstrument.value
                                        val isGuitarFamily =
                                            currentInst.contains("Guitar") || currentInst == "Ukulele"
                                        val keyboardModifier = if (fsLayoutMode == "Keyboard") {
                                            Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 190.dp, max = 225.dp)
                                                .background(Color(0xFF161616))
                                        } else {
                                            Modifier
                                                .fillMaxWidth()
                                                .height(viewModel.keyboardHeightFactor.floatValue.dp)
                                                .background(Color(0xFF161616))
                                        }

                                        Box(modifier = keyboardModifier) {
                                            if (isGuitarFamily) {
                                                val isUkulele = currentInst == "Ukulele"
                                                val isSevenString = currentInst == "7-String Guitar"
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                UnifiedGuitarFretboard(
                                                    numFrets = if (isUkulele) 15 else 24,
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    accentColor = primaryColor,
                                                    showNoteNames = fsShowLetters,
                                                    stringLabels = when {
                                                        isUkulele -> listOf("A", "E", "C", "G")
                                                        isSevenString -> listOf(
                                                            "e",
                                                            "B",
                                                            "G",
                                                            "D",
                                                            "A",
                                                            "E",
                                                            "B"
                                                        )

                                                        else -> listOf("e", "B", "G", "D", "A", "E")
                                                    },
                                                    openStringsSemis = when {
                                                        isUkulele -> listOf(9, 4, 0, 7)
                                                        isSevenString -> listOf(
                                                            4,
                                                            11,
                                                            7,
                                                            2,
                                                            9,
                                                            4,
                                                            11
                                                        )

                                                        else -> listOf(4, 11, 7, 2, 9, 4)
                                                    },
                                                    onFretTapped = { _, _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    onFretReleased = { _, _, midi ->
                                                        fsManualPressedKeys.remove(midi)
                                                        viewModel.onMidiNoteOff(midi)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Realistic Anatomical Human Hand & Celluloid Pick Overlay for Guitar & Ukulele
                                                GuitarHumanHandsOverlay(
                                                    activeMidiNotes = playAlongAndActiveNotesMap,
                                                    score = currentScore,
                                                    playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                                    playheadOffset = viewModel.scorePlayheadOffset.value,
                                                    numStrings = if (isUkulele) 4 else if (isSevenString) 7 else 6,
                                                    numFrets = if (isUkulele) 15 else 24,
                                                    baseFret = 1,
                                                    openStringsMidi = when {
                                                        isUkulele -> listOf(69, 64, 60, 67)
                                                        isSevenString -> listOf(64, 59, 55, 50, 45, 40, 35)
                                                        else -> listOf(64, 59, 55, 50, 45, 40)
                                                    },
                                                    showFrettingHand = viewModel.showLeftHandGuide.value && !viewModel.isLeftHandHidden.value,
                                                    showPickingHand = viewModel.showRightHandGuide.value && !viewModel.isRightHandHidden.value,
                                                    handAccentColor = primaryColor,
                                                    pickColor = Color(0xFFFFB347),
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Sitar") {
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                com.creativeblisslabs.peqlearn.app.ui.components.UnifiedSitarVisualizer(
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    accentColor = Color(0xFFFFB300),
                                                    showNoteNames = fsShowLetters,
                                                    onFretTapped = { _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Tabla") {
                                                val activeTablaBol = remember(
                                                    viewModel.midiActiveNotes.keys,
                                                    viewModel.midiPlayingNote.value
                                                ) {
                                                    val activeKey = viewModel.midiPlayingNote.value
                                                        ?: viewModel.midiActiveNotes.keys.firstOrNull()
                                                    activeKey?.let { midi ->
                                                        when (midi % 12) {
                                                            0, 1 -> "GHE"
                                                            2, 3 -> "DHA"
                                                            4, 5 -> "TIN"
                                                            6, 7 -> "NA"
                                                            8, 9 -> "TA"
                                                            else -> "KAT"
                                                        }
                                                    }
                                                }
                                                UnifiedTablaVisualizer(
                                                    activeBol = activeTablaBol,
                                                    accentColor = Color(0xFFFFB300),
                                                    onBolTapped = { bol ->
                                                        val midi = when (bol) {
                                                            "GHE" -> 36
                                                            "DHA" -> 38
                                                            "TIN" -> 42
                                                            "NA" -> 46
                                                            "TA" -> 48
                                                            else -> 40
                                                        }
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    onBolReleased = { bol ->
                                                        val midi = when (bol) {
                                                            "GHE" -> 36
                                                            "DHA" -> 38
                                                            "TIN" -> 42
                                                            "NA" -> 46
                                                            "TA" -> 48
                                                            else -> 40
                                                        }
                                                        fsManualPressedKeys.remove(midi)
                                                        viewModel.onMidiNoteOff(midi)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Harmonium") {
                                                UnifiedHarmoniumVisualizer(
                                                    activeMidiNotes = transposedActiveMidiNotesMap,
                                                    accentColor = Color(0xFFFFB300),
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    onKeyTapped = { midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    onKeyReleased = { midi ->
                                                        fsManualPressedKeys.remove(midi)
                                                        viewModel.onMidiNoteOff(midi)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Violin") {
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                UnifiedViolinFingerboard(
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    accentColor = Color(0xFFE040FB),
                                                    showNoteNames = fsShowLetters,
                                                    onPositionTapped = { _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst == "Flute") {
                                                val activeList =
                                                    remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                UnifiedFluteVisualizer(
                                                    activeNotes = transposedActiveList,
                                                    activeMidiNote = transposedPlayingNote,
                                                    accentColor = Color(0xFF00E5FF),
                                                    onToneHoleTapped = { _, midi ->
                                                        if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(
                                                            midi
                                                        )
                                                        viewModel.onMidiNoteOn(midi, 95)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (currentInst.contains(
                                                    "Drum",
                                                    ignoreCase = true
                                                )
                                            ) {
                                                com.creativeblisslabs.peqlearn.app.ui.components.ThreeDDrumVisualizer(
                                                    viewModel = viewModel,
                                                    primaryColor = primaryColor,
                                                    currentKitLabels = viewModel.drumPadLabels,
                                                    showOnlyPartLabel = true,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                UnifiedPianoKeyboard(
                                                    activeMidiNotes = playAlongAndActiveNotesMap,
                                                    customKeyColors = transposedActiveNoteColors,
                                                    accentColor = primaryColor,
                                                    activeCoordinate = viewModel.midiPlayingNote.value,
                                                    isPlaying = isPlaying,
                                                    startNote = 21,
                                                    endNote = 108,
                                                    fillWidth = true,
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                    isRightHandHidden = viewModel.isRightHandHidden.value,
                                                    onKeyTapped = null,
                                                    onKeyReleased = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Articulated Moving Left & Right Human Hands with Finger Numbers (1..5)
                                                PianoHumanHandsOverlay(
                                                    activeMidiNotes = playAlongAndActiveNotesMap,
                                                    score = currentScore,
                                                    playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                                    playheadOffset = viewModel.scorePlayheadOffset.value,
                                                    activeNoteColors = viewModel.activeNoteTrackColors +
                                                            viewModel.playAlongEngine.liveKeyColors,
                                                    startNote = 21,
                                                    endNote = 108,
                                                    showLeftHand = viewModel.showLeftHandGuide.value && !viewModel.isLeftHandHidden.value,
                                                    showRightHand = viewModel.showRightHandGuide.value && !viewModel.isRightHandHidden.value,
                                                    leftHandColor = viewModel.leftHandColor.value,
                                                    rightHandColor = viewModel.rightHandColor.value,
                                                    transposition = totalTranspositionSemis,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Studio Red Velvet Cloth Strip
                                                if (viewModel.keyboardShowVelvetCloth.value) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(4.dp)
                                                            .align(Alignment.TopCenter)
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    listOf(
                                                                        viewModel.keyboardClothColor.value,
                                                                        viewModel.keyboardClothColor.value.copy(
                                                                            alpha = 0.85f
                                                                        ),
                                                                        Color(0xFF330000)
                                                                    )
                                                                )
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Floating 3D Drum Kit Visualizer docked on the right side of the Waterfall above the 3-Pedal Unit
                        val hasActiveDrumTrack = remember(currentScore, enabledTracksSnapshot) {
                            currentScore?.tracks?.any {
                                it.channel == 9 && (enabledTracksSnapshot[it.trackIndex]
                                    ?: it.isEnabled)
                            } == true
                        }
                        val showMini3DDrumOverlay =
                            (hasActiveDrumTrack || viewModel.sequencerTriggeredPads.values.any { it }) &&
                                    fsLayoutMode != "Notation" &&
                                    !viewModel.selectedInstrument.value.contains(
                                        "Drum",
                                        ignoreCase = true
                                    )

                        if (showMini3DDrumOverlay) {
                            val drumOverlayBottomPad = if (viewModel.showPedals.value) {
                                (viewModel.keyboardHeightFactor.floatValue + 94f).dp
                            } else {
                                (viewModel.keyboardHeightFactor.floatValue + 10f).dp
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 12.dp, bottom = drumOverlayBottomPad)
                                    .width(235.dp)
                                    .height(145.dp)
                                    .zIndex(18f)
                                    .shadow(12.dp, RoundedCornerShape(14.dp))
                            ) {
                                com.creativeblisslabs.peqlearn.app.ui.components.ThreeDDrumVisualizer(
                                    viewModel = viewModel,
                                    primaryColor = primaryColor,
                                    currentKitLabels = viewModel.drumPadLabels,
                                    showOnlyPartLabel = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        // 3-Pedal Grand Piano Unit cleanly embedded in right corner
                        if (viewModel.showPedals.value) {
                            val pedalBottomPadding =
                                if (fsLayoutMode == "Notation") 20.dp else 152.dp
                            PianoThreePedalUnit(
                                sustainPressed = viewModel.sustainPedalActive.value,
                                sostenutoPressed = viewModel.sostenutoPedalActive.value,
                                softPressed = viewModel.softPedalActive.value,
                                onPedalClick = { type, pressed ->
                                    viewModel.onMidiPedalEventReceived(type, pressed)
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 12.dp, bottom = pedalBottomPadding)
                                    .graphicsLayer(
                                        alpha = viewModel.pedalOpacity.floatValue,
                                        scaleX = viewModel.pedalScaling.floatValue,
                                        scaleY = viewModel.pedalScaling.floatValue
                                    )
                            )
                        }


                        AnimatedVisibility(
                            visible = areControlsVisible &&
                                    !stageCaptureController.isExportingVideo &&
                                    !viewModel.isVideoExporting.value,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .zIndex(30f),
                            enter = fadeIn(tween(250)),
                            exit = fadeOut(tween(300))
                        ) {

                            Surface(
                                modifier = Modifier
                                    // .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Color(0xFF0F0B18).copy(alpha = 0.92f),
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(1.dp, Color(0xFF281E38))
                            ) {
                                val topBarScrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(topBarScrollState)
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
// Direct Load Score Button + Current Score Title / Filename Readout
                                    val activeScoreTitle = remember(
                                        currentScore?.title,
                                        viewModel.currentMediaItem.value?.title,
                                        viewModel.currentMediaItem.value?.path
                                    ) {
                                        val sTitle = currentScore?.title?.trim().orEmpty()
                                        val mTitle = viewModel.currentMediaItem.value?.title?.trim()
                                            .orEmpty()
                                        val fName = viewModel.currentMediaItem.value?.path
                                            ?.substringAfterLast("/")
                                            ?.substringBeforeLast(".")
                                            ?.replace(Regex("[_-]+"), " ")
                                            ?.trim()
                                            .orEmpty()

                                        when {
                                            sTitle.isNotEmpty() && !sTitle.equals(
                                                "Untitled Score",
                                                true
                                            ) && !sTitle.equals("Imported Score", true) -> sTitle

                                            mTitle.isNotEmpty() && !mTitle.equals(
                                                "Untitled Score",
                                                true
                                            ) -> mTitle

                                            fName.isNotEmpty() -> fName
                                            currentScore != null -> "Loaded Score"
                                            else -> "No Score Loaded"
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .background(
                                                Color(0xFF140E26),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .border(
                                                BorderStroke(1.dp, Color(0xFF382956)),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.onPickScoreFileRequested?.invoke() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFF2E204A
                                                )
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFF523B7A)),
                                            contentPadding = PaddingValues(
                                                horizontal = 10.dp,
                                                vertical = 4.dp
                                            ),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.FileOpen,
                                                contentDescription = "Load Score",
                                                tint = Color(0xFFC5CAE9),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                "Load Score",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC5CAE9)
                                            )
                                        }

                                        VerticalDivider(
                                            modifier = Modifier.height(16.dp).width(1.dp),
                                            color = Color(0xFF382956)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.widthIn(min = 90.dp, max = 210.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (currentScore != null) Color(
                                                            0xFF00E676
                                                        ) else Color.Gray
                                                    )
                                            )
                                            Text(
                                                text = activeScoreTitle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (currentScore != null) Color.White else Color.White.copy(
                                                    alpha = 0.55f
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }


                                    // Helper to cleanly update visualizer layout and notation format
                                    val updateLayout: (String, String, AppViewModel, () -> Unit) -> Unit =
                                        { mode, format, vm, closeMenu ->
                                            fsLayoutMode = mode
                                            if (format.isNotBlank()) {
                                                vm.notationFormat.value = format
                                            }
                                            closeMenu()
                                        }

                                    // A single state controls the visibility of the combined menu
                                    var menuExpanded by remember { mutableStateOf(false) }

                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF1E162E),
                                            border = BorderStroke(1.dp, Color(0xFF372754)),
                                            modifier = Modifier.clickable { menuExpanded = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 5.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ViewQuilt,
                                                    contentDescription = "Layout Mode",
                                                    tint = Color(0xFF81D4FA),
                                                    modifier = Modifier.size(13.dp)
                                                )

                                                // Format the current active state label cleanly
                                                val currentFormatLabel =
                                                    when (viewModel.notationFormat.value.trim()
                                                        .lowercase()) {
                                                        "tab", "tab only" -> "Tab"
                                                        "staff", "staff only" -> "Staff"
                                                        else -> "Both"
                                                    }
                                                val displayLabel = when (fsLayoutMode) {
                                                    "Split" -> "Split ($currentFormatLabel)"
                                                    "Notation" -> "Notation ($currentFormatLabel)"
                                                    "KeyboardWaterfall" -> "Keyboard + Waterfall"
                                                    "Waterfall" -> "Waterfall Only"
                                                    else -> "Keyboard Only"
                                                }

                                                Text(
                                                    displayLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White
                                                )
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            // 1. SPLIT MODE WITH SUB-OPTIONS
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Split (Staff + Waterfall)",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    updateLayout("Split", "staff", viewModel) {
                                                        menuExpanded = false
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Split (Tab + Waterfall)",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    updateLayout("Split", "tab", viewModel) {
                                                        menuExpanded = false
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Split (Both + Waterfall)",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    updateLayout("Split", "both", viewModel) {
                                                        menuExpanded = false
                                                    }
                                                }
                                            )

                                            Divider(color = Color(0xFF372754).copy(alpha = 0.5f))

                                            // 2. NOTATION ONLY WITH SUB-OPTIONS
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Notation Only (Staff)",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    updateLayout(
                                                        "Notation",
                                                        "staff",
                                                        viewModel
                                                    ) { menuExpanded = false }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Notation Only (Tab)",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    updateLayout("Notation", "tab", viewModel) {
                                                        menuExpanded = false
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Notation Only (Both)",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    updateLayout("Notation", "both", viewModel) {
                                                        menuExpanded = false
                                                    }
                                                }
                                            )

                                            Divider(color = Color(0xFF372754).copy(alpha = 0.5f))

                                            // 3. STANDARD MODES
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Keyboard + Waterfall",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    updateLayout(
                                                        "KeyboardWaterfall",
                                                        "",
                                                        viewModel
                                                    ) { menuExpanded = false }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Waterfall Only", fontSize = 12.sp) },
                                                onClick = {
                                                    updateLayout("Waterfall", "", viewModel) {
                                                        menuExpanded = false
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Keyboard Only", fontSize = 12.sp) },
                                                onClick = {
                                                    updateLayout("Keyboard", "", viewModel) {
                                                        menuExpanded = false
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    // pEQInstrument Dropdown for visualizer & audio sample playback
                                    val currentSelectedInstType =
                                        ChordInstrumentType.fromNameOrNull(viewModel.selectedInstrument.value)
                                            ?: ChordInstrumentType.PIANO_ACOUSTIC

                                    pEQInstrumentDropdown(
                                        selectedInstrument = currentSelectedInstType,
                                        accentColor = Color(0xFF64B5F6),
                                        instrumentVolumes = viewModel.instrumentVolumes,
                                        onInstrumentVolumeChange = { inst, vol ->
                                            if (viewModel.selectedInstrument.value == inst.title) {
                                                viewModel.audioEngine.setVolume(vol * viewModel.masterVolume.value)
                                            }
                                        },
                                        onInstrumentChange = { newInst ->
                                            viewModel.selectedInstrument.value = newInst.title
                                            try {
                                                viewModel.audioEngine.setInstrument(newInst.soundEngineName)
                                                val instVol =
                                                    viewModel.instrumentVolumes[newInst.title]
                                                        ?: 1.0f
                                                viewModel.audioEngine.setVolume(instVol * viewModel.masterVolume.value)
                                            } catch (_: Exception) {
                                            }

                                            if (newInst.title.contains("Guitar") || newInst == ChordInstrumentType.UKULELE) {
                                                viewModel.notationFormat.value = "Tab Only"
                                                notationFormat = "Tab Only"
                                            } else if (newInst == ChordInstrumentType.PIANO_ACOUSTIC || newInst == ChordInstrumentType.PIANO_ELECTRIC || newInst == ChordInstrumentType.HARMONIUM) {
                                                viewModel.notationFormat.value = "Staff Only"
                                                notationFormat = "Staff Only"
                                            }
                                        },
                                        modifier = Modifier.width(185.dp).height(32.dp)
                                    )

                                    // MIDI Tracks Studio Mixer & Color-Coded Instrument Selector
                                    val currentScoreObj =
                                        viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score
                                    val foundTracks = currentScoreObj?.tracks ?: emptyList()

                                    // Initialize enabledTrackIndices whenever a new score is loaded
                                    LaunchedEffect(currentScoreObj) {
                                        if (currentScoreObj != null && currentScoreObj.tracks.isNotEmpty()) {
                                            currentScoreObj.tracks.forEach { tr ->
                                                if (!viewModel.enabledTrackIndices.containsKey(tr.trackIndex)) {
                                                    viewModel.enabledTrackIndices[tr.trackIndex] =
                                                        tr.isEnabled
                                                }
                                            }
                                        }
                                    }

                                    if (foundTracks.isNotEmpty()) {
                                        Box {
                                            val activeTracksCount = foundTracks.count { tr ->
                                                viewModel.enabledTrackIndices[tr.trackIndex]
                                                    ?: tr.isEnabled
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFF1E162E),
                                                border = BorderStroke(1.dp, Color(0xFF2E2445)),
                                                modifier = Modifier.clickable {
                                                    fsInstrumentMenuExpanded = true
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = 9.dp,
                                                        vertical = 5.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Tune,
                                                        contentDescription = null,
                                                        tint = Color(0xFF9FA8DA),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        "Tracks ($activeTracksCount/${foundTracks.size})",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White
                                                    )
                                                    Icon(
                                                        Icons.Default.ArrowDropDown,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = fsInstrumentMenuExpanded,
                                                onDismissRequest = {
                                                    fsInstrumentMenuExpanded = false
                                                },
                                                modifier = Modifier
                                                    .widthIn(min = 300.dp, max = 360.dp)
                                                    .background(Color(0xFF140E26))
                                                    .border(
                                                        BorderStroke(1.dp, Color(0xFF382956)),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            horizontal = 14.dp,
                                                            vertical = 6.dp
                                                        ),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "MIDI TRACKS & INSTRUMENTS (${foundTracks.size})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                    Text(
                                                        text = "ALL ON",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF81D4FA),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                foundTracks.forEach { tr ->
                                                                    viewModel.enabledTrackIndices[tr.trackIndex] =
                                                                        true
                                                                }
                                                            }
                                                            .padding(
                                                                horizontal = 6.dp,
                                                                vertical = 2.dp
                                                            )
                                                    )
                                                }
                                                HorizontalDivider(color = Color(0xFF281C3F))

                                                foundTracks.forEach { track ->
                                                    val isEnabled =
                                                        viewModel.enabledTrackIndices[track.trackIndex]
                                                            ?: track.isEnabled
                                                    val isDrumTrack = track.channel == 9
                                                    val trackColor = when {
                                                        isDrumTrack -> Color(0xFFFF5252)
                                                        track.trackIndex == 0 -> viewModel.rightHandColor.value
                                                        track.trackIndex == 1 -> viewModel.leftHandColor.value
                                                        else -> Color(
                                                            com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX[
                                                                track.trackIndex % com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX.size
                                                            ]
                                                        )
                                                    }

                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(
                                                                        8.dp
                                                                    )
                                                                ) {
                                                                    // Color-coded Track Swatch
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(12.dp)
                                                                            .clip(CircleShape)
                                                                            .background(
                                                                                if (isEnabled) trackColor else trackColor.copy(
                                                                                    alpha = 0.25f
                                                                                )
                                                                            )
                                                                            .border(
                                                                                1.dp,
                                                                                Color.White.copy(
                                                                                    alpha = 0.6f
                                                                                ),
                                                                                CircleShape
                                                                            )
                                                                    )
                                                                    val trackVol =
                                                                        viewModel.trackVolumes[track.trackIndex]
                                                                            ?: 1.0f
                                                                    Column(
                                                                        modifier = Modifier.weight(
                                                                            1f
                                                                        )
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Text(
                                                                                track.trackName.ifBlank { "Track ${track.trackIndex + 1}" },
                                                                                fontSize = 12.sp,
                                                                                fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                                                                color = if (isEnabled) Color.White else Color.White.copy(
                                                                                    alpha = 0.45f
                                                                                ),
                                                                                maxLines = 1,
                                                                                modifier = Modifier.weight(
                                                                                    1f
                                                                                )
                                                                            )
                                                                            Text(
                                                                                "${(trackVol * 100).toInt()}%",
                                                                                fontSize = 9.5.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = trackColor
                                                                            )
                                                                        }
                                                                        val roleTag =
                                                                            if (isDrumTrack) "GM Percussion • 3D Kit" else "Ch ${track.channel + 1} • Patch ${track.program + 1}"
                                                                        Text(
                                                                            "$roleTag • ${track.noteCount} notes",
                                                                            fontSize = 10.sp,
                                                                            color = if (isDrumTrack) Color(
                                                                                0xFFFF8A80
                                                                            ) else Color.White.copy(
                                                                                alpha = 0.55f
                                                                            )
                                                                        )
                                                                        Slider(
                                                                            value = trackVol,
                                                                            onValueChange = { newV ->
                                                                                viewModel.trackVolumes[track.trackIndex] =
                                                                                    newV
                                                                                if (newV <= 0.01f) {
                                                                                    viewModel.enabledTrackIndices[track.trackIndex] =
                                                                                        false
                                                                                } else if (!isEnabled) {
                                                                                    viewModel.enabledTrackIndices[track.trackIndex] =
                                                                                        true
                                                                                }
                                                                            },
                                                                            valueRange = 0f..1f,
                                                                            modifier = Modifier.fillMaxWidth()
                                                                                .height(20.dp),
                                                                            colors = SliderDefaults.colors(
                                                                                thumbColor = trackColor,
                                                                                activeTrackColor = trackColor,
                                                                                inactiveTrackColor = Color.White.copy(
                                                                                    alpha = 0.15f
                                                                                )
                                                                            )
                                                                        )
                                                                    }
                                                                }

                                                                // SOLO Button ("S")
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = Color(0xFF281E3D),
                                                                    border = BorderStroke(
                                                                        1.dp,
                                                                        Color(0xFFFFD700).copy(alpha = 0.5f)
                                                                    ),
                                                                    modifier = Modifier
                                                                        .padding(horizontal = 4.dp)
                                                                        .clickable {
                                                                            foundTracks.forEach { other ->
                                                                                viewModel.enabledTrackIndices[other.trackIndex] =
                                                                                    (other.trackIndex == track.trackIndex)
                                                                            }
                                                                        }
                                                                ) {
                                                                    Text(
                                                                        "SOLO",
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Black,
                                                                        color = Color(0xFFFFD700),
                                                                        modifier = Modifier.padding(
                                                                            horizontal = 6.dp,
                                                                            vertical = 3.dp
                                                                        )
                                                                    )
                                                                }

                                                                Switch(
                                                                    checked = isEnabled,
                                                                    onCheckedChange = { checked ->
                                                                        viewModel.enabledTrackIndices[track.trackIndex] =
                                                                            checked
                                                                    }
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            val currentVal =
                                                                viewModel.enabledTrackIndices[track.trackIndex]
                                                                    ?: track.isEnabled
                                                            viewModel.enabledTrackIndices[track.trackIndex] =
                                                                !currentVal
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }


                                    // Guided Play-Along & Flashing Key Follow-Up Studio Button
                                    val isPlayAlongActive =
                                        viewModel.playAlongEngine.isEvaluating.value || viewModel.isGuidedPlayAlongActive.value
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isPlayAlongActive) Color(0xFF143328) else Color(
                                            0xFF1E162E
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isPlayAlongActive) Color(0xFF00E676) else Color(
                                                0xFF2E2445
                                            )
                                        ),
                                        modifier = Modifier.clickable {
                                            showPlayAlongStatsDialog = true
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 9.dp,
                                                vertical = 5.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.GraphicEq,
                                                contentDescription = "Guided Play-Along",
                                                tint = if (isPlayAlongActive) Color(0xFF00E676) else Color.White.copy(
                                                    alpha = 0.75f
                                                ),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (isPlayAlongActive) {
                                                    when (viewModel.playAlongPracticeMode.value) {
                                                        "WAIT_FOR_NOTE" -> "Step-Wait"
                                                        "MEASURE_FOLLOWUP" -> "Bar Follow"
                                                        "TEMPO_RAMP" -> "Tempo Ramp"
                                                        else -> "Live Eval"
                                                    }
                                                } else "Play-Along",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPlayAlongActive) Color(0xFF00E676) else Color.White.copy(
                                                    alpha = 0.85f
                                                )
                                            )
                                        }
                                    }
                                    // USB MIDI Device Active Toggle Button (Toggles USB MIDI Visualizer Input ON/OFF)
                                    val isMidiHardwareConnected =
                                        viewModel.connectedMidiDeviceName.value?.isNotBlank() == true
                                    val isUsbInputActive = viewModel.isUsbMidiInputEnabled.value
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = when {
                                            isMidiHardwareConnected && isUsbInputActive -> Color(
                                                0xFF1C2B24
                                            )

                                            isMidiHardwareConnected && !isUsbInputActive -> Color(
                                                0xFF2B1C1C
                                            )

                                            else -> Color(0xFF1E162E)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isMidiHardwareConnected && isUsbInputActive -> Color(
                                                    0xFF10B981
                                                )

                                                isMidiHardwareConnected && !isUsbInputActive -> Color(
                                                    0xFFFF5252
                                                ).copy(
                                                    alpha = 0.6f
                                                )

                                                else -> Color(0xFF2E2445)
                                            }
                                        ),
                                        modifier = Modifier.clickable {
                                            if (isMidiHardwareConnected) {
                                                viewModel.toggleUsbMidiInput()
                                            } else {
                                                viewModel.onScanMidiDevicesRequested?.invoke()
                                                viewModel.toggleUsbMidiInput()
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 9.dp,
                                                vertical = 5.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Piano,
                                                contentDescription = "USB MIDI Input Toggle",
                                                tint = when {
                                                    !isUsbInputActive -> Color(0xFFFF5252)
                                                    isMidiHardwareConnected -> Color(0xFF10B981)
                                                    else -> Color.White.copy(alpha = 0.7f)
                                                },
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (isUsbInputActive) "USB ON" else "USB OFF",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUsbInputActive) Color(0xFF10B981) else Color(
                                                    0xFFFF8A80
                                                )
                                            )
                                        }
                                    }

                                    // Export Button
                                    Button(
                                        onClick = { showExportDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF2A2045
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFF4C3D75)),
                                        contentPadding = PaddingValues(
                                            horizontal = 11.dp,
                                            vertical = 4.dp
                                        ),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Upload,
                                            contentDescription = "Export",
                                            tint = Color(0xFFC5CAE9),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Export",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFC5CAE9)
                                        )
                                    }
                                    // Ear Trainer & Sight-Reading Academy Button
                                    IconButton(
                                        onClick = { showMusicianshipAcademyScreen = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = "Rhythm, Ear Training & " +
                          "ing Academy",
                                            tint = Color(0xFFFFD370),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // AI Audio-to-Score Model
                                    IconButton(
                                        onClick = { showAudioToMidiScreen = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "AI Audio to MIDI / Sheet Music",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }


                                    // Learn Mode (Scales, Arpeggios & Chords Trainer)
                                    IconButton(
                                        onClick = { showLearnModeDialog = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LibraryMusic,
                                            contentDescription = "Library & Practice",
                                            tint = Color(0xFF80CBC4),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Settings Menu
                                    IconButton(
                                        onClick = { viewModel.showSettings.value = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Menu / Settings",
                                            tint = Color.White.copy(0.75f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // 13. Close / Dismiss Pill Button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1E162E),
                                        modifier = Modifier.size(26.dp)
                                    ) {

                                        IconButton(
                                            onClick = {
                                                viewModel.isFullscreenKeyboard.value = false
                                            }, // FIXED: Exits fullscreen!
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close Fullscreen",
                                                tint = Color.White.copy(0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }


                        // --- FLOATING BOTTOM PLAYBACK BAR ---
                        val controlsBottomPadding =
                            if (fsLayoutMode == "Notation") 18.dp else 155.dp


                        AnimatedVisibility(
                            visible = areControlsVisible &&
                                    !stageCaptureController.isExportingVideo &&
                                    !viewModel.isVideoExporting.value,
                            enter = fadeIn(tween(250)),
                            exit = fadeOut(tween(300)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter) // Anchor playback bar to screen bottom
                                .zIndex(20f)
                                .padding(bottom = controlsBottomPadding)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = Color(0xFF0F0B18).copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, Color(0xFF281E38)),
                                shadowElevation = 10.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                val bottomControlsScrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(bottomControlsScrollState)
                                        .height(IntrinsicSize.Min) // Prevent vertical stretching
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Drag / Grip Handle
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Grip",
                                        tint = Color.White.copy(0.35f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .align(Alignment.CenterVertically)
                                    )

                                    // 3. Previous Measure / Previous PDF Page (Long-press or double-click at Bar 1 resets to 0:00)
                                    IconButton(
                                        onClick = { viewModel.stepToPreviousMeasureOrPage() },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        Icon(
                                            Icons.Default.SkipPrevious,
                                            contentDescription = "Previous Measure / Page",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // 4. Circular Glowing Play / Pause Button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF5C6BC0),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleScorePlayback() },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 5. Next Measure / Next PDF Page
                                    IconButton(
                                        onClick = { viewModel.stepToNextMeasureOrPage() },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = "Next Measure / Page",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // 6. Current Time Elapsed
                                    Text(
                                        currentTimeStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(0.65f),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )

                                    // 7. Blue Scrubbing Progress Slider
                                    Slider(
                                        value = timelineProgress,
                                        onValueChange = { targetProgress ->
                                            if (currentScore != null && totalSeconds > 0) {
                                                val targetTotalSec =
                                                    targetProgress * totalSeconds.toFloat()
                                                var accumulated = 0f
                                                var targetM = 0
                                                var targetO = 0f
                                                for (i in currentScore.measures.indices) {
                                                    val m = currentScore.measures[i]
                                                    val quarterBeats =
                                                        m.timeSignatureNumerator.toFloat() * (4f / m.timeSignatureDenominator.coerceAtLeast(
                                                            1
                                                        ).toFloat())
                                                    val mDur = (60f / effectiveTempo) * quarterBeats
                                                    if (targetTotalSec < accumulated + mDur) {
                                                        targetM = i
                                                        targetO =
                                                            ((targetTotalSec - accumulated) / mDur).coerceIn(
                                                                0f,
                                                                1f
                                                            )
                                                        break
                                                    }
                                                    accumulated += mDur
                                                    if (i == currentScore.measures.lastIndex) {
                                                        targetM = i
                                                        targetO = 1.0f
                                                    }
                                                }
                                                viewModel.seekToPosition(targetM, targetO)
                                            }
                                        },
                                        modifier = Modifier
                                            .width(200.dp)
                                            .align(Alignment.CenterVertically),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF5C6BC0),
                                            inactiveTrackColor = Color(0xFF231838)
                                        )
                                    )

                                    // 8. Total Song Duration
                                    Text(
                                        totalTimeStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(0.65f),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )

                                    Spacer(
                                        Modifier
                                            .width(2.dp)
                                            .align(Alignment.CenterVertically)
                                    )

                                    // 9. Volume Control Slider (Synced with AudioEngine & Score Synth)
                                    Icon(
                                        imageVector = if (viewModel.masterVolume.value <= 0.01f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Volume",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.CenterVertically)
                                            .clickable {
                                                val nextVol =
                                                    if (viewModel.masterVolume.value > 0.01f) 0f else 0.85f
                                                viewModel.masterVolume.value = nextVol
                                                viewModel.audioEngine.setVolume(nextVol)
                                            }
                                    )
                                    Slider(
                                        value = viewModel.masterVolume.value,
                                        onValueChange = { newVol ->
                                            viewModel.masterVolume.value = newVol
                                            viewModel.audioEngine.setVolume(newVol)
                                        },
                                        modifier = Modifier
                                            .width(110.dp)
                                            .align(Alignment.CenterVertically),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF5C6BC0),
                                            inactiveTrackColor = Color(0xFF231838)
                                        )
                                    )

                                    // 10. Speed Control Pill with +/- Buttons
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E162E),
                                        border = BorderStroke(1.dp, Color(0xFF2E2445)),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 2.dp
                                            )
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.playbackSpeed.value =
                                                        when (viewModel.playbackSpeed.value) {
                                                            1.5f -> 1.25f
                                                            1.25f -> 1.0f
                                                            1.0f -> 0.75f
                                                            0.75f -> 0.5f
                                                            else -> 0.5f
                                                        }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Decrease Speed",
                                                    tint = Color.White.copy(0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Text(
                                                text = "${viewModel.playbackSpeed.value}x",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(0.9f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .padding(horizontal = 6.dp)
                                                    .widthIn(min = 36.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    viewModel.playbackSpeed.value =
                                                        when (viewModel.playbackSpeed.value) {
                                                            0.5f -> 0.75f
                                                            0.75f -> 1.0f
                                                            1.0f -> 1.25f
                                                            1.25f -> 1.5f
                                                            else -> 1.5f
                                                        }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Increase Speed",
                                                    tint = Color.White.copy(0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 11. Transpose Pill with Octave Shifts & Semitone Buttons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1E162E))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                viewModel.transposition.intValue =
                                                    (viewModel.transposition.intValue - 12).coerceAtLeast(
                                                        -12
                                                    )
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Octave Down",
                                                tint = Color.White.copy(0.4f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.transposition.intValue =
                                                    (viewModel.transposition.intValue - 1).coerceAtLeast(
                                                        -12
                                                    )
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "Transpose Down",
                                                tint = Color.White.copy(0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        Text(
                                            text = if (viewModel.transposition.intValue > 0) "+${viewModel.transposition.intValue}" else "${viewModel.transposition.intValue}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .clickable { viewModel.transposition.intValue = 0 }
                                                .padding(horizontal = 4.dp)
                                                .widthIn(min = 20.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                viewModel.transposition.intValue =
                                                    (viewModel.transposition.intValue + 1).coerceAtMost(
                                                        12
                                                    )
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Transpose Up",
                                                tint = Color.White.copy(0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.transposition.intValue =
                                                    (viewModel.transposition.intValue + 12).coerceAtMost(
                                                        12
                                                    )
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Octave Up",
                                                tint = Color.White.copy(0.4f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    // 11. Metronome / Notification Bell
                                    IconButton(
                                        onClick = {
                                            viewModel.metronomeEnabled.value =
                                                !viewModel.metronomeEnabled.value
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = "Metronome",
                                            tint = if (viewModel.metronomeEnabled.value) Color(
                                                0xFF7986CB
                                            ) else Color.White.copy(0.45f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // 12. Independent Left/Right Hand Note & Moving Virtual Hand Guide Toggles
                                    val lhNotesEnabled = !viewModel.isLeftHandHidden.value
                                    val rhNotesEnabled = !viewModel.isRightHandHidden.value
                                    val lhHandGuideOn = viewModel.showLeftHandGuide.value
                                    val rhHandGuideOn = viewModel.showRightHandGuide.value

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1E162E))
                                            .border(
                                                BorderStroke(1.dp, Color(0xFF372754)),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        // Left Hand Notes Toggle (LH)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (lhNotesEnabled) viewModel.leftHandColor.value else Color.Transparent)
                                                .clickable {
                                                    val nextHidden =
                                                        !viewModel.isLeftHandHidden.value
                                                    viewModel.isLeftHandHidden.value = nextHidden
                                                    viewModel.isLeftHandMuted.value = nextHidden
                                                    viewModel.enabledTrackIndices[1] = !nextHidden
                                                }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "LH",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (lhNotesEnabled) Color.White else Color.White.copy(
                                                    alpha = 0.45f
                                                )
                                            )
                                        }

                                        // Right Hand Notes Toggle (RH)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (rhNotesEnabled) viewModel.rightHandColor.value else Color.Transparent)
                                                .clickable {
                                                    val nextHidden =
                                                        !viewModel.isRightHandHidden.value
                                                    viewModel.isRightHandHidden.value = nextHidden
                                                    viewModel.isRightHandMuted.value = nextHidden
                                                    viewModel.enabledTrackIndices[0] = !nextHidden
                                                }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "RH",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (rhNotesEnabled) Color.White else Color.White.copy(
                                                    alpha = 0.45f
                                                )
                                            )
                                        }

                                        VerticalDivider(
                                            modifier = Modifier.height(14.dp).width(1.dp),
                                            color = Color(0xFF372754)
                                        )

                                        // Left Moving Hand Guide Toggle (🖐L)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (lhHandGuideOn) viewModel.leftHandColor.value.copy(
                                                        alpha = 0.35f
                                                    ) else Color.Transparent
                                                )
                                                .border(
                                                    1.dp,
                                                    if (lhHandGuideOn) viewModel.leftHandColor.value else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.showLeftHandGuide.value =
                                                        !lhHandGuideOn
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "🖐L",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (lhHandGuideOn) Color.White else Color.White.copy(
                                                    alpha = 0.4f
                                                )
                                            )
                                        }

                                        // Right Moving Hand Guide Toggle (🖐R)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (rhHandGuideOn) viewModel.rightHandColor.value.copy(
                                                        alpha = 0.35f
                                                    ) else Color.Transparent
                                                )
                                                .border(
                                                    1.dp,
                                                    if (rhHandGuideOn) viewModel.rightHandColor.value else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.showRightHandGuide.value =
                                                        !rhHandGuideOn
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "🖐R",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rhHandGuideOn) Color.White else Color.White.copy(
                                                    alpha = 0.4f
                                                )
                                            )
                                        }
                                    }

                                    // 13. Close / Dismiss Pill Button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1E162E),
                                        modifier = Modifier
                                            .size(26.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        IconButton(
                                            onClick = { areControlsVisible = false },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Hide Controls",
                                                tint = Color.White.copy(0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // Big Centered Export Progress Dialog & Non-Responsive Backdrop
                    if (viewModel.isVideoExporting.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(50f)
                                .background(Color.Black.copy(alpha = 0.82f))
                                .pointerInput(Unit) {
                                    detectTapGestures { } // Consume all taps/clicks so background is non-responsive
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF12131A),
                                border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.7f)),
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .fillMaxWidth(0.88f)
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Header Title with Movie Icon
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "Rendering & Exporting Video",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Format Details Badge
                                    Text(
                                        text = "${viewModel.videoExportResolution.value.uppercase()} @ ${viewModel.videoExportFps.value} FPS • ${viewModel.videoExportTheme.value} Theme",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )

                                    // Circular Progress Gauge with Centered Percentage
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(110.dp)
                                            .padding(6.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { viewModel.videoExportProgress.floatValue },
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFFFFD700),
                                            trackColor = Color(0xFF282A36),
                                            strokeWidth = 9.dp
                                        )
                                        Text(
                                            text = "${(viewModel.videoExportProgress.floatValue * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    // Linear Progress Bar
                                    LinearProgressIndicator(
                                        progress = { viewModel.videoExportProgress.floatValue },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFFFFD700),
                                        trackColor = Color(0xFF282A36)
                                    )

                                    // Details Card: Frame Count & Speed/ETA
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF0A0B10),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Frame Progress:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = viewModel.videoExportFrameText.value.ifBlank { "Processing Frames..." },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Encoding Speed / ETA:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = viewModel.videoExportEtaText.value.ifBlank { "Calculating ETA..." },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Background Execution & Screen Wake Tip
                                    Text(
                                        text = "🔒 Screen is kept awake. You can safely switch apps — export will continue running in background via ongoing notification.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    // Speed Optimization Alert Dialog when frames / audio lag or drops are detected
                    if (showSpeedOptimizationDialog) {
                        AlertDialog(
                            onDismissRequest = { showSpeedOptimizationDialog = false },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFF5C6BC0),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Optimize Visualization Speed",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            },
                            text = {
                                Text(
                                    "Audio/frame delay or rendering drop was detected. Switching to the recommended speed (Slow / 0.75x) ensures smooth 60fps playback without stutter, lag, or dropped notes.",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(0.85f),
                                    lineHeight = 18.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.fallingNotesSpeed.value = "Slow"
                                        viewModel.playbackSpeed.value = 0.75f
                                        showSpeedOptimizationDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF5C6BC0
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Apply Recommended Speed",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showSpeedOptimizationDialog = false }
                                ) {
                                    Text(
                                        "Keep Current",
                                        color = Color.White.copy(0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            containerColor = Color(0xFF1E162E),
                            tonalElevation = 6.dp
                        )
                    }

                }
            }

            // Comprehensive Export Studio Modal Bottom Sheet
            if (showExportDialog) {
                var exportStudioTab by rememberSaveable { mutableIntStateOf(0) }
                ModalBottomSheet(
                    onDismissRequest = { showExportDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (exportStudioTab == 0) Icons.Default.Movie else Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (exportStudioTab == 0) "Performance Video Studio" else "Sheet Music PDF Export",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Parser Score .txt Dump button
                            Button(
                                onClick = { showDumpDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF1565C0
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                // Fully qualified core baseline icon
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                                    contentDescription = "Score Dump",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                // Fully qualified Spacer package path
                                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                                Text(
                                    "Score TXT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            // Piano Hands Overlay Dump button
                            Button(
                                onClick = { showHandsDumpDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF4A148C
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Piano,
                                    contentDescription = "Hands Overlay Dump",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                                Text(
                                    "Hands TXT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { showExportDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        // Tab selector between Video Export and PDF Score Export
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf("Video Studio", "Sheet Music PDF")
                            tabs.forEachIndexed { index, title ->
                                val isSelected = exportStudioTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { exportStudioTab = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (exportStudioTab == 0) {
                            VideoExportStudioSection(
                                viewModel = viewModel,
                                onExportStarted = {
                                    showExportDialog = false
                                }
                            )
                        } else {
                            PdfExportStudioSection(
                                viewModel = viewModel,
                                onExportStarted = {
                                    showExportDialog = false
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            if (viewModel.showSettings.value) {
                PlaybackSettingsSheet(
                    viewModel = viewModel,
                    notationFormat = viewModel.notationFormat.value,
                    onFormatChange = { viewModel.notationFormat.value = it },
                    showLetters = viewModel.showPianoKeyLabels.value,
                    onShowLettersChange = { viewModel.showPianoKeyLabels.value = it },
                    showLyrics = viewModel.showLyrics.value,
                    onShowLyricsChange = { viewModel.showLyrics.value = it },
                    initialTab = 0,
                    onDismiss = { viewModel.showSettings.value = false }
                )
            }
        }
    }

