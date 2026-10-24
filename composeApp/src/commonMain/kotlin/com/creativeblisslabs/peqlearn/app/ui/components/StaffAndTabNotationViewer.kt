package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import com.creativeblisslabs.peqlearn.app.util.GuitarShape
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import kotlin.math.pow

private fun DrawScope.drawLedgerLines(
    x: Float,
    y: Float,
    topY: Float,
    bottomY: Float,
    spacing: Float,
    color: Color
) {
    if (spacing <= 0f) return
    val lineBoundExt = 12f
    val halfSpacing = spacing / 2f
    val lineColor = color.copy(alpha = 0.85f)

    if (y < topY - halfSpacing) {
        var curL = topY - spacing
        var count = 0
        while (curL >= y - (halfSpacing * 0.8f) && count < 6) {
            drawLine(lineColor, Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = 1.3f)
            curL -= spacing
            count++
        }
    } else if (y > bottomY + halfSpacing) {
        var curL = bottomY + spacing
        var count = 0
        while (curL <= y + (halfSpacing * 0.8f) && count < 6) {
            drawLine(lineColor, Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = 1.3f)
            curL += spacing
            count++
        }
    }
}


private fun snap(value: Float): Float = kotlin.math.round(value)

// Phase 1 Performance: Precomputed data models to eliminate per-frame allocations and calculations
internal data class PrecomputedRenderNote(
    val note: ScoreNote,
    val midiNote: Int,
    val isTrebleClef: Boolean,
    val nx: Float,
    val ny: Float,
    val topY: Float,
    val bottomY: Float,
    val beatIndex: Int
)

internal data class PrecomputedBeamGroup(
    val stemDir: Float,
    val firstX: Float,
    val lastX: Float,
    val startY: Float,
    val endY: Float,
    val noteStemYMap: Map<ScoreNote, Float>,
    val isSixteenth: Boolean = false,
    val sFirstX: Float = 0f,
    val sLastX: Float = 0f,
    val sStartY: Float = 0f,
    val sEndY: Float = 0f
)

internal data class PrecomputedStaffLayout(
    val visibleNotes: List<PrecomputedRenderNote>,
    val computedXByPosition: Map<Float, Float>,
    val noteBeamMap: Map<ScoreNote, PrecomputedBeamGroup>,
    val beamGroups: List<PrecomputedBeamGroup>,
    val trebleRests: List<RestSymbol>,
    val bassRests: List<RestSymbol>,
    val usableMeasureWidth: Float,
    val notesStartX: Float,
    val beatsPerMeasure: Float
)

internal data class PrecomputedTabNote(
    val note: ScoreNote,
    val midiNote: Int,
    val nx: Float,
    val ny: Float,
    val fretText: String,
    val pillWidth: Float,
    val pillHeight: Float,
    val circleRadius: Float
)

internal data class PrecomputedTabLayout(
    val notes: List<PrecomputedTabNote>,
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
        drawHeaders: Boolean,
        startX: Float,
        endX: Float,
        trebleBaseline: Float,
        bassBaseline: Float,
        lineSpacing: Float,
        isLeftHandHidden: Boolean,
        isRightHandHidden: Boolean
    ): PrecomputedStaffLayout {
        val key = "${measure.measureIndex}_${drawHeaders}_${startX.toInt()}_${endX.toInt()}_${trebleBaseline.toInt()}_${bassBaseline.toInt()}_${lineSpacing.toInt()}_${isLeftHandHidden}_${isRightHandHidden}_${measure.notes.size}"
        return staffCache.getOrPut(key) {
            computeStaffLayout(measure, drawHeaders, startX, endX, trebleBaseline, bassBaseline, lineSpacing, isLeftHandHidden, isRightHandHidden)
        }
    }

    fun getTabLayout(
        measure: Measure,
        drawHeaders: Boolean,
        startX: Float,
        endX: Float,
        tBase: Float,
        lineSpacing: Float
    ): PrecomputedTabLayout {
        val key = "${measure.measureIndex}_${drawHeaders}_${startX.toInt()}_${endX.toInt()}_${tBase.toInt()}_${lineSpacing.toInt()}_${measure.notes.size}"
        return tabCache.getOrPut(key) {
            computeTabLayout(measure, drawHeaders, startX, endX, tBase, lineSpacing)
        }
    }

    fun clear() {
        staffCache.clear()
        tabCache.clear()
    }
}

