package com.creativeblisslabs.peqlearn.app.video

import android.content.Context
import com.creativeblisslabs.peqlearn.app.pdf.AndroidPdfExporter
import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

lateinit var applicationContext: Context
actual fun exportVideoPlatformSpecific(
    config: VideoExportConfig,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            AndroidVideoExporter.exportMidiVideo(
                context = applicationContext,
                score = config.score,
                resolution = config.resolution,
                fps = config.fps,
                effects = config.effects,
                layoutMode = config.layoutMode,
                notationFormat = config.notationFormat,
                visualTheme = config.visualTheme,
                bitrateMbps = config.bitrateMbps,
                sampleRateHz = config.sampleRateHz,
                cameraYawDeg = config.cameraYawDeg,
                cameraPitchDeg = config.cameraPitchDeg,
                eqBarsCount = config.eqBarsCount,
                peakSensitivity = config.peakSensitivity,
                rightHandColorInt = config.rightHandColorInt,
                leftHandColorInt = config.leftHandColorInt,
                isRightHandHidden = config.isRightHandHidden,
                isLeftHandHidden = config.isLeftHandHidden,
                enabledTrackIndices = config.enabledTrackIndices,
                externalAudioUri = config.externalAudioUri,
                onProgress = onProgress,
                onComplete = { filePath ->
                    onComplete(filePath)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    }
}

actual fun compilePdfPlatformSpecific(
    score: Score,
    options: PdfExportOptions,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            AndroidPdfExporter.exportScoreToPdf(
                context = applicationContext,
                score = score,
                options = options,
                onProgress = onProgress,
                onComplete = onComplete
            )
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    }
}