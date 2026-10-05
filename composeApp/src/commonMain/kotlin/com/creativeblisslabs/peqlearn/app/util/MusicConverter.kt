package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.data.Alankaar
import com.creativeblisslabs.peqlearn.app.data.Raaga
import com.creativeblisslabs.peqlearn.app.data.Taal
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote

object MusicConverter {

    fun convertAlankaarToScore(
        alankaar: Alankaar,
        rootMidi: Int,
        taal: Taal,
        tempo: Float = 120.0f
    ): Score {
        val notes: List<Int> = alankaar.midiPattern.map { rootMidi + it }
        val numBeats: Int = if (taal.beats > 0) taal.beats else 4
        val beatsPerMeasure: Float = numBeats.toFloat()
        val noteDuration: Float = 1.0f / beatsPerMeasure
        val measures = mutableListOf<Measure>()
        var currentMeasureNotes = mutableListOf<ScoreNote>()

        for (index in notes.indices) {
            val midiNote = notes[index]
            val positionInMeasure = (index % numBeats).toFloat() / beatsPerMeasure
            currentMeasureNotes.add(
                ScoreNote(
                    id = 0,
                    midiNote = midiNote,
                    position = positionInMeasure,
                    duration = noteDuration
                )
            )

            if ((index + 1) % numBeats == 0 || index == notes.size - 1) {
                measures.add(
                    Measure(
                        notes = currentMeasureNotes,
                        barline = if (index == notes.size - 1) "final" else "single"
                    )
                )
                currentMeasureNotes = mutableListOf()
            }
        }

        return Score(
            title = alankaar.name,
            tempo = tempo,
            timeSignature = "$numBeats/4",
            measures = measures
        )
    }

    fun convertRaagaToScore(
        raaga: Raaga,
        rootMidi: Int,
        taal: Taal,
        tempo: Float = 120.0f,
        system: String = "Hindustani"
    ): Score {
        val arohaOffsets: List<Int> = SwaraMapper.sargamToSemitones(raaga.aroha.split(" "), system)
        val avarohaOffsets: List<Int> = SwaraMapper.sargamToSemitones(raaga.avaroha.split(" "), system)
        val allNotes: List<Int> = (arohaOffsets + avarohaOffsets).map { rootMidi + it }

        val numBeats: Int = if (taal.beats > 0) taal.beats else 4
        val beatsPerMeasure: Float = numBeats.toFloat()
        val noteDuration: Float = 1.0f / beatsPerMeasure
        val measures = mutableListOf<Measure>()
        var currentMeasureNotes = mutableListOf<ScoreNote>()

        for (index in allNotes.indices) {
            val midiNote = allNotes[index]
            val positionInMeasure = (index % numBeats).toFloat() / beatsPerMeasure
            currentMeasureNotes.add(
                ScoreNote(
                    id = 0,
                    midiNote = midiNote,
                    position = positionInMeasure,
                    duration = noteDuration
                )
            )

            if ((index + 1) % numBeats == 0 || index == allNotes.size - 1) {
                measures.add(
                    Measure(
                        notes = currentMeasureNotes,
                        barline = if (index == allNotes.size - 1) "final" else "single"
                    )
                )
                currentMeasureNotes = mutableListOf()
            }
        }

        return Score(
            title = raaga.name,
            tempo = tempo,
            timeSignature = "$numBeats/4",
            measures = measures
        )
    }
}
