package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.Taal
import com.creativeblisslabs.peqlearn.app.ui.components.FullscreenDialog
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedTablaVisualizer
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Saarang & Digirang Concert Electronic Tanpura & Tabla Studio:
 * - High-contrast Backlit LCD/OLED Telemetry Display with live Bol, Matra, Sam (X) / Khali (0), and Riyaz Timer
 * - Interactive 4/5/6-String Miraj Tanpura Visualizer with Manka Fine-Tuning Swan Beads, Jiva silk threads,
 *   Jawari overtone control, and Tap-to-Pluck manual string audition
 * - Interactive Tala Matra & Vibhag Bol Grid with Sam/Tali/Khali markers and Laya (0.5x, 1x, 2x, 3x, 4x) multipliers
 * - Collapsible Live Dual-Tabla Touch Pads for practicing alongside the Tanpura drone
 * - 3-Channel Master Console (Tanpura, Tabla, Metronome) with independent Volume, +/- BPM steppers, and Tap Tempo
 */
@Composable
fun TanpuraTablaStudio(
    viewModel: AppViewModel,
    isCarnatic: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onDismiss: (() -> Unit)? = null
) {
    var isFullscreen by remember { mutableStateOf(false) }
    LaunchedEffect(isFullscreen) {
        viewModel.isTanpuraFullscreen.value = isFullscreen
    }

    if (isFullscreen) {
        FullscreenDialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0A0E14)
            ) {
                HardwareEnclosure(
                    viewModel = viewModel,
                    initialIsCarnatic = isCarnatic,
                    accentColor = accentColor,
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    onClose = onDismiss
                )
            }
        }
    } else {
        HardwareEnclosure(
            viewModel = viewModel,
            initialIsCarnatic = isCarnatic,
            accentColor = accentColor,
            isFullscreen = false,
            onToggleFullscreen = { isFullscreen = true },
            onClose = onDismiss
        )
    }
}

