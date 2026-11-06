package com.creativeblisslabs.peqlearn.app.video

actual fun exportVideoPlatformSpecific(
    config: com.creativeblisslabs.peqlearn.app.video.VideoExportConfig,
    viewModel: com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
}