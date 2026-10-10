package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.isActive
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

@Composable
fun PEQLearnScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit = {}
) {
    var showTheory by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showTheory,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "main_content_anim"
    ) { isTheoryActive ->
        if (isTheoryActive) {
            MusicTheoryScreen(
                viewModel = viewModel,
                onBack = { showTheory = false }
            )
        } else {
            LearnMainContent(
                viewModel = viewModel,
                onOpenTheory = { showTheory = true }
            )
        }
    }
}
@Composable
private fun LearnMainContent(
    viewModel: AppViewModel,
    onOpenTheory: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 1000.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.Transparent)
                .padding(horizontal = if (isWide) 32.dp else 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. MUSIC THEORY BOOK ENTRY CARD
            val infiniteAnim = rememberInfiniteTransition(label = "theory_glow")
            val glowAlpha by infiniteAnim.animateFloat(
                initialValue = 0.12f, targetValue = 0.26f,
                animationSpec = infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Reverse),
                label = "glow"
            )
            Surface(
                onClick = onOpenTheory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = primaryColor.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = glowAlpha))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(shape = RoundedCornerShape(14.dp), color = primaryColor.copy(alpha = 0.18f), modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("📖", fontSize = 26.sp) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MUSIC THEORY BOOK", fontSize = 14.sp, fontWeight = FontWeight.Black, color = primaryColor, letterSpacing = 0.5.sp)
                        Text("Indian & Carnatic Classical, Rhythm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), lineHeight = 15.sp)
                    }
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = primaryColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }

            // 2. WORKSPACE NAV TABS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple("CHORDS", "Chords", Icons.Default.LibraryMusic),
                    Triple("PIANO", "Piano", Icons.Default.Piano),
                    Triple("DRUMS", "Drums", Icons.Default.Radio),
                    Triple("NOTATION", "Notation", Icons.Default.AudioFile),
                    Triple("RIYAAZ", "Riyaaz", Icons.Default.SelfImprovement),
                )

                tabs.forEach { (tabId, label, iconVector) ->
                    val isSelected = viewModel.selectedWorkspaceTab.value == tabId
                    Button(
                        onClick = { viewModel.selectedWorkspaceTab.value = tabId },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) primaryColor else primaryColor.copy(0.08f),
                            contentColor = if (isSelected) Color.White else primaryColor
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(iconVector, null, modifier = Modifier.size(13.dp))
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            // 3. TAB CONTENT ROUTER
            AnimatedContent(
                targetState = viewModel.selectedWorkspaceTab.value,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "tab_router"
            ) { currentTab ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (currentTab) {
                        "CHORDS" -> {
                            ChordDictionaryPanel(viewModel = viewModel)
                        }
                        "PIANO" -> {
                            PianoKeyboard88Keys(viewModel = viewModel, primaryColor = primaryColor)
                            StrobeTunerSection(viewModel)
                        }
                        "DRUMS" -> {
                            DrumAcademySection(viewModel, primaryColor)
                            Spacer(Modifier.height(16.dp))
                            SongArrangerTimeline(viewModel = viewModel, primaryColor = primaryColor, modifier = Modifier)                        }
                        "NOTATION" -> {
                            Box(modifier = Modifier.fillMaxWidth().height(650.dp)) {
                                NotationWorkspaceItem(
                                    viewModel = viewModel,
                                    accentColor = primaryColor,
                                    launchFilePicker = { viewModel.onPickScoreFileRequested?.invoke() }
                                )
                            }
                        }
                        "RIYAAZ" -> {
                            RiyaazScreen(
                                viewModel = viewModel,
                                primaryColor = primaryColor,
                                onStartRiyaaz = { scale, _, _ -> viewModel.startRiyaazEngine(scale, 16) },
                                onPlayTanpura = { scale, _ ->
                                    viewModel.audioEngine.playSample(scale, 0, "TANPURA")
                                    viewModel.isTanpuraActive.value = true
                                },
                                onStopTanpura = {
                                    viewModel.audioEngine.playSample("STOP", 0, "TANPURA")
                                    viewModel.isTanpuraActive.value = false
                                }
                            )
                        }
                        else -> {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }

            // 4. SPACER
            Spacer(Modifier.height(100.dp))
        }
    }
}



@Composable
fun pEQLearnScreen(viewModel: AppViewModel) {
    var showTheory by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showTheory,
        label = "theory_nav",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { isTheory ->
        if (isTheory) {
            MusicTheoryScreen(viewModel = viewModel, onBack = { showTheory = false })
        } else {
            PEQLearnScreen(
                viewModel = viewModel,
                onBack = { showTheory = true }
            )
        }
    }
}

@Composable
fun AnalysisPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
