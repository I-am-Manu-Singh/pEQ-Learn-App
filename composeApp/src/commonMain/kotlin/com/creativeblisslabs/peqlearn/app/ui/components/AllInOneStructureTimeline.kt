package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ai.AllInOneAnalysisResult
import com.creativeblisslabs.peqlearn.app.ai.MusicSegment
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

object AllInOneColorPalette {
    fun getColorForLabel(label: String): Color {
        val lower = label.lowercase().trim()
        return when {
            lower.contains("intro") || lower == "start" -> Color(0xFF42A5F5) // Blue
            lower.contains("verse") -> Color(0xFF66BB6A) // Green
            lower.contains("chorus") || lower.contains("hook") -> Color(0xFFFF7043) // Coral / Orange-Red
            lower.contains("bridge") -> Color(0xFFAB47BC) // Purple
            lower.contains("solo") || lower.contains("inst") -> Color(0xFFFFCA28) // Amber / Gold
            lower.contains("pre") -> Color(0xFF26A69A) // Teal
            lower.contains("outro") || lower.contains("end") -> Color(0xFF78909C) // Slate / Grey
            else -> Color(0xFF8D6E63) // Warm earth
        }
    }
}

@Composable
fun AllInOneStructureTimeline(
    analysis: AllInOneAnalysisResult?,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (analysis == null || analysis.segments.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "All-In-One Structure Analyzer: Import a song & tap 'Split Stems' or 'Analyze Structure' to generate structural timeline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val currentSeconds = (currentPositionMs / 1000f).coerceAtLeast(0f)
    val maxDuration = if (totalDurationMs > 0) (totalDurationMs / 1000f) else analysis.durationSeconds.coerceAtLeast(1f)
    val activeSegment = analysis.getSegmentAt(currentSeconds)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header info bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ALL-IN-ONE MUSIC STRUCTURE (MIR-AIDJ)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${analysis.bpm.toInt()} BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (activeSegment != null) {
                    val segColor = AllInOneColorPalette.getColorForLabel(activeSegment.label)
                    Surface(
                        color = segColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, segColor),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "CURRENT: ${activeSegment.label.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Segment block timeline
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Interactive blocks row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    analysis.segments.forEach { segment ->
                        val duration = (segment.end - segment.start).coerceAtLeast(0.1f)
                        val weight = (duration / maxDuration).coerceIn(0.02f, 1f)
                        val isCurrent = currentSeconds >= segment.start && currentSeconds < segment.end
                        val color = AllInOneColorPalette.getColorForLabel(segment.label)

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(if (isCurrent) color else color.copy(alpha = 0.65f))
                                .border(
                                    width = if (isCurrent) 2.dp else 0.5.dp,
                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                )
                                .clickable {
                                    onSeekTo((segment.start * 1000L).toLong())
                                }
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = segment.label.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }

                // Interactive Mini Scrub bar with Beat ticks
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(4.dp))
                ) {
                    val progressFraction = (currentSeconds / maxDuration).coerceIn(0f, 1f)

                    // Draw progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressFraction)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    )

                    // Playhead indicator
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressFraction)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }

                    // Downbeat markers count indicator
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimelineTime(currentPositionMs),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${analysis.downbeats.size} Bars • ${analysis.beats.size} Beats",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatTimelineTime((maxDuration * 1000).toLong()),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimelineTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}
