package com.creativeblisslabs.peqlearn.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.PdfPageSize
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AndroidPdfExporter {

    suspend fun exportScoreToPdf(
        context: Context,
        score: Score,
        options: PdfExportOptions,
        onProgress: (Float) -> Unit,
        onComplete: (String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                onProgress(0.1f)
                val pageSizeEnum = try {
                    PdfPageSize.valueOf(options.pageSize.uppercase())
                } catch (e: Exception) {
                    PdfPageSize.A4
                }

                val isLandscape = options.orientation.equals("Landscape", ignoreCase = true)
                val pageWidth = if (isLandscape) pageSizeEnum.heightPt else pageSizeEnum.widthPt
                val pageHeight = if (isLandscape) pageSizeEnum.widthPt else pageSizeEnum.heightPt

                val document = PdfDocument()

                // Layout calculations
                val margin = 36f // 0.5 inch margins
                val usableWidth = pageWidth - (margin * 2)

                val measures = score.measures
                val totalMeasures = measures.size.coerceAtLeast(1)
                val measuresPerSystem = options.measuresPerSystem.coerceIn(2, 8)
                val totalSystems = (totalMeasures + measuresPerSystem - 1) / measuresPerSystem

                val systemHeight = when (options.staffFormat) {
                    "Staff" -> 90f
                    "Tab" -> 75f
                    else -> 155f // Both
                }
                val systemGap = 28f

                val titleBlockHeight = if (options.includeTitleHeader) 70f else 0f
                val availableHeightFirstPage = pageHeight - (margin * 2) - titleBlockHeight
                val systemsPerPage = ((pageHeight - (margin * 2)) / (systemHeight + systemGap)).toInt().coerceAtLeast(2)
                val systemsFirstPage = (availableHeightFirstPage / (systemHeight + systemGap)).toInt().coerceAtLeast(1)

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                val subtitlePaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                val composerPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10f
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                }

                val staffLinePaint = Paint().apply {
                    color = if (options.colorTheme == "Monochrome") Color.BLACK else Color.rgb(60, 60, 60)
                    strokeWidth = 1f
                    isAntiAlias = true
                }

                val barlinePaint = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 1.4f
                    isAntiAlias = true
                }

                val noteHeadPaint = Paint().apply {
                    color = when (options.colorTheme) {
                        "Color (LH/RH)" -> Color.rgb(74, 144, 226) // Right hand primary
                        else -> Color.BLACK
                    }
                    isAntiAlias = true
                }

                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 8f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                var currentMeasureIndex = 0
                var pageNumber = 1
                var systemIndex = 0

                while (currentMeasureIndex < totalMeasures) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    val isFirstPage = (pageNumber == 1)
                    var currentY = margin

                    // Header on first page
                    if (isFirstPage && options.includeTitleHeader) {
                        val titleText = options.title.ifBlank { score.title.ifBlank { "Score Sheet" } }
                        canvas.drawText(titleText, pageWidth / 2f, currentY + 20f, titlePaint)
                        val composerText = options.composer.ifBlank { score.composer.ifBlank { "pEQ Learn" } }
                        canvas.drawText(composerText, pageWidth - margin, currentY + 36f, composerPaint)
                        canvas.drawText("Tempo: ${score.bpm} BPM | Time: ${score.timeSignature}", pageWidth / 2f, currentY + 40f, subtitlePaint)
                        currentY += titleBlockHeight
                    }

                    val maxSystemsThisPage = if (isFirstPage) systemsFirstPage else systemsPerPage
                    var pageSystemCount = 0

                    while (pageSystemCount < maxSystemsThisPage && currentMeasureIndex < totalMeasures) {
                        val measuresInThisSystem = minOf(measuresPerSystem, totalMeasures - currentMeasureIndex)
                        val measureWidth = usableWidth / measuresInThisSystem

                        for (mIdx in 0 until measuresInThisSystem) {
                            val actualMeasureIdx = currentMeasureIndex + mIdx
                            val measure = measures.getOrNull(actualMeasureIdx)
                            val mLeft = margin + (mIdx * measureWidth)
                            val mRight = mLeft + measureWidth

                            val drawStaff = options.staffFormat == "Staff" || options.staffFormat == "Both"
                            val drawTab = options.staffFormat == "Tab" || options.staffFormat == "Both"

                            var staffY = currentY + 15f
                            val lineSpacing = 8f

                            // Draw Grand Staff (Treble & Bass)
                            if (drawStaff) {
                                // Treble 5 lines
                                for (l in 0 until 5) {
                                    val y = staffY + (l * lineSpacing)
                                    canvas.drawLine(mLeft, y, mRight, y, staffLinePaint)
                                }
                                // Bass 5 lines
                                val bassStaffY = staffY + (6 * lineSpacing)
                                for (l in 0 until 5) {
                                    val y = bassStaffY + (l * lineSpacing)
                                    canvas.drawLine(mLeft, y, mRight, y, staffLinePaint)
                                }

                                // Barlines
                                canvas.drawLine(mRight, staffY, mRight, staffY + (4 * lineSpacing), barlinePaint)
                                canvas.drawLine(mRight, bassStaffY, mRight, bassStaffY + (4 * lineSpacing), barlinePaint)
                                if (mIdx == 0) {
                                    canvas.drawLine(mLeft, staffY, mLeft, bassStaffY + (4 * lineSpacing), barlinePaint)
                                }

                                staffY = bassStaffY + (5 * lineSpacing) + 10f
                            }

                            // Draw Tab (6 guitar lines)
                            if (drawTab) {
                                val tabLineSpacing = 8.5f
                                for (l in 0 until 6) {
                                    val y = staffY + (l * tabLineSpacing)
                                    canvas.drawLine(mLeft, y, mRight, y, staffLinePaint)
                                }
                                canvas.drawLine(mRight, staffY, mRight, staffY + (5 * tabLineSpacing), barlinePaint)
                                if (mIdx == 0) {
                                    canvas.drawLine(mLeft, staffY, mLeft, staffY + (5 * tabLineSpacing), barlinePaint)
                                }
                            }

                            // Measure number
                            if (options.includeMeasureNumbers) {
                                val mNumPaint = Paint().apply {
                                    color = Color.GRAY
                                    textSize = 7f
                                    isAntiAlias = true
                                }
                                canvas.drawText("M.${actualMeasureIdx + 1}", mLeft + 2f, currentY + 10f, mNumPaint)
                            }

                            // Draw notes in measure with accurate pitch and tab rendering
                            if (measure != null) {
                                val beatsPerMeasure = measure.timeSignatureNumerator.toFloat().coerceAtLeast(1f)
                                val usableMWidth = measureWidth - 25f
                                val notesStartX = mLeft + 15f
                                val guitarOpenStringsMidi = intArrayOf(64, 59, 55, 50, 45, 40)

                                val sortedNotes = measure.notes.filter { !it.isRest }.sortedBy { it.position }
                                sortedNotes.forEach { note ->
                                    val noteX = notesStartX + (usableMWidth * (note.position / beatsPerMeasure).coerceIn(0f, 0.95f))
                                    val isTreble = note.midiNote >= 60

                                    if (drawStaff) {
                                        val trebleBaseline = currentY + 15f
                                        val bassBaseline = trebleBaseline + (6 * lineSpacing)
                                        val diatonicStep = com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine.getDiatonicStep(note.midiNote)
                                        val noteY = if (isTreble) {
                                            trebleBaseline + (38.0f - diatonicStep) * (lineSpacing / 2f)
                                        } else {
                                            bassBaseline + (26.0f - diatonicStep) * (lineSpacing / 2f)
                                        }

                                        val stemDir = if (isTreble) (if (note.midiNote >= 71) 1f else -1f) else (if (note.midiNote >= 50) 1f else -1f)
                                        val stemEndY = noteY + (stemDir * 16f)

                                        // Notehead & Stem
                                        canvas.drawCircle(noteX, noteY, 3.2f, noteHeadPaint)
                                        val stemX = if (stemDir > 0) noteX - 3.2f else noteX + 3.2f
                                        canvas.drawLine(stemX, noteY, stemX, stemEndY, staffLinePaint)

                                        if (options.includeChordLabels) {
                                            val labelText = when (note.midiNote % 12) {
                                                0 -> "C"; 1 -> "C#"; 2 -> "D"; 3 -> "D#"; 4 -> "E"; 5 -> "F"
                                                6 -> "F#"; 7 -> "G"; 8 -> "G#"; 9 -> "A"; 10 -> "A#"; else -> "B"
                                            }
                                            val chordPaint = Paint().apply {
                                                color = Color.BLACK
                                                textSize = 5.5f
                                                textAlign = Paint.Align.CENTER
                                                isAntiAlias = true
                                            }
                                            canvas.drawText(labelText, noteX, noteY - 5f, chordPaint)
                                        }
                                    }

                                    // Render Tab frets accurately on guitar strings 0..5
                                    if (drawTab) {
                                        val tabBaseline = if (drawStaff) currentY + 15f + (11 * lineSpacing) + 10f else currentY + 15f
                                        val tabLineSpacing = 8.5f
                                        val stringIdx = note.string?.coerceIn(0, 5) ?: guitarOpenStringsMidi.indices.minByOrNull { s ->
                                            val diff = note.midiNote - guitarOpenStringsMidi[s]
                                            if (diff in 0..22) diff else 1000 + kotlin.math.abs(diff)
                                        } ?: 0

                                        val fret = note.fret ?: (note.midiNote - guitarOpenStringsMidi[stringIdx]).coerceIn(0, 24)
                                        val tabY = tabBaseline + (stringIdx * tabLineSpacing)

                                        val bgPillPaint = Paint().apply {
                                            color = Color.WHITE
                                            style = Paint.Style.FILL
                                            isAntiAlias = true
                                        }
                                        val fretPaint = Paint().apply {
                                            color = Color.BLACK
                                            textSize = 6.5f
                                            textAlign = Paint.Align.CENTER
                                            isFakeBoldText = true
                                            isAntiAlias = true
                                        }
                                        // Draw white background pill so staff line behind fret text is masked
                                        canvas.drawRect(noteX - 4.5f, tabY - 4.5f, noteX + 4.5f, tabY + 4.5f, bgPillPaint)
                                        canvas.drawText(fret.toString(), noteX, tabY + 2.5f, fretPaint)
                                    }

                                    // Render Lyrics if present and enabled
                                    if (options.includeLyrics && note.lyric.isNotBlank()) {
                                        val lyricPaint = Paint().apply {
                                            color = Color.DKGRAY
                                            textSize = 6.5f
                                            textAlign = Paint.Align.CENTER
                                            isAntiAlias = true
                                        }
                                        val lyricY = currentY + systemHeight - 2f
                                        canvas.drawText(note.lyric, noteX, lyricY, lyricPaint)
                                    }
                                }
                            }
                        }

                        currentMeasureIndex += measuresInThisSystem
                        pageSystemCount++
                        systemIndex++
                        currentY += systemHeight + systemGap
                    }

                    // Page numbering footer
                    val footerPaint = Paint().apply {
                        color = Color.GRAY
                        textSize = 8f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("- Page $pageNumber -", pageWidth / 2f, pageHeight - 16f, footerPaint)

                    document.finishPage(page)
                    pageNumber++
                    onProgress(0.1f + (0.8f * (currentMeasureIndex.toFloat() / totalMeasures)))
                }

                val exportDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "pEQ_Scores"
                )
                if (!exportDir.exists()) exportDir.mkdirs()

                val cleanTitle = options.title.ifBlank { score.title.ifBlank { "Score" } }.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
                val outputFile = File(exportDir, "pEQ_${cleanTitle}_${System.currentTimeMillis()}.pdf")

                FileOutputStream(outputFile).use { out ->
                    document.writeTo(out)
                }
                document.close()

                onProgress(1.0f)
                onComplete(outputFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }
}
