package com.creativeblisslabs.peqlearn.app.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.creativeblisslabs.peqlearn.app.ui.components.MeasureLayoutCache
import com.creativeblisslabs.peqlearn.app.ui.components.renderScoreSystemToDrawScope
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
        isLeftHandHidden: Boolean = false,
        isRightHandHidden: Boolean = false,
        isSplitStaff: Boolean = true,
        enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
        transposition: Int = 0,
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
                val printScale = 2.0f
                val pageWidth = ((if (isLandscape) pageSizeEnum.heightPt else pageSizeEnum.widthPt) * printScale).toInt()
                val pageHeight = ((if (isLandscape) pageSizeEnum.widthPt else pageSizeEnum.heightPt) * printScale).toInt()

                val document = PdfDocument()
                val marginX = 54f
                val marginY = 52f

                val measures = score.measures
                val totalMeasures = measures.size.coerceAtLeast(1)
                val requestedMeasuresPerSystem = options.measuresPerSystem.coerceIn(2, 5)

                val normFormat = options.staffFormat.trim().lowercase()
                val showStaff = normFormat != "tab" && normFormat != "tab only"
                val showTab = normFormat == "tab" || normFormat == "tab only" || normFormat == "both" || normFormat == "both viewers"
                val effectiveLeftHandHidden = isLeftHandHidden || !isSplitStaff

                val isInverted = options.colorTheme.equals("Inverted Dark", ignoreCase = true)
                val isMono = options.colorTheme.equals("Monochrome", ignoreCase = true)

                val pageBgInt = if (isInverted) android.graphics.Color.parseColor("#0D0B14") else android.graphics.Color.WHITE
                val headerTextInt = if (isInverted) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#111111")
                val subHeaderTextInt = if (isInverted) android.graphics.Color.parseColor("#B5FFFFFF") else android.graphics.Color.parseColor("#444444")

                val pageBgComposeColor = if (isInverted) Color(0xFF0D0B14) else Color.White
                val inkComposeColor = if (isInverted) Color(0xFFF2F2F5) else Color(0xFF111111)
                val staffLineComposeColor = when {
                    isInverted -> Color(0xFF626472)
                    isMono -> Color(0xFF333333)
                    else -> Color(0xFF6E6E75)
                }
                val primaryComposeColor = when {
                    isMono -> Color(0xFF111111)
                    isInverted -> Color(0xFFFFD700)
                    else -> Color(0xFF1565C0)
                }
                val rhComposeColor = when {
                    isMono -> Color(0xFF111111)
                    isInverted -> Color(0xFF4FC3F7)
                    else -> Color(0xFF1565C0)
                }
                val lhComposeColor = when {
                    isMono -> Color(0xFF111111)
                    isInverted -> Color(0xFFCE93D8)
                    else -> Color(0xFF5E35B1)
                }
                val tabPillBgComposeColor = pageBgComposeColor

                val densityScale = when {
                    requestedMeasuresPerSystem >= 5 -> 0.96f
                    requestedMeasuresPerSystem == 4 -> 1.04f
                    requestedMeasuresPerSystem == 3 -> 1.12f
                    else -> 1.20f
                } * (if (isLandscape) 1.10f else 1.0f)

                val pdfDensity = Density(density = densityScale, fontScale = 1.0f)
                val fontFamilyResolver = createFontFamilyResolver(context)
                val textMeasurer = TextMeasurer(
                    defaultFontFamilyResolver = fontFamilyResolver,
                    defaultDensity = pdfDensity,
                    defaultLayoutDirection = LayoutDirection.Ltr
                )
                val layoutCache = MeasureLayoutCache()
                val drawScope = CanvasDrawScope()

                // Generous Inter-Staff Vertical Geometry (8.5 spaces Treble->Bass, 9.5 spaces Bass->TAB)
                val staffLineSpacing = 11.2f * densityScale
                val tabLineSpacing = 12.0f * densityScale
                val trebleRelTop = 36f * densityScale
                val bassRelTop = if (effectiveLeftHandHidden) {
                    trebleRelTop
                } else {
                    trebleRelTop + (8.5f * staffLineSpacing)
                }
                val tabRelTop = if (showStaff) {
                    (if (effectiveLeftHandHidden) trebleRelTop else bassRelTop) + (9.5f * staffLineSpacing)
                } else {
                    34f * densityScale
                }

                val systemHeight = when {
                    showStaff && showTab -> tabRelTop + (5.0f * tabLineSpacing) + (40f * densityScale)
                    showStaff -> (if (effectiveLeftHandHidden) trebleRelTop else bassRelTop) + (4.0f * staffLineSpacing) + (44f * densityScale)
                    else -> tabRelTop + (5.0f * tabLineSpacing) + (44f * densityScale)
                }
                val systemGap = 34f * densityScale

                val bgPaint = Paint().apply { color = pageBgInt; style = Paint.Style.FILL }
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = headerTextInt
                    textSize = 36f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val composerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = subHeaderTextInt
                    textSize = 19f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    textAlign = Paint.Align.RIGHT
                }
                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = subHeaderTextInt
                    textSize = 18f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = subHeaderTextInt
                    textSize = 16f
                    textAlign = Paint.Align.CENTER
                }
                val headerRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isInverted) android.graphics.Color.parseColor("#33FFFFFF") else android.graphics.Color.parseColor("#26000000")
                    strokeWidth = 1.5f
                }

                val titleBlockHeight = if (options.includeTitleHeader) 102f else 0f
                val footerReserve = 42f

                // Adaptive System Chunking: dense 12-beat triplet measures (like M.24, M.111, M.112)
                // automatically limit row count so chords never overlap horizontally
                val systems = mutableListOf<List<Pair<Int, com.creativeblisslabs.peqlearn.app.ui.screens.Measure>>>()
                var mCursor = 0
                while (mCursor < totalMeasures) {
                    val currentRow = mutableListOf<Pair<Int, com.creativeblisslabs.peqlearn.app.ui.screens.Measure>>()
                    var rowNoteSlots = 0
                    val maxSlotsPerRow = if (isLandscape) 34 else 22

                    while (mCursor < totalMeasures && currentRow.size < requestedMeasuresPerSystem) {
                        val m = measures[mCursor]
                        val distinctPos = m.notes.filter { !it.isRest }.map { it.position }.distinct().size.coerceAtLeast(3)
                        if (currentRow.isNotEmpty() && rowNoteSlots + distinctPos > maxSlotsPerRow) {
                            break
                        }
                        currentRow.add(mCursor to m)
                        rowNoteSlots += distinctPos
                        mCursor++
                    }
                    systems.add(currentRow)
                }

                val totalSystems = systems.size.coerceAtLeast(1)
                var systemCursor = 0
                var pageNumber = 1

                while (systemCursor < totalSystems) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = document.startPage(pageInfo)
                    val pdfCanvas: Canvas = page.canvas

                    pdfCanvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                    var currentTopY = marginY
                    if (pageNumber == 1 && options.includeTitleHeader) {
                        val titleText = options.title.ifBlank { score.title.ifBlank { "Untitled Score" } }
                        pdfCanvas.drawText(titleText, pageWidth / 2f, currentTopY + 32f, titlePaint)

                        val composerText = options.composer.ifBlank {
                            buildString {
                                if (score.composer.isNotBlank()) append(score.composer)
                                if (score.lyricist.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(score.lyricist)
                                }
                            }.ifBlank { "pEQ Learn Studio" }
                        }
                        pdfCanvas.drawText(composerText, pageWidth - marginX, currentTopY + 66f, composerPaint)

                        val tempoMeta = buildString {
                            append("𝅘𝅥 = ${score.tempo.toInt()} BPM")
                            if (score.tempoText.isNotBlank()) append(" (${score.tempoText})")
                            append("   •   ${score.timeSignature}")
                            append("   •   ${score.keySignature.keyName}")
                            if (score.capo > 0) append("   •   Capo ${score.capo}")
                        }
                        pdfCanvas.drawText(tempoMeta, marginX, currentTopY + 66f, metaPaint)
                        pdfCanvas.drawLine(marginX, currentTopY + 82f, pageWidth - marginX, currentTopY + 82f, headerRulePaint)
                        currentTopY += titleBlockHeight
                    }

                    val maxBottomY = pageHeight - marginY - footerReserve
                    var systemsDrawnOnThisPage = 0

                    while (systemCursor < totalSystems && (currentTopY + systemHeight <= maxBottomY || systemsDrawnOnThisPage == 0)) {
                        val systemMeasures = systems[systemCursor]

                        pdfCanvas.save()
                        pdfCanvas.translate(0f, currentTopY)

                        val composeCanvas = androidx.compose.ui.graphics.Canvas(pdfCanvas)
                        drawScope.draw(
                            density = pdfDensity,
                            layoutDirection = LayoutDirection.Ltr,
                            canvas = composeCanvas,
                            size = Size(pageWidth.toFloat(), systemHeight)
                        ) {
                            renderScoreSystemToDrawScope(
                                drawScope = this,
                                score = score,
                                systemMeasures = systemMeasures,
                                startX = marginX,
                                endX = pageWidth - marginX,
                                trebleBaseline = trebleRelTop,
                                bassBaseline = bassRelTop,
                                tabBaseline = tabRelTop,
                                staffLineSpacing = staffLineSpacing,
                                tabLineSpacing = tabLineSpacing,
                                showStaff = showStaff,
                                showTab = showTab,
                                showMeasureNumbers = options.includeMeasureNumbers,
                                showNoteLetters = options.includeChordLabels,
                                showLyrics = options.includeLyrics,
                                isSplitStaff = isSplitStaff,
                                isDarkTheme = isInverted,
                                pageBackgroundColor = pageBgComposeColor,
                                primaryColor = primaryComposeColor,
                                rightHandColor = rhComposeColor,
                                leftHandColor = lhComposeColor,
                                inkColor = inkComposeColor,
                                staffLineColor = staffLineComposeColor,
                                tabPillBgColor = tabPillBgComposeColor,
                                isLeftHandHidden = isLeftHandHidden,
                                isRightHandHidden = isRightHandHidden,
                                enabledTrackIndices = enabledTrackIndices,
                                transposition = transposition,
                                textMeasurer = textMeasurer,
                                layoutCache = layoutCache
                            )
                        }

                        pdfCanvas.restore()

                        currentTopY += systemHeight + systemGap
                        systemCursor++
                        systemsDrawnOnThisPage++
                    }

                    val footerTitle = options.title.ifBlank { score.title.ifBlank { "pEQ Learn Score" } }
                    pdfCanvas.drawText(
                        "$footerTitle  —  Page $pageNumber",
                        pageWidth / 2f,
                        pageHeight - 26f,
                        footerPaint
                    )

                    document.finishPage(page)
                    pageNumber++

                    val prog = 0.05f + 0.90f * (systemCursor.toFloat() / totalSystems)
                    withContext(Dispatchers.Main) {
                        onProgress(prog.coerceIn(0.05f, 0.95f))
                    }
                }

                val cleanTitle = options.title.ifBlank { score.title.ifBlank { "Score" } }
                    .replace(" ", "_")
                    .replace("[^a-zA-Z0-9_]".toRegex(), "")
                val pdfFileName = "pEQ_${cleanTitle}_${System.currentTimeMillis()}.pdf"

                val cachePdfFile = File(context.cacheDir, pdfFileName)
                FileOutputStream(cachePdfFile).use { out ->
                    document.writeTo(out)
                }
                document.close()

                var finalSavedPath = cachePdfFile.absolutePath
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/pEQ_Scores")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outStream ->
                            cachePdfFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val doneValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.IS_PENDING, 0)
                            }
                            resolver.update(uri, doneValues, null, null)
                        }
                        finalSavedPath = "/Documents/pEQ_Scores/$pdfFileName"
                    } else {
                        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "pEQ_Scores")
                        if (!exportDir.exists()) exportDir.mkdirs()
                        val fallbackFile = File(exportDir, pdfFileName)
                        cachePdfFile.copyTo(fallbackFile, overwrite = true)
                        finalSavedPath = fallbackFile.absolutePath
                    }
                }

                withContext(Dispatchers.Main) {
                    onProgress(1.0f)
                    onComplete(finalSavedPath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }
}