package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.screens.PianoKeyboard88Keys
import com.creativeblisslabs.peqlearn.app.util.GuitarShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
fun UnifiedGuitarFretboard(
    numFrets: Int,
    activeNotes: List<Int> = emptyList(),
    rootNote: Int = 0,
    accentColor: Color,
    showNoteNames: Boolean = false,
    chordFrets: List<Int>? = null,
    chordFingers: List<Int>? = null,
    baseFret: Int = 1,
    barreFret: Int? = null,
    stringLabels: List<String> = listOf("e", "B", "G", "D", "A", "E"),
    openStringsSemis: List<Int> = listOf(4, 11, 7, 2, 9, 4),
    activeCoordinate: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier
) {
    val numStrings = stringLabels.size
    val isDark = isSystemInDarkTheme()

    val normalizedActiveNotes = remember(activeNotes) { activeNotes.map { it % 12 } }
    val normalizedRootNote = remember(rootNote) { rootNote % 12 }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad  = 54f
        val topPad   = 28f
        val rightPad = 24f
        val fretW    = (w - leftPad - rightPad) / numFrets
        val stringH  = (h - topPad * 2) / (numStrings - 1).coerceAtLeast(1).toFloat()
        val dotSizePx = 24f

        // Wood Background
        val woodColors = if (isDark) {
            listOf(Color(0xFF1E1610), Color(0xFF2D221A), Color(0xFF1E1610))
        } else {
            listOf(Color(0xFF5C3317), Color(0xFF704214), Color(0xFF5C3317))
        }
        drawRect(brush = Brush.linearGradient(colors = woodColors, start = Offset(0f, 0f), end = Offset(w, h)))

        // Inlays
        val inlayColor = if (isDark) Color(0xFF7A7571) else Color(0xFFE5E0DB)
        listOf(3, 5, 7, 9, 12).forEach { markerFret ->
            if (markerFret <= baseFret + numFrets - 1 && markerFret >= baseFret) {
                val relFret = markerFret - baseFret + 1
                val inlayX = leftPad + (relFret - 0.5f) * fretW
                val midY = topPad + (h - topPad * 2) / 2f
                if (markerFret == 12) {
                    drawCircle(color = inlayColor, radius = 4.5f.dp.toPx(), center = Offset(inlayX, midY - stringH * 0.65f))
                    drawCircle(color = inlayColor, radius = 4.5f.dp.toPx(), center = Offset(inlayX, midY + stringH * 0.65f))
                } else {
                    drawCircle(color = inlayColor, radius = 4.5f.dp.toPx(), center = Offset(inlayX, midY))
                }
            }
        }

        // Fret lines
        if (baseFret == 1) {
            drawLine(color = if (isDark) Color(0xFFCCCCCC) else Color(0xFFEBEBEB), start = Offset(leftPad, topPad - 2f), end = Offset(leftPad, h - topPad + 2f), strokeWidth = 5f)
        }
        for (f in 0..numFrets) {
            val x = leftPad + f * fretW
            if (!(f == 0 && baseFret == 1)) {
                drawLine(color = if (isDark) Color(0xFF5E5A57) else Color(0xFF9E9A96), start = Offset(x, topPad), end = Offset(x, h - topPad), strokeWidth = 1.8f)
            }
        }

        // Strings
        for (s in 0 until numStrings) {
            val y = topPad + s * stringH
            drawLine(brush = Brush.verticalGradient(colors = listOf(Color(0xFFEEEEEE), Color(0xFF999999))), start = Offset(leftPad, y), end = Offset(w - rightPad, y), strokeWidth = 1.2f + (numStrings - 1 - s) * 0.5f)
            drawContext.canvas.nativeCanvas.drawText(stringLabels[s], leftPad - 20f, y + 5f, android.graphics.Paint().apply { color = android.graphics.Color.argb(200, 230, 230, 230); textSize = 13.dp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT; typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD); isAntiAlias = true })
        }

        // Barre
        if (barreFret != null) {
            val relFret = barreFret - baseFret + 1
            if (relFret in 1..numFrets) {
                val barreX = leftPad + (relFret - 0.5f) * fretW
                drawRoundRect(color = accentColor.copy(alpha = 0.85f), topLeft = Offset(barreX - (dotSizePx / 2f), topPad - 4f), size = Size(dotSizePx, (h - topPad * 2) + 8f), cornerRadius = CornerRadius(dotSizePx / 3f))
            }
        }

        // Chord/Scale rendering logic
        if (chordFrets != null && chordFingers != null) {
            // ... (your existing chord logic here)
        } else {
            for (s in 0 until numStrings) {
                val y = topPad + s * stringH
                val openSemi = openStringsSemis.getOrNull(s) ?: 0
                for (f in 1..numFrets) {
                    val absoluteFret = baseFret + f - 1
                    val fretSemi = (openSemi + absoluteFret) % 12
                    val isCurrentlyPlaying = (activeCoordinate != null && activeCoordinate.first == s && activeCoordinate.second == absoluteFret)

                    if (normalizedActiveNotes.contains(fretSemi) || isCurrentlyPlaying) {
                        val isRoot = (fretSemi == normalizedRootNote)
                        val cx = leftPad + (f - 0.5f) * fretW
                        val dotRadius = if (isCurrentlyPlaying) 13.dp.toPx() else 9.5f.dp.toPx()
                        
                        // Drawing glow and dot
                        if (isCurrentlyPlaying) drawCircle(color = Color.White.copy(0.4f), radius = dotRadius + 6.dp.toPx(), center = Offset(cx, y))
                        drawCircle(brush = Brush.radialGradient(colors = listOf(if(isCurrentlyPlaying) Color.White else if(isRoot) accentColor else Color(0xFF81D4FA).copy(0.45f), if(isCurrentlyPlaying) accentColor else if(isRoot) accentColor.copy(0.75f) else Color(0xFF29B6F6).copy(0.35f)), center = Offset(cx, y), radius = dotRadius), radius = dotRadius, center = Offset(cx, y))
                        drawCircle(color = Color.Black.copy(0.4f), radius = dotRadius, center = Offset(cx, y), style = Stroke(1.2f))

                        if (showNoteNames || isRoot || isCurrentlyPlaying) {
                            drawContext.canvas.nativeCanvas.drawText(NOTE_NAMES[fretSemi], cx, y + 4.5f, android.graphics.Paint().apply { color = if (isCurrentlyPlaying) android.graphics.Color.BLACK else if (isRoot) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY; textSize = 12.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true; typeface = android.graphics.Typeface.DEFAULT_BOLD })
                        }
                    }
                }
            }
        }

        // Fret Numbers and Position Text
        listOf(3, 5, 7, 9, 12).filter { it <= baseFret + numFrets - 1 && it >= baseFret }.forEach { f ->
            val relF = f - baseFret + 1
            val x = leftPad + (relF - 0.5f) * fretW
            drawContext.canvas.nativeCanvas.drawText("$f", x, h - 6f, android.graphics.Paint().apply { color = android.graphics.Color.argb(220, 200, 200, 200); textSize = 11.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); isAntiAlias = true })
        }
    }
}

