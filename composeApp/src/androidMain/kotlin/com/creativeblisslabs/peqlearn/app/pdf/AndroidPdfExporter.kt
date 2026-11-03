package com.creativeblisslabs.peqlearn.app.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.creativeblisslabs.peqlearn.app.ui.screens.Accidental
import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.PdfPageSize
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import com.creativeblisslabs.peqlearn.app.ui.screens.TabArticulation
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

/**
 * Studio-Grade High-Resolution Vector PDF Sheet Music Exporter for pEQ Learn.
 */
object AndroidPdfExporter {

    private val NOTE_NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val GUITAR_OPEN_MIDI = intArrayOf(64, 59, 55, 50, 45, 40)

    suspend fun exportScoreToPdf(
        context: Context,
        score: Score,
        options: PdfExportOptions,
        onProgress: (Float) -> Unit,
        onComplete: (String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                onProgress(0.05f)
                val pageSizeEnum = try {
                    PdfPageSize.valueOf(options.pageSize.uppercase())
                } catch (_: Exception) {
                    PdfPageSize.A4
                }

                val isLandscape = options.orientation.equals("Landscape", ignoreCase = true)
                val pageWidth = if (isLandscape) pageSizeEnum.heightPt else pageSizeEnum.widthPt
                val pageHeight = if (isLandscape) pageSizeEnum.widthPt else pageSizeEnum.heightPt

                val document = PdfDocument()
                val margin = 36f
                val usableWidth = pageWidth - (margin * 2f)

                val measures = score.measures
                val totalMeasures = measures.size.coerceAtLeast(1)
                val measuresPerSystem = options.measuresPerSystem.coerceIn(2, 8)

                val isInverted = options.colorTheme.equals("Inverted Dark", ignoreCase = true)
                val isMono = options.colorTheme.equals("Monochrome", ignoreCase = true)

                val canvasBgColor = if (isInverted) Color.parseColor("#121216") else Color.WHITE
                val textColor = if (isInverted) Color.WHITE else Color.BLACK
                val staffLineColor = when {
                    isInverted -> Color.parseColor("#5A5C68")
                    isMono -> Color.BLACK
                    else -> Color.parseColor("#666666")
                }
                val barlineColor = if (isInverted) Color.WHITE else Color.BLACK
                val rhColor = if (isMono) Color.BLACK else if (isInverted) Color.parseColor("#4FC3F7") else Color.parseColor("#4A90E2")
                val lhColor = if (isMono) Color.BLACK else if (isInverted) Color.parseColor("#BA68C8") else Color.parseColor("#6C63FF")
                val accentGold = Color.parseColor("#FFD700")

                val bgPaint = Paint().apply { color = canvasBgColor }
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isInverted) Color.parseColor("#B0FFFFFF") else Color.DKGRAY
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                }
                val composerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isInverted) Color.parseColor("#B0FFFFFF") else Color.DKGRAY
                    textSize = 10f
                    textAlign = Paint.Align.RIGHT
                }
                val staffLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = staffLineColor
                    strokeWidth = 0.9f
                }
                val barlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = barlineColor
                    strokeWidth = 1.3f
                }
                val heavyBarlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = barlineColor
                    strokeWidth = 3.2f
                }
                val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    strokeWidth = 1.2f
                }
                val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    strokeWidth = 3.5f
                }
                val noteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                val noteStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.6f
                }
                val clefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    typeface = Typeface.SERIF
                }
                val accidentalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 11f
                    textAlign = Paint.Align.RIGHT
                }
                val measureNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isInverted) Color.parseColor("#90EE90") else Color.GRAY
                    textSize = 7f
                }
                val chordLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentGold
                    textSize = 5.5f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                }
                val lyricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 6.5f
                    textAlign = Paint.Align.CENTER
                }
                val tabPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isInverted) Color.parseColor("#282830") else Color.parseColor("#E8E8EC")
                    style = Paint.Style.FILL
                }
                val tabFretTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = 6.5f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isInverted) Color.parseColor("#80FFFFFF") else Color.GRAY
                    textSize = 8f
                    textAlign = Paint.Align.CENTER
                }

                val drawStaff = options.staffFormat == "Staff" || options.staffFormat == "Both"
                val drawTab = options.staffFormat == "Tab" || options.staffFormat == "Both"

                val lineSpacing = 7.5f
                val halfSpacing = lineSpacing / 2f
                val trebleStaffHeight = lineSpacing * 4f
                val staffGap = lineSpacing * 3.2f
                val systemHeight = when {
                    drawStaff && drawTab -> trebleStaffHeight * 2f + staffGap + (lineSpacing * 5f) + 36f
                    drawStaff -> trebleStaffHeight * 2f + staffGap + 18f
                    else -> lineSpacing * 5f + 24f
                }
                val systemGap = 24f

                val titleBlockHeight = if (options.includeTitleHeader) 54f else 0f
                val availableHeightFirstPage = pageHeight - (margin * 2f) - titleBlockHeight
                val systemsFirstPage = (availableHeightFirstPage / (systemHeight + systemGap)).toInt().coerceAtLeast(1)
                val systemsPerPage = ((pageHeight - (margin * 2f)) / (systemHeight + systemGap)).toInt().coerceAtLeast(2)

                var currentMeasureIndex = 0
                var pageNumber = 1

                while (currentMeasureIndex < totalMeasures) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                    val isFirstPage = (pageNumber == 1)
                    var currentY = margin

                    if (isFirstPage && options.includeTitleHeader) {
                        val titleText = options.title.ifBlank { score.title.ifBlank { "Score Sheet" } }
                        canvas.drawText(titleText, pageWidth / 2f, currentY + 16f, titlePaint)
                        val composerText = options.composer.ifBlank { score.composer.ifBlank { "pEQ Learn" } }
                        canvas.drawText(composerText, pageWidth - margin, currentY + 28f, composerPaint)
                        canvas.drawText("Tempo: ${score.bpm} BPM | Time: ${score.timeSignature} | Key: ${score.keySignature.keyName}", pageWidth / 2f, currentY + 32f, subtitlePaint)
                        currentY += titleBlockHeight
                    }

                    val maxSystemsThisPage = if (isFirstPage) systemsFirstPage else systemsPerPage
                    var pageSystemCount = 0

                    while (pageSystemCount < maxSystemsThisPage && currentMeasureIndex < totalMeasures) {
                        val measuresInThisSystem = minOf(measuresPerSystem, totalMeasures - currentMeasureIndex)
                        val measureWidth = usableWidth / measuresInThisSystem

                        for (mIdx in 0 until measuresInThisSystem) {
                            val actualMeasureIdx = currentMeasureIndex + mIdx
                            val measure = measures.getOrNull(actualMeasureIdx) ?: continue
                            val mLeft = margin + (mIdx * measureWidth)
                            val mRight = mLeft + measureWidth
                            val isFirstInSystem = (mIdx == 0)
                            val beatsPerM = (measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)

                            val headerOff = if (isFirstInSystem) 56f * (pageWidth / 1920f).coerceIn(0.8f, 1.4f) else 12f
                            val notesStartX = mLeft + headerOff
                            val usableMWidth = (mRight - notesStartX - 16f).coerceAtLeast(40f)

                            val staffY = currentY + 14f
                            val trebleTop = staffY
                            val bassTop = trebleTop + trebleStaffHeight + staffGap
                            val tabTop = if (drawStaff) bassTop + trebleStaffHeight + staffGap * 0.8f else trebleTop

                            if (drawStaff) {
                                for (l in 0..4) {
                                    val ty = trebleTop + (l * lineSpacing)
                                    canvas.drawLine(mLeft, ty, mRight, ty, staffLinePaint)
                                    val by = bassTop + (l * lineSpacing)
                                    canvas.drawLine(mLeft, by, mRight, by, staffLinePaint)
                                }

                                canvas.drawLine(mRight, trebleTop, mRight, trebleTop + trebleStaffHeight, barlinePaint)
                                canvas.drawLine(mRight, bassTop, mRight, bassTop + trebleStaffHeight, barlinePaint)

                                if (isFirstInSystem) {
                                    canvas.drawLine(mLeft, trebleTop, mLeft, bassTop + trebleStaffHeight, barlinePaint)
                                    clefPaint.textSize = lineSpacing * 3.6f
                                    canvas.drawText("𝄞", mLeft + 4f, trebleTop + lineSpacing * 3.2f, clefPaint)
                                    clefPaint.textSize = lineSpacing * 2.8f
                                    canvas.drawText("𝄢", mLeft + 4f, bassTop + lineSpacing * 2.6f, clefPaint)

                                    clefPaint.textSize = lineSpacing * 1.8f
                                    val tsX = mLeft + 32f
                                    canvas.drawText("${measure.timeSignatureNumerator}", tsX, trebleTop + lineSpacing * 1.7f, clefPaint)
                                    canvas.drawText("${measure.timeSignatureDenominator}", tsX, trebleTop + lineSpacing * 3.7f, clefPaint)
                                    canvas.drawText("${measure.timeSignatureNumerator}", tsX, bassTop + lineSpacing * 1.7f, clefPaint)
                                    canvas.drawText("${measure.timeSignatureDenominator}", tsX, bassTop + lineSpacing * 3.7f, clefPaint)
                                }
                            }

                            if (drawTab) {
                                for (l in 0..5) {
                                    val ty = tabTop + (l * lineSpacing)
                                    canvas.drawLine(mLeft, ty, mRight, ty, staffLinePaint)
                                }
                                canvas.drawLine(mRight, tabTop, mRight, tabTop + (5 * lineSpacing), barlinePaint)
                                if (isFirstInSystem) {
                                    canvas.drawLine(mLeft, tabTop, mLeft, tabTop + (5 * lineSpacing), barlinePaint)
                                    clefPaint.textSize = lineSpacing * 1.3f
                                    canvas.drawText("T", mLeft + 6f, tabTop + lineSpacing * 1.3f, clefPaint)
                                    canvas.drawText("A", mLeft + 6f, tabTop + lineSpacing * 2.8f, clefPaint)
                                    canvas.drawText("B", mLeft + 6f, tabTop + lineSpacing * 4.3f, clefPaint)
                                }
                            }

                            val sysBottom = if (drawTab) tabTop + (5 * lineSpacing) else bassTop + trebleStaffHeight
                            val sysTop = if (drawStaff) trebleTop else tabTop
                            canvas.drawLine(mRight, sysTop, mRight, sysBottom, if (actualMeasureIdx == totalMeasures - 1) heavyBarlinePaint else barlinePaint)

                            if (options.includeMeasureNumbers) {
                                canvas.drawText("M.${actualMeasureIdx + 1}", mLeft + 2f, currentY + 8f, measureNumPaint)
                            }

                            val visibleNotes = measure.notes.filter { !it.isRest }.sortedBy { it.position }
                            val distinctPos = visibleNotes.map { it.position }.distinct().sorted()
                            val posToX = HashMap<Float, Float>()
                            if (distinctPos.isNotEmpty()) {
                                val minDur = visibleNotes.minOfOrNull { it.duration.coerceAtLeast(0.125f) } ?: 0.5f
                                val weights = FloatArray(distinctPos.size)
                                var sumW = 0f
                                for (i in distinctPos.indices) {
                                    val nextP = if (i < distinctPos.lastIndex) distinctPos[i + 1] else beatsPerM
                                    val delta = (nextP - distinctPos[i]).coerceAtLeast(minDur)
                                    val w = (delta / minDur).toDouble().pow(0.52).toFloat()
                                    weights[i] = w
                                    sumW += w
                                }
                                var runW = 0f
                                var prevX = notesStartX
                                val minGap = 20f
                                for (i in distinctPos.indices) {
                                    val frac = (runW / sumW.coerceAtLeast(1f)).coerceIn(0f, 0.95f)
                                    val idealX = notesStartX + frac * usableMWidth
                                    val clampedX = (if (i == 0) idealX else maxOf(idealX, prevX + minGap)).coerceIn(notesStartX, mRight - 14f)
                                    posToX[distinctPos[i]] = clampedX
                                    prevX = clampedX
                                    runW += weights[i]
                                }
                            }

                            val noteCoords = HashMap<ScoreNote, Pair<Float, Float>>()
                            val hasVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }

                            if (drawStaff) {
                                for (note in visibleNotes) {
                                    val nx = posToX[note.position] ?: (notesStartX + (note.position / beatsPerM) * usableMWidth)
                                    val isTreble = if (hasVoices) note.voice == 0 else note.midiNote >= 60
                                    val step = ChordVoicingEngine.getDiatonicStep(note.midiNote)
                                    val ny = if (isTreble) {
                                        trebleTop + (38f - step) * halfSpacing
                                    } else {
                                        bassTop + (26f - step) * halfSpacing
                                    }
                                    noteCoords[note] = nx to ny

                                    val sTop = if (isTreble) trebleTop else bassTop
                                    val sBot = sTop + trebleStaffHeight
                                    val ledExt = lineSpacing * 0.75f
                                    if (ny < sTop - halfSpacing * 0.8f) {
                                        var ly = sTop - lineSpacing
                                        while (ly >= ny - halfSpacing * 0.6f) {
                                            canvas.drawLine(nx - ledExt, ly, nx + ledExt, ly, staffLinePaint)
                                            ly -= lineSpacing
                                        }
                                    } else if (ny > sBot + halfSpacing * 0.8f) {
                                        var ly = sBot + lineSpacing
                                        while (ly <= ny + halfSpacing * 0.6f) {
                                            canvas.drawLine(nx - ledExt, ly, nx + ledExt, ly, staffLinePaint)
                                            ly += lineSpacing
                                        }
                                    }

                                    val noteColor = if (options.colorTheme == "Color (LH/RH)") {
                                        if (isTreble) rhColor else lhColor
                                    } else {
                                        textColor
                                    }

                                    val rx = lineSpacing * 0.58f
                                    val ry = lineSpacing * 0.42f
                                    if (note.duration >= 2.0f) {
                                        noteStrokePaint.color = noteColor
                                        canvas.drawOval(nx - rx, ny - ry, nx + rx, ny + ry, noteStrokePaint)
                                    } else {
                                        noteFillPaint.color = noteColor
                                        canvas.drawOval(nx - rx, ny - ry, nx + rx, ny + ry, noteFillPaint)
                                    }

                                    if (note.dotted || note.isDotted) {
                                        noteFillPaint.color = noteColor
                                        canvas.drawCircle(nx + rx + 3.5f, ny - 2f, 2.2f, noteFillPaint)
                                    }

                                    if (options.includeChordLabels) {
                                        val pcVal = ((note.midiNote % 12) + 12) % 12
                                        val nLabel = NOTE_NAMES[pcVal].take(1)
                                        chordLabelPaint.color = if (isInverted || options.colorTheme == "Color (LH/RH)") Color.WHITE else Color.BLACK
                                        chordLabelPaint.textSize = lineSpacing * 0.65f
                                        canvas.drawText(nLabel, nx, ny + 2f, chordLabelPaint)
                                    }

                                    val pcVal = ((note.midiNote % 12) + 12) % 12
                                    if (pcVal in intArrayOf(1, 3, 6, 8, 10) || note.accidental != Accidental.NATURAL) {
                                        val accStr = when (note.accidental) {
                                            Accidental.FLAT -> "♭"
                                            Accidental.DOUBLE_SHARP -> "𝄪"
                                            Accidental.DOUBLE_FLAT -> "𝄫"
                                            else -> "♯"
                                        }
                                        accidentalPaint.color = textColor
                                        accidentalPaint.textSize = lineSpacing * 1.1f
                                        canvas.drawText(accStr, nx - rx - 2.5f, ny + lineSpacing * 0.35f, accidentalPaint)
                                    }
                                }

                                val byClefAndBeat = visibleNotes.groupBy { n ->
                                    val isT = if (hasVoices) n.voice == 0 else n.midiNote >= 60
                                    "${isT}_${floor(n.position).toInt()}"
                                }
                                for ((_, beatGroup) in byClefAndBeat) {
                                    val posMap = beatGroup.groupBy { it.position }.toSortedMap()
                                    val positions = posMap.keys.toList()
                                    val shortPos = positions.filter { p -> (posMap[p]?.minOf { it.duration } ?: 1f) <= 0.76f }

                                    if (shortPos.size >= 2) {
                                        val avgMidi = beatGroup.map { it.midiNote }.average()
                                        val isTreble = if (hasVoices) beatGroup.first().voice == 0 else beatGroup.first().midiNote >= 60
                                        val stemDir = if (isTreble) (if (avgMidi >= 71) 1f else -1f) else (if (avgMidi >= 50) 1f else -1f)
                                        val stemOffX = if (stemDir > 0) -lineSpacing * 0.52f else lineSpacing * 0.52f
                                        val stemLen = lineSpacing * 2.2f

                                        val firstNotes = posMap[shortPos.first()]!!
                                        val lastNotes = posMap[shortPos.last()]!!
                                        val firstAnchor = if (stemDir > 0) firstNotes.maxByOrNull { noteCoords[it]?.second ?: 0f }!! else firstNotes.minByOrNull { noteCoords[it]?.second ?: 0f }!!
                                        val lastAnchor = if (stemDir > 0) lastNotes.maxByOrNull { noteCoords[it]?.second ?: 0f }!! else lastNotes.minByOrNull { noteCoords[it]?.second ?: 0f }!!

                                        val (fx, fy) = noteCoords[firstAnchor] ?: continue
                                        val (lx, ly) = noteCoords[lastAnchor] ?: continue
                                        val bx1 = fx + stemOffX
                                        val bx2 = lx + stemOffX
                                        val rawSlope = (ly - fy) / (bx2 - bx1).coerceAtLeast(1f)
                                        val slope = (rawSlope * 0.25f).coerceIn(-0.10f, 0.10f)
                                        val by1 = fy + stemDir * stemLen
                                        val by2 = by1 + slope * (bx2 - bx1)

                                        beamPaint.color = textColor
                                        canvas.drawLine(bx1, by1, bx2, by2, beamPaint)

                                        for (p in shortPos) {
                                            val pNotes = posMap[p]!!
                                            val px = (noteCoords[pNotes.first()]?.first ?: 0f) + stemOffX
                                            val minY = pNotes.minOf { noteCoords[it]?.second ?: 0f }
                                            val maxY = pNotes.maxOf { noteCoords[it]?.second ?: 0f }
                                            val startY = if (stemDir > 0) minY else maxY
                                            val beamY = by1 + slope * (px - bx1)
                                            stemPaint.color = textColor
                                            canvas.drawLine(px, startY, px, beamY, stemPaint)
                                        }
                                    } else {
                                        for (p in positions) {
                                            val pNotes = posMap[p]!!
                                            val maxDur = pNotes.maxOf { it.duration }
                                            if (maxDur >= 4.0f) continue
                                            val avgMidi = pNotes.map { it.midiNote }.average()
                                            val isTreble = if (hasVoices) pNotes.first().voice == 0 else pNotes.first().midiNote >= 60
                                            val stemDir = if (isTreble) (if (avgMidi >= 71) 1f else -1f) else (if (avgMidi >= 50) 1f else -1f)
                                            val stemOffX = if (stemDir > 0) -lineSpacing * 0.52f else lineSpacing * 0.52f
                                            val stemLen = lineSpacing * 2.2f
                                            val px = (noteCoords[pNotes.first()]?.first ?: 0f) + stemOffX
                                            val minY = pNotes.minOf { noteCoords[it]?.second ?: 0f }
                                            val maxY = pNotes.maxOf { noteCoords[it]?.second ?: 0f }
                                            val startY = if (stemDir > 0) minY else maxY
                                            val tipY = (if (stemDir > 0) maxY else minY) + stemDir * stemLen
                                            stemPaint.color = textColor
                                            canvas.drawLine(px, startY, px, tipY, stemPaint)
                                        }
                                    }
                                }
                            }

                            if (drawTab) {
                                val byPos = visibleNotes.groupBy { it.position }
                                for ((pos, notesAtPos) in byPos) {
                                    val nx = posToX[pos] ?: (notesStartX + (pos / beatsPerM) * usableMWidth)
                                    val usedStrings = HashSet<Int>()
                                    for (note in notesAtPos.sortedByDescending { it.midiNote }) {
                                        val strIdx = if (note.string != null && note.string in 0..5 && note.string !in usedStrings) {
                                            note.string!!
                                        } else {
                                            (0..5).filter { it !in usedStrings }.minByOrNull { s ->
                                                val f = note.midiNote - GUITAR_OPEN_MIDI[s]
                                                if (f in 0..22) f else 1000 + abs(f)
                                            } ?: 0
                                        }
                                        usedStrings.add(strIdx)
                                        val fret = note.fret ?: (note.midiNote - GUITAR_OPEN_MIDI[strIdx]).coerceIn(0, 24)
                                        val ny = tabTop + strIdx * lineSpacing

                                        val fText = if (note.articulation == TabArticulation.DEAD_NOTE) "X" else fret.toString()
                                        val pw = maxOf(lineSpacing * 0.85f, (fText.length * 7f + 6f))
                                        val ph = lineSpacing * 0.82f
                                        canvas.drawRect(nx - pw * 0.5f, ny - ph * 0.5f, nx + pw * 0.5f, ny + ph * 0.5f, tabPillBgPaint)
                                        canvas.drawText(fText, nx, ny + 2.5f, tabFretTextPaint)
                                    }
                                }
                            }

                            if (options.includeLyrics) {
                                for (note in visibleNotes) {
                                    if (note.lyric.isNotBlank()) {
                                        val nx = posToX[note.position] ?: (notesStartX + (note.position / beatsPerM) * usableMWidth)
                                        val lyricY = sysBottom + 12f
                                        lyricPaint.color = textColor
                                        canvas.drawText(note.lyric, nx, lyricY, lyricPaint)
                                    }
                                }
                            }
                        }

                        currentMeasureIndex += measuresInThisSystem
                        pageSystemCount++
                        currentY += systemHeight + systemGap
                    }

                    canvas.drawText("- Page $pageNumber -", pageWidth / 2f, pageHeight - 16f, footerPaint)
                    document.finishPage(page)
                    pageNumber++
                    onProgress(0.05f + (0.9f * (currentMeasureIndex.toFloat() / totalMeasures)))
                }

                val cleanTitle = options.title.ifBlank { score.title.ifBlank { "Score" } }.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "pEQ_${cleanTitle}_${System.currentTimeMillis()}.pdf")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/pEQ_Scores")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)

                val outputFile = if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        document.writeTo(out)
                    }
                    null
                } else {
                    val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "pEQ_Scores")
                    if (!exportDir.exists()) exportDir.mkdirs()
                    val fallbackFile = File(exportDir, "pEQ_${cleanTitle}_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(fallbackFile).use { out ->
                        document.writeTo(out)
                    }
                    fallbackFile
                }
                document.close()

                onProgress(1.0f)
                onComplete(outputFile?.absolutePath ?: uri?.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }
}