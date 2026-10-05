package com.creativeblisslabs.peqlearn.app.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun FloatingLyricsOverlay(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val currentPosition by viewModel.currentPosition
    val lyrics = viewModel.syncedLyrics
    
    val currentLineIndex by remember(lyrics, currentPosition) {
        derivedStateOf {
            lyrics.indexOfLast { it.timestampMs <= currentPosition }.coerceAtLeast(0)
        }
    }
    
    val currentLyric = lyrics.getOrNull(currentLineIndex)
    
    Surface(
        modifier = modifier
            .padding(16.dp)
            .widthIn(max = 340.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.88f),
        border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), Color.Transparent))),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(18.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentLyric?.text?.takeIf { it.isNotBlank() } ?: "NEURAL pEQ STATION",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                val nextLyric = lyrics.getOrNull(currentLineIndex + 1)
                if (nextLyric != null) {
                    Text(
                        text = nextLyric.text,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))

            IconButton(
                onClick = { viewModel.isLyricsVisible.value = false },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
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
fun MiniPlayerDock(
    viewModel: com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel,
    modifier: Modifier = Modifier,
    isCompactSidebar: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF141724),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "pEQ Concert Player",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Lossless Audio Master • 96kHz",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = { viewModel.togglePlayback() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        if (viewModel.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Playback",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.nextTrack() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
