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
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

data class AudioStemChannel(
    val name: String,
    val iconName: String,
    var volume: Float = 0.8f,
    var pan: Float = 0.0f, // -1.0 (L) to +1.0 (R)
    var isMuted: Boolean = false,
    var isSolo: Boolean = false
)

@Composable
fun pEQMixerScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit = {}
) {
    val stems = remember {
        mutableStateListOf(
            AudioStemChannel("Vocals", "Mic", 0.85f, 0.0f),
            AudioStemChannel("Drums", "Radio", 0.90f, 0.0f),
            AudioStemChannel("Bass", "GraphicEq", 0.80f, 0.0f),
            AudioStemChannel("Piano", "Piano", 0.75f, -0.3f),
            AudioStemChannel("Guitar", "MusicNote", 0.78f, 0.35f),
            AudioStemChannel("Other / FX", "AutoFixHigh", 0.65f, 0.1f)
        )
    }

    var masterVolume by remember { mutableStateOf(0.9f) }
    var isMasterPlaying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        Column {
                            Text(
                                text = "STEM MIXER",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• 6 Isolated AI Stems",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }


                    Button(
                        onClick = { isMasterPlaying = !isMasterPlaying },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMasterPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(if (isMasterPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isMasterPlaying) "PAUSE" else "PLAY STEMS", style = MaterialTheme.typography.labelMedium)
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

            if (isLandscapeOrWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LEFT PANEL: Master Bus output fader card, presets, headroom
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pEQGlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "MASTER BUS OUTPUT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(masterVolume * 100).toInt()}% • 0.0 dB Headroom",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Slider(
                                    value = masterVolume,
                                    onValueChange = { masterVolume = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pEQFilterChip(
                                selected = false,
                                onClick = {
                                    stems.forEach { it.isMuted = false; it.isSolo = false; it.volume = 0.8f; it.pan = 0f }
                                },
                                label = { Text("Bypass Reset", style = MaterialTheme.typography.labelSmall) }
                            )

                            pEQFilterChip(
                                selected = stems.firstOrNull()?.isSolo == true,
                                onClick = {
                                    stems.forEachIndexed { idx, it ->
                                        it.isSolo = (idx == 0)
                                        it.isMuted = (idx != 0)
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
                                    }
                                },
                                label = { Text("Rhythm Solo", style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        Spacer(Modifier.height(40.dp))
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
                                    onVolumeChange = { stem.volume = it },
                                    onPanChange = { stem.pan = it },
                                    onMuteToggle = { stem.isMuted = !stem.isMuted },
                                    onSoloToggle = {
                                        val newSolo = !stem.isSolo
                                        stem.isSolo = newSolo
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Master Strip Summary
                    pEQGlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
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
                                    text = "${(masterVolume * 100).toInt()}% • 0.0 dB Headroom",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            Slider(
                                value = masterVolume,
                                onValueChange = { masterVolume = it },
                                modifier = Modifier.width(180.dp)
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
                                    stems.forEach { it.isMuted = false; it.isSolo = false; it.volume = 0.8f; it.pan = 0f }
                                },
                                label = { Text("Bypass Reset", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            pEQFilterChip(
                                selected = stems.firstOrNull()?.isSolo == true,
                                onClick = {
                                    stems.forEachIndexed { idx, it ->
                                        it.isSolo = (idx == 0)
                                        it.isMuted = (idx != 0)
                                    }
                                },
                                label = { Text("Acappella Solo", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            pEQFilterChip(
                                selected = stems.firstOrNull()?.isMuted == true && stems.getOrNull(1)?.isMuted == false,
                                onClick = {
                                    stems.forEachIndexed { idx, it ->
                                        it.isMuted = (idx == 0) // Mute Vocals for Karaoke
                                        it.isSolo = false
                                    }
                                },
                                label = { Text("Karaoke Mode", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            pEQFilterChip(
                                selected = stems.getOrNull(1)?.isSolo == true && stems.getOrNull(2)?.isSolo == true,
                                onClick = {
                                    stems.forEachIndexed { idx, it ->
                                        // Solo Drums & Bass
                                        it.isMuted = (idx != 1 && idx != 2)
                                        it.isSolo = (idx == 1 || idx == 2)
                                    }
                                },
                                label = { Text("Rhythm Solo", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Stems Console Channels (Horizontal Scrollable Desk)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(stems) { stem ->
                            StemFaderColumn(
                                stem = stem,
                                onVolumeChange = { stem.volume = it },
                                onPanChange = { stem.pan = it },
                                onMuteToggle = { stem.isMuted = !stem.isMuted },
                                onSoloToggle = {
                                    val newSolo = !stem.isSolo
                                    stem.isSolo = newSolo
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

                // Vertical Slider Touch Strip
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount.y / 200f
                                onVolumeChange((stem.volume + delta).coerceIn(0f, 1f))
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Fader Thumb
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(stem.volume.coerceIn(0.05f, 1f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .offset(y = (-(stem.volume * 160)).dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.primary)
                    )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x / 100f
                        onPanChange((pan + delta).coerceIn(-1f, 1f))
                    }
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
                        .size(3.dp, 8.dp)
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
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stem, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = primaryColor, fontWeight = FontWeight.Black)
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
