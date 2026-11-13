package com.creativeblisslabs.peqlearn.app.data

import kotlin.collections.emptyMap
import kotlin.collections.emptyList
import kotlin.random.Random

import kotlinx.serialization.Serializable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Serializable
enum class MediaType { AUDIO, VIDEO }

@Serializable
enum class ChordInstrumentType(
    val label: String,
    val title: String,
    val soundEngineName: String,
    val assetFolder: String,
    val defaultSf2Path: String? = null
) {
    GUITAR_ACOUSTIC("🎸", "Acoustic Guitar", "GUITAR_ACOUSTIC", "guitar/guitar-acoustic", null),
    GUITAR_ELECTRIC("⚡", "Electric Guitar", "GUITAR_ELECTRIC", "guitar/guitar-electric", null),
    GUITAR_CLASSICAL("🎻", "Classical Guitar", "GUITAR_CLASSICAL", "guitar/guitar-classical", null),
    GUITAR_7("🎸", "7-String Guitar", "GUITAR_ELECTRIC", "guitar/guitar-electric", null),
    UKULELE("🪕", "Ukulele", "UKULELE", "guitar/guitar-acoustic", null),
    HARMONIUM("🪗", "Harmonium", "HARMONIUM", "harmonium", null),
    DRUM_KIT("🥁", "Drum Kit", "DRUM", "drums", null),
    TABLA("🪘", "Tabla", "TABLA", "tabla", null),
    SITAR("🪕", "Sitar", "SITAR", "sitar", null),
    PIANO_ACOUSTIC("🎹", "Acoustic Piano", "PIANO_ACOUSTIC", "piano/acoustic", null),
    PIANO_ELECTRIC("🎹", "Electric Piano", "PIANO_ELECTRIC", "piano/electric", null),
    FLUTE("🪈", "Flute", "FLUTE", "flute", null),
    VIOLIN("🎻", "Violin", "VIOLIN", "violin", null);

    companion object {
        fun fromNameOrNull(name: String?): ChordInstrumentType? {
            if (name == null) return null
            val upper = name.uppercase().trim()
            return entries.firstOrNull { it.name == upper || it.soundEngineName == upper || upper.contains(it.name) }
        }
    }
}

@Serializable
enum class RepeatMode { OFF, ALL, ONE, FOLDER }

data class AudioFile(
    val id: String,
    val name: String,
    val title: String? = null,
    val path: String,
    val duration: Long,
    val sizeBytes: Long,
    val bitDepth: Int = 16,
    val sampleRate: Int = 44100,
    val format: String = "MP3",
    val isHiRes: Boolean = false,
    val isLossless: Boolean = false,
    val isVideo: Boolean = false,
    val hasLyrics: Boolean = false,
    val artist: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val genre: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val bitrate: Int = 0,
    val channels: Int = 2,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val thumbnail: String? = null,
    val lyrics: String? = null
)
data class VideoFile(
    val id: String,
    val name: String,
    val title: String? = null,
    val path: String,
    val duration: Long,
    val sizeBytes: Long,
    val resolution: String = "1080p",
    val codec: String = "AVC",
    val fps: Int = 0,
    val bitrate: Int = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val hasSubtitles: Boolean = false,
    val hasMultiAudio: Boolean = false,
    val hasChapters: Boolean = false,
    val thumbnail: String? = null
)
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
    val useOsmdRenderer: Boolean = false,
    val showWaterfall: Boolean = true,
    val particleCount: Int = 16,
    val particleSpeed: Float = 4.5f,
    val particleScale: Float = 1.0f,
    val particleExpansion: Float = 1.0f,
    val lockParticleColor: Boolean = true,
    val flashSize: Float = 1.0f,
    val flashHaloIntensity: Float = 0.65f,
    val noteCornerRadius: Float = 6.0f,
    val noteEdgeThickness: Float = 1.2f,
    val noteStyleOval: Boolean = false,
    val keyboardHighlightActive: Boolean = true,
    val keyboardShowChords: Boolean = true,
    val keyboardMinorEdges: Boolean = true,
    val keyboardShowVelvetCloth: Boolean = true,
    val keyboardShowSideFrames: Boolean = true,
    val showPedals: Boolean = true,
    val pedalScaling: Float = 1.0f,
    val pedalOpacity: Float = 0.95f
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



// ==========================================
// MEDIA METADATA & PRESETS (Consolidated)
// ==========================================

@Serializable
data class MediaChapter(
    val id: Long = Random.nextLong(),
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val thumbnail: String? = null
)

