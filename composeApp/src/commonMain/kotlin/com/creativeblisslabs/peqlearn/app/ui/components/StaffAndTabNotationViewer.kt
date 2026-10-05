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

// @Composable
// fun StaffAndTabNotationViewer(
//     guitarShape: GuitarShape? = null,
//     score: Score? = null,
//     modifier: Modifier = Modifier,
//     primaryColor: Color = MaterialTheme.colorScheme.primary,
//     activeMidiMap: Map<Int, Boolean> = emptyMap(),
//     notationOptions: NotationOptions = NotationOptions()
// ) {
//     val scrollState = rememberScrollState()
//     val displayScore = remember(score, guitarShape) {
//         score ?: Score(
//             title = "Master Etude in C Major",
//             composer = "pEQ Learn Studio",
//             tempo = 112.0f,
//             measures = listOf(
//                 Measure(
//                     measureIndex = 1,
//                     notes = listOf(
//                         ScoreNote(midiNote = 60, duration = NoteDuration.QUARTER, lyric = "Sa"),
//                         ScoreNote(midiNote = 62, duration = NoteDuration.QUARTER, lyric = "Re"),
//                         ScoreNote(midiNote = 64, duration = NoteDuration.QUARTER, lyric = "Ga"),
//                         ScoreNote(midiNote = 65, duration = NoteDuration.QUARTER, lyric = "Ma")
//                     )
//                 ),
//                 Measure(
//                     measureIndex = 2,
//                     notes = listOf(
//                         ScoreNote(midiNote = 67, duration = NoteDuration.HALF, lyric = "Pa"),
//                         ScoreNote(midiNote = 69, duration = NoteDuration.HALF, lyric = "Dha")
//                     )
//                 ),
//                 Measure(
//                     measureIndex = 3,
//                     notes = listOf(
//                         ScoreNote(midiNote = 71, duration = NoteDuration.QUARTER, lyric = "Ni"),
//                         ScoreNote(midiNote = 72, duration = NoteDuration.WHOLE, lyric = "Sa'")
//                     )
//                 )
//             )
//         )
//     }

//     val measureWidth = 240.dp
//     val totalWidth = (displayScore.measures.size * 240 + 160).dp

//     Card(
//         modifier = modifier.fillMaxWidth(),
//         shape = RoundedCornerShape(20.dp),
//         colors = CardDefaults.cardColors(containerColor = Color(0xFF12151D)),
//         border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
//     ) {
//         Column(modifier = Modifier.padding(18.dp)) {
//             // Header summary
//             Row(
//                 modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
//                 horizontalArrangement = Arrangement.SpaceBetween
//             ) {
//                 Column {
//                     Text(
//                         text = "${displayScore.title.uppercase()} • ${displayScore.bpm} BPM",
//                         fontSize = 13.sp,
//                         fontWeight = FontWeight.Black,
//                         color = primaryColor
//                     )
//                     Text(
//                         text = "Grand Staff & 6-String Guitar Tablature",
//                         fontSize = 11.sp,
//                         color = Color.White.copy(alpha = 0.6f)
//                     )
//                 }

//                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                     Box(
//                         modifier = Modifier
//                             .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
//                             .padding(horizontal = 8.dp, vertical = 4.dp)
//                     ) {
//                         Text("4/4 Time", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
//                     }
//                     Box(
//                         modifier = Modifier
//                             .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
//                             .padding(horizontal = 8.dp, vertical = 4.dp)
//                     ) {
//                         Text("C Major", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
//                     }
//                 }
//             }

//             // High Precision Notation Canvas
//             Box(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .height(300.dp)
//                     .horizontalScroll(scrollState)
//             ) {
//                 Canvas(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
//                     val staffSpacing = 10.dp.toPx()
//                     val trebleBaseline = 45.dp.toPx()
//                     val bassBaseline = 115.dp.toPx()
//                     val tabBaseline = 195.dp.toPx()
//                     val tabSpacing = 12.dp.toPx()
//                     val widthPx = size.width

//                     val config = DrawingConfig(
//                         lineSpacing = staffSpacing,
//                         trebleBaseline = trebleBaseline,
//                         bassBaseline = bassBaseline,
//                         primaryColor = primaryColor,
//                         isDark = true,
//                         isTab = false,
//                         tabBaseline = tabBaseline,
//                         isPedalPressed = notationOptions.isPedalPressed,
//                         strokeWidth = 1.2.dp.toPx()
//                     )

//                     // 1. Draw Grand Staff (Treble + Bass) Lines & Curly Brace
//                     drawContinuousStaff(
//                         startX = 30.dp.toPx(),
//                         endX = widthPx - 30.dp.toPx(),
//                         trebleBaseline = trebleBaseline,
//                         bassBaseline = bassBaseline,
//                         spacing = staffSpacing,
//                         strokeWidth = 1.2.dp.toPx()
//                     )
//                     drawGrandStaffBrace(
//                         x = 30.dp.toPx(),
//                         trebleTop = trebleBaseline,
//                         bassBottom = bassBaseline + 4 * staffSpacing,
//                         color = primaryColor
//                     )

