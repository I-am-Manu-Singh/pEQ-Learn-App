package com.creativeblisslabs.peqlearn.app.data

/**
 * pEQ IDENTITY HUB: PERSONALIZATION & DIAGNOSTICS
 * Interface for user profiles, secure error reporting, and air-gap management.
 */
interface pEQIdentityHub {
    fun setOfflineMode(enabled: Boolean)
    fun reportError(message: String, severity: String)
    fun getCurrentProfile(): UserProfile
    fun saveProfile(profile: UserProfile)
}

data class UserProfile(
    val name: String,
    val role: String,
    val experienceLevel: String
)
