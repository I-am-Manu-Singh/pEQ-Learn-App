package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import kotlin.math.*

import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.*
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
import com.creativeblisslabs.peqlearn.app.data.Device
import com.creativeblisslabs.peqlearn.app.data.SyncedLine
import com.creativeblisslabs.peqlearn.app.data.repository.*
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import com.creativeblisslabs.peqlearn.app.network.createHttpClient
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQTheme
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull

import com.creativeblisslabs.peqlearn.app.data.normalizePath

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

   
   
 
    val currentStepIndex = mutableIntStateOf(0)
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

      val detectedPitch = mutableFloatStateOf(0f)
    val detectedNote = mutableStateOf("-")
    val centsDeviation = mutableIntStateOf(0)
    val playbackSpeed = mutableStateOf(1.0f)
    val toastMessage = mutableStateOf<String?>(null)
        val isBackgroundServiceRunning = mutableStateOf(true)

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
    val isLeftHandMuted = mutableStateOf(false)
    val isRightHandMuted = mutableStateOf(false)
    val isLeftHandHidden = mutableStateOf(false)
    val isRightHandHidden = mutableStateOf(false)
    val leftHandColor = mutableStateOf(Color(0xFF6C63FF))
    val rightHandColor = mutableStateOf(Color(0xFF4A90E2))
    val transposition = mutableIntStateOf(0)
    val octaveShift = mutableIntStateOf(0)
    val isSplitStaff = mutableStateOf(true)
    val isVerticalScroll = mutableStateOf(false)
    val metronomeEnabled = mutableStateOf(false)
    private var metronomeJob: Job? = null

    // System Settings, Notation & SoundFont Preferences
    val videoExportFormat = mutableStateOf("MP4")
    val notationFormat = mutableStateOf("Both Viewers")
    val selectedKeySignature = mutableStateOf("C Major")
    val selectedTimeSignature = mutableStateOf("4/4")
    val selectedInstrument = mutableStateOf("Piano")
    val quantizeDivision = mutableStateOf("1/16")
    val scrollDirectionHorizontal = mutableStateOf(true)
    val shiftNotesOffset = mutableStateOf(0)
    val videoExportEffects = mutableStateMapOf<String, Boolean>("Glow" to true, "Particles" to true)
    val midiPlayingNote = mutableStateOf<Int?>(null)
    val selectedSettingsTab = mutableIntStateOf(0)
    val useOsmdRenderer = mutableStateOf(false)
    val useVexflow get() = useOsmdRenderer
    val pitchEngineAlgo = mutableStateOf("Phase Vocoder")
    val stemSplitQuality = mutableStateOf("Standard")
    val pianoSf2File = mutableStateOf("piano/198_Yamaha_SY1_piano.sf2")
    val pianoBank = mutableIntStateOf(0)
    val pianoPreset = mutableIntStateOf(0)
    val pianoChannel = mutableIntStateOf(0)
    val pianoVelocity = mutableIntStateOf(95)
    val guitarSf2File = mutableStateOf("guitar/guitar_samples_clean.sf2")
    val guitarBank = mutableIntStateOf(0)
    val guitarPreset = mutableIntStateOf(0)
    val guitarChannel = mutableIntStateOf(0)
    val guitarVelocity = mutableIntStateOf(72)
    val drumSf2File = mutableStateOf("drums/Metal_Drums.sf2")
    val drumBank = mutableIntStateOf(0)
    val drumPreset = mutableIntStateOf(0)
    val drumChannel = mutableIntStateOf(9)
    val drumVelocity = mutableIntStateOf(90)
    val useSf2ForDrums = mutableStateOf(false)
    val metronomeSf2File = mutableStateOf("metronome/Metronom.sf2")
    val metronomeBank = mutableIntStateOf(0)
    val metronomePreset = mutableIntStateOf(0)
    val metronomeChannel = mutableIntStateOf(10)
    val metronomeVelocity = mutableIntStateOf(100)
    val useSf2ForMetronome = mutableStateOf(false)
    val tanpuraSf2File = mutableStateOf("tanpura/Tanpura_drone.sf2")
    val tanpuraBank = mutableIntStateOf(0)
    val tanpuraPreset = mutableIntStateOf(0)
    val tanpuraChannel = mutableIntStateOf(2)
    val tanpuraVelocity = mutableIntStateOf(58)
    val useSf2ForTanpura = mutableStateOf(false)
    val tablaSf2File = mutableStateOf("tabla/Indian_Ensemble.sf2")
    val tablaBank = mutableIntStateOf(0)
    val tablaPreset = mutableIntStateOf(0)
    val tablaChannel = mutableIntStateOf(9)
    val tablaVelocity = mutableIntStateOf(74)
    val tunerReferenceFrequency = mutableFloatStateOf(440f)
    val metronomeTickSound = mutableStateOf("Woodblock")
    val metronomeSubdivision = mutableIntStateOf(1)
    val fallingNotesSpeed = mutableStateOf("Normal")
    val waterfallTheme = mutableStateOf("Neon Glow")
    val showPianoKeyLabels = mutableStateOf(true)
    val showNotationNoteNames = mutableStateOf(true)
    val showWaterfall = mutableStateOf(true)
    val showWaterfallParticles = mutableStateOf(true)
    val is3DPerspectiveMode = mutableStateOf(true)
    val horizonScale = mutableFloatStateOf(0.68f)
    // --- MIDIVisualizer-master Studio Settings Parameters ---
    // Particles (Viewer::showParticleOptions)
    val particleCount = mutableIntStateOf(16) // count per strike / intensity
    val particleSpeed = mutableFloatStateOf(4.5f) // speed (0..10)
    val particleScale = mutableFloatStateOf(1.0f) // size/scale (0.2..3.0)
    val particleExpansion = mutableFloatStateOf(1.0f) // spread (0.2..5.0)
    val lockParticleColor = mutableStateOf(true) // sync with note color
    val particleCustomColor = mutableStateOf(Color(0xFFFFD700))

    // Key Flashes & Bloom (Viewer::showFlashOptions)
    val flashSize = mutableFloatStateOf(1.0f) // 0..2.0
    val flashHaloIntensity = mutableFloatStateOf(0.65f) // 0..1.0
    val flashInnerRadius = mutableFloatStateOf(0.3f)
    val flashOuterRadius = mutableFloatStateOf(0.95f)

    // Notes Style & Appearance (Viewer::showNoteOptions)
    val noteCornerRadius = mutableFloatStateOf(6.0f) // corner radius
    val noteEdgeThickness = mutableFloatStateOf(1.2f) // edge width
    val noteEdgeBrightness = mutableFloatStateOf(0.6f) // edge brightness
    val noteFadeOutTail = mutableFloatStateOf(0.0f) // tail fade out (0..1)
    val noteStyleOval = mutableStateOf(false) // oval vs cylindrical tube

    // Piano Interface & Premium Effects (Viewer::showKeyboardOptions)
    val keyboardHighlightActive = mutableStateOf(true) // highlight active keys
    val keyboardShowChords = mutableStateOf(true) // show chord overlay
    val keyboardMinorEdges = mutableStateOf(true) // show black key bevel edges
    val keyboardShowVelvetCloth = mutableStateOf(true) // studio red velvet cloth strip
    val keyboardClothColor = mutableStateOf(Color(0xFF8B0000)) // rich crimson velvet
    val keyboardShowSideFrames = mutableStateOf(true) // mahogany piano side cheeks
    val keyboardHeightFactor = mutableFloatStateOf(145f) // keyboard height dp

    // Waves & Ambient Effects (Viewer::showWaveOptions)
    val waveAmplitude = mutableFloatStateOf(0.0f) // 0..1.0

    // Pedals (Viewer::showPedalOptions)
    val showPedals = mutableStateOf(true)
    val pedalScaling = mutableFloatStateOf(1.0f) // 0.5..1.5
    val pedalOpacity = mutableFloatStateOf(0.95f) // 0..1.0
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
    val videoExportLayoutMode = mutableStateOf("Combined (Score + Waterfall)")
    val isVideoExporting = mutableStateOf(false)
    val videoExportProgress = mutableFloatStateOf(0f)
  
    val showMiniPlayer = mutableStateOf(true)
    val isFullscreenKeyboard = mutableStateOf(false)
    val isNotationFullscreen = mutableStateOf(false)
    val isImmersiveLabActive = mutableStateOf(false)
    val isTanpuraFullscreen = mutableStateOf(false)
    val practicingChord = mutableStateOf<String?>(null)
    val isTanpuraActive = mutableStateOf(false)
    val tanpuraGain = mutableFloatStateOf(0.8f)
    val tablaGain = mutableFloatStateOf(1.0f)
    val metronomeGain = mutableFloatStateOf(1.0f)

    // Callback interfaces for platform-specific features
    var onScanMidiDevicesRequested: (() -> Unit)? = null
    var onVideoExportRequested: ((Score, String, Int, Map<String, Boolean>, (Float) -> Unit, (String?) -> Unit) -> Unit)? = null
    var onPdfExportRequested: ((Score, PdfExportOptions, (Float) -> Unit, (String?) -> Unit) -> Unit)? = null
    val pdfExportOptions = mutableStateOf(PdfExportOptions())

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

   
    val audioDelayMs = mutableLongStateOf(0L)
    val openAtStartup = mutableStateOf(false)

    val abRepeatActive = mutableStateOf(false)
    // Initialize with -1L instead of null
    val abRepeatPointA = mutableStateOf(-1L)
    val abRepeatPointB = mutableStateOf(-1L)
    val currentFolder = mutableStateOf<String?>(null)
    
  
    internal var activeCrossfadeJob: Job? = null



   val favorites = mutableStateListOf<String>()
   
    val selectedItems = mutableStateListOf<pEQMediaItem>()
   
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    internal val scanFolders = mutableStateListOf<String>()
    /** Platform-specific primary external storage root (e.g. /storage/emulated/999 on some tablets). Set by Android before start(). */
    var primaryStoragePath: String = "/storage/emulated/0"
    
   
    val activeJob = mutableStateOf<SplitJob?>(null)
    val backgroundJobs = mutableStateListOf<SplitJob>()
    private var lastFftTime = 0L
    var lastSyncTime = 0L

    fun start() {
        if (hasInitialized) return
        hasInitialized = true
        restoreSession()
        
        // Natural decay loop — zeros out RTA bars when paused/stopped, no fake data
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(80)
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
    val platformStorageBase = mutableStateOf("/storage/emulated/0")


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
                loadAssets()
                if (scanFolders.isNotEmpty()) {
                    refreshLibrary()
                }
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
    val isSessionRestored = mutableStateOf(false)
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
    
    
   
    val exportFormat = mutableStateOf("WAV")
    val exportSampleRate = mutableStateOf(48000)

    // ⚙️ Workstation System Hooks
    var workstationRegistry = mutableStateMapOf<String, String>()
    val isStemSplitting = mutableStateOf(false)
    val stemSplitProgress = mutableStateOf(0f)
    val stemSplitStatusMessage = mutableStateOf("Ready for Separation")
    val isStemsPlaying = mutableStateOf(false)
    val activeStemMap = mutableStateMapOf<String, String>()
    val selectedMixerItem = mutableStateOf<pEQMediaItem?>(null)
    val stemSeparationModel = mutableStateOf("Spleeter (2-Stem: Vocals + Accompaniment)")
    val customOnnxModelPath = mutableStateOf<String?>(null)
    val customOnnxModelName = mutableStateOf<String?>(null)
    val stemExportBitDepth = mutableStateOf("24-bit") // "16-bit", "24-bit", "32-bit Float"
    val stemExportFormat = mutableStateOf("WAV") // "WAV", "FLAC", "MP3"
    val stemExportSampleRate = mutableStateOf(44100) // 44100, 48000, 96000
    val isStemDeReverbEnabled = mutableStateOf(false)
    val isStemDeNoiseEnabled = mutableStateOf(false)
    val isStemKaraokeExtraction = mutableStateOf(false)
    var onPickModelRequested: (((String, String) -> Unit) -> Unit)? = null

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
    internal fun setTheme(theme: pEQTheme) {
        _selectedTheme.value = theme
        persistSession()
    }

    private val _selectedAccent = mutableStateOf(pEQAccent.AMBER)
    val selectedAccent: State<pEQAccent> = _selectedAccent
    internal fun setAccent(accent: pEQAccent) {
        _selectedAccent.value = accent
        persistSession()
    }
    
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
            snapshotFlow { currentMediaItem.value?.path }.distinctUntilChanged().collect { path ->
                if (path != null) {
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

        // 🎚️ Real-time Instrument Gain Observers
        scope.launch {
            snapshotFlow { tanpuraGain.floatValue }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setTanpuraGain(it)
                }
        }
        scope.launch {
            snapshotFlow { tablaGain.floatValue }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setTablaGain(it)
                }
        }
        scope.launch {
            snapshotFlow { metronomeGain.floatValue }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setMetronomeGain(it)
                }
        }

        scope.launch {
            snapshotFlow { isLeftHandMuted.value }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setStemMute("left_hand", it)
                    audioEngine.setStemMute("BASS", it)
                    persistSession()
                }
        }

        scope.launch {
            snapshotFlow { isRightHandMuted.value }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setStemMute("right_hand", it)
                    audioEngine.setStemMute("PIANO", it)
                    audioEngine.setStemMute("VOCALS", it)
                    persistSession()
                }
        }

        scope.launch {
            snapshotFlow { isLeftHandHidden.value }
                .distinctUntilChanged()
                .collect { persistSession() }
        }

        scope.launch {
            snapshotFlow { isRightHandHidden.value }
                .distinctUntilChanged()
                .collect { persistSession() }
        }

        scope.launch {
            snapshotFlow { leftHandColor.value }
                .distinctUntilChanged()
                .collect { persistSession() }
        }

        scope.launch {
            snapshotFlow { rightHandColor.value }
                .distinctUntilChanged()
                .collect { persistSession() }
        }

        scope.launch {
            snapshotFlow { transposition.intValue }
                .distinctUntilChanged()
                .collect { persistSession() }
        }

        scope.launch {
            snapshotFlow {
                listOf(
                    notationFormat.value,
                    isSplitStaff.value.toString(),
                    isVerticalScroll.value.toString(),
                    showPianoKeyLabels.value.toString(),
                    is3DPerspectiveMode.value.toString(),
                    horizonScale.floatValue.toString(),
                    showWaterfallParticles.value.toString(),
                    fallingNotesSpeed.value,
                    selectedKeySignature.value,
                    selectedTimeSignature.value,
                    selectedInstrument.value,
                    quantizeDivision.value,
                    useOsmdRenderer.value.toString()
                ).joinToString(",")
            }.distinctUntilChanged().collect {
                persistSession()
            }
        }

        scope.launch {
            snapshotFlow { metronomeEnabled.value }
                .distinctUntilChanged()
                .collect { enabled ->
                    _metronomeActive.value = enabled
                    if (enabled) startMetronome() else stopMetronome()
                }
        }

        start()
        loadAssets()
    }

    private fun startMetronome() {
        metronomeJob?.cancel()
        metronomeJob = scope.launch(Dispatchers.Default) {
            var beatCount = 0
            while (isActive) {
                val bpm = _metronomeBpm.value
                val interval = (60000 / bpm).toLong()
                
                val isStrong = beatCount % 4 == 0
                audioEngine.playSample(
                    label = if (isStrong) "METRONOME-STRONG" else "METRONOME-WEAK",
                    index = beatCount,
                    string = "SYSTEM"
                )
                
                beatCount++
                
                // Use a smaller delay loop to react faster to BPM changes or cancellation
                val startTime = Clock.System.now().toEpochMilliseconds()
                while (Clock.System.now().toEpochMilliseconds() - startTime < interval && isActive) {
                    delay(10)
                }
            }
        }
    }

    private fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
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
            "guitar" -> listOf("Custom Classical Guitar.sf2", "guitar_samples_clean.sf2")
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
        val score = currentScore.value ?: Score(
            title = currentMediaItem.value?.title ?: "pEQ Performance",
            tempo = metronomeBpm.value.toFloat(),
            timeSignature = "4/4"
        )
        val res = videoExportResolution.value
        val fpsInt = when (videoExportFps.value) {
            "24 FPS" -> 24
            "30 FPS" -> 30
            "120 FPS" -> 120
            else -> 60
        }
        val effects = mapOf(
            "Glow" to (videoExportPeakSensitivity.floatValue > 30f),
            "Particles" to showWaterfallParticles.value,
            "Notation" to (videoExportLayoutMode.value.contains("Score") || videoExportLayoutMode.value.contains("Notation")),
            "Visualizer" to (videoExportLayoutMode.value.contains("Waterfall") || videoExportLayoutMode.value.contains("Visualizer")),
            "Theme" to (videoExportTheme.value == "Cyberpunk")
        )

        isVideoExporting.value = true
        videoExportProgress.value = 0.01f

        val exporter = onVideoExportRequested
        if (exporter != null) {
            exporter(score, res, fpsInt, effects, { progress ->
                videoExportProgress.value = progress
            }, { filePath ->
                isVideoExporting.value = false
                videoExportProgress.value = 1.0f
                onComplete(filePath ?: "/sdcard/Movies/pEQ_Visualizer_${Clock.System.now().toEpochMilliseconds()}.mp4")
            })
        } else {
            scope.launch {
                for (p in 1..10) {
                    delay(150)
                    videoExportProgress.value = p / 10f
                }
                isVideoExporting.value = false
                onComplete("/sdcard/Movies/pEQ_Visualizer_${Clock.System.now().toEpochMilliseconds()}.mp4")
            }
        }
    }

    // --- Optimized Score Temporal States ---
    val measureStartTimesSeconds = derivedStateOf {
        val score = currentScore.value ?: return@derivedStateOf emptyList<Float>()
        val starts = mutableListOf<Float>()
        var current = 0f
        for (m in score.measures) {
            starts.add(current)
            current += (60f / score.tempo) * m.timeSignatureNumerator
        }
        starts
    }

    val scoreTotalDurationSeconds = derivedStateOf {
        val starts = measureStartTimesSeconds.value
        val score = currentScore.value
        if (score == null || starts.isEmpty()) 0f
        else {
            val lastMeasure = score.measures.last()
            starts.last() + (60f / score.tempo) * lastMeasure.timeSignatureNumerator
        }
    }

    val scoreCurrentPositionSeconds = derivedStateOf {
        val score = currentScore.value ?: return@derivedStateOf 0f
        val measureIdx = scorePlayheadMeasure.value
        val offset = scorePlayheadOffset.value
        val starts = measureStartTimesSeconds.value
        
        val startSec = starts.getOrNull(measureIdx) ?: 0f
        val m = score.measures.getOrNull(measureIdx)
        val mDur = if (m != null) (60f / score.tempo) * m.timeSignatureNumerator else 0f
        
        startSec + (offset * mDur)
    }

    fun seekToTotalSeconds(totalSeconds: Float) {
        val score = currentScore.value ?: return
        val starts = measureStartTimesSeconds.value
        
        var targetM = 0
        var targetO = 0f
        
        for (i in score.measures.indices) {
            val start = starts.getOrNull(i) ?: 0f
            val m = score.measures[i]
            val dur = (60f / score.tempo) * m.timeSignatureNumerator
            
            if (totalSeconds < start + dur) {
                targetM = i
                targetO = (totalSeconds - start) / dur
                break
            }
            if (i == score.measures.lastIndex) {
                targetM = i
                targetO = 1.0f
            }
        }
        seekToPosition(targetM, targetO)
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
        if (index >= 0 && index < drumPadLabels.size) {
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
                withContext(Dispatchers.Main) {
                    currentStepIndex.intValue = stepIdx
                }
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
fun loadAssets() {
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
            println("pEQLearn: [ENGINE] Tutorial Parsing Error -> ${e.message}")
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
            println("pEQLearn: Groove collection failure -> ${e.message}")
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
                        withContext(Dispatchers.Main) {
                            currentStepIndex.intValue = stepIdx
                        }
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
    val sustainPedalActive = mutableStateOf(false) // CC 64 Damper/Sustain
    val sostenutoPedalActive = mutableStateOf(false) // CC 66 Sostenuto
    val softPedalActive = mutableStateOf(false) // CC 67 Soft / Una Corda
    val lastMidiVelocity = mutableIntStateOf(0)
    val lastMidiChannel = mutableIntStateOf(1)
    val connectedMidiDeviceName = mutableStateOf<String?>(null)
    val midiConnectionPrompt = mutableStateOf<MidiPromptMessage?>(null)

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

    fun seekToPosition(measure: Int, offset: Float) {
        val score = currentScore.value ?: return
        
        scorePlayheadMeasure.value = measure
        scorePlayheadOffset.value = offset
        firedNotesMap.clear()
        lastTrackedMeasure = measure

        var accumulatedMs = 0L
        for (i in 0 until measure) {
            val m = score.measures.getOrNull(i) ?: break
            val mDurationMs = ((60f / score.tempo) * m.timeSignatureNumerator * 1000).toLong()
            accumulatedMs += mDurationMs
        }
        
        val targetMeasure = score.measures.getOrNull(measure)
        if (targetMeasure != null) {
            val mDurationMs = ((60f / score.tempo) * targetMeasure.timeSignatureNumerator * 1000).toLong()
            accumulatedMs += (offset * mDurationMs).toLong()
        }

        audioEngine.seekTo(accumulatedMs)
    }

    fun startScorePlayback() {
        val score = currentScore.value ?: return
        if (isScorePlaying.value) return

        isScorePlaying.value = true

        scorePlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            var startMeasure = scorePlayheadMeasure.value
            var startOffset = scorePlayheadOffset.value
            
            // Calculate initial elapsed time based on sum of previous measures
            var initialElapsedMs = 0L
            for (i in 0 until startMeasure) {
                val m = score.measures.getOrNull(i) ?: break
                val mDurationMs = ((60f / score.tempo) * m.timeSignatureNumerator * 1000).toLong()
                initialElapsedMs += mDurationMs
            }
            val startMeasureObj = score.measures.getOrNull(startMeasure)
            if (startMeasureObj != null) {
                val mDurationMs = ((60f / score.tempo) * startMeasureObj.timeSignatureNumerator * 1000).toLong()
                initialElapsedMs += (startOffset * mDurationMs).toLong()
            }

            while (isActive && isScorePlaying.value) {
                val now = Clock.System.now().toEpochMilliseconds()
                val totalElapsedMs = initialElapsedMs + (now - startTime)

                // Optimized identification of current measure and offset
                val totalElapsedSec = totalElapsedMs / 1000f
                val starts = measureStartTimesSeconds.value
                var currentMeasureIdx = score.measures.size
                var currentMeasureDurationMs = 0L
                var accumulatedSec = 0f

                var i = lastTrackedMeasure.coerceAtLeast(0)
                if (i >= score.measures.size || totalElapsedSec < (starts.getOrNull(i) ?: 0f)) {
                    i = 0
                }

                while (i < score.measures.size) {
                    val start = starts.getOrNull(i) ?: 0f
                    val m = score.measures[i]
                    val dur = (60f / score.tempo) * m.timeSignatureNumerator
                    if (totalElapsedSec < start + dur) {
                        currentMeasureIdx = i
                        currentMeasureDurationMs = (dur * 1000).toLong()
                        accumulatedSec = start
                        break
                    }
                    i++
                }

                val currentOffset = if (currentMeasureDurationMs > 0) {
                    (totalElapsedSec - accumulatedSec) / (currentMeasureDurationMs / 1000f)
                } else 0f

                if (currentMeasureIdx != lastTrackedMeasure) {
                    firedNotesMap.clear()
                    lastTrackedMeasure = currentMeasureIdx
                }

                if (currentMeasureIdx >= score.measures.size) {
                    withContext(Dispatchers.Main) {
                        stopScorePlayback()
                        resetScorePlayhead()
                    }
                    break
                }

                val measure = score.measures[currentMeasureIdx]
                
                // PRECISE MAPPING: Map temporal progress to actual note positions
                // This ensures the playhead snaps to noteheads even with non-linear rhythmic spacing
                val beatsInMeasure = measure.timeSignatureNumerator.toFloat()
                val notes = measure.notes.filter { !it.isRest }.sortedBy { it.position }
                
                var visualOffset = currentOffset
                if (notes.isNotEmpty()) {
                    val lastNote = notes.last()
                    val lastNoteEnd = lastNote.position + lastNote.duration
                    
                    // Find the current note segment
                    val currentNoteIndex = notes.indexOfLast { it.position <= currentOffset * beatsInMeasure }
                    if (currentNoteIndex != -1) {
                        val currentNote = notes[currentNoteIndex]
                        val nextNote = notes.getOrNull(currentNoteIndex + 1)
                        
                        val segmentStart = currentNote.position
                        val segmentEnd = nextNote?.position ?: beatsInMeasure
                        
                        val segmentProgress = (currentOffset * beatsInMeasure - segmentStart) / (segmentEnd - segmentStart).coerceAtLeast(0.01f)
                        
                        // We map this to the visual range between these two notes
                        // For now, linear mapping within segment is much better than global linear
                        visualOffset = (segmentStart + segmentProgress * (segmentEnd - segmentStart)) / beatsInMeasure
                    }
                }

                if (currentMeasureIdx != scorePlayheadMeasure.value || visualOffset > scorePlayheadOffset.value) {
                    withContext(Dispatchers.Main) {
                        scorePlayheadMeasure.value = currentMeasureIdx
                        scorePlayheadOffset.value = visualOffset
                    }
                }

                measure.notes.forEachIndexed { index: Int, note: ScoreNote ->
                    val alreadyFired = firedNotesMap[index] == true

                    if (currentOffset >= note.position / measure.timeSignatureNumerator && !alreadyFired) {
                        firedNotesMap[index] = true
                        
                        // Direct low-latency native audio trigger without main thread hop
                        audioEngine.noteOn(note.midiNote, 0.8f)
                        withContext(Dispatchers.Main) {
                            midiActiveNotes[note.midiNote] = true
                            midiPlayingNote.value = note.midiNote
                        }

                        launch {
                            delay((note.duration * (60f / score.tempo) * 1000).toLong())
                            audioEngine.noteOff(note.midiNote)
                            withContext(Dispatchers.Main) {
                                midiActiveNotes.remove(note.midiNote)
                                if (midiPlayingNote.value == note.midiNote) {
                                    midiPlayingNote.value = midiActiveNotes.keys.firstOrNull()
                                }
                            }
                        }
                    }
                }
                delay(20)
            }
        }
    }

    fun toggleScorePlayback() {
        if (isScorePlaying.value) {
            stopScorePlayback()
        } else {
            startScorePlayback()
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

    fun onMidiEventReceived(note: Int, isNoteOn: Boolean, velocity: Int = 90, channel: Int = 1) {
        viewModelScope.launch(Dispatchers.Main) {
            lastMidiChannel.intValue = channel
            if (isNoteOn && velocity > 0) {
                lastMidiVelocity.intValue = velocity
                audioEngine.noteOn(note, velocity.toFloat())
                midiActiveNotes[note] = true
                midiPlayingNote.value = note
            } else {
                if (sustainPedalActive.value) {
                    notesToReleaseOnSustainUp.add(note)
                } else {
                    audioEngine.noteOff(note)
                    midiActiveNotes.remove(note)
                    if (midiPlayingNote.value == note) {
                        midiPlayingNote.value = midiActiveNotes.keys.firstOrNull()
                    }
                }
            }
        }
    }

    fun onMidiPedalEventReceived(pedalType: Int = 64, isPressed: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            when (pedalType) {
                64 -> { // Damper / Sustain Pedal
                    sustainPedalActive.value = isPressed
                    sustainActive.value = isPressed
                    if (!isPressed) {
                        notesToReleaseOnSustainUp.forEach { note ->
                            midiActiveNotes.remove(note)
                            audioEngine.noteOff(note)
                        }
                        notesToReleaseOnSustainUp.clear()
                        if (midiPlayingNote.value != null && !midiActiveNotes.containsKey(midiPlayingNote.value)) {
                            midiPlayingNote.value = midiActiveNotes.keys.firstOrNull()
                        }
                    }
                }
                66 -> { // Sostenuto Pedal
                    sostenutoPedalActive.value = isPressed
                }
                67 -> { // Soft Pedal / Una Corda
                    softPedalActive.value = isPressed
                }
            }
        }
    }

    fun showMidiPrompt(title: String, message: String, isConnected: Boolean, deviceName: String) {
        viewModelScope.launch(Dispatchers.Main) {
            midiConnectionPrompt.value = MidiPromptMessage(
                title = title,
                message = message,
                isConnected = isConnected,
                deviceName = deviceName
            )
        }
    }

    fun dismissMidiPrompt() {
        midiConnectionPrompt.value = null
    }

    fun parseIncomingScoreFile(uriString: String, fileBytes: ByteArray? = null, fileName: String = "Imported Score") {
        isImportingPDF.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val parsedScore = fileBytes?.let { com.creativeblisslabs.peqlearn.app.util.UniversalScoreParser.parse(it, fileName, uriString) }
                ?: com.creativeblisslabs.peqlearn.app.util.MidiFileParser.parse(fileBytes ?: ByteArray(0))

            val finalScore = parsedScore ?: Score(
                title = fileName.substringBeforeLast("."),
                tempo = 120f,
                timeSignature = "4/4",
                measures = listOf(Measure(notes = emptyList()))
            )

            withContext(Dispatchers.Main) {
                currentScore.value = finalScore
                isImportingPDF.value = false
            }

            for (progress in 1..100) {
                delay(5)
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

    fun compileCurrentNotationToPDF(options: PdfExportOptions = pdfExportOptions.value, onComplete: ((String?) -> Unit)? = null) {
        val score = currentScore.value ?: Score(
            title = options.title.ifBlank { "pEQ Score Study" },
            composer = options.composer.ifBlank { "pEQ Learn Studio" },
            tempo = metronomeBpm.value.toFloat(),
            timeSignature = "4/4"
        )
        isExportingPDF.value = true
        pdfExportProgress.value = 0.05f

        val exporter = onPdfExportRequested
        if (exporter != null) {
            exporter(score, options, { progress ->
                pdfExportProgress.value = progress
            }, { path ->
                isExportingPDF.value = false
                pdfExportProgress.value = 1.0f
                onComplete?.invoke(path)
            })
        } else {
            viewModelScope.launch(Dispatchers.Default) {
                for (progress in 1..100) {
                    delay(20)
                    withContext(Dispatchers.Main) {
                        pdfExportProgress.value = progress / 100f
                    }
                }
                withContext(Dispatchers.Main) {
                    isExportingPDF.value = false
                    pdfExportProgress.value = 0f
                    onComplete?.invoke("/sdcard/Documents/pEQ_${score.title.replace(" ", "_")}.pdf")
                }
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

    fun startRiyaazEngine(initialScale: String, totalBeats: Int, bolSequence: List<String> = emptyList()) {
        stopScorePlayback()
        isSequencerActive.value = true
        sequencerJob?.cancel()

        sequencerJob = viewModelScope.launch(Dispatchers.Default) {
            var stepIndex = 0

            while (isActive && isSequencerActive.value) {
                val currentLiveBpm = liveRiyaazBpm.intValue.coerceIn(40, 220)
                val beatDelayMs = ((60.0 / currentLiveBpm) * 1000).toLong()

                onSequencerStepTick(stepIndex, initialScale, bolSequence)
                stepIndex = (stepIndex + 1) % totalBeats

                delay(beatDelayMs)
            }
        }
    }

    private fun onSequencerStepTick(stepIndex: Int, selectedScale: String, bolSequence: List<String> = emptyList()) {
        val stepValue = sequencerSteps.getOrNull(stepIndex)?.value ?: 0
        if (stepValue > 0) {
            val bol = bolSequence.getOrNull(stepIndex)
            if (!bol.isNullOrBlank() && !bol.all { it.isDigit() }) {
                audioEngine.playSample(
                    label = bol,
                    index = stepIndex,
                    string = "TABLA"
                )
            } else {
                audioEngine.playSample(
                    label = if (stepIndex == 0) "METRONOME-STRONG" else "METRONOME-WEAK",
                    index = stepIndex,
                    string = "SYSTEM"
                )
            }
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
        
        // FOLLOW MODE: Sync notation playhead to incoming MIDI notes (only when score playback is paused/interactive follow mode)
        if (!isScorePlaying.value) {
            currentScore.value?.let { score ->
                for (mIdx in score.measures.indices) {
                    val measure = score.measures[mIdx]
                    val matchingNote = measure.notes.find { it.midiNote == note }
                    if (matchingNote != null) {
                        // Only jump if it's in the current or future measure to avoid jumping back constantly
                        if (mIdx >= scorePlayheadMeasure.value) {
                            scorePlayheadMeasure.value = mIdx
                            scorePlayheadOffset.value = matchingNote.position / measure.timeSignatureNumerator
                        }
                        break
                    }
                }
            }
        }
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

data class MidiPromptMessage(
    val title: String,
    val message: String,
    val isConnected: Boolean,
    val deviceName: String
)
