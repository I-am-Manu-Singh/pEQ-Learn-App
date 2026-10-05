package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import kotlin.math.*

import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.*
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock
import com.creativeblisslabs.peqlearn.app.Tab
import com.creativeblisslabs.peqlearn.app.data.pEQMediaItem
import com.creativeblisslabs.peqlearn.app.ai.pEQCloudDownloader
import com.creativeblisslabs.peqlearn.app.ai.pEQStemSplitter
import com.creativeblisslabs.peqlearn.app.audio.AudioEngine
import kotlinx.coroutines.*
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.observeLibraryState
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.refreshLibrary
import com.creativeblisslabs.peqlearn.app.data.Device
import com.creativeblisslabs.peqlearn.app.data.SyncedLine
import com.creativeblisslabs.peqlearn.app.data.lyrics.LyricsRepository
import com.creativeblisslabs.peqlearn.app.data.repository.*
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.components.getSystemVolume
import com.creativeblisslabs.peqlearn.app.ui.components.setSystemVolume
import com.creativeblisslabs.peqlearn.app.network.createHttpClient
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQTheme
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull

open class AppViewModel(

    val audioEngine: AudioEngine,

    val persistenceManager: PersistenceManager,

    val stemSplitter: pEQStemSplitter? = null,
    val cloudDownloader: pEQCloudDownloader? = null,
    private val ragaRepository: RagaRepository? = null

) : pEQViewModel(){


    // =========================
// VIEW MODE ENUM
// PLACE INSIDE AppViewModel
// =========================

    enum class LibraryViewMode {

        LIBRARY,
        FILES,
        ARTISTS,
        ALBUMS
    }

    enum class VisualizerMode {
        BARS,
        CIRCULAR,
        RTA,
        WAVEFORM
    }
    val snackbarFlow =

    MutableSharedFlow<String>()
    var visualizerMode = mutableStateOf(VisualizerMode.BARS)

    var libraryViewMode =
        mutableStateOf(
            LibraryViewMode.LIBRARY
        )
val isLandscapeMode =
    mutableStateOf(true)
    var libraryMediaTypeFilter =
        mutableStateOf<MediaType?>(
            null
        )
    val isSeeking =
        mutableStateOf(false)
    companion object {
        var splashHasShown = false
    }

    var splashCompleted by mutableStateOf(splashHasShown)
        private set

    fun completeSplash() {
        splashCompleted = true
        splashHasShown = true
    }
    
val client = createHttpClient()
    private var metadataLoadJob: Job? = null

    private var hasInitialized = false
    // 🎬 Playback Core
    val isPlaying = mutableStateOf(false)
    val isInPipMode = mutableStateOf(false)
    val isLockScreenMode = mutableStateOf(false)
    val isExternalLaunch = mutableStateOf(false)
    
    var onEnterPiP: (() -> Unit)? = null
    
    val playbackSpeed = mutableStateOf(1.0f)
    val peakLevelDb = mutableStateOf(-60f)
    val toastMessage = mutableStateOf<String?>(null)
    
    var onPickFolderRequested: (() -> Unit)? = null
    var onPickLyricsRequested: (() -> Unit)? = null
    var onPickSubtitleRequested: (() -> Unit)? = null
    var onPickTrackRequested: (((pEQMediaItem) -> Unit) -> Unit)? = null
    var onPickScoreFileRequested: (() -> Unit)? = null
    var onRestoreSamplesRequested: (() -> Unit)? = null
    val currentPosition = mutableLongStateOf(0L)
    val currentDuration = mutableStateOf(1L)
    val playbackQueue = mutableStateListOf<pEQMediaItem>()
    val currentMediaItem = mutableStateOf<pEQMediaItem?>(null)
    
    val currentTab = mutableStateOf(Tab.Dashboard)
    val dashboardSubTab = mutableStateOf(0)
    val selectedWorkspaceTab = mutableStateOf("CHORDS")
    val selectedOutputDevice = mutableStateOf(com.creativeblisslabs.peqlearn.app.data.Device.INTERNAL)
    val isVideoFullScreen = mutableStateOf(false)
    val isLandscapeLocked = mutableStateOf(false)
    val masterVolume = mutableStateOf(1f)
    val customAccentColor = mutableStateOf<Color?>(null)
    val isVocalRemovalActive = mutableStateOf(false)
    val isBuffering = mutableStateOf(false)
    val isPiPMode = mutableStateOf(false)
    val showMiniPlayer = mutableStateOf(false)
    val isFullscreenKeyboard = mutableStateOf(false)
    val practicingChord = mutableStateOf<String?>(null)
    val isTanpuraActive = mutableStateOf(false)

    internal val _currentQueueIndex = mutableStateOf(-1)
    val currentQueueIndex: State<Int> = _currentQueueIndex             

    internal fun setCurrentQueueIndex(index: Int) {
        _currentQueueIndex.value = index
    }

    private val _isShuffleEnabled = mutableStateOf(false)
    var isShuffleEnabled: Boolean
        get() = _isShuffleEnabled.value
        set(value) {
            _isShuffleEnabled.value = value
            audioEngine.setShuffleMode(value)
        }

    fun setShuffleEnabledState(enabled: Boolean) {
        _isShuffleEnabled.value = enabled
    }

    fun setRepeatModeState(mode: RepeatMode) {
        repeatMode.value = mode
    }

    val isFavorite = mutableStateOf(false)
    val repeatMode = mutableStateOf(RepeatMode.OFF)

    // ⏰ Industrial Utilities
    val sleepTimerActive = mutableStateOf(false)
    val sleepTimerMinutes = mutableStateOf(30)
    val sleepTimerRemainingSec = mutableLongStateOf(0L)
    
    val isCasting = mutableStateOf(false)
   
    val audioDelayMs = mutableLongStateOf(0L)
    val openAtStartup = mutableStateOf(false)
    val showSubtitleConsole = mutableStateOf(false)
    val showCutter = mutableStateOf(false)
    val subtitleSettings = mutableStateOf(SubtitleSettings())
    val osApiKey = mutableStateOf(persistenceManager.openSubtitlesApiKey)

    val abRepeatActive = mutableStateOf(false)
    // Initialize with -1L instead of null
    val abRepeatPointA = mutableStateOf(-1L)
    val abRepeatPointB = mutableStateOf(-1L)
    val currentFolder = mutableStateOf<String?>(null)
    
    val showBrowser = mutableStateOf(false)
    val browserUrl = mutableStateOf("")
    val browserTitle = mutableStateOf("pEQ BROWSER")

    val selectedAutoEqProfile = mutableStateOf<AutoEqProfile?>(null)
    val autoEqProfiles = mutableStateListOf<AutoEqProfile>()
    val isAiUpscalingEnabled = mutableStateOf(false)
    val isCrossfading = mutableStateOf(false)
    val crossfadeProgress = mutableStateOf(0f)
    internal var activeCrossfadeJob: Job? = null

    private val osClient by lazy { com.creativeblisslabs.peqlearn.app.ai.OpenSubtitlesClient(persistenceManager.openSubtitlesApiKey) }

    internal val lyricsRepository = com.creativeblisslabs.peqlearn.app.data.lyrics.LyricsRepository()
    internal val subtitleRepository = com.creativeblisslabs.peqlearn.app.data.repository.SubtitleRepository { osApiKey.value }


    val eqGains = mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) // 10 Bands
    val eqQs = mutableStateListOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
    val convolverIrPath = mutableStateOf<String?>(null)
    val favorites = mutableStateListOf<String>()
    // 📝 Lyrics & Temporal Metadata
    val syncedLyrics = mutableStateListOf<SyncedLine>()
    val syncedSubtitles = mutableStateListOf<SyncedLine>()
    val currentChapters = mutableStateListOf<MediaChapter>()
    val isLyricsVisible = mutableStateOf(false)
    val isChaptersVisible = mutableStateOf(false)
    val isEditingLyrics = mutableStateOf(false)
    val isEditingChapters = mutableStateOf(false)
    val isEditingSubtitles = mutableStateOf(false)
    val isMoodMakerVisible = mutableStateOf(false)
    val selectedItems = mutableStateListOf<pEQMediaItem>()
    

    val lyricsAvailable = derivedStateOf { syncedLyrics.isNotEmpty() }
    val chaptersAvailable = derivedStateOf { currentChapters.isNotEmpty() }
    val isLyricsWebViewVisible = mutableStateOf(false)
    val lyricsSearchUrl = mutableStateOf("")
    
    // 🔍 AutoEQ & Parametric Engine
    val autoEqSearchResults = mutableStateListOf<AutoEqProfile>()
    val isAutoEqLoading = mutableStateOf(false)
    
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    internal val scanFolders = mutableStateListOf<String>()
    /** Platform-specific primary external storage root (e.g. /storage/emulated/999 on some tablets). Set by Android before start(). */
    var primaryStoragePath: String = "/storage/emulated/0"
    val isHapticsEnabled = mutableStateOf(true)
    
    // 🌀 Convolution & Dynamics Engine
    val convolverIrName = mutableStateOf("Reference Room (Default)")
    val convolverIrDryWet = mutableFloatStateOf(0.3f)
    
    val isConvolverActive = mutableStateOf(false)
    val compressorThreshold = mutableFloatStateOf(-20f)
    val compressorRatio = mutableStateOf(4f)
    val compressorAttackMs = mutableFloatStateOf(10f)
    val compressorReleaseMs = mutableFloatStateOf(100f)
    val limiterEnabled = mutableStateOf(true)
    val isSubHarmonicActive = mutableStateOf(false)
    
    // 📊 Signal Telemetry (Pre/Post DSP)
    val preDspLevel = mutableFloatStateOf(0f)
    val dspProcessingLoad = mutableStateOf(0f)
    val isAutoScanEnabled = mutableStateOf(true)
    val isNeuralSaturationEnabled = mutableStateOf(false)
    val detectedPitch = mutableFloatStateOf(0f)
    val detectedNote = mutableStateOf("-")
    val centsDeviation = mutableIntStateOf(0)
    
    val isGaplessEnabled = mutableStateOf(true)
    val isBackgroundServiceRunning = mutableStateOf(true)
    val isBitPerfectEnabled = mutableStateOf(false)
    val phaseInverted = mutableStateOf(false)
    val isDynamicColorsEnabled = mutableStateOf(true)
    val bufferPriority = mutableStateOf("High")
    val channelBalance = mutableFloatStateOf(0f)
    val isNeuralEnvActive = mutableStateOf(false)
    val activeJob = mutableStateOf<SplitJob?>(null)
    val backgroundJobs = mutableStateListOf<SplitJob>()
    private var lastFftTime = 0L

    fun start() {
        if (hasInitialized) return
        hasInitialized = true
        startHardwarePolling()
        restoreSession()
        
        // Natural decay loop — zeros out RTA bars when paused/stopped, no fake data
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(80)
                if (!isPlaying.value) {
                    val current = rtaData.value
                    if (current.any { it > 0f }) {
                        rtaData.value = FloatArray(current.size) { i ->
                            (current[i] * 0.65f).let { if (it < 0.005f) 0f else it }
                        }
                        val peak = rtaData.value.maxOrNull() ?: 0f
                        if (peak > 0f) {
                            peakLevelDb.value = (20 * kotlin.math.log10(peak.toDouble().coerceAtLeast(1e-6))).toFloat()
                            preDspLevel.value = peak
                        } else {
                            peakLevelDb.value = -60f
                            preDspLevel.value = 0f
                            detectedPitch.value = 0f
                            detectedNote.value = ""
                        }
                    }
                }
            }
        }
        
        audioEngine.onFftDataReceived = { fft ->
            lastFftTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            scope.launch(Dispatchers.Main) {
                // Smooth the FFT data for professional RTA look
                val smoothedFft = FloatArray(fft.size) { i ->
                    val prev = rtaData.value.getOrElse(i) { 0f }
                    fft[i] * 0.7f + prev * 0.3f
                }
                rtaData.value = smoothedFft
                
                // Update peak level for Hardware screen
                val peak = fft.maxOrNull() ?: 0f
                peakLevelDb.value = (20 * log10(peak.toDouble().coerceAtLeast(1e-6))).toFloat()
                preDspLevel.value = peak

                // 🎹 Professional Harmonic Analysis (Learn Screen)
                val peakBin = smoothedFft.indices.maxByOrNull { smoothedFft[it] } ?: 0
                val peakMagnitude = smoothedFft[peakBin]
                val currentId = currentMediaItem.value?.id ?: ""
                
                if (peakMagnitude > 0.05f) {
                    // Nyquist / numBins * peakBin
                    val freq = (peakBin * (44100f / (fft.size * 2))).coerceAtLeast(20f)
                    val noteIdx = (12 * (log2(freq / 440.0) + 4.0)).toInt() % 12
                    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
                    val note = notes[noteIdx.coerceIn(0, 11)]
                    
                    detectedNote.value = note
                    detectedPitch.value = freq
                    centsDeviation.value = ((1200 * log2(freq / 440.0)) % 100).toInt()
                }
            }
        }

        // 🎼 SET UP ENGINE CALLBACKS
        audioEngine.onTrackEnded = {
            scope.launch {
                when (repeatMode.value) {
                    RepeatMode.ONE -> {
                        val current = currentMediaItem.value
                        if (current != null) {
                            playMediaItem(current, addToQueue = false)
                        }
                    }
                    RepeatMode.ALL, RepeatMode.FOLDER -> {
                        nextTrack()
                    }
                    else -> {
                        if (currentQueueIndex.value < playbackQueue.size - 1) {
                            nextTrack()
                        } else {
                            isPlaying.value = false
                        }
                    }
                }
            }
        }
        
        audioEngine.onTrackChanged = {
            syncFromEngineTrack()
            onTrackLoaded()
        }

        audioEngine.onBufferingChanged = { buffering ->
            isBuffering.value = buffering
        }

        // 🔄 Sync Playback State
        scope.launch {
            while(true) {
                val actualPlaying = audioEngine.isPlaying()
                if (isPlaying.value != actualPlaying) {
                    isPlaying.value = actualPlaying
                    syncNowPlaying()
                }
                
                val pos = audioEngine.getCurrentPosition()
                val rawDuration = audioEngine.getDuration()
                if (!isSeeking.value) {
                    currentPosition.longValue = pos
                    val validDuration = if (rawDuration > 0 && rawDuration != -9223372036854775807L) rawDuration else 1L
                    currentDuration.value = validDuration
                }
                
                if (actualPlaying && !isSeeking.value) {
                    // 💓 Neural Heartbeat: Jitter data slightly for "Live" feel if hardware is idle
                    if (detectedPitch.value == 440f || detectedPitch.value == 0f) {
                         detectedPitch.value = 440f + (sin(Clock.System.now().toEpochMilliseconds() / 500.0) * 2f).toFloat()
                    }

                    // 🔁 INDUSTRIAL LOOP ENGINE (A-B Repeat)
                    if (abRepeatActive.value && abRepeatPointA.value != -1L && abRepeatPointB.value != -1L) {
                        if (pos >= abRepeatPointB.value) {
                            seekTo(abRepeatPointA.value)
                        }
                    }
                }
                delay(100)
            }
        }
        
        observeScanner()
    }
    
    internal var lastBeatMs = 0L
    val bufferSizeFrames = mutableIntStateOf(1024)
    val hapticIntensity = mutableFloatStateOf(0.5f)
    val platformStorageBase = mutableStateOf("/storage/emulated/0")

    // 📺 Track Management
    val availableSubtitleTracks = mutableStateListOf<MediaTrack>()
    val availableAudioTracks = mutableStateListOf<MediaTrack>()
    val selectedSubtitleTrack = mutableStateOf<MediaTrack?>(null)
    val selectedAudioTrack = mutableStateOf<MediaTrack?>(null)
    val showAudioTrackConsole = mutableStateOf(false)
    val showEqualizer = mutableStateOf(false)
    val isFetchingEverything = mutableStateOf(false)
    val isFetchingSubtitles = mutableStateOf(false)
    val isFocusMode = mutableStateOf(false)
    val isFloatingLyricsActive = mutableStateOf(false)
    val isAppInBackground = mutableStateOf(false)
    

    
        // 🥁 BEAT CREATION ENGINE
    internal var persistenceCounter = 0
    
    internal var _scanner: pEQFileScanner? = null
    var scanner: pEQFileScanner?
        get() = _scanner
        set(value) {
            _scanner = value
            value?.let { 
                observeScanner()
                loadNeuralAssets()
                if (scanFolders.isNotEmpty()) {
                    refreshLibrary()
                }
            }
        }
    
    private fun loadNeuralAssets() {
        scope.launch {
            try {
                val autoeqJson = scanner?.readResourceContent("autoeq_database.json") ?: "[]"
                val profiles = Json.decodeFromString<List<AutoEqProfile>>(autoeqJson)
                autoEqProfiles.clear()
                autoEqProfiles.addAll(profiles)
            } catch (e: Exception) {
                println("pEQControl: [ENGINE] Asset Loading Failure -> ${e.message}")
            }
            loadAssets()
        }
    }

    internal val tapTimes = mutableListOf<Long>()


    // 📊 UNIFIED MEDIA LIBRARY
    val searchQuery = mutableStateOf("")

    internal val _mediaLibrary = MutableStateFlow<List<pEQMediaItem>>(emptyList())
    val mediaLibrary: StateFlow<List<pEQMediaItem>> = _mediaLibrary.combine(snapshotFlow { searchQuery.value }) { items, query ->
        if (query.isBlank()) items
        else items.filter { (it.title as CharSequence).contains(query, ignoreCase = true) || (it.artist as CharSequence?)?.contains(query, ignoreCase = true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 📁 Industrial Library Engine
    val playlists: Map<String, List<pEQMediaItem>> get() = customPlaylists
    val favoriteItems = mutableStateListOf<pEQMediaItem>()
    val playCounts = mutableStateMapOf<String, Int>()
    val lastPlayed = mutableStateMapOf<String, Long>()
    val mostPlayedItems = mutableStateListOf<pEQMediaItem>()
    val recentlyPlayedItems = mutableStateListOf<pEQMediaItem>()
    val mediaByAlbum = mutableStateMapOf<String, List<pEQMediaItem>>()
    val mediaByArtist = mutableStateMapOf<String, List<pEQMediaItem>>()
    val mediaByFormat = mutableStateMapOf<String, List<pEQMediaItem>>()
    val smartPlaylists = mutableStateMapOf<String, List<pEQMediaItem>>()

    val selectedLibraryTab = mutableStateOf("Media")
    val isScanning = mutableStateOf(false)
    val showFolders = mutableStateOf(false)

    // 🎬 Playback Core
    
    // 📁 Archive Management State
    val currentFolderPath = mutableStateOf("")
    val folderHistory = mutableStateListOf<String>()
    val folderFuture = mutableStateListOf<String>()
    
    
    // 🎥 Professional Media State
    val isVideoLocked = mutableStateOf(false)
    val videoZoom = mutableStateOf(1.0f)
    val videoAspectRatio = mutableStateOf("FIT")
    
    // ✂️ Cutter State
    val trimStartMs = mutableLongStateOf(0L)
    val trimEndMs = mutableLongStateOf(0L)

    // 🧠 AI & Sync Engine
    val currentLyricsLine = mutableStateOf(0)
    val rtaData = mutableStateOf(FloatArray(32))
    val exportFormat = mutableStateOf("WAV")
    val exportSampleRate = mutableStateOf(48000)
    val isDitheringEnabled = mutableStateOf(true)
    val stereoWidth = mutableStateOf(1.0f)

    // ⚙️ Workstation System Hooks
    var workstationRegistry = mutableStateMapOf<String, String>()
    val hardwareSettings get() = workstationRegistry
    val currentDevice = mutableStateOf(Device.INTERNAL)
    val isStemSplitting = mutableStateOf(false)
    val isMasterBypassActive = mutableStateOf(false)
    val showDiagnostics = mutableStateOf(false)
    val showTrackInfoSheet = mutableStateOf(false)
    val selectedItemForInfo = mutableStateOf<pEQMediaItem?>(null)
    val isEditingMetadata = mutableStateOf(false)
    val connectedDevices = mutableStateListOf<Device>(Device.INTERNAL) 
    val currentBluetoothProfile = mutableStateOf(com.creativeblisslabs.peqlearn.app.audio.BluetoothProfile.LDAC)
    val bluetoothResumeEnabled = mutableStateOf(true)

    // 📂 PLYLIST ENGINE (P-12)
    val customPlaylists = mutableStateMapOf<String, SnapshotStateList<pEQMediaItem>>()
    val selectedItemForPlaylist = mutableStateOf<pEQMediaItem?>(null)
    val isPlaylistBulkAction = mutableStateOf(false)
    val selectedFolderForPlaylist = mutableStateOf<String?>(null)
    




    // 🔍 INTEGRATED SEARCH ENGINE
    val globalSearchQuery = mutableStateOf("")
    val searchResults: StateFlow<List<pEQMediaItem>> = snapshotFlow { globalSearchQuery.value }
        .combine(mediaLibrary) { query, library ->
            if (query.isEmpty()) emptyList()
            else library.filter { it.title.contains(query, true) || it.artist?.contains(query, true) == true }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    
    val selectedPreset = mutableStateOf(Presets.Flat)
    
    val showSettings = mutableStateOf(false)
    val isKidsLockActive = mutableStateOf(false)
    val showLyrics = mutableStateOf(true)
    val dominantColor =
        mutableStateOf(Color(0xFF121212))
    val ambientIntensity = mutableStateOf(0.12f)
    val transientImpact = mutableStateOf(0f)
    val showQueueDrawer = mutableStateOf(false)
    val showFolderManager = mutableStateOf(false)

    val showSleepTimerSheet = mutableStateOf(false)
    private var sleepTimerJob: Job? = null

    internal fun setSleepTimerJob(job: Job) {
        sleepTimerJob = job
    }
    internal fun cancelSleepTimerJob() {
        sleepTimerJob?.cancel()
    }
    val isPlaylistPickerShown = mutableStateOf(false)
    val isMicMonitoringEnabled = mutableStateOf(false)
    val isSubtitlesEnabled = mutableStateOf(false)
    val isPipActive = mutableStateOf(false)
    val isReplayGainEnabled = mutableStateOf(false)
    val isForceHiResEnabled = mutableStateOf(false)

    val isEQInteracting = mutableStateOf(false)

    // val currentChapters = mutableStateListOf<com.creativeblisslabs.peqlearn.app.data.MediaChapter>()
    val availableTracks = mutableStateListOf<com.creativeblisslabs.peqlearn.app.data.MediaTrack>()
    var videoRotationDegrees = mutableStateOf(0)
    var videoWidth = mutableStateOf(0)
    var videoHeight = mutableStateOf(0)
    val isRotationLocked = mutableStateOf(false)
    val youtubeSearchQuery = mutableStateOf("")
    val youtubeSearchResults = mutableStateListOf<YoutubeResult>()
    val availableFormats = mutableStateListOf<com.creativeblisslabs.peqlearn.app.ai.FormatInfo>()
    val isFetchingFormats = mutableStateOf(false)
    val sharedUrl = mutableStateOf("")
    val scanProgress = mutableStateOf(0f)
    val scanStatus = mutableStateOf("")
    val videoBrightness = mutableStateOf(0.5f)
    val isGestureInteracting = mutableStateOf(false)
    val gestureSeekDelta = mutableLongStateOf(0L)
    internal var lastVolume = -1f

    // 🔌 Hardware Switching Hub
    val showDeviceSwitcher = mutableStateOf(false)
    val pendingDevice = mutableStateOf<Device?>(null)

    val isBackgroundPlayEnabled = mutableStateOf(true)
    val crossfadeSeconds = mutableStateOf(0f)
    val eqFrequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    // 📊 Library Management (P-14)
    val sortBy = mutableStateOf("NAME") // NAME, SIZE, DATE
    val filterType = mutableStateOf<MediaType?>(null)
    
    // 🎧 Professional Audio Telemetry
    val currentBitrate = mutableStateOf(0)
    val currentCodec = mutableStateOf("PCM")
    val currentSampleRate = mutableStateOf(44100)
    val currentBitDepth = mutableStateOf(16)

    val neuralProgress = mutableStateOf(0f)


    val stepSequencer = mutableStateMapOf<Int, MutableList<String>>()

    
    val diagnosticLog = mutableStateListOf<String>()
    val isDiagnosticRunning = mutableStateOf(false)
    
    private val _selectedTheme = mutableStateOf(pEQTheme.DARK)
    val selectedTheme: State<pEQTheme> = _selectedTheme
    internal fun setTheme(theme: pEQTheme) { _selectedTheme.value = theme }

    private val _selectedAccent = mutableStateOf(pEQAccent.AMBER)
    val selectedAccent: State<pEQAccent> = _selectedAccent
    internal fun setAccent(accent: pEQAccent) { _selectedAccent.value = accent }
    
    internal fun enqueueJob(job: SplitJob) {
        backgroundJobs.add(job)
        activeJob.value = job
    }

    internal fun updateJob(id: String, progress: Float, status: String) {
        val index = backgroundJobs.indexOfFirst { it.id == id }
        if (index != -1) {
            backgroundJobs[index] = backgroundJobs[index].copy(progress = progress, status = status)
            if (activeJob.value?.id == id) {
                activeJob.value = backgroundJobs[index]
            }
        }
    }

    internal fun removeJob(id: String) {
        backgroundJobs.removeAll { it.id == id }
        if (activeJob.value?.id == id) {
            activeJob.value = backgroundJobs.lastOrNull()
        }
    }

    init {
        scope.launch {
            snapshotFlow { masterVolume.value }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setVolume(it)
                }
        }
        
        observeScanner()
        
        // 🚀 Automatic Metadata & Lyric Fetching
        scope.launch {
            snapshotFlow { currentMediaItem.value }.distinctUntilChanged().collect { item ->
                if (item != null) {
                    onTrackLoaded()
                    syncNowPlaying()
                }
            }
        }

        // 🔄 Sync Playback State to Widget Immediately
        scope.launch {
            snapshotFlow { isPlaying.value }.distinctUntilChanged().collect {
                syncNowPlaying()
            }
        }

        // 🔄 Sync Shuffle & Repeat State to Widget Immediately
        scope.launch {
            snapshotFlow { isShuffleEnabled }.distinctUntilChanged().collect { isShuffle ->
                persistenceManager.updatePlaybackState(isShuffle, repeatMode.value.name)
            }
        }
        scope.launch {
            snapshotFlow { repeatMode.value }.distinctUntilChanged().collect { mode ->
                persistenceManager.updatePlaybackState(isShuffleEnabled, mode.name)
            }
        }
        scope.launch {
            snapshotFlow {
                val currentPath = currentMediaItem.value?.path
                if (currentPath != null) {
                    favoriteItems.any { it.path == currentPath }
                } else {
                    false
                }
            }.distinctUntilChanged().collect {
                syncNowPlaying()
            }
        }

        start()
    }

    fun syncFromEngineTrack() {
        val currentUri = audioEngine.getCurrentTrackUri() ?: return
        val normalizedCurrent = normalizePath(currentUri)
        val matchedIndex = playbackQueue.indexOfFirst { item ->
            normalizePath(item.path) == normalizedCurrent
        }
        if (matchedIndex != -1) {
            val matchedItem = playbackQueue[matchedIndex]
            if (currentQueueIndex.value != matchedIndex) {
                setCurrentQueueIndex(matchedIndex)
            }
            if (currentMediaItem.value?.path != matchedItem.path) {
                currentMediaItem.value = matchedItem
            }
        }
    }

    // ========================================================================
    // EXPANDED VIEWMODEL EXTENSION PROPERTIES & SYNTHETIC SOUND METHODS
    // ========================================================================
    val videoExportFormat = mutableStateOf("MP4")
    val selectedKeySignature = mutableStateOf("C Major")
    val selectedTimeSignature = mutableStateOf("4/4")
    val scrollDirectionHorizontal = mutableStateOf(true)
    val shiftNotesOffset = mutableStateOf(0)
    val videoExportEffects = mutableStateMapOf<String, Boolean>("Glow" to true, "Particles" to true)

    // Interactive note playing state
    val midiPlayingNote = mutableStateOf<Int?>(null)

    // System Settings & SoundFont / Instrument Customization
    val selectedSettingsTab = mutableIntStateOf(0)
    val useVexflow = mutableStateOf(false)
    val pitchEngineAlgo = mutableStateOf("Phase Vocoder")
    val stemSplitQuality = mutableStateOf("Standard")

    // Piano SoundFont
    val pianoSf2File = mutableStateOf("piano/198_Yamaha_SY1_piano.sf2")
    val pianoBank = mutableIntStateOf(0)
    val pianoPreset = mutableIntStateOf(0)
    val pianoChannel = mutableIntStateOf(0)
    val pianoVelocity = mutableIntStateOf(95)

    // Guitar SoundFont
    val guitarSf2File = mutableStateOf("guitar/Miniis_electric_guitar.sf2")
    val guitarBank = mutableIntStateOf(0)
    val guitarPreset = mutableIntStateOf(0)
    val guitarChannel = mutableIntStateOf(0)
    val guitarVelocity = mutableIntStateOf(72)

    // Drum SoundFont
    val drumSf2File = mutableStateOf("drums/Metal_Drums.sf2")
    val drumBank = mutableIntStateOf(0)
    val drumPreset = mutableIntStateOf(0)
    val drumChannel = mutableIntStateOf(9)
    val drumVelocity = mutableIntStateOf(90)
    val useSf2ForDrums = mutableStateOf(false)

    // Metronome SoundFont
    val metronomeSf2File = mutableStateOf("metronome/Metronom.sf2")
    val metronomeBank = mutableIntStateOf(0)
    val metronomePreset = mutableIntStateOf(0)
    val metronomeChannel = mutableIntStateOf(10)
    val metronomeVelocity = mutableIntStateOf(100)
    val useSf2ForMetronome = mutableStateOf(false)

    // Tanpura SoundFont
    val tanpuraSf2File = mutableStateOf("tanpura/Tanpura_drone.sf2")
    val tanpuraBank = mutableIntStateOf(0)
    val tanpuraPreset = mutableIntStateOf(0)
    val tanpuraChannel = mutableIntStateOf(2)
    val tanpuraVelocity = mutableIntStateOf(58)
    val useSf2ForTanpura = mutableStateOf(false)

    // Tabla SoundFont
    val tablaSf2File = mutableStateOf("tabla/Indian_Ensemble.sf2")
    val tablaBank = mutableIntStateOf(0)
    val tablaPreset = mutableIntStateOf(0)
    val tablaChannel = mutableIntStateOf(9)
    val tablaVelocity = mutableIntStateOf(74)

    // Learn & Creative Studio
    val tunerReferenceFrequency = mutableFloatStateOf(440f)
    val metronomeTickSound = mutableStateOf("Woodblock")
    val metronomeSubdivision = mutableIntStateOf(1)
    val fallingNotesSpeed = mutableStateOf("Normal")
    val waterfallTheme = mutableStateOf("Neon Glow")
    val showPianoKeyLabels = mutableStateOf(true)
    val showWaterfallParticles = mutableStateOf(true)

    // Advanced Creative Export Studio
    val videoExportTheme = mutableStateOf("Cyberpunk")
    val videoExportResolution = mutableStateOf("1080p")
    val videoExportFps = mutableStateOf("60 FPS")
    val videoExportBitrate = mutableStateOf("10 Mbps")
    val videoExportAudioCodec = mutableStateOf("AAC")
    val videoExportSampleRate = mutableStateOf("48 kHz")
    val videoExportCameraYaw = mutableFloatStateOf(0f)
    val videoExportCameraPitch = mutableFloatStateOf(15f)
    val videoExportEqBars = mutableIntStateOf(64)
    val videoExportPeakSensitivity = mutableFloatStateOf(80f)
    val isVideoExporting = mutableStateOf(false)
    val videoExportProgress = mutableFloatStateOf(0f)

    fun syncSoundFontPreferences() {
        audioEngine.updateSoundFontPreferences(
            pianoSf2 = pianoSf2File.value,
            pianoBank = pianoBank.intValue,
            pianoPreset = pianoPreset.intValue,
            pianoChannel = pianoChannel.intValue,
            pianoVelocity = pianoVelocity.intValue,
            guitarSf2 = guitarSf2File.value,
            guitarBank = guitarBank.intValue,
            guitarPreset = guitarPreset.intValue,
            guitarChannel = guitarChannel.intValue,
            guitarVelocity = guitarVelocity.intValue,
            tanpuraSf2 = tanpuraSf2File.value,
            tanpuraBank = tanpuraBank.intValue,
            tanpuraPreset = tanpuraPreset.intValue,
            tanpuraChannel = tanpuraChannel.intValue,
            tanpuraVelocity = tanpuraVelocity.intValue,
            drumSf2 = drumSf2File.value,
            drumBank = drumBank.intValue,
            drumPreset = drumPreset.intValue,
            drumChannel = drumChannel.intValue,
            drumVelocity = drumVelocity.intValue,
            metronomeSf2 = metronomeSf2File.value,
            metronomeBank = metronomeBank.intValue,
            metronomePreset = metronomePreset.intValue,
            metronomeChannel = metronomeChannel.intValue,
            metronomeVelocity = metronomeVelocity.intValue,
            tablaSf2 = tablaSf2File.value,
            tablaBank = tablaBank.intValue,
            tablaPreset = tablaPreset.intValue,
            tablaChannel = tablaChannel.intValue,
            tablaVelocity = tablaVelocity.intValue,
            useSf2ForDrums = useSf2ForDrums.value,
            useSf2ForMetronome = useSf2ForMetronome.value,
            useSf2ForTanpura = useSf2ForTanpura.value
        )
    }

    fun listAssetSoundfonts(category: String): List<String> {
        val fromEngine = audioEngine.listAssetSoundfonts(category)
        if (fromEngine.isNotEmpty()) return fromEngine
        return when (category.lowercase()) {
            "piano" -> listOf("198_Yamaha_SY1_piano.sf2", "Grand_Piano.sf2", "SC55_Piano_V2.sf2")
            "guitar" -> listOf("Miniis_electric_guitar.sf2", "Seagull_Acoustic_Guitar.sf2", "guitar_samples_clean.sf2")
            "drums" -> listOf("Metal_Drums.sf2", "drums_ken_ardency.sf2")
            "metronome" -> listOf("Metronom.sf2")
            "tanpura" -> listOf("Tanpura_drone.sf2")
            "tabla" -> listOf("Indian_Ensemble.sf2", "tabla.sf2")
            else -> listOf("default.sf2")
        }
    }

    fun getLoadedPresetNames(instrumentKey: String): List<String> =
        audioEngine.getLoadedPresetNames(instrumentKey).ifEmpty { listOf("Default Preset") }

    fun selectPresetByIndex(instrumentKey: String, index: Int): Pair<Int, Int>? =
        audioEngine.selectPresetByIndex(instrumentKey, index) ?: Pair(0, index)

    fun exportVideoToFile(onComplete: (String) -> Unit) {
        scope.launch {
            isVideoExporting.value = true
            videoExportProgress.value = 0.1f
            delay(500)
            videoExportProgress.value = 0.5f
            delay(500)
            videoExportProgress.value = 1.0f
            isVideoExporting.value = false
            onComplete("/sdcard/Movies/pEQ_Visualizer_${Clock.System.now().toEpochMilliseconds()}.mp4")
        }
    }


    fun triggerKeyboardNote(midiNote: Int, mode: String, scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            try {
                audioEngine.noteOn(midiNote, 0.9f)
                delay(250L)
                audioEngine.noteOff(midiNote)
            } catch (e: Exception) {
                // Audio engine safe handling
            }
        }
    }
    val isPatternLooping = mutableStateOf(false)
    val liveTutorialBpm = mutableIntStateOf(120) // Linked straight to your new slider

    // Recovered Sequencer & Drum Studio State
    val isSequencerActive = mutableStateOf(false)
    var sequencerJob: Job? = null
    val masterBpm = mutableFloatStateOf(120f)
    val currentStep = mutableIntStateOf(0)
    val sequencerSteps = List(16) { mutableIntStateOf(0) }
    val isRecordingDrumSession = mutableStateOf(false)
    val drumPadLabels = mutableStateListOf<String>()
    val drumPadVolumes = mutableStateMapOf<Int, Float>()
    val drumKits = mutableStateListOf<DrumKitPreset>()
    val selectedDrumKit = mutableStateOf<DrumKitPreset?>(null)
    val drumTutorials = mutableStateListOf<DrumTutorial>()
    val selectedTutorial = mutableStateOf<DrumTutorial?>(null)
    val isKitBuilderActive = mutableStateOf(false)
    val drumReverb = mutableStateOf(0f)
    val drumDelay = mutableStateOf(0f)
    val drumKitVolume = mutableStateOf(1f)
    val drumWorkspaceMode = mutableStateOf("PADS")
    val editingPadIndex = mutableStateOf(-1)
    val sampleLibrary: Map<String, List<String>> = mapOf(
        "Kicks" to listOf("0-KICK", "4-KICK_ALT", "12-BASS_SUB"),
        "Snares" to listOf("1-SNARE", "5-SNARE_ALT", "13-RIM_CLICK"),
        "Hats" to listOf("2-HIHAT_CLOSED", "6-HIHAT_OPEN", "11-RIDE", "7-CRASH"),
        "Percussion" to listOf("3-CLAP", "8-TOM_HI", "9-TOM_MID", "10-TOM_LO", "14-PERC", "15-FX")
    )
    fun remapPad(index: Int, sample: String) {
        if (index in 0 until drumPadLabels.size) {
            drumPadLabels[index] = sample
        }
    }
    fun updateDrumPadVolume(index: Int, volume: Float) {
        drumPadVolumes[index] = volume
    }
    val isTunerActive = mutableStateOf(false)
    val detectedChord = mutableStateOf("-")

fun playPattern(pattern: DrumTutorial) {
    // Kill any active sequence thread passes before spinning up a new timeline lane
    sequencerJob?.cancel()
    isPatternLooping.value = true
    
    val activeKit = selectedDrumKit.value ?: drumKits.firstOrNull() ?: return

    sequencerJob = viewModelScope.launch(Dispatchers.Default) {
        // Continuous execution check framework loop pass
        while (isActive && isPatternLooping.value) {
            pattern.steps.forEachIndexed { stepIdx: Int, activeTokens: List<String> ->
                // Guard check to instantly break out of the sequence if looping is turned off mid-cycle
                if (!isPatternLooping.value) return@launch

                // Read tempo calculations dynamically on every single tick step event handler
                val currentBpm = liveTutorialBpm.intValue.coerceIn(40, 240)
                val stepDelayMs = ((60.0 / currentBpm) * 1000 / 4).toLong() // 16th note subdivision math

                activeTokens.forEach { token: String ->
                    val baseLabel = token.uppercase().trim()
                    val targetIndex = activeKit.labels.indexOfFirst { it.contains(baseLabel) }

                    if (targetIndex != -1) {
                        val literalLabel = activeKit.labels[targetIndex]

                        // High-priority low-latency audio execution pass
                        audioEngine.playSample(label = literalLabel, index = targetIndex, string = "DRUM")

                        // Push dynamic highlight tracking state onto UI elements
                        launch(Dispatchers.Main) {
                            sequencerTriggeredPads[literalLabel] = true
                            launch {
                                delay(70)
                                sequencerTriggeredPads[literalLabel] = false
                            }
                        }
                    }
                }
                delay(stepDelayMs)
            }
        }
    }
}
private fun loadAssets() {
    scope.launch {
        // Define your strict blueprint layout sequence once
        val standardLabels = listOf(
            "0-KICK",      "1-SNARE",      "2-HIHAT_CLOSED", "3-CLAP",
            "4-KICK_ALT",  "5-SNARE_ALT",  "6-HIHAT_OPEN",   "7-CRASH",
            "8-TOM_HI",    "9-TOM_MID",    "10-TOM_LO",      "11-RIDE",
            "12-BASS_SUB", "13-RIM_CLICK", "14-PERC",        "15-FX"
        )

        // 1. Directly populate your kits list in memory
        val professionalKits = listOf(
            DrumKitPreset("ELECTRONIC_808", standardLabels),
            DrumKitPreset("ACOUSTIC_STUDIO", standardLabels),
            DrumKitPreset("METAL_CORE", standardLabels),
            DrumKitPreset("HIPHOP_TRAP", standardLabels),
            DrumKitPreset("LOFI_CHILL", standardLabels)
        )

        withContext(Dispatchers.Main) {
            drumKits.clear()
            drumKits.addAll(professionalKits)
            
            // Set default kit selection on startup
            if (professionalKits.isNotEmpty()) {
                selectedDrumKit.value = professionalKits[0]
                drumPadLabels.clear()
                drumPadLabels.addAll(professionalKits[0].labels)
            }
        }

        // 2. Load Tutorials from JSON (Keep this since it has your 100 patterns!)
        try {
            val tutsJson = scanner?.readResourceContent("drums/drum_tutorials.json") ?: "[]"
            if (tutsJson != "[]") {
                val tuts = Json.decodeFromString<List<DrumTutorial>>(tutsJson)
                withContext(Dispatchers.Main) {
                    drumTutorials.clear()
                    drumTutorials.addAll(tuts)
                }
            }
        } catch (e: Exception) {
            println("pEQControl: [ENGINE] Tutorial Parsing Error -> ${e.message}")
        }

        // 3. Initialize Groove System
        try {
            val loader = object : PatternLoader {
                override suspend fun loadGrooveJson(): String {
                    return scanner?.readResourceContent("drums/drum_patterns.json") ?: "{}"
                }
            }
            grooveRepository = GrooveRepository(loader)
            grooveRepository?.patterns?.collect { patterns ->
                groovePatterns.value = patterns
            }
        } catch (e: Exception) {
            println("pEQControl: Groove collection failure -> ${e.message}")
        }
    }
}
fun stopPatternPlayback() {
    isPatternLooping.value = false
    sequencerJob?.cancel()
    sequencerJob = null
    // Clean remaining flash borders instantly
    sequencerTriggeredPads.clear()
}
    val sequencerTriggeredPads = mutableStateMapOf<String, Boolean>()
    val drumKitRippleProgress = mutableStateMapOf<String, Float>()
    fun triggerThreeDDrumVisualizer(label: String) {
        sequencerTriggeredPads[label] = true
        drumKitRippleProgress[label] = 0f

        viewModelScope.launch {
            // Safely iterate while the progress is less than 1f
            while ((drumKitRippleProgress[label] ?: 0f) < 1f) {
                val current = drumKitRippleProgress[label] ?: 0f
                drumKitRippleProgress[label] = current + 0.08f
                delay(16)
            }
            
            // Clean up
            drumKitRippleProgress.remove(label)
            sequencerTriggeredPads[label] = false
        }
    }
    // Inside your AppViewModel class:
val songTimeline = mutableStateListOf<SongBar>().apply {
    // Start with a default 4-bar skeleton structural loop
    add(SongBar("1", "ELECTRONIC_808: 4/4 Core Foundation"))
    add(SongBar("2", "ELECTRONIC_808: Club Syncopation"))
    add(SongBar("3", "ELECTRONIC_808: 4/4 Core Foundation"))
    add(SongBar("4", "ELECTRONIC_808: Build Up Tension Fills", isFiller = true))
}

val currentPlayingBarIndex = mutableIntStateOf(-1)
val isSongPlaybackActive = mutableStateOf(false)
private var songSequencerJob: Job? = null

/**
 * Playback Engine: Loops through the timeline bars sequentially
 */
fun startSongPlayback() {
    stopPatternPlayback() // Stop single pattern loop
    songSequencerJob?.cancel()
    isSongPlaybackActive.value = true

    val activeKit = selectedDrumKit.value ?: drumKits.firstOrNull() ?: return

    songSequencerJob = viewModelScope.launch(Dispatchers.Default) {
        while (isActive && isSongPlaybackActive.value) {
            
            for (barIdx in 0 until songTimeline.size) {
                if (!isSongPlaybackActive.value) break
                
                withContext(Dispatchers.Main) {
                    currentPlayingBarIndex.intValue = barIdx
                }

                val currentBar = songTimeline[barIdx]
                // Find the associated pattern structure in your library matching the timeline step property
                val pattern = drumTutorials.find { it.name == currentBar.patternName }

                if (pattern != null) {
                    // Execute the 16 steps of this specific bar matching the master BPM clock
                    pattern.steps.forEachIndexed { stepIdx: Int, activeTokens: List<String> ->
                        if (!isSongPlaybackActive.value) return@launch

                        val currentBpm = liveTutorialBpm.intValue.coerceIn(40, 240)
                        val stepDelayMs = ((60.0 / currentBpm) * 1000 / 4).toLong()

                        activeTokens.forEach { token: String ->
                            val baseLabel = token.uppercase().trim()
                            val targetIndex = activeKit.labels.indexOfFirst { it.contains(baseLabel) }

                            if (targetIndex != -1) {
                                val literalLabel = activeKit.labels[targetIndex]
                                audioEngine.playSample(label = literalLabel, index = targetIndex, string = "DRUM")

                                launch(Dispatchers.Main) {
                                    sequencerTriggeredPads[literalLabel] = true
                                    launch { delay(70); sequencerTriggeredPads[literalLabel] = false }
                                }
                            }
                        }
                        delay(stepDelayMs)
                    }
                } else {
                    // Fallback baseline safety delay if a slot is left unassigned or empty
                    delay(((60.0 / liveTutorialBpm.intValue) * 1000 * 4).toLong())
                }
            }
        }
    }
}

fun stopSongPlayback() {
    isSongPlaybackActive.value = false
    songSequencerJob?.cancel()
    songSequencerJob = null
    currentPlayingBarIndex.intValue = -1
    sequencerTriggeredPads.clear()
}
fun startTutorial(tutorial: DrumTutorial) {
    selectedTutorial.value = tutorial
    // ✅ Automatically triggers steady loop tracking on state update
    playPattern(tutorial) 
}
fun handleTapTempo() {
    val now = Clock.System.now().toEpochMilliseconds()
    tapTimes.add(now)
    
    // Maintain a moving window of the last 4 structural taps
    if (tapTimes.size > 4) {
        tapTimes.removeAt(0)
    }
    
    if (tapTimes.size >= 2) {
        val intervals = mutableListOf<Long>()
        for (i in 1 until tapTimes.size) {
            intervals.add(tapTimes[i] - tapTimes[i - 1])
        }
        val averageIntervalMs = intervals.average()
        if (averageIntervalMs > 0) {
            val calculatedBpm = (60000.0 / averageIntervalMs).toInt().coerceIn(40, 240)
            liveTutorialBpm.intValue = calculatedBpm
        }
    }
}
// --- SEQUENCER TIMELINE MANIPULATION UTILITIES ---

fun addBarToTimeline(patternName: String, isFiller: Boolean = false) {
    val newId = (songTimeline.size + 1).toString()
    songTimeline.add(SongBar(newId, patternName, isFiller))
}

fun removeBarFromTimeline(index: Int) {
    if (index in songTimeline.indices) {
        songTimeline.removeAt(index)
    }
}

fun updateBarPattern(index: Int, newPatternName: String, isFiller: Boolean = false) {
    if (index in songTimeline.indices) {
        songTimeline[index] = songTimeline[index].copy(patternName = newPatternName, isFiller = isFiller)
    }
}
    fun moveTimelineItem(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    if (fromIndex in songTimeline.indices && toIndex in songTimeline.indices) {
        // Run the swap operation on the main thread to ensure immediate UI synchronization
        val movingItem = songTimeline.removeAt(fromIndex)
        songTimeline.add(toIndex, movingItem)
    }
}
fun scrubSongPlayhead(targetProgress: Float) {
    if (songTimeline.isEmpty()) return
    
    // Convert 0.0f - 1.0f progress ratio to an index boundary check
    val absoluteTargetIndex = (targetProgress * songTimeline.size).toInt().coerceIn(0, songTimeline.size - 1)
    
    if (currentPlayingBarIndex.intValue != absoluteTargetIndex) {
        currentPlayingBarIndex.intValue = absoluteTargetIndex
        
        // If song playback is active, gracefully sync the background coroutine step to the new bar position
        if (isSongPlaybackActive.value) {
            startSongPlayback() 
        }
    }
}
    val ragaList = mutableStateOf<List<Raaga>>(emptyList())
    val isFetchingRagas = mutableStateOf(false)

    val currentScore = mutableStateOf<Score?>(null)
    val scorePlayheadMeasure = mutableIntStateOf(0)
    val scorePlayheadOffset = mutableFloatStateOf(0f)
    val isScorePlaying = mutableStateOf(false)
    private var scorePlaybackJob: Job? = null

    private val firedNotesMap = mutableMapOf<Int, Boolean>()
    private var lastTrackedMeasure = -1
    private var grooveRepository: GrooveRepository? = null
    val groovePatterns = MutableStateFlow<List<GroovePattern>>(emptyList())

    val pdfImportProgress = mutableStateOf(0f)
    val isImportingPDF = mutableStateOf(false)
    val pdfExportProgress = mutableStateOf(0f)
    val isExportingPDF = mutableStateOf(false)

    val sustainActive = mutableStateOf(false)
    private val notesToReleaseOnSustainUp = mutableSetOf<Int>()
    val virtualMidiConnected = mutableStateOf(false)
    val midiConnectedDevices = androidx.compose.runtime.mutableStateListOf<String>()
    val midiActiveNotes = mutableStateMapOf<Int, Boolean>()

    val notationStaffMidiPlaying = mutableStateOf(false)
    val notationPlayProgress = mutableStateOf(0f)

    private val _selectedSystemState = MutableStateFlow<String?>(null)
    val selectedSystemState: StateFlow<String?> = _selectedSystemState.asStateFlow()

    fun loadRagas(system: String? = null) {
        viewModelScope.launch {
            try {
                isFetchingRagas.value = true

                val rawData = ragaRepository?.fetchExpandedRaagas() ?: emptyList()

                val allRagas = if (rawData.isNotEmpty()) {
                    (rawData + INDIAN_RAAGAS + CARNATIC_RAAGAS).distinctBy { it.name }
                } else {
                    (INDIAN_RAAGAS + CARNATIC_RAAGAS).distinctBy { it.name }
                }

                ragaList.value = if (system != null) {
                    allRagas.filter { it.system.equals(system, ignoreCase = true) }
                } else {
                    allRagas
                }

            } catch (e: Exception) {
                println("pEQLearn: Error in loadRagas: ${e.message}")
                val fallback = (INDIAN_RAAGAS + CARNATIC_RAAGAS).distinctBy { it.name }
                ragaList.value = if (system != null) {
                    fallback.filter { it.system.equals(system, ignoreCase = true) }
                } else {
                    fallback
                }
            } finally {
                isFetchingRagas.value = false
            }
        }
    }

    fun loadRagasBySystem(system: String) {
        loadRagas(system)
    }

    fun seekToPosition(measure: Int, offset: Float, beatsPerMeasure: Float) {
        scorePlayheadMeasure.value = measure
        scorePlayheadOffset.value = offset
        firedNotesMap.clear()
        lastTrackedMeasure = measure

        val score = currentScore.value
        val dynamicSecondsPerMeasure = if (score != null) {
            (60f / score.tempo) * beatsPerMeasure
        } else {
            2.5f
        }

        val totalSeconds = (measure.toFloat() + offset) * dynamicSecondsPerMeasure
        val targetPositionMs = (totalSeconds * 1000).toLong()
        audioEngine.seekTo(targetPositionMs)
    }

    fun startScorePlayback(beatsPerMeasure: Float) {
        val score = currentScore.value ?: return
        if (isScorePlaying.value) return

        isScorePlaying.value = true

        val dynamicSecondsPerMeasure = (60f / score.tempo) * beatsPerMeasure
        val measureDurationMs = (dynamicSecondsPerMeasure * 1000).toLong()

        scorePlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            val startMeasure = scorePlayheadMeasure.value
            val startOffset = scorePlayheadOffset.value
            val initialElapsedMs = (startMeasure * measureDurationMs) + (startOffset * measureDurationMs).toLong()

            while (isActive && isScorePlaying.value) {
                val now = Clock.System.now().toEpochMilliseconds()
                val totalElapsedMs = initialElapsedMs + (now - startTime)

                val currentMeasure = (totalElapsedMs / measureDurationMs).toInt()
                val currentOffset = (totalElapsedMs % measureDurationMs).toFloat() / measureDurationMs

                if (currentMeasure != lastTrackedMeasure) {
                    firedNotesMap.clear()
                    lastTrackedMeasure = currentMeasure
                }

                if (currentMeasure >= score.measures.size) {
                    withContext(Dispatchers.Main) {
                        stopScorePlayback()
                        resetScorePlayhead()
                    }
                    break
                }

                withContext(Dispatchers.Main) {
                    scorePlayheadMeasure.value = currentMeasure
                    scorePlayheadOffset.value = currentOffset
                }

                val measure = score.measures[currentMeasure]
                measure.notes.forEachIndexed { index: Int, note: ScoreNote ->
                    val alreadyFired = firedNotesMap[index] == true

                    if (currentOffset >= note.position && !alreadyFired) {
                        firedNotesMap[index] = true
                        onMidiEventReceived(note.midiNote, isNoteOn = true)

                        launch {
                            delay((note.duration * measureDurationMs).toLong())
                            onMidiEventReceived(note.midiNote, isNoteOn = false)
                        }
                    }
                }
                delay(16)
            }
        }
    }

    fun toggleScorePlayback(beatsPerMeasure: Float) {
        if (isScorePlaying.value) {
            stopScorePlayback()
        } else {
            startScorePlayback(beatsPerMeasure)
        }
    }

    fun stopScorePlayback() {
        isScorePlaying.value = false
        scorePlaybackJob?.cancel()
        scorePlaybackJob = null

        audioEngine.allNotesOff()

        midiActiveNotes.clear()
        midiPlayingNote.value = null
        firedNotesMap.clear()
    }

    fun resetScorePlayhead() {
        stopScorePlayback()
        scorePlayheadMeasure.value = 0
        scorePlayheadOffset.value = 0f 
        audioEngine.seekTo(0L)
    }

    fun onMidiEventReceived(note: Int, isNoteOn: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            if (isNoteOn) {
                audioEngine.noteOn(note, 90f)
                midiActiveNotes[note] = true
                midiPlayingNote.value = note
            } else {
                audioEngine.noteOff(note)
                midiActiveNotes.remove(note)
                if (midiPlayingNote.value == note) {
                    midiPlayingNote.value = midiActiveNotes.keys.firstOrNull()
                }
            }
        }
    }

    fun parseIncomingScoreFile(uriString: String) {
        isImportingPDF.value = true
        viewModelScope.launch(Dispatchers.Default) {
            delay(1000)

            val mockScore = Score(
                title = "Imported Score",
                tempo = 96f,
                timeSignature = "4/4",
                measures = List(16) { mIdx ->
                    Measure(
                        notes = listOf(
                            ScoreNote(midiNote = 60 + (mIdx % 12), position = 0f, duration = 0.25f),
                            ScoreNote(midiNote = 64 + (mIdx % 12), position = 0.25f, duration = 0.25f),
                            ScoreNote(midiNote = 67 + (mIdx % 12), position = 0.5f, duration = 0.25f),
                            ScoreNote(midiNote = 72 + (mIdx % 12), position = 0.75f, duration = 0.25f)
                        ),
                        barline = if (mIdx == 15) "final" else "single"
                    )
                }
            )

            withContext(Dispatchers.Main) {
                currentScore.value = mockScore
                isImportingPDF.value = false
            }

            for (progress in 1..100) {
                delay(10)
                withContext(Dispatchers.Main) {
                    pdfImportProgress.value = progress / 100f
                }
            }
            withContext(Dispatchers.Main) {
                isImportingPDF.value = false
                pdfImportProgress.value = 0f
            }
        }
    }

    fun compileCurrentNotationToPDF() {
        isExportingPDF.value = true
        viewModelScope.launch(Dispatchers.Default) {
            for (progress in 1..100) {
                delay(40)
                withContext(Dispatchers.Main) {
                    pdfExportProgress.value = progress / 100f
                }
            }
            withContext(Dispatchers.Main) {
                isExportingPDF.value = false
                pdfExportProgress.value = 0f
            }
        }
    }

    fun playPattern(pattern: GroovePattern) {
        val kit = drumKits.firstOrNull() ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val stepDelayMs = ((60.0 / pattern.bpm.coerceIn(40, 240)) * 1000 / 4).toLong()
            for (step in 0 until pattern.stepsPerBar) {
                val stepEvents = pattern.events.filter { it.step == step }
                for (event in stepEvents) {
                    val label = kit.labels.getOrNull(event.padIndex) ?: "KICK"
                    audioEngine.playSample(label, event.padIndex, "DRUM")
                }
                delay(stepDelayMs)
            }
        }
    }

    val liveRiyaazBpm = mutableIntStateOf(100)

    fun startRiyaazEngine(initialScale: String, totalBeats: Int) {
        stopScorePlayback()
        isSequencerActive.value = true
        sequencerJob?.cancel()

        sequencerJob = viewModelScope.launch(Dispatchers.Default) {
            var stepIndex = 0

            while (isActive && isSequencerActive.value) {
                val currentLiveBpm = liveRiyaazBpm.intValue.coerceIn(40, 220)
                val beatDelayMs = ((60.0 / currentLiveBpm) * 1000).toLong()

                onSequencerStepTick(stepIndex, initialScale)
                stepIndex = (stepIndex + 1) % totalBeats

                delay(beatDelayMs)
            }
        }
    }

    private fun onSequencerStepTick(stepIndex: Int, selectedScale: String) {
        val stepValue = sequencerSteps.getOrNull(stepIndex)?.value ?: 0
        if (stepValue > 0) {
            audioEngine.playSample(
                label = if (stepIndex == 0) "METRONOME-STRONG" else "METRONOME-WEAK",
                index = stepIndex,
                string = "SYSTEM"
            )
        }

        if (stepIndex % 4 == 0) {
            audioEngine.playSample(
                label = selectedScale.uppercase().trim(),
                index = stepIndex,
                string = "TANPURA"
            )
        }

        viewModelScope.launch(Dispatchers.Main) {
            currentStep.intValue = stepIndex
        }
    }

    fun stopRiyaazEngine() {
        isSequencerActive.value = false
        sequencerJob?.cancel()
        sequencerJob = null

        audioEngine.playSample(label = "STOP", index = 0, string = "TANPURA")
    }

    fun onMidiNoteOn(note: Int, velocity: Int) {
        midiActiveNotes[note] = true
        audioEngine.noteOn(note, velocity.toFloat() / 127f)
        notesToReleaseOnSustainUp.remove(note)
    }

    fun onMidiNoteOff(note: Int) {
        if (sustainActive.value) {
            notesToReleaseOnSustainUp.add(note)
        } else {
            midiActiveNotes[note] = false
            audioEngine.noteOff(note)
        }
    }

    fun onMidiController(controller: Int, value: Int) {
        audioEngine.sendMidiController(controller, value)
        if (controller == 64) {
            val active = value >= 64
            if (sustainActive.value && !active) {
                notesToReleaseOnSustainUp.forEach { note ->
                    midiActiveNotes[note] = false
                    audioEngine.noteOff(note)
                }
                notesToReleaseOnSustainUp.clear()
            }
            sustainActive.value = active
        }
    }

    fun playMidiNote(midiNote: Int) {
        audioEngine.playMidiNote(midiNote)
    }

    private fun getNoteNameFromMidi(midiNote: Int): String {
        if (midiNote !in 0..127) return "UNKNOWN"
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val noteIndex = midiNote % 12
        val octave = (midiNote / 12) - 1
        return "${noteNames[noteIndex]}$octave"
    }
}
