package com.creativeblisslabs.peqlearn.app.data

import kotlinx.coroutines.flow.StateFlow

/**
 * INDUSTRIAL FILE SCANNER: STORAGE AUDITOR The primary engine for discovering and monitoring
 * production media.
 */
interface pEQFileScanner {
    val mediaFiles: StateFlow<List<VideoFile>>
    val audioFiles: StateFlow<List<AudioFile>>
    val scanProgress: StateFlow<Float>
    val scanStatus: StateFlow<String>

    fun scan(includePaths: List<String>, excludePaths: List<String>)
    fun setExcludedFolders(folders: List<String>)
    fun setIncludedFolders(folders: List<String>)
    fun scanNetworkDrives(host: String)
    fun refresh()
    fun renameFile(file: VideoFile, newName: String)
    fun renameAudioFile(file: AudioFile, newName: String)
    fun deleteFile(file: VideoFile)
    fun deleteAudioFile(file: AudioFile)
    fun shareFile(file: VideoFile)
    fun shareAudioFile(file: AudioFile)
    fun exportPlaylist(name: String, tracks: List<AudioFile>)
    fun readFileContent(path: String): String?
    fun readResourceContent(name: String): String?
    fun listFiles(path: String): List<String>
    fun updateAudioMetadata(path: String, title: String, artist: String, album: String, albumArtist: String, genre: String, composer: String, trackNumber: Int, discNumber: Int, year: Int)
    // Multi-Volume Case-Insensitive Path Matching helper
    fun isPathIncluded(filePath: String, includedFolders: List<String>): Boolean {
        if (includedFolders.isEmpty()) return true
        val normalizedItemPath = filePath.replace("\\", "/").trim().lowercase()
        return includedFolders.any { inc ->
            val normInc = inc.replace("\\", "/").trim().lowercase()
            normalizedItemPath.startsWith(normInc)
        }
    }

}
