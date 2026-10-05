package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.audio.AudioEngine
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedGuitarFretboard
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.MusicConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class IndianClassicalPanelTab(val label: String) {
    RAAGAS("Rāga Library"),
    THAATS("10 Thaats"),
    TAALS("Tāla Systems"),
    ALANKAARS("20+ Alaankars"),
    TABLA_BOLS("Tabla Bols")
}

@Composable
internal fun IndianClassicalInteractivePanel(viewModel: AppViewModel) {
    val indianColor = Color(0xFFFF9933)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Raagas", "Thaats", "Taals", "Alankaars", "Tabla Bols")
    var notationFormat by remember { mutableStateOf("Staff Notation") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Museum, null, modifier = Modifier.size(16.dp), tint = indianColor)
            Text("INDIAN CLASSICAL MUSIC", fontSize = 12.sp, fontWeight = FontWeight.Black, color = indianColor, letterSpacing = 1.5.sp)
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = indianColor,
            indicator = { tabPositions -> if (selectedTab < tabPositions.size) TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = indianColor) }
        ) {
            tabs.forEachIndexed { i, label -> Tab(selected = selectedTab == i, onClick = { selectedTab = i; viewModel.currentScore.value = null }, text = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }) }
        }

        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (selectedTab) {
                0 -> IndianRaagaBrowser(viewModel, indianColor)
                1 -> IndianThaatBrowser(viewModel, indianColor)
                2 -> IndianTaalBrowser(viewModel, indianColor)
                3 -> IndianAlankaarPanel(viewModel, indianColor)
                4 -> IndianTablaBolsPanel(indianColor)
            }
            
            // Notation Viewer linked to viewModel.currentScore
            StaffAndTabNotationViewer(
                score = viewModel.currentScore.value as? Score,
                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                playheadOffset = viewModel.scorePlayheadOffset.value,
                activeMidiNotes = viewModel.midiActiveNotes,
                playingNote = viewModel.midiPlayingNote.value,
                primaryColor = indianColor,
                currentFormat = notationFormat,
                onFormatChange = { notationFormat = it },
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
    }
}

