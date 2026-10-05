package com.creativeblisslabs.peqlearn.app.data

import androidx.compose.ui.graphics.Color

/**
 * CINEMATIC SUBTITLE CONFIGURATION
 * Defines the physical appearance of the subtitle overlay.
 */
data class SubtitleSettings(
    val fontSize: Float = 18f,
    val textColor: Color = Color.White,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    val isBold: Boolean = true,
    val edgeType: SubtitleEdgeType = SubtitleEdgeType.OUTLINE,
    val verticalOffset: Int = 0, // Relative position shift
    val delayMs: Long = 0
)

enum class SubtitleEdgeType {
    NONE, OUTLINE, DROP_SHADOW
}
