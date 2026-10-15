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
    onFretTapped: ((stringIndex: Int, fret: Int, midiNote: Int) -> Unit)? = null,
    fretboardBrush: Brush? = null,
    showInlayDots: Boolean = true,
    modifier: Modifier = Modifier
) {
    val numStrings = stringLabels.size
    val isDark = isSystemInDarkTheme()

    val normalizedActiveNotes = remember(activeNotes) { activeNotes.map { it % 12 } }
    val normalizedRootNote = remember(rootNote) { rootNote % 12 }

    BoxWithConstraints(modifier = modifier) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val leftPad  = 46f
        val topPad   = 16f
        val rightPad = 20f
        val bottomPad = 26f
        val fretW    = (totalWidth - leftPad - rightPad) / numFrets.coerceAtLeast(1)
        val playableH = totalHeight - topPad - bottomPad
        val guitarOpenStringsMidi = listOf(64, 59, 55, 50, 45, 40)

        // Realistic Fretboard Taper: nut end (left) is narrower, bridge/body end (right) is wider
        val nutH = playableH * 0.88f
        // val bridgeH = playableH * 1.08f
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

                        // Determine closest string based on tapered Y
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

                        val baseMidi = guitarOpenStringsMidi.getOrNull(closestString) ?: (openStringsSemis.getOrElse(closestString) { 0 } + 40)
                        val noteMidi = baseMidi + tappedFret
                        onFretTapped(closestString, tappedFret, noteMidi)
                    }
                }
        ) {
            val w = size.width
            val pillColor = Color(0xFFFF9E1B)

            // 1. Tapered Fretboard Background Polygon
            val fretboardPath = Path().apply {
                moveTo(leftPad, nutTop)
                lineTo(w - rightPad, bridgeTop)
                lineTo(w - rightPad, bridgeBottom)
                lineTo(leftPad, nutBottom)
                close()
            }
            drawPath(
                path = fretboardPath,
                brush = fretboardBrush ?: Brush.verticalGradient(
                    colors = listOf(Color(0xFF2B303C), Color(0xFF20242E), Color(0xFF1B1E26))
                )
            )

            // Fretboard outline
            drawPath(
                path = fretboardPath,
                color = Color(0xFF3E4554).copy(alpha = 0.5f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Nut (Fret 0 divider)
            drawLine(
                color = Color(0xFFE8E8E8),
                start = Offset(leftPad, nutTop),
                end = Offset(leftPad, nutBottom),
                strokeWidth = 4.dp.toPx()
            )

            // Inlays on frets 3, 5, 7, 9, 12
            if (showInlayDots) {
                val inlayColor = Color(0xFF8A7E72).copy(alpha = 0.65f)
                listOf(3, 5, 7, 9, 12).forEach { markerFret ->
                    if (markerFret <= baseFret + numFrets - 1 && markerFret >= baseFret) {
                        val relFret = markerFret - baseFret + 1
                        val inlayX = leftPad + (relFret - 0.5f) * fretW
                        val t = ((inlayX - leftPad) / (w - leftPad - rightPad)).coerceIn(0f, 1f)
                        val currentH = nutH + (bridgeH - nutH) * t
                        val curStringH = currentH / (numStrings - 1).coerceAtLeast(1).toFloat()
                        if (markerFret == 12) {
                            drawCircle(color = inlayColor, radius = 3.5f.dp.toPx(), center = Offset(inlayX, midY - curStringH * 0.75f))
                            drawCircle(color = inlayColor, radius = 3.5f.dp.toPx(), center = Offset(inlayX, midY + curStringH * 0.75f))
                        } else {
                            drawCircle(color = inlayColor, radius = 3.5f.dp.toPx(), center = Offset(inlayX, midY))
                        }
                    }
                }
            }

            // Fret wire dividers following the neck taper
            for (f in 1..numFrets) {
                val x = leftPad + f * fretW
                val t = ((x - leftPad) / (w - leftPad - rightPad)).coerceIn(0f, 1f)
                val curTop = nutTop + (bridgeTop - nutTop) * t
                val curBottom = nutBottom + (bridgeBottom - nutBottom) * t
                drawLine(
                    color = Color(0xFF8D7365).copy(alpha = 0.85f),
                    start = Offset(x, curTop),
                    end = Offset(x, curBottom),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Guitar Strings (E to e) following the taper from nut to bridge
            for (s in 0 until numStrings) {
                val startY = getYForStringAtX(s, leftPad)
                val endY = getYForStringAtX(s, w - rightPad)
                val gauge = 1.0f + (numStrings - 1 - s) * 0.45f
                drawLine(
                    color = Color(0xFFC0C4CC),
                    start = Offset(leftPad, startY),
                    end = Offset(w - rightPad, endY),
                    strokeWidth = gauge.dp.toPx()
                )

                // String Label on left (e, B, G, D, A, E)
                drawContext.canvas.nativeCanvas.drawText(
                    stringLabels[s],
                    leftPad - 24f,
                    startY + 4.5f.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 12.dp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                    }
                )
            }

            // Barre
            if (barreFret != null && barreFret > 0) {
                val relFret = barreFret - baseFret + 1
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

            // Open string indicators / Muted strings at the Nut
            if (chordFrets != null) {
                for (s in 0 until numStrings) {
                    val f = chordFrets.getOrNull(s) ?: -1
                    val y = getYForStringAtX(s, leftPad)
                    val nutIndicatorX = leftPad - 12f
                    if (f == -1) {
                        // Muted string (Red 'X')
                        drawContext.canvas.nativeCanvas.drawText(
                            "✕",
                            nutIndicatorX,
                            y + 4.5f.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.rgb(239, 83, 80)
                                textSize = 13.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                            }
                        )
                    } else if (f == 0) {
                        // Open ringing string (White 'o')
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(nutIndicatorX, y),
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }
            }

            // Active Chord Notes / Fretted positions
            for (s in 0 until numStrings) {
                val openSemi = openStringsSemis.getOrNull(s) ?: 0

                for (f in 1..numFrets) {
                    val absoluteFret = baseFret + f - 1
                    val fretSemi = (openSemi + absoluteFret) % 12
                    val isChordFret = chordFrets != null && chordFrets.getOrNull(s) == absoluteFret
                    val isCurrentlyPlaying = (activeCoordinate != null && activeCoordinate.first == s && activeCoordinate.second == absoluteFret)
                    val isScaleNote = chordFrets == null && normalizedActiveNotes.contains(fretSemi)

                    if (isChordFret || isCurrentlyPlaying || isScaleNote) {
                        val cx = leftPad + (f - 0.5f) * fretW
                        val cy = getYForStringAtX(s, cx)
                        val dotRadius = 11.5.dp.toPx()

                        // Glow effect
                        drawCircle(
                            color = pillColor.copy(alpha = 0.35f),
                            radius = dotRadius + 3.dp.toPx(),
                            center = Offset(cx, cy)
                        )

                        // Filled warm orange pill
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFB74D), pillColor),
                                center = Offset(cx, cy),
                                radius = dotRadius
                            ),
                            radius = dotRadius,
                            center = Offset(cx, cy)
                        )

                        // Text inside pill (Finger number or Note name)
                        val fingerNum = if (isChordFret) chordFingers?.getOrNull(s) else null
                        val displayText = if (fingerNum != null && fingerNum > 0) "$fingerNum" else NOTE_NAMES[fretSemi]

                        drawContext.canvas.nativeCanvas.drawText(
                            displayText,
                            cx,
                            cy + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 11.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }

        // Fret Numbers at the bottom (3, 5, 7, 9, 12)
        listOf(3, 5, 7, 9, 12).filter { it <= baseFret + numFrets - 1 && it >= baseFret }.forEach { f ->
            val relF = f - baseFret + 1
            val x = leftPad + (relF - 0.5f) * fretW
            drawContext.canvas.nativeCanvas.drawText(
                "$f",
                x,
                size.height - 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(200, 180, 186, 198)
                    textSize = 11.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
            )
        }
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
    startNote: Int = 21,
    endNote: Int = 108,
    fillWidth: Boolean = false,
    onKeyTapped: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pillAmberColor = Color(0xFFFF9E1B)

    val baseWhiteStart = Color(0xFFFAF7F0)
    val baseWhiteMid = Color(0xFFF4EFE6)
    val baseWhiteEnd = Color(0xFFEDE5D8)

    val baseBlackStart = Color(0xFF333333)
    val baseBlackMid = Color(0xFF1C1C1C)
    val baseBlackEnd = Color(0xFF0A0A0A)

    val normalizedActiveMidi = remember(activeMidiNotes) {
        activeMidiNotes.filter { it.value }
    }

    val whiteMidiNotes = remember(startNote, endNote) {
        (startNote..endNote).filter { n -> (n % 12) !in listOf(1, 3, 6, 8, 10) }
    }

    val maxWhiteKeyWidthPx = with(density) { 42.dp.toPx() }

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
                    detectTapGestures { tapOffset ->
                        if (onKeyTapped == null) return@detectTapGestures
                        val adjustedX = tapOffset.x - startPaddingOffset
                        val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f
                        val blackKeyHeightPx = size.height * 0.62f

                        var triggeredMidi: Int? = null
                        if (tapOffset.y < blackKeyHeightPx) {
                            for (i in whiteMidiNotes.indices) {
                                val midiNote = whiteMidiNotes[i]
                                if ((midiNote % 12) in listOf(0, 2, 5, 7, 9)) {
                                    val blackX = i * finalWhiteKeyWidthPx + finalWhiteKeyWidthPx - (blackKeyWidthPx / 2f)
                                    if (adjustedX >= blackX && adjustedX <= (blackX + blackKeyWidthPx)) {
                                        triggeredMidi = midiNote + 1
                                        break
                                    }
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
            val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f
            val blackKeyHeightPx = totalHeight * 0.62f
            val cornerRadiusPx = 4.dp.toPx()

            translate(left = startPaddingOffset) {
                // 1. Draw Ivory White Keys
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    val isCurrentlyPlaying = activeCoordinate != null && (midiNote == activeCoordinate || (activeCoordinate < 12 && (midiNote % 12) == activeCoordinate) || (activeCoordinate >= 12 && (midiNote % 12) == (activeCoordinate % 12) && (activeCoordinate !in startNote..endNote)))
                    val isExplicitActive = normalizedActiveMidi[midiNote] == true ||
                            (activeCoordinate != null && activeCoordinate < 12 && (midiNote % 12) == activeCoordinate) ||
                            fingering?.notes?.contains(midiNote) == true ||
                            isCurrentlyPlaying

                    val x = index * finalWhiteKeyWidthPx
                    val fingerIndex = fingering?.notes?.indexOf(midiNote) ?: -1
                    val fingerNum = if (fingerIndex != -1) {
                        fingering?.fingers?.getOrNull(fingerIndex)
                    } else if (fingering == null) {
                        // Fallback only if no fingering data was provided at all
                        when (midiNote % 12) {
                            (rootNote % 12) -> 1
                            (rootNote + 4) % 12 -> 3
                            (rootNote + 7) % 12 -> 5
                            else -> null
                        }
                    } else null

                    // Key Background: Distinct highlight for active/sounding key vs chord keys
                    drawRoundRect(
                        brush = when {
                            isCurrentlyPlaying -> Brush.verticalGradient(
                                listOf(Color(0xFFFF7043), Color(0xFFFF5722), Color(0xFFE64A19))
                            )
                            isExplicitActive -> Brush.verticalGradient(
                                listOf(Color(0xFFFFF176), Color(0xFFFFD54F), Color(0xFFFFCA28))
                            )
                            else -> Brush.verticalGradient(
                                listOf(baseWhiteStart, baseWhiteMid, baseWhiteEnd)
                            )
                        },
                        topLeft = Offset(x + 1f, 0f),
                        size = Size(finalWhiteKeyWidthPx - 2f, totalHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )

                    // Key Border
                    drawRoundRect(
                        color = Color(0xFF1E222B).copy(alpha = 0.5f),
                        topLeft = Offset(x + 1f, 0f),
                        size = Size(finalWhiteKeyWidthPx - 2f, totalHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    val centerX = x + finalWhiteKeyWidthPx / 2f

                    // Octave markers (C3, C4, C5) on bottom of C keys
                    if (midiNote % 12 == 0) {
                        val octaveNumber = (midiNote / 12) - 1
                        drawContext.canvas.nativeCanvas.drawText(
                            "C$octaveNumber",
                            centerX,
                            totalHeight - 8.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(170, 120, 125, 135)
                                textSize = 9.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                            }
                        )
                    }

                    // Active Note Pill with Finger Number (or Note Name fallback)
                    if (isExplicitActive) {
                        val badgeY = totalHeight - 20.dp.toPx()
                        val badgeRadius = if (isCurrentlyPlaying) 11.5.dp.toPx() else 10.dp.toPx()

                        // Circular pill with finger number or note name
                        drawCircle(
                            color = if (isCurrentlyPlaying) Color(0xFFFFEB3B) else Color.White,
                            radius = badgeRadius,
                            center = Offset(centerX, badgeY)
                        )

                        val displayNum = if (fingerNum != null && fingerNum > 0) {
                            fingerNum.toString()
                        } else {
                            NOTE_NAMES[midiNote % 12]
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            displayNum,
                            centerX,
                            badgeY + 3.5.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = if (isCurrentlyPlaying) android.graphics.Color.rgb(190, 40, 0) else android.graphics.Color.rgb(230, 110, 0)
                                textSize = 10.5.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                            }
                        )
                    }
                }

                // 2. Draw Black Keys
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    if ((midiNote % 12) in listOf(0, 2, 5, 7, 9)) {
                        val blackMidiNote = midiNote + 1
                        val isCurrentlyPlaying = activeCoordinate != null && (blackMidiNote == activeCoordinate || (activeCoordinate < 12 && (blackMidiNote % 12) == activeCoordinate) || (activeCoordinate >= 12 && (blackMidiNote % 12) == (activeCoordinate % 12) && (activeCoordinate !in startNote..endNote)))
                        val isExplicitActive = normalizedActiveMidi[blackMidiNote] == true ||
                                (activeCoordinate != null && activeCoordinate < 12 && (blackMidiNote % 12) == activeCoordinate) ||
                                fingering?.notes?.contains(blackMidiNote) == true ||
                                isCurrentlyPlaying

                        val blackX = (index * finalWhiteKeyWidthPx) + (finalWhiteKeyWidthPx - blackKeyWidthPx / 2f)
                        val fingerIndex = fingering?.notes?.indexOf(blackMidiNote) ?: -1
                        val fingerNum = if (fingerIndex != -1) fingering?.fingers?.getOrNull(fingerIndex) else null

                        // Black Key Shadow
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(blackX + 1.5f, 2f),
                            size = Size(blackKeyWidthPx, blackKeyHeightPx + 2f),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )

                        // Black Key Body: Deep amber-golden vivid yellow for active black keys, red-orange for currently playing
                        drawRoundRect(
                            brush = when {
                                isCurrentlyPlaying -> Brush.verticalGradient(
                                    listOf(Color(0xFFFF7043), Color(0xFFFF5722), Color(0xFFE64A19))
                                )
                                isExplicitActive -> Brush.verticalGradient(
                                    listOf(Color(0xFFFFB300), Color(0xFFFF8F00), Color(0xFFFF6F00))
                                )
                                else -> Brush.verticalGradient(
                                    listOf(baseBlackStart, baseBlackMid, baseBlackEnd)
                                )
                            },
                            topLeft = Offset(blackX, 0f),
                            size = Size(blackKeyWidthPx, blackKeyHeightPx),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )

                        val centerX = blackX + blackKeyWidthPx / 2f
                        if (isExplicitActive) {
                            val badgeY = blackKeyHeightPx - 16.dp.toPx()
                            val badgeRadius = if (isCurrentlyPlaying) 10.dp.toPx() else 8.5.dp.toPx()

                            drawCircle(
                                color = if (isCurrentlyPlaying) Color(0xFFFFEB3B) else Color.White,
                                radius = badgeRadius,
                                center = Offset(centerX, badgeY)
                            )

                            val displayNum = if (fingerNum != null && fingerNum > 0) {
                                fingerNum.toString()
                            } else {
                                NOTE_NAMES[blackMidiNote % 12]
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                displayNum,
                                centerX,
                                badgeY + 3.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = if (isCurrentlyPlaying) android.graphics.Color.rgb(190, 40, 0) else android.graphics.Color.rgb(230, 110, 0)
                                    textSize = 9.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    isAntiAlias = true
                                }
                            )
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
    }
}