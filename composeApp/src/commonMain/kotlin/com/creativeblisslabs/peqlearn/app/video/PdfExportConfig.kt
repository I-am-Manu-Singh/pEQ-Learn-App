package com.creativeblisslabs.peqlearn.app.video

import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.Score

data class PdfExportConfig(
    val score: Score,
    val options: PdfExportOptions,
    val isLeftHandHidden: Boolean,
    val isRightHandHidden: Boolean,
    val isSplitStaff: Boolean,
    val enabledTrackIndices: Map<Int, Boolean>,
    val transposition: Int
)