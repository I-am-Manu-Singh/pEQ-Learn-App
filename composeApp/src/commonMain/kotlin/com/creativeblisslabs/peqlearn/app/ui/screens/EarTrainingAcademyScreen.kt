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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.abs

@Composable
fun EarTrainingAcademyScreen(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onBack: () -> Unit = {}
) {
    // 5 Complete Academy Modules:
    // "SIGHT_READING", "RHYTHM_TRAINER", "EAR_INTERVALS", "CHORD_EAR", "MELODIC_DICTATION"
    var activeMode by remember { mutableStateOf("SIGHT_READING") }
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
                            "RHYTHM_TRAINER" -> "RHYTHM & TIMING TAPPER ACADEMY"
                            "EAR_INTERVALS" -> "INTERVAL EAR TRAINER"
                            "CHORD_EAR" -> "CHORD HARMONY & INVERSION TRAINER"
                            "MELODIC_DICTATION" -> "MELODIC PLAYBACK & SCALE/RAGA EAR"
                            else -> "GRAND STAFF SIGHT-READING STUDIO"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )
                    Text(
                        text = "Complete Musicianship Curriculum for Beginner, Intermediate & Advanced Pianists",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 5-Module Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "SIGHT_READING" to "1. Grand Staff Sight-Reading",
                "RHYTHM_TRAINER" to "2. Rhythm & Subdivision Tapper",
                "EAR_INTERVALS" to "3. Interval Ear Trainer",
                "CHORD_EAR" to "4. Chord & Inversion Ear",
                "MELODIC_DICTATION" to "5. Melodic Dictation & Scales"
            ).forEach { (modeKey, label) ->
                FilterChip(
                    selected = activeMode == modeKey,
                    onClick = { activeMode = modeKey },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (activeMode == modeKey) FontWeight.Bold else FontWeight.Medium) }
                )
            }
        }

        AnimatedContent(targetState = activeMode, label = "academy_switch") { mode ->
            when (mode) {
                "RHYTHM_TRAINER" -> RhythmTrainingStudioView(viewModel = viewModel, tier = tier, onTierChange = { tier = it })
                "EAR_INTERVALS" -> EarTrainingView(viewModel = viewModel, tier = tier, onTierChange = { tier = it })
                "CHORD_EAR" -> ChordQualityEarTrainerView(viewModel = viewModel, tier = tier, onTierChange = { tier = it })
                "MELODIC_DICTATION" -> MelodicDictationAndScaleEarView(viewModel = viewModel, tier = tier, onTierChange = { tier = it })
                else -> SightReadingView(viewModel = viewModel)
            }
        }
    }
}

private data class RhythmPatternChallenge(
    val title: String,
    val tier: String,
    val timeSig: String,
    val bpm: Int,
    val beatOffsets: List<Float>, // Onset times in quarter-note beats (0.0 .. 3.75)
    val durations: List<Float>,
    val notationLabel: String
)

