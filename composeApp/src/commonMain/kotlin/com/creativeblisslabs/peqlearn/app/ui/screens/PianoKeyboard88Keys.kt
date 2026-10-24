package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.foundation.Canvas
import com.creativeblisslabs.peqlearn.app.ui.components.VideoExportStudioSection
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.List      
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.creativeblisslabs.peqlearn.app.ui.components.setScreenOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.setSystemBarsVisibility
import com.creativeblisslabs.peqlearn.app.ui.components.pEQOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
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
    var radius: Float = 36f
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
    var notationFormat by rememberSaveable { mutableStateOf("Staff") }
    val showNoteLabels = viewModel.showPianoKeyLabels.value
    val showWaterfall = viewModel.showWaterfall.value
    val is3DPerspectiveMode = viewModel.is3DPerspectiveMode.value
    val horizonScale = viewModel.horizonScale.floatValue
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

    var internalKeyboardModeName by rememberSaveable { mutableStateOf("PIANO") }
    LaunchedEffect(internalKeyboardModeName) {
        if (internalKeyboardModeName == "PIANO") {
            viewModel.audioEngine.setInstrument("PIANO")
        }
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

LearnSectionCard(title = "Notation & Piano Viewer", icon = Icons.Filled.List) {
        Column(
        ) {
           

            // NOTATION VIEW ON TOP (Explicit height for scrollable container)
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            // PIANO KEYBOARD ON BOTTOM
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                       


                                         Button(
                                            onClick = { viewModel.showNotationNoteNames.value = !viewModel.showNotationNoteNames.value },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (viewModel.showNotationNoteNames.value) primaryColor else MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Letters", fontSize = 10.sp, color = if (viewModel.showNotationNoteNames.value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                        Button(
                                            onClick = { viewModel.showLyrics.value = !viewModel.showLyrics.value },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (viewModel.showLyrics.value) primaryColor else MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Lyrics", fontSize = 10.sp, color = if (viewModel.showLyrics.value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }

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
                                            Text("Score", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.showWaterfall.value = !viewModel.showWaterfall.value
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (viewModel.showWaterfall.value) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Waterfall", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (viewModel.showWaterfall.value) Color.Black else MaterialTheme.colorScheme.onSurface)
                                        }

                                        IconButton(onClick = { viewModel.showSettings.value = true }, modifier = Modifier.size(26.dp)) {
                                            Icon(imageVector = Icons.Default.Tune, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                        }
                                        Button(
                                            onClick = { showExportDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(Icons.Default.Movie, contentDescription = "Export Video", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Video", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    
                                    
                                        Button(
                                            onClick = { showFullscreenNotation = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(Icons.Default.MusicNote, contentDescription = "Full Notation", modifier = Modifier.size(13.dp), tint = primaryColor)
                                            Spacer(Modifier.width(3.dp))
                                            Text("Notation", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
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
                        // PianoMidiStatusBadge(
                        //     deviceName = viewModel.connectedMidiDeviceName.value,
                        //     lastVelocity = viewModel.lastMidiVelocity.intValue,
                        //     lastChannel = viewModel.lastMidiChannel.intValue,
                        //     onDeviceClick = {
                        //         viewModel.onScanMidiDevicesRequested?.invoke()
                        //     }
                        // )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                   
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(0.3f))
                    ) {
                        if (showWaterfall) {
                            Canvas(modifier = Modifier.width(keyboardWidth).height(140.dp).background(Color.Black.copy(0.25f))) {
                                val _tick = frameTicker // Re-draw canvas on frame ticker without causing column recomposition
                                val availableWidth = size.width
                                val canvasH = size.height
                                val maxWhiteKeyWidthPx = with(density) { 42.dp.toPx() }
                                val calculatedKeyWidth = availableWidth / whiteNotes.size.coerceAtLeast(1)
                                val finalWhiteKeyWidthPx = if (calculatedKeyWidth > maxWhiteKeyWidthPx) maxWhiteKeyWidthPx else calculatedKeyWidth
                                val totalKeyboardWidthPx = finalWhiteKeyWidthPx * whiteNotes.size
                                val startPaddingOffset = (availableWidth - totalKeyboardWidthPx) / 2f
                                val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f

                                // 1. Straight Falling Note Ribbons with Neon Glow & Centered Note Names
                                fallingBlocks.forEach { block ->
                                    val isBlack = isMidiNoteBlack(block.midiNote)
                                    val baseBlockWidth = if (isBlack) blackKeyWidthPx else (finalWhiteKeyWidthPx - 2f)
                                    val finalX = if (isBlack) {
                                        val prevWhiteNote = block.midiNote - 1
                                        val whiteIndex = whiteNotes.indexOf(prevWhiteNote)
                                        if (whiteIndex >= 0) {
                                            startPaddingOffset + (whiteIndex * finalWhiteKeyWidthPx) + (finalWhiteKeyWidthPx - blackKeyWidthPx / 2f)
                                        } else {
                                            startPaddingOffset
                                        }
                                    } else {
                                        val whiteIndex = whiteNotes.indexOf(block.midiNote)
                                        val left = if (whiteIndex >= 0) startPaddingOffset + (whiteIndex * finalWhiteKeyWidthPx) else startPaddingOffset
                                        left + 1f
                                    }

                                    // Straight vertical fall/rise travelling full height to top of keyboard
                                    val blockHeight = 28f
                                    val bottomY = if (block.isAscending) block.currentY + blockHeight else block.currentY.coerceAtMost(canvasH)
                                    val topY = if (block.isAscending) block.currentY.coerceAtLeast(0f) else (bottomY - blockHeight).coerceAtLeast(0f)
                                    val centerX = finalX + baseBlockWidth / 2f
                                    val blockAlpha = block.alpha.coerceIn(0f, 1f)

                                    // Contact line hit detection: triggers right as ribbon reaches top of keyboard keys
                                    val contactHit = if (block.isAscending) {
                                        block.currentY >= (canvasH - 20f)
                                    } else {
                                        block.currentY >= (canvasH - 14f) && block.currentY <= (canvasH + 18f)
                                    }
                                    if (contactHit && viewModel.showWaterfallParticles.value) {
                                        if (inlineFlashes.none { it.midiNote == block.midiNote && it.alpha > 0.35f }) {
                                            inlineFlashes.add(
                                                PianoKeyFlash(
                                                    midiNote = block.midiNote,
                                                    centerX = centerX,
                                                    centerY = canvasH,
                                                    color = block.color,
                                                    alpha = 0.95f,
                                                    radius = if (isBlack) 24f else 32f
                                                )
                                            )
                                            // Trigger Physics Sparks
                                            val sparkCount = if (isBlack) 10 else 14
                                            for (i in 0 until sparkCount) {
                                                val angleRad = (-(35f + Random.nextFloat() * 110f) * (Math.PI / 180f)).toFloat()
                                                val speed = Random.nextFloat() * 6.5f + 2.5f
                                                val life = Random.nextFloat() * 20f + 16f
                                                inlineSparks.add(
                                                    PianoSpark(
                                                        x = centerX + (Random.nextFloat() - 0.5f) * 6f,
                                                        y = canvasH,
                                                        vx = cos(angleRad) * speed,
                                                        vy = sin(angleRad) * speed,
                                                        alpha = 1.0f,
                                                        size = Random.nextFloat() * 2.2f + 1.4f,
                                                        maxLife = life,
                                                        currentLife = life,
                                                        color = block.color
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    val pillCorner = 12f
                                    val pillHeight = (bottomY - topY).coerceAtLeast(10f)

                                    // Outer Soft Glow
                                    drawRoundRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                block.color.copy(alpha = 0.40f * blockAlpha),
                                                block.color.copy(alpha = 0.12f * blockAlpha),
                                                Color.Transparent
                                            ),
                                            center = Offset(centerX, topY + pillHeight / 2f),
                                            radius = baseBlockWidth * 1.3f
                                        ),
                                        topLeft = Offset(finalX - 3f, topY - 3f),
                                        size = Size(baseBlockWidth + 6f, pillHeight + 6f),
                                        cornerRadius = CornerRadius(pillCorner + 3f, pillCorner + 3f)
                                    )

                                    // Core Waterdrop Pill Body
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                block.color.copy(alpha = 0.90f * blockAlpha),
                                                block.color.copy(alpha = 1.0f * blockAlpha),
                                                block.color.copy(alpha = 0.92f * blockAlpha)
                                            ),
                                            startY = topY,
                                            endY = bottomY
                                        ),
                                        topLeft = Offset(finalX, topY),
                                        size = Size(baseBlockWidth, pillHeight),
                                        cornerRadius = CornerRadius(pillCorner, pillCorner)
                                    )

                                    // Top Gloss Reflection (Waterdrop glassy shine)
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.35f * blockAlpha),
                                                Color.White.copy(alpha = 0.05f * blockAlpha),
                                                Color.Transparent
                                            ),
                                            startY = topY,
                                            endY = topY + (pillHeight * 0.55f)
                                        ),
                                        topLeft = Offset(finalX + 1.2f, topY + 1.2f),
                                        size = Size(baseBlockWidth - 2.4f, pillHeight * 0.55f),
                                        cornerRadius = CornerRadius(pillCorner - 1f, pillCorner - 1f)
                                    )

                                    // Crisp Border
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.28f * blockAlpha),
                                        topLeft = Offset(finalX, topY),
                                        size = Size(baseBlockWidth, pillHeight),
                                        cornerRadius = CornerRadius(pillCorner, pillCorner),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
                                    )

                                    // Pitch Letter (e.g., A, B, C#, F#)
                                    val noteLabel = getPitchOnlyFromMidi(block.midiNote)
                                    val textY = topY + (pillHeight / 2f) + 3.8.dp.toPx()
                                    drawContext.canvas.nativeCanvas.drawText(
                                        noteLabel,
                                        centerX,
                                        textY,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 10.dp.toPx()
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                            isAntiAlias = true
                                            setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
                                        }
                                    )
                                }

                                // 1b. MIDI Score Lookahead Falling Notes: streams down from top and reaches contact line (canvasH) exactly when active
                                val currentScoreObj = viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score
                                if (viewModel.isScorePlaying.value && currentScoreObj != null) {
                                    val currentM = viewModel.scorePlayheadMeasure.value
                                    val currentOff = viewModel.scorePlayheadOffset.value
                                    val speedMul = when (viewModel.fallingNotesSpeed.value) {
                                        "Slow" -> 0.75f
                                        "Fast" -> 1.5f
                                        "Extreme" -> 2.2f
                                        else -> 1.1f
                                    }
                                    val pixelsPerBeat = (canvasH / 3.0f) * speedMul
                                    val rhColor = viewModel.rightHandColor.value
                                    val lhColor = viewModel.leftHandColor.value

                                    for (mIdx in currentM..(currentM + 2)) {
                                        val measure = currentScoreObj.measures.getOrNull(mIdx) ?: continue
                                        val beatsInMeasure = measure.timeSignatureNumerator.toFloat().coerceAtLeast(1f)
                                        val measureBeatStart = (mIdx - currentM) * beatsInMeasure - (currentOff * beatsInMeasure)

                                        for (note in measure.notes) {
                                            if (note.isRest || note.midiNote !in startMidi..(startMidi + totalKeys - 1)) continue
                                            val noteDistBeats = measureBeatStart + note.position
                                            val noteDurBeats = note.duration.coerceAtLeast(0.2f)

                                            // Note reaches keys (canvasH) right when noteDistBeats == 0
                                            val bottomY = canvasH - (noteDistBeats * pixelsPerBeat)
                                            val topY = bottomY - (noteDurBeats * pixelsPerBeat)

                                            // Render only if visible in canvas viewport
                                            if (bottomY < 0f || topY > canvasH) continue

                                            val isBlack = isMidiNoteBlack(note.midiNote)
                                            val baseBlockWidth = if (isBlack) blackKeyWidthPx else (finalWhiteKeyWidthPx - 2f)
                                            val finalX = if (isBlack) {
                                                val prevWhiteNote = note.midiNote - 1
                                                val whiteIndex = whiteNotes.indexOf(prevWhiteNote)
                                                if (whiteIndex >= 0) {
                                                    startPaddingOffset + (whiteIndex * finalWhiteKeyWidthPx) + (finalWhiteKeyWidthPx - blackKeyWidthPx / 2f)
                                                } else {
                                                    startPaddingOffset
                                                }
                                            } else {
                                                val whiteIndex = whiteNotes.indexOf(note.midiNote)
                                                val left = if (whiteIndex >= 0) startPaddingOffset + (whiteIndex * finalWhiteKeyWidthPx) else startPaddingOffset
                                                left + 1f
                                            }

                                            val clampedTopY = topY.coerceAtLeast(0f)
                                            val clampedBottomY = bottomY.coerceAtMost(canvasH)
                                            val pillHeight = (clampedBottomY - clampedTopY).coerceAtLeast(10f)
                                            val pillCorner = 12f
                                            val centerX = finalX + baseBlockWidth / 2f
                                            val noteColor = if (note.midiNote >= 60) rhColor else lhColor

                                            // Ambient Glow
                                            drawRoundRect(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        noteColor.copy(alpha = 0.40f),
                                                        noteColor.copy(alpha = 0.12f),
                                                        Color.Transparent
                                                    ),
                                                    center = Offset(centerX, clampedTopY + pillHeight / 2f),
                                                    radius = baseBlockWidth * 1.3f
                                                ),
                                                topLeft = Offset(finalX - 3f, clampedTopY - 3f),
                                                size = Size(baseBlockWidth + 6f, pillHeight + 6f),
                                                cornerRadius = CornerRadius(pillCorner + 3f, pillCorner + 3f)
                                            )

                                            // Core Solid Body
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        noteColor.copy(alpha = 0.90f),
                                                        noteColor.copy(alpha = 1.0f),
                                                        noteColor.copy(alpha = 0.92f)
                                                    ),
                                                    startY = clampedTopY,
                                                    endY = clampedBottomY
                                                ),
                                                topLeft = Offset(finalX, clampedTopY),
                                                size = Size(baseBlockWidth, pillHeight),
                                                cornerRadius = CornerRadius(pillCorner, pillCorner)
                                            )

                                            // Crisp Border
                                            drawRoundRect(
                                                color = Color.White.copy(alpha = 0.28f),
                                                topLeft = Offset(finalX, clampedTopY),
                                                size = Size(baseBlockWidth, pillHeight),
                                                cornerRadius = CornerRadius(pillCorner, pillCorner),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
                                            )

                                            // Pitch Letter
                                            val noteLabel = getPitchOnlyFromMidi(note.midiNote)
                                            val textY = clampedTopY + (pillHeight / 2f) + 3.8.dp.toPx()
                                            drawContext.canvas.nativeCanvas.drawText(
                                                noteLabel,
                                                centerX,
                                                textY,
                                                android.graphics.Paint().apply {
                                                    color = android.graphics.Color.WHITE
                                                    textSize = 10.dp.toPx()
                                                    textAlign = android.graphics.Paint.Align.CENTER
                                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                                    isAntiAlias = true
                                                    setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. Key Strike Contact Bloom / Flashes
                                inlineFlashes.forEach { f ->
                                    val flashRadius = f.radius
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = (f.alpha * 0.95f).coerceIn(0f, 1f)),
                                                f.color.copy(alpha = (f.alpha * 0.7f).coerceIn(0f, 1f)),
                                                f.color.copy(alpha = (f.alpha * 0.25f).coerceIn(0f, 1f)),
                                                Color.Transparent
                                            ),
                                            center = Offset(f.centerX, canvasH),
                                            radius = flashRadius
                                        ),
                                        radius = flashRadius,
                                        center = Offset(f.centerX, canvasH)
                                    )
                                }

                                // 3. Directional Physics Sparks with Velocity Trails
                                inlineSparks.forEach { p ->
                                    val pAlpha = p.alpha.coerceIn(0f, 1f)
                                    val sparkRadius = p.size.dp.toPx()

                                    drawCircle(
                                        color = Color.White.copy(alpha = pAlpha),
                                        radius = sparkRadius * 0.55f,
                                        center = Offset(p.x, p.y)
                                    )
                                    drawCircle(
                                        color = p.color.copy(alpha = pAlpha * 0.75f),
                                        radius = sparkRadius,
                                        center = Offset(p.x, p.y)
                                    )
                                    drawLine(
                                        color = p.color.copy(alpha = pAlpha * 0.6f),
                                        start = Offset(p.x, p.y),
                                        end = Offset(p.x - p.vx * 1.4f, p.y - p.vy * 1.4f),
                                        strokeWidth = sparkRadius * 0.7f
                                    )
                                }
                            }
                        }
                        
                            UnifiedPianoKeyboard(
                                activeMidiNotes = viewModel.midiActiveNotes,
                                accentColor = primaryColor,
                                activeCoordinate = viewModel.midiPlayingNote.value,
                                showNoteNames = showNoteLabels,
                                isPlaying = viewModel.isScorePlaying.value,
                                startNote = startMidi,
                                endNote = startMidi + totalKeys - 1,
                                leftHandColor = viewModel.leftHandColor.value,
                                rightHandColor = viewModel.rightHandColor.value,
                                onKeyTapped = { midiNote ->
                                    pressedKeys.add(midiNote)
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

                        // 3-Pedal Grand Piano Unit placed above piano keyboard in right corner
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
    
    

    // Real-Time MIDI Connection & Disconnection Prompt Dialog
    viewModel.midiConnectionPrompt.value?.let { prompt ->
        MidiConnectionPromptDialog(
            prompt = prompt,
            onDismiss = { viewModel.dismissMidiPrompt() }
        )
    }

    if (showFullscreenNotation) {
        Dialog(
            onDismissRequest = { showFullscreenNotation = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07080C))
            ) {
                NotationWorkspaceItem(
                    viewModel = viewModel,
                    accentColor = primaryColor,
                    onBack = { showFullscreenNotation = false },
                    launchFilePicker = { viewModel.onPickScoreFileRequested?.invoke() }
                )
            }
        }
    }

    if (viewModel.isFullscreenKeyboard.value) {
        Dialog(
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
                // Layout Mode: "Split" (Notation on Top + Waterfall + Keyboard on Bottom), "Waterfall", or "Notation"
                var fsLayoutMode by rememberSaveable { mutableStateOf("Split") }
                var fsLayoutMenuExpanded by remember { mutableStateOf(false) }
                var fsInstrumentMenuExpanded by remember { mutableStateOf(false) }
                val fsManualPressedKeys = remember { mutableStateListOf<Int>() }
                var showSpeedOptimizationDialog by remember { mutableStateOf(false) }

                val isPlaying = viewModel.isScorePlaying.value
                val currentScore = viewModel.currentScore.value as? com.creativeblisslabs.peqlearn.app.ui.screens.Score

                val totalSeconds = remember(currentScore) {
                    if (currentScore == null) 0.0
                    else {
                        currentScore.measures.sumOf {
                            (60.0 / currentScore.tempo.toDouble()) * it.timeSignatureNumerator.toDouble()
                        }
                    }
                }

                val currentMeasure = viewModel.scorePlayheadMeasure.value
                val currentOffset = viewModel.scorePlayheadOffset.value

                val currentSeconds = remember(currentScore, currentMeasure, currentOffset) {
                    if (currentScore == null) 0.0
                    else {
                        var elapsed = 0.0
                        for (i in 0 until currentMeasure) {
                            val m = currentScore.measures.getOrNull(i) ?: break
                            elapsed += (60.0 / currentScore.tempo.toDouble()) * m.timeSignatureNumerator.toDouble()
                        }
                        val m = currentScore.measures.getOrNull(currentMeasure)
                        if (m != null) {
                            elapsed += (currentOffset.toDouble() * (60.0 / currentScore.tempo.toDouble()) * m.timeSignatureNumerator.toDouble())
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
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
                                        Column {
                                            Text(
                                                "NOW PLAYING",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7986CB),
                                                letterSpacing = 1.sp
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
                                    }

                                    // Right: Clean Non-redundant Action Icons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Open / Import Score Button
                                        Button(
                                            onClick = { viewModel.onPickScoreFileRequested?.invoke() },
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
                                        }

                    

                                        // Theme / Palette Button (Toggles Letters / Style)
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF231838),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val nextState = !viewModel.showNotationNoteNames.value
                                                    viewModel.showNotationNoteNames.value = nextState
                                                    viewModel.showPianoKeyLabels.value = nextState
                                                    fsShowLetters = nextState
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Palette,
                                                    contentDescription = "Color Theme / Labels",
                                                    tint = Color(0xFF9FA8DA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
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

                                        // Display Mode Selector (Split: Notation on Top + Waterfall, Waterfall Only, Notation Only)
                                        Box {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFF1E162E),
                                                border = BorderStroke(1.dp, Color(0xFF372754)),
                                                modifier = Modifier.clickable { fsLayoutMenuExpanded = true }
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
                                                    Text(
                                                        fsLayoutMode,
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
                                                expanded = fsLayoutMenuExpanded,
                                                onDismissRequest = { fsLayoutMenuExpanded = false }
                                            ) {
                                                listOf(
                                                    "Split (Notation + Waterfall)" to "Split",
                                                    "Keyboard + Waterfall" to "KeyboardWaterfall",
                                                    "Waterfall Only" to "Waterfall",
                                                    "Notation Only" to "Notation",
                                                    "Keyboard Only" to "Keyboard"
                                                ).forEach { (label, modeKey) ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                label,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (fsLayoutMode == modeKey) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (fsLayoutMode == modeKey) Color(0xFF5C6BC0) else Color.Unspecified
                                                            )
                                                        },
                                                        onClick = {
                                                            fsLayoutMode = modeKey
                                                            fsShowWaterfall = (modeKey == "Waterfall" || modeKey == "Split" || modeKey == "KeyboardWaterfall")
                                                            fsLayoutMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Learn / Theory
                                        IconButton(
                                            onClick = { fsShowLyrics = !fsShowLyrics },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.School,
                                                contentDescription = "Learn / Lyrics",
                                                tint = if (fsShowLyrics) Color(0xFF5C6BC0) else Color.White.copy(0.75f),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Instrument Dropdown Selector
                                        Box {
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
                                                        viewModel.selectedInstrument.value,
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
                                                listOf("Piano", "Violin", "Guitar", "Bass", "Flute", "Saxophone").forEach { inst ->
                                                    DropdownMenuItem(
                                                        text = { Text(inst, fontSize = 12.sp) },
                                                        onClick = {
                                                            viewModel.selectedInstrument.value = inst
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
                            val isLandscapeWide = availableW >= 600.dp
                            // In landscape/wide screens, fit 100% of the 88 keys to screen width so no horizontal scroll is needed
                            val finalWidth = if (isLandscapeWide) availableW else maxOf(availableW, 30.dp * whiteKeyCount)

                            val scrollModifier = if (isLandscapeWide) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(fsScrollState)
                            }

                            Box(modifier = scrollModifier) {
                                Column(
                                    modifier = Modifier
                                        .width(finalWidth)
                                        .fillMaxHeight()
                                ) {
                                    // Visual Area: Accommodates Split (Notation on Top + Waterfall), Waterfall only, Notation only, or Keyboard only
                                    when (fsLayoutMode) {
                                        "Split" -> {
                                            // 1. Notation Rendering on Top (Balanced 52% height with bottom padding to prevent cut off)
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

                                            // 2. Waterfall Falling Notes in Middle (48% height)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(0.48f)
                                                    .background(Color.Black)
                                            ) {
                                                SynthesiaWaterfallOverlay(
                                                    activeMidiNotes = viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key }.toSet(),
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
                                                    is3DPerspective = viewModel.is3DPerspectiveMode.value,
                                                    horizonScale = viewModel.horizonScale.floatValue,
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
                                                    onFpsDropDetected = {
                                                        if (viewModel.fallingNotesSpeed.value != "Slow" || viewModel.playbackSpeed.value > 0.75f) {
                                                            showSpeedOptimizationDialog = true
                                                        }
                                                    }
                                                 )
                                            }
                                        }
                                        "Notation" -> {
                                            // Full height notation viewer
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
                                                    showScoreHeader = false,
                                                    compactStaffMode = true,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        "Keyboard" -> {
                                            // Keyboard Only: Spacer for upper visual area so piano keyboard dominates
                                            Spacer(modifier = Modifier.weight(0.12f))
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
                                                    activeMidiNotes = viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key }.toSet(),
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
                                                    is3DPerspective = viewModel.is3DPerspectiveMode.value,
                                                    horizonScale = viewModel.horizonScale.floatValue,
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
                                                    onFpsDropDetected = {
                                                        if (viewModel.fallingNotesSpeed.value != "Slow" || viewModel.playbackSpeed.value > 0.75f) {
                                                            showSpeedOptimizationDialog = true
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Full-fit 88-Key Piano Keyboard (Visible in Split, Keyboard + Waterfall, and Keyboard Only modes)
                                    if (fsLayoutMode != "Notation" && fsLayoutMode != "Waterfall") {
                                        val keyboardModifier = if (fsLayoutMode == "Keyboard") {
                                            Modifier
                                                .fillMaxWidth()
                                                .weight(0.88f)
                                                .background(Color(0xFF161616))
                                        } else {
                                            Modifier
                                                .fillMaxWidth()
                                                .height(145.dp)
                                                .background(Color(0xFF161616))
                                        }

                                        Box(modifier = keyboardModifier) {
                                            UnifiedPianoKeyboard(
                                                activeMidiNotes = viewModel.midiActiveNotes,
                                                accentColor = primaryColor,
                                                activeCoordinate = viewModel.midiPlayingNote.value,
                                                // showNoteNames = fsShowLetters,
                                                isPlaying = isPlaying,
                                                startNote = 21,
                                                endNote = 108,
                                                fillWidth = true,
                                                leftHandColor = viewModel.leftHandColor.value,
                                                rightHandColor = viewModel.rightHandColor.value,
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

                                            // Studio Red Velvet Cloth Strip (from MIDIVisualizer-master velvet.frag)
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
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                                val mDur = (60f / currentScore.tempo) * m.timeSignatureNumerator
                                                if (targetTotalSec < accumulated + mDur) {
                                                    targetM = i
                                                    targetO = (targetTotalSec - accumulated) / mDur
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

                                // 12. Vertical Scroll / Zoom / Perspective Slider
                                Icon(
                                    Icons.Default.SwapVert,
                                    contentDescription = "Perspective / Horizon",
                                    tint = Color.White.copy(0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Slider(
                                    value = viewModel.horizonScale.floatValue,
                                    onValueChange = { viewModel.horizonScale.floatValue = it },
                                    valueRange = 0.3f..1.0f,
                                    modifier = Modifier.width(65.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF5C6BC0),
                                        inactiveTrackColor = Color(0xFF231838)
                                    )
                                )

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
                                Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Performance Video & Notation Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showExportDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Render Full Comprehensive Studio Settings
                    VideoExportStudioSection(viewModel = viewModel)

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
                is3DPerspective = viewModel.is3DPerspectiveMode.value,
                on3DPerspectiveChange = { viewModel.is3DPerspectiveMode.value = it },
                horizonScale = viewModel.horizonScale.floatValue,
                onHorizonScaleChange = { viewModel.horizonScale.floatValue = it },
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
    is3DPerspective: Boolean = viewModel.is3DPerspectiveMode.value,
    on3DPerspectiveChange: (Boolean) -> Unit = { viewModel.is3DPerspectiveMode.value = it },
    horizonScale: Float = viewModel.horizonScale.floatValue,
    onHorizonScaleChange: (Float) -> Unit = { viewModel.horizonScale.floatValue = it },
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
                SettingsSection(title = "Notation Display Format") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Format Mode", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            val formats = listOf("Staff", "Tab", "Both")
                            Row(modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(8.dp)).padding(3.dp)) {
                                formats.forEach { f ->
                                    val currentFmt = viewModel.notationFormat.value
                                    val active = (f == "Staff" && (currentFmt == "Staff" || currentFmt == "Staff")) ||
                                            (f == "Tab" && (currentFmt == "Tab" || currentFmt == "Tab")) ||
                                            (f == "Both" && (currentFmt == "Both" || currentFmt == "Both"))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) Color(0xFFFFD700) else Color.Transparent)
                                            .clickable {
                                                val target = if (f == "Both Viewers") "Both Viewers" else "$f Only"
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
                        val keys = listOf("C Major", "G Major", "D Major", "A Major", "F Major", "B♭ Major", "E♭ Major", "A Minor")
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
                                        .clickable { viewModel.selectedKeySignature.value = key }
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
                                        .clickable { viewModel.selectedInstrument.value = inst }
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("3D Perspective Waterfall", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = is3DPerspective,
                                onCheckedChange = on3DPerspectiveChange,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        if (is3DPerspective) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Horizon Vanishing Depth: ${(horizonScale * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = horizonScale,
                                    onValueChange = onHorizonScaleChange,
                                    valueRange = 0.35f..0.95f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                                )
                            }
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
    is3DPerspective: Boolean = true,
    horizonScale: Float = 0.68f,
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
    onFpsDropDetected: () -> Unit = {}
) {
    val fallingBlocks = remember { ArrayList<PianoWaterfallBlock>() }
    val particles = remember { ArrayList<PianoSpark>() }
    val flashes = remember { ArrayList<PianoKeyFlash>() }
    var canvasWidthPx by remember { mutableFloatStateOf(1200f) }
    var canvasHeightPx by remember { mutableFloatStateOf(500f) }
    val whiteKeyCount = 52

    var fsFrameTicker by remember { mutableLongStateOf(0L) }

    // 1. Manual User Key Press: Waterfall rises from bottom (contact line) to top
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

    // 2. Active MIDI Notes Fallback (when no score loaded): notes fall towards the keys
    LaunchedEffect(activeMidiNotes, leftHandColor, rightHandColor) {
        if (activeMidiNotes.isNotEmpty() && (score == null || !isPlaying)) {
            for (midi in activeMidiNotes) {
                if (midi !in manualPressedNotes) {
                    val baseColor = if (midi >= 60) rightHandColor else leftHandColor
                    val color = if (isMidiNoteBlack(midi)) baseColor.copy(alpha = 0.95f) else baseColor
                    if (fallingBlocks.size < 60) {
                        fallingBlocks.add(
                            PianoWaterfallBlock(
                                midiNote = midi,
                                startY = 0f,
                                currentY = 24f,
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

    // Trigger key landing flash and particles when a MIDI note becomes active (landing at keyboard)
    LaunchedEffect(activeMidiNotes) {
        if (activeMidiNotes.isNotEmpty()) {
            val currentCanvasH = canvasHeightPx
            val currentCanvasW = canvasWidthPx
            val contactY = if (currentCanvasH > 50f) currentCanvasH else 320f
            val whiteKeyW = currentCanvasW / whiteKeyCount.toFloat()

            for (midi in activeMidiNotes) {
                val wIdx = getWhiteKeyIndexForMidi(midi)
                val isBlack = isMidiNoteBlack(midi)
                val baseBlockW = if (isBlack) 0.62f * whiteKeyW else 0.88f * whiteKeyW
                val xOff = (whiteKeyW - baseBlockW) / 2f
                val strikeX = (wIdx * whiteKeyW) + xOff + baseBlockW / 2f
                val strikeY = contactY
                val baseColor = if (midi >= 60) rightHandColor else leftHandColor

                if (glowEffect && flashes.none { it.midiNote == midi && it.alpha > 0.4f }) {
                    val baseRad = if (isBlack) 28f else 38f
                    if (flashes.size < 24) {
                        flashes.add(
                            PianoKeyFlash(
                                midiNote = midi,
                                centerX = strikeX,
                                centerY = strikeY,
                                color = baseColor,
                                velocity = 95,
                                alpha = 0.95f * flashHaloIntensity.coerceIn(0.2f, 1.0f),
                                radius = baseRad * flashSize.coerceIn(0.2f, 2.5f)
                            )
                        )
                    }
                }

                if (particlesEnabled && particles.size < 64) {
                    val sparkBase = if (isBlack) (particleCount * 0.5f).toInt() else (particleCount * 0.75f).toInt()
                    val count = sparkBase.coerceIn(2, 24)
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

    LaunchedEffect(Unit) {
        val gravity = 0.28f
        var lastTimeNanos = 0L
        var droppedFrameCount = 0
        while (true) {
            val beforeDelay = io.ktor.util.date.getTimeMillis()
            delay(16L)
            val afterDelay = io.ktor.util.date.getTimeMillis()
            val frameDelta = afterDelay - beforeDelay
            if (frameDelta > 45L) { // Frame took > 45ms (less than 22 FPS)
                droppedFrameCount++
                if (droppedFrameCount >= 6) { // 6 lag spikes detected
                    onFpsDropDetected()
                    droppedFrameCount = 0
                }
            } else if (frameDelta < 22L) {
                droppedFrameCount = (droppedFrameCount - 1).coerceAtLeast(0)
            }

            val currentCanvasH = canvasHeightPx
            val currentCanvasW = canvasWidthPx
            val contactY = if (currentCanvasH > 50f) currentCanvasH else 320f
            val whiteKeyW = currentCanvasW / whiteKeyCount.toFloat()

            val speedMultiplier = when (fallingSpeedSetting) {
                "Slow" -> 0.75f
                "Fast" -> 1.5f
                "Extreme" -> 2.2f
                else -> 1.0f
            }
            val stepY = 6f * speedMultiplier

            val iterator = fallingBlocks.listIterator()
            while (iterator.hasNext()) {
                val block = iterator.next()
                if (block.isAscending) {
                    // Manual keypress: rises upwards from bottom to top
                    block.currentY -= stepY
                    block.startY = block.currentY + 28f
                    block.alpha = (block.currentY / contactY).coerceIn(0f, 1f)
                    if (block.currentY < -40f || block.alpha <= 0.04f) {
                        iterator.remove()
                    }
                } else {
                    block.startY += stepY
                    block.currentY += stepY

                    // Contact Line Hit Detection when ribbon reaches bottom contact line right on top of keyboard
                    if (block.currentY in (contactY - 14f)..(contactY + 10f)) {
                        val wIdx = getWhiteKeyIndexForMidi(block.midiNote)
                        val isBlack = isMidiNoteBlack(block.midiNote)
                        val baseBlockW = if (isBlack) 0.62f * whiteKeyW else 0.88f * whiteKeyW
                        val xOff = (whiteKeyW - baseBlockW) / 2f
                        val strikeX = (wIdx * whiteKeyW) + xOff + baseBlockW / 2f
                        val strikeY = contactY

                        // 1. Trigger Contact-Line Light Flash & Radial Bloom with flashSize scaling
                        if (glowEffect && flashes.none { it.midiNote == block.midiNote && it.alpha > 0.4f }) {
                            val baseRad = if (isBlack) 28f else 38f
                            if (flashes.size < 24) {
                                flashes.add(
                                    PianoKeyFlash(
                                        midiNote = block.midiNote,
                                        centerX = strikeX,
                                        centerY = strikeY,
                                        color = block.color,
                                        velocity = 95,
                                        alpha = 0.95f * flashHaloIntensity.coerceIn(0.2f, 1.0f),
                                        radius = baseRad * flashSize.coerceIn(0.2f, 2.5f)
                                    )
                                )
                            }
                        }

                        // 2. Trigger Physics-based Sparks with Directional Velocity Arc erupting from piano keys
                        if (particlesEnabled && particles.size < 64) {
                            val sparkBase = if (isBlack) (particleCount * 0.5f).toInt() else (particleCount * 0.75f).toInt()
                            val count = sparkBase.coerceIn(2, 24)
                            val pColor = if (lockParticleColor) block.color else particleCustomColor
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

                    if (block.startY > contactY + 80f) {
                        iterator.remove()
                    }
                }
            }

            // Update Particle Physics (Velocity + Gravity + Decay)
            val pIter = particles.listIterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.x += p.vx
                p.y += p.vy
                p.vy += gravity // Downward gravity
                p.vx *= 0.985f  // Air resistance
                p.currentLife -= 1f
                p.alpha = (p.currentLife / p.maxLife).coerceIn(0f, 1f)
                if (p.currentLife <= 0f || p.alpha <= 0f) {
                    pIter.remove()
                }
            }

            // Update Key Strike Flashes (Exponential Bloom Decay)
            val fIter = flashes.listIterator()
            while (fIter.hasNext()) {
                val f = fIter.next()
                f.radius += 1.8f * flashSize.coerceIn(0.5f, 2.0f)
                f.alpha *= 0.88f
                if (f.alpha < 0.03f) {
                    fIter.remove()
                }
            }

            fsFrameTicker++
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val _fsTick = fsFrameTicker
        val whiteKeyWidthPx = size.width / whiteKeyCount.toFloat()
        val canvasH = size.height
        if (canvasWidthPx != size.width) canvasWidthPx = size.width
        if (canvasHeightPx != size.height) canvasHeightPx = size.height

        // Background Subtle Key Lane Grid Lines matching reference image
        val gridLineColor = Color(0xFF221510)
        for (i in 0..whiteKeyCount) {
            val gx = i * whiteKeyWidthPx
            drawLine(
                color = gridLineColor,
                start = Offset(gx, 0f),
                end = Offset(gx, canvasH),
                strokeWidth = 1f
            )
        }
        val gridHorizColor = Color(0xFF1C1310)
        val hStep = canvasH / 4f
        for (j in 1..3) {
            val gy = j * hStep
            drawLine(
                color = gridHorizColor,
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1f
            )
        }

        // 1. Draw Waterdrop Note Pills
        for (block in fallingBlocks) {
            val isBlack = isMidiNoteBlack(block.midiNote)
            val wIdx = getWhiteKeyIndexForMidi(block.midiNote)
            val baseBlockWidth = if (isBlack) 0.58f * whiteKeyWidthPx else 0.82f * whiteKeyWidthPx
            val xOffset = (whiteKeyWidthPx - baseBlockWidth) / 2f
            val finalX = (wIdx * whiteKeyWidthPx) + xOffset
            val blockAlpha = block.alpha.coerceIn(0f, 1f)
            val bottomY = if (block.isAscending) (block.currentY + 28f).coerceAtMost(canvasH) else block.currentY.coerceAtMost(canvasH)
            val topY = if (block.isAscending) block.currentY.coerceAtLeast(0f) else block.startY.coerceAtMost(canvasH)
            val blockHeight = (bottomY - topY).coerceAtLeast(18f)
            val yRatioBottom = (bottomY / canvasH).coerceIn(0f, 1f)
            val yRatioTop = (topY / canvasH).coerceIn(0f, 1f)

            val scaleBottom = if (is3DPerspective) horizonScale + (1.0f - horizonScale) * yRatioBottom else 1.0f
            val scaleTop = if (is3DPerspective) horizonScale + (1.0f - horizonScale) * yRatioTop else 1.0f

            val centerX = finalX + baseBlockWidth / 2f
            val screenCenterX = size.width / 2f

            val projectedCenterBottomX = if (is3DPerspective) screenCenterX + (centerX - screenCenterX) * scaleBottom else centerX
            val projectedCenterTopX = if (is3DPerspective) screenCenterX + (centerX - screenCenterX) * scaleTop else centerX

            val halfWidthBottom = (baseBlockWidth * scaleBottom) / 2f
            val halfWidthTop = (baseBlockWidth * scaleTop) / 2f

            val pillCorner = 14f * (if (is3DPerspective) scaleBottom else 1.0f)

            if (is3DPerspective) {
                // 3D Perspective Trapezoid Ribbon / Pill
                val ribbonPath = Path().apply {
                    moveTo(projectedCenterTopX - halfWidthTop, topY)
                    lineTo(projectedCenterTopX + halfWidthTop, topY)
                    lineTo(projectedCenterBottomX + halfWidthBottom, bottomY)
                    lineTo(projectedCenterBottomX - halfWidthBottom, bottomY)
                    close()
                }

                if (glowEffect) {
                    val bloomPath = Path().apply {
                        moveTo(projectedCenterTopX - halfWidthTop - 5f, topY - 2f)
                        lineTo(projectedCenterTopX + halfWidthTop + 5f, topY - 2f)
                        lineTo(projectedCenterBottomX + halfWidthBottom + 5f, bottomY + 3f)
                        lineTo(projectedCenterBottomX - halfWidthBottom - 5f, bottomY + 3f)
                        close()
                    }
                    drawPath(
                        path = bloomPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                block.color.copy(alpha = 0.08f * blockAlpha),
                                block.color.copy(alpha = 0.30f * blockAlpha),
                                block.color.copy(alpha = 0.55f * blockAlpha)
                            ),
                            startY = topY,
                            endY = bottomY
                        )
                    )
                }

                // Core Waterdrop Fill with Rich Amber / Copper Gradient
                drawPath(
                    path = ribbonPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            block.color.copy(alpha = 0.82f * blockAlpha),
                            block.color.copy(alpha = 0.95f * blockAlpha),
                            block.color.copy(alpha = 1.0f * blockAlpha)
                        ),
                        startY = topY,
                        endY = bottomY
                    )
                )

                // Subtle Top & Side 3D Sheen Highlight
                val topSheenPath = Path().apply {
                    val sheenW = halfWidthBottom * 0.70f
                    moveTo(projectedCenterBottomX - sheenW, bottomY - blockHeight * 0.35f)
                    lineTo(projectedCenterBottomX + sheenW, bottomY - blockHeight * 0.35f)
                    lineTo(projectedCenterBottomX + sheenW * 0.85f, bottomY - 3f)
                    lineTo(projectedCenterBottomX - sheenW * 0.85f, bottomY - 3f)
                    close()
                }
                drawPath(
                    path = topSheenPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f * blockAlpha),
                            Color.Transparent
                        )
                    )
                )

                // Delicate Border Stroke for Definition
                drawPath(
                    path = ribbonPath,
                    color = Color.White.copy(alpha = 0.25f * blockAlpha),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
                )
            } else {
                // 2D Waterdrop Rounded Pill Mode matching reference screenshot
                val noteRectTop = topY
                val noteRectHeight = blockHeight
                val noteRectLeft = finalX
                val noteRectWidth = baseBlockWidth

                // Soft Ambient Glow
                if (glowEffect) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                block.color.copy(alpha = 0.45f * blockAlpha),
                                block.color.copy(alpha = 0.15f * blockAlpha),
                                Color.Transparent
                            ),
                            center = Offset(noteRectLeft + noteRectWidth / 2f, noteRectTop + noteRectHeight / 2f),
                            radius = noteRectWidth * 1.3f
                        ),
                        topLeft = Offset(noteRectLeft - 4f, noteRectTop - 4f),
                        size = Size(noteRectWidth + 8f, noteRectHeight + 8f),
                        cornerRadius = CornerRadius(pillCorner + 4f, pillCorner + 4f)
                    )
                }

                // Core Solid Pill Body
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            block.color.copy(alpha = 0.90f * blockAlpha),
                            block.color.copy(alpha = 1.0f * blockAlpha),
                            block.color.copy(alpha = 0.92f * blockAlpha)
                        ),
                        startY = noteRectTop,
                        endY = noteRectTop + noteRectHeight
                    ),
                    topLeft = Offset(noteRectLeft, noteRectTop),
                    size = Size(noteRectWidth, noteRectHeight),
                    cornerRadius = CornerRadius(pillCorner, pillCorner)
                )

                // Top Gloss Reflection (Waterdrop glassy shine)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f * blockAlpha),
                            Color.White.copy(alpha = 0.05f * blockAlpha),
                            Color.Transparent
                        ),
                        startY = noteRectTop,
                        endY = noteRectTop + (noteRectHeight * 0.55f)
                    ),
                    topLeft = Offset(noteRectLeft + 1.5f, noteRectTop + 1.5f),
                    size = Size(noteRectWidth - 3f, (noteRectHeight * 0.55f)),
                    cornerRadius = CornerRadius(pillCorner - 1f, pillCorner - 1f)
                )

                // Subtle Crisp Border
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.28f * blockAlpha),
                    topLeft = Offset(noteRectLeft, noteRectTop),
                    size = Size(noteRectWidth, noteRectHeight),
                    cornerRadius = CornerRadius(pillCorner, pillCorner),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
                )
            }

            // Clean Centered Pitch Letter (e.g. C, C#, D, E, F#, G, A, B)
            val notePitch = getPitchOnlyFromMidi(block.midiNote)
            val labelX = if (is3DPerspective) projectedCenterBottomX else finalX + baseBlockWidth / 2f
            val centerY = (topY + bottomY) / 2f
            val labelY = centerY + 3.8.dp.toPx()
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = (255 * blockAlpha).toInt().coerceIn(0, 255)
                textSize = (if (is3DPerspective) 9.5.dp.toPx() * scaleBottom else 10.dp.toPx()).coerceAtLeast(8.dp.toPx())
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb((180 * blockAlpha).toInt(), 0, 0, 0))
            }
            drawContext.canvas.nativeCanvas.drawText(
                notePitch,
                labelX,
                labelY,
                textPaint
            )
        }

        // 1b. MIDI Score Lookahead Falling Notes: streams down from top and reaches contactY right when active
        if (isPlaying && score != null) {
            val speedMul = when (fallingSpeedSetting) {
                "Slow" -> 0.75f
                "Fast" -> 1.5f
                "Extreme" -> 2.2f
                else -> 1.0f
            }
            val pixelsPerBeat = (canvasH / 3.0f) * speedMul
            val contactY = canvasH

            for (mIdx in playheadMeasure..(playheadMeasure + 2)) {
                val measure = score.measures.getOrNull(mIdx) ?: continue
                val beatsInMeasure = measure.timeSignatureNumerator.toFloat().coerceAtLeast(1f)
                val measureBeatStart = (mIdx - playheadMeasure) * beatsInMeasure - (playheadOffset * beatsInMeasure)

                for (note in measure.notes) {
                    if (note.isRest || note.midiNote !in 21..108) continue
                    val noteDistBeats = measureBeatStart + note.position
                    val noteDurBeats = note.duration.coerceAtLeast(0.2f)

                    // Note arrives at contact line (contactY) right when noteDistBeats == 0
                    val bottomY = contactY - (noteDistBeats * pixelsPerBeat)
                    val topY = bottomY - (noteDurBeats * pixelsPerBeat)

                    // Visible range check
                    if (bottomY < 0f || topY > canvasH) continue

                    val isBlack = isMidiNoteBlack(note.midiNote)
                    val wIdx = getWhiteKeyIndexForMidi(note.midiNote)
                    val baseBlockWidth = if (isBlack) 0.58f * whiteKeyWidthPx else 0.82f * whiteKeyWidthPx
                    val xOffset = (whiteKeyWidthPx - baseBlockWidth) / 2f
                    val finalX = (wIdx * whiteKeyWidthPx) + xOffset
                    val clampedBottomY = bottomY.coerceAtMost(canvasH)
                    val clampedTopY = topY.coerceAtLeast(0f)
                    val blockHeight = (clampedBottomY - clampedTopY).coerceAtLeast(18f)

                    val yRatioBottom = (clampedBottomY / canvasH).coerceIn(0f, 1f)
                    val yRatioTop = (clampedTopY / canvasH).coerceIn(0f, 1f)
                    val scaleBottom = if (is3DPerspective) horizonScale + (1.0f - horizonScale) * yRatioBottom else 1.0f
                    val scaleTop = if (is3DPerspective) horizonScale + (1.0f - horizonScale) * yRatioTop else 1.0f

                    val centerX = finalX + baseBlockWidth / 2f
                    val screenCenterX = size.width / 2f
                    val projectedCenterBottomX = if (is3DPerspective) screenCenterX + (centerX - screenCenterX) * scaleBottom else centerX
                    val projectedCenterTopX = if (is3DPerspective) screenCenterX + (centerX - screenCenterX) * scaleTop else centerX
                    val halfWidthBottom = (baseBlockWidth * scaleBottom) / 2f
                    val halfWidthTop = (baseBlockWidth * scaleTop) / 2f
                    val pillCorner = 14f * (if (is3DPerspective) scaleBottom else 1.0f)
                    val noteColor = if (note.midiNote >= 60) rightHandColor else leftHandColor

                    if (is3DPerspective) {
                        val ribbonPath = Path().apply {
                            moveTo(projectedCenterTopX - halfWidthTop, clampedTopY)
                            lineTo(projectedCenterTopX + halfWidthTop, clampedTopY)
                            lineTo(projectedCenterBottomX + halfWidthBottom, clampedBottomY)
                            lineTo(projectedCenterBottomX - halfWidthBottom, clampedBottomY)
                            close()
                        }

                        if (glowEffect) {
                            val bloomPath = Path().apply {
                                moveTo(projectedCenterTopX - halfWidthTop - 5f, clampedTopY - 2f)
                                lineTo(projectedCenterTopX + halfWidthTop + 5f, clampedTopY - 2f)
                                lineTo(projectedCenterBottomX + halfWidthBottom + 5f, clampedBottomY + 3f)
                                lineTo(projectedCenterBottomX - halfWidthBottom - 5f, clampedBottomY + 3f)
                                close()
                            }
                            drawPath(
                                path = bloomPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        noteColor.copy(alpha = 0.08f),
                                        noteColor.copy(alpha = 0.30f),
                                        noteColor.copy(alpha = 0.55f)
                                    ),
                                    startY = clampedTopY,
                                    endY = clampedBottomY
                                )
                            )
                        }

                        // Core Waterdrop Fill
                        drawPath(
                            path = ribbonPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    noteColor.copy(alpha = 0.82f),
                                    noteColor.copy(alpha = 0.95f),
                                    noteColor.copy(alpha = 1.0f)
                                ),
                                startY = clampedTopY,
                                endY = clampedBottomY
                            )
                        )

                        // Subtle Top Sheen Highlight
                        val topSheenPath = Path().apply {
                            val sheenW = halfWidthBottom * 0.70f
                            moveTo(projectedCenterBottomX - sheenW, clampedBottomY - blockHeight * 0.35f)
                            lineTo(projectedCenterBottomX + sheenW, clampedBottomY - blockHeight * 0.35f)
                            lineTo(projectedCenterBottomX + sheenW * 0.85f, clampedBottomY - 3f)
                            lineTo(projectedCenterBottomX - sheenW * 0.85f, clampedBottomY - 3f)
                            close()
                        }
                        drawPath(
                            path = topSheenPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Delicate Border Stroke
                        drawPath(
                            path = ribbonPath,
                            color = Color.White.copy(alpha = 0.25f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
                        )
                    } else {
                        // 2D Waterdrop Rounded Pill
                        if (glowEffect) {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        noteColor.copy(alpha = 0.45f),
                                        noteColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    ),
                                    center = Offset(finalX + baseBlockWidth / 2f, clampedTopY + blockHeight / 2f),
                                    radius = baseBlockWidth * 1.3f
                                ),
                                topLeft = Offset(finalX - 4f, clampedTopY - 4f),
                                size = Size(baseBlockWidth + 8f, blockHeight + 8f),
                                cornerRadius = CornerRadius(pillCorner + 4f, pillCorner + 4f)
                            )
                        }

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    noteColor.copy(alpha = 0.90f),
                                    noteColor.copy(alpha = 1.0f),
                                    noteColor.copy(alpha = 0.92f)
                                ),
                                startY = clampedTopY,
                                endY = clampedBottomY
                            ),
                            topLeft = Offset(finalX, clampedTopY),
                            size = Size(baseBlockWidth, blockHeight),
                            cornerRadius = CornerRadius(pillCorner, pillCorner)
                        )

                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.28f),
                            topLeft = Offset(finalX, clampedTopY),
                            size = Size(baseBlockWidth, blockHeight),
                            cornerRadius = CornerRadius(pillCorner, pillCorner),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
                        )
                    }

                    // Pitch Letter
                    val notePitch = getPitchOnlyFromMidi(note.midiNote)
                    val labelX = if (is3DPerspective) projectedCenterBottomX else finalX + baseBlockWidth / 2f
                    val centerY = (clampedTopY + clampedBottomY) / 2f
                    val labelY = centerY + 3.8.dp.toPx()
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = (if (is3DPerspective) 9.5.dp.toPx() * scaleBottom else 10.dp.toPx()).coerceAtLeast(8.dp.toPx())
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                        setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        notePitch,
                        labelX,
                        labelY,
                        textPaint
                    )
                }
            }
        }

        // 2. Draw Key Landing Warm Glow / Flare at bottom contact line
        if (glowEffect) {
            for (f in flashes) {
                val flashRadius = f.radius * (size.width / 1200f).coerceIn(0.8f, 1.6f)
                // Warm contact flare at the piano key landing edge
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFCC80).copy(alpha = (f.alpha * 0.95f).coerceIn(0f, 1f)),
                            f.color.copy(alpha = (f.alpha * 0.75f).coerceIn(0f, 1f)),
                            f.color.copy(alpha = (f.alpha * 0.20f).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(f.centerX, canvasH),
                        radius = flashRadius
                    ),
                    topLeft = Offset(f.centerX - flashRadius * 1.4f, canvasH - flashRadius * 0.45f),
                    size = Size(flashRadius * 2.8f, flashRadius * 0.9f)
                )
            }
        }

        // 3. Draw Physics Sparks with Velocity Trail and Decay erupting from piano keys
        if (particlesEnabled) {
            for (p in particles) {
                val pAlpha = p.alpha.coerceIn(0f, 1f)
                val sparkRadius = p.size.dp.toPx()

                // Spark Core
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha),
                    radius = sparkRadius * 0.55f,
                    center = Offset(p.x, p.y)
                )
                // Spark Outer Color Flare
                drawCircle(
                    color = p.color.copy(alpha = pAlpha * 0.75f),
                    radius = sparkRadius,
                    center = Offset(p.x, p.y)
                )
                // Spark Velocity Trail
                drawLine(
                    color = p.color.copy(alpha = pAlpha * 0.6f),
                    start = Offset(p.x, p.y),
                    end = Offset(p.x - p.vx * 1.5f, p.y - p.vy * 1.5f),
                    strokeWidth = sparkRadius * 0.75f
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