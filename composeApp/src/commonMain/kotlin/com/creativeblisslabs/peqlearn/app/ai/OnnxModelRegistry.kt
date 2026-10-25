package com.creativeblisslabs.peqlearn.app.ai

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class OnnxModelDefinition(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val description: String,
    val purpose: String,
    val isCustom: Boolean = false,
    val localPath: String = "" // For custom imported models
)

/**
 * Central registry for all ONNX models used across the app.
 * Handles downloading, importing custom models, persistence, RAM caching,
 * model switching, and progress tracking.
 */
object OnnxModelRegistry {

    // ── Built-in Model Definitions ──────────────────────────────────────────

    val MUSCRIPTOR_SMALL_COND = OnnxModelDefinition(
        id = "muscriptor_small_cond",
        name = "MuScriptor Conditioner",
        fileName = "muscriptor-small-cond.onnx",
        downloadUrl = "https://huggingface.co/unifolia/muscriptor-small-onnx/resolve/main/muscriptor-small-cond.onnx",
        sizeBytes = 6_000_000L, // ~6 MB conditioner
        description = "Audio conditioner: converts 5-second 16kHz audio chunks into prefix embeddings (f32 [1, 501+1+K, 768]).",
        purpose = "Audio Conditioning (Spectrogram → Prefix Embeddings)"
    )

    val MUSCRIPTOR_SMALL = OnnxModelDefinition(
        id = "muscriptor_small",
        name = "MuScriptor Small AMT",
        fileName = "muscriptor-small-lm.onnx",
        downloadUrl = "https://huggingface.co/unifolia/muscriptor-small-onnx/resolve/main/muscriptor-small-lm.onnx",
        sizeBytes = 405_211_928L, // ~386 MB
        description = "Decoder-only transformer (103M params). Greedy autoregressive decoding with 1393 MT3 token vocabulary. Converts prefix embeddings to MIDI note events.",
        purpose = "Audio-to-MIDI & Sheet Music Transcription"
    )

    val SPLEETER_VOCALS = OnnxModelDefinition(
        id = "spleeter_vocals",
        name = "Spleeter Vocals Extractor",
        fileName = "spleeter_vocals.onnx",
        downloadUrl = "https://github.com/deezer/spleeter/releases/download/v1.4.0/2stems.tar.gz",
        sizeBytes = 19_681_017L,
        description = "Deep convolutional model isolating vocal tracks with high fidelity.",
        purpose = "Stem Separation (Vocals)"
    )

    val SPLEETER_ACCOMPANIMENT = OnnxModelDefinition(
        id = "spleeter_accompaniment",
        name = "Spleeter Accompaniment Engine",
        fileName = "spleeter_accompaniment.onnx",
        downloadUrl = "https://github.com/deezer/spleeter/releases/download/v1.4.0/2stems.tar.gz",
        sizeBytes = 19_681_024L,
        description = "Complementary model isolating accompaniment, backing instruments, and chords.",
        purpose = "Stem Separation (Backing Instruments)"
    )

    val HTDEMUCS_QUANTIZED_6s = OnnxModelDefinition(
        id = "htdemucs_quantized_6s",
        name = "HTDemucs quantized_6s",
        fileName = "htdemucs_quantized_6s.onnx",
        downloadUrl = "https://huggingface.co/StemSplitio/htdemucs-6s-onnx/blob/main/htdemucs_6s.onnx",
        sizeBytes = 82_450_000L,
        description = "State-of-the-art hybrid transformer separating Drums, Bass, Vocals, and Other stems.",
        purpose = "Stem Separation (4-Stem Studio Pro)"
    )
  val HTDEMUCS_QUANTIZED_fp16 = OnnxModelDefinition(
        id = "htdemucs_quantized_fp16",
        name = "HTDemucs fp16_6s",
        fileName = "htdemucs_quantized_fp16.onnx",
        downloadUrl = "https://huggingface.co/StemSplitio/htdemucs-6s-onnx/blob/main/htdemucs_6s_fp16weights.onnx",
     sizeBytes = 82_450_000L,
        description = "State-of-the-art hybrid transformer separating Drums, Bass, Vocals, and Other stems.",
        purpose = "Stem Separation (4-Stem Studio Pro)"
    )
    
        val builtInModels = listOf(
        MUSCRIPTOR_SMALL_COND,
        MUSCRIPTOR_SMALL,
        SPLEETER_VOCALS,
        SPLEETER_ACCOMPANIMENT,
        HTDEMUCS_QUANTIZED_6s,
        HTDEMUCS_QUANTIZED_fp16
    )

    // ── State Maps (Observable by Compose UI) ───────────────────────────────