@Composable
private fun HardwareEnclosure(
    viewModel: AppViewModel,
    initialIsCarnatic: Boolean,
    accentColor: Color,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val audioEngine = remember { getAudioEngine() }
    val scope = rememberCoroutineScope()

    // --- State: Pitch / Shruti (Safed 1..7 / Kali 1..5 labels + Western) ---
    val pitches = remember {
        listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }
    val indianShrutiNames = remember {
        listOf(
            "Safed 1 (C)", "Kali 1 (C#)", "Safed 2 (D)", "Kali 2 (D#)",
            "Safed 3 (E)", "Safed 4 (F)", "Kali 3 (F#)", "Safed 5 (G)",
            "Kali 4 (G#)", "Safed 6 (A)", "Kali 5 (A#)", "Safed 7 (B)"
        )
    }
    var selectedPitchIdx by remember { mutableIntStateOf(1) } // C# (Kali 1) default
    var fineTuneCents by remember { mutableFloatStateOf(0f) } // -50 to +50 cents
    var isFemaleUpperOctave by remember { mutableStateOf(false) } // Male (Oct 3) vs Female (Oct 4)

    // --- State: String Tuning Mode (Pa / Ma / Ni / Sa) ---
    val firstStringModes = remember { listOf("PA (5th)", "MA (4th)", "NI (7th)", "SA (Unison)") }
    var selectedFirstStringMode by remember { mutableIntStateOf(0) }
    var stringCount by remember { mutableIntStateOf(4) } // 4, 5, or 6 strings
    var pluckStyle by remember { mutableStateOf("MIRAJ") } // "MIRAJ" (Traditional rest after Kharaj) vs "EVEN"
    var jawariResonance by remember { mutableFloatStateOf(0.80f) } // 0.2f .. 1.0f

    // --- State: Tanpura Drone ---
    var isTanpuraRunning by remember { mutableStateOf(false) }
    var tanpuraTempoBpm by remember { mutableIntStateOf(62) }
    var activeStringIndex by remember { mutableIntStateOf(-1) }
    var tanpuraJob by remember { mutableStateOf<Job?>(null) }

    // --- State: Taal System Selector (Hindustani vs Carnatic) ---
    var selectedTradition by remember { mutableStateOf(if (initialIsCarnatic) "CARNATIC" else "HINDUSTANI") }
    val taals = remember(selectedTradition) {
        if (selectedTradition == "CARNATIC") CARNATIC_TALAS else INDIAN_TAALS
    }
    var selectedTaalIdx by remember(selectedTradition) { mutableIntStateOf(0) }
    val currentTaal = taals.getOrNull(selectedTaalIdx) ?: taals.first()

    var isTablaRunning by remember { mutableStateOf(false) }
    var tablaBpm by remember { mutableIntStateOf(110) }
    var layaMultiplier by remember { mutableFloatStateOf(1.0f) } // 0.5x, 1.0x, 2.0x, 3.0x, 4.0x
    var currentMatraIndex by remember { mutableIntStateOf(-1) }
    var tablaJob by remember { mutableStateOf<Job?>(null) }
    var isSyncWithMetronome by remember { mutableStateOf(false) }
    var showLiveTablaPads by remember { mutableStateOf(false) }

    // --- State: Metronome ---
    val isMetronomeActive by viewModel.metronomeActive.collectAsState()
    val metronomeBpm by viewModel.metronomeBpm.collectAsState()

    // --- State: Riyaz Practice Session Timer ---
    var riyazElapsedSeconds by remember { mutableIntStateOf(0) }
    val isAnyChannelRunning = isTanpuraRunning || isTablaRunning || isMetronomeActive

    LaunchedEffect(isAnyChannelRunning) {
        while (isActive && isAnyChannelRunning) {
            delay(1000L)
            riyazElapsedSeconds++
        }
    }

    LaunchedEffect(isSyncWithMetronome, metronomeBpm) {
        if (isSyncWithMetronome) {
            tablaBpm = metronomeBpm
        }
    }

    // --- Tap Tempo Timestamps ---
    var lastTanpuraTapTime by remember { mutableLongStateOf(0L) }
    var lastTablaTapTime by remember { mutableLongStateOf(0L) }
    var lastMetronomeTapTime by remember { mutableLongStateOf(0L) }

    fun handleTapTempo(lastTime: Long, minBpm: Int = 30, maxBpm: Int = 280, onBpmCalculated: (Int) -> Unit): Long {
        val now = ClockHelper.currentTimeMillis()
        if (lastTime > 0L) {
            val diff = now - lastTime
            if (diff in 180..2600) {
                val calculatedBpm = (60000L / diff).toInt().coerceIn(minBpm, maxBpm)
                onBpmCalculated(calculatedBpm)
            }
        }
        return now
    }

    // Compute exact Tanpura String Semitone Offsets & Swar Labels
    val tanpuraStringConfig = remember(selectedFirstStringMode, stringCount) {
        val (firstSwarLabel, firstSemi) = when (selectedFirstStringMode) {
            1 -> "Ma" to 5
            2 -> "Ni" to 11
            3 -> "Sa" to 0
            else -> "Pa" to 7
        }
        when (stringCount) {
            5 -> listOf(
                firstSwarLabel to firstSemi,
                "Ni" to 11,
                "Sa" to 12,
                "Sa" to 12,
                "Ṣa" to 0
            )
            6 -> listOf(
                firstSwarLabel to firstSemi,
                "Ma" to 5,
                "Ni" to 11,
                "Sa" to 12,
                "Sa" to 12,
                "Ṣa" to 0
            )
            else -> listOf(
                firstSwarLabel to firstSemi,
                "Sa" to 12,
                "Sa" to 12,
                "Ṣa" to 0
            )
        }
    }

    // Helper to trigger an individual Tanpura string (used by both auto-loop and manual string tap)
    fun triggerTanpuraString(sIdx: Int) {
        val (_, semiOffset) = tanpuraStringConfig.getOrElse(sIdx) { "Sa" to 12 }
        val baseMidi = (if (isFemaleUpperOctave) 60 else 48) + selectedPitchIdx
        val targetMidi = (baseMidi + semiOffset).coerceIn(36, 84)
        val gain = viewModel.tanpuraGain.floatValue.coerceIn(0.05f, 1f)
        val velocity = (gain * (0.72f + 0.25f * jawariResonance)).coerceIn(0.1f, 1f)
        runCatching {
            audioEngine.setInstrument("SITAR")
            audioEngine.noteOn(targetMidi, velocity)
        }
    }

    // Tanpura continuous drone & authentic Miraj string pluck loop
    DisposableEffect(
        isTanpuraRunning,
        selectedPitchIdx,
        selectedFirstStringMode,
        stringCount,
        tanpuraTempoBpm,
        pluckStyle,
        isFemaleUpperOctave
    ) {
        if (isTanpuraRunning) {
            tanpuraJob?.cancel()
            val basePitch = pitches[selectedPitchIdx]
            viewModel.isTanpuraActive.value = true
            runCatching { audioEngine.playSample(basePitch, 0, "TANPURA") }

            tanpuraJob = scope.launch {
                while (isActive && isTanpuraRunning) {
                    val baseStepMs = (60000L / tanpuraTempoBpm.coerceIn(30, 160))
                    for (sIdx in tanpuraStringConfig.indices) {
                        if (!isTanpuraRunning || !isActive) break
                        activeStringIndex = sIdx
                        triggerTanpuraString(sIdx)

                        val waitMs = if (pluckStyle == "MIRAJ") {
                            // Authentic Miraj Jhod: slight pause after Kharaj (last string)
                            if (sIdx == tanpuraStringConfig.lastIndex) {
                                (baseStepMs * 1.35f).toLong()
                            } else {
                                (baseStepMs * 0.82f).toLong()
                            }
                        } else {
                            baseStepMs
                        }
                        delay(waitMs.coerceAtLeast(140L))
                    }
                }
            }
        } else {
            tanpuraJob?.cancel()
            activeStringIndex = -1
            viewModel.isTanpuraActive.value = false
            runCatching { audioEngine.playSample("STOP", 0, "TANPURA") }
        }
        onDispose {
            tanpuraJob?.cancel()
            viewModel.isTanpuraActive.value = false
            runCatching { audioEngine.playSample("STOP", 0, "TANPURA") }
        }
    }

    // Tabla / Tala Theka loop with Laya Multiplier & Metronome Synchronization
    DisposableEffect(
        isTablaRunning,
        currentTaal,
        tablaBpm,
        layaMultiplier,
        isSyncWithMetronome,
        metronomeBpm
    ) {
        if (isTablaRunning) {
            tablaJob?.cancel()
            tablaJob = scope.launch {
                val bolSeq = currentTaal.bolSequence
                while (isActive && isTablaRunning) {
                    for (bIdx in bolSeq.indices) {
                        if (!isTablaRunning || !isActive) break
                        val bol = bolSeq[bIdx]
                        currentMatraIndex = bIdx
                        runCatching { audioEngine.playSample(bol, bIdx, "TABLA") }

                        val baseBpm = if (isSyncWithMetronome) metronomeBpm else tablaBpm
                        val effectiveBpm = (baseBpm * layaMultiplier).roundToInt().coerceIn(20, 480)
                        val delayMs = (60000L / effectiveBpm).coerceAtLeast(95L)
                        delay(delayMs)
                    }
                }
            }
        } else {
            tablaJob?.cancel()
            currentMatraIndex = -1
        }
        onDispose {
            tablaJob?.cancel()
        }
    }

    val scrollMod = if (isFullscreen) Modifier.verticalScroll(rememberScrollState()) else Modifier

    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(scrollMod)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =====================================================================
            // 1. HEADER: RADEL CONCERT BRANDPLATE, RIYAZ TIMER & FULLSCREEN ACTIONS
            // =====================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFB300).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "SAARANG • DIGIRANG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Concert Tanpura & Tabla Studio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${indianShrutiNames[selectedPitchIdx]} • ${currentTaal.name} (${currentTaal.beats} Matras)",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Riyaz Session Timer Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isAnyChannelRunning) Color(0xFF10281E) else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            1.dp,
                            if (isAnyChannelRunning) Color(0xFF00E676).copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Riyaz Timer",
                                tint = if (isAnyChannelRunning) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = formatRiyazDuration(riyazElapsedSeconds),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isAnyChannelRunning) Color(0xFF7AFFBA) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Master Stop All Button when any channel is active
                    if (isAnyChannelRunning) {
                        IconButton(
                            onClick = {
                                isTanpuraRunning = false
                                isTablaRunning = false
                                if (isMetronomeActive) viewModel.toggleMetronome()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F).copy(alpha = 0.22f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = "Stop All",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isFullscreen && onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // =====================================================================
            // 2. VINTAGE RADEL BACKLIT DIGITAL LCD / OLED DISPLAY
            // =====================================================================
            val currentBolText = if (currentMatraIndex in currentTaal.bolSequence.indices) {
                currentTaal.bolSequence[currentMatraIndex]
            } else {
                currentTaal.bolSequence.firstOrNull() ?: "DHA"
            }

            BacklitLcdDisplay(
                pitch = pitches[selectedPitchIdx],
                shrutiLabel = indianShrutiNames[selectedPitchIdx],
                fineTune = fineTuneCents,
                stringMode = firstStringModes[selectedFirstStringMode],
                stringCount = stringCount,
                activeString = activeStringIndex,
                activeSwarLabel = tanpuraStringConfig.getOrNull(activeStringIndex)?.first ?: "--",
                taalName = currentTaal.name,
                matra = currentMatraIndex,
                totalMatras = currentTaal.beats,
                currentBol = currentBolText,
                layaMultiplier = layaMultiplier,
                tanpuraBpm = tanpuraTempoBpm,
                tablaBpm = (if (isSyncWithMetronome) metronomeBpm else tablaBpm),
                metronomeBpm = metronomeBpm,
                isMetronomeActive = isMetronomeActive
            )

            // =====================================================================
            // 3. INTERACTIVE MIRAJ TANPURA STRINGS & MANKA BEADS VISUALIZER
            // =====================================================================
            TanpuraStringsMotionVisualizer(
                stringConfig = tanpuraStringConfig,
                selectedPitch = pitches[selectedPitchIdx],
                activeString = activeStringIndex,
                isVibrating = isTanpuraRunning,
                jawariResonance = jawariResonance,
                onStringTapped = { tappedIdx ->
                    activeStringIndex = tappedIdx
                    triggerTanpuraString(tappedIdx)
                }
            )

            // =====================================================================
            // 4. INTERACTIVE TĀLA MATRA & VIBHAG BOL PLAYHEAD STRIP
            // =====================================================================
            InteractiveTaalMatraStrip(
                taal = currentTaal,
                currentMatraIndex = currentMatraIndex,
                isTablaRunning = isTablaRunning,
                layaMultiplier = layaMultiplier,
                onSelectLaya = { layaMultiplier = it },
                onSelectTempoPreset = { presetBpm -> tablaBpm = presetBpm },
                onToggleLivePads = { showLiveTablaPads = !showLiveTablaPads },
                showLivePads = showLiveTablaPads,
                onMatraBolTapped = { matraIdx, bol ->
                    currentMatraIndex = matraIdx
                    runCatching { audioEngine.playSample(bol, matraIdx, "TABLA") }
                }
            )

            // Optional Collapsible Live Dual-Tabla Touch Pads
            AnimatedVisibility(visible = showLiveTablaPads) {
                UnifiedTablaVisualizer(
                    activeBol = if (isTablaRunning && currentMatraIndex in currentTaal.bolSequence.indices) {
                        currentTaal.bolSequence[currentMatraIndex]
                    } else null,
                    accentColor = Color(0xFF00E676),
                    dayanTonicName = "Sa (${pitches[selectedPitchIdx]})",
                    bayanBassName = "Dagga (Ge/Ghe)",
                    onBolTapped = { tappedBol ->
                        runCatching { audioEngine.playSample(tappedBol, 0, "TABLA") }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                )
            }

            // =====================================================================
            // 5. 3-CHANNEL MASTER CONSOLE (Tanpura | Tabla | Metronome)
            // =====================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3-CHANNEL MASTER MIXER & TEMPO CONSOLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isFemaleUpperOctave) "Register: Female / Flute" else "Register: Male / Inst",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300),
                    modifier = Modifier.clickable { isFemaleUpperOctave = !isFemaleUpperOctave }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Channel 1: Tanpura Drone (Now includes full BPM stepper & Tap Tempo!)
                InstrumentChannelStrip(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "🪕 TANPURA",
                    subtitle = "Key: ${pitches[selectedPitchIdx]} • ${firstStringModes[selectedFirstStringMode].substringBefore(" ")}",
                    gain = viewModel.tanpuraGain.floatValue,
                    onGainChange = { viewModel.tanpuraGain.floatValue = it },
                    bpm = tanpuraTempoBpm,
                    onBpmChange = { tanpuraTempoBpm = it.coerceIn(30, 160) },
                    isRunning = isTanpuraRunning,
                    onToggleRun = { isTanpuraRunning = !isTanpuraRunning },
                    onTapTempo = {
                        lastTanpuraTapTime = handleTapTempo(lastTanpuraTapTime, minBpm = 30, maxBpm = 160) {
                            tanpuraTempoBpm = it
                        }
                    },
                    accentColor = Color(0xFFFFB300)
                )

                // Channel 2: Tabla Machine
                InstrumentChannelStrip(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "🥁 TABLA",
                    subtitle = "${currentTaal.name} (${if (layaMultiplier == 1f) "1x" else "${layaMultiplier}x"})",
                    gain = viewModel.tablaGain.floatValue,
                    onGainChange = { viewModel.tablaGain.floatValue = it },
                    bpm = if (isSyncWithMetronome) metronomeBpm else tablaBpm,
                    onBpmChange = {
                        if (isSyncWithMetronome) viewModel.setMetronomeBpm(it) else tablaBpm = it
                    },
                    isRunning = isTablaRunning,
                    onToggleRun = { isTablaRunning = !isTablaRunning },
                    onTapTempo = {
                        lastTablaTapTime = handleTapTempo(lastTablaTapTime) {
                            if (isSyncWithMetronome) viewModel.setMetronomeBpm(it) else tablaBpm = it
                        }
                    },
                    accentColor = Color(0xFF00E676)
                )

                // Channel 3: Precision Metronome
                InstrumentChannelStrip(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "⏱ METRONOME",
                    subtitle = if (isSyncWithMetronome) "Synced w/ Tabla" else "Independent Pulse",
                    gain = viewModel.metronomeGain.floatValue,
                    onGainChange = { viewModel.metronomeGain.floatValue = it },
                    bpm = metronomeBpm,
                    onBpmChange = { viewModel.setMetronomeBpm(it) },
                    isRunning = isMetronomeActive,
                    onToggleRun = { viewModel.toggleMetronome() },
                    onTapTempo = {
                        lastMetronomeTapTime = handleTapTempo(lastMetronomeTapTime) {
                            viewModel.setMetronomeBpm(it)
                        }
                    },
                    accentColor = Color(0xFF00BCD4)
                )
            }

            // =====================================================================
            // 6. RADEL DIGITAL PARAMETER DOCK (Pitch, Strings, Jawari, Pluck, Tala)
            // =====================================================================
            ParameterDock(
                pitches = pitches,
                indianShrutiNames = indianShrutiNames,
                selectedPitchIdx = selectedPitchIdx,
                onSelectPitch = { selectedPitchIdx = it },
                fineTuneCents = fineTuneCents,
                onFineTuneChange = { fineTuneCents = it },
                firstStringModes = firstStringModes,
                selectedStringMode = selectedFirstStringMode,
                onSelectStringMode = { selectedFirstStringMode = it },
                stringCount = stringCount,
                onSelectStringCount = { stringCount = it },
                pluckStyle = pluckStyle,
                onSelectPluckStyle = { pluckStyle = it },
                jawariResonance = jawariResonance,
                onJawariChange = { jawariResonance = it },
                isFemaleUpperOctave = isFemaleUpperOctave,
                onToggleOctaveRegister = { isFemaleUpperOctave = !isFemaleUpperOctave },
                selectedTradition = selectedTradition,
                onSelectTradition = {
                    selectedTradition = it
                    selectedTaalIdx = 0
                },
                taals = taals,
                selectedTaalIdx = selectedTaalIdx,
                onSelectTaal = { selectedTaalIdx = it },
                isSyncWithMetronome = isSyncWithMetronome,
                onToggleSyncWithMetronome = { isSyncWithMetronome = !isSyncWithMetronome },
                accentColor = accentColor
            )
        }
    }
}

