package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.Tab
import com.creativeblisslabs.peqlearn.app.data.MediaType
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Close
/**
 * Global persistent widgets for the pEQ Workstation.
 */

@Composable
fun VolumeControlSlider(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val volume = viewModel.masterVolume.value

    val volumeIcon = when {
        volume > 0.5f -> Icons.AutoMirrored.Filled.VolumeUp
        volume > 0f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeMute
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.wrapContentWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Slider(
                value = volume,
                onValueChange = { viewModel.masterVolume.value = it },
                modifier = Modifier.width(140.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                )
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "${(volume * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun NowPlayingTelemetryCard(viewModel: AppViewModel, primaryColor: Color = MaterialTheme.colorScheme.primary) {
    val currentItem by viewModel.currentMediaItem
    val peakLevel by viewModel.peakLevelDb
    
    pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = primaryColor.copy(alpha = 0.1f)
            ) {
                if (currentItem?.thumbnail != null || currentItem?.path != null) {
                    pEQImage(
                        data = currentItem?.thumbnail,
                        fallbackPath = currentItem?.path,
                        modifier = Modifier.fillMaxSize(),
                        mediaType = currentItem?.type
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = primaryColor
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(currentItem?.title ?: "SYSTEM IDLE", fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(currentItem?.subtitle ?: "No signal detected", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                Text("${peakLevel.toInt()} dB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (peakLevel > -3f) Color.Red else primaryColor)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(0.2f), CircleShape)) {
                    val progress = ((peakLevel + 60f) / 60f).coerceIn(0f, 1f)
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (peakLevel > -3f) Color.Red else primaryColor, CircleShape))
                }
            }
        }
    }
}

@Composable
fun LiveSpectrumVisualizer(viewModel: AppViewModel, height: androidx.compose.ui.unit.Dp = 60.dp, primaryColor: Color = MaterialTheme.colorScheme.primary) {
    val rtaData by viewModel.rtaData
    
    Box(modifier = Modifier.fillMaxWidth().height(height).background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            rtaData.forEach { level ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(level.coerceIn(0.05f, 1f))
                        .background(primaryColor.copy(alpha = 0.6f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }
    }
}


// ============================================================================
// 100% RECOVERED ORIGINAL M3 DROPDOWN MENU & GLOBAL MINI PLAYER DOCK
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3DropdownMenu(
    label: String,
    selectedText: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    itemIcons: List<androidx.compose.ui.graphics.vector.ImageVector>? = null,
    items: List<String>,
    onItemSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item, fontSize = 12.sp) },
                    leadingIcon = itemIcons?.getOrNull(index)?.let {
                        { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    },
                    onClick = {
                        onItemSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ThinMiniPlayer(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val nowPlaying = viewModel.currentMediaItem.value ?: return
    val isPlaying by viewModel.isPlaying
    val isLandscape = false
    var isExpanded by remember { mutableStateOf(true) }

    val currentPos = viewModel.currentPosition.value
    val duration = viewModel.currentDuration.value.coerceAtLeast(1L)
    val progress = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val isFavorite = viewModel.favoriteItems.any { it.id == nowPlaying.id }
    val speed = viewModel.playbackSpeed.value
    val abActive = viewModel.abRepeatActive.value

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 2.dp),
        color = Color(0xFF141724).copy(alpha = 0.95f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. TOP EDGE SEEKBAR (Ultra-thin interactive / touch slider flush with the top edge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                // Active progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isDraggingSlider) dragProgress else progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )

                // Invisible slider on top edge for direct scrub touch interaction
                Slider(
                    value = if (isDraggingSlider) dragProgress else progress,
                    onValueChange = {
                        isDraggingSlider = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        val seekTargetMs = (dragProgress * duration).toLong()
                        viewModel.seekTo(seekTargetMs)
                        isDraggingSlider = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .alpha(0.01f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            // 2. UNIFIED SINGLE ROW: Thumbnail, Title, Prev, Play/Pause, Next, A-B, Speed, Favorite, Playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.currentTab.value = Tab.Player
                        if (nowPlaying.type == MediaType.VIDEO) {
                            viewModel.isVideoFullScreen.value = true
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail / Icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                ) {
                    pEQImage(
                        data = nowPlaying.thumbnail,
                        fallbackPath = nowPlaying.path,
                        modifier = Modifier.fillMaxSize().alpha(0.9f),
                        contentScale = ContentScale.Crop,
                        mediaType = nowPlaying.type
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Title & Subtitle (flexibly weighted)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = nowPlaying.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nowPlaying.subtitle.ifBlank { "pEQ Lossless Audio" },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // All Action & Transport Buttons in Single Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Previous Track
                    IconButton(
                        onClick = { viewModel.previousTrack() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Play / Pause
                    FilledIconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Next Track
                    IconButton(
                        onClick = { viewModel.nextTrack() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // A-B Repeat Button
                    IconButton(
                        onClick = { viewModel.toggleAbRepeat() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "A-B Repeat",
                            tint = if (abActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Speed Button (clickable text badge)
                    Surface(
                        onClick = { viewModel.cyclePlaybackSpeed() },
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Favorite Button
                    IconButton(
                        onClick = { viewModel.toggleFavorite(nowPlaying) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF5252) else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Add to Playlist Button
                    IconButton(
                        onClick = {
                            viewModel.selectedItemForPlaylist.value = nowPlaying
                            viewModel.isPlaylistBulkAction.value = false
                            viewModel.isPlaylistPickerShown.value = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
