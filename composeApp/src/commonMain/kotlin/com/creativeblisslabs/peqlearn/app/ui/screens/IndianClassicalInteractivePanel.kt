package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
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
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.MusicConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
enum class IndianClassicalPanelTab(val label: String) {
    RAAGAS("Rāga Library"),
    THAATS("10 Thaats"),
    TAALS("Tāla Systems"),
    ALANKAARS("20+ Alaankars"),
    TABLA_BOLS("Tabla Bols")
}

@Composable
internal fun IndianClassicalInteractivePanel(viewModel: AppViewModel) {
    val accentColor = MaterialTheme.colorScheme.primary
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Raagas", "Thaats", "Taals", "Alankaars", "Tabla Bols")
    val tabIcons = listOf(Icons.Default.SurroundSound, Icons.Default.MusicNote, Icons.Default.LibraryMusic, Icons.Default.Speed, Icons.Default.AutoStories, Icons.Default.GraphicEq)
    var notationFormat by remember { mutableStateOf("Staff") }

    var selectedRagaState by remember { mutableStateOf<Raaga?>(null) }
    var selectedInstrument by remember { mutableStateOf(ChordInstrumentType.HARMONIUM) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        
        ScrollableTabRow(
            selectedTabIndex = selectedTab, 
            containerColor = Color.Transparent, 
            contentColor = accentColor,
            edgePadding = 0.dp,
            indicator = { tabPositions -> 
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = accentColor) 
                } 
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        ) {
            tabs.forEachIndexed { i, label -> 
                val isSelected = selectedTab == i
                Tab(
                    selected = isSelected, 
                    onClick = { selectedTab = i; viewModel.currentScore.value = null }, 
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
                            maxLines = 1
                        ) 
                    }
                ) 
            }
        }

        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(12.dp)) {
                    when (selectedTab) {
                        0 -> UnifiedRaagaBrowser(viewModel, accentColor, isCarnatic = false, onSelectRaga = { selectedRagaState = it })
                        1 -> UnifiedThaatBrowser(viewModel, accentColor)
                        2 -> UnifiedTaalBrowser(viewModel, accentColor, isCarnatic = false)
                        3 -> UnifiedAlankaarPanel(viewModel, accentColor, isCarnatic = false)
                        4 -> IndianTablaBolsPanel(accentColor)
                    }
                }
            }

            // Shared Instrument Selection Row & Piano / Guitar Interactive Visualizer
            val activeSemitones = remember(selectedRagaState) {
                selectedRagaState?.semitones?.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11, 12) } ?: listOf(0, 2, 4, 5, 7, 9, 11, 12)
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
                pEQInstrumentDropdown(
                    selectedInstrument = selectedInstrument,
                    accentColor = accentColor,
                    onInstrumentChange = {
                        selectedInstrument = it
                        try {
                            getAudioEngine().setInstrument(it.soundEngineName)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.width(180.dp)
                )
            }

            AnimatedContent(
                targetState = selectedInstrument,
                transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                label = "classical_instrument_visualizer"
            ) { inst ->
                val baseScaleMap = remember(activeSemitones) {
                    activeSemitones.associate { (60 + it) to true }
                }
                val scalePitches = remember(activeSemitones) {
                    activeSemitones.map { 60 + it }
                }

                when (inst) {
                    ChordInstrumentType.HARMONIUM, ChordInstrumentType.TABLA -> {
                        UnifiedHarmoniumVisualizer(
                            activeMidiNotes = baseScaleMap,
                            accentColor = accentColor,
                            activeMidiNote = playingNote,
                            onKeyTapped = { midiNote ->
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midiNote, 95)
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midiNote)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    ChordInstrumentType.PIANO_ACOUSTIC, ChordInstrumentType.PIANO_ELECTRIC -> {
                        UnifiedPianoKeyboard(
                            activeMidiNotes = baseScaleMap,
                            accentColor = accentColor,
                            activeCoordinate = playingNote,
                            startNote = 48,
                            endNote = 84,
                            showNoteNames = true,
                            isPlaying = playingNote != null,
                            onKeyTapped = { midi ->
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midi, 95)
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midi)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .height(120.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    ChordInstrumentType.GUITAR_ACOUSTIC, ChordInstrumentType.GUITAR_ELECTRIC, ChordInstrumentType.GUITAR_CLASSICAL -> {
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
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midiNote, 95)
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midiNote)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(125.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    ChordInstrumentType.GUITAR_7 -> {
                        UnifiedGuitarFretboard(
                            numFrets = 12,
                            accentColor = accentColor,
                            rootNote = 0,
                            showNoteNames = true,
                            markOctaves = true,
                            highlightOctaveC = true,
                            baseFret = 1,
                            stringLabels = listOf("e", "B", "G", "D", "A", "E", "B"),
                            openStringsSemis = listOf(4, 11, 7, 2, 9, 4, 11),
                            activeMidiNote = playingNote,
                            activeNotes = scalePitches,
                            onFretTapped = { _, _, midiNote ->
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midiNote, 95)
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midiNote)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    ChordInstrumentType.UKULELE -> {
                        UnifiedGuitarFretboard(
                            numFrets = 5,
                            accentColor = accentColor,
                            rootNote = 0,
                            showNoteNames = true,
                            markOctaves = true,
                            highlightOctaveC = true,
                            baseFret = 1,
                            stringLabels = listOf("A", "E", "C", "G"),
                            openStringsSemis = listOf(9, 4, 0, 7),
                            activeMidiNote = playingNote,
                            activeNotes = scalePitches,
                            onFretTapped = { _, _, midiNote ->
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midiNote, 95)
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midiNote)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    ChordInstrumentType.SITAR -> {
                        UnifiedSitarVisualizer(
                            activeNotes = scalePitches,
                            activeMidiNote = playingNote,
                            accentColor = accentColor,
                            showNoteNames = true,
                            onFretTapped = { _, midiNote ->
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midiNote, 95)
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midiNote)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    ChordInstrumentType.VIOLIN -> {
                        UnifiedViolinFingerboard(
                            activeNotes = scalePitches,
                            activeMidiNote = playingNote,
                            accentColor = accentColor,
                            showNoteNames = true,
                            onPositionTapped = { _, midiNote ->
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midiNote, 95)
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midiNote)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(135.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    ChordInstrumentType.FLUTE -> {
                        UnifiedFluteVisualizer(
                            activeNotes = scalePitches,
                            activeMidiNote = playingNote,
                            accentColor = accentColor,
                            onToneHoleTapped = { _, midiNote ->
                                try {
                                    viewModel.audioEngine.setInstrument(selectedInstrument.soundEngineName)
                                    viewModel.onMidiNoteOn(midiNote, 95)
                                    viewModel.audioEngine.noteOn(midiNote, 0.85f)
                                    scope.launch {
                                        delay(300L)
                                        viewModel.onMidiNoteOff(midiNote)
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // Notation Viewer linked to viewModel.currentScore with full comprehensive attributes wired
            StaffAndTabNotationViewer(
                score = viewModel.currentScore.value as? Score,
                playheadMeasure = viewModel.scorePlayheadMeasure.value,
                playheadOffset = viewModel.scorePlayheadOffset.value,
                activeMidiNotes = viewModel.midiActiveNotes,
                playingNote = viewModel.midiPlayingNote.value,
                primaryColor = accentColor,
                currentFormat = notationFormat,
                onFormatChange = { notationFormat = it },
                showLetters = viewModel.showNotationNoteNames.value,
                showLyrics = true,
                isSplitStaff = viewModel.isSplitStaff.value,
                isVerticalScroll = viewModel.isVerticalScroll.value,
                onVerticalScrollChange = { viewModel.isVerticalScroll.value = it },
                leftHandColor = viewModel.leftHandColor.value,
                rightHandColor = viewModel.rightHandColor.value,
                isLeftHandHidden = viewModel.isLeftHandHidden.value,
                isRightHandHidden = viewModel.isRightHandHidden.value,
                enabledTrackIndices = viewModel.enabledTrackIndices,
                showScoreHeader = false,
                modifier = Modifier.fillMaxWidth().height(240.dp)
            )
                        Spacer(modifier = Modifier.height(110.dp))

        }
    }
}


@Composable
fun InfoChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    pEQGlassCard {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun Modifier.fillModifierOrWrap() = this.fillMaxWidth().wrapContentHeight()


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IndianTablaBolsPanel(accentColor: Color) {
    val engine = getAudioEngine()
    var lastPlayedBol by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Bayan (Bass)", "Dayan (Treble)", "Compound")

    val filteredBols = remember(selectedCategory) {
        when (selectedCategory) {
            "Bayan (Bass)" -> INDIAN_TABLA_BOLS.filter { (bol, _) -> bol.uppercase() in listOf("GHE", "GE", "KA", "KE", "KAT") }
            "Dayan (Treble)" -> INDIAN_TABLA_BOLS.filter { (bol, _) -> bol.uppercase() in listOf("NA", "TA", "TIN", "TI", "TE", "TUN", "THUN") }
            "Compound" -> INDIAN_TABLA_BOLS.filter { (bol, _) -> bol.uppercase() in listOf("DHA", "DHIN", "TIRAKITA", "DHIRDHIR", "KREN") }
            else -> INDIAN_TABLA_BOLS
        }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Visual Interactive Tabla drums
        UnifiedTablaVisualizer(
            activeBol = lastPlayedBol,
            accentColor = accentColor,
            onBolTapped = { bol ->
                lastPlayedBol = bol
                engine.playSample(bol, 0, "TABLA")
            },
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )

        // Filter chips row to reduce clutter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = cat },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.height(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Clean Grid of Tabla Bols Cards
        Text(
            text = "Tap to sound bol (${filteredBols.size} Bols):",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filteredBols.forEach { (bol, desc) ->
                val isCurrent = lastPlayedBol.equals(bol, ignoreCase = true)
                pEQGlassCard(
                    modifier = Modifier
                        .widthIn(min = 140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { 
                            lastPlayedBol = bol
                            engine.playSample(bol, 0, "TABLA")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .background(if (isCurrent) accentColor.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isCurrent) accentColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = bol.take(2).uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCurrent) Color.Black else accentColor
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bol,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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