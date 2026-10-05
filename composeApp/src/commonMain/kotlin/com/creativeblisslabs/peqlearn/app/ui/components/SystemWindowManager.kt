package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.runtime.Composable


expect fun setScreenOrientation(orientation: Int)
expect fun setSystemBarsVisibility(visible: Boolean)
expect fun setBrightness(brightness: Float)
expect fun setSystemVolume(volume: Float)
expect fun getSystemVolume(): Float

object pEQOrientation {
    const val UNSPECIFIED = -1
    const val LANDSCAPE = 0
    const val PORTRAIT = 1
    const val USER = 2
    const val LOCKED = 3
}
