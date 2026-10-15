package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.audio.AudioEngine
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.MusicConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
internal fun KarnaticClassicalInteractivePanel(viewModel: AppViewModel) {
    val accentColor = MaterialTheme.colorScheme.primary
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("72 Melakartas", "Sapta Talas", "35 Alankarams", "Solkattu")
    val tabIcons = listOf(Icons.Default.SurroundSound, Icons.Default.MusicNote, Icons.Default.Speed, Icons.Default.AutoStories, Icons.Default.GraphicEq)
    var notationFormat by remember { mutableStateOf("Staff") }

    var selectedRagaState by remember { mutableStateOf<Raaga?>(null) }
    var visualizerMode by remember { mutableIntStateOf(0) } // 0 = Harmonium, 1 = Guitar
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Changed TabRow to ScrollableTabRow to support horizontal scrolling
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = accentColor,
            edgePadding = 0.dp, // Removes default start/end padding for left alignment
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = accentColor
                    )
                }
            }
        ) {
            tabs.forEachIndexed { i, label ->
                val isSelected = selectedTab == i
                Tab(
                    selected = isSelected,
                    onClick = {
                        selectedTab = i
                        viewModel.currentScore.value = null
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    icon = {
                        Icon(
                            imageVector = tabIcons.getOrElse(i) { Icons.Default.MusicNote },
                            contentDescription = label,
                            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = { 
                        Text(
                            text = label, 
                            fontSize = 11.sp, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(12.dp)) {
                    when (selectedTab) {
                        0 -> UnifiedRaagaBrowser(viewModel, accentColor, isCarnatic = true, onSelectRaga = { selectedRagaState = it })
                        1 -> UnifiedTaalBrowser(viewModel, accentColor, isCarnatic = true)
                        2 -> UnifiedAlankaarPanel(viewModel, accentColor, isCarnatic = true)
                        3 -> KarnaticSolkattuPanel(accentColor)
                    }
                }
            }

            // Shared Instrument Selection Row & Piano / Guitar Interactive Visualizer
            val activeSemitones = remember(selectedRagaState) {
                selectedRagaState?.semitones?.ifEmpty { listOf(0, 1, 4, 5, 7, 8, 11, 12) } ?: listOf(0, 1, 4, 5, 7, 8, 11, 12)
            }
            val playingNote = viewModel.midiPlayingNote.value

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTRUMENT SCALE VISUALIZER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .height(36.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("🪗 Harmonium", "🎸 Guitar").forEachIndexed { idx, label ->
                        val isSelected = visualizerMode == idx
                        pEQFilterChip(
                            selected = isSelected,
                            onClick = {
                                visualizerMode = idx
                                val inst = if (idx == 0) "HARMONIUM" else "GUITAR"
                                getAudioEngine().setInstrument(inst)
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
            if (visualizerMode == 0) {
                val baseScaleMap = remember(activeSemitones) {
                    activeSemitones.associate { (60 + it) to true }
                }
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    UnifiedPianoKeyboard(
                        activeMidiNotes = baseScaleMap,
                        accentColor = accentColor,
                        activeCoordinate = playingNote,
                        startNote = 48,
                        endNote = 84,
                        showNoteNames = true,
                        isPlaying = playingNote != null,
                        onKeyTapped = { midi ->
                            viewModel.audioEngine.setInstrument("HARMONIUM")
                            viewModel.onMidiNoteOn(midi, 95)
                            viewModel.audioEngine.noteOn(midi, 0.85f)
                            scope.launch {
                                delay(300L)
                                viewModel.onMidiNoteOff(midi)
                            }
                        },
                        modifier = Modifier
                            .height(120.dp)       // Lock the desired height
                            .wrapContentWidth()  // Let internal key math determine the proportional width
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } else {
                val highlightedGuitarList = remember(activeSemitones, playingNote) {
                    if (playingNote != null) listOf(playingNote - 60) else activeSemitones
                }
                UnifiedGuitarFretboard(
                    numFrets = 12,
                    activeNotes = highlightedGuitarList,
                    rootNote = 0,
                    accentColor = accentColor,
                    showNoteNames = true,
                    onFretTapped = { _, _, midiNote ->
                        viewModel.audioEngine.setInstrument("GUITAR")
                        viewModel.onMidiNoteOn(midiNote, 95)
                        viewModel.audioEngine.noteOn(midiNote, 0.85f)
                        scope.launch {
                            delay(300L)
                            viewModel.onMidiNoteOff(midiNote)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(8.dp))
                )
            }

            // Notation Viewer linked to viewModel.currentScore
            StaffAndTabNotationViewer(
                score = viewModel.currentScore.value as? Score,
                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                playheadOffset = viewModel.scorePlayheadOffset.value,
                activeMidiNotes = viewModel.midiActiveNotes,
                playingNote = viewModel.midiPlayingNote.value,
                primaryColor = accentColor,
                currentFormat = notationFormat,
                onFormatChange = { notationFormat = it },
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
    }
}


@Composable
fun KarnaticSolkattuPanel(accentColor: Color) {
    val solkattuList = listOf(
        Pair("Tha", "1-Akshara primary strike"),
        Pair("Dhi", "2nd syllable in Tisra / Chatusra gati"),
        Pair("Thom", "Resonant bass stroke on Mridangam Thoppi"),
        Pair("Nam", "Open resonant ringing strike on Dhimmu"),
        Pair("Tha-Ka", "2-Pulse rhythmic unit"),
        Pair("Tha-Ki-Ta", "Tisra Gati 3-beat rhythm"),
        Pair("Tha-Ka-Dhi-Mi", "Chatusra Gati 4-beat unit"),
        Pair("Tha-Ka-Tha-Ki-Ta", "Khanda Gati 5-beat rhythm"),
        Pair("Tha-Ki-Ta-Tha-Ka-Dhi-Mi", "Misra Gati 7-beat rhythm"),
        Pair("Tha-Ka-Dhi-Mi-Tha-Ka-Tha-Ki-Ta", "Sankeerna Gati 9-beat rhythm")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mridangam Solkattu Rhythmic Phrasing & Konnakol", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        solkattuList.forEach { (syllable, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val engine = getAudioEngine()
                        engine.playSample(syllable, 0, "TABLA")
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = syllable,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(130.dp)
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}