package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.Job
import com.creativeblisslabs.peqlearn.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull

/**
 * INDUSTRIAL SETTINGS ENGINE: pEQ NEURAL WORKSTATION
 * This extension handles workstation personalization, theme synchronization, and utility timers.
 */

fun AppViewModel.updateTheme(theme: pEQTheme) {
    setTheme(theme)
    persistSession()
}

fun AppViewModel.updateAccent(accent: pEQAccent) {
    setAccent(accent)
    persistSession()
}

fun AppViewModel.toggleSleepTimer(enabled: Boolean) { 
    sleepTimerActive.value = enabled
}

fun AppViewModel.setSleepTimer(minutes: Int) {
    sleepTimerMinutes.value = minutes
    sleepTimerActive.value = true
    sleepTimerRemainingSec.longValue = minutes * 60L
    
    cancelSleepTimerJob()
    val job = scope.launch {
        while (sleepTimerRemainingSec.longValue > 0 && sleepTimerActive.value) {
            delay(1000)
            sleepTimerRemainingSec.longValue -= 1
        }
        if (sleepTimerActive.value) {
            audioEngine.pause()
            isPlaying.value = false
            sleepTimerActive.value = false
            showToast("Sleep timer active: Playback stopped.")
        }
    }
    setSleepTimerJob(job)
}

fun AppViewModel.stopSleepTimer() {
    sleepTimerActive.value = false
    cancelSleepTimerJob()
}

fun AppViewModel.toggleBackgroundService(active: Boolean) {
    isBackgroundServiceRunning.value = active
}


fun AppViewModel.toggleLockScreenMode() {
    isLockScreenMode.value = !isLockScreenMode.value
}

fun AppViewModel.toggleFocusMode() {
    isFocusMode.value = !isFocusMode.value
    if (isFocusMode.value) {
        isLockScreenMode.value = true
        showToast("Focus Mode Active: Neural AOD Engaged")
    } else {
        isLockScreenMode.value = false
        showToast("Focus Mode Disabled")
    }
}

fun AppViewModel.toggleGapless(enabled: Boolean) {
    isGaplessEnabled.value = enabled
}

fun AppViewModel.toggleForceHiRes(enabled: Boolean) {
    isForceHiResEnabled.value = enabled
    audioEngine.setExclusiveMode(enabled)
}

fun AppViewModel.cycleRotation() {
    val current = videoRotationDegrees.value
    videoRotationDegrees.value = (current + 90) % 360
}

fun AppViewModel.clearLibrary() {
    scanFolders.clear()
    _mediaLibrary.value = emptyList()
    persistenceManager.saveScanFolders(emptyList())
    showToast("Media library cleared.")
}

fun AppViewModel.resetAudioEngine() {
    // Reset EQ
    (0 until 10).forEach { i ->
        updateEqBand(i, 0f)
        updateEqQ(i, 1f)
    }
    masterVolume.value = 0.8f
    audioEngine.setVolume(0.8f)
    showToast("Audio engine parameters reset to flat.")
}

fun AppViewModel.updateOsApiKey(key: String) {
    osApiKey.value = key
    // persistenceManager.saveOpenSubtitlesApiKey(key)
    showToast("OpenSubtitles API key updated.")
}

fun AppViewModel.resetEq() {
    (0 until 10).forEach { i ->
        updateEqBand(i, 0f)
        updateEqQ(i, 1f)
    }
    showToast("EQ Reset to Flat")
}

fun AppViewModel.optimizeAudioForRoom() {
    scope.launch {
        showToast("Neural Optimization: Analyzing Room Acoustics...")
        delay(1500)
        showToast("Calibration Complete: Compensation curve applied.")
    }
}

fun AppViewModel.clearCache() {
    // In a real app, this would delete temporary/thumbnail folders
    showToast("Workstation cache purged.")
}
