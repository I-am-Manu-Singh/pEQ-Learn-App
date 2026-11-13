package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.DrumTutorial
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.SongBar
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeActive
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeBpm
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.setMetronomeBpm
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.switchDrumKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LearnSectionCard(
    title: String,
    icon: ImageVector,
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        if (info != null) {
                            Text(text = info, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            content()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumAcademySection(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val padLabels = remember {
        listOf(
            "0-KICK", "1-SNARE", "2-HH_CLOSED", "3-HH_OPEN",
            "4-TOM_HI", "5-TOM_MID", "6-FLOOR", "7-CRASH",
            "8-RIDE", "9-CLAP", "10-PERC", "11-SHAKER",
            "12-KICK2", "13-SNARE2", "14-CRASH2", "15-FX"
        )
    }
    val activeIndices = remember { mutableStateListOf<Int>() }
    var drumViewMode by remember { mutableIntStateOf(0) } // 0 = 16-Pad MPC, 1 = 3D Drum Studio, 2 = Drum Kits

    LearnSectionCard(
        title = "DRUM ACADEMY, SEQUENCER & METRONOME STUDIO",
        icon = Icons.Filled.Adjust,
        info = "Kit: ${viewModel.selectedDrumKit.value?.name ?: viewModel.audioEngine.currentSelectedKitName} • ${viewModel.liveTutorialBpm.intValue} BPM"
    ) {
        // Top Bar: Pattern Selector + Play/Stop Pattern + Import MIDI + Export MIDI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var dropdownExpanded by remember { mutableStateOf(false) }
            val currentSelectedTutorial = viewModel.selectedTutorial.value

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = currentSelectedTutorial?.name?.uppercase() ?: "SELECT DRUM PATTERN",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    textStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    viewModel.drumTutorials.forEach { pattern ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = pattern.name.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = if (currentSelectedTutorial == pattern) FontWeight.Black else FontWeight.Medium,
                                    color = if (currentSelectedTutorial == pattern) primaryColor else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                viewModel.startTutorial(pattern)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Play / Stop Selected Pattern Button
            IconButton(
                onClick = {
                    if (viewModel.isPatternLooping.value) {
                        viewModel.stopPatternPlayback()
                    } else {
                        currentSelectedTutorial?.let { viewModel.playPattern(it) }
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (viewModel.isPatternLooping.value) Color(0xFFFF4D6D).copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = if (viewModel.isPatternLooping.value) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Play/Stop Pattern",
                    tint = if (viewModel.isPatternLooping.value) Color(0xFFFF4D6D) else primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Import MIDI Score into Drum Sequencer
            OutlinedButton(
                onClick = {
                    if (viewModel.currentScore.value != null) {
                        viewModel.importCurrentScoreIntoDrumSequencer()
                    } else {
                        viewModel.onPickScoreFileRequested?.invoke()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = "Import MIDI", modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Import MIDI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Export Recorded Groove / Arrangement to MIDI & Score
            OutlinedButton(
                onClick = { viewModel.exportDrumCompositionAsMidi() },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Export MIDI", modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export MIDI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Workstation Section: Mechanical Metronome + Fully Wired 16-Step Drum Sequencer
        RhythmGrooveWorkstation(
            viewModel = viewModel,
            primaryColor = primaryColor
        )

        // Switcher between 16-Pad MPC Grid, 3D Interactive Drum Kit, & Kit Selector
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pEQFilterChip(
                        selected = drumViewMode == 0,
                        onClick = { drumViewMode = 0 },
                        label = { Text("16-Pad MPC Grid", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    pEQFilterChip(
                        selected = drumViewMode == 1,
                        onClick = { drumViewMode = 1 },
                        label = { Text("3D Drum Kit Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    pEQFilterChip(
                        selected = drumViewMode == 2,
                        onClick = { drumViewMode = 2 },
                        label = { Text("Drum Kits (${viewModel.drumKits.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (drumViewMode) {
                0 -> {
                    val effectiveLabels = if (viewModel.drumPadLabels.size >= 16) viewModel.drumPadLabels else padLabels
                    DrumPadKitGrid(
                        viewModel = viewModel,
                        labels = effectiveLabels,
                        activeIndices = activeIndices,
                        guidedIndices = emptySet(),
                        onPadClick = { index ->
                            activeIndices.clear()
                            activeIndices.add(index)
                            val lbl = effectiveLabels.getOrNull(index) ?: padLabels.getOrNull(index) ?: "0-KICK"
                            val cleanShort = lbl.substringAfter("-")
                            viewModel.audioEngine.playSample(cleanShort, index, "DRUM")
                            scope.launch {
                                viewModel.sequencerTriggeredPads[lbl] = true
                                viewModel.sequencerTriggeredPads[cleanShort] = true
                                viewModel.triggerThreeDDrumVisualizer(cleanShort)
                                delay(110L)
                                viewModel.sequencerTriggeredPads[lbl] = false
                                viewModel.sequencerTriggeredPads[cleanShort] = false
                            }
                        },
                        onPadLongClick = { index ->
                            viewModel.editingPadIndex.value = index
                            viewModel.isKitBuilderActive.value = true
                        }
                    )
                }
                1 -> {
                    com.creativeblisslabs.peqlearn.app.ui.components.ThreeDDrumVisualizer(
                        viewModel = viewModel,
                        primaryColor = primaryColor,
                        currentKitLabels = if (viewModel.drumPadLabels.isNotEmpty()) viewModel.drumPadLabels else padLabels,
                        showOnlyPartLabel = true, // Set to true for raw labels ("SNARE", "KICK"), or false for full names ("MAIN SNARE", "BASS DRUM 1")
                        modifier = Modifier.fillMaxWidth().height(360.dp)
                    )
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currentKitName = viewModel.selectedDrumKit.value?.name ?: viewModel.audioEngine.currentSelectedKitName
                        viewModel.drumKits.forEach { kit ->
                            val isSelected = kit.name == currentKitName || kit.id == currentKitName
                            pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(kit.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${kit.labels.size} Studio Samples Ready", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.switchDrumKit(kit)
                                                viewModel.audioEngine.currentSelectedKitName = kit.id
                                                viewModel.audioEngine.playSample("KICK", 0, "DRUM")
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Audition", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.switchDrumKit(kit)
                                                viewModel.audioEngine.currentSelectedKitName = kit.id
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Text(if (isSelected) "ACTIVE KIT" else "LOAD KIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Multi-Bar Song Arrangement Timeline
            SongArrangerTimeline(viewModel = viewModel, primaryColor = primaryColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutorialAndPlaybackControl(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentSelectedTutorial = viewModel.selectedTutorial.value

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
            modifier = Modifier.weight(1.2f)
        ) {
            OutlinedTextField(
                value = currentSelectedTutorial?.name?.uppercase() ?: "SELECT TRAINING PATTERN",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                textStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                viewModel.drumTutorials.forEach { pattern ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = pattern.name.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSelectedTutorial == pattern) primaryColor else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            viewModel.startTutorial(pattern)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        IconButton(
            onClick = {
                if (viewModel.isPatternLooping.value) {
                    viewModel.stopPatternPlayback()
                } else {
                    currentSelectedTutorial?.let { viewModel.playPattern(it) }
                }
            },
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (viewModel.isPatternLooping.value) Color(0xFFFF4D6D).copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = if (viewModel.isPatternLooping.value) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = "Toggle Loop Engine",
                tint = if (viewModel.isPatternLooping.value) Color(0xFFFF4D6D) else primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TempoControlRow(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SEQUENCE TEMPO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(
                    text = "${viewModel.liveTutorialBpm.intValue} BPM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor
                )
            }
            Slider(
                value = viewModel.liveTutorialBpm.intValue.toFloat(),
                onValueChange = { viewModel.liveTutorialBpm.intValue = it.toInt() },
                valueRange = 60f..200f,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = { viewModel.handleTapTempo() },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.height(2.dp))
                Text("TAP", fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun DrumWorkspaceSelector(
    drumViewMode: Int,
    onViewModeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pEQFilterChip(
            selected = drumViewMode == 0,
            onClick = { onViewModeChange(0) },
            label = { Text("16-Pad MPC Grid", fontSize = 11.sp) }
        )
        pEQFilterChip(
            selected = drumViewMode == 1,
            onClick = { onViewModeChange(1) },
            label = { Text("3D Visualizer", fontSize = 11.sp) }
        )
        pEQFilterChip(
            selected = drumViewMode == 2,
            onClick = { onViewModeChange(2) },
            label = { Text("Drum Kits", fontSize = 11.sp) }
        )
    }
}

@Composable
private fun DrumWorkspaceContent(
    drumViewMode: Int,
    viewModel: AppViewModel,
    primaryColor: Color,
    padLabels: List<String>,
    activeIndices: MutableList<Int>
) {
    when (drumViewMode) {
        0 -> {
            DrumPadKitGrid(
                viewModel = viewModel,
                labels = if (viewModel.drumPadLabels.isNotEmpty()) viewModel.drumPadLabels else padLabels,
                activeIndices = activeIndices,
                guidedIndices = setOf(0, 4, 8, 12),
                onPadClick = { index ->
                    activeIndices.clear()
                    activeIndices.add(index)
                    val lbl = viewModel.drumPadLabels.getOrNull(index) ?: padLabels.getOrNull(index) ?: "KICK"
                    viewModel.audioEngine.playSample(lbl, index, "DRUM")
                },
                onPadLongClick = { _ -> }
            )
        }
        1 -> {
            com.creativeblisslabs.peqlearn.app.ui.components.ThreeDDrumVisualizer(
                viewModel = viewModel,
                primaryColor = primaryColor,
                currentKitLabels = if (viewModel.drumPadLabels.isNotEmpty()) viewModel.drumPadLabels else padLabels,
                showOnlyPartLabel = true, // Set to true for raw labels ("SNARE", "KICK"), or false for full names ("MAIN SNARE", "BASS DRUM 1")
                modifier = Modifier.fillMaxWidth().height(360.dp)
            )
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentKitName = viewModel.selectedDrumKit.value?.name ?: ""
                viewModel.drumKits.forEach { kit ->
                    val isSelected = kit.name == currentKitName
                    pEQGlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(kit.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${kit.labels.size} Samples Preloaded", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Button(
                                onClick = {
                                    viewModel.switchDrumKit(kit)
                                    viewModel.audioEngine.currentSelectedKitName = kit.id
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(if (isSelected) "ACTIVE" else "LOAD", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrumPadItem(
    label: String,
    index: Int,
    primaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isGuided: Boolean = false,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cleanLabel = remember(label) { label.substringAfter("-").uppercase() }

    val padColor = remember(cleanLabel) {
        when {
            cleanLabel.contains("KICK") -> Color(0xFFFF4D6D)
            cleanLabel.contains("SNARE") -> Color(0xFFFFC107)
            cleanLabel.contains("HAT") -> Color(0xFF00D4FF)
            cleanLabel.contains("CLAP") -> Color(0xFFB388FF)
            cleanLabel.contains("BASS") || cleanLabel.contains("808") -> Color(0xFF536DFE)
            else -> Color(0xFF4ADE80)
        }
    }

    val padIcon = remember(cleanLabel) {
        when {
            cleanLabel.contains("KICK") -> Icons.Default.RadioButtonChecked
            cleanLabel.contains("SNARE") -> Icons.Default.GraphicEq
            cleanLabel.contains("HAT") -> Icons.Default.AutoAwesome
            cleanLabel.contains("CLAP") -> Icons.Default.Waves
            cleanLabel.contains("BASS") -> Icons.Default.Speaker
            else -> Icons.Default.MusicNote
        }
    }

    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when { pressed -> 0.92f; isActive -> 1.03f; else -> 1f },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pad_scale"
    )

    pEQGlassCard(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = { onLongClick() },
                onClick = {}
            )
            .pointerInput(label) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()

                        if (change != null && change.changedToDown()) {
                            pressed = true
                            onClick()

                            scope.launch {
                                delay(60)
                                pressed = false
                            }
                        }
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (pressed || isGuided || isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(colors = listOf(padColor.copy(alpha = 0.12f), Color.Transparent)))
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(4.dp), color = padColor.copy(alpha = 0.12f)) {
                        Text(
                            text = (index + 1).toString().padStart(2, '0'),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = padColor
                        )
                    }
                    Icon(
                        imageVector = padIcon,
                        contentDescription = null,
                        tint = if (pressed || isActive) padColor else padColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayLabel = remember(label) {
                        label.substringAfter("-").replace("_", " ").replace("-", " ").uppercase()
                    }
                    Text(
                        text = displayLabel,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(padColor.copy(alpha = if (pressed) 1f else 0.40f))
                    )
                }
            }
        }
    }
}
@Composable
fun DrumPadKitGrid(
    viewModel: AppViewModel,
    labels: List<String>,
    activeIndices: List<Int>,
    guidedIndices: Set<Int>,
    onPadClick: (Int) -> Unit,
    onPadLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        com.creativeblisslabs.peqlearn.app.ui.components.FullscreenDialog(
            onDismissRequest = { isFullscreen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0B0D14)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FULLSCREEN 16-PAD MPC DRUM RACK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { isFullscreen = false }) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val availH = maxHeight - 12.dp
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth().height(availH),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false
                        ) {
                            gridItemsIndexed(labels) { index, label ->
                                val upperLbl = label.uppercase()
                                val isTriggered = viewModel.sequencerTriggeredPads[label] == true ||
                                        viewModel.sequencerTriggeredPads["$index-$upperLbl"] == true ||
                                        viewModel.sequencerTriggeredPads.entries.any { (k, v) ->
                                            v && (
                                                    k.equals(label, ignoreCase = true) ||
                                                            (upperLbl.contains("KICK") && (k.contains("KICK", true) || k == "KCK")) ||
                                                            (upperLbl.contains("SNARE") && (k.contains("SNARE", true) || k == "SNR"))
                                                    )
                                        }
                                val isActive = activeIndices.contains(index) || isTriggered
                                DrumPadItem(
                                    label = label,
                                    index = index,
                                    primaryColor = MaterialTheme.colorScheme.primary,
                                    isActive = isActive,
                                    isGuided = guidedIndices.contains(index) || isTriggered,
                                    onClick = { onPadClick(index) },
                                    onLongClick = { onPadLongClick(index) },
                                    modifier = Modifier.height(availH / 4.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isLandscape = maxWidth > maxHeight || maxHeight < 520.dp
        val gridHeight = if (isLandscape) (maxHeight - 40.dp).coerceIn(190.dp, 320.dp) else 420.dp

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrumRackLabel("MPC CORE BANK A (4x4)", MaterialTheme.colorScheme.primary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnalysisPill("VELOCITY", "127", MaterialTheme.colorScheme.primary)
                    AnalysisPill("LATENCY", "4ms", Color(0xFF00E676))
                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen Pads",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            SpectrumBalanceBar(
                low = 0.6f,
                mid = 0.4f,
                high = 0.3f,
                primaryColor = MaterialTheme.colorScheme.primary
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(gridHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
            gridItemsIndexed(labels) { index, label ->
                val upperLbl = label.uppercase()
                val isTriggered = viewModel.sequencerTriggeredPads[label] == true ||
                        viewModel.sequencerTriggeredPads["$index-$upperLbl"] == true ||
                        viewModel.sequencerTriggeredPads.entries.any { (k, v) ->
                            v && (
                                    k.equals(label, ignoreCase = true) ||
                                            (upperLbl.contains("KICK") && (k.contains("KICK", true) || k == "KCK")) ||
                                            (upperLbl.contains("SNARE") && (k.contains("SNARE", true) || k == "SNR")) ||
                                            (upperLbl.contains("CLOSED") && (k.contains("CLOSED", true) || k == "HHC")) ||
                                            (upperLbl.contains("OPEN") && (k.contains("OPEN", true) || k == "OHH")) ||
                                            (upperLbl.contains("CLAP") && (k.contains("CLAP", true) || k == "CLP")) ||
                                            (upperLbl.contains("CRASH") && k.contains("CRASH", true)) ||
                                            (upperLbl.contains("RIDE") && (k.contains("RIDE", true) || k == "RID")) ||
                                            (upperLbl.contains("TOM") && k.contains("TOM", true))
                                    )
                        }
                val isActive = activeIndices.contains(index) || isTriggered

                DrumPadItem(
                    label = label,
                    index = index,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    isActive = isActive,
                    isGuided = guidedIndices.contains(index) || isTriggered,
                    onClick = { onPadClick(index) },
                    onLongClick = { onPadLongClick(index) }
                )
            }
        }
    }
}
}
enum class GrooveStudioTab(val label: String, val icon: ImageVector) {
    STEP_SEQUENCER("16-Step Drum Grid", Icons.Default.GridView),
    METRONOME("Mechanical Metronome", Icons.Default.Speed)
}

@Composable
fun RhythmGrooveWorkstation(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    var selectedTab by remember { mutableStateOf(GrooveStudioTab.STEP_SEQUENCER) }
    val bpm by viewModel.metronomeBpm.collectAsState()
    var isCustomSequencerPlaying by remember { mutableStateOf(false) }
    var isLivePadRecording by remember { mutableStateOf(false) }
    var localSeqStep by remember { mutableIntStateOf(0) }
    var customPatternCounter by remember { mutableIntStateOf(1) }

    // 8 Core Drum Voices mapped directly to DrumKitLoader / 3D Visualizer labels
    val drumTracks = remember {
        listOf(
            Triple("KICK", "0-KICK", 0),
            Triple("SNARE", "1-SNARE", 1),
            Triple("CL HAT", "2-HH_CLOSED", 2),
            Triple("OP HAT", "3-HH_OPEN", 3),
            Triple("HI TOM", "4-TOM_HI", 4),
            Triple("FL TOM", "6-FLOOR", 6),
            Triple("CRASH", "7-CRASH", 7),
            Triple("CLAP", "9-CLAP", 9)
        )
    }

    val gridState = remember {
        mutableStateMapOf<String, MutableList<Boolean>>().apply {
            drumTracks.forEach { (trackKey, _, _) ->
                this[trackKey] = mutableStateListOf<Boolean>().apply {
                    for (i in 0 until 16) {
                        add(
                            when (trackKey) {
                                "KICK" -> i == 0 || i == 8 || i == 10
                                "SNARE" -> i == 4 || i == 12
                                "CL HAT" -> i % 2 == 0
                                else -> false
                            }
                        )
                    }
                }
            }
        }
    }

    // Two-Way Sync: Whenever user selects a pattern in the top dropdown (or clicks a bar in the timeline),
    // automatically load that pattern's 16 steps into the 16-Step Grid!
    val activeTutorial = viewModel.selectedTutorial.value
    LaunchedEffect(activeTutorial) {
        if (activeTutorial != null && activeTutorial.steps.isNotEmpty()) {
            drumTracks.forEach { (trackKey, _, _) ->
                val list = gridState[trackKey] ?: return@forEach
                for (s in 0 until 16) list[s] = false
            }
            activeTutorial.steps.forEachIndexed { stepIdx, hits ->
                if (stepIdx !in 0..15) return@forEachIndexed
                hits.forEach { rawHit ->
                    val up = rawHit.uppercase()
                    val matchedKey = when {
                        up.contains("KICK") || up == "KCK" -> "KICK"
                        up.contains("SNARE") || up == "SNR" -> "SNARE"
                        up.contains("OPEN") || up == "OHH" || up.contains("OHAT") -> "OP HAT"
                        up.contains("CLOSED") || up.contains("HAT") || up == "HHC" || up == "HH" -> "CL HAT"
                        up.contains("FLOOR") || up.contains("TOM_LO") -> "FL TOM"
                        up.contains("TOM") -> "HI TOM"
                        up.contains("CRASH") || up.contains("RIDE") -> "CRASH"
                        up.contains("CLAP") || up == "CLP" || up.contains("PERC") -> "CLAP"
                        else -> null
                    }
                    if (matchedKey != null) {
                        gridState[matchedKey]?.set(stepIdx, true)
                    }
                }
            }
        }
    }

    // Helper to push any manual grid edits back into `viewModel.selectedTutorial` so Pattern Play & Timeline Play stay 100% synced!
    fun syncGridToActiveTutorial() {
        val stepsList = List(16) { stepIdx ->
            val hits = mutableListOf<String>()
            drumTracks.forEach { (trackKey, fullLabel, _) ->
                if (gridState[trackKey]?.getOrNull(stepIdx) == true) {
                    hits.add(fullLabel)
                }
            }
            hits.toList()
        }
        val curName = viewModel.selectedTutorial.value?.name ?: "${viewModel.audioEngine.currentSelectedKitName}: Live Groove"
        val updated = DrumTutorial(name = curName, steps = stepsList)
        viewModel.selectedTutorial.value = updated
        val existingIdx = viewModel.drumTutorials.indexOfFirst { it.name == curName }
        if (existingIdx >= 0) {
            viewModel.drumTutorials[existingIdx] = updated
        } else {
            viewModel.drumTutorials.add(0, updated)
        }
    }

    // Live Pad Recording: when REC is armed and user taps any MPC pad or 3D drum, quantize hit into current step!
    val triggeredSnapshot = viewModel.sequencerTriggeredPads.entries.filter { it.value }.map { it.key }
    LaunchedEffect(triggeredSnapshot, isLivePadRecording) {
        if (isLivePadRecording && (isCustomSequencerPlaying || viewModel.isPatternLooping.value || viewModel.isSongPlaybackActive.value)) {
            val step = if (isCustomSequencerPlaying) localSeqStep else viewModel.currentStepIndex.intValue.coerceIn(0, 15)
            var changed = false
            triggeredSnapshot.forEach { rawKey ->
                val up = rawKey.uppercase()
                val matchedTrack = when {
                    up.contains("KICK") -> "KICK"
                    up.contains("SNARE") -> "SNARE"
                    up.contains("OPEN") || up.contains("OHAT") -> "OP HAT"
                    up.contains("CLOSED") || up.contains("HAT") || up == "HH" -> "CL HAT"
                    up.contains("FLOOR") -> "FL TOM"
                    up.contains("TOM") -> "HI TOM"
                    up.contains("CRASH") || up.contains("RIDE") -> "CRASH"
                    up.contains("CLAP") || up.contains("PERC") -> "CLAP"
                    else -> null
                }
                if (matchedTrack != null && gridState[matchedTrack]?.getOrNull(step) == false) {
                    gridState[matchedTrack]?.set(step, true)
                    changed = true
                }
            }
            if (changed) syncGridToActiveTutorial()
        }
    }

    // Dedicated 16-Step Sequencer Playback Engine (Triggers AudioEngine + Flashes 16-Pad Grid + Animates 3D Drum Kit)
    LaunchedEffect(isCustomSequencerPlaying, bpm) {
        if (!isCustomSequencerPlaying) {
            viewModel.sequencerTriggeredPads.clear()
            return@LaunchedEffect
        }
        while (isCustomSequencerPlaying) {
            val safeBpm = bpm.coerceIn(30, 280)
            val stepDurationMs = (15_000L / safeBpm).coerceAtLeast(40L)
            val step = localSeqStep % 16
            viewModel.currentStepIndex.intValue = step

            viewModel.sequencerTriggeredPads.clear()
            drumTracks.forEach { (trackKey, fullLabel, padIdx) ->
                if (gridState[trackKey]?.getOrNull(step) == true) {
                    val shortLbl = fullLabel.substringAfter("-")
                    viewModel.audioEngine.playSample(label = shortLbl, index = padIdx, string = "DRUM")
                    viewModel.sequencerTriggeredPads[fullLabel] = true
                    viewModel.sequencerTriggeredPads[shortLbl] = true
                    viewModel.triggerThreeDDrumVisualizer(shortLbl)
                }
            }

            delay(stepDurationMs)
            localSeqStep = (localSeqStep + 1) % 16
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Tab Switcher: 16-Step Drum Grid | Vintage Mechanical Metronome
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GrooveStudioTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    pEQFilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        leadingIcon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (selectedTab == GrooveStudioTab.STEP_SEQUENCER) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { isLivePadRecording = !isLivePadRecording },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isLivePadRecording) Color(0xFFFF1744) else MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isLivePadRecording) Color(0xFFFF1744).copy(alpha = 0.18f) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isLivePadRecording) Color(0xFFFF1744) else Color.Gray)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isLivePadRecording) "REC ON" else "REC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLivePadRecording) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            if (!isCustomSequencerPlaying) {
                                viewModel.stopPatternPlayback()
                                viewModel.stopSongPlayback()
                                localSeqStep = 0
                            }
                            isCustomSequencerPlaying = !isCustomSequencerPlaying
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCustomSequencerPlaying) Color(0xFFFF4D6D) else primaryColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isCustomSequencerPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isCustomSequencerPlaying) "STOP GRID" else "PLAY GRID", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        when (selectedTab) {
            GrooveStudioTab.METRONOME -> {
                VintageMechanicalMetronomeStudio(viewModel = viewModel)
            }

            GrooveStudioTab.STEP_SEQUENCER -> {
                val activeStep = if (isCustomSequencerPlaying) localSeqStep else viewModel.currentStepIndex.intValue
                val anySequencerRunning = isCustomSequencerPlaying || viewModel.isPatternLooping.value || viewModel.isSongPlaybackActive.value

                pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Beat Header Row (1 e & a 2 e & a 3 e & a 4 e & a)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(64.dp))
                            val subLabels = listOf("1", "e", "&", "a", "2", "e", "&", "a", "3", "e", "&", "a", "4", "e", "&", "a")
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (i in 0 until 16) {
                                    val isCurrent = (i == activeStep && anySequencerRunning)
                                    val isDownbeat = i % 4 == 0
                                    Text(
                                        text = subLabels[i],
                                        fontSize = 9.sp,
                                        fontWeight = if (isDownbeat || isCurrent) FontWeight.Black else FontWeight.Medium,
                                        color = when {
                                            isCurrent -> Color(0xFFFFEB3B)
                                            isDownbeat -> primaryColor
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // 8 Drum Track Rows x 16 Steps
                        drumTracks.forEach { (trackKey, fullLabel, padIdx) ->
                            val steps = gridState[trackKey] ?: return@forEach
                            val rowColor = when (trackKey) {
                                "KICK" -> Color(0xFFFF4D6D)
                                "SNARE" -> Color(0xFFFFC107)
                                "CL HAT", "OP HAT" -> Color(0xFF00D4FF)
                                "HI TOM", "FL TOM" -> Color(0xFF4ADE80)
                                "CRASH" -> Color(0xFFFF9800)
                                else -> Color(0xFFB388FF)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trackKey,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = rowColor,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .clickable {
                                            val shortLbl = fullLabel.substringAfter("-")
                                            viewModel.audioEngine.playSample(shortLbl, padIdx, "DRUM")
                                            viewModel.triggerThreeDDrumVisualizer(shortLbl)
                                        }
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (i in 0 until 16) {
                                        val isActive = steps.getOrNull(i) == true
                                        val isCurrentStep = (i == activeStep && anySequencerRunning)
                                        val isDownbeat = i % 4 == 0

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(
                                                    when {
                                                        isCurrentStep && isActive -> Color.White
                                                        isActive -> rowColor
                                                        isCurrentStep -> primaryColor.copy(alpha = 0.38f)
                                                        isDownbeat -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                                    }
                                                )
                                                .border(
                                                    width = if (isCurrentStep) 1.5.dp else 0.8.dp,
                                                    color = when {
                                                        isCurrentStep -> Color(0xFFFFEB3B)
                                                        isActive -> Color.White.copy(alpha = 0.6f)
                                                        else -> Color.White.copy(alpha = 0.08f)
                                                    },
                                                    shape = RoundedCornerShape(5.dp)
                                                )
                                                .clickable {
                                                    val nextVal = !steps[i]
                                                    steps[i] = nextVal
                                                    syncGridToActiveTutorial()
                                                    if (nextVal) {
                                                        val shortLbl = fullLabel.substringAfter("-")
                                                        viewModel.audioEngine.playSample(shortLbl, padIdx, "DRUM")
                                                        viewModel.triggerThreeDDrumVisualizer(shortLbl)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Sequencer Footer Controls: Clear Grid, Save as New Pattern, Append to Song Timeline
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    drumTracks.forEach { (tr, _, _) ->
                                        val list = gridState[tr] ?: return@forEach
                                        for (i in 0 until 16) list[i] = false
                                    }
                                    syncGridToActiveTutorial()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.28f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val stepsList = List(16) { stepIdx ->
                                        val hitsAtStep = mutableListOf<String>()
                                        drumTracks.forEach { (tr, fullLbl, _) ->
                                            if (gridState[tr]?.getOrNull(stepIdx) == true) {
                                                hitsAtStep.add(fullLbl)
                                            }
                                        }
                                        hitsAtStep.toList()
                                    }
                                    val customName = "${viewModel.audioEngine.currentSelectedKitName}: Custom Groove #${customPatternCounter++}"
                                    val newTutorial = DrumTutorial(name = customName, steps = stepsList)
                                    viewModel.drumTutorials.add(0, newTutorial)
                                    viewModel.selectedTutorial.value = newTutorial
                                    viewModel.addBarToTimeline(patternName = customName, isFiller = false)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                modifier = Modifier.weight(0.72f)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SAVE & APPEND BAR TO TIMELINE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
/**
 * VINTAGE MECHANICAL PYRAMID METRONOME STUDIO (Matches Reference Screenshot 100%):
 * - Left: Wittner-style Mahogany Pyramid Casing, Cream Tempo Scale (20..300 BPM with Grave, Largo,
 *   Larghetto, Adagio, Andante, Moderato, Allegro, Presto, Prestissimo), Swinging Brass Pendulum Rod
 *   with Sliding Trapezoidal Weight, Brass Winding Key, and Bottom Beat-Indicator LEDs.
 * - Right: Tempo Marking Header ("Andante at a walking pace"), [- / ♩ = 86 / +] Controls, Red Slider,
 *   [Tap] & Red [▶ Start] Buttons, Time Signature Dropdown (4/4, 3/4, 2/4, 6/8, 5/4, 7/8),
 *   Subdivide Segmented Selector [Off | 2 | 3 | 4], Sound Selector [Wood | Click | Beep | Mechanical],
 *   Volume Slider, [Visual only] Checkbox, expandable [Tempo trainer] (+N BPM every M bars),
 *   and Keyboard Shortcuts + Latency Calibration Footer.
 */
@Composable
fun VintageMechanicalMetronomeStudio(
    viewModel: AppViewModel
) {
    val isMetronomeActiveVm by viewModel.metronomeActive.collectAsState()
    var isRunning by remember { mutableStateOf(isMetronomeActiveVm) }
    val bpm by viewModel.metronomeBpm.collectAsState()
    var timeSig by remember { mutableStateOf(viewModel.selectedTimeSignature.value.ifBlank { "4/4" }) }
    var timeSigMenuExpanded by remember { mutableStateOf(false) }
    var subdivision by remember { mutableIntStateOf(viewModel.metronomeSubdivision.intValue.coerceIn(1, 4)) } // 1 = Off, 2 = 8ths, 3 = Triplets, 4 = 16ths
    var soundType by remember { mutableStateOf(viewModel.metronomeTickSound.value.ifBlank { "Mechanical" }) } // "Wood", "Click", "Beep", "Mechanical"
    var volume by remember { mutableFloatStateOf(0.85f) }
    var visualOnly by remember { mutableStateOf(false) }

    // Tempo Trainer State
    var tempoTrainerExpanded by remember { mutableStateOf(false) }
    var tempoTrainerEnabled by remember { mutableStateOf(false) }
    var stepBpmIncrement by remember { mutableIntStateOf(2) }
    var stepBarsInterval by remember { mutableIntStateOf(4) }
    var targetMaxBpm by remember { mutableIntStateOf(160) }

    // Tap Tempo History
    val tapTimestamps = remember { mutableStateListOf<Long>() }

    // Live Pendulum & Beat State
    var currentBeatInBar by remember { mutableIntStateOf(0) }
    var pendulumPhase by remember { mutableFloatStateOf(0f) } // -1f (left) .. +1f (right)

    val beatsPerBar = remember(timeSig) {
        timeSig.substringBefore("/").toIntOrNull()?.coerceIn(1, 12) ?: 4
    }

    val (tempoItalian, tempoSubtitle) = remember(bpm) {
        when {
            bpm < 40 -> "Grave" to "very slow and solemn"
            bpm < 50 -> "Largo" to "broadly and dignified"
            bpm < 60 -> "Larghetto" to "rather broadly"
            bpm < 72 -> "Adagio" to "slow and stately (at ease)"
            bpm < 96 -> "Andante" to "at a walking pace"
            bpm < 112 -> "Moderato" to "at a moderate speed"
            bpm < 156 -> "Allegro" to "fast, quickly and bright"
            bpm < 200 -> "Presto" to "very fast"
            else -> "Prestissimo" to "extremely fast, as fast as possible"
        }
    }

    // High-Precision Metronome Pendulum & Subdivision Audio Loop
    androidx.compose.runtime.LaunchedEffect(isRunning, bpm, timeSig, subdivision, soundType, volume, visualOnly, tempoTrainerEnabled) {
        if (!isRunning) {
            pendulumPhase = 0f
            currentBeatInBar = 0
            return@LaunchedEffect
        }

        viewModel.audioEngine.setMetronomeGain(if (visualOnly) 0f else volume)
        var beatCounter = 0L
        var barsCompleted = 0

        while (isRunning) {
            val activeBpm = viewModel.metronomeBpm.value.coerceIn(20, 300)
            val beatDurationSec = 60.0 / activeBpm.toDouble()
            val subCount = subdivision.coerceIn(1, 4)
            val subDurationMs = ((beatDurationSec * 1000.0) / subCount).toLong().coerceAtLeast(20L)

            val beatIdx = (beatCounter % beatsPerBar).toInt()
            currentBeatInBar = beatIdx

            for (subIdx in 0 until subCount) {
                if (!isRunning) break
                val isPrimaryBeat = (subIdx == 0)
                val isAccentDownbeat = isPrimaryBeat && (beatIdx == 0)

                if (!visualOnly && volume > 0.01f) {
                    when (soundType) {
                        "Wood" -> {
                            viewModel.audioEngine.playSample(
                                if (isAccentDownbeat) "METRONOME-STRONG" else "METRONOME-WEAK",
                                0,
                                "SYSTEM"
                            )
                        }
                        "Click" -> {
                            viewModel.audioEngine.playSample(
                                if (isAccentDownbeat) "SNARE" else "HH_CLOSED",
                                if (isAccentDownbeat) 13 else 2,
                                "DRUM"
                            )
                        }
                        "Beep" -> {
                            val pitch = if (isAccentDownbeat) 84 else if (isPrimaryBeat) 76 else 72
                            viewModel.audioEngine.noteOn(pitch, (volume * 0.55f).coerceIn(0.05f, 1f))
                        }
                        else -> { // "Mechanical" Escapement Tick-Tock
                            viewModel.audioEngine.playSample(
                                if (isAccentDownbeat) "METRONOME-STRONG" else "METRONOME-WEAK",
                                0,
                                "SYSTEM"
                            )
                        }
                    }
                }

                // Smoothly animate pendulum rod during this subdivision slice
                val sliceSteps = 8
                val sliceStepDelay = (subDurationMs / sliceSteps).coerceAtLeast(4L)
                val swingDir = if (beatCounter % 2L == 0L) 1f else -1f
                for (s in 0 until sliceSteps) {
                    val progressInBeat = (subIdx.toFloat() + (s.toFloat() / sliceSteps)) / subCount.toFloat()
                    // Sinusoidal pendulum arc from -swingDir to +swingDir
                    pendulumPhase = swingDir * (-kotlin.math.cos((1f - progressInBeat) * kotlin.math.PI.toFloat()))
                    delay(sliceStepDelay)
                }

                if (!visualOnly && soundType == "Beep") {
                    val pitch = if (isAccentDownbeat) 84 else if (isPrimaryBeat) 76 else 72
                    viewModel.audioEngine.noteOff(pitch)
                }
            }

            beatCounter++
            if (beatCounter % beatsPerBar == 0L) {
                barsCompleted++
                if (tempoTrainerEnabled && barsCompleted % stepBarsInterval.coerceAtLeast(1) == 0) {
                    val nextBpm = (viewModel.metronomeBpm.value + stepBpmIncrement).coerceAtMost(targetMaxBpm)
                    viewModel.setMetronomeBpm(nextBpm)
                    viewModel.liveTutorialBpm.intValue = nextBpm
                }
            }
        }
    }

    val crimsonAccent = Color(0xFFB83B2B)
    val creamHighlight = Color(0xFFF3EFE6)

    val scaleNumberPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(45, 38, 30)
            textSize = 21f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }
    val scaleTempoWordPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(60, 50, 40)
            textSize = 16f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0E0E0E),
        border = BorderStroke(1.dp, Color(0xFF262626)),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            val isWide = maxWidth >= 560.dp

            @Composable
            fun MechanicalPyramidCanvas(modifier: Modifier) {
                Box(
                    modifier = modifier
                        .height(370.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height
                        val cx = cw * 0.5f

                        val topY = ch * 0.06f
                        val baseTopY = ch * 0.86f
                        val baseBotY = ch * 0.94f
                        val topHalfW = cw * 0.11f
                        val botHalfW = cw * 0.38f

                        // 1. Mahogany Pyramid Outer Body
                        val bodyPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(cx - topHalfW, topY)
                            lineTo(cx + topHalfW, topY)
                            lineTo(cx + botHalfW, baseTopY)
                            lineTo(cx - botHalfW, baseTopY)
                            close()
                        }
                        drawPath(
                            path = bodyPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF3A2314), Color(0xFF5C3A21), Color(0xFF472B18), Color(0xFF2A180D)),
                                startX = cx - botHalfW,
                                endX = cx + botHalfW
                            )
                        )
                        drawPath(bodyPath, color = Color(0xFF1E1108), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))

                        // 2. Cream Recessed Scale Faceplate
                        val plateTopY = ch * 0.14f
                        val plateBotY = ch * 0.83f
                        val plateTopHalfW = cw * 0.085f
                        val plateBotHalfW = cw * 0.29f
                        val platePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(cx - plateTopHalfW, plateTopY)
                            lineTo(cx + plateTopHalfW, plateTopY)
                            lineTo(cx + plateBotHalfW, plateBotY)
                            lineTo(cx - plateBotHalfW, plateBotY)
                            close()
                        }
                        drawPath(
                            path = platePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFC8BEAA), Color(0xFFE5DEC9), Color(0xFFD5CCB8)),
                                startX = cx - plateBotHalfW,
                                endX = cx + plateBotHalfW
                            )
                        )
                        drawPath(platePath, color = Color(0xFF28170B), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))

                        // 3. BPM Scale Ticks (20..300) & Italian Tempo Markings
                        val scaleMarks = listOf(
                            20 to "Grave", 40 to "", 50 to "Largo", 60 to "Larghetto",
                            72 to "Adagio", 84 to "", 96 to "Andante", 108 to "Moderato",
                            120 to "", 132 to "", 144 to "Allegro", 160 to "",
                            176 to "Presto", 192 to "", 208 to "", 224 to "",
                            240 to "Prestissimo", 264 to "", 288 to "", 300 to ""
                        )
                        scaleNumberPaint.textSize = (ch * 0.028f).coerceIn(14f, 22f)
                        scaleTempoWordPaint.textSize = (ch * 0.023f).coerceIn(12f, 18f)

                        scaleMarks.forEachIndexed { idx, (markBpm, word) ->
                            val frac = idx.toFloat() / (scaleMarks.size - 1).coerceAtLeast(1)
                            val my = plateTopY + 18f + frac * (plateBotY - plateTopY - 42f)
                            drawLine(
                                color = Color(0xFF3A2E24),
                                start = androidx.compose.ui.geometry.Offset(cx - 10f, my),
                                end = androidx.compose.ui.geometry.Offset(cx + 10f, my),
                                strokeWidth = 1.6f
                            )
                            scaleNumberPaint.textAlign = android.graphics.Paint.Align.RIGHT
                            drawContext.canvas.nativeCanvas.drawText(
                                "$markBpm",
                                cx - 14f,
                                my + scaleNumberPaint.textSize * 0.35f,
                                scaleNumberPaint
                            )
                            if (word.isNotEmpty()) {
                                scaleTempoWordPaint.textAlign = android.graphics.Paint.Align.LEFT
                                drawContext.canvas.nativeCanvas.drawText(
                                    word,
                                    cx + 14f,
                                    my + scaleTempoWordPaint.textSize * 0.35f,
                                    scaleTempoWordPaint
                                )
                            }
                        }

                        // 4. Right-Side Brass Winding Key
                        val keyY = ch * 0.72f
                        val keyStartX = cx + botHalfW * 0.86f
                        drawRect(
                            color = Color(0xFFB59449),
                            topLeft = androidx.compose.ui.geometry.Offset(keyStartX, keyY - 3f),
                            size = androidx.compose.ui.geometry.Size(18f, 6f)
                        )
                        drawRoundRect(
                            color = Color(0xFFC5A455),
                            topLeft = androidx.compose.ui.geometry.Offset(keyStartX + 14f, keyY - 11f),
                            size = androidx.compose.ui.geometry.Size(7f, 22f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )

                        // 5. Swinging Metallic Pendulum Rod & Sliding Brass Trapezoid Weight
                        val pivotY = plateBotY - 12f
                        val maxSwingDeg = 24f
                        val currentAngleDeg = if (isRunning) pendulumPhase * maxSwingDeg else 0f

                        rotate(
                            degrees = currentAngleDeg,
                            pivot = androidx.compose.ui.geometry.Offset(cx, pivotY)
                        ) {
                            val rodTopY = topY + 22f
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF6E5D38), Color(0xFFE6D296), Color(0xFF8C7748)),
                                    startX = cx - 3f,
                                    endX = cx + 3f
                                ),
                                start = androidx.compose.ui.geometry.Offset(cx, pivotY),
                                end = androidx.compose.ui.geometry.Offset(cx, rodTopY),
                                strokeWidth = 4.5f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )

                            // Sliding Brass Weight Position mapped to BPM (20 BPM at top, 300 BPM near bottom)
                            val weightFrac = ((bpm - 20f) / 280f).coerceIn(0f, 1f)
                            val weightCenterY = (plateTopY + 22f) + weightFrac * (plateBotY - plateTopY - 58f)
                            val wTop = 13f
                            val wBot = 9.5f
                            val wHalfH = 11f
                            val weightPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - wTop, weightCenterY - wHalfH)
                                lineTo(cx + wTop, weightCenterY - wHalfH)
                                lineTo(cx + wBot, weightCenterY + wHalfH)
                                lineTo(cx - wBot, weightCenterY + wHalfH)
                                close()
                            }
                            drawPath(
                                path = weightPath,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF7D6632), Color(0xFFE8D292), Color(0xFFB3944C), Color(0xFF5E4B22)),
                                    startX = cx - wTop,
                                    endX = cx + wTop
                                )
                            )
                            drawPath(weightPath, color = Color(0xFF3E3012), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
                        }

                        // Pivot Escapement Nut
                        drawCircle(color = Color(0xFFC5A455), radius = 6.5f, center = androidx.compose.ui.geometry.Offset(cx, pivotY))
                        drawCircle(color = Color(0xFF3E2723), radius = 2.5f, center = androidx.compose.ui.geometry.Offset(cx, pivotY))

                        // 6. Tiered Mahogany Base & Beat Indicator Dots
                        val baseWidth = botHalfW * 2.16f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF4E301B), Color(0xFF362011), Color(0xFF1F1209)),
                                startY = baseTopY,
                                endY = baseBotY
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(cx - baseWidth * 0.5f, baseTopY),
                            size = androidx.compose.ui.geometry.Size(baseWidth, baseBotY - baseTopY),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                        val plinthWidth = baseWidth * 1.06f
                        drawRoundRect(
                            color = Color(0xFF24140A),
                            topLeft = androidx.compose.ui.geometry.Offset(cx - plinthWidth * 0.5f, baseBotY - 2f),
                            size = androidx.compose.ui.geometry.Size(plinthWidth, ch * 0.035f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )

                        // Bottom Beat LEDs (Brass/Ivory dots matching active Time Signature beats)
                        val dotSpacing = 22f
                        val totalDotsW = (beatsPerBar - 1) * dotSpacing
                        val startDotX = cx - totalDotsW * 0.5f
                        val dotY = (baseTopY + baseBotY) * 0.5f
                        for (b in 0 until beatsPerBar) {
                            val dx = startDotX + b * dotSpacing
                            val isThisBeatActive = isRunning && (currentBeatInBar == b)
                            val isFirstBeat = (b == 0)
                            drawCircle(
                                color = when {
                                    isThisBeatActive && isFirstBeat -> Color(0xFFFFD54F)
                                    isThisBeatActive -> Color.White
                                    isFirstBeat -> Color(0xFFC8A858)
                                    else -> Color(0xFFD8D0C0).copy(alpha = 0.75f)
                                },
                                radius = if (isThisBeatActive) 5.5f else 4.2f,
                                center = androidx.compose.ui.geometry.Offset(dx, dotY)
                            )
                        }
                    }
                }
            }

            @Composable
            fun MetronomeControlsColumn(modifier: Modifier) {
                Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Tempo Marking Header ("Andante at a walking pace")
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tempoItalian,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tempoSubtitle,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // 2. [- ]   ♩ = 86   [ +] Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                val next = (bpm - 1).coerceIn(20, 300)
                                viewModel.setMetronomeBpm(next)
                                viewModel.liveTutorialBpm.intValue = next
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1A1A1A),
                            border = BorderStroke(1.dp, Color(0xFF3A3A3A)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("−", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("♩ =", fontSize = 22.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                            Text(
                                text = "$bpm",
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = creamHighlight
                            )
                        }

                        Surface(
                            onClick = {
                                val next = (bpm + 1).coerceIn(20, 300)
                                viewModel.setMetronomeBpm(next)
                                viewModel.liveTutorialBpm.intValue = next
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1A1A1A),
                            border = BorderStroke(1.dp, Color(0xFF3A3A3A)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3. Crimson Tempo Slider (20..300 BPM)
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = {
                            val next = it.roundToInt().coerceIn(20, 300)
                            viewModel.setMetronomeBpm(next)
                            viewModel.liveTutorialBpm.intValue = next
                        },
                        valueRange = 20f..300f,
                        colors = SliderDefaults.colors(
                            thumbColor = crimsonAccent,
                            activeTrackColor = crimsonAccent,
                            inactiveTrackColor = Color(0xFF2C2C2C)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. [Tap] & Red [▶ Start / ■ Stop] Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (tapTimestamps.isNotEmpty() && now - tapTimestamps.last() > 2500L) {
                                    tapTimestamps.clear()
                                }
                                tapTimestamps.add(now)
                                if (tapTimestamps.size > 5) tapTimestamps.removeAt(0)
                                if (tapTimestamps.size >= 2) {
                                    val intervals = tapTimestamps.zipWithNext { a, b -> b - a }
                                    val avgMs = intervals.average()
                                    if (avgMs > 180) {
                                        val tappedBpm = (60_000.0 / avgMs).roundToInt().coerceIn(20, 300)
                                        viewModel.setMetronomeBpm(tappedBpm)
                                        viewModel.liveTutorialBpm.intValue = tappedBpm
                                    }
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF424242)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF161616)),
                            modifier = Modifier.weight(0.38f).height(42.dp)
                        ) {
                            Text("Tap", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { isRunning = !isRunning },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = crimsonAccent),
                            modifier = Modifier.weight(0.62f).height(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "Stop" else "Start",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 5. Time Signature Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Time signature", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        Box {
                            Surface(
                                onClick = { timeSigMenuExpanded = true },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1A1A1A),
                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(timeSig, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = timeSigMenuExpanded,
                                onDismissRequest = { timeSigMenuExpanded = false }
                            ) {
                                listOf("4/4", "3/4", "2/4", "6/8", "5/4", "7/8", "12/8").forEach { sig ->
                                    DropdownMenuItem(
                                        text = { Text(sig, fontWeight = if (sig == timeSig) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            timeSig = sig
                                            viewModel.selectedTimeSignature.value = sig
                                            timeSigMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 6. Subdivide Selector Row: [Off | 2 | 3 | 4]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subdivide", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF3E3E3E), RoundedCornerShape(6.dp))
                        ) {
                            listOf(1 to "Off", 2 to "2", 3 to "3", 4 to "4").forEach { (subVal, lbl) ->
                                val active = subdivision == subVal
                                Box(
                                    modifier = Modifier
                                        .background(if (active) creamHighlight else Color(0xFF161616))
                                        .clickable {
                                            subdivision = subVal
                                            viewModel.metronomeSubdivision.intValue = subVal
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lbl,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 7. Sound Selector Row: [Wood | Click | Beep | Mechanical]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF3E3E3E), RoundedCornerShape(6.dp))
                        ) {
                            listOf("Wood", "Click", "Beep", "Mechanical").forEach { snd ->
                                val active = soundType == snd
                                Box(
                                    modifier = Modifier
                                        .background(if (active) creamHighlight else Color(0xFF161616))
                                        .clickable {
                                            soundType = snd
                                            viewModel.metronomeTickSound.value = snd
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = snd,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 8. Volume Slider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.width(72.dp))
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                viewModel.audioEngine.setMetronomeGain(it)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = crimsonAccent,
                                activeTrackColor = crimsonAccent,
                                inactiveTrackColor = Color(0xFF2C2C2C)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 9. Visual Only Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 64.dp)
                            .clickable { visualOnly = !visualOnly }
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = visualOnly,
                            onCheckedChange = { visualOnly = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = crimsonAccent)
                        )
                        Text("Visual only", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }

                    HorizontalDivider(color = Color(0xFF262626))

                    // 10. Expandable Tempo Trainer Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempoTrainerExpanded = !tempoTrainerExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Tempo trainer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = if (tempoTrainerEnabled) "+$stepBpmIncrement BPM / $stepBarsInterval bars (Max $targetMaxBpm)" else "Off",
                                    fontSize = 13.sp,
                                    color = if (tempoTrainerEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Icon(
                                imageVector = if (tempoTrainerExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        if (tempoTrainerExpanded) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF161616),
                                border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Enable Automatic Speed-Up", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        androidx.compose.material3.Switch(
                                            checked = tempoTrainerEnabled,
                                            onCheckedChange = { tempoTrainerEnabled = it }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Increase by: +$stepBpmIncrement BPM", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(1, 2, 5, 10).forEach { inc ->
                                                pEQFilterChip(
                                                    selected = stepBpmIncrement == inc,
                                                    onClick = { stepBpmIncrement = inc },
                                                    label = { Text("+$inc", fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Every: $stepBarsInterval Bars", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(2, 4, 8, 16).forEach { bCount ->
                                                pEQFilterChip(
                                                    selected = stepBarsInterval == bCount,
                                                    onClick = { stepBarsInterval = bCount },
                                                    label = { Text("$bCount Bars", fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Target Ceiling: $targetMaxBpm BPM", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(120, 140, 160, 200).forEach { maxVal ->
                                                pEQFilterChip(
                                                    selected = targetMaxBpm == maxVal,
                                                    onClick = { targetMaxBpm = maxVal },
                                                    label = { Text("$maxVal", fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 11. Keyboard Shortcuts & Latency Calibration Note (Matches Bottom Right of Reference Screenshot!)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1A1A1A),
                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                            ) {
                                Text(
                                    text = "Space",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text("start or stop ·", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1A1A1A),
                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                            ) {
                                Text(
                                    text = "←",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1A1A1A),
                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                            ) {
                                Text(
                                    text = "→",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Text("tempo (with Shift, by 10) ·", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1A1A1A),
                                border = BorderStroke(1.dp, Color(0xFF3E3E3E))
                            ) {
                                Text(
                                    text = "T",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text("tap", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        }

                        Text(
                            text = "The click plays from this device; most digital pianos cannot play a click over MIDI. The pendulum swings with what you hear, allowing for the latency calibration if you have made one.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Color.White.copy(alpha = 0.42f)
                        )
                    }
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MechanicalPyramidCanvas(modifier = Modifier.weight(0.45f))
                    MetronomeControlsColumn(modifier = Modifier.weight(0.55f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MechanicalPyramidCanvas(modifier = Modifier.fillMaxWidth())
                    MetronomeControlsColumn(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun SongArrangerTimeline(
    viewModel: AppViewModel,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val totalBarsCount = viewModel.songTimeline.size
    val currentPlayingIdx = viewModel.currentPlayingBarIndex.intValue
    val currentStepInBar = viewModel.currentStepIndex.intValue.coerceIn(0, 15)
    val bpm = viewModel.liveTutorialBpm.intValue.coerceIn(40, 260)
    val isSongPlaying = viewModel.isSongPlaybackActive.value

    // Calculate real-time elapsed & total arrangement duration in seconds
    val secPerBar = (60f / bpm.toFloat()) * 4f
    val totalDurationSec = totalBarsCount * secPerBar
    val elapsedSec = if (currentPlayingIdx >= 0 && totalBarsCount > 0) {
        (currentPlayingIdx + (currentStepInBar / 16f)) * secPerBar
    } else 0f

    fun formatArrangementTime(sec: Float): String {
        val total = sec.toInt().coerceAtLeast(0)
        val m = total / 60
        val s = total % 60
        val tenths = ((sec - total) * 10).toInt().coerceIn(0, 9)
        return "$m:${s.toString().padStart(2, '0')}.$tenths"
    }

    var isUserScrubbing by remember { mutableStateOf(false) }
    var userScrubValue by remember { mutableFloatStateOf(0f) }

    val livePlaybackProgress = remember(currentPlayingIdx, currentStepInBar, totalBarsCount) {
        if (totalBarsCount > 0 && currentPlayingIdx >= 0) {
            val fractionalBar = currentPlayingIdx.toFloat() + (currentStepInBar / 16f)
            (fractionalBar / totalBarsCount.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    val horizontalTimelineScroll = rememberScrollState()

    // Auto-scroll the horizontal arrangement lane to follow the active bar during song playback
    androidx.compose.runtime.LaunchedEffect(currentPlayingIdx, isSongPlaying) {
        if (isSongPlaying && currentPlayingIdx >= 0) {
            val targetScrollPx = (currentPlayingIdx * 240 - 120).coerceAtLeast(0)
            horizontalTimelineScroll.animateScrollTo(targetScrollPx)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Header Bar: Metadata Badges + Master Play/Stop + Export MIDI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HORIZONTAL SONG ARRANGEMENT TIMELINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSongPlaying) Color(0xFF00E676).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = if (currentPlayingIdx == -1) "READY • $totalBarsCount BARS"
                            else "PLAYING BAR ${(currentPlayingIdx + 1).toString().padStart(2, '0')}/${totalBarsCount.toString().padStart(2, '0')} • STEP ${(currentStepInBar + 1).toString().padStart(2, '0')}/16",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSongPlaying) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Time: ${formatArrangementTime(elapsedSec)} / ${formatArrangementTime(totalDurationSec)} • Tempo: $bpm BPM (4/4) • Kit: ${viewModel.selectedDrumKit.value?.name ?: viewModel.audioEngine.currentSelectedKitName}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (isSongPlaying) viewModel.stopSongPlayback()
                        else viewModel.startSongPlayback()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSongPlaying) Color(0xFFFF4D6D) else primaryColor
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Icon(
                        imageVector = if (isSongPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isSongPlaying) "STOP SONG" else "PLAY ARRANGEMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 2. Continuous Master Scrubbing Slider & Interactive Bar Ruler Markings
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF10121A),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                Slider(
                    value = if (isUserScrubbing) userScrubValue else livePlaybackProgress,
                    onValueChange = { progress ->
                        isUserScrubbing = true
                        userScrubValue = progress.coerceIn(0f, 1f)
                        if (totalBarsCount > 0) {
                            viewModel.scrubSongPlayhead(userScrubValue)
                        }
                    },
                    onValueChangeFinished = {
                        isUserScrubbing = false
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD700),
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                )

                // Ruler Tick Markings & Clickable Bar Jump Labels
                if (totalBarsCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until totalBarsCount) {
                            val isHighlighted = currentPlayingIdx == i
                            val barSec = i * secPerBar
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        val targetFrac = if (totalBarsCount <= 1) 0f else i.toFloat() / (totalBarsCount - 1).toFloat()
                                        viewModel.scrubSongPlayhead(targetFrac)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(if (isHighlighted) 7.dp else 4.dp)
                                        .background(if (isHighlighted) Color(0xFFFFD700) else Color.White.copy(alpha = 0.35f))
                                )
                                Text(
                                    text = "B${i + 1}",
                                    fontSize = 8.5.sp,
                                    fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isHighlighted) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${barSec.toInt()}s",
                                    fontSize = 7.sp,
                                    color = Color.White.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Horizontal DAW Arrangement Track Lane (Cards laid out horizontally left-to-right!)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0B0D13),
            border = BorderStroke(1.dp, Color(0xFF252836)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalTimelineScroll)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                viewModel.songTimeline.forEachIndexed { index: Int, bar: SongBar ->
                    val isPlayingCurrent = currentPlayingIdx == index
                    var showEditContextBox by remember { mutableStateOf(false) }
                    val matchedTutorial = viewModel.drumTutorials.firstOrNull { it.name == bar.patternName }
                    val activeHitsCount = matchedTutorial?.steps?.sumOf { it.size } ?: 16
                    val sectionRole = when {
                        bar.isFiller || bar.patternName.contains("Fill", true) -> "DRUM FILL"
                        index == 0 -> "INTRO"
                        index == totalBarsCount - 1 && totalBarsCount > 2 -> "OUTRO"
                        index % 4 == 3 -> "TURNAROUND"
                        else -> "MAIN GROOVE"
                    }
                    val accentBarColor = when {
                        isPlayingCurrent -> Color(0xFF00E676)
                        bar.isFiller || bar.patternName.contains("Fill", true) -> Color(0xFFFFC107)
                        else -> primaryColor
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPlayingCurrent) Color(0xFF17223B) else Color(0xFF141722),
                        border = BorderStroke(
                            width = if (isPlayingCurrent) 2.dp else 1.dp,
                            color = if (isPlayingCurrent) Color(0xFF00E676) else accentBarColor.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .width(185.dp)
                            .height(126.dp)
                            .clickable {
                                val targetFrac = if (totalBarsCount <= 1) 0f else index.toFloat() / (totalBarsCount - 1).toFloat()
                                viewModel.scrubSongPlayhead(targetFrac)
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Bar Number Badge + Section Tag + Move Left/Right & Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accentBarColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "BAR ${(index + 1).toString().padStart(2, '0')}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = accentBarColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = sectionRole,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index > 0) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Move Left",
                                            tint = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    val newIndex = (index - 1).coerceIn(0, viewModel.songTimeline.size - 1)
                                                    viewModel.moveTimelineItem(index, newIndex)
                                                }
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Bar",
                                        tint = Color(0xFFFF4D6D),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.removeBarFromTimeline(index) }
                                    )
                                }
                            }

                            // Middle: Pattern Title & Mini 16-Step Visual Beat Strip
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = bar.patternName.substringAfter(": ").uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
                                )

                                // 16-Step Mini Matrix Preview Bar (Lights up active step during playback!)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (s in 0 until 16) {
                                        val hasHitAtStep = matchedTutorial?.steps?.getOrNull(s)?.isNotEmpty() ?: (s % 4 == 0)
                                        val isCurrentPlayheadStep = isPlayingCurrent && currentStepInBar == s
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    when {
                                                        isCurrentPlayheadStep -> Color(0xFFFFEB3B)
                                                        hasHitAtStep -> accentBarColor.copy(alpha = 0.85f)
                                                        s % 4 == 0 -> Color.White.copy(alpha = 0.16f)
                                                        else -> Color.White.copy(alpha = 0.06f)
                                                    }
                                                )
                                        )
                                    }
                                }
                            }

                            // Bottom Row: Note Density Info + Swap Pattern Button + Duplicate Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$activeHitsCount hits • ${secPerBar.toInt()}s",
                                    fontSize = 8.5.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Duplicate Bar Chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier.clickable {
                                            viewModel.addBarToTimeline(patternName = bar.patternName, isFiller = bar.isFiller)
                                        }
                                    ) {
                                        Text(
                                            text = "+DUP",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Swap Pattern Menu Trigger
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = accentBarColor.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, accentBarColor.copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable { showEditContextBox = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Default.Tune, null, tint = accentBarColor, modifier = Modifier.size(10.dp))
                                                Text("SWAP", fontSize = 8.sp, fontWeight = FontWeight.Black, color = accentBarColor)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showEditContextBox,
                                            onDismissRequest = { showEditContextBox = false },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .widthIn(min = 220.dp, max = 320.dp)
                                        ) {
                                            Text(
                                                text = "SELECT BAR ${index + 1} GROOVE / FILL",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = primaryColor,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                            val availablePatterns = if (viewModel.drumTutorials.isNotEmpty()) {
                                                viewModel.drumTutorials
                                            } else {
                                                listOf(
                                                    DrumTutorial("ELECTRONIC_808: 4/4 Core Foundation", emptyList()),
                                                    DrumTutorial("ELECTRONIC_808: Club Syncopation", emptyList()),
                                                    DrumTutorial("ELECTRONIC_808: Build Up Tension Fills", emptyList())
                                                )
                                            }

                                            availablePatterns.forEach { selectionPattern ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            selectionPattern.name.uppercase(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        val isFill = selectionPattern.name.contains("Fill", ignoreCase = true)
                                                        viewModel.updateBarPattern(index, selectionPattern.name, isFiller = isFill)
                                                        showEditContextBox = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // End-of-Track "+ Append Groove Bar" & "+ Append Drum Fill" Action Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.width(145.dp).height(126.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable {
                                val activePattern = viewModel.selectedTutorial.value?.name
                                    ?: viewModel.drumTutorials.firstOrNull()?.name
                                    ?: "ELECTRONIC_808: 4/4 Core Foundation"
                                viewModel.addBarToTimeline(patternName = activePattern, isFiller = false)
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = primaryColor, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+ ADD GROOVE BAR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryColor)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFC107).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable {
                                val fillPattern = viewModel.drumTutorials.firstOrNull { it.name.contains("Fill", true) }?.name
                                    ?: viewModel.selectedTutorial.value?.name
                                    ?: "ELECTRONIC_808: Build Up Tension Fills"
                                viewModel.addBarToTimeline(patternName = fillPattern, isFiller = true)
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+ ADD DRUM FILL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFC107))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalDrumKitGrid(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val professionalPadLabels = remember {
        listOf(
            "0-KICK", "1-SNARE", "2-HIHAT_CLOSED", "3-CLAP",
            "4-KICK_ALT", "5-SNARE_ALT", "6-HIHAT_OPEN", "7-CRASH",
            "8-TOM_HI", "9-TOM_MID", "10-TOM_LO", "11-RIDE",
            "12-BASS_SUB", "13-RIM_CLICK", "14-PERC", "15-FX"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        professionalPadLabels.chunked(4).forEach { rowLabels ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowLabels.forEach { label ->
                    val padIndex = professionalPadLabels.indexOf(label)
                    val isTriggered = viewModel.sequencerTriggeredPads[label] ?: false

                    DrumPadItem(
                        label = label,
                        index = padIndex,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        isGuided = isTriggered,
                        isActive = isTriggered,
                        onClick = {
                            viewModel.audioEngine.playSample(
                                label = label,
                                index = padIndex,
                                string = "DRUM"
                            )

                            scope.launch {
                                viewModel.sequencerTriggeredPads[label] = true
                                delay(80)
                                viewModel.sequencerTriggeredPads[label] = false
                            }
                        },
                        onLongClick = {
                            viewModel.editingPadIndex.value = padIndex
                            viewModel.isKitBuilderActive.value = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrumRackLabel(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = color
            )
        }
    }
}

@Composable
    private fun KitBuilderOverlay(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    pEQGlassCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KIT BUILDER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (viewModel.editingPadIndex.value == -1)
                            "SELECT A PAD TO REMAP"
                        else
                            "EDITING PAD ${viewModel.editingPadIndex.value + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.isKitBuilderActive.value = false
                        viewModel.editingPadIndex.value = -1
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Kit Builder")
                }
            }

            Spacer(Modifier.height(18.dp))

            if (viewModel.editingPadIndex.value == -1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LONG PRESS ANY PAD TO REMAP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            } else {
                val categories = viewModel.sampleLibrary.keys.toList()
                var selectedCategory by remember {
                    mutableStateOf(categories.firstOrNull().orEmpty())
                }

                Text(
                    text = "SAMPLE LIBRARY",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val selected = selectedCategory == category

                        pEQFilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val samples = viewModel.sampleLibrary[selectedCategory] ?: emptyList()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (label in samples) {
                        val padIndex = viewModel.editingPadIndex.value
                        val selected = viewModel.drumPadLabels.getOrNull(padIndex) == label
                        val currentVolume = viewModel.drumPadVolumes[padIndex] ?: 1f

                        Surface(
                            onClick = {
                                viewModel.remapPad(padIndex, label)
                                viewModel.audioEngine.playSample(label, string = "SYSTEM")
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected)
                                primaryColor.copy(alpha = 0.10f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                            border = BorderStroke(
                                1.dp,
                                if (selected)
                                    primaryColor.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )

                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (selected) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Slider(
                                            value = currentVolume,
                                            onValueChange = { viewModel.updateDrumPadVolume(padIndex, it) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.isKitBuilderActive.value = false
                        viewModel.editingPadIndex.value = -1
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = "DONE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun SpectrumBalanceBar(
    low: Float,
    mid: Float,
    high: Float,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    pEQGlassCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(16.dp).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier.weight(low.coerceAtLeast(0.1f)).fillMaxHeight()
                    .background(Color(0xFFE91E63), RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier.weight(mid.coerceAtLeast(0.1f)).fillMaxHeight()
                    .background(primaryColor, RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier.weight(high.coerceAtLeast(0.1f)).fillMaxHeight()
                    .background(Color(0xFF00E676), RoundedCornerShape(4.dp))
            )
        }
    }
}
