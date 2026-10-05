package com.creativeblisslabs.peqlearn.app.ui.widgets

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.data.pEQMediaItem
import com.creativeblisslabs.peqlearn.app.ui.components.pEQImage

@Composable
fun pEQWidgetCommand(
    item: pEQMediaItem?,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    duration: Long = 0L,
    currentPosition: Long = 0L,
    isFavorite: Boolean = false,
    isShuffleEnabled: Boolean = false,
    queue: List<pEQMediaItem> = emptyList(),
    onQueueItemClick: (pEQMediaItem) -> Unit = {},
    onFavoriteToggle: () -> Unit = {}
) {
    // Dynamic background color derived from current track metadata to match album art
    val dominantColor = remember(item) {
        if (item == null) {
            Color(0xFF2C0B3E) // Premium deep violet/plum fallback
        } else {
            val hash = (item.title + item.artist).hashCode()
            val r = ((hash and 0xFF0000) shr 16) % 30 + 35  // 35-65 (deep red/plum)
            val g = ((hash and 0x00FF00) shr 8) % 15 + 10    // 10-25 (dark green/blue)
            val b = (hash and 0x0000FF) % 35 + 45            // 45-80 (medium blue/violet)
            Color(r, g, b)
        }
    }

    val cyanAccent = Color(0xFF00E5FF)
    val mutedCyan = Color(0xFF80DEEA)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onLaunch()
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // =========================================================
        // CARD 1: PLAYER CARD
        // =========================================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = dominantColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Art on the left
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.3f)
                    ) {
                        pEQImage(
                            data = item?.thumbnail,
                            fallbackPath = item?.path,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            mediaType = item?.type
                        )
                    }

                    // Metadata Centered
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item?.title ?: "pEQ Station",
                            color = cyanAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item?.artist ?: "Ready for Neural Input",
                            color = mutedCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transport Control Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Toggle
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = cyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Fast Rewind
                    IconButton(
                        onClick = { /* simulated rewind / skip back */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind",
                            tint = cyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Play/Pause
                    IconButton(
                        onClick = onTogglePlayback,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = cyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Fast Forward
                    IconButton(
                        onClick = { /* simulated fast forward */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Fast Forward",
                            tint = cyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Skip Next
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Queue/Playlist icon
                    IconButton(
                        onClick = onLaunch,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = cyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // =========================================================
        // CARD 2: CURRENT QUEUE CARD
        // =========================================================
        if (queue.isNotEmpty()) {
            val activeIndex = remember(item, queue) {
                queue.indexOfFirst { it.id == item?.id }.coerceAtLeast(0)
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = dominantColor,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Current Queue (${activeIndex + 1}/${queue.size})",
                        color = mutedCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Show active item and a few surrounding items to keep it clean and fits the height
                        val startIndex = (activeIndex - 2).coerceAtLeast(0)
                        val itemsToShow = queue.drop(startIndex).take(5)

                        itemsToShow.forEachIndexed { indexInSlice, queueItem ->
                            val globalIndex = startIndex + indexInSlice
                            val isActive = queueItem.id == item?.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isActive) Color.White.copy(alpha = 0.06f) else Color.Transparent
                                    )
                                    .clickable { onQueueItemClick(queueItem) }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Index or Active Play Icon
                                Box(
                                    modifier = Modifier.width(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (isActive) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Playing",
                                            tint = cyanAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = (globalIndex + 1).toString(),
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Title and Artist
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = queueItem.title,
                                        color = if (isActive) cyanAccent else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = queueItem.artist ?: "Unknown Artist",
                                        color = if (isActive) mutedCyan else Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Duration
                                Text(
                                    text = formatDuration(queueItem.durationMs),
                                    color = if (isActive) cyanAccent else Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val m = minutes.toString()
    val s = seconds.toString().padStart(2, '0')
    return "$m:$s"
}