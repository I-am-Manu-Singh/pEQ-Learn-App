package com.creativeblisslabs.peqlearn.app.data

// SplitJob is defined in Models.kt

data class StreamFormats(
    val format: String,
    val bitrateKbps: Int,
    val channels: Int = 2
)
