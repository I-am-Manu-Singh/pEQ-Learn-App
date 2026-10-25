package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.AutoAwesome
import com.creativeblisslabs.peqlearn.app.ui.components.VideoExportStudioSection
import com.creativeblisslabs.peqlearn.app.ui.components.PdfExportStudioSection
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.creativeblisslabs.peqlearn.app.ui.components.FullscreenDialog
import com.creativeblisslabs.peqlearn.app.ui.components.setScreenOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.setSystemBarsVisibility
import com.creativeblisslabs.peqlearn.app.ui.components.pEQOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedGuitarFretboard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.playMediaItem
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.persistSession
import com.creativeblisslabs.peqlearn.app.util.LiveChordDetector
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PianoKeyboard88Keys(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    activeMidiNotes: Set<Int> = emptySet(),
    modifier: Modifier = Modifier,
    onNotePressed: (Int) -> Unit = {}
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

    val fallingBlocks = remember { mutableStateListOf<PianoFallingNoteBlock>() }
    val inlineSparks = remember { mutableStateListOf<PianoSpark>() }
    val inlineFlashes = remember { mutableStateListOf<PianoKeyFlash>() }
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

    var frameTicker by remember { mutableLongStateOf(0L) }
    var internalKeyboardModeName by rememberSaveable { mutableStateOf("PIANO") }
    LaunchedEffect(internalKeyboardModeName) {
        if (internalKeyboardModeName == "PIANO") {
            viewModel.audioEngine.setInstrument("PIANO")
        }
    }

    // var showDumpDialog by rememberSaveable { mutableStateOf(false) }
    // var showPlayAlongStatsDialog by rememberSaveable { mutableStateOf(false) }
    // var showLearnModeDialog by rememberSaveable { mutableStateOf(false) }
    // var showAudioToMidiScreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pressedKeys.toList(), viewModel.rightHandColor.value, viewModel.leftHandColor.value) {
        val rhColor = viewModel.rightHandColor.value
        val lhColor = viewModel.leftHandColor.value
        pressedKeys.forEach { note ->
            val color = when {
                internalKeyboardModeName == "DRUM" -> if (isMidiNoteBlack(note)) Color(0xFFFFB300) else Color(0xFFFF6F00)
                note >= 60 -> if (isMidiNoteBlack(note)) rhColor.copy(alpha = 0.95f) else rhColor
                else -> if (isMidiNoteBlack(note)) lhColor.copy(alpha = 0.95f) else lhColor
            }
            if (fallingBlocks.none { it.midiNote == note && it.isAscending && it.currentY > 100f }) {
                fallingBlocks.add(PianoFallingNoteBlock(midiNote = note, startY = 140f, currentY = 140f, color = color, isAscending = true, alpha = 1.0f))
            }
        }
    }

    LaunchedEffect(activeNotesSnapshot.value, viewModel.rightHandColor.value, viewModel.leftHandColor.value) {
        if (viewModel.currentScore.value == null || !viewModel.isScorePlaying.value) {
            val rhColor = viewModel.rightHandColor.value
            val lhColor = viewModel.leftHandColor.value
            activeNotesSnapshot.value.forEach { note ->
                if (note !in pressedKeys) {
                    val color = when {
                        internalKeyboardModeName == "DRUM" -> if (isMidiNoteBlack(note)) Color(0xFFFFB300) else Color(0xFFFF6F00)
                        note >= 60 -> if (isMidiNoteBlack(note)) rhColor.copy(alpha = 0.95f) else rhColor
                        else -> if (isMidiNoteBlack(note)) lhColor.copy(alpha = 0.95f) else lhColor
                    }
                    if (fallingBlocks.none { it.midiNote == note && !it.isAscending && it.currentY < 50f }) {
                        fallingBlocks.add(PianoFallingNoteBlock(midiNote = note, startY = 0f, currentY = 0f, color = color, isAscending = false, alpha = 1.0f))
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val gravity = 0.28f
        while (true) {
            val speedMultiplier = when (viewModel.fallingNotesSpeed.value) {
                "Slow" -> 0.75f
                "Fast" -> 1.5f
                "Extreme" -> 2.2f
                else -> 1.1f
            }
            val stepY = 8.5f * speedMultiplier

            val iterator = fallingBlocks.iterator()
            while (iterator.hasNext()) {
                val block = iterator.next()
                if (block.isAscending) {
                    block.currentY -= stepY
                    block.startY = block.currentY + 28f
                    block.alpha = (block.currentY / 140f).coerceIn(0f, 1f)
                    if (block.currentY < -30f || block.alpha <= 0.05f) {
                        iterator.remove()
                    }
                } else {
                    block.currentY += stepY
                    if (block.currentY > 220f) {
                        iterator.remove()
                    }
                }
            }

            // Update Particle Physics
            val sIter = inlineSparks.listIterator()
            while (sIter.hasNext()) {
                val s = sIter.next()
                s.x += s.vx
                s.y += s.vy
                s.vy += gravity
                s.vx *= 0.985f
                s.currentLife -= 1f
                s.alpha = (s.currentLife / s.maxLife).coerceIn(0f, 1f)
                if (s.currentLife <= 0f || s.alpha <= 0f) {
                    sIter.remove()
                }
            }

            // Update Key Strike Flashes
            val fIter = inlineFlashes.listIterator()
            while (fIter.hasNext()) {
                val f = fIter.next()
                f.radius += 1.8f
                f.alpha *= 0.86f
                if (f.alpha < 0.03f) {
                    fIter.remove()
                }
            }

            frameTicker++
            delay(16)
        }
    }

    val isFullscreen = viewModel.isFullscreenKeyboard.value
    val activeMidiNoteSet by remember {
        derivedStateOf { viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key }.toSet() }
    }

    var showDumpDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayAlongStatsDialog by rememberSaveable { mutableStateOf(false) }
    var showLearnModeDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioToMidiScreen by rememberSaveable { mutableStateOf(false) }

    // CRITICAL FIX: Do NOT render or tick the inline Notation + Waterfall + Keyboard when Fullscreen mode is open!
    if (!isFullscreen) {
        LearnSectionCard(title = "Notation & Piano Viewer", icon = Icons.Filled.List) {
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.onPickScoreFileRequested?.invoke() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = "Open Score", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(3.dp))
                            Text("Import", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
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
                            currentFormat = notationFormat,
                            onFormatChange = { notationFormat = it },
                            showLetters = viewModel.showNotationNoteNames.value,
                            isSplitStaff = viewModel.isSplitStaff.value,
                            isVerticalScroll = viewModel.isVerticalScroll.value,
                            leftHandColor = viewModel.leftHandColor.value,
                            rightHandColor = viewModel.rightHandColor.value,
                            isLeftHandHidden = viewModel.isLeftHandHidden.value,
                            isRightHandHidden = viewModel.isRightHandHidden.value,
                            enabledTrackIndices = viewModel.enabledTrackIndices,
                            modifier = Modifier.fillMaxSize()
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
                                    playbackSpeed = viewModel.playbackSpeed.value
                                )
                            }

                            if (layoutMode != "Waterfall") {
                                UnifiedPianoKeyboard(
                                    activeMidiNotes = viewModel.midiActiveNotes,
                                    accentColor = primaryColor,
                                    activeCoordinate = viewModel.midiPlayingNote.value,
                                    isPlaying = viewModel.isScorePlaying.value,
                                    startNote = startMidi,
                                    endNote = startMidi + totalKeys - 1,
                                    leftHandColor = viewModel.leftHandColor.value,
                                    rightHandColor = viewModel.rightHandColor.value,
                                    customKeyColors = viewModel.playAlongEngine.liveKeyColors,
                                    onKeyTapped = { midiNote ->
                                        if (midiNote !in pressedKeys) pressedKeys.add(midiNote)
                                        onNotePressed(midiNote)
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        val freq = viewModel.tunerReferenceFrequency.value * 2.0.pow((midiNote - 69) / 12.0).toFloat()
                                        viewModel.detectedPitch.value = freq
                                        viewModel.detectedNote.value = getNoteNameFromMidi(midiNote)
                                    },
                                    onKeyReleased = { midiNote ->
                                        viewModel.onMidiNoteOff(midiNote)
                                        pressedKeys.remove(midiNote)
                                    },
                                    modifier = Modifier
                                        .width(keyboardWidth)
                                        .height(whiteKeyHeight)
                                        .padding(vertical = 4.dp)
                                )
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
            val titleSlug = currentScoreObj?.title?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "score"
            com.creativeblisslabs.peqlearn.app.shareTextFile("${titleSlug}_parser_dump.txt", dumpText)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
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
        Text("Share File", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
    Button(
        onClick = {
            clipboardManager.setText(AnnotatedString(dumpText))
            copiedMessage = true
        },
        colors = ButtonDefaults.buttonColors(containerColor = if (copiedMessage) Color(0xFF2E7D32) else Color(0xFF1976D2)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Swapped for standard core baseline icons to bypass compilation failure
        Icon(
            imageVector = if (copiedMessage) androidx.compose.material.icons.Icons.Default.Close else androidx.compose.material.icons.Icons.Default.Menu, 
            contentDescription = null, 
            modifier = Modifier.size(16.dp)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
        Text(if (copiedMessage) "Copied!" else "Copy Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // Play-Along Evaluation & Live Accuracy Scorecard Dialog
    if (showPlayAlongStatsDialog) {
        val scorecard = viewModel.playAlongEngine.scorecard.value
        val isEvaluating = viewModel.playAlongEngine.isEvaluating.value

        ModalBottomSheet(
            onDismissRequest = { showPlayAlongStatsDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0F0B1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Live Play-Along Performance", color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (isEvaluating) "Listening to USB MIDI Keyboard Input..." else "Evaluation Stopped", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { showPlayAlongStatsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
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
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ACCURACY", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${scorecard.accuracyPercentage}%", color = Color(0xFF00E676), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Avg timing error: ${scorecard.averageTimingErrorMs} ms", color = Color.White.copy(0.7f), fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF9100)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(scorecard.grade, color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Detailed Statistics Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF161226), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HIT NOTES", fontSize = 10.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold)
                            Text("${scorecard.hitNotesCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF161226), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MISSED", fontSize = 10.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold)
                            Text("${scorecard.missedNotesCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF161226), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("STREAK", fontSize = 10.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold)
                            Text("${scorecard.currentStreak}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF161226), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BEST", fontSize = 10.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold)
                            Text("${scorecard.bestStreak}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80D8FF))
                        }
                    }
                }

                // Control Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (isEvaluating) {
                                viewModel.playAlongEngine.stopEvaluation()
                            } else {
                                val s = viewModel.currentScore.value as? Score
                                if (s != null) viewModel.playAlongEngine.startEvaluation(s, s.tempo)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEvaluating) Color(0xFFD32F2F) else Color(0xFF00E676),
                            contentColor = if (isEvaluating) Color.White else Color.Black
                        )
                    ) {
                        Text(if (isEvaluating) "Stop Evaluating" else "Start Evaluating", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Learn Mode (Scales & Chords Practice Library) Dialog
    if (showLearnModeDialog) {
        ModalBottomSheet(
            onDismissRequest = { showLearnModeDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0E0B1A)
        ) {
            var practiceStudioTab by remember { mutableIntStateOf(0) } // 0 = Masterworks Tutorials, 1 = Scales, Chords & Exercises
            var selectedTrainingMode by remember { mutableStateOf("FULL") } // "FULL", "RH_ONLY", "LH_ONLY", "SLOW_TEMPO"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Interactive Practice Studio", color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Masterworks, Hand Separation & Technical Mastery", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { showLearnModeDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Tab Switcher: Inbuilt Masterworks vs Scales/Chords
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF130E24))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (practiceStudioTab == 0) Color(0xFF5C6BC0) else Color.Transparent)
                            .clickable { practiceStudioTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Masterworks Tutorials (${PracticeLibrary.builtInTutorials.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (practiceStudioTab == 1) Color(0xFF5C6BC0) else Color.Transparent)
                            .clickable { practiceStudioTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Scales & Chords (${PracticeLibrary.items.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // If in Masterworks tab: Training Mode selector pill
                if (practiceStudioTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TRAINING FOCUS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF19132E))
                                .padding(2.dp)
                        ) {
                            listOf(
                                "FULL" to "Full Score",
                                "RH_ONLY" to "RH Only",
                                "LH_ONLY" to "LH Only",
                                "SLOW_TEMPO" to "0.7x Tempo"
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
                                        color = if (isSelected) Color.White else Color.White.copy(0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                // List content
                if (practiceStudioTab == 0) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(PracticeLibrary.builtInTutorials) { tutorial ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF19132E),
                                border = BorderStroke(1.dp, Color(0xFF2C2250)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val readBytesFn: (String) -> ByteArray? = { name ->
                                            viewModel.onReadAssetBytes?.invoke(name)
                                        }
                                        val loadedScore = PracticeLibrary.loadTutorialScore(
                                            tutorial = tutorial,
                                            readBytes = readBytesFn,
                                            trainingMode = selectedTrainingMode
                                        )
                                        if (loadedScore != null) {
                                            viewModel.currentScore.value = loadedScore
                                            viewModel.resetScorePlayhead()
                                            // Synchronize hand practice toggle
                                            when (selectedTrainingMode) {
                                                "RH_ONLY" -> viewModel.setHandPracticeMode("RIGHT")
                                                "LH_ONLY" -> viewModel.setHandPracticeMode("LEFT")
                                                else -> viewModel.setHandPracticeMode("BOTH")
                                            }
                                            showLearnModeDialog = false
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tutorial.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("${tutorial.composer} • ${tutorial.era}", color = Color(0xFF81D4FA), fontSize = 11.sp)
                                        }
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(tutorial.difficulty, fontSize = 10.sp, color = Color(0xFFA5D6A7)) }
                                        )
                                    }

                                    Text(tutorial.description, color = Color.White.copy(0.75f), fontSize = 11.sp, lineHeight = 16.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("${tutorial.timeSignature} Time", fontSize = 10.sp, color = Color.White.copy(0.6f))
                                            Text("•", fontSize = 10.sp, color = Color.White.copy(0.4f))
                                            Text("${tutorial.tempo} BPM", fontSize = 10.sp, color = Color(0xFFFFD700))
                                            Text("•", fontSize = 10.sp, color = Color.White.copy(0.4f))
                                            Text("${tutorial.totalMeasures} Bars", fontSize = 10.sp, color = Color.White.copy(0.6f))
                                        }
                                        Button(
                                            onClick = {
                                                val readBytesFn: (String) -> ByteArray? = { name ->
                                                    viewModel.onReadAssetBytes?.invoke(name)
                                                }
                                                val loadedScore = PracticeLibrary.loadTutorialScore(
                                                    tutorial = tutorial,
                                                    readBytes = readBytesFn,
                                                    trainingMode = selectedTrainingMode
                                                )
                                                if (loadedScore != null) {
                                                    viewModel.currentScore.value = loadedScore
                                                    viewModel.resetScorePlayhead()
                                                    when (selectedTrainingMode) {
                                                        "RH_ONLY" -> viewModel.setHandPracticeMode("RIGHT")
                                                        "LH_ONLY" -> viewModel.setHandPracticeMode("LEFT")
                                                        else -> viewModel.setHandPracticeMode("BOTH")
                                                    }
                                                    showLearnModeDialog = false
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Practice", fontSize = 11.sp)
                                        }
                                    }

                                    // Focus pills
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        tutorial.focusAreas.forEach { focus ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF281D45))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(focus, fontSize = 9.sp, color = Color(0xFFCE93D8))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
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
                                        val practiceScore = PracticeLibrary.createScoreForPracticeItem(item)
                                        viewModel.currentScore.value = practiceScore
                                        viewModel.resetScorePlayhead()
                                        showLearnModeDialog = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(item.category, fontSize = 10.sp) }
                                        )
                                    }
                                    Text(item.description, color = Color.White.copy(0.7f), fontSize = 12.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("RH: ${item.fingeringRightHand.joinToString(" ")}", fontSize = 11.sp, color = Color(0xFF81D4FA))
                                        Text("LH: ${item.fingeringLeftHand.joinToString(" ")}", fontSize = 11.sp, color = Color(0xFFCE93D8))
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
        Dialog(
            onDismissRequest = { showAudioToMidiScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF6C63FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                com.creativeblisslabs.peqlearn.app.ai.AudioToMidiScreen(
                    viewModel = viewModel,
                    onScoreGenerated = { score ->
                        viewModel.currentScore.value = score
                        viewModel.scorePlayheadMeasure.value = 0
                        viewModel.scorePlayheadOffset.value = 0f
                        showAudioToMidiScreen = false
                        viewModel.toastMessage.value = "Loaded ${score.title} with ${score.measures.size} measures!"
                    },
                    onBack = { showAudioToMidiScreen = false }
                )
            }
        }
    }
    if (viewModel.isFullscreenKeyboard.value) {
        FullscreenDialog(
            onDismissRequest = { viewModel.isFullscreenKeyboard.value = false },
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
                val currentScore = viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score


               val effectiveTempo = (currentScore?.tempo ?: 120f).coerceAtLeast(20f) * viewModel.playbackSpeed.value.coerceIn(0.25f, 2.5f)

                val totalSeconds = remember(currentScore, effectiveTempo) {
                    if (currentScore == null) 0.0
                    else {
                        currentScore.measures.sumOf { m ->
                            val quarterBeats = m.timeSignatureNumerator.toDouble() * (4.0 / m.timeSignatureDenominator.coerceAtLeast(1).toDouble())
                            (60.0 / effectiveTempo.toDouble()) * quarterBeats
                        }
                    }
                }

                val currentMeasure = viewModel.scorePlayheadMeasure.value
                val currentOffset = viewModel.scorePlayheadOffset.value

                val currentSeconds = remember(currentScore, currentMeasure, currentOffset, effectiveTempo) {
                    if (currentScore == null) 0.0
                    else {
                        var elapsed = 0.0
                        for (i in 0 until currentMeasure) {
                            val m = currentScore.measures.getOrNull(i) ?: break
                            val quarterBeats = m.timeSignatureNumerator.toDouble() * (4.0 / m.timeSignatureDenominator.coerceAtLeast(1).toDouble())
                            elapsed += (60.0 / effectiveTempo.toDouble()) * quarterBeats
                        }
                        val m = currentScore.measures.getOrNull(currentMeasure)
                        if (m != null) {
                            val quarterBeats = m.timeSignatureNumerator.toDouble() * (4.0 / m.timeSignatureDenominator.coerceAtLeast(1).toDouble())
                            elapsed += (currentOffset.toDouble() * (60.0 / effectiveTempo.toDouble()) * quarterBeats)
                        }
                        elapsed
                    }
                }

                val timelineProgress = if (totalSeconds > 0) (currentSeconds / totalSeconds).toFloat() else 0f
                val currentMin = currentSeconds.toInt() / 60
                val currentSec = currentSeconds.toInt() % 60
                val currentTimeStr = "$currentMin:${if (currentSec < 10) "0$currentSec" else currentSec}"

                val totalMin = totalSeconds.toInt() / 60
                val totalSec = totalSeconds.toInt() % 60
                val totalTimeStr = "$totalMin:${if (totalSec < 10) "0$totalSec" else totalSec}"

                val fsScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures(onTap = { areControlsVisible = !areControlsVisible }) }
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


                    // --- TOP BAR: Floats at TopCenter without shifting content ---
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier
                            .zIndex(20f)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    // Left: Song Info with Blue Pulsing Dot & Song Title
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        // Blue glowing indicator dot
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF5C6BC0))
                                        )
                                            val songTitle = currentScore?.title 
                                                ?: viewModel.currentMediaItem.value?.title 
                                                ?: viewModel.selectedMixerItem.value?.displayName 
                                                ?: "No Score Selected"
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    songTitle,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                                // Animated equalizer icon
                                                Icon(
                                                    Icons.Default.GraphicEq,
                                                    contentDescription = null,
                                                    tint = Color(0xFF5C6BC0),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    

                                    // Right: Clean Non-redundant Action Icons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Open / Import Score Button with Dropdown
                                        var showImportMenu by remember { mutableStateOf(false) }
                                        Box {
                                            Button(
                                                onClick = { showImportMenu = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E204A)),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(1.dp, Color(0xFF523B7A)),
                                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.FileOpen,
                                                    contentDescription = "Import Score",
                                                    tint = Color(0xFFC5CAE9),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Import",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFC5CAE9)
                                                )
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = Color(0xFFC5CAE9).copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showImportMenu,
                                                onDismissRequest = { showImportMenu = false },
                                                modifier = Modifier
                                                    .background(Color(0xFF140E26))
                                                    .border(BorderStroke(1.dp, Color(0xFF382956)), RoundedCornerShape(8.dp))
                                            ) {
                                                // 1. Custom File Picker
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(8.dp))
                                                            Column {
                                                                Text("Import Custom File...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                                Text("MIDI (.mid), MusicXML (.xml), PDF Score", fontSize = 10.sp, color = Color.White.copy(0.6f))
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        showImportMenu = false
                                                        viewModel.onPickScoreFileRequested?.invoke()
                                                    }
                                                )

                                                HorizontalDivider(color = Color(0xFF281C3F), modifier = Modifier.padding(vertical = 4.dp))

                                                // 2. Header for Built-in Tutorials
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(13.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("INBUILT MIDI TUTORIALS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81D4FA))
                                                }

                                                // List each built-in tutorial
                                                PracticeLibrary.builtInTutorials.forEach { tutorial ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(tutorial.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                                                    Text("${tutorial.tempo} BPM", fontSize = 10.sp, color = Color(0xFFFFD700))
                                                                }
                                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                    Text(tutorial.composer, fontSize = 10.sp, color = Color(0xFF81D4FA))
                                                                    Text("•", fontSize = 10.sp, color = Color.White.copy(0.4f))
                                                                    Text(tutorial.timeSignature, fontSize = 10.sp, color = Color.White.copy(0.6f))
                                                                    Text("•", fontSize = 10.sp, color = Color.White.copy(0.4f))
                                                                    Text(tutorial.difficulty, fontSize = 10.sp, color = Color(0xFFA5D6A7))
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            showImportMenu = false
                                                            val readBytesFn: (String) -> ByteArray? = { name ->
                                                                viewModel.onReadAssetBytes?.invoke(name)
                                                            }
                                                            val loadedScore = PracticeLibrary.loadTutorialScore(
                                                                tutorial = tutorial,
                                                                readBytes = readBytesFn,
                                                                trainingMode = "FULL"
                                                            )
                                                            if (loadedScore != null) {
                                                                viewModel.currentScore.value = loadedScore
                                                                viewModel.resetScorePlayhead()
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                    
// Helper to cleanly update visualizer layout and notation format
val updateLayout: (String, String, AppViewModel, () -> Unit) -> Unit = { mode, format, vm, closeMenu ->
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
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
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
            val currentFormatLabel = when (viewModel.notationFormat.value.trim().lowercase()) {
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
            text = { Text("Split (Staff + Waterfall)", fontSize = 12.sp) },
            onClick = {
                updateLayout("Split", "staff", viewModel) { menuExpanded = false }
            }
        )
        DropdownMenuItem(
            text = { Text("Split (Tab + Waterfall)", fontSize = 12.sp) },
            onClick = {
                updateLayout("Split", "tab", viewModel) { menuExpanded = false }
            }
        )
        DropdownMenuItem(
            text = { Text("Split (Both + Waterfall)", fontSize = 12.sp) },
            onClick = {
                updateLayout("Split", "both", viewModel) { menuExpanded = false }
            }
        )

        Divider(color = Color(0xFF372754).copy(alpha = 0.5f))

        // 2. NOTATION ONLY WITH SUB-OPTIONS
        DropdownMenuItem(
            text = { Text("Notation Only (Staff)", fontSize = 12.sp) },
            onClick = {
                updateLayout("Notation", "staff", viewModel) { menuExpanded = false }
            }
        )
        DropdownMenuItem(
            text = { Text("Notation Only (Tab)", fontSize = 12.sp) },
            onClick = {
                updateLayout("Notation", "tab", viewModel) { menuExpanded = false }
            }
        )
        DropdownMenuItem(
            text = { Text("Notation Only (Both)", fontSize = 12.sp) },
            onClick = {
                updateLayout("Notation", "both", viewModel) { menuExpanded = false }
            }
        )

        Divider(color = Color(0xFF372754).copy(alpha = 0.5f))

        // 3. STANDARD MODES
        DropdownMenuItem(
            text = { Text("Keyboard + Waterfall", fontSize = 12.sp) },
            onClick = {
                updateLayout("KeyboardWaterfall", "", viewModel) { menuExpanded = false }
            }
        )
        DropdownMenuItem(
            text = { Text("Waterfall Only", fontSize = 12.sp) },
            onClick = {
                updateLayout("Waterfall", "", viewModel) { menuExpanded = false }
            }
        )
        DropdownMenuItem(
            text = { Text("Keyboard Only", fontSize = 12.sp) },
            onClick = {
                updateLayout("Keyboard", "", viewModel) { menuExpanded = false }
            }
        )
    }
}
                                        
                                        // Learn Mode (Scales, Arpeggios & Chords Trainer)
                                        IconButton(
                                            onClick = { showLearnModeDialog = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.School,
                                                contentDescription = "Practice Scales & Chords",
                                                tint = Color(0xFF80CBC4),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Play-Along Evaluation & Live Accuracy
                                        val isPlayAlongActive = viewModel.playAlongEngine.isEvaluating.value
                                        IconButton(
                                            onClick = {
                                                if (!isPlayAlongActive) {
                                                    val score = viewModel.currentScore.value as? Score
                                                    if (score != null) {
                                                        viewModel.playAlongEngine.startEvaluation(score, score.tempo)
                                                        if (!viewModel.isScorePlaying.value) {
                                                            viewModel.startScorePlayback()
                                                        }
                                                    }
                                                }
                                                showPlayAlongStatsDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.GraphicEq,
                                                contentDescription = "Play-Along Accuracy",
                                                tint = if (isPlayAlongActive) Color(0xFF00E676) else Color.White.copy(0.75f),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // AI Audio-to-Score Model (muscriptor-small-lm.onnx)
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

                                        // Instrument / MIDI Tracks Dropdown Selector
                                        Box {
                                            val currentScoreObj = viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score
                                            val foundTracks = currentScoreObj?.tracks ?: emptyList()
                                            val activeTracksCount = if (foundTracks.isNotEmpty()) {
                                                foundTracks.count { tr -> viewModel.enabledTrackIndices[tr.trackIndex] ?: tr.isEnabled }
                                            } else 0
                                            val buttonLabel = if (foundTracks.isNotEmpty()) {
                                                "Tracks ($activeTracksCount/${foundTracks.size})"
                                            } else {
                                                viewModel.selectedInstrument.value
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFF1E162E),
                                                border = BorderStroke(1.dp, Color(0xFF2E2445)),
                                                modifier = Modifier.clickable { fsInstrumentMenuExpanded = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
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
                                                        buttonLabel,
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
                                                expanded = fsInstrumentMenuExpanded,
                                                onDismissRequest = { fsInstrumentMenuExpanded = false }
                                            ) {
                                                if (foundTracks.isNotEmpty()) {
                                                    Text(
                                                        text = "MIDI TRACKS (${foundTracks.size})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD700),
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                    )
                                                    foundTracks.forEach { track ->
                                                        val isEnabled = viewModel.enabledTrackIndices[track.trackIndex] ?: track.isEnabled
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            track.trackName.ifBlank { "Track ${track.trackIndex + 1}" },
                                                                            fontSize = 12.sp,
                                                                            fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                                                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                                                        )
                                                                        Text(
                                                                            "Ch ${track.channel + 1} • ${track.noteCount} notes",
                                                                            fontSize = 10.sp,
                                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                                                        )
                                                                    }
                                                                    Switch(
                                                                        checked = isEnabled,
                                                                        onCheckedChange = { checked ->
                                                                            viewModel.enabledTrackIndices[track.trackIndex] = checked
                                                                        },
                                                                        modifier = Modifier.padding(start = 8.dp)
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                val currentVal = viewModel.enabledTrackIndices[track.trackIndex] ?: track.isEnabled
                                                                viewModel.enabledTrackIndices[track.trackIndex] = !currentVal
                                                            }
                                                        )
                                                    }
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                    Text(
                                                        text = "VIEW MODE INSTRUMENT",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                    )
                                                }

                                                listOf("Piano", "Violin", "Guitar", "Bass", "Flute", "Saxophone").forEach { inst ->
                                                    DropdownMenuItem(
                                                        text = { Text(inst, fontSize = 12.sp) },
                                                        onClick = {
                                                            viewModel.selectedInstrument.value = inst
                                                            if (inst == "Guitar") {
                                                                viewModel.notationFormat.value = "Tab Only"
                                                                notationFormat = "Tab Only"
                                                            } else if (inst == "Piano") {
                                                                viewModel.notationFormat.value = "Staff Only"
                                                                notationFormat = "Staff Only"
                                                            }
                                                            fsInstrumentMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // MIDI Device Button
                                        val isMidiConnected = viewModel.connectedMidiDeviceName.value?.isNotBlank() == true
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isMidiConnected) Color(0xFF1C2B24) else Color(0xFF1E162E),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isMidiConnected) Color(0xFF10B981).copy(0.5f) else Color(0xFF2E2445)
                                            ),
                                            modifier = Modifier.clickable {
                                                viewModel.onScanMidiDevicesRequested?.invoke()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                 Icon(
                                                     Icons.Default.Piano,
                                                     contentDescription = "MIDI Device",
                                                     tint = if (isMidiConnected) Color(0xFF10B981) else Color.White.copy(0.8f),
                                                     modifier = Modifier.size(15.dp)
                                                 )
                                            }
                                        }

                                        // Export Button
                                        Button(
                                            onClick = { showExportDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2045)),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color(0xFF4C3D75)),
                                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
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
                                        // Parser Score .txt Dump button
Button(
    onClick = { showDumpDialog = true },
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
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
    Text("TXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                        onClick = { areControlsVisible = !areControlsVisible },
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
                    }
                    
                        // Fullscreen Interactive Piano Area with Synchronized Waterfall & Keyboard (Floats behind top & bottom overlay controls)
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = fsZoomFactor, scaleY = fsZoomFactor, translationX = panOffset.x, translationY = panOffset.y)
                                .background(Color.Black)
                        ) {
                            val availableW = maxWidth
                            val isNotationOnly = fsLayoutMode == "Notation"
                            val isLandscapeWide = availableW >= 600.dp
                            // Piano & waterfall width: in landscape wide or notation mode it fits availableW; in portrait split/waterfall/keyboard it expands for 88 playable keys
                            val keyboardWidth = if (isLandscapeWide) availableW else maxOf(availableW, 30.dp * whiteKeyCount)

                            val activeMidiNoteSet by remember {
                                derivedStateOf {
                                    viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key }.toSet()
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
                                            onFormatChange = { newFormat -> notationFormat = newFormat },
                                            showLetters = viewModel.showNotationNoteNames.value,
                                            showLyrics = fsShowLyrics,
                                            isSplitStaff = viewModel.isSplitStaff.value,
                                            isVerticalScroll = viewModel.isVerticalScroll.value,
                                            leftHandColor = viewModel.leftHandColor.value,
                                            rightHandColor = viewModel.rightHandColor.value,
                                            isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                            isRightHandHidden = viewModel.isRightHandHidden.value,
                                            enabledTrackIndices = viewModel.enabledTrackIndices,
                                            showScoreHeader = false,
                                            compactStaffMode = true,
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
                                        Modifier.fillMaxWidth().weight(0.48f).horizontalScroll(fsScrollState)
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
                                                    }
                                                )
                                            }

                                            // Bottom Instrument Visualizer: UnifiedPianoKeyboard or UnifiedGuitarFretboard
                                            val isGuitarFamily = viewModel.selectedInstrument.value == "Guitar" || viewModel.selectedInstrument.value == "Ukulele"
                                            val keyboardModifier = Modifier
                                                .fillMaxWidth()
                                                .height(viewModel.keyboardHeightFactor.floatValue.dp)
                                                .background(Color(0xFF161616))

                                            Box(modifier = keyboardModifier) {
                                                if (isGuitarFamily) {
                                                    val isUkulele = viewModel.selectedInstrument.value == "Ukulele"
                                                    val activeList = remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                    UnifiedGuitarFretboard(
                                                        numFrets = if (isUkulele) 15 else 22,
                                                        activeNotes = activeList,
                                                        activeMidiNote = viewModel.midiPlayingNote.value,
                                                        isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                        isRightHandHidden = viewModel.isRightHandHidden.value,
                                                        accentColor = primaryColor,
                                                        showNoteNames = fsShowLetters,
                                                        stringLabels = if (isUkulele) listOf("A", "E", "C", "G") else listOf("e", "B", "G", "D", "A", "E"),
                                                        openStringsSemis = if (isUkulele) listOf(9, 4, 0, 7) else listOf(4, 11, 7, 2, 9, 4),
                                                        onFretTapped = { _, _, midi ->
                                                            if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(midi)
                                                            viewModel.onMidiNoteOn(midi, 95)
                                                        },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    UnifiedPianoKeyboard(
                                                        activeMidiNotes = viewModel.midiActiveNotes,
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
                                                        customKeyColors = viewModel.playAlongEngine.liveKeyColors,
                                                        onKeyTapped = { midi ->
                                                            if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(midi)
                                                            viewModel.onMidiNoteOn(midi, 95)
                                                        },
                                                        onKeyReleased = { midi ->
                                                            fsManualPressedKeys.remove(midi)
                                                            viewModel.onMidiNoteOff(midi)
                                                        },
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
                                                                            viewModel.keyboardClothColor.value.copy(alpha = 0.85f),
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
                                val finalContentWidth = if (isNotationOnly || isLandscapeWide) availableW else keyboardWidth

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
                                                        onFormatChange = { newFormat -> notationFormat = newFormat },
                                                        showLetters = viewModel.showNotationNoteNames.value,
                                                        showLyrics = fsShowLyrics,
                                                        isSplitStaff = viewModel.isSplitStaff.value,
                                                        isVerticalScroll = viewModel.isVerticalScroll.value,
                                                        leftHandColor = viewModel.leftHandColor.value,
                                                        rightHandColor = viewModel.rightHandColor.value,
                                                        isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                        isRightHandHidden = viewModel.isRightHandHidden.value,
                                                        enabledTrackIndices = viewModel.enabledTrackIndices,
                                                        showScoreHeader = false,
                                                        compactStaffMode = true,
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
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Bottom Instrument Visualizer for Keyboard / Waterfall+Keyboard modes
                                        if (fsLayoutMode != "Notation" && fsLayoutMode != "Waterfall") {
                                            val isGuitarFamily = viewModel.selectedInstrument.value == "Guitar" || viewModel.selectedInstrument.value == "Ukulele"
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
                                                    val isUkulele = viewModel.selectedInstrument.value == "Ukulele"
                                                    val activeList = remember(viewModel.midiActiveNotes.keys) {
                                                        viewModel.midiActiveNotes.keys.toList()
                                                    }
                                                    UnifiedGuitarFretboard(
                                                        numFrets = if (isUkulele) 15 else 22,
                                                        activeNotes = activeList,
                                                        activeMidiNote = viewModel.midiPlayingNote.value,
                                                        isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                                        isRightHandHidden = viewModel.isRightHandHidden.value,
                                                        accentColor = primaryColor,
                                                        showNoteNames = fsShowLetters,
                                                        stringLabels = if (isUkulele) listOf("A", "E", "C", "G") else listOf("e", "B", "G", "D", "A", "E"),
                                                        openStringsSemis = if (isUkulele) listOf(9, 4, 0, 7) else listOf(4, 11, 7, 2, 9, 4),
                                                        onFretTapped = { _, _, midi ->
                                                            if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(midi)
                                                            viewModel.onMidiNoteOn(midi, 95)
                                                        },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    UnifiedPianoKeyboard(
                                                        activeMidiNotes = viewModel.midiActiveNotes,
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
                                                        customKeyColors = viewModel.playAlongEngine.liveKeyColors,
                                                        onKeyTapped = { midi ->
                                                            if (midi !in fsManualPressedKeys) fsManualPressedKeys.add(midi)
                                                            viewModel.onMidiNoteOn(midi, 95)
                                                        },
                                                        onKeyReleased = { midi ->
                                                            fsManualPressedKeys.remove(midi)
                                                            viewModel.onMidiNoteOff(midi)
                                                        },
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
                                                                            viewModel.keyboardClothColor.value.copy(alpha = 0.85f),
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

                            // 3-Pedal Grand Piano Unit cleanly embedded in right corner
                            if (viewModel.showPedals.value) {
                                val pedalBottomPadding = if (fsLayoutMode == "Notation") 20.dp else 152.dp
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
                        }

                    // --- FLOATING BOTTOM PLAYBACK BAR: Styled exactly like reference image 1 ---
                    val controlsBottomPadding = if (fsLayoutMode == "Notation") 18.dp else 155.dp
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier
                            .zIndex(20f)
                            .align(Alignment.BottomCenter)
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
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Drag / Grip Handle
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Grip",
                                    tint = Color.White.copy(0.35f),
                                    modifier = Modifier.size(14.dp)
                                )

                                // 2. Metronome / Notification Bell
                                IconButton(
                                    onClick = {
                                        viewModel.metronomeEnabled.value = !viewModel.metronomeEnabled.value
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = "Metronome",
                                        tint = if (viewModel.metronomeEnabled.value) Color(0xFF7986CB) else Color.White.copy(0.45f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // 3. Previous Track / Reset
                                IconButton(
                                    onClick = { viewModel.resetScorePlayhead() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.White.copy(0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // 4. Circular Glowing Play / Pause Button
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF5C6BC0),
                                    modifier = Modifier.size(38.dp)
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

                                // 5. Next Track
                                IconButton(
                                    onClick = { /* Next marker */ },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.White.copy(0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // 6. Current Time Elapsed
                                Text(
                                    currentTimeStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(0.65f)
                                )

                                // 7. Blue Scrubbing Progress Slider
                                Slider(
                                    value = timelineProgress,
                                   onValueChange = { targetProgress ->
                                        if (currentScore != null && totalSeconds > 0) {
                                            val targetTotalSec = targetProgress * totalSeconds.toFloat()
                                            var accumulated = 0f
                                            var targetM = 0
                                            var targetO = 0f
                                            for (i in currentScore.measures.indices) {
                                                val m = currentScore.measures[i]
                                                val quarterBeats = m.timeSignatureNumerator.toFloat() * (4f / m.timeSignatureDenominator.coerceAtLeast(1).toFloat())
                                                val mDur = (60f / effectiveTempo) * quarterBeats
                                                if (targetTotalSec < accumulated + mDur) {
                                                    targetM = i
                                                    targetO = ((targetTotalSec - accumulated) / mDur).coerceIn(0f, 1f)
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
                                    modifier = Modifier.width(130.dp),
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
                                    color = Color.White.copy(0.65f)
                                )

                                Spacer(Modifier.width(2.dp))

                                // 9. Volume Control Slider
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = Color.White.copy(0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Slider(
                                    value = viewModel.masterVolume.value,
                                    onValueChange = { viewModel.masterVolume.value = it },
                                    modifier = Modifier.width(75.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF5C6BC0),
                                        inactiveTrackColor = Color(0xFF231838)
                                    )
                                )

                                // 10. Speed Pill (e.g., 1x, 0.75x, 1.25x)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E162E),
                                    border = BorderStroke(1.dp, Color(0xFF2E2445)),
                                    modifier = Modifier.clickable {
                                        viewModel.playbackSpeed.value = when (viewModel.playbackSpeed.value) {
                                            0.5f -> 0.75f
                                            0.75f -> 1.0f
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            else -> 0.5f
                                        }
                                    }
                                ) {
                                    Text(
                                        "${viewModel.playbackSpeed.value}x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(0.9f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // 11. Transpose Pill with Minus / Number / Plus
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E162E))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.transposition.intValue = (viewModel.transposition.intValue - 1).coerceAtLeast(-12) },
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
                                        "${viewModel.transposition.intValue}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .clickable { viewModel.transposition.intValue = 0 }
                                            .padding(horizontal = 3.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.transposition.intValue = (viewModel.transposition.intValue + 1).coerceAtMost(12) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Transpose Up",
                                            tint = Color.White.copy(0.8f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                // 12. Hand Practice Mode Toggle (Both / Right / Left)
                                val currentHandMode = viewModel.activeHandMode.value
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E162E))
                                        .border(BorderStroke(1.dp, Color(0xFF372754)), RoundedCornerShape(12.dp))
                                        .padding(2.dp)
                                ) {
                                    // Both Hands
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentHandMode == "BOTH") Color(0xFF5C6BC0) else Color.Transparent)
                                            .clickable { viewModel.setHandPracticeMode("BOTH") }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "Both",
                                            fontSize = 10.sp,
                                            fontWeight = if (currentHandMode == "BOTH") FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentHandMode == "BOTH") Color.White else Color.White.copy(0.6f)
                                        )
                                    }

                                    // Right Hand Only
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentHandMode == "RIGHT") Color(0xFF4A90E2) else Color.Transparent)
                                            .clickable { viewModel.setHandPracticeMode("RIGHT") }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "Right (RH)",
                                            fontSize = 10.sp,
                                            fontWeight = if (currentHandMode == "RIGHT") FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentHandMode == "RIGHT") Color.White else Color.White.copy(0.6f)
                                        )
                                    }

                                    // Left Hand Only
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentHandMode == "LEFT") Color(0xFF8E24AA) else Color.Transparent)
                                            .clickable { viewModel.setHandPracticeMode("LEFT") }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "Left (LH)",
                                            fontSize = 10.sp,
                                            fontWeight = if (currentHandMode == "LEFT") FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentHandMode == "LEFT") Color.White else Color.White.copy(0.6f)
                                        )
                                    }
                                }

                                // 13. Close / Dismiss Pill Button
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E162E),
                                    modifier = Modifier.size(26.dp)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Apply Recommended Speed", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showSpeedOptimizationDialog = false }
                                ) {
                                    Text("Keep Current", color = Color.White.copy(0.6f), fontSize = 12.sp)
                                }
                            },
                            containerColor = Color(0xFF1E162E),
                            tonalElevation = 6.dp
                        )
                    }
                }
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
                        IconButton(onClick = { showExportDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Tab selector between Video Export and PDF Score Export
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
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
                        VideoExportStudioSection(viewModel = viewModel)
                    } else {
                        PdfExportStudioSection(viewModel = viewModel)
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

// @Composable
// fun LiveChordHud(
//     detectedChord: com.creativeblisslabs.peqlearn.app.util.LiveChordDetector.DetectedChord?,
//     primaryColor: Color,
// ) {
//     Row(
//         modifier = modifier
//             .fillMaxWidth()
//             .padding(horizontal = 2.dp, vertical = 2.dp),
//         verticalAlignment = Alignment.CenterVertically,
//         horizontalArrangement = Arrangement.SpaceBetween
//     ) {
//         if (detectedChord != null) {
//             Surface(
//                 shape = RoundedCornerShape(8.dp),
//                 color = Color(0xFF14171E).copy(alpha = 0.95f),
//                 border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
//                 shadowElevation = 4.dp
//             ) {
//                 Row(
//                     modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
//                     verticalAlignment = Alignment.CenterVertically,
//                     horizontalArrangement = Arrangement.spacedBy(6.dp)
//                 ) {
//                     Box(
//                         modifier = Modifier
//                             .size(8.dp)
//                             .clip(RoundedCornerShape(4.dp))
//                             .background(Color(0xFF00E676))
//                     )
//                     Text(
//                         text = detectedChord.fullName,
//                         fontSize = 13.sp,
//                         fontWeight = FontWeight.ExtraBold,
//                         color = Color(0xFFFFD700)
//                     )
//                     // Optional inversion text can be re-enabled here if needed:
//                     // Text(
//                     //     text = "• ${detectedChord.inversionText}",
//                     //     fontSize = 11.sp,
//                     //     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                     // )
//                 }
//             }
//         } else {
//             Spacer(Modifier.width(1.dp))
//         }
//     }
// }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsSheet(
    viewModel: AppViewModel,
    notationFormat: String = viewModel.notationFormat.value,
    onFormatChange: (String) -> Unit = { viewModel.notationFormat.value = it },
    showLetters: Boolean = viewModel.showPianoKeyLabels.value,
    onShowLettersChange: (Boolean) -> Unit = { viewModel.showPianoKeyLabels.value = it },
    showNotationNoteNames: Boolean = viewModel.showNotationNoteNames.value,
    onShowNotationNoteNamesChange: (Boolean) -> Unit = { viewModel.showNotationNoteNames.value = it },
    showLyrics: Boolean = true,
    onShowLyricsChange: (Boolean) -> Unit = {},
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by rememberSaveable { mutableIntStateOf(initialTab) }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.persistSession()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFFFD700).copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Studio & Workspace Settings",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        viewModel.persistSession()
                        onDismiss()
                    }
                ) {
                    Text(
                        "DONE",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Tab Navigation Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Notation Viewer", "Keyboard & Visualizer")
                tabs.forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFD700) else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (activeTab == 0) {
                // =========================================================================
                // TAB 0: NOTATION VIEWER SETTINGS
                // =========================================================================
                SettingsSection(title = "Hands & Notation Visibility") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Right Hand Notes (Treble)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Show/hide right hand across Staff, Tab, Keyboard & Waterfall", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = !viewModel.isRightHandHidden.value,
                                onCheckedChange = { viewModel.isRightHandHidden.value = !it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Left Hand Notes (Bass)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Show/hide left hand across Staff, Tab, Keyboard & Waterfall", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = !viewModel.isLeftHandHidden.value,
                                onCheckedChange = { viewModel.isLeftHandHidden.value = !it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }
                    }
                }

                SettingsSection(title = "Notation Display Format") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Format Mode", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            val formats = listOf("Staff", "Tab", "Both")
                            Row(modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(8.dp)).padding(3.dp)) {
                                formats.forEach { f ->
                                    val currentFmt = viewModel.notationFormat.value.trim().lowercase()
                                    val active = when (f) {
                                        "Staff" -> currentFmt == "staff" || currentFmt == "staff only"
                                        "Tab" -> currentFmt == "tab" || currentFmt == "tab only"
                                        "Both" -> currentFmt == "both" || currentFmt == "both viewers"
                                        else -> false
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) Color(0xFFFFD700) else Color.Transparent)
                                            .clickable {
                                                val target = when (f) {
                                                    "Both" -> "Both Viewers"
                                                    else -> "$f Only"
                                                }
                                                viewModel.notationFormat.value = target
                                                onFormatChange(target)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            f,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.Black else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notehead Note Names", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.showNotationNoteNames.value,
                                onCheckedChange = {
                                    viewModel.showNotationNoteNames.value = it
                                    onShowNotationNoteNamesChange(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Split Staff (Grand Staff)", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.isSplitStaff.value,
                                onCheckedChange = { viewModel.isSplitStaff.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vertical Scrolling", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.isVerticalScroll.value,
                                onCheckedChange = {
                                    viewModel.isVerticalScroll.value = it
                                    viewModel.scrollDirectionHorizontal.value = !it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("OpenSheetMusicDisplay (Web Engine)", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.useOsmdRenderer.value,
                                onCheckedChange = { viewModel.useOsmdRenderer.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }
                    }
                }

                SettingsSection(title = "Key Signature & Transposition") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Active Key Signature", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                        val keys = listOf(
                            "C Major", "G Major", "D Major", "A Major", "E Major", "B Major", "F# Major",
                            "F Major", "B♭ Major", "E♭ Major", "A♭ Major", "D♭ Major", "A Minor", "E Minor", "D Minor"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            keys.forEach { key ->
                                val selected = viewModel.selectedKeySignature.value == key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.06f))
                                        .clickable {
                                            viewModel.selectedKeySignature.value = key
                                            // Map key name to KeySignature enum and update score
                                            val keySigEnum = when (key) {
                                                "G Major", "E Minor" -> KeySignature.G_MAJOR
                                                "D Major", "B Minor" -> KeySignature.D_MAJOR
                                                "A Major", "F# Minor" -> KeySignature.A_MAJOR
                                                "E Major", "C# Minor" -> KeySignature.E_MAJOR
                                                "B Major", "G# Minor" -> KeySignature.B_MAJOR
                                                "F# Major" -> KeySignature.F_SHARP_MAJOR
                                                "F Major", "D Minor" -> KeySignature.F_MAJOR
                                                "B♭ Major", "G Minor" -> KeySignature.B_FLAT_MAJOR
                                                "E♭ Major", "C Minor" -> KeySignature.E_FLAT_MAJOR
                                                "A♭ Major", "F Minor" -> KeySignature.A_FLAT_MAJOR
                                                "D♭ Major", "Bb Minor" -> KeySignature.D_FLAT_MAJOR
                                                else -> KeySignature.C_MAJOR
                                            }
                                            val current = viewModel.currentScore.value as? Score
                                            if (current != null) {
                                                viewModel.currentScore.value = current.copy(
                                                    keySignature = keySigEnum,
                                                    measures = current.measures.map { it.copy(keySignature = keySigEnum) }
                                                )
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        key,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StepperControl(
                                label = "Transposition",
                                value = viewModel.transposition.value,
                                onValueChange = { viewModel.transposition.value = it }
                            )
                            StepperControl(
                                label = "Octave Shift",
                                value = viewModel.octaveShift.value,
                                onValueChange = { viewModel.octaveShift.value = it }
                            )
                        }
                    }
                }

                SettingsSection(title = "Time Signature & Quantization") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Time Signature", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                        val timeSigs = listOf("4/4", "3/4", "2/4", "6/8", "12/8", "5/4", "7/8")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            timeSigs.forEach { ts ->
                                val selected = viewModel.selectedTimeSignature.value == ts
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.06f))
                                        .clickable { viewModel.selectedTimeSignature.value = ts }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        ts,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text("Quantize Grid", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                        val quantizeOptions = listOf("1/4", "1/8", "1/16", "1/32")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            quantizeOptions.forEach { q ->
                                val selected = viewModel.quantizeDivision.value == q
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.06f))
                                        .clickable { viewModel.quantizeDivision.value = q }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        q,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text("Target Notation Instrument", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                        val instruments = listOf("Piano", "Guitar", "Bass", "Drum kit", "Ukulele")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            instruments.forEach { inst ->
                                val selected = viewModel.selectedInstrument.value == inst
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.06f))
                                        .clickable {
                                            viewModel.selectedInstrument.value = inst
                                            // Piano88 Screen Rule: Guitar shows Tab, Piano shows Staff notation
                                            if (inst == "Guitar" || inst == "Ukulele") {
                                                viewModel.notationFormat.value = "Tab Only"
                                                onFormatChange("Tab Only")
                                            } else if (inst == "Piano") {
                                                viewModel.notationFormat.value = "Staff Only"
                                                onFormatChange("Staff Only")
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        inst,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // TAB 1: KEYBOARD & VISUALIZER STUDIO SETTINGS (MIDIVisualizer-master Parity)
                // =========================================================================
                // Visualizer Mode Selector
                SettingsSection(title = "VISUALIZER DISPLAY MODE") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Choose what visualizer to show on screen",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            fontSize = 12.sp
                        )
                        val visualizers = listOf(
                            "Split (Notation + Waterfall)" to "Split",
                            "Keyboard + Waterfall" to "KeyboardWaterfall",
                            "Waterfall Only" to "Waterfall",
                            "Notation Only" to "Notation",
                            "Keyboard Only" to "Keyboard"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            visualizers.forEach { (label, modeKey) ->
                                val isSelected = viewModel.visualizerLayoutMode.value == modeKey
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFFFD700).copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFFFD700) else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.visualizerLayoutMode.value = modeKey
                                            when (modeKey) {
                                                "Waterfall" -> {
                                                    viewModel.showWaterfall.value = true
                                                }
                                                "Notation" -> {
                                                    viewModel.showWaterfall.value = false
                                                }
                                                "Keyboard" -> {
                                                    viewModel.showWaterfall.value = false
                                                }
                                                "Split", "KeyboardWaterfall" -> {
                                                    viewModel.showWaterfall.value = true
                                                }
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. NOTE STYLE & COLORS (Viewer::showNoteOptions)
                SettingsSection(title = "NOTE STYLE & APPEARANCE") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Falling Notes", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.showWaterfall.value,
                                onCheckedChange = { viewModel.showWaterfall.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Falling Note Stream / Visualizer Height: ${viewModel.keyboardHeightFactor.floatValue.toInt()}dp", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                            Slider(
                                value = viewModel.keyboardHeightFactor.floatValue,
                                onValueChange = { viewModel.keyboardHeightFactor.floatValue = it },
                                valueRange = 100f..260f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Vertical Scale (Speed)", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Slow", "Normal", "Fast", "Extreme").forEach { spd ->
                                    val active = viewModel.fallingNotesSpeed.value == spd
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.08f))
                                            .clickable { viewModel.fallingNotesSpeed.value = spd }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = spd,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            color = if (active) Color.Black else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Corner Roundness: ${viewModel.noteCornerRadius.floatValue.toInt()}px", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                            Slider(
                                value = viewModel.noteCornerRadius.floatValue,
                                onValueChange = { viewModel.noteCornerRadius.floatValue = it },
                                valueRange = 0f..14f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Edge Thickness: ${(viewModel.noteEdgeThickness.floatValue * 10).toInt() / 10.0}px", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                            Slider(
                                value = viewModel.noteEdgeThickness.floatValue,
                                onValueChange = { viewModel.noteEdgeThickness.floatValue = it },
                                valueRange = 0.5f..5.0f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Style Note Heads as Ovals", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.noteStyleOval.value,
                                onCheckedChange = { viewModel.noteStyleOval.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }
                    }
                }

                // 2. PARTICLES & SPARKS (Viewer::showParticleOptions)
                SettingsSection(title = "PARTICLES & SPARKS") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Particles", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.showWaterfallParticles.value,
                                onCheckedChange = { viewModel.showWaterfallParticles.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        if (viewModel.showWaterfallParticles.value) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Intensity / Count: ${viewModel.particleCount.intValue} sparks", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                                Slider(
                                    value = viewModel.particleCount.intValue.toFloat(),
                                    onValueChange = { viewModel.particleCount.intValue = it.toInt() },
                                    valueRange = 4f..48f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Particle Speed: ${(viewModel.particleSpeed.floatValue * 10).toInt() / 10.0}x", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                                Slider(
                                    value = viewModel.particleSpeed.floatValue,
                                    onValueChange = { viewModel.particleSpeed.floatValue = it },
                                    valueRange = 1.0f..10.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Particle Size: ${(viewModel.particleScale.floatValue * 10).toInt() / 10.0}x", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                                Slider(
                                    value = viewModel.particleScale.floatValue,
                                    onValueChange = { viewModel.particleScale.floatValue = it },
                                    valueRange = 0.4f..2.5f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Spread / Expansion: ${(viewModel.particleExpansion.floatValue * 10).toInt() / 10.0}x", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                                Slider(
                                    value = viewModel.particleExpansion.floatValue,
                                    onValueChange = { viewModel.particleExpansion.floatValue = it },
                                    valueRange = 0.5f..3.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sync Color With Notes", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = viewModel.lockParticleColor.value,
                                    onCheckedChange = { viewModel.lockParticleColor.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                                )
                            }
                        }
                    }
                }

                // 3. KEY FLASHES & BLOOM (Viewer::showFlashOptions)
                SettingsSection(title = "KEY FLASHES & BLOOM") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Flash Size: ${(viewModel.flashSize.floatValue * 10).toInt() / 10.0}x", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                            Slider(
                                value = viewModel.flashSize.floatValue,
                                onValueChange = { viewModel.flashSize.floatValue = it },
                                valueRange = 0.4f..2.2f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Halo Intensity: ${(viewModel.flashHaloIntensity.floatValue * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                            Slider(
                                value = viewModel.flashHaloIntensity.floatValue,
                                onValueChange = { viewModel.flashHaloIntensity.floatValue = it },
                                valueRange = 0.2f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                            )
                        }
                    }
                }

                // 4. PIANO INTERFACE & STUDIO EFFECTS (Viewer::showKeyboardOptions)
                SettingsSection(title = "PIANO INTERFACE & STUDIO EFFECTS") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Highlight Active Keys", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.keyboardHighlightActive.value,
                                onCheckedChange = { viewModel.keyboardHighlightActive.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Note Names on Keys", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = showLetters,
                                onCheckedChange = onShowLettersChange,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Chord Overlay", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.keyboardShowChords.value,
                                onCheckedChange = { viewModel.keyboardShowChords.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Black Key Edges", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.keyboardMinorEdges.value,
                                onCheckedChange = { viewModel.keyboardMinorEdges.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Velvet Cloth (Crimson Felt Strip)", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.keyboardShowVelvetCloth.value,
                                onCheckedChange = { viewModel.keyboardShowVelvetCloth.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Side Frames (Mahogany Cheeks)", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.keyboardShowSideFrames.value,
                                onCheckedChange = { viewModel.keyboardShowSideFrames.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }
                    }
                }

                // 5. PEDALS (Viewer::showPedalOptions)
                SettingsSection(title = "PEDALS & FOOT CONTROLS") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show 3-Pedal Grand Unit", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.showPedals.value,
                                onCheckedChange = { viewModel.showPedals.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        if (viewModel.showPedals.value) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Pedal Scaling: ${(viewModel.pedalScaling.floatValue * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                                Slider(
                                    value = viewModel.pedalScaling.floatValue,
                                    onValueChange = { viewModel.pedalScaling.floatValue = it },
                                    valueRange = 0.5f..1.5f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Pedal Opacity: ${(viewModel.pedalOpacity.floatValue * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 13.sp)
                                Slider(
                                    value = viewModel.pedalOpacity.floatValue,
                                    onValueChange = { viewModel.pedalOpacity.floatValue = it },
                                    valueRange = 0.2f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                                )
                            }
                        }
                    }
                }

                // Theme Presets
                SettingsSection(title = "Visualizer Theme Presets") {
                    val presets = listOf(
                        Triple("Synthesia", Color(0xFF00E5FF), Color(0xFFFF4081)),
                        Triple("Cyberpunk", Color(0xFFD500F9), Color(0xFF00E676)),
                        Triple("Romantic", Color(0xFFFFD700), Color(0xFFFF6F00)),
                        Triple("Minimal", Color(0xFFFFFFFF), Color(0xFF90A4AE)),
                        Triple("Chroma Key", Color(0xFF00FF00), Color(0xFF00FF00))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (name, rhColor, lhColor) ->
                            val isSelected = viewModel.rightHandColor.value == rhColor && viewModel.leftHandColor.value == lhColor
                            val primaryCol = MaterialTheme.colorScheme.primary
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.rightHandColor.value = rhColor
                                        viewModel.leftHandColor.value = lhColor
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) primaryCol.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(0.04f),
                                border = BorderStroke(1.dp, if (isSelected) primaryCol else Color.Transparent)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(rhColor))
                                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(lhColor))
                                    }
                                    Text(
                                        text = name,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) primaryCol else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Hands & Muting
                SettingsSection(title = "Hands & Track Muting") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HandControlRow(
                            label = "Right Hand (Treble)",
                            isMuted = viewModel.isRightHandMuted.value,
                            isHidden = viewModel.isRightHandHidden.value,
                            color = viewModel.rightHandColor.value,
                            onMuteToggle = { viewModel.isRightHandMuted.value = it },
                            onHideToggle = { viewModel.isRightHandHidden.value = it },
                            onColorChange = { viewModel.rightHandColor.value = it }
                        )
                        HandControlRow(
                            label = "Left Hand (Bass)",
                            isMuted = viewModel.isLeftHandMuted.value,
                            isHidden = viewModel.isLeftHandHidden.value,
                            color = viewModel.leftHandColor.value,
                            onMuteToggle = { viewModel.isLeftHandMuted.value = it },
                            onHideToggle = { viewModel.isLeftHandHidden.value = it },
                            onColorChange = { viewModel.leftHandColor.value = it }
                        )
                    }
                }

                // Speed & Loop
                SettingsSection(title = "Speed & Loop Controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val speed = viewModel.playbackSpeed.value
                            val formattedSpeed = (kotlin.math.round(speed * 10) / 10.0).toString()
                            Text(
                                "Tempo Scale: ${formattedSpeed}x",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Slider(
                                value = viewModel.playbackSpeed.value,
                                onValueChange = { viewModel.playbackSpeed.value = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.width(140.dp),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Metronome Click", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.metronomeEnabled.value,
                                onCheckedChange = { viewModel.metronomeEnabled.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("A-B Loop Active", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.abRepeatActive.value,
                                onCheckedChange = { viewModel.abRepeatActive.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }
                    }
                }

                // Current Queue
                if (viewModel.playbackQueue.isNotEmpty()) {
                    SettingsSection(title = "Current Queue") {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            viewModel.playbackQueue.forEach { item ->
                                val isActive = viewModel.currentMediaItem.value == item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playMediaItem(item) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = if (isActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        item.title,
                                        color = if (isActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = Color(0xFFFFD700).copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun HandControlRow(
    label: String,
    isMuted: Boolean,
    isHidden: Boolean,
    color: Color,
    onMuteToggle: (Boolean) -> Unit,
    onHideToggle: (Boolean) -> Unit,
    onColorChange: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(0.03f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Color(0xFF6C63FF), Color(0xFF4A90E2), Color(0xFFFF6B35), Color(0xFFFFD700), Color(0xFF4CAF50)).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(c)
                            .border(if (color == c) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp))
                            .clickable { onColorChange(c) }
                    )
                }
            }
        }

        IconButton(onClick = { onMuteToggle(!isMuted) }) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = null,
                tint = if (isMuted) Color.Red.copy(0.7f) else MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
        }

        IconButton(onClick = { onHideToggle(!isHidden) }) {
            Icon(
                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(0.2f) else Color(0xFFFFD700)
            )
        }
    }
}

@Composable
private fun StepperControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.onSurface.copy(0.08f), RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            }
            Text(
                text = if (value > 0) "+$value" else value.toString(),
                color = if (value != 0) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onValueChange(value + 1) }) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun isMidiNoteBlack(midiNote: Int): Boolean {
    val semitone = midiNote % 12
    val normalized = if (semitone < 0) semitone + 12 else semitone
    return normalized == 1 || normalized == 3 || normalized == 6 || normalized == 8 || normalized == 10
}

