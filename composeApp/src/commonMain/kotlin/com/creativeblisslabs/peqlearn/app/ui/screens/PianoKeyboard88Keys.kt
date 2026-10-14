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

LearnSectionCard(title = "Notation & Piano Viewwe", icon = Icons.Filled.List) {
        Column(
        ) {
            // Top Toolbar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            
                IconButton(
                    onClick = { viewModel.isFullscreenKeyboard.value = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // NOTATION VIEW ON TOP (Takes available weighted space)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.25f),
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
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (keyboardMode == PianoKeyboardMode.FULL_88) {
                            IconButton(
                                onClick = { scope.launch { scrollState.animateScrollTo(0) } },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("A0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
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
                                Text("C8", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            }
                        }

                        IconButton(
                            onClick = { showWaterfall = !showWaterfall },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (showWaterfall) Icons.Default.WaterDrop else Icons.Default.VerticalAlignBottom,
                                contentDescription = "Waterfall",
                                tint = if (showWaterfall) primaryColor else Color.White.copy(alpha = 0.7f),
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
                                tint = Color.White.copy(alpha = 0.7f),
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
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showWaterfall) {
                    SynthesiaWaterfallOverlay(
                        activeMidiNotes = activeMidiNotes + pressedKeys + activeNotesSnapshot.value,
                        primaryColor = primaryColor
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(0.3f))
                ) {
                    Canvas(modifier = Modifier.width(keyboardWidth).height(80.dp).background(Color.Black.copy(0.2f))) {
                        val finalWhiteKeyWidthPx = size.width / whiteNotes.size.coerceAtLeast(1)
                        fallingBlocks.forEach { block ->
                            val isBlack = isMidiNoteBlack(block.midiNote)
                            val wIdx = getWhiteKeyIndexForMidi(block.midiNote) - getWhiteKeyIndexForMidi(startMidi)
                            val blockWidth = if (isBlack) finalWhiteKeyWidthPx * 0.60f else finalWhiteKeyWidthPx * 0.85f
                            val xOffset = if (isBlack) (finalWhiteKeyWidthPx - blockWidth) / 2f else 2f
                            val finalX = (wIdx * finalWhiteKeyWidthPx) + xOffset

                            drawRoundRect(
                                color = block.color.copy(alpha = 0.85f),
                                topLeft = Offset(finalX, block.currentY),
                                size = Size(blockWidth, 20f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
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
                            try {
                                viewModel.audioEngine.noteOn(midiNote, 0.85f)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .width(keyboardWidth)
                            .height(whiteKeyHeight)
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
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
                                    try {
                                        viewModel.audioEngine.noteOn(midi, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.width(fsKeyboardWidth).height(160.dp)
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
                            color = Color(0xEE121212),
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
                                    Text(text = currentTimeStr, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
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
                                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                    Text(text = totalTimeStr, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
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
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        IconButton(onClick = { viewModel.resetScorePlayhead() }) {
                                            Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }

                                        Text(
                                            text = "Speed: 1.0x",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = "BPM: ${currentScore?.tempo ?: 120}",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )

                                        IconButton(
                                            onClick = { viewModel.showSettings.value = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Settings",
                                                tint = Color.White.copy(alpha = 0.8f),
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
                                                containerColor = if (fsShowLetters) primaryColor else Color.White.copy(0.1f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Letters", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { fsShowLyrics = !fsShowLyrics },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (fsShowLyrics) primaryColor else Color.White.copy(0.1f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Lyrics", fontSize = 10.sp, color = Color.White)
                                        }

                                        IconButton(onClick = { viewModel.isFullscreenKeyboard.value = false }) {
                                            Icon(Icons.Default.FullscreenExit, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(22.dp))
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
        containerColor = Color(0xFF151515),
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
                        Text("Notation Format", color = Color.White.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        val formats = listOf("Staff", "Tab", "Combined")
                        Row(modifier = Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(8.dp)).padding(3.dp)) {
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
                                    Text(f, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) Color.Black else Color.White)
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Split Staff (Piano View)", color = Color.White.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = viewModel.isSplitStaff.value,
                            onCheckedChange = { viewModel.isSplitStaff.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vertical Scrolling", color = Color.White.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = viewModel.isVerticalScroll.value,
                            onCheckedChange = { viewModel.isVerticalScroll.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Note Names", color = Color.White.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
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
                            .background(Color.White.copy(0.05f))
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
                                    tint = if (isActive) Color(0xFFFFD700) else Color.White.copy(0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    item.title,
                                    color = if (isActive) Color(0xFFFFD700) else Color.White,
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
                            color = Color.White.copy(0.8f),
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
                        Text("Metronome Click", color = Color.White.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = viewModel.metronomeEnabled.value,
                            onCheckedChange = { viewModel.metronomeEnabled.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A-B Loop Active", color = Color.White.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
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
            .background(Color.White.copy(0.03f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Color(0xFF6C63FF), Color(0xFF4A90E2), Color(0xFFFF6B35), Color(0xFFFFD700), Color(0xFF4CAF50)).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(c)
                            .border(if (color == c) 2.dp else 0.dp, Color.White, RoundedCornerShape(6.dp))
                            .clickable { onColorChange(c) }
                    )
                }
            }
        }

        IconButton(onClick = { onMuteToggle(!isMuted) }) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = null,
                tint = if (isMuted) Color.Red.copy(0.7f) else Color.White.copy(0.6f)
            )
        }

        IconButton(onClick = { onHideToggle(!isHidden) }) {
            Icon(
                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = if (isHidden) Color.White.copy(0.2f) else Color(0xFFFFD700)
            )
        }
    }
}

@Composable
private fun StepperControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                text = if (value > 0) "+$value" else value.toString(),
                color = if (value != 0) Color(0xFFFFD700) else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onValueChange(value + 1) }) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
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