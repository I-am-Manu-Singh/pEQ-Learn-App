package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import com.creativeblisslabs.peqlearn.app.Tab
import com.creativeblisslabs.peqlearn.app.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull
/**
 * INDUSTRIAL PLAYER ENGINE: pEQ NEURAL WORKSTATION
 * This extension handles cinematic transport, mixer control, and playback metadata.
 */

fun AppViewModel.stop() {
    audioEngine.stop()
    isPlaying.value = false
    syncNowPlaying()
}

fun AppViewModel.togglePlayback() {
    val enginePlaying = audioEngine.isPlaying()
    
    // Safety check: If UI thinks we are playing but engine is not, try to resume engine
    if (isPlaying.value && !enginePlaying) {
        audioEngine.resume()
        syncNowPlaying()
        return
    }

    // Restored track or paused idle engine check
    if (!enginePlaying) {
        val current = currentMediaItem.value
        val currentUri = audioEngine.getCurrentTrackUri()
        
        // If engine does not have the current track loaded yet, load it and restore position
        if (current != null && (currentUri == null || normalizePath(currentUri) != normalizePath(current.path))) {
            val savedPos = currentPosition.value
            audioEngine.loadTrack(current.path, current.title, current.artist, current.thumbnail)
            if (savedPos > 0L) {
                audioEngine.seekTo(savedPos)
            }
            audioEngine.resume()
            isPlaying.value = true
            syncNowPlaying()
            return
        } else if (current != null && currentUri != null) {
            // Already loaded in player (e.g. from restoreSession), just resume!
            audioEngine.resume()
            isPlaying.value = true
            syncNowPlaying()
            return
        }
    }

    if (isPlaying.value) {
        audioEngine.pause()
        isPlaying.value = false
    } else {
        audioEngine.resume()
        isPlaying.value = true
    }
    syncNowPlaying()
}

fun AppViewModel.playExternalFile(
    path: String,
    displayName: String? = null,
    mimeType: String? = null,
    sizeBytes: Long? = null,
    title: String? = null,
    artist: String? = null,
    durationMs: Long? = null
) {
    val decodedDisplayName = if (displayName != null) {
        displayName
    } else {
        path.substringAfterLast("/").substringBeforeLast(".")
    }
    
    val extension = if (displayName != null) {
        displayName.substringAfterLast(".", "").lowercase()
    } else {
        path.substringAfterLast(".").lowercase()
    }

    val isVideo = if (mimeType != null) {
        mimeType.startsWith("video/")
    } else {
        listOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "ts", "flv", "m4v").contains(extension) || 
        path.contains("video", ignoreCase = true) || 
        path.contains("DCIM", ignoreCase = true)
    }
    
    val item = pEQMediaItem(
        id = "external_${path.hashCode()}",
        title = if (title != null && title.isNotBlank()) title else decodedDisplayName.substringBeforeLast(".", decodedDisplayName),
        subtitle = if (artist != null && artist.isNotBlank()) artist else "External Source",
        artist = if (artist != null && artist.isNotBlank()) artist else "External Source",
        path = path,
        type = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
        format = if (extension.isNotEmpty()) extension.uppercase() else (if (isVideo) "VIDEO" else "AUDIO"),
        sizeBytes = sizeBytes ?: 0L,
        durationMs = durationMs ?: 0L
    )
    
    playMediaItem(item, addToQueue = true)
    
    // Explicitly set player tab and handle video fullscreen if needed
    currentTab.value = Tab.Player
    if (isVideo) {
        isVideoFullScreen.value = true
    }
}

fun AppViewModel.nextTrack() {
    if (playbackQueue.isEmpty()) return
    val nextIndex = if (isShuffleEnabled) {
        (0 until playbackQueue.size).random()
    } else {
        (currentQueueIndex.value + 1) % playbackQueue.size
    }
    setCurrentQueueIndex(nextIndex)
    playMediaItem(playbackQueue[nextIndex])
}

fun AppViewModel.previousTrack() {
    if (playbackQueue.isEmpty()) return
    val prevIndex = if (currentPosition.value > 5000) {
        currentQueueIndex.value
    } else {
        if (currentQueueIndex.value > 0) currentQueueIndex.value - 1 else playbackQueue.size - 1
    }
    setCurrentQueueIndex(prevIndex)
    playMediaItem(playbackQueue[prevIndex])
}

