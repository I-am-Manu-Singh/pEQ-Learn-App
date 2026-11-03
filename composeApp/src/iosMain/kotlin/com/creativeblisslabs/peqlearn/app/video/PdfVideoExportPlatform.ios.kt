package com.creativeblisslabs.peqlearn.app.video

import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.Score

actual fun exportVideoPlatformSpecific(

    config: com.creativeblisslabs.peqlearn.app.video.VideoExportConfig,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
}

actual fun compilePdfPlatformSpecific(
    score: Score,
    options: PdfExportOptions,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
}