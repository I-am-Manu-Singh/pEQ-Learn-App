package com.creativeblisslabs.peqlearn.app.ui.screens

import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.persistSession
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.playMediaItem
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random


@Composable
fun LiveChordHud(
    detectedChord: com.creativeblisslabs.peqlearn.app.util.LiveChordDetector.DetectedChord?,
    primaryColor: Color = Color(0xFF00E676),
    modifier: Modifier = Modifier
) {
    if (detectedChord != null && detectedChord.chordQuality != "Note") {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF14171E).copy(alpha = 0.90f),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = detectedChord.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsSheet(
    viewModel: AppViewModel,
    notationFormat: String = viewModel.notationFormat.value,
    onFormatChange: (String) -> Unit = { viewModel.notationFormat.value = it },
    showLetters: Boolean = viewModel.showPianoKeyLabels.value,
    onShowLettersChange: (Boolean) -> Unit = { viewModel.showPianoKeyLabels.value = it },
    showNotationNoteNames: Boolean = viewModel.showNotationNoteNames.value,
    onShowNotationNoteNamesChange: (Boolean) -> Unit = {
        viewModel.showNotationNoteNames.value = it
    },
    showLyrics: Boolean = true,
    onShowLyricsChange: (Boolean) -> Unit = {},
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by rememberSaveable { mutableIntStateOf(initialTab) }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.persistSession()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFFFD700).copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Studio & Workspace Settings",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        viewModel.persistSession()
                        onDismiss()
                    }
                ) {
                    Text(
                        "DONE",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Tab Navigation Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Notation Viewer", "Keyboard & Visualizer")
                tabs.forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFD700) else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.7f
                            )
                        )
                    }
                }
            }

            if (activeTab == 0) {
                // =========================================================================
                // TAB 0: NOTATION VIEWER SETTINGS
                // =========================================================================
                SettingsSection(title = "Hands & Notation Visibility") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Right Hand Notes (Treble)",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Show/hide right hand across Staff, Tab, Keyboard & Waterfall",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = !viewModel.isRightHandHidden.value,
                                onCheckedChange = { viewModel.isRightHandHidden.value = !it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Left Hand Notes (Bass)",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Show/hide left hand across Staff, Tab, Keyboard & Waterfall",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = !viewModel.isLeftHandHidden.value,
                                onCheckedChange = { viewModel.isLeftHandHidden.value = !it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }
                    }
                }

                SettingsSection(title = "Notation Display Format") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Format Mode",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            val formats = listOf("Staff", "Tab", "Both")
                            Row(
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        0.05f
                                    ), RoundedCornerShape(8.dp)
                                ).padding(3.dp)
                            ) {
                                formats.forEach { f ->
                                    val currentFmt =
                                        viewModel.notationFormat.value.trim().lowercase()
                                    val active = when (f) {
                                        "Staff" -> currentFmt == "staff" || currentFmt == "staff only"
                                        "Tab" -> currentFmt == "tab" || currentFmt == "tab only"
                                        "Both" -> currentFmt == "both" || currentFmt == "both viewers"
                                        else -> false
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) Color(0xFFFFD700) else Color.Transparent)
                                            .clickable {
                                                val target = when (f) {
                                                    "Both" -> "Both Viewers"
                                                    else -> "$f Only"
                                                }
                                                viewModel.notationFormat.value = target
                                                onFormatChange(target)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            f,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.Black else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Notehead Note Names",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.showNotationNoteNames.value,
                                onCheckedChange = {
                                    viewModel.showNotationNoteNames.value = it
                                    onShowNotationNoteNamesChange(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Suggested Piano Fingerings (1–5)",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Stack vertical RH/LH finger numbers above treble & below bass chords",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = viewModel.showSuggestedFingerings.value,
                                onCheckedChange = {
                                    viewModel.showSuggestedFingerings.value = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.3f)
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Split Staff (Grand Staff)",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.isSplitStaff.value,
                                onCheckedChange = { viewModel.isSplitStaff.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Vertical Scrolling",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.isVerticalScroll.value,
                                onCheckedChange = {
                                    viewModel.isVerticalScroll.value = it
                                    viewModel.scrollDirectionHorizontal.value = !it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "OpenSheetMusicDisplay (Web Engine)",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.useOsmdRenderer.value,
                                onCheckedChange = { viewModel.useOsmdRenderer.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }
                    }
                }

                SettingsSection(title = "Key Signature & Transposition") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Active Key Signature",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                            fontSize = 13.sp
                        )
                        val keys = listOf(
                            "C Major",
                            "G Major",
                            "D Major",
                            "A Major",
                            "E Major",
                            "B Major",
                            "F# Major",
                            "F Major",
                            "B♭ Major",
                            "E♭ Major",
                            "A♭ Major",
                            "D♭ Major",
                            "A Minor",
                            "E Minor",
                            "D Minor"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            keys.forEach { key ->
                                val selected = viewModel.selectedKeySignature.value == key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(
                                                0.06f
                                            )
                                        )
                                        .clickable {
                                            viewModel.selectedKeySignature.value = key
                                            // Map key name to KeySignature enum and update score
                                            val keySigEnum = when (key) {
                                                "G Major", "E Minor" -> KeySignature.G_MAJOR
                                                "D Major", "B Minor" -> KeySignature.D_MAJOR
                                                "A Major", "F# Minor" -> KeySignature.A_MAJOR
                                                "E Major", "C# Minor" -> KeySignature.E_MAJOR
                                                "B Major", "G# Minor" -> KeySignature.B_MAJOR
                                                "F# Major" -> KeySignature.F_SHARP_MAJOR
                                                "F Major", "D Minor" -> KeySignature.F_MAJOR
                                                "B♭ Major", "G Minor" -> KeySignature.B_FLAT_MAJOR
                                                "E♭ Major", "C Minor" -> KeySignature.E_FLAT_MAJOR
                                                "A♭ Major", "F Minor" -> KeySignature.A_FLAT_MAJOR
                                                "D♭ Major", "Bb Minor" -> KeySignature.D_FLAT_MAJOR
                                                else -> KeySignature.C_MAJOR
                                            }
                                            val current = viewModel.currentScore.value as? Score
                                            if (current != null) {
                                                viewModel.currentScore.value = current.copy(
                                                    keySignature = keySigEnum,
                                                    measures = current.measures.map {
                                                        it.copy(
                                                            keySignature = keySigEnum
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        key,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StepperControl(
                                label = "Transposition",
                                value = viewModel.transposition.value,
                                onValueChange = { viewModel.transposition.value = it }
                            )
                            StepperControl(
                                label = "Octave Shift",
                                value = viewModel.octaveShift.value,
                                onValueChange = { viewModel.octaveShift.value = it }
                            )
                        }
                    }
                }

                SettingsSection(title = "Time Signature & Quantization") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Time Signature",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                            fontSize = 13.sp
                        )
                        val timeSigs = listOf("4/4", "3/4", "2/4", "6/8", "12/8", "5/4", "7/8")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            timeSigs.forEach { ts ->
                                val selected = viewModel.selectedTimeSignature.value == ts
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(
                                                0.06f
                                            )
                                        )
                                        .clickable {
                                            viewModel.selectedTimeSignature.value = ts
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        ts,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text(
                            "Quantize Grid",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                            fontSize = 13.sp
                        )
                        val quantizeOptions = listOf("1/4", "1/8", "1/16", "1/32")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            quantizeOptions.forEach { q ->
                                val selected = viewModel.quantizeDivision.value == q
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(
                                                0.06f
                                            )
                                        )
                                        .clickable { viewModel.quantizeDivision.value = q }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        q,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text(
                            "Target Notation Instrument",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                            fontSize = 13.sp
                        )
                        val instruments = listOf(
                            "Piano",
                            "Harmonium",
                            "Guitar",
                            "7-String Guitar",
                            "Violin",
                            "Flute",
                            "Bass",
                            "Drum kit",
                            "Ukulele"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            instruments.forEach { inst ->
                                val instType = ChordInstrumentType.fromNameOrNull(inst)
                                val currentType = ChordInstrumentType.fromNameOrNull(viewModel.selectedInstrument.value)
                                val selected = (instType != null && instType == currentType) || viewModel.selectedInstrument.value == inst
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(
                                                0.06f
                                            )
                                        )
                                        .clickable {
                                            val canonicalTitle = instType?.title ?: inst
                                            viewModel.selectedInstrument.value = canonicalTitle
                                            val soundName = instType?.soundEngineName
                                            if (soundName != null) {
                                                try {
                                                    viewModel.audioEngine.setInstrument(soundName)
                                                } catch (e: Exception) {
                                                    // ignore engine setInstrument error
                                                }
                                            }
                                            // Piano88 Screen Rule: Guitar shows Tab, Piano shows Staff notation
                                            if (inst.contains("Guitar") || inst == "Ukulele") {
                                                viewModel.notationFormat.value = "Tab Only"
                                                onFormatChange("Tab Only")
                                            } else if (inst == "Piano" || inst == "Harmonium") {
                                                viewModel.notationFormat.value = "Staff Only"
                                                onFormatChange("Staff Only")
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        inst,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // TAB 1: KEYBOARD & VISUALIZER STUDIO SETTINGS (MIDIVisualizer-master Parity)
                // =========================================================================
                // Visualizer Mode Selector
                SettingsSection(title = "VISUALIZER DISPLAY MODE") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Choose what visualizer to show on screen",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            fontSize = 12.sp
                        )
                        val visualizers = listOf(
                            "Split (Notation + Waterfall)" to "Split",
                            "Keyboard + Waterfall" to "KeyboardWaterfall",
                            "Waterfall Only" to "Waterfall",
                            "Notation Only" to "Notation",
                            "Keyboard Only" to "Keyboard"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            visualizers.forEach { (label, modeKey) ->
                                val isSelected = viewModel.visualizerLayoutMode.value == modeKey
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFFFFD700).copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.04f
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFFFD700) else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.visualizerLayoutMode.value = modeKey
                                            when (modeKey) {
                                                "Waterfall" -> {
                                                    viewModel.showWaterfall.value = true
                                                }

                                                "Notation" -> {
                                                    viewModel.showWaterfall.value = false
                                                }

                                                "Keyboard" -> {
                                                    viewModel.showWaterfall.value = false
                                                }

                                                "Split", "KeyboardWaterfall" -> {
                                                    viewModel.showWaterfall.value = true
                                                }
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. NOTE STYLE & COLORS (Viewer::showNoteOptions)
                SettingsSection(title = "NOTE STYLE & APPEARANCE") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show Falling Notes",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.showWaterfall.value,
                                onCheckedChange = { viewModel.showWaterfall.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Falling Note Stream / Visualizer Height: ${viewModel.keyboardHeightFactor.floatValue.toInt()}dp",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Slider(
                                value = viewModel.keyboardHeightFactor.floatValue,
                                onValueChange = {
                                    viewModel.keyboardHeightFactor.floatValue = it
                                },
                                valueRange = 100f..260f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Vertical Scale (Speed)",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Slow", "Normal", "Fast", "Extreme").forEach { spd ->
                                    val active = viewModel.fallingNotesSpeed.value == spd
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (active) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(
                                                    0.08f
                                                )
                                            )
                                            .clickable {
                                                viewModel.fallingNotesSpeed.value = spd
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = spd,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            color = if (active) Color.Black else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Corner Roundness: ${viewModel.noteCornerRadius.floatValue.toInt()}px",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Slider(
                                value = viewModel.noteCornerRadius.floatValue,
                                onValueChange = { viewModel.noteCornerRadius.floatValue = it },
                                valueRange = 0f..14f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Edge Thickness: ${(viewModel.noteEdgeThickness.floatValue * 10).toInt() / 10.0}px",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Slider(
                                value = viewModel.noteEdgeThickness.floatValue,
                                onValueChange = { viewModel.noteEdgeThickness.floatValue = it },
                                valueRange = 0.5f..5.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Style Note Heads as Ovals",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.noteStyleOval.value,
                                onCheckedChange = { viewModel.noteStyleOval.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }
                    }
                }

                // 2. PARTICLES & SPARKS (Viewer::showParticleOptions)
                SettingsSection(title = "PARTICLES & SPARKS") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show Particles",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.showWaterfallParticles.value,
                                onCheckedChange = {
                                    viewModel.showWaterfallParticles.value = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        if (viewModel.showWaterfallParticles.value) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Intensity / Count: ${viewModel.particleCount.intValue} sparks",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = viewModel.particleCount.intValue.toFloat(),
                                    onValueChange = {
                                        viewModel.particleCount.intValue = it.toInt()
                                    },
                                    valueRange = 4f..48f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700)
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Particle Speed: ${(viewModel.particleSpeed.floatValue * 10).toInt() / 10.0}x",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = viewModel.particleSpeed.floatValue,
                                    onValueChange = { viewModel.particleSpeed.floatValue = it },
                                    valueRange = 1.0f..10.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700)
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Particle Size: ${(viewModel.particleScale.floatValue * 10).toInt() / 10.0}x",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = viewModel.particleScale.floatValue,
                                    onValueChange = { viewModel.particleScale.floatValue = it },
                                    valueRange = 0.4f..2.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700)
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Spread / Expansion: ${(viewModel.particleExpansion.floatValue * 10).toInt() / 10.0}x",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = viewModel.particleExpansion.floatValue,
                                    onValueChange = {
                                        viewModel.particleExpansion.floatValue = it
                                    },
                                    valueRange = 0.5f..3.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700)
                                    )
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Sync Color With Notes",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = viewModel.lockParticleColor.value,
                                    onCheckedChange = {
                                        viewModel.lockParticleColor.value = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(
                                            0xFFFFD700
                                        ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. KEY FLASHES & BLOOM (Viewer::showFlashOptions)
                SettingsSection(title = "KEY FLASHES & BLOOM") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Flash Size: ${(viewModel.flashSize.floatValue * 10).toInt() / 10.0}x",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Slider(
                                value = viewModel.flashSize.floatValue,
                                onValueChange = { viewModel.flashSize.floatValue = it },
                                valueRange = 0.4f..2.2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Halo Intensity: ${(viewModel.flashHaloIntensity.floatValue * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp
                            )
                            Slider(
                                value = viewModel.flashHaloIntensity.floatValue,
                                onValueChange = {
                                    viewModel.flashHaloIntensity.floatValue = it
                                },
                                valueRange = 0.2f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }
                    }
                }

                // 4. PIANO INTERFACE & STUDIO EFFECTS (Viewer::showKeyboardOptions)
                SettingsSection(title = "PIANO INTERFACE & STUDIO EFFECTS") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Highlight Active Keys",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.keyboardHighlightActive.value,
                                onCheckedChange = {
                                    viewModel.keyboardHighlightActive.value = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show Note Names on Keys",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = showLetters,
                                onCheckedChange = onShowLettersChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show Chord Overlay",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.keyboardShowChords.value,
                                onCheckedChange = { viewModel.keyboardShowChords.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show Black Key Edges",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.keyboardMinorEdges.value,
                                onCheckedChange = { viewModel.keyboardMinorEdges.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show Velvet Cloth (Crimson Felt Strip)",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.keyboardShowVelvetCloth.value,
                                onCheckedChange = {
                                    viewModel.keyboardShowVelvetCloth.value = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show Side Frames (Mahogany Cheeks)",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.keyboardShowSideFrames.value,
                                onCheckedChange = {
                                    viewModel.keyboardShowSideFrames.value = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }
                    }
                }

                // 5. PEDALS (Viewer::showPedalOptions)
                SettingsSection(title = "PEDALS & FOOT CONTROLS") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Show 3-Pedal Grand Unit",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.showPedals.value,
                                onCheckedChange = { viewModel.showPedals.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        if (viewModel.showPedals.value) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Pedal Scaling: ${(viewModel.pedalScaling.floatValue * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = viewModel.pedalScaling.floatValue,
                                    onValueChange = { viewModel.pedalScaling.floatValue = it },
                                    valueRange = 0.5f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700)
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Pedal Opacity: ${(viewModel.pedalOpacity.floatValue * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = viewModel.pedalOpacity.floatValue,
                                    onValueChange = { viewModel.pedalOpacity.floatValue = it },
                                    valueRange = 0.2f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700)
                                    )
                                )
                            }
                        }
                    }
                }

                // Theme Presets
                SettingsSection(title = "Visualizer Theme Presets") {
                    val presets = listOf(
                        Triple("Synthesia", Color(0xFF00E5FF), Color(0xFFFF4081)),
                        Triple("Cyberpunk", Color(0xFFD500F9), Color(0xFF00E676)),
                        Triple("Romantic", Color(0xFFFFD700), Color(0xFFFF6F00)),
                        Triple("Minimal", Color(0xFFFFFFFF), Color(0xFF90A4AE)),
                        Triple("Chroma Key", Color(0xFF00FF00), Color(0xFF00FF00))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (name, rhColor, lhColor) ->
                            val isSelected =
                                viewModel.rightHandColor.value == rhColor && viewModel.leftHandColor.value == lhColor
                            val primaryCol = MaterialTheme.colorScheme.primary
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.rightHandColor.value = rhColor
                                        viewModel.leftHandColor.value = lhColor
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) primaryCol.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(
                                    0.04f
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) primaryCol else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        vertical = 8.dp,
                                        horizontal = 4.dp
                                    ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(
                                            modifier = Modifier.size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(rhColor)
                                        )
                                        Box(
                                            modifier = Modifier.size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(lhColor)
                                        )
                                    }
                                    Text(
                                        text = name,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) primaryCol else MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.8f
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Hands & Multi-Track Instrument Mixer
                SettingsSection(title = "Hands & Multi-Track Instrument Mixer") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HandControlRow(
                            label = "Right Hand (Treble / Track 1)",
                            isMuted = viewModel.isRightHandMuted.value,
                            isHidden = viewModel.isRightHandHidden.value,
                            color = viewModel.rightHandColor.value,
                            onMuteToggle = {
                                viewModel.isRightHandMuted.value = it
                                viewModel.enabledTrackIndices[0] = !it
                            },
                            onHideToggle = {
                                viewModel.isRightHandHidden.value = it
                                viewModel.enabledTrackIndices[0] = !it
                            },
                            onColorChange = { viewModel.rightHandColor.value = it }
                        )
                        HandControlRow(
                            label = "Left Hand (Bass / Track 2)",
                            isMuted = viewModel.isLeftHandMuted.value,
                            isHidden = viewModel.isLeftHandHidden.value,
                            color = viewModel.leftHandColor.value,
                            onMuteToggle = {
                                viewModel.isLeftHandMuted.value = it
                                viewModel.enabledTrackIndices[1] = !it
                            },
                            onHideToggle = {
                                viewModel.isLeftHandHidden.value = it
                                viewModel.enabledTrackIndices[1] = !it
                            },
                            onColorChange = { viewModel.leftHandColor.value = it }
                        )

                        val scoreTracks =
                            (viewModel.currentScore.value as? Score)?.tracks ?: emptyList()
                        if (scoreTracks.isNotEmpty()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.1f
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "SCORE MIDI TRACKS & DRUMS (${scoreTracks.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD700)
                                )
                                TextButton(
                                    onClick = {
                                        scoreTracks.forEach { tr ->
                                            viewModel.enabledTrackIndices[tr.trackIndex] = true
                                        }
                                        viewModel.isRightHandHidden.value = false
                                        viewModel.isLeftHandHidden.value = false
                                    }
                                ) {
                                    Text(
                                        "Enable All",
                                        fontSize = 11.sp,
                                        color = Color(0xFF81D4FA),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            scoreTracks.forEach { track ->
                                val isTrackOn = viewModel.enabledTrackIndices[track.trackIndex]
                                    ?: track.isEnabled
                                val isDrum = track.channel == 9
                                val swatchColor = when {
                                    isDrum -> Color(0xFFFF5252)
                                    track.trackIndex == 0 -> viewModel.rightHandColor.value
                                    track.trackIndex == 1 -> viewModel.leftHandColor.value
                                    else -> Color(
                                        com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX[
                                            track.trackIndex % com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX.size
                                        ]
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.04f
                                            )
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isTrackOn) swatchColor else swatchColor.copy(
                                                        alpha = 0.25f
                                                    )
                                                )
                                                .border(
                                                    1.dp,
                                                    Color.White.copy(alpha = 0.7f),
                                                    CircleShape
                                                )
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.trackName.ifBlank { "Track ${track.trackIndex + 1}" },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTrackOn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.4f
                                                ),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (isDrum) "Channel 10 Percussion • ${track.noteCount} hits"
                                                else "Channel ${track.channel + 1} • Patch ${track.program + 1} • ${track.noteCount} notes",
                                                fontSize = 10.sp,
                                                color = if (isDrum) Color(0xFFFF8A80) else MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.55f
                                                )
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            scoreTracks.forEach { other ->
                                                viewModel.enabledTrackIndices[other.trackIndex] =
                                                    (other.trackIndex == track.trackIndex)
                                            }
                                        }
                                    ) {
                                        Text(
                                            "SOLO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD700)
                                        )
                                    }

                                    Switch(
                                        checked = isTrackOn,
                                        onCheckedChange = { checked ->
                                            viewModel.enabledTrackIndices[track.trackIndex] =
                                                checked
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = swatchColor,
                                            checkedTrackColor = swatchColor.copy(alpha = 0.35f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Speed & Loop
                SettingsSection(title = "Speed & Loop Controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val speed = viewModel.playbackSpeed.value
                            val formattedSpeed =
                                (kotlin.math.round(speed * 10) / 10.0).toString()
                            Text(
                                "Tempo Scale: ${formattedSpeed}x",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Slider(
                                value = viewModel.playbackSpeed.value,
                                onValueChange = { viewModel.playbackSpeed.value = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.width(140.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Metronome Click",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.metronomeEnabled.value,
                                onCheckedChange = { viewModel.metronomeEnabled.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "A-B Loop Active",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.abRepeatActive.value,
                                onCheckedChange = { viewModel.abRepeatActive.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(
                                        0xFFFFD700
                                    ), checkedTrackColor = Color(0xFFFFD700).copy(0.3f)
                                )
                            )
                        }
                    }
                }

                // Current Queue
                if (viewModel.playbackQueue.isNotEmpty()) {
                    SettingsSection(title = "Current Queue") {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            viewModel.playbackQueue.forEach { item ->
                                val isActive = viewModel.currentMediaItem.value == item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playMediaItem(item) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = if (isActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(
                                            0.3f
                                        ),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        item.title,
                                        color = if (isActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = Color(0xFFFFD700).copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun HandControlRow(
    label: String,
    isMuted: Boolean,
    isHidden: Boolean,
    color: Color,
    onMuteToggle: (Boolean) -> Unit,
    onHideToggle: (Boolean) -> Unit,
    onColorChange: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(0.03f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Color(0xFF6C63FF),
                    Color(0xFF4A90E2),
                    Color(0xFFFF6B35),
                    Color(0xFFFFD700),
                    Color(0xFF4CAF50)
                ).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(c)
                            .border(
                                if (color == c) 2.dp else 0.dp,
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onColorChange(c) }
                    )
                }
            }
        }

        IconButton(onClick = { onMuteToggle(!isMuted) }) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = null,
                tint = if (isMuted) Color.Red.copy(0.7f) else MaterialTheme.colorScheme.onSurface.copy(
                    0.6f
                )
            )
        }

        IconButton(onClick = { onHideToggle(!isHidden) }) {
            Icon(
                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(0.2f) else Color(
                    0xFFFFD700
                )
            )
        }
    }
}

@Composable
fun StepperControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(0.08f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Icon(
                    Icons.Default.Remove,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = if (value > 0) "+$value" else value.toString(),
                color = if (value != 0) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onValueChange(value + 1) }) {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun getWhiteKeyIndexForMidi(midiNote: Int): Float {
    val whiteNotes = (21..108).filter { !isMidiNoteBlack(it) }
    if (!isMidiNoteBlack(midiNote)) {
        val idx = whiteNotes.indexOf(midiNote)
        return if (idx >= 0) idx.toFloat() else 0f
    } else {
        val prevWhite = midiNote - 1
        val nextWhite = midiNote + 1
        val idx1 = whiteNotes.indexOf(prevWhite)
        val idx2 = whiteNotes.indexOf(nextWhite)
        return if (idx1 >= 0 && idx2 >= 0) (idx1 + idx2) / 2f else 0f
    }
}

/**
 * Phase 2 High-Performance Waterfall Precomputations:
 * 1. Static Geometry Cache per MIDI key (21..108) calculated once per canvas width change.
 * 2. Flat temporal timeline of notes with absolute beat offsets allowing O(log N) temporal windowing.
 */
data class MidiKeyGeometry(
    val midiNote: Int,
    val isBlack: Boolean,
    val whiteKeyIndex: Float,
    val pitchName: String,
    val baseWidthFraction: Float // Fraction of whiteKeyWidth
)

object WaterfallKeyGeometryLookup {
    val keyGeometries: Array<MidiKeyGeometry> = Array(128) { midi ->
        val isBlack = isMidiNoteBlack(midi)
        val wIdx = getWhiteKeyIndexForMidi(midi)
        val pitch = getPitchOnlyFromMidi(midi)
        val widthFrac = if (isBlack) 0.58f else 0.82f
        MidiKeyGeometry(
            midiNote = midi,
            isBlack = isBlack,
            whiteKeyIndex = wIdx,
            pitchName = pitch,
            baseWidthFraction = widthFrac
        )
    }
}

data class FlatWaterfallNote(
    val midiNote: Int,
    val absoluteBeatStart: Float,
    val durationBeats: Float,
    val isRightHand: Boolean,
    val voice: Int,
    val isChordNote: Boolean,
    val stringIndex: Int? = null, 
    val fretNumber: Int? = null
)

data class WaterfallTimeline(
    val notes: List<FlatWaterfallNote>,
    val measureCumulativeBeats: FloatArray
) {
    fun findStartWindowIndex(targetBeat: Float): Int {
        var low = 0
        var high = notes.lastIndex
        var result = notes.size
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (notes[mid].absoluteBeatStart + notes[mid].durationBeats >= targetBeat) {
                result = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return result
    }
}
@Composable
fun SynthesiaWaterfallOverlay(
    activeMidiNotes: Set<Int>,
    manualPressedNotes: Set<Int> = emptySet(),
    score: com.creativeblisslabs.peqlearn.app.ui.screens.Score? = null,
    playheadMeasure: Int = 0,
    playheadOffset: Float = 0f,
    isPlaying: Boolean = false,
    fallingSpeedSetting: String = "Normal",
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    glowEffect: Boolean = true,
    particlesEnabled: Boolean = true,
    particleCount: Int = 16,
    particleSpeed: Float = 4.5f,
    particleScale: Float = 1.0f,
    particleExpansion: Float = 1.0f,
    lockParticleColor: Boolean = true,
    particleCustomColor: Color = Color(0xFFFFD700),
    flashSize: Float = 1.0f,
    flashHaloIntensity: Float = 0.65f,
    noteCornerRadius: Float = 6.0f,
    noteEdgeThickness: Float = 1.2f,
    noteStyleOval: Boolean = false,
    leftHandColor: Color = Color(0xFF6C63FF),
    rightHandColor: Color = Color(0xFF4A90E2),
    isLeftHandHidden: Boolean = false,
    isRightHandHidden: Boolean = false,
    enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
    playbackSpeed: Float = 1.0f,
    onFpsDropDetected: () -> Unit = {},
    exportTimeSec: Float? = null,
    selectedInstrument: String = "Piano",
    activeNoteColors: Map<Int, Color> = emptyMap(),
    transposition: Int = 0
) {
    val fallingBlocks = remember { ArrayList<PianoWaterfallBlock>(64) }
    val particles = remember { ArrayList<PianoSpark>(128) }
    val flashes = remember { ArrayList<PianoKeyFlash>(32) }
    var canvasWidthPx by remember { mutableFloatStateOf(1200f) }
    var canvasHeightPx by remember { mutableFloatStateOf(500f) }
    val whiteKeyCount = 52

    val waterfallTimeline = remember(score, transposition, selectedInstrument) {
        if (score == null || score.measures.isEmpty()) {
            null
        } else {
            val cumBeats = FloatArray(score.measures.size + 1)
            var currentBeatTotal = 0f
            score.measures.forEachIndexed { idx, measure ->
                cumBeats[idx] = currentBeatTotal
                val beatsInMeasure =
                    (measure.timeSignatureNumerator.toFloat() * (4f / measure.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
                currentBeatTotal += beatsInMeasure
            }
            cumBeats[score.measures.size] = currentBeatTotal

            val isUke = selectedInstrument.equals("Ukulele", ignoreCase = true)
            val isSeven = selectedInstrument.contains("7", ignoreCase = true)
            val openStringsMidi = when {
                isUke -> listOf(69, 64, 60, 67)
                isSeven -> listOf(64, 59, 55, 50, 45, 40, 35)
                else -> score.tuningMidi.ifEmpty { listOf(64, 59, 55, 50, 45, 40) }
            }
            var runningGuitarBoxCenter = 3.0f

            val flatNotes = ArrayList<FlatWaterfallNote>()
            score.measures.forEachIndexed { mIdx, measure ->
                val measureStartBeat = cumBeats[mIdx]
                val hasDifferentiatedVoices =
                    measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }

                // Group by beat position so chords are solved together by the Viterbi Fretboard Solver!
                val byPos = measure.notes.filter { !it.isRest }.groupBy { (it.position * 20f).toInt() }

                for ((_, notesAtSlice) in byPos.entries.sortedBy { it.key }) {
                    val isChord = notesAtSlice.size >= 2
                    val transposedSlicePitches = notesAtSlice.map { (it.midiNote + transposition).coerceIn(21, 108) }
                    val (fretPlacements, nextBoxCenter) = com.creativeblisslabs.peqlearn.app.util.BiomechanicalPerformersEngine.solveFrettedSliceBiomechanical(
                        pitches = transposedSlicePitches,
                        openStringsMidiHighToLow = openStringsMidi,
                        maxFret = if (isUke) 15 else 24,
                        lastHandBoxCenterFret = runningGuitarBoxCenter
                    )
                    runningGuitarBoxCenter = nextBoxCenter

                    for (note in notesAtSlice) {
                        val shiftedMidi = (note.midiNote + transposition).coerceIn(21, 108)
                        val isRh = when {
                            shiftedMidi < 48 -> false
                            shiftedMidi > 72 -> true
                            hasDifferentiatedVoices -> note.voice == 0
                            else -> shiftedMidi >= 60
                        }
                        val placement = fretPlacements[shiftedMidi]
                        flatNotes.add(
                            FlatWaterfallNote(
                                midiNote = shiftedMidi,
                                absoluteBeatStart = measureStartBeat + note.position,
                                durationBeats = note.duration.coerceAtLeast(0.15f),
                                isRightHand = isRh,
                                voice = note.voice,
                                isChordNote = isChord,
                                stringIndex = placement?.stringIndex,
                                fretNumber = placement?.fret
                            )
                        )
                    }
                }
            }
            flatNotes.sortBy { it.absoluteBeatStart }
            WaterfallTimeline(flatNotes, cumBeats)
        }
    }

    // High-contrast Note Label Paint with dark drop-shadow halo so white text is crisp over any neon bar
    val reusableTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.BOLD
            )
            isAntiAlias = true
            setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.argb(220, 0, 0, 0))
        }
    }

    var fsFrameTicker by remember { mutableLongStateOf(0L) }
    var previouslyActiveNotes by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // 1. Free-Play / USB MIDI Ascending Note Blocks (when no score is actively playing)
    LaunchedEffect(activeMidiNotes, manualPressedNotes, leftHandColor, rightHandColor, isPlaying) {
        val combinedFreePlay = if (score == null || !isPlaying) activeMidiNotes + manualPressedNotes else manualPressedNotes
        if (combinedFreePlay.isNotEmpty()) {
            val contactY = if (canvasHeightPx > 50f) canvasHeightPx else 320f
            for (midi in combinedFreePlay) {
                val baseColor = if (midi >= 60) rightHandColor else leftHandColor
                val color = if (isMidiNoteBlack(midi)) baseColor.copy(alpha = 0.95f) else baseColor
                if (fallingBlocks.none { it.midiNote == midi && it.isAscending && it.currentY > contactY - 36f }) {
                    fallingBlocks.add(
                        PianoWaterfallBlock(
                            midiNote = midi,
                            startY = contactY,
                            currentY = contactY,
                            color = color,
                            isAscending = true,
                            alpha = 1.0f
                        )
                    )
                }
            }
        }
    }

    // 2. Spawn Key-Strike Light Pillars & Glowing Ember Particles ONLY on Newly Struck Notes
    LaunchedEffect(activeMidiNotes) {
        val newlyStruck = activeMidiNotes - previouslyActiveNotes
        previouslyActiveNotes = activeMidiNotes

        if (newlyStruck.isNotEmpty() && exportTimeSec == null) {
            val currentCanvasH = if (canvasHeightPx > 50f) canvasHeightPx else 320f
            val currentCanvasW = if (canvasWidthPx > 50f) canvasWidthPx else 1200f
            val whiteKeyW = currentCanvasW / whiteKeyCount.toFloat()

            for (midi in newlyStruck) {
                if (midi !in 21..108) continue
                val geom = WaterfallKeyGeometryLookup.keyGeometries[midi]
                val baseBlockW = (if (geom.isBlack) 0.60f else 0.86f) * whiteKeyW
                val xOff = (whiteKeyW - baseBlockW) * 0.5f
                val strikeX = (geom.whiteKeyIndex * whiteKeyW) + xOff + baseBlockW * 0.5f
                val baseColor = activeNoteColors[midi] ?: if (midi >= 60) rightHandColor else leftHandColor
                val isChordStrike = activeMidiNotes.size >= 2
                val flareColor = if (isChordStrike) Color(0xFFFFD700) else baseColor
                val baseRad = (if (geom.isBlack) whiteKeyW * 1.15f else whiteKeyW * 1.55f).coerceIn(18f, 54f)

                if (flashes.size < 28) {
                    flashes.add(
                        PianoKeyFlash(
                            midiNote = midi,
                            centerX = strikeX,
                            centerY = currentCanvasH,
                            color = flareColor,
                            velocity = 100,
                            alpha = (0.95f * flashHaloIntensity).coerceIn(0.25f, 1.0f),
                            radius = baseRad * flashSize.coerceIn(0.4f, 2.2f),
                            isChordStrike = isChordStrike
                        )
                    )
                }

                if (particlesEnabled && particles.size < 84) {
                    val sparkBase = if (geom.isBlack) (particleCount * 0.45f).toInt() else (particleCount * 0.65f).toInt()
                    val count = sparkBase.coerceIn(3, 18)
                    val pColor = if (lockParticleColor) baseColor else particleCustomColor
                    for (i in 0 until count) {
                        val angleRad = (-(35f + Random.nextFloat() * 110f) * (Math.PI / 180f)).toFloat()
                        val speed = (Random.nextFloat() * 4.2f + 2.2f) * (particleSpeed / 4.5f).coerceIn(0.3f, 2.8f)
                        val life = Random.nextFloat() * 20f + 14f
                        particles.add(
                            PianoSpark(
                                x = strikeX + (Random.nextFloat() - 0.5f) * (baseBlockW * 0.7f),
                                y = currentCanvasH - 2f,
                                vx = cos(angleRad) * speed * particleExpansion.coerceIn(0.5f, 2.2f),
                                vy = sin(angleRad) * speed,
                                alpha = 1.0f,
                                size = (Random.nextFloat() * 2.0f + 1.4f) * particleScale.coerceIn(0.4f, 2.5f),
                                maxLife = life,
                                currentLife = life,
                                color = pColor
                            )
                        )
                    }
                }
            }
        }
    }

    // 3. VSYNC-Locked Physics Loop (Only runs when particles, flashes, or free-play blocks exist)
    val shouldRunPhysicsLoop = exportTimeSec == null && (
            isPlaying || activeMidiNotes.isNotEmpty() || manualPressedNotes.isNotEmpty() ||
                    fallingBlocks.isNotEmpty() || particles.isNotEmpty() || flashes.isNotEmpty()
            )

    LaunchedEffect(shouldRunPhysicsLoop, fallingSpeedSetting) {
        var lastFrameNanos = 0L
        val gravity = 0.24f

        while (shouldRunPhysicsLoop && (
                    isPlaying || activeMidiNotes.isNotEmpty() || manualPressedNotes.isNotEmpty() ||
                            fallingBlocks.isNotEmpty() || particles.isNotEmpty() || flashes.isNotEmpty()
                    )
        ) {
            withFrameNanos { frameNanos ->
                val dtScale = if (lastFrameNanos == 0L) 1f else ((frameNanos - lastFrameNanos) / 16_666_667f).coerceIn(0.5f, 2.5f)
                lastFrameNanos = frameNanos

                val contactY = if (canvasHeightPx > 50f) canvasHeightPx else 320f
                val speedMultiplier = when (fallingSpeedSetting) {
                    "Slow" -> 0.75f
                    "Fast" -> 1.5f
                    "Extreme" -> 2.2f
                    else -> 1.0f
                }
                val stepY = 5.8f * speedMultiplier * dtScale

                val iterator = fallingBlocks.listIterator()
                while (iterator.hasNext()) {
                    val block = iterator.next()
                    if (block.isAscending) {
                        block.currentY -= stepY
                        block.startY = block.currentY + 32f
                        block.alpha = (block.currentY / contactY).coerceIn(0f, 1f)
                        if (block.currentY < -40f || block.alpha <= 0.04f) iterator.remove()
                    } else {
                        block.startY += stepY
                        block.currentY += stepY
                        if (block.startY > contactY + 60f) iterator.remove()
                    }
                }

                val pIter = particles.listIterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx * dtScale
                    p.y += p.vy * dtScale
                    p.vy += gravity * dtScale
                    p.vx *= 0.975f
                    p.currentLife -= dtScale
                    p.alpha = (p.currentLife / p.maxLife).coerceIn(0f, 1f)
                    if (p.currentLife <= 0f || p.alpha <= 0.02f) pIter.remove()
                }

                val fIter = flashes.listIterator()
                while (fIter.hasNext()) {
                    val f = fIter.next()
                    f.radius += 1.4f * flashSize.coerceIn(0.5f, 2.0f) * dtScale
                    f.alpha *= 0.85f
                    if (f.alpha < 0.03f) fIter.remove()
                }

                if (fallingBlocks.isNotEmpty() || particles.isNotEmpty() || flashes.isNotEmpty()) {
                    fsFrameTicker++
                }
            }
        }
    }

    // 4. Deterministic Video Export Stepper
    var prevExportTimeSec by remember { mutableStateOf<Float?>(null) }
    var prevExportActiveNotes by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(exportTimeSec) {
        val curT = exportTimeSec ?: run {
            prevExportTimeSec = null
            prevExportActiveNotes = emptySet()
            return@LaunchedEffect
        }
        val prevT = prevExportTimeSec
        val dtSec = if (prevT != null && curT >= prevT) (curT - prevT).coerceIn(0.001f, 0.1f) else (1f / 60f)
        val dtScale = (dtSec * 60f).coerceIn(0.25f, 3.0f)
        prevExportTimeSec = curT

        val contactY = if (canvasHeightPx > 50f) canvasHeightPx else 320f
        val whiteKeyW = (if (canvasWidthPx > 50f) canvasWidthPx else 1200f) / whiteKeyCount.toFloat()

        val newlyStruck = activeMidiNotes - prevExportActiveNotes
        prevExportActiveNotes = activeMidiNotes.toSet()

        if (newlyStruck.isNotEmpty()) {
            val isChordStrike = activeMidiNotes.size >= 2
            for (midi in newlyStruck) {
                if (midi !in 21..108) continue
                val geom = WaterfallKeyGeometryLookup.keyGeometries[midi]
                val baseBlockW = (if (geom.isBlack) 0.60f else 0.86f) * whiteKeyW
                val xOff = (whiteKeyW - baseBlockW) * 0.5f
                val strikeX = (geom.whiteKeyIndex * whiteKeyW) + xOff + baseBlockW * 0.5f
                val baseColor = if (midi >= 60) rightHandColor else leftHandColor
                val flareColor = if (isChordStrike) Color(0xFFFFD700) else baseColor
                val baseRad = (if (geom.isBlack) whiteKeyW * 1.15f else whiteKeyW * 1.55f).coerceIn(18f, 54f)

                flashes.add(
                    PianoKeyFlash(
                        midiNote = midi,
                        centerX = strikeX,
                        centerY = contactY,
                        color = flareColor,
                        velocity = 100,
                        alpha = (0.95f * flashHaloIntensity).coerceIn(0.25f, 1.0f),
                        radius = baseRad * flashSize.coerceIn(0.4f, 2.2f),
                        isChordStrike = isChordStrike
                    )
                )

                if (particlesEnabled && particles.size < 96) {
                    val sparkBase = if (geom.isBlack) (particleCount * 0.45f).toInt() else (particleCount * 0.65f).toInt()
                    val count = sparkBase.coerceIn(3, 18)
                    val pColor = if (lockParticleColor) baseColor else particleCustomColor
                    for (i in 0 until count) {
                        val angleRad = (-(35f + Random.nextFloat() * 110f) * (Math.PI / 180f)).toFloat()
                        val speed = (Random.nextFloat() * 4.2f + 2.2f) * (particleSpeed / 4.5f).coerceIn(0.3f, 2.8f)
                        val life = Random.nextFloat() * 20f + 14f
                        particles.add(
                            PianoSpark(
                                x = strikeX + (Random.nextFloat() - 0.5f) * (baseBlockW * 0.7f),
                                y = contactY - 2f,
                                vx = cos(angleRad) * speed * particleExpansion.coerceIn(0.5f, 2.2f),
                                vy = sin(angleRad) * speed,
                                alpha = 1.0f,
                                size = (Random.nextFloat() * 2.0f + 1.4f) * particleScale.coerceIn(0.4f, 2.5f),
                                maxLife = life,
                                currentLife = life,
                                color = pColor
                            )
                        )
                    }
                }
            }
        }

        val gravity = 0.24f
        val pIter = particles.listIterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx * dtScale
            p.y += p.vy * dtScale
            p.vy += gravity * dtScale
            p.vx *= 0.975f
            p.currentLife -= dtScale
            p.alpha = (p.currentLife / p.maxLife).coerceIn(0f, 1f)
            if (p.currentLife <= 0f || p.alpha <= 0.02f) pIter.remove()
        }

        val fIter = flashes.listIterator()
        while (fIter.hasNext()) {
            val f = fIter.next()
            f.radius += 1.4f * flashSize.coerceIn(0.5f, 2.0f) * dtScale
            f.alpha *= 0.85f
            if (f.alpha < 0.03f) fIter.remove()
        }

        fsFrameTicker++
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val _tick = fsFrameTicker
        val canvasW = size.width
        val canvasH = size.height
        val whiteKeyWidthPx = canvasW / whiteKeyCount.toFloat()
        if (canvasWidthPx != canvasW) canvasWidthPx = canvasW
        if (canvasHeightPx != canvasH) canvasHeightPx = canvasH

        val inst = selectedInstrument.trim()
        val isGuitarMode = inst.contains("Guitar", true) || inst.equals("Ukulele", true)
        val isHarmoniumMode = inst.equals("Harmonium", true)
        val isDrumOrTablaMode = inst.contains("Drum", true) || inst.equals("Tabla", true)

      // 0. Deep Studio Stage Backdrop + Instrument-Specific Guide Lanes
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF06070B), Color(0xFF0B0D14), Color(0xFF10131D))
            ),
            size = size
        )

        val subtleLaneColor = Color.White.copy(alpha = 0.045f)
        val accentGuideColor = Color(0xFF4A90E2).copy(alpha = 0.18f)

        if (isGuitarMode) {
            val isUke = inst.equals("Ukulele", true)
            val numFrets = if (isUke) 15 else 24
            val leftPad = (canvasW * 0.042f).coerceIn(28f, 68f)
            val rightPad = (canvasW * 0.018f).coerceIn(10f, 32f)
            val playableW = (canvasW - leftPad - rightPad).coerceAtLeast(100f)

            drawLine(
                color = Color(0xFFFFD700).copy(alpha = 0.35f),
                start = Offset(leftPad, 0f),
                end = Offset(leftPad, canvasH),
                strokeWidth = 2.2f
            )

            val r = 0.056125687f
            var scaleSum = 0f
            for (f in 1..numFrets) scaleSum += (1f - r).toDouble().pow(f.toDouble()).toFloat() * r
            var acc = 0f
            val inlayFrets = setOf(3, 5, 7, 9, 12, 15, 17, 19, 21, 24)
            for (f in 1..numFrets) {
                val prevAcc = acc
                acc += (((1f - r).toDouble().pow(f.toDouble()).toFloat() * r) / scaleSum) * playableW
                val wireX = leftPad + acc
                val isMarker = f in inlayFrets
                if (isMarker) {
                    val laneW = acc - prevAcc
                    drawRect(
                        color = if (f == 12 || f == 24) Color(0xFF4A90E2).copy(alpha = 0.07f) else Color.White.copy(alpha = 0.025f),
                        topLeft = Offset(leftPad + prevAcc, 0f),
                        size = Size(laneW, canvasH)
                    )
                }
                drawLine(
                    color = if (f == 12 || f == 24) accentGuideColor else subtleLaneColor,
                    start = Offset(wireX, 0f),
                    end = Offset(wireX, canvasH),
                    strokeWidth = if (f == 12 || f == 24) 1.5f else 0.8f
                )
            }
        } else if (isDrumOrTablaMode) {
            val laneW = canvasW / 8f
            for (i in 0..8) {
                val lx = i * laneW
                drawLine(
                    color = if (i % 2 == 0) accentGuideColor else subtleLaneColor,
                    start = Offset(lx, 0f),
                    end = Offset(lx, canvasH),
                    strokeWidth = 1.2f
                )
            }
        } else {
            // Piano (88-key) or Harmonium (37-key) vertical lane guides using loop variable `midi`
            val startM = if (isHarmoniumMode) 48 else 21
            val endM = if (isHarmoniumMode) 84 else 108
            for (midi in startM..endM) {
                val lane = resolveInstrumentWaterfallGeometry(
                    midiNote = midi,
                    selectedInstrument = selectedInstrument,
                    canvasWidth = canvasW,
                    canvasHeight = canvasH,
                    preassignedString = null,
                    preassignedFret = null
                )
                val isBlack = isMidiNoteBlack(midi)
                if (isBlack) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.28f),
                        topLeft = Offset(lane.centerX - lane.widthPx * 0.5f, 0f),
                        size = Size(lane.widthPx, canvasH)
                    )
                } else if (midi % 12 == 0) {
                    val leftEdgeX = lane.centerX - (lane.widthPx / 0.86f) * 0.5f
                    drawLine(
                        color = accentGuideColor,
                        start = Offset(leftEdgeX, 0f),
                        end = Offset(leftEdgeX, canvasH),
                        strokeWidth = 1.4f
                    )
                }
            }
        }

        val pillCorner = if (noteStyleOval) 24f else noteCornerRadius.coerceIn(3f, 16f)
        val cornerRad = CornerRadius(pillCorner, pillCorner)
        val outerBorderStroke = Stroke(width = noteEdgeThickness.coerceIn(0.8f, 3.5f))
        val innerRimStroke = Stroke(width = 1.0f)

        fun drawNeonSynthesiaBar(
            x: Float,
            topY: Float,
            width: Float,
            height: Float,
            baseColor: Color,
            alpha: Float,
            isSoundingNow: Boolean,
            pitchLabel: String,
            textBaselineOffset: Float
        ) {
            val lighterTop = Color(
                red = (baseColor.red + 0.22f).coerceAtMost(1f),
                green = (baseColor.green + 0.22f).coerceAtMost(1f),
                blue = (baseColor.blue + 0.22f).coerceAtMost(1f),
                alpha = alpha
            )
            val darkerBottom = Color(
                red = (baseColor.red * 0.72f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.72f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.72f).coerceIn(0f, 1f),
                alpha = alpha
            )

            if (glowEffect && isSoundingNow) {
                drawRoundRect(
                    color = baseColor.copy(alpha = 0.30f * alpha),
                    topLeft = Offset(x - 4f, topY - 4f),
                    size = Size(width + 8f, height + 8f),
                    cornerRadius = CornerRadius(pillCorner + 4f, pillCorner + 4f)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, baseColor.copy(alpha = 0.16f)),
                        startY = (canvasH - 110f).coerceAtLeast(0f),
                        endY = canvasH
                    ),
                    topLeft = Offset(x - 2f, (canvasH - 110f).coerceAtLeast(0f)),
                    size = Size(width + 4f, 110f.coerceAtMost(canvasH))
                )
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(lighterTop, baseColor.copy(alpha = alpha), darkerBottom),
                    startY = topY,
                    endY = topY + height
                ),
                topLeft = Offset(x, topY),
                size = Size(width, height),
                cornerRadius = cornerRad
            )

            if (width >= 8f && height >= 10f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = (if (isSoundingNow) 0.45f else 0.24f) * alpha),
                    topLeft = Offset(x + 1.2f, topY + 1.2f),
                    size = Size((width - 2.4f).coerceAtLeast(1f), (height - 2.4f).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius((pillCorner - 1f).coerceAtLeast(2f), (pillCorner - 1f).coerceAtLeast(2f)),
                    style = innerRimStroke
                )
            }

            drawRoundRect(
                color = if (isSoundingNow) Color.White.copy(alpha = 0.92f * alpha) else lighterTop.copy(alpha = 0.65f * alpha),
                topLeft = Offset(x, topY),
                size = Size(width, height),
                cornerRadius = cornerRad,
                style = outerBorderStroke
            )

            if (height >= 16f && width >= 10f) {
                val labelX = x + width * 0.5f
                val labelY = if (height > 36f) {
                    (topY + height - 12f).coerceAtLeast(topY + 12f)
                } else {
                    topY + height * 0.5f - textBaselineOffset
                }
                reusableTextPaint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
                drawContext.canvas.nativeCanvas.drawText(pitchLabel, labelX, labelY, reusableTextPaint)
            }
        }

        val labelTextSizePx = (whiteKeyWidthPx * 0.44f).coerceIn(8.5f.dp.toPx(), 11.5f.dp.toPx())
        reusableTextPaint.textSize = labelTextSizePx
        val fm = reusableTextPaint.fontMetrics
        val textBaselineOffset = (fm.ascent + fm.descent) * 0.5f

        // 1. Draw Free-Play / USB MIDI Ascending Note Bars
        for (block in fallingBlocks) {
            val lane = resolveInstrumentWaterfallGeometry(block.midiNote, selectedInstrument, canvasW, canvasH)
            val finalX = lane.centerX - lane.widthPx * 0.5f
            val blockAlpha = block.alpha.coerceIn(0f, 1f)
            val bottomY = if (block.isAscending) (block.currentY + 32f).coerceAtMost(canvasH) else block.currentY.coerceAtMost(canvasH)
            val topY = if (block.isAscending) block.currentY.coerceAtLeast(0f) else block.startY.coerceAtMost(canvasH)
            val blockHeight = (bottomY - topY).coerceAtLeast(18f)

            drawNeonSynthesiaBar(
                x = finalX,
                topY = topY,
                width = lane.widthPx,
                height = blockHeight,
                baseColor = lane.laneColor ?: block.color,
                alpha = blockAlpha,
                isSoundingNow = true,
                pitchLabel = lane.label,
                textBaselineOffset = textBaselineOffset
            )
        }

        // 2. Draw MIDI Score Lookahead Falling Notes (Aligned to Active Instrument!)
        if (waterfallTimeline != null) {
            val speedMul = when (fallingSpeedSetting) {
                "Slow" -> 0.75f
                "Fast" -> 1.5f
                "Extreme" -> 2.2f
                else -> 1.0f
            } * playbackSpeed.coerceIn(0.5f, 2.0f)
            val pixelsPerBeat = (canvasH / 3.0f) * speedMul
            val contactY = canvasH

            val cumBeats = waterfallTimeline.measureCumulativeBeats
            val mStartBeat = if (playheadMeasure < cumBeats.size) cumBeats[playheadMeasure] else 0f
            val mNextBeat = if (playheadMeasure + 1 < cumBeats.size) cumBeats[playheadMeasure + 1] else (mStartBeat + 4f)
            val measureBeats = (mNextBeat - mStartBeat).coerceAtLeast(1f)
            val currentPlaybackBeat = mStartBeat + (playheadOffset * measureBeats)

            val maxVisibleLookaheadBeats = (canvasH / pixelsPerBeat) + 0.5f
            val minBeat = currentPlaybackBeat - 0.5f
            val maxBeat = currentPlaybackBeat + maxVisibleLookaheadBeats

            val startIndex = waterfallTimeline.findStartWindowIndex(minBeat)
            val allNotes = waterfallTimeline.notes

            for (idx in startIndex until allNotes.size) {
                val note = allNotes[idx]
                if (note.absoluteBeatStart > maxBeat) break

                val trackInfo = score?.tracks?.firstOrNull { it.trackIndex == note.voice }
                val isDrumNote = trackInfo?.channel == 9
                // Hide Channel 10 Drum notes from melodic waterfalls unless Drum Kit is the active instrument!
                if (isDrumNote && !isDrumOrTablaMode) continue

                if (note.isRightHand && isRightHandHidden) continue
                if (!note.isRightHand && isLeftHandHidden) continue
                if (!(enabledTrackIndices[note.voice] ?: true)) continue

                val noteDistBeats = note.absoluteBeatStart - currentPlaybackBeat
                val bottomY = contactY - (noteDistBeats * pixelsPerBeat)
                val topY = bottomY - (note.durationBeats * pixelsPerBeat)

                if (bottomY < 0f || topY > canvasH) continue

     // 2. Draw MIDI Score Lookahead Falling Notes (Aligned to Active Instrument!)
          val lane = resolveInstrumentWaterfallGeometry(
                    midiNote = note.midiNote,
                    selectedInstrument = selectedInstrument,
                    canvasWidth = canvasW,
                    canvasHeight = canvasH,
                    preassignedString = note.stringIndex,
                    preassignedFret = note.fretNumber
                )
                val finalX = lane.centerX - lane.widthPx * 0.5f

                val isCurrentlySounding = noteDistBeats <= 0.05f && (noteDistBeats + note.durationBeats) >= -0.02f
                val clampedBottomY = (if (isCurrentlySounding && bottomY > contactY - 8f) contactY else bottomY).coerceAtMost(canvasH)
                val clampedTopY = topY.coerceAtLeast(0f)
                val blockHeight = (clampedBottomY - clampedTopY).coerceAtLeast(14f)

                val noteColor = lane.laneColor ?: when {
                    isDrumNote -> Color(0xFFFF5252)
                    note.voice >= 2 -> Color(
                        com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX[
                            note.voice % com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX.size
                        ]
                    )
                    note.isRightHand -> rightHandColor
                    else -> leftHandColor
                }

                drawNeonSynthesiaBar(
                    x = finalX,
                    topY = clampedTopY,
                    width = lane.widthPx,
                    height = blockHeight,
                    baseColor = noteColor,
                    alpha = if (isCurrentlySounding) 1.0f else 0.92f,
                    isSoundingNow = isCurrentlySounding,
                    pitchLabel = lane.label,
                    textBaselineOffset = textBaselineOffset
                )
            }
        }

        // 3. Bottom Laser Hit-Line & Soft Radial Key/Fret Strike Flares
        drawLine(
            color = primaryColor.copy(alpha = 0.45f),
            start = Offset(0f, canvasH - 1.5f),
            end = Offset(canvasW, canvasH - 1.5f),
            strokeWidth = 3f
        )

        if (glowEffect) {
            for (f in flashes) {
                val lane = resolveInstrumentWaterfallGeometry(f.midiNote, selectedInstrument, canvasW, canvasH)
                val strikeCenterX = lane.centerX
                val fAlpha = f.alpha.coerceIn(0f, 1f)
                val flareW = f.radius * 1.4f
                val flareH = (f.radius * 0.55f).coerceAtLeast(10f)
                val fColor = lane.laneColor ?: f.color

                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = fAlpha * 0.90f),
                            fColor.copy(alpha = fAlpha * 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(strikeCenterX, canvasH),
                        radius = flareW * 0.6f
                    ),
                    topLeft = Offset(strikeCenterX - flareW * 0.5f, canvasH - flareH * 0.5f),
                    size = Size(flareW, flareH)
                )

                val barW = lane.widthPx.coerceAtLeast(12f)
                drawRoundRect(
                    color = Color.White.copy(alpha = fAlpha),
                    topLeft = Offset(strikeCenterX - barW * 0.5f, canvasH - 3.5f),
                    size = Size(barW, 3.5f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }

        // 4. Glowing Ember Sparks
        if (particlesEnabled) {
            for (p in particles) {
                val pAlpha = p.alpha.coerceIn(0f, 1f)
                val sparkRadius = p.size.dp.toPx()

                drawCircle(
                    color = p.color.copy(alpha = pAlpha * 0.38f),
                    radius = sparkRadius * 2.1f,
                    center = Offset(p.x, p.y)
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = pAlpha * 0.9f), p.color.copy(alpha = pAlpha * 0.5f), Color.Transparent),
                        start = Offset(p.x, p.y),
                        end = Offset(p.x - p.vx * 2.2f, p.y - p.vy * 2.2f)
                    ),
                    start = Offset(p.x, p.y),
                    end = Offset(p.x - p.vx * 2.2f, p.y - p.vy * 2.2f),
                    strokeWidth = sparkRadius * 1.1f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha * 0.95f),
                    radius = sparkRadius * 0.65f,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

