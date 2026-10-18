package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.data.MediaType
import com.creativeblisslabs.peqlearn.app.data.pEQMediaItem
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHome(viewModel: AppViewModel) {
    val items by viewModel.mediaLibrary.collectAsState()
    val recentlyPlayed = viewModel.recentlyPlayedItems
    val playlists = viewModel.customPlaylists
    val favorites = viewModel.favoriteItems

    var streamUrl by remember { mutableStateOf("") }
    var selectedPlaylistName by remember { mutableStateOf<String?>(null) }
    var historyViewGrid by remember { mutableStateOf(false) }

    // Dialog States
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var trackForPlaylistAction by remember { mutableStateOf<pEQMediaItem?>(null) }
    var showAddTrackToPlaylistDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isLandscapeOrWide = maxWidth > 800.dp || maxWidth > maxHeight

        // 1. PLAYLIST DETAIL VIEW
        if (selectedPlaylistName != null) {
            val playlistName = selectedPlaylistName!!
            val playlistTracks = if (playlistName == "FAVORITES") favorites else playlists[playlistName] ?: emptyList()

            if (isLandscapeOrWide) {
                // Split playlist view: left side header & actions, right side tracks
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedPlaylistName = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "PLAYLIST",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = playlistName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${playlistTracks.size} tracks total",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )

                                if (playlistName != "FAVORITES") {
                                    Button(
                                        onClick = {
                                            viewModel.deletePlaylist(playlistName)
                                            selectedPlaylistName = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("DELETE PLAYLIST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (playlistTracks.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "NO TRACKS IN THIS PLAYLIST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                items(playlistTracks) { item ->
                                    TrackListRow(
                                        item = item,
                                        onPlay = { viewModel.playMediaItem(item) },
                                        onRemove = {
                                            viewModel.removeFromPlaylist(playlistName, item)
                                        },
                                        onAddToOtherPlaylist = {
                                            trackForPlaylistAction = item
                                            showAddTrackToPlaylistDialog = true
                                        },
                                        isFavorite = favorites.contains(item),
                                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { selectedPlaylistName = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlistName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${playlistTracks.size} tracks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        if (playlistName != "FAVORITES") {
                            IconButton(
                                onClick = {
                                    viewModel.deletePlaylist(playlistName)
                                    selectedPlaylistName = null
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (playlistTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "NO TRACKS IN THIS PLAYLIST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 140.dp)
                        ) {
                            items(playlistTracks) { item ->
                                TrackListRow(
                                    item = item,
                                    onPlay = { viewModel.playMediaItem(item) },
                                    onRemove = {
                                        viewModel.removeFromPlaylist(playlistName, item)
                                    },
                                    onAddToOtherPlaylist = {
                                        trackForPlaylistAction = item
                                        showAddTrackToPlaylistDialog = true
                                    },
                                    isFavorite = favorites.contains(item),
                                    onToggleFavorite = { viewModel.toggleFavorite(item) }
                                )
                            }
                        }
                    }
                }
            }
        } else if (isLandscapeOrWide) {
            // 2. MAIN DASHBOARD SPLIT VIEW (Landscape / Wide)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "pEQ Learn",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Interactive Music Learning",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.onPickTrackRequested?.invoke { pickedItem ->
                                viewModel.playMediaItem(pickedItem, fromHomeScreen = true)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("OPEN AUDIO / VIDEO FILE", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // LEFT COLUMN
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MostPlayedSection(viewModel)

                        // PLAYLISTS SECTION
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader("PLAYLISTS")
                                TextButton(
                                    onClick = { showCreatePlaylistDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("NEW", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            if (playlists.isEmpty() && favorites.isEmpty()) {
                                pEQGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "No playlists yet — tap NEW to create one",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        )
                                    }
                                }
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    item {
                                        PlaylistCard(
                                            name = "Favorites",
                                            count = favorites.size,
                                            onClick = { selectedPlaylistName = "FAVORITES" }
                                        )
                                    }
                                    items(playlists.keys.toList()) { name ->
                                        PlaylistCard(
                                            name = name,
                                            count = playlists[name]?.size ?: 0,
                                            onClick = { selectedPlaylistName = name }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(80.dp))
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                    // RIGHT COLUMN: HISTORY & RECENTS
                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                SectionHeader("HISTORY")
                                Text(
                                    "${recentlyPlayed.size} files",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                            IconButton(onClick = { historyViewGrid = !historyViewGrid }) {
                                Icon(
                                    imageVector = if (historyViewGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = if (historyViewGrid) "Switch to list view" else "Switch to grid view",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (recentlyPlayed.isEmpty()) {
                            pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        "NO HISTORY YET",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                    Text(
                                        "Pick a local file or stream a URL to get started",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        } else if (historyViewGrid) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                items(recentlyPlayed) { item ->
                                    HistoryGridCard(
                                        item = item,
                                        isFavorite = favorites.contains(item),
                                        onPlay = { viewModel.playMediaItem(item, fromHomeScreen = true) },
                                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                                        onAddToPlaylist = {
                                            trackForPlaylistAction = item
                                            showAddTrackToPlaylistDialog = true
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                items(recentlyPlayed) { item ->
                                    RecentlyOpenedCard(
                                        item = item,
                                        onPlay = { viewModel.playMediaItem(item, fromHomeScreen = true) },
                                        onAddToPlaylist = {
                                            trackForPlaylistAction = item
                                            showAddTrackToPlaylistDialog = true
                                        },
                                        isFavorite = favorites.contains(item),
                                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 2. MAIN DASHBOARD / WORKSTATION VIEW (PORTRAIT SINGLE COLUMN)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 140.dp)
            ) {
                // Header & Open File Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "pEQ Learn",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Interactive Music Learning",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.onPickTrackRequested?.invoke { pickedItem ->
                                    viewModel.playMediaItem(pickedItem, fromHomeScreen = true)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("OPEN AUDIO / VIDEO FILE", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            }
                        }
                    }
                }

                // MOST PLAYED SECTION
                item {
                    MostPlayedSection(viewModel)
                }

                // PLAYLISTS SECTION
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader("PLAYLISTS")
                            TextButton(
                                onClick = { showCreatePlaylistDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("NEW", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        if (playlists.isEmpty() && favorites.isEmpty()) {
                            pEQGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No playlists yet — tap NEW to create one",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                item {
                                    PlaylistCard(
                                        name = "Favorites",
                                        count = favorites.size,
                                        onClick = { selectedPlaylistName = "FAVORITES" }
                                    )
                                }
                                items(playlists.keys.toList()) { name ->
                                    PlaylistCard(
                                        name = name,
                                        count = playlists[name]?.size ?: 0,
                                        onClick = { selectedPlaylistName = name }
                                    )
                                }
                            }
                        }
                    }
                }

                // UNIFIED HISTORY SECTION
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SectionHeader("HISTORY")
                            Text(
                                "${recentlyPlayed.size} files",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        IconButton(onClick = { historyViewGrid = !historyViewGrid }) {
                            Icon(
                                imageVector = if (historyViewGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (historyViewGrid) "Switch to list view" else "Switch to grid view",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (recentlyPlayed.isEmpty()) {
                    item {
                        pEQGlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                                Text(
                                    "NO HISTORY YET",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                                Text(
                                    "Pick a local file or stream a URL above to get started",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                } else if (historyViewGrid) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 900.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false
                        ) {
                            items(recentlyPlayed) { item ->
                                HistoryGridCard(
                                    item = item,
                                    isFavorite = favorites.contains(item),
                                    onPlay = { viewModel.playMediaItem(item, fromHomeScreen = true) },
                                    onToggleFavorite = { viewModel.toggleFavorite(item) },
                                    onAddToPlaylist = {
                                        trackForPlaylistAction = item
                                        showAddTrackToPlaylistDialog = true
                                    }
                                )
                            }
                        }
                    }
                } else {
                    items(recentlyPlayed) { item ->
                        RecentlyOpenedCard(
                            item = item,
                            onPlay = { viewModel.playMediaItem(item, fromHomeScreen = true) },
                            onAddToPlaylist = {
                                trackForPlaylistAction = item
                                showAddTrackToPlaylistDialog = true
                            },
                            isFavorite = favorites.contains(item),
                            onToggleFavorite = { viewModel.toggleFavorite(item) }
                        )
                    }
                }
            }
        }

        // 3. DIALOGS
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("Create New Playlist") },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName.trim())
                                newPlaylistName = ""
                                showCreatePlaylistDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddTrackToPlaylistDialog && trackForPlaylistAction != null) {
            val targetTrack = trackForPlaylistAction!!
            AlertDialog(
                onDismissRequest = { showAddTrackToPlaylistDialog = false },
                title = { Text("Add Track to Playlist") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = targetTrack.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        if (playlists.isEmpty()) {
                            Text(
                                "No playlists created. Create one below:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                placeholder = { Text("Playlist Name", fontSize = 14.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (newPlaylistName.isNotBlank()) {
                                        viewModel.createPlaylist(newPlaylistName)
                                        viewModel.addToPlaylist(newPlaylistName, targetTrack)
                                        newPlaylistName = ""
                                        showAddTrackToPlaylistDialog = false
                                    }
                                },
                                enabled = newPlaylistName.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("CREATE PLAYLIST", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                playlists.keys.forEach { pName ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addToPlaylist(pName, targetTrack)
                                                showAddTrackToPlaylistDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlaylistPlay, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(10.dp))
                                        Text(pName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddTrackToPlaylistDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun MostPlayedSection(viewModel: AppViewModel) {
    var timeFilter by remember { mutableStateOf("ALL") }
    val items by viewModel.mediaLibrary.collectAsState()
    
    val filteredItems = remember(items, timeFilter) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val threshold = when (timeFilter) {
            "WEEK" -> 7 * 24 * 3600 * 1000L
            "MONTH" -> 30 * 24 * 3600 * 1000L
            "YEAR" -> 365 * 24 * 3600 * 1000L
            else -> Long.MAX_VALUE
        }
        items.filter { it.playCount > 0 && (timeFilter == "ALL" || it.lastPlayed == 0L || (now - it.lastPlayed) < threshold) }
             .sortedByDescending { it.playCount }
             .take(10)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("MOST PLAYED")

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("WEEK", "MONTH", "YEAR", "ALL").forEach { filter ->
                    TextButton(
                        onClick = { timeFilter = filter },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            filter, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Black,
                            color = if (timeFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        if (filteredItems.isEmpty()) {
            pEQGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "NO RECENT PLAYBACK HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(filteredItems) { item ->
                    SquareMediaCard(
                        item = item,
                        onClick = { viewModel.playMediaItem(item, fromHomeScreen = true) }
                    )
                }
            }
        }
    }
}
@Composable
fun PlaylistCard(name: String, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        pEQGlassCard(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$count tracks",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SquareMediaCard(item: pEQMediaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        pEQGlassCard(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
        ) {
            Box {
                pEQImage(
                    data = item.thumbnail,
                    fallbackPath = item.path,
                    modifier = Modifier.fillMaxSize()
                )
                
                if (item.playCount > 0) {
                    pEQGlassCard(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = item.playCount.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.artist ?: "Unknown Artist",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentlyOpenedCard(
    item: pEQMediaItem,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isVideo = item.path.lowercase().let { p ->
        p.contains(".mp4") || p.contains(".mkv") || p.contains(".mov") || p.contains(".webm") || p.contains(".3gp")
    }

    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pEQGlassCard(
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    pEQImage(data = item.thumbnail, fallbackPath = item.path, modifier = Modifier.fillMaxSize())
                    if (isVideo) {
                        pEQGlassCard(
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.artist ?: if (isVideo) "Video" else "Unknown Artist",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.durationMs > 0L) {
                        val mins = (item.durationMs / 60000).toInt()
                        val secs = ((item.durationMs % 60000) / 1000).toInt()
                        val secsStr = if (secs < 10) "0$secs" else "$secs"
                        Text(
                            text = "$mins:$secsStr",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onAddToPlaylist() }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackListRow(
    item: pEQMediaItem,
    onPlay: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onAddToOtherPlaylist: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pEQGlassCard(
                modifier = Modifier.size(44.dp)
            ) {
                pEQImage(
                    data = item.thumbnail,
                    fallbackPath = item.path,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.artist ?: "Unknown Artist",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onAddToOtherPlaylist()
                        }
                    )

                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from Playlist", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.PlaylistRemove, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryGridCard(
    item: pEQMediaItem,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onPlay)
    ) {
        Box(Modifier.fillMaxSize()) {
            pEQImage(
                data = item.thumbnail,
                fallbackPath = item.path,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, null)
                            },
                            onClick = {
                                showMenu = false
                                onAddToPlaylist()
                            }
                        )
                    }
                }
            }

            pEQGlassCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        item.artist ?: "Unknown Artist",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}