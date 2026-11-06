package com.creativeblisslabs.peqlearn.app.video

import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

expect fun exportVideoPlatformSpecific(
    config: VideoExportConfig,
    viewModel: AppViewModel,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
)

expect fun compilePdfPlatformSpecific(
    config: PdfExportConfig,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
)