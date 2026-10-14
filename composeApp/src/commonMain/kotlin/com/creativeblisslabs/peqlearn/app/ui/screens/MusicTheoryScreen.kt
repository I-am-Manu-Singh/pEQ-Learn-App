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

    Scaffold(
        topBar = {
       
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp,
                                vertical = 4.dp),
                            placeholder = { Text("Search chords, scales, ragas ..") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    }


                    // Navigation Tabs
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TheoryPart.entries) { part ->
                            val isSelected = selectedPart == part
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPart = part
                                    searchQuery = ""
                                },
                                label = {
                                    Text(
                                        text = part.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = part.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
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
                TheoryPart.CONCEPTS -> {
                    ConceptsBrowser(
                        concepts = filteredConcepts,
                        subFilterMode = subFilterMode,
                        onSubFilterChange = { subFilterMode = it },
                        selectedConcept = selectedConcept,
                        onSelectConcept = { selectedConcept = if (selectedConcept == it) null else it }
                    )
                }
                TheoryPart.STRUCTURE -> {
                    SongStructureAcademyPanel()
                }
                TheoryPart.INDIAN_CLASSICAL -> {
                    IndianClassicalInteractivePanel(viewModel = viewModel)
                }
                TheoryPart.KARNATIC -> {
                    KarnaticClassicalInteractivePanel(viewModel = viewModel)
                }
                else -> {
                    // Fallback for CHORDS, GROOVE, or future additions
                    Box(Modifier.fillMaxSize())
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("ALL", "STRUCTURE", "INDIAN_CLASSICAL", "KARNATIC")
                items(filters) { f ->
                    FilterChip(
                        selected = subFilterMode == f,
                        onClick = { onSubFilterChange(f) },
                        label = { Text(f.replace("_", " "), fontSize = 11.sp) }
                    )
                }
            }
        }

        item {
            Text(
                text = "${concepts.size} concepts available",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(concepts, key = { it.title }) { concept ->
            val isExpanded = selectedConcept == concept
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { onSelectConcept(concept) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                )
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = concept.shortDef,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text(concept.part.label, fontSize = 10.sp) },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = concept.longDef,
                                fontSize = 13.sp,
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
                                    Text("Formula / Notes:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    concept.highlightNotes.forEach { note ->
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$note",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
private fun SongStructureAcademyPanel() {
    val sections = listOf(
        Triple("Intro", 4, Color(0xFF42A5F5)),
        Triple("Verse 1", 16, Color(0xFF66BB6A)),
        Triple("Pre-Chorus", 8, Color(0xFFFFA726)),
        Triple("Chorus", 16, Color(0xFFAB47BC)),
        Triple("Verse 2", 16, Color(0xFF66BB6A)),
        Triple("Chorus 2", 16, Color(0xFFAB47BC)),
        Triple("Bridge", 8, Color(0xFFEC407A)),
        Triple("Chorus 3", 16, Color(0xFFAB47BC)),
        Triple("Outro", 8, Color(0xFF78909C))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text(
                text = "SONG STRUCTURE MAP & TIMELINE LAB",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Standard Pop/Rock Structure Timeline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp))
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
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
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

// ============================================================================
// 100% RECOVERED INTERACTIVE STRUCTURE LAB (TIMELINE & CONCEPT ANALYSIS)
// ============================================================================

@Composable
fun InteractiveStructureLab(
    concept: TheoryConcept,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var selectedBlockIdx by remember { mutableStateOf(0) }
    var activeSubFilterMode by remember { mutableStateOf("Timeline") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF12141F),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Lab Title & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.padding(6.dp).size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = concept.title.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = concept.subtitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = concept.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // Interactive Structure Blocks / Sections
            if (concept.structureBlocks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Song Structure Timeline & Cadence Analysis",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(concept.structureBlocks) { idx, block ->
                            val isSelected = selectedBlockIdx == idx
                            Surface(
                                onClick = { selectedBlockIdx = idx },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) accentColor else Color(0xFF1E2235),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.08f)
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                    Text(
                                        block.bars,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.7f) else accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Formula Breakdown & Note Interval Mapping
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0C12), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("THEORETICAL FORMULA", fontSize = 9.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                    Text(
                        concept.formula.ifEmpty { "Traditional Classical Structure" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                                Text(note, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Concept Description Body
            Text(
                text = concept.description,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
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
