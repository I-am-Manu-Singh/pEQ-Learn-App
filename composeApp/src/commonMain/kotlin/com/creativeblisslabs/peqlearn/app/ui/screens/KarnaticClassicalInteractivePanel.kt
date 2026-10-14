package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
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
    val tabs = listOf("72 Melakartas", "Sapta Talas", "35 Alankarams", "Solkattu")
    var notationFormat by remember { mutableStateOf("Staff Notation") }

    var selectedRagaState by remember { mutableStateOf<Raaga?>(null) }
    var visualizerMode by remember { mutableIntStateOf(0) } // 0 = Piano, 1 = Guitar

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
                0 -> UnifiedRaagaBrowser(viewModel, karnaticColor, isCarnatic = true, onSelectRaga = { selectedRagaState = it })
                1 -> UnifiedTaalBrowser(viewModel, karnaticColor, isCarnatic = true)
                2 -> UnifiedAlankaarPanel(viewModel, karnaticColor, isCarnatic = true)
                3 -> KarnaticSolkattuPanel(karnaticColor)
            }

            // Shared Instrument Selection Row & Piano / Guitar Interactive Visualizer
            val activeSemitones = remember(selectedRagaState) {
                selectedRagaState?.semitones?.ifEmpty { listOf(0, 1, 4, 5, 7, 8, 11, 12) } ?: listOf(0, 1, 4, 5, 7, 8, 11, 12)
            }
            val playingNote = viewModel.midiPlayingNote.value

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTRUMENT SCALE VISUALIZER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = karnaticColor,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("🎹 Piano", "🎸 Guitar").forEachIndexed { idx, label ->
                        val isSelected = visualizerMode == idx
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) karnaticColor.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { visualizerMode = idx }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) karnaticColor else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (visualizerMode == 0) {
                val activeMidiMap = remember(activeSemitones, playingNote) {
                    if (playingNote != null) mapOf(playingNote to true)
                    else activeSemitones.associate { (60 + it) to true }
                }
                UnifiedPianoKeyboard(
                    activeMidiNotes = activeMidiMap,
                    accentColor = karnaticColor,
                    startNote = 60,
                    endNote = 72,
                    showNoteNames = true,
                    modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                val highlightedGuitarList = remember(activeSemitones, playingNote) {
                    if (playingNote != null) listOf(playingNote - 60) else activeSemitones
                }
                UnifiedGuitarFretboard(
                    numFrets = 12,
                    activeNotes = highlightedGuitarList,
                    rootNote = 0,
                    accentColor = karnaticColor,
                    showNoteNames = true,
                    modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(8.dp))
                )
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
fun KarnaticRagaBrowser(
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

    val activeTaalFramework = remember(currentRaga) {
        CARNATIC_TALAS.find { it.name.equals(currentRaga?.taal, ignoreCase = true) } ?: CARNATIC_TALAS.first()
    }

    LaunchedEffect(currentRaga, activeTaalFramework) {
        currentRaga?.let {
            viewModel.currentScore.value = MusicConverter.convertRaagaToScore(it, 60, activeTaalFramework)
        }
    }

    var isChakraDropdownExpanded by remember { mutableStateOf(false) }
    var isRagaDropdownExpanded by remember { mutableStateOf(false) }
    val selectedChakraIdx = selectedMelakartaIdx / 6
    val currentChakraName = chakras.getOrElse(selectedChakraIdx) { "Chakra" }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Dropdown Selectors Row for Chakra and Melakarta
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Chakra Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { isChakraDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chakra", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(
                                text = currentChakraName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (isChakraDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Chakra",
                            tint = accentColor
                        )
                    }
                }

                DropdownMenu(
                    expanded = isChakraDropdownExpanded,
                    onDismissRequest = { isChakraDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF141724))
                ) {
                    chakras.forEachIndexed { cIdx, chakraName ->
                        val isSelected = selectedChakraIdx == cIdx
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = chakraName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else Color.White
                                )
                            },
                            onClick = {
                                selectedMelakartaIdx = cIdx * 6
                                isChakraDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Melakarta Raga Dropdown
            Box(modifier = Modifier.weight(1.3f)) {
                OutlinedButton(
                    onClick = { isRagaDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Melakarta Rāga", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(
                                text = currentRaga?.let { "${it.melakarta}. ${it.name}" } ?: "Select Rāga",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (isRagaDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Rāga",
                            tint = accentColor
                        )
                    }
                }

                DropdownMenu(
                    expanded = isRagaDropdownExpanded,
                    onDismissRequest = { isRagaDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF141724))
                ) {
                    melakartaList.forEachIndexed { idx, raga ->
                        val isSelected = idx == selectedMelakartaIdx
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${raga.melakarta}. ${raga.name} (${raga.chakra ?: ""})",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else Color.White
                                )
                            },
                            onClick = {
                                selectedMelakartaIdx = idx
                                isRagaDropdownExpanded = false
                            }
                        )
                    }
                }
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
                        Column(modifier = Modifier.weight(1f)) {
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
                            Icon(
                                if (isAuditionRunning) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isAuditionRunning) "Playing..." else "Audition Scale", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetaChip("Vadi", raga.vadi.ifEmpty { "S" }, Color(0xFF64B5F6))
                        MetaChip("Samvadi", raga.samvadi.ifEmpty { "P" }, Color(0xFF81C784))
                        MetaChip("Time", raga.time.ifEmpty { "Any" }, Color(0xFFFFB74D))
                        MetaChip("Mood", raga.mood.ifEmpty { "Devotional" }, Color(0xFFE57373))
                    }

                    KarnaticPlayableInfoRow(
                        icon = Icons.Default.TrendingUp,
                        label = "Arohanam (Ascent)",
                        value = raga.aroha,
                        semitones = raga.semitones,
                        accentColor = accentColor,
                        scope = scope,
                        viewModel = viewModel
                    )

                    KarnaticPlayableInfoRow(
                        icon = Icons.Default.TrendingDown,
                        label = "Avarohanam (Descent)",
                        value = raga.avaroha,
                        semitones = raga.semitones.reversed(),
                        accentColor = accentColor,
                        scope = scope,
                        viewModel = viewModel
                    )

                    if (raga.pakad.isNotEmpty()) {
                        KarnaticPlayableInfoRow(Icons.Default.Star, "Pakad (Signature Phrase)", raga.pakad, raga.semitones, Color(0xFFFFD54F), scope, viewModel)
                    }
                    if (raga.chalan.isNotEmpty()) {
                        KarnaticPlayableInfoRow(Icons.Default.LinearScale, "Chalan (Movement Flow)", raga.chalan, raga.semitones, Color(0xFF81C784), scope, viewModel)
                    }

                    if (raga.description.isNotEmpty()) {
                        Text(
                            text = raga.description,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    BandishSection(
                        selectedRaga = raga,
                        accentColor = accentColor
                    )

                    SwaraSthanasVisualizer(
                        activeSemitones = raga.semitones,
                        currentPlayingMidi = viewModel.midiPlayingNote.value,
                        accentColor = accentColor,
                        onPlayNote = { midiNote ->
                            viewModel.midiPlayingNote.value = midiNote
                            getAudioEngine().noteOn(midiNote, 0.85f)
                        },
                        onStopNote = { midiNote ->
                            getAudioEngine().noteOff(midiNote)
                            if (viewModel.midiPlayingNote.value == midiNote) {
                                viewModel.midiPlayingNote.value = null
                            }
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun SwaraSthanasVisualizer(
    activeSemitones: List<Int>,
    currentPlayingMidi: Int?,
    accentColor: Color,
    onPlayNote: (Int) -> Unit,
    onStopNote: (Int) -> Unit,
    modifier: Modifier = Modifier // Fixed syntax typo here
) {
    // 12 semitones + high S' (index 12)
    val swaraSthanasLabels = listOf("S", "R1", "R2/G1", "R3/G2", "G3", "M1", "M2", "P", "D1", "D2/N1", "D3/N2", "N3", "S'")
    val verifiedSemitones = activeSemitones.ifEmpty { listOf(0, 1, 4, 5, 7, 8, 11, 12) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header with proper typography and alignment
        Text(
            text = "CARNATIC SWARA STHANAS (S, R, G, M, P, D, N)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = accentColor.copy(alpha = 0.9f),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // The Piano Roll Board Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp) // Slightly taller to allow long slash labels (R2/G1) to breathe
                .background(Color(0xFF0D0F15), RoundedCornerShape(16.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp), // Cleaner spacing between key segments
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (semi in 0..12) {
                val isIncluded = verifiedSemitones.contains(semi) || (semi == 12 && verifiedSemitones.contains(0))
                val isBlackKey = listOf(1, 3, 6, 8, 10).contains(semi % 12)
                val noteMidi = 60 + semi
                val isPlaying = currentPlayingMidi == noteMidi

                // Dynamic coloring configuration based on tone status
                val keyBackground = when {
                    isPlaying -> Color(0xFFFFD54F) // Active Playing State
                    isIncluded && isBlackKey -> accentColor.copy(alpha = 0.85f)
                    isIncluded -> accentColor.copy(alpha = 0.25f)
                    isBlackKey -> Color(0xFF1A1D26) // Piano Black Key Color
                    else -> Color.White.copy(alpha = 0.04f) // Inactive White Key Color
                }

                val borderTint = when {
                    isPlaying -> Color(0xFFFFD54F)
                    isIncluded -> accentColor.copy(alpha = 0.5f)
                    else -> Color.White.copy(alpha = 0.06f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(keyBackground)
                        .border(1.dp, borderTint, RoundedCornerShape(6.dp))
                        .pointerInput(noteMidi) {
                            detectTapGestures(
                                onPress = {
                                    try {
                                        onPlayNote(noteMidi)
                                        tryAwaitRelease()
                                    } finally {
                                        onStopNote(noteMidi)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Optimized Label Formatting to prevent truncation or overlap
                    val currentLabel = swaraSthanasLabels[semi]
                    val containsSlash = currentLabel.contains("/")

                    Column(
                        modifier = Modifier.padding(bottom = 8.dp, start = 1.dp, end = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (containsSlash) {
                            // Split labels (e.g. R2/G1) into stacked layout to avoid text clipping
                            val parts = currentLabel.split("/")
                            parts.forEach { part ->
                                Text(
                                    text = part,
                                    fontSize = 8.sp,
                                    lineHeight = 9.sp,
                                    fontWeight = if (isIncluded) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isPlaying) Color.Black else if (isIncluded) Color.White else Color.White.copy(alpha = 0.25f)
                                )
                            }
                        } else {
                            Text(
                                text = currentLabel,
                                fontSize = 9.sp,
                                fontWeight = if (isIncluded) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isPlaying) Color.Black else if (isIncluded) Color.White else Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BandishSection(
    selectedRaga: Raaga,
    accentColor: Color
) {
    // State to track if the English text meaning translation is visible
    var showTranslation by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (showTranslation) 180f else 0f)

    // Helper functions to parse and break down lines from the structural datasets
    fun parseSectionLines(fullText: String, prefix: String): List<String> {
        if (fullText.isEmpty()) return emptyList()
        val section = fullText.split("Antara:").map { it.trim() }
        val targetBlock = if (prefix == "Sthai") {
            section.firstOrNull()?.replace("Sthai:", "")?.trim() ?: ""
        } else {
            if (section.size > 1) section[1] else ""
        }
        return targetBlock.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Prepare text segments cleanly
    val sthaiLines = selectedRaga.sthai.ifEmpty { "Vatapi Ganapatim Bhajeham\nVaranaasya Prapannartiharam" }.split("\n")
    val sthaiTrans = parseSectionLines(selectedRaga.transliteration, "Sthai")
    val sthaiMean = parseSectionLines(selectedRaga.meaning, "Sthai")

    val antaraLines = selectedRaga.antara.ifEmpty { "Bhutadi Samsevita Charanam\nBhutabhautika Prapancha Bharanam" }.split("\n")
    val antaraTrans = parseSectionLines(selectedRaga.transliteration, "Antara")
    val antaraMean = parseSectionLines(selectedRaga.meaning, "Antara")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Composition Header and Control Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Composition Name",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = selectedRaga.bandish.ifEmpty { "Traditional Kriti" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            // Compact Pill Toggle Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { showTranslation = !showTranslation }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showTranslation) "Hide Meaning" else "Show Meaning",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Toggle Translation",
                    modifier = Modifier.size(14.dp).rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Sthai / Pallavi Segment Container
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Pallavi / Sthai",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF81C784),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            sthaiLines.forEachIndexed { index, line ->
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Native script line
                    Text(text = line.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
                    
                    // Linked Transliteration directly underneath line
                    val lineTrans = sthaiTrans.getOrNull(index) ?: sthaiTrans.firstOrNull()
                    if (!lineTrans.isNullOrEmpty()) {
                        Text(text = lineTrans, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.6f))
                    }
                    
                    // Linked English translation
                    val lineMean = sthaiMean.getOrNull(index) ?: sthaiMean.firstOrNull()
                    AnimatedVisibility(visible = showTranslation && !lineMean.isNullOrEmpty()) {
                        if (lineMean != null) {
                            Text(text = lineMean, fontSize = 11.sp, color = Color(0xFFFFD54F).copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // 3. Antara / Charanam Segment Container
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Charanam / Antara",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF64B5F6),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            antaraLines.forEachIndexed { index, line ->
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Native script line
                    Text(text = line.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
                    
                    // Linked Transliteration directly underneath line
                    val lineTrans = antaraTrans.getOrNull(index) ?: antaraTrans.firstOrNull()
                    if (!lineTrans.isNullOrEmpty()) {
                        Text(text = lineTrans, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.6f))
                    }
                    
                    // Linked English translation
                    val lineMean = antaraMean.getOrNull(index) ?: antaraMean.firstOrNull()
                    AnimatedVisibility(visible = showTranslation && !lineMean.isNullOrEmpty()) {
                        if (lineMean != null) {
                            Text(text = lineMean, fontSize = 11.sp, color = Color(0xFFFFD54F).copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun KarnaticTalaBrowser(viewModel: AppViewModel, accentColor: Color) {
    var selectedIdx by remember { mutableStateOf(0) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val selectedTala = CARNATIC_TALAS.getOrNull(selectedIdx) ?: CARNATIC_TALAS.first()
    var currentBeatPlayhead by remember { mutableIntStateOf(-1) }
    var isTalaLooping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("The Suladi Sapta Tala System (Dhruva, Matya, Rupaka, Jhampa, Triputa, Ata, Eka)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))

        // Dropdown Selector for Karnatic Talas
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { isDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tala: ${selectedTala.name} (${selectedTala.beats} aksharas)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Icon(
                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Tala",
                        tint = accentColor
                    )
                }
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(Color(0xFF141724))
            ) {
                CARNATIC_TALAS.forEachIndexed { idx, tala ->
                    val isSelected = idx == selectedIdx
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${tala.name} (${tala.beats} aksharas)",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else Color.White
                            )
                        },
                        onClick = {
                            selectedIdx = idx
                            isTalaLooping = false
                            currentBeatPlayhead = -1
                            isDropdownExpanded = false
                        }
                    )
                }
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
                                            selectedTala.bolSequence.forEachIndexed { bIdx, bol ->
                                                if (!isTalaLooping) return@forEachIndexed
                                                currentBeatPlayhead = bIdx
                                                engine.playSample(bol, bIdx, "TABLA")
                                                delay(480L)
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
                            border = BorderStroke(1.dp, if (bIdx == 0) accentColor else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable {
                                getAudioEngine().playSample(bol, bIdx, "TABLA")
                            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val engine = getAudioEngine()
                        engine.playSample(syllable, 0, "TABLA")
                    }
                    .padding(vertical = 4.dp),
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