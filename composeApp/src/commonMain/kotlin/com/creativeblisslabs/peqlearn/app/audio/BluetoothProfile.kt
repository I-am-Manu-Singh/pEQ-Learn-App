package com.creativeblisslabs.peqlearn.app.audio

/**
 * Universal Bluetooth Codec Profiles for High-Fidelity transmission.
 * Grounded with physical performance metrics (Bitrate, Latency, Hi-Res status).
 */
data class BluetoothProfile(
    val name: String, 
    val maxBitrate: Int, 
    val isHiRes: Boolean,
    val latency: String = "Standard"
) {
    val bitRateDisplay: String get() = "$maxBitrate kbps"

    companion object {
        val LDAC = BluetoothProfile("Sony LDAC", 990, true, "Moderate")
        val LHDC = BluetoothProfile("LHDC 5.0", 900, true, "Low")
        val APTX_HD = BluetoothProfile("Qualcomm aptX HD", 576, true, "Low")
        val AAC = BluetoothProfile("Apple AAC", 256, false, "Standard")
        val SBC = BluetoothProfile("SBC (Universal)", 328, false, "Standard")
    }
}
