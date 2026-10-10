package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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

enum class ChordInstrumentType(val label: String) {
    GUITAR("Guitar (6-Str)"),
    PIANO("Piano (88-Key)"),
    UKULELE("Ukulele (4-Str)")
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
            getAudioEngine().setInstrument(selectedInstrument.name)
        } catch (e: Exception) {
            println("pEQControl: [INSTRUMENT-SWITCH] Failed to sync audio engine pointer: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (selectedInstrument == ChordInstrumentType.PIANO) Icons.Default.Piano else Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = partColor
                    )
                    Text(
                        text = if (mode == 0) "CHORD DICTIONARY - ${typeDef.label.uppercase()}" else "SCALE EXPLORER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = partColor,
                        letterSpacing = 1.5.sp
                    )
                }
                Text(
                    text = chordName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = { isLabModeOpen = true },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = partColor.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = partColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Immersive Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = partColor)
                }

                IconButton(onClick = { showCircleOfFifths = !showCircleOfFifths }) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = "Circle of Fifths",
                        tint = if (showCircleOfFifths) partColor else Color.White.copy(alpha = 0.7f)
                    )
                }
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

        InstrumentSelectionRow(
            selectedInstrument = selectedInstrument,
            onInstrumentChange = { selectedInstrument = it }
        )

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

        Surface(shape = RoundedCornerShape(14.dp), color = partColor.copy(0.08f)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
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
                        Modifier.padding(top = 4.dp),
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
                                    .padding(horizontal = if (isActiveStep) 4.dp else 0.dp, vertical = 1.dp)
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
                            pianoPitches.forEachIndexed { idx: Int, midiPitch: Int ->
                                currentPlayingIdx = idx
                                viewModel.scorePlayheadMeasure.value = 0
                                viewModel.scorePlayheadOffset.value = idx.toFloat() / pianoPitches.size.toFloat()

                                viewModel.midiPlayingNote.value = midiPitch
                                viewModel.midiActiveNotes.clear()
                                viewModel.midiActiveNotes[midiPitch] = true

                                try {
                                    getAudioEngine().playSample("PIANO_NOTE", midiPitch, "SYSTEM")
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
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(2.dp))
                            GuitarFingerLegend(accentColor = partColor)
                        }
                    }
                    ChordInstrumentType.PIANO -> {
                        val startNoteBoundary = (pianoPitches.minOrNull() ?: 60) - 2
                        val endNoteBoundary = (pianoPitches.maxOrNull() ?: 72) + (if (selectedOctaveSpan == 1) 24 else 12)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            UnifiedPianoKeyboard(
                                activeMidiNotes = activeMidiMap,
                                accentColor = partColor,
                                startNote = startNoteBoundary,
                                endNote = endNoteBoundary,
                                showNoteNames = true,
                                modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(8.dp))
                            )
                            PianoFingerLegend(accentColor = partColor, activeSuffix = typeDef.suffix)
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
            listOf("Staff Notation", "Tab Notation").forEach { fmt ->
                FilterChip(selected = unifiedFormat == fmt, onClick = { onFormatChange(fmt) }, label = { Text(fmt, fontSize = 11.sp) })
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
            modifier = Modifier.fillMaxWidth().height(220.dp),
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

        Button(
            onClick = {
                viewModel.practicingChord.value = if (mode == 0) chordName else "${roots[rootIdx]} Scale"
                onPracticeTapped()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = partColor)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Practice Workspace Target", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
        }
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
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChordInstrumentType.entries.forEach { inst ->
            val isSelected = inst == selectedInstrument
            FilterChip(
                selected = isSelected,
                onClick = { onInstrumentChange(inst) },
                label = { Text(inst.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PianoFingerLegend(accentColor: Color, activeSuffix: String) {
    val assignedFingersList = remember(activeSuffix) { PIANO_RH_FINGERS[activeSuffix] ?: listOf(1, 3, 5) }
    val fingerNameLabels = listOf("Thumb", "Index", "Middle", "Ring", "Pinky")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        assignedFingersList.forEach { fingerNum ->
            val structuralLabelName = fingerNameLabels.getOrNull(fingerNum - 1) ?: "Finger"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(accentColor), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$fingerNum", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center, lineHeight = 10.sp,
                        style = TextStyle(lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both))
                    )
                }
                Text(text = structuralLabelName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
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
        color = Color(0xFF141724),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "⚡ EASY CHORD QUICK REFERENCE & SUBSTITUTIONS",
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
                        color = Color(0xFF1E2235),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(name, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                            Text(
                                notes.joinToString(" - ") { NOTE_NAMES_CD[it % 12] },
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
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

@Composable
fun ImmersiveKeyboardFretboardLabDialog(
    viewModel: AppViewModel,
    rootIdx: Int,
    qualityIdx: Int,
    instrumentType: ChordInstrumentType,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    var playbackSpeedMultiplier by remember { mutableStateOf(1.0f) }
    var isArpeggioRunning by remember { mutableStateOf(false) }
    var currentPlayingIdx by remember { mutableStateOf(-1) }
    var cagedPosition by remember { mutableStateOf(0) }

    val roots = NOTE_NAMES_CD
    val rootName = roots[rootIdx % roots.size]
    val shape = remember(rootIdx, qualityIdx, cagedPosition) {
        ChordVoicingEngine.getGuitarVoicing(rootName, "maj", cagedPosition)
    }

    LaunchedEffect(isArpeggioRunning, playbackSpeedMultiplier) {
        if (isArpeggioRunning && shape.soundingMidiNotes.isNotEmpty()) {
            val notes = shape.soundingMidiNotes
            while (isArpeggioRunning) {
                for (i in notes.indices) {
                    currentPlayingIdx = i
                    val delayMs = (400L / playbackSpeedMultiplier).toLong().coerceAtLeast(100L)
                    delay(delayMs)
                }
            }
        } else {
            currentPlayingIdx = -1
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF10121A),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = accentColor, modifier = Modifier.padding(8.dp).size(24.dp))
                        }
                        Column {
                            Text(text = "IMMERSIVE INSTRUMENT LAB", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                            Text(text = "$rootName Interactive Fretboard & Keyboard Analysis", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF161922), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { isArpeggioRunning = !isArpeggioRunning },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (isArpeggioRunning) accentColor else Color(0xFF2A2D3D))
                        ) {
                            Icon(if (isArpeggioRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isArpeggioRunning) "Halt Arpeggio" else "Run Arpeggiator", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                            FilterChip(selected = playbackSpeedMultiplier == spd, onClick = { playbackSpeedMultiplier = spd }, label = { Text("${spd}x", fontSize = 11.sp) })
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0C0E14), RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Harmonic Staff & Tablature Projection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            StaffAndTabNotationViewer(
                                score = viewModel.currentScore.value as Score?,
                                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                                playheadOffset = viewModel.scorePlayheadOffset.value,
                                activeMidiNotes = viewModel.midiActiveNotes,
                                playingNote = viewModel.midiPlayingNote.value,
                                primaryColor = accentColor,
                                currentFormat = "Tab",
                                onFormatChange = {},
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text("88-Key Synchronized Keyboard Canvas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            val activeNotes = remember(shape, currentPlayingIdx) {
                                if (currentPlayingIdx in shape.soundingMidiNotes.indices) {
                                    setOf(shape.soundingMidiNotes[currentPlayingIdx])
                                } else {
                                    shape.soundingMidiNotes.toSet()
                                }
                            }
                            PianoKeyboard88Keys(viewModel = viewModel, activeMidiNotes = activeNotes, modifier = Modifier.fillMaxSize())
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