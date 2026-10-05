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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.creativeblisslabs.peqlearn.app.ui.components.setScreenOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.pEQOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

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
    val whiteKeyHeight = 160.dp
    val blackKeyHeight = 100.dp
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

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.08f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
           Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(22.dp)) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            listOf("PIANO", "DRUM").forEach { mode ->
                                val active = internalKeyboardModeName == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(if (active) primaryColor else Color.Transparent)
                                        .clickable { internalKeyboardModeName = mode }
                                        .padding(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        mode,
                                        fontSize = 8.sp,
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
                                modifier = Modifier.size(28.dp)
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
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("C4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                            IconButton(
                                onClick = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("C8", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            }
                        }

                        IconButton(
                            onClick = { showWaterfall = !showWaterfall },
                            modifier = Modifier.size(28.dp)
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
                            modifier = Modifier.size(28.dp)
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
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Mode",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.isFullscreenKeyboard.value = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(0.3f))
            ) {
                Canvas(modifier = Modifier.width(keyboardWidth).height(120.dp).background(Color.Black.copy(0.2f))) {
                    val whiteKeyWidthPx = size.width / whiteKeyCount.toFloat()
                    fallingBlocks.forEach { block ->
                        val isBlack = isMidiNoteBlack(block.midiNote)
                        val wIdx = getWhiteKeyIndexForMidi(block.midiNote)

                        val blockWidth = if (isBlack) whiteKeyWidthPx * 0.60f else whiteKeyWidthPx - 4f
                        val xOffset = if (isBlack) (whiteKeyWidthPx - blockWidth) / 2f else 2f
                        val finalX = (wIdx * whiteKeyWidthPx) + xOffset

                        drawRoundRect(
                            color = block.color.copy(alpha = 0.85f),
                            topLeft = Offset(finalX, block.currentY),
                            size = Size(blockWidth, 24f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(keyboardWidth)
                        .height(whiteKeyHeight + 20.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Row {
                        whiteNotes.forEach { midiNote ->
                            val isPressed = pressedKeys.contains(midiNote) || viewModel.midiActiveNotes[midiNote] == true
                            val noteName = getNoteNameFromMidi(midiNote)
                            val isMiddleC = midiNote == 60

                            Box(
                                modifier = Modifier
                                    .width(whiteKeyWidth)
                                    .height(whiteKeyHeight)
                                    .padding(horizontal = 0.5.dp)
                                    .shadow(if (isPressed) 2.dp else 4.dp, RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                                    .background(
                                        brush = if (isPressed) {
                                            Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.8f), primaryColor))
                                        } else if (isMiddleC) {
                                            Brush.verticalGradient(listOf(Color(0xFFFFF9E6), Color(0xFFFFECC0)))
                                        } else {
                                            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE8ECEF)))
                                        },
                                        shape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)
                                    )
                                    .border(
                                        width = if (isMiddleC) 1.dp else 0.5.dp,
                                        color = if (isMiddleC) primaryColor.copy(alpha = 0.6f) else Color(0x33000000),
                                        shape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)
                                    )
                                    .pointerInput(midiNote) {
                                        detectTapGestures(
                                            onPress = {
                                                pressedKeys.add(midiNote)
                                                onNotePressed(midiNote)
                                                viewModel.onMidiNoteOn(midiNote, 95)
                                                val freq = viewModel.tunerReferenceFrequency.value * 2.0.pow((midiNote - 69) / 12.0).toFloat()
                                                viewModel.detectedPitch.value = freq
                                                viewModel.detectedNote.value = getNoteNameFromMidi(midiNote)

                                                try {
                                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                                } catch (_: Exception) {}

                                                tryAwaitRelease()
                                                pressedKeys.remove(midiNote)
                                                viewModel.onMidiNoteOff(midiNote)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (showNoteLabels) {
                                    Text(
                                        text = if (isMiddleC) "C4" else if (noteName.startsWith("C")) noteName else noteName.take(1),
                                        fontSize = if (isMiddleC) 10.sp else 9.sp,
                                        fontWeight = if (isMiddleC) FontWeight.Black else FontWeight.Medium,
                                        color = if (isPressed) Color.White else if (isMiddleC) primaryColor else Color(0xFF555555),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    whiteNotes.forEachIndexed { whiteIdx, whiteMidi ->
                        val potentialBlackMidi = whiteMidi + 1
                        if (allNotes.contains(potentialBlackMidi) && isMidiNoteBlack(potentialBlackMidi)) {
                            val isPressed = pressedKeys.contains(potentialBlackMidi) || viewModel.midiActiveNotes[potentialBlackMidi] == true
                            val leftOffset = (whiteIdx * 28 + 19).dp

                            Box(
                                modifier = Modifier
                                    .offset(x = leftOffset)
                                    .width(blackKeyWidth)
                                    .height(blackKeyHeight)
                                    .shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(
                                        brush = if (isPressed) {
                                            Brush.verticalGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.7f)))
                                        } else {
                                            Brush.verticalGradient(listOf(Color(0xFF2B2D33), Color(0xFF111215)))
                                        },
                                        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                                    )
                                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .pointerInput(potentialBlackMidi) {
                                        detectTapGestures(
                                            onPress = {
                                                pressedKeys.add(potentialBlackMidi)
                                                onNotePressed(potentialBlackMidi)
                                                viewModel.onMidiNoteOn(potentialBlackMidi, 95)
                                                val freq = viewModel.tunerReferenceFrequency.value * 2.0.pow((potentialBlackMidi - 69) / 12.0).toFloat()
                                                viewModel.detectedPitch.value = freq
                                                viewModel.detectedNote.value = getNoteNameFromMidi(potentialBlackMidi)

                                                try {
                                                    viewModel.audioEngine.noteOn(potentialBlackMidi, 0.85f)
                                                } catch (_: Exception) {}

                                                tryAwaitRelease()
                                                pressedKeys.remove(potentialBlackMidi)
                                                viewModel.onMidiNoteOff(potentialBlackMidi)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (showNoteLabels) {
                                    Text(
                                        text = getNoteNameFromMidi(potentialBlackMidi),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPressed) Color.White else Color(0xFFBBBBBB),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
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
                var fsKeyboardMode by rememberSaveable { mutableStateOf("PIANO") }
                var fsZoomFactor by rememberSaveable { mutableStateOf(1.0f) }
                var panOffset by remember { mutableStateOf(Offset.Zero) }
                var areControlsVisible by remember { mutableStateOf(true) }

                val isPlaying = viewModel.isScorePlaying.value
                val currentScore = viewModel.currentScore.value

                val totalMeasures = currentScore?.measures?.size ?: 0
                val timelineProgress = if (totalMeasures > 0) {
                    (viewModel.scorePlayheadMeasure.value.toFloat() + viewModel.scorePlayheadOffset.value) / totalMeasures.toFloat()
                } else 0f

                val secondsPerMeasure = 2.5f
                val totalSeconds = (totalMeasures * secondsPerMeasure).toInt()
                val currentSeconds = ((viewModel.scorePlayheadMeasure.value.toFloat() + viewModel.scorePlayheadOffset.value) * secondsPerMeasure).toInt()

                val currentMin = currentSeconds / 60
                val currentSec = currentSeconds % 60
                val currentTimeStr = "$currentMin:${if (currentSec < 10) "0$currentSec" else currentSec}"

                val totalMin = totalSeconds / 60
                val totalSec = totalSeconds % 60
                val totalTimeStr = "$totalMin:${if (totalSec < 10) "0$totalSec" else totalSec}"

                val fsScrollState = rememberScrollState()
                val fsKeyboardWidth = 32.dp * whiteKeyCount

                LaunchedEffect(areControlsVisible, activeNotesSnapshot.value.isNotEmpty()) {
                    if (areControlsVisible && activeNotesSnapshot.value.isNotEmpty()) {
                        delay(4500L)
                        areControlsVisible = false
                    }
                }

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
                                .height(270.dp)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFCFBF7))
                        ) {
                            StaffAndTabNotationViewer(
                                score = currentScore as Score?,
                                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                playheadOffset = viewModel.scorePlayheadOffset.value,
                                activeMidiNotes = viewModel.midiActiveNotes,
                                playingNote = viewModel.midiPlayingNote.value,
                                primaryColor = primaryColor,
                                currentFormat = notationFormat,
                                modifier = Modifier.fillMaxSize(),
                                onFormatChange = { newFormat -> notationFormat = newFormat }
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f).horizontalScroll(fsScrollState)
                        ) {
                            Canvas(modifier = Modifier.width(fsKeyboardWidth).weight(1f).background(Color.Black.copy(0.15f))) {
                                val whiteKeyWidthPx = size.width / whiteKeyCount.toFloat()
                                fallingBlocks.forEach { block ->
                                    val isBlack = isMidiNoteBlack(block.midiNote)
                                    val wIdx = getWhiteKeyIndexForMidi(block.midiNote)

                                    val blockWidth = if (isBlack) whiteKeyWidthPx * 0.60f else whiteKeyWidthPx - 4f
                                    val xOffset = if (isBlack) (whiteKeyWidthPx - blockWidth) / 2f else 2f
                                    val finalX = (wIdx * whiteKeyWidthPx) + xOffset

                                    drawRoundRect(
                                        color = block.color.copy(alpha = 0.85f),
                                        topLeft = Offset(finalX, block.currentY * 2.2f),
                                        size = Size(blockWidth, 32f),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }

                            UnifiedPianoKeyboard(
                                activeMidiNotes = viewModel.midiActiveNotes,
                                accentColor = if (fsKeyboardMode == "DRUM") Color(0xFFFF9800) else Color(0xFF6C63FF),
                                startNote = 21,
                                endNote = 108,
                                onKeyTapped = { note ->
                                    viewModel.midiActiveNotes[note] = true
                                    val freq = viewModel.tunerReferenceFrequency.value * 2.0.pow((note - 69) / 12.0).toFloat()
                                    viewModel.detectedPitch.value = freq
                                    viewModel.detectedNote.value = getNoteNameFromMidi(note)

                                    try {
                                        viewModel.audioEngine.noteOn(note, 0.85f)
                                    } catch (_: Exception) {}

                                    scope.launch {
                                        delay(300)
                                        viewModel.midiActiveNotes[note] = false
                                    }
                                },
                                modifier = Modifier.width(fsKeyboardWidth).height(150.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(400)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Surface(
                            color = Color(0xEE121212),
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .wrapContentHeight()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f, fill = false),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(shape = RoundedCornerShape(50), color = primaryColor, modifier = Modifier.size(34.dp)) {
                                            val beatsPerMeasure = viewModel.currentScore.value?.timeSignature?.split("/")?.firstOrNull()?.toFloatOrNull() ?: 4f

                                            IconButton(onClick = { viewModel.toggleScorePlayback(beatsPerMeasure = beatsPerMeasure) }) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.resetScorePlayhead() },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }

                                        VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 2.dp), color = Color.White.copy(0.15f))

                                        Surface(color = Color.White.copy(0.06f), shape = RoundedCornerShape(22.dp)) {
                                            Row(modifier = Modifier.padding(2.dp)) {
                                                listOf("PIANO" to "P", "DRUM" to "D").forEach { (fullName, codeName) ->
                                                    val active = fsKeyboardMode == fullName
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(22.dp))
                                                            .background(if (active) primaryColor else Color.Transparent)
                                                            .clickable { fsKeyboardMode = fullName }
                                                            .padding(horizontal = 10.dp, vertical = 0.dp)
                                                    ) {
                                                        Text(codeName, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (active) Color.White else Color.White.copy(0.5f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (viewModel.detectedNote.value.isNotEmpty()) {
                                            Surface(color = primaryColor.copy(0.15f), shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(end = 2.dp)) {
                                                Text(
                                                    text = viewModel.detectedNote.value,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = primaryColor
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                fsZoomFactor = (fsZoomFactor - 0.1f).coerceAtLeast(1.0f)
                                                if (fsZoomFactor <= 1.0f) panOffset = Offset.Zero
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ZoomOut, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = "${(fsZoomFactor * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.width(36.dp),
                                            textAlign = TextAlign.Center
                                        )

                                        IconButton(
                                            onClick = { fsZoomFactor = (fsZoomFactor + 0.1f).coerceAtMost(2.2f) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ZoomIn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 4.dp), color = Color.White.copy(0.15f))

                                        IconButton(
                                            onClick = { viewModel.isFullscreenKeyboard.value = false },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.FullscreenExit, "Minimize Keyboard", tint = Color(0xFFFF6B35), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().height(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = currentTimeStr, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = timelineProgress,
                                        onValueChange = {
                                            if (currentScore != null && totalMeasures > 0) {
                                                val totalProgressMeasures = totalMeasures.toFloat()
                                                val targetMeasure = (it * totalProgressMeasures).toInt().coerceIn(0, totalMeasures - 1)
                                                val targetOffset = (it * totalProgressMeasures) - targetMeasure
                                                viewModel.scorePlayheadMeasure.value = targetMeasure
                                                viewModel.scorePlayheadOffset.value = targetOffset
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = primaryColor,
                                            activeTrackColor = primaryColor,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                    Text(text = totalTimeStr, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isMidiNoteBlack(midiNote: Int): Boolean {
    val semitone = (midiNote - 12) % 12
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
                    for (i in 0..3) {
                        val angle = (Random.nextFloat() * Math.PI).toFloat()
                        val speed = Random.nextFloat() * 4f + 2f
                        particles.add(
                            PianoSpark(
                                x = (getWhiteKeyIndexForMidi(block.midiNote) / whiteKeyCount) * 1200f,
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
        val whiteKeyWidthPx = size.width / whiteKeyCount

        for (block in fallingBlocks) {
            val isBlack = isMidiNoteBlack(block.midiNote)
            val wIdx = getWhiteKeyIndexForMidi(block.midiNote)
            val blockWidth = if (isBlack) 0.6f * whiteKeyWidthPx else whiteKeyWidthPx - 4f
            val xOffset = if (isBlack) (whiteKeyWidthPx - blockWidth) / 2f else 2f
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