package com.creativeblisslabs.peqlearn.app.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

val _currentSelectedRaga = MutableStateFlow<Raaga?>(null)
val AppViewModel.currentSelectedRaga: StateFlow<Raaga?> get() = _currentSelectedRaga.asStateFlow()

val _metronomeActive = MutableStateFlow(false)
val AppViewModel.metronomeActive: StateFlow<Boolean> get() = _metronomeActive.asStateFlow()

val _metronomeBpm = MutableStateFlow(120)
val AppViewModel.metronomeBpm: StateFlow<Int> get() = _metronomeBpm.asStateFlow()

fun AppViewModel.selectRaga(raga: Raaga) {
    _currentSelectedRaga.value = raga
}

fun AppViewModel.toggleMetronome() {
    _metronomeActive.value = !_metronomeActive.value
}

fun AppViewModel.setMetronomeBpm(bpm: Int) {
    _metronomeBpm.value = bpm.coerceIn(40, 280)
}

// ============================================================================
// RECOVERED SEQUENCER, METRONOME, & DRUM RECORDING DISPATCHERS
// ============================================================================

fun AppViewModel.startSequencer() {
    isSequencerActive.value = true
}

fun AppViewModel.stopSequencer() {
    isSequencerActive.value = false
}

fun AppViewModel.toggleSequencerStep(step: Int, type: String = "Kick") {
    if (step >= 0 && step < sequencerSteps.size) {
        val currentState = sequencerSteps[step].value
        sequencerSteps[step].value = (currentState + 1) % 3
    }
}

fun AppViewModel.updateMasterBpm(bpm: Float) {
    masterBpm.floatValue = bpm
}

fun AppViewModel.tapTempo() {
    val now = Clock.System.now().toEpochMilliseconds()
    tapTimes.add(now)
    if (tapTimes.size > 8) {
        tapTimes.removeAt(0)
    }
    if (tapTimes.size >= 2) {
        val totalDiff = tapTimes.last() - tapTimes.first()
        val avgDiff = totalDiff / (tapTimes.size - 1)
        if (avgDiff in 250..1500) {
            val newBpm = 60000.0f / avgDiff.toFloat()
            updateMasterBpm(newBpm)
        }
    }
}

fun AppViewModel.toggleDrumRecording() {
    isRecordingDrumSession.value = !isRecordingDrumSession.value
}

fun AppViewModel.remapPad(index: Int, sample: String) {
    if (index >= 0 && index < drumPadLabels.size) {
        drumPadLabels[index] = sample
    }
}

fun AppViewModel.updateDrumPadVolume(index: Int, volume: Float) {
    drumPadVolumes[index] = volume
}

fun AppViewModel.onTunerPitchDetected(pitch: Float, note: String, cents: Int) {
    detectedPitch.floatValue = pitch
    detectedNote.value = note
    centsDeviation.intValue = cents
}

fun AppViewModel.onChordDetected(chord: String) {
    detectedChord.value = chord
}

fun AppViewModel.toggleTuner(active: Boolean) {
    isTunerActive.value = active
}

fun AppViewModel.startKitBuilding() {
    isKitBuilderActive.value = true
}

fun AppViewModel.observeMetronome() {
    // Hooks metronome tick events to audio engine
}

fun AppViewModel.startTutorial(pattern: DrumTutorial) {
    selectedTutorial.value = pattern
}

fun AppViewModel.switchDrumKit(kit: DrumKitPreset) {
    selectedDrumKit.value = kit
    drumPadLabels.clear()
    drumPadLabels.addAll(kit.labels)
}
