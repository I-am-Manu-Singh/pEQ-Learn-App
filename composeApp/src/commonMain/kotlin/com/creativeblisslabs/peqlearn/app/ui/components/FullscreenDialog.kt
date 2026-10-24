package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * Platform-aware fullscreen dialog that eliminates system bar letterboxing,
 * cutout padding, and left/right status bar margins.
 */
@Composable
expect fun FullscreenDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
    ),
    content: @Composable () -> Unit
)