@Composable
fun PianoThreePedalUnit(
    sustainPressed: Boolean,
    sostenutoPressed: Boolean,
    softPressed: Boolean,
    onPedalClick: ((pedalType: Int, isPressed: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 1. Soft / Una Corda (CC 67)
                SinglePedalBlade(
                    name = "SOFT",
                    sub = "Una Corda",
                    isPressed = softPressed,
                    onClick = {
                        onPedalClick?.invoke(67, !softPressed)
                    }
                )

                // 2. Sostenuto (CC 66)
                SinglePedalBlade(
                    name = "SOST",
                    sub = "Sostenuto",
                    isPressed = sostenutoPressed,
                    onClick = {
                        onPedalClick?.invoke(66, !sostenutoPressed)
                    }
                )

                // 3. Sustain / Damper (CC 64)
                SinglePedalBlade(
                    name = "SUST",
                    sub = "Damper",
                    isPressed = sustainPressed,
                    onClick = {
                        onPedalClick?.invoke(64, !sustainPressed)
                    }
                )
            }
        }
    }
}

@Composable
private fun SinglePedalBlade(
    name: String,
    sub: String,
    isPressed: Boolean,
    onClick: () -> Unit
) {
    val brassGradientNormal = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE5C07B),
            Color(0xFFC89B3C),
            Color(0xFF996B1E),
            Color(0xFF734C0A)
        )
    )

    val brassGradientPressed = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF2A3),
            Color(0xFFFFD54F),
            Color(0xFFFF9800),
            Color(0xFFE65100)
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 0.dp)
    ) {
        // Top Mounting Bracket / Pin Housing (Pedal Top)
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A4A4C), Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                    )
                )
        )

        // Physical Brass Foot-pedal blade shape
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(26.dp)
                .graphicsLayer {
                    translationY = if (isPressed) 5.dp.toPx() else 0f
                    scaleY = if (isPressed) 0.90f else 1.0f
                }
                .clip(
                    RoundedCornerShape(
                        topStart = 3.dp,
                        topEnd = 3.dp,
                        bottomStart = 10.dp,
                        bottomEnd = 10.dp
                    )
                )
                .background(if (isPressed) brassGradientPressed else brassGradientNormal)
                .border(
                    width = if (isPressed) 1.5.dp else 1.dp,
                    color = if (isPressed) Color(0xFFFFE082) else Color(0x664A3508),
                    shape = RoundedCornerShape(
                        topStart = 3.dp,
                        topEnd = 3.dp,
                        bottomStart = 10.dp,
                        bottomEnd = 10.dp
                    )
                )
        ) {
            // Left Metallic Reflection Highlight
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp, top = 3.dp, bottom = 4.dp)
                    .background(
                        Color.White.copy(alpha = if (isPressed) 0.65f else 0.35f),
                        RoundedCornerShape(2.dp)
                    )
            )

            // Right Bevel Shadow
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 1.dp, top = 4.dp, bottom = 4.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            )

            if (isPressed) {
                // Radiant glow ring when pressed
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33FFB300))
                )
            }
        }

        Text(
            text = name,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = if (isPressed) Color(0xFFFFCA28) else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.7f
            )
        )
    }
}

