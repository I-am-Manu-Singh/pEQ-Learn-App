package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

@Composable
fun DrumLibrary(
    viewModel: AppViewModel,
    onBack: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("Acoustic") }
    val categories = listOf("Acoustic", "Electronic 808/909", "World Percussion", "Lo-Fi Vintage", "Cinematic Impact")

    val sampleKits = remember(selectedCategory) {
        when (selectedCategory) {
            "Acoustic" -> listOf("Studio Birch Kit", "Nashville Maple Session", "Vintage Jazz Gretsch", "Deep Rock Punch")
            "Electronic 808/909" -> listOf("TR-808 Classic Boomy", "TR-909 House Punch", "Trap Sub-Bass Special", "Electro Pop Crisp")
            "World Percussion" -> listOf("Tabla Master Boli", "Mridangam Carnatic Soul", "Djembe West Africa", "Cajon Flamenco Box")
            "Lo-Fi Vintage" -> listOf("Dusty Tape Cassette", "Vinyl Warmth Crackle", "12-Bit SP-1200 Punch", "Bedroom Chillhop")
            else -> listOf("Trailer Heavy Sub Hit", "Orchestral Taiko Boom", "Metal Shaker Risers", "Sub Bass Drops")
        }
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF14171F), tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column {
                            Text("DRUM & SAMPLE LIBRARY", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("Factory Soundbanks • High-Precision Transient Samples", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Kits List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sampleKits) { kit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E28)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(kit, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("16 Velocity Multi-Samples • 24-bit 48kHz WAV", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                            }

                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("LOAD KIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KitDetailItem(
    name: String,
    samplesCount: Int,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onSelect,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
        ),
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            androidx.compose.material3.Text(
                name,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 13.sp,
                color = androidx.compose.ui.graphics.Color.White
            )
            androidx.compose.material3.Text(
                "$samplesCount Samples",
                fontSize = 11.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
