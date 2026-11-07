package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.material.icons.filled.Adjust  
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.GridView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.SongBar
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.DrumTutorial
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.switchDrumKit
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeActive
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeBpm
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.setMetronomeBpm
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.toggleMetronome
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.components.pEQDesign
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlin.collections.set
@Composable
fun LearnSectionCard(
    title: String,
    icon: ImageVector,
    info: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    pEQGlassCard(modifier = modifier) {
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

@Composable
private fun AnalysisTile(
    label: String,
    value: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    pEQGlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = primaryColor)
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
    var drumViewMode by remember { mutableStateOf(0) }
    
    val padLabels = remember {
        listOf(
            "Kick 1", "Snare 1", "Rimshot", "Clap",
            "Closed Hat", "Open Hat", "Pedal Hat", "Crash",
            "Low Tom", "Mid Tom", "High Tom", "Ride Cymbal",
            "Shaker", "Cowbell", "Tambourine", "Sub Drop"
        )
    }
    val activeIndices = remember { mutableStateListOf<Int>() }

    LearnSectionCard(
        title = "DRUM ACADEMY & SEQUENCER", 
        icon = Icons.Filled.Adjust,
        modifier = modifier
    ) {
        // BoxWithConstraints is fully supported in Compose Multiplatform (Android, iOS, Desktop, Web)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWideScreen = maxWidth > 840.dp || maxWidth > maxHeight

            if (isWideScreen) {
                // --- WIDE / LANDSCAPE LEFT-RIGHT SPLIT ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TutorialAndPlaybackControl(viewModel, primaryColor)
                        TempoControlRow(viewModel, primaryColor)
                        RhythmGrooveWorkstation(viewModel = viewModel, primaryColor = primaryColor)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DrumWorkspaceSelector(
                            drumViewMode = drumViewMode,
                            onViewModeChange = { drumViewMode = it }
                        )
                        
                        DrumWorkspaceContent(
                            drumViewMode = drumViewMode,
                            viewModel = viewModel,
                            primaryColor = primaryColor,
                            padLabels = padLabels,
                            activeIndices = activeIndices
                        )
                        
                        SongArrangerTimeline(viewModel = viewModel, primaryColor = primaryColor)
                    }
                }
            } else {
                // --- STANDARD PORTRAIT VERTICAL STACK ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TutorialAndPlaybackControl(viewModel, primaryColor)
                    TempoControlRow(viewModel, primaryColor)
                    RhythmGrooveWorkstation(viewModel = viewModel, primaryColor = primaryColor)
                    
                    DrumWorkspaceSelector(
                        drumViewMode = drumViewMode,
                        onViewModeChange = { drumViewMode = it }
                    )
                    
                    DrumWorkspaceContent(
                        drumViewMode = drumViewMode,
                        viewModel = viewModel,
                        primaryColor = primaryColor,
                        padLabels = padLabels,
                        activeIndices = activeIndices
                    )
                    
                    SongArrangerTimeline(viewModel = viewModel, primaryColor = primaryColor)
                }
            }
        }
    }
}

