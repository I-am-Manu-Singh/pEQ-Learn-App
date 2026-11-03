package com.creativeblisslabs.peqlearn.app.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import com.creativeblisslabs.peqlearn.app.ui.screens.Accidental
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote
import com.creativeblisslabs.peqlearn.app.ui.screens.TabArticulation
import com.creativeblisslabs.peqlearn.app.util.ChordVoicingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Studio-Grade Hardware-Accelerated MP4 Video & Audio Renderer for pEQ Learn.
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

    private data class ExportFlatNote(
        val note: ScoreNote, val measureIndex: Int, val startSec: Float,
        val durationSec: Float, val endSec: Float, val isRightHand: Boolean, val isChord: Boolean
    )

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
        onProgress: (Float) -> Unit,
        onComplete: (String) -> Unit
    ) {
        withContext(Dispatchers.Default) {
            val (width, height) = when (resolution.lowercase()) {
                "4k", "2160p" -> Pair(3840, 2160)
                "1440p", "2k" -> Pair(2560, 1440)
                "720p" -> Pair(1280, 720)
                else -> Pair(1920, 1080)
            }
            val scale = width / 1920f
            val targetFps = fps.coerceIn(24, 120)
            val totalMeasures = score.measures.size.coerceAtLeast(1)
            val bpm = score.tempo.coerceAtLeast(30f)

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

            val tailSec = 2.0f
            val totalDurationSec = (accTimeSec + tailSec).coerceAtLeast(3.0f)
            val totalFrames = (totalDurationSec * targetFps).toInt().coerceIn(60, 36000)

            val cleanTitle = score.title.ifBlank { "pEQ_Composition" }.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
            val videoFileName = "pEQ_Render_${cleanTitle}_${System.currentTimeMillis()}.mp4"

            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/pEQ_Visualizer")
            }

            val resolver = context?.contentResolver
            val videoUri = resolver?.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

            val outputFile = if (videoUri != null) {
                File(context.cacheDir, videoFileName)
            } else {
                val exportDir = File(context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "pEQ_Visualizer")
                if (!exportDir.exists()) exportDir.mkdirs()
                File(exportDir, videoFileName)
            }

            val normLayout = layoutMode.trim().lowercase()
            val renderNotation = when {
                normLayout.contains("notation") || normLayout.contains("score") || normLayout.contains("split") -> true
                normLayout.contains("waterfall") -> false
                else -> effects["Notation"] ?: true
            }
            val renderVisualizer = when {
                normLayout == "notation" || normLayout == "notation only" -> false
                else -> effects["Visualizer"] ?: true
            }

            val normFmt = notationFormat.trim().lowercase()
            val showStaffInNotation = normFmt != "tab" && normFmt != "tab only"
            val showTabInNotation = normFmt == "tab" || normFmt == "tab only" || normFmt == "both" || normFmt == "both viewers"

            val glowEnabled = effects["Glow"] ?: true
            val particlesEnabled = effects["Particles"] ?: true
            val renderPedals = effects["RenderPedals"] ?: true

            val rhColor = rightHandColorInt ?: when (visualTheme.lowercase()) {
                "cyberpunk" -> Color.parseColor("#00E5FF")
                "neon fall" -> Color.parseColor("#00E676")
                else -> Color.parseColor("#4A90E2")
            }
            val lhColor = leftHandColorInt ?: when (visualTheme.lowercase()) {
                "cyberpunk" -> Color.parseColor("#FF4081")
                "neon fall" -> Color.parseColor("#D500F9")
                else -> Color.parseColor("#6C63FF")
            }
            val bgTopColor = when (visualTheme.lowercase()) {
                "cyberpunk" -> Color.parseColor("#08090F")
                "neon fall" -> Color.parseColor("#050B0A")
                else -> Color.parseColor("#0D0B14")
            }
            val bgBottomColor = Color.parseColor("#050508")
            val accentGold = Color.parseColor("#FFD700")

            val bottomSafetyMargin = 6f * scale
            val notationHeight = if (renderNotation && renderVisualizer) {
                if (showStaffInNotation && showTabInNotation) height * 0.48f else height * 0.43f
            } else if (renderNotation) {
                height.toFloat()
            } else {
                0f
            }
            val visualizerTop = if (renderNotation && renderVisualizer) notationHeight else 0f
            val visualizerHeight = (height - visualizerTop).coerceAtLeast(0f)

            val keyboardHeight = if (renderVisualizer) (visualizerHeight * 0.25f).coerceIn(130f * scale, 240f * scale) else 0f
            val keyboardTop = height - keyboardHeight - bottomSafetyMargin
            val keyboardBottom = height - bottomSafetyMargin
            val waterfallHeight = (keyboardTop - visualizerTop).coerceAtLeast(0f)

            val flatNotes = ArrayList<ExportFlatNote>()
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
                            note = note, measureIndex = mIdx, startSec = nStart,
                            durationSec = nDur, endSec = nStart + nDur, isRightHand = isRh, isChord = isChord
                        )
                    )
                }
            }
            flatNotes.sortBy { it.startSec }

            val sampleRate = sampleRateHz.coerceIn(44100, 48000)
            val totalAudioSamples = (totalDurationSec * sampleRate).toInt()
            val audioPcm = FloatArray(totalAudioSamples)

            for (fn in flatNotes) {
                val pitch = fn.note.midiNote.coerceIn(21, 108)
                val startSample = (fn.startSec * sampleRate).toInt().coerceIn(0, totalAudioSamples - 1)
                val ringSamples = ((fn.durationSec + 0.35f) * sampleRate).toInt()
                val freq = 440.0 * 2.0.pow((pitch - 69) / 12.0)

                for (s in 0 until ringSamples) {
                    val idx = startSample + s
                    if (idx >= totalAudioSamples) break
                    val t = s.toFloat() / sampleRate
                    val releaseFade = if (t > fn.durationSec) exp(-(t - fn.durationSec) * 14.0).toFloat() else 1.0f
                    val env = (exp(-t * 2.4) * min(1.0, t * 180.0)).toFloat() * releaseFade
                    val wave = (
                            sin(2.0 * Math.PI * freq * t) * 0.58 +
                                    sin(2.0 * Math.PI * freq * 2.0 * t) * 0.24 +
                                    sin(2.0 * Math.PI * freq * 3.0 * t) * 0.11 +
                                    sin(2.0 * Math.PI * freq * 4.0 * t) * 0.07
                            ).toFloat() * env * 0.38f
                    audioPcm[idx] = (audioPcm[idx] + wave).coerceIn(-0.98f, 0.98f)
                }
            }

            val pcmByteBuffer = ByteBuffer.allocate(totalAudioSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in audioPcm) {
                pcmByteBuffer.putShort((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
            }
            pcmByteBuffer.flip()

            val videoMime = MediaFormat.MIMETYPE_VIDEO_AVC
            val targetBitrate = bitrateMbps.coerceIn(4, 40) * 1_000_000
            val videoFormat = MediaFormat.createVideoFormat(videoMime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 160_000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }

            var videoEncoder: MediaCodec? = null
            var audioEncoder: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var videoTrackIdx = -1
            var audioTrackIdx = -1
            var muxerStarted = false
            var inputSurface: android.view.Surface? = null

            try {
                videoEncoder = MediaCodec.createEncoderByType(videoMime).apply {
                    configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    inputSurface = createInputSurface()
                    start()
                }
                audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                    configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    start()
                }
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } catch (_: Exception) {
                videoEncoder = null
            }

            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val srcRect = Rect(0, 0, width, height)
            val dstRect = Rect(0, 0, width, height)

            val bgPaint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, height.toFloat(), bgTopColor, bgBottomColor, Shader.TileMode.CLAMP)
            }
            val staffLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#759E9E9E")
                strokeWidth = 1.4f * scale
            }
            val barlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0CCCCCC")
                strokeWidth = 1.8f * scale
            }
            val heavyBarlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E0EAEAEA")
                strokeWidth = 4.0f * scale
            }
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EAEAEA")
                strokeWidth = 1.6f * scale
            }
            val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EAEAEA")
                strokeWidth = 4.2f * scale
                strokeCap = Paint.Cap.BUTT
            }
            val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D8EAEAEA")
                style = Paint.Style.STROKE
                strokeWidth = 2.0f * scale
            }
            val noteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val noteStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.0f * scale
            }
            val glowCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 22f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0FFFFFF")
                textSize = 14f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val measureNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentGold
                alpha = 180
                textSize = 12f * scale
                typeface = Typeface.DEFAULT_BOLD
            }
            val clefSymbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EAEAEA")
                typeface = Typeface.SERIF
            }
            val accidentalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EAEAEA")
                textSize = 15f * scale
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val tabPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#252528")
                style = Paint.Style.FILL
            }
            val tabFretTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11f * scale
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentGold
                strokeWidth = 3.0f * scale
            }
            val laneGridPaint = Paint().apply {
                color = Color.parseColor("#18FFFFFF")
                strokeWidth = 1f
            }
            val whiteKeyTopPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val whiteKeyLipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val whiteKeyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#661E222B")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * scale
            }
            val blackKeyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#77000000")
            }
            val blackKeyBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val keyBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val pillLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 12f * scale
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                setShadowLayer(3f * scale, 0f, 1f * scale, Color.argb(190, 0, 0, 0))
            }

            val whiteNotes = (21..108).filter { n -> (n % 12) !in intArrayOf(1, 3, 6, 8, 10) }
            val whiteKeyWidth = width / 52f
            val keyXCenter = FloatArray(128)
            val keyBoxWidth = FloatArray(128)
            val keyIsBlack = BooleanArray(128)
            for (midi in 21..108) {
                val isB = (midi % 12) in intArrayOf(1, 3, 6, 8, 10)
                keyIsBlack[midi] = isB
                if (!isB) {
                    val wIdx = whiteNotes.indexOf(midi).coerceAtLeast(0)
                    keyXCenter[midi] = wIdx * whiteKeyWidth + whiteKeyWidth * 0.5f
                    keyBoxWidth[midi] = whiteKeyWidth * 0.84f
                } else {
                    val prevW = whiteNotes.indexOf(midi - 1).coerceAtLeast(0)
                    keyXCenter[midi] = (prevW + 1) * whiteKeyWidth
                    keyBoxWidth[midi] = whiteKeyWidth * 0.58f
                }
            }

            val sparks = ArrayList<ExportSpark>(96)
            val flashes = ArrayList<ExportFlash>(32)
            val prevActiveKeys = BooleanArray(128)
            val curActiveKeys = BooleanArray(128)
            val reusableRect = RectF()
            val reusablePath = Path()

            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()
            var pcmAudioQueuedBytes = 0
            val bytesPerSample = 2
            var audioEosSent = false

            fun drainEncoders(endOfStream: Boolean) {
                val vEnc = videoEncoder ?: return
                val aEnc = audioEncoder
                val mux = muxer ?: return

                if (endOfStream) {
                    runCatching { vEnc.signalEndOfInputStream() }
                }

                while (true) {
                    val outIdx = vEnc.dequeueOutputBuffer(videoBufferInfo, if (endOfStream) 5000L else 0L)
                    if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrackIdx = mux.addTrack(vEnc.outputFormat)
                            if (aEnc != null) {
                                val aIdx = aEnc.dequeueOutputBuffer(audioBufferInfo, 2000L)
                                if (aIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    audioTrackIdx = mux.addTrack(aEnc.outputFormat)
                                } else {
                                    audioTrackIdx = mux.addTrack(audioFormat)
                                    if (aIdx >= 0) aEnc.releaseOutputBuffer(aIdx, false)
                                }
                            }
                            mux.start()
                            muxerStarted = true
                        }
                    } else if (outIdx >= 0) {
                        val encodedData = vEnc.getOutputBuffer(outIdx)
                        if (encodedData != null && videoBufferInfo.size > 0 && muxerStarted && videoTrackIdx >= 0) {
                            encodedData.position(videoBufferInfo.offset)
                            encodedData.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            mux.writeSampleData(videoTrackIdx, encodedData, videoBufferInfo)
                        }
                        vEnc.releaseOutputBuffer(outIdx, false)
                        if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    } else {
                        break
                    }
                }

                if (aEnc != null) {
                    while (true) {
                        val aOutIdx = aEnc.dequeueOutputBuffer(audioBufferInfo, if (endOfStream) 3000L else 0L)
                        if (aOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (!muxerStarted && videoTrackIdx >= 0) {
                                audioTrackIdx = mux.addTrack(aEnc.outputFormat)
                                mux.start()
                                muxerStarted = true
                            }
                        } else if (aOutIdx >= 0) {
                            val encData = aEnc.getOutputBuffer(aOutIdx)
                            if (encData != null && audioBufferInfo.size > 0 && muxerStarted && audioTrackIdx >= 0) {
                                encData.position(audioBufferInfo.offset)
                                encData.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                mux.writeSampleData(audioTrackIdx, encData, audioBufferInfo)
                            }
                            aEnc.releaseOutputBuffer(aOutIdx, false)
                            if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                        } else {
                            break
                        }
                    }
                }
            }

            for (frameIndex in 0 until totalFrames) {
                val currentTimeSec = frameIndex.toFloat() / targetFps
                val progress = frameIndex.toFloat() / totalFrames

                frameCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                var currentMeasureIdx = 0
                for (i in 0 until totalMeasures) {
                    if (currentTimeSec >= measureStartTimes[i]) {
                        currentMeasureIdx = i
                    } else {
                        break
                    }
                }
                val curMStart = measureStartTimes[currentMeasureIdx]
                val curMDur = measureDurations[currentMeasureIdx].coerceAtLeast(0.001f)
                val playheadOffsetInMeasure = ((currentTimeSec - curMStart) / curMDur).coerceIn(0f, 1f)

                java.util.Arrays.fill(curActiveKeys, false)
                for (fn in flatNotes) {
                    if (fn.startSec > currentTimeSec + 0.02f) break
                    if (currentTimeSec >= fn.startSec - 0.01f && currentTimeSec <= fn.endSec) {
                        val p = fn.note.midiNote.coerceIn(0, 127)
                        curActiveKeys[p] = true
                    }
                }

                if (renderNotation && notationHeight > 0f) {
                    val headerY = 30f * scale
                    frameCanvas.drawText(score.title.ifBlank { "pEQ Learn Studio Score" }, 36f * scale, headerY, titlePaint)
                    frameCanvas.drawText("𝅘𝅥 = ${bpm.toInt()} BPM  •  ${score.timeSignature}  •  ${score.keySignature.keyName}", width - 36f * scale, headerY, metaPaint)

                    val measuresPerRow = if (renderVisualizer) 2 else 3
                    val rowStartMeasure = (currentMeasureIdx / measuresPerRow) * measuresPerRow
                    val leftMargin = 36f * scale
                    val rightMargin = 36f * scale
                    val systemWidth = width - leftMargin - rightMargin
                    val measureW = systemWidth / measuresPerRow

                    val staffSpacing = when {
                        showStaffInNotation && showTabInNotation -> (notationHeight * 0.032f).coerceIn(9f * scale, 15f * scale)
                        else -> (notationHeight * 0.045f).coerceIn(11f * scale, 19f * scale)
                    }
                    val halfStaffSpacing = staffSpacing * 0.5f
                    val trebleTop = headerY + (28f * scale)
                    val bassTop = trebleTop + staffSpacing * 5.6f
                    val tabTop = if (showStaffInNotation) bassTop + staffSpacing * 5.2f else headerY + (36f * scale)
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

                        frameCanvas.drawText("M.${mIdx + 1}", mStartX + 6f * scale, (if (showStaffInNotation) trebleTop else tabTop) - staffSpacing * 0.9f, measureNumPaint)

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

                            val noteCoords = HashMap<ScoreNote, Pair<Float, Float>>()
                            val hasVoices = measure.notes.any { it.voice == 0 } && measure.notes.any { it.voice == 1 }

                            for (note in visibleMeasureNotes) {
                                val nx = posToX[note.position] ?: (notesStartX + (note.position / beatsPerM) * usableW)
                                val isTreble = if (hasVoices) note.voice == 0 else note.midiNote >= 60
                                val step = ChordVoicingEngine.getDiatonicStep(note.midiNote)
                                val ny = if (isTreble) trebleTop + (38f - step) * halfStaffSpacing else bassTop + (26f - step) * halfStaffSpacing
                                noteCoords[note] = nx to ny

                                val sTop = if (isTreble) trebleTop else bassTop
                                val sBot = sTop + 4f * staffSpacing
                                val ledExt = staffSpacing * 0.85f
                                val lineSpacing = staffSpacing // or use your specific line spacing variable, e.g., staffLineSpacing
                                val halfSpacing = lineSpacing / 2f
                                if (ny < sTop - halfSpacing * 0.8f) {
                                    var ly = sTop - staffSpacing
                                    while (ly >= ny - halfSpacing * 0.6f) {
                                        frameCanvas.drawLine(nx - ledExt, ly, nx + ledExt, ly, staffLinePaint)
                                        ly -= staffSpacing
                                    }
                                } else if (ny > sBot + halfSpacing * 0.8f) {
                                    var ly = sBot + staffSpacing
                                    while (ly <= ny + halfSpacing * 0.6f) {
                                        frameCanvas.drawLine(nx - ledExt, ly, nx + ledExt, ly, staffLinePaint)
                                        ly += staffSpacing
                                    }
                                }

                                val currentBeat = playheadOffsetInMeasure * beatsPerM
                                val isSoundingNow = isCurrentActiveMeasure && currentBeat >= (note.position - 0.06f) && currentBeat <= (note.position + note.duration.coerceAtLeast(0.125f) + 0.04f)
                                val baseColor = if (isSoundingNow) accentGold else if (isTreble) rhColor else lhColor
                                val rx = staffSpacing * 0.62f
                                val ry = staffSpacing * 0.44f

                                if (isSoundingNow && glowEnabled) {
                                    glowCirclePaint.color = accentGold
                                    glowCirclePaint.alpha = 75
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
                                    val isSoundingNow = isCurrentActiveMeasure && currentBeat >= (note.position - 0.06f) && currentBeat <= (note.position + note.duration.coerceAtLeast(0.125f) + 0.04f)

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
                                    lowerP = p
                                    lowerX = x
                                }
                                if (p > currentBeat && p < higherP) {
                                    higherP = p
                                    higherX = x
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

                    val numEqBars = eqBarsCount.coerceIn(16, 128)
                    val eqBarWidth = width.toFloat() / numEqBars
                    val centerSample = (currentTimeSec * sampleRate).toInt().coerceIn(0, (totalAudioSamples - 1).coerceAtLeast(0))
                    val sensMul = (peakSensitivity / 75f).coerceIn(0.1f, 2.0f)

                    if (sensMul > 0.05f && totalAudioSamples > 0) {
                        for (b in 0 until numEqBars) {
                            val stride = (b % 12) + 1
                            var energySum = 0f
                            val windowSamples = 48
                            for (w in 0 until windowSamples) {
                                val sIdx = (centerSample + w * stride).coerceIn(0, totalAudioSamples - 1)
                                energySum += abs(audioPcm[sIdx])
                            }
                            val avgEnergy = (energySum / windowSamples) * sensMul * 2.2f
                            val eqH = (avgEnergy * waterfallHeight * 0.38f).coerceIn(0f, waterfallHeight * 0.45f)
                            if (eqH > 2f) {
                                noteFillPaint.color = if (b < numEqBars / 2) lhColor else rhColor
                                noteFillPaint.alpha = 55
                                frameCanvas.drawRect(b * eqBarWidth + 2f, contactY - eqH, (b + 1) * eqBarWidth - 2f, contactY, noteFillPaint)
                            }
                        }
                    }

                    val lookaheadSec = 2.6f
                    val pixelsPerSec = waterfallHeight / lookaheadSec
                    val pitchTiltFactor = (cameraPitchDeg / 90f).coerceIn(0f, 0.45f)
                    val yawShiftPx = (cameraYawDeg / 45f) * (width * 0.06f)
                    val pillCorner = 12f * scale

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
                        val pillH = (clampedBottomY - clampedTopY).coerceAtLeast(10f * scale)

                        val noteColor = if (fn.isRightHand) rhColor else lhColor

                        if (glowEnabled) {
                            noteFillPaint.color = noteColor
                            noteFillPaint.alpha = if (isCurrentlySounding) 95 else 55
                            reusableRect.set(pillLeft - 4f * scale, clampedTopY - 4f * scale, pillRight + 4f * scale, clampedBottomY + 4f * scale)
                            frameCanvas.drawRoundRect(reusableRect, pillCorner + 4f * scale, pillCorner + 4f * scale, noteFillPaint)
                        }

                        noteFillPaint.color = noteColor
                        noteFillPaint.alpha = if (isCurrentlySounding) 255 else 230
                        reusableRect.set(pillLeft, clampedTopY, pillRight, clampedBottomY)
                        frameCanvas.drawRoundRect(reusableRect, pillCorner, pillCorner, noteFillPaint)

                        noteFillPaint.color = Color.WHITE
                        noteFillPaint.alpha = 70
                        reusableRect.set(pillLeft + 1.5f * scale, clampedTopY + 1.5f * scale, pillRight - 1.5f * scale, clampedTopY + pillH * 0.45f)
                        frameCanvas.drawRoundRect(reusableRect, pillCorner - 1f, pillCorner - 1f, noteFillPaint)

                        noteStrokePaint.color = Color.WHITE
                        noteStrokePaint.alpha = if (isCurrentlySounding) 180 else 80
                        noteStrokePaint.strokeWidth = 1.3f * scale
                        reusableRect.set(pillLeft, clampedTopY, pillRight, clampedBottomY)
                        frameCanvas.drawRoundRect(reusableRect, pillCorner, pillCorner, noteStrokePaint)

                        if (pillH >= 18f * scale) {
                            val pitchName = NOTE_NAMES[p % 12]
                            pillLabelPaint.textSize = (11f * scale).coerceAtMost(pillW * 0.65f)
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
                                        radius = (if (keyIsBlack[midi]) 26f else 36f) * scale,
                                        alpha = 1.0f, color = if (isChordHit) accentGold else c, isChord = isChordHit
                                    )
                                )
                            }
                            if (particlesEnabled && sparks.size < 120) {
                                val count = if (keyIsBlack[midi]) 8 else 12
                                for (s in 0 until count) {
                                    val angleRad = (-(30f + Random.nextFloat() * 120f) * (Math.PI / 180f)).toFloat()
                                    val spd = (Random.nextFloat() * 4.5f + 2.2f) * scale
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
                            glowCirclePaint.alpha = (f.alpha * 210).toInt().coerceIn(0, 255)
                            frameCanvas.drawCircle(f.x, f.y, f.radius * 0.42f, glowCirclePaint)
                        }
                        f.radius += 2.0f * scale
                        f.alpha *= 0.86f
                        if (f.alpha < 0.04f) fIter.remove()
                    }

                    val sIter = sparks.iterator()
                    val gravity = 0.28f * scale
                    while (sIter.hasNext()) {
                        val sp = sIter.next()
                        val a = (sp.life / sp.maxLife).coerceIn(0f, 1f)
                        if (particlesEnabled) {
                            glowCirclePaint.color = sp.color
                            glowCirclePaint.alpha = (a * 220).toInt().coerceIn(0, 255)
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
                            keyTextPaint.color = Color.argb(180, 110, 115, 125)
                            keyTextPaint.textSize = 9.5f * scale
                            frameCanvas.drawText("C$oct", cx, keyboardBottom - 6f * scale, keyTextPaint)
                        }

                        if (isKeyActive) {
                            val badgeY = topSurfaceBottom - 16f * scale
                            val badgeR = (whiteKeyWidth * 0.36f).coerceIn(7f * scale, 13f * scale)
                            keyBadgePaint.color = Color.parseColor("#FFEB3B")
                            frameCanvas.drawCircle(cx, badgeY, badgeR, keyBadgePaint)

                            keyTextPaint.color = Color.rgb(180, 40, 0)
                            keyTextPaint.textSize = badgeR * 1.05f
                            frameCanvas.drawText(NOTE_NAMES[midi % 12], cx, badgeY + keyTextPaint.textSize * 0.35f, keyTextPaint)
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

                                reusableRect.set(bkLeft + 1.5f * scale, keyboardTop + 2f * scale, bkRight + 1.5f * scale, blackKeyBot + 2.5f * scale)
                                frameCanvas.drawRoundRect(reusableRect, keyCorner, keyCorner, blackKeyShadowPaint)

                                blackKeyBodyPaint.color = if (isBlackActive) handCol else Color.parseColor("#1C1C1E")
                                reusableRect.set(bkLeft, keyboardTop, bkRight, blackKeyBot)
                                frameCanvas.drawRoundRect(reusableRect, keyCorner, keyCorner, blackKeyBodyPaint)

                                if (isBlackActive) {
                                    val cx = (bkLeft + bkRight) * 0.5f
                                    val badgeY = blackKeyBot - 13f * scale
                                    val badgeR = (blackKeyW * 0.42f).coerceIn(6f * scale, 10f * scale)
                                    keyBadgePaint.color = Color.parseColor("#FFEB3B")
                                    frameCanvas.drawCircle(cx, badgeY, badgeR, keyBadgePaint)

                                    keyTextPaint.color = Color.rgb(180, 40, 0)
                                    keyTextPaint.textSize = badgeR * 1.0f
                                    frameCanvas.drawText(NOTE_NAMES[blackMidi % 12], cx, badgeY + keyTextPaint.textSize * 0.35f, keyTextPaint)
                                }
                            }
                        }
                    }

                    noteFillPaint.color = Color.parseColor("#990012")
                    frameCanvas.drawRect(0f, keyboardTop, width.toFloat(), keyboardTop + 4.5f * scale, noteFillPaint)

                    if (renderPedals) {
                        val pedalUnitW = 92f * scale
                        val pedalUnitH = 54f * scale
                        val pedalRight = width - 16f * scale
                        val pedalLeft = pedalRight - pedalUnitW
                        val pedalBottom = keyboardTop - 10f * scale
                        val pedalTop = pedalBottom - pedalUnitH

                        noteFillPaint.color = Color.parseColor("#D81C1C22")
                        reusableRect.set(pedalLeft, pedalTop, pedalRight, pedalBottom)
                        frameCanvas.drawRoundRect(reusableRect, 12f * scale, 12f * scale, noteFillPaint)

                        noteStrokePaint.color = Color.parseColor("#665A5A60")
                        noteStrokePaint.strokeWidth = 1.5f * scale
                        frameCanvas.drawRoundRect(reusableRect, 12f * scale, 12f * scale, noteStrokePaint)

                        val bladeW = 16f * scale
                        val bladeGap = 11f * scale
                        val startBladeX = pedalLeft + 11f * scale
                        val pedalNames = arrayOf("SOFT", "SOST", "SUST")

                        for (pIdx in 0..2) {
                            val isPressed = (pIdx == 2 && activeCountNow > 0)
                            val bx1 = startBladeX + pIdx * (bladeW + bladeGap)
                            val bx2 = bx1 + bladeW
                            val pressShift = if (isPressed) 4f * scale else 0f
                            val by1 = pedalTop + 8f * scale + pressShift
                            val by2 = pedalBottom - 15f * scale + pressShift

                            noteFillPaint.color = if (isPressed) Color.parseColor("#FFD54F") else Color.parseColor("#C89B3C")
                            reusableRect.set(bx1, by1, bx2, by2)
                            frameCanvas.drawRoundRect(reusableRect, 6f * scale, 6f * scale, noteFillPaint)

                            keyTextPaint.color = if (isPressed) accentGold else Color.parseColor("#B0FFFFFF")
                            keyTextPaint.textSize = 8f * scale
                            frameCanvas.drawText(pedalNames[pIdx], (bx1 + bx2) * 0.5f, pedalBottom - 4f * scale, keyTextPaint)
                        }
                    }
                }

                if (inputSurface != null && videoEncoder != null && muxer != null) {
                    try {
                        val surfaceCanvas = inputSurface.lockCanvas(null)
                        dstRect.set(0, 0, surfaceCanvas.width, surfaceCanvas.height)
                        surfaceCanvas.drawBitmap(frameBitmap, srcRect, dstRect, null)
                        inputSurface.unlockCanvasAndPost(surfaceCanvas)
                    } catch (_: Exception) {}
                }

                if (audioEncoder != null && !audioEosSent) {
                    val targetAudioByteLimit = (((currentTimeSec + 0.15f) * sampleRate).toInt() * bytesPerSample)
                        .coerceAtMost(pcmByteBuffer.limit())

                    while (pcmAudioQueuedBytes < targetAudioByteLimit || (frameIndex == totalFrames - 1 && !audioEosSent)) {
                        val inIdx = audioEncoder.dequeueInputBuffer(0L)
                        if (inIdx < 0) break
                        val inBuf = audioEncoder.getInputBuffer(inIdx) ?: break
                        inBuf.clear()

                        val remainingTotal = pcmByteBuffer.limit() - pcmAudioQueuedBytes
                        if (remainingTotal <= 0) {
                            val ptsUs = (pcmAudioQueuedBytes.toLong() * 1_000_000L) / (sampleRate * bytesPerSample)
                            audioEncoder.queueInputBuffer(inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            audioEosSent = true
                            break
                        }

                        val toCopy = minOf(inBuf.remaining(), remainingTotal)
                        pcmByteBuffer.position(pcmAudioQueuedBytes)
                        val slice = pcmByteBuffer.slice()
                        slice.limit(toCopy)
                        inBuf.put(slice)

                        val ptsUs = (pcmAudioQueuedBytes.toLong() * 1_000_000L) / (sampleRate * bytesPerSample)
                        pcmAudioQueuedBytes += toCopy
                        val isLastChunk = pcmAudioQueuedBytes >= pcmByteBuffer.limit()
                        val flags = if (isLastChunk) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        audioEncoder.queueInputBuffer(inIdx, 0, toCopy, ptsUs, flags)
                        if (isLastChunk) {
                            audioEosSent = true
                            break
                        }
                    }
                }

                drainEncoders(endOfStream = false)

                if (frameIndex % (targetFps / 4).coerceAtLeast(1) == 0 || frameIndex == totalFrames - 1) {
                    onProgress(progress.coerceIn(0f, 0.99f))
                }
            }

            drainEncoders(endOfStream = true)

            runCatching { audioEncoder?.stop(); audioEncoder?.release() }
            runCatching { videoEncoder?.stop(); videoEncoder?.release() }
            runCatching { if (muxerStarted) muxer?.stop(); muxer?.release() }
            runCatching { inputSurface?.release() }

            if (videoUri != null && resolver != null && outputFile.exists()) {
                resolver.openOutputStream(videoUri)?.use { outputStream ->
                    outputStream.write(outputFile.readBytes())
                }
            }

            frameBitmap.recycle()
            onProgress(1.0f)
            onComplete(videoUri?.toString() ?: outputFile.absolutePath)
        }
    }
}