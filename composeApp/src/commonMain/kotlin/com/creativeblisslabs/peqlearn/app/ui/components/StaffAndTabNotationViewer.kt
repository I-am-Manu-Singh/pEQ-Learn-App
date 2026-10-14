package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import com.creativeblisslabs.peqlearn.app.util.GuitarShape
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine

/**
 * StaffAndTabNotationViewer
 * Full-scale industrial notation rendering engine supporting:
 * - Dual Grand Staff (Treble + Bass systems)
 * - 6-String Guitar Tablature
 * - Dynamic ledger lines (notes exceeding stave bounds)
 * - Melodic Ties & Slur arcs (bezier curves)
 * - String Bends & Barres
 * - Navigation Marks (Segno, Coda, Fine, Da Capo, Dal Segno)
 * - Measure barlines and real-time playback note highlighting
 */
//
//private fun DrawScope.drawContinuousStaff(
//    startX: Float,
//    endX: Float,
//    trebleBaseline: Float,
//    bassBaseline: Float,
//    spacing: Float,
//    strokeWidth: Float
//) {
//    // Treble 5 lines
//    for (i in 0 until 5) {
//        val y = trebleBaseline + i * spacing
//        drawLine(
//            color = Color.White.copy(alpha = 0.25f),
//            start = Offset(startX, y),
//            end = Offset(endX, y),
//            strokeWidth = strokeWidth
//        )
//    }
//
//    // Bass 5 lines
//    for (i in 0 until 5) {
//        val y = bassBaseline + i * spacing
//        drawLine(
//            color = Color.White.copy(alpha = 0.25f),
//            start = Offset(startX, y),
//            end = Offset(endX, y),
//            strokeWidth = strokeWidth
//        )
//    }
//
//    // Left Grand Staff Connecting Bar
//    drawLine(
//        color = Color.White.copy(alpha = 0.6f),
//        start = Offset(startX, trebleBaseline),
//        end = Offset(startX, bassBaseline + 4 * spacing),
//        strokeWidth = strokeWidth * 2.5f
//    )
//}
//
//private fun DrawScope.drawContinuousTab(
//    startX: Float,
//    endX: Float,
//    baseline: Float,
//    spacing: Float
//) {
//    for (i in 0 until 6) {
//        val y = baseline + i * spacing
//        drawLine(
//            color = Color.White.copy(alpha = 0.35f),
//            start = Offset(startX, y),
//            end = Offset(endX, y),
//            strokeWidth = 1.dp.toPx()
//        )
//    }
//
//    // Left Tab Bracket Line
//    drawLine(
//        color = Color.White.copy(alpha = 0.6f),
//        start = Offset(startX, baseline),
//        end = Offset(startX, baseline + 5 * spacing),
//        strokeWidth = 2.dp.toPx()
//    )
//}

//
//private fun DrawScope.drawSlurArc(
//    startX: Float,
//    startY: Float,
//    endX: Float,
//    endY: Float,
//    color: Color
//) {
//    val path = Path().apply {
//        moveTo(startX, startY)
//        quadraticBezierTo(
//            (startX + endX) / 2f,
//            minOf(startY, endY) - 16f,
//            endX,
//            endY
//        )
//    }
//    drawPath(
//        path = path,
//        color = color,
//        style = Stroke(width = 1.5.dp.toPx())
//    )
//}

