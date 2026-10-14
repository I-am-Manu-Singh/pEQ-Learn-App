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
    viewModel: AppViewModel, // Or substitute with individual states/lambdas for previews
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val nowPlaying =
        viewModel.currentMediaItem.value
    // 1. Observe your state properly (using collectAsState or direct value if mutableStateOf)
    val isPlaying by viewModel.isPlaying

    Surface(
        onClick = {
            if (nowPlaying != null) {
                viewModel.currentTab.value = Tab.Player
                if (nowPlaying.type == MediaType.VIDEO) {
                    viewModel.isVideoFullScreen.value = true
                }
            }
        },
        // 2. Removed .weight(1f) here so it doesn't throw a function invocation error
        // when called outside a Row/Column scope. Add .weight(1f) at the call-site if needed.
        modifier = modifier
            .heightIn(
                min = if (isLandscape) 54.dp else 64.dp,
                max = if (isLandscape) 62.dp else 74.dp
            )
            .auroraBorder(),
        shape = RoundedCornerShape(if (isLandscape) 22.dp else 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
    ) {
        if (nowPlaying != null) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isLandscape) 10.dp else 12.dp,
                            vertical = if (isLandscape) 6.dp else 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // =========================
                    // THUMBNAIL
                    // =========================
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 38.dp else 46.dp)
                            .clip(RoundedCornerShape(if (isLandscape) 10.dp else 14.dp))
                            .background(Color.Black)
                    ) {
                        pEQImage(
                            data = nowPlaying.thumbnail,
                            fallbackPath = nowPlaying.path,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.75f),
                            contentScale = ContentScale.Crop,
                            mediaType = nowPlaying.type
                        )
                    }

                    Spacer(Modifier.width(if (isLandscape) 8.dp else 10.dp))

                    // =========================
                    // INFO
                    // =========================
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = nowPlaying.title,
                            fontSize = if (isLandscape) 10.sp else 11.sp,
                            lineHeight = if (isLandscape) 12.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = nowPlaying.subtitle,
                            fontSize = if (isLandscape) 8.sp else 9.sp,
                            lineHeight = if (isLandscape) 9.sp else 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(if (isLandscape) 2.dp else 6.dp))

                    // =========================
                    // CONTROLS
                    // =========================
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 1.dp else 3.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.previousTrack() },
                            modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = null,
                                modifier = Modifier.size(if (isLandscape) 13.dp else 16.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier.size(if (isLandscape) 28.dp else 34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(if (isLandscape) 15.dp else 18.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.nextTrack() },
                            modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(if (isLandscape) 13.dp else 16.dp)
                            )
                        }
                    }
                }

                val progress = (
                        viewModel.currentPosition.value.toFloat() /
                                viewModel.currentDuration.value.coerceAtLeast(1L).toFloat()
                        ).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 2.dp else 3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isLandscape) 10.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
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