fun AppViewModel.seekTo(positionMs: Long) {
    currentPosition.longValue = positionMs
    isSeeking.value = true
    audioEngine.seekTo(positionMs)

    // SYNC NOTATION: If we have a score, sync the playhead when seeking in track
    currentScore.value?.let { score ->
        val totalSec = positionMs / 1000f
        val starts = measureStartTimesSeconds.value
        
        var targetM = 0
        var targetO = 0f
        
        for (i in score.measures.indices) {
            val start = starts.getOrNull(i) ?: 0f
            val m = score.measures[i]
            val dur = (60f / score.tempo) * m.timeSignatureNumerator
            
            if (totalSec < start + dur) {
                targetM = i
                targetO = (totalSec - start) / dur
                break
            }
            if (i == score.measures.lastIndex) {
                targetM = i
                targetO = 1.0f
            }
        }
        scorePlayheadMeasure.value = targetM
        scorePlayheadOffset.value = targetO
    }

    scope.launch {
        delay(400)
        isSeeking.value = false
    }
}

fun AppViewModel.seekRelative(deltaMs: Long) {
    val newPos = (currentPosition.value + deltaMs).coerceIn(0, currentDuration.value)
    seekTo(newPos)
}

fun AppViewModel.playMediaItem(item: pEQMediaItem, addToQueue: Boolean = true, fromHomeScreen: Boolean = false) {
    currentMediaItem.value = item
    
    scope.launch {
        val playPath = if (item.path.startsWith("http") && (item.path.contains("youtube.com") || item.path.contains("youtu.be"))) {
            cloudDownloader?.getStreamUrl(item.path) ?: item.path
        } else item.path
        
        audioEngine.playTrack(playPath, item.title, item.artist, item.thumbnail)
        isPlaying.value = true

        var finalLyrics = item.lyrics
        if (finalLyrics.isNullOrBlank() && !item.path.startsWith("http")) {
            val normalizedPath = normalizePath(item.path)
            try {
                val file = java.io.File(normalizedPath)
                if (file.exists()) {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    finalLyrics = audioFile.tag?.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                }
            } catch (_: Exception) {}
        }

       
    }
    
    if (addToQueue) {
        val existingIndex = playbackQueue.indexOfFirst { it.path == item.path }
        if (existingIndex == -1) {
            playbackQueue.add(0, item)
            setCurrentQueueIndex(0)
        } else {
            setCurrentQueueIndex(existingIndex)
        }
    }

    if (!recentlyPlayedItems.any { it.path == item.path }) {
        recentlyPlayedItems.add(0, item)
        if (recentlyPlayedItems.size > 20) recentlyPlayedItems.removeAt(recentlyPlayedItems.size - 1)
    }
    
    updateMostPlayed(item)
    if (item.type == MediaType.VIDEO) {
        currentTab.value = Tab.Player
    }
    
    // If video, enter immersive mode immediately
    if (item.type == MediaType.VIDEO) {
        isVideoFullScreen.value = true
    } else {
        isVideoFullScreen.value = false
    }
    
    persistSession()
    syncNowPlaying()
}

fun AppViewModel.playMediaItemFromQueue(index: Int) {
    if (index in playbackQueue.indices) {
        val item = playbackQueue[index]
        setCurrentQueueIndex(index)
        playMediaItem(item, addToQueue = false)
        syncNowPlaying()
    }
}


