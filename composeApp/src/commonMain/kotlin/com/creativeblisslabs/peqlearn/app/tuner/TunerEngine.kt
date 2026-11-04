package com.creativeblisslabs.peqlearn.app.tuner

/**
 * TUNER AUDIO RECORDER & PITCH DETECTOR ENGINE
 * Multiplatform interface for real-time microphone capture and pitch estimation.
 */
interface TunerEngine {
    fun startListening(onPitchDetected: (pitchHz: Float, noteName: String, centsDeviation: Int, amplitude: Float) -> Unit)
    fun stopListening()
    fun isListening(): Boolean
}

expect fun getTunerEngine(): TunerEngine

