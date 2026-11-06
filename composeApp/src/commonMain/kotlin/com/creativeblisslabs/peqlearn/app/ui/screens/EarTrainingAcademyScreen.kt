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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.learn.EarTrainingEngine
import com.creativeblisslabs.peqlearn.app.learn.SightReadingEngine
import com.creativeblisslabs.peqlearn.app.ui.components.MusicVectorSymbols
import com.creativeblisslabs.peqlearn.app.ui.components.pEQGlassCard
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EarTrainingAcademyScreen(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onBack: () -> Unit = {}
) {
    var activeMode by remember { mutableStateOf("SIGHT_READING") } // "SIGHT_READING", "EAR_TRAINING", "CHORD_EAR"
    var tier by remember { mutableStateOf("Beginner") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                        text = when (activeMode) {
                            "EAR_TRAINING" -> "INTERVAL EAR TRAINER"
                            "CHORD_EAR" -> "CHORD QUALITY TRAINER"
                            else -> "GRAND STAFF SIGHT-READING"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )
                    Text(
                        text = "Beginner, Intermediate & Advanced Pianist Mastery",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = activeMode == "SIGHT_READING",
                    onClick = { activeMode = "SIGHT_READING" },
                    label = { Text("Sight-Reading", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = activeMode == "EAR_TRAINING",
                    onClick = { activeMode = "EAR_TRAINING" },
                    label = { Text("Intervals", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = activeMode == "CHORD_EAR",
                    onClick = { activeMode = "CHORD_EAR" },
                    label = { Text("Chords", fontSize = 11.sp) }
                )
            }
        }

        AnimatedContent(targetState = activeMode, label = "academy_switch") { mode ->
            when (mode) {
                "EAR_TRAINING" -> EarTrainingView(viewModel = viewModel, tier = tier, onTierChange = { tier = it })
                "CHORD_EAR" -> ChordQualityEarTrainerView(viewModel = viewModel, tier = tier, onTierChange = { tier = it })
                else -> SightReadingView(viewModel = viewModel)
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
    var answerState by remember { mutableStateOf<Boolean?>(null) }
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
                delay(550)
                viewModel.audioEngine.noteOff(currentQuestion.rootPitch)
                delay(90)
                viewModel.audioEngine.noteOn(currentQuestion.targetPitch, 0.9f)
                delay(750)
                viewModel.audioEngine.noteOff(currentQuestion.targetPitch)
            }
        }
    }

    LaunchedEffect(currentQuestion) {
        playInterval(harmonic = false)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
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

            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1E222D)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Score: $scoreCount", fontSize = 11.sp, color = Color.White)
                    Text("Streak: $streak 🔥", fontSize = 11.sp, color = Color(0xFFFFD700))
                }
            }
        }

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Listen to the two notes and identify the interval distance", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { playInterval(harmonic = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Melodic (Arpeggiated)")
                    }
                    OutlinedButton(onClick = { playInterval(harmonic = true) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Harmonic (Together)")
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
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
                                    answerState = isCorrect
                                    if (isCorrect) {
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

                if (answerState != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (answerState == true) "Correct! +10 Points" else "It was ${currentQuestion.interval.fullName}",
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
private fun ChordQualityEarTrainerView(
    viewModel: AppViewModel,
    tier: String,
    onTierChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val chordTypes = remember(tier) {
        when (tier) {
            "Beginner" -> listOf(
                "Major Triad" to listOf(0, 4, 7),
                "Minor Triad" to listOf(0, 3, 7),
                "Suspended 4th" to listOf(0, 5, 7)
            )
            "Intermediate" -> listOf(
                "Major Triad" to listOf(0, 4, 7),
                "Minor Triad" to listOf(0, 3, 7),
                "Diminished" to listOf(0, 3, 6),
                "Augmented" to listOf(0, 4, 8),
                "Dominant 7th" to listOf(0, 4, 7, 10)
            )
            else -> listOf(
                "Major 7th" to listOf(0, 4, 7, 11),
                "Minor 7th" to listOf(0, 3, 7, 10),
                "Dominant 7th" to listOf(0, 4, 7, 10),
                "Half-Diminished (m7b5)" to listOf(0, 3, 6, 10),
                "Diminished 7th" to listOf(0, 3, 6, 9),
                "Major 9th" to listOf(0, 4, 7, 11, 14)
            )
        }
    }

    var rootMidi by remember { mutableIntStateOf(60) }
    var targetChord by remember(chordTypes) { mutableStateOf(chordTypes.random()) }
    var answerResult by remember { mutableStateOf<String?>(null) }
    var streak by remember { mutableIntStateOf(0) }

    fun playCurrentChord(arpeggiate: Boolean) {
        scope.launch {
            val notes = targetChord.second.map { rootMidi + it }
            if (arpeggiate) {
                notes.forEach { n ->
                    viewModel.audioEngine.noteOn(n, 0.85f)
                    delay(240)
                }
                delay(700)
                notes.forEach { viewModel.audioEngine.noteOff(it) }
            } else {
                notes.forEach { viewModel.audioEngine.noteOn(it, 0.85f) }
                delay(1100)
                notes.forEach { viewModel.audioEngine.noteOff(it) }
            }
        }
    }

    LaunchedEffect(targetChord, rootMidi) {
        playCurrentChord(arpeggiate = false)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            answerResult = null
                        },
                        label = { Text(t, fontSize = 11.sp, fontWeight = if (tier == t) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            Text("Streak: $streak 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        }

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Identify the Chord Harmony Quality", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { playCurrentChord(arpeggiate = false) }) {
                        Text("Play Block Chord")
                    }
                    OutlinedButton(onClick = { playCurrentChord(arpeggiate = true) }) {
                        Text("Arpeggiate Notes")
                    }
                }

                chordTypes.forEach { (name, intervals) ->
                    val isCorrect = name == targetChord.first
                    val bg = when {
                        answerResult != null && isCorrect -> Color(0xFF00E676)
                        answerResult == name && !isCorrect -> Color(0xFFFF5252)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = answerResult == null) {
                                answerResult = name
                                if (isCorrect) streak++ else streak = 0
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontWeight = FontWeight.Bold, color = if (answerResult != null && isCorrect) Color.Black else Color.White)
                            Text("Intervals: ${intervals.joinToString("-")}", fontSize = 11.sp, color = if (answerResult != null && isCorrect) Color.Black else Color.White.copy(alpha = 0.6f))
                        }
                    }
                }

                if (answerResult != null) {
                    Button(
                        onClick = {
                            rootMidi = (53..67).random()
                            targetChord = chordTypes.random()
                            answerResult = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Next Chord")
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
    var currentPitch by remember { mutableIntStateOf(SightReadingEngine.nextNote(currentTier)) }
    var showHintOnStaff by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var totalSolved by remember { mutableIntStateOf(0) }
    val weakNotes = remember { mutableStateListOf<Int>() }

    // Listen to live USB MIDI / On-Screen Piano Key presses for hands-free sight-reading!
    val livePlayedMidi = viewModel.midiPlayingNote.value
    LaunchedEffect(livePlayedMidi) {
        if (livePlayedMidi != null) {
            if (livePlayedMidi == currentPitch || (livePlayedMidi % 12) == (currentPitch % 12)) {
                streak++
                totalSolved++
                showHintOnStaff = false
                currentPitch = SightReadingEngine.nextNote(currentTier, lastPitch = currentPitch, weakNotes = weakNotes.toSet())
            } else {
                streak = 0
                if (currentPitch !in weakNotes) weakNotes.add(currentPitch)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
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

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "READ FROM GRAND STAFF (OR PLAY ON MIDI KEYBOARD)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showHintOnStaff = !showHintOnStaff }) {
                        Text(if (showHintOnStaff) "Hide Hint: ${SightReadingEngine.pitchToName(currentPitch)}" else "Show Hint", fontSize = 11.sp, color = Color(0xFFFFD700))
                    }
                }

                // Real Grand Staff Vector Note Flashcard Canvas (Treble + Bass Clef + Ledger Lines)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF141122),
                    border = BorderStroke(1.dp, Color(0xFF2E264E)),
                    modifier = Modifier.fillMaxWidth().height(195.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp)) {
                        val spacing = 12.dp.toPx()
                        val halfSpacing = spacing / 2f
                        val trebleTop = 20.dp.toPx()
                        val bassTop = trebleTop + 6.5f * spacing
                        val lineColor = Color.White.copy(alpha = 0.45f)
                        val clefColor = Color.White.copy(alpha = 0.9f)

                        for (i in 0..4) {
                            val ty = trebleTop + i * spacing
                            val by = bassTop + i * spacing
                            drawLine(lineColor, Offset(0f, ty), Offset(size.width, ty), strokeWidth = 1.3f)
                            drawLine(lineColor, Offset(0f, by), Offset(size.width, by), strokeWidth = 1.3f)
                        }

                        with(MusicVectorSymbols) {
                            val clefH = spacing * 6.4f
                            val clefW = clefH * 0.36f
                            drawTrebleClef(clefColor, Offset(12f, trebleTop + 3f * spacing - 0.63f * clefH), Size(clefW, clefH))
                            val bassH = spacing * 3.2f
                            val bassW = bassH * 0.81f
                            drawBassClef(clefColor, Offset(14f, bassTop + spacing - 0.32f * bassH), Size(bassW, bassH))
                        }

                        val isTreble = currentPitch >= 60
                        val step = ChordVoicingEngine.getDiatonicStep(currentPitch)
                        val ny = if (isTreble) {
                            trebleTop + (38f - step) * halfSpacing
                        } else {
                            bassTop + (26f - step) * halfSpacing
                        }
                        val nx = size.width * 0.54f
                        val sTop = if (isTreble) trebleTop else bassTop
                        val sBot = sTop + 4f * spacing

                        // Ledger lines
                        if (ny < sTop - halfSpacing * 0.8f) {
                            var ly = sTop - spacing
                            while (ly >= ny - halfSpacing * 0.6f) {
                                drawLine(Color.White.copy(alpha = 0.75f), Offset(nx - spacing, ly), Offset(nx + spacing, ly), strokeWidth = 1.5f)
                                ly -= spacing
                            }
                        } else if (ny > sBot + halfSpacing * 0.8f) {
                            var ly = sBot + spacing
                            while (ly <= ny + halfSpacing * 0.6f) {
                                drawLine(Color.White.copy(alpha = 0.75f), Offset(nx - spacing, ly), Offset(nx + spacing, ly), strokeWidth = 1.5f)
                                ly += spacing
                            }
                        }

                        with(MusicVectorSymbols) {
                            val nw = spacing * 1.25f
                            val nh = spacing * 0.90f
                            drawNoteHead(
                                color = Color(0xFFFFD700),
                                size = Size(nw, nh),
                                isHollow = false,
                                isWhole = false,
                                topLeft = Offset(nx - nw / 2f, ny - nh / 2f)
                            )
                            if ((currentPitch % 12) in listOf(1, 3, 6, 8, 10)) {
                                drawSharp(Color(0xFFFFD700), Size(spacing * 0.55f, spacing * 1.4f), Offset(nx - nw - 6f, ny - spacing * 0.7f))
                            }
                        }
                    }
                }

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
                                    totalSolved++
                                    showHintOnStaff = false
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
                    Text("Solved: $totalSolved • Streak: $streak 🔥", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (weakNotes.isNotEmpty()) {
                        Text("${weakNotes.size} weak notes prioritized (3x adaptive frequency)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}