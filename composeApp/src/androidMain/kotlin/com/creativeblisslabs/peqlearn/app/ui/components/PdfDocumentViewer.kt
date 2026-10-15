package com.creativeblisslabs.peqlearn.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun PdfDocumentViewer(
    uriString: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    var pages by remember(uriString) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember(uriString) { mutableStateOf(true) }
    var errorMessage by remember(uriString) { mutableStateOf<String?>(null) }

    LaunchedEffect(uriString) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val fileDescriptor: ParcelFileDescriptor? = if (uriString.startsWith("content://")) {
                    context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")
                } else {
                    val file = File(uriString)
                    if (file.exists()) {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    } else {
                        // Try copying stream from content or assets
                        val tempFile = File(context.cacheDir, "temp_sheet_${System.currentTimeMillis()}.pdf")
                        val input = context.contentResolver.openInputStream(Uri.parse(uriString))
                            ?: context.assets.open(uriString.removePrefix("file:///android_asset/"))
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                        ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                }

                if (fileDescriptor != null) {
                    val renderer = PdfRenderer(fileDescriptor)
                    val renderedBitmaps = mutableListOf<Bitmap>()
                    val pageCount = renderer.pageCount.coerceAtMost(30) // Cap to avoid OOM on giant PDFs

                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        // Scale to 2x display resolution for crisp high-dpi music engraving
                        val densityScale = 2
                        val width = page.width * densityScale
                        val height = page.height * densityScale
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // Paint white background
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        renderedBitmaps.add(bitmap)
                        page.close()
                    }
                    renderer.close()
                    fileDescriptor.close()

                    withContext(Dispatchers.Main) {
                        pages = renderedBitmaps
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Could not open PDF file"
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Failed to render PDF document"
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Rendering PDF Sheet Music...", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Error displaying PDF",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
            pages.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(pages) { index, bmp ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Page ${index + 1}",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
        }
    }
}
