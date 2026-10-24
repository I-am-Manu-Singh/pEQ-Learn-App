package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.RealChordPosition
import com.creativeblisslabs.peqlearn.app.util.RealChordsDb
import com.creativeblisslabs.peqlearn.app.util.RealPianoChordsDb
import com.creativeblisslabs.peqlearn.app.util.RealInversionsDb
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import com.creativeblisslabs.peqlearn.app.util.PianoVoicingStyle
import com.creativeblisslabs.peqlearn.app.util.InversionStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Musical Reference Studio Tab modeled directly after optilude/guitar-practice.
 * Features 4 master panels: Scales, Arpeggios, Chords, and Inversions, with SVG chord diagrams
 * and fretboard visualization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceStudioPanel(
    viewModel: AppViewModel,
    onOpenChordGallery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("Chords") }
    val subTabs = remember { listOf("Chords", "Inversions", "Circle of 5ths", "Substitutions", "Scales", "Arpeggios") }

    val roots = remember { listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B") }
    var selectedRoot by remember { mutableStateOf("C") }
    var selectedSuffix by remember { mutableStateOf("major") }
    var selectedInstrument by remember { mutableStateOf("guitar") } // "guitar", "piano", "ukulele"

    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedQualityCategory by remember { mutableStateOf("All") }

    val qualityCategories = remember {
        listOf("All", "Major", "Minor", "7th", "Extended", "Dim/Aug", "Sus")
    }

    val allSuffixes = remember(selectedInstrument) {
        if (selectedInstrument == "piano") {
            RealPianoChordsDb.suffixes
        } else {
            RealChordsDb.getAvailableSuffixes(selectedInstrument)
        }
    }

    val displaySuffixes = remember(activeSubTab, allSuffixes, searchQuery, selectedQualityCategory) {
        val baseList = if (activeSubTab == "Inversions") {
            listOf("major", "minor", "7", "maj7", "m7", "dim", "aug")
        } else allSuffixes

        baseList.filter { s ->
            val matchesSearch = if (searchQuery.isBlank()) true else s.contains(searchQuery.trim(), ignoreCase = true)
            val matchesCat = when (selectedQualityCategory) {
                "Major" -> s.contains("maj", ignoreCase = true) || s == "major" || s == "6"
                "Minor" -> s.contains("min", ignoreCase = true) || (s.startsWith("m") && !s.startsWith("maj"))
                "7th" -> s.contains("7")
                "Extended" -> s.contains("9") || s.contains("11") || s.contains("13")
                "Dim/Aug" -> s.contains("dim", ignoreCase = true) || s.contains("aug", ignoreCase = true) || s.contains("°")
                "Sus" -> s.contains("sus", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCat
        }
    }

    var selectedCagedFilter by remember { mutableStateOf("All CAGED") }
    var selectedFretRange by remember { mutableStateOf("All Frets") }

    val cagedFilterOptions = remember { listOf("All CAGED", "C-Shape", "A-Shape", "G-Shape", "E-Shape", "D-Shape") }
    val fretRangeFilterOptions = remember { listOf("All Frets", "Open (0-3)", "Low (1-5)", "Mid (5-9)", "High (9-14)", "12+ Frets") }

    val positions = remember(activeSubTab, selectedInstrument, selectedRoot, selectedSuffix, selectedCagedFilter, selectedFretRange) {
        val rawPositions = if (activeSubTab == "Inversions") {
            val invPositions = RealInversionsDb.getInversions(selectedRoot, selectedSuffix)
            if (invPositions.isNotEmpty()) {
                invPositions.map { invPos ->
                    RealChordPosition(
                        frets = invPos.frets,
                        fingers = invPos.fingers,
                        baseFret = invPos.baseFret,
                        barres = invPos.barres,
                        midi = emptyList()
                    )
                }
            } else {
                RealChordsDb.getGuitarPositions(selectedRoot, selectedSuffix)
            }
        } else if (selectedInstrument == "ukulele") {
            RealChordsDb.getUkulelePositions(selectedRoot, selectedSuffix)
        } else {
            RealChordsDb.getGuitarPositions(selectedRoot, selectedSuffix)
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1218))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Reference Studio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Interactive Encyclopedia (optilude/guitar-practice)",
                    fontSize = 11.sp,
                    color = Color(0xFF9EABB8)
                )
            }

            OutlinedButton(
                onClick = onOpenChordGallery,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB347)),
                border = BorderStroke(1.dp, Color(0xFFFFB347).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("All Shapes Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Instrument Switcher Row: Guitar, Ukulele, Piano
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INSTRUMENT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )
            SingleChoiceSegmentedButtonRow {
                listOf("guitar" to "🎸 Guitar", "ukulele" to "🪕 Ukulele", "piano" to "🎹 Piano").forEachIndexed { idx, (key, label) ->
                    SegmentedButton(
                        selected = selectedInstrument == key,
                        onClick = {
                            selectedInstrument = key
                            try {
                                viewModel.audioEngine.setInstrument(if (key == "piano") "PIANO" else "GUITAR")
                            } catch (_: Exception) {}
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Subtabs: Chords | Inversions | Scales | Arpeggios
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            subTabs.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = activeSubTab == tab,
                    onClick = { activeSubTab = tab },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = subTabs.size)
                ) {
                    Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Root note row
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
                        onClick = { selectedRoot = r },
                        label = { Text(r, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        when (activeSubTab) {
            "Chords", "Inversions" -> {
                // Search & Quality Categories
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter qualities (e.g. 7#9, m7, sus4)...", fontSize = 12.sp, color = Color(0xFF6C788A)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64B5F6), modifier = Modifier.size(18.dp)) },
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
                                focusedBorderColor = Color(0xFF64B5F6),
                                unfocusedBorderColor = Color(0xFF262D3D),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(qualityCategories) { cat ->
                            val isSel = selectedQualityCategory == cat
                            Surface(
                                onClick = { selectedQualityCategory = cat },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF64B5F6).copy(alpha = 0.22f) else Color(0xFF161A22),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF64B5F6) else Color(0xFF262D3D))
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color(0xFF90CAF9) else Color(0xFF9EABB8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (selectedInstrument == "guitar") {
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

                // Chord Suffix selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHORD QUALITY / SUFFIX (${displaySuffixes.size} types)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E8B9B),
                            letterSpacing = 1.sp
                        )
                    }

                    if (displaySuffixes.isEmpty()) {
                        Text("No chord qualities match '$searchQuery'", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(displaySuffixes) { s ->
                                val isSelected = selectedSuffix == s
                                pEQFilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSuffix = s },
                                    label = { Text(s, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "$selectedRoot $selectedSuffix ($activeSubTab) • ${positions.size} Shapes Found",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64B5F6)
                )

                if (selectedInstrument == "piano") {
                    val pianoChordData = remember(selectedRoot, selectedSuffix) {
                        RealPianoChordsDb.getChord(selectedRoot, selectedSuffix)
                    }
                    val pianoMidiNotes = remember(pianoChordData, selectedRoot, selectedSuffix) {
                        if (pianoChordData != null && pianoChordData.midi.isNotEmpty()) {
                            pianoChordData.midi
                        } else {
                            val rIdx = roots.indexOf(selectedRoot).coerceAtLeast(0)
                            ChordVoicingEngine.buildPianoShape(
                                rootIdx = rIdx,
                                suffix = selectedSuffix,
                                style = PianoVoicingStyle.CLOSE,
                                inversion = InversionStyle.ROOT,
                                octave = 4
                            )
                        }
                    }
                    val activeMap = remember(pianoMidiNotes) {
                        pianoMidiNotes.associateWith { true }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val notesText = pianoChordData?.notes?.joinToString(" - ") ?: pianoMidiNotes.joinToString(" - ") { roots[it % 12] }
                            val intText = pianoChordData?.intervals?.joinToString(", ") ?: ""
                            Text(
                                text = "Notes: $notesText" + (if (intText.isNotBlank()) " • Intervals: $intText" else ""),
                                fontSize = 12.sp,
                                color = Color(0xFF9EABB8)
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.audioEngine.setInstrument("PIANO")
                                        pianoMidiNotes.forEach { midi ->
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                            delay(35L)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9E1B)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("Play Piano", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard(
                            activeMidiNotes = activeMap,
                            accentColor = Color(0xFFFF9E1B),
                            startNote = 48,
                            endNote = 84,
                            fillWidth = true,
                            showNoteNames = true,
                            onKeyTapped = { midi ->
                                try {
                                    viewModel.audioEngine.setInstrument("PIANO")
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(185.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(positions.indices.toList()) { idx ->
                            val pos = positions[idx]
                            SvgChordBox(
                                chord = pos,
                                chordName = "$selectedRoot$selectedSuffix #${idx + 1}",
                                instrument = selectedInstrument,
                                accentColor = Color(0xFF64B5F6),
                                cardBackground = Color(0xFF161A22),
                                width = 135.dp,
                                height = 175.dp,
                                onClick = {
                                    scope.launch {
                                        val instr = if (selectedInstrument == "ukulele") "GUITAR" else "GUITAR"
                                        viewModel.audioEngine.setInstrument(instr)
                                        pos.midi.forEach { midi ->
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                            delay(35L)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            "Circle of 5ths" -> {
                com.creativeblisslabs.peqlearn.app.ui.components.InteractiveCircleOfFifths(
                    selectedKey = selectedRoot,
                    onKeySelected = { selectedRoot = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Substitutions" -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Harmonic Substitutions for $selectedRoot$selectedSuffix",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB347)
                    )

                    val subsList = remember(selectedRoot, selectedSuffix) {
                        listOf(
                            Triple("Tritone Substitution", "Substitute dominant 7th with dominant 7th a tritone (6 semitones) away.", "F#7 for C7"),
                            Triple("Relative Minor / Major", "Shares 2 common chord tones, softer tonic resolution.", "Am7 for Cmaj7"),
                            Triple("Secondary Dominant", "V7 of the target chord to create momentum.", "D7 → G7 → C"),
                            Triple("Backdoor Dominant", "♭VII7 whole step below next target, borrowed from Aeolian.", "B♭7 → Cmaj7"),
                            Triple("ii-V Jazz Preparation", "Precede any target chord with its related ii-V pair.", "Dm7 - G7 → C"),
                            Triple("Diminished Passing", "Chromatic leading tone diminished 7th chord.", "B°7 → Cmaj7"),
                            Triple("Coltrane Changes", "Three tonal centres a major third apart.", "Cmaj7 → E♭7 → A♭maj7 → B7 → Emaj7")
                        )
                    }

                    subsList.forEach { (rule, desc, example) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF161A22),
                            border = BorderStroke(1.dp, Color(0xFF262D3D))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rule, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFF9E1B).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            example,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFFB347),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(desc, fontSize = 11.sp, color = Color(0xFF9EABB8))
                            }
                        }
                    }
                }
            }
            "Scales", "Arpeggios" -> {
                val scaleIntervals = if (activeSubTab == "Scales") {
                    listOf(0, 2, 4, 5, 7, 9, 11) // Major scale
                } else {
                    listOf(0, 4, 7) // Major arpeggio triad
                }
                val rootIdx = roots.indexOf(selectedRoot).coerceAtLeast(0)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$selectedRoot $activeSubTab on " + (if (selectedInstrument == "piano") "Piano Keyboard" else "Fretboard"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )

                    if (selectedInstrument == "piano") {
                        val activeScalePitches = remember(rootIdx, scaleIntervals) {
                            val baseMidi = 60 + rootIdx
                            scaleIntervals.map { baseMidi + it }.associateWith { true }
                        }
                        com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard(
                            activeMidiNotes = activeScalePitches,
                            accentColor = Color(0xFF81C784),
                            startNote = 48,
                            endNote = 84,
                            fillWidth = true,
                            showNoteNames = true,
                            onKeyTapped = { midi ->
                                try {
                                    viewModel.audioEngine.setInstrument("PIANO")
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        UnifiedGuitarFretboard(
                            numFrets = 15,
                            activeNotes = scaleIntervals.map { (rootIdx + it) % 12 },
                            rootNote = rootIdx,
                            accentColor = Color(0xFF81C784),
                            showNoteNames = true,
                            markOctaves = true,
                            highlightOctaveC = true,
                            stringLabels = if (selectedInstrument == "ukulele") listOf("A", "E", "C", "G") else listOf("e", "B", "G", "D", "A", "E"),
                            openStringsSemis = if (selectedInstrument == "ukulele") listOf(9, 4, 0, 7) else listOf(4, 11, 7, 2, 9, 4),
                            baseFret = 1,
                            onFretTapped = { _, _, midi ->
                                try {
                                    viewModel.audioEngine.setInstrument(if (selectedInstrument == "ukulele") "GUITAR" else "GUITAR")
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }

                    Text(
                        text = "Notes: " + scaleIntervals.joinToString(", ") { roots[(rootIdx + it) % 12] },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
