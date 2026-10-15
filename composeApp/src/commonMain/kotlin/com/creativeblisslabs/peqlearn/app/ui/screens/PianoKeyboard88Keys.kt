package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.List      
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.creativeblisslabs.peqlearn.app.ui.components.pEQOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.playMediaItem
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
    val startY: Float,
    currentY: Float,
    val color: Color
) {
    var currentY by mutableStateOf(currentY)
}

data class PianoWaterfallBlock(
    val midiNote: Int,
    var startY: Float = 0f,
    var currentY: Float = 24f,
    val color: Color
)

data class PianoSpark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    val color: Color
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
    var notationFormat by rememberSaveable { mutableStateOf("Staff Notation") }
    var showNoteLabels by remember { mutableStateOf(true) }
    var showWaterfall by remember { mutableStateOf(true) }
    var currentOctaveOffset by remember { mutableStateOf(0) }
    val pressedKeys = remember { mutableStateListOf<Int>() }

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
    val activeNotesSnapshot = remember {
        derivedStateOf { viewModel.midiActiveNotes.entries.filter { it.value }.map { it.key } }
    }

    LaunchedEffect(activeNotesSnapshot.value) {
        activeNotesSnapshot.value.forEach { note ->
            val color = if (internalKeyboardModeName == "DRUM") {
                if (isMidiNoteBlack(note)) Color(0xFFFFB300) else Color(0xFFFF6F00)
            } else {
                if (isMidiNoteBlack(note)) Color(0xFFFF6B35) else Color(0xFF6C63FF)
            }
            if (fallingBlocks.none { it.midiNote == note && it.currentY < 40f }) {
                fallingBlocks.add(PianoFallingNoteBlock(note, 0f, 0f, color))
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val iterator = fallingBlocks.iterator()
            while (iterator.hasNext()) {
                val block = iterator.next()
                block.currentY += 7f
                if (block.currentY > 420f) {
                    iterator.remove()
                }
            }
            delay(16)
        }
    }

LearnSectionCard(title = "Notation & Piano Viewer", icon = Icons.Filled.List) {
        Column(
        ) {
           

            // NOTATION VIEW ON TOP (Takes available weighted space)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                    showLetters = showNoteLabels,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(22.dp)) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                listOf("PIANO", "DRUM").forEach { mode ->
                                    val active = internalKeyboardModeName == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(if (active) primaryColor else Color.Transparent)
                                            .clickable { internalKeyboardModeName = mode }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            mode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        // Corner Connected Piano Name & Velocity Badge
                        PianoMidiStatusBadge(
                            deviceName = viewModel.connectedMidiDeviceName.value,
                            lastVelocity = viewModel.lastMidiVelocity.intValue,
                            lastChannel = viewModel.lastMidiChannel.intValue,
                            onDeviceClick = {
                                viewModel.onScanMidiDevicesRequested?.invoke()
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (keyboardMode == PianoKeyboardMode.FULL_88) {
                            IconButton(
                                onClick = { scope.launch { scrollState.animateScrollTo(0) } },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("A0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            IconButton(
                                onClick = {
                                    val middleCWhiteIdx = whiteNotes.indexOfFirst { it == 60 }
                                    if (middleCWhiteIdx >= 0) {
                                        scope.launch { scrollState.animateScrollTo((middleCWhiteIdx * 28).coerceAtLeast(0)) }
                                    }
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("C4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                            IconButton(
                                onClick = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("C8", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }

                        IconButton(
                            onClick = { showWaterfall = !showWaterfall },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (showWaterfall) Icons.Default.WaterDrop else Icons.Default.VerticalAlignBottom,
                                contentDescription = "Waterfall",
                                tint = if (showWaterfall) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { showNoteLabels = !showNoteLabels },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (showNoteLabels) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Labels",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextIdx = (keyboardMode.ordinal + 1) % PianoKeyboardMode.entries.size
                                keyboardMode = PianoKeyboardMode.entries[nextIdx]
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Mode",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
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
                            Canvas(modifier = Modifier.width(keyboardWidth).height(120.dp).background(Color.Black.copy(0.25f))) {
                                val availableWidth = size.width
                                val maxWhiteKeyWidthPx = with(density) { 42.dp.toPx() }
                                val calculatedKeyWidth = availableWidth / whiteNotes.size.coerceAtLeast(1)
                                val finalWhiteKeyWidthPx = if (calculatedKeyWidth > maxWhiteKeyWidthPx) maxWhiteKeyWidthPx else calculatedKeyWidth
                                val totalKeyboardWidthPx = finalWhiteKeyWidthPx * whiteNotes.size
                                val startPaddingOffset = (availableWidth - totalKeyboardWidthPx) / 2f
                                val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f

                                fallingBlocks.forEach { block ->
                                    val isBlack = isMidiNoteBlack(block.midiNote)
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
                                    val blockWidth = if (isBlack) blackKeyWidthPx else (finalWhiteKeyWidthPx - 2f)

                                    drawRoundRect(
                                        color = block.color.copy(alpha = 0.85f),
                                        topLeft = Offset(finalX, block.currentY),
                                        size = Size(blockWidth, 22f),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                        }
                        UnifiedPianoKeyboard(
                            activeMidiNotes = viewModel.midiActiveNotes,
                            accentColor = primaryColor,
                            showNoteNames = showNoteLabels,
                            isPlaying = viewModel.isScorePlaying.value,
                            startNote = startMidi,
                            endNote = startMidi + totalKeys - 1,
                            onKeyTapped = { midiNote ->
                                pressedKeys.add(midiNote)
                                onNotePressed(midiNote)
                                viewModel.onMidiNoteOn(midiNote, 95)
                                val freq = viewModel.tunerReferenceFrequency.value * 2.0.pow((midiNote - 69) / 12.0).toFloat()
                                viewModel.detectedPitch.value = freq
                                viewModel.detectedNote.value = getNoteNameFromMidi(midiNote)
                                scope.launch {
                                    delay(320L)
                                    viewModel.onMidiNoteOff(midiNote)
                                    pressedKeys.remove(midiNote)
                                }
                            },
                            modifier = Modifier
                                .width(keyboardWidth)
                                .height(whiteKeyHeight)
                                .padding(vertical = 4.dp)
                        )
                    }

                    // 3-Pedal Grand Piano Unit in top right corner above piano keyboard
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

    // Real-Time MIDI Connection & Disconnection Prompt Dialog
    viewModel.midiConnectionPrompt.value?.let { prompt ->
        MidiConnectionPromptDialog(
            prompt = prompt,
            onDismiss = { viewModel.dismissMidiPrompt() }
        )
    }

    if (viewModel.isFullscreenKeyboard.value) {
        Dialog(
            onDismissRequest = { viewModel.isFullscreenKeyboard.value = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DisposableEffect(Unit) {
                setScreenOrientation(pEQOrientation.LANDSCAPE)
                onDispose {
                    setScreenOrientation(pEQOrientation.USER)
                }
            }
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
                var fsZoomFactor by rememberSaveable { mutableStateOf(1.0f) }
                var panOffset by remember { mutableStateOf(Offset.Zero) }
                var areControlsVisible by remember { mutableStateOf(true) }
                var fsShowLetters by rememberSaveable { mutableStateOf(showNoteLabels) }
                var fsShowLyrics by rememberSaveable { mutableStateOf(true) }

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
                val fsKeyboardWidth = 32.dp * whiteKeyCount

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = fsZoomFactor, scaleY = fsZoomFactor, translationX = panOffset.x, translationY = panOffset.y)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black)
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
                                showLetters = fsShowLetters,
                                showLyrics = fsShowLyrics,
                                isSplitStaff = viewModel.isSplitStaff.value,
                                isVerticalScroll = viewModel.isVerticalScroll.value,
                                leftHandColor = viewModel.leftHandColor.value,
                                rightHandColor = viewModel.rightHandColor.value,
                                isLeftHandHidden = viewModel.isLeftHandHidden.value,
                                isRightHandHidden = viewModel.isRightHandHidden.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight().horizontalScroll(fsScrollState)
                            ) {
                                UnifiedPianoKeyboard(
                                    activeMidiNotes = viewModel.midiActiveNotes,
                                    accentColor = primaryColor,
                                    showNoteNames = fsShowLetters,
                                    isPlaying = isPlaying,
                                    startNote = 21,
                                    endNote = 108,
                                    onKeyTapped = { midi ->
                                        viewModel.onMidiNoteOn(midi, 95)
                                        scope.launch {
                                            delay(320L)
                                            viewModel.onMidiNoteOff(midi)
                                        }
                                    },
                                    modifier = Modifier.width(fsKeyboardWidth).height(160.dp)
                                )
                            }

                            // Fullscreen 3-Pedal Grand Piano Unit in top right corner above keyboard
                            PianoThreePedalUnit(
                                sustainPressed = viewModel.sustainPedalActive.value,
                                sostenutoPressed = viewModel.sostenutoPedalActive.value,
                                softPressed = viewModel.softPedalActive.value,
                                onPedalClick = { type, pressed ->
                                    viewModel.onMidiPedalEventReceived(type, pressed)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 12.dp, top = 6.dp)
                            )
                        }
                    }

                    // Top Toolbar (HUD)
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .widthIn(max = 800.dp)
                    ) {
                        Surface(
                                               color = MaterialTheme.colorScheme.surface
,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = currentTimeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
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
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = primaryColor,
                                            activeTrackColor = primaryColor,
                                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                        )
                                    )
                                    Text(text = totalTimeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(shape = RoundedCornerShape(50), color = primaryColor, modifier = Modifier.size(36.dp)) {
                                            IconButton(onClick = { viewModel.toggleScorePlayback() }) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        IconButton(onClick = { viewModel.resetScorePlayhead() }) {
                                            Icon(Icons.Default.Stop, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                        }

                                        Text(
                                            text = "Speed: 1.0x",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = "BPM: ${currentScore?.tempo ?: 120}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )

                                        IconButton(
                                            onClick = { viewModel.showSettings.value = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Settings",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { fsShowLetters = !fsShowLetters },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (fsShowLetters) primaryColor else MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Letters", fontSize = 10.sp, color = if (fsShowLetters) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                        Button(
                                            onClick = { fsShowLyrics = !fsShowLyrics },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (fsShowLyrics) primaryColor else MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Lyrics", fontSize = 10.sp, color = if (fsShowLyrics) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }

                                        IconButton(onClick = { viewModel.isFullscreenKeyboard.value = false }) {
                                            Icon(Icons.Default.FullscreenExit, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fullscreen Corner MIDI Device Status Badge
                    PianoMidiStatusBadge(
                        deviceName = viewModel.connectedMidiDeviceName.value,
                        lastVelocity = viewModel.lastMidiVelocity.intValue,
                        lastChannel = viewModel.lastMidiChannel.intValue,
                        onDeviceClick = {
                            viewModel.onScanMidiDevicesRequested?.invoke()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }

    if (viewModel.showSettings.value) {
        PlaybackSettingsSheet(
            viewModel = viewModel,
            notationFormat = notationFormat,
            onFormatChange = { notationFormat = it },
            showLetters = showNoteLabels,
            onShowLettersChange = { showNoteLabels = it },
            showLyrics = true,
            onShowLyricsChange = {},
            onDismiss = { viewModel.showSettings.value = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsSheet(
    viewModel: AppViewModel,
    notationFormat: String,
    onFormatChange: (String) -> Unit,
    showLetters: Boolean,
    onShowLettersChange: (Boolean) -> Unit,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playback & Notation Settings",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onDismiss() }
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

            // --- VISUAL CONTROLS ---
            SettingsSection(title = "Visual Controls") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notation Format", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        val formats = listOf("Staff", "Tab", "Combined")
                        Row(modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(8.dp)).padding(3.dp)) {
                            formats.forEach { f ->
                                val active = (f == "Staff" && notationFormat.contains("Staff")) ||
                                        (f == "Tab" && notationFormat.contains("Tab")) ||
                                        (f == "Combined" && notationFormat == "Combined")
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) Color(0xFFFFD700) else Color.Transparent)
                                        .clickable { onFormatChange(if (f == "Combined") "Combined" else "$f Notation") }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(f, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Split Staff (Piano View)", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
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
                            onCheckedChange = { viewModel.isVerticalScroll.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Note Names", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = showLetters,
                            onCheckedChange = onShowLettersChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                        )
                    }
                }
            }

            // --- HANDS & MUTING ---
            SettingsSection(title = "Hands & Muting") {
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

            // --- TRANSPOSE ---
            SettingsSection(title = "Transpose & Pitch") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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

            // --- TRACK LIST ---
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

            // --- SPEED & LOOP ---
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

@Composable
fun SynthesiaWaterfallOverlay(
    activeMidiNotes: Set<Int>,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    glowEffect: Boolean = true,
    particlesEnabled: Boolean = true
) {
    val fallingBlocks = remember { mutableStateListOf<PianoWaterfallBlock>() }
    val particles = remember { mutableStateListOf<PianoSpark>() }
    val whiteKeyCount = 52

    LaunchedEffect(activeMidiNotes) {
        for (midi in activeMidiNotes) {
            fallingBlocks.add(
                PianoWaterfallBlock(
                    midiNote = midi,
                    startY = 0f,
                    currentY = 24f,
                    color = if (isMidiNoteBlack(midi)) primaryColor.copy(alpha = 0.9f) else Color(0xFF64B5F6)
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            val iterator = fallingBlocks.listIterator()
            while (iterator.hasNext()) {
                val block = iterator.next()
                block.startY += 6f
                block.currentY += 6f

                if (block.currentY in 300f..312f && particlesEnabled) {
                    val wIdx = getWhiteKeyIndexForMidi(block.midiNote)
                    for (i in 0..3) {
                        val angle = (Random.nextFloat() * Math.PI).toFloat()
                        val speed = Random.nextFloat() * 4f + 2f
                        particles.add(
                            PianoSpark(
                                x = (wIdx + 0.5f) * (1200f / whiteKeyCount),
                                y = 300f,
                                vx = cos(angle) * speed,
                                vy = -sin(angle) * speed,
                                alpha = 1.0f,
                                color = block.color
                            )
                        )
                    }
                }

                if (block.startY > 400f) {
                    iterator.remove()
                }
            }

            val pIter = particles.listIterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.x += p.vx
                p.y += p.vy
                p.alpha -= 0.04f
                if (p.alpha <= 0f) {
                    pIter.remove()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(260.dp)) {
        val whiteKeyWidthPx = size.width / whiteKeyCount.toFloat()

        for (block in fallingBlocks) {
            val isBlack = isMidiNoteBlack(block.midiNote)
            val wIdx = getWhiteKeyIndexForMidi(block.midiNote)
            val blockWidth = if (isBlack) 0.6f * whiteKeyWidthPx else 0.85f * whiteKeyWidthPx
            val xOffset = if (isBlack) (whiteKeyWidthPx - blockWidth) / 2f else (whiteKeyWidthPx - blockWidth) / 2f
            val finalX = (wIdx * whiteKeyWidthPx) + xOffset
            val blockHeight = (block.currentY - block.startY).coerceAtLeast(24f)

            if (glowEffect) {
                drawRoundRect(
                    color = block.color.copy(alpha = 0.25f),
                    topLeft = Offset(finalX - 4f, block.startY - 4f),
                    size = Size(blockWidth + 8f, blockHeight + 8f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            drawRoundRect(
                color = block.color.copy(alpha = 0.85f),
                topLeft = Offset(finalX, block.startY),
                size = Size(blockWidth, blockHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        if (particlesEnabled) {
            for (p in particles) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                    radius = 2.5.dp.toPx(),
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

/**
 * High-fidelity 3-Pedal Grand Piano Unit:
 * 1. Soft / Una Corda Pedal (CC 67)
 * 2. Sostenuto Pedal (CC 66)
 * 3. Sustain / Damper Pedal (CC 64)
 *
 * Designed with brushed brass/gold physical pedal shapes, deep metallic housing,
 * mechanical depression translation when pressed, and radiant glowing highlights.
 */
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
            .padding(2.dp)
    ) {
        // Physical Brass Foot-pedal shape
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(44.dp)
                .graphicsLayer {
                    translationY = if (isPressed) 4.dp.toPx() else 0f
                    scaleY = if (isPressed) 0.92f else 1.0f
                }
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 9.dp, bottomEnd = 9.dp))
                .background(if (isPressed) brassGradientPressed else brassGradientNormal)
                .border(
                    width = if (isPressed) 1.5.dp else 1.dp,
                    color = if (isPressed) Color(0xFFFFE082) else Color(0x664A3508),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 9.dp, bottomEnd = 9.dp)
                )
        ) {
            // Shiny highlight bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp, top = 4.dp, bottom = 4.dp)
                    .background(Color.White.copy(alpha = if (isPressed) 0.6f else 0.3f), RoundedCornerShape(2.dp))
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

        Spacer(modifier = Modifier.height(3.dp))

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
                        text = if (isConnected) "🎹 $deviceName" else "🎹 No MIDI Piano",
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