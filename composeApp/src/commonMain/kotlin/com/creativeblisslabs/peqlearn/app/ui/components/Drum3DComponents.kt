package com.creativeblisslabs.peqlearn.app.ui.components

data class FingeringData(
    val notes: List<Int>? = null,
    val fingers: List<Int>? = null,
    val stringNumber: Int = 0,
    val fretNumber: Int = 0,
    val fingerAssigned: Int = 0,
    val noteName: String = ""
)
