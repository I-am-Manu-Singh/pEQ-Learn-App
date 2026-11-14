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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Normalizes UI chord suffixes (e.g. "", "m", "M7", "min7") to canonical database keys
 * used by `RealChordsDb` and `RealPianoChordsDb` ("major", "minor", "maj7", "m7", etc.).
 */
private fun normalizeDbSuffix(rawSuffix: String): String {
    return when (val trimmed = rawSuffix.trim()) {
        "", "M", "maj", "Major", "major" -> "major"
        "m", "min", "Minor", "minor", "-" -> "minor"
        "M7", "Maj7", "maj7" -> "maj7"
        "min7", "m7", "-7" -> "m7"
        "dom7", "7" -> "7"
        "dim", "°" -> "dim"
        "dim7", "°7" -> "dim7"
        "aug", "+" -> "aug"
        "m7b5", "ø", "ø7" -> "m7b5"
        else -> trimmed
    }
}

/**
 * Backward-compatible entry point for `ChordDisplayGalleryScreen`.
 * Delegates directly to the unified `ChordDictionaryPanel` without any code duplication!
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
    val mappedInstrument = remember(initialInstrument) {
        when (initialInstrument.lowercase()) {
            "piano" -> ChordInstrumentType.PIANO_ACOUSTIC
            "ukulele", "uke" -> ChordInstrumentType.UKULELE
            "sitar" -> ChordInstrumentType.SITAR
            "violin" -> ChordInstrumentType.VIOLIN
            "flute" -> ChordInstrumentType.FLUTE
            "harmonium" -> ChordInstrumentType.HARMONIUM
            else -> ChordInstrumentType.GUITAR_ACOUSTIC
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1218)
    ) {
        ChordDictionaryPanel(
            viewModel = viewModel,
            initialRootName = initialRoot,
            initialSuffixName = initialSuffix,
            initialInstrumentType = mappedInstrument,
            showTopBackBar = true,
            onBack = onBack,
            isScrollable = true
        )
    }
}

/**
 * Unified Master Chord & Scale Dictionary + Multi-Voicing Gallery Studio.
 * Combines `ChordDictionaryPanel` and `ChordDisplayGalleryScreen` into a single, race-condition-free
 * interactive workstation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordDictionaryPanel(
    viewModel: AppViewModel,
    unifiedFormat: String = "STANDARD",
    onFormatChange: (String) -> Unit = {},
    onPracticeTapped: () -> Unit = {},
    onBack: () -> Unit = {},
    isScrollable: Boolean = false,
    initialRootName: String? = null,
    initialSuffixName: String? = null,
    initialInstrumentType: ChordInstrumentType? = null,
    showTopBackBar: Boolean = false
) {
    val partColor = Color(0xFFFFB347)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val roots = remember { NOTE_NAMES_CD } // ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]

    var mode by remember { mutableIntStateOf(0) } // 0 = Chords, 1 = Scales
    var rootIdx by remember {
        val initIdx = initialRootName?.let { r ->
            roots.indexOf(r.replace("♯", "#")).takeIf { it >= 0 }
        } ?: 0
        mutableIntStateOf(initIdx)
    }
    var chordTypeIdx by remember {
        val normInit = initialSuffixName?.let { normalizeDbSuffix(it) }
        val foundIdx = if (normInit != null) {
            CHORD_TYPE_DEFS.indexOfFirst { normalizeDbSuffix(it.suffix) == normInit }.takeIf { it >= 0 } ?: 0
        } else 0
        mutableIntStateOf(foundIdx)
    }

    // Support direct selection of extended database suffixes (from RealChordsDb / RealPianoChordsDb)
    // that go beyond CHORD_TYPE_DEFS
    var customExtendedSuffix by remember {
        val normInit = initialSuffixName?.let { normalizeDbSuffix(it) }
        val inStandard = normInit == null || CHORD_TYPE_DEFS.any { normalizeDbSuffix(it.suffix) == normInit }
        mutableStateOf(if (!inStandard) normInit else null)
    }

    var selectedInstrument by remember {
        mutableStateOf(initialInstrumentType ?: ChordInstrumentType.GUITAR_ACOUSTIC)
    }
    var selectedDiff by remember { mutableStateOf(ChordDiff.EASY) }
    var capoFret by remember { mutableIntStateOf(0) }
    var fromKeyIdx by remember { mutableIntStateOf(0) }
    var toKeyIdx by remember { mutableIntStateOf(7) }

    var selectedScaleIdx by remember { mutableIntStateOf(0) }
    var selectedIntervalIdx by remember { mutableIntStateOf(0) }
    var selectedVoicingIdx by remember { mutableIntStateOf(0) }
    var selectedPositionIndex by remember { mutableIntStateOf(0) }
    var selectedOctaveSpan by remember { mutableIntStateOf(1) }
    var selectedStartFret by remember { mutableIntStateOf(1) }
    var selectedVoicingType by remember { mutableStateOf("Open") }
    var selectedCagedFilter by remember { mutableStateOf("All CAGED") }
    var selectedFretRange by remember { mutableStateOf("All Frets") }
    var scaleDirectionMode by remember { mutableStateOf("Up & Down") }
    var badgeDisplayMode by remember { mutableStateOf("Auto") }

    // Search & Category Filtering (merged from ChordDisplayGalleryScreen)
    var searchQuery by remember { mutableStateOf("") }
    var selectedQualityCategory by remember { mutableStateOf("All") }
    var showSearchAndFilters by remember { mutableStateOf(showTopBackBar) }
    var showAllVariationsGrid by remember { mutableStateOf(false) }

    // Expandable Bottom Tool Sections (Capo, Transposer, Quick Substitutions, 3D Lab)
    var activeBottomDrawer by remember { mutableStateOf<String?>(null) } // "CAPO", "TRANSPOSE", "SUBS"
    var isLabModeOpen by remember { mutableStateOf(false) }

    val cagedOptions = remember { listOf("All CAGED", "C-Shape", "A-Shape", "G-Shape", "E-Shape", "D-Shape") }
    val fretRangeOptions = remember { listOf("All Frets", "Open (0-3)", "Low (1-5)", "Mid (5-9)", "High (9-14)", "12+ Frets") }
    val qualityCategories = remember { listOf("All", "Major", "Minor", "7th", "Extended (9/11/13)", "Dim/Aug", "Sus") }

    val guitarVoicings = remember { listOf("Open", "Barre", "Power", "Shell") }
    val pianoVoicings = remember { listOf("Closed", "Open", "Shell", "Drop 2") }
    val ukeVoicings = remember { listOf("Open", "Barre", "Closed") }
    val voicingOptions = remember { listOf("Root Position", "1st Inversion", "2nd Inversion", "3rd Inversion", "Drop 2") }
    val octaveOptions = remember { listOf("1 Octave", "2 Octaves") }

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

    // Race-condition-safe Playback State & Generation Token
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var singleNoteTapJob by remember { mutableStateOf<Job?>(null) }
    var playbackGenerationId by remember { mutableIntStateOf(0) }
    val currentlySoundingNotes = remember { mutableStateListOf<Int>() }

    var currentPlayingCoordinate by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var currentPlayingIdx by remember { mutableIntStateOf(-1) }
    var isArpeggioRunning by remember { mutableStateOf(false) }
    var activeSoundStyle by remember { mutableStateOf(InstrumentSoundStyle.SEQUENTIAL) }

    // Safe helper to stop all active playback and silence any sounding MIDI notes immediately
    fun stopActivePlaybackAndSilence(restoreStaticMap: Map<Int, Boolean>? = null) {
        playbackGenerationId++
        playbackJob?.cancel()
        playbackJob = null
        singleNoteTapJob?.cancel()
        singleNoteTapJob = null

        val toSilence = currentlySoundingNotes.toSet()
        currentlySoundingNotes.clear()
        toSilence.forEach { midi ->
            runCatching { viewModel.audioEngine.noteOff(midi) }
        }

        isArpeggioRunning = false
        currentPlayingIdx = -1
        currentPlayingCoordinate = null
        viewModel.midiPlayingNote.value = null
        if (restoreStaticMap != null) {
            viewModel.midiActiveNotes.clear()
            viewModel.midiActiveNotes.putAll(restoreStaticMap)
        }
    }

    // Safe single-note audition helper (prevents hanging notes when tapping frets/keys rapidly)
    fun auditionSingleMidiNote(midiNote: Int, engineInstrument: String) {
        singleNoteTapJob?.cancel()
        singleNoteTapJob = scope.launch {
            try {
                currentlySoundingNotes.add(midiNote)
                runCatching {
                    viewModel.audioEngine.setInstrument(engineInstrument)
                    viewModel.onMidiNoteOn(midiNote, 95)
                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                }
                delay(360L)
            } finally {
                currentlySoundingNotes.remove(midiNote)
                runCatching { viewModel.audioEngine.noteOff(midiNote) }
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val verificationLog = ChordVoicingEngine.verifyIntegrity()
            println("[CHORD_VOICING_ENGINE_VERIFY]\n$verificationLog")
        }
    }

    val scaleFormulas = remember {
        listOf(
            "Major (Bilawal)" to SCALE_MAJOR_PATTERN,
            "Natural Minor (Asavari)" to SCALE_MINOR_NAT_PATTERN,
            "Harmonic Minor" to SCALE_MINOR_HAR_PATTERN,
            "Melodic Minor" to SCALE_MINOR_MEL_PATTERN,
            "Major Pentatonic (Bhoopali)" to SCALE_PENTATONIC_M_PATTERN,
            "Minor Pentatonic (Malkauns)" to SCALE_PENTATONIC_N_PATTERN,
            "Blues Scale" to SCALE_BLUES_PATTERN,
            "Dorian Mode (Kafi)" to SCALE_DORIAN_PATTERN,
            "Mixolydian Mode (Khamaj)" to SCALE_MIXOLYDIAN_PATTERN,
            "Lydian Mode (Kalyan / Yaman)" to SCALE_LYDIAN_PATTERN,
            "Phrygian (Bhairavi)" to SCALE_BHAIRAVI_PATTERN,
            "Bhairav Thaat" to SCALE_BHAIRAV_PATTERN,
            "Thaat Kalyan (Yaman)" to SCALE_KALYAN_PATTERN,
            "Thaat Marwa" to SCALE_MARWA_PATTERN,
            "Thaat Poorvi" to SCALE_POORVI_PATTERN,
            "Thaat Todi" to SCALE_TODI_PATTERN,
            "Mayamalavagowla Parent" to SCALE_BHAIRAV_PATTERN
        )
    }

    val safeRootIdx = rootIdx.coerceIn(0, roots.lastIndex)
    val currentRootName = roots[safeRootIdx]
    val typeDef = CHORD_TYPE_DEFS[chordTypeIdx.coerceIn(0, CHORD_TYPE_DEFS.lastIndex)]
    val effectiveRawSuffix = customExtendedSuffix ?: typeDef.suffix
    val normalizedDbSuffix = remember(effectiveRawSuffix) { normalizeDbSuffix(effectiveRawSuffix) }
    val chordDisplayName = if (customExtendedSuffix != null) {
        "$currentRootName $customExtendedSuffix"
    } else {
        "$currentRootName${typeDef.suffix}"
    }
    val scalePair = scaleFormulas[selectedScaleIdx.coerceIn(0, scaleFormulas.lastIndex)]

    // Complete suffix catalog for Search & Category Filter (supports all RealChordsDb & RealPianoChordsDb suffixes)
    val allDatabaseSuffixes = remember(isKeyboardFamily, isUkulele) {
        val dbList = when {
            isKeyboardFamily -> RealPianoChordsDb.suffixes
            isUkulele -> RealChordsDb.getAvailableSuffixes("ukulele")
            else -> RealChordsDb.getAvailableSuffixes("guitar")
        }
        (CHORD_TYPE_DEFS.map { normalizeDbSuffix(it.suffix) } + dbList).distinct()
    }

    val filteredDatabaseSuffixes = remember(allDatabaseSuffixes, searchQuery, selectedQualityCategory) {
        allDatabaseSuffixes.filter { s ->
            val matchesSearch = if (searchQuery.isBlank()) true else s.contains(searchQuery.trim(), ignoreCase = true)
            val matchesCat = when (selectedQualityCategory) {
                "Major" -> s.contains("maj", ignoreCase = true) || s == "major" || s == "6"
                "Minor" -> s.contains("min", ignoreCase = true) || (s.startsWith("m") && !s.startsWith("maj"))
                "7th" -> s.contains("7")
                "Extended (9/11/13)" -> s.contains("9") || s.contains("11") || s.contains("13")
                "Dim/Aug" -> s.contains("dim", ignoreCase = true) || s.contains("aug", ignoreCase = true) || s.contains("°")
                "Sus" -> s.contains("sus", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCat
        }
    }

    // All Fretboard Voicing Variations from RealChordsDb (for the integrated Voicing Carousel & Grid)
    val allFretboardVariations: List<RealChordPosition> = remember(
        isUkulele,
        isFretboardInstrument,
        currentRootName,
        normalizedDbSuffix,
        selectedCagedFilter,
        selectedFretRange,
        capoFret
    ) {
        if (!isFretboardInstrument) {
            emptyList()
        } else {
            val effectiveLookupRoot = roots[(safeRootIdx + capoFret) % 12]
            val rawPositions = if (isUkulele) {
                RealChordsDb.getUkulelePositions(effectiveLookupRoot, normalizedDbSuffix)
                    .ifEmpty { RealChordsDb.getUkulelePositions(effectiveLookupRoot, effectiveRawSuffix) }
            } else {
                RealChordsDb.getGuitarPositions(effectiveLookupRoot, normalizedDbSuffix)
                    .ifEmpty { RealChordsDb.getGuitarPositions(effectiveLookupRoot, effectiveRawSuffix) }
            }

            if (!isUkulele) {
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
    }

    // Clamp `selectedPositionIndex` synchronously to prevent IndexOutOfBounds race conditions on state switches
    val safePositionIndex = if (allFretboardVariations.isEmpty()) {
        0
    } else {
        selectedPositionIndex.coerceIn(0, allFretboardVariations.lastIndex)
    }

    LaunchedEffect(allFretboardVariations.size, safeRootIdx, normalizedDbSuffix, selectedInstrument) {
        if (selectedPositionIndex >= allFretboardVariations.size && allFretboardVariations.isNotEmpty()) {
            selectedPositionIndex = 0
        }
    }

    // Piano database lookup + procedural fallback
    val dbPianoChord = remember(currentRootName, normalizedDbSuffix, effectiveRawSuffix) {
        RealPianoChordsDb.getChord(currentRootName, normalizedDbSuffix)
            ?: RealPianoChordsDb.getChord(currentRootName, effectiveRawSuffix)
    }

    // Compute All Piano Voicing & Inversion Variations (Root, 1st Inv, 2nd Inv, 3rd Inv, Drop 2)
    val allPianoVariations: List<Pair<String, List<Int>>> = remember(
        dbPianoChord,
        safeRootIdx,
        typeDef,
        selectedIntervalIdx,
        selectedVoicingType,
        capoFret
    ) {
        val baseIntervals = typeDef.intervals
        val extraSemitone = if (selectedIntervalIdx > 0 && selectedIntervalIdx in INTERVALS_DATA.indices) {
            INTERVALS_DATA[selectedIntervalIdx][0].toIntOrNull()
                ?: INTERVALS_DATA[selectedIntervalIdx].getOrNull(4)?.toIntOrNull()
        } else null

        val rootMidi = 60 + ((safeRootIdx + capoFret) % 12)
        val rawBaseMidi = if (dbPianoChord != null && dbPianoChord.midi.isNotEmpty() && extraSemitone == null) {
            dbPianoChord.midi.map { it + capoFret }
        } else {
            val withExtra = if (extraSemitone != null) (baseIntervals + extraSemitone).distinct().sorted() else baseIntervals
            withExtra.map { rootMidi + it }
        }

        val list = mutableListOf<Pair<String, List<Int>>>()
        if (rawBaseMidi.isNotEmpty()) {
            list.add("Root Position" to rawBaseMidi)
            if (rawBaseMidi.size >= 2) {
                val inv1 = (rawBaseMidi.drop(1) + (rawBaseMidi.first() + 12)).sorted()
                list.add("1st Inversion" to inv1)
            }
            if (rawBaseMidi.size >= 3) {
                val inv2 = (rawBaseMidi.drop(2) + rawBaseMidi.take(2).map { it + 12 }).sorted()
                list.add("2nd Inversion" to inv2)
            }
            if (rawBaseMidi.size >= 4) {
                val inv3 = (rawBaseMidi.drop(3) + rawBaseMidi.take(3).map { it + 12 }).sorted()
                list.add("3rd Inversion" to inv3)
            }
            if (rawBaseMidi.size >= 3) {
                val drop2 = rawBaseMidi.toMutableList().apply {
                    val dropIdx = size - 2
                    this[dropIdx] = this[dropIdx] - 12
                }.sorted()
                list.add("Drop 2 Voicing" to drop2)
            }
            if (rawBaseMidi.size >= 3) {
                val openSpread = rawBaseMidi.mapIndexed { idx, m -> if (idx == 1) m + 12 else m }.sorted()
                list.add("Open Spread" to openSpread)
            }
        }
        list
    }

    val safePianoVariationIdx = if (allPianoVariations.isEmpty()) {
        0
    } else {
        selectedVoicingIdx.coerceIn(0, allPianoVariations.lastIndex)
    }

    val voicedIntervals = remember(chordTypeIdx, safePianoVariationIdx, selectedIntervalIdx, selectedVoicingType) {
        val extraSemitone = if (selectedIntervalIdx > 0 && selectedIntervalIdx in INTERVALS_DATA.indices) {
            INTERVALS_DATA[selectedIntervalIdx][0].toIntOrNull()
                ?: INTERVALS_DATA[selectedIntervalIdx].getOrNull(4)?.toIntOrNull()
        } else null

        val inverted = ChordVoicingEngine.getVoicedIntervals(
            baseIntervals = typeDef.intervals,
            voicingIdx = safePianoVariationIdx,
            extraSemitone = extraSemitone
        )
        when (selectedVoicingType) {
            "Open" -> if (inverted.size >= 3) {
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

    val activeMidiMap = remember(
        safeRootIdx,
        chordTypeIdx,
        customExtendedSuffix,
        allPianoVariations,
        safePianoVariationIdx,
        voicedIntervals,
        mode,
        selectedScaleIdx,
        selectedOctaveSpan,
        capoFret
    ) {
        if (mode == 0 && allPianoVariations.isNotEmpty()) {
            allPianoVariations[safePianoVariationIdx].second.associateWith { true }
        } else {
            val intervals = if (mode == 0) {
                voicedIntervals
            } else {
                val basePattern = scalePair.second
                val numOctaves = if (selectedOctaveSpan == 1) 2 else 1
                val multiOctave = mutableListOf<Int>()
                for (oct in 0 until numOctaves) {
                    basePattern.forEach { semi -> multiOctave.add(semi + oct * 12) }
                }
                multiOctave.add(numOctaves * 12)
                multiOctave.distinct().sorted()
            }
            ChordVoicingEngine.getActiveMidiNotes(
                rootIdx = (safeRootIdx + (if (mode == 0) capoFret else 0)) % 12,
                intervals = intervals,
                baseMidi = 60
            )
        }
    }

    val sortedMidi = remember(activeMidiMap) { activeMidiMap.keys.sorted() }
    val noteNames = remember(sortedMidi) { sortedMidi.map { roots[(it % 12 + 12) % 12] } }
    val pianoPitches = sortedMidi

    val pianoFingering = remember(pianoPitches, effectiveRawSuffix, mode, safePianoVariationIdx) {
        ChordVoicingEngine.computePianoFingering(
            pitches = pianoPitches,
            chordSuffix = effectiveRawSuffix,
            isScale = (mode != 0),
            voicingIdx = safePianoVariationIdx
        )
    }

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

    // Fallback procedural shapes when user hasn't picked a specific carousel index or when RealChordsDb has no entry
    val fallbackGuitarShape = remember(
        safeRootIdx, chordTypeIdx, selectedDiff, capoFret,
        selectedStartFret, selectedVoicingType, mode,
        selectedCagedFilter, selectedFretRange, resolvedFretMin, resolvedFretMax
    ) {
        ChordVoicingEngine.getDictionaryGuitarShape(
            rootName = currentRootName,
            suffix = effectiveRawSuffix,
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

    val fallbackUkuShape = remember(safeRootIdx, chordTypeIdx, selectedDiff, capoFret, selectedStartFret, selectedVoicingType, mode) {
        ChordVoicingEngine.getDictionaryUkuShape(
            rootName = currentRootName.replace("♯", "#"),
            suffix = effectiveRawSuffix,
            diff = selectedDiff,
            capoFret = capoFret,
            startFret = selectedStartFret,
            voicingType = selectedVoicingType,
            fallbackIntervals = typeDef.intervals
        )
    }

    // Active RealChordPosition from the variations carousel (if available)
    val selectedRealVariation: RealChordPosition? = allFretboardVariations.getOrNull(safePositionIndex)

    // Synchronize the Hero Fretboard with the currently selected variation card in the carousel!
    val effectiveGuitarFrets: List<Int> = remember(selectedRealVariation, fallbackGuitarShape) {
        if (selectedRealVariation != null && selectedRealVariation.frets.size >= 6) {
            selectedRealVariation.frets.reversed()
        } else {
            fallbackGuitarShape.frets
        }
    }
    val effectiveGuitarFingers: List<Int> = remember(selectedRealVariation, fallbackGuitarShape) {
        if (selectedRealVariation != null && selectedRealVariation.fingers.size >= 6) {
            selectedRealVariation.fingers.reversed()
        } else {
            fallbackGuitarShape.fingers
        }
    }
    val effectiveGuitarBaseFret: Int = selectedRealVariation?.baseFret ?: (fallbackGuitarShape.baseFret ?: 1)
    val effectiveGuitarBarre: Int = selectedRealVariation?.barres?.firstOrNull() ?: (fallbackGuitarShape.barre ?: -1)

    val effectiveUkeFrets: List<Int> = remember(selectedRealVariation, fallbackUkuShape) {
        if (selectedRealVariation != null && selectedRealVariation.frets.size == 4) {
            selectedRealVariation.frets.reversed()
        } else {
            fallbackUkuShape.frets
        }
    }
    val effectiveUkeFingers: List<Int> = remember(selectedRealVariation, fallbackUkuShape) {
        if (selectedRealVariation != null && selectedRealVariation.fingers.size == 4) {
            selectedRealVariation.fingers.reversed()
        } else {
            fallbackUkuShape.fingers
        }
    }
    val effectiveUkeBaseFret: Int = selectedRealVariation?.baseFret ?: fallbackUkuShape.baseFret
    val effectiveUkeBarre: Int = selectedRealVariation?.barres?.firstOrNull() ?: (fallbackUkuShape.barre ?: -1)

    val cadenceList = remember(safeRootIdx) {
        ChordVoicingEngine.getCadenceProgressions(rootIdx = safeRootIdx, rootNames = roots)
    }

    // Race-condition-safe Audition Launcher for any variation card or Hero Play button
    fun launchAuditionSequence(customVariationPos: RealChordPosition? = null, customPianoNotes: List<Int>? = null) {
        stopActivePlaybackAndSilence()
        val myGenId = ++playbackGenerationId
        isArpeggioRunning = true

        val openStrings = when {
            isUkulele -> listOf(69, 64, 60, 67)
            is7String -> listOf(64, 59, 55, 50, 45, 40, 35)
            else -> listOf(64, 59, 55, 50, 45, 40)
        }

        val sequenceSteps: List<Triple<Int, Int, Int>> = when {
            customPianoNotes != null -> {
                val baseSteps = customPianoNotes.mapIndexed { idx, midi -> Triple(idx, 0, midi) }
                if (activeSoundStyle == InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP && baseSteps.size > 2) {
                    baseSteps + baseSteps.subList(0, baseSteps.size - 1).reversed()
                } else baseSteps
            }
            customVariationPos != null && mode == 0 && isFretboardInstrument -> {
                val (_, _, strumSteps) = BiomechanicalPerformersEngine.normalizeRealChordPositionTopToBottom(
                    pos = customVariationPos,
                    openStringsMidiHighToLow = openStrings
                )
                if (activeSoundStyle == InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP && strumSteps.size > 2) {
                    strumSteps + strumSteps.subList(0, strumSteps.size - 1).reversed()
                } else strumSteps
            }
            else -> {
                ChordVoicingEngine.generateAuditionSteps(
                    mode = mode,
                    openStrings = openStrings,
                    isFretboardInstrument = isFretboardInstrument,
                    isGuitarFamily = isGuitarFamily,
                    isUkulele = isUkulele,
                    is7String = is7String,
                    guitarShapeFrets = effectiveGuitarFrets,
                    ukuShapeFrets = effectiveUkeFrets,
                    ukuBaseFret = effectiveUkeBaseFret,
                    rootIdx = safeRootIdx,
                    scaleFormulaSemis = scalePair.second,
                    selectedFretRange = selectedFretRange,
                    selectedStartFret = selectedStartFret,
                    selectedOctaveSpan = selectedOctaveSpan,
                    pianoPitches = pianoPitches,
                    scaleDirectionMode = scaleDirectionMode,
                    isRoundTrip = (activeSoundStyle == InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP)
                )
            }
        }

        val soundSequencePitches: List<Int> = sequenceSteps.map { it.third }
        val notesPerBar = 8
        val chunkedSteps = sequenceSteps.chunked(notesPerBar).ifEmpty { listOf(emptyList()) }
        val builtMeasures = chunkedSteps.mapIndexed { mIdx, barSteps ->
            val barNotes = barSteps.mapIndexed { idxInBar, step ->
                val midiPitch = step.third
                val assignedFinger = pianoFingering.notes.indexOf(midiPitch)
                    .takeIf { it >= 0 }
                    ?.let { pianoFingering.fingers.getOrNull(it) }
                ScoreNote(
                    id = (mIdx * notesPerBar) + idxInBar + 1,
                    midiNote = midiPitch,
                    position = if (activeSoundStyle == InstrumentSoundStyle.ALL_AT_ONCE && mode == 0) 0f else idxInBar * 0.5f,
                    duration = if (activeSoundStyle == InstrumentSoundStyle.ALL_AT_ONCE && mode == 0) 4.0f else 0.5f,
                    string = step.first.takeIf { isFretboardInstrument },
                    fret = step.second.takeIf { isFretboardInstrument },
                    fingering = assignedFinger
                )
            }
            Measure(measureIndex = mIdx, notes = barNotes)
        }

        val rawMockScore = Score(
            title = if (mode == 0) "$chordDisplayName Audition" else "$currentRootName ${scalePair.first}",
            timeSignature = "4/4",
            tuningMidi = openStrings,
            capo = capoFret,
            measures = builtMeasures
        )
        val enrichedScore = BiomechanicalPerformersEngine.enrichScoreWithViterbiFretboardAndFingers(
            score = rawMockScore,
            tuningMidiHighToLow = openStrings,
            capo = capoFret
        )
        viewModel.currentScore.value = enrichedScore

        val enrichedMelodicNotes = enrichedScore.measures.flatMap { it.notes }.filter { !it.isRest }
        val viterbiSteps: List<Triple<Int, Int, Int>> = if (isFretboardInstrument && enrichedMelodicNotes.isNotEmpty()) {
            sequenceSteps.mapIndexed { idx, rawStep ->
                val solvedNote = enrichedMelodicNotes.getOrNull(idx)
                if (solvedNote != null && solvedNote.string != null && solvedNote.fret != null) {
                    Triple(solvedNote.string!!, solvedNote.fret!!, solvedNote.midiNote)
                } else {
                    rawStep
                }
            }
        } else {
            sequenceSteps
        }

        playbackJob = scope.launch {
            val localTriggeredNotes = mutableSetOf<Int>()
            try {
                runCatching { viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName) }

                if (activeSoundStyle == InstrumentSoundStyle.ALL_AT_ONCE && mode == 0) {
                    viewModel.scorePlayheadMeasure.value = 0
                    viewModel.scorePlayheadOffset.value = 0f
                    viewModel.midiActiveNotes.clear()
                    currentPlayingCoordinate = null
                    soundSequencePitches.forEach { midiPitch ->
                        viewModel.midiActiveNotes[midiPitch] = true
                        localTriggeredNotes.add(midiPitch)
                        currentlySoundingNotes.add(midiPitch)
                        runCatching { viewModel.audioEngine.noteOn(midiPitch, 0.85f) }
                    }
                    delay(1200L)
                    soundSequencePitches.forEach { midiPitch ->
                        currentlySoundingNotes.remove(midiPitch)
                        runCatching { viewModel.audioEngine.noteOff(midiPitch) }
                    }
                } else {
                    val stepDelay = if (activeSoundStyle == InstrumentSoundStyle.SEQUENTIAL) 270L else 310L
                    for (globalIdx in viterbiSteps.indices) {
                        if (!isActive || playbackGenerationId != myGenId) break
                        val step = viterbiSteps[globalIdx]
                        val midiPitch = step.third
                        val mIdx = globalIdx / notesPerBar
                        val idxInBar = globalIdx % notesPerBar

                        viewModel.scorePlayheadMeasure.value = mIdx
                        viewModel.scorePlayheadOffset.value = (idxInBar * 0.5f) / 4.0f

                        currentPlayingIdx = noteNames.indexOf(roots[(midiPitch % 12 + 12) % 12])
                        currentPlayingCoordinate = if (isFretboardInstrument) (step.first to step.second) else null
                        viewModel.midiPlayingNote.value = midiPitch
                        viewModel.midiActiveNotes.clear()
                        viewModel.midiActiveNotes[midiPitch] = true

                        localTriggeredNotes.add(midiPitch)
                        currentlySoundingNotes.add(midiPitch)
                        runCatching { viewModel.audioEngine.noteOn(midiPitch, 0.85f) }

                        delay(stepDelay)

                        currentlySoundingNotes.remove(midiPitch)
                        runCatching { viewModel.audioEngine.noteOff(midiPitch) }
                    }
                }
            } finally {
                // Always silence any notes triggered by THIS job
                localTriggeredNotes.forEach { midiPitch ->
                    currentlySoundingNotes.remove(midiPitch)
                    runCatching { viewModel.audioEngine.noteOff(midiPitch) }
                }
                // Only reset shared UI state if no newer playback job has superseded this one!
                if (playbackGenerationId == myGenId) {
                    currentPlayingIdx = -1
                    currentPlayingCoordinate = null
                    viewModel.midiPlayingNote.value = null
                    viewModel.scorePlayheadMeasure.value = 0
                    viewModel.scorePlayheadOffset.value = 0f
                    viewModel.midiActiveNotes.clear()
                    viewModel.midiActiveNotes.putAll(activeMidiMap)
                    isArpeggioRunning = false
                }
            }
        }
    }

    LaunchedEffect(
        activeMidiMap,
        isArpeggioRunning,
        safeRootIdx,
        chordTypeIdx,
        customExtendedSuffix,
        mode,
        selectedInstrument,
        safePianoVariationIdx,
        safePositionIndex,
        selectedCagedFilter,
        selectedFretRange,
        selectedStartFret,
        selectedScaleIdx
    ) {
        if (!isArpeggioRunning) {
            viewModel.midiActiveNotes.clear()
            viewModel.midiActiveNotes.putAll(activeMidiMap)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopActivePlaybackAndSilence()
            viewModel.midiActiveNotes.clear()
            viewModel.midiPlayingNote.value = null
        }
    }

    LaunchedEffect(selectedInstrument) {
        selectedVoicingType = when (selectedInstrument) {
            ChordInstrumentType.PIANO_ACOUSTIC, ChordInstrumentType.PIANO_ELECTRIC, ChordInstrumentType.HARMONIUM -> "Closed"
            ChordInstrumentType.UKULELE -> "Open"
            else -> "Open"
        }
        selectedPositionIndex = 0
        runCatching {
            getAudioEngine().setInstrument(selectedInstrument.soundEngineName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Optional Top Navigation Header when opened as a standalone gallery screen
        if (showTopBackBar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        stopActivePlaybackAndSilence()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "Chord & Voicing Dictionary Studio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Interactive Shapes, CAGED Boxes, Inversions & Notation",
                            fontSize = 11.sp,
                            color = Color(0xFF9EABB8)
                        )
                    }
                }
            }
        }

        // ====================================================================
        // ROW 1: INSTRUMENT SWITCHER | CHORDS vs SCALES | SEARCH & 3D LAB
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1.3f)) {
                pEQInstrumentDropdown(
                    selectedInstrument = selectedInstrument,
                    accentColor = partColor,
                    instrumentVolumes = viewModel.instrumentVolumes,
                    onInstrumentVolumeChange = { inst, vol ->
                        if (viewModel.selectedInstrument.value == inst.title) {
                            viewModel.audioEngine.setVolume(vol * viewModel.masterVolume.value)
                        }
                    },
                    onInstrumentChange = {
                        stopActivePlaybackAndSilence()
                        selectedInstrument = it
                    }
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.height(40.dp)) {
                SegmentedButton(
                    selected = mode == 0,
                    onClick = { stopActivePlaybackAndSilence(activeMidiMap); mode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Chords", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = mode == 1,
                    onClick = { stopActivePlaybackAndSilence(activeMidiMap); mode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Scales", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Toggle Search & Extended Suffix Filter Bar
            IconButton(
                onClick = { showSearchAndFilters = !showSearchAndFilters },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showSearchAndFilters) partColor.copy(alpha = 0.22f) else Color(0xFF161A22))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Qualities",
                    tint = if (showSearchAndFilters) partColor else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Open Fullscreen 3D Lab Button
            FilledTonalButton(
                onClick = {
                    stopActivePlaybackAndSilence(activeMidiMap)
                    isLabModeOpen = true
                },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = partColor.copy(alpha = 0.18f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Science, contentDescription = null, tint = partColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = partColor)
            }
        }

        // ====================================================================
        // COLLAPSIBLE SEARCH & EXTENDED CHORD QUALITY CATALOG BAR
        // ====================================================================
        AnimatedVisibility(visible = showSearchAndFilters && mode == 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF141821),
                border = BorderStroke(1.dp, Color(0xFF262D3D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search all chord suffixes (e.g. 7#9, m7b5, maj9, sus4, add9)...", fontSize = 12.sp, color = Color(0xFF6C788A)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = partColor, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(qualityCategories) { cat ->
                            val isSel = selectedQualityCategory == cat
                            pEQFilterChip(
                                selected = isSel,
                                onClick = { selectedQualityCategory = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    if (filteredDatabaseSuffixes.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredDatabaseSuffixes) { suf ->
                                val isSel = normalizedDbSuffix.equals(suf, ignoreCase = true)
                                pEQFilterChip(
                                    selected = isSel,
                                    onClick = {
                                        stopActivePlaybackAndSilence()
                                        selectedPositionIndex = 0
                                        val stdIdx = CHORD_TYPE_DEFS.indexOfFirst {
                                            normalizeDbSuffix(it.suffix).equals(suf, ignoreCase = true)
                                        }
                                        if (stdIdx >= 0) {
                                            chordTypeIdx = stdIdx
                                            customExtendedSuffix = null
                                        } else {
                                            customExtendedSuffix = suf
                                        }
                                    },
                                    label = { Text("$currentRootName$suf", fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // CONFIGURATION DROPDOWNS (Tailored to Active Instrument & Mode)
        // ====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(0.95f)) {
                    M3DropdownMenu(
                        label = if (selectedInstrument in listOf(ChordInstrumentType.SITAR, ChordInstrumentType.FLUTE, ChordInstrumentType.HARMONIUM, ChordInstrumentType.TABLA)) "Sa / Root" else "Root Key",
                        selectedText = currentRootName,
                        items = roots,
                        onItemSelect = {
                            stopActivePlaybackAndSilence()
                            rootIdx = it
                            selectedPositionIndex = 0
                        }
                    )
                }
                Box(modifier = Modifier.weight(1.55f)) {
                    if (mode == 0) {
                        val activeTypeLabel = customExtendedSuffix?.let { "Extended ($it)" } ?: typeDef.label
                        M3DropdownMenu(
                            label = "Chord Type",
                            selectedText = activeTypeLabel,
                            items = CHORD_TYPE_DEFS.map { it.label },
                            onItemSelect = {
                                stopActivePlaybackAndSilence()
                                chordTypeIdx = it
                                customExtendedSuffix = null
                                selectedPositionIndex = 0
                            }
                        )
                    } else {
                        M3DropdownMenu(
                            label = "Scale / Thaat / Raga",
                            selectedText = scalePair.first,
                            items = scaleFormulas.map { it.first },
                            onItemSelect = {
                                stopActivePlaybackAndSilence()
                                selectedScaleIdx = it
                            }
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
                            onItemSelect = {
                                stopActivePlaybackAndSilence()
                                selectedIntervalIdx = it
                            }
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
                            val invLabel = allPianoVariations.getOrNull(safePianoVariationIdx)?.first
                                ?: voicingOptions.getOrElse(safePianoVariationIdx) { "Root Position" }
                            val invMenuItems = if (allPianoVariations.isNotEmpty()) {
                                allPianoVariations.map { it.first }
                            } else voicingOptions
                            M3DropdownMenu(
                                label = "Inversion / Voicing",
                                selectedText = invLabel,
                                items = invMenuItems,
                                onItemSelect = { selectedVoicingIdx = it }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            M3DropdownMenu(
                                label = "Octave Span",
                                selectedText = octaveOptions[selectedOctaveSpan.coerceIn(0, octaveOptions.lastIndex)],
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
                    Box(modifier = Modifier.weight(1f)) {
                        M3DropdownMenu(
                            label = "Register Span",
                            selectedText = octaveOptions[selectedOctaveSpan.coerceIn(0, octaveOptions.lastIndex)],
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

            if (isGuitarFamily || is7String) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        M3DropdownMenu(
                            label = if (mode == 0) "CAGED Chord Shape" else "CAGED Scale Box",
                            selectedText = selectedCagedFilter,
                            items = cagedOptions,
                            onItemSelect = {
                                selectedCagedFilter = cagedOptions[it]
                                selectedPositionIndex = 0
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
                                onItemSelect = {
                                    selectedFretRange = fretRangeOptions[it]
                                    selectedPositionIndex = 0
                                }
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

        // ====================================================================
        // HERO SUMMARY CARD & AUDITION TRANSPORT
        // ====================================================================
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
                    val displayCapoText = if (capoFret > 0 && mode == 0 && !isKeyboardFamily) " (Capo $capoFret)" else ""
                    val variationSubtitle = when {
                        mode == 0 && isFretboardInstrument && allFretboardVariations.isNotEmpty() ->
                            "Shape ${safePositionIndex + 1} of ${allFretboardVariations.size} • Base Fret $effectiveGuitarBaseFret"
                        mode == 0 && isKeyboardFamily && allPianoVariations.isNotEmpty() ->
                            allPianoVariations[safePianoVariationIdx].first
                        else -> ""
                    }

                    Text(
                        text = if (mode == 0) "$chordDisplayName$displayCapoText" else "$currentRootName ${scalePair.first.split(" ")[0]}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isArpeggioRunning) Color(0xFFFFD54F) else partColor
                    )

                    if (variationSubtitle.isNotEmpty()) {
                        Text(
                            text = variationSubtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    Row(
                        Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        noteNames.forEachIndexed { idx, name ->
                            val isActiveStep = idx == currentPlayingIdx
                            val midiForBadge = sortedMidi.getOrNull(idx)
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = if (isActiveStep) FontWeight.Black else FontWeight.Bold,
                                color = if (isActiveStep) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color = if (isActiveStep) partColor else Color.Transparent)
                                    .clickable {
                                        midiForBadge?.let { auditionSingleMidiNote(it, selectedInstrument.soundEngineName) }
                                    }
                                    .padding(horizontal = if (isActiveStep) 6.dp else 3.dp, vertical = 2.dp)
                            )
                            if (idx < noteNames.lastIndex) {
                                Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                            }
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
                    onClick = {
                        if (isArpeggioRunning) {
                            stopActivePlaybackAndSilence(activeMidiMap)
                        } else {
                            launchAuditionSequence(
                                customVariationPos = selectedRealVariation,
                                customPianoNotes = if (isKeyboardFamily && mode == 0) allPianoVariations.getOrNull(safePianoVariationIdx)?.second else null
                            )
                        }
                    }
                )
            }
        }

        // ====================================================================
        // HERO INTERACTIVE INSTRUMENT VISUALIZER (Fretboard / Piano / Indian / Drums)
        // ====================================================================
        AnimatedContent(
            targetState = selectedInstrument,
            transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
            label = "instrument_view"
        ) { inst ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (inst) {
                    ChordInstrumentType.GUITAR_ACOUSTIC, ChordInstrumentType.GUITAR_ELECTRIC, ChordInstrumentType.GUITAR_CLASSICAL -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        val openGuitarStrings = listOf(64, 59, 55, 50, 45, 40)
                        val (fretMin, fretMax) = remember(selectedFretRange, selectedStartFret, mode) {
                            ChordVoicingEngine.resolveFretRangeBounds(selectedFretRange, selectedStartFret, mode == 1)
                        }

                        val highlightedGuitarCoordinate = remember(
                            currentPlayingCoordinate,
                            activeMidiToken,
                            effectiveGuitarFrets,
                            isArpeggioRunning,
                            mode,
                            fretMin,
                            fretMax
                        ) {
                            if (isArpeggioRunning) {
                                ChordVoicingEngine.resolveActiveFretCoordinate(
                                    activeMidiToken = activeMidiToken,
                                    currentPlayingCoordinate = currentPlayingCoordinate,
                                    openStrings = openGuitarStrings,
                                    chordFrets = effectiveGuitarFrets,
                                    fretMin = fretMin,
                                    fretMax = fretMax,
                                    isScaleMode = (mode == 1)
                                )
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
                                rootNote = safeRootIdx,
                                showNoteNames = when (badgeDisplayMode) {
                                    "Note Names" -> true
                                    "Fingers (1-5)", "Fingers (1-4)" -> false
                                    else -> (mode == 1)
                                },
                                markOctaves = true,
                                highlightOctaveC = true,
                                cagedMode = resolvedCaged,
                                fretMinConstrain = fretMin,
                                fretMaxConstrain = fretMax,
                                chordFrets = if (mode == 0) effectiveGuitarFrets else null,
                                chordFingers = if (mode == 0) effectiveGuitarFingers else null,
                                baseFret = if (mode == 0) effectiveGuitarBaseFret else 1,
                                barreFret = if (mode == 0) effectiveGuitarBarre else -1,
                                capoFret = capoFret,
                                onCapoChanged = { capoFret = it },
                                stringLabels = listOf("e", "B", "G", "D", "A", "E"),
                                openStringsSemis = listOf(4, 11, 7, 2, 9, 4),
                                activeCoordinate = highlightedGuitarCoordinate,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                activeNotes = pianoPitches,
                                onFretTapped = { _, _, midiNote ->
                                    auditionSingleMidiNote(midiNote, "GUITAR")
                                },
                                modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(12.dp))
                            )
                            GuitarFingerLegend(accentColor = partColor)
                        }
                    }
                    ChordInstrumentType.PIANO_ACOUSTIC, ChordInstrumentType.PIANO_ELECTRIC -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        val startNoteBoundary = ((pianoPitches.minOrNull() ?: 60) - 4).coerceAtLeast(36)
                        val endNoteBoundary = ((pianoPitches.maxOrNull() ?: 72) + (if (selectedOctaveSpan == 1) 16 else 8)).coerceAtMost(96)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            UnifiedPianoKeyboard(
                                activeMidiNotes = activeMidiMap,
                                accentColor = partColor,
                                rootNote = safeRootIdx,
                                fingering = pianoFingering,
                                activeCoordinate = if (isArpeggioRunning) activeMidiToken else null,
                                startNote = startNoteBoundary,
                                endNote = endNoteBoundary,
                                fillWidth = true,
                                showNoteNames = when (badgeDisplayMode) {
                                    "Note Names" -> true
                                    "Fingers (1-5)", "Fingers (1-4)" -> false
                                    else -> (mode == 1)
                                },
                                isPlaying = isArpeggioRunning,
                                onKeyTapped = { midiNote ->
                                    auditionSingleMidiNote(midiNote, "PIANO")
                                },
                                modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(8.dp))
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
                        val openUkuStrings = listOf(69, 64, 60, 67)
                        val highlightedUkuCoordinate = remember(currentPlayingCoordinate, activeMidiToken, effectiveUkeFrets, isArpeggioRunning, mode) {
                            if (isArpeggioRunning) {
                                ChordVoicingEngine.resolveActiveFretCoordinate(
                                    activeMidiToken = activeMidiToken,
                                    currentPlayingCoordinate = currentPlayingCoordinate,
                                    openStrings = openUkuStrings,
                                    chordFrets = effectiveUkeFrets,
                                    fretMin = 0,
                                    fretMax = 7,
                                    isScaleMode = (mode == 1)
                                )
                            } else null
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedGuitarFretboard(
                                numFrets = 7,
                                accentColor = partColor,
                                rootNote = safeRootIdx,
                                showNoteNames = when (badgeDisplayMode) {
                                    "Note Names" -> true
                                    "Fingers (1-5)", "Fingers (1-4)" -> false
                                    else -> (mode == 1)
                                },
                                markOctaves = true,
                                highlightOctaveC = true,
                                chordFrets = if (mode == 0) effectiveUkeFrets else null,
                                chordFingers = if (mode == 0) effectiveUkeFingers else null,
                                activeNotes = pianoPitches,
                                baseFret = if (mode == 0) effectiveUkeBaseFret else 1,
                                barreFret = if (mode == 0) effectiveUkeBarre else -1,
                                capoFret = capoFret,
                                onCapoChanged = { capoFret = it },
                                stringLabels = listOf("A", "E", "C", "G"),
                                openStringsSemis = listOf(9, 4, 0, 7),
                                activeCoordinate = highlightedUkuCoordinate,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                onFretTapped = { _, _, midiNote ->
                                    auditionSingleMidiNote(midiNote, "GUITAR")
                                },
                                modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(12.dp))
                            )
                            GuitarFingerLegend(accentColor = partColor)
                        }
                    }
                    ChordInstrumentType.GUITAR_7 -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        val open7Strings = listOf(64, 59, 55, 50, 45, 40, 35)
                        val (fretMin, fretMax) = remember(selectedFretRange, selectedStartFret, mode) {
                            ChordVoicingEngine.resolveFretRangeBounds(selectedFretRange, selectedStartFret, mode == 1)
                        }

                        val padded7Frets = remember(effectiveGuitarFrets) {
                            if (effectiveGuitarFrets.size == 6) effectiveGuitarFrets + listOf(-1) else effectiveGuitarFrets
                        }
                        val padded7Fingers = remember(effectiveGuitarFingers) {
                            if (effectiveGuitarFingers.size == 6) effectiveGuitarFingers + listOf(0) else effectiveGuitarFingers
                        }

                        val highlighted7Coordinate = remember(currentPlayingCoordinate, activeMidiToken, isArpeggioRunning, mode, fretMin, fretMax, padded7Frets) {
                            if (isArpeggioRunning) {
                                ChordVoicingEngine.resolveActiveFretCoordinate(
                                    activeMidiToken = activeMidiToken,
                                    currentPlayingCoordinate = currentPlayingCoordinate,
                                    openStrings = open7Strings,
                                    chordFrets = padded7Frets,
                                    fretMin = fretMin,
                                    fretMax = fretMax,
                                    isScaleMode = (mode == 1)
                                )
                            } else null
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnifiedGuitarFretboard(
                                numFrets = 15,
                                accentColor = partColor,
                                rootNote = safeRootIdx,
                                showNoteNames = when (badgeDisplayMode) {
                                    "Note Names" -> true
                                    "Fingers (1-5)", "Fingers (1-4)" -> false
                                    else -> (mode == 1)
                                },
                                markOctaves = true,
                                highlightOctaveC = true,
                                fretMinConstrain = fretMin,
                                fretMaxConstrain = fretMax,
                                chordFrets = if (mode == 0) padded7Frets else null,
                                chordFingers = if (mode == 0) padded7Fingers else null,
                                baseFret = if (mode == 0) effectiveGuitarBaseFret else 1,
                                barreFret = if (mode == 0) effectiveGuitarBarre else -1,
                                stringLabels = listOf("e", "B", "G", "D", "A", "E", "B"),
                                openStringsSemis = listOf(4, 11, 7, 2, 9, 4, 11),
                                activeCoordinate = highlighted7Coordinate,
                                activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                                activeNotes = pianoPitches,
                                onFretTapped = { _, _, midiNote ->
                                    auditionSingleMidiNote(midiNote, "GUITAR")
                                },
                                modifier = Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(12.dp))
                            )
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
                                    auditionSingleMidiNote(midiNote, "HARMONIUM")
                                },
                                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp))
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
                        UnifiedSitarVisualizer(
                            activeNotes = pianoPitches,
                            activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                            accentColor = partColor,
                            showNoteNames = when (badgeDisplayMode) {
                                "Note Names" -> true
                                "Fingers (1-5)", "Fingers (1-4)" -> false
                                else -> (mode == 1)
                            },
                            onFretTapped = { _, midiNote ->
                                auditionSingleMidiNote(midiNote, selectedInstrument.soundEngineName)
                            },
                            modifier = Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(12.dp))
                        )
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
                                },
                                onPositionTapped = { _, midiNote ->
                                    auditionSingleMidiNote(midiNote, "VIOLIN")
                                },
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                            )
                            GuitarFingerLegend(accentColor = partColor)
                        }
                    }
                    ChordInstrumentType.FLUTE -> {
                        val activeMidiToken = viewModel.midiPlayingNote.value
                        UnifiedFluteVisualizer(
                            activeNotes = pianoPitches,
                            activeMidiNote = if (isArpeggioRunning) activeMidiToken else null,
                            accentColor = partColor,
                            onToneHoleTapped = { _, midiNote ->
                                auditionSingleMidiNote(midiNote, "FLUTE")
                            },
                            modifier = Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                    ChordInstrumentType.DRUM_KIT -> {
                        ThreeDDrumVisualizer(
                            viewModel = viewModel,
                            primaryColor = partColor,
                            currentKitLabels = viewModel.drumPadLabels,
                            showOnlyPartLabel = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        // ====================================================================
        // INTEGRATED VOICINGS, CAGED SHAPES & INVERSIONS GALLERY (Merged from ChordDisplayGalleryScreen)
        // ====================================================================
        if (mode == 0 && (isFretboardInstrument || isKeyboardFamily)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF141821),
                border = BorderStroke(1.dp, Color(0xFF252C3B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headerTitle = if (isKeyboardFamily) {
                            "PIANO VOICINGS, INVERSIONS & TONAL STRUCTURE (${allPianoVariations.size})"
                        } else {
                            "ALL CHORD SHAPES & VOICINGS (${allFretboardVariations.size} variations)"
                        }
                        Text(
                            text = headerTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = partColor,
                            letterSpacing = 0.8.sp
                        )

                        if (isFretboardInstrument && allFretboardVariations.size > 3) {
                            TextButton(
                                onClick = { showAllVariationsGrid = !showAllVariationsGrid },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(
                                    imageVector = if (showAllVariationsGrid) Icons.Default.ViewCarousel else Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = partColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (showAllVariationsGrid) "Carousel" else "Expand Grid",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = partColor
                                )
                            }
                        }
                    }

                    if (isKeyboardFamily) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            itemsIndexed(allPianoVariations) { idx, (invLabel, notes) ->
                                val isSelected = safePianoVariationIdx == idx
                                SvgPianoChordBox(
                                    midiNotes = notes,
                                    chordName = chordDisplayName,
                                    inversionLabel = invLabel,
                                    accentColor = if (isSelected) Color(0xFFFF9E1B) else Color(0xFF81C784),
                                    cardBackground = if (isSelected) Color(0xFF222938) else Color(0xFF161A22),
                                    onClick = {
                                        selectedVoicingIdx = idx
                                        launchAuditionSequence(customPianoNotes = notes)
                                    }
                                )
                            }
                        }

                        // Tonal Structure & Interval Cards (Tap any tone card to audition its exact MIDI pitch)
                        val activeVariationNotes = allPianoVariations.getOrNull(safePianoVariationIdx)?.second ?: pianoPitches
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeVariationNotes.forEachIndexed { i, midi ->
                                val semi = (midi % 12 + 12) % 12
                                val relSemi = ((semi - safeRootIdx) % 12 + 12) % 12
                                val noteLabel = roots[semi]
                                val intervalTag = dbPianoChord?.intervals?.getOrNull(i) ?: DEGREE_LABELS[relSemi]

                                Surface(
                                    modifier = Modifier
                                        .widthIn(min = 74.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            auditionSingleMidiNote(midi, selectedInstrument.soundEngineName)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1A202C),
                                    border = BorderStroke(1.dp, if (i == 0) partColor.copy(alpha = 0.6f) else Color(0xFF2B3446))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = noteLabel,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (i == 0) partColor else Color.White
                                        )
                                        Text(
                                            text = intervalTag,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (i == 0) partColor else Color(0xFF9EABB8)
                                        )
                                        Text(
                                            text = "MIDI $midi",
                                            fontSize = 9.sp,
                                            color = Color(0xFF6C788A)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val instrumentKey = if (isUkulele) "ukulele" else "guitar"
                        if (allFretboardVariations.isEmpty()) {
                            Text(
                                text = "Showing procedural voicing for $chordDisplayName",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        } else if (showAllVariationsGrid) {
                            // Expanded 2-Row / Multi-Column Wrap Grid of all Chord Shapes
                            val chunkedRows = remember(allFretboardVariations) { allFretboardVariations.chunked(3) }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                chunkedRows.forEachIndexed { rowIdx, rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEachIndexed { colIdx, pos ->
                                            val globalIdx = rowIdx * 3 + colIdx
                                            val isSelected = globalIdx == safePositionIndex
                                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                                SvgChordBox(
                                                    chord = pos,
                                                    chordName = "$chordDisplayName #${globalIdx + 1}",
                                                    instrument = instrumentKey,
                                                    accentColor = if (isSelected) Color(0xFFFF9E1B) else Color(0xFF81C784),
                                                    cardBackground = if (isSelected) Color(0xFF222938) else Color(0xFF161A22),
                                                    onClick = {
                                                        selectedPositionIndex = globalIdx
                                                        launchAuditionSequence(customVariationPos = pos)
                                                    }
                                                )
                                            }
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                itemsIndexed(allFretboardVariations) { idx, pos ->
                                    val isSelected = idx == safePositionIndex
                                    SvgChordBox(
                                        chord = pos,
                                        chordName = "$chordDisplayName #${idx + 1}",
                                        instrument = instrumentKey,
                                        accentColor = if (isSelected) Color(0xFFFF9E1B) else Color(0xFF81C784),
                                        cardBackground = if (isSelected) Color(0xFF222938) else Color(0xFF161A22),
                                        onClick = {
                                            selectedPositionIndex = idx
                                            launchAuditionSequence(customVariationPos = pos)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // NOTATION FORMAT SWITCHER (Both | Staff | Tab) + QUICK STUDIO DRAWERS
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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

            // Collapsible Quick Studio Tools (Capo, Transpose, Substitutions)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isFretboardInstrument) {
                    AssistChip(
                        onClick = { activeBottomDrawer = if (activeBottomDrawer == "CAPO") null else "CAPO" },
                        label = {
                            Text(
                                if (capoFret > 0) "Capo $capoFret" else "Capo",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeBottomDrawer == "CAPO" || capoFret > 0) partColor else Color.White
                            )
                        }
                    )
                }
                AssistChip(
                    onClick = { activeBottomDrawer = if (activeBottomDrawer == "TRANSPOSE") null else "TRANSPOSE" },
                    label = {
                        Text(
                            "Transpose",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeBottomDrawer == "TRANSPOSE") partColor else Color.White
                        )
                    }
                )
                AssistChip(
                    onClick = { activeBottomDrawer = if (activeBottomDrawer == "SUBS") null else "SUBS" },
                    label = {
                        Text(
                            "Cadences",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeBottomDrawer == "SUBS") partColor else Color.White
                        )
                    }
                )
            }
        }

        // Expandable Studio Drawer Content
        AnimatedVisibility(visible = activeBottomDrawer != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF141821),
                border = BorderStroke(1.dp, partColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    when (activeBottomDrawer) {
                        "CAPO" -> CapoSection(
                            capoFret = capoFret,
                            activeRootIdx = safeRootIdx,
                            onCapoChange = {
                                stopActivePlaybackAndSilence()
                                capoFret = it
                            },
                            partColor = partColor,
                            onKeyDispatched = {
                                stopActivePlaybackAndSilence()
                                rootIdx = it
                            }
                        )
                        "TRANSPOSE" -> TransposeSection(
                            fromKeyIdx = fromKeyIdx,
                            toKeyIdx = toKeyIdx,
                            selectedScaleIdx = selectedScaleIdx,
                            scaleFormulas = scaleFormulas,
                            partColor = partColor,
                            onFromChange = {
                                stopActivePlaybackAndSilence()
                                fromKeyIdx = it
                            },
                            onToChange = {
                                stopActivePlaybackAndSilence()
                                toKeyIdx = it
                            },
                            onKeyDispatched = {
                                stopActivePlaybackAndSilence()
                                rootIdx = it
                            }
                        )
                        "SUBS" -> EasyChordQuickRef(
                            rootIdx = safeRootIdx,
                            qualityIdx = chordTypeIdx,
                            cadences = cadenceList,
                            accentColor = partColor,
                            onSelectChord = { label ->
                                stopActivePlaybackAndSilence()
                                val cleanLabel = label.substringBefore(" ")
                                    .replace("m", "")
                                    .replace("7", "")
                                    .replace("dim", "")
                                    .replace("aug", "")
                                val targetIdx = roots.indexOf(cleanLabel)
                                if (targetIdx >= 0) {
                                    rootIdx = targetIdx
                                    customExtendedSuffix = null
                                    chordTypeIdx = when {
                                        label.contains("dim") -> 5
                                        label.contains("aug") -> 6
                                        label.contains("m") -> 1
                                        else -> 0
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ====================================================================
        // STAFF & TAB NOTATION VIEWER
        // ====================================================================
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
            fallbackTitle = viewModel.currentMediaItem.value?.title
                ?: viewModel.selectedMixerItem.value?.displayName
                ?: "",
            scrollState = scrollState
        )

        Spacer(Modifier.height(16.dp))
    }

    if (isLabModeOpen) {
        ImmersiveKeyboardFretboardLabDialog(
            viewModel = viewModel,
            rootIdx = safeRootIdx,
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
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        fingering.fingers.forEachIndexed { idx, fingerNum ->
            val noteLabel = noteNames.getOrNull(idx) ?: ""
            val structuralLabelName = fingerNameLabels.getOrNull(fingerNum - 1) ?: "Finger"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$fingerNum",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp,
                        style = TextStyle(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
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
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = n,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 11.sp,
                        style = TextStyle(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }
                Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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