@Composable
private fun RhythmTrainingStudioView(
    viewModel: AppViewModel,
    tier: String,
    onTierChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val challenges = remember {
        listOf(
            RhythmPatternChallenge("Four Quarter Beats", "Beginner", "4/4", 80, listOf(0f, 1f, 2f, 3f), listOf(1f, 1f, 1f, 1f), "♩   ♩   ♩   ♩"),
            RhythmPatternChallenge("Half & Quarter Mix", "Beginner", "4/4", 84, listOf(0f, 2f, 3f), listOf(2f, 1f, 1f), "𝅗𝅥     ♩   ♩"),
            RhythmPatternChallenge("Eighth-Note Pulse", "Beginner", "4/4", 80, listOf(0f, 0.5f, 1f, 2f, 2.5f, 3f), listOf(0.5f, 0.5f, 1f, 0.5f, 0.5f, 1f), "♫  ♩   ♫  ♩"),
            RhythmPatternChallenge("Dotted Quarter & Eighth", "Intermediate", "4/4", 88, listOf(0f, 1.5f, 2f, 3f), listOf(1.5f, 0.5f, 1f, 1f), "♩.  ♪  ♩   ♩"),
            RhythmPatternChallenge("Syncopated Off-Beat", "Intermediate", "4/4", 86, listOf(0f, 0.5f, 1.5f, 2f, 3f), listOf(0.5f, 1f, 0.5f, 1f, 1f), "♪ ♩ ♪  ♩   ♩"),
            RhythmPatternChallenge("Triplet Flow", "Intermediate", "4/4", 80, listOf(0f, 1f, 1.333f, 1.666f, 2f, 3f), listOf(1f, 0.33f, 0.33f, 0.33f, 1f, 1f), "♩  [3: ♫♪]  ♩  ♩"),
            RhythmPatternChallenge("16th-Note Gallop", "Advanced", "4/4", 92, listOf(0f, 0.5f, 0.75f, 1f, 2f, 2.25f, 2.5f, 3f), listOf(0.5f, 0.25f, 0.25f, 1f, 0.25f, 0.25f, 0.5f, 1f), "♪ ♬  ♩   ♬ ♪  ♩"),
            RhythmPatternChallenge("3:2 Polyrhythm Clave", "Advanced", "4/4", 90, listOf(0f, 0.75f, 1.5f, 2.5f, 3f), listOf(0.75f, 0.75f, 1f, 0.5f, 1f), "♩.  ♩.  ♩   ♪ ♩")
        )
    }

    val tierChallenges = remember(tier) {
        challenges.filter { it.tier.equals(tier, ignoreCase = true) }.ifEmpty { challenges }
    }
    var currentChallenge by remember(tierChallenges) { mutableStateOf(tierChallenges.first()) }
    var isPlayingDemo by remember { mutableStateOf(false) }
    var isUserTurnActive by remember { mutableStateOf(false) }
    var activePlayheadBeat by remember { mutableFloatStateOf(-1f) }
    val userTapBeats = remember { mutableStateListOf<Float>() }
    var turnStartMarkMs by remember { mutableLongStateOf(0L) }
    var accuracyFeedback by remember { mutableStateOf<String?>(null) }
    var streak by remember { mutableIntStateOf(0) }

    val msPerBeat = 60_000f / currentChallenge.bpm.toFloat()

    fun startRhythmDrill(demoOnly: Boolean) {
        scope.launch {
            isPlayingDemo = demoOnly
            isUserTurnActive = !demoOnly
            userTapBeats.clear()
            accuracyFeedback = null

            // 4-beat count-in with metronome woodblock
            for (countBeat in 1..4) {
                viewModel.audioEngine.playSample(
                    if (countBeat == 1) "METRONOME-STRONG" else "METRONOME-WEAK",
                    0,
                    "SYSTEM"
                )
                delay(msPerBeat.toLong())
            }

            turnStartMarkMs = System.currentTimeMillis()
            val totalDurationMs = (4f * msPerBeat).toLong()
            val firedIndices = HashSet<Int>()

            while (System.currentTimeMillis() - turnStartMarkMs < totalDurationMs) {
                val elapsedMs = (System.currentTimeMillis() - turnStartMarkMs).toFloat()
                val curBeat = elapsedMs / msPerBeat
                activePlayheadBeat = curBeat

                if (demoOnly) {
                    currentChallenge.beatOffsets.forEachIndexed { idx, targetBeat ->
                        if (idx !in firedIndices && curBeat >= targetBeat) {
                            firedIndices.add(idx)
                            viewModel.audioEngine.playSample("SNARE", 1, "DRUM")
                        }
                    }
                }
                delay(12L)
            }

            activePlayheadBeat = -1f
            isPlayingDemo = false

            if (isUserTurnActive) {
                isUserTurnActive = false
                // Grade user taps against currentChallenge.beatOffsets
                if (userTapBeats.isEmpty()) {
                    accuracyFeedback = "No taps detected! Tap the big RHYTHM PAD (or press any MIDI key) in time with the beat."
                    streak = 0
                } else {
                    var totalErrorMs = 0f
                    var matchedCount = 0
                    for (expectedBeat in currentChallenge.beatOffsets) {
                        val closestTap = userTapBeats.minByOrNull { abs(it - expectedBeat) }
                        if (closestTap != null) {
                            val errMs = abs(closestTap - expectedBeat) * msPerBeat
                            totalErrorMs += errMs
                            if (errMs <= 95f) matchedCount++
                        }
                    }
                    val avgErr = (totalErrorMs / currentChallenge.beatOffsets.size.coerceAtLeast(1)).toInt()
                    val pct = ((matchedCount.toFloat() / currentChallenge.beatOffsets.size.coerceAtLeast(1)) * 100f).toInt().coerceIn(0, 100)
                    if (pct >= 80) streak++ else streak = 0
                    accuracyFeedback = "Accuracy: $pct% ($matchedCount/${currentChallenge.beatOffsets.size} hits) • Avg Timing Offset: ${avgErr}ms"
                }
            }
        }
    }

    fun recordUserTap() {
        viewModel.audioEngine.playSample("SNARE", 1, "DRUM")
        if (isUserTurnActive && turnStartMarkMs > 0L) {
            val elapsedMs = (System.currentTimeMillis() - turnStartMarkMs).toFloat()
            val beat = (elapsedMs / msPerBeat).coerceIn(0f, 4f)
            userTapBeats.add(beat)
        }
    }

    // Also allow tapping any USB MIDI key during Rhythm Trainer!
    val liveMidiNote = viewModel.midiPlayingNote.value
    LaunchedEffect(liveMidiNote) {
        if (liveMidiNote != null && isUserTurnActive) {
            recordUserTap()
        }
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
                        onClick = { onTierChange(t) },
                        label = { Text(t, fontSize = 11.sp, fontWeight = if (tier == t) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            Text("Rhythm Streak: $streak 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        }

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${currentChallenge.title} (${currentChallenge.timeSig} @ ${currentChallenge.bpm} BPM)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = currentChallenge.notationLabel,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )

                // Visual 4-Beat Timeline Grid with Target Markers, Playhead & User Tap Dots
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141122),
                    border = BorderStroke(1.dp, Color(0xFF2E264E)),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
                        val w = size.width
                        val midY = size.height * 0.5f

                        // Base timeline bar
                        drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, midY), Offset(w, midY), strokeWidth = 3f)

                        // 4 Quarter-beat ticks
                        for (b in 0..4) {
                            val bx = (b / 4f) * w
                            drawLine(Color.White.copy(alpha = 0.45f), Offset(bx, midY - 16f), Offset(bx, midY + 16f), strokeWidth = 2f)
                        }

                        // Target Rhythm Note Markers
                        currentChallenge.beatOffsets.forEach { beatOff ->
                            val nx = (beatOff / 4f) * w
                            drawRoundRect(
                                color = Color(0xFF4A90E2),
                                topLeft = Offset(nx - 8f, midY - 14f),
                                size = Size(16f, 28f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }

                        // User Tap Dots (Green if within 0.15 beats of a target, Red if off-beat)
                        userTapBeats.forEach { tapB ->
                            val tx = (tapB / 4f) * w
                            val isClose = currentChallenge.beatOffsets.any { abs(it - tapB) <= 0.18f }
                            drawCircle(
                                color = if (isClose) Color(0xFF00E676) else Color(0xFFFF5252),
                                radius = 7.dp.toPx(),
                                center = Offset(tx, midY)
                            )
                        }

                        // Moving Playhead
                        if (activePlayheadBeat in 0f..4f) {
                            val px = (activePlayheadBeat / 4f) * w
                            drawLine(Color(0xFFFFD700), Offset(px, 0f), Offset(px, size.height), strokeWidth = 3.dp.toPx())
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { startRhythmDrill(demoOnly = true) },
                        enabled = !isPlayingDemo && !isUserTurnActive
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("1. Listen to Rhythm")
                    }

                    Button(
                        onClick = { startRhythmDrill(demoOnly = false) },
                        enabled = !isPlayingDemo && !isUserTurnActive,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("2. Count-In & Tap Test", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val nextIdx = (tierChallenges.indexOf(currentChallenge) + 1) % tierChallenges.size
                            currentChallenge = tierChallenges[nextIdx]
                            userTapBeats.clear()
                            accuracyFeedback = null
                        }
                    ) {
                        Text("Next Pattern")
                    }
                }

                // Giant Responsive Drum/Rhythm Tap Pad
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUserTurnActive) Color(0xFF281E4A) else Color(0xFF1A162B),
                    border = BorderStroke(2.dp, if (isUserTurnActive) Color(0xFF00E676) else Color(0xFF3D3264)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clickable { recordUserTap() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isUserTurnActive) "TAP HERE IN TIME WITH THE RHYTHM! (OR PRESS USB MIDI KEY)"
                            else "TAP PAD TO AUDITION SNARE / START COUNT-IN ABOVE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isUserTurnActive) Color(0xFF00E676) else Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                if (accuracyFeedback != null) {
                    Text(
                        text = accuracyFeedback!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

@Composable
private fun MelodicDictationAndScaleEarView(
    viewModel: AppViewModel,
    tier: String,
    onTierChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scalesPool = remember(tier) {
        when (tier) {
            "Beginner" -> listOf(
                "Major Scale (Ionian / Bilawal)" to listOf(0, 2, 4, 5, 7, 9, 11, 12),
                "Natural Minor (Aeolian / Asavari)" to listOf(0, 2, 3, 5, 7, 8, 10, 12),
                "Major Pentatonic" to listOf(0, 2, 4, 7, 9, 12)
            )
            "Intermediate" -> listOf(
                "Major Scale (Ionian)" to listOf(0, 2, 4, 5, 7, 9, 11, 12),
                "Harmonic Minor" to listOf(0, 2, 3, 5, 7, 8, 11, 12),
                "Melodic Minor" to listOf(0, 2, 3, 5, 7, 9, 11, 12),
                "Dorian Mode (Kafi)" to listOf(0, 2, 3, 5, 7, 9, 10, 12),
                "Mixolydian Mode (Khamaj)" to listOf(0, 2, 4, 5, 7, 9, 10, 12)
            )
            else -> listOf(
                "Lydian Mode (Kalyan / Yaman)" to listOf(0, 2, 4, 6, 7, 9, 11, 12),
                "Phrygian Mode (Bhairavi)" to listOf(0, 1, 3, 5, 7, 8, 10, 12),
                "Double Harmonic (Bhairav)" to listOf(0, 1, 4, 5, 7, 8, 11, 12),
                "Harmonic Minor" to listOf(0, 2, 3, 5, 7, 8, 11, 12),
                "Whole-Tone Scale" to listOf(0, 2, 4, 6, 8, 10, 12),
                "Blues Scale" to listOf(0, 3, 5, 6, 7, 10, 12)
            )
        }
    }

    var rootMidi by remember { mutableIntStateOf(60) }
    var targetScale by remember(scalesPool) { mutableStateOf(scalesPool.random()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var streak by remember { mutableIntStateOf(0) }

    fun playScaleAudio() {
        scope.launch {
            for (semi in targetScale.second) {
                viewModel.audioEngine.noteOn(rootMidi + semi, 0.85f)
                delay(260L)
                viewModel.audioEngine.noteOff(rootMidi + semi)
            }
        }
    }

    LaunchedEffect(targetScale, rootMidi) {
        playScaleAudio()
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
                        onClick = { onTierChange(t); selectedAnswer = null },
                        label = { Text(t, fontSize = 11.sp, fontWeight = if (tier == t) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            Text("Scale Ear Streak: $streak 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        }

        pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Listen to the ascending scale/mode and identify its tonal color", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))

                Button(onClick = { playScaleAudio() }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Replay Scale / Mode")
                }

                scalesPool.forEach { (scaleName, formula) ->
                    val isCorrect = scaleName == targetScale.first
                    val bg = when {
                        selectedAnswer != null && isCorrect -> Color(0xFF00E676)
                        selectedAnswer == scaleName && !isCorrect -> Color(0xFFFF5252)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = selectedAnswer == null) {
                                selectedAnswer = scaleName
                                if (isCorrect) streak++ else streak = 0
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                scaleName,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAnswer != null && isCorrect) Color.Black else Color.White
                            )
                            Text(
                                "Semitones: ${formula.joinToString("-")}",
                                fontSize = 11.sp,
                                color = if (selectedAnswer != null && isCorrect) Color.Black else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (selectedAnswer != null) {
                    Button(
                        onClick = {
                            rootMidi = listOf(57, 60, 62, 65, 67).random()
                            targetScale = scalesPool.random()
                            selectedAnswer = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Next Scale Challenge")
                    }
                }
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
                "Major Triad (Root Pos)" to listOf(0, 4, 7),
                "Minor Triad (Root Pos)" to listOf(0, 3, 7),
                "Suspended 4th (sus4)" to listOf(0, 5, 7),
                "Suspended 2nd (sus2)" to listOf(0, 2, 7)
            )
            "Intermediate" -> listOf(
                "Major Triad" to listOf(0, 4, 7),
                "Minor Triad" to listOf(0, 3, 7),
                "Diminished Triad" to listOf(0, 3, 6),
                "Augmented Triad" to listOf(0, 4, 8),
                "Dominant 7th (7)" to listOf(0, 4, 7, 10),
                "Major 1st Inversion" to listOf(4, 7, 12)
            )
            else -> listOf(
                "Major 7th (maj7)" to listOf(0, 4, 7, 11),
                "Minor 7th (m7)" to listOf(0, 3, 7, 10),
                "Dominant 7th (7)" to listOf(0, 4, 7, 10),
                "Half-Diminished (m7b5)" to listOf(0, 3, 6, 10),
                "Fully Diminished 7th (dim7)" to listOf(0, 3, 6, 9),
                "Major 9th (maj9)" to listOf(0, 4, 7, 11, 14)
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
                            Text("Intervals: ${intervals.joinToString("-")}", fontSize = 11.sp, color = if (selectedAnswerColor(answerResult, isCorrect)) Color.Black else Color.White.copy(alpha = 0.6f))
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

private fun selectedAnswerColor(answerResult: String?, isCorrect: Boolean): Boolean {
    return answerResult != null && isCorrect
}

@Composable
private fun SightReadingView(viewModel: AppViewModel) {
    var selectedTierId by remember { mutableStateOf("landmark") }
    val currentTier = remember(selectedTierId) { SightReadingEngine.getTier(selectedTierId) }
    var phraseLength by remember { mutableIntStateOf(1) } // 1 = Single Note Flashcard, 4 = 4-Note Melodic Phrase Sight-Reading
    var targetPhrase by remember(currentTier, phraseLength) {
        mutableStateOf(List(phraseLength) { SightReadingEngine.nextNote(currentTier) })
    }
    var currentPhraseStep by remember { mutableIntStateOf(0) }
    var showHintOnStaff by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var totalSolved by remember { mutableIntStateOf(0) }
    val weakNotes = remember { mutableStateListOf<Int>() }

    val currentPitch = targetPhrase.getOrElse(currentPhraseStep) { 60 }

    fun advanceOrGenerateNext() {
        streak++
        totalSolved++
        showHintOnStaff = false
        if (currentPhraseStep + 1 < targetPhrase.size) {
            currentPhraseStep++
        } else {
            currentPhraseStep = 0
            var lastP = currentPitch
            targetPhrase = List(phraseLength) {
                val nextP = SightReadingEngine.nextNote(currentTier, lastPitch = lastP, weakNotes = weakNotes.toSet())
                lastP = nextP
                nextP
            }
        }
    }

    val livePlayedMidi = viewModel.midiPlayingNote.value
    LaunchedEffect(livePlayedMidi) {
        if (livePlayedMidi != null) {
            if (livePlayedMidi == currentPitch || (livePlayedMidi % 12) == (currentPitch % 12)) {
                advanceOrGenerateNext()
            } else {
                streak = 0
                if (currentPitch !in weakNotes) weakNotes.add(currentPitch)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SightReadingEngine.TIERS.forEach { t ->
                    FilterChip(
                        selected = selectedTierId == t.id,
                        onClick = {
                            selectedTierId = t.id
                            currentPhraseStep = 0
                            targetPhrase = List(phraseLength) { SightReadingEngine.nextNote(t, weakNotes = weakNotes.toSet()) }
                        },
                        label = { Text(t.name, fontSize = 11.sp) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1 to "Single Note", 4 to "4-Note Phrase").forEach { (len, lbl) ->
                    AssistChip(
                        onClick = {
                            phraseLength = len
                            currentPhraseStep = 0
                            targetPhrase = List(len) { SightReadingEngine.nextNote(currentTier, weakNotes = weakNotes.toSet()) }
                        },
                        label = { Text(lbl, fontSize = 10.sp, fontWeight = if (phraseLength == len) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
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
                        text = if (phraseLength == 1) "READ NOTE FROM GRAND STAFF (PLAY ON USB MIDI OR TAP BELOW)"
                        else "SIGHT-READ 4-NOTE PHRASE IN ORDER (NOTE ${currentPhraseStep + 1} OF ${targetPhrase.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showHintOnStaff = !showHintOnStaff }) {
                        Text(
                            if (showHintOnStaff) "Hint: ${SightReadingEngine.pitchToName(currentPitch)}" else "Show Hint",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

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

                        targetPhrase.forEachIndexed { idx, pitch ->
                            val isTreble = pitch >= 60
                            val step = ChordVoicingEngine.getDiatonicStep(pitch)
                            val ny = if (isTreble) {
                                trebleTop + (38f - step) * halfSpacing
                            } else {
                                bassTop + (26f - step) * halfSpacing
                            }
                            val nx = if (targetPhrase.size == 1) {
                                size.width * 0.54f
                            } else {
                                val startX = size.width * 0.28f
                                val spanW = size.width * 0.58f
                                startX + (idx.toFloat() / (targetPhrase.size - 1).coerceAtLeast(1)) * spanW
                            }
                            val sTop = if (isTreble) trebleTop else bassTop
                            val sBot = sTop + 4f * spacing

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

                            val noteColor = when {
                                idx < currentPhraseStep -> Color(0xFF00E676) // Solved!
                                idx == currentPhraseStep -> Color(0xFFFFD700) // Active target!
                                else -> Color.White.copy(alpha = 0.75f)
                            }

                            with(MusicVectorSymbols) {
                                val nw = spacing * 1.25f
                                val nh = spacing * 0.90f
                                drawNoteHead(
                                    color = noteColor,
                                    size = Size(nw, nh),
                                    isHollow = false,
                                    isWhole = false,
                                    topLeft = Offset(nx - nw / 2f, ny - nh / 2f)
                                )
                                if ((pitch % 12) in listOf(1, 3, 6, 8, 10)) {
                                    drawSharp(noteColor, Size(spacing * 0.55f, spacing * 1.4f), Offset(nx - nw - 6f, ny - spacing * 0.7f))
                                }
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
                                    viewModel.audioEngine.noteOn(currentPitch, 0.9f)
                                    advanceOrGenerateNext()
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