// --- HELPER SUB-COMPOSABLES ---

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
    viewModel: AppViewModel, // <-- Pass viewModel here
    labels: List<String>,
    activeIndices: List<Int>,
    guidedIndices: Set<Int>,
    onPadClick: (Int) -> Unit,
    onPadLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrumRackLabel("MPC CORE BANK A (4x4)", MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AnalysisPill("VELOCITY", "127", MaterialTheme.colorScheme.primary)
                AnalysisPill("LATENCY", "4ms", Color(0xFF00E676))
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
            modifier = Modifier.fillMaxWidth().height(480.dp),
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
enum class GrooveStudioTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    METRONOME("Metronome", Icons.Default.Speed),
    STEP_SEQUENCER("16-Step Drum Grid", Icons.Default.GridView),
}
@Composable
fun RhythmGrooveWorkstation(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    val isMetronomeRunning by viewModel.metronomeActive.collectAsState()
    val bpm by viewModel.metronomeBpm.collectAsState()
    var selectedTab by remember { mutableStateOf(GrooveStudioTab.METRONOME) }
    var isCustomSequencerPlaying by remember { mutableStateOf(false) }
    var isLivePadRecording by remember { mutableStateOf(false) }
    var localSeqStep by remember { mutableStateOf(0) }
    var customPatternCounter by remember { mutableStateOf(1) }
    var timeSignatureNumerator by remember { mutableStateOf(4) }

    val drumTracks = remember {
        listOf("Kick", "Snare", "Hi-Hat", "Clap", "Tom", "Rimshot")
    }
    val trackToSampleLabel = remember {
        mapOf(
            "Kick" to ("KICK" to 0),
            "Snare" to ("SNARE" to 1),
            "Hi-Hat" to ("HH_CLOSED" to 2),
            "Clap" to ("CLAP" to 9),
            "Tom" to ("TOM_MID" to 5),
            "Rimshot" to ("SNARE" to 13)
        )
    }

    val gridState = remember {
        mutableStateMapOf<String, MutableList<Boolean>>().apply {
            drumTracks.forEach { track ->
                this[track] = mutableStateListOf<Boolean>().apply {
                    for (i in 0 until 16) {
                        add(
                            when (track) {
                                "Kick" -> i % 4 == 0
                                "Snare" -> i % 8 == 4
                                "Hi-Hat" -> i % 2 == 0
                                else -> false
                            }
                        )
                    }
                }
            }
        }
    }

    // Live Pad Recording listener: when REC is armed and user taps any drum pad, quantize hit to current step!
    val triggeredKeysSnapshot = viewModel.sequencerTriggeredPads.entries.filter { it.value }.map { it.key }
    androidx.compose.runtime.LaunchedEffect(triggeredKeysSnapshot, isLivePadRecording) {
        if (isLivePadRecording && (isCustomSequencerPlaying || viewModel.isPatternLooping.value)) {
            val step = if (isCustomSequencerPlaying) localSeqStep else viewModel.currentStepIndex.intValue.coerceIn(0, 15)
            triggeredKeysSnapshot.forEach { rawKey ->
                val up = rawKey.uppercase()
                val matchedTrack = when {
                    up.contains("KICK") || up == "KCK" -> "Kick"
                    up.contains("SNARE") || up == "SNR" -> "Snare"
                    up.contains("HAT") || up == "HHC" || up == "OHH" -> "Hi-Hat"
                    up.contains("CLAP") || up == "CLP" -> "Clap"
                    up.contains("TOM") || up.contains("FLOOR") -> "Tom"
                    up.contains("RIM") -> "Rimshot"
                    else -> null
                }
                if (matchedTrack != null) {
                    gridState[matchedTrack]?.set(step, true)
                }
            }
        }
    }

    // Dedicated 16-Step Sequencer Audio & Visual Clock Loop
    androidx.compose.runtime.LaunchedEffect(isCustomSequencerPlaying, bpm) {
        if (!isCustomSequencerPlaying) {
            viewModel.sequencerTriggeredPads.clear()
            return@LaunchedEffect
        }
        while (isCustomSequencerPlaying) {
            val safeBpm = bpm.coerceIn(40, 260)
            val stepDurationMs = (15_000L / safeBpm).coerceAtLeast(45L) // 16th-note step duration
            val step = localSeqStep % 16
            viewModel.currentStepIndex.intValue = step

            viewModel.sequencerTriggeredPads.clear()
            drumTracks.forEach { trackName ->
                if (gridState[trackName]?.getOrNull(step) == true) {
                    val (sampleLbl, padIdx) = trackToSampleLabel[trackName] ?: ("KICK" to 0)
                    viewModel.audioEngine.playSample(label = sampleLbl, index = padIdx, string = "DRUM")
                    viewModel.sequencerTriggeredPads[sampleLbl] = true
                    viewModel.triggerThreeDDrumVisualizer(sampleLbl)
                }
            }

            delay(stepDurationMs)
            localSeqStep = (localSeqStep + 1) % 16
        }
    }

    val isActiveRun = if (selectedTab == GrooveStudioTab.STEP_SEQUENCER) isCustomSequencerPlaying else isMetronomeRunning

    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RHYTHM WORKSTATION",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor
                )
                Text(
                    text = "Time Signature: $timeSignatureNumerator/4 • $bpm BPM",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectedTab == GrooveStudioTab.STEP_SEQUENCER) {
                    OutlinedButton(
                        onClick = { isLivePadRecording = !isLivePadRecording },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isLivePadRecording) Color(0xFFFF1744) else MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isLivePadRecording) Color(0xFFFF1744).copy(alpha = 0.18f) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isLivePadRecording) Color(0xFFFF1744) else Color.Gray)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isLivePadRecording) "REC ARMED" else "REC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLivePadRecording) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(
                    onClick = {
                        if (selectedTab == GrooveStudioTab.STEP_SEQUENCER) {
                            if (!isCustomSequencerPlaying) {
                                viewModel.stopPatternPlayback()
                                viewModel.stopSongPlayback()
                                localSeqStep = 0
                            }
                            isCustomSequencerPlaying = !isCustomSequencerPlaying
                        } else {
                            viewModel.toggleMetronome()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActiveRun) MaterialTheme.colorScheme.error else primaryColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isActiveRun) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isActiveRun) "STOP" else "START", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GrooveStudioTab.entries) { tab ->
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

        when (selectedTab) {
            GrooveStudioTab.METRONOME -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$bpm",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            bpm < 60 -> "Largo"
                            bpm < 80 -> "Adagio"
                            bpm < 108 -> "Andante"
                            bpm < 120 -> "Moderato"
                            bpm < 168 -> "Allegro"
                            else -> "Presto"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor
                    )

                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = {
                            viewModel.setMetronomeBpm(it.toInt())
                            viewModel.liveTutorialBpm.intValue = it.toInt()
                        },
                        valueRange = 30f..240f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 90, 120, 140, 160).forEach { presetBpm ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.setMetronomeBpm(presetBpm)
                                    viewModel.liveTutorialBpm.intValue = presetBpm
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("$presetBpm", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            GrooveStudioTab.STEP_SEQUENCER -> {
                val activeStep = if (isCustomSequencerPlaying) localSeqStep else viewModel.currentStepIndex.intValue
                val anySequencerRunning = isCustomSequencerPlaying || viewModel.isPatternLooping.value || viewModel.isSongPlaybackActive.value

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    drumTracks.forEach { track ->
                        val steps = gridState[track] ?: return@forEach
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = track,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.width(60.dp)
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (i in 0 until 16) {
                                    val isActive = steps.getOrNull(i) == true
                                    val isCurrentStep = (i == activeStep && anySequencerRunning)
                                    val isDownbeat = i % 4 == 0

                                    pEQGlassCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(26.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    when {
                                                        isCurrentStep && isActive -> Color(0xFFFFEB3B)
                                                        isCurrentStep -> primaryColor.copy(alpha = 0.55f)
                                                        isActive -> primaryColor
                                                        isDownbeat -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable {
                                                    val nextVal = !steps[i]
                                                    steps[i] = nextVal
                                                    if (nextVal) {
                                                        val (sampleLbl, padIdx) = trackToSampleLabel[track] ?: ("KICK" to 0)
                                                        viewModel.audioEngine.playSample(sampleLbl, padIdx, "DRUM")
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Row: Clear Grid & Save/Append Custom Beat to Song Arrangement Timeline
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                drumTracks.forEach { tr ->
                                    val list = gridState[tr] ?: return@forEach
                                    for (i in 0 until list.size) list[i] = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.38f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                // Convert the 16-step grid into a DrumTutorial and append it to SongArrangerTimeline!
                                val stepsList = List(16) { stepIdx ->
                                    val hitsAtStep = mutableListOf<String>()
                                    drumTracks.forEach { tr ->
                                        if (gridState[tr]?.getOrNull(stepIdx) == true) {
                                            val (sampleLbl, padIdx) = trackToSampleLabel[tr] ?: ("KICK" to 0)
                                            hitsAtStep.add("$padIdx-$sampleLbl")
                                        }
                                    }
                                    hitsAtStep.toList()
                                }
                                val customName = "${viewModel.audioEngine.currentSelectedKitName}: Custom Groove #${customPatternCounter++} (${bpm} BPM)"
                                val newTutorial = DrumTutorial(name = customName, steps = stepsList)
                                if (viewModel.drumTutorials.none { it.name == customName }) {
                                    viewModel.drumTutorials.add(0, newTutorial)
                                }
                                viewModel.selectedTutorial.value = newTutorial
                                viewModel.addBarToTimeline(patternName = customName, isFiller = false)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.weight(0.62f)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("RECORD GROOVE TO TIMELINE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header & Master Play/Stop Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SONG ARRANGEMENT TIMELINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
                Text(
                            text = if (viewModel.currentPlayingBarIndex.intValue == -1) "IDLE"
                            else "BAR ${(viewModel.currentPlayingBarIndex.intValue + 1).toString().padStart(2, '0')} / ${viewModel.songTimeline.size.toString().padStart(2, '0')}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (viewModel.currentPlayingBarIndex.intValue != -1) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
            }

            Button(
                onClick = {
                    if (viewModel.isSongPlaybackActive.value) viewModel.stopSongPlayback()
                    else viewModel.startSongPlayback()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isSongPlaybackActive.value) Color(0xFFFF4D6D) else primaryColor
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = if (viewModel.isSongPlaybackActive.value) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (viewModel.isSongPlaybackActive.value) "STOP SONG" else "PLAY ARRANGEMENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }



        val totalBarsCount = viewModel.songTimeline.size
        val currentPlayingIdx = viewModel.currentPlayingBarIndex.intValue
        val currentStepInBar = viewModel.currentStepIndex.intValue.coerceIn(0, 15)

        var isUserScrubbing by remember { mutableStateOf(false) }
        var userScrubValue by remember { mutableFloatStateOf(0f) }

        val livePlaybackProgress = remember(currentPlayingIdx, currentStepInBar, totalBarsCount) {
            if (totalBarsCount > 0 && currentPlayingIdx >= 0) {
                val fractionalBar = currentPlayingIdx.toFloat() + (currentStepInBar / 16f)
                (fractionalBar / totalBarsCount.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }

        Column(modifier = Modifier.fillMaxWidth()) {
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
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (totalBarsCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until totalBarsCount) {
                        val isHighlighted = currentPlayingIdx == i
                        Text(
                            text = "Bar ${i + 1}",
                            fontSize = 8.sp,
                            fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Normal,
                            color = if (isHighlighted) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                val targetFrac = if (totalBarsCount <= 1) 0f else i.toFloat() / (totalBarsCount - 1).toFloat()
                                viewModel.scrubSongPlayhead(targetFrac)
                            }
                        )
                    }
                }
            }
        }
            
        
            if (viewModel.songTimeline.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO BARS IN TIMELINE. APPEND A BLOCK BELOW.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    viewModel.songTimeline.forEachIndexed { index: Int, bar: SongBar ->
                        val isPlayingCurrent = viewModel.currentPlayingBarIndex.intValue == index
                        var showEditContextBox by remember { mutableStateOf(false) }

                     
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                var accumulatedDragY by remember(bar.stableId) { mutableFloatStateOf(0f) }
                                val rowHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }

                                // Drag Handle for Reordering
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .pointerInput(bar.stableId) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { accumulatedDragY = 0f },
                                                onDragEnd = { accumulatedDragY = 0f },
                                                onDragCancel = { accumulatedDragY = 0f },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDragY += dragAmount.y

                                                    if (kotlin.math.abs(accumulatedDragY) >= rowHeightPx) {
                                                        val targetOffsetIndex = if (accumulatedDragY > 0) 1 else -1
                                                        val newIndex = (index + targetOffsetIndex).coerceIn(0, viewModel.songTimeline.size - 1)

                                                        if (newIndex != index) {
                                                            viewModel.moveTimelineItem(index, newIndex)
                                                            accumulatedDragY = 0f
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Hold to Drag Track",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Pattern Identification & Metadata Info
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "SLOT BAR ${(index + 1).toString().padStart(2, '0')}${if (bar.isFiller) " • FILLER INJECT" else ""}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (bar.isFiller) Color(0xFFFFC107) else primaryColor,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = bar.patternName.substringAfter(": ").uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }

                                // Action Controls (Tune / Swap & Delete)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { showEditContextBox = true },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Tune, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                        }

                                        // Swap Pattern Dropdown Menu anchored directly to Tune button
                                        DropdownMenu(
                                            expanded = showEditContextBox,
                                            onDismissRequest = { showEditContextBox = false },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .widthIn(min = 220.dp, max = 320.dp)
                                        ) {
                                            Text(
                                                text = "SWAP BAR TRACK TYPE",
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
                                                    DrumTutorial("ELECTRONIC_808: 4/4 Core Foundation", emptyList<List<String>>()),
                                                    DrumTutorial("ELECTRONIC_808: Club Syncopation", emptyList<List<String>>()),
                                                    DrumTutorial("ELECTRONIC_808: Build Up Tension Fills", emptyList<List<String>>())
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
                                                        val fitsAsFillerTrack = selectionPattern.name.contains("Fill", ignoreCase = true)
                                                        viewModel.updateBarPattern(index, selectionPattern.name, isFiller = fitsAsFillerTrack)
                                                        showEditContextBox = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.removeBarFromTimeline(index) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFFFF4D6D).copy(alpha = 0.10f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Delete, null, tint = Color(0xFFFF4D6D), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        
                    }

                    Button(
                        onClick = {
                            viewModel.addBarToTimeline(
                                patternName = viewModel.drumTutorials.firstOrNull()?.name ?: "ELECTRONIC_808: 4/4 Core Foundation",
                                isFiller = false
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("APPEND BLANK ARRANGEMENT BLOCK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
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
