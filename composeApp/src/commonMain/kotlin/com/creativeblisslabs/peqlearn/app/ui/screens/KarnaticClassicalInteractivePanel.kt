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
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedGuitarFretboard
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.MusicConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun KarnaticClassicalInteractivePanel(viewModel: AppViewModel) {
    val karnaticColor = Color(0xFF00BCD4) // Vibrant Cyan matching Karnatic theme
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("72 Melakartas", "Janya Ragas", "Sapta Talas", "35 Alankarams", "Solkattu")
    var notationFormat by remember { mutableStateOf("Staff Notation") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = karnaticColor,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = karnaticColor
                    )
                }
            }
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick = {
                        selectedTab = i
                        viewModel.currentScore.value = null
                    },
                    text = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> KarnaticMelakartaBrowser(viewModel, karnaticColor)
                1 -> KarnaticRagaBrowser(viewModel, karnaticColor)
                2 -> KarnaticTalaBrowser(viewModel, karnaticColor)
                3 -> KarnaticAlankaarPanel(viewModel, karnaticColor)
                4 -> KarnaticSolkattuPanel(karnaticColor)
            }

            // Notation Viewer linked to viewModel.currentScore
            StaffAndTabNotationViewer(
                score = viewModel.currentScore.value as? Score,
                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                playheadOffset = viewModel.scorePlayheadOffset.value,
                activeMidiNotes = viewModel.midiActiveNotes,
                playingNote = viewModel.midiPlayingNote.value,
                primaryColor = karnaticColor,
                currentFormat = notationFormat,
                onFormatChange = { notationFormat = it },
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
    }
}

