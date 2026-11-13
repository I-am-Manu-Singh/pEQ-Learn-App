package com.creativeblisslabs.peqlearn.app.ui.components

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.screens.FingeringData
import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val SARGAM_NAMES = listOf("Sa", "re", "Re", "ga", "Ga", "Ma", "ma", "Pa", "dha", "Dha", "ni", "Ni")

/**
 * Helper to compute aspect-ratio-safe viewport bounds inside any arbitrary Canvas size.
 * Guarantees that instruments never stretch into distorted proportions or clip vertically/horizontally
 * when resized via external Modifiers.
 */
private data class ViewportFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centerX: Float get() = left + width * 0.5f
    val centerY: Float get() = top + height * 0.5f
}

private fun computeAspectSafeViewport(
    canvasWidth: Float,
    canvasHeight: Float,
    minAspect: Float,
    maxAspect: Float,
    referenceHeight: Float = 160f,
    horizontalPaddingFrac: Float = 0.015f,
    verticalPaddingFrac: Float = 0.03f
): ViewportFrame {
    val safeW = (canvasWidth * (1f - horizontalPaddingFrac * 2f)).coerceAtLeast(1f)
    val safeH = (canvasHeight * (1f - verticalPaddingFrac * 2f)).coerceAtLeast(1f)
    val currentAspect = safeW / safeH

    val fittedW: Float
    val fittedH: Float
    when {
        currentAspect > maxAspect -> {
            fittedH = safeH
            fittedW = (safeH * maxAspect).coerceAtMost(safeW)
        }
        currentAspect < minAspect -> {
            fittedW = safeW
            fittedH = (safeW / minAspect).coerceAtMost(safeH)
        }
        else -> {
            fittedW = safeW
            fittedH = safeH
        }
    }

    val left = (canvasWidth - fittedW) * 0.5f
    val top = (canvasHeight - fittedH) * 0.5f
    val scale = (fittedH / referenceHeight).coerceIn(0.42f, 2.5f)
    return ViewportFrame(left, top, fittedW, fittedH, scale)
}

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
    cagedMode: String = "AUTO", // "AUTO", "C", "A", "G", "E", "D", or "ALL"
    fretMinConstrain: Int = 0,
    fretMaxConstrain: Int = 24,
    onFretTapped: ((stringIndex: Int, fret: Int, midiNote: Int) -> Unit)? = null,
    onFretReleased: ((stringIndex: Int, fret: Int, midiNote: Int) -> Unit)? = null,
    fretboardBrush: Brush? = null,
    showInlayDots: Boolean = true,
    modifier: Modifier = Modifier
) {
    val numStrings = stringLabels.size.coerceAtLeast(1)
    val safeNumFrets = numFrets.coerceIn(4, 24)
    val safeBaseFret = baseFret.coerceIn(1, 24)

    val filteredActiveNotes = remember(activeNotes, isLeftHandHidden, isRightHandHidden) {
        activeNotes.filter { midi ->
            val isTreble = midi >= 60
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }
    }
    val normalizedActiveNotes = remember(filteredActiveNotes) { filteredActiveNotes.map { (it % 12 + 12) % 12 }.toSet() }
    val effectiveActiveMidiNote = remember(activeMidiNote, isLeftHandHidden, isRightHandHidden) {
        if (activeMidiNote == null) null
        else {
            val isTreble = activeMidiNote >= 60
            if (isTreble && isRightHandHidden) null
            else if (!isTreble && isLeftHandHidden) null
            else activeMidiNote
        }
    }

    val defaultOpenStringsMidi = remember(numStrings) {
        when (numStrings) {
            4 -> listOf(69, 64, 60, 67) // Ukulele A4, E4, C4, G4
            7 -> listOf(64, 59, 55, 50, 45, 40, 35) // 7-String Guitar: e4, B3, G3, D3, A2, E2, B1
            else -> listOf(64, 59, 55, 50, 45, 40) // 6-String Guitar: e4, B3, G3, D3, A2, E2
        }
    }

    val allUniquePitches = remember(filteredActiveNotes, effectiveActiveMidiNote) {
        (filteredActiveNotes + listOfNotNull(effectiveActiveMidiNote)).distinct()
    }
    val distinctPitchClasses = remember(allUniquePitches) {
        allUniquePitches.map { (it % 12 + 12) % 12 }.toSet()
    }
    val isScaleMode = remember(distinctPitchClasses) {
        distinctPitchClasses.size >= 6
    }

    val effectiveResolvedChordFrets = remember(chordFrets, allUniquePitches, distinctPitchClasses, isScaleMode, cagedMode, numStrings) {
        if (chordFrets != null) {
            chordFrets
        } else if (isScaleMode) {
            null
        } else if (allUniquePitches.size in 2..5 && numStrings >= 6) {
            val detected = com.creativeblisslabs.peqlearn.app.util.LiveChordDetector.detect(allUniquePitches)
            if (detected != null && detected.chordQuality != "Note") {
                val root = detected.rootName.replace("♯", "#").replace("♭", "b")
                val quality = when (detected.chordQuality) {
                    "Major", "" -> "major"
                    "Minor", "m" -> "minor"
                    "7", "Dominant 7th" -> "7"
                    "maj7", "Major 7th" -> "maj7"
                    "m7", "Minor 7th" -> "m7"
                    "dim", "Diminished" -> "dim"
                    "dim7" -> "dim7"
                    "sus4", "Suspended 4th" -> "sus4"
                    "sus2", "Suspended 2nd" -> "sus2"
                    "5", "Power Chord" -> "5"
                    else -> detected.chordQuality.lowercase()
                }
                val dbPositions = com.creativeblisslabs.peqlearn.app.util.RealChordsDb.getGuitarPositions(root, quality)
                val filteredByCaged = if (cagedMode != "AUTO" && cagedMode != "ALL") {
                    dbPositions.filter { pos ->
                        when (cagedMode) {
                            "C" -> (pos.frets.getOrNull(1) ?: -1) > 0 && (pos.frets.getOrNull(0) ?: -1) <= 0
                            "A" -> (pos.frets.getOrNull(1) ?: -1) >= 0 && (pos.frets.getOrNull(2) ?: -1) >= 0
                            "G" -> (pos.frets.getOrNull(0) ?: -1) > 0 && (pos.frets.getOrNull(5) ?: -1) > 0
                            "E" -> (pos.frets.getOrNull(0) ?: -1) >= 0 && (pos.frets.getOrNull(1) ?: -1) >= 0
                            "D" -> (pos.frets.getOrNull(0) ?: -1) < 0 && (pos.frets.getOrNull(1) ?: -1) < 0
                            else -> true
                        }
                    }
                } else dbPositions

                val bestPos = filteredByCaged.firstOrNull() ?: dbPositions.firstOrNull()
                if (bestPos != null) {
                    val base6 = bestPos.frets.reversed()
                    if (numStrings == 7) base6 + listOf(-1) else base6
                } else null
            } else null
        } else null
    }

   // Stateful hand-box center fret memory (initialized to Fret 3 for natural low-neck playability)
    var lastHandBoxCenterFret by remember { mutableFloatStateOf(3.0f) }

    val activeFretPoints = remember(
        allUniquePitches,
        effectiveResolvedChordFrets,
        defaultOpenStringsMidi,
        isScaleMode,
        fretMinConstrain,
        fretMaxConstrain,
        safeNumFrets,
        safeBaseFret
    ) {
        if (effectiveResolvedChordFrets != null) {
            emptySet()
        } else if (allUniquePitches.isEmpty()) {
            emptySet()
        } else {
            val maxAllowedFret = (safeBaseFret + safeNumFrets - 1).coerceAtMost(fretMaxConstrain)
            val minAllowedFret = fretMinConstrain.coerceAtLeast(0)
            val sortedPitches = allUniquePitches.sorted()

            // Test candidate 4-fret hand-box windows [boxStart .. boxStart+3] across the neck (frets 1..15)
            // to find the single hand position that plays the maximum number of notes with zero >4-fret stretches!
            var bestBoxAssignment = emptySet<Pair<Int, Int>>()
            var bestBoxCost = Float.MAX_VALUE
            var bestBoxCenter = lastHandBoxCenterFret

            val maxBoxStart = (maxAllowedFret - 3).coerceAtLeast(1)
            for (boxStart in 1..maxBoxStart) {
                val boxEnd = boxStart + 3
                val boxCenter = boxStart + 1.5f
                val usedStrings = mutableSetOf<Int>()
                val assignment = mutableSetOf<Pair<Int, Int>>()
                var boxCost = abs(boxCenter - lastHandBoxCenterFret) * 4.5f + (boxStart - 1) * 2.2f

                for (midi in sortedPitches) {
                    // Find all (stringIndex, fret) candidates for this MIDI pitch
                    val candidates = defaultOpenStringsMidi.mapIndexedNotNull { sIdx, openMidi ->
                        val fret = midi - openMidi
                        if (fret in minAllowedFret..maxAllowedFret) sIdx to fret else null
                    }
                    if (candidates.isEmpty()) continue

                    val available = candidates.filter { it.first !in usedStrings }.ifEmpty { candidates }
                    val bestForNote = available.minByOrNull { (sIdx, fret) ->
                        val stretchOutsideBox = when {
                            fret == 0 -> if (boxStart <= 4) 0 else 18 // Open strings pair naturally with low positions (frets 1..4)
                            fret < boxStart -> (boxStart - fret) * 35
                            fret > boxEnd -> (fret - boxEnd) * 35
                            else -> 0
                        }
                        val woundBassHighFretPenalty = if (sIdx >= 4 && fret > 7) 40 else if (sIdx == 3 && fret > 9) 20 else 0
                        val stringReusePenalty = if (sIdx in usedStrings) 120 else 0
                        stretchOutsideBox + woundBassHighFretPenalty + stringReusePenalty + fret
                    }

                    if (bestForNote != null) {
                        assignment.add(bestForNote)
                        usedStrings.add(bestForNote.first)
                        val f = bestForNote.second
                        if (f > 0 && (f < boxStart || f > boxEnd)) {
                            boxCost += abs(f - boxCenter) * 40f // Heavy penalty for stretching outside the 4-fret box
                        }
                    }
                }

                // Enforce anatomical 4-finger span across all fretted notes in the chord
                val fretted = assignment.map { it.second }.filter { it > 0 }
                val span = if (fretted.size > 1) fretted.max() - fretted.min() else 0
                if (span > 4) boxCost += (span - 4) * 200f
                if (span == 4) boxCost += 25f

                // Reward capturing more distinct strings
                boxCost -= assignment.size * 30f

                if (boxCost < bestBoxCost) {
                    bestBoxCost = boxCost
                    bestBoxAssignment = assignment
                    if (fretted.isNotEmpty()) {
                        bestBoxCenter = fretted.average().toFloat()
                    }
                }
            }

            if (bestBoxAssignment.any { it.second > 0 }) {
                lastHandBoxCenterFret = bestBoxCenter.coerceIn(1.5f, 12f)
            }
            bestBoxAssignment
        }
    }

    val normalizedRootNote = remember(rootNote) { (rootNote % 12 + 12) % 12 }

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
    val permanentOctavePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(120, 160, 168, 185)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }
    val defaultNeckBrush = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFF2B303C), Color(0xFF20242E), Color(0xFF1B1E26)))
    }

    val r = 0.056125687f
    val fretScaleFactor = remember(safeNumFrets, safeBaseFret) {
        var sum = 0f
        for (f in 1..safeNumFrets) {
            val absoluteFret = safeBaseFret + f - 1
            val lengthFraction = (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
            sum += lengthFraction
        }
        sum.coerceAtLeast(0.001f)
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1218).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF2E3544).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        val totalWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val totalHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val vp = remember(totalWidth, totalHeight) {
            computeAspectSafeViewport(
                canvasWidth = totalWidth,
                canvasHeight = totalHeight,
                minAspect = 2.0f,
                maxAspect = 11.5f,
                referenceHeight = 165f,
                horizontalPaddingFrac = 0.01f,
                verticalPaddingFrac = 0.02f
            )
        }

        val s = vp.scale
        val leftPad = vp.left + (44f * s).coerceIn(28f, vp.width * 0.16f)
        val rightPad = (totalWidth - vp.right) + (16f * s).coerceIn(10f, vp.width * 0.06f)
        val topPad = vp.top + (12f * s).coerceAtLeast(6f)
        val bottomPad = (totalHeight - vp.bottom) + (22f * s).coerceAtLeast(14f)

        val playableW = (totalWidth - leftPad - rightPad).coerceAtLeast(1f)
        val playableH = (totalHeight - topPad - bottomPad).coerceAtLeast(1f)

        val fretXPositions = remember(playableW, leftPad, safeNumFrets, safeBaseFret, fretScaleFactor) {
            val list = mutableListOf<Float>()
            var accumulated = 0f
            for (f in 1..safeNumFrets) {
                val absoluteFret = safeBaseFret + f - 1
                val lengthFraction = (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
                accumulated += (lengthFraction / fretScaleFactor) * playableW
                list.add(leftPad + accumulated)
            }
            list
        }

        fun getFretCenterX(f: Int): Float {
            val start = if (f <= 1) leftPad else fretXPositions[f - 2]
            val end = fretXPositions.getOrElse(f - 1) { totalWidth - rightPad }
            return (start + end) / 2f
        }

        val nutH = playableH * 0.88f
        val bridgeH = playableH * 0.98f
        val midY = topPad + playableH / 2f
        val nutTop = midY - nutH / 2f
        val nutBottom = midY + nutH / 2f
        val bridgeTop = midY - bridgeH / 2f
        val bridgeBottom = midY + bridgeH / 2f

        fun getYForStringAtX(strIdx: Int, x: Float): Float {
            val t = ((x - leftPad) / playableW).coerceIn(0f, 1f)
            val currentH = nutH + (bridgeH - nutH) * t
            val currentTop = midY - currentH / 2f
            val curStringH = currentH / (numStrings - 1).coerceAtLeast(1).toFloat()
            return if (numStrings == 1) midY else currentTop + strIdx * curStringH
        }

        val fretboardPath = remember(leftPad, rightPad, totalWidth, nutTop, nutBottom, bridgeTop, bridgeBottom) {
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
                .then(
                    if (onFretTapped != null || onFretReleased != null) {
                        Modifier
                .pointerInput(safeNumFrets, numStrings, safeBaseFret, leftPad, rightPad, playableW, nutH, bridgeH, midY) {
                    val activeFretPointers = mutableMapOf<Long, Triple<Int, Int, Int>>()

                    fun resolveFretAt(offset: Offset): Triple<Int, Int, Int> {
                        val tappedFret = if (offset.x < leftPad) {
                            0
                        } else {
                            var matchedFret = safeNumFrets
                            for (f in 1..safeNumFrets) {
                                val wireX = fretXPositions.getOrElse(f - 1) { totalWidth - rightPad }
                                if (offset.x <= wireX) {
                                    matchedFret = f
                                    break
                                }
                            }
                            (safeBaseFret + matchedFret - 1).coerceIn(0, 24)
                        }

                        var closestString = 0
                        var minDistance = Float.MAX_VALUE
                        for (strIdx in 0 until numStrings) {
                            val strY = getYForStringAtX(strIdx, offset.x)
                            val dist = abs(offset.y - strY)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestString = strIdx
                            }
                        }

                        val baseMidi = defaultOpenStringsMidi.getOrNull(closestString)
                            ?: (openStringsSemis.getOrElse(closestString) { 0 } + 40)
                        val noteMidi = baseMidi + tappedFret
                        return Triple(closestString, tappedFret, noteMidi)
                    }

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                val id = change.id.value
                                if (change.pressed) {
                                    val item = resolveFretAt(change.position)
                                    val prev = activeFretPointers[id]
                                    if (prev == null) {
                                        activeFretPointers[id] = item
                                        onFretTapped?.invoke(item.first, item.second, item.third)
                                    } else if (prev != item) {
                                        onFretReleased?.invoke(prev.first, prev.second, prev.third)
                                        activeFretPointers[id] = item
                                        onFretTapped?.invoke(item.first, item.second, item.third)
                                    }
                                } else {
                                    activeFretPointers.remove(id)?.let { released ->
                                        onFretReleased?.invoke(released.first, released.second, released.third)
                                    }
                                }
                            }
                        }
                    }
                }
                    } else {
                        Modifier
                    }
                )
        ) {
            val w = size.width
            val pillColor = Color(0xFFFF9E1B)
            val stringSpacingAtNut = (nutH / (numStrings - 1).coerceAtLeast(1)).coerceAtLeast(8f)

            drawPath(
                path = fretboardPath,
                color = Color.Black.copy(alpha = 0.55f),
                style = Stroke(width = (6f * s).coerceAtLeast(2f))
            )
            drawPath(
                path = fretboardPath,
                color = Color.Black.copy(alpha = 0.35f),
                style = Stroke(width = (3f * s).coerceAtLeast(1.5f))
            )

            drawPath(path = fretboardPath, brush = fretboardBrush ?: defaultNeckBrush)
            drawPath(path = fretboardPath, color = Color(0xFF3E4554).copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))

            // Nut
            drawLine(
                color = Color(0xFFE8E8E8),
                start = Offset(leftPad, nutTop),
                end = Offset(leftPad, nutBottom),
                strokeWidth = (4f * s).coerceIn(2f, 6f)
            )

            // Inlays
            if (showInlayDots) {
                val inlayColor = Color(0xFF8A7E72).copy(alpha = 0.65f)
                val dotRad = (stringSpacingAtNut * 0.18f).coerceIn(2f, 5.5f.dp.toPx())
                for (markerFret in intArrayOf(3, 5, 7, 9, 12, 15, 17, 19, 21, 24)) {
                    if (markerFret in safeBaseFret..(safeBaseFret + safeNumFrets - 1)) {
                        val relFret = markerFret - safeBaseFret + 1
                        val inlayX = getFretCenterX(relFret)
                        val t = ((inlayX - leftPad) / playableW).coerceIn(0f, 1f)
                        val currentH = nutH + (bridgeH - nutH) * t
                        val curStringH = currentH / (numStrings - 1).coerceAtLeast(1).toFloat()
                        if (markerFret == 12 || markerFret == 24) {
                            drawCircle(color = inlayColor, radius = dotRad, center = Offset(inlayX, midY - curStringH * 0.75f))
                            drawCircle(color = inlayColor, radius = dotRad, center = Offset(inlayX, midY + curStringH * 0.75f))
                        } else {
                            drawCircle(color = inlayColor, radius = dotRad, center = Offset(inlayX, midY))
                        }
                    }
                }
            }

            // Fret wires
            val fretStroke = (1.6f * s).coerceIn(1f, 3f)
            for (f in 1..safeNumFrets) {
                val x = fretXPositions.getOrElse(f - 1) { w - rightPad }
                val t = ((x - leftPad) / playableW).coerceIn(0f, 1f)
                val curTop = nutTop + (bridgeTop - nutTop) * t
                val curBottom = nutBottom + (bridgeBottom - nutBottom) * t
                drawLine(
                    color = Color.Black.copy(alpha = 0.45f),
                    start = Offset(x + 1f, curTop),
                    end = Offset(x + 1f, curBottom),
                    strokeWidth = fretStroke
                )
                drawLine(
                    color = Color(0xFFC5C9D3).copy(alpha = 0.9f),
                    start = Offset(x, curTop),
                    end = Offset(x, curBottom),
                    strokeWidth = fretStroke
                )
            }

            // Strings
            stringLabelPaint.textSize = (stringSpacingAtNut * 0.55f).coerceIn(8.dp.toPx(), 13.dp.toPx())
            val labelYOff = stringLabelPaint.textSize * 0.36f
            val nutIndicatorX = leftPad - (12f * s).coerceIn(8f, 18f)
            val labelX = nutIndicatorX - (11f * s).coerceIn(7f, 16f)

            for (strIdx in 0 until numStrings) {
                val startY = getYForStringAtX(strIdx, leftPad)
                val endY = getYForStringAtX(strIdx, w - rightPad)
                val gauge = ((1.0f + strIdx * 0.42f) * s).coerceIn(0.8f, 4.5f)
                drawLine(
                    color = Color(0xFFC0C4CC),
                    start = Offset(leftPad, startY),
                    end = Offset(w - rightPad, endY),
                    strokeWidth = gauge
                )
                drawContext.canvas.nativeCanvas.drawText(
                    stringLabels.getOrElse(strIdx) { "" },
                    labelX,
                    startY + labelYOff,
                    stringLabelPaint
                )
            }

            // Barre
            if (barreFret != null && barreFret > 0) {
                val relFret = if (barreFret >= safeBaseFret) barreFret - safeBaseFret + 1 else barreFret
                if (relFret in 1..safeNumFrets) {
                    val barreX = getFretCenterX(relFret)
                    val t = ((barreX - leftPad) / playableW).coerceIn(0f, 1f)
                    val curTop = nutTop + (bridgeTop - nutTop) * t
                    val curH = (nutBottom + (bridgeBottom - nutBottom) * t) - curTop
                    val barreW = (stringSpacingAtNut * 0.95f).coerceIn(10f, 24.dp.toPx())
                    drawRoundRect(
                        color = pillColor.copy(alpha = 0.85f),
                        topLeft = Offset(barreX - barreW * 0.5f, curTop - 2f),
                        size = Size(barreW, curH + 4f),
                        cornerRadius = CornerRadius(barreW * 0.5f)
                    )
                }
            }

            // Permanent C Octave Markers
            if (markOctaves || highlightOctaveC) {
                permanentOctavePaint.textSize = (stringSpacingAtNut * 0.38f).coerceIn(6.5f.dp.toPx(), 9.5f.dp.toPx())
                for (strIdx in 0 until numStrings) {
                    val openSemi = openStringsSemis.getOrNull(strIdx) ?: 0
                    val baseStringMidi = defaultOpenStringsMidi.getOrNull(strIdx) ?: (openSemi + 40)
                    for (f in 1..safeNumFrets) {
                        val absoluteFret = safeBaseFret + f - 1
                        val fretSemi = (openSemi + absoluteFret) % 12
                        if (fretSemi == 0) {
                            val fullMidiNote = baseStringMidi + absoluteFret
                            val octaveNum = (fullMidiNote / 12) - 1
                            val cx = getFretCenterX(f)
                            val cy = getYForStringAtX(strIdx, cx)
                            drawContext.canvas.nativeCanvas.drawText(
                                "C$octaveNum",
                                cx,
                                cy + permanentOctavePaint.textSize * 0.95f,
                                permanentOctavePaint
                            )
                        }
                    }
                }
            }

            // Open string indicators
            val maxBadgeRad = (stringSpacingAtNut * 0.44f).coerceIn(5.dp.toPx(), 13.dp.toPx())
            nutMutedPaint.textSize = (maxBadgeRad * 1.3f).coerceIn(8.dp.toPx(), 14.dp.toPx())
            val activeChordFrets = effectiveResolvedChordFrets

            if (activeChordFrets != null) {
                for (strIdx in 0 until numStrings) {
                    val f = activeChordFrets.getOrNull(strIdx) ?: -1
                    val y = getYForStringAtX(strIdx, leftPad)
                    val openSemi = ((openStringsSemis.getOrNull(strIdx) ?: 0) % 12 + 12) % 12
                    val openMidi = defaultOpenStringsMidi.getOrNull(strIdx) ?: (openSemi + 40)
                    val isOpenPlaying = f == 0 && (
                        (activeCoordinate != null && activeCoordinate.first == strIdx && activeCoordinate.second == 0) ||
                            (effectiveActiveMidiNote != null && openMidi == effectiveActiveMidiNote)
                        )

                    if (f == -1) {
                        drawContext.canvas.nativeCanvas.drawText("✕", nutIndicatorX, y + labelYOff, nutMutedPaint)
                    } else if (f == 0) {
                        val nutRad = if (isOpenPlaying) maxBadgeRad * 0.85f else maxBadgeRad * 0.72f
                        drawCircle(
                            color = if (isOpenPlaying) Color(0xFFFFEB3B) else Color(0xFF1E222B).copy(alpha = 0.9f),
                            radius = nutRad,
                            center = Offset(nutIndicatorX, y)
                        )
                        drawCircle(
                            color = if (isOpenPlaying) Color.White else Color(0xFF9E9E9E),
                            radius = nutRad,
                            center = Offset(nutIndicatorX, y),
                            style = Stroke(width = if (isOpenPlaying) 2.dp.toPx() else 1.2.dp.toPx())
                        )
                        val nutText = NOTE_NAMES[openSemi]
                        notePillTextPaint.color = if (isOpenPlaying) android.graphics.Color.rgb(180, 40, 0) else android.graphics.Color.WHITE
                        notePillTextPaint.textSize = (nutRad * 1.05f).coerceIn(6.dp.toPx(), 10.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(nutText, nutIndicatorX, y + notePillTextPaint.textSize * 0.36f, notePillTextPaint)
                    }
                }
            } else {
                for (strIdx in 0 until numStrings) {
                    val openSemi = ((openStringsSemis.getOrNull(strIdx) ?: 0) % 12 + 12) % 12
                    val isExactOpenActive = (activeCoordinate != null && activeCoordinate.first == strIdx && activeCoordinate.second == 0) ||
                        activeFretPoints.contains(strIdx to 0)
                    val isScaleOpen = isScaleMode && normalizedActiveNotes.contains(openSemi)

                    if (isExactOpenActive || isScaleOpen) {
                        val y = getYForStringAtX(strIdx, leftPad)
                        val isRoot = openSemi == normalizedRootNote
                        val nutRad = if (isExactOpenActive) maxBadgeRad * 0.88f else maxBadgeRad * 0.72f

                        drawCircle(
                            color = when {
                                isExactOpenActive -> Color(0xFFFFEB3B)
                                isRoot -> Color(0xFFFF5252)
                                else -> accentColor
                            },
                            radius = nutRad,
                            center = Offset(nutIndicatorX, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = nutRad,
                            center = Offset(nutIndicatorX, y),
                            style = Stroke(width = if (isExactOpenActive) 2.dp.toPx() else 1.2.dp.toPx())
                        )
                        val nutText = NOTE_NAMES[openSemi]
                        notePillTextPaint.color = if (isExactOpenActive) android.graphics.Color.rgb(180, 40, 0) else android.graphics.Color.WHITE
                        notePillTextPaint.textSize = (nutRad * 1.05f).coerceIn(6.dp.toPx(), 10.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(nutText, nutIndicatorX, y + notePillTextPaint.textSize * 0.36f, notePillTextPaint)
                    }
                }
            }

            // Active Fretted Notes
            for (strIdx in 0 until numStrings) {
                val openSemi = openStringsSemis.getOrNull(strIdx) ?: 0
                val stringChordFret = activeChordFrets?.getOrNull(strIdx) ?: -1
                val baseStringMidi = defaultOpenStringsMidi.getOrNull(strIdx) ?: (openSemi + 40)

                for (f in 1..safeNumFrets) {
                    val absoluteFret = safeBaseFret + f - 1
                    val fretSemi = ((openSemi + absoluteFret) % 12 + 12) % 12
                    val fullMidiNote = baseStringMidi + absoluteFret
                    val fretWidth = (fretXPositions.getOrElse(f - 1) { w - rightPad }) - (if (f <= 1) leftPad else fretXPositions[f - 2])

                    val isChordFret = activeChordFrets != null && stringChordFret > 0 && (
                        stringChordFret == absoluteFret || (safeBaseFret > 1 && stringChordFret == f)
                        )
                    val matchesCoord = activeCoordinate != null && activeCoordinate.first == strIdx && (
                        activeCoordinate.second == absoluteFret || activeCoordinate.second == f
                        )
                    val isExactMidiPlaying = effectiveActiveMidiNote != null && fullMidiNote == effectiveActiveMidiNote
                    val isFretPointActive = activeChordFrets == null && activeFretPoints.contains(strIdx to absoluteFret)
                    val isCurrentlyPlaying = matchesCoord || (if (activeChordFrets != null) isChordFret && isExactMidiPlaying else isFretPointActive)

                    val isScaleFret = isScaleMode && normalizedActiveNotes.contains(fretSemi) && (absoluteFret in fretMinConstrain..fretMaxConstrain)
                    val isRoot = fretSemi == normalizedRootNote

                    if (isChordFret || isCurrentlyPlaying || isScaleFret) {
                        val cx = getFretCenterX(f)
                        val cy = getYForStringAtX(strIdx, cx)
                        val cellMaxRad = min(maxBadgeRad, (fretWidth * 0.44f).coerceAtLeast(4.dp.toPx()))
                        val dotRadius = when {
                            isCurrentlyPlaying -> cellMaxRad
                            isScaleFret -> cellMaxRad * 0.82f
                            else -> cellMaxRad * 0.90f
                        }

                        val primaryDotColor = when {
                            isCurrentlyPlaying -> Color(0xFFFFEB3B)
                            isRoot && isScaleFret -> Color(0xFFFF5252)
                            else -> pillColor
                        }

                        drawCircle(
                            color = if (isCurrentlyPlaying) Color(0xFFFFEB3B).copy(alpha = 0.55f) else primaryDotColor.copy(alpha = 0.30f),
                            radius = dotRadius * 1.28f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = primaryDotColor,
                            radius = dotRadius,
                            center = Offset(cx, cy)
                        )

                        if (isCurrentlyPlaying || (isRoot && isScaleFret)) {
                            drawCircle(
                                color = Color.White,
                                radius = dotRadius,
                                center = Offset(cx, cy),
                                style = Stroke(width = (dotRadius * 0.18f).coerceAtLeast(1f))
                            )
                        }

                        val fingerNum = if (isChordFret) chordFingers?.getOrNull(strIdx) else null
                        val displayText = if (fingerNum != null && fingerNum > 0 && !showNoteNames) {
                            "$fingerNum"
                        } else {
                            NOTE_NAMES[fretSemi]
                        }

                        notePillTextPaint.color = if (isCurrentlyPlaying) android.graphics.Color.rgb(180, 40, 0) else android.graphics.Color.WHITE
                        notePillTextPaint.textSize = (dotRadius * (if (displayText.length > 1) 0.92f else 1.08f)).coerceIn(6.dp.toPx(), 12.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(displayText, cx, cy + notePillTextPaint.textSize * 0.36f, notePillTextPaint)
                    }
                }
            }

            // Bottom Fret Numbers
            fretNumPaint.textSize = (10.5f * s).coerceIn(7.dp.toPx(), 11.5f.dp.toPx())
            val fretNumY = (nutBottom.coerceAtLeast(bridgeBottom) + fretNumPaint.textSize + 2f * s).coerceAtMost(size.height - 3f)
            for (f in intArrayOf(3, 5, 7, 9, 12, 15, 17, 19, 21, 24)) {
                if (f in safeBaseFret..(safeBaseFret + safeNumFrets - 1)) {
                    val relF = f - safeBaseFret + 1
                    val x = getFretCenterX(relF)
                    drawContext.canvas.nativeCanvas.drawText("$f", x, fretNumY, fretNumPaint)
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
    val safeStart = min(startNote, endNote).coerceIn(0, 127)
    val safeEnd = kotlin.math.max(startNote, endNote).coerceIn(0, 127)

    val whiteMidiNotes = remember(safeStart, safeEnd) {
        (safeStart..safeEnd).filter { n -> (n % 12) !in listOf(1, 3, 6, 8, 10) }
    }

    val maxWhiteKeyWidthPx = with(density) { 42.dp.toPx() }

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

    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(6.dp))) {
        val availableWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val availableHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val calculatedKeyWidth = availableWidth / whiteMidiNotes.size.coerceAtLeast(1)
        val finalWhiteKeyWidthPx = if (fillWidth) {
            calculatedKeyWidth
        } else {
            calculatedKeyWidth.coerceAtMost(maxWhiteKeyWidthPx)
        }
        val totalKeyboardWidthPx = finalWhiteKeyWidthPx * whiteMidiNotes.size
        // Aspect-ratio guard: prevent vertical over-stretching if placed in a very tall container
        val maxReasonableKeyHeight = (finalWhiteKeyWidthPx * 6.2f).coerceAtLeast(48f)
        val renderedHeight = availableHeight.coerceAtMost(maxReasonableKeyHeight)
        val topPaddingOffset = (availableHeight - renderedHeight) * 0.5f
        val startPaddingOffset = if (fillWidth) 0f else (availableWidth - totalKeyboardWidthPx) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onKeyTapped != null || onKeyReleased != null) {
                        Modifier
                .pointerInput(safeStart, safeEnd, whiteMidiNotes, finalWhiteKeyWidthPx, startPaddingOffset, renderedHeight, topPaddingOffset) {
                    val activePointers = mutableMapOf<Long, Int>()
                    fun resolveMidiAt(offset: Offset): Int? {
                        val adjustedX = offset.x - startPaddingOffset
                        val adjustedY = (offset.y - topPaddingOffset).coerceIn(0f, renderedHeight)
                        if (adjustedX < 0f || adjustedX > totalKeyboardWidthPx) return null

                        val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f
                        val blackKeyHeightPx = renderedHeight * 0.62f
                        if (adjustedY < blackKeyHeightPx) {
                            for (i in whiteMidiNotes.indices) {
                                val midiNote = whiteMidiNotes[i]
                                val semitone = (midiNote % 12 + 12) % 12
                                if (semitone in listOf(0, 2, 5, 7, 9)) {
                                    val blackMidi = midiNote + 1
                                    if (blackMidi <= safeEnd) {
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
                    } else {
                        Modifier
                    }
                )
        ) {
            val totalHeight = renderedHeight
            val blackKeyWidthPx = finalWhiteKeyWidthPx * 0.58f
            val blackKeyHeightPx = totalHeight * 0.62f
            val cornerRadiusPx = (finalWhiteKeyWidthPx * 0.14f).coerceIn(2f, 5.dp.toPx())
            val keyCorner = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            val borderStroke = Stroke(width = 1.dp.toPx())
            val frontFaceH = totalHeight * 0.11f
            val topSurfaceH = totalHeight - frontFaceH

            octaveTextPaint.textSize = (finalWhiteKeyWidthPx * 0.34f).coerceIn(6.dp.toPx(), 9.5f.dp.toPx())

            translate(left = startPaddingOffset, top = topPaddingOffset) {
                // 1. White Keys (3D Ivory Gradient, Depressed Key Shadow, & Neon Key-Strike Tint)
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    val customColor = customKeyColors[midiNote]
                    val belongsToRh = when {
                        customColor != null && kotlin.math.abs(customColor.red - rightHandColor.red) < 0.15f && kotlin.math.abs(customColor.blue - rightHandColor.blue) < 0.15f -> true
                        customColor != null && kotlin.math.abs(customColor.red - leftHandColor.red) < 0.15f && kotlin.math.abs(customColor.blue - leftHandColor.blue) < 0.15f -> false
                        midiNote < 48 -> false
                        midiNote > 72 -> true
                        else -> midiNote >= 60
                    }
                    val isHandHidden = if (belongsToRh) isRightHandHidden else isLeftHandHidden
                    val isCurrentlyPlaying = !isHandHidden && activeCoordinate != null &&
                            (midiNote == activeCoordinate || (activeCoordinate < 12 && (midiNote % 12) == activeCoordinate))
                    val isExplicitActive = !isHandHidden && (
                            activeMidiNotes[midiNote] == true ||
                                    (activeCoordinate != null && activeCoordinate < 12 && (midiNote % 12) == activeCoordinate) ||
                                    fingering?.notes?.contains(midiNote) == true ||
                                    isCurrentlyPlaying
                            )

                    val x = index * finalWhiteKeyWidthPx
                    val keyW = (finalWhiteKeyWidthPx - 1.4f).coerceAtLeast(1f)
                    val handColor = if (belongsToRh) rightHandColor else leftHandColor
                    val activeKeyColor = customColor ?: handColor

                    val pressSinkY = if (isCurrentlyPlaying || isExplicitActive) (totalHeight * 0.018f).coerceAtMost(3f) else 0f

                    if (isCurrentlyPlaying || isExplicitActive) {
                        // Illuminated 3D Active White Key
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    activeKeyColor.copy(alpha = 0.88f),
                                    activeKeyColor
                                ),
                                startY = 0f,
                                endY = topSurfaceH
                            ),
                            topLeft = Offset(x + 0.7f, pressSinkY),
                            size = Size(keyW, topSurfaceH),
                            cornerRadius = keyCorner
                        )
                        drawRoundRect(
                            color = activeKeyColor.copy(alpha = 0.72f),
                            topLeft = Offset(x + 0.7f, topSurfaceH - 1f),
                            size = Size(keyW, frontFaceH + 1f),
                            cornerRadius = keyCorner
                        )
                    } else {
                        // Natural 3D Concert Ivory White Key with subtle top-to-bottom shading
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFECEAE2), Color(0xFFFBF9F3), Color(0xFFF4F0E6)),
                                startY = 0f,
                                endY = topSurfaceH
                            ),
                            topLeft = Offset(x + 0.7f, 0f),
                            size = Size(keyW, topSurfaceH),
                            cornerRadius = keyCorner
                        )
                        drawRoundRect(
                            color = Color(0xFFD2C8B8),
                            topLeft = Offset(x + 0.7f, topSurfaceH - 1f),
                            size = Size(keyW, frontFaceH + 1f),
                            cornerRadius = keyCorner
                        )
                    }

                    // Crisp key separator border
                    drawRoundRect(
                        color = if (isExplicitActive) Color.White.copy(alpha = 0.8f) else Color(0xFF1A1D24).copy(alpha = 0.55f),
                        topLeft = Offset(x + 0.7f, pressSinkY),
                        size = Size(keyW, totalHeight - pressSinkY),
                        cornerRadius = keyCorner,
                        style = borderStroke
                    )

                    val centerX = x + finalWhiteKeyWidthPx * 0.5f

                    if (midiNote % 12 == 0 && finalWhiteKeyWidthPx >= 10f && !isExplicitActive) {
                        val octaveNumber = (midiNote / 12) - 1
                        drawContext.canvas.nativeCanvas.drawText(
                            "C$octaveNumber",
                            centerX,
                            totalHeight - (totalHeight * 0.045f).coerceAtLeast(4f),
                            octaveTextPaint
                        )
                    }

                    if (isExplicitActive || showNoteNames) {
                        val fingerIndex = fingering?.notes?.indexOf(midiNote) ?: -1
                        val fingerNum = if (fingerIndex != -1) fingering?.fingers?.getOrNull(fingerIndex) else null
                        val pillW = (finalWhiteKeyWidthPx * 0.78f).coerceIn(8f, 22.dp.toPx())
                        val pillH = pillW * 0.88f
                        val badgeCenterY = (totalHeight - frontFaceH - pillH * 0.65f - 3f).coerceAtLeast(blackKeyHeightPx + pillH * 0.6f + 2f)

                        drawRoundRect(
                            color = when {
                                isCurrentlyPlaying -> Color(0xFF111318).copy(alpha = 0.92f)
                                isExplicitActive -> Color(0xFF161922).copy(alpha = 0.88f)
                                else -> Color(0xFF1E222B).copy(alpha = 0.78f)
                            },
                            topLeft = Offset(centerX - pillW * 0.5f, badgeCenterY - pillH * 0.5f),
                            size = Size(pillW, pillH),
                            cornerRadius = CornerRadius(pillH * 0.35f, pillH * 0.35f)
                        )

                        val displayNum = if (isExplicitActive && fingerNum != null && fingerNum > 0) {
                            fingerNum.toString()
                        } else {
                            NOTE_NAMES[(midiNote % 12 + 12) % 12]
                        }
                        keyBadgeTextPaint.color = when {
                            isCurrentlyPlaying -> android.graphics.Color.rgb(255, 235, 59)
                            isExplicitActive -> android.graphics.Color.WHITE
                            else -> android.graphics.Color.rgb(230, 235, 245)
                        }
                        keyBadgeTextPaint.textSize = (pillH * 0.58f).coerceIn(6.dp.toPx(), 11.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            displayNum,
                            centerX,
                            badgeCenterY + keyBadgeTextPaint.textSize * 0.36f,
                            keyBadgeTextPaint
                        )
                    }
                }

                // 2. Black Keys (3D Ebony Bevel + Specular Top Cap & Illuminated Strike State)
                whiteMidiNotes.forEachIndexed { index, midiNote ->
                    val semitone = (midiNote % 12 + 12) % 12
                    if (semitone in listOf(0, 2, 5, 7, 9)) {
                        val blackMidiNote = midiNote + 1
                        if (blackMidiNote <= safeEnd) {
                            val customBlackColor = customKeyColors[blackMidiNote]
                            val belongsToRh = when {
                                customBlackColor != null && abs(customBlackColor.red - rightHandColor.red) < 0.15f && abs(customBlackColor.blue - rightHandColor.blue) < 0.15f -> true
                                customBlackColor != null && abs(customBlackColor.red - leftHandColor.red) < 0.15f && abs(customBlackColor.blue - leftHandColor.blue) < 0.15f -> false
                                blackMidiNote < 48 -> false
                                blackMidiNote > 72 -> true
                                else -> blackMidiNote >= 60
                            }
                            val isHandHidden = if (belongsToRh) isRightHandHidden else isLeftHandHidden
                            val isCurrentlyPlaying = !isHandHidden && activeCoordinate != null &&
                                    (blackMidiNote == activeCoordinate || (activeCoordinate < 12 && (blackMidiNote % 12) == activeCoordinate))
                            val isExplicitActive = !isHandHidden && (
                                    activeMidiNotes[blackMidiNote] == true ||
                                            (activeCoordinate != null && activeCoordinate < 12 && (blackMidiNote % 12) == activeCoordinate) ||
                                            fingering?.notes?.contains(blackMidiNote) == true ||
                                            isCurrentlyPlaying
                                    )

                            val blackX = (index * finalWhiteKeyWidthPx) + (finalWhiteKeyWidthPx - blackKeyWidthPx * 0.5f)
                            val blackHandColor = if (belongsToRh) rightHandColor else leftHandColor
                            val activeBlackColor = customBlackColor ?: blackHandColor

                            // Drop shadow on white keys
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.52f),
                                topLeft = Offset(blackX + 1.5f, 2f),
                                size = Size(blackKeyWidthPx, blackKeyHeightPx + 2.5f),
                                cornerRadius = keyCorner
                            )

                            // Outer Ebony or Active Neon Body
                            if (isCurrentlyPlaying || isExplicitActive) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.9f),
                                            activeBlackColor,
                                            activeBlackColor.copy(alpha = 0.78f)
                                        ),
                                        startY = 0f,
                                        endY = blackKeyHeightPx
                                    ),
                                    topLeft = Offset(blackX, 0f),
                                    size = Size(blackKeyWidthPx, blackKeyHeightPx),
                                    cornerRadius = keyCorner
                                )
                            } else {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF141519), Color(0xFF22242B), Color(0xFF0E0F12)),
                                        startY = 0f,
                                        endY = blackKeyHeightPx
                                    ),
                                    topLeft = Offset(blackX, 0f),
                                    size = Size(blackKeyWidthPx, blackKeyHeightPx),
                                    cornerRadius = keyCorner
                                )
                                // 3D Ebony inner top cap bevel
                                val inset = (blackKeyWidthPx * 0.12f).coerceIn(1.2f, 3f)
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF323640), Color(0xFF1B1D23)),
                                        startY = 0f,
                                        endY = blackKeyHeightPx * 0.86f
                                    ),
                                    topLeft = Offset(blackX + inset, 1f),
                                    size = Size((blackKeyWidthPx - inset * 2f).coerceAtLeast(1f), blackKeyHeightPx * 0.86f),
                                    cornerRadius = CornerRadius((cornerRadiusPx - 1f).coerceAtLeast(1f))
                                )
                            }

                            val centerX = blackX + blackKeyWidthPx * 0.5f
                            if (isExplicitActive || showNoteNames) {
                                val fingerIndex = fingering?.notes?.indexOf(blackMidiNote) ?: -1
                                val fingerNum = if (fingerIndex != -1) fingering?.fingers?.getOrNull(fingerIndex) else null
                                val pillW = (blackKeyWidthPx * 0.84f).coerceIn(7f, 18.dp.toPx())
                                val pillH = pillW * 0.90f
                                val badgeCenterY = (blackKeyHeightPx - pillH * 0.65f - 4f).coerceAtLeast(pillH * 0.6f + 2f)

                                drawRoundRect(
                                    color = Color(0xFF101217).copy(alpha = 0.90f),
                                    topLeft = Offset(centerX - pillW * 0.5f, badgeCenterY - pillH * 0.5f),
                                    size = Size(pillW, pillH),
                                    cornerRadius = CornerRadius(pillH * 0.35f, pillH * 0.35f)
                                )

                                val displayNum = if (isExplicitActive && fingerNum != null && fingerNum > 0) {
                                    fingerNum.toString()
                                } else {
                                    NOTE_NAMES[(blackMidiNote % 12 + 12) % 12]
                                }
                                keyBadgeTextPaint.color = if (isCurrentlyPlaying) {
                                    android.graphics.Color.rgb(255, 235, 59)
                                } else {
                                    android.graphics.Color.WHITE
                                }
                                keyBadgeTextPaint.textSize = (pillH * 0.54f).coerceIn(5.2f.dp.toPx(), 9.5f.dp.toPx())
                                drawContext.canvas.nativeCanvas.drawText(
                                    displayNum,
                                    centerX,
                                    badgeCenterY + keyBadgeTextPaint.textSize * 0.36f,
                                    keyBadgeTextPaint
                                )
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
    notes: List<Int> = emptyList(),
    accentColor: Color,
    label: String = "",
    soundStyle: InstrumentSoundStyle = InstrumentSoundStyle.SEQUENTIAL,
    arpDelayMs: Long = 85,
    isPlaying: Boolean? = null,
    onPlay: ((Int) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onSoundStyleToggle: (() -> Unit)? = null, // Added parameter here
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var internalPlayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    val activePlaying = isPlaying ?: (internalPlayJob?.isActive == true)

    DisposableEffect(Unit) {
        onDispose { internalPlayJob?.cancel() }
    }

    val scale by animateFloatAsState(
        targetValue = if (activePlaying) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "play_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (activePlaying) 0.65f else 1f,
        animationSpec = tween(150),
        label = "play_alpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = {
                if (onClick != null) {
                    onClick()
                } else if (internalPlayJob?.isActive == true) {
                    internalPlayJob?.cancel()
                    internalPlayJob = null
                } else if (notes.isNotEmpty() && onPlay != null) {
                    val snapshotNotes = notes.toList()
                    internalPlayJob = coroutineScope.launch {
                        try {
                            when (soundStyle) {
                                InstrumentSoundStyle.SEQUENTIAL -> {
                                    snapshotNotes.forEach { note ->
                                        onPlay(note)
                                        delay(arpDelayMs)
                                    }
                                }
                                InstrumentSoundStyle.ALL_AT_ONCE -> {
                                    snapshotNotes.forEach { note -> onPlay(note) }
                                }
                                InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> {
                                    val roundTripNotes = if (snapshotNotes.size > 2) {
                                        snapshotNotes + snapshotNotes.subList(0, snapshotNotes.size - 1).reversed()
                                    } else if (snapshotNotes.size == 2) {
                                        listOf(snapshotNotes[0], snapshotNotes[1], snapshotNotes[0])
                                    } else {
                                        snapshotNotes
                                    }
                                    roundTripNotes.forEach { note ->
                                        onPlay(note)
                                        delay(arpDelayMs)
                                    }
                                }
                            }
                            delay(350)
                        } finally {
                            internalPlayJob = null
                        }
                    }
                }
            },
            shape = CircleShape,
            color = if (activePlaying) accentColor else accentColor.copy(alpha = 0.18f),
            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
            modifier = Modifier.size(36.dp).scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (activePlaying) Icons.Default.VolumeUp else Icons.Default.PlayCircle,
                    contentDescription = soundStyle.iconDesc,
                    tint = if (activePlaying) Color.White else accentColor,
                    modifier = Modifier.size(20.dp).graphicsLayer(alpha = alpha)
                )
            }
        }

        // Optional sound style toggle button next to the play button
        if (onSoundStyleToggle != null) {
            Surface(
                onClick = onSoundStyleToggle,
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle Sound Style",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (label.isNotBlank()) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(alpha = 0.85f))
        }
    }
}
@Composable
fun pEQInstrumentDropdown(
    selectedInstrument: ChordInstrumentType,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onInstrumentChange: (ChordInstrumentType) -> Unit,
    instrumentVolumes: MutableMap<String, Float>? = null,
    onInstrumentVolumeChange: ((ChordInstrumentType, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(selectedInstrument.label, fontSize = 16.sp)
                    Text(
                        text = selectedInstrument.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Select Instrument",
                    tint = accentColor
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            ChordInstrumentType.entries.forEach { inst ->
                val isSelected = inst == selectedInstrument
                val curVol = instrumentVolumes?.get(inst.title) ?: 1.0f
                DropdownMenuItem(
                    leadingIcon = {
                        Text(inst.label, fontSize = 16.sp)
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inst.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(curVol * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor.copy(alpha = 0.85f)
                                )
                            }
                            if (instrumentVolumes != null) {
                                Slider(
                                    value = curVol,
                                    onValueChange = { newV ->
                                        instrumentVolumes[inst.title] = newV
                                        onInstrumentVolumeChange?.invoke(inst, newV)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth().height(22.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor
                                    )
                                )
                            }
                        }
                    },
                    onClick = {
                        onInstrumentChange(inst)
                        expanded = false
                    }
                )
            }
        }
    }
}
// ============================================================================
// MAJESTIC CONCERT-GRADE HARMONIUM, VIOLIN, FLUTE, TABLA & SITAR VISUALIZERS
// ============================================================================

