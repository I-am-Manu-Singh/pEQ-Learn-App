package com.creativeblisslabs.peqlearn.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.data.MediaType
import com.creativeblisslabs.peqlearn.app.data.pEQMediaItem
import com.creativeblisslabs.peqlearn.app.ui.components.pEQImage
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

@Composable
fun PiPPlayerView(viewModel: AppViewModel, item: pEQMediaItem) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (item.type == MediaType.VIDEO) {
            // Simplified video view
            com.creativeblisslabs.peqlearn.app.ui.components.VideoSurface(modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else {
            // Lyric/Artwork view for audio
            AudioPiPContent(viewModel, item)
        }

        // Mini Controls Overlay (visible on hover or always)
        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.togglePlayback() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (viewModel.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White
                    )
                }
                IconButton(onClick = { viewModel.isPiPMode.value = false }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.OpenInFull, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AudioPiPContent(viewModel: AppViewModel, item: pEQMediaItem) {
    val currentLyric = viewModel.syncedLyrics.getOrNull(viewModel.currentLyricsLine.value)
    
    Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        pEQImage(
            data = item.thumbnail,
            fallbackPath = item.path,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
            mediaType = item.type
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.artist ?: "Unknown Artist",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
            
            Spacer(Modifier.height(4.dp))
            
            if (currentLyric != null) {
                Text(
                    currentLyric.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


