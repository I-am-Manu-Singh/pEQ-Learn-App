package com.creativeblisslabs.peqlearn.app.ui.screens

import com.creativeblisslabs.peqlearn.app.data.Raaga

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
        AnimatedContent<TheoryPart>(
            targetState = selectedPart,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "theory_part_anim"
        ) { activeSection ->
            when (activeSection) {
                TheoryPart.CHORDS -> {
                    ChordDictionaryPanel(viewModel = viewModel)
                }
               TheoryPart.REFERENCE -> {

               }
                TheoryPart.TOOLS -> {
            ToolsStudioPanel(viewModel = viewModel)
                }
                // TheoryPart.PROGRESSIONS -> {
                //     ProgressionsStudioPanel(viewModel = viewModel)
                // }
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
           
                TheoryPart.INDIAN_CLASSICAL -> {
                    IndianClassicalInteractivePanel(viewModel = viewModel)
                }
                TheoryPart.KARNATIC -> {
                    KarnaticClassicalInteractivePanel(viewModel = viewModel)
                }

                TheoryPart.REFERENCE -> TODO()
                TheoryPart.PROGRESSIONS -> TODO()
                TheoryPart.STRUCTURE -> TODO()
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
