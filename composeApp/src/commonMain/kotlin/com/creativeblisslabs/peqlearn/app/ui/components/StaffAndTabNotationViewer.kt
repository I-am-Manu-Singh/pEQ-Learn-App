package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

private fun DrawScope.drawLedgerLines(
    x: Float,
    y: Float,
    topY: Float,
    bottomY: Float,
    spacing: Float,
    color: Color,
    handAccent: Color? = null
) {
    if (spacing <= 0f) return
    val lineBoundExt = 12f
    val halfSpacing = spacing / 2f
    val lineColor = if (handAccent != null) {
        Color(
            red = (color.red * 0.7f + handAccent.red * 0.3f),
            green = (color.green * 0.7f + handAccent.green * 0.3f),
            blue = (color.blue * 0.7f + handAccent.blue * 0.3f),
            alpha = 0.90f
        )
    } else {
        color.copy(alpha = 0.85f)
    }

    if (y < topY - halfSpacing) {
        var curL = topY - spacing
        var count = 0
        while (curL >= y - (halfSpacing * 0.8f) && count < 7) {
            drawLine(lineColor, Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = 1.3f)
            curL -= spacing
            count++
        }
    } else if (y > bottomY + halfSpacing) {
        var curL = bottomY + spacing
        var count = 0
        while (curL <= y + (halfSpacing * 0.8f) && count < 7) {
            drawLine(lineColor, Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = 1.3f)
            curL += spacing
            count++
        }
    }
}

private fun snap(value: Float): Float = round(value)

private fun isNoteTemporallyActive(
    note: ScoreNote,
    isMeasureActive: Boolean,
    playheadOffset: Float?,
    beatsPerMeasure: Float,
    playingNote: Int?,
    activeMidiNotes: Set<Int>
): Boolean {
    if (!isMeasureActive) return false
    val pitchMatches = (playingNote != null && note.midiNote == playingNote) || activeMidiNotes.contains(note.midiNote)
    if (!pitchMatches) return false

    if (playheadOffset != null) {
        val currentBeat = playheadOffset * beatsPerMeasure
        val noteStart = note.position
        val noteEnd = note.position + note.duration.coerceAtLeast(0.125f)
        val onsetTolerance = 0.08f
        val releaseTolerance = 0.04f
        return currentBeat >= (noteStart - onsetTolerance) && currentBeat <= (noteEnd + releaseTolerance)
    }
    return true
}

