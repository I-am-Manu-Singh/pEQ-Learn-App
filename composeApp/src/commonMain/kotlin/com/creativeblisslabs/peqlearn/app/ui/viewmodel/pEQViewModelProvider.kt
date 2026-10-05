package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * WORKSTATION PROVIDER: pEQ NEURAL REGISTRY
 * A global bridge to share the active AppViewModel across Service/Activity boundaries.
 */
object pEQViewModelProvider {
    var viewModel: AppViewModel? by mutableStateOf(null)
}
