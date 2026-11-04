package com.creativeblisslabs.peqlearn.app.ui.components

import kotlin.math.pow
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
    // Intelligent position solver: resolves pitches to an ergonomic guitar hand box (preferring standard frets 0..9)
    // to prevent impossible finger leaps and scattered dots across all 6 strings
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
                if (candidates.isNotEmpty()) {
                    // Check if candidate exists within user-specified constraint box
                    val inUserBox = candidates.filter { it.second in fretMinConstrain..fretMaxConstrain }
                    val searchPool = if (inUserBox.isNotEmpty()) inUserBox else candidates

                    // Ergonomic hand scoring:
                    // 1. Natural guitar melody playing comfortably sits in the lower/middle neck (frets 0..9).
                    // 2. High frets (> 12) on low wound strings (E2, A2, D3) are unergonomic for fast melodies like Turkish March.
                    // 3. Score minimizes unergonomic stretch: prefer lower frets (0..7) and strings matching melodic register.
                    val chosen = searchPool.minByOrNull { (sIdx, fret) ->
                        val fretPenalty = when {
                            fret in 0..5 -> fret * 2 // Ideal open/first position
                            fret in 6..9 -> 12 + (fret - 5) * 3 // Comfortable second/third position
                            fret in 10..14 -> 30 + (fret - 9) * 5
                            else -> 70 + fret * 10 // Avoid extreme upper neck unless no alternative
                        }
                        // Penalize playing high notes on low thick wound strings (strings 4, 5)
                        val stringRegisterPenalty = if (sIdx >= 4 && fret > 7) 40 else 0
                        fretPenalty + stringRegisterPenalty
                    }
                    if (chosen != null) {
                        set.add(chosen)
                    }
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
                val lengthFraction = (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
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
                val lengthFraction = (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
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
                        activeFretPoints.contains(s to 0)
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
                    val isCurrentlyPlaying = matchesCoord || (if (activeChordFrets != null) isChordFret && isExactMidiPlaying else isFretPointActive)

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
// HARMONIUM, VIOLIN, FLUTE, TABLA, & SITAR UNIFIED INSTRUMENT VISUALIZERS
// ============================================================================

/**
 * UNIFIED HARMONIUM VISUALIZER
 * Features realistic 7-fold wooden/leather accordion bellows with authentic brass stops,
 * scale stops, teakwood side handles, and keyboard keybed.
 */
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
    onKeyTapped: ((midiNote: Int) -> Unit)? = null,
    onKeyReleased: ((midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF381F14), Color(0xFF26150C), Color(0xFF190E08))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.5.dp, Color(0xFF5D361F), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        // TOP BELLOWS & BRASS STOPS PANEL (Clean decorative bellows fold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1B0D07),
                            Color(0xFF3E1E11),
                            Color(0xFF2C150C),
                            Color(0xFF3E1E11),
                            Color(0xFF1B0D07)
                        )
                    )
                )
                .border(1.dp, Color(0xFF8D5B34), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Brass decorative stops with authentic pull knobs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(9) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300), Color(0xFF8D6E63))
                                )
                            )
                            .border(0.8.dp, Color(0xFFB58500), CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        // HARMONIUM KEYBED (REEDS)
        UnifiedPianoKeyboard(
            activeMidiNotes = activeMidiNotes,
            accentColor = accentColor,
            rootNote = rootNote,
            activeCoordinate = activeMidiNote,
            showNoteNames = showNoteNames,
            isPlaying = activeMidiNotes.values.any { it } || activeMidiNote != null,
            startNote = startNote,
            endNote = endNote,
            fillWidth = true,
            leftHandColor = leftHandColor,
            rightHandColor = rightHandColor,
            isLeftHandHidden = isLeftHandHidden,
            isRightHandHidden = isRightHandHidden,
            onKeyTapped = { onKeyTapped?.invoke(it) },
            onKeyReleased = { onKeyReleased?.invoke(it) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

/**
 * UNIFIED VIOLIN FINGERBOARD VISUALIZER
 * Authentic high-detail orchestral & Carnatic violin neck visualizer.
 * Features curly maple neck border, authentic ebony nut, realistic wound strings,
 * bridge curvature markers, intonation tape guides, and active note glow.
 */
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
    val filteredActiveNotes = remember(activeNotes, isLeftHandHidden, isRightHandHidden) {
        activeNotes.filter { midi ->
            val isTreble = midi >= 65
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2E1708), Color(0xFF1A0E06), Color(0xFF140B05), Color(0xFF2E1708))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.5.dp, Color(0xFF6A3B18), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            openStringsMidi.forEachIndexed { sIdx, openMidi ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // String Label & Peg with pearloid inlay (clear, unclipt)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF23140C),
                        border = BorderStroke(1.2.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringLabels.getOrElse(sIdx) { "S" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // String wire with intonation position markers and glowing active notes
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // String wire
                        val wireHeight = if (sIdx == 3) 3.5.dp else if (sIdx == 2) 2.8.dp else if (sIdx == 1) 2.2.dp else 1.5.dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(wireHeight)
                                .background(
                                    when (sIdx % 4) {
                                        0 -> Color(0xFFE0E0E0) // Plain Steel E
                                        1 -> Color(0xFFCFD8DC) // Aluminum A
                                        2 -> Color(0xFFB0BEC5) // Silver D
                                        else -> Color(0xFFFFD54F).copy(alpha = 0.85f) // Gold Gut G
                                    }
                                )
                        )

                        // Chromatic note positions along the fingerboard
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (pos in 0..totalPositions) {
                                val currentMidi = openMidi + pos
                                val isActive = filteredActiveNotes.contains(currentMidi) || activeMidiNote == currentMidi
                                val isRoot = (currentMidi % 12) == (rootNote % 12)

                                val dotSize by animateFloatAsState(
                                    targetValue = if (isActive) 22f else 10f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(dotSize.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) (if (isRoot) Color(0xFFFF5252) else accentColor)
                                            else if (pos in listOf(2, 4, 5, 7, 9, 12)) Color.White.copy(0.35f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isActive) 1.5.dp else 0.5.dp,
                                            color = if (isActive) Color.White else if (pos in listOf(2, 4, 5, 7, 9, 12)) Color.White.copy(0.5f) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .pointerInput(sIdx, currentMidi) {
                                            detectTapGestures(
                                                onPress = {
                                                    onPositionTapped?.invoke(sIdx, currentMidi)
                                                    tryAwaitRelease()
                                                    onPositionReleased?.invoke(sIdx, currentMidi)
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isActive) {
                                        // Inner glowing core
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                    if (isActive && showNoteNames) {
                                        Text(
                                            text = NOTE_NAMES[currentMidi % 12],
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
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
 * Realistic bamboo flute (Bansuri) and concert silver flute with authentic tone holes,
 * embouchure lip plate with airflow indicator, and threaded bindings (Dhaaga).
 */
@Composable
fun UnifiedFluteVisualizer(
    activeNotes: List<Int> = emptyList(),
    activeMidiNote: Int? = null,
    rootNote: Int = 0,
    accentColor: Color = Color(0xFF00E5FF),
    showNoteNames: Boolean = true,
    holeMidiPitches: List<Int> = listOf(60, 62, 64, 65, 67, 69, 71, 72), // C4, D4, E4, F4, G4, A4, B4, C5
    holeNames: List<String> = listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni", "Ṡa"),
    onToneHoleTapped: ((holeIndex: Int, midiNote: Int) -> Unit)? = null,
    onToneHoleReleased: ((holeIndex: Int, midiNote: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E1C0C), Color(0xFF1D1107), Color(0xFF100904))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.5.dp, Color(0xFF6B4226), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // FLUTE BODY (Natural bamboo tube with traditional red/gold bindings & tone holes)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFD4A373), Color(0xFFFAEDCD), Color(0xFFCCD5AE), Color(0xFFB58463))
                    )
                )
                .border(1.dp, Color(0xFF8B5A2B), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Embouchure hole (Mouthpiece) with cork plug
                Box(
                    modifier = Modifier
                        .size(22.dp, 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B1B1B))
                        .border(1.5.dp, Color(0xFF3E2723), RoundedCornerShape(8.dp))
                )

                // Threaded Decorative Binding Ring (Dhaaga)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(34.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFD32F2F), Color(0xFFFFD54F), Color(0xFFD32F2F))
                            )
                        )
                )

                // Tone Holes with glowing active states
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    holeMidiPitches.forEachIndexed { index, midi ->
                        val isCovered = activeNotes.contains(midi) || activeMidiNote == midi
                        val isRoot = (midi % 12) == (rootNote % 12)

                        val holeSize by animateFloatAsState(
                            targetValue = if (isCovered) 24f else 18f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(holeSize.dp)
                                    .clip(CircleShape)
                                    .background(if (isCovered) (if (isRoot) Color(0xFFFF5252) else accentColor) else Color(0xFF2B1810))
                                    .border(
                                        width = if (isCovered) 2.dp else 1.2.dp,
                                        color = if (isCovered) Color.White else Color(0xFF5D4037),
                                        shape = CircleShape
                                    )
                                    .pointerInput(index, midi) {
                                        detectTapGestures(
                                            onPress = {
                                                onToneHoleTapped?.invoke(index, midi)
                                                tryAwaitRelease()
                                                onToneHoleReleased?.invoke(index, midi)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCovered) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                            Text(
                                text = holeNames.getOrElse(index) { NOTE_NAMES[midi % 12] },
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCovered) Color(0xFFB71C1C) else Color(0xFF3E2723)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * UNIFIED TABLA VISUALIZER
 * Interactive two-drum visualizer:
 * - Dayan (Right Wooden Drum): Tuned to tonic Sa; features central black Syahi, Sur ring, Maidan, Kinar, and braided Gajra rim.
 * - Bayan (Left Metal/Clay Bass Drum): Features off-center Syahi, deep resonant bass strike, and sliding modulation.
 */
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
    val isBayanActive = activeBol != null && activeBol in listOf("GHE", "GE", "KA", "KAT", "DHA", "DHIN")
    val isDayanActive = activeBol != null && activeBol in listOf("DHA", "DHIN", "NA", "TA", "TIN", "TI", "TE", "RE")

    Row(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A1B12), Color(0xFF190F09), Color(0xFF0F0905))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.5.dp, Color(0xFF5A3821), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BAYAN (Left Bass Drum, Dagga)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showLabels) {
                Text("BAYAN ($bayanBassName)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
            }
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isBayanActive) listOf(Color(0xFFFFD54F), accentColor, Color(0xFF37474F))
                            else listOf(Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFF37474F), Color(0xFF263238))
                        )
                    )
                    .border(if (isBayanActive) 3.dp else 2.5.dp, if (isBayanActive) Color.White else Color(0xFF8D6E63), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onBolTapped?.invoke("GHE")
                                tryAwaitRelease()
                                onBolReleased?.invoke("GHE")
                            }
                        )
                    },
                contentAlignment = Alignment.TopStart
            ) {
                // Braided Gajra Outer Rim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD7CCC8))
                        .border(1.dp, Color(0xFF8D6E63), CircleShape)
                ) {
                    // Off-center Syahi (Black tuning paste)
                    Box(
                        modifier = Modifier
                            .padding(start = 14.dp, top = 14.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isBayanActive) accentColor else Color(0xFF1A1A1A))
                            .border(1.dp, Color(0xFF424242), CircleShape)
                    )
                }
            }
            if (showLabels) {
                Text("Bols: Ghe, Ge, Ka, Kat", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        // DAYAN (Right Treble Drum, Tabla)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showLabels) {
                Text("DAYAN ($dayanTonicName)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isDayanActive) listOf(Color(0xFFFFD54F), accentColor, Color(0xFF3E1E11))
                            else listOf(Color(0xFF8D5B34), Color(0xFF5D361F), Color(0xFF3E1E11))
                        )
                    )
                    .border(if (isDayanActive) 3.dp else 2.5.dp, if (isDayanActive) Color.White else Color(0xFF8D6E63), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onBolTapped?.invoke("DHA")
                                tryAwaitRelease()
                                onBolReleased?.invoke("DHA")
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer Gajra Rim & Chanti
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5DC))
                        .border(1.dp, Color(0xFF8D6E63), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Sur / Maidan Ring
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE0D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Centered Syahi
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (isDayanActive) accentColor else Color(0xFF111111))
                                .border(1.dp, Color(0xFF424242), CircleShape)
                        )
                    }
                }
            }
            if (showLabels) {
                Text("Bols: Dha, Dhin, Na, Ta, Tin", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

/**
 * UNIFIED SITAR VISUALIZER
 * Authentic high-detail classical Indian Sitar visualizer.
 * Features:
 * - Seasoned tun/teakwood neck with natural hollow resonating chamber
 * - Arched curved brass frets (Parda) tied with silk thread
 * - Baj Tar (main melody string, #1), Jod, Kharaj, and Pancham strings
 * - Elevated Chikari high drone posts and pegs
 * - Bone bridge (Ghoraj / Javari) for the distinctive buzzing resonance
 * - Interactive movable fret touch tracking with Meend pull simulation
 */
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
    val filteredActiveNotes = remember(activeNotes, isLeftHandHidden, isRightHandHidden) {
        activeNotes.filter { midi ->
            val isTreble = midi >= 60
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C1608), Color(0xFF1E0E05), Color(0xFF130903))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.5.dp, Color(0xFF6B3A19), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Sitar Fretboard Neck with Arched Brass Frets and Strings
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3E1F0E), Color(0xFF271308), Color(0xFF3E1F0E))
                    )
                )
        ) {
            // Draw 16 Curved Brass Frets
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (fret in 1..numFrets) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFB58500), Color(0xFFFFD54F))
                                )
                            )
                            .border(0.5.dp, Color(0xFF6D4C41))
                    )
                }
            }

            // Overlay Strings & Interactive Notes
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                sitarStrings.forEachIndexed { sIdx, (name, baseMidi, gauge) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // String Label & Peg with clear padding and no clipping
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF150A04),
                            border = BorderStroke(1.2.dp, Color(0xFFFFD54F)),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (sIdx == 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.9f),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(Modifier.width(6.dp))

                        // String wire and interactive note hits
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(gauge.dp)
                                    .background(
                                        if (sIdx == 0) Color(0xFFFFF9C4)
                                        else if (sIdx in 1..3) Color(0xFFFFD54F).copy(alpha = 0.85f)
                                        else Color(0xFFE0E0E0)
                                    )
                            )

                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (fret in 1..numFrets) {
                                    val midi = baseMidi + fret
                                    val isActive = filteredActiveNotes.contains(midi) || activeMidiNote == midi
                                    val isRoot = (midi % 12) == (rootNote % 12)

                                    val beadSize by animateFloatAsState(
                                        targetValue = if (isActive) 20f else 9f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(beadSize.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isActive) (if (isRoot) Color(0xFFFF5252) else accentColor)
                                                else if (fret in listOf(1, 3, 5, 7, 8, 10, 12, 13, 15)) Color.White.copy(alpha = 0.25f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = if (isActive) 1.5.dp else 0.5.dp,
                                                color = if (isActive) Color.White else if (fret in listOf(1, 3, 5, 7, 8, 10, 12, 13, 15)) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .pointerInput(fret, midi) {
                                                detectTapGestures(
                                                    onPress = {
                                                        onFretTapped?.invoke(fret, midi)
                                                        tryAwaitRelease()
                                                        onFretReleased?.invoke(fret, midi)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                        if (isActive && showNoteNames) {
                                            Text(
                                                text = NOTE_NAMES[midi % 12],
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Black,
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
}