@Composable
fun InfoChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.1f), border = BorderStroke(1.dp, color.copy(0.3f))) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun Modifier.fillModifierOrWrap() = this.fillMaxWidth().wrapContentHeight()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IndianRaagaBrowser(
    viewModel: AppViewModel? = null,
    accentColor: Color,
    onSelectRaga: (Raaga) -> Unit = {}
) {
    val allRagas = viewModel?.ragaList?.value ?: emptyList()
    val apiRagas = allRagas.filter { it.system.equals("Hindustani", ignoreCase = true) }
    val isFallback = apiRagas.isEmpty()

    var selectedIdx by remember { mutableStateOf(0) }
    var visualizerMode by remember { mutableIntStateOf(0) } // 0 = Harmonium/Piano, 1 = Guitar
    var selectedDetailsTab by remember { mutableIntStateOf(0) } // 0 = Details, 1 = Bandish, 2 = Advanced Data
    var isAuditionRunning by remember { mutableStateOf(false) }
    var currentPlayingIdx by remember { mutableIntStateOf(-1) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val sourceList = if (allRagas.isNotEmpty()) allRagas.filter { it.system.equals("Hindustani", ignoreCase = true) } else INDIAN_RAAGAS
    val filteredRagas = remember(searchQuery, sourceList) {
        if (searchQuery.isEmpty()) sourceList
        else sourceList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.thaat.contains(searchQuery, ignoreCase = true) ||
            it.mood.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeSelectedIdx = selectedIdx.coerceIn(0, (filteredRagas.size - 1).coerceAtLeast(0))
    val selectedRaga = filteredRagas.getOrNull(activeSelectedIdx) ?: INDIAN_RAAGAS.first()

    val ragaName = selectedRaga.name
    val parentThaat = selectedRaga.thaat
    val arohaText = selectedRaga.aroha
    val avarohaText = selectedRaga.avaroha
    val descText = selectedRaga.description

    val arohaSemitones = remember(selectedRaga) { selectedRaga.semitones }
    val avarohaSemitones = remember(selectedRaga) { selectedRaga.semitones.reversed() }
    val completeRagaMelodyPattern = remember(arohaSemitones, avarohaSemitones) { arohaSemitones + avarohaSemitones }

    val activeTaalFramework = remember(selectedRaga) {
        INDIAN_TAALS.find { it.name.equals(selectedRaga.taal, ignoreCase = true) } ?: INDIAN_TAALS.first()
    }

    val currentRaagaData = selectedRaga

    LaunchedEffect(currentRaagaData, activeTaalFramework) {
        viewModel?.currentScore?.value = MusicConverter.convertRaagaToScore(currentRaagaData, 60, activeTaalFramework)
    }

    Column(modifier = Modifier.fillModifierOrWrap(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; selectedIdx = 0 },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search ragas (e.g. Yaman, Bhairav, Darbari)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredRagas.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(filteredRagas.take(40)) { idx, raga ->
                    val name = raga.name
                    val isSelected = activeSelectedIdx == idx
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedIdx = idx
                            onSelectRaga(currentRaagaData)
                        },
                        label = { Text(name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.25f),
                            selectedLabelColor = accentColor
                        )
                    )
                }
            }
        }

        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("Mood: ${currentRaagaData.mood}", Icons.Default.MusicNote, accentColor)
            InfoChip("Taal: ${currentRaagaData.taal}", Icons.Default.MusicNote, accentColor)
            InfoChip("Time: ${currentRaagaData.time}", Icons.Default.MusicNote, accentColor)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(ragaName, fontSize = 20.sp, fontWeight = FontWeight.Black, color = accentColor)
                        Text(
                            text = "Parent Thaat: $parentThaat • ${currentRaagaData.jati.ifEmpty { "Sampoorna" }}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Button(
                        onClick = {
                            if (!isAuditionRunning) {
                                isAuditionRunning = true
                                val engine = getAudioEngine()
                                val scaleNotes = completeRagaMelodyPattern.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11, 12) }
                                scope.launch {
                                    try {
                                        scaleNotes.forEachIndexed { idx, semitone ->
                                            currentPlayingIdx = idx
                                            val currentMidiPitch = 65 + semitone
                                            if (viewModel != null) {
                                                viewModel.scorePlayheadMeasure.value = idx / activeTaalFramework.beats
                                                viewModel.scorePlayheadOffset.value = (idx % activeTaalFramework.beats).toFloat() / activeTaalFramework.beats.toFloat()
                                                viewModel.midiPlayingNote.value = currentMidiPitch
                                            }
                                            engine.noteOn(currentMidiPitch, 0.85f)
                                            delay(380L)
                                            engine.noteOff(currentMidiPitch)
                                        }
                                    } finally {
                                        currentPlayingIdx = -1
                                        if (viewModel != null) {
                                            viewModel.midiPlayingNote.value = null
                                            viewModel.scorePlayheadMeasure.value = 0
                                            viewModel.scorePlayheadOffset.value = 0f
                                        }
                                        isAuditionRunning = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(if (isAuditionRunning) Icons.Default.GraphicEq else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAuditionRunning) "Auditioning..." else "Audition Rāga", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val subTabs = listOf("Details", "Bandish", "Advanced Data")
                TabRow(
                    selectedTabIndex = selectedDetailsTab,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedDetailsTab]),
                            color = accentColor
                        )
                    }
                ) {
                    subTabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedDetailsTab == idx,
                            onClick = { selectedDetailsTab = idx },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedDetailsTab == idx) accentColor else Color.White.copy(alpha = 0.6f)) }
                        )
                    }
                }

                when (selectedDetailsTab) {
                    0 -> {
                        Surface(shape = RoundedCornerShape(10.dp), color = accentColor.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("↑ Aroha: $arohaText", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isAuditionRunning && currentPlayingIdx < arohaSemitones.size) accentColor else Color.White)
                                Text("↓ Avaroha: $avarohaText", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isAuditionRunning && currentPlayingIdx >= arohaSemitones.size) accentColor else Color.White)
                            }
                        }

                        if (currentRaagaData.pakad.isNotEmpty()) {
                            PlayableInfoRow(Icons.Default.Star, "Pakad (Signature Phrase)", currentRaagaData.pakad, completeRagaMelodyPattern, Color(0xFFFFD54F), scope, viewModel)
                        }
                        if (currentRaagaData.chalan.isNotEmpty()) {
                            PlayableInfoRow(Icons.Default.LinearScale, "Chalan (Movement Flow)", currentRaagaData.chalan, completeRagaMelodyPattern, Color(0xFF81C784), scope, viewModel)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetaChip("Vadi (Primary)", currentRaagaData.vadi.ifEmpty { "Sa" }, Color(0xFF64B5F6))
                            MetaChip("Samvadi (Secondary)", currentRaagaData.samvadi.ifEmpty { "Pa" }, Color(0xFF81C784))
                            MetaChip("Time (Prahar)", currentRaagaData.time.ifEmpty { "Evening" }, Color(0xFFFFB74D))
                            MetaChip("Rasa (Mood)", currentRaagaData.mood.ifEmpty { "Shanta" }, Color(0xFFE57373))
                        }

                        if (descText.isNotEmpty()) {
                            Text(
                                descText,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentRaagaData.bandish.isNotEmpty()) currentRaagaData.bandish else "Traditional Classical Bandish",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                if (currentRaagaData.taal.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "Taal: ${currentRaagaData.taal}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("स्थायी (Sthayi - First Verse):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                                    Text(
                                        text = if (currentRaagaData.sthai.isNotEmpty()) currentRaagaData.sthai else "Paayo ji maine Ram ratan dhan paayo",
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("अंतरा (Antara - Second Verse):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                    Text(
                                        text = if (currentRaagaData.antara.isNotEmpty()) currentRaagaData.antara else "Janam janam ki poonji paayi",
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }

                            if (currentRaagaData.transliteration.isNotEmpty()) {
                                Text(
                                    "Transliteration: ${currentRaagaData.transliteration}",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            if (currentRaagaData.meaning.isNotEmpty()) {
                                Text(
                                    "Translation: ${currentRaagaData.meaning}",
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Alaap Phrases: ${if (currentRaagaData.alaap.isNotEmpty()) currentRaagaData.alaap.joinToString(", ") else "N/A"}", fontSize = 12.sp, color = Color.White)
                            Text("Taans / Paltas: ${if (currentRaagaData.taansPaltas.isNotEmpty()) currentRaagaData.taansPaltas.joinToString(", ") else "N/A"}", fontSize = 12.sp, color = Color.White)
                            Text("Jhala / Tihai: ${currentRaagaData.jhala.ifEmpty { "N/A" }}", fontSize = 12.sp, color = Color.White)
                            Text("Bol Taal Pattern: ${currentRaagaData.bolTaal.ifEmpty { "N/A" }}", fontSize = 12.sp, color = Color.White)
                            Text("Stream URL: ${currentRaagaData.streamUrl ?: "None"}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("Video URL: ${currentRaagaData.videoUrl ?: "None"}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("Accent Color: ${currentRaagaData.accentColor ?: "Default"}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "INTERACTIVE SWARA MAP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("🎹 Piano" to Icons.Default.Keyboard, "🎸 Guitar" to Icons.Default.MusicNote).forEachIndexed { index, (modeTitle, modeIcon) ->
                            val isCurrentActive = visualizerMode == index
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isCurrentActive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { visualizerMode = index }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = modeTitle,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isCurrentActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = modeTitle,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrentActive) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isCurrentActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                val activeSemitones = completeRagaMelodyPattern.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11, 12) }
                val dynamicPlayingSemitone = if (currentPlayingIdx in activeSemitones.indices) activeSemitones[currentPlayingIdx] else -1

                if (visualizerMode == 0) {
                    val activeMidiMap = remember(activeSemitones, dynamicPlayingSemitone) {
                        if (dynamicPlayingSemitone != -1) mapOf((60 + dynamicPlayingSemitone) to true)
                        else activeSemitones.associate { (60 + it) to true }
                    }
                    UnifiedPianoKeyboard(
                        activeMidiNotes = activeMidiMap,
                        accentColor = accentColor,
                        startNote = 60,
                        endNote = 72,
                        showNoteNames = true,
                        modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    val highlightedGuitarList = remember(activeSemitones, dynamicPlayingSemitone) {
                        if (dynamicPlayingSemitone != -1) listOf(dynamicPlayingSemitone) else activeSemitones
                    }
                    UnifiedGuitarFretboard(
                        numFrets = 12,
                        activeNotes = highlightedGuitarList,
                        rootNote = 0,
                        accentColor = accentColor,
                        showNoteNames = true,
                        modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(8.dp))
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text("Associated Bandish Composition", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)

                if (currentRaagaData.sthai.isNotEmpty() || currentRaagaData.bandish.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentRaagaData.sthai.isNotEmpty()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("स्थायी (Sthayi)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                    Text(currentRaagaData.sthai, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        if (currentRaagaData.antara.isNotEmpty()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("अन्तरा (Antara)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                    Text(currentRaagaData.antara, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Text("Taal Framework: ${currentRaagaData.taal}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (currentRaagaData.meaning.isNotEmpty()) {
                            Text("Translation: ${currentRaagaData.meaning}", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("No local classical bandish composition linked to this scale format.", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
fun IndianThaatBrowser(viewModel: AppViewModel? = null, accentColor: Color) {
    var selectedIdx by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentThaat = INDIAN_THAATS.getOrNull(selectedIdx) ?: INDIAN_THAATS.first()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("The 10 Foundational Parent Thaats of Hindustani Classical Music", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(INDIAN_THAATS) { idx, thaat ->
                val isSelected = idx == selectedIdx
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedIdx = idx },
                    label = { Text(thaat.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.25f),
                        selectedLabelColor = accentColor
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentThaat.name + " Thaat", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    IconButton(
                        onClick = {
                            if (!isPlaying) {
                                isPlaying = true
                                val engine = getAudioEngine()
                                scope.launch {
                                    try {
                                        val notes = currentThaat.semitones.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11, 12) }
                                        notes.forEach { semi ->
                                            val note = 60 + semi
                                            viewModel?.midiPlayingNote?.value = note
                                            engine.noteOn(note, 0.85f)
                                            delay(380L)
                                            engine.noteOff(note)
                                        }
                                    } finally {
                                        viewModel?.midiPlayingNote?.value = null
                                        isPlaying = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp).background(accentColor.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                }

                Text("Swaras: ${currentThaat.swaras.joinToString("  •  ")}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(currentThaat.description, fontSize = 12.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
fun IndianTaalBrowser(viewModel: AppViewModel? = null, accentColor: Color) {
    var selectedIdx by remember { mutableStateOf(0) }
    val taal = INDIAN_TAALS.getOrNull(selectedIdx) ?: INDIAN_TAALS.first()
    var currentBeatPlayhead by remember { mutableIntStateOf(-1) }
    var isTaalLooping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(INDIAN_TAALS) { idx, t ->
                val isSelected = idx == selectedIdx
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedIdx = idx
                        isTaalLooping = false
                        currentBeatPlayhead = -1
                    },
                    label = { Text("${t.name} (${t.beats}m)", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.25f),
                        selectedLabelColor = accentColor
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(taal.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Text("${taal.beats} Matras • ${taal.vibhags.size} Vibhags", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = {
                            if (isTaalLooping) {
                                isTaalLooping = false
                                currentBeatPlayhead = -1
                            } else {
                                isTaalLooping = true
                                val engine = getAudioEngine()
                                scope.launch {
                                    try {
                                        while (isTaalLooping) {
                                            taal.bolSequence.forEachIndexed { bIdx, _ ->
                                                if (!isTaalLooping) return@forEachIndexed
                                                currentBeatPlayhead = bIdx
                                                val drumMidi = if (bIdx == 0) 36 else 38
                                                engine.noteOn(drumMidi, 0.9f)
                                                delay(500L)
                                                engine.noteOff(drumMidi)
                                            }
                                        }
                                    } finally {
                                        currentBeatPlayhead = -1
                                        isTaalLooping = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTaalLooping) Color(0xFFE53935) else accentColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(if (isTaalLooping) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isTaalLooping) "Stop Loop" else "Play Tāla Loop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("Theka Matra Matrix:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(taal.bolSequence) { bIdx, bol ->
                        val isCurrentBeat = currentBeatPlayhead == bIdx
                        val isSam = bIdx == 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isCurrentBeat -> Color(0xFFFFD54F)
                                isSam -> accentColor.copy(alpha = 0.35f)
                                else -> Color.White.copy(alpha = 0.05f)
                            },
                            border = BorderStroke(1.dp, if (isSam) accentColor else Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${bIdx + 1}",
                                    fontSize = 9.sp,
                                    color = if (isCurrentBeat) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = bol,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentBeat) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                Text(taal.description, fontSize = 12.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
fun IndianAlankaarPanel(viewModel: AppViewModel? = null, accentColor: Color) {
    var isRunning by remember { mutableStateOf(false) }
    var activeAlankaarIdx by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${INDIAN_ALAANKARS.size} Graded Vocal/Instrumental Exercise Sequences", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        INDIAN_ALAANKARS.take(15).forEachIndexed { index, alankaar ->
            val isThisRunning = isRunning && activeAlankaarIdx == index
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, if (isThisRunning) accentColor else Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(alankaar.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor)
                        Text(alankaar.pattern, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, color = Color.White)
                        Text(alankaar.description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    IconButton(
                        onClick = {
                            if (!isRunning) {
                                isRunning = true
                                activeAlankaarIdx = index
                                val engine = getAudioEngine()
                                scope.launch {
                                    try {
                                        val patternSemitones = alankaar.midiPattern.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11, 12, 11, 9, 7, 5, 4, 2, 0) }
                                        patternSemitones.forEach { semi ->
                                            val note = 60 + semi
                                            viewModel?.midiPlayingNote?.value = note
                                            engine.noteOn(note, 0.85f)
                                            delay(320L)
                                            engine.noteOff(note)
                                        }
                                    } finally {
                                        viewModel?.midiPlayingNote?.value = null
                                        isRunning = false
                                        activeAlankaarIdx = -1
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp).background(accentColor.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(if (isThisRunning) Icons.Default.GraphicEq else Icons.Default.PlayArrow, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IndianTablaBolsPanel(accentColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tabla Syllables & Finger Articulations", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        INDIAN_TABLA_BOLS.forEach { (bol, desc) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bol,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    modifier = Modifier.width(90.dp)
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlayableInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    semitones: List<Int>,
    accentColor: Color,
    scope: CoroutineScope,
    viewModel: AppViewModel?
) {
    var isRowPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Column {
                Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        IconButton(
            onClick = {
                if (!isRowPlaying) {
                    isRowPlaying = true
                    val engine = getAudioEngine()
                    scope.launch {
                        try {
                            val notes = if (semitones.isNotEmpty()) semitones else listOf(0, 2, 4, 5, 7, 9, 11, 12)
                            notes.forEach { semi ->
                                val note = 60 + semi
                                viewModel?.midiPlayingNote?.value = note
                                engine.noteOn(note, 0.85f)
                                delay(350L)
                                engine.noteOff(note)
                            }
                        } finally {
                            viewModel?.midiPlayingNote?.value = null
                            isRowPlaying = false
                        }
                    }
                }
            },
            modifier = Modifier.size(28.dp).background(accentColor.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                if (isRowPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MetaChip(label: String, value: String, chipColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = chipColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = chipColor)
        }
    }
}