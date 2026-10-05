package com.creativeblisslabs.peqlearn.app.security

/**
 * pEQ SECURITY ENGINE: BIOMETRIC & AUDIT HUB
 * Contract for industrial-grade data protection and immutable logging.
 */
interface pEQSecurity {
    suspend fun authenticateBiometrically(): Boolean
    fun logAuditAction(action: String)
    fun archiveProject(path: String): String
}
