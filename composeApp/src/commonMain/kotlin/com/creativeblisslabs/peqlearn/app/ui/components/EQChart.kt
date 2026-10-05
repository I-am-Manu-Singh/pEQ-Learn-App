package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.peqlearn.app.data.Preset
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.exp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import androidx.compose.ui.geometry.Offset

@Composable
fun EQChart(preset: Preset) {
    val viewModel = com.creativeblisslabs.peqlearn.app.LocalAppViewModel.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { viewModel.isEQInteracting.value = true },
                    onDragEnd = { viewModel.isEQInteracting.value = false },
                    onDrag = { change, _ ->
                        val freq = (change.position.x / size.width) * 20000f
                        val gain = ((size.height / 2 - change.position.y) / (size.height / 2)) * 12f
                        viewModel.onEQPointDragged(freq, gain)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw grid
        val gridColor = onSurfaceColor.copy(alpha = 0.1f)
        for (i in 1..4) {
            val y = (height / 5) * i
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y))
        }

        // Draw frequency response curve (Simplified visualization based on preset)
        val path = Path()
        path.moveTo(0f, centerY)

        for (x in 0..width.toInt() step 5) {
            val freqRatio = x / width
            val currentFreq = freqRatio * 20000f
            var totalGain = 0f
            
            // 1. Parametric Bands (Preset)
            preset.bands.forEach { band ->
                val dist = abs(currentFreq - band.frequency) / band.frequency
                val influence = exp(-(dist * dist) / (0.2f / band.qFactor)).toFloat()
                totalGain += band.gain * influence
            }
            
            // 2. Graphic EQ Bands (Live gains)
            viewModel.eqFrequencies.indices.forEach { i ->
                val bandFreq = viewModel.eqFrequencies[i].replace("k", "000").toFloat()
                val dist = abs(currentFreq - bandFreq) / bandFreq
                val influence = exp(-(dist * dist) / 0.15f).toFloat()
                totalGain += viewModel.eqGains[i] * influence
            }

            val y = centerY - (totalGain * (height / 24f))
            path.lineTo(x.toFloat(), y)
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
            style = Stroke(width = 3.dp.toPx())
        )
        
        // Glow effect
        drawPath(
            path = path,
            color = primaryColor.copy(alpha = 0.2f),
            style = Stroke(width = 8.dp.toPx())
        )
    }
}
