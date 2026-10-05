package com.creativeblisslabs.peqlearn.app.data.repository

interface PatternLoader {
    suspend fun loadGrooveJson(): String
}