private fun DrawScope.drawEngravedCurve(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    direction: Float, // -1f = curve UP (above), +1f = curve DOWN (below)
    color: Color,
    isDashed: Boolean = false
) {
    val spanX = (endX - startX).coerceAtLeast(10f)
    val dy = endY - startY
    val archHeight = (6f + (spanX * 0.11f)).coerceIn(7f, 22f) * direction
    val thickness = 2.0f * direction

    val cp1X = startX + spanX * 0.25f
    val cp2X = startX + spanX * 0.75f
    val cp1Y = startY + dy * 0.25f + archHeight
    val cp2Y = startY + dy * 0.75f + archHeight

    if (isDashed) {
        val path = Path().apply {
            moveTo(startX, startY)
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, endX, endY)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f))
        )
    } else {
        val path = Path().apply {
            moveTo(startX, startY)
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, endX, endY)
            cubicTo(cp2X, cp2Y + thickness, cp1X, cp1Y + thickness, startX, startY)
            close()
        }
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawHorizontalDashedBracket(
    label: String,
    startX: Float,
    endX: Float,
    y: Float,
    hookDir: Float, // +1f = hook down, -1f = hook up
    color: Color,
    textMeasurer: TextMeasurer
) {
    val textLayout = textMeasurer.measure(
        text = label,
        style = TextStyle(color = color, fontSize = 7.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
    )
    drawText(textLayout, topLeft = Offset(startX, y - textLayout.size.height / 2f))
    val lineStartX = startX + textLayout.size.width + 4f
    if (endX > lineStartX + 4f) {
        drawLine(
            color = color,
            start = Offset(lineStartX, y),
            end = Offset(endX, y),
            strokeWidth = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
        )
        drawLine(
            color = color,
            start = Offset(endX, y),
            end = Offset(endX, y + (hookDir * 5f)),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawFermataSymbol(centerX: Float, bottomY: Float, color: Color) {
    val archW = 14f
    val archH = 7f
    val path = Path().apply {
        moveTo(centerX - archW / 2f, bottomY)
        cubicTo(
            centerX - archW / 2f, bottomY - archH,
            centerX + archW / 2f, bottomY - archH,
            centerX + archW / 2f, bottomY
        )
        cubicTo(
            centerX + archW / 2f, bottomY - archH + 2.2f,
            centerX - archW / 2f, bottomY - archH + 2.2f,
            centerX - archW / 2f, bottomY
        )
        close()
    }
    drawPath(path, color)
    drawCircle(color, radius = 1.9f, center = Offset(centerX, bottomY - 2.2f))
}

private fun DrawScope.drawArpeggioRoll(
    x: Float,
    topY: Float,
    bottomY: Float,
    direction: ArpeggioDirection,
    color: Color
) {
    val startY = (topY - 4f)
    val endY = (bottomY + 4f)
    val step = 5f
    val path = Path().apply {
        var curY = startY
        moveTo(x, curY)
        var toggle = 1f
        while (curY < endY) {
            val nextY = (curY + step).coerceAtMost(endY)
            quadraticBezierTo(x + (toggle * 3f), (curY + nextY) / 2f, x, nextY)
            curY = nextY
            toggle = -toggle
        }
    }
    drawPath(path, color = color, style = Stroke(width = 1.3f))
    if (direction == ArpeggioDirection.UP) {
        val arrow = Path().apply {
            moveTo(x, startY - 3f)
            lineTo(x - 3f, startY + 2f)
            lineTo(x + 3f, startY + 2f)
            close()
        }
        drawPath(arrow, color)
    } else if (direction == ArpeggioDirection.DOWN) {
        val arrow = Path().apply {
            moveTo(x, endY + 3f)
            lineTo(x - 3f, endY - 2f)
            lineTo(x + 3f, endY - 2f)
            close()
        }
        drawPath(arrow, color)
    }
}

private fun DrawScope.drawGrandStaffBrace(
    x: Float,
    topY: Float,
    bottomY: Float,
    color: Color
) {
    val midY = (topY + bottomY) / 2f
    val h = (bottomY - topY).coerceAtLeast(20f)
    val w = 7f
    val path = Path().apply {
        moveTo(x, topY)
        cubicTo(x - w, topY + h * 0.12f, x + 1f, midY - h * 0.08f, x - w - 2f, midY)
        cubicTo(x + 1f, midY + h * 0.08f, x - w, bottomY - h * 0.12f, x, bottomY)
        cubicTo(x - w + 2.2f, bottomY - h * 0.14f, x + 2.5f, midY + h * 0.06f, x - w + 0.5f, midY)
        cubicTo(x + 2.5f, midY - h * 0.06f, x - w + 2.2f, topY + h * 0.14f, x, topY)
        close()
    }
    drawPath(path, color)
}

internal data class PrecomputedRenderNote(
    val note: ScoreNote,
    val midiNote: Int,
    val isTrebleClef: Boolean,
    val anchorX: Float,
    val nx: Float,
    val ny: Float,
    val topY: Float,
    val bottomY: Float,
    val beatIndex: Int,
    val diatonicStep: Int,
    val isDisplacedSecond: Boolean = false,
    val accidentalColumn: Int = 0,
    val resolvedAccidental: Accidental? = null,
    val polyphonicStemDir: Float? = null
)

internal data class PrecomputedBeamGroup(
    val stemDir: Float,
    val firstX: Float,
    val lastX: Float,
    val startY: Float,
    val endY: Float,
    val noteStemYMap: Map<ScoreNote, Float>,
    val secondarySegments: List<Pair<Offset, Offset>> = emptyList(),
    val tertiarySegments: List<Pair<Offset, Offset>> = emptyList(),
    val beamlets: List<Pair<Offset, Offset>> = emptyList(),
    val notes: List<ScoreNote> = emptyList()
)

internal data class PrecomputedStaffLayout(
    val visibleNotes: List<PrecomputedRenderNote>,
    val computedXByPosition: Map<Float, Float>,
    val noteBeamMap: Map<ScoreNote, PrecomputedBeamGroup>,
    val beamGroups: List<PrecomputedBeamGroup>,
    val trebleRests: List<RestSymbol>,
    val bassRests: List<RestSymbol>,
    val hasPolyphonicVoicesOnTreble: Boolean,
    val hasPolyphonicVoicesOnBass: Boolean,
    val usableMeasureWidth: Float,
    val notesStartX: Float,
    val beatsPerMeasure: Float
)

internal data class PrecomputedTabNote(
    val note: ScoreNote,
    val midiNote: Int,
    val stringIdx: Int,
    val fretValue: Int,
    val nx: Float,
    val ny: Float,
    val fretText: String,
    val pillWidth: Float,
    val pillHeight: Float,
    val circleRadius: Float
)

internal data class PrecomputedTabLayout(
    val notes: List<PrecomputedTabNote>,
    val computedXByPosition: Map<Float, Float>,
    val rests: List<RestSymbol>,
    val systemContentStartX: Float,
    val usableMeasureWidth: Float,
    val beatsPerMeasure: Float
)

internal class MeasureLayoutCache {
    private val staffCache = mutableMapOf<String, PrecomputedStaffLayout>()
    private val tabCache = mutableMapOf<String, PrecomputedTabLayout>()

    fun getStaffLayout(
        measure: Measure,
        drawClefHeader: Boolean,
        drawKeyHeader: Boolean,
        drawTimeHeader: Boolean,
        startX: Float,
        endX: Float,
        trebleBaseline: Float,
        bassBaseline: Float,
        lineSpacing: Float,
        isLeftHandHidden: Boolean,
        isRightHandHidden: Boolean,
        enabledTrackIndices: Map<Int, Boolean> = emptyMap()
    ): PrecomputedStaffLayout {
        val tracksKey = enabledTrackIndices.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
        val key = "${measure.measureIndex}_${drawClefHeader}_${drawKeyHeader}_${drawTimeHeader}_${startX.toInt()}_${endX.toInt()}_${trebleBaseline.toInt()}_${bassBaseline.toInt()}_${lineSpacing.toInt()}_${isLeftHandHidden}_${isRightHandHidden}_${measure.notes.size}_$tracksKey"
        return staffCache.getOrPut(key) {
            computeStaffLayout(
                measure, drawClefHeader, drawKeyHeader, drawTimeHeader,
                startX, endX, trebleBaseline, bassBaseline, lineSpacing,
                isLeftHandHidden, isRightHandHidden, enabledTrackIndices
            )
        }
    }

    fun getTabLayout(
        measure: Measure,
        drawClefHeader: Boolean,
        drawTimeHeader: Boolean,
        startX: Float,
        endX: Float,
        tBase: Float,
        lineSpacing: Float,
        tuning: List<Int>,
        capo: Int,
        isLeftHandHidden: Boolean = false,
        isRightHandHidden: Boolean = false,
        enabledTrackIndices: Map<Int, Boolean> = emptyMap()
    ): PrecomputedTabLayout {
        val tracksKey = enabledTrackIndices.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
        val key = "${measure.measureIndex}_${drawClefHeader}_${drawTimeHeader}_${startX.toInt()}_${endX.toInt()}_${tBase.toInt()}_${lineSpacing.toInt()}_${isLeftHandHidden}_${isRightHandHidden}_${measure.notes.size}_${capo}_$tracksKey"
        return tabCache.getOrPut(key) {
            computeTabLayout(
                measure, drawClefHeader, drawTimeHeader, startX, endX, tBase, lineSpacing,
                tuning, capo, isLeftHandHidden, isRightHandHidden, enabledTrackIndices
            )
        }
    }

    fun clear() {
        staffCache.clear()
        tabCache.clear()
    }
}

private fun defaultStepAlterationInKey(diatonicStepMod7: Int, keySig: KeySignature): Int {
    // diatonicStepMod7: 0=C, 1=D, 2=E, 3=F, 4=G, 5=A, 6=B
    val sharpOrder = intArrayOf(3, 0, 4, 1, 5, 2, 6) // F, C, G, D, A, E, B
    val flatOrder = intArrayOf(6, 2, 5, 1, 4, 0, 3)  // B, E, A, D, G, C, F
    val count = keySig.accidentalsCount.coerceIn(0, 7)
    if (count == 0) return 0
    return if (keySig.isSharp) {
        if ((0 until count).any { sharpOrder[it] == diatonicStepMod7 }) 1 else 0
    } else {
        if ((0 until count).any { flatOrder[it] == diatonicStepMod7 }) -1 else 0
    }
}

private fun computeStaffLayout(
    measure: Measure,
    drawClefHeader: Boolean,
    drawKeyHeader: Boolean,
    drawTimeHeader: Boolean,
    startX: Float,
    endX: Float,
    trebleBaseline: Float,
    bassBaseline: Float,
    lineSpacing: Float,
    isLeftHandHidden: Boolean,
    isRightHandHidden: Boolean,
    enabledTrackIndices: Map<Int, Boolean> = emptyMap()
): PrecomputedStaffLayout {
    val ksCount = measure.keySignature.accidentalsCount
    val clefW = if (drawClefHeader) 42f else 0f
    val ksW = if (drawKeyHeader && ksCount > 0) minOf(ksCount, 7) * 8f + 8f else 0f
    val tsW = if (drawTimeHeader) 32f else 0f
    val symbolAreaWidth = clefW + ksW + tsW + (if (drawClefHeader || drawKeyHeader || drawTimeHeader) 8f else 0f)
    val notesStartX = startX + symbolAreaWidth
    val usableMeasureWidth = (endX - notesStartX - 10f).coerceAtLeast(80f)

    val nominalBeats = (measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.toFloat().coerceAtLeast(1f))).coerceAtLeast(1f)
    val maxNoteReach = measure.notes.maxOfOrNull { it.position + it.duration } ?: nominalBeats
    val beatsPerMeasure = if (measure.isPickup && maxNoteReach < nominalBeats) maxNoteReach.coerceAtLeast(0.5f) else nominalBeats
    val halfLineSpacing = lineSpacing / 2f

    val hasDifferentiatedVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }
    val rawVisibleNotes = measure.notes.filter { !it.isRest }
        .filter { note -> enabledTrackIndices[note.voice] ?: true }
        .distinctBy { "${it.position}_${it.midiNote}_${it.voice}_${it.grace}" }
        .mapNotNull { note ->
            val midiNote = note.midiNote
            val isTrebleClef = if (hasDifferentiatedVoices && !note.crossStaff) {
                note.voice == 0
            } else if (note.crossStaff) {
                note.voice != 0
            } else {
                midiNote >= 60
            }
            if (isTrebleClef && isRightHandHidden) return@mapNotNull null
            if (!isTrebleClef && isLeftHandHidden) return@mapNotNull null
            Triple(note, midiNote, isTrebleClef)
        }.sortedWith(compareBy({ it.first.position }, { !it.first.grace }, { it.second }))

    val nonGracePositions = rawVisibleNotes.filter { !it.first.grace }.map { it.first.position }.distinct().sorted()
    val minNoteGap = 26f
    val computedXByPosition = mutableMapOf<Float, Float>()

    if (nonGracePositions.isNotEmpty()) {
        val minDur = rawVisibleNotes.filter { !it.first.grace }.minOfOrNull { it.first.duration.coerceAtLeast(0.125f) } ?: 1.0f
        val posDurations = nonGracePositions.mapIndexed { idx, pos ->
            val nextPos = if (idx < nonGracePositions.lastIndex) nonGracePositions[idx + 1] else beatsPerMeasure
            val delta = (nextPos - pos).coerceAtLeast(minDur)
            (delta / minDur).toDouble().pow(0.50).toFloat()
        }
        val sumWeighted = posDurations.sum().coerceAtLeast(1f)
        val noteContentMaxX = endX - 18f
        val noteContentStartX = notesStartX + 14f
        val maxAvailableSpan = (noteContentMaxX - noteContentStartX).coerceAtLeast(40f)

        val naturalSpacingNeeded = (nonGracePositions.size - 1) * minNoteGap
        val effectiveGap = if (naturalSpacingNeeded > maxAvailableSpan && nonGracePositions.size > 1) {
            maxAvailableSpan / (nonGracePositions.size - 1)
        } else {
            minNoteGap
        }

        var runningX = noteContentStartX
        var runningWeight = 0f
        nonGracePositions.forEachIndexed { idx, pos ->
            val fraction = (runningWeight / sumWeighted).coerceIn(0f, 1f)
            val idealX = noteContentStartX + (maxAvailableSpan * fraction)
            val targetX = if (idx == 0) idealX else maxOf(idealX, runningX + effectiveGap)
            val clampedX = targetX.coerceIn(noteContentStartX, noteContentMaxX)
            computedXByPosition[pos] = snap(clampedX)
            runningX = clampedX
            runningWeight += posDurations[idx]
        }
    }

    val noteScale = lineSpacing / 12f
    val stdNoteWidth = 15f * noteScale

    // Detect if multiple voices coexist on the same staff (Polyphonic separation)
    val trebleVoices = rawVisibleNotes.filter { it.third }.map { it.first.voice }.distinct()
    val bassVoices = rawVisibleNotes.filter { !it.third }.map { it.first.voice }.distinct()
    val hasPolyphonicVoicesOnTreble = trebleVoices.size > 1
    val hasPolyphonicVoicesOnBass = bassVoices.size > 1

    // Intra-measure accidental memory map: diatonicStep -> current alteration (-2..+2)
    val activeStepAlteration = mutableMapOf<Int, Int>()
    val naturalCScaleSemitones = intArrayOf(0, 2, 4, 5, 7, 9, 11)

    val initialNotes = rawVisibleNotes.map { (note, midiNote, isTrebleClef) ->
        val baseNx = computedXByPosition[note.position]
            ?: snap((notesStartX + 14f + ((usableMeasureWidth - 28f) * (note.position / beatsPerMeasure))).coerceIn(notesStartX + 12f, endX - 16f))
        val anchorX = if (note.grace) (baseNx - 12f).coerceAtLeast(notesStartX + 6f) else baseNx

        // Apply octaveShift to visual staff display position so ledger lines stay readable
        // val shiftedMidiForDisplay = (midiNote + note.octaveShift.semitoneShift).coerceIn(12, 115)
        
       // Apply octaveShift (-8, +8, -15, +15) to visual staff display position so ledger lines stay readable
        val shiftedMidiForDisplay = when (note.octaveShift) {
            OctaveShift.OTTAVA_ALTA_8VA -> midiNote - 12
            OctaveShift.QUINDICESIMA_ALTA_15MA -> midiNote - 24
            OctaveShift.OTTAVA_BASSA_8VB -> midiNote + 12
            OctaveShift.QUINDICESIMA_BASSA_15MB -> midiNote + 24
            OctaveShift.NONE -> midiNote
        }.coerceIn(12, 115)

        val diatonicStep = ChordVoicingEngine.getDiatonicStep(shiftedMidiForDisplay)
        val ny = if (isTrebleClef) {
            trebleBaseline + (38.0f - diatonicStep) * halfLineSpacing
        } else {
            bassBaseline + (26.0f - diatonicStep) * halfLineSpacing
        }
        val topY = if (isTrebleClef) trebleBaseline else bassBaseline
        val bottomY = topY + (4 * lineSpacing)
        val beatIdx = floor(note.position).toInt()

        // Resolve intra-measure accidental state
        val stepMod7 = ((diatonicStep % 7) + 7) % 7
        val defaultAlt = defaultStepAlterationInKey(stepMod7, measure.keySignature)
        val currentExpectedAlt = activeStepAlteration.getOrElse(diatonicStep) { defaultAlt }

        val naturalPitchClass = naturalCScaleSemitones[stepMod7]
        val actualPitchClass = ((midiNote % 12) + 12) % 12
        var semitoneDiff = actualPitchClass - naturalPitchClass
        if (semitoneDiff > 6) semitoneDiff -= 12
        if (semitoneDiff < -6) semitoneDiff += 12

        val actualAlt = when (note.accidental) {
            Accidental.DOUBLE_SHARP -> 2
            Accidental.DOUBLE_FLAT -> -2
            Accidental.SHARP -> 1
            Accidental.FLAT -> -1
            else -> semitoneDiff.coerceIn(-2, 2)
        }

        val resolvedAccidental: Accidental? = if (note.tieEnd && !note.courtesyAccidental) {
            null // Tied continuation notes never repeat the accidental
        } else if (actualAlt != currentExpectedAlt || note.courtesyAccidental) {
            activeStepAlteration[diatonicStep] = actualAlt
            when (actualAlt) {
                2 -> Accidental.DOUBLE_SHARP
                1 -> Accidental.SHARP
                -1 -> Accidental.FLAT
                -2 -> Accidental.DOUBLE_FLAT
                else -> Accidental.NATURAL
            }
        } else {
            null
        }

        val polyDir: Float? = if (isTrebleClef && hasPolyphonicVoicesOnTreble) {
            if (note.voice == trebleVoices.minOrNull()) -1f else 1f
        } else if (!isTrebleClef && hasPolyphonicVoicesOnBass) {
            if (note.voice == bassVoices.minOrNull()) -1f else 1f
        } else null

        PrecomputedRenderNote(
            note = note,
            midiNote = midiNote,
            isTrebleClef = isTrebleClef,
            anchorX = anchorX,
            nx = anchorX,
            ny = ny,
            topY = topY,
            bottomY = bottomY,
            beatIndex = beatIdx,
            diatonicStep = diatonicStep,
            resolvedAccidental = resolvedAccidental,
            polyphonicStemDir = polyDir
        )
    }

    // Resolve chord 2nd-interval horizontal offsets and accidental columns
    val visibleNotes = mutableListOf<PrecomputedRenderNote>()
    val chordBuckets = initialNotes.groupBy { "${it.note.position}_${it.isTrebleClef}_${it.note.voice}_${it.note.grace}" }
    for ((_, bucket) in chordBuckets) {
        if (bucket.size == 1) {
            visibleNotes.add(bucket.first())
        } else {
            val sortedByStep = bucket.sortedBy { it.diatonicStep }
            val avgMidi = sortedByStep.map { it.midiNote }.average()
            val firstItem = sortedByStep.first()
            val stemDir = firstItem.polyphonicStemDir
                ?: if (firstItem.isTrebleClef) (if (avgMidi >= 71) 1f else -1f) else (if (avgMidi >= 50) 1f else -1f)

            var lastDisplaced = false
            var prevStep = Int.MIN_VALUE
            var accColCounter = 0

            sortedByStep.forEachIndexed { idx, item ->
                val isSecond = (idx > 0 && abs(item.diatonicStep - prevStep) <= 1 && !lastDisplaced)
                lastDisplaced = isSecond
                prevStep = item.diatonicStep
                val offsetDir = if (stemDir < 0) 1f else -1f
                val adjustedX = if (isSecond) item.anchorX + (offsetDir * (stdNoteWidth * 0.78f)) else item.anchorX
                val col = if (item.resolvedAccidental != null) (accColCounter++ % 3) else 0
                visibleNotes.add(
                    item.copy(
                        nx = adjustedX,
                        isDisplacedSecond = isSecond,
                        accidentalColumn = col
                    )
                )
            }
        }
    }
    visibleNotes.sortWith(compareBy({ it.note.position }, { !it.note.grace }, { it.midiNote }))

    val noteBeamMap = mutableMapOf<ScoreNote, PrecomputedBeamGroup>()
    val precomputedBeams = mutableListOf<PrecomputedBeamGroup>()

    val isCompoundMeter = measure.timeSignatureDenominator == 8 && measure.timeSignatureNumerator % 3 == 0 && measure.timeSignatureNumerator >= 6
    val gridResolution = if (isCompoundMeter) 1.5f else 1.0f

    val groupedByClef = visibleNotes.filter { !it.note.grace }.groupBy { it.isTrebleClef }
    for ((isTreble, clefNotes) in groupedByClef) {
        val groupedByVoice = clefNotes.groupBy { it.note.voice }

        for ((_, voiceNotes) in groupedByVoice) {
            val groupedByBeat = voiceNotes.groupBy {
                floor((it.note.position + 0.001f) / gridResolution).toInt()
            }

            for ((_, subBeatNotes) in groupedByBeat) {
                val beamEligible = subBeatNotes.filter { it.note.duration <= 0.76f }
                val distinctPos = beamEligible.map { it.note.position }.distinct().sorted()
                if (distinctPos.size < 2) continue

                val posGroups = beamEligible.groupBy { it.note.position }.toSortedMap()

                val contiguousRuns = mutableListOf<List<Float>>()
                var currentRun = mutableListOf<Float>()
                for (pos in distinctPos) {
                    if (currentRun.isEmpty()) {
                        currentRun.add(pos)
                    } else {
                        val prevPos = currentRun.last()
                        val prevMaxDur = posGroups[prevPos]?.maxOfOrNull { it.note.duration } ?: 0.5f
                        if (pos - (prevPos + prevMaxDur) <= 0.08f) {
                            currentRun.add(pos)
                        } else {
                            if (currentRun.size >= 2) contiguousRuns.add(currentRun)
                            currentRun = mutableListOf(pos)
                        }
                    }
                }
                if (currentRun.size >= 2) contiguousRuns.add(currentRun)

                for (runPositions in contiguousRuns) {
                    val cluster = runPositions.flatMap { posGroups[it] ?: emptyList() }
                    if (cluster.size < 2) continue

                    val firstPosNotes = posGroups[runPositions.first()] ?: continue
                    val lastPosNotes = posGroups[runPositions.last()] ?: continue
                    val noteSpan = lastPosNotes.first().anchorX - firstPosNotes.first().anchorX
                    if (noteSpan !in 6f..240f) continue

                    val avgMidi = cluster.map { it.midiNote }.average()
                    val groupStemDir = cluster.first().polyphonicStemDir ?: if (isTreble) {
                        if (avgMidi >= 71) 1f else -1f
                    } else {
                        if (avgMidi >= 50) 1f else -1f
                    }

                    val beamAnchorNotes = runPositions.map { pos ->
                        val notesAtPos = posGroups[pos]!!
                        if (groupStemDir > 0) notesAtPos.maxByOrNull { it.ny }!!
                        else notesAtPos.minByOrNull { it.ny }!!
                    }

                    val firstAnchor = beamAnchorNotes.first()
                    val lastAnchor = beamAnchorNotes.last()
                    val stemOffset = (stdNoteWidth * 0.44f).coerceIn(5.2f, 7.5f)

                    val firstX = if (groupStemDir > 0) firstAnchor.anchorX - stemOffset else firstAnchor.anchorX + stemOffset
                    val lastX = if (groupStemDir > 0) lastAnchor.anchorX - stemOffset else lastAnchor.anchorX + stemOffset
                    val dx = (lastX - firstX).coerceAtLeast(1f)

                    val rawPitchSlope = (lastAnchor.ny - firstAnchor.ny) / dx
                    val isMonotonic = beamAnchorNotes.zipWithNext().all { (a, b) -> b.ny >= a.ny } ||
                        beamAnchorNotes.zipWithNext().all { (a, b) -> b.ny <= a.ny }
                    val clampedSlope = if (isMonotonic) (rawPitchSlope * 0.25f).coerceIn(-0.12f, 0.12f) else 0f

                    val standardStemLength = (lineSpacing * 2.3f).coerceAtLeast(24f)
                    var constrainedStartY = if (groupStemDir > 0) {
                        beamAnchorNotes.maxOf { it.ny + standardStemLength - (clampedSlope * (it.anchorX - firstAnchor.anchorX)) }
                    } else {
                        beamAnchorNotes.minOf { it.ny - standardStemLength - (clampedSlope * (it.anchorX - firstAnchor.anchorX)) }
                    }

                    val trebleMaxBeamY = trebleBaseline + (4.3f * lineSpacing)
                    val bassMinBeamY = bassBaseline - (0.4f * lineSpacing)

                    if (isTreble && groupStemDir > 0) {
                        val maxRawY = maxOf(constrainedStartY, constrainedStartY + (clampedSlope * dx))
                        if (maxRawY > trebleMaxBeamY) constrainedStartY -= (maxRawY - trebleMaxBeamY)
                    } else if (!isTreble && groupStemDir < 0) {
                        val minRawY = minOf(constrainedStartY, constrainedStartY + (clampedSlope * dx))
                        if (minRawY < bassMinBeamY) constrainedStartY += (bassMinBeamY - minRawY)
                    }

                    val minStemClearance = (lineSpacing * 1.6f).coerceAtLeast(18f)
                    for (n in beamAnchorNotes) {
                        val beamYAtN = constrainedStartY + (clampedSlope * (n.anchorX - firstAnchor.anchorX))
                        val stemLen = if (groupStemDir > 0) (beamYAtN - n.ny) else (n.ny - beamYAtN)
                        if (stemLen < minStemClearance) {
                            constrainedStartY += (groupStemDir * (minStemClearance - stemLen))
                        }
                    }

                    val constrainedEndY = constrainedStartY + (clampedSlope * dx)

                    val noteStemYMap = mutableMapOf<ScoreNote, Float>()
                    cluster.forEach { n ->
                        val beamYAtNote = constrainedStartY + (clampedSlope * (n.anchorX - firstAnchor.anchorX))
                        noteStemYMap[n.note] = beamYAtNote
                    }

                    val secondaryGap = -groupStemDir * 5.2f
                    val tertiaryGap = -groupStemDir * 10.4f

                    val secondarySegments = mutableListOf<Pair<Offset, Offset>>()
                    val tertiarySegments = mutableListOf<Pair<Offset, Offset>>()
                    val beamlets = mutableListOf<Pair<Offset, Offset>>()

                    val posMinDur = runPositions.map { pos ->
                        posGroups[pos]!!.minOf { it.note.duration }
                    }

                    var idx = 0
                    while (idx < runPositions.size) {
                        if (posMinDur[idx] <= 0.38f) {
                            var endIdx = idx
                            while (endIdx + 1 < runPositions.size && posMinDur[endIdx + 1] <= 0.38f) {
                                endIdx++
                            }
                            if (endIdx > idx) {
                                val aStart = beamAnchorNotes[idx]
                                val aEnd = beamAnchorNotes[endIdx]
                                val sx1 = if (groupStemDir > 0) aStart.anchorX - stemOffset else aStart.anchorX + stemOffset
                                val sx2 = if (groupStemDir > 0) aEnd.anchorX - stemOffset else aEnd.anchorX + stemOffset
                                val sy1 = constrainedStartY + clampedSlope * (sx1 - firstX) + secondaryGap
                                val sy2 = constrainedStartY + clampedSlope * (sx2 - firstX) + secondaryGap
                                secondarySegments.add(Pair(Offset(sx1, sy1), Offset(sx2, sy2)))
                            } else {
                                val singleAnchor = beamAnchorNotes[idx]
                                val sx = if (groupStemDir > 0) singleAnchor.anchorX - stemOffset else singleAnchor.anchorX + stemOffset
                                val dir = if (idx == 0) 1f else -1f
                                val bx2 = sx + dir * 9f
                                val sy1 = constrainedStartY + clampedSlope * (sx - firstX) + secondaryGap
                                val sy2 = constrainedStartY + clampedSlope * (bx2 - firstX) + secondaryGap
                                beamlets.add(Pair(Offset(sx, sy1), Offset(bx2, sy2)))
                            }
                            idx = endIdx + 1
                        } else {
                            idx++
                        }
                    }

                    var tIdx = 0
                    while (tIdx < runPositions.size) {
                        if (posMinDur[tIdx] <= 0.19f) {
                            var endIdx = tIdx
                            while (endIdx + 1 < runPositions.size && posMinDur[endIdx + 1] <= 0.19f) {
                                endIdx++
                            }
                            if (endIdx > tIdx) {
                                val aStart = beamAnchorNotes[tIdx]
                                val aEnd = beamAnchorNotes[endIdx]
                                val sx1 = if (groupStemDir > 0) aStart.anchorX - stemOffset else aStart.anchorX + stemOffset
                                val sx2 = if (groupStemDir > 0) aEnd.anchorX - stemOffset else aEnd.anchorX + stemOffset
                                val sy1 = constrainedStartY + clampedSlope * (sx1 - firstX) + tertiaryGap
                                val sy2 = constrainedStartY + clampedSlope * (sx2 - firstX) + tertiaryGap
                                tertiarySegments.add(Pair(Offset(sx1, sy1), Offset(sx2, sy2)))
                            } else {
                                val singleAnchor = beamAnchorNotes[tIdx]
                                val sx = if (groupStemDir > 0) singleAnchor.anchorX - stemOffset else singleAnchor.anchorX + stemOffset
                                val dir = if (tIdx == 0) 1f else -1f
                                val bx2 = sx + dir * 9f
                                val sy1 = constrainedStartY + clampedSlope * (sx - firstX) + tertiaryGap
                                val sy2 = constrainedStartY + clampedSlope * (bx2 - firstX) + tertiaryGap
                                beamlets.add(Pair(Offset(sx, sy1), Offset(bx2, sy2)))
                            }
                            tIdx = endIdx + 1
                        } else {
                            tIdx++
                        }
                    }

                    val beamInfo = PrecomputedBeamGroup(
                        stemDir = groupStemDir,
                        firstX = firstX,
                        lastX = lastX,
                        startY = constrainedStartY,
                        endY = constrainedEndY,
                        noteStemYMap = noteStemYMap,
                        secondarySegments = secondarySegments,
                        tertiarySegments = tertiarySegments,
                        beamlets = beamlets,
                        notes = cluster.map { it.note }
                    )
                    cluster.forEach { info ->
                        noteBeamMap[info.note] = beamInfo
                    }
                    precomputedBeams.add(beamInfo)
                }
            }
        }
    }

    val staffAssignmentMap = measure.notes.associateWith { note ->
        if (hasDifferentiatedVoices && !note.crossStaff) note.voice == 0 else if (note.crossStaff) note.voice != 0 else note.midiNote >= 60
    }
    val explicitTrebleRests = measure.rests.filter { it.voice == 0 }
    val explicitBassRests = measure.rests.filter { it.voice == 1 }
    val trebleRests = if (!isRightHandHidden) calculateRestsForStaff(measure.notes, isTreble = true, staffAssignmentMap, beatsPerMeasure, explicitTrebleRests) else emptyList()
    val bassRests = if (!isLeftHandHidden) calculateRestsForStaff(measure.notes, isTreble = false, staffAssignmentMap, beatsPerMeasure, explicitBassRests) else emptyList()

    return PrecomputedStaffLayout(
        visibleNotes = visibleNotes,
        computedXByPosition = computedXByPosition,
        noteBeamMap = noteBeamMap,
        beamGroups = precomputedBeams,
        trebleRests = trebleRests,
        bassRests = bassRests,
        hasPolyphonicVoicesOnTreble = hasPolyphonicVoicesOnTreble,
        hasPolyphonicVoicesOnBass = hasPolyphonicVoicesOnBass,
        usableMeasureWidth = usableMeasureWidth,
        notesStartX = notesStartX,
        beatsPerMeasure = beatsPerMeasure
    )
}

private fun computeTabLayout(
    measure: Measure,
    drawClefHeader: Boolean,
    drawTimeHeader: Boolean,
    startX: Float,
    endX: Float,
    tBase: Float,
    lineSpacing: Float,
    tuning: List<Int>,
    capo: Int,
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    enabledTrackIndices: Map<Int, Boolean> = emptyMap()
): PrecomputedTabLayout {
    val tabHeaderWidth = (if (drawClefHeader) 46f else 0f) + (if (drawTimeHeader) 34f else 0f)
    val systemContentStartX = startX + tabHeaderWidth
    val usableMeasureWidth = (endX - systemContentStartX - 10f).coerceAtLeast(80f)
    val nominalBeats = (measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.toFloat().coerceAtLeast(1f))).coerceAtLeast(1f)
    val maxNoteReach = measure.notes.maxOfOrNull { it.position + it.duration } ?: nominalBeats
    val beatsPerMeasure = if (measure.isPickup && maxNoteReach < nominalBeats) maxNoteReach.coerceAtLeast(0.5f) else nominalBeats

    val defaultTuning = intArrayOf(64, 59, 55, 50, 45, 40)
    val guitarOpenStringsMidi = IntArray(6) { idx ->
        (tuning.getOrElse(idx) { defaultTuning[idx] }) + capo
    }

    val hasDifferentiatedVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }
    val validNotes = measure.notes.filter { !it.isRest }
        .filter { note -> enabledTrackIndices[note.voice] ?: true }
        .filter { note ->
            val isTreble = if (hasDifferentiatedVoices) note.voice == 0 else note.midiNote >= 60
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }

    val distinctPositions = validNotes.filter { !it.grace }.map { it.position }.distinct().sorted()
    val minTabGap = 24f
    val computedXByPosition = mutableMapOf<Float, Float>()

    if (distinctPositions.isNotEmpty()) {
        val minDur = validNotes.filter { !it.grace }.minOfOrNull { it.duration.coerceAtLeast(0.125f) } ?: 1.0f
        val posDurations = distinctPositions.mapIndexed { idx, pos ->
            val nextPos = if (idx < distinctPositions.lastIndex) distinctPositions[idx + 1] else beatsPerMeasure
            val delta = (nextPos - pos).coerceAtLeast(minDur)
            (delta / minDur).toDouble().pow(0.55).toFloat()
        }
        val sumWeighted = posDurations.sum().coerceAtLeast(1f)
        val noteContentMaxX = endX - 18f
        val noteContentStartX = systemContentStartX + 14f
        val totalNaturalSpan = (noteContentMaxX - noteContentStartX).coerceAtLeast(45f)

        val naturalSpacingNeeded = (distinctPositions.size - 1) * minTabGap
        val effectiveGap = if (naturalSpacingNeeded > totalNaturalSpan && distinctPositions.size > 1) {
            totalNaturalSpan / (distinctPositions.size - 1)
        } else {
            minTabGap
        }

        var runningX = noteContentStartX
        var runningWeight = 0f
        distinctPositions.forEachIndexed { idx, pos ->
            val fraction = (runningWeight / sumWeighted).coerceIn(0f, 1f)
            val idealX = noteContentStartX + (totalNaturalSpan * fraction)
            val targetX = if (idx == 0) idealX else maxOf(idealX, runningX + effectiveGap)
            val clampedX = targetX.coerceIn(noteContentStartX, noteContentMaxX)
            computedXByPosition[pos] = snap(clampedX)
            runningX = clampedX
            runningWeight += posDurations[idx]
        }
    }

    val chordGroups = validNotes.groupBy { "${it.position}_${it.grace}" }
    val tabNotes = mutableListOf<PrecomputedTabNote>()

    chordGroups.values.forEach { notesAtPos ->
        val sample = notesAtPos.first()
        val baseNx = computedXByPosition[sample.position] ?: snap(systemContentStartX + 14f + ((usableMeasureWidth - 28f) * (sample.position / beatsPerMeasure)))
        val nx = if (sample.grace) (baseNx - 12f).coerceAtLeast(systemContentStartX + 6f) else baseNx
        val sortedDescMidi = notesAtPos.sortedByDescending { it.midiNote }
        val usedStrings = mutableSetOf<Int>()

        val candidateFrets = sortedDescMidi.mapNotNull { n ->
            (0..5).map { s -> n.midiNote - guitarOpenStringsMidi[s] }.filter { it in 1..22 }.minOrNull()
        }
        val targetFretAnchor = if (candidateFrets.isNotEmpty()) candidateFrets.average().toInt().coerceIn(1, 15) else 0

        sortedDescMidi.forEach { note ->
            val normalizedExplicitString = note.string?.let { s ->
                if (s in 1..6 && note.fret != null && (note.midiNote - guitarOpenStringsMidi.getOrElse(s - 1) { 0 }) == note.fret) s - 1
                else s.coerceIn(0, 5)
            }
            val stringIdx = if (normalizedExplicitString != null && normalizedExplicitString in 0..5 && normalizedExplicitString !in usedStrings) {
                normalizedExplicitString
            } else {
                var bestStr = (0..5).filter { it !in usedStrings }.minByOrNull { sIdx ->
                    val openPitch = guitarOpenStringsMidi[sIdx]
                    val fret = note.midiNote - openPitch
                    when {
                        fret == 0 -> 0
                        fret in 1..24 -> {
                            val distanceToAnchor = abs(fret - targetFretAnchor)
                            if (distanceToAnchor <= 4) distanceToAnchor else distanceToAnchor + 20
                        }
                        else -> 1000 + abs(fret)
                    }
                }
                if (bestStr == null) {
                    bestStr = guitarOpenStringsMidi.indices.firstOrNull { it !in usedStrings } ?: 0
                }
                bestStr
            }
            usedStrings.add(stringIdx)

            val fretValue = note.fret ?: (note.midiNote - guitarOpenStringsMidi[stringIdx]).coerceIn(0, 24)
            val ny = tBase + (stringIdx * lineSpacing)
            val fretText = when {
                note.articulation == TabArticulation.DEAD_NOTE -> "X"
                note.guitarHarmonic != null || note.harmonic != null -> "<$fretValue>"
                note.ghostNote || (note.tieEnd && !note.tieStart) -> "($fretValue)"
                else -> fretValue.toString()
            }

            val circleRadius = (lineSpacing * if (note.grace) 0.34f else 0.42f).coerceAtLeast(5.5f)
            val pillWidth = maxOf(circleRadius * 2f, (fretText.length * 6.8f) + 7f)
            val pillHeight = circleRadius * 2f

            tabNotes.add(
                PrecomputedTabNote(
                    note = note,
                    midiNote = note.midiNote,
                    stringIdx = stringIdx,
                    fretValue = fretValue,
                    nx = nx,
                    ny = ny,
                    fretText = fretText,
                    pillWidth = pillWidth,
                    pillHeight = pillHeight,
                    circleRadius = circleRadius
                )
            )
        }
    }

    tabNotes.sortWith(compareBy({ it.note.position }, { !it.note.grace }, { it.stringIdx }))
    val rests = calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { true }, beatsPerMeasure)

    return PrecomputedTabLayout(
        notes = tabNotes,
        computedXByPosition = computedXByPosition,
        rests = rests,
        systemContentStartX = systemContentStartX,
        usableMeasureWidth = usableMeasureWidth,
        beatsPerMeasure = beatsPerMeasure
    )
}

