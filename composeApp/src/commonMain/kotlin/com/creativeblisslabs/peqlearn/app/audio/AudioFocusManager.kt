package com.creativeblisslabs.peqlearn.app.audio

/**
 * High-precision manager for system audio interruptions.
 * Ensures pEQ Media respects OS-level priority (calls, notifications).
 */
interface AudioFocusManager {
    fun requestFocus(): Boolean
    fun abandonFocus()
    fun setOnFocusChangeListener(listener: (FocusState) -> Unit)
}

enum class FocusState {
    GAINED, 
    LOST, 
    LOST_TRANSIENT, // Pause temporarily (e.g., call)
    LOST_TRANSIENT_CAN_DUCK // Lower volume (e.g., notification)
}