@Composable
fun UnifiedHarmoniumVisualizer(
    activeMidiNotes: Map<Int, Boolean> = emptyMap(),
    accentColor: Color = Color(0xFFFFB300),
    activeMidiNote: Int? = null,
    rootNote: Int = 0,
    showNoteNames: Boolean = true,
    startNote: Int = 48, // C3 default
    endNote: Int = 84,   // C6 default (3 octaves)
    leftHandColor: Color = Color(0xFFFF8F00),
    rightHandColor: Color = Color(0xFFFFD54F),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    customKeyColors: Map<Int, Color> = emptyMap(),
    onKeyTapped: ((midiNote: Int) -> Unit)? = null,
    onKeyReleased: ((midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val safeStart = min(startNote, endNote).coerceIn(21, 108)
    val safeEnd = kotlin.math.max(startNote, endNote).coerceIn(21, 108)
    val normalizedRoot = remember(rootNote) { (rootNote % 12 + 12) % 12 }

    val whiteMidiNotes = remember(safeStart, safeEnd) {
        (safeStart..safeEnd).filter { n -> (n % 12) !in listOf(1, 3, 6, 8, 10) }
    }

    val isAnyNotePlaying = remember(activeMidiNotes, activeMidiNote) {
        activeMidiNote != null || activeMidiNotes.values.any { it }
    }
    val bellowsExpansion by animateFloatAsState(
        targetValue = if (isAnyNotePlaying) 1f else 0.15f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "harmonium_bellows"
    )

    val labelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val sargamPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }
    val stopTagPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 255, 224, 130)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    val cabinetBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF522812), Color(0xFF33180A), Color(0xFF1D0D05))
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(cabinetBrush, RoundedCornerShape(10.dp))
            .border(1.5.dp, Color(0xFFB58500), RoundedCornerShape(10.dp))
    ) {
        val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val vp = remember(canvasW, canvasH, whiteMidiNotes.size) {
            computeAspectSafeViewport(
                canvasWidth = canvasW,
                canvasHeight = canvasH,
                minAspect = 1.8f,
                maxAspect = 11.5f,
                referenceHeight = 160f,
                horizontalPaddingFrac = 0.010f,
                verticalPaddingFrac = 0.020f
            )
        }

        val framePad = (5f * vp.scale).coerceIn(3f, 10f)
        val innerLeft = vp.left + framePad
        val innerTop = vp.top + framePad
        val innerW = (vp.width - framePad * 2f).coerceAtLeast(1f)
        val innerH = (vp.height - framePad * 2f).coerceAtLeast(1f)

        val showHeaderPanel = innerH >= 54f
        val headerH = if (showHeaderPanel) (innerH * 0.26f).coerceIn(20f, 46f) else 0f
        val velvetStripH = (3.8f * vp.scale).coerceIn(2f, 6f)
        val keybedTop = innerTop + headerH + velvetStripH
        val keybedH = (innerH - headerH - velvetStripH).coerceAtLeast(24f)
        val whiteKeyW = innerW / whiteMidiNotes.size.coerceAtLeast(1)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onKeyTapped != null || onKeyReleased != null) {
                        Modifier.pointerInput(safeStart, safeEnd, whiteMidiNotes, innerLeft, keybedTop, innerW, keybedH, whiteKeyW) {
                            val activePointers = mutableMapOf<Long, Int>()

                            fun resolveMidiAt(offset: Offset): Int? {
                                val relX = offset.x - innerLeft
                                val relY = offset.y - keybedTop
                                if (relX < 0f || relX > innerW || relY < -8f || relY > keybedH + 8f) return null

                                val clampedY = relY.coerceIn(0f, keybedH)
                                val blackKeyW = whiteKeyW * 0.58f
                                val blackKeyH = keybedH * 0.60f

                                if (clampedY < blackKeyH) {
                                    for (i in whiteMidiNotes.indices) {
                                        val midiNote = whiteMidiNotes[i]
                                        val semitone = (midiNote % 12 + 12) % 12
                                        if (semitone in listOf(0, 2, 5, 7, 9)) {
                                            val blackMidi = midiNote + 1
                                            if (blackMidi <= safeEnd) {
                                                val blackX = i * whiteKeyW + whiteKeyW - (blackKeyW / 2f)
                                                if (relX >= blackX && relX <= blackX + blackKeyW) {
                                                    return blackMidi
                                                }
                                            }
                                        }
                                    }
                                }
                                val whiteIdx = (relX / whiteKeyW).toInt()
                                return if (whiteIdx in whiteMidiNotes.indices) whiteMidiNotes[whiteIdx] else null
                            }

                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        val id = change.id.value
                                        if (change.pressed) {
                                            val midi = resolveMidiAt(change.position)
                                            val prev = activePointers[id]
                                            if (prev == null && midi != null) {
                                                activePointers[id] = midi
                                                onKeyTapped?.invoke(midi)
                                            } else if (prev != null && midi != null && prev != midi) {
                                                onKeyReleased?.invoke(prev)
                                                activePointers[id] = midi
                                                onKeyTapped?.invoke(midi)
                                            }
                                        } else {
                                            activePointers.remove(id)?.let { released ->
                                                onKeyReleased?.invoke(released)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            drawRoundRect(
                color = Color(0xFF281309),
                topLeft = Offset(vp.left, vp.top),
                size = Size(vp.width, vp.height),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFFD4AF37).copy(alpha = 0.72f),
                topLeft = Offset(vp.left, vp.top),
                size = Size(vp.width, vp.height),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 1.4.dp.toPx())
            )

            // 1. TOP BELLOWS, JALI SOUND GRILLE & LABELED BRASS STOPS PANEL
            if (showHeaderPanel) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1B0C05), Color(0xFF3E1D0D), Color(0xFF281207), Color(0xFF3E1D0D), Color(0xFF1B0C05)),
                        startX = innerLeft,
                        endX = innerLeft + innerW
                    ),
                    topLeft = Offset(innerLeft, innerTop),
                    size = Size(innerW, headerH),
                    cornerRadius = CornerRadius(5.dp.toPx())
                )

                val pleatCount = 6
                val pleatAreaH = headerH * (0.38f + 0.14f * bellowsExpansion)
                for (p in 1..pleatCount) {
                    val py = innerTop + (pleatAreaH / (pleatCount + 1)) * p
                    drawLine(
                        color = if (p % 2 == 0) Color(0xFF8D5524).copy(alpha = 0.6f) else Color(0xFF120703).copy(alpha = 0.75f),
                        start = Offset(innerLeft + 8f, py),
                        end = Offset(innerLeft + innerW - 8f, py),
                        strokeWidth = 1.1.dp.toPx()
                    )
                }

                val railY = innerTop + headerH * 0.62f
                drawLine(
                    color = Color(0xFFD4AF37).copy(alpha = 0.75f),
                    start = Offset(innerLeft + innerW * 0.06f, railY),
                    end = Offset(innerLeft + innerW * 0.94f, railY),
                    strokeWidth = (2.4f * vp.scale).coerceIn(1.2f, 4f)
                )

                val stopLabels = arrayOf("BASS", "DRONE", "MALE", "TREM", "FEMALE", "SWAR", "KHARIJ", "COUPLER", "OCTAVE")
                val numStops = stopLabels.size
                val stopRadius = (headerH * 0.21f).coerceIn(3.5f, 8.dp.toPx())
                val stopSpacing = (innerW * 0.80f) / (numStops - 1).coerceAtLeast(1)
                val stopStartX = innerLeft + innerW * 0.10f
                stopTagPaint.textSize = (headerH * 0.18f).coerceIn(5.dp.toPx(), 7.5.dp.toPx())

                for (sIdx in 0 until numStops) {
                    val sx = stopStartX + sIdx * stopSpacing
                    val isLargeStop = sIdx % 2 == 0
                    val r = if (isLargeStop) stopRadius else stopRadius * 0.78f
                    val pulledOffset = if (isAnyNotePlaying && isLargeStop) -1.8f * vp.scale else 0f

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.55f),
                        radius = r + 1.2f,
                        center = Offset(sx, railY + pulledOffset + 1.5f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFF795548)),
                            center = Offset(sx - r * 0.25f, railY + pulledOffset - r * 0.25f),
                            radius = r * 1.3f
                        ),
                        radius = r,
                        center = Offset(sx, railY + pulledOffset)
                    )
                    if (isLargeStop) {
                        drawCircle(
                            color = Color(0xFFFFF8E1),
                            radius = r * 0.36f,
                            center = Offset(sx, railY + pulledOffset)
                        )
                    }
                    if (innerW >= 360f && isLargeStop) {
                        drawContext.canvas.nativeCanvas.drawText(
                            stopLabels[sIdx],
                            sx,
                            innerTop + headerH - 2f,
                            stopTagPaint
                        )
                    }
                }
            }

            // 2. RED VELVET FELT STRIP ABOVE REED KEYS
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF5D0E11), Color(0xFFA31621), Color(0xFF5D0E11)),
                    startX = innerLeft,
                    endX = innerLeft + innerW
                ),
                topLeft = Offset(innerLeft, innerTop + headerH),
                size = Size(innerW, velvetStripH)
            )

            // 3. HARMONIUM WHITE REED KEYS (Respects customKeyColors for accurate LH/RH & Track coloring!)
            val cornerRad = (whiteKeyW * 0.14f).coerceIn(2f, 5.dp.toPx())
            val keyCorner = CornerRadius(cornerRad, cornerRad)
            val frontFaceH = keybedH * 0.12f
            val topSurfaceH = keybedH - frontFaceH
            val blackKeyW = whiteKeyW * 0.58f
            val blackKeyH = keybedH * 0.60f

            whiteMidiNotes.forEachIndexed { index, midiNote ->
                val customCol = customKeyColors[midiNote]
                val belongsToRh = when (customCol) {
                    rightHandColor -> true
                    leftHandColor -> false
                    else -> midiNote >= 60
                }
                val isHandHidden = if (belongsToRh) isRightHandHidden else isLeftHandHidden
                val isCurrentlyPlaying = !isHandHidden && activeMidiNote != null &&
                        (midiNote == activeMidiNote || (activeMidiNote < 12 && (midiNote % 12) == activeMidiNote))
                val isExplicitActive = !isHandHidden && (
                        activeMidiNotes[midiNote] == true || isCurrentlyPlaying
                        )
                val semitone = (midiNote % 12 + 12) % 12
                val isRoot = semitone == normalizedRoot

                val x = innerLeft + index * whiteKeyW
                val activeFill = customCol ?: (if (belongsToRh) rightHandColor else leftHandColor)

                val topKeyColor = when {
                    isCurrentlyPlaying -> Color(0xFFFFEB3B)
                    isExplicitActive -> activeFill
                    else -> Color(0xFFFFF8E7)
                }
                val frontLipColor = when {
                    isCurrentlyPlaying -> Color(0xFFFBC02D)
                    isExplicitActive -> activeFill.copy(alpha = 0.82f)
                    else -> Color(0xFFDCC7AA)
                }

                val pressOffsetY = if (isCurrentlyPlaying || isExplicitActive) (2.0f * vp.scale).coerceAtMost(frontFaceH * 0.5f) else 0f

                drawRoundRect(
                    color = topKeyColor,
                    topLeft = Offset(x + 0.8f, keybedTop + pressOffsetY),
                    size = Size((whiteKeyW - 1.6f).coerceAtLeast(1f), topSurfaceH),
                    cornerRadius = keyCorner
                )
                drawRoundRect(
                    color = frontLipColor,
                    topLeft = Offset(x + 0.8f, keybedTop + topSurfaceH - 1f),
                    size = Size((whiteKeyW - 1.6f).coerceAtLeast(1f), frontFaceH + 1f),
                    cornerRadius = keyCorner
                )
                drawRoundRect(
                    color = Color(0xFF2B1508).copy(alpha = 0.55f),
                    topLeft = Offset(x + 0.8f, keybedTop),
                    size = Size((whiteKeyW - 1.6f).coerceAtLeast(1f), keybedH),
                    cornerRadius = keyCorner,
                    style = Stroke(width = 1.dp.toPx())
                )

                val centerX = x + whiteKeyW * 0.5f

                if (isExplicitActive || showNoteNames || isRoot) {
                    val maxRad = (whiteKeyW * 0.42f).coerceIn(4f, 12.dp.toPx())
                    val badgeRadius = if (isCurrentlyPlaying) maxRad else if (isExplicitActive) maxRad * 0.92f else maxRad * 0.78f
                    val badgeY = (keybedTop + topSurfaceH - badgeRadius - 4f).coerceAtLeast(keybedTop + blackKeyH + badgeRadius + 2f)

                    if (isExplicitActive || isRoot) {
                        drawCircle(
                            color = when {
                                isCurrentlyPlaying -> Color(0xFFD84315)
                                isExplicitActive -> Color(0xFF1F140E)
                                else -> Color(0xFFB71C1C).copy(alpha = 0.22f)
                            },
                            radius = badgeRadius,
                            center = Offset(centerX, badgeY)
                        )
                    }

                    val relSemi = ((semitone - normalizedRoot) % 12 + 12) % 12
                    val sargamLabel = SARGAM_NAMES[relSemi]
                    val westernLabel = NOTE_NAMES[semitone]

                    labelPaint.color = when {
                        isCurrentlyPlaying || isExplicitActive -> android.graphics.Color.WHITE
                        isRoot -> android.graphics.Color.rgb(183, 28, 28)
                        else -> android.graphics.Color.rgb(93, 64, 55)
                    }
                    labelPaint.textSize = (badgeRadius * 0.98f).coerceIn(5.5f.dp.toPx(), 10.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(
                        if (isExplicitActive) sargamLabel else westernLabel,
                        centerX,
                        badgeY + labelPaint.textSize * 0.35f,
                        labelPaint
                    )

                    if (whiteKeyW >= 14f && frontFaceH >= 8f) {
                        sargamPaint.color = android.graphics.Color.argb(190, 80, 50, 30)
                        sargamPaint.textSize = (whiteKeyW * 0.30f).coerceIn(5.dp.toPx(), 8.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            if (isExplicitActive) westernLabel else sargamLabel,
                            centerX,
                            keybedTop + keybedH - (frontFaceH * 0.22f),
                            sargamPaint
                        )
                    }
                }
            }

            // 4. HARMONIUM BLACK REED KEYS
            whiteMidiNotes.forEachIndexed { index, midiNote ->
                val semitone = (midiNote % 12 + 12) % 12
                if (semitone in listOf(0, 2, 5, 7, 9)) {
                    val blackMidiNote = midiNote + 1
                    if (blackMidiNote <= safeEnd) {
                        val customCol = customKeyColors[blackMidiNote]
                        val belongsToRh = when (customCol) {
                            rightHandColor -> true
                            leftHandColor -> false
                            else -> blackMidiNote >= 60
                        }
                        val isHandHidden = if (belongsToRh) isRightHandHidden else isLeftHandHidden
                        val isCurrentlyPlaying = !isHandHidden && activeMidiNote != null &&
                                (blackMidiNote == activeMidiNote || (activeMidiNote < 12 && (blackMidiNote % 12) == activeMidiNote))
                        val isExplicitActive = !isHandHidden && (
                                activeMidiNotes[blackMidiNote] == true || isCurrentlyPlaying
                                )

                        val blackX = innerLeft + (index * whiteKeyW) + (whiteKeyW - blackKeyW * 0.5f)
                        val activeFill = customCol ?: (if (belongsToRh) rightHandColor else leftHandColor)

                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            topLeft = Offset(blackX + 1.5f, keybedTop + 2f),
                            size = Size(blackKeyW, blackKeyH + 2f),
                            cornerRadius = keyCorner
                        )

                        val bodyColor = when {
                            isCurrentlyPlaying -> Color(0xFFFFEB3B)
                            isExplicitActive -> activeFill
                            else -> Color(0xFF221610)
                        }
                        drawRoundRect(
                            color = bodyColor,
                            topLeft = Offset(blackX, keybedTop),
                            size = Size(blackKeyW, blackKeyH),
                            cornerRadius = keyCorner
                        )
                        drawRoundRect(
                            color = if (isExplicitActive) Color.White else Color(0xFF8D6E63).copy(alpha = 0.45f),
                            topLeft = Offset(blackX, keybedTop),
                            size = Size(blackKeyW, blackKeyH),
                            cornerRadius = keyCorner,
                            style = Stroke(width = 0.8.dp.toPx())
                        )

                        val centerX = blackX + blackKeyW * 0.5f
                        if (isExplicitActive || showNoteNames) {
                            val maxRad = (blackKeyW * 0.44f).coerceIn(3.5f, 9.5f.dp.toPx())
                            val badgeRadius = if (isCurrentlyPlaying) maxRad else if (isExplicitActive) maxRad * 0.9f else maxRad * 0.78f
                            val badgeY = (keybedTop + blackKeyH - badgeRadius - 4f).coerceAtLeast(keybedTop + badgeRadius + 2f)

                            drawCircle(
                                color = when {
                                    isCurrentlyPlaying -> Color(0xFFD84315)
                                    isExplicitActive -> Color.White
                                    else -> Color(0xFF140B07).copy(alpha = 0.9f)
                                },
                                radius = badgeRadius,
                                center = Offset(centerX, badgeY)
                            )

                            val displayLabel = NOTE_NAMES[(blackMidiNote % 12 + 12) % 12]
                            labelPaint.color = when {
                                isCurrentlyPlaying -> android.graphics.Color.WHITE
                                isExplicitActive -> android.graphics.Color.rgb(180, 50, 0)
                                else -> android.graphics.Color.rgb(255, 224, 130)
                            }
                            labelPaint.textSize = (badgeRadius * 0.95f).coerceIn(5.dp.toPx(), 8.5f.dp.toPx())
                            drawContext.canvas.nativeCanvas.drawText(
                                displayLabel,
                                centerX,
                                badgeY + labelPaint.textSize * 0.35f,
                                labelPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedViolinFingerboard(
    activeNotes: List<Int> = emptyList(),
    activeMidiNote: Int? = null,
    rootNote: Int = 0,
    accentColor: Color = Color(0xFFE040FB),
    showNoteNames: Boolean = true,
    numPositions: Int = 14,
    stringLabels: List<String> = listOf("E", "A", "D", "G"),
    openStringsMidi: List<Int> = listOf(76, 69, 62, 55),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    onPositionTapped: ((stringIndex: Int, midiNote: Int) -> Unit)? = null,
    onPositionReleased: ((stringIndex: Int, midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalPositions = numPositions.coerceIn(7, 24)
    val numStrings = openStringsMidi.size.coerceAtLeast(1)
    val normalizedRoot = remember(rootNote) { (rootNote % 12 + 12) % 12 }

    val filteredActiveNotes = remember(activeNotes, isLeftHandHidden, isRightHandHidden) {
        activeNotes.filter { midi ->
            val isTreble = midi >= 65
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }
    }
    val effectiveActiveMidi = remember(activeMidiNote, isLeftHandHidden, isRightHandHidden) {
        if (activeMidiNote == null) null
        else {
            val isTreble = activeMidiNote >= 65
            if (isTreble && isRightHandHidden) null
            else if (!isTreble && isLeftHandHidden) null
            else activeMidiNote
        }
    }

    val allUniquePitches = remember(filteredActiveNotes, effectiveActiveMidi) {
        (filteredActiveNotes + listOfNotNull(effectiveActiveMidi)).distinct()
    }
    val distinctPitchClasses = remember(allUniquePitches) {
        allUniquePitches.map { (it % 12 + 12) % 12 }.toSet()
    }
    val isScaleMode = remember(distinctPitchClasses) {
        distinctPitchClasses.size >= 6
    }

    val resolvedPoints = remember(allUniquePitches, openStringsMidi, totalPositions, isScaleMode) {
        val points = mutableSetOf<Pair<Int, Int>>()
        if (isScaleMode) {
            for (sIdx in openStringsMidi.indices) {
                val openMidi = openStringsMidi[sIdx]
                for (pos in 0..min(totalPositions, 7)) {
                    val semi = ((openMidi + pos) % 12 + 12) % 12
                    if (distinctPitchClasses.contains(semi)) {
                        points.add(sIdx to pos)
                    }
                }
            }
        } else {
            val usedStrings = mutableSetOf<Int>()
            allUniquePitches.sorted().forEach { midi ->
                val candidates = mutableListOf<Pair<Int, Int>>()
                openStringsMidi.forEachIndexed { sIdx, openMidi ->
                    val pos = midi - openMidi
                    if (pos in 0..totalPositions) {
                        candidates.add(sIdx to pos)
                    }
                }
                val unused = candidates.filter { it.first !in usedStrings }
                val pool = if (unused.isNotEmpty()) unused else candidates
                val best = pool.minByOrNull { (sIdx, pos) ->
                    val posPenalty = when {
                        pos == 0 -> 1
                        pos in 1..5 -> pos * 2
                        pos in 6..9 -> 14 + (pos - 5) * 4
                        else -> 35 + pos * 7
                    }
                    val registerPenalty = if (sIdx >= 2 && pos > 6) 25 else 0
                    posPenalty + registerPenalty
                }
                if (best != null) {
                    points.add(best)
                    usedStrings.add(best.first)
                }
            }
        }
        points
    }

    val r = 0.056125687f
    val posScaleFactor = remember(totalPositions) {
        var sum = 0f
        for (p in 1..totalPositions) {
            sum += (1f - r).toDouble().pow(p.toDouble()).toFloat() * r
        }
        sum.coerceAtLeast(0.001f)
    }

    val stringLabelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 213, 79)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val noteTextPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val tapeLabelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(185, 230, 200, 160)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C1306), Color(0xFF1A0B03), Color(0xFF2C1306))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, Color(0xFF8D4E22), RoundedCornerShape(10.dp))
    ) {
        val totalWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val totalHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val vp = remember(totalWidth, totalHeight) {
            computeAspectSafeViewport(
                canvasWidth = totalWidth,
                canvasHeight = totalHeight,
                minAspect = 2.0f,
                maxAspect = 11.5f,
                referenceHeight = 140f,
                horizontalPaddingFrac = 0.010f,
                verticalPaddingFrac = 0.020f
            )
        }

        val s = vp.scale
        val pegBoxW = (48f * s).coerceIn(30f, vp.width * 0.16f)
        val rightBodyW = (54f * s).coerceIn(32f, vp.width * 0.18f)
        val nutX = vp.left + pegBoxW
        val bridgeX = vp.right - rightBodyW * 0.35f
        val fingerboardEndX = vp.right - rightBodyW
        val playableW = (fingerboardEndX - nutX).coerceAtLeast(1f)

        val topPad = vp.top + (10f * s).coerceAtLeast(4f)
        val bottomPad = (totalHeight - vp.bottom) + (18f * s).coerceAtLeast(10f)
        val playableH = (totalHeight - topPad - bottomPad).coerceAtLeast(1f)

        val nutH = playableH * 0.78f
        val bridgeH = playableH * 0.94f
        val midY = topPad + playableH * 0.5f
        val nutTop = midY - nutH * 0.5f
        val nutBottom = midY + nutH * 0.5f
        val bridgeTop = midY - bridgeH * 0.5f
        val bridgeBottom = midY + bridgeH * 0.5f

        val posXCenters = remember(nutX, playableW, totalPositions, posScaleFactor) {
            val centers = FloatArray(totalPositions + 1)
            centers[0] = nutX - (10f * s).coerceIn(6f, 14f)
            var prevEdge = nutX
            var acc = 0f
            for (p in 1..totalPositions) {
                val frac = (1f - r).toDouble().pow(p.toDouble()).toFloat() * r
                acc += (frac / posScaleFactor) * playableW
                val curEdge = nutX + acc
                centers[p] = (prevEdge + curEdge) * 0.5f
                prevEdge = curEdge
            }
            centers
        }

        fun getYForStringAtX(sIdx: Int, x: Float): Float {
            val t = ((x - nutX) / (bridgeX - nutX).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val curH = nutH + (bridgeH - nutH) * t
            val curTop = midY - curH * 0.5f
            val spacing = curH / (numStrings - 1).coerceAtLeast(1)
            return if (numStrings == 1) midY else curTop + sIdx * spacing
        }

        val fingerboardPath = remember(nutX, fingerboardEndX, nutTop, nutBottom, bridgeTop, bridgeBottom) {
            Path().apply {
                moveTo(nutX, nutTop)
                lineTo(fingerboardEndX, bridgeTop)
                lineTo(fingerboardEndX, bridgeBottom)
                lineTo(nutX, nutBottom)
                close()
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onPositionTapped != null || onPositionReleased != null) {
                        Modifier.pointerInput(totalPositions, numStrings, nutX, playableW, posXCenters) {
                            val activePointers = mutableMapOf<Long, Pair<Int, Int>>()

                            fun resolveAt(offset: Offset): Pair<Int, Int> {
                                var closestPos = 0
                                var minPosDist = Float.MAX_VALUE
                                for (p in 0..totalPositions) {
                                    val d = abs(offset.x - posXCenters[p])
                                    if (d < minPosDist) {
                                        minPosDist = d
                                        closestPos = p
                                    }
                                }

                                var closestString = 0
                                var minStrDist = Float.MAX_VALUE
                                for (sIdx in 0 until numStrings) {
                                    val sy = getYForStringAtX(sIdx, offset.x)
                                    val d = abs(offset.y - sy)
                                    if (d < minStrDist) {
                                        minStrDist = d
                                        closestString = sIdx
                                    }
                                }
                                val midi = (openStringsMidi.getOrElse(closestString) { 60 }) + closestPos
                                return closestString to midi
                            }

                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        val id = change.id.value
                                        if (change.pressed) {
                                            val resolved = resolveAt(change.position)
                                            val prev = activePointers[id]
                                            if (prev == null) {
                                                activePointers[id] = resolved
                                                onPositionTapped?.invoke(resolved.first, resolved.second)
                                            } else if (prev != resolved) {
                                                onPositionReleased?.invoke(prev.first, prev.second)
                                                activePointers[id] = resolved
                                                onPositionTapped?.invoke(resolved.first, resolved.second)
                                            }
                                        } else {
                                            activePointers.remove(id)?.let { released ->
                                                onPositionReleased?.invoke(released.first, released.second)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val stringSpacingAtNut = (nutH / (numStrings - 1).coerceAtLeast(1)).coerceAtLeast(8f)

            // 1. VIOLIN SPRUCE BODY SHOULDER, PURFLING RIM & CLASSIC F-HOLES ON RIGHT
            val bodyLeftX = fingerboardEndX - 18f * s
            val bodyPath = Path().apply {
                moveTo(bodyLeftX, topPad)
                cubicTo(
                    fingerboardEndX + 10f * s, topPad - 6f * s,
                    vp.right - 8f * s, topPad + playableH * 0.15f,
                    vp.right, midY - playableH * 0.46f
                )
                lineTo(vp.right, midY + playableH * 0.46f)
                cubicTo(
                    vp.right - 8f * s, topPad + playableH * 0.85f,
                    fingerboardEndX + 10f * s, topPad + playableH + 6f * s,
                    bodyLeftX, topPad + playableH
                )
                cubicTo(
                    bodyLeftX - 12f * s, midY + playableH * 0.25f,
                    bodyLeftX - 12f * s, midY - playableH * 0.25f,
                    bodyLeftX, topPad
                )
                close()
            }
            drawPath(
                path = bodyPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD87A2E), Color(0xFF9E4710), Color(0xFF4A1C05)),
                    center = Offset((fingerboardEndX + vp.right) * 0.5f, midY),
                    radius = (rightBodyW * 1.4f).coerceAtLeast(24f)
                )
            )
            drawPath(bodyPath, color = Color(0xFF220B02), style = Stroke(width = 2.dp.toPx()))

            // Upper & Lower f-Holes on the Violin Soundboard
            val fHoleX = (fingerboardEndX + bridgeX) * 0.5f
            for (dir in intArrayOf(-1, 1)) {
                val fy = midY + dir * (bridgeH * 0.36f)
                drawLine(
                    color = Color(0xFF120601),
                    start = Offset(fHoleX - 6f * s, fy - dir * 5f * s),
                    end = Offset(fHoleX + 6f * s, fy + dir * 5f * s),
                    strokeWidth = (2.4f * s).coerceAtLeast(1.5f),
                    cap = StrokeCap.Round
                )
                drawCircle(Color(0xFF120601), radius = (2.2f * s).coerceAtLeast(1.2f), center = Offset(fHoleX - 6f * s, fy - dir * 5f * s))
                drawCircle(Color(0xFF120601), radius = (2.2f * s).coerceAtLeast(1.2f), center = Offset(fHoleX + 6f * s, fy + dir * 5f * s))
            }

            // 2. CARVED SCROLL & PEGBOX ON LEFT + EBONY FINGERBOARD
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF3E1C0A), Color(0xFF6E3515), Color(0xFF281105)),
                    startX = vp.left,
                    endX = nutX
                ),
                topLeft = Offset(vp.left + 2f, nutTop - 2f),
                size = Size((nutX - vp.left - 2f).coerceAtLeast(8f), nutH + 4f),
                cornerRadius = CornerRadius(nutH * 0.35f, nutH * 0.35f)
            )

            drawPath(
                path = fingerboardPath,
                color = Color.Black.copy(alpha = 0.55f),
                style = Stroke(width = (5f * s).coerceIn(2f, 8f))
            )
            drawPath(
                path = fingerboardPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF242226), Color(0xFF121114), Color(0xFF1E1C20)),
                    startY = bridgeTop,
                    endY = bridgeBottom
                )
            )

            // Ebony Nut & Carved Maple Bridge
            drawLine(
                color = Color(0xFFD7CCC8),
                start = Offset(nutX, nutTop),
                end = Offset(nutX, nutBottom),
                strokeWidth = (3.5f * s).coerceIn(2f, 5.5f),
                cap = StrokeCap.Round
            )
            drawRoundRect(
                color = Color(0xFFE6B88A),
                topLeft = Offset(bridgeX - 2.dp.toPx(), bridgeTop - 2.dp.toPx()),
                size = Size(4.dp.toPx(), (bridgeBottom - bridgeTop) + 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // 3. ANIMATED HORSEHAIR BOW GLIDING ACROSS ACTIVE STRING
            if (resolvedPoints.isNotEmpty()) {
                val activeStrIdx = resolvedPoints.first().first
                val bowX = (fingerboardEndX + bridgeX) * 0.5f
                val activeStrY = getYForStringAtX(activeStrIdx, bowX)
                drawLine(
                    color = Color(0xFFFFF8E1).copy(alpha = 0.85f),
                    start = Offset(bowX - 3f * s, topPad - 4f),
                    end = Offset(bowX + 3f * s, topPad + playableH + 4f),
                    strokeWidth = (2.8f * s).coerceAtLeast(1.8f),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFFFFEB3B).copy(alpha = 0.6f),
                    radius = (6f * s).coerceAtLeast(4f),
                    center = Offset(bowX, activeStrY)
                )
            }

            // 4. INTONATION POSITION TAPES (1st, 2nd, 3rd, 4th Finger Tapes)
            val tapePositions = intArrayOf(2, 4, 5, 7, 9, 12)
            tapeLabelPaint.textSize = (9.5f * s).coerceIn(6.5f.dp.toPx(), 10.5f.dp.toPx())
            val tapeLabelY = (bridgeBottom + tapeLabelPaint.textSize + 2f * s).coerceAtMost(size.height - 2f)

            for (pos in tapePositions) {
                if (pos <= totalPositions) {
                    val tx = posXCenters[pos]
                    val t = ((tx - nutX) / playableW).coerceIn(0f, 1f)
                    val curTop = nutTop + (bridgeTop - nutTop) * t
                    val curBottom = nutBottom + (bridgeBottom - nutBottom) * t
                    val isOctave = pos == 12

                    drawLine(
                        color = if (isOctave) Color(0xFFFFD54F).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.22f),
                        start = Offset(tx, curTop + 2f),
                        end = Offset(tx, curBottom - 2f),
                        strokeWidth = if (isOctave) 2.dp.toPx() else 1.2.dp.toPx()
                    )

                    val posName = when (pos) {
                        2 -> "1st (I)"
                        4 -> "2nd"
                        5 -> "3rd (III)"
                        7 -> "4th"
                        9 -> "Pos V"
                        12 -> "Octave"
                        else -> "$pos"
                    }
                    drawContext.canvas.nativeCanvas.drawText(posName, tx, tapeLabelY, tapeLabelPaint)
                }
            }

            // 5. VIOLIN STRINGS & TUNING PEGS
            val pegCenterX = vp.left + pegBoxW * 0.44f
            val pegRad = (stringSpacingAtNut * 0.38f).coerceIn(6.dp.toPx(), 11.dp.toPx())
            stringLabelPaint.textSize = (pegRad * 1.05f).coerceIn(7.dp.toPx(), 11.dp.toPx())

            for (sIdx in 0 until numStrings) {
                val startY = getYForStringAtX(sIdx, nutX)
                val endY = getYForStringAtX(sIdx, bridgeX)
                val isStringActive = resolvedPoints.any { it.first == sIdx }
                val wireColor = when (sIdx % 4) {
                    0 -> Color(0xFFF5F5F5) // E5 Steel
                    1 -> Color(0xFFCFD8DC) // A4 Aluminum
                    2 -> Color(0xFFB0BEC5) // D4 Silver
                    else -> Color(0xFFFFD54F) // G3 Gold
                }
                val gauge = ((1.2f + sIdx * 0.52f) * s).coerceIn(1f, 4.2f)

                drawLine(
                    color = if (isStringActive) accentColor.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.5f),
                    start = Offset(nutX, startY + 1.2f),
                    end = Offset(bridgeX, endY + 1.2f),
                    strokeWidth = if (isStringActive) gauge * 2.1f else gauge
                )
                drawLine(
                    color = if (isStringActive) Color.White else wireColor,
                    start = Offset(nutX, startY),
                    end = Offset(bridgeX, endY),
                    strokeWidth = gauge
                )

                drawCircle(color = Color(0xFF1A0D06), radius = pegRad, center = Offset(pegCenterX, startY))
                drawCircle(
                    color = if (isStringActive) Color(0xFFFFEB3B) else Color(0xFFFFD54F).copy(alpha = 0.85f),
                    radius = pegRad,
                    center = Offset(pegCenterX, startY),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(
                    stringLabels.getOrElse(sIdx) { "S" },
                    pegCenterX,
                    startY + stringLabelPaint.textSize * 0.36f,
                    stringLabelPaint
                )
            }

            // 6. ACTIVE INTONATION DOTS WITH EDUCATIONAL VIOLIN FINGER NUMBERS (0=Open, 1=Index, 2=Mid, 3=Ring, 4=Pinky)
            val maxDotRad = (stringSpacingAtNut * 0.45f).coerceIn(5.5f.dp.toPx(), 13.dp.toPx())

            for (sIdx in 0 until numStrings) {
                val openMidi = openStringsMidi.getOrElse(sIdx) { 60 }
                for (pos in 0..totalPositions) {
                    val currentMidi = openMidi + pos
                    val cx = posXCenters[pos]
                    val cy = getYForStringAtX(sIdx, cx)
                    val isResolvedActive = resolvedPoints.contains(sIdx to pos)
                    val isExactPlaying = effectiveActiveMidi != null && currentMidi == effectiveActiveMidi && isResolvedActive
                    val isRoot = ((currentMidi % 12 + 12) % 12) == normalizedRoot
                    val isGuidePos = pos in tapePositions

                    if (isResolvedActive) {
                        val neighborDist = if (pos < totalPositions) abs(posXCenters[pos + 1] - cx) else abs(cx - posXCenters[(pos - 1).coerceAtLeast(0)])
                        val radius = min(maxDotRad, (neighborDist * 0.46f).coerceAtLeast(5.dp.toPx())) *
                                (if (isExactPlaying) 1.08f else if (isScaleMode) 0.86f else 0.96f)

                        val fill = when {
                            isExactPlaying -> Color(0xFFFFEB3B)
                            isRoot -> Color(0xFFFF5252)
                            else -> accentColor
                        }

                        drawCircle(color = fill.copy(alpha = 0.40f), radius = radius * 1.35f, center = Offset(cx, cy))
                        drawCircle(color = fill, radius = radius, center = Offset(cx, cy))
                        drawCircle(color = Color.White, radius = radius, center = Offset(cx, cy), style = Stroke(width = 1.4.dp.toPx()))

                        // Violin Finger Number (0 = Open, 1 = 1st finger, 2 = 2nd, 3 = 3rd, 4 = 4th)
                        val violinFinger = when (pos) {
                            0 -> "0"
                            1, 2 -> "1"
                            3, 4 -> "2"
                            5, 6 -> "3"
                            else -> "4"
                        }
                        val noteName = NOTE_NAMES[(currentMidi % 12 + 12) % 12]
                        val displayLabel = if (showNoteNames) noteName else "F$violinFinger"

                        noteTextPaint.color = if (isExactPlaying) android.graphics.Color.rgb(170, 35, 0) else android.graphics.Color.WHITE
                        noteTextPaint.textSize = (radius * (if (displayLabel.length > 1) 0.88f else 1.05f)).coerceIn(6.dp.toPx(), 11.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(displayLabel, cx, cy + noteTextPaint.textSize * 0.36f, noteTextPaint)
                    } else if (isGuidePos && pos > 0) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.32f),
                            radius = (stringSpacingAtNut * 0.14f).coerceIn(2f, 4.dp.toPx()),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedFluteVisualizer(
    activeNotes: List<Int> = emptyList(),
    activeMidiNote: Int? = null,
    rootNote: Int = 0,
    accentColor: Color = Color(0xFF00E5FF),
    showNoteNames: Boolean = true,
    holeMidiPitches: List<Int> = listOf(60, 62, 64, 65, 67, 69, 71, 72), // Sa, Re, Ga, Ma, Pa, Dha, Ni, Sa'
    holeNames: List<String> = listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni", "Ṡa"),
    onToneHoleTapped: ((holeIndex: Int, midiNote: Int) -> Unit)? = null,
    onToneHoleReleased: ((holeIndex: Int, midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val numHoles = holeMidiPitches.size.coerceAtLeast(1)
    val normalizedRoot = remember(rootNote) { (rootNote % 12 + 12) % 12 }
    val primaryPitch = activeMidiNote ?: activeNotes.lastOrNull()
    val primarySemitoneFromSa = primaryPitch?.let { ((it - (60 + normalizedRoot)) % 12 + 12) % 12 }
    val isAnyPlaying = activeNotes.isNotEmpty() || activeMidiNote != null

    // Authentic 6-Hole Bansuri Fingering Table (1.0f = Closed ●, 0.5f = Half-Hole ◐ for Komal/Tivra, 0.0f = Open ○)
    // Assumes all holes closed = Pa (or Ma), 3 holes closed = Sa:
    fun getHoleClosureState(holeIdx: Int, semitoneFromSa: Int?): Float {
        if (semitoneFromSa == null || holeIdx >= 6) return 0f
        return when (semitoneFromSa) {
            0 -> if (holeIdx < 3) 1f else 0f // Sa: Holes 1, 2, 3 Closed
            1 -> if (holeIdx < 2) 1f else if (holeIdx == 2) 0.5f else 0f // komal re: Hole 3 Half-open
            2 -> if (holeIdx < 2) 1f else 0f // Shuddha Re: Holes 1, 2 Closed
            3 -> if (holeIdx == 0) 1f else if (holeIdx == 1) 0.5f else 0f // komal ga: Hole 2 Half-open
            4 -> if (holeIdx == 0) 1f else 0f // Shuddha Ga: Hole 1 Closed
            5 -> 0f // Shuddha Ma: All holes Open (or Hole 1 half-closed)
            6 -> if (holeIdx in 1..5) 1f else 0.5f // Tivra Ma: Holes 2..6 Closed, Hole 1 Open
            7 -> 1f // Pa: All 6 Holes Closed
            8 -> if (holeIdx < 5) 1f else 0.5f // komal dha: Hole 6 Half-open
            9 -> if (holeIdx < 5) 1f else 0f // Shuddha Dha: Holes 1..5 Closed
            10 -> if (holeIdx < 4) 1f else if (holeIdx == 4) 0.5f else 0f // komal ni: Hole 5 Half-open
            11 -> if (holeIdx < 4) 1f else 0f // Shuddha Ni: Holes 1..4 Closed
            else -> 0f
        }
    }

    val holeLabelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val subLabelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF28180B), Color(0xFF170D06), Color(0xFF0F0803))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, Color(0xFF8D5B34), RoundedCornerShape(10.dp))
    ) {
        val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val vp = remember(canvasW, canvasH) {
            computeAspectSafeViewport(
                canvasWidth = canvasW,
                canvasHeight = canvasH,
                minAspect = 2.4f,
                maxAspect = 12.0f,
                referenceHeight = 110f,
                horizontalPaddingFrac = 0.02f,
                verticalPaddingFrac = 0.04f
            )
        }

        val tubeHeight = (vp.height * 0.54f).coerceIn(24f, vp.width * 0.22f)
        val tubeTop = vp.centerY - tubeHeight * 0.5f
        val tubeLeft = vp.left + vp.width * 0.02f
        val tubeWidth = vp.width * 0.96f

        val embouchureX = tubeLeft + tubeWidth * 0.10f
        val holesStartX = tubeLeft + tubeWidth * 0.27f
        val holesEndX = tubeLeft + tubeWidth * 0.92f
        val holeSpacing = (holesEndX - holesStartX) / (numHoles - 1).coerceAtLeast(1)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onToneHoleTapped != null || onToneHoleReleased != null) {
                        Modifier.pointerInput(numHoles, holesStartX, holeSpacing, holeMidiPitches) {
                            val activePointers = mutableMapOf<Long, Pair<Int, Int>>()

                            fun resolveHoleAt(offset: Offset): Pair<Int, Int> {
                                var bestIdx = 0
                                var minDist = Float.MAX_VALUE
                                for (i in 0 until numHoles) {
                                    val hx = holesStartX + i * holeSpacing
                                    val d = abs(offset.x - hx)
                                    if (d < minDist) {
                                        minDist = d
                                        bestIdx = i
                                    }
                                }
                                return bestIdx to holeMidiPitches[bestIdx]
                            }

                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        val id = change.id.value
                                        if (change.pressed) {
                                            val resolved = resolveHoleAt(change.position)
                                            val prev = activePointers[id]
                                            if (prev == null) {
                                                activePointers[id] = resolved
                                                onToneHoleTapped?.invoke(resolved.first, resolved.second)
                                            } else if (prev != resolved) {
                                                onToneHoleReleased?.invoke(prev.first, prev.second)
                                                activePointers[id] = resolved
                                                onToneHoleTapped?.invoke(resolved.first, resolved.second)
                                            }
                                        } else {
                                            activePointers.remove(id)?.let { released ->
                                                onToneHoleReleased?.invoke(released.first, released.second)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val tubeCorner = CornerRadius(tubeHeight * 0.5f, tubeHeight * 0.5f)

            // Drop shadow under Seasoned Bamboo Flute
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(tubeLeft + 2f, tubeTop + 4f),
                size = Size(tubeWidth, tubeHeight),
                cornerRadius = tubeCorner
            )

            // 3D Cylindrical Seasoned Bamboo Body with Specular Highlights
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9C6638),
                        Color(0xFFE6C280),
                        Color(0xFFFAEDCD),
                        Color(0xFFD4A373),
                        Color(0xFF6F421C)
                    ),
                    startY = tubeTop,
                    endY = tubeTop + tubeHeight
                ),
                topLeft = Offset(tubeLeft, tubeTop),
                size = Size(tubeWidth, tubeHeight),
                cornerRadius = tubeCorner
            )

            // Natural Bamboo Node Ring Ridge near Headjoint
            val nodeX = tubeLeft + tubeWidth * 0.055f
            drawLine(
                color = Color(0xFF582F0E),
                start = Offset(nodeX, tubeTop),
                end = Offset(nodeX, tubeTop + tubeHeight),
                strokeWidth = 3.dp.toPx()
            )

            // Silk Thread Bindings (Crimson & Gold Woven Dhaaga)
            val bindingPositions = floatArrayOf(0.030f, 0.165f, 0.195f, 0.955f)
            val bindingW = (tubeWidth * 0.018f).coerceIn(5f, 14f)
            for (bFrac in bindingPositions) {
                val bx = tubeLeft + tubeWidth * bFrac
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF7F0000), Color(0xFFD32F2F), Color(0xFFFFD54F), Color(0xFFD32F2F), Color(0xFF7F0000)),
                        startY = tubeTop,
                        endY = tubeTop + tubeHeight
                    ),
                    topLeft = Offset(bx, tubeTop + 1f),
                    size = Size(bindingW, tubeHeight - 2f)
                )
            }

            // Embouchure Blowing Hole (Mukharandhra) with Reactive Acoustic Breath Ripples
            val embRadX = (tubeHeight * 0.25f).coerceIn(5f, 14.dp.toPx())
            val embRadY = embRadX * 0.78f
            if (isAnyPlaying) {
                drawCircle(color = accentColor.copy(alpha = 0.35f), radius = embRadX * 1.7f, center = Offset(embouchureX, vp.centerY))
                drawCircle(color = Color.White.copy(alpha = 0.25f), radius = embRadX * 1.25f, center = Offset(embouchureX, vp.centerY), style = Stroke(width = 1.2.dp.toPx()))
            }
            drawOval(
                color = Color(0xFF1A0F0A),
                topLeft = Offset(embouchureX - embRadX, vp.centerY - embRadY),
                size = Size(embRadX * 2f, embRadY * 2f)
            )
            drawOval(
                color = if (isAnyPlaying) accentColor else Color(0xFF5D4037),
                topLeft = Offset(embouchureX - embRadX, vp.centerY - embRadY),
                size = Size(embRadX * 2f, embRadY * 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Tone Holes with Cumulative Fingering Visualization (Closed ●, Half ◐, Open ○)
            val maxHoleRad = min(tubeHeight * 0.32f, holeSpacing * 0.36f).coerceIn(5.dp.toPx(), 15.dp.toPx())
            holeLabelPaint.textSize = (maxHoleRad * 0.85f).coerceIn(6.5f.dp.toPx(), 11.dp.toPx())
            subLabelPaint.textSize = (maxHoleRad * 0.72f).coerceIn(6.dp.toPx(), 9.5f.dp.toPx())

            for (index in 0 until numHoles) {
                val midi = holeMidiPitches[index]
                val semi = (midi % 12 + 12) % 12
                val hx = holesStartX + index * holeSpacing
                val hy = vp.centerY

                val isTargetPitch = activeNotes.contains(midi) || activeMidiNote == midi ||
                        (primaryPitch != null && (primaryPitch % 12 + 12) % 12 == semi)
                val closureFrac = getHoleClosureState(index, primarySemitoneFromSa)
                val holeRad = if (isTargetPitch) maxHoleRad * 1.10f else maxHoleRad * 0.90f

                // Base dark bamboo bore hole
                drawCircle(color = Color(0xFF1E0F06), radius = holeRad, center = Offset(hx, hy))

                // If hole is Closed (1.0f) or Half-Closed (0.5f) for the current note, draw shaded finger cover!
                if (closureFrac >= 0.9f) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.78f),
                        radius = holeRad * 0.92f,
                        center = Offset(hx, hy)
                    )
                } else if (closureFrac >= 0.4f) {
                    // Half-hole shading (left half covered)
                    drawArc(
                        color = accentColor.copy(alpha = 0.85f),
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(hx - holeRad, hy - holeRad),
                        size = Size(holeRad * 2f, holeRad * 2f)
                    )
                }

                if (isTargetPitch) {
                    drawCircle(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.42f),
                        radius = holeRad * 1.38f,
                        center = Offset(hx, hy)
                    )
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        radius = holeRad,
                        center = Offset(hx, hy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = if (closureFrac > 0f) Color.White.copy(alpha = 0.7f) else Color(0xFF6D4C41),
                        radius = holeRad,
                        center = Offset(hx, hy),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                val westernName = NOTE_NAMES[semi]
                val sargamName = holeNames.getOrElse(index) { SARGAM_NAMES[((semi - normalizedRoot) % 12 + 12) % 12] }

                if (showNoteNames || isTargetPitch) {
                    holeLabelPaint.color = if (isTargetPitch) android.graphics.Color.rgb(255, 235, 59) else android.graphics.Color.WHITE
                    drawContext.canvas.nativeCanvas.drawText(
                        westernName,
                        hx,
                        hy + holeLabelPaint.textSize * 0.36f,
                        holeLabelPaint
                    )
                }

                val sargamY = if (vp.bottom - (tubeTop + tubeHeight) >= subLabelPaint.textSize + 3f) {
                    tubeTop + tubeHeight + subLabelPaint.textSize + 2f
                } else {
                    tubeTop + tubeHeight - 3f
                }
                subLabelPaint.color = if (isTargetPitch) android.graphics.Color.rgb(255, 235, 59) else android.graphics.Color.argb(195, 225, 200, 170)
                drawContext.canvas.nativeCanvas.drawText(sargamName, hx, sargamY, subLabelPaint)
            }
        }
    }
}