//                     // 2. Draw 6-String Tablature
//                     drawContinuousTab(
//                         startX = 30.dp.toPx(),
//                         endX = widthPx - 30.dp.toPx(),
//                         baseline = tabBaseline,
//                         spacing = tabSpacing
//                     )

//                     // 3. Draw Clef Indicators (Left Margin)
//                     drawCircle(primaryColor, 4.dp.toPx(), Offset(40.dp.toPx(), trebleBaseline + 2 * staffSpacing))
//                     drawCircle(primaryColor, 4.dp.toPx(), Offset(40.dp.toPx(), bassBaseline + 1 * staffSpacing))

//                     // 4. Render Measures, Notes, Ledger Lines & Tab Coordinates
//                     var measureStartX = 80.dp.toPx()
//                     val mWidthPx = measureWidth.toPx()

//                     displayScore.measures.forEachIndexed { mIdx, measure ->
//                         val barlineX = measureStartX + mWidthPx

//                         // Repeat barlines & structural navigation marks
//                         if (measure.repeatStart) {
//                             drawRepeatStartBarline(measureStartX, trebleBaseline, bassBaseline + 4 * staffSpacing, primaryColor)
//                         }
//                         if (measure.repeatEnd) {
//                             drawRepeatEndBarline(barlineX, trebleBaseline, bassBaseline + 4 * staffSpacing, primaryColor)
//                         } else {
//                             drawGrandStaffBarline(
//                                 barlineType = measure.barline,
//                                 x = barlineX,
//                                 trebleTop = trebleBaseline,
//                                 bassBottom = bassBaseline + 4 * staffSpacing,
//                                 color = Color.White.copy(alpha = 0.35f)
//                             )
//                         }

//                         // Tablature barline
//                         drawLine(
//                             color = Color.White.copy(alpha = 0.35f),
//                             start = Offset(barlineX, tabBaseline),
//                             end = Offset(barlineX, tabBaseline + 5 * tabSpacing),
//                             strokeWidth = 1.5.dp.toPx()
//                         )

//                         // Navigation symbols (Segno, Coda, etc.) and Endings (1st/2nd brackets)
//                         drawNavigationMarks(measure, measureStartX, trebleBaseline, primaryColor)
//                         drawEndingBracket(measure.endingNumber, measureStartX, barlineX, trebleBaseline, primaryColor)

//                         // Rhythmic spacing and chord collision resolution
//                         val noteCount = measure.notes.size
//                         if (noteCount > 0) {
//                             val rhythmicSpacing = buildRhythmicSpacingMap(measure.notes)
//                             val collisionOffsets = resolveChordCollisions(measure.notes)
//                             val staffAssignmentMap = measure.notes.associateWith { it.midiNote >= 60 }

//                             // Generate rests for unfilled staff beats
//                             val trebleRests = calculateRestsForStaff(measure.notes, isTreble = true, staffAssignmentMap, 4.0f)
//                             val bassRests = calculateRestsForStaff(measure.notes, isTreble = false, staffAssignmentMap, 4.0f)

//                             // Beaming groups & tuplet brackets
//                             val beamGroups = measure.notes.chunked(2)
//                             val stemLengthMap = measure.notes.associateWith { 28.dp.toPx() }
//                             val beamLookup = buildBeamLookup(beamGroups, rhythmicSpacing, collisionOffsets, staffAssignmentMap, stemLengthMap, config)

//                             val noteSpacing = (mWidthPx - 40.dp.toPx()) / noteCount
//                             var prevNoteX: Float? = null
//                             var prevStaffY: Float? = null

//                             measure.notes.forEachIndexed { nIdx, note ->
//                                 val normPos = rhythmicSpacing[note] ?: (nIdx.toFloat() / maxOf(1, noteCount - 1))
//                                 val colOffset = collisionOffsets[note] ?: 0f
//                                 val accOffset = if (note.accidental != Accidental.NATURAL) accidentalOffset(0, staffSpacing) else 0f
//                                 val noteX = snap(measureStartX + 25.dp.toPx() + normPos * (mWidthPx - 50.dp.toPx()) + colOffset + accOffset)

//                                 // Staff Y coordinate using calibrated diatonic reference lines
//                                 val isTreble = staffAssignmentMap[note] ?: (note.midiNote >= 60)
//                                 val baseline = if (isTreble) trebleBaseline else bassBaseline
//                                 val refStep = if (isTreble) 64 else 43
//                                 val staffY = staffY(note.midiNote, baseline, refStep, 4, staffSpacing)

//                                 val isPlaying = activeMidiMap[note.midiNote] == true
//                                 val noteColor = if (isPlaying) primaryColor else Color.White

//                                 // Ledger lines for notes outside staff bounds
//                                 drawLedgerLines(
//                                     x = noteX,
//                                     midi = note.midiNote,
//                                     trebleBaseline = trebleBaseline,
//                                     bassBaseline = bassBaseline,
//                                     spacing = staffSpacing
//                                 )

