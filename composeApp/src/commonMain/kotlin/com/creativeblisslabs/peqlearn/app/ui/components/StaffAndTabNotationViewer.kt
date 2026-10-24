package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
    beatsPerMeasure: Float,
    explicitRests: List<RestSymbol> = emptyList()
): List<RestSymbol> {
    if (explicitRests.isNotEmpty()) {
        return explicitRests
    }

    val staffNotes = notes.filter { note ->
        !note.isRest && (staffAssignmentMap[note] ?: (note.midiNote >= 60)) == isTreble
    }.sortedBy { it.position }

    // Standard musical durations sorted descending
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
                    }
                }

                val showStaff = when {
                    currentFormat.equals("Tab", ignoreCase = true) || currentFormat.equals("TAB", ignoreCase = true) -> false
                    else -> true
                }
                val showTab = when {
                    currentFormat.equals("Staff", ignoreCase = true) || currentFormat.equals("STAFF", ignoreCase = true) -> false
                    else -> true
                }
                val rowHeight = when {
                    compactStaffMode -> if (showStaff && showTab) 220.dp else 150.dp
                    showStaff && showTab -> 380.dp
                    showStaff -> 240.dp
                    else -> 190.dp
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
                                    val tabLineSpacing = if (compactStaffMode) 11.dp.toPx() else 16.dp.toPx()
                                    val trebleBaseline = if (compactStaffMode) 16.dp.toPx() else 28.dp.toPx()
                                    val bassBaseline = trebleBaseline + (5.5f * staffLineSpacing)
                                    val tabBaseline = if (showStaff) bassBaseline + (5.5f * staffLineSpacing) + 12f else (if (compactStaffMode) 16.dp.toPx() else 28.dp.toPx())
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
                                    val tabLineSpacing = if (compactStaffMode) 11.dp.toPx() else 16.dp.toPx()
                                    val trebleBaseline = if (compactStaffMode) 16.dp.toPx() else 28.dp.toPx()
                                    val bassBaseline = trebleBaseline + (5.5f * staffLineSpacing)
                                    val tabBaseline = if (showStaff) bassBaseline + (5.5f * staffLineSpacing) + 12f else (if (compactStaffMode) 16.dp.toPx() else 28.dp.toPx())
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

                                        drawStaffMeasureContent(
                                            drawScope = this, startX = startX, endX = endX, h = size.height,
                                            measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                            playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                            config = staffConfig, textMeasurer = textMeasurer,
                                            noteLabels = MusicConstants.NOTE_LABELS,
                                            diatonicSteps = MusicConstants.CHROMATIC_TO_DIATONIC_STEPS,
                                            isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                            timeSignature = score.timeSignature, drawHeaders = isFirstMeasure,
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
                                            drawLeftBar = isFirstMeasure,
                                            config = tabConfig
                                        )

                                        drawTabMeasureContent(
                                            drawScope = this, startX = startX, endX = endX, h = size.height,
                                            measure = measure, activeMidiNotes = stableActiveNotes, playingNote = playingNote,
                                            playheadOffset = if (isPlayheadInThisMeasure) playheadOffset else null,
                                            config = tabConfig, labelLayout = labelLayout, tBase = tabBaseline, textMeasurer = textMeasurer,
                                            isFinalBar = (mIdx == score.measures.lastIndex), isMeasureActive = isPlayheadInThisMeasure,
                                            timeSignature = score.timeSignature, drawHeaders = isFirstMeasure
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
    isFinalBar: Boolean, isMeasureActive: Boolean, timeSignature: String, drawHeaders: Boolean
) {
    with(drawScope) {
        val colorScheme = config.colorScheme
        val clefTextColor = colorScheme?.onSurface ?: (if (config.isDark) Color(0xFFEAEAEA) else Color(0xFF121212))
        val tabHeaderWidth = if (drawHeaders) 80f else 0f
        val systemContentStartX = startX + tabHeaderWidth
        val usableMeasureWidth = (endX - systemContentStartX - 10f).coerceAtLeast(100f)
        
        if (drawHeaders) {
            // TAB Clef fits across all 6 lines (5 spaces = 5 * lineSpacing)
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

        val guitarOpenStringsMidi = listOf(64, 59, 55, 50, 45, 40)
        val beatsPerMeasure = measure.timeSignatureNumerator.toFloat()
        measure.notes.forEach { note ->
            if (note.isRest) return@forEach
            val nx = snap(systemContentStartX + (usableMeasureWidth * note.position / beatsPerMeasure))
            val stringIdx = note.string ?: guitarOpenStringsMidi.indexOfFirst { it <= note.midiNote }.coerceIn(0, 5)
            val fretValue = note.fret ?: (note.midiNote - guitarOpenStringsMidi[stringIdx]).coerceIn(0, 24)

            val ny = tBase + (stringIdx * config.lineSpacing)
            val isSounding = isMeasureActive && ((playingNote != null && note.midiNote == playingNote) || activeMidiNotes.contains(note.midiNote))

            val fretText = fretValue.toString()
            val fretLayout = textMeasurer.measure(
                text = fretText,
                style = TextStyle(color = if (isSounding) Color.White else clefTextColor, fontSize = 7.sp
                // , fontWeight = FontWeight.Bold
                )
            )

            // Draw compact background capsule/circle so strings above/below are not clipped
            val circleRadius = (config.lineSpacing * 0.42f).coerceAtLeast(6f)
            val pillWidth = maxOf(circleRadius * 2f, fretLayout.size.width + 8f)
            val pillHeight = circleRadius * 2f
            drawRoundRect(
                color = if (isSounding) config.primaryColor else (colorScheme?.surfaceVariant ?: (if (config.isDark) Color(0xFF2C2C2A) else Color(0xFFFCFBF7))),
                topLeft = Offset(nx - pillWidth / 2f, ny - pillHeight / 2f),
                size = Size(pillWidth, pillHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillHeight / 2f, pillHeight / 2f)
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
            val matchingTabNote = if (playingNote != null) {
                measure.notes.firstOrNull { !it.isRest && it.midiNote == playingNote }
            } else null

            val targetPlayheadX = if (matchingTabNote != null) {
                snap(systemContentStartX + (usableMeasureWidth * matchingTabNote.position / beatsPerMeasure))
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
        val trebleBaseline = config.trebleBaseline
        val bassBaseline = config.bassBaseline
        val staffLineColor = colorScheme?.outlineVariant ?: (if (config.isDark) Color(0xFF4A4945) else Color(0xFFCCCCCC))
        val halfLineSpacing = config.lineSpacing / 2f

        val symbolAreaWidth = if (drawHeaders) 85f else 0f
        val notesStartX = startX + symbolAreaWidth

        // Dynamic horizontal scaling: Adjust usable width based on note density
        val noteDensity = measure.notes.size
        val dynamicMeasureWidth = (endX - notesStartX - 10f).coerceAtLeast(100f)
        val usableMeasureWidth = dynamicMeasureWidth

      if (drawHeaders) {
    // 1. CLEFS: Authentic SMuFL / Bravura vector clefs with exact staff line centering
    // G-Clef: G4 line is Line 3 (trebleBaseline + 3 * lineSpacing).
    // In SMuFL font units (height 2375), G4 center is at fraction 0.6316 from visual top.
    // Center G4 spiral on Line 3: trebleClefTop = (trebleBaseline + 3f * config.lineSpacing) - (0.6316f * clefH).
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

    // Bass Clef centered on F line (Line 1 from top = bassBaseline + 1 * lineSpacing)
    // In SMuFL font units (height 1107), F3 dot center is at fraction 0.3171 from visual top.
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

    // 2. TIME SIGNATURE: Authentic SMuFL engraved digits on Treble & Bass staves
    val tsParts = timeSignature.split("/")
    if (tsParts.size == 2) {
        val tsX = startX + maxOf(clefW, bassW) + 10f
        with(MusicVectorSymbols) {
            // Draw on Treble Staff
            drawTimeSignature(
                numerator = tsParts[0],
                denominator = tsParts[1],
                color = clefTextColor,
                topLeft = Offset(tsX, trebleBaseline),
                lineSpacing = config.lineSpacing
            )
            // Draw on Bass Staff
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
        val rawVisibleNotes = measure.notes.filter { !it.isRest }.distinctBy { "${it.position}_${it.midiNote}" }.mapNotNull { note ->
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
        val beamGroupsList = mutableListOf<List<RenderNoteInfo>>()

        // Cluster by clef and beat group: group notes within 1.0 beat (or 2 beats for 4/4 half-measure groupings)
        val groupedByClef = visibleNotes.groupBy { it.isTrebleClef }
        for ((isTreble, clefNotes) in groupedByClef) {
            // Group notes by integer beat or standard 2-beat group for 4/4 time
            val groupedByBeat = clefNotes.groupBy { 
                val beat = kotlin.math.floor(it.note.position).toInt()
                // In standard 4/4, beam eighth notes in pairs/beats: 0, 1, 2, 3
                beat
            }

            for ((beatIdx, beatNotes) in groupedByBeat) {
                // Eligible notes for beaming: duration <= 0.8f (8th notes and 16th notes)
                // Distinct positions required so chord notes at the same time don't count as a group
                val beamEligible = beatNotes.filter { it.note.duration <= 0.8f }
                val distinctPositions = beamEligible.map { it.note.position }.distinct()
                
                if (distinctPositions.size >= 2) {
                    val groupKey = "${if (isTreble) "treble" else "bass"}_$beatIdx"
                    // Consistent stem direction for the entire beam group based on average note pitch
                    val avgMidi = beamEligible.map { it.midiNote }.average()
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
                    beamGroupsList.add(beamEligible)
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
                else -> 16f * noteScale
            }
            val noteHeight = 11.5f * noteScale

            with(MusicVectorSymbols) {
                // 1. Draw Notehead using Authentic SMuFL / Bravura Vector Symbols (OpenSheetMusicDisplay standards)
                when {
                    // Double Whole / Breve notehead
                    duration >= 8.0f -> {
                        drawSmuflSymbol("noteheadDoubleWhole", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    }
                    // Harmonic notehead
                    note.harmonic != null -> {
                        drawSmuflSymbol("noteheadDiamondBlack", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    }
                    // Dead note / Percussion
                    note.articulation == TabArticulation.DEAD_NOTE -> {
                        drawSmuflSymbol("noteheadXBlack", beadColor, Offset(nx - noteWidth / 2f, ny - noteHeight / 2f), Size(noteWidth, noteHeight), 373f)
                    }
                    // Whole note / Semibreve
                    duration >= 4.0f -> {
                        drawNoteHead(
                            color = beadColor,
                            size = Size(noteWidth, noteHeight),
                            isHollow = true,
                            isWhole = true,
                            topLeft = Offset(nx - noteWidth / 2f, ny - noteHeight / 2f)
                        )
                    }
                    // Half note / Minim
                    duration >= 2.0f -> {
                        drawNoteHead(
                            color = beadColor,
                            size = Size(noteWidth, noteHeight),
                            isHollow = true,
                            isWhole = false,
                            topLeft = Offset(nx - noteWidth / 2f, ny - noteHeight / 2f)
                        )
                    }
                    // Crotchet (1/4), Quaver (1/8), Semiquaver (1/16), 32nd, 64th notehead
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

            // Flags only for isolated non-beamed notes (quaver, semiquaver, 32nd, 64th)
            if (duration <= 1.0f && !isHollow && beamInfo?.isBeamed != true) {
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
                val labelResult = labelCache[midiNote % 12]!!
                val labelX = nx - (labelResult.size.width / 2f)
                val labelY = ny - (labelResult.size.height / 2f)
                drawText(labelResult, topLeft = Offset(labelX, labelY))
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
                val isSharp = label.contains("#") || note.accidental == Accidental.SHARP
                val isDoubleSharp = note.accidental == Accidental.DOUBLE_SHARP
                val isDoubleFlat = note.accidental == Accidental.DOUBLE_FLAT
                val isNatural = note.accidental == Accidental.NATURAL && (label.contains("#") || label.contains("b"))
                // Standard SMuFL / Bravura accidental proportions (avoid vertical stretching)
                val accH = config.lineSpacing * 1.35f
                val accW = when {
                    isDoubleSharp -> accH * 0.44f
                    isDoubleFlat -> accH * 0.44f
                    isSharp -> accH * 0.38f
                    isNatural -> accH * 0.28f
                    else -> accH * 0.32f
                }
                
                // Position accidental directly to the left of the notehead
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

        // Draw Connected Horizontal Beams for beat groups perfectly joining all stems
        beamGroupsList.forEach { groupNotes ->
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
        val explicitTrebleRests = measure.rests.filter { it.voice == 0 }
        val explicitBassRests = measure.rests.filter { it.voice == 1 }
        val trebleRests = if (!config.isRightHandHidden) calculateRestsForStaff(measure.notes, isTreble = true, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure, explicitTrebleRests) else emptyList()
        val bassRests = if (!config.isLeftHandHidden) calculateRestsForStaff(measure.notes, isTreble = false, measure.notes.associateWith { it.midiNote >= 60 }, beatsPerMeasure, explicitBassRests) else emptyList()

        (trebleRests + bassRests).forEach { rest ->
            val isTreble = trebleRests.contains(rest)
            val rx = snap(notesStartX + (usableMeasureWidth * (rest.position / beatsPerMeasure)))
            // Standard staff reference: line 1 (top) = baseline, line 2 = +1 spacing, line 3 = +2 spacing, line 4 = +3 spacing, line 5 (bottom) = +4 spacing
            val staffBase = if (isTreble) config.trebleBaseline else config.bassBaseline
            val spacing = config.lineSpacing
            val restScale = spacing / 14f
            val restColor = clefTextColor.copy(alpha = 0.75f)

            with(MusicVectorSymbols) {
                when {
                    // Breve rest (double whole) fills between line 3 and line 4
                    rest.duration >= 8.0f -> {
                        val rw = 12f * restScale
                        val rh = spacing
                        drawRestDoubleWhole(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + spacing))
                    }
                    // Semibreve rest (whole) hangs down from line 4 (second line from top in standard European convention, or fourth line from bottom)
                    // In 5-line staff from top: line 0 = top (F5), line 1 (D5), line 2 (B4/middle), line 3 (G4), line 4 (E4/bottom)
                    // In OpenSheetMusicDisplay: whole rest hangs under line 1 (D5, second line from top: staffBase + 1 * spacing)
                    rest.duration >= 3.5f -> {
                        val rw = 16f * restScale
                        val rh = spacing * 0.95f
                        drawRestWhole(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + spacing))
                    }
                    // Minim rest (half) sits upright on middle line 2 (staffBase + 2 * spacing)
                    rest.duration >= 1.75f -> {
                        val rw = 16f * restScale
                        val rh = spacing * 0.95f
                        drawRestHalf(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + spacing))
                    }
                    // Crotchet rest (quarter) centered around line 2 (middle staff line)
                    rest.duration >= 0.85f -> {
                        val rw = 12f * restScale
                        val rh = spacing * 2.8f
                        drawRestQuarter(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (2f * spacing) - (rh / 2f)))
                    }
                    // Quaver rest (eighth) located between line 2 and line 3
                    rest.duration >= 0.4f -> {
                        val rw = 11f * restScale
                        val rh = spacing * 2.0f
                        drawRestEighth(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (1.5f * spacing)))
                    }
                    // Semiquaver rest (16th)
                    rest.duration >= 0.2f -> {
                        val rw = 13f * restScale
                        val rh = spacing * 2.8f
                        drawRest16th(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (1.2f * spacing)))
                    }
                    // Demisemiquaver rest (32nd)
                    rest.duration >= 0.1f -> {
                        val rw = 14f * restScale
                        val rh = spacing * 3.4f
                        drawRest32nd(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (0.9f * spacing)))
                    }
                    // Hemidemisemiquaver rest (64th)
                    else -> {
                        val rw = 15f * restScale
                        val rh = spacing * 4.0f
                        drawRest64th(restColor, Size(rw, rh), topLeft = Offset(rx - rw / 2f, staffBase + (0.7f * spacing)))
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
            val matchingActiveNote = if (playingNote != null) {
                visibleNotes.firstOrNull { it.midiNote == playingNote }
            } else null

            val targetPlayheadX = if (matchingActiveNote != null) {
                matchingActiveNote.nx
            } else if (computedXByPosition.isNotEmpty()) {
                val targetPos = playheadOffset * beatsPerMeasure
                val keys = computedXByPosition.keys.sorted()
                val exactOrLower = keys.lastOrNull { it <= targetPos }
                val higher = keys.firstOrNull { it > targetPos }
                if (exactOrLower != null && higher != null) {
                    val x1 = computedXByPosition[exactOrLower] ?: (notesStartX + 12f)
                    val x2 = computedXByPosition[higher] ?: (notesStartX + usableMeasureWidth)
                    val fraction = if (higher > exactOrLower) (targetPos - exactOrLower) / (higher - exactOrLower) else 0f
                    x1 + (x2 - x1) * fraction
                } else if (exactOrLower != null) {
                    computedXByPosition[exactOrLower] ?: (notesStartX + (usableMeasureWidth * playheadOffset))
                } else {
                    computedXByPosition[keys.first()] ?: (notesStartX + (usableMeasureWidth * playheadOffset))
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
