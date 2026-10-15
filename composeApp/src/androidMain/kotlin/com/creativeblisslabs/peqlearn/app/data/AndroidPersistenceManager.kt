package com.creativeblisslabs.peqlearn.app.data

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import com.creativeblisslabs.peqlearn.app.data.SessionState
import com.creativeblisslabs.peqlearn.app.data.pEQMediaItem

private var _instance: PersistenceManager? = null

fun initPersistenceManager(context: Context) {
    _instance = AndroidPersistenceManager(context)
}

actual fun getPersistenceManager(): PersistenceManager {
    return _instance ?: throw IllegalStateException("PersistenceManager not initialized. Call initPersistenceManager() first.")
}

class AndroidPersistenceManager(private val context: Context) : PersistenceManager {
    override var openSubtitlesApiKey: String
        get() = getString("open_subtitles_api_key", "") ?: ""
        set(value) = saveString("open_subtitles_api_key", value)



    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("peq_learn_prefs", Context.MODE_PRIVATE)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    override fun saveLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun saveSession(state: SessionState) {
        try {
            val encoded = json.encodeToString(state)
            saveString("session_state", encoded)
        } catch (_: Exception) {}
    }

    override fun loadSession(): SessionState? {
        val raw = getString("session_state", null) ?: return null
        return try {
            json.decodeFromString<SessionState>(raw)
        } catch (_: Exception) {
            null
        }
    }

    override fun updateNowPlaying(item: pEQMediaItem?, isPlaying: Boolean) {
        // No-op for learn app
    }

    override fun updatePlaybackState(isShuffle: Boolean, repeatMode: String) {
        prefs.edit()
            .putBoolean("is_shuffle", isShuffle)
            .putString("repeat_mode", repeatMode)
            .apply()
    }

    override fun updateHardwareState(settings: Map<String, String>) {
        try {
            saveString("hardware_state", json.encodeToString(settings))
        } catch (_: Exception) {}
    }

    override fun saveFavorites(paths: List<String>) {
        try {
            saveString("favorites", json.encodeToString(paths))
        } catch (_: Exception) {}
    }

    override fun loadFavorites(): List<String> {
        val raw = getString("favorites", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun savePlaylists(playlists: Map<String, List<String>>) {
        try {
            saveString("playlists", json.encodeToString(playlists))
        } catch (_: Exception) {}
    }

    override fun loadPlaylists(): Map<String, List<String>> {
        val raw = getString("playlists", null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, List<String>>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun saveScanFolders(paths: List<String>) {
        try {
            saveString("scan_folders", json.encodeToString(paths))
        } catch (_: Exception) {}
    }

    override fun loadScanFolders(): List<String> {
        val raw = getString("scan_folders", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveRecentSessions(paths: List<String>) {
        try {
            saveString("recent_sessions", json.encodeToString(paths))
        } catch (_: Exception) {}
    }

    override fun loadRecentSessions(): List<String> {
        val raw = getString("recent_sessions", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveLibraryState(currentPath: String, showFolders: Boolean) {
        prefs.edit()
            .putString("lib_current_path", currentPath)
            .putBoolean("lib_show_folders", showFolders)
            .apply()
    }

    override fun loadLibraryState(): Pair<String, Boolean> {
        val path = prefs.getString("lib_current_path", "") ?: ""
        val show = prefs.getBoolean("lib_show_folders", false)
        return Pair(path, show)
    }

    override fun saveMostPlayed(playCounts: Map<String, Int>) {
        try {
            saveString("most_played", json.encodeToString(playCounts))
        } catch (_: Exception) {}
    }

    override fun loadMostPlayed(): Map<String, Int> {
        val raw = getString("most_played", null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, Int>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun saveTheme(theme: String) {
        saveString("selected_theme", theme)
    }

    override fun loadTheme(): String {
        return getString("selected_theme", "DARK") ?: "DARK"
    }

    override fun saveAccent(accent: String) {
        saveString("selected_accent", accent)
    }

    override fun loadAccent(): String {
        return getString("selected_accent", "AMBER") ?: "AMBER"
    }

    fun clearCache(activePath: String?) {
        try {
            context.cacheDir.deleteRecursively()
        } catch (_: Exception) {}
    }

    fun readBytes(path: String): ByteArray? {
        return try {
            File(path).readBytes()
        } catch (_: Exception) {
            null
        }
    }

    fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return try {
            File(path).writeBytes(bytes)
            true
        } catch (_: Exception) {
            false
        }
    }
}
