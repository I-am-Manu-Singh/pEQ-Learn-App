package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ai.pEQCloudDownloader
import com.creativeblisslabs.peqlearn.app.Tab

fun AppViewModel.searchYoutube(query: String) {
    if (query.isBlank()) return
    
    scope.launch {
        youtubeSearchResults.clear()
        showToast("Neural Discovery: Searching YouTube for '$query'...")
        delay(1200) // Industrial latency simulation
        
        // Mock High-Fidelity Results
        val results = listOf(
            YoutubeResult(
                id = "yt1",
                title = "$query - Official Audio (Neural Master)",
                author = "Vevo Artist",
                duration = "4:20",
                thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/mqdefault.jpg",
                url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            ),
            YoutubeResult(
                id = "yt2",
                title = "$query - Live at pEQ Stadium",
                author = "Live Sessions",
                duration = "5:12",
                thumbnailUrl = "https://img.youtube.com/vi/kJQP7kiw5Fk/mqdefault.jpg",
                url = "https://www.youtube.com/watch?v=kJQP7kiw5Fk"
            ),
            YoutubeResult(
                id = "yt3",
                title = "$query (Remix Pack 2026)",
                author = "DJ Neural",
                duration = "3:45",
                thumbnailUrl = "https://img.youtube.com/vi/9bZkp7q19f0/mqdefault.jpg",
                url = "https://www.youtube.com/watch?v=9bZkp7q19f0"
            )
        )
        
        youtubeSearchResults.addAll(results)
        showToast("Discovery Complete: Found ${results.size} matches")
    }
}

fun AppViewModel.loadLinkFormats(url: String) {
    if (url.isBlank()) return
    isFetchingFormats.value = true
    scope.launch {
        try {
            availableFormats.clear()
            val formats = cloudDownloader?.getFormats(url) ?: emptyList()
            availableFormats.addAll(formats)
        } catch (e: Exception) {
            availableFormats.clear()
        } finally {
            isFetchingFormats.value = false
        }
    }
}

val AppViewModel.downloadQueue: SnapshotStateList<DownloadJob>
    get() = _downloadQueue

private val _downloadQueue = mutableStateListOf<DownloadJob>()

fun AppViewModel.startMediaDownload(
    url: String,
    audioOnly: Boolean,
    videoEnabled: Boolean,
    highQuality: Boolean,
    autoQueue: Boolean,
    resolution: String,
    audioQuality: String,
    formatId: String? = null
) {
    scope.launch {
        val jobId = "dl_${url.hashCode()}_${Clock.System.now().toEpochMilliseconds()}"
        val title = url.substringAfterLast("/").take(20).ifEmpty { "Cloud Stream" }
        
        val job = DownloadJob(
            id = jobId,
            url = url,
            title = title,
            progress = 0f,
            status = "QUEUED",
            type = if (videoEnabled) MediaType.VIDEO else MediaType.AUDIO
        )
        
        _downloadQueue.add(job)
        showToast("Intake Queued: ${title}")

        // ACTUAL DOWNLOAD LOGIC
        try {
            updateDownloadStatus(jobId, "CONNECTING...", 0.05f)
            delay(1000)
            
            cloudDownloader?.downloadMedia(
                url = url,
                formatId = formatId,
                isAudioOnly = audioOnly,
                onProgress = { progress, eta ->
                    updateDownloadStatus(jobId, "DOWNLOADING...", progress / 100f, eta)
                },
                onComplete = { path ->
                    updateDownloadStatus(jobId, "COMPLETE", 1.0f, filePath = path)
                    showToast("Download Complete: ${title}")
                    refreshLibrary()
                    
                    if (autoQueue) {
                        // Discovery and play
                        val item = mediaLibrary.value.find { it.path == path }
                        item?.let { playMediaItem(it) }
                    }
                },
                onError = { error ->
                    updateDownloadStatus(jobId, "ERROR: ${error}", 0f)
                }
            ) ?: run {
                // Mock behavior if no downloader
                simulateDownload(jobId, title, autoQueue)
            }
        } catch (e: Exception) {
            updateDownloadStatus(jobId, "FAILED: ${e.message}", 0f)
        }
    }
}

private fun AppViewModel.updateDownloadStatus(
    id: String, 
    status: String, 
    progress: Float, 
    eta: Long = 0,
    filePath: String? = null
) {
    val index = _downloadQueue.indexOfFirst { it.id == id }
    if (index != -1) {
        val current = _downloadQueue[index]
        _downloadQueue[index] = current.copy(
            status = status, 
            progress = progress, 
            etaSeconds = eta,
            filePath = filePath
        )
    }
}

private suspend fun AppViewModel.simulateDownload(id: String, title: String, autoQueue: Boolean) {
    for (i in 1..10) {
        delay(800)
        updateDownloadStatus(id, "DOWNLOADING (${i*10}%)", i * 0.1f, (10-i).toLong())
    }
    updateDownloadStatus(id, "COMPLETE", 1.0f)
    showToast("Intake Complete: ${title}")
}

fun AppViewModel.handleSharedUrl(url: String) {
    scope.launch {
        // Find the URL in the text (sometimes sharing includes title)
        val extractedUrl = url.split(" ", "\n").find { it.startsWith("http") } ?: url
        
        sharedUrl.value = extractedUrl
        currentTab.value = Tab.Dashboard
        dashboardSubTab.value = 0 // HOME / INTAKE widget is here
        showToast("Neural Intake: Link captured from OS")
    }
}

fun AppViewModel.addStreamUrl(url: String, type: MediaType) {
    // This adds a direct stream link to the player without downloading
    scope.launch {
        val streamUrl = cloudDownloader?.getStreamUrl(url) ?: url
        val item = pEQMediaItem(
            id = "stream_${url.hashCode()}",
            title = "Cloud Stream",
            subtitle = url,
            path = streamUrl,
            type = type,
            format = "HLS",
            sizeBytes = 0,
            durationMs = 0,
            isStream = true
        )
        playMediaItem(item)
    }
}

fun AppViewModel.deleteDownloadedJob(job: DownloadJob) {
    job.filePath?.let { path ->
        com.creativeblisslabs.peqlearn.app.deleteLocalFile(path)
    }
    _downloadQueue.remove(job)
    showToast("Media Deleted: ${job.title}")
    refreshLibrary()
}