private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
@Composable
fun UnifiedPianoKeyboard(
    activeMidiNotes: Map<Int, Boolean>,
    accentColor: Color,
    rootNote: Int = 0,
    fingering: FingeringData? = null,
    activeCoordinate: Int? = null,
    showNoteNames: Boolean = false,
    isPlaying: Boolean = false,
    startNote: Int = 48,
    endNote: Int = 84,
    onKeyTapped: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val NOTE_NAMES = remember { listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B") }

    val baseWhiteStart = Color(0xFFF7F1E3)
    val baseWhiteMid = Color(0xFFF2EBDD)
    val baseWhiteEnd = Color(0xFFE8DFC9)

    val baseBlackStart = Color(0xFF4B4B4B)
    val baseBlackMid = Color(0xFF222222)
    val baseBlackEnd = Color(0xFF080808)

    val normalizedActiveMidi = remember(activeMidiNotes) {
        activeMidiNotes.flatMap { entry -> listOf(entry.key to entry.value, (entry.key % 12) to entry.value) }.toMap()
    }

    val whiteMidiNotes = remember(startNote, endNote) {
        (startNote..endNote).filter { n -> (n % 12) !in listOf(1, 3, 6, 8, 10) }
    }

    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }

    val maxWhiteKeyWidthPx = with(density) { 42.dp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val availableWidth = constraints.maxWidth.toFloat()
        val calculatedKeyWidth = availableWidth / whiteMidiNotes.size.coerceAtLeast(1)
        val finalWhiteKeyWidthPx = if (calculatedKeyWidth > maxWhiteKeyWidthPx) maxWhiteKeyWidthPx else calculatedKeyWidth
        val totalKeyboardWidthPx = finalWhiteKeyWidthPx * whiteMidiNotes.size
        val startPaddingOffset = (availableWidth - totalKeyboardWidthPx) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(startNote, endNote, whiteMidiNotes, finalWhiteKeyWidthPx, startPaddingOffset) {
                    detectTapGestures { tapOffset ->
                        if (onKeyTapped == null) return@detectTapGestures
                        val adjustedX = tapOffset.x - startPaddingOffset
                        val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.60f
                        val blackKeyHeightPx = size.height * 0.62f

                        var triggeredMidi: Int? = null
                        if (tapOffset.y < blackKeyHeightPx) {
                            whiteMidiNotes.forEachIndexed { i, midiNote ->
                                if ((midiNote % 12) in listOf(0, 2, 5, 7, 9)) {
                                    val blackX = i * finalWhiteKeyWidthPx + finalWhiteKeyWidthPx - (blackKeyWidthPx / 2f)
                                    if (adjustedX in blackX..(blackX + blackKeyWidthPx)) triggeredMidi = midiNote + 1
                                }
                            }
                        }
                        if (triggeredMidi == null) {
                            val whiteIndex = (adjustedX / finalWhiteKeyWidthPx).toInt().coerceIn(0, whiteMidiNotes.size - 1)
                            triggeredMidi = whiteMidiNotes[whiteIndex]
                        }
                        triggeredMidi?.let { onKeyTapped(it) }
                    }
                }
        ) {
            val totalHeight = size.height
            val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.60f
            val blackKeyHeightPx = totalHeight * 0.62f
            val cornerRadiusPx = 3.dp.toPx()

            translate(left = startPaddingOffset) {
                // 1. Draw White Keys
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    val isExplicitActive = normalizedActiveMidi[midiNote] == true || 
                                          activeMidiNotes[midiNote] == true || 
                                          fingering?.notes?.contains(midiNote) == true || 
                                          midiNote == activeCoordinate
                    
                    val x = index * finalWhiteKeyWidthPx
                    val fingerIndex = fingering?.notes?.indexOf(midiNote) ?: -1
                    val fingerNum = if (fingerIndex != -1) fingering?.fingers?.getOrNull(fingerIndex) else ((midiNote % 12) % 5 + 1)

                    drawRoundRect(
                        brush = if (isExplicitActive) Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.75f), accentColor, accentColor.copy(alpha = 0.9f))) else Brush.verticalGradient(listOf(baseWhiteStart, baseWhiteMid, baseWhiteEnd)),
                        topLeft = Offset(x, 0f), size = Size(finalWhiteKeyWidthPx - 1f, totalHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                    drawRoundRect(color = Color(0xFFBDBDBD), topLeft = Offset(x, 0f), size = Size(finalWhiteKeyWidthPx - 1f, totalHeight), cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx), style = Stroke(width = 1.dp.toPx()))

                    val centerX = x + finalWhiteKeyWidthPx / 2f
                    
                    if (showNoteNames || isExplicitActive) {
                        val badgeY = totalHeight - 24.dp.toPx()
                        if (isExplicitActive) {
                            drawCircle(color = Color.White, radius = 14.dp.toPx(), center = Offset(centerX, badgeY))
                            drawCircle(color = accentColor, radius = 14.dp.toPx(), style = Stroke(width = 2.dp.toPx()), center = Offset(centerX, badgeY))
                        }
                        
                        textPaint.color = if (isExplicitActive) android.graphics.Color.BLACK else android.graphics.Color.DKGRAY
                        textPaint.textSize = 10.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(NOTE_NAMES[midiNote % 12], centerX, if (isExplicitActive) badgeY + 4.dp.toPx() else totalHeight - 20.dp.toPx(), textPaint)
                        
                        if (isExplicitActive && fingerNum != null) {
                            textPaint.textSize = 9.dp.toPx()
                            textPaint.color = android.graphics.Color.DKGRAY
                            drawContext.canvas.nativeCanvas.drawText(fingerNum.toString(), centerX, badgeY + 16.dp.toPx(), textPaint)
                        }
                    }
                }

                // 2. Draw Black Keys
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    if ((midiNote % 12) in listOf(0, 2, 5, 7, 9)) {
                        val blackMidiNote = midiNote + 1
                        val isExplicitActive = normalizedActiveMidi[blackMidiNote] == true || 
                                              activeMidiNotes[blackMidiNote] == true || 
                                              fingering?.notes?.contains(blackMidiNote) == true || 
                                              blackMidiNote == activeCoordinate

                        val blackX = (index * finalWhiteKeyWidthPx) + (finalWhiteKeyWidthPx - blackKeyWidthPx / 2f)
                        val fingerIndex = fingering?.notes?.indexOf(blackMidiNote) ?: -1
                        val fingerNum = if (fingerIndex != -1) fingering?.fingers?.getOrNull(fingerIndex) else ((blackMidiNote % 12) % 5 + 1)
                        
                        drawRoundRect(
                            brush = if (isExplicitActive) SolidColor(accentColor) else Brush.verticalGradient(listOf(baseBlackStart, baseBlackMid, baseBlackEnd)),
                            topLeft = Offset(blackX, 0f), size = Size(blackKeyWidthPx, blackKeyHeightPx),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                        
                        val centerX = blackX + blackKeyWidthPx / 2f
                        if (showNoteNames || isExplicitActive) {
                            val badgeY = blackKeyHeightPx - 20.dp.toPx()
                            if (isExplicitActive) {
                                drawCircle(color = Color.Black, radius = 10.dp.toPx(), center = Offset(centerX, badgeY))
                            }
                            textPaint.color = android.graphics.Color.WHITE
                            textPaint.textSize = 9.dp.toPx()
                            drawContext.canvas.nativeCanvas.drawText(NOTE_NAMES[blackMidiNote % 12], centerX, if (isExplicitActive) badgeY + 3.dp.toPx() else badgeY, textPaint)
                            
                            if (isExplicitActive && fingerNum != null) {
                                textPaint.textSize = 8.dp.toPx()
                                drawContext.canvas.nativeCanvas.drawText(fingerNum.toString(), centerX, badgeY + 13.dp.toPx(), textPaint)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun InstrumentPlayButton(
    notes: List<Int>,
    accentColor: Color,
    label: String = "",
    arpeggiate: Boolean = true,
    arpDelayMs: Long = 120,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "play_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.65f else 1f,
        animationSpec = tween(150), label = "play_alpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = {
                if (!isPlaying && notes.isNotEmpty()) {
                    isPlaying = true
                    coroutineScope.launch {
                        if (arpeggiate) {
                            notes.forEach { note ->
                                onPlay(note)
                                delay(arpDelayMs)
                            }
                        } else {
                            notes.forEach { note -> onPlay(note) }
                        }
                        delay(400)
                        isPlaying = false
                    }
                }
            },
            shape = CircleShape,
            color = if (isPlaying) accentColor else accentColor.copy(alpha = 0.18f),
            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
            modifier = Modifier.size(36.dp).scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayCircle,
                    contentDescription = "Play notes",
                    tint = if (isPlaying) Color.White else accentColor,
                    modifier = Modifier.size(20.dp).graphicsLayer(alpha = alpha)
                )
            }
        }
        if (label.isNotBlank()) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun InstrumentPlayButton(
    accentColor: Color,
    label: String = "",
    isPlaying: Boolean = false,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onPlayClick,
            shape = CircleShape,
            color = if (isPlaying) accentColor else accentColor.copy(alpha = 0.18f),
            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayCircle,
                    contentDescription = "Play notes",
                    tint = if (isPlaying) Color.White else accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (label.isNotBlank()) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(alpha = 0.85f))
        }
    }
}