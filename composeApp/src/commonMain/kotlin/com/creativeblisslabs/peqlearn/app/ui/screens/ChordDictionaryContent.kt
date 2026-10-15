package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

enum class ChordInstrumentType(val label: String) {
    PIANO("🎹"),
    GUITAR("🎸"),
    UKULELE("🪕")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordDictionaryPanel(
    viewModel: AppViewModel,
    unifiedFormat: String = "STANDARD",
    onFormatChange: (String) -> Unit = {},
    onPracticeTapped: () -> Unit = {},
    onBack: () -> Unit = {},
    isScrollable: Boolean = false
) {
    val partColor = Color(0xFFFFB347)
    val scrollState = rememberScrollState()

    val roots = remember { listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B") }
    val chordTypes = remember {
        listOf(
            "Major", "Minor", "7", "maj7", "m7", "dim", "aug",
            "sus2", "sus4", "7sus4", "add9", "9", "m9", "11", "13"
        )
    }

    var mode by remember { mutableStateOf(0) }
    var rootIdx by remember { mutableStateOf(0) }
    var chordTypeIdx by remember { mutableStateOf(0) }
    var selectedInstrument by remember { mutableStateOf(ChordInstrumentType.GUITAR) }
    var selectedDiff by remember { mutableStateOf(ChordDiff.EASY) }
    var capoFret by remember { mutableStateOf(0) }
    var fromKeyIdx by remember { mutableStateOf(0) }
    var toKeyIdx by remember { mutableStateOf(7) }

    var selectedScaleIdx by remember { mutableStateOf(0) }
    var selectedIntervalIdx by remember { mutableStateOf(0) }
    var selectedVoicingIdx by remember { mutableStateOf(0) }
    var selectedOctaveSpan by remember { mutableStateOf(1) }
    var selectedScaleBoxIdx by remember { mutableStateOf(0) }
    var selectedStartFret by remember { mutableStateOf(1) }
    var selectedPianoOctave by remember { mutableStateOf(4) }
    var selectedVoicingType by remember { mutableStateOf("Open") }

    val guitarVoicings = listOf("Open", "Barre", "Power", "Shell")
    val pianoVoicings = listOf("Closed", "Open", "Shell", "Drop 2")
    val ukeVoicings = listOf("Open", "Barre", "Closed")

    var showCircleOfFifths by remember { mutableStateOf(false) }
    var isLabModeOpen by remember { mutableStateOf(false) }

    val voicingOptions = remember { listOf("Root Position", "1st Inversion", "2nd Inversion", "Drop 2") }
    val octaveOptions = remember { listOf("2 Octaves", "3 Octaves") }
    val scope = rememberCoroutineScope()
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    val scaleFormulas = remember {
        listOf(
            "Major (Bilawal)" to SCALE_MAJOR_PATTERN,
            "Natural Minor (Asavari)" to SCALE_MINOR_NAT_PATTERN,
            "Harmonic Minor" to SCALE_MINOR_HAR_PATTERN,
            "Melodic Minor" to SCALE_MINOR_MEL_PATTERN,
            "Major Pentatonic" to SCALE_PENTATONIC_M_PATTERN,
            "Minor Pentatonic" to SCALE_PENTATONIC_N_PATTERN,
            "Blues Scale" to SCALE_BLUES_PATTERN,
            "Dorian Mode (Kafi)" to SCALE_DORIAN_PATTERN,
            "Mixolydian Mode (Khamaj)" to SCALE_MIXOLYDIAN_PATTERN,
            "Lydian Mode (Kayan)" to SCALE_LYDIAN_PATTERN,
            "Phrygian (Bhairavi)" to SCALE_BHAIRAVI_PATTERN,
            "Bhairav Scale" to SCALE_BHAIRAV_PATTERN,
            "Thaat Kalyan (Yaman)" to SCALE_KALYAN_PATTERN,
            "Thaat Marwa" to SCALE_MARWA_PATTERN,
            "Thaat Poorvi" to SCALE_POORVI_PATTERN,
            "Thaat Todi" to SCALE_TODI_PATTERN,
            "Mayamalavagowla Parent" to SCALE_BHAIRAV_PATTERN
        )
    }

    val typeDef = CHORD_TYPE_DEFS[chordTypeIdx.coerceIn(0, CHORD_TYPE_DEFS.lastIndex)]
    val chordName = roots[rootIdx] + typeDef.suffix
    val scalePair = scaleFormulas[selectedScaleIdx.coerceIn(0, scaleFormulas.lastIndex)]

    var currentPlayingIdx by remember { mutableStateOf(-1) }
    var isArpeggioRunning by remember { mutableStateOf(false) }

    val voicedIntervals = remember(chordTypeIdx, selectedVoicingIdx, selectedIntervalIdx) {
        val base = typeDef.intervals.toMutableList()
        if (selectedIntervalIdx > 0 && selectedIntervalIdx in INTERVALS_DATA.indices) {
            val semitones = INTERVALS_DATA[selectedIntervalIdx][4].toIntOrNull() ?: 0
            if (!base.contains(semitones)) { base.add(semitones); base.sort() }
        }
        when (selectedVoicingIdx) {
            1 -> { val m = base.toMutableList(); m[0] += 12; m.sorted() }
            2 -> { val m = base.toMutableList(); if (m.isNotEmpty()) m[0] += 12; if (m.size >= 2) m[1] += 12; m.sorted() }
            3 -> {
                if (base.size >= 4) { val m = base.toMutableList(); m[m.size - 2] -= 12; m.sorted() } else base
            }
            else -> base
        }
    }

    val activeMidiMap = remember(rootIdx, chordTypeIdx, selectedDiff, voicedIntervals, mode, selectedScaleIdx, capoFret) {
        val targetMap = mutableMapOf<Int, Boolean>()
        val baseMidi = 60 + rootIdx

        if (mode == 0) {
            voicedIntervals.forEach { targetMap[baseMidi + it] = true }
        } else {
            scalePair.second.forEach { targetMap[baseMidi + it] = true }
        }
        targetMap
    }

    val pianoPitches = remember(activeMidiMap) { activeMidiMap.keys.toList().sorted() }
    val noteNames = remember(pianoPitches, mode) { pianoPitches.map { roots[it % 12] } }

    val pianoFingering = remember(pianoPitches, typeDef.suffix, mode, selectedVoicingIdx) {
        if (mode == 0) {
            val baseList = PIANO_RH_FINGERS[typeDef.suffix] ?: when (pianoPitches.size) {
                1 -> listOf(1)
                2 -> listOf(1, 5)
                3 -> when (selectedVoicingIdx) {
                    1 -> listOf(1, 2, 5) // 1st inversion
                    2 -> listOf(1, 3, 5) // 2nd inversion
                    else -> listOf(1, 3, 5) // root position
                }
                4 -> listOf(1, 2, 4, 5)
                else -> listOf(1, 2, 3, 4, 5)
            }
            val fingers = pianoPitches.indices.map { i ->
                baseList.getOrElse(i) { (i % 5) + 1 }
            }
            FingeringData(notes = pianoPitches, fingers = fingers)
        } else {
            // Scale fingering for right hand
            val rhScalePattern = listOf(1, 2, 3, 1, 2, 3, 4, 5)
            val fingers = pianoPitches.indices.map { i ->
                rhScalePattern[i % rhScalePattern.size]
            }
            FingeringData(notes = pianoPitches, fingers = fingers)
        }
    }

    val guitarShape = remember(rootIdx, chordTypeIdx, selectedDiff, capoFret, selectedStartFret, selectedVoicingType, mode) {
        ChordVoicingEngine.generateDynamicShape(
            rootMidiNote = (rootIdx + capoFret) % 12,
            targetSemitones = typeDef.intervals,
            voicingStyle = selectedVoicingType,
            startFret = selectedStartFret,
            openStringPitches = listOf(4, 9, 2, 7, 11, 4),
            isPiano = false
        )
    }

    val ukuShape = remember(rootIdx, chordTypeIdx, selectedDiff, capoFret, selectedStartFret, selectedVoicingType, mode) {
        ChordVoicingEngine.generateDynamicShape(
            rootMidiNote = (rootIdx + capoFret) % 12,
            targetSemitones = typeDef.intervals,
            voicingStyle = selectedVoicingType,
            startFret = selectedStartFret,
            openStringPitches = listOf(7, 0, 4, 9),
            isPiano = false
        )
    }

    val cadenceList = remember(rootIdx) {
        listOf(
            Pair("${roots[(rootIdx + 7) % 12]} (V)", listOf(rootIdx + 7, rootIdx + 11, rootIdx + 2)),
            Pair("${roots[(rootIdx + 5) % 12]} (IV)", listOf(rootIdx + 5, rootIdx + 9, rootIdx)),
            Pair("${roots[(rootIdx + 9) % 12]}m (vi)", listOf(rootIdx + 9, rootIdx, rootIdx + 4)),
            Pair("${roots[rootIdx]} (I)", listOf(rootIdx, rootIdx + 4, rootIdx + 7))
        )
    }

    LaunchedEffect(activeMidiMap, isArpeggioRunning) {
        if (!isArpeggioRunning) {
            viewModel.midiActiveNotes.clear()
            viewModel.midiActiveNotes.putAll(activeMidiMap)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackJob?.cancel()
            viewModel.midiActiveNotes.clear()
            viewModel.midiPlayingNote.value = null
        }
    }

    LaunchedEffect(selectedInstrument) {
        selectedVoicingType = when (selectedInstrument) {
            ChordInstrumentType.PIANO -> "Closed"
            ChordInstrumentType.UKULELE -> "Open"
            else -> "Open"
        }
        try {
            val instr = when (selectedInstrument) {
                ChordInstrumentType.GUITAR, ChordInstrumentType.UKULELE -> "GUITAR"
                ChordInstrumentType.PIANO -> "PIANO"
            }
            getAudioEngine().setInstrument(instr)
        } catch (e: Exception) {
            println("pEQLearn: [INSTRUMENT-SWITCH] Failed to sync audio engine pointer: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
     
Row(
    modifier = Modifier.fillMaxWidth(), // Ensures the top row fills available bounds
    horizontalArrangement = Arrangement.spacedBy(8.dp), 
    verticalAlignment = Alignment.CenterVertically
) {
    // 1. Added Modifier.weight(1f) here so it leaves room for the buttons
    Box(modifier = Modifier.weight(1f)) {
        InstrumentSelectionRow(
            selectedInstrument = selectedInstrument,
            onInstrumentChange = { selectedInstrument = it }
        )
    }

    // 2. Lab Toggle Button (Will now display perfectly)
    FilledTonalButton(
        onClick = { isLabModeOpen = true },
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = partColor.copy(alpha = 0.2f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), // Cleans up inner spacing bounds
        modifier = Modifier.height(40.dp) // Matches the height profile of the instrument selector row
    ) {
        Icon(Icons.Default.Science, contentDescription = null, tint = partColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = partColor)
    }

    // 3. Circle of Fifths Button
    IconButton(
        onClick = { showCircleOfFifths = !showCircleOfFifths },
        modifier = Modifier.size(40.dp) // Keeps a consistent touch target profile matching 40.dp height
    ) {
        Icon(
            imageVector = Icons.Default.Adjust,
            contentDescription = "Circle of Fifths",
            tint = if (showCircleOfFifths) partColor else Color.White.copy(alpha = 0.7f)
        )
    }
}
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == 0,
                onClick = { playbackJob?.cancel(); mode = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Chords", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            SegmentedButton(
                selected = mode == 1,
                onClick = { playbackJob?.cancel(); mode = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Scales", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        AnimatedVisibility(visible = showCircleOfFifths) {
            CircleOfFifthsVisualizer(
                selectedRootIdx = rootIdx,
                onSelectRoot = { rootIdx = it }
            )
        }

      

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    M3DropdownMenu(
                        label = "Root Key",
                        selectedText = roots[rootIdx],
                        items = roots,
                        onItemSelect = { playbackJob?.cancel(); rootIdx = it }
                    )
                }
                Box(modifier = Modifier.weight(1.5f)) {
                    if (mode == 0) {
                        M3DropdownMenu(
                            label = "Chord Suffix",
                            selectedText = typeDef.label,
                            items = chordTypes,
                            onItemSelect = { playbackJob?.cancel(); chordTypeIdx = it }
                        )
                    } else {
                        M3DropdownMenu(
                            label = "Scale Formula",
                            selectedText = scalePair.first,
                            items = scaleFormulas.map { it.first },
                            onItemSelect = { playbackJob?.cancel(); selectedScaleIdx = it }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1.3f)) {
                    if (mode == 0) {
                        val currentIntervalLabel = INTERVALS_DATA.getOrNull(selectedIntervalIdx)?.get(1)?.toString() ?: "None"
                        val intervalItems = listOf("None") + INTERVALS_DATA.drop(1).map { "${it[2]} (${it[4]}st)" }
                        M3DropdownMenu(
                            label = "Extension / Mod",
                            selectedText = currentIntervalLabel,
                            items = intervalItems,
                            onItemSelect = { playbackJob?.cancel(); selectedIntervalIdx = it }
                        )
                    } else {
                        M3DropdownMenu(
                            label = "Keyboard Range",
                            selectedText = octaveOptions[selectedOctaveSpan],
                            items = octaveOptions,
                            onItemSelect = { selectedOctaveSpan = it }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentVoicingList = when (selectedInstrument) {
                    ChordInstrumentType.GUITAR -> guitarVoicings
                    ChordInstrumentType.UKULELE -> ukeVoicings
                    ChordInstrumentType.PIANO -> pianoVoicings
                }
                Box(modifier = Modifier.weight(1f)) {
                    M3DropdownMenu(
                        label = "Voicing Style",
                        selectedText = selectedVoicingType,
                        items = currentVoicingList,
                        onItemSelect = { selectedVoicingType = currentVoicingList[it] }
                    )
                }
                if (selectedInstrument == ChordInstrumentType.PIANO) {
                    if (mode == 0) {
                        Box(modifier = Modifier.weight(1f)) {
                            M3DropdownMenu(
                                label = "Piano Voicing",
                                selectedText = voicingOptions[selectedVoicingIdx],
                                items = voicingOptions,
                                onItemSelect = { selectedVoicingIdx = it }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        M3DropdownMenu(
                            label = "Fret Diff",
                            selectedText = selectedDiff.label,
                            items = ChordDiff.entries.map { it.label },
                            onItemSelect = { selectedDiff = ChordDiff.entries[it] }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    M3DropdownMenu(
                        label = "Start Fret",
                        selectedText = "Fret $selectedStartFret",
                        items = (1..12).map { "Fret $it" },
                        onItemSelect = { selectedStartFret = it + 1 }
                    )
                }
            }
        }

        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayCapoText = if (capoFret > 0 && mode == 0 && selectedInstrument != ChordInstrumentType.PIANO) " (Capo $capoFret)" else ""
                    Text(
                        text = if (mode == 0) "$chordName$displayCapoText" else "${roots[rootIdx]} ${scalePair.first.split(" ")[0]}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = partColor
                    )

                    Row(
                        Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        noteNames.forEachIndexed { idx, name ->
                            val isActiveStep = idx == currentPlayingIdx
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = if (isActiveStep) FontWeight.Black else FontWeight.Bold,
                                color = if (isActiveStep) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .background(color = if (isActiveStep) partColor else Color.Transparent, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = if (isActiveStep) 6.dp else 2.dp, vertical = 2.dp)
                            )
                            if (idx < noteNames.lastIndex) Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                        }
                    }
                }

                InstrumentPlayButton(
                    accentColor = partColor,
                    label = "Play",
                    isPlaying = isArpeggioRunning,
                    onPlayClick = {
                        if (isArpeggioRunning) {
                            playbackJob?.cancel()
                            currentPlayingIdx = -1
                            viewModel.midiPlayingNote.value = null
                            viewModel.currentScore.value = null
                            viewModel.midiActiveNotes.clear()
                            viewModel.midiActiveNotes.putAll(activeMidiMap)
                            isArpeggioRunning = false
                        } else {
                            playbackJob?.cancel()
                            isArpeggioRunning = true

                            val mockNotesList = pianoPitches.mapIndexed { idx: Int, midiPitch: Int ->
                                val targetString = when (selectedInstrument) {
                                    ChordInstrumentType.GUITAR -> guitarShape.frets.indices.filter { guitarShape.frets[it] != -1 }.getOrNull(idx)
                                    ChordInstrumentType.UKULELE -> ukuShape.frets.indices.filter { ukuShape.frets[it] != -1 }.getOrNull(idx)
                                    else -> null
                                }
                                val targetFret = when (selectedInstrument) {
                                    ChordInstrumentType.GUITAR -> targetString?.let { guitarShape.frets[it] }
                                    ChordInstrumentType.UKULELE -> targetString?.let { ukuShape.frets[it] }
                                    else -> null
                                }

                                ScoreNote(
                                    midiNote = midiPitch,
                                    position = idx.toFloat() / pianoPitches.size.toFloat(),
                                    duration = 0.25f,
                                    string = targetString,
                                    fret = targetFret
                                )
                            }

                            viewModel.currentScore.value = Score(
                                title = "Live Audition",
                                timeSignature = "4/4",
                                measures = listOf(Measure(notes = mockNotesList))
                            )

                            playbackJob = scope.launch {
                                val instr = if (selectedInstrument == ChordInstrumentType.GUITAR || selectedInstrument == ChordInstrumentType.UKULELE) "GUITAR" else "PIANO"
                                try {
                                    viewModel.audioEngine.setInstrument(instr)
                                } catch (_: Exception) {}

                                pianoPitches.forEachIndexed { idx: Int, midiPitch: Int ->
                                    currentPlayingIdx = idx
                                    viewModel.scorePlayheadMeasure.value = 0
                                    viewModel.scorePlayheadOffset.value = idx.toFloat() / pianoPitches.size.toFloat()

                                    viewModel.midiPlayingNote.value = midiPitch
                                    viewModel.midiActiveNotes.clear()
                                    viewModel.midiActiveNotes[midiPitch] = true

                                    try {
                                        viewModel.audioEngine.noteOn(midiPitch, 0.85f)
                                    } catch (_: Exception) {}
                                    delay(500)
                                }

                                currentPlayingIdx = -1
                                viewModel.midiPlayingNote.value = null
                                viewModel.currentScore.value = null
                                viewModel.midiActiveNotes.clear()
                                viewModel.midiActiveNotes.putAll(activeMidiMap)
                                isArpeggioRunning = false
                            }
                        }
                    }
                )
            }
        }

        AnimatedContent(
            targetState = selectedInstrument,
            transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
            label = "instrument_view"
        ) { inst ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (inst) {
                    ChordInstrumentType.GUITAR -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        val openGuitarStrings = listOf(40, 45, 50, 55, 59, 64)
                        val highlightedGuitarCoordinate = remember(activeMidiToken, guitarShape.frets, isArpeggioRunning) {
                            if (isArpeggioRunning && activeMidiToken != null) {
                                var targetMatch: Pair<Int, Int>? = null
                                for (s in guitarShape.frets.indices) {
                                    val fret = guitarShape.frets[s]
                                    if (fret != -1 && (openGuitarStrings[s] + fret) % 12 == activeMidiToken % 12) {
                                        targetMatch = s to fret
                                        break
                                    }
                                }
                                targetMatch
                            } else null
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedGuitarFretboard(
                                numFrets = 13,
                                accentColor = partColor,
                                rootNote = rootIdx,
                                chordFrets = if (!isArpeggioRunning && mode == 0) guitarShape.frets else null,
                                chordFingers = if (!isArpeggioRunning && mode == 0) guitarShape.fingers else null,
                                activeCoordinate = highlightedGuitarCoordinate,
                                activeNotes = if (isArpeggioRunning) emptyList() else pianoPitches,
                                onFretTapped = { _, _, midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument("GUITAR")
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(2.dp))
                            GuitarFingerLegend(accentColor = partColor)
                        }
                    }
                    ChordInstrumentType.PIANO -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        val startNoteBoundary = (pianoPitches.minOrNull() ?: 60) - 2
                        val endNoteBoundary = (pianoPitches.maxOrNull() ?: 72) + (if (selectedOctaveSpan == 1) 24 else 12)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            UnifiedPianoKeyboard(
                                activeMidiNotes = activeMidiMap,
                                accentColor = partColor,
                                rootNote = rootIdx,
                                fingering = pianoFingering,
                                activeCoordinate = if (isArpeggioRunning) activeMidiToken else null,
                                startNote = startNoteBoundary,
                                endNote = endNoteBoundary,
                                showNoteNames = false,
                                isPlaying = isArpeggioRunning,
                                onKeyTapped = { midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument("PIANO")
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(108.dp).clip(RoundedCornerShape(8.dp))
                            )
                            PianoFingerLegend(
                                accentColor = partColor,
                                fingering = pianoFingering,
                                noteNames = noteNames
                            )
                        }
                    }
                    ChordInstrumentType.UKULELE -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        val highlightedUkuCoordinate = remember(activeMidiToken, ukuShape.frets, isArpeggioRunning) {
                            if (isArpeggioRunning && activeMidiToken != null) {
                                val openUkuStrings = listOf(67, 60, 64, 69)
                                var targetMatch: Pair<Int, Int>? = null
                                for (s in ukuShape.frets.indices) {
                                    val fret = ukuShape.frets[s]
                                    if (fret != -1 && (openUkuStrings[s] + fret) % 12 == activeMidiToken % 12) {
                                        targetMatch = s to fret
                                        break
                                    }
                                }
                                targetMatch
                            } else null
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedGuitarFretboard(
                                numFrets = 5,
                                accentColor = partColor,
                                rootNote = rootIdx,
                                chordFrets = if (mode == 0) ukuShape.frets else null,
                                chordFingers = if (mode == 0) ukuShape.fingers else null,
                                activeNotes = pianoPitches,
                                baseFret = if (mode == 0) ukuShape.baseFret else 1,
                                barreFret = -1,
                                stringLabels = listOf("G", "C", "E", "A"),
                                openStringsSemis = listOf(7, 0, 4, 9),
                                activeCoordinate = highlightedUkuCoordinate,
                                onFretTapped = { _, _, midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument("GUITAR")
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(2.dp))
                            GuitarFingerLegend(accentColor = partColor)
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Both", "Staff", "Tab").forEach { fmt ->
                val isSelected = unifiedFormat.equals(fmt, ignoreCase = true) ||
                    (fmt == "Staff" && unifiedFormat.contains("staff", ignoreCase = true)) ||
                    (fmt == "Tab" && unifiedFormat.contains("tab", ignoreCase = true))
                FilterChip(
                    selected = isSelected,
                    onClick = { onFormatChange(fmt) },
                    label = { Text(fmt, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = partColor.copy(alpha = 0.2f),
                        selectedLabelColor = partColor
                    )
                )
            }
        }

        StaffAndTabNotationViewer(
            score = viewModel.currentScore.value as Score?,
            playheadMeasure = viewModel.scorePlayheadMeasure.value,
            playheadOffset = viewModel.scorePlayheadOffset.value,
            activeMidiNotes = viewModel.midiActiveNotes,
            playingNote = viewModel.midiPlayingNote.value,
            primaryColor = partColor,
            currentFormat = unifiedFormat,
            onFormatChange = onFormatChange,
            showScoreHeader = false,
            modifier = Modifier.fillMaxWidth().height(240.dp),
            scrollState = scrollState
        )

        if (selectedInstrument == ChordInstrumentType.GUITAR || selectedInstrument == ChordInstrumentType.UKULELE) {
            HorizontalDivider(color = partColor.copy(0.1f))
            CapoSection(
                capoFret = capoFret,
                activeRootIdx = rootIdx,
                onCapoChange = { playbackJob?.cancel(); capoFret = it },
                partColor = partColor,
                onKeyDispatched = { rootIdx = it }
            )
        } else if (selectedInstrument == ChordInstrumentType.PIANO) {
            HorizontalDivider(color = partColor.copy(0.1f))
            TransposeSection(
                fromKeyIdx = fromKeyIdx,
                toKeyIdx = toKeyIdx,
                selectedScaleIdx = selectedScaleIdx,
                scaleFormulas = scaleFormulas,
                partColor = partColor,
                onFromChange = { playbackJob?.cancel(); fromKeyIdx = it },
                onToChange = { playbackJob?.cancel(); toKeyIdx = it },
                onKeyDispatched = { rootIdx = it }
            )
        }

        HorizontalDivider(color = partColor.copy(0.1f))
        EasyChordQuickRef(
            rootIdx = rootIdx,
            qualityIdx = chordTypeIdx,
            cadences = cadenceList,
            accentColor = partColor,
            onSelectChord = { label ->
                playbackJob?.cancel()
                val cleanLabel = label.substringBefore(" ").replace("m", "").replace("7", "").replace("dim", "").replace("aug", "")
                val targetIdx = roots.indexOf(cleanLabel)
                if (targetIdx >= 0) {
                    rootIdx = targetIdx
                    chordTypeIdx = when {
                        label.contains("m") -> 1
                        label.contains("dim") -> 5
                        label.contains("aug") -> 6
                        else -> 0
                    }
                }
            }
        )
        Spacer(Modifier.height(20.dp))
    }

    if (isLabModeOpen) {
        ImmersiveKeyboardFretboardLabDialog(
            viewModel = viewModel,
            rootIdx = rootIdx,
            qualityIdx = chordTypeIdx,
            instrumentType = selectedInstrument,
            accentColor = partColor,
            onDismiss = { isLabModeOpen = false }
        )
    }
}

@Composable
fun InstrumentSelectionRow(
    selectedInstrument: ChordInstrumentType,
    onInstrumentChange: (ChordInstrumentType) -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChordInstrumentType.entries.forEach { inst ->
            val isSelected = inst == selectedInstrument
            Box(
                modifier = Modifier
                    .weight(1f) // Distributes width evenly across items
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onInstrumentChange(inst) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = inst.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PianoFingerLegend(
    accentColor: Color,
    fingering: FingeringData,
    noteNames: List<String>
) {
    val fingerNameLabels = listOf("Thumb", "Index", "Middle", "Ring", "Pinky")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        fingering.fingers.forEachIndexed { idx, fingerNum ->
            val noteLabel = noteNames.getOrNull(idx) ?: ""
            val structuralLabelName = fingerNameLabels.getOrNull(fingerNum - 1) ?: "Finger"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$fingerNum", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center, lineHeight = 10.sp,
                        style = TextStyle(lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both))
                    )
                }
                Text(
                    text = if (noteLabel.isNotBlank()) "$noteLabel ($structuralLabelName)" else structuralLabelName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GuitarFingerLegend(accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("1" to "Index", "2" to "Middle", "3" to "Ring", "4" to "Pinky").forEach { (n, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor), contentAlignment = Alignment.Center) {
                    Text(
                        text = n, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center, lineHeight = 11.sp,
                        style = TextStyle(lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both))
                    )
                }
                Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CapoSection(
    capoFret: Int,
    activeRootIdx: Int,
    onCapoChange: (Int) -> Unit,
    partColor: Color,
    onKeyDispatched: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🎸 DYNAMIC CAPO CALCULATOR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = partColor, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(8.dp), color = if (capoFret > 0) partColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceContainerLow) {
                Text("Fret $capoFret", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (capoFret > 0) partColor else MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        }

        Slider(value = capoFret.toFloat(), onValueChange = { onCapoChange(it.toInt()) }, valueRange = 0f..12f, steps = 11, colors = SliderDefaults.colors(thumbColor = partColor, activeTrackColor = partColor))

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SHAPE YOU GRAB", fontSize = 10.sp, fontWeight = FontWeight.Black, color = partColor.copy(0.6f))
                    Text("ACTUAL SOUNDING KEY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = partColor.copy(0.6f))
                }

                NOTE_NAMES_CD.forEachIndexed { shapeIdx, shapeName ->
                    val actualSoundingSemitone = (shapeIdx + capoFret) % 12
                    val isSoundingActiveRoot = actualSoundingSemitone == activeRootIdx

                    Surface(
                        onClick = { onKeyDispatched(actualSoundingSemitone) },
                        color = if (isSoundingActiveRoot) partColor.copy(alpha = 0.08f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = if (isSoundingActiveRoot) BorderStroke(1.dp, partColor.copy(0.4f)) else null
                    ) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("$shapeName Shape", fontSize = 13.sp, fontWeight = if (isSoundingActiveRoot) FontWeight.Black else FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("→", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                            Text(
                                text = NOTE_NAMES_CD[actualSoundingSemitone],
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSoundingActiveRoot) partColor else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransposeSection(
    fromKeyIdx: Int,
    toKeyIdx: Int,
    selectedScaleIdx: Int,
    scaleFormulas: List<Pair<String, List<Int>>>,
    partColor: Color,
    onFromChange: (Int) -> Unit,
    onToChange: (Int) -> Unit,
    onKeyDispatched: (Int) -> Unit
) {
    val semitoneShift = ((toKeyIdx - fromKeyIdx) + 12) % 12
    val shiftLabel = if (semitoneShift == 0) "Same key" else "+$semitoneShift Semitones"
    val scope = rememberCoroutineScope()

    val songIdeas = remember {
        listOf(
            "Epic/Heroic Hook (I - V - vi - IV)" to listOf(0, 4, 5, 3),
            "Moody Sad/Emotional (vi - IV - I - V)" to listOf(5, 3, 0, 4),
            "Dark Suspense/Tension (i - bVI - bVII - v)" to listOf(0, 5, 6, 4),
            "Neo-Soul/Jazz Fluidity (ii - V - I - IV)" to listOf(1, 4, 0, 3)
        )
    }

    fun getRomanNumeral(stepIdx: Int, isMinor: Boolean, isDiminished: Boolean): String {
        val baseNumeral = when (stepIdx) { 0 -> "I"; 1 -> "II"; 2 -> "III"; 3 -> "IV"; 4 -> "V"; 5 -> "VI"; 6 -> "VII"; else -> "?" }
        return when {
            isDiminished -> baseNumeral.lowercase() + "°"
            isMinor -> baseNumeral.lowercase()
            else -> baseNumeral
        }
    }

    val activeScaleSteps = scaleFormulas.getOrNull(selectedScaleIdx)?.second ?: listOf(0, 2, 4, 5, 7, 9, 11)
    val activeScaleName = scaleFormulas.getOrNull(selectedScaleIdx)?.first?.split(" ")?.firstOrNull() ?: "Scale"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("✨ SONGWRITING IDEAS ($activeScaleName)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = partColor, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(8.dp), color = partColor.copy(0.12f)) {
                Text(shiftLabel, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Black, color = partColor)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CdLabel("CREATIVE SEED KEY (FROM)")
            ScrollableRow {
                NOTE_NAMES_CD.forEachIndexed { i, name ->
                    val sel = i == fromKeyIdx
                    Surface(onClick = { onFromChange(i) }, shape = RoundedCornerShape(8.dp), color = if (sel) partColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.padding(end = 4.dp)) {
                        Text(name, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Normal, color = if (sel) partColor else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CdLabel("TARGET ARRANGEMENT KEY (TO)")
            ScrollableRow {
                NOTE_NAMES_CD.forEachIndexed { i, name ->
                    val sel = i == toKeyIdx
                    Surface(onClick = { onToChange(i); onKeyDispatched(i) }, shape = RoundedCornerShape(8.dp), color = if (sel) partColor else MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.padding(end = 4.dp)) {
                        Text(name, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Normal, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                songIdeas.forEach { (vibeTitle, stepsList) ->
                    Column(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(vibeTitle, fontSize = 12.sp, fontWeight = FontWeight.Black, color = partColor)
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        stepsList.forEach { stepIdx ->
                                            val intervalOffset = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                            val targetMidi = 60 + toKeyIdx + intervalOffset
                                            try { getAudioEngine().noteOn(targetMidi, 0.95f) } catch (_: Exception) {}
                                            delay(600)
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp).background(partColor.copy(0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = partColor)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                            stepsList.forEach { stepIdx ->
                                val interval = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                val nextThird = activeScaleSteps.find { it == (interval + 3) % 12 || it == (interval + 4) % 12 } ?: (interval + 4)
                                val nextFifth = activeScaleSteps.find { it == (interval + 7) % 12 || it == (interval + 6) % 12 } ?: (interval + 7)
                                val isMinor = ((nextThird - interval + 12) % 12) == 3
                                val isDim = ((nextFifth - interval + 12) % 12) == 6
                                Text(text = getRomanNumeral(stepIdx, isMinor, isDim), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${NOTE_NAMES_CD[fromKeyIdx]}:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.width(32.dp))
                            stepsList.forEach { stepIdx ->
                                val intervalOffset = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                val nextThird = activeScaleSteps.find { it == (intervalOffset + 3) % 12 || it == (intervalOffset + 4) % 12 } ?: (intervalOffset + 4)
                                val isMinor = ((nextThird - intervalOffset + 12) % 12) == 3
                                val chordName = NOTE_NAMES_CD[(fromKeyIdx + intervalOffset) % 12] + (if (isMinor) "m" else "")
                                Text(chordName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.7f), modifier = Modifier.padding(end = 8.dp))
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${NOTE_NAMES_CD[toKeyIdx]}:", fontSize = 11.sp, color = partColor, modifier = Modifier.width(32.dp))
                            stepsList.forEach { stepIdx ->
                                val intervalOffset = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                val nextThird = activeScaleSteps.find { it == (intervalOffset + 3) % 12 || it == (intervalOffset + 4) % 12 } ?: (intervalOffset + 4)
                                val isMinor = ((nextThird - intervalOffset + 12) % 12) == 3
                                val chordName = NOTE_NAMES_CD[(toKeyIdx + intervalOffset) % 12] + (if (isMinor) "m" else "")
                                Text(chordName, fontSize = 12.sp, fontWeight = FontWeight.Black, color = partColor, modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EasyChordQuickRef(
    rootIdx: Int,
    qualityIdx: Int,
    cadences: List<Pair<String, List<Int>>>,
    accentColor: Color,
    onSelectChord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "QUICK REFERENCE & SUBSTITUTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                letterSpacing = 1.sp
            )

            ScrollableRow {
                cadences.forEach { (name, notes) ->
                    Surface(
                        onClick = { onSelectChord(name) },
                        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(name, fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                notes.joinToString(" - ") { NOTE_NAMES_CD[it % 12] },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircleOfFifthsVisualizer(
    selectedRootIdx: Int,
    onSelectRoot: (Int) -> Unit
) {
    val circleKeys = listOf("C", "G", "D", "A", "E", "B", "F♯", "D♭", "A♭", "E♭", "B♭", "F")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E28))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Circle of Fifths Harmonic Wheel",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(circleKeys) { index, key ->
                    val isSelected = index == selectedRootIdx
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectRoot(index) },
                        label = {
                            Text(key, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
            ) {
                val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
                val radius = constraints.maxWidth / 2.6f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0..11) {
                        val angle = (i * 30.0 - 90.0) * Math.PI / 180.0
                        val x = center.x + radius * kotlin.math.cos(angle).toFloat()
                        val y = center.y + radius * kotlin.math.sin(angle).toFloat()
                        val isSelected = i == selectedRootIdx

                        drawCircle(
                            color = if (isSelected) Color(0xFFFFB347) else Color.Gray.copy(alpha = 0.2f),
                            radius = 32f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveKeyboardFretboardLabDialog(
    viewModel: AppViewModel,
    rootIdx: Int,
    qualityIdx: Int,
    instrumentType: ChordInstrumentType,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    DisposableEffect(Unit) {
        viewModel.isImmersiveLabActive.value = true
        setScreenOrientation(pEQOrientation.LANDSCAPE)
        setSystemBarsVisibility(false)
        onDispose {
            viewModel.isImmersiveLabActive.value = false
            setScreenOrientation(pEQOrientation.USER)
            setSystemBarsVisibility(true)
        }
    }
    var activeLabTab by remember { mutableStateOf(if (instrumentType == ChordInstrumentType.PIANO) "Piano" else if (instrumentType == ChordInstrumentType.UKULELE) "Uke" else "Guitar") }
    var selectedSpeed by remember { mutableStateOf("1.0x") }
    var playbackMode by remember { mutableStateOf("Arpeggio") }
    var isPlaying by remember { mutableStateOf(false) }

    var selectedMode by remember { mutableStateOf(0) } // 0 = Chords, 1 = Scales
    var labRootIdx by remember { mutableStateOf(rootIdx) }
    var labSuffixIdx by remember { mutableStateOf(qualityIdx) }
    var labInversion by remember { mutableStateOf("Root Position") }

    // Left sidebar controls
    var guitarConfigStr by remember { mutableStateOf("6 Str") }
    var tuningOption by remember { mutableStateOf("Standard") }
    var capoFretSelected by remember { mutableStateOf(0) } // 0 = Open, 1..5
    var fretNeckZoom by remember { mutableStateOf("12f") }
    var isLeftHanded by remember { mutableStateOf(false) }
    var showInlayDots by remember { mutableStateOf(true) }
    var labelOverlay by remember { mutableStateOf("Notes") } // Notes, Intervals, Fingers
    var noteNamingSystem by remember { mutableStateOf("Western (C, D, E)") }
    var synthSound by remember { mutableStateOf("Acoustic Piano") }

    val roots = NOTE_NAMES_CD
    val currentRootName = roots[labRootIdx.coerceIn(0, roots.size - 1)]

    val chordSuffixes = listOf("Major", "Minor", "7th", "Major 7th", "Minor 7th", "Diminished", "Augmented", "Sus4", "Add9")
    val currentSuffixName = chordSuffixes.getOrElse(labSuffixIdx) { "Major" }
    val inversions = listOf("Root Position", "1st Inversion", "2nd Inversion")

    val activeChordPitches = remember(labRootIdx, labSuffixIdx, labInversion) {
        val rootMidi = 60 + labRootIdx
        val intervals = when (labSuffixIdx) {
            1 -> listOf(0, 3, 7)
            2 -> listOf(0, 4, 7, 10)
            3 -> listOf(0, 4, 7, 11)
            4 -> listOf(0, 3, 7, 10)
            5 -> listOf(0, 3, 6)
            6 -> listOf(0, 4, 8)
            7 -> listOf(0, 5, 7)
            8 -> listOf(0, 4, 7, 14)
            else -> listOf(0, 4, 7) // Major
        }
        val baseNotes = intervals.map { rootMidi + it }
        when (labInversion) {
            "1st Inversion" -> if (baseNotes.size > 1) baseNotes.drop(1) + (baseNotes.first() + 12) else baseNotes
            "2nd Inversion" -> if (baseNotes.size > 2) baseNotes.drop(2) + baseNotes.take(2).map { it + 12 } else baseNotes
            else -> baseNotes
        }
    }

    val activeMidiMap = remember(activeChordPitches) {
        activeChordPitches.associateWith { true }
    }

    val orangeColor = Color(0xFFFF9E1B)
    val darkCardBg = Color(0xFF13151B)
    val sidebarBg = Color(0xFF0F1015)
    val rosewoodBrush = Brush.verticalGradient(listOf(Color(0xFF4A342E), Color(0xFF382723), Color(0xFF291C19)))

    // Arpeggio / Strum playback loop
    var currentAuditionIdx by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(isPlaying, selectedSpeed, playbackMode, activeChordPitches) {
        if (!isPlaying || activeChordPitches.isEmpty()) {
            currentAuditionIdx = null
            return@LaunchedEffect
        }
        val speedMult = when (selectedSpeed) {
            "0.5x" -> 0.5f
            "1.5x" -> 1.5f
            "2.0x" -> 2.0f
            else -> 1.0f
        }
        val stepDelay = ((if (playbackMode == "Arpeggio") 400L else 700L) / speedMult).toLong()

        while (isPlaying) {
            if (playbackMode == "Arpeggio") {
                for (idx in activeChordPitches.indices) {
                    if (!isPlaying) break
                    currentAuditionIdx = idx
                    val midi = activeChordPitches[idx]
                    try {
                        val inst = if (activeLabTab == "Guitar" || activeLabTab == "Uke") "GUITAR" else "PIANO"
                        viewModel.audioEngine.setInstrument(inst)
                        viewModel.audioEngine.noteOn(midi, 0.85f)
                    } catch (_: Exception) {}
                    delay(stepDelay)
                }
            } else {
                currentAuditionIdx = null
                activeChordPitches.forEach { midi ->
                    try {
                        val inst = if (activeLabTab == "Guitar" || activeLabTab == "Uke") "GUITAR" else "PIANO"
                        viewModel.audioEngine.setInstrument(inst)
                        viewModel.audioEngine.noteOn(midi, 0.85f)
                    } catch (_: Exception) {}
                }
                delay(stepDelay)
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0B0E))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. LEFT SIDEBAR (Dark control panel matching screenshots)
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(sidebarBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Visualizer Lab",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = orangeColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )

                    // Active Visualizer Buttons: [Piano] [Guitar] [Uke] [Split]
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Active Visualizer", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Piano", "Guitar", "Uke", "Split").forEach { tab ->
                                val isSelected = activeLabTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) orangeColor else Color(0xFF1E2129))
                                        .clickable { activeLabTab = tab },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Visualizer specific configs:
                    if (activeLabTab == "Guitar" || activeLabTab == "Split") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Guitar Config", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("6 Str", "7 Str").forEach { strConf ->
                                    val isSel = guitarConfigStr == strConf
                                    Box(
                                        modifier = Modifier
                                            .width(54.dp)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF3B4054) else Color(0xFF1E2129))
                                            .border(1.dp, if (isSel) Color(0xFF7580A5) else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable { guitarConfigStr = strConf },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(strConf, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        // Tuning Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tuning", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF161820))
                                    .border(1.dp, Color(0xFF282D3A), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(tuningOption, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Capo Fret: [Open] [1] [2] [3] [4] [5]
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Capo Fret", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                (0..3).forEach { fret ->
                                    val label = if (fret == 0) "Open" else "$fret"
                                    val isSel = capoFretSelected == fret
                                    Box(
                                        modifier = Modifier
                                            .weight(if (fret == 0) 1.4f else 1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) orangeColor else Color(0xFF1E2129))
                                            .clickable { capoFretSelected = fret },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                (4..5).forEach { fret ->
                                    val isSel = capoFretSelected == fret
                                    Box(
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) orangeColor else Color(0xFF1E2129))
                                            .clickable { capoFretSelected = fret },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$fret", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                    }
                                }
                            }
                        }

                        // Fret Neck Zoom: [5f] [8f] [12f] [15f]
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Fret Neck Zoom", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("5f", "8f", "12f", "15f").forEach { z ->
                                    val isSel = fretNeckZoom == z
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF3B4054) else Color(0xFF1E2129))
                                            .border(1.dp, if (isSel) Color(0xFF7580A5) else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable { fretNeckZoom = z },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(z, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    } else if (activeLabTab == "Uke") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Ukulele Tuning", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF161820))
                                    .border(1.dp, Color(0xFF282D3A), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Standard C", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Toggles: Left-Handed, Inlay Dots
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fretboard View", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        if (activeLabTab != "Piano") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Left-Handed", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = isLeftHanded,
                                    onCheckedChange = { isLeftHanded = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = orangeColor)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Inlay Dots", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = showInlayDots,
                                onCheckedChange = { showInlayDots = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = orangeColor)
                            )
                        }
                    }

                    // Label Overlay: [Notes] [Intervals] [Fingers]
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Label Overlay", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Notes", "Intervals").forEach { l ->
                                val isSel = labelOverlay == l
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF3B4054) else Color(0xFF1E2129))
                                        .border(1.dp, if (isSel) Color(0xFF7580A5) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { labelOverlay = l },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(l, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (labelOverlay == "Fingers") Color(0xFF3B4054) else Color(0xFF1E2129))
                                .border(1.dp, if (labelOverlay == "Fingers") Color(0xFF7580A5) else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { labelOverlay = "Fingers" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Fingers", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Note Naming System Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Note Naming System", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161820))
                                .border(1.dp, Color(0xFF282D3A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(noteNamingSystem, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Sound Generator
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sound Generator", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161820))
                                .border(1.dp, Color(0xFF282D3A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(synthSound, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // 2. MAIN WORKSTATION AREA (Top bar, Instrument Canvas, Bottom Configuration Bar)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // TOP BAR: [Back Arrow] Title + Subtitle | [Speed buttons] [Arpeggio] [Play Button]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text(
                                    text = "$currentRootName Workstation",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Speed: Normal ($selectedSpeed) | Playback: $playbackMode",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Speeds: 0.5x, 1.0x, 1.5x, 2.0x
                            listOf("0.5x", "1.0x", "1.5x", "2.0x").forEach { spd ->
                                val isSel = selectedSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) orangeColor else Color(0xFF1E2129))
                                        .clickable { selectedSpeed = spd },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(spd, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                }
                            }

                            // Playback Mode Chip: Arpeggio / Strum
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .padding(horizontal = 10.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2129))
                                    .clickable {
                                        playbackMode = if (playbackMode == "Arpeggio") "Strum" else "Arpeggio"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(playbackMode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            // Play Button (Orange circle with Play icon)
                            Button(
                                onClick = { isPlaying = !isPlaying },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color(0xFFD32F2F) else orangeColor),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isPlaying) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "Stop" else "Play",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPlaying) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    // CENTER: INSTRUMENT CANVAS (Keyboard, Fretboard, or Split view)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (activeLabTab) {
                            "Piano" -> {
                                UnifiedPianoKeyboard(
                                    activeMidiNotes = activeMidiMap,
                                    accentColor = orangeColor,
                                    startNote = 48,
                                    endNote = 84,
                                    showNoteNames = true,
                                    isPlaying = isPlaying,
                                    onKeyTapped = { midi ->
                                        try {
                                            viewModel.audioEngine.setInstrument("PIANO")
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            "Guitar" -> {
                                val numF = when (fretNeckZoom) {
                                    "5f" -> 5
                                    "8f" -> 8
                                    "15f" -> 15
                                    else -> 12
                                }
                                UnifiedGuitarFretboard(
                                    numFrets = numF,
                                    activeNotes = activeChordPitches,
                                    rootNote = labRootIdx,
                                    accentColor = orangeColor,
                                    showNoteNames = true,
                                    baseFret = if (capoFretSelected > 0) capoFretSelected else 1,
                                    fretboardBrush = rosewoodBrush,
                                    showInlayDots = showInlayDots,
                                    onFretTapped = { _, _, midi ->
                                        try {
                                            viewModel.audioEngine.setInstrument("GUITAR")
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            "Uke" -> {
                                UnifiedGuitarFretboard(
                                    numFrets = 7,
                                    activeNotes = activeChordPitches,
                                    rootNote = labRootIdx,
                                    accentColor = orangeColor,
                                    showNoteNames = true,
                                    stringLabels = listOf("A", "E", "C", "G"),
                                    openStringsSemis = listOf(9, 4, 0, 7),
                                    fretboardBrush = rosewoodBrush,
                                    showInlayDots = showInlayDots,
                                    onFretTapped = { _, _, midi ->
                                        try {
                                            viewModel.audioEngine.setInstrument("GUITAR")
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            else -> {
                                // Split View (Piano on top, Guitar on bottom)
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    UnifiedPianoKeyboard(
                                        activeMidiNotes = activeMidiMap,
                                        accentColor = orangeColor,
                                        startNote = 48,
                                        endNote = 84,
                                        showNoteNames = true,
                                        isPlaying = isPlaying,
                                        onKeyTapped = { midi ->
                                            try {
                                                viewModel.audioEngine.setInstrument("PIANO")
                                                viewModel.audioEngine.noteOn(midi, 0.85f)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    UnifiedGuitarFretboard(
                                        numFrets = 12,
                                        activeNotes = activeChordPitches,
                                        rootNote = labRootIdx,
                                        accentColor = orangeColor,
                                        showNoteNames = true,
                                        fretboardBrush = rosewoodBrush,
                                        showInlayDots = showInlayDots,
                                        onFretTapped = { _, _, midi ->
                                            try {
                                                viewModel.audioEngine.setInstrument("GUITAR")
                                                viewModel.audioEngine.noteOn(midi, 0.85f)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }

                    // BOTTOM BAR: Mode [Chords] [Scales] | Root Dropdown | Chord Suffix Dropdown | Inversion Dropdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1117), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E212B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mode: [Chords] [Scales]
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Mode:", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1B1E28))
                                    .padding(2.dp)
                            ) {
                                listOf("Chords", "Scales").forEachIndexed { idx, m ->
                                    val isSel = selectedMode == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF383E54) else Color.Transparent)
                                            .clickable { selectedMode = idx }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(m, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else Color.White.copy(0.6f))
                                    }
                                }
                            }
                        }

                        // Root Dropdown
                        var rootExpanded by remember { mutableStateOf(false) }
                        Box {
                            Column(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13161F))
                                    .border(1.dp, Color(0xFF282E40), RoundedCornerShape(8.dp))
                                    .clickable { rootExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Root", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentRootName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = rootExpanded, onDismissRequest = { rootExpanded = false }) {
                                roots.forEachIndexed { idx, r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = { labRootIdx = idx; rootExpanded = false }
                                    )
                                }
                            }
                        }

                        // Chord Suffix Dropdown
                        var suffixExpanded by remember { mutableStateOf(false) }
                        Box {
                            Column(
                                modifier = Modifier
                                    .width(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13161F))
                                    .border(1.dp, Color(0xFF282E40), RoundedCornerShape(8.dp))
                                    .clickable { suffixExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Chord Suffix", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentSuffixName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = suffixExpanded, onDismissRequest = { suffixExpanded = false }) {
                                chordSuffixes.forEachIndexed { idx, s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = { labSuffixIdx = idx; suffixExpanded = false }
                                    )
                                }
                            }
                        }

                        // Inversion Dropdown
                        var inversionExpanded by remember { mutableStateOf(false) }
                        Box {
                            Column(
                                modifier = Modifier
                                    .width(105.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13161F))
                                    .border(1.dp, Color(0xFF282E40), RoundedCornerShape(8.dp))
                                    .clickable { inversionExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Inversion", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(labInversion, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = inversionExpanded, onDismissRequest = { inversionExpanded = false }) {
                                inversions.forEach { inv ->
                                    DropdownMenuItem(
                                        text = { Text(inv) },
                                        onClick = { labInversion = inv; inversionExpanded = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3DropdownMenu(
    label: String,
    selectedText: String,
    items: List<String>,
    onItemSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item, fontSize = 12.sp) },
                    onClick = {
                        onItemSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CdLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        ),
        modifier = modifier
    )
}

@Composable
private fun ScrollableRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}