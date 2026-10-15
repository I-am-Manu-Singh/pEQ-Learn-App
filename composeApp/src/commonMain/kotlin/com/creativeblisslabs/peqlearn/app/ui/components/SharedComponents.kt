package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * GLOBAL UI TOKENS & SHARED WIDGETS: pEQ NEURAL WORKSTATION
 * This module contains universal design tokens, glass-morphic containers, and shared primitives.
 */

object pEQDesign {
    val GlassColor
        @Composable
        get() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
        }

    val GlassBorder
        @Composable
        get() = BorderStroke(
            0.5.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    Color.Transparent,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            )
        )

    val PrimaryGlow
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    val GlassElevation
        @Composable
        get() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) 0.dp else 4.dp

    val GlassTonalElevation
        @Composable
        get() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) 0.dp else 2.dp

    val CardShape @Composable get() = MaterialTheme.shapes.medium
}

@Composable
fun pEQEditorContainer(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun InfoChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun MetadataBadge(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
fun pEQGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = pEQDesign.GlassColor,
        shape = pEQDesign.CardShape,
        border = pEQDesign.GlassBorder,
        tonalElevation = pEQDesign.GlassTonalElevation,
        shadowElevation = pEQDesign.GlassElevation
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun pEQFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        leadingIcon = leadingIcon,
        shape = shape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    )
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}


@Composable
expect fun pEQBrowser(
    url: String,
    onClose: () -> Unit,
    onStreamCaptured: (String) -> Unit = {}
)