private fun DrawScope.drawBend(
    start: Offset,
    bendInfo: BendInfo,
    color: Color,
    textMeasurer: TextMeasurer
) {
    val amount = bendInfo.bendSteps.coerceIn(0.25f, 2.0f)
    val targetY = start.y - (amount * 18f)
    val apexX = start.x + 12f

    when (bendInfo.type) {
        BendType.PRE_BEND -> {
            // Straight vertical line up before striking
            drawLine(color, Offset(start.x, start.y - 4f), Offset(start.x, targetY), strokeWidth = 1.5f)
            val arrow = Path().apply {
                moveTo(start.x, targetY)
                lineTo(start.x - 3f, targetY + 4.5f)
                lineTo(start.x + 3f, targetY + 4.5f)
                close()
            }
            drawPath(arrow, color)
        }
        BendType.BEND_RELEASE, BendType.PRE_BEND_RELEASE -> {
            val releaseX = apexX + 10f
            val releaseY = start.y - (bendInfo.releaseSteps.coerceIn(0f, amount) * 18f) - 2f
            val path = Path().apply {
                moveTo(start.x + 4f, start.y - 2f)
                quadraticBezierTo(start.x + 9f, start.y - 2f, apexX, targetY)
                quadraticBezierTo(apexX + 5f, targetY, releaseX, releaseY)
            }
            drawPath(path, color = color, style = Stroke(width = 1.5f))
            val arrow = Path().apply {
                moveTo(releaseX, releaseY)
                lineTo(releaseX - 3f, releaseY - 4.5f)
                lineTo(releaseX + 3f, releaseY - 4.5f)
                close()
            }
            drawPath(arrow, color)
        }
        else -> {
            val path = Path().apply {
                moveTo(start.x + 4f, start.y - 2f)
                quadraticBezierTo(start.x + 10f, start.y - 2f, apexX, targetY)
            }
            drawPath(path, color = color, style = Stroke(width = 1.5f))
            val arrowPath = Path().apply {
                moveTo(apexX, targetY)
                lineTo(apexX - 3.2f, targetY + 4.5f)
                lineTo(apexX + 3.2f, targetY + 4.5f)
                close()
            }
            drawPath(arrowPath, color = color)
        }
    }

    val label = when {
        amount >= 1.9f -> "2"
        amount >= 1.4f -> "1½"
        amount >= 0.9f -> "full"
        amount >= 0.4f -> "½"
        else -> "¼"
    }
    val layout = textMeasurer.measure(label, TextStyle(color = color, fontSize = 7.5.sp, fontWeight = FontWeight.Bold))
    drawText(layout, topLeft = Offset(apexX - layout.size.width / 2f, targetY - layout.size.height - 1f))
}

private fun DrawScope.drawWhammyDip(
    center: Offset,
    dipSteps: Float,
    color: Color,
    textMeasurer: TextMeasurer
) {
    val path = Path().apply {
        moveTo(center.x - 7f, center.y - 14f)
        lineTo(center.x, center.y - 6f)
        lineTo(center.x + 7f, center.y - 14f)
    }
    drawPath(path, color = color, style = Stroke(width = 1.4f))
    val label = if (abs(dipSteps) >= 1f) "${dipSteps.toInt()}" else "½"
    val layout = textMeasurer.measure(label, TextStyle(color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - 24f))
}

private fun DrawScope.drawBarre(
    barre: BarreInfo,
    x: Float,
    topY: Float,
    bottomY: Float,
    color: Color,
    textMeasurer: TextMeasurer? = null
) {
    drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = 2.8f, cap = StrokeCap.Round)
    if (textMeasurer != null && barre.fret > 0) {
        val roman = when (barre.fret) {
            1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
            6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"; 10 -> "X"
            11 -> "XI"; 12 -> "XII"; else -> "B${barre.fret}"
        }
        val bLayout = textMeasurer.measure("C$roman", TextStyle(color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold))
        drawText(bLayout, topLeft = Offset(x - 4f, topY - bLayout.size.height - 4f))
    }
}

private fun calculateRestsForStaff(
    notes: List<ScoreNote>,
    isTreble: Boolean,
    staffAssignmentMap: Map<ScoreNote, Boolean>,
    beatsPerMeasure: Float,
    explicitRests: List<RestSymbol> = emptyList()
): List<RestSymbol> {
    if (explicitRests.isNotEmpty()) {
        return explicitRests
    }

    val staffNotes = notes.filter { note ->
        !note.isRest && !note.grace && (staffAssignmentMap[note] ?: (note.midiNote >= 60)) == isTreble
    }.sortedBy { it.position }

    val standardDurations = floatArrayOf(8.0f, 4.0f, 2.0f, 1.0f, 0.5f, 0.25f, 0.125f)

    fun decomposeInterval(startPos: Float, length: Float, dest: MutableList<RestSymbol>) {
        var pos = startPos
        var remaining = length
        while (remaining > 0.06f) {
            var matchedDur = 0.125f
            for (dur in standardDurations) {
                if (dur <= remaining + 0.02f) {
                    matchedDur = dur
                    break
                }
            }
            dest.add(RestSymbol(position = pos, duration = matchedDur))
            pos += matchedDur
            remaining -= matchedDur
        }
    }

    if (staffNotes.isEmpty()) {
        val wholeMeasureRests = mutableListOf<RestSymbol>()
        if (beatsPerMeasure in 3.0f..4.5f) {
            wholeMeasureRests.add(RestSymbol(position = 0.0f, duration = 4.0f))
        } else {
            decomposeInterval(0.0f, beatsPerMeasure, wholeMeasureRests)
        }
        return wholeMeasureRests
    }

    val rests = mutableListOf<RestSymbol>()
    var currentPos = 0.0f

    for (note in staffNotes) {
        if (note.position > currentPos + 0.06f) {
            decomposeInterval(currentPos, note.position - currentPos, rests)
        }
        currentPos = maxOf(currentPos, note.position + note.duration)
    }

    if (currentPos < beatsPerMeasure - 0.06f) {
        decomposeInterval(currentPos, beatsPerMeasure - currentPos, rests)
    }
    return rests
}

