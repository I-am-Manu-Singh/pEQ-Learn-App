package com.creativeblisslabs.peqlearn.app.video

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

object AudioSynthesizer {
    fun synthesizeAudioTrack(
        flatNotes: List<AndroidVideoExporter.ExportFlatNote>,
        totalDurationSec: Float,
        sampleRate: Int
    ): ByteBuffer {
        val totalAudioSamples = (totalDurationSec * sampleRate).toInt()
        val audioPcm = FloatArray(totalAudioSamples)

        for (fn in flatNotes) {
            val pitch = fn.note.midiNote.coerceIn(21, 108)
            val startSample = (fn.startSec * sampleRate).toInt().coerceIn(0, totalAudioSamples - 1)
            val ringSamples = ((fn.durationSec + 0.42f) * sampleRate).toInt()
            val freq = 440.0 * 2.0.pow((pitch - 69) / 12.0)
            val velGain = (90 / 127f) * 0.36f

            for (s in 0 until ringSamples) {
                val idx = startSample + s
                if (idx >= totalAudioSamples) break
                val t = s.toFloat() / sampleRate
                val releaseFade = if (t > fn.durationSec) exp(-(t - fn.durationSec) * 12.0).toFloat() else 1.0f
                val hammerDecay = exp(-t * (1.8 + (pitch - 21) * 0.035)).toFloat()
                val attack = min(1.0f, t * 240.0f)
                val env = hammerDecay * attack * releaseFade
                val inharmonicity = 1.0 + 0.00015 * ((pitch - 60).coerceAtLeast(0))
                val wave = (
                    sin(2.0 * Math.PI * freq * t) * 0.60 +
                        sin(2.0 * Math.PI * freq * 2.0 * inharmonicity * t) * 0.23 +
                        sin(2.0 * Math.PI * freq * 3.0 * inharmonicity * t) * 0.10 +
                        sin(2.0 * Math.PI * freq * 4.0 * inharmonicity * t) * 0.05 +
                        sin(2.0 * Math.PI * freq * 5.0 * inharmonicity * t) * 0.02
                    ).toFloat() * env * velGain
                audioPcm[idx] = (audioPcm[idx] + wave).coerceIn(-0.98f, 0.98f)
            }
        }

        val pcmByteBuffer = ByteBuffer.allocate(totalAudioSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in audioPcm) {
            pcmByteBuffer.putShort((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        }
        pcmByteBuffer.flip()
        return pcmByteBuffer
    }
}
