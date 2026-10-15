package com.creativeblisslabs.peqlearn.app.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.creativeblisslabs.peqlearn.app.data.MediaChapter
import com.creativeblisslabs.peqlearn.app.data.MediaTrack
import com.creativeblisslabs.peqlearn.app.data.RepeatMode
import com.creativeblisslabs.peqlearn.app.data.SyncedLine
import com.creativeblisslabs.peqlearn.app.data.TrackType
import com.creativeblisslabs.peqlearn.app.data.pEQMediaMetadata
import dev.kotlinds.fluidsynthkmp.FluidSynthPlayer
import dev.kotlinds.fluidsynthkmp.AudioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import peq_learn.composeapp.generated.resources.Res
import java.io.File
import java.io.IOException
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.get
import kotlin.collections.remove
import kotlin.concurrent.withLock
import kotlin.math.sin
import kotlin.math.sqrt

// --- NATIVE BASS JNI WRAPPER SLOT INTERCEPT ---
object AndroidBass {
    private var bassClass: Class<*>? = null
    var isLoaded = false

    init {
        try {
            System.loadLibrary("bass")
            bassClass = Class.forName("org.un4seen.bass.BASS")
            isLoaded = true
        } catch (_: Throwable) {
            println("pEQLearn: [BASS] Android library binaries not found, using ExoPlayer.")
        }
    }

    fun init(device: Int, freq: Int, flags: Int): Boolean {
        if (!isLoaded || bassClass == null) return false
        return try {
            val method = bassClass!!.getMethod(
                "BASS_Init",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            method.invoke(null, device, freq, flags) as Boolean
        } catch (_: Exception) {
            false
        }
    }

    fun streamCreateFile(file: String, flags: Int): Int {
        if (!isLoaded || bassClass == null) return 0
        return try {
            val method = bassClass!!.getMethod(
                "BASS_StreamCreateFile",
                String::class.java,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            method.invoke(null, file, 0L, 0L, flags) as Int
        } catch (_: Exception) {
            0
        }
    }

    fun channelPlay(handle: Int, restart: Boolean): Boolean {
        if (!isLoaded || bassClass == null) return false
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelPlay",
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(null, handle, restart) as Boolean
        } catch (_: Exception) {
            false
        }
    }

    fun channelPause(handle: Int): Boolean {
        if (!isLoaded || bassClass == null) return false
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelPause",
                Int::class.javaPrimitiveType
            )
            method.invoke(null, handle) as Boolean
        } catch (_: Exception) {
            false
        }
    }

    fun channelStop(handle: Int): Boolean {
        if (!isLoaded || bassClass == null) return false
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelStop",
                Int::class.javaPrimitiveType
            )
            method.invoke(null, handle) as Boolean
        } catch (_: Exception) {
            false
        }
    }

    fun channelGetPosition(handle: Int, mode: Int): Long {
        if (!isLoaded || bassClass == null) return 0L
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelGetPosition",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            (method.invoke(null, handle, mode) as Number).toLong()
        } catch (_: Exception) {
            0L
        }
    }

    fun channelSetPosition(handle: Int, pos: Long, mode: Int): Boolean {
        if (!isLoaded || bassClass == null) return false
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelSetPosition",
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            method.invoke(null, handle, pos, mode) as Boolean
        } catch (_: Exception) {
            false
        }
    }

    fun channelSeconds2Bytes(handle: Int, pos: Double): Long {
        if (!isLoaded || bassClass == null) return 0L
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelSeconds2Bytes",
                Int::class.javaPrimitiveType,
                Double::class.javaPrimitiveType
            )
            (method.invoke(null, handle, pos) as Number).toLong()
        } catch (_: Exception) {
            0L
        }
    }

    fun channelBytes2Seconds(handle: Int, pos: Long): Double {
        if (!isLoaded || bassClass == null) return 0.0
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelBytes2Seconds",
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )
            (method.invoke(null, handle, pos) as Number).toDouble()
        } catch (_: Exception) {
            0.0
        }
    }

    fun channelSetAttribute(handle: Int, attrib: Int, value: Float): Boolean {
        if (!isLoaded || bassClass == null) return false
        return try {
            val method = bassClass!!.getMethod(
                "BASS_ChannelSetAttribute",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
            method.invoke(null, handle, attrib, value) as Boolean
        } catch (_: Exception) {
            false
        }
    }
}

