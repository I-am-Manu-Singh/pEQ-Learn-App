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
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(viewModel: AppViewModel) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isLandscapeOrWide = maxWidth > 800.dp || maxWidth > maxHeight
        val tabs = listOf("SYSTEM", "INSTRUMENTS", "SOUNDFONTS", "EXPORT")
        val tabIcons = listOf(Icons.Default.Settings, Icons.Default.MusicNote, Icons.Default.GraphicEq, Icons.Default.Share)

        if (isLandscapeOrWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT NAVIGATION RAIL
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "PREFERENCES",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = viewModel.selectedSettingsTab.intValue == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectedSettingsTab.intValue = index },
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcons.getOrElse(index) { Icons.Default.Settings },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                // RIGHT CONTENT AREA
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (viewModel.selectedSettingsTab.intValue) {
                        0 -> SystemTabContent(viewModel)
                        1 -> InstrumentsTabContent(viewModel)
                        2 -> SoundFontManagerTabContent(viewModel)
                        3 -> ExportTabContent(viewModel)
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        } else {
            // PORTRAIT TAB ROW + SCROLLABLE COLUMN
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
                    Text(
                        "SYSTEM PREFERENCES",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Main Tabs: SYSTEM | INSTRUMENTS | SOUNDFONTS | EXPORT
TabRow( // FIXED: Changed ScrollableTabRow to TabRow
    selectedTabIndex = viewModel.selectedSettingsTab.intValue,
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.primary,
    // REMOVED: edgePadding = 0.dp (Not needed or supported in full-width TabRow)
    indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedSettingsTab.intValue]),
            color = MaterialTheme.colorScheme.primary
        )
    },
    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) }
) {
    tabs.forEachIndexed { index, title ->
        val isSelected = viewModel.selectedSettingsTab.intValue == index
        Tab(
            selected = isSelected,
            onClick = { viewModel.selectedSettingsTab.intValue = index },
            modifier = Modifier.weight(1f), // Forces tabs to divide the width evenly if they fit
            icon = {
                Icon(
                    imageVector = tabIcons.getOrElse(index) { Icons.Default.Settings },
                    contentDescription = title,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            },
            text = {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
    }
}

@Composable
private fun SystemTabContent(viewModel: AppViewModel) {
    // AESTHETICS & THEME
    SettingsSection("AESTHETICS & THEME", icon = Icons.Default.Palette) {
        Text("STATION THEME", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        ThemeSelectionRow(viewModel)
        Spacer(Modifier.height(8.dp))

        Text("INDUSTRIAL ACCENT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        AccentSelectionRow(viewModel)
        Spacer(Modifier.height(8.dp))

        TogglePreference(
            label = "Use OpenSheetMusicDisplay (OSMD)",
            desc = "Use the high-quality OpenSheetMusicDisplay webview engraving engine for sheet music instead of the native canvas renderer.",
            checked = viewModel.useOsmdRenderer.value,
            onCheckedChange = { viewModel.useOsmdRenderer.value = it }
        )
    }

    // Pitch Detection Algorithm
    SettingsSection("Pitch Detection Algorithm", icon = Icons.Default.GraphicEq) {
        Text("DSP algorithm used for realtime pitch and chord parsing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Phase Vocoder", "FFT Pitch", "PSOLA").forEach { algo ->
                pEQFilterChip(
                    selected = viewModel.pitchEngineAlgo.value == algo,
                    onClick = { viewModel.pitchEngineAlgo.value = algo },
                    label = { Text(algo, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
            }
        }
    }

    // AI Stem Quality
    SettingsSection("AI Stem Quality", icon = Icons.Default.AutoAwesome) {
        Text("Model depth and decoupling quality for source separation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Standard", "Pro Neural").forEach { quality ->
                pEQFilterChip(
                    selected = viewModel.stemSplitQuality.value == quality,
                    onClick = { viewModel.stemSplitQuality.value = quality },
                    label = { Text(quality, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
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
    SectionHeader("SOUNDFONT & INSTRUMENT CUSTOMIZATION")
    Spacer(Modifier.height(8.dp))

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
                Text("Tuner Reference Frequency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${viewModel.tunerReferenceFrequency.floatValue.toInt()} Hz", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text("Standard concert pitch reference frequency (default: 440 Hz).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
            Text("Metronome Sound", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Audio sample trigger style for metronome clicks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Woodblock", "Cowbell", "Digital Beep").forEach { snd ->
                    pEQFilterChip(
                        selected = viewModel.metronomeTickSound.value == snd,
                        onClick = { viewModel.metronomeTickSound.value = snd },
                        label = { Text(snd, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Metronome Subdivision
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Metronome Subdivision", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Clicks per beat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "1 (Quarter)", 2 to "2 (Eighth)", 4 to "4 (Sixteenth)").forEach { (sub, lbl) ->
                    pEQFilterChip(
                        selected = viewModel.metronomeSubdivision.intValue == sub,
                        onClick = { viewModel.metronomeSubdivision.intValue = sub },
                        label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Unified Piano & Visualizer Studio Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "All real-time MIDI visualizer settings (3D perspective, horizon vanishing depth, falling speed, particle physics, multi-track hand colors, and themes) are unified in the Piano Keyboard studio bottomsheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Button(
                    onClick = {
                        viewModel.currentTab.value = com.creativeblisslabs.peqlearn.app.Tab.Learn
                        viewModel.showSettings.value = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Open Visualizer Settings Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
fun ExportTabContent(viewModel: AppViewModel) {
    SectionHeader("EXPORT STUDIO")
    Spacer(Modifier.height(8.dp))

    // Video & Composition Export Section
    VideoExportStudioSection(viewModel)

    Spacer(Modifier.height(16.dp))

    // High-Resolution Sheet Music PDF Export Section
    PdfExportStudioSection(viewModel)
}

@Composable
fun VideoExportStudioSection(viewModel: AppViewModel) {
    // Layout Mode (Combined / Visualizer / Notation)
    SettingsSection("Export Composition Mode", icon = Icons.Default.Dashboard) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Score + Waterfall", "Waterfall", "Notation").forEach { mode ->
                pEQFilterChip(
                    selected = viewModel.videoExportLayoutMode.value == mode,
                    onClick = { viewModel.videoExportLayoutMode.value = mode },
                    label = { Text(mode, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Rendering Visual Theme
    SettingsSection("Rendering Visual Theme", icon = Icons.Default.Palette) {
        Text("Visual style/colors of the visualizer scene.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Cyberpunk", "Classic Staff", "Neon Fall").forEach { theme ->
                pEQFilterChip(
                    selected = viewModel.videoExportTheme.value == theme,
                    onClick = { viewModel.videoExportTheme.value = theme },
                    label = { Text(theme, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Resolution
    SettingsSection("Resolution", icon = Icons.Default.Hd) {
        Text("Output dimensions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1080p", "4K").forEach { res ->
                pEQFilterChip(
                    selected = viewModel.videoExportResolution.value == res,
                    onClick = { viewModel.videoExportResolution.value = res },
                    label = { Text(res, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Frame Rate (FPS)
    SettingsSection("Frame Rate (FPS)", icon = Icons.Default.Speed) {
        Text("Target video smoothness.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("24 FPS", "30 FPS", "60 FPS", "120 FPS").forEach { fps ->
                pEQFilterChip(
                    selected = viewModel.videoExportFps.value == fps,
                    onClick = { viewModel.videoExportFps.value = fps },
                    label = { Text(fps, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Bitrate Limit
    SettingsSection("Bitrate Limit", icon = Icons.Default.Compress) {
        Text("Output compression rate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("5 Mbps", "10 Mbps", "20 Mbps").forEach { br ->
                pEQFilterChip(
                    selected = viewModel.videoExportBitrate.value == br,
                    onClick = { viewModel.videoExportBitrate.value = br },
                    label = { Text(br, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Video Container
    SettingsSection("Video Container", icon = Icons.Default.Movie) {
        Text("Output format container.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MP4", "MKV", "WebM").forEach { fmt ->
                pEQFilterChip(
                    selected = viewModel.videoExportFormat.value == fmt,
                    onClick = { viewModel.videoExportFormat.value = fmt },
                    label = { Text(fmt, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Audio Codec
    SettingsSection("Audio Codec", icon = Icons.Default.Audiotrack) {
        Text("Compression format for the audio stream.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("AAC", "FLAC", "Opus").forEach { cdc ->
                pEQFilterChip(
                    selected = viewModel.videoExportAudioCodec.value == cdc,
                    onClick = { viewModel.videoExportAudioCodec.value = cdc },
                    label = { Text(cdc, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Audio Sample Rate
    SettingsSection("Audio Sample Rate", icon = Icons.Default.GraphicEq) {
        Text("Frequency response fidelity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("44.1 kHz", "48 kHz", "96 kHz").forEach { sr ->
                pEQFilterChip(
                    selected = viewModel.videoExportSampleRate.value == sr,
                    onClick = { viewModel.videoExportSampleRate.value = sr },
                    label = { Text(sr, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Camera Perspective Yaw & Pitch & Visualizer Configs
    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Yaw
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Camera Perspective Yaw", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${viewModel.videoExportCameraYaw.floatValue.toInt()}°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Adjust horizontal viewing angle of the scene.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text("Camera Perspective Pitch", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${viewModel.videoExportCameraPitch.floatValue.toInt()}°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Adjust vertical tilt viewing angle of the scene.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text("Visualizer EQ Bars Density", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${viewModel.videoExportEqBars.intValue} Bars", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Number of active spectrum analyzer bars.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text("Audio Reactive Peak Sensitivity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${viewModel.videoExportPeakSensitivity.floatValue.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Multiplier for volume-based object scaling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Slider(
                    value = viewModel.videoExportPeakSensitivity.floatValue,
                    onValueChange = { viewModel.videoExportPeakSensitivity.floatValue = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }

    // Export Progress & Status
    if (viewModel.isVideoExporting.value) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Encoding Video Frames & Audio Stream...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(viewModel.videoExportProgress.floatValue * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { viewModel.videoExportProgress.floatValue },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }

    // Export Button
    Button(
        onClick = { viewModel.exportVideoToFile { } },
        enabled = !viewModel.isVideoExporting.value,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (viewModel.isVideoExporting.value) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(10.dp))
            Text("EXPORTING VIDEO...", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text("RENDER & EXPORT CREATIVE VIDEO", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun PdfExportStudioSection(viewModel: AppViewModel) {
    SectionHeader("HIGH-RESOLUTION SHEET MUSIC PDF EXPORT")
    Spacer(Modifier.height(8.dp))

    val pdfOpts = viewModel.pdfExportOptions.value

    // PDF Paper Size & Orientation
    SettingsSection("Page Dimensions & Orientation", icon = Icons.Default.Description) {
        Text("Standard sheet music print formats.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("A4", "Letter", "Legal", "A3").forEach { size ->
                pEQFilterChip(
                    selected = pdfOpts.pageSize == size,
                    onClick = { viewModel.pdfExportOptions.value = pdfOpts.copy(pageSize = size) },
                    label = { Text(size, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Portrait", "Landscape").forEach { orient ->
                pEQFilterChip(
                    selected = pdfOpts.orientation == orient,
                    onClick = { viewModel.pdfExportOptions.value = pdfOpts.copy(orientation = orient) },
                    label = { Text(orient, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // PDF Notation Layout Format
    SettingsSection("Score Notation Format", icon = Icons.Default.LibraryMusic) {
        Text("Choose which staves to include in exported sheet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Both", "Staff", "Tab").forEach { fmt ->
                pEQFilterChip(
                    selected = pdfOpts.staffFormat == fmt,
                    onClick = { viewModel.pdfExportOptions.value = pdfOpts.copy(staffFormat = fmt) },
                    label = { Text(if (fmt == "Both") "Grand Staff + TAB" else fmt, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // PDF Visual Color Theme
    SettingsSection("Color Theme", icon = Icons.Default.InvertColors) {
        Text("Print contrast style.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Monochrome", "Color (LH/RH)", "Inverted Dark").forEach { theme ->
                pEQFilterChip(
                    selected = pdfOpts.colorTheme == theme,
                    onClick = { viewModel.pdfExportOptions.value = pdfOpts.copy(colorTheme = theme) },
                    label = { Text(theme, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // PDF Engraving Options (Switches)
    SettingsSection("Engraving & Notation Detail Elements", icon = Icons.Default.Checklist) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.pdfExportOptions.value = pdfOpts.copy(includeTitleHeader = !pdfOpts.includeTitleHeader)
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Include Title & Composer Header", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = pdfOpts.includeTitleHeader,
                    onCheckedChange = { viewModel.pdfExportOptions.value = pdfOpts.copy(includeTitleHeader = it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.pdfExportOptions.value = pdfOpts.copy(includeMeasureNumbers = !pdfOpts.includeMeasureNumbers)
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Include Measure Numbers (M.1, M.2...)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = pdfOpts.includeMeasureNumbers,
                    onCheckedChange = { viewModel.pdfExportOptions.value = pdfOpts.copy(includeMeasureNumbers = it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.pdfExportOptions.value = pdfOpts.copy(includeChordLabels = !pdfOpts.includeChordLabels)
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Include Note Letters Inside Noteheads", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = pdfOpts.includeChordLabels,
                    onCheckedChange = { viewModel.pdfExportOptions.value = pdfOpts.copy(includeChordLabels = it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.pdfExportOptions.value = pdfOpts.copy(includeLyrics = !pdfOpts.includeLyrics)
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Include Lyrics Line", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = pdfOpts.includeLyrics,
                    onCheckedChange = { viewModel.pdfExportOptions.value = pdfOpts.copy(includeLyrics = it) }
                )
            }
        }
    }

    // Measures per System Slider
    SettingsSection("Density: Measures Per System (${pdfOpts.measuresPerSystem})", icon = Icons.Default.ViewColumn) {
        Slider(
            value = pdfOpts.measuresPerSystem.toFloat(),
            onValueChange = { viewModel.pdfExportOptions.value = pdfOpts.copy(measuresPerSystem = it.toInt()) },
            valueRange = 2f..8f,
            steps = 5,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
    }

    // PDF Export Progress Bar
    if (viewModel.isExportingPDF.value) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compiling High-Resolution Vector PDF Pages...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(viewModel.pdfExportProgress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { viewModel.pdfExportProgress.value },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }

    // Compile & Export PDF Button
    Button(
        onClick = { viewModel.compileCurrentNotationToPDF() },
        enabled = !viewModel.isExportingPDF.value,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
    ) {
        if (viewModel.isExportingPDF.value) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(Modifier.width(10.dp))
            Text("COMPILING PDF...", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White)
        } else {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("EXPORT SHEET MUSIC PDF", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White)
        }
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Custom SoundFont Importer Card
        pEQGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("IMPORT CUSTOM SOUNDFONT (.SF2)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Auto-indexes instruments, presets, and sample pitch boundaries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                        placeholder = { Text("/sdcard/Download/my_tabla.sf2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
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
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("LOAD & AUTO-MAP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
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
                pEQFilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // SoundFont Cards
        filteredList.forEach { sf ->
            pEQGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetadataBadge(sf.category)
                            Text(sf.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(sf.sizeMb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    // Key details
                    Text("Sample Range: ${sf.samplePitchRange}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("Presets: ${sf.presets.size} | Instruments: ${sf.instruments.size} | Samples: ${sf.samples.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

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
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(4.dp))
                            Text("TEST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        OutlinedButton(
                            onClick = {
                                inspectedSoundFont = sf
                            },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.TableChart, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                            modifier = Modifier.height(34.dp).weight(1f)
                        ) {
                            Text("SET AS ACTIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
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
            containerColor = MaterialTheme.colorScheme.surface,
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
                            MetadataBadge(sf.category)
                            Text(sf.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "Size: ${sf.sizeMb} • Pitch Range: ${sf.samplePitchRange}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(onClick = { inspectedSoundFont = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SAMPLES (${sf.samples.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.2f))
                    Text("INSTRUMENTS (${sf.instruments.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    Text("PRESETS (${sf.presets.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
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
                                    if (rowIdx % 2 == 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sampleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sampleText != "—") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = instText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (instText != "—") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = presetText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (presetText != "—") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(6.dp))
                        Text("AUDITION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1.5f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("SET ACTIVE & MAP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
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
    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Select Asset
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(assetLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(assetDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    assets.forEach { ast ->
                        pEQFilterChip(
                            selected = ast == selectedAsset,
                            onClick = { onSelectAsset(ast) },
                            label = { Text(ast, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Path & Browse
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SoundFont SF2 File", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Button(
                        onClick = { },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    ) {
                        Text("Browse", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            // Preset Dropdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Select Preset (Sound)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Choose instrument patch presets directly from the loaded SoundFont.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                pEQFilterChip(
                    selected = true,
                    onClick = { onSelectPreset(selectedPreset) },
                    label = { Text(selectedPreset, style = MaterialTheme.typography.labelSmall) }
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
                    Text("MIDI Channel (0-15)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("$channel", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                    Text("Fixed Velocity key strike", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("$velocity", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                        Text("Output Gain (Volume)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${(gain * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
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
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 14.sp)
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
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