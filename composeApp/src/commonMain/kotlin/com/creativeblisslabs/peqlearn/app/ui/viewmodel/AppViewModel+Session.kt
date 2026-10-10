package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.observeLibraryState
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.refreshLibrary
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull

/**
 * SESSION ENGINE EXTENSION: pEQ NEURAL WORKSTATION
 * Encapsulates session restoration, persistence, and state synchronization.
 */

fun AppViewModel.restoreSession() {
    val manager = persistenceManager

    scope.launch {
        try {
            // 1. Restore Scan Folders immediately to allow library refresh
            val storedFolders = manager.loadScanFolders()
            scanFolders.clear()
            scanFolders.addAll(storedFolders)
            
            // 🚀 Kick off library scan so items exist when we wait for them
            refreshLibrary()

            // 🚀 Industrial Library Synchronization
            scope.launch {
                mediaLibrary.collect { library ->
                    val current = currentMediaItem.value ?: return@collect
                    if (current.subtitle == "Media Resource" || current.artist == "Unknown Artist" || current.subtitle == "Local archive" || current.subtitle == "Neural Engine") {
                        library.find { it.path == current.path }?.let { matched ->
                            currentMediaItem.value = matched
                            println("Industrial Sync: Resolved fallback metadata for ${matched.title}")
                        }
                    }
                }
            }

            // 2. Restore Session State
            val session = manager.loadSession() ?: SessionState()
            isLandscapeLocked.value = session.isRotationLocked
            isLeftHandMuted.value = session.isLeftHandMuted
            isRightHandMuted.value = session.isRightHandMuted
            isLeftHandHidden.value = session.isLeftHandHidden
            isRightHandHidden.value = session.isRightHandHidden
            session.leftHandColor?.let { leftHandColor.value = Color(it.toLong()) }
            session.rightHandColor?.let { rightHandColor.value = Color(it.toLong()) }
            transposition.intValue = session.notationTransposition

            // 3. Wait for library to populate
            val items = withTimeoutOrNull(4000) { 
                mediaLibrary.first { it.isNotEmpty() }
            } ?: mediaLibrary.value

            val resolveItem: (String) -> com.creativeblisslabs.peqlearn.app.data.pEQMediaItem? = { path ->
                val normalized = normalizePath(path)
                _mediaLibrary.value.find { normalizePath(it.path) == normalized } ?: run {
                    val engineUri = audioEngine.getCurrentTrackUri()
                    val isCurrentActive = engineUri != null && normalizePath(engineUri) == normalized
                    val isLastPlayed = session.lastTrackPath != null && normalizePath(session.lastTrackPath) == normalized
                    
                    if (isCurrentActive || isLastPlayed) {
                        com.creativeblisslabs.peqlearn.app.data.pEQMediaItem(
                            id = "engine_${path.hashCode()}",
                            title = session.lastTrackTitle ?: path.substringAfterLast("/").substringBeforeLast("."),
                            subtitle = session.lastTrackArtist ?: "Neural Engine",
                            artist = session.lastTrackArtist,
                            path = path,
                            thumbnail = session.lastTrackThumbnail,
                            type = if (path.lowercase().let { it.contains("video") || it.contains(".mp4") || it.contains(".mkv") }) 
                                com.creativeblisslabs.peqlearn.app.data.MediaType.VIDEO else com.creativeblisslabs.peqlearn.app.data.MediaType.AUDIO,
                            format = path.substringAfterLast(".").uppercase(),
                            sizeBytes = 0L,
                            durationMs = 0L
                        )
                    } else if (path.startsWith("http")) {
                        com.creativeblisslabs.peqlearn.app.data.pEQMediaItem(
                            id = "stream_${path.hashCode()}",
                            title = "Web Stream",
                            subtitle = path,
                            path = path,
                            type = if (path.lowercase().let { it.contains(".mp4") || it.contains(".m3u8") || it.contains(".mov") || it.contains("video") || it.contains(".mkv") }) 
                                com.creativeblisslabs.peqlearn.app.data.MediaType.VIDEO else com.creativeblisslabs.peqlearn.app.data.MediaType.AUDIO,
                            format = "STREAM",
                            sizeBytes = 0L,
                            durationMs = 0L,
                            isStream = true
                        )
                    } else {
                        val fileName = path.substringAfterLast("/").substringBeforeLast(".")
                        val extension = path.substringAfterLast(".").lowercase()
                        val isVideoExt = listOf("mp4", "mkv", "webm", "mov", "3gp").contains(extension)
                        
                        com.creativeblisslabs.peqlearn.app.data.pEQMediaItem(
                            id = "resolved_${path.hashCode()}",
                            title = fileName,
                            subtitle = "Local archive",
                            path = path,
                            type = if (isVideoExt) com.creativeblisslabs.peqlearn.app.data.MediaType.VIDEO else com.creativeblisslabs.peqlearn.app.data.MediaType.AUDIO,
                            format = extension.uppercase(),
                            sizeBytes = 0L,
                            durationMs = 0L
                        )
                    }
                }
            }

            // 4. Restore Queue
            if (session.queue.isNotEmpty()) {
                playbackQueue.clear()
                playbackQueue.addAll(session.queue)
            }

            // 5. Restore Current Item
            val activeUri = audioEngine.getCurrentTrackUri()
            val targetPath = activeUri ?: session.lastTrackPath

            if (targetPath != null) resolveItem(targetPath)?.let { restoredItem ->
                currentMediaItem.value = restoredItem
                currentPosition.value = session.lastPosition
                
                currentSampleRate.value = restoredItem.sampleRate ?: 44100
                currentBitDepth.value = restoredItem.bitDepth ?: 16
                
                val index = playbackQueue.indexOfFirst { normalizePath(it.path) == normalizePath(restoredItem.path) }
                if (index != -1) {
                    setCurrentQueueIndex(index)
                }
                
                if (activeUri == null && !audioEngine.isPlaying() && audioEngine.getDuration() <= 0L) {
                    if (restoredItem.type != MediaType.VIDEO) {
                        audioEngine.loadTrack(restoredItem.path, restoredItem.title, restoredItem.subtitle, restoredItem.thumbnail)
                        if (session.lastPosition > 0L) {
                            audioEngine.seekTo(session.lastPosition)
                        }
                    }
                } else if (audioEngine.getDuration() > 0L) {
                    currentPosition.value = audioEngine.getCurrentPosition()
                }
                // Ensure currentMediaItem reflects the accurately restored track immediately
                currentMediaItem.value = restoredItem
            }

            // 6. Restore Playlists & Favorites
            val storedPlaylists = manager.loadPlaylists()
            customPlaylists.clear()
            storedPlaylists.forEach { (name, paths) ->
                customPlaylists[name] = paths.mapNotNull { p -> resolveItem(p) }.toMutableStateList()
            }
            
            val storedFavorites = session.favorites.ifEmpty { manager.loadFavorites() }
            favoriteItems.clear()
            favoriteItems.addAll(storedFavorites.mapNotNull { p -> resolveItem(p) })

            val storedRecent = manager.loadRecentSessions()
            recentlyPlayedItems.clear()
            recentlyPlayedItems.addAll(storedRecent.mapNotNull { path -> resolveItem(path) })
            
            // 7. Restore Playback Flags
            isShuffleEnabled = session.isShuffle
            repeatMode.value = session.repeatMode
            favorites.addAll(session.favorites)
            
            // Restore DSP State
            if (session.eqGains.isNotEmpty()) {
                session.eqGains.forEachIndexed { index, gain -> 
                    if (index < eqGains.size) {
                        eqGains[index] = gain
                        audioEngine.updateEqBand(index, gain)
                    }
                }
            }
            if (session.eqQs.isNotEmpty()) {
                session.eqQs.forEachIndexed { index, q -> 
                    if (index < eqQs.size) {
                        eqQs[index] = q
                        audioEngine.updateEqQ(index, q)
                    }
                }
            }
            session.lastIrPath?.let { path ->
                convolverIrPath.value = path
                convolverIrName.value = path.substringAfterLast("/")
            }

            // 8. Load Play Counts
            val storedCounts = manager.loadMostPlayed()
            playCounts.putAll(storedCounts)
            
            // Note: Since we don't have a separate lastPlayed persistence yet, 
            // we'll assume current session for now or implement it in PersistenceManager later.
            // For now, let's at least populate the library items with what we have.
            _mediaLibrary.value = _mediaLibrary.value.map { item ->
                item.copy(playCount = playCounts[item.path] ?: 0)
            }
            
            val (path, show) = manager.loadLibraryState()
            currentFolderPath.value = path
            showFolders.value = show
            
            // Sync now playing metadata to widget
            syncNowPlaying()
            
        } catch (e: Exception) {
            println("pEQControl: [SYSTEM] Session restore failed: ${e.message}")
        } finally {
            completeSplash()
        }
    }
    
    observeLibraryState()
}

