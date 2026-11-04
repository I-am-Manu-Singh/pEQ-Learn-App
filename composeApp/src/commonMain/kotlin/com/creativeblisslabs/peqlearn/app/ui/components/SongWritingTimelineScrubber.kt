package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ai.AllInOneAnalysisResult
import com.creativeblisslabs.peqlearn.app.ai.MusicSegment
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

@Composable
fun SongWritingTimelineScrubber(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val analysis = viewModel.songStructureAnalysis.value
    val currentPosMs = viewModel.currentPosition.longValue
    val totalDurationMs = viewModel.currentDuration.value
    val isPlaying = viewModel.isPlaying.value || viewModel.isStemsPlaying.value
    val currentTrack = viewModel.selectedMixerItem.value ?: viewModel.currentMediaItem.value

    val currentSeconds = (currentPosMs / 1000f).coerceAtLeast(0f)
    val maxSeconds = if (totalDurationMs > 0) (totalDurationMs / 1000f) else (analysis?.durationSeconds ?: 180f).coerceAtLeast(1f)

    val activeSegment = analysis?.getSegmentAt(currentSeconds)
    val activeChord = analysis?.getChordAt(currentSeconds)
    val harmonic = analysis?.harmonicAnalysis

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Track title, Key, Scale, and BPM badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SONGWRITING TIMELINE & STRUCTURE SCRUBBER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentTrack?.displayName ?: "No Track Selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (harmonic != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "KEY: ${harmonic.scaleName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${(analysis?.bpm ?: 120f).toInt()} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Real-Time Harmonic Insight HUD: Active Section, Active Chord, Roman Numeral
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Section
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val secColor = activeSegment?.let { AllInOneColorPalette.getColorForLabel(it.label) } ?: MaterialTheme.colorScheme.primary
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(secColor))
                    Text(
                        text = activeSegment?.label?.uppercase() ?: "INTRO / START",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Current Chord & Roman Numeral
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activeChord != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CHORD:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeChord.chordName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (activeChord.romanNumeral.isNotEmpty()) {
                                Text(
                                    text = "(${activeChord.romanNumeral})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (harmonic != null) {
                        Text(
                            text = harmonic.primaryProgression,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Segment Blocks Timeline (Interactive jump buttons)
            val segments = analysis?.segments?.ifEmpty { null } ?: listOf(
                MusicSegment(0f, 15f, "intro"),
                MusicSegment(15f, 45f, "verse 1"),
                MusicSegment(45f, 75f, "chorus 1"),
                MusicSegment(75f, 105f, "verse 2"),
                MusicSegment(105f, 135f, "chorus 2"),
                MusicSegment(135f, 155f, "bridge"),
                MusicSegment(155f, 180f, "outro")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                segments.forEach { segment ->
                    val segDuration = (segment.end - segment.start).coerceAtLeast(0.1f)
                    val weight = (segDuration / maxSeconds).coerceIn(0.015f, 1f)
                    val isCurrent = currentSeconds >= segment.start && currentSeconds < segment.end
                    val segColor = AllInOneColorPalette.getColorForLabel(segment.label)

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(if (isCurrent) segColor else segColor.copy(alpha = 0.6f))
                            .border(
                                width = if (isCurrent) 2.5.dp else 0.5.dp,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            )
                            .clickable {
                                viewModel.seekTo((segment.start * 1000L).toLong())
                            }
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = segment.label.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            // Continuous Draggable Scrubber Bar with Playhead
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(6.dp))
                    .pointerInput(maxSeconds) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val targetMs = (fraction * maxSeconds * 1000L).toLong()
                            viewModel.seekTo(targetMs)
                        }
                    }
                    .pointerInput(maxSeconds) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val targetMs = (fraction * maxSeconds * 1000L).toLong()
                            viewModel.seekTo(targetMs)
                        }
                    }
            ) {
                val totalWidthPx = constraints.maxWidth.toFloat()
                val progressFraction = (currentSeconds / maxSeconds).coerceIn(0f, 1f)

                // Track Progress fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressFraction)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                )

                // Downbeat ticks overlay (if present from All-In-One analysis)
                analysis?.downbeats?.forEach { downbeatSec ->
                    val frac = (downbeatSec / maxSeconds).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .offset(x = (totalWidthPx * frac).dp / 2.75f) // approximate tick display
                            .width(1.dp)
                            .height(8.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }

                // Playhead needle
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressFraction)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                // Time counters
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatPlayerTime(currentPosMs),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Drag to Scrub Timeline",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Text(
                        text = formatPlayerTime((maxSeconds * 1000L).toLong()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Player & Segment Navigation Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segment Navigation (Previous Section)
                FilledTonalButton(
                    onClick = {
                        val prev = segments.lastOrNull { it.start < (currentSeconds - 1.0f) } ?: segments.firstOrNull()
                        if (prev != null) viewModel.seekTo((prev.start * 1000L).toLong())
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Section", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prev Section", fontSize = 11.sp)
                }

                // Transport Play / Pause / Rewind
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val newMs = (currentPosMs - 5000L).coerceAtLeast(0L)
                            viewModel.seekTo(newMs)
                        }
                    ) {
                        Icon(Icons.Default.Replay5, contentDescription = "-5s", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            if (viewModel.activeStemMap.isNotEmpty()) {
                                viewModel.toggleStemsPlayback()
                            } else {
                                viewModel.togglePlayback()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newMs = (currentPosMs + 5000L).coerceAtMost((maxSeconds * 1000L).toLong())
                            viewModel.seekTo(newMs)
                        }
                    ) {
                        Icon(Icons.Default.Forward5, contentDescription = "+5s", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Segment Navigation (Next Section)
                FilledTonalButton(
                    onClick = {
                        val next = segments.firstOrNull { it.start > (currentSeconds + 0.5f) }
                        if (next != null) viewModel.seekTo((next.start * 1000L).toLong())
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Next Section", fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Section", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun formatPlayerTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}
