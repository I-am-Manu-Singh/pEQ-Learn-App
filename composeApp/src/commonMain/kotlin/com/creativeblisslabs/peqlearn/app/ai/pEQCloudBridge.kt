package com.creativeblisslabs.peqlearn.app.ai

/**
 * pEQ CLOUD BRIDGE: PRODUCTION INFRASTRUCTURE
 * Interface for secure cloud archival, preset syncing, and industrial analytics.
 */
interface pEQCloudBridge {
    suspend fun syncPresets(): Boolean
    suspend fun mergeCloudLibrary(provider: String): List<String>
    fun logAnalyticsEvent(event: String, data: Map<String, String>)
}
