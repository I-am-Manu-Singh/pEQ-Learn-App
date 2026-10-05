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
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotationWorkspaceItem(
    viewModel: AppViewModel,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onBack: () -> Unit = {},
    launchFilePicker: () -> Unit = {}
) {
    var notationFormat by rememberSaveable { mutableStateOf("Both Viewers") }
    val formats = listOf("Staff Only", "Tab Only", "Both Viewers")

    var selectedKeySig by rememberSaveable { mutableStateOf("C Major") }
    val keySignatures = listOf("C Major", "G Major", "D Major", "A Major", "F Major", "B♭ Major", "E♭ Major", "A Minor")

    var selectedTimeSig by rememberSaveable { mutableStateOf("4/4") }
    val timeSignatures = listOf("4/4", "3/4", "2/4", "6/8", "12/8", "5/4", "7/8")

    var selectedInstrument by rememberSaveable { mutableStateOf("Guitar") }
    val instruments = listOf("Guitar", "Piano", "Bass", "Drum kit", "Ukulele")

    var videoExportFormat by rememberSaveable { mutableStateOf("MP4") }
    val exportFormats = listOf("MP4", "MKV", "WEBM", "WAV", "MIDI")

    var isHorizontalScroll by rememberSaveable { mutableStateOf(true) }
    var shiftNotesOffset by rememberSaveable { mutableStateOf(0) }
    var inlineZoomFactor by rememberSaveable { mutableStateOf(1.0f) }
    var isQuantizeActive by rememberSaveable { mutableStateOf(true) }
    var quantizeDivision by rememberSaveable { mutableStateOf("1/16") }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showFullscreenPdf by rememberSaveable { mutableStateOf(false) }
    var isZooming by remember { mutableStateOf(false) }

    val stablePlayheadMeasure by remember { derivedStateOf { viewModel.scorePlayheadMeasure.value } }
    val stablePlayheadOffset by remember { derivedStateOf { viewModel.scorePlayheadOffset.value } }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Industrial Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column {
                    Text(
                        text = "NOTATION WORKSPACE & ARRANGER",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Multi-System Engraving • MIDI Playback • Dynamic Clef Routing",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

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
                    label = { Text(if (isHorizontalScroll) "Horizontal" else "Vertical", fontSize = 11.sp) }
                )

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
        }

        // Comprehensive Control Toolbar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Viewer Format + Key Shift
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Format:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                        formats.forEach { fmt ->
                            FilterChip(
                                selected = notationFormat == fmt,
                                onClick = { notationFormat = fmt },
                                label = { Text(fmt, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Key Shift: ${if (shiftNotesOffset >= 0) "+$shiftNotesOffset" else "$shiftNotesOffset"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        IconButton(
                            onClick = { shiftNotesOffset-- },
                            modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { shiftNotesOffset++ },
                            modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Row 2: Key Signatures & Time Signatures Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Key:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(keySignatures) { key ->
                                FilterChip(
                                    selected = selectedKeySig == key,
                                    onClick = { selectedKeySig = key },
                                    label = { Text(key, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Time:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                        timeSignatures.take(4).forEach { ts ->
                            FilterChip(
                                selected = selectedTimeSig == ts,
                                onClick = { selectedTimeSig = ts },
                                label = { Text(ts, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Row 3: Target Instrument & Quantization Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Instrument:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                        instruments.forEach { inst ->
                            FilterChip(
                                selected = selectedInstrument == inst,
                                onClick = { selectedInstrument = inst },
                                label = { Text(inst, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Quantize:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                        listOf("1/4", "1/8", "1/16", "1/32").forEach { q ->
                            FilterChip(
                                selected = isQuantizeActive && quantizeDivision == q,
                                onClick = {
                                    isQuantizeActive = true
                                    quantizeDivision = q
                                },
                                label = { Text(q, fontSize = 10.sp) }
                            )
                        }
                    }
                }
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
                    currentFormat = notationFormat,
                    onFormatChange = { notationFormat = it },
                    modifier = Modifier.fillMaxSize(),
                    scrollState = scrollState
                )
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

            val isPlaying = viewModel.isScorePlaying.value
            val currentScore = viewModel.currentScore.value
            val measuresPerPage = 8
            val totalMeasures = currentScore?.measures?.size ?: 0
            val totalPages = if (totalMeasures > 0) (totalMeasures + measuresPerPage - 1) / measuresPerPage else 1

            val beatsPerMeasure = remember(currentScore?.timeSignature) {
                currentScore?.timeSignature?.split("/")?.firstOrNull()?.toFloatOrNull() ?: 4f
            }

            val currentMeasure = stablePlayheadMeasure
            val currentOffset = stablePlayheadOffset
            val liveProgress = if (totalMeasures > 0) (currentMeasure.toFloat() + currentOffset) / totalMeasures.toFloat() else 0f

            val density = LocalDensity.current
            val rowHeightPx = remember(density) { with(density) { 260.dp.toPx() } }

            LaunchedEffect(currentMeasure) {
                if (totalMeasures > 0 && !isScrubbing) {
                    currentPage = (currentMeasure / measuresPerPage) + 1
                    val measuresPerRow = 2
                    val currentRow = currentMeasure / measuresPerRow
                    val targetScrollY = (currentRow * rowHeightPx).toInt()
                    scrollState.animateScrollTo(targetScrollY)
                }
            }

            LaunchedEffect(currentPage) {
                val firstMeasureOfPage = (currentPage - 1) * measuresPerPage
                val rowOfPage = firstMeasureOfPage / 2
                val targetScrollY = (rowOfPage * rowHeightPx).toInt()
                if (scrollState.value != targetScrollY) {
                    scrollState.animateScrollTo(targetScrollY)
                }
            }

            val dynamicSecondsPerMeasure = if (currentScore != null) (60f / currentScore.tempo) * beatsPerMeasure else 2.5f
            val totalSeconds = (totalMeasures * dynamicSecondsPerMeasure).toInt()
            val currentSeconds = if (isScrubbing) {
                (scrubbingProgress * totalMeasures * dynamicSecondsPerMeasure).toInt()
            } else {
                ((currentMeasure.toFloat() + currentOffset) * dynamicSecondsPerMeasure).toInt()
            }

            val currentTimeStr = "${currentSeconds / 60}:${String.format("%02d", currentSeconds % 60)}"
            val totalTimeStr = "${totalSeconds / 60}:${String.format("%02d", totalSeconds % 60)}"

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
                                                isZooming = true
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
                                Box(modifier = if (isZooming) Modifier else Modifier.verticalScroll(scrollState)) {
                                    StaffAndTabNotationViewer(
                                        score = currentScore as Score?,
                                        playheadMeasure = currentMeasure,
                                        playheadOffset = currentOffset,
                                        activeMidiNotes = viewModel.midiActiveNotes,
                                        playingNote = viewModel.midiPlayingNote.value,
                                        primaryColor = accentColor,
                                        currentFormat = notationFormat,
                                        onFormatChange = { notationFormat = it },
                                        modifier = Modifier.fillMaxSize(),
                                        scrollState = scrollState
                                    )
                                }
                            }
                        }

                        // HUD Layer
                        androidx.compose.animation.AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(250)),
                            exit = fadeOut(tween(300)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            Surface(
                                color = Color(0xEE121212),
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
                                                IconButton(onClick = { viewModel.toggleScorePlayback(beatsPerMeasure) }) {
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
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (currentPage > 1) {
                                                        currentPage--
                                                        val targetScrollY = (((currentPage - 1) * measuresPerPage) / 2 * rowHeightPx).toInt()
                                                        scope.launch { scrollState.animateScrollTo(targetScrollY) }
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }

                                            Text(
                                                text = "Page $currentPage / $totalPages",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (currentPage < totalPages) {
                                                        currentPage++
                                                        val targetScrollY = (((currentPage - 1) * measuresPerPage) / 2 * rowHeightPx).toInt()
                                                        scope.launch { scrollState.animateScrollTo(targetScrollY) }
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }

                                            VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 4.dp), color = Color.White.copy(0.2f))

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
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )

                                        Slider(
                                            value = if (isScrubbing) scrubbingProgress else liveProgress,
                                            onValueChange = { targetProgress ->
                                                isScrubbing = true
                                                scrubbingProgress = targetProgress
                                                if (currentScore != null && totalMeasures > 0) {
                                                    val targetTotal = targetProgress * totalMeasures.toFloat()
                                                    val targetMeasure = targetTotal.toInt().coerceIn(0, totalMeasures - 1)
                                                    val targetOffset = targetTotal - targetMeasure
                                                    viewModel.seekToPosition(targetMeasure, targetOffset, beatsPerMeasure)
                                                }
                                            },
                                            onValueChangeFinished = {
                                                isScrubbing = false
                                                if (currentScore != null && totalMeasures > 0) {
                                                    val targetTotal = scrubbingProgress * totalMeasures.toFloat()
                                                    val targetMeasure = targetTotal.toInt().coerceIn(0, totalMeasures - 1)
                                                    val targetOffset = targetTotal - targetMeasure
                                                    viewModel.seekToPosition(targetMeasure, targetOffset, beatsPerMeasure)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            )
                                        )

                                        Text(
                                            text = totalTimeStr,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.6f),
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

        // Export Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Notation Score") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select format for exporting score sheets and audio renders:")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(exportFormats) { fmt ->
                                FilterChip(
                                    selected = videoExportFormat == fmt,
                                    onClick = { videoExportFormat = fmt },
                                    label = { Text(fmt) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showExportDialog = false }) {
                        Text("Confirm Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}