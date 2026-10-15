package com.creativeblisslabs.peqlearn.app.data

import kotlin.collections.emptyMap
import kotlin.collections.emptyList

import kotlinx.serialization.Serializable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Serializable
enum class MediaType { AUDIO, VIDEO }

@Serializable
enum class RepeatMode { OFF, ALL, ONE, FOLDER }

@Serializable
data class pEQMediaItem(
    val id: String,
    val title: String,
    val path: String = "",
    val thumbnail: String? = null,
    val sizeBytes: Long = 0,
    val durationMs: Long = 0,
    val artist: String? = null,
    val isFavorite: Boolean = false,
    val isStream: Boolean = false,
    
    // Auxiliary metadata properties
    val subtitle: String = "",
    val type: MediaType = MediaType.AUDIO,
    val format: String = "",
    val bitDepth: Int = 16,
    val sampleRate: Int = 44100,
    val isHiRes: Boolean = false,
    val isLossless: Boolean = false,
    val resolution: String? = null,  
    val codec: String? = null,      
    val album: String? = null,      
    val albumArtist: String? = null,  
    val composer: String? = null,     
    val genre: String? = null,        
    val trackNumber: Int? = null,     
    val discNumber: Int? = null,      
    val year: Int? = null,           
    val bitrate: Int = 0,            
    val channels: Int = 2,           
    val dateAdded: Long = 0,         
    val lastPlayed: Long = 0,        
    val dateModified: Long = 0,
    val frameRate: Float = 0f,       
    val videoWidth: Int = 0,        
    val videoHeight: Int = 0,       
    val playCount: Int = 0,         
    val lyrics: String? = null
) {
    val fileName: String get() = path.substringAfterLast("/").ifEmpty { title }
    val displayName: String get() = title.ifEmpty { fileName }
    val displaySize: String get() = "${(sizeBytes / 1024).toFloat() / 1024.0f}MB"
}

@Serializable
data class SplitJob(
    val id: String,
    val name: String,
    val progress: Float,
    val status: String,
    val resultPaths: Map<String, String> = emptyMap(),
    val fileName: String = name,
    val etaSeconds: Long = 0
)

@Serializable
data class SessionState(
    val lastTrackPath: String? = null,
    val lastTrackTitle: String? = null,
    val lastTrackArtist: String? = null,
    val lastTrackThumbnail: String? = null,
    val lastPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val queue: List<pEQMediaItem> = emptyList(),
    val favorites: List<String> = emptyList(),
    val accentColor: Int? = null,
    
    // Additional session parameters
    val isRotationLocked: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val eqGains: List<Float> = emptyList(),
    val eqQs: List<Float> = emptyList(),
    val lastIrPath: String? = null,
    val lastSubtitleTrackId: String? = null,
    val lastAudioTrackId: String? = null,
    val volume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val pitchSemiTones: Float = 0.0f,
    val currentTabIndex: Int = 0,
    val isAiVocalRemovalActive: Boolean = false,
    val lastKnownBpm: Float = 120.0f,
    val isLeftHandMuted: Boolean = false,
    val isRightHandMuted: Boolean = false,
    val isLeftHandHidden: Boolean = false,
    val isRightHandHidden: Boolean = false,
    val leftHandColor: Int? = null,
    val rightHandColor: Int? = null,
    val notationTransposition: Int = 0,
    val scanFolders: List<String> = emptyList(),
    val playCounts: Map<String, Int> = emptyMap(),
    val lastPlayed: Map<String, Long> = emptyMap(),
    val notationFormat: String = "Both Viewers",
    val isSplitStaff: Boolean = true,
    val isVerticalScroll: Boolean = false,
    val showPianoKeyLabels: Boolean = true,
    val is3DPerspectiveMode: Boolean = true,
    val horizonScale: Float = 0.68f,
    val showWaterfallParticles: Boolean = true,
    val fallingNotesSpeed: String = "Normal",
    val selectedKeySignature: String = "C Major",
    val selectedTimeSignature: String = "4/4",
    val selectedInstrument: String = "Piano",
    val quantizeDivision: String = "1/16",
    val useOsmdRenderer: Boolean = false
)

@Serializable
data class Device(
    val name: String = "System Speakers",
    val chip: String = "Internal Audio",
    val mode: String = "Standard Mode",
    val isFeatured: Boolean = true,
    val activeProfile: String = "Reference Profile",
    val sampleRate: Int = 44100,
    val isHighRes: Boolean = false,
    val id: String = "",
    val type: String = "Speaker",
    val isDefault: Boolean = true
) {
    val deviceName: String get() = name
    
    val color: Color get() = when(name) {
        "Pro Wired DAC" -> Color(0xFF3B82F6)
        "Wireless DAC Adapter" -> Color(0xFF22D3EE)
        "Universal USB DAC" -> Color(0xFF64748B)
        else -> Color(0xFFF59E0B)
    }
    
    val icon: ImageVector get() = when(name) {
        "Pro Wired DAC" -> Icons.Default.Headset
        "Wireless DAC Adapter" -> Icons.Default.BluetoothAudio
        "Universal USB DAC" -> Icons.Default.Usb
        else -> Icons.Default.Speaker
    }

    companion object {
        val HI_RES_DAC = Device("Pro Wired DAC", "Hi-Res Master", "Bit-Perfect", isHighRes = true, sampleRate = 192000)
        val WIRELESS_ADAPTER = Device("Wireless DAC Adapter", "Hi-Res 990kbps", "Wireless Mode", sampleRate = 96000)
        val GENERIC = Device("Universal USB DAC", "UAC 2.0 Driver", "High-Gain")
        val INTERNAL = Device("System Speakers", "Internal Audio", "Standard Mode")
        val HEADPHONES = Device("Wired Headphones", "Direct 3.5mm", "Low-Impedance")
        val BLUETOOTH = WIRELESS_ADAPTER
        
        fun values() = listOf(INTERNAL, HI_RES_DAC, WIRELESS_ADAPTER, GENERIC)
        val entries = listOf(INTERNAL, HI_RES_DAC, WIRELESS_ADAPTER, GENERIC)
    }
}


@Serializable
data class AutoEqProfile(
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val gains: List<Float>? = null
)

@Serializable
data class YoutubeResult(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val duration: String = "",
    val thumbnailUrl: String = "",
    val url: String = ""
)

@Serializable
data class DownloadJob(
    val id: String = "",
    val url: String = "",
    val title: String = "",
    val progress: Float = 0f,
    val status: String = "QUEUED",
    val type: MediaType = MediaType.AUDIO,
    val filePath: String? = null,
    val etaSeconds: Long = 0L,
    val resolution: String = "1080p",
    val audioQuality: String = "320kbps",
    val formatId: String? = null
)