/**
 * Vintage Backlit LCD Display inspired by Digirang & Saarang
 */
@Composable
private fun BacklitLcdDisplay(
    pitch: String,
    shrutiLabel: String,
    fineTune: Float,
    stringMode: String,
    stringCount: Int,
    activeString: Int,
    activeSwarLabel: String,
    taalName: String,
    matra: Int,
    totalMatras: Int,
    currentBol: String,
    layaMultiplier: Float,
    tanpuraBpm: Int,
    tablaBpm: Int,
    metronomeBpm: Int,
    isMetronomeActive: Boolean
) {
    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0A1813), Color(0xFF10241C), Color(0xFF0A1813))
                    )
                )
                .border(1.dp, Color(0xFF285842), RoundedCornerShape(12.dp))
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SECTION 1: Shruti / Key, Fine Tune & Active Plucked Swar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = shrutiLabel.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF55C48E)
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = pitch,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF7AFFBA),
                        fontFamily = FontFamily.Monospace
                    )
                    if (fineTune.roundToInt() != 0) {
                        Text(
                            text = "${if (fineTune > 0) "+" else ""}${fineTune.roundToInt()}¢",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (activeString >= 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFB300).copy(alpha = 0.22f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "STR ${activeString + 1}: $activeSwarLabel",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${stringCount}S • $stringMode",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF55C48E).copy(alpha = 0.85f)
                )
            }

            // SECTION 2: Tāla Beat Playhead, Sam Indicator & Live Bol
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isSam = matra == 0
                val isKhali = matra == totalMatras / 2
                Text(
                    text = when {
                        isSam -> "★ SAM (FIRST BEAT) ★"
                        isKhali -> "○ KHALI (WAVE) ○"
                        else -> "TĀLA MATRA PLAYHEAD"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        isSam -> Color(0xFFFF5252)
                        isKhali -> Color(0xFF29B6F6)
                        else -> Color(0xFF55C48E)
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (matra >= 0) "${matra + 1}" else "--",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            isSam -> Color(0xFFFF5252)
                            isKhali -> Color(0xFF4FC3F7)
                            else -> Color(0xFF7AFFBA)
                        },
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "/ $totalMatras",
                        fontSize = 13.sp,
                        color = Color(0xFF55C48E).copy(alpha = 0.75f),
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSam) Color(0xFFFF5252).copy(alpha = 0.24f) else Color(0xFF1A3A2C)
                    ) {
                        Text(
                            text = currentBol.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSam) Color(0xFFFF8A80) else Color(0xFFFFD54F),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${taalName.uppercase()} • ${if (layaMultiplier == 1f) "BARABAR (1x)" else "${layaMultiplier}x LAYA"}",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7AFFBA)
                )
            }

            // SECTION 3: BPM Telemetry Readout
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("BPM TELEMETRY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF55C48E))
                Text(
                    text = "TAN: $tanpuraBpm BPM",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )
                Text(
                    text = "TAB: ${(tablaBpm * layaMultiplier).roundToInt()} BPM",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7AFFBA)
                )
                Text(
                    text = "MET: $metronomeBpm ${if (isMetronomeActive) "●" else "○"}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isMetronomeActive) Color(0xFF00E676) else Color(0xFF55C48E).copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Interactive Miraj Tanpura Strings Motion Visualizer with Carved Ghoraj Bridge,
 * Red Silk Jiva Threads, Swan Manka Beads, and Tap-to-Pluck String Audition.
 */
@Composable
private fun TanpuraStringsMotionVisualizer(
    stringConfig: List<Pair<String, Int>>,
    selectedPitch: String,
    activeString: Int,
    isVibrating: Boolean,
    jawariResonance: Float,
    onStringTapped: (Int) -> Unit
) {
    val stringCount = stringConfig.size.coerceAtLeast(1)
    val infiniteTransition = rememberInfiniteTransition(label = "tanpura_string_vib")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(85, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tanpura_wave_offset"
    )

    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.LEFT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1C0E06), Color(0xFF2D160A), Color(0xFF1C0E06))
                    )
                )
                .pointerInput(stringCount) {
                    detectTapGestures { tapOffset ->
                        val spacing = size.height / (stringCount + 1f)
                        val tappedIdx = ((tapOffset.y / spacing).roundToInt() - 1).coerceIn(0, stringCount - 1)
                        onStringTapped(tappedIdx)
                    }
                }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            val width = size.width
            val height = size.height
            val leftNutX = width * 0.14f
            val bridgeX = width * 0.84f
            val stringSpacing = height / (stringCount + 1f)

            // Ivory Nut (Targahan) on Left
            drawLine(
                color = Color(0xFFFFF8E1),
                start = Offset(leftNutX, 4f),
                end = Offset(leftNutX, height - 4f),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Carved Bone Ghoraj (Jawari Bridge) on Right
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF8E1), Color(0xFFD7CCC8), Color(0xFF8D6E63))
                ),
                topLeft = Offset(bridgeX - 5.dp.toPx(), 4f),
                size = Size(10.dp.toPx(), height - 8f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            labelPaint.textSize = 10.dp.toPx()

            for (i in 0 until stringCount) {
                val y = stringSpacing * (i + 1)
                val (swarName, _) = stringConfig[i]
                val isActive = activeString == i && isVibrating
                val isBronze = i == 0 || i == stringCount - 1

                val strokeColor = when {
                    isActive -> Color(0xFFFFEB3B)
                    isBronze -> Color(0xFFD4AF37)
                    else -> Color(0xFFDCE0E6)
                }

                val strokeWidth = if (i == stringCount - 1) 2.8f else 1.8f

                // Swar label on left of nut
                labelPaint.color = if (isActive) {
                    android.graphics.Color.rgb(255, 235, 59)
                } else {
                    android.graphics.Color.argb(215, 240, 225, 195)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "S${i + 1} $swarName",
                    0f,
                    y + 3.5.dp.toPx(),
                    labelPaint
                )

                if (isActive) {
                    val points = 32
                    val vibLen = bridgeX - leftNutX
                    val path = Path()
                    val amp = (4.2f * (0.5f + 0.5f * jawariResonance))
                    for (p in 0..points) {
                        val u = p.toFloat() / points
                        val x = leftNutX + u * vibLen
                        val envelope = sin(u * PI.toFloat())
                        val wave = sin(waveOffset + p * 0.35f) * envelope * amp
                        if (p == 0) path.moveTo(x, y + wave) else path.lineTo(x, y + wave)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFFB300).copy(alpha = 0.45f),
                        style = Stroke(width = strokeWidth * 2.6f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = strokeWidth * 1.2f, cap = StrokeCap.Round)
                    )
                } else {
                    drawLine(
                        color = strokeColor.copy(alpha = 0.85f),
                        start = Offset(leftNutX, y),
                        end = Offset(width, y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Red Silk Jiva Thread on Ghoraj Bridge
                drawCircle(
                    color = Color(0xFFD32F2F),
                    radius = 2.4.dp.toPx(),
                    center = Offset(bridgeX - 3.dp.toPx(), y)
                )

                // Ivory Manka Fine-Tuning Swan Bead to the right of Bridge
                val mankaX = bridgeX + (width - bridgeX) * (0.38f + (i % 3) * 0.14f)
                drawOval(
                    color = if (isActive) Color(0xFFFFEB3B) else Color(0xFFFFF8E1),
                    topLeft = Offset(mankaX - 4.5.dp.toPx(), y - 3.dp.toPx()),
                    size = Size(9.dp.toPx(), 6.dp.toPx())
                )
            }
        }
    }
}

