package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList // Added for explicit mutable state list casting
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeActive
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeBpm
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.toggleMetronome
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.setMetronomeBpm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay    // Added for step timing loop
import kotlinx.coroutines.isActive // Added for coroutine cancellation checking
import kotlinx.coroutines.launch

data class AcademicTimeSignature(
    val name: String,
    val beats: Int,
    val bolSequence: List<String>,
    val description: String
)

val ACADEMIC_TIME_SIGNATURES: List<AcademicTimeSignature> = listOf(
    AcademicTimeSignature("4/4 Common Time", 4, listOf("1", "2", "3", "4"), "Four quarter notes per measure. Standard quadruple meter found across rock, pop, and classical."),
    AcademicTimeSignature("3/4 Waltz Meter", 3, listOf("1", "2", "3"), "Three quarter notes per measure with accent on beat 1. Classic waltz and minuet feel."),
    AcademicTimeSignature("2/4 March Meter", 2, listOf("1", "2"), "Two quarter notes per measure. Duple march tempo with strong back-and-forth pulse."),
    AcademicTimeSignature("6/8 Compound Duple", 6, listOf("1", "2", "3", "4", "5", "6"), "Six eighth notes organized into two primary beats of three notes each. Swaying feel."),
    AcademicTimeSignature("12/8 Blues Slow Shuff", 12, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"), "Twelve eighth notes in four beats of triplets. Essential slow blues ballad groove."),
    AcademicTimeSignature("5/4 Asymmetric Meter", 5, listOf("1", "2", "3", "4", "5"), "Five quarter notes divided as 3+2 or 2+3. Complex, energetic modern jazz signature."),
    AcademicTimeSignature("7/8 Balkan Groove", 7, listOf("1", "2", "3", "4", "5", "6", "7"), "Seven eighth notes subdivided as 2+2+3 or 3+2+2. Dynamic folk and progressive rock signature.")
)

@Composable
fun ScaleSelector(selected: String, onSelect: (String) -> Unit) {
    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        items(notes) { note ->
            val isSelected = note == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(note) },
                label = { Text(note, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetronomeControlSection(
    viewModel: AppViewModel,
    primaryColor: Color,
    genreThemeColor: Color,
    workstationTab: Int,
    onTabChange: (Int) -> Unit,
    currentBpm: Float,
    onBpmChange: (Float) -> Unit,
    selectedSigIdx: Int,
    selectedTaalIdx: Int,
    selectedKarnaticIdx: Int,
    totalGridSteps: Int,
    stepsPerBeat: Int,
    beatsPerMeasure: Float,
    selectedScale: String,
    runEngineSafetyCheck: (Int) -> Unit,
    onUpdateIndices: (Int, Int, Int) -> Unit
) {
    val composableScope = rememberCoroutineScope()
    val tapTimes = remember { mutableStateListOf<Long>() }
    val isPlaying = viewModel.isSequencerActive.value

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Slider Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Slider(
                value = currentBpm.coerceIn(40f, 220f),
                onValueChange = { onBpmChange(it) },
                valueRange = 40f..220f,
                colors = SliderDefaults.colors(
                    thumbColor = genreThemeColor,
                    activeTrackColor = genreThemeColor
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${currentBpm.toInt()} BPM",
                fontWeight = FontWeight.Black,
                color = genreThemeColor,
                modifier = Modifier.width(64.dp)
            )
        }

        // Genre Workstation Tab Row
        TabRow(
            selectedTabIndex = workstationTab,
            indicator = { tabPositions ->
                if (workstationTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[workstationTab]),
                        color = genreThemeColor
                    )
                }
            }
        ) {
            listOf("Western", "Hindustani", "Carnatic").forEachIndexed { index, title ->
                Tab(
                    selected = workstationTab == index,
                    onClick = { onTabChange(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (workstationTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (workstationTab == index) genreThemeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Dropdown Metric Configurations
        when (workstationTab) {
            0 -> {
                var sigExpanded by remember { mutableStateOf(false) }
                val currentSig = ACADEMIC_TIME_SIGNATURES.getOrNull(selectedSigIdx) ?: ACADEMIC_TIME_SIGNATURES.first()
                ExposedDropdownMenuBox(expanded = sigExpanded, onExpandedChange = { sigExpanded = it }) {
                    OutlinedTextField(
                        value = currentSig.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("TIME SIGNATURE SCORE") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sigExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = sigExpanded, onDismissRequest = { sigExpanded = false }) {
                        ACADEMIC_TIME_SIGNATURES.forEachIndexed { i, signature ->
                            DropdownMenuItem(
                                text = { Text(signature.name) },
                                onClick = {
                                    composableScope.launch(Dispatchers.Default) { viewModel.stopRiyaazEngine() }
                                    onUpdateIndices(i, selectedTaalIdx, selectedKarnaticIdx)
                                    sigExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                var taalExpanded by remember { mutableStateOf(false) }
                val currentTaal = INDIAN_TAALS.getOrNull(selectedTaalIdx) ?: INDIAN_TAALS.first()
                ExposedDropdownMenuBox(expanded = taalExpanded, onExpandedChange = { taalExpanded = it }) {
                    OutlinedTextField(
                        value = "${currentTaal.name} (${currentTaal.beats} Beats)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SELECT HINDUSTANI TAAL") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = taalExpanded, onDismissRequest = { taalExpanded = false }, modifier = Modifier.heightIn(max = 240.dp)) {
                        INDIAN_TAALS.forEachIndexed { i, t ->
                            DropdownMenuItem(
                                text = { Text("${t.name} (${t.beats} Matras)") },
                                onClick = {
                                    composableScope.launch(Dispatchers.Default) { viewModel.stopRiyaazEngine() }
                                    onUpdateIndices(selectedSigIdx, i, selectedKarnaticIdx)
                                    viewModel.currentStep.value = 0
                                    taalExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            2 -> {
                var carnExpanded by remember { mutableStateOf(false) }
                val currentTala = CARNATIC_TALAS.getOrNull(selectedKarnaticIdx) ?: CARNATIC_TALAS.first()
                ExposedDropdownMenuBox(expanded = carnExpanded, onExpandedChange = { carnExpanded = it }) {
                    OutlinedTextField(
                        value = "${currentTala.name} (${currentTala.beats} Aksharas)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SELECT CARNATIC SAPTA TALA") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = carnExpanded, onDismissRequest = { carnExpanded = false }) {
                        CARNATIC_TALAS.forEachIndexed { i, t ->
                            DropdownMenuItem(
                                text = { Text("${t.name} (${t.beats} Beats)") },
                                onClick = {
                                    composableScope.launch(Dispatchers.Default) { viewModel.stopRiyaazEngine() }
                                    onUpdateIndices(selectedSigIdx, selectedTaalIdx, i)
                                    carnExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Core Trigger Toolbar
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    composableScope.launch(Dispatchers.Default) {
                        if (isPlaying) {
                            viewModel.stopRiyaazEngine()
                        } else {
                            viewModel.stopRiyaazEngine()
                            runEngineSafetyCheck(totalGridSteps)
                            viewModel.startRiyaazEngine(selectedScale, totalGridSteps)
                        }
                    }
                },
                modifier = Modifier.weight(1.2f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = genreThemeColor)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isPlaying) "STOP" else "START", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2000) tapTimes.clear()
                    tapTimes.add(now)
                    if (tapTimes.size >= 2) {
                        val avg = tapTimes.zipWithNext { a, b -> b - a }.average()
                        val calculatedBpm = (60000.0 / avg).toInt().coerceIn(40, 220)
                        onBpmChange(calculatedBpm.toFloat())
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = genreThemeColor),
                border = BorderStroke(1.dp, genreThemeColor.copy(alpha = 0.5f)),
                modifier = Modifier.weight(0.8f).height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("TAP", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        // Multi-Tonal Beat Accents Highlights Grid Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "BEAT CYCLE HIGHLIGHTS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val currentActiveBeat = if (stepsPerBeat > 0) (viewModel.currentStep.value / stepsPerBeat) else 0
                val totalBeats = beatsPerMeasure.toInt().coerceAtLeast(1)

                for (beat in 0 until totalBeats) {
                    val playing = isPlaying && currentActiveBeat == beat
                    val isDownbeat = (beat == 0)

                    val isKhaliBeat = when (workstationTab) {
                        0 -> selectedSigIdx == 3 && beat >= 3 // Off-beat markers for compound 6/8 grids
                        1 -> beat == (INDIAN_TAALS.getOrNull(selectedTaalIdx)?.khali ?: -1)
                        else -> false
                    }

                    // Multi-tonal dynamic variations matching the selected sub-genres
                    val cardContainerColor = when {
                        playing -> genreThemeColor // Solid focus layer
                        isDownbeat -> genreThemeColor.copy(alpha = 0.40f) // Distinct downbeat weight
                        isKhaliBeat -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) // Muted drop tint
                        else -> genreThemeColor.copy(alpha = 0.15f) // Balanced step layer
                    }

                    val contentTextColor = when {
                        playing -> Color.White
                        isDownbeat -> genreThemeColor
                        isKhaliBeat -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else -> genreThemeColor.copy(alpha = 0.85f)
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = when {
                            playing -> BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            isDownbeat -> BorderStroke(2.dp, genreThemeColor)
                            else -> null
                        },
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor)
                    ) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            val labelText = when (workstationTab) {
                                0 -> ACADEMIC_TIME_SIGNATURES.getOrNull(selectedSigIdx)?.bolSequence?.getOrNull(beat) ?: "${beat + 1}"
                                1 -> INDIAN_TAALS.getOrNull(selectedTaalIdx)?.bolSequence?.getOrNull(beat) ?: "B"
                                2 -> CARNATIC_TALAS.getOrNull(selectedKarnaticIdx)?.bolSequence?.getOrNull(beat) ?: "A"
                                else -> "${beat + 1}"
                            }
                            Text(
                                text = labelText,
                                fontSize = 10.sp,
                                fontWeight = if (isDownbeat || playing) FontWeight.Black else FontWeight.Bold,
                                color = contentTextColor
                            )
                        }
                    }
                }
            }
        }

        // Step Sequencer Velocity Grid Row
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("STEP SEQUENCE METER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val totalRenderedSteps = totalGridSteps.coerceIn(1, 16)
                for (s in 0 until totalRenderedSteps) {
                    val stepState = viewModel.sequencerSteps.getOrNull(s)?.value ?: 0
                    val playing = isPlaying && viewModel.currentStep.value == s

                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    playing -> Color.White
                                    stepState == 2 -> genreThemeColor
                                    stepState == 1 -> genreThemeColor.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                            .clickable {
                                if (s < viewModel.sequencerSteps.size) {
                                    val currentState = viewModel.sequencerSteps[s].value
                                    viewModel.sequencerSteps[s].value = (currentState + 1) % 3
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${s + 1}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playing) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Dynamic Text Context Description Surface
        Surface(
            color = genreThemeColor.copy(alpha = 0.06f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (workstationTab) {
                    0 -> {
                        val sig = ACADEMIC_TIME_SIGNATURES.getOrNull(selectedSigIdx) ?: ACADEMIC_TIME_SIGNATURES.first()
                        Text(sig.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = genreThemeColor)
                        Text(sig.description, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                    1 -> {
                        val taal = INDIAN_TAALS.getOrNull(selectedTaalIdx) ?: INDIAN_TAALS.first()
                        Text("${taal.name} | ${taal.beats} Matras", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = genreThemeColor)
                        Text("Vibhag Structure: ${taal.vibhags.joinToString(" • ")}", fontSize = 10.sp)
                        Text(taal.description, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                    2 -> {
                        val tala = CARNATIC_TALAS.getOrNull(selectedKarnaticIdx) ?: CARNATIC_TALAS.first()
                        Text("${tala.name} | ${tala.beats} Aksharas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = genreThemeColor)
                        Text("Anga Division: ${tala.vibhags.joinToString(" + ")}", fontSize = 10.sp)
                        Text(tala.description, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiyaazScreen(
    viewModel: AppViewModel,
    primaryColor: Color,
    onStartRiyaaz: (scale: String, tempo: Int, beatCycleName: String) -> Unit,
    onPlayTanpura: (scale: String, tuningType: String) -> Unit,
    onStopTanpura: () -> Unit
) {
    val composableScope = rememberCoroutineScope()
    var workstationTab by remember { mutableIntStateOf(0) }
    var currentBpm by remember { mutableFloatStateOf(viewModel.liveRiyaazBpm.value.toFloat()) }
    var selectedScale by remember { mutableStateOf("C#") }

    val isTanpuraPlaying = viewModel.isTanpuraActive.value
    val isPlaying by viewModel.isSequencerActive

    var selectedSigIdx by remember { mutableIntStateOf(0) }
    var selectedTaalIdx by remember { mutableIntStateOf(0) }
    var selectedKarnaticIdx by remember { mutableIntStateOf(0) }

    // Multi-tonal dynamic palette context
    val genreThemeColor = remember(workstationTab, primaryColor) {
        when (workstationTab) {
            0 -> Color(0xFF008080) // Deep Cyan/Teal for Western Academic Meter
            1 -> Color(0xFF8B0000) // Classic Crimson/Maroon for Hindustani Taals
            2 -> Color(0xFF4B0082) // Royal Violet/Indigo for Carnatic Aksharas
            else -> primaryColor
        }
    }

    // Safely look up the active Western signature properties from the global dataset
    val currentWesternSig = remember(selectedSigIdx) {
        ACADEMIC_TIME_SIGNATURES.getOrNull(selectedSigIdx) ?: ACADEMIC_TIME_SIGNATURES.first()
    }

    // Evaluate steps density cleanly based on global data configurations
    val stepsPerBeat = remember(workstationTab, selectedSigIdx) {
        if (workstationTab == 0) {
            if (currentWesternSig.beats > 4) currentWesternSig.beats / 4 else 1
        } else {
            1
        }
    }

    // Handles the downbeats structure layer cleanly without overlapping allocations
    val beatsPerMeasure = remember(workstationTab, selectedSigIdx, selectedTaalIdx, selectedKarnaticIdx) {
        when (workstationTab) {
            0 -> if (currentWesternSig.beats > 4) 4 else currentWesternSig.beats
            1 -> INDIAN_TAALS.getOrNull(selectedTaalIdx)?.beats ?: 8
            else -> CARNATIC_TALAS.getOrNull(selectedKarnaticIdx)?.beats ?: 8
        }
    }
    
    val totalGridSteps = (beatsPerMeasure * stepsPerBeat)

    val runEngineSafetyCheck = { steps: Int ->
        val safeCount = steps.coerceAtMost(viewModel.sequencerSteps.size)
        for (i in 0 until safeCount) {
            viewModel.sequencerSteps[i].value = 2 // Accent default
        }
        for (i in safeCount until viewModel.sequencerSteps.size) {
            viewModel.sequencerSteps[i].value = 0 // Clear unused slots
        }
    }

    // Synchronize UI slider state if the hardware state object modifications shift
    LaunchedEffect(viewModel.liveRiyaazBpm.value) {
        currentBpm = viewModel.liveRiyaazBpm.value.toFloat()
    }

    LearnSectionCard(title = "Riyaaz Guru", icon = Icons.Filled.Mediation) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Dynamic pitch alignment on the fly
            ScaleSelector(
                selected = selectedScale,
                onSelect = { newScale ->
                    selectedScale = newScale
                    if (isTanpuraPlaying) {
                        onPlayTanpura(newScale, "PA")
                    }
                }
            )

            MetronomeControlSection(
                viewModel = viewModel,
                primaryColor = primaryColor,
                genreThemeColor = genreThemeColor,
                workstationTab = workstationTab,
                onTabChange = { workstationTab = it },
                currentBpm = currentBpm,
                onBpmChange = { newBpm ->
                    currentBpm = newBpm
                    viewModel.liveRiyaazBpm.value = newBpm.toInt()
                },
                selectedSigIdx = selectedSigIdx,
                selectedTaalIdx = selectedTaalIdx,
                selectedKarnaticIdx = selectedKarnaticIdx,
                totalGridSteps = totalGridSteps,
                stepsPerBeat = stepsPerBeat,
                beatsPerMeasure = beatsPerMeasure.toFloat(),
                runEngineSafetyCheck = runEngineSafetyCheck,
                selectedScale = selectedScale,
                onUpdateIndices = { sig, taal, karn ->
                    selectedSigIdx = sig
                    selectedTaalIdx = taal
                    selectedKarnaticIdx = karn
                }
            )

            // Fully Decoupled Independent Audio Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Independent Sequencer Controller
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.error else primaryColor
                    ),
                    onClick = {
                        composableScope.launch(Dispatchers.Default) {
                            if (isPlaying) {
                                viewModel.stopRiyaazEngine()
                            } else {
                                runEngineSafetyCheck(totalGridSteps)
                                viewModel.liveRiyaazBpm.value = currentBpm.toInt()
                                viewModel.startRiyaazEngine(selectedScale, totalGridSteps)
                            }
                        }
                    }
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "STOP BEAT" else "START BEAT", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp
                    )
                }

                // Independent Tanpura Controller
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTanpuraPlaying) MaterialTheme.colorScheme.error else genreThemeColor
                    ),
                    onClick = {
                        if (isTanpuraPlaying) {
                            onStopTanpura()
                        } else {
                            onPlayTanpura(selectedScale, "PA")
                        }
                    }
                ) {
                    Icon(if (isTanpuraPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isTanpuraPlaying) "STOP DRONE" else "TANPURA", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GrooveStudio(viewModel: AppViewModel, primaryColor: Color) {
    val patterns by viewModel.groovePatterns.collectAsState(initial = emptyList())
    val selectedKit = viewModel.selectedDrumKit.value ?: viewModel.drumKits.firstOrNull()

    // FIX 1 & 2: Cleaned up map type initialization and eliminated the array spread operator bug
 val gridState = remember(selectedKit) {
    mutableStateMapOf<String, SnapshotStateList<Int>>().apply {
        selectedKit?.labels?.forEach { label ->
            this[label] = mutableStateListOf<Int>().apply {
                addAll(List(16) { 0 })
            }
        }
    }
}

    var currentStep by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var bpm by remember { mutableFloatStateOf(120f) }

    LaunchedEffect(isPlaying, bpm) {
        if (isPlaying) {
            // FIX 3: Requires 'import kotlinx.coroutines.isActive'
            while (isActive) { 
                gridState.forEach { (label, steps) ->
                    if (steps.getOrNull(currentStep) == 1) {
                        viewModel.audioEngine.playSample(label, currentStep, "DRUM")
                    }
                }
                // FIX 4: Requires 'import kotlinx.coroutines.delay'
                delay(60000L / (bpm.toLong() * 4)) 
                currentStep = (currentStep + 1) % 16
            }
        } else {
            currentStep = 0
        }
    }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        // --- CONTROL BAR ---
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                }
                Slider(value = bpm, onValueChange = { bpm = it }, valueRange = 60f..200f, modifier = Modifier.weight(1f))
                Text("${bpm.toInt()} BPM", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- ROW-BASED SEQUENCER GRID ---
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
       selectedKit?.labels?.forEach { label ->
    val steps = gridState[label] ?: return@forEach
    Text(label.take(8).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
    
    Row(Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until steps.size) {
            val isOn = steps[i] == 1
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            i == currentStep -> Color.White.copy(alpha = 0.5f)
                            isOn -> primaryColor
                            else -> surfaceVariantColor
                        }
                    )
                    // Mutates seamlessly because 'steps' is explicitly a SnapshotStateList
                    .clickable { steps[i] = if (isOn) 0 else 1 } 
            )
        }
    }
}
    }
}