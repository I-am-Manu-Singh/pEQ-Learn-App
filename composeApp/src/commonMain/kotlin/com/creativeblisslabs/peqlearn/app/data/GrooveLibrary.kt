package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class GrooveLibrary(
    val version: Int = 1,
    val genres: List<String> = emptyList(),
    val patterns: List<GroovePattern> = emptyList()
)
