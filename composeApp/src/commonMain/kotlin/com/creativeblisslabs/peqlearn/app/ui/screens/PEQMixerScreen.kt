package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

class AudioStemChannel(
    val name: String,
    val iconName: String,
    initialVolume: Float = 0.85f,
    initialPan: Float = 0.0f,
    initialMuted: Boolean = false,
    initialSolo: Boolean = false
) {
    val volumeState = mutableFloatStateOf(initialVolume)
    var volume: Float
        get() = volumeState.floatValue
        set(v) { volumeState.floatValue = v }

    val panState = mutableFloatStateOf(initialPan)
    var pan: Float
        get() = panState.floatValue
        set(p) { panState.floatValue = p }

    val isMutedState = mutableStateOf(initialMuted)
    var isMuted: Boolean
        get() = isMutedState.value
        set(m) { isMutedState.value = m }

    val isSoloState = mutableStateOf(initialSolo)
    var isSolo: Boolean
        get() = isSoloState.value
        set(s) { isSoloState.value = s }
}

@Composable
fun pEQMixerScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit = {}
) {
    val selectedModel = viewModel.stemSeparationModel.value
    val targetStems = remember(selectedModel) {
        when {
            selectedModel.contains("2-Stem") || selectedModel.contains("Spleeter") -> listOf(
                AudioStemChannel("Vocals", "Mic", 0.85f, 0.0f),
                AudioStemChannel("Accompaniment", "Radio", 0.85f, 0.0f)
            )
            selectedModel.contains("4-Stem") || selectedModel.contains("Demucs") -> listOf(
                AudioStemChannel("Vocals", "Mic", 0.85f, 0.0f),
                AudioStemChannel("Drums", "Radio", 0.90f, 0.0f),
                AudioStemChannel("Bass", "GraphicEq", 0.80f, 0.0f),
                AudioStemChannel("Other", "AutoFixHigh", 0.70f, 0.0f)
            )
            else -> listOf(
                AudioStemChannel("Vocals", "Mic", 0.85f, 0.0f),
                AudioStemChannel("Drums", "Radio", 0.90f, 0.0f),
                AudioStemChannel("Bass", "GraphicEq", 0.80f, 0.0f),
                AudioStemChannel("Piano", "Piano", 0.75f, -0.3f),
                AudioStemChannel("Guitar", "MusicNote", 0.78f, 0.35f),
                AudioStemChannel("Other / FX", "AutoFixHigh", 0.65f, 0.1f)
            )
        }
    }

    val stems = remember(targetStems) {
        mutableStateListOf<AudioStemChannel>().apply { addAll(targetStems) }
    }

    var masterVolume by remember { mutableStateOf(0.9f) }
    val isStemSplitting by viewModel.isStemSplitting
    val splitProgress by viewModel.stemSplitProgress
    val statusMessage by viewModel.stemSplitStatusMessage
    val isStemsPlaying by viewModel.isStemsPlaying
    val currentTrack = viewModel.selectedMixerItem.value ?: viewModel.currentMediaItem.value

    var showExportDialog by remember { mutableStateOf(false) }
    var showTrackSelectDialog by remember { mutableStateOf(false) }
    var showModelChooserDialog by remember { mutableStateOf(false) }

    LaunchedEffect(stems) {
        stems.forEach { stem ->
            viewModel.updateStemVolume(stem.name, stem.volume)
            viewModel.updateStemPan(stem.name, stem.pan)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp
            ) {
               
                    
                                Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "AI STEM MIXER",
                                                fontSize = 16.sp,

                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTrack?.displayName ?: "No Track Loaded • Tap Import",
                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,

                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                             LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                       item{
                        // Track Import Button
                        OutlinedButton(
                            onClick = {
                                if (viewModel.playbackQueue.isNotEmpty()) {
                                    showTrackSelectDialog = true
                                } else {
                                    viewModel.onPickTrackRequested?.invoke { item ->
                                        viewModel.selectTrackForMixer(item)
                                    }
                                }
                            },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("IMPORT", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    item{
                        // Split Stems Trigger Button
                        Button(
                            onClick = { viewModel.startStemSplit() },
                            enabled = !isStemSplitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isStemSplitting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text("SPLITTING...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("SPLIT STEMS", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    item{
                        // Export Stems Button
                        FilledTonalButton(
                            onClick = { showExportDialog = true },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("EXPORT", style = MaterialTheme.typography.labelMedium)
                        }

                        // Master Stems Play/Pause
                        IconButton(
                            onClick = { viewModel.toggleStemsPlayback() }
                        ) {
                            Icon(
                                if (isStemsPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play/Pause Stems",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                        }
                     

                  
                
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isLandscapeOrWide = maxWidth > 800.dp || maxWidth > maxHeight

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Neural Processing & Status Banner
                if (isStemSplitting || statusMessage.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                                if (isStemSplitting) {
                                    Text("${(splitProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (isStemSplitting) {
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { splitProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                if (isLandscapeOrWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // LEFT PANEL: Master Bus output fader card, presets, headroom
                        Column(
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            pEQGlassCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                     Text(
                text = "MASTER VOLUME",
        color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black            )


            Text(
                text = "${(masterVolume * 100).toInt()}% • 24-bit Lossless Engine",
                                    color = MaterialTheme.colorScheme.onSurface
            )
                                 

                                    Slider(
                                        value = masterVolume,
                                        onValueChange = {
                                            masterVolume = it
                                            viewModel.audioEngine.setVolume(it)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // AI Stem Model Selection Card (deanturpin / xeon_split_audio architecture)
                            pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("SEPARATION ENGINE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        TextButton(
                                            onClick = { showModelChooserDialog = true },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("MODELS", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = viewModel.stemSeparationModel.value,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "${stems.size} Channels Active (${stems.joinToString { it.name }})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    val models = listOf(
                                        "Spleeter (2-Stem: Vocals + Accompaniment)",
                                        "Demucs v4 (4-Stem: Vocals, Drums, Bass, Other)",
                                        "Xeon Multi-Kit (6-Stem: Vocals, Drums, Bass, Piano, Guitar, FX)"
                                    )
                                    models.forEach { m ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .clickable { viewModel.selectModel(m) }
                                                .background(
                                                    if (viewModel.stemSeparationModel.value == m) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else Color.Transparent
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(m.substringBefore(" ("), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                            if (viewModel.stemSeparationModel.value == m) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "QUICK STEM PRESETS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pEQFilterChip(
                                    selected = false,
                                    onClick = {
                                        stems.forEach {
                                            it.isMuted = false
                                            it.isSolo = false
                                            it.volume = 0.8f
                                            it.pan = 0f
                                            viewModel.updateStemVolume(it.name, 0.8f)
                                            viewModel.updateStemPan(it.name, 0f)
                                            viewModel.toggleStemMute(it.name)
                                        }
                                    },
                                    label = { Text("Bypass Reset", style = MaterialTheme.typography.labelSmall) }
                                )

                                pEQFilterChip(
                                    selected = stems.firstOrNull()?.isSolo == true,
                                    onClick = {
                                        stems.forEachIndexed { idx, it ->
                                            it.isSolo = (idx == 0)
                                            it.isMuted = (idx != 0)
                                            viewModel.toggleStemSolo(it.name, idx == 0)
                                        }
                                    },
                                    label = { Text("Acappella Solo", style = MaterialTheme.typography.labelSmall) }
                                )

                                pEQFilterChip(
                                    selected = stems.firstOrNull()?.isMuted == true && stems.getOrNull(1)?.isMuted == false,
                                    onClick = {
                                        stems.forEachIndexed { idx, it ->
                                            it.isMuted = (idx == 0)
                                            it.isSolo = false
                                            viewModel.audioEngine.setStemMute(it.name, idx == 0)
                                        }
                                    },
                                    label = { Text("Karaoke Mode", style = MaterialTheme.typography.labelSmall) }
                                )

                                pEQFilterChip(
                                    selected = stems.getOrNull(1)?.isSolo == true && stems.getOrNull(2)?.isSolo == true,
                                    onClick = {
                                        stems.forEachIndexed { idx, it ->
                                            it.isMuted = (idx != 1 && idx != 2)
                                            it.isSolo = (idx == 1 || idx == 2)
                                            viewModel.toggleStemSolo(it.name, idx == 1 || idx == 2)
                                        }
                                    },
                                    label = { Text("Rhythm Solo (Drums+Bass)", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                        // RIGHT PANEL: Multi-channel fader desk
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                items(stems) { stem ->
                                    StemFaderColumn(
                                        stem = stem,
                                        onVolumeChange = {
                                            stem.volume = it
                                            viewModel.updateStemVolume(stem.name, it)
                                        },
                                        onPanChange = {
                                            stem.pan = it
                                            viewModel.updateStemPan(stem.name, it)
                                        },
                                        onMuteToggle = {
                                            stem.isMuted = !stem.isMuted
                                            viewModel.toggleStemMute(stem.name)
                                        },
                                        onSoloToggle = {
                                            val newSolo = !stem.isSolo
                                            stem.isSolo = newSolo
                                            viewModel.toggleStemSolo(stem.name, newSolo)
                                            if (newSolo) {
                                                stems.forEach { if (it != stem) it.isMuted = true }
                                            } else {
                                                stems.forEach { it.isMuted = false }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // PORTRAIT STACKED LAYOUT
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Master Strip Summary
                        pEQGlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "MASTER BUS OUTPUT",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${(masterVolume * 100).toInt()}% • 24-bit Lossless",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                Slider(
                                    value = masterVolume,
                                    onValueChange = {
                                        masterVolume = it
                                        viewModel.audioEngine.setVolume(it)
                                    },
                                    modifier = Modifier.width(160.dp)
                                )
                            }
                        }

                        // Quick Stem Presets Bar
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                pEQFilterChip(
                                    selected = false,
                                    onClick = {
                                        stems.forEach {
                                            it.isMuted = false
                                            it.isSolo = false
                                            it.volume = 0.8f
                                            it.pan = 0f
                                            viewModel.updateStemVolume(it.name, 0.8f)
                                            viewModel.updateStemPan(it.name, 0f)
                                        }
                                    },
                                    label = { Text("Reset", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            item {
                                pEQFilterChip(
                                    selected = stems.firstOrNull()?.isSolo == true,
                                    onClick = {
                                        stems.forEachIndexed { idx, it ->
                                            it.isSolo = (idx == 0)
                                            it.isMuted = (idx != 0)
                                            viewModel.toggleStemSolo(it.name, idx == 0)
                                        }
                                    },
                                    label = { Text("Acappella", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            item {
                                pEQFilterChip(
                                    selected = stems.firstOrNull()?.isMuted == true,
                                    onClick = {
                                        stems.forEachIndexed { idx, it ->
                                            it.isMuted = (idx == 0)
                                            it.isSolo = false
                                            viewModel.audioEngine.setStemMute(it.name, idx == 0)
                                        }
                                    },
                                    label = { Text("Karaoke", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            item {
                                pEQFilterChip(
                                    selected = stems.getOrNull(1)?.isSolo == true && stems.getOrNull(2)?.isSolo == true,
                                    onClick = {
                                        stems.forEachIndexed { idx, it ->
                                            it.isMuted = (idx != 1 && idx != 2)
                                            it.isSolo = (idx == 1 || idx == 2)
                                            viewModel.toggleStemSolo(it.name, idx == 1 || idx == 2)
                                        }
                                    },
                                    label = { Text("Rhythm Solo", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                      LazyRow(
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    // Centers the items horizontally inside the LazyRow viewport
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    contentPadding = PaddingValues(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    items(stems) { stem ->
        StemFaderColumn(
            stem = stem,
            onVolumeChange = {
                stem.volume = it
                viewModel.updateStemVolume(stem.name, it)
            },
            onPanChange = {
                stem.pan = it
                viewModel.updateStemPan(stem.name, it)
            },
            onMuteToggle = {
                stem.isMuted = !stem.isMuted
                viewModel.toggleStemMute(stem.name)
            },
            onSoloToggle = {
                val newSolo = !stem.isSolo
                stem.isSolo = newSolo
                viewModel.toggleStemSolo(stem.name, newSolo)
                if (newSolo) {
                    stems.forEach { if (it != stem) it.isMuted = true }
                } else {
                    stems.forEach { it.isMuted = false }
                }
            }
        )
    }
}
                    }
                }
            }
        }
    }

    // TRACK SELECTION DIALOG
    if (showTrackSelectDialog) {
        AlertDialog(
            onDismissRequest = { showTrackSelectDialog = false },
            title = { Text("Select Song for Stem Separation", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.playbackQueue.forEach { item ->
                        Surface(
                            onClick = {
                                viewModel.selectTrackForMixer(item)
                                showTrackSelectDialog = false
                            },
                            shape = MaterialTheme.shapes.small,
                            color = if (viewModel.selectedMixerItem.value?.path == item.path)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(item.artist ?: "Unknown Artist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                if (viewModel.selectedMixerItem.value?.path == item.path) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTrackSelectDialog = false
                    viewModel.onPickTrackRequested?.invoke { item ->
                        viewModel.selectTrackForMixer(item)
                    }
                }) {
                    Text("Pick External File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrackSelectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADVANCED EXPORT STEMS DIALOG (xeon_split_audio & deanturpin inspired)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Stem Export Settings", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("FORMAT & BIT DEPTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WAV", "FLAC", "MP3").forEach { fmt ->
                            pEQFilterChip(
                                selected = viewModel.stemExportFormat.value == fmt,
                                onClick = { viewModel.stemExportFormat.value = fmt },
                                label = { Text(fmt, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("16-bit", "24-bit", "32-bit Float").forEach { bit ->
                            pEQFilterChip(
                                selected = viewModel.stemExportBitDepth.value == bit,
                                onClick = { viewModel.stemExportBitDepth.value = bit },
                                label = { Text(bit, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("SAMPLE RATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(44100 to "44.1 kHz", 48000 to "48 kHz", 96000 to "96 kHz").forEach { (sr, label) ->
                            pEQFilterChip(
                                selected = viewModel.stemExportSampleRate.value == sr,
                                onClick = { viewModel.stemExportSampleRate.value = sr },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("AI DSP ENHANCEMENTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vocal De-Reverb / De-Echo", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = viewModel.isStemDeReverbEnabled.value,
                            onCheckedChange = { viewModel.isStemDeReverbEnabled.value = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AI De-Noise / Hiss Suppression", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = viewModel.isStemDeNoiseEnabled.value,
                            onCheckedChange = { viewModel.isStemDeNoiseEnabled.value = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        viewModel.exportCurrentStems()
                    }
                ) {
                    Text("START EXPORT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // AI MODEL CHOOSER & DOWNLOADER MODAL (deanturpin & sherpa-onnx repositories)
    if (showModelChooserDialog) {
        AlertDialog(
            onDismissRequest = { showModelChooserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("AI Separation Models", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("PRE-LOADED ASSET MODELS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    
                    Surface(
                        onClick = {
                            viewModel.selectModel("Spleeter (2-Stem: Vocals + Accompaniment)")
                            showModelChooserDialog = false
                        },
                        shape = MaterialTheme.shapes.small,
                        color = if (viewModel.stemSeparationModel.value.contains("Spleeter")) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Spleeter 2-Stem (Bundled ONNX)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("spleeter_vocals.onnx + spleeter_accompaniment.onnx (19.6 MB)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            if (viewModel.stemSeparationModel.value.contains("Spleeter")) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Text("IMPORT CUSTOM ONNX MODEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = {
                            showModelChooserDialog = false
                            viewModel.onPickModelRequested?.invoke { path, name ->
                                viewModel.importCustomModel(path, name)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse & Import .onnx File")
                    }

                    if (viewModel.customOnnxModelName.value != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Active: ${viewModel.customOnnxModelName.value}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Text("DOWNLOAD COMMUNITY MODELS (GITHUB)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                    val communityModels = listOf(
                        Triple(
                            "Demucs v4 (htdemucs_quantized.onnx)",
                            "https://github.com/deanturpin/stems/raw/main/models/htdemucs_quantized.onnx",
                            "htdemucs_quantized.onnx"
                        ),
                        Triple(
                            "Sherpa Spleeter FP16 (vocals.fp16.onnx)",
                            "https://github.com/k2-fsa/sherpa-onnx/releases/download/source-separation-models/vocals.fp16.onnx",
                            "vocals.fp16.onnx"
                        ),
                        Triple(
                            "Sherpa Spleeter FP16 (accompaniment.fp16.onnx)",
                            "https://github.com/k2-fsa/sherpa-onnx/releases/download/source-separation-models/accompaniment.fp16.onnx",
                            "accompaniment.fp16.onnx"
                        )
                    )

                    communityModels.forEach { (title, url, targetName) ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Official GitHub Model Release", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                FilledTonalIconButton(
                                    onClick = {
                                        showModelChooserDialog = false
                                        viewModel.downloadModelFromUrl(url, targetName)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Download", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelChooserDialog = false }) {
                    Text("CLOSE")
                }
            }
        )
    }
}

@Composable
fun StemFaderColumn(
    stem: AudioStemChannel,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit
) {
    pEQGlassCard(
        modifier = Modifier.width(96.dp).fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Channel Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stem.name.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                // Rotary Pan Knob
                PanKnob(
                    pan = stem.pan,
                    onPanChange = onPanChange
                )
            }

            // Solo / Mute Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onMuteToggle,
                    modifier = Modifier.weight(1f).height(28.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stem.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("M", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                Button(
                    onClick = onSoloToggle,
                    modifier = Modifier.weight(1f).height(28.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stem.isSolo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("S", style = MaterialTheme.typography.labelMedium, color = if (stem.isSolo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }

            // Vertical Fader Bar & dB Level Meter
            Row(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Peak Meter
                Column(
                    modifier = Modifier.width(6.dp).fillMaxHeight().clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val activeHeight = if (stem.isMuted) 0f else stem.volume
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(activeHeight)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            )
                    )
                }

                val currentVol by rememberUpdatedState(stem.volume)
                val currentOnVolChange by rememberUpdatedState(onVolumeChange)
                // Vertical Slider Touch Strip
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount.y / 200f
                                    currentOnVolChange((currentVol + delta).coerceIn(0f, 1f))
                                }
                            )
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Fader Level Indicator Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(stem.volume.coerceIn(0.04f, 1f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                    )
                    // Fader Physical Thumb Handle
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val availableH = maxHeight
                        val thumbY = availableH * (1f - stem.volume.coerceIn(0f, 1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .offset(y = thumbY)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f), MaterialTheme.shapes.extraSmall)
                        )
                    }
                }
            }

            // Volume Readout
            Text(
                text = "${(stem.volume * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PanKnob(
    pan: Float,
    onPanChange: (Float) -> Unit
) {
    val currentPan by rememberUpdatedState(pan)
    val currentOnPanChange by rememberUpdatedState(onPanChange)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val delta = dragAmount.x / 100f
                            currentOnPanChange((currentPan + delta).coerceIn(-1f, 1f))
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Pointer indicator line
            val angle = pan * 135f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(angle)
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp, 10.dp)
                        .align(Alignment.TopCenter)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Text(
            text = when {
                pan < -0.05f -> "L ${(pan * -100).toInt()}"
                pan > 0.05f -> "R ${(pan * 100).toInt()}"
                else -> "C"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun CompactTransportBanner(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    pEQGlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "CONCERT MASTER TRANSPORT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("BPM: 120", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("4/4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                FilledTonalIconButton(
                    onClick = {},
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun MiniMixerButton(
    text: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall,
        color = if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(26.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun MixerSectionCard(
    title: String,
    icon: ImageVector,
    info: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                if (info.isNotEmpty()) {
                    Text(info, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            content()
        }
    }
}

@Composable
fun MasterFaderSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    var masterVol by remember { mutableStateOf(0.85f) }
    var masterLimiter by remember { mutableStateOf(true) }

    MixerSectionCard(
        title = "MASTER OUTPUT BUS",
        icon = Icons.Default.VolumeUp,
        info = "24-bit / 96kHz Lossless"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Master Volume: ${(masterVol * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = masterVol,
                    onValueChange = { masterVol = it },
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor
                    )
                )
            }

            Spacer(Modifier.width(16.dp))

            MiniMixerButton(
                text = "LIMITER",
                active = masterLimiter,
                activeColor = primaryColor,
                onClick = { masterLimiter = !masterLimiter }
            )
        }
    }
}
@Composable
fun NeuralStemPipeline(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    MixerSectionCard(
        title = "NEURAL STEM ISOLATION ENGINE",
        icon = Icons.Default.Memory,
        info = "Demucs v4 High-Q Multi-Track Separation"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Vocals", "Drums", "Bass", "Other").forEach { stem ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stem,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StemConsoleSection(
    viewModel: AppViewModel,
    primaryColor: Color,
    stems: List<String> = listOf("Vocals", "Drums", "Bass", "Guitar", "Piano", "FX")
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MasterFaderSection(viewModel, primaryColor)
        NeuralStemPipeline(viewModel, primaryColor)
    }
}
