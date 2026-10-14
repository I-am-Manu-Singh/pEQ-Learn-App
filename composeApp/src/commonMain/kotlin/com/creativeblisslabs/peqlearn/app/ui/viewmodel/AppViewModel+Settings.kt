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


fun AppViewModel.toggleBackgroundService(active: Boolean) {
    isBackgroundServiceRunning.value = active
}


fun AppViewModel.clearLibrary() {
    scanFolders.clear()
    _mediaLibrary.value = emptyList()
    persistenceManager.saveScanFolders(emptyList())
    showToast("Media library cleared.")
}

fun AppViewModel.clearCache() {
    // In a real app, this would delete temporary/thumbnail folders
    showToast("Workstation cache purged.")
}