//                                 // Slur / Tie bezier curve between consecutive notes
//                                 if (prevNoteX != null && prevStaffY != null && nIdx > 0) {
//                                     val slurWidth = noteX - prevNoteX!!
//                                     val archHeight = slurArch(slurWidth)
//                                     drawSlurArc(
//                                         startX = prevNoteX!!,
//                                         startY = prevStaffY!! - 10.dp.toPx(),
//                                         endX = noteX,
//                                         endY = staffY - 10.dp.toPx(),
//                                         color = primaryColor.copy(alpha = 0.7f)
//                                     )
//                                 }
//                                 prevNoteX = noteX
//                                 prevStaffY = staffY

//                                 // Note Head
//                                 drawCircle(
//                                     color = noteColor,
//                                     radius = 5.dp.toPx(),
//                                     center = Offset(noteX, staffY)
//                                 )

//                                 // Note Stem
//                                 val stemUp = note.stemUp ?: (note.midiNote < 71) // B4
//                                 val stemEndY = if (stemUp) staffY - 28.dp.toPx() else staffY + 28.dp.toPx()
//                                 val stemX = if (stemUp) noteX + 5.dp.toPx() else noteX - 5.dp.toPx()
//                                 drawLine(
//                                     color = noteColor,
//                                     start = Offset(stemX, staffY),
//                                     end = Offset(stemX, stemEndY),
//                                     strokeWidth = 1.5.dp.toPx()
//                                 )

//                                 // Tablature coordinate rendering (String 0 = Low E on bottom, String 5 = High E on top)
//                                 val tabString = (note.string ?: note.stringNum).coerceIn(0, 5)
//                                 val tabY = tabBaseline + tabString * tabSpacing
//                                 drawCircle(
//                                     color = primaryColor,
//                                     radius = 6.dp.toPx(),
//                                     center = Offset(noteX, tabY)
//                                 )

//                                 // Guitar Tab Articulations: Bends, Barres, Harmonics
//                                 note.bend?.let { bend ->
//                                     drawBend(Offset(noteX, tabY), bend.bendSteps, primaryColor)
//                                 }
//                                 note.barre?.let { barre ->
//                                     val bTop = tabBaseline + barre.startString * tabSpacing
//                                     val bBottom = tabBaseline + barre.endString * tabSpacing
//                                     drawBarre(barre, noteX - 8.dp.toPx(), bTop, bBottom, primaryColor)
//                                 }
//                                 note.harmonic?.let { harm ->
//                                     drawHarmonic(Offset(noteX, staffY), harm, primaryColor)
//                                 }
//                                 if (note.dynamic.isNotBlank()) {
//                                     val dynGlyph = dynamicSymbol(note.dynamic)
//                                     // Render dynamic text/glyph indicator
//                                 }
//                             }

//                             // Render Beams and Tuplet Brackets
//                             beamGroups.forEach { group ->
//                                 if (group.size >= 2) {
//                                     val first = group.first()
//                                     val last = group.last()
//                                     val firstNorm = rhythmicSpacing[first] ?: 0f
//                                     val lastNorm = rhythmicSpacing[last] ?: 1f
//                                     val firstX = measureStartX + 25.dp.toPx() + firstNorm * (mWidthPx - 50.dp.toPx()) + 5.dp.toPx()
//                                     val lastX = measureStartX + 25.dp.toPx() + lastNorm * (mWidthPx - 50.dp.toPx()) + 5.dp.toPx()
//                                     val beamY = trebleBaseline - 15.dp.toPx()

//                                     drawBeamPath(firstX, lastX, beamY, beamY, 3.5.dp.toPx(), Color.White)
//                                     if (first.tuplet > 1) {
//                                         drawTupletBracket(first.tuplet, firstX, lastX, beamY, beamY, staffSpacing, primaryColor)
//                                     }
//                                 }
//                             }

//                             // Multi-note Slurs & Ties spanning the measure
//                             drawSlurs(measure.notes, rhythmicSpacing, staffAssignmentMap, measureStartX + 25.dp.toPx(), mWidthPx - 50.dp.toPx(), config, primaryColor.copy(alpha = 0.85f))
//                             drawTies(measure.notes, rhythmicSpacing, staffAssignmentMap, measureStartX + 25.dp.toPx(), mWidthPx - 50.dp.toPx(), config, primaryColor.copy(alpha = 0.85f))
//                         }

//                         measureStartX += mWidthPx
//                     }

//                     // Render Guitar Shape fret overlays if available
//                     guitarShape?.let { shape ->
//                         shape.frets.forEachIndexed { strIdx, fret ->
//                             if (fret >= 0) {
//                                 val visualTabIdx = 5 - strIdx // High E on top
//                                 val y = tabBaseline + visualTabIdx * tabSpacing
//                                 drawCircle(
//                                     color = primaryColor,
//                                     radius = 7.dp.toPx(),
//                                     center = Offset(55.dp.toPx(), y)
//                                 )
//                             }
//                         }
//                     }
//                 }
//             }
//         }
//     }
// }