@Composable
fun KarnaticMelakartaBrowser(
    viewModel: AppViewModel,
    accentColor: Color
) {
    var selectedMelakartaIdx by remember { mutableIntStateOf(0) }
    var isAuditionRunning by remember { mutableStateOf(false) }
    var currentPlayingIdx by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    val melakartaList = remember { CARNATIC_RAAGAS.filter { it.melakarta != null }.sortedBy { it.melakarta } }
    val currentRaga = melakartaList.getOrNull(selectedMelakartaIdx.coerceIn(0, (melakartaList.size - 1).coerceAtLeast(0)))
    val chakras = listOf("Indu (1-6)", "Netra (7-12)", "Agni (13-18)", "Veda (19-24)", "Bana (25-30)", "Rutu (31-36)", "Rishi (37-42)", "Vasu (43-48)", "Brahma (49-54)", "Disi (55-60)", "Rudra (61-66)", "Aditya (67-72)")

    val activeTaalFramework = remember {
        CARNATIC_TALAS.find { it.name.equals(currentRaga?.taal, ignoreCase = true) } ?: CARNATIC_TALAS.first()
    }

    LaunchedEffect(currentRaga, activeTaalFramework) {
        currentRaga?.let {
            viewModel.currentScore.value = MusicConverter.convertRaagaToScore(it, 60, activeTaalFramework)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("12 Chakras (72 Janaka Melakarta Scheme):", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(chakras) { cIdx, chakraName ->
                val isChakraSelected = selectedMelakartaIdx / 6 == cIdx
                FilterChip(
                    selected = isChakraSelected,
                    onClick = { selectedMelakartaIdx = cIdx * 6 },
                    label = { Text(chakraName, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.25f),
                        selectedLabelColor = accentColor
                    )
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(melakartaList) { idx, raga ->
                val isSelected = idx == selectedMelakartaIdx
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedMelakartaIdx = idx },
                    label = { Text("${raga.melakarta}. ${raga.name}", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.25f),
                        selectedLabelColor = accentColor
                    )
                )
            }
        }

        currentRaga?.let { raga ->
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
                            Text(
                                "Melakarta #${raga.melakarta}: ${raga.name}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor
                            )
                            Text(
                                "Chakra: ${raga.chakra ?: "N/A"} • Jati: ${raga.jati.ifEmpty { "Sampoorna" }}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = {
                                if (!isAuditionRunning) {
                                    isAuditionRunning = true
                                    val engine = getAudioEngine()
                                    scope.launch {
                                        try {
                                            val scaleNotes = if (raga.semitones.isNotEmpty()) raga.semitones else listOf(0, 1, 4, 5, 7, 8, 11, 12)
                                            scaleNotes.forEachIndexed { idx, semi ->
                                                currentPlayingIdx = idx
                                                val note = 60 + semi
                                                viewModel.scorePlayheadMeasure.value = idx / activeTaalFramework.beats
                                                viewModel.scorePlayheadOffset.value = (idx % activeTaalFramework.beats).toFloat() / activeTaalFramework.beats.toFloat()
                                                viewModel.midiPlayingNote.value = note
                                                engine.noteOn(note, 0.85f)
                                                delay(360L)
                                                engine.noteOff(note)
                                            }
                                        } finally {
                                            currentPlayingIdx = -1
                                            viewModel.midiPlayingNote.value = null
                                            viewModel.scorePlayheadMeasure.value = 0
                                            viewModel.scorePlayheadOffset.value = 0f
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
                            Text(if (isAuditionRunning) "Playing..." else "Audition Scale", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    KarnaticPlayableInfoRow(Icons.Default.TrendingUp, "Arohanam (Ascent)", raga.aroha, raga.semitones, accentColor, scope, viewModel)
                    KarnaticPlayableInfoRow(Icons.Default.TrendingDown, "Avarohanam (Descent)", raga.avaroha, raga.semitones.reversed(), accentColor, scope, viewModel)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetaChip("Vadi", raga.vadi.ifEmpty { "S" }, Color(0xFF64B5F6))
                        MetaChip("Samvadi", raga.samvadi.ifEmpty { "P" }, Color(0xFF81C784))
                        MetaChip("Time", raga.time.ifEmpty { "Any" }, Color(0xFFFFB74D))
                        MetaChip("Mood", raga.mood.ifEmpty { "Devotional" }, Color(0xFFE57373))
                    }

                    if (raga.pakad.isNotEmpty()) {
                        Text("Pakad (Signature Phrase): ${raga.pakad}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    if (raga.chalan.isNotEmpty()) {
                        Text("Chalan (Movement Flow): ${raga.chalan}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF81C784))
                    }
                    if (raga.alaap.isNotEmpty()) {
                        Text("Alaap Phrases: ${raga.alaap.joinToString(" | ")}", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.8f))
                    }
                    if (raga.taansPaltas.isNotEmpty()) {
                        Text("Taans / Swara Patterns: ${raga.taansPaltas.joinToString(" | ")}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    if (raga.jhala.isNotEmpty()) {
                        Text("Jhala / Tihai Pattern: ${raga.jhala}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    if (raga.bolTaal.isNotEmpty()) {
                        Text("Bol / Tala Pattern: ${raga.bolTaal}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }

                    if (raga.description.isNotEmpty()) {
                        Text(
                            raga.description,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Text(
                        "CARNATIC SWARA STHANAS (S, R, G, M, P, D, N)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )

                    val karnaticSwaraSthanas = listOf("S", "R1", "R2/G1", "R3/G2", "G3", "M1", "M2", "P", "D1", "D2/N1", "D3/N2", "N3", "S'")
                    val activeSemitones = if (raga.semitones.isNotEmpty()) raga.semitones else listOf(0, 1, 4, 5, 7, 8, 11, 12)

                    Row(
                        modifier = Modifier.fillMaxWidth().height(90.dp).background(Color(0xFF0D0F15), RoundedCornerShape(12.dp)).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (semi in 0..12) {
                            val isIncluded = activeSemitones.contains(semi) || (semi == 12 && activeSemitones.contains(0))
                            val isBlackKey = listOf(1, 3, 6, 8, 10).contains(semi % 12)
                            val noteMidi = 60 + semi
                            val isPlaying = viewModel.midiPlayingNote.value == noteMidi

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isPlaying -> Color(0xFFFFD54F)
                                            isIncluded && isBlackKey -> accentColor.copy(alpha = 0.85f)
                                            isIncluded -> accentColor.copy(alpha = 0.4f)
                                            isBlackKey -> Color(0xFF1E222D)
                                            else -> Color.White.copy(alpha = 0.1f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isIncluded) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .pointerInput(noteMidi) {
                                        detectTapGestures(
                                            onPress = {
                                                val engine = getAudioEngine()
                                                viewModel.midiPlayingNote.value = noteMidi
                                                engine.noteOn(noteMidi, 0.85f)
                                                tryAwaitRelease()
                                                engine.noteOff(noteMidi)
                                                viewModel.midiPlayingNote.value = null
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = karnaticSwaraSthanas[semi],
                                        fontSize = 9.sp,
                                        fontWeight = if (isIncluded) FontWeight.Black else FontWeight.Normal,
                                        color = if (isPlaying) Color.Black else if (isIncluded) Color.White else Color.White.copy(alpha = 0.3f)
                                    )
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
fun KarnaticRagaBrowser(
    viewModel: AppViewModel,
    accentColor: Color
) {
    var searchQuery by remember { mutableStateOf("") }
    val allRagas = viewModel.ragaList.value.ifEmpty { CARNATIC_RAAGAS }
    val mappedRagas = remember(allRagas) {
        allRagas.filter { it.system.equals("Carnatic", ignoreCase = true) }
    }
    val sourceList = if (mappedRagas.isNotEmpty()) mappedRagas else CARNATIC_RAAGAS
    val filteredRagas = remember(searchQuery, sourceList) {
        if (searchQuery.isEmpty()) sourceList
        else sourceList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.thaat.contains(searchQuery, ignoreCase = true)
        }
    }

    var selectedRaga by remember(filteredRagas) { mutableStateOf(filteredRagas.firstOrNull() ?: CARNATIC_RAAGAS.first()) }
    var selectedDetailsTab by remember { mutableIntStateOf(0) }
    var visualizerMode by remember { mutableIntStateOf(0) }
    var isAuditionRunning by remember { mutableStateOf(false) }
    var currentPlayingIdx by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val completePattern = selectedRaga.semitones

    val activeTaalFramework = remember(selectedRaga) {
        CARNATIC_TALAS.find { it.name.equals(selectedRaga.taal, ignoreCase = true) } ?: CARNATIC_TALAS.first()
    }

    LaunchedEffect(selectedRaga, activeTaalFramework) {
        viewModel.currentScore.value = MusicConverter.convertRaagaToScore(selectedRaga, 60, activeTaalFramework)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Carnatic Janya & Melakarta ragas...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredRagas.take(35)) { raga ->
                val isSelected = raga.name == selectedRaga.name
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedRaga = raga },
                    label = { Text(raga.name, fontSize = 11.sp) },
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedRaga.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = accentColor)
                    if (selectedRaga.melakarta != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.2f)) {
                            Text("Melakarta #${selectedRaga.melakarta}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                }

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
                    listOf("Details & Svaras", "Kriti / Composition", "Advanced Data").forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedDetailsTab == idx,
                            onClick = { selectedDetailsTab = idx },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedDetailsTab == idx) accentColor else Color.White.copy(alpha = 0.6f)) }
                        )
                    }
                }

                when (selectedDetailsTab) {
                    0 -> {
                        KarnaticPlayableInfoRow(Icons.Default.TrendingUp, "Arohanam", selectedRaga.aroha, selectedRaga.semitones, accentColor, scope, viewModel)
                        KarnaticPlayableInfoRow(Icons.Default.TrendingDown, "Avarohanam", selectedRaga.avaroha, selectedRaga.semitones.reversed(), accentColor, scope, viewModel)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetaChip("Vadi", selectedRaga.vadi.ifEmpty { "S" }, Color(0xFF64B5F6))
                            MetaChip("Samvadi", selectedRaga.samvadi.ifEmpty { "P" }, Color(0xFF81C784))
                            MetaChip("Taal", selectedRaga.taal.ifEmpty { "Adi" }, Color(0xFFFFB74D))
                            MetaChip("Jati", selectedRaga.jati.ifEmpty { "Sampoorna" }, Color(0xFFE57373))
                        }
                        if (selectedRaga.pakad.isNotEmpty()) {
                            Text("Pakad: ${selectedRaga.pakad}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                        }
                        if (selectedRaga.description.isNotEmpty()) {
                            Text(selectedRaga.description, fontSize = 12.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Composition Name: ${selectedRaga.bandish.ifEmpty { "Traditional Kriti" }}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            Text("Pallavi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            Text(selectedRaga.sthai.ifEmpty { "Vatapi Ganapatim Bhajeham\nVaranaasya Prapannartiharam" }, fontSize = 13.sp, lineHeight = 20.sp, color = Color.White)
                            Text("Charanam / Antara:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                            Text(selectedRaga.antara.ifEmpty { "Bhutadi Samsevita Charanam\nBhutabhautika Prapancha Bharanam" }, fontSize = 13.sp, lineHeight = 20.sp, color = Color.White)
                            if (selectedRaga.transliteration.isNotEmpty()) {
                                Text("Transliteration: ${selectedRaga.transliteration}", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.7f))
                            }
                            if (selectedRaga.meaning.isNotEmpty()) {
                                Text("Meaning: ${selectedRaga.meaning}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Chalan: ${selectedRaga.chalan.ifEmpty { "N/A" }}", fontSize = 12.sp, color = Color.White)
                            Text("Alaap Phrases: ${if (selectedRaga.alaap.isNotEmpty()) selectedRaga.alaap.joinToString(", ") else "N/A"}", fontSize = 12.sp, color = Color.White)
                            Text("Taans / Paltas: ${if (selectedRaga.taansPaltas.isNotEmpty()) selectedRaga.taansPaltas.joinToString(", ") else "N/A"}", fontSize = 12.sp, color = Color.White)
                            Text("Jhala / Tihai: ${selectedRaga.jhala.ifEmpty { "N/A" }}", fontSize = 12.sp, color = Color.White)
                            Text("Bol Taal Pattern: ${selectedRaga.bolTaal.ifEmpty { "N/A" }}", fontSize = 12.sp, color = Color.White)
                            Text("Stream URL: ${selectedRaga.streamUrl ?: "None"}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("Video URL: ${selectedRaga.videoUrl ?: "None"}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("Accent Color: ${selectedRaga.accentColor ?: "Default"}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scale Visualizer Map",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("🎹 Piano", "🎸 Guitar").forEachIndexed { index, modeLabel ->
                            FilterChip(
                                selected = visualizerMode == index,
                                onClick = { visualizerMode = index },
                                label = { Text(modeLabel, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.25f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }

                val dynamicPlayingSemitone = if (currentPlayingIdx in completePattern.indices) completePattern[currentPlayingIdx] else -1

                if (visualizerMode == 0) {
                    val activeMidiMap = remember(completePattern, dynamicPlayingSemitone) {
                        if (dynamicPlayingSemitone != -1) mapOf((60 + dynamicPlayingSemitone) to true)
                        else completePattern.associate { (60 + it) to true }
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
                    val highlightedGuitarList = remember(completePattern, dynamicPlayingSemitone) {
                        if (dynamicPlayingSemitone != -1) listOf(dynamicPlayingSemitone) else completePattern
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
            }
        }
    }
}

@Composable
fun KarnaticTalaBrowser(viewModel: AppViewModel, accentColor: Color) {
    var selectedIdx by remember { mutableStateOf(0) }
    val selectedTala = CARNATIC_TALAS.getOrNull(selectedIdx) ?: CARNATIC_TALAS.first()
    var currentBeatPlayhead by remember { mutableIntStateOf(-1) }
    var isTalaLooping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("The Suladi Sapta Tala System (Dhruva, Matya, Rupaka, Jhampa, Triputa, Ata, Eka)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(CARNATIC_TALAS) { idx, tala ->
                val isSelected = idx == selectedIdx
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedIdx = idx
                        isTalaLooping = false
                        currentBeatPlayhead = -1
                    },
                    label = { Text("${tala.name} (${tala.beats} aksharas)", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                        Text(selectedTala.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Text("${selectedTala.beats} Aksharas • Angas: ${selectedTala.vibhags.joinToString(" + ") { "${it}m" }}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }

                    Button(
                        onClick = {
                            if (isTalaLooping) {
                                isTalaLooping = false
                                currentBeatPlayhead = -1
                            } else {
                                isTalaLooping = true
                                val engine = getAudioEngine()
                                scope.launch {
                                    try {
                                        while (isTalaLooping) {
                                            selectedTala.bolSequence.forEachIndexed { bIdx, _ ->
                                                if (!isTalaLooping) return@forEachIndexed
                                                currentBeatPlayhead = bIdx
                                                val drumMidi = if (bIdx == 0) 36 else 38
                                                engine.noteOn(drumMidi, 0.9f)
                                                delay(480L)
                                                engine.noteOff(drumMidi)
                                            }
                                        }
                                    } finally {
                                        currentBeatPlayhead = -1
                                        isTalaLooping = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTalaLooping) Color(0xFFE53935) else accentColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(if (isTalaLooping) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isTalaLooping) "Stop Tala" else "Play Tala Loop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(selectedTala.bolSequence) { bIdx, bol ->
                        val isCurrentBeat = currentBeatPlayhead == bIdx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrentBeat) Color(0xFFFFD54F) else if (bIdx == 0) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (bIdx == 0) accentColor else Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${bIdx + 1}", fontSize = 9.sp, color = if (isCurrentBeat) Color.Black else Color.White.copy(alpha = 0.4f))
                                Text(bol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isCurrentBeat) Color.Black else Color.White)
                            }
                        }
                    }
                }

                Text(selectedTala.description, fontSize = 12.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
fun KarnaticAlankaarPanel(viewModel: AppViewModel, accentColor: Color) {
    var isRunning by remember { mutableStateOf(false) }
    var activeIdx by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("35 Suladi Sapta Tala Alankarams in 3 Kaalams (Speeds)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        CARNATIC_ALANKARAS.take(12).forEachIndexed { idx, alankaram ->
            val isThisRunning = isRunning && activeIdx == idx
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
                        Text(alankaram.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor)
                        Text(alankaram.pattern, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, color = Color.White)
                        Text(alankaram.description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }

                    IconButton(
                        onClick = {
                            if (!isRunning) {
                                isRunning = true
                                activeIdx = idx
                                val engine = getAudioEngine()
                                scope.launch {
                                    try {
                                        val patternSemitones = alankaram.midiPattern.ifEmpty { listOf(0, 1, 4, 5, 7, 8, 11, 12, 11, 8, 7, 5, 4, 1, 0) }
                                        patternSemitones.forEach { semi ->
                                            val note = 60 + semi
                                            viewModel.midiPlayingNote.value = note
                                            engine.noteOn(note, 0.85f)
                                            delay(320L)
                                            engine.noteOff(note)
                                        }
                                    } finally {
                                        viewModel.midiPlayingNote.value = null
                                        isRunning = false
                                        activeIdx = -1
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
fun KarnaticSolkattuPanel(accentColor: Color) {
    val solkattuList = listOf(
        Pair("Tha", "1-Akshara primary strike"),
        Pair("Dhi", "2nd syllable in Tisra / Chatusra gati"),
        Pair("Thom", "Resonant bass stroke on Mridangam Thoppi"),
        Pair("Nam", "Open resonant ringing strike on Dhimmu"),
        Pair("Tha-Ka", "2-Pulse rhythmic unit"),
        Pair("Tha-Ki-Ta", "Tisra Gati 3-beat rhythm"),
        Pair("Tha-Ka-Dhi-Mi", "Chatusra Gati 4-beat unit"),
        Pair("Tha-Ka-Tha-Ki-Ta", "Khanda Gati 5-beat rhythm"),
        Pair("Tha-Ki-Ta-Tha-Ka-Dhi-Mi", "Misra Gati 7-beat rhythm"),
        Pair("Tha-Ka-Dhi-Mi-Tha-Ka-Tha-Ki-Ta", "Sankeerna Gati 9-beat rhythm")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mridangam Solkattu Rhythmic Phrasing & Konnakol", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        solkattuList.forEach { (syllable, desc) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = syllable,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    modifier = Modifier.width(130.dp)
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
fun KarnaticPlayableInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    semitones: List<Int>,
    accentColor: Color,
    scope: CoroutineScope,
    viewModel: AppViewModel
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
                            val notes = if (semitones.isNotEmpty()) semitones else listOf(0, 1, 4, 5, 7, 8, 11, 12)
                            notes.forEach { semi ->
                                val note = 60 + semi
                                viewModel.midiPlayingNote.value = note
                                engine.noteOn(note, 0.85f)
                                delay(350L)
                                engine.noteOff(note)
                            }
                        } finally {
                            viewModel.midiPlayingNote.value = null
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