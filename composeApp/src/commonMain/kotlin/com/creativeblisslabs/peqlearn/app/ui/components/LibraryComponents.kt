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
