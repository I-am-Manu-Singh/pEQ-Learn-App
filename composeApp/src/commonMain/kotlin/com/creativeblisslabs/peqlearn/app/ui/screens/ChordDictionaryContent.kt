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
import androidx.compose.ui.window.Dialog
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.*
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector



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

val roots = remember { NOTE_NAMES_CD } // Uses "C#", "D#", "F#", "G#", "A#" so RealChordsDb lookups never fail!    val chordTypes = remember { CHORD_TYPE_DEFS.map { it.label } }

    var mode by remember { mutableStateOf(0) }
    var rootIdx by remember { mutableStateOf(0) }
    var chordTypeIdx by remember { mutableStateOf(0) }
    var selectedInstrument by remember { mutableStateOf(ChordInstrumentType.GUITAR_ACOUSTIC) }
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
    var selectedCagedFilter by remember { mutableStateOf("All CAGED") }
    var selectedFretRange by remember { mutableStateOf("All Frets") }

    val cagedOptions = remember { listOf("All CAGED", "C-Shape", "A-Shape", "G-Shape", "E-Shape", "D-Shape") }
    val fretRangeOptions = remember { listOf("All Frets", "Open (0-3)", "Low (1-5)", "Mid (5-9)", "High (9-14)", "12+ Frets") }

    val guitarVoicings = listOf("Open", "Barre", "Power", "Shell")
    val pianoVoicings = listOf("Closed", "Open", "Shell", "Drop 2")
    val ukeVoicings = listOf("Open", "Barre", "Closed")

    var showCircleOfFifths by remember { mutableStateOf(false) }
    var isLabModeOpen by remember { mutableStateOf(false) }

    val voicingOptions = remember { listOf("Root Position", "1st Inversion", "2nd Inversion", "Drop 2") }
    val octaveOptions = remember { listOf("2 Octaves", "3 Octaves") }
    val scope = rememberCoroutineScope()
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        val verificationLog = ChordVoicingEngine.verifyIntegrity()
        println("[CHORD_VOICING_ENGINE_VERIFY]\n$verificationLog")
    }

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

    var currentPlayingCoordinate by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var currentPlayingIdx by remember { mutableStateOf(-1) }
    var isArpeggioRunning by remember { mutableStateOf(false) }
    var activeSoundStyle by remember { mutableStateOf(InstrumentSoundStyle.SEQUENTIAL) }

    val voicedIntervals = remember(chordTypeIdx, selectedVoicingIdx, selectedIntervalIdx, selectedVoicingType) {
        val extraSemitone = if (selectedIntervalIdx > 0 && selectedIntervalIdx in INTERVALS_DATA.indices) {
            INTERVALS_DATA[selectedIntervalIdx][0].toIntOrNull()
                ?: INTERVALS_DATA[selectedIntervalIdx].getOrNull(4)?.toIntOrNull()
        } else null

        val inverted = ChordVoicingEngine.getVoicedIntervals(
            baseIntervals = typeDef.intervals,
            voicingIdx = selectedVoicingIdx,
            extraSemitone = extraSemitone
        )
        when (selectedVoicingType) {
            "Open" -> if (inverted.size >= 3) {
                // Spread middle voice up an octave for an open piano voicing
                inverted.mapIndexed { idx, semi -> if (idx == 1) semi + 12 else semi }.sorted()
            } else inverted
            "Shell" -> inverted.filterIndexed { idx, _ -> idx == 0 || idx == 1 || idx == inverted.lastIndex }
            "Drop 2" -> if (inverted.size >= 3) {
                val m = inverted.toMutableList()
                val dropIdx = m.size - 2
                m[dropIdx] = m[dropIdx] - 12
                m.sorted()
            } else inverted
            else -> inverted
        }
    }

    val activeMidiMap = remember(rootIdx, chordTypeIdx, selectedDiff, voicedIntervals, mode, selectedScaleIdx, selectedOctaveSpan, capoFret) {
        val intervals = if (mode == 0) {
            voicedIntervals
        } else {
            // Build multi-octave scale intervals (2 Octaves or 3 Octaves) ending cleanly on the top tonic root!
            val basePattern = scalePair.second
            val numOctaves = if (selectedOctaveSpan == 1) 2 else 1
            val multiOctave = mutableListOf<Int>()
            for (oct in 0 until numOctaves) {
                basePattern.forEach { semi -> multiOctave.add(semi + oct * 12) }
            }
            multiOctave.add(numOctaves * 12) // Top tonic resolution note
            multiOctave.distinct().sorted()
        }
        ChordVoicingEngine.getActiveMidiNotes(
            rootIdx = (rootIdx + (if (mode == 0) capoFret else 0)) % 12,
            intervals = intervals,
            baseMidi = 60
        )
    }

    val sortedMidi = remember(activeMidiMap) { activeMidiMap.keys.sorted() }
    val noteNames = remember(sortedMidi, mode) { sortedMidi.map { roots[(it % 12 + 12) % 12] } }
    val pianoPitches = sortedMidi

    val pianoFingering = remember(pianoPitches, typeDef.suffix, mode, selectedVoicingIdx) {
        ChordVoicingEngine.computePianoFingering(
            pitches = pianoPitches,
            chordSuffix = typeDef.suffix,
            isScale = (mode != 0),
            voicingIdx = selectedVoicingIdx
        )
    }

    val currentRootName = roots[rootIdx]
    val currentSuffix = typeDef.suffix

    val (resolvedFretMin, resolvedFretMax) = remember(selectedFretRange, selectedStartFret) {
        when (selectedFretRange) {
            "Open (0-3)" -> 0 to 4
            "Low (1-5)" -> 1 to 5
            "Mid (5-9)" -> 5 to 9
            "High (9-14)" -> 9 to 14
            "12+ Frets" -> 12 to 24
            else -> if (selectedStartFret > 1) selectedStartFret to (selectedStartFret + 4) else 0 to 24
        }
    }

    val guitarShape = remember(
        rootIdx, chordTypeIdx, selectedDiff, capoFret,
        selectedStartFret, selectedVoicingType, mode,
        selectedCagedFilter, selectedFretRange, resolvedFretMin, resolvedFretMax
    ) {
        ChordVoicingEngine.getDictionaryGuitarShape(
            rootName = currentRootName,
            suffix = currentSuffix,
            diff = selectedDiff,
            capoFret = capoFret,
            startFret = selectedStartFret,
            voicingType = selectedVoicingType,
            cagedFilter = if (selectedCagedFilter == "All CAGED") "AUTO" else selectedCagedFilter,
            fretRangeFilter = selectedFretRange,
            fretMinConstrain = resolvedFretMin,
            fretMaxConstrain = resolvedFretMax,
            fallbackIntervals = typeDef.intervals
        )
    }

    val ukuShape = remember(rootIdx, chordTypeIdx, selectedDiff, capoFret, selectedStartFret, selectedVoicingType, mode) {
        ChordVoicingEngine.getDictionaryUkuShape(
            rootName = currentRootName.replace("♯", "#"),
            suffix = currentSuffix,
            diff = selectedDiff,
            capoFret = capoFret,
            startFret = selectedStartFret,
            voicingType = selectedVoicingType,
            fallbackIntervals = typeDef.intervals
        )
    }

    val cadenceList = remember(rootIdx) {
        ChordVoicingEngine.getCadenceProgressions(rootIdx = rootIdx, rootNames = roots)
    }

    // Comprehensive selection debug logger for issue auditing
    LaunchedEffect(
        mode,
        rootIdx,
        chordTypeIdx,
        selectedScaleIdx,
        selectedInstrument,
        selectedDiff,
        selectedVoicingType,
        selectedVoicingIdx,
        selectedStartFret,
        selectedCagedFilter,
        selectedFretRange,
        selectedOctaveSpan
    ) {
        val rootStr = roots.getOrNull(rootIdx) ?: "C"
        val modeStr = if (mode == 0) "CHORD" else "SCALE"
        val itemStr = if (mode == 0) chordName else scalePair.first
        val instStr = selectedInstrument.name
        val diffStr = selectedDiff.label
        val rangeStr = selectedFretRange
        val cagedStr = selectedCagedFilter
        val startFretStr = "Fret $selectedStartFret"
        val voicingStr = if (selectedInstrument == ChordInstrumentType.PIANO_ACOUSTIC) {
            voicingOptions.getOrNull(selectedVoicingIdx) ?: "Default"
        } else {
            selectedVoicingType
        }
        val octaveStr = octaveOptions.getOrNull(selectedOctaveSpan) ?: "Default"

        println(
            "pEQLearn [StudioSelection]: Mode=$modeStr | Root=$rootStr | Selection=\"$itemStr\" | Inst=$instStr | " +
                "Voicing=$voicingStr | FretDiff=$diffStr | StartFret=$startFretStr | CAGED=$cagedStr | FretRange=$rangeStr | Octaves=$octaveStr | Pitches=$pianoPitches"
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
            ChordInstrumentType.PIANO_ACOUSTIC, ChordInstrumentType.HARMONIUM -> "Closed"
            ChordInstrumentType.UKULELE -> "Open"
            else -> "Open"
        }
        try {
            getAudioEngine().setInstrument(selectedInstrument.soundEngineName)
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
     
    var isChordGalleryOpen by remember { mutableStateOf(false) }

    if (isChordGalleryOpen) {
        com.creativeblisslabs.peqlearn.app.ui.components.FullscreenDialog(
            onDismissRequest = { isChordGalleryOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ChordDisplayGalleryScreen(
                viewModel = viewModel,
                initialRoot = roots[rootIdx],
                initialSuffix = typeDef.suffix,
                initialInstrument = if (selectedInstrument == ChordInstrumentType.UKULELE) "ukulele" else "guitar",
                onBack = { isChordGalleryOpen = false }
            )
        }
    }

Row(
    modifier = Modifier.fillMaxWidth(), // Ensures the top row fills available bounds
    horizontalArrangement = Arrangement.spacedBy(8.dp), 
    verticalAlignment = Alignment.CenterVertically
) {
    // 1. Instrument Dropdown Switcher
    Box(modifier = Modifier.weight(1.3f)) {
        pEQInstrumentDropdown(
            selectedInstrument = selectedInstrument,
            accentColor = partColor,
            onInstrumentChange = { selectedInstrument = it }
        )
    }

     SingleChoiceSegmentedButtonRow( modifier = Modifier.height(40.dp)) {
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
    // 3. Lab Toggle Button
    FilledTonalButton(
        onClick = { isLabModeOpen = true },
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = partColor.copy(alpha = 0.2f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Icon(Icons.Default.Science, contentDescription = null, tint = partColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = partColor)
    }


}
              // Row 2: Secondary Configuration Dropdowns (Dynamically tailored to Instrument & Mode)

        var scaleDirectionMode by remember { mutableStateOf("Up & Down") } // "Ascending", "Descending", "Up & Down"
            var badgeDisplayMode by remember { mutableStateOf("Auto") }
           

        val isGuitarFamily = selectedInstrument in listOf(
            ChordInstrumentType.GUITAR_ACOUSTIC,
            ChordInstrumentType.GUITAR_ELECTRIC,
            ChordInstrumentType.GUITAR_CLASSICAL
        )
        val is7String = selectedInstrument == ChordInstrumentType.GUITAR_7
        val isUkulele = selectedInstrument == ChordInstrumentType.UKULELE
        val isFretboardInstrument = isGuitarFamily || is7String || isUkulele
        val isKeyboardFamily = selectedInstrument in listOf(
            ChordInstrumentType.PIANO_ACOUSTIC,
            ChordInstrumentType.PIANO_ELECTRIC,
            ChordInstrumentType.HARMONIUM
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ROW 1: Root Key | Chord Suffix (or Scale Formula) | Extension (Chord Mode) or Scale Direction (Scale Mode)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(0.95f)) {
                    M3DropdownMenu(
                        label = if (selectedInstrument in listOf(ChordInstrumentType.SITAR, ChordInstrumentType.FLUTE, ChordInstrumentType.HARMONIUM, ChordInstrumentType.TABLA)) "Sa / Root" else "Root Key",
                        selectedText = roots[rootIdx],
                        items = roots,
                        onItemSelect = { playbackJob?.cancel(); rootIdx = it }
                    )
                }
                Box(modifier = Modifier.weight(1.55f)) {
                    if (mode == 0) {
                        M3DropdownMenu(
                            label = "Chord Type",
                            selectedText = typeDef.label,
                            items = CHORD_TYPE_DEFS.map { it.label },
                            onItemSelect = { playbackJob?.cancel(); chordTypeIdx = it }
                        )
                    } else {
                        M3DropdownMenu(
                            label = "Scale / Thaat / Raga",
                            selectedText = scalePair.first,
                            items = scaleFormulas.map { it.first },
                            onItemSelect = { playbackJob?.cancel(); selectedScaleIdx = it }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1.2f)) {
                    if (mode == 0) {
                        val intervalItems = remember {
                            listOf("None") + INTERVALS_DATA.drop(1).map { "${it[1]} (+${it[0]}st)" }
                        }
                        val currentIntervalLabel = intervalItems.getOrElse(selectedIntervalIdx) { "None" }
                        M3DropdownMenu(
                            label = "Add Interval",
                            selectedText = currentIntervalLabel,
                            items = intervalItems,
                            onItemSelect = { playbackJob?.cancel(); selectedIntervalIdx = it }
                        )
                    } else {
                        val dirOptions = listOf("Ascending", "Descending", "Up & Down")
                        M3DropdownMenu(
                            label = "Play Direction",
                            selectedText = scaleDirectionMode,
                            items = dirOptions,
                            onItemSelect = { scaleDirectionMode = dirOptions[it] }
                        )
                    }
                }
            }

            // ROW 2: Instrument-Contextual Controls (Only shows options that actually apply to the active instrument!)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isKeyboardFamily) {
                    if (mode == 0) {
                        Box(modifier = Modifier.weight(1f)) {
                            M3DropdownMenu(
                                label = "Voicing Spread",
                                selectedText = selectedVoicingType,
                                items = pianoVoicings,
                                onItemSelect = { selectedVoicingType = pianoVoicings[it] }
                            )
                        }
                        Box(modifier = Modifier.weight(1.1f)) {
                            M3DropdownMenu(
                                label = "Inversion",
                                selectedText = voicingOptions[selectedVoicingIdx],
                                items = voicingOptions,
                                onItemSelect = { selectedVoicingIdx = it }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            M3DropdownMenu(
                                label = "Octave Span",
                                selectedText = octaveOptions[selectedOctaveSpan],
                                items = octaveOptions,
                                onItemSelect = { selectedOctaveSpan = it }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val badgeOpts = listOf("Auto", "Note Names", "Fingers (1-5)")
                        M3DropdownMenu(
                            label = "Key Badges",
                            selectedText = badgeDisplayMode,
                            items = badgeOpts,
                            onItemSelect = { badgeDisplayMode = badgeOpts[it] }
                        )
                    }
                } else if (isFretboardInstrument) {
                    if (mode == 0) {
                        val currentVoicingList = if (isUkulele) ukeVoicings else guitarVoicings
                        Box(modifier = Modifier.weight(1f)) {
                            M3DropdownMenu(
                                label = "Voicing Style",
                                selectedText = selectedVoicingType,
                                items = currentVoicingList,
                                onItemSelect = { selectedVoicingType = currentVoicingList[it] }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            M3DropdownMenu(
                                label = "Fret Difficulty",
                                selectedText = selectedDiff.label,
                                items = ChordDiff.entries.map { it.label },
                                onItemSelect = { selectedDiff = ChordDiff.entries[it] }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1.1f)) {
                            M3DropdownMenu(
                                label = "Neck Position Box",
                                selectedText = selectedFretRange,
                                items = fretRangeOptions,
                                onItemSelect = { selectedFretRange = fretRangeOptions[it] }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(0.95f)) {
                        M3DropdownMenu(
                            label = "Anchor Fret",
                            selectedText = "Fret $selectedStartFret",
                            items = (1..12).map { "Fret $it" },
                            onItemSelect = { selectedStartFret = it + 1 }
                        )
                    }
                } else {
                    // Sitar, Violin, Flute, Tabla
                    Box(modifier = Modifier.weight(1f)) {
                        M3DropdownMenu(
                            label = "Register Span",
                            selectedText = octaveOptions[selectedOctaveSpan],
                            items = octaveOptions,
                            onItemSelect = { selectedOctaveSpan = it }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        val styleOpts = listOf("Sequential", "Block Chord", "Roundtrip Arp")
                        val curStyleIdx = when (activeSoundStyle) {
                            InstrumentSoundStyle.SEQUENTIAL -> 0
                            InstrumentSoundStyle.ALL_AT_ONCE -> 1
                            InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> 2
                        }
                        M3DropdownMenu(
                            label = "Audition Style",
                            selectedText = styleOpts[curStyleIdx],
                            items = styleOpts,
                            onItemSelect = {
                                activeSoundStyle = when (it) {
                                    1 -> InstrumentSoundStyle.ALL_AT_ONCE
                                    2 -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                                    else -> InstrumentSoundStyle.SEQUENTIAL
                                }
                            }
                        )
                    }
                }
            }

            // ROW 3: Guitar / 7-String CAGED & Fret Position Filters
            if (isGuitarFamily || is7String) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        M3DropdownMenu(
                            label = if (mode == 0) "CAGED Chord Shape" else "CAGED Scale Box",
                            selectedText = selectedCagedFilter,
                            items = cagedOptions,
                            onItemSelect = {
                                selectedCagedFilter = cagedOptions[it]
                                // In scale mode, automatically sync neck position to the chosen CAGED box!
                                if (mode == 1) {
                                    selectedStartFret = when (cagedOptions[it]) {
                                        "C-Shape" -> 1
                                        "A-Shape" -> 3
                                        "G-Shape" -> 5
                                        "E-Shape" -> 8
                                        "D-Shape" -> 10
                                        else -> 1
                                    }
                                }
                            }
                        )
                    }
                    if (mode == 0) {
                        Box(modifier = Modifier.weight(1f)) {
                            M3DropdownMenu(
                                label = "Fret Zone Filter",
                                selectedText = selectedFretRange,
                                items = fretRangeOptions,
                                onItemSelect = { selectedFretRange = fretRangeOptions[it] }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(0.9f)) {
                        val badgeOpts = listOf("Auto", "Note Names", "Fingers (1-4)")
                        M3DropdownMenu(
                            label = "Fret Labels",
                            selectedText = badgeDisplayMode,
                            items = badgeOpts,
                            onItemSelect = { badgeDisplayMode = badgeOpts[it] }
                        )
                    }
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
                    val displayCapoText = if (capoFret > 0 && mode == 0 && selectedInstrument != ChordInstrumentType.PIANO_ACOUSTIC && selectedInstrument != ChordInstrumentType.PIANO_ELECTRIC) " (Capo $capoFret)" else ""
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
                    soundStyle = activeSoundStyle,
                    onSoundStyleToggle = {
                        activeSoundStyle = when (activeSoundStyle) {
                            InstrumentSoundStyle.SEQUENTIAL -> InstrumentSoundStyle.ALL_AT_ONCE
                            InstrumentSoundStyle.ALL_AT_ONCE -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                            InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> InstrumentSoundStyle.SEQUENTIAL
                        }
                    },
                    onPlayClick = {
                        if (isArpeggioRunning) {
                            playbackJob?.cancel()
                            pianoPitches.forEach { midi ->
                                try { viewModel.audioEngine.noteOff(midi) } catch (_: Exception) {}
                            }
                            currentPlayingIdx = -1
                            currentPlayingCoordinate = null
                            viewModel.midiPlayingNote.value = null
                            viewModel.currentScore.value = null
                            viewModel.midiActiveNotes.clear()
                            viewModel.midiActiveNotes.putAll(activeMidiMap)
                            isArpeggioRunning = false
                        } else {
                            playbackJob?.cancel()
                            isArpeggioRunning = true

                            // 1. Determine instrument-specific open strings and constraints
                            val isGuitarFamily = selectedInstrument in listOf(
                                ChordInstrumentType.GUITAR_ACOUSTIC,
                                ChordInstrumentType.GUITAR_ELECTRIC,
                                ChordInstrumentType.GUITAR_CLASSICAL
                            )
                            val isUkulele = selectedInstrument == ChordInstrumentType.UKULELE
                            val is7String = selectedInstrument == ChordInstrumentType.GUITAR_7

                            val openStrings = when {
                                isUkulele -> listOf(69, 64, 60, 67)
                                is7String -> listOf(64, 59, 55, 50, 45, 40, 35)
                                else -> listOf(64, 59, 55, 50, 45, 40)
                            }

                            val (fretMinBox, fretMaxBox) = when (selectedFretRange) {
                                "Open (0-3)" -> 0 to 3
                                "Low (1-5)" -> 1 to 5
                                "Mid (5-9)" -> 5 to 9
                                "High (9-14)" -> 9 to 14
                                "12+ Frets" -> 12 to 24
                                else -> {
                                    if (isGuitarFamily || is7String) {
                                        // Standard open/first position box for natural ergonomics
                                        0 to 4
                                    } else 0 to 15
                                }
                            }

                            // 2. Build Authentic, Strictly Monotonic Audition Steps (stringIndex, absoluteFret, midiPitch)
                            val auditionSteps: List<Triple<Int, Int, Int>> = if (mode == 0) {
                                // CHORD MODE: Strum from lowest-pitched string to highest-pitched string
                                if (isUkulele) {
                                    val baseOff = if (ukuShape.baseFret > 1 && ukuShape.frets.filter { it > 0 }.maxOrNull() ?: 0 <= 4) ukuShape.baseFret - 1 else 0
                                    listOf(3, 2, 1, 0).mapNotNull { s ->
                                        val f = ukuShape.frets.getOrNull(s) ?: -1
                                        if (f >= 0) {
                                            val absFret = if (f > 0) f + baseOff else 0
                                            Triple(s, absFret, openStrings[s] + absFret)
                                        } else null
                                    }
                                } else if (isGuitarFamily) {
                                    listOf(5, 4, 3, 2, 1, 0).mapNotNull { s ->
                                        val f = guitarShape.frets.getOrNull(s) ?: -1
                                        if (f >= 0) Triple(s, f, openStrings[s] + f) else null
                                    }
                                } else if (is7String) {
                                    listOf(6, 5, 4, 3, 2, 1, 0).mapNotNull { s ->
                                        val f = guitarShape.frets.getOrNull(s) ?: -1
                                        if (f >= 0) Triple(s, f, openStrings[s] + f) else null
                                    }
                                } else {
                                    pianoPitches.map { Triple(0, 0, it) }
                                }
                            } else {
                                // SCALE MODE: Strictly Ascending Monotonic CAGED Box Run (no duplicate pitches at string crossings!)
                                val scaleSemis = scalePair.second.map { (rootIdx + it) % 12 }.toSet()
                                if (isGuitarFamily || is7String) {
                                    val boxMin = if (fretMinBox == 0 && fretMaxBox > 5) selectedStartFret.coerceAtLeast(0) else fretMinBox
                                    val boxMax = (boxMin + 4).coerceAtMost(24)
                                    val stringScanOrder = if (is7String) listOf(6, 5, 4, 3, 2, 1, 0) else listOf(5, 4, 3, 2, 1, 0)
                                    val candidates = mutableListOf<Triple<Int, Int, Int>>()
                                    for (s in stringScanOrder) {
                                        for (f in boxMin..boxMax) {
                                            val m = openStrings[s] + f
                                            if ((m % 12) in scaleSemis) {
                                                candidates.add(Triple(s, f, m))
                                            }
                                        }
                                    }
                                    // Deduplicate by MIDI pitch and sort strictly ascending from the lowest Root note in the box!
                                    val uniqueSorted = candidates
                                        .groupBy { it.third }
                                        .map { (_, group) ->
                                            // Prefer fretted notes inside the hand box over open strings if higher up the neck
                                            group.minByOrNull { abs(it.second - (boxMin + 2)) } ?: group.first()
                                        }
                                        .sortedBy { it.third }

                                    val firstRootPos = uniqueSorted.indexOfFirst { (it.third % 12) == (rootIdx % 12) }
                                    val fromRoot = if (firstRootPos >= 0 && (uniqueSorted.size - firstRootPos) >= 6) {
                                        uniqueSorted.subList(firstRootPos, uniqueSorted.size)
                                    } else uniqueSorted
                                    val maxNotes = if (selectedOctaveSpan == 1) 15 else 8
                                    fromRoot.take(maxNotes)
                                } else if (isUkulele) {
                                    // On Ukulele (re-entrant G4), sort by pitch across C4(2), E4(1), A4(0) for a true ascending scale
                                    val candidates = mutableListOf<Triple<Int, Int, Int>>()
                                    for (s in listOf(2, 1, 0)) {
                                        for (f in 0..7) {
                                            val m = openStrings[s] + f
                                            if ((m % 12) in scaleSemis) {
                                                candidates.add(Triple(s, f, m))
                                            }
                                        }
                                    }
                                    val uniqueSorted = candidates.distinctBy { it.third }.sortedBy { it.third }
                                    val firstRootPos = uniqueSorted.indexOfFirst { (it.third % 12) == (rootIdx % 12) }
                                    val fromRoot = if (firstRootPos >= 0) uniqueSorted.subList(firstRootPos, uniqueSorted.size) else uniqueSorted
                                    fromRoot.take(8)
                                } else {
                                    pianoPitches.map { Triple(0, 0, it) }
                                }
                            }

                            val soundSequencePitches: List<Int> = if (auditionSteps.isNotEmpty()) {
                                when (activeSoundStyle) {
                                    InstrumentSoundStyle.SEQUENTIAL -> auditionSteps.map { it.third }
                                    InstrumentSoundStyle.ALL_AT_ONCE -> auditionSteps.map { it.third }
                                    InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> {
                                        val p = auditionSteps.map { it.third }
                                        if (p.size > 2) p + p.subList(0, p.size - 1).reversed()
                                        else if (p.size == 2) listOf(p[0], p[1], p[0])
                                        else p
                                    }
                                }
                            } else {
                                pianoPitches
                            }

                            val mockNotesList = soundSequencePitches.mapIndexed { idx: Int, midiPitch: Int ->
                                val step = auditionSteps.getOrNull(idx % auditionSteps.size.coerceAtLeast(1))
                                ScoreNote(
                                    midiNote = midiPitch,
                                    position = idx.toFloat() / soundSequencePitches.size.coerceAtLeast(1).toFloat(),
                                    duration = 0.25f,
                                    string = step?.first,
                                    fret = step?.second
                                )
                            }

                            viewModel.currentScore.value = Score(
                                title = "Live Audition",
                                timeSignature = "4/4",
                                measures = listOf(Measure(notes = mockNotesList))
                            )

                            playbackJob = scope.launch {
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                } catch (_: Exception) {}

                                try {
                                    if (activeSoundStyle == InstrumentSoundStyle.ALL_AT_ONCE) {
                                        // Sound all notes at once with ample sustain
                                        viewModel.scorePlayheadMeasure.value = 0
                                        viewModel.scorePlayheadOffset.value = 0f
                                        viewModel.midiActiveNotes.clear()
                                        currentPlayingCoordinate = null
                                        soundSequencePitches.forEach { midiPitch ->
                                            viewModel.midiActiveNotes[midiPitch] = true
                                            try {
                                                viewModel.audioEngine.noteOn(midiPitch, 0.85f)
                                            } catch (_: Exception) {}
                                        }
                                        delay(1200L)
                                        soundSequencePitches.forEach { midiPitch ->
                                            try {
                                                viewModel.audioEngine.noteOff(midiPitch)
                                            } catch (_: Exception) {}
                                        }
                                    } else {
                                        // Relaxed musical speeds: ~280ms for sequential strum, ~340ms for expressive arpeggio
                                        val stepDelay = if (activeSoundStyle == InstrumentSoundStyle.SEQUENTIAL) 280L else 340L
                                        val sequenceSteps: List<Triple<Int, Int, Int>> = if (auditionSteps.isNotEmpty()) {
                                            when (activeSoundStyle) {
                                                InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> {
                                                    if (auditionSteps.size > 2) auditionSteps + auditionSteps.subList(0, auditionSteps.size - 1).reversed()
                                                    else if (auditionSteps.size == 2) listOf(auditionSteps[0], auditionSteps[1], auditionSteps[0])
                                                    else auditionSteps
                                                }
                                                else -> auditionSteps
                                            }
                                        } else emptyList()

                                        soundSequencePitches.forEachIndexed { idx: Int, midiPitch: Int ->
                                            val step = sequenceSteps.getOrNull(idx)
                                            currentPlayingIdx = idx % pianoPitches.size.coerceAtLeast(1)
                                            currentPlayingCoordinate = step?.let { it.first to it.second }
                                            viewModel.scorePlayheadMeasure.value = 0
                                            viewModel.scorePlayheadOffset.value = idx.toFloat() / soundSequencePitches.size.coerceAtLeast(1).toFloat()

                                            viewModel.midiPlayingNote.value = midiPitch
                                            // Keep all chord/scale notes visible while highlighting the currently sounding note!
                                            if (viewModel.midiActiveNotes.isEmpty()) {
                                                viewModel.midiActiveNotes.putAll(activeMidiMap)
                                            }
//                                            viewModel.midiActiveNotes[midiPitch] = true

                                            try {
                                                viewModel.audioEngine.noteOn(midiPitch, 0.85f)
                                            } catch (_: Exception) {}
                                            delay(stepDelay)
                                        }
                                    }
                                } finally {
                                    // Ensure all sounding notes are cleanly silenced even if cancelled
                                    soundSequencePitches.forEach { midiPitch ->
                                        try {
                                            viewModel.audioEngine.noteOff(midiPitch)
                                        } catch (_: Exception) {}
                                    }
                                    currentPlayingIdx = -1
                                    currentPlayingCoordinate = null
                                    viewModel.midiPlayingNote.value = null
                                    viewModel.currentScore.value = null
                                    viewModel.midiActiveNotes.clear()
                                    viewModel.midiActiveNotes.putAll(activeMidiMap)
                                    isArpeggioRunning = false
                                }
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
                    ChordInstrumentType.GUITAR_ACOUSTIC, ChordInstrumentType.GUITAR_ELECTRIC, ChordInstrumentType.GUITAR_CLASSICAL -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        // Strings top to bottom: e4 (64), B3 (59), G3 (55), D3 (50), A2 (45), E2 (40)
                        val openGuitarStrings = listOf(64, 59, 55, 50, 45, 40)
                        val (fretMin, fretMax) = remember(selectedFretRange) {
                            when (selectedFretRange) {
                                "Open (0-3)" -> 0 to 3
                                "Low (1-5)" -> 1 to 5
                                "Mid (5-9)" -> 5 to 9
                                "High (9-14)" -> 9 to 14
                                "12+ Frets" -> 12 to 24
                                else -> 0 to 24
                            }
                        }

                        val highlightedGuitarCoordinate = remember(currentPlayingCoordinate, activeMidiToken, guitarShape.frets, isArpeggioRunning, mode, fretMin, fretMax) {
                            if (isArpeggioRunning) {
                                if (currentPlayingCoordinate != null) {
                                    currentPlayingCoordinate
                                } else if (activeMidiToken != null) {
                                    var targetMatch: Pair<Int, Int>? = null
                                    if (mode == 0) {
                                        for (s in guitarShape.frets.indices) {
                                            val fret = guitarShape.frets[s]
                                            if (fret != -1 && (openGuitarStrings[s] + fret) == activeMidiToken) {
                                                targetMatch = s to fret
                                                break
                                            }
                                        }
                                        // Fallback to pitch class if octave didn't match directly
                                        if (targetMatch == null) {
                                            for (s in guitarShape.frets.indices) {
                                                val fret = guitarShape.frets[s]
                                                if (fret != -1 && (openGuitarStrings[s] + fret) % 12 == activeMidiToken % 12) {
                                                    targetMatch = s to fret
                                                    break
                                                }
                                            }
                                        }
                                    } else {
                                        // Scale mode: find matching fret within the selected fret constraints
                                        for (s in openGuitarStrings.indices) {
                                            val fret = (activeMidiToken % 12 - openGuitarStrings[s] % 12 + 12) % 12
                                            if (fret in fretMin..fretMax) {
                                                targetMatch = s to fret
                                                break
                                            }
                                        }
                                        if (targetMatch == null) {
                                            for (s in openGuitarStrings.indices) {
                                                val fret = (activeMidiToken % 12 - openGuitarStrings[s] % 12 + 12) % 12
                                                if (fret in 0..15) {
                                                    targetMatch = s to fret
                                                    break
                                                }
                                            }
                                        }
                                    }
                                    targetMatch
                                } else null
                            } else null
                        }

                        val resolvedCaged = remember(selectedCagedFilter) {
                            when (selectedCagedFilter) {
                                "C-Shape" -> "C"
                                "A-Shape" -> "A"
                                "G-Shape" -> "G"
                                "E-Shape" -> "E"
                                "D-Shape" -> "D"
                                else -> "AUTO"
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedGuitarFretboard(
                                numFrets = 15,
                                accentColor = partColor,
                                rootNote = rootIdx,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                markOctaves = true,
                                highlightOctaveC = true,
                                cagedMode = resolvedCaged,
                                fretMinConstrain = fretMin,
                                fretMaxConstrain = fretMax,
                                chordFrets = if (mode == 0) guitarShape.frets else null,
                                chordFingers = if (mode == 0) guitarShape.fingers else null,
                                baseFret = if (mode == 0) (guitarShape.baseFret ?: 1) else 1,
                                barreFret = if (mode == 0) (guitarShape.barre ?: -1) else -1,
                                stringLabels = listOf("e", "B", "G", "D", "A", "E"),
                                openStringsSemis = listOf(4, 11, 7, 2, 9, 4),
                                activeCoordinate = highlightedGuitarCoordinate,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                activeNotes = pianoPitches,
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
                    ChordInstrumentType.PIANO_ACOUSTIC, ChordInstrumentType.PIANO_ELECTRIC -> {
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
                                showNoteNames = when (badgeDisplayMode) {
                                    "Note Names" -> true
                                    "Fingers (1-5)", "Fingers (1-4)" -> false
                                    else -> (mode == 1)
                                },                                isPlaying = isArpeggioRunning,
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
                        val highlightedUkuCoordinate = remember(currentPlayingCoordinate, activeMidiToken, ukuShape.frets, isArpeggioRunning, mode) {
                            if (isArpeggioRunning) {
                                if (currentPlayingCoordinate != null) {
                                    currentPlayingCoordinate
                                } else if (activeMidiToken != null) {
                                    // Strings top to bottom: A4 (69), E4 (64), C4 (60), G4 (67)
                                    val openUkuStrings = listOf(69, 64, 60, 67)
                                    var targetMatch: Pair<Int, Int>? = null
                                    if (mode == 0) {
                                        for (s in ukuShape.frets.indices) {
                                            val fret = ukuShape.frets[s]
                                            if (fret != -1 && (openUkuStrings[s] + fret) % 12 == activeMidiToken % 12) {
                                                targetMatch = s to fret
                                                break
                                            }
                                        }
                                    } else {
                                        for (s in openUkuStrings.indices) {
                                            val fret = (activeMidiToken % 12 - openUkuStrings[s] % 12 + 12) % 12
                                            if (fret in 0..5) {
                                                targetMatch = s to fret
                                                break
                                            }
                                        }
                                    }
                                    targetMatch
                                } else null
                            } else null
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedGuitarFretboard(
                                numFrets = 5,
                                accentColor = partColor,
                                rootNote = rootIdx,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                markOctaves = true,
                                highlightOctaveC = true,
                                chordFrets = if (mode == 0) ukuShape.frets else null,
                                chordFingers = if (mode == 0) ukuShape.fingers else null,
                                activeNotes = pianoPitches,
                                baseFret = if (mode == 0) ukuShape.baseFret else 1,
                                barreFret = ukuShape.barre ?: -1,
                                stringLabels = listOf("A", "E", "C", "G"),
                                openStringsSemis = listOf(9, 4, 0, 7),
                                activeCoordinate = highlightedUkuCoordinate,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
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
                    ChordInstrumentType.GUITAR_7 -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        // 7-string standard tuning: e4(64), B3(59), G3(55), D3(50), A2(45), E2(40), B1(35)
                        val open7Strings = listOf(64, 59, 55, 50, 45, 40, 35)
                        val (fretMin, fretMax) = remember(selectedFretRange) {
                            when (selectedFretRange) {
                                "Open (0-3)" -> 0 to 3
                                "Low (1-5)" -> 1 to 5
                                "Mid (5-9)" -> 5 to 9
                                "High (9-14)" -> 9 to 14
                                "12+ Frets" -> 12 to 24
                                else -> 0 to 24
                            }
                        }

                        val highlighted7Coordinate = remember(currentPlayingCoordinate, activeMidiToken, isArpeggioRunning, mode, fretMin, fretMax) {
                            if (isArpeggioRunning) {
                                if (currentPlayingCoordinate != null) {
                                    currentPlayingCoordinate
                                } else if (activeMidiToken != null) {
                                    var targetMatch: Pair<Int, Int>? = null
                                    for (s in open7Strings.indices) {
                                        val fret = (activeMidiToken % 12 - open7Strings[s] % 12 + 12) % 12
                                        if (fret in fretMin..fretMax) {
                                            targetMatch = s to fret
                                            break
                                        }
                                    }
                                    targetMatch
                                } else null
                            } else null
                        }

                        val padded7Frets = remember(guitarShape.frets) {
                            if (guitarShape.frets.size == 6) {
                                guitarShape.frets + listOf(-1) // mute 7th string B1 for 6-string shapes
                            } else guitarShape.frets
                        }
                        val padded7Fingers = remember(guitarShape.fingers) {
                            if (guitarShape.fingers.size == 6) {
                                guitarShape.fingers + listOf(0)
                            } else guitarShape.fingers
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedGuitarFretboard(
                                numFrets = 15,
                                accentColor = partColor,
                                rootNote = rootIdx,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                markOctaves = true,
                                highlightOctaveC = true,
                                fretMinConstrain = fretMin,
                                fretMaxConstrain = fretMax,
                                chordFrets = if (mode == 0) padded7Frets else null,
                                chordFingers = if (mode == 0) padded7Fingers else null,
                                baseFret = if (mode == 0) (guitarShape.baseFret ?: 1) else 1,
                                barreFret = if (mode == 0) (guitarShape.barre ?: -1) else -1,
                                stringLabels = listOf("e", "B", "G", "D", "A", "E", "B"),
                                openStringsSemis = listOf(4, 11, 7, 2, 9, 4, 11),
                                activeCoordinate = highlighted7Coordinate,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                activeNotes = pianoPitches,
                                onFretTapped = { _, _, midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument("GUITAR")
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(2.dp))
                            GuitarFingerLegend(accentColor = partColor)
                        }
                    }
                    ChordInstrumentType.HARMONIUM, ChordInstrumentType.TABLA -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            UnifiedHarmoniumVisualizer(
                                activeMidiNotes = activeMidiMap,
                                accentColor = partColor,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                onKeyTapped = { midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument("PIANO")
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(12.dp))
                            )
                            PianoFingerLegend(
                                accentColor = partColor,
                                fingering = pianoFingering,
                                noteNames = noteNames
                            )
                        }
                    }
                    ChordInstrumentType.SITAR -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedSitarVisualizer(
                                activeNotes = pianoPitches,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                accentColor = partColor,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                onFretTapped = { _, midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    ChordInstrumentType.VIOLIN -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedViolinFingerboard(
                                activeNotes = pianoPitches,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                accentColor = partColor,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                onPositionTapped = { _, midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument("VIOLIN")
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
                    ChordInstrumentType.FLUTE -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            UnifiedFluteVisualizer(
                                activeNotes = pianoPitches,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                accentColor = partColor,
                                onToneHoleTapped = { _, midiNote ->
                                    try {
                                        viewModel.audioEngine.setInstrument("FLUTE")
                                        viewModel.onMidiNoteOn(midiNote, 95)
                                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }

        // Interactive Format Switcher (Both, Staff, Tab)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Both", "Staff", "Tab").forEach { fmt ->
                val isSelected = unifiedFormat.equals(fmt, ignoreCase = true)
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
            fallbackTitle = viewModel.currentMediaItem.value?.title ?: viewModel.selectedMixerItem.value?.displayName ?: "", // <-- PASSED HERE
            scrollState = scrollState
        )

        if (selectedInstrument == ChordInstrumentType.GUITAR_ACOUSTIC || selectedInstrument == ChordInstrumentType.GUITAR_ELECTRIC || selectedInstrument == ChordInstrumentType.GUITAR_CLASSICAL || selectedInstrument == ChordInstrumentType.GUITAR_7 || selectedInstrument == ChordInstrumentType.UKULELE) {
            HorizontalDivider(color = partColor.copy(0.1f))
            CapoSection(
                capoFret = capoFret,
                activeRootIdx = rootIdx,
                onCapoChange = { playbackJob?.cancel(); capoFret = it },
                partColor = partColor,
                onKeyDispatched = { rootIdx = it }
            )
        } else if (selectedInstrument == ChordInstrumentType.PIANO_ACOUSTIC) {
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
internal fun PianoFingerLegend(
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
internal fun GuitarFingerLegend(accentColor: Color) {
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
    var activeLabTab by remember { mutableStateOf(if (instrumentType == ChordInstrumentType.PIANO_ACOUSTIC) "Piano" else if (instrumentType == ChordInstrumentType.UKULELE) "Uke" else "Guitar") }
    var selectedSpeed by remember { mutableStateOf("1.0x") }
    var playbackMode by remember { mutableStateOf("Sequential") }
    var isPlaying by remember { mutableStateOf(false) }

    var selectedMode by remember { mutableStateOf(0) } // 0 = Chords, 1 = Scales
    var mode by remember { mutableIntStateOf(0) }
    var badgeDisplayMode by remember { mutableStateOf("Note Names") }
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

// Use ASCII '#' so all lookups in RealChordsDb, RealPianoChordsDb, and NOTE_NAMES_CD match 100%!
    val roots = remember { NOTE_NAMES_CD }
    val currentRootName = roots[labRootIdx.coerceIn(0, roots.size - 1)]

    val chordSuffixes = listOf("Major", "Minor", "7th", "Major 7th", "Minor 7th", "Diminished", "Augmented", "Sus4", "Add9")
    val currentSuffixName = chordSuffixes.getOrElse(labSuffixIdx) { "Major" }
    val inversions = listOf("Root Position", "1st Inversion", "2nd Inversion")

    val activeChordPitches = remember(labRootIdx, labSuffixIdx, labInversion) {
        val labSuffixMap = listOf("", "m", "7", "maj7", "m7", "dim", "aug", "sus4", "add9")
        val chosenSuffix = labSuffixMap.getOrElse(labSuffixIdx) { "" }
        val inv = when (labInversion) {
            "1st Inversion" -> InversionStyle.FIRST
            "2nd Inversion" -> InversionStyle.SECOND
            else -> InversionStyle.ROOT
        }
        ChordVoicingEngine.buildPianoShape(
            rootIdx = labRootIdx,
            suffix = chosenSuffix,
            style = PianoVoicingStyle.CLOSE,
            inversion = inv,
            octave = 4
        )
    }

    val activeMidiMap = remember(activeChordPitches) {
        activeChordPitches.associateWith { true }
    }

    val orangeColor = Color(0xFFFF9E1B)
    val darkCardBg = Color(0xFF13151B)
    val sidebarBg = Color(0xFF0F1015)
    val rosewoodBrush = Brush.verticalGradient(listOf(Color(0xFF4A342E), Color(0xFF382723), Color(0xFF291C19)))

    // 3 Sounding types: "Sequential" (one-by-one), "Block Chord" (all at once), "Roundtrip Arp" (low to high then back down)
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

        while (isPlaying) {
            when (playbackMode) {
                "Block Chord" -> {
                    currentAuditionIdx = null
                    activeChordPitches.forEach { midi ->
                        try {
                            val inst = if (activeLabTab == "Guitar" || activeLabTab == "Uke") "GUITAR" else "PIANO"
                            viewModel.audioEngine.setInstrument(inst)
                            viewModel.audioEngine.noteOn(midi, 0.85f)
                        } catch (_: Exception) {}
                    }
                    delay((650L / speedMult).toLong())
                }
                "Roundtrip Arp" -> {
                    val roundtripPitches = if (activeChordPitches.size > 2) {
                        activeChordPitches + activeChordPitches.subList(0, activeChordPitches.size - 1).reversed()
                    } else if (activeChordPitches.size == 2) {
                        listOf(activeChordPitches[0], activeChordPitches[1], activeChordPitches[0])
                    } else {
                        activeChordPitches
                    }
                    val stepDelay = ((320L) / speedMult).toLong()
                    for (pitch in roundtripPitches) {
                        if (!isPlaying) break
                        currentAuditionIdx = activeChordPitches.indexOf(pitch)
                        try {
                            val inst = if (activeLabTab == "Guitar" || activeLabTab == "Uke") "GUITAR" else "PIANO"
                            viewModel.audioEngine.setInstrument(inst)
                            viewModel.audioEngine.noteOn(pitch, 0.85f)
                        } catch (_: Exception) {}
                        delay(stepDelay)
                    }
                }
                else -> { // "Sequential" / "Arpeggio"
                    val stepDelay = ((380L) / speedMult).toLong()
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
                }
            }
        }
    }

    com.creativeblisslabs.peqlearn.app.ui.components.FullscreenDialog(
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
                            val guitarTuningList = listOf("Standard (EADGBE)", "Drop D (DADGBE)", "Half-Step Down (Eb)", "DADGAD Celtic", "Open G")
                        M3DropdownMenu(
                            label = "Tuning",
                            selectedText = tuningOption,
                            items = guitarTuningList,
                            onItemSelect = { tuningOption = guitarTuningList[it] }
                        )
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
                            val ukeTuningList = listOf("Standard (GCEA)", "Low G (gCEA)", "Baritone (DGBE)")
                        M3DropdownMenu(
                            label = "Ukulele Tuning",
                            selectedText = tuningOption,
                            items = ukeTuningList,
                            onItemSelect = { tuningOption = ukeTuningList[it] }
                        )
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
                       val namingList = listOf("Western (C, D, E)", "Sargam (Sa, Re, Ga)", "Solfege (Do, Re, Mi)")
                        M3DropdownMenu(
                            label = "Note Naming System",
                            selectedText = noteNamingSystem,
                            items = namingList,
                            onItemSelect = { noteNamingSystem = namingList[it] }
                        )
                    }

                    // Sound Generator
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sound Generator", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                      val soundGenList = listOf("Acoustic Piano", "Electric Piano", "Acoustic Guitar", "Electric Guitar", "Classical Guitar", "Harmonium", "Sitar", "Violin", "Flute")
                        M3DropdownMenu(
                            label = "Sound Generator",
                            selectedText = synthSound,
                            items = soundGenList,
                            onItemSelect = { idx ->
                                synthSound = soundGenList[idx]
                                val engineName = when (synthSound) {
                                    "Electric Piano" -> "PIANO_ELECTRIC"
                                    "Acoustic Guitar" -> "GUITAR_ACOUSTIC"
                                    "Electric Guitar" -> "GUITAR_ELECTRIC"
                                    "Classical Guitar" -> "GUITAR_CLASSICAL"
                                    "Harmonium" -> "HARMONIUM"
                                    "Sitar" -> "SITAR"
                                    "Violin" -> "VIOLIN"
                                    "Flute" -> "FLUTE"
                                    else -> "PIANO_ACOUSTIC"
                                }
                                runCatching { viewModel.audioEngine.setInstrument(engineName) }
                            }
                        )
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

                            // Playback Mode Chip: Sequential / Block Chord / Roundtrip Arp
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .padding(horizontal = 10.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2129))
                                    .clickable {
                                        playbackMode = when (playbackMode) {
                                            "Sequential" -> "Block Chord"
                                            "Block Chord" -> "Roundtrip Arp"
                                            else -> "Sequential"
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (playbackMode) {
                                        "Sequential" -> "Seq Strum"
                                        "Block Chord" -> "Block (All)"
                                        "Roundtrip Arp" -> "Arp (L⇄R)"
                                        else -> "Seq Strum"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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
                                    showNoteNames = when (badgeDisplayMode) {
                                        "Note Names" -> true
                                        "Fingers (1-5)", "Fingers (1-4)" -> false
                                        else -> (mode == 1)
                                    },                                    isPlaying = isPlaying,
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
                                val activeLabPlayingPitch = if (isPlaying) currentAuditionIdx?.let { activeChordPitches.getOrNull(it) } else null
                                UnifiedGuitarFretboard(
                                    numFrets = numF,
                                    activeNotes = activeChordPitches,
                                    activeMidiNote = activeLabPlayingPitch,
                                    rootNote = labRootIdx,
                                    accentColor = orangeColor,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                    baseFret = if (capoFretSelected > 0) capoFretSelected else 1,
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
                                val activeLabPlayingPitch = if (isPlaying) currentAuditionIdx?.let { activeChordPitches.getOrNull(it) } else null
                                UnifiedGuitarFretboard(
                                    numFrets = 7,
                                    activeNotes = activeChordPitches,
                                    activeMidiNote = activeLabPlayingPitch,
                                    rootNote = labRootIdx,
                                    accentColor = orangeColor,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                    stringLabels = listOf("A", "E", "C", "G"),
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
                                    val activeLabPlayingPitch = if (isPlaying) currentAuditionIdx?.let { activeChordPitches.getOrNull(it) } else null
                                    UnifiedPianoKeyboard(
                                        activeMidiNotes = activeMidiMap,
                                        accentColor = orangeColor,
                                        activeCoordinate = activeLabPlayingPitch,
                                        startNote = 48,
                                        endNote = 84,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                        isPlaying = isPlaying,
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
                                        activeMidiNote = activeLabPlayingPitch,
                                        rootNote = labRootIdx,
                                        accentColor = orangeColor,
showNoteNames = when (badgeDisplayMode) {
    "Note Names" -> true
    "Fingers (1-5)", "Fingers (1-4)" -> false
    else -> (mode == 1)
},                                        fretboardBrush = rosewoodBrush,
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
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
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
                    text = {
                        Text(
                            text = item,
                            fontSize = 12.sp,
                            fontWeight = if (item == selectedText) FontWeight.Bold else FontWeight.Normal
                        )
                    },
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
internal fun CdLabel(text: String, modifier: Modifier = Modifier) {
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
internal fun ScrollableRow(
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