/**
 * Corner MIDI Status Badge showing:
 * - Real Connected Piano / Keyboard Name
 * - Incoming note velocity badge (e.g. "Vel 112")
 * - Active MIDI channel
 */
@Composable
fun PianoMidiStatusBadge(
    deviceName: String?,
    lastVelocity: Int,
    lastChannel: Int,
    onDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = !deviceName.isNullOrBlank()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onDeviceClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (isConnected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF6B7280).copy(
                alpha = 0.3f
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Glowing status indicator light
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isConnected) Color(0xFF10B981) else Color(0xFF9CA3AF))
                    .border(
                        1.dp,
                        if (isConnected) Color(0xFF34D399) else Color(0xFF6B7280),
                        RoundedCornerShape(50)
                    )
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isConnected) "🎹" else "🎹 🚫",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.6f
                        ),
                        maxLines = 1
                    )
                }

                if (isConnected && lastVelocity > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VEL: $lastVelocity",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFB300)
                        )
                        Text(
                            text = "CH: $lastChannel",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Elegant prompt dialog for USB/MIDI Piano connection and disconnection
 */
@Composable
fun MidiConnectionPromptDialog(
    prompt: com.creativeblisslabs.peqlearn.app.ui.viewmodel.MidiPromptMessage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (prompt.isConnected) "🎹" else "🔌",
                    fontSize = 20.sp
                )
                Text(
                    text = prompt.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (prompt.isConnected) Color(0xFF34D399) else Color(0xFFFF7043)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = prompt.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Device:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            prompt.deviceName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prompt.isConnected) Color(0xFF059669) else MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    )
}
