package com.creativeblisslabs.peqlearn.app.video

import com.creativeblisslabs.peqlearn.app.ui.screens.Score

data class VideoExportConfig(
    val score: Score,
    val resolution: String,
    val fps: Int,
    val effects: Map<String, Boolean>,
    val layoutMode: String,
    val notationFormat: String,
    val visualTheme: String,
    val bitrateMbps: Int,
    val sampleRateHz: Int,
    val cameraYawDeg: Float,
    val cameraPitchDeg: Float,
    val eqBarsCount: Int,
    val peakSensitivity: Float,
    val rightHandColorInt: Int,
    val leftHandColorInt: Int,
    val isRightHandHidden: Boolean,
    val isLeftHandHidden: Boolean,
    val enabledTrackIndices: Map<Int, Boolean>,
    val externalAudioUri: String?
)