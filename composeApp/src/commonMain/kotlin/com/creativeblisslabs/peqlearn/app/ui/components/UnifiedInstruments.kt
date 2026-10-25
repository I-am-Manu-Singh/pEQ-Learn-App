package com.creativeblisslabs.peqlearn.app.ui.components

import kotlin.math.roundToInt
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
import androidx.compose.ui.graphics.Path
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
import com.creativeblisslabs.peqlearn.app.ui.screens.FingeringData
import com.creativeblisslabs.peqlearn.app.ui.screens.PianoKeyboard88Keys
import com.creativeblisslabs.peqlearn.app.util.GuitarShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
@Composable
fun UnifiedGuitarFretboard(
    numFrets: Int,
    activeNotes: List<Int> = emptyList(),
    rootNote: Int = 0,
    accentColor: Color,
    showNoteNames: Boolean = false,
    markOctaves: Boolean = true,
    highlightOctaveC: Boolean = true,
    chordFrets: List<Int>? = null,
    chordFingers: List<Int>? = null,
    baseFret: Int = 1,
    barreFret: Int? = null,
    stringLabels: List<String> = listOf("e", "B", "G", "D", "A", "E"),
    openStringsSemis: List<Int> = listOf(4, 11, 7, 2, 9, 4),
    activeCoordinate: Pair<Int, Int>? = null,
    activeMidiNote: Int? = null,
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    onFretTapped: ((stringIndex: Int, fret: Int, midiNote: Int) -> Unit)? = null,
    fretboardBrush: Brush? = null,
    showInlayDots: Boolean = true,
    modifier: Modifier = Modifier
) {
    val numStrings = stringLabels.size

    val filteredActiveNotes = remember(activeNotes, isLeftHandHidden, isRightHandHidden) {
        activeNotes.filter { midi ->
            val isTreble = midi >= 60
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }
    }
    val normalizedActiveNotes = remember(filteredActiveNotes) { filteredActiveNotes.map { it % 12 }.toSet() }
    val effectiveActiveMidiNote = remember(activeMidiNote, isLeftHandHidden, isRightHandHidden) {
        if (activeMidiNote == null) null
        else {
            val isTreble = activeMidiNote >= 60
            if (isTreble && isRightHandHidden) null
            else if (!isTreble && isLeftHandHidden) null
            else activeMidiNote
        }
    }
    val normalizedRootNote = remember(rootNote) { rootNote % 12 }

    val defaultOpenStringsMidi = remember(numStrings) {
        if (numStrings == 4) listOf(69, 64, 60, 67) else listOf(64, 59, 55, 50, 45, 40)
    }

    // Hoisted reusable native Paints to eliminate per-frame JNI allocations
    val stringLabelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }
    val nutMutedPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(239, 83, 80)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val notePillTextPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val fretNumPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 180, 186, 198)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val defaultNeckBrush = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFF2B303C), Color(0xFF20242E), Color(0xFF1B1E26)))
    }

    BoxWithConstraints(modifier = modifier) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val leftPad = 46f
        val topPad = 16f
        val rightPad = 20f
        val bottomPad = 26f
        val fretW = (totalWidth - leftPad - rightPad) / numFrets.coerceAtLeast(1)
        val playableH = totalHeight - topPad - bottomPad

        val nutH = playableH * 0.88f
        val bridgeH = playableH * 0.98f

        val midY = topPad + playableH / 2f
        val nutTop = midY - nutH / 2f
        val nutBottom = midY + nutH / 2f
        val bridgeTop = midY - bridgeH / 2f
        val bridgeBottom = midY + bridgeH / 2f

        fun getYForStringAtX(s: Int, x: Float): Float {
            val t = ((x - leftPad) / (totalWidth - leftPad - rightPad).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val currentH = nutH + (bridgeH - nutH) * t
            val currentTop = midY - currentH / 2f
            val curStringH = currentH / (numStrings - 1).coerceAtLeast(1).toFloat()
            return currentTop + s * curStringH
        }

        val fretboardPath = remember(totalWidth, totalHeight, nutTop, nutBottom, bridgeTop, bridgeBottom) {
            Path().apply {
                moveTo(leftPad, nutTop)
                lineTo(totalWidth - rightPad, bridgeTop)
                lineTo(totalWidth - rightPad, bridgeBottom)
                lineTo(leftPad, nutBottom)
                close()
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(numFrets, numStrings, baseFret) {
                    detectTapGestures { tapOffset ->
                        if (onFretTapped == null) return@detectTapGestures
                        val tappedFret = if (tapOffset.x < leftPad) {
                            0
                        } else {
                            val relFret = ((tapOffset.x - leftPad) / fretW).toInt() + 1
                            (baseFret + relFret - 1).coerceIn(0, 24)
                        }

                        var closestString = 0
                        var minDistance = Float.MAX_VALUE
                        for (s in 0 until numStrings) {
                            val strY = getYForStringAtX(s, tapOffset.x)
                            val dist = kotlin.math.abs(tapOffset.y - strY)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestString = s
                            }
                        }

                        val baseMidi = defaultOpenStringsMidi.getOrNull(closestString) ?: (openStringsSemis.getOrElse(closestString) { 0 } + 40)
                        val noteMidi = baseMidi + tappedFret
                        onFretTapped(closestString, tappedFret, noteMidi)
                    }
                }
        ) {
            val w = size.width
            val pillColor = Color(0xFFFF9E1B)

            drawPath(path = fretboardPath, brush = fretboardBrush ?: defaultNeckBrush)
            drawPath(path = fretboardPath, color = Color(0xFF3E4554).copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))

            // Nut
            drawLine(
                color = Color(0xFFE8E8E8),
                start = Offset(leftPad, nutTop),
                end = Offset(leftPad, nutBottom),
                strokeWidth = 4.dp.toPx()
            )

            // Inlays
            if (showInlayDots) {
                val inlayColor = Color(0xFF8A7E72).copy(alpha = 0.65f)
                val dotRad = 3.5f.dp.toPx()
                for (markerFret in intArrayOf(3, 5, 7, 9, 12)) {
                    if (markerFret in baseFret..(baseFret + numFrets - 1)) {
                        val relFret = markerFret - baseFret + 1
                        val inlayX = leftPad + (relFret - 0.5f) * fretW
                        val t = ((inlayX - leftPad) / (w - leftPad - rightPad)).coerceIn(0f, 1f)
                        val currentH = nutH + (bridgeH - nutH) * t
                        val curStringH = currentH / (numStrings - 1).coerceAtLeast(1).toFloat()
                        if (markerFret == 12) {
                            drawCircle(color = inlayColor, radius = dotRad, center = Offset(inlayX, midY - curStringH * 0.75f))
                            drawCircle(color = inlayColor, radius = dotRad, center = Offset(inlayX, midY + curStringH * 0.75f))
                        } else {
                            drawCircle(color = inlayColor, radius = dotRad, center = Offset(inlayX, midY))
                        }
                    }
                }
            }

            // Fret wires
            val fretStroke = 1.5.dp.toPx()
            for (f in 1..numFrets) {
                val x = leftPad + f * fretW
                val t = ((x - leftPad) / (w - leftPad - rightPad)).coerceIn(0f, 1f)
                val curTop = nutTop + (bridgeTop - nutTop) * t
                val curBottom = nutBottom + (bridgeBottom - nutBottom) * t
                drawLine(
                    color = Color(0xFF8D7365).copy(alpha = 0.85f),
                    start = Offset(x, curTop),
                    end = Offset(x, curBottom),
                    strokeWidth = fretStroke
                )
            }

            // Strings
            stringLabelPaint.textSize = 12.dp.toPx()
            val labelYOff = 4.5f.dp.toPx()
            for (s in 0 until numStrings) {
                val startY = getYForStringAtX(s, leftPad)
                val endY = getYForStringAtX(s, w - rightPad)
                val gauge = (1.0f + s * 0.45f).dp.toPx()
                drawLine(
                    color = Color(0xFFC0C4CC),
                    start = Offset(leftPad, startY),
                    end = Offset(w - rightPad, endY),
                    strokeWidth = gauge
                )
                drawContext.canvas.nativeCanvas.drawText(
                    stringLabels.getOrElse(s) { "" },
                    leftPad - 24f,
                    startY + labelYOff,
                    stringLabelPaint
                )
            }

            // Barre
            if (barreFret != null && barreFret > 0) {
                val relFret = if (barreFret >= baseFret) barreFret - baseFret + 1 else barreFret
                if (relFret in 1..numFrets) {
                    val barreX = leftPad + (relFret - 0.5f) * fretW
                    val t = ((barreX - leftPad) / (w - leftPad - rightPad)).coerceIn(0f, 1f)
                    val curTop = nutTop + (bridgeTop - nutTop) * t
                    val curH = (nutBottom + (bridgeBottom - nutBottom) * t) - curTop
                    drawRoundRect(
                        color = pillColor.copy(alpha = 0.85f),
                        topLeft = Offset(barreX - 12.dp.toPx(), curTop - 2f),
                        size = Size(24.dp.toPx(), curH + 4f),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
            }

            // Open string indicators
            nutMutedPaint.textSize = 13.dp.toPx()
            if (chordFrets != null) {
                for (s in 0 until numStrings) {
                    val f = chordFrets.getOrNull(s) ?: -1
                    val y = getYForStringAtX(s, leftPad)
                    val nutIndicatorX = leftPad - 12f
                    val isOpenPlaying = (f == 0 && (
                        (activeCoordinate != null && activeCoordinate.first == s && activeCoordinate.second == 0) ||
                            (activeMidiNote != null && ((defaultOpenStringsMidi.getOrNull(s) ?: (openStringsSemis.getOrElse(s) { 0 } + 40)) % 12 == (activeMidiNote % 12)))
                        ))

                    if (f == -1) {
                        drawContext.canvas.nativeCanvas.drawText("✕", nutIndicatorX, y + labelYOff, nutMutedPaint)
                    } else if (f == 0) {
                        if (isOpenPlaying) {
                            drawCircle(color = Color(0xFFFFEB3B).copy(alpha = 0.5f), radius = 9.dp.toPx(), center = Offset(nutIndicatorX, y))
                            drawCircle(color = Color(0xFFFFEB3B), radius = 6.dp.toPx(), center = Offset(nutIndicatorX, y))
                            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(nutIndicatorX, y), style = Stroke(width = 2.dp.toPx()))
                        } else {
                            drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = Offset(nutIndicatorX, y), style = Stroke(width = 1.8.dp.toPx()))
                        }
                    }
                }
            } else if (normalizedActiveNotes.isNotEmpty()) {
                for (s in 0 until numStrings) {
                    val openSemi = (openStringsSemis.getOrNull(s) ?: 0) % 12
                    if (normalizedActiveNotes.contains(openSemi)) {
                        val y = getYForStringAtX(s, leftPad)
                        val nutIndicatorX = leftPad - 14f
                        val isRoot = openSemi == normalizedRootNote
                        val isOpenPlaying = (
                            (activeCoordinate != null && activeCoordinate.first == s && activeCoordinate.second == 0) ||
                                (effectiveActiveMidiNote != null && (openSemi == (effectiveActiveMidiNote % 12)))
                            )

                        if (isOpenPlaying) {
                            drawCircle(color = Color(0xFFFFEB3B).copy(alpha = 0.6f), radius = 10.dp.toPx(), center = Offset(nutIndicatorX, y))
                            drawCircle(color = Color(0xFFFFEB3B), radius = 6.5.dp.toPx(), center = Offset(nutIndicatorX, y))
                            drawCircle(color = Color.White, radius = 6.5.dp.toPx(), center = Offset(nutIndicatorX, y), style = Stroke(width = 2.dp.toPx()))
                        } else {
                            drawCircle(color = if (isRoot) Color(0xFFFF5252) else accentColor, radius = 5.dp.toPx(), center = Offset(nutIndicatorX, y))
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(nutIndicatorX, y), style = Stroke(width = 1.2.dp.toPx()))
                        }
                    }
                }
            }

            // Active Fretted Notes
            val textOffsetY = 4.dp.toPx()
            for (s in 0 until numStrings) {
                val openSemi = openStringsSemis.getOrNull(s) ?: 0
                val stringChordFret = chordFrets?.getOrNull(s) ?: -1
                val baseStringMidi = defaultOpenStringsMidi.getOrNull(s) ?: (openSemi + 40)

                for (f in 1..numFrets) {
                    val absoluteFret = baseFret + f - 1
                    val fretSemi = (openSemi + absoluteFret) % 12
                    val fullMidiNote = baseStringMidi + absoluteFret
                    val isChordFret = chordFrets != null && stringChordFret > 0 && (
                        stringChordFret == absoluteFret || (baseFret > 1 && stringChordFret == f)
                        )
                    val matchesCoord = activeCoordinate != null && activeCoordinate.first == s && (
                        activeCoordinate.second == absoluteFret || activeCoordinate.second == f
                        )
                    val matchesMidi = effectiveActiveMidiNote != null && (
                        (chordFrets != null && isChordFret && (fullMidiNote % 12 == effectiveActiveMidiNote % 12)) ||
                            (chordFrets == null && fullMidiNote == effectiveActiveMidiNote)
                        )
                    val isCurrentlyPlaying = matchesCoord || matchesMidi
                    val isScaleNote = chordFrets == null && normalizedActiveNotes.contains(fretSemi)
                    val isRoot = fretSemi == normalizedRootNote

                    if (isChordFret || isCurrentlyPlaying || isScaleNote) {
                        val cx = leftPad + (f - 0.5f) * fretW
                        val cy = getYForStringAtX(s, cx)
                        val dotRadius = when {
                            isCurrentlyPlaying -> 13.dp.toPx()
                            isScaleNote -> 10.dp.toPx()
                            else -> 11.5.dp.toPx()
                        }

                        val primaryDotColor = when {
                            isCurrentlyPlaying -> Color(0xFFFFEB3B)
                            isRoot && isScaleNote -> Color(0xFFFF5252)
                            else -> pillColor
                        }

                        drawCircle(
                            color = if (isCurrentlyPlaying) Color(0xFFFFEB3B).copy(alpha = 0.55f) else primaryDotColor.copy(alpha = 0.30f),
                            radius = dotRadius + (if (isCurrentlyPlaying) 4.dp.toPx() else 2.5f.dp.toPx()),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = primaryDotColor,
                            radius = dotRadius,
                            center = Offset(cx, cy)
                        )

                        if (isCurrentlyPlaying || (isRoot && isScaleNote)) {
                            drawCircle(
                                color = Color.White,
                                radius = dotRadius,
                                center = Offset(cx, cy),
                                style = Stroke(width = if (isCurrentlyPlaying) 2.2.dp.toPx() else 1.6.dp.toPx())
                            )
                        }

                        val fingerNum = if (isChordFret) chordFingers?.getOrNull(s) else null
                        val octaveNumber = (fullMidiNote / 12) - 1
                        val isNoteC = fretSemi == 0

                        val displayText = if (fingerNum != null && fingerNum > 0) {
                            "$fingerNum"
                        } else if (isNoteC && markOctaves) {
                            "C$octaveNumber"
                        } else if (markOctaves && (isRoot || showNoteNames)) {
                            "${NOTE_NAMES[fretSemi]}$octaveNumber"
                        } else {
                            NOTE_NAMES[fretSemi]
                        }

                        notePillTextPaint.color = if (isCurrentlyPlaying) android.graphics.Color.rgb(180, 40, 0) else android.graphics.Color.WHITE
                        notePillTextPaint.textSize = (if (displayText.length > 2) 9.dp else if (isScaleNote) 10.dp else 11.dp).toPx()
                        drawContext.canvas.nativeCanvas.drawText(displayText, cx, cy + textOffsetY, notePillTextPaint)
                    }
                }
            }

            // Bottom Fret Numbers
            fretNumPaint.textSize = 11.dp.toPx()
            for (f in intArrayOf(3, 5, 7, 9, 12)) {
                if (f in baseFret..(baseFret + numFrets - 1)) {
                    val relF = f - baseFret + 1
                    val x = leftPad + (relF - 0.5f) * fretW
                    drawContext.canvas.nativeCanvas.drawText("$f", x, size.height - 6f, fretNumPaint)
                }
            }
        }
    }
}