fun AppViewModel.onTrackLoaded() {
    scope.launch {
        val engineMetadata = audioEngine.getEmbeddedMetadata()
        
        var embeddedLyricsText: String? = null
        var trackTitle = currentMediaItem.value?.title ?: ""
        var trackArtist = currentMediaItem.value?.artist ?: "External Source"
        var sampleRateVal = audioEngine.getSampleRate()
        var bitDepthVal = audioEngine.getBitDepth()
        var codecVal = audioEngine.getCodec()
        var sizeBytesVal = currentMediaItem.value?.sizeBytes ?: 0L
        var durationMsVal = currentMediaItem.value?.durationMs ?: 0L

        val currentPath = currentMediaItem.value?.path ?: ""
        if (!currentPath.startsWith("http")) {
            val normalizedPath = normalizePath(currentPath)
            try {
                val file = java.io.File(normalizedPath)
                if (file.exists()) {
                    if (sizeBytesVal == 0L) sizeBytesVal = file.length()
                    
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    val tag = audioFile.tag
                    if (tag != null) {
                        embeddedLyricsText = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                        val tagTitle = tag.getFirst(org.jaudiotagger.tag.FieldKey.TITLE)
                        if (!tagTitle.isNullOrBlank()) {
                            trackTitle = tagTitle
                        }
                        val tagArtist = tag.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST)
                        if (!tagArtist.isNullOrBlank()) {
                            trackArtist = tagArtist
                        }
                    }
                }
            } catch (_: Exception) {}

            // Platform-independent metadata extraction is handled via audioEngine embedded probe
            if (trackTitle.isBlank()) {
                trackTitle = normalizedPath.substringAfterLast("/").substringBeforeLast(".")
            }
        }

        if (trackTitle.isBlank()) {
            trackTitle = currentPath.substringAfterLast("/").substringBeforeLast(".")
        }
        if (trackArtist.isBlank()) {
            trackArtist = "External Source"
        }

        val updatedItem = currentMediaItem.value?.copy(
            title = trackTitle,
            subtitle = trackArtist,
            artist = trackArtist,
            bitrate = engineMetadata.availableTracks.firstOrNull { it.type == TrackType.AUDIO }?.codec?.toIntOrNull() ?: 0,
            codec = codecVal,
            sampleRate = sampleRateVal,
            bitDepth = bitDepthVal,
            sizeBytes = sizeBytesVal,
            durationMs = if (durationMsVal > 0L) durationMsVal else (currentMediaItem.value?.durationMs ?: 0L),
            lyrics = if (!embeddedLyricsText.isNullOrBlank()) embeddedLyricsText else currentMediaItem.value?.lyrics
        )

        currentMediaItem.value = updatedItem
        
        // Update the queue item as well if present
        val queueIndex = playbackQueue.indexOfFirst { it.path == currentPath }
        if (queueIndex != -1 && updatedItem != null) {
            playbackQueue[queueIndex] = updatedItem
        }

        currentSampleRate.value = sampleRateVal
        currentBitDepth.value = bitDepthVal
        currentCodec.value = codecVal

        delay(500)
        delay(1000)
        persistSession()
        syncNowPlaying()
    }
}

fun AppViewModel.addFolderToQueue(path: String) {
    val folderItems = mediaLibrary.value.filter { it.path.startsWith(path) }
    playbackQueue.addAll(folderItems)
}

fun AppViewModel.addFolderToPlaylist(path: String, playlistName: String) {
    val folderItems = mediaLibrary.value.filter { it.path.startsWith(path) }
    val playlist = customPlaylists[playlistName]
    playlist?.addAll(folderItems)
    persistSession()
}



// Transport helpers moved from AppViewModel.kt
fun AppViewModel.cyclePlaybackSpeed() {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val currentIndex = speeds.indexOf(playbackSpeed.value)
    val nextIndex = (currentIndex + 1) % speeds.size
    playbackSpeed.value = speeds[nextIndex]
    audioEngine.setPlaybackSpeed(playbackSpeed.value)
}

fun AppViewModel.shuffleQueue() {
    val current = playbackQueue.toList()
    playbackQueue.clear()
    playbackQueue.addAll(current.shuffled())
    showToast("Queue Shuffled")
}

fun AppViewModel.openPlaylistAsQueue(name: String) {
    val items = if (name == "FAVORITES") favoriteItems else customPlaylists[name] ?: emptyList()
    if (items.isEmpty()) return
    playbackQueue.clear()
    playbackQueue.addAll(items)
    playMediaItem(items.first())
}

fun AppViewModel.shufflePlaylistAndPlay(name: String) {
    val items = if (name == "FAVORITES") favoriteItems else customPlaylists[name] ?: emptyList()
    if (items.isEmpty()) return
    val shuffled = items.shuffled()
    playbackQueue.clear()
    playbackQueue.addAll(shuffled)
    playMediaItem(shuffled.first())
}

