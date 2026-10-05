package com.creativeblisslabs.peqlearn.app.data

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
    val Flat = Preset(com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_FLAT, emptyList())

    val BassBoost =
            Preset(
                    com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_BASS_BOOST,
                    listOf(
                            EQBand(60f, 6f, 0.7f, BandType.LOW_SHELF),
                            EQBand(150f, 2f, 1.0f, BandType.PEAKING)
                    )
            )

    val Bollywood =
            Preset(
                    com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.TRENDING_IN_INDIA,
                    listOf(
                            EQBand(100f, 4f, 0.8f, BandType.PEAKING),
                            EQBand(3000f, 2f, 1.2f, BandType.PEAKING),
                            EQBand(8000f, 3f, 1.0f, BandType.HIGH_SHELF)
                    )
            )

    val Pop =
            Preset(
                    com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_POP,
                    listOf(
                            EQBand(100f, 2f, 0.7f, BandType.LOW_SHELF),
                            EQBand(1000f, -1f, 1.0f, BandType.PEAKING),
                            EQBand(10000f, 2f, 0.7f, BandType.HIGH_SHELF)
                    )
            )

    val ReferenceStudio =
            Preset(
                    com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_REFERENCE,
                    listOf(
                            EQBand(30f, 3.0f, 0.7f, BandType.LOW_SHELF),
                            EQBand(200f, -1.5f, 1.0f, BandType.PEAKING),
                            EQBand(3000f, 1.0f, 1.5f, BandType.PEAKING),
                            EQBand(4000f, -2.5f, 2.0f, BandType.PEAKING),
                            EQBand(8000f, 1.5f, 1.5f, BandType.PEAKING),
                            EQBand(12000f, 3.5f, 0.7f, BandType.HIGH_SHELF)
                    )
            )

    val WirelessEarbuds =
            Preset(
                    com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_TWS,
                    listOf(
                            EQBand(60f, 5.0f, 0.8f, BandType.LOW_SHELF), // Punchy bass
                            EQBand(400f, -2.0f, 1.0f, BandType.PEAKING), // Clean up mud
                            EQBand(3000f, 3.0f, 1.5f, BandType.PEAKING), // Vocal presence
                            EQBand(10000f, 4.0f, 0.7f, BandType.HIGH_SHELF) // Sparkle
                    )
            )

    val SystemSpeakers =
            Preset(
                    com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_SYSTEM,
                    listOf(
                            EQBand(100f, 6.0f, 0.7f, BandType.LOW_SHELF),  // Attempt bass
                            EQBand(1000f, -2.0f, 1.0f, BandType.PEAKING), // Reduce tininess
                            EQBand(4000f, 2.0f, 1.2f, BandType.PEAKING),  // Clarity
                            EQBand(12000f, 3.0f, 0.7f, BandType.HIGH_SHELF) // Air
                    )
            )

    val PodcastDialogue =
            Preset(
                    com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_PODCAST,
                    listOf(
                            EQBand(100f, -6.0f, 0.7f, BandType.LOW_SHELF), // Remove rumble
                            // Enhance vocal presence (Speech frequencies)
                            EQBand(3000f, 3.5f, 1.2f, BandType.PEAKING),  
                            // Sibilance control/definition
                            EQBand(6000f, 2.0f, 1.5f, BandType.PEAKING),
                            EQBand(12000f, 2.0f, 0.7f, BandType.HIGH_SHELF) // Air
                    )
            )

    val PhoneSpeakers =
            Preset(
                com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings.PRESET_PHONE,
                listOf(
                    EQBand(200f, 4.0f, 0.7f, BandType.LOW_SHELF),
                    EQBand(4000f, 2.0f, 1.2f, BandType.PEAKING)
                )
            )

    val all = listOf(Flat, BassBoost, Bollywood, Pop, ReferenceStudio, WirelessEarbuds, SystemSpeakers, PhoneSpeakers, PodcastDialogue)
}
