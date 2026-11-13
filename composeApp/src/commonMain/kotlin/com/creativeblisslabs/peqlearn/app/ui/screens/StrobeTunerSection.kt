package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.security.getPermissionManager
import com.creativeblisslabs.peqlearn.app.tuner.*
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlin.math.abs

/**
 * PRODUCTION-GRADE PRECISION CHROMATIC & INSTRUMENT TUNER (Chroma-Inspired)
 * 
 * Features:
 * 1. Real-time microphone listening with PermissionManager check
 * 2. Arc Gauge with dynamic needle, cents deviation (-50..+50) & "Tune Up / Tune Down / In Tune" guidance
 * 3. Auto String Detection Mode vs Manual Peg Selection
 * 4. Tunings Selector Dialog (Favorites, Categories: Common, Power, Open, Extended, Drop)
 * 5. Add Custom Tuning Dialog (Add/Remove strings, Tune Up/Down per string)
 * 6. Tuner Settings (Inline vs Side-by-side strings, Simple vs Semitones vs Cents, Sound feedback)
 * 7. Chromatic Mode & Instrument Modes (Guitar, 7-String, Bass, Ukulele, Violin)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrobeTunerSection(viewModel: AppViewModel) {
    val permissionManager = remember { getPermissionManager() }
    val tunerEngine = remember { getTunerEngine() }

    var isListening by remember { mutableStateOf(tunerEngine.isListening()) }
    var showPermissionAlert by remember { mutableStateOf(false) }

    // Tuner States
    var isAutoMode by remember { mutableStateOf(true) }
    var selectedTuning by remember { mutableStateOf(TuningsCatalog.BUILT_IN_TUNINGS.first()) }
    var selectedStringIdx by remember { mutableIntStateOf(0) }
    var isChromaticMode by remember { mutableStateOf(false) }

    // Live Mic Pitch State
    var detectedFreq by remember { mutableFloatStateOf(0f) }
    var detectedNote by remember { mutableStateOf("--") }
    var centsDeviation by remember { mutableIntStateOf(0) }
    var micAmplitude by remember { mutableFloatStateOf(0f) }

    // Dialogs
    var showTuningDialog by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Custom Tunings List
    val customTunings = remember { mutableStateListOf<InstrumentTuning>() }
    val favoriteTuningIds = remember { mutableStateListOf("guitar_standard", "guitar_drop_d", "bass_standard", "ukulele_standard") }

    // Tuner Settings Options
    var stringLayout by remember { mutableStateOf("Inline") } // "Inline" vs "Side by side"
    var tuningDisplayType by remember { mutableStateOf("Cents") } // "Simple", "Semitones", "Cents"
    var playSoundOnStringSelect by remember { mutableStateOf(true) }
    var playInTuneSound by remember { mutableStateOf(false) }

    // Start/Stop Mic Listener
    fun toggleListening() {
        if (!permissionManager.hasMicrophonePermission()) {
            permissionManager.requestMicrophonePermission()
            showPermissionAlert = true
            return
        }

        if (isListening) {
            tunerEngine.stopListening()
            isListening = false
        } else {
           tunerEngine.startListening { freq, note, cents, amp ->

    if (!freq.isFinite() || freq <= 0f) {
        detectedFreq = 0f
        detectedNote = "--"
        centsDeviation = 0
        micAmplitude = 0f
        return@startListening
    }

    detectedFreq = freq
    detectedNote = note.ifBlank { "--" }
    centsDeviation = cents.coerceIn(-100, 100)
    micAmplitude = amp.takeIf { it.isFinite() } ?: 0f

    if (
        isAutoMode &&
        !isChromaticMode &&
        selectedTuning.stringNotes.isNotEmpty() &&
        detectedNote != "--"
    ) {
        val targetIdx = selectedTuning.stringNotes.indexOfFirst {
            it.dropLastWhile(Char::isDigit) ==
                detectedNote.dropLastWhile(Char::isDigit)
        }

        if (targetIdx >= 0) {
            selectedStringIdx = targetIdx
        }
    }
}
            isListening = true
        }
    }

  DisposableEffect(tunerEngine) {
    onDispose {
        tunerEngine.stopListening()
    }
}

    val primaryColor = MaterialTheme.colorScheme.primary
    val inTuneColor = Color(0xFF00E676)
    val tuneUpColor = Color(0xFFFF9100) // Need to tighten string
    val tuneDownColor = Color(0xFF29B6F6) // Need to loosen string



    val isInTune = abs(centsDeviation) <= 3 && detectedFreq > 0f

    val gaugeColor = when {
        isInTune -> inTuneColor
        centsDeviation < -3 -> tuneUpColor
        centsDeviation > 3 -> tuneDownColor
        else -> Color.White.copy(alpha = 0.4f)
    }

    val tuningGuidanceText = when {
        detectedFreq <= 0f -> "Pluck a string"
        isInTune -> "In Tune"
        centsDeviation < -3 -> "Tune Up"
        else -> "Tune Down"
    }

val safeSelectedStringIdx = selectedStringIdx.coerceIn(
    0,
    (selectedTuning.stringNotes.size - 1).coerceAtLeast(0)
)

val currentTargetNote = when {
    isChromaticMode -> detectedNote
    selectedTuning.stringNotes.isEmpty() -> "--"
    else -> selectedTuning.stringNotes[safeSelectedStringIdx]
}
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F1710), // Deep emerald/forest dark theme matching chroma
        border = BorderStroke(1.dp, Color(0xFF1E3824))
    ) {
      Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
            // TOP BAR: Tuner Title + Quick Actions (Chromatic toggle, Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Chromatic Tuner",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = inTuneColor
                    )
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(inTuneColor)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { isChromaticMode = !isChromaticMode }) {
                        Icon(
                            imageVector = if (isChromaticMode) Icons.Default.MusicNote else Icons.Outlined.MusicNote,
                            contentDescription = "Chromatic Toggle",
                            tint = if (isChromaticMode) inTuneColor else Color.White.copy(0.6f)
                        )
                    }
                    IconButton(onClick = { toggleListening() }) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Mic Toggle",
                            tint = if (isListening) inTuneColor else Color.White.copy(0.6f)
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Tuner Settings", tint = Color.White.copy(0.6f))
                    }
                }
            }

          Box(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 140.dp, max = 160.dp),
    contentAlignment = Alignment.Center
) {
    TunerArcGauge(
        centsDeviation = centsDeviation,
        gaugeColor = gaugeColor
    )

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val deviationText = when (tuningDisplayType) {
            "Simple" -> currentTargetNote
            "Semitones" -> {
                val st = centsDeviation / 100f
                if (st > 0f) "+%.1f st".format(st) else "%.1f st".format(st)
            }
            else -> if (centsDeviation > 0) "+$centsDeviation ¢" else "$centsDeviation ¢"
        }

        Text(
            text = if (tuningDisplayType == "Simple")
                currentTargetNote
            else deviationText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = gaugeColor
        )

        Text(
            text = tuningGuidanceText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f)
        )

        if (detectedFreq.isFinite() && detectedFreq > 0f) {
            Text(
                text = "%.1f Hz".format(detectedFreq),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

            // AUTO TOGGLE ROW
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(0.8f),
                    letterSpacing = 1.sp
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

            // STRINGS LIST / PEGS SELECTOR (Inline or Side-by-Side)
            if (!isChromaticMode) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isChromaticMode && selectedTuning.stringNotes.isNotEmpty()) {

                    selectedTuning.stringNotes.forEachIndexed { idx, stringNote ->
                        val isSelected = selectedStringIdx == idx
                        val isCurrentStringInTune = isSelected && isInTune

                        Row(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tune down button (-)
                            IconButton(
                                onClick = {
                                    val shifted = TuningsCatalog.shiftNote(stringNote, -1)
                                    val updatedNotes = selectedTuning.stringNotes.toMutableList().apply { set(idx, shifted) }
                                    selectedTuning = selectedTuning.copy(stringNotes = updatedNotes)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.6f))
                            }

                            // String Bubble Button
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = when {
                                    isCurrentStringInTune -> inTuneColor.copy(alpha = 0.35f)
                                    isSelected -> Color(0xFF243B53) // Highlighted blue peg as in screenshots
                                    else -> Color(0xFF142418)
                                },
                                border = BorderStroke(
                                    1.2.dp,
                                    when {
                                        isCurrentStringInTune -> inTuneColor
                                        isSelected -> Color(0xFF486581)
                                        else -> Color(0xFF1E3824)
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .padding(horizontal = 8.dp)
                                    .clickable {
                                        selectedStringIdx = idx
                                        isAutoMode = false
                                       if (playSoundOnStringSelect) {
            val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val pitchPart = stringNote.dropLastWhile { it.isDigit() || it == '-' }
            val octPart = stringNote.takeLastWhile { it.isDigit() || it == '-' }.toIntOrNull() ?: 4
            val pc = noteNames.indexOf(pitchPart).takeIf { it >= 0 } ?: 0
            val targetMidi = ((octPart + 1) * 12 + pc).coerceIn(21, 108)
            runCatching {
                viewModel.audioEngine.setInstrument("GUITAR")
                viewModel.audioEngine.noteOn(targetMidi, 0.85f)
            }
        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stringNote,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.White else Color.White.copy(0.8f)
                                    )
                                }
                            }

                            // Tune up button (+)
                            IconButton(
                                onClick = {
                                    val shifted = TuningsCatalog.shiftNote(stringNote, 1)
                                    val updatedNotes = selectedTuning.stringNotes.toMutableList().apply { set(idx, shifted) }
                                    selectedTuning = selectedTuning.copy(stringNotes = updatedNotes)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.6f))
                            }
                        }
                    }
                }
                }
            } else {
                // Chromatic 12-Tone Quick Grid
                val chromaticNotes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chromaticNotes.forEach { note ->
                        val isDetected = detectedNote.startsWith(note)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDetected) inTuneColor.copy(0.3f) else Color(0xFF142418),
                            border = BorderStroke(1.dp, if (isDetected) inTuneColor else Color(0xFF1E3824)),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(note, fontWeight = FontWeight.Bold, color = if (isDetected) inTuneColor else Color.White)
                            }
                        }
                    }
                }
            }

            // BOTTOM TUNING DROPDOWN SELECTOR BAR
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
                            text = if (isChromaticMode) "Chromatic" else selectedTuning.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isChromaticMode) "Tune to any note" else "${selectedTuning.instrument} • ${selectedTuning.stringNotes.reversed().joinToString(", ")}",
                            fontSize = 11.sp,
                            color = Color.White.copy(0.55f)
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Tuning", tint = inTuneColor)
                }
            }
        }
    }

    // ========================================================================
    // 1. SELECT TUNING MODAL (Favorites, Categorized, New Tuning button)
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
                    Text("Select Tuning", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showTuningDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                val allTunings = TuningsCatalog.BUILT_IN_TUNINGS + customTunings
                var filterCategory by remember { mutableStateOf("All") }
                var filterInstrument by remember { mutableStateOf("All") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Instrument filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Guitar", "Bass", "Ukulele", "Violin").forEach { inst ->
                            FilterChip(
                                selected = filterInstrument == inst,
                                onClick = { filterInstrument = inst },
                                label = { Text(inst, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Favorites Section
                    Text("Favourites", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    allTunings.filter { favoriteTuningIds.contains(it.id) }.forEach { tuning ->
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

                    // All Tunings Section
                    Text("All Tunings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    allTunings
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
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Tuning", color = Color.Black, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Strings", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    // Add string button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { customStrings.add(0, "E4") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("+ Add String")
                        }
                    }

                    // Strings list with +/- tuning controls
                    customStrings.forEachIndexed { sIdx, note ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { customStrings.removeAt(sIdx) }) {
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
                val canSaveTuning =
    customName.isNotBlank() &&
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
    colors = ButtonDefaults.buttonColors(
        containerColor = inTuneColor
    )
) {
    Text("Save", color = Color.Black)
}
            }
        )
    }

    // ========================================================================
    // 3. SETTINGS DIALOG (Chroma Settings Replica)
    // ========================================================================
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // String Layout
                    Text("String Layout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    listOf("Inline", "Side by side").forEach { opt ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { stringLayout = opt },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = stringLayout == opt, onClick = { stringLayout = opt })
                            Spacer(Modifier.width(8.dp))
                            Text(opt, fontSize = 13.sp)
                        }
                    }

                    // Tuning Display
                    Text("Tuning Display", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    listOf("Simple", "Semitones", "Cents").forEach { opt ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { tuningDisplayType = opt },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = tuningDisplayType == opt, onClick = { tuningDisplayType = opt })
                            Spacer(Modifier.width(8.dp))
                            Text(opt, fontSize = 13.sp)
                        }
                    }

                    // Sound Switches
                    Text("Sound", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inTuneColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Play sound on string selection", fontSize = 12.sp)
                        Switch(checked = playSoundOnStringSelect, onCheckedChange = { playSoundOnStringSelect = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Play in-tune sound chime", fontSize = 12.sp)
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

    // Permission Alert Dialog
    if (showPermissionAlert) {
        AlertDialog(
            onDismissRequest = { showPermissionAlert = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("To detect instrument pitch and tune your strings, please allow microphone access.") },
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
        color = if (isSelected) Color(0xFF1E3824) else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E676) else Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tuning.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "${tuning.instrument} • ${tuning.stringNotes.reversed().joinToString(", ")}",
                    fontSize = 10.sp,
                    color = Color.White.copy(0.6f)
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFF29B6F6) else Color.White.copy(0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
@Composable
private fun TunerArcGauge(
    centsDeviation: Int,
    gaugeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp)
    ) {
        val strokeWidth = 12.dp.toPx()
        val dotRadius = 7.dp.toPx()

        // Keep the complete arc inside the Canvas.
        val horizontalPadding = strokeWidth / 2f + dotRadius
        val topPadding = strokeWidth / 2f + dotRadius
        val bottomPadding = strokeWidth / 2f + dotRadius

        val availableWidth = size.width - horizontalPadding * 2
        val availableHeight = size.height - topPadding - bottomPadding

        // Fit a semicircular-style gauge into available bounds.
        val radius = minOf(
            availableWidth / 2f,
            availableHeight
        )

        val centerX = size.width / 2f
        val centerY = size.height - bottomPadding

        val topLeft = androidx.compose.ui.geometry.Offset(
            centerX - radius,
            centerY - radius
        )

        val arcSize = androidx.compose.ui.geometry.Size(
            radius * 2f,
            radius * 2f
        )

        // Background arc.
        drawArc(
            color = Color(0xFF203B2A),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )

        // Deviation indicator.
        val safeCents = centsDeviation.coerceIn(-50, 50)
        val fraction = (safeCents + 50f) / 100f

        val angle = Math.toRadians(
            (200f + fraction * 140f).toDouble()
        )

        val dotCenter = androidx.compose.ui.geometry.Offset(
            x = centerX + radius * kotlin.math.cos(angle).toFloat(),
            y = centerY + radius * kotlin.math.sin(angle).toFloat()
        )

        drawCircle(
            color = gaugeColor.copy(alpha = 0.2f),
            radius = dotRadius * 1.9f,
            center = dotCenter
        )

        drawCircle(
            color = gaugeColor,
            radius = dotRadius,
            center = dotCenter
        )
    }
}