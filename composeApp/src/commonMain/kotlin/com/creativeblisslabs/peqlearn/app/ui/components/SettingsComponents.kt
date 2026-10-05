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

@Composable
fun SettingsPanel(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B0F))
            .padding(horizontal = 20.dp, vertical = 16.dp)
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
                Text(
                    "INDUSTRIAL WORKSTATION v2.0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
            IconButton(
                onClick = { viewModel.currentTab.value = Tab.Dashboard },
                modifier = Modifier.background(Color.White.copy(alpha = 0.06f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }

        // Three Main Tabs: SYSTEM | INSTRUMENTS | EXPORT
        val tabs = listOf("SYSTEM", "INSTRUMENTS", "EXPORT")
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
                2 -> ExportTabContent(viewModel)
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
        onVelocityChange = { viewModel.tanpuraVelocity.intValue = it }
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
        onVelocityChange = { viewModel.tablaVelocity.intValue = it }
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
