package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

/**
 * INDUSTRIAL PLAYER ENGINE: pEQ NEURAL WORKSTATION
 * This module manages the high-fidelity queue drawer, transport telemetry, and subtitle controls.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueContent(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val queue = viewModel.playbackQueue
    val currentIndex by viewModel.currentQueueIndex
    val listState = rememberLazyListState()
    val searchQuery = viewModel.globalSearchQuery.value

    val filtered = queue.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
    }

    val activeFilteredIndex = filtered.indexOfFirst { it.path == viewModel.currentMediaItem.value?.path }

    LaunchedEffect(currentIndex, queue.size, activeFilteredIndex) {
        if (activeFilteredIndex != -1) {
            kotlinx.coroutines.delay(250)
            if (!listState.isScrollInProgress) {
                try {
                    listState.scrollToItem(activeFilteredIndex)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    if (filtered.isEmpty()) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Surface(
                    modifier = Modifier.size(86.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = if (searchQuery.isNotBlank())
                        "No Matching Queue Items"
                    else
                        "Queue Empty",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (searchQuery.isNotBlank())
                        "No queued media matched your search."
                    else
                        "Your playback queue is currently empty.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

    } else {

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 90.dp
            )
        ) {

            itemsIndexed(
                items = filtered,
                key = { index, item ->
                    "${item.path}_${index}_${item.id}"
                }
            ) { index, item ->

                val currentPlayingPath =
                    viewModel.currentMediaItem.value?.path

                val isCurrent = item.path == currentPlayingPath

                var accumulatedDrag by remember(item.path, index) {
                    mutableFloatStateOf(0f)
                }

                val dismissState = rememberSwipeToDismissBoxState(
                    positionalThreshold = { it * 0.22f },
                    confirmValueChange = { value ->
                        if (isCurrent) {
                            viewModel.showToast("Current playing cannot be removed")
                            return@rememberSwipeToDismissBoxState false
                        }

                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.removeFromQueue(item)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = !isCurrent,

                    backgroundContent = {

                        if (!isCurrent) {

                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            ) {

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 18.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {

                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(Modifier.width(6.dp))

                                        Text(
                                            text = "REMOVE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {

                    MediaItemCard(
                        item = item,
                        isActive = isCurrent,
                        showFavorite = false,
                        showMenu = true,
                        showIndex = true,
                        index = index + 1,

                        onClick = {
                            val latestIndex = queue.indexOfFirst {
                                it.path == item.path
                            }

                            if (latestIndex != -1) {
                                viewModel.playMediaItemFromQueue(latestIndex)
                            }
                        },

                        trailing = if (isCurrent) null else {
                            {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        )
                                        .pointerInput(item.path, queue.size) {
                                            detectVerticalDragGestures(
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDrag += dragAmount
                                                    val currentItemIndex = queue.indexOfFirst { it.path == item.path }
                                                    if (currentItemIndex == -1) {
                                                        return@detectVerticalDragGestures
                                                    }
                                                    val threshold = 65f
                                                    if (accumulatedDrag > threshold && currentItemIndex < queue.lastIndex) {
                                                        viewModel.reorderQueue(currentItemIndex, currentItemIndex + 1)
                                                        accumulatedDrag = 0f
                                                    } else if (accumulatedDrag < -threshold && currentItemIndex > 0) {
                                                        viewModel.reorderQueue(currentItemIndex, currentItemIndex - 1)
                                                        accumulatedDrag = 0f
                                                    }
                                                },
                                                onDragEnd = {
                                                    accumulatedDrag = 0f
                                                }
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Reorder Queue",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        },

                        modifier = Modifier.animateContentSize()
                    )
                }
            }

            item(key = "queue_bottom_spacing") {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueDrawer(
    viewModel: AppViewModel,
    onClose: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = onClose
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),

                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "UP NEXT (${viewModel.playbackQueue.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // SHUFFLE ALL BUTTON
                FilledTonalIconButton(
                    onClick = {
                        viewModel.shuffleQueue()
                    },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (viewModel.isShuffleEnabled)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (viewModel.isShuffleEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ADD TO PLAYLIST BUTTON
                FilledIconButton(
                    onClick = {
                        viewModel.isPlaylistBulkAction.value = true
                        viewModel.selectedItemForPlaylist.value = null
                        viewModel.isPlaylistPickerShown.value = true
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            QueueContent(viewModel = viewModel)
        }
    }
}

@Composable
fun SubtitleSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(0.92f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(999.dp), color = accentColor.copy(alpha = 0.12f)) {
                    Text(text = valueText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }
            Spacer(Modifier.height(12.dp))
            Slider(value = value, onValueChange = onValueChange, valueRange = range)
        }
    }
}
