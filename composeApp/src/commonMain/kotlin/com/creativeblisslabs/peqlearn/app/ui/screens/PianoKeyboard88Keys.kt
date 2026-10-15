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
import com.creativeblisslabs.peqlearn.app.ui.components.pEQOrientation
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.playMediaItem
import com.creativeblisslabs.peqlearn.app.util.LiveChordDetector
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
    var notationFormat by rememberSaveable { mutableStateOf("Staff Notation") }
    val showNoteLabels = viewModel.showPianoKeyLabels.value
    val showWaterfall = viewModel.showWaterfall.value
    val is3DPerspectiveMode = viewModel.is3DPerspectiveMode.value
    val horizonScale = viewModel.horizonScale.floatValue
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

    LaunchedEffect(activeNotesSnapshot.value, viewModel.rightHandColor.value, viewModel.leftHandColor.value) {
        val rhColor = viewModel.rightHandColor.value
        val lhColor = viewModel.leftHandColor.value
        activeNotesSnapshot.value.forEach { note ->
            val color = when {
                internalKeyboardModeName == "DRUM" -> if (isMidiNoteBlack(note)) Color(0xFFFFB300) else Color(0xFFFF6F00)
                note >= 60 -> if (isMidiNoteBlack(note)) rhColor.copy(alpha = 0.95f) else rhColor
                else -> if (isMidiNoteBlack(note)) lhColor.copy(alpha = 0.95f) else lhColor
            }
            if (fallingBlocks.none { it.midiNote == note && it.currentY < 40f }) {
                fallingBlocks.add(PianoFallingNoteBlock(note, 0f, 0f, color))
            }
        }
    }

    LaunchedEffect(Unit) {
        val gravity = 0.28f
        while (true) {
            val speedMultiplier = when (viewModel.fallingNotesSpeed.value) {
                "Slow" -> 0.65f
                "Fast" -> 1.4f
                "Extreme" -> 2.0f
                else -> 1.0f
            }
            val stepY = 6.5f * speedMultiplier

            val iterator = fallingBlocks.iterator()
            while (iterator.hasNext()) {
                val block = iterator.next()
                block.currentY += stepY
                if (block.currentY > 140f) {
                    iterator.remove()
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
                f.radius += 1.6f
                f.alpha *= 0.88f
                if (f.alpha < 0.03f) {
                    fIter.remove()
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
                    
                    

                        // Corner Connected Piano Name & Velocity Badge
                        PianoMidiStatusBadge(
                            deviceName = viewModel.connectedMidiDeviceName.value,
                            lastVelocity = viewModel.lastMidiVelocity.intValue,
                            lastChannel = viewModel.lastMidiChannel.intValue,
                            onDeviceClick = {
                                viewModel.onScanMidiDevicesRequested?.invoke()
                            }
                        )
                    

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
                            onClick = { viewModel.showWaterfall.value = !viewModel.showWaterfall.value },
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
                            onClick = { viewModel.is3DPerspectiveMode.value = !viewModel.is3DPerspectiveMode.value },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "3D Mode",
                                tint = if (is3DPerspectiveMode) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.showPianoKeyLabels.value = !viewModel.showPianoKeyLabels.value },
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
                                val canvasH = size.height
                                val maxWhiteKeyWidthPx = with(density) { 42.dp.toPx() }
                                val calculatedKeyWidth = availableWidth / whiteNotes.size.coerceAtLeast(1)
                                val finalWhiteKeyWidthPx = if (calculatedKeyWidth > maxWhiteKeyWidthPx) maxWhiteKeyWidthPx else calculatedKeyWidth
                                val totalKeyboardWidthPx = finalWhiteKeyWidthPx * whiteNotes.size
                                val startPaddingOffset = (availableWidth - totalKeyboardWidthPx) / 2f
                                val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f

                                // 1. Falling Note Ribbons with 3D Perspective Projection & Bloom & Labels
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
                                    val baseBlockWidth = if (isBlack) blackKeyWidthPx else (finalWhiteKeyWidthPx - 2f)

                                    // Perspective depth calculation:
                                    // yRatio = 1.0 at keyboard contact line (canvasH), 0.0 at top horizon
                                    val bottomY = block.currentY
                                    val topY = (block.currentY - 24f).coerceAtLeast(0f)
                                    val yRatioBottom = (bottomY / canvasH).coerceIn(0f, 1f)
                                    val yRatioTop = (topY / canvasH).coerceIn(0f, 1f)

                                    val scaleBottom = if (is3DPerspectiveMode) horizonScale + (1.0f - horizonScale) * yRatioBottom else 1.0f
                                    val scaleTop = if (is3DPerspectiveMode) horizonScale + (1.0f - horizonScale) * yRatioTop else 1.0f

                                    val centerX = finalX + baseBlockWidth / 2f
                                    val screenCenterX = availableWidth / 2f

                                    val projectedCenterBottomX = if (is3DPerspectiveMode) screenCenterX + (centerX - screenCenterX) * scaleBottom else centerX
                                    val projectedCenterTopX = if (is3DPerspectiveMode) screenCenterX + (centerX - screenCenterX) * scaleTop else centerX

                                    val halfWidthBottom = (baseBlockWidth * scaleBottom) / 2f
                                    val halfWidthTop = (baseBlockWidth * scaleTop) / 2f

                                    // Contact line hit detection for inline sparks & flashes
                                    if (block.currentY in (canvasH - 8f)..(canvasH + 4f) && viewModel.showWaterfallParticles.value) {
                                        if (inlineFlashes.none { it.midiNote == block.midiNote && it.alpha > 0.4f }) {
                                            inlineFlashes.add(
                                                PianoKeyFlash(
                                                    midiNote = block.midiNote,
                                                    centerX = projectedCenterBottomX,
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
                                                        x = projectedCenterBottomX + (Random.nextFloat() - 0.5f) * 6f,
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

                                    if (is3DPerspectiveMode) {
                                        // 3D Perspective Trapezoid Ribbon
                                        val ribbonPath = Path().apply {
                                            moveTo(projectedCenterTopX - halfWidthTop, topY)
                                            lineTo(projectedCenterTopX + halfWidthTop, topY)
                                            lineTo(projectedCenterBottomX + halfWidthBottom, bottomY)
                                            lineTo(projectedCenterBottomX - halfWidthBottom, bottomY)
                                            close()
                                        }

                                        // Outer Glow Trapezoid
                                        val bloomPath = Path().apply {
                                            moveTo(projectedCenterTopX - halfWidthTop - 4f, topY)
                                            lineTo(projectedCenterTopX + halfWidthTop + 4f, topY)
                                            lineTo(projectedCenterBottomX + halfWidthBottom + 4f, bottomY + 2f)
                                            lineTo(projectedCenterBottomX - halfWidthBottom - 4f, bottomY + 2f)
                                            close()
                                        }

                                        drawPath(
                                            path = bloomPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    block.color.copy(alpha = 0.08f),
                                                    block.color.copy(alpha = 0.35f),
                                                    block.color.copy(alpha = 0.55f)
                                                ),
                                                startY = topY,
                                                endY = bottomY
                                            )
                                        )

                                        drawPath(
                                            path = ribbonPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    block.color.copy(alpha = 0.75f),
                                                    block.color.copy(alpha = 0.95f),
                                                    Color.White.copy(alpha = 0.85f)
                                                ),
                                                startY = topY,
                                                endY = bottomY
                                            )
                                        )
                                    } else {
                                        // 2D Orthographic Flat Mode
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    block.color.copy(alpha = 0.08f),
                                                    block.color.copy(alpha = 0.35f),
                                                    block.color.copy(alpha = 0.55f)
                                                ),
                                                startY = topY,
                                                endY = bottomY
                                            ),
                                            topLeft = Offset(finalX - 3f, topY),
                                            size = Size(baseBlockWidth + 6f, 26f),
                                            cornerRadius = CornerRadius(6f, 6f)
                                        )

                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    block.color.copy(alpha = 0.75f),
                                                    block.color.copy(alpha = 0.95f),
                                                    Color.White.copy(alpha = 0.85f)
                                                ),
                                                startY = topY,
                                                endY = bottomY
                                            ),
                                            topLeft = Offset(finalX, topY),
                                            size = Size(baseBlockWidth, 22f),
                                            cornerRadius = CornerRadius(4f, 4f)
                                        )
                                    }

                                    // Dynamic Note Pitch Label inside Block
                                    val noteLabel = getNoteNameFromMidi(block.midiNote)
                                    val labelX = if (is3DPerspectiveMode) projectedCenterBottomX else finalX + baseBlockWidth / 2f
                                    drawContext.canvas.nativeCanvas.drawText(
                                        noteLabel,
                                        labelX,
                                        block.currentY - 5f,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 8.5.dp.toPx()
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                            isAntiAlias = true
                                            setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
                                        }
                                    )
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

                    // Real-Time Polyphonic Chord Recognition HUD (MIDIVisualizer parity)
                    liveDetectedChord?.let { chord ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF14171E).copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF00E676))
                                )
                                Text(
                                    text = chord.fullName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = "• ${chord.inversionText}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
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
                            // Fullscreen Live Polyphonic Chord Recognition HUD
                            liveDetectedChord?.let { chord ->
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 16.dp, top = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF14171E).copy(alpha = 0.9f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF00E676))
                                        )
                                        Text(
                                            text = chord.fullName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFFD700)
                                        )
                                        Text(
                                            text = "• ${chord.inversionText}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // Fullscreen 3-Pedal Grand Piano Unit in top right corner above keyboard bed
                            PianoThreePedalUnit(
                                sustainPressed = viewModel.sustainPedalActive.value,
                                sostenutoPressed = viewModel.sostenutoPedalActive.value,
                                softPressed = viewModel.softPedalActive.value,
                                onPedalClick = { type, pressed ->
                                    viewModel.onMidiPedalEventReceived(type, pressed)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 16.dp, top = 6.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight().horizontalScroll(fsScrollState)
                            ) {
                                UnifiedPianoKeyboard(
                                    activeMidiNotes = viewModel.midiActiveNotes,
                                    accentColor = primaryColor,
                                    activeCoordinate = viewModel.midiPlayingNote.value,
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
            notationFormat = viewModel.notationFormat.value,
            onFormatChange = { viewModel.notationFormat.value = it },
            showLetters = viewModel.showPianoKeyLabels.value,
            onShowLettersChange = { viewModel.showPianoKeyLabels.value = it },
            is3DPerspective = viewModel.is3DPerspectiveMode.value,
            on3DPerspectiveChange = { viewModel.is3DPerspectiveMode.value = it },
            horizonScale = viewModel.horizonScale.floatValue,
            onHorizonScaleChange = { viewModel.horizonScale.floatValue = it },
            showLyrics = true,
            onShowLyricsChange = {},
            initialTab = 1,
            onDismiss = { viewModel.showSettings.value = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsSheet(
    viewModel: AppViewModel,
    notationFormat: String = viewModel.notationFormat.value,
    onFormatChange: (String) -> Unit = { viewModel.notationFormat.value = it },
    showLetters: Boolean = viewModel.showPianoKeyLabels.value,
    onShowLettersChange: (Boolean) -> Unit = { viewModel.showPianoKeyLabels.value = it },
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
                            val formats = listOf("Staff", "Tab", "Both Viewers")
                            Row(modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(8.dp)).padding(3.dp)) {
                                formats.forEach { f ->
                                    val currentFmt = viewModel.notationFormat.value
                                    val active = (f == "Staff" && (currentFmt == "Staff" || currentFmt == "Staff Only")) ||
                                            (f == "Tab" && (currentFmt == "Tab" || currentFmt == "Tab Only")) ||
                                            (f == "Both Viewers" && (currentFmt == "Both Viewers" || currentFmt == "Combined"))
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
                // TAB 1: KEYBOARD & VISUALIZER SETTINGS
                // =========================================================================
                SettingsSection(title = "3D Waterfall & Visual FX") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Note Names on Keys", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = showLetters,
                                onCheckedChange = onShowLettersChange,
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
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700)
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable Waterfall Sparks & Bloom", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.showWaterfallParticles.value,
                                onCheckedChange = { viewModel.showWaterfallParticles.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFFFFD700).copy(0.3f))
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Falling Notes Speed", color = MaterialTheme.colorScheme.onSurface.copy(0.8f), fontSize = 14.sp)
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

@Composable
fun SynthesiaWaterfallOverlay(
    activeMidiNotes: Set<Int>,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    glowEffect: Boolean = true,
    particlesEnabled: Boolean = true,
    is3DPerspective: Boolean = true,
    horizonScale: Float = 0.68f
) {
    val fallingBlocks = remember { mutableStateListOf<PianoWaterfallBlock>() }
    val particles = remember { mutableStateListOf<PianoSpark>() }
    val flashes = remember { mutableStateListOf<PianoKeyFlash>() }
    val whiteKeyCount = 52

    LaunchedEffect(activeMidiNotes) {
        for (midi in activeMidiNotes) {
            val color = if (isMidiNoteBlack(midi)) primaryColor.copy(alpha = 0.95f) else Color(0xFF64B5F6)
            fallingBlocks.add(
                PianoWaterfallBlock(
                    midiNote = midi,
                    startY = 0f,
                    currentY = 24f,
                    color = color
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        val gravity = 0.28f
        while (true) {
            delay(16L)
            val iterator = fallingBlocks.listIterator()
            while (iterator.hasNext()) {
                val block = iterator.next()
                block.startY += 6f
                block.currentY += 6f

                // Contact Line Hit Detection at Y = 260f
                if (block.currentY in 258f..272f) {
                    val wIdx = getWhiteKeyIndexForMidi(block.midiNote)
                    val strikeX = (wIdx + 0.5f) * (1200f / whiteKeyCount)
                    val strikeY = 260f

                    // 1. Trigger Contact-Line Light Flash & Radial Bloom
                    if (glowEffect && flashes.none { it.midiNote == block.midiNote && it.alpha > 0.4f }) {
                        flashes.add(
                            PianoKeyFlash(
                                midiNote = block.midiNote,
                                centerX = strikeX,
                                centerY = strikeY,
                                color = block.color,
                                velocity = 95,
                                alpha = 0.95f,
                                radius = if (isMidiNoteBlack(block.midiNote)) 28f else 38f
                            )
                        )
                    }

                    // 2. Trigger Physics-based Sparks with Directional Velocity Arc
                    if (particlesEnabled) {
                        val sparkCount = if (isMidiNoteBlack(block.midiNote)) 12 else 16
                        for (i in 0 until sparkCount) {
                            // Erupt upwards between -20 and -160 degrees
                            val angleRad = (-(30f + Random.nextFloat() * 120f) * (Math.PI / 180f)).toFloat()
                            val speed = Random.nextFloat() * 7.5f + 3.0f
                            val life = Random.nextFloat() * 22f + 18f
                            particles.add(
                                PianoSpark(
                                    x = strikeX + (Random.nextFloat() - 0.5f) * 8f,
                                    y = strikeY,
                                    vx = cos(angleRad) * speed,
                                    vy = sin(angleRad) * speed,
                                    alpha = 1.0f,
                                    size = Random.nextFloat() * 2.8f + 1.8f,
                                    maxLife = life,
                                    currentLife = life,
                                    color = block.color
                                )
                            )
                        }
                    }
                }

                if (block.startY > 360f) {
                    iterator.remove()
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
                f.radius += 1.8f
                f.alpha *= 0.88f
                if (f.alpha < 0.03f) {
                    fIter.remove()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(260.dp)) {
        val whiteKeyWidthPx = size.width / whiteKeyCount.toFloat()
        val canvasH = size.height

        // 1. Draw Falling Note Ribbons with 3D Perspective or 2D Glow
        for (block in fallingBlocks) {
            val isBlack = isMidiNoteBlack(block.midiNote)
            val wIdx = getWhiteKeyIndexForMidi(block.midiNote)
            val baseBlockWidth = if (isBlack) 0.62f * whiteKeyWidthPx else 0.88f * whiteKeyWidthPx
            val xOffset = (whiteKeyWidthPx - baseBlockWidth) / 2f
            val finalX = (wIdx * whiteKeyWidthPx) + xOffset
            val blockHeight = (block.currentY - block.startY).coerceAtLeast(24f)

            val bottomY = block.currentY
            val topY = block.startY
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

            if (is3DPerspective) {
                // 3D Perspective Trapezoid
                val ribbonPath = Path().apply {
                    moveTo(projectedCenterTopX - halfWidthTop, topY)
                    lineTo(projectedCenterTopX + halfWidthTop, topY)
                    lineTo(projectedCenterBottomX + halfWidthBottom, bottomY)
                    lineTo(projectedCenterBottomX - halfWidthBottom, bottomY)
                    close()
                }

                if (glowEffect) {
                    val bloomPath = Path().apply {
                        moveTo(projectedCenterTopX - halfWidthTop - 4f, topY)
                        lineTo(projectedCenterTopX + halfWidthTop + 4f, topY)
                        lineTo(projectedCenterBottomX + halfWidthBottom + 4f, bottomY + 2f)
                        lineTo(projectedCenterBottomX - halfWidthBottom - 4f, bottomY + 2f)
                        close()
                    }
                    drawPath(
                        path = bloomPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                block.color.copy(alpha = 0.05f),
                                block.color.copy(alpha = 0.35f),
                                block.color.copy(alpha = 0.55f)
                            ),
                            startY = topY,
                            endY = bottomY
                        )
                    )
                }

                drawPath(
                    path = ribbonPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            block.color.copy(alpha = 0.75f),
                            block.color.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.9f)
                        ),
                        startY = topY,
                        endY = bottomY
                    )
                )
            } else {
                if (glowEffect) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                block.color.copy(alpha = 0.05f),
                                block.color.copy(alpha = 0.35f),
                                block.color.copy(alpha = 0.55f)
                            ),
                            startY = block.startY - 6f,
                            endY = block.currentY + 6f
                        ),
                        topLeft = Offset(finalX - 6f, block.startY - 6f),
                        size = Size(baseBlockWidth + 12f, blockHeight + 12f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            block.color.copy(alpha = 0.75f),
                            block.color.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.9f)
                        ),
                        startY = block.startY,
                        endY = block.currentY
                    ),
                    topLeft = Offset(finalX, block.startY),
                    size = Size(baseBlockWidth, blockHeight),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }

            // Dynamic Pitch Label on Leading Edge of Note Block
            if (blockHeight >= 28f) {
                val noteLabel = getNoteNameFromMidi(block.midiNote)
                val labelX = if (is3DPerspective) projectedCenterBottomX else finalX + baseBlockWidth / 2f
                drawContext.canvas.nativeCanvas.drawText(
                    noteLabel,
                    labelX,
                    block.currentY - 6f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                        setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
                    }
                )
            }
        }

        // 2. Draw Contact-Line Radial Bloom / Key Flashes
        if (glowEffect) {
            for (f in flashes) {
                val scaledX = (f.centerX / 1200f) * size.width
                val flashRadius = f.radius * (size.width / 1200f).coerceIn(0.8f, 1.6f)
                // Outer radial bloom glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (f.alpha * 0.95f).coerceIn(0f, 1f)),
                            f.color.copy(alpha = (f.alpha * 0.7f).coerceIn(0f, 1f)),
                            f.color.copy(alpha = (f.alpha * 0.25f).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(scaledX, canvasH),
                        radius = flashRadius
                    ),
                    radius = flashRadius,
                    center = Offset(scaledX, canvasH)
                )
            }
        }

        // 3. Draw Physics Sparks with Velocity Trail and Decay
        if (particlesEnabled) {
            for (p in particles) {
                val scaledX = (p.x / 1200f) * size.width
                val pAlpha = p.alpha.coerceIn(0f, 1f)
                val sparkRadius = p.size.dp.toPx()

                // Spark Core
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha),
                    radius = sparkRadius * 0.55f,
                    center = Offset(scaledX, p.y)
                )
                // Spark Outer Color Flare
                drawCircle(
                    color = p.color.copy(alpha = pAlpha * 0.75f),
                    radius = sparkRadius,
                    center = Offset(scaledX, p.y)
                )
                // Spark Velocity Trail
                drawLine(
                    color = p.color.copy(alpha = pAlpha * 0.6f),
                    start = Offset(scaledX, p.y),
                    end = Offset(scaledX - p.vx * 1.5f, p.y - p.vy * 1.5f),
                    strokeWidth = sparkRadius * 0.75f
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
            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                .width(24.dp)
                .height(46.dp)
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