    /** Download progress per model: modelId → 0.0f..1.0f */
    val downloadProgressMap = mutableStateMapOf<String, Float>()

    /** Whether a download is currently active */
    val isDownloadingMap = mutableStateMapOf<String, Boolean>()

    /** Human-readable status string per model */
    val modelStatusMap = mutableStateMapOf<String, String>()

    /** Whether model is warm in RAM */
    val inRamLoadedSessions = mutableStateMapOf<String, Boolean>()

    /** In-RAM byte buffer cache (for fast OrtSession creation) */
    val inRamModelBuffers = mutableMapOf<String, ByteArray>()

    /** Currently selected/active model for transcription */
    val activeTranscriptionModelId = mutableStateMapOf<String, String>() // purpose → modelId

    /** Custom user-imported model definitions (persisted to SharedPrefs) */
    val customModels = mutableStateMapOf<String, OnnxModelDefinition>()

    /** Loading progress for model initialization (RAM warm-up) */
    val loadingProgressMap = mutableStateMapOf<String, Float>()

    /** Whether a model is currently being loaded into RAM */
    val isLoadingMap = mutableStateMapOf<String, Boolean>()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    // ── Combined Model List ─────────────────────────────────────────────────

    /** All available models: built-in + user-imported */
    val allModels: List<OnnxModelDefinition>
        get() = builtInModels + customModels.values.toList()

    // ── File Path Helpers ───────────────────────────────────────────────────

    fun getModelFile(modelsDir: File, definition: OnnxModelDefinition): File {
        return if (definition.isCustom && definition.localPath.isNotEmpty()) {
            File(definition.localPath)
        } else {
            File(modelsDir, definition.fileName)
        }
    }

    /**
     * Check if a model is available on-disk (or in assets) and ready to use.
     */
    fun isModelReady(modelsDir: File, definition: OnnxModelDefinition): Boolean {
        // Check RAM cache first
        if (inRamLoadedSessions[definition.id] == true && inRamModelBuffers.containsKey(definition.id)) {
            return true
        }
        // Check custom model path
        if (definition.isCustom && definition.localPath.isNotEmpty()) {
            val f = File(definition.localPath)
            return f.exists() && f.length() > 1000L
        }
        // Check models directory
        val targetFile = File(modelsDir, definition.fileName)
        return targetFile.exists() && targetFile.length() > 1000L
    }

    // ── Model Loading (Disk → RAM) ──────────────────────────────────────────

