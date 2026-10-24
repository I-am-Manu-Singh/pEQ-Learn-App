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
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.RealChordPosition
import com.creativeblisslabs.peqlearn.app.util.RealChordsDb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tools Studio Panel cloned from optilude/guitar-practice.
 * Implements the 5 guitar practice tools:
 * 1. Transposer
 * 2. Key Finder
 * 3. Chord Reverse Finder
 * 4. Scale Finder
 * 5. Interactive Metronome
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsStudioPanel(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeTool by remember { mutableStateOf("Transposer") }
    val toolList = remember { listOf("Transposer", "Circle of 5ths", "Key Finder", "Chord Finder", "Scale Finder", "Metronome", "Time Signatures") }
    val scope = rememberCoroutineScope()
    val roots = remember { listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1218))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Musical Tools",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Transposer, Key Finder, Chord/Scale Search & Metronome",
                    fontSize = 11.sp,
                    color = Color(0xFF9EABB8)
                )
            }
        }

        // Tools horizontal selector
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(toolList) { tool ->
                val isSelected = activeTool == tool
                pEQFilterChip(
                    selected = isSelected,
                    onClick = { activeTool = tool },
                    label = { Text(tool, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color(0xFF262D3D))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (activeTool) {
                    "Transposer" -> TransposerTool(roots = roots)
                    "Circle of 5ths" -> {
                        var circleKey by remember { mutableStateOf("C") }
                        com.creativeblisslabs.peqlearn.app.ui.components.InteractiveCircleOfFifths(
                            selectedKey = circleKey,
                            onKeySelected = { circleKey = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "Key Finder" -> KeyFinderTool(roots = roots)
                    "Chord Finder" -> ChordFinderTool(viewModel = viewModel, roots = roots)
                    "Scale Finder" -> ScaleFinderTool(roots = roots)
                    "Metronome" -> MetronomeTool(viewModel = viewModel)
                    "Time Signatures" -> TimeSignaturesTool()
                }
            }
        }
    }
}

@Composable
private fun TransposerTool(roots: List<String>) {
    var fromKey by remember { mutableStateOf("C") }
    var toKey by remember { mutableStateOf("G") }
    var inputChords by remember { mutableStateOf("C, Am, F, G") }

    val fromIdx = roots.indexOf(fromKey).coerceAtLeast(0)
    val toIdx = roots.indexOf(toKey).coerceAtLeast(0)
    val semitoneDiff = (toIdx - fromIdx + 12) % 12

    val transposedChords = remember(inputChords, semitoneDiff) {
        inputChords.split(",", " ").filter { it.isNotBlank() }.map { token ->
            val clean = token.trim()
            val matchRoot = roots.sortedByDescending { it.length }.firstOrNull { clean.startsWith(it) }
            if (matchRoot != null) {
                val oldRootIdx = roots.indexOf(matchRoot)
                val newRoot = roots[(oldRootIdx + semitoneDiff) % 12]
                clean.replaceFirst(matchRoot, newRoot)
            } else clean
        }.joinToString("  •  ")
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Chord Transposer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB347))
        Text("Transpose any sequence of chords cleanly across musical keys.", fontSize = 12.sp, color = Color.Gray)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("FROM KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(roots) { r ->
                        pEQFilterChip(selected = fromKey == r, onClick = { fromKey = r }, label = { Text(r) })
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TO KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(roots) { r ->
                        pEQFilterChip(selected = toKey == r, onClick = { toKey = r }, label = { Text(r) })
                    }
                }
            }
        }

        OutlinedTextField(
            value = inputChords,
            onValueChange = { inputChords = it },
            label = { Text("Chord Progression") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF222938)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("TRANSPOSED RESULT ($fromKey → $toKey, +$semitoneDiff st):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                Spacer(Modifier.height(6.dp))
                Text(transposedChords, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9E1B))
            }
        }
    }
}

