package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.components.OpenSheetMusicDisplayViewer
import com.creativeblisslabs.peqlearn.app.ui.components.ExportTabContent
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotationWorkspaceItem(
    viewModel: AppViewModel,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onBack: () -> Unit = {},
    launchFilePicker: () -> Unit = {}
) {
    val formats = listOf("Staff Only", "Tab Only", "Both Viewers")
    val keySignatures = listOf("C Major", "G Major", "D Major", "A Major", "F Major", "B♭ Major", "E♭ Major", "A Minor")
    val timeSignatures = listOf("4/4", "3/4", "2/4", "6/8", "12/8", "5/4", "7/8")
    val instruments = listOf("Guitar", "Piano", "Bass", "Drum kit", "Ukulele")

    var videoExportFormat by rememberSaveable { mutableStateOf("MP4") }
    val exportFormats = listOf("MP4", "MKV", "WEBM", "WAV", "MIDI")

    var isHorizontalScroll by rememberSaveable { mutableStateOf(true) }
    var inlineZoomFactor by rememberSaveable { mutableStateOf(1.0f) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showFullscreenPdf by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showFullscreenPdf) {
        viewModel.isNotationFullscreen.value = showFullscreenPdf
    }

    val stablePlayheadMeasure by remember { derivedStateOf { viewModel.scorePlayheadMeasure.value } }
    val stablePlayheadOffset by remember { derivedStateOf { viewModel.scorePlayheadOffset.value } }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

   LearnSectionCard(title = "Notation Viewer", icon = Icons.Filled.MusicNote) {
        

            // Quick Tool Buttons (Scroll Direction, Import, Export, Fullscreen)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isHorizontalScroll,
                    onClick = { isHorizontalScroll = !isHorizontalScroll },
                    leadingIcon = {
                        Icon(
                            if (isHorizontalScroll) Icons.Default.SwapHoriz else Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(if (isHorizontalScroll) "" else "", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = viewModel.useOsmdRenderer.value,
                    onClick = { viewModel.useOsmdRenderer.value = !viewModel.useOsmdRenderer.value },
                    leadingIcon = {
                        Icon(
                            if (viewModel.useOsmdRenderer.value) Icons.Default.Language else Icons.Default.Brush,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (viewModel.useOsmdRenderer.value) "OSMD (Web)" else "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )

                IconButton(
                    onClick = { viewModel.showSettings.value = true },
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Settings", tint = accentColor, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = { launchFilePicker() },
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Import", tint = accentColor, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = { showFullscreenPdf = true },
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = accentColor, modifier = Modifier.size(20.dp))
                }

                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

        
        // Main Notation Stage Canvas with Zoom Wrapper
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF07080C))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            if (viewModel.useOsmdRenderer.value) {
                OpenSheetMusicDisplayViewer(
                    score = viewModel.currentScore.value as Score?,
                    playheadMeasure = stablePlayheadMeasure,
                    playheadOffset = stablePlayheadOffset,
                    isDarkMode = true,
                    zoom = inlineZoomFactor * 0.85f,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = inlineZoomFactor,
                            scaleY = inlineZoomFactor,
                            transformOrigin = TransformOrigin(0f, 0f)
                        )
                ) {
                    StaffAndTabNotationViewer(
                        score = viewModel.currentScore.value as Score?,
                        playheadMeasure = stablePlayheadMeasure,
                        playheadOffset = stablePlayheadOffset,
                        activeMidiNotes = viewModel.midiActiveNotes,
                        playingNote = viewModel.midiPlayingNote.value,
                        primaryColor = accentColor,
                        currentFormat = viewModel.notationFormat.value,
                        onFormatChange = { viewModel.notationFormat.value = it },
                        isPedalPressed = viewModel.sustainActive.value,
                        modifier = Modifier.fillMaxSize(),
                        scrollState = scrollState
                    )
                }
            }
        }

        // Fullscreen Modal Interaction Interface Window Layers
        if (showFullscreenPdf) {
            var currentPage by rememberSaveable { mutableStateOf(1) }
            var fsZoomFactor by rememberSaveable { mutableStateOf(1.0f) }
            var panOffset by remember { mutableStateOf(Offset.Zero) }
            var areControlsVisible by remember { mutableStateOf(true) }
            var isScrubbing by remember { mutableStateOf(false) }
            var scrubbingProgress by remember { mutableStateOf(0f) }

            val fsScrollState = rememberScrollState()
            
            val isPlaying = viewModel.isScorePlaying.value
            val currentScore = viewModel.currentScore.value
            val measuresPerPage = 8
            val totalMeasures = currentScore?.measures?.size ?: 0
            val totalPages = if (totalMeasures > 0) (totalMeasures + measuresPerPage - 1) / measuresPerPage else 1

            val currentMeasure = stablePlayheadMeasure
            val currentOffset = stablePlayheadOffset

            val totalSeconds = viewModel.scoreTotalDurationSeconds.value
            val currentSeconds = viewModel.scoreCurrentPositionSeconds.value

            val liveProgress = if (totalSeconds > 0) currentSeconds / totalSeconds else 0f

            val density = LocalDensity.current
            val rowHeightPx = remember(density) { with(density) { 260.dp.toPx() } }

            LaunchedEffect(currentMeasure) {
                if (totalMeasures > 0 && !isScrubbing) {
                    currentPage = (currentMeasure / measuresPerPage) + 1
                    val measuresPerRow = 2
                    val currentRow = currentMeasure / measuresPerRow
                    val targetScrollY = (currentRow * rowHeightPx).toInt()
                    fsScrollState.animateScrollTo(targetScrollY)
                }
            }

            LaunchedEffect(currentPage) {
                val firstMeasureOfPage = (currentPage - 1) * measuresPerPage
                val rowOfPage = firstMeasureOfPage / 2
                val targetScrollY = (rowOfPage * rowHeightPx).toInt()
                if (fsScrollState.value != targetScrollY) {
                    fsScrollState.animateScrollTo(targetScrollY)
                }
            }

            val displaySeconds = if (isScrubbing) {
                (scrubbingProgress * totalSeconds).toInt()
            } else {
                currentSeconds.toInt()
            }

            val currentTimeStr = "${displaySeconds / 60}:${(displaySeconds % 60).toString().padStart(2, '0')}"
            val totalSecondsInt = totalSeconds.toInt()
            val totalTimeStr = "${totalSecondsInt / 60}:${(totalSecondsInt % 60).toString().padStart(2, '0')}"

            Dialog(
                onDismissRequest = { showFullscreenPdf = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val viewWidth = maxWidth
                        val viewHeight = maxHeight

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(fsZoomFactor) {
                                    coroutineScope {
                                        launch {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                fsZoomFactor = (fsZoomFactor * zoom).coerceIn(0.7f, 2.2f)
                                                if (fsZoomFactor > 1.0f) {
                                                    val maxPanX = (fsZoomFactor - 1f) * size.width / 2f
                                                    val maxPanY = (fsZoomFactor - 1f) * size.height / 2f
                                                    panOffset = Offset(
                                                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                                    )
                                                } else {
                                                    panOffset = Offset.Zero
                                                }
                                            }
                                        }
                                        launch {
                                            detectTapGestures(onTap = {
                                                areControlsVisible = !areControlsVisible
                                            })
                                        }
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(viewWidth, viewHeight)
                                    .graphicsLayer(
                                        scaleX = fsZoomFactor,
                                        scaleY = fsZoomFactor,
                                        translationX = panOffset.x,
                                        translationY = panOffset.y,
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    )
                            ) {
                                if (viewModel.useOsmdRenderer.value) {
                                    OpenSheetMusicDisplayViewer(
                                        score = currentScore as Score?,
                                        playheadMeasure = currentMeasure,
                                        playheadOffset = currentOffset,
                                        isDarkMode = true,
                                        zoom = fsZoomFactor * 0.95f,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    StaffAndTabNotationViewer(
                                        score = currentScore as Score?,
                                        playheadMeasure = currentMeasure,
                                        playheadOffset = currentOffset,
                                        activeMidiNotes = viewModel.midiActiveNotes,
                                        playingNote = viewModel.midiPlayingNote.value,
                                        primaryColor = accentColor,
                                        currentFormat = viewModel.notationFormat.value,
                                        onFormatChange = { viewModel.notationFormat.value = it },
                                        modifier = Modifier.fillMaxSize(),
                                        scrollState = fsScrollState
                                    )
                                }
                            }
                        }

                        // HUD Layer with max width constraint for landscape / tablets
                        androidx.compose.animation.AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(250)),
                            exit = fadeOut(tween(300)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                                .widthIn(max = 680.dp)
                        ) {
                            Surface(
                    color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = accentColor,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                IconButton(onClick = { viewModel.toggleScorePlayback() }) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { viewModel.resetScorePlayhead() }) {
                                                Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Measure ${currentMeasure + 1} / $totalMeasures",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                                IconButton(
                                                    onClick = {
                                                        if (currentPage > 1) {
                                                            currentPage--
                                                            val targetScrollY = (((currentPage - 1) * measuresPerPage) / 2 * rowHeightPx).toInt()
                                                            scope.launch { fsScrollState.animateScrollTo(targetScrollY) }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                                }

                                                Text(
                                                    text = "Page $currentPage / $totalPages",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )

                                                IconButton(
                                                    onClick = {
                                                        if (currentPage < totalPages) {
                                                            currentPage++
                                                            val targetScrollY = (((currentPage - 1) * measuresPerPage) / 2 * rowHeightPx).toInt()
                                                            scope.launch { fsScrollState.animateScrollTo(targetScrollY) }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                            }

                                            VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.2f))

                                            IconButton(onClick = { showFullscreenPdf = false }) {
                                                Icon(Icons.Default.FullscreenExit, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    // Live Dynamic Scrubber
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = currentTimeStr,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )

                                        Slider(
                                            value = if (isScrubbing) scrubbingProgress else liveProgress,
                                            onValueChange = { targetProgress ->
                                                isScrubbing = true
                                                scrubbingProgress = targetProgress
                                                if (currentScore != null && totalSeconds > 0) {
                                                    viewModel.seekToTotalSeconds(targetProgress * totalSeconds)
                                                }
                                            },
                                            onValueChangeFinished = {
                                                isScrubbing = false
                                                if (currentScore != null && totalSeconds > 0) {
                                                    viewModel.seekToTotalSeconds(scrubbingProgress * totalSeconds)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor,
                                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            )
                                        )

                                        Text(
                                            text = totalTimeStr,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Comprehensive Export Studio Modal Bottom Sheet
        if (showExportDialog) {
            ModalBottomSheet(
                onDismissRequest = { showExportDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Video & Notation Exporter",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showExportDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Render Full Comprehensive Studio Settings
                    ExportTabContent(viewModel = viewModel)

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        if (viewModel.showSettings.value) {
            PlaybackSettingsSheet(
                viewModel = viewModel,
                notationFormat = viewModel.notationFormat.value,
                onFormatChange = { viewModel.notationFormat.value = it },
                showLetters = viewModel.showPianoKeyLabels.value,
                onShowLettersChange = { viewModel.showPianoKeyLabels.value = it },
                is3DPerspective = viewModel.is3DPerspectiveMode.value,
                on3DPerspectiveChange = { viewModel.is3DPerspectiveMode.value = it },
                horizonScale = viewModel.horizonScale.floatValue,
                onHorizonScaleChange = { viewModel.horizonScale.floatValue = it },
                initialTab = 0,
                onDismiss = { viewModel.showSettings.value = false }
            )
        }
    }
}