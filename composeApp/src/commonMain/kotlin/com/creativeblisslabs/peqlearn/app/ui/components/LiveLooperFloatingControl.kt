package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.peqlearn.app.midi.LiveLooper
import kotlinx.datetime.Clock

@Composable
fun LiveLooperFloatingControl(
    looper: LiveLooper,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(looper.state) }
    var layers by remember { mutableIntStateOf(looper.layerCount) }

    fun refresh() {
        state = looper.state
        layers = looper.layerCount
    }

    val tint = when (state) {
        LiveLooper.State.IDLE -> Color.White.copy(alpha = 0.65f)
        LiveLooper.State.ARMED -> Color(0xFFFFD600)
        LiveLooper.State.RECORDING -> Color(0xFFFF1744)
        LiveLooper.State.PLAYING -> Color(0xFF00E676)
        LiveLooper.State.OVERDUBBING -> Color(0xFFFF9100)
    }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = { looper.toggle(Clock.System.now().toEpochMilliseconds()); refresh() },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = when (state) {
                    LiveLooper.State.IDLE -> Icons.Default.FiberManualRecord
                    LiveLooper.State.ARMED -> Icons.Default.RadioButtonChecked
                    LiveLooper.State.RECORDING -> Icons.Default.Stop
                    LiveLooper.State.PLAYING -> Icons.Default.PlayArrow
                    LiveLooper.State.OVERDUBBING -> Icons.Default.Layers
                },
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }

        if (layers > 0) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Layers, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Text("$layers", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.offset(x = 8.dp, y = (-7).dp))
            }
            IconButton(onClick = { looper.undo(); refresh() }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Undo, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = { looper.clear(); refresh() }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
            }
        }
    }
}