@Composable
private fun KeyFinderTool(roots: List<String>) {
    var selectedChords by remember { mutableStateOf(setOf("C", "G", "Am", "F")) }
    val commonChords = remember { listOf("C", "Dm", "Em", "F", "G", "Am", "Bdim", "D", "A", "E", "Bm", "F#m", "C#m") }

    val keyScores = remember(selectedChords) {
        roots.map { root ->
            val rootIdx = roots.indexOf(root)
            val diatonicTriads = listOf(
                roots[rootIdx],
                roots[(rootIdx + 2) % 12] + "m",
                roots[(rootIdx + 4) % 12] + "m",
                roots[(rootIdx + 5) % 12],
                roots[(rootIdx + 7) % 12],
                roots[(rootIdx + 9) % 12] + "m",
                roots[(rootIdx + 11) % 12] + "dim"
            )
            val matches = selectedChords.count { it in diatonicTriads }
            root to matches
        }.sortedByDescending { it.second }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Key Finder", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
        Text("Select the chords in your progression to discover the most likely parent key signature.", fontSize = 12.sp, color = Color.Gray)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(commonChords) { c ->
                val isSel = c in selectedChords
                pEQFilterChip(
                    selected = isSel,
                    onClick = {
                        selectedChords = if (isSel) selectedChords - c else selectedChords + c
                    },
                    label = { Text(c) }
                )
            }
        }

        Text("BEST MATCHING KEYS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E8B9B))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            keyScores.take(3).forEach { (key, score) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E2838),
                    border = BorderStroke(1.dp, Color(0xFF3B4860)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$key Major", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB347))
                        Text("$score / ${selectedChords.size} Chords", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChordFinderTool(viewModel: AppViewModel, roots: List<String>) {
    var frets by remember { mutableStateOf(listOf(-1, 3, 2, 0, 1, 0)) }
    val strings = remember { listOf("E (6th)", "A (5th)", "D (4th)", "G (3rd)", "B (2nd)", "e (1st)") }
    val stringOpenMidi = remember { listOf(40, 45, 50, 55, 59, 64) }

    // Detect active notes
    val soundedNotes = remember(frets) {
        frets.mapIndexedNotNull { strIdx, fret ->
            if (fret >= 0) {
                val midi = stringOpenMidi[strIdx] + fret
                val noteName = roots[midi % 12]
                noteName to midi
            } else null
        }
    }

    val detectedName = remember(soundedNotes) {
        val noteNames = soundedNotes.map { it.first }.distinct()
        if (noteNames.isEmpty()) "Muted / No Frets Selected"
        else if (noteNames.size == 1) "${noteNames[0]} Single Note"
        else {
            // Find root and match common triad/seventh
            val rootCandidate = soundedNotes.firstOrNull()?.first ?: "C"
            val allNotePitches = soundedNotes.map { it.second % 12 }.distinct()
            val rIdx = roots.indexOf(rootCandidate).coerceAtLeast(0)
            val intervals = allNotePitches.map { (it - rIdx + 12) % 12 }.toSet()
            
            when {
                intervals.contains(4) && intervals.contains(7) && intervals.contains(11) -> "$rootCandidate maj7"
                intervals.contains(4) && intervals.contains(7) && intervals.contains(10) -> "$rootCandidate 7"
                intervals.contains(3) && intervals.contains(7) && intervals.contains(10) -> "$rootCandidate m7"
                intervals.contains(4) && intervals.contains(7) -> "$rootCandidate Major"
                intervals.contains(3) && intervals.contains(7) -> "$rootCandidate Minor"
                intervals.contains(5) && intervals.contains(7) -> "$rootCandidate sus4"
                intervals.contains(2) && intervals.contains(7) -> "$rootCandidate sus2"
                intervals.contains(3) && intervals.contains(6) -> "$rootCandidate dim"
                intervals.contains(4) && intervals.contains(8) -> "$rootCandidate aug"
                else -> "$rootCandidate (" + noteNames.joinToString("-") + ")"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Reverse Chord Finder (Find Chords by Shape)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8))
        Text("Tap frets for each guitar string from 6th (low E) to 1st (high e) to detect the chord name, notes, and voicing inversion.", fontSize = 12.sp, color = Color.Gray)

        // String selector rows
        strings.forEachIndexed { sIdx, strName ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strName, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(65.dp), fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val fretOptions = listOf(-1, 0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 12)
                    items(fretOptions) { f ->
                        val isSel = frets[sIdx] == f
                        pEQFilterChip(
                            selected = isSel,
                            onClick = {
                                val next = frets.toMutableList()
                                next[sIdx] = f
                                frets = next
                            },
                            label = { Text(if (f == -1) "✕" else "$f", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    frets = listOf(-1, -1, -1, -1, -1, -1)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3242)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Clear All Strings", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    soundedNotes.forEach { (_, midi) ->
                        viewModel.audioEngine.setInstrument("GUITAR")
                        viewModel.audioEngine.noteOn(midi, 0.85f)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB47BC)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Strum Voicing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Result Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF221A2B),
            border = BorderStroke(1.dp, Color(0xFFAB47BC).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("IDENTIFIED CHORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8), letterSpacing = 1.sp)
                Text(detectedName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                val notesString = soundedNotes.joinToString(" • ") { "${it.first} (${it.second})" }
                Text("Sounded Notes: " + notesString.ifEmpty { "None" }, fontSize = 12.sp, color = Color(0xFFD1C4E9))
                Text("Tab Shape: [" + frets.joinToString(", ") { if (it == -1) "X" else "$it" } + "]", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ScaleFinderTool(roots: List<String>) {
    var inputNotes by remember { mutableStateOf(setOf("C", "E", "G")) }

    // Comprehensive scale formulas
    val scaleTypes = remember {
        listOf(
            "Major / Ionian" to listOf(0, 2, 4, 5, 7, 9, 11),
            "Natural Minor / Aeolian" to listOf(0, 2, 3, 5, 7, 8, 10),
            "Dorian" to listOf(0, 2, 3, 5, 7, 9, 10),
            "Phrygian" to listOf(0, 1, 3, 5, 7, 8, 10),
            "Lydian" to listOf(0, 2, 4, 6, 7, 9, 11),
            "Mixolydian" to listOf(0, 2, 4, 5, 7, 9, 10),
            "Locrian" to listOf(0, 1, 3, 5, 6, 8, 10),
            "Melodic Minor" to listOf(0, 2, 3, 5, 7, 9, 11),
            "Harmonic Minor" to listOf(0, 2, 3, 5, 7, 8, 11),
            "Major Pentatonic" to listOf(0, 2, 4, 7, 9),
            "Minor Pentatonic" to listOf(0, 3, 5, 7, 10),
            "Blues Scale" to listOf(0, 3, 5, 6, 7, 10)
        )
    }

    val matchedScales = remember(inputNotes) {
        val results = mutableListOf<Triple<String, String, List<String>>>()
        roots.forEach { root ->
            val rootIdx = roots.indexOf(root)
            scaleTypes.forEach { (scaleName, intervals) ->
                val scaleNotes = intervals.map { roots[(rootIdx + it) % 12] }
                if (inputNotes.all { it in scaleNotes }) {
                    results.add(Triple(root, scaleName, scaleNotes))
                }
            }
        }
        results
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Scale Finder (Find Scales From Notes)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
        Text("Select any combination of notes on the wheel to identify matching scales, modes, and pentatonics.", fontSize = 12.sp, color = Color.Gray)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(roots) { r ->
                val isSel = r in inputNotes
                pEQFilterChip(
                    selected = isSel,
                    onClick = { inputNotes = if (isSel) inputNotes - r else inputNotes + r },
                    label = { Text(r, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { inputNotes = setOf("C", "E", "G") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3242)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Preset: C Major Triad", fontSize = 10.sp)
            }
            Button(
                onClick = { inputNotes = setOf("A", "C", "E") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3242)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Preset: A Minor Triad", fontSize = 10.sp)
            }
            Button(
                onClick = { inputNotes = emptySet() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2024)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Clear Notes", fontSize = 10.sp, color = Color(0xFFFF8A80))
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141F16),
            border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "MATCHING SCALES (${matchedScales.size} found containing ${inputNotes.joinToString(", ")})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )

                if (matchedScales.isEmpty()) {
                    Text("No common scale contains all selected notes together.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    matchedScales.take(8).forEach { (root, scaleName, scaleNotes) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1A2A1E),
                            border = BorderStroke(1.dp, Color(0xFF2B4230)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("$root $scaleName", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Scale Notes: " + scaleNotes.joinToString(" - "), fontSize = 11.sp, color = Color(0xFFA5D6A7))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetronomeTool(viewModel: AppViewModel) {
    var bpm by remember { mutableStateOf(120) }
    var isRunning by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isRunning, bpm) {
        if (isRunning) {
            val intervalMs = (60000L / bpm).coerceAtLeast(100L)
            while (isRunning) {
                try {
                    // Play metronome click
                    val midiClick = if (currentBeat == 1) 76 else 77
                    viewModel.audioEngine.noteOn(midiClick, 0.9f)
                } catch (_: Exception) {}
                delay(intervalMs)
                currentBeat = if (currentBeat >= 4) 1 else currentBeat + 1
            }
        } else {
            currentBeat = 1
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Metronome", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9E1B))
        Spacer(Modifier.height(10.dp))
        Text("$bpm BPM", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..4).forEach { b ->
                val active = isRunning && currentBeat == b
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) Color(0xFFFF9E1B) else Color(0xFF2C3242),
                    modifier = Modifier.size(24.dp)
                ) {}
            }
        }

        Spacer(Modifier.height(16.dp))
        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.toInt() },
            valueRange = 40f..240f,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { isRunning = !isRunning },
            colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0xFFE53935) else Color(0xFFFF9E1B))
        ) {
            Text(if (isRunning) "STOP METRONOME" else "START METRONOME", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimeSignaturesTool() {
    val meterSignatures = remember {
        listOf(
            Triple("4/4", "Common Time", "4 quarter notes per measure (STRONG - weak - medium - weak). Found in 90% of Pop, Rock, and Electronic music."),
            Triple("3/4", "Waltz Meter", "3 quarter notes per measure (STRONG - weak - weak). Found in waltzes, ballads, and classical music."),
            Triple("6/8", "Compound Duple", "Two main beats, each divided into three eighth notes (ONE-two-three, FOUR-five-six). Found in Blues, Afro-Cuban, and Celtic."),
            Triple("2/4", "Marching Duple", "2 quarter notes per measure (ONE - two). Found in Polka, Marches, and Country train-beats."),
            Triple("5/4", "Asymmetric (Dave Brubeck)", "Compound odd meter grouped as 3+2 or 2+3. Famous from 'Take Five' and 'Mission Impossible'."),
            Triple("7/8", "Progressive / Balkan", "Fast odd meter subdivided as 2+2+3 or 3+2+2. Found in Balkan folk, Prog Rock (Pink Floyd's 'Money')."),
            Triple("12/8", "Slow Blues Shuffle", "Four main beats with triplet subdivisions (ONE-and-a, TWO-and-a, THREE-and-a, FOUR-and-a).")
        )
    }

    var selectedMeter by remember { mutableStateOf("4/4") }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Time Signatures & Rhythmic Meters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB347))
        Text("Explore pulse subdivisions, accents, and musical feels across standard and odd meters.", fontSize = 12.sp, color = Color.Gray)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(meterSignatures) { (meter, name, _) ->
                val isSel = selectedMeter == meter
                pEQFilterChip(
                    selected = isSel,
                    onClick = { selectedMeter = meter },
                    label = { Text("$meter ($name)", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        val activeItem = meterSignatures.find { it.first == selectedMeter } ?: meterSignatures[0]

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E2430),
            border = BorderStroke(1.dp, Color(0xFF2B3444))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activeItem.first} - ${activeItem.second}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF9E1B).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Meter",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB347),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = activeItem.third,
                    fontSize = 13.sp,
                    color = Color(0xFF9EABB8),
                    lineHeight = 18.sp
                )

                // Visual Beat Grid
                val beatCount = when (activeItem.first) {
                    "3/4" -> 3
                    "2/4" -> 2
                    "5/4" -> 5
                    "6/8" -> 6
                    "7/8" -> 7
                    "12/8" -> 12
                    else -> 4
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..beatCount).forEach { beat ->
                        val isPrimary = beat == 1
                        val isSecondary = (activeItem.first == "4/4" && beat == 3) || (activeItem.first == "6/8" && beat == 4) || (activeItem.first == "12/8" && beat in listOf(4, 7, 10))
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isPrimary -> Color(0xFFFF9E1B)
                                isSecondary -> Color(0xFFFF9E1B).copy(alpha = 0.5f)
                                else -> Color(0xFF2C3242)
                            }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$beat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPrimary) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

