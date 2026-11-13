package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import kotlin.math.*
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
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
import com.creativeblisslabs.peqlearn.app.video.PdfExportConfig
import com.creativeblisslabs.peqlearn.app.video.VideoExportConfig
import com.creativeblisslabs.peqlearn.app.video.compilePdfPlatformSpecific
import com.creativeblisslabs.peqlearn.app.video.exportVideoPlatformSpecific
import kotlin.time.TimeSource

data class VideoExportHistoryItem(
    val id: String = "${Clock.System.now().toEpochMilliseconds()}",
    val title: String,
    val resolution: String,
    val fps: String,
    val audioSource: String,
    val timestampMs: Long = Clock.System.now().toEpochMilliseconds(),
    val status: String = "In Progress", // "In Progress", "Completed", "Failed"
    val progress: Float = 0f,
    val filePath: String? = null
)

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
    val enabledTrackIndices = mutableStateMapOf<Int, Boolean>()

    // Per-MIDI-track volume (trackIndex -> 0.0f..1.0f, default 1.0f)
    val trackVolumes = mutableStateMapOf<Int, Float>()

    // Per-Instrument volume (instrumentTitle -> 0.0f..1.0f, default 1.0f)
    val instrumentVolumes = mutableStateMapOf<String, Float>()

    // Live per-note color map so Piano, Guitar, Harmonium, Violin, Flute & Sitar flash the EXACT same hand/track color as the Waterfall & Staff!
    val activeNoteTrackColors = mutableStateMapOf<Int, androidx.compose.ui.graphics.Color>()

    // Configurable Measures Per Row in Notation Viewer (1, 2, 3, or 4; default 2 for clear tablet/phone readability)
    val notationMeasuresPerRow = mutableIntStateOf(2)

    // Current PDF page index when viewing a raw PDF score
    val currentPdfPageIndex = mutableIntStateOf(0)
    val totalPdfPageCount = mutableIntStateOf(1)

    // Thread-safe seek signal so scrubbing the slider while playing updates scoreElapsedSec in-place without pausing!
    @kotlin.concurrent.Volatile
    private var pendingSeekScoreSec: Float = -1f

    private val sostenutoCapturedNotes = HashSet<Int>()
    val leftHandColor = mutableStateOf(Color(0xFF6C63FF))
    val rightHandColor = mutableStateOf(Color(0xFF4A90E2))
    
    // Controls whether the articulated 3D/2D Human Left Hand (LH) & Right Hand (RH) Finger Guides are drawn over the Piano Keyboard
    val showLeftHandGuide = mutableStateOf(true)
    val showRightHandGuide = mutableStateOf(true)

    val transposition = mutableIntStateOf(0)
    val octaveShift = mutableIntStateOf(0)
    val isSplitStaff = mutableStateOf(true)
    val isVerticalScroll = mutableStateOf(false)
    val metronomeEnabled = mutableStateOf(false)
    private var metronomeJob: Job? = null

    // Hand Practice Mode: "BOTH", "RIGHT", "LEFT"
    val activeHandMode = mutableStateOf("BOTH")

    fun setHandPracticeMode(mode: String) {
        activeHandMode.value = mode
        when (mode.uppercase()) {
            "RIGHT" -> {
                // Right hand only: Mute and hide Left hand
                isLeftHandMuted.value = true
                isLeftHandHidden.value = true
                isRightHandMuted.value = false
                isRightHandHidden.value = false
            }
            "LEFT" -> {
                // Left hand only: Mute and hide Right hand
                isRightHandMuted.value = true
                isRightHandHidden.value = true
                isLeftHandMuted.value = false
                isLeftHandHidden.value = false
            }
            else -> {
                // Both hands: Unmute and show both
                isLeftHandMuted.value = false
                isLeftHandHidden.value = false
                isRightHandMuted.value = false
                isRightHandHidden.value = false
            }
        }
    }

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
    val showSuggestedFingerings = mutableStateOf(true)
    val showWaterfall = mutableStateOf(true)
    val visualizerLayoutMode = mutableStateOf("Split") // "Split", "KeyboardWaterfall", "Waterfall", "Notation", "Keyboard"
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
    val videoExportRenderPedals = mutableStateOf(true)
    // Audio source for video export: "MidiScore" (Synthesized from .midi), "AudioFile" (Selected via file picker), "None"
    val videoExportAudioSource = mutableStateOf("MidiScore")
    val videoExportExternalAudioUri = mutableStateOf<String?>(null)
    val videoExportExternalAudioName = mutableStateOf<String?>(null)
    
    // Piano & Notation sound engine mode: "SalamanderSamples", "AndroidMidi", or "SoundFont"
    val pianoAudioEngineMode = mutableStateOf("SalamanderSamples")

    // Video Export Queue & History item model
    val videoExportHistory = mutableStateListOf<VideoExportHistoryItem>()
    val isVideoExporting = mutableStateOf(false)
    val videoExportProgress = mutableFloatStateOf(0f)
    val videoExportFrameText = mutableStateOf("")
    val videoExportEtaText = mutableStateOf("")
  
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
    val stemSplitLog = mutableStateOf("")
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
    val songStructureAnalysis = mutableStateOf<com.creativeblisslabs.peqlearn.app.ai.AllInOneAnalysisResult?>(null)
    val isAnalyzingStructure = mutableStateOf(false)
    var onPickModelRequested: (((String, String) -> Unit) -> Unit)? = null

    // ─── ONNX Model Management Platform Callbacks ───────────────────────────
    /** Returns the absolute path of the models directory on disk */
    var onGetModelsDir: (() -> String)? = null
    /** Persists the ONNX registry state (custom models + active selections) to platform storage */
    var onPersistOnnxState: (() -> Unit)? = null
    /** Restores the ONNX registry state from platform storage */
    var onRestoreOnnxState: (() -> Unit)? = null
    /** Resolves a model file name to its full path on disk (filesDir or assets), null if missing */
    var onResolveModelPath: ((fileName: String) -> String?)? = null
    /** Reads raw bytes of an asset / resource file by name, null if missing */
    var onReadAssetBytes: ((assetName: String) -> ByteArray?)? = null
    var onListMidiAssetsRequested: (() -> List<String>)? = null
    var onListMidiAssets: (() -> List<String>)? = null

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
            "guitar" -> emptyList()
            "drums" -> listOf("Metal_Drums.sf2")
            "metronome" -> listOf("Metronom.sf2")
            "tanpura" -> listOf("Tanpura_drone.sf2")
            "tabla" -> listOf("tabla.sf2")
            "harmonium" -> listOf("Wetthasinghe_Harmnium_GM_Ver.sf2")
            else -> emptyList()
        }
    }

    fun getLoadedPresetNames(instrumentKey: String): List<String> =
        audioEngine.getLoadedPresetNames(instrumentKey).ifEmpty { listOf("Default Preset") }

    fun selectPresetByIndex(instrumentKey: String, index: Int): Pair<Int, Int>? =
        audioEngine.selectPresetByIndex(instrumentKey, index) ?: Pair(0, index)

    // --- Optimized Score Temporal States ---
    val measureStartTimesSeconds = derivedStateOf {
        val score = currentScore.value ?: return@derivedStateOf emptyList<Float>()
        val starts = mutableListOf<Float>()
        var current = 0f
        for (m in score.measures) {
            starts.add(current)
            current += getMeasureDurationSeconds(m, score.tempo)
        }
        starts
    }

    val scoreTotalDurationSeconds = derivedStateOf {
        val starts = measureStartTimesSeconds.value
        val score = currentScore.value
        if (score == null || starts.isEmpty()) 0f
        else {
            val lastMeasure = score.measures.last()
            starts.last() + getMeasureDurationSeconds(lastMeasure, score.tempo)
        }
    }

    val scoreCurrentPositionSeconds = derivedStateOf {
        val score = currentScore.value ?: return@derivedStateOf 0f
        val measureIdx = scorePlayheadMeasure.value
        val offset = scorePlayheadOffset.value
        val starts = measureStartTimesSeconds.value

        val startSec = starts.getOrNull(measureIdx) ?: 0f
        val m = score.measures.getOrNull(measureIdx)
        val mDur = if (m != null) getMeasureDurationSeconds(m, score.tempo) else 0f

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
            val dur = getMeasureDurationSeconds(m, score.tempo).coerceAtLeast(0.001f)

            if (totalSeconds < start + dur) {
                targetM = i
                targetO = ((totalSeconds - start) / dur).coerceIn(0f, 1f)
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
    private val padFlashJobs = mutableMapOf<String, Job>()

    /**
     * Playback Engine: Loops through the timeline bars sequentially (100% KMP commonMain)
     */
    fun startSongPlayback() {
        stopPatternPlayback() // Stop single pattern loop
        songSequencerJob?.cancel()

        val activeKit = selectedDrumKit.value ?: drumKits.firstOrNull()
        if (activeKit == null || songTimeline.isEmpty()) {
            isSongPlaybackActive.value = false
            return
        }

        isSongPlaybackActive.value = true

        songSequencerJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                val timeSource = TimeSource.Monotonic
                var nextStepMark = timeSource.markNow()

                while (isActive && isSongPlaybackActive.value) {
                    val timelineSnapshot = songTimeline.toList()
                    if (timelineSnapshot.isEmpty()) break

                    val startBar = currentPlayingBarIndex.intValue.coerceIn(0, timelineSnapshot.lastIndex)

                    for (barIdx in startBar until timelineSnapshot.size) {
                        if (!isActive || !isSongPlaybackActive.value) break

                        currentPlayingBarIndex.intValue = barIdx
                        val currentBar = timelineSnapshot[barIdx]
                        val pattern = drumTutorials.find { it.name == currentBar.patternName }

                        if (pattern != null && pattern.steps.isNotEmpty()) {
                            pattern.steps.forEachIndexed { stepIdx, activeTokens ->
                                if (!isActive || !isSongPlaybackActive.value) return@launch

                                currentStepIndex.intValue = stepIdx

                                val currentBpm = liveTutorialBpm.intValue.coerceIn(40, 240)
                                val stepDurationMs = ((60_000.0 / currentBpm) / 4.0).toLong()

                                activeTokens.forEach { token ->
                                    val baseLabel = token.uppercase().trim()
                                    val targetIndex = activeKit.labels.indexOfFirst {
                                        it.uppercase().contains(baseLabel)
                                    }

                                    if (targetIndex != -1) {
                                        val literalLabel = activeKit.labels[targetIndex]
                                        audioEngine.playSample(
                                            label = literalLabel,
                                            index = targetIndex,
                                            string = "DRUM"
                                        )

                                        padFlashJobs[literalLabel]?.cancel()
                                        sequencerTriggeredPads[literalLabel] = true
                                        padFlashJobs[literalLabel] = launch {
                                            delay(70L)
                                            sequencerTriggeredPads[literalLabel] = false
                                        }
                                    }
                                }

                                // Drift-free KMP scheduling using Long ms
                                val elapsedMs = nextStepMark.elapsedNow().inWholeMilliseconds
                                val sleepMs = stepDurationMs - elapsedMs
                                if (sleepMs > 0L) {
                                    delay(sleepMs)
                                }
                                nextStepMark = timeSource.markNow()
                            }
                        } else {
                            // Safe fallback for unassigned or empty bars (1 full 4/4 bar)
                            val currentBpm = liveTutorialBpm.intValue.coerceIn(40, 240)
                            val barDurationMs = ((60_000.0 / currentBpm) * 4.0).toLong()

                            val elapsedMs = nextStepMark.elapsedNow().inWholeMilliseconds
                            val sleepMs = barDurationMs - elapsedMs
                            if (sleepMs > 0L) {
                                delay(sleepMs)
                            }
                            nextStepMark = timeSource.markNow()
                        }
                    }

                    currentPlayingBarIndex.intValue = 0
                }
            } finally {
                currentStepIndex.intValue = -1
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

// var currentScore = mutableStateOf<com.creativeblisslabs.peqlearn.app.ui.screens.Score?>(null)
var visualizerStageCaptureController: com.creativeblisslabs.peqlearn.app.ui.screens.VisualizerStageCaptureController? = null
    val scorePlayheadMeasure = mutableIntStateOf(0)

    val scorePlayheadOffset = mutableFloatStateOf(0f)
    val isScorePlaying = mutableStateOf(false)
    val practiceMeasureRange = mutableStateOf<Pair<Int, Int>?>(null) // Optional (startMeasureIdx, endMeasureIdx) for measure-by-measure looping
    val isPracticeLoopEnabled = mutableStateOf(false)
    private var scorePlaybackJob: Job? = null

    private val firedNotesMap = mutableMapOf<Int, Boolean>()
    private val firedPedalsMap = mutableMapOf<Int, Boolean>()
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
    val liveLooper = com.creativeblisslabs.peqlearn.app.midi.LiveLooper(
        onPlaybackNoteOn = { pitch, vel -> audioEngine.noteOn(pitch, vel) },
        onPlaybackNoteOff = { pitch -> audioEngine.noteOff(pitch) }
    )
    val virtualMidiConnected = mutableStateOf(false)
    val midiConnectedDevices = androidx.compose.runtime.mutableStateListOf<String>()
    val midiActiveNotes = mutableStateMapOf<Int, Boolean>()
    val playAlongEngine = com.creativeblisslabs.peqlearn.app.ui.screens.PlayAlongEngine()

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
private fun composeColorToArgbInt(color: androidx.compose.ui.graphics.Color): Int {
        val a = (color.alpha * 255f + 0.5f).toInt().coerceIn(0, 255)
        val r = (color.red * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun mapGmDrumMidiToLabel(midiNote: Int): String? {
        return when (midiNote) {
            35, 36 -> "KICK"
            38, 40, 37 -> "SNARE"
            42, 44 -> "HH_CLOSED"
            46 -> "HH_OPEN"
            49, 55 -> "CRASH"
            57, 52 -> "CRASH2"
            51, 53, 59 -> "RIDE"
            41, 43 -> "FLOOR"
            45, 47 -> "TOM_LO"
            48, 50 -> "TOM_HI"
            39 -> "CLAP"
            56 -> "PERC"
            69, 70, 82 -> "SHAKER"
            else -> null
        }
    }

    /**
     * Synchronizes [enabledTrackIndices] with the currently loaded [Score.tracks].
     */
    fun syncScoreTracks(score: Score?) {
        enabledTrackIndices.clear()
        if (score != null && score.tracks.isNotEmpty()) {
            score.tracks.forEach { tr ->
                enabledTrackIndices[tr.trackIndex] = tr.isEnabled
            }
        } else {
            enabledTrackIndices[0] = true
            enabledTrackIndices[1] = true
        }
    }

    /**
     * Toggles a specific MIDI track on/off and immediately silences any hanging notes if muted.
     */
    fun setTrackEnabled(trackIndex: Int, enabled: Boolean) {
        enabledTrackIndices[trackIndex] = enabled
        if (!enabled) {
            audioEngine.allNotesOff()
            midiActiveNotes.clear()
            midiPlayingNote.value = null
        }
    }

    private fun getMeasureDurationSeconds(measure: Measure, tempoBpm: Float): Float {
        val safeTempo = tempoBpm.coerceAtLeast(20f)
        val quarterBeats = measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())
        return (60f / safeTempo) * quarterBeats
    }
    /**
     * Unified Logical Hand & Track Assignment:
     * - If the score has explicit RH (voice 0) and LH (voice 1) tracks, respects voice 0/1 unless the pitch crosses
     *   into the extreme opposite register (< 48 C3 is always LH, > 72 C5 is always RH).
     * - Returns Pair(isRightHand, resolvedColor).
     */
    fun resolveNoteHandAndColor(
        note: ScoreNote,
        score: Score?,
        rhColor: androidx.compose.ui.graphics.Color = rightHandColor.value,
        lhColor: androidx.compose.ui.graphics.Color = leftHandColor.value
    ): Pair<Boolean, androidx.compose.ui.graphics.Color> {
        val trackInfo = score?.tracks?.firstOrNull { it.trackIndex == note.voice }
        if (trackInfo?.channel == 9) {
            return true to androidx.compose.ui.graphics.Color(0xFFFF5252) // Drum track crimson
        }
        if (note.voice >= 2) {
            val paletteHex = com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX
            val col = androidx.compose.ui.graphics.Color(paletteHex[note.voice % paletteHex.size])
            return (note.midiNote >= 60) to col
        }

        val hasDifferentiatedVoices = score?.measures?.any { m ->
            m.notes.any { it.voice == 0 } && m.notes.any { it.voice == 1 }
        } == true

        val isRh = when {
            note.midiNote < 48 -> false
            note.midiNote > 72 -> true
            hasDifferentiatedVoices -> note.voice == 0
            else -> note.midiNote >= 60
        }
        return isRh to (if (isRh) rhColor else lhColor)
    }

    /**
     * Steps to the Next Measure (for MIDI/MusicXML scores) or Next Page (for PDF scores).
     */
    fun stepToNextMeasureOrPage() {
        val score = currentScore.value ?: return
        if (score.isPdfScore && score.measures.all { it.notes.isEmpty() }) {
            currentPdfPageIndex.intValue = (currentPdfPageIndex.intValue + 1)
                .coerceAtMost((totalPdfPageCount.intValue - 1).coerceAtLeast(0))
        } else if (score.measures.isNotEmpty()) {
            val nextM = (scorePlayheadMeasure.value + 1).coerceAtMost(score.measures.lastIndex)
            seekToPosition(nextM, 0f)
        }
    }

    /**
     * Steps to the Previous Measure (for MIDI/MusicXML scores) or Previous Page (for PDF scores).
     * If already past 20% of the current measure, rewinds to the start of the current measure first.
     */
    fun stepToPreviousMeasureOrPage() {
        val score = currentScore.value ?: return
        if (score.isPdfScore && score.measures.all { it.notes.isEmpty() }) {
            currentPdfPageIndex.intValue = (currentPdfPageIndex.intValue - 1).coerceAtLeast(0)
        } else if (score.measures.isNotEmpty()) {
            val targetM = if (scorePlayheadOffset.value > 0.25f) {
                scorePlayheadMeasure.value
            } else {
                (scorePlayheadMeasure.value - 1).coerceAtLeast(0)
            }
            seekToPosition(targetM, 0f)
        }
    }

    fun seekToPosition(measure: Int, offset: Float) {
        val score = currentScore.value ?: return
        val clampedMeasure = measure.coerceIn(0, (score.measures.size - 1).coerceAtLeast(0))
        val clampedOffset = offset.coerceIn(0f, 1f)

        var accumulatedSec = 0f
        for (i in 0 until clampedMeasure) {
            val m = score.measures.getOrNull(i) ?: break
            accumulatedSec += getMeasureDurationSeconds(m, score.tempo)
        }
        val targetMeasure = score.measures.getOrNull(clampedMeasure)
        if (targetMeasure != null) {
            accumulatedSec += clampedOffset * getMeasureDurationSeconds(targetMeasure, score.tempo)
        }

        // Signal the active playback coroutine to jump immediately to `accumulatedSec` on its next 6ms tick!
        pendingSeekScoreSec = accumulatedSec

        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
            scorePlayheadMeasure.value = clampedMeasure
            scorePlayheadOffset.value = clampedOffset
            currentPosition.value = (accumulatedSec * 1000f).toLong()
            midiActiveNotes.clear()
            activeNoteTrackColors.clear()
            midiPlayingNote.value = null
        }

        audioEngine.allNotesOff()
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
        isPlaying.value = false
        scorePlaybackJob?.cancel()
        scorePlaybackJob = null

        audioEngine.allNotesOff()

        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
            midiActiveNotes.clear()
            activeNoteTrackColors.clear()
            midiPlayingNote.value = null
            firedNotesMap.clear()
            firedPedalsMap.clear()
            notesToReleaseOnSustainUp.clear()
            sostenutoCapturedNotes.clear()
            sustainPedalActive.value = false
            sustainActive.value = false
            sostenutoPedalActive.value = false
            softPedalActive.value = false
        }
    }

    fun resetScorePlayhead() {
        stopScorePlayback()
        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
            midiActiveNotes.clear()
            activeNoteTrackColors.clear()
            scorePlayheadMeasure.value = 0
            scorePlayheadOffset.value = 0f
            currentPosition.value = 0L
        }
        audioEngine.seekTo(0L)
    }

    // --- USB MIDI INPUT GATE & INTERACTIVE PLAY-ALONG MODES ---
    val isUsbMidiInputEnabled = mutableStateOf(true)
    val isGuidedPlayAlongActive = mutableStateOf(false)
    // Modes: "WAIT_FOR_NOTE" (Step-by-Step), "MEASURE_FOLLOWUP" (Teacher Bar -> Student Bar),
    //        "CONTINUOUS" (Live Sync), "TEMPO_RAMP" (+5% Speed per Clean Bar)
    val playAlongPracticeMode = mutableStateOf("WAIT_FOR_NOTE")
    val playAlongPhaseBanner = mutableStateOf("") // e.g. "WATCH & LISTEN (Bar 1)" or "YOUR TURN! Play Highlighted Keys"
    val guidedTargetNotes = mutableStateListOf<Int>() // Notes currently flashing on visualizer waiting for user
    val guidedSatisfiedNotes = mutableStateListOf<Int>() // Notes already hit in the current chord step

    fun toggleUsbMidiInput() {
        val nextState = !isUsbMidiInputEnabled.value
        isUsbMidiInputEnabled.value = nextState
        if (!nextState) {
            // Immediately clear any held USB MIDI keys/pedals from visualizer & audio engine
            notesToReleaseOnSustainUp.clear()
            sostenutoCapturedNotes.clear()
            sustainPedalActive.value = false
            sustainActive.value = false
            sostenutoPedalActive.value = false
            softPedalActive.value = false
            if (!isScorePlaying.value) {
                midiActiveNotes.clear()
                midiPlayingNote.value = null
                audioEngine.allNotesOff()
            }
        }
    }

    /**
     * Maps a track's GM Program number (0..127) and channel to the appropriate AndroidAudioEngine instrument key.
     */
    private fun resolveTrackInstrumentKey(measureScore: Score, voiceIndex: Int): String {
        val track = measureScore.tracks.firstOrNull { it.trackIndex == voiceIndex }
            ?: return selectedInstrument.value
        if (track.channel == 9) return "DRUM"
        return when (track.program.coerceIn(0, 127)) {
            in 0..7 -> "PIANO_ACOUSTIC"
            in 8..15 -> "PIANO_ELECTRIC"
            in 16..23 -> "HARMONIUM"
            24 -> "GUITAR_CLASSICAL"
            25 -> "GUITAR_ACOUSTIC"
            in 26..31 -> "GUITAR_ELECTRIC"
            in 32..39 -> "GUITAR_ACOUSTIC"
            in 40..55 -> "VIOLIN"
            in 72..79 -> "FLUTE"
            104 -> "SITAR"
            in 112..119 -> "TABLA"
            else -> selectedInstrument.value
        }
    }

    fun startScorePlayback() {
        val score = currentScore.value ?: return
        if (isScorePlaying.value) return

        if (enabledTrackIndices.isEmpty()) {
            syncScoreTracks(score)
        }

        isScorePlaying.value = true
        isPlaying.value = true

        scorePlaybackJob?.cancel()
        scorePlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            val numMeasures = score.measures.size
            if (numMeasures == 0) {
                withContext(Dispatchers.Main) { stopScorePlayback() }
                return@launch
            }

            val drumTrackIndices = score.tracks.filter { it.channel == 9 }.map { it.trackIndex }.toSet()

            val measureStartsSec = FloatArray(numMeasures)
            val measureDurationsSec = FloatArray(numMeasures)
            val measureQuarterBeats = FloatArray(numMeasures)
            var runningSec = 0f
            for (i in 0 until numMeasures) {
                val m = score.measures[i]
                val qBeats = (m.timeSignatureNumerator.toFloat() * (4f / m.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
                val durSec = (60f / score.tempo.coerceAtLeast(20f)) * qBeats
                measureStartsSec[i] = runningSec
                measureDurationsSec[i] = durSec
                measureQuarterBeats[i] = qBeats
                runningSec += durSec
            }

            val startMeasure = scorePlayheadMeasure.value.coerceIn(0, numMeasures - 1)
            val startOffset = scorePlayheadOffset.value.coerceIn(0f, 1f)
            var scoreElapsedSec = measureStartsSec[startMeasure] + (startOffset * measureDurationsSec[startMeasure])

            firedNotesMap.clear()
            firedPedalsMap.clear()
            lastTrackedMeasure = startMeasure
            val initialBeat = startOffset * measureQuarterBeats[startMeasure]
            score.measures[startMeasure].notes.forEachIndexed { idx, note ->
                if (note.position < initialBeat - 0.04f) firedNotesMap[idx] = true
            }
            score.measures[startMeasure].pedalEvents.forEachIndexed { pIdx, ev ->
                if (ev.position < initialBeat - 0.04f) firedPedalsMap[pIdx] = true
            }

            val scheduledNoteReleases = HashMap<Int, Pair<Float, Int>>(32)
            val pitchesToActivate = ArrayList<Int>(16)
            val pitchesToRelease = ArrayList<Int>(16)
            val drumLabelsToTrigger = ArrayList<String>(4)

            // Measure Follow-Up (Call & Response) State:
            // false = Teacher Demo Phase (app plays the measure), true = Student Follow-Up Phase (user plays the measure)
            var isStudentFollowUpTurn = false

            val timeSource = TimeSource.Monotonic
            val playbackOriginMark = timeSource.markNow()
            var lastLoopMark = playbackOriginMark
            var lastUiBroadcastMs = 0L

            while (isActive && isScorePlaying.value) {
                val elapsedDuration = lastLoopMark.elapsedNow()
                lastLoopMark = timeSource.markNow()
                val wallDeltaSec = elapsedDuration.inWholeMicroseconds / 1_000_000f

                // Declare this at the top of the loop scope so step 2, 3, and 4 can all access it
                val isWaitingForUserStep = isGuidedPlayAlongActive.value &&
                        guidedTargetNotes.isNotEmpty() &&
                        (playAlongPracticeMode.value == "WAIT_FOR_NOTE" ||
                                (playAlongPracticeMode.value == "MEASURE_FOLLOWUP" && isStudentFollowUpTurn))

                // 0. Check if user scrubbed the timeline slider or tapped Prev/Next Measure while playing!
                val requestedSeekSec = pendingSeekScoreSec
                if (requestedSeekSec >= 0f) {
                    pendingSeekScoreSec = -1f
                    scoreElapsedSec = requestedSeekSec
                    scheduledNoteReleases.forEach { (p, _) -> audioEngine.noteOff(p) }
                    scheduledNoteReleases.clear()
                    firedNotesMap.clear()
                    firedPedalsMap.clear()

                    var seekMIdx = 0
                    while (seekMIdx < numMeasures - 1 &&
                        scoreElapsedSec >= measureStartsSec[seekMIdx] + measureDurationsSec[seekMIdx]
                    ) {
                        seekMIdx++
                    }
                    lastTrackedMeasure = seekMIdx
                    val mStart = measureStartsSec[seekMIdx]
                    val mDur = measureDurationsSec[seekMIdx].coerceAtLeast(0.001f)
                    val seekBeat = ((scoreElapsedSec - mStart) / mDur).coerceIn(0f, 1f) * measureQuarterBeats[seekMIdx]
                    score.measures[seekMIdx].notes.forEachIndexed { idx, note ->
                        if (note.position < seekBeat - 0.04f) firedNotesMap[idx] = true
                    }
                    score.measures[seekMIdx].pedalEvents.forEachIndexed { pIdx, ev ->
                        if (ev.position < seekBeat - 0.04f) firedPedalsMap[pIdx] = true
                    }
                } else {
                    val speed = playbackSpeed.value.coerceIn(0.25f, 2.5f)
                    if (!isWaitingForUserStep) {
                        scoreElapsedSec += wallDeltaSec * speed
                    }
                }
                val totalElapsedMs = (scoreElapsedSec * 1000f).toLong()

                // 1. Identify current measure index
                var currentMeasureIdx = lastTrackedMeasure.coerceIn(0, numMeasures - 1)
                if (scoreElapsedSec < measureStartsSec[currentMeasureIdx]) {
                    currentMeasureIdx = 0
                }
                while (currentMeasureIdx < numMeasures &&
                    scoreElapsedSec >= measureStartsSec[currentMeasureIdx] + measureDurationsSec[currentMeasureIdx]
                ) {
                    currentMeasureIdx++
                }

                // Handle Measure Follow-Up (Teacher Demo -> Student Turn per Measure)
                if (isGuidedPlayAlongActive.value && playAlongPracticeMode.value == "MEASURE_FOLLOWUP" &&
                    currentMeasureIdx != lastTrackedMeasure
                ) {
                    if (!isStudentFollowUpTurn) {
                        // Teacher just finished playing `lastTrackedMeasure` -> Rewind to start of `lastTrackedMeasure` for Student Turn!
                        isStudentFollowUpTurn = true
                        currentMeasureIdx = lastTrackedMeasure
                        scoreElapsedSec = measureStartsSec[currentMeasureIdx]
                        firedNotesMap.clear()
                        firedPedalsMap.clear()
                        scheduledNoteReleases.forEach { (p, _) -> audioEngine.noteOff(p) }
                        scheduledNoteReleases.clear()
                        withContext(Dispatchers.Main) {
                            playAlongPhaseBanner.value = "YOUR TURN! Play Bar ${currentMeasureIdx + 1} on USB Keyboard"
                            midiActiveNotes.clear()
                        }
                    } else {
                        // Student finished Bar `lastTrackedMeasure` -> Advance to Teacher Demo of next bar!
                        isStudentFollowUpTurn = false
                        withContext(Dispatchers.Main) {
                            playAlongPhaseBanner.value = "LISTEN & WATCH: Bar ${currentMeasureIdx + 1}"
                        }
                    }
                }

                val loopRange = practiceMeasureRange.value
                val isMeasureLoopActive = isPracticeLoopEnabled.value && loopRange != null
                val isAbLoopActive = abRepeatActive.value && abRepeatPointA.value != -1L && abRepeatPointB.value != -1L && abRepeatPointB.value > abRepeatPointA.value
                val isLoopActive = isMeasureLoopActive || isAbLoopActive

                val targetEndMeasure = if (isMeasureLoopActive) loopRange!!.second else numMeasures - 1
                val isAbRepeatBoundaryReached = isAbLoopActive && totalElapsedMs >= abRepeatPointB.value

                if (currentMeasureIdx > targetEndMeasure || currentMeasureIdx >= numMeasures || isAbRepeatBoundaryReached) {
                    if (isLoopActive) {
                        // If Tempo Ramp mode is active and accuracy >= 85%, boost speed by +5% up to 1.25x!
                        if (isGuidedPlayAlongActive.value && playAlongPracticeMode.value == "TEMPO_RAMP") {
                            val acc = playAlongEngine.scorecard.value.accuracyPercentage
                            if (acc >= 85) {
                                val nextSpd = (playbackSpeed.value + 0.05f).coerceAtMost(1.25f)
                                withContext(Dispatchers.Main) {
                                    playbackSpeed.value = (kotlin.math.round(nextSpd * 100f) / 100f)
                                    playAlongPhaseBanner.value = "Tempo Ramp +5% -> ${(playbackSpeed.value * 100).toInt()}% Speed!"
                                }
                            }
                        }
                        if (isAbLoopActive && !isMeasureLoopActive) {
                            val loopStartMs = abRepeatPointA.value
                            scoreElapsedSec = (loopStartMs / 1000f).coerceAtLeast(0f)
                            currentMeasureIdx = 0
                            while (currentMeasureIdx < numMeasures - 1 &&
                                scoreElapsedSec >= measureStartsSec[currentMeasureIdx] + measureDurationsSec[currentMeasureIdx]
                            ) {
                                currentMeasureIdx++
                            }
                        } else {
                            val loopStartMeasure = loopRange!!.first.coerceIn(0, numMeasures - 1)
                            currentMeasureIdx = loopStartMeasure
                            scoreElapsedSec = measureStartsSec[loopStartMeasure]
                        }
                        firedNotesMap.clear()
                        firedPedalsMap.clear()
                        lastTrackedMeasure = currentMeasureIdx
                        scheduledNoteReleases.forEach { (p, _) -> audioEngine.noteOff(p) }
                        scheduledNoteReleases.clear()
                        pitchesToActivate.clear()
                        pitchesToRelease.clear()
                    } else {
                        withContext(Dispatchers.Main) {
                            stopScorePlayback()
                            resetScorePlayhead()
                        }
                        break
                    }
                }

                if (currentMeasureIdx != lastTrackedMeasure) {
                    firedNotesMap.clear()
                    firedPedalsMap.clear()
                    lastTrackedMeasure = currentMeasureIdx
                }

                val mStartSec = measureStartsSec[currentMeasureIdx]
                val mDurSec = measureDurationsSec[currentMeasureIdx].coerceAtLeast(0.001f)
                val currentOffset = ((scoreElapsedSec - mStartSec) / mDurSec).coerceIn(0f, 1f)
                val beatsInMeasure = measureQuarterBeats[currentMeasureIdx]
                val currentBeatInMeasure = currentOffset * beatsInMeasure
                val measure = score.measures[currentMeasureIdx]

                pitchesToActivate.clear()
                pitchesToRelease.clear()
                drumLabelsToTrigger.clear()

                // 2. Release expired notes or notes belonging to a track that was just switched OFF
                val rhMuted = isRightHandMuted.value || isRightHandHidden.value
                val lhMuted = isLeftHandMuted.value || isLeftHandHidden.value
                val releaseIter = scheduledNoteReleases.entries.iterator()

                while (releaseIter.hasNext()) {
                    val entry = releaseIter.next()
                    val pitch = entry.key
                    val (releaseTimeSec, voiceIdx) = entry.value
                    val trackStillEnabled = enabledTrackIndices[voiceIdx] ?: true
                    val handMutedNow = (voiceIdx == 0 && rhMuted) || (voiceIdx == 1 && lhMuted)
                    val isWaitingForUserStep = isGuidedPlayAlongActive.value &&
                            guidedTargetNotes.isNotEmpty() &&
                            (playAlongPracticeMode.value == "WAIT_FOR_NOTE" ||
                                    (playAlongPracticeMode.value == "MEASURE_FOLLOWUP" && isStudentFollowUpTurn))

                    // Do not auto-expire target notes while paused waiting for the user's USB MIDI input!
                    if ((!isWaitingForUserStep && scoreElapsedSec >= releaseTimeSec) || !trackStillEnabled || handMutedNow) {
                        audioEngine.noteOff(pitch)
                        pitchesToRelease.add(pitch)
                        releaseIter.remove()
                    }
                }

                // 3. Fire due notes at the current beat
                val transOffset = transposition.value + (octaveShift.value * 12)
                val secPerBeat = 60f / score.tempo.coerceAtLeast(20f)
                val newlyQueuedTargets = ArrayList<Int>(8)

                // Inside Step 3 of startScorePlayback():
                val masterVol = masterVolume.value.coerceIn(0f, 1f)
                val rhColorSnap = rightHandColor.value
                val lhColorSnap = leftHandColor.value
                val activatedColors = HashMap<Int, androidx.compose.ui.graphics.Color>(8)

                if (!isWaitingForUserStep) {
                    measure.notes.forEachIndexed { index, note ->
                        if (note.isRest || firedNotesMap[index] == true) return@forEachIndexed
                        if (currentBeatInMeasure >= note.position) {
                            firedNotesMap[index] = true

                            val isTrackEnabled = enabledTrackIndices[note.voice] ?: true
                            if (!isTrackEnabled) return@forEachIndexed

                            val trackVol = (trackVolumes[note.voice] ?: 1.0f).coerceIn(0f, 1f)
                            val instVol = (instrumentVolumes[selectedInstrument.value] ?: 1.0f).coerceIn(0f, 1f)
                            val effectiveMasterVol = (masterVol * trackVol * instVol).coerceIn(0f, 1f)

                            val isDrumNote = note.voice in drumTrackIndices
                            if (isDrumNote) {
                                if (effectiveMasterVol > 0.01f) {
                                    val drumLabel = mapGmDrumMidiToLabel(note.midiNote) ?: "SNARE"
                                    audioEngine.setTablaGain(effectiveMasterVol)
                                    audioEngine.playSample(label = drumLabel, index = note.midiNote, string = "DRUM")
                                    drumLabelsToTrigger.add(drumLabel)
                                }
                                return@forEachIndexed
                            }

                            val (isRightHand, noteTrackColor) = resolveNoteHandAndColor(note, score, rhColorSnap, lhColorSnap)
                            if ((isRightHand && rhMuted) || (!isRightHand && lhMuted)) return@forEachIndexed

                            val effectivePitch = (note.midiNote + transOffset).coerceIn(0, 127)
                            activatedColors[effectivePitch] = noteTrackColor

                            val shouldWaitOnThisNote = isGuidedPlayAlongActive.value && (
                                    playAlongPracticeMode.value == "WAIT_FOR_NOTE" ||
                                            (playAlongPracticeMode.value == "MEASURE_FOLLOWUP" && isStudentFollowUpTurn)
                                    )
                            if (shouldWaitOnThisNote) {
                                newlyQueuedTargets.add(effectivePitch)
                                pitchesToActivate.add(effectivePitch)
                                val noteDurSec = (note.duration * secPerBeat * 0.94f).coerceAtLeast(0.15f)
                                scheduledNoteReleases[effectivePitch] = (scoreElapsedSec + noteDurSec) to note.voice
                            } else {
                                if (scheduledNoteReleases.containsKey(effectivePitch)) {
                                    audioEngine.noteOff(effectivePitch)
                                }
                                val baseGain = if (softPedalActive.value) 0.62f else 0.85f
                                val finalGain = (baseGain * effectiveMasterVol).coerceIn(0f, 1f)
                                if (finalGain > 0.01f) {
                                    audioEngine.noteOn(effectivePitch, finalGain)
                                }

                                val noteDurSec = (note.duration * secPerBeat * 0.94f).coerceAtLeast(0.06f)
                                scheduledNoteReleases[effectivePitch] = (scoreElapsedSec + noteDurSec) to note.voice
                                pitchesToRelease.remove(effectivePitch)
                                pitchesToActivate.add(effectivePitch)

                                if (isGuidedPlayAlongActive.value) {
                                    newlyQueuedTargets.add(effectivePitch)
                                }
                            }
                        }
                    }
                }
                // 4. Process Pedal Events
                var pedalStateChanged: Pair<Int, Boolean>? = null
                if (measure.pedalEvents.isNotEmpty() && !isWaitingForUserStep) {
                    measure.pedalEvents.forEachIndexed { pIdx, pedalEvent ->
                        if (firedPedalsMap[pIdx] != true && currentBeatInMeasure >= pedalEvent.position) {
                            firedPedalsMap[pIdx] = true
                            pedalStateChanged = pedalEvent.pedalType to pedalEvent.isPressed
                        }
                    }
                }

                // 5. Batch UI state updates onto Main Thread
                val nowMs = playbackOriginMark.elapsedNow().inWholeMilliseconds
                val hasNoteChanges = pitchesToActivate.isNotEmpty() ||
                    pitchesToRelease.isNotEmpty() ||
                    newlyQueuedTargets.isNotEmpty() ||
                    drumLabelsToTrigger.isNotEmpty() ||
                    pedalStateChanged != null

                if (hasNoteChanges || (nowMs - lastUiBroadcastMs) >= 16L) {
                    lastUiBroadcastMs = nowMs
                    val actCopy = if (pitchesToActivate.isNotEmpty()) pitchesToActivate.toIntArray() else null
                    val relCopy = if (pitchesToRelease.isNotEmpty()) pitchesToRelease.toIntArray() else null
                    val targetCopy = if (newlyQueuedTargets.isNotEmpty()) newlyQueuedTargets.distinct() else null
                    val drumCopy = if (drumLabelsToTrigger.isNotEmpty()) drumLabelsToTrigger.toList() else null
                    val pedCopy = pedalStateChanged

                    withContext(Dispatchers.Main) {
                        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                            scorePlayheadMeasure.value = currentMeasureIdx
                            scorePlayheadOffset.value = currentOffset
                            currentPosition.value = totalElapsedMs

                            relCopy?.forEach { p ->
                                midiActiveNotes.remove(p)
                                activeNoteTrackColors.remove(p)
                                playAlongEngine.liveKeyColors.remove(p)
                            }
                            if (targetCopy != null && isGuidedPlayAlongActive.value) {
                                guidedTargetNotes.clear()
                                guidedTargetNotes.addAll(targetCopy)
                                guidedSatisfiedNotes.clear()
                                targetCopy.forEach { targetPitch ->
                                    // Flash target key in bright Cyan/Gold on the visualizer so user knows what to press!
                                    playAlongEngine.liveKeyColors[targetPitch] = androidx.compose.ui.graphics.Color(0xFF00E5FF)
                                }
                                if (playAlongPracticeMode.value == "WAIT_FOR_NOTE") {
                                    playAlongPhaseBanner.value = "PLAY FLASHING KEY(S): ${targetCopy.joinToString { com.creativeblisslabs.peqlearn.app.ui.screens.getNoteNameFromMidi(it) }}"
                                }
                            }
                            actCopy?.forEach { p ->
                                midiActiveNotes[p] = true
                                midiPlayingNote.value = p
                                activatedColors[p]?.let { col ->
                                    activeNoteTrackColors[p] = col
                                }
                            }
                            if (relCopy != null && actCopy == null && midiPlayingNote.value !in midiActiveNotes) {
                                midiPlayingNote.value = midiActiveNotes.keys.firstOrNull()
                            }
                            pedCopy?.let { (pType, pressed) ->
                                when (pType) {
                                    64 -> {
                                        sustainPedalActive.value = pressed
                                        sustainActive.value = pressed
                                    }
                                    66 -> sostenutoPedalActive.value = pressed
                                    67 -> softPedalActive.value = pressed
                                }
                            }
                        }
                        drumCopy?.forEach { dLabel ->
                            viewModelScope.launch { triggerThreeDDrumVisualizer(dLabel) }
                        }
                        playAlongEngine.updatePlayheadPosition(totalElapsedMs)
                    }
                }

                delay(6L)
            }
        }
    }

    fun onMidiEventReceived(note: Int, isNoteOn: Boolean, velocity: Int = 90, channel: Int = 1) {
        // STRICT USB MIDI GATE: If the user toggled the MIDI Device button OFF, ignore all incoming USB MIDI data!
        if (!isUsbMidiInputEnabled.value) return

        val isHeldBySustain = sustainPedalActive.value
        val isHeldBySostenuto = sostenutoPedalActive.value && note in sostenutoCapturedNotes

        // Trigger audio engine immediately for zero-latency hardware keyboard response
        if (isNoteOn && velocity > 0) {
            val softScale = if (softPedalActive.value) 0.72f else 1.0f
            val gain = ((velocity / 127f) * masterVolume.value.coerceIn(0f, 1f)).coerceIn(0.08f, 1.0f)
            if (channel == 10) {
                val drumLabel = mapGmDrumMidiToLabel(note) ?: "SNARE"
                audioEngine.playSample(label = drumLabel, index = note, string = "DRUM")
            } else {
                audioEngine.noteOn(note, gain)
            }
        } else if (!isHeldBySustain && !isHeldBySostenuto) {
            if (channel != 10) {
                audioEngine.noteOff(note)
            }
        }

        // Update Visualizer & Guided Play-Along Step Engine on Main Thread
        viewModelScope.launch(Dispatchers.Main) {
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                lastMidiChannel.intValue = channel
                if (isNoteOn && velocity > 0) {
                    lastMidiVelocity.intValue = velocity
                    midiActiveNotes[note] = true
                    midiPlayingNote.value = note
                    playAlongEngine.onUserMidiNoteOn(note, currentPosition.value)

                    // Guided Play-Along Step Matching: check if user pressed a flashing target note (exact or pitch-class match)
                    if (isGuidedPlayAlongActive.value && guidedTargetNotes.isNotEmpty()) {
                        val matchedTarget = guidedTargetNotes.firstOrNull { it == note }
                            ?: guidedTargetNotes.firstOrNull { (it % 12) == (note % 12) && it !in guidedSatisfiedNotes }

                        if (matchedTarget != null) {
                            if (matchedTarget !in guidedSatisfiedNotes) {
                                guidedSatisfiedNotes.add(matchedTarget)
                            }
                            // Color key Emerald Green to reward correct hit!
                            playAlongEngine.liveKeyColors[matchedTarget] = androidx.compose.ui.graphics.Color(0xFF00E676)
                            playAlongEngine.liveKeyColors[note] = androidx.compose.ui.graphics.Color(0xFF00E676)

                            // Once all notes in the current flashing chord/step are pressed, advance to the next note/chord!
                            if (guidedSatisfiedNotes.size >= guidedTargetNotes.size) {
                                guidedTargetNotes.forEach { t ->
                                    midiActiveNotes.remove(t)
                                    playAlongEngine.liveKeyColors.remove(t)
                                }
                                guidedTargetNotes.clear()
                                guidedSatisfiedNotes.clear()
                            }
                        } else {
                            // Wrong key pressed -> Flash Crimson Red on that key
                            playAlongEngine.liveKeyColors[note] = androidx.compose.ui.graphics.Color(0xFFFF5252)
                        }
                    }
                } else {
                    playAlongEngine.liveKeyColors.remove(note)
                    if (sustainPedalActive.value || (sostenutoPedalActive.value && note in sostenutoCapturedNotes)) {
                        notesToReleaseOnSustainUp.add(note)
                    } else {
                        // Keep flashing target notes illuminated if the user hasn't satisfied them yet
                        if (note !in guidedTargetNotes) {
                            midiActiveNotes.remove(note)
                        }
                        if (midiPlayingNote.value == note) {
                            midiPlayingNote.value = midiActiveNotes.keys.firstOrNull()
                        }
                    }
                }
            }
        }
    }

    fun onMidiPedalEventReceived(pedalType: Int = 64, isPressed: Boolean) {
        if (!isUsbMidiInputEnabled.value) return
        viewModelScope.launch(Dispatchers.Main) {
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                when (pedalType) {
                    64 -> {
                        sustainPedalActive.value = isPressed
                        sustainActive.value = isPressed
                        if (!isPressed) {
                            val iter = notesToReleaseOnSustainUp.iterator()
                            while (iter.hasNext()) {
                                val note = iter.next()
                                if (!sostenutoPedalActive.value || note !in sostenutoCapturedNotes) {
                                    if (note !in guidedTargetNotes) midiActiveNotes.remove(note)
                                    audioEngine.noteOff(note)
                                    iter.remove()
                                }
                            }
                            if (midiPlayingNote.value != null && !midiActiveNotes.containsKey(midiPlayingNote.value)) {
                                midiPlayingNote.value = midiActiveNotes.keys.firstOrNull()
                            }
                        }
                    }
                    66 -> {
                        sostenutoPedalActive.value = isPressed
                        if (isPressed) {
                            sostenutoCapturedNotes.clear()
                            sostenutoCapturedNotes.addAll(midiActiveNotes.filterValues { it }.keys)
                        } else {
                            sostenutoCapturedNotes.forEach { capturedNote ->
                                if (capturedNote in notesToReleaseOnSustainUp && !sustainPedalActive.value) {
                                    if (capturedNote !in guidedTargetNotes) midiActiveNotes.remove(capturedNote)
                                    audioEngine.noteOff(capturedNote)
                                    notesToReleaseOnSustainUp.remove(capturedNote)
                                }
                            }
                            sostenutoCapturedNotes.clear()
                        }
                    }
                    67 -> {
                        softPedalActive.value = isPressed
                    }
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
        pdfImportProgress.value = 0.05f

        val cleanFallbackTitle = fileName
            .substringAfterLast("/")
            .substringBeforeLast(".")
            .replace(Regex("[_-]+"), " ")
            .trim()
            .ifBlank { "Imported Score" }

        viewModelScope.launch(Dispatchers.Default) {
            val parsedScore = fileBytes?.let { bytes ->
                com.creativeblisslabs.peqlearn.app.util.MidiFileParser.parse(bytes, cleanFallbackTitle)
                    ?: com.creativeblisslabs.peqlearn.app.util.UniversalScoreParser.parse(bytes, cleanFallbackTitle, uriString)
            }

            val finalScore = (parsedScore ?: Score(
                title = cleanFallbackTitle,
                tempo = 120f,
                timeSignature = "4/4",
                measures = listOf(Measure(notes = emptyList()))
            )).let { s ->
                val hasPlayableNotes = s.measures.any { m -> m.notes.any { !it.isRest } }
                val hasValidTitle = s.title.isNotBlank() &&
                        !s.title.equals("Untitled Score", ignoreCase = true) &&
                        !s.title.equals("Score", ignoreCase = true) &&
                        !s.title.equals("Imported Score", ignoreCase = true)
                s.copy(
                    title = if (hasValidTitle) s.title else cleanFallbackTitle,
                    isPdfScore = if (hasPlayableNotes) false else s.isPdfScore
                )
            }

            val safeTempo = finalScore.tempo.coerceAtLeast(20f)
            var totalDurationMs = 0L
            finalScore.measures.forEach { m ->
                val quarterBeatsInMeasure = m.timeSignatureNumerator.toFloat() * (4f / m.timeSignatureDenominator.toFloat().coerceAtLeast(1f))
                val mDurSec = (60f / safeTempo) * quarterBeatsInMeasure
                totalDurationMs += (mDurSec * 1000f).toLong()
            }
            if (totalDurationMs <= 0L) totalDurationMs = 180000L

            val ext = fileName.substringAfterLast(".", "SCORE").uppercase()
            val scoreItem = pEQMediaItem(
                id = "score_${uriString.hashCode()}",
                title = finalScore.title.ifBlank { cleanFallbackTitle },
                subtitle = "${finalScore.composer.ifBlank { "Score" }} • ${finalScore.timeSignature} • ${finalScore.tempo.toInt()} BPM",
                artist = finalScore.composer.ifBlank { "Score Composer" },
                path = uriString,
                type = MediaType.AUDIO,
                format = ext,
                sizeBytes = fileBytes?.size?.toLong() ?: 0L,
                durationMs = totalDurationMs
            )

            for (progress in 10..90 step 20) {
                delay(10)
                withContext(Dispatchers.Main) {
                    pdfImportProgress.value = progress / 100f
                }
            }

            withContext(Dispatchers.Main) {
                stopScorePlayback()
                currentScore.value = finalScore
                syncScoreTracks(finalScore)
                currentMediaItem.value = scoreItem
                currentDuration.value = totalDurationMs
                currentPosition.value = 0L
                scorePlayheadMeasure.value = 0
                scorePlayheadOffset.value = 0f

                val existingQueueIdx = playbackQueue.indexOfFirst { it.path == scoreItem.path }
                if (existingQueueIdx == -1) {
                    playbackQueue.add(0, scoreItem)
                    setCurrentQueueIndex(0)
                } else {
                    setCurrentQueueIndex(existingQueueIdx)
                }

                if (!recentlyPlayedItems.any { it.path == scoreItem.path }) {
                    recentlyPlayedItems.add(0, scoreItem)
                    if (recentlyPlayedItems.size > 20) recentlyPlayedItems.removeAt(recentlyPlayedItems.size - 1)
                }
                updateMostPlayed(scoreItem)
                persistSession()

                pdfImportProgress.value = 1f
                isImportingPDF.value = false
                pdfImportProgress.value = 0f
            }
        }
    }

    fun compileCurrentNotationToPDF(
        options: PdfExportOptions = pdfExportOptions.value,
        onComplete: ((String?) -> Unit)? = null
    ) {
        val rawScore = currentScore.value ?: Score(
            title = options.title.ifBlank { currentMediaItem.value?.title ?: "pEQ Score Study" },
            composer = options.composer.ifBlank { "pEQ Learn Studio" },
            tempo = metronomeBpm.value.toFloat(),
            timeSignature = "4/4"
        )
        val resolvedTitle = listOf(
            options.title,
            rawScore.title.takeUnless { it.equals("Untitled Score", ignoreCase = true) },
            currentMediaItem.value?.title,
            selectedMixerItem.value?.displayName,
            rawScore.tracks.firstOrNull { it.trackName.isNotBlank() && !it.trackName.equals("Track 1", true) }?.trackName
        ).firstOrNull { !it.isNullOrBlank() }?.substringAfterLast("/")?.substringBeforeLast(".")?.replace("_", " ") ?: "pEQ Studio Score"

        val score = rawScore.copy(title = resolvedTitle)

        isExportingPDF.value = true
        pdfExportProgress.value = 0.05f

        val config = PdfExportConfig(
            score = score,
            options = options,
            isLeftHandHidden = isLeftHandHidden.value,
            isRightHandHidden = isRightHandHidden.value,
            isSplitStaff = isSplitStaff.value,
            enabledTrackIndices = enabledTrackIndices.toMap(),
            transposition = transposition.intValue
        )

        compilePdfPlatformSpecific(
            config = config,
            onProgress = { progress: Float ->
                viewModelScope.launch(Dispatchers.Main) {
                    pdfExportProgress.value = progress
                }
            },
            onComplete = { path: String? ->
                viewModelScope.launch(Dispatchers.Main) {
                    isExportingPDF.value = false
                    pdfExportProgress.value = 1.0f
                    onComplete?.invoke(path)
                }
            }
        )
    }

    fun exportVideoToFile(onComplete: (String) -> Unit) {
        val rawScore = currentScore.value ?: Score(
            title = currentMediaItem.value?.title ?: "pEQ Performance",
            tempo = metronomeBpm.value.toFloat(),
            timeSignature = "4/4"
        )
        val resolvedTitle = listOf(
            rawScore.title.takeUnless { it.equals("Untitled Score", ignoreCase = true) },
            currentMediaItem.value?.title,
            selectedMixerItem.value?.displayName
        ).firstOrNull { !it.isNullOrBlank() }?.substringAfterLast("/")?.substringBeforeLast(".")?.replace("_", " ") ?: "pEQ Composition"

        val score = rawScore.copy(title = resolvedTitle)
        val res = videoExportResolution.value
        val fpsInt = videoExportFps.value.filter { it.isDigit() }.toIntOrNull() ?: 60

        val effectsMap = mapOf(
            "Notation" to (videoExportLayoutMode.value != "Waterfall"),
            "Visualizer" to (videoExportLayoutMode.value != "Notation"),
            "Glow" to (videoExportPeakSensitivity.floatValue > 30f),
            "Particles" to showWaterfallParticles.value,
            "RenderPedals" to videoExportRenderPedals.value,
            "Theme" to (videoExportTheme.value == "Cyberpunk"),
            "AudioSource" to (videoExportAudioSource.value == "MidiScore")
        )

        val queueItem = com.creativeblisslabs.peqlearn.app.ui.viewmodel.VideoExportHistoryItem(
            title = resolvedTitle,
            resolution = res,
            fps = videoExportFps.value,
            audioSource = if (videoExportAudioSource.value == "AudioFile") videoExportExternalAudioName.value
                ?: "Custom Audio File" else "MIDI Score Synth",
            status = "Rendering",
            progress = 0.01f
        )
        videoExportHistory.add(0, queueItem)

        isVideoExporting.value = true
        videoExportProgress.value = 0.01f

        val config = VideoExportConfig(
            score = score,
            resolution = res,
            fps = fpsInt,
            effects = effectsMap,
            layoutMode = videoExportLayoutMode.value,
            notationFormat = notationFormat.value,
            visualTheme = videoExportTheme.value,
            bitrateMbps = videoExportBitrate.value.filter { it.isDigit() }.toIntOrNull() ?: 12,
            sampleRateHz = if (videoExportSampleRate.value.contains("48")) 48000 else 44100,
            cameraYawDeg = videoExportCameraYaw.floatValue,
            cameraPitchDeg = videoExportCameraPitch.floatValue,
            eqBarsCount = videoExportEqBars.intValue,
            peakSensitivity = videoExportPeakSensitivity.floatValue,
            rightHandColorInt = composeColorToArgbInt(rightHandColor.value),
            leftHandColorInt = composeColorToArgbInt(leftHandColor.value),
            isRightHandHidden = isRightHandHidden.value,
            isLeftHandHidden = isLeftHandHidden.value,
            enabledTrackIndices = enabledTrackIndices.toMap(),
            externalAudioUri = videoExportExternalAudioUri.value
        )

        exportVideoPlatformSpecific(
            viewModel = this,
            config = config,
            onProgress = { progress: Float ->
                videoExportProgress.value = progress
                val idx = videoExportHistory.indexOfFirst { it.id == queueItem.id }
                if (idx != -1) {
                    videoExportHistory[idx] = videoExportHistory[idx].copy(progress = progress)
                }
            },
            onComplete = { filePath: String? ->
                isVideoExporting.value = false
                val idx = videoExportHistory.indexOfFirst { it.id == queueItem.id }
                if (filePath == null) {
                    videoExportProgress.value = 0f
                    if (idx != -1) {
                        videoExportHistory[idx] = videoExportHistory[idx].copy(
                            status = "Failed",
                            progress = 0f,
                            filePath = null
                        )
                    }
                } else {
                    videoExportProgress.value = 1f
                    if (idx != -1) {
                        videoExportHistory[idx] = videoExportHistory[idx].copy(
                            status = "Completed",
                            progress = 1f,
                            filePath = filePath
                        )
                    }
                    onComplete(filePath)
                }
            }
        )
    }
    /**
     * Imports a loaded Score's drum/percussion notes (or all notes) into 16-step DrumTutorial bars
     * and populates [drumTutorials] + [songTimeline].
     */
    fun importCurrentScoreIntoDrumSequencer() {
        val score = currentScore.value ?: return
        val drumTracks = score.tracks.filter { it.channel == 9 }.map { it.trackIndex }.toSet()
        val importedBars = mutableListOf<DrumTutorial>()

        score.measures.take(32).forEachIndexed { mIdx, measure ->
            val beatsInM = (measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
            val steps = List(16) { MutableList(0) { "" } }
            val notesForDrums = measure.notes.filter { !it.isRest && (drumTracks.isEmpty() || it.voice in drumTracks) }

            notesForDrums.forEach { n ->
                val stepIdx = ((n.position / beatsInM) * 16f).toInt().coerceIn(0, 15)
                val padLabel = when (n.midiNote) {
                    35, 36 -> "0-KICK"
                    38, 40, 37 -> "1-SNARE"
                    42, 44 -> "2-HH_CLOSED"
                    46 -> "3-HH_OPEN"
                    48, 50 -> "4-TOM_HI"
                    45, 47 -> "5-TOM_MID"
                    41, 43 -> "6-FLOOR"
                    49, 55, 57 -> "7-CRASH"
                    51, 53, 59 -> "8-RIDE"
                    39 -> "9-CLAP"
                    56 -> "10-PERC"
                    69, 70, 82 -> "11-SHAKER"
                    else -> "${(n.midiNote % 12)}-SNARE"
                }
                if (padLabel !in steps[stepIdx]) {
                    steps[stepIdx].add(padLabel)
                }
            }
            if (steps.any { it.isNotEmpty() }) {
                val barTut = DrumTutorial(
                    name = "${score.title.take(14)} Bar${mIdx + 1}",
                    steps = steps.map { it.toList() }
                )
                importedBars.add(barTut)
            }
        }

        if (importedBars.isNotEmpty()) {
            liveTutorialBpm.intValue = score.tempo.toInt().coerceIn(40, 240)
            setMetronomeBpm(liveTutorialBpm.intValue)
            importedBars.forEach { tut ->
                if (drumTutorials.none { it.name == tut.name }) {
                    drumTutorials.add(tut)
                }
            }
            selectedTutorial.value = importedBars.first()
            songTimeline.clear()
            importedBars.forEach { tut ->
                addBarToTimeline(tut.name, isFiller = false)
            }
            toastMessage.value = "Imported ${importedBars.size} drum bars from${score.title}!"
        }
    }

    /**
     * Exports the current [songTimeline] (or active [selectedTutorial]) as a Standard MIDI File (.mid)
     * on General MIDI Channel 10 (Percussion) and triggers the platform share sheet.
     */
    fun exportDrumCompositionAsMidi() {
        viewModelScope.launch(Dispatchers.Default) {
            val barsToExport = if (songTimeline.isNotEmpty()) {
                songTimeline.mapNotNull { bar ->
                    drumTutorials.find { it.name.equals(bar.patternName, ignoreCase = true) }
                        ?: selectedTutorial.value
                }
            } else {
                listOfNotNull(selectedTutorial.value)
            }
            if (barsToExport.isEmpty()) return@launch

            val bpm = liveTutorialBpm.intValue.coerceIn(40, 240)
            val ppq = 480
            val ticksPer16th = ppq / 4 // 120 ticks
            val out = java.io.ByteArrayOutputStream()

            fun writeInt32(v: Int) {
                out.write((v ushr 24) and 0xFF)
                out.write((v ushr 16) and 0xFF)
                out.write((v ushr 8) and 0xFF)
                out.write(v and 0xFF)
            }
            fun writeInt16(v: Int) {
                out.write((v ushr 8) and 0xFF)
                out.write(v and 0xFF)
            }
            fun writeVlq(buf: java.io.ByteArrayOutputStream, value: Int) {
                var v = value.coerceAtLeast(0)
                var buffer = v and 0x7F
                while ((v shr 7) > 0) {
                    v = v shr 7
                    buffer = (buffer shl 8) or ((v and 0x7F) or 0x80)
                }
                while (true) {
                    buf.write(buffer and 0xFF)
                    if ((buffer and 0x80) != 0) buffer = buffer ushr 8 else break
                }
            }

            // Build Track Chunk
            val trk = java.io.ByteArrayOutputStream()
            // Tempo Meta Event (FF 51 03)
            val microsPerBeat = 60_000_000 / bpm
            writeVlq(trk, 0)
            trk.write(byteArrayOf(0xFF.toByte(), 0x51, 0x03, ((microsPerBeat ushr 16) and 0xFF).toByte(), ((microsPerBeat ushr 8) and 0xFF).toByte(), (microsPerBeat and 0xFF).toByte()))

            var lastEventTick = 0
            barsToExport.forEachIndexed { barIdx, tut ->
                for (stepIdx in 0 until 16) {
                    val stepTick = (barIdx * 16 + stepIdx) * ticksPer16th
                    val hits = tut.steps.getOrNull(stepIdx) ?: emptyList()
                    val gmNotes = hits.map { raw ->
                        val up = raw.uppercase()
                        when {
                            up.contains("KICK") -> 36
                            up.contains("SNARE") -> 38
                            up.contains("OPEN") || up.contains("OHAT") -> 46
                            up.contains("CLOSED") || up.contains("HAT") || up.contains("HH") -> 42
                            up.contains("CRASH") -> 49
                            up.contains("RIDE") -> 51
                            up.contains("TOM_HI") -> 50
                            up.contains("TOM_MID") -> 47
                            up.contains("FLOOR") || up.contains("TOM_LO") -> 43
                            up.contains("CLAP") -> 39
                            up.contains("PERC") || up.contains("COWBELL") -> 56
                            up.contains("SHAKER") -> 70
                            else -> 38
                        }
                    }.distinct()

                    if (gmNotes.isNotEmpty()) {
                        gmNotes.forEachIndexed { idx, midiNote ->
                            val delta = if (idx == 0) (stepTick - lastEventTick).coerceAtLeast(0) else 0
                            writeVlq(trk, delta)
                            trk.write(0x99) // Note On Channel 10
                            trk.write(midiNote and 0x7F)
                            trk.write(105)
                        }
                        lastEventTick = stepTick
                        val noteOffTick = stepTick + (ticksPer16th / 2)
                        gmNotes.forEachIndexed { idx, midiNote ->
                            val delta = if (idx == 0) (noteOffTick - lastEventTick).coerceAtLeast(1) else 0
                            writeVlq(trk, delta)
                            trk.write(0x89) // Note Off Channel 10
                            trk.write(midiNote and 0x7F)
                            trk.write(0)
                        }
                        lastEventTick = noteOffTick
                    }
                }
            }

            // End of Track (FF 2F 00)
            writeVlq(trk, ticksPer16th)
            trk.write(byteArrayOf(0xFF.toByte(), 0x2F, 0x00))
            val trkBytes = trk.toByteArray()

            // MThd Header
            out.write("MThd".encodeToByteArray())
            writeInt32(6)
            writeInt16(0) // Format 0
            writeInt16(1) // 1 Track
            writeInt16(ppq)
            out.write("MTrk".encodeToByteArray())
            writeInt32(trkBytes.size)
            out.write(trkBytes)

            // Also load this exported drum groove into currentScore so the user can view/export it in Staff/Video too!
            val parsedDrumScore = com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.parse(out.toByteArray(), "pEQ Drum Groove (${bpm} BPM)")
            withContext(Dispatchers.Main) {
                if (parsedDrumScore != null) {
                    currentScore.value = parsedDrumScore
                    syncScoreTracks(parsedDrumScore)
                }
                toastMessage.value = "Exported ${barsToExport.size}-Bar Drum Groove (${bpm} BPM) to Score & MIDI!"
            }
            com.creativeblisslabs.peqlearn.app.shareTextFile(
                "pEQ_Drum_Groove_${bpm}BPM.txt",
                parsedDrumScore?.dumpAsText() ?: "pEQ Drum Groove (${barsToExport.size} Bars @ $bpm BPM)"
            )
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

        fun onSequencerStepTick(stepIndex: Int, selectedScale: String, bolSequence: List<String> = emptyList()) {
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


    fun stopRiyaazEngine() {
        isSequencerActive.value = false
        sequencerJob?.cancel()
        sequencerJob = null

        audioEngine.playSample(label = "STOP", index = 0, string = "TANPURA")
    }

    fun onMidiNoteOn(note: Int, velocity: Int) {
        midiActiveNotes[note] = true
        midiPlayingNote.value = note
        audioEngine.noteOn(note, velocity.toFloat() / 127f)
        notesToReleaseOnSustainUp.remove(note)
        liveLooper.onLiveNote(note, velocity.toFloat() / 127f, true, Clock.System.now().toEpochMilliseconds())
        
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
        liveLooper.onLiveNote(note, 0f, false, Clock.System.now().toEpochMilliseconds())
        if (sustainActive.value) {
            notesToReleaseOnSustainUp.add(note)
        } else {
            midiActiveNotes.remove(note)
            if (midiPlayingNote.value == note) {
                midiPlayingNote.value = midiActiveNotes.filterValues { it }.keys.firstOrNull()
            }
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

    fun getNoteNameFromMidi(midiNote: Int): String {
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
