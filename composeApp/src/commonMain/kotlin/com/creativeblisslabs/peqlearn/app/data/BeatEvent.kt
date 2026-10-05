package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class BeatEvent(
    val step: Int,
    val padIndex: Int,
    val velocity: Float = 0.8f,
    val isAccent: Boolean = false
)
