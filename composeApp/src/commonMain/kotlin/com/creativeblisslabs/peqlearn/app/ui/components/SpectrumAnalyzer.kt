package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Size

/**
 * pEQ SPECTRUM ANALYZER: INDUSTRIAL FFT HUD Provides high-resolution frequency visualization for
 * signal monitoring.
 */
@Composable
fun SpectrumAnalyzer(
        magnitudes: FloatArray, // 0.0 to 1.0 magnitudes for frequency bins
        modifier: Modifier = Modifier
) {
        Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
                val width = size.width
                val height = size.height
                val barWidth = (width / magnitudes.size) * 0.8f
                val gap = (width / magnitudes.size) * 0.2f

                // 🚀 Aurora Glow Shader Background
                val sumMag = magnitudes.sum()
                val avgMag = if (magnitudes.isNotEmpty()) sumMag / magnitudes.size else 0f
                val pulseScale = 0.5f + (avgMag * 1.5f)
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0, 229, 255).copy(alpha = (0.08f * pulseScale).coerceIn(0f, 0.25f)),
                            Color.Transparent
                        )
                    ),
                    radius = (size.minDimension / 1.1f) * pulseScale,
                    center = Offset(width / 2f, height / 2f)
                )

                magnitudes.forEachIndexed { index, magnitude ->
                        val barHeight = magnitude * height
                        val x = index * (barWidth + gap)

                        // Industrial Gradient: Cyan to Deep Blue
                        val brush =
                                Brush.verticalGradient(
                                        colors =
                                                kotlin.collections.listOf(
                                                        Color(0, 229, 255),
                                                        Color(0, 176, 255).copy(alpha = 0.3f)
                                                ),
                                        startY = height - barHeight,
                                        endY = height
                                )

                        drawRoundRect(
                                brush = brush,
                                topLeft = Offset(x, height - barHeight),
                                size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx())
                        )
                }
        }
}

/**
 * pEQ CIRCULAR ANALYZER: NEURAL RING Provides a 360-degree frequency overview.
 */
@Composable
fun CircularSpectrumAnalyzer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(240.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val innerRadius = size.width * 0.25f
        val maxOuterRadius = size.width * 0.45f
        
        val sumMag = magnitudes.sum()
        val avgMag = if (magnitudes.isNotEmpty()) sumMag / magnitudes.size else 0f
        val pulseScale = 0.6f + (avgMag * 1.8f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0, 229, 255).copy(alpha = (0.12f * pulseScale).coerceIn(0f, 0.3f)),
                    Color.Transparent
                )
            ),
            radius = innerRadius * 1.8f * pulseScale,
            center = center
        )
        
        val angleStep = 360f / magnitudes.size
        
        magnitudes.forEachIndexed { index, magnitude ->
            val angle = index * angleStep
            val barHeight = magnitude * (maxOuterRadius - innerRadius)
            
            val start = Offset(
                x = center.x + innerRadius * kotlin.math.cos(angle * 3.14159f / 180f),
                y = center.y + innerRadius * kotlin.math.sin(angle * 3.14159f / 180f)
            )
            val end = Offset(
                x = center.x + (innerRadius + barHeight) * kotlin.math.cos(angle * 3.14159f / 180f),
                y = center.y + (innerRadius + barHeight) * kotlin.math.sin(angle * 3.14159f / 180f)
            )
            
            drawLine(
                color = Color(0, 229, 255).copy(alpha = 0.8f + (magnitude * 0.2f)),
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * pEQ SPECTROGRAM: TEMPORAL FREQUENCY MAP Displays frequency intensity over time.
 */
@Composable
fun SpectrogramAnalyzer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    // Placeholder for a real spectrogram which would require a history buffer
    Box(modifier = modifier.fillMaxWidth().height(120.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Spectrogram Mode (Placeholder)", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}
/**
 * pEQ RTA: INDUSTRIAL FREQUENCY FLUX
 * Provides mirrored vertical bars for a symmetrical balanced view.
 */
@Composable
fun RtaSpectrumVisualizer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp).padding(horizontal = 24.dp)) {
        val width = size.width
        val height = size.height
        val barCount = magnitudes.size
        val barWidth = (width / barCount) * 0.7f
        val gap = (width / barCount) * 0.3f
        val primary = Color(0, 229, 255)
        
        magnitudes.forEachIndexed { i, mag ->
            val barHeight = (mag * height).coerceIn(4.dp.toPx(), height)
            val x = i * (barWidth + gap)
            
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(primary, primary.copy(alpha = 0.2f), Color.Transparent)
                ),
                topLeft = Offset(x, (height - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

/**
 * pEQ WAVEFORM: NEURAL TEMPORAL MAP
 */
@Composable
fun WaveformVisualizer(
    viewModel: com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel,
    modifier: Modifier = Modifier
) {
    val magnitudes = viewModel.rtaData.value
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        val step = width / magnitudes.size
        
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(0f, height / 2)
        
        magnitudes.forEachIndexed { i, mag ->
            val x = i * step
            val y = (height / 2) + (if (i % 2 == 0) mag else -mag) * (height / 2)
            path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = Color(0, 229, 255),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * pEQ VU METER: ANALOG INTENSITY TELEMETRY
 */
@Composable
fun VuMeter(
    viewModel: com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel,
    modifier: Modifier = Modifier
) {
    val peak = viewModel.peakLevelDb.value
    val primary = Color(0, 229, 255)
    
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text("PEAK: ${peak.toInt()} dB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primary)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f))) {
            val progress = ((peak + 60f) / 60f).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(
                Brush.horizontalGradient(listOf(primary, Color.Red))
            ))
        }
    }
}
