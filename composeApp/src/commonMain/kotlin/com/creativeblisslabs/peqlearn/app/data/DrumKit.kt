package com.creativeblisslabs.peqlearn.app.data

data class DrumKit(
    val id: String,
    val name: String,
    val soundfontPath: String = "",
    val samplePads: Map<Int, String> = emptyMap(),
    val description: String = ""
)
