package com.creativeblisslabs.peqlearn.app.ui.screens

enum class LabLabelMode {
    NOTE_NAME,
    NOTE_NAMES,
    INTERVAL,
    INTERVALS,
    SCALE_DEGREE,
    SOLFEGE,
    FINGERING,
    NONE,
    NOTES,
    FINGERS
}

enum class LabPlayMode {
    STRUM,
    CHORD_BLOCK,
    CHORD_PLAY,
    ARPEGGIO,
    ARPEGGIO_UP,
    ARPEGGIO_DOWN,
    SCALE_RUN,
    SILENT,
    BLOCK_CHORD
}

enum class LabViewType {
    PIANO,
    GUITAR,
    UKULELE,
    STACKED,
    SPLIT_VIEW,
    PIANO_88,
    FRETBOARD,
    GUITAR_FRETBOARD,
    UKULELE_FRETBOARD,
    BASS_FRETBOARD,
    STAFF,
    TAB,
    DUAL_STACK
}