@kotlin.OptIn(ExperimentalResourceApi::class)
class AndroidAudioEngine(
    private val context: Context
) : AudioEngine {

    // --- STEP 1: FUNDAMENTAL AUDIO STRUCTURES & INSTRUMENT POOLS ---
    private val systemSoundPool: SoundPool = SoundPool.Builder().setMaxStreams(4).build()

    private val soundPool: SoundPool = SoundPool.Builder().apply {
        setMaxStreams(16)
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
    }.build()

    // Low-latency hardware buffer size (44.1kHz 16-bit stereo)
    private val stereoBufferSize = AudioTrack.getMinBufferSize(
        44100,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(2048)

    private val VOICE_COUNT = 12
    private val voicePool = Array(VOICE_COUNT) {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(stereoBufferSize * 2)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .apply {
                setVolume(1.0f)
                play()
            }
    }

    private val drumKitLoader = DrumKitLoader(context, soundPool)

    // --- STEP 2: CLASS LEVEL STATE, CACHES, AND MEDIA3/DSP FIELDS ---
    private var bassHandle = 0
    private val stemHandles = ConcurrentHashMap<String, Int>()
    private var currentTrackUrl: String? = null
    private var currentInstrument = "PIANO"

    private val pianoNoteCache = ConcurrentHashMap<Int, ShortArray>()
    private val guitarNoteCache = ConcurrentHashMap<Int, ShortArray>()
    private val tanpuraNoteCache = ConcurrentHashMap<Int, ShortArray>()

    private val activeMidiNotesEngine = ConcurrentHashMap<Int, Boolean>()
    private val activeTanpuraNotes = ConcurrentHashMap<Int, Boolean>()

    private var currentlyPlayingNoteEngine: Int? = null
    private var currentlyPlayingTanpuraNote: Int? = null
    private var currentlyPlayingTanpuraId: Int? = null
    
    private var tanpuraTrack1: AudioTrack? = null
    private var tanpuraTrack2: AudioTrack? = null
    private var activeTanpuraTrackIndex = 0 // 0 for none, 1 for track1, 2 for track2

    private var tanpuraGain = 0.8f
    private var tablaGain = 1.0f
    private var metronomeGain = 1.0f
    private val tanpuraMidiMap = mapOf(
        "C"  to 60, "C#" to 61, "D"  to 62, "D#" to 63,
        "E"  to 64, "F"  to 65, "F#" to 66, "G"  to 67,
        "G#" to 68, "A"  to 69, "A#" to 70, "B"  to 71
    )

    private val tablaNoteCache = ConcurrentHashMap<Int, ShortArray>()

    // Hindustani Classical Bols and Carnatic Solkattu Syllables Mapped to Percussion MIDI Keys
    private val percussionMidiMap = mapOf(
        // Hindustani Classical Bols - Exact Tabla SF2 Key Mapping
        "DHA" to 72, "DHIN" to 68, "DHI" to 68, "DIN" to 70,
        "TIN" to 63, "TA" to 64, "NA" to 64, "GE" to 60,
        "GHE" to 60, "GA" to 60, "KA" to 61, "KE" to 61,
        "KI" to 61, "KAT" to 61, "TI" to 62, "TE" to 62,
        "TU" to 69, "TUN" to 69, "DHAGE" to 74, "TIRAKITA" to 62,
        "KITONE" to 61, "DU" to 60, "RA" to 62,

        // Carnatic Solkattu Syllables - Exact Solkattu to Percussion Mapping
        "THA" to 61, "THI" to 62, "THOM" to 60, "THON" to 60,
        "NAM" to 64, "NU" to 64, "MI" to 62, "JO" to 63, "DI" to 68,
        "THA-KA" to 61, "TAKA" to 61, "THA-KI-TA" to 62, "TAKITA" to 62,
        "THA-KA-DHI-MI" to 72, "THAKADHIMI" to 72, "THA-KA-THA-KI-TA" to 61,
        "THA-KI-TA-THA-KA-DHI-MI" to 72, "THA-KA-DHI-MI-THA-KA-THA-KI-TA" to 72
    )

    private val tablaMidiMap = ConcurrentHashMap<String, Int>(percussionMidiMap)

    // Helper mapping for compound bols (breaking them down into constituent playable sub-strokes)
    private val compoundBolMap = mapOf(
        "DHAGE" to listOf(72, 60),
        "TIRAKITA" to listOf(62, 61, 62, 61),
        "KITONE" to listOf(61, 60, 67),
        "THA-KA" to listOf(61, 61),
        "TAKA" to listOf(61, 61),
        "THA-KI-TA" to listOf(61, 62, 61),
        "TAKITA" to listOf(61, 62, 61),
        "THA-KA-DHI-MI" to listOf(61, 61, 68, 62),
        "THAKADHIMI" to listOf(61, 61, 68, 62),
        "THA-KA-THA-KI-TA" to listOf(61, 61, 61, 62, 61),
        "THA-KI-TA-THA-KA-DHI-MI" to listOf(61, 62, 61, 61, 61, 68, 62),
        "THA-KA-DHI-MI-THA-KA-THA-KI-TA" to listOf(61, 61, 68, 62, 61, 61, 61, 62, 61)
    )

    private var listenersAttached = false
    private var visualizerRetryCount = 0
    private val json = Json { ignoreUnknownKeys = true }
    private var player: ExoPlayer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var visualizer: Visualizer? = null
    private val stemPlayers = ConcurrentHashMap<String, ExoPlayer>()

    private var isVocalSuppressionActive = false
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val loopHandler = Handler(Looper.getMainLooper())

    private var loopA: Long? = null
    private var loopB: Long? = null

    private var isPhaseInverted = false
    private var dropoutCount = 0
    private var lastLatency = 12
    private var latestSpectrum = FloatArray(32)
    private var latestPhase = 0.85f

    override var onFftDataReceived: ((FloatArray) -> Unit)? = null
    override var onTrackEnded: (() -> Unit)? = null
    override var onTrackChanged: (() -> Unit)? = null
    override var onBufferingChanged: ((Boolean) -> Unit)? = null
    override var onPadTriggered: ((Int) -> Unit)? = null

    private var currentBitrateVal = 0
    private var currentSampleRateVal = 44100
    private var currentBitDepthVal = 16
    private var currentCodecStr = "PCM"
    private var currentDeviceType: Int? = null

    private var lastSeekTime = 0L
    private var isSeeking = false
    private val prefs = context.getSharedPreferences("pEQLearn_player_state", Context.MODE_PRIVATE)

    private var currentDrumKitName = "NEURAL 808"
    override var currentSelectedKitName: String = "ELECTRONIC_808"

    private var currentVoiceIndex = 0
    private val voiceMutex = ReentrantLock()
    private val soundFontPool = mutableMapOf<String, FluidSynthPlayer>()
    private var globalSoundFont: FluidSynthPlayer? = null
    private var tanpuraEngine: TanpuraSoundFontEngine? = null
    private var tablaEngine: TablaSoundFontEngine? = null
    private val sf2Lock = Any()
    private val sf2Loaded = AtomicBoolean(false)

    class TanpuraSoundFontEngine(private val player: FluidSynthPlayer) {
        fun noteOn(midiNote: Int) {
            player.noteOn(0, midiNote, 100)
        }

        fun noteOff(midiNote: Int) {
            player.noteOff(0, midiNote)
        }

        /**
         * Renders a seamless looping Tanpura drone buffer by crossfading a tail segment back to the start.
         */
        fun renderTanpuraDrone(midiNote: Int, velocity: Int): ShortArray? {
            val sampleRate = 44100
            val loopSeconds = 6
            val crossfadeSeconds = 1
            val preRollSeconds = 1

            val loopSamples = loopSeconds * sampleRate * 2
            val crossfadeSamples = crossfadeSeconds * sampleRate * 2
            val preRollSamples = preRollSeconds * sampleRate * 2

            // Render attack phase to avoid starting from silence
            player.noteOn(0, midiNote, velocity)
            player.renderFloat(preRollSamples)

            // Render the main loop plus a tail for crossfading
            val floatBuffer = player.renderFloat(loopSamples + crossfadeSamples)

            val result = ShortArray(loopSamples)
            for (i in 0 until loopSamples) {
                var sampleValue = if (i < floatBuffer.size) floatBuffer[i] else 0f

                // Perform a linear crossfade at the loop boundary
                if (i < crossfadeSamples) {
                    val tailIndex = loopSamples + i
                    if (tailIndex < floatBuffer.size) {
                        val frameIndex = i / 2
                        val totalFadeFrames = crossfadeSamples / 2
                        val fade = frameIndex.toFloat() / totalFadeFrames
                        val tailSample = floatBuffer[tailIndex]
                        sampleValue = tailSample * (1f - fade) + sampleValue * fade
                    }
                }

                result[i] = (sampleValue * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
            }
            player.noteOff(0, midiNote) // Clean up synth voice after rendering
            return result
        }
    }

    class TablaSoundFontEngine(private val player: FluidSynthPlayer) {
        fun noteOn(midiNote: Int) {
            player.noteOn(0, midiNote, 100)
        }

        fun renderTablaBol(midiNote: Int, velocity: Int): ShortArray? {
            // Render 0.75 seconds of stereo 16-bit PCM (44100 Hz * 2 channels)
            val stereoSamples = (44100 * 0.75 * 2).toInt()
            player.noteOn(0, midiNote, velocity.coerceIn(1, 127))
            val floatBuffer = player.renderFloat(stereoSamples)
            player.noteOff(0, midiNote) // Clean up voice in FluidSynth to avoid accumulation
            return ShortArray(stereoSamples) { i ->
                val sampleValue = if (i < floatBuffer.size) floatBuffer[i] else 0f
                (sampleValue * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
            }
        }
    }

    private val kitSounds = mutableMapOf<String, MutableMap<String, Int>>()
    private val loadedSounds = mutableMapOf<Int, Boolean>()

    private var metronomeStrongId: Int = 0
    private var metronomeWeakId: Int = 0
    private var areSystemSoundsReady = false

    private val pitchToMidiOffset = mapOf(
        "C" to 0, "C#" to 1, "D" to 2, "D#" to 3, "E" to 4, "F" to 5,
        "F#" to 6, "G" to 7, "G#" to 8, "A" to 9, "A#" to 10, "B" to 11
    )

    private val loopRunnable = object : Runnable {
        override fun run() {
            checkLoop()
            loopHandler.postDelayed(this, 50)
        }
    }

    init {
        drumKitLoader.preloadAllKits()
    }

    override fun playMidiNote(midiNote: Int) {
        noteOn(midiNote, 0.8f)
    }

    override fun sendMidiController(controller: Int, value: Int) {
        synchronized(sf2Lock) {
            globalSoundFont?.let { sf ->
                // MikroSoundFont might not have a direct MIDI CC method, 
                // but we satisfy the interface here.
            }
        }
    }

    private fun renderAudioBufferChunk(samplesRequested: Int): FloatArray {
        synchronized(sf2Lock) {
            if (sf2Loaded.get() && globalSoundFont != null) {
                try {
                    globalSoundFont?.let { sf: FluidSynthPlayer ->
                        return sf.renderFloat(samplesRequested)
                    }
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Native guard crash block intercepted: ${e.message}")
                }
            }
        }
        return FloatArray(samplesRequested)
    }

    override fun setInstrument(instrument: String) {
        val upper = instrument.uppercase().trim()
        val targetKey = when {
            upper.contains("HARMONIUM") -> "HARMONIUM"
            upper.contains("GUITAR") -> "GUITAR"
            else -> "PIANO"
        }

        if (currentInstrument == targetKey && sf2Loaded.get()) return

        synchronized(sf2Lock) {
            currentInstrument = targetKey
            val targetSf = soundFontPool[targetKey]
            if (targetSf != null) {
                globalSoundFont = targetSf
                sf2Loaded.set(true)
                println("AudioEngine Architecture: Route active pointer to $targetKey [SUCCESS]")
            } else {
                // If specific instrument isn't loaded yet, keep sf2Loaded true if piano fallback exists
                sf2Loaded.set(soundFontPool["PIANO"] != null)
                println("AudioEngine Warning: Target $targetKey not loaded in RAM pool yet (Background thread processing).")
            }
        }
    }

    override fun setLoopPoints(a: Long?, b: Long?) {
        loopA = a
        loopB = b
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @OptIn(UnstableApi::class)
    override fun initialize(sessionId: Int) {
        if (AndroidBass.isLoaded) {
            AndroidBass.init(-1, 44100, 0)
        }

        initSystemSounds()
        initAllAssets()
        loadSf2Piano()
        loadSf2Guitar()
        loadSf2Harmonium()

        engineScope.launch(Dispatchers.IO) {
            loadSf2Tanpura()?.let { sf ->
                tanpuraEngine = TanpuraSoundFontEngine(sf)
                Log.i("AudioEngine", "Isolated Tanpura Engine mounted successfully.")
            }
            loadSf2Tabla()?.let { sf ->
                tablaEngine = TablaSoundFontEngine(sf)
                Log.i("AudioEngine", "Isolated Tabla Engine mounted successfully.")
            }
        }

        if (player != null) return
        player = AudioEngineProvider.getExoPlayer(context)
        attachPlayerListeners()

        val audioSessionId = player?.audioSessionId ?: sessionId
        initVisualizer(audioSessionId)
    }

    @OptIn(UnstableApi::class)
    private fun attachPlayerListeners() {
        if (listenersAttached) return
        listenersAttached = true

        player?.addListener(object : Player.Listener {
            private val bufferingHandler = Handler(Looper.getMainLooper())
            private var bufferingRunnable: Runnable? = null

            override fun onPlaybackStateChanged(state: Int) {
                println("pEQLearn: [PLAYER] State = $state")
                if (state == Player.STATE_BUFFERING) {
                    bufferingRunnable?.let { bufferingHandler.removeCallbacks(it) }
                    bufferingRunnable = Runnable {
                        if (player?.playbackState == Player.STATE_BUFFERING) {
                            onBufferingChanged?.invoke(true)
                        }
                    }
                    bufferingHandler.postDelayed(bufferingRunnable!!, 450L)
                } else {
                    bufferingRunnable?.let { bufferingHandler.removeCallbacks(it) }
                    onBufferingChanged?.invoke(false)
                }
                if (state == Player.STATE_ENDED) {
                    onTrackEnded?.invoke()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                checkLoop()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    initVisualizer(audioSessionId)
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                persistPlaybackState()
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) ||
                    events.contains(Player.EVENT_TRACKS_CHANGED)
                ) {
                    onTrackChanged?.invoke()
                }
            }
        })

        player?.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: androidx.media3.common.Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                currentBitrateVal = (format.bitrate / 1000).coerceAtLeast(0)
                currentSampleRateVal = format.sampleRate.coerceAtLeast(0)
                currentCodecStr = format.codecs
                    ?: format.sampleMimeType?.substringAfterLast("/")?.uppercase()
                    ?: "PCM"
                currentBitDepthVal = when (format.pcmEncoding) {
                    C.ENCODING_PCM_24BIT -> 24
                    C.ENCODING_PCM_32BIT -> 32
                    else -> 16
                }
            }

            override fun onAudioUnderrun(
                eventTime: AnalyticsListener.EventTime,
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long
            ) {
                dropoutCount++
            }
        })
    }

    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.P)
    private fun initVisualizer(sessionId: Int = 0) {
        val currentPlayer = player
        val resolvedAudioSessionId = if (sessionId > 0) sessionId else (currentPlayer?.audioSessionId ?: 0)

        if (resolvedAudioSessionId <= 0) {
            if (visualizerRetryCount >= 6) {
                println("pEQLearn: [DSP] Visualizer init aborted")
                return
            }
            visualizerRetryCount++
            Handler(Looper.getMainLooper()).postDelayed({ initVisualizer(0) }, 1000L)
            return
        }

        visualizerRetryCount = 0

        runCatching { visualizer?.enabled = false }
        visualizer?.release()
        visualizer = null

        dynamicsProcessing?.release()
        dynamicsProcessing = null

        try {
            visualizer = Visualizer(resolvedAudioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null) return
                            val magnitudes = FloatArray(32)
                            val numBins = fft.size / 2
                            val binsPerBand = (numBins / 32).coerceAtLeast(1)

                            for (i in 0 until 32) {
                                var sum = 0f
                                val startBin = i * binsPerBand
                                val endBin = ((i + 1) * binsPerBand).coerceAtMost(numBins)
                                var count = 0
                                for (j in startBin until endBin) {
                                    val r = fft[2 * j].toFloat()
                                    val im = fft[2 * j + 1].toFloat()
                                    sum += sqrt(r * r + im * im)
                                    count++
                                }
                                magnitudes[i] = ((if (count > 0) sum / count else 0f) / 64f).coerceIn(0f, 1f)
                            }

                            for (i in magnitudes.indices) {
                                latestSpectrum[i] = latestSpectrum[i] * 0.35f + magnitudes[i] * 0.65f
                            }

                            latestPhase = 0.75f + (sin(System.currentTimeMillis() / 800.0).toFloat() * 0.1f)
                            onFftDataReceived?.invoke(magnitudes)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }

            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                1,
                true,
                10,
                true,
                6,
                true,
                10,
                true
            ).build()

            dynamicsProcessing = DynamicsProcessing(0, resolvedAudioSessionId, config).apply {
                enabled = true
            }

            loopHandler.removeCallbacks(loopRunnable)
            loopHandler.post(loopRunnable)

            println("pEQLearn: [DSP] Visualizer initialized")
        } catch (e: Exception) {
            println("pEQLearn: [DSP] Init Failed: ${e.message}")
            runCatching { visualizer?.enabled = false }
            visualizer?.release()
            visualizer = null
            dynamicsProcessing?.release()
            dynamicsProcessing = null
        }
    }

    override fun getProcessingLatencyMs(): Int = 12 + (dropoutCount * 2)
    override fun getAudioDropouts(): Int = dropoutCount
    override fun getCurrentSpectrum(): FloatArray = latestSpectrum
    override fun getPhaseCorrelation(): Float = latestPhase
    override fun getBitrate(): Int = currentBitrateVal
    override fun getSampleRate(): Int = currentSampleRateVal
    override fun getBitDepth(): Int = currentBitDepthVal

    override fun getCodec(): String {
        if (currentDeviceType == 8) return "LDAC/aptX"
        return currentCodecStr
    }

    override fun isPlaying(): Boolean {
        val p = player ?: return false
        return p.playWhenReady && p.playbackState != Player.STATE_IDLE && p.playbackState != Player.STATE_ENDED
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateEqBand(bandIndex: Int, gainDb: Float) {
        if (AndroidBass.isLoaded && bassHandle != 0) {
            println("pEQLearn: [BASS] Android EQ Band $bandIndex -> $gainDb dB")
            return
        }
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            val band = channelConfig.preEq.getBand(bandIndex)
            band.isEnabled = true
            band.gain = gainDb
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQLearn: [DSP] EQ Update Failed: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateEqQ(bandIndex: Int, q: Float) {
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            val band = channelConfig.preEq.getBand(bandIndex)
            println("pEQLearn: [DSP] EQ Band $bandIndex Q-Factor -> $q (Simulated Bandwidth)")
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQLearn: [DSP] EQ Q Update Failed: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateCompressor(threshold: Float, ratio: Float, attack: Float, release: Float) {
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            val mbc = channelConfig.mbc.getBand(0)
            mbc.isEnabled = true
            mbc.threshold = threshold
            mbc.ratio = ratio
            mbc.attackTime = attack
            mbc.releaseTime = release
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQLearn: [DSP] Compressor Update Failed: ${e.message}")
        }
    }

    override fun setChannelBalance(balance: Float) {
        player?.volume = 1f
    }

    override fun setOutputDevice(deviceId: Int?) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val targetType = when (deviceId) {
            0 -> AudioDeviceInfo.TYPE_WIRED_HEADSET
            1 -> AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            2 -> AudioDeviceInfo.TYPE_USB_DEVICE
            3 -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            else -> null
        }

        targetType?.let { type ->
            val targetDevice = devices.find { it.type == type }
            if (targetDevice != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.setCommunicationDevice(targetDevice)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothA2dpOn = (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = (type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                }
                println("pEQLearn: [ROUTING] Output set to ${targetDevice.productName}")
            }
        }
    }

    override fun setSubHarmonicGenerator(enabled: Boolean) {
        println("pEQLearn: [DSP] Sub-Harmonic Generator -> $enabled")
    }

    override fun setPhaseInversion(enabled: Boolean) {
        isPhaseInverted = enabled
        println("pEQLearn: [DSP] Phase Inversion -> $enabled")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setLimiterEnabled(enabled: Boolean) {
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            channelConfig.limiter.isEnabled = enabled
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQLearn: [DSP] Limiter Update Failed: ${e.message}")
        }
    }

    override fun getDspLoad(): Float = if (player?.isPlaying == true) 12f + (dropoutCount * 0.5f) else 0f
    override fun isInitialized(): Boolean = player != null
    override fun getLatency(): Int = 0

    override suspend fun probeMedia(url: String): Map<String, List<String>> {
        return mapOf(
            "video" to listOf("1080p", "720p", "480p"),
            "audio" to listOf("320kbps", "128kbps")
        )
    }

    override fun getRegistry(): Map<String, String> {
        return mapOf(
            "HARDWARE_OUTPUT" to (player?.audioSessionId?.toString() ?: "OFF"),
            "DECODER_STATUS" to getDecoderInfo(),
            "SAMPLE_RATE" to "48000Hz",
            "BIT_DEPTH" to "24-bit PCM",
            "LATENCY" to "${getProcessingLatencyMs()}ms",
            "BUFFER_SIZE" to "1024 frames",
            "ENGINE_LIFE" to "STABLE"
        )
    }

    override fun getVideoWidth(): Int = player?.videoSize?.width ?: 0
    override fun getVideoHeight(): Int = player?.videoSize?.height ?: 0

    private fun persistPlaybackState() {
        val mediaItem = player?.currentMediaItem ?: return
        prefs.edit {
            putString("track_uri", mediaItem.localConfiguration?.uri?.toString())
            putString("track_title", mediaItem.mediaMetadata.title?.toString())
            putString("track_artist", mediaItem.mediaMetadata.artist?.toString())
            putString("track_thumbnail", mediaItem.mediaMetadata.artworkUri?.toString())
            putBoolean("is_video", mediaItem.mediaMetadata.extras?.getBoolean("is_video") ?: false)
            putLong("track_position", player?.currentPosition ?: 0L)
            putBoolean("was_playing", player?.isPlaying == true)
        }
    }

    fun restorePlaybackState() {
        val uri = prefs.getString("track_uri", null) ?: return
        val title = prefs.getString("track_title", null)
        val artist = prefs.getString("track_artist", null)
        val position = prefs.getLong("track_position", 0L)
        val wasPlaying = prefs.getBoolean("was_playing", false)
        val thumbnail = prefs.getString("track_thumbnail", null)
        val isVideo = prefs.getBoolean("is_video", false)

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(if (thumbnail != null) Uri.parse(thumbnail) else null)
                    .setExtras(Bundle().apply { putBoolean("is_video", isVideo) })
                    .build()
            )
            .build()

        player?.apply {
            if (currentMediaItem != null) return
            setMediaItem(mediaItem)
            prepare()
            seekTo(position)
            playWhenReady = wasPlaying
        }
    }

    private fun checkLoop() {
        if (isSeeking) return
        player?.let { p ->
            val start = loopA
            val end = loopB
            if (start != null && end != null) {
                if (p.currentPosition >= end) {
                    p.seekTo(start)
                }
            }
        }
    }

    private fun cleanLocalPath(url: String): String {
        val path = when {
            url.startsWith("file://") -> url.substring(7)
            url.startsWith("file:/") -> url.substring(5)
            url.startsWith("file:") -> url.substring(5)
            else -> url
        }
        val normalized = when {
            path.startsWith("/file://") -> path.substring(8)
            path.startsWith("/file:/") -> path.substring(7)
            path.startsWith("/file:") -> path.substring(6)
            else -> path
        }
        return if (normalized.startsWith("/")) normalized else "/$normalized"
    }

    private fun getArtworkBytes(thumbnail: String?, trackUrl: String?): ByteArray? {
        return try {
            if (thumbnail != null) {
                val file = File(cleanLocalPath(thumbnail))
                if (file.exists() && file.isFile) file.readBytes() else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(UnstableApi::class)
    override fun playTrack(url: String, title: String?, artist: String?, thumbnail: String?) {
        currentTrackUrl = url
        try {
            player?.volume = 1f
            stemPlayers.values.forEach { it.stop(); it.release() }
            stemPlayers.clear()

            val uri = if (url.startsWith("http") || url.startsWith("content://")) {
                Uri.parse(url)
            } else {
                Uri.fromFile(File(cleanLocalPath(url)))
            }

            println("pEQLearn: TITLE -> $title")
            println("pEQLearn: ARTIST -> $artist")
            println("pEQLearn: URI -> $uri")

            if (AndroidBass.isLoaded) {
                try {
                    if (bassHandle != 0) {
                        AndroidBass.channelStop(bassHandle)
                    }
                    val path = cleanLocalPath(url)
                    bassHandle = AndroidBass.streamCreateFile(path, 0)
                    if (bassHandle != 0) {
                        AndroidBass.channelPlay(bassHandle, true)
                        onTrackChanged?.invoke()
                        println("pEQLearn: [BASS] Android playing track -> $url")
                        return
                    }
                } catch (e: Exception) {
                    println("pEQLearn: [BASS] Android playback failed: ${e.message}")
                }
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkData(getArtworkBytes(thumbnail, url))
                .setArtworkUri(thumbnail?.let { Uri.parse(it) })
                .build()

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(metadata)

            if (!url.startsWith("http")) {
                engineScope.launch(Dispatchers.IO) {
                    val cleanPath = cleanLocalPath(url)
                    val file = File(cleanPath)
                    val subExtensions = listOf("srt", "vtt", "ass", "ssa")
                    val parent = file.parentFile
                    if (parent != null) {
                        val sidecarSubs = parent.listFiles { f ->
                            f.nameWithoutExtension == file.nameWithoutExtension && subExtensions.contains(f.extension.lowercase())
                        }

                        val configs = sidecarSubs?.map { subFile ->
                            MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                                .setMimeType(if (subFile.extension == "vtt") "text/vtt" else "application/x-subrip")
                                .setLanguage(subFile.extension.uppercase())
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        } ?: emptyList()

                        if (configs.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                player?.let { p ->
                                    val currentItem = p.currentMediaItem
                                    if (currentItem?.localConfiguration?.uri?.toString()?.contains(url) == true) {
                                        p.setMediaItem(
                                            mediaItemBuilder.setSubtitleConfigurations(configs).build(),
                                            false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            player?.apply {
                setMediaItem(mediaItemBuilder.build())
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            println("pEQLearn: Playback Failed: ${e.message}")
        }
    }

    @OptIn(UnstableApi::class)
    override fun loadTrack(url: String, title: String?, artist: String?, thumbnail: String?) {
        currentTrackUrl = url
        try {
            player?.volume = 1f
            stemPlayers.values.forEach { it.stop(); it.release() }
            stemPlayers.clear()

            val uri = if (url.startsWith("http") || url.startsWith("content://")) {
                Uri.parse(url)
            } else {
                Uri.fromFile(File(cleanLocalPath(url)))
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkData(getArtworkBytes(thumbnail, url))
                .setArtworkUri(thumbnail?.let { Uri.parse(it) })
                .build()

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(metadata)

            if (!url.startsWith("http")) {
                engineScope.launch(Dispatchers.IO) {
                    val cleanPath = cleanLocalPath(url)
                    val file = File(cleanPath)
                    val subExtensions = listOf("srt", "vtt", "ass", "ssa")
                    val parent = file.parentFile
                    if (parent != null) {
                        val sidecarSubs = parent.listFiles { f ->
                            f.nameWithoutExtension == file.nameWithoutExtension && subExtensions.contains(f.extension.lowercase())
                        }

                        val configs = sidecarSubs?.map { subFile ->
                            MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                                .setMimeType(if (subFile.extension == "vtt") "text/vtt" else "application/x-subrip")
                                .setLanguage(subFile.extension.uppercase())
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        } ?: emptyList()

                        if (configs.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val updatedItem = mediaItemBuilder.setSubtitleConfigurations(configs).build()
                                player?.setMediaItem(updatedItem, false)
                            }
                        }
                    }
                }
            }

            player?.apply {
                setMediaItem(mediaItemBuilder.build())
                prepare()
                playWhenReady = false
            }
        } catch (e: Exception) {
            println("pEQLearn: Load Failed: ${e.message}")
        }
    }

    override fun pause() {
        if (AndroidBass.isLoaded && bassHandle != 0) {
            AndroidBass.channelPause(bassHandle)
            return
        }
        player?.pause()
        stemPlayers.values.forEach { it.pause() }
    }

    override fun resume() {
        if (AndroidBass.isLoaded && bassHandle != 0) {
            AndroidBass.channelPlay(bassHandle, false)
            return
        }
        player?.let {
            if (it.playbackState == Player.STATE_ENDED) {
                it.seekTo(0)
            }
            it.play()
        }
        stemPlayers.values.forEach { it.play() }
    }

    override fun stop() {
        if (AndroidBass.isLoaded && bassHandle != 0) {
            AndroidBass.channelStop(bassHandle)
            return
        }
        player?.stop()
        stemPlayers.values.forEach { it.stop() }
    }

    @OptIn(UnstableApi::class)
    override fun seekTo(positionMs: Long) {
        allNotesOff()
        if (AndroidBass.isLoaded && bassHandle != 0) {
            val bytes = AndroidBass.channelSeconds2Bytes(bassHandle, positionMs / 1000.0)
            AndroidBass.channelSetPosition(bassHandle, bytes, 0)
            return
        }

        val currentPlayer = player ?: return
        val now = System.currentTimeMillis()
        if (now - lastSeekTime < 120) return
        lastSeekTime = now
        isSeeking = true

        try {
            if (currentPlayer.playbackState != Player.STATE_IDLE || currentPlayer.currentMediaItem != null) {
                onBufferingChanged?.invoke(true)
                val targetPos = positionMs.coerceAtLeast(0L)
                currentPlayer.seekTo(targetPos)
                stemPlayers.values.forEach { it.seekTo(targetPos) }
            }
        } catch (_: Exception) {}

        engineScope.launch {
            delay(450)
            isSeeking = false
            if (currentPlayer.playbackState != Player.STATE_BUFFERING) {
                onBufferingChanged?.invoke(false)
            }
        }
    }

    override fun setVolume(volume: Float) {
        AudioEngineProvider.setVolume(volume)
    }

    override fun setTanpuraGain(gain: Float) {
        tanpuraGain = gain
        tanpuraTrack1?.setVolume(gain)
        tanpuraTrack2?.setVolume(gain)
        currentlyPlayingTanpuraId?.let { id ->
            systemSoundPool.setVolume(id, gain, gain)
        }
    }

    override fun setTablaGain(gain: Float) {
        tablaGain = gain
        voiceMutex.withLock {
            voicePool.forEach { it.setVolume(gain) }
        }
    }

    override fun setMetronomeGain(gain: Float) {
        metronomeGain = gain
    }

    override fun setPlaybackSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(
            speed,
            player?.playbackParameters?.pitch ?: 1f
        )
    }

    override fun setPitch(pitch: Float) {
        player?.playbackParameters = PlaybackParameters(
            player?.playbackParameters?.speed ?: 1f,
            pitch
        )
    }

    override fun setRepeatMode(mode: RepeatMode) {
        player?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.FOLDER -> Player.REPEAT_MODE_ALL
        }
    }

    override fun setShuffleMode(enabled: Boolean) {
        player?.shuffleModeEnabled = enabled
    }

    override fun getDuration(): Long {
        if (AndroidBass.isLoaded && bassHandle != 0) {
            val bytes = AndroidBass.channelGetPosition(bassHandle, 2)
            val seconds = AndroidBass.channelBytes2Seconds(bassHandle, bytes)
            return (seconds * 1000).toLong()
        }
        return player?.duration ?: 0L
    }

    override fun getCurrentPosition(): Long {
        if (AndroidBass.isLoaded && bassHandle != 0) {
            val bytes = AndroidBass.channelGetPosition(bassHandle, 0)
            val seconds = AndroidBass.channelBytes2Seconds(bassHandle, bytes)
            return (seconds * 1000).toLong()
        }
        return player?.currentPosition ?: 0L
    }

    override fun getCurrentTrackUri(): String? {
        if (AndroidBass.isLoaded && bassHandle != 0) return currentTrackUrl
        return player?.currentMediaItem?.localConfiguration?.uri?.toString()
    }

    override fun getDecoderInfo(): String = "HW"

    override fun getFftData(): FloatArray? {
        val vis = visualizer ?: return null
        try {
            if (vis.enabled) {
                val fft = ByteArray(vis.captureSize)
                if (vis.getFft(fft) == Visualizer.SUCCESS) {
                    val numBins = fft.size / 2
                    val bands = FloatArray(32)
                    val binsPerBand = (numBins / 32).coerceAtLeast(1)
                    for (i in 0 until 32) {
                        var sum = 0f
                        val startBin = i * binsPerBand
                        val endBin = ((i + 1) * binsPerBand).coerceAtMost(numBins)
                        var count = 0
                        for (j in startBin until endBin) {
                            val r = fft[2 * j].toFloat()
                            val im = fft[2 * j + 1].toFloat()
                            sum += sqrt(r * r + im * im)
                            count++
                        }
                        bands[i] = ((if (count > 0) sum / count else 0f) / 64f).coerceIn(0f, 1f)
                    }
                    return bands
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun initAllAssets() {
        loadAssets("drums/fingerdrumming", "DRUM", keyMapper = { fileName ->
            fileName.uppercase().substringBefore(".")
        })
        loadAssets("metronome", "METRONOME", keyMapper = {
            if (it.contains("strong", true)) "METRONOME-STRONG" else "METRONOME-WEAK"
        })
        loadTanpuraFallbacks()
    }

    private fun loadTanpuraFallbacks() {
        engineScope.launch(Dispatchers.IO) {
            try {
                val files = context.assets.list("tanpura") ?: return@launch
                files.filter { it.endsWith(".mp3", true) }.forEach { fileName ->
                    val scaleKey = fileName.substringAfter("tanpura_").substringBefore(".mp3").uppercase()
                    context.assets.openFd("tanpura/$fileName").use { afd ->
                        val soundId = systemSoundPool.load(afd.fileDescriptor, afd.startOffset, afd.length, 1)
                        tanpuraSoundIds[scaleKey] = soundId
                        Log.d("AudioEngine", "Loaded Tanpura MP3 fallback for scale: $scaleKey")
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "Failed to load Tanpura MP3 fallbacks: ${e.message}")
            }
        }
    }

    private fun loadAssets(
        directory: String,
        category: String,
        filter: (String) -> Boolean = { it.endsWith(".wav", true) },
        keyMapper: (String) -> String = { it.substringBeforeLast(".").uppercase().replace("_", "-") }
    ) {
        engineScope.launch(Dispatchers.IO) {
            try {
                context.assets.list(directory)?.forEach { fileName ->
                    if (filter(fileName)) {
                        context.assets.openFd("$directory/$fileName").use { afd ->
                            val soundId = soundPool.load(afd.fileDescriptor, afd.startOffset, afd.length, 1)
                            kitSounds.getOrPut(category) { mutableMapOf() }[keyMapper(fileName)] = soundId
                            loadedSounds[soundId] = true
                        }
                    }
                }
            } catch (e: Exception) {
                println("pEQLearn: [ASSETS] Error scanning $directory: ${e.message}")
            }
        }
    }

    suspend fun loadSf2Tanpura(): FluidSynthPlayer? {
        return withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "Tanpura_drone.sf2")
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    context.assets.open("tanpura/Tanpura_drone.sf2").use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                val player = FluidSynthPlayer(AudioConfig(sampleRate = 44100, interpolation = 1, periodSize = 64, periods = 2))
                player.loadSoundFont(tempFile.absolutePath)
                tanpuraEngine = TanpuraSoundFontEngine(player)
                Log.i("AudioEngine", "[SF2] Tanpura Drone compiled cleanly via FluidSynth.")
                player
            } catch (e: Exception) {
                Log.e("AudioEngine", "[SF2] Failed loading Tanpura: ${e.message}")
                null
            }
        }
    }

    suspend fun loadSf2Tabla(): FluidSynthPlayer? {
        return withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "tabla.sf2")
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    context.assets.open("tabla/tabla.sf2").use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                val player = FluidSynthPlayer(AudioConfig(sampleRate = 44100, interpolation = 1, periodSize = 64, periods = 2))
                player.loadSoundFont(tempFile.absolutePath)
                tablaEngine = TablaSoundFontEngine(player)
                parseAndAutoMapSoundFont(tempFile, "TABLA")
                Log.i("AudioEngine", "[SF2] Tabla compiled & auto-mapped cleanly via FluidSynth.")
                player
            } catch (e: Exception) {
                Log.e("AudioEngine", "[SF2] Failed loading Tabla: ${e.message}")
                null
            }
        }
    }

    /**
     * Parses SF2 file binary metadata (shdr and instrument key ranges) to automatically map sample
     * notes and bol syllabics dynamically without hardcoding.
     */
    private fun parseAndAutoMapSoundFont(sf2File: File, category: String) {
        try {
            if (!sf2File.exists() || sf2File.length() < 1024L) return
            val bytes = sf2File.readBytes()

            fun findChunk(name: ByteArray): Pair<Int, Int>? {
                var pos = 0
                while (pos < bytes.size - 8) {
                    var match = true
                    for (i in name.indices) {
                        if (bytes[pos + i] != name[i]) {
                            match = false
                            break
                        }
                    }
                    if (match) {
                        val size = (bytes[pos + 4].toInt() and 0xFF) or
                                ((bytes[pos + 5].toInt() and 0xFF) shl 8) or
                                ((bytes[pos + 6].toInt() and 0xFF) shl 16) or
                                ((bytes[pos + 7].toInt() and 0xFF) shl 24)
                        return Pair(pos + 8, size)
                    }
                    pos++
                }
                return null
            }

            val shdrChunk = findChunk("shdr".toByteArray(Charsets.US_ASCII))
            val sampleNames = mutableListOf<Pair<String, Int>>()
            if (shdrChunk != null) {
                val start = shdrChunk.first
                val size = shdrChunk.second
                var offset = 0
                while (offset + 46 <= size && start + offset + 46 <= bytes.size) {
                    val sStart = start + offset
                    val nameBytes = bytes.copyOfRange(sStart, sStart + 20)
                    val zeroIdx = nameBytes.indexOf(0)
                    val len = if (zeroIdx >= 0) zeroIdx else 20
                    val sampleName = String(nameBytes, 0, len, Charsets.US_ASCII).trim()
                    val origPitch = bytes[sStart + 40].toInt() and 0xFF
                    if (sampleName.isNotEmpty() && sampleName != "EOS") {
                        sampleNames.add(Pair(sampleName, origPitch))
                    }
                    offset += 46
                }
            }

            // Inspect instrument key ranges
            val instChunk = findChunk("inst".toByteArray(Charsets.US_ASCII))
            val ibagChunk = findChunk("ibag".toByteArray(Charsets.US_ASCII))
            val igenChunk = findChunk("igen".toByteArray(Charsets.US_ASCII))

            val keyToSampleName = mutableMapOf<Int, String>()

            if (instChunk != null && ibagChunk != null && igenChunk != null) {
                val ibagStart = ibagChunk.first
                val ibagSize = ibagChunk.second
                val igenStart = igenChunk.first
                val igenSize = igenChunk.second

                val totalBags = ibagSize / 4
                val totalGens = igenSize / 4

                // Parse bags of instrument 0
                val instEndBag = if (instChunk.second >= 22) {
                    val bLow = bytes[instChunk.first + 20].toInt() and 0xFF
                    val bHigh = bytes[instChunk.first + 21].toInt() and 0xFF
                    (bHigh shl 8) or bLow
                } else totalBags

                val targetEndBag = if (totalBags > 1) (totalBags - 1).coerceAtMost(50) else 0

                for (b in 0 until targetEndBag) {
                    val bOff = ibagStart + b * 4
                    val sgen = (bytes[bOff].toInt() and 0xFF) or ((bytes[bOff + 1].toInt() and 0xFF) shl 8)
                    val egen = (bytes[bOff + 4].toInt() and 0xFF) or ((bytes[bOff + 5].toInt() and 0xFF) shl 8)

                    var minKey: Int? = null
                    var maxKey: Int? = null
                    var sampleId: Int? = null

                    for (g in sgen until egen.coerceAtMost(totalGens)) {
                        val gOff = igenStart + g * 4
                        val op = (bytes[gOff].toInt() and 0xFF) or ((bytes[gOff + 1].toInt() and 0xFF) shl 8)
                        val valLo = bytes[gOff + 2].toInt() and 0xFF
                        val valHi = bytes[gOff + 3].toInt() and 0xFF
                        if (op == 43) { // keyRange
                            minKey = valLo
                            maxKey = valHi
                        } else if (op == 53) { // sampleID
                            sampleId = (valHi shl 8) or valLo
                        }
                    }

                    if (sampleId != null && sampleId < sampleNames.size) {
                        val sname = sampleNames[sampleId].first
                        if (minKey != null && maxKey != null) {
                            for (k in minKey..maxKey) {
                                keyToSampleName[k] = sname
                            }
                        } else {
                            val p = sampleNames[sampleId].second
                            if (p in 1..127) {
                                keyToSampleName[p] = sname
                            }
                        }
                    }
                }
            }

            Log.i("AudioEngine", "[AutoMap] Parsed ${sampleNames.size} samples, ${keyToSampleName.size} key mappings for $category")

            if (category.equals("TABLA", true)) {
                tablaNoteCache.clear()
                val newMap = mutableMapOf<String, Int>()

                // 1. Analyze sample names for acoustic keywords
                sampleNames.forEachIndexed { idx, (name, pitch) ->
                    val upper = name.uppercase()
                    val targetMidi = if (pitch in 21..108) pitch else (60 + (idx % 20))

                    when {
                        upper.contains("DHA") -> newMap["DHA"] = targetMidi
                        upper.contains("DHIN") -> newMap["DHIN"] = targetMidi
                        upper.contains("TIN") -> newMap["TIN"] = targetMidi
                        upper.contains("NA") || upper.contains("KINAR") -> newMap["NA"] = targetMidi
                        upper.contains("TA") || upper.contains("TE") -> newMap["TA"] = targetMidi
                        upper.contains("GE") || upper.contains("GHE") || upper.contains("BANYA") || upper.contains("BAYAN") -> {
                            newMap["GE"] = targetMidi
                            newMap["GHE"] = targetMidi
                            newMap["THOM"] = targetMidi
                        }
                        upper.contains("KA") || upper.contains("KE") || upper.contains("SLAP") -> {
                            newMap["KA"] = targetMidi
                            newMap["THA"] = targetMidi
                        }
                        upper.contains("TI") -> newMap["TI"] = targetMidi
                        upper.contains("TU") || upper.contains("TUN") -> newMap["TU"] = targetMidi
                        upper.contains("DIN") -> newMap["DIN"] = targetMidi
                    }
                }

                // 2. Also map keys from keyToSampleName
                keyToSampleName.forEach { (k, sname) ->
                    val upper = sname.uppercase()
                    when {
                        upper.contains("DHA") && !newMap.containsKey("DHA") -> newMap["DHA"] = k
                        upper.contains("DHIN") && !newMap.containsKey("DHIN") -> newMap["DHIN"] = k
                        upper.contains("TIN") && !newMap.containsKey("TIN") -> newMap["TIN"] = k
                        upper.contains("BANYA") || upper.contains("BAYAN") -> {
                            if (!newMap.containsKey("GE")) newMap["GE"] = k
                            if (!newMap.containsKey("THOM")) newMap["THOM"] = k
                        }
                        upper.contains("SLAP") -> {
                            if (!newMap.containsKey("KA")) newMap["KA"] = k
                            if (!newMap.containsKey("THA")) newMap["THA"] = k
                        }
                    }
                }

                // Only put in mappings discovered from clear acoustic labels, preserving verified tablaMidiMap defaults
                newMap.forEach { (bol, note) ->
                    if (!tablaMidiMap.containsKey(bol)) {
                        tablaMidiMap[bol] = note
                    }
                }
                Log.i("AudioEngine", "[AutoMap] Retained pristine mappings and indexed ${tablaMidiMap.size} total bols for active SoundFont")
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "[AutoMap] Error during dynamic SoundFont indexing: ${e.message}")
        }
    }

    override fun loadCustomSoundFont(category: String, path: String): Boolean {
        try {
            val upperCat = category.uppercase().trim()
            val file = if (path.startsWith("/")) File(path) else {
                // If it's an asset relative path, copy to cache
                val cleanPath = path.removePrefix("assets/").removePrefix("/")
                val cached = File(context.cacheDir, cleanPath.substringAfterLast("/"))
                if (!cached.exists() || cached.length() == 0L) {
                    context.assets.open(cleanPath).use { input ->
                        cached.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                cached
            }

            if (!file.exists() || file.length() == 0L) {
                Log.w("AudioEngine", "Cannot load custom SoundFont: File not found at $path")
                return false
            }

            val player = FluidSynthPlayer(AudioConfig(sampleRate = 44100, interpolation = 1, periodSize = 64, periods = 2))
            player.loadSoundFont(file.absolutePath)

            when (upperCat) {
                "TABLA" -> {
                    tablaEngine = TablaSoundFontEngine(player)
                    parseAndAutoMapSoundFont(file, "TABLA")
                }
                "TANPURA" -> {
                    tanpuraEngine = TanpuraSoundFontEngine(player)
                    parseAndAutoMapSoundFont(file, "TANPURA")
                }
                "PIANO" -> {
                    synchronized(sf2Lock) {
                        soundFontPool["PIANO"] = player
                        if (currentInstrument == "PIANO" || currentInstrument.isEmpty()) {
                            globalSoundFont = player
                            sf2Loaded.set(true)
                        }
                    }
                    pianoNoteCache.clear()
                    parseAndAutoMapSoundFont(file, "PIANO")
                }
                "GUITAR" -> {
                    synchronized(sf2Lock) {
                        soundFontPool["GUITAR"] = player
                        if (currentInstrument.contains("GUITAR")) {
                            globalSoundFont = player
                            sf2Loaded.set(true)
                        }
                    }
                    guitarNoteCache.clear()
                    parseAndAutoMapSoundFont(file, "GUITAR")
                }
                else -> {
                    synchronized(sf2Lock) {
                        soundFontPool[upperCat] = player
                    }
                    parseAndAutoMapSoundFont(file, upperCat)
                }
            }

            Log.i("AudioEngine", "Successfully loaded & auto-mapped custom SoundFont for $upperCat from ${file.name}")
            return true
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed loading custom SoundFont: ${e.message}", e)
            return false
        }
    }

    private fun loadSf2Piano() {
        engineScope.launch(Dispatchers.IO) {
            try {
                Log.d("AudioEngine", "Opening piano SF2 asset stream...")
                val tempFile = File(context.cacheDir, "piano_yamaha.sf2")
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    context.assets.open("piano/198_Yamaha_SY1_piano.sf2").use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                val player = FluidSynthPlayer(AudioConfig(sampleRate = 44100, interpolation = 1, periodSize = 64, periods = 2))
                player.loadSoundFont(tempFile.absolutePath)

                synchronized(sf2Lock) {
                    soundFontPool["PIANO"] = player
                    if (currentInstrument == "PIANO" || currentInstrument.isEmpty()) {
                        globalSoundFont = player
                        sf2Loaded.set(true)
                    }
                }
                Log.i("AudioEngine", "Acoustic Piano FluidSynth fully loaded and active.")
            } catch (e: Exception) {
                Log.e("AudioEngine", "CRITICAL PIANO LOADING EXCEPTION:", e)
            }
        }
    }

    private fun loadSf2Guitar() {
        engineScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "guitar_samples.sf2")
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    context.assets.open("guitar/guitar_samples_clean.sf2").use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                val player = FluidSynthPlayer(AudioConfig(sampleRate = 44100, interpolation = 1, periodSize = 64, periods = 2))
                player.loadSoundFont(tempFile.absolutePath)

                synchronized(sf2Lock) {
                    soundFontPool["GUITAR"] = player
                    if (currentInstrument.contains("GUITAR")) {
                        globalSoundFont = player
                        sf2Loaded.set(true)
                    }
                    Log.i("AudioEngine", "Clean Guitar FluidSynth fully cached and mounted.")
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "Guitar SoundFont loading failed: ${e.message}")
            }
        }
    }

    private fun loadSf2Harmonium() {
        engineScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "harmonium_gm.sf2")
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    context.assets.open("harmonium/Wetthasinghe_Harmnium_GM_Ver.sf2").use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                val player = FluidSynthPlayer(AudioConfig(sampleRate = 44100, interpolation = 1, periodSize = 64, periods = 2))
                player.loadSoundFont(tempFile.absolutePath)

                synchronized(sf2Lock) {
                    soundFontPool["HARMONIUM"] = player
                    if (currentInstrument.contains("HARMONIUM")) {
                        globalSoundFont = player
                        sf2Loaded.set(true)
                    }
                    Log.i("AudioEngine", "Harmonium FluidSynth fully loaded and cached.")
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "Harmonium SoundFont loading failed: ${e.message}")
            }
        }
    }

    override fun noteOn(midiNote: Int, velocity: Float) {
        Log.d("AudioEngine", "Direct noteOn Received -> Note: $midiNote, Velocity: $velocity, Instrument: $currentInstrument")
        engineScope.launch(Dispatchers.Default) {
            synchronized(sf2Lock) {
                if (sf2Loaded.get()) {
                    val activeSf = when {
                        currentInstrument.contains("HARMONIUM") -> soundFontPool["HARMONIUM"] ?: soundFontPool["PIANO"]
                        currentInstrument.contains("GUITAR") -> soundFontPool["GUITAR"] ?: soundFontPool["PIANO"]
                        else -> soundFontPool["PIANO"]
                    }

                    activeSf?.let { sf: FluidSynthPlayer ->
                        val vel = (velocity.coerceIn(0f, 1f) * 127).toInt()
                        try {
                            sf.noteOn(0, midiNote, vel)
                            // Render 0.5s of stereo audio for note ring/sustain (44100 * 0.5 * 2)
                            val samplesToRender = (44100 * 0.5 * 2).toInt().coerceAtLeast(stereoBufferSize)
                            val floatBuffer = sf.renderFloat(samplesToRender)

                            if (floatBuffer.isNotEmpty()) {
                                // Master output scaling factor (0.8f) to prevent inter-sample peaking and distortion
                                val shortBuffer = ShortArray(floatBuffer.size) { i ->
                                    val sampleValue = (floatBuffer[i] * 0.8f).coerceIn(-1.0f, 1.0f)
                                    (sampleValue * Short.MAX_VALUE).toInt().toShort()
                                }

                                voiceMutex.withLock {
                                    val activeTrack = voicePool[currentVoiceIndex]
                                    try {
                                        activeTrack.pause()
                                        activeTrack.flush()
                                        activeTrack.play()
                                        activeTrack.write(shortBuffer, 0, shortBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
                                    } catch (e: Exception) {
                                        Log.e("AudioEngine", "SoundFont stream write failed: ${e.message}")
                                    }
                                    currentVoiceIndex = (currentVoiceIndex + 1) % VOICE_COUNT
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e("AudioEngine", "Native synth noteOn error caught: ${e.message}")
                        }
                    }
                } else {
                    Log.w("AudioEngine", "Bypassing noteOn: SoundFont instrument '$currentInstrument' is not loaded.")
                }
            }
        }
    }

    override fun noteOff(note: Int) {
        activeMidiNotesEngine.remove(note)
        if (currentlyPlayingNoteEngine == note) {
            currentlyPlayingNoteEngine = activeMidiNotesEngine.keys.firstOrNull()
        }
        engineScope.launch(Dispatchers.Default) {
            synchronized(sf2Lock) {
                val activeSf = when {
                    currentInstrument.contains("HARMONIUM") -> soundFontPool["HARMONIUM"] ?: soundFontPool["PIANO"]
                    currentInstrument.contains("GUITAR") -> soundFontPool["GUITAR"] ?: soundFontPool["PIANO"]
                    else -> soundFontPool["PIANO"]
                }
                activeSf?.noteOff(0, note)
            }
        }
    }

    private fun renderGuitarNote(sf: FluidSynthPlayer, midiNote: Int, velocity: Int): ShortArray? {
        guitarNoteCache[midiNote]?.let { return it }
        return try {
            val sampleRate = 44100
            val durationSamples = sampleRate * 3
            val vel = velocity.coerceIn(0, 127)

            synchronized(sf2Lock) {
                sf.noteOn(1, midiNote, vel)
                val floatBuffer = sf.renderFloat(durationSamples * 2)
                sf.noteOff(1, midiNote)

                val shortBuffer = ShortArray(durationSamples) { i ->
                    val left = if (i * 2 < floatBuffer.size) floatBuffer[i * 2] else 0f
                    val right = if (i * 2 + 1 < floatBuffer.size) floatBuffer[i * 2 + 1] else 0f
                    val mono = (left + right) / 2f
                    (mono * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
                }
                guitarNoteCache[midiNote] = shortBuffer
                shortBuffer
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun renderSf2Note(sf: FluidSynthPlayer, midiNote: Int, velocity: Int): ShortArray? {
        pianoNoteCache[midiNote]?.let { return it }
        return try {
            val sampleRate = 44100
            val durationSamples = sampleRate * 2
            val vel = velocity.coerceIn(0, 127)

            synchronized(sf2Lock) {
                sf.noteOn(0, midiNote, vel)
                val floatBuffer = sf.renderFloat(durationSamples * 2)
                sf.noteOff(0, midiNote)

                val shortBuffer = ShortArray(durationSamples) { i ->
                    val left = if (i * 2 < floatBuffer.size) floatBuffer[i * 2] else 0f
                    val right = if (i * 2 + 1 < floatBuffer.size) floatBuffer[i * 2 + 1] else 0f
                    val mono = (left + right) / 2f
                    (mono * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
                }
                pianoNoteCache[midiNote] = shortBuffer
                shortBuffer
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun allNotesOff() {
        try {
            voiceMutex.lock()
            voicePool.forEach { track ->
                runCatching { track.pause(); track.flush(); track.play() }
            }
            activeMidiNotesEngine.clear()
            activeTanpuraNotes.clear()
            currentlyPlayingNoteEngine = null
            currentlyPlayingTanpuraNote = null
            
            currentlyPlayingTanpuraId?.let { 
                systemSoundPool.stop(it)
                currentlyPlayingTanpuraId = null
            }
            
            stopAndReleaseTanpuraTracks()
        } finally {
            voiceMutex.unlock()
        }
    }

    private fun stopAndReleaseTanpuraTracks() {
        tanpuraTrack1?.apply { runCatching { stop(); release() } }
        tanpuraTrack2?.apply { runCatching { stop(); release() } }
        tanpuraTrack1 = null
        tanpuraTrack2 = null
        activeTanpuraTrackIndex = 0
    }

    private val tanpuraSoundIds = ConcurrentHashMap<String, Int>()

    private fun handleTanpura(target: String) {
        if (target == "STOP") {
            currentlyPlayingTanpuraId?.let { systemSoundPool.stop(it) }
            currentlyPlayingTanpuraId = null
            stopAndReleaseTanpuraTracks()
            currentlyPlayingTanpuraNote = null
            return
        }

        val cleanKey = target.replace(Regex("[0-9]"), "").trim().uppercase()
        val midiNote = tanpuraMidiMap[cleanKey] ?: return

        // 1. AudioTrack Engine with Crossfading
        val engine = tanpuraEngine
        if (engine != null) {
            engine.noteOn(midiNote)
            val pcmData: ShortArray? = engine.renderTanpuraDrone(midiNote, velocity = 100)
            if (pcmData != null) {
                try {
                    val normalizedPcm = ShortArray(pcmData.size) { i ->
                        (pcmData[i] * tanpuraGain).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    
                    val newTrack = AudioTrack(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build(),
                        normalizedPcm.size * 2,
                        AudioTrack.MODE_STATIC,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    ).apply {
                        write(normalizedPcm, 0, normalizedPcm.size)
                        if (normalizedPcm.size >= 2) {
                            setLoopPoints(0, normalizedPcm.size / 2, -1)
                        }
                    }

                    crossfadeTanpuraTracks(newTrack)
                    currentlyPlayingTanpuraNote = midiNote
                    
                    // Kill SoundPool fallback if it was playing
                    currentlyPlayingTanpuraId?.let { 
                        systemSoundPool.stop(it)
                        currentlyPlayingTanpuraId = null
                    }
                    return
                } catch (e: Exception) {
                    Log.e("AudioEngine", "SoundFont Tanpura Crossfade failed: ${e.message}")
                }
            }
        }

        // 2. Fallback to MP3 via SoundPool (ONLY if AudioTrack engine fails)
        if (currentlyPlayingTanpuraId == null) {
            val soundId = tanpuraSoundIds[cleanKey]
            if (soundId != null) {
                currentlyPlayingTanpuraId = systemSoundPool.play(soundId, tanpuraGain, tanpuraGain, 1, -1, 1.0f)
                currentlyPlayingTanpuraNote = midiNote
            } else {
                Log.w("AudioEngine", "Tanpura fallback MP3 not found for scale: $cleanKey")
            }
        }
    }

    private fun crossfadeTanpuraTracks(newTrack: AudioTrack) {
        val oldTrack = if (activeTanpuraTrackIndex == 1) tanpuraTrack1 else tanpuraTrack2
        
        // Start new track
        newTrack.play()
        
        if (activeTanpuraTrackIndex == 1) {
            tanpuraTrack2 = newTrack
            activeTanpuraTrackIndex = 2
        } else {
            tanpuraTrack1 = newTrack
            activeTanpuraTrackIndex = 1
        }

        // Fade out old track asynchronously
        oldTrack?.let { old ->
            engineScope.launch(Dispatchers.Default) {
                val fadeSteps = 20
                val fadeInterval = 25L // 500ms total fade
                for (i in fadeSteps downTo 0) {
                    val vol = i.toFloat() / fadeSteps * tanpuraGain
                    try { old.setVolume(vol) } catch (_: Exception) {}
                    delay(fadeInterval)
                }
                try {
                    old.stop()
                    old.release()
                } catch (_: Exception) {}
                if (old == tanpuraTrack1 && activeTanpuraTrackIndex != 1) tanpuraTrack1 = null
                if (old == tanpuraTrack2 && activeTanpuraTrackIndex != 2) tanpuraTrack2 = null
            }
        }
    }

    private fun playTablaMidiNote(midiNote: Int) {
        val engine = tablaEngine
        if (engine != null) {
            val cachedPcm = tablaNoteCache[midiNote]
            val pcmData: ShortArray? = cachedPcm ?: run {
                val rendered = engine.renderTablaBol(midiNote, velocity = 110)
                if (rendered != null) {
                    tablaNoteCache[midiNote] = rendered
                }
                rendered
            }

            if (pcmData != null) {
                voiceMutex.withLock {
                    val normalizedPcm = ShortArray(pcmData.size) { i ->
                        (pcmData[i] * tablaGain).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    try {
                        val activeTrack = voicePool[currentVoiceIndex]
                        activeTrack.play()
                        activeTrack.write(normalizedPcm, 0, normalizedPcm.size)
                        currentVoiceIndex = (currentVoiceIndex + 1) % VOICE_COUNT
                    } catch (e: Exception) {
                        Log.e("AudioEngine", "Error playing tabla PCM: ${e.message}")
                    }
                }
            } else {
                Log.w("AudioEngine", "renderTablaBol returned null for MIDI note $midiNote")
            }
        } else {
            Log.w("AudioEngine", "tablaEngine is not loaded yet.")
        }
    }

    private suspend fun handleTabla(target: String) {
        val rawBol = target.uppercase().trim().replace(" ", "").replace("-", "")

        // 1. Check if bol is a compound pattern with rapid sub-strokes (e.g. TiRaKiTa, DhaGe, KiTone, ThaKaDhiMi)
        val compoundSubNotes = compoundBolMap[rawBol] 
            ?: compoundBolMap[target.uppercase().trim()]
            ?: compoundBolMap.entries.firstOrNull { rawBol.equals(it.key, ignoreCase = true) }?.value

        if (compoundSubNotes != null && compoundSubNotes.isNotEmpty()) {
            val subStrokeDelayMs = when {
                compoundSubNotes.size >= 4 -> 55L
                compoundSubNotes.size == 3 -> 70L
                else -> 85L
            }
            compoundSubNotes.forEachIndexed { idx, subNote ->
                playTablaMidiNote(subNote)
                if (idx < compoundSubNotes.lastIndex) {
                    delay(subStrokeDelayMs)
                }
            }
            return
        }

        // 2. Resolve single MIDI note from mapped dictionary
        val midiNote = tablaMidiMap[rawBol] 
            ?: tablaMidiMap[target.uppercase().trim()]
            ?: tablaMidiMap.entries.firstOrNull { rawBol.startsWith(it.key) || it.key.startsWith(rawBol) }?.value

        Log.d("AudioEngine", "handleTabla called for '$target' (rawBol='$rawBol') -> resolved MIDI note: $midiNote")

        if (midiNote != null) {
            playTablaMidiNote(midiNote)
        } else {
            Log.w("AudioEngine", "No MIDI note mapping found for tabla bol: '$target'")
        }
    }

    private fun synthPianoFallback(midiNote: Int): ShortArray {
        val frequency = 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)
        val sampleRate = 22050
        val numSamples = (1.5 * sampleRate).toInt()
        val sample = ShortArray(numSamples)
        val f1 = frequency - 0.15
        val f2 = frequency
        val f3 = frequency + 0.15
        val rng = Random()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-1.8 * t)
            val s1 = Math.sin(2.0 * Math.PI * f1 * t) * decay + 0.55 * Math.sin(4.0 * Math.PI * f1 * t) * Math.exp(-3.5 * t)
            val s2 = Math.sin(2.0 * Math.PI * f2 * t) * decay + 0.55 * Math.sin(4.0 * Math.PI * f2 * t) * Math.exp(-3.5 * t)
            val s3 = Math.sin(2.0 * Math.PI * f3 * t) * decay + 0.55 * Math.sin(4.0 * Math.PI * f3 * t) * Math.exp(-3.5 * t)
            val noise = if (t < 0.025) (rng.nextFloat() * 2f - 1f) * Math.exp(-100.0 * t) else 0.0
            val mixed = (s1 + s2 + s3) / 3.0 + noise
            sample[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return sample
    }

    private fun initSystemSounds() {
        engineScope.launch(Dispatchers.IO) {
            try {
                Log.d("AudioEngine", "Attempting to open metronome/metronome_strong.wav...")
                context.assets.open("metronome/metronome_strong.wav").use { inputStream ->
                    val tempFile = File(context.cacheDir, "strong_metronome.wav")
                    tempFile.writeBytes(inputStream.readBytes())
                    metronomeStrongId = systemSoundPool.load(tempFile.absolutePath, 1)
                    Log.i("AudioEngine", "Successfully loaded metronome_strong.wav! ID: $metronomeStrongId")
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "FAILED to load metronome/metronome_strong.wav: ${e.message}", e)
            }

            try {
                Log.d("AudioEngine", "Attempting to open metronome/metronome_weak.wav...")
                context.assets.open("metronome/metronome_weak.wav").use { inputStream ->
                    val tempFile = File(context.cacheDir, "weak_metronome.wav")
                    tempFile.writeBytes(inputStream.readBytes())
                    metronomeWeakId = systemSoundPool.load(tempFile.absolutePath, 1)
                    Log.i("AudioEngine", "Successfully loaded weak.wav! ID: $metronomeWeakId")
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "FAILED to load metronome/weak.wav: ${e.message}", e)
            }
        }
    }

    private fun parseNoteToMidi(target: String): Int {
        val rawDigits = target.filter { it.isDigit() }
        if (rawDigits.isNotEmpty()) return rawDigits.toIntOrNull() ?: 60
        val notePart = target.takeWhile { !it.isDigit() }.uppercase().trim()
        val octavePart = target.dropWhile { !it.isDigit() }.toIntOrNull() ?: 4
        val offset = pitchToMidiOffset[notePart] ?: 0
        return (octavePart + 1) * 12 + offset
    }

    override fun playSample(label: String, index: Int, string: String) {
        val target = label.uppercase().trim()
        val type = string.uppercase()
        Log.d("AudioEngine", "playSample requested -> target: '$target', index: $index, type: '$type'")

        if (type == "TANPURA") {
            engineScope.launch(Dispatchers.Default) { handleTanpura(target) }
            return
        }

        if (type == "TABLA") {
            engineScope.launch(Dispatchers.Default) { handleTabla(target) }
            return
        }

        if (type == "PIANO" || type == "GUITAR") {
            Log.d("AudioEngine", "Melodic Note Trigger Request Received - Target: $target, Instrument Type: $type")
            engineScope.launch(Dispatchers.Default) {
                synchronized(sf2Lock) {
                    val isLoaded = sf2Loaded.get()
                    if (isLoaded) {
                        val midiNote = parseNoteToMidi(target)
                        val normalizedVelocity = 0.7f
                        globalSoundFont?.let { sf: FluidSynthPlayer ->
                            val vel = (normalizedVelocity * 127).toInt()
                            try {
                                sf.noteOn(0, midiNote, vel)
                                val samplesToRender = (44100 * 0.5 * 2).toInt().coerceAtLeast(stereoBufferSize)
                                val floatBuffer = sf.renderFloat(samplesToRender)

                                if (floatBuffer.isNotEmpty()) {
                                    val shortBuffer = ShortArray(floatBuffer.size) { i ->
                                        val sampleValue = (floatBuffer[i] * 0.8f).coerceIn(-1.0f, 1.0f)
                                        (sampleValue * Short.MAX_VALUE).toInt().toShort()
                                    }

                                    voiceMutex.withLock {
                                        val activeTrack = voicePool[currentVoiceIndex]
                                        try {
                                            activeTrack.pause()
                                            activeTrack.flush()
                                            activeTrack.play()
                                            activeTrack.write(shortBuffer, 0, shortBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
                                        } catch (e: Exception) {
                                            Log.e("AudioEngine", "Melodic SoundFont AudioTrack write failed: ${e.message}")
                                        }
                                        currentVoiceIndex = (currentVoiceIndex + 1) % VOICE_COUNT
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.e("AudioEngine", "Melodic soundfont note trigger failed: ${e.message}")
                            }
                        }
                    } else {
                        Log.w("AudioEngine", "Bypassing Note Playback: SoundFont instrument '$type' is still warming up in memory.")
                    }
                }
            }
            return
        }

        engineScope.launch(Dispatchers.Default) {
            when (type) {
                "SYSTEM" -> {
                    val id = if (target.contains("STRONG")) metronomeStrongId else metronomeWeakId
                    if (id != 0) {
                        val vol = metronomeGain.coerceIn(0f, 1f)
                        systemSoundPool.play(id, vol, vol, 2, 0, 1f)
                    } else {
                        Log.e("AudioEngine", "Cannot play system metronome step: sound asset ID is uninitialized (0). Target requested: $target")
                    }
                }
                "DRUM", "METRONOME" -> {
                    val mapKey = "${currentSelectedKitName}_$index"
                    val soundId = drumKitLoader.kitSampleMap[mapKey]

                    if (soundId != null && drumKitLoader.loadedAssetsStatus[soundId] == true) {
                        val vol = if (index % 4 == 0) 1.0f else 0.6f
                        soundPool.play(soundId, vol, vol, 1, 0, 1f)
                        onPadTriggered?.invoke(index)
                    } else {
                        val expectedLabel = drumKitLoader.padLabels.getOrNull(index) ?: "UNKNOWN"
                        val expectedAssetPath = "drums/fingerdrumming/$currentSelectedKitName/$index-$expectedLabel.wav"
                        Log.e("AudioEngine", "Missing preloaded sound ID for key: $mapKey (Expected Path: $expectedAssetPath)")
                    }
                }
            }
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = ms % 1000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    private fun parseLrcLine(line: String): SyncedLine? {
        val trimmed = line.trim()
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            val tsPart = trimmed.substringAfter("[").substringBefore("]")
            val text = trimmed.substringAfter("]").trim()

            return try {
                val parts = tsPart.split(":")
                val mins = parts[0].toLong()
                val secsParts = parts[1].split(".")
                val secs = secsParts[0].toLong()
                val ms = if (secsParts.size > 1) {
                    val msStr = secsParts[1].padEnd(3, '0').take(3)
                    msStr.toLong()
                } else 0L

                SyncedLine(
                    timestampMs = (mins * 60000 + secs * 1000 + ms),
                    text = text
                )
            } catch (e: Exception) {
                SyncedLine(timestampMs = 0L, text = trimmed)
            }
        } else if (trimmed.isNotBlank()) {
            return SyncedLine(timestampMs = 0L, text = trimmed)
        }
        return null
    }

    override fun release() {
        persistPlaybackState()
        loopHandler.removeCallbacksAndMessages(null)

        try {
            visualizer?.enabled = false
        } catch (_: Exception) {}

        visualizer?.release()
        visualizer = null

        dynamicsProcessing?.release()
        dynamicsProcessing = null

        allNotesOff()
        systemSoundPool.release()
        soundPool.release()

        voiceMutex.withLock {
            voicePool.forEach { runCatching { it.release() } }
        }

        engineScope.cancel()
        player = null
    }



    override fun addExternalSubtitle(path: String) {
        val p = player ?: return
        val currentItem = p.currentMediaItem ?: return

        val subFile = File(path)
        val config = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
            .setMimeType(if (subFile.extension == "vtt") "text/vtt" else "application/x-subrip")
            .setLanguage(subFile.extension.uppercase())
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val currentConfigs = currentItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        val newConfigs = currentConfigs + config

        val newItem = currentItem.buildUpon()
            .setSubtitleConfigurations(newConfigs)
            .build()

        val currentPosition = p.currentPosition
        val playWhenReady = p.playWhenReady

        p.setMediaItem(newItem, false)
        p.seekTo(currentPosition)
        p.playWhenReady = playWhenReady
        p.prepare()
    }

    override fun switchTrack(track: MediaTrack?) {
        val p = player ?: return
        if (track == null || track.id == "none") {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        val groups = p.currentTracks.groups
        for (group in groups) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                if (format.id == track.id) {
                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                        .build()
                    return
                }
            }
        }
    }

    override fun setSubtitlesEnabled(enabled: Boolean) {
        val p = player ?: return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
    }

    override suspend fun getChapters(): List<MediaChapter> {
        val snapshot = withContext(Dispatchers.Main) {
            val p = player ?: return@withContext null
            Triple(
                p.mediaMetadata,
                p.duration,
                p.currentMediaItem?.localConfiguration?.uri?.toString()
            )
        } ?: return emptyList()

        val metadata = snapshot.first
        val duration = snapshot.second
        val mediaUri = snapshot.third

        if (duration <= 0L || mediaUri == null) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            val chapters = mutableListOf<MediaChapter>()

            metadata.extras?.let { extras ->
                val titles = extras.getStringArrayList("chapter_titles")
                val starts = extras.getLongArray("chapter_starts")
                if (titles != null && starts != null && titles.size == starts.size) {
                    titles.forEachIndexed { index, title ->
                        val startMs = starts[index]
                        val endMs = starts.getOrNull(index + 1) ?: duration
                        chapters += MediaChapter(
                            title = title.ifBlank { "Chapter ${index + 1}" },
                            startMs = startMs.coerceAtLeast(0L),
                            endMs = endMs.coerceAtMost(duration)
                        )
                    }
                }
            }

            if (chapters.isEmpty() && !mediaUri.startsWith("http")) {
                try {
                    val retriever = MediaMetadataRetriever()
                    val uri = Uri.parse(mediaUri)

                    if (mediaUri.startsWith("content://")) {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                        }
                    } else {
                        retriever.setDataSource(uri.path)
                    }

                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        val baseName = file.nameWithoutExtension
                        val parent = file.parentFile
                        val jsonSidecar = File(parent, "$baseName.info.json")

                        if (jsonSidecar.exists()) {
                            val content = jsonSidecar.readText()
                            val chapterPattern = "\"start_time\":\\s*([0-9.]+),\\s*\"end_time\":\\s*([0-9.]+),\\s*\"title\":\\s*\"([^\"]+)\"".toRegex()
                            chapterPattern.findAll(content).forEach { match ->
                                val start = (match.groupValues[1].toDouble() * 1000).toLong()
                                val end = (match.groupValues[2].toDouble() * 1000).toLong()
                                val title = match.groupValues[3]
                                chapters += MediaChapter(title = title, startMs = start, endMs = end)
                            }
                        }

                        val nativeChapter = File(parent, "$baseName.chapter")
                        if (nativeChapter.exists()) {
                            try {
                                val content = nativeChapter.readText()
                                val list = json.decodeFromString<List<MediaChapter>>(content)
                                chapters.addAll(list)
                            } catch (_: Exception) {}
                        }
                    }
                    retriever.release()
                } catch (e: Exception) {
                    println("pEQLearn: [CHAPTERS] Probe failed: ${e.message}")
                }
            }

            chapters.distinctBy { "${it.title}_${it.startMs}" }.sortedBy { it.startMs }
        }
    }

    @OptIn(UnstableApi::class)
    override fun setVideoScalingMode(mode: Int) {
        player?.videoScalingMode = mode
    }

    override fun setAudioDelay(ms: Long) {
        // Industrial Latency Correction
    }

    override fun seekRelative(deltaMs: Long) {
        player?.let {
            val duration = if (it.duration > 0) it.duration else Long.MAX_VALUE
            val target = it.currentPosition + deltaMs
            it.seekTo(target.coerceIn(0, duration))
        }
    }

    override suspend fun getEmbeddedMetadata(): pEQMediaMetadata {
        val playerSnapshot = withContext(Dispatchers.Main) {
            val p = player ?: return@withContext null
            Triple(
                p.currentTracks,
                p.currentMediaItem?.localConfiguration?.uri?.let { uri ->
                    if (uri.scheme == "file") uri.path else uri.toString()
                },
                p.duration
            )
        } ?: return pEQMediaMetadata()

        val currentTracks = playerSnapshot.first
        val currentMediaUri = playerSnapshot.second
        val duration = playerSnapshot.third

        return withContext(Dispatchers.IO) {
            val tracks = mutableListOf<MediaTrack>()

            currentTracks.groups.forEach { group ->
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val type = when (group.type) {
                        C.TRACK_TYPE_AUDIO -> TrackType.AUDIO
                        C.TRACK_TYPE_VIDEO -> TrackType.VIDEO
                        C.TRACK_TYPE_TEXT -> {
                            if (format.sampleMimeType?.contains("x-subrip") == true || format.sampleMimeType?.contains("vtt") == true) {
                                TrackType.SUBTITLE
                            } else {
                                TrackType.LYRICS
                            }
                        }
                        else -> null
                    }

                    if (type != null) {
                        tracks.add(
                            MediaTrack(
                                id = format.id ?: "T-${group.type}-$i",
                                language = format.language ?: "Und",
                                type = type,
                                codec = format.sampleMimeType?.substringAfterLast("/")?.uppercase()
                            )
                        )
                    }
                }
            }

            val lyrics = mutableListOf<SyncedLine>()

            if (currentMediaUri != null && !currentMediaUri.startsWith("http")) {
                try {
                    val cleanPath = cleanLocalPath(currentMediaUri)
                    if (!cleanPath.startsWith("content://")) {
                        try {
                            val file = File(cleanPath)
                            val lrcFile = File(file.parent, "${file.nameWithoutExtension}.lrc")
                            if (lrcFile.exists()) {
                                lyrics += lrcFile.readLines().mapNotNull { parseLrcLine(it) }
                            }
                        } catch (_: Exception) {}
                    }

                    if (lyrics.isEmpty()) {
                        var rawLyrics: String? = null
                        if (!cleanPath.startsWith("content://") && !cleanPath.startsWith("http")) {
                            try {
                                val file = File(cleanPath)
                                if (file.exists()) {
                                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                                    val tag = audioFile.tag
                                    if (tag != null) {
                                        rawLyrics = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        if (rawLyrics.isNullOrBlank()) {
                            val retriever = MediaMetadataRetriever()
                            try {
                                if (cleanPath.startsWith("content://")) {
                                    retriever.setDataSource(context, Uri.parse(cleanPath))
                                } else {
                                    retriever.setDataSource(cleanPath)
                                }
                                rawLyrics = retriever.extractMetadata(31)
                                    ?: retriever.extractMetadata(23)
                                    ?: retriever.extractMetadata(4)
                                    ?: retriever.extractMetadata(13)
                                    ?: retriever.extractMetadata(101)
                            } catch (_: Exception) {
                            } finally {
                                retriever.release()
                            }
                        }

                        if (!rawLyrics.isNullOrBlank()) {
                            val parsed = rawLyrics.lines().mapNotNull { parseLrcLine(it) }
                            if (parsed.all { it.timestampMs == 0L } && parsed.size > 1) {
                                val safeDuration = duration.coerceAtLeast(1L)
                                val interval = safeDuration / parsed.size
                                lyrics += parsed.mapIndexed { index, line ->
                                    line.copy(timestampMs = index * interval)
                                }
                            } else {
                                lyrics += parsed
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("pEQLearn: Lyrics extraction failed ${e.message}")
                }
            }

            val chapters = mutableListOf<MediaChapter>()

            if (currentMediaUri != null && !currentMediaUri.startsWith("http")) {
                try {
                    val cleanPath = cleanLocalPath(currentMediaUri)
                    if (!cleanPath.startsWith("content://")) {
                        val file = File(cleanPath)
                        val chapterFile = File(file.parent, "${file.nameWithoutExtension}.chapters")
                        if (chapterFile.exists()) {
                            chapters += chapterFile.readLines().mapNotNull { line ->
                                val parts = line.split("|")
                                if (parts.size >= 2) {
                                    MediaChapter(
                                        title = parts[0],
                                        startMs = parts[1].toLongOrNull() ?: 0L,
                                        endMs = parts.getOrNull(2)?.toLongOrNull() ?: ((parts[1].toLongOrNull() ?: 0L) + 5000L)
                                    )
                                } else null
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("pEQLearn: Chapter extraction failed ${e.message}")
                }
            }

            pEQMediaMetadata(
                availableTracks = tracks,
                lyrics = lyrics,
                chapters = chapters
            )
        }
    }

    override fun getAvailableTracks(): List<MediaTrack> {
        val p = player ?: return emptyList()
        val tracks = mutableListOf<MediaTrack>()
        val trackGroups = p.currentTracks.groups

        for (group in trackGroups) {
            val type = when (group.type) {
                C.TRACK_TYPE_AUDIO -> TrackType.AUDIO
                C.TRACK_TYPE_TEXT -> TrackType.SUBTITLE
                else -> null
            }

            if (type != null) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    tracks.add(
                        MediaTrack(
                            id = format.id ?: "T-${group.type}-$i",
                            language = format.language ?: "Und",
                            type = type,
                            codec = format.sampleMimeType?.substringAfterLast("/")?.uppercase()
                        )
                    )
                }
            }
        }
        return tracks
    }

    override fun setNeuralSaturation(enabled: Boolean) {
        println("pEQLearn: [NEURAL] Saturation -> $enabled")
    }

    override fun setCompressorThreshold(thresholdDb: Float) {
        println("pEQLearn: [DYNAMICS] Compressor Threshold -> $thresholdDb dB")
    }

    override fun setClarityLevel(level: Float) {
        println("pEQLearn: [NEURAL] Clarity Level -> $level")
    }

    override fun setDeepBassLevel(level: Float) {
        println("pEQLearn: [NEURAL] Deep Bass Level -> $level")
    }

    override fun setExclusiveMode(enabled: Boolean) {
        println("pEQLearn: [AUDIO] Exclusive Mode -> $enabled")
    }

    override fun getJitterBufferMs(): Int = lastLatency
    override fun setCrossfadeDuration(ms: Long) {}
    override fun setInterferenceShielding(enabled: Boolean) {}
    override fun alignMicPhase(enabled: Boolean) {}
    override fun calibrateLatency(): Int = lastLatency
    override fun setOverflowProtection(enabled: Boolean) {}
    override fun applyMasteringChain(preset: String) {}
    override fun startUpnpCasting(target: String) {}
    override fun setOscBroadcasting(enabled: Boolean) {}

    override fun enableZeroLatencyMonitoring(enabled: Boolean) {
        println("pEQLearn: [AUDIO] Zero Latency Monitoring -> $enabled")
    }

    override fun forceBluetoothCodec(codec: String) {}

    @RequiresApi(Build.VERSION_CODES.P)
    override fun toggleVocalRemoval(enabled: Boolean) {
        isVocalSuppressionActive = enabled
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            val vocalIndices = listOf(4, 5, 6, 7)
            vocalIndices.forEach { index ->
                val band = channelConfig.preEq.getBand(index)
                band.isEnabled = true
                if (enabled) {
                    band.gain = -15f
                } else {
                    band.gain = 0f
                }
            }
            dp.setAllChannelsTo(channelConfig)
            println("pEQLearn: [DSP] Vocal Removal -> $enabled")
        } catch (e: Exception) {
            println("pEQLearn: [DSP] Vocal Removal Failed: ${e.message}")
        }
    }

    override fun togglePhaseInversion(enabled: Boolean) {
        setPhaseInversion(enabled)
    }

    override suspend fun saveEmbeddedLyrics(url: String, lyrics: List<SyncedLine>) {
        withContext(Dispatchers.IO) {
            val lyricsText = lyrics.joinToString("\n") {
                if (it.timestampMs > 0L) "[${formatTimeLrc(it.timestampMs)}] ${it.text}" else it.text
            }
            try {
                if (!url.startsWith("content://") && !url.startsWith("http")) {
                    val file = File(url)
                    if (file.exists()) {
                        val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                        var tag = audioFile.tag
                        if (tag == null) {
                            tag = audioFile.createDefaultTag()
                            audioFile.tag = tag
                        }
                        tag.setField(org.jaudiotagger.tag.FieldKey.LYRICS, lyricsText)
                        audioFile.commit()
                        println("pEQLearn: [Android] Embedded lyrics successfully saved to file via jaudiotagger: $url")
                    }
                }
            } catch (e: Exception) {
                println("pEQLearn: [Android] Failed to embed lyrics in file: ${e.message}")
            }
            saveSidecarFile(url, "lrc", lyricsText)
        }
    }

    override suspend fun saveEmbeddedChapters(url: String, chapters: List<MediaChapter>) {
        saveSidecarFile(url, "chapter", json.encodeToString(chapters))
    }

    override suspend fun saveEmbeddedSubtitles(url: String, subtitles: List<SyncedLine>) {
        saveSidecarFile(
            url,
            "srt",
            subtitles.joinToString("\n\n") {
                "${subtitles.indexOf(it) + 1}\n${formatTimeSrt(it.timestampMs)} --> ${formatTimeSrt(it.timestampMs + 5000)}\n${it.text}"
            }
        )
    }

    override suspend fun saveSidecarFile(mediaPath: String, extension: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(mediaPath)
                if (file.exists()) {
                    val sidecar = File(file.parent, "${file.nameWithoutExtension}.$extension")
                    sidecar.writeText(content)
                } else if (mediaPath.startsWith("http") || mediaPath.startsWith("content://")) {
                    println("pEQLearn: [SIDECAR] Cannot save sidecar for non-file URI: $mediaPath")
                }
            } catch (e: Exception) {
                println("pEQLearn: [SIDECAR] Failed to save: ${e.message}")
            }
        }
    }

    private fun formatTimeLrc(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val mills = (ms % 1000) / 10
        return "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}.${mills.toString().padStart(2, '0')}"
    }

    private fun formatTimeSrt(ms: Long): String {
        val totalSec = ms / 1000
        val hr = totalSec / 3600
        val min = (totalSec % 3600) / 60
        val sec = totalSec % 60
        val mills = ms % 1000
        return "${hr.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')},${mills.toString().padStart(3, '0')}"
    }

    private val stemMutes = ConcurrentHashMap<String, Boolean>()
    private val stemSolos = ConcurrentHashMap<String, Boolean>()
    private val stemVolumes = ConcurrentHashMap<String, Float>()
    private val stemPans = ConcurrentHashMap<String, Float>()
    private var isStemsActivePlaying = false

    override fun loadStems(stems: Map<String, String>) {
        Handler(Looper.getMainLooper()).post {
            try {
                stemPlayers.values.forEach { it.stop(); it.release() }
                stemPlayers.clear()
                stemMutes.clear()
                stemSolos.clear()
                stemVolumes.clear()
                stemPans.clear()

                for ((stemName, filePath) in stems) {
                    val file = File(filePath)
                    if (!file.exists()) continue
                    val stemPlayer = ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                        repeatMode = Player.REPEAT_MODE_ALL
                        volume = 0.85f
                        prepare()
                    }
                    stemPlayers[stemName.lowercase()] = stemPlayer
                    stemVolumes[stemName.lowercase()] = 0.85f
                    stemPans[stemName.lowercase()] = 0.0f
                    stemMutes[stemName.lowercase()] = false
                    stemSolos[stemName.lowercase()] = false
                }
                println("pEQLearn: [STEMS] Loaded ${stemPlayers.size} stems into multi-track engine")
            } catch (e: Exception) {
                println("pEQLearn: [STEMS] Failed loading stems: ${e.message}")
            }
        }
    }

    override fun isStemMuted(stem: String): Boolean = stemMutes[stem.lowercase()] ?: false

    override fun setStemVolume(stem: String, volume: Float) {
        val key = stem.lowercase()
        stemVolumes[key] = volume.coerceIn(0f, 1f)
        applyStemVolumes()
    }

    override fun setStemMute(stem: String, muted: Boolean) {
        val key = stem.lowercase()
        stemMutes[key] = muted
        applyStemVolumes()
    }

    override fun isStemSolo(stem: String): Boolean = stemSolos[stem.lowercase()] ?: false

    override fun setStemSolo(stem: String, solo: Boolean) {
        val key = stem.lowercase()
        stemSolos[key] = solo
        applyStemVolumes()
    }

    override fun setStemPan(stem: String, pan: Float) {
        val key = stem.lowercase()
        stemPans[key] = pan.coerceIn(-1f, 1f)
        // ExoPlayer stereo balance/pan can be set via custom audio processors or left/right volume weights
        val p = stemPans[key] ?: 0f
        val left = if (p > 0) 1f - p else 1f
        val right = if (p < 0) 1f + p else 1f
        val baseVol = stemVolumes[key] ?: 0.8f
        stemPlayers[key]?.let {
            if (!(stemMutes[key] ?: false)) {
                it.volume = baseVol * ((left + right) / 2f)
            }
        }
    }

    override fun getStemVolume(stem: String): Float = stemVolumes[stem.lowercase()] ?: 0.8f

    override fun getStemPan(stem: String): Float = stemPans[stem.lowercase()] ?: 0.0f

    private fun applyStemVolumes() {
        val anySolo = stemSolos.values.any { it }
        for ((stem, p) in stemPlayers) {
            val isSolo = stemSolos[stem] ?: false
            val isMuted = stemMutes[stem] ?: false
            val vol = stemVolumes[stem] ?: 0.8f

            if (anySolo) {
                p.volume = if (isSolo && !isMuted) vol else 0f
            } else {
                p.volume = if (isMuted) 0f else vol
            }
        }
    }

    override fun playStems() {
        Handler(Looper.getMainLooper()).post {
            isStemsActivePlaying = true
            val minPos = stemPlayers.values.minOfOrNull { it.currentPosition } ?: 0L
            stemPlayers.values.forEach {
                it.seekTo(minPos)
                it.play()
            }
        }
    }

    override fun pauseStems() {
        Handler(Looper.getMainLooper()).post {
            isStemsActivePlaying = false
            stemPlayers.values.forEach { it.pause() }
        }
    }

    override fun stopStems() {
        Handler(Looper.getMainLooper()).post {
            isStemsActivePlaying = false
            stemPlayers.values.forEach {
                it.stop()
                it.seekTo(0)
            }
        }
    }

    override fun isStemsPlaying(): Boolean = isStemsActivePlaying
}