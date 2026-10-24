package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.components.SvgChordBox
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedGuitarFretboard
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.ui.components.InstrumentPlayButton
import com.creativeblisslabs.peqlearn.app.ui.components.InstrumentSoundStyle
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.RealChordPosition
import com.creativeblisslabs.peqlearn.app.util.RealChordsDb
import com.creativeblisslabs.peqlearn.app.util.RealPianoChordsDb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Chord Gallery & Display Screen modeled after react-chords and guitar-practice reference.
 * Allows browsing all chord variations (open, barre, inversions, CAGED shapes) across all keys and suffixes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordDisplayGalleryScreen(
    viewModel: AppViewModel,
    initialRoot: String = "C",
    initialSuffix: String = "major",
    initialInstrument: String = "guitar",
    onBack: () -> Unit = {}
) {
    var selectedInstrument by remember { mutableStateOf(initialInstrument) } // "guitar", "ukulele", or "piano"
    var selectedRoot by remember { mutableStateOf(initialRoot) }
    var selectedSuffix by remember { mutableStateOf(initialSuffix) }
    var selectedPositionIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedQualityCategory by remember { mutableStateOf("All") }

    val scope = rememberCoroutineScope()
    val roots = remember { listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B") }
    
    val allSuffixes = remember(selectedInstrument) {
        if (selectedInstrument == "piano") {
            RealPianoChordsDb.suffixes
        } else {
            RealChordsDb.getAvailableSuffixes(selectedInstrument)
        }
    }

    val qualityCategories = remember {
        listOf("All", "Major", "Minor", "7th", "Extended (9/11/13)", "Dim/Aug", "Sus")
    }

    val filteredSuffixes = remember(allSuffixes, searchQuery, selectedQualityCategory) {
        allSuffixes.filter { s ->
            val matchesSearch = if (searchQuery.isBlank()) true else s.contains(searchQuery.trim(), ignoreCase = true)
            val matchesCategory = when (selectedQualityCategory) {
                "Major" -> s.contains("maj", ignoreCase = true) || s == "major" || s == "6"
                "Minor" -> s.contains("min", ignoreCase = true) || s.startsWith("m") && !s.startsWith("maj")
                "7th" -> s.contains("7")
                "Extended (9/11/13)" -> s.contains("9") || s.contains("11") || s.contains("13")
                "Dim/Aug" -> s.contains("dim", ignoreCase = true) || s.contains("aug", ignoreCase = true) || s.contains("°")
                "Sus" -> s.contains("sus", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    val pianoChord = remember(selectedInstrument, selectedRoot, selectedSuffix) {
        if (selectedInstrument == "piano") {
            RealPianoChordsDb.getChord(selectedRoot, selectedSuffix)
        } else null
    }

    var selectedCagedFilter by remember { mutableStateOf("All CAGED") }
    var selectedFretRange by remember { mutableStateOf("All Frets") }

    val cagedFilterOptions = remember { listOf("All CAGED", "C-Shape", "A-Shape", "G-Shape", "E-Shape", "D-Shape") }
    val fretRangeFilterOptions = remember { listOf("All Frets", "Open (0-3)", "Low (1-5)", "Mid (5-9)", "High (9-14)", "12+ Frets") }

    val chordPositions: List<RealChordPosition> = remember(selectedInstrument, selectedRoot, selectedSuffix, selectedCagedFilter, selectedFretRange) {
        val rawPositions = if (selectedInstrument == "ukulele") {
            RealChordsDb.getUkulelePositions(selectedRoot, selectedSuffix)
        } else if (selectedInstrument == "guitar") {
            RealChordsDb.getGuitarPositions(selectedRoot, selectedSuffix)
        } else {
            emptyList()
        }

        if (selectedInstrument == "guitar") {
            val filtered = rawPositions.filter { pos ->
                val matchesFretRange = when (selectedFretRange) {
                    "Open (0-3)" -> pos.baseFret in 0..3
                    "Low (1-5)" -> pos.baseFret in 1..5
                    "Mid (5-9)" -> pos.baseFret in 5..9
                    "High (9-14)" -> pos.baseFret in 9..14
                    "12+ Frets" -> pos.baseFret >= 12
                    else -> true
                }
                val matchesCaged = when (selectedCagedFilter) {
                    "C-Shape" -> (pos.frets.getOrNull(1) ?: -1) > 0 && (pos.frets.getOrNull(0) ?: -1) <= 0
                    "A-Shape" -> (pos.frets.getOrNull(1) ?: -1) >= 0 && (pos.frets.getOrNull(2) ?: -1) >= 0
                    "G-Shape" -> (pos.frets.getOrNull(0) ?: -1) > 0 && (pos.frets.getOrNull(5) ?: -1) > 0
                    "E-Shape" -> (pos.frets.getOrNull(0) ?: -1) >= 0 && (pos.frets.getOrNull(1) ?: -1) >= 0
                    "D-Shape" -> (pos.frets.getOrNull(0) ?: -1) < 0 && (pos.frets.getOrNull(1) ?: -1) < 0 && (pos.frets.getOrNull(2) ?: -1) >= 0
                    else -> true
                }
                matchesFretRange && matchesCaged
            }
            if (filtered.isNotEmpty()) filtered else rawPositions
        } else {
            rawPositions
        }
    }

    LaunchedEffect(chordPositions) {
        if (selectedPositionIndex >= chordPositions.size) {
            selectedPositionIndex = 0
        }
    }

    val currentPosition = chordPositions.getOrNull(selectedPositionIndex) ?: chordPositions.firstOrNull()
    var gallerySoundStyle by remember { mutableStateOf(InstrumentSoundStyle.SEQUENTIAL) }

    fun playMidiNotes(notes: List<Int>, instrument: String) {
        if (notes.isEmpty()) return
        scope.launch {
            try {
                viewModel.audioEngine.setInstrument(instrument)
                when (gallerySoundStyle) {
                    InstrumentSoundStyle.SEQUENTIAL -> {
                        notes.forEach { midiNote ->
                            viewModel.audioEngine.noteOn(midiNote, 0.85f)
                            delay(45L)
                        }
                    }
                    InstrumentSoundStyle.ALL_AT_ONCE -> {
                        notes.forEach { midiNote ->
                            viewModel.audioEngine.noteOn(midiNote, 0.85f)
                        }
                    }
                    InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> {
                        val roundTripNotes = if (notes.size > 2) {
                            notes + notes.subList(0, notes.size - 1).reversed()
                        } else if (notes.size == 2) {
                            listOf(notes[0], notes[1], notes[0])
                        } else {
                            notes
                        }
                        roundTripNotes.forEach { midiNote ->
                            viewModel.audioEngine.noteOn(midiNote, 0.85f)
                            delay(60L)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun playPosition(pos: RealChordPosition) {
        playMidiNotes(pos.midi, if (selectedInstrument == "ukulele") "GUITAR" else "GUITAR")
    }

    val screenScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1218))
            .verticalScroll(screenScrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar: Back button, Title & Instrument Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text(
                        text = "Chord Display Studio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Chord Shapes & Voicings",
                        fontSize = 11.sp,
                        color = Color(0xFF9EABB8)
                    )
                }
            }

            // Instrument switch: Guitar vs Ukulele vs Piano
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = selectedInstrument == "guitar",
                    onClick = { 
                        selectedInstrument = "guitar"
                        selectedPositionIndex = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Guitar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = selectedInstrument == "ukulele",
                    onClick = { 
                        selectedInstrument = "ukulele"
                        selectedPositionIndex = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Ukulele", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = selectedInstrument == "piano",
                    onClick = { 
                        selectedInstrument = "piano"
                        selectedPositionIndex = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Piano", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Root Keys Selector (Horizontal Row)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ROOT NOTE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(roots) { r ->
                    val isSelected = selectedRoot == r
                    pEQFilterChip(
                        selected = isSelected,
                        onClick = { 
                            selectedRoot = r
                            selectedPositionIndex = 0
                        },
                        label = { Text(r, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        // 2. Search & Quality Category Filters
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chords (e.g. 7#9, m7b5, sus4)...", fontSize = 12.sp, color = Color(0xFF6C788A)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFFF9E1B), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF161A22),
                        unfocusedContainerColor = Color(0xFF13161C),
                        focusedBorderColor = Color(0xFFFF9E1B),
                        unfocusedBorderColor = Color(0xFF262D3D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }

            // Quality Categories row (All, Major, Minor, 7th, Extended, Dim/Aug, Sus)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(qualityCategories) { cat ->
                    val isSel = selectedQualityCategory == cat
                    Surface(
                        onClick = { selectedQualityCategory = cat },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) Color(0xFFFF9E1B).copy(alpha = 0.22f) else Color(0xFF161A22),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFFFF9E1B) else Color(0xFF262D3D))
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color(0xFFFFB347) else Color(0xFF9EABB8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (selectedInstrument == "guitar") {
                // CAGED & Fret Range Filters Row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cagedFilterOptions) { caged ->
                        val isSel = selectedCagedFilter == caged
                        Surface(
                            onClick = { selectedCagedFilter = caged },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF4CAF50).copy(alpha = 0.22f) else Color(0xFF161A22),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF4CAF50) else Color(0xFF262D3D))
                        ) {
                            Text(
                                text = caged,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color(0xFF81C784) else Color(0xFF9EABB8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(fretRangeFilterOptions) { fretR ->
                        val isSel = selectedFretRange == fretR
                        Surface(
                            onClick = { selectedFretRange = fretR },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF29B6F6).copy(alpha = 0.22f) else Color(0xFF161A22),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF29B6F6) else Color(0xFF262D3D))
                        ) {
                            Text(
                                text = fretR,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color(0xFF4FC3F7) else Color(0xFF9EABB8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Chord Suffix / Quality Selector (Filtered)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHORD QUALITY / SUFFIX (${filteredSuffixes.size} of ${allSuffixes.size} types)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E8B9B),
                    letterSpacing = 1.sp
                )
                if (searchQuery.isNotEmpty() || selectedQualityCategory != "All") {
                    Text(
                        text = "Filtered",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9E1B)
                    )
                }
            }
            if (filteredSuffixes.isEmpty()) {
                Text("No qualities match '$searchQuery'", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredSuffixes) { s ->
                        val isSelected = selectedSuffix == s
                        pEQFilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedSuffix = s
                                selectedPositionIndex = 0
                            },
                            label = { Text(s, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }

        // 3. Main Display: Hero Fretboard or Piano Keyboard with interactive notes + playback button
        if (selectedInstrument == "piano") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color(0xFF262D3D))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$selectedRoot $selectedSuffix (Piano)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFB347)
                            )
                            val notesText = pianoChord?.notes?.joinToString(" - ") ?: "Notes not available"
                            val intText = pianoChord?.intervals?.joinToString(", ") ?: ""
                            Text(
                                text = "Notes: $notesText • Intervals: $intText",
                                fontSize = 12.sp,
                                color = Color(0xFF9EABB8)
                            )
                        }

                        InstrumentPlayButton(
                            accentColor = Color(0xFFFF9E1B),
                            label = "Play",
                            soundStyle = gallerySoundStyle,
                            onSoundStyleToggle = {
                                gallerySoundStyle = when (gallerySoundStyle) {
                                    InstrumentSoundStyle.SEQUENTIAL -> InstrumentSoundStyle.ALL_AT_ONCE
                                    InstrumentSoundStyle.ALL_AT_ONCE -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                                    InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> InstrumentSoundStyle.SEQUENTIAL
                                }
                            },
                            onPlayClick = {
                                pianoChord?.midi?.let { playMidiNotes(it, "PIANO") }
                            }
                        )
                    }

                    val activePianoNotes = remember(pianoChord) {
                        pianoChord?.midi?.associateWith { true } ?: emptyMap()
                    }

                    com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard(
                        activeMidiNotes = activePianoNotes,
                        accentColor = Color(0xFFFF9E1B),
                        startNote = 48, // C3
                        endNote = 84,   // C6
                        fillWidth = true,
                        showNoteNames = true,
                        onKeyTapped = { midiNote ->
                            try {
                                viewModel.audioEngine.setInstrument("PIANO")
                                viewModel.audioEngine.noteOn(midiNote, 0.85f)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        } else if (currentPosition != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color(0xFF262D3D))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$selectedRoot $selectedSuffix",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFB347)
                            )
                            Text(
                                text = "Variation ${selectedPositionIndex + 1} of ${chordPositions.size} • Base Fret ${currentPosition.baseFret}",
                                fontSize = 12.sp,
                                color = Color(0xFF9EABB8)
                            )
                        }

                        InstrumentPlayButton(
                            accentColor = Color(0xFFFF9E1B),
                            label = "Play",
                            soundStyle = gallerySoundStyle,
                            onSoundStyleToggle = {
                                gallerySoundStyle = when (gallerySoundStyle) {
                                    InstrumentSoundStyle.SEQUENTIAL -> InstrumentSoundStyle.ALL_AT_ONCE
                                    InstrumentSoundStyle.ALL_AT_ONCE -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                                    InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> InstrumentSoundStyle.SEQUENTIAL
                                }
                            },
                            onPlayClick = { playPosition(currentPosition) }
                        )
                    }

                    // Tapered Interactive Fretboard
                    val isUke = selectedInstrument == "ukulele"
                    val stringLabels = if (isUke) listOf("A", "E", "C", "G") else listOf("e", "B", "G", "D", "A", "E")
                    val openSemis = if (isUke) listOf(9, 4, 0, 7) else listOf(4, 11, 7, 2, 9, 4)
                    
                    // UnifiedGuitarFretboard expects top-to-bottom strings (high string on top, low string on bottom)
                    val reversedFrets = currentPosition.frets.reversed()
                    val reversedFingers = currentPosition.fingers.reversed()

                    UnifiedGuitarFretboard(
                        numFrets = 7,
                        accentColor = Color(0xFFFF9E1B),
                        chordFrets = reversedFrets,
                        chordFingers = reversedFingers,
                        baseFret = currentPosition.baseFret,
                        barreFret = currentPosition.barres.firstOrNull() ?: -1,
                        stringLabels = stringLabels,
                        openStringsSemis = openSemis,
                        markOctaves = true,
                        highlightOctaveC = true,
                        onFretTapped = { _, _, midiNote ->
                            try {
                                viewModel.audioEngine.setInstrument("GUITAR")
                                viewModel.audioEngine.noteOn(midiNote, 0.85f)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(if (isUke) 120.dp else 145.dp).clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }

        // 4. All Variations Grid (SVG Chord Diagrams from react-chords library, or Piano details)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val variationsTitle = if (selectedInstrument == "piano") {
                "PIANO CHORD NOTES & TONAL STRUCTURE"
            } else {
                "ALL VOICINGS & INVERSIONS (${chordPositions.size} variations)"
            }
            Text(
                text = variationsTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )

            if (selectedInstrument == "piano") {
                if (pianoChord == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No piano chord data found for $selectedRoot $selectedSuffix", color = Color.Gray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pianoChord.notes.forEachIndexed { i, noteName ->
                            val interval = pianoChord.intervals.getOrNull(i) ?: ""
                            val midi = pianoChord.midi.getOrNull(i)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        midi?.let {
                                            viewModel.audioEngine.setInstrument("PIANO")
                                            viewModel.audioEngine.noteOn(it, 0.85f)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161A22),
                                border = BorderStroke(1.dp, Color(0xFF262D3D))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = noteName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (i == 0) Color(0xFFFFB347) else Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (i == 0) Color(0xFFFF9E1B).copy(alpha = 0.2f) else Color(0xFF262D3D)
                                    ) {
                                        Text(
                                            text = interval.ifEmpty { "Tone" },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (i == 0) Color(0xFFFFB347) else Color(0xFF9EABB8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (midi != null) {
                                        Text(
                                            text = "MIDI $midi",
                                            fontSize = 9.sp,
                                            color = Color(0xFF6C788A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (chordPositions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No positions found for $selectedRoot $selectedSuffix", color = Color.Gray)
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(chordPositions.indices.toList()) { idx ->
                        val pos = chordPositions[idx]
                        val isSelected = idx == selectedPositionIndex
                        
                        SvgChordBox(
                            chord = pos,
                            chordName = "$selectedRoot$selectedSuffix #${idx + 1}",
                            instrument = selectedInstrument,
                            accentColor = if (isSelected) Color(0xFFFF9E1B) else Color(0xFF81C784),
                            cardBackground = if (isSelected) Color(0xFF222938) else Color(0xFF161A22),
                            onClick = {
                                selectedPositionIndex = idx
                                playPosition(pos)
                            }
                        )
                    }
                }
            }
        }
    }
}
