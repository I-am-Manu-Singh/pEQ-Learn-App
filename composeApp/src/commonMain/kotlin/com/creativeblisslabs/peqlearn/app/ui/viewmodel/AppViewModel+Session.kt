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
            // 1. Restore Scan Folders and Play Counts immediately so scanner uses them from start
            val storedFolders = manager.loadScanFolders()
            scanFolders.clear()
            scanFolders.addAll(storedFolders)
            
            val storedCounts = manager.loadMostPlayed()
            playCounts.putAll(storedCounts)

            // Restore Theme & Accent
            val savedTheme = manager.loadTheme()
            val savedAccent = manager.loadAccent()
            
            try {
                setTheme(com.creativeblisslabs.peqlearn.app.ui.theme.pEQTheme.valueOf(savedTheme))
            } catch (e: Exception) {
                setTheme(com.creativeblisslabs.peqlearn.app.ui.theme.pEQTheme.DARK)
            }
            
            try {
                setAccent(com.creativeblisslabs.peqlearn.app.ui.theme.pEQAccent.valueOf(savedAccent))
            } catch (e: Exception) {
                setAccent(com.creativeblisslabs.peqlearn.app.ui.theme.pEQAccent.AMBER)
            }
            
            // 🚀 Kick off library scan so items exist when we wait for them
            refreshLibrary()

            // 🚀 Industrial Library Synchronization: Update metadata in customPlaylists, favorites, and recents once scanner emits
            scope.launch {
                mediaLibrary.collect { library ->
                    if (library.isEmpty()) return@collect
                    val libByPath = library.associateBy { normalizePath(it.path) }

                    // Re-sync currentMediaItem if fallback
                    val current = currentMediaItem.value
                    if (current != null) {
                        libByPath[normalizePath(current.path)]?.let { matched ->
                            currentMediaItem.value = matched
                        }
                    }

                    // Re-sync favoriteItems
                    val updatedFavs = favoriteItems.map { fav ->
                        libByPath[normalizePath(fav.path)] ?: fav
                    }
                    if (updatedFavs != favoriteItems.toList()) {
                        favoriteItems.clear()
                        favoriteItems.addAll(updatedFavs)
                    }

                    // Re-sync customPlaylists
                    customPlaylists.keys.toList().forEach { name ->
                        val list = customPlaylists[name] ?: return@forEach
                        val updatedList = list.map { item -> libByPath[normalizePath(item.path)] ?: item }
                        if (updatedList != list.toList()) {
                            list.clear()
                            list.addAll(updatedList)
                        }
                    }

                    // Re-sync recentlyPlayedItems
                    val updatedRecents = recentlyPlayedItems.map { rec ->
                        libByPath[normalizePath(rec.path)] ?: rec
                    }
                    if (updatedRecents != recentlyPlayedItems.toList()) {
                        recentlyPlayedItems.clear()
                        recentlyPlayedItems.addAll(updatedRecents)
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
            notationFormat.value = session.notationFormat
            isSplitStaff.value = session.isSplitStaff
            isVerticalScroll.value = session.isVerticalScroll
            showPianoKeyLabels.value = session.showPianoKeyLabels
            is3DPerspectiveMode.value = session.is3DPerspectiveMode
            horizonScale.floatValue = session.horizonScale
            showWaterfallParticles.value = session.showWaterfallParticles
            fallingNotesSpeed.value = session.fallingNotesSpeed
            selectedKeySignature.value = session.selectedKeySignature
            selectedTimeSignature.value = session.selectedTimeSignature
            selectedInstrument.value = session.selectedInstrument
            quantizeDivision.value = session.quantizeDivision
            useOsmdRenderer.value = session.useOsmdRenderer
            showWaterfall.value = session.showWaterfall
            particleCount.intValue = session.particleCount
            particleSpeed.floatValue = session.particleSpeed
            particleScale.floatValue = session.particleScale
            particleExpansion.floatValue = session.particleExpansion
            lockParticleColor.value = session.lockParticleColor
            flashSize.floatValue = session.flashSize
            flashHaloIntensity.floatValue = session.flashHaloIntensity
            noteCornerRadius.floatValue = session.noteCornerRadius
            noteEdgeThickness.floatValue = session.noteEdgeThickness
            noteStyleOval.value = session.noteStyleOval
            keyboardHighlightActive.value = session.keyboardHighlightActive
            keyboardShowChords.value = session.keyboardShowChords
            keyboardMinorEdges.value = session.keyboardMinorEdges
            keyboardShowVelvetCloth.value = session.keyboardShowVelvetCloth
            keyboardShowSideFrames.value = session.keyboardShowSideFrames
            showPedals.value = session.showPedals
            pedalScaling.floatValue = session.pedalScaling
            pedalOpacity.floatValue = session.pedalOpacity

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
            isSessionRestored.value = true
        } catch (e: Exception) {
            println("pEQLearn: [SYSTEM] Session restore failed: ${e.message}")
            isSessionRestored.value = true
        } finally {
            completeSplash()
        }
    }
    
    observeLibraryState()
}

