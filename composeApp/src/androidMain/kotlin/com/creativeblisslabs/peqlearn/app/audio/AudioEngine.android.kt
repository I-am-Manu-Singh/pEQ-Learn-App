package com.creativeblisslabs.peqlearn.app.audio

import com.creativeblisslabs.peqlearn.app.pEQApplication
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.pEQViewModelProvider

private var bootstrapEngine: AndroidAudioEngine? = null
private val lock = Any()

actual fun getAudioEngine(): AudioEngine {
    val vm = pEQViewModelProvider.viewModel
    if (vm != null) {
        val engine = vm.audioEngine
        if (engine != null) return engine
    }
    val existing = bootstrapEngine
    if (existing != null) return existing
    synchronized(lock) {
        var engine = bootstrapEngine
        if (engine == null) {
            engine = AndroidAudioEngine(pEQApplication.instance)
            engine.initialize(0)
            bootstrapEngine = engine
        }
        return engine
    }
}