//
//private fun accidentalOffset(column: Int, spacing: Float): Float = -((column + 1) * spacing * 0.95f)
//
//private fun beamGroup(position: Float, timeSignature: String): Int {
//    val beats = when (timeSignature) {
//        "6/8" -> 2
//        "3/4" -> 3
//        "2/4" -> 2
//        else -> 4
//    }
//    return (position * beats).toInt().coerceAtLeast(0)
//}
//
//private fun dynamicSymbol(dynamic: String): String {
//    return when (dynamic.lowercase()) {
//        "ppp" -> "𝄠"
//        "pp" -> "𝄡"
//        "p" -> "𝄢"
//        "mp" -> "𝄣"
//        "mf" -> "𝄤"
//        "f" -> "𝄥"
//        "ff" -> "𝄦"
//        "fff" -> "𝄧"
//        else -> dynamic
//    }
//}
//
//private fun DrawScope.drawGrandStaffBarline(
//    barlineType: String,
//    x: Float,
//    trebleTop: Float,
//    bassBottom: Float,
//    color: Color
//) {
//    val strokeThin = 1.2f
//    val strokeThick = 3.5f
//    when (barlineType) {
//        "double" -> {
//            drawLine(color, Offset(x - 4f, trebleTop), Offset(x - 4f, bassBottom), strokeWidth = strokeThin)
//            drawLine(color, Offset(x, trebleTop), Offset(x, bassBottom), strokeWidth = strokeThin)
//        }
//        "final" -> {
//            drawLine(color, Offset(x - 4f, trebleTop), Offset(x - 4f, bassBottom), strokeWidth = strokeThin)
//            drawLine(color, Offset(x, trebleTop), Offset(x, bassBottom), strokeWidth = strokeThick)
//        }
//        else -> {
//            drawLine(color, Offset(x, trebleTop), Offset(x, bassBottom), strokeWidth = strokeThin)
//        }
//    }
//}
//
//private fun DrawScope.drawGrandStaffBrace(
//    x: Float,
//    trebleTop: Float,
//    bassBottom: Float,
//    color: Color
//) {
//    val path = Path().apply {
//        val midY = (trebleTop + bassBottom) / 2f
//        moveTo(x, trebleTop)
//        cubicTo(x - 8f, trebleTop + 15f, x - 12f, midY - 20f, x - 18f, midY)
//        cubicTo(x - 12f, midY + 20f, x - 8f, bassBottom - 15f, x, bassBottom)
//        cubicTo(x - 5f, bassBottom - 15f, x - 9f, midY + 20f, x - 15f, midY)
//        cubicTo(x - 9f, midY - 20f, x - 5f, trebleTop + 15f, x, trebleTop)
//        close()
//    }
//    drawPath(path, color = color)
//}
//
//private fun DrawScope.drawRepeatStartBarline(
//    x: Float,
//    top: Float,
//    bottom: Float,
//    color: Color
//) {
//    val thin = 1.5f
//    val thick = 3.5f
//    val gap = 4.0f
//    val dotRadius = 2.5f
//    val centerY = (top + bottom) / 2f
//    drawLine(color, Offset(x, top), Offset(x, bottom), strokeWidth = thick)
//    drawLine(color, Offset(x + gap, top), Offset(x + gap, bottom), strokeWidth = thin)
//    drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, centerY - 8f))
//    drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, centerY + 8f))
//}
//
//private fun DrawScope.drawRepeatEndBarline(
//    x: Float,
//    top: Float,
//    bottom: Float,
//    color: Color
//) {
//    val thin = 1.5f
//    val thick = 3.5f
//    val gap = 4.0f
//    val dotRadius = 2.5f
//    val centerY = (top + bottom) / 2f
//    drawLine(color, Offset(x - gap, top), Offset(x - gap, bottom), strokeWidth = thin)
//    drawLine(color, Offset(x, top), Offset(x, bottom), strokeWidth = thick)
//    drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, centerY - 8f))
//    drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, centerY + 8f))
//}
//
//private fun DrawScope.drawEndingBracket(
//    ending: Int?,
//    startX: Float,
//    endX: Float,
//    baseline: Float,
//    color: Color
//) {
//    if (ending == null) return
//    val y = baseline - 56.0f
//    drawLine(color, Offset(startX + 4f, y), Offset(endX - 4f, y), strokeWidth = 1.2f)
//    drawLine(color, Offset(startX + 4f, y), Offset(startX + 4f, y + 25f), strokeWidth = 1.2f)
//}
//
//private fun DrawScope.drawNavigationMarks(
//    measure: Measure,
//    startX: Float,
//    baseline: Float,
//    color: Color
//) {
//    val y = baseline - 45f
//    if (measure.segno) {
//        drawLine(color, Offset(startX + 28f, y - 6f), Offset(startX + 32f, y + 6f), strokeWidth = 1.5f)
//        drawCircle(color, radius = 2f, center = Offset(startX + 26f, y))
//        drawCircle(color, radius = 2f, center = Offset(startX + 34f, y))
//    }
//    if (measure.coda) {
//        drawCircle(color, radius = 6f, center = Offset(startX + 52f, y), style = Stroke(width = 1.5f))
//        drawLine(color, Offset(startX + 44f, y), Offset(startX + 60f, y), strokeWidth = 1.2f)
//        drawLine(color, Offset(startX + 52f, y - 8f), Offset(startX + 52f, y + 8f), strokeWidth = 1.2f)
//    }
//}

//
//private fun staffY(
//    midi: Int,
//    baseline: Float,
//    referenceStep: Int,
//    referenceLine: Int,
//    spacing: Float
//): Float {
//    val diatonicStep = com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine.getDiatonicStep(midi)
//    val stepDiff = diatonicStep - referenceStep
//    return baseline - (referenceLine * spacing) - (stepDiff * (spacing / 2.0f))
//}
//
//private fun slurArch(width: Float): Float = (width * 0.18f).coerceIn(8.0f, 32.0f)
//
//private fun tieArch(width: Float): Float = (width * 0.12f).coerceIn(6.0f, 22.0f)

