package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.components.getSystemVolume
import com.creativeblisslabs.peqlearn.app.ui.components.setSystemVolume
import kotlin.math.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull

/**
 * INDUSTRIAL HARDWARE ENGINE: pEQ NEURAL WORKSTATION
 * This extension manages hardware polling, spectral telemetry, and device calibration.
 */

private data class HardwarePollingState(
    val isPlaying: Boolean,
    val position: Long = 0L,
    val duration: Long = 0L,
    val bitrate: Int = 0,
    val codec: String = "",
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val spectrum: FloatArray = FloatArray(0)
)


fun AppViewModel.setMasterVolume(volume: Float) {
    val safeVolume = volume.coerceIn(0f, 2.0f)
    if (abs(safeVolume - lastVolume) < 0.01f) return
    lastVolume = safeVolume
    masterVolume.value = safeVolume
    audioEngine.setVolume(safeVolume)
    setSystemVolume(safeVolume.coerceAtMost(1.0f))
}

fun AppViewModel.updateDevice(device: Device) {
    selectedOutputDevice.value = device
    audioEngine.setOutputDevice(null) // Mock routing
}

fun AppViewModel.confirmDeviceSwitch() {
    pendingDevice.value?.let { updateDevice(it) }
    showDeviceSwitcher.value = false
    pendingDevice.value = null
}
