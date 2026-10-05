package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.LocalAppViewModel
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

/**
 * INDUSTRIAL LIBRARY ENGINE: pEQ NEURAL WORKSTATION
 * This module manages high-fidelity media cards, folder registries, and playlist managers.
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun pEQItemCard(
    item: pEQMediaItem,
    isActive: Boolean = false,
    showFavorite: Boolean = true,
    showMenu: Boolean = true,
    showIndex: Boolean = false,
    showPlayCount: Boolean = false,
    index: Int = 0,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) = MediaItemCard(item, isActive, showFavorite, showMenu, showIndex, showPlayCount, index, modifier, trailing, onClick, onLongClick)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun pEQItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    item: pEQMediaItem,
    isActive: Boolean = false,
    showFavorite: Boolean = true,
    showMenu: Boolean = true,
    showIndex: Boolean = false,
    showPlayCount: Boolean = false,
    index: Int = 0,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val viewModel = LocalAppViewModel.current
    val isFavorite = remember(item, viewModel.favoriteItems.size) {
        viewModel.favoriteItems.contains(item)
    }
    var showMenuState by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(22.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1.0f),
        border = BorderStroke(1.2.dp, if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)),
        tonalElevation = if (isActive) 8.dp else 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                    pEQImage(
                        data = item.thumbnail,
                        fallbackPath = item.path,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        mediaType = item.type
                    )
                }
                if (isActive) {
                    Box(modifier = Modifier.matchParentSize().background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), shape = RoundedCornerShape(16.dp)))
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }
                } else if (showIndex) {
                    Surface(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                        Box(modifier = Modifier.size(21.dp), contentAlignment = Alignment.Center) {
                            Text(text = index.toString(), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title.ifBlank { "Unknown Media" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.subtitle.ifBlank { "Unknown Artist" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (showPlayCount && item.playCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(text = item.playCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    MetadataBadge(text = if (item.type == MediaType.VIDEO) buildString { append(item.resolution ?: "VIDEO"); item.codec?.let { append(" • "); append(it) } } else buildString { append("${item.bitDepth}Bit"); if (item.sampleRate > 0) { append(" • "); append("${item.sampleRate / 1000}kHz") } })
                }
            }
            Spacer(Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showFavorite) {
                    IconButton(onClick = { viewModel.toggleFavorite(item) }, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
                    }
                }
                if (showMenu) {
                    Box {
                        IconButton(onClick = { showMenuState = true }, modifier = Modifier.size(34.dp)) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showMenuState, onDismissRequest = { showMenuState = false }) {
                            DropdownMenuItem(text = { Text("Play Next") }, leadingIcon = { Icon(imageVector = Icons.Default.SkipNext, contentDescription = null) }, onClick = { viewModel.playNext(item); showMenuState = false })
                            DropdownMenuItem(text = { Text("Add to Queue") }, leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) }, onClick = { viewModel.addToQueue(item); showMenuState = false })
                            DropdownMenuItem(text = { Text("Add to Playlist") }, leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }, onClick = { viewModel.isPlaylistBulkAction.value = false; viewModel.selectedItemForPlaylist.value = item; viewModel.isPlaylistPickerShown.value = true; showMenuState = false })
                            DropdownMenuItem(text = { Text("Info") }, leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) }, onClick = { viewModel.selectedItemForInfo.value = item; viewModel.showTrackInfoSheet.value = true; showMenuState = false })
                            DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(imageVector = Icons.Default.Share, contentDescription = null) }, onClick = { viewModel.shareItem(item); showMenuState = false })
                            DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) }, onClick = { viewModel.deleteItem(item); showMenuState = false })
                        }
                    }
                }
                trailing?.invoke()
            }
        }
    }
}

@Composable
fun MediaFolderCard(
    name: String,
    audioCount: Int = 0,
    videoCount: Int = 0,
    totalSize: Long = 0,
    onAddQueue: () -> Unit = {},
    onAddPlaylist: () -> Unit = {},
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name.substringAfterLast("/"), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (audioCount > 0) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
                        Text(text = " $audioCount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                    }
                    if (videoCount > 0) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
                        Text(text = " $videoCount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddQueue) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onAddPlaylist) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(4.dp))
                Text(text = "${totalSize / 1024 / 1024}MB", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPicker(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val playlists = viewModel.playlists
    val selectedSingleItem = viewModel.selectedItemForPlaylist.value ?: viewModel.currentMediaItem.value
    val selectedItems = remember(selectedSingleItem, viewModel.playbackQueue.size, viewModel.isPlaylistBulkAction.value, viewModel.isPlaylistPickerShown.value) {
        if (viewModel.isPlaylistBulkAction.value) {
            viewModel.playbackQueue.toList()
        } else {
            listOfNotNull(selectedSingleItem)
        }
    }
    val totalItems = selectedItems.size
    var newPlaylistName by remember { mutableStateOf("") }
    var showCreateDialogInPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 12.dp, dragHandle = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Surface(modifier = Modifier.width(52.dp).height(5.dp), shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)) {}
            Spacer(Modifier.height(16.dp))
        }
    }) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Playlist Manager", fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(text = if (totalItems == 1) "Add media into your collection." else "Add these tracks into a playlist.", fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { showCreateDialogInPicker = true }, modifier = Modifier.size(42.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
                if (showCreateDialogInPicker) {
                    var tempName by remember { mutableStateOf("") }
                    AlertDialog(onDismissRequest = { showCreateDialogInPicker = false }, title = { Text("NEW COLLECTION", fontWeight = FontWeight.Black) }, text = { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, placeholder = { Text("Playlist Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true) }, confirmButton = { TextButton(onClick = { if (tempName.isNotBlank()) { viewModel.createPlaylist(tempName); selectedItems.forEach { item -> viewModel.addToPlaylist(tempName, item) }; showCreateDialogInPicker = false; onDismiss() } }, enabled = tempName.isNotBlank()) { Text("CREATE & ADD", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showCreateDialogInPicker = false }) { Text("CANCEL") } })
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Your Playlists", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) { Text(text = "${playlists.size} PLAYLISTS", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(16.dp))
            if (playlists.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 70.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(88.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), modifier = Modifier.size(38.dp)) }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(text = "No Playlists Yet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(text = "Initialize your first archive to begin.", textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(28.dp))
                        OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, placeholder = { Text("Playlist Name", fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth(0.85f), shape = RoundedCornerShape(16.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { if (newPlaylistName.isNotBlank()) { viewModel.createPlaylist(newPlaylistName); selectedItems.forEach { item -> viewModel.addToPlaylist(newPlaylistName, item) }; newPlaylistName = ""; onDismiss() } }, enabled = newPlaylistName.isNotBlank(), modifier = Modifier.fillMaxWidth(0.85f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("CREATE PLAYLIST", fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp)) {
                    items(items = playlists.keys.toList(), key = { it }) { playlistName ->
                        val trackCount = playlists[playlistName]?.size ?: 0
                        Surface(onClick = { selectedItems.forEach { item -> viewModel.addToPlaylist(playlistName, item) }; onDismiss() }, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(54.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) } }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = playlistName, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = "$trackCount tracks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add To Playlist", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) } }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PEQArchiveRegistryDialog(onDismiss: () -> Unit) {
    val viewModel = LocalAppViewModel.current
    val folders = viewModel.scanFolders
    val scanProgress by viewModel.scanProgress
    val scanStatus by viewModel.scanStatus
    var newPath by remember { mutableStateOf("") }
    val quickFolders = listOf("Music", "Download", "Movies", "DCIM", "Pictures", "Podcasts", "Audiobooks", "Recordings")
    val availableQuickFolders = quickFolders.filterNot { folderName -> folders.any { it.contains("/$folderName") } }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 12.dp, sheetMaxWidth = 680.dp, dragHandle = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Surface(modifier = Modifier.width(48.dp).height(5.dp), shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)) {}
            Spacer(Modifier.height(12.dp))
        }
    }) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(54.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Archive Registry", fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(text = "Manage storage paths and synchronize media assets with your local engine.", fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = onDismiss) { Icon(imageVector = Icons.Default.Close, contentDescription = "Close") }
            }
            AnimatedVisibility(visible = scanProgress > 0f && scanProgress < 1f) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    LinearProgressIndicator(progress = { scanProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = scanStatus.ifBlank { "Scanning media..." }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(10.dp))
                        Text(text = "${(scanProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Quick Add", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) { Text(text = "${folders.size}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(14.dp))
            if (availableQuickFolders.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
                    items(availableQuickFolders) { folderName ->
                        ElevatedFilterChip(selected = false, onClick = { viewModel.addScanFolder("${viewModel.primaryStoragePath}/$folderName") }, label = { Text(text = folderName, fontSize = 11.sp, maxLines = 1) }, leadingIcon = { Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }, shape = RoundedCornerShape(14.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Custom Path", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = newPath, onValueChange = { newPath = it }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), singleLine = true, placeholder = { Text(text = "Add storage path...", fontSize = 11.sp) }, textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), leadingIcon = { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null) }, trailingIcon = { AnimatedVisibility(visible = newPath.isNotBlank()) { IconButton(onClick = { viewModel.addScanFolder(newPath.trim()); newPath = "" }) { Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm", tint = MaterialTheme.colorScheme.primary) } } })
                        FilledTonalIconButton(onClick = { viewModel.requestFolderPicker() }, modifier = Modifier.size(54.dp)) { Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Browse Folders") }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(text = "Registered Paths", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 140.dp, max = 340.dp)) {
                if (folders.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(74.dp)) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), modifier = Modifier.size(32.dp)) } }
                        Spacer(Modifier.height(18.dp))
                        Text(text = "No folders registered", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(text = "Add folders above to begin scanning.", textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(items = folders, key = { it }) { folder ->
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), modifier = Modifier.size(46.dp)) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp)) } }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = folder.substringAfterLast("/"), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(3.dp))
                                        Text(text = folder, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    FilledTonalIconButton(onClick = { viewModel.removeScanFolder(folder) }, modifier = Modifier.size(40.dp)) { Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Remove Folder", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { viewModel.refreshLibrary() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) { Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(text = "Rescan", fontSize = 12.sp) }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) { Text(text = "Done", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MediaSelectionSheet(
    title: String,
    items: List<T>,
    selectedItem: T? = null,
    isLoading: Boolean = false,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    onClose: () -> Unit,
    onItemClick: (T) -> Unit,
    onOnlineSearch: (() -> Unit)? = null,
    itemKey: (T) -> Any,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String? = { null },
    itemTrailing: @Composable ((T, Boolean) -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onPickFile: (() -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 12.dp, sheetMaxWidth = 620.dp, dragHandle = {
        Box(modifier = Modifier.padding(top = 12.dp, bottom = 10.dp).width(48.dp).height(5.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), CircleShape))
    }) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(text = when { 
                        isLoading -> ""
                        items.isEmpty() -> "No items available" 
                        items.size == 1 -> "1 item detected" 
                        else -> "${items.size} items detected" 
                    }, fontSize = 11.sp, color = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (actionLabel != null && onAction != null) { TextButton(onClick = onAction) { Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) } }
                    if (onOnlineSearch != null && !isLoading) { FilledTonalButton(onClick = onOnlineSearch) { Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(text = "Online", fontSize = 11.sp) }; Spacer(Modifier.width(8.dp)) }
                    if (onPickFile != null) { FilledTonalIconButton(onClick = onPickFile) { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Pick Local File") }; Spacer(Modifier.width(8.dp)) }
                    FilledTonalIconButton(onClick = onClose) { Icon(imageVector = Icons.Default.Close, contentDescription = null) }
                }
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            
            if (isLoading) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(38.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(24.dp))
                    Text(text = "Searching...", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    
                }
            } else if (items.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(74.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = emptyIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), modifier = Modifier.size(34.dp)) } }
                    Spacer(Modifier.height(18.dp))
                    Text(text = emptyTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(text = emptyMessage, textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)) {
                    itemsIndexed(items = items, key = { i, item -> "${itemKey(item)}_$i" }) { _, item ->
                        val isSelected = selectedItem == item
                        Surface(onClick = { onItemClick(item) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) } }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = itemTitle(item), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    itemSubtitle(item)?.let { Spacer(Modifier.height(3.dp)); Text(text = it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                itemTrailing?.invoke(item, isSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobProgressItem(job: SplitJob) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(job.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${(job.progress * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { job.progress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        Text(job.status, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

fun mapResolutionToLabel(resolution: String?): String {
    if (resolution == null) return "VIDEO"
    return when {
        resolution.contains("2160") -> "4K"
        resolution.contains("1080") -> "FHD"
        resolution.contains("720") -> "HD"
        else -> resolution
    }
}
