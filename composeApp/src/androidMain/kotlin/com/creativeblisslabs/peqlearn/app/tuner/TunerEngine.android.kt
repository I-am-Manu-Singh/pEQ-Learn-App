package com.creativeblisslabs.peqlearn.app.tuner

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * ANDROID REAL-TIME MICROPHONE PITCH DETECTOR
 * Implements autocorrelation (YIN-like algorithm) with parabolic interpolation
 * to accurately estimate fundamental pitch (F0) from mic audio buffer.
 */
class AndroidTunerEngine : TunerEngine {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 44100
    private val bufferSize = 4096 // Gives ~10.7Hz frequency resolution at 44.1kHz

    private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    @SuppressLint("MissingPermission")
    override fun startListening(
        onPitchDetected: (pitchHz: Float, noteName: String, centsDeviation: Int, amplitude: Float) -> Unit
    ) {
        if (isRecording) return

        try {
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val actualBufSize = maxOf(minBufSize, bufferSize * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                actualBufSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                println("pEQLearn [Tuner]: AudioRecord failed to initialize")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch {
                val audioBuffer = ShortArray(bufferSize)

                while (isActive && isRecording) {
                    val readSamples = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                    if (readSamples > 0) {
                        // Calculate RMS amplitude
                        var sumSquares = 0.0
                        for (i in 0 until readSamples) {
                            val sample = audioBuffer[i] / 32768.0
                            sumSquares += sample * sample
                        }
                        val rms = sqrt(sumSquares / readSamples).toFloat()

                        // Noise floor threshold (avoid pitch detection on silence or faint ambient noise)
                        if (rms > 0.015f) {
                            val detectedFreq = detectPitchAutocorrelation(audioBuffer, readSamples, sampleRate)

                            // Practical acoustic instrument range (B0 30.87Hz up to B6 1975.5Hz)
                            if (detectedFreq in 30.0f..2000.0f) {
                                val (noteName, cents) = frequencyToNoteAndCents(detectedFreq)
                                withContext(Dispatchers.Main) {
                                    onPitchDetected(detectedFreq, noteName, cents, rms)
                                }
                            }
                        }
                    }
                    delay(30) // ~33 FPS updates
                }
            }
        } catch (e: Exception) {
            println("pEQLearn [Tuner]: Exception in startListening: ${e.message}")
            stopListening()
        }
    }

    override fun stopListening() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    override fun isListening(): Boolean = isRecording

    /**
     * Autocorrelation with parabolic interpolation peak detection.
     */
    private fun detectPitchAutocorrelation(buffer: ShortArray, size: Int, sampleRate: Int): Float {
        val minPeriod = sampleRate / 1200 // Max ~1200 Hz
        val maxPeriod = sampleRate / 35   // Min ~35 Hz (Low E on Bass / B0)

        var bestPeriod = -1
        var maxCorr = -1.0
        val normBuffer = FloatArray(size) { buffer[it] / 32768f }

        // Autocorrelation over candidate pitch periods
        val correlations = DoubleArray(maxPeriod + 2)

        for (lag in minPeriod..maxPeriod) {
            var sum = 0.0
            for (i in 0 until (size - lag)) {
                sum += normBuffer[i] * normBuffer[i + lag]
            }
            correlations[lag] = sum
        }

        // Peak picking with local maxima check
        for (lag in (minPeriod + 1) until maxPeriod) {
            if (correlations[lag] > correlations[lag - 1] && correlations[lag] > correlations[lag + 1]) {
                if (correlations[lag] > maxCorr) {
                    maxCorr = correlations[lag]
                    bestPeriod = lag
                }
            }
        }

        if (bestPeriod == -1) return 0f

        // Parabolic interpolation around bestPeriod for sub-sample accuracy
        val alpha = correlations[bestPeriod - 1]
        val beta = correlations[bestPeriod]
        val gamma = correlations[bestPeriod + 1]
        val delta = (gamma - alpha) / (2.0 * (2.0 * beta - alpha - gamma))
        val refinedPeriod = bestPeriod + delta

        return (sampleRate / refinedPeriod).toFloat()
    }

    /**
     * Converts Hz to Note Name (e.g. "D3", "E4") and cents deviation (-50..+50) against A4=440Hz standard.
     */
    private fun frequencyToNoteAndCents(freqHz: Float): Pair<String, Int> {
        val a4 = 440.0
        val semitonesFromA4 = 12.0 * log2(freqHz / a4)
        val roundedSemitones = semitonesFromA4.roundToInt()
        val cents = ((semitonesFromA4 - roundedSemitones) * 100.0).roundToInt().coerceIn(-50, 50)

        // MIDI pitch: A4 = 69
        val midiPitch = 69 + roundedSemitones
        val noteIdx = ((midiPitch % 12) + 12) % 12
        val octave = (midiPitch / 12) - 1

        val noteName = "${NOTE_NAMES[noteIdx]}$octave"
        return Pair(noteName, cents)
    }
}

private val androidTuner = AndroidTunerEngine()
actual fun getTunerEngine(): TunerEngine = androidTuner

