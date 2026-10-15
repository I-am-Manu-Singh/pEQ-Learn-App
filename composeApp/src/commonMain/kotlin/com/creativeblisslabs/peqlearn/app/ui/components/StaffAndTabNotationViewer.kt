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
    isPedalPressed: Boolean = false,
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
            if (score.isPdfScore && !score.pdfUri.isNullOrBlank()) {
                PdfDocumentViewer(
                    uriString = score.pdfUri,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
                                text = "${score.tempo.toInt()} BPM${if (score.tempoText.isNotBlank() && score.tempoText != "Moderato") " (${score.tempoText})" else ""}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Text(
                            text = score.timeSignature,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Text(
                            text = score.keySignature.keyName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor.copy(alpha = 0.9f)
                        )
                        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Text(
                            text = "${score.measures.size} Bars",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // Interactive Format Switcher Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Both Viewers", "Staff Only", "Tab Only").forEach { fmt ->
                            val isSelected = currentFormat.equals(fmt, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onFormatChange(fmt) },
                                label = { Text(fmt, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                    selectedLabelColor = primaryColor
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                score.measures.chunked(2).forEachIndexed { rowIndex, rowMeasures ->
                    val showStaff = !currentFormat.equals("Tab Only", ignoreCase = true)
                    val showTab = currentFormat.equals("Both Viewers", ignoreCase = true) || currentFormat.equals("Tab Only", ignoreCase = true)
                    val rowHeight = when {
                        showStaff && showTab -> 380.dp
                        showStaff -> 240.dp
                        else -> 190.dp
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val staffLineSpacing = 14.dp.toPx()
                            val tabLineSpacing = 16.dp.toPx()
                            val trebleBaseline = 40f
                            val bassBaseline = trebleBaseline + (6 * staffLineSpacing)
                            val tabBaseline = if (showStaff) bassBaseline + (6 * staffLineSpacing) + 20f else 45f
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

                                 val measureLabel = "M.${mIdx + 1}"
                                 val labelLayout = textMeasurer.measure(
                                     text = measureLabel,
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

                                     val barType = if (mIdx == score.measures.lastIndex) "final" else "single"
                                     drawGrandStaffSystemBarline(
                                         type = barType,
                                         x = endX,
                                         trebleTop = trebleBaseline,
                                         trebleBottom = trebleBaseline + (4 * staffLineSpacing),
                                         bassTop = bassBaseline,
                                         bassBottom = bassBaseline + (4 * staffLineSpacing),
                                         color = onSurface.copy(alpha = 0.2f)
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
                                         labelCache = labelCache, sharpCache = sharpCache, flatCache = flatCache
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

                                     drawTabMeasureContent(
                                         drawScope = this, startX = startX, endX = endX, h = size.height,
                                         measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                         playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                         config = tabConfig, labelLayout = labelLayout, tBase = tabBaseline, textMeasurer = textMeasurer,
                                         isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                         timeSignature = score.timeSignature, drawHeaders = (mIdxInRow == 0)
                                     )
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
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))
        val systemContentStartX = startX + if (drawHeaders) 85f else 0f
        val usableMeasureWidth = (endX - systemContentStartX - 10f).coerceAtLeast(100f)
        
        if (drawHeaders) {
            with(MusicVectorSymbols) {
                drawTabClef(
                    color = clefTextColor,
                    topLeft = Offset(startX + 8f, tBase + (config.lineSpacing * 0.3f)),
                    size = Size(config.lineSpacing * 1.8f, config.lineSpacing * 4.4f)
                )
            }

            val tsParts = timeSignature.split("/")
            if (tsParts.size == 2) {
                with(MusicVectorSymbols) {
                    drawTimeSignature(
                        numerator = tsParts[0],
                        denominator = tsParts[1],
                        color = clefTextColor,
                        topLeft = Offset(startX + 42f, tBase + (config.lineSpacing * 0.6f)),
                        lineSpacing = config.lineSpacing
                    )
                }
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
                color = if (isSounding) config.primaryColor else (colorScheme?.surfaceVariant ?: (if (config.isDark) Color(0xFF2C2C2A) else Color(0xFFFCFBF7))),
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

            // Render Guitar Articulations (Bends, Barres, Hammer, Pull, Slide, Vibrato, Palm Mute)
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
                TabArticulation.DEAD_NOTE -> {
                    // Rendered as cross notehead on fret
                }
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

        // Render Rests for Tab staff if applicable (typically not but for consistency)
        val rests = calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { true }, beatsPerMeasure)

        rests.forEach { rest ->
            val rx = snap(systemContentStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            val ry = tBase + 2.5f * config.lineSpacing
            val restScale = config.lineSpacing / 14f
            val restColor = clefTextColor.copy(alpha = 0.55f)

            with(MusicVectorSymbols) {
                when {
                    rest.duration >= 4.0f -> drawRestWhole(restColor, Size(16f * restScale, 10f * restScale), topLeft = Offset(rx - 8f * restScale, ry - 5f * restScale))
                    rest.duration >= 2.0f -> drawRestHalf(restColor, Size(16f * restScale, 10f * restScale), topLeft = Offset(rx - 8f * restScale, ry))
                    rest.duration >= 1.0f -> {
                        val rw = 12f * restScale
                        val rh = 28f * restScale
                        drawRestQuarter(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                    else -> {
                        val rw = 11f * restScale
                        val rh = 22f * restScale
                        drawRestEighth(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
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
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))
        val trebleBaseline = 40f
        val bassBaseline = trebleBaseline + (6 * config.lineSpacing)
        val staffLineColor = colorScheme?.outlineVariant ?: (if (config.isDark) Color(0xFF4A4945) else Color(0xFFCCCCCC))
        val halfLineSpacing = config.lineSpacing / 2f

        val symbolAreaWidth = if (drawHeaders) 85f else 0f
        val notesStartX = startX + symbolAreaWidth

        // Dynamic horizontal scaling: Adjust usable width based on note density
        val noteDensity = measure.notes.size
        val dynamicMeasureWidth = (endX - notesStartX - 10f).coerceAtLeast(100f)
        val usableMeasureWidth = dynamicMeasureWidth

      if (drawHeaders) {
    // 1. CLEFS: Authentic SMuFL / Bravura vector clefs from OSMD
    val clefH = config.lineSpacing * 5.2f
    val clefW = clefH * 0.36f // Standard Treble Clef aspect ratio
    
    // Treble Clef centered on G line (line 2 from bottom)
    with(MusicVectorSymbols) {
        drawTrebleClef(
            color = clefTextColor,
            topLeft = Offset(startX + 10f, trebleBaseline - (config.lineSpacing * 0.8f)),
            size = Size(clefW, clefH)
        )
    }

    // Bass Clef centered on F line (line 4 from bottom)
    val bassH = config.lineSpacing * 3.6f
    val bassW = bassH * 0.81f // Standard Bass Clef aspect ratio
    with(MusicVectorSymbols) {
        drawBassClef(
            color = clefTextColor,
            topLeft = Offset(startX + 10f, bassBaseline + (config.lineSpacing * 0.1f)),
            size = Size(bassW, bassH)
        )
    }

    // 2. TIME SIGNATURE: Authentic SMuFL engraved digits
    val tsParts = timeSignature.split("/")
    if (tsParts.size == 2) {
        with(MusicVectorSymbols) {
            drawTimeSignature(
                numerator = tsParts[0],
                denominator = tsParts[1],
                color = clefTextColor,
                topLeft = Offset(startX + 44f, trebleBaseline + (config.lineSpacing * 0.1f)),
                lineSpacing = config.lineSpacing
            )
        }
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

        // 1. Calculate Human-Readable, Mathematically Proportional Note Spacing with Collision Avoidance
        val rawVisibleNotes = measure.notes.filter { !it.isRest }.mapNotNull { note ->
            val midiNote = note.midiNote
            val isTrebleClef = midiNote >= 60
            if (isTrebleClef && config.isRightHandHidden) return@mapNotNull null
            if (!isTrebleClef && config.isLeftHandHidden) return@mapNotNull null
            Triple(note, midiNote, isTrebleClef)
        }.sortedBy { it.first.position }

        // Compute non-linear collision-free x coordinates for notes across measure
        val distinctPositions = rawVisibleNotes.map { it.first.position }.distinct().sorted()
        val minNoteGap = 24f
        val computedXByPosition = mutableMapOf<Float, Float>()

        if (distinctPositions.isNotEmpty()) {
            val totalNaturalSpan = usableMeasureWidth - 20f
            var runningX = notesStartX + 12f
            distinctPositions.forEachIndexed { idx, pos ->
                val timeFraction = (pos / beatsPerMeasure).coerceIn(0f, 0.95f)
                val idealX = notesStartX + 12f + (totalNaturalSpan * timeFraction)
                val targetX = if (idx == 0) idealX else maxOf(idealX, runningX + minNoteGap)
                computedXByPosition[pos] = snap(targetX)
                runningX = targetX
            }
        }

        val visibleNotes = rawVisibleNotes.map { (note, midiNote, isTrebleClef) ->
            val nx = computedXByPosition[note.position] 
                ?: snap(notesStartX + (usableMeasureWidth * (note.position / beatsPerMeasure).coerceIn(0f, 0.95f)))
            val diatonicStep = ChordVoicingEngine.getDiatonicStep(midiNote)
            val ny = if (isTrebleClef) {
                // Diatonic step for F5 (top line) = 38. F5 ny = trebleBaseline.
                trebleBaseline + (38.0f - diatonicStep) * halfLineSpacing
            } else {
                // Diatonic step for A3 (top line of bass staff) = 26. A3 ny = bassBaseline.
                bassBaseline + (26.0f - diatonicStep) * halfLineSpacing
            }
            val topY = if (isTrebleClef) trebleBaseline else bassBaseline
            val bottomY = topY + (4 * config.lineSpacing)
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
            val beamGroupKey: String?,
            val beamY: Float? = null
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
                
                val calculatedBeamY = if (groupStemDir > 0) {
                    beamEligible.maxOf { it.ny + (groupStemDir * 28f) }
                } else {
                    beamEligible.minOf { it.ny + (groupStemDir * 28f) }
                }

                beamEligible.forEach { info ->
                    noteBeamMap[info.note] = BeamInfo(
                        stemDir = groupStemDir, 
                        isBeamed = true, 
                        beamGroupKey = groupKey,
                        beamY = calculatedBeamY
                    )
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

            // Draw Stem for non-whole/non-breve notes, connecting seamlessly to beam line if beamed
            if (duration < 4.0f) {
                val stemEndY = if (beamInfo?.isBeamed == true && beamInfo.beamY != null) {
                    beamInfo.beamY
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
                else -> 15f * noteScale
            }
            val noteHeight = 10f * noteScale

            with(MusicVectorSymbols) {
                when {
                    duration >= 8.0f -> drawSmuflSymbol("noteheadDoubleWhole", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    note.harmonic != null -> drawSmuflSymbol("noteheadDiamondBlack", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    note.articulation == TabArticulation.DEAD_NOTE -> drawSmuflSymbol("noteheadXBlack", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    else -> drawNoteHead(
                        color = beadColor,
                        size = Size(noteWidth, noteHeight),
                        isHollow = isHollow,
                        isWhole = duration >= 4.0f,
                        topLeft = Offset(nx - noteWidth / 2f, ny - noteHeight / 2f)
                    )
                }

                // 2. Augmentation Dot (SMuFL augmentationDot)
                if (note.dotted || note.isDotted) {
                    val dotX = nx + (noteWidth / 2f) + 4f
                    drawSmuflSymbol("augmentationDot", beadColor, Offset(dotX, ny - 3f), Size(5f, 5f), 200f)
                }

                // 3. Musical Articulations (Staccato, Accent, Tenuto, Marcato, Fermata)
                val articY = if (stemDir > 0) ny + (stemDir * 32f) else ny - 16f
                when (note.musicalArticulation) {
                    MusicalArticulation.STACCATO -> drawStaccato(clefTextColor, Offset(nx, articY), radius = 2.2f)
                    MusicalArticulation.ACCENT -> drawAccent(clefTextColor, Size(12f, 8f), Offset(nx - 6f, articY))
                    MusicalArticulation.TENUTO -> drawTenuto(clefTextColor, Size(12f, 3f), Offset(nx - 6f, articY))
                    MusicalArticulation.MARCATO -> drawSmuflSymbol("articMarcatoAbove", clefTextColor, Offset(nx - 6f, articY), Size(10f, 10f), 400f)
                    MusicalArticulation.NONE -> {}
                }

                // 4. Dynamics (p, mp, mf, f, ff, sfz, etc.)
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
            if (duration <= 1.0f && !isHollow && beamInfo?.isBeamed != true) {
                with(MusicVectorSymbols) {
                    if (duration <= 0.25f) {
                        drawSmuflSymbol(if (stemDir > 0) "flag16thDown" else "flag16thUp", clefTextColor, Offset(stemX, if (stemDir > 0) ny + (stemDir * 26f) else ny + (stemDir * 26f) - 22f), Size(10f, 22f), 800f)
                    } else {
                        drawEighthFlag(clefTextColor, stemX, ny + (stemDir * 26f), stemDir < 0)
                    }
                }
            }
            
            if (config.showLabels) {
                val labelResult = labelCache[midiNote % 12]!!
                drawText(labelResult, topLeft = Offset(nx - labelResult.size.width / 2f, ny - labelResult.size.height / 2f))
            }

            if (config.showLyrics && note.lyric.isNotEmpty()) {
                val lyricStyle = TextStyle(color = clefTextColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Normal)
                val lyricLayout = textMeasurer.measure(note.lyric, lyricStyle)
                drawText(lyricLayout, topLeft = Offset(nx - lyricLayout.size.width / 2f, bassBaseline + 5 * config.lineSpacing + 22f))
            }

            // 5. Accidentals (Sharp, Flat, Natural, Double Sharp, Double Flat, Courtesy Parentheses)
            val label = noteLabels[midiNote % 12]
            val hasAccidental = label.contains("#") || label.contains("b") || note.accidental != Accidental.NATURAL
            if (hasAccidental) {
                val accH = config.lineSpacing * 2.2f
                val isSharp = label.contains("#") || note.accidental == Accidental.SHARP
                val isDoubleSharp = note.accidental == Accidental.DOUBLE_SHARP
                val isDoubleFlat = note.accidental == Accidental.DOUBLE_FLAT
                val accW = when {
                    isDoubleSharp -> accH * 0.40f
                    isDoubleFlat -> accH * 0.45f
                    isSharp -> accH * 0.30f
                    else -> accH * 0.32f
                }
                
                with(MusicVectorSymbols) {
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol("accidentalParensLeft", config.primaryColor.copy(alpha = 0.6f), Offset(nx - accW - 14f, ny - accH / 2f), Size(5f, accH), 800f)
                    }
                    when {
                        isDoubleSharp -> drawDoubleSharp(config.primaryColor, Size(accW, accH), Offset(nx - accW - 8f, ny - accH / 2f))
                        isDoubleFlat -> drawDoubleFlat(config.primaryColor, Size(accW, accH), Offset(nx - accW - 8f, ny - accH * 0.7f))
                        isSharp -> drawSharp(config.primaryColor, size = Size(accW, accH), topLeft = Offset(nx - accW - 8f, ny - accH / 2f))
                        else -> drawFlat(config.primaryColor, size = Size(accW, accH), topLeft = Offset(nx - accW - 8f, ny - accH * 0.7f))
                    }
                    if (note.courtesyAccidental) {
                        drawSmuflSymbol("accidentalParensRight", config.primaryColor.copy(alpha = 0.6f), Offset(nx - 7f, ny - accH / 2f), Size(5f, accH), 800f)
                    }
                }
            }
        }

        // Draw Connected Horizontal Beams for beat groups perfectly joining all stems
        groupedByClefAndBeat.values.forEach { groupNotes ->
            val beamedNotes = groupNotes.filter { noteBeamMap[it.note]?.isBeamed == true }.sortedBy { it.nx }
            if (beamedNotes.size >= 2) {
                val firstNoteBeam = noteBeamMap[beamedNotes.first().note]
                val stemDir = firstNoteBeam?.stemDir ?: 1f
                val firstX = if (stemDir > 0) beamedNotes.first().nx - 5f else beamedNotes.first().nx + 5f
                val lastX = if (stemDir > 0) beamedNotes.last().nx - 5f else beamedNotes.last().nx + 5f
                val beamY = firstNoteBeam?.beamY ?: if (stemDir > 0) {
                    beamedNotes.maxOf { it.ny + (stemDir * 28f) }
                } else {
                    beamedNotes.minOf { it.ny + (stemDir * 28f) }
                }

                val beamThickness = 3.8f
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

        // Render Rests using Authentic SMuFL Glyphs
        val trebleRests = if (!config.isRightHandHidden) calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure) else emptyList()
        val bassRests = if (!config.isLeftHandHidden) calculateRestsForStaff(measure.notes, isTreble = false, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure) else emptyList()

        (trebleRests + bassRests).forEach { rest ->
            val isTreble = trebleRests.contains(rest)
            val rx = snap(notesStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            val ry = if (isTreble) config.trebleBaseline + 2 * config.lineSpacing else config.bassBaseline + 2 * config.lineSpacing
            
            val restScale = config.lineSpacing / 14f
            val restColor = clefTextColor.copy(alpha = 0.55f)

            with(MusicVectorSymbols) {
                when {
                    rest.duration >= 4.0f -> drawRestWhole(restColor, Size(16f * restScale, 10f * restScale), topLeft = Offset(rx - 8f * restScale, ry - 5f * restScale))
                    rest.duration >= 2.0f -> drawRestHalf(restColor, Size(16f * restScale, 10f * restScale), topLeft = Offset(rx - 8f * restScale, ry))
                    rest.duration >= 1.0f -> {
                        val rw = 12f * restScale
                        val rh = 28f * restScale
                        drawRestQuarter(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                    else -> {
                        val rw = 11f * restScale
                        val rh = 22f * restScale
                        drawRestEighth(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, ry - rh / 2f))
                    }
                }
            }
        }
        // Navigation & Measure Marks (Segno, Coda, Rehearsal, Endings, Fine, D.C., D.S.)
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
