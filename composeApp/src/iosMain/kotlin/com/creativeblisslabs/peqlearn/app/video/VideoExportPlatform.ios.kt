package com.creativeblisslabs.peqlearn.app.video

import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

actual fun exportVideoPlatformSpecific(
    config: VideoExportConfig,
    viewModel: AppViewModel,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
}