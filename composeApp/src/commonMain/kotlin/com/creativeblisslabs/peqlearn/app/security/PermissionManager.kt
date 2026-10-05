package com.creativeblisslabs.peqlearn.app.security

interface PermissionManager {
    fun requestMediaPermissions()
    fun hasMediaPermissions(): Boolean
    fun requestMicrophonePermission()
    fun hasMicrophonePermission(): Boolean
    fun requestNotificationPermission()
    fun hasNotificationPermission(): Boolean
    fun getContext(): Any
}

expect fun getPermissionManager(): PermissionManager
