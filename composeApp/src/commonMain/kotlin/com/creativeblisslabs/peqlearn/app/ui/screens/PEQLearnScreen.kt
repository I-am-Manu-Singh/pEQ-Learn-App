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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.components.*
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
        val isLandscapeOrWide = maxWidth > 800.dp || maxWidth > maxHeight

        val tabs = listOf(
            Triple("CHORDS", "Chords", Icons.Default.LibraryMusic),
            Triple("REFERENCE", "Reference", Icons.Default.MenuBook),
            Triple("PROGRESSIONS", "Progressions", Icons.Default.AltRoute),
            Triple("TOOLS", "Tools", Icons.Default.Build),
            Triple("PIANO", "Piano & Notation", Icons.Default.Piano),
            Triple("DRUMS", "Drums", Icons.Default.Radio),
            Triple("RIYAAZ", "Riyaaz", Icons.Default.SelfImprovement),
        )

        if (isLandscapeOrWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT SIDEBAR: Music Theory Book + Workspace Navigation Rail/List
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // MUSIC THEORY BOOK ENTRY CARD
                    pEQGlassCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenTheory() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = primaryColor.copy(alpha = 0.18f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text("📖", fontSize = 22.sp) }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "THEORY BOOK",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = primaryColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Ragas, Scales & Rhythm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "WORKSPACES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )

                    // Vertical Workspace Tabs
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEach { (tabId, label, iconVector) ->
                            val isSelected = viewModel.selectedWorkspaceTab.value == tabId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectedWorkspaceTab.value = tabId },
                                color = if (isSelected) primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) primaryColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                // RIGHT CONTENT AREA: Active Workspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedContent(
                            targetState = viewModel.selectedWorkspaceTab.value,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "tab_router_wide"
                        ) { currentTab ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                when (currentTab) {
                                    "CHORDS" -> {
                                        ChordDictionaryPanel(viewModel = viewModel)
                                    }
                                    "REFERENCE" -> {
                                        ReferenceStudioPanel(
                                            viewModel = viewModel,
                                            onOpenChordGallery = {
                                                // opens gallery modal or view
                                            }
                                        )
                                    }
                                    "PROGRESSIONS" -> {
                                        ProgressionsStudioPanel(viewModel = viewModel)
                                    }
                                    "TOOLS" -> {
                                        ToolsStudioPanel(viewModel = viewModel)
                                    }
                                    "PIANO" -> {
                                        PianoKeyboard88Keys(viewModel = viewModel, primaryColor = primaryColor)
                                        StrobeTunerSection(viewModel)
                                    }
                                    "DRUMS" -> {
                                        DrumAcademySection(viewModel, primaryColor, modifier = Modifier)
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

                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        } else {
            // PORTRAIT SINGLE COLUMN LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. MUSIC THEORY BOOK ENTRY CARD
                val infiniteAnim = rememberInfiniteTransition(label = "theory_glow")
                val glowAlpha by infiniteAnim.animateFloat(
                    initialValue = 0.12f, targetValue = 0.26f,
                    animationSpec = infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Reverse),
                    label = "glow"
                )

                pEQGlassCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTheory() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium, 
                            color = primaryColor.copy(alpha = 0.18f), 
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("📖", fontSize = 26.sp) }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MUSIC THEORY BOOK", 
                                style = MaterialTheme.typography.labelLarge,
                                color = primaryColor, 
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Indian & Carnatic Classical, Rhythm", 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), 
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForwardIos, 
                            contentDescription = null, 
                            tint = primaryColor.copy(alpha = 0.5f), 
                            modifier = Modifier.size(16.dp)
                        )
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
                    tabs.forEach { (tabId, label, iconVector) ->
                        val isSelected = viewModel.selectedWorkspaceTab.value == tabId
                        pEQFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedWorkspaceTab.value = tabId },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(iconVector, null, modifier = Modifier.size(13.dp))
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        )
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
                            "REFERENCE" -> {
                                ReferenceStudioPanel(
                                    viewModel = viewModel,
                                    onOpenChordGallery = {
                                        // opens gallery modal or view
                                    }
                                )
                            }
                            "PROGRESSIONS" -> {
                                ProgressionsStudioPanel(viewModel = viewModel)
                            }
                            "TOOLS" -> {
                                ToolsStudioPanel(viewModel = viewModel)
                            }
                            "PIANO" -> {
                                PianoKeyboard88Keys(viewModel = viewModel, primaryColor = primaryColor)
                                StrobeTunerSection(viewModel)
                            }
                            "DRUMS" -> {
                                DrumAcademySection(viewModel, primaryColor)
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
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
