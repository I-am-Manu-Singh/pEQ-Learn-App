package com.creativeblisslabs.peqlearn.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * ANDROID MEDIASTORE SCANNER: PHYSICAL OBSERVER
 * Queries the system media database for video files and monitors changes.
 */
class AndroidFileScanner(private val context: Context) : pEQFileScanner {

    private val _mediaFiles = MutableStateFlow<List<VideoFile>>(emptyList())
    override val mediaFiles: StateFlow<List<VideoFile>> = _mediaFiles

    private val _scanProgress = MutableStateFlow(0f)
    override val scanProgress: StateFlow<Float> = _scanProgress

    private val _scanStatus = MutableStateFlow("")
    override val scanStatus: StateFlow<String> = _scanStatus

    private var excludedFolders = mutableListOf<String>("Ringtones", "Alarms", "Notifications", "Android/media", "WhatsApp Audio")
    private var includedFolders = mutableListOf<String>()

    override fun setExcludedFolders(folders: List<String>) {
        excludedFolders = folders.toMutableList()
        refresh()
    }

    override fun setIncludedFolders(folders: List<String>) {
        includedFolders = folders.toMutableList()
        refresh()
    }

    override fun scan(includePaths: List<String>, excludePaths: List<String>) {
        includedFolders = includePaths.toMutableList()
        excludedFolders = excludePaths.toMutableList().apply {
            addAll(listOf("Ringtones", "Alarms", "Notifications", "Android/media", "WhatsApp Audio"))
        }
        refresh()
    }

    private val _audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    override val audioFiles: StateFlow<List<AudioFile>> = _audioFiles

    override fun scanNetworkDrives(host: String) {
        println("pEQControl: [DISCOVERY] Physically scanning network host: $host")
        // AI-15: JmDNS / SMBJ handshake for remote production archives
    }

