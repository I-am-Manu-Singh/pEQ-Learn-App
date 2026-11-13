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
import com.creativeblisslabs.peqlearn.app.ui.components.SvgPianoChordBox
import com.creativeblisslabs.peqlearn.app.ui.components.M3DropdownMenu
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedGuitarFretboard
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.ui.components.InstrumentPlayButton
import com.creativeblisslabs.peqlearn.app.ui.components.InstrumentSoundStyle
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.RealChordPosition
import com.creativeblisslabs.peqlearn.app.util.RealChordsDb
import com.creativeblisslabs.peqlearn.app.util.RealPianoChordsDb
import kotlinx.coroutines.Job
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
    var isGalleryPlaying by remember { mutableStateOf(false) }
    var galleryActiveMidi by remember { mutableStateOf<Int?>(null) }
    var galleryActiveCoord by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var galleryJob by remember { mutableStateOf<Job?>(null) }

    fun playMidiNotes(notes: List<Int>, instrument: String, noteCoords: List<Pair<Int, Int>?> = emptyList()) {
        if (notes.isEmpty()) return
        galleryJob?.cancel()
        galleryJob = scope.launch {
            isGalleryPlaying = true
            try {
                viewModel.audioEngine.setInstrument(instrument)
                when (gallerySoundStyle) {
                    InstrumentSoundStyle.SEQUENTIAL -> {
                        notes.forEachIndexed { idx, midiNote ->
                            galleryActiveMidi = midiNote
                            galleryActiveCoord = noteCoords.getOrNull(idx)
                            viewModel.midiPlayingNote.value = midiNote
                            viewModel.audioEngine.noteOn(midiNote, 0.85f)
                            delay(260L)
                            viewModel.audioEngine.noteOff(midiNote)
                        }
                    }
                    InstrumentSoundStyle.ALL_AT_ONCE -> {
                        galleryActiveCoord = null
                        galleryActiveMidi = null
                        notes.forEach { midiNote ->
                            viewModel.audioEngine.noteOn(midiNote, 0.85f)
                        }
                        delay(1200L)
                        notes.forEach { midiNote ->
                            try { viewModel.audioEngine.noteOff(midiNote) } catch (_: Exception) {}
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
                        val roundTripCoords = if (noteCoords.size > 2) {
                            noteCoords + noteCoords.subList(0, noteCoords.size - 1).reversed()
                        } else if (noteCoords.size == 2) {
                            listOf(noteCoords[0], noteCoords[1], noteCoords[0])
                        } else {
                            noteCoords
                        }
                        roundTripNotes.forEachIndexed { idx, midiNote ->
                            galleryActiveMidi = midiNote
                            galleryActiveCoord = roundTripCoords.getOrNull(idx)
                            viewModel.midiPlayingNote.value = midiNote
                            viewModel.audioEngine.noteOn(midiNote, 0.85f)
                            delay(300L)
                            viewModel.audioEngine.noteOff(midiNote)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                notes.forEach { midi ->
                    try { viewModel.audioEngine.noteOff(midi) } catch (_: Exception) {}
                }
                isGalleryPlaying = false
                galleryActiveMidi = null
                galleryActiveCoord = null
                viewModel.midiPlayingNote.value = null
            }
        }
    }

  fun playPosition(pos: RealChordPosition) {
        if (isGalleryPlaying) {
            galleryJob?.cancel()
            pos.midi.forEach { try { viewModel.audioEngine.noteOff(it) } catch (_: Exception) {} }
            isGalleryPlaying = false
            galleryActiveMidi = null
            galleryActiveCoord = null
            viewModel.midiPlayingNote.value = null
            return
        }
        val isUke = selectedInstrument == "ukulele"
        val defaultOpenMidi = if (isUke) listOf(69, 64, 60, 67) else listOf(64, 59, 55, 50, 45, 40)
        val (_, _, strumSteps) = com.creativeblisslabs.peqlearn.app.util.BiomechanicalPerformersEngine
            .normalizeRealChordPositionTopToBottom(pos, defaultOpenMidi)

        val notes = strumSteps.map { it.third }
        val coords = strumSteps.map { it.first to it.second }
        playMidiNotes(notes, "GUITAR", coords)
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

        // 1. Sleek Dropdown Configuration Row (Root Note, Quality / Suffix, and Category)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                M3DropdownMenu(
                    label = "Root Note",
                    selectedText = selectedRoot,
                    items = roots,
                    onItemSelect = {
                        selectedRoot = roots[it]
                        selectedPositionIndex = 0
                    }
                )
            }
            Box(modifier = Modifier.weight(1.5f)) {
                val suffixOptions = if (filteredSuffixes.isNotEmpty()) filteredSuffixes else allSuffixes
                M3DropdownMenu(
                    label = "Chord Suffix",
                    selectedText = selectedSuffix,
                    items = suffixOptions,
                    onItemSelect = {
                        selectedSuffix = suffixOptions[it]
                        selectedPositionIndex = 0
                    }
                )
            }
            Box(modifier = Modifier.weight(1.2f)) {
                M3DropdownMenu(
                    label = "Category",
                    selectedText = selectedQualityCategory,
                    items = qualityCategories,
                    onItemSelect = {
                        selectedQualityCategory = qualityCategories[it]
                    }
                )
            }
        }

        // 2. Instrument Specific Neck & Position Filters
        if (selectedInstrument == "guitar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    M3DropdownMenu(
                        label = "CAGED Shape",
                        selectedText = selectedCagedFilter,
                        items = cagedFilterOptions,
                        onItemSelect = { selectedCagedFilter = cagedFilterOptions[it] }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    M3DropdownMenu(
                        label = "Fret Window",
                        selectedText = selectedFretRange,
                        items = fretRangeFilterOptions,
                        onItemSelect = { selectedFretRange = fretRangeFilterOptions[it] }
                    )
                }
            }
        }

        // 3. Search Bar for direct suffix typing
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
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )

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
                        val isHeaderFlashing = isGalleryPlaying
                        Column {
                            Text(
                                text = "$selectedRoot $selectedSuffix (Piano)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isHeaderFlashing) Color(0xFFFFD54F) else Color(0xFFFFB347)
                            )
                            val notesText = pianoChord?.notes?.joinToString(" - ") ?: "Notes not available"
                            val intText = pianoChord?.intervals?.joinToString(", ") ?: ""
                            Text(
                                text = "Notes: $notesText • Intervals: $intText",
                                fontSize = 12.sp,
                                color = if (isHeaderFlashing) Color(0xFFFFE082) else Color(0xFF9EABB8)
                            )
                        }

                        InstrumentPlayButton(
                            accentColor = Color(0xFFFF9E1B),
                            label = "Play",
                            isPlaying = isGalleryPlaying,
                            soundStyle = gallerySoundStyle,
                            onSoundStyleToggle = {
                                gallerySoundStyle = when (gallerySoundStyle) {
                                    InstrumentSoundStyle.SEQUENTIAL -> InstrumentSoundStyle.ALL_AT_ONCE
                                    InstrumentSoundStyle.ALL_AT_ONCE -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                                    InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> InstrumentSoundStyle.SEQUENTIAL
                                }
                            },
                            onClick = {
                                if (isGalleryPlaying) {
                                    galleryJob?.cancel()
                                    pianoChord?.midi?.forEach { try { viewModel.audioEngine.noteOff(it) } catch (_: Exception) {} }
                                    isGalleryPlaying = false
                                    galleryActiveMidi = null
                                    viewModel.midiPlayingNote.value = null
                                } else {
                                    pianoChord?.midi?.let { playMidiNotes(it, "PIANO") }
                                }
                            }
                        )
                    }

                   val baseMidiList = pianoChord?.midi ?: emptyList()
                    val activeVariationNotes = remember(baseMidiList, selectedPositionIndex) {
                        if (baseMidiList.isEmpty()) emptyList()
                        else when (selectedPositionIndex) {
                            1 -> if (baseMidiList.size >= 2) (baseMidiList.drop(1) + (baseMidiList.first() + 12)).sorted() else baseMidiList
                            2 -> if (baseMidiList.size >= 3) (baseMidiList.drop(2) + baseMidiList.take(2).map { it + 12 }).sorted() else baseMidiList
                            3 -> if (baseMidiList.size >= 4) (baseMidiList.drop(3) + baseMidiList.take(3).map { it + 12 }).sorted() else baseMidiList
                            4 -> if (baseMidiList.size == 4) listOf(baseMidiList[1] - 12, baseMidiList[0], baseMidiList[2], baseMidiList[3]).sorted() else baseMidiList
                            else -> baseMidiList
                        }
                    }
                    val activePianoNotes = remember(activeVariationNotes) {
                        activeVariationNotes.associateWith { true }
                    }
                    val galleryPianoFingering = remember(activeVariationNotes, selectedSuffix, selectedPositionIndex) {
                        com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine.computePianoFingering(
                            pitches = activeVariationNotes,
                            chordSuffix = selectedSuffix,
                            isScale = false,
                            voicingIdx = selectedPositionIndex
                        )
                    }

                    com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard(
                        activeMidiNotes = activePianoNotes,
                        fingering = galleryPianoFingering,
                        accentColor = Color(0xFFFF9E1B),
                        activeCoordinate = galleryActiveMidi,
                        isPlaying = isGalleryPlaying,
                        startNote = 48,
                        endNote = 84,
                        fillWidth = true,
                        showNoteNames = true,
                        onKeyTapped = { midiNote ->
                            scope.launch {
                                try {
                                    viewModel.audioEngine.setInstrument("PIANO")
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    delay(350L)
                                    viewModel.audioEngine.noteOff(midiNote)
                                } catch (_: Exception) {}
                            }
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
                        val isHeaderFlashing = isGalleryPlaying
                        Column {
                            Text(
                                text = "$selectedRoot $selectedSuffix",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isHeaderFlashing) Color(0xFFFFD54F) else Color(0xFFFFB347)
                            )
                            Text(
                                text = "Variation ${selectedPositionIndex + 1} of ${chordPositions.size} • Base Fret ${currentPosition.baseFret}",
                                fontSize = 12.sp,
                                color = if (isHeaderFlashing) Color(0xFFFFE082) else Color(0xFF9EABB8)
                            )
                        }

                        InstrumentPlayButton(
                            accentColor = Color(0xFFFF9E1B),
                            label = "Play",
                            isPlaying = isGalleryPlaying,
                            soundStyle = gallerySoundStyle,
                            onSoundStyleToggle = {
                                gallerySoundStyle = when (gallerySoundStyle) {
                                    InstrumentSoundStyle.SEQUENTIAL -> InstrumentSoundStyle.ALL_AT_ONCE
                                    InstrumentSoundStyle.ALL_AT_ONCE -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                                    InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> InstrumentSoundStyle.SEQUENTIAL
                                }
                            },
                            onClick = { playPosition(currentPosition) }
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
                        activeCoordinate = galleryActiveCoord,
                        activeMidiNote = galleryActiveMidi,
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
                    val baseMidiList = pianoChord.midi
                    // Generate inversions/variations: Root position, 1st Inversion, 2nd Inversion, 3rd Inversion (if 4+ notes)
                    val pianoVariations = remember(baseMidiList) {
                        val variations = mutableListOf<Pair<String, List<Int>>>()
                        if (baseMidiList.isNotEmpty()) {
                            variations.add("Root Position" to baseMidiList)
                            if (baseMidiList.size >= 2) {
                                // 1st inversion: bottom note up an octave (+12)
                                val inv1 = (baseMidiList.drop(1) + (baseMidiList.first() + 12)).sorted()
                                variations.add("1st Inversion" to inv1)
                            }
                            if (baseMidiList.size >= 3) {
                                // 2nd inversion: bottom 2 notes up an octave
                                val inv2 = (baseMidiList.drop(2) + baseMidiList.take(2).map { it + 12 }).sorted()
                                variations.add("2nd Inversion" to inv2)
                            }
                            if (baseMidiList.size >= 4) {
                                // 3rd inversion: bottom 3 notes up an octave
                                val inv3 = (baseMidiList.drop(3) + baseMidiList.take(3).map { it + 12 }).sorted()
                                variations.add("3rd Inversion" to inv3)
                            }
                            // Drop 2 voicing if 4 notes
                            if (baseMidiList.size == 4) {
                                val drop2 = listOf(baseMidiList[1] - 12, baseMidiList[0], baseMidiList[2], baseMidiList[3]).sorted()
                                variations.add("Drop 2" to drop2)
                            }
                        }
                        variations
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(pianoVariations.indices.toList()) { idx ->
                            val (invLabel, notes) = pianoVariations[idx]
                            val isSelected = selectedPositionIndex == idx
                            com.creativeblisslabs.peqlearn.app.ui.components.SvgPianoChordBox(
                                midiNotes = notes,
                                chordName = "$selectedRoot$selectedSuffix",
                                inversionLabel = invLabel,
                                accentColor = if (isSelected) Color(0xFFFF9E1B) else Color(0xFF81C784),
                                cardBackground = if (isSelected) Color(0xFF222938) else Color(0xFF161A22),
                                onClick = {
                                    selectedPositionIndex = idx
                                    playMidiNotes(notes, "PIANO")
                                }
                            )
                        }
                    }

                    // Tonal structure tags below cards
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
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
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
