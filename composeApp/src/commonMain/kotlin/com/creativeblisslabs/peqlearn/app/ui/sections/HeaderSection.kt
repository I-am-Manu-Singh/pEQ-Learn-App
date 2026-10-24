@file:OptIn(ExperimentalMaterial3Api::class)

package com.creativeblisslabs.peqlearn.app.ui.sections

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.creativeblisslabs.peqlearn.app.data.Device
import com.creativeblisslabs.peqlearn.app.data.MediaType
import com.creativeblisslabs.peqlearn.app.Tab
import com.creativeblisslabs.peqlearn.app.auroraBorder
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.theme.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.painterResource
import peq_learn.composeapp.generated.resources.Res
import peq_learn.composeapp.generated.resources.app_icon

@Composable
fun HeaderSection(
    viewModel: AppViewModel
) {

    var showAudioPanel by remember {
        mutableStateOf(false)
    }

    var showDeviceSheet by remember {
        mutableStateOf(false)
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val nowPlaying =
        viewModel.currentMediaItem.value

    BoxWithConstraints {

        val isLandscape =
            maxWidth > maxHeight

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        if (isLandscape) 10.dp
                        else 8.dp,

                    vertical =
                        if (isLandscape) 4.dp
                        else 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(
                    if (isLandscape) 6.dp
                    else 10.dp
                )
        ) {
            // =========================
            // PLAYER CARD
            // =========================
            NowPlayingBar(
                viewModel = viewModel,
                isLandscape = isLandscape,
                modifier = Modifier.weight(1f) // <--- Handled here instead of inside the composable
            )
        }
    }
}
@Composable
fun NowPlayingBar(
    viewModel: AppViewModel,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val nowPlaying = viewModel.currentMediaItem.value
    val isPlaying by viewModel.isPlaying
    val speed = viewModel.playbackSpeed.value
    val abActive = viewModel.abRepeatActive.value

    Surface(
        onClick = {
            if (nowPlaying != null) {
                viewModel.currentTab.value = Tab.Player
                if (nowPlaying.type == MediaType.VIDEO) {
                    viewModel.isVideoFullScreen.value = true
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .auroraBorder(),
        shape = RoundedCornerShape(if (isLandscape) 18.dp else 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
    ) {
        if (nowPlaying != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                
                // CONTENT CONTAINER (Vertical Stack)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isLandscape) 10.dp else 14.dp, vertical = 10.dp)
                ) {
                    // ROW 1: Artwork Thumbnail & Title/Subtitle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork Thumbnail Box
                        Box(
                            modifier = Modifier
                                .size(if (isLandscape) 38.dp else 46.dp)
                                .clip(RoundedCornerShape(if (isLandscape) 10.dp else 14.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            pEQImage(
                                data = nowPlaying.thumbnail,
                                fallbackPath = nowPlaying.path,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.85f),
                                contentScale = ContentScale.Crop,
                                mediaType = nowPlaying.type
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Title & Subtitle
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = nowPlaying.title,
                                fontSize = if (isLandscape) 11.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = nowPlaying.subtitle.ifBlank { "pEQ Lossless Audio" },
                                fontSize = if (isLandscape) 9.sp else 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Transport & Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // A-B Repeat Button
                        IconButton(
                            onClick = { viewModel.toggleAbRepeat() },
                            modifier = Modifier.size(if (isLandscape) 26.dp else 32.dp)
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = "A-B Repeat",
                                tint = if (abActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.size(if (isLandscape) 14.dp else 18.dp)
                            )
                        }

                        // Previous Track
                        IconButton(
                            onClick = { viewModel.previousTrack() },
                            modifier = Modifier.size(if (isLandscape) 26.dp else 32.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp)
                            )
                        }

                        // Play / Pause
                        FilledIconButton(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(if (isLandscape) 18.dp else 22.dp)
                            )
                        }

                        // Next Track
                        IconButton(
                            onClick = { viewModel.nextTrack() },
                            modifier = Modifier.size(if (isLandscape) 26.dp else 32.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp)
                            )
                        }

                        // Favorite Button
                        IconButton(
                            onClick = { viewModel.toggleFavorite(nowPlaying) },
                            modifier = Modifier.size(if (isLandscape) 26.dp else 32.dp)
                        ) {
                            val isFavorite = viewModel.favoriteItems.any { it.id == nowPlaying.id }
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.size(if (isLandscape) 14.dp else 18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Secondary Sub-Controls Row (Speed & Playlist actions)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = { viewModel.cyclePlaybackSpeed() },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = if (isLandscape) 9.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                viewModel.selectedItemForPlaylist.value = nowPlaying
                                viewModel.isPlaylistBulkAction.value = false
                                viewModel.isPlaylistPickerShown.value = true
                            },
                            modifier = Modifier.size(if (isLandscape) 26.dp else 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp)
                            )
                        }
                    }
                }

                val progress = (
                    viewModel.currentPosition.value.toFloat() /
                        viewModel.currentDuration.value.coerceAtLeast(1L).toFloat()
                ).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 2.dp else 3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
            }
        } else {
            // Empty State
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 10.dp else 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(if (isLandscape) 10.dp else 14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.size(if (isLandscape) 38.dp else 46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(Res.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.width(if (isLandscape) 8.dp else 12.dp))

                Column {
                    Text(
                        text = "pEQ Learn Player",
                        fontSize = if (isLandscape) 11.sp else 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "No active playback",
                        fontSize = if (isLandscape) 8.sp else 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}