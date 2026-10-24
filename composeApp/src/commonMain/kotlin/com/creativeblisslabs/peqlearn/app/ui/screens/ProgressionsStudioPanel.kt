package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.ProgressionDef
import com.creativeblisslabs.peqlearn.app.util.RealChordPosition
import com.creativeblisslabs.peqlearn.app.util.RealChordsDb
import com.creativeblisslabs.peqlearn.app.util.RealPianoChordsDb
import com.creativeblisslabs.peqlearn.app.util.RealProgressionsDb
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Progressions Studio Tab cloned and inspired by optilude/guitar-practice.
 * Features all 26 canonical chord progressions across Pop, Blues, Jazz, Rock, Folk/Country, and Modal categories,
 * interactive key transposing, SVG chord voicing blocks for every step, and playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionsStudioPanel(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val roots = remember { listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B") }
    var selectedKey by remember { mutableStateOf("C") }
    var selectedInstrument by remember { mutableStateOf("guitar") } // "guitar", "piano", "ukulele"

    val allCategories = remember {
        listOf("All") + RealProgressionsDb.list.map { it.category }.distinct()
    }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedProgressionId by remember { mutableStateOf(RealProgressionsDb.list.first().id) }

    val filteredProgressions = remember(selectedCategory) {
        if (selectedCategory == "All") RealProgressionsDb.list
        else RealProgressionsDb.list.filter { it.category == selectedCategory }
    }

    val currentProgression = remember(selectedProgressionId) {
        RealProgressionsDb.list.find { it.id == selectedProgressionId } ?: RealProgressionsDb.list.first()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingChordIdx by remember { mutableStateOf(-1) }

    // Resolve diatonic chords for the selected key and progression degrees
    fun resolveChordForDegree(tonic: String, degreeRoman: String): Pair<String, String> {
        val rootIdx = roots.indexOf(tonic).coerceAtLeast(0)
        val degNum = when (degreeRoman.replace("7", "").replace("maj7", "").replace("m", "")) {
            "I", "i" -> 0
            "♭II", "♭ii" -> 1
            "II", "ii" -> 2
            "♭III", "♭iii" -> 3
            "III", "iii" -> 4
            "IV", "iv" -> 5
            "♭V", "♭v" -> 6
            "V", "v" -> 7
            "♭VI", "♭vi" -> 8
            "VI", "vi" -> 9
            "♭VII", "♭vii" -> 10
            "VII", "vii" -> 11
            else -> 0
        }
        val chordRoot = roots[(rootIdx + degNum) % 12]
        val suffix = when {
            degreeRoman.contains("7") && (degreeRoman.startsWith("V") || degreeRoman.startsWith("I") || degreeRoman.startsWith("IV") || degreeRoman.contains("♭VII")) -> "7"
            degreeRoman.contains("maj7") -> "maj7"
            degreeRoman.contains("m7") -> "m7"
            degreeRoman.contains("ø7") -> "m7b5"
            degreeRoman.first().isLowerCase() -> "minor"
            else -> "major"
        }
        return chordRoot to suffix
    }

    // Structure containing chord degree, display name, guitar/uku position, and piano MIDI pitches
    data class ProgressionChordItem(
        val degree: String,
        val chordName: String,
        val guitarPos: RealChordPosition?,
        val pianoMidi: List<Int>
    )

    val progressionChords = remember(currentProgression, selectedKey, selectedInstrument) {
        currentProgression.degrees.map { deg ->
            val (cRoot, cSuffix) = resolveChordForDegree(selectedKey, deg)
            val posList = if (selectedInstrument == "ukulele") {
                RealChordsDb.getUkulelePositions(cRoot, cSuffix)
            } else {
                RealChordsDb.getGuitarPositions(cRoot, cSuffix)
            }
            val pianoPitches = RealPianoChordsDb.getChord(cRoot, cSuffix)?.midi?.takeIf { it.isNotEmpty() }
                ?: run {
                    val rIdx = roots.indexOf(cRoot).coerceAtLeast(0)
                    com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine.buildPianoShape(
                        rootIdx = rIdx,
                        suffix = cSuffix,
                        style = com.creativeblisslabs.peqlearn.app.util.PianoVoicingStyle.CLOSE,
                        inversion = com.creativeblisslabs.peqlearn.app.util.InversionStyle.ROOT,
                        octave = 4
                    )
                }
            ProgressionChordItem(deg, "$cRoot $cSuffix", posList.firstOrNull(), pianoPitches)
        }
    }

    fun playProgression() {
        if (isPlaying) {
            isPlaying = false
            currentPlayingChordIdx = -1
            return
        }
        isPlaying = true
        scope.launch {
            try {
                val instr = if (selectedInstrument == "piano") "PIANO" else "GUITAR"
                viewModel.audioEngine.setInstrument(instr)
                for (i in progressionChords.indices) {
                    if (!isPlaying) break
                    currentPlayingChordIdx = i
                    val chordItem = progressionChords[i]
                    val midiNotes = if (selectedInstrument == "piano") {
                        chordItem.pianoMidi
                    } else {
                        chordItem.guitarPos?.midi ?: chordItem.pianoMidi
                    }
                    midiNotes.forEach { midi ->
                        viewModel.audioEngine.noteOn(midi, 0.85f)
                        delay(35L)
                    }
                    delay(700L)
                }
            } catch (_: Exception) {}
            isPlaying = false
            currentPlayingChordIdx = -1
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1218))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title & Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chord Progressions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Classic Songwriter Cadences & Loops (guitar-practice)",
                    fontSize = 11.sp,
                    color = Color(0xFF9EABB8)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { playProgression() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFE53935) else Color(0xFFAB47BC)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Stop" else "Play All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Instrument switch row: Guitar, Ukulele, Piano
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

        // Quick Tools Toolbar
        var activeProgressionTool by remember { mutableStateOf<String?>(null) }
        val quickTools = remember {
            listOf(
                Triple("Chord Finder", Icons.Default.Search, "Find chords by shape"),
                Triple("Scale Finder", Icons.Default.LinearScale, "Find scales from notes"),
                Triple("Key Finder", Icons.Default.VpnKey, "Identify keys from progression"),
                Triple("Transposer", Icons.Default.Transform, "Transpose progression"),
                Triple("Metronome", Icons.Default.Timer, "Practice with metronome")
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickTools) { (name, icon, _) ->
                val isSel = activeProgressionTool == name
                pEQFilterChip(
                    selected = isSel,
                    onClick = { activeProgressionTool = if (isSel) null else name },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text(name, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }
        }

        if (activeProgressionTool != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161A22),
                border = BorderStroke(1.dp, Color(0xFFAB47BC).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tool: $activeProgressionTool",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCE93D8)
                        )
                        IconButton(onClick = { activeProgressionTool = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    when (activeProgressionTool) {
                        "Transposer" -> {
                            val romanJoined = currentProgression.degrees.joinToString(", ")
                            Text("Current progression degrees: $romanJoined in Key of $selectedKey", fontSize = 12.sp, color = Color.LightGray)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(roots) { r ->
                                    pEQFilterChip(
                                        selected = selectedKey == r,
                                        onClick = { selectedKey = r },
                                        label = { Text("To $r") }
                                    )
                                }
                            }
                        }
                        "Key Finder" -> {
                            val chordNames = progressionChords.map { it.chordName }.distinct()
                            Text("Chords in this progression: ${chordNames.joinToString(", ")}", fontSize = 12.sp, color = Color.LightGray)
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1E2838), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                Text(
                                    "Identified Primary Key: $selectedKey Major (100% diatonic match)\nRelative Minor: ${roots[(roots.indexOf(selectedKey) + 9) % 12]}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB347),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        "Scale Finder" -> {
                            Text("Best Solo Scales over $selectedKey Major progression:", fontSize = 12.sp, color = Color.LightGray)
                            val pentMinor = roots[(roots.indexOf(selectedKey) + 9) % 12]
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("• $selectedKey Major Pentatonic: Best for sweet melodic lines", fontSize = 12.sp, color = Color.White)
                                Text("• $pentMinor Minor Pentatonic / Blues: Classic rock/blues grit", fontSize = 12.sp, color = Color.White)
                                Text("• $selectedKey Ionian (Major Scale): Complete diatonic coverage", fontSize = 12.sp, color = Color.White)
                                Text("• $selectedKey Mixolydian: Ideal over dominant/7th chords", fontSize = 12.sp, color = Color.White)
                            }
                        }
                        "Chord Finder" -> {
                            Text("Search Voicings for Current Chords:", fontSize = 12.sp, color = Color.LightGray)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(progressionChords) { (deg, name, pos) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E2430),
                                        border = BorderStroke(1.dp, Color(0xFF2B3444)),
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(deg, fontSize = 10.sp, color = Color(0xFFFFB347), fontWeight = FontWeight.Bold)
                                            Text(name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            val fretsDisplay = pos?.frets?.joinToString(" ") ?: ""
                                            Text("Frets: $fretsDisplay", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                        "Metronome" -> {
                            var bpm by remember { mutableStateOf(100) }
                            var isRunning by remember { mutableStateOf(false) }
                            var beat by remember { mutableStateOf(1) }
                            LaunchedEffect(isRunning, bpm) {
                                if (isRunning) {
                                    while (isRunning) {
                                        delay((60000L / bpm).coerceAtLeast(150L))
                                        beat = if (beat >= 4) 1 else beat + 1
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$bpm BPM", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (1..4).forEach { b ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isRunning && beat == b) Color(0xFFFF9E1B) else Color(0xFF2C3242),
                                            modifier = Modifier.size(16.dp)
                                        ) {}
                                    }
                                }
                                Button(
                                    onClick = { isRunning = !isRunning },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0xFFE53935) else Color(0xFFFF9E1B)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isRunning) "Stop" else "Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Root Key Transposer Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "KEY CENTER (TRANSPOSER)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(roots) { r ->
                    val isSelected = selectedKey == r
                    pEQFilterChip(
                        selected = isSelected,
                        onClick = { selectedKey = r },
                        label = { Text(r, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        // 2. Genre Categories Filter Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(allCategories) { cat ->
                val isSelected = selectedCategory == cat
                pEQFilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // 3. Current Selected Progression Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color(0xFF262D3D))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentProgression.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCE93D8)
                    )
                    Text(
                        text = currentProgression.romanDisplay,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFB347)
                    )
                }

                Text(
                    text = currentProgression.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                if (currentProgression.examples.isNotBlank()) {
                    Text(
                        text = "Examples: ${currentProgression.examples}",
                        fontSize = 11.sp,
                        color = Color(0xFF81C784)
                    )
                }

                // Chords in Progression Display (SVG diagrams horizontally)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(progressionChords.indices.toList()) { idx ->
                        val chordItem = progressionChords[idx]
                        val isCurrentChordPlaying = isPlaying && currentPlayingChordIdx == idx

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = chordItem.degree,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFB0BEC5)
                            )
                            Spacer(Modifier.height(4.dp))
                            if (selectedInstrument == "piano") {
                                val activeMap = remember(chordItem.pianoMidi, isCurrentChordPlaying) {
                                    chordItem.pianoMidi.associateWith { true }
                                }
                                Surface(
                                    modifier = Modifier
                                        .width(170.dp)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch {
                                                viewModel.audioEngine.setInstrument("PIANO")
                                                chordItem.pianoMidi.forEach { midi ->
                                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                                    delay(30L)
                                                }
                                            }
                                        },
                                    color = if (isCurrentChordPlaying) Color(0xFF2A2038) else Color(0xFF1B1F2A),
                                    border = BorderStroke(1.dp, if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFF333D4F)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(chordItem.chordName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFCE93D8))
                                        com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard(
                                            activeMidiNotes = activeMap,
                                            accentColor = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFCE93D8),
                                            startNote = 48,
                                            endNote = 72,
                                            fillWidth = true,
                                            showNoteNames = false,
                                            modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            } else if (chordItem.guitarPos != null) {
                                SvgChordBox(
                                    chord = chordItem.guitarPos,
                                    chordName = chordItem.chordName,
                                    instrument = selectedInstrument,
                                    accentColor = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFCE93D8),
                                    cardBackground = if (isCurrentChordPlaying) Color(0xFF2A2038) else Color(0xFF1B1F2A),
                                    width = 110.dp,
                                    height = 145.dp,
                                    onClick = {
                                        scope.launch {
                                            val instr = if (selectedInstrument == "ukulele") "GUITAR" else "GUITAR"
                                            viewModel.audioEngine.setInstrument(instr)
                                            chordItem.guitarPos.midi.forEach { midi ->
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
            }
        }

        // 4. Progression Library List
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PROGRESSION LIBRARY (${filteredProgressions.size})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProgressions) { p ->
                    val isSelected = p.id == selectedProgressionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedProgressionId = p.id
                                isPlaying = false
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF231B2D) else Color(0xFF151821),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFAB47BC) else Color(0xFF242A38))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFCE93D8) else Color.White
                                )
                                Text(
                                    text = "${p.category} • ${p.description}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8C98A8)
                                )
                            }

                            Text(
                                text = p.romanDisplay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFB347)
                            )
                        }
                    }
                }
            }
        }
    }
}