private fun computeStaffLayout(
    measure: Measure,
    drawHeaders: Boolean,
    startX: Float,
    endX: Float,
    trebleBaseline: Float,
    bassBaseline: Float,
    lineSpacing: Float,
    isLeftHandHidden: Boolean,
    isRightHandHidden: Boolean
): PrecomputedStaffLayout {
    val symbolAreaWidth = if (drawHeaders) 85f else 0f
    val notesStartX = startX + symbolAreaWidth
    val usableMeasureWidth = (endX - notesStartX - 10f).coerceAtLeast(100f)
    val beatsPerMeasure = measure.timeSignatureNumerator.toFloat().coerceAtLeast(1f)
    val halfLineSpacing = lineSpacing / 2f

    val rawVisibleNotes = measure.notes.filter { !it.isRest }.distinctBy { "${it.position}_${it.midiNote}" }.mapNotNull { note ->
        val midiNote = note.midiNote
        val isTrebleClef = midiNote >= 60
        if (isTrebleClef && isRightHandHidden) return@mapNotNull null
        if (!isTrebleClef && isLeftHandHidden) return@mapNotNull null
        Triple(note, midiNote, isTrebleClef)
    }.sortedBy { it.first.position }

    val distinctPositions = rawVisibleNotes.map { it.first.position }.distinct().sorted()
    val minNoteGap = 24f
    val computedXByPosition = mutableMapOf<Float, Float>()

    if (distinctPositions.isNotEmpty()) {
        val minDur = rawVisibleNotes.minOfOrNull { it.first.duration.coerceAtLeast(0.125f) } ?: 1.0f
        val posDurations = distinctPositions.mapIndexed { idx, pos ->
            val nextPos = if (idx < distinctPositions.lastIndex) distinctPositions[idx + 1] else beatsPerMeasure
            val delta = (nextPos - pos).coerceAtLeast(minDur)
            (delta / minDur).toDouble().pow(0.58).toFloat()
        }
        val sumWeighted = posDurations.sum().coerceAtLeast(1f)
        val noteContentMaxX = endX - 16f
        val noteContentStartX = notesStartX + 12f
        val maxAvailableSpan = (noteContentMaxX - noteContentStartX).coerceAtLeast(40f)

        // Check if evenly distributing positions with minGap would overflow the measure
        val naturalSpacingNeeded = (distinctPositions.size - 1) * minNoteGap
        val effectiveGap = if (naturalSpacingNeeded > maxAvailableSpan && distinctPositions.size > 1) {
            maxAvailableSpan / (distinctPositions.size - 1)
        } else {
            minNoteGap
        }

        var runningX = noteContentStartX
        var runningWeight = 0f
        distinctPositions.forEachIndexed { idx, pos ->
            val fraction = (runningWeight / sumWeighted).coerceIn(0f, 1f)
            val idealX = noteContentStartX + (maxAvailableSpan * fraction)
            val targetX = if (idx == 0) idealX else maxOf(idealX, runningX + effectiveGap)
            val clampedX = targetX.coerceIn(noteContentStartX, noteContentMaxX)
            computedXByPosition[pos] = snap(clampedX)
            runningX = clampedX
            runningWeight += posDurations[idx]
        }
    }

    val visibleNotes = rawVisibleNotes.map { (note, midiNote, isTrebleClef) ->
        val nx = computedXByPosition[note.position]
            ?: snap((notesStartX + 12f + ((usableMeasureWidth - 28f) * (note.position / beatsPerMeasure))).coerceIn(notesStartX + 12f, endX - 16f))
        val diatonicStep = ChordVoicingEngine.getDiatonicStep(midiNote)
        val ny = if (isTrebleClef) {
            trebleBaseline + (38.0f - diatonicStep) * halfLineSpacing
        } else {
            bassBaseline + (26.0f - diatonicStep) * halfLineSpacing
        }
        val topY = if (isTrebleClef) trebleBaseline else bassBaseline
        val bottomY = topY + (4 * lineSpacing)
        val beatIdx = kotlin.math.floor(note.position).toInt()

        PrecomputedRenderNote(
            note = note,
            midiNote = midiNote,
            isTrebleClef = isTrebleClef,
            nx = nx,
            ny = ny,
            topY = topY,
            bottomY = bottomY,
            beatIndex = beatIdx
        )
    }

    val noteBeamMap = mutableMapOf<ScoreNote, PrecomputedBeamGroup>()
    val precomputedBeams = mutableListOf<PrecomputedBeamGroup>()

    val groupedByClef = visibleNotes.groupBy { it.isTrebleClef }
    for ((isTreble, clefNotes) in groupedByClef) {
        val groupedByBeat = clefNotes.groupBy {
            kotlin.math.floor(it.note.position).toInt()
        }

        for ((_, beatNotes) in groupedByBeat) {
            // Only beam notes of duration eighth or shorter (<= 0.5f in beat units, up to 0.75f for dotted 8th)
            val beamEligible = beatNotes.filter { it.note.duration <= 0.75f }
            val distinctPos = beamEligible.map { it.note.position }.distinct()

            // A valid beam group requires at least 2 distinct temporal positions within a reasonable horizontal span
            if (distinctPos.size >= 2) {
                val beamedSorted = beamEligible.sortedBy { it.nx }
                val firstNote = beamedSorted.first()
                val lastNote = beamedSorted.last()
                val noteSpan = lastNote.nx - firstNote.nx

                // Do not create giant cross-measure or excessively wide beams (max beat span 140px)
                if (noteSpan in 8f..140f) {
                    val avgMidi = beamEligible.map { it.midiNote }.average()
                    // Stem direction:
                    // In screen coordinates, -1f means UPWARDS (towards top of screen, lower Y), +1f means DOWNWARDS (higher Y).
                    // For Treble: B4 is MIDI 71 (middle line). Notes >= 71 have stems pointing DOWN (+1f).
                    // Notes < 71 (below middle line, like E4, F4, G4, A4) MUST have stems pointing UP (-1f)!
                    // Crucially, for treble notes pointing DOWN (+1f), the stems must never cross into the bass staff.
                    // For Bass: D3 is MIDI 50 (middle line). Notes >= 50 stems DOWN (+1f), notes < 50 stems UP (-1f).
                    val groupStemDir = if (isTreble) {
                        if (avgMidi >= 71) 1f else -1f
                    } else {
                        if (avgMidi >= 50) 1f else -1f
                    }

                    // Stem attachment point: stems up attach to right side (+5f), stems down attach to left side (-5f)
                    val firstX = if (groupStemDir > 0) firstNote.nx - 5f else firstNote.nx + 5f
                    val lastX = if (groupStemDir > 0) lastNote.nx - 5f else lastNote.nx + 5f

                    val dx = (lastX - firstX).coerceAtLeast(1f)
                    val rawPitchSlope = (lastNote.ny - firstNote.ny) / dx
                    // Tightly clamp slope to standard musical notation angles (-0.12 to 0.12) to prevent wild diagonal beams
                    val clampedSlope = (rawPitchSlope * 0.25f).coerceIn(-0.12f, 0.12f)

                    val standardStemLength = 24f
                    // Calculate natural beam anchor based on extreme notehead
                    val baseStemY = if (groupStemDir > 0) {
                        // Stems pointing down (toward bottom/higher Y): beam line is at or below the lowest stem end
                        beamedSorted.map { it.ny + standardStemLength - (clampedSlope * (it.nx - firstX)) }.maxOrNull() ?: (firstNote.ny + standardStemLength)
                    } else {
                        // Stems pointing up (toward top/lower Y): beam line is at or above the highest stem end
                        beamedSorted.map { it.ny - standardStemLength - (clampedSlope * (it.nx - firstX)) }.minOrNull() ?: (firstNote.ny - standardStemLength)
                    }

                    // Strict staff clearance boundaries:
                    // Treble staff lines span trebleBaseline to (trebleBaseline + 4 * lineSpacing).
                    // Bass staff starts at bassBaseline = trebleBaseline + 5.5 * lineSpacing.
                    // Ledger line for Middle C is at trebleBaseline + 5 * lineSpacing.
                    // Treble beams must NEVER enter the bass clef area!
                    // Bass beams must NEVER enter the treble clef area!
                    val trebleMaxBeamY = trebleBaseline + (4.5f * lineSpacing) // Ceiling: stay above Middle C / bass staff
                    val bassMinBeamY = bassBaseline - (0.8f * lineSpacing)     // Floor: stay below treble staff

                    var constrainedStartY = baseStemY
                    if (isTreble && groupStemDir > 0) {
                        val maxRawY = maxOf(baseStemY, baseStemY + (clampedSlope * dx))
                        if (maxRawY > trebleMaxBeamY) {
                            constrainedStartY -= (maxRawY - trebleMaxBeamY)
                        }
                    } else if (!isTreble && groupStemDir < 0) {
                        val minRawY = minOf(baseStemY, baseStemY + (clampedSlope * dx))
                        if (minRawY < bassMinBeamY) {
                            constrainedStartY += (bassMinBeamY - minRawY)
                        }
                    }

                    // Enforce stem length limits (between 18f and 32f) across all notes in the beam group
                    // If any note's stem would be too short or too long, adjust constrainedStartY
                    for (n in beamedSorted) {
                        val beamYAtN = constrainedStartY + (clampedSlope * (n.nx - firstX))
                        val stemLen = if (groupStemDir > 0) (beamYAtN - n.ny) else (n.ny - beamYAtN)
                        if (stemLen < 18f) {
                            val diff = 18f - stemLen
                            constrainedStartY += (groupStemDir * diff)
                        } else if (stemLen > 32f) {
                            val diff = stemLen - 32f
                            constrainedStartY -= (groupStemDir * diff)
                        }
                    }

                    val constrainedEndY = constrainedStartY + (clampedSlope * dx)

                    val noteStemYMap = mutableMapOf<ScoreNote, Float>()
                    beamedSorted.forEach { n ->
                        val beamYAtNote = constrainedStartY + (clampedSlope * (n.nx - firstX))
                        noteStemYMap[n.note] = beamYAtNote
                    }

                    val sixteenthNotes = beamedSorted.filter { it.note.duration <= 0.35f }
                    val hasSixteenth = sixteenthNotes.size >= 2
                    val sFirstX = if (hasSixteenth) (if (groupStemDir > 0) sixteenthNotes.first().nx - 5f else sixteenthNotes.first().nx + 5f) else 0f
                    val sLastX = if (hasSixteenth) (if (groupStemDir > 0) sixteenthNotes.last().nx - 5f else sixteenthNotes.last().nx + 5f) else 0f
                    val sStartY = if (hasSixteenth) (constrainedStartY + clampedSlope * (sFirstX - firstX)) - (groupStemDir * 4.5f) else 0f
                    val sEndY = if (hasSixteenth) (constrainedStartY + clampedSlope * (sLastX - firstX)) - (groupStemDir * 4.5f) else 0f

                    val beamInfo = PrecomputedBeamGroup(
                        stemDir = groupStemDir,
                        firstX = firstX,
                        lastX = lastX,
                        startY = constrainedStartY,
                        endY = constrainedEndY,
                        noteStemYMap = noteStemYMap,
                        isSixteenth = hasSixteenth,
                        sFirstX = sFirstX,
                        sLastX = sLastX,
                        sStartY = sStartY,
                        sEndY = sEndY
                    )
                    beamEligible.forEach { info ->
                        noteBeamMap[info.note] = beamInfo
                    }
                    precomputedBeams.add(beamInfo)
                }
            }
        }
    }

    val explicitTrebleRests = measure.rests.filter { it.voice == 0 }
    val explicitBassRests = measure.rests.filter { it.voice == 1 }
    val trebleRests = if (!isRightHandHidden) calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure, explicitTrebleRests) else emptyList()
    val bassRests = if (!isLeftHandHidden) calculateRestsForStaff(measure.notes, isTreble = false, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure, explicitBassRests) else emptyList()

    return PrecomputedStaffLayout(
        visibleNotes = visibleNotes,
        computedXByPosition = computedXByPosition,
        noteBeamMap = noteBeamMap,
        beamGroups = precomputedBeams,
        trebleRests = trebleRests,
        bassRests = bassRests,
        usableMeasureWidth = usableMeasureWidth,
        notesStartX = notesStartX,
        beatsPerMeasure = beatsPerMeasure
    )
}