fun getWhiteKeyIndexForMidi(midiNote: Int): Float {
    val whiteNotes = (21..108).filter { !isMidiNoteBlack(it) }
    if (!isMidiNoteBlack(midiNote)) {
        val idx = whiteNotes.indexOf(midiNote)
        return if (idx >= 0) idx.toFloat() else 0f
    } else {
        val prevWhite = midiNote - 1
        val nextWhite = midiNote + 1
        val idx1 = whiteNotes.indexOf(prevWhite)
        val idx2 = whiteNotes.indexOf(nextWhite)
        return if (idx1 >= 0 && idx2 >= 0) (idx1 + idx2) / 2f else 0f
    }
}

fun getNoteNameFromMidi(midiNote: Int): String {
    val names = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    val octave = (midiNote / 12) - 1
    val noteName = names[midiNote % 12]
    return "$noteName$octave"
}

fun getPitchOnlyFromMidi(midiNote: Int): String {
    val names = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    return names[midiNote % 12]
}

/**
 * Phase 2 High-Performance Waterfall Precomputations:
 * 1. Static Geometry Cache per MIDI key (21..108) calculated once per canvas width change.
 * 2. Flat temporal timeline of notes with absolute beat offsets allowing O(log N) temporal windowing.
 */
data class MidiKeyGeometry(
    val midiNote: Int,
    val isBlack: Boolean,
    val whiteKeyIndex: Float,
    val pitchName: String,
    val baseWidthFraction: Float // Fraction of whiteKeyWidth
)