@Serializable
data class SyncedLine(
    val id: Long = Random.nextLong(),
    val timestampMs: Long,
    val text: String,
    val translation: String? = null,
    val content: String = text
)

@Serializable
data class MediaTrack(
    val id: String,
    val language: String,
    val type: TrackType,
    val codec: String? = null,
    val isHardwareDecoded: Boolean = true,
    val isExternal: Boolean = false,
    val externalUrl: String? = null
)

enum class TrackType {
    AUDIO, SUBTITLE, VIDEO, LYRICS
}

@Serializable
data class pEQMediaMetadata(
    val chapters: List<MediaChapter> = emptyList(),
    val lyrics: List<SyncedLine> = emptyList(),
    val activeAudioTrack: MediaTrack? = null,
    val activeSubtitleTrack: MediaTrack? = null,
    val availableTracks: List<MediaTrack> = emptyList()
)

data class EQBand(
    val frequency: Float,
    val gain: Float,
    val qFactor: Float,
    val type: BandType = BandType.PEAKING
)

enum class BandType {
    LOW_SHELF,
    PEAKING,
    HIGH_SHELF,
    NOTCH,
    BANDPASS
}

data class Preset(val name: String, val bands: List<EQBand>)

object Presets {
    val Flat = Preset("Flat", emptyList())

    val BassBoost = Preset(
        "Bass Boost",
        listOf(
            EQBand(60f, 6f, 0.7f, BandType.LOW_SHELF),
            EQBand(150f, 2f, 1.0f, BandType.PEAKING)
        )
    )

    val Bollywood = Preset(
        "Trending",
        listOf(
            EQBand(100f, 4f, 0.8f, BandType.PEAKING),
            EQBand(3000f, 2f, 1.2f, BandType.PEAKING),
            EQBand(8000f, 3f, 1.0f, BandType.HIGH_SHELF)
        )
    )

    val Pop = Preset(
        "Pop",
        listOf(
            EQBand(100f, 2f, 0.7f, BandType.LOW_SHELF),
            EQBand(1000f, -1f, 1.0f, BandType.PEAKING),
            EQBand(10000f, 2f, 0.7f, BandType.HIGH_SHELF)
        )
    )

    val ReferenceStudio = Preset(
        "Reference",
        listOf(
            EQBand(30f, 3.0f, 0.7f, BandType.LOW_SHELF),
            EQBand(200f, -1.5f, 1.0f, BandType.PEAKING),
            EQBand(3000f, 1.0f, 1.5f, BandType.PEAKING),
            EQBand(4000f, -2.5f, 2.0f, BandType.PEAKING),
            EQBand(8000f, 1.5f, 1.5f, BandType.PEAKING),
            EQBand(12000f, 3.5f, 0.7f, BandType.HIGH_SHELF)
        )
    )

    val WirelessEarbuds = Preset(
        "TWS Earbuds",
        listOf(
            EQBand(60f, 5.0f, 0.8f, BandType.LOW_SHELF),
            EQBand(400f, -2.0f, 1.0f, BandType.PEAKING),
            EQBand(3000f, 3.0f, 1.5f, BandType.PEAKING),
            EQBand(10000f, 4.0f, 0.7f, BandType.HIGH_SHELF)
        )
    )

    val SystemSpeakers = Preset(
        "System Speakers",
        listOf(
            EQBand(100f, 6.0f, 0.7f, BandType.LOW_SHELF),
            EQBand(1000f, -2.0f, 1.0f, BandType.PEAKING),
            EQBand(4000f, 2.0f, 1.2f, BandType.PEAKING),
            EQBand(12000f, 3.0f, 0.7f, BandType.HIGH_SHELF)
        )
    )

    val PodcastDialogue = Preset(
        "Podcast",
        listOf(
            EQBand(100f, -6.0f, 0.7f, BandType.LOW_SHELF),
            EQBand(3000f, 3.5f, 1.2f, BandType.PEAKING),
            EQBand(6000f, 2.0f, 1.5f, BandType.PEAKING),
            EQBand(12000f, 2.0f, 0.7f, BandType.HIGH_SHELF)
        )
    )

    val PhoneSpeakers = Preset(
        "Phone Speakers",
        listOf(
            EQBand(200f, 4.0f, 0.7f, BandType.LOW_SHELF),
            EQBand(4000f, 2.0f, 1.2f, BandType.PEAKING)
        )
    )

    val all = listOf(Flat, BassBoost, Bollywood, Pop, ReferenceStudio, WirelessEarbuds, SystemSpeakers, PhoneSpeakers, PodcastDialogue)
}
