package com.creativeblisslabs.peqlearn.app.ui.components

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interactive Circle of Fifths Component cloned directly from optilude/guitar-practice.
 * Displays:
 * 1. 12 Major Keys in Circle of 5ths order (Outer Ring)
 * 2. 12 Relative Minor Keys (Inner Ring)
 * 3. Accidentals for each key (# / b counts)
 * 4. Roman Numerals (I, ii, iii, IV, V, vi, vii°) relative to the selected tonic
 * 5. Tap interactions to select root key and preview diatonic family
 */
@Composable
fun InteractiveCircleOfFifths(
    selectedKey: String,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val majorKeys = remember { listOf("C", "G", "D", "A", "E", "B", "F#", "Db", "Ab", "Eb", "Bb", "F") }
    val relativeMinors = remember { listOf("Am", "Em", "Bm", "F#m", "C#m", "G#m", "D#m", "Bbm", "Fm", "Cm", "Gm", "Dm") }
    
    val diatonicRoman = remember {
        mapOf(
            0 to "I",
            1 to "V",
            2 to "ii",
            3 to "vi",
            4 to "iii",
            5 to "vii°",
            11 to "IV"
        )
    }

    val keyAccidentals = remember {
        listOf(
            "Natural",
            "1♯ (F♯)",
            "2♯ (F♯, C♯)",
            "3♯ (F♯, C♯, G♯)",
            "4♯ (4 sharps)",
            "5♯ (5 sharps)",
            "6♯ (6 sharps)",
            "5♭ (5 flats)",
            "4♭ (4 flats)",
            "3♭ (B♭, E♭, A♭)",
            "2♭ (B♭, E♭)",
            "1♭ (B♭)"
        )
    }

    val selectedIdx = majorKeys.indexOf(selectedKey).let { if (it == -1) 0 else it }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circle Canvas
        Box(
            modifier = Modifier
                .size(320.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedKey) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            
                            // Check if tapped within outer/inner ring (between 40dp and 150dp radius)
                            if (dist > 35.dp.toPx() && dist < 155.dp.toPx()) {
                                var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0) angle += 360f
                                val slice = ((angle + 15f) / 30f).toInt() % 12
                                onKeySelected(majorKeys[slice])
                            }
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.width * 0.48f
                val midR = size.width * 0.34f
                val innerR = size.width * 0.20f

                val sliceAngle = 360f / 12f

                // Draw 12 Slices
                for (i in 0 until 12) {
                    val startAngle = i * sliceAngle - sliceAngle / 2f - 90f
                    val sweepAngle = sliceAngle
                    val isSelected = (i == selectedIdx)
                    val offsetFromSelected = (i - selectedIdx + 12) % 12
                    val isDiatonic = diatonicRoman.containsKey(offsetFromSelected)

                    // 1. Outer Ring Slice (Major key)
                    val outerColor = when {
                        isSelected -> Color(0xFFFF9E1B)
                        isDiatonic -> Color(0xFF2A364F)
                        else -> Color(0xFF161A22)
                    }

                    drawArc(
                        color = outerColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = androidx.compose.ui.geometry.Size(outerR * 2, outerR * 2),
                        topLeft = Offset(cx - outerR, cy - outerR)
                    )

                    // 2. Middle Ring Slice (Relative minor key)
                    val innerColor = when {
                        isSelected -> Color(0xFFFFB347).copy(alpha = 0.5f)
                        isDiatonic -> Color(0xFF1E2838)
                        else -> Color(0xFF10131A)
                    }

                    drawArc(
                        color = innerColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = androidx.compose.ui.geometry.Size(midR * 2, midR * 2),
                        topLeft = Offset(cx - midR, cy - midR)
                    )
                }

                // Slice Divider Lines
                for (i in 0 until 12) {
                    val angleRad = ((i * sliceAngle - sliceAngle / 2f - 90f) * PI / 180f).toFloat()
                    val p1 = Offset(cx + innerR * cos(angleRad), cy + innerR * sin(angleRad))
                    val p2 = Offset(cx + outerR * cos(angleRad), cy + outerR * sin(angleRad))
                    drawLine(
                        color = Color(0xFF0F1218),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Inner Hub Circle
                drawCircle(
                    color = Color(0xFF0F1218),
                    radius = innerR,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF262D3D),
                    radius = innerR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Labels Overlay
            for (i in 0 until 12) {
                val midAngle = i * 30f - 90f
                val rad = (midAngle * PI / 180f).toFloat()
                val isSelected = (i == selectedIdx)
                val offsetFromSelected = (i - selectedIdx + 12) % 12
                val roman = diatonicRoman[offsetFromSelected]

                // Outer Major Label
                val outerDist = 125.dp
                Box(
                    modifier = Modifier.offset(
                        x = outerDist * cos(rad),
                        y = outerDist * sin(rad)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = majorKeys[i],
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                        if (roman != null) {
                            Text(
                                text = roman,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black.copy(alpha = 0.8f) else Color(0xFFFFB347)
                            )
                        }
                    }
                }

                // Inner Minor Label
                val innerDist = 80.dp
                Box(
                    modifier = Modifier.offset(
                        x = innerDist * cos(rad),
                        y = innerDist * sin(rad)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = relativeMinors[i],
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF9EABB8)
                    )
                }
            }

            // Center Display: Selected Tonic and Relative Minor
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedKey,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFB347)
                )
                Text(
                    text = relativeMinors[selectedIdx],
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9EABB8)
                )
            }
        }

        // Selected Key Information Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color(0xFF262D3D))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KEY OF $selectedKey MAJOR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF9E1B).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = keyAccidentals[selectedIdx],
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFB347),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Diatonic Chords in $selectedKey:",
                    fontSize = 11.sp,
                    color = Color(0xFF7E8B9B)
                )

                // 7 Diatonic Chords Row
                val diatonicChords = remember(selectedKey) {
                    computeDiatonicChords(selectedKey)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    diatonicChords.forEach { (roman, chordName) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E2430),
                            border = BorderStroke(1.dp, Color(0xFF2B3444))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = roman,
                                    fontSize = 10.sp,
                                    color = Color(0xFFFFB347),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = chordName,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun computeDiatonicChords(key: String): List<Pair<String, String>> {
    val chromatic = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val normKey = when (key) {
        "Db" -> "C#"
        "Eb" -> "D#"
        "Gb" -> "F#"
        "Ab" -> "G#"
        "Bb" -> "A#"
        else -> key
    }
    val rootIdx = chromatic.indexOf(normKey).let { if (it == -1) 0 else it }
    val majorIntervals = listOf(0, 2, 4, 5, 7, 9, 11)
    val romans = listOf("I", "ii", "iii", "IV", "V", "vi", "vii°")
    val qualities = listOf("", "m", "m", "", "", "m", "dim")

    return majorIntervals.mapIndexed { idx, semitones ->
        val note = chromatic[(rootIdx + semitones) % 12]
        romans[idx] to "$note${qualities[idx]}"
    }
}
