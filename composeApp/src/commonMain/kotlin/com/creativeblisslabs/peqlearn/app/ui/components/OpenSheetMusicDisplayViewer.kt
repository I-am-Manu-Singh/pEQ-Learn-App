package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.creativeblisslabs.peqlearn.app.ui.screens.Score

@Composable
expect fun OpenSheetMusicDisplayViewer(
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    isDarkMode: Boolean = true,
    zoom: Float = 0.85f,
    modifier: Modifier = Modifier
)
