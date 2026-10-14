package com.creativeblisslabs.peqlearn.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import com.creativeblisslabs.peqlearn.app.data.MediaType
import android.media.MediaMetadataRetriever
import android.os.Build

@Composable
actual fun pEQImage(
    data: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    alpha: Float,
    mediaType: MediaType?,
    fallbackPath: String?
) {
    if ((data == null || data.isEmpty()) && (fallbackPath == null || fallbackPath.isEmpty())) {
        DefaultPlaceholder(modifier, mediaType)
        return
    }

    val context = LocalContext.current
    var bitmap by remember(data, fallbackPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(data, fallbackPath) {
        withContext(Dispatchers.IO) {
            try {
                // Helper to load image bitmap from path/uri
                fun loadBitmap(source: String?): android.graphics.Bitmap? {
                    if (source == null || source.isEmpty()) return null
                    if (source.startsWith("content://")) {
                        val uri = Uri.parse(source)
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                // Note: We only return if it successfully decoded a bitmap (i.e. is actually an image stream)
                                val bmp = BitmapFactory.decodeStream(stream)
                                if (bmp != null) return bmp
                            }
                        } catch (_: Exception) {}
                        
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(context, uri)
                            val art = retriever.embeddedPicture
                            if (art != null) {
                                return BitmapFactory.decodeByteArray(art, 0, art.size)
                            }
                            val isVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null
                            if (isVideo) {
                                return retriever.getFrameAtTime(1000000)
                            }
                        } catch (_: Exception) {}

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                return context.contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)
                            } catch (_: Exception) {}
                        }
                    } else {
                        val cleaned = source.replace("file://", "")
                        val file = File(cleaned)
                        if (file.exists()) {
                            val extension = file.extension.lowercase()
                            if (extension in setOf("mp4", "mkv", "mov", "webm", "3gp", "avi")) {
                                val retriever = MediaMetadataRetriever()
                                try {
                                    retriever.setDataSource(file.absolutePath)
                                    return retriever.getFrameAtTime(1000000) // 1 second in
                                } catch (_: Exception) {
                                } finally {
                                    retriever.release()
                                }
                            } else if (extension in setOf("mp3", "flac", "wav", "m4a", "ogg", "wma", "aac")) {
                                val retriever = MediaMetadataRetriever()
                                try {
                                    retriever.setDataSource(file.absolutePath)
                                    val art = retriever.embeddedPicture
                                    if (art != null) {
                                        return BitmapFactory.decodeByteArray(art, 0, art.size)
                                    }
                                } catch (_: Exception) {
                                } finally {
                                    retriever.release()
                                }
                            } else {
                                return BitmapFactory.decodeFile(file.absolutePath)
                            }
                        }
                    }
                    return null
                }

                // 1. Try loading from data
                var bmp = loadBitmap(data)
                if (bmp != null) {
                    bitmap = bmp
                    return@withContext
                }

                // 2. Try loading from fallbackPath
                bmp = loadBitmap(fallbackPath)
                if (bmp != null) {
                    bitmap = bmp
                    return@withContext
                }

                // 3. If data is content:// and failed, try to scan the fallback file directly as a last resort
                if (data != null && data.startsWith("content://") && fallbackPath != null && !fallbackPath.startsWith("content://")) {
                    bmp = loadBitmap(fallbackPath)
                    if (bmp != null) {
                        bitmap = bmp
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                println("pEQLearn: [IMAGE] Load Failed: ${e.message}")
            }
        }
    }
    
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha
        )
    } ?: run {
        DefaultPlaceholder(modifier, mediaType)
    }
}

@Composable
private fun DefaultPlaceholder(modifier: Modifier, mediaType: MediaType?) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (mediaType == MediaType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    }
}
