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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.Taal
import com.creativeblisslabs.peqlearn.app.ui.screens.INDIAN_TAALS
import com.creativeblisslabs.peqlearn.app.ui.screens.CARNATIC_TALAS
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radel Electronic Tanpura & Tabla Studio with high-fidelity digital display,
 * 4/5/6 string drone simulation, tabla theka machine, and metronome controls.
 */
@Composable
fun RadelTanpuraTablaStudio(
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
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                RadelHardwareEnclosure(
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
        RadelHardwareEnclosure(
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
private fun RadelHardwareEnclosure(
    viewModel: AppViewModel,
    initialIsCarnatic: Boolean,
    accentColor: Color,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val audioEngine = remember { getAudioEngine() }
    val scope = rememberCoroutineScope()

    // --- State: Pitch / Shruti ---
    val pitches = remember {
        listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }
    var selectedPitchIdx by remember { mutableIntStateOf(1) } // C# default
    var fineTuneCents by remember { mutableFloatStateOf(0f) } // -50 to +50 cents

    // --- State: String Tuning Mode (Pa / Ma / Ni) ---
    val firstStringModes = remember { listOf("PA (5th)", "MA (4th)", "NI (7th)") }
    var selectedFirstStringMode by remember { mutableIntStateOf(0) }
    var stringCount by remember { mutableIntStateOf(4) } // 4, 5, or 6 strings

    // --- State: Tanpura Drone ---
    var isTanpuraRunning by remember { mutableStateOf(false) }
    var tanpuraTempoBpm by remember { mutableIntStateOf(60) } // Pluck cycle speed
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
    var currentMatraIndex by remember { mutableIntStateOf(-1) }
    var tablaJob by remember { mutableStateOf<Job?>(null) }
    var isSyncWithMetronome by remember { mutableStateOf(false) }

    // --- State: Metronome ---
    val isMetronomeActive by viewModel.metronomeActive.collectAsState()
    val metronomeBpm by viewModel.metronomeBpm.collectAsState()

    // Synchronize BPM when Sync is active
    LaunchedEffect(isSyncWithMetronome, metronomeBpm) {
        if (isSyncWithMetronome) {
            tablaBpm = metronomeBpm
        }
    }

    // --- Tap Tempo Timestamps ---
    var lastTanpuraTapTime by remember { mutableLongStateOf(0L) }
    var lastTablaTapTime by remember { mutableLongStateOf(0L) }
    var lastMetronomeTapTime by remember { mutableLongStateOf(0L) }

    // Function to calculate tap tempo
    fun handleTapTempo(lastTime: Long, onBpmCalculated: (Int) -> Unit): Long {
        val now = ClockHelper.currentTimeMillis()
        if (lastTime > 0L) {
            val diff = now - lastTime
            if (diff in 200..2500) { // Valid interval for 24-300 BPM
                val calculatedBpm = (60000L / diff).toInt().coerceIn(30, 280)
                onBpmCalculated(calculatedBpm)
            }
        }
        return now
    }

    // Tanpura continuous drone & string pluck loop
    DisposableEffect(isTanpuraRunning, selectedPitchIdx, selectedFirstStringMode, stringCount, tanpuraTempoBpm) {
        if (isTanpuraRunning) {
            tanpuraJob?.cancel()
            val basePitch = pitches[selectedPitchIdx]
            viewModel.isTanpuraActive.value = true
            // Start base continuous drone once
            audioEngine.playSample(basePitch, 0, "TANPURA")

            tanpuraJob = scope.launch {
                val strings = when (stringCount) {
                    5 -> listOf(0, 1, 1, 2, 0)
                    6 -> listOf(0, 1, 1, 2, 2, 0)
                    else -> listOf(0, 1, 1, 2)
                }

                while (isTanpuraRunning) {
                    strings.forEachIndexed { sIdx, _ ->
                        if (!isTanpuraRunning) return@forEachIndexed
                        activeStringIndex = sIdx
                        val stepMs = (60000L / tanpuraTempoBpm.coerceAtLeast(30)) / strings.size
                        delay(stepMs)
                    }
                }
            }
        } else {
            tanpuraJob?.cancel()
            activeStringIndex = -1
            viewModel.isTanpuraActive.value = false
            audioEngine.playSample("STOP", 0, "TANPURA")
        }
        onDispose {
            tanpuraJob?.cancel()
            viewModel.isTanpuraActive.value = false
            audioEngine.playSample("STOP", 0, "TANPURA")
        }
    }

    // Tabla / Tala Theka loop with Metronome Synchronization
    DisposableEffect(isTablaRunning, currentTaal, tablaBpm, isSyncWithMetronome, isMetronomeActive) {
        if (isTablaRunning) {
            tablaJob?.cancel()
            tablaJob = scope.launch {
                val bolSeq = currentTaal.bolSequence
                while (isTablaRunning) {
                    bolSeq.forEachIndexed { bIdx, bol ->
                        if (!isTablaRunning) return@forEachIndexed
                        currentMatraIndex = bIdx
                        audioEngine.playSample(bol, bIdx, "TABLA")

                        val effectiveBpm = if (isSyncWithMetronome) metronomeBpm else tablaBpm
                        val delayMs = (60000L / effectiveBpm.coerceAtLeast(40))
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

    // --- Neural Studio Glass-morphic Enclosure ---

    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullscreen) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
            .padding(if (isFullscreen) 16.dp else 0.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // HEADER: Brandplate, Mode & Fullscreen Action
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
             
                    
                    Text(
                        text = "Electronic Tanpura & Tabla Studio",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .size(34.dp)
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
                                .size(34.dp)
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

            // ==========================================
            // VINTAGE RADEL BACKLIT LCD DISPLAY
            // ==========================================
            BacklitLcdDisplay(
                pitch = pitches[selectedPitchIdx],
                fineTune = fineTuneCents,
                stringMode = firstStringModes[selectedFirstStringMode],
                stringCount = stringCount,
                activeString = activeStringIndex,
                taalName = currentTaal.name,
                matra = currentMatraIndex,
                totalMatras = currentTaal.beats,
                tanpuraBpm = tanpuraTempoBpm,
                tablaBpm = tablaBpm,
                metronomeBpm = metronomeBpm,
                isMetronomeActive = isMetronomeActive,
                accentColor = accentColor
            )

            // ==========================================
            // TANPURA STRING VIBRATION VISUALIZER
            // ==========================================
            TanpuraStringsMotionVisualizer(
                stringCount = stringCount,
                activeString = activeStringIndex,
                isVibrating = isTanpuraRunning,
                accentColor = accentColor
            )

            // ==========================================
            // MAIN 3-CHANNEL DUAL-SLIDER MIXER & TAP TEMPO
            // (Tanpura | Tabla | Metronome)
            // ==========================================
            Text(
                text = "PRECISION 3-CHANNEL MASTER CONSOLE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Channel 1: Tanpura Drone (Volume & State only)
                TanpuraDroneChannelStrip(
                    modifier = Modifier.weight(1f),
                    title = "🪕 TANPURA",
                    gain = viewModel.tanpuraGain.floatValue,
                    onGainChange = { viewModel.tanpuraGain.floatValue = it },
                    isRunning = isTanpuraRunning,
                    onToggleRun = { isTanpuraRunning = !isTanpuraRunning },
                    selectedScale = pitches[selectedPitchIdx],
                    accentColor = Color(0xFFFFB300)
                )

                // Channel 2: Tabla Machine
                InstrumentChannelStrip(
                    modifier = Modifier.weight(1f),
                    title = "🥁 TABLA",
                    gain = viewModel.tablaGain.floatValue,
                    onGainChange = { viewModel.tablaGain.floatValue = it },
                    bpm = tablaBpm,
                    onBpmChange = { tablaBpm = it },
                    isRunning = isTablaRunning,
                    onToggleRun = { isTablaRunning = !isTablaRunning },
                    onTapTempo = {
                        lastTablaTapTime = handleTapTempo(lastTablaTapTime) { tablaBpm = it }
                    },
                    accentColor = Color(0xFF00E676)
                )

                // Channel 3: Precision Metronome
                InstrumentChannelStrip(
                    modifier = Modifier.weight(1f),
                    title = "⏱ METRONOME",
                    gain = viewModel.metronomeGain.floatValue,
                    onGainChange = { viewModel.metronomeGain.floatValue = it },
                    bpm = metronomeBpm,
                    onBpmChange = { viewModel.setMetronomeBpm(it) },
                    isRunning = isMetronomeActive,
                    onToggleRun = { viewModel.toggleMetronome() },
                    onTapTempo = {
                        lastMetronomeTapTime = handleTapTempo(lastMetronomeTapTime) { viewModel.setMetronomeBpm(it) }
                    },
                    accentColor = Color(0xFF00BCD4)
                )
            }

            // ==========================================
            // RADEL DIGITAL PARAMETER DOCK (PITCH, STRINGS, TAAL)
            // ==========================================
            RadelParameterDock(
                pitches = pitches,
                selectedPitchIdx = selectedPitchIdx,
                onSelectPitch = { selectedPitchIdx = it },
                fineTuneCents = fineTuneCents,
                onFineTuneChange = { fineTuneCents = it },
                firstStringModes = firstStringModes,
                selectedStringMode = selectedFirstStringMode,
                onSelectStringMode = { selectedFirstStringMode = it },
                stringCount = stringCount,
                onSelectStringCount = { stringCount = it },
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
 * Vintage Backlit LCD Display inspired by Radel Digirang & Saarang
 */
@Composable
private fun BacklitLcdDisplay(
    pitch: String,
    fineTune: Float,
    stringMode: String,
    stringCount: Int,
    activeString: Int,
    taalName: String,
    matra: Int,
    totalMatras: Int,
    tanpuraBpm: Int,
    tablaBpm: Int,
    metronomeBpm: Int,
    isMetronomeActive: Boolean,
    accentColor: Color
) {
    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFF0F1E19).copy(alpha = 0.9f))
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SECTION 1: Shruti / Key & String Mode
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("SHRUTI / KEY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF55C48E))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(pitch, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF7AFFBA), fontFamily = FontFamily.Monospace)
                    if (fineTune != 0f) {
                        Text(
                            text = "${if (fineTune > 0) "+" else ""}${fineTune.toInt()}¢",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF55C48E),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
                Text(
                    text = "$stringCount-STR • $stringMode",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF55C48E).copy(alpha = 0.8f)
                )
            }

            // SECTION 2: Active String Pluck & Tala Matra Beat
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("TĀLA BEAT PLAYHEAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF55C48E))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (matra >= 0) "${matra + 1}" else "--",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = if (matra == 0) Color(0xFFFF5252) else Color(0xFF7AFFBA),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "/ $totalMatras",
                        fontSize = 14.sp,
                        color = Color(0xFF55C48E).copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = taalName.uppercase(),
                    fontSize = 10.sp,
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
                    text = "TAN: ${tanpuraBpm} BPM",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7AFFBA)
                )
                Text(
                    text = "TAB: ${tablaBpm} BPM",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7AFFBA)
                )
                Text(
                    text = "MET: ${metronomeBpm} ${if (isMetronomeActive) "●" else "○"}",
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
 * Dynamic Tanpura Strings Motion Visualizer with physical sine-wave simulation
 */
@Composable
private fun TanpuraStringsMotionVisualizer(
    stringCount: Int,
    activeString: Int,
    isVibrating: Boolean,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp)) {
            val width = size.width
            val height = size.height
            val stringSpacing = height / (stringCount + 1)

            for (i in 0 until stringCount) {
                val y = stringSpacing * (i + 1)
                val isActive = activeString == i && isVibrating

                val strokeColor = when {
                    isActive -> Color(0xFFFFD54F)
                    i == 0 || i == stringCount - 1 -> Color(0xFFD4AF37) // Brass/Bronze strings
                    else -> Color(0xFFCCCCCC) // Steel strings
                }

                val strokeWidth = if (i == stringCount - 1) 3.5f else 2.0f

                if (isActive) {
                    // Draw vibrating sinusoidal string
                    val points = 30
                    for (p in 0 until points) {
                        val x1 = (p.toFloat() / points) * width
                        val x2 = ((p + 1).toFloat() / points) * width
                        val envelope = sin((p.toFloat() / points) * 3.14159f) // Fixed at pegs/bridge
                        val wave1 = sin(waveOffset + p * 0.4f) * envelope * 4.5f
                        val wave2 = sin(waveOffset + (p + 1) * 0.4f) * envelope * 4.5f

                        drawLine(
                            color = strokeColor,
                            start = Offset(x1, y + wave1),
                            end = Offset(x2, y + wave2),
                            strokeWidth = strokeWidth * 1.5f,
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    // Resting taut string
                    drawLine(
                        color = strokeColor.copy(alpha = if (isVibrating) 0.6f else 0.85f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Clean & Professional Tanpura Drone Channel Strip (Volume & Scale status only, no tap BPM)
 */
@Composable
private fun TanpuraDroneChannelStrip(
    modifier: Modifier = Modifier,
    title: String,
    gain: Float,
    onGainChange: (Float) -> Unit,
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    selectedScale: String,
    accentColor: Color
) {
    ElevatedCard(
        modifier = modifier.then(
            if (isRunning) Modifier.border(2.dp, accentColor, MaterialTheme.shapes.large) else Modifier
        ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title & Run Status
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRunning) accentColor else MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )

            // Play / Stop Button - Material 3 Expressive Button
            Button(
                onClick = onToggleRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else accentColor,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "STOP" else "START",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }

            // Volume Control Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "VOLUME",
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
                    modifier = Modifier.height(24.dp)
                )
            }

            // Scale Display Status Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accentColor.copy(alpha = 0.14f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "KEY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        selectedScale,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Precision Channel Strip for Tabla or Metronome with Material 3 Expressive styling
 */
@Composable
private fun InstrumentChannelStrip(
    modifier: Modifier = Modifier,
    title: String,
    gain: Float,
    onGainChange: (Float) -> Unit,
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    onTapTempo: () -> Unit,
    accentColor: Color
) {
    ElevatedCard(
        modifier = modifier.then(
            if (isRunning) Modifier.border(2.dp, accentColor, MaterialTheme.shapes.large) else Modifier
        ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title & Run Status
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRunning) accentColor else MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )

            // Play / Stop Button - Material 3 Expressive Button
            Button(
                onClick = onToggleRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else accentColor,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "STOP" else "START",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }

            // Volume Control Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "VOLUME",
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
                    modifier = Modifier.height(24.dp)
                )
            }

            // BPM Adjuster Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onBpmChange((bpm - 1).coerceAtLeast(30)) },
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }

                Text(
                    text = "$bpm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                )

                FilledTonalIconButton(
                    onClick = { onBpmChange((bpm + 1).coerceAtMost(280)) },
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }

            // Dedicated TAP TEMPO Button
            OutlinedButton(
                onClick = onTapTempo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                ),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("TAP BPM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            }
        }
    }
}

/**
 * Radel Parameter Dock: Pitch / Shruti, Fine-tuning, String tuning, and Tala selector
 */
@Composable
private fun RadelParameterDock(
    pitches: List<String>,
    selectedPitchIdx: Int,
    onSelectPitch: (Int) -> Unit,
    fineTuneCents: Float,
    onFineTuneChange: (Float) -> Unit,
    firstStringModes: List<String>,
    selectedStringMode: Int,
    onSelectStringMode: (Int) -> Unit,
    stringCount: Int,
    onSelectStringCount: (Int) -> Unit,
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

    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ELECTRONIC CALIBRATION & DRONE TUNING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                // Sync with Metronome beat toggle
                FilterChip(
                    selected = isSyncWithMetronome,
                    onClick = onToggleSyncWithMetronome,
                    label = {
                        Text(
                            text = if (isSyncWithMetronome) "SYNCED WITH METRONOME" else "SYNC METRONOME",
                            fontSize = 9.sp,
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

            // Tradition Selector: Hindustani vs Carnatic
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            }

            // ROW 1: Pitch Selector & String Mode Dropdown & String Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pitch Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { pitchExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    ) {
                        Text("Key: ${pitches[selectedPitchIdx]}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = pitchExpanded, onDismissRequest = { pitchExpanded = false }) {
                        pitches.forEachIndexed { i, p ->
                            DropdownMenuItem(
                                text = { Text(p, fontWeight = if (i == selectedPitchIdx) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { onSelectPitch(i); pitchExpanded = false }
                            )
                        }
                    }
                }

                // First String Mode (Pa, Ma, Ni)
                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedButton(
                        onClick = { stringModeExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    ) {
                        Text(firstStringModes[selectedStringMode], fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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

                // String Count Selector (4, 5, 6)
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

            // ROW 2: Fine-Tuning Slider & Tala Selection Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fine-tune slider (-50¢ to +50¢)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("FINE TUNE (CENTS)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = "${if (fineTuneCents > 0) "+" else ""}${fineTuneCents.toInt()}¢",
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

                // Tala Selector Dropdown with beats and count
                Box(modifier = Modifier.weight(1.2f)) {
                    val currentTala = taals.getOrNull(selectedTaalIdx)
                    val currentTalaName = currentTala?.name ?: "Tāla"
                    OutlinedButton(
                        onClick = { taalExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "$currentTalaName (${currentTala?.beats ?: 0}b)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
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
                                            text = "${t.name} • ${t.beats} beats",
                                            fontWeight = if (i == selectedTaalIdx) FontWeight.Black else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (i == selectedTaalIdx) accentColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = t.bolSequence.take(6).joinToString(" ") + "...",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
}

/**
 * Universal clock epoch helper compatible with Kotlin Multiplatform
 */
private object ClockHelper {
    fun currentTimeMillis(): Long {
        return try {
            kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        } catch (_: Throwable) {
            0L
        }
    }
}
