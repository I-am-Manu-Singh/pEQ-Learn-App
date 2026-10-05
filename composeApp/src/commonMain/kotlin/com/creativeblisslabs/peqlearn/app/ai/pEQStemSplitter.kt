package com.creativeblisslabs.peqlearn.app.ai

import kotlinx.coroutines.flow.StateFlow

interface pEQStemSplitter {
    val isProcessing: StateFlow<Boolean>
    val progress: StateFlow<Float>
    suspend fun splitTrack(inputPath: String, outputDir: String): Map<String, String>
    suspend fun splitBatch(inputPaths: List<String>, outputDir: String): Map<String, Map<String, String>>
    suspend fun classifyGenre(inputPath: String): String
    suspend fun upscaleAudio(inputPath: String): String
}
