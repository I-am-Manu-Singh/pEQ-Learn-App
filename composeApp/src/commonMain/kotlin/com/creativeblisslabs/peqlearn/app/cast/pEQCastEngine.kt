package com.creativeblisslabs.peqlearn.app.cast

import kotlinx.coroutines.flow.StateFlow

/**
 * pEQ CAST ENGINE: PRODUCTION BROADCAST HUB
 * Interface for cross-platform media routing (Google Cast, AirPlay, etc).
 */
interface pEQCastEngine {
    val isCasting: StateFlow<Boolean>
    
    fun castMedia(url: String, title: String)
    fun stopCasting()
}
