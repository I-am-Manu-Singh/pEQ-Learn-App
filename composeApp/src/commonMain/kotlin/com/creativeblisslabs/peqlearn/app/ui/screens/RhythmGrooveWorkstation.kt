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
import androidx.compose.material.icons.filled.LibraryMusic 
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeActive
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.metronomeBpm
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.toggleMetronome
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.setMetronomeBpm
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
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
    val isPlaying by viewModel.isSequencerActive

    // Workstation mode: 0 = Western Academic, 1 = Hindustani, 2 = Carnatic
    var workstationTab by remember { mutableIntStateOf(1) } // Default Hindustani classical for Riyaaz
    var selectedSigIdx by remember { mutableIntStateOf(0) }
    var selectedTaalIdx by remember { mutableIntStateOf(0) }
    var selectedKarnaticIdx by remember { mutableIntStateOf(0) }

    // Safely look up active Western signature
    val currentWesternSig = remember(selectedSigIdx) {
        ACADEMIC_TIME_SIGNATURES.getOrNull(selectedSigIdx) ?: ACADEMIC_TIME_SIGNATURES.first()
    }

    val stepsPerBeat = remember(workstationTab, selectedSigIdx) {
        if (workstationTab == 0) {
            if (currentWesternSig.beats > 4) currentWesternSig.beats / 4 else 1
        } else {
            1
        }
    }

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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // =========================================================================
        // SECTION 1: RADEL ELECTRONIC TANPURA & TABLA MASTER STUDIO
        // Full digital LCD telemetry, fine-tune cents, 4/5/6 strings, string motion
        // visualizer, and 3-channel precision mixer (Tanpura, Tabla, Metronome).
        // =========================================================================
        RadelTanpuraTablaStudio(
            viewModel = viewModel,
            isCarnatic = (workstationTab == 2),
            accentColor = primaryColor
        )

        // =========================================================================
        // SECTION 2: INTERACTIVE RIYAAZ STEP SEQUENCER & TĀLA CYCLE ENGINE
        // Eliminates duplicate tempo sliders and duplicate scale chips while giving
        // deep interactive control over beat accents, bol articulations, and steps.
        // =========================================================================
        pEQGlassCard(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Tradition Tab Switcher
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
                            imageVector = Icons.Filled.Mediation,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "STEP SEQUENCER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Sequencer Start / Stop Master Button
                    Button(
                        onClick = {
                            composableScope.launch(Dispatchers.Default) {
                                if (isPlaying) {
                                    viewModel.stopRiyaazEngine()
                                } else {
                                    runEngineSafetyCheck(totalGridSteps)
                                    val activeBols = when (workstationTab) {
                                        1 -> INDIAN_TAALS.getOrNull(selectedTaalIdx)?.bolSequence ?: emptyList()
                                        2 -> CARNATIC_TALAS.getOrNull(selectedKarnaticIdx)?.bolSequence ?: emptyList()
                                        else -> emptyList()
                                    }
                                    viewModel.startRiyaazEngine("C#", totalGridSteps, activeBols)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.error else primaryColor,
                            contentColor = if (isPlaying) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "STOP CYCLE" else "START CYCLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Tradition Tabs
                val genreTabs = listOf("Western", "Hindustani", "Carnatic")
                val genreTabIcons = listOf(Icons.Default.LibraryMusic, Icons.Default.GraphicEq, Icons.Default.SurroundSound)
                TabRow(
                    selectedTabIndex = workstationTab,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        if (workstationTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[workstationTab]),
                                color = primaryColor
                            )
                        }
                    }
                ) {
                    genreTabs.forEachIndexed { index, title ->
                        val isSelected = workstationTab == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                workstationTab = index
                                composableScope.launch(Dispatchers.Default) {
                                    if (isPlaying) viewModel.stopRiyaazEngine()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = genreTabIcons.getOrElse(index) { Icons.Default.MusicNote },
                                    contentDescription = title,
                                    tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Metric & Structure Dropdown Selectors
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
                                            selectedSigIdx = i
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taalExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = taalExpanded,
                                onDismissRequest = { taalExpanded = false },
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                INDIAN_TAALS.forEachIndexed { i, t ->
                                    DropdownMenuItem(
                                        text = { Text("${t.name} (${t.beats} Matras)") },
                                        onClick = {
                                            composableScope.launch(Dispatchers.Default) { viewModel.stopRiyaazEngine() }
                                            selectedTaalIdx = i
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = carnExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = carnExpanded, onDismissRequest = { carnExpanded = false }) {
                                CARNATIC_TALAS.forEachIndexed { i, t ->
                                    DropdownMenuItem(
                                        text = { Text("${t.name} (${t.beats} Beats)") },
                                        onClick = {
                                            composableScope.launch(Dispatchers.Default) { viewModel.stopRiyaazEngine() }
                                            selectedKarnaticIdx = i
                                            carnExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Multi-Tonal Beat Cycle Highlights
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BEAT HIGHLIGHTS",
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
                                0 -> selectedSigIdx == 3 && beat >= 3
                                1 -> beat == (INDIAN_TAALS.getOrNull(selectedTaalIdx)?.khali ?: -1)
                                else -> false
                            }

                            val cardContainerColor = when {
                                playing -> primaryColor
                                isDownbeat -> primaryColor.copy(alpha = 0.40f)
                                isKhaliBeat -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            }

                            val contentTextColor = when {
                                playing -> MaterialTheme.colorScheme.onPrimary
                                isDownbeat -> primaryColor
                                isKhaliBeat -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else -> primaryColor.copy(alpha = 0.85f)
                            }

                            pEQGlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable {
                                        val labelText = when (workstationTab) {
                                            1 -> INDIAN_TAALS.getOrNull(selectedTaalIdx)?.bolSequence?.getOrNull(beat)
                                            2 -> CARNATIC_TALAS.getOrNull(selectedKarnaticIdx)?.bolSequence?.getOrNull(beat)
                                            else -> null
                                        }
                                        if (!labelText.isNullOrBlank()) {
                                            viewModel.audioEngine.playSample(labelText, beat, "TABLA")
                                        }
                                    }
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(cardContainerColor),
                                    Alignment.Center
                                ) {
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
                    Text(
                        text = "16-STEP VELOCITY ACCENT GRID",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val totalRenderedSteps = totalGridSteps.coerceIn(1, 16)
                        for (s in 0 until totalRenderedSteps) {
                            val stepState = viewModel.sequencerSteps.getOrNull(s)?.value ?: 0
                            val playing = isPlaying && viewModel.currentStep.value == s

                            pEQGlassCard(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            when {
                                                playing -> MaterialTheme.colorScheme.onSurface
                                                stepState == 2 -> primaryColor
                                                stepState == 1 -> primaryColor.copy(alpha = 0.4f)
                                                else -> Color.Transparent
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
                                        color = if (playing) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Dynamic Context Structure Description Surface
                pEQGlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (workstationTab) {
                            0 -> {
                                val sig = ACADEMIC_TIME_SIGNATURES.getOrNull(selectedSigIdx) ?: ACADEMIC_TIME_SIGNATURES.first()
                                Text(sig.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(sig.description, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                            1 -> {
                                val taal = INDIAN_TAALS.getOrNull(selectedTaalIdx) ?: INDIAN_TAALS.first()
                                Text("${taal.name} | ${taal.beats} Matras", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Vibhag Structure: ${taal.vibhags.joinToString(" • ")}", fontSize = 10.sp)
                                Text(taal.description, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                            2 -> {
                                val tala = CARNATIC_TALAS.getOrNull(selectedKarnaticIdx) ?: CARNATIC_TALAS.first()
                                Text("${tala.name} | ${tala.beats} Aksharas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Anga Division: ${tala.vibhags.joinToString(" + ")}", fontSize = 10.sp)
                                Text(tala.description, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }
                    }
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
        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Dropdown to select predefined drum pattern
                var patternExpanded by remember { mutableStateOf(false) }
                var selectedPatternName by remember { mutableStateOf<String?>(null) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { patternExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = selectedPatternName ?: "SELECT DRUM PATTERN (${patterns.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }

                        DropdownMenu(
                            expanded = patternExpanded,
                            onDismissRequest = { patternExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            patterns.forEach { pat ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(pat.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("${pat.genre} • ${pat.bpm} BPM", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                    },
                                    onClick = {
                                        selectedPatternName = pat.name
                                        patternExpanded = false
                                        // Load pattern events into grid
                                        selectedKit?.labels?.forEach { lbl ->
                                            val steps = gridState[lbl]
                                            if (steps != null) {
                                                for (i in 0 until steps.size) steps[i] = 0
                                            }
                                        }
                                        pat.events.forEach { evt ->
                                            val label = selectedKit?.labels?.getOrNull(evt.padIndex)
                                                ?: selectedKit?.labels?.firstOrNull()
                                            if (label != null) {
                                                val steps = gridState[label]
                                                if (steps != null && evt.step in 0 until steps.size) {
                                                    steps[evt.step] = 1
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Kit indicator badge
                    Surface(
                        color = primaryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = selectedKit?.name ?: "KIT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { isPlaying = !isPlaying },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.error else primaryColor
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = bpm,
                        onValueChange = { bpm = it },
                        valueRange = 60f..200f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${bpm.toInt()} BPM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
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
            pEQGlassCard(
                Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            when {
                                i == currentStep -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                isOn -> primaryColor
                                else -> Color.Transparent
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
}