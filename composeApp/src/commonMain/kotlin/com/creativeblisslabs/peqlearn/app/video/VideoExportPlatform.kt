package com.creativeblisslabs.peqlearn.app.video

import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.Score


expect fun exportVideoPlatformSpecific(
    config: VideoExportConfig,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
)

expect fun compilePdfPlatformSpecific(
    score: Score,
    options: PdfExportOptions,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
)