package com.creativeblisslabs.peqlearn

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.creativeblisslabs.peqlearn.app.App
import com.creativeblisslabs.peqlearn.app.LocalAppViewModel
import com.creativeblisslabs.peqlearn.app.ai.AndroidpEQStemSplitter
import com.creativeblisslabs.peqlearn.app.audio.AndroidAudioEngine
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.midi.AndroidMidiHandler
import com.creativeblisslabs.peqlearn.app.services.pEQMediaSessionService
import com.creativeblisslabs.peqlearn.app.security.initPermissionManager
import com.creativeblisslabs.peqlearn.app.security.getPermissionManager
import com.creativeblisslabs.peqlearn.app.data.initPersistenceManager
import com.creativeblisslabs.peqlearn.app.data.getPersistenceManager
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.playExternalFile
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.nextTrack
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.previousTrack
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.persistSession
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.pEQViewModelProvider
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private var mediaController: MediaController? = null
    private lateinit var viewModel: AppViewModel
    private var mediaCommandReceiver: BroadcastReceiver? = null
    private var midiHandler: AndroidMidiHandler? = null

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        enableEdgeToEdge()

        initPermissionManager(this)
        initPersistenceManager(applicationContext)

        val audioEngine = AndroidAudioEngine(this).apply { initialize(0) }

        viewModel = AppViewModel(
            audioEngine = audioEngine,
            persistenceManager = getPersistenceManager(),
            stemSplitter = AndroidpEQStemSplitter(this),
            cloudDownloader = null,
            ragaRepository = null
        ).apply {
            pEQViewModelProvider.viewModel = this
            scanner = AndroidFileScanner(this@MainActivity)
        }

        // Initialize and start Android MIDI Handler
        midiHandler = AndroidMidiHandler(this, viewModel).apply {
            start()
        }

        viewModel.onScanMidiDevicesRequested = {
            midiHandler?.start()
        }

       viewModel.onVideoExportRequested = { score, res, fps, effects, progressCallback, completeCallback ->
            lifecycleScope.launch {
                com.creativeblisslabs.peqlearn.app.video.AndroidVideoExporter.exportMidiVideo(
                    score = score,
                    resolution = res,
                    fps = fps,
                    effects = effects,
                    onProgress = progressCallback,
                    onComplete = completeCallback
                )
            }
        }

        viewModel.onPdfExportRequested = { score, options, progressCallback, completeCallback ->
            lifecycleScope.launch {
                com.creativeblisslabs.peqlearn.app.pdf.AndroidPdfExporter.exportScoreToPdf(
                    context = applicationContext,
                    score = score,
                    options = options,
                    onProgress = progressCallback,
                    onComplete = completeCallback
                )
            }
        }

        setupLaunchers()

        getPermissionManager().apply {
            requestMediaPermissions()
            requestMicrophonePermission()
            requestNotificationPermission()
        }

        performCleanup()
        initializeExternalTools()

        setContent {
            CompositionLocalProvider(LocalAppViewModel provides viewModel) {
                App()
            }
        }

        startSystemSync()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerMediaCommandReceiver()
        }
        handleIntent(intent)
        viewModel.start()
    }

    private fun performCleanup() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val statePrefs = getSharedPreferences("pEQ_player_state", MODE_PRIVATE)
                val activePath = statePrefs.getString("track_uri", null)?.let { Uri.parse(it).path }
                listOf(File(cacheDir, "content_cache"), File(cacheDir, "stems")).forEach { dir ->
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles()?.forEach { file ->
                            if (activePath == null || !file.absolutePath.contains(activePath)) file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initializeExternalTools() {
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupLaunchers() {
        var trackPickerCallback: ((com.creativeblisslabs.peqlearn.app.data.pEQMediaItem) -> Unit)? = null
        val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                lifecycleScope.launch {
                    val displayName = getFileName(it) ?: "Imported Track"
                    val mimeType = contentResolver.getType(it)
                    val sizeBytes = getFileSize(it)
                    val durationMs = getFileDuration(it)
                    
                    val item = com.creativeblisslabs.peqlearn.app.data.pEQMediaItem(
                        id = "external_${it.toString().hashCode()}",
                        title = displayName.substringBeforeLast(".", displayName),
                        subtitle = "External Source",
                        artist = "External Source",
                        path = it.toString(),
                        type = if (mimeType?.startsWith("video/") == true) com.creativeblisslabs.peqlearn.app.data.MediaType.VIDEO else com.creativeblisslabs.peqlearn.app.data.MediaType.AUDIO,
                        format = displayName.substringAfterLast(".", "AUDIO").uppercase(),
                        sizeBytes = sizeBytes,
                        durationMs = durationMs
                    )

                    if (trackPickerCallback != null) {
                        trackPickerCallback?.invoke(item)
                        trackPickerCallback = null
                    } else {
                        viewModel.playExternalFile(
                            path = it.toString(),
                            displayName = displayName,
                            mimeType = mimeType,
                            sizeBytes = sizeBytes,
                            durationMs = durationMs
                        )
                    }
                }
            }
        }

        val scorePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                lifecycleScope.launch {
                    val fileName = getFileName(it) ?: "Imported Score"
                    val bytes = try {
                        contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                    } catch (e: Exception) {
                        null
                    }
                    viewModel.parseIncomingScoreFile(it.toString(), bytes, fileName)
                }
            }
        }

        var modelPickerCallback: ((String, String) -> Unit)? = null
        val modelPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                lifecycleScope.launch {
                    val name = getFileName(it) ?: "custom_model.onnx"
                    val modelsDir = File(filesDir, "models").apply { if (!exists()) mkdirs() }
                    val destFile = File(modelsDir, name)
                    try {
                        contentResolver.openInputStream(it)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        modelPickerCallback?.invoke(destFile.absolutePath, name)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        viewModel.onPickModelRequested = { callback ->
            modelPickerCallback = callback
            modelPickerLauncher.launch(arrayOf("*/*"))
        }

        viewModel.onPickTrackRequested = { callback ->
            trackPickerCallback = callback
            filePickerLauncher.launch(arrayOf("audio/*", "video/*"))
        }
        viewModel.onPickScoreFileRequested = {
            scorePickerLauncher.launch(
                arrayOf(
                    "audio/midi",
                    "audio/x-midi",
                    "application/xml",
                    "text/xml",
                    "application/pdf",
                    "*/*"
                )
            )
        }
        viewModel.onRestoreSamplesRequested = { restoreFactorySamples() }
    }

    private fun restoreFactorySamples() {
        val samplesDir = File("/storage/emulated/0/pEQ/Samples")
        if (!samplesDir.exists()) samplesDir.mkdirs()
        listOf("KICK", "SNARE", "HIHAT", "TOM-H", "CRASH", "RIDE", "COWBELL", "SHAKER").forEach { type ->
            val sampleFile = File(samplesDir, "${type.lowercase()}.wav")
            if (!sampleFile.exists()) {
                try {
                    assets.open("samples/${type.lowercase()}.wav").use { input ->
                        sampleFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) return cursor.getString(nameIndex)
        }
        return uri.path?.substringAfterLast('/')
    }

    private fun getFileSize(uri: Uri): Long {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) return cursor.getLong(sizeIndex)
        }
        return 0L
    }

    private fun getFileDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerMediaCommandReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.creativeblisslabs.peqlearn.app.ACTION_SYSTEM_NEXT")
            addAction("com.creativeblisslabs.peqlearn.app.ACTION_SYSTEM_PREV")
        }
        mediaCommandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.creativeblisslabs.peqlearn.app.ACTION_SYSTEM_NEXT" -> viewModel.nextTrack()
                    "com.creativeblisslabs.peqlearn.app.ACTION_SYSTEM_PREV" -> viewModel.previousTrack()
                }
            }
        }
        registerReceiver(mediaCommandReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                lifecycleScope.launch {
                    viewModel.playExternalFile(
                        path = uri.toString(),
                        displayName = getFileName(uri) ?: "Media",
                        mimeType = null,
                        sizeBytes = getFileSize(uri),
                        title = null,
                        durationMs = getFileDuration(uri)
                    )
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun startSystemSync() {
        try {
            val sessionToken = SessionToken(this, ComponentName(this, pEQMediaSessionService::class.java))
            val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

            controllerFuture.addListener({
                try {
                    mediaController = controllerFuture.get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        midiHandler?.stop()
        mediaController?.release()
        mediaCommandReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        viewModel.persistSession()
    }

    override fun onResume() {
        super.onResume()
        viewModel.isAppInBackground.value = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1005 && resultCode == RESULT_OK) {
            if (::viewModel.isInitialized) {
                lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.scanner?.refresh()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (granted && ::viewModel.isInitialized) {
                lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.scanner?.refresh()
                }
            }
        }
    }
}