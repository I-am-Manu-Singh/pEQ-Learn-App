package com.creativeblisslabs.peqlearn.app.audio

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Intelligent Scanner for system-level Bluetooth codec support.
 * Negotiates between host capabilities and hardware profiles.
 */
interface CodecScanner {
    fun getSupportedCodecs(): List<BluetoothProfile>
    fun isCodecActive(profile: BluetoothProfile): Boolean
}

data class CodecMatchResult(
    val activeCodec: BluetoothProfile,
    val isOptimal: Boolean,
    val fallbackReason: String? = null
)

val LocalCodecScanner = staticCompositionLocalOf<CodecScanner?> { null }