//
//private fun DrawScope.drawHarmonic(
//    center: Offset,
//    harmonic: HarmonicType?,
//    color: Color
//) {
//    val size = 8f
//    val path = Path().apply {
//        moveTo(center.x, center.y - size)
//        lineTo(center.x + size, center.y)
//        lineTo(center.x, center.y + size)
//        lineTo(center.x - size, center.y)
//        close()
//    }
//    drawPath(path, color = color, style = Stroke(width = 1.5f))
//}
//
//private fun DrawScope.drawBeamPath(
//    firstStemX: Float,
//    lastStemX: Float,
//    firstBeamY: Float,
//    lastBeamY: Float,
//    thickness: Float,
//    color: Color
//) {
//    val path = Path().apply {
//        moveTo(firstStemX, firstBeamY)
//        lineTo(lastStemX, lastBeamY)
//        lineTo(lastStemX, lastBeamY + thickness)
//        lineTo(firstStemX, firstBeamY + thickness)
//        close()
//    }
//    drawPath(path, color = color)
//}
//
//private fun DrawScope.drawTupletBracket(
//    tuplet: Int,
//    firstStemX: Float,
//    lastStemX: Float,
//    firstBeamY: Float,
//    lastBeamY: Float,
//    spacing: Float,
//    color: Color
//) {
//    if (tuplet <= 1) return
//    val midX = (firstStemX + lastStemX) / 2.0f
//    val y = minOf(firstBeamY, lastBeamY) - (1.35f * spacing)
//    drawLine(color, Offset(firstStemX, y), Offset(midX - 9f, y), strokeWidth = 1.2f)
//    drawLine(color, Offset(midX + 9f, y), Offset(lastStemX, y), strokeWidth = 1.2f)
//    drawLine(color, Offset(firstStemX, y), Offset(firstStemX, y + 4f), strokeWidth = 1.2f)
//    drawLine(color, Offset(lastStemX, y), Offset(lastStemX, y + 4f), strokeWidth = 1.2f)
//}
//
//private fun DrawScope.drawSlurs(
//    notes: List<ScoreNote>,
//    positionMap: Map<ScoreNote, Float>,
//    staffAssignmentMap: Map<ScoreNote, Boolean>,
//    notesStartX: Float,
//    usableWidth: Float,
//    config: DrawingConfig,
//    color: Color
//) {
//    val slurStarts = notes.filter { it.slurStart }
//    for (start in slurStarts) {
//        val startIndex = notes.indexOf(start)
//        val end = notes.drop(startIndex + 1).firstOrNull { it.slurEnd } ?: continue
//
//        val x1 = snap(notesStartX + (positionMap[start] ?: start.position) * usableWidth)
//        val x2 = snap(notesStartX + (positionMap[end] ?: end.position) * usableWidth)
//        val isTreble = staffAssignmentMap[start] ?: (start.midiNote >= 60)
//        val baseline = if (isTreble) config.trebleBaseline else config.bassBaseline
//        val refStep = if (isTreble) 64 else 43
//
//        val y = staffY(start.midiNote, baseline, refStep, 4, config.lineSpacing)
//        val width = x2 - x1
//        val arch = slurArch(width)
//        val above = start.stemUp != true
//
//        val path = Path().apply {
//            moveTo(x1, y)
//            cubicTo(x1 + (width * 0.25f), if (above) y - arch else y + arch, x1 + (width * 0.75f), if (above) y - arch else y + arch, x2, y)
//            cubicTo(x1 + (0.75f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1 + (0.25f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1, y)
//            close()
//        }
//        drawPath(path, color = color)
//    }
//}
//
//private fun DrawScope.drawTies(
//    notes: List<ScoreNote>,
//    positionMap: Map<ScoreNote, Float>,
//    staffAssignmentMap: Map<ScoreNote, Boolean>,
//    notesStartX: Float,
//    usableWidth: Float,
//    config: DrawingConfig,
//    color: Color
//) {
//    val tieStarts = notes.filter { it.tieStart }
//    for (start in tieStarts) {
//        val startIndex = notes.indexOf(start)
//        val end = notes.drop(startIndex + 1).firstOrNull { it.tieEnd && it.midiNote == start.midiNote } ?: continue
//
//        val x1 = snap(notesStartX + (positionMap[start] ?: start.position) * usableWidth)
//        val x2 = snap(notesStartX + (positionMap[end] ?: end.position) * usableWidth)
//        val isTreble = staffAssignmentMap[start] ?: (start.midiNote >= 60)
//        val baseline = if (isTreble) config.trebleBaseline else config.bassBaseline
//        val refStep = if (isTreble) 64 else 43
//
//        val y = staffY(start.midiNote, baseline, refStep, 4, config.lineSpacing)
//        val width = x2 - x1
//        val arch = tieArch(width)
//        val above = start.stemUp != true
//
//        val path = Path().apply {
//            moveTo(x1, y)
//            cubicTo(x1 + (width * 0.25f), if (above) y - arch else y + arch, x1 + (width * 0.75f), if (above) y - arch else y + arch, x2, y)
//            cubicTo(x1 + (0.75f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1 + (0.25f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1, y)
//            close()
//        }
//        drawPath(path, color = color)
//    }
//}
//
//private fun buildRhythmicSpacingMap(notes: List<ScoreNote>): Map<ScoreNote, Float> {
//    if (notes.isEmpty()) return emptyMap()
//    val distinctPositions = notes.map { it.position }.distinct().sorted()
//    if (distinctPositions.size <= 1) return notes.associateWith { it.position }
//
//    val minPos = distinctPositions.first()
//    val maxPos = distinctPositions.last()
//    val span = if (maxPos > minPos) maxPos - minPos else 1.0f
//
//    return notes.associateWith { note ->
//        (note.position - minPos) / span
//    }
//}
//
//private fun resolveChordCollisions(notes: List<ScoreNote>): Map<ScoreNote, Float> {
//    val map = mutableMapOf<ScoreNote, Float>()
//    val grouped = notes.groupBy { it.position }
//
//    for ((_, chord) in grouped) {
//        val sortedChord = chord.sortedBy { it.midiNote }
//        for (i in sortedChord.indices) {
//            val note = sortedChord[i]
//            if (i > 0 && kotlin.math.abs(sortedChord[i - 1].midiNote - note.midiNote) <= 2) {
//                map[note] = 12.0f
//            } else {
//                map[note] = 0.0f
//            }
//        }
//    }
//    return map
//}
//
//private fun buildBeamLookup(
//    beamGroups: List<List<ScoreNote>>,
//    positionMap: Map<ScoreNote, Float>,
//    collisionOffsets: Map<ScoreNote, Float>,
//    staffAssignmentMap: Map<ScoreNote, Boolean>,
//    stemLengthMap: Map<ScoreNote, Float>,
//    config: DrawingConfig
//): Map<ScoreNote, BeamInfo> {
//    val result = mutableMapOf<ScoreNote, BeamInfo>()
//    for (group in beamGroups) {
//        if (group.size < 2) continue
//        val first = group.first()
//        val last = group.last()
//        val info = BeamInfo(
//            primaryBeam = true,
//            secondaryBeams = group.size > 2,
//            stemDirectionUp = first.stemUp ?: true,
//            beamStartY = config.trebleBaseline - 30f,
//            beamEndY = config.trebleBaseline - 30f
//        )
//        for (note in group) {
//            result[note] = info
//        }
//    }
//    return result
//}

private fun DrawScope.drawLedgerLines(
    x: Float,
    y: Float,
    topY: Float,
    bottomY: Float,
    spacing: Float,
    color: Color
) {
    val lineBoundExt = 12f
    val halfSpacing = spacing / 2f

    if (y < topY - halfSpacing) {
        var curL = topY - spacing
        while (curL >= y - 0.1f) {
            drawLine(color.copy(alpha = 0.5f), Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = 1.2f)
            curL -= spacing
        }
    } else if (y > bottomY + halfSpacing) {
        var curL = bottomY + spacing
        while (curL <= y + 0.1f) {
            drawLine(color.copy(alpha = 0.5f), Offset(x - lineBoundExt, curL), Offset(x + lineBoundExt, curL), strokeWidth = 1.2f)
            curL += spacing
        }
    }
}