object WaterfallKeyGeometryLookup {
    val keyGeometries: Array<MidiKeyGeometry> = Array(128) { midi ->
        val isBlack = isMidiNoteBlack(midi)
        val wIdx = getWhiteKeyIndexForMidi(midi)
        val pitch = getPitchOnlyFromMidi(midi)
        val widthFrac = if (isBlack) 0.58f else 0.82f
        MidiKeyGeometry(
            midiNote = midi,
            isBlack = isBlack,
            whiteKeyIndex = wIdx,
            pitchName = pitch,
            baseWidthFraction = widthFrac
        )
    }
}
data class FlatWaterfallNote(
    val midiNote: Int,
    val absoluteBeatStart: Float,
    val durationBeats: Float,
    val isRightHand: Boolean,
    val voice: Int,
    val isChordNote: Boolean
)

data class WaterfallTimeline(
    val notes: List<FlatWaterfallNote>,
    val measureCumulativeBeats: FloatArray
) {
    fun findStartWindowIndex(targetBeat: Float): Int {
        var low = 0
        var high = notes.lastIndex
        var result = notes.size
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (notes[mid].absoluteBeatStart + notes[mid].durationBeats >= targetBeat) {
                result = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return result
    }
}
@Composable
fun SynthesiaWaterfallOverlay(
    activeMidiNotes: Set<Int>,
    manualPressedNotes: Set<Int> = emptySet(),
    score: com.creativeblisslabs.peqlearn.app.ui.screens.Score? = null,
    playheadMeasure: Int = 0,
    playheadOffset: Float = 0f,
    isPlaying: Boolean = false,
    fallingSpeedSetting: String = "Normal",
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    glowEffect: Boolean = true,
    particlesEnabled: Boolean = true,
    particleCount: Int = 16,
    particleSpeed: Float = 4.5f,
    particleScale: Float = 1.0f,
    particleExpansion: Float = 1.0f,
    lockParticleColor: Boolean = true,
    particleCustomColor: Color = Color(0xFFFFD700),
    flashSize: Float = 1.0f,
    flashHaloIntensity: Float = 0.65f,
    noteCornerRadius: Float = 6.0f,
    noteEdgeThickness: Float = 1.2f,
    noteStyleOval: Boolean = false,
    leftHandColor: Color = Color(0xFF6C63FF),
    rightHandColor: Color = Color(0xFF4A90E2),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
    playbackSpeed: Float = 1.0f,
    onFpsDropDetected: () -> Unit = {}
) {
    val fallingBlocks = remember { ArrayList<PianoWaterfallBlock>() }
    val particles = remember { ArrayList<PianoSpark>() }
    val flashes = remember { ArrayList<PianoKeyFlash>() }
    var canvasWidthPx by remember { mutableFloatStateOf(1200f) }
    var canvasHeightPx by remember { mutableFloatStateOf(500f) }
    val whiteKeyCount = 52

    // Precomputed flattened temporal score timeline with accurate compound-meter beats & pre-calculated chord flags
    val waterfallTimeline = remember(score) {
        if (score == null || score.measures.isEmpty()) {
            null
        } else {
            val cumBeats = FloatArray(score.measures.size + 1)
            var currentBeatTotal = 0f
            score.measures.forEachIndexed { idx, measure ->
                cumBeats[idx] = currentBeatTotal
                val beatsInMeasure = (measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
                currentBeatTotal += beatsInMeasure
            }
            cumBeats[score.measures.size] = currentBeatTotal

            val flatNotes = ArrayList<FlatWaterfallNote>()
            score.measures.forEachIndexed { mIdx, measure ->
                val measureStartBeat = cumBeats[mIdx]
                val hasDifferentiatedVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }
                val posCounts = measure.notes.filter { !it.isRest }.groupingBy { (it.position * 20f).toInt() }.eachCount()

                for (note in measure.notes) {
                    if (!note.isRest && note.midiNote in 21..108) {
                        val isRh = if (hasDifferentiatedVoices) note.voice == 0 else note.midiNote >= 60
                        val isChord = (posCounts[(note.position * 20f).toInt()] ?: 0) >= 2
                        flatNotes.add(
                            FlatWaterfallNote(
                                midiNote = note.midiNote,
                                absoluteBeatStart = measureStartBeat + note.position,
                                durationBeats = note.duration.coerceAtLeast(0.15f),
                                isRightHand = isRh,
                                voice = note.voice,
                                isChordNote = isChord
                            )
                        )
                    }
                }
            }
            flatNotes.sortBy { it.absoluteBeatStart }
            WaterfallTimeline(flatNotes, cumBeats)
        }
    }

    val reusableTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }

    var fsFrameTicker by remember { mutableLongStateOf(0L) }

    // 1. Manual User Key Press
    LaunchedEffect(manualPressedNotes, leftHandColor, rightHandColor) {
        if (manualPressedNotes.isNotEmpty()) {
            val contactY = if (canvasHeightPx > 50f) canvasHeightPx else 320f
            for (midi in manualPressedNotes) {
                val baseColor = if (midi >= 60) rightHandColor else leftHandColor
                val color = if (isMidiNoteBlack(midi)) baseColor.copy(alpha = 0.95f) else baseColor
                if (fallingBlocks.none { it.midiNote == midi && it.isAscending && it.currentY > contactY - 80f }) {
                    fallingBlocks.add(
                        PianoWaterfallBlock(
                            midiNote = midi,
                            startY = contactY,
                            currentY = contactY,
                            color = color,
                            isAscending = true,
                            alpha = 1.0f
                        )
                    )
                }
            }
        }
    }

    // 2. Active MIDI Notes Fallback (when no score loaded)
    LaunchedEffect(activeMidiNotes, leftHandColor, rightHandColor) {
        if (activeMidiNotes.isNotEmpty() && (score == null || !isPlaying)) {
            val currentCanvasH = canvasHeightPx
            val contactY = if (currentCanvasH > 50f) currentCanvasH else 320f
            for (midi in activeMidiNotes) {
                if (midi !in manualPressedNotes) {
                    val baseColor = if (midi >= 60) rightHandColor else leftHandColor
                    val color = if (isMidiNoteBlack(midi)) baseColor.copy(alpha = 0.95f) else baseColor
                    if (fallingBlocks.none { it.midiNote == midi && !it.isAscending && it.currentY >= contactY - 10f }) {
                        fallingBlocks.add(
                            PianoWaterfallBlock(
                                midiNote = midi,
                                startY = contactY - 60f,
                                currentY = contactY,
                                color = color,
                                isAscending = false,
                                alpha = 1.0f
                            )
                        )
                    }
                }
            }
        }
    }

    // 3. Key landing flash and particles when a MIDI note becomes active
    LaunchedEffect(activeMidiNotes) {
        if (activeMidiNotes.isNotEmpty()) {
            val currentCanvasH = canvasHeightPx
            val currentCanvasW = canvasWidthPx
            val contactY = if (currentCanvasH > 50f) currentCanvasH else 320f
            val whiteKeyW = currentCanvasW / whiteKeyCount.toFloat()

            for (midi in activeMidiNotes) {
                if (midi !in 0..127) continue
                val geom = WaterfallKeyGeometryLookup.keyGeometries[midi]
                val baseBlockW = if (geom.isBlack) 0.62f * whiteKeyW else 0.88f * whiteKeyW
                val xOff = (whiteKeyW - baseBlockW) / 2f
                val strikeX = (geom.whiteKeyIndex * whiteKeyW) + xOff + baseBlockW / 2f
                val strikeY = contactY
                val baseColor = if (midi >= 60) rightHandColor else leftHandColor

                val isChordStrike = activeMidiNotes.size >= 2
                val chordColor = if (isChordStrike) Color(0xFFFFD700) else baseColor
                val baseRad = if (geom.isBlack) 28f else 38f
                if (flashes.size < 20) {
                    flashes.add(
                        PianoKeyFlash(
                            midiNote = midi,
                            centerX = strikeX,
                            centerY = strikeY,
                            color = chordColor,
                            velocity = 95,
                            alpha = 0.95f * flashHaloIntensity.coerceIn(0.2f, 1.0f),
                            radius = (if (isChordStrike) baseRad * 1.35f else baseRad) * flashSize.coerceIn(0.2f, 2.5f),
                            isChordStrike = isChordStrike
                        )
                    )
                }

                if (particlesEnabled && particles.size < 48) {
                    val sparkBase = if (geom.isBlack) (particleCount * 0.4f).toInt() else (particleCount * 0.6f).toInt()
                    val count = sparkBase.coerceIn(2, 16)
                    val pColor = if (lockParticleColor) baseColor else particleCustomColor
                    for (i in 0 until count) {
                        val angleRad = (-(30f + Random.nextFloat() * 120f) * (Math.PI / 180f)).toFloat()
                        val speed = (Random.nextFloat() * 4.5f + 2.0f) * (particleSpeed / 4.5f).coerceIn(0.2f, 3.0f)
                        val life = Random.nextFloat() * 18f + 14f
                        particles.add(
                            PianoSpark(
                                x = strikeX + (Random.nextFloat() - 0.5f) * (8f * particleExpansion.coerceIn(0.2f, 4f)),
                                y = strikeY,
                                vx = cos(angleRad) * speed * particleExpansion.coerceIn(0.5f, 2.5f),
                                vy = sin(angleRad) * speed,
                                alpha = 1.0f,
                                size = (Random.nextFloat() * 2.2f + 1.6f) * particleScale.coerceIn(0.3f, 3.0f),
                                maxLife = life,
                                currentLife = life,
                                color = pColor
                            )
                        )
                    }
                }
            }
        }
    }

    // 4. VSYNC-Locked Physics Loop (Sleeps when idle to save 100% CPU when paused)
    val hasActiveAnimations = isPlaying || activeMidiNotes.isNotEmpty() || manualPressedNotes.isNotEmpty()
    LaunchedEffect(hasActiveAnimations, fallingSpeedSetting) {
        var lastFrameNanos = 0L
        val gravity = 0.28f

        while (hasActiveAnimations || fallingBlocks.isNotEmpty() || particles.isNotEmpty() || flashes.isNotEmpty()) {
            withFrameNanos { frameNanos ->
                val dtScale = if (lastFrameNanos == 0L) 1f else ((frameNanos - lastFrameNanos) / 16_666_667f).coerceIn(0.5f, 2.5f)
                lastFrameNanos = frameNanos

                val currentCanvasH = canvasHeightPx
                val contactY = if (currentCanvasH > 50f) currentCanvasH else 320f

                val speedMultiplier = when (fallingSpeedSetting) {
                    "Slow" -> 0.75f
                    "Fast" -> 1.5f
                    "Extreme" -> 2.2f
                    else -> 1.0f
                }
                val stepY = 6f * speedMultiplier * dtScale

                val iterator = fallingBlocks.listIterator()
                while (iterator.hasNext()) {
                    val block = iterator.next()
                    if (block.isAscending) {
                        block.currentY -= stepY
                        block.startY = block.currentY + 28f
                        block.alpha = (block.currentY / contactY).coerceIn(0f, 1f)
                        if (block.currentY < -40f || block.alpha <= 0.04f) {
                            iterator.remove()
                        }
                    } else {
                        block.startY += stepY
                        block.currentY += stepY
                        if (block.startY > contactY + 80f) {
                            iterator.remove()
                        }
                    }
                }

                val pIter = particles.listIterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx * dtScale
                    p.y += p.vy * dtScale
                    p.vy += gravity * dtScale
                    p.vx *= 0.985f
                    p.currentLife -= dtScale
                    p.alpha = (p.currentLife / p.maxLife).coerceIn(0f, 1f)
                    if (p.currentLife <= 0f || p.alpha <= 0f) {
                        pIter.remove()
                    }
                }

                val fIter = flashes.listIterator()
                while (fIter.hasNext()) {
                    val f = fIter.next()
                    f.radius += 1.8f * flashSize.coerceIn(0.5f, 2.0f) * dtScale
                    f.alpha *= 0.88f
                    if (f.alpha < 0.03f) {
                        fIter.remove()
                    }
                }

                if (fallingBlocks.isNotEmpty() || particles.isNotEmpty() || flashes.isNotEmpty()) {
                    fsFrameTicker++
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val _fsTick = fsFrameTicker
        val whiteKeyWidthPx = size.width / whiteKeyCount.toFloat()
        val canvasH = size.height
        if (canvasWidthPx != size.width) canvasWidthPx = size.width
        if (canvasHeightPx != size.height) canvasHeightPx = size.height

        val gridLineColor = Color(0xFF221510)
        for (i in 0..whiteKeyCount) {
            val gx = i * whiteKeyWidthPx
            drawLine(color = gridLineColor, start = Offset(gx, 0f), end = Offset(gx, canvasH), strokeWidth = 1f)
        }

        val pillCorner = noteCornerRadius.coerceIn(2f, 16f)
        val cornerRad = CornerRadius(pillCorner, pillCorner)
        val borderStroke = Stroke(width = noteEdgeThickness.coerceIn(0.5f, 4f))

        // 1. Draw Free-Play / Ascending Note Pills
        for (block in fallingBlocks) {
            val geom = WaterfallKeyGeometryLookup.keyGeometries[block.midiNote]
            val baseBlockWidth = geom.baseWidthFraction * whiteKeyWidthPx
            val xOffset = (whiteKeyWidthPx - baseBlockWidth) / 2f
            val finalX = (geom.whiteKeyIndex * whiteKeyWidthPx) + xOffset
            val blockAlpha = block.alpha.coerceIn(0f, 1f)
            val bottomY = if (block.isAscending) (block.currentY + 28f).coerceAtMost(canvasH) else block.currentY.coerceAtMost(canvasH)
            val topY = if (block.isAscending) block.currentY.coerceAtLeast(0f) else block.startY.coerceAtMost(canvasH)
            val blockHeight = (bottomY - topY).coerceAtLeast(18f)

            if (glowEffect) {
                drawRoundRect(
                    color = block.color.copy(alpha = 0.22f * blockAlpha),
                    topLeft = Offset(finalX - 3f, topY - 3f),
                    size = Size(baseBlockWidth + 6f, blockHeight + 6f),
                    cornerRadius = CornerRadius(pillCorner + 3f, pillCorner + 3f)
                )
            }

            drawRoundRect(
                color = block.color.copy(alpha = 0.94f * blockAlpha),
                topLeft = Offset(finalX, topY),
                size = Size(baseBlockWidth, blockHeight),
                cornerRadius = cornerRad
            )

            drawRoundRect(
                color = Color.White.copy(alpha = 0.30f * blockAlpha),
                topLeft = Offset(finalX, topY),
                size = Size(baseBlockWidth, blockHeight),
                cornerRadius = cornerRad,
                style = borderStroke
            )

            if (blockHeight >= 14f) {
                val labelX = finalX + baseBlockWidth / 2f
                val centerY = (topY + bottomY) / 2f
                reusableTextPaint.alpha = (255 * blockAlpha).toInt().coerceIn(0, 255)
                reusableTextPaint.textSize = 10.dp.toPx()
                val fm = reusableTextPaint.fontMetrics
                val labelY = centerY - (fm.ascent + fm.descent) / 2f
                drawContext.canvas.nativeCanvas.drawText(geom.pitchName, labelX, labelY, reusableTextPaint)
            }
        }

        // 1b. Draw MIDI Score Lookahead Falling Notes (O(log N) Windowed & Zero-Allocation)
        if (waterfallTimeline != null) {
            val speedMul = when (fallingSpeedSetting) {
                "Slow" -> 0.75f
                "Fast" -> 1.5f
                "Extreme" -> 2.2f
                else -> 1.0f
            } * playbackSpeed.coerceIn(0.5f, 2.0f)
            val pixelsPerBeat = (canvasH / 3.0f) * speedMul
            val contactY = canvasH

            val cumBeats = waterfallTimeline.measureCumulativeBeats
            val mStartBeat = if (playheadMeasure < cumBeats.size) cumBeats[playheadMeasure] else 0f
            val mNextBeat = if (playheadMeasure + 1 < cumBeats.size) cumBeats[playheadMeasure + 1] else (mStartBeat + 4f)
            val measureBeats = (mNextBeat - mStartBeat).coerceAtLeast(1f)
            val currentPlaybackBeat = mStartBeat + (playheadOffset * measureBeats)

            val maxVisibleLookaheadBeats = (canvasH / pixelsPerBeat) + 0.5f
            val minBeat = currentPlaybackBeat - 0.5f
            val maxBeat = currentPlaybackBeat + maxVisibleLookaheadBeats

            val startIndex = waterfallTimeline.findStartWindowIndex(minBeat)
            val allNotes = waterfallTimeline.notes

            reusableTextPaint.alpha = 255
            reusableTextPaint.textSize = 10.dp.toPx()
            val fm = reusableTextPaint.fontMetrics
            val textBaselineOffset = (fm.ascent + fm.descent) / 2f

            for (idx in startIndex until allNotes.size) {
                val note = allNotes[idx]
                if (note.absoluteBeatStart > maxBeat) break

                if (note.isRightHand && isRightHandHidden) continue
                if (!note.isRightHand && isLeftHandHidden) continue
                if (!(enabledTrackIndices[note.voice] ?: true)) continue

                val noteDistBeats = note.absoluteBeatStart - currentPlaybackBeat
                val bottomY = contactY - (noteDistBeats * pixelsPerBeat)
                val topY = bottomY - (note.durationBeats * pixelsPerBeat)

                if (bottomY < 0f || topY > canvasH) continue

                val geom = WaterfallKeyGeometryLookup.keyGeometries[note.midiNote]
                val baseBlockWidth = geom.baseWidthFraction * whiteKeyWidthPx
                val xOffset = (whiteKeyWidthPx - baseBlockWidth) / 2f
                val finalX = (geom.whiteKeyIndex * whiteKeyWidthPx) + xOffset

                val isCurrentlySounding = noteDistBeats <= 0.05f && (noteDistBeats + note.durationBeats) >= -0.02f
                val effectiveBottomY = if (isCurrentlySounding && bottomY > contactY - 8f) bottomY.coerceAtMost(contactY) else bottomY
                val clampedBottomY = effectiveBottomY.coerceAtMost(canvasH)
                val clampedTopY = topY.coerceAtLeast(0f)
                val blockHeight = (clampedBottomY - clampedTopY).coerceAtLeast(14f)
                val noteColor = if (note.isRightHand) rightHandColor else leftHandColor

                if (glowEffect && isCurrentlySounding) {
                    drawRoundRect(
                        color = noteColor.copy(alpha = 0.32f),
                        topLeft = Offset(finalX - 3.5f, clampedTopY - 3.5f),
                        size = Size(baseBlockWidth + 7f, blockHeight + 7f),
                        cornerRadius = CornerRadius(pillCorner + 3.5f, pillCorner + 3.5f)
                    )
                }

                drawRoundRect(
                    color = noteColor.copy(alpha = if (isCurrentlySounding) 1.0f else 0.90f),
                    topLeft = Offset(finalX, clampedTopY),
                    size = Size(baseBlockWidth, blockHeight),
                    cornerRadius = cornerRad
                )

                drawRoundRect(
                    color = if (isCurrentlySounding) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.25f),
                    topLeft = Offset(finalX, clampedTopY),
                    size = Size(baseBlockWidth, blockHeight),
                    cornerRadius = cornerRad,
                    style = borderStroke
                )

                if (blockHeight >= 15f) {
                    val labelX = finalX + baseBlockWidth / 2f
                    val centerY = (clampedTopY + clampedBottomY) / 2f
                    drawContext.canvas.nativeCanvas.drawText(geom.pitchName, labelX, centerY - textBaselineOffset, reusableTextPaint)
                }
            }
        }

        // 2. Draw Contact Line Flashes
        if (glowEffect) {
            for (f in flashes) {
                val flashRadius = f.radius * (size.width / 1200f).coerceIn(0.8f, 1.6f)
                drawCircle(
                    color = f.color.copy(alpha = (f.alpha * 0.55f).coerceIn(0f, 1f)),
                    radius = flashRadius,
                    center = Offset(f.centerX, canvasH)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (f.alpha * 0.85f).coerceIn(0f, 1f)),
                    radius = flashRadius * 0.4f,
                    center = Offset(f.centerX, canvasH)
                )
            }
        }

        // 3. Draw Physics Sparks
        if (particlesEnabled) {
            for (p in particles) {
                val pAlpha = p.alpha.coerceIn(0f, 1f)
                val sparkRadius = p.size.dp.toPx()
                drawCircle(
                    color = p.color.copy(alpha = pAlpha * 0.85f),
                    radius = sparkRadius,
                    center = Offset(p.x, p.y)
                )
                drawLine(
                    color = Color.White.copy(alpha = pAlpha * 0.7f),
                    start = Offset(p.x, p.y),
                    end = Offset(p.x - p.vx * 1.4f, p.y - p.vy * 1.4f),
                    strokeWidth = sparkRadius * 0.65f
                )
            }
        }
    }
}
@Composable
fun PianoThreePedalUnit(
    sustainPressed: Boolean,
    sostenutoPressed: Boolean,
    softPressed: Boolean,
    onPedalClick: ((pedalType: Int, isPressed: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 1. Soft / Una Corda (CC 67)
                SinglePedalBlade(
                    name = "SOFT",
                    sub = "Una Corda",
                    isPressed = softPressed,
                    onClick = {
                        onPedalClick?.invoke(67, !softPressed)
                    }
                )

                // 2. Sostenuto (CC 66)
                SinglePedalBlade(
                    name = "SOST",
                    sub = "Sostenuto",
                    isPressed = sostenutoPressed,
                    onClick = {
                        onPedalClick?.invoke(66, !sostenutoPressed)
                    }
                )

                // 3. Sustain / Damper (CC 64)
                SinglePedalBlade(
                    name = "SUST",
                    sub = "Damper",
                    isPressed = sustainPressed,
                    onClick = {
                        onPedalClick?.invoke(64, !sustainPressed)
                    }
                )
            }
        }
    }
}

