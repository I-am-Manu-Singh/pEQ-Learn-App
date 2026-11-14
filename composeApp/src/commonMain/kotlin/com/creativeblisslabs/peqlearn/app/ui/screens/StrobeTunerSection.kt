package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.security.getPermissionManager
import com.creativeblisslabs.peqlearn.app.tuner.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val TUNER_PITCH_CLASSES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private fun noteNameToMidiAndHz(noteStr: String, a4ReferenceHz: Float = 440f): Pair<Int, Float> {
    val clean = noteStr.trim()
        .replace("♯", "#")
        .replace("♭", "b")
    if (clean.isEmpty() || clean == "--") return 69 to a4ReferenceHz

    val pitchPart = clean.dropLastWhile { it.isDigit() || it == '-' }
    val octPart = clean.takeLastWhile { it.isDigit() || it == '-' }.toIntOrNull() ?: 4

    val pc = when (pitchPart) {
        "C" -> 0
        "C#", "Db" -> 1
        "D" -> 2
        "D#", "Eb" -> 3
        "E" -> 4
        "F" -> 5
        "F#", "Gb" -> 6
        "G" -> 7
        "G#", "Ab" -> 8
        "A" -> 9
        "A#", "Bb" -> 10
        "B" -> 11
        else -> 0
    }
    val midi = ((octPart + 1) * 12 + pc).coerceIn(12, 120)
    val hz = (a4ReferenceHz * 2.0.pow((midi - 69) / 12.0)).toFloat()
    return midi to hz
}

private fun instrumentProgramForName(instrument: String): String {
    val low = instrument.lowercase()
    return when {
        low.contains("violin") -> "VIOLIN"
        low.contains("sitar") || low.contains("tanpura") -> "SITAR"
        low.contains("ukulele") -> "GUITAR"
        low.contains("bass") -> "GUITAR"
        else -> "GUITAR"
    }
}

/**
 * Generates instrument-specific, peg-side-aware mechanical instructions so the player knows
 * whether to turn the machine head / peg clockwise or counter-clockwise, or slide the Manka bead.
 */