@Composable
fun UnifiedPianoKeyboard(
    activeMidiNotes: Map<Int, Boolean>,
    accentColor: Color,
    rootNote: Int = 0,
    fingering: FingeringData? = null,
    activeCoordinate: Int? = null,
    showNoteNames: Boolean = false,
    isPlaying: Boolean = false,
    startNote: Int = 21,
    endNote: Int = 108,
    fillWidth: Boolean = false,
    leftHandColor: Color = Color(0xFF6C63FF),
    rightHandColor: Color = Color(0xFF4A90E2),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    customKeyColors: Map<Int, Color> = emptyMap(),
    onKeyTapped: ((Int) -> Unit)? = null,
    onKeyReleased: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val whiteMidiNotes = remember(startNote, endNote) {
        (startNote..endNote).filter { n -> (n % 12) !in listOf(1, 3, 6, 8, 10) }
    }

    val maxWhiteKeyWidthPx = with(density) { 42.dp.toPx() }

    // Hoisted reusable Paint objects to prevent 88 allocations per frame
    val octaveTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(170, 120, 125, 135)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val keyBadgeTextPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val availableWidth = constraints.maxWidth.toFloat()
        val calculatedKeyWidth = availableWidth / whiteMidiNotes.size.coerceAtLeast(1)
        val finalWhiteKeyWidthPx = if (fillWidth) {
            calculatedKeyWidth
        } else {
            if (calculatedKeyWidth > maxWhiteKeyWidthPx) maxWhiteKeyWidthPx else calculatedKeyWidth
        }
        val totalKeyboardWidthPx = finalWhiteKeyWidthPx * whiteMidiNotes.size
        val startPaddingOffset = if (fillWidth) 0f else (availableWidth - totalKeyboardWidthPx) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(startNote, endNote, whiteMidiNotes, finalWhiteKeyWidthPx, startPaddingOffset) {
                    val activePointers = mutableMapOf<Long, Int>()
                    fun resolveMidiAt(offset: Offset): Int? {
                        val adjustedX = offset.x - startPaddingOffset
                        val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f
                        val blackKeyHeightPx = size.height * 0.62f
                        if (offset.y < blackKeyHeightPx) {
                            for (i in whiteMidiNotes.indices) {
                                val midiNote = whiteMidiNotes[i]
                                val semitone = (midiNote % 12).let { if (it < 0) it + 12 else it }
                                if (semitone in listOf(0, 2, 5, 7, 9)) {
                                    val blackMidi = midiNote + 1
                                    if (blackMidi <= endNote) {
                                        val blackX = i * finalWhiteKeyWidthPx + finalWhiteKeyWidthPx - (blackKeyWidthPx / 2f)
                                        if (adjustedX >= blackX && adjustedX <= (blackX + blackKeyWidthPx)) {
                                            return blackMidi
                                        }
                                    }
                                }
                            }
                        }
                        val whiteIndex = (adjustedX / finalWhiteKeyWidthPx).toInt()
                        return if (whiteIndex in whiteMidiNotes.indices) whiteMidiNotes[whiteIndex] else null
                    }

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                val id = change.id.value
                                if (change.pressed) {
                                    val midi = resolveMidiAt(change.position)
                                    val prevMidi = activePointers[id]
                                    if (prevMidi == null && midi != null) {
                                        activePointers[id] = midi
                                        onKeyTapped?.invoke(midi)
                                    } else if (prevMidi != null && midi != null && prevMidi != midi) {
                                        onKeyReleased?.invoke(prevMidi)
                                        activePointers[id] = midi
                                        onKeyTapped?.invoke(midi)
                                    }
                                } else {
                                    activePointers.remove(id)?.let { releasedMidi ->
                                        onKeyReleased?.invoke(releasedMidi)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val totalHeight = size.height
            val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f
            val blackKeyHeightPx = totalHeight * 0.62f
            val cornerRadiusPx = 4.dp.toPx()
            val keyCorner = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            val borderStroke = Stroke(width = 1.dp.toPx())
            val frontFaceH = totalHeight * 0.12f
            val topSurfaceH = totalHeight - frontFaceH

            octaveTextPaint.textSize = 9.dp.toPx()

            translate(left = startPaddingOffset) {
                // 1. Draw White Keys (Zero per-key Brush allocations)
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    val isHandHidden = if (midiNote >= 60) isRightHandHidden else isLeftHandHidden
                    val isCurrentlyPlaying = !isHandHidden && activeCoordinate != null && (midiNote == activeCoordinate || (activeCoordinate < 12 && (midiNote % 12) == activeCoordinate))
                    val isExplicitActive = !isHandHidden && (
                        activeMidiNotes[midiNote] == true ||
                            (activeCoordinate != null && activeCoordinate < 12 && (midiNote % 12) == activeCoordinate) ||
                            fingering?.notes?.contains(midiNote) == true ||
                            isCurrentlyPlaying
                        )

                    val x = index * finalWhiteKeyWidthPx
                    val handColor = if (midiNote >= 60) rightHandColor else leftHandColor
                    val customColor = customKeyColors[midiNote]
                    val topKeyColor = when {
                        customColor != null -> customColor
                        isCurrentlyPlaying || isExplicitActive -> handColor
                        else -> Color(0xFFF7F4EC)
                    }
                    val frontLipColor = when {
                        customColor != null -> customColor.copy(alpha = 0.82f)
                        isCurrentlyPlaying || isExplicitActive -> handColor.copy(alpha = 0.82f)
                        else -> Color(0xFFD4C9B8)
                    }

                    // Top Surface
                    drawRoundRect(
                        color = topKeyColor,
                        topLeft = Offset(x + 1f, 0f),
                        size = Size(finalWhiteKeyWidthPx - 2f, topSurfaceH),
                        cornerRadius = keyCorner
                    )

                    // Front Lip
                    drawRoundRect(
                        color = frontLipColor,
                        topLeft = Offset(x + 1f, topSurfaceH - 1f),
                        size = Size(finalWhiteKeyWidthPx - 2f, frontFaceH + 1f),
                        cornerRadius = keyCorner
                    )

                    // Border
                    drawRoundRect(
                        color = Color(0xFF1E222B).copy(alpha = 0.45f),
                        topLeft = Offset(x + 1f, 0f),
                        size = Size(finalWhiteKeyWidthPx - 2f, totalHeight),
                        cornerRadius = keyCorner,
                        style = borderStroke
                    )

                    val centerX = x + finalWhiteKeyWidthPx / 2f

                    if (midiNote % 12 == 0) {
                        val octaveNumber = (midiNote / 12) - 1
                        drawContext.canvas.nativeCanvas.drawText(
                            "C$octaveNumber",
                            centerX,
                            totalHeight - 8.dp.toPx(),
                            octaveTextPaint
                        )
                    }

                    if (isExplicitActive || showNoteNames) {
                        val fingerIndex = fingering?.notes?.indexOf(midiNote) ?: -1
                        val fingerNum = if (fingerIndex != -1) fingering?.fingers?.getOrNull(fingerIndex) else null
                        val badgeY = totalHeight - 20.dp.toPx()
                        val badgeRadius = if (isCurrentlyPlaying) 12.dp.toPx() else if (isExplicitActive) 10.5.dp.toPx() else 8.5.dp.toPx()

                        drawCircle(
                            color = when {
                                isCurrentlyPlaying -> Color(0xFFFFEB3B)
                                isExplicitActive -> Color.White
                                else -> Color(0xFF1E222B).copy(alpha = 0.85f)
                            },
                            radius = badgeRadius,
                            center = Offset(centerX, badgeY)
                        )

                        val displayNum = if (isExplicitActive && fingerNum != null && fingerNum > 0) {
                            fingerNum.toString()
                        } else {
                            NOTE_NAMES[midiNote % 12]
                        }
                        keyBadgeTextPaint.color = when {
                            isCurrentlyPlaying -> android.graphics.Color.rgb(190, 40, 0)
                            isExplicitActive -> android.graphics.Color.rgb(230, 110, 0)
                            else -> android.graphics.Color.WHITE
                        }
                        keyBadgeTextPaint.textSize = if (isExplicitActive) 10.5.dp.toPx() else 8.5.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(displayNum, centerX, badgeY + 3.5.dp.toPx(), keyBadgeTextPaint)
                    }
                }

                // 2. Draw Black Keys
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    val semitone = (midiNote % 12).let { if (it < 0) it + 12 else it }
                    if (semitone in listOf(0, 2, 5, 7, 9)) {
                        val blackMidiNote = midiNote + 1
                        if (blackMidiNote <= endNote) {
                            val isHandHidden = if (blackMidiNote >= 60) isRightHandHidden else isLeftHandHidden
                            val isCurrentlyPlaying = !isHandHidden && activeCoordinate != null && (blackMidiNote == activeCoordinate || (activeCoordinate < 12 && (blackMidiNote % 12) == activeCoordinate))
                            val isExplicitActive = !isHandHidden && (
                                activeMidiNotes[blackMidiNote] == true ||
                                    (activeCoordinate != null && activeCoordinate < 12 && (blackMidiNote % 12) == activeCoordinate) ||
                                    fingering?.notes?.contains(blackMidiNote) == true ||
                                    isCurrentlyPlaying
                                )

                            val blackX = (index * finalWhiteKeyWidthPx) + (finalWhiteKeyWidthPx - blackKeyWidthPx / 2f)
                            val blackHandColor = if (blackMidiNote >= 60) rightHandColor else leftHandColor
                            val customBlackColor = customKeyColors[blackMidiNote]

                            // Shadow
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.45f),
                                topLeft = Offset(blackX + 1.5f, 2f),
                                size = Size(blackKeyWidthPx, blackKeyHeightPx + 2f),
                                cornerRadius = keyCorner
                            )

                            // Body
                            val bodyColor = when {
                                customBlackColor != null -> customBlackColor
                                isCurrentlyPlaying || isExplicitActive -> blackHandColor
                                else -> Color(0xFF1C1C1E)
                            }
                            drawRoundRect(
                                color = bodyColor,
                                topLeft = Offset(blackX, 0f),
                                size = Size(blackKeyWidthPx, blackKeyHeightPx),
                                cornerRadius = keyCorner
                            )

                            val centerX = blackX + blackKeyWidthPx / 2f
                            if (isExplicitActive || showNoteNames) {
                                val fingerIndex = fingering?.notes?.indexOf(blackMidiNote) ?: -1
                                val fingerNum = if (fingerIndex != -1) fingering?.fingers?.getOrNull(fingerIndex) else null
                                val badgeY = blackKeyHeightPx - 16.dp.toPx()
                                val badgeRadius = if (isCurrentlyPlaying) 10.dp.toPx() else if (isExplicitActive) 8.5.dp.toPx() else 7.5.dp.toPx()

                                drawCircle(
                                    color = when {
                                        isCurrentlyPlaying -> Color(0xFFFFEB3B)
                                        isExplicitActive -> Color.White
                                        else -> Color(0xFF1E222B).copy(alpha = 0.9f)
                                    },
                                    radius = badgeRadius,
                                    center = Offset(centerX, badgeY)
                                )

                                val displayNum = if (isExplicitActive && fingerNum != null && fingerNum > 0) {
                                    fingerNum.toString()
                                } else {
                                    NOTE_NAMES[blackMidiNote % 12]
                                }
                                keyBadgeTextPaint.color = when {
                                    isCurrentlyPlaying -> android.graphics.Color.rgb(190, 40, 0)
                                    isExplicitActive -> android.graphics.Color.rgb(230, 110, 0)
                                    else -> android.graphics.Color.WHITE
                                }
                                keyBadgeTextPaint.textSize = if (isExplicitActive) 9.dp.toPx() else 7.5.dp.toPx()
                                drawContext.canvas.nativeCanvas.drawText(displayNum, centerX, badgeY + 3.dp.toPx(), keyBadgeTextPaint)
                            }
                        }
                    }
                }
            }
        }
    }
}
enum class InstrumentSoundStyle(val displayName: String, val iconDesc: String) {
    SEQUENTIAL("Sequential Strum", "Plays notes in sequence low to high"),
    ALL_AT_ONCE("All Notes at Once", "Sounds full chord block simultaneously"),
    ARPEGGIATE_ROUNDTRIP("Arpeggiate Up & Down", "Plays left to right then back to start")
}

