package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.isActive
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.SongBar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PEQLearnScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit = {}
) {
    var showTheory by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showTheory,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "main_content_anim"
    ) { isTheoryActive ->
        if (isTheoryActive) {
            MusicTheoryScreen(
                viewModel = viewModel,
                onBack = { showTheory = false }
            )
        } else {
            LearnMainContent(
                viewModel = viewModel,
                onOpenTheory = { showTheory = true }
            )
        }
    }
}
@Composable
private fun LearnMainContent(
    viewModel: AppViewModel,
    onOpenTheory: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 1000.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.Transparent)
                .padding(horizontal = if (isWide) 32.dp else 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. MUSIC THEORY BOOK ENTRY CARD
            val infiniteAnim = rememberInfiniteTransition(label = "theory_glow")
            val glowAlpha by infiniteAnim.animateFloat(
                initialValue = 0.12f, targetValue = 0.26f,
                animationSpec = infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Reverse),
                label = "glow"
            )
            Surface(
                onClick = onOpenTheory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = primaryColor.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = glowAlpha))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(shape = RoundedCornerShape(14.dp), color = primaryColor.copy(alpha = 0.18f), modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("📖", fontSize = 26.sp) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MUSIC THEORY BOOK", fontSize = 14.sp, fontWeight = FontWeight.Black, color = primaryColor, letterSpacing = 0.5.sp)
                        Text("Essential concepts · Indian & Carnatic Classical, Piano, Guitar, Rhythm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), lineHeight = 15.sp)
                    }
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = primaryColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            // 2. WORKSPACE NAV TABS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple("CHORDS", "Chords", Icons.Default.LibraryMusic),
                    Triple("PIANO", "Piano", Icons.Default.Piano),
                    Triple("DRUMS", "Drums", Icons.Default.Radio),
                    Triple("NOTATION", "Notation", Icons.Default.AudioFile),
                    Triple("RIYAAZ", "Riyaaz", Icons.Default.SelfImprovement),
                    Triple("THEORY", "Theory", Icons.Default.MenuBook)
                )

                tabs.forEach { (tabId, label, iconVector) ->
                    val isSelected = viewModel.selectedWorkspaceTab.value == tabId
                    Button(
                        onClick = { viewModel.selectedWorkspaceTab.value = tabId },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) primaryColor else primaryColor.copy(0.08f),
                            contentColor = if (isSelected) Color.White else primaryColor
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(iconVector, null, modifier = Modifier.size(13.dp))
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            // 3. TAB CONTENT ROUTER
            AnimatedContent(
                targetState = viewModel.selectedWorkspaceTab.value,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "tab_router"
            ) { currentTab ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (currentTab) {
                        "CHORDS" -> {
                            ChordDictionaryPanel(viewModel = viewModel)
                        }
                        "PIANO" -> {
                            PianoKeyboard88Keys(viewModel = viewModel, primaryColor = primaryColor)
                        }
                        "DRUMS" -> {
                            DrumAcademySection(viewModel, primaryColor)
                            Spacer(Modifier.height(16.dp))
                            SongArrangerTimeline(viewModel = viewModel, primaryColor = primaryColor, modifier = Modifier)                        }
                        "NOTATION" -> {
                            Box(modifier = Modifier.fillMaxWidth().height(650.dp)) {
                                NotationWorkspaceItem(
                                    viewModel = viewModel,
                                    accentColor = primaryColor,
                                    launchFilePicker = { viewModel.onPickScoreFileRequested?.invoke() }
                                )
                            }
                        }
                        "RIYAAZ" -> {
                            RiyaazScreen(
                                viewModel = viewModel,
                                primaryColor = primaryColor,
                                onStartRiyaaz = { scale, _, _ -> viewModel.startRiyaazEngine(scale, 16) },
                                onPlayTanpura = { scale, _ ->
                                    viewModel.audioEngine.playSample(scale, 0, "TANPURA")
                                    viewModel.isTanpuraActive.value = true
                                },
                                onStopTanpura = {
                                    viewModel.audioEngine.playSample("STOP", 0, "TANPURA")
                                    viewModel.isTanpuraActive.value = false
                                }
                            )
                        }
                        else -> {
                            ChordDictionaryPanel(viewModel = viewModel)
                        }
                    }
                }
            }

            // 4. SPACER
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun LearnSectionCard(
    title: String,
    icon: ImageVector,
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF14171F),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = primaryColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumAcademySection(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    var workspaceMode by remember { mutableStateOf(0) }
    val padLabels = remember {
        listOf(
            "Kick 1", "Snare 1", "Rimshot", "Clap",
            "Closed Hat", "Open Hat", "Pedal Hat", "Crash",
            "Low Tom", "Mid Tom", "High Tom", "Ride Cymbal",
            "Shaker", "Cowbell", "Tambourine", "Sub Drop"
        )
    }
    val activeIndices = remember { mutableStateListOf<Int>() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "TRAINING MODULES & MASTER CLOCK",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = primaryColor.copy(alpha = 0.75f),
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var dropdownExpanded by remember { mutableStateOf(false) }
            val currentSelectedTutorial = viewModel.selectedTutorial.value

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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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

                Button(
                    onClick = { viewModel.handleTapTempo() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.height(2.dp))
                        Text("TAP", fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // Workspace switcher: 0 = 16-Pad MPC Grid, 1 = 3D Drum Visualizer, 2 = Drum Kits
    var drumViewMode by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = drumViewMode == 0,
                    onClick = { drumViewMode = 0 },
                    label = { Text("16-Pad MPC Grid", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = drumViewMode == 1,
                    onClick = { drumViewMode = 1 },
                    label = { Text("3D Drum Visualizer", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = drumViewMode == 2,
                    onClick = { drumViewMode = 2 },
                    label = { Text("Drum Kits", fontSize = 11.sp) }
                )
            }
        }

        when (drumViewMode) {
            0 -> {
                DrumPadKitGrid(
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(kit.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("${kit.labels.size} Samples Preloaded", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                }
                                Button(
                                    onClick = {
                                        viewModel.selectedDrumKit.value = kit
                                        viewModel.drumPadLabels.clear()
                                        viewModel.drumPadLabels.addAll(kit.labels)
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
}
@Composable
fun DrumPadKitGrid(
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
            modifier = Modifier.fillMaxWidth().height(380.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            gridItemsIndexed(labels) { index, label ->
                val isActive = activeIndices.contains(index)
                DrumPadItem(
                    label = label,
                    index = index,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    isActive = isActive,
                    isGuided = guidedIndices.contains(index),
                    onClick = { onPadClick(index) },
                    onLongClick = { onPadLongClick(index) }
                )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SONG ARRANGEMENT TIMELINE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = primaryColor.copy(alpha = 0.75f),
                letterSpacing = 1.sp
            )

            Button(
                onClick = {
                    if (viewModel.isSongPlaybackActive.value) viewModel.stopSongPlayback()
                    else viewModel.startSongPlayback()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isSongPlaybackActive.value) Color(0xFFFF4D6D) else primaryColor
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (viewModel.isSongPlaybackActive.value) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (viewModel.isSongPlaybackActive.value) "STOP SONG" else "PLAY ARRANGEMENT", 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Black
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LinearScale, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Text("ARRANGER PLAYHEAD SCRUBBER", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }

                    Text(
                        text = if (viewModel.currentPlayingBarIndex.intValue == -1) "IDLE"
                        else "PLAYING BAR ${(viewModel.currentPlayingBarIndex.intValue + 1).toString().padStart(2, '0')}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )
                }

                val totalBarsCount = viewModel.songTimeline.size
                val currentScrubProgress = remember(viewModel.currentPlayingBarIndex.intValue, totalBarsCount) {
                    if (totalBarsCount > 0 && viewModel.currentPlayingBarIndex.intValue != -1) {
                        viewModel.currentPlayingBarIndex.intValue.toFloat() / totalBarsCount.toFloat()
                    } else 0f
                }

                Slider(
                    value = currentScrubProgress,
                    onValueChange = { viewModel.scrubSongPlayhead(it) },
                    valueRange = 0f..0.99f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text(
            text = "TIMELINE BARS (LONG PRESS DRAG HANDLE TO MOVE)",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = primaryColor.copy(alpha = 0.75f),
            letterSpacing = 1.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = viewModel.songTimeline,
                    key = { _, item: SongBar -> item.stableId }
                ) { index: Int, bar: SongBar ->
                    val isPlayingCurrent = viewModel.currentPlayingBarIndex.intValue == index
                    var showEditContextBox by remember { mutableStateOf(false) }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isPlayingCurrent -> primaryColor.copy(alpha = 0.16f)
                            bar.isFiller -> Color(0xFFFFC107).copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isPlayingCurrent) 2.dp else 1.dp,
                            color = when {
                                isPlayingCurrent -> primaryColor
                                bar.isFiller -> Color(0xFFFFC107).copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            var accumulatedDragY by remember(bar.stableId) { mutableFloatStateOf(0f) }
                            val rowHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }

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
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Tune, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { viewModel.removeBarFromTimeline(index) },
                                        modifier = Modifier.size(32.dp).background(Color(0xFFFF4D6D).copy(alpha = 0.10f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF4D6D), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Box {
                                DropdownMenu(
                                    expanded = showEditContextBox,
                                    onDismissRequest = { showEditContextBox = false }
                                ) {
                                    Text(
                                        text = " SWAP BAR TRACK TYPE", 
                                        fontSize = 7.sp, 
                                        fontWeight = FontWeight.Black, 
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    viewModel.drumTutorials.forEach { selectionPattern ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(selectionPattern.name.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black) 
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
                        }
                    }
                }

                item {
                    Button(
                        onClick = { 
                            viewModel.addBarToTimeline(
                                patternName = viewModel.drumTutorials.firstOrNull()?.name ?: "EMPTY", 
                                isFiller = false
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
}


@Composable
fun pEQLearnScreen(viewModel: AppViewModel) {
    var showTheory by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showTheory,
        label = "theory_nav",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { isTheory ->
        if (isTheory) {
            MusicTheoryScreen(viewModel = viewModel, onBack = { showTheory = false })
        } else {
            PEQLearnScreen(
                viewModel = viewModel,
                onBack = { showTheory = true }
            )
        }
    }
}

@Composable
fun AnalysisPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF151824)
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

    Surface(
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
            },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(
            width = if (pressed || isActive) 2.dp else 1.dp,
            color = if (pressed || isActive) padColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),
        tonalElevation = if (pressed) 1.dp else 4.dp
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
private fun KitBuilderOverlay(
    viewModel: AppViewModel,
    primaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(1.dp, primaryColor.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
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

                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor.copy(alpha = 0.18f),
                                selectedLabelColor = primaryColor
                            )
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