/**
 * Interactive Tāla Matra & Vibhag Bol Strip with Sam (X), Khali (0), Laya Multipliers,
 * Classical Tempo Zones (Vilambit / Madhya / Drut), and Tap-to-Audition Bols.
 */
@Composable
private fun InteractiveTaalMatraStrip(
    taal: Taal,
    currentMatraIndex: Int,
    isTablaRunning: Boolean,
    layaMultiplier: Float,
    onSelectLaya: (Float) -> Unit,
    onSelectTempoPreset: (Int) -> Unit,
    onToggleLivePads: () -> Unit,
    showLivePads: Boolean,
    onMatraBolTapped: (Int, String) -> Unit
) {
    val totalBeats = taal.beats.coerceAtLeast(1)
    val khaliBeatIdx = totalBeats / 2

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Laya Multipliers (0.5x, 1x, 2x, 3x, 4x) & Live Tabla Pad Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAYA:",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676),
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    listOf(
                        0.5f to "Ada ½x",
                        1.0f to "1x",
                        2.0f to "Dugun 2x",
                        3.0f to "Tigun 3x",
                        4.0f to "Chaugun 4x"
                    ).forEach { (mult, label) ->
                        val isSel = layaMultiplier == mult
                        Surface(
                            onClick = { onSelectLaya(mult) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFF00E676).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF00E676) else Color.Transparent)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSel) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Classical Tempo Presets (Vilambit, Madhya, Drut)
                    listOf("Vilambit" to 52, "Madhya" to 110, "Drut" to 180).forEach { (name, presetBpm) ->
                        Surface(
                            onClick = { onSelectTempoPreset(presetBpm) },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = name,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                Surface(
                    onClick = onToggleLivePads,
                    shape = RoundedCornerShape(8.dp),
                    color = if (showLivePads) Color(0xFF00E676).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, if (showLivePads) Color(0xFF00E676) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Live Tabla Pads",
                            tint = if (showLivePads) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Pads",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showLivePads) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Scrollable Matra & Bol Cells (Tap any cell to audition its Bol)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(taal.bolSequence) { idx, bol ->
                    val isCurrent = isTablaRunning && currentMatraIndex == idx
                    val isSam = idx == 0
                    val isKhali = idx == khaliBeatIdx

                    val cellBorderColor = when {
                        isCurrent && isSam -> Color(0xFFFF5252)
                        isCurrent -> Color(0xFF00E676)
                        isSam -> Color(0xFFFF5252).copy(alpha = 0.55f)
                        isKhali -> Color(0xFF29B6F6).copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    }
                    val cellBgColor = when {
                        isCurrent && isSam -> Color(0xFFFF5252).copy(alpha = 0.28f)
                        isCurrent -> Color(0xFF00E676).copy(alpha = 0.26f)
                        isSam -> Color(0xFFFF5252).copy(alpha = 0.10f)
                        isKhali -> Color(0xFF29B6F6).copy(alpha = 0.10f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
                    }

                    Surface(
                        onClick = { onMatraBolTapped(idx, bol) },
                        shape = RoundedCornerShape(8.dp),
                        color = cellBgColor,
                        border = BorderStroke(if (isCurrent) 1.8.dp else 1.dp, cellBorderColor),
                        modifier = Modifier.widthIn(min = 48.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val markerTag = when {
                                isSam -> "X • 1"
                                isKhali -> "0 • ${idx + 1}"
                                else -> "${idx + 1}"
                            }
                            Text(
                                text = markerTag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    isSam -> Color(0xFFFF5252)
                                    isKhali -> Color(0xFF4FC3F7)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = bol,
                                fontSize = 11.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstrumentChannelStrip(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    gain: Float,
    onGainChange: (Float) -> Unit,
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    accentColor: Color,
    bpm: Int? = null,
    onBpmChange: ((Int) -> Unit)? = null,
    onTapTempo: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (isRunning) Modifier.border(2.dp, accentColor, MaterialTheme.shapes.large) else Modifier
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRunning) accentColor else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onToggleRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else accentColor,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onError else Color.Black
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isRunning) "STOP" else "START",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "VOL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(gain * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
                Slider(
                    value = gain,
                    onValueChange = onGainChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.height(22.dp)
                )
            }

            if (bpm != null && onBpmChange != null && onTapTempo != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { onBpmChange((bpm - 1).coerceAtLeast(30)) },
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape
                        ) {
                            Text("-", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        }

                        Text(
                            text = "$bpm",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )

                        FilledTonalIconButton(
                            onClick = { onBpmChange((bpm + 1).coerceAtMost(280)) },
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape
                        ) {
                            Text("+", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        }
                    }

                    OutlinedButton(
                        onClick = onTapTempo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("TAP BPM", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * Parameter Dock: Pitch / Shruti, Fine-tuning, String tuning, Pluck Style, Jawari, and Tala selector
 */
@Composable
private fun ParameterDock(
    pitches: List<String>,
    indianShrutiNames: List<String>,
    selectedPitchIdx: Int,
    onSelectPitch: (Int) -> Unit,
    fineTuneCents: Float,
    onFineTuneChange: (Float) -> Unit,
    firstStringModes: List<String>,
    selectedStringMode: Int,
    onSelectStringMode: (Int) -> Unit,
    stringCount: Int,
    onSelectStringCount: (Int) -> Unit,
    pluckStyle: String,
    onSelectPluckStyle: (String) -> Unit,
    jawariResonance: Float,
    onJawariChange: (Float) -> Unit,
    isFemaleUpperOctave: Boolean,
    onToggleOctaveRegister: () -> Unit,
    selectedTradition: String,
    onSelectTradition: (String) -> Unit,
    taals: List<Taal>,
    selectedTaalIdx: Int,
    onSelectTaal: (Int) -> Unit,
    isSyncWithMetronome: Boolean,
    onToggleSyncWithMetronome: () -> Unit,
    accentColor: Color
) {
    var pitchExpanded by remember { mutableStateOf(false) }
    var stringModeExpanded by remember { mutableStateOf(false) }
    var taalExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DRONE & TĀLA PARAMETER DOCK",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = pluckStyle == "MIRAJ",
                    onClick = { onSelectPluckStyle(if (pluckStyle == "MIRAJ") "EVEN" else "MIRAJ") },
                    label = {
                        Text(
                            text = if (pluckStyle == "MIRAJ") "Miraj Pluck" else "Even Pluck",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                FilterChip(
                    selected = isSyncWithMetronome,
                    onClick = onToggleSyncWithMetronome,
                    label = {
                        Text(
                            text = if (isSyncWithMetronome) "SYNCED" else "SYNC MET",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF00E676)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Tradition Selector & Register Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            listOf("HINDUSTANI" to "🇮🇳 HINDUSTANI", "CARNATIC" to "🪕 CARNATIC").forEach { (trad, label) ->
                val isSelected = selectedTradition == trad
                pEQFilterChip(
                    selected = isSelected,
                    onClick = { onSelectTradition(trad) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            pEQFilterChip(
                selected = isFemaleUpperOctave,
                onClick = onToggleOctaveRegister,
                label = {
                    Text(
                        text = if (isFemaleUpperOctave) "Female Pitch (Oct 4)" else "Male Pitch (Oct 3)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // ROW 1: Pitch / Shruti Selector | First String Mode (Pa, Ma, Ni, Sa) | String Count (4S, 5S, 6S)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1.1f)) {
                OutlinedButton(
                    onClick = { pitchExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = indianShrutiNames[selectedPitchIdx],
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(expanded = pitchExpanded, onDismissRequest = { pitchExpanded = false }) {
                    indianShrutiNames.forEachIndexed { i, label ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (i == selectedPitchIdx) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { onSelectPitch(i); pitchExpanded = false }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1.1f)) {
                OutlinedButton(
                    onClick = { stringModeExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = firstStringModes[selectedStringMode],
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                DropdownMenu(expanded = stringModeExpanded, onDismissRequest = { stringModeExpanded = false }) {
                    firstStringModes.forEachIndexed { i, m ->
                        DropdownMenuItem(
                            text = { Text(m, fontWeight = if (i == selectedStringMode) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { onSelectStringMode(i); stringModeExpanded = false }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(4, 5, 6).forEach { count ->
                    val isSelected = stringCount == count
                    pEQFilterChip(
                        selected = isSelected,
                        onClick = { onSelectStringCount(count) },
                        label = {
                            Text(
                                text = "${count}S",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // ROW 2: Fine-Tuning Slider (-50¢..+50¢), Jawari Overtone Slider & Tala Selection Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.95f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "FINE TUNE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        text = "${if (fineTuneCents > 0) "+" else ""}${fineTuneCents.roundToInt()}¢",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Slider(
                    value = fineTuneCents,
                    onValueChange = onFineTuneChange,
                    valueRange = -50f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.height(20.dp)
                )
            }

            Column(modifier = Modifier.weight(0.95f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "JAWARI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        text = "${(jawariResonance * 100).roundToInt()}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                }
                Slider(
                    value = jawariResonance,
                    onValueChange = onJawariChange,
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFB300),
                        activeTrackColor = Color(0xFFFFB300),
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.height(20.dp)
                )
            }

            Box(modifier = Modifier.weight(1.25f)) {
                val currentTala = taals.getOrNull(selectedTaalIdx)
                val currentTalaName = currentTala?.name ?: "Tāla"
                OutlinedButton(
                    onClick = { taalExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "$currentTalaName (${currentTala?.beats ?: 0}b)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = taalExpanded,
                    onDismissRequest = { taalExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    taals.forEachIndexed { i, t ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${t.name} • ${t.beats} matras",
                                        fontWeight = if (i == selectedTaalIdx) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (i == selectedTaalIdx) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = t.bolSequence.take(8).joinToString(" "),
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                            },
                            onClick = { onSelectTaal(i); taalExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

private fun formatRiyazDuration(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        "%d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}

private object ClockHelper {
    fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }
}