private fun buildMechanicalPegReminder(
    instrument: String,
    stringIdx: Int,
    numStrings: Int,
    centsDeviation: Int,
    hasSignal: Boolean
): String {
    if (!hasSignal) {
        return "Pluck string cleanly near the soundhole/pickup and let it ring."
    }
    if (abs(centsDeviation) <= 3) {
        return "Pitch is locked within ±3¢ concert tolerance. Move to next string."
    }

    val instLow = instrument.lowercase()
    val needTighten = centsDeviation < 0
    val physLeftToRightIdx = (numStrings - 1 - stringIdx).coerceIn(0, numStrings - 1)
    val isLeftSidePeg = physLeftToRightIdx < (numStrings + 1) / 2

    return when {
        instLow.contains("tanpura") || instLow.contains("sitar") -> {
            if (abs(centsDeviation) <= 18) {
                if (needTighten) {
                    "Fine Tune: Slide the swan Manka bead DOWN toward the Ghoraj bridge to raise pitch."
                } else {
                    "Fine Tune: Slide the swan Manka bead UP toward the Khunti pegs to lower pitch."
                }
            } else {
                if (needTighten) {
                    "Coarse Tune: Twist wooden Khunti peg inward & tighten slowly while pressing it into the pegbox."
                } else {
                    "Coarse Tune: Ease wooden Khunti peg slightly counter-clockwise, then re-tighten up to pitch."
                }
            }
        }
        instLow.contains("violin") -> {
            if (abs(centsDeviation) <= 15) {
                if (needTighten) "Fine Tuner: Turn tailpiece fine-tuner screw CLOCKWISE ↻ to sharpen."
                else "Fine Tuner: Turn tailpiece fine-tuner screw COUNTER-CLOCKWISE ↺ to flatten."
            } else {
                if (needTighten) "Pegbox: Push ebony peg inward firmly while turning away from you to tighten."
                else "Pegbox: Loosen peg slightly below target pitch, then tune UP to lock friction."
            }
        }
        else -> {
            val sideLabel = if (isLeftSidePeg) "Left-Side Peg" else "Right-Side Peg"
            val turnDir = if (needTighten) {
                if (isLeftSidePeg) "COUNTER-CLOCKWISE ↺ (away from you)" else "CLOCKWISE ↻ (away from you)"
            } else {
                if (isLeftSidePeg) "CLOCKWISE ↻ (toward you)" else "COUNTER-CLOCKWISE ↺ (toward you)"
            }
            val tip = if (!needTighten) "Pro Tip: Loosen slightly below pitch, then tighten up so gears stay locked." else ""
            "$sideLabel: Turn knob $turnDir to ${if (needTighten) "tighten string" else "loosen string"}. $tip".trim()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrobeTunerSection(viewModel: AppViewModel) {
    val permissionManager = remember { getPermissionManager() }
    val tunerEngine = remember { getTunerEngine() }

    // Top-level studio mode: "TUNER" vs "TANPURA_STUDIO"
    var activeStudioMode by remember { mutableStateOf("TUNER") }

    var isListening by remember { mutableStateOf(tunerEngine.isListening()) }
    var showPermissionAlert by remember { mutableStateOf(false) }

    // Extended Indian & World Instrument Presets
    val extraIndianTunings = remember {
        listOf(
            InstrumentTuning(
                id = "tanpura_pa_c_sharp",
                name = "Tanpura • Pa-Sa-Sa-Sa (C#)",
                instrument = "Tanpura",
                stringNotes = listOf("G#3", "C#4", "C#4", "C#3")
            ),
            InstrumentTuning(
                id = "tanpura_ma_c_sharp",
                name = "Tanpura • Ma-Sa-Sa-Sa (C#)",
                instrument = "Tanpura",
                stringNotes = listOf("F#3", "C#4", "C#4", "C#3")
            ),
            InstrumentTuning(
                id = "tanpura_ni_c_sharp",
                name = "Tanpura • Ni-Sa-Sa-Sa (C#)",
                instrument = "Tanpura",
                stringNotes = listOf("C4", "C#4", "C#4", "C#3")
            ),
            InstrumentTuning(
                id = "tanpura_pa_c",
                name = "Tanpura • Pa-Sa-Sa-Sa (C)",
                instrument = "Tanpura",
                stringNotes = listOf("G3", "C4", "C4", "C3")
            ),
            InstrumentTuning(
                id = "tanpura_pa_g",
                name = "Tanpura • Male Vocal (G)",
                instrument = "Tanpura",
                stringNotes = listOf("D3", "G3", "G3", "G2")
            ),
            InstrumentTuning(
                id = "tanpura_5str_c_sharp",
                name = "Tanpura 5-String • Pa-Ni-Sa-Sa-Sa (C#)",
                instrument = "Tanpura",
                stringNotes = listOf("G#3", "C4", "C#4", "C#4", "C#3")
            ),
            InstrumentTuning(
                id = "sitar_pancham_c_sharp",
                name = "Sitar • Main Strings (C#)",
                instrument = "Sitar",
                stringNotes = listOf("F#3", "C#3", "G#2", "C#2", "G#3", "C#4", "C#5")
            )
        )
    }

    val customTunings = remember { mutableStateListOf<InstrumentTuning>() }
    val allAvailableTunings = remember(customTunings.size) {
        val base = TuningsCatalog.BUILT_IN_TUNINGS
        val existingIds = base.map { it.id }.toSet()
        base + extraIndianTunings.filter { it.id !in existingIds } + customTunings
    }

    // Tuner States
    var isAutoMode by remember { mutableStateOf(true) }
    var selectedTuning by remember { mutableStateOf(allAvailableTunings.first()) }
    var selectedStringIdx by remember { mutableIntStateOf(0) }
    var isChromaticMode by remember { mutableStateOf(false) }
    var a4ReferenceHz by remember { mutableFloatStateOf(440f) }

    // Per-string locked status memory for the current tuning
    val lockedStringsSet = remember(selectedTuning.id) { mutableStateListOf<Int>() }

    // Live Mic Pitch State
    var detectedFreq by remember { mutableFloatStateOf(0f) }
    var detectedNote by remember { mutableStateOf("--") }
    var rawChromaticCents by remember { mutableIntStateOf(0) }
    var micAmplitude by remember { mutableFloatStateOf(0f) }
    var inTuneLockFrames by remember { mutableIntStateOf(0) }

    // Dialogs
    var showTuningDialog by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val favoriteTuningIds = remember {
        mutableStateListOf(
            "guitar_standard",
            "guitar_drop_d",
            "bass_standard",
            "ukulele_standard",
            "tanpura_pa_c_sharp"
        )
    }

    // Tuner Settings Options
    var stringLayout by remember { mutableStateOf("Side by side") } // "Inline" vs "Side by side"
    var tuningDisplayType by remember { mutableStateOf("Cents") } // "Simple", "Semitones", "Cents"
    var playSoundOnStringSelect by remember { mutableStateOf(true) }
    var playInTuneSound by remember { mutableStateOf(false) }

    val safeSelectedStringIdx = selectedStringIdx.coerceIn(
        0,
        (selectedTuning.stringNotes.size - 1).coerceAtLeast(0)
    )

    fun playReferenceTone(noteStr: String) {
        val (targetMidi, _) = noteNameToMidiAndHz(noteStr, a4ReferenceHz)
        runCatching {
            viewModel.audioEngine.setInstrument(instrumentProgramForName(selectedTuning.instrument))
            viewModel.audioEngine.noteOn(targetMidi, 0.88f)
        }
    }

    fun startMicEngine() {
        if (!permissionManager.hasMicrophonePermission()) {
            permissionManager.requestMicrophonePermission()
            showPermissionAlert = true
            return
        }
        tunerEngine.startListening { freq, note, cents, amp ->
            if (!freq.isFinite() || freq <= 0f) {
                detectedFreq = 0f
                detectedNote = "--"
                rawChromaticCents = 0
                micAmplitude = (amp.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f)
                return@startListening
            }

            detectedFreq = freq
            detectedNote = note.ifBlank { "--" }
            rawChromaticCents = cents.coerceIn(-100, 100)
            micAmplitude = (amp.takeIf { it.isFinite() } ?: 0f).coerceIn(0f, 1f)

            if (isAutoMode && !isChromaticMode && selectedTuning.stringNotes.isNotEmpty()) {
                var bestIdx = selectedStringIdx.coerceIn(0, selectedTuning.stringNotes.lastIndex)
                var minAbsCents = Double.MAX_VALUE
                selectedTuning.stringNotes.forEachIndexed { idx, sNote ->
                    val (_, targetHz) = noteNameToMidiAndHz(sNote, a4ReferenceHz)
                    val centsDiff = abs(1200.0 * log2(freq.toDouble() / targetHz.toDouble()))
                    if (centsDiff < minAbsCents) {
                        minAbsCents = centsDiff
                        bestIdx = idx
                    }
                }
                if (minAbsCents <= 700.0) {
                    selectedStringIdx = bestIdx
                }
            }
        }
        isListening = tunerEngine.isListening()
    }

    fun toggleListening() {
        if (isListening) {
            tunerEngine.stopListening()
            isListening = false
            detectedFreq = 0f
            detectedNote = "--"
            rawChromaticCents = 0
            micAmplitude = 0f
        } else {
            startMicEngine()
        }
    }

    LaunchedEffect(Unit) {
        if (permissionManager.hasMicrophonePermission() && !tunerEngine.isListening()) {
            startMicEngine()
        }
    }

    DisposableEffect(tunerEngine) {
        onDispose {
            tunerEngine.stopListening()
        }
    }

    val currentTargetNote = when {
        isChromaticMode -> detectedNote
        selectedTuning.stringNotes.isEmpty() -> "--"
        else -> selectedTuning.stringNotes[safeSelectedStringIdx]
    }

    val (_, targetFreqHz) = remember(currentTargetNote, a4ReferenceHz) {
        noteNameToMidiAndHz(currentTargetNote, a4ReferenceHz)
    }

    val effectiveCentsDeviation = remember(detectedFreq, isChromaticMode, rawChromaticCents, targetFreqHz) {
        if (detectedFreq <= 0f) {
            0
        } else if (isChromaticMode || currentTargetNote == "--") {
            rawChromaticCents
        } else {
            val exactCents = 1200.0 * log2(detectedFreq.toDouble() / targetFreqHz.toDouble())
            exactCents.roundToInt().coerceIn(-500, 500)
        }
    }

    val animatedCents by animateFloatAsState(
        targetValue = if (detectedFreq > 0f) effectiveCentsDeviation.toFloat().coerceIn(-60f, 60f) else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
        label = "tuner_needle_cents"
    )

    val strobePhase = remember { Animatable(0f) }
    LaunchedEffect(isListening) {
        while (isListening) {
            withFrameNanos {
                val activeSignal = detectedFreq > 0f
                val driftVelocity = if (!activeSignal) {
                    0f
                } else if (abs(effectiveCentsDeviation) <= 2) {
                    0f
                } else {
                    (effectiveCentsDeviation.coerceIn(-50, 50) / 50f) * 0.055f
                }
                if (driftVelocity != 0f) {
                    val next = (strobePhase.value + driftVelocity) % 1f
                    kotlinx.coroutines.runBlocking {
                        strobePhase.snapTo(if (next < 0f) next + 1f else next)
                    }
                }
            }
        }
    }

    val inTuneColor = Color(0xFF00E676)
    val tuneUpColor = Color(0xFFFFAB00)   // Flat -> Tighten string
    val tuneDownColor = Color(0xFF29B6F6) // Sharp -> Loosen string
    val dangerSharpColor = Color(0xFFFF5252)

    val hasValidSignal = detectedFreq > 0f
    val isInTune = hasValidSignal && abs(effectiveCentsDeviation) <= 3

    LaunchedEffect(isInTune, safeSelectedStringIdx) {
        if (isInTune) {
            inTuneLockFrames++
            if (inTuneLockFrames >= 8 && !lockedStringsSet.contains(safeSelectedStringIdx)) {
                lockedStringsSet.add(safeSelectedStringIdx)
            }
            if (inTuneLockFrames == 10 && playInTuneSound) {
                playReferenceTone(currentTargetNote)
            }
        } else {
            inTuneLockFrames = 0
        }
    }

    val gaugeColor = when {
        !hasValidSignal -> Color.White.copy(alpha = 0.42f)
        isInTune -> inTuneColor
        effectiveCentsDeviation > 45 -> dangerSharpColor
        effectiveCentsDeviation < -3 -> tuneUpColor
        else -> tuneDownColor
    }

    val isTanpuraOrSitar = selectedTuning.instrument.contains("Tanpura", true) ||
        selectedTuning.instrument.contains("Sitar", true)

    val primaryGuidanceBanner = when {
        !isListening -> "MIC OFF • TAP MIC OR PLUCK STRING"
        !hasValidSignal -> "LISTENING • PLUCK ${if (isChromaticMode) "ANY STRING" else "STRING ${safeSelectedStringIdx + 1} ($currentTargetNote)"}"
        isInTune -> "✓ PERFECTLY IN TUNE • STRING ${safeSelectedStringIdx + 1} LOCKED"
        effectiveCentsDeviation < -35 -> "TOO FLAT • TIGHTEN PEG ↻ (+${abs(effectiveCentsDeviation)}¢)"
        effectiveCentsDeviation < -10 -> "FLAT • TIGHTEN PEG SLIGHTLY ↻"
        effectiveCentsDeviation < -3 -> if (isTanpuraOrSitar) {
            "SLIGHTLY FLAT • SLIDE MANKA BEAD DOWN ↓ OR TIGHTEN PEG ↻"
        } else {
            "ALMOST THERE • MICRO-TIGHTEN PEG ↻"
        }
        effectiveCentsDeviation > 45 -> "⚠ DANGER OVER-TENSION • LOOSEN PEG IMMEDIATELY ↺"
        effectiveCentsDeviation > 10 -> "SHARP • LOOSEN PEG SLIGHTLY ↺"
        else -> if (isTanpuraOrSitar) {
            "SLIGHTLY SHARP • SLIDE MANKA BEAD UP ↑ OR LOOSEN PEG ↺"
        } else {
            "ALMOST THERE • MICRO-LOOSEN PEG ↺"
        }
    }

    val detailedMechanicalReminder = remember(
        selectedTuning.instrument,
        safeSelectedStringIdx,
        selectedTuning.stringNotes.size,
        effectiveCentsDeviation,
        hasValidSignal
    ) {
        buildMechanicalPegReminder(
            instrument = selectedTuning.instrument,
            stringIdx = safeSelectedStringIdx,
            numStrings = selectedTuning.stringNotes.size,
            centsDeviation = effectiveCentsDeviation,
            hasSignal = hasValidSignal
        )
    }

    val quarterTurnsEstimate = remember(effectiveCentsDeviation, hasValidSignal) {
        if (!hasValidSignal || abs(effectiveCentsDeviation) <= 3) ""
        else {
            val turns = (abs(effectiveCentsDeviation) / 28f).coerceIn(0.05f, 3.5f)
            if (turns < 0.2f) "Nudge peg ~1/16 turn"
            else "Turn peg ~%.1f quarter-turn%s".format(turns, if (turns >= 1.15f) "s" else "")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0B130E),
        border = BorderStroke(1.2.dp, if (isInTune) inTuneColor.copy(alpha = 0.7f) else Color(0xFF1E3824))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ====================================================================
            // 0. MODE SWITCHER: [STROBE TUNER] <-> [TANPURA DRONE STUDIO]
            // ====================================================================
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeStudioMode == "TUNER",
                    onClick = { activeStudioMode = "TUNER" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFF173320),
                        activeContentColor = inTuneColor,
                        inactiveContainerColor = Color(0xFF0E1A12),
                        inactiveContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Precision Strobe Tuner", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = activeStudioMode == "TANPURA_STUDIO",
                    onClick = { activeStudioMode = "TANPURA_STUDIO" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFF33220E),
                        activeContentColor = Color(0xFFFFD54F),
                        inactiveContainerColor = Color(0xFF0E1A12),
                        inactiveContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tanpura & Shruti Drone", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeStudioMode == "TANPURA_STUDIO") {
                TanpuraDroneStudioPanel(
                    viewModel = viewModel,
                    a4ReferenceHz = a4ReferenceHz,
                    onOpenTanpuraTuner = { tanpuraPresetId ->
                        allAvailableTunings.firstOrNull { it.id == tanpuraPresetId }?.let {
                            selectedTuning = it
                            selectedStringIdx = 0
                            isChromaticMode = false
                        }
                        activeStudioMode = "TUNER"
                    }
                )
            } else {
                // ====================================================================
                // 1. TOP BAR: TUNER TITLE, LIVE MIC SENSITIVITY BAR & QUICK ACTIONS
                // ====================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isChromaticMode) "Chromatic Strobe" else "${selectedTuning.instrument} Tuner",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = inTuneColor
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isListening) Color(0xFF14281B) else Color(0xFF281818),
                            border = BorderStroke(1.dp, if (isListening) inTuneColor.copy(alpha = 0.6f) else Color.Red.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { toggleListening() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isListening) inTuneColor else Color.Red)
                                )
                                Text(
                                    text = if (isListening) "MIC LIVE" else "MIC OFF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF142418),
                            border = BorderStroke(1.dp, Color(0xFF1E3824)),
                            modifier = Modifier.clickable {
                                a4ReferenceHz = when (a4ReferenceHz.roundToInt()) {
                                    440 -> 442f
                                    442 -> 432f
                                    else -> 440f
                                }
                            }
                        ) {
                            Text(
                                text = "A4=${a4ReferenceHz.roundToInt()}Hz",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF80CBC4),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { isChromaticMode = !isChromaticMode }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = if (isChromaticMode) Icons.Default.MusicNote else Icons.Outlined.MusicNote,
                                contentDescription = "Chromatic Toggle",
                                tint = if (isChromaticMode) inTuneColor else Color.White.copy(0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(onClick = { toggleListening() }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Mic Toggle",
                                tint = if (isListening) inTuneColor else Color(0xFFFF5252),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Tuner Settings",
                                tint = Color.White.copy(0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // ====================================================================
                // 2. MOVING PHASE-STROBE BAND + PRECISION NEEDLE ARC GAUGE
                // ====================================================================
                TunerPhaseStrobeStrip(
                    strobePhase = strobePhase.value,
                    centsDeviation = effectiveCentsDeviation,
                    hasSignal = hasValidSignal,
                    gaugeColor = gaugeColor,
                    micAmplitude = micAmplitude
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TunerArcGauge(
                        animatedCents = animatedCents,
                        hasSignal = hasValidSignal,
                        isInTune = isInTune,
                        gaugeColor = gaugeColor
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        val deviationText = when {
                            !hasValidSignal -> currentTargetNote
                            tuningDisplayType == "Simple" -> currentTargetNote
                            tuningDisplayType == "Semitones" -> {
                                val st = effectiveCentsDeviation / 100f
                                if (st > 0f) "+%.1f st".format(st) else "%.1f st".format(st)
                            }
                            else -> if (effectiveCentsDeviation > 0) "+$effectiveCentsDeviation ¢" else "$effectiveCentsDeviation ¢"
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentTargetNote,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = if (hasValidSignal) gaugeColor else Color.White
                            )
                            if (hasValidSignal && tuningDisplayType != "Simple") {
                                Text(
                                    text = deviationText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = gaugeColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (hasValidSignal) {
                                "Detected: %.1f Hz  •  Target: %.1f Hz (%s)".format(
                                    detectedFreq,
                                    targetFreqHz,
                                    detectedNote
                                )
                            } else {
                                "Target Pitch: $currentTargetNote (%.1f Hz)".format(targetFreqHz)
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }

                // ====================================================================
                // 3. TIGHTEN / LOOSEN PEG ACTION BANNER + MECHANICAL COACHING
                // ====================================================================
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = gaugeColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, gaugeColor.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = primaryGuidanceBanner,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (hasValidSignal) gaugeColor else Color.White.copy(alpha = 0.85f)
                                )
                                if (quarterTurnsEstimate.isNotEmpty()) {
                                    Text(
                                        text = quarterTurnsEstimate,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = gaugeColor.copy(alpha = 0.90f)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { playReferenceTone(currentTargetNote) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF183020),
                                    contentColor = inTuneColor
                                )
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Play Reference Tone", modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("TONE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Instrument-specific peg direction & Manka fine-tuning reminder
                        Text(
                            text = detailedMechanicalReminder,
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }

                // ====================================================================
                // 4. AUTO STRING DETECTION TOGGLE & LAYOUT SWITCH
                // ====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AUTO DETECT STRING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(0.82f),
                            letterSpacing = 0.8.sp
                        )
                        Switch(
                            checked = isAutoMode,
                            onCheckedChange = { isAutoMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = inTuneColor,
                                uncheckedTrackColor = Color(0xFF1E3824)
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (lockedStringsSet.isNotEmpty()) {
                            Text(
                                text = "${lockedStringsSet.size}/${selectedTuning.stringNotes.size} Tuned ✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = inTuneColor
                            )
                        }
                        TextButton(
                            onClick = {
                                stringLayout = if (stringLayout == "Side by side") "Inline" else "Side by side"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewModule,
                                contentDescription = null,
                                tint = inTuneColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringLayout.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = inTuneColor
                            )
                        }
                    }
                }

                // ====================================================================
                // 5. PHOTOREALISTIC INTERACTIVE INSTRUMENT HEADSTOCK & PEG SELECTOR
                // ====================================================================
                if (!isChromaticMode && selectedTuning.stringNotes.isNotEmpty()) {
                    RealisticInstrumentHeadstockCard(
                        tuning = selectedTuning,
                        selectedStringIdx = safeSelectedStringIdx,
                        lockedStrings = lockedStringsSet.toSet(),
                        centsDeviation = effectiveCentsDeviation,
                        hasSignal = hasValidSignal,
                        isInTune = isInTune,
                        gaugeColor = gaugeColor,
                        stringLayout = stringLayout,
                        a4ReferenceHz = a4ReferenceHz,
                        onSelectString = { idx ->
                            selectedStringIdx = idx
                            isAutoMode = false
                            if (playSoundOnStringSelect) {
                                playReferenceTone(selectedTuning.stringNotes[idx])
                            }
                        },
                        onShiftStringSemitone = { idx, delta ->
                            val shifted = TuningsCatalog.shiftNote(selectedTuning.stringNotes[idx], delta)
                            val updated = selectedTuning.stringNotes.toMutableList().apply { set(idx, shifted) }
                            selectedTuning = selectedTuning.copy(stringNotes = updated)
                            if (playSoundOnStringSelect) {
                                playReferenceTone(shifted)
                            }
                        }
                    )
                } else {
                    val chromaticNotes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chromaticNotes.forEach { note ->
                            val cleanDetected = detectedNote.dropLastWhile { it.isDigit() || it == '-' }
                            val isDetected = hasValidSignal && cleanDetected == note
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDetected) gaugeColor.copy(0.28f) else Color(0xFF142418),
                                border = BorderStroke(1.2.dp, if (isDetected) gaugeColor else Color(0xFF1E3824)),
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable {
                                        playReferenceTone("${note}4")
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = note,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDetected) gaugeColor else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // ====================================================================
                // 6. BOTTOM TUNING PRESET DROPDOWN SELECTOR BAR
                // ====================================================================
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF142418),
                    border = BorderStroke(1.dp, Color(0xFF1E3824)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTuningDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isChromaticMode) "Chromatic Mode (All 12 Notes)" else selectedTuning.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isChromaticMode) {
                                    "Tap any note above to audition reference tone"
                                } else {
                                    "${selectedTuning.instrument} • Low-to-High: ${selectedTuning.stringNotes.reversed().joinToString(" - ")}"
                                },
                                fontSize = 11.sp,
                                color = Color.White.copy(0.60f)
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Tuning", tint = inTuneColor)
                    }
                }
            }
        }
    }

    // ========================================================================
    // 1. SELECT TUNING MODAL
    // ========================================================================
    if (showTuningDialog) {
        AlertDialog(
            onDismissRequest = { showTuningDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Instrument & Tuning", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(onClick = { showTuningDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                var filterInstrument by remember { mutableStateOf("All") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Guitar", "Bass", "Ukulele", "Violin", "Tanpura", "Sitar").forEach { inst ->
                            FilterChip(
                                selected = filterInstrument == inst,
                                onClick = { filterInstrument = inst },
                                label = { Text(inst, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Favourites", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    allAvailableTunings.filter { favoriteTuningIds.contains(it.id) }.forEach { tuning ->
                        TuningItemRow(
                            tuning = tuning,
                            isSelected = selectedTuning.id == tuning.id,
                            isFavorite = true,
                            onSelect = {
                                selectedTuning = tuning
                                isChromaticMode = false
                                selectedStringIdx = 0
                                showTuningDialog = false
                            },
                            onToggleFavorite = {
                                if (favoriteTuningIds.contains(tuning.id)) favoriteTuningIds.remove(tuning.id)
                                else favoriteTuningIds.add(tuning.id)
                            }
                        )
                    }

                    Text("All Tunings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    allAvailableTunings
                        .filter { filterInstrument == "All" || it.instrument.equals(filterInstrument, ignoreCase = true) }
                        .forEach { tuning ->
                            TuningItemRow(
                                tuning = tuning,
                                isSelected = selectedTuning.id == tuning.id,
                                isFavorite = favoriteTuningIds.contains(tuning.id),
                                onSelect = {
                                    selectedTuning = tuning
                                    isChromaticMode = false
                                    selectedStringIdx = 0
                                    showTuningDialog = false
                                },
                                onToggleFavorite = {
                                    if (favoriteTuningIds.contains(tuning.id)) favoriteTuningIds.remove(tuning.id)
                                    else favoriteTuningIds.add(tuning.id)
                                }
                            )
                        }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTuningDialog = false
                        showAddCustomDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = inTuneColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Custom Tuning", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ========================================================================
    // 2. ADD CUSTOM TUNING DIALOG
    // ========================================================================
    if (showAddCustomDialog) {
        var customName by remember { mutableStateOf("") }
        var customInstrument by remember { mutableStateOf("Guitar") }
        val customStrings = remember { mutableStateListOf("E4", "B3", "G3", "D3", "A2", "E2") }

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Custom Tuning", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showAddCustomDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Preset Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Guitar", "Bass", "Ukulele", "Violin", "Tanpura", "Sitar").forEach { inst ->
                            FilterChip(
                                selected = customInstrument == inst,
                                onClick = { customInstrument = inst },
                                label = { Text(inst, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Strings (High to Low)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Button(
                            onClick = { if (customStrings.size < 12) customStrings.add(0, "E4") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("+ Add String", fontSize = 11.sp)
                        }
                    }

                    customStrings.forEachIndexed { sIdx, note ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (customStrings.size > 1) customStrings.removeAt(sIdx) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(0.7f))
                            }
                            IconButton(onClick = { customStrings[sIdx] = TuningsCatalog.shiftNote(note, -1) }) {
                                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(note, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(onClick = { customStrings[sIdx] = TuningsCatalog.shiftNote(note, 1) }) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val canSaveTuning = customName.isNotBlank() &&
                    customStrings.isNotEmpty() &&
                    customStrings.size <= 12 &&
                    customStrings.all { it.isNotBlank() }

                Button(
                    onClick = {
                        if (canSaveTuning) {
                            val newTuning = InstrumentTuning(
                                id = "custom_${System.currentTimeMillis()}",
                                name = customName.trim(),
                                instrument = customInstrument,
                                stringNotes = customStrings.toList(),
                                isCustom = true
                            )
                            customTunings.add(newTuning)
                            selectedTuning = newTuning
                            selectedStringIdx = 0
                            isChromaticMode = false
                            showAddCustomDialog = false
                        }
                    },
                    enabled = canSaveTuning,
                    colors = ButtonDefaults.buttonColors(containerColor = inTuneColor)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ========================================================================
    // 3. SETTINGS DIALOG
    // ========================================================================
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Tuner Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("String Peg Layout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    listOf("Side by side", "Inline").forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { stringLayout = opt },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = stringLayout == opt, onClick = { stringLayout = opt })
                            Spacer(Modifier.width(8.dp))
                            Text(opt, fontSize = 13.sp)
                        }
                    }

                    Text("Deviation Readout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    listOf("Cents", "Semitones", "Simple").forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tuningDisplayType = opt },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = tuningDisplayType == opt, onClick = { tuningDisplayType = opt })
                            Spacer(Modifier.width(8.dp))
                            Text(opt, fontSize = 13.sp)
                        }
                    }

                    Text("Audio Feedback", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Play reference tone when selecting peg", fontSize = 12.sp)
                        Switch(checked = playSoundOnStringSelect, onCheckedChange = { playSoundOnStringSelect = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Play confirmation chime when in tune", fontSize = 12.sp)
                        Switch(checked = playInTuneSound, onCheckedChange = { playInTuneSound = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showPermissionAlert) {
        AlertDialog(
            onDismissRequest = { showPermissionAlert = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("To detect your instrument's pitch in real time and guide string tightening/loosening, please grant microphone access.") },
            confirmButton = {
                Button(
                    onClick = {
                        permissionManager.requestMicrophonePermission()
                        showPermissionAlert = false
                    }
                ) {
                    Text("Allow Mic")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Moving Strobe Phase Strip + Live Mic Input Signal Meter
 */
@Composable
private fun TunerPhaseStrobeStrip(
    strobePhase: Float,
    centsDeviation: Int,
    hasSignal: Boolean,
    gaugeColor: Color,
    micAmplitude: Float
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF070D09),
        border = BorderStroke(1.dp, Color(0xFF1B3322)),
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stripeCount = 14
            val stripeW = w / stripeCount

            for (i in -2..stripeCount + 2) {
                val x = ((i + strobePhase) * stripeW)
                val distFromCenter = abs((x + stripeW * 0.25f) - w * 0.5f) / (w * 0.5f)
                val alpha = ((1f - distFromCenter.coerceIn(0f, 1f)) * (if (hasSignal) 0.72f else 0.18f))
                drawRoundRect(
                    color = gaugeColor.copy(alpha = alpha),
                    topLeft = Offset(x, 4f),
                    size = Size(stripeW * 0.48f, h - 8f),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }

            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h),
                strokeWidth = 2.dp.toPx()
            )

            val levelFrac = (micAmplitude * 12f).coerceIn(0f, 1f)
            drawRect(
                color = Color(0xFF00E676).copy(alpha = 0.75f),
                topLeft = Offset(0f, h - 2.5.dp.toPx()),
                size = Size(w * levelFrac, 2.5.dp.toPx())
            )
        }
    }
}

/**
 * Precision 180-Degree Strobe Arc Needle Gauge with ±50 Cents Graduation Ticks
 */
@Composable
private fun TunerArcGauge(
    animatedCents: Float,
    hasSignal: Boolean,
    isInTune: Boolean,
    gaugeColor: Color
) {
    val tickPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 190, 210, 198)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val pivotY = h * 0.86f
        val radius = minOf(w * 0.42f, h * 0.78f).coerceAtLeast(48f)

        // Outer subtle glow arc
        drawArc(
            color = Color(0xFF172D1E),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - radius, pivotY - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        )

        // In-Tune ±3 Cents Sweet-Spot Emerald Band at top center (270 deg)
        drawArc(
            color = Color(0xFF00E676).copy(alpha = if (isInTune) 0.85f else 0.35f),
            startAngle = 264f,
            sweepAngle = 12f,
            useCenter = false,
            topLeft = Offset(centerX - radius, pivotY - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        )

        tickPaint.textSize = 9.5.dp.toPx()

        // Graduation ticks from -50 cents (-70 deg) to +50 cents (+70 deg)
        for (c in -50..50 step 5) {
            val angleDeg = -90f + (c / 50f) * 70f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val isMajor = c % 10 == 0
            val isZero = c == 0
            val innerR = radius - (if (isZero) 14.dp.toPx() else if (isMajor) 10.dp.toPx() else 6.dp.toPx())
            val outerR = radius + (if (isZero) 6.dp.toPx() else 2.dp.toPx())

            val sx = centerX + (innerR * cos(angleRad)).toFloat()
            val sy = pivotY + (innerR * sin(angleRad)).toFloat()
            val ex = centerX + (outerR * cos(angleRad)).toFloat()
            val ey = pivotY + (outerR * sin(angleRad)).toFloat()

            val tickCol = when {
                isZero -> Color(0xFF00E676)
                abs(c) <= 10 -> Color.White.copy(alpha = 0.8f)
                else -> Color.White.copy(alpha = 0.42f)
            }

            drawLine(
                color = tickCol,
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (isZero) 2.6.dp.toPx() else if (isMajor) 1.6.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            if (c in intArrayOf(-50, -30, -10, 0, 10, 30, 50)) {
                val labelR = radius + 14.dp.toPx()
                val lx = centerX + (labelR * cos(angleRad)).toFloat()
                val ly = pivotY + (labelR * sin(angleRad)).toFloat()
                val labelStr = when {
                    c == 0 -> "0¢"
                    c > 0 -> "+$c"
                    else -> "$c"
                }
                drawContext.canvas.nativeCanvas.drawText(labelStr, lx, ly + 3.dp.toPx(), tickPaint)
            }
        }

        // Dynamic Needle Vector
        val clampedCents = animatedCents.coerceIn(-55f, 55f)
        val needleAngleDeg = -90f + (clampedCents / 50f) * 70f
        val needleRad = Math.toRadians(needleAngleDeg.toDouble())
        val needleLen = radius - 6.dp.toPx()
        val tipX = centerX + (needleLen * cos(needleRad)).toFloat()
        val tipY = pivotY + (needleLen * sin(needleRad)).toFloat()

        if (hasSignal) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(gaugeColor.copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(tipX, tipY),
                    radius = 18.dp.toPx()
                ),
                radius = 18.dp.toPx(),
                center = Offset(tipX, tipY)
            )
        }

        drawLine(
            color = if (hasSignal) gaugeColor else Color.White.copy(alpha = 0.35f),
            start = Offset(centerX, pivotY),
            end = Offset(tipX, tipY),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color(0xFF142418),
            radius = 9.dp.toPx(),
            center = Offset(centerX, pivotY)
        )
        drawCircle(
            color = gaugeColor,
            radius = 5.dp.toPx(),
            center = Offset(centerX, pivotY)
        )
    }
}

/**
 * Photorealistic Multi-Instrument Headstock Card with Tap-to-Select Pegs,
 * Wound/Steel Strings, Manka Fine-Tuning Swan Beads, and Animated Tighten (↻) / Loosen (↺) Indicators.
 */
@Composable
private fun RealisticInstrumentHeadstockCard(
    tuning: InstrumentTuning,
    selectedStringIdx: Int,
    lockedStrings: Set<Int>,
    centsDeviation: Int,
    hasSignal: Boolean,
    isInTune: Boolean,
    gaugeColor: Color,
    stringLayout: String,
    a4ReferenceHz: Float,
    onSelectString: (Int) -> Unit,
    onShiftStringSemitone: (Int, Int) -> Unit
) {
    val numStrings = tuning.stringNotes.size.coerceAtLeast(1)
    val instLow = tuning.instrument.lowercase()
    val isUkulele = instLow.contains("ukulele")
    val isBass = instLow.contains("bass")
    val isViolin = instLow.contains("violin")
    val isTanpuraOrSitar = instLow.contains("tanpura") || instLow.contains("sitar")
    val isSixInLine = stringLayout == "Inline" && !isViolin && !isUkulele && !isTanpuraOrSitar

    val infiniteTransition = rememberInfiniteTransition(label = "headstock_string_vib")
    val vibPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 75, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "headstock_vib_phase"
    )

    val pegLabelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }
    val actionHintPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0D1811),
        border = BorderStroke(1.dp, Color(0xFF1F3B27)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tuning.instrument.uppercase()} HEADSTOCK ($numStrings STRINGS)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676).copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TAP ANY PEG TO LOCK & AUDITION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.52f)
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
            ) {
                val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                val pegPositions = remember(canvasW, canvasH, numStrings, isSixInLine, isViolin, isTanpuraOrSitar) {
                    val midX = canvasW * 0.5f
                    val nutY = canvasH * 0.88f
                    val topY = canvasH * 0.14f
                    val headHalfW = canvasW * (if (isViolin) 0.12f else if (isUkulele) 0.17f else 0.20f)

                    List(numStrings) { sIdx ->
                        val physLeftToRightIdx = (numStrings - 1 - sIdx).coerceIn(0, numStrings - 1)
                        val halfCount = (numStrings + 1) / 2

                        if (isSixInLine) {
                            val frac = physLeftToRightIdx / (numStrings - 1).coerceAtLeast(1).toFloat()
                            val py = (nutY - canvasH * 0.15f) - frac * (canvasH * 0.62f)
                            val px = midX - headHalfW * (1.15f - frac * 0.35f)
                            Offset(px, py)
                        } else {
                            val isLeftSide = physLeftToRightIdx < halfCount
                            if (isLeftSide) {
                                val rowFrac = if (halfCount <= 1) 0.5f else physLeftToRightIdx / (halfCount - 1f)
                                val py = (nutY - canvasH * 0.18f) - rowFrac * (canvasH * 0.54f)
                                val px = midX - headHalfW * 1.18f
                                Offset(px, py)
                            } else {
                                val rightCount = (numStrings - halfCount).coerceAtLeast(1)
                                val rightIdx = physLeftToRightIdx - halfCount
                                val rowFrac = if (rightCount <= 1) 0.5f else rightIdx / (rightCount - 1f)
                                val py = topY + rowFrac * (canvasH * 0.54f)
                                val px = midX + headHalfW * 1.18f
                                Offset(px, py)
                            }
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pegPositions, numStrings) {
                            detectTapGestures { tapOffset ->
                                var bestIdx = -1
                                var minDist = Float.MAX_VALUE
                                pegPositions.forEachIndexed { idx, pegPos ->
                                    val isLeft = pegPos.x < size.width * 0.5f
                                    val badgeX = if (isLeft) pegPos.x - 44.dp.toPx() else pegPos.x + 44.dp.toPx()
                                    val dPeg = hypot(tapOffset.x - pegPos.x, tapOffset.y - pegPos.y)
                                    val dBadge = hypot(tapOffset.x - badgeX, tapOffset.y - pegPos.y)
                                    val d = minOf(dPeg, dBadge)
                                    if (d < minDist) {
                                        minDist = d
                                        bestIdx = idx
                                    }
                                }
                                if (bestIdx in 0 until numStrings && minDist <= 58.dp.toPx()) {
                                    onSelectString(bestIdx)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val midX = w * 0.5f
                    val nutY = h * 0.88f
                    val nutHalfW = w * (if (isViolin) 0.085f else if (isUkulele) 0.11f else 0.135f)
                    val headHalfW = w * (if (isViolin) 0.12f else if (isUkulele) 0.17f else 0.20f)

                    val headstockPath = Path().apply {
                        when {
                            isViolin -> {
                                moveTo(midX - nutHalfW, h)
                                lineTo(midX - nutHalfW, nutY)
                                lineTo(midX - headHalfW * 0.88f, h * 0.22f)
                                cubicTo(
                                    midX - headHalfW * 1.25f, h * 0.04f,
                                    midX + headHalfW * 1.25f, h * 0.04f,
                                    midX + headHalfW * 0.88f, h * 0.22f
                                )
                                lineTo(midX + nutHalfW, nutY)
                                lineTo(midX + nutHalfW, h)
                                close()
                            }
                            isSixInLine -> {
                                moveTo(midX - nutHalfW, h)
                                lineTo(midX - nutHalfW, nutY)
                                cubicTo(
                                    midX - headHalfW * 1.35f, h * 0.72f,
                                    midX - headHalfW * 0.95f, h * 0.25f,
                                    midX - headHalfW * 0.45f, h * 0.06f
                                )
                                cubicTo(
                                    midX + headHalfW * 0.65f, h * 0.02f,
                                    midX + headHalfW * 1.25f, h * 0.28f,
                                    midX + headHalfW * 0.45f, h * 0.52f
                                )
                                quadraticBezierTo(
                                    midX + nutHalfW * 0.95f, h * 0.72f,
                                    midX + nutHalfW, nutY
                                )
                                lineTo(midX + nutHalfW, h)
                                close()
                            }
                            isTanpuraOrSitar -> {
                                moveTo(midX - nutHalfW * 1.08f, h)
                                lineTo(midX - nutHalfW * 1.08f, nutY)
                                lineTo(midX - headHalfW * 0.92f, h * 0.14f)
                                quadraticBezierTo(midX, h * 0.02f, midX + headHalfW * 0.92f, h * 0.14f)
                                lineTo(midX + nutHalfW * 1.08f, nutY)
                                lineTo(midX + nutHalfW * 1.08f, h)
                                close()
                            }
                            else -> {
                                moveTo(midX - nutHalfW, h)
                                lineTo(midX - nutHalfW, nutY)
                                cubicTo(
                                    midX - nutHalfW * 1.15f, nutY - h * 0.08f,
                                    midX - headHalfW * 1.08f, h * 0.74f,
                                    midX - headHalfW, h * 0.64f
                                )
                                lineTo(midX - headHalfW * 0.96f, h * 0.14f)
                                cubicTo(
                                    midX - headHalfW * 0.45f, h * 0.04f,
                                    midX - headHalfW * 0.15f, h * 0.11f,
                                    midX, h * 0.09f
                                )
                                cubicTo(
                                    midX + headHalfW * 0.15f, h * 0.11f,
                                    midX + headHalfW * 0.45f, h * 0.04f,
                                    midX + headHalfW * 0.96f, h * 0.14f
                                )
                                lineTo(midX + headHalfW, h * 0.64f)
                                cubicTo(
                                    midX + headHalfW * 1.08f, h * 0.74f,
                                    midX + nutHalfW * 1.15f, nutY - h * 0.08f,
                                    midX + nutHalfW, nutY
                                )
                                lineTo(midX + nutHalfW, h)
                                close()
                            }
                        }
                    }

                   val shadowOffsetY = 4.dp.toPx()

                    withTransform({
                        translate(left = 0f, top = shadowOffsetY)
                    }) {
                        drawPath(headstockPath, color = Color.Black.copy(alpha = 0.55f))
                    }

                    val woodColors = when {
                        isSixInLine -> listOf(Color(0xFF8B5E3C), Color(0xFF5C3A21), Color(0xFF382212))
                        isTanpuraOrSitar -> listOf(Color(0xFF6E3514), Color(0xFF431E09), Color(0xFF240F04))
                        isViolin -> listOf(Color(0xFF9E4710), Color(0xFF5C2406), Color(0xFF280E02))
                        else -> listOf(Color(0xFF3E2418), Color(0xFF26150E), Color(0xFF160C08))
                    }

                    drawPath(
                        path = headstockPath,
                        brush = Brush.verticalGradient(colors = woodColors, startY = 0f, endY = h)
                    )
                    drawPath(
                        path = headstockPath,
                        color = if (isTanpuraOrSitar) Color(0xFFFFD54F).copy(alpha = 0.55f) else Color(0xFF8D6E63).copy(alpha = 0.45f),
                        style = Stroke(width = 1.6.dp.toPx())
                    )

                    // Truss rod cover or traditional ivory inlay crest
                    if (!isViolin && !isSixInLine) {
                        val crestPath = Path().apply {
                            moveTo(midX, nutY - 34.dp.toPx())
                            quadraticBezierTo(midX - 9.dp.toPx(), nutY - 14.dp.toPx(), midX - 6.dp.toPx(), nutY - 8.dp.toPx())
                            lineTo(midX + 6.dp.toPx(), nutY - 8.dp.toPx())
                            quadraticBezierTo(midX + 9.dp.toPx(), nutY - 14.dp.toPx(), midX, nutY - 34.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = crestPath,
                            color = if (isTanpuraOrSitar) Color(0xFFFFF8E1).copy(alpha = 0.35f) else Color(0xFF121418)
                        )
                    }

                    // 2. Bone / Ivory Nut (Targahan)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFD7CCC8), Color(0xFFFFFBF2), Color(0xFFD7CCC8)),
                            startX = midX - nutHalfW,
                            endX = midX + nutHalfW
                        ),
                        topLeft = Offset(midX - nutHalfW, nutY - 3.dp.toPx()),
                        size = Size(nutHalfW * 2f, 6.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // 3. Draw Strings, Tuning Posts, Machine Heads, and Interactive Peg Callouts
                    pegLabelPaint.textSize = 11.5.dp.toPx()
                    actionHintPaint.textSize = 9.5.dp.toPx()

                    pegPositions.forEachIndexed { sIdx, pegPos ->
                        val isSelected = sIdx == selectedStringIdx
                        val isLocked = sIdx in lockedStrings
                        val noteStr = tuning.stringNotes[sIdx]
                        val physLeftToRightIdx = (numStrings - 1 - sIdx).coerceIn(0, numStrings - 1)
                        val isLeftSide = pegPos.x < midX

                        val nutFrac = if (numStrings <= 1) 0.5f else physLeftToRightIdx / (numStrings - 1f)
                        val nutSlotX = (midX - nutHalfW * 0.80f) + nutFrac * (nutHalfW * 1.60f)
                        val postX = if (isSixInLine) {
                            pegPos.x + 18.dp.toPx()
                        } else if (isLeftSide) {
                            pegPos.x + 18.dp.toPx()
                        } else {
                            pegPos.x - 18.dp.toPx()
                        }
                        val postY = pegPos.y

                        // String gauge & color (wound bass vs plain steel)
                        val isWound = physLeftToRightIdx < numStrings / 2 && !isUkulele
                        val baseStringCol = when {
                            isSelected && hasSignal -> gaugeColor
                            isSelected -> Color.White
                            isLocked -> Color(0xFF00E676).copy(alpha = 0.80f)
                            isWound -> Color(0xFFD7B899).copy(alpha = 0.78f)
                            else -> Color(0xFFCFD8DC).copy(alpha = 0.78f)
                        }
                        val strStroke = (1.2f + (numStrings - 1 - sIdx) * 0.28f).dp.toPx()

                        // String segment from bottom fretboard -> nut slot -> tuning post
                        val vibOffset = if (isSelected && hasSignal) sin(vibPhase) * 2.2.dp.toPx() else 0f
                        drawLine(
                            color = baseStringCol,
                            start = Offset(nutSlotX + vibOffset * 0.4f, h),
                            end = Offset(nutSlotX, nutY),
                            strokeWidth = strStroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = baseStringCol,
                            start = Offset(nutSlotX, nutY),
                            end = Offset(postX, postY),
                            strokeWidth = strStroke,
                            cap = StrokeCap.Round
                        )

                        // Tanpura / Sitar Manka Fine-Tuning Swan Bead along the string
                        if (isTanpuraOrSitar) {
                            val mankaY = nutY + (h - nutY) * 0.48f
                            drawCircle(
                                color = if (isSelected) gaugeColor else Color(0xFFFFF8E1),
                                radius = 3.5.dp.toPx(),
                                center = Offset(nutSlotX, mankaY)
                            )
                        }

                        // Metallic Tuning Capstan Post & Coiled String Bushing
                        drawCircle(
                            color = Color(0xFF90A4AE),
                            radius = 5.5.dp.toPx(),
                            center = Offset(postX, postY)
                        )
                        drawCircle(
                            color = if (isSelected) gaugeColor else Color(0xFF263238),
                            radius = 2.8.dp.toPx(),
                            center = Offset(postX, postY)
                        )

                        // Machine Head Shaft & Peg Knob
                        val knobX = if (isLeftSide) pegPos.x - 4.dp.toPx() else pegPos.x + 4.dp.toPx()
                        drawLine(
                            color = Color(0xFFB0BEC5),
                            start = Offset(postX, postY),
                            end = Offset(knobX, pegPos.y),
                            strokeWidth = 3.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        val knobColor = when {
                            isSelected -> gaugeColor
                            isLocked -> Color(0xFF00E676)
                            isTanpuraOrSitar || isViolin -> Color(0xFFD7A86E)
                            else -> Color(0xFFCFD8DC)
                        }
                        val knobW = if (isBass) 12.dp.toPx() else 9.dp.toPx()
                        val knobH = if (isBass) 16.dp.toPx() else 13.dp.toPx()
                        drawRoundRect(
                            color = knobColor,
                            topLeft = Offset(knobX - knobW * 0.5f, pegPos.y - knobH * 0.5f),
                            size = Size(knobW, knobH),
                            cornerRadius = CornerRadius(knobW * 0.45f, knobW * 0.45f)
                        )

                        // Interactive Peg Callout Bubble on Left/Right Margin
                        val bubbleX = if (isLeftSide) (pegPos.x - 46.dp.toPx()).coerceAtLeast(32.dp.toPx())
                        else (pegPos.x + 46.dp.toPx()).coerceAtMost(w - 32.dp.toPx())
                        val bubbleY = pegPos.y
                        val bubbleRad = if (isSelected) 17.dp.toPx() else 14.5.dp.toPx()

                        // Dashed/solid connector line from peg knob to callout bubble
                        drawLine(
                            color = knobColor.copy(alpha = if (isSelected) 0.85f else 0.35f),
                            start = Offset(knobX, pegPos.y),
                            end = Offset(bubbleX, bubbleY),
                            strokeWidth = if (isSelected) 1.6.dp.toPx() else 1.dp.toPx()
                        )

                        if (isSelected) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(gaugeColor.copy(alpha = 0.55f), Color.Transparent),
                                    center = Offset(bubbleX, bubbleY),
                                    radius = bubbleRad * 1.55f
                                ),
                                radius = bubbleRad * 1.55f,
                                center = Offset(bubbleX, bubbleY)
                            )
                        }

                        drawCircle(
                            color = when {
                                isSelected -> gaugeColor.copy(alpha = 0.25f)
                                isLocked -> Color(0xFF00E676).copy(alpha = 0.18f)
                                else -> Color(0xFF142418)
                            },
                            radius = bubbleRad,
                            center = Offset(bubbleX, bubbleY)
                        )
                        drawCircle(
                            color = when {
                                isSelected -> gaugeColor
                                isLocked -> Color(0xFF00E676)
                                else -> Color(0xFF2E5438)
                            },
                            radius = bubbleRad,
                            center = Offset(bubbleX, bubbleY),
                            style = Stroke(width = if (isSelected) 2.2.dp.toPx() else 1.2.dp.toPx())
                        )

                        pegLabelPaint.color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.argb(220, 220, 235, 225)
                        drawContext.canvas.nativeCanvas.drawText(
                            noteStr,
                            bubbleX,
                            bubbleY + pegLabelPaint.textSize * 0.36f,
                            pegLabelPaint
                        )

                        // Actionable Tighten ↻ / Loosen ↺ Tag next to active peg
                        if (isSelected && hasSignal) {
                            val actionTag = when {
                                isInTune -> "✓ OK"
                                centsDeviation < -3 -> "TIGHTEN ↻"
                                else -> "LOOSEN ↺"
                            }
                            actionHintPaint.color = when {
                                isInTune -> android.graphics.Color.rgb(0, 230, 118)
                                centsDeviation < -3 -> android.graphics.Color.rgb(255, 171, 0)
                                else -> android.graphics.Color.rgb(41, 182, 246)
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                actionTag,
                                bubbleX,
                                bubbleY - bubbleRad - 4.dp.toPx(),
                                actionHintPaint
                            )
                        } else if (isLocked) {
                            actionHintPaint.color = android.graphics.Color.rgb(0, 230, 118)
                            drawContext.canvas.nativeCanvas.drawText(
                                "✓",
                                bubbleX + bubbleRad * 0.85f,
                                bubbleY - bubbleRad * 0.55f,
                                actionHintPaint
                            )
                        }
                    }
                }
            }

            // Bottom String Fine-Semitone Adjuster Strip for Selected String
            val activeNote = tuning.stringNotes[selectedStringIdx]
            val (_, activeHz) = noteNameToMidiAndHz(activeNote, a4ReferenceHz)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF122117), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "String ${selectedStringIdx + 1}: $activeNote (%.1f Hz)".format(activeHz),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onShiftStringSemitone(selectedStringIdx, -1) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("-1 Semitone", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { onShiftStringSemitone(selectedStringIdx, +1) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("+1 Semitone", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Concert Miraj Tanpura & Shruti Drone Studio Panel
 */
@Composable
private fun TanpuraDroneStudioPanel(
    viewModel: AppViewModel,
    a4ReferenceHz: Float,
    onOpenTanpuraTuner: (String) -> Unit
) {
    val pitchClasses = remember { listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B") }
    var selectedSaRoot by remember { mutableStateOf("C#") }
    var selectedSwarMode by remember { mutableStateOf("Pa (5th)") } // "Pa (5th)", "Ma (4th)", "Ni (7th)", "Sa (Unison)"
    var selectedOctave by remember { mutableIntStateOf(3) } // 3 = Male/Instrumental, 4 = Female/Flute
    var droneBpm by remember { mutableFloatStateOf(64f) }
    var jawariRichness by remember { mutableFloatStateOf(0.78f) }
    var isDronePlaying by remember { mutableStateOf(false) }
    var activePluckedStringIdx by remember { mutableIntStateOf(-1) }

    val swarOptions = remember { listOf("Pa (5th)", "Ma (4th)", "Ni (7th)", "Sa (Unison)") }

    // Compute the 4 Tanpura strings: [0] = Pancham/Madhyam/Nishad, [1] = Madhya Sa, [2] = Madhya Sa, [3] = Kharaj (Low) Sa
    val tanpuraStrings = remember(selectedSaRoot, selectedSwarMode, selectedOctave) {
        val rootSemi = pitchClasses.indexOf(selectedSaRoot).coerceAtLeast(0)
        val firstStringOffset = when {
            selectedSwarMode.startsWith("Pa") -> 7
            selectedSwarMode.startsWith("Ma") -> 5
            selectedSwarMode.startsWith("Ni") -> 11
            else -> 0
        }
        val firstSemi = (rootSemi + firstStringOffset) % 12
        val firstOct = if (firstStringOffset > 0 && rootSemi + firstStringOffset >= 12) selectedOctave + 1 else selectedOctave
        val s1Note = "${pitchClasses[firstSemi]}$firstOct"
        val s2Note = "$selectedSaRoot${selectedOctave + 1}"
        val s3Note = "$selectedSaRoot${selectedOctave + 1}"
        val s4Note = "$selectedSaRoot$selectedOctave"

        val s1Title = when {
            selectedSwarMode.startsWith("Pa") -> "Pa"
            selectedSwarMode.startsWith("Ma") -> "Ma"
            selectedSwarMode.startsWith("Ni") -> "Ni"
            else -> "Sa"
        }
        listOf(
            s1Title to s1Note,
            "Sa" to s2Note,
            "Sa" to s3Note,
            "Ṣa (Kharaj)" to s4Note
        )
    }

    // Continuous authentic Tanpura pluck cycle coroutine (String 1 -> String 2 -> String 3 -> String 4 with rest)
    LaunchedEffect(isDronePlaying, tanpuraStrings, droneBpm, a4ReferenceHz) {
        if (!isDronePlaying) {
            activePluckedStringIdx = -1
            return@LaunchedEffect
        }
        val beatMs = (60_000f / droneBpm.coerceIn(30f, 150f)).toLong()
        while (isActive && isDronePlaying) {
            for (sIdx in tanpuraStrings.indices) {
                if (!isDronePlaying) break
                activePluckedStringIdx = sIdx
                val (_, noteStr) = tanpuraStrings[sIdx]
                val (midi, _) = noteNameToMidiAndHz(noteStr, a4ReferenceHz)
                runCatching {
                    viewModel.audioEngine.setInstrument("SITAR")
                    viewModel.audioEngine.noteOn(midi, 0.72f + jawariRichness * 0.22f)
                }
                // Authentic Miraj rhythm: slight breath pause after Kharaj (4th string)
                val stepWait = if (sIdx == 3) (beatMs * 1.45f).toLong() else (beatMs * 0.85f).toLong()
                delay(stepWait)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isDronePlaying = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tanpura_drone_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 85, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tanpura_wave_phase"
    )

    val goldAccent = Color(0xFFFFD54F)
    val amberAccent = Color(0xFFFFB300)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Start/Stop Drone Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Miraj Concert Tanpura Drone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = goldAccent
                )
                Text(
                    text = "Key of $selectedSaRoot • $selectedSwarMode - Sa - Sa - Ṣa",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            Button(
                onClick = { isDronePlaying = !isDronePlaying },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDronePlaying) Color(0xFFD32F2F) else amberAccent
                ),
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    imageVector = if (isDronePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isDronePlaying) Color.White else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isDronePlaying) "STOP DRONE" else "START DRONE",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDronePlaying) Color.White else Color.Black
                )
            }
        }

        // Interactive 4-String Carved Tanpura Gourd (Tumba), Ghoraj Bridge, Jiva Threads & Manka Swan Beads
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF160D06),
            border = BorderStroke(1.2.dp, Color(0xFF8D5B28)),
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            val labelPaint = remember {
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tanpuraStrings, a4ReferenceHz) {
                        detectTapGestures { offset ->
                            val strIdx = ((offset.y / size.height) * tanpuraStrings.size).toInt()
                                .coerceIn(0, tanpuraStrings.lastIndex)
                            activePluckedStringIdx = strIdx
                            val (midi, _) = noteNameToMidiAndHz(tanpuraStrings[strIdx].second, a4ReferenceHz)
                            runCatching {
                                viewModel.audioEngine.setInstrument("SITAR")
                                viewModel.audioEngine.noteOn(midi, 0.90f)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val leftNutX = w * 0.14f
                val bridgeX = w * 0.78f

                // Carved Miraj Resonator Gourd (Tumba) on right
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC86D24), Color(0xFF6E2C08), Color(0xFF280E02)),
                        center = Offset(w * 0.80f, h * 0.45f),
                        radius = h * 0.62f
                    ),
                    topLeft = Offset(w * 0.62f, h * 0.06f),
                    size = Size(w * 0.35f, h * 0.88f)
                )

                // Teakwood Dand (Neck)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4A220C), Color(0xFF281105), Color(0xFF3E1C09)),
                        startY = h * 0.14f,
                        endY = h * 0.86f
                    ),
                    topLeft = Offset(leftNutX - 8.dp.toPx(), h * 0.14f),
                    size = Size(bridgeX - leftNutX + 16.dp.toPx(), h * 0.72f),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Ivory Nut (Atak / Targahan)
                drawLine(
                    color = Color(0xFFFFF8E1),
                    start = Offset(leftNutX, h * 0.15f),
                    end = Offset(leftNutX, h * 0.85f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Wide Curved Bone Ghoraj (Jawari Bridge)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF8E1), Color(0xFFD7CCC8), Color(0xFF8D6E63)),
                        startY = h * 0.18f,
                        endY = h * 0.82f
                    ),
                    topLeft = Offset(bridgeX - 7.dp.toPx(), h * 0.18f),
                    size = Size(14.dp.toPx(), h * 0.64f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                labelPaint.textSize = 10.5.dp.toPx()

                tanpuraStrings.forEachIndexed { idx, (swarName, noteStr) ->
                    val sy = h * (0.24f + idx * 0.175f)
                    val isPlucked = idx == activePluckedStringIdx
                    val isBronzeString = idx == 0 || idx == 3

                    val path = Path()
                    val steps = 28
                    for (step in 0..steps) {
                        val u = step / steps.toFloat()
                        val x = leftNutX + u * (bridgeX - leftNutX)
                        val wave = if (isPlucked) {
                            sin(u * PI.toFloat()) * sin(wavePhase + idx) * (3.5.dp.toPx() * (0.5f + 0.5f * jawariRichness))
                        } else 0f
                        if (step == 0) path.moveTo(x, sy + wave) else path.lineTo(x, sy + wave)
                    }

                    if (isPlucked) {
                        drawPath(
                            path = path,
                            color = amberAccent.copy(alpha = 0.55f),
                            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    drawPath(
                        path = path,
                        color = when {
                            isPlucked -> Color.White
                            isBronzeString -> Color(0xFFFFD54F)
                            else -> Color(0xFFE0E0E0)
                        },
                        style = Stroke(width = if (idx == 3) 2.2.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Red Silk Jiva Thread under string on Ghoraj bridge
                    drawCircle(
                        color = Color(0xFFD32F2F),
                        radius = 2.8.dp.toPx(),
                        center = Offset(bridgeX - 5.dp.toPx(), sy)
                    )

                    // Ivory Manka Fine-Tuning Swan Bead between bridge and tailpiece
                    val mankaX = bridgeX + (w * 0.11f) - (idx * 3.dp.toPx())
                    drawOval(
                        color = if (isPlucked) goldAccent else Color(0xFFFFF8E1),
                        topLeft = Offset(mankaX - 5.dp.toPx(), sy - 3.5.dp.toPx()),
                        size = Size(10.dp.toPx(), 7.dp.toPx())
                    )

                    // Left String Label Pill
                    drawContext.canvas.nativeCanvas.drawText(
                        "$swarName ($noteStr)",
                        leftNutX * 0.5f,
                        sy + 4.dp.toPx(),
                        labelPaint
                    )
                }
            }
        }

        // Tonic Pitch (Sa) Selector Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "SA (TONIC PITCH) • TAP ANY STRING ABOVE TO PLUCK MANUALLY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = goldAccent.copy(alpha = 0.85f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pitchClasses.forEach { pc ->
                    val isSel = selectedSaRoot == pc
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedSaRoot = pc },
                        label = { Text(pc, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }
            }
        }

        // First String Swar (Pa / Ma / Ni / Sa) & Register Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            swarOptions.forEach { swar ->
                FilterChip(
                    selected = selectedSwarMode == swar,
                    onClick = { selectedSwarMode = swar },
                    label = { Text(swar, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
            AssistChip(
                onClick = { selectedOctave = if (selectedOctave == 3) 4 else 3 },
                label = {
                    Text(
                        if (selectedOctave == 3) "Register: Male/Inst (Oct 3)" else "Register: Female (Oct 4)",
                        fontSize = 11.sp
                    )
                }
            )
        }

        // Tempo (BPM) & Jawari Richness Sliders + Open in Strobe Tuner Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cycle Tempo: ${droneBpm.roundToInt()} BPM", fontSize = 10.5.sp, color = Color.White.copy(0.8f))
                Slider(
                    value = droneBpm,
                    onValueChange = { droneBpm = it },
                    valueRange = 36f..132f,
                    modifier = Modifier.height(26.dp),
                    colors = SliderDefaults.colors(thumbColor = goldAccent, activeTrackColor = amberAccent)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Jawari Shimmer: ${(jawariRichness * 100).roundToInt()}%", fontSize = 10.5.sp, color = Color.White.copy(0.8f))
                Slider(
                    value = jawariRichness,
                    onValueChange = { jawariRichness = it },
                    valueRange = 0.2f..1.0f,
                    modifier = Modifier.height(26.dp),
                    colors = SliderDefaults.colors(thumbColor = goldAccent, activeTrackColor = amberAccent)
                )
            }
        }

        OutlinedButton(
            onClick = {
                isDronePlaying = false
                onOpenTanpuraTuner("tanpura_pa_c_sharp")
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = goldAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tune Physical Tanpura with Strobe Tuner", color = goldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TuningItemRow(
    tuning: InstrumentTuning,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF173522) else Color(0xFF121E16),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E676) else Color(0xFF1F3626)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tuning.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF00E676) else Color.White
                )
                Text(
                    text = "${tuning.instrument} • ${tuning.stringNotes.reversed().joinToString(" - ")}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.45f)
                )
            }
        }
    }
}