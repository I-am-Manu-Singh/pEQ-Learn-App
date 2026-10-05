package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class PartComposition(
    val partName: String,
    val patternId: String,
    val repeatCount: Int = 1,
    val muteChannels: List<Int> = emptyList()
)