private fun getMeasureDpWidth(measure: Measure, isFirstMeasure: Boolean, hasMidChange: Boolean): Dp {
    val noteCount = measure.notes.size
    val baseMeasureW = when {
        measure.isPickup -> 165.dp
        noteCount > 24 -> 420.dp
        noteCount > 16 -> 360.dp
        noteCount > 8 -> 300.dp
        noteCount > 2 -> 250.dp
        else -> 200.dp
    }
    return when {
        isFirstMeasure -> baseMeasureW + 70.dp
        hasMidChange -> baseMeasureW + 40.dp
        else -> baseMeasureW
    }
}
@Composable
fun StaffAndTabNotationViewer(
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    activeMidiNotes: Map<Int, Boolean>,
    playingNote: Int?,
    primaryColor: Color,
    currentFormat: String,
    onFormatChange: (String) -> Unit,
    showLetters: Boolean = true,
    showLyrics: Boolean = true,
    isSplitStaff: Boolean = true,
    isVerticalScroll: Boolean = false,
    onVerticalScrollChange: ((Boolean) -> Unit)? = null,
    leftHandColor: Color = Color(0xFF6C63FF),
    rightHandColor: Color = Color(0xFF4A90E2),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
    isPedalPressed: Boolean = false,
    showScoreHeader: Boolean = true,
    compactStaffMode: Boolean = false,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val stableActiveNotes by remember(activeMidiNotes) {
        derivedStateOf { activeMidiNotes.filterValues { it }.keys }
    }

    val labelCache = remember(textMeasurer) {
        (0..11).associateWith { i ->
            textMeasurer.measure(
                text = MusicConstants.NOTE_LABELS[i].take(1),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 7.5.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    background = Color.Transparent
                )
            )
        }
    }
    val sharpCache = remember(textMeasurer, primaryColor) {
        textMeasurer.measure("♯", TextStyle(color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold))
    }
    val flatCache = remember(textMeasurer, primaryColor) {
        textMeasurer.measure("♭", TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    }
    val measureLabelsCache = remember(textMeasurer, score?.measures?.size, primaryColor) {
        val count = score?.measures?.size ?: 0
        (0 until count).associateWith { mIdx ->
            textMeasurer.measure(
                text = "M.${mIdx + 1}",
                style = TextStyle(color = primaryColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
    val layoutCache = remember(score) { MeasureLayoutCache() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        if (score != null && score.measures.isNotEmpty()) {
            if (score.isPdfScore && !score.pdfUri.isNullOrBlank()) {
                PdfDocumentViewer(
                    uriString = score.pdfUri,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    if (showScoreHeader) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = score.title.ifBlank { "Untitled Score" },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )
                            if (score.composer.isNotBlank() || score.lyricist.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                val byline = buildString {
                                    if (score.composer.isNotBlank()) append(score.composer)
                                    if (score.lyricist.isNotBlank()) {
                                        if (isNotEmpty()) append(" • Words: ")
                                        append(score.lyricist)
                                    }
                                }
                                Text(
                                    text = byline,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("𝅘𝅥 =", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                    Text(
                                        text = "${score.tempo.toInt()} BPM${if (score.tempoText.isNotBlank() && score.tempoText != "Moderato") " (${score.tempoText})" else ""}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text(
                                    text = score.timeSignature,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text(
                                    text = score.keySignature.keyName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor.copy(alpha = 0.9f)
                                )
                                if (score.capo > 0) {
                                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    Text(
                                        text = "Capo ${score.capo}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                                Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text(
                                    text = "${score.measures.size} Bars",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val formats = listOf("Staff" to "Staff Only", "Tab" to "Tab Only", "Both" to "Both Viewers")
                                val normFmt = currentFormat.trim().lowercase()
                                formats.forEach { (label, key) ->
                                    val active = when (label) {
                                        "Staff" -> normFmt == "staff" || normFmt == "staff only"
                                        "Tab" -> normFmt == "tab" || normFmt == "tab only"
                                        else -> normFmt == "both" || normFmt == "both viewers"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (active) primaryColor else Color.Transparent)
                                            .clickable { onFormatChange(key) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            color = if (active) Color.Black else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val normalizedFormat = currentFormat.trim().lowercase()
                    val showStaff = when {
                        normalizedFormat == "tab" || normalizedFormat == "tab only" -> false
                        else -> true
                    }
                    val showTab = when {
                        normalizedFormat == "staff" || normalizedFormat == "staff only" -> false
                        else -> true
                    }

                    val effectiveLeftHandHidden = isLeftHandHidden || !isSplitStaff
                    val rowHeight = when {
                        compactStaffMode -> if (showStaff && showTab) 345.dp else if (showStaff) (if (effectiveLeftHandHidden) 135.dp else 190.dp) else 160.dp
                        showStaff && showTab -> if (effectiveLeftHandHidden) 415.dp else 505.dp
                        showStaff -> if (effectiveLeftHandHidden) 195.dp else 280.dp
                        else -> 225.dp
                    }

                    val verticalScrollState = rememberScrollState()
                    val horizontalScrollState = rememberScrollState()
                    val rowHeightPx = with(density) { rowHeight.toPx() }

                    val horizontalMeasureBounds = remember(score.measures, density) {
                        var currentX = 0f
                        score.measures.mapIndexed { mIdx, measure ->
                            val isFirstMeasure = (mIdx == 0)
                            val noteCount = measure.notes.size
                            val baseMeasureW = when {
                                noteCount > 24 -> 420.dp
                                noteCount > 16 -> 360.dp
                                noteCount > 8 -> 300.dp
                                noteCount > 2 -> 250.dp
                                else -> 200.dp
                            }
                            val measureWidthDp = if (isFirstMeasure) baseMeasureW + 70.dp else baseMeasureW
                            val widthPx = with(density) { measureWidthDp.toPx() }
                            val startPx = currentX
                            currentX += widthPx
                            Triple(measureWidthDp, startPx, currentX)
                        }
                    }

                    var lastScrolledMeasure by remember { mutableIntStateOf(-1) }
                    LaunchedEffect(playheadMeasure, isVerticalScroll, rowHeightPx) {
                        if (playheadMeasure >= 0 && score.measures.isNotEmpty() && playheadMeasure != lastScrolledMeasure) {
                            lastScrolledMeasure = playheadMeasure
                            if (isVerticalScroll) {
                                val targetRow = playheadMeasure / 2
                                val targetY = (targetRow * rowHeightPx).toInt()
                                verticalScrollState.animateScrollTo(targetY.coerceAtLeast(0))
                            } else {
                                val targetStartPx = horizontalMeasureBounds.getOrNull(playheadMeasure)?.second ?: 0f
                                val centeredX = (targetStartPx - 80f).toInt().coerceAtLeast(0)
                                horizontalScrollState.animateScrollTo(centeredX)
                            }
                        }
                    }

                    val rowMeasureChunks = remember(score.measures, isVerticalScroll) {
                        if (isVerticalScroll) {
                            score.measures.chunked(2)
                        } else {
                            score.measures.chunked(1)
                        }
                    }

                    if (isVerticalScroll) {
                        val scrollY = verticalScrollState.value.toFloat()
                        val viewportH = if (verticalScrollState.viewportSize > 0) verticalScrollState.viewportSize.toFloat() else 900f
                        val minVisibleRow = ((scrollY - rowHeightPx) / rowHeightPx).toInt().coerceAtLeast(0)
                        val maxVisibleRow = ((scrollY + viewportH + rowHeightPx) / rowHeightPx).toInt().coerceAtMost(rowMeasureChunks.lastIndex)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(verticalScrollState)
                                .padding(vertical = 12.dp)
                        ) {
                            rowMeasureChunks.forEachIndexed { rowIndex, rowMeasures ->
                                if (rowIndex !in minVisibleRow..maxVisibleRow) {
                                    Spacer(modifier = Modifier.fillMaxWidth().height(rowHeight))
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val staffLineSpacing = if (compactStaffMode) 9.5f.dp.toPx() else 14.dp.toPx()
                                            val tabLineSpacing = if (compactStaffMode) 10.dp.toPx() else 15.dp.toPx()
                                            val trebleBaseline = if (compactStaffMode) 22.dp.toPx() else 34.dp.toPx()
                                            val bassBaseline = trebleBaseline + (5.5f * staffLineSpacing)
                                            val tabBaseline = if (showStaff) {
                                                bassBaseline + (4.0f * staffLineSpacing) + (if (compactStaffMode) 56.dp.toPx() else 84.dp.toPx())
                                            } else {
                                                if (compactStaffMode) 24.dp.toPx() else 36.dp.toPx()
                                            }
                                            val onSurface = colorScheme.onSurface
                                            val leftPadding = 12f
                                            val rightPadding = 12f
                                            val availableWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(100f)
                                            val measureWidth = availableWidth / rowMeasures.size.coerceAtLeast(1)

                                            rowMeasures.forEachIndexed { mIdxInRow, measure ->
                                                val mIdx = (rowIndex * 2) + mIdxInRow
                                                val prevMeasure = if (mIdx > 0) score.measures[mIdx - 1] else null
                                                val nextMeasure = if (mIdx + 1 < score.measures.size) score.measures[mIdx + 1] else null

                                                val isRowStart = (mIdxInRow == 0)
                                                val keyChanged = prevMeasure != null && prevMeasure.keySignature != measure.keySignature
                                                val timeChanged = prevMeasure != null && (prevMeasure.timeSignatureNumerator != measure.timeSignatureNumerator || prevMeasure.timeSignatureDenominator != measure.timeSignatureDenominator)

                                                val startX = snap(leftPadding + (mIdxInRow * measureWidth))
                                                val endX = snap(startX + measureWidth)
                                                val isPlayheadInThisMeasure = (mIdx == playheadMeasure)
                                                
                                                val staffConfig = DrawingConfig(
                                                    lineSpacing = staffLineSpacing,
                                                    trebleBaseline = trebleBaseline,
                                                    bassBaseline = bassBaseline,
                                                    primaryColor = primaryColor,
                                                    leftHandColor = leftHandColor,
                                                    rightHandColor = rightHandColor,
                                                    isLeftHandHidden = effectiveLeftHandHidden,
                                                    isRightHandHidden = isRightHandHidden,
                                                    isSingleStaff = !isSplitStaff,
                                                    isGuitarScore = showTab,
                                                    isDark = isDark,
                                                    isTab = false,
                                                    tabBaseline = tabBaseline,
                                                    isPedalPressed = isPedalPressed,
                                                    showLabels = showLetters,
                                                    showLyrics = showLyrics,
                                                    colorScheme = colorScheme
                                                )

                                                val tabConfig = DrawingConfig(
                                                    lineSpacing = tabLineSpacing,
                                                    trebleBaseline = tabBaseline,
                                                    bassBaseline = tabBaseline,
                                                    primaryColor = primaryColor,
                                                    leftHandColor = leftHandColor,
                                                    rightHandColor = rightHandColor,
                                                    isLeftHandHidden = effectiveLeftHandHidden,
                                                    isRightHandHidden = isRightHandHidden,
                                                    isDark = isDark,
                                                    isTab = true,
                                                    tabBaseline = tabBaseline,
                                                    showLabels = showLetters,
                                                    showLyrics = showLyrics,
                                                    colorScheme = colorScheme
                                                )

                                                val labelLayout = measureLabelsCache[mIdx] ?: textMeasurer.measure(
                                                    text = "M.${mIdx + 1}",
                                                    style = TextStyle(color = primaryColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                )

                                                drawText(
                                                    textLayoutResult = labelLayout,
                                                    topLeft = Offset(startX + 5f, if (showStaff) trebleBaseline - (staffLineSpacing * 2.1f) else tabBaseline - (tabLineSpacing * 2.1f))
                                                )

                                                if (showStaff) {
                                                    drawGrandStaffSystem(
                                                        ds = this,
                                                        startX = startX,
                                                        endX = endX,
                                                        trebleY = trebleBaseline,
                                                        bassY = bassBaseline,
                                                        spacing = staffLineSpacing,
                                                        isFinal = (mIdx == score.measures.lastIndex),
                                                        color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                        drawLeftBar = isRowStart,
                                                        config = staffConfig
                                                    )

                                                    if (measure.repeatStart) {
                                                        drawGrandStaffSystemBarline(
                                                            type = "repeat_start",
                                                            x = startX,
                                                            trebleTop = trebleBaseline,
                                                            trebleBottom = trebleBaseline + (4 * staffLineSpacing),
                                                            bassTop = if (effectiveLeftHandHidden) trebleBaseline else bassBaseline,
                                                            bassBottom = if (effectiveLeftHandHidden) trebleBaseline + (4 * staffLineSpacing) else bassBaseline + (4 * staffLineSpacing),
                                                            color = onSurface.copy(alpha = 0.6f),
                                                            repeatStart = true,
                                                            repeatEnd = false
                                                        )
                                                    }

                                                    val endBarType = when {
                                                        measure.repeatEnd -> "repeat_end"
                                                        mIdx == score.measures.lastIndex -> "final"
                                                        measure.barline.isNotBlank() -> measure.barline
                                                        else -> "single"
                                                    }
                                                    drawGrandStaffSystemBarline(
                                                        type = endBarType,
                                                        x = endX,
                                                        trebleTop = trebleBaseline,
                                                        trebleBottom = trebleBaseline + (4 * staffLineSpacing),
                                                        bassTop = if (effectiveLeftHandHidden) trebleBaseline else bassBaseline,
                                                        bassBottom = if (effectiveLeftHandHidden) trebleBaseline + (4 * staffLineSpacing) else bassBaseline + (4 * staffLineSpacing),
                                                        color = onSurface.copy(alpha = 0.5f),
                                                        repeatStart = false,
                                                        repeatEnd = measure.repeatEnd
                                                    )

                                                    val staffLayout = layoutCache.getStaffLayout(
                                                        measure = measure,
                                                        drawClefHeader = isRowStart,
                                                        drawKeyHeader = isRowStart || keyChanged,
                                                        drawTimeHeader = (mIdx == 0) || timeChanged,
                                                        startX = startX,
                                                        endX = endX,
                                                        trebleBaseline = trebleBaseline,
                                                        bassBaseline = bassBaseline,
                                                        lineSpacing = staffLineSpacing,
                                                        isLeftHandHidden = effectiveLeftHandHidden,
                                                        isRightHandHidden = isRightHandHidden,
                                                        enabledTrackIndices = enabledTrackIndices
                                                    )

                                                    drawStaffMeasureContent(
                                                        drawScope = this, startX = startX, endX = endX, h = size.height,
                                                        measure = measure, prevMeasure = prevMeasure, nextMeasure = nextMeasure,
                                                        activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                                        playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                        config = staffConfig, textMeasurer = textMeasurer,
                                                        noteLabels = MusicConstants.NOTE_LABELS,
                                                        diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                                        isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                                        timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                                                        drawClefHeader = isRowStart,
                                                        drawKeyHeader = isRowStart || keyChanged,
                                                        drawTimeHeader = (mIdx == 0) || timeChanged,
                                                        labelCache = labelCache, sharpCache = sharpCache, flatCache = flatCache,
                                                        staffLayout = staffLayout
                                                    )
                                                }

                                                if (showTab) {
                                                    drawGrandStaffSystem(
                                                        ds = this,
                                                        startX = startX,
                                                        endX = endX,
                                                        trebleY = tabBaseline,
                                                        bassY = tabBaseline,
                                                        spacing = tabLineSpacing,
                                                        isFinal = (mIdx == score.measures.lastIndex),
                                                        color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                        drawLeftBar = isRowStart,
                                                        config = tabConfig
                                                    )

                                                    val tabLayout = layoutCache.getTabLayout(
                                                        measure = measure,
                                                        drawClefHeader = isRowStart,
                                                        drawTimeHeader = (mIdx == 0) || timeChanged,
                                                        startX = startX,
                                                        endX = endX,
                                                        tBase = tabBaseline,
                                                        lineSpacing = tabLineSpacing,
                                                        tuning = score.tuningMidi,
                                                        capo = score.capo,
                                                        isLeftHandHidden = effectiveLeftHandHidden,
                                                        isRightHandHidden = isRightHandHidden,
                                                        enabledTrackIndices = enabledTrackIndices
                                                    )

                                                    drawTabMeasureContent(
                                                        drawScope = this, startX = startX, endX = endX, h = size.height,
                                                        measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                                        playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                        config = tabConfig, labelLayout = labelLayout, tBase = tabBaseline, textMeasurer = textMeasurer,
                                                        isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                                        timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                                                        drawClefHeader = isRowStart,
                                                        drawTimeHeader = (mIdx == 0) || timeChanged,
                                                        tuning = score.tuningMidi,
                                                        tabLayout = tabLayout,
                                                        showRhythmStems = !showStaff
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val scrollX = horizontalScrollState.value.toFloat()
                        val viewportW = if (horizontalScrollState.viewportSize > 0) horizontalScrollState.viewportSize.toFloat() else 1400f
                        val minVisibleX = scrollX - 450f
                        val maxVisibleX = scrollX + viewportW + 450f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState)
                                .padding(vertical = 6.dp)
                        ) {
                            score.measures.forEachIndexed { mIdx, measure ->
                                val isFirstMeasure = (mIdx == 0)
                                val bounds = horizontalMeasureBounds[mIdx]
                                val measureWidth = bounds.first
                                val mStartPx = bounds.second
                                val mEndPx = bounds.third
                                val isVisibleInViewport = (mEndPx >= minVisibleX && mStartPx <= maxVisibleX) || (mIdx == playheadMeasure)

                                if (!isVisibleInViewport) {
                                    Spacer(modifier = Modifier.width(measureWidth).height(rowHeight))
                                } else {
                                    Box(modifier = Modifier.width(measureWidth).height(rowHeight)) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val staffLineSpacing = if (compactStaffMode) 9.5f.dp.toPx() else 14.dp.toPx()
                                            val tabLineSpacing = if (compactStaffMode) 10.dp.toPx() else 15.dp.toPx()
                                            val trebleBaseline = if (compactStaffMode) 22.dp.toPx() else 34.dp.toPx()
                                            val bassBaseline = trebleBaseline + (5.5f * staffLineSpacing)
                                            val tabBaseline = if (showStaff) {
                                                bassBaseline + (4.0f * staffLineSpacing) + (if (compactStaffMode) 56.dp.toPx() else 84.dp.toPx())
                                            } else {
                                                if (compactStaffMode) 24.dp.toPx() else 36.dp.toPx()
                                            }
                                            val onSurface = colorScheme.onSurface
                                            val startX = 0f
                                            val endX = size.width
                                            val isPlayheadInThisMeasure = (mIdx == playheadMeasure)
                                            val prevMeasure = if (mIdx > 0) score.measures[mIdx - 1] else null
                                            val nextMeasure = if (mIdx + 1 < score.measures.size) score.measures[mIdx + 1] else null
                                            val keyChanged = prevMeasure != null && prevMeasure.keySignature != measure.keySignature
                                            val timeChanged = prevMeasure != null && (prevMeasure.timeSignatureNumerator != measure.timeSignatureNumerator || prevMeasure.timeSignatureDenominator != measure.timeSignatureDenominator)

                                            val staffConfig = DrawingConfig(
                                                lineSpacing = staffLineSpacing,
                                                trebleBaseline = trebleBaseline,
                                                bassBaseline = bassBaseline,
                                                primaryColor = primaryColor,
                                                leftHandColor = leftHandColor,
                                                rightHandColor = rightHandColor,
                                                isLeftHandHidden = effectiveLeftHandHidden,
                                                isRightHandHidden = isRightHandHidden,
                                                isSingleStaff = !isSplitStaff,
                                                isGuitarScore = showTab,
                                                isDark = isDark,
                                                isTab = false,
                                                tabBaseline = tabBaseline,
                                                isPedalPressed = isPedalPressed,
                                                showLabels = showLetters,
                                                showLyrics = showLyrics,
                                                colorScheme = colorScheme
                                            )

                                            val tabConfig = DrawingConfig(
                                                lineSpacing = tabLineSpacing,
                                                trebleBaseline = tabBaseline,
                                                bassBaseline = tabBaseline,
                                                primaryColor = primaryColor,
                                                leftHandColor = leftHandColor,
                                                rightHandColor = rightHandColor,
                                                isLeftHandHidden = effectiveLeftHandHidden,
                                                isRightHandHidden = isRightHandHidden,
                                                isDark = isDark,
                                                isTab = true,
                                                tabBaseline = tabBaseline,
                                                showLabels = showLetters,
                                                showLyrics = showLyrics,
                                                colorScheme = colorScheme
                                            )

                                            val labelLayout = measureLabelsCache[mIdx] ?: textMeasurer.measure(
                                                text = "M.${mIdx + 1}",
                                                style = TextStyle(color = primaryColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            )

                                            drawText(
                                                textLayoutResult = labelLayout,
                                                topLeft = Offset(startX + (if (isFirstMeasure) 8f else 5f), if (showStaff) trebleBaseline - (staffLineSpacing * 2.1f) else tabBaseline - (tabLineSpacing * 2.1f))
                                            )

                                            if (showStaff) {
                                                drawGrandStaffSystem(
                                                    ds = this,
                                                    startX = startX,
                                                    endX = endX,
                                                    trebleY = trebleBaseline,
                                                    bassY = bassBaseline,
                                                    spacing = staffLineSpacing,
                                                    isFinal = (mIdx == score.measures.lastIndex),
                                                    color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    drawLeftBar = isFirstMeasure,
                                                    config = staffConfig
                                                )

                                                if (measure.repeatStart) {
                                                    drawGrandStaffSystemBarline(
                                                        type = "repeat_start",
                                                        x = startX,
                                                        trebleTop = trebleBaseline,
                                                        trebleBottom = trebleBaseline + (4 * staffLineSpacing),
                                                        bassTop = if (effectiveLeftHandHidden) trebleBaseline else bassBaseline,
                                                        bassBottom = if (effectiveLeftHandHidden) trebleBaseline + (4 * staffLineSpacing) else bassBaseline + (4 * staffLineSpacing),
                                                        color = onSurface.copy(alpha = 0.6f),
                                                        repeatStart = true,
                                                        repeatEnd = false
                                                    )
                                                }

                                                val endBarType = when {
                                                    measure.repeatEnd -> "repeat_end"
                                                    mIdx == score.measures.lastIndex -> "final"
                                                    measure.barline.isNotBlank() -> measure.barline
                                                    else -> "single"
                                                }
                                                drawGrandStaffSystemBarline(
                                                    type = endBarType,
                                                    x = endX,
                                                    trebleTop = trebleBaseline,
                                                    trebleBottom = trebleBaseline + (4 * staffLineSpacing),
                                                    bassTop = if (effectiveLeftHandHidden) trebleBaseline else bassBaseline,
                                                    bassBottom = if (effectiveLeftHandHidden) trebleBaseline + (4 * staffLineSpacing) else bassBaseline + (4 * staffLineSpacing),
                                                    color = onSurface.copy(alpha = 0.5f),
                                                    repeatStart = false,
                                                    repeatEnd = measure.repeatEnd
                                                )

                                                val staffLayout = layoutCache.getStaffLayout(
                                                    measure = measure,
                                                    drawClefHeader = isFirstMeasure,
                                                    drawKeyHeader = isFirstMeasure || keyChanged,
                                                    drawTimeHeader = isFirstMeasure || timeChanged,
                                                    startX = startX,
                                                    endX = endX,
                                                    trebleBaseline = trebleBaseline,
                                                    bassBaseline = bassBaseline,
                                                    lineSpacing = staffLineSpacing,
                                                    isLeftHandHidden = effectiveLeftHandHidden,
                                                    isRightHandHidden = isRightHandHidden,
                                                    enabledTrackIndices = enabledTrackIndices
                                                )

                                                drawStaffMeasureContent(
                                                    drawScope = this, startX = startX, endX = endX, h = size.height,
                                                    measure = measure, prevMeasure = prevMeasure, nextMeasure = nextMeasure,
                                                    activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                                    playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                    config = staffConfig, textMeasurer = textMeasurer,
                                                    noteLabels = MusicConstants.NOTE_LABELS,
                                                    diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                                    isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                                    timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                                                    drawClefHeader = isFirstMeasure,
                                                    drawKeyHeader = isFirstMeasure || keyChanged,
                                                    drawTimeHeader = isFirstMeasure || timeChanged,
                                                    labelCache = labelCache, sharpCache = sharpCache, flatCache = flatCache,
                                                    staffLayout = staffLayout
                                                )
                                            }

                                            if (showTab) {
                                                drawGrandStaffSystem(
                                                    ds = this,
                                                    startX = startX,
                                                    endX = endX,
                                                    trebleY = tabBaseline,
                                                    bassY = tabBaseline,
                                                    spacing = tabLineSpacing,
                                                    isFinal = (mIdx == score.measures.lastIndex),
                                                    color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    drawLeftBar = isFirstMeasure,
                                                    config = tabConfig
                                                )

                                                val tabLayout = layoutCache.getTabLayout(
                                                    measure = measure,
                                                    drawClefHeader = isFirstMeasure,
                                                    drawTimeHeader = isFirstMeasure || timeChanged,
                                                    startX = startX,
                                                    endX = endX,
                                                    tBase = tabBaseline,
                                                    lineSpacing = tabLineSpacing,
                                                    tuning = score.tuningMidi,
                                                    capo = score.capo,
                                                    isLeftHandHidden = effectiveLeftHandHidden,
                                                    isRightHandHidden = isRightHandHidden,
                                                    enabledTrackIndices = enabledTrackIndices
                                                )

                                                drawTabMeasureContent(
                                                    drawScope = this, startX = startX, endX = endX, h = size.height,
                                                    measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                                    playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                    config = tabConfig, labelLayout = labelLayout, tBase = tabBaseline, textMeasurer = textMeasurer,
                                                    isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                                    timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                                                    drawClefHeader = isFirstMeasure,
                                                    drawTimeHeader = isFirstMeasure || timeChanged,
                                                    tuning = score.tuningMidi,
                                                    tabLayout = tabLayout,
                                                    showRhythmStems = !showStaff
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
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No score loaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
private fun DrawScope.drawMeasureBarline(
    type: String,
    x: Float,
    topY: Float,
    bottomY: Float,
    color: Color,
    repeatStart: Boolean = false,
    repeatEnd: Boolean = false
) {
    val finalStroke = 3.dp.toPx()
    val thinStroke = 1.dp.toPx()
    val gap = 4.dp.toPx()

    if (repeatEnd) {
        val midY = (topY + bottomY) / 2f
        val dotRadius = 2.2f
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, midY - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, midY + 6f))
        drawLine(color, Offset(x - gap, topY), Offset(x - gap, bottomY), strokeWidth = thinStroke)
        drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = finalStroke)
        return
    }

    if (repeatStart) {
        val midY = (topY + bottomY) / 2f
        val dotRadius = 2.2f
        drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = finalStroke)
        drawLine(color, Offset(x + gap, topY), Offset(x + gap, bottomY), strokeWidth = thinStroke)
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, midY - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, midY + 6f))
        return
    }

    when (type) {
        "double" -> {
            drawLine(color, Offset(x - gap, topY), Offset(x - gap, bottomY), strokeWidth = thinStroke)
            drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = thinStroke)
        }
        "final" -> {
            drawLine(color, Offset(x - gap - 1f, topY), Offset(x - gap - 1f, bottomY), strokeWidth = thinStroke)
            drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = finalStroke)
        }
        else -> drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = thinStroke)
    }
}

private fun DrawScope.drawGrandStaffSystemBarline(
    type: String,
    x: Float,
    trebleTop: Float,
    trebleBottom: Float,
    bassTop: Float,
    bassBottom: Float,
    color: Color,
    repeatStart: Boolean = false,
    repeatEnd: Boolean = false
) {
    val finalStroke = 3.dp.toPx()
    val thinStroke = 1.dp.toPx()
    val gap = 4.dp.toPx()

    if (repeatEnd) {
        val trebleMid = (trebleTop + trebleBottom) / 2f
        val bassMid = (bassTop + bassBottom) / 2f
        val dotRadius = 2.2f
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, trebleMid - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, trebleMid + 6f))
        if (bassTop != trebleTop) {
            drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, bassMid - 6f))
            drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, bassMid + 6f))
        }

        drawLine(color, Offset(x - gap, trebleTop), Offset(x - gap, trebleBottom), strokeWidth = thinStroke)
        if (bassTop != trebleTop) drawLine(color, Offset(x - gap, bassTop), Offset(x - gap, bassBottom), strokeWidth = thinStroke)
        drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
        if (bassTop != trebleTop) drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
        return
    }

    if (repeatStart) {
        val trebleMid = (trebleTop + trebleBottom) / 2f
        val bassMid = (bassTop + bassBottom) / 2f
        val dotRadius = 2.2f
        drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
        if (bassTop != trebleTop) drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
        drawLine(color, Offset(x + gap, trebleTop), Offset(x + gap, trebleBottom), strokeWidth = thinStroke)
        if (bassTop != trebleTop) drawLine(color, Offset(x + gap, bassTop), Offset(x + gap, bassBottom), strokeWidth = thinStroke)

        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, trebleMid - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, trebleMid + 6f))
        if (bassTop != trebleTop) {
            drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, bassMid - 6f))
            drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, bassMid + 6f))
        }
        return
    }

    when (type) {
        "final" -> {
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
            if (bassTop != trebleTop) drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
            drawLine(color, Offset(x - gap - 1f, trebleTop), Offset(x - gap - 1f, trebleBottom), strokeWidth = thinStroke)
            if (bassTop != trebleTop) drawLine(color, Offset(x - gap - 1f, bassTop), Offset(x - gap - 1f, bassBottom), strokeWidth = thinStroke)
        }
        "double" -> {
            drawLine(color, Offset(x - gap, trebleTop), Offset(x - gap, trebleBottom), strokeWidth = thinStroke)
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = thinStroke)
            if (bassTop != trebleTop) {
                drawLine(color, Offset(x - gap, bassTop), Offset(x - gap, bassBottom), strokeWidth = thinStroke)
                drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = thinStroke)
            }
        }
        else -> {
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = thinStroke)
            if (bassTop != trebleTop) drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = thinStroke)
        }
    }
}

private fun drawTabMeasureContent(
    drawScope: DrawScope, startX: Float, endX: Float, h: Float, labelLayout: TextLayoutResult,
    tBase: Float,
    measure: Measure, activeMidiNotes: Set<Int>, playingNote: Int?,
    playheadOffset: Float?, config: DrawingConfig, textMeasurer: TextMeasurer,
    isFinalBar: Boolean, isMeasureActive: Boolean, timeSignature: String,
    drawClefHeader: Boolean, drawTimeHeader: Boolean,
    tuning: List<Int>,
    tabLayout: PrecomputedTabLayout? = null,
    showRhythmStems: Boolean = true
) {
    with(drawScope) {
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))
        val tabClefH = config.lineSpacing * 5.0f
        val tabClefW = tabClefH * 0.33f

        if (drawClefHeader) {
            with(MusicVectorSymbols) {
                drawTabClef(
                    color = clefTextColor,
                    topLeft = Offset(startX + 8f, tBase),
                    size = Size(tabClefW, tabClefH)
                )
            }

            // Render String Tuning Labels (e.g., E B G D A E or Drop D)
            val pitchNames = arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
            for (sIdx in 0..5) {
                val midi = tuning.getOrElse(sIdx) { intArrayOf(64, 59, 55, 50, 45, 40)[sIdx] }
                val sName = pitchNames[((midi % 12) + 12) % 12]
                val sLayout = textMeasurer.measure(sName, TextStyle(color = clefTextColor.copy(alpha = 0.55f), fontSize = 6.5.sp, fontWeight = FontWeight.Bold))
                val sy = tBase + (sIdx * config.lineSpacing) - (sLayout.size.height / 2f)
                drawText(sLayout, topLeft = Offset((startX + 2f).coerceAtLeast(0f), sy))
            }
        }

        if (drawTimeHeader) {
            val tsX = startX + (if (drawClefHeader) 12f + tabClefW + 6f else 8f)
            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                with(MusicVectorSymbols) {
                    drawTimeSignature(
                        numerator = tsParts[0],
                        denominator = tsParts[1],
                        color = clefTextColor,
                        topLeft = Offset(tsX, tBase + (config.lineSpacing * 0.5f)),
                        lineSpacing = config.lineSpacing
                    )
                }
            }
        }

        if (measure.repeatStart) {
            drawMeasureBarline(
                type = "repeat_start",
                x = startX,
                topY = tBase,
                bottomY = tBase + (5 * config.lineSpacing),
                color = clefTextColor.copy(alpha = 0.6f),
                repeatStart = true,
                repeatEnd = false
            )
        }

        val tabBarType = when {
            measure.repeatEnd -> "repeat_end"
            isFinalBar -> "final"
            measure.barline.isNotBlank() -> measure.barline
            else -> "single"
        }
        drawMeasureBarline(
            type = tabBarType,
            x = endX,
            topY = tBase,
            bottomY = tBase + (5 * config.lineSpacing),
            color = clefTextColor.copy(alpha = 0.5f),
            repeatStart = false,
            repeatEnd = measure.repeatEnd
        )

        val layout = tabLayout ?: computeTabLayout(
            measure = measure,
            drawClefHeader = drawClefHeader,
            drawTimeHeader = drawTimeHeader,
            startX = startX,
            endX = endX,
            tBase = tBase,
            lineSpacing = config.lineSpacing,
            tuning = tuning,
            capo = 0
        )

        val systemContentStartX = layout.systemContentStartX
        val usableMeasureWidth = layout.usableMeasureWidth
        val beatsPerMeasure = layout.beatsPerMeasure

        val notesByString = layout.notes.groupBy { it.stringIdx }

        // Group chords by position for Arpeggio rolls on Tab
        val notesByPosition = layout.notes.groupBy { it.note.position }
        for ((_, chordAtPos) in notesByPosition) {
            val arpNote = chordAtPos.firstOrNull { it.note.arpeggio != ArpeggioDirection.NONE }
            if (arpNote != null && chordAtPos.size >= 2) {
                val minY = chordAtPos.minOf { it.ny }
                val maxY = chordAtPos.maxOf { it.ny }
                drawArpeggioRoll(arpNote.nx - 11f, minY, maxY, arpNote.note.arpeggio, config.primaryColor)
            }
        }

        layout.notes.forEach { tabNote ->
            val note = tabNote.note
            val nx = tabNote.nx
            val ny = tabNote.ny
            val isSounding = isNoteTemporallyActive(
                note = note,
                isMeasureActive = isMeasureActive,
                playheadOffset = playheadOffset,
                beatsPerMeasure = beatsPerMeasure,
                playingNote = playingNote,
                activeMidiNotes = activeMidiNotes
            )

            if (note.chordSymbol.isNotEmpty() && tabNote == notesByPosition[note.position]?.firstOrNull()) {
                val chordLayout = textMeasurer.measure(
                    text = note.chordSymbol,
                    style = TextStyle(color = config.primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(chordLayout, topLeft = Offset(nx - chordLayout.size.width / 2f, tBase - 26f))
            }

            drawRoundRect(
                color = if (isSounding) config.primaryColor else (colorScheme?.surfaceVariant ?: (if (config.isDark) Color(0xFF2C2C2A) else Color(0xFFFCFBF7))),
                topLeft = Offset(nx - tabNote.pillWidth / 2f, ny - tabNote.pillHeight / 2f),
                size = Size(tabNote.pillWidth, tabNote.pillHeight),
                cornerRadius = CornerRadius(tabNote.pillHeight / 2f, tabNote.pillHeight / 2f)
            )

            val fretFontSize = if (note.grace) 6.5.sp else 8.sp
            val fretLayout = textMeasurer.measure(
                text = tabNote.fretText,
                style = TextStyle(
                    color = if (isSounding) Color.White else clefTextColor,
                    fontSize = fretFontSize,
                    fontWeight = if (isSounding) FontWeight.Black else FontWeight.Bold
                )
            )
            drawText(fretLayout, topLeft = Offset(nx - fretLayout.size.width / 2f, ny - fretLayout.size.height / 2f))

            if (config.showLyrics && note.lyric.isNotEmpty() && tabNote == notesByPosition[note.position]?.firstOrNull()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                val lyricY = tBase + 5 * config.lineSpacing + (if (showRhythmStems) 28f else 10f)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, lyricY))
            }

            val sameStringNotes = notesByString[tabNote.stringIdx] ?: emptyList()
            val strIdxPos = sameStringNotes.indexOf(tabNote)
            val nextOnString = if (strIdxPos >= 0 && strIdxPos + 1 < sameStringNotes.size) sameStringNotes[strIdxPos + 1] else null

          when (note.articulation) {
                TabArticulation.HAMMER_ON, TabArticulation.PULL_OFF -> {
                    val label = if (note.articulation == TabArticulation.HAMMER_ON) MusicSymbols.HAMMER_ON else MusicSymbols.PULL_OFF
                    val targetX = nextOnString?.nx ?: (nx + 20f).coerceAtMost(endX - 6f)
                    val midX = (nx + targetX) / 2f
                    drawEngravedCurve(
                        startX = nx + 3f,
                        startY = ny - tabNote.pillHeight * 0.45f,
                        endX = targetX - 3f,
                        endY = ny - tabNote.pillHeight * 0.45f,
                        direction = -1f,
                        color = config.primaryColor
                    )
                    val hpLayout = textMeasurer.measure(label, TextStyle(color = config.primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                    drawText(hpLayout, topLeft = Offset(midX - hpLayout.size.width / 2f, ny - config.lineSpacing * 1.15f))
                }
                TabArticulation.SLIDE_UP, TabArticulation.LEGATO_SLIDE_UP -> {
                    val targetX = nextOnString?.nx ?: (nx + 18f)
                    val sStartX = nx + tabNote.pillWidth / 2f + 1f
                    val sEndX = (targetX - (nextOnString?.pillWidth ?: 10f) / 2f - 1f).coerceAtLeast(sStartX + 8f)
                    drawLine(config.primaryColor, Offset(sStartX, ny + 4f), Offset(sEndX, ny - 4f), strokeWidth = 1.8f)
                }
                TabArticulation.SLIDE_DOWN, TabArticulation.LEGATO_SLIDE_DOWN -> {
                    val targetX = nextOnString?.nx ?: (nx + 18f)
                    val sStartX = nx + tabNote.pillWidth / 2f + 1f
                    val sEndX = (targetX - (nextOnString?.pillWidth ?: 10f) / 2f - 1f).coerceAtLeast(sStartX + 8f)
                    drawLine(config.primaryColor, Offset(sStartX, ny - 4f), Offset(sEndX, ny + 4f), strokeWidth = 1.8f)
                }
                TabArticulation.RELEASE -> {
                    val rLayout = textMeasurer.measure(MusicSymbols.RELEASE, TextStyle(color = config.primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                    drawText(rLayout, topLeft = Offset(nx - rLayout.size.width / 2f, ny - config.lineSpacing * 0.9f))
                }
                TabArticulation.TAP -> {
                    val tLayout = textMeasurer.measure("T", TextStyle(color = config.primaryColor, fontSize = 8.5.sp, fontWeight = FontWeight.Black))
                    drawText(tLayout, topLeft = Offset(nx - tLayout.size.width / 2f, ny - config.lineSpacing * 0.95f))
                }
                TabArticulation.DEAD_NOTE, TabArticulation.NONE -> {}
            }

            if ((note.tieStart || note.slurStart) && note.articulation != TabArticulation.HAMMER_ON && note.articulation != TabArticulation.PULL_OFF) {
                val targetX = nextOnString?.nx ?: (endX - 4f)
                val curveDir = if (tabNote.stringIdx <= 2) -1f else 1f
                val anchorY = ny + curveDir * (tabNote.pillHeight * 0.45f)
                drawEngravedCurve(
                    startX = nx + 3f,
                    startY = anchorY,
                    endX = (targetX - 3f).coerceAtLeast(nx + 12f),
                    endY = anchorY,
                    direction = curveDir,
                    color = clefTextColor.copy(alpha = 0.8f),
                    isDashed = note.tieStart
                )
            }

            // Specific Harmonic Label (N.H., A.H., P.H., T.H.)
            val harmText = note.guitarHarmonic?.label ?: if (note.harmonic != null) "Harm." else null
            if (harmText != null) {
                val harmLayout = textMeasurer.measure(harmText, TextStyle(color = config.primaryColor, fontSize = 7.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold))
                drawText(harmLayout, topLeft = Offset(nx - harmLayout.size.width / 2f, tBase - 14f))
            }

            // Pick Direction (Downstroke ⊓ / Upstroke ∨)
            if (note.pickDirection != PickDirection.NONE && tabNote == notesByPosition[note.position]?.firstOrNull()) {
                val py = tBase - 11f
                if (note.pickDirection == PickDirection.DOWN) {
                    drawLine(clefTextColor, Offset(nx - 3.5f, py), Offset(nx + 3.5f, py), strokeWidth = 1.6f)
                    drawLine(clefTextColor, Offset(nx - 3.5f, py), Offset(nx - 3.5f, py + 5.5f), strokeWidth = 1.3f)
                    drawLine(clefTextColor, Offset(nx + 3.5f, py), Offset(nx + 3.5f, py + 5.5f), strokeWidth = 1.3f)
                } else {
                    drawLine(clefTextColor, Offset(nx - 3.5f, py), Offset(nx, py + 6f), strokeWidth = 1.4f)
                    drawLine(clefTextColor, Offset(nx + 3.5f, py), Offset(nx, py + 6f), strokeWidth = 1.4f)
                }
            }

            // Fingerstyle right-hand letters (p, i, m, a)
            if (note.rightHandFinger.isNotEmpty()) {
                val rhLayout = textMeasurer.measure(note.rightHandFinger, TextStyle(color = clefTextColor.copy(alpha = 0.85f), fontSize = 7.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold))
                drawText(rhLayout, topLeft = Offset(nx + tabNote.pillWidth / 2f + 1f, ny - 6f))
            }

            if (note.vibrato) {
                with(MusicVectorSymbols) {
                    drawSmuflSymbol("wiggleArpeggiatoUp", config.primaryColor, Offset(nx - 6f, ny - 13f), Size(12f, 7f), 400f)
                }
            }

            if (note.fermata && tabNote == notesByPosition[note.position]?.firstOrNull()) {
                drawFermataSymbol(nx, tBase - 14f, clefTextColor)
            }

            note.whammy?.let { whammyInfo ->
                drawWhammyDip(Offset(nx, tBase), whammyInfo.steps, config.primaryColor, textMeasurer)
            }

            note.bend?.let { bend ->
                drawBend(Offset(nx, ny), bend, config.primaryColor, textMeasurer)
            }
            note.barre?.let { barre ->
                val bTop = tBase + (barre.startString.coerceIn(0, 5) * config.lineSpacing)
                val bBottom = tBase + (barre.endString.coerceIn(0, 5) * config.lineSpacing)
                drawBarre(barre, nx - 11f, bTop, bBottom, config.primaryColor.copy(alpha = 0.65f), textMeasurer)
            }
        }

        // Draw Continuous Palm Mute (P.M. ---┐) and Let Ring (let ring ---┐) Spans
        val orderedPositions = notesByPosition.keys.sorted()
        fun renderSpanOnTab(predicate: (ScoreNote) -> Boolean, label: String, yPos: Float) {
            var idx = 0
            while (idx < orderedPositions.size) {
                val pos = orderedPositions[idx]
                val notesAtP = notesByPosition[pos] ?: emptyList()
                if (notesAtP.any { predicate(it.note) }) {
                    var endIdx = idx
                    while (endIdx + 1 < orderedPositions.size && (notesByPosition[orderedPositions[endIdx + 1]]?.any { predicate(it.note) } == true)) {
                        endIdx++
                    }
                    val startXSpan = notesAtP.first().nx - 4f
                    val endXSpan = (notesByPosition[orderedPositions[endIdx]]?.first()?.nx ?: startXSpan) + 6f
                    drawHorizontalDashedBracket(label, startXSpan, endXSpan, yPos, hookDir = 1f, clefTextColor.copy(alpha = 0.72f), textMeasurer)
                    idx = endIdx + 1
                } else {
                    idx++
                }
            }
        }
        renderSpanOnTab({ it.palmMute }, "P.M.", tBase - 11f)
        renderSpanOnTab({ it.letRing }, "let ring", tBase - 20f)

        // Draw Tab Rhythm Stems below the 6th string
        if (showRhythmStems) {
            val stemTopY = tBase + (5f * config.lineSpacing) + 5f
            val stemBottomY = stemTopY + 15f
            val posMap = layout.notes.filter { !it.note.grace }.groupBy { it.note.position }.toSortedMap()
            val positions = posMap.keys.toList()

            positions.forEach { pos ->
                val notesAtPos = posMap[pos] ?: return@forEach
                val rep = notesAtPos.first()
                val dur = notesAtPos.minOf { it.note.duration }
                val sx = rep.nx
                val stemColor = clefTextColor.copy(alpha = 0.65f)

                if (dur < 4.0f) {
                    val topStart = if (dur >= 2.0f) stemTopY + 6f else stemTopY
                    drawLine(stemColor, Offset(sx, topStart), Offset(sx, stemBottomY), strokeWidth = 1.2f)
                }
                if (notesAtPos.any { it.note.dotted || it.note.isDotted }) {
                    drawCircle(stemColor, radius = 1.8f, center = Offset(sx + 4.5f, stemBottomY - 3f))
                    if (notesAtPos.any { it.note.doubleDotted }) {
                        drawCircle(stemColor, radius = 1.8f, center = Offset(sx + 8.5f, stemBottomY - 3f))
                    }
                }
            }

            val beatGroups = positions.groupBy { floor(it).toInt() }
            for ((_, beatPosList) in beatGroups) {
                val eighthPositions = beatPosList.filter { p -> (posMap[p]?.minOf { it.note.duration } ?: 1f) <= 0.75f }
                if (eighthPositions.size >= 2) {
                    val x1 = posMap[eighthPositions.first()]!!.first().nx
                    val x2 = posMap[eighthPositions.last()]!!.first().nx
                    drawLine(clefTextColor.copy(alpha = 0.7f), Offset(x1, stemBottomY), Offset(x2, stemBottomY), strokeWidth = 2.6f)

                    val sixteenthPositions = eighthPositions.filter { p -> (posMap[p]?.minOf { it.note.duration } ?: 1f) <= 0.35f }
                    if (sixteenthPositions.size >= 2) {
                        val sx1 = posMap[sixteenthPositions.first()]!!.first().nx
                        val sx2 = posMap[sixteenthPositions.last()]!!.first().nx
                        drawLine(clefTextColor.copy(alpha = 0.7f), Offset(sx1, stemBottomY - 4f), Offset(sx2, stemBottomY - 4f), strokeWidth = 2.4f)
                    }
                } else if (eighthPositions.size == 1) {
                    val p = eighthPositions.first()
                    val x = posMap[p]!!.first().nx
                    val dur = posMap[p]!!.minOf { it.note.duration }
                    drawLine(clefTextColor.copy(alpha = 0.7f), Offset(x, stemBottomY), Offset(x + 6f, stemBottomY - 4f), strokeWidth = 2.0f)
                    if (dur <= 0.35f) {
                        drawLine(clefTextColor.copy(alpha = 0.7f), Offset(x, stemBottomY - 4f), Offset(x + 6f, stemBottomY - 8f), strokeWidth = 2.0f)
                    }
                }
            }
        }
// Render Rests for Tab staff
        layout.rests.forEach { rest ->
            val rx = layout.computedXByPosition[rest.position]
                ?: snap(systemContentStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            val ry = tBase + 2.5f * config.lineSpacing
            val restScale = config.lineSpacing / 14f
            val restColor = clefTextColor.copy(alpha = 0.55f)

            with(MusicVectorSymbols) {
                when {
                    rest.duration >= 8.0f -> drawRestDoubleWhole(restColor, Size(12f * restScale, config.lineSpacing), topLeft = Offset(rx - 6f * restScale, ry))
                    rest.duration >= 3.5f -> {
                        val centerMeasureX = snap((systemContentStartX + endX) / 2f)
                        val wholeRestX = if (rest.position == 0f && layout.rests.size == 1) centerMeasureX else rx
                        drawRestWhole(restColor, Size(16f * restScale, 10f * restScale), topLeft = Offset(wholeRestX - 8f * restScale, ry - 5f * restScale))
                    }
                    rest.duration >= 1.75f -> drawRestHalf(restColor, Size(16f * restScale, 10f * restScale), topLeft = Offset(rx - 8f * restScale, ry))
                    rest.duration >= 0.85f -> {
                        val rw = 12f * restScale
                        val rh = 28f * restScale
                        drawRestQuarter(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                    rest.duration >= 0.4f -> {
                        val rw = 11f * restScale
                        val rh = 22f * restScale
                        drawRestEighth(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                    rest.duration >= 0.2f -> {
                        val rw = 13f * restScale
                        val rh = 26f * restScale
                        drawRest16th(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                    rest.duration >= 0.1f -> {
                        val rw = 14f * restScale
                        val rh = 30f * restScale
                        drawRest32nd(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                    else -> {
                        val rw = 15f * restScale
                        val rh = 34f * restScale
                        drawRest64th(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                }
            }
        }

        // Navigation marks (Segno, Coda, D.S., D.C., Fine) for Tab
        if (measure.segno) {
            with(MusicVectorSymbols) {
                drawSegno(config.primaryColor, Size(18f, 18f), topLeft = Offset(startX + 5f, tBase - 24f))
            }
        }
        if (measure.coda) {
            with(MusicVectorSymbols) {
                drawCoda(config.primaryColor, Size(18f, 18f), topLeft = Offset(startX + 26f, tBase - 24f))
            }
        }
        if (measure.dalSegno || measure.daCapo || measure.toCoda || measure.fine) {
            val navText = when {
                measure.dalSegno -> "D.S."
                measure.daCapo -> "D.C."
                measure.toCoda -> "To Coda 𝄌"
                else -> "Fine"
            }
            val navLayout = textMeasurer.measure(
                text = navText,
                style = TextStyle(color = clefTextColor, fontSize = 8.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
            )
            drawText(navLayout, topLeft = Offset(endX - navLayout.size.width - 6f, tBase - 22f))
        }

        if (playheadOffset != null) {
            drawPlayheadTabLine(
                drawScope = this,
                notes = layout.notes,
                computedXByPosition = layout.computedXByPosition,
                systemContentStartX = systemContentStartX,
                usableMeasureWidth = usableMeasureWidth,
                beatsPerMeasure = beatsPerMeasure,
                tBase = tBase,
                playingNote = playingNote,
                playheadOffset = playheadOffset,
                config = config
            )
        }
    }
}

internal fun drawPlayheadTabLine(
    drawScope: DrawScope,
    notes: List<PrecomputedTabNote>,
    computedXByPosition: Map<Float, Float>,
    systemContentStartX: Float,
    usableMeasureWidth: Float,
    beatsPerMeasure: Float,
    tBase: Float,
    playingNote: Int?,
    playheadOffset: Float,
    config: DrawingConfig
) {
    with(drawScope) {
        val currentBeat = playheadOffset * beatsPerMeasure
        val temporallyMatchedNote = if (playingNote != null) {
            notes.firstOrNull {
                it.midiNote == playingNote &&
                    currentBeat >= (it.note.position - 0.08f) &&
                    currentBeat <= (it.note.position + it.note.duration.coerceAtLeast(0.125f) + 0.04f)
            }
        } else null

        val targetPlayheadX = if (temporallyMatchedNote != null && abs(currentBeat - temporallyMatchedNote.note.position) <= 0.12f) {
            temporallyMatchedNote.nx
        } else if (computedXByPosition.isNotEmpty()) {
            interpolatePlayheadX(computedXByPosition, currentBeat, systemContentStartX, usableMeasureWidth, playheadOffset)
        } else {
            systemContentStartX + (usableMeasureWidth * playheadOffset)
        }

        drawLine(
            color = config.primaryColor,
            start = Offset(targetPlayheadX, tBase - 10f),
            end = Offset(targetPlayheadX, tBase + (5 * config.lineSpacing) + 10f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun interpolatePlayheadX(
    computedXByPosition: Map<Float, Float>,
    targetPos: Float,
    startX: Float,
    usableWidth: Float,
    playheadOffset: Float
): Float {
    var exactOrLowerPos = -1f
    var exactOrLowerX = -1f
    var higherPos = Float.MAX_VALUE
    var higherX = -1f

    for ((pos, x) in computedXByPosition) {
        if (pos <= targetPos && pos > exactOrLowerPos) {
            exactOrLowerPos = pos
            exactOrLowerX = x
        }
        if (pos > targetPos && pos < higherPos) {
            higherPos = pos
            higherX = x
        }
    }

    return when {
        exactOrLowerX != -1f && higherX != -1f -> {
            val fraction = if (higherPos > exactOrLowerPos) (targetPos - exactOrLowerPos) / (higherPos - exactOrLowerPos) else 0f
            exactOrLowerX + (higherX - exactOrLowerX) * fraction
        }
        exactOrLowerX != -1f -> exactOrLowerX
        higherX != -1f -> higherX
        else -> startX + (usableWidth * playheadOffset)
    }
}
private fun drawStaffMeasureContent(
    drawScope: DrawScope, startX: Float, endX: Float, h: Float,
    measure: Measure, prevMeasure: Measure?, nextMeasure: Measure?,
    activeMidiNotes: Set<Int>, playingNote: Int?,
    playheadOffset: Float?, config: DrawingConfig, textMeasurer: TextMeasurer,
    noteLabels: List<String>, diatonicSteps: IntArray, isFinalBar: Boolean,
    isMeasureActive: Boolean, timeSignature: String, drawClefHeader: Boolean,
    drawKeyHeader: Boolean, drawTimeHeader: Boolean,
    labelCache: Map<Int, TextLayoutResult>, sharpCache: TextLayoutResult, flatCache: TextLayoutResult,
    staffLayout: PrecomputedStaffLayout? = null
) {
    with(drawScope) {
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))
        val trebleBaseline = config.trebleBaseline
        val bassBaseline = config.bassBaseline
        val staffLineColor = colorScheme?.outlineVariant ?: (if (config.isDark) Color(0xFF4A4945) else Color(0xFFCCCCCC))

        val clefH = config.lineSpacing * 6.8f
        val clefW = clefH * 0.357f
        val bassH = config.lineSpacing * 3.4f
        val bassW = bassH * 0.81f
        val maxClefW = maxOf(clefW, bassW)

        if (drawClefHeader) {
            val trebleClefTop = (trebleBaseline + 3f * config.lineSpacing) - (0.6316f * clefH)

            if (!config.isRightHandHidden) {
                with(MusicVectorSymbols) {
                    drawTrebleClef(
                        color = clefTextColor,
                        topLeft = Offset(startX + 6f, trebleClefTop),
                        size = Size(clefW, clefH)
                    )
                }
            }

            val bassClefTop = (bassBaseline + 1f * config.lineSpacing) - (0.3171f * bassH)
            if (!config.isLeftHandHidden) {
                with(MusicVectorSymbols) {
                    drawBassClef(
                        color = clefTextColor,
                        topLeft = Offset(startX + 8f, bassClefTop),
                        size = Size(bassW, bassH)
                    )
                }
            }
        }

        // Render standard Key Signature sharps or flats between Clef and Time Signature
        val ksCount = measure.keySignature.accidentalsCount
        val ksIsSharp = measure.keySignature.isSharp
        val ksStepX = 8f
        val ksStartX = startX + maxClefW + 6f
        
        if (drawKeyHeader && ksCount > 0) {
            val accH = config.lineSpacing * 1.35f
            val accW = if (ksIsSharp) accH * 0.38f else accH * 0.32f
            val trebleSharpOffsets = floatArrayOf(0.0f, 1.5f, -0.5f, 1.0f, 2.5f, 0.5f, 2.0f) // F, C, G, D, A, E, B
            val bassSharpOffsets = floatArrayOf(1.0f, 2.5f, 0.5f, 2.0f, 3.5f, 1.5f, 3.0f)
            val trebleFlatOffsets = floatArrayOf(2.0f, 0.5f, 2.5f, 1.0f, 3.0f, 1.5f, 3.5f) // B, E, A, D, G, C, F
            val bassFlatOffsets = floatArrayOf(3.0f, 1.5f, 3.5f, 2.0f, 4.0f, 2.5f, 4.5f)

            with(MusicVectorSymbols) {
                for (i in 0 until minOf(ksCount, 7)) {
                    val kX = ksStartX + (i * ksStepX)
                    if (ksIsSharp) {
                        val tY = trebleBaseline + (trebleSharpOffsets[i] * config.lineSpacing)
                        val bY = bassBaseline + (bassSharpOffsets[i] * config.lineSpacing)
                        if (!config.isRightHandHidden) drawSharp(clefTextColor, Size(accW, accH), Offset(kX, tY - accH / 2f))
                        if (!config.isLeftHandHidden) drawSharp(clefTextColor, Size(accW, accH), Offset(kX, bY - accH / 2f))
                    } else {
                        val tY = trebleBaseline + (trebleFlatOffsets[i] * config.lineSpacing)
                        val bY = bassBaseline + (bassFlatOffsets[i] * config.lineSpacing)
                        if (!config.isRightHandHidden) drawFlat(clefTextColor, Size(accW, accH), Offset(kX, tY - (accH * 0.72f)))
                        if (!config.isLeftHandHidden) drawFlat(clefTextColor, Size(accW, accH), Offset(kX, bY - (accH * 0.72f)))
                    }
                }
            }
        }

        if (drawTimeHeader) {
            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                val tsX = if (ksCount > 0) ksStartX + (minOf(ksCount, 7) * ksStepX) + 6f else startX + maxClefW + 10f
                with(MusicVectorSymbols) {
                    if (!config.isRightHandHidden) {
                        drawTimeSignature(
                            numerator = tsParts[0],
                            denominator = tsParts[1],
                            color = clefTextColor,
                            topLeft = Offset(tsX, trebleBaseline),
                            lineSpacing = config.lineSpacing
                        )
                    }
                    if (!config.isLeftHandHidden) {
                        drawTimeSignature(
                            numerator = tsParts[0],
                            denominator = tsParts[1],
                            color = clefTextColor,
                            topLeft = Offset(tsX, bassBaseline),
                            lineSpacing = config.lineSpacing
                        )
                    }
                }
            }
        }

        val layout = staffLayout ?: computeStaffLayout(
            measure = measure,
            drawClefHeader = drawClefHeader,
            drawKeyHeader = drawKeyHeader,
            drawTimeHeader = drawTimeHeader,
            startX = startX,
            endX = endX,
            trebleBaseline = trebleBaseline,
            bassBaseline = bassBaseline,
            lineSpacing = config.lineSpacing,
            isLeftHandHidden = config.isLeftHandHidden,
            isRightHandHidden = config.isRightHandHidden
        )

        val visibleNotes = layout.visibleNotes
        val noteBeamMap = layout.noteBeamMap
        val beamGroups = layout.beamGroups
        val trebleRests = layout.trebleRests
        val bassRests = layout.bassRests
        val usableMeasureWidth = layout.usableMeasureWidth
        val notesStartX = layout.notesStartX
        val beatsPerMeasure = layout.beatsPerMeasure

        val noteScale = config.lineSpacing / 12f
        val stdStemOffset = (15f * noteScale * 0.44f).coerceIn(5.2f, 7.5f)

        // 1. Draw Unified Stems and Flags per (position, clef, voice) chord cluster so chord notes share one clean stem
        val nonGraceNotes = visibleNotes.filter { !it.note.grace }
        val chordClusters = nonGraceNotes.groupBy { "${it.note.position}_${it.isTrebleClef}_${it.note.voice}" }

        for ((_, cluster) in chordClusters) {
            val sample = cluster.first()
            val maxDur = cluster.maxOf { it.note.duration }
            if (maxDur >= 4.0f) continue // Whole/Breve notes have no stems

            val beamInfo = cluster.firstNotNullOfOrNull { noteBeamMap[it.note] }
            val avgMidi = cluster.map { it.midiNote }.average()
            val stemDir = beamInfo?.stemDir ?: sample.note.stemUp?.let { if (it) -1f else 1f }
                ?: if (sample.isTrebleClef) (if (avgMidi >= 71) 1f else -1f) else (if (avgMidi >= 50) 1f else -1f)

            val anchorX = sample.anchorX
            val stemX = if (stemDir > 0) anchorX - stdStemOffset else anchorX + stdStemOffset

            val minNy = cluster.minOf { it.ny }
            val maxNy = cluster.maxOf { it.ny }
            val stemStartNoteY = if (stemDir > 0) minNy else maxNy
            val extremeTipNoteY = if (stemDir > 0) maxNy else minNy
            val defaultStemLen = (config.lineSpacing * 2.35f).coerceAtLeast(25f)

            val stemEndY = if (beamInfo != null) {
                beamInfo.noteStemYMap[sample.note] ?: (extremeTipNoteY + (stemDir * defaultStemLen))
            } else {
                extremeTipNoteY + (stemDir * defaultStemLen)
            }

            drawLine(clefTextColor, Offset(stemX, stemStartNoteY), Offset(stemX, stemEndY), strokeWidth = 1.35f)

            // Draw flag ONLY for isolated non-beamed notes shorter than a quarter note (< 0.90f)
            val minDur = cluster.minOf { it.note.duration }
            if (minDur < 0.90f && beamInfo == null) {
                with(MusicVectorSymbols) {
                    val flagH = 22f * (config.lineSpacing / 14f).coerceAtLeast(0.8f)
                    val flagW = 10f * (config.lineSpacing / 14f).coerceAtLeast(0.8f)
                    val flagY = if (stemDir > 0) stemEndY - flagH else stemEndY
                    when {
                        minDur <= 0.08f -> drawSmuflSymbol(if (stemDir > 0) "flag64thDown" else "flag64thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.5f), 800f)
                        minDur <= 0.16f -> drawSmuflSymbol(if (stemDir > 0) "flag32ndDown" else "flag32ndUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.3f), 800f)
                        minDur <= 0.35f -> drawSmuflSymbol(if (stemDir > 0) "flag16thDown" else "flag16thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.15f), 800f)
                        else -> drawSmuflSymbol(if (stemDir > 0) "flag8thDown" else "flag8thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH), 800f)
                    }
                }
            }
        }

        // Intra-measure accidental state tracker: maps diatonicStep -> last active semitone alteration (-2..+2)
        // Initialized from the Measure's KeySignature so redundant sharps/flats are suppressed and natural cancellations are automatic
        val activeStepAlteration = mutableMapOf<Int, Int>()
        val keySig = measure.keySignature
        val sharpAlterSteps = intArrayOf(3, 0, 4, 1, 5, 2, 6) // F, C, G, D, A, E, B (step % 7)
        val flatAlterSteps = intArrayOf(6, 2, 5, 1, 4, 0, 3)  // B, E, A, D, G, C, F (step % 7)
        val alteredPitchClassesInKey = mutableSetOf<Int>()
        for (i in 0 until minOf(keySig.accidentalsCount, 7)) {
            if (keySig.isSharp) alteredPitchClassesInKey.add(sharpAlterSteps[i])
            else alteredPitchClassesInKey.add(flatAlterSteps[i])
        }

        // 2. Render each Notehead, Ledger Lines, Accidentals, Articulations, and Labels
        visibleNotes.forEach { info ->
            val note = info.note
            val midiNote = info.midiNote
            val isTrebleClef = info.isTrebleClef
            val nx = info.nx
            val ny = info.ny
            val topY = info.topY
            val bottomY = info.bottomY

            val isSounding = isNoteTemporallyActive(
                note = note,
                isMeasureActive = isMeasureActive,
                playheadOffset = playheadOffset,
                beatsPerMeasure = beatsPerMeasure,
                playingNote = playingNote,
                activeMidiNotes = activeMidiNotes
            )
            val beadColor = if (isSounding) config.primaryColor else if (isTrebleClef) config.rightHandColor else config.leftHandColor

            drawLedgerLines(
                x = nx,
                y = ny,
                topY = topY,
                bottomY = bottomY,
                spacing = config.lineSpacing,
                color = staffLineColor,
                handAccent = beadColor
            )

            val beamInfo = noteBeamMap[note]
            val stemDir = beamInfo?.stemDir ?: note.stemUp?.let { if (it) -1f else 1f }
                ?: if (isTrebleClef) (if (midiNote >= 71) 1f else -1f) else (if (midiNote >= 50) 1f else -1f)

            val duration = note.duration
            val graceFactor = if (note.grace) 0.68f else 1.0f
            val noteWidth = (when {
                duration >= 8.0f -> 22f * noteScale
                duration >= 4.0f -> 20f * noteScale
                else -> 16f * noteScale
            }) * graceFactor
            val noteHeight = 11.5f * noteScale * graceFactor

            // Grace note stem + acciaccatura slash
            if (note.grace) {
                val gStemX = nx + (noteWidth * 0.42f)
                val gStemTopY = ny - 16f
                drawLine(clefTextColor, Offset(gStemX, ny), Offset(gStemX, gStemTopY), strokeWidth = 1.1f)
                drawLine(clefTextColor, Offset(gStemX, gStemTopY), Offset(gStemX + 5f, gStemTopY + 5f), strokeWidth = 1.2f)
                drawLine(clefTextColor, Offset(gStemX - 4f, ny - 6f), Offset(gStemX + 5f, ny - 12f), strokeWidth = 1.1f)
            }

            // Active Note Soft Glow Halo
            if (isSounding) {
                drawCircle(
                    color = config.primaryColor.copy(alpha = 0.25f),
                    radius = noteWidth * 0.72f,
                    center = Offset(nx, ny)
                )
            }

            with(MusicVectorSymbols) {
                when {
                    duration >= 8.0f -> {
                        drawSmuflSymbol("noteheadDoubleWhole", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    }
                    note.harmonic != null -> {
                        drawSmuflSymbol("noteheadDiamondBlack", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    }
                    note.articulation == TabArticulation.DEAD_NOTE -> {
                        drawSmuflSymbol("noteheadXBlack", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    }
                    duration >= 4.0f -> {
                        drawNoteHead(
                            color = beadColor,
                            size = Size(noteWidth, noteHeight),
                            isHollow = true,
                            isWhole = true,
                            topLeft = Offset(nx - noteWidth / 2f, ny - noteHeight / 2f)
                        )
                    }
                    duration >= 2.0f -> {
                        drawNoteHead(
                            color = beadColor,
                            size = Size(noteWidth, noteHeight),
                            isHollow = true,
                            isWhole = false,
                            topLeft = Offset(nx - noteWidth / 2f, ny - noteHeight / 2f)
                        )
                    }
                    else -> {
                        drawNoteHead(
                            color = beadColor,
                            size = Size(noteWidth, noteHeight),
                            isHollow = false,
                            isWhole = false,
                            topLeft = Offset(nx - noteWidth / 2f, ny - noteHeight / 2f)
                        )
                    }
                }

                // Augmentation Dot (shifted off staff line into upper space if note sits directly on a line)
                if (note.dotted || note.isDotted) {
                    val dotX = nx + (noteWidth / 2f) + 4.5f
                    val isOnStaffLine = (info.diatonicStep % 2 == 0)
                    val dotY = if (isOnStaffLine) ny - (config.lineSpacing * 0.28f) - 2.5f else ny - 2.5f
                    drawSmuflSymbol("augmentationDot", beadColor, Offset(dotX, dotY), Size(5f, 5f), 200f)
                }

                // Musical Articulations placed on notehead side opposite the stem
                val articOppositeStemY = if (stemDir > 0) ny - noteHeight - 6f else ny + noteHeight + 3f
                when (note.musicalArticulation) {
                    MusicalArticulation.STACCATO -> drawStaccato(clefTextColor, Offset(nx, articOppositeStemY), radius = 2.2f)
                    MusicalArticulation.STACCATISSIMO -> drawStaccato(clefTextColor, Offset(nx, articOppositeStemY), radius = 1.4f)
                    MusicalArticulation.ACCENT -> drawAccent(clefTextColor, Size(12f, 8f), Offset(nx - 6f, articOppositeStemY - 4f))
                    MusicalArticulation.TENUTO -> drawTenuto(clefTextColor, Size(12f, 3f), Offset(nx - 6f, articOppositeStemY - 1.5f))
                    MusicalArticulation.MARCATO -> drawSmuflSymbol("articMarcatoAbove", clefTextColor, Offset(nx - 5f, ny - noteHeight - 12f), Size(10f, 10f), 400f)
                    MusicalArticulation.NONE -> {}
                }

                // Composite & Standard Dynamics (pp, p, mp, mf, f, ff, sfz, rfz)
                if (note.dynamic.isNotEmpty()) {
                    val dynY = bassBaseline + 4.3f * config.lineSpacing
                    val dynLower = note.dynamic.lowercase().trim()
                    when (dynLower) {
                        "p" -> drawDynamic("dynamicPiano", clefTextColor.copy(alpha = 0.85f), Size(12f, 14f), Offset(nx - 6f, dynY))
                        "f" -> drawDynamic("dynamicForte", clefTextColor.copy(alpha = 0.85f), Size(12f, 14f), Offset(nx - 6f, dynY))
                        "sfz" -> drawDynamic("dynamicSforzando", clefTextColor.copy(alpha = 0.85f), Size(16f, 14f), Offset(nx - 8f, dynY))
                        "rfz" -> drawDynamic("dynamicRinforzando", clefTextColor.copy(alpha = 0.85f), Size(16f, 14f), Offset(nx - 8f, dynY))
                        else -> {
                            val dynLayout = textMeasurer.measure(
                                text = dynLower,
                                style = TextStyle(
                                    color = clefTextColor.copy(alpha = 0.88f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                            drawText(dynLayout, topLeft = Offset(nx - dynLayout.size.width / 2f, dynY - 2f))
                        }
                    }
                }
            }

            // Chord symbol above treble staff
            if (note.chordSymbol.isNotEmpty()) {
                val chordLayout = textMeasurer.measure(
                    text = note.chordSymbol,
                    style = TextStyle(color = config.primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                drawText(chordLayout, topLeft = Offset(nx - chordLayout.size.width / 2f, trebleBaseline - 22f))
            }

            // Fingering number above/below note
            if (note.fingering != null && note.fingering in 1..5) {
                val fLayout = textMeasurer.measure(
                    text = note.fingering.toString(),
                    style = TextStyle(color = clefTextColor.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                )
                val fy = if (isTrebleClef) minOf(ny - 18f, topY - 12f) else maxOf(ny + 8f, bottomY + 4f)
                drawText(fLayout, topLeft = Offset(nx - fLayout.size.width / 2f, fy))
            }

            if (config.showLabels && !note.grace && duration < 2.0f) {
                val labelResult = labelCache[midiNote % 12]
                if (labelResult != null) {
                    val labelX = nx - (labelResult.size.width / 2f)
                    val labelY = ny - (labelResult.size.height / 2f)
                    drawText(labelResult, topLeft = Offset(labelX, labelY))
                }
            }

            if (config.showLyrics && note.lyric.isNotEmpty()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, bassBaseline + 4.8f * config.lineSpacing + 14f))
            }

            // Accidentals with Intra-Measure Memory & Key-Signature Awareness
            val label = noteLabels[midiNote % 12]
            val stepMod7 = ((info.diatonicStep % 7) + 7) % 7
            val defaultKeyAlt = if (stepMod7 in alteredPitchClassesInKey) (if (keySig.isSharp) 1 else -1) else 0
            val currentStepState = activeStepAlteration.getOrElse(info.diatonicStep) { defaultKeyAlt }

            val noteAlteration = when {
                note.accidental == Accidental.DOUBLE_SHARP -> 2
                note.accidental == Accidental.DOUBLE_FLAT -> -2
                note.accidental == Accidental.SHARP || label.contains("#") -> 1
                note.accidental == Accidental.FLAT || label.contains("b") -> -1
                else -> 0
            }

            val shouldDrawAccidental = (noteAlteration != currentStepState) || note.courtesyAccidental
            activeStepAlteration[info.diatonicStep] = noteAlteration

            if (shouldDrawAccidental) {
                val isDoubleSharp = noteAlteration == 2
                val isDoubleFlat = noteAlteration == -2
                val isSharp = noteAlteration == 1
                val isNatural = noteAlteration == 0
                val accH = config.lineSpacing * 1.35f
                val accW = when {
                    isDoubleSharp || isDoubleFlat -> accH * 0.44f
                    isSharp -> accH * 0.38f
                    isNatural -> accH * 0.28f
                    else -> accH * 0.32f
                }

                val colShift = info.accidentalColumn * (accW + 2.5f)
                val accRightEdge = info.anchorX - (noteWidth / 2f) - 3.5f - colShift
                val accLeftX = accRightEdge - accW
                val accidentalColor = clefTextColor

                with(MusicVectorSymbols) {
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol("accidentalParensLeft", accidentalColor.copy(alpha = 0.6f), Offset(accLeftX - 6f, ny - accH / 2f), Size(4.5f, accH), 800f)
                    }
                    when {
                        isDoubleSharp -> drawDoubleSharp(accidentalColor, Size(accW, accH), Offset(accLeftX, ny - accH / 2f))
                        isDoubleFlat -> drawDoubleFlat(accidentalColor, Size(accW, accH), Offset(accLeftX, ny - (accH * 0.72f)))
                        isSharp -> drawSharp(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - accH / 2f))
                        isNatural -> drawNatural(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - accH / 2f))
                        else -> drawFlat(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - (accH * 0.72f)))
                    }
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol("accidentalParensRight", accidentalColor.copy(alpha = 0.6f), Offset(accRightEdge + 1f, ny - accH / 2f), Size(4.5f, accH), 800f)
                    }
                }
            }
        }

        // 3. Draw Ties and Slurs with accurate endpoint pairing and cross-barline handling
        val notesByClefAndVoice = visibleNotes.groupBy { "${it.isTrebleClef}_${it.note.voice}" }
        for ((_, lineNotes) in notesByClefAndVoice) {
            val sortedLine = lineNotes.sortedWith(compareBy({ it.note.position }, { it.midiNote }))

            sortedLine.forEachIndexed { idx, src ->
                val note = src.note
                val stemDir = noteBeamMap[note]?.stemDir
                    ?: if (src.isTrebleClef) (if (src.midiNote >= 71) 1f else -1f) else (if (src.midiNote >= 50) 1f else -1f)

                // A. Ties (Same pitch connection)
                if (note.tieStart) {
                    val targetSamePitch = sortedLine.drop(idx + 1).firstOrNull {
                        it.midiNote == src.midiNote && it.note.position > note.position + 0.01f
                    }
                    val tieCurveDir = if (stemDir > 0) -1f else 1f
                    val yOffset = tieCurveDir * (config.lineSpacing * 0.45f)
                    val startTieX = src.nx + 5f
                    val endTieX = targetSamePitch?.let { it.nx - 5f } ?: (endX - 3f)
                    if (endTieX > startTieX + 4f) {
                        drawEngravedCurve(
                            startX = startTieX,
                            startY = src.ny + yOffset,
                            endX = endTieX,
                            endY = (targetSamePitch?.ny ?: src.ny) + yOffset,
                            direction = tieCurveDir,
                            color = clefTextColor
                        )
                    }
                }

                // Incoming Tie from previous measure
                if (note.tieEnd && !note.tieStart) {
                    val hasEarlierTieStartInMeasure = sortedLine.take(idx).any {
                        it.midiNote == src.midiNote && it.note.tieStart
                    }
                    if (!hasEarlierTieStartInMeasure) {
                        val tieCurveDir = if (stemDir > 0) -1f else 1f
                        val yOffset = tieCurveDir * (config.lineSpacing * 0.45f)
                        val incomingStartX = (notesStartX + 2f).coerceAtMost(src.nx - 12f)
                        drawEngravedCurve(
                            startX = incomingStartX,
                            startY = src.ny + yOffset,
                            endX = src.nx - 5f,
                            endY = src.ny + yOffset,
                            direction = tieCurveDir,
                            color = clefTextColor
                        )
                    }
                }

                // B. Slurs (Phrase connection across different pitches or grace -> main note)
                if (note.slurStart || note.grace) {
                    val slurTarget = if (note.grace) {
                        sortedLine.drop(idx + 1).firstOrNull { !it.note.grace && it.note.position >= note.position }
                    } else {
                        sortedLine.drop(idx + 1).firstOrNull { it.note.slurEnd && it.note.position > note.position }
                            ?: sortedLine.drop(idx + 1).firstOrNull { it.note.position > note.position + 0.01f }
                    }

                    val slurCurveDir = if (note.grace) 1f else (if (stemDir > 0) -1f else 1f)
                    val yStartOff = slurCurveDir * (config.lineSpacing * 0.6f)
                    val sX = src.nx + 4f
                    val eX = slurTarget?.let { it.nx - 4f } ?: (endX - 4f)
                    val eY = (slurTarget?.ny ?: src.ny) + yStartOff
                    if (eX > sX + 4f) {
                        drawEngravedCurve(
                            startX = sX,
                            startY = src.ny + yStartOff,
                            endX = eX,
                            endY = eY,
                            direction = slurCurveDir,
                            color = clefTextColor.copy(alpha = 0.9f)
                        )
                    }
                }

                // Incoming Cross-Barline Slur (slurEnd in this measure with no preceding slurStart in this measure)
                if (note.slurEnd && !note.slurStart) {
                    val hasEarlierSlurStart = sortedLine.take(idx).any { it.note.slurStart }
                    if (!hasEarlierSlurStart) {
                        val slurCurveDir = if (stemDir > 0) -1f else 1f
                        val yOff = slurCurveDir * (config.lineSpacing * 0.6f)
                        val incomingStartX = (notesStartX + 2f).coerceAtMost(src.nx - 14f)
                        drawEngravedCurve(
                            startX = incomingStartX,
                            startY = src.ny + yOff,
                            endX = src.nx - 4f,
                            endY = src.ny + yOff,
                            direction = slurCurveDir,
                            color = clefTextColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 4. Draw Connected Sloped Beams (Primary 8th, Secondary 16th, Tertiary 32nd, and Beamlets)
        val beamThickness = 3.8f
        beamGroups.forEach { beam ->
            val isBeamActive = beam.notes.any { bn ->
                isNoteTemporallyActive(
                    note = bn,
                    isMeasureActive = isMeasureActive,
                    playheadOffset = playheadOffset,
                    beatsPerMeasure = beatsPerMeasure,
                    playingNote = playingNote,
                    activeMidiNotes = activeMidiNotes
                )
            }
            val activeBeamColor = if (isBeamActive) config.primaryColor else clefTextColor

            if (isBeamActive) {
                drawLine(
                    color = config.primaryColor.copy(alpha = 0.25f),
                    start = Offset(beam.firstX - 2f, beam.startY),
                    end = Offset(beam.lastX + 2f, beam.endY),
                    strokeWidth = beamThickness + 4.5f
                )
            }

            // Primary 8th Beam
            drawLine(
                color = activeBeamColor,
                start = Offset(beam.firstX, beam.startY),
                end = Offset(beam.lastX, beam.endY),
                strokeWidth = beamThickness
            )

            // Secondary 16th Beam Segments
            beam.secondarySegments.forEach { (sStart, sEnd) ->
                drawLine(
                    color = activeBeamColor,
                    start = sStart,
                    end = sEnd,
                    strokeWidth = beamThickness * 0.9f
                )
            }

            // Tertiary 32nd Beam Segments
            beam.tertiarySegments.forEach { (tStart, tEnd) ->
                drawLine(
                    color = activeBeamColor,
                    start = tStart,
                    end = tEnd,
                    strokeWidth = beamThickness * 0.85f
                )
            }

            // Fractional 16th/32nd Beamlets
            beam.beamlets.forEach { (bStart, bEnd) ->
                drawLine(
                    color = activeBeamColor,
                    start = bStart,
                    end = bEnd,
                    strokeWidth = beamThickness * 0.9f
                )
            }
        }

        // 5. Draw Tuplet Numbers & Brackets (e.g. Triplets "3")
        val tupletNotes = visibleNotes.filter { it.note.tuplet > 0 }
        if (tupletNotes.isNotEmpty()) {
            val tupletBeats = tupletNotes.groupBy { "${it.isTrebleClef}_${it.beatIndex}_${it.note.tuplet}" }
            for ((_, tGroup) in tupletBeats) {
                val tupletVal = tGroup.first().note.tuplet
                val minX = tGroup.minOf { it.nx }
                val maxX = tGroup.maxOf { it.nx }
                val minY = tGroup.minOf { it.ny }
                val tLayout = textMeasurer.measure(
                    text = tupletVal.toString(),
                    style = TextStyle(color = clefTextColor, fontSize = 9.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                )
                val midX = (minX + maxX) / 2f
                val bracketY = (minY - 26f).coerceAtMost(tGroup.first().topY - 10f)
                drawText(tLayout, topLeft = Offset(midX - tLayout.size.width / 2f, bracketY - 6f))
                if (maxX - minX > 20f) {
                    drawLine(clefTextColor.copy(alpha = 0.7f), Offset(minX - 3f, bracketY), Offset(midX - 8f, bracketY), strokeWidth = 1.1f)
                    drawLine(clefTextColor.copy(alpha = 0.7f), Offset(midX + 8f, bracketY), Offset(maxX + 3f, bracketY), strokeWidth = 1.1f)
                    drawLine(clefTextColor.copy(alpha = 0.7f), Offset(minX - 3f, bracketY), Offset(minX - 3f, bracketY + 4f), strokeWidth = 1.1f)
                    drawLine(clefTextColor.copy(alpha = 0.7f), Offset(maxX + 3f, bracketY), Offset(maxX + 3f, bracketY + 4f), strokeWidth = 1.1f)
                }
            }
        }

        // 6. Render Rests using precomputed collections
        (trebleRests + bassRests).forEach { rest ->
            val isTreble = trebleRests.contains(rest)
            val rx = layout.computedXByPosition[rest.position]
                ?: snap(notesStartX + 14f + ((usableMeasureWidth - 28f) * (rest.position / beatsPerMeasure)))
            val staffBase = if (isTreble) config.trebleBaseline else config.bassBaseline
            val spacing = config.lineSpacing
            val restScale = spacing / 14f
            val restColor = clefTextColor.copy(alpha = 0.75f)

            with(MusicVectorSymbols) {
                when {
                    rest.duration >= 8.0f -> {
                        val rw = 12f * restScale
                        val rh = spacing
                        drawRestDoubleWhole(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + spacing))
                    }
                    rest.duration >= 3.5f -> {
                        val rw = 16f * restScale
                        val rh = spacing * 0.95f
                        val centerMeasureX = snap((notesStartX + endX) / 2f)
                        val wholeRestX = if (rest.position == 0f && (isTreble && trebleRests.size == 1 || !isTreble && bassRests.size == 1)) centerMeasureX else rx
                        drawRestWhole(restColor, Size(rw, rh), topLeft = Offset(wholeRestX - rw / 2f, staffBase + spacing))
                    }
                    rest.duration >= 1.75f -> {
                        val rw = 16f * restScale
                        val rh = spacing * 0.95f
                        drawRestHalf(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + spacing))
                    }
                    rest.duration >= 0.85f -> {
                        val rw = 12f * restScale
                        val rh = spacing * 2.8f
                        drawRestQuarter(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (2f * spacing) - (rh / 2f)))
                    }
                    rest.duration >= 0.4f -> {
                        val rw = 11f * restScale
                        val rh = spacing * 2.0f
                        drawRestEighth(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (1.5f * spacing)))
                    }
                    rest.duration >= 0.2f -> {
                        val rw = 13f * restScale
                        val rh = spacing * 2.8f
                        drawRest16th(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (1.2f * spacing)))
                    }
                    rest.duration >= 0.1f -> {
                        val rw = 14f * restScale
                        val rh = spacing * 3.4f
                        drawRest32nd(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (0.9f * spacing)))
                    }
                    else -> {
                        val rw = 15f * restScale
                        val rh = spacing * 4.0f
                        drawRest64th(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (0.7f * spacing)))
                    }
                }
            }
        }

        // 7. Navigation, Pedal Events & Measure Marks
        if (measure.segno) {
            with(MusicVectorSymbols) {
                drawSegno(config.primaryColor, Size(20f, 20f), topLeft = Offset(startX + 5f, config.trebleBaseline - 40f))
            }
        }
        if (measure.coda) {
            with(MusicVectorSymbols) {
                drawCoda(config.primaryColor, Size(20f, 20f), topLeft = Offset(startX + 28f, config.trebleBaseline - 40f))
            }
        }
        if (measure.dalSegno || measure.daCapo || measure.toCoda || measure.fine) {
            val navText = when {
                measure.dalSegno -> "D.S. al Coda"
                measure.daCapo -> "D.C. al Fine"
                measure.toCoda -> "To Coda 𝄌"
                else -> "Fine"
            }
            val navLayout = textMeasurer.measure(
                text = navText,
                style = TextStyle(color = clefTextColor, fontSize = 9.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black)
            )
            drawText(navLayout, topLeft = Offset(endX - navLayout.size.width - 6f, config.trebleBaseline - 24f))
        }
        if (measure.rehearsalMark.isNotEmpty()) {
            val rmLayout = textMeasurer.measure(measure.rehearsalMark, TextStyle(color = config.primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            val rmW = rmLayout.size.width.toFloat() + 8f
            val rmH = rmLayout.size.height.toFloat() + 4f
            val rmX = startX + 6f
            val rmY = config.trebleBaseline - 46f
            drawRect(clefTextColor.copy(alpha = 0.15f), topLeft = Offset(rmX, rmY), size = Size(rmW, rmH))
            drawRect(config.primaryColor, topLeft = Offset(rmX, rmY), size = Size(rmW, rmH), style = Stroke(width = 1.2f))
            drawText(rmLayout, topLeft = Offset(rmX + 4f, rmY + 2f))
        }
        if (measure.endingNumber != null) {
            val endLayout = textMeasurer.measure("${measure.endingNumber}.", TextStyle(color = clefTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            val bY = config.trebleBaseline - 25f
            drawLine(clefTextColor, Offset(startX, bY), Offset(endX, bY), strokeWidth = 1.2f)
            drawLine(clefTextColor, Offset(startX, bY), Offset(startX, bY + 12f), strokeWidth = 1.2f)
            if (measure.repeatEnd) {
                drawLine(clefTextColor, Offset(endX, bY), Offset(endX, bY + 12f), strokeWidth = 1.2f)
            }
            drawText(endLayout, topLeft = Offset(startX + 6f, bY - 14f))
        }

        // Measure-level or Live Sustain Pedal markings
        if (measure.pedalEvents.isNotEmpty()) {
            val pedY = bassBaseline + 4.8f * config.lineSpacing + 4f
            measure.pedalEvents.forEach { ev ->
                val px = layout.computedXByPosition[ev.position]
                    ?: snap(notesStartX + (usableMeasureWidth * (ev.position / beatsPerMeasure)))
                if (ev.isPressed) {
                    with(MusicVectorSymbols) {
                        drawPedal(config.primaryColor.copy(alpha = 0.8f), Size(18f, 14f), Offset(px - 6f, pedY))
                    }
                } else {
                    val upLayout = textMeasurer.measure("*", TextStyle(color = clefTextColor.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    drawText(upLayout, topLeft = Offset(px - 3f, pedY))
                }
            }
        } else if (config.isPedalPressed && isMeasureActive) {
            with(MusicVectorSymbols) {
                drawPedal(config.primaryColor.copy(alpha = 0.8f), Size(18f, 14f), Offset(startX + 6f, bassBaseline + 5 * config.lineSpacing + 10f))
            }
        }

        if (playheadOffset != null) {
            drawPlayheadStaffLine(
                drawScope = this,
                visibleNotes = visibleNotes,
                computedXByPosition = layout.computedXByPosition,
                notesStartX = notesStartX,
                usableMeasureWidth = usableMeasureWidth,
                beatsPerMeasure = beatsPerMeasure,
                playingNote = playingNote,
                playheadOffset = playheadOffset,
                config = config
            )
        }
    }
}

internal fun drawPlayheadStaffLine(
    drawScope: DrawScope,
    visibleNotes: List<PrecomputedRenderNote>,
    computedXByPosition: Map<Float, Float>,
    notesStartX: Float,
    usableMeasureWidth: Float,
    beatsPerMeasure: Float,
    playingNote: Int?,
    playheadOffset: Float,
    config: DrawingConfig
) {
    with(drawScope) {
        val currentBeat = playheadOffset * beatsPerMeasure
        val matchingActiveNote = if (playingNote != null) {
            visibleNotes.firstOrNull {
                it.midiNote == playingNote &&
                    currentBeat >= (it.note.position - 0.08f) &&
                    currentBeat <= (it.note.position + it.note.duration.coerceAtLeast(0.125f) + 0.04f)
            }
        } else null

        val targetPlayheadX = if (matchingActiveNote != null && abs(currentBeat - matchingActiveNote.note.position) <= 0.12f) {
            matchingActiveNote.anchorX
        } else if (computedXByPosition.isNotEmpty()) {
            interpolatePlayheadX(computedXByPosition, currentBeat, notesStartX, usableMeasureWidth, playheadOffset)
        } else {
            notesStartX + (usableMeasureWidth * playheadOffset)
        }

        drawLine(
            color = config.primaryColor,
            start = Offset(targetPlayheadX, config.trebleBaseline - 15f),
            end = Offset(targetPlayheadX, config.bassBaseline + (4 * config.lineSpacing) + 15f),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

private fun DrawScope.drawGrandStaffSystem(
    ds: DrawScope,
    startX: Float,
    endX: Float,
    trebleY: Float,
    bassY: Float,
    spacing: Float,
    isFinal: Boolean,
    color: Color,
    drawLeftBar: Boolean = false,
    config: DrawingConfig
) {
    with(ds) {
        val lineColor = if (config.isDark) Color(0xFF6A6B72) else Color(0xFF9E9E9E)
        val strokeW = 1.3f

        if (config.isTab) {
            for (i in 0 until 6) {
                val y = trebleY + (i * spacing)
                drawLine(lineColor, Offset(startX, y), Offset(endX, y), strokeWidth = strokeW)
            }
            if (drawLeftBar) {
                drawLine(
                    color = lineColor,
                    start = Offset(startX, trebleY),
                    end = Offset(startX, trebleY + (5 * spacing)),
                    strokeWidth = 2.dp.toPx()
                )
            }
        } else {
            if (!config.isRightHandHidden) {
                for (i in 0 until 5) {
                    val y = trebleY + (i * spacing)
                    drawLine(lineColor, Offset(startX, y), Offset(endX, y), strokeWidth = strokeW)
                }
            }
            if (!config.isLeftHandHidden) {
                for (i in 0 until 5) {
                    val y = bassY + (i * spacing)
                    drawLine(lineColor, Offset(startX, y), Offset(endX, y), strokeWidth = strokeW)
                }
            }

            if (drawLeftBar) {
                val barTop = if (config.isRightHandHidden) bassY else trebleY
                val barBottom = if (config.isLeftHandHidden) trebleY + 4 * spacing else bassY + 4 * spacing

                if (!config.isLeftHandHidden || !config.isRightHandHidden) {
                    drawLine(
                        color = lineColor,
                        start = Offset(startX, barTop),
                        end = Offset(startX, barBottom),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}