@Composable
fun InstrumentPlayButton(
    notes: List<Int>,
    accentColor: Color,
    label: String = "",
    soundStyle: InstrumentSoundStyle = InstrumentSoundStyle.SEQUENTIAL,
    arpDelayMs: Long = 85,
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
                        when (soundStyle) {
                            InstrumentSoundStyle.SEQUENTIAL -> {
                                notes.forEach { note ->
                                    onPlay(note)
                                    delay(arpDelayMs)
                                }
                            }
                            InstrumentSoundStyle.ALL_AT_ONCE -> {
                                notes.forEach { note -> onPlay(note) }
                            }
                            InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> {
                                val roundTripNotes = if (notes.size > 2) {
                                    notes + notes.subList(0, notes.size - 1).reversed()
                                } else if (notes.size == 2) {
                                    listOf(notes[0], notes[1], notes[0])
                                } else {
                                    notes
                                }
                                roundTripNotes.forEach { note ->
                                    onPlay(note)
                                    delay(arpDelayMs)
                                }
                            }
                        }
                        delay(350)
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
                    contentDescription = soundStyle.iconDesc,
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
    soundStyle: InstrumentSoundStyle? = null,
    onSoundStyleToggle: (() -> Unit)? = null,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    tint = if (isPlaying) Color.White else accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        val displayLabel = if (label.isNotBlank()) {
            if (isPlaying && label.equals("Play", ignoreCase = true)) "Stop" else label
        } else ""
        if (displayLabel.isNotBlank()) {
            Text(displayLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(alpha = 0.85f))
        }

        if (soundStyle != null && onSoundStyleToggle != null) {
            Surface(
                onClick = onSoundStyleToggle,
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                modifier = Modifier.height(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = when (soundStyle) {
                            InstrumentSoundStyle.SEQUENTIAL -> "Strum (Seq)"
                            InstrumentSoundStyle.ALL_AT_ONCE -> "Block (All)"
                            InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> "Arp (L⇄R)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}