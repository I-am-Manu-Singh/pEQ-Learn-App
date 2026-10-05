package com.creativeblisslabs.peqlearn.app.data

data class Thaat(
    val name: String,
    val semitones: List<Int>,
    val description: String
) {
    val swaras: List<String>
        get() = description.substringBefore("—").trim().split(" ")
}
