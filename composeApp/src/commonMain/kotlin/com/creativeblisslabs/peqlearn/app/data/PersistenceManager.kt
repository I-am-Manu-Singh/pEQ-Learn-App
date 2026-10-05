package com.creativeblisslabs.peqlearn.app.data

/**
 * INDUSTRIAL PERSISTENCE ENGINE: pEQ VAULT
 * Cross-platform contract for storing and retrieving production metadata.
 */
interface PersistenceManager {
    var openSubtitlesApiKey: String
    fun saveString(key: String, value: String)
    fun getString(key: String, defaultValue: String? = null): String?
    fun saveLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun remove(key: String)

    fun saveSession(state: SessionState)
    fun loadSession(): SessionState?
    
    fun updateNowPlaying(item: pEQMediaItem?, isPlaying: Boolean)
    fun updatePlaybackState(isShuffle: Boolean, repeatMode: String)
    fun updateHardwareState(settings: Map<String, String>)
    fun saveFavorites(paths: List<String>)
    fun loadFavorites(): List<String>
    fun savePlaylists(playlists: Map<String, List<String>>)
    fun loadPlaylists(): Map<String, List<String>>
    fun saveScanFolders(paths: List<String>)
    fun loadScanFolders(): List<String>
    fun saveRecentSessions(paths: List<String>)
    fun loadRecentSessions(): List<String>
    
    fun saveLibraryState(currentPath: String, showFolders: Boolean)
    fun loadLibraryState(): Pair<String, Boolean>
    
    fun saveMostPlayed(playCounts: Map<String, Int>)
    fun loadMostPlayed(): Map<String, Int>
    fun saveOpenSubtitlesApiKey(key: String)
}

expect fun getPersistenceManager(): PersistenceManager
