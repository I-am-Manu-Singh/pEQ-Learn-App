package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.creativeblisslabs.peqlearn.app.learn.EarTrainingEngine
import com.creativeblisslabs.peqlearn.app.learn.SightReadingEngine
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * INTERACTIVE EAR TRAINING & SIGHT-READING ACADEMY
 * Directly ports and enhances midee's interval quiz and sight-reading trainer.
 */
@Composable
fun EarTrainingAcademyScreen(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onBack: () -> Unit = {}
) {
    var activeMode by remember { mutableStateOf("EAR_TRAINING") } // "EAR_TRAINING" or "SIGHT_READING"
    var tier by remember { mutableStateOf("Beginner") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text(
                        text = if (activeMode == "EAR_TRAINING") "INTERVAL EAR TRAINER" else "SIGHT-READING TRAINER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )
                    Text(
                        text = if (activeMode == "EAR_TRAINING") "Recognize musical distances by ear" else "Master note identification & key signatures",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Mode Toggle Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = activeMode == "EAR_TRAINING",
                    onClick = { activeMode = "EAR_TRAINING" },
                    label = { Text("Ear Trainer", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = activeMode == "SIGHT_READING",
                    onClick = { activeMode = "SIGHT_READING" },
                    label = { Text("Sight-Reading", fontSize = 11.sp) }
                )
            }
        }

        AnimatedContent(targetState = activeMode, label = "academy_switch") { mode ->
            if (mode == "EAR_TRAINING") {
                EarTrainingView(viewModel = viewModel, tier = tier, onTierChange = { tier = it })
            } else {
                SightReadingView(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun EarTrainingView(
    viewModel: AppViewModel,
    tier: String,
    onTierChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentQuestion by remember { mutableStateOf(EarTrainingEngine.generateQuestion(tier)) }
    var selectedChoice by remember { mutableStateOf<EarTrainingEngine.Interval?>(null) }
    var answerState by remember { mutableStateOf<Boolean?>(null) } // true = correct, false = incorrect
    var scoreCount by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }

    fun playInterval(harmonic: Boolean = false) {
        scope.launch {
            if (harmonic) {
                viewModel.audioEngine.noteOn(currentQuestion.rootPitch, 0.9f)
                viewModel.audioEngine.noteOn(currentQuestion.targetPitch, 0.9f)
                delay(1200)
                viewModel.audioEngine.noteOff(currentQuestion.rootPitch)
                viewModel.audioEngine.noteOff(currentQuestion.targetPitch)
            } else {
                viewModel.audioEngine.noteOn(currentQuestion.rootPitch, 0.9f)
                delay(600)
                viewModel.audioEngine.noteOff(currentQuestion.rootPitch)
                delay(100)
                viewModel.audioEngine.noteOn(currentQuestion.targetPitch, 0.9f)
                delay(800)
                viewModel.audioEngine.noteOff(currentQuestion.targetPitch)
            }
        }
    }

    LaunchedEffect(currentQuestion) {
        playInterval(harmonic = false)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // TIER SELECTOR & STATS BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Beginner", "Intermediate", "Advanced").forEach { t ->
                    AssistChip(
                        onClick = {
                            onTierChange(t)
                            currentQuestion = EarTrainingEngine.generateQuestion(t)
                            selectedChoice = null
                            answerState = null
                        },
                        label = { Text(t, fontSize = 11.sp, fontWeight = if (tier == t) FontWeight.Bold else FontWeight.Normal) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (tier == t) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E222D)
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Score: $scoreCount", fontSize = 11.sp, color = Color.White)
                    Text("Streak: $streak 🔥", fontSize = 11.sp, color = Color(0xFFFFD700))
                }
            }
        }

        // MAIN QUIZ CARD
        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Listen carefully to the two notes",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { playInterval(harmonic = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Melodic (One-by-One)")
                    }

                    OutlinedButton(
                        onClick = { playInterval(harmonic = true) }
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Harmonic (Together)")
                    }
                }

                // MULTIPLE CHOICE OPTIONS
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currentQuestion.choices.forEach { choice ->
                        val isSelected = selectedChoice == choice
                        val isCorrect = choice == currentQuestion.interval
                        val btnColor = when {
                            answerState != null && isCorrect -> Color(0xFF00E676)
                            answerState != null && isSelected && !isCorrect -> Color(0xFFFF5252)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = btnColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = answerState == null) {
                                    selectedChoice = choice
                                    val correct = isCorrect
                                    answerState = correct
                                    if (correct) {
                                        scoreCount += 10
                                        streak += 1
                                    } else {
                                        streak = 0
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${choice.shortName} (${choice.fullName})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (answerState != null && (isCorrect || isSelected)) Color.Black else Color.White
                                )
                                Text(
                                    text = "${choice.semitones} semitones",
                                    fontSize = 12.sp,
                                    color = if (answerState != null && (isCorrect || isSelected)) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // FEEDBACK & NEXT BUTTON
                if (answerState != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (answerState == true) "Correct! +10 Points" else "Miss! It was ${currentQuestion.interval.fullName}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (answerState == true) Color(0xFF00E676) else Color(0xFFFF5252)
                        )

                        Button(
                            onClick = {
                                currentQuestion = EarTrainingEngine.generateQuestion(tier)
                                selectedChoice = null
                                answerState = null
                            }
                        ) {
                            Text("Next Interval")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SightReadingView(viewModel: AppViewModel) {
    var selectedTierId by remember { mutableStateOf("landmark") }
    val currentTier = remember(selectedTierId) { SightReadingEngine.getTier(selectedTierId) }
    var currentPitch by remember { mutableStateOf(SightReadingEngine.nextNote(currentTier)) }
    var streak by remember { mutableIntStateOf(0) }
    val weakNotes = remember { mutableStateListOf<Int>() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // TIER CHIPS
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SightReadingEngine.TIERS.forEach { t ->
                FilterChip(
                    selected = selectedTierId == t.id,
                    onClick = {
                        selectedTierId = t.id
                        currentPitch = SightReadingEngine.nextNote(t, weakNotes = weakNotes.toSet())
                    },
                    label = { Text(t.name, fontSize = 11.sp) }
                )
            }
        }

        // FLASHCARD
        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "IDENTIFY THIS NOTE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E222D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = SightReadingEngine.pitchToName(currentPitch),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700)
                    )
                }

                Text(
                    text = "Press the key on the piano or choose below:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Quick choices
                val noteLetters = listOf("C", "D", "E", "F", "G", "A", "B")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    noteLetters.forEach { letter ->
                        Button(
                            onClick = {
                                val targetName = SightReadingEngine.pitchToName(currentPitch)
                                val correct = targetName.startsWith(letter)
                                if (correct) {
                                    streak++
                                    viewModel.audioEngine.noteOn(currentPitch, 0.9f)
                                    currentPitch = SightReadingEngine.nextNote(currentTier, lastPitch = currentPitch, weakNotes = weakNotes.toSet())
                                } else {
                                    streak = 0
                                    if (currentPitch !in weakNotes) weakNotes.add(currentPitch)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(letter, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Streak: $streak 🔥", color = Color(0xFFFFD700), fontSize = 12.sp)
                    if (weakNotes.isNotEmpty()) {
                        Text("${weakNotes.size} weak notes prioritized (3x frequency)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