fun AppViewModel.persistSession() {
    // 🛡️ Prevent wiping playlists, favorites, and recents if persistSession is invoked before restoreSession completes
    if (!isSessionRestored.value) {
        return
    }

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
        favorites = favoriteItems.map { it.path },
        isLeftHandMuted = isLeftHandMuted.value,
        isRightHandMuted = isRightHandMuted.value,
        isLeftHandHidden = isLeftHandHidden.value,
        isRightHandHidden = isRightHandHidden.value,
        leftHandColor = leftHandColor.value.toArgb(),
        rightHandColor = rightHandColor.value.toArgb(),
        notationTransposition = transposition.intValue,
        notationFormat = notationFormat.value,
        isSplitStaff = isSplitStaff.value,
        isVerticalScroll = isVerticalScroll.value,
        showPianoKeyLabels = showPianoKeyLabels.value,
        is3DPerspectiveMode = is3DPerspectiveMode.value,
        horizonScale = horizonScale.floatValue,
        showWaterfallParticles = showWaterfallParticles.value,
        fallingNotesSpeed = fallingNotesSpeed.value,
        selectedKeySignature = selectedKeySignature.value,
        selectedTimeSignature = selectedTimeSignature.value,
        selectedInstrument = selectedInstrument.value,
        quantizeDivision = quantizeDivision.value,
        useOsmdRenderer = useOsmdRenderer.value,
        showWaterfall = showWaterfall.value,
        particleCount = particleCount.intValue,
        particleSpeed = particleSpeed.floatValue,
        particleScale = particleScale.floatValue,
        particleExpansion = particleExpansion.floatValue,
        lockParticleColor = lockParticleColor.value,
        flashSize = flashSize.floatValue,
        flashHaloIntensity = flashHaloIntensity.floatValue,
        noteCornerRadius = noteCornerRadius.floatValue,
        noteEdgeThickness = noteEdgeThickness.floatValue,
        noteStyleOval = noteStyleOval.value,
        keyboardHighlightActive = keyboardHighlightActive.value,
        keyboardShowChords = keyboardShowChords.value,
        keyboardMinorEdges = keyboardMinorEdges.value,
        keyboardShowVelvetCloth = keyboardShowVelvetCloth.value,
        keyboardShowSideFrames = keyboardShowSideFrames.value,
        showPedals = showPedals.value,
        pedalScaling = pedalScaling.floatValue,
        pedalOpacity = pedalOpacity.floatValue
    )
    
    val playlistsToSave = customPlaylists.mapValues { entry -> entry.value.map { item -> item.path } }
    val favoritesToSave = favoriteItems.map { it.path }
    val recentToSave = recentlyPlayedItems.map { it.path }

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        persistenceManager.saveSession(sessionToSave)
        persistenceManager.savePlaylists(playlistsToSave)
        persistenceManager.saveFavorites(favoritesToSave)
        persistenceManager.saveRecentSessions(recentToSave)
        persistenceManager.saveMostPlayed(playCounts.toMap())
        persistenceManager.saveTheme(selectedTheme.value.name)
        persistenceManager.saveAccent(selectedAccent.value.name)
    }
}