fun AppViewModel.addToQueue(item: pEQMediaItem) {
    playbackQueue.add(item)
    showToast("Added to Queue")
}

fun AppViewModel.removeFromQueue(item: pEQMediaItem) {
    playbackQueue.remove(item)
}

fun AppViewModel.playFromQueue(item: pEQMediaItem) {
    playMediaItem(item)
}

fun AppViewModel.reorderQueue(from: Int, to: Int) {
    if (from < playbackQueue.size && to < playbackQueue.size) {
        val item = playbackQueue.removeAt(from)
        playbackQueue.add(to, item)
    }
}

fun AppViewModel.cycleRepeatMode() {
    val next = when(repeatMode.value) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
        else -> RepeatMode.OFF
    }
    repeatMode.value = next
    audioEngine.setRepeatMode(next)
}

fun AppViewModel.toggleShuffle() {
    isShuffleEnabled = !isShuffleEnabled
    audioEngine.setShuffleMode(isShuffleEnabled)
}

fun AppViewModel.toggleAbRepeat() {
    abRepeatActive.value = !abRepeatActive.value
    if (abRepeatActive.value) {
        val pos = currentPosition.value
        val dur = currentDuration.value.takeIf { it > 0 } ?: (pos + 10000L)
        abRepeatPointA.value = pos
        abRepeatPointB.value = (pos + 5000L).coerceAtMost(dur)
        audioEngine.setLoopPoints(abRepeatPointA.value, abRepeatPointB.value)
    } else {
        abRepeatPointA.value = -1L
        abRepeatPointB.value = -1L
        audioEngine.setLoopPoints(-1L, -1L)
        updateLoopPoints(-1L, -1L)
    }
}

fun AppViewModel.setABPoint(point: Char) {
    val currentPos = currentPosition.value
    if (point == 'A') {
        abRepeatPointA.value = currentPos
    } else {
        val pointA = abRepeatPointA.value
        if (currentPos > pointA && pointA != -1L) {
            abRepeatPointB.value = currentPos
        } else {
            abRepeatPointA.value = currentPos
            abRepeatPointB.value = -1L
        }
    }
    if (abRepeatPointA.value != -1L && abRepeatPointB.value != -1L) {
        audioEngine.setLoopPoints(abRepeatPointA.value, abRepeatPointB.value)
    }
}



fun AppViewModel.updateLoopPoints(a: Long?, b: Long?) {
    audioEngine.setLoopPoints(a, b)
}

fun AppViewModel.syncNowPlaying() {
    val now = Clock.System.now().toEpochMilliseconds()
    // Throttle to once every 2 seconds unless playback state changes
    if (now - lastSyncTime < 2000 && audioEngine.isPlaying() == isPlaying.value) return
    
    lastSyncTime = now
    persistenceManager.updateNowPlaying(currentMediaItem.value, isPlaying.value)
}

fun AppViewModel.syncNotificationState() {}

fun AppViewModel.clearQueue() {
    playbackQueue.clear()
    _currentQueueIndex.value = -1
    currentMediaItem.value = null
    isPlaying.value = false
    audioEngine.stop()
    persistSession()
}

fun AppViewModel.updateStemVolume(stem: String, volume: Float) {
    audioEngine.setStemVolume(stem, volume)
}

fun AppViewModel.toggleStemMute(stem: String) {
    val currentMuted = audioEngine.isStemMuted(stem)
    audioEngine.setStemMute(stem, !currentMuted)
}

fun AppViewModel.toggleStemSolo(stem: String, active: Boolean? = null) {
    val newState = active ?: !audioEngine.isStemSolo(stem)
    audioEngine.setStemSolo(stem, newState)
}

fun AppViewModel.updateStemPan(stem: String, pan: Float) {
    audioEngine.setStemPan(stem, pan)
}

fun AppViewModel.playStems() {
    audioEngine.playStems()
    isStemsPlaying.value = true
}

fun AppViewModel.pauseStems() {
    audioEngine.pauseStems()
    isStemsPlaying.value = false
}

fun AppViewModel.toggleStemsPlayback() {
    if (isStemsPlaying.value) {
        pauseStems()
    } else {
        playStems()
    }
}
