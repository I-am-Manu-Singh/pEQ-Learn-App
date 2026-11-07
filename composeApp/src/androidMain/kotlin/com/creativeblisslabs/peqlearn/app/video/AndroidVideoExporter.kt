package com.creativeblisslabs.peqlearn.app.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asAndroidBitmap
import com.creativeblisslabs.peqlearn.app.services.pEQVideoExportService
import com.creativeblisslabs.peqlearn.app.ui.components.setKeepScreenOn
import com.creativeblisslabs.peqlearn.app.ui.screens.Accidental
import com.creativeblisslabs.peqlearn.app.ui.screens.ExportTimelineNote
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import com.creativeblisslabs.peqlearn.app.ui.screens.TabArticulation
import com.creativeblisslabs.peqlearn.app.ui.screens.VisualizerStageCaptureController
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic Frame-by-Frame MP4 Video & Audio Exporter for pEQ Learn.
 *
 * Captures the live Compose Visualizer UI (StaffAndTabNotationViewer + SynthesiaWaterfallOverlay +
 * UnifiedPianoKeyboard / Guitar / Sitar / Harmonium / Violin / Flute) frame-by-frame with overlays
 * hidden, and encodes to H.264 + AAC MP4 with exact buffer presentation timestamps.
 */
object AndroidVideoExporter {

    private val NOTE_NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val GUITAR_OPEN_MIDI = intArrayOf(64, 59, 55, 50, 45, 40)

    private data class ExportSpark(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var size: Float, var life: Float, val maxLife: Float, val color: Int
    )

    private data class ExportFlash(
        val x: Float, val y: Float, var radius: Float, var alpha: Float,
        val color: Int, val isChord: Boolean
    )

    data class ExportFlatNote(
        val note: ScoreNote, val measureIndex: Int, val startSec: Float,
        val durationSec: Float, val endSec: Float, val isRightHand: Boolean, val isChord: Boolean
    )