@Composable
fun UnifiedTablaVisualizer(
    activeBol: String? = null,
    accentColor: Color = Color(0xFFFF9800),
    showLabels: Boolean = true,
    dayanTonicName: String = "Sa (C)",
    bayanBassName: String = "Ge/Ghe",
    onBolTapped: ((String) -> Unit)? = null,
    onBolReleased: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var localPressedBol by remember { mutableStateOf<String?>(null) }
    val effectiveBol = (activeBol ?: localPressedBol)?.uppercase()

    val isBayanActive = effectiveBol != null && effectiveBol in setOf("GHE", "GE", "KA", "KAT", "DHA", "DHIN", "DHIR")
    val isDayanActive = effectiveBol != null && effectiveBol in setOf("DHA", "DHIN", "NA", "TA", "TIN", "TI", "TE", "RE", "TIT", "TUN")

    val bayanScale by animateFloatAsState(
        targetValue = if (isBayanActive) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bayan_pulse"
    )
    val dayanScale by animateFloatAsState(
        targetValue = if (isDayanActive) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dayan_pulse"
    )

    val titlePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(255, 213, 79)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val zoneLabelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val subPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(195, 230, 220, 205)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A1B12), Color(0xFF190F09), Color(0xFF0F0905))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, Color(0xFF8D5B34), RoundedCornerShape(10.dp))
    ) {
        val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val vp = remember(canvasW, canvasH) {
            computeAspectSafeViewport(
                canvasWidth = canvasW,
                canvasHeight = canvasH,
                minAspect = 1.6f,
                maxAspect = 6.5f,
                referenceHeight = 145f,
                horizontalPaddingFrac = 0.02f,
                verticalPaddingFrac = 0.03f
            )
        }

        val showTopBottomText = showLabels && vp.height >= 64f
        val topLabelSpace = if (showTopBottomText) (vp.height * 0.14f).coerceIn(10f, 20f) else 2f
        val bottomLabelSpace = if (showTopBottomText) (vp.height * 0.14f).coerceIn(10f, 20f) else 2f
        val availDrumH = (vp.height - topLabelSpace - bottomLabelSpace).coerceAtLeast(24f)

        val maxRadius = min(availDrumH * 0.48f, vp.width * 0.21f).coerceAtLeast(12f)
        val bayanRad = maxRadius
        val dayanRad = maxRadius * 0.90f

        val centerY = vp.top + topLabelSpace + availDrumH * 0.5f
        val bayanCenter = Offset(vp.centerX - maxRadius * 1.32f, centerY)
        val dayanCenter = Offset(vp.centerX + maxRadius * 1.32f, centerY)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onBolTapped != null || onBolReleased != null) {
                        Modifier.pointerInput(bayanCenter, dayanCenter, bayanRad, dayanRad) {
                            val activePointers = mutableMapOf<Long, String>()

                            fun resolveBolAt(pos: Offset): String? {
                                val distBayan = hypot(pos.x - bayanCenter.x, pos.y - bayanCenter.y)
                                if (distBayan <= bayanRad * 1.12f) {
                                    val syahiCenter = Offset(bayanCenter.x + bayanRad * 0.16f, bayanCenter.y - bayanRad * 0.12f)
                                    val distSyahi = hypot(pos.x - syahiCenter.x, pos.y - syahiCenter.y)
                                    return when {
                                        distSyahi <= bayanRad * 0.40f -> "GHE"
                                        distBayan <= bayanRad * 0.76f -> "GE"
                                        else -> "KA"
                                    }
                                }

                                val distDayan = hypot(pos.x - dayanCenter.x, pos.y - dayanCenter.y)
                                if (distDayan <= dayanRad * 1.12f) {
                                    return when {
                                        distDayan <= dayanRad * 0.42f -> "TI"
                                        distDayan <= dayanRad * 0.74f -> "TIN"
                                        else -> "NA"
                                    }
                                }
                                return null
                            }

                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        val id = change.id.value
                                        if (change.pressed) {
                                            val bol = resolveBolAt(change.position)
                                            val prev = activePointers[id]
                                            if (prev == null && bol != null) {
                                                activePointers[id] = bol
                                                localPressedBol = bol
                                                onBolTapped?.invoke(bol)
                                            } else if (prev != null && bol != null && prev != bol) {
                                                onBolReleased?.invoke(prev)
                                                activePointers[id] = bol
                                                localPressedBol = bol
                                                onBolTapped?.invoke(bol)
                                            }
                                        } else {
                                            activePointers.remove(id)?.let { released ->
                                                if (localPressedBol == released) localPressedBol = null
                                                onBolReleased?.invoke(released)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            fun drawTablaDrum(
                center: Offset,
                baseRadius: Float,
                scaleFactor: Float,
                isBayan: Boolean,
                isActive: Boolean
            ) {
                val r = baseRadius * scaleFactor

                // 1. Velvet Cushioned Chutta Ring & Wooden Gatta Tuning Pegs around Perimeter
                drawCircle(
                    color = Color.Black.copy(alpha = 0.58f),
                    radius = r * 1.12f,
                    center = Offset(center.x + 2f, center.y + 4f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8E0000), Color(0xFF4A0808)),
                        center = center,
                        radius = r * 1.10f
                    ),
                    radius = r * 1.09f,
                    center = center
                )

                // 8 Cylindrical Rosewood Gatta Tuning Blocks around the Dayan/Bayan shell
                for (gIdx in 0 until 8) {
                    val gAngle = (gIdx * 45.0 + 22.5) * (Math.PI / 180.0)
                    val gx = center.x + (r * 1.02f * cos(gAngle)).toFloat()
                    val gy = center.y + (r * 1.02f * sin(gAngle)).toFloat()
                    drawCircle(
                        color = if (isBayan) Color(0xFFB87333) else Color(0xFF5D2906),
                        radius = (r * 0.08f).coerceAtLeast(2f),
                        center = Offset(gx, gy)
                    )
                }

                // 2. Braided Gajra Outer Leather Rim with 16 Baddhi Strap Laces
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE0D6CC), Color(0xFFA1887F), Color(0xFF4E342E)),
                        center = center,
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
                for (peg in 0 until 16) {
                    val angle = (peg * 22.5) * (Math.PI / 180.0)
                    val pStart = Offset(
                        center.x + (r * 0.87f * cos(angle)).toFloat(),
                        center.y + (r * 0.87f * sin(angle)).toFloat()
                    )
                    val pEnd = Offset(
                        center.x + (r * 1.02f * cos(angle)).toFloat(),
                        center.y + (r * 1.02f * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF3E2723),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = (r * 0.065f).coerceIn(1f, 3.8f),
                        cap = StrokeCap.Round
                    )
                }

                // 3. Kinar / Chanti (Outer Goat-Skin Ring)
                val kinarR = r * 0.86f
                val isKinarHit = isActive && effectiveBol in setOf("KA", "KAT", "NA", "TA", "DHA")
                drawCircle(
                    color = if (isKinarHit) Color(0xFFFFF59D) else Color(0xFFF7F2EA),
                    radius = kinarR,
                    center = center
                )
                drawCircle(
                    color = if (isKinarHit) accentColor else Color(0xFF8D6E63),
                    radius = kinarR,
                    center = center,
                    style = Stroke(width = if (isKinarHit) 2.dp.toPx() else 1.dp.toPx())
                )

                // 4. Maidan / Sur (Resonant Middle Membrane)
                val maidanR = r * 0.68f
                val isMaidanHit = isActive && effectiveBol in setOf("GE", "TIN", "DHIN", "TUN")
                drawCircle(
                    color = if (isMaidanHit) Color(0xFFFFE082) else Color(0xFFEDE0D4),
                    radius = maidanR,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFB09B88),
                    radius = maidanR,
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // 5. Syahi (Black Iron-Paste Tuning Circle: Off-Center on Bayan, Centered on Dayan)
                val syahiCenter = if (isBayan) {
                    Offset(center.x + r * 0.15f, center.y - r * 0.11f)
                } else {
                    center
                }
                val syahiR = if (isBayan) r * 0.36f else r * 0.38f
                val isSyahiHit = isActive && effectiveBol in setOf("GHE", "DHA", "DHIN", "TI", "TE", "RE", "TIT")

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isSyahiHit || isActive) {
                            listOf(Color(0xFFFFEB3B), accentColor, Color(0xFF1C1C1C))
                        } else {
                            listOf(Color(0xFF383838), Color(0xFF1A1A1A), Color(0xFF0A0A0A))
                        },
                        center = syahiCenter,
                        radius = syahiR
                    ),
                    radius = syahiR,
                    center = syahiCenter
                )
                drawCircle(
                    color = Color.White.copy(alpha = if (isActive) 0.7f else 0.20f),
                    radius = syahiR * 0.72f,
                    center = syahiCenter,
                    style = Stroke(width = 0.9.dp.toPx())
                )

                zoneLabelPaint.textSize = (syahiR * 0.54f).coerceIn(7.dp.toPx(), 12.dp.toPx())
                zoneLabelPaint.color = android.graphics.Color.WHITE
                val centerBolLabel = when {
                    isActive && effectiveBol != null -> effectiveBol
                    isBayan -> "GHE"
                    else -> "DHA"
                }
                drawContext.canvas.nativeCanvas.drawText(
                    centerBolLabel,
                    syahiCenter.x,
                    syahiCenter.y + zoneLabelPaint.textSize * 0.35f,
                    zoneLabelPaint
                )
            }

            drawTablaDrum(bayanCenter, bayanRad, bayanScale, isBayan = true, isActive = isBayanActive)
            drawTablaDrum(dayanCenter, dayanRad, dayanScale, isBayan = false, isActive = isDayanActive)

            if (showTopBottomText) {
                titlePaint.textSize = (topLabelSpace * 0.65f).coerceIn(7.5f.dp.toPx(), 11.dp.toPx())
                subPaint.textSize = (bottomLabelSpace * 0.58f).coerceIn(6.5f.dp.toPx(), 9.5f.dp.toPx())

                val topY = vp.top + titlePaint.textSize
                drawContext.canvas.nativeCanvas.drawText("BAYAN • DAGGA ($bayanBassName)", bayanCenter.x, topY, titlePaint)
                drawContext.canvas.nativeCanvas.drawText("DAYAN • TABLA ($dayanTonicName)", dayanCenter.x, topY, titlePaint)

                val botY = vp.bottom - 2f
                drawContext.canvas.nativeCanvas.drawText("Syahi: Ghe • Maidan: Ge • Rim: Ka", bayanCenter.x, botY, subPaint)
                drawContext.canvas.nativeCanvas.drawText("Syahi: Ti/Te • Maidan: Tin • Rim: Na/Ta", dayanCenter.x, botY, subPaint)
            }
        }
    }
}

