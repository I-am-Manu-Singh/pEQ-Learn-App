package com.creativeblisslabs.peqlearn.app.ui.screens

import com.creativeblisslabs.peqlearn.app.data.Raaga

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import androidx.compose.foundation.lazy.itemsIndexed
import com.creativeblisslabs.peqlearn.app.ui.components.pEQFilterChip
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.components.MetadataBadge
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTheoryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit = {}
) {
    var selectedPart by remember { mutableStateOf(TheoryPart.CONCEPTS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedConcept by remember { mutableStateOf<TheoryConcept?>(null) }
    var subFilterMode by remember { mutableStateOf("ALL") }

    val filteredConcepts = remember(searchQuery, selectedPart, subFilterMode) {
        ALL_CONCEPTS.filter { concept ->
            val matchesCategory = if (selectedPart == TheoryPart.CONCEPTS) {
                if (subFilterMode == "ALL") true else concept.part.name == subFilterMode
            } else {
                concept.part == selectedPart
            }
            val matchesQuery = searchQuery.isEmpty() ||
                concept.title.contains(searchQuery, ignoreCase = true) ||
                concept.shortDef.contains(searchQuery, ignoreCase = true) ||
                concept.longDef.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }
var isSearchActive by remember { mutableStateOf(false) }
    var isChordGalleryOpen by remember { mutableStateOf(false) }

    if (isChordGalleryOpen) {
        com.creativeblisslabs.peqlearn.app.ui.components.FullscreenDialog(
            onDismissRequest = { isChordGalleryOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ChordDisplayGalleryScreen(
                viewModel = viewModel,
                onBack = { isChordGalleryOpen = false }
            )
        }
    }

    Scaffold(
        topBar = {
       
             Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 0.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    if (isSearchActive) {
        // FULL WIDTH SEARCH BAR MODE
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.weight(1f), // Takes up all available space
            placeholder = { Text("Search chords, scales, ragas ..", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { 
                IconButton(onClick = { 
                    isSearchActive = false 
                    searchQuery = "" // Clear search when closing
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Standard back button behavior
                        contentDescription = "Close search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    } else {
        // TABS + SEARCH ICON MODE
        
        // LazyRow takes up remaining width next to the search icon
        val visibleMainTabs = remember {
            TheoryPart.entries.filter { it != TheoryPart.CHORDS && it != TheoryPart.GROOVE }
        }
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleMainTabs) { part ->
                val isSelected = selectedPart == part
                pEQFilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedPart = part
                        searchQuery = ""
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = part.icon,
                            contentDescription = part.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else part.color.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = part.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }

        // All Chords Gallery Button
        IconButton(
            onClick = { isChordGalleryOpen = true },
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = "Open Chord Gallery",
                tint = Color(0xFFFF9E1B)
            )
        }

        // The single search icon that triggers full width search
        IconButton(
            onClick = { isSearchActive = true },
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Open search",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
            
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedPart,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { activeSection ->
            when (activeSection) {
                TheoryPart.CHORDS -> {
                    ChordDictionaryPanel(viewModel = viewModel)
                }
                TheoryPart.REFERENCE -> {
                    ReferenceStudioPanel(
                        viewModel = viewModel,
                        onOpenChordGallery = { isChordGalleryOpen = true }
                    )
                }
                TheoryPart.TOOLS -> {
                    ToolsStudioPanel(viewModel = viewModel)
                }
                TheoryPart.PROGRESSIONS -> {
                    ProgressionsStudioPanel(viewModel = viewModel)
                }
                TheoryPart.CONCEPTS -> {
                    ConceptsBrowser(
                        concepts = filteredConcepts,
                        subFilterMode = subFilterMode,
                        onSubFilterChange = { subFilterMode = it },
                        selectedConcept = selectedConcept,
                        onSelectConcept = { selectedConcept = if (selectedConcept == it) null else it }
                    )
                }
                TheoryPart.GROOVE -> {
                    RhythmGrooveWorkstation(viewModel = viewModel)
                }
                TheoryPart.STRUCTURE -> {
                    SongStructureAcademyPanel(viewModel = viewModel)
                }
                TheoryPart.INDIAN_CLASSICAL -> {
                    IndianClassicalInteractivePanel(viewModel = viewModel)
                }
                TheoryPart.KARNATIC -> {
                    KarnaticClassicalInteractivePanel(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun ConceptsBrowser(
    concepts: List<TheoryConcept>,
    subFilterMode: String,
    onSubFilterChange: (String) -> Unit,
    selectedConcept: TheoryConcept?,
    onSelectConcept: (TheoryConcept) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
    ) {
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                val filters = listOf("ALL", "CHORDS", "GROOVE", "STRUCTURE", "INDIAN_CLASSICAL", "KARNATIC")
                items(filters) { f ->
                    pEQFilterChip(
                        selected = subFilterMode == f,
                        onClick = { onSubFilterChange(f) },
                        label = { Text(f.replace("_", " "), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        item {
            val auxSummary = remember {
                val hName = HarmonicType.NATURAL.displayName()
                val fTitle = FretboardViewMode.CAGED_BOX.title()
                val uSum = UnifiedShape("Major Triad", listOf(60, 64, 67)).summary()
                val pAlive = if (SparkParticle(0f, 0f, 0f, 0f).isAlive()) "Active" else "Spark"
                val vText = VisualizerModeItem("mode_1", "Score View").displayText()
                val sLabel = Suggestion("Explore Harmonics", "Learn N.H., A.H., and P.H.").displayLabel()
                "$hName • $fTitle • $uSum • $vText • $pAlive • $sLabel"
            }
            Text(
                text = "${concepts.size} concepts available • $auxSummary",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(concepts, key = { it.title }) { concept ->
            val isExpanded = selectedConcept == concept
            pEQGlassCard(
                modifier = Modifier.fillMaxWidth().clickable { onSelectConcept(concept) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(concept.emoji, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = concept.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = concept.shortDef,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MetadataBadge(
                            text = concept.part.label
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = concept.longDef,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Structure Lab & Timeline Analysis
                            InteractiveStructureLab(
                                concept = concept,
                                accentColor = MaterialTheme.colorScheme.primary
                            )

                            if (concept.highlightNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Formula / Notes:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    concept.highlightNotes.forEach { note ->
                                        Surface(
                                            modifier = Modifier.clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "$note",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SongStructureAcademyPanel(viewModel: AppViewModel? = null) {
    val importedAnalysis = viewModel?.songStructureAnalysis?.value
    val currentTrack = viewModel?.selectedMixerItem?.value ?: viewModel?.currentMediaItem?.value
    val isAnalyzing = viewModel?.isAnalyzingStructure?.value == true

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline

    val sections = remember(importedAnalysis, primaryColor, secondaryColor, tertiaryColor, errorColor, outlineColor) {
        if (importedAnalysis != null && importedAnalysis.segments.isNotEmpty()) {
            importedAnalysis.segments.map { segment ->
                val duration = (segment.end - segment.start).coerceAtLeast(1f)
                val bars = (duration / 2.0f).toInt().coerceAtLeast(1)
                Triple(
                    segment.label.replaceFirstChar { it.uppercase() },
                    bars,
                    com.creativeblisslabs.peqlearn.app.ui.components.AllInOneColorPalette.getColorForLabel(segment.label)
                )
            }
        } else {
            listOf(
                Triple("Intro", 4, primaryColor),
                Triple("Verse 1", 16, secondaryColor),
                Triple("Pre-Chorus", 8, tertiaryColor),
                Triple("Chorus", 16, errorColor),
                Triple("Verse 2", 16, secondaryColor),
                Triple("Chorus 2", 16, errorColor),
                Triple("Bridge", 8, primaryColor.copy(alpha = 0.7f)),
                Triple("Chorus 3", 16, errorColor),
                Triple("Outro", 8, outlineColor)
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SONG STRUCTURE MAP & TIMELINE LAB",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (importedAnalysis != null) "All-In-One Model Analysis: ${currentTrack?.displayName ?: "Imported Song"}"
                               else "Standard Reference Pop/Rock Arrangement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (viewModel != null) {
                    FilledTonalButton(
                        onClick = {
                            val track = viewModel.selectedMixerItem.value ?: viewModel.currentMediaItem.value
                            if (track != null) {
                                viewModel.runAllInOneAnalysis(track.path)
                            } else {
                                viewModel.onPickTrackRequested?.invoke { item ->
                                    viewModel.selectTrackForMixer(item)
                                }
                            }
                        },
                        enabled = !isAnalyzing,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Analyzing...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (currentTrack != null) "Re-Analyze" else "Import Song", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (viewModel != null) {
            item {
                com.creativeblisslabs.peqlearn.app.ui.components.SongWritingTimelineScrubber(
                    viewModel = viewModel
                )
            }
        }

        item {
            pEQGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (importedAnalysis != null) "Detected Song Structure (${importedAnalysis.bpm.toInt()} BPM)"
                        else "Standard Pop/Rock Structure Timeline",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().height(36.dp).clip(MaterialTheme.shapes.small)
                    ) {
                        sections.forEach { (name, bars, col) ->
                            Box(
                                modifier = Modifier
                                    .weight(bars.toFloat())
                                    .fillMaxHeight()
                                    .background(col)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$name (${bars}b)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        items(sections) { (name, bars, col) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape).background(col)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("$bars bars", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun InteractiveStructureLab(
    concept: TheoryConcept,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var selectedBlockIdx by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Interactive Structure Blocks / Sections (if present)
        if (concept.structureBlocks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Song Structure Timeline & Cadence Analysis",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(concept.structureBlocks) { idx, block ->
                        val isSelected = selectedBlockIdx == idx
                        Surface(
                            onClick = { selectedBlockIdx = idx },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.height(64.dp).defaultMinSize(minWidth = 90.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    block.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    block.bars,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else accentColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Formula Breakdown & Note Interval Mapping (if present)
        if (concept.formula.isNotEmpty() || concept.noteHighlights.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("THEORETICAL FORMULA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    Text(
                        concept.formula.ifEmpty { "Traditional Classical Structure" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    concept.noteHighlights.forEach { note ->
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                Text(note, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongStructureMapPanel(
    selectedConcept: TheoryConcept? = null,
    onBack: () -> Unit = {}
) {
    SongStructureAcademyPanel()
}