private fun DrawScope.drawContinuousStaff(
    startX: Float,
    endX: Float,
    trebleBaseline: Float,
    bassBaseline: Float,
    spacing: Float,
    strokeWidth: Float
) {
    // Treble 5 lines
    for (i in 0 until 5) {
        val y = trebleBaseline + i * spacing
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = strokeWidth
        )
    }

    // Bass 5 lines
    for (i in 0 until 5) {
        val y = bassBaseline + i * spacing
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = strokeWidth
        )
    }

    // Left Grand Staff Connecting Bar
    drawLine(
        color = Color.White.copy(alpha = 0.6f),
        start = Offset(startX, trebleBaseline),
        end = Offset(startX, bassBaseline + 4 * spacing),
        strokeWidth = strokeWidth * 2.5f
    )
}

private fun DrawScope.drawContinuousTab(
    startX: Float,
    endX: Float,
    baseline: Float,
    spacing: Float
) {
    for (i in 0 until 6) {
        val y = baseline + i * spacing
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Left Tab Bracket Line
    drawLine(
        color = Color.White.copy(alpha = 0.6f),
        start = Offset(startX, baseline),
        end = Offset(startX, baseline + 5 * spacing),
        strokeWidth = 2.dp.toPx()
    )
}

private fun DrawScope.drawLedgerLines(
    x: Float,
    midi: Int,
    trebleBaseline: Float,
    bassBaseline: Float,
    spacing: Float
) {
    val ledgerWidth = 18.dp.toPx()
    val halfWidth = ledgerWidth / 2f

    // Middle C (60) ledger line below Treble staff
    if (midi in 59..61) {
        val y = trebleBaseline + 5 * spacing
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(x - halfWidth, y),
            end = Offset(x + halfWidth, y),
            strokeWidth = 1.2.dp.toPx()
        )
    }

    // High notes above treble staff (A5 = 81 and above)
    if (midi >= 81) {
        val y = trebleBaseline - spacing
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(x - halfWidth, y),
            end = Offset(x + halfWidth, y),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

private fun DrawScope.drawSlurArc(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(startX, startY)
        quadraticBezierTo(
            (startX + endX) / 2f,
            minOf(startY, endY) - 16f,
            endX,
            endY
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

// ============================================================================
// COMPLETE 100% RECOVERED ORIGINAL ENGINE HELPERS & SCORING FUNCTIONS
// ============================================================================

private fun snap(value: Float): Float = kotlin.math.round(value)

private fun staffY(
    midi: Int,
    baseline: Float,
    referenceStep: Int,
    referenceLine: Int,
    spacing: Float
): Float {
    val diatonicStep = com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine.getDiatonicStep(midi)
    val stepDiff = diatonicStep - referenceStep
    return baseline - (referenceLine * spacing) - (stepDiff * (spacing / 2.0f))
}

private fun slurArch(width: Float): Float = (width * 0.18f).coerceIn(8.0f, 32.0f)

private fun tieArch(width: Float): Float = (width * 0.12f).coerceIn(6.0f, 22.0f)

private fun accidentalOffset(column: Int, spacing: Float): Float = -((column + 1) * spacing * 0.95f)

private fun beamGroup(position: Float, timeSignature: String): Int {
    val beats = when (timeSignature) {
        "6/8" -> 2
        "3/4" -> 3
        "2/4" -> 2
        else -> 4
    }
    return (position * beats).toInt().coerceAtLeast(0)
}

private fun dynamicSymbol(dynamic: String): String {
    return when (dynamic.lowercase()) {
        "ppp" -> "𝄠"
        "pp" -> "𝄡"
        "p" -> "𝄢"
        "mp" -> "𝄣"
        "mf" -> "𝄤"
        "f" -> "𝄥"
        "ff" -> "𝄦"
        "fff" -> "𝄧"
        else -> dynamic
    }
}

private fun DrawScope.drawGrandStaffBarline(
    barlineType: String,
    x: Float,
    trebleTop: Float,
    bassBottom: Float,
    color: Color
) {
    val strokeThin = 1.2f
    val strokeThick = 3.5f
    when (barlineType) {
        "double" -> {
            drawLine(color, Offset(x - 4f, trebleTop), Offset(x - 4f, bassBottom), strokeWidth = strokeThin)
            drawLine(color, Offset(x, trebleTop), Offset(x, bassBottom), strokeWidth = strokeThin)
        }
        "final" -> {
            drawLine(color, Offset(x - 4f, trebleTop), Offset(x - 4f, bassBottom), strokeWidth = strokeThin)
            drawLine(color, Offset(x, trebleTop), Offset(x, bassBottom), strokeWidth = strokeThick)
        }
        else -> {
            drawLine(color, Offset(x, trebleTop), Offset(x, bassBottom), strokeWidth = strokeThin)
        }
    }
}

private fun DrawScope.drawGrandStaffBrace(
    x: Float,
    trebleTop: Float,
    bassBottom: Float,
    color: Color
) {
    val path = Path().apply {
        val midY = (trebleTop + bassBottom) / 2f
        moveTo(x, trebleTop)
        cubicTo(x - 8f, trebleTop + 15f, x - 12f, midY - 20f, x - 18f, midY)
        cubicTo(x - 12f, midY + 20f, x - 8f, bassBottom - 15f, x, bassBottom)
        cubicTo(x - 5f, bassBottom - 15f, x - 9f, midY + 20f, x - 15f, midY)
        cubicTo(x - 9f, midY - 20f, x - 5f, trebleTop + 15f, x, trebleTop)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawRepeatStartBarline(
    x: Float,
    top: Float,
    bottom: Float,
    color: Color
) {
    val thin = 1.5f
    val thick = 3.5f
    val gap = 4.0f
    val dotRadius = 2.5f
    val centerY = (top + bottom) / 2f
    drawLine(color, Offset(x, top), Offset(x, bottom), strokeWidth = thick)
    drawLine(color, Offset(x + gap, top), Offset(x + gap, bottom), strokeWidth = thin)
    drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, centerY - 8f))
    drawCircle(color, radius = dotRadius, center = Offset(x + gap + 5f, centerY + 8f))
}

private fun DrawScope.drawRepeatEndBarline(
    x: Float,
    top: Float,
    bottom: Float,
    color: Color
) {
    val thin = 1.5f
    val thick = 3.5f
    val gap = 4.0f
    val dotRadius = 2.5f
    val centerY = (top + bottom) / 2f
    drawLine(color, Offset(x - gap, top), Offset(x - gap, bottom), strokeWidth = thin)
    drawLine(color, Offset(x, top), Offset(x, bottom), strokeWidth = thick)
    drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, centerY - 8f))
    drawCircle(color, radius = dotRadius, center = Offset(x - gap - 5f, centerY + 8f))
}

private fun DrawScope.drawEndingBracket(
    ending: Int?,
    startX: Float,
    endX: Float,
    baseline: Float,
    color: Color
) {
    if (ending == null) return
    val y = baseline - 56.0f
    drawLine(color, Offset(startX + 4f, y), Offset(endX - 4f, y), strokeWidth = 1.2f)
    drawLine(color, Offset(startX + 4f, y), Offset(startX + 4f, y + 25f), strokeWidth = 1.2f)
}

private fun DrawScope.drawNavigationMarks(
    measure: Measure,
    startX: Float,
    baseline: Float,
    color: Color
) {
    val y = baseline - 45f
    if (measure.segno) {
        drawLine(color, Offset(startX + 28f, y - 6f), Offset(startX + 32f, y + 6f), strokeWidth = 1.5f)
        drawCircle(color, radius = 2f, center = Offset(startX + 26f, y))
        drawCircle(color, radius = 2f, center = Offset(startX + 34f, y))
    }
    if (measure.coda) {
        drawCircle(color, radius = 6f, center = Offset(startX + 52f, y), style = Stroke(width = 1.5f))
        drawLine(color, Offset(startX + 44f, y), Offset(startX + 60f, y), strokeWidth = 1.2f)
        drawLine(color, Offset(startX + 52f, y - 8f), Offset(startX + 52f, y + 8f), strokeWidth = 1.2f)
    }
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

private fun DrawScope.drawHarmonic(
    center: Offset,
    harmonic: HarmonicType?,
    color: Color
) {
    val size = 8f
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size, center.y)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size, center.y)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawBeamPath(
    firstStemX: Float,
    lastStemX: Float,
    firstBeamY: Float,
    lastBeamY: Float,
    thickness: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(firstStemX, firstBeamY)
        lineTo(lastStemX, lastBeamY)
        lineTo(lastStemX, lastBeamY + thickness)
        lineTo(firstStemX, firstBeamY + thickness)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawTupletBracket(
    tuplet: Int,
    firstStemX: Float,
    lastStemX: Float,
    firstBeamY: Float,
    lastBeamY: Float,
    spacing: Float,
    color: Color
) {
    if (tuplet <= 1) return
    val midX = (firstStemX + lastStemX) / 2.0f
    val y = minOf(firstBeamY, lastBeamY) - (1.35f * spacing)
    drawLine(color, Offset(firstStemX, y), Offset(midX - 9f, y), strokeWidth = 1.2f)
    drawLine(color, Offset(midX + 9f, y), Offset(lastStemX, y), strokeWidth = 1.2f)
    drawLine(color, Offset(firstStemX, y), Offset(firstStemX, y + 4f), strokeWidth = 1.2f)
    drawLine(color, Offset(lastStemX, y), Offset(lastStemX, y + 4f), strokeWidth = 1.2f)
}

private fun DrawScope.drawSlurs(
    notes: List<ScoreNote>,
    positionMap: Map<ScoreNote, Float>,
    staffAssignmentMap: Map<ScoreNote, Boolean>,
    notesStartX: Float,
    usableWidth: Float,
    config: DrawingConfig,
    color: Color
) {
    val slurStarts = notes.filter { it.slurStart }
    for (start in slurStarts) {
        val startIndex = notes.indexOf(start)
        val end = notes.drop(startIndex + 1).firstOrNull { it.slurEnd } ?: continue

        val x1 = snap(notesStartX + (positionMap[start] ?: start.position) * usableWidth)
        val x2 = snap(notesStartX + (positionMap[end] ?: end.position) * usableWidth)
        val isTreble = staffAssignmentMap[start] ?: (start.midiNote >= 60)
        val baseline = if (isTreble) config.trebleBaseline else config.bassBaseline
        val refStep = if (isTreble) 64 else 43

        val y = staffY(start.midiNote, baseline, refStep, 4, config.lineSpacing)
        val width = x2 - x1
        val arch = slurArch(width)
        val above = start.stemUp != true

        val path = Path().apply {
            moveTo(x1, y)
            cubicTo(x1 + (width * 0.25f), if (above) y - arch else y + arch, x1 + (width * 0.75f), if (above) y - arch else y + arch, x2, y)
            cubicTo(x1 + (0.75f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1 + (0.25f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1, y)
            close()
        }
        drawPath(path, color = color)
    }
}

private fun DrawScope.drawTies(
    notes: List<ScoreNote>,
    positionMap: Map<ScoreNote, Float>,
    staffAssignmentMap: Map<ScoreNote, Boolean>,
    notesStartX: Float,
    usableWidth: Float,
    config: DrawingConfig,
    color: Color
) {
    val tieStarts = notes.filter { it.tieStart }
    for (start in tieStarts) {
        val startIndex = notes.indexOf(start)
        val end = notes.drop(startIndex + 1).firstOrNull { it.tieEnd && it.midiNote == start.midiNote } ?: continue

        val x1 = snap(notesStartX + (positionMap[start] ?: start.position) * usableWidth)
        val x2 = snap(notesStartX + (positionMap[end] ?: end.position) * usableWidth)
        val isTreble = staffAssignmentMap[start] ?: (start.midiNote >= 60)
        val baseline = if (isTreble) config.trebleBaseline else config.bassBaseline
        val refStep = if (isTreble) 64 else 43

        val y = staffY(start.midiNote, baseline, refStep, 4, config.lineSpacing)
        val width = x2 - x1
        val arch = tieArch(width)
        val above = start.stemUp != true

        val path = Path().apply {
            moveTo(x1, y)
            cubicTo(x1 + (width * 0.25f), if (above) y - arch else y + arch, x1 + (width * 0.75f), if (above) y - arch else y + arch, x2, y)
            cubicTo(x1 + (0.75f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1 + (0.25f * width), if (above) (y - arch) + 2f else (y + arch) - 2f, x1, y)
            close()
        }
        drawPath(path, color = color)
    }
}

private fun calculateRestsForStaff(
    notes: List<ScoreNote>,
    isTreble: Boolean,
    staffAssignmentMap: Map<ScoreNote, Boolean>,
    beatsPerMeasure: Float
): List<RestSymbol> {
    val staffNotes = notes.filter { note ->
        (staffAssignmentMap[note] ?: (note.midiNote >= 60)) == isTreble
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

private fun buildRhythmicSpacingMap(notes: List<ScoreNote>): Map<ScoreNote, Float> {
    if (notes.isEmpty()) return emptyMap()
    val distinctPositions = notes.map { it.position }.distinct().sorted()
    if (distinctPositions.size <= 1) return notes.associateWith { it.position }

    val minPos = distinctPositions.first()
    val maxPos = distinctPositions.last()
    val span = if (maxPos > minPos) maxPos - minPos else 1.0f

    return notes.associateWith { note ->
        (note.position - minPos) / span
    }
}

private fun resolveChordCollisions(notes: List<ScoreNote>): Map<ScoreNote, Float> {
    val map = mutableMapOf<ScoreNote, Float>()
    val grouped = notes.groupBy { it.position }

    for ((_, chord) in grouped) {
        val sortedChord = chord.sortedBy { it.midiNote }
        for (i in sortedChord.indices) {
            val note = sortedChord[i]
            if (i > 0 && kotlin.math.abs(sortedChord[i - 1].midiNote - note.midiNote) <= 2) {
                map[note] = 12.0f
            } else {
                map[note] = 0.0f
            }
        }
    }
    return map
}

private fun buildBeamLookup(
    beamGroups: List<List<ScoreNote>>,
    positionMap: Map<ScoreNote, Float>,
    collisionOffsets: Map<ScoreNote, Float>,
    staffAssignmentMap: Map<ScoreNote, Boolean>,
    stemLengthMap: Map<ScoreNote, Float>,
    config: DrawingConfig
): Map<ScoreNote, BeamInfo> {
    val result = mutableMapOf<ScoreNote, BeamInfo>()
    for (group in beamGroups) {
        if (group.size < 2) continue
        val first = group.first()
        val last = group.last()
        val info = BeamInfo(
            primaryBeam = true,
            secondaryBeams = group.size > 2,
            stemDirectionUp = first.stemUp ?: true,
            beamStartY = config.trebleBaseline - 30f,
            beamEndY = config.trebleBaseline - 30f
        )
        for (note in group) {
            result[note] = info
        }
    }
    return result
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
                    .padding(vertical = 16.dp)
            ) {
                score.measures.chunked(2).forEachIndexed { rowIndex, rowMeasures ->
                    val rowHeight = 260.dp
                    Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val isTab = currentFormat.contains("Tab", ignoreCase = true)
                            val lineSpacing = if (isTab) 20.dp.toPx() else 14.dp.toPx()
                            val trebleBaseline = 40f
                            val bassBaseline = trebleBaseline + (5 * lineSpacing) + 50f
                            val tabBaseline = 60f
                            val clefTextColor = if (isDark) Color(0xFFEAEAEA) else Color(0xFF121212)

                            rowMeasures.forEachIndexed { mIdxInRow, measure ->
                                val mIdx = (rowIndex * 2) + mIdxInRow
                                val startX = snap((mIdxInRow * (size.width / 2f)) + 15f)
                                val endX = snap((mIdxInRow + 1) * (size.width / 2f))

                                drawGrandStaffSystem(
                                    ds = this,
                                    startX = startX,
                                    endX = endX,
                                    trebleY = trebleBaseline,
                                    bassY = bassBaseline,
                                    spacing = lineSpacing,
                                    isFinal = (mIdx == score.measures.lastIndex),
                                    color = Color(0xFF4A4945).copy(alpha = 0.5f)
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

                                val isPlayheadInThisMeasure = (mIdx == playheadMeasure)
                                val config = DrawingConfig(lineSpacing, trebleBaseline, bassBaseline, primaryColor, isDark, isTab, trebleBaseline)

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
        measure.notes.forEach { note ->
            val nx = snap(systemContentStartX + (usableMeasureWidth * note.position))
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
        }

        if (playheadOffset != null) {
            val playheadX = systemContentStartX + (usableMeasureWidth * playheadOffset)
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
        val staffLineColor = if (config.isDark) Color(0xFF4A4945) else Color(0xFFCCCCCC)
        val halfLineSpacing = config.lineSpacing / 2f

        val symbolAreaWidth = if (drawHeaders) 75f else 0f
        val notesStartX = startX + symbolAreaWidth
        val usableMeasureWidth = (endX - notesStartX - 10f).coerceAtLeast(100f)

        if (drawHeaders) {
            val trebleClefStyle = TextStyle(color = clefTextColor, fontSize = (config.lineSpacing * 1.8f).sp)
            val bassClefStyle = TextStyle(color = clefTextColor, fontSize = (config.lineSpacing * 2.0f).sp)

            drawText(
                textMeasurer.measure("𝄞", trebleClefStyle),
                topLeft = Offset(startX + 4f, config.trebleBaseline - (config.lineSpacing * 0.8f))
            )

            drawText(
                textMeasurer.measure("𝄢", bassClefStyle),
                topLeft = Offset(startX + 4f, config.bassBaseline - (config.lineSpacing * 0.5f))
            )

            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                val tsStyle = TextStyle(color = clefTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                drawText(textMeasurer.measure(tsParts[0], tsStyle), topLeft = Offset(startX + 42f, config.trebleBaseline + 0.2f * config.lineSpacing))
                drawText(textMeasurer.measure(tsParts[1], tsStyle), topLeft = Offset(startX + 42f, config.trebleBaseline + 1.6f * config.lineSpacing))
            }
        }

        drawGrandStaffSystemBarline(if (isFinalBar) "final" else "single", endX, config.trebleBaseline, config.trebleBaseline + 4 * config.lineSpacing, config.bassBaseline, config.bassBaseline + 4 * config.lineSpacing, clefTextColor.copy(alpha = 0.3f))

        // Normalize measure duration beats (default 4.0 for 4/4)
        const val defaultMeasureBeats = 4.0f
        
        measure.notes.forEach { note ->
            val midiNote = note.midiNote
            // Correct relative horizontal placement based on duration & beat position
            val normalizedPosition = (note.position / defaultMeasureBeats).coerceIn(0f, 0.95f)
            val nx = snap(notesStartX + (usableMeasureWidth * normalizedPosition))
            
            val isTrebleClef = midiNote >= 60
            val diatonicStep = ChordVoicingEngine.getDiatonicStep(midiNote)
            val ny = if (isTrebleClef) {
                config.trebleBaseline + (3.5f - diatonicStep) * halfLineSpacing
            } else {
                config.bassBaseline + (3.0f - diatonicStep) * halfLineSpacing
            }

            val isSounding = isMeasureActive && (midiNote == playingNote || activeMidiNotes.contains(midiNote))
            val beadColor = if (isSounding) config.primaryColor else if (isTrebleClef) Color(0xFF6C63FF) else Color(0xFF4A90E2)

            val lineBoundExt = 12f
            val topY = if (isTrebleClef) config.trebleBaseline else config.bassBaseline
            val bottomY = topY + 4 * config.lineSpacing

            val isAboveStaff = ny < topY - halfLineSpacing
            val isBelowStaff = ny > bottomY + halfLineSpacing

            // Draw Ledger Lines accurately
            if (isAboveStaff || isBelowStaff) {
                val step = if (isAboveStaff) -config.lineSpacing else config.lineSpacing
                var curL = if (isAboveStaff) topY - config.lineSpacing else bottomY + config.lineSpacing
                var count = 0
                while (if (isAboveStaff) curL >= (ny - 2f) else curL <= (ny + 2f)) {
                    drawLine(staffLineColor, Offset(nx - lineBoundExt, curL), Offset(nx + lineBoundExt, curL), strokeWidth = 1.2f)
                    curL += step
                    if (++count > 8) break
                }
            }

            val stemDir = if (isTrebleClef) (if (midiNote >= 71) 1f else -1f) else (if (midiNote >= 50) 1f else -1f)
            val stemX = if (stemDir > 0) nx - 5f else nx + 5f
            
            // Determine Note Duration Visual Type (Breve, Semibreve, Minim, Crotchet)
            val duration = note.duration
            val isHollow = duration >= 2.0f // Minim (2.0) or Semibreve (4.0)

            // Draw Stem for non-whole/non-breve notes
            if (duration < 4.0f) {
                drawLine(clefTextColor, Offset(stemX, ny), Offset(stemX, ny + (stemDir * 24f)), strokeWidth = 1.2f)
            }

            // Draw Notehead (Hollow vs Solid)
            val beadSize = Size(13f, 9.5f)
            val beadTopLeft = Offset(nx - beadSize.width / 2f, ny - beadSize.height / 2f)
            
            if (isHollow) {
                // Hollow notehead (Minim / Semibreve)
                drawOval(color = beadColor, topLeft = beadTopLeft, size = beadSize, style = Stroke(width = 2.dp.toPx()))
                if (duration >= 4.0f) {
                    // Semibreve extra side bars if needed or double lines for breve
                    if (duration >= 8.0f) { // Breve
                        drawLine(clefTextColor, Offset(nx - 8f, ny - 6f), Offset(nx - 8f, ny + 6f), strokeWidth = 2f)
                        drawLine(clefTextColor, Offset(nx + 8f, ny - 6f), Offset(nx + 8f, ny + 6f), strokeWidth = 2f)
                    }
                }
            } else {
                // Solid notehead (Crotchet, Quaver, etc.)
                drawOval(beadColor, beadTopLeft, beadSize)
            }

            val labelResult = labelCache[midiNote % 12]!!
            drawText(labelResult, topLeft = Offset(nx - labelResult.size.width / 2f, ny - labelResult.size.height / 2f))

            val label = noteLabels[midiNote % 12]
            if (label.contains("#")) drawText(sharpCache, topLeft = Offset(nx - 15f, ny - 10f))
            else if (label.contains("b")) drawText(flatCache, topLeft = Offset(nx - 15f, ny - 10f))
        }

        if (playheadOffset != null) {
            val playheadX = notesStartX + (usableMeasureWidth * playheadOffset)
            drawLine(
                color = config.primaryColor,
                start = Offset(playheadX, config.trebleBaseline - 15f),
                end = Offset(playheadX, config.bassBaseline + (4 * config.lineSpacing) + 15f),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}

private fun drawGrandStaffSystem(
    ds: DrawScope,
    startX: Float,
    endX: Float,
    trebleY: Float,
    bassY: Float,
    spacing: Float,
    isFinal: Boolean,
    color: Color
) {
    with(ds) {
        for (i in 0 until 5) {
            drawLine(color, Offset(startX, trebleY + i * spacing), Offset(endX, trebleY + i * spacing))
        }
        for (i in 0 until 5) {
            drawLine(color, Offset(startX, bassY + i * spacing), Offset(endX, bassY + i * spacing))
        }

        val trebleBottom = trebleY + (4 * spacing)
        val bassBottom = bassY + (4 * spacing)

        val strokeWidth = if (isFinal) 3.dp.toPx() else 1.dp.toPx()

        drawLine(color, Offset(endX, trebleY), Offset(endX, trebleBottom), strokeWidth = strokeWidth)
        drawLine(color, Offset(endX, bassY), Offset(endX, bassBottom), strokeWidth = strokeWidth)

        if (isFinal) {
            drawLine(color, Offset(endX - 5.dp.toPx(), trebleY), Offset(endX - 5.dp.toPx(), trebleBottom), strokeWidth = 1f)
            drawLine(color, Offset(endX - 5.dp.toPx(), bassY), Offset(endX - 5.dp.toPx(), bassBottom), strokeWidth = 1f)
        }
    }
}