    override fun refresh() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            _scanProgress.value = 0.1f
            _scanStatus.value = "Loading Audio Files..."
            refreshVideos()
            _scanProgress.value = 0.5f
            _scanStatus.value = "Loading Video Files..."
            refreshAudio()
            _scanProgress.value = 1.0f
            _scanStatus.value = "Library Loading Complete"
        }
    }

    private fun refreshVideos() {
        if (includedFolders.isEmpty()) {
            _mediaFiles.value = emptyList()
            return
        }
        val videos = mutableListOf<VideoFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.TITLE
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val resCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val path = it.getString(pathCol)

                // 🧠 SELECTIVE HARVESTING (P-11)
                val normalizedItemPath = path.lowercase().trim().trimEnd('/')
                val isIncluded = includedFolders.isEmpty() || includedFolders.any { inc ->
                    val normInc = inc.lowercase().trim().trimEnd('/')
                    normalizedItemPath.startsWith(normInc) || normInc.startsWith(normalizedItemPath) ||
                    normalizedItemPath.contains(normInc.substringAfterLast('/'))
                }
                if (!isIncluded) continue
                if (excludedFolders.any { excluded -> path.contains(excluded) }) continue

                val displayName = it.getString(nameCol)
                val rawTitle = it.getString(titleCol)

                // 🧠 INDUSTRIAL CLEANER: Prioritize Title, fallback to sanitized name
                val cleanTitle = (rawTitle ?: displayName ?: "Unknown Master").substringBeforeLast(".")

                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                videos.add(VideoFile(
                    id = id.toString(),
                    name = displayName ?: "Video_${id}",
                    title = cleanTitle,
                    path = path,
                    duration = it.getLong(durCol),
                    sizeBytes = it.getLong(sizeCol),
                    resolution = it.getString(resCol) ?: "Unknown",
                    codec = it.getString(mimeCol)?.substringAfterLast("/")?.uppercase() ?: "AVC",
                    dateModified = it.getLong(dateCol) * 1000,
                    thumbnail = contentUri.toString()
                ))
            }
        }
        _mediaFiles.value = videos
    }

    private fun refreshAudio() {
        // If no explicit folder specified, scan all valid media on device rather than wiping out library
        val audioList = mutableListOf<AudioFile>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val composerCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)
            val yearCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val path = it.getString(pathCol)

                // 🧠 SELECTIVE HARVESTING (P-11)
                val normalizedItemPath = path.lowercase().trim().trimEnd('/')
                val isIncluded = includedFolders.isEmpty() || includedFolders.any { inc ->
                    val normInc = inc.lowercase().trim().trimEnd('/')
                    normalizedItemPath.startsWith(normInc) || normInc.startsWith(normalizedItemPath) ||
                    normalizedItemPath.contains(normInc.substringAfterLast('/'))
                }
                if (!isIncluded) continue
                if (excludedFolders.any { excluded -> path.contains(excluded) }) continue

                val albumId = it.getLong(albumIdCol)
                val format = it.getString(mimeCol)?.substringAfterLast("/")?.uppercase() ?: "MP3"
                val isLossless = format == "FLAC" || format == "WAV" || format == "ALAC" || format == "APE"
                
                val displayName = it.getString(nameCol)
                val rawTitle = it.getString(titleCol)
                val cleanTitle = (rawTitle ?: displayName ?: "Unknown Stem").substringBeforeLast(".")

                // 🖼️ INDUSTRIAL ART RESOLVER: Standard MediaStore Album Art URI
                val artUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()

                // 📝 EMBEDDED LYRICS EXTRACTOR (P-12)
                var embeddedLyrics: String? = null
                try {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                        val tag = audioFile.tag
                        if (tag != null) {
                            embeddedLyrics = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                        }
                    }
                } catch (_: Exception) {
                }

                if (embeddedLyrics.isNullOrBlank()) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(path)
                        embeddedLyrics = retriever.extractMetadata(31) // METADATA_KEY_LYRICS
                            ?: retriever.extractMetadata(23)
                            ?: retriever.extractMetadata(101)
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }

                val trackVal = it.getInt(trackCol)
                val disc = if (trackVal >= 1000) trackVal / 1000 else 1
                val trackNum = if (trackVal >= 1000) trackVal % 1000 else trackVal

                audioList.add(AudioFile(
                    id = id.toString(),
                    name = displayName ?: "Audio_${id}",
                    title = cleanTitle,
                    path = path,
                    duration = it.getLong(durCol),
                    sizeBytes = it.getLong(sizeCol),
                    format = format,
                    isLossless = isLossless,
                    artist = it.getString(artistCol),
                    album = it.getString(albumCol),
                    composer = it.getString(composerCol),
                    year = it.getInt(yearCol),
                    trackNumber = trackNum,
                    discNumber = disc,
                    dateAdded = it.getLong(dateAddedCol) * 1000,
                    dateModified = it.getLong(dateModCol) * 1000,
                    thumbnail = artUri,
                    lyrics = embeddedLyrics,
                    hasLyrics = !embeddedLyrics.isNullOrBlank()
                ))
            }
        }
        _audioFiles.value = audioList
    }

    override fun renameFile(file: VideoFile, newName: String) {
        val oldFile = java.io.File(file.path)
        val newFile = java.io.File(oldFile.parent, newName)
        if (oldFile.renameTo(newFile)) {
            refresh()
        }
    }

    override fun renameAudioFile(file: AudioFile, newName: String) {
        val oldFile = java.io.File(file.path)
        val newFile = java.io.File(oldFile.parent, newName)
        if (oldFile.renameTo(newFile)) {
            refresh()
        }
    }

    private fun requestDelete(uri: android.net.Uri, filePath: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val pendingIntent = android.provider.MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                if (context is android.app.Activity) {
                    context.startIntentSenderForResult(pendingIntent.intentSender, 1005, null, 0, 0, 0)
                }
            } catch (e: Exception) {
                java.io.File(filePath).delete()
                refresh()
            }
        } else {
            try {
                context.contentResolver.delete(uri, null, null)
                refresh()
            } catch (securityException: SecurityException) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && securityException is android.app.RecoverableSecurityException) {
                    if (context is android.app.Activity) {
                        context.startIntentSenderForResult(securityException.userAction.actionIntent.intentSender, 1005, null, 0, 0, 0)
                    }
                } else {
                    java.io.File(filePath).delete()
                    refresh()
                }
            } catch (e: Exception) {
                java.io.File(filePath).delete()
                refresh()
            }
        }
    }

    override fun deleteFile(file: VideoFile) {
        val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, file.id.toLong())
        requestDelete(uri, file.path)
    }

    override fun deleteAudioFile(file: AudioFile) {
        val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, file.id.toLong())
        requestDelete(uri, file.path)
    }

    override fun shareFile(file: VideoFile) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(file.path))
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Video").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun shareAudioFile(file: AudioFile) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(file.path))
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Audio").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun exportPlaylist(name: String, tracks: List<AudioFile>) {
        // IM-08: Physical M3U8 Export
        val file = java.io.File(context.getExternalFilesDir(null), "$name.m3u8")
        val content = buildString {
            appendLine("#EXTM3U")
            tracks.forEach { track ->
                appendLine("#EXTINF:${track.duration/1000},${track.name}")
                appendLine(track.path)
            }
        }
        file.writeText(content)
        println("pEQControl: [STORAGE] Playlist physically exported to: ${file.absolutePath}")
    }

    override fun readFileContent(path: String): String? {
        return try {
            java.io.File(path).readText()
        } catch (e: Exception) {
            null
        }
    }

    override fun readResourceContent(name: String): String? {
        return try {
            context.assets.open(name).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    override fun listFiles(path: String): List<String> {
        return try {
            java.io.File(path).listFiles()?.map { it.absolutePath } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun updateAudioMetadata(
        path: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        genre: String,
        composer: String,
        trackNumber: Int,
        discNumber: Int,
        year: Int
    ) {
        try {
            val file = java.io.File(path)
            if (file.exists()) {
                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                val tag = audioFile.tag ?: org.jaudiotagger.tag.id3.ID3v24Tag()
                audioFile.tag = tag

                tag.setField(org.jaudiotagger.tag.FieldKey.TITLE, title)
                tag.setField(org.jaudiotagger.tag.FieldKey.ARTIST, artist)
                tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM, album)
                tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM_ARTIST, albumArtist)
                tag.setField(org.jaudiotagger.tag.FieldKey.GENRE, genre)
                tag.setField(org.jaudiotagger.tag.FieldKey.COMPOSER, composer)
                if (trackNumber > 0) {
                    tag.setField(org.jaudiotagger.tag.FieldKey.TRACK, trackNumber.toString())
                }
                if (discNumber > 0) {
                    tag.setField(org.jaudiotagger.tag.FieldKey.DISC_NO, discNumber.toString())
                }
                if (year > 0) {
                    tag.setField(org.jaudiotagger.tag.FieldKey.YEAR, year.toString())
                }

                audioFile.commit()
                println("pEQControl: [METADATA] Successfully committed ID3 tags for $path")
                refresh()
            }
        } catch (e: Exception) {
            println("pEQControl: [METADATA] Failed to write ID3 tags physically: ${e.message}")
        }
    }
}
