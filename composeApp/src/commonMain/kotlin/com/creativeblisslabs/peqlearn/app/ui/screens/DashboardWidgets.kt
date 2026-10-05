package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import com.creativeblisslabs.peqlearn.app.ui.theme.*
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ai.FormatInfo
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf


// Float formatting extension helper
private fun Float.formatSize(): String {
    val rounded = (this * 100).toInt() / 100f
    return rounded.toString()
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
        }
    }
}

@Composable
fun pEQTabs(
    tabs: List<String>,
    selectedTab: Int,
    viewModel: AppViewModel,
    onTabSelected: (Int) -> Unit
) {
    val mediaLibrary by viewModel.mediaLibrary.collectAsState()
    val playlists = viewModel.customPlaylists
    val queue = viewModel.playbackQueue
    val downloads = viewModel.downloadQueue

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        userScrollEnabled = true
    ) {
        itemsIndexed(items = tabs) { index, title ->
            val selected = selectedTab == index
            val icon = when (index) {
                0 -> Icons.Default.Dashboard
                else -> Icons.Default.Radio
            }
            val bubbleText: String? = when (index) {
                1 -> playlists.size.toString()
                2 -> queue.size.toString()
                3 -> downloads.size.toString()
                else -> null
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 6.dp, end = 8.dp)
            ) {
                Surface(
                    onClick = { onTabSelected(index) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    tonalElevation = if (selected) 6.dp else 1.dp
                ) {
                    Box(
                        modifier = Modifier.height(44.dp).padding(horizontal = if (selected) 16.dp else 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = title, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(tween(180)) + expandHorizontally(),
                                exit = fadeOut(tween(120)) + shrinkHorizontally()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.2.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Visible)
                                }
                            }
                        }
                    }
                }

                // Display Bubble Badge count if positive, offset outside the chip boundary
                if (bubbleText != null && bubbleText != "0") {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(18.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = bubbleText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onError,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SquareMediaCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TrackListRow(
    title: String,
    artist: String,
    durationText: String = "3:45",
    isFavorite: Boolean = false,
    onPlay: () -> Unit = {},
    onToggleFavorite: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onPlay() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            Text(artist, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(durationText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Removed duplicate RecentlyOpenedCard implementation to resolve conflicting overloads


// Removed duplicate HistoryGridCard implementation to resolve conflicting overloads

