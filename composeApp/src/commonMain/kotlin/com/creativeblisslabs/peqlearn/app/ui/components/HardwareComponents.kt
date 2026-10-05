package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * INDUSTRIAL HARDWARE ENGINE: pEQ NEURAL WORKSTATION
 * This module manages high-fidelity hardware telemetry, DSP cards, and system status monitors.
 */

@Composable
fun HardwareSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
fun DSPSmallCard(
    label: String,
    value: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)),
        tonalElevation = if (active) 2.dp else 0.dp,
        shadowElevation = if (active) 1.dp else 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
            Text(text = label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        Text(text = label, fontSize = 10.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)), tonalElevation = 2.dp, shadowElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(3.dp))
            Text(text = label, fontSize = 10.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.92f))
        }
    }
}
