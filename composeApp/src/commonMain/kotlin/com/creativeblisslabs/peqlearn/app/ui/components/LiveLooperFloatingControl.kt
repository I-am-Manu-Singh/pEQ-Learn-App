package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.midi.LiveLooper
import kotlinx.datetime.Clock
import androidx.compose.foundation.BorderStroke

/**
 * LIVE LOOPER FLOATING CONTROL HUD
 * Modeled on midee's single-button looper control & overdub HUD.
 * 
 * Provides:
 * - 1-tap state toggling: Idle -> Armed -> Recording -> Playing -> Overdubbing
 * - Layer count badge
 * - Undo layer button
 * - Clear looper button
 */
@Composable
fun LiveLooperFloatingControl(
    looper: LiveLooper,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var looperState by remember { mutableStateOf(looper.state) }
    var layerCount by remember { mutableIntStateOf(looper.layerCount) }

    fun updateState() {
        looperState = looper.state
        layerCount = looper.layerCount
    }

    val statusColor = when (looperState) {
        LiveLooper.State.RECORDING -> Color(0xFFFF1744) // Bright red
        LiveLooper.State.OVERDUBBING -> Color(0xFFFF9100) // Amber / orange
        LiveLooper.State.PLAYING -> Color(0xFF00E676) // Bright green
        LiveLooper.State.ARMED -> Color(0xFFFFD600) // Yellow armed
        LiveLooper.State.IDLE -> Color.White.copy(alpha = 0.5f)
    }

    val stateText = when (looperState) {
        LiveLooper.State.IDLE -> "LOOP"
        LiveLooper.State.ARMED -> "ARMED"
        LiveLooper.State.RECORDING -> "REC"
        LiveLooper.State.PLAYING -> "PLAY"
        LiveLooper.State.OVERDUBBING -> "DUB"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF14171E).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Action Button (Record / Play / Overdub Toggle)
            Button(
                onClick = {
                    looper.toggle(Clock.System.now().toEpochMilliseconds())
                    updateState()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = statusColor.copy(alpha = 0.2f),
                    contentColor = statusColor
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stateText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Layer Count Badge
            if (layerCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "L$layerCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Undo Button
                IconButton(
                    onClick = {
                        looper.undo()
                        updateState()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo Layer",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear Button
                IconButton(
                    onClick = {
                        looper.clear()
                        updateState()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear Loop",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

