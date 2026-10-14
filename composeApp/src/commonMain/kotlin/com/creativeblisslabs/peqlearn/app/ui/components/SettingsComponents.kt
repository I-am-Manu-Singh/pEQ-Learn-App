package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQTheme
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQAccent
import androidx.compose.ui.graphics.vector.ImageVector
import com.creativeblisslabs.peqlearn.app.Tab

/**
 * 100% RECOVERED ORIGINAL SETTINGS SCREEN COMPONENT
 * Matches Settings_systemtab.jpg, Settings_Instrumenttab.jpg, and Settings_exporttab.jpg
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SYSTEM PREFERENCES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Main Tabs: SYSTEM | INSTRUMENTS | SOUNDFONTS | EXPORT
        val tabs = listOf("SYSTEM", "INSTRUMENTS", "SOUNDFONTS", "EXPORT")
        TabRow(
            selectedTabIndex = viewModel.selectedSettingsTab.intValue,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedSettingsTab.intValue]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = viewModel.selectedSettingsTab.intValue == index,
                    onClick = { viewModel.selectedSettingsTab.intValue = index },
                    text = {
                        Text(
                            title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.selectedSettingsTab.intValue == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Content per tab
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (viewModel.selectedSettingsTab.intValue) {
                0 -> SystemTabContent(viewModel)
                1 -> InstrumentsTabContent(viewModel)
                2 -> SoundFontManagerTabContent(viewModel)
                3 -> ExportTabContent(viewModel)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SystemTabContent(viewModel: AppViewModel) {
    // AESTHETICS & THEME
    SettingsSection("AESTHETICS & THEME", icon = Icons.Default.Palette) {
        Text("STATION THEME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        ThemeSelectionRow(viewModel)
        Spacer(Modifier.height(8.dp))

        Text("INDUSTRIAL ACCENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        AccentSelectionRow(viewModel)
        Spacer(Modifier.height(8.dp))

        TogglePreference(
            label = "Use VexFlow Renderer",
            desc = "Use the high-quality, offline VexFlow engraving engine for sheet music instead of the native canvas renderer.",
            checked = viewModel.useVexflow.value,
            onCheckedChange = { viewModel.useVexflow.value = it }
        )
    }

    // Pitch Detection Algorithm
    SettingsSection("Pitch Detection Algorithm", icon = Icons.Default.GraphicEq) {
        Text("DSP algorithm used for realtime pitch and chord parsing.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Phase Vocoder", "FFT Pitch", "PSOLA").forEach { algo ->
                FilterChip(
                    selected = viewModel.pitchEngineAlgo.value == algo,
                    onClick = { viewModel.pitchEngineAlgo.value = algo },
                    label = { Text(algo, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }

    // AI Stem Quality
    SettingsSection("AI Stem Quality", icon = Icons.Default.AutoAwesome) {
        Text("Model depth and decoupling quality for source separation.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Standard", "Pro Neural").forEach { quality ->
                FilterChip(
                    selected = viewModel.stemSplitQuality.value == quality,
                    onClick = { viewModel.stemSplitQuality.value = quality },
                    label = { Text(quality, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }

    // ARCHIVE MAINTENANCE
    SettingsSection("ARCHIVE MAINTENANCE", icon = Icons.Default.SdStorage) {
        ActionPreference(
            title = "Purge Workstation Cache",
            desc = "Delete temporary data",
            icon = Icons.Default.Refresh
        ) {
            viewModel.clearCache()
        }
    }
}

@Composable
private fun InstrumentsTabContent(viewModel: AppViewModel) {
    Text(
        "SOUNDFONT & INSTRUMENT CUSTOMIZATION",
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
    )

    // 1. Acoustic Piano Customization
    InstrumentCustomizationCard(
        title = "Acoustic Piano Customization",
        assetLabel = "Select Piano SoundFont Asset",
        assetDesc = "Quickly choose from soundfonts bundled in assets.",
        assets = viewModel.listAssetSoundfonts("piano"),
        selectedAsset = viewModel.pianoSf2File.value.substringAfterLast("/"),
        onSelectAsset = {
            viewModel.pianoSf2File.value = "piano/$it"
            viewModel.syncSoundFontPreferences()
        },
        sf2Path = viewModel.pianoSf2File.value,
        onPathChange = { viewModel.pianoSf2File.value = it },
        presets = viewModel.getLoadedPresetNames("PIANO"),
        selectedPreset = "Default Preset",
        onSelectPreset = {
            val p = viewModel.selectPresetByIndex("PIANO", 0)
            if (p != null) {
                viewModel.pianoBank.intValue = p.first
                viewModel.pianoPreset.intValue = p.second
            }
        },
        channel = viewModel.pianoChannel.intValue,
        onChannelChange = { viewModel.pianoChannel.intValue = it },
        velocity = viewModel.pianoVelocity.intValue,
        onVelocityChange = { viewModel.pianoVelocity.intValue = it }
    )

    // 2. Clean Guitar Customization
    InstrumentCustomizationCard(
        title = "Clean Guitar Customization",
        assetLabel = "Select Guitar SoundFont Asset",
        assetDesc = "Quickly choose from guitar soundfonts bundled in assets.",
        assets = viewModel.listAssetSoundfonts("guitar"),
        selectedAsset = viewModel.guitarSf2File.value.substringAfterLast("/"),
        onSelectAsset = {
            viewModel.guitarSf2File.value = "guitar/$it"
            viewModel.syncSoundFontPreferences()
        },
        sf2Path = viewModel.guitarSf2File.value,
        onPathChange = { viewModel.guitarSf2File.value = it },
        presets = viewModel.getLoadedPresetNames("GUITAR"),
        selectedPreset = "Default Preset",
        onSelectPreset = {
            val p = viewModel.selectPresetByIndex("GUITAR", 0)
            if (p != null) {
                viewModel.guitarBank.intValue = p.first
                viewModel.guitarPreset.intValue = p.second
            }
        },
        channel = viewModel.guitarChannel.intValue,
        onChannelChange = { viewModel.guitarChannel.intValue = it },
        velocity = viewModel.guitarVelocity.intValue,
        onVelocityChange = { viewModel.guitarVelocity.intValue = it }
    )

    // 3. Drums Kit Customization
    InstrumentCustomizationCard(
        title = "Drums Kit Customization",
        assetLabel = "Select Drum SoundFont Asset",
        assetDesc = "Quickly choose from drum soundfonts bundled in assets.",
        assets = viewModel.listAssetSoundfonts("drums"),
        selectedAsset = viewModel.drumSf2File.value.substringAfterLast("/"),
        onSelectAsset = {
            viewModel.drumSf2File.value = "drums/$it"
            viewModel.syncSoundFontPreferences()
        },
        sf2Path = viewModel.drumSf2File.value,
        onPathChange = { viewModel.drumSf2File.value = it },
        presets = viewModel.getLoadedPresetNames("DRUMS"),
        selectedPreset = "Default Preset",
        onSelectPreset = {
            val p = viewModel.selectPresetByIndex("DRUMS", 0)
            if (p != null) {
                viewModel.drumBank.intValue = p.first
                viewModel.drumPreset.intValue = p.second
            }
        },
        useSoundFont = viewModel.useSf2ForDrums.value,
        onToggleUseSoundFont = { viewModel.useSf2ForDrums.value = it },
        channel = viewModel.drumChannel.intValue,
        onChannelChange = { viewModel.drumChannel.intValue = it },
        velocity = viewModel.drumVelocity.intValue,
        onVelocityChange = { viewModel.drumVelocity.intValue = it }
    )

    // 4. Metronome sound Customization
    InstrumentCustomizationCard(
        title = "Metronome sound Customization",
        assetLabel = "Select Metronome SoundFont Asset",
        assetDesc = "Quickly choose from metronome soundfonts bundled in assets.",
        assets = viewModel.listAssetSoundfonts("metronome"),
        selectedAsset = viewModel.metronomeSf2File.value.substringAfterLast("/"),
        onSelectAsset = {
            viewModel.metronomeSf2File.value = "metronome/$it"
            viewModel.syncSoundFontPreferences()
        },
        sf2Path = viewModel.metronomeSf2File.value,
        onPathChange = { viewModel.metronomeSf2File.value = it },
        presets = viewModel.getLoadedPresetNames("METRONOME"),
        selectedPreset = "Default Preset",
        onSelectPreset = {
            val p = viewModel.selectPresetByIndex("METRONOME", 0)
            if (p != null) {
                viewModel.metronomeBank.intValue = p.first
                viewModel.metronomePreset.intValue = p.second
            }
        },
        useSoundFont = viewModel.useSf2ForMetronome.value,
        onToggleUseSoundFont = { viewModel.useSf2ForMetronome.value = it },
        channel = viewModel.metronomeChannel.intValue,
        onChannelChange = { viewModel.metronomeChannel.intValue = it },
        velocity = viewModel.metronomeVelocity.intValue,
        onVelocityChange = { viewModel.metronomeVelocity.intValue = it }
    )

    // 5. Tanpura Drone Customization
    InstrumentCustomizationCard(
        title = "Tanpura Drone Customization",
        assetLabel = "Select Tanpura SoundFont Asset",
        assetDesc = "Quickly choose from tanpura soundfonts bundled in assets.",
        assets = viewModel.listAssetSoundfonts("tanpura"),
        selectedAsset = viewModel.tanpuraSf2File.value.substringAfterLast("/"),
        onSelectAsset = {
            viewModel.tanpuraSf2File.value = "tanpura/$it"
            viewModel.syncSoundFontPreferences()
        },
        sf2Path = viewModel.tanpuraSf2File.value,
        onPathChange = { viewModel.tanpuraSf2File.value = it },
        presets = viewModel.getLoadedPresetNames("TANPURA"),
        selectedPreset = "Default Preset",
        onSelectPreset = {
            val p = viewModel.selectPresetByIndex("TANPURA", 0)
            if (p != null) {
                viewModel.tanpuraBank.intValue = p.first
                viewModel.tanpuraPreset.intValue = p.second
            }
        },
        useSoundFont = viewModel.useSf2ForTanpura.value,
        onToggleUseSoundFont = { viewModel.useSf2ForTanpura.value = it },
        channel = viewModel.tanpuraChannel.intValue,
        onChannelChange = { viewModel.tanpuraChannel.intValue = it },
        velocity = viewModel.tanpuraVelocity.intValue,
        onVelocityChange = { viewModel.tanpuraVelocity.intValue = it },
        gain = viewModel.tanpuraGain.floatValue,
        onGainChange = { viewModel.tanpuraGain.floatValue = it }
    )

    // 6. Tabla SoundFont Customization
    InstrumentCustomizationCard(
        title = "Tabla SoundFont Customization",
        assetLabel = "Select Tabla SoundFont Asset",
        assetDesc = "Quickly choose from tabla soundfonts bundled in assets.",
        assets = viewModel.listAssetSoundfonts("tabla"),
        selectedAsset = viewModel.tablaSf2File.value.substringAfterLast("/"),
        onSelectAsset = {
            viewModel.tablaSf2File.value = "tabla/$it"
            viewModel.syncSoundFontPreferences()
        },
        sf2Path = viewModel.tablaSf2File.value,
        onPathChange = { viewModel.tablaSf2File.value = it },
        presets = viewModel.getLoadedPresetNames("TABLA"),
        selectedPreset = "Default Preset",
        onSelectPreset = {
            val p = viewModel.selectPresetByIndex("TABLA", 0)
            if (p != null) {
                viewModel.tablaBank.intValue = p.first
                viewModel.tablaPreset.intValue = p.second
            }
        },
        channel = viewModel.tablaChannel.intValue,
        onChannelChange = { viewModel.tablaChannel.intValue = it },
        velocity = viewModel.tablaVelocity.intValue,
        onVelocityChange = { viewModel.tablaVelocity.intValue = it },
        gain = viewModel.tablaGain.floatValue,
        onGainChange = { viewModel.tablaGain.floatValue = it }
    )

    // 7. LEARN & CREATIVE STUDIO
    SettingsSection("LEARN & CREATIVE STUDIO", icon = Icons.Default.School) {
        // Tuner Reference Frequency
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tuner Reference Frequency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${viewModel.tunerReferenceFrequency.floatValue.toInt()} Hz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text("Standard concert pitch reference frequency (default: 440 Hz).", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Slider(
                value = viewModel.tunerReferenceFrequency.floatValue,
                onValueChange = { viewModel.tunerReferenceFrequency.floatValue = it },
                valueRange = 420f..460f,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Metronome Sound
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Metronome Sound", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Audio sample trigger style for metronome clicks.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Woodblock", "Cowbell", "Digital Beep").forEach { snd ->
                    FilterChip(
                        selected = viewModel.metronomeTickSound.value == snd,
                        onClick = { viewModel.metronomeTickSound.value = snd },
                        label = { Text(snd, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Metronome Subdivision
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Metronome Subdivision", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Clicks per beat.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "1 (Quarter)", 2 to "2 (Eighth)", 4 to "4 (Sixteenth)").forEach { (sub, lbl) ->
                    FilterChip(
                        selected = viewModel.metronomeSubdivision.intValue == sub,
                        onClick = { viewModel.metronomeSubdivision.intValue = sub },
                        label = { Text(lbl, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Falling Notes Speed
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Falling Notes Speed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Visual speed multiplier of falling notes waterfall.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Slow", "Normal", "Fast", "Extreme").forEach { spd ->
                    FilterChip(
                        selected = viewModel.fallingNotesSpeed.value == spd,
                        onClick = { viewModel.fallingNotesSpeed.value = spd },
                        label = { Text(spd, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Waterfall Theme
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Waterfall Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Color palette theme for active note tracks.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Neon Glow", "Cyberpunk", "Classic Staff").forEach { thm ->
                    FilterChip(
                        selected = viewModel.waterfallTheme.value == thm,
                        onClick = { viewModel.waterfallTheme.value = thm },
                        label = { Text(thm, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        TogglePreference(
            label = "Show Key Letter Labels",
            desc = "Overlay semitone note names (C, D, E...) directly on the piano keyboard keys.",
            checked = viewModel.showPianoKeyLabels.value,
            onCheckedChange = { viewModel.showPianoKeyLabels.value = it }
        )

        TogglePreference(
            label = "Enable Waterfall Sparks & Particles",
            desc = "Spawn floating spark particle physics when falling notes hit the keyboard.",
            checked = viewModel.showWaterfallParticles.value,
            onCheckedChange = { viewModel.showWaterfallParticles.value = it }
        )
    }
}

@Composable
private fun ExportTabContent(viewModel: AppViewModel) {
    Text(
        "ADVANCED CREATIVE EXPORT STUDIO",
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
    )

    // Rendering Visual Theme
    SettingsSection("Rendering Visual Theme", icon = Icons.Default.Palette) {
        Text("Visual style/colors of the visualizer scene.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Cyberpunk", "Classic Staff", "Neon Fall").forEach { theme ->
                FilterChip(
                    selected = viewModel.videoExportTheme.value == theme,
                    onClick = { viewModel.videoExportTheme.value = theme },
                    label = { Text(theme, fontSize = 11.sp) }
                )
            }
        }
    }

    // Resolution
    SettingsSection("Resolution", icon = Icons.Default.Hd) {
        Text("Output dimensions.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1080p", "4K").forEach { res ->
                FilterChip(
                    selected = viewModel.videoExportResolution.value == res,
                    onClick = { viewModel.videoExportResolution.value = res },
                    label = { Text(res, fontSize = 11.sp) }
                )
            }
        }
    }

    // Frame Rate (FPS)
    SettingsSection("Frame Rate (FPS)", icon = Icons.Default.Speed) {
        Text("Target video smoothness.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("24 FPS", "30 FPS", "60 FPS", "120 FPS").forEach { fps ->
                FilterChip(
                    selected = viewModel.videoExportFps.value == fps,
                    onClick = { viewModel.videoExportFps.value = fps },
                    label = { Text(fps, fontSize = 11.sp) }
                )
            }
        }
    }

    // Bitrate Limit
    SettingsSection("Bitrate Limit", icon = Icons.Default.Compress) {
        Text("Output compression rate.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("5 Mbps", "10 Mbps", "20 Mbps").forEach { br ->
                FilterChip(
                    selected = viewModel.videoExportBitrate.value == br,
                    onClick = { viewModel.videoExportBitrate.value = br },
                    label = { Text(br, fontSize = 11.sp) }
                )
            }
        }
    }

    // Video Container
    SettingsSection("Video Container", icon = Icons.Default.Movie) {
        Text("Output format container.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MP4", "MKV", "WebM").forEach { fmt ->
                FilterChip(
                    selected = viewModel.videoExportFormat.value == fmt,
                    onClick = { viewModel.videoExportFormat.value = fmt },
                    label = { Text(fmt, fontSize = 11.sp) }
                )
            }
        }
    }

    // Audio Codec
    SettingsSection("Audio Codec", icon = Icons.Default.Audiotrack) {
        Text("Compression format for the audio stream.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("AAC", "FLAC", "Opus").forEach { cdc ->
                FilterChip(
                    selected = viewModel.videoExportAudioCodec.value == cdc,
                    onClick = { viewModel.videoExportAudioCodec.value = cdc },
                    label = { Text(cdc, fontSize = 11.sp) }
                )
            }
        }
    }

    // Audio Sample Rate
    SettingsSection("Audio Sample Rate", icon = Icons.Default.GraphicEq) {
        Text("Frequency response fidelity.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("44.1 kHz", "48 kHz", "96 kHz").forEach { sr ->
                FilterChip(
                    selected = viewModel.videoExportSampleRate.value == sr,
                    onClick = { viewModel.videoExportSampleRate.value = sr },
                    label = { Text(sr, fontSize = 11.sp) }
                )
            }
        }
    }

    // Camera Perspective Yaw & Pitch
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141724),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Yaw
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Camera Perspective Yaw", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${viewModel.videoExportCameraYaw.floatValue.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Adjust horizontal viewing angle of the scene.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                Slider(
                    value = viewModel.videoExportCameraYaw.floatValue,
                    onValueChange = { viewModel.videoExportCameraYaw.floatValue = it },
                    valueRange = -45f..45f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            // Pitch
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Camera Perspective Pitch", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${viewModel.videoExportCameraPitch.floatValue.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Adjust vertical tilt viewing angle of the scene.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                Slider(
                    value = viewModel.videoExportCameraPitch.floatValue,
                    onValueChange = { viewModel.videoExportCameraPitch.floatValue = it },
                    valueRange = 0f..60f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            // Visualizer EQ Bars Density
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Visualizer EQ Bars Density", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${viewModel.videoExportEqBars.intValue} Bars", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Number of active spectrum analyzer bars.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                Slider(
                    value = viewModel.videoExportEqBars.intValue.toFloat(),
                    onValueChange = { viewModel.videoExportEqBars.intValue = it.toInt() },
                    valueRange = 16f..128f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            // Audio Reactive Peak Sensitivity
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Audio Reactive Peak Sensitivity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${viewModel.videoExportPeakSensitivity.floatValue.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Multiplier for volume-based object scaling.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                Slider(
                    value = viewModel.videoExportPeakSensitivity.floatValue,
                    onValueChange = { viewModel.videoExportPeakSensitivity.floatValue = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }

    // Export Button
    Button(
        onClick = { viewModel.exportVideoToFile { } },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text("RENDER & EXPORT CREATIVE VIDEO", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.Black)
    }
}

data class Sf2InspectionItem(
    val category: String,
    val fileName: String,
    val sizeMb: String,
    val presets: List<String>,
    val instruments: List<String>,
    val samples: List<String>,
    val samplePitchRange: String,
    val isBundled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundFontManagerTabContent(viewModel: AppViewModel) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    val categories = listOf("ALL", "TABLA", "PIANO", "GUITAR", "DRUMS", "METRONOME", "TANPURA")

    var inspectedSoundFont by remember { mutableStateOf<Sf2InspectionItem?>(null) }

    val soundfonts = remember {
        listOf(
            Sf2InspectionItem(
                category = "TABLA",
                fileName = "tabla.sf2",
                sizeMb = "2.48 MB",
                presets = listOf("Tabla (Bank 0, Patch 0)"),
                instruments = listOf("Tabla Main Head"),
                samples = listOf(
                    "TablaC3 (GE Bass 86Hz)", "TablaC#3 (TA Rim 258Hz)", "TablaD3 (TI Center)", "TablaD#3 (TIN Sur 420Hz)",
                    "TablaE3 (NA Kinar 678Hz)", "TablaF3 (KA Flat Slap)", "TablaF#3 (Tirakita Roll)", "TablaG3 (GHE Slide)",
                    "TablaG#3 (DHIN Resonant)", "TablaA3 (TU Bell Tone)", "TablaA#3 (DIN Core)", "TablaB3 (DHA Vilambit)",
                    "TablaC4 (DHA Full Sam)", "TablaC#4 (DHIN Mid)", "TablaD4 (DHAGE Compound)", "TablaD#4 (TIN Long)",
                    "TablaE4 (NA Resonant)", "TablaF4 (TA Open)", "TablaF#4 (KI Snap)", "TablaG4 (GHE Low Decay)"
                ),
                samplePitchRange = "MIDI 60..79 (C4..G5, 20 Stereo Bols)"
            ),
            Sf2InspectionItem(
                category = "TABLA",
                fileName = "Indian_Ensemble.sf2",
                sizeMb = "3.86 MB",
                presets = listOf(
                    "Tamboura/Tabla (Bank 0, Patch 0)", "Sarangi Riffs C (Bank 0, Patch 1)",
                    "Sitar (Bank 0, Patch 2)", "Low Sitar (Bank 0, Patch 3)", "Sarod (Bank 0, Patch 4)"
                ),
                instruments = listOf("Tamboura/Tabla", "Sarangi Riffs C", "Sitar", "Low Sitar", "Sarod"),
                samples = listOf(
                    "Tambouras C#2", "Tabla Tone #1-8", "Open Tabla Hit", "Banya #1-5",
                    "Banya Slap #1-3", "Sharp Hit", "Tabla/Banya #1-3", "Sitar Pakhawaj", "Sarod Strum"
                ),
                samplePitchRange = "MIDI 36..96"
            ),
            Sf2InspectionItem(
                category = "PIANO",
                fileName = "198_Yamaha_SY1_piano.sf2",
                sizeMb = "1.12 MB",
                presets = listOf("Yamaha SY-1 Piano (Bank 0, Patch 0)"),
                instruments = listOf("Yamaha sy-1 piano"),
                samples = listOf("c1(L/R)", "c2(L/R)", "c3(L/R)", "g1(L/R)", "g2(L/R)", "c5(L/R)"),
                samplePitchRange = "MIDI 24..108 (Full 88-Key)"
            ),
            Sf2InspectionItem(
                category = "PIANO",
                fileName = "SC55_Piano_V2.sf2",
                sizeMb = "1.87 MB",
                presets = listOf(
                    "Piano 1 (Bank 0, Patch 0)", "Stereo Piano (Bank 0, Patch 1)",
                    "Bright Piano (Bank 0, Patch 2)", "Dark Piano (Bank 0, Patch 3)"
                ),
                instruments = listOf("SC55 Piano V1-V5", "SC55 Piano ST1-ST5"),
                samples = listOf("SC55 Piano C1n", "C2n", "C4n", "C5n", "C7n", "E3n", "E4n", "E5n", "G#5n", "C6n"),
                samplePitchRange = "MIDI 21..108"
            ),
            Sf2InspectionItem(
                category = "PIANO",
                fileName = "Grand_Piano.sf2",
                sizeMb = "0.64 MB",
                presets = listOf("Grand Piano (Bank 0, Patch 0)"),
                instruments = listOf("Grand Piano"),
                samples = listOf("PianoA G#0", "PianoA C#1", "PianoA F1", "PianoA C2", "PianoA G2", "PianoA C3", "PianoA F#3", "PianoA C4", "PianoA F4", "PianoA B4", "PianoA F5", "PianoA G5", "PianoA B5"),
                samplePitchRange = "MIDI 21..108"
            ),
            Sf2InspectionItem(
                category = "GUITAR",
                fileName = "Custom Classical Guitar.sf2",
                sizeMb = "15.35 MB",
                presets = listOf("Classical Guitar (Bank 0, Patch 0)"),
                instruments = listOf("Classical Guitar"),
                samples = listOf("Guitar E2", "Guitar D3", "Guitar A3", "Guitar D4", "Guitar G4", "Guitar B4", "Guitar E5", "Guitar A5", "Guitar D#6", "Guitar E6", "Guitar A6"),
                samplePitchRange = "MIDI 40..93 (E2..A6)"
            ),
            Sf2InspectionItem(
                category = "GUITAR",
                fileName = "guitar_samples_clean.sf2",
                sizeMb = "17.76 MB",
                presets = listOf("guitar_samples_clean (Bank 0, Patch 0)", "guitar_samples_jazz (Bank 0, Patch 1)"),
                instruments = listOf("Clean Guitar", "Jazz Guitar"),
                samples = listOf("F2_s1_01", "G3_s4_01", "E4_s6_01", "G4_s6_01", "G#5_s6_03"),
                samplePitchRange = "MIDI 36..85 (C2..C#6)"
            ),
            Sf2InspectionItem(
                category = "DRUMS",
                fileName = "Metal_Drums.sf2",
                sizeMb = "2.01 MB",
                presets = listOf("MetlDrums Damp (1-4)", "MetlDrums Dry (1-4)", "MetlDrums Soak (1-4)", "Kick Stacks", "Snare Stacks", "Solo HiHats", "Solo Toms"),
                instruments = listOf("Metal Drum Kit"),
                samples = listOf("Metal Kick 1-3", "Metal Snare 1-2", "Metal HiHat Cl/Op", "Metal Crash 1-3", "Metal China Crsh", "Metal Splash"),
                samplePitchRange = "GM Drum Keys (MIDI 35..81)"
            ),
            Sf2InspectionItem(
                category = "DRUMS",
                fileName = "drums_ken_ardency.sf2",
                sizeMb = "11.48 MB",
                presets = listOf("drum kit (Bank 128, Patch 0)"),
                instruments = listOf("Ken Ardency Drum Kit"),
                samples = listOf("WRM KIK1H", "MEDSNR3Q", "HI-HAT CL", "HI-HAT OP", "RIDE AC", "CRASH 2", "17 CHINA", "10 SPLASH"),
                samplePitchRange = "GM Standard Kit (MIDI 36..81)"
            ),
            Sf2InspectionItem(
                category = "METRONOME",
                fileName = "Metronom.sf2",
                sizeMb = "0.01 MB",
                presets = listOf("Metronom Percussion (Bank 128, Patch 48)", "Metronom GM (Bank 0, Patch 115)"),
                instruments = listOf("Metronome Click"),
                samples = listOf("tick (MIDI 76 - High Woodblock)", "tack (MIDI 77 - Low Woodblock)"),
                samplePitchRange = "MIDI 76..77 (Tick=76, Tack=77)"
            ),
            Sf2InspectionItem(
                category = "TANPURA",
                fileName = "Tanpura_drone.sf2",
                sizeMb = "22.75 MB",
                presets = listOf("Tanpura (Bank 0, Patch 0)"),
                instruments = listOf("tanpura_drone"),
                samples = listOf("tanpura_C", "tanpura_C#", "tanpura_D", "tanpura_D#", "tanpura_E", "tanpura_F", "tanpura_F#", "tanpura_G", "tanpura_G#", "tanpura_A", "tanpura_A#", "tanpura_B"),
                samplePitchRange = "MIDI 60..71 (12 Root Chromatic Drones)"
            )
        )
    }

    val filteredList = soundfonts.filter {
        selectedCategory == "ALL" || it.category.equals(selectedCategory, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SOUNDFONT AUTO-INSPECTOR & AUDITION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    "Automatically matches sample pitches, patches, and bol articulations",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Custom SoundFont Importer Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141724),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("IMPORT CUSTOM SOUNDFONT (.SF2)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text("Auto-indexes instruments, presets, and sample pitch boundaries", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var customPath by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = customPath,
                        onValueChange = { customPath = it },
                        placeholder = { Text("/sdcard/Download/my_tabla.sf2", fontSize = 11.sp, color = Color.White.copy(0.4f)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White)
                    )
                    Button(
                        onClick = {
                            if (customPath.isNotBlank()) {
                                val engine = com.creativeblisslabs.peqlearn.app.audio.getAudioEngine()
                                val detectedCategory = when {
                                    customPath.contains("tabla", true) -> "TABLA"
                                    customPath.contains("piano", true) -> "PIANO"
                                    customPath.contains("guitar", true) -> "GUITAR"
                                    customPath.contains("drum", true) -> "DRUMS"
                                    customPath.contains("tanpura", true) -> "TANPURA"
                                    else -> if (selectedCategory != "ALL") selectedCategory else "TABLA"
                                }

                                // 1. Run dynamic binary inspection and auto-mapping
                                engine.loadCustomSoundFont(detectedCategory, customPath)

                                // 2. Update view model state
                                when (detectedCategory) {
                                    "TABLA" -> viewModel.tablaSf2File.value = customPath
                                    "PIANO" -> viewModel.pianoSf2File.value = customPath
                                    "GUITAR" -> viewModel.guitarSf2File.value = customPath
                                    "DRUMS" -> viewModel.drumSf2File.value = customPath
                                    "TANPURA" -> viewModel.tanpuraSf2File.value = customPath
                                    else -> viewModel.tablaSf2File.value = customPath
                                }
                                viewModel.syncSoundFontPreferences()

                                // Set as active inspected SF
                                val fileName = customPath.substringAfterLast("/")
                                inspectedSoundFont = Sf2InspectionItem(
                                    category = detectedCategory,
                                    fileName = fileName,
                                    sizeMb = "Custom",
                                    presets = listOf("Auto-Loaded Preset"),
                                    instruments = listOf("Custom Instrument ($detectedCategory)"),
                                    samples = listOf("Dynamically auto-indexed from SF2 headers"),
                                    samplePitchRange = "Dynamic Auto-Mapped",
                                    isBundled = false
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("LOAD & AUTO-MAP", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.Black)
                    }
                }
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // SoundFont Cards
        filteredList.forEach { sf ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10131F),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    sf.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(sf.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                        Text(sf.sizeMb, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }

                    // Key details
                    Text("Sample Range: ${sf.samplePitchRange}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Text("Presets: ${sf.presets.size} | Instruments: ${sf.instruments.size} | Samples: ${sf.samples.size}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))

                    // Action Buttons Row: AUDITION, DETAILS SHEET, SET ACTIVE
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val engine = com.creativeblisslabs.peqlearn.app.audio.getAudioEngine()
                                when (sf.category) {
                                    "TABLA" -> engine.playSample("DHA", 0, "TABLA")
                                    "PIANO" -> engine.noteOn(60, 0.8f)
                                    "GUITAR" -> engine.noteOn(64, 0.8f)
                                    "DRUMS" -> engine.playSample("KICK", 0, "DRUM")
                                    "METRONOME" -> engine.playSample("METRONOME-STRONG", 0, "SYSTEM")
                                    "TANPURA" -> engine.playSample("C", 0, "TANPURA")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, null, Modifier.size(14.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                inspectedSoundFont = sf
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.TableChart, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Button(
                            onClick = {
                                val engine = com.creativeblisslabs.peqlearn.app.audio.getAudioEngine()
                                val assetPath = "${sf.category.lowercase()}/${sf.fileName}"

                                // Dynamically load and auto-map
                                engine.loadCustomSoundFont(sf.category, assetPath)

                                when (sf.category) {
                                    "TABLA" -> viewModel.tablaSf2File.value = "tabla/${sf.fileName}"
                                    "PIANO" -> viewModel.pianoSf2File.value = "piano/${sf.fileName}"
                                    "GUITAR" -> viewModel.guitarSf2File.value = "guitar/${sf.fileName}"
                                    "DRUMS" -> viewModel.drumSf2File.value = "drums/${sf.fileName}"
                                    "METRONOME" -> viewModel.metronomeSf2File.value = "metronome/${sf.fileName}"
                                    "TANPURA" -> viewModel.tanpuraSf2File.value = "tanpura/${sf.fileName}"
                                }
                                viewModel.syncSoundFontPreferences()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                            modifier = Modifier.height(34.dp).weight(1f)
                        ) {
                            Text("SET AS ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // --- SOUNDFONT INFO BOTTOM SHEET MODAL TABLE ---
    inspectedSoundFont?.let { sf ->
        ModalBottomSheet(
            onDismissRequest = { inspectedSoundFont = null },
            containerColor = Color(0xFF0F121C),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bottom Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    sf.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(sf.fileName, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text(
                            "Size: ${sf.sizeMb} • Pitch Range: ${sf.samplePitchRange}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = { inspectedSoundFont = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SAMPLES (${sf.samples.size})", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.2f))
                    Text("INSTRUMENTS (${sf.instruments.size})", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    Text("PRESETS (${sf.presets.size})", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                }

                // Table Content Rows
                val maxRows = maxOf(sf.samples.size, sf.instruments.size, sf.presets.size).coerceAtLeast(1)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (rowIdx in 0 until maxRows) {
                        val sampleText = sf.samples.getOrNull(rowIdx) ?: "—"
                        val instText = sf.instruments.getOrNull(rowIdx) ?: "—"
                        val presetText = sf.presets.getOrNull(rowIdx) ?: "—"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (rowIdx % 2 == 0) Color.White.copy(alpha = 0.02f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sampleText,
                                fontSize = 11.sp,
                                color = if (sampleText != "—") Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = instText,
                                fontSize = 11.sp,
                                color = if (instText != "—") Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = presetText,
                                fontSize = 11.sp,
                                color = if (presetText != "—") MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Footer Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val engine = com.creativeblisslabs.peqlearn.app.audio.getAudioEngine()
                            when (sf.category) {
                                "TABLA" -> engine.playSample("DHA", 0, "TABLA")
                                "PIANO" -> engine.noteOn(60, 0.8f)
                                "GUITAR" -> engine.noteOn(64, 0.8f)
                                "DRUMS" -> engine.playSample("KICK", 0, "DRUM")
                                "METRONOME" -> engine.playSample("METRONOME-STRONG", 0, "SYSTEM")
                                "TANPURA" -> engine.playSample("C", 0, "TANPURA")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("AUDITION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val engine = com.creativeblisslabs.peqlearn.app.audio.getAudioEngine()
                            val path = if (sf.isBundled) "${sf.category.lowercase()}/${sf.fileName}" else sf.fileName
                            engine.loadCustomSoundFont(sf.category, path)
                            when (sf.category) {
                                "TABLA" -> viewModel.tablaSf2File.value = path
                                "PIANO" -> viewModel.pianoSf2File.value = path
                                "GUITAR" -> viewModel.guitarSf2File.value = path
                                "DRUMS" -> viewModel.drumSf2File.value = path
                                "METRONOME" -> viewModel.metronomeSf2File.value = path
                                "TANPURA" -> viewModel.tanpuraSf2File.value = path
                            }
                            viewModel.syncSoundFontPreferences()
                            inspectedSoundFont = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp), tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("SET ACTIVE & MAP", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstrumentCustomizationCard(
    title: String,
    assetLabel: String,
    assetDesc: String,
    assets: List<String>,
    selectedAsset: String,
    onSelectAsset: (String) -> Unit,
    sf2Path: String,
    onPathChange: (String) -> Unit,
    presets: List<String>,
    selectedPreset: String,
    onSelectPreset: (String) -> Unit,
    channel: Int,
    onChannelChange: (Int) -> Unit,
    velocity: Int,
    onVelocityChange: (Int) -> Unit,
    gain: Float? = null,
    onGainChange: ((Float) -> Unit)? = null,
    useSoundFont: Boolean? = null,
    onToggleUseSoundFont: ((Boolean) -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141724),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Select Asset
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(assetLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(assetDesc, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    assets.forEach { ast ->
                        FilterChip(
                            selected = ast == selectedAsset,
                            onClick = { onSelectAsset(ast) },
                            label = { Text(ast, fontSize = 10.sp) }
                        )
                    }
                }
            }

            // Path & Browse
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SoundFont SF2 File", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sf2Path,
                        onValueChange = onPathChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White)
                    )
                    Button(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    ) {
                        Text("Browse", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            // Preset Dropdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Select Preset (Sound)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Choose instrument patch presets directly from the loaded SoundFont.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                FilterChip(
                    selected = true,
                    onClick = { onSelectPreset(selectedPreset) },
                    label = { Text(selectedPreset, fontSize = 11.sp) }
                )
            }

            // Optional SoundFont toggle
            if (useSoundFont != null && onToggleUseSoundFont != null) {
                TogglePreference(
                    label = "Use SoundFont for $title",
                    desc = "Currently playing using default high-quality audio files",
                    checked = useSoundFont,
                    onCheckedChange = onToggleUseSoundFont
                )
            }

            // Channel Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MIDI Channel (0-15)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("$channel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = channel.toFloat(),
                    onValueChange = { onChannelChange(it.toInt()) },
                    valueRange = 0f..15f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            // Velocity Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fixed Velocity key strike", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("$velocity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = velocity.toFloat(),
                    onValueChange = { onVelocityChange(it.toInt()) },
                    valueRange = 0f..127f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            // Output Gain Slider
            if (gain != null && onGainChange != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Output Gain (Volume)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        Text("${(gain * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = gain,
                        onValueChange = onGainChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

// Reusable Preferences
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141724),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
            content()
        }
    }
}

@Composable
fun TogglePreference(
    label: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Text(desc, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun ActionPreference(
    title: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.04f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Text(desc, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun ThemeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun AccentCircle(
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accentColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ThemeSelectionRow(viewModel: AppViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        pEQTheme.entries.forEach { thm ->
            ThemeChip(
                label = thm.name,
                isSelected = viewModel.selectedTheme.value == thm,
                onClick = { viewModel.updateTheme(thm) }
            )
        }
    }
}

@Composable
fun AccentSelectionRow(viewModel: AppViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        pEQAccent.entries.forEach { acc ->
            AccentCircle(
                accentColor = acc.color,
                isSelected = viewModel.selectedAccent.value == acc,
                onClick = { viewModel.updateAccent(acc) }
            )
        }
    }
}

@Composable
fun SelectorPreference(
    label: String,
    desc: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
        Text(desc, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = opt == selected,
                    onClick = { onSelect(opt) },
                    label = { Text(opt, fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
fun InputPreference(
    label: String,
    desc: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
        Text(desc, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp)
        )
    }
}

@Composable
fun TextPreference(
    label: String,
    desc: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPickFile: (() -> Unit)? = null
) {
    InputPreference(label = label, desc = desc, value = value, onValueChange = onValueChange)
}