    /**
     * Load a model file from disk into the in-RAM byte buffer cache.
     * Returns the byte array, or null on failure.
     * Tracks loading progress via [loadingProgressMap] and [isLoadingMap].
     */
    suspend fun loadModelIntoRam(
        modelsDir: File,
        definition: OnnxModelDefinition,
        onProgress: (Float) -> Unit = {}
    ): ByteArray? = withContext(Dispatchers.IO) {
        val modelFile = getModelFile(modelsDir, definition)
        if (!modelFile.exists() || modelFile.length() < 1000L) {
            modelStatusMap[definition.id] = "File Not Found"
            return@withContext null
        }

        isLoadingMap[definition.id] = true
        loadingProgressMap[definition.id] = 0f
        modelStatusMap[definition.id] = "Loading into RAM..."

        try {
            val fileLength = modelFile.length()
            val buffer = ByteArray(fileLength.toInt())
            var totalRead = 0L

            modelFile.inputStream().buffered(65536).use { input ->
                val chunk = ByteArray(65536)
                var bytesRead: Int
                while (input.read(chunk).also { bytesRead = it } != -1) {
                    System.arraycopy(chunk, 0, buffer, totalRead.toInt(), bytesRead)
                    totalRead += bytesRead
                    val progress = (totalRead.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                    loadingProgressMap[definition.id] = progress
                    onProgress(progress)
                }
            }

            inRamModelBuffers[definition.id] = buffer
            inRamLoadedSessions[definition.id] = true
            loadingProgressMap[definition.id] = 1f
            modelStatusMap[definition.id] = "Cached in RAM & Disk (${formatFileSize(fileLength)})"
            isLoadingMap[definition.id] = false

            println("pEQLearn: [ONNX] Loaded ${definition.name} into RAM: ${formatFileSize(fileLength)}")
            buffer
        } catch (e: Exception) {
            e.printStackTrace()
            modelStatusMap[definition.id] = "Load Failed: ${e.message?.take(60)}"
            loadingProgressMap[definition.id] = 0f
            isLoadingMap[definition.id] = false
            null
        }
    }

    /**
     * Evict a model from RAM cache to free memory.
     */
    fun evictFromRam(modelId: String) {
        inRamModelBuffers.remove(modelId)
        inRamLoadedSessions[modelId] = false
        val def = allModels.firstOrNull { it.id == modelId }
        modelStatusMap[modelId] = if (def != null) "Ready on Disk" else "Unknown"
        println("pEQLearn: [ONNX] Evicted $modelId from RAM cache")
    }

    // ── Model Downloading ───────────────────────────────────────────────────

    /**
     * Download an ONNX model from its URL to the local models directory.
     * Supports resume via .part files and tracks real progress.
     */
    suspend fun downloadModel(
        modelsDir: File,
        definition: OnnxModelDefinition,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (!modelsDir.exists()) modelsDir.mkdirs()
        val targetFile = File(modelsDir, definition.fileName)
        val tempFile = File(modelsDir, "${definition.fileName}.part")

        // Already exists check
        if (targetFile.exists() && targetFile.length() > 1000L) {
            downloadProgressMap[definition.id] = 1.0f
            isDownloadingMap[definition.id] = false
            modelStatusMap[definition.id] = "Ready on Disk (${formatFileSize(targetFile.length())})"
            onProgress(1.0f)
            return@withContext true
        }

        isDownloadingMap[definition.id] = true
        downloadProgressMap[definition.id] = 0f
        modelStatusMap[definition.id] = "Connecting..."

        try {
            var url = URL(definition.downloadUrl)
            var connection: HttpURLConnection
            var redirectCount = 0

            // Follow redirects (HuggingFace uses 302)
            while (redirectCount < 5) {
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 20_000
                connection.readTimeout = 60_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "pEQ-Learn-Android/1.0")
                connection.connect()

                val code = connection.responseCode
                if (code in 301..303 || code == 307 || code == 308) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) {
                        url = if (location.startsWith("http")) URL(location) else URL(url, location)
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }

                if (code != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    modelStatusMap[definition.id] = "HTTP Error $code"
                    isDownloadingMap[definition.id] = false
                    return@withContext false
                }

                val fileLength = connection.contentLengthLong.takeIf { it > 0 } ?: definition.sizeBytes
                var totalBytesRead = 0L

                modelStatusMap[definition.id] = "Downloading (${formatFileSize(fileLength)})..."

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32768)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            val progress = (totalBytesRead.toFloat() / fileLength.toFloat()).coerceIn(0f, 0.99f)
                            downloadProgressMap[definition.id] = progress
                            onProgress(progress)
                        }
                    }
                }

                connection.disconnect()

                // Rename temp → final
                if (tempFile.exists() && tempFile.length() > 1000L) {
                    tempFile.renameTo(targetFile)
                    downloadProgressMap[definition.id] = 1.0f
                    isDownloadingMap[definition.id] = false
                    modelStatusMap[definition.id] = "Ready on Disk (${formatFileSize(targetFile.length())})"
                    onProgress(1.0f)
                    println("pEQLearn: [ONNX] Downloaded ${definition.name}: ${formatFileSize(targetFile.length())}")
                    return@withContext true
                } else {
                    tempFile.delete()
                    modelStatusMap[definition.id] = "Download Incomplete"
                    isDownloadingMap[definition.id] = false
                    return@withContext false
                }
            }

            modelStatusMap[definition.id] = "Too Many Redirects"
            isDownloadingMap[definition.id] = false
            false
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            isDownloadingMap[definition.id] = false
            modelStatusMap[definition.id] = "Download Failed: ${e.message?.take(50)}"
            false
        }
    }

    // ── Custom Model Import ─────────────────────────────────────────────────

    /**
     * Import a custom ONNX model from a picked file path.
     * Copies the file into the app's models directory, registers it as
     * a custom model, and persists the registration to SharedPreferences.
     */
    suspend fun importCustomModel(
        modelsDir: File,
        sourcePath: String,
        displayName: String,
        purpose: String = "Custom AMT Model"
    ): OnnxModelDefinition? = withContext(Dispatchers.IO) {
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val sourceFile = File(sourcePath)
        if (!sourceFile.exists() || sourceFile.length() < 1000L) {
            println("pEQLearn: [ONNX] Import failed: source file not found or too small: $sourcePath")
            return@withContext null
        }

        val sanitizedName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(modelsDir, sanitizedName)

        // Copy if not already in models dir
        if (destFile.absolutePath != sourceFile.absolutePath) {
            try {
                sourceFile.inputStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output, 65536)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

        val modelId = "custom_${sanitizedName.hashCode().toUInt()}"
        val definition = OnnxModelDefinition(
            id = modelId,
            name = displayName.removeSuffix(".onnx"),
            fileName = sanitizedName,
            downloadUrl = "",
            sizeBytes = destFile.length(),
            description = "User-imported ONNX model: $displayName",
            purpose = purpose,
            isCustom = true,
            localPath = destFile.absolutePath
        )

        customModels[modelId] = definition
        modelStatusMap[modelId] = "Ready on Disk (${formatFileSize(destFile.length())})"
        downloadProgressMap[modelId] = 1.0f

        println("pEQLearn: [ONNX] Imported custom model '$displayName' → ${destFile.absolutePath}")
        definition
    }

    /**
     * Remove a custom model from the registry and optionally delete its file.
     */
    fun removeCustomModel(modelId: String, deleteFile: Boolean = true) {
        val def = customModels.remove(modelId)
        if (def != null && deleteFile) {
            try {
                if (def.localPath.isNotEmpty()) File(def.localPath).delete()
            } catch (_: Exception) {}
        }
        evictFromRam(modelId)
        modelStatusMap.remove(modelId)
        downloadProgressMap.remove(modelId)
        isDownloadingMap.remove(modelId)
    }

    // ── Model Switching ─────────────────────────────────────────────────────

    /**
     * Set the active model for a given purpose category (e.g. "transcription", "stem_split").
     */
    fun setActiveModel(purpose: String, modelId: String) {
        activeTranscriptionModelId[purpose] = modelId
        val def = allModels.firstOrNull { it.id == modelId }
        println("pEQLearn: [ONNX] Active model for '$purpose' → ${def?.name ?: modelId}")
    }

    /**
     * Get the active model definition for a given purpose.
     */
    fun getActiveModel(purpose: String): OnnxModelDefinition? {
        val activeId = activeTranscriptionModelId[purpose] ?: return null
        return allModels.firstOrNull { it.id == activeId }
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    /**
     * Serialize custom models to JSON string for persistent storage.
     */
    fun serializeCustomModels(): String {
        return jsonSerializer.encodeToString(customModels.values.toList())
    }

    /**
     * Serialize active model selections to JSON string for persistent storage.
     */
    fun serializeActiveModels(): String {
        return jsonSerializer.encodeToString(activeTranscriptionModelId.toMap())
    }

    /**
     * Save custom model definitions and active model selections via string saving callback.
     */
    fun persistState(saveString: (key: String, value: String) -> Unit) {
        try {
            val customJson = serializeCustomModels()
            val activeJson = serializeActiveModels()
            saveString("onnx_custom_models", customJson)
            saveString("onnx_active_models", activeJson)
            println("pEQLearn: [ONNX] Persisted ${customModels.size} custom models")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Restore custom model definitions and active selections from serialized state.
     */
    fun restoreState(
        getString: (key: String) -> String?,
        modelsDir: File,
        resolveAssetPath: ((String) -> String?)? = null
    ) {
        try {
            // Restore custom models
            val customJson = getString("onnx_custom_models")
            if (!customJson.isNullOrBlank()) {
                val restored = jsonSerializer.decodeFromString<List<OnnxModelDefinition>>(customJson)
                restored.forEach { def ->
                    customModels[def.id] = def
                    // Validate file still exists
                    val file = getModelFile(modelsDir, def)
                    if (file.exists() && file.length() > 1000L) {
                        modelStatusMap[def.id] = "Ready on Disk (${formatFileSize(file.length())})"
                        downloadProgressMap[def.id] = 1.0f
                    } else {
                        modelStatusMap[def.id] = "File Missing"
                        downloadProgressMap[def.id] = 0f
                    }
                }
                println("pEQLearn: [ONNX] Restored ${restored.size} custom models")
            }

            // Restore active model selections
            val activeJson = getString("onnx_active_models")
            if (!activeJson.isNullOrBlank()) {
                val restored = jsonSerializer.decodeFromString<Map<String, String>>(activeJson)
                restored.forEach { (purpose, modelId) ->
                    activeTranscriptionModelId[purpose] = modelId
                }
            }

            // Scan built-in models and set initial status
            builtInModels.forEach { def ->
                val file = File(modelsDir, def.fileName)
                val resolvedPath = if (file.exists() && file.length() > 1000L) {
                    file.absolutePath
                } else {
                    resolveAssetPath?.invoke(def.fileName)
                }

                if (resolvedPath != null && File(resolvedPath).exists() && File(resolvedPath).length() > 1000L) {
                    val actualLen = File(resolvedPath).length()
                    modelStatusMap[def.id] = "Ready (Assets - ${formatFileSize(actualLen)})"
                    downloadProgressMap[def.id] = 1.0f
                } else {
                    modelStatusMap[def.id] = "Not Downloaded"
                    downloadProgressMap[def.id] = 0f
                }
            }

            // Default active transcription model
            if (!activeTranscriptionModelId.containsKey("transcription")) {
                activeTranscriptionModelId["transcription"] = MUSCRIPTOR_SMALL.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
