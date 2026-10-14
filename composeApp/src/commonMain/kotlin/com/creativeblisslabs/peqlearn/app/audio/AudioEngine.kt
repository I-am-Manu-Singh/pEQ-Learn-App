package com.creativeblisslabs.peqlearn.app.audio

/**
 * INDUSTRIAL AUDIO INTERFACE: pEQ CORE
 * The primary contract for low-latency DSP and cinematic transport.
 */
interface AudioEngine {
    var currentSelectedKitName: String
    fun initialize(audioSessionId: Int)
    fun playTrack(url: String, title: String? = null, artist: String? = null, thumbnail: String? = null)
    fun setNeuralSaturation(enabled: Boolean)
    fun loadTrack(url: String, title: String? = null, artist: String? = null, thumbnail: String? = null)
    fun setVideoSurface(surface: Any?)
    fun setVideoScalingMode(mode: Int)
    fun setAudioDelay(ms: Long)
    var onFftDataReceived: ((FloatArray) -> Unit)?
    var onTrackEnded: (() -> Unit)?
    var onTrackChanged: (() -> Unit)?
    var onBufferingChanged: ((Boolean) -> Unit)?
    var onPadTriggered: ((Int) -> Unit)?
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekRelative(deltaMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun setRepeatMode(mode: com.creativeblisslabs.peqlearn.app.data.RepeatMode)
    fun setShuffleMode(enabled: Boolean)
    fun setLoopPoints(a: Long?, b: Long?)
    suspend fun getEmbeddedMetadata(): com.creativeblisslabs.peqlearn.app.data.pEQMediaMetadata
    suspend fun getChapters(): List<com.creativeblisslabs.peqlearn.app.data.MediaChapter>
    fun getAvailableTracks(): List<com.creativeblisslabs.peqlearn.app.data.MediaTrack>
    fun switchTrack(track: com.creativeblisslabs.peqlearn.app.data.MediaTrack?)
    fun updateEqBand(bandIndex: Int, gainDb: Float)
    fun updateEqQ(bandIndex: Int, q: Float)
    fun setCompressorThreshold(thresholdDb: Float)
    fun updateCompressor(threshold: Float, ratio: Float, attack: Float, release: Float)
    fun setLimiterEnabled(enabled: Boolean)
    fun setClarityLevel(level: Float)
    fun setDeepBassLevel(level: Float)
    fun getDuration(): Long
    fun getFftData(): FloatArray?
    fun getCurrentPosition(): Long
    fun playMidiNote(midiNote: Int)
    fun getCurrentTrackUri(): String?
    fun setExclusiveMode(enabled: Boolean)
    fun getDecoderInfo(): String // Returns "HW", "SW", or "Unknown"
    
    // Performance Telemetry (P-09)
    fun getProcessingLatencyMs(): Int
    fun getAudioDropouts(): Int
    fun getJitterBufferMs(): Int
    
    fun setCrossfadeDuration(ms: Long)
    fun setInterferenceShielding(enabled: Boolean)
    fun alignMicPhase(enabled: Boolean)
    
    fun calibrateLatency(): Int
    fun setOverflowProtection(enabled: Boolean)
    
    fun applyMasteringChain(preset: String)
    fun startUpnpCasting(target: String)
    fun setOscBroadcasting(enabled: Boolean)
    fun enableZeroLatencyMonitoring(enabled: Boolean)
    
    fun getCurrentSpectrum(): FloatArray
    fun getPhaseCorrelation(): Float
    
    fun forceBluetoothCodec(codec: String)
    fun togglePhaseInversion(enabled: Boolean)
    fun setPhaseInversion(enabled: Boolean)
    fun setVolume(volume: Float)
    fun setChannelBalance(balance: Float)
    fun sendMidiController(controller: Int, value: Int)
    fun toggleVocalRemoval(enabled: Boolean)
    fun setSubHarmonicGenerator(enabled: Boolean)
    fun isPlaying(): Boolean
    fun setOutputDevice(deviceId: Int?) // New: Routing Control
    fun getBitrate(): Int
    fun getSampleRate(): Int
    fun getBitDepth(): Int
    fun getCodec(): String
    fun getVideoWidth(): Int
    fun getVideoHeight(): Int
    suspend fun saveEmbeddedLyrics(url: String, lyrics: List<com.creativeblisslabs.peqlearn.app.data.SyncedLine>)
    suspend fun saveEmbeddedChapters(url: String, chapters: List<com.creativeblisslabs.peqlearn.app.data.MediaChapter>)
    suspend fun saveEmbeddedSubtitles(url: String, subtitles: List<com.creativeblisslabs.peqlearn.app.data.SyncedLine>)
    suspend fun saveSidecarFile(mediaPath: String, extension: String, content: String)

    fun addExternalSubtitle(path: String)
    fun getConnectedDevices(): List<com.creativeblisslabs.peqlearn.app.data.Device> = emptyList()
    fun setSubtitlesEnabled(enabled: Boolean)

    fun togglePlayback() {}

    fun nextTrack() {}

    fun previousTrack() {}

    fun getDspLoad(): Float = 0f
    fun isInitialized(): Boolean = true
    fun isStemMuted(stem: String): Boolean = false
    fun setStemVolume(stem: String, volume: Float) {}
    fun setStemMute(stem: String, muted: Boolean) {}
    fun isStemSolo(stem: String): Boolean = false
    fun setStemSolo(stem: String, solo: Boolean) {}
    fun setStemPan(stem: String, pan: Float) {}

    fun noteOn(midiNote: Int, velocity: Float = 0.8f) {}
    fun noteOff(midiNote: Int) {}
    fun allNotesOff() {}
    fun setInstrument(instrument: String) {}
    fun listAssetSoundfonts(category: String): List<String> = emptyList()
    fun getLoadedPresetNames(instrumentKey: String): List<String> = emptyList()
    fun selectPresetByIndex(instrumentKey: String, index: Int): Pair<Int, Int>? = null
    fun updateSoundFontPreferences(
        pianoSf2: String, pianoBank: Int, pianoPreset: Int, pianoChannel: Int, pianoVelocity: Int,
        guitarSf2: String, guitarBank: Int, guitarPreset: Int, guitarChannel: Int, guitarVelocity: Int,
        tanpuraSf2: String, tanpuraBank: Int, tanpuraPreset: Int, tanpuraChannel: Int, tanpuraVelocity: Int,
        drumSf2: String, drumBank: Int, drumPreset: Int, drumChannel: Int, drumVelocity: Int,
        metronomeSf2: String, metronomeBank: Int, metronomePreset: Int, metronomeChannel: Int, metronomeVelocity: Int,
        tablaSf2: String, tablaBank: Int, tablaPreset: Int, tablaChannel: Int, tablaVelocity: Int,
        useSf2ForDrums: Boolean, useSf2ForMetronome: Boolean, useSf2ForTanpura: Boolean
    ) {}
    fun playSample(label: String, index: Int = -1, string: String = "PIANO") {}
    fun playTabla(bol: String, velocity: Int = 100) {}
    fun loadCustomSoundFont(category: String, path: String): Boolean = false
    fun setTanpuraGain(gain: Float) {}
    fun setTablaGain(gain: Float) {}
    fun getLatency(): Int = 0
    fun getRegistry(): Map<String, String> = emptyMap()
    fun release()
    suspend fun probeMedia(url: String): Map<String, List<String>>
}

expect fun getAudioEngine(): AudioEngine