@Composable
fun UnifiedSitarVisualizer(
    activeNotes: List<Int> = emptyList(),
    activeMidiNote: Int? = null,
    rootNote: Int = 0,
    accentColor: Color = Color(0xFFFFB300),
    showNoteNames: Boolean = true,
    numFrets: Int = 16,
    stringSetup: List<Triple<String, Int, Float>> = listOf(
        Triple("Ma", 53, 2.8f),
        Triple("Sa", 48, 2.5f),
        Triple("Pa", 43, 3.2f),
        Triple("Sa'", 36, 3.6f),
        Triple("Pa'", 55, 1.6f),
        Triple("Sa", 60, 1.3f),
        Triple("Sa''", 72, 1.0f)
    ),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    onFretTapped: ((fretIndex: Int, midiNote: Int) -> Unit)? = null,
    onFretReleased: ((fretIndex: Int, midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sitarStrings = stringSetup
    val numStrings = sitarStrings.size.coerceAtLeast(1)
    val safeNumFrets = numFrets.coerceIn(7, 22)
    val normalizedRoot = remember(rootNote) { (rootNote % 12 + 12) % 12 }

    val filteredActiveNotes = remember(activeNotes, isLeftHandHidden, isRightHandHidden) {
        activeNotes.filter { midi ->
            val isTreble = midi >= 60
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }
    }
    val effectiveActiveMidi = remember(activeMidiNote, isLeftHandHidden, isRightHandHidden) {
        if (activeMidiNote == null) null
        else {
            val isTreble = activeMidiNote >= 60
            if (isTreble && isRightHandHidden) null
            else if (!isTreble && isLeftHandHidden) null
            else activeMidiNote
        }
    }

    val allUniquePitches = remember(filteredActiveNotes, effectiveActiveMidi) {
        (filteredActiveNotes + listOfNotNull(effectiveActiveMidi)).distinct()
    }
    val distinctPitchClasses = remember(allUniquePitches) {
        allUniquePitches.map { (it % 12 + 12) % 12 }.toSet()
    }
    val isScaleMode = remember(distinctPitchClasses) {
        distinctPitchClasses.size >= 6
    }

    val resolvedSitarPoints = remember(allUniquePitches, sitarStrings, safeNumFrets, isScaleMode) {
        val points = mutableSetOf<Pair<Int, Int>>()
        if (isScaleMode) {
            for (sIdx in 0..min(1, sitarStrings.lastIndex)) {
                val baseMidi = sitarStrings[sIdx].second
                for (fret in 0..safeNumFrets) {
                    val semi = ((baseMidi + fret) % 12 + 12) % 12
                    if (distinctPitchClasses.contains(semi)) {
                        points.add(sIdx to fret)
                    }
                }
            }
        } else {
            allUniquePitches.forEach { midi ->
                val candidates = mutableListOf<Pair<Int, Int>>()
                sitarStrings.forEachIndexed { sIdx, (_, baseMidi, _) ->
                    val fret = midi - baseMidi
                    if (fret in 0..safeNumFrets) {
                        candidates.add(sIdx to fret)
                    }
                }
                val best = candidates.minByOrNull { (sIdx, fret) ->
                    when {
                        sIdx == 0 -> fret * 2
                        sIdx == 1 -> 8 + fret * 3
                        fret == 0 && sIdx >= 4 -> 4
                        sIdx in 2..3 -> 22 + fret * 4
                        else -> 60 + fret * 8
                    }
                }
                if (best != null) points.add(best)
            }
        }
        points
    }

    val r = 0.056125687f
    val fretScaleFactor = remember(safeNumFrets) {
        var sum = 0f
        for (f in 1..safeNumFrets) {
            sum += (1f - r).toDouble().pow(f.toDouble()).toFloat() * r
        }
        sum.coerceAtLeast(0.001f)
    }

    val pegTextPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val beadTextPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val fretNumPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(195, 240, 210, 155)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C1608), Color(0xFF1A0C04), Color(0xFF120802))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, Color(0xFFB58500), RoundedCornerShape(10.dp))
    ) {
        val totalWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val totalHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val vp = remember(totalWidth, totalHeight) {
            computeAspectSafeViewport(
                canvasWidth = totalWidth,
                canvasHeight = totalHeight,
                minAspect = 2.0f,
                maxAspect = 11.5f,
                referenceHeight = 165f,
                horizontalPaddingFrac = 0.010f,
                verticalPaddingFrac = 0.020f
            )
        }

        val s = vp.scale
        val leftPad = vp.left + (46f * s).coerceIn(30f, vp.width * 0.15f)
        val rightTumbaW = (58f * s).coerceIn(36f, vp.width * 0.18f)
        val rightPad = (totalWidth - vp.right) + rightTumbaW
        val topPad = vp.top + (10f * s).coerceAtLeast(4f)
        val bottomPad = (totalHeight - vp.bottom) + (18f * s).coerceAtLeast(10f)

        val playableW = (totalWidth - leftPad - rightPad).coerceAtLeast(1f)
        val playableH = (totalHeight - topPad - bottomPad).coerceAtLeast(1f)

        val fretXPositions = remember(leftPad, playableW, safeNumFrets, fretScaleFactor) {
            val list = FloatArray(safeNumFrets + 1)
            list[0] = leftPad
            var acc = 0f
            for (f in 1..safeNumFrets) {
                val frac = (1f - r).toDouble().pow(f.toDouble()).toFloat() * r
                acc += (frac / fretScaleFactor) * playableW
                list[f] = leftPad + acc
            }
            list
        }

        fun getFretCenterX(f: Int): Float {
            if (f <= 0) return leftPad - (10f * s).coerceIn(6f, 14f)
            val start = fretXPositions[(f - 1).coerceIn(0, safeNumFrets)]
            val end = fretXPositions[f.coerceIn(0, safeNumFrets)]
            return (start + end) * 0.5f
        }

        fun getYForString(sIdx: Int): Float {
            val spacing = playableH / (numStrings - 1).coerceAtLeast(1)
            return if (numStrings == 1) topPad + playableH * 0.5f else topPad + sIdx * spacing
        }

        val stringSpacing = (playableH / (numStrings - 1).coerceAtLeast(1)).coerceAtLeast(6f)
        val neckTop = topPad - stringSpacing * 0.35f
        val neckBottom = topPad + playableH + stringSpacing * 0.35f
        val neckRight = totalWidth - rightPad + (6f * s)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onFretTapped != null || onFretReleased != null) {
                        Modifier.pointerInput(safeNumFrets, numStrings, leftPad, playableW, fretXPositions) {
                            val activePointers = mutableMapOf<Long, Pair<Int, Int>>()

                            fun resolveAt(offset: Offset): Pair<Int, Int> {
                                val tappedFret = if (offset.x < leftPad) {
                                    0
                                } else {
                                    var matched = safeNumFrets
                                    for (f in 1..safeNumFrets) {
                                        if (offset.x <= fretXPositions[f]) {
                                            matched = f
                                            break
                                        }
                                    }
                                    matched
                                }

                                var closestString = 0
                                var minDist = Float.MAX_VALUE
                                for (sIdx in 0 until numStrings) {
                                    val sy = getYForString(sIdx)
                                    val d = abs(offset.y - sy)
                                    if (d < minDist) {
                                        minDist = d
                                        closestString = sIdx
                                    }
                                }

                                val baseMidi = sitarStrings.getOrNull(closestString)?.second ?: 48
                                return tappedFret to (baseMidi + tappedFret)
                            }

                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        val id = change.id.value
                                        if (change.pressed) {
                                            val resolved = resolveAt(change.position)
                                            val prev = activePointers[id]
                                            if (prev == null) {
                                                activePointers[id] = resolved
                                                onFretTapped?.invoke(resolved.first, resolved.second)
                                            } else if (prev != resolved) {
                                                onFretReleased?.invoke(prev.first, prev.second)
                                                activePointers[id] = resolved
                                                onFretTapped?.invoke(resolved.first, resolved.second)
                                            }
                                        } else {
                                            activePointers.remove(id)?.let { released ->
                                                onFretReleased?.invoke(released.first, released.second)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            // 1. MAJESTIC CARVED RESONANT TUMBA (GOURD) & TABLI SOUNDBOARD ON RIGHT
            val tumbaCenterX = neckRight + rightTumbaW * 0.46f
            val tumbaCenterY = (neckTop + neckBottom) * 0.5f
            val tumbaRx = rightTumbaW * 0.54f
            val tumbaRy = ((neckBottom - neckTop) * 0.58f).coerceAtMost(vp.height * 0.49f)

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC86D24), Color(0xFF7F380C), Color(0xFF361404)),
                    center = Offset(tumbaCenterX - tumbaRx * 0.2f, tumbaCenterY - tumbaRy * 0.2f),
                    radius = maxOf(tumbaRx, tumbaRy) * 1.15f
                ),
                topLeft = Offset(tumbaCenterX - tumbaRx, tumbaCenterY - tumbaRy),
                size = Size(tumbaRx * 2f, tumbaRy * 2f)
            )
            // Ivory Floral Gul & Tabli Trim
            drawOval(
                color = Color(0xFFFFF8E1).copy(alpha = 0.55f),
                topLeft = Offset(tumbaCenterX - tumbaRx, tumbaCenterY - tumbaRy),
                size = Size(tumbaRx * 2f, tumbaRy * 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Seasoned Teakwood Hollow Dand (Neck)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF43210E), Color(0xFF261207), Color(0xFF1B0C04), Color(0xFF3B1D0C)),
                    startY = neckTop,
                    endY = neckBottom
                ),
                topLeft = Offset(leftPad - 4f, neckTop),
                size = Size(neckRight - leftPad + 4f, neckBottom - neckTop),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            drawLine(
                color = Color(0xFFFFF8E1).copy(alpha = 0.42f),
                start = Offset(leftPad, neckTop + 2f),
                end = Offset(neckRight - 4f, neckTop + 2f),
                strokeWidth = 1.1.dp.toPx()
            )
            drawLine(
                color = Color(0xFFFFF8E1).copy(alpha = 0.42f),
                start = Offset(leftPad, neckBottom - 2f),
                end = Offset(neckRight - 4f, neckBottom - 2f),
                strokeWidth = 1.1.dp.toPx()
            )

            // 3. 13 Sympathetic Taraf Strings & Ivory Eyelets (Shimmer when matching pitch class is played!)
            val numTaraf = 13
            for (tIdx in 0 until numTaraf) {
                val ty = neckTop + (neckBottom - neckTop) * (0.16f + 0.68f * (tIdx / (numTaraf - 1f)))
                val eyeletX = leftPad + playableW * (0.08f + 0.78f * (tIdx / (numTaraf - 1f)))
                val isSympatheticResonating = distinctPitchClasses.contains(tIdx % 12)
                drawLine(
                    color = if (isSympatheticResonating) Color(0xFFFFD54F).copy(alpha = 0.55f) else Color(0xFFFFECB3).copy(alpha = 0.16f),
                    start = Offset(eyeletX, ty),
                    end = Offset(tumbaCenterX, ty),
                    strokeWidth = if (isSympatheticResonating) 1.3.dp.toPx() else 0.7.dp.toPx()
                )
                drawCircle(
                    color = if (isSympatheticResonating) Color(0xFFFFEB3B) else Color(0xFFFFF8E1).copy(alpha = 0.38f),
                    radius = (1.8f * s).coerceIn(1f, 3f),
                    center = Offset(eyeletX, ty)
                )
            }

            // 4. Ivory Targahan (Nut) & Carved Deer-Horn/Bone Ghoraj (Jawari Bridge on the Tumba)
            drawLine(
                color = Color(0xFFFFF8E1),
                start = Offset(leftPad, neckTop + 2f),
                end = Offset(leftPad, neckBottom - 2f),
                strokeWidth = (4.2f * s).coerceIn(2.5f, 6.5f),
                cap = StrokeCap.Round
            )

            val bridgeW = (12f * s).coerceIn(7f, 18f)
            val bridgeX = tumbaCenterX - bridgeW * 0.5f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF8E1), Color(0xFFD7CCC8), Color(0xFF8D6E63)),
                    startY = neckTop + 6f,
                    endY = neckBottom - 6f
                ),
                topLeft = Offset(bridgeX, neckTop + 6f),
                size = Size(bridgeW, (neckBottom - neckTop) - 12f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // 5. Curved Brass Frets (Parda) with Silk Thread Knots & Sargam Thaat Labels
            val fretStroke = (2.6f * s).coerceIn(1.5f, 4.2f)
            fretNumPaint.textSize = (9.0f * s).coerceIn(6.5f.dp.toPx(), 10.5f.dp.toPx())
            val fretLabelY = (neckBottom + fretNumPaint.textSize + 2f * s).coerceAtMost(size.height - 2f)
            val bajTarOpenMidi = sitarStrings.firstOrNull()?.second ?: 53

            for (f in 1..safeNumFrets) {
                val fx = fretXPositions[f]
                val cx = getFretCenterX(f)
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(fx + 1.5f, neckTop + 2f),
                    end = Offset(fx + 1.5f, neckBottom - 2f),
                    strokeWidth = fretStroke
                )
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFB58500), Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFB58500)),
                        startY = neckTop,
                        endY = neckBottom
                    ),
                    start = Offset(fx, neckTop + 2f),
                    end = Offset(fx, neckBottom - 2f),
                    strokeWidth = fretStroke,
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color(0xFFC62828), radius = fretStroke * 0.78f, center = Offset(fx, neckTop + 2.5f))
                drawCircle(color = Color(0xFFC62828), radius = fretStroke * 0.78f, center = Offset(fx, neckBottom - 2.5f))

                // Educational Sargam Thaat Note Label below primary Bilawal Pardas
                val semiOnBaj = ((bajTarOpenMidi + f - normalizedRoot) % 12 + 12) % 12
                if (semiOnBaj in intArrayOf(0, 2, 4, 5, 7, 9, 11)) {
                    drawContext.canvas.nativeCanvas.drawText(
                        SARGAM_NAMES[semiOnBaj],
                        cx,
                        fretLabelY,
                        fretNumPaint
                    )
                }
            }

            // 6. Main Playing & Chikari Strings + Peg Labels
            val pegCenterX = vp.left + (leftPad - vp.left) * 0.42f
            val pegRad = (stringSpacing * 0.40f).coerceIn(5.dp.toPx(), 10.5f.dp.toPx())
            pegTextPaint.textSize = (pegRad * 0.92f).coerceIn(6.dp.toPx(), 9.5f.dp.toPx())

            sitarStrings.forEachIndexed { sIdx, (name, _, gauge) ->
                val sy = getYForString(sIdx)
                val isStringPlucked = resolvedSitarPoints.any { it.first == sIdx }
                val scaledGauge = (gauge * 0.65f * s).coerceIn(0.8f, 3.2f)
                val wireColor = when (sIdx) {
                    0 -> Color(0xFFFFF9C4)
                    1, 2, 3 -> Color(0xFFFFD54F)
                    else -> Color(0xFFE0E0E0)
                }

                drawLine(
                    color = if (isStringPlucked) accentColor.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f),
                    start = Offset(leftPad, sy + 1.2f),
                    end = Offset(bridgeX + bridgeW * 0.5f, sy + 1.2f),
                    strokeWidth = if (isStringPlucked) scaledGauge * 2.2f else scaledGauge
                )
                drawLine(
                    color = if (isStringPlucked) Color.White else wireColor,
                    start = Offset(leftPad, sy),
                    end = Offset(bridgeX + bridgeW * 0.5f, sy),
                    strokeWidth = scaledGauge
                )

                drawCircle(
                    color = if (sIdx == 0) Color(0xFF3E1F0D) else Color(0xFF1B0C05),
                    radius = pegRad,
                    center = Offset(pegCenterX, sy)
                )
                drawCircle(
                    color = if (isStringPlucked) Color(0xFFFFEB3B) else if (sIdx == 0) Color(0xFFFFD54F) else Color(0xFFBCAAA4).copy(alpha = 0.75f),
                    radius = pegRad,
                    center = Offset(pegCenterX, sy),
                    style = Stroke(width = 1.1.dp.toPx())
                )
                pegTextPaint.color = if (sIdx == 0 || isStringPlucked) android.graphics.Color.rgb(255, 213, 79) else android.graphics.Color.WHITE
                drawContext.canvas.nativeCanvas.drawText(
                    name,
                    pegCenterX,
                    sy + pegTextPaint.textSize * 0.36f,
                    pegTextPaint
                )
            }

            // 7. Active Note Beads & Dual Sargam/Western Labels
            val maxBeadRad = (stringSpacing * 0.46f).coerceIn(4.8f.dp.toPx(), 12.dp.toPx())

            sitarStrings.forEachIndexed { sIdx, (_, baseMidi, _) ->
                val sy = getYForString(sIdx)
                for (fret in 0..safeNumFrets) {
                    val midi = baseMidi + fret
                    val semi = (midi % 12 + 12) % 12
                    val isResolvedActive = resolvedSitarPoints.contains(sIdx to fret)
                    val isExactPlaying = effectiveActiveMidi != null && midi == effectiveActiveMidi && isResolvedActive
                    val isRoot = semi == normalizedRoot

                    if (isResolvedActive) {
                        val cx = getFretCenterX(fret)
                        val fretW = if (fret == 0) 20f * s else (fretXPositions[fret] - fretXPositions[fret - 1])
                        val beadRad = min(maxBeadRad, (fretW * 0.45f).coerceAtLeast(4.5f.dp.toPx())) *
                                (if (isExactPlaying) 1.08f else if (isScaleMode) 0.85f else 0.96f)

                        val fill = when {
                            isExactPlaying -> Color(0xFFFFEB3B)
                            isRoot -> Color(0xFFFF5252)
                            else -> accentColor
                        }

                        drawCircle(color = fill.copy(alpha = 0.42f), radius = beadRad * 1.35f, center = Offset(cx, sy))
                        drawCircle(color = fill, radius = beadRad, center = Offset(cx, sy))
                        drawCircle(color = Color.White, radius = beadRad, center = Offset(cx, sy), style = Stroke(width = 1.3.dp.toPx()))

                        val relSargam = SARGAM_NAMES[((semi - normalizedRoot) % 12 + 12) % 12]
                        val label = if (showNoteNames) NOTE_NAMES[semi] else relSargam
                        beadTextPaint.color = if (isExactPlaying) android.graphics.Color.rgb(165, 35, 0) else android.graphics.Color.BLACK
                        beadTextPaint.textSize = (beadRad * (if (label.length > 1) 0.88f else 1.05f)).coerceIn(5.5f.dp.toPx(), 10.5f.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            cx,
                            sy + beadTextPaint.textSize * 0.36f,
                            beadTextPaint
                        )
                    } else if (sIdx == 0 && fret in intArrayOf(2, 4, 5, 7, 9, 11, 12)) {
                        val cx = getFretCenterX(fret)
                        drawCircle(
                            color = Color(0xFFFFF8E1).copy(alpha = 0.34f),
                            radius = (stringSpacing * 0.14f).coerceIn(1.8f, 3.5f.dp.toPx()),
                            center = Offset(cx, sy)
                        )
                    }
                }
            }
        }
    }
}