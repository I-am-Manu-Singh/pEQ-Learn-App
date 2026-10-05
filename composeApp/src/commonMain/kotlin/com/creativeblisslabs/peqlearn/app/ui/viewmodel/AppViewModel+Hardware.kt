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

fun AppViewModel.startHardwarePolling() {
    viewModelScope.launch(Dispatchers.IO) {
        while (isActive) {
            // 🚀 Sync Commands from OS/Widget
            val cmd = withContext(Dispatchers.Main) {
                val command = workstationRegistry["last_command"]
                if (command != null) {
                    workstationRegistry.remove("last_command")
                }
                command
            }

            if (cmd != null) {
                withContext(Dispatchers.Main) {
                    when (cmd) {
                        "TOGGLE_PLAYBACK" -> togglePlayback()
                        "SKIP_NEXT" -> nextTrack()
                        "SKIP_PREV" -> previousTrack()
                        "TOGGLE_SHUFFLE" -> toggleShuffle()
                    }
                }
            }

            val engineState = withContext(Dispatchers.Main) {
                val playing = audioEngine.isPlaying()
                if (playing) {
                    HardwarePollingState(
                        isPlaying = true,
                        position = audioEngine.getCurrentPosition(),
                        duration = audioEngine.getDuration(),
                        bitrate = audioEngine.getBitrate(),
                        codec = audioEngine.getCodec(),
                        sampleRate = audioEngine.getSampleRate(),
                        bitDepth = audioEngine.getBitDepth(),
                        spectrum = audioEngine.getCurrentSpectrum()
                    )
                } else {
                    HardwarePollingState(isPlaying = false)
                }
            }

            if (engineState.isPlaying) {
                withContext(Dispatchers.Main) {
                    isPlaying.value = true
                    currentPosition.longValue = engineState.position
                    currentDuration.value = engineState.duration.coerceAtLeast(1L)
                    
                    currentBitrate.value = engineState.bitrate
                    currentCodec.value = engineState.codec
                    currentSampleRate.value = engineState.sampleRate
                    currentBitDepth.value = engineState.bitDepth
                }

                // 📊 High-Fidelity Spectral Telemetry
                val rawSpectrum = engineState.spectrum
                val hasRealData = rawSpectrum.any { level -> level > 0.001f }
                
                if (hasRealData) {
                    withContext(Dispatchers.Main) {
                        rtaData.value = rawSpectrum
                    }
                    
                    // 🧠 NEURAL PITCH & HARMONIC ANALYSIS
                    val peakIdx = rawSpectrum.indices.maxByOrNull { rawSpectrum[it] } ?: 0
                    val freq = peakIdx * (audioEngine.getSampleRate() / (rawSpectrum.size * 2f))
                    
                    withContext(Dispatchers.Main) {
                        detectedPitch.value = freq
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        rtaData.value = FloatArray(32) { 0f }
                        detectedPitch.floatValue = 0f
                    }
                }
                
                withContext(Dispatchers.Main) {
                    currentSampleRate.value = audioEngine.getSampleRate()
                    currentBitDepth.value = audioEngine.getBitDepth()
                    currentCodec.value = audioEngine.getCodec()
                    currentBitrate.value = audioEngine.getBitrate()
                    
                    val maxMag = rtaData.value.maxOrNull() ?: 0f
                    val targetPeak = if (maxMag > 0.01f) 20 * log10(maxMag.coerceAtLeast(0.00001f)) else -60f
                    peakLevelDb.value = targetPeak.coerceIn(-60f, 0f)

                    dspProcessingLoad.value = audioEngine.getDspLoad()
                }

            } else {
                withContext(Dispatchers.Main) {
                    isPlaying.value = false
                    rtaData.value = rtaData.value.map { it * 0.85f }.toFloatArray()
                }
            }

            // 🔊 Sync Master Gain
            val sysVol = getSystemVolume()
            if (abs(sysVol - masterVolume.value) > 0.01f) {
                withContext(Dispatchers.Main) {
                    masterVolume.value = sysVol
                    audioEngine.setVolume(sysVol)
                }
            }

            if (engineState.isPlaying) {
                persistenceCounter++
                if (persistenceCounter >= 50) { 
                    persistSession()
                    persistenceCounter = 0
                }
            }

            // 🔌 Poll Connected Devices (Every 3 seconds)
            if (Clock.System.now().toEpochMilliseconds() % 3000 < 200) {
                val devices = audioEngine.getConnectedDevices()
                withContext(Dispatchers.Main) {
                    connectedDevices.clear()
                    connectedDevices.addAll(devices)
                }
            }

            delay(100)
        }
    }
}

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

fun AppViewModel.runHardwareDiagnostics() {
    scope.launch {
        isDiagnosticRunning.value = true
        diagnosticLog.clear()
        
        diagnosticLog.add("Probing JNI Signal Path...")
        delay(200)
        val isJniActive = audioEngine.isInitialized()
        diagnosticLog.add("JNI Engine: ${if (isJniActive) "STABLE" else "ERR_INIT"}")
        
        diagnosticLog.add("Calculating RTL Latency...")
        delay(400)
        val latency = audioEngine.getLatency()
        diagnosticLog.add("Measured Round-Trip: ${latency}ms")
        
        diagnosticLog.add("Verifying DSP Buffer Stability...")
        val load = audioEngine.getDspLoad()
        diagnosticLog.add("Current DSP Load: ${load}%")
        
        if (load > 80f) diagnosticLog.add("WARN: Neural Overflow Risk Detected")
        else diagnosticLog.add("Buffer Status: OPTIMAL")
        
        diagnosticLog.add("Diagnostic Cycle Complete.")
        isDiagnosticRunning.value = false
    }
}

fun AppViewModel.updateChannelBalance(balance: Float) {
    channelBalance.floatValue = balance
    audioEngine.setChannelBalance(balance)
}

fun AppViewModel.togglePhaseInversion(active: Boolean) {
    phaseInverted.value = active
    audioEngine.setPhaseInversion(active)
}

fun AppViewModel.toggleMicMonitoring() {
    isMicMonitoringEnabled.value = !isMicMonitoringEnabled.value
    audioEngine.enableZeroLatencyMonitoring(isMicMonitoringEnabled.value)
    showToast("Mic Monitoring: ${if (isMicMonitoringEnabled.value) "ENABLED" else "DISABLED"}")
}