private fun computeTabLayout(
    measure: Measure,
    drawHeaders: Boolean,
    startX: Float,
    endX: Float,
    tBase: Float,
    lineSpacing: Float
): PrecomputedTabLayout {
    val tabHeaderWidth = if (drawHeaders) 80f else 0f
    val systemContentStartX = startX + tabHeaderWidth
    val usableMeasureWidth = (endX - systemContentStartX - 10f).coerceAtLeast(100f)
    val beatsPerMeasure = measure.timeSignatureNumerator.toFloat().coerceAtLeast(1f)
    val guitarOpenStringsMidi = intArrayOf(64, 59, 55, 50, 45, 40) // 1st high E (64) to 6th low E (40)

    val validNotes = measure.notes.filter { !it.isRest }
    val distinctPositions = validNotes.map { it.position }.distinct().sorted()
    val minTabGap = 24f
    val computedXByPosition = mutableMapOf<Float, Float>()

    if (distinctPositions.isNotEmpty()) {
        val minDur = validNotes.minOfOrNull { it.duration.coerceAtLeast(0.125f) } ?: 1.0f
        val posDurations = distinctPositions.mapIndexed { idx, pos ->
            val nextPos = if (idx < distinctPositions.lastIndex) distinctPositions[idx + 1] else beatsPerMeasure
            val delta = (nextPos - pos).coerceAtLeast(minDur)
            (delta / minDur).toDouble().pow(0.58).toFloat()
        }
        val sumWeighted = posDurations.sum().coerceAtLeast(1f)
        val totalNaturalSpan = (usableMeasureWidth - 28f).coerceAtLeast(50f)

        var runningX = systemContentStartX + 14f
        var runningWeight = 0f
        distinctPositions.forEachIndexed { idx, pos ->
            val fraction = (runningWeight / sumWeighted).coerceIn(0f, 0.96f)
            val idealX = systemContentStartX + 14f + (totalNaturalSpan * fraction)
            val targetX = if (idx == 0) idealX else maxOf(idealX, runningX + minTabGap)
            computedXByPosition[pos] = snap(targetX)
            runningX = targetX
            runningWeight += posDurations[idx]
        }
    }

    val chordGroups = validNotes.groupBy { it.position }
    val tabNotes = mutableListOf<PrecomputedTabNote>()

    chordGroups.forEach { (pos, notesAtPos) ->
        val nx = computedXByPosition[pos] ?: snap(systemContentStartX + (usableMeasureWidth * pos / beatsPerMeasure))
        val sortedDescMidi = notesAtPos.sortedByDescending { it.midiNote }
        val usedStrings = mutableSetOf<Int>()

        sortedDescMidi.forEach { note ->
            val stringIdx = if (note.string != null && note.string in 0..5 && note.string !in usedStrings) {
                note.string!!
            } else {
                // Find eligible guitar strings where note can be played within frets 0..22 without colliding with already assigned strings
                var bestStr = (0..5).filter { it !in usedStrings }.minByOrNull { sIdx ->
                    val openPitch = guitarOpenStringsMidi[sIdx]
                    val fret = note.midiNote - openPitch
                    if (fret in 0..22) {
                        // Prefer standard comfortable frets (frets 0..9)
                        if (fret <= 9) fret else fret + 10
                    } else {
                        // Out of natural range for this string, penalize heavily
                        1000 + kotlin.math.abs(fret)
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
            val fretText = fretValue.toString()

            val circleRadius = (lineSpacing * 0.42f).coerceAtLeast(6f)
            val pillWidth = maxOf(circleRadius * 2f, (fretText.length * 7f) + 8f)
            val pillHeight = circleRadius * 2f

            tabNotes.add(
                PrecomputedTabNote(
                    note = note,
                    midiNote = note.midiNote,
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

    val rests = calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { true }, beatsPerMeasure)

    return PrecomputedTabLayout(
        notes = tabNotes,
        rests = rests,
        systemContentStartX = systemContentStartX,
        usableMeasureWidth = usableMeasureWidth,
        beatsPerMeasure = beatsPerMeasure
    )
}

private fun DrawScope.drawBend(
    start: Offset,
    amount: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticBezierTo(start.x + 8f, start.y - 12f, start.x + 12f, start.y - (amount * 24f))
    }
    drawPath(path, color = color, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawBarre(
    barre: BarreInfo,
    x: Float,
    topY: Float,
    bottomY: Float,
    color: Color
) {
    drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = 3f)
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
        !note.isRest && (staffAssignmentMap[note] ?: (note.midiNote >= 60)) == isTreble
    }.sortedBy { it.position }

    val standardDurations = floatArrayOf(8.0f, 4.0f, 2.0f, 1.0f, 0.5f, 0.25f, 0.125f)

    fun decomposeInterval(startPos: Float, length: Float, dest: MutableList<RestSymbol>) {
        var pos = startPos
        var remaining = length
        while (remaining > 0.04f) {
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
        if (beatsPerMeasure in 3.5f..4.5f) {
            wholeMeasureRests.add(RestSymbol(position = 0.0f, duration = beatsPerMeasure))
        } else {
            decomposeInterval(0.0f, beatsPerMeasure, wholeMeasureRests)
        }
        return wholeMeasureRests
    }

    val rests = mutableListOf<RestSymbol>()
    var currentPos = 0.0f

    for (note in staffNotes) {
        if (note.position > currentPos + 0.05f) {
            decomposeInterval(currentPos, note.position - currentPos, rests)
        }
        currentPos = maxOf(currentPos, note.position + note.duration)
    }

    if (currentPos < beatsPerMeasure - 0.05f) {
        decomposeInterval(currentPos, beatsPerMeasure - currentPos, rests)
    }
    return rests
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
    isPedalPressed: Boolean = false,
    showScoreHeader: Boolean = true,
    compactStaffMode: Boolean = false,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
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
    val sharpCache = remember(textMeasurer) {
        textMeasurer.measure("♯", TextStyle(color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold))
    }
    val flatCache = remember(textMeasurer) {
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
                    // Score Title and Tempo/Key Metadata Header
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
                                Text("𝅘𝅥 =", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
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
                            Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Text(
                                text = "${score.measures.size} Bars",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Interactive Notation Format Mode Switcher (Staff / Tab / Both)
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
                val rowHeight = when {
                    compactStaffMode -> if (showStaff && showTab) 330.dp else if (showStaff) 180.dp else 145.dp
                    showStaff && showTab -> 490.dp
                    showStaff -> 270.dp
                    else -> 210.dp
                }

                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

                // Auto-scroll the notation viewer to follow active playhead measure
                var lastScrolledMeasure by remember { mutableIntStateOf(-1) }
                androidx.compose.runtime.LaunchedEffect(playheadMeasure, isVerticalScroll) {
                    if (playheadMeasure >= 0 && score.measures.isNotEmpty() && playheadMeasure != lastScrolledMeasure) {
                        lastScrolledMeasure = playheadMeasure
                        if (isVerticalScroll) {
                            val targetRow = playheadMeasure / 2
                            val rowHeightPx = 280f
                            val targetY = (targetRow * rowHeightPx).toInt()
                            verticalScrollState.animateScrollTo(targetY.coerceAtLeast(0))
                        } else {
                            val measureWidthPx = 300f
                            val targetX = (playheadMeasure * measureWidthPx).toInt()
                            horizontalScrollState.animateScrollTo(targetX.coerceAtLeast(0))
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(verticalScrollState)
                            .padding(vertical = 12.dp)
                    ) {
                        rowMeasureChunks.forEachIndexed { rowIndex, rowMeasures ->
                            Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val staffLineSpacing = if (compactStaffMode) 9.5f.dp.toPx() else 14.dp.toPx()
                                    val tabLineSpacing = if (compactStaffMode) 10.dp.toPx() else 15.dp.toPx()
                                    val trebleBaseline = if (compactStaffMode) 16.dp.toPx() else 28.dp.toPx()
                                    val bassBaseline = trebleBaseline + (5.5f * staffLineSpacing)
                                    val tabBaseline = if (showStaff) {
                                        bassBaseline + (4.0f * staffLineSpacing) + (if (compactStaffMode) 56.dp.toPx() else 84.dp.toPx())
                                    } else {
                                        if (compactStaffMode) 18.dp.toPx() else 30.dp.toPx()
                                    }
                                    val onSurface = colorScheme.onSurface
                                    val leftPadding = 12f
                                    val rightPadding = 12f
                                    val availableWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(100f)
                                    val measureWidth = availableWidth / rowMeasures.size.coerceAtLeast(1)

                                    rowMeasures.forEachIndexed { mIdxInRow, measure ->
                                         val mIdx = (rowIndex * 2) + mIdxInRow
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
                                             isLeftHandHidden = isLeftHandHidden,
                                             isRightHandHidden = isRightHandHidden,
                                             isDark = isDark,
                                             isTab = false,
                                             tabBaseline = tabBaseline,
                                             isPedalPressed = isPedalPressed,
                                             showLabels = showLetters,
                                             showLyrics = showLyrics
                                         )

                                         val tabConfig = DrawingConfig(
                                             lineSpacing = tabLineSpacing,
                                             trebleBaseline = tabBaseline,
                                             bassBaseline = tabBaseline,
                                             primaryColor = primaryColor,
                                             leftHandColor = leftHandColor,
                                             rightHandColor = rightHandColor,
                                             isLeftHandHidden = isLeftHandHidden,
                                             isRightHandHidden = isRightHandHidden,
                                             isDark = isDark,
                                             isTab = true,
                                             tabBaseline = tabBaseline,
                                             showLabels = showLetters,
                                             showLyrics = showLyrics
                                         )

                                         val labelLayout = measureLabelsCache[mIdx] ?: textMeasurer.measure(
                                             text = "M.${mIdx + 1}",
                                             style = TextStyle(color = primaryColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                         )

                                         drawText(
                                             textLayoutResult = labelLayout,
                                             topLeft = Offset(startX + 5f, if (showStaff) trebleBaseline - (staffLineSpacing * 2f) else tabBaseline - (tabLineSpacing * 2f))
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
                                                 drawLeftBar = (mIdxInRow == 0),
                                                 config = staffConfig
                                             )

                                             val barType = when {
                                                 measure.repeatEnd -> "repeat_end"
                                                 measure.repeatStart -> "repeat_start"
                                                 mIdx == score.measures.lastIndex -> "final"
                                                 measure.barline.isNotBlank() -> measure.barline
                                                 else -> "single"
                                             }
                                             drawGrandStaffSystemBarline(
                                                 type = barType,
                                                 x = endX,
                                                 trebleTop = trebleBaseline,
                                                 trebleBottom = trebleBaseline + (4 * staffLineSpacing),
                                                 bassTop = bassBaseline,
                                                 bassBottom = bassBaseline + (4 * staffLineSpacing),
                                                 color = onSurface.copy(alpha = 0.5f),
                                                 repeatStart = measure.repeatStart,
                                                 repeatEnd = measure.repeatEnd
                                             )

                                             val staffLayout = layoutCache.getStaffLayout(
                                                 measure = measure,
                                                 drawHeaders = (mIdxInRow == 0),
                                                 startX = startX,
                                                 endX = endX,
                                                 trebleBaseline = trebleBaseline,
                                                 bassBaseline = bassBaseline,
                                                 lineSpacing = staffLineSpacing,
                                                 isLeftHandHidden = isLeftHandHidden,
                                                 isRightHandHidden = isRightHandHidden
                                             )

                                             drawStaffMeasureContent(
                                                 drawScope = this, startX = startX, endX = endX, h = size.height,
                                                 measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                                 playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                 config = staffConfig, textMeasurer = textMeasurer,
                                                 noteLabels = MusicConstants.NOTE_LABELS,
                                                 diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                                 isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                                 timeSignature = score.timeSignature, drawHeaders = (mIdxInRow == 0),
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
                                                 drawLeftBar = (mIdxInRow == 0),
                                                 config = tabConfig
                                             )

                                             val tabLayout = layoutCache.getTabLayout(
                                                 measure = measure,
                                                 drawHeaders = (mIdxInRow == 0),
                                                 startX = startX,
                                                 endX = endX,
                                                 tBase = tabBaseline,
                                                 lineSpacing = tabLineSpacing
                                             )

                                             drawTabMeasureContent(
                                                 drawScope = this, startX = startX, endX = endX, h = size.height,
                                                 measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                                 playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                                 config = tabConfig, labelLayout = labelLayout, tBase = tabBaseline, textMeasurer = textMeasurer,
                                                 isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                                 timeSignature = score.timeSignature, drawHeaders = (mIdxInRow == 0),
                                                 tabLayout = tabLayout
                                             )
                                         }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                            .padding(vertical = 6.dp)
                    ) {
                        score.measures.forEachIndexed { mIdx, measure ->
                            val isFirstMeasure = (mIdx == 0)
                            val measureWidth = if (isFirstMeasure) 360.dp else 280.dp
                            Box(modifier = Modifier.width(measureWidth).height(rowHeight)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val staffLineSpacing = if (compactStaffMode) 9.5f.dp.toPx() else 14.dp.toPx()
                                    val tabLineSpacing = if (compactStaffMode) 10.dp.toPx() else 15.dp.toPx()
                                    val trebleBaseline = if (compactStaffMode) 16.dp.toPx() else 28.dp.toPx()
                                    val bassBaseline = trebleBaseline + (5.5f * staffLineSpacing)
                                    val tabBaseline = if (showStaff) {
                                        bassBaseline + (4.0f * staffLineSpacing) + (if (compactStaffMode) 56.dp.toPx() else 84.dp.toPx())
                                    } else {
                                        if (compactStaffMode) 18.dp.toPx() else 30.dp.toPx()
                                    }
                                    val onSurface = colorScheme.onSurface
                                    val startX = 0f
                                    val endX = size.width
                                    val isPlayheadInThisMeasure = (mIdx == playheadMeasure)

                                    val staffConfig = DrawingConfig(
                                        lineSpacing = staffLineSpacing,
                                        trebleBaseline = trebleBaseline,
                                        bassBaseline = bassBaseline,
                                        primaryColor = primaryColor,
                                        leftHandColor = leftHandColor,
                                        rightHandColor = rightHandColor,
                                        isLeftHandHidden = isLeftHandHidden,
                                        isRightHandHidden = isRightHandHidden,
                                        isDark = isDark,
                                        isTab = false,
                                        tabBaseline = tabBaseline,
                                        isPedalPressed = isPedalPressed,
                                        showLabels = showLetters,
                                        showLyrics = showLyrics
                                    )

                                    val tabConfig = DrawingConfig(
                                        lineSpacing = tabLineSpacing,
                                        trebleBaseline = tabBaseline,
                                        bassBaseline = tabBaseline,
                                        primaryColor = primaryColor,
                                        leftHandColor = leftHandColor,
                                        rightHandColor = rightHandColor,
                                        isLeftHandHidden = isLeftHandHidden,
                                        isRightHandHidden = isRightHandHidden,
                                        isDark = isDark,
                                        isTab = true,
                                        tabBaseline = tabBaseline,
                                        showLabels = showLetters,
                                        showLyrics = showLyrics
                                    )

                                    val labelLayout = measureLabelsCache[mIdx] ?: textMeasurer.measure(
                                        text = "M.${mIdx + 1}",
                                        style = TextStyle(color = primaryColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )

                                    drawText(
                                        textLayoutResult = labelLayout,
                                        topLeft = Offset(startX + (if (isFirstMeasure) 8f else 5f), if (showStaff) trebleBaseline - (staffLineSpacing * 2f) else tabBaseline - (tabLineSpacing * 2f))
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

                                        val barType = when {
                                            measure.repeatEnd -> "repeat_end"
                                            measure.repeatStart -> "repeat_start"
                                            mIdx == score.measures.lastIndex -> "final"
                                            measure.barline.isNotBlank() -> measure.barline
                                            else -> "single"
                                        }
                                        drawGrandStaffSystemBarline(
                                            type = barType,
                                            x = endX,
                                            trebleTop = trebleBaseline,
                                            trebleBottom = trebleBaseline + (4 * staffLineSpacing),
                                            bassTop = bassBaseline,
                                            bassBottom = bassBaseline + (4 * staffLineSpacing),
                                            color = onSurface.copy(alpha = 0.5f),
                                            repeatStart = measure.repeatStart,
                                            repeatEnd = measure.repeatEnd
                                        )

                                        val staffLayout = layoutCache.getStaffLayout(
                                            measure = measure,
                                            drawHeaders = isFirstMeasure,
                                            startX = startX,
                                            endX = endX,
                                            trebleBaseline = trebleBaseline,
                                            bassBaseline = bassBaseline,
                                            lineSpacing = staffLineSpacing,
                                            isLeftHandHidden = isLeftHandHidden,
                                            isRightHandHidden = isRightHandHidden
                                        )

                                        drawStaffMeasureContent(
                                            drawScope = this, startX = startX, endX = endX, h = size.height,
                                            measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                            playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                            config = staffConfig, textMeasurer = textMeasurer,
                                            noteLabels = MusicConstants.NOTE_LABELS,
                                            diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                            isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                            timeSignature = score.timeSignature, drawHeaders = isFirstMeasure,
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
                                            drawHeaders = isFirstMeasure,
                                            startX = startX,
                                            endX = endX,
                                            tBase = tabBaseline,
                                            lineSpacing = tabLineSpacing
                                        )

                                        drawTabMeasureContent(
                                            drawScope = this, startX = startX, endX = endX, h = size.height,
                                            measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                            playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                            config = tabConfig, labelLayout = labelLayout, tBase = tabBaseline, textMeasurer = textMeasurer,
                                            isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                            timeSignature = score.timeSignature, drawHeaders = isFirstMeasure,
                                            tabLayout = tabLayout
                                        )
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
        // Repeat End: two dots before thin line, then thick barline
        val midY = (topY + bottomY) / 2f
        val dotRadius = 2.2f
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, midY - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, midY + 6f))
        drawLine(color, Offset(x - gap, topY), Offset(x - gap, bottomY), strokeWidth = thinStroke)
        drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = finalStroke)
        return
    }

    if (repeatStart) {
        // Repeat Start: thick line, thin line, then two dots after
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
        // Repeat dots on treble staff (between lines 2 and 4)
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, trebleMid - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, trebleMid + 6f))
        // Repeat dots on bass staff
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, bassMid - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, bassMid + 6f))

        drawLine(color, Offset(x - gap, trebleTop), Offset(x - gap, trebleBottom), strokeWidth = thinStroke)
        drawLine(color, Offset(x - gap, bassTop), Offset(x - gap, bassBottom), strokeWidth = thinStroke)
        drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
        drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
        return
    }

    if (repeatStart) {
        val trebleMid = (trebleTop + trebleBottom) / 2f
        val bassMid = (bassTop + bassBottom) / 2f
        val dotRadius = 2.2f
        drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
        drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
        drawLine(color, Offset(x + gap, trebleTop), Offset(x + gap, trebleBottom), strokeWidth = thinStroke)
        drawLine(color, Offset(x + gap, bassTop), Offset(x + gap, bassBottom), strokeWidth = thinStroke)

        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, trebleMid - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, trebleMid + 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, bassMid - 6f))
        drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, bassMid + 6f))
        return
    }

    when (type) {
        "final" -> {
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
            drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
            drawLine(color, Offset(x - gap - 1f, trebleTop), Offset(x - gap - 1f, trebleBottom), strokeWidth = thinStroke)
            drawLine(color, Offset(x - gap - 1f, bassTop), Offset(x - gap - 1f, bassBottom), strokeWidth = thinStroke)
        }
        "double" -> {
            drawLine(color, Offset(x - gap, trebleTop), Offset(x - gap, trebleBottom), strokeWidth = thinStroke)
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = thinStroke)
            drawLine(color, Offset(x - gap, bassTop), Offset(x - gap, bassBottom), strokeWidth = thinStroke)
            drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = thinStroke)
        }
        else -> {
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = thinStroke)
            drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = thinStroke)
        }
    }
}

private fun drawTabMeasureContent(
    drawScope: DrawScope, startX: Float, endX: Float, h: Float, labelLayout: TextLayoutResult,
    tBase: Float,
    measure: Measure, activeMidiNotes: Set<Int>, playingNote: Int?,
    playheadOffset: Float?, config: DrawingConfig, textMeasurer: TextMeasurer,
    isFinalBar: Boolean, isMeasureActive: Boolean, timeSignature: String, drawHeaders: Boolean,
    tabLayout: PrecomputedTabLayout? = null
) {
    with(drawScope) {
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))

        if (drawHeaders) {
            val tabClefH = config.lineSpacing * 5.0f
            val tabClefW = tabClefH * 0.33f
            with(MusicVectorSymbols) {
                drawTabClef(
                    color = clefTextColor,
                    topLeft = Offset(startX + 8f, tBase),
                    size = Size(tabClefW, tabClefH)
                )
            }

            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                with(MusicVectorSymbols) {
                    drawTimeSignature(
                        numerator = tsParts[0],
                        denominator = tsParts[1],
                        color = clefTextColor,
                        topLeft = Offset(startX + 12f + tabClefW + 6f, tBase + (config.lineSpacing * 0.5f)),
                        lineSpacing = config.lineSpacing
                    )
                }
            }
        }

        val tabBarType = when {
            measure.repeatEnd -> "repeat_end"
            measure.repeatStart -> "repeat_start"
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
            repeatStart = measure.repeatStart,
            repeatEnd = measure.repeatEnd
        )

        val layout = tabLayout ?: computeTabLayout(
            measure = measure,
            drawHeaders = drawHeaders,
            startX = startX,
            endX = endX,
            tBase = tBase,
            lineSpacing = config.lineSpacing
        )

        val systemContentStartX = layout.systemContentStartX
        val usableMeasureWidth = layout.usableMeasureWidth
        val beatsPerMeasure = layout.beatsPerMeasure

        layout.notes.forEach { tabNote ->
            val note = tabNote.note
            val nx = tabNote.nx
            val ny = tabNote.ny
            val midiNote = tabNote.midiNote
            val isSounding = isMeasureActive && ((playingNote != null && midiNote == playingNote) || activeMidiNotes.contains(midiNote))

            val fretLayout = textMeasurer.measure(
                text = tabNote.fretText,
                style = TextStyle(color = if (isSounding) Color.White else clefTextColor, fontSize = 7.sp)
            )

            drawRoundRect(
                color = if (isSounding) config.primaryColor else (colorScheme?.surfaceVariant ?: (if (config.isDark) Color(0xFF2C2C2A) else Color(0xFFFCFBF7))),
                topLeft = Offset(nx - tabNote.pillWidth / 2f, ny - tabNote.pillHeight / 2f),
                size = Size(tabNote.pillWidth, tabNote.pillHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(tabNote.pillHeight / 2f, tabNote.pillHeight / 2f)
            )

            drawText(fretLayout, topLeft = Offset(nx - fretLayout.size.width / 2f, ny - fretLayout.size.height / 2f))

            if (config.showLyrics && note.lyric.isNotEmpty()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, tBase + 6 * config.lineSpacing + 10f))
            }

            when (note.articulation) {
                TabArticulation.HAMMER_ON -> {
                    val hLayout = textMeasurer.measure(MusicSymbols.HAMMER_ON, TextStyle(color = config.primaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    drawText(hLayout, topLeft = Offset(nx - hLayout.size.width / 2f, ny - config.lineSpacing * 0.9f))
                }
                TabArticulation.PULL_OFF -> {
                    val pLayout = textMeasurer.measure(MusicSymbols.PULL_OFF, TextStyle(color = config.primaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    drawText(pLayout, topLeft = Offset(nx - pLayout.size.width / 2f, ny - config.lineSpacing * 0.9f))
                }
                TabArticulation.SLIDE_UP -> {
                    drawLine(config.primaryColor, Offset(nx - 8f, ny + 4f), Offset(nx + 8f, ny - 4f), strokeWidth = 1.8f)
                }
                TabArticulation.SLIDE_DOWN -> {
                    drawLine(config.primaryColor, Offset(nx - 8f, ny - 4f), Offset(nx + 8f, ny + 4f), strokeWidth = 1.8f)
                }
                TabArticulation.RELEASE -> {
                    val rLayout = textMeasurer.measure(MusicSymbols.RELEASE, TextStyle(color = config.primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Normal))
                    drawText(rLayout, topLeft = Offset(nx - rLayout.size.width / 2f, ny - config.lineSpacing * 0.9f))
                }
                TabArticulation.TAP -> {
                    val tLayout = textMeasurer.measure("T", TextStyle(color = config.primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Black))
                    drawText(tLayout, topLeft = Offset(nx - tLayout.size.width / 2f, ny - config.lineSpacing * 0.9f))
                }
                TabArticulation.DEAD_NOTE -> {}
                TabArticulation.NONE -> {}
            }

            if (note.palmMute) {
                val pmLayout = textMeasurer.measure(MusicSymbols.PALM_MUTE, TextStyle(color = clefTextColor.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Medium))
                drawText(pmLayout, topLeft = Offset(nx - pmLayout.size.width / 2f, tBase - 12f))
            }
            if (note.vibrato) {
                with(MusicVectorSymbols) {
                    drawSmuflSymbol("wiggleArpeggiatoUp", config.primaryColor, Offset(nx - 6f, ny - 14f), Size(12f, 8f), 400f)
                }
            }

            note.bend?.let { bend ->
                drawBend(Offset(nx, ny), bend.bendSteps, config.primaryColor)
            }
            note.barre?.let { barre ->
                val bTop = tBase + (barre.startString * config.lineSpacing)
                val bBottom = tBase + (barre.endString * config.lineSpacing)
                drawBarre(barre, nx - 12f, bTop, bBottom, config.primaryColor.copy(alpha = 0.6f))
            }
        }

        // Render Rests for Tab staff
        layout.rests.forEach { rest ->
            val rx = snap(systemContentStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            val ry = tBase + 2.5f * config.lineSpacing
            val restScale = config.lineSpacing / 14f
            val restColor = clefTextColor.copy(alpha = 0.55f)

            with(MusicVectorSymbols) {
                when {
                    rest.duration >= 8.0f -> drawRestDoubleWhole(restColor, Size(12f * restScale, config.lineSpacing), topLeft = Offset(rx - 6f * restScale, ry))
                    rest.duration >= 3.5f -> drawRestWhole(restColor, Size(16f * restScale, 10f * restScale), topLeft = Offset(rx - 8f * restScale, ry - 5f * restScale))
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

        // Navigation marks (Segno, Coda)
        if (measure.segno || measure.dalSegno) {
            with(MusicVectorSymbols) {
                drawSegno(config.primaryColor, Size(20f, 20f), topLeft = Offset(startX + 5f, tBase - 25f))
            }
        }
        if (measure.coda || measure.toCoda) {
            with(MusicVectorSymbols) {
                drawCoda(config.primaryColor, Size(20f, 20f), topLeft = Offset(endX - 25f, tBase - 25f))
            }
        }

        if (playheadOffset != null) {
            drawPlayheadTabLine(
                drawScope = this,
                notes = layout.notes,
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
    systemContentStartX: Float,
    usableMeasureWidth: Float,
    beatsPerMeasure: Float,
    tBase: Float,
    playingNote: Int?,
    playheadOffset: Float,
    config: DrawingConfig
) {
    with(drawScope) {
        val matchingTabNote = if (playingNote != null) {
            notes.firstOrNull { it.midiNote == playingNote }
        } else null

        val targetPlayheadX = if (matchingTabNote != null) {
            matchingTabNote.nx
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
private fun drawStaffMeasureContent(
    drawScope: DrawScope, startX: Float, endX: Float, h: Float,
    measure: Measure, activeMidiNotes: Set<Int>, playingNote: Int?,
    playheadOffset: Float?, config: DrawingConfig, textMeasurer: TextMeasurer,
    noteLabels: List<String>, diatonicSteps: IntArray, isFinalBar: Boolean,
    isMeasureActive: Boolean, timeSignature: String, drawHeaders: Boolean,
    labelCache: Map<Int, TextLayoutResult>, sharpCache: TextLayoutResult, flatCache: TextLayoutResult,
    staffLayout: PrecomputedStaffLayout? = null
) {
    with(drawScope) {
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))
        val trebleBaseline = config.trebleBaseline
        val bassBaseline = config.bassBaseline
        val staffLineColor = colorScheme?.outlineVariant ?: (if (config.isDark) Color(0xFF4A4945) else Color(0xFFCCCCCC))

        if (drawHeaders) {
            val clefH = config.lineSpacing * 6.8f
            val clefW = clefH * 0.357f
            val trebleClefTop = (trebleBaseline + 3f * config.lineSpacing) - (0.6316f * clefH)
            
            with(MusicVectorSymbols) {
                drawTrebleClef(
                    color = clefTextColor,
                    topLeft = Offset(startX + 6f, trebleClefTop),
                    size = Size(clefW, clefH)
                )
            }

            val bassH = config.lineSpacing * 3.4f
            val bassW = bassH * 0.81f
            val bassClefTop = (bassBaseline + 1f * config.lineSpacing) - (0.3171f * bassH)
            with(MusicVectorSymbols) {
                drawBassClef(
                    color = clefTextColor,
                    topLeft = Offset(startX + 8f, bassClefTop),
                    size = Size(bassW, bassH)
                )
            }

            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                val tsX = startX + maxOf(clefW, bassW) + 10f
                with(MusicVectorSymbols) {
                    drawTimeSignature(
                        numerator = tsParts[0],
                        denominator = tsParts[1],
                        color = clefTextColor,
                        topLeft = Offset(tsX, trebleBaseline),
                        lineSpacing = config.lineSpacing
                    )
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

        val layout = staffLayout ?: computeStaffLayout(
            measure = measure,
            drawHeaders = drawHeaders,
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

        // Render each note
        visibleNotes.forEach { info ->
            val note = info.note
            val midiNote = info.midiNote
            val isTrebleClef = info.isTrebleClef
            val nx = info.nx
            val ny = info.ny
            val topY = info.topY
            val bottomY = info.bottomY

            val isSounding = isMeasureActive && (midiNote == playingNote || activeMidiNotes.contains(midiNote))
            val beadColor = if (isSounding) config.primaryColor else if (isTrebleClef) config.rightHandColor else config.leftHandColor

            // Draw Ledger Lines accurately
            drawLedgerLines(
                x = nx,
                y = ny,
                topY = topY,
                bottomY = bottomY,
                spacing = config.lineSpacing,
                color = staffLineColor
            )

            val beamInfo = noteBeamMap[note]
            val stemDir = beamInfo?.stemDir ?: if (isTrebleClef) (if (midiNote >= 71) 1f else -1f) else (if (midiNote >= 50) 1f else -1f)
            val stemX = if (stemDir > 0) nx - 5f else nx + 5f
            
            val duration = note.duration
            val isHollow = duration >= 2.0f

            // Draw Stem for non-whole/non-breve notes, connecting seamlessly to beam line if beamed
            if (duration < 4.0f) {
                val stemEndY = if (beamInfo != null) {
                    beamInfo.noteStemYMap[note] ?: (ny + (stemDir * 26f))
                } else {
                    ny + (stemDir * 26f)
                }
                drawLine(clefTextColor, Offset(stemX, ny), Offset(stemX, stemEndY), strokeWidth = 1.3f)
            }

            // 1. Draw Notehead using Authentic SMuFL / Bravura Vector Symbols
            val noteScale = config.lineSpacing / 12f
            val noteWidth = when {
                duration >= 8.0f -> 22f * noteScale
                duration >= 4.0f -> 20f * noteScale
                else -> 16f * noteScale
            }
            val noteHeight = 11.5f * noteScale

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

                // 2. Augmentation Dot
                if (note.dotted || note.isDotted) {
                    val dotX = nx + (noteWidth / 2f) + 4f
                    drawSmuflSymbol("augmentationDot", beadColor, Offset(dotX, ny - 3f), Size(5f, 5f), 200f)
                }

                // 3. Musical Articulations
                val articY = if (stemDir > 0) ny + (stemDir * 32f) else ny - 16f
                when (note.musicalArticulation) {
                    MusicalArticulation.STACCATO -> drawStaccato(clefTextColor, Offset(nx, articY), radius = 2.2f)
                    MusicalArticulation.ACCENT -> drawAccent(clefTextColor, Size(12f, 8f), Offset(nx - 6f, articY))
                    MusicalArticulation.TENUTO -> drawTenuto(clefTextColor, Size(12f, 3f), Offset(nx - 6f, articY))
                    MusicalArticulation.MARCATO -> drawSmuflSymbol("articMarcatoAbove", clefTextColor, Offset(nx - 6f, articY), Size(10f, 10f), 400f)
                    MusicalArticulation.NONE -> {}
                }

                // 4. Dynamics
                if (note.dynamic.isNotEmpty()) {
                    val dynY = bassBaseline + 5 * config.lineSpacing + 8f
                    val dynName = when (note.dynamic.lowercase()) {
                        "p" -> "dynamicPiano"
                        "m", "mp" -> "dynamicMezzo"
                        "f" -> "dynamicForte"
                        "sfz" -> "dynamicSforzando"
                        "rfz" -> "dynamicRinforzando"
                        else -> "dynamicPiano"
                    }
                    drawDynamic(dynName, clefTextColor.copy(alpha = 0.85f), Size(12f, 14f), Offset(nx - 6f, dynY))
                }
            }

            // Flags only for isolated non-beamed notes
            if (duration <= 1.0f && !isHollow && beamInfo == null) {
                with(MusicVectorSymbols) {
                    val flagH = 22f
                    val flagW = 10f
                    val stemEndY = ny + (stemDir * 26f)
                    val flagY = if (stemDir > 0) stemEndY - flagH else stemEndY
                    when {
                        duration <= 0.0625f -> drawSmuflSymbol(if (stemDir > 0) "flag64thDown" else "flag64thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.5f), 800f)
                        duration <= 0.125f -> drawSmuflSymbol(if (stemDir > 0) "flag32ndDown" else "flag32ndUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.3f), 800f)
                        duration <= 0.25f -> drawSmuflSymbol(if (stemDir > 0) "flag16thDown" else "flag16thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH * 1.15f), 800f)
                        else -> drawSmuflSymbol(if (stemDir > 0) "flag8thDown" else "flag8thUp", clefTextColor, Offset(stemX, flagY), Size(flagW, flagH), 800f)
                    }
                }
            }
            
            if (config.showLabels) {
                val labelResult = labelCache[midiNote % 12]
                if (labelResult != null) {
                    val labelX = nx - (labelResult.size.width / 2f)
                    val labelY = ny - (labelResult.size.height / 2f)
                    drawText(labelResult, topLeft = Offset(labelX, labelY))
                }
            }

            if (config.showLyrics && note.lyric.isNotEmpty()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, bassBaseline + 5 * config.lineSpacing + 22f))
            }

            // 5. Accidentals
            val label = noteLabels[midiNote % 12]
            val hasAccidental = label.contains("#") || label.contains("b") || note.accidental != Accidental.NATURAL
            if (hasAccidental) {
                val isSharp = label.contains("#") || note.accidental == Accidental.SHARP
                val isDoubleSharp = note.accidental == Accidental.DOUBLE_SHARP
                val isDoubleFlat = note.accidental == Accidental.DOUBLE_FLAT
                val isNatural = note.accidental == Accidental.NATURAL && (label.contains("#") || label.contains("b"))
                val accH = config.lineSpacing * 1.35f
                val accW = when {
                    isDoubleSharp -> accH * 0.44f
                    isDoubleFlat -> accH * 0.44f
                    isSharp -> accH * 0.38f
                    isNatural -> accH * 0.28f
                    else -> accH * 0.32f
                }
                
                val accRightEdge = nx - (noteWidth / 2f) - 3.5f
                val accLeftX = accRightEdge - accW
                val accidentalColor = clefTextColor

                with(MusicVectorSymbols) {
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol("accidentalParensLeft", accidentalColor.copy(alpha = 0.6f), Offset(accLeftX - 7f, ny - accH / 2f), Size(5f, accH), 800f)
                    }
                    when {
                        isDoubleSharp -> drawDoubleSharp(accidentalColor, Size(accW, accH), Offset(accLeftX, ny - accH / 2f))
                        isDoubleFlat -> drawDoubleFlat(accidentalColor, Size(accW, accH), Offset(accLeftX, ny - (accH * 0.72f)))
                        isSharp -> drawSharp(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - accH / 2f))
                        isNatural -> drawNatural(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - accH / 2f))
                        else -> drawFlat(accidentalColor, size = Size(accW, accH), topLeft = Offset(accLeftX, ny - (accH * 0.72f)))
                    }
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol("accidentalParensRight", accidentalColor.copy(alpha = 0.6f), Offset(accRightEdge + 1f, ny - accH / 2f), Size(5f, accH), 800f)
                    }
                }
            }
        }

        // Draw Connected Sloped Beams using precomputed positions
        val beamThickness = 3.8f
        beamGroups.forEach { beam ->
            drawLine(
                color = clefTextColor,
                start = Offset(beam.firstX, beam.startY),
                end = Offset(beam.lastX, beam.endY),
                strokeWidth = beamThickness
            )

            if (beam.isSixteenth) {
                drawLine(
                    color = clefTextColor,
                    start = Offset(beam.sFirstX, beam.sStartY),
                    end = Offset(beam.sLastX, beam.sEndY),
                    strokeWidth = beamThickness
                )
            }
        }

        // Render Rests using precomputed collections
        (trebleRests + bassRests).forEach { rest ->
            val isTreble = trebleRests.contains(rest)
            val rx = snap(notesStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
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
                        drawRestWhole(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + spacing))
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
        // Navigation & Measure Marks
        if (measure.segno || measure.dalSegno) {
            with(MusicVectorSymbols) {
                drawSegno(config.primaryColor, Size(20f, 20f), topLeft = Offset(startX + 5f, config.trebleBaseline - 40f))
            }
        }
        if (measure.coda || measure.toCoda) {
            with(MusicVectorSymbols) {
                drawCoda(config.primaryColor, Size(20f, 20f), topLeft = Offset(endX - 25f, config.trebleBaseline - 40f))
            }
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
            drawText(endLayout, topLeft = Offset(startX + 6f, bY - 14f))
        }
        if (measure.fine) {
            val fineLayout = textMeasurer.measure("Fine", TextStyle(color = clefTextColor, fontSize = 9.sp, fontWeight = FontWeight.Black))
            drawText(fineLayout, topLeft = Offset(endX - fineLayout.size.width - 6f, config.trebleBaseline - 24f))
        }
        if (config.isPedalPressed) {
            with(MusicVectorSymbols) {
                drawPedal(config.primaryColor.copy(alpha = 0.8f), Size(18f, 14f), Offset(startX + 6f, bassBaseline + 5 * config.lineSpacing + 10f))
            }
        }

        // Draw Playhead line if present
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
        val matchingActiveNote = if (playingNote != null) {
            visibleNotes.firstOrNull { it.midiNote == playingNote }
        } else null

        val targetPlayheadX = if (matchingActiveNote != null) {
            matchingActiveNote.nx
        } else if (computedXByPosition.isNotEmpty()) {
            val targetPos = playheadOffset * beatsPerMeasure
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

            if (exactOrLowerX != -1f && higherX != -1f) {
                val fraction = if (higherPos > exactOrLowerPos) (targetPos - exactOrLowerPos) / (higherPos - exactOrLowerPos) else 0f
                exactOrLowerX + (higherX - exactOrLowerX) * fraction
            } else if (exactOrLowerX != -1f) {
                exactOrLowerX
            } else if (higherX != -1f) {
                higherX
            } else {
                notesStartX + (usableMeasureWidth * playheadOffset)
            }
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
            // Draw 6 Guitar Tab Lines
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
            // Treble 5 lines
            if (!config.isRightHandHidden) {
                for (i in 0 until 5) {
                    val y = trebleY + (i * spacing)
                    drawLine(lineColor, Offset(startX, y), Offset(endX, y), strokeWidth = strokeW)
                }
            }
            // Bass 5 lines
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
