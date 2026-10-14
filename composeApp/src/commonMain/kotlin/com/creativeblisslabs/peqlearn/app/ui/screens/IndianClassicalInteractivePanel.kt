package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
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
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedPianoKeyboard
import com.creativeblisslabs.peqlearn.app.ui.components.UnifiedGuitarFretboard
import com.creativeblisslabs.peqlearn.app.ui.components.StaffAndTabNotationViewer
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
    val indianColor = Color(0xFFFF9933)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Raagas", "Thaats", "Taals", "Alankaars", "Tabla Bols")
    var notationFormat by remember { mutableStateOf("Staff Notation") }

    var selectedRagaState by remember { mutableStateOf<Raaga?>(null) }
    var visualizerMode by remember { mutableIntStateOf(0) } // 0 = Piano, 1 = Guitar

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = indianColor,
            indicator = { tabPositions -> if (selectedTab < tabPositions.size) TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = indianColor) }
        ) {
            tabs.forEachIndexed { i, label -> Tab(selected = selectedTab == i, onClick = { selectedTab = i; viewModel.currentScore.value = null }, text = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }) }
        }

        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (selectedTab) {
                0 -> UnifiedRaagaBrowser(viewModel, indianColor, isCarnatic = false, onSelectRaga = { selectedRagaState = it })
                1 -> UnifiedThaatBrowser(viewModel, indianColor)
                2 -> UnifiedTaalBrowser(viewModel, indianColor, isCarnatic = false)
                3 -> UnifiedAlankaarPanel(viewModel, indianColor, isCarnatic = false)
                4 -> IndianTablaBolsPanel(indianColor)
            }

            // Shared Instrument Selection Row & Piano / Guitar Interactive Visualizer
            val activeSemitones = remember(selectedRagaState) {
                selectedRagaState?.semitones?.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11, 12) } ?: listOf(0, 2, 4, 5, 7, 9, 11, 12)
            }
            val playingNote = viewModel.midiPlayingNote.value

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTRUMENT SCALE VISUALIZER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = indianColor,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("🎹 Piano", "🎸 Guitar").forEachIndexed { idx, label ->
                        val isSelected = visualizerMode == idx
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) indianColor.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { visualizerMode = idx }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) indianColor else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (visualizerMode == 0) {
                val activeMidiMap = remember(activeSemitones, playingNote) {
                    if (playingNote != null) mapOf(playingNote to true)
                    else activeSemitones.associate { (60 + it) to true }
                }
                UnifiedPianoKeyboard(
                    activeMidiNotes = activeMidiMap,
                    accentColor = indianColor,
                    startNote = 60,
                    endNote = 72,
                    showNoteNames = true,
                    modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                val highlightedGuitarList = remember(activeSemitones, playingNote) {
                    if (playingNote != null) listOf(playingNote - 60) else activeSemitones
                }
                UnifiedGuitarFretboard(
                    numFrets = 12,
                    activeNotes = highlightedGuitarList,
                    rootNote = 0,
                    accentColor = indianColor,
                    showNoteNames = true,
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
                primaryColor = indianColor,
                currentFormat = notationFormat,
                onFormatChange = { notationFormat = it },
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
    }
}

@Composable
fun InfoChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.1f), border = BorderStroke(1.dp, color.copy(0.3f))) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun Modifier.fillModifierOrWrap() = this.fillMaxWidth().wrapContentHeight()


@Composable
fun IndianTablaBolsPanel(accentColor: Color) {
    val engine = getAudioEngine()
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tabla Syllables & Finger Articulations", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        INDIAN_TABLA_BOLS.forEach { (bol, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        engine.playSample(bol, 0, "TABLA")
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bol,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    modifier = Modifier.width(90.dp)
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}