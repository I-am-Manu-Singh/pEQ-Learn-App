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
import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
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
    cagedMode: String = "AUTO", // "AUTO", "C", "A", "G", "E", "D", or "ALL"
    fretMinConstrain: Int = 0,
    fretMaxConstrain: Int = 24,
    onFretTapped: ((stringIndex: Int, fret: Int, midiNote: Int) -> Unit)? = null,
    onFretReleased: ((stringIndex: Int, fret: Int, midiNote: Int) -> Unit)? = null,
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

    val defaultOpenStringsMidi = remember(numStrings) {
        when (numStrings) {
            4 -> listOf(69, 64, 60, 67) // Ukulele A4, E4, C4, G4
            7 -> listOf(64, 59, 55, 50, 45, 40, 35) // 7-String Guitar: e4, B3, G3, D3, A2, E2, B1
            else -> listOf(64, 59, 55, 50, 45, 40) // 6-String Guitar: e4, B3, G3, D3, A2, E2
        }
    }

    // --- CHORD VS SCALE DISTINCTION & LOGICAL TRUTH RESOLVER ---
    // A chord has 2 to 5 distinct pitch classes. A 6+ note collection (like a diatonic scale with 7 notes)
    // is a SCALE / MELODY RUN, not a single chord shape.
    val allUniquePitches = remember(filteredActiveNotes, effectiveActiveMidiNote) {
        (filteredActiveNotes + listOfNotNull(effectiveActiveMidiNote)).distinct()
    }
    val distinctPitchClasses = remember(allUniquePitches) {
        allUniquePitches.map { (it % 12 + 12) % 12 }.toSet()
    }
    val isScaleMode = remember(distinctPitchClasses) {
        distinctPitchClasses.size >= 6 // 6 or 7 distinct semitones = scale run!
    }

    val effectiveResolvedChordFrets = remember(chordFrets, allUniquePitches, distinctPitchClasses, isScaleMode, cagedMode, numStrings) {
        if (chordFrets != null) {
            chordFrets
        } else if (isScaleMode) {
            null // Scales should NOT be collapsed into a chord shape!
        } else if (allUniquePitches.size in 2..5 && numStrings >= 6) {
            // Authentic Chord Shape from RealChordsDb
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
                    val base6 = bestPos.frets.reversed() // RealChordsDb is low-to-high, reverse to e4..E2
                    if (numStrings == 7) {
                        base6 + listOf(-1)
                    } else {
                        base6
                    }
                } else null
            } else null
        } else null
    }

    // Exact frets matching currently playing / active notes (for single note melody / scale sequences)
    val activeFretPoints = remember(allUniquePitches, effectiveResolvedChordFrets, defaultOpenStringsMidi, isScaleMode, fretMinConstrain, fretMaxConstrain) {
        if (effectiveResolvedChordFrets != null) {
            emptySet<Pair<Int, Int>>() // Driven by chord shape
        } else {
            val set = mutableSetOf<Pair<Int, Int>>() // Set of (stringIdx, fret)
            allUniquePitches.forEach { midi ->
                val candidates = mutableListOf<Pair<Int, Int>>()
                defaultOpenStringsMidi.forEachIndexed { sIdx, openMidi ->
                    val fret = midi - openMidi
                    if (fret in 0..numFrets) {
                        candidates.add(sIdx to fret)
                    }
                }
                // Filter to constrained fret window (default open pos frets 0..5 or user box)
                val inBox = candidates.filter { it.second in fretMinConstrain..fretMaxConstrain }
                val chosen = if (inBox.isNotEmpty()) {
                    inBox.minByOrNull { it.second } // Most ergonomic fret
                } else {
                    candidates.minByOrNull { kotlin.math.abs(it.second - 2) }
                }
                if (chosen != null) {
                    set.add(chosen)
                }
            }
            set
        }
    }

    LaunchedEffect(allUniquePitches, effectiveResolvedChordFrets, activeFretPoints, isScaleMode) {
        if (allUniquePitches.isNotEmpty()) {
            val noteLabels = allUniquePitches.joinToString { midi ->
                val name = NOTE_NAMES[(midi % 12 + 12) % 12]
                val oct = (midi / 12) - 1
                "$name$oct($midi)"
            }
            val desc = when {
                effectiveResolvedChordFrets != null -> "Chord Shape: $effectiveResolvedChordFrets"
                isScaleMode -> "Scale Mode: ${allUniquePitches.size} notes"
                else -> "Active Frets: $activeFretPoints"
            }
            println("pEQLearn [FretboardFlash]: Flashing ${allUniquePitches.size} note(s): $noteLabels | $desc")
        }
    }
    val normalizedRootNote = remember(rootNote) { (rootNote % 12 + 12) % 12 }

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

    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFF0F1218).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF2E3544).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val leftPad = 46f
        val topPad = 16f
        val rightPad = 20f
        val bottomPad = 26f
        val playableW = (totalWidth - leftPad - rightPad).coerceAtLeast(1f)
        val playableH = totalHeight - topPad - bottomPad

        // Rule of 17.817 fret progression ratio (realistic decreasing fret width towards bridge)
        // Ratio r = 1 - 2^(-1/12) ≈ 0.056125687
        val r = 0.056125687f
        val fretScaleFactor = remember(numFrets, baseFret) {
            var sum = 0f
            for (f in 1..numFrets) {
                val absoluteFret = baseFret + f - 1
                val lengthFraction = kotlin.math.pow((1f - r).toDouble(), absoluteFret.toDouble()).toFloat() * r
                sum += lengthFraction
            }
            sum.coerceAtLeast(0.001f)
        }

        // Compute exact non-linear fret boundaries
        val fretXPositions = remember(playableW, numFrets, baseFret, fretScaleFactor) {
            val list = mutableListOf<Float>()
            var accumulated = 0f
            for (f in 1..numFrets) {
                val absoluteFret = baseFret + f - 1
                val lengthFraction = kotlin.math.pow((1f - r).toDouble(), absoluteFret.toDouble()).toFloat() * r
                accumulated += (lengthFraction / fretScaleFactor) * playableW
                list.add(leftPad + accumulated)
            }
            list
        }

        fun getFretStartX(f: Int): Float = if (f <= 0) leftPad else fretXPositions.getOrElse(f - 1) { totalWidth - rightPad }
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

        fun getYForStringAtX(s: Int, x: Float): Float {
            val t = ((x - leftPad) / playableW).coerceIn(0f, 1f)
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
                    val activeFretPointers = mutableMapOf<Long, Triple<Int, Int, Int>>() // pointerId -> (string, fret, midi)

                    fun resolveFretAt(offset: Offset): Triple<Int, Int, Int> {
                        val tappedFret = if (offset.x < leftPad) {
                            0
                        } else {
                            var matchedFret = numFrets
                            for (f in 1..numFrets) {
                                val wireX = fretXPositions.getOrElse(f - 1) { totalWidth - rightPad }
                                if (offset.x <= wireX) {
                                    matchedFret = f
                                    break
                                }
                            }
                            (baseFret + matchedFret - 1).coerceIn(0, 24)
                        }

                        var closestString = 0
                        var minDistance = Float.MAX_VALUE
                        for (s in 0 until numStrings) {
                            val strY = getYForStringAtX(s, offset.x)
                            val dist = kotlin.math.abs(offset.y - strY)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestString = s
                            }
                        }

                        val baseMidi = defaultOpenStringsMidi.getOrNull(closestString) ?: (openStringsSemis.getOrElse(closestString) { 0 } + 40)
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
        ) {
            val w = size.width
            val pillColor = Color(0xFFFF9E1B)

            // Realistic deep drop shadow behind the wooden fretboard neck
            drawPath(
                path = fretboardPath,
                color = Color.Black.copy(alpha = 0.55f),
                style = Stroke(width = 8.dp.toPx())
            )
            drawPath(
                path = fretboardPath,
                color = Color.Black.copy(alpha = 0.35f),
                style = Stroke(width = 4.dp.toPx())
            )

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
                for (markerFret in intArrayOf(3, 5, 7, 9, 12, 15, 17, 19, 21, 24)) {
                    if (markerFret in baseFret..(baseFret + numFrets - 1)) {
                        val relFret = markerFret - baseFret + 1
                        val inlayX = getFretCenterX(relFret)
                        val t = ((inlayX - leftPad) / (w - leftPad - rightPad)).coerceIn(0f, 1f)
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

            // Fret wires with nickel-silver sheen
            val fretStroke = 1.6.dp.toPx()
            for (f in 1..numFrets) {
                val x = fretXPositions.getOrElse(f - 1) { totalWidth - rightPad }
                val t = ((x - leftPad) / (w - leftPad - rightPad)).coerceIn(0f, 1f)
                val curTop = nutTop + (bridgeTop - nutTop) * t
                val curBottom = nutBottom + (bridgeBottom - nutBottom) * t
                // Subtle fret shadow
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
                    val barreX = getFretCenterX(relFret)
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

            // Permanent C Octave Markers (like Piano Keyboard C1, C2, C3, C4, C5)
            permanentOctavePaint.textSize = 8.5f.dp.toPx()
            for (s in 0 until numStrings) {
                val openSemi = openStringsSemis.getOrNull(s) ?: 0
                val baseStringMidi = defaultOpenStringsMidi.getOrNull(s) ?: (openSemi + 40)
                for (f in 1..numFrets) {
                    val absoluteFret = baseFret + f - 1
                    val fretSemi = (openSemi + absoluteFret) % 12
                    if (fretSemi == 0) {
                        val fullMidiNote = baseStringMidi + absoluteFret
                        val octaveNum = (fullMidiNote / 12) - 1
                        val cx = getFretCenterX(f)
                        val cy = getYForStringAtX(s, cx)
                        drawContext.canvas.nativeCanvas.drawText(
                            "C$octaveNum",
                            cx,
                            cy + 8.5f.dp.toPx(),
                            permanentOctavePaint
                        )
                    }
                }
            }

            // Open string indicators
            val textOffsetY = 4.dp.toPx()
            nutMutedPaint.textSize = 13.dp.toPx()
            val activeChordFrets = effectiveResolvedChordFrets
            if (activeChordFrets != null) {
                for (s in 0 until numStrings) {
                    val f = activeChordFrets.getOrNull(s) ?: -1
                    val y = getYForStringAtX(s, leftPad)
                    val nutIndicatorX = leftPad - 14f
                    val openSemi = (openStringsSemis.getOrNull(s) ?: 0) % 12
                    val openMidi = defaultOpenStringsMidi.getOrNull(s) ?: (openSemi + 40)
                    val openOct = (openMidi / 12) - 1
                    val isOpenPlaying = f == 0 && (
                        (activeCoordinate != null && activeCoordinate.first == s && activeCoordinate.second == 0) ||
                            (effectiveActiveMidiNote != null && openMidi == effectiveActiveMidiNote)
                        )

                    if (f == -1) {
                        drawContext.canvas.nativeCanvas.drawText("✕", nutIndicatorX, y + labelYOff, nutMutedPaint)
                    } else if (f == 0) {
                        val nutRad = if (isOpenPlaying) 9.5f.dp.toPx() else 8.dp.toPx()
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
                        notePillTextPaint.textSize = 8.5f.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(nutText, nutIndicatorX, y + textOffsetY, notePillTextPaint)
                    }
                }
            } else {
                // Scale / Melody note mode: check open strings against active or scale notes
                for (s in 0 until numStrings) {
                    val openSemi = (openStringsSemis.getOrNull(s) ?: 0) % 12
                    val openMidi = defaultOpenStringsMidi.getOrNull(s) ?: (openSemi + 40)
                    val openOct = (openMidi / 12) - 1
                    val isExactOpenActive = (activeCoordinate != null && activeCoordinate.first == s && activeCoordinate.second == 0) ||
                        activeFretPoints.contains(s to 0) ||
                        (effectiveActiveMidiNote != null && effectiveActiveMidiNote == openMidi)
                    val isScaleOpen = isScaleMode && normalizedActiveNotes.contains(openSemi)

                    if (isExactOpenActive || isScaleOpen) {
                        val y = getYForStringAtX(s, leftPad)
                        val nutIndicatorX = leftPad - 14f
                        val isRoot = openSemi == normalizedRootNote
                        val nutRad = if (isExactOpenActive) 10.dp.toPx() else 8.dp.toPx()

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
                            style = Stroke(width = if (isExactOpenActive) 2.2.dp.toPx() else 1.2.dp.toPx())
                        )
                        val nutText = NOTE_NAMES[openSemi]
                        notePillTextPaint.color = if (isExactOpenActive) android.graphics.Color.rgb(180, 40, 0) else android.graphics.Color.WHITE
                        notePillTextPaint.textSize = 8.5f.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(nutText, nutIndicatorX, y + textOffsetY, notePillTextPaint)
                    }
                }
            }

            // Active Fretted Notes
            for (s in 0 until numStrings) {
                val openSemi = openStringsSemis.getOrNull(s) ?: 0
                val stringChordFret = activeChordFrets?.getOrNull(s) ?: -1
                val baseStringMidi = defaultOpenStringsMidi.getOrNull(s) ?: (openSemi + 40)

                for (f in 1..numFrets) {
                    val absoluteFret = baseFret + f - 1
                    val fretSemi = (openSemi + absoluteFret) % 12
                    val fullMidiNote = baseStringMidi + absoluteFret

                    val isChordFret = activeChordFrets != null && stringChordFret > 0 && (
                        stringChordFret == absoluteFret || (baseFret > 1 && stringChordFret == f)
                        )
                    val matchesCoord = activeCoordinate != null && activeCoordinate.first == s && (
                        activeCoordinate.second == absoluteFret || activeCoordinate.second == f
                        )
                    val isExactMidiPlaying = effectiveActiveMidiNote != null && fullMidiNote == effectiveActiveMidiNote
                    val isFretPointActive = activeChordFrets == null && activeFretPoints.contains(s to absoluteFret)
                    val isCurrentlyPlaying = matchesCoord || (if (activeChordFrets != null) isChordFret && isExactMidiPlaying else isExactMidiPlaying || isFretPointActive)

                    // In Scale Mode: highlight valid notes of the scale within the constrained fret window
                    val isScaleFret = isScaleMode && normalizedActiveNotes.contains(fretSemi) && (absoluteFret in fretMinConstrain..fretMaxConstrain)
                    val isRoot = fretSemi == normalizedRootNote

                    if (isChordFret || isCurrentlyPlaying || isScaleFret) {
                        val cx = getFretCenterX(f)
                        val cy = getYForStringAtX(s, cx)
                        val dotRadius = when {
                            isCurrentlyPlaying -> 13.dp.toPx()
                            isScaleFret -> 10.dp.toPx()
                            else -> 11.5.dp.toPx()
                        }

                        val primaryDotColor = when {
                            isCurrentlyPlaying -> Color(0xFFFFEB3B)
                            isRoot && isScaleFret -> Color(0xFFFF5252)
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

                        if (isCurrentlyPlaying || (isRoot && isScaleFret)) {
                            drawCircle(
                                color = Color.White,
                                radius = dotRadius,
                                center = Offset(cx, cy),
                                style = Stroke(width = if (isCurrentlyPlaying) 2.2.dp.toPx() else 1.6.dp.toPx())
                            )
                        }

                        val fingerNum = if (isChordFret) chordFingers?.getOrNull(s) else null

                        val displayText = if (fingerNum != null && fingerNum > 0) {
                            "$fingerNum"
                        } else {
                            NOTE_NAMES[fretSemi]
                        }

                        notePillTextPaint.color = if (isCurrentlyPlaying) android.graphics.Color.rgb(180, 40, 0) else android.graphics.Color.WHITE
                        notePillTextPaint.textSize = (if (displayText.length > 2) 9.dp else if (isScaleFret) 10.dp else 11.dp).toPx()
                        drawContext.canvas.nativeCanvas.drawText(displayText, cx, cy + textOffsetY, notePillTextPaint)
                    }
                }
            }

            // Bottom Fret Numbers
            fretNumPaint.textSize = 11.dp.toPx()
            for (f in intArrayOf(3, 5, 7, 9, 12, 15, 17, 19, 21, 24)) {
                if (f in baseFret..(baseFret + numFrets - 1)) {
                    val relF = f - baseFret + 1
                    val x = getFretCenterX(relF)
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

@Composable
fun pEQInstrumentDropdown(
    selectedInstrument: ChordInstrumentType,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onInstrumentChange: (ChordInstrumentType) -> Unit,
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
                        color = MaterialTheme.colorScheme.onSurface
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
                .width(220.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            ChordInstrumentType.entries.forEach { inst ->
                val isSelected = inst == selectedInstrument
                DropdownMenuItem(
                    leadingIcon = {
                        Text(inst.label, fontSize = 16.sp)
                    },
                    text = {
                        Text(
                            text = inst.title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                        )
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
// HARMONIUM, VIOLIN, & FLUTE UNIFIED INSTRUMENT VISUALIZERS
// ============================================================================

/**
 * UNIFIED HARMONIUM VISUALIZER
 * Features traditional pump-organ bellows texture, wooden reed casing,
 * brass stops, and responsive reed keys (C3 to C6).
 */
@Composable
fun UnifiedHarmoniumVisualizer(
    activeMidiNotes: Map<Int, Boolean> = emptyMap(),
    accentColor: Color = Color(0xFFFFB300),
    activeMidiNote: Int? = null,
    onKeyTapped: ((midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val startMidi = 48 // C3
    val endMidi = 84   // C6 (3 octaves)
    val totalKeys = endMidi - startMidi + 1

    Column(
        modifier = modifier
            .background(Color(0xFF261810)) // Rich rosewood harmonium body
            .padding(4.dp)
    ) {
        // TOP BELLOWS & STOPS PANEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(0xFF3E2723))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Brass decorative stops
                repeat(7) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD54F))
                            .border(1.dp, Color(0xFFB58500), CircleShape)
                    )
                }
            }
            Text(
                text = "HARMONIUM • REED BELLOWS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD54F),
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(3.dp))

        // HARMONIUM KEYBED (REEDS)
        UnifiedPianoKeyboard(
            activeMidiNotes = activeMidiNotes,
            accentColor = accentColor,
            activeCoordinate = activeMidiNote,
            isPlaying = activeMidiNotes.values.any { it } || activeMidiNote != null,
            startNote = startMidi,
            endNote = endMidi,
            fillWidth = true,
            leftHandColor = Color(0xFFFF8F00),
            rightHandColor = Color(0xFFFFD54F),
            onKeyTapped = { onKeyTapped?.invoke(it) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

/**
 * UNIFIED VIOLIN FINGERBOARD VISUALIZER
 * Models an orchestral 4-string fretless fingerboard (G3, D4, A4, E5).
 * Renders fretless intonation markers (tape guides at 1st, 2nd, 3rd, and 4th positions)
 * and finger positions.
 */
@Composable
fun UnifiedViolinFingerboard(
    activeNotes: List<Int> = emptyList(),
    activeMidiNote: Int? = null,
    accentColor: Color = Color(0xFFE040FB),
    showNoteNames: Boolean = true,
    onPositionTapped: ((stringIndex: Int, midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Violin open strings: G3 (55), D4 (62), A4 (69), E5 (76)
    val stringLabels = listOf("E", "A", "D", "G")
    val openStringsMidi = listOf(76, 69, 62, 55)
    val totalPositions = 14 // Half-step intonation positions along neck

    LaunchedEffect(activeNotes, activeMidiNote) {
        val allNotes = (activeNotes + listOfNotNull(activeMidiNote)).distinct()
        if (allNotes.isNotEmpty()) {
            val names = allNotes.joinToString { midi ->
                val name = NOTE_NAMES[midi % 12]
                val oct = (midi / 12) - 1
                "$name$oct($midi)"
            }
            println("pEQLearn [ViolinFingerboard]: Active violin note(s): $names")
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFF1A120B)) // Deep ebony wood
            .border(1.dp, Color(0xFF4A3423))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val totalW = maxWidth
        val totalH = maxHeight

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("VIOLIN (Fretless Fingerboard)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE040FB))
                Text("G3 • D4 • A4 • E5", fontSize = 10.sp, color = Color.White.copy(0.6f))
            }

            // Fingerboard Strings & Intonation Tapes
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                openStringsMidi.forEachIndexed { sIdx, openMidi ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // String Label & Peg
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF3E2723),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(stringLabels[sIdx], fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // String Length with chromatic pitch markers
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (sIdx == 3) 4.dp else (3 - sIdx * 0.5f).dp)
                                .background(Color(0xFFD7CCC8)),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (pos in 0..totalPositions) {
                                val currentMidi = openMidi + pos
                                val isActive = activeNotes.contains(currentMidi) || activeMidiNote == currentMidi

                                Box(
                                    modifier = Modifier
                                        .size(if (isActive) 18.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) accentColor else if (pos in listOf(2, 4, 5, 7, 9, 12)) Color.White.copy(0.35f) else Color.Transparent)
                                        .clickable { onPositionTapped?.invoke(sIdx, currentMidi) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isActive && showNoteNames) {
                                        Text(
                                            text = NOTE_NAMES[currentMidi % 12],
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
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

/**
 * UNIFIED FLUTE / BANSURI VISUALIZER
 * Visualizes a transverse concert flute / Indian Bansuri with tone holes,
 * embouchure hole, and responsive finger coverage for active pitches.
 */
@Composable
fun UnifiedFluteVisualizer(
    activeNotes: List<Int> = emptyList(),
    activeMidiNote: Int? = null,
    accentColor: Color = Color(0xFF00E5FF),
    onToneHoleTapped: ((holeIndex: Int, midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Concert Flute standard register C4 (60) to C7 (96)
    // 7 Main Tone Holes (Sa, Re, Ga, Ma, Pa, Dha, Ni)
    val holeMidiPitches = listOf(60, 62, 64, 65, 67, 69, 71, 72) // C4, D4, E4, F4, G4, A4, B4, C5
    val holeNames = listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5")

    LaunchedEffect(activeNotes, activeMidiNote) {
        val all = (activeNotes + listOfNotNull(activeMidiNote)).distinct()
        if (all.isNotEmpty()) {
            val names = all.joinToString { midi ->
                val name = NOTE_NAMES[midi % 12]
                val oct = (midi / 12) - 1
                "$name$oct($midi)"
            }
            println("pEQLearn [FluteVisualizer]: Active flute pitch: $names")
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF0D1B2A))
            .border(1.dp, Color(0xFF1B263B))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FLUTE & BANSURI", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor)
            Text("Acoustic Woodwind • Chromatic Tone Holes", fontSize = 10.sp, color = Color.White.copy(0.6f))
        }

        // FLUTE BODY (Cylindrical tube with embouchure & tone holes)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFC0C0C0), Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF757575))
                    )
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Embouchure hole (Mouthpiece)
                Box(
                    modifier = Modifier
                        .size(24.dp, 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF212121))
                        .border(1.dp, Color(0xFF424242), RoundedCornerShape(8.dp))
                )

                Spacer(Modifier.width(16.dp))

                // Tone Holes
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    holeMidiPitches.forEachIndexed { index, midi ->
                        val isCovered = activeNotes.contains(midi) || activeMidiNote == midi

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isCovered) accentColor else Color(0xFF1E1E1E))
                                    .border(1.5.dp, if (isCovered) Color.White else Color(0xFF424242), CircleShape)
                                    .clickable { onToneHoleTapped?.invoke(index, midi) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCovered) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                            Text(
                                text = holeNames[index],
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCovered) accentColor else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
