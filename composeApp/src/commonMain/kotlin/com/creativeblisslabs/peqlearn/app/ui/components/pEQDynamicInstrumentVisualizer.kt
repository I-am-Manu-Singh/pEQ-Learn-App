package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

@Composable
fun pEQDynamicInstrumentVisualizer(
    selectedInstrument: ChordInstrumentType,
    activeNotes: List<Int>,
    activeMidiToken: Int? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onNoteTapped: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeMap = remember(activeNotes) {
        activeNotes.associateWith { true }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
            when (selectedInstrument) {
                ChordInstrumentType.PIANO -> {
                    UnifiedPianoKeyboard(
                        activeMidiNotes = activeMap,
                        accentColor = accentColor,
                        activeMidiNote = activeMidiToken,
                        startNote = 36,
                        endNote = 84,
                        fillWidth = true,
                        showNoteNames = true,
                        onKeyTapped = onNoteTapped,
                        modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                ChordInstrumentType.GUITAR, ChordInstrumentType.UKULELE, ChordInstrumentType.GUITAR_7 -> {
                    val stringsCount = when (selectedInstrument) {
                        ChordInstrumentType.UKULELE -> 4
                        ChordInstrumentType.GUITAR_7 -> 7
                        else -> 6
                    }
                    val stringLabels = when (selectedInstrument) {
                        ChordInstrumentType.UKULELE -> listOf("A", "E", "C", "G")
                        ChordInstrumentType.GUITAR_7 -> listOf("e", "B", "G", "D", "A", "E", "B")
                        else -> listOf("e", "B", "G", "D", "A", "E")
                    }
                    val openSemis = when (selectedInstrument) {
                        ChordInstrumentType.UKULELE -> listOf(9, 4, 0, 7)
                        ChordInstrumentType.GUITAR_7 -> listOf(4, 11, 7, 2, 9, 4, 11)
                        else -> listOf(4, 11, 7, 2, 9, 4)
                    }

                    UnifiedFretboard(
                        strings = stringsCount,
                        frets = 15,
                        accentColor = accentColor,
                        showNoteNames = true,
                        markOctaves = true,
                        highlightOctaveC = true,
                        baseFret = 1,
                        stringLabels = stringLabels,
                        openStringsSemis = openSemis,
                        activeMidiNote = activeMidiToken,
                        activeNotes = activeNotes,
                        onFretTapped = { _, _, midi -> onNoteTapped(midi) },
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                ChordInstrumentType.HARMONIUM -> {
                    UnifiedHarmoniumVisualizer(
                        activeMidiNotes = activeMap,
                        accentColor = accentColor,
                        activeMidiNote = activeMidiToken,
                        onKeyTapped = onNoteTapped,
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                ChordInstrumentType.SITAR -> {
                    UnifiedSitarVisualizer(
                        activeNotes = activeNotes,
                        activeMidiNote = activeMidiToken,
                        accentColor = accentColor,
                        showNoteNames = true,
                        onFretTapped = { _, midi -> onNoteTapped(midi) },
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                ChordInstrumentType.VIOLIN -> {
                    UnifiedViolinFingerboard(
                        activeNotes = activeNotes,
                        activeMidiNote = activeMidiToken,
                        accentColor = accentColor,
                        showNoteNames = true,
                        onPositionTapped = { _, midi -> onNoteTapped(midi) },
                        modifier = Modifier.fillMaxWidth().height(135.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                ChordInstrumentType.FLUTE -> {
                    UnifiedFluteVisualizer(
                        activeNotes = activeNotes,
                        activeMidiNote = activeMidiToken,
                        accentColor = accentColor,
                        onToneHoleTapped = { _, midi -> onNoteTapped(midi) },
                        modifier = Modifier.fillMaxWidth().height(95.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
