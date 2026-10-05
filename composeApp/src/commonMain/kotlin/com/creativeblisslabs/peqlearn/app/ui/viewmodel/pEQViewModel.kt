package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class pEQViewModel {
    private val viewModelJob = SupervisorJob()
    val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.Main.immediate)

    protected open fun onCleared() {
        viewModelJob.cancel()
    }

    fun clear() {
        onCleared()
    }
}
