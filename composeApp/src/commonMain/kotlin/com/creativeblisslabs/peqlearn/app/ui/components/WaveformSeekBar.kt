package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random

@Composable
fun WaveformSeekbar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressFinished: () -> Unit,
    rtaData: FloatArray = FloatArray(32),
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.White.copy(alpha = 0.08f)
) {
    val barCount = 48 
    val density = LocalDensity.current
    
    // We use a persistent state for smoothing to ensure fluid animation even if RTA data is jittery
    val smoothedAmplitudes = remember { FloatArray(barCount) { 0.1f } }
    val baseWaveform = remember { List(barCount) { Random.nextFloat() * 0.25f + 0.15f } }

    val premiumGradients = remember {
        listOf(
            Color(0xFF00E5FF), // Electric Cyan
            Color(0xFF2979FF), // Bright Blue
            Color(0xFFD500F9), // Neon Purple
            Color(0xFFFF1744)  // Vivid Red (for peak/end)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 4.dp)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val barWidthPx = widthPx / barCount
        
        // High-sensitivity RTA mapping
        LaunchedEffect(rtaData) {
            for (i in 0 until barCount) {
                val rtaIndex = (i * rtaData.size / barCount).coerceIn(0, rtaData.size - 1)
                // Boosted sensitivity: 3.5x multiplier for more "jumpy" professional feel
                val target = (rtaData[rtaIndex] * 3.5f).coerceIn(0.02f, 1.0f)
                // Faster attack, slower decay for "punchy" visuals
                val decay = if (target > smoothedAmplitudes[i]) 0.7f else 0.3f
                smoothedAmplitudes[i] = smoothedAmplitudes[i] * (1f - decay) + target * decay
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            val progress = (offset.x / widthPx).coerceIn(0f, 1f)
                            onProgressChange(progress)
                            onProgressFinished()
                        }
                    )
                }
                .pointerInput(widthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                             onProgressChange((offset.x / widthPx).coerceIn(0f, 1f))
                        },
                        onHorizontalDrag = { change, _ ->
                            onProgressChange((change.position.x / widthPx).coerceIn(0f, 1f))
                        },
                        onDragEnd = { onProgressFinished() },
                        onDragCancel = { onProgressFinished() }
                    )
                }
        ) {
            for (i in 0 until barCount) {
                val isPast = (i.toFloat() / barCount) <= progress
                val amplitude = smoothedAmplitudes[i]
                
                // Combine base static waveform with dynamic RTA
                val barHeight = (baseWaveform[i] * 0.4f + amplitude * 0.8f).coerceIn(0.12f, 1.0f) * heightPx
                
                val x = i * barWidthPx
                val y = (heightPx - barHeight) / 2
                
                // Dynamic multi-stop gradient calculation
                val color = if (isPast) {
                    val pos = i.toFloat() / barCount
                    val segment = 1.0f / (premiumGradients.size - 1)
                    val colorIndex = (pos / segment).toInt().coerceIn(0, premiumGradients.size - 2)
                    val fraction = (pos % segment) / segment
                    lerp(premiumGradients[colorIndex], premiumGradients[colorIndex + 1], fraction)
                } else {
                    inactiveColor
                }

                // Main bar
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x + 2.dp.toPx(), y),
                    size = Size(barWidthPx - 4.dp.toPx(), barHeight),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                
                // High-fidelity reactivity: Add neon glow to active vibrating bars
                if (isPast && amplitude > 0.25f) {
                    drawRoundRect(
                        color = color.copy(alpha = 0.35f * amplitude),
                        topLeft = Offset(x - 1.dp.toPx(), y - 4.dp.toPx()),
                        size = Size(barWidthPx + 2.dp.toPx(), barHeight + 8.dp.toPx()),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                }
            }
            
            // Minimalist neon playhead
            val progressX = progress * widthPx
            drawLine(
                color = Color.White,
                start = Offset(progressX, 0f),
                end = Offset(progressX, heightPx),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Playhead tip glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = Offset(progressX, heightPx / 2),
                    radius = 12.dp.toPx()
                ),
                radius = 12.dp.toPx(),
                center = Offset(progressX, heightPx / 2)
            )
        }
    }
}
