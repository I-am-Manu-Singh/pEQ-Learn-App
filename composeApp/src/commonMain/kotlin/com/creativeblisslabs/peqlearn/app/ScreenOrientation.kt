package com.creativeblisslabs.peqlearn.app

enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO
}

data class ScreenOrientationState(
    val currentOrientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
    val isLocked: Boolean = false
)