fun AppViewModel.persistSession() {
    // 📸 Capture snapshots on the Main thread to avoid ConcurrentModificationException on IO thread
    val sessionToSave = SessionState(
        lastTrackPath = currentMediaItem.value?.path,
        lastTrackTitle = currentMediaItem.value?.title,
        lastTrackArtist = currentMediaItem.value?.artist,
        lastTrackThumbnail = currentMediaItem.value?.thumbnail,
        lastPosition = currentPosition.value,
        isPlaying = isPlaying.value,
        queue = playbackQueue.toList(),
        isShuffle = isShuffleEnabled,
        repeatMode = repeatMode.value,
        favorites = favorites.toList(),
        eqGains = eqGains.toList(),
        eqQs = eqQs.toList(),
        isLeftHandMuted = isLeftHandMuted.value,
        isRightHandMuted = isRightHandMuted.value,
        isLeftHandHidden = isLeftHandHidden.value,
        isRightHandHidden = isRightHandHidden.value,
        leftHandColor = leftHandColor.value.toArgb(),
        rightHandColor = rightHandColor.value.toArgb(),
        notationTransposition = transposition.intValue,
        lastIrPath = convolverIrPath.value,
        lastSubtitleTrackId = selectedSubtitleTrack.value?.id,
        lastAudioTrackId = selectedAudioTrack.value?.id
    )
    
    val playlistsToSave = customPlaylists.mapValues { entry -> entry.value.map { item -> item.path } }
    val favoritesToSave = favoriteItems.map { it.path }
    val recentToSave = recentlyPlayedItems.map { it.path }

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        persistenceManager.saveSession(sessionToSave)
        persistenceManager.savePlaylists(playlistsToSave)
        persistenceManager.saveFavorites(favoritesToSave)
        persistenceManager.saveRecentSessions(recentToSave)
    }
}
