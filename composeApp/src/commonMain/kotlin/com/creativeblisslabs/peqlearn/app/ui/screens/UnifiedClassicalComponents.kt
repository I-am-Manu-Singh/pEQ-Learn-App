package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.MusicConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified Classical Raaga Browser component.
 * Reuses the superior KarnaticMelakartaBrowser UI design for both Hindustani and Carnatic traditions.
 * All chip selections are replaced with sleek Dropdown menus.
 */
@Composable
fun UnifiedRaagaBrowser(
    viewModel: AppViewModel? = null,
    accentColor: Color,
    isCarnatic: Boolean = false,
    onSelectRaga: (Raaga) -> Unit = {}
) {
    val allRagas = viewModel?.ragaList?.value ?: emptyList()
    val sourceList = remember(allRagas, isCarnatic) {
        if (isCarnatic) {
            val carnaticApi = allRagas.filter { it.system.equals("Carnatic", ignoreCase = true) }
            if (carnaticApi.isNotEmpty()) carnaticApi else CARNATIC_RAAGAS
        } else {
            val hindustaniApi = allRagas.filter { it.system.equals("Hindustani", ignoreCase = true) }
            if (hindustaniApi.isNotEmpty()) hindustaniApi else INDIAN_RAAGAS
        }
    }

    var selectedIdx by remember(isCarnatic) { mutableIntStateOf(0) }
    var selectedCategoryIdx by remember(isCarnatic) { mutableIntStateOf(0) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isRagaDropdownExpanded by remember { mutableStateOf(false) }
    var isAuditionRunning by remember { mutableStateOf(false) }
    var currentPlayingIdx by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    // Categories: 12 Chakras for Carnatic, 10 Thaats for Hindustani
    val categories = remember(isCarnatic) {
        if (isCarnatic) {
            listOf("Indu (1-6)", "Netra (7-12)", "Agni (13-18)", "Veda (19-24)", "Bana (25-30)", "Rutu (31-36)", "Rishi (37-42)", "Vasu (43-48)", "Brahma (49-54)", "Disi (55-60)", "Rudra (61-66)", "Aditya (67-72)")
        } else {
            INDIAN_THAATS.map { it.name }
        }
    }

    // Filter ragas based on category if applicable
    val displayRagas = remember(selectedCategoryIdx, sourceList, isCarnatic) {
        if (isCarnatic) {
            val cStart = selectedCategoryIdx * 6 + 1
            val cEnd = cStart + 5
            val filtered = sourceList.filter { (it.melakarta ?: 0) in cStart..cEnd }
            if (filtered.isNotEmpty()) filtered else sourceList
        } else {
            val targetThaat = categories.getOrNull(selectedCategoryIdx) ?: ""
            val filtered = sourceList.filter { it.thaat.equals(targetThaat, ignoreCase = true) }
            if (filtered.isNotEmpty()) filtered else sourceList
        }
    }

    val currentRaga = displayRagas.getOrNull(selectedIdx.coerceIn(0, (displayRagas.size - 1).coerceAtLeast(0)))
        ?: sourceList.firstOrNull()

    val activeTaalFramework = remember(currentRaga, isCarnatic) {
        if (isCarnatic) {
            CARNATIC_TALAS.find { it.name.equals(currentRaga?.taal, ignoreCase = true) } ?: CARNATIC_TALAS.first()
        } else {
            INDIAN_TAALS.find { it.name.equals(currentRaga?.taal, ignoreCase = true) } ?: INDIAN_TAALS.first()
        }
    }

    LaunchedEffect(currentRaga, activeTaalFramework) {
        currentRaga?.let {
            viewModel?.currentScore?.value = MusicConverter.convertRaagaToScore(it, 60, activeTaalFramework)
            onSelectRaga(it)
        }
    }

    val currentCategoryLabel = categories.getOrElse(selectedCategoryIdx) { if (isCarnatic) "Chakra" else "Thaat" }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Dropdown Selectors Row for Category & Rāga
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Category Dropdown (Chakra / Thaat)
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { isCategoryDropdownExpanded = true },
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
                            Text(if (isCarnatic) "Chakra" else "Thaat", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(
                                text = currentCategoryLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (isCategoryDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Category",
                            tint = accentColor
                        )
                    }
                }

                DropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFF141724))
                ) {
                    categories.forEachIndexed { cIdx, catName ->
                        val isSelected = selectedCategoryIdx == cIdx
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = catName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else Color.White
                                )
                            },
                            onClick = {
                                selectedCategoryIdx = cIdx
                                selectedIdx = 0
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Rāga Dropdown
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
                            Text(if (isCarnatic) "Melakarta Rāga" else "Rāga Selection", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(
                                text = currentRaga?.let {
                                    if (it.melakarta != null) "${it.melakarta}. ${it.name}" else it.name
                                } ?: "Select Rāga",
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
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFF141724))
                ) {
                    displayRagas.forEachIndexed { idx, raga ->
                        val isSelected = idx == selectedIdx
                        DropdownMenuItem(
                            text = {
                                val itemText = if (raga.melakarta != null) "${raga.melakarta}. ${raga.name}" else "${raga.name} (${raga.thaat})"
                                Text(
                                    text = itemText,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else Color.White
                                )
                            },
                            onClick = {
                                selectedIdx = idx
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
                            val titleText = if (raga.melakarta != null) "Melakarta #${raga.melakarta}: ${raga.name}" else raga.name
                            Text(
                                titleText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor
                            )
                            val subText = if (isCarnatic) {
                                "Chakra: ${raga.chakra ?: "N/A"} • Jati: ${raga.jati.ifEmpty { "Sampoorna" }}"
                            } else {
                                "Parent Thaat: ${raga.thaat} • Jati: ${raga.jati.ifEmpty { "Sampoorna" }}"
                            }
                            Text(
                                subText,
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
                                            val scaleNotes = if (raga.semitones.isNotEmpty()) raga.semitones else listOf(0, 2, 4, 5, 7, 9, 11, 12)
                                            scaleNotes.forEachIndexed { idx, semi ->
                                                currentPlayingIdx = idx
                                                val note = 60 + semi
                                                viewModel?.let {
                                                    it.scorePlayheadMeasure.value = idx / activeTaalFramework.beats
                                                    it.scorePlayheadOffset.value = (idx % activeTaalFramework.beats).toFloat() / activeTaalFramework.beats.toFloat()
                                                    it.midiPlayingNote.value = note
                                                }
                                                engine.noteOn(note, 0.85f)
                                                delay(360L)
                                                engine.noteOff(note)
                                            }
                                        } finally {
                                            currentPlayingIdx = -1
                                            viewModel?.let {
                                                it.midiPlayingNote.value = null
                                                it.scorePlayheadMeasure.value = 0
                                                it.scorePlayheadOffset.value = 0f
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
                        MetaChip("Vadi", raga.vadi.ifEmpty { "Sa" }, Color(0xFF64B5F6))
                        MetaChip("Samvadi", raga.samvadi.ifEmpty { "Pa" }, Color(0xFF81C784))
                        MetaChip("Time", raga.time.ifEmpty { "Any" }, Color(0xFFFFB74D))
                        MetaChip("Mood", raga.mood.ifEmpty { "Devotional" }, Color(0xFFE57373))
                    }

                    PlayableInfoRowShared(
                        icon = Icons.Default.TrendingUp,
                        label = if (isCarnatic) "Arohanam (Ascent)" else "Aroha (Ascent)",
                        value = raga.aroha,
                        semitones = raga.semitones,
                        accentColor = accentColor,
                        scope = scope,
                        viewModel = viewModel
                    )

                    PlayableInfoRowShared(
                        icon = Icons.Default.TrendingDown,
                        label = if (isCarnatic) "Avarohanam (Descent)" else "Avaroha (Descent)",
                        value = raga.avaroha,
                        semitones = raga.semitones.reversed(),
                        accentColor = accentColor,
                        scope = scope,
                        viewModel = viewModel
                    )

                    if (raga.pakad.isNotEmpty()) {
                        PlayableInfoRowShared(Icons.Default.Star, "Pakad (Signature Phrase)", raga.pakad, raga.semitones, Color(0xFFFFD54F), scope, viewModel)
                    }
                    if (raga.chalan.isNotEmpty()) {
                        PlayableInfoRowShared(Icons.Default.LinearScale, "Chalan (Movement Flow)", raga.chalan, raga.semitones, Color(0xFF81C784), scope, viewModel)
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
                        currentPlayingMidi = viewModel?.midiPlayingNote?.value,
                        accentColor = accentColor,
                        onPlayNote = { midiNote: Int ->
                            viewModel?.midiPlayingNote?.value = midiNote
                            getAudioEngine().noteOn(midiNote, 0.85f)
                        },
                        onStopNote = { midiNote: Int ->
                            getAudioEngine().noteOff(midiNote)
                            if (viewModel?.midiPlayingNote?.value == midiNote) {
                                viewModel.midiPlayingNote.value = null
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Unified Classical Taal Browser component.
 * Serves both Hindustani Indian Taals and Carnatic Suladi Sapta Talas.
 * Features full dropdown selection and interactive bol playback matrix.
 */
@Composable
fun UnifiedTaalBrowser(
    viewModel: AppViewModel? = null,
    accentColor: Color,
    isCarnatic: Boolean = false
) {
    var selectedIdx by remember(isCarnatic) { mutableStateOf(0) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val taals = if (isCarnatic) CARNATIC_TALAS else INDIAN_TAALS
    val currentTaal = taals.getOrNull(selectedIdx) ?: taals.first()
    var currentBeatPlayhead by remember { mutableIntStateOf(-1) }
    var isTaalLooping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isCarnatic) "The Suladi Sapta Tala System (Dhruva, Matya, Rupaka, Jhampa, Triputa, Ata, Eka)"
            else "The Foundational Tāla Systems of Hindustani Classical Music",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        // Dropdown Selector for Taals
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
                        text = "Tāla: ${currentTaal.name} (${currentTaal.beats} ${if (isCarnatic) "aksharas" else "matras"})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Icon(
                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Tāla",
                        tint = accentColor
                    )
                }
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFF141724))
            ) {
                taals.forEachIndexed { idx, t ->
                    val isSelected = idx == selectedIdx
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${t.name} (${t.beats} ${if (isCarnatic) "aksharas" else "m"})",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else Color.White
                            )
                        },
                        onClick = {
                            selectedIdx = idx
                            isTaalLooping = false
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentTaal.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Text(
                            text = "${currentTaal.beats} Beats • Vibhag: ${currentTaal.vibhags.joinToString("+")} • Tempo: ${currentTaal.tempo}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
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
                                            currentTaal.bolSequence.forEachIndexed { bIdx, bol ->
                                                if (!isTaalLooping) return@forEachIndexed
                                                currentBeatPlayhead = bIdx
                                                engine.playSample(bol, bIdx, "TABLA")
                                                delay(450L)
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
                        Text(if (isTaalLooping) "Stop Loop" else "Play Loop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(if (isCarnatic) "Akshara Solkattu Matrix:" else "Theka Matra Matrix:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(currentTaal.bolSequence) { bIdx, bol ->
                        val isCurrentBeat = currentBeatPlayhead == bIdx
                        val isSam = bIdx == 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isCurrentBeat -> Color(0xFFFFD54F)
                                isSam -> accentColor.copy(alpha = 0.35f)
                                else -> Color.White.copy(alpha = 0.05f)
                            },
                            border = BorderStroke(1.dp, if (isSam) accentColor else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable {
                                getAudioEngine().playSample(bol, bIdx, "TABLA")
                            }
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

                Text(currentTaal.description, fontSize = 12.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

/**
 * Unified Classical Thaat / Parent Scale Browser component.
 * Displays foundational parent scales with full dropdown selector and audio auditioning.
 */
@Composable
fun UnifiedThaatBrowser(
    viewModel: AppViewModel? = null,
    accentColor: Color
) {
    var selectedIdx by remember { mutableStateOf(0) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentThaat = INDIAN_THAATS.getOrNull(selectedIdx) ?: INDIAN_THAATS.first()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("The 10 Foundational Parent Thaats of Hindustani Classical Music", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))

        // Dropdown Selector for Thaats
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
                        text = "Thaat: ${currentThaat.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Icon(
                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Thaat",
                        tint = accentColor
                    )
                }
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFF141724))
            ) {
                INDIAN_THAATS.forEachIndexed { idx, thaat ->
                    val isSelected = idx == selectedIdx
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = thaat.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else Color.White
                            )
                        },
                        onClick = {
                            selectedIdx = idx
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

/**
 * Unified Classical Alankaar / Alankaram Panel.
 * Handles both Hindustani graded sequences and Carnatic Suladi Sapta Tala Alankarams.
 */
@Composable
fun UnifiedAlankaarPanel(
    viewModel: AppViewModel? = null,
    accentColor: Color,
    isCarnatic: Boolean = false
) {
    var isRunning by remember { mutableStateOf(false) }
    var activeIdx by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val alankaarList = if (isCarnatic) CARNATIC_ALANKARAS else INDIAN_ALAANKARS

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isCarnatic) "35 Suladi Sapta Tala Alankarams in 3 Kaalams (Speeds)"
            else "${INDIAN_ALAANKARS.size} Graded Vocal/Instrumental Exercise Sequences",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        alankaarList.take(15).forEachIndexed { idx, alankaram ->
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
                                        val patternSemitones = alankaram.midiPattern.ifEmpty {
                                            if (isCarnatic) listOf(0, 1, 4, 5, 7, 8, 11, 12, 11, 8, 7, 5, 4, 1, 0)
                                            else listOf(0, 2, 4, 5, 7, 9, 11, 12, 11, 9, 7, 5, 4, 2, 0)
                                        }
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
fun PlayableInfoRowShared(
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
fun MetaChip(label: String, value: String, chipColor: Color) {
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

@Composable
fun SwaraSthanasVisualizer(
    activeSemitones: List<Int>,
    currentPlayingMidi: Int?,
    accentColor: Color,
    onPlayNote: (Int) -> Unit,
    onStopNote: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val swaraSthanasLabels = listOf("S", "R1", "R2/G1", "R3/G2", "G3", "M1", "M2", "P", "D1", "D2/N1", "D3/N2", "N3", "S'")
    val verifiedSemitones = activeSemitones.ifEmpty { listOf(0, 1, 4, 5, 7, 8, 11, 12) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SWARA STHANAS / INTERACTIVE SCALE MAP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = accentColor.copy(alpha = 0.9f),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color(0xFF0D0F15), RoundedCornerShape(16.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (semi in 0..12) {
                val isIncluded = verifiedSemitones.contains(semi) || (semi == 12 && verifiedSemitones.contains(0))
                val isBlackKey = listOf(1, 3, 6, 8, 10).contains(semi % 12)
                val noteMidi = 60 + semi
                val isPlaying = currentPlayingMidi == noteMidi

                val keyBackground = when {
                    isPlaying -> Color(0xFFFFD54F)
                    isIncluded && isBlackKey -> accentColor.copy(alpha = 0.85f)
                    isIncluded -> accentColor.copy(alpha = 0.25f)
                    isBlackKey -> Color(0xFF1A1D26)
                    else -> Color.White.copy(alpha = 0.04f)
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
                    val currentLabel = swaraSthanasLabels[semi]
                    val containsSlash = currentLabel.contains("/")

                    Column(
                        modifier = Modifier.padding(bottom = 8.dp, start = 1.dp, end = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (containsSlash) {
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
    var showTranslation by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (showTranslation) 180f else 0f)

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

    val sthaiLines = selectedRaga.sthai.ifEmpty { "Traditional Classical Verse\nSthai Line 2" }.split("\n")
    val sthaiTrans = parseSectionLines(selectedRaga.transliteration, "Sthai")
    val sthaiMean = parseSectionLines(selectedRaga.meaning, "Sthai")

    val antaraLines = selectedRaga.antara.ifEmpty { "Second Movement Verse\nAntara Line 2" }.split("\n")
    val antaraTrans = parseSectionLines(selectedRaga.transliteration, "Antara")
    val antaraMean = parseSectionLines(selectedRaga.meaning, "Antara")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Composition / Bandish / Kriti",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = selectedRaga.bandish.ifEmpty { "Classical Composition" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

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

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Sthai / Pallavi",
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
                    Text(text = line.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
                    val lineTrans = sthaiTrans.getOrNull(index) ?: sthaiTrans.firstOrNull()
                    if (!lineTrans.isNullOrEmpty()) {
                        Text(text = lineTrans, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.6f))
                    }
                    val lineMean = sthaiMean.getOrNull(index) ?: sthaiMean.firstOrNull()
                    AnimatedVisibility(visible = showTranslation && !lineMean.isNullOrEmpty()) {
                        if (lineMean != null) {
                            Text(text = lineMean, fontSize = 11.sp, color = Color(0xFFFFD54F).copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Antara / Charanam",
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
                    Text(text = line.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
                    val lineTrans = antaraTrans.getOrNull(index) ?: antaraTrans.firstOrNull()
                    if (!lineTrans.isNullOrEmpty()) {
                        Text(text = lineTrans, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.6f))
                    }
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


