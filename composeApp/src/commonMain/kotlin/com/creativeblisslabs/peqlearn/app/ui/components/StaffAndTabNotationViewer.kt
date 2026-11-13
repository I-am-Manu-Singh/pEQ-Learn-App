package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
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
    val lineBoundExt = spacing * 0.78f
    val halfSpacing = spacing / 2f
    val strokeW = (spacing * 0.10f).coerceIn(0.85f, 1.5f)
    val lineColor = if (handAccent != null) {
        Color(
            red = (color.red * 0.72f + handAccent.red * 0.28f),
            green = (color.green * 0.72f + handAccent.green * 0.28f),
            blue = (color.blue * 0.72f + handAccent.blue * 0.28f),
            alpha = 0.90f
        )
    } else {
        color.copy(alpha = 0.88f)
    }

    if (y < topY - halfSpacing * 0.85f) {
        var curL = topY - spacing
        var count = 0
        while (curL >= y - (halfSpacing * 0.60f) && count < 5) {
            drawLine(lineColor, Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = strokeW)
            curL -= spacing
            count++
        }
    } else if (y > bottomY + halfSpacing * 0.85f) {
        var curL = bottomY + spacing
        var count = 0
        while (curL <= y + (halfSpacing * 0.60f) && count < 5) {
            drawLine(lineColor, Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = strokeW)
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
    lineSpacing: Float = 12f,
    isDashed: Boolean = false
) {
    val scale = (lineSpacing / 12f).coerceIn(0.55f, 1.6f)
    val spanX = (endX - startX)
    // Never draw invalid or zero-length curves
    if (spanX < 4f * scale) return

    val dy = endY - startY
    val archHeight = ((3.8f * scale) + (spanX * 0.07f)).coerceIn(4.0f * scale, 12f * scale) * direction
    val thickness = (1.5f * scale) * direction

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
            style = Stroke(width = 1.1f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * scale, 3f * scale), 0f))
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
    hookDir: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.6f, 1.5f)
    val textLayout = textMeasurer.measure(
        text = label,
        style = TextStyle(color = color, fontSize = (7.2f * scale).sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
    )
    drawText(textLayout, topLeft = Offset(startX, y - textLayout.size.height / 2f))
    val lineStartX = startX + textLayout.size.width + 3f * scale
    if (endX > lineStartX + 4f * scale) {
        drawLine(
            color = color,
            start = Offset(lineStartX, y),
            end = Offset(endX, y),
            strokeWidth = 1.0f * scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * scale, 3f * scale), 0f)
        )
        drawLine(
            color = color,
            start = Offset(endX, y),
            end = Offset(endX, y + (hookDir * 4.0f * scale)),
            strokeWidth = 1.0f * scale
        )
    }
}

private fun DrawScope.drawFermataSymbol(centerX: Float, bottomY: Float, color: Color, lineSpacing: Float = 12f) {
    val scale = (lineSpacing / 12f).coerceIn(0.6f, 1.5f)
    val archW = 12f * scale
    val archH = 6f * scale
    val path = Path().apply {
        moveTo(centerX - archW / 2f, bottomY)
        cubicTo(
            centerX - archW / 2f, bottomY - archH,
            centerX + archW / 2f, bottomY - archH,
            centerX + archW / 2f, bottomY
        )
        cubicTo(
            centerX + archW / 2f, bottomY - archH + 1.8f * scale,
            centerX - archW / 2f, bottomY - archH + 1.8f * scale,
            centerX - archW / 2f, bottomY
        )
        close()
    }
    drawPath(path, color)
    drawCircle(color, radius = 1.6f * scale, center = Offset(centerX, bottomY - 1.8f * scale))
}

private fun DrawScope.drawArpeggioRoll(
    x: Float,
    topY: Float,
    bottomY: Float,
    direction: ArpeggioDirection,
    color: Color,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.6f, 1.5f)
    val startY = (topY - 3.0f * scale)
    val endY = (bottomY + 3.0f * scale)
    val step = 4.2f * scale
    val path = Path().apply {
        var curY = startY
        moveTo(x, curY)
        var toggle = 1f
        while (curY < endY) {
            val nextY = (curY + step).coerceAtMost(endY)
            quadraticBezierTo(x + (toggle * 2.4f * scale), (curY + nextY) / 2f, x, nextY)
            curY = nextY
            toggle = -toggle
        }
    }
    drawPath(path, color = color, style = Stroke(width = 1.1f * scale))
    if (direction == ArpeggioDirection.UP) {
        val arrow = Path().apply {
            moveTo(x, startY - 2.2f * scale)
            lineTo(x - 2.2f * scale, startY + 1.8f * scale)
            lineTo(x + 2.2f * scale, startY + 1.8f * scale)
            close()
        }
        drawPath(arrow, color)
    } else if (direction == ArpeggioDirection.DOWN) {
        val arrow = Path().apply {
            moveTo(x, endY + 2.2f * scale)
            lineTo(x - 2.2f * scale, endY - 1.8f * scale)
            lineTo(x + 2.2f * scale, endY - 1.8f * scale)
            close()
        }
        drawPath(arrow, color)
    }
}

private fun DrawScope.drawGrandStaffBrace(
    x: Float,
    topY: Float,
    bottomY: Float,
    color: Color,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.6f, 1.6f)
    val midY = (topY + bottomY) / 2f
    val h = (bottomY - topY).coerceAtLeast(20f)
    val w = 6.5f * scale
    val path = Path().apply {
        moveTo(x, topY)
        cubicTo(x - w, topY + h * 0.12f, x + 1f * scale, midY - h * 0.08f, x - w - 1.8f * scale, midY)
        cubicTo(x + 1f * scale, midY + h * 0.08f, x - w, bottomY - h * 0.12f, x, bottomY)
        cubicTo(x - w + 2.0f * scale, bottomY - h * 0.14f, x + 2.2f * scale, midY + h * 0.06f, x - w + 0.4f * scale, midY)
        cubicTo(x + 2.2f * scale, midY - h * 0.06f, x - w + 2.0f * scale, topY + h * 0.14f, x, topY)
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
        enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
        transposition: Int = 0
    ): PrecomputedStaffLayout {
        val tracksKey = enabledTrackIndices.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
        val key = "v2_${measure.measureIndex}_${drawClefHeader}_${drawKeyHeader}_${drawTimeHeader}_${startX.toInt()}_${endX.toInt()}_${trebleBaseline.toInt()}_${bassBaseline.toInt()}_${(lineSpacing * 10).toInt()}_${isLeftHandHidden}_${isRightHandHidden}_${measure.notes.size}_${transposition}_$tracksKey"
        return staffCache.getOrPut(key) {
            computeStaffLayout(
                measure, drawClefHeader, drawKeyHeader, drawTimeHeader,
                startX, endX, trebleBaseline, bassBaseline, lineSpacing,
                isLeftHandHidden, isRightHandHidden, enabledTrackIndices,
                transposition
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
        enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
        transposition: Int = 0
    ): PrecomputedTabLayout {
        val tracksKey = enabledTrackIndices.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
        val key = "v2_${measure.measureIndex}_${drawClefHeader}_${drawTimeHeader}_${startX.toInt()}_${endX.toInt()}_${tBase.toInt()}_${(lineSpacing * 10).toInt()}_${isLeftHandHidden}_${isRightHandHidden}_${measure.notes.size}_${capo}_${transposition}_$tracksKey"
        return tabCache.getOrPut(key) {
            computeTabLayout(
                measure, drawClefHeader, drawTimeHeader, startX, endX, tBase, lineSpacing,
                tuning, capo, isLeftHandHidden, isRightHandHidden, enabledTrackIndices,
                transposition
            )
        }
    }

    fun clear() {
        staffCache.clear()
        tabCache.clear()
    }
}

private fun defaultStepAlterationInKey(diatonicStepMod7: Int, keySig: KeySignature): Int {
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

/**
 * Assigns each note to Treble or Bass clef using a strict pitch split at Middle C (MIDI 60)
 * when both staves are visible. This prevents `voice == 0` notes below Middle C (like C3..B3)
 * from hanging below the Treble staff and colliding with `A3..C4` notes on the Bass staff!
 */
private fun resolveClefForNote(
    note: ScoreNote,
    midiNote: Int,
    hasDifferentiatedVoices: Boolean,
    isLeftHandHidden: Boolean,
    isRightHandHidden: Boolean
): Boolean {
    if (isLeftHandHidden) return true
    if (isRightHandHidden) return false

    // Strict register split around Middle C (MIDI 60):
    // Notes < 59 (B3 and below) ALWAYS go on Bass Clef; Notes >= 60 (C4 and above) ALWAYS go on Treble Clef!
    // This completely eliminates Treble notes and Bass notes overlapping in the inter-staff gap!
    return midiNote >= 60
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
    enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
    transposition: Int = 0
): PrecomputedStaffLayout {
    val ksCount = measure.keySignature.accidentalsCount

  val clefW = if (drawClefHeader) 3.25f * lineSpacing else 0f
    val ksW = if (drawKeyHeader && ksCount > 0) (minOf(ksCount, 7) * 0.68f * lineSpacing) + (0.55f * lineSpacing) else 0f
    val tsW = if (drawTimeHeader) 2.35f * lineSpacing else 0f
    val symbolAreaWidth = clefW + ksW + tsW + (if (drawClefHeader || drawKeyHeader || drawTimeHeader) 0.95f * lineSpacing else 0.45f * lineSpacing)
    val notesStartX = startX + symbolAreaWidth
    val rightPad = 1.35f * lineSpacing
    // Reserve extra left padding so accidentals, grace notes, and beam boxes at Beat 0 never touch the Time/Key/Clef header!
    val hasAccidentalOnFirstBeat = measure.notes.any { !it.isRest && it.position <= 0.05f && (it.accidental != Accidental.NONE || it.grace) }
    val leftNotePad = if (hasAccidentalOnFirstBeat) 2.15f * lineSpacing else 1.45f * lineSpacing
    val usableMeasureWidth = (endX - notesStartX - rightPad).coerceAtLeast(3.5f * lineSpacing)

    val nominalBeats = (measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.toFloat().coerceAtLeast(1f))).coerceAtLeast(1f)
    val maxNoteReach = measure.notes.maxOfOrNull { it.position + it.duration } ?: nominalBeats
    val beatsPerMeasure = if (measure.isPickup && maxNoteReach < nominalBeats) maxNoteReach.coerceAtLeast(0.5f) else nominalBeats
    val halfLineSpacing = lineSpacing / 2f

    val hasDifferentiatedVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }
    val rawVisibleNotes = measure.notes.filter { !it.isRest }
        .filter { note -> enabledTrackIndices[note.voice] ?: true }
        .distinctBy { "${it.position}_${it.midiNote}_${it.grace}" } // Deduplicate identical pitch at same position
        .mapNotNull { note ->
            val midiNote = (note.midiNote + transposition).coerceIn(24, 103)
            val isTrebleClef = resolveClefForNote(
                note = note,
                midiNote = midiNote,
                hasDifferentiatedVoices = hasDifferentiatedVoices,
                isLeftHandHidden = isLeftHandHidden,
                isRightHandHidden = isRightHandHidden
            )
            if (isTrebleClef && isRightHandHidden) return@mapNotNull null
            if (!isTrebleClef && isLeftHandHidden) return@mapNotNull null
            Triple(note, midiNote, isTrebleClef)
        }.sortedWith(compareBy({ it.first.position }, { !it.first.grace }, { it.second }))

    val nonGracePositions = rawVisibleNotes.filter { !it.first.grace }.map { it.first.position }.distinct().sorted()
    val minNoteGap = 1.85f * lineSpacing
    val computedXByPosition = mutableMapOf<Float, Float>()

    if (nonGracePositions.isNotEmpty()) {
        val minDur = rawVisibleNotes.filter { !it.first.grace }.minOfOrNull { it.first.duration.coerceAtLeast(0.125f) } ?: 1.0f
        val posDurations = nonGracePositions.mapIndexed { idx, pos ->
            val nextPos = if (idx < nonGracePositions.lastIndex) nonGracePositions[idx + 1] else beatsPerMeasure
            val delta = (nextPos - pos).coerceAtLeast(minDur)
            (delta / minDur).toDouble().pow(0.52).toFloat()
        }
        val sumWeighted = posDurations.sum().coerceAtLeast(1f)
        val noteContentStartX = notesStartX + leftNotePad
        val noteContentMaxX = (endX - rightPad).coerceAtLeast(noteContentStartX + lineSpacing)
        val maxAvailableSpan = (noteContentMaxX - noteContentStartX).coerceAtLeast(2.0f * lineSpacing)

        val naturalSpacingNeeded = (nonGracePositions.size - 1) * minNoteGap
        val effectiveGap = if (naturalSpacingNeeded > maxAvailableSpan && nonGracePositions.size > 1) {
            maxAvailableSpan / nonGracePositions.size.toFloat()
        } else {
            minNoteGap
        }

        var runningX = noteContentStartX
        var runningWeight = 0f
        nonGracePositions.forEachIndexed { idx, pos ->
            val fraction = (runningWeight / sumWeighted).coerceIn(0f, 0.94f)
            val idealX = noteContentStartX + (maxAvailableSpan * fraction)
            val targetX = if (idx == 0) idealX else maxOf(idealX, runningX + effectiveGap)
            val clampedX = targetX.coerceIn(noteContentStartX, noteContentMaxX)
            computedXByPosition[pos] = clampedX
            runningX = clampedX
            runningWeight += posDurations[idx]
        }
    }

    val stdNoteWidth = 1.14f * lineSpacing

    val activeStepAlteration = mutableMapOf<Int, Int>()
    val naturalCScaleSemitones = intArrayOf(0, 2, 4, 5, 7, 9, 11)

    val initialNotes = rawVisibleNotes.map { (note, midiNote, isTrebleClef) ->
        val baseNx = computedXByPosition[note.position]
            ?: (notesStartX + leftNotePad + ((usableMeasureWidth - leftNotePad) * (note.position / beatsPerMeasure))).coerceIn(notesStartX + leftNotePad, endX - rightPad)
        val anchorX = if (note.grace) (baseNx - 1.05f * lineSpacing).coerceAtLeast(notesStartX + 0.35f * lineSpacing) else baseNx

      // FIXED: Preserve accurate diatonic pitch for deep bass (E1..B1) and high treble (A6..C8)
        // by applying automatic 8vb/8va display folding instead of clamping pitches to C2 (MIDI 36)!
        val rawShiftedMidi = when (note.octaveShift) {
            OctaveShift.OTTAVA_ALTA_8VA -> midiNote - 12
            OctaveShift.QUINDICESIMA_ALTA_15MA -> midiNote - 24
            OctaveShift.OTTAVA_BASSA_8VB -> midiNote + 12
            OctaveShift.QUINDICESIMA_BASSA_15MB -> midiNote + 24
            OctaveShift.NONE -> {
                if (!isTrebleClef && midiNote < 33) midiNote + 12 // Fold E1..G#1 up 1 octave (8vb) so pitch class is preserved!
                else if (isTrebleClef && midiNote > 96) midiNote - 12
                else midiNote
            }
        }
        val shiftedMidiForDisplay = if (isTrebleClef) {
            rawShiftedMidi.coerceIn(55, 96)
        } else {
            rawShiftedMidi.coerceIn(33, 64)
        }

        val diatonicStep = ChordVoicingEngine.getDiatonicStep(shiftedMidiForDisplay)
        val ny = if (isTrebleClef) {
            trebleBaseline + (38.0f - diatonicStep) * halfLineSpacing
        } else {
            bassBaseline + (26.0f - diatonicStep) * halfLineSpacing
        }
        val topY = if (isTrebleClef) trebleBaseline else bassBaseline
        val bottomY = topY + (4 * lineSpacing)
        val beatIdx = floor(note.position).toInt()

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
            null
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

        // Strict Outward/Non-Colliding Stem Directions:
        // - Treble notes below B4 (MIDI 71) point UP (-1f) so stems NEVER hang into the inter-staff gap!
        // - Bass notes below B2 (MIDI 47) point UP (-1f) so stems NEVER hang into the TAB staff below!
        // - Bass notes >= D3 (MIDI 50) point DOWN (+1f) so stems NEVER poke up into the inter-staff gap!
        val safeStemDir: Float = if (isTrebleClef) {
            if (midiNote < 71) -1f else 1f
        } else {
            if (midiNote < 48) -1f else 1f
        }

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
            polyphonicStemDir = safeStemDir
        )
    }

    // Group all notes at the same (position, isTrebleClef, grace) into a single unified chord per staff
    // so multi-voice MIDI files don't draw two opposing stems/beams on the same staff at the same beat!
    val visibleNotes = mutableListOf<PrecomputedRenderNote>()
    val chordBuckets = initialNotes.groupBy { "${it.note.position}_${it.isTrebleClef}_${it.note.grace}" }
    for ((_, bucket) in chordBuckets) {
        if (bucket.size == 1) {
            visibleNotes.add(bucket.first())
        } else {
            val sortedByStep = bucket.sortedBy { it.diatonicStep }
            val minMidi = sortedByStep.minOf { it.midiNote }
            val avgMidi = sortedByStep.map { it.midiNote }.average()
            val firstItem = sortedByStep.first()

            val chordStemDir = if (firstItem.isTrebleClef) {
                if (minMidi < 69 || avgMidi < 71) -1f else 1f
            } else {
                // On Bass staff, if chord contains low notes (<= B2 / 47), point stem UP (-1f) away from TAB!
                if (minMidi <= 47) -1f else 1f
            }

            var lastDisplaced = false
            var prevStep = Int.MIN_VALUE
            var accColCounter = 0

            sortedByStep.forEachIndexed { idx, item ->
                val isSecond = (idx > 0 && abs(item.diatonicStep - prevStep) <= 1 && !lastDisplaced)
                lastDisplaced = isSecond
                prevStep = item.diatonicStep
                val offsetDir = if (chordStemDir < 0) 1f else -1f
                val adjustedX = if (isSecond) item.anchorX + (offsetDir * (stdNoteWidth * 0.88f)) else item.anchorX
                val col = if (item.resolvedAccidental != null) (accColCounter++ % 3) else 0
                visibleNotes.add(
                    item.copy(
                        nx = adjustedX,
                        isDisplacedSecond = isSecond,
                        accidentalColumn = col,
                        polyphonicStemDir = chordStemDir
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
    val stemOffset = stdNoteWidth * 0.44f

    // Group by Clef ONLY (not by voice) so each staff has at most ONE clean beam group per beat!
    val groupedByClef = visibleNotes.filter { !it.note.grace }.groupBy { it.isTrebleClef }
    for ((isTreble, clefNotes) in groupedByClef) {
        val groupedByBeat = clefNotes.groupBy {
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
                if (noteSpan !in (0.4f * lineSpacing)..(26f * lineSpacing)) continue

                val minClusterMidi = cluster.minOf { it.midiNote }
                val avgMidi = cluster.map { it.midiNote }.average()

                // Safe Beam Group Direction:
                // - Treble beams with any note < B4 (71) point UP (-1f) above the treble staff!
                // - Bass beams with any note <= B2 (47) point UP (-1f) inside the bass staff, never down into TAB!
                val groupStemDir = if (isTreble) {
                    if (minClusterMidi < 70 || avgMidi < 72) -1f else 1f
                } else {
                    if (minClusterMidi <= 48 || avgMidi < 50) -1f else 1f
                }

                val beamAnchorNotes = runPositions.map { pos ->
                    val notesAtPos = posGroups[pos]!!
                    if (groupStemDir > 0) notesAtPos.maxByOrNull { it.ny }!!
                    else notesAtPos.minByOrNull { it.ny }!!
                }

                val firstAnchor = beamAnchorNotes.first()
                val lastAnchor = beamAnchorNotes.last()

                val firstX = if (groupStemDir > 0) firstAnchor.anchorX - stemOffset else firstAnchor.anchorX + stemOffset
                val lastX = if (groupStemDir > 0) lastAnchor.anchorX - stemOffset else lastAnchor.anchorX + stemOffset
                val dx = (lastX - firstX).coerceAtLeast(1f)

                val rawPitchSlope = (lastAnchor.ny - firstAnchor.ny) / dx
                val isMonotonic = beamAnchorNotes.zipWithNext().all { (a, b) -> b.ny >= a.ny } ||
                        beamAnchorNotes.zipWithNext().all { (a, b) -> b.ny <= a.ny }
                val clampedSlope = if (isMonotonic) (rawPitchSlope * 0.20f).coerceIn(-0.08f, 0.08f) else 0f

                val standardStemLength = lineSpacing * 2.55f
                var constrainedStartY = if (groupStemDir > 0) {
                    beamAnchorNotes.maxOf { it.ny + standardStemLength - (clampedSlope * (it.anchorX - firstAnchor.anchorX)) }
                } else {
                    beamAnchorNotes.minOf { it.ny - standardStemLength - (clampedSlope * (it.anchorX - firstAnchor.anchorX)) }
                }

                val minStemClearance = lineSpacing * 1.85f
                for (n in beamAnchorNotes) {
                    val beamYAtN = constrainedStartY + (clampedSlope * (n.anchorX - firstAnchor.anchorX))
                    val stemLen = if (groupStemDir > 0) (beamYAtN - n.ny) else (n.ny - beamYAtN)
                    if (stemLen < minStemClearance) {
                        constrainedStartY += (groupStemDir * (minStemClearance - stemLen))
                    }
                }

                // Strict Inter-Staff & Bass-to-TAB Beam Clamp
                val interStaffMidY = (trebleBaseline + 4f * lineSpacing + bassBaseline) * 0.5f
                val bassMaxDownBeamY = bassBaseline + 4.8f * lineSpacing
                if (isTreble && groupStemDir > 0) {
                    val maxRawY = maxOf(constrainedStartY, constrainedStartY + (clampedSlope * dx))
                    if (maxRawY > interStaffMidY - 0.5f * lineSpacing) {
                        constrainedStartY -= (maxRawY - (interStaffMidY - 0.5f * lineSpacing))
                    }
                } else if (!isTreble && groupStemDir < 0) {
                    val minRawY = minOf(constrainedStartY, constrainedStartY + (clampedSlope * dx))
                    if (minRawY < interStaffMidY + 0.5f * lineSpacing) {
                        constrainedStartY += ((interStaffMidY + 0.5f * lineSpacing) - minRawY)
                    }
                } else if (!isTreble && groupStemDir > 0) {
                    val maxRawY = maxOf(constrainedStartY, constrainedStartY + (clampedSlope * dx))
                    if (maxRawY > bassMaxDownBeamY) {
                        constrainedStartY -= (maxRawY - bassMaxDownBeamY)
                    }
                }

                val constrainedEndY = constrainedStartY + (clampedSlope * dx)

                val noteStemYMap = mutableMapOf<ScoreNote, Float>()
                cluster.forEach { n ->
                    val beamYAtNote = constrainedStartY + (clampedSlope * (n.anchorX - firstAnchor.anchorX))
                    noteStemYMap[n.note] = beamYAtNote
                }

                val secondaryGap = -groupStemDir * (lineSpacing * 0.55f)
                val tertiaryGap = -groupStemDir * (lineSpacing * 1.10f)
                val beamletLength = lineSpacing * 0.68f

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
                            val bx2 = sx + dir * beamletLength
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
                            val bx2 = sx + dir * beamletLength
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

 val enabledNotesInMeasure = measure.notes.filter { enabledTrackIndices[it.voice] ?: true }
    val staffAssignmentMap = enabledNotesInMeasure.associateWith { note ->
        val midiNote = (note.midiNote + transposition).coerceIn(24, 103)
        resolveClefForNote(note, midiNote, hasDifferentiatedVoices, isLeftHandHidden, isRightHandHidden)
    }
    val explicitTrebleRests = measure.rests.filter { it.voice == 0 && (enabledTrackIndices[0] ?: true) }
    val explicitBassRests = measure.rests.filter { it.voice == 1 && (enabledTrackIndices[1] ?: true) }
    val trebleRests = if (!isRightHandHidden) calculateRestsForStaff(enabledNotesInMeasure, isTreble = true, staffAssignmentMap, beatsPerMeasure, explicitTrebleRests) else emptyList()
    val bassRests = if (!isLeftHandHidden) calculateRestsForStaff(enabledNotesInMeasure, isTreble = false, staffAssignmentMap, beatsPerMeasure, explicitBassRests) else emptyList()
    return PrecomputedStaffLayout(
        visibleNotes = visibleNotes,
        computedXByPosition = computedXByPosition,
        noteBeamMap = noteBeamMap,
        beamGroups = precomputedBeams,
        trebleRests = trebleRests,
        bassRests = bassRests,
        hasPolyphonicVoicesOnTreble = false,
        hasPolyphonicVoicesOnBass = false,
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
    enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
    transposition: Int = 0
): PrecomputedTabLayout {
    val tabHeaderWidth = (if (drawClefHeader) 3.0f * lineSpacing else 0.3f * lineSpacing) + (if (drawTimeHeader) 2.2f * lineSpacing else 0f)
    val systemContentStartX = startX + tabHeaderWidth
    val rightPad = 1.25f * lineSpacing
    val leftNotePad = 1.05f * lineSpacing
    val usableMeasureWidth = (endX - systemContentStartX - rightPad).coerceAtLeast(3.5f * lineSpacing)
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
            val noteMidi = note.midiNote + transposition
            val isTreble = if (hasDifferentiatedVoices) note.voice == 0 else noteMidi >= 60
            if (isTreble && isRightHandHidden) false
            else if (!isTreble && isLeftHandHidden) false
            else true
        }

    val distinctPositions = validNotes.filter { !it.grace }.map { it.position }.distinct().sorted()
    val minTabGap = 1.80f * lineSpacing
    val computedXByPosition = mutableMapOf<Float, Float>()

    if (distinctPositions.isNotEmpty()) {
        val minDur = validNotes.filter { !it.grace }.minOfOrNull { it.duration.coerceAtLeast(0.125f) } ?: 1.0f
        val posDurations = distinctPositions.mapIndexed { idx, pos ->
            val nextPos = if (idx < distinctPositions.lastIndex) distinctPositions[idx + 1] else beatsPerMeasure
            val delta = (nextPos - pos).coerceAtLeast(minDur)
            (delta / minDur).toDouble().pow(0.52).toFloat()
        }
        val sumWeighted = posDurations.sum().coerceAtLeast(1f)
        val noteContentStartX = systemContentStartX + leftNotePad
        val noteContentMaxX = (endX - rightPad).coerceAtLeast(noteContentStartX + lineSpacing)
        val totalNaturalSpan = (noteContentMaxX - noteContentStartX).coerceAtLeast(2.0f * lineSpacing)

        val naturalSpacingNeeded = (distinctPositions.size - 1) * minTabGap
        val effectiveGap = if (naturalSpacingNeeded > totalNaturalSpan && distinctPositions.size > 1) {
            totalNaturalSpan / distinctPositions.size.toFloat()
        } else {
            minTabGap
        }

        var runningX = noteContentStartX
        var runningWeight = 0f
        distinctPositions.forEachIndexed { idx, pos ->
            val fraction = (runningWeight / sumWeighted).coerceIn(0f, 0.94f)
            val idealX = noteContentStartX + (totalNaturalSpan * fraction)
            val targetX = if (idx == 0) idealX else maxOf(idealX, runningX + effectiveGap)
            val clampedX = targetX.coerceIn(noteContentStartX, noteContentMaxX)
            computedXByPosition[pos] = clampedX
            runningX = clampedX
            runningWeight += posDurations[idx]
        }
    }

    val chordGroups = validNotes.groupBy { "${it.position}_${it.grace}" }
    val tabNotes = mutableListOf<PrecomputedTabNote>()

    chordGroups.values.forEach { notesAtPos ->
        val sample = notesAtPos.first()
        val baseNx = computedXByPosition[sample.position]
            ?: (systemContentStartX + leftNotePad + ((usableMeasureWidth - leftNotePad) * (sample.position / beatsPerMeasure)))
        val nx = if (sample.grace) (baseNx - 1.0f * lineSpacing).coerceAtLeast(systemContentStartX + 0.35f * lineSpacing) else baseNx
        val sortedDescMidi = notesAtPos.sortedByDescending { it.midiNote + transposition }
        val usedStrings = mutableSetOf<Int>()

        val candidateFrets = sortedDescMidi.mapNotNull { n ->
            val effectivePitch = n.midiNote + transposition
            (0..5).map { s -> effectivePitch - guitarOpenStringsMidi[s] }.filter { it in 1..22 }.minOrNull()
        }
        val targetFretAnchor = if (candidateFrets.isNotEmpty()) candidateFrets.average().toInt().coerceIn(1, 15) else 0

        sortedDescMidi.forEach { note ->
            val effectivePitch = note.midiNote + transposition
            val normalizedExplicitString = note.string?.let { s ->
                if (s in 1..6 && note.fret != null && (effectivePitch - guitarOpenStringsMidi.getOrElse(s - 1) { 0 }) == (note.fret + transposition)) s - 1
                else s.coerceIn(0, 5)
            }
            val stringIdx = if (normalizedExplicitString != null && normalizedExplicitString in 0..5 && normalizedExplicitString !in usedStrings) {
                normalizedExplicitString
            } else {
                var bestStr = (0..5).filter { it !in usedStrings }.minByOrNull { sIdx ->
                    val openPitch = guitarOpenStringsMidi[sIdx]
                    val fret = effectivePitch - openPitch
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

            val fretValue = if (transposition != 0) {
                (effectivePitch - guitarOpenStringsMidi[stringIdx]).coerceIn(0, 24)
            } else {
                note.fret ?: (effectivePitch - guitarOpenStringsMidi[stringIdx]).coerceIn(0, 24)
            }
            val ny = tBase + (stringIdx * lineSpacing)

            // CRITICAL FIX: Never wrap normal fret numbers in <13> angle brackets or (0) parentheses!
            // Parser often populates guitarHarmonic = NONE or tieEnd = true on regular notes.
            val fretText = if (note.articulation == TabArticulation.DEAD_NOTE) {
                "X"
            } else {
                fretValue.toString()
            }

            val circleRadius = lineSpacing * (if (note.grace) 0.26f else 0.34f)
            val pillHeight = circleRadius * 2f
            val charW = lineSpacing * 0.44f
            val pillWidth = maxOf(pillHeight, (fretText.length * charW) + (lineSpacing * 0.26f))

            tabNotes.add(
                PrecomputedTabNote(
                    note = note,
                    midiNote = effectivePitch,
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
val rests = calculateRestsForStaff(validNotes, isTreble = true, validNotes.associateWith { true }, beatsPerMeasure)
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
    textMeasurer: TextMeasurer,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.6f, 1.5f)
    val amount = bendInfo.bendSteps.coerceIn(0.25f, 2.0f)
    val targetY = start.y - (amount * 14f * scale)
    val apexX = start.x + 9f * scale

    when (bendInfo.type) {
        BendType.PRE_BEND -> {
            drawLine(color, Offset(start.x, start.y - 3f * scale), Offset(start.x, targetY), strokeWidth = 1.2f * scale)
            val arrow = Path().apply {
                moveTo(start.x, targetY)
                lineTo(start.x - 2.5f * scale, targetY + 3.8f * scale)
                lineTo(start.x + 2.5f * scale, targetY + 3.8f * scale)
                close()
            }
            drawPath(arrow, color)
        }
        BendType.BEND_RELEASE, BendType.PRE_BEND_RELEASE -> {
            val releaseX = apexX + 8f * scale
            val releaseY = start.y - (bendInfo.releaseSteps.coerceIn(0f, amount) * 14f * scale) - 2f * scale
            val path = Path().apply {
                moveTo(start.x + 3.5f * scale, start.y - 2f * scale)
                quadraticBezierTo(start.x + 7.0f * scale, start.y - 2f * scale, apexX, targetY)
                quadraticBezierTo(apexX + 4.0f * scale, targetY, releaseX, releaseY)
            }
            drawPath(path, color = color, style = Stroke(width = 1.2f * scale))
            val arrow = Path().apply {
                moveTo(releaseX, releaseY)
                lineTo(releaseX - 2.5f * scale, releaseY - 3.8f * scale)
                lineTo(releaseX + 2.5f * scale, releaseY - 3.8f * scale)
                close()
            }
            drawPath(arrow, color)
        }
        else -> {
            val path = Path().apply {
                moveTo(start.x + 3.5f * scale, start.y - 2f * scale)
                quadraticBezierTo(start.x + 8.0f * scale, start.y - 2f * scale, apexX, targetY)
            }
            drawPath(path, color = color, style = Stroke(width = 1.2f * scale))
            val arrowPath = Path().apply {
                moveTo(apexX, targetY)
                lineTo(apexX - 2.6f * scale, targetY + 3.8f * scale)
                lineTo(apexX + 2.6f * scale, targetY + 3.8f * scale)
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
    val layout = textMeasurer.measure(label, TextStyle(color = color, fontSize = (6.8f * scale).sp, fontWeight = FontWeight.Bold))
    drawText(layout, topLeft = Offset(apexX - layout.size.width / 2f, targetY - layout.size.height - 1f))
}

private fun DrawScope.drawWhammyDip(
    center: Offset,
    dipSteps: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.6f, 1.5f)
    val path = Path().apply {
        moveTo(center.x - 5.5f * scale, center.y - 11f * scale)
        lineTo(center.x, center.y - 4.5f * scale)
        lineTo(center.x + 5.5f * scale, center.y - 11f * scale)
    }
    drawPath(path, color = color, style = Stroke(width = 1.15f * scale))
    val label = if (abs(dipSteps) >= 1f) "${dipSteps.toInt()}" else "½"
    val layout = textMeasurer.measure(label, TextStyle(color = color, fontSize = (6.5f * scale).sp, fontWeight = FontWeight.Bold))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - 19f * scale))
}

private fun DrawScope.drawBarre(
    barre: BarreInfo,
    x: Float,
    topY: Float,
    bottomY: Float,
    color: Color,
    textMeasurer: TextMeasurer? = null,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.6f, 1.5f)
    drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = 2.0f * scale, cap = StrokeCap.Round)
    if (textMeasurer != null && barre.fret > 0) {
        val roman = when (barre.fret) {
            1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
            6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"; 10 -> "X"
            11 -> "XI"; 12 -> "XII"; else -> "B${barre.fret}"
        }
        val bLayout = textMeasurer.measure("C$roman", TextStyle(color = color, fontSize = (7.2f * scale).sp, fontWeight = FontWeight.Bold))
        drawText(bLayout, topLeft = Offset(x - 3f * scale, topY - bLayout.size.height - 3f * scale))
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
    }

    val standardDurations = floatArrayOf(4.0f, 2.0f, 1.0f, 0.5f, 0.25f, 0.125f)

    fun decomposeInterval(startPos: Float, length: Float, dest: MutableList<RestSymbol>) {
        var pos = startPos
        var remaining = length
        while (remaining >= 0.11f) {
            // Snap to clean beat boundaries first so rests don't straddle beats awkwardly
            val matchedDur = standardDurations.firstOrNull { it <= remaining + 0.03f } ?: 0.125f
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

    // Merge overlapping note intervals [start, end] across all voices on this staff
    val rawIntervals = staffNotes.map { n ->
        val start = n.position.coerceIn(0f, beatsPerMeasure)
        val end = (n.position + n.duration.coerceAtLeast(0.125f)).coerceIn(start, beatsPerMeasure)
        start to end
    }.sortedBy { it.first }

    val mergedIntervals = mutableListOf<Pair<Float, Float>>()
    for (interval in rawIntervals) {
        if (mergedIntervals.isEmpty()) {
            mergedIntervals.add(interval)
        } else {
            val last = mergedIntervals.last()
            if (interval.first <= last.second + 0.06f) {
                mergedIntervals[mergedIntervals.lastIndex] = last.first to maxOf(last.second, interval.second)
            } else {
                mergedIntervals.add(interval)
            }
        }
    }

    val rests = mutableListOf<RestSymbol>()
    var cursor = 0.0f
    for ((start, end) in mergedIntervals) {
        if (start > cursor + 0.11f) {
            decomposeInterval(cursor, start - cursor, rests)
        }
        cursor = maxOf(cursor, end)
    }
    if (cursor < beatsPerMeasure - 0.11f) {
        decomposeInterval(cursor, beatsPerMeasure - cursor, rests)
    }
    return rests
}

data class NotationViewerOptions(
    val primaryColor: Color = Color(0xFF6C63FF),
    val format: String = "Staff Only",
    val onFormatChange: (String) -> Unit = {},
    val showLetters: Boolean = true,
    val showFingerings: Boolean = true,
    val showLyrics: Boolean = true,
    val isSplitStaff: Boolean = true,
    val isVerticalScroll: Boolean = false,
    val onVerticalScrollChange: ((Boolean) -> Unit)? = null,
    val leftHandColor: Color = Color(0xFF6C63FF),
    val rightHandColor: Color = Color(0xFF4A90E2),
    val isLeftHandHidden: Boolean = false,
    val isRightHandHidden: Boolean = false,
    val enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
    val isPedalPressed: Boolean = false,
    val showScoreHeader: Boolean = true,
    val compactStaffMode: Boolean = false,
    val transposition: Int = 0,
    val fallbackTitle: String = ""
)

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
    showFingerings: Boolean = true,
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
    transposition: Int = 0,
    fallbackTitle: String = "",
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    StaffAndTabNotationViewer(
        score = score,
        playheadMeasure = playheadMeasure,
        playheadOffset = playheadOffset,
        activeMidiNotes = activeMidiNotes,
        playingNote = playingNote,
        options = NotationViewerOptions(
            primaryColor = primaryColor,
            format = currentFormat,
            onFormatChange = onFormatChange,
            showLetters = showLetters,
            showFingerings = showFingerings,
            showLyrics = showLyrics,
            isSplitStaff = isSplitStaff,
            isVerticalScroll = isVerticalScroll,
            onVerticalScrollChange = onVerticalScrollChange,
            leftHandColor = leftHandColor,
            rightHandColor = rightHandColor,
            isLeftHandHidden = isLeftHandHidden,
            isRightHandHidden = isRightHandHidden,
            enabledTrackIndices = enabledTrackIndices,
            isPedalPressed = isPedalPressed,
            showScoreHeader = showScoreHeader,
            compactStaffMode = compactStaffMode,
            transposition = transposition,
            fallbackTitle = fallbackTitle
        ),
        modifier = modifier,
        scrollState = scrollState
    )
}

@Composable
fun StaffAndTabNotationViewer(
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    activeMidiNotes: Map<Int, Boolean>,
    playingNote: Int?,
    options: NotationViewerOptions,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    val primaryColor = options.primaryColor
    val currentFormat = options.format
    val onFormatChange = options.onFormatChange
    val showLetters = options.showLetters
    val showLyrics = options.showLyrics
    val showFingerings = options.showFingerings
    val isSplitStaff = options.isSplitStaff
    val isVerticalScroll = options.isVerticalScroll
    val onVerticalScrollChange = options.onVerticalScrollChange
    val leftHandColor = options.leftHandColor
    val rightHandColor = options.rightHandColor
    val isLeftHandHidden = options.isLeftHandHidden
    val isRightHandHidden = options.isRightHandHidden
    val enabledTrackIndices = options.enabledTrackIndices
    val isPedalPressed = options.isPedalPressed
    val showScoreHeader = options.showScoreHeader
    val compactStaffMode = options.compactStaffMode
    val transposition = options.transposition
    val fallbackTitle = options.fallbackTitle
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val stableActiveNotes by remember(activeMidiNotes) {
        derivedStateOf { activeMidiNotes.filterValues { it }.keys }
    }

    val labelCache = remember(textMeasurer, compactStaffMode) {
        val fSize = if (compactStaffMode) 5.8.sp else 6.8.sp
        (0..11).associateWith { i ->
            textMeasurer.measure(
                text = MusicConstants.NOTE_LABELS[i].take(1),
                style = TextStyle(
                    color = Color.White,
                    fontSize = fSize,
                    lineHeight = fSize,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    background = Color.Transparent
                )
            )
        }
    }
    val sharpCache = remember(textMeasurer, primaryColor) {
        textMeasurer.measure("♯", TextStyle(color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
    val flatCache = remember(textMeasurer, primaryColor) {
        textMeasurer.measure("♭", TextStyle(color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
    val measureLabelsCache = remember(textMeasurer, score?.measures?.size, primaryColor) {
        val count = score?.measures?.size ?: 0
        (0 until count).associateWith { mIdx ->
            textMeasurer.measure(
                text = "${mIdx + 1}",
                style = TextStyle(color = primaryColor.copy(alpha = 0.72f), fontSize = 9.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
            )
        }
    }
  
    // Force snapshot observation of enabledTrackIndices so toggling any track switch immediately recomposes the Canvas
    val tracksSnapshotKey = enabledTrackIndices.entries.joinToString(",") { "${it.key}:${it.value}" }
    val drumTrackIndices = remember(score?.tracks) {
        score?.tracks?.filter { it.channel == 9 }?.map { it.trackIndex }?.toSet() ?: emptySet()
    }
    val hasEnabledMelodicTracks = remember(score?.tracks, tracksSnapshotKey) {
        val tracks = score?.tracks ?: emptyList()
        tracks.isEmpty() || tracks.any { it.channel != 9 && (enabledTrackIndices[it.trackIndex] ?: it.isEnabled) }
    }
    // Automatically exclude Channel 10 Drum tracks from melodic Staff/TAB notation unless the user SOLOed the drum track
    val effectiveStaffTrackIndices = remember(tracksSnapshotKey, drumTrackIndices, hasEnabledMelodicTracks) {
        val map = enabledTrackIndices.toMutableMap()
        if (hasEnabledMelodicTracks) {
            drumTrackIndices.forEach { dIdx -> map[dIdx] = false }
        }
        map.toMap()
    }

    val layoutCache = remember(score, transposition, tracksSnapshotKey) { MeasureLayoutCache() }
    var notationZoom by rememberSaveable { mutableFloatStateOf(1.0f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            val zoomChange = event.calculateZoom()
                            if (kotlin.math.abs(zoomChange - 1f) > 0.008f) {
                                notationZoom = (notationZoom * zoomChange).coerceIn(0.65f, 2.4f)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ){
        if (score != null && score.measures.isNotEmpty()) {
            val hasExtractedNotes = remember(score) { score.measures.any { m -> m.notes.any { !it.isRest } } }
            if (score.isPdfScore && !hasExtractedNotes && !score.pdfUri.isNullOrBlank()) {
                PdfDocumentViewer(
                    uriString = score.pdfUri,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp)
                ) {
                    if (showScoreHeader) {
                        val resolvedTitle = remember(score.title, fallbackTitle, score.tracks) {
                            val rawTitle = score.title.trim()
                            when {
                                rawTitle.isNotEmpty() && !rawTitle.equals("Untitled Score", ignoreCase = true) && !rawTitle.equals("Score", ignoreCase = true) -> rawTitle
                                fallbackTitle.isNotBlank() -> fallbackTitle.substringAfterLast("/").substringBeforeLast(".")
                                score.tracks.any { it.trackName.isNotBlank() && !it.trackName.equals("Track 1", true) } ->
                                    score.tracks.first { it.trackName.isNotBlank() }.trackName
                                else -> "Studio Score"
                            }.replace("_", " ")
                        }
                        val resolvedTempoMark = remember(score.tempo, score.tempoText) {
                            if (score.tempoText.isNotBlank() && score.tempoText != "Moderato") score.tempoText
                            else when (score.tempo.toInt()) {
                                in 0..59 -> "Largo"
                                in 60..75 -> "Adagio"
                                in 76..107 -> "Andante"
                                in 108..119 -> "Moderato"
                                in 120..155 -> "Allegro"
                                in 156..175 -> "Vivace"
                                else -> "Presto"
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = resolvedTitle,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 0.6.sp,
                                textAlign = TextAlign.Center
                            )
                            val byline = buildString {
                                if (score.composer.isNotBlank()) append(score.composer)
                                if (score.lyricist.isNotBlank()) {
                                    if (isNotEmpty()) append(" • Arranged/Words: ")
                                    append(score.lyricist)
                                }
                            }
                            if (byline.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = byline,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "𝅘𝅥 = ${score.tempo.toInt()} BPM ($resolvedTempoMark)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text(
                                    text = "${score.timeSignature} Time",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text(
                                    text = score.keySignature.keyName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor.copy(alpha = 0.9f)
                                )
                                if (score.capo > 0) {
                                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    Text("Capo ${score.capo}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                                Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text(
                                    text = "${score.measures.size} Bars",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                    val normalizedFormat = currentFormat.trim().lowercase()
                    val showStaff = normalizedFormat != "tab" && normalizedFormat != "tab only"
                    val showTab = normalizedFormat != "staff" && normalizedFormat != "staff only"
                    val effectiveLeftHandHidden = isLeftHandHidden || !isSplitStaff

                    var measuresPerRow by rememberSaveable { mutableIntStateOf(2) }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val viewportWidthDp = maxWidth
                        val viewportHeightDp = maxHeight
                        val viewportWidthPx = with(density) { viewportWidthDp.toPx() }
                        val viewportHeightPx = with(density) { viewportHeightDp.toPx() }

                        val idealStaffSpacingPx = with(density) {
                            (if (compactStaffMode) 10.5.dp else 13.0.dp).toPx() * notationZoom
                        }
                        val idealTabSpacingPx = with(density) {
                            (if (!showStaff) 15.5.dp else if (compactStaffMode) 12.5.dp else 14.2.dp).toPx() * notationZoom
                        }

                        val topMarginSpaces = 4.5f
                        val bottomMarginSpaces = 2.8f
                        val staffToTabGapSpaces = 9.2f
                        val totalStaffSpacesNeeded = when {
                            showStaff && showTab -> topMarginSpaces + (if (effectiveLeftHandHidden) 4f else 12.2f) + staffToTabGapSpaces + (5f * (idealTabSpacingPx / idealStaffSpacingPx)) + bottomMarginSpaces
                            showStaff -> topMarginSpaces + (if (effectiveLeftHandHidden) 4f else 12.2f) + bottomMarginSpaces
                            else -> topMarginSpaces + 5.5f + bottomMarginSpaces
                        }

                        val fittedStaffSpacingPx = if (!isVerticalScroll && viewportHeightPx > 80f) {
                            minOf(idealStaffSpacingPx, (viewportHeightPx * 0.92f) / totalStaffSpacesNeeded)
                                .coerceAtLeast(with(density) { 7.0.dp.toPx() })
                        } else {
                            idealStaffSpacingPx
                        }
                        val fittedTabSpacingPx = if (!showStaff && !isVerticalScroll && viewportHeightPx > 80f) {
                            minOf(idealTabSpacingPx, (viewportHeightPx * 0.90f) / 10.5f)
                                .coerceAtLeast(with(density) { 10.5.dp.toPx() })
                        } else {
                            idealTabSpacingPx * (fittedStaffSpacingPx / idealStaffSpacingPx.coerceAtLeast(1f))
                        }

                        val computedSystemContentHeightPx = when {
                            showStaff && showTab -> {
                                val tBase = topMarginSpaces * fittedStaffSpacingPx
                                val bBase = if (effectiveLeftHandHidden) tBase else tBase + (8.2f * fittedStaffSpacingPx)
                                val tbBase = bBase + (staffToTabGapSpaces * fittedStaffSpacingPx)
                                tbBase + (5f * fittedTabSpacingPx) + (bottomMarginSpaces * fittedStaffSpacingPx)
                            }
                            showStaff -> {
                                val tBase = topMarginSpaces * fittedStaffSpacingPx
                                val bBase = if (effectiveLeftHandHidden) tBase else tBase + (8.2f * fittedStaffSpacingPx)
                                bBase + (4f * fittedStaffSpacingPx) + (bottomMarginSpaces * fittedStaffSpacingPx)
                            }
                            else -> {
                                val tbBase = (topMarginSpaces * 0.85f) * fittedTabSpacingPx
                                tbBase + (5f * fittedTabSpacingPx) + (bottomMarginSpaces * fittedTabSpacingPx)
                            }
                        }

                        val rowHeightPx = if (isVerticalScroll) {
                            computedSystemContentHeightPx.coerceAtLeast(with(density) { 150.dp.toPx() })
                        } else {
                            maxOf(viewportHeightPx, computedSystemContentHeightPx)
                        }
                        val rowHeight = with(density) { rowHeightPx.toDp() }

                        val verticalCenteringOffsetPx = if (!isVerticalScroll && viewportHeightPx > computedSystemContentHeightPx) {
                            ((viewportHeightPx - computedSystemContentHeightPx) * 0.5f).coerceAtLeast(0f)
                        } else {
                            0f
                        }

                        val verticalScrollState = rememberScrollState()
                        val horizontalScrollState = rememberScrollState()

                        val horizontalMeasureBounds = remember(
                            score.measures,
                            density,
                            notationZoom,
                            tracksSnapshotKey,
                            viewportWidthDp,
                            measuresPerRow
                        ) {
                            var currentX = 0f
                            val minReadableMeasureW = (viewportWidthDp / measuresPerRow.coerceIn(1, 4).toFloat()).coerceAtLeast(250.dp)
                            score.measures.mapIndexed { mIdx, measure ->
                                val isFirstMeasure = (mIdx == 0)
                                val distinctPositions = measure.notes
                                    .filter { !it.isRest && (effectiveStaffTrackIndices[it.voice] ?: true) }
                                    .map { it.position }.distinct().size
                                val densityWidthDp = when {
                                    distinctPositions >= 12 -> 540.dp
                                    distinctPositions >= 9 -> 450.dp
                                    distinctPositions >= 6 -> 365.dp
                                    distinctPositions >= 4 -> 300.dp
                                    else -> 250.dp
                                }
                                val baseMeasureW = maxOf(minReadableMeasureW, densityWidthDp)
                                val measureWidthDp = (if (isFirstMeasure) baseMeasureW + 65.dp else baseMeasureW) * notationZoom
                                val widthPx = with(density) { measureWidthDp.toPx() }
                                val startPx = currentX
                                currentX += widthPx
                                Triple(measureWidthDp, startPx, currentX)
                            }
                        }

                        LaunchedEffect(
                            playheadMeasure,
                            isVerticalScroll,
                            rowHeightPx,
                            viewportWidthPx,
                            viewportHeightPx,
                            measuresPerRow
                        ) {
                            if (playheadMeasure >= 0 && score.measures.isNotEmpty()) {
                                if (isVerticalScroll) {
                                    val targetRow = playheadMeasure / measuresPerRow.coerceAtLeast(1)
                                    val rowTopPx = targetRow * rowHeightPx
                                    val rowBottomPx = rowTopPx + rowHeightPx
                                    val curScrollY = verticalScrollState.value.toFloat()
                                    val visibleH = if (verticalScrollState.viewportSize > 0) verticalScrollState.viewportSize.toFloat() else viewportHeightPx

                                    if (rowBottomPx > curScrollY + visibleH + 12f || rowTopPx < curScrollY - 12f) {
                                        verticalScrollState.animateScrollTo(rowTopPx.toInt().coerceAtLeast(0))
                                    }
                                } else {
                                    val bounds = horizontalMeasureBounds.getOrNull(playheadMeasure)
                                    if (bounds != null) {
                                        val mStartPx = bounds.second
                                        val mEndPx = bounds.third
                                        val curScrollX = horizontalScrollState.value.toFloat()
                                        val visibleW = if (horizontalScrollState.viewportSize > 0) horizontalScrollState.viewportSize.toFloat() else viewportWidthPx

                                        if (mEndPx > curScrollX + visibleW - 24f || mStartPx < curScrollX + 8f) {
                                            val targetScrollX = (mStartPx - 24f).toInt().coerceAtLeast(0)
                                            horizontalScrollState.animateScrollTo(targetScrollX)
                                        }
                                    }
                                }
                            }
                        }

                        val safeMeasuresPerRow = measuresPerRow.coerceIn(1, 4)
                        val rowMeasureChunks = remember(score.measures, isVerticalScroll, safeMeasuresPerRow) {
                            if (isVerticalScroll) {
                                score.measures.chunked(safeMeasuresPerRow)
                            } else {
                                score.measures.chunked(1)
                            }
                        }

                        if (isVerticalScroll) {
                            val scrollY = verticalScrollState.value.toFloat()
                            val viewportH = if (verticalScrollState.viewportSize > 0) verticalScrollState.viewportSize.toFloat() else viewportHeightPx
                            val minVisibleRow = ((scrollY - rowHeightPx) / rowHeightPx).toInt().coerceAtLeast(0)
                            val maxVisibleRow = ((scrollY + viewportH + rowHeightPx) / rowHeightPx).toInt().coerceAtMost(rowMeasureChunks.lastIndex)

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(verticalScrollState)
                                    .padding(vertical = 4.dp)
                            ) {
                                rowMeasureChunks.forEachIndexed { rowIndex, rowMeasures ->
                                    if (rowIndex !in minVisibleRow..maxVisibleRow) {
                                        Spacer(modifier = Modifier.fillMaxWidth().height(rowHeight))
                                    } else {
                                        Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val staffLineSpacing = fittedStaffSpacingPx
                                                val tabLineSpacing = fittedTabSpacingPx
                                                val trebleBaseline = topMarginSpaces * staffLineSpacing
                                                val bassBaseline = if (effectiveLeftHandHidden) trebleBaseline else trebleBaseline + (8.2f * staffLineSpacing)
                                                val tabBaseline = if (showStaff) {
                                                    bassBaseline + (staffToTabGapSpaces * staffLineSpacing)
                                                } else {
                                                    (topMarginSpaces * 0.85f) * tabLineSpacing
                                                }
                                                val onSurface = colorScheme.onSurface
                                                val leftPadding = 16f
                                                val rightPadding = 14f
                                                val availableWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(100f)
                                                val measureWidth = availableWidth / rowMeasures.size.coerceAtLeast(1)

                                                if (showStaff && !effectiveLeftHandHidden && !isRightHandHidden) {
                                                    drawGrandStaffBrace(
                                                        x = leftPadding - 3f,
                                                        topY = trebleBaseline,
                                                        bottomY = bassBaseline + (4f * staffLineSpacing),
                                                        color = onSurface.copy(alpha = 0.75f),
                                                        lineSpacing = staffLineSpacing
                                                    )
                                                }

                                                rowMeasures.forEachIndexed { mIdxInRow, measure ->
                                                    val mIdx = (rowIndex * safeMeasuresPerRow) + mIdxInRow
                                                    val prevMeasure = if (mIdx > 0) score.measures.getOrNull(mIdx - 1) else null
                                                    val nextMeasure = if (mIdx + 1 < score.measures.size) score.measures.getOrNull(mIdx + 1) else null

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
                                                        text = "${mIdx + 1}",
                                                        style = TextStyle(color = primaryColor.copy(alpha = 0.72f), fontSize = 9.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                                                    )

                                                    drawText(
                                                        textLayoutResult = labelLayout,
                                                        topLeft = Offset(startX + 4f, if (showStaff) trebleBaseline - (staffLineSpacing * 2.1f) else tabBaseline - (tabLineSpacing * 2.1f))
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
                                                            color = colorScheme.outlineVariant.copy(alpha = 0.55f),
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
                                                                color = onSurface.copy(alpha = 0.65f),
                                                                repeatStart = true,
                                                                repeatEnd = false,
                                                                lineSpacing = staffLineSpacing
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
                                                            color = onSurface.copy(alpha = 0.55f),
                                                            repeatStart = false,
                                                            repeatEnd = measure.repeatEnd,
                                                            lineSpacing = staffLineSpacing
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
                                                            enabledTrackIndices = effectiveStaffTrackIndices,
                                                            transposition = transposition
                                                        )

                                                        drawStaffMeasureContent(
                                                            drawScope = this,
                                                            startX = startX,
                                                            endX = endX,
                                                            h = size.height,
                                                            measure = measure,
                                                            prevMeasure = prevMeasure,
                                                            nextMeasure = nextMeasure,
                                                            activeMidiNotes = stableActiveNotes,
                                                            playingNote = playingNote,
                                                            playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                            config = staffConfig,
                                                            textMeasurer = textMeasurer,
                                                            noteLabels = MusicConstants.NOTE_LABELS,
                                                            diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                                            isFinalBar = (mIdx == score.measures.lastIndex),
                                                            isMeasureActive = isPlayheadInThisMeasure,
                                                            timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                                                            drawClefHeader = isRowStart,
                                                            drawKeyHeader = isRowStart || keyChanged,
                                                            drawTimeHeader = (mIdx == 0) || timeChanged,
                                                            labelCache = labelCache,
                                                            sharpCache = sharpCache,
                                                            flatCache = flatCache,
                                                            staffLayout = staffLayout,
                                                            showFingerings = showFingerings
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
                                                            color = colorScheme.outlineVariant.copy(alpha = 0.55f),
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
                                                            enabledTrackIndices = effectiveStaffTrackIndices,
                                                            transposition = transposition
                                                        )

                                                        drawTabMeasureContent(
                                                            drawScope = this,
                                                            startX = startX,
                                                            endX = endX,
                                                            h = size.height,
                                                            measure = measure,
                                                            activeMidiNotes = stableActiveNotes,
                                                            playingNote = playingNote,
                                                            playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                            config = tabConfig,
                                                            labelLayout = labelLayout,
                                                            tBase = tabBaseline,
                                                            textMeasurer = textMeasurer,
                                                            isFinalBar = (mIdx == score.measures.lastIndex),
                                                            isMeasureActive = isPlayheadInThisMeasure,
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
                            // HORIZONTAL SCROLL MODE: Each measure is placed directly inside the Row (NO inner Box stacking them at x=0!)
                            val scrollX = horizontalScrollState.value.toFloat()
                            val viewportW = if (horizontalScrollState.viewportSize > 0) horizontalScrollState.viewportSize.toFloat() else viewportWidthPx
                            val minVisibleX = scrollX - 450f
                            val maxVisibleX = scrollX + viewportW + 450f

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(horizontalScrollState)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
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
                                                val staffLineSpacing = fittedStaffSpacingPx
                                                val tabLineSpacing = fittedTabSpacingPx
                                                val trebleBaseline = verticalCenteringOffsetPx + (topMarginSpaces * staffLineSpacing)
                                                val bassBaseline = if (effectiveLeftHandHidden) trebleBaseline else trebleBaseline + (8.2f * staffLineSpacing)
                                                val tabBaseline = if (showStaff) {
                                                    bassBaseline + (staffToTabGapSpaces * staffLineSpacing)
                                                } else {
                                                    verticalCenteringOffsetPx + ((topMarginSpaces * 0.85f) * tabLineSpacing)
                                                }
                                                val onSurface = colorScheme.onSurface
                                                val startX = if (isFirstMeasure) 14f else 0f
                                                val endX = size.width
                                                val isPlayheadInThisMeasure = (mIdx == playheadMeasure)
                                                val prevMeasure = if (mIdx > 0) score.measures.getOrNull(mIdx - 1) else null
                                                val nextMeasure = if (mIdx + 1 < score.measures.size) score.measures.getOrNull(mIdx + 1) else null
                                                val keyChanged = prevMeasure != null && prevMeasure.keySignature != measure.keySignature
                                                val timeChanged = prevMeasure != null && (prevMeasure.timeSignatureNumerator != measure.timeSignatureNumerator || prevMeasure.timeSignatureDenominator != measure.timeSignatureDenominator)

                                                if (isFirstMeasure && showStaff && !effectiveLeftHandHidden && !isRightHandHidden) {
                                                    drawGrandStaffBrace(
                                                        x = startX - 3f,
                                                        topY = trebleBaseline,
                                                        bottomY = bassBaseline + (4f * staffLineSpacing),
                                                        color = onSurface.copy(alpha = 0.75f),
                                                        lineSpacing = staffLineSpacing
                                                    )
                                                }

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
                                                    text = "${mIdx + 1}",
                                                    style = TextStyle(color = primaryColor.copy(alpha = 0.72f), fontSize = 9.5.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                                                )

                                                drawText(
                                                    textLayoutResult = labelLayout,
                                                    topLeft = Offset(startX + (if (isFirstMeasure) 6f else 4f), if (showStaff) trebleBaseline - (staffLineSpacing * 2.1f) else tabBaseline - (tabLineSpacing * 2.1f))
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
                                                        color = colorScheme.outlineVariant.copy(alpha = 0.55f),
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
                                                            color = onSurface.copy(alpha = 0.65f),
                                                            repeatStart = true,
                                                            repeatEnd = false,
                                                            lineSpacing = staffLineSpacing
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
                                                        color = onSurface.copy(alpha = 0.55f),
                                                        repeatStart = false,
                                                        repeatEnd = measure.repeatEnd,
                                                        lineSpacing = staffLineSpacing
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
                                                        enabledTrackIndices = effectiveStaffTrackIndices,
                                                        transposition = transposition
                                                    )

                                                    drawStaffMeasureContent(
                                                        drawScope = this,
                                                        startX = startX,
                                                        endX = endX,
                                                        h = size.height,
                                                        measure = measure,
                                                        prevMeasure = prevMeasure,
                                                        nextMeasure = nextMeasure,
                                                        activeMidiNotes = stableActiveNotes,
                                                        playingNote = playingNote,
                                                        playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                        config = staffConfig,
                                                        textMeasurer = textMeasurer,
                                                        noteLabels = MusicConstants.NOTE_LABELS,
                                                        diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                                        isFinalBar = (mIdx == score.measures.lastIndex),
                                                        isMeasureActive = isPlayheadInThisMeasure,
                                                        timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                                                        drawClefHeader = isFirstMeasure,
                                                        drawKeyHeader = isFirstMeasure || keyChanged,
                                                        drawTimeHeader = isFirstMeasure || timeChanged,
                                                        labelCache = labelCache,
                                                        sharpCache = sharpCache,
                                                        flatCache = flatCache,
                                                        staffLayout = staffLayout,
                                                        showFingerings = showFingerings
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
                                                        color = colorScheme.outlineVariant.copy(alpha = 0.55f),
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
                                                        enabledTrackIndices = effectiveStaffTrackIndices,
                                                        transposition = transposition
                                                    )

                                                    drawTabMeasureContent(
                                                        drawScope = this,
                                                        startX = startX,
                                                        endX = endX,
                                                        h = size.height,
                                                        measure = measure,
                                                        activeMidiNotes = stableActiveNotes,
                                                        playingNote = playingNote,
                                                        playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                        config = tabConfig,
                                                        labelLayout = labelLayout,
                                                        tBase = tabBaseline,
                                                        textMeasurer = textMeasurer,
                                                        isFinalBar = (mIdx == score.measures.lastIndex),
                                                        isMeasureActive = isPlayheadInThisMeasure,
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
    repeatEnd: Boolean = false,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.55f, 1.6f)
    val finalStroke = 2.8f * scale
    val thinStroke = 1.0f * scale
    val gap = 3.5f * scale

    if (repeatEnd) {
        val midY = (topY + bottomY) / 2f
        val dotRadius = 1.9f * scale
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 4.5f * scale, midY - 5f * scale))
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 4.5f * scale, midY + 5f * scale))
        drawLine(color, Offset(x - gap, topY), Offset(x - gap, bottomY), strokeWidth = thinStroke)
        drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = finalStroke)
        return
    }

    if (repeatStart) {
        val midY = (topY + bottomY) / 2f
        val dotRadius = 1.9f * scale
        drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = finalStroke)
        drawLine(color, Offset(x + gap, topY), Offset(x + gap, bottomY), strokeWidth = thinStroke)
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 4.5f * scale, midY - 5f * scale))
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 4.5f * scale, midY + 5f * scale))
        return
    }

    when (type) {
        "double" -> {
            drawLine(color, Offset(x - gap, topY), Offset(x - gap, bottomY), strokeWidth = thinStroke)
            drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = thinStroke)
        }
        "final" -> {
            drawLine(color, Offset(x - gap - 1f * scale, topY), Offset(x - gap - 1f * scale, bottomY), strokeWidth = thinStroke)
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
    repeatEnd: Boolean = false,
    lineSpacing: Float = 12f
) {
    val scale = (lineSpacing / 12f).coerceIn(0.55f, 1.6f)
    val finalStroke = 2.8f * scale
    val thinStroke = 1.0f * scale
    val gap = 3.5f * scale

    if (repeatEnd) {
        val trebleMid = (trebleTop + trebleBottom) / 2f
        val bassMid = (bassTop + bassBottom) / 2f
        val dotRadius = 1.9f * scale
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 4.5f * scale, trebleMid - 5f * scale))
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 4.5f * scale, trebleMid + 5f * scale))
        if (bassTop != trebleTop) {
            drawCircle(color, radius = dotRadius, center = Offset(x - gap - 4.5f * scale, bassMid - 5f * scale))
            drawCircle(color, radius = dotRadius, center = Offset(x - gap - 4.5f * scale, bassMid + 5f * scale))
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
        val dotRadius = 1.9f * scale
        drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
        if (bassTop != trebleTop) drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
        drawLine(color, Offset(x + gap, trebleTop), Offset(x + gap, trebleBottom), strokeWidth = thinStroke)
        if (bassTop != trebleTop) drawLine(color, Offset(x + gap, bassTop), Offset(x + gap, bassBottom), strokeWidth = thinStroke)

        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 4.5f * scale, trebleMid - 5f * scale))
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 4.5f * scale, trebleMid + 5f * scale))
        if (bassTop != trebleTop) {
            drawCircle(color, radius = dotRadius, center = Offset(x + gap + 4.5f * scale, bassMid - 5f * scale))
            drawCircle(color, radius = dotRadius, center = Offset(x + gap + 4.5f * scale, bassMid + 5f * scale))
        }
        return
    }

    when (type) {
        "final" -> {
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
            if (bassTop != trebleTop) drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
            drawLine(color, Offset(x - gap - 1f * scale, trebleTop), Offset(x - gap - 1f * scale, trebleBottom), strokeWidth = thinStroke)
            if (bassTop != trebleTop) drawLine(color, Offset(x - gap - 1f * scale, bassTop), Offset(x - gap - 1f * scale, bassBottom), strokeWidth = thinStroke)
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
        val maskBgColor = colorScheme?.background ?: (if (config.isDark) Color(0xFF0D0B14) else Color.White)
        val scale = (config.lineSpacing / 12f).coerceIn(0.55f, 1.6f)
        val tabClefH = config.lineSpacing * 5.0f
        val tabClefW = tabClefH * 0.33f

        if (drawClefHeader) {
            with(MusicVectorSymbols) {
                drawTabClef(
                    color = clefTextColor,
                    topLeft = Offset(startX + 0.55f * config.lineSpacing, tBase),
                    size = Size(tabClefW, tabClefH)
                )
            }

            val pitchNames = arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
            for (sIdx in 0..5) {
                val midi = tuning.getOrElse(sIdx) { intArrayOf(64, 59, 55, 50, 45, 40)[sIdx] }
                val sName = pitchNames[((midi % 12) + 12) % 12]
                val sLayout = textMeasurer.measure(sName, TextStyle(color = clefTextColor.copy(alpha = 0.60f), fontSize = (6.0f * scale).sp, fontWeight = FontWeight.Bold))
                val sy = tBase + (sIdx * config.lineSpacing) - (sLayout.size.height / 2f)
                drawText(sLayout, topLeft = Offset((startX + 1.5f * scale).coerceAtLeast(0f), sy))
            }
        }

        if (drawTimeHeader) {
            val tsX = startX + (if (drawClefHeader) (0.8f * config.lineSpacing) + tabClefW + (0.4f * config.lineSpacing) else 0.6f * config.lineSpacing)
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
                color = clefTextColor.copy(alpha = 0.65f),
                repeatStart = true,
                repeatEnd = false,
                lineSpacing = config.lineSpacing
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
            color = clefTextColor.copy(alpha = 0.55f),
            repeatStart = false,
            repeatEnd = measure.repeatEnd,
            lineSpacing = config.lineSpacing
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
        val notesByPosition = layout.notes.groupBy { it.note.position }.toSortedMap()
        val orderedTabPositions = notesByPosition.keys.toList()

        for ((_, chordAtPos) in notesByPosition) {
            val arpNote = chordAtPos.firstOrNull { it.note.arpeggio != ArpeggioDirection.NONE }
            if (arpNote != null && chordAtPos.size >= 2) {
                val minY = chordAtPos.minOf { it.ny }
                val maxY = chordAtPos.maxOf { it.ny }
                drawArpeggioRoll(arpNote.nx - 0.85f * config.lineSpacing, minY, maxY, arpNote.note.arpeggio, config.primaryColor, config.lineSpacing)
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
                    style = TextStyle(color = config.primaryColor, fontSize = (9.0f * scale).sp, fontWeight = FontWeight.Bold)
                )
                drawText(chordLayout, topLeft = Offset(nx - chordLayout.size.width / 2f, tBase - 1.8f * config.lineSpacing))
            }

            // Crisp, compact fret number sizing (7.5sp scaled) so 2-digit numbers like "13" never crowd adjacent notes
            val fretFontSizeSp = with(this) {
                (config.lineSpacing * (if (note.grace) 0.54f else 0.68f)).toSp()
            }
            val fretLayout = textMeasurer.measure(
                text = tabNote.fretText,
                style = TextStyle(
                    color = if (isSounding) Color.White else clefTextColor,
                    fontSize = fretFontSizeSp,
                    lineHeight = fretFontSizeSp,
                    fontWeight = FontWeight.Bold
                )
            )

            val padX = config.lineSpacing * 0.14f
            val padY = config.lineSpacing * 0.04f
            val maskW = (fretLayout.size.width + padX * 2f).coerceAtLeast(config.lineSpacing * 0.48f)
            val maskH = (fretLayout.size.height + padY * 2f).coerceAtMost(config.lineSpacing * 0.68f)

            drawRoundRect(
                color = if (isSounding) config.primaryColor else maskBgColor,
                topLeft = Offset(nx - maskW / 2f, ny - maskH / 2f),
                size = Size(maskW, maskH),
                cornerRadius = CornerRadius(maskH / 2f, maskH / 2f)
            )

            drawText(fretLayout, topLeft = Offset(nx - fretLayout.size.width / 2f, ny - fretLayout.size.height / 2f))

            if (config.showLyrics && note.lyric.isNotEmpty() && tabNote == notesByPosition[note.position]?.firstOrNull()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.80f), fontSize = (8.5f * scale).sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                val lyricY = tBase + 5 * config.lineSpacing + (if (showRhythmStems) 2.1f * config.lineSpacing else 0.8f * config.lineSpacing)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, lyricY))
            }

            // Find the IMMEDIATELY NEXT note on the same string (must be at the very next rhythmic position, never jumping across other notes!)
            val curPosIdx = orderedTabPositions.indexOf(note.position)
            val immediateNextPos = if (curPosIdx >= 0 && curPosIdx + 1 < orderedTabPositions.size) orderedTabPositions[curPosIdx + 1] else null
            val nextOnString = if (immediateNextPos != null) {
                notesByPosition[immediateNextPos]?.firstOrNull { it.stringIdx == tabNote.stringIdx }
            } else null

            when (note.articulation) {
                TabArticulation.HAMMER_ON, TabArticulation.PULL_OFF -> {
                    // NEVER draw a hammer-on or pull-off curve unless there is an actual target note in this measure!
                    if (nextOnString != null) {
                        val label = if (note.articulation == TabArticulation.HAMMER_ON) MusicSymbols.HAMMER_ON else MusicSymbols.PULL_OFF
                        val targetX = nextOnString.nx
                        val midX = (nx + targetX) / 2f
                        drawEngravedCurve(
                            startX = nx + 2f * scale,
                            startY = ny - maskH * 0.48f,
                            endX = targetX - 2f * scale,
                            endY = ny - maskH * 0.48f,
                            direction = -1f,
                            color = config.primaryColor,
                            lineSpacing = config.lineSpacing
                        )
                        val hpLayout = textMeasurer.measure(label, TextStyle(color = config.primaryColor, fontSize = (7.0f * scale).sp, fontWeight = FontWeight.Bold))
                        drawText(hpLayout, topLeft = Offset(midX - hpLayout.size.width / 2f, ny - config.lineSpacing * 1.05f))
                    }
                }
                TabArticulation.SLIDE_UP, TabArticulation.LEGATO_SLIDE_UP -> {
                    if (nextOnString != null) {
                        val sStartX = nx + maskW / 2f + 1f
                        val sEndX = (nextOnString.nx - maskW / 2f - 1f).coerceAtLeast(sStartX + 4f * scale)
                        drawLine(config.primaryColor, Offset(sStartX, ny + 2.5f * scale), Offset(sEndX, ny - 2.5f * scale), strokeWidth = 1.3f * scale)
                    }
                }
                TabArticulation.SLIDE_DOWN, TabArticulation.LEGATO_SLIDE_DOWN -> {
                    if (nextOnString != null) {
                        val sStartX = nx + maskW / 2f + 1f
                        val sEndX = (nextOnString.nx - maskW / 2f - 1f).coerceAtLeast(sStartX + 4f * scale)
                        drawLine(config.primaryColor, Offset(sStartX, ny - 2.5f * scale), Offset(sEndX, ny + 2.5f * scale), strokeWidth = 1.3f * scale)
                    }
                }
                TabArticulation.RELEASE -> {
                    val rLayout = textMeasurer.measure(MusicSymbols.RELEASE, TextStyle(color = config.primaryColor, fontSize = (7.0f * scale).sp, fontWeight = FontWeight.Bold))
                    drawText(rLayout, topLeft = Offset(nx - rLayout.size.width / 2f, ny - config.lineSpacing * 0.85f))
                }
                TabArticulation.TAP -> {
                    val tLayout = textMeasurer.measure("T", TextStyle(color = config.primaryColor, fontSize = (7.2f * scale).sp, fontWeight = FontWeight.Black))
                    drawText(tLayout, topLeft = Offset(nx - tLayout.size.width / 2f, ny - config.lineSpacing * 0.88f))
                }
                TabArticulation.DEAD_NOTE, TabArticulation.NONE -> {}
            }

            // CRITICAL FIX FOR TAB TIES/SLURS:
            // 1. NEVER draw a tie or slur on TAB if `nextOnString == null` (no dangling curves at end of measure!).
            // 2. Only draw a melodic slur on TAB when `note.slurStart && !note.tieStart`, `nextOnString != null`,
            //    `nextOnString.fretValue != tabNote.fretValue`, and the beat has at most 2 notes.
            if (note.slurStart && !note.tieStart && nextOnString != null &&
                nextOnString.fretValue != tabNote.fretValue &&
                (notesByPosition[note.position]?.size ?: 1) <= 2 &&
                note.articulation != TabArticulation.HAMMER_ON && note.articulation != TabArticulation.PULL_OFF
            ) {
                val curveDir = if (tabNote.stringIdx <= 2) -1f else 1f
                val anchorY = ny + curveDir * (maskH * 0.48f)
                drawEngravedCurve(
                    startX = nx + 2.5f * scale,
                    startY = anchorY,
                    endX = nextOnString.nx - 2.5f * scale,
                    endY = anchorY,
                    direction = curveDir,
                    color = clefTextColor.copy(alpha = 0.78f),
                    lineSpacing = config.lineSpacing,
                    isDashed = false
                )
            }

            if (note.pickDirection != PickDirection.NONE && tabNote == notesByPosition[note.position]?.firstOrNull()) {
                val py = tBase - 0.9f * config.lineSpacing
                if (note.pickDirection == PickDirection.DOWN) {
                    drawLine(clefTextColor, Offset(nx - 3f * scale, py), Offset(nx + 3f * scale, py), strokeWidth = 1.3f * scale)
                    drawLine(clefTextColor, Offset(nx - 3f * scale, py), Offset(nx - 3f * scale, py + 4.5f * scale), strokeWidth = 1.1f * scale)
                    drawLine(clefTextColor, Offset(nx + 3f * scale, py), Offset(nx + 3f * scale, py + 4.5f * scale), strokeWidth = 1.1f * scale)
                } else {
                    drawLine(clefTextColor, Offset(nx - 3f * scale, py), Offset(nx, py + 5f * scale), strokeWidth = 1.2f * scale)
                    drawLine(clefTextColor, Offset(nx + 3f * scale, py), Offset(nx, py + 5f * scale), strokeWidth = 1.2f * scale)
                }
            }

            if (note.vibrato) {
                with(MusicVectorSymbols) {
                    drawSmuflSymbol("wiggleArpeggiatoUp", config.primaryColor, Offset(nx - 5f * scale, ny - 10f * scale), Size(10f * scale, 5.5f * scale), 400f)
                }
            }

            if (note.fermata && tabNote == notesByPosition[note.position]?.firstOrNull()) {
                drawFermataSymbol(nx, tBase - 1.1f * config.lineSpacing, clefTextColor, config.lineSpacing)
            }

            note.whammy?.let { whammyInfo ->
                drawWhammyDip(Offset(nx, tBase), whammyInfo.steps, config.primaryColor, textMeasurer, config.lineSpacing)
            }

            note.bend?.let { bend ->
                drawBend(Offset(nx, ny), bend, config.primaryColor, textMeasurer, config.lineSpacing)
            }
            note.barre?.let { barre ->
                val bTop = tBase + (barre.startString.coerceIn(0, 5) * config.lineSpacing)
                val bBottom = tBase + (barre.endString.coerceIn(0, 5) * config.lineSpacing)
                drawBarre(barre, nx - 0.85f * config.lineSpacing, bTop, bBottom, config.primaryColor.copy(alpha = 0.65f), textMeasurer, config.lineSpacing)
            }
        }

        fun renderSpanOnTab(predicate: (ScoreNote) -> Boolean, label: String, yPos: Float) {
            var idx = 0
            while (idx < orderedTabPositions.size) {
                val pos = orderedTabPositions[idx]
                val notesAtP = notesByPosition[pos] ?: emptyList()
                if (notesAtP.any { predicate(it.note) }) {
                    var endIdx = idx
                    while (endIdx + 1 < orderedTabPositions.size && (notesByPosition[orderedTabPositions[endIdx + 1]]?.any { predicate(it.note) } == true)) {
                        endIdx++
                    }
                    val startXSpan = notesAtP.first().nx - 3f * scale
                    val endXSpan = (notesByPosition[orderedTabPositions[endIdx]]?.first()?.nx ?: startXSpan) + 5f * scale
                    drawHorizontalDashedBracket(label, startXSpan, endXSpan, yPos, hookDir = 1f, clefTextColor.copy(alpha = 0.72f), textMeasurer, config.lineSpacing)
                    idx = endIdx + 1
                } else {
                    idx++
                }
            }
        }
        renderSpanOnTab({ it.palmMute }, "P.M.", tBase - 0.85f * config.lineSpacing)
        renderSpanOnTab({ it.letRing }, "let ring", tBase - 1.55f * config.lineSpacing)

        if (showRhythmStems) {
            val stemTopY = tBase + (5f * config.lineSpacing) + (0.4f * config.lineSpacing)
            val stemBottomY = stemTopY + (1.25f * config.lineSpacing)
            val posMap = layout.notes.filter { !it.note.grace }.groupBy { it.note.position }.toSortedMap()
            val positions = posMap.keys.toList()

            positions.forEach { pos ->
                val notesAtPos = posMap[pos] ?: return@forEach
                val rep = notesAtPos.first()
                val dur = notesAtPos.minOf { it.note.duration }
                val sx = rep.nx
                val stemColor = clefTextColor.copy(alpha = 0.65f)

                if (dur < 4.0f) {
                    val topStart = if (dur >= 2.0f) stemTopY + (0.45f * config.lineSpacing) else stemTopY
                    drawLine(stemColor, Offset(sx, topStart), Offset(sx, stemBottomY), strokeWidth = 1.05f * scale)
                }
                if (notesAtPos.any { it.note.dotted || it.note.isDotted }) {
                    drawCircle(stemColor, radius = 1.5f * scale, center = Offset(sx + 3.8f * scale, stemBottomY - 2.5f * scale))
                    if (notesAtPos.any { it.note.doubleDotted }) {
                        drawCircle(stemColor, radius = 1.5f * scale, center = Offset(sx + 7.2f * scale, stemBottomY - 2.5f * scale))
                    }
                }
            }

            val beatGroups = positions.groupBy { floor(it).toInt() }
            for ((_, beatPosList) in beatGroups) {
                val eighthPositions = beatPosList.filter { p -> (posMap[p]?.minOf { it.note.duration } ?: 1f) <= 0.75f }
                if (eighthPositions.size >= 2) {
                    val x1 = posMap[eighthPositions.first()]!!.first().nx
                    val x2 = posMap[eighthPositions.last()]!!.first().nx
                    drawLine(clefTextColor.copy(alpha = 0.72f), Offset(x1, stemBottomY), Offset(x2, stemBottomY), strokeWidth = 2.1f * scale)

                    val sixteenthPositions = eighthPositions.filter { p -> (posMap[p]?.minOf { it.note.duration } ?: 1f) <= 0.35f }
                    if (sixteenthPositions.size >= 2) {
                        val sx1 = posMap[sixteenthPositions.first()]!!.first().nx
                        val sx2 = posMap[sixteenthPositions.last()]!!.first().nx
                        drawLine(clefTextColor.copy(alpha = 0.72f), Offset(sx1, stemBottomY - 3.5f * scale), Offset(sx2, stemBottomY - 3.5f * scale), strokeWidth = 1.9f * scale)
                    }
                } else if (eighthPositions.size == 1) {
                    val p = eighthPositions.first()
                    val x = posMap[p]!!.first().nx
                    val dur = posMap[p]!!.minOf { it.note.duration }
                    drawLine(clefTextColor.copy(alpha = 0.72f), Offset(x, stemBottomY), Offset(x + 5f * scale, stemBottomY - 3.5f * scale), strokeWidth = 1.6f * scale)
                    if (dur <= 0.35f) {
                        drawLine(clefTextColor.copy(alpha = 0.72f), Offset(x, stemBottomY - 3.5f * scale), Offset(x + 5f * scale, stemBottomY - 7f * scale), strokeWidth = 1.6f * scale)
                    }
                }
            }
        }

        layout.rests.forEach { rest ->
            val rx = layout.computedXByPosition[rest.position]
                ?: (systemContentStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            val ry = tBase + 2.5f * config.lineSpacing
            val restScale = config.lineSpacing / 14f
            val restColor = clefTextColor.copy(alpha = 0.55f)

            with(MusicVectorSymbols) {
                when {
                    rest.duration >= 8.0f -> drawRestDoubleWhole(restColor, Size(12f * restScale, config.lineSpacing), topLeft = Offset(rx - 6f * restScale, ry))
                    rest.duration >= 3.5f -> {
                        val centerMeasureX = (systemContentStartX + endX) / 2f
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

        if (measure.segno) {
            with(MusicVectorSymbols) {
                drawSegno(config.primaryColor, Size(16f * scale, 16f * scale), topLeft = Offset(startX + 4f * scale, tBase - 20f * scale))
            }
        }
        if (measure.coda) {
            with(MusicVectorSymbols) {
                drawCoda(config.primaryColor, Size(16f * scale, 16f * scale), topLeft = Offset(startX + 22f * scale, tBase - 20f * scale))
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
                style = TextStyle(color = clefTextColor, fontSize = (8.0f * scale).sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
            )
            drawText(navLayout, topLeft = Offset(endX - navLayout.size.width - 5f * scale, tBase - 18f * scale))
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
        val targetPlayheadX = if (computedXByPosition.isNotEmpty()) {
            interpolatePlayheadX(computedXByPosition, currentBeat, systemContentStartX, usableMeasureWidth, playheadOffset)
        } else {
            systemContentStartX + (usableMeasureWidth * playheadOffset)
        }

        drawLine(
            color = config.primaryColor,
            start = Offset(targetPlayheadX, tBase - 8f),
            end = Offset(targetPlayheadX, tBase + (5 * config.lineSpacing) + 8f),
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
    if (computedXByPosition.isEmpty()) {
        return startX + (usableWidth * playheadOffset)
    }

    val sortedEntries = computedXByPosition.entries.sortedBy { it.key }
    val firstEntry = sortedEntries.first()
    val lastEntry = sortedEntries.last()

    if (targetPos <= firstEntry.key) {
        val frac = if (firstEntry.key > 0f) (targetPos / firstEntry.key).coerceIn(0f, 1f) else 1f
        return startX + (firstEntry.value - startX) * frac
    }

    val endX = startX + usableWidth
    if (targetPos >= lastEntry.key) {
        val remainingBeats = (4f - lastEntry.key).coerceAtLeast(1f)
        val frac = ((targetPos - lastEntry.key) / remainingBeats).coerceIn(0f, 1f)
        return lastEntry.value + (endX - lastEntry.value) * frac
    }

    for (i in 0 until sortedEntries.size - 1) {
        val lower = sortedEntries[i]
        val upper = sortedEntries[i + 1]
        if (targetPos >= lower.key && targetPos <= upper.key) {
            val span = upper.key - lower.key
            val frac = if (span > 0.001f) (targetPos - lower.key) / span else 0f
            return lower.value + (upper.value - lower.value) * frac
        }
    }

    return startX + (usableWidth * playheadOffset)
}
/**
 * Computes vertically stacked piano fingerings (top-to-bottom order) for a chord or single note.
 * - Treble (RH): returns top-to-bottom list e.g. [5, 3, 1] or [5, 4, 2, 1] (drawn above staff).
 * - Bass (LH): returns top-to-bottom list e.g. [1, 3, 5] or [1, 5] (drawn below staff).
 */
private fun resolveStackedChordFingerings(
    clusterBottomToTop: List<PrecomputedRenderNote>,
    isTreble: Boolean
): List<Int> {
    if (clusterBottomToTop.isEmpty()) return emptyList()
    val sortedAsc = clusterBottomToTop.sortedBy { it.midiNote } // lowest pitch first
    val count = sortedAsc.size

    // If all notes already have explicit fingering, return them in top-to-bottom visual order
    if (sortedAsc.all { it.note.fingering != null && it.note.fingering in 1..5 }) {
        return sortedAsc.reversed().map { it.note.fingering!! }
    }

    val spanSemis = (sortedAsc.last().midiNote - sortedAsc.first().midiNote).coerceAtLeast(0)

    val fingersAsc: List<Int> = if (isTreble) {
        // Right Hand (1 = thumb on lowest note, 5 = pinky on highest note)
        when (count) {
            1 -> {
                val explicit = sortedAsc[0].note.fingering
                if (explicit != null && explicit in 1..5) listOf(explicit)
                else {
                    val m = sortedAsc[0].midiNote
                    val f = when (m % 12) {
                        0, 5 -> 1
                        2, 7 -> 2
                        4, 9 -> 3
                        1, 3, 6, 8, 10 -> 3
                        else -> 4
                    }
                    listOf(f)
                }
            }
            2 -> when {
                spanSemis <= 2 -> listOf(1, 2)
                spanSemis <= 4 -> listOf(1, 3)
                spanSemis <= 6 -> listOf(1, 4)
                else -> listOf(1, 5)
            }
            3 -> {
                val midInterval = sortedAsc[1].midiNote - sortedAsc[0].midiNote
                when {
                    spanSemis <= 5 -> listOf(1, 2, 4)
                    midInterval <= 3 -> listOf(1, 2, 5)
                    else -> listOf(1, 3, 5)
                }
            }
            4 -> {
                val topInterval = sortedAsc[3].midiNote - sortedAsc[2].midiNote
                if (topInterval <= 2) listOf(1, 2, 4, 5) else listOf(1, 2, 3, 5)
            }
            else -> listOf(1, 2, 3, 4, 5).take(count)
        }
    } else {
        // Left Hand (5 = pinky on lowest note, 1 = thumb on highest note)
        when (count) {
            1 -> {
                val explicit = sortedAsc[0].note.fingering
                if (explicit != null && explicit in 1..5) listOf(explicit)
                else {
                    val m = sortedAsc[0].midiNote
                    val f = when {
                        m < 45 -> 5
                        m < 50 -> 4
                        m < 55 -> 3
                        m < 58 -> 2
                        else -> 1
                    }
                    listOf(f)
                }
            }
            2 -> when {
                spanSemis <= 2 -> listOf(2, 1)
                spanSemis <= 4 -> listOf(3, 1)
                spanSemis <= 6 -> listOf(4, 1)
                else -> listOf(5, 1)
            }
            3 -> {
                val lowInterval = sortedAsc[1].midiNote - sortedAsc[0].midiNote
                if (lowInterval >= 5) listOf(5, 2, 1) else listOf(5, 3, 1)
            }
            4 -> listOf(5, 3, 2, 1)
            else -> listOf(5, 4, 3, 2, 1).take(count)
        }
    }

    // Return in top-to-bottom vertical drawing order (highest pitch's finger first)
    return fingersAsc.reversed()
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
    staffLayout: PrecomputedStaffLayout? = null, showFingerings: Boolean = true
) {
    with(drawScope) {
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))
        val trebleBaseline = config.trebleBaseline
        val bassBaseline = config.bassBaseline
        val staffLineColor = colorScheme?.outlineVariant ?: (if (config.isDark) Color(0xFF5A5B62) else Color(0xFF9E9E9E))
        val noteScale = (config.lineSpacing / 12f).coerceIn(0.45f, 2.0f)

        val clefH = config.lineSpacing * 6.5f
        val clefW = clefH * 0.357f
        val bassH = config.lineSpacing * 3.25f
        val bassW = bassH * 0.81f
        val maxClefW = maxOf(clefW, bassW)

        if (drawClefHeader) {
            val trebleClefTop = (trebleBaseline + 3f * config.lineSpacing) - (0.6316f * clefH)
            if (!config.isRightHandHidden) {
                with(MusicVectorSymbols) {
                    drawTrebleClef(
                        color = clefTextColor,
                        topLeft = Offset(startX + 0.45f * config.lineSpacing, trebleClefTop),
                        size = Size(clefW, clefH)
                    )
                }
            }

            val bassClefTop = (bassBaseline + 1f * config.lineSpacing) - (0.3171f * bassH)
            if (!config.isLeftHandHidden) {
                with(MusicVectorSymbols) {
                    drawBassClef(
                        color = clefTextColor,
                        topLeft = Offset(startX + 0.55f * config.lineSpacing, bassClefTop),
                        size = Size(bassW, bassH)
                    )
                }
            }
        }

        val ksCount = measure.keySignature.accidentalsCount
        val ksIsSharp = measure.keySignature.isSharp
        val ksStepX = 0.65f * config.lineSpacing
        val ksStartX = startX + (if (drawClefHeader) maxClefW + 0.55f * config.lineSpacing else 0.45f * config.lineSpacing)

        if (drawKeyHeader && ksCount > 0) {
            val accH = config.lineSpacing * 1.30f
            val accW = if (ksIsSharp) accH * 0.38f else accH * 0.32f
            val trebleSharpOffsets = floatArrayOf(0.0f, 1.5f, -0.5f, 1.0f, 2.5f, 0.5f, 2.0f)
            val bassSharpOffsets = floatArrayOf(1.0f, 2.5f, 0.5f, 2.0f, 3.5f, 1.5f, 3.0f)
            val trebleFlatOffsets = floatArrayOf(2.0f, 0.5f, 2.5f, 1.0f, 3.0f, 1.5f, 3.5f)
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
                val tsX = if (drawKeyHeader && ksCount > 0) {
                    ksStartX + (minOf(ksCount, 7) * ksStepX) + 0.45f * config.lineSpacing
                } else if (drawClefHeader) {
                    startX + maxClefW + 0.75f * config.lineSpacing
                } else {
                    startX + 0.5f * config.lineSpacing
                }
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

        val stdNoteWidth = 1.14f * config.lineSpacing
        val stdNoteHeight = 0.84f * config.lineSpacing
        val stdStemOffset = stdNoteWidth * 0.44f
        val stemStrokeWidth = (config.lineSpacing * 0.11f).coerceIn(0.95f, 1.7f)

        // 1. Draw Unified Stems and Flags per (position, clef) chord cluster
        val nonGraceNotes = visibleNotes.filter { !it.note.grace }
        val chordClusters = nonGraceNotes.groupBy { "${it.note.position}_${it.isTrebleClef}" }

        for ((_, cluster) in chordClusters) {
            val sample = cluster.first()
            val maxDur = cluster.maxOf { it.note.duration }
            if (maxDur >= 4.0f) continue

            val beamInfo = cluster.firstNotNullOfOrNull { noteBeamMap[it.note] }
            val minMidi = cluster.minOf { it.midiNote }
            val avgMidi = cluster.map { it.midiNote }.average()
            val stemDir = beamInfo?.stemDir ?: sample.polyphonicStemDir ?: if (sample.isTrebleClef) {
                if (minMidi < 70 || avgMidi < 71) -1f else 1f
            } else {
                if (minMidi <= 47) -1f else 1f
            }

            val anchorX = sample.anchorX
            val stemX = if (stemDir > 0) anchorX - stdStemOffset else anchorX + stdStemOffset

            val minNy = cluster.minOf { it.ny }
            val maxNy = cluster.maxOf { it.ny }
            val ovalTangentYOffset = stdNoteHeight * 0.16f
            val stemStartNoteY = if (stemDir > 0) (minNy + ovalTangentYOffset) else (maxNy - ovalTangentYOffset)
            val extremeTipNoteY = if (stemDir > 0) maxNy else minNy
            val defaultStemLen = config.lineSpacing * 2.55f

            val stemEndY = if (beamInfo != null) {
                beamInfo.noteStemYMap[sample.note] ?: (extremeTipNoteY + (stemDir * defaultStemLen))
            } else {
                extremeTipNoteY + (stemDir * defaultStemLen)
            }

            drawLine(clefTextColor, Offset(stemX, stemStartNoteY), Offset(stemX, stemEndY), strokeWidth = stemStrokeWidth)

            val minDur = cluster.minOf { it.note.duration }
            if (minDur < 0.90f && beamInfo == null) {
                with(MusicVectorSymbols) {
                    val flagH = config.lineSpacing * 1.80f
                    val flagW = config.lineSpacing * 0.78f
                    val flagY = if (stemDir > 0) stemEndY - flagH else stemEndY
                    when {
                        minDur <= 0.08f -> drawSmuflSymbol(if (stemDir > 0) "flag64thDown" else "flag64thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.35f), 800f)
                        minDur <= 0.16f -> drawSmuflSymbol(if (stemDir > 0) "flag32ndDown" else "flag32ndUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.22f), 800f)
                        minDur <= 0.35f -> drawSmuflSymbol(if (stemDir > 0) "flag16thDown" else "flag16thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.12f), 800f)
                        else -> drawSmuflSymbol(if (stemDir > 0) "flag8thDown" else "flag8thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH), 800f)
                    }
                }
            }
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
                val beadColor = when {
                        isSounding -> config.primaryColor
                        note.voice == 0 -> config.rightHandColor
                        note.voice == 1 -> config.leftHandColor
                        note.voice >= 2 -> Color(
                            com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX[
                                note.voice % com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX.size
                            ]
                        )
                        isTrebleClef -> config.rightHandColor
                        else -> config.leftHandColor
                    }
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
            val stemDir = beamInfo?.stemDir ?: info.polyphonicStemDir ?: if (isTrebleClef) -1f else 1f

            val duration = note.duration
            val graceFactor = if (note.grace) 0.68f else 1.0f
            val noteWidth = (when {
                duration >= 8.0f -> 1.50f * config.lineSpacing
                duration >= 4.0f -> 1.38f * config.lineSpacing
                else -> stdNoteWidth
            }) * graceFactor
            val noteHeight = stdNoteHeight * graceFactor

            if (note.grace) {
                val gStemX = nx + (noteWidth * 0.44f)
                val gStemTopY = ny - (config.lineSpacing * 1.45f)
                drawLine(clefTextColor, Offset(gStemX, ny), Offset(gStemX, gStemTopY), strokeWidth = stemStrokeWidth * 0.8f)
                drawLine(clefTextColor, Offset(gStemX, gStemTopY), Offset(gStemX + 4f * noteScale, gStemTopY + 4f * noteScale), strokeWidth = stemStrokeWidth * 0.85f)
                drawLine(clefTextColor, Offset(gStemX - 3.5f * noteScale, ny - 5f * noteScale), Offset(gStemX + 4.5f * noteScale, ny - 10f * noteScale), strokeWidth = stemStrokeWidth * 0.8f)
            }

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

                if (note.dotted || note.isDotted) {
                    val dotX = nx + (noteWidth / 2f) + (0.30f * config.lineSpacing)
                    val isOnStaffLine = (info.diatonicStep % 2 == 0)
                    val dotSize = 0.34f * config.lineSpacing
                    val dotY = if (isOnStaffLine) ny - (config.lineSpacing * 0.28f) - dotSize / 2f else ny - dotSize / 2f
                    drawSmuflSymbol("augmentationDot", beadColor, Offset(dotX, dotY), Size(dotSize, dotSize), 200f)
                }

                val articOppositeStemY = if (stemDir > 0) ny - noteHeight - (0.42f * config.lineSpacing) else ny + noteHeight + (0.24f * config.lineSpacing)
                when (note.musicalArticulation) {
                    MusicalArticulation.STACCATO -> drawStaccato(clefTextColor, Offset(nx, articOppositeStemY), radius = 0.16f * config.lineSpacing)
                    MusicalArticulation.STACCATISSIMO -> drawStaccato(clefTextColor, Offset(nx, articOppositeStemY), radius = 0.11f * config.lineSpacing)
                    MusicalArticulation.ACCENT -> drawAccent(clefTextColor, Size(0.90f * config.lineSpacing, 0.60f * config.lineSpacing), Offset(nx - 0.45f * config.lineSpacing, articOppositeStemY - 0.30f * config.lineSpacing))
                    MusicalArticulation.TENUTO -> drawTenuto(clefTextColor, Size(0.90f * config.lineSpacing, 0.20f * config.lineSpacing), Offset(nx - 0.45f * config.lineSpacing, articOppositeStemY - 0.10f * config.lineSpacing))
                    MusicalArticulation.MARCATO -> drawSmuflSymbol("articMarcatoAbove", clefTextColor, Offset(nx - 0.38f * config.lineSpacing, ny - noteHeight - 0.90f * config.lineSpacing), Size(0.76f * config.lineSpacing, 0.76f * config.lineSpacing), 400f)
                    MusicalArticulation.NONE -> {}
                }

                if (note.dynamic.isNotEmpty()) {
                    val staffTopY = if (isTrebleClef) trebleBaseline else bassBaseline
                    val staffBottomY = staffTopY + 4 * config.lineSpacing
                    val lowestNoteYInCluster = visibleNotes.filter { it.isTrebleClef == isTrebleClef && abs(it.note.position - note.position) <= 0.25f }.maxOfOrNull { it.ny } ?: ny
                    val dynY = maxOf(staffBottomY + 1.0f * config.lineSpacing, lowestNoteYInCluster + 1.15f * config.lineSpacing)
                    val dynLower = note.dynamic.lowercase().trim()
                    val dynW = config.lineSpacing * 0.95f
                    val dynH = config.lineSpacing * 1.10f
                    when (dynLower) {
                        "p" -> drawDynamic("dynamicPiano", clefTextColor.copy(alpha = 0.85f), Size(dynW, dynH), Offset(nx - dynW / 2f, dynY))
                        "f" -> drawDynamic("dynamicForte", clefTextColor.copy(alpha = 0.85f), Size(dynW, dynH), Offset(nx - dynW / 2f, dynY))
                        "sfz" -> drawDynamic("dynamicSforzando", clefTextColor.copy(alpha = 0.85f), Size(dynW * 1.3f, dynH), Offset(nx - dynW * 0.65f, dynY))
                        "rfz" -> drawDynamic("dynamicRinforzando", clefTextColor.copy(alpha = 0.85f), Size(dynW * 1.3f, dynH), Offset(nx - dynW * 0.65f, dynY))
                        else -> {
                            val dynLayout = textMeasurer.measure(
                                text = dynLower,
                                style = TextStyle(
                                    color = clefTextColor.copy(alpha = 0.88f),
                                    fontSize = (5.0f * noteScale).sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                            drawText(dynLayout, topLeft = Offset(nx - dynLayout.size.width / 2f, dynY - 2f))
                        }
                    }
                }
            }

            if (note.chordSymbol.isNotEmpty()) {
                val chordLayout = textMeasurer.measure(
                    text = note.chordSymbol,
                    style = TextStyle(color = config.primaryColor, fontSize = (9.0f * noteScale).sp, fontWeight = FontWeight.Bold)
                )
                drawText(chordLayout, topLeft = Offset(nx - chordLayout.size.width / 2f, trebleBaseline - 1.8f * config.lineSpacing))
            }



            if (config.showLabels && !note.grace && duration < 2.0f && config.lineSpacing >= 8.5f) {
                val labelResult = labelCache[(midiNote % 12 + 12) % 12]
                if (labelResult != null) {
                    val labelX = nx - (labelResult.size.width / 2f)
                    val labelY = ny - (labelResult.size.height / 2f)
                    drawText(labelResult, topLeft = Offset(labelX, labelY))
                }
            }

            if (config.showLyrics && note.lyric.isNotEmpty()) {
                val lyricFontSp = with(this) { (config.lineSpacing * 0.68f).toSp() }
                val lyricStyle = TextStyle(
                    color = clefTextColor.copy(alpha = 0.92f),
                    fontSize = lyricFontSp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic
                )
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                // Place lyrics in the clean vocal lane between Treble and Bass staves (or below Treble if single-staff)
                val lyricY = if (!config.isLeftHandHidden && bassBaseline > trebleBaseline) {
                    trebleBaseline + 4.8f * config.lineSpacing
                } else {
                    trebleBaseline + 5.2f * config.lineSpacing
                }
                drawText(
                    textLayoutResult = lyricLayout,
                    topLeft = Offset(
                        (nx - lyricLayout.size.width / 2f).coerceIn(startX + 2f, (endX - lyricLayout.size.width - 2f).coerceAtLeast(startX + 2f)),
                        lyricY
                    )
                )
            }

            val resolvedAcc = info.resolvedAccidental
            if (resolvedAcc != null && resolvedAcc != Accidental.NONE) {
                val accH = config.lineSpacing * 1.25f
                val accW = when (resolvedAcc) {
                    Accidental.DOUBLE_SHARP, Accidental.DOUBLE_FLAT -> accH * 0.44f
                    Accidental.SHARP -> accH * 0.38f
                    Accidental.NATURAL -> accH * 0.28f
                    Accidental.FLAT -> accH * 0.32f
                    Accidental.NONE -> 0f
                }

                val colShift = info.accidentalColumn * (accW + 0.16f * config.lineSpacing)
                val accRightEdge = info.anchorX - (noteWidth / 2f) - (0.22f * config.lineSpacing) - colShift
                val accLeftX = accRightEdge - accW
                val accidentalColor = clefTextColor
             with(MusicVectorSymbols) {
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol(
                            "accidentalParensLeft",
                            accidentalColor.copy(alpha = 0.6f),
                            Offset(accLeftX - 0.38f * config.lineSpacing, ny - accH / 2f),
                            Size(0.30f * config.lineSpacing, accH),
                            800f
                        )
                    }
                    when (resolvedAcc) {
                        Accidental.DOUBLE_SHARP -> drawDoubleSharp(accidentalColor, Size(accW, accH), Offset(accLeftX, ny - accH / 2f))
                        Accidental.DOUBLE_FLAT -> drawDoubleFlat(accidentalColor, Size(accW, accH), Offset(accLeftX, ny - (accH * 0.72f)))
                        Accidental.SHARP -> drawSharp(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - accH / 2f))
                        Accidental.NATURAL -> drawNatural(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - accH / 2f))
                        Accidental.FLAT -> drawFlat(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - (accH * 0.72f)))
                        Accidental.NONE -> Unit
                    }
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol(
                            "accidentalParensRight",
                            accidentalColor.copy(alpha = 0.6f),
                            Offset(accRightEdge + 0.08f * config.lineSpacing, ny - accH / 2f),
                            Size(0.30f * config.lineSpacing, accH),
                            800f
                        )
                    }
                }
            }
        }
        // 3b. Draw Collision-Free Chord & Note Fingerings (Offsets above Tuplets & Chord Symbols)
        if (showFingerings) {
            val fingerFontSp = with(this) { (config.lineSpacing * 0.50f).toSp() }
            val fingerLineStep = config.lineSpacing * 0.52f
            val fingerGlyphCache = (1..5).associateWith { num ->
                textMeasurer.measure(
                    text = num.toString(),
                    style = TextStyle(
                        color = clefTextColor.copy(alpha = 0.86f),
                        fontSize = fingerFontSp,
                        lineHeight = fingerFontSp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            for ((_, cluster) in chordClusters) {
                val isTreble = cluster.first().isTrebleClef
                val anchorX = cluster.first().anchorX
                val rawFingers = resolveStackedChordFingerings(cluster, isTreble)
                if (rawFingers.isEmpty()) continue

                // In compact split mode, show at most 2 contour fingers (top & bottom) so stacks never crowd
                val stackedFingers = if (rawFingers.size > 2) {
                    listOf(rawFingers.first(), rawFingers.last()).distinct()
                } else {
                    rawFingers
                }

                val beamInfo = cluster.firstNotNullOfOrNull { noteBeamMap[it.note] }
                val hasTupletOnCluster = cluster.any { it.note.tuplet > 0 }
                val hasChordSymOnCluster = cluster.any { it.note.chordSymbol.isNotEmpty() }
                val minNy = cluster.minOf { it.ny }
                val maxNy = cluster.maxOf { it.ny }
                val stemDir = beamInfo?.stemDir ?: cluster.first().polyphonicStemDir ?: if (isTreble) -1f else 1f

                if (isTreble) {
                    val staffTop = config.trebleBaseline
                    val highestObstacleY = if (stemDir < 0) {
                        val stemTip = beamInfo?.noteStemYMap?.get(cluster.first().note)
                            ?: (minNy - config.lineSpacing * 2.25f)
                        minOf(minNy, stemTip, staffTop)
                    } else {
                        minOf(minNy - config.lineSpacing * 0.45f, staffTop)
                    }
                    // If this beat also has an upward-stemmed tuplet number ("3") or chord symbol, shift fingerings above it!
                    val tupletClearance = if (hasTupletOnCluster && stemDir < 0) config.lineSpacing * 0.82f else 0f
                    val chordSymClearance = if (hasChordSymOnCluster) config.lineSpacing * 0.75f else 0f
                    val totalStackH = (stackedFingers.size - 1) * fingerLineStep
                    val bottomFingerY = (highestObstacleY - config.lineSpacing * 0.58f - tupletClearance - chordSymClearance)
                        .coerceAtLeast(totalStackH + 4f)

                    stackedFingers.reversed().forEachIndexed { idxFromBottom, fingerNum ->
                        val fLayout = fingerGlyphCache[fingerNum] ?: return@forEachIndexed
                        val fy = (bottomFingerY - (idxFromBottom * fingerLineStep) - fLayout.size.height / 2f)
                            .coerceIn(2f, (staffTop - fLayout.size.height - 2f).coerceAtLeast(3f))
                        drawText(
                            textLayoutResult = fLayout,
                            topLeft = Offset(anchorX - fLayout.size.width / 2f, fy)
                        )
                    }
                } else {
                    val staffBottom = config.bassBaseline + 4f * config.lineSpacing
                    val maxAllowedBottomY = if (config.isGuitarScore) {
                        config.tabBaseline - config.lineSpacing * 2.1f
                    } else {
                        size.height - config.lineSpacing * 1.3f
                    }
                    val lowestObstacleY = if (stemDir > 0) {
                        val stemTip = beamInfo?.noteStemYMap?.get(cluster.first().note)
                            ?: (maxNy + config.lineSpacing * 2.25f)
                        maxOf(maxNy, stemTip, staffBottom)
                    } else {
                        maxOf(maxNy + config.lineSpacing * 0.45f, staffBottom)
                    }
                    val tupletClearance = if (hasTupletOnCluster && stemDir > 0) config.lineSpacing * 0.80f else 0f
                    val totalStackH = (stackedFingers.size - 1) * fingerLineStep
                    val topFingerY = (lowestObstacleY + config.lineSpacing * 0.52f + tupletClearance)
                        .coerceAtMost((maxAllowedBottomY - totalStackH - 6f).coerceAtLeast(staffBottom + 2f))

                    stackedFingers.forEachIndexed { idxFromTop, fingerNum ->
                        val fLayout = fingerGlyphCache[fingerNum] ?: return@forEachIndexed
                        val fy = (topFingerY + (idxFromTop * fingerLineStep) - fLayout.size.height / 2f)
                            .coerceIn(staffBottom + 2f, (maxAllowedBottomY - fLayout.size.height).coerceAtLeast(staffBottom + 3f))
                        drawText(
                            textLayoutResult = fLayout,
                            topLeft = Offset(anchorX - fLayout.size.width / 2f, fy)
                        )
                    }
                }
            }
        }
        // 3. Draw Ties and Slurs (STRICT TARGET VALIDATION: Zero Dangling Curves & Zero Crisscrossing!)
        val nonGraceVisible = visibleNotes.filter { !it.note.grace }
        val notesByClef = nonGraceVisible.groupBy { it.isTrebleClef }

        for ((isTrebleStaff, staffNotes) in notesByClef) {
            val byPosition = staffNotes.groupBy { it.note.position }.toSortedMap()
            val positions = byPosition.keys.toList()

            positions.forEachIndexed { posIdx, pos ->
                // A tie or slur can ONLY connect to the IMMEDIATELY NEXT rhythmic position in this measure.
                // If there is no next position in this measure, DO NOT draw a dangling curve to the barline!
                val nextPos = positions.getOrNull(posIdx + 1) ?: return@forEachIndexed
                val chordAtPos = (byPosition[pos] ?: return@forEachIndexed).sortedBy { it.ny }
                val nextChord = (byPosition[nextPos] ?: return@forEachIndexed).sortedBy { it.ny }

                val sample = chordAtPos.first()
                val stemDir = chordAtPos.firstNotNullOfOrNull { noteBeamMap[it.note]?.stemDir }
                    ?: sample.polyphonicStemDir
                    ?: if (isTrebleStaff) -1f else 1f

                // A. TIES: Only draw if `nextChord` actually contains the exact same MIDI pitch!
                val matchedTies = chordAtPos.mapNotNull { src ->
                    if (!src.note.tieStart) return@mapNotNull null
                    val dst = nextChord.firstOrNull { it.midiNote == src.midiNote } ?: return@mapNotNull null
                    src to dst
                }

                val chordMidY = (chordAtPos.first().ny + chordAtPos.last().ny) * 0.5f
                matchedTies.forEachIndexed { tIdx, (src, dst) ->
                    val tieDir = if (chordAtPos.size == 1) {
                        if (stemDir > 0) -1f else 1f
                    } else {
                        // Top notes curve UP (-1f), bottom notes curve DOWN (+1f) so ties NEVER cross!
                        if (src.ny < chordMidY - 0.5f) -1f
                        else if (src.ny > chordMidY + 0.5f) 1f
                        else if (tIdx == 0) -1f else 1f
                    }

                    val yOff = tieDir * (config.lineSpacing * 0.28f)
                    val sX = src.nx + 0.45f * config.lineSpacing
                    val eX = dst.nx - 0.45f * config.lineSpacing
                    if (eX > sX + 0.30f * config.lineSpacing) {
                        drawEngravedCurve(
                            startX = sX,
                            startY = src.ny + yOff,
                            endX = eX,
                            endY = dst.ny + yOff,
                            direction = tieDir * 0.65f,
                            color = clefTextColor.copy(alpha = 0.85f),
                            lineSpacing = config.lineSpacing
                        )
                    }
                }

                // B. SLURS: Only draw if there are no ties at this position, at most ONE slur per chord,
                // and ONLY to the immediately adjacent chord when pitches actually change!
                val hasSlurStart = chordAtPos.any { it.note.slurStart && !it.note.tieStart }
                if (hasSlurStart && matchedTies.isEmpty()) {
                    val slurDir = if (stemDir > 0) -1f else 1f
                    val srcAnchor = if (slurDir < 0) chordAtPos.first() else chordAtPos.last()
                    val dstAnchor = if (slurDir < 0) nextChord.first() else nextChord.last()

                    if (srcAnchor.midiNote != dstAnchor.midiNote) {
                        val yOff = slurDir * (config.lineSpacing * 0.52f)
                        val sX = srcAnchor.nx + 0.35f * config.lineSpacing
                        val eX = dstAnchor.nx - 0.35f * config.lineSpacing
                        if (eX > sX + 0.35f * config.lineSpacing) {
                            drawEngravedCurve(
                                startX = sX,
                                startY = srcAnchor.ny + yOff,
                                endX = eX,
                                endY = dstAnchor.ny + yOff,
                                direction = slurDir * 0.85f,
                                color = clefTextColor.copy(alpha = 0.88f),
                                lineSpacing = config.lineSpacing
                            )
                        }
                    }
                }
            }
            // ADD right after the `positions.forEachIndexed` loop in Section 3 (Ties and Slurs, around Line 1610):
        // C. CROSS-BARLINE TIES (Connects TieStart on the last beat of this measure to the right barline,
        // and TieEnd on Beat 0.00 of this measure from the left note-start area)
        for ((isTrebleStaff, staffNotes) in notesByClef) {
            val byPosition = staffNotes.groupBy { it.note.position }.toSortedMap()
            val lastPos = byPosition.keys.lastOrNull()
            if (lastPos != null) {
                val lastChord = byPosition[lastPos] ?: emptyList()
                val tiedOutNotes = lastChord.filter {
                    it.note.tieStart && (it.note.position + it.note.duration >= beatsPerMeasure - 0.18f)
                }
                val chordMidY = if (lastChord.isNotEmpty()) (lastChord.minOf { it.ny } + lastChord.maxOf { it.ny }) * 0.5f else 0f
                tiedOutNotes.forEachIndexed { idx, src ->
                    val tieDir = if (src.ny < chordMidY - 0.5f) -1f else if (src.ny > chordMidY + 0.5f) 1f else if (idx % 2 == 0) -1f else 1f
                    val yOff = tieDir * (config.lineSpacing * 0.26f)
                    val sX = src.nx + 0.45f * config.lineSpacing
                    val eX = (endX - 0.15f * config.lineSpacing).coerceAtLeast(sX + 0.55f * config.lineSpacing)
                    drawEngravedCurve(
                        startX = sX,
                        startY = src.ny + yOff,
                        endX = eX,
                        endY = src.ny + yOff,
                        direction = tieDir * 0.55f,
                        color = clefTextColor.copy(alpha = 0.82f),
                        lineSpacing = config.lineSpacing
                    )
                }
            }
        }
        }

        // 4. Draw Connected Sloped Beams + Soft Translucent Span Box when Beam Group is Active
        val beamThickness = (config.lineSpacing * 0.32f).coerceIn(1.7f, 3.8f)
        val staffHighlightTop = if (config.isRightHandHidden) config.bassBaseline else config.trebleBaseline
        val staffHighlightBottom = (if (config.isLeftHandHidden) config.trebleBaseline else config.bassBaseline) + (4f * config.lineSpacing)

        beamGroups.forEach { beam ->
            val isBeamActive = isMeasureActive && playheadOffset != null && run {
                val curBeat = playheadOffset * beatsPerMeasure
                val minBeat = beam.notes.minOfOrNull { it.position } ?: 0f
                val maxBeat = beam.notes.maxOfOrNull { it.position + it.duration.coerceAtLeast(0.125f) } ?: 0f
                curBeat in (minBeat - 0.05f)..(maxBeat + 0.02f)
            }

           // Soft Translucent Blue Span Box behind active beamed group (clamped so it never overlaps the left Clef/Key/Time header!)
            if (isBeamActive) {
                val minHeaderSafeX = notesStartX + config.lineSpacing * 0.15f
                val boxLeft = (minOf(beam.firstX, beam.lastX) - config.lineSpacing * 0.85f).coerceAtLeast(minHeaderSafeX)
                val boxRight = (maxOf(beam.firstX, beam.lastX) + config.lineSpacing * 0.85f).coerceAtMost(endX - 2f)
                val boxPadY = config.lineSpacing * 0.45f
                val spanBoxColor = Color(0xFF4A90E2)
                drawRoundRect(
                    color = spanBoxColor.copy(alpha = if (config.isDark) 0.16f else 0.11f),
                    topLeft = Offset(boxLeft, staffHighlightTop - boxPadY),
                    size = Size((boxRight - boxLeft).coerceAtLeast(config.lineSpacing), (staffHighlightBottom - staffHighlightTop) + boxPadY * 2f),
                    cornerRadius = CornerRadius(config.lineSpacing * 0.4f, config.lineSpacing * 0.4f)
                )
                drawRoundRect(
                    color = spanBoxColor.copy(alpha = 0.35f),
                    topLeft = Offset(boxLeft, staffHighlightTop - boxPadY),
                    size = Size((boxRight - boxLeft).coerceAtLeast(config.lineSpacing), (staffHighlightBottom - staffHighlightTop) + boxPadY * 2f),
                    cornerRadius = CornerRadius(config.lineSpacing * 0.4f, config.lineSpacing * 0.4f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Crisp engraved beams (always sharp ink color, never bloated)
            drawLine(
                color = clefTextColor,
                start = Offset(beam.firstX, beam.startY),
                end = Offset(beam.lastX, beam.endY),
                strokeWidth = beamThickness
            )

            beam.secondarySegments.forEach { (sStart, sEnd) ->
                drawLine(
                    color = clefTextColor,
                    start = sStart,
                    end = sEnd,
                    strokeWidth = beamThickness * 0.86f
                )
            }

            beam.tertiarySegments.forEach { (tStart, tEnd) ->
                drawLine(
                    color = clefTextColor,
                    start = tStart,
                    end = tEnd,
                    strokeWidth = beamThickness * 0.80f
                )
            }

            beam.beamlets.forEach { (bStart, bEnd) ->
                drawLine(
                    color = clefTextColor,
                    start = bStart,
                    end = bEnd,
                    strokeWidth = beamThickness * 0.86f
                )
            }
        }
// REPLACE Section 5 (Tuplet Numbers & Brackets) in drawStaffMeasureContent with:
        val drawnTupletBeams = mutableSetOf<PrecomputedBeamGroup>()
        val tupletNotes = visibleNotes.filter { it.note.tuplet > 0 }
        if (tupletNotes.isNotEmpty()) {
            val tupletFontSp = with(this) { (config.lineSpacing * 0.62f).toSp() }
            val tupletBeats = tupletNotes.groupBy { "${it.isTrebleClef}_${it.beatIndex}" }
            for ((_, tGroup) in tupletBeats) {
                val tupletVal = tGroup.first().note.tuplet
                val beam = tGroup.firstNotNullOfOrNull { noteBeamMap[it.note] }
                val tLayout = textMeasurer.measure(
                    text = tupletVal.toString(),
                    style = TextStyle(
                        color = clefTextColor,
                        fontSize = tupletFontSp,
                        lineHeight = tupletFontSp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (beam != null) {
                    if (!drawnTupletBeams.add(beam)) continue
                    val midX = (beam.firstX + beam.lastX) * 0.5f
                    val midBeamY = (beam.startY + beam.endY) * 0.5f
                    val labelY = if (beam.stemDir < 0) {
                        midBeamY - tLayout.size.height - (0.22f * config.lineSpacing)
                    } else {
                        midBeamY + (0.22f * config.lineSpacing)
                    }
                    drawText(tLayout, topLeft = Offset(midX - tLayout.size.width / 2f, labelY))
                } else {
                    val minX = tGroup.minOf { it.nx }
                    val maxX = tGroup.maxOf { it.nx }
                    val tSample = tGroup.first()
                    val staffTopY = if (tSample.isTrebleClef) config.trebleBaseline else config.bassBaseline
                    val highestNoteY = tGroup.minOf { it.ny }
                    val bracketY = minOf(highestNoteY - 1.9f * config.lineSpacing, staffTopY - 0.75f * config.lineSpacing)
                    val midX = (minX + maxX) * 0.5f
                    drawText(tLayout, topLeft = Offset(midX - tLayout.size.width / 2f, bracketY - tLayout.size.height * 0.5f))
                }
            }
        }

        // 6. Render Rests
        (trebleRests + bassRests).forEach { rest ->
            val isTreble = trebleRests.contains(rest)
            val rx = layout.computedXByPosition[rest.position]
                ?: (notesStartX + config.lineSpacing + ((usableMeasureWidth - 2f * config.lineSpacing) * (rest.position / beatsPerMeasure)))
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
                        val centerMeasureX = (notesStartX + endX) / 2f
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
                drawSegno(config.primaryColor, Size(18f * noteScale, 18f * noteScale), topLeft = Offset(startX + 4f * noteScale, config.trebleBaseline - 3.0f * config.lineSpacing))
            }
        }
        if (measure.coda) {
            with(MusicVectorSymbols) {
                drawCoda(config.primaryColor, Size(18f * noteScale, 18f * noteScale), topLeft = Offset(startX + 24f * noteScale, config.trebleBaseline - 3.0f * config.lineSpacing))
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
                style = TextStyle(color = clefTextColor, fontSize = (8.5f * noteScale).sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black)
            )
            drawText(navLayout, topLeft = Offset(endX - navLayout.size.width - 5f * noteScale, config.trebleBaseline - 1.9f * config.lineSpacing))
        }
        if (measure.rehearsalMark.isNotEmpty()) {
            val rmLayout = textMeasurer.measure(measure.rehearsalMark, TextStyle(color = config.primaryColor, fontSize = (10f * noteScale).sp, fontWeight = FontWeight.Bold))
            val rmW = rmLayout.size.width.toFloat() + 7f * noteScale
            val rmH = rmLayout.size.height.toFloat() + 3f * noteScale
            val rmX = startX + 5f * noteScale
            val rmY = config.trebleBaseline - 3.4f * config.lineSpacing
            drawRect(clefTextColor.copy(alpha = 0.12f), topLeft = Offset(rmX, rmY), size = Size(rmW, rmH))
            drawRect(config.primaryColor, topLeft = Offset(rmX, rmY), size = Size(rmW, rmH), style = Stroke(width = 1.1f * noteScale))
            drawText(rmLayout, topLeft = Offset(rmX + 3.5f * noteScale, rmY + 1.5f * noteScale))
        }
        if (measure.endingNumber != null) {
            val endLayout = textMeasurer.measure("${measure.endingNumber}.", TextStyle(color = clefTextColor, fontSize = (9.0f * noteScale).sp, fontWeight = FontWeight.Bold))
            val bY = config.trebleBaseline - 2.0f * config.lineSpacing
            drawLine(clefTextColor, Offset(startX, bY), Offset(endX, bY), strokeWidth = 1.1f * noteScale)
            drawLine(clefTextColor, Offset(startX, bY), Offset(startX, bY + 0.9f * config.lineSpacing), strokeWidth = 1.1f * noteScale)
            if (measure.repeatEnd) {
                drawLine(clefTextColor, Offset(endX, bY), Offset(endX, bY + 0.9f * config.lineSpacing), strokeWidth = 1.1f * noteScale)
            }
            drawText(endLayout, topLeft = Offset(startX + 5f * noteScale, bY - 1.1f * config.lineSpacing))
        }
        // Engraved Horizontal Sustain Pedal Bracket Line (└────────┘) below Bass Staff (Hidden when Guitar TAB is below)
        if (!config.isLeftHandHidden && !config.isGuitarScore) {
            val pedBracketY = (bassBaseline + 5.65f * config.lineSpacing).coerceAtMost(size.height - 3f)
            val pedHookH = config.lineSpacing * 0.45f
            val pedStrokeW = (config.lineSpacing * 0.09f).coerceIn(0.85f, 1.4f)
            val pedBracketColor = clefTextColor.copy(alpha = 0.68f)

            val bStartX = (notesStartX + 0.4f * config.lineSpacing).coerceAtMost(endX - config.lineSpacing)
            val bEndX = (endX - 0.6f * config.lineSpacing).coerceAtLeast(bStartX + config.lineSpacing)
            if (bEndX > bStartX + config.lineSpacing && visibleNotes.any { !it.isTrebleClef }) {
                drawLine(pedBracketColor, Offset(bStartX, pedBracketY - pedHookH), Offset(bStartX, pedBracketY), strokeWidth = pedStrokeW)
                drawLine(pedBracketColor, Offset(bStartX, pedBracketY), Offset(bEndX, pedBracketY), strokeWidth = pedStrokeW)
                drawLine(pedBracketColor, Offset(bEndX, pedBracketY), Offset(bEndX, pedBracketY - pedHookH), strokeWidth = pedStrokeW)
            }
        } else if (config.isPedalPressed && isMeasureActive) {
            with(MusicVectorSymbols) {
                drawPedal(config.primaryColor.copy(alpha = 0.8f), Size(16f * noteScale, 12f * noteScale), Offset(startX + 5f * noteScale, bassBaseline + 4.8f * config.lineSpacing + 6f * noteScale))
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
//internal fun drawPlayheadStaffLine(
//    drawScope: DrawScope,
//    visibleNotes: List<PrecomputedRenderNote>,
//    computedXByPosition: Map<Float, Float>,
//    notesStartX: Float,
//    usableMeasureWidth: Float,
//    beatsPerMeasure: Float,
//    playingNote: Int?,
//    playheadOffset: Float,
//    config: DrawingConfig
//) {
//    with(drawScope) {
//        val currentBeat = playheadOffset * beatsPerMeasure
//        val targetPlayheadX = if (computedXByPosition.isNotEmpty()) {
//            interpolatePlayheadX(computedXByPosition, currentBeat, notesStartX, usableMeasureWidth, playheadOffset)
//        } else {
//            notesStartX + (usableMeasureWidth * playheadOffset)
//        }
//
//        val topStaffY = if (config.isRightHandHidden) config.bassBaseline else config.trebleBaseline
//        val bottomStaffY = (if (config.isLeftHandHidden) config.trebleBaseline else config.bassBaseline) + (4f * config.lineSpacing)
//
//        val capsuleHalfW = (config.lineSpacing * 0.95f).coerceIn(9f, 18f)
//        val capsulePadY = config.lineSpacing * 0.75f
//        val capsuleTop = topStaffY - capsulePadY
//        val capsuleBottom = bottomStaffY + capsulePadY
//        val capsuleWidth = capsuleHalfW * 2f
//        val capsuleHeight = (capsuleBottom - capsuleTop).coerceAtLeast(config.lineSpacing * 4f)
//        val cornerRad = CornerRadius(capsuleHalfW, capsuleHalfW)
//        val capsuleBlue = Color(0xFF3B82F6)
//
//        // 1. Soft translucent blue interior fill
//        drawRoundRect(
//            color = capsuleBlue.copy(alpha = if (config.isDark) 0.22f else 0.16f),
//            topLeft = Offset(targetPlayheadX - capsuleHalfW, capsuleTop),
//            size = Size(capsuleWidth, capsuleHeight),
//            cornerRadius = cornerRad
//        )
//
//        // 2. Crisp white inner border ring
//        val innerInset = 1.6f.dp.toPx()
//        drawRoundRect(
//            color = Color.White.copy(alpha = 0.92f),
//            topLeft = Offset(targetPlayheadX - capsuleHalfW + innerInset, capsuleTop + innerInset),
//            size = Size(capsuleWidth - innerInset * 2f, capsuleHeight - innerInset * 2f),
//            cornerRadius = CornerRadius((capsuleHalfW - innerInset).coerceAtLeast(2f), (capsuleHalfW - innerInset).coerceAtLeast(2f)),
//            style = Stroke(width = 1.5f.dp.toPx())
//        )
//
//        // 3. Vibrant blue outer border capsule
//        drawRoundRect(
//            color = capsuleBlue,
//            topLeft = Offset(targetPlayheadX - capsuleHalfW, capsuleTop),
//            size = Size(capsuleWidth, capsuleHeight),
//            cornerRadius = cornerRad,
//            style = Stroke(width = 2.2f.dp.toPx())
//        )
//    }
//}

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
        val targetPlayheadX = if (computedXByPosition.isNotEmpty()) {
            interpolatePlayheadX(computedXByPosition, currentBeat, notesStartX, usableMeasureWidth, playheadOffset)
        } else {
            notesStartX + (usableMeasureWidth * playheadOffset)
        }

        // Keep staff logic from the capsule code to properly adjust bounds when a hand is hidden
        val topStaffY = if (config.isRightHandHidden) config.bassBaseline else config.trebleBaseline
        val bottomStaffY = (if (config.isLeftHandHidden) config.trebleBaseline else config.bassBaseline) + (4f * config.lineSpacing)

        // Add padding above and below the staff lines
        val paddingY = 15f
        val lineTop = topStaffY - paddingY
        val lineBottom = bottomStaffY + paddingY

        // Draw the simple vertical playhead line in yellow
        drawLine(
            color = Color(0xFFFFD700), // Vibrant gold/yellow color
            start = Offset(targetPlayheadX, lineTop),
            end = Offset(targetPlayheadX, lineBottom),
            strokeWidth = 2.5f.dp.toPx()
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
        val lineColor = colorSchemeLineColor(config)
        val strokeW = (spacing * 0.088f).coerceIn(0.8f, 1.35f)
        val leftBarStroke = (spacing * 0.14f).coerceIn(1.2f, 2.2f)

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
                    strokeWidth = leftBarStroke
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
                        strokeWidth = leftBarStroke
                    )
                }
            }
        }
    }
}

private fun colorSchemeLineColor(config: DrawingConfig): Color {
    return config.colorScheme?.outlineVariant ?: (if (config.isDark) Color(0xFF6A6B72) else Color(0xFF8C8C92))
}
internal fun renderScoreSystemToDrawScope(
    drawScope: DrawScope,
    score: Score,
    systemMeasures: List<Pair<Int, Measure>>,
    startX: Float,
    endX: Float,
    trebleBaseline: Float,
    bassBaseline: Float,
    tabBaseline: Float,
    staffLineSpacing: Float,
    tabLineSpacing: Float,
    showStaff: Boolean,
    showTab: Boolean,
    showMeasureNumbers: Boolean,
    showNoteLetters: Boolean,
    showFingerings: Boolean = true,
    showLyrics: Boolean,
    isSplitStaff: Boolean,
    isDarkTheme: Boolean,
    pageBackgroundColor: Color,
    primaryColor: Color,
    rightHandColor: Color,
    leftHandColor: Color,
    inkColor: Color,
    staffLineColor: Color,
    tabPillBgColor: Color,
    isLeftHandHidden: Boolean,
    isRightHandHidden: Boolean,
    enabledTrackIndices: Map<Int, Boolean>,
    transposition: Int,
    textMeasurer: TextMeasurer,
    layoutCache: MeasureLayoutCache
) {
    with(drawScope) {
        if (systemMeasures.isEmpty()) return

        val effectiveLeftHandHidden = isLeftHandHidden || !isSplitStaff

        // --- ADDED FIX: Resolve drum vs melodic track filter for PDF & system rendering ---
        val drumTrackIndices = score.tracks.filter { it.channel == 9 }.map { it.trackIndex }.toSet()
        val hasEnabledMelodicTracks = score.tracks.isEmpty() || score.tracks.any { it.channel != 9 && (enabledTrackIndices[it.trackIndex] ?: it.isEnabled) }
        val effectiveStaffTrackIndices = run {
            val map = enabledTrackIndices.toMutableMap()
            if (hasEnabledMelodicTracks) {
                drumTrackIndices.forEach { dIdx -> map[dIdx] = false }
            }
            map.toMap()
        }
        // -------------------------------------------------------------------------------

        val availableWidth = (endX - startX).coerceAtLeast(100f)

        val measureWeights = systemMeasures.mapIndexed { idxInRow, (globalIdx, m) ->
            val distinctPosCount = m.notes.filter { !it.isRest }.map { it.position }.distinct().size
            val headerWeight = if (idxInRow == 0) 2.2f else if (globalIdx == 0) 1.8f else 0.5f
            headerWeight + distinctPosCount.coerceAtLeast(2).toFloat().pow(0.75f)
        }
        val totalWeight = measureWeights.sum().coerceAtLeast(1f)

        val noteScale = (staffLineSpacing / 12f).coerceIn(0.55f, 1.6f)
        val labelCache = (0..11).associateWith { i ->
            textMeasurer.measure(
                text = MusicConstants.NOTE_LABELS[i].take(1),
                style = TextStyle(
                    color = Color.White,
                    fontSize = (6.4f * noteScale).sp,
                    lineHeight = (6.8f * noteScale).sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            )
        }
        val sharpCache = textMeasurer.measure("♯", TextStyle(color = primaryColor, fontSize = (13f * noteScale).sp, fontWeight = FontWeight.Bold))
        val flatCache = textMeasurer.measure("♭", TextStyle(color = primaryColor, fontSize = (10.5f * noteScale).sp, fontWeight = FontWeight.Bold))

        if (showStaff && !effectiveLeftHandHidden && !isRightHandHidden) {
            drawGrandStaffBrace(
                x = (startX - 3f * noteScale).coerceAtLeast(2f),
                topY = trebleBaseline,
                bottomY = bassBaseline + (4f * staffLineSpacing),
                color = inkColor.copy(alpha = 0.85f),
                lineSpacing = staffLineSpacing
            )
        }

        var runningStartX = startX
        systemMeasures.forEachIndexed { idxInRow, (globalIdx, measure) ->
            val weightFrac = measureWeights[idxInRow] / totalWeight
            val mSpanW = availableWidth * weightFrac
            val mStartX = snap(runningStartX)
            val mEndX = if (idxInRow == systemMeasures.lastIndex) snap(endX) else snap(runningStartX + mSpanW)
            runningStartX += mSpanW

            val prevMeasure = if (globalIdx > 0) score.measures.getOrNull(globalIdx - 1) else null
            val nextMeasure = if (globalIdx + 1 < score.measures.size) score.measures.getOrNull(globalIdx + 1) else null
            val isRowStart = (idxInRow == 0)
            val keyChanged = prevMeasure != null && prevMeasure.keySignature != measure.keySignature
            val timeChanged = prevMeasure != null && (
                    prevMeasure.timeSignatureNumerator != measure.timeSignatureNumerator ||
                            prevMeasure.timeSignatureDenominator != measure.timeSignatureDenominator
                    )

            val syntheticScheme = if (isDarkTheme) {
                darkColorScheme(
                    background = pageBackgroundColor,
                    onSurface = inkColor,
                    outlineVariant = staffLineColor,
                    surfaceVariant = tabPillBgColor
                )
            } else {
                lightColorScheme(
                    background = pageBackgroundColor,
                    onSurface = inkColor,
                    outlineVariant = staffLineColor,
                    surfaceVariant = tabPillBgColor
                )
            }

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
                isDark = isDarkTheme,
                isTab = false,
                tabBaseline = tabBaseline,
                isPedalPressed = false,
                showLabels = showNoteLetters,
                showLyrics = showLyrics,
                colorScheme = syntheticScheme
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
                isDark = isDarkTheme,
                isTab = true,
                tabBaseline = tabBaseline,
                showLabels = showNoteLetters,
                showLyrics = showLyrics,
                colorScheme = syntheticScheme
            )

            val mLabelLayout = textMeasurer.measure(
                text = "${globalIdx + 1}",
                style = TextStyle(
                    color = inkColor.copy(alpha = 0.65f),
                    fontSize = (8.5f * noteScale).sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold
                )
            )

            if (showMeasureNumbers) {
                val labelY = if (showStaff) {
                    trebleBaseline - (staffLineSpacing * 2.1f)
                } else {
                    tabBaseline - (tabLineSpacing * 2.1f)
                }
                drawText(
                    textLayoutResult = mLabelLayout,
                    topLeft = Offset(mStartX + 3f * noteScale, labelY)
                )
            }

            if (showStaff) {
                drawGrandStaffSystem(
                    ds = this,
                    startX = mStartX,
                    endX = mEndX,
                    trebleY = trebleBaseline,
                    bassY = bassBaseline,
                    spacing = staffLineSpacing,
                    isFinal = (globalIdx == score.measures.lastIndex),
                    color = staffLineColor,
                    drawLeftBar = isRowStart,
                    config = staffConfig
                )

                if (measure.repeatStart) {
                    drawGrandStaffSystemBarline(
                        type = "repeat_start",
                        x = mStartX,
                        trebleTop = trebleBaseline,
                        trebleBottom = trebleBaseline + (4f * staffLineSpacing),
                        bassTop = if (effectiveLeftHandHidden) trebleBaseline else bassBaseline,
                        bassBottom = if (effectiveLeftHandHidden) trebleBaseline + (4f * staffLineSpacing) else bassBaseline + (4f * staffLineSpacing),
                        color = inkColor.copy(alpha = 0.78f),
                        repeatStart = true,
                        repeatEnd = false,
                        lineSpacing = staffLineSpacing
                    )
                }

                val endBarType = when {
                    measure.repeatEnd -> "repeat_end"
                    globalIdx == score.measures.lastIndex -> "final"
                    measure.barline.isNotBlank() -> measure.barline
                    else -> "single"
                }
                drawGrandStaffSystemBarline(
                    type = endBarType,
                    x = mEndX,
                    trebleTop = trebleBaseline,
                    trebleBottom = trebleBaseline + (4f * staffLineSpacing),
                    bassTop = if (effectiveLeftHandHidden) trebleBaseline else bassBaseline,
                    bassBottom = if (effectiveLeftHandHidden) trebleBaseline + (4f * staffLineSpacing) else bassBaseline + (4f * staffLineSpacing),
                    color = inkColor.copy(alpha = 0.70f),
                    repeatStart = false,
                    repeatEnd = measure.repeatEnd,
                    lineSpacing = staffLineSpacing
                )

                val staffLayout = layoutCache.getStaffLayout(
                    measure = measure,
                    drawClefHeader = isRowStart,
                    drawKeyHeader = isRowStart || keyChanged,
                    drawTimeHeader = (globalIdx == 0) || timeChanged,
                    startX = mStartX,
                    endX = mEndX,
                    trebleBaseline = trebleBaseline,
                    bassBaseline = bassBaseline,
                    lineSpacing = staffLineSpacing,
                    isLeftHandHidden = effectiveLeftHandHidden,
                    isRightHandHidden = isRightHandHidden,
                    enabledTrackIndices = effectiveStaffTrackIndices,
                    transposition = transposition
                )

                drawStaffMeasureContent(
                    drawScope = this,
                    startX = mStartX,
                    endX = mEndX,
                    h = size.height,
                    measure = measure,
                    prevMeasure = prevMeasure,
                    nextMeasure = nextMeasure,
                    activeMidiNotes = emptySet(),
                    playingNote = null,
                    playheadOffset = null,
                    config = staffConfig,
                    textMeasurer = textMeasurer,
                    noteLabels = MusicConstants.NOTE_LABELS,
                    showFingerings = showFingerings,
                    diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                    isFinalBar = (globalIdx == score.measures.lastIndex),
                    isMeasureActive = false,
                    timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                    drawClefHeader = isRowStart,
                    drawKeyHeader = isRowStart || keyChanged,
                    drawTimeHeader = (globalIdx == 0) || timeChanged,
                    labelCache = labelCache,
                    sharpCache = sharpCache,
                    flatCache = flatCache,
                    staffLayout = staffLayout
                )
            }

            if (showTab) {
                drawGrandStaffSystem(
                    ds = this,
                    startX = mStartX,
                    endX = mEndX,
                    trebleY = tabBaseline,
                    bassY = tabBaseline,
                    spacing = tabLineSpacing,
                    isFinal = (globalIdx == score.measures.lastIndex),
                    color = staffLineColor,
                    drawLeftBar = isRowStart,
                    config = tabConfig
                )

                val tabLayout = layoutCache.getTabLayout(
                    measure = measure,
                    drawClefHeader = isRowStart,
                    drawTimeHeader = (globalIdx == 0) || timeChanged,
                    startX = mStartX,
                    endX = mEndX,
                    tBase = tabBaseline,
                    lineSpacing = tabLineSpacing,
                    tuning = score.tuningMidi,
                    capo = score.capo,
                    isLeftHandHidden = effectiveLeftHandHidden,
                    isRightHandHidden = isRightHandHidden,
                    enabledTrackIndices = effectiveStaffTrackIndices,
                    transposition = transposition
                )

                drawTabMeasureContent(
                    drawScope = this,
                    startX = mStartX,
                    endX = mEndX,
                    h = size.height,
                    labelLayout = mLabelLayout,
                    tBase = tabBaseline,
                    measure = measure,
                    activeMidiNotes = emptySet(),
                    playingNote = null,
                    playheadOffset = null,
                    config = tabConfig,
                    textMeasurer = textMeasurer,
                    isFinalBar = (globalIdx == score.measures.lastIndex),
                    isMeasureActive = false,
                    timeSignature = "${measure.timeSignatureNumerator}/${measure.timeSignatureDenominator}",
                    drawClefHeader = isRowStart,
                    drawTimeHeader = (globalIdx == 0) || timeChanged,
                    tuning = score.tuningMidi,
                    tabLayout = tabLayout,
                    showRhythmStems = !showStaff
                )
            }
        }
    }
}