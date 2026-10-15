package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull
fun AppViewModel.showToast(message: String) {
    toastMessage.value = message
}