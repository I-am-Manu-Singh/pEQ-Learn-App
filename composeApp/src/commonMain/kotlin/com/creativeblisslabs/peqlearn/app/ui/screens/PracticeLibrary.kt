package com.creativeblisslabs.peqlearn.app.ui.screens

data class PracticeScaleItem(
    val name: String,
    val category: String, // Scale, Arpeggio, Chord Progression
    val rootPitch: Int = 60, // Middle C
    val intervals: List<Int>, // Semitone intervals
    val fingeringRightHand: List<Int>,
    val fingeringLeftHand: List<Int>,
    val description: String
)

object PracticeLibrary {
    val items = listOf(
        // Scales
        PracticeScaleItem(
            name = "C Major Scale",
            category = "Scales",
            rootPitch = 60,
            intervals = listOf(0, 2, 4, 5, 7, 9, 11, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "Natural major scale without sharps or flats. Essential foundation for classical and contemporary piano."
        ),
        PracticeScaleItem(
            name = "G Major Scale",
            category = "Scales",
            rootPitch = 67,
            intervals = listOf(0, 2, 4, 5, 7, 9, 11, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "One sharp (F#). Perfect practice for cross-under thumb technique."
        ),
        PracticeScaleItem(
            name = "A Natural Minor",
            category = "Scales",
            rootPitch = 57,
            intervals = listOf(0, 2, 3, 5, 7, 8, 10, 12),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 4, 5),
            fingeringLeftHand = listOf(5, 4, 3, 2, 1, 3, 2, 1),
            description = "Relative minor of C Major, creating a solemn, contemplative tone."
        ),
        PracticeScaleItem(
            name = "C Minor Pentatonic & Blues",
            category = "Scales",
            rootPitch = 60,
            intervals = listOf(0, 3, 5, 6, 7, 10, 12),
            fingeringRightHand = listOf(1, 2, 3, 4, 1, 3, 4),
            fingeringLeftHand = listOf(4, 3, 2, 1, 3, 2, 1),
            description = "The fundamental blues scale with the blue note (Gb/F#) for improvisation."
        ),
        // Arpeggios
        PracticeScaleItem(
            name = "C Major Arpeggio",
            category = "Arpeggios",
            rootPitch = 60,
            intervals = listOf(0, 4, 7, 12, 16, 19, 24),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 5),
            fingeringLeftHand = listOf(5, 4, 2, 1, 4, 2, 1),
            description = "Ascending two-octave major triad arpeggio for wrist flexibility."
        ),
        PracticeScaleItem(
            name = "A Minor Arpeggio",
            category = "Arpeggios",
            rootPitch = 57,
            intervals = listOf(0, 3, 7, 12, 15, 19, 24),
            fingeringRightHand = listOf(1, 2, 3, 1, 2, 3, 5),
            fingeringLeftHand = listOf(5, 4, 2, 1, 4, 2, 1),
            description = "Minor triad two-octave arpeggio building finger independence."
        ),
        // Chord Progressions
        PracticeScaleItem(
            name = "ii - V - I Jazz Cadence",
            category = "Chords",
            rootPitch = 60,
            intervals = listOf(2, 5, 9, 12,  7, 11, 14, 17,  0, 4, 7, 11), // Dm7 -> G7 -> Cmaj7
            fingeringRightHand = listOf(1, 2, 3, 5,  1, 2, 3, 5,  1, 2, 3, 5),
            fingeringLeftHand = listOf(5, 3, 2, 1,  5, 3, 2, 1,  5, 3, 2, 1),
            description = "The timeless jazz cadence: Dmin7 to G7 to Cmaj7."
        ),
        PracticeScaleItem(
            name = "I - V - vi - IV Pop Progression",
            category = "Chords",
            rootPitch = 60,
            intervals = listOf(0, 4, 7, 12,  7, 11, 14, 19,  9, 12, 16, 21,  5, 9, 12, 17), // C - G - Am - F
            fingeringRightHand = listOf(1, 3, 5, 5,  1, 3, 5, 5,  1, 3, 5, 5,  1, 3, 5, 5),
            fingeringLeftHand = listOf(5, 3, 1, 1,  5, 3, 1, 1,  5, 3, 1, 1,  5, 3, 1, 1),
            description = "The most popular four chords in modern music (C - G - Am - F)."
        )
    ) + com.creativeblisslabs.peqlearn.app.util.RealProgressionsDb.list.map { prog ->
        // Convert progression degrees into semitone intervals dynamically
        val intervals = mutableListOf<Int>()
        prog.degrees.forEach { deg ->
            val rootOffset = when (deg.uppercase().replace("7", "").replace("M", "")) {
                "I" -> 0
                "II" -> 2
                "III" -> 4
                "IV" -> 5
                "V" -> 7
                "VI" -> 9
                "VII" -> 11
                else -> 0
            }
            val isMinor = deg.firstOrNull()?.isLowerCase() == true
            val third = if (isMinor) 3 else 4
            val fifth = 7
            intervals.addAll(listOf(rootOffset, rootOffset + third, rootOffset + fifth, rootOffset + 12))
        }
        PracticeScaleItem(
            name = prog.name,
            category = "Tutorial: ${prog.category}",
            rootPitch = 60,
            intervals = intervals,
            fingeringRightHand = List(intervals.size) { (it % 4) + 1 },
            fingeringLeftHand = List(intervals.size) { 5 - (it % 4) },
            description = "${prog.romanDisplay} • ${prog.description} (e.g. ${prog.examples})"
        )
    }

    fun createScoreForPracticeItem(item: PracticeScaleItem): Score {
        val notes = mutableListOf<ScoreNote>()
        var currentBeat = 0.0f
        
        item.intervals.forEachIndexed { index, semitoneOffset ->
            val pitch = item.rootPitch + semitoneOffset
            val rhFinger = item.fingeringRightHand.getOrNull(index)
            val isRh = pitch >= 60

            notes.add(
                ScoreNote(
                    id = index + 1,
                    midiNote = pitch,
                    position = currentBeat,
                    duration = 1.0f,
                    voice = if (isRh) 0 else 1,
                    fingering = rhFinger,
                    chordSymbol = if (index == 0) item.name else ""
                )
            )
            currentBeat += 1.0f
        }

        val measures = mutableListOf<Measure>()
        val beatsPerMeasure = 4
        var measureIdx = 1
        var noteIdx = 0

        while (noteIdx < notes.size) {
            val chunk = mutableListOf<ScoreNote>()
            val chunkStartBeat = (measureIdx - 1) * beatsPerMeasure.toFloat()
            val chunkEndBeat = chunkStartBeat + beatsPerMeasure

            while (noteIdx < notes.size && notes[noteIdx].position < chunkEndBeat) {
                val n = notes[noteIdx]
                chunk.add(n.copy(position = n.position - chunkStartBeat))
                noteIdx++
            }

            measures.add(
                Measure(
                    notes = chunk,
                    measureIndex = measureIdx,
                    timeSignatureNumerator = 4,
                    timeSignatureDenominator = 4,
                    barline = if (noteIdx >= notes.size) "final" else "single"
                )
            )
            measureIdx++
        }

        return Score(
            title = item.name,
            composer = "pEQ Learn Studio",
            tempo = 80f,
            tempoText = "Andante",
            measures = measures
        )
    }
}
