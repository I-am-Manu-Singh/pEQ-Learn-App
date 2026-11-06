package com.creativeblisslabs.peqlearn.app.video

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.creativeblisslabs.peqlearn.app.pdf.AndroidPdfExporter
import com.creativeblisslabs.peqlearn.app.ui.screens.PdfExportOptions
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/**
 * Resolves the Android application context.
 */
private fun resolveApplicationContext(): Context {
    return runCatching {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val method = activityThreadClass.getMethod("currentApplication")

        method.invoke(null) as? Context
    }.getOrNull() ?: throw IllegalStateException(
        "Unable to obtain Android application context."
    )
}

/**
 * Android implementation of MIDI video export.
 */
@RequiresApi(Build.VERSION_CODES.O)
actual fun exportVideoPlatformSpecific(
    config: VideoExportConfig,
    viewModel: AppViewModel,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            AndroidVideoExporter.exportMidiVideo(
                context = resolveApplicationContext(),
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
                viewModel = viewModel,
                stageCaptureController =
                    viewModel.visualizerStageCaptureController,
                onProgress = { progress ->
                    onProgress(progress)
                },
                onComplete = { savedPath ->
                    onComplete(savedPath)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    }
}
actual fun compilePdfPlatformSpecific(
    config: PdfExportConfig,
    onProgress: (Float) -> Unit,
    onComplete: (String?) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val context = runCatching {
                Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null) as? android.content.Context
            }.getOrNull() ?: throw IllegalStateException("Android Context unavailable for PDF export")

            AndroidPdfExporter.exportScoreToPdf(
                context = context,
                score = config.score,
                options = config.options,
                isLeftHandHidden = config.isLeftHandHidden,
                isRightHandHidden = config.isRightHandHidden,
                isSplitStaff = config.isSplitStaff,
                enabledTrackIndices = config.enabledTrackIndices,
                transposition = config.transposition,
                onProgress = { progress ->
                    onProgress(progress)
                },
                onComplete = { path ->
                    onComplete(path)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    }
}