    /**
     * Resolves the Android application [Context] automatically even if the caller in commonMain
     * passed `context = null`.
     */
    private fun resolveAppContext(explicitContext: Context?): Context? {
        if (explicitContext != null) return explicitContext
        return runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val method = activityThreadClass.getMethod("currentApplication")
            method.invoke(null) as? Context
        }.getOrNull()
    }

    /**
     * Optimized 2x2 block ARGB_8888 -> YUV420 (NV12 / I420) encoder.
     * Processes 4 pixels per iteration to cut inner loop iterations by 75% and
     * eliminate per-pixel division and bounds checks.
     */
    private fun encodeArgbToYuv420(
        argbPixels: IntArray,
        yuvBuffer: ByteArray,
        width: Int,
        height: Int,
        isSemiPlanarNV12: Boolean
    ) {
        val frameSize = width * height
        var uvIndex = frameSize
        var uIndex = frameSize
        var vIndex = frameSize + (frameSize shr 2)

        for (j in 0 until height step 2) {
            val row1 = j * width
            val row2 = (j + 1) * width
            for (i in 0 until width step 2) {
                val p00 = argbPixels[row1 + i]
                val p01 = argbPixels[row1 + i + 1]
                val p10 = argbPixels[row2 + i]
                val p11 = argbPixels[row2 + i + 1]

                val r00 = (p00 shr 16) and 0xFF
                val g00 = (p00 shr 8) and 0xFF
                val b00 = p00 and 0xFF

                val r01 = (p01 shr 16) and 0xFF
                val g01 = (p01 shr 8) and 0xFF
                val b01 = p01 and 0xFF

                val r10 = (p10 shr 16) and 0xFF
                val g10 = (p10 shr 8) and 0xFF
                val b10 = p10 and 0xFF

                val r11 = (p11 shr 16) and 0xFF
                val g11 = (p11 shr 8) and 0xFF
                val b11 = p11 and 0xFF

                // Fast integer Y calculation for BT.601
                val y00 = (((66 * r00 + 129 * g00 + 25 * b00 + 128) shr 8) + 16).coerceIn(16, 235)
                val y01 = (((66 * r01 + 129 * g01 + 25 * b01 + 128) shr 8) + 16).coerceIn(16, 235)
                val y10 = (((66 * r10 + 129 * g10 + 25 * b10 + 128) shr 8) + 16).coerceIn(16, 235)
                val y11 = (((66 * r11 + 129 * g11 + 25 * b11 + 128) shr 8) + 16).coerceIn(16, 235)

                yuvBuffer[row1 + i] = y00.toByte()
                yuvBuffer[row1 + i + 1] = y01.toByte()
                yuvBuffer[row2 + i] = y10.toByte()
                yuvBuffer[row2 + i + 1] = y11.toByte()

                // Calculate U & V once per 2x2 pixel block using average RGB
                val rAvg = (r00 + r01 + r10 + r11) shr 2
                val gAvg = (g00 + g01 + g10 + g11) shr 2
                val bAvg = (b00 + b01 + b10 + b11) shr 2

                val u = (((-38 * rAvg - 74 * gAvg + 112 * bAvg + 128) shr 8) + 128).coerceIn(16, 240)
                val v = (((112 * rAvg - 94 * gAvg - 18 * bAvg + 128) shr 8) + 128).coerceIn(16, 240)

                if (isSemiPlanarNV12) {
                    yuvBuffer[uvIndex++] = u.toByte()
                    yuvBuffer[uvIndex++] = v.toByte()
                } else {
                    yuvBuffer[uIndex++] = u.toByte()
                    yuvBuffer[vIndex++] = v.toByte()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportMidiVideo(
        context: Context? = null,
        score: Score,
        resolution: String,
        fps: Int,
        effects: Map<String, Boolean>,
        layoutMode: String = "Score + Waterfall",
        notationFormat: String = "Staff Only",
        visualTheme: String = "Cyberpunk",
        bitrateMbps: Int = 12,
        sampleRateHz: Int = 44100,
        cameraYawDeg: Float = 0f,
        cameraPitchDeg: Float = 0f,
        eqBarsCount: Int = 48,
        peakSensitivity: Float = 75f,
        rightHandColorInt: Int? = null,
        leftHandColorInt: Int? = null,
        isRightHandHidden: Boolean = false,
        isLeftHandHidden: Boolean = false,
        enabledTrackIndices: Map<Int, Boolean> = emptyMap(),
        externalAudioUri: String? = null,
        viewModel: AppViewModel? = null,
        stageCaptureController: VisualizerStageCaptureController? = null,
        onProgress: (Float) -> Unit,
        onComplete: (String) -> Unit
    ) {
        withContext(Dispatchers.Default) {
            val appContext = resolveAppContext(context)

            // Ensure dimensions are multiples of 16 for hardware H.264 encoder compatibility
            val (rawW, rawH) = when (resolution.lowercase()) {
                "4k", "2160p" -> Pair(3840, 2160)
                "1440p", "2k" -> Pair(2560, 1440)
                "720p" -> Pair(1280, 720)
                else -> Pair(1920, 1080)
            }
            val width = (rawW / 16) * 16
            val height = (rawH / 16) * 16
            val scale = width / 1920f
            val targetFps = fps.coerceIn(24, 60)

            val totalMeasures = score.measures.size.coerceAtLeast(1)
            val playbackSpeed = viewModel?.playbackSpeed?.value?.coerceIn(0.25f, 2.5f) ?: 1.0f
            val bpm = (score.tempo.coerceAtLeast(30f) * playbackSpeed).coerceAtLeast(20f)

            val measureStartTimes = FloatArray(totalMeasures)
            val measureDurations = FloatArray(totalMeasures)
            val measureQuarterBeats = FloatArray(totalMeasures)
            var accTimeSec = 0f

            for (mIdx in 0 until totalMeasures) {
                val m = score.measures[mIdx]
                val qBeats = (m.timeSignatureNumerator.toFloat() * (4f / m.timeSignatureDenominator.coerceAtLeast(1).toFloat())).coerceAtLeast(1f)
                val mSec = (qBeats * 60f) / bpm
                measureStartTimes[mIdx] = accTimeSec
                measureDurations[mIdx] = mSec
                measureQuarterBeats[mIdx] = qBeats
                accTimeSec += mSec
            }

            val tailSec = 1.8f
            val totalDurationSec = (accTimeSec + tailSec).coerceAtLeast(3.0f)
            val totalFrames = (totalDurationSec * targetFps).toInt().coerceIn(30, 36000)

            val cleanTitle = score.title.ifBlank { "pEQ_Composition" }
                .replace(" ", "_")
                .replace("[^a-zA-Z0-9_]".toRegex(), "")
            val videoFileName = "pEQ_Render_${cleanTitle}_${System.currentTimeMillis()}.mp4"

            // Safe output file creation in cacheDir or Movies directory
            val outputFile = if (appContext != null) {
                File(appContext.cacheDir, videoFileName)
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val exportDir = File(moviesDir, "pEQ_Visualizer")
                if (!exportDir.exists()) exportDir.mkdirs()
                File(exportDir, videoFileName)
            }

            // Build Flattened Note Timeline for Audio & Frame Sync
            val flatNotes = ArrayList<ExportFlatNote>()
            val controllerTimelineNotes = ArrayList<ExportTimelineNote>()
            for (mIdx in 0 until totalMeasures) {
                val measure = score.measures[mIdx]
                val mStart = measureStartTimes[mIdx]
                val secPerBeat = 60f / bpm
                val hasDiffVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }
                val posCounts = measure.notes.filter { !it.isRest }.groupingBy { (it.position * 20f).toInt() }.eachCount()

                for (note in measure.notes) {
                    if (note.isRest) continue
                    if (!(enabledTrackIndices[note.voice] ?: true)) continue
                    val isRh = if (hasDiffVoices) note.voice == 0 else note.midiNote >= 60
                    if (isRh && isRightHandHidden) continue
                    if (!isRh && isLeftHandHidden) continue

                    val nStart = mStart + note.position * secPerBeat
                    val nDur = (note.duration * secPerBeat).coerceAtLeast(0.08f)
                    val isChord = (posCounts[(note.position * 20f).toInt()] ?: 0) >= 2
                    flatNotes.add(
                        ExportFlatNote(
                            note = note,
                            measureIndex = mIdx,
                            startSec = nStart,
                            durationSec = nDur,
                            endSec = nStart + nDur,
                            isRightHand = isRh,
                            isChord = isChord
                        )
                    )
                    controllerTimelineNotes.add(
                        ExportTimelineNote(
                            midiNote = note.midiNote,
                            startSec = nStart,
                            endSec = nStart + nDur,
                            voice = note.voice,
                            isRightHand = isRh
                        )
                    )
                }
            }
            flatNotes.sortBy { it.startSec }
            controllerTimelineNotes.sortBy { it.startSec }

            // Synthesize Polyphonic Studio Audio Track
            val sampleRate = sampleRateHz.coerceIn(44100, 48000)
            val pcmByteBuffer = AudioSynthesizer.synthesizeAudioTrack(flatNotes, totalDurationSec, sampleRate)

            // Mount Fullscreen UI Stage if not already open and wait up to 600ms for GraphicsLayer to attach
            val wasPlayingBeforeExport = viewModel?.isScorePlaying?.value == true
            val wasFullscreenBeforeExport = viewModel?.isFullscreenKeyboard?.value == true

            if (viewModel != null) {
                withContext(Dispatchers.Main) {
                    if (wasPlayingBeforeExport) viewModel.isScorePlaying.value = false
                    viewModel.isVideoExporting.value = true
                    viewModel.isFullscreenKeyboard.value = true
                }
                var waitAttempts = 0
                while (viewModel.visualizerStageCaptureController?.graphicsLayer == null && waitAttempts < 35) {
                    delay(20L)
                    waitAttempts++
                }
            }

            val activeController = stageCaptureController ?: viewModel?.visualizerStageCaptureController
            if (activeController != null) {
                withContext(Dispatchers.Main) {
                    activeController.isExportingVideo = true
                    activeController.exportProgress = 0f
                }
                delay(50L) // Allow animated overlays to hide completely before frame 0
            }

            // Configure Hardware AVC Video Encoder (ByteBuffer YUV420 mode for 100% deterministic PTS)
            val videoMime = MediaFormat.MIMETYPE_VIDEO_AVC
            val targetBitrate = bitrateMbps.coerceIn(4, 35) * 1_000_000
            val videoFormat = MediaFormat.createVideoFormat(videoMime, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }

            var videoEncoder: MediaCodec? = null
            var audioEncoder: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var videoTrackIdx = -1
            var audioTrackIdx = -1
            var muxerStarted = false
            var isSemiPlanarNV12 = true

            try {
                videoEncoder = MediaCodec.createEncoderByType(videoMime).apply {
                    configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    // Check input color format to decide NV12 (semi-planar) vs I420 (planar)
                    val inputFmt = runCatching { inputFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT) }.getOrNull()
                    isSemiPlanarNV12 = inputFmt != MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                    start()
                }
                audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                    configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    start()
                }
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } catch (e: Exception) {
                e.printStackTrace()
                videoEncoder = null
            }

            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val dstRect = Rect(0, 0, width, height)
            val bitmapScalePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val argbPixels = IntArray(width * height)
            val yuvBytes = ByteArray((width * height * 3) / 2)

            val rhColor = rightHandColorInt ?: viewModel?.rightHandColor?.value?.let {
                Color.argb((it.alpha * 255).toInt(), (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt())
            } ?: Color.parseColor("#4A90E2")
            val lhColor = leftHandColorInt ?: viewModel?.leftHandColor?.value?.let {
                Color.argb((it.alpha * 255).toInt(), (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt())
            } ?: Color.parseColor("#6C63FF")

            val fallbackRenderer = HeadlessFallbackFrameRenderer(
                width = width,
                height = height,
                scale = scale,
                score = score,
                bpm = bpm,
                layoutMode = layoutMode,
                notationFormat = notationFormat,
                visualTheme = visualTheme,
                effects = effects,
                rhColor = rhColor,
                lhColor = lhColor,
                isRightHandHidden = isRightHandHidden,
                isLeftHandHidden = isLeftHandHidden,
                enabledTrackIndices = enabledTrackIndices,
                eqBarsCount = eqBarsCount,
                peakSensitivity = peakSensitivity,
                cameraYawDeg = cameraYawDeg,
                cameraPitchDeg = cameraPitchDeg,
                measureStartTimes = measureStartTimes,
                measureDurations = measureDurations,
                measureQuarterBeats = measureQuarterBeats,
                flatNotes = flatNotes
            )

            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()
            var pcmAudioQueuedBytes = 0
            val bytesPerSample = 2
            var audioEosSent = false
            var videoEosSent = false

            // Buffer queues for packets that arrive before both tracks have emitted INFO_OUTPUT_FORMAT_CHANGED
            data class PendingPacket(val isVideo: Boolean, val data: ByteBuffer, val info: MediaCodec.BufferInfo)
            val pendingPackets = ArrayList<PendingPacket>()

            fun tryStartMuxerIfReady() {
                val mux = muxer ?: return
                if (!muxerStarted && videoTrackIdx >= 0 && (audioEncoder == null || audioTrackIdx >= 0)) {
                    mux.start()
                    muxerStarted = true
                    for (pkt in pendingPackets) {
                        val tIdx = if (pkt.isVideo) videoTrackIdx else audioTrackIdx
                        if (tIdx >= 0) mux.writeSampleData(tIdx, pkt.data, pkt.info)
                    }
                    pendingPackets.clear()
                }
            }

            fun drainEncoders(endOfStream: Boolean) {
                val vEnc = videoEncoder ?: return
                val aEnc = audioEncoder
                val mux = muxer ?: return

                while (true) {
                    val outIdx = vEnc.dequeueOutputBuffer(videoBufferInfo, if (endOfStream) 2500L else 0L)
                    if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (videoTrackIdx < 0) {
                            videoTrackIdx = mux.addTrack(vEnc.outputFormat)
                            tryStartMuxerIfReady()
                        }
                    } else if (outIdx >= 0) {
                        val encodedData = vEnc.getOutputBuffer(outIdx)
                        val isConfig = (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (encodedData != null && videoBufferInfo.size > 0 && !isConfig) {
                            encodedData.position(videoBufferInfo.offset)
                            encodedData.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            if (muxerStarted) {
                                mux.writeSampleData(videoTrackIdx, encodedData, videoBufferInfo)
                            } else {
                                val copy = ByteBuffer.allocateDirect(videoBufferInfo.size)
                                copy.put(encodedData)
                                copy.flip()
                                val infoCopy = MediaCodec.BufferInfo().apply {
                                    set(0, videoBufferInfo.size, videoBufferInfo.presentationTimeUs, videoBufferInfo.flags)
                                }
                                pendingPackets.add(PendingPacket(true, copy, infoCopy))
                            }
                        }
                        vEnc.releaseOutputBuffer(outIdx, false)
                        if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    } else {
                        break
                    }
                }

                if (aEnc != null) {
                    while (true) {
                        val aOutIdx = aEnc.dequeueOutputBuffer(audioBufferInfo, if (endOfStream) 2500L else 0L)
                        if (aOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (audioTrackIdx < 0) {
                                audioTrackIdx = mux.addTrack(aEnc.outputFormat)
                                tryStartMuxerIfReady()
                            }
                        } else if (aOutIdx >= 0) {
                            val encData = aEnc.getOutputBuffer(aOutIdx)
                            val isConfig = (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                            if (encData != null && audioBufferInfo.size > 0 && !isConfig) {
                                encData.position(audioBufferInfo.offset)
                                encData.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                if (muxerStarted) {
                                    mux.writeSampleData(audioTrackIdx, encData, audioBufferInfo)
                                } else {
                                    val copy = ByteBuffer.allocateDirect(audioBufferInfo.size)
                                    copy.put(encData)
                                    copy.flip()
                                    val infoCopy = MediaCodec.BufferInfo().apply {
                                        set(0, audioBufferInfo.size, audioBufferInfo.presentationTimeUs, audioBufferInfo.flags)
                                    }
                                    pendingPackets.add(PendingPacket(false, copy, infoCopy))
                                }
                            }
                            aEnc.releaseOutputBuffer(aOutIdx, false)
                            if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                        } else {
                            break
                        }
                    }
                }
            }

            if (appContext != null) {
                pEQVideoExportService.startExportService(appContext)
            }
            setKeepScreenOn(true)
            val exportStartMs = System.currentTimeMillis()

            try {
                for (frameIndex in 0 until totalFrames) {
                    val currentTimeSec = frameIndex.toFloat() / targetFps
                    val progress = frameIndex.toFloat() / totalFrames
                    val ptsUs = (frameIndex.toLong() * 1_000_000L) / targetFps

                    // 1. Capture the exact live PianoKeyboard88Keys Compose UI Stage
                    val activeCtrl = stageCaptureController ?: viewModel?.visualizerStageCaptureController
                    val capturedUiBitmap: Bitmap? = if (activeCtrl != null && viewModel != null && activeCtrl.graphicsLayer != null) {
                        val imgBmp = activeCtrl.stepAndCaptureFrame(
                            viewModel = viewModel,
                            timeSec = currentTimeSec,
                            measureStartTimes = measureStartTimes,
                            measureDurations = measureDurations,
                            flatNotes = controllerTimelineNotes
                        )
                        imgBmp?.asAndroidBitmap()?.let { rawBmp ->
                            if (rawBmp.config == Bitmap.Config.HARDWARE) {
                                rawBmp.copy(Bitmap.Config.ARGB_8888, false)
                            } else {
                                rawBmp
                            }
                        }
                    } else {
                        null
                    }

                    if (capturedUiBitmap != null && capturedUiBitmap.width > 0 && capturedUiBitmap.height > 0) {
                        frameCanvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC)
                        val uiRect = Rect(0, 0, capturedUiBitmap.width, capturedUiBitmap.height)
                        frameCanvas.drawBitmap(capturedUiBitmap, uiRect, dstRect, bitmapScalePaint)
                    } else {
                        fallbackRenderer.renderFrame(frameCanvas, currentTimeSec)
                    }

                    // 2. Convert Frame Bitmap -> YUV420 and Queue into Hardware Video Encoder with Exact PTS
                    val vEnc = videoEncoder
                    if (vEnc != null && !videoEosSent) {
                        frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                        encodeArgbToYuv420(argbPixels, yuvBytes, width, height, isSemiPlanarNV12)

                        var queuedFrame = false
                        var retries = 0
                        while (!queuedFrame && retries < 25) {
                            val inIdx = vEnc.dequeueInputBuffer(5000L)
                            if (inIdx >= 0) {
                                val inBuf = vEnc.getInputBuffer(inIdx)
                                if (inBuf != null) {
                                    inBuf.clear()
                                    val copyLen = minOf(inBuf.remaining(), yuvBytes.size)
                                    inBuf.put(yuvBytes, 0, copyLen)
                                    val isLastFrame = (frameIndex == totalFrames - 1)
                                    val flags = if (isLastFrame) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                    vEnc.queueInputBuffer(inIdx, 0, copyLen, ptsUs, flags)
                                    if (isLastFrame) videoEosSent = true
                                    queuedFrame = true
                                }
                            } else {
                                drainEncoders(endOfStream = false)
                                retries++
                            }
                        }
                    }

                    // 3. Queue Synchronized Audio PCM Chunks
                    val aEnc = audioEncoder
                    if (aEnc != null && !audioEosSent) {
                        val targetAudioByteLimit = (((currentTimeSec + 0.15f) * sampleRate).toInt() * bytesPerSample)
                            .coerceAtMost(pcmByteBuffer.limit())

                        while (pcmAudioQueuedBytes < targetAudioByteLimit || (frameIndex == totalFrames - 1 && !audioEosSent)) {
                            val inIdx = aEnc.dequeueInputBuffer(2000L)
                            if (inIdx < 0) break
                            val inBuf = aEnc.getInputBuffer(inIdx) ?: break
                            inBuf.clear()

                            val remainingTotal = pcmByteBuffer.limit() - pcmAudioQueuedBytes
                            if (remainingTotal <= 0) {
                                val audioPtsUs = (pcmAudioQueuedBytes.toLong() * 1_000_000L) / (sampleRate * bytesPerSample)
                                aEnc.queueInputBuffer(inIdx, 0, 0, audioPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                audioEosSent = true
                                break
                            }

                            val toCopy = minOf(inBuf.remaining(), remainingTotal)
                            pcmByteBuffer.position(pcmAudioQueuedBytes)
                            val slice = pcmByteBuffer.slice()
                            slice.limit(toCopy)
                            inBuf.put(slice)

                            val audioPtsUs = (pcmAudioQueuedBytes.toLong() * 1_000_000L) / (sampleRate * bytesPerSample)
                            pcmAudioQueuedBytes += toCopy
                            val isLastChunk = pcmAudioQueuedBytes >= pcmByteBuffer.limit()
                            val flags = if (isLastChunk) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                            aEnc.queueInputBuffer(inIdx, 0, toCopy, audioPtsUs, flags)
                            if (isLastChunk) {
                                audioEosSent = true
                                break
                            }
                        }
                    }

                    drainEncoders(endOfStream = false)

                    if (frameIndex % (targetFps / 4).coerceAtLeast(1) == 0 || frameIndex == totalFrames - 1) {
                        val clampedProg = progress.coerceIn(0f, 0.99f)
                        activeController?.exportProgress = clampedProg

                        val elapsedMs = (System.currentTimeMillis() - exportStartMs).coerceAtLeast(1L)
                        val elapsedSec = elapsedMs / 1000f
                        val currentFrame = frameIndex + 1
                        val fpsRendered = currentFrame / elapsedSec
                        val remainingFrames = (totalFrames - currentFrame).coerceAtLeast(0)
                        val etaSec = if (fpsRendered > 0f) (remainingFrames / fpsRendered).toInt() else 0
                        val etaMin = etaSec / 60
                        val etaSecRem = etaSec % 60

                        val speedFactor = fpsRendered / targetFps
                        val speedText = if (speedFactor >= 1.0f) String.format("%.1fx (%.0f FPS)", speedFactor, fpsRendered) else String.format("%.2fx (%.0f FPS)", speedFactor, fpsRendered)
                        val frameText = "Frame $currentFrame / $totalFrames"
                        val etaText = String.format("ETA %02d:%02d • %s", etaMin, etaSecRem, speedText)

                        viewModel?.let { vm ->
                            vm.videoExportFrameText.value = frameText
                            vm.videoExportEtaText.value = etaText
                        }

                        if (appContext != null) {
                            pEQVideoExportService.updateProgress(appContext, clampedProg, frameText, etaText)
                        }

                        withContext(Dispatchers.Main) {
                            onProgress(clampedProg)
                        }
                    }
                }

                drainEncoders(endOfStream = true)
            } finally {
                if (appContext != null) {
                    pEQVideoExportService.stopExportService(appContext)
                }
                setKeepScreenOn(false)
                if (viewModel != null) {
                    withContext(Dispatchers.Main) {
                        viewModel.visualizerStageCaptureController?.exportTimeSec = null
                        viewModel.visualizerStageCaptureController?.isExportingVideo = false
                        viewModel.isVideoExporting.value = false
                        if (!wasFullscreenBeforeExport) {
                            viewModel.isFullscreenKeyboard.value = false
                        }
                        viewModel.resetScorePlayhead()
                    }
                }
                runCatching { audioEncoder?.stop(); audioEncoder?.release() }
                runCatching { videoEncoder?.stop(); videoEncoder?.release() }
                runCatching { if (muxerStarted) muxer?.stop(); muxer?.release() }
            }

            // Publish completed MP4 into Android MediaStore Movies/pEQ_Visualizer gallery
            var finalSavedPath = outputFile.absolutePath
            if (appContext != null && outputFile.exists() && outputFile.length() > 0L) {
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/pEQ_Visualizer")
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }
                    val resolver = appContext.contentResolver
                    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outStream ->
                            outputFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val completeValues = ContentValues().apply {
                                put(MediaStore.Video.Media.IS_PENDING, 0)
                            }
                            resolver.update(uri, completeValues, null, null)
                        }
                        finalSavedPath = "/Movies/pEQ_Visualizer/$videoFileName"
                    }
                }
            }

            frameBitmap.recycle()
            withContext(Dispatchers.Main) {
                onProgress(1.0f)
                onComplete(finalSavedPath)
            }
        }
    }

    /**
     * Fallback renderer if no Compose UI stage is attached.
     */
    private class HeadlessFallbackFrameRenderer(
        private val width: Int,
        private val height: Int,
        private val scale: Float,
        private val score: Score,
        private val bpm: Float,
        layoutMode: String,
        notationFormat: String,
        visualTheme: String,
        effects: Map<String, Boolean>,
        private val rhColor: Int,
        private val lhColor: Int,
        private val isRightHandHidden: Boolean,
        private val isLeftHandHidden: Boolean,
        private val enabledTrackIndices: Map<Int, Boolean>,
        private val eqBarsCount: Int,
        private val peakSensitivity: Float,
        private val cameraYawDeg: Float,
        private val cameraPitchDeg: Float,
        private val measureStartTimes: FloatArray,
        private val measureDurations: FloatArray,
        private val measureQuarterBeats: FloatArray,
        private val flatNotes: List<ExportFlatNote>
    ) {
        private val totalMeasures = score.measures.size.coerceAtLeast(1)
        private val normLayout = layoutMode.trim().lowercase()
        private val renderNotation = when {
            normLayout.contains("notation") || normLayout.contains("score") || normLayout.contains("split") -> true
            normLayout.contains("waterfall") -> false
            else -> effects["Notation"] ?: true
        }
        private val renderVisualizer = when {
            normLayout == "notation" || normLayout == "notation only" -> false
            else -> effects["Visualizer"] ?: true
        }
        private val normFmt = notationFormat.trim().lowercase()
        private val showStaffInNotation = normFmt != "tab" && normFmt != "tab only"
        private val showTabInNotation = normFmt == "tab" || normFmt == "tab only" || normFmt == "both" || normFmt == "both viewers"
        private val glowEnabled = effects["Glow"] ?: true
        private val particlesEnabled = effects["Particles"] ?: true

        private val bgTopColor = when (visualTheme.lowercase()) {
            "cyberpunk" -> Color.parseColor("#08090F")
            "neon fall" -> Color.parseColor("#050B0A")
            else -> Color.parseColor("#0D0B14")
        }
        private val bgBottomColor = Color.parseColor("#050508")
        private val accentGold = Color.parseColor("#FFD700")

        private val bottomSafetyMargin = 6f * scale
        private val notationHeight = if (renderNotation && renderVisualizer) {
            if (showStaffInNotation && showTabInNotation) height * 0.50f else height * 0.46f
        } else if (renderNotation) {
            height.toFloat()
        } else {
            0f
        }
        private val visualizerTop = if (renderNotation && renderVisualizer) notationHeight else 0f
        private val visualizerHeight = (height - visualizerTop).coerceAtLeast(0f)
        private val keyboardHeight = if (renderVisualizer) (visualizerHeight * 0.26f).coerceIn(135f * scale, 240f * scale) else 0f
        private val keyboardTop = height - keyboardHeight - bottomSafetyMargin
        private val keyboardBottom = height - bottomSafetyMargin
        private val waterfallHeight = (keyboardTop - visualizerTop).coerceAtLeast(0f)

        private val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), bgTopColor, bgBottomColor, Shader.TileMode.CLAMP)
        }
        private val staffLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#759E9E9E")
            strokeWidth = 1.4f * scale
        }
        private val barlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0CCCCCC")
            strokeWidth = 1.8f * scale
        }
        private val heavyBarlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0EAEAEA")
            strokeWidth = 4.0f * scale
        }
        private val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EAEAEA")
            strokeWidth = 1.8f * scale
        }
        private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EAEAEA")
            style = Paint.Style.FILL
        }
        private val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8EAEAEA")
            style = Paint.Style.STROKE
            strokeWidth = 2.0f * scale
        }
        private val noteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val noteStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.0f * scale
        }
        private val glowCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val measureNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentGold
            alpha = 180
            textSize = 12f * scale
            typeface = Typeface.DEFAULT_BOLD
        }
        private val clefSymbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EAEAEA")
            typeface = Typeface.SERIF
        }
        private val accidentalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EAEAEA")
            textSize = 15f * scale
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
        private val tabPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#252528")
            style = Paint.Style.FILL
        }
        private val tabFretTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f * scale
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentGold
            strokeWidth = 3.0f * scale
        }
        private val laneGridPaint = Paint().apply {
            color = Color.parseColor("#221510")
            strokeWidth = 1f
        }
        private val whiteKeyTopPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val whiteKeyLipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val whiteKeyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#661E222B")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * scale
        }
        private val blackKeyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#77000000")
        }
        private val blackKeyBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val keyBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        private val pillLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f * scale
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f * scale, 0f, 1f * scale, Color.argb(190, 0, 0, 0))
        }

        private val whiteNotes = (21..108).filter { n -> (n % 12) !in intArrayOf(1, 3, 6, 8, 10) }
        private val whiteKeyWidth = width / 52f
        private val keyXCenter = FloatArray(128)
        private val keyBoxWidth = FloatArray(128)
        private val keyIsBlack = BooleanArray(128)
        private val sparks = ArrayList<ExportSpark>(96)
        private val flashes = ArrayList<ExportFlash>(32)
        private val prevActiveKeys = BooleanArray(128)
        private val curActiveKeys = BooleanArray(128)
        private val reusableRect = RectF()
        private val beamPath = Path()
        private val slurPath = Path()

        init {
            for (midi in 21..108) {
                val isB = (midi % 12) in intArrayOf(1, 3, 6, 8, 10)
                keyIsBlack[midi] = isB
                if (!isB) {
                    val wIdx = whiteNotes.indexOf(midi).coerceAtLeast(0)
                    keyXCenter[midi] = wIdx * whiteKeyWidth + whiteKeyWidth * 0.5f
                    keyBoxWidth[midi] = whiteKeyWidth * 0.82f
                } else {
                    val prevW = whiteNotes.indexOf(midi - 1).coerceAtLeast(0)
                    keyXCenter[midi] = (prevW + 1) * whiteKeyWidth
                    keyBoxWidth[midi] = whiteKeyWidth * 0.58f
                }
            }
        }

        fun renderFrame(frameCanvas: Canvas, currentTimeSec: Float) {
            frameCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            var currentMeasureIdx = 0
            for (i in 0 until totalMeasures) {
                if (currentTimeSec >= measureStartTimes[i]) currentMeasureIdx = i else break
            }
            val curMStart = measureStartTimes[currentMeasureIdx]
            val curMDur = measureDurations[currentMeasureIdx].coerceAtLeast(0.001f)
            val playheadOffsetInMeasure = ((currentTimeSec - curMStart) / curMDur).coerceIn(0f, 1f)

            java.util.Arrays.fill(curActiveKeys, false)
            for (fn in flatNotes) {
                if (fn.startSec > currentTimeSec + 0.02f) break
                if (currentTimeSec >= fn.startSec - 0.01f && currentTimeSec <= fn.endSec) {
                    curActiveKeys[fn.note.midiNote.coerceIn(0, 127)] = true
                }
            }

            if (renderNotation && notationHeight > 0f) {
                val measuresPerRow = if (renderVisualizer) 2 else 3
                val rowStartMeasure = (currentMeasureIdx / measuresPerRow) * measuresPerRow
                val leftMargin = 32f * scale
                val rightMargin = 32f * scale
                val systemWidth = width - leftMargin - rightMargin
                val measureW = systemWidth / measuresPerRow

                val staffSpacing = when {
                    showStaffInNotation && showTabInNotation -> (notationHeight * 0.035f).coerceIn(10f * scale, 16f * scale)
                    else -> (notationHeight * 0.052f).coerceIn(12f * scale, 21f * scale)
                }
                val halfStaffSpacing = staffSpacing * 0.5f
                val trebleTop = 42f * scale
                val bassTop = trebleTop + staffSpacing * 6.0f
                val tabTop = if (showStaffInNotation) bassTop + staffSpacing * 5.4f else 48f * scale
                val tabSpacing = staffSpacing * 1.05f

                for (mSlot in 0 until measuresPerRow) {
                    val mIdx = rowStartMeasure + mSlot
                    if (mIdx >= totalMeasures) break
                    val measure = score.measures[mIdx]
                    val mStartX = leftMargin + mSlot * measureW
                    val mEndX = mStartX + measureW
                    val isFirstInRow = (mSlot == 0)
                    val isCurrentActiveMeasure = (mIdx == currentMeasureIdx)
                    val beatsPerM = measureQuarterBeats[mIdx]

                    frameCanvas.drawText(
                        "${mIdx + 1}",
                        mStartX + 4f * scale,
                        (if (showStaffInNotation) trebleTop else tabTop) - staffSpacing * 0.8f,
                        measureNumPaint
                    )

                    val headerOffset = if (isFirstInRow) 78f * scale else 18f * scale
                    val notesStartX = mStartX + headerOffset
                    val usableW = (mEndX - notesStartX - 20f * scale).coerceAtLeast(60f * scale)

                    val visibleMeasureNotes = measure.notes.filter { n ->
                        !n.isRest && (enabledTrackIndices[n.voice] ?: true) &&
                            !(if (n.midiNote >= 60) isRightHandHidden else isLeftHandHidden)
                    }.sortedBy { it.position }

                    val distinctPos = visibleMeasureNotes.map { it.position }.distinct().sorted()
                    val posToX = HashMap<Float, Float>()
                    if (distinctPos.isNotEmpty()) {
                        val minDur = visibleMeasureNotes.minOfOrNull { it.duration.coerceAtLeast(0.125f) } ?: 0.5f
                        val weights = FloatArray(distinctPos.size)
                        var sumW = 0f
                        for (i in distinctPos.indices) {
                            val nextP = if (i < distinctPos.lastIndex) distinctPos[i + 1] else beatsPerM
                            val delta = (nextP - distinctPos[i]).coerceAtLeast(minDur)
                            val w = (delta / minDur).toDouble().pow(0.52).toFloat()
                            weights[i] = w
                            sumW += w
                        }
                        var runW = 0f
                        var prevX = notesStartX
                        val minGap = 22f * scale
                        for (i in distinctPos.indices) {
                            val frac = (runW / sumW.coerceAtLeast(1f)).coerceIn(0f, 0.95f)
                            val idealX = notesStartX + frac * usableW
                            val clampedX = (if (i == 0) idealX else maxOf(idealX, prevX + minGap)).coerceIn(notesStartX, mEndX - 18f * scale)
                            posToX[distinctPos[i]] = clampedX
                            prevX = clampedX
                            runW += weights[i]
                        }
                    }

                    if (showStaffInNotation) {
                        for (lineIdx in 0..4) {
                            if (!isRightHandHidden) frameCanvas.drawLine(mStartX, trebleTop + lineIdx * staffSpacing, mEndX, trebleTop + lineIdx * staffSpacing, staffLinePaint)
                            if (!isLeftHandHidden) frameCanvas.drawLine(mStartX, bassTop + lineIdx * staffSpacing, mEndX, bassTop + lineIdx * staffSpacing, staffLinePaint)
                        }

                        val sysTop = if (isRightHandHidden) bassTop else trebleTop
                        val sysBottom = if (isLeftHandHidden) trebleTop + 4f * staffSpacing else bassTop + 4f * staffSpacing
                        if (isFirstInRow) {
                            frameCanvas.drawLine(mStartX, sysTop, mStartX, sysBottom, barlinePaint)
                            clefSymbolPaint.textSize = staffSpacing * 3.8f
                            if (!isRightHandHidden) frameCanvas.drawText("𝄞", mStartX + 8f * scale, trebleTop + staffSpacing * 3.3f, clefSymbolPaint)
                            if (!isLeftHandHidden) {
                                clefSymbolPaint.textSize = staffSpacing * 2.9f
                                frameCanvas.drawText("𝄢", mStartX + 8f * scale, bassTop + staffSpacing * 2.7f, clefSymbolPaint)
                            }
                            clefSymbolPaint.textSize = staffSpacing * 1.9f
                            val tsX = mStartX + 44f * scale
                            if (!isRightHandHidden) {
                                frameCanvas.drawText("${measure.timeSignatureNumerator}", tsX, trebleTop + staffSpacing * 1.8f, clefSymbolPaint)
                                frameCanvas.drawText("${measure.timeSignatureDenominator}", tsX, trebleTop + staffSpacing * 3.8f, clefSymbolPaint)
                            }
                            if (!isLeftHandHidden) {
                                frameCanvas.drawText("${measure.timeSignatureNumerator}", tsX, bassTop + staffSpacing * 1.8f, clefSymbolPaint)
                                frameCanvas.drawText("${measure.timeSignatureDenominator}", tsX, bassTop + staffSpacing * 3.8f, clefSymbolPaint)
                            }
                        }

                        frameCanvas.drawLine(mEndX, sysTop, mEndX, sysBottom, if (mIdx == totalMeasures - 1) heavyBarlinePaint else barlinePaint)

                        val hasVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }
                        val currentBeat = playheadOffsetInMeasure * beatsPerM
                        val rx = staffSpacing * 0.62f
                        val ry = staffSpacing * 0.44f
                        val stemLen = staffSpacing * 3.2f

                        data class RenderedNoteHead(val note: ScoreNote, val nx: Float, val ny: Float, val isTreble: Boolean, val color: Int)
                        val renderedHeads = ArrayList<RenderedNoteHead>()

                        for (note in visibleMeasureNotes) {
                            val nx = posToX[note.position] ?: (notesStartX + (note.position / beatsPerM) * usableW)
                            val isTreble = if (hasVoices) note.voice == 0 else note.midiNote >= 60
                            val step = ChordVoicingEngine.getDiatonicStep(note.midiNote)
                            val ny = if (isTreble) trebleTop + (38f - step) * halfStaffSpacing else bassTop + (26f - step) * halfStaffSpacing

                            val sTop = if (isTreble) trebleTop else bassTop
                            val sBot = sTop + 4f * staffSpacing
                            val ledExt = staffSpacing * 0.85f
                            if (ny < sTop - halfStaffSpacing * 0.8f) {
                                var ly = sTop - staffSpacing
                                while (ly >= ny - halfStaffSpacing * 0.6f) {
                                    frameCanvas.drawLine(nx - ledExt, ly, nx + ledExt, ly, staffLinePaint)
                                    ly -= staffSpacing
                                }
                            } else if (ny > sBot + halfStaffSpacing * 0.8f) {
                                var ly = sBot + staffSpacing
                                while (ly <= ny + halfStaffSpacing * 0.6f) {
                                    frameCanvas.drawLine(nx - ledExt, ly, nx + ledExt, ly, staffLinePaint)
                                    ly += staffSpacing
                                }
                            }

                            val isSoundingNow = isCurrentActiveMeasure &&
                                currentBeat >= (note.position - 0.06f) &&
                                currentBeat <= (note.position + note.duration.coerceAtLeast(0.125f) + 0.04f)
                            val baseColor = if (isSoundingNow) accentGold else if (isTreble) rhColor else lhColor

                            if (isSoundingNow && glowEnabled) {
                                glowCirclePaint.color = accentGold
                                glowCirclePaint.alpha = 80
                                frameCanvas.drawCircle(nx, ny, rx * 1.9f, glowCirclePaint)
                            }

                            reusableRect.set(nx - rx, ny - ry, nx + rx, ny + ry)
                            if (note.duration >= 2.0f) {
                                noteStrokePaint.color = baseColor
                                frameCanvas.drawOval(reusableRect, noteStrokePaint)
                            } else {
                                noteFillPaint.color = baseColor
                                frameCanvas.drawOval(reusableRect, noteFillPaint)
                            }

                            if (note.dotted || note.isDotted) {
                                noteFillPaint.color = baseColor
                                frameCanvas.drawCircle(nx + rx + 4f * scale, ny - 2f * scale, 2.5f * scale, noteFillPaint)
                            }

                            val pc = ((note.midiNote % 12) + 12) % 12
                            if (pc in intArrayOf(1, 3, 6, 8, 10) || note.accidental != Accidental.NATURAL) {
                                val accStr = when (note.accidental) {
                                    Accidental.FLAT -> "♭"
                                    Accidental.DOUBLE_SHARP -> "𝄪"
                                    Accidental.DOUBLE_FLAT -> "𝄫"
                                    else -> "♯"
                                }
                                accidentalPaint.textSize = staffSpacing * 1.15f
                                frameCanvas.drawText(accStr, nx - rx - 3f * scale, ny + staffSpacing * 0.38f, accidentalPaint)
                            }

                            renderedHeads.add(RenderedNoteHead(note, nx, ny, isTreble, baseColor))
                        }

                        listOf(true, false).forEach { trebleGroup ->
                            val handHeads = renderedHeads.filter { it.isTreble == trebleGroup && it.note.duration < 4.0f }
                            val byPos = handHeads.groupBy { it.note.position }.entries.sortedBy { it.key }
                            val staffMidY = (if (trebleGroup) trebleTop else bassTop) + 2f * staffSpacing

                            data class StemInfo(val x: Float, val tipY: Float, val baseY: Float, val stemUp: Boolean, val dur: Float, val pos: Float, val color: Int)
                            val stems = ArrayList<StemInfo>()

                            for ((pos, chordGroup) in byPos) {
                                val minY = chordGroup.minOf { it.ny }
                                val maxY = chordGroup.maxOf { it.ny }
                                val avgY = (minY + maxY) * 0.5f
                                val stemUp = avgY >= staffMidY
                                val sx = if (stemUp) chordGroup.first().nx + rx * 0.88f else chordGroup.first().nx - rx * 0.88f
                                val baseY = if (stemUp) maxY else minY
                                val tipY = if (stemUp) minY - stemLen else maxY + stemLen
                                val col = chordGroup.first().color

                                stemPaint.color = col
                                frameCanvas.drawLine(sx, baseY, sx, tipY, stemPaint)
                                stems.add(StemInfo(sx, tipY, baseY, stemUp, chordGroup.minOf { it.note.duration }, pos, col))
                            }

                            var i = 0
                            while (i < stems.size) {
                                val first = stems[i]
                                if (first.dur <= 0.55f) {
                                    val beatBucket = first.pos.toInt()
                                    val beamRun = mutableListOf(first)
                                    var j = i + 1
                                    while (j < stems.size && stems[j].dur <= 0.55f && stems[j].pos.toInt() == beatBucket && stems[j].stemUp == first.stemUp) {
                                        beamRun.add(stems[j])
                                        j++
                                    }
                                    if (beamRun.size >= 2) {
                                        val sStart = beamRun.first()
                                        val sEnd = beamRun.last()
                                        val bThick = staffSpacing * 0.42f
                                        val dir = if (sStart.stemUp) 1f else -1f
                                        beamPaint.color = sStart.color
                                        beamPath.reset()
                                        beamPath.moveTo(sStart.x, sStart.tipY)
                                        beamPath.lineTo(sEnd.x, sEnd.tipY)
                                        beamPath.lineTo(sEnd.x, sEnd.tipY + dir * bThick)
                                        beamPath.lineTo(sStart.x, sStart.tipY + dir * bThick)
                                        beamPath.close()
                                        frameCanvas.drawPath(beamPath, beamPaint)
                                        i = j
                                        continue
                                    } else {
                                        val dir = if (first.stemUp) 1f else -1f
                                        curvePaint.color = first.color
                                        slurPath.reset()
                                        slurPath.moveTo(first.x, first.tipY)
                                        slurPath.cubicTo(
                                            first.x + rx * 1.4f, first.tipY + dir * staffSpacing * 0.6f,
                                            first.x + rx * 1.2f, first.tipY + dir * staffSpacing * 1.4f,
                                            first.x + rx * 0.4f, first.tipY + dir * staffSpacing * 2.0f
                                        )
                                        frameCanvas.drawPath(slurPath, curvePaint)
                                    }
                                }
                                i++
                            }
                        }
                    }

                    if (showTabInNotation) {
                        for (sIdx in 0..5) {
                            frameCanvas.drawLine(mStartX, tabTop + sIdx * tabSpacing, mEndX, tabTop + sIdx * tabSpacing, staffLinePaint)
                        }
                        val tabBot = tabTop + 5f * tabSpacing
                        if (isFirstInRow) {
                            frameCanvas.drawLine(mStartX, tabTop, mStartX, tabBot, barlinePaint)
                            clefSymbolPaint.textSize = tabSpacing * 1.35f
                            frameCanvas.drawText("T", mStartX + 10f * scale, tabTop + tabSpacing * 1.5f, clefSymbolPaint)
                            frameCanvas.drawText("A", mStartX + 10f * scale, tabTop + tabSpacing * 3.1f, clefSymbolPaint)
                            frameCanvas.drawText("B", mStartX + 10f * scale, tabTop + tabSpacing * 4.7f, clefSymbolPaint)
                        }
                        frameCanvas.drawLine(mEndX, tabTop, mEndX, tabBot, if (mIdx == totalMeasures - 1) heavyBarlinePaint else barlinePaint)

                        val byPos = visibleMeasureNotes.groupBy { it.position }
                        for ((pos, notesAtPos) in byPos) {
                            val nx = posToX[pos] ?: (notesStartX + (pos / beatsPerM) * usableW)
                            val usedStrings = HashSet<Int>()
                            for (note in notesAtPos.sortedByDescending { it.midiNote }) {
                                val strIdx = if (note.string != null && note.string in 0..5 && note.string !in usedStrings) {
                                    note.string!!
                                } else {
                                    (0..5).filter { it !in usedStrings }.minByOrNull { s ->
                                        val f = note.midiNote - GUITAR_OPEN_MIDI[s]
                                        if (f in 0..22) f else 1000 + abs(f)
                                    } ?: 0
                                }
                                usedStrings.add(strIdx)
                                val fretVal = note.fret ?: (note.midiNote - GUITAR_OPEN_MIDI[strIdx]).coerceIn(0, 24)
                                val ny = tabTop + strIdx * tabSpacing
                                val currentBeat = playheadOffsetInMeasure * beatsPerM
                                val isSoundingNow = isCurrentActiveMeasure &&
                                    currentBeat >= (note.position - 0.06f) &&
                                    currentBeat <= (note.position + note.duration.coerceAtLeast(0.125f) + 0.04f)

                                val fText = if (note.articulation == TabArticulation.DEAD_NOTE) "X" else fretVal.toString()
                                val pw = maxOf(tabSpacing * 0.9f, (fText.length * 8f + 8f) * scale)
                                val ph = tabSpacing * 0.85f
                                reusableRect.set(nx - pw * 0.5f, ny - ph * 0.5f, nx + pw * 0.5f, ny + ph * 0.5f)
                                tabPillBgPaint.color = if (isSoundingNow) accentGold else Color.parseColor("#252528")
                                frameCanvas.drawRoundRect(reusableRect, ph * 0.5f, ph * 0.5f, tabPillBgPaint)

                                tabFretTextPaint.color = if (isSoundingNow) Color.BLACK else Color.WHITE
                                tabFretTextPaint.textSize = tabSpacing * 0.72f
                                frameCanvas.drawText(fText, nx, ny + tabFretTextPaint.textSize * 0.35f, tabFretTextPaint)
                            }
                        }
                    }

                    if (isCurrentActiveMeasure) {
                        val currentBeat = playheadOffsetInMeasure * beatsPerM
                        var lowerP = -1f
                        var lowerX = -1f
                        var higherP = Float.MAX_VALUE
                        var higherX = -1f
                        for ((p, x) in posToX) {
                            if (p <= currentBeat && p > lowerP) {
                                lowerP = p; lowerX = x
                            }
                            if (p > currentBeat && p < higherP) {
                                higherP = p; higherX = x
                            }
                        }
                        val phX = when {
                            lowerX != -1f && higherX != -1f -> {
                                val f = ((currentBeat - lowerP) / (higherP - lowerP).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                                lowerX + (higherX - lowerX) * f
                            }
                            lowerX != -1f -> lowerX + ((currentBeat - lowerP) / beatsPerM.coerceAtLeast(1f)) * (mEndX - lowerX)
                            else -> notesStartX + playheadOffsetInMeasure * usableW
                        }.coerceIn(mStartX + 4f, mEndX - 4f)

                        val phTop = (if (showStaffInNotation) trebleTop else tabTop) - staffSpacing
                        val phBot = (if (showTabInNotation) tabTop + 5f * tabSpacing else bassTop + 4f * staffSpacing) + staffSpacing
                        frameCanvas.drawLine(phX, phTop, phX, phBot, playheadPaint)
                    }
                }

                if (renderVisualizer) {
                    frameCanvas.drawLine(0f, visualizerTop, width.toFloat(), visualizerTop, barlinePaint)
                }
            }

            if (renderVisualizer && visualizerHeight > 0f) {
                val waterfallTop = visualizerTop
                val contactY = keyboardTop

                for (k in 0..52) {
                    val gx = k * whiteKeyWidth
                    frameCanvas.drawLine(gx, waterfallTop, gx, contactY, laneGridPaint)
                }

                val lookaheadSec = 2.5f
                val pixelsPerSec = waterfallHeight / lookaheadSec
                val pitchTiltFactor = (cameraPitchDeg / 90f).coerceIn(0f, 0.45f)
                val yawShiftPx = (cameraYawDeg / 45f) * (width * 0.06f)
                val pillCorner = 8f * scale

                for (fn in flatNotes) {
                    if (fn.endSec < currentTimeSec - 0.05f) continue
                    if (fn.startSec > currentTimeSec + lookaheadSec) break

                    val p = fn.note.midiNote.coerceIn(21, 108)
                    val noteDistSec = fn.startSec - currentTimeSec
                    val rawBottomY = contactY - (noteDistSec * pixelsPerSec)
                    val rawTopY = rawBottomY - (fn.durationSec * pixelsPerSec)

                    val clampedTopY = rawTopY.coerceAtLeast(waterfallTop)
                    val isCurrentlySounding = currentTimeSec in fn.startSec..fn.endSec
                    val clampedBottomY = (if (isCurrentlySounding && rawBottomY < contactY) contactY else rawBottomY).coerceAtMost(contactY)

                    if (clampedBottomY <= waterfallTop || clampedTopY >= contactY) continue

                    val baseCenterX = keyXCenter[p]
                    val baseW = keyBoxWidth[p]
                    val tMid = ((clampedTopY + clampedBottomY) * 0.5f - waterfallTop) / waterfallHeight.coerceAtLeast(1f)
                    val perspScale = (1f - pitchTiltFactor) + pitchTiltFactor * tMid
                    val centerX = (width * 0.5f) + (baseCenterX - width * 0.5f) * perspScale + yawShiftPx * (1f - tMid)
                    val pillW = baseW * perspScale
                    val pillLeft = centerX - pillW * 0.5f
                    val pillRight = centerX + pillW * 0.5f
                    val pillH = (clampedBottomY - clampedTopY).coerceAtLeast(14f * scale)
                    val voiceIdx = fn.note.voice
                    val trackObj = score.tracks.firstOrNull { it.trackIndex == voiceIdx }
                    val noteColor = when {
                        trackObj?.channel == 9 -> Color.parseColor("#FF5252") // Drum Kit Crimson
                        voiceIdx == 0 -> rhColor
                        voiceIdx == 1 -> lhColor
                        else -> com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX[
                            voiceIdx.coerceAtLeast(0) % com.creativeblisslabs.peqlearn.app.util.MidiScoreParser.TRACK_COLOR_PALETTE_HEX.size
                        ].toInt()
                    }
                    if (glowEnabled && isCurrentlySounding) {
                        noteFillPaint.color = noteColor
                        noteFillPaint.alpha = 85
                        reusableRect.set(pillLeft - 3.5f * scale, clampedTopY - 3.5f * scale, pillRight + 3.5f * scale, clampedBottomY + 3.5f * scale)
                        frameCanvas.drawRoundRect(reusableRect, pillCorner + 3.5f * scale, pillCorner + 3.5f * scale, noteFillPaint)
                    }

                    noteFillPaint.color = noteColor
                    noteFillPaint.alpha = if (isCurrentlySounding) 255 else 230
                    reusableRect.set(pillLeft, clampedTopY, pillRight, clampedBottomY)
                    frameCanvas.drawRoundRect(reusableRect, pillCorner, pillCorner, noteFillPaint)

                    noteStrokePaint.color = Color.WHITE
                    noteStrokePaint.alpha = if (isCurrentlySounding) 170 else 65
                    noteStrokePaint.strokeWidth = 1.2f * scale
                    frameCanvas.drawRoundRect(reusableRect, pillCorner, pillCorner, noteStrokePaint)

                    if (pillH >= 16f * scale) {
                        val pitchName = NOTE_NAMES[p % 12]
                        pillLabelPaint.textSize = (10.5f * scale).coerceAtMost(pillW * 0.65f)
                        val textY = (clampedTopY + clampedBottomY) * 0.5f + pillLabelPaint.textSize * 0.36f
                        frameCanvas.drawText(pitchName, centerX, textY, pillLabelPaint)
                    }
                }

                val activeCountNow = curActiveKeys.count { it }
                val isChordHit = activeCountNow >= 2
                for (midi in 21..108) {
                    if (curActiveKeys[midi] && !prevActiveKeys[midi]) {
                        val sx = keyXCenter[midi]
                        val sy = contactY
                        val c = if (midi >= 60) rhColor else lhColor
                        if (glowEnabled && flashes.size < 32) {
                            flashes.add(
                                ExportFlash(
                                    x = sx, y = sy,
                                    radius = (if (keyIsBlack[midi]) 28f else 38f) * scale,
                                    alpha = 0.95f, color = if (isChordHit) accentGold else c, isChord = isChordHit
                                )
                            )
                        }
                        if (particlesEnabled && sparks.size < 96) {
                            val count = if (keyIsBlack[midi]) 7 else 10
                            for (s in 0 until count) {
                                val angleRad = (-(30f + Random.nextFloat() * 120f) * (Math.PI / 180f)).toFloat()
                                val spd = (Random.nextFloat() * 4.5f + 2.0f) * scale
                                val life = Random.nextFloat() * 18f + 14f
                                sparks.add(
                                    ExportSpark(
                                        x = sx + (Random.nextFloat() - 0.5f) * 8f * scale, y = sy,
                                        vx = cos(angleRad) * spd, vy = sin(angleRad) * spd,
                                        size = (Random.nextFloat() * 2.2f + 1.6f) * scale,
                                        life = life, maxLife = life, color = c
                                    )
                                )
                            }
                        }
                    }
                    prevActiveKeys[midi] = curActiveKeys[midi]
                }

                val fIter = flashes.iterator()
                while (fIter.hasNext()) {
                    val f = fIter.next()
                    if (glowEnabled) {
                        glowCirclePaint.color = f.color
                        glowCirclePaint.alpha = (f.alpha * 140).toInt().coerceIn(0, 255)
                        frameCanvas.drawCircle(f.x, f.y, f.radius, glowCirclePaint)
                        glowCirclePaint.color = Color.WHITE
                        glowCirclePaint.alpha = (f.alpha * 215).toInt().coerceIn(0, 255)
                        frameCanvas.drawCircle(f.x, f.y, f.radius * 0.40f, glowCirclePaint)
                    }
                    f.radius += 1.8f * scale
                    f.alpha *= 0.88f
                    if (f.alpha < 0.03f) fIter.remove()
                }

                val sIter = sparks.iterator()
                val gravity = 0.28f * scale
                while (sIter.hasNext()) {
                    val sp = sIter.next()
                    val a = (sp.life / sp.maxLife).coerceIn(0f, 1f)
                    if (particlesEnabled) {
                        glowCirclePaint.color = sp.color
                        glowCirclePaint.alpha = (a * 215).toInt().coerceIn(0, 255)
                        frameCanvas.drawCircle(sp.x, sp.y, sp.size, glowCirclePaint)
                        stemPaint.color = Color.WHITE
                        stemPaint.alpha = (a * 180).toInt().coerceIn(0, 255)
                        frameCanvas.drawLine(sp.x, sp.y, sp.x - sp.vx * 1.4f, sp.y - sp.vy * 1.4f, stemPaint)
                        stemPaint.color = Color.parseColor("#EAEAEA")
                        stemPaint.alpha = 255
                    }
                    sp.x += sp.vx
                    sp.y += sp.vy
                    sp.vy += gravity
                    sp.vx *= 0.985f
                    sp.life -= 1f
                    if (sp.life <= 0f) sIter.remove()
                }

                val keyCorner = 4f * scale
                val frontLipH = keyboardHeight * 0.12f
                val topSurfaceBottom = keyboardBottom - frontLipH

                for (k in whiteNotes.indices) {
                    val midi = whiteNotes[k]
                    val kLeft = k * whiteKeyWidth + 1f
                    val kRight = (k + 1) * whiteKeyWidth - 1f
                    val isKeyActive = curActiveKeys[midi]
                    val handCol = if (midi >= 60) rhColor else lhColor

                    whiteKeyTopPaint.color = if (isKeyActive) handCol else Color.parseColor("#F7F4EC")
                    reusableRect.set(kLeft, keyboardTop, kRight, topSurfaceBottom)
                    frameCanvas.drawRoundRect(reusableRect, keyCorner, keyCorner, whiteKeyTopPaint)

                    whiteKeyLipPaint.color = if (isKeyActive) handCol else Color.parseColor("#D4C9B8")
                    if (isKeyActive) whiteKeyLipPaint.alpha = 210
                    reusableRect.set(kLeft, topSurfaceBottom - 1f, kRight, keyboardBottom)
                    frameCanvas.drawRoundRect(reusableRect, keyCorner, keyCorner, whiteKeyLipPaint)

                    reusableRect.set(kLeft, keyboardTop, kRight, keyboardBottom)
                    frameCanvas.drawRoundRect(reusableRect, keyCorner, keyCorner, whiteKeyBorderPaint)

                    val cx = (kLeft + kRight) * 0.5f
                    if (midi % 12 == 0) {
                        val oct = (midi / 12) - 1
                        keyTextPaint.color = Color.argb(170, 120, 125, 135)
                        keyTextPaint.textSize = 9f * scale
                        frameCanvas.drawText("C$oct", cx, keyboardBottom - 8f * scale, keyTextPaint)
                    }

                    if (isKeyActive) {
                        val badgeY = keyboardBottom - 20f * scale
                        val badgeR = 11.5f * scale
                        keyBadgePaint.color = Color.parseColor("#FFEB3B")
                        frameCanvas.drawCircle(cx, badgeY, badgeR, keyBadgePaint)

                        keyTextPaint.color = Color.rgb(190, 40, 0)
                        keyTextPaint.textSize = 10.5f * scale
                        frameCanvas.drawText(NOTE_NAMES[midi % 12], cx, badgeY + 3.5f * scale, keyTextPaint)
                    }
                }

                val blackKeyW = whiteKeyWidth * 0.58f
                val blackKeyH = keyboardHeight * 0.62f
                val blackKeyBot = keyboardTop + blackKeyH

                for (k in whiteNotes.indices) {
                    val midi = whiteNotes[k]
                    if ((midi % 12) in intArrayOf(0, 2, 5, 7, 9)) {
                        val blackMidi = midi + 1
                        if (blackMidi <= 108) {
                            val bkLeft = (k + 1) * whiteKeyWidth - blackKeyW * 0.5f
                            val bkRight = bkLeft + blackKeyW
                            val isBlackActive = curActiveKeys[blackMidi]
                            val handCol = if (blackMidi >= 60) rhColor else lhColor

                            reusableRect.set(bkLeft + 1.5f * scale, keyboardTop + 2f * scale, bkRight + 1.5f * scale, blackKeyBot + 2f * scale)
                            frameCanvas.drawRoundRect(reusableRect, keyCorner, keyCorner, blackKeyShadowPaint)

                            blackKeyBodyPaint.color = if (isBlackActive) handCol else Color.parseColor("#1C1C1E")
                            reusableRect.set(bkLeft, keyboardTop, bkRight, blackKeyBot)
                            frameCanvas.drawRoundRect(reusableRect, keyCorner, keyCorner, blackKeyBodyPaint)

                            if (isBlackActive) {
                                val cx = (bkLeft + bkRight) * 0.5f
                                val badgeY = blackKeyBot - 16f * scale
                                val badgeR = 9.5f * scale
                                keyBadgePaint.color = Color.parseColor("#FFEB3B")
                                frameCanvas.drawCircle(cx, badgeY, badgeR, keyBadgePaint)

                                keyTextPaint.color = Color.rgb(190, 40, 0)
                                keyTextPaint.textSize = 9f * scale
                                frameCanvas.drawText(NOTE_NAMES[blackMidi % 12], cx, badgeY + 3f * scale, keyTextPaint)
                            }
                        }
                    }
                }

                noteFillPaint.color = Color.parseColor("#990012")
                frameCanvas.drawRect(0f, keyboardTop, width.toFloat(), keyboardTop + 4f * scale, noteFillPaint)
            }
        }
    }
}