package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

@Composable
fun StrobeTunerSection(viewModel: AppViewModel) {
    var isListening by remember { mutableStateOf(false) }
    var detectedFreq by remember { mutableStateOf(440.0) }
    var detectedNote by remember { mutableStateOf("A4") }
    var centsOffset by remember { mutableStateOf(0.0) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRECISION STROBE TUNER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { isListening = !isListening }) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle Tuner",
                        tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = detectedNote,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = if (Math.abs(centsOffset) < 3.0) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${(detectedFreq * 10.0).toInt() / 10.0} Hz",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Cent offset slider indicator
            LinearProgressIndicator(
                progress = { ((centsOffset + 50.0) / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (Math.abs(centsOffset) < 3.0) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer
            )
        }
    }
}

// ============================================================================
// 100% RECOVERED PEG CONTROL NODES & ROTARY STROBE PATTERN GENERATOR
// ============================================================================

@Composable
fun PegControlNode(
    stringNote: String,
    isSelected: Boolean,
    isInTune: Boolean,
    tunerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) tunerColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            1.5.dp,
            if (isInTune && isSelected) Color(0xFF4CAF50)
            else if (isSelected) tunerColor
            else Color.White.copy(alpha = 0.15f)
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringNote,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (isSelected) tunerColor else Color.White
            )
            Text(
                text = if (isInTune && isSelected) "LOCKED" else if (isSelected) "ACTIVE" else "READY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInTune && isSelected) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

private fun DrawScope.drawStrobeMark(
    tunerColor: Color,
    strong: Boolean,
    trackWidth: Float
) {
    val markColor = tunerColor.copy(alpha = if (strong) 0.85f else 0.2f)
    val startOffset = Offset(trackWidth / 2f, 3.dp.toPx())
    val endOffset = Offset(trackWidth / 2f, if (strong) 12.dp.toPx() else 8.dp.toPx())
    drawLine(
        color = markColor,
        start = startOffset,
        end = endOffset,
        strokeWidth = if (strong) 2.dp.toPx() else 1.dp.toPx()
    )
}
