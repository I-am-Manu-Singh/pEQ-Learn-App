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
import org.jetbrains.compose.resources.ExperimentalResourceApi
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.MusicConverter
import com.creativeblisslabs.peqlearn.app.ui.components.*
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
    accentColor: Color = MaterialTheme.colorScheme.primary,
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
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isCarnatic) "Chakra" else "Thaat", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(
                                text = currentCategoryLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (isCategoryDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Category",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    categories.forEachIndexed { cIdx, catName ->
                        val isSelected = selectedCategoryIdx == cIdx
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = catName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isCarnatic) "Melakarta Rāga" else "Rāga Selection", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(
                                text = currentRaga?.let {
                                    if (it.melakarta != null) "${it.melakarta}. ${it.name}" else it.name
                                } ?: "Select Rāga",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = if (isRagaDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Rāga",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isRagaDropdownExpanded,
                    onDismissRequest = { isRagaDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
            pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val subText = if (isCarnatic) {
                                "Chakra: ${raga.chakra ?: "N/A"} • Jati: ${raga.jati.ifEmpty { "Sampoorna" }}"
                            } else {
                                "Parent Thaat: ${raga.thaat} • Jati: ${raga.jati.ifEmpty { "Sampoorna" }}"
                            }
                            Text(
                                subText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                    InstrumentPlayButton(
                        accentColor = accentColor,
                        label = if (isAuditionRunning) "Playing..." else "Audition Scale",
                        isPlaying = isAuditionRunning,
                        onPlayClick = {
                            if (!isAuditionRunning) {
                                isAuditionRunning = true
                                val engine = getAudioEngine()
                                engine.setInstrument(ChordInstrumentType.HARMONIUM.soundEngineName)
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
                            } else {
                                isAuditionRunning = false
                                currentPlayingIdx = -1
                                viewModel?.let {
                                    it.midiPlayingNote.value = null
                                    it.scorePlayheadMeasure.value = 0
                                    it.scorePlayheadOffset.value = 0f
                                }
                            }
                        }
                    )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetaChip("Vadi", raga.vadi.ifEmpty { "Sa" }, MaterialTheme.colorScheme.primary)
                        MetaChip("Samvadi", raga.samvadi.ifEmpty { "Pa" }, MaterialTheme.colorScheme.secondary)
                        MetaChip("Time", raga.time.ifEmpty { "Any" }, MaterialTheme.colorScheme.tertiary)
                        MetaChip("Mood", raga.mood.ifEmpty { "Devotional" }, MaterialTheme.colorScheme.outline)
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
                        PlayableInfoRowShared(Icons.Default.Star, "Pakad (Signature Phrase)", raga.pakad, raga.semitones, MaterialTheme.colorScheme.tertiary, scope, viewModel)
                    }
                    if (raga.chalan.isNotEmpty()) {
                        PlayableInfoRowShared(Icons.Default.LinearScale, "Chalan (Movement Flow)", raga.chalan, raga.semitones, MaterialTheme.colorScheme.secondary, scope, viewModel)
                    }

                    if (raga.description.isNotEmpty()) {
                        Text(
                            text = raga.description,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

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
    accentColor: Color = MaterialTheme.colorScheme.primary,
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
            else "The Foundational & Advanced Tāla Systems of Hindustani Classical Music (Bhatkhande Notation)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Dropdown Selector for Taals
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { isDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Tāla",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                taals.forEachIndexed { idx, t ->
                    val isSelected = idx == selectedIdx
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${t.name} (${t.beats} ${if (isCarnatic) "aksharas" else "m"}) — ${t.origin}",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(currentTaal.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            if (currentTaal.jati.isNotEmpty()) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${currentTaal.jati} Jati",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (currentTaal.origin.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = currentTaal.origin,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${currentTaal.beats} Beats • Vibhag: ${currentTaal.vibhags.joinToString("+")} • Tempo: ${currentTaal.tempo}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    InstrumentPlayButton(
                        accentColor = accentColor,
                        label = if (isTaalLooping) "Stop Loop" else "Play Loop",
                        isPlaying = isTaalLooping,
                        onPlayClick = {
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
                        }
                    )
                }

                // Devanagari Theka Representation if available
                if (currentTaal.devanagariTheka.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ठेका (Devanagari Theka)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentTaal.devanagariTheka,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Interactive Bhatkhande Notation Matra Grid
                Text(
                    text = if (isCarnatic) "Akshara Solkattu Matrix:" else "Bhatkhande Matra Matrix (X = Sam, 0 = Khali, 2,3,4 = Taali):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(currentTaal.bolSequence) { bIdx, bol ->
                        val isCurrentBeat = currentBeatPlayhead == bIdx
                        val isSam = bIdx == 0
                        val isKhali = currentTaal.khaliIndices.contains(bIdx)
                        val isTali = currentTaal.taliIndices.contains(bIdx) && !isSam

                        val markerText = when {
                            isSam -> "X (Sam)"
                            isKhali -> "0 (Khali)"
                            isTali -> {
                                val taliNum = currentTaal.taliIndices.indexOf(bIdx) + 1
                                "${taliNum} (Taali)"
                            }
                            else -> "${bIdx + 1}"
                        }

                        pEQGlassCard(
                            modifier = Modifier
                                .widthIn(min = 64.dp)
                                .clickable {
                                    getAudioEngine().playSample(bol, bIdx, "TABLA")
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = markerText,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSam || isKhali || isTali) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isCurrentBeat -> MaterialTheme.colorScheme.tertiary
                                        isSam -> MaterialTheme.colorScheme.error
                                        isKhali -> MaterialTheme.colorScheme.secondary
                                        isTali -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    }
                                )
                                Text(
                                    text = bol,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentBeat) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Bhatkhande Full Notation Grid rendered in authentic Swarlipi sheet music card
                val cleanedNotation = remember(currentTaal.notationGrid) {
                    currentTaal.notationGrid.replace("\\n", "\n").trim()
                }
                if (cleanedNotation.isNotEmpty()) {
                    val notationLines = remember(cleanedNotation) {
                        cleanedNotation.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    val signsLine = notationLines.firstOrNull() ?: ""
                    val bolsLine = notationLines.getOrNull(1) ?: ""

                    val signsVibhags = remember(signsLine) {
                        signsLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    val bolsVibhags = remember(bolsLine) {
                        bolsLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    }

                    pEQGlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "TAAL SWARLIPI NOTATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = accentColor
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Bhatkhande System",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${currentTaal.name} (${currentTaal.beats} Beats)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            // Visual Vibhag grid with dividing bars ('|') matching SwarlipiNotationViewer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .background(
                                        color = Color(0xFF14171E).copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Left boundary bar
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(68.dp)
                                        .background(accentColor.copy(alpha = 0.7f))
                                )

                                val maxVibhags = maxOf(signsVibhags.size, bolsVibhags.size)
                                for (vIdx in 0 until maxVibhags) {
                                    val signStr = signsVibhags.getOrNull(vIdx) ?: ""
                                    val bolStr = bolsVibhags.getOrNull(vIdx) ?: ""
                                    val bolTokens = bolStr.split(Regex("\\s+")).filter { it.isNotBlank() }

                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        if (bolTokens.isNotEmpty()) {
                                            bolTokens.forEachIndexed { bSubIdx, token ->
                                                Column(
                                                    modifier = Modifier
                                                        .widthIn(min = 40.dp)
                                                        .padding(vertical = 2.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Taal Vibhag Sign (shown on 1st matra of vibhag)
                                                    Text(
                                                        text = if (bSubIdx == 0) signStr else " ",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (signStr.startsWith("X") || signStr.startsWith("✕")) Color(0xFFFF5252)
                                                        else if (signStr.startsWith("0") || signStr.startsWith("०")) Color(0xFF64B5F6)
                                                        else Color(0xFFFFB74D),
                                                        maxLines = 1,
                                                        textAlign = TextAlign.Center
                                                    )

                                                    // Bol syllable
                                                    pEQGlassCard(
                                                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                                    ) {
                                                        Text(
                                                            text = token,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = signStr,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFFFB74D)
                                                )
                                                Text(
                                                    text = bolStr,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Vibhag vertical division bar '|'
                                    Box(
                                        modifier = Modifier
                                            .width(if (vIdx == maxVibhags - 1) 2.5.dp else 1.5.dp)
                                            .height(68.dp)
                                            .background(
                                                if (vIdx == maxVibhags - 1) accentColor.copy(alpha = 0.8f)
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Text(currentTaal.description, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))

                // Traditional Theka Variations from Prof. Sudhir K. Varma
                if (currentTaal.variations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Traditional Variations & Forms (घराना भेद):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        currentTaal.variations.forEach { variation ->
                            Text(
                                text = "• $variation",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Layakaris (Thah, Dugun, Chaugun)
                if (currentTaal.layakaris.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Layakaris (लयकारी):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        currentTaal.layakaris.forEach { (laya, pattern) ->
                            Text(
                                text = "• $laya: $pattern",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
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
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var selectedIdx by remember { mutableStateOf(0) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentThaat = INDIAN_THAATS.getOrNull(selectedIdx) ?: INDIAN_THAATS.first()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("The 10 Foundational Parent Thaats of Hindustani Classical Music", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        // Dropdown Selector for Thaats
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { isDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                INDIAN_THAATS.forEachIndexed { idx, thaat ->
                    val isSelected = idx == selectedIdx
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = thaat.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
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

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentThaat.name + " Thaat", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    InstrumentPlayButton(
                        accentColor = accentColor,
                        label = "Play Thaat",
                        isPlaying = isPlaying,
                        onPlayClick = {
                            if (!isPlaying) {
                                isPlaying = true
                                val engine = getAudioEngine()
                                engine.setInstrument(ChordInstrumentType.HARMONIUM.soundEngineName)
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
                        }
                    )
                }

                // Swarlipi-style Swaras Grid with authentic Saptak & Komal indicators
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "THAAT SWARLIPI SCALE (भातखंडे स्वर लिपि)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(
                                color = Color(0xFF14171E).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        currentThaat.swaras.forEachIndexed { sIdx, swara ->
                            val targetNote = 60 + (currentThaat.semitones.getOrNull(sIdx) ?: 0)
                            val isSounding = viewModel?.midiPlayingNote?.value == targetNote
                            val isKomal = swara.any { it.isLowerCase() } && swara.uppercase() != "MA"
                            val isTeevra = swara.contains("Ma", ignoreCase = false) && currentThaat.semitones.getOrNull(sIdx) == 6

                            Column(
                                modifier = Modifier
                                    .widthIn(min = 44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSounding) Color(0xFFFFEB3B).copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Pitch number
                                Text(
                                    text = "${sIdx + 1}",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )

                                // Main Swara glyph
                                Text(
                                    text = if (isTeevra) "M'" else swara.uppercase().take(2),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSounding) Color(0xFFFFEB3B) else accentColor
                                )

                                // Komal underline indicator or dot
                                Box(
                                    modifier = Modifier.height(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isKomal) {
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height(2.dp)
                                                .background(if (isSounding) Color(0xFFFFEB3B) else accentColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(currentThaat.description, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }
        }
    }
}
/**
- Unified Classical Alankaar / Alankaram Panel.
- Handles both Hindustani graded sequences and Carnatic Suladi Sapta Tala Alankarams with precise real-time note highlighting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedAlankaarPanel(
    viewModel: AppViewModel? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isCarnatic: Boolean = false
) {
    var isRunning by remember { mutableStateOf(false) }
    var activeIdx by remember { mutableIntStateOf(-1) }
    var activeNoteIndex by remember { mutableIntStateOf(-1) }
    var selectedAlankarIdx by remember(isCarnatic) { mutableIntStateOf(0) }
    var selectedTypeFilter by remember(isCarnatic) { mutableStateOf("All") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fullList = if (isCarnatic) CARNATIC_ALANKARAS else INDIAN_ALAANKARS

    val availableTypes = remember(fullList) {
        listOf("All") + fullList.map { it.type.ifEmpty { "General" } }.distinct()
    }

    val filteredList = remember(selectedTypeFilter, fullList) {
        if (selectedTypeFilter == "All") fullList
        else fullList.filter { (it.type.ifEmpty { "General" }) == selectedTypeFilter }
    }

    val currentAlankaram = filteredList.getOrNull(selectedAlankarIdx.coerceIn(0, (filteredList.size - 1).coerceAtLeast(0)))
        ?: fullList.first()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isCarnatic) "35 Suladi Sapta Tala Alankarams in 3 Kaalams (Speeds)"
            else "${INDIAN_ALAANKARS.size} Graded Vocal/Instrumental Exercise Sequences",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Dropdown Selector for selecting specific Alankaar
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { isDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isCarnatic) "Alankaram Selection" else "Alankaar Selection", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = "${currentAlankaram.number}. ${currentAlankaram.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Alankaar",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                filteredList.forEachIndexed { idx, alnk ->
                    val isSelected = idx == selectedAlankarIdx
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${alnk.number}. ${alnk.name}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            selectedAlankarIdx = idx
                            isRunning = false
                            activeIdx = -1
                            activeNoteIndex = -1
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Filter chips row for Type / Technique
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableTypes.forEach { t ->
                val isSelected = selectedTypeFilter == t
                Surface(
                    onClick = {
                        selectedTypeFilter = t
                        selectedAlankarIdx = 0
                        isRunning = false
                        activeIdx = -1
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.height(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                        Text(
                            text = t,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Selected Alankaram Display Card (Cleaned and De-cluttered)
        val isThisRunning = isRunning && activeIdx == currentAlankaram.number

        val phraseGroups = remember(currentAlankaram.pattern) {
            currentAlankaram.pattern.split("|").map { group ->
                group.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            }.filter { it.isNotEmpty() }
        }
        val flatTokens = remember(phraseGroups) { phraseGroups.flatten() }

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentAlankaram.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = accentColor
                            )
                            if (currentAlankaram.type.isNotEmpty()) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = currentAlankaram.type,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (currentAlankaram.technique.isNotEmpty() && currentAlankaram.technique != "Sargam") {
                            Text(
                                text = "Technique: ${currentAlankaram.technique}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    InstrumentPlayButton(
                        accentColor = accentColor,
                        label = if (isThisRunning) "Stop" else "Practice",
                        isPlaying = isThisRunning,
                        onPlayClick = {
                            if (!isRunning) {
                                isRunning = true
                                activeIdx = currentAlankaram.number
                                activeNoteIndex = -1
                                val engine = getAudioEngine()
                                engine.setInstrument(ChordInstrumentType.HARMONIUM.soundEngineName)
                                scope.launch {
                                    try {
                                        val patternSemitones = currentAlankaram.midiPattern.ifEmpty {
                                            if (isCarnatic) listOf(0, 1, 4, 5, 7, 8, 11, 12, 11, 8, 7, 5, 4, 1, 0)
                                            else listOf(0, 2, 4, 5, 7, 9, 11, 12, 11, 9, 7, 5, 4, 2, 0)
                                        }
                                        val totalSteps = maxOf(flatTokens.size, patternSemitones.size)
                                        for (stepIdx in 0 until totalSteps) {
                                            if (!isRunning) return@launch
                                            activeNoteIndex = stepIdx % flatTokens.size
                                            val semi = patternSemitones[stepIdx % patternSemitones.size]
                                            val note = 60 + semi
                                            viewModel?.midiPlayingNote?.value = note
                                            engine.noteOn(note, 0.85f)
                                            delay(320L)
                                            engine.noteOff(note)
                                        }
                                    } finally {
                                        viewModel?.midiPlayingNote?.value = null
                                        activeNoteIndex = -1
                                        activeIdx = -1
                                        isRunning = false
                                    }
                                }
                            } else {
                                isRunning = false
                                activeIdx = -1
                                activeNoteIndex = -1
                                viewModel?.midiPlayingNote?.value = null
                            }
                        }
                    )
                }

                // Swarlipi-style Staff Sheet Music Layout for Pattern
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(
                            color = Color(0xFF14171E).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var globalTokenCounter = 0
                    phraseGroups.forEachIndexed { gIdx, group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            group.forEach { token ->
                                val currentFlatIdx = globalTokenCounter++
                                val isNoteSounding = isThisRunning && activeNoteIndex == currentFlatIdx
                                val isTar = token.contains("'") || token.contains("*")
                                val isMandra = token.contains(".")
                                val isKomal = token.any { it.isLowerCase() } && !token.equals("ma", ignoreCase = true)
                                val cleanName = token.replace("'", "").replace("*", "").replace(".", "")

                                Column(
                                    modifier = Modifier
                                        .widthIn(min = 34.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isNoteSounding) Color(0xFFFFEB3B).copy(alpha = 0.25f)
                                            else Color.Transparent
                                        )
                                        .padding(vertical = 2.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    // Taar dot
                                    Box(modifier = Modifier.height(4.dp), contentAlignment = Alignment.Center) {
                                        if (isTar) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isNoteSounding) Color(0xFFFFEB3B) else accentColor)
                                            )
                                        }
                                    }

                                    // Swara text
                                    Text(
                                        text = cleanName,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isNoteSounding) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isNoteSounding) Color(0xFFFFEB3B) else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    // Mandra dot or Komal line
                                    Box(modifier = Modifier.height(4.dp), contentAlignment = Alignment.Center) {
                                        if (isKomal) {
                                            Box(
                                                modifier = Modifier
                                                    .width(12.dp)
                                                    .height(1.8.dp)
                                                    .background(if (isNoteSounding) Color(0xFFFFEB3B) else accentColor)
                                            )
                                        } else if (isMandra) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isNoteSounding) Color(0xFFFFEB3B) else accentColor)
                                            )
                                        }
                                    }
                                }
                            }

                            // Phrase division bar '|'
                            if (gIdx < phraseGroups.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }

                if (currentAlankaram.arohan.isNotEmpty() && currentAlankaram.avarohan.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Arohan: ${currentAlankaram.arohan}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Avarohan: ${currentAlankaram.avarohan}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                        )
                    }
                }

                Text(
                    text = currentAlankaram.description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
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
    var currentPlayingRowIdx by remember { mutableIntStateOf(-1) }

    // Split value (e.g. "S R1 G2 M1 P D1 N2 Ṡ" or "S - R - G") into tokens for active highlighting
    val tokens = remember(value) {
        val parts = value.trim().split(Regex("\\s+"))
        if (parts.size == 1 && value.contains("-")) {
            value.split("-").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            parts
        }
    }

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
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                if (tokens.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tokens.forEachIndexed { idx, token ->
                            val isHighlighted = isRowPlaying && currentPlayingRowIdx == idx
                            pEQGlassCard(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    token,
                                    fontSize = 13.sp,
                                    fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isHighlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = if (isHighlighted) 4.dp else 1.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        InstrumentPlayButton(
            accentColor = accentColor,
            label = "Play",
            isPlaying = isRowPlaying,
            onPlayClick = {
                if (!isRowPlaying) {
                    isRowPlaying = true
                    val engine = getAudioEngine()
                    engine.setInstrument(ChordInstrumentType.HARMONIUM.soundEngineName)
                    scope.launch {
                        try {
                            val notes = if (semitones.isNotEmpty()) semitones else listOf(0, 2, 4, 5, 7, 9, 11, 12)
                            notes.forEachIndexed { idx, semi ->
                                currentPlayingRowIdx = idx
                                val note = 60 + semi
                                viewModel?.midiPlayingNote?.value = note
                                engine.noteOn(note, 0.85f)
                                delay(350L)
                                engine.noteOff(note)
                            }
                        } finally {
                            currentPlayingRowIdx = -1
                            viewModel?.midiPlayingNote?.value = null
                            isRowPlaying = false
                        }
                    }
                } else {
                    isRowPlaying = false
                    currentPlayingRowIdx = -1
                    viewModel?.midiPlayingNote?.value = null
                }
            }
        )
    }
}

@Composable
fun MetaChip(label: String, value: String, chipColor: Color) {
    pEQGlassCard(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
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
                    isPlaying -> MaterialTheme.colorScheme.tertiary
                    isIncluded && isBlackKey -> accentColor.copy(alpha = 0.85f)
                    isIncluded -> accentColor.copy(alpha = 0.25f)
                    isBlackKey -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                }

                val borderTint = when {
                    isPlaying -> MaterialTheme.colorScheme.tertiary
                    isIncluded -> accentColor.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
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
                                    color = if (isPlaying) MaterialTheme.colorScheme.onTertiary else if (isIncluded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                            }
                        } else {
                            Text(
                                text = currentLabel,
                                fontSize = 9.sp,
                                fontWeight = if (isIncluded) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isPlaying) MaterialTheme.colorScheme.onTertiary else if (isIncluded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
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
    var showSwarlipiViewer by remember { mutableStateOf(true) }
    val rotationState by animateFloatAsState(targetValue = if (showTranslation) 180f else 0f)

    fun parseSectionLines(fullText: String, prefix: String): List<String> {
        if (fullText.isEmpty()) return emptyList()
        val section = fullText.split("Antara:", "|").map { it.trim() }
        val targetBlock = if (prefix == "Sthai") {
            section.firstOrNull()?.replace("Sthai:", "")?.replace("Sthayi:", "")?.trim() ?: ""
        } else {
            if (section.size > 1) section[1].replace("Antara:", "").trim() else ""
        }
        return targetBlock.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Aligns transliteration or meaning clauses to each individual verse line.
     * Prevents single sentence meaning/transliteration from inappropriately repeating
     * or appearing on only line 1 without matching line 2.
     */
    fun alignFieldLines(rawFieldText: String, targetCount: Int, isTransliteration: Boolean = false): List<String> {
        val trimmed = rawFieldText.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. Direct newline split if already formatted across lines
        val rawLines = trimmed.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.size == targetCount) return rawLines

        // 2. Semicolon split
        if (trimmed.contains(";")) {
            val semiParts = trimmed.split(";").map { it.trim().trimEnd('.') }.filter { it.isNotEmpty() }
            if (semiParts.size == targetCount) return semiParts
        }

        // 3. Comma split for transliterations (e.g. "Ja re kagva..., piya se kahiyo...")
        if (isTransliteration && trimmed.contains(",")) {
            val commaParts = trimmed.split(",").map { it.trim().trimEnd('.') }.filter { it.isNotEmpty() }
            if (commaParts.size == targetCount) return commaParts
            if (targetCount == 2 && commaParts.size > 2) {
                val mid = commaParts.size / 2
                return listOf(commaParts.take(mid).joinToString(", "), commaParts.drop(mid).joinToString(", "))
            }
        }

        return rawLines
    }

    val sthaiLines = selectedRaga.sthai.ifEmpty { "Traditional Classical Verse\nSthai Line 2" }.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val sthaiTrans = if (selectedRaga.sthaiTransliteration.isNotEmpty()) {
        alignFieldLines(selectedRaga.sthaiTransliteration, sthaiLines.size, isTransliteration = true)
    } else {
        alignFieldLines(parseSectionLines(selectedRaga.transliteration, "Sthai").joinToString("\n"), sthaiLines.size, isTransliteration = true)
    }
    val sthaiMean = if (selectedRaga.sthaiMeaning.isNotEmpty()) {
        alignFieldLines(selectedRaga.sthaiMeaning, sthaiLines.size, isTransliteration = false)
    } else {
        alignFieldLines(parseSectionLines(selectedRaga.meaning, "Sthai").joinToString("\n"), sthaiLines.size, isTransliteration = false)
    }

    val antaraLines = selectedRaga.antara.ifEmpty { "Second Movement Verse\nAntara Line 2" }.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val antaraTrans = if (selectedRaga.antaraTransliteration.isNotEmpty()) {
        alignFieldLines(selectedRaga.antaraTransliteration, antaraLines.size, isTransliteration = true)
    } else {
        alignFieldLines(parseSectionLines(selectedRaga.transliteration, "Antara").joinToString("\n"), antaraLines.size, isTransliteration = true)
    }
    val antaraMean = if (selectedRaga.antaraMeaning.isNotEmpty()) {
        alignFieldLines(selectedRaga.antaraMeaning, antaraLines.size, isTransliteration = false)
    } else {
        alignFieldLines(parseSectionLines(selectedRaga.meaning, "Antara").joinToString("\n"), antaraLines.size, isTransliteration = false)
    }

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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = selectedRaga.bandish.ifEmpty { "Classical Composition" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Swarlipi Notation Viewer Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (showSwarlipiViewer) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (showSwarlipiViewer) accentColor else Color.Transparent),
                    modifier = Modifier.clickable { showSwarlipiViewer = !showSwarlipiViewer }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (showSwarlipiViewer) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showSwarlipiViewer) "Hide Swarlipi" else "Swarlipi Notation",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showSwarlipiViewer) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
        }

        // Swarlipi Notation Viewer component for Sthai and Antara Bandish notations
        AnimatedVisibility(visible = showSwarlipiViewer) {
            SwarlipiNotationViewer(
                raaga = selectedRaga,
                accentColor = accentColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Sthai / Pallavi",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            sthaiLines.forEachIndexed { index, line ->
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = line.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    val lineTrans = sthaiTrans.getOrNull(index) ?: (if (sthaiTrans.size == 1 && index == 0) sthaiTrans[0] else null)
                    if (!lineTrans.isNullOrEmpty()) {
                        Text(text = lineTrans, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    val lineMean = sthaiMean.getOrNull(index) ?: (if (sthaiMean.size == 1 && index == 0) sthaiMean[0] else null)
                    AnimatedVisibility(visible = showTranslation && !lineMean.isNullOrEmpty()) {
                        if (lineMean != null) {
                            Text(text = lineMean, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            if (sthaiMean.size == 1 && sthaiLines.size > 1) {
                // If the entire Sthai verse is translated as a single cohesive sentence:
                AnimatedVisibility(visible = showTranslation && sthaiMean.isNotEmpty()) {
                    Text(
                        text = sthaiMean[0],
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Antara / Charanam",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            antaraLines.forEachIndexed { index, line ->
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = line.trim(), fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    val lineTrans = antaraTrans.getOrNull(index) ?: (if (antaraTrans.size == 1 && index == 0) antaraTrans[0] else null)
                    if (!lineTrans.isNullOrEmpty()) {
                        Text(text = lineTrans, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    val lineMean = antaraMean.getOrNull(index) ?: (if (antaraMean.size == 1 && index == 0) antaraMean[0] else null)
                    AnimatedVisibility(visible = showTranslation && !lineMean.isNullOrEmpty()) {
                        if (lineMean != null) {
                            Text(text = lineMean, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            if (antaraMean.size == 1 && antaraLines.size > 1) {
                AnimatedVisibility(visible = showTranslation && antaraMean.isNotEmpty()) {
                    Text(
                        text = antaraMean[0],
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}