private fun snap(value: Float): Float = kotlin.math.round(value)



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
    beatsPerMeasure: Float
): List<RestSymbol> {
    val staffNotes = notes.filter { note ->
        !note.isRest && (staffAssignmentMap[note] ?: (note.midiNote >= 60)) == isTreble
    }.sortedBy { it.position }

    if (staffNotes.isEmpty()) {
        return listOf(RestSymbol(position = 0.0f, duration = beatsPerMeasure))
    }

    val rests = mutableListOf<RestSymbol>()
    var currentPos = 0.0f

    for (note in staffNotes) {
        if (note.position > currentPos + 0.05f) {
            rests.add(RestSymbol(position = currentPos, duration = note.position - currentPos))
        }
        currentPos = maxOf(currentPos, note.position + note.duration)
    }

    if (currentPos < beatsPerMeasure - 0.05f) {
        rests.add(RestSymbol(position = currentPos, duration = beatsPerMeasure - currentPos))
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
    leftHandColor: Color = Color(0xFF6C63FF),
    rightHandColor: Color = Color(0xFF4A90E2),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    val isDark = isSystemInDarkTheme()
    val textMeasurer = rememberTextMeasurer()

    val stableActiveNotes by remember(activeMidiNotes) {
        derivedStateOf { activeMidiNotes.filterValues { it }.keys }
    }

    val labelCache = remember(textMeasurer) {
        (0..11).associateWith { i ->
            textMeasurer.measure(
                text = MusicConstants.NOTE_LABELS[i].take(1),
                style = TextStyle(color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
            )
        }
    }
    val sharpCache = remember(textMeasurer) {
        textMeasurer.measure("♯", TextStyle(color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold))
    }
    val flatCache = remember(textMeasurer) {
        textMeasurer.measure("♭", TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        if (score != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 12.dp)
            ) {
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
                        color = if (isDark) Color(0xFFEEEEEE) else Color(0xFF1E1E1E),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("𝅘𝅥 =", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            Text(
                                text = "${score.tempo.toInt()} BPM",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFCCCCCC) else Color(0xFF444444)
                            )
                        }
                        Text("•", fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.5f))
                        Text(
                            text = score.timeSignature,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCCCCCC) else Color(0xFF444444)
                        )
                        Text("•", fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.5f))
                        Text(
                            text = score.keySignature.keyName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                score.measures.chunked(2).forEachIndexed { rowIndex, rowMeasures ->
                    val rowHeight = 260.dp
                    Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val isTab = currentFormat.contains("Tab", ignoreCase = true)
                            val lineSpacing = if (isTab) 20.dp.toPx() else 14.dp.toPx()
                            val trebleBaseline = 40f
                            val bassBaseline = trebleBaseline + (6 * lineSpacing)
                            val tabBaseline = 60f
                            val clefTextColor = if (isDark) Color(0xFFEAEAEA) else Color(0xFF121212)
                            val leftPadding = 12f
                            val rightPadding = 12f
                            val availableWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(100f)
                            val measureWidth = availableWidth / rowMeasures.size.coerceAtLeast(1)

                            rowMeasures.forEachIndexed { mIdxInRow, measure ->
                                 val mIdx = (rowIndex * 2) + mIdxInRow
                                 val startX = snap(leftPadding + (mIdxInRow * measureWidth))
                                 val endX = snap(startX + measureWidth)

                                 val isPlayheadInThisMeasure = (mIdx == playheadMeasure)

                                 // 1. Create config FIRST so it's in scope for all drawing functions
                                 val config = DrawingConfig(
                                     lineSpacing = lineSpacing,
                                     trebleBaseline = trebleBaseline,
                                     bassBaseline = bassBaseline,
                                     primaryColor = primaryColor,
                                     leftHandColor = leftHandColor,
                                     rightHandColor = rightHandColor,
                                     isLeftHandHidden = isLeftHandHidden,
                                     isRightHandHidden = isRightHandHidden,
                                     isDark = isDark,
                                     isTab = isTab,
                                     tabBaseline = trebleBaseline,
                                     showLabels = showLetters,
                                     showLyrics = showLyrics
                                 )

                                 // 2. Now call drawGrandStaffSystem using the config
                                 drawGrandStaffSystem(
                                     ds = this,
                                     startX = startX,
                                     endX = endX,
                                     trebleY = trebleBaseline,
                                     bassY = bassBaseline,
                                     spacing = lineSpacing,
                                     isFinal = (mIdx == score.measures.lastIndex),
                                     color = Color(0xFF4A4945).copy(alpha = 0.5f),
                                     drawLeftBar = (mIdxInRow == 0),
                                     config = config
                                 )

                                 val barType = if (mIdx == score.measures.lastIndex) "final" else "single"
                                 drawGrandStaffSystemBarline(
                                     type = barType,
                                     x = endX,
                                     trebleTop = trebleBaseline,
                                     trebleBottom = trebleBaseline + (4 * lineSpacing),
                                     bassTop = bassBaseline,
                                     bassBottom = bassBaseline + (4 * lineSpacing),
                                     color = clefTextColor.copy(alpha = 0.2f)
                                 )

                                val measureLabel = "M.${mIdx + 1}"
                                val labelLayout = textMeasurer.measure(
                                    text = measureLabel,
                                    style = TextStyle(color = primaryColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )

                                drawText(
                                    textLayoutResult = labelLayout,
                                    topLeft = Offset(startX + 5f, (if (isTab) tabBaseline else trebleBaseline) - (lineSpacing * 2f))
                                )

                                if (isTab) {
                                    drawTabMeasureContent(
                                        drawScope = this, startX = startX, endX = endX, h = size.height,
                                        measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                        playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                        config = config, labelLayout = labelLayout, tBase = trebleBaseline, textMeasurer = textMeasurer,
                                        isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                        timeSignature = score.timeSignature, drawHeaders = (mIdxInRow == 0)
                                    )
                                } else {
                                    drawStaffMeasureContent(
                                        drawScope = this, startX = startX, endX = endX, h = size.height,
                                        measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                        playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                        config = config, textMeasurer = textMeasurer,
                                        noteLabels = MusicConstants.NOTE_LABELS,
                                        diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                        isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                        timeSignature = score.timeSignature, drawHeaders = (mIdxInRow == 0),
                                        labelCache = labelCache, sharpCache = sharpCache, flatCache = flatCache
                                    )
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

private fun DrawScope.drawMeasureBarline(type: String, x: Float, topY: Float, bottomY: Float, color: Color) {
    when (type) {
        "double" -> {
            drawLine(color, Offset(x - 4.dp.toPx(), topY), Offset(x - 4.dp.toPx(), bottomY), strokeWidth = 1f)
            drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = 1f)
        }
        "final" -> {
            drawLine(color, Offset(x - 5.dp.toPx(), topY), Offset(x - 5.dp.toPx(), bottomY), strokeWidth = 1f)
            drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = 3.dp.toPx())
        }
        else -> drawLine(color, Offset(x, topY), Offset(x, bottomY), strokeWidth = 1f)
    }
}

private fun DrawScope.drawGrandStaffSystemBarline(
    type: String,
    x: Float,
    trebleTop: Float,
    trebleBottom: Float,
    bassTop: Float,
    bassBottom: Float,
    color: Color
) {
    val finalStroke = 3.dp.toPx()
    val thinStroke = 1.dp.toPx()
    val gap = 4.dp.toPx()

    when (type) {
        "final" -> {
            drawLine(color, Offset(x, trebleTop), Offset(x, trebleBottom), strokeWidth = finalStroke)
            drawLine(color, Offset(x, bassTop), Offset(x, bassBottom), strokeWidth = finalStroke)
            drawLine(color, Offset(x - gap - 1f, trebleTop), Offset(x - gap - 1f, trebleBottom), strokeWidth = thinStroke)
            drawLine(
                color = color,
                start = Offset(x - gap - 1f, bassTop),
                end = Offset(x - gap - 1f, bassBottom),
                strokeWidth = thinStroke
            )
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
    isFinalBar: Boolean, isMeasureActive: Boolean, timeSignature: String, drawHeaders: Boolean
) {
    with(drawScope) {
        val clefTextColor = if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212)
        val systemContentStartX = startX + if (drawHeaders) 85f else 0f
        val usableMeasureWidth = (endX - systemContentStartX - 10f).coerceAtLeast(100f)
        
        if (drawHeaders) {
            val tabHeaderLayout = textMeasurer.measure("T\nA\nB", TextStyle(color = clefTextColor, fontSize = 15.sp, fontWeight = FontWeight.Black))
            drawText(tabHeaderLayout, topLeft = Offset(startX + 5f, tBase + (config.lineSpacing * 0.5f)))

            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                val tsLayout = textMeasurer.measure("${tsParts[0]}\n${tsParts[1]}", TextStyle(color = clefTextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                drawText(tsLayout, topLeft = Offset(startX + 45f, tBase + config.lineSpacing * 1.0f))
            }
        }

        drawMeasureBarline(if (isFinalBar) "final" else "single", endX, tBase, tBase + (5 * config.lineSpacing), clefTextColor.copy(alpha = 0.2f))

        val guitarOpenStringsMidi = listOf(64, 59, 55, 50, 45, 40)
        val beatsPerMeasure = measure.timeSignatureNumerator.toFloat()
        measure.notes.forEach { note ->
            if (note.isRest) return@forEach
            val nx = snap(systemContentStartX + (usableMeasureWidth * note.position / beatsPerMeasure))
            val stringIdx = note.string ?: guitarOpenStringsMidi.indexOfFirst { it <= note.midiNote }.coerceIn(0, 5)
            val fretValue = note.fret ?: (note.midiNote - guitarOpenStringsMidi[stringIdx]).coerceIn(0, 24)

            val ny = tBase + (stringIdx * config.lineSpacing)
            val isSounding = isMeasureActive && ((playingNote != null && note.midiNote == playingNote) || activeMidiNotes.contains(note.midiNote))

            drawCircle(
                color = if (isSounding) config.primaryColor else if (config.isDark) Color(0xFF2C2C2A) else Color(0xFFFCFBF7),
                radius = 9.dp.toPx(),
                center = Offset(nx, ny)
            )

            val fretLayout = textMeasurer.measure(
                text = fretValue.toString(),
                style = TextStyle(color = if (isSounding) Color.White else clefTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            drawText(fretLayout, topLeft = Offset(nx - fretLayout.size.width / 2f, ny - fretLayout.size.height / 2f))

            if (config.showLyrics && note.lyric.isNotEmpty()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, tBase + 6 * config.lineSpacing + 10f))
            }

            // Render Guitar Articulations (Bends, Barres)
            note.bend?.let { bend ->
                drawBend(Offset(nx, ny), bend.bendSteps, config.primaryColor)
            }
            note.barre?.let { barre ->
                val bTop = tBase + (barre.startString * config.lineSpacing)
                val bBottom = tBase + (barre.endString * config.lineSpacing)
                drawBarre(barre, nx - 12f, bTop, bBottom, config.primaryColor.copy(alpha = 0.6f))
            }
        }

        // Render Rests for Tab staff if applicable (typically not but for consistency)
        val rests = calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { true }, beatsPerMeasure)

        rests.forEach { rest ->
            val rx = snap(systemContentStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            val ry = tBase + 2.5f * config.lineSpacing
            
            val restWidth = 15f
            val restHeight = if (rest.duration >= 2.0f) 10f else 30f
            val restScale = config.lineSpacing / 15f

            withTransform({
                translate(left = rx - (restWidth * restScale) / 2f, top = ry - (restHeight * restScale) / 2f)
                scale(scaleX = restScale, scaleY = restScale, pivot = Offset.Zero)
            }) {
                with(MusicVectorSymbols) {
                    when {
                        rest.duration >= 4.0f -> drawRestWhole(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                        rest.duration >= 2.0f -> drawRestHalf(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                        rest.duration >= 1.0f -> drawRestQuarter(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                        rest.duration >= 0.5f -> drawRestEighth(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                        // Add others if needed
                    }
                }
            }
        }

        // Navigation marks (Segno, Coda)
        if (measure.segno) {
            withTransform({
                translate(left = startX + 5f, top = config.trebleBaseline - 40f)
            }) {
                with(MusicVectorSymbols) {
                    drawSegno(config.primaryColor, Size(20f, 20f))
                }
            }
        }
        if (measure.coda) {
            withTransform({
                translate(left = endX - 25f, top = config.trebleBaseline - 40f)
            }) {
                with(MusicVectorSymbols) {
                    drawCoda(config.primaryColor, Size(20f, 20f))
                }
            }
        }


        if (playheadOffset != null) {
            val soundingNote = measure.notes.firstOrNull { !it.isRest && isMeasureActive && ((playingNote != null && it.midiNote == playingNote) || activeMidiNotes.contains(it.midiNote)) }
            val playheadX = if (soundingNote != null) {
                val beatsPerMeasure = measure.timeSignatureNumerator.toFloat().coerceAtLeast(1f)
                snap(systemContentStartX + (usableMeasureWidth * soundingNote.position / beatsPerMeasure))
            } else {
                systemContentStartX + (usableMeasureWidth * playheadOffset)
            }
            drawLine(
                color = config.primaryColor,
                start = Offset(playheadX, tBase - 10f),
                end = Offset(playheadX, tBase + (5 * config.lineSpacing) + 10f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
private fun drawStaffMeasureContent(
    drawScope: DrawScope, startX: Float, endX: Float, h: Float,
    measure: Measure, activeMidiNotes: Set<Int>, playingNote: Int?,
    playheadOffset: Float?, config: DrawingConfig, textMeasurer: TextMeasurer,
    noteLabels: List<String>, diatonicSteps: IntArray, isFinalBar: Boolean,
    isMeasureActive: Boolean, timeSignature: String, drawHeaders: Boolean,
    labelCache: Map<Int, TextLayoutResult>, sharpCache: TextLayoutResult, flatCache: TextLayoutResult
) {
    with(drawScope) {
        val clefTextColor = if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212)
        val trebleBaseline = 40f
        val bassBaseline = trebleBaseline + (6 * config.lineSpacing)
        val staffLineColor = if (config.isDark) Color(0xFF4A4945) else Color(0xFFCCCCCC)
        val halfLineSpacing = config.lineSpacing / 2f

        val symbolAreaWidth = if (drawHeaders) 85f else 0f
        val notesStartX = startX + symbolAreaWidth

        // Dynamic horizontal scaling: Adjust usable width based on note density
        val noteDensity = measure.notes.size
        val dynamicMeasureWidth = (endX - notesStartX - 10f).coerceAtLeast(100f)
        val usableMeasureWidth = dynamicMeasureWidth

        if (drawHeaders) {
            val clefWidth = 22f
            val trebleHeight = config.lineSpacing * 3.8f
            val bassHeight = config.lineSpacing * 2.5f

            drawTrebleClefSymbol(clefTextColor, Offset(startX + 12f, trebleBaseline - (config.lineSpacing * 0.4f)), Size(clefWidth, trebleHeight))
            drawBassClefSymbol(clefTextColor, Offset(startX + 12f, bassBaseline - (config.lineSpacing * 0.1f)), Size(clefWidth, bassHeight))

            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                val tsStyle = TextStyle(color = clefTextColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                drawText(textMeasurer.measure(tsParts[0], tsStyle), topLeft = Offset(startX + 44f, trebleBaseline - 0.2f * config.lineSpacing))
                drawText(textMeasurer.measure(tsParts[1], tsStyle), topLeft = Offset(startX + 44f, trebleBaseline + 1.8f * config.lineSpacing))
            }
        }

        drawGrandStaffSystemBarline(if (isFinalBar) "final" else "single", endX, trebleBaseline, trebleBaseline + 4 * config.lineSpacing, bassBaseline, bassBaseline + 4 * config.lineSpacing, clefTextColor.copy(alpha = 0.3f))

        // Normalize measure duration beats
        val beatsPerMeasure = measure.timeSignatureNumerator.toFloat()
        
        // Data structure to facilitate beat-level beam grouping for readable notation
        data class RenderNoteInfo(
            val note: ScoreNote,
            val midiNote: Int,
            val isTrebleClef: Boolean,
            val nx: Float,
            val ny: Float,
            val topY: Float,
            val bottomY: Float,
            val beatIndex: Int
        )

        val visibleNotes = measure.notes.filter { !it.isRest }.mapNotNull { note ->
            val midiNote = note.midiNote
            val isTrebleClef = midiNote >= 60
            if (isTrebleClef && config.isRightHandHidden) return@mapNotNull null
            if (!isTrebleClef && config.isLeftHandHidden) return@mapNotNull null

            val normalizedPosition = (note.position / beatsPerMeasure).coerceIn(0f, 0.95f)
            val nx = snap(notesStartX + (usableMeasureWidth * normalizedPosition))
            val diatonicStep = ChordVoicingEngine.getDiatonicStep(midiNote)
            val ny = if (isTrebleClef) {
                trebleBaseline + (38.0f - diatonicStep) * halfLineSpacing
            } else {
                bassBaseline + (26.0f - diatonicStep) * halfLineSpacing
            }
            val topY = if (isTrebleClef) trebleBaseline else bassBaseline
            val bottomY = topY + 4 * config.lineSpacing
            val beatIdx = kotlin.math.floor(note.position).toInt()

            RenderNoteInfo(
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

        // Group notes into beamed clusters (8th/16th notes occurring in the same beat and same clef)
        data class BeamInfo(
            val stemDir: Float,
            val isBeamed: Boolean,
            val beamGroupKey: String?
        )

        // Map note to its beam attributes
        val noteBeamMap = mutableMapOf<ScoreNote, BeamInfo>()
        val groupedByClefAndBeat = visibleNotes.groupBy { "${if (it.isTrebleClef) "treble" else "bass"}_${it.beatIndex}" }

        for ((groupKey, groupNotes) in groupedByClefAndBeat) {
            val beamEligible = groupNotes.filter { it.note.duration <= 1.0f }
            if (beamEligible.size >= 2) {
                // Determine consistent stem direction for the entire beam group
                val avgMidi = beamEligible.map { it.midiNote }.average()
                val isTreble = beamEligible.first().isTrebleClef
                val groupStemDir = if (isTreble) (if (avgMidi >= 71) 1f else -1f) else (if (avgMidi >= 50) 1f else -1f)
                
                beamEligible.forEach { info ->
                    noteBeamMap[info.note] = BeamInfo(stemDir = groupStemDir, isBeamed = true, beamGroupKey = groupKey)
                }
            }
        }

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
            
            // Determine Note Duration Visual Type
            val duration = note.duration
            val isHollow = duration >= 2.0f

            // Draw Stem for non-whole/non-breve notes
            if (duration < 4.0f) {
                drawLine(clefTextColor, Offset(stemX, ny), Offset(stemX, ny + (stemDir * 24f)), strokeWidth = 1.2f)
            }

            // Draw Notehead using Vector Symbols
            val noteScale = config.lineSpacing / 12f
            val noteWidth = when {
                duration >= 16.0f -> 24f
                duration >= 8.0f -> 22f
                duration >= 4.0f -> 20f
                else -> 15f
            }
            val noteHeight = 10f

            withTransform({
                translate(left = nx - (noteWidth * noteScale) / 2f, top = ny - (noteHeight * noteScale) / 2f)
                scale(scaleX = noteScale, scaleY = noteScale, pivot = Offset.Zero)
            }) {
                with(MusicVectorSymbols) {
                    drawNoteHead(beadColor, Size(noteWidth, noteHeight), isHollow)
                }
            }

            // Flags only for isolated non-beamed notes
            if (duration <= 1.0f && !isHollow && beamInfo?.isBeamed != true) {
                with(MusicVectorSymbols) {
                    drawEighthFlag(clefTextColor, stemX, ny + (stemDir * 24f), stemDir < 0)
                }
            }
            
            if (config.showLabels) {
                val labelResult = labelCache[midiNote % 12]!!
                drawText(labelResult, topLeft = Offset(nx - labelResult.size.width / 2f, ny - labelResult.size.height / 2f))
            }

            if (config.showLyrics && note.lyric.isNotEmpty()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, bassBaseline + 5 * config.lineSpacing + 10f))
            }

            val label = noteLabels[midiNote % 12]
            if (label.contains("#") || label.contains("b")) {
                val accSize = if (label.contains("#")) Size(15f, 30f) else Size(10f, 30f)
                val accScale = config.lineSpacing / 20f
                
                withTransform({
                    translate(left = nx - 22f, top = ny - (accSize.height * accScale) / 2f)
                    scale(scaleX = accScale, scaleY = accScale, pivot = Offset.Zero)
                }) {
                    with(MusicVectorSymbols) {
                        if (label.contains("#")) drawSharp(config.primaryColor, accSize) else drawFlat(config.primaryColor, accSize)
                    }
                }
            }
        }

        // Draw Connected Horizontal Beams for beat groups
        groupedByClefAndBeat.values.forEach { groupNotes ->
            val beamedNotes = groupNotes.filter { noteBeamMap[it.note]?.isBeamed == true }.sortedBy { it.nx }
            if (beamedNotes.size >= 2) {
                val stemDir = noteBeamMap[beamedNotes.first().note]?.stemDir ?: 1f
                val firstX = if (stemDir > 0) beamedNotes.first().nx - 5f else beamedNotes.first().nx + 5f
                val lastX = if (stemDir > 0) beamedNotes.last().nx - 5f else beamedNotes.last().nx + 5f
                
                // Primary beam connecting all notes in group at stem tips
                val beamY = if (stemDir > 0) {
                    beamedNotes.maxOf { it.ny + (stemDir * 24f) }
                } else {
                    beamedNotes.minOf { it.ny + (stemDir * 24f) }
                }

                val beamThickness = 3.5f
                // First beam (8th note beam)
                drawLine(
                    color = clefTextColor,
                    start = Offset(firstX, beamY),
                    end = Offset(lastX, beamY),
                    strokeWidth = beamThickness
                )

                // Secondary beam for 16th notes (duration <= 0.5f)
                val sixteenthNotes = beamedNotes.filter { it.note.duration <= 0.5f }
                if (sixteenthNotes.size >= 2) {
                    val sFirstX = if (stemDir > 0) sixteenthNotes.first().nx - 5f else sixteenthNotes.first().nx + 5f
                    val sLastX = if (stemDir > 0) sixteenthNotes.last().nx - 5f else sixteenthNotes.last().nx + 5f
                    val subBeamY = beamY - (stemDir * 5f)
                    drawLine(
                        color = clefTextColor,
                        start = Offset(sFirstX, subBeamY),
                        end = Offset(sLastX, subBeamY),
                        strokeWidth = beamThickness
                    )
                }
            }
        }

        // Render Rests
        val trebleRests = if (!config.isRightHandHidden) calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure) else emptyList()
        val bassRests = if (!config.isLeftHandHidden) calculateRestsForStaff(measure.notes, isTreble = false, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure) else emptyList()

        (trebleRests + bassRests).forEach { rest ->
            val isTreble = trebleRests.contains(rest)
            val rx = snap(notesStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            val ry = if (isTreble) config.trebleBaseline + 2 * config.lineSpacing else config.bassBaseline + 2 * config.lineSpacing
            
            val restWidth = 15f
            val restHeight = if (rest.duration >= 2.0f) 10f else 30f
            val restScale = config.lineSpacing / 15f

            withTransform({
                translate(left = rx - (restWidth * restScale) / 2f, top = ry - (restHeight * restScale) / 2f)
                scale(scaleX = restScale, scaleY = restScale, pivot = Offset.Zero)
            }) {
                with(MusicVectorSymbols) {
                    when {
                        rest.duration >= 4.0f -> drawRestWhole(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                        rest.duration >= 2.0f -> drawRestHalf(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                        rest.duration >= 1.0f -> drawRestQuarter(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                        rest.duration >= 0.5f -> drawRestEighth(clefTextColor.copy(alpha = 0.5f), Size(restWidth, restHeight))
                    }
                }
            }
        }
        // Navigation marks (Segno, Coda)
        if (measure.segno) {
            withTransform({
                translate(left = startX + 5f, top = config.trebleBaseline - 40f)
            }) {
                with(MusicVectorSymbols) {
                    drawSegno(config.primaryColor, Size(20f, 20f))
                }
            }
        }
        if (measure.coda) {
            withTransform({
                translate(left = endX - 25f, top = config.trebleBaseline - 40f)
            }) {
                with(MusicVectorSymbols) {
                    drawCoda(config.primaryColor, Size(20f, 20f))
                }
            }
        }


        if (playheadOffset != null) {
            val soundingNote = measure.notes.firstOrNull { !it.isRest && isMeasureActive && ((playingNote != null && it.midiNote == playingNote) || activeMidiNotes.contains(it.midiNote)) }
            val playheadX = if (soundingNote != null) {
                val normalizedPosition = (soundingNote.position / beatsPerMeasure).coerceIn(0f, 0.95f)
                snap(notesStartX + (usableMeasureWidth * normalizedPosition))
            } else {
                notesStartX + (usableMeasureWidth * playheadOffset)
            }
            drawLine(
                color = config.primaryColor,
                start = Offset(playheadX, config.trebleBaseline - 15f),
                end = Offset(playheadX, config.bassBaseline + (4 * config.lineSpacing) + 15f),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}
private fun DrawScope.drawTrebleClefSymbol(color: Color, topLeft: Offset, size: Size) {
    val path = Path().apply {
        moveTo(topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.95f)
        cubicTo(topLeft.x + size.width * 0.20f, topLeft.y + size.height * 0.90f, topLeft.x + size.width * 0.10f, topLeft.y + size.height * 0.70f, topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.55f)
        cubicTo(topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.45f, topLeft.x + size.width * 0.55f, topLeft.y + size.height * 0.35f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.20f)
        cubicTo(topLeft.x + size.width * 0.35f, topLeft.y + size.height * 0.05f, topLeft.x + size.width * 0.65f, topLeft.y + size.height * 0.02f, topLeft.x + size.width * 0.70f, topLeft.y + size.height * 0.25f)
        cubicTo(topLeft.x + size.width * 0.75f, topLeft.y + size.height * 0.50f, topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.70f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.95f)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawBassClefSymbol(color: Color, topLeft: Offset, size: Size) {
    val path = Path().apply {
        moveTo(topLeft.x + size.width * 0.20f, topLeft.y + size.height * 0.40f)
        cubicTo(topLeft.x + size.width * 0.05f, topLeft.y + size.height * 0.20f, topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.05f, topLeft.x + size.width * 0.55f, topLeft.y + size.height * 0.25f)
        cubicTo(topLeft.x + size.width * 0.75f, topLeft.y + size.height * 0.45f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.85f, topLeft.x + size.width * 0.20f, topLeft.y + size.height * 0.90f)
        lineTo(topLeft.x + size.width * 0.70f, topLeft.y + size.height * 0.90f)
        lineTo(topLeft.x + size.width * 0.70f, topLeft.y + size.height * 0.75f)
        cubicTo(topLeft.x + size.width * 0.50f, topLeft.y + size.height * 0.75f, topLeft.x + size.width * 0.40f, topLeft.y + size.height * 0.65f, topLeft.x + size.width * 0.45f, topLeft.y + size.height * 0.55f)
        cubicTo(topLeft.x + size.width * 0.55f, topLeft.y + size.height * 0.45f, topLeft.x + size.width * 0.50f, topLeft.y + size.height * 0.30f, topLeft.x + size.width * 0.38f, topLeft.y + size.height * 0.30f)
        cubicTo(topLeft.x + size.width * 0.30f, topLeft.y + size.height * 0.30f, topLeft.x + size.width * 0.25f, topLeft.y + size.height * 0.35f, topLeft.x + size.width * 0.28f, topLeft.y + size.height * 0.42f)
        close()
    }
    drawPath(path, color = color)
    drawCircle(color, radius = size.width * 0.08f, center = Offset(topLeft.x + size.width * 0.85f, topLeft.y + size.height * 0.35f))
    drawCircle(color, radius = size.width * 0.08f, center = Offset(topLeft.x + size.width * 0.85f, topLeft.y + size.height * 0.65f))
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
        // Treble 5 lines
        if (!config.isRightHandHidden) {
            for (i in 0 until 5) {
                drawLine(color, Offset(startX, trebleY + i * spacing), Offset(endX, trebleY + i * spacing))
            }
        }
        // Bass 5 lines
        if (!config.isLeftHandHidden) {
            for (i in 0 until 5) {
                drawLine(color, Offset(startX, bassY + i * spacing), Offset(endX, bassY + i * spacing))
            }
        }

        if (drawLeftBar) {
            val barTop = if (config.isRightHandHidden) bassY else trebleY
            val barBottom = if (config.isLeftHandHidden) trebleY + 4 * spacing else bassY + 4 * spacing
            
            if (!config.isLeftHandHidden || !config.isRightHandHidden) {
                drawLine(
                    color = color.copy(alpha = 0.8f),
                    start = Offset(startX, barTop),
                    end = Offset(startX, barBottom),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