@Composable
private fun SinglePedalBlade(
    name: String,
    sub: String,
    isPressed: Boolean,
    onClick: () -> Unit
) {
    val brassGradientNormal = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE5C07B),
            Color(0xFFC89B3C),
            Color(0xFF996B1E),
            Color(0xFF734C0A)
        )
    )

    val brassGradientPressed = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF2A3),
            Color(0xFFFFD54F),
            Color(0xFFFF9800),
            Color(0xFFE65100)
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 0.dp)
    ) {
        // Top Mounting Bracket / Pin Housing (Pedal Top)
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A4A4C), Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                    )
                )
        )

        // Physical Brass Foot-pedal blade shape
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(26.dp)
                .graphicsLayer {
                    translationY = if (isPressed) 5.dp.toPx() else 0f
                    scaleY = if (isPressed) 0.90f else 1.0f
                }
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(if (isPressed) brassGradientPressed else brassGradientNormal)
                .border(
                    width = if (isPressed) 1.5.dp else 1.dp,
                    color = if (isPressed) Color(0xFFFFE082) else Color(0x664A3508),
                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                )
        ) {
            // Left Metallic Reflection Highlight
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp, top = 3.dp, bottom = 4.dp)
                    .background(Color.White.copy(alpha = if (isPressed) 0.65f else 0.35f), RoundedCornerShape(2.dp))
            )

            // Right Bevel Shadow
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 1.dp, top = 4.dp, bottom = 4.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            )

            if (isPressed) {
                // Radiant glow ring when pressed
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33FFB300))
                )
            }
        }

        Text(
            text = name,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = if (isPressed) Color(0xFFFFCA28) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Corner MIDI Status Badge showing:
 * - Real Connected Piano / Keyboard Name
 * - Incoming note velocity badge (e.g. "Vel 112")
 * - Active MIDI channel
 */
@Composable
fun PianoMidiStatusBadge(
    deviceName: String?,
    lastVelocity: Int,
    lastChannel: Int,
    onDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = !deviceName.isNullOrBlank()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onDeviceClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (isConnected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF6B7280).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Glowing status indicator light
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isConnected) Color(0xFF10B981) else Color(0xFF9CA3AF))
                    .border(
                        1.dp,
                        if (isConnected) Color(0xFF34D399) else Color(0xFF6B7280),
                        RoundedCornerShape(50)
                    )
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isConnected) "🎹" else "🎹 🚫",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }

                if (isConnected && lastVelocity > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VEL: $lastVelocity",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFB300)
                        )
                        Text(
                            text = "CH: $lastChannel",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Elegant prompt dialog for USB/MIDI Piano connection and disconnection
 */
@Composable
fun MidiConnectionPromptDialog(
    prompt: com.creativeblisslabs.peqlearn.app.ui.viewmodel.MidiPromptMessage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (prompt.isConnected) "🎹" else "🔌",
                    fontSize = 20.sp
                )
                Text(
                    text = prompt.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (prompt.isConnected) Color(0xFF34D399) else Color(0xFFFF7043)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = prompt.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Device:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(prompt.deviceName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prompt.isConnected) Color(0xFF059669) else MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    )
}