package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.creativeblisslabs.peqlearn.app.ai.MusicSegment
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.ChordInstrumentType
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import com.creativeblisslabs.peqlearn.app.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ============================================================================
// 1. UNIFIED CANONICAL MUSIC THEORY CONSTANTS, SCALES, ARPEGGIOS & CONCEPTS
// ============================================================================

internal val STUDIO_ROOT_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
internal val SARGAM_SYLLABLES = listOf("Sa", "re", "Re", "ga", "Ga", "Ma", "ma", "Pa", "dha", "Dha", "ni", "Ni")
internal val SOLFEGE_SYLLABLES = listOf("Do", "Di", "Re", "Ri", "Mi", "Fa", "Fi", "Sol", "Si", "La", "Li", "Ti")
internal val DEGREE_LABELS = arrayOf(
    "1 (Sa)", "♭2 (re)", "2 (Re)", "♭3 (ga)", "3 (Ga)", "4 (Ma)",
    "♯4 (ma)", "5 (Pa)", "♭6 (dha)", "6 (Dha)", "♭7 (ni)", "7 (Ni)"
)

val STUDIO_SCALE_LIBRARY: Map<String, List<Int>> = linkedMapOf(
    "Major (Ionian / Bilawal)" to listOf(0, 2, 4, 5, 7, 9, 11),
    "Natural Minor (Aeolian / Asavari)" to listOf(0, 2, 3, 5, 7, 8, 10),
    "Harmonic Minor" to listOf(0, 2, 3, 5, 7, 8, 11),
    "Melodic Minor" to listOf(0, 2, 3, 5, 7, 9, 11),
    "Major Pentatonic (Bhoopali)" to listOf(0, 2, 4, 7, 9),
    "Minor Pentatonic (Malkauns)" to listOf(0, 3, 5, 7, 10),
    "Blues Scale" to listOf(0, 3, 5, 6, 7, 10),
    "Dorian (Kafi)" to listOf(0, 2, 3, 5, 7, 9, 10),
    "Mixolydian (Khamaj)" to listOf(0, 2, 4, 5, 7, 9, 10),
    "Lydian (Yaman / Kalyan)" to listOf(0, 2, 4, 6, 7, 9, 11),
    "Phrygian (Bhairavi)" to listOf(0, 1, 3, 5, 7, 8, 10),
    "Bhairav Thaat" to listOf(0, 1, 4, 5, 7, 8, 11),
    "Purvi Thaat" to listOf(0, 1, 4, 6, 7, 8, 11),
    "Marwa Thaat" to listOf(0, 1, 4, 6, 7, 9, 11),
    "Todi Thaat" to listOf(0, 1, 3, 6, 7, 8, 11)
)

val STUDIO_ARPEGGIO_LIBRARY: Map<String, List<Int>> = linkedMapOf(
    "Major Triad (1-3-5)" to listOf(0, 4, 7),
    "Minor Triad (1-♭3-5)" to listOf(0, 3, 7),
    "Major 7th (1-3-5-7)" to listOf(0, 4, 7, 11),
    "Dominant 7th (1-3-5-♭7)" to listOf(0, 4, 7, 10),
    "Minor 7th (1-♭3-5-♭7)" to listOf(0, 3, 7, 10),
    "Half-Diminished m7♭5" to listOf(0, 3, 6, 10),
    "Diminished 7th (1-♭3-♭5-𝄫7)" to listOf(0, 3, 6, 9),
    "Augmented Triad (1-3-♯5)" to listOf(0, 4, 8),
    "Sus4 Arpeggio (1-4-5)" to listOf(0, 5, 7),
    "Major 9th (1-3-5-7-9)" to listOf(0, 4, 7, 11, 14),
    "Minor 9th (1-♭3-5-♭7-9)" to listOf(0, 3, 7, 10, 14)
)

val ALL_CONCEPTS: List<TheoryConcept> = listOf(
    TheoryConcept("🎵", "The Musical Alphabet", "Notes A through G form all of music", "The 12 notes of Western music repeat across octaves using letters A–G plus sharps/flats. Every melody, chord, and scale is built from this alphabet.", TheoryPart.CHORDS, listOf(0, 2, 4, 5, 7, 9, 11)),
    TheoryConcept(MusicSymbols.SHARP, "Sharps & Flats", "The black keys between main notes", "A sharp (♯) raises a note by one half step; a flat (♭) lowers it. On a piano, these are the black keys — C♯ is the black key right of C.", TheoryPart.CHORDS, listOf(1, 3, 6, 8, 10)),
    TheoryConcept("⚓", "Root Note", "The anchor that names a chord", "The root is the foundational note a chord is built on. In a C major chord, C is the root — it gives the chord its name and tonal center.", TheoryPart.CHORDS, listOf(0)),
    TheoryConcept("🎼", "Octave", "The same note, higher or lower", "An octave is the distance of 12 half steps (semitones). C3 and C4 are an octave apart — same pitch name, different frequency. C4 vibrates at exactly double C3.", TheoryPart.CHORDS, listOf(0, 12)),
    TheoryConcept("☀️", "Major Scale", "The happy 7-note formula", "Built with the pattern W-W-H-W-W-W-H (Whole/Half steps). C major: C D E F G A B. Sounds complete, bright, and resolved.", TheoryPart.CHORDS, listOf(0, 2, 4, 5, 7, 9, 11)),
    TheoryConcept("🌑", "Minor Scale", "The dark 7-note formula", "Built W-H-W-W-H-W-W. A natural minor: A B C D E F G. Sounds melancholic, serious, or dramatic — three of its notes are one half step lower than major.", TheoryPart.CHORDS, listOf(0, 2, 3, 5, 7, 8, 10)),
    TheoryConcept("🏠", "Key Center", "The song's tonal home base", "The key center is the note/scale a song gravitates toward. In the key of G, the note G feels like home — melodies and chords resolve there.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("👯", "Relative Minor", "Same notes, different home", "Every major key has a relative minor that uses the exact same 7 notes but starts on a different root. C major ↔ A minor. They share no sharps or flats.", TheoryPart.CHORDS, listOf(0, 2, 3, 5, 7, 8, 10)),
    TheoryConcept("🌤", "Major Chord (Triad)", "Bright, stable 3-note chord", "Stack root + major 3rd (4 semitones) + perfect 5th (7 semitones). C major = C E G. The foundation of bright, happy music.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("🌩", "Minor Chord (Triad)", "Dark, moody 3-note chord", "Stack root + minor 3rd (3 semitones) + perfect 5th. Am = A C E. Lower the middle note by one half step vs major to get minor.", TheoryPart.CHORDS, listOf(0, 3, 7)),
    TheoryConcept("⚡", "Diminished Chord", "Highly tense, unstable chord", "Root + minor 3rd + diminished 5th (6 semitones). Bdim = B D F. All notes are 3 semitones apart. Used as a passing chord to create tension.", TheoryPart.CHORDS, listOf(0, 3, 6)),
    TheoryConcept("🌊", "Sus Chord", "Floating, unresolved sound", "Replace the 3rd with a 2nd (sus2) or 4th (sus4). Csus4 = C F G. No major/minor quality — sounds suspended, yearning to resolve.", TheoryPart.CHORDS, listOf(0, 5, 7)),
    TheoryConcept("✨", "Major 7th Chord", "Lush, jazzy 4-note chord", "Add the major 7th (11 semitones from root) to a major triad. Cmaj7 = C E G B. Sounds dreamy, sophisticated, Lo-Fi.", TheoryPart.CHORDS, listOf(0, 4, 7, 11)),
    TheoryConcept("🎷", "Minor 7th Chord", "Smooth, soulful 4-note chord", "Add the minor 7th (10 semitones) to a minor triad. Am7 = A C E G. The sound of smooth jazz, R&B, and neo-soul.", TheoryPart.CHORDS, listOf(0, 3, 7, 10)),
    TheoryConcept("🔥", "Dominant 7th Chord", "Bluesy tension wanting to resolve", "Root + major 3rd + perfect 5th + minor 7th. G7 = G B D F. Dominant tension pulls strongly to the tonic (home key).", TheoryPart.CHORDS, listOf(0, 4, 7, 10)),
    TheoryConcept("🎺", "Extended Chords 9/11/13", "Extra high notes for jazz/R&B", "Keep stacking 3rds: 9th, 11th, 13th. Cmaj9 = C E G B D. Rich jazz and R&B harmony — each extension adds a new colour layer.", TheoryPart.CHORDS, listOf(0, 4, 7, 11, 14)),
    TheoryConcept("🔄", "Inversion", "Flip a chord so another note is lowest", "First inversion: 3rd in bass (C/E). Second: 5th in bass (C/G). Inversions create smoother voice leading between chords.", TheoryPart.CHORDS, listOf(4, 7, 12)),
    TheoryConcept("🎨", "Voicing", "How you spread chord notes across hands", "Same chord, different layout. Close voicing: all notes within one octave. Open voicing: spread across the keyboard for a richer, more spacious sound.", TheoryPart.CHORDS, listOf(0, 7, 16)),
    TheoryConcept("🎸/", "Slash Chord", "Specific bass note under a chord", "C/G means play a C chord with G in the bass. Common trick for smooth bass lines while chords stay the same. E.g. C → C/B → Am progression.", TheoryPart.CHORDS, listOf(-5, 0, 4, 7)),
    TheoryConcept("🏃", "Arpeggio", "Chord notes played one by one", "Instead of pressing all chord notes at once, play them in sequence (up or down). Creates movement and fills space — heavily used in pop, rock, and piano ballads.", TheoryPart.CHORDS, listOf(0, 4, 7, 12)),
    TheoryConcept("🏡", "Diatonic Chords", "The natural chord family of a key", "Each degree of the major scale produces a chord using only notes in that key. C major diatonic chords: C Dm Em F G Am Bdim.", TheoryPart.CHORDS, listOf(0, 2, 4, 5, 7, 9, 11)),
    TheoryConcept("🔢", "Roman Numeral System", "Label chords by scale degree", "I=tonic, IV=subdominant, V=dominant. So I-IV-V works in any key. In C: C-F-G. In G: G-C-D. Universal chord-communication language.", TheoryPart.CHORDS, listOf(0, 5, 7)),
    TheoryConcept("🎸", "I - IV - V - I", "The blues/rock chord skeleton", "The most foundational progression in Western music. In G: G-C-D-G. Drives blues, country, folk, and classic rock. The V chord creates tension that resolves to I.", TheoryPart.CHORDS, listOf(0, 5, 7)),
    TheoryConcept("🎷", "ii - V - I", "The jazz progression", "The cornerstone of jazz. Dm7 - G7 - Cmaj7 in C major. The ii chord sets up tension, V intensifies it, I resolves it. Found in almost every jazz standard.", TheoryPart.CHORDS, listOf(2, 7, 0)),
    TheoryConcept("📻", "I - V - vi - IV", "The pop chord loop", "Used in hundreds of hits: Let It Be, Someone Like You, No Woman No Cry, Africa. In C: C - G - Am - F. Works because vi provides emotional contrast.", TheoryPart.CHORDS, listOf(0, 7, 9, 5)),
    TheoryConcept("🧵", "Voice Leading", "Moving chords with minimal finger motion", "Change chords by moving individual notes as small a distance as possible. Creates smooth, connected chord progressions instead of jumpy ones.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("🌉", "Passing Chord", "A quick chord sneaked between two main chords", "A temporary chord (often chromatic) inserted to smoothly connect two diatonic chords. Adds sophistication and movement without changing the key.", TheoryPart.CHORDS, listOf(0, 3, 6, 9)),
    TheoryConcept("🎁", "Borrowed Chord", "Stealing a chord from the minor key", "Temporarily borrowing a chord from the parallel minor key while staying in major. Creates surprise and emotional depth — common in pop and rock.", TheoryPart.CHORDS, listOf(0, 3, 8)),
    TheoryConcept("🚀", "Modulation", "Shifting the entire song to a new key", "Moving the tonal center to a completely new key for a section. Creates energy, freshness, and emotional impact. Common in choruses and final sections.", TheoryPart.CHORDS, listOf(0, 2, 6, 9)),
    TheoryConcept("🔁", "Turnaround", "A quick pattern that loops back to the start", "A short chord sequence at the end of a loop that kicks cleanly back to the beginning. Standard jazz turnaround: I - VI - ii - V - I.", TheoryPart.CHORDS, listOf(0, 9, 2, 7)),
    TheoryConcept("↕", "Half Step", "One fret up or down", "The smallest interval in Western music. Moving from fret 5 to fret 6 is one half step (one semitone). 12 half steps = one octave.", TheoryPart.CHORDS, listOf(0, 1)),
    TheoryConcept("↕↕", "Whole Step", "Two frets up or down", "Equal to two half steps. Moving from fret 3 to fret 5 is one whole step. Major scale uses a specific pattern of whole and half steps.", TheoryPart.CHORDS, listOf(0, 2)),
    TheoryConcept("📏", "Interval", "The distance between two notes", "An interval is the gap between two pitches, measured in semitones. Each interval has a unique sound quality — intervals build scales and chords.", TheoryPart.CHORDS, listOf(0, 7)),
    TheoryConcept("⚡", "Perfect 5th / Power Chord", "The rock two-note chord", "Root + perfect 5th (7 semitones). Two fingers, sounds huge with distortion. No major/minor quality so it works in any context. The backbone of rock and metal.", TheoryPart.CHORDS, listOf(0, 7)),
    TheoryConcept("🎵", "Open Chords", "Chords using open ringing strings", "G, C, D, E, Em, Am use open strings at the nut. Fuller sound than barre chords. The first chords every guitarist learns. Limited to a few keys.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("✊", "Barre Chords", "First finger clamps across all strings", "Index finger pressed across all 6 strings acts as a moveable nut. Allows any open chord shape to be played in any key. E-shape and A-shape are most common.", TheoryPart.CHORDS, listOf(0, 7, 12)),
    TheoryConcept("🗺️", "CAGED System", "Five positions to find any chord on the neck", "C, A, G, E, D open chord shapes can all be played as barre chords up the neck. Five positions, one connected fretboard map.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("📦", "Fretboard Box Pattern", "A finger shape that unlocks a scale", "A box pattern is a shape (usually within 4-5 frets) that covers all notes of a scale. Learn one box, play it at any fret = any key.", TheoryPart.CHORDS, SCALE_PENTATONIC_M_PATTERN),
    TheoryConcept("🎸", "Pentatonic Minor", "The 5-note solo scale", "Root, ♭3, 4, 5, ♭7 — five notes that sound great over almost anything. The universal rock/blues/pop solo scale. Learn Position 1 first.", TheoryPart.CHORDS, SCALE_PENTATONIC_M_PATTERN),
    TheoryConcept("☀️", "Pentatonic Major", "Sweet, country/pop leads", "Root, M2, M3, P5, M6. Same notes as relative minor pentatonic, just starting from a different root. Country, classic rock, and sweet pop solos.", TheoryPart.CHORDS, listOf(0, 2, 4, 7, 9)),
    TheoryConcept("💙", "The Blue Note", "The one note that makes it bluesy", "Add the ♭5 (tritone) to the minor pentatonic scale. That one dissonant note creates the characteristic tension of the blues.", TheoryPart.CHORDS, SCALE_BLUES_PATTERN),
    TheoryConcept("🎵", "Mixolydian Mode", "Major with a ♭7 — classic rock flavour", "Like a major scale but the 7th note is one half step lower. R W W H W W H W. The scale of classic rock jams, blues, and Hendrix-style playing.", TheoryPart.CHORDS, SCALE_MIXOLYDIAN_PATTERN),
    TheoryConcept("🎷", "Dorian Mode", "Minor with a raised 6th — funk/jazz", "Like natural minor but the 6th note is one half step higher. Dark but slightly bright on that 6th. Santana, Herbie Hancock, and Daft Punk live here.", TheoryPart.CHORDS, SCALE_DORIAN_PATTERN),
    TheoryConcept("🌌", "Lydian Mode", "Major with a ♯4 — cinematic, spacey", "Like major but the 4th is raised. Sounds dreamy, floating, and mysterious. John Williams film scores, Joe Satriani, and progressive rock.", TheoryPart.CHORDS, SCALE_LYDIAN_PATTERN),
    TheoryConcept("⚔️", "Phrygian Mode", "Dark minor with ♭2 — metal/flamenco", "Like natural minor but the 2nd note is one half step lower. Deeply dark and exotic. Heavy metal guitar, Spanish flamenco, and Middle Eastern music.", TheoryPart.CHORDS, SCALE_PHRYGIAN_PATTERN),
    TheoryConcept("🔨", "Hammer-on", "Fret a note without picking", "Fret with your picking hand, then hammer a higher fret with your fretting hand — the second note sounds without picking. Creates fast, smooth legato phrases.", TheoryPart.CHORDS, listOf(0, 2)),
    TheoryConcept("💧", "Pull-off", "Flick finger to sound a lower note", "Grip two frets, then flick the higher finger off — the lower fretting note rings. The reverse of a hammer-on. Together they create fast legato runs.", TheoryPart.CHORDS, listOf(2, 0)),
    TheoryConcept("¼", "Slide", "Glide from one note to another", "Keep fretting pressure while moving your finger along the string. Creates a smooth, vocal-like glide between notes. Blues, country, and rock staple.", TheoryPart.CHORDS, listOf(0, 4)),
    TheoryConcept("〰", "String Bend", "Push a string to warp its pitch", "Push a string perpendicular to the neck to raise its pitch. A full bend = 2 frets up. Gives guitar its most vocal, expressive character.", TheoryPart.CHORDS, listOf(7, 9)),
    TheoryConcept("〰〰", "Vibrato", "Rapid shaking for expressive sustain", "Repeatedly bend and release a held note to create a vocal wobble. The most personal expression technique — every player's vibrato is unique.", TheoryPart.CHORDS, listOf(0)),
    TheoryConcept("🤚", "Palm Muting", "Chunky, tight muted notes", "Rest the edge of your picking hand on the strings near the bridge. Shortens the string vibration, making notes sound thick and percussive. Rock and metal rhythm technique.", TheoryPart.CHORDS, listOf(0, 7)),
    TheoryConcept("🔔", "Harmonics", "Bell-like tones from light touch", "Lightly touch (don't press) a string directly over a fret wire (5th, 7th, 12th frets work best), then pick. Creates a pure, glass-bell tone.", TheoryPart.CHORDS, listOf(12)),
    TheoryConcept("🎤", "Lead vs. Rhythm", "Solo melodies vs. chord strumming", "Rhythm guitar: strumming chords to drive the song's groove. Lead guitar: single-note melodies and solos over the top. Most guitarists do both.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("🪝", "Riff", "The song's repeating guitar hook", "A catchy, short repeating melodic pattern that defines a song's character. Smoke on the Water, Whole Lotta Love, Seven Nation Army — all iconic riffs.", TheoryPart.CHORDS, listOf(0, 3, 5)),
    TheoryConcept("✍", "Lick", "A short solo phrase", "A ready-made short phrase (usually 2–8 notes) that you practise until fluent, then insert into solos. Build a vocabulary of licks to improvise confidently.", TheoryPart.CHORDS, listOf(0, 3, 5, 7, 10)),
    TheoryConcept("🎵🎵", "Double Stopping", "Two notes at once in a solo", "Grab two strings simultaneously during a solo. Creates a harmonised melody line, adding richness. Used constantly in country, blues, and rock leads.", TheoryPart.CHORDS, listOf(0, 4)),
    TheoryConcept("🔗", "Capo", "A clip that changes your open-chord key", "Clamped across the neck, a capo raises the pitch of all open strings. Play an open G shape with capo on 2nd fret — sounds as A. Transpose easily.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("🎸", "Standard Tuning", "E A D G B e (low to high)", "The default guitar tuning. Low E is the thickest string. Each string is tuned a perfect 4th above the previous (except G to B, which is a major 3rd).", TheoryPart.CHORDS, listOf(4, 9, 2, 7, 11, 4)),
    TheoryConcept("⬇", "Drop D Tuning", "Low E dropped to D for power chords", "Lower the thickest string from E to D. Power chords become 1-finger shapes on the lowest three strings. Heavy, dark sound — metal and grunge staple.", TheoryPart.CHORDS, listOf(2, 9, 2)),
    TheoryConcept("📋", "Tablature (Tabs)", "Guitar notation using numbers on lines", "Six lines represent six strings. Numbers show which fret to press. No music reading required. The most common way guitar music is written online.", TheoryPart.CHORDS, listOf(0, 4, 7)),
    TheoryConcept("💓", "The Beat (Pulse)", "The steady heartbeat of a song", "The pulse is the recurring, equal division of time that people clap along to. Every element of a song is organised around the beat.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("🏃", "BPM", "Beats Per Minute — the tempo number", "60 BPM = 1 beat per second. 120 BPM = 2 beats per second. Genre guideline: hip-hop 70-100, house 120-130, drum & bass 160-180.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("🎯", "Click Track / Metronome", "The digital ticking clock for timing", "A metronome produces a click on every beat. Recording with a click ensures all tracks line up perfectly. Practising to a click develops internal timing.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("📊", "The Grid", "The visual timeline in a DAW", "The DAW arranges music on a horizontal timeline divided into bars and beats. Zooming in reveals subdivisions. Drum hits are placed (or 'snapped') on grid lines.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("🎯", "Quantization", "Auto-snapping hits to the grid", "Quantize pushes off-time notes to the nearest grid point. Over-quantizing creates a robotic feel; slight looseness gives human groove. Use tastefully.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("📐", "Time Signature", "The rule for beats per bar", "Written as a fraction: 4/4, 3/4, 6/8. Top number = beats per bar. Bottom number = note value of each beat. 4/4 is most common in pop/rock.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("4️⃣", "4/4 Time", "Standard 'four on the floor' time", "Four quarter-note beats per bar. By far the most common time signature in pop, rock, R&B, electronic, and country music.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("3️⃣", "3/4 Time", "Waltz timing — three beats per bar", "Three quarter-note beats. Gives a flowing, 'ONE-two-three ONE-two-three' feel. Used in waltzes, many ballads, and some folk music.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("6️⃣", "6/8 Time", "A swinging compound meter", "Six eighth-note beats per bar, felt as two groups of three. Creates a heavy swinging feel. Used in slow blues, gospel ballads, and many pop songs.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("1️⃣", "Downbeat", "Beat 1 — the start of the bar", "The strongest beat in any time signature. Musicians return to the downbeat after fills and solos. Most chord changes happen on downbeats.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("An", "Upbeat", "The space between main beats", "The 'and' in '1 and 2 and 3 and 4 and'. Playing on upbeats creates forward momentum and rhythmic interest. Essential for reggae and funk.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("♩", "Quarter Notes", "Exactly on the main beats", "One note per beat. In 4/4, four quarter notes fill one bar. The simplest rhythm — 'ONE, TWO, THREE, FOUR'.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("♪♪", "Eighth Notes", "Splitting each beat in half", "Two eighth notes per beat, doubling the density. Counted '1-and-2-and-3-and-4-and'. The most common subdivision in most genres.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("⚡", "Sixteenth Notes", "Four per beat — the trap grid", "Four sixteenth notes per beat. Counted '1-e-and-a'. Essential for hi-hat programming in trap, intricate pop percussion, and funk drum patterns.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("3️⃣", "Triplets", "Three evenly spaced hits per beat", "Divide one beat into three equal parts. Triplets have a rolling, shuffling feel. Cornerstone of jazz, blues, and shuffle grooves.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("🎯", "In The Pocket", "Rock-solid timing that locks a band", "Playing 'in the pocket' means your timing is so consistent it creates an almost physical groove. The drummer's primary job. Listeners feel compelled to move.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("📏", "Straight Feel", "Even, perfectly spaced rhythm", "Every subdivision is metronomically equal. Most electronic music and rock is straight. Feels driving and precise. Opposite of swing.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("🌊", "Swing Feel", "Delayed off-beats for jazz bounce", "Off-beats (the 'ands') are played later than the grid. Triplet-based: the first of each beat-pair is long, second is short. Creates a bouncing, dancing feel.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("🔀", "Shuffle Feel", "Blues triplet groove with middle left out", "In each triplet group, play the first and third hits, skip the middle. '1, -(skip)- trip, 2, -(skip)- trip'. The driving feel of Chicago blues.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("🎲", "Syncopation", "Accents on weak beats for surprise", "Intentionally emphasising the 'off' beat or 'and' instead of the main beat. Creates rhythmic tension and surprise. The engine of funk, reggae, and jazz.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("💥", "Backbeat", "Snare on beats 2 and 4", "The most recognisable element of rock and pop drumming. The snare hit on 2 and 4 creates the signature pop rhythm feel. Clapping on 2 & 4 is the back beat.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("👻", "Ghost Notes", "Ultra-quiet notes for texture", "Extremely soft snare strikes barely audible in the mix. They add rhythmic richness and make a groove feel alive rather than mechanical. Heavy ghost notes = funk.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("⏳", "Half-Time", "Snare moves to beat 3 — slower feel", "Move the snare from beats 2 & 4 to just beat 3. The whole groove suddenly feels twice as slow/heavy without changing BPM. Hip-hop and trap staple.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("⚡", "Double-Time", "Groove feels twice as fast", "Double the density of the groove (move snare to every beat) without raising the BPM. Feels manic and energetic — used for breakdowns and climaxes.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("💥", "Drum Fill", "Break from groove to transition sections", "A short melodic pattern around the kit signalling a section change (chorus dropping in, verse returning). Fills are like punctuation marks for sections.", TheoryPart.GROOVE, emptyList()),
    TheoryConcept("📋", "Arrangement", "Who plays what, and when", "Deciding which instruments play in each section, when they enter, and when they drop out. Great arrangement makes a simple song feel epic.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("📢", "Dynamics", "Intentional volume and energy changes", "Getting quieter in the verse so the chorus hits harder. Dynamics are the most powerful tool for creating emotional impact in a song.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🚪", "Intro", "The opening that sets the mood", "Establishes tempo, key, mood, and often the main riff or groove. Should hook the listener immediately. Can be as short as 2 bars or as long as a minute.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("📖", "Verse", "The storytelling section", "Where the lyrics tell the story — each verse usually has different words but the same musical structure. Typically quieter and more sparse than the chorus.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🌅", "Pre-Chorus", "The tension-building section", "A short rising section between the verse and chorus. Builds anticipation by increasing energy and momentum so the chorus feels like a release.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🎆", "Chorus", "The biggest, most memorable section", "The emotional peak of the song — loudest, busiest arrangement, most memorable melody. The chorus is what people sing along to. Usually same words each time.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🌉", "Bridge", "A one-time contrasting section", "Appears once, usually before the final chorus. Different melody, chords, or feel from the verse and chorus. Provides contrast and a new perspective.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🚪", "Outro", "The closing section", "Wraps up the song — can fade out, stop suddenly, or gradually strip instruments away. Gives the listener a sense of resolution and closure.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🪝", "The Hook", "The catchiest part that sticks in your head", "The musical idea (vocal line, guitar riff, synth melody) that people remember after the song ends. A great hook is worth more than any other songwriting skill.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🔁", "Vamp / Jam", "Looping a section for improvisation", "Repeat a short chord loop over and over so musicians can solo or improvise freely. Latin music vamping, jazz head-solos-head structure.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("💥", "Breakdown", "Strip everything to just bass & drums", "Remove most instruments suddenly to spotlight the groove or create drama. The stripped-down section creates tension before the big drop or build.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("📈", "Build-up", "Gradually adding energy", "Add instruments, volume, and intensity over 4–16 bars to transition from quiet to loud. The build creates anticipation — the payoff is the drop or chorus.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("📞", "Call & Response", "Musical conversation between parts", "One instrument plays a phrase (call), another answers (response). Like a question and answer. Roots in West African music — central to jazz, blues, and gospel.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🎵🎵🎵", "Unison Line", "All instruments play the same melody", "Guitar, bass, and keys all play the exact same fast melodic phrase together. Creates a powerful, dense textural moment used in jazz intros and rock breakdowns.", TheoryPart.STRUCTURE, listOf(0, 3, 5, 7)),
    TheoryConcept("📄", "Lead Sheet", "The 1-page song cheat-sheet", "Contains just the vocal melody (in standard notation) and chord symbols above — no arrangement details. Everything musicians need to learn any song on the fly.", TheoryPart.STRUCTURE, emptyList()),
    TheoryConcept("🎵", "Raga (Raaga)", "The melodic framework of Indian music", "A raga is more than a scale — it's a complete melodic system with specific ascending/descending rules (aroha/avaroha), characteristic phrases, ornaments, time of day, and emotional quality (rasa).", TheoryPart.INDIAN_CLASSICAL, listOf(0, 1, 4, 5, 7, 8, 11)),
    TheoryConcept("📏", "Thaat System", "10 parent scales of Hindustani music", "Vishnu Narayan Bhatkhande organized all Hindustani ragas under 10 parent scales called Thaats. Each thaat is a combination of 7 swaras from the 12 available.", TheoryPart.INDIAN_CLASSICAL, listOf(0, 2, 4, 5, 7, 9, 11)),
    TheoryConcept("🎼", "Saptak (Octave)", "The 7 + 5 = 12 swaras of Indian music", "Indian music uses 7 swaras: Sa Re Ga Ma Pa Dha Ni. The 5 vikrit (modified) swaras give 12 notes. Sa and Pa are shuddha (pure) and never modified.", TheoryPart.INDIAN_CLASSICAL, listOf(0, 2, 4, 5, 7, 9, 11)),
    TheoryConcept("🕐", "Prahar (Time of Day)", "Ragas have specific performance times", "Indian classical tradition assigns each raga to a specific time of day or season. Morning ragas use komal Re/Dha. Evening ragas use tivra Ma.", TheoryPart.INDIAN_CLASSICAL, listOf(0, 1, 4, 6, 7, 8, 11)),
    TheoryConcept("🎭", "Nava Rasa (Nine Emotions)", "Music conveys 9 fundamental emotional states", "Indian aesthetics identifies 9 rasas: Shringara, Hasya, Karuna, Raudra, Veera, Bhayanaka, Bibhatsa, Adbhuta, Shanta. Each raga is associated with specific rasas.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("🥁", "Taal (Rhythmic Cycle)", "The cyclical rhythmic framework", "Taal is the rhythmic cycle of Indian classical music. Each taal has a fixed number of beats (matras) divided into sections (vibhags). Tabla provides the rhythmic foundation.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("🎯", "Sam (The Crucial Beat)", "Beat 1 — the most important rhythmic moment", "Sam is the first beat of a taal cycle. Soloist and tabla player must arrive at Sam simultaneously after improvisation.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("✋", "Khali (Empty Beat)", "The open, muted section of the taal", "Khali is the 'empty' section of a taal where the bass drum (bayan) is not struck. It provides contrast to the resonant sam sections.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("🎼", "Alankaar (Ornamental Exercise)", "Scale exercises for voice & instrument", "Alankars are systematic vocal/instrumental exercises using specific note patterns across all seven swaras. Develops dexterity and grammar control.", TheoryPart.INDIAN_CLASSICAL, listOf(0, 2, 4, 2, 4, 5, 4, 5, 7)),
    TheoryConcept("🎤", "Khayal", "The dominant Hindustani vocal form", "Khayal (meaning 'imagination') is the primary Hindustani classical vocal form. It begins with a slow bandish (composition), then expands through improvisation.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("📜", "Bandish", "A fixed composition in a raga and taal", "A bandish is a pre-composed song in a specific raga and taal. It consists of a sthayi (first verse) and antara (second verse).", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("〰", "Meend (Glide)", "A smooth pitch glide between notes", "Meend is the smooth glide from one note to another without any break. It is one of the most expressive ornaments in Indian classical music.", TheoryPart.INDIAN_CLASSICAL, listOf(0, 7)),
    TheoryConcept("〰〰", "Gamak (Oscillation)", "Rapid forceful note oscillations", "Gamak is the rapid, forceful oscillation between two notes. Unlike meend, gamak has a punchy, striking quality.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("🌊", "Andolan (Gentle Sway)", "A slow, gentle oscillation of a note", "Andolan is the soft, slow oscillation of certain raag-specific notes. Unlike gamak, andolan is gentle and introspective.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("🎭", "Dhrupad", "The most ancient Hindustani classical form", "Dhrupad is the oldest surviving Hindustani classical vocal form, with roots in temple music 1,000+ years ago. Austere, structured, and profound.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("🎵", "Thumri", "A lighter, romantic vocal genre", "Thumri is a semi-classical form emphasising romantic devotional themes (usually about Krishna). More freedom of raag interpretation than khayal.", TheoryPart.INDIAN_CLASSICAL, emptyList()),
    TheoryConcept("🎵", "Carnatic Music System", "The classical music of South India", "Carnatic music is the classical tradition of South India. While sharing roots with Hindustani music, it developed independently with distinct systems.", TheoryPart.KARNATIC, listOf(0, 1, 4, 5, 7, 8, 11)),
    TheoryConcept("📊", "72 Melakarta Raagas", "The parent scale system of Carnatic music", "Carnatic music organizes all ragas under 72 parent scales called Melakartas. Each Melakarta contains all 7 swaras.", TheoryPart.KARNATIC, listOf(0, 2, 4, 5, 7, 9, 11)),
    TheoryConcept("🏗️", "Chakra System", "12 groups of 6 Melakartas each", "The 72 Melakartas are grouped into 12 chakras. The first 36 use shuddha Madhyam; the second 36 use prathi Madhyam.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("〰", "Gamaka", "Ornaments fundamental to Carnatic music", "Gamakas are melodic ornaments central to Carnatic raga identity. Unlike Western music where notes are discrete, Carnatic notes are highly ornamented.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🏛️", "Kriti", "The core compositional form of Carnatic music", "A kriti is a complete composition in Carnatic music with a pallavi (first section), anupallavi (counter-melody), and charanam (verse).", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("📐", "Shruti (Microtones)", "22 microtonal divisions of the octave", "Ancient Indian music theory divides the octave into 22 shrutis. Performers naturally produce microtonal inflections that don't align with Western temperament.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🥁", "Tala System (Carnatic)", "The Carnatic rhythmic framework", "Carnatic tala uses a system of hand gestures: anudrutam (1 beat), drutam (2 beats), and laghu (variable beats).", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("✋", "Solkattu (Konokol)", "Vocal percussion in Carnatic music", "Konokol is the Carnatic art of spoken rhythmic syllables. Vocalists recite rhythmic patterns with precise timing.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🌿", "Varnam", "A foundational compositional form for study", "A varnam is a complete study piece used in Carnatic music education. It covers all melodic and rhythmic aspects of a raga systematically.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🏃", "Pallavi", "A single-line composition for elaborate improvisation", "In a ragam-tanam-pallavi format, the pallavi is a single line of music that forms the basis for elaborate improvisational development.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🎵", "Solfège Swaras", "The core syllables Sa Ri Ga Ma Pa Da Ni", "Carnatic layouts utilize specific linguistic structural updates (Ri/Da variants) compared to Northern systems.", TheoryPart.KARNATIC, listOf(0, 2, 4, 5, 7, 9, 11)),
    TheoryConcept("📐", "22 Microtonal Shrutis", "Advanced organic octave mapping", "An absolute microtonal split layout of the basic octave matrix allowing advanced organic shifts.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🔢", "Katapayadi Formula", "Ancient alphanumeric naming key", "An ancient Sanskrit alphanumeric hashing structure directly linking names to Melakarta structural parameters.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🎹", "Drone Box Foundation", "Continuous tonic baseline tracking", "Continuous tonic baseline layer mapping supporting real-time relative microtonal tracking.", TheoryPart.KARNATIC, listOf(0, 7, 12)),
    TheoryConcept("🎓", "Guru Lineage System", "Traditional lineage transfer methods", "Systematic validation methods relying directly on traditional master-apprentice transfers.", TheoryPart.KARNATIC, emptyList()),
    TheoryConcept("🏆", "The Core Trinity Masters", "Foundational pillar composers", "Tyagaraja, Muthuswami Dikshitar, and Syama Sastri's foundational pieces forming the structural core.", TheoryPart.KARNATIC, emptyList())
)

// ============================================================================
// 2. REFERENCE STUDIO PANEL (Chords, Inversions, Circle of 5ths, Subs, Scales, Arps, Theory)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceStudioPanel(
    viewModel: AppViewModel,
    onOpenChordGallery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("Chords") }
    val subTabs = remember {
        listOf("Chords", "Inversions", "Circle of 5ths", "Substitutions", "Scales", "Arpeggios", "Concepts")
    }

    val roots = STUDIO_ROOT_NAMES
    var selectedRoot by remember { mutableStateOf("C") }
    var selectedSuffix by remember { mutableStateOf("major") }
    var selectedScaleName by remember { mutableStateOf(STUDIO_SCALE_LIBRARY.keys.first()) }
    var selectedArpeggioName by remember { mutableStateOf(STUDIO_ARPEGGIO_LIBRARY.keys.first()) }
    var soundStyle by remember { mutableStateOf(InstrumentSoundStyle.SEQUENTIAL) }
    var showImmersiveLab by remember { mutableStateOf(false) }

    var selectedInstrumentType by remember { mutableStateOf(ChordInstrumentType.GUITAR_ACOUSTIC) }
    val selectedInstrument = remember(selectedInstrumentType) {
        when (selectedInstrumentType) {
            ChordInstrumentType.PIANO_ACOUSTIC, ChordInstrumentType.HARMONIUM -> "piano"
            ChordInstrumentType.UKULELE -> "ukulele"
            else -> "guitar"
        }
    }

    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedQualityCategory by remember { mutableStateOf("All") }

    val qualityCategories = remember {
        listOf("All", "Major", "Minor", "7th", "Extended", "Dim/Aug", "Sus")
    }

    val allSuffixes = remember(selectedInstrument) {
        if (selectedInstrument == "piano") {
            RealPianoChordsDb.suffixes
        } else {
            RealChordsDb.getAvailableSuffixes(selectedInstrument)
        }
    }

    val displaySuffixes = remember(activeSubTab, allSuffixes, searchQuery, selectedQualityCategory) {
        val baseList = if (activeSubTab == "Inversions") {
            listOf("major", "minor", "7", "maj7", "m7", "dim", "aug")
        } else allSuffixes

        baseList.filter { s ->
            val matchesSearch = if (searchQuery.isBlank()) true else s.contains(searchQuery.trim(), ignoreCase = true)
            val matchesCat = when (selectedQualityCategory) {
                "Major" -> s.contains("maj", ignoreCase = true) || s == "major" || s == "6"
                "Minor" -> s.contains("min", ignoreCase = true) || (s.startsWith("m") && !s.startsWith("maj"))
                "7th" -> s.contains("7")
                "Extended" -> s.contains("9") || s.contains("11") || s.contains("13")
                "Dim/Aug" -> s.contains("dim", ignoreCase = true) || s.contains("aug", ignoreCase = true) || s.contains("°")
                "Sus" -> s.contains("sus", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCat
        }
    }

    var selectedCagedFilter by remember { mutableStateOf("All CAGED") }
    var selectedFretRange by remember { mutableStateOf("All Frets") }

    val cagedFilterOptions = remember { listOf("All CAGED", "C-Shape", "A-Shape", "G-Shape", "E-Shape", "D-Shape") }
    val fretRangeFilterOptions = remember { listOf("All Frets", "Open (0-3)", "Low (1-5)", "Mid (5-9)", "High (9-14)", "12+ Frets") }

    val positions = remember(activeSubTab, selectedInstrument, selectedRoot, selectedSuffix, selectedCagedFilter, selectedFretRange) {
        val rawPositions = if (activeSubTab == "Inversions") {
            val invPositions = RealInversionsDb.getInversions(selectedRoot, selectedSuffix)
            if (invPositions.isNotEmpty()) {
                invPositions.map { invPos ->
                    RealChordPosition(
                        frets = invPos.frets,
                        fingers = invPos.fingers,
                        baseFret = invPos.baseFret,
                        barres = invPos.barres,
                        midi = emptyList()
                    )
                }
            } else {
                RealChordsDb.getGuitarPositions(selectedRoot, selectedSuffix)
            }
        } else if (selectedInstrument == "ukulele") {
            RealChordsDb.getUkulelePositions(selectedRoot, selectedSuffix)
        } else {
            RealChordsDb.getGuitarPositions(selectedRoot, selectedSuffix)
        }

        if (selectedInstrument == "guitar") {
            val filtered = rawPositions.filter { pos ->
                val matchesFretRange = when (selectedFretRange) {
                    "Open (0-3)" -> pos.baseFret in 0..3
                    "Low (1-5)" -> pos.baseFret in 1..5
                    "Mid (5-9)" -> pos.baseFret in 5..9
                    "High (9-14)" -> pos.baseFret in 9..14
                    "12+ Frets" -> pos.baseFret >= 12
                    else -> true
                }
                val matchesCaged = when (selectedCagedFilter) {
                    "C-Shape" -> (pos.frets.getOrNull(1) ?: -1) > 0 && (pos.frets.getOrNull(0) ?: -1) <= 0
                    "A-Shape" -> (pos.frets.getOrNull(1) ?: -1) >= 0 && (pos.frets.getOrNull(2) ?: -1) >= 0
                    "G-Shape" -> (pos.frets.getOrNull(0) ?: -1) > 0 && (pos.frets.getOrNull(5) ?: -1) > 0
                    "E-Shape" -> (pos.frets.getOrNull(0) ?: -1) >= 0 && (pos.frets.getOrNull(1) ?: -1) >= 0
                    "D-Shape" -> (pos.frets.getOrNull(0) ?: -1) < 0 && (pos.frets.getOrNull(1) ?: -1) < 0 && (pos.frets.getOrNull(2) ?: -1) >= 0
                    else -> true
                }
                matchesFretRange && matchesCaged
            }
            if (filtered.isNotEmpty()) filtered else rawPositions
        } else {
            rawPositions
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1218))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Fullscreen Lab & Gallery Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reference Studio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Interactive Chords, Inversions, Scales, Arpeggios & Theory Lab",
                    fontSize = 11.sp,
                    color = Color(0xFF9EABB8)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = { showImmersiveLab = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1F293D),
                        contentColor = Color(0xFF64B5F6)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("3D Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenChordGallery,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB347)),
                    border = BorderStroke(1.dp, Color(0xFFFFB347).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Instrument Switcher Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INSTRUMENT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )
            pEQInstrumentDropdown(
                selectedInstrument = selectedInstrumentType,
                accentColor = Color(0xFFFFB347),
                instrumentVolumes = viewModel.instrumentVolumes,
                onInstrumentVolumeChange = { inst, vol ->
                    if (viewModel.selectedInstrument.value == inst.title) {
                        viewModel.audioEngine.setVolume(vol * viewModel.masterVolume.value)
                    }
                },
                onInstrumentChange = { newInst ->
                    selectedInstrumentType = newInst
                    runCatching { viewModel.audioEngine.setInstrument(newInst.soundEngineName) }
                },
                modifier = Modifier.width(220.dp)
            )
        }

        // Subtabs Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(subTabs) { tab ->
                val isSel = activeSubTab == tab
                pEQFilterChip(
                    selected = isSel,
                    onClick = { activeSubTab = tab },
                    label = { Text(tab, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) }
                )
            }
        }

        // Root Note Selector Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ROOT NOTE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(roots) { r ->
                    val isSelected = selectedRoot == r
                    pEQFilterChip(
                        selected = isSelected,
                        onClick = { selectedRoot = r },
                        label = { Text(r, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        when (activeSubTab) {
            "Chords", "Inversions" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter qualities (e.g. 7#9, m7, sus4)...", fontSize = 12.sp, color = Color(0xFF6C788A)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64B5F6), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161A22),
                            unfocusedContainerColor = Color(0xFF13161C),
                            focusedBorderColor = Color(0xFF64B5F6),
                            unfocusedBorderColor = Color(0xFF262D3D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(qualityCategories) { cat ->
                            val isSel = selectedQualityCategory == cat
                            Surface(
                                onClick = { selectedQualityCategory = cat },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF64B5F6).copy(alpha = 0.22f) else Color(0xFF161A22),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF64B5F6) else Color(0xFF262D3D))
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color(0xFF90CAF9) else Color(0xFF9EABB8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (selectedInstrument == "guitar") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(cagedFilterOptions) { caged ->
                                val isSel = selectedCagedFilter == caged
                                Surface(
                                    onClick = { selectedCagedFilter = caged },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF4CAF50).copy(alpha = 0.22f) else Color(0xFF161A22),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF4CAF50) else Color(0xFF262D3D))
                                ) {
                                    Text(
                                        text = caged,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color(0xFF81C784) else Color(0xFF9EABB8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            items(fretRangeFilterOptions) { fretR ->
                                val isSel = selectedFretRange == fretR
                                Surface(
                                    onClick = { selectedFretRange = fretR },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF29B6F6).copy(alpha = 0.22f) else Color(0xFF161A22),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF29B6F6) else Color(0xFF262D3D))
                                ) {
                                    Text(
                                        text = fretR,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color(0xFF4FC3F7) else Color(0xFF9EABB8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "CHORD QUALITY / SUFFIX (${displaySuffixes.size} types)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E8B9B),
                        letterSpacing = 1.sp
                    )
                    if (displaySuffixes.isEmpty()) {
                        Text("No chord qualities match '$searchQuery'", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(displaySuffixes) { s ->
                                val isSelected = selectedSuffix == s
                                pEQFilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSuffix = s },
                                    label = { Text(s, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "$selectedRoot $selectedSuffix ($activeSubTab) • ${positions.size} Shapes Found",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64B5F6)
                )

                if (selectedInstrument == "piano") {
                    val pianoChordData = remember(selectedRoot, selectedSuffix) {
                        RealPianoChordsDb.getChord(selectedRoot, selectedSuffix)
                    }
                    val pianoMidiNotes = remember(pianoChordData, selectedRoot, selectedSuffix) {
                        if (pianoChordData != null && pianoChordData.midi.isNotEmpty()) {
                            pianoChordData.midi
                        } else {
                            val rIdx = roots.indexOf(selectedRoot).coerceAtLeast(0)
                            ChordVoicingEngine.buildPianoShape(
                                rootIdx = rIdx,
                                suffix = selectedSuffix,
                                style = PianoVoicingStyle.CLOSE,
                                inversion = InversionStyle.ROOT,
                                octave = 4
                            )
                        }
                    }
                    val activeMap = remember(pianoMidiNotes) { pianoMidiNotes.associateWith { true } }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val notesText = pianoChordData?.notes?.joinToString(" - ") ?: pianoMidiNotes.joinToString(" - ") { roots[it % 12] }
                            val intText = pianoChordData?.intervals?.joinToString(", ") ?: ""
                            Text(
                                text = "Notes: $notesText" + (if (intText.isNotBlank()) " • Intervals: $intText" else ""),
                                fontSize = 12.sp,
                                color = Color(0xFF9EABB8)
                            )

                            InstrumentPlayButton(
                                notes = pianoMidiNotes,
                                accentColor = Color(0xFFFF9E1B),
                                label = soundStyle.displayName,
                                soundStyle = soundStyle,
                                arpDelayMs = 45L,
                                onSoundStyleToggle = {
                                    soundStyle = when (soundStyle) {
                                        InstrumentSoundStyle.SEQUENTIAL -> InstrumentSoundStyle.ALL_AT_ONCE
                                        InstrumentSoundStyle.ALL_AT_ONCE -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                                        InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> InstrumentSoundStyle.SEQUENTIAL
                                    }
                                },
                                onPlay = { midi ->
                                    runCatching {
                                        viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                                        viewModel.audioEngine.noteOn(midi, 0.85f)
                                    }
                                }
                            )
                        }

                        UnifiedPianoKeyboard(
                            activeMidiNotes = activeMap,
                            accentColor = Color(0xFFFF9E1B),
                            startNote = 48,
                            endNote = 84,
                            fillWidth = true,
                            showNoteNames = true,
                            onKeyTapped = { midi ->
                                runCatching {
                                    viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(185.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(positions.indices.toList()) { idx ->
                            val pos = positions[idx]
                            SvgChordBox(
                                chord = pos,
                                chordName = "$selectedRoot$selectedSuffix #${idx + 1}",
                                instrument = selectedInstrument,
                                accentColor = Color(0xFF64B5F6),
                                cardBackground = Color(0xFF161A22),
                                width = 135.dp,
                                height = 175.dp,
                                onClick = {
                                    scope.launch {
                                        viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                                        pos.midi.forEach { midi ->
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                            delay(35L)
                                        }
                                        delay(650L)
                                        pos.midi.forEach { midi ->
                                            runCatching { viewModel.audioEngine.noteOff(midi) }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            "Circle of 5ths" -> {
                InteractiveCircleOfFifths(
                    selectedKey = selectedRoot,
                    onKeySelected = { selectedRoot = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Substitutions" -> {
                val rootIdx = roots.indexOf(selectedRoot).coerceAtLeast(0)
                val dynamicCadences = remember(rootIdx) {
                    listOf(
                        "${roots[(rootIdx + 6) % 12]}7 (Tritone Sub)" to listOf((rootIdx + 6) % 12, (rootIdx + 10) % 12, (rootIdx + 1) % 12, (rootIdx + 4) % 12),
                        "${roots[(rootIdx + 9) % 12]}m7 (Relative Minor)" to listOf((rootIdx + 9) % 12, rootIdx, (rootIdx + 4) % 12, (rootIdx + 7) % 12),
                        "${roots[(rootIdx + 2) % 12]}7 (V/V Secondary Dom)" to listOf((rootIdx + 2) % 12, (rootIdx + 6) % 12, (rootIdx + 9) % 12, rootIdx),
                        "${roots[(rootIdx + 10) % 12]}7 (♭VII7 Backdoor)" to listOf((rootIdx + 10) % 12, (rootIdx + 2) % 12, (rootIdx + 5) % 12, (rootIdx + 8) % 12),
                        "${roots[(rootIdx + 5) % 12]}m6 (Minor Plagal)" to listOf((rootIdx + 5) % 12, (rootIdx + 8) % 12, rootIdx, (rootIdx + 2) % 12)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EasyChordQuickRef(
                        rootIdx = rootIdx,
                        qualityIdx = 0,
                        cadences = dynamicCadences,
                        accentColor = Color(0xFFFFB347),
                        onSelectChord = { name ->
                            val targetNotes = dynamicCadences.firstOrNull { it.first == name }?.second.orEmpty()
                            scope.launch {
                                runCatching { viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName) }
                                targetNotes.forEach { pc ->
                                    runCatching { viewModel.audioEngine.noteOn(60 + pc, 0.85f) }
                                    delay(45L)
                                }
                            }
                        }
                    )

                    val subsList = remember(selectedRoot, selectedSuffix, rootIdx) {
                        val tritoneName = "${roots[(rootIdx + 1) % 12]}7 → ${roots[rootIdx]}maj7"
                        val relMinor = "${roots[(rootIdx + 9) % 12]}m7 ↔ ${roots[rootIdx]}maj7"
                        val secDom = "${roots[(rootIdx + 2) % 12]}7 → ${roots[(rootIdx + 7) % 12]}7 → ${roots[rootIdx]}"
                        val backdoor = "${roots[(rootIdx + 5) % 12]}m7 - ${roots[(rootIdx + 10) % 12]}7 → ${roots[rootIdx]}maj7"
                        val iiV = "${roots[(rootIdx + 2) % 12]}m7 - ${roots[(rootIdx + 7) % 12]}7 → ${roots[rootIdx]}maj7"
                        val dimPass = "${roots[(rootIdx + 1) % 12]}°7 → ${roots[(rootIdx + 2) % 12]}m7"
                        listOf(
                            Triple("Tritone Substitution", "Substitute V7 with the dominant 7th a tritone (6 semitones) away sharing the same 3rd & 7th guide tones.", tritoneName),
                            Triple("Relative Minor / Major", "Shares 3 common chord tones for a softer, more intimate tonic resolution.", relMinor),
                            Triple("Secondary Dominant (V/V)", "Precedes the dominant chord with its own V7 to drive harmonic momentum.", secDom),
                            Triple("Backdoor Dominant (♭VII7)", "Resolves into the tonic from a whole step below, borrowed from parallel Aeolian.", backdoor),
                            Triple("ii - V - I Jazz Preparation", "Precede any target chord with its diatonic supertonic-dominant pair.", iiV),
                            Triple("Chromatic Diminished Passing", "Connects diatonic chords half-step apart with a symmetrical °7 chord.", dimPass)
                        )
                    }

                    subsList.forEach { (rule, desc, example) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF161A22),
                            border = BorderStroke(1.dp, Color(0xFF262D3D))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rule, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFF9E1B).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            example,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFFB347),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(desc, fontSize = 11.sp, color = Color(0xFF9EABB8))
                            }
                        }
                    }
                }
            }
            "Scales", "Arpeggios" -> {
                val isScalesTab = activeSubTab == "Scales"
                val patternMap = if (isScalesTab) STUDIO_SCALE_LIBRARY else STUDIO_ARPEGGIO_LIBRARY
                val activePatternName = if (isScalesTab) selectedScaleName else selectedArpeggioName
                val scaleIntervals = patternMap[activePatternName] ?: listOf(0, 2, 4, 5, 7, 9, 11)
                val rootIdx = roots.indexOf(selectedRoot).coerceAtLeast(0)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(patternMap.keys.toList()) { name ->
                            val isSel = activePatternName == name
                            pEQFilterChip(
                                selected = isSel,
                                onClick = {
                                    if (isScalesTab) selectedScaleName = name else selectedArpeggioName = name
                                },
                                label = { Text(name, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedRoot $activePatternName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )

                        val scaleNotes = remember(rootIdx, scaleIntervals) {
                            val baseMidi = 60 + rootIdx
                            (scaleIntervals + listOf(12)).map { baseMidi + it }
                        }
                        InstrumentPlayButton(
                            notes = scaleNotes,
                            accentColor = Color(0xFF81C784),
                            label = soundStyle.displayName,
                            soundStyle = soundStyle,
                            arpDelayMs = 115L,
                            onSoundStyleToggle = {
                                soundStyle = when (soundStyle) {
                                    InstrumentSoundStyle.SEQUENTIAL -> InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP
                                    InstrumentSoundStyle.ARPEGGIATE_ROUNDTRIP -> InstrumentSoundStyle.ALL_AT_ONCE
                                    InstrumentSoundStyle.ALL_AT_ONCE -> InstrumentSoundStyle.SEQUENTIAL
                                }
                            },
                            onPlay = { midi ->
                                runCatching {
                                    viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                }
                            }
                        )
                    }

                    if (selectedInstrument == "piano") {
                        val activeScalePitches = remember(rootIdx, scaleIntervals) {
                            val baseMidi = 60 + rootIdx
                            (scaleIntervals + listOf(12)).map { baseMidi + it }.associateWith { true }
                        }
                        UnifiedPianoKeyboard(
                            activeMidiNotes = activeScalePitches,
                            accentColor = Color(0xFF81C784),
                            startNote = 48,
                            endNote = 84,
                            fillWidth = true,
                            showNoteNames = true,
                            onKeyTapped = { midi ->
                                runCatching {
                                    viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        val openStringsMidi = if (selectedInstrument == "ukulele") {
                            listOf(69, 64, 60, 67)
                        } else {
                            listOf(64, 59, 55, 50, 45, 40)
                        }
                        val playableScaleMidi = remember(rootIdx, scaleIntervals, selectedInstrument) {
                            BiomechanicalPerformersEngine.solvePlayableScaleBoxRun(
                                rootIdx = rootIdx,
                                scaleIntervals = scaleIntervals.map { it % 12 }.distinct(),
                                openStringsMidiHighToLow = openStringsMidi,
                                boxStartFret = 0,
                                boxSpanFrets = 4,
                                numOctaves = 2
                            ).map { it.third }
                        }
                        UnifiedGuitarFretboard(
                            numFrets = 15,
                            activeNotes = playableScaleMidi,
                            rootNote = rootIdx,
                            accentColor = Color(0xFF81C784),
                            showNoteNames = true,
                            markOctaves = true,
                            highlightOctaveC = true,
                            stringLabels = if (selectedInstrument == "ukulele") listOf("A", "E", "C", "G") else listOf("e", "B", "G", "D", "A", "E"),
                            openStringsSemis = if (selectedInstrument == "ukulele") listOf(9, 4, 0, 7) else listOf(4, 11, 7, 2, 9, 4),
                            baseFret = 1,
                            onFretTapped = { _, _, midi ->
                                scope.launch {
                                    runCatching {
                                        viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                                        viewModel.audioEngine.noteOn(midi, 0.85f)
                                        delay(380L)
                                        viewModel.audioEngine.noteOff(midi)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        scaleIntervals.forEach { semiOffset ->
                            val noteName = roots[(rootIdx + semiOffset) % 12]
                            val degreeName = DEGREE_LABELS[semiOffset % 12]
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF162219),
                                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    runCatching {
                                        viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                                        viewModel.audioEngine.noteOn(60 + rootIdx + semiOffset, 0.88f)
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(noteName, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text(degreeName, fontSize = 9.5.sp, color = Color(0xFF81C784))
                                }
                            }
                        }
                    }
                }
            }
            "Concepts" -> {
                InteractiveTheoryConceptsBrowser(
                    viewModel = viewModel,
                    selectedRoot = selectedRoot,
                    selectedInstrumentType = selectedInstrumentType
                )
            }
        }
    }

    if (showImmersiveLab) {
        val rootIdx = roots.indexOf(selectedRoot).coerceAtLeast(0)
        ImmersiveKeyboardFretboardLabDialog(
            viewModel = viewModel,
            rootIdx = rootIdx,
            qualityIdx = 0,
            instrumentType = selectedInstrumentType,
            accentColor = Color(0xFFFF9E1B),
            onDismiss = { showImmersiveLab = false }
        )
    }
}

/**
 * Interactive Theory Concepts Explorer powered by `ALL_CONCEPTS` with instant audio/visual auditioning.
 */
@Composable
private fun InteractiveTheoryConceptsBrowser(
    viewModel: AppViewModel,
    selectedRoot: String,
    selectedInstrumentType: ChordInstrumentType
) {
    val scope = rememberCoroutineScope()
    var selectedPart by remember { mutableStateOf<TheoryPart?>(null) }
    var conceptQuery by remember { mutableStateOf("") }
    val rootIdx = STUDIO_ROOT_NAMES.indexOf(selectedRoot).coerceAtLeast(0)

    val filteredConcepts = remember(selectedPart, conceptQuery) {
        ALL_CONCEPTS.filter { c ->
            val partMatch = selectedPart == null || c.part == selectedPart
            val queryMatch = conceptQuery.isBlank() ||
                c.title.contains(conceptQuery, ignoreCase = true) ||
                c.subtitle.contains(conceptQuery, ignoreCase = true) ||
                c.description.contains(conceptQuery, ignoreCase = true)
            partMatch && queryMatch
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = conceptQuery,
            onValueChange = { conceptQuery = it },
            placeholder = { Text("Search ${ALL_CONCEPTS.size} music theory, raga, carnatic & groove concepts...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFFFB347), modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                pEQFilterChip(
                    selected = selectedPart == null,
                    onClick = { selectedPart = null },
                    label = { Text("All (${ALL_CONCEPTS.size})", fontSize = 11.sp) }
                )
            }
            items(TheoryPart.entries.toList()) { part ->
                pEQFilterChip(
                    selected = selectedPart == part,
                    onClick = { selectedPart = part },
                    label = { Text(part.name.replace("_", " "), fontSize = 11.sp) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredConcepts.forEach { concept ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF161A22),
                    border = BorderStroke(1.dp, Color(0xFF262D3D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(concept.icon, fontSize = 18.sp)
                                Column {
                                    Text(concept.title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(concept.subtitle, fontSize = 11.sp, color = Color(0xFFFFB347))
                                }
                            }
                            if (concept.intervals.isNotEmpty()) {
                                FilledTonalIconButton(
                                    onClick = {
                                        scope.launch {
                                            runCatching { viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName) }
                                            concept.intervals.forEach { st ->
                                                runCatching { viewModel.audioEngine.noteOn(60 + rootIdx + st, 0.85f) }
                                                delay(110L)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Audition Concept", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        Text(concept.description, fontSize = 11.5.sp, color = Color(0xFF9EABB8), lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. PROGRESSIONS STUDIO PANEL (Zero Duplication — Uses Shared Studio Tools)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionsStudioPanel(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val roots = STUDIO_ROOT_NAMES
    var selectedKey by remember { mutableStateOf("C") }
    var selectedInstrumentType by remember { mutableStateOf(ChordInstrumentType.GUITAR_ACOUSTIC) }
    val selectedInstrument = remember(selectedInstrumentType) {
        when (selectedInstrumentType) {
            ChordInstrumentType.PIANO_ACOUSTIC, ChordInstrumentType.HARMONIUM -> "piano"
            ChordInstrumentType.UKULELE -> "ukulele"
            else -> "guitar"
        }
    }

    val allCategories = remember {
        listOf("All") + RealProgressionsDb.list.map { it.category }.distinct()
    }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedProgressionId by remember { mutableStateOf(RealProgressionsDb.list.first().id) }

    val filteredProgressions = remember(selectedCategory) {
        if (selectedCategory == "All") RealProgressionsDb.list
        else RealProgressionsDb.list.filter { it.category == selectedCategory }
    }

    val currentProgression = remember(selectedProgressionId) {
        RealProgressionsDb.list.find { it.id == selectedProgressionId } ?: RealProgressionsDb.list.first()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingChordIdx by remember { mutableIntStateOf(-1) }

    fun resolveChordForDegree(tonic: String, degreeRoman: String): Pair<String, String> {
        val rootIdx = roots.indexOf(tonic).coerceAtLeast(0)
        val degNum = when (degreeRoman.replace("7", "").replace("maj7", "").replace("m", "").replace("ø", "")) {
            "I", "i" -> 0
            "♭II", "♭ii" -> 1
            "II", "ii" -> 2
            "♭III", "♭iii" -> 3
            "III", "iii" -> 4
            "IV", "iv" -> 5
            "♭V", "♭v" -> 6
            "V", "v" -> 7
            "♭VI", "♭vi" -> 8
            "VI", "vi" -> 9
            "♭VII", "♭vii" -> 10
            "VII", "vii" -> 11
            else -> 0
        }
        val chordRoot = roots[(rootIdx + degNum) % 12]
        val suffix = when {
            degreeRoman.contains("7") && (degreeRoman.startsWith("V") || degreeRoman.startsWith("I") || degreeRoman.startsWith("IV") || degreeRoman.contains("♭VII")) -> "7"
            degreeRoman.contains("maj7") -> "maj7"
            degreeRoman.contains("m7") -> "m7"
            degreeRoman.contains("ø7") -> "m7b5"
            degreeRoman.first().isLowerCase() -> "minor"
            else -> "major"
        }
        return chordRoot to suffix
    }

    data class ProgressionChordItem(
        val degree: String,
        val chordName: String,
        val guitarPos: RealChordPosition?,
        val pianoMidi: List<Int>
    )

    val progressionChords = remember(currentProgression, selectedKey, selectedInstrument) {
        currentProgression.degrees.map { deg ->
            val (cRoot, cSuffix) = resolveChordForDegree(selectedKey, deg)
            val posList = if (selectedInstrument == "ukulele") {
                RealChordsDb.getUkulelePositions(cRoot, cSuffix)
            } else {
                RealChordsDb.getGuitarPositions(cRoot, cSuffix)
            }
            val pianoPitches = RealPianoChordsDb.getChord(cRoot, cSuffix)?.midi?.takeIf { it.isNotEmpty() }
                ?: run {
                    val rIdx = roots.indexOf(cRoot).coerceAtLeast(0)
                    ChordVoicingEngine.buildPianoShape(
                        rootIdx = rIdx,
                        suffix = cSuffix,
                        style = PianoVoicingStyle.CLOSE,
                        inversion = InversionStyle.ROOT,
                        octave = 4
                    )
                }
            ProgressionChordItem(deg, "$cRoot $cSuffix", posList.firstOrNull(), pianoPitches)
        }
    }

    fun playProgression() {
        if (isPlaying) {
            isPlaying = false
            currentPlayingChordIdx = -1
            return
        }
        isPlaying = true
        scope.launch {
            try {
                viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                for (i in progressionChords.indices) {
                    if (!isPlaying) break
                    currentPlayingChordIdx = i
                    val chordItem = progressionChords[i]
                    val midiNotes = if (selectedInstrument == "piano") {
                        chordItem.pianoMidi
                    } else {
                        chordItem.guitarPos?.midi?.takeIf { it.isNotEmpty() } ?: chordItem.pianoMidi
                    }
                    midiNotes.forEach { midi ->
                        viewModel.audioEngine.noteOn(midi, 0.85f)
                        delay(35L)
                    }
                    delay(650L)
                    midiNotes.forEach { midi ->
                        runCatching { viewModel.audioEngine.noteOff(midi) }
                    }
                    delay(650L)
                }
            } catch (_: Exception) {}
            isPlaying = false
            currentPlayingChordIdx = -1
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1218))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chord Progressions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Classic Songwriter Cadences & Harmonic Loops",
                    fontSize = 11.sp,
                    color = Color(0xFF9EABB8)
                )
            }

            InstrumentPlayButton(
                isPlaying = isPlaying,
                accentColor = Color(0xFFAB47BC),
                label = if (isPlaying) "Stop" else "Play Progression",
                onClick = { playProgression() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE INSTRUMENT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )
            pEQInstrumentDropdown(
                selectedInstrument = selectedInstrumentType,
                accentColor = Color(0xFFAB47BC),
                instrumentVolumes = viewModel.instrumentVolumes,
                onInstrumentVolumeChange = { inst, vol ->
                    if (viewModel.selectedInstrument.value == inst.title) {
                        viewModel.audioEngine.setVolume(vol * viewModel.masterVolume.value)
                    }
                },
                onInstrumentChange = {
                    selectedInstrumentType = it
                    runCatching { viewModel.audioEngine.setInstrument(it.soundEngineName) }
                },
                modifier = Modifier.width(220.dp)
            )
        }

        val currentChordItem = progressionChords.getOrNull(if (currentPlayingChordIdx >= 0) currentPlayingChordIdx else 0)
        val activeChordPitches = currentChordItem?.pianoMidi ?: emptyList()
        pEQDynamicInstrumentVisualizer(
            selectedInstrument = selectedInstrumentType,
            activeNotes = activeChordPitches,
            accentColor = Color(0xFFAB47BC),
            onNoteTapped = { midi ->
                runCatching {
                    viewModel.audioEngine.setInstrument(selectedInstrumentType.soundEngineName)
                    viewModel.audioEngine.noteOn(midi, 0.85f)
                }
            }
        )

        // Root Key Transposer Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "KEY CENTER (TRANSPOSER)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(roots) { r ->
                    val isSelected = selectedKey == r
                    pEQFilterChip(
                        selected = isSelected,
                        onClick = { selectedKey = r },
                        label = { Text(r, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        // Genre Categories Filter Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(allCategories) { cat ->
                val isSelected = selectedCategory == cat
                pEQFilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Current Selected Progression Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color(0xFF262D3D))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentProgression.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCE93D8)
                    )
                    Text(
                        text = currentProgression.romanDisplay,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFB347)
                    )
                }

                Text(
                    text = currentProgression.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                if (currentProgression.examples.isNotBlank()) {
                    Text(
                        text = "Examples: ${currentProgression.examples}",
                        fontSize = 11.sp,
                        color = Color(0xFF81C784)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(progressionChords.indices.toList()) { idx ->
                        val chordItem = progressionChords[idx]
                        val isCurrentChordPlaying = isPlaying && currentPlayingChordIdx == idx

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = chordItem.degree,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFB0BEC5)
                            )
                            Spacer(Modifier.height(4.dp))
                            if (selectedInstrument == "piano") {
                                val activeMap = remember(chordItem.pianoMidi, isCurrentChordPlaying) {
                                    chordItem.pianoMidi.associateWith { true }
                                }
                                Surface(
                                    modifier = Modifier
                                        .width(170.dp)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch {
                                                viewModel.audioEngine.setInstrument("PIANO")
                                                chordItem.pianoMidi.forEach { midi ->
                                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                                    delay(30L)
                                                }
                                            }
                                        },
                                    color = if (isCurrentChordPlaying) Color(0xFF2A2038) else Color(0xFF1B1F2A),
                                    border = BorderStroke(1.dp, if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFF333D4F)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            chordItem.chordName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFCE93D8)
                                        )
                                        UnifiedPianoKeyboard(
                                            activeMidiNotes = activeMap,
                                            accentColor = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFCE93D8),
                                            startNote = 48,
                                            endNote = 72,
                                            fillWidth = true,
                                            showNoteNames = false,
                                            modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            } else if (chordItem.guitarPos != null) {
                                SvgChordBox(
                                    chord = chordItem.guitarPos,
                                    chordName = chordItem.chordName,
                                    instrument = selectedInstrument,
                                    accentColor = if (isCurrentChordPlaying) Color(0xFFFF9E1B) else Color(0xFFCE93D8),
                                    cardBackground = if (isCurrentChordPlaying) Color(0xFF2A2038) else Color(0xFF1B1F2A),
                                    width = 110.dp,
                                    height = 145.dp,
                                    onClick = {
                                        scope.launch {
                                            viewModel.audioEngine.setInstrument("GUITAR")
                                            chordItem.guitarPos.midi.forEach { midi ->
                                                viewModel.audioEngine.noteOn(midi, 0.85f)
                                                delay(35L)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Progression Library List
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PROGRESSION LIBRARY (${filteredProgressions.size})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7E8B9B),
                letterSpacing = 1.sp
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProgressions) { p ->
                    val isSelected = p.id == selectedProgressionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedProgressionId = p.id
                                isPlaying = false
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF231B2D) else Color(0xFF151821),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFAB47BC) else Color(0xFF242A38))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFCE93D8) else Color.White
                                )
                                Text(
                                    text = "${p.category} • ${p.description}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8C98A8)
                                )
                            }

                            Text(
                                text = p.romanDisplay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFB347)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. TOOLS STUDIO PANEL (Includes Capo Calculator, Songwriter Transpose, Reverse Chord Finder, Scale Finder, Metronome)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsStudioPanel(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeTool by remember { mutableStateOf("Transposer") }
    val toolList = remember {
        listOf(
            "Transposer",
            "Capo Calculator",
            "Songwriting Ideas",
            "Circle of 5ths",
            "Key Finder",
            "Chord Finder",
            "Scale Finder",
            "Metronome",
            "Time Signatures"
        )
    }
    val roots = STUDIO_ROOT_NAMES

    // Shared states for CapoSection & TransposeSection when opened inside ToolsStudioPanel
    var toolCapoFret by remember { mutableIntStateOf(2) }
    var toolActiveRootIdx by remember { mutableIntStateOf(0) }
    var toolFromKeyIdx by remember { mutableIntStateOf(0) }
    var toolToKeyIdx by remember { mutableIntStateOf(7) }
    var toolScaleIdx by remember { mutableIntStateOf(0) }
    val scaleFormulaList = remember { STUDIO_SCALE_LIBRARY.entries.map { it.key to it.value } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1218))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Musical Tools & Calculators",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Transposer, Capo Chart, Reverse Chord/Scale Finder, Metronome & Odd Meters",
                    fontSize = 11.sp,
                    color = Color(0xFF9EABB8)
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(toolList) { tool ->
                val isSelected = activeTool == tool
                pEQFilterChip(
                    selected = isSelected,
                    onClick = { activeTool = tool },
                    label = { Text(tool, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color(0xFF262D3D))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (activeTool) {
                    "Transposer" -> TransposerTool(viewModel = viewModel, roots = roots)
                    "Capo Calculator" -> CapoSection(
                        capoFret = toolCapoFret,
                        activeRootIdx = toolActiveRootIdx,
                        onCapoChange = { toolCapoFret = it },
                        partColor = Color(0xFFFFB347),
                        onKeyDispatched = { newRoot ->
                            toolActiveRootIdx = newRoot
                            runCatching {
                                viewModel.audioEngine.setInstrument("GUITAR")
                                viewModel.audioEngine.noteOn(60 + newRoot, 0.88f)
                                viewModel.audioEngine.noteOn(64 + newRoot, 0.82f)
                                viewModel.audioEngine.noteOn(67 + newRoot, 0.82f)
                            }
                        }
                    )
                    "Songwriting Ideas" -> TransposeSection(
                        fromKeyIdx = toolFromKeyIdx,
                        toKeyIdx = toolToKeyIdx,
                        selectedScaleIdx = toolScaleIdx,
                        scaleFormulas = scaleFormulaList,
                        partColor = Color(0xFFCE93D8),
                        onFromChange = { toolFromKeyIdx = it },
                        onToChange = { toolToKeyIdx = it },
                        onKeyDispatched = { newRoot -> toolActiveRootIdx = newRoot }
                    )
                    "Circle of 5ths" -> {
                        var circleKey by remember { mutableStateOf("C") }
                        InteractiveCircleOfFifths(
                            selectedKey = circleKey,
                            onKeySelected = { circleKey = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "Key Finder" -> KeyFinderTool(roots = roots)
                    "Chord Finder" -> ChordFinderTool(viewModel = viewModel, roots = roots)
                    "Scale Finder" -> ScaleFinderTool(viewModel = viewModel, roots = roots)
                    "Metronome" -> MetronomeTool(viewModel = viewModel)
                    "Time Signatures" -> TimeSignaturesTool(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun TransposerTool(viewModel: AppViewModel, roots: List<String>) {
    var fromKey by remember { mutableStateOf("C") }
    var toKey by remember { mutableStateOf("G") }
    var inputChords by remember { mutableStateOf("C, Am, F, G") }

    val fromIdx = roots.indexOf(fromKey).coerceAtLeast(0)
    val toIdx = roots.indexOf(toKey).coerceAtLeast(0)
    val semitoneDiff = (toIdx - fromIdx + 12) % 12

    val transposedChords = remember(inputChords, semitoneDiff) {
        inputChords.split(",", " ").filter { it.isNotBlank() }.map { token ->
            val clean = token.trim()
            val matchRoot = roots.sortedByDescending { it.length }.firstOrNull { clean.startsWith(it) }
            if (matchRoot != null) {
                val oldRootIdx = roots.indexOf(matchRoot)
                val newRoot = roots[(oldRootIdx + semitoneDiff) % 12]
                clean.replaceFirst(matchRoot, newRoot)
            } else clean
        }.joinToString("  •  ")
    }

    val chordList = remember(transposedChords) {
        transposedChords.split("  •  ").map { it.trim() }.filter { it.isNotEmpty() }
    }
    var previewChord by remember(chordList) { mutableStateOf(chordList.firstOrNull() ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Chord Transposer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB347))
        Text("Transpose any sequence of chords cleanly across musical keys.", fontSize = 12.sp, color = Color.Gray)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("FROM KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(roots) { r ->
                        pEQFilterChip(selected = fromKey == r, onClick = { fromKey = r }, label = { Text(r) })
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TO KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(roots) { r ->
                        pEQFilterChip(selected = toKey == r, onClick = { toKey = r }, label = { Text(r) })
                    }
                }
            }
        }

        OutlinedTextField(
            value = inputChords,
            onValueChange = { inputChords = it },
            label = { Text("Chord Progression") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF222938)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TRANSPOSED RESULT ($fromKey → $toKey, +$semitoneDiff st):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                Text(transposedChords, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9E1B))

                if (chordList.isNotEmpty()) {
                    val scope = rememberCoroutineScope()
                    var selectedToolInstrument by remember { mutableStateOf(ChordInstrumentType.PIANO_ACOUSTIC) }
                    var isPlayingChord by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select chord to view:", fontSize = 10.sp, color = Color.Gray)
                        pEQInstrumentDropdown(
                            selectedInstrument = selectedToolInstrument,
                            accentColor = Color(0xFFFF9E1B),
                            instrumentVolumes = viewModel.instrumentVolumes,
                            onInstrumentVolumeChange = { inst, vol ->
                                if (viewModel.selectedInstrument.value == inst.title) {
                                    viewModel.audioEngine.setVolume(vol * viewModel.masterVolume.value)
                                }
                            },
                            onInstrumentChange = { selectedToolInstrument = it },
                            modifier = Modifier.width(180.dp)
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(chordList) { ch ->
                            val isSel = previewChord == ch
                            pEQFilterChip(
                                selected = isSel,
                                onClick = { previewChord = ch },
                                label = { Text(ch, fontSize = 11.sp) }
                            )
                        }
                    }

                    val pianoPitches = remember(previewChord) {
                        val clean = previewChord.trim()
                        val matchedRoot = roots.sortedByDescending { it.length }.firstOrNull { clean.startsWith(it) } ?: "C"
                        val rawSuffix = clean.removePrefix(matchedRoot).trim().ifEmpty { "major" }
                        val cSuffix = if (rawSuffix == "m") "minor" else rawSuffix
                        RealPianoChordsDb.getChord(matchedRoot, cSuffix)?.midi?.takeIf { it.isNotEmpty() }
                            ?: run {
                                val rIdx = roots.indexOf(matchedRoot).coerceAtLeast(0)
                                ChordVoicingEngine.buildPianoShape(
                                    rootIdx = rIdx,
                                    suffix = rawSuffix,
                                    style = PianoVoicingStyle.CLOSE,
                                    inversion = InversionStyle.ROOT,
                                    octave = 4
                                )
                            }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audition: $previewChord",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9E1B)
                        )
                        InstrumentPlayButton(
                            isPlaying = isPlayingChord,
                            accentColor = Color(0xFFFF9E1B),
                            label = if (isPlayingChord) "Stop" else "Play Chord",
                            onClick = {
                                if (isPlayingChord) {
                                    isPlayingChord = false
                                } else {
                                    isPlayingChord = true
                                    scope.launch {
                                        try {
                                            viewModel.audioEngine.setInstrument(selectedToolInstrument.soundEngineName)
                                            pianoPitches.forEach { midi ->
                                                viewModel.audioEngine.noteOn(midi, 0.85f)
                                                delay(40L)
                                            }
                                            delay(500L)
                                        } finally {
                                            isPlayingChord = false
                                        }
                                    }
                                }
                            }
                        )
                    }

                    if (pianoPitches.isNotEmpty()) {
                        pEQDynamicInstrumentVisualizer(
                            selectedInstrument = selectedToolInstrument,
                            activeNotes = pianoPitches,
                            accentColor = Color(0xFFFF9E1B),
                            onNoteTapped = { midi ->
                                runCatching {
                                    viewModel.audioEngine.setInstrument(selectedToolInstrument.soundEngineName)
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyFinderTool(roots: List<String>) {
    var selectedChords by remember { mutableStateOf(setOf("C", "G", "Am", "F")) }
    val commonChords = remember {
        listOf("C", "Dm", "Em", "F", "G", "Am", "Bdim", "D", "A", "E", "Bm", "F#m", "C#m", "Bb", "Eb", "Gm", "Cm", "Fm")
    }

    val keyScores = remember(selectedChords) {
        roots.map { root ->
            val rootIdx = roots.indexOf(root)
            val diatonicTriads = listOf(
                roots[rootIdx],
                roots[(rootIdx + 2) % 12] + "m",
                roots[(rootIdx + 4) % 12] + "m",
                roots[(rootIdx + 5) % 12],
                roots[(rootIdx + 7) % 12],
                roots[(rootIdx + 9) % 12] + "m",
                roots[(rootIdx + 11) % 12] + "dim"
            )
            val matches = selectedChords.count { it in diatonicTriads }
            Triple(root, roots[(rootIdx + 9) % 12] + "m", matches)
        }.sortedByDescending { it.third }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Key Finder", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
        Text("Select the chords in your song to identify the parent Major & Relative Minor key signatures.", fontSize = 12.sp, color = Color.Gray)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(commonChords) { c ->
                val isSel = c in selectedChords
                pEQFilterChip(
                    selected = isSel,
                    onClick = {
                        selectedChords = if (isSel) selectedChords - c else selectedChords + c
                    },
                    label = { Text(c) }
                )
            }
        }

        Text("BEST MATCHING KEYS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E8B9B))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keyScores.take(4).forEach { (majKey, relMin, score) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2838),
                    border = BorderStroke(1.dp, Color(0xFF3B4860)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$majKey Major", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB347))
                        Text("Rel. Minor: $relMin", fontSize = 11.sp, color = Color(0xFF81C784))
                        Text("$score / ${selectedChords.size} Chords Match", fontSize = 10.5.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChordFinderTool(viewModel: AppViewModel, roots: List<String>) {
    var frets by remember { mutableStateOf(listOf(-1, 3, 2, 0, 1, 0)) }
    val stringOpenMidi = remember { listOf(64, 59, 55, 50, 45, 40) }
    var selectedFinderInstrument by remember { mutableStateOf(ChordInstrumentType.GUITAR_ACOUSTIC) }
    var isPlayingVoicing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val soundedNotes = remember(frets) {
        val list = mutableListOf<Pair<String, Int>>()
        for (i in 0..5) {
            val f = frets[i]
            if (f >= 0) {
                val openMidi = stringOpenMidi[5 - i]
                val midi = openMidi + f
                val noteName = roots[midi % 12]
                list.add(noteName to midi)
            }
        }
        list
    }

    val detectedName = remember(soundedNotes) {
        val pitches = soundedNotes.map { it.second }
        if (pitches.isEmpty()) {
            "Muted / No Frets Selected"
        } else {
            val live = LiveChordDetector.detect(pitches)
            live?.fullName ?: "${soundedNotes.first().first} (${soundedNotes.map { it.first }.distinct().joinToString("-")})"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Reverse Chord Finder (Find Chords by Shape)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8))
                Text("Tap frets directly on the interactive Unified Fretboard to identify the chord.", fontSize = 12.sp, color = Color.Gray)
            }

            pEQInstrumentDropdown(
                selectedInstrument = selectedFinderInstrument,
                accentColor = Color(0xFFCE93D8),
                instrumentVolumes = viewModel.instrumentVolumes,
                onInstrumentVolumeChange = { inst, vol ->
                    if (viewModel.selectedInstrument.value == inst.title) {
                        viewModel.audioEngine.setVolume(vol * viewModel.masterVolume.value)
                    }
                },
                onInstrumentChange = { selectedFinderInstrument = it },
                modifier = Modifier.width(180.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161922),
            border = BorderStroke(1.dp, Color(0xFF333C4E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOUCH FRETBOARD (TAP TO SET FRETS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCE93D8),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tab: [${frets.joinToString(", ") { if (it == -1) "X" else "$it" }}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9E1B)
                    )
                }

                UnifiedGuitarFretboard(
                    numFrets = 15,
                    accentColor = Color(0xFFCE93D8),
                    showNoteNames = true,
                    markOctaves = true,
                    highlightOctaveC = true,
                    baseFret = 1,
                    chordFrets = frets.reversed(),
                    fretMinConstrain = 0,
                    fretMaxConstrain = 15,
                    openStringsSemis = listOf(4, 11, 7, 2, 9, 4),
                    stringLabels = listOf("e", "B", "G", "D", "A", "E"),
                    activeNotes = soundedNotes.map { it.second },
                    onFretTapped = { strIdx: Int, fretIdx: Int, midi: Int ->
                        val guitarStringIdx = 5 - strIdx
                        val currentFret = frets[guitarStringIdx]
                        val nextFrets = frets.toMutableList()
                        nextFrets[guitarStringIdx] = if (currentFret == fretIdx) -1 else fretIdx
                        frets = nextFrets
                        scope.launch {
                            runCatching {
                                viewModel.audioEngine.setInstrument(selectedFinderInstrument.soundEngineName)
                                viewModel.audioEngine.noteOn(midi, 0.85f)
                                delay(350L)
                                viewModel.audioEngine.noteOff(midi)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { frets = listOf(-1, -1, -1, -1, -1, -1) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear All Strings", fontSize = 11.sp)
            }

            InstrumentPlayButton(
                isPlaying = isPlayingVoicing,
                accentColor = Color(0xFFCE93D8),
                label = if (isPlayingVoicing) "Stop" else "Strum Voicing",
                onClick = {
                    if (isPlayingVoicing) {
                        isPlayingVoicing = false
                    } else {
                        isPlayingVoicing = true
                        scope.launch {
                            try {
                                viewModel.audioEngine.setInstrument(selectedFinderInstrument.soundEngineName)
                                soundedNotes.forEach { (_, midi) ->
                                    viewModel.audioEngine.noteOn(midi, 0.85f)
                                    delay(40L)
                                }
                                delay(600L)
                            } finally {
                                isPlayingVoicing = false
                            }
                        }
                    }
                }
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF221A2B),
            border = BorderStroke(1.dp, Color(0xFFAB47BC).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("IDENTIFIED CHORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8), letterSpacing = 1.sp)
                Text(detectedName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                val notesString = soundedNotes.joinToString(" • ") { "${it.first} (${it.second})" }
                Text("Sounded Notes: " + notesString.ifEmpty { "None" }, fontSize = 12.sp, color = Color(0xFFD1C4E9))

                if (soundedNotes.isNotEmpty()) {
                    val activePianoNotes = remember(soundedNotes) { soundedNotes.associate { it.second to true } }
                    Text("PIANO KEYBOARD VOICING:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8), modifier = Modifier.padding(top = 4.dp))
                    UnifiedPianoKeyboard(
                        activeMidiNotes = activePianoNotes,
                        accentColor = Color(0xFFCE93D8),
                        startNote = 36,
                        endNote = 72,
                        fillWidth = true,
                        showNoteNames = true,
                        onKeyTapped = { midi ->
                            runCatching {
                                viewModel.audioEngine.setInstrument("PIANO")
                                viewModel.audioEngine.noteOn(midi, 0.85f)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaleFinderTool(viewModel: AppViewModel, roots: List<String>) {
    var inputNotes by remember { mutableStateOf(setOf("C", "E", "G")) }
    var selectedScaleItem by remember { mutableStateOf<Triple<String, String, List<String>>?>(null) }
    var visualizerInstrument by remember { mutableStateOf("piano") }

    val scaleTypes = remember { STUDIO_SCALE_LIBRARY.entries.map { it.key to it.value } }

    val matchedScales = remember(inputNotes) {
        val results = mutableListOf<Triple<String, String, List<String>>>()
        roots.forEach { root ->
            val rootIdx = roots.indexOf(root)
            scaleTypes.forEach { (scaleName, intervals) ->
                val scaleNotes = intervals.map { roots[(rootIdx + it) % 12] }
                if (inputNotes.all { it in scaleNotes }) {
                    results.add(Triple(root, scaleName, scaleNotes))
                }
            }
        }
        results
    }

    LaunchedEffect(matchedScales) {
        selectedScaleItem = matchedScales.firstOrNull()
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Scale Finder (Find Scales & Thaats From Notes)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
        Text("Select any combination of notes to identify matching Western modes, pentatonics, and Hindustani Thaats.", fontSize = 12.sp, color = Color.Gray)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(roots) { r ->
                val isSel = r in inputNotes
                pEQFilterChip(
                    selected = isSel,
                    onClick = { inputNotes = if (isSel) inputNotes - r else inputNotes + r },
                    label = { Text(r, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { inputNotes = setOf("C", "E", "G") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3242)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Preset: C Major Triad", fontSize = 10.sp)
            }
            Button(
                onClick = { inputNotes = setOf("A", "C", "E") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3242)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Preset: A Minor Triad", fontSize = 10.sp)
            }
            Button(
                onClick = { inputNotes = emptySet() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2024)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Clear Notes", fontSize = 10.sp, color = Color(0xFFFF8A80))
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141F16),
            border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MATCHING SCALES (${matchedScales.size} found containing ${inputNotes.joinToString(", ")})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )

                if (matchedScales.isEmpty()) {
                    Text("No scale contains all selected notes together.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchedScales.take(12)) { scaleItem ->
                            val (root, scaleName, _) = scaleItem
                            val isSel = selectedScaleItem == scaleItem
                            Surface(
                                onClick = { selectedScaleItem = scaleItem },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF2E7D32) else Color(0xFF1A2A1E),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF81C784) else Color(0xFF2B4230))
                            ) {
                                Text(
                                    text = "$root $scaleName",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFFA5D6A7),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    selectedScaleItem?.let { (root, scaleName, scaleNotes) ->
                        val rootIdx = roots.indexOf(root).coerceAtLeast(0)
                        val activeScaleMidi = scaleNotes.mapNotNull { n ->
                            val sIdx = roots.indexOf(n)
                            if (sIdx >= 0) 60 + sIdx else null
                        }.associateWith { true }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scale: $root $scaleName (${scaleNotes.joinToString(" - ")})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = visualizerInstrument == "piano",
                                    onClick = { visualizerInstrument = "piano" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text("🎹 Piano", fontSize = 10.sp)
                                }
                                SegmentedButton(
                                    selected = visualizerInstrument == "guitar",
                                    onClick = { visualizerInstrument = "guitar" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("🎸 Guitar", fontSize = 10.sp)
                                }
                            }
                        }

                        if (visualizerInstrument == "piano") {
                            UnifiedPianoKeyboard(
                                activeMidiNotes = activeScaleMidi,
                                accentColor = Color(0xFF81C784),
                                startNote = 48,
                                endNote = 84,
                                fillWidth = true,
                                showNoteNames = true,
                                onKeyTapped = { midi ->
                                    runCatching {
                                        viewModel.audioEngine.setInstrument("PIANO")
                                        viewModel.audioEngine.noteOn(midi, 0.85f)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            val playableGuitarScaleMidi = remember(rootIdx, scaleNotes) {
                                val semis = scaleNotes.mapNotNull { n ->
                                    val idx = roots.indexOf(n)
                                    if (idx >= 0) ((idx - rootIdx) % 12 + 12) % 12 else null
                                }
                                BiomechanicalPerformersEngine.solvePlayableScaleBoxRun(
                                    rootIdx = rootIdx,
                                    scaleIntervals = semis,
                                    openStringsMidiHighToLow = listOf(64, 59, 55, 50, 45, 40),
                                    boxStartFret = 0,
                                    boxSpanFrets = 4,
                                    numOctaves = 2
                                ).map { it.third }
                            }
                            UnifiedGuitarFretboard(
                                numFrets = 15,
                                activeNotes = playableGuitarScaleMidi,
                                rootNote = rootIdx,
                                accentColor = Color(0xFF81C784),
                                showNoteNames = true,
                                markOctaves = true,
                                highlightOctaveC = true,
                                baseFret = 1,
                                onFretTapped = { _: Int, _: Int, midi: Int ->
                                    runCatching {
                                        viewModel.audioEngine.setInstrument("GUITAR")
                                        viewModel.audioEngine.noteOn(midi, 0.85f)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetronomeTool(viewModel: AppViewModel) {
    var bpm by remember { mutableIntStateOf(120) }
    var beatsPerBar by remember { mutableIntStateOf(4) }
    var isRunning by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableIntStateOf(1) }
    val tapTimestamps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(isRunning, bpm, beatsPerBar) {
        if (isRunning) {
            val intervalMs = (60000L / bpm.coerceIn(30, 260)).coerceAtLeast(100L)
            while (isActive && isRunning) {
                runCatching {
                    val midiClick = if (currentBeat == 1) 76 else 77
                    viewModel.audioEngine.noteOn(midiClick, if (currentBeat == 1) 0.95f else 0.72f)
                }
                delay(intervalMs)
                currentBeat = if (currentBeat >= beatsPerBar) 1 else currentBeat + 1
            }
        } else {
            currentBeat = 1
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Precision Studio Metronome", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9E1B))
        Text("$bpm BPM", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

        // Beats Per Bar Selector (2/4, 3/4, 4/4, 5/4, 6/8, 7/8)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2, 3, 4, 5, 6, 7).forEach { bCount ->
                pEQFilterChip(
                    selected = beatsPerBar == bCount,
                    onClick = { beatsPerBar = bCount; currentBeat = 1 },
                    label = { Text("$bCount Beats", fontSize = 11.sp) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..beatsPerBar).forEach { b ->
                val active = isRunning && currentBeat == b
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        active && b == 1 -> Color(0xFF00E676)
                        active -> Color(0xFFFF9E1B)
                        else -> Color(0xFF2C3242)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$b", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (active) Color.Black else Color.White)
                    }
                }
            }
        }

        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.toInt() },
            valueRange = 40f..240f,
            modifier = Modifier.fillMaxWidth(0.88f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (tapTimestamps.isNotEmpty() && now - tapTimestamps.last() > 2500L) {
                        tapTimestamps.clear()
                    }
                    tapTimestamps.add(now)
                    if (tapTimestamps.size >= 2) {
                        val intervals = tapTimestamps.zipWithNext { a, b -> b - a }
                        val avgMs = intervals.average()
                        if (avgMs > 180.0) {
                            bpm = (60000.0 / avgMs).toInt().coerceIn(40, 240)
                        }
                    }
                }
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("TAP TEMPO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0xFFE53935) else Color(0xFFFF9E1B))
            ) {
                Text(
                    if (isRunning) "STOP" else "START METRONOME",
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
private fun TimeSignaturesTool(viewModel: AppViewModel) {
    val meterSignatures = remember {
        listOf(
            Triple("4/4", "Common Time", "4 quarter notes per measure (STRONG - weak - medium - weak). Found in 90% of Pop, Rock, and Electronic music."),
            Triple("3/4", "Waltz Meter", "3 quarter notes per measure (STRONG - weak - weak). Found in waltzes, ballads, and classical music."),
            Triple("6/8", "Compound Duple", "Two main beats, each divided into three eighth notes (ONE-two-three, FOUR-five-six). Found in Blues, Afro-Cuban, and Celtic."),
            Triple("2/4", "Marching Duple", "2 quarter notes per measure (ONE - two). Found in Polka, Marches, and Country train-beats."),
            Triple("5/4", "Asymmetric (Dave Brubeck)", "Compound odd meter grouped as 3+2 or 2+3. Famous from 'Take Five' and 'Mission Impossible'."),
            Triple("7/8", "Progressive / Balkan", "Fast odd meter subdivided as 2+2+3 or 3+2+2. Found in Balkan folk, Prog Rock (Pink Floyd's 'Money')."),
            Triple("12/8", "Slow Blues Shuffle", "Four main beats with triplet subdivisions (ONE-and-a, TWO-and-a, THREE-and-a, FOUR-and-a).")
        )
    }

    var selectedMeter by remember { mutableStateOf("4/4") }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Time Signatures & Rhythmic Meters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB347))
        Text("Explore pulse subdivisions, accents, and musical feels across standard and odd meters. Tap any beat block to audition accent.", fontSize = 12.sp, color = Color.Gray)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(meterSignatures) { (meter, name, _) ->
                val isSel = selectedMeter == meter
                pEQFilterChip(
                    selected = isSel,
                    onClick = { selectedMeter = meter },
                    label = { Text("$meter ($name)", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        val activeItem = meterSignatures.find { it.first == selectedMeter } ?: meterSignatures[0]

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E2430),
            border = BorderStroke(1.dp, Color(0xFF2B3444))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activeItem.first} - ${activeItem.second}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF9E1B).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Tap Beats to Audition",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB347),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = activeItem.third,
                    fontSize = 13.sp,
                    color = Color(0xFF9EABB8),
                    lineHeight = 18.sp
                )

                val beatCount = when (activeItem.first) {
                    "3/4" -> 3
                    "2/4" -> 2
                    "5/4" -> 5
                    "6/8" -> 6
                    "7/8" -> 7
                    "12/8" -> 12
                    else -> 4
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..beatCount).forEach { beat ->
                        val isPrimary = beat == 1
                        val isSecondary = (activeItem.first == "4/4" && beat == 3) ||
                            (activeItem.first == "5/4" && beat == 4) ||
                            (activeItem.first == "6/8" && beat == 4) ||
                            (activeItem.first == "7/8" && beat in listOf(3, 5)) ||
                            (activeItem.first == "12/8" && beat in listOf(4, 7, 10))
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    runCatching {
                                        val pitch = when {
                                            isPrimary -> 76
                                            isSecondary -> 72
                                            else -> 67
                                        }
                                        viewModel.audioEngine.noteOn(pitch, if (isPrimary) 0.95f else 0.72f)
                                    }
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isPrimary -> Color(0xFFFF9E1B)
                                isSecondary -> Color(0xFFFF9E1B).copy(alpha = 0.5f)
                                else -> Color(0xFF2C3242)
                            }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$beat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPrimary) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. CAPO SECTION, TRANSPOSE SECTION & EASY CHORD QUICK REF
// ============================================================================

@Composable
fun CapoSection(
    capoFret: Int,
    activeRootIdx: Int,
    onCapoChange: (Int) -> Unit,
    partColor: Color,
    onKeyDispatched: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🎸 DYNAMIC CAPO CALCULATOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = partColor,
                letterSpacing = 2.sp
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (capoFret > 0) partColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    if (capoFret == 0) "Open (No Capo)" else "Capo Fret $capoFret",
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (capoFret > 0) partColor else MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SLIDE OR TAP CAPO FRET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = partColor.copy(0.8f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        if (capoFret > 0) "+$capoFret Semitones" else "Standard Pitch",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (0..12).forEach { f ->
                        val isSelected = (capoFret == f)
                        Surface(
                            onClick = { onCapoChange(f) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) partColor else MaterialTheme.colorScheme.surfaceContainerHigh.copy(0.6f),
                            border = if (isSelected) BorderStroke(1.5.dp, Color.White) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f)),
                            modifier = Modifier.width(42.dp).height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (f == 0) "0" else "$f",
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Slider(
                    value = capoFret.toFloat(),
                    onValueChange = { onCapoChange(it.toInt()) },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = partColor, activeTrackColor = partColor)
                )
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SHAPE YOU GRAB", fontSize = 10.sp, fontWeight = FontWeight.Black, color = partColor.copy(0.6f))
                    Text("ACTUAL SOUNDING KEY (TAP TO AUDITION)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = partColor.copy(0.6f))
                }

                NOTE_NAMES_CD.forEachIndexed { shapeIdx, shapeName ->
                    val actualSoundingSemitone = (shapeIdx + capoFret) % 12
                    val isSoundingActiveRoot = actualSoundingSemitone == activeRootIdx

                    Surface(
                        onClick = {
                            onKeyDispatched(actualSoundingSemitone)
                            runCatching {
                                getAudioEngine().noteOn(60 + actualSoundingSemitone, 0.85f)
                                getAudioEngine().noteOn(64 + actualSoundingSemitone, 0.80f)
                                getAudioEngine().noteOn(67 + actualSoundingSemitone, 0.80f)
                            }
                        },
                        color = if (isSoundingActiveRoot) partColor.copy(alpha = 0.12f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = if (isSoundingActiveRoot) BorderStroke(1.dp, partColor.copy(0.5f)) else null
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$shapeName Open Shape",
                                fontSize = 13.sp,
                                fontWeight = if (isSoundingActiveRoot) FontWeight.Black else FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("→", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                            Text(
                                text = "${NOTE_NAMES_CD[actualSoundingSemitone]} Concert Pitch",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSoundingActiveRoot) partColor else MaterialTheme.colorScheme.onSurface.copy(0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransposeSection(
    fromKeyIdx: Int,
    toKeyIdx: Int,
    selectedScaleIdx: Int,
    scaleFormulas: List<Pair<String, List<Int>>>,
    partColor: Color,
    onFromChange: (Int) -> Unit,
    onToChange: (Int) -> Unit,
    onKeyDispatched: (Int) -> Unit
) {
    val semitoneShift = ((toKeyIdx - fromKeyIdx) + 12) % 12
    val shiftLabel = if (semitoneShift == 0) "Same key" else "+$semitoneShift Semitones"
    val scope = rememberCoroutineScope()

    val songIdeas = remember {
        listOf(
            "Epic/Heroic Hook (I - V - vi - IV)" to listOf(0, 4, 5, 3),
            "Moody Sad/Emotional (vi - IV - I - V)" to listOf(5, 3, 0, 4),
            "Dark Suspense/Tension (i - ♭VI - ♭VII - v)" to listOf(0, 5, 6, 4),
            "Neo-Soul/Jazz Fluidity (ii - V - I - IV)" to listOf(1, 4, 0, 3),
            "Royal Andalusian Cadence (vi - V - IV - III)" to listOf(5, 4, 3, 2)
        )
    }

    fun getRomanNumeral(stepIdx: Int, isMinor: Boolean, isDiminished: Boolean): String {
        val baseNumeral = when (stepIdx) {
            0 -> "I"; 1 -> "II"; 2 -> "III"; 3 -> "IV"; 4 -> "V"; 5 -> "VI"; 6 -> "VII"; else -> "?"
        }
        return when {
            isDiminished -> baseNumeral.lowercase() + "°"
            isMinor -> baseNumeral.lowercase()
            else -> baseNumeral
        }
    }

    val activeScaleSteps = scaleFormulas.getOrNull(selectedScaleIdx)?.second ?: listOf(0, 2, 4, 5, 7, 9, 11)
    val activeScaleName = scaleFormulas.getOrNull(selectedScaleIdx)?.first?.split(" ")?.firstOrNull() ?: "Scale"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "✨ SONGWRITING IDEAS ($activeScaleName)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = partColor,
                letterSpacing = 2.sp
            )
            Surface(shape = RoundedCornerShape(8.dp), color = partColor.copy(0.12f)) {
                Text(
                    shiftLabel,
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = partColor
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CdLabel("CREATIVE SEED KEY (FROM)")
            ScrollableRow {
                NOTE_NAMES_CD.forEachIndexed { i, name ->
                    val sel = i == fromKeyIdx
                    Surface(
                        onClick = { onFromChange(i) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) partColor.copy(0.15f) else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            name,
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Black else FontWeight.Normal,
                            color = if (sel) partColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CdLabel("TARGET ARRANGEMENT KEY (TO)")
            ScrollableRow {
                NOTE_NAMES_CD.forEachIndexed { i, name ->
                    val sel = i == toKeyIdx
                    Surface(
                        onClick = { onToChange(i); onKeyDispatched(i) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) partColor else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            name,
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Black else FontWeight.Normal,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                songIdeas.forEach { (vibeTitle, stepsList) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(vibeTitle, fontSize = 12.sp, fontWeight = FontWeight.Black, color = partColor)
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        stepsList.forEach { stepIdx ->
                                            val intervalOffset = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                            val nextThird = activeScaleSteps.find { it == (intervalOffset + 3) % 12 || it == (intervalOffset + 4) % 12 } ?: (intervalOffset + 4)
                                            val isMinor = ((nextThird - intervalOffset + 12) % 12) == 3
                                            val rootMidi = 60 + toKeyIdx + intervalOffset
                                            runCatching {
                                                getAudioEngine().noteOn(rootMidi, 0.90f)
                                                getAudioEngine().noteOn(rootMidi + (if (isMinor) 3 else 4), 0.82f)
                                                getAudioEngine().noteOn(rootMidi + 7, 0.82f)
                                            }
                                            delay(580)
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp).background(partColor.copy(0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = partColor)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                            stepsList.forEach { stepIdx ->
                                val interval = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                val nextThird = activeScaleSteps.find { it == (interval + 3) % 12 || it == (interval + 4) % 12 } ?: (interval + 4)
                                val nextFifth = activeScaleSteps.find { it == (interval + 7) % 12 || it == (interval + 6) % 12 } ?: (interval + 7)
                                val isMinor = ((nextThird - interval + 12) % 12) == 3
                                val isDim = ((nextFifth - interval + 12) % 12) == 6
                                Text(
                                    text = getRomanNumeral(stepIdx, isMinor, isDim),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                                )
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${NOTE_NAMES_CD[fromKeyIdx]}:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.width(32.dp))
                            stepsList.forEach { stepIdx ->
                                val intervalOffset = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                val nextThird = activeScaleSteps.find { it == (intervalOffset + 3) % 12 || it == (intervalOffset + 4) % 12 } ?: (intervalOffset + 4)
                                val isMinor = ((nextThird - intervalOffset + 12) % 12) == 3
                                val chordName = NOTE_NAMES_CD[(fromKeyIdx + intervalOffset) % 12] + (if (isMinor) "m" else "")
                                Text(chordName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.7f), modifier = Modifier.padding(end = 8.dp))
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${NOTE_NAMES_CD[toKeyIdx]}:", fontSize = 11.sp, color = partColor, modifier = Modifier.width(32.dp))
                            stepsList.forEach { stepIdx ->
                                val intervalOffset = activeScaleSteps.getOrNull(stepIdx % activeScaleSteps.size) ?: 0
                                val nextThird = activeScaleSteps.find { it == (intervalOffset + 3) % 12 || it == (intervalOffset + 4) % 12 } ?: (intervalOffset + 4)
                                val isMinor = ((nextThird - intervalOffset + 12) % 12) == 3
                                val chordName = NOTE_NAMES_CD[(toKeyIdx + intervalOffset) % 12] + (if (isMinor) "m" else "")
                                Text(chordName, fontSize = 12.sp, fontWeight = FontWeight.Black, color = partColor, modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EasyChordQuickRef(
    rootIdx: Int,
    qualityIdx: Int,
    cadences: List<Pair<String, List<Int>>>,
    accentColor: Color,
    onSelectChord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "QUICK REFERENCE & SUBSTITUTIONS (TAP TO AUDITION)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                letterSpacing = 1.sp
            )

            ScrollableRow {
                cadences.forEach { (name, notes) ->
                    Surface(
                        onClick = { onSelectChord(name) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(name, fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                notes.joinToString(" - ") { NOTE_NAMES_CD[(it % 12 + 12) % 12] },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. IMMERSIVE FULLSCREEN KEYBOARD & FRETBOARD WORKSTATION LAB DIALOG
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveKeyboardFretboardLabDialog(
    viewModel: AppViewModel,
    rootIdx: Int,
    qualityIdx: Int,
    instrumentType: ChordInstrumentType,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    DisposableEffect(Unit) {
        viewModel.isImmersiveLabActive.value = true
        setScreenOrientation(pEQOrientation.LANDSCAPE)
        setSystemBarsVisibility(false)
        onDispose {
            viewModel.isImmersiveLabActive.value = false
            setScreenOrientation(pEQOrientation.USER)
            setSystemBarsVisibility(true)
        }
    }

    var activeLabTab by remember {
        mutableStateOf(
            when (instrumentType) {
                ChordInstrumentType.PIANO_ACOUSTIC -> "Piano"
                ChordInstrumentType.UKULELE -> "Uke"
                else -> "Guitar"
            }
        )
    }
    var selectedSpeed by remember { mutableStateOf("1.0x") }
    var playbackMode by remember { mutableStateOf("Sequential") }
    var isPlaying by remember { mutableStateOf(false) }

    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Chords, 1 = Scales
    var labRootIdx by remember { mutableIntStateOf(rootIdx) }
    var labSuffixIdx by remember { mutableIntStateOf(qualityIdx) }
    var labInversion by remember { mutableStateOf("Root Position") }

    // Left sidebar controls
    var guitarConfigStr by remember { mutableStateOf("6 Str") }
    var tuningOption by remember { mutableStateOf("Standard (EADGBE)") }
    var capoFretSelected by remember { mutableIntStateOf(0) }
    var fretNeckZoom by remember { mutableStateOf("12f") }
    var isLeftHanded by remember { mutableStateOf(false) }
    var showInlayDots by remember { mutableStateOf(true) }
    var labelOverlay by remember { mutableStateOf("Notes") } // "Notes", "Intervals", "Fingers"
    var noteNamingSystem by remember { mutableStateOf("Western (C, D, E)") }
    var synthSound by remember { mutableStateOf("Acoustic Piano") }

    val roots = remember { NOTE_NAMES_CD }
    val currentRootName = roots[labRootIdx.coerceIn(0, roots.size - 1)]

    val chordSuffixes = listOf("Major", "Minor", "7th", "Major 7th", "Minor 7th", "Diminished", "Augmented", "Sus4", "Add9")
    val currentSuffixName = chordSuffixes.getOrElse(labSuffixIdx) { "Major" }
    val inversions = listOf("Root Position", "1st Inversion", "2nd Inversion")

    val isSevenStrLab = guitarConfigStr == "7 Str"
    val labOpenStringsMidi = remember(activeLabTab, isSevenStrLab, tuningOption, capoFretSelected) {
        val base = if (activeLabTab == "Uke") {
            when {
                tuningOption.contains("Low G") -> listOf(69, 64, 60, 55)
                tuningOption.contains("Baritone") -> listOf(64, 59, 55, 50)
                else -> listOf(69, 64, 60, 67)
            }
        } else if (isSevenStrLab) {
            listOf(64, 59, 55, 50, 45, 40, 35)
        } else {
            when {
                tuningOption.contains("Drop D") -> listOf(64, 59, 55, 50, 45, 38)
                tuningOption.contains("Half-Step") -> listOf(63, 58, 54, 49, 44, 39)
                tuningOption.contains("DADGAD") -> listOf(62, 57, 55, 50, 45, 38)
                tuningOption.contains("Open G") -> listOf(62, 59, 55, 50, 43, 38)
                else -> listOf(64, 59, 55, 50, 45, 40)
            }
        }
        base.map { it + capoFretSelected }
    }

    val labStringLabels = remember(labOpenStringsMidi, isLeftHanded) {
        val labels = labOpenStringsMidi.mapIndexed { idx, midi ->
            val name = STUDIO_ROOT_NAMES[(midi % 12 + 12) % 12]
            if (idx == 0 && name == "E") "e" else name
        }
        if (isLeftHanded) labels.reversed() else labels
    }

    val labOpenStringsSemis = remember(labOpenStringsMidi, isLeftHanded) {
        val semis = labOpenStringsMidi.map { (it % 12 + 12) % 12 }
        if (isLeftHanded) semis.reversed() else semis
    }

    val activeChordPitches = remember(labRootIdx, labSuffixIdx, labInversion, selectedMode, capoFretSelected) {
        if (selectedMode == 1) {
            val pat = if (labSuffixIdx in listOf(1, 4)) SCALE_MINOR_NAT_PATTERN else SCALE_MAJOR_PATTERN
            (pat + listOf(12)).map { 60 + ((labRootIdx + capoFretSelected) % 12) + it }
        } else {
            val labSuffixMap = listOf("", "m", "7", "maj7", "m7", "dim", "aug", "sus4", "add9")
            val chosenSuffix = labSuffixMap.getOrElse(labSuffixIdx) { "" }
            val inv = when (labInversion) {
                "1st Inversion" -> InversionStyle.FIRST
                "2nd Inversion" -> InversionStyle.SECOND
                else -> InversionStyle.ROOT
            }
            ChordVoicingEngine.buildPianoShape(
                rootIdx = (labRootIdx + capoFretSelected) % 12,
                suffix = chosenSuffix,
                style = PianoVoicingStyle.CLOSE,
                inversion = inv,
                octave = 4
            )
        }
    }

    val labPianoFingering = remember(activeChordPitches, selectedMode) {
        ChordVoicingEngine.computePianoFingering(
            pitches = activeChordPitches,
            isScale = (selectedMode == 1)
        )
    }

    // FIXED: Wire `labelOverlay` ("Notes", "Intervals", "Fingers") directly to `showNoteNames` & `fingering`!
    val showNoteNamesInLab = labelOverlay != "Fingers"

    val activeMidiMap = remember(activeChordPitches) {
        activeChordPitches.associateWith { true }
    }

    val orangeColor = Color(0xFFFF9E1B)
    val sidebarBg = Color(0xFF0F1015)
    val rosewoodBrush = Brush.verticalGradient(listOf(Color(0xFF4A342E), Color(0xFF382723), Color(0xFF291C19)))

    var currentAuditionIdx by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(isPlaying, selectedSpeed, playbackMode, activeChordPitches) {
        if (!isPlaying || activeChordPitches.isEmpty()) {
            currentAuditionIdx = null
            return@LaunchedEffect
        }
        val speedMult = when (selectedSpeed) {
            "0.5x" -> 0.5f
            "1.5x" -> 1.5f
            "2.0x" -> 2.0f
            else -> 1.0f
        }

        while (isActive && isPlaying) {
            when (playbackMode) {
                "Block Chord" -> {
                    currentAuditionIdx = null
                    activeChordPitches.forEach { midi ->
                        runCatching {
                            val inst = if (activeLabTab == "Guitar" || activeLabTab == "Uke") "GUITAR" else "PIANO"
                            viewModel.audioEngine.setInstrument(inst)
                            viewModel.audioEngine.noteOn(midi, 0.85f)
                        }
                    }
                    delay((650L / speedMult).toLong())
                }
                "Roundtrip Arp" -> {
                    val roundtripPitches = if (activeChordPitches.size > 2) {
                        activeChordPitches + activeChordPitches.subList(0, activeChordPitches.size - 1).reversed()
                    } else if (activeChordPitches.size == 2) {
                        listOf(activeChordPitches[0], activeChordPitches[1], activeChordPitches[0])
                    } else {
                        activeChordPitches
                    }
                    val stepDelay = (320L / speedMult).toLong()
                    for (pitch in roundtripPitches) {
                        if (!isPlaying) break
                        currentAuditionIdx = activeChordPitches.indexOf(pitch)
                        runCatching {
                            val inst = if (activeLabTab == "Guitar" || activeLabTab == "Uke") "GUITAR" else "PIANO"
                            viewModel.audioEngine.setInstrument(inst)
                            viewModel.audioEngine.noteOn(pitch, 0.85f)
                        }
                        delay(stepDelay)
                    }
                }
                else -> {
                    val stepDelay = (380L / speedMult).toLong()
                    for (idx in activeChordPitches.indices) {
                        if (!isPlaying) break
                        currentAuditionIdx = idx
                        val midi = activeChordPitches[idx]
                        runCatching {
                            val inst = if (activeLabTab == "Guitar" || activeLabTab == "Uke") "GUITAR" else "PIANO"
                            viewModel.audioEngine.setInstrument(inst)
                            viewModel.audioEngine.noteOn(midi, 0.85f)
                        }
                        delay(stepDelay)
                    }
                }
            }
        }
    }

    FullscreenDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0B0E))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. LEFT SIDEBAR
                Column(
                    modifier = Modifier
                        .width(225.dp)
                        .fillMaxHeight()
                        .background(sidebarBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Visualizer Lab",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = orangeColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Active Visualizer", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Piano", "Guitar", "Uke", "Split").forEach { tab ->
                                val isSelected = activeLabTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) orangeColor else Color(0xFF1E2129))
                                        .clickable { activeLabTab = tab },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (activeLabTab == "Guitar" || activeLabTab == "Split") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Guitar Config", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("6 Str", "7 Str").forEach { strConf ->
                                    val isSel = guitarConfigStr == strConf
                                    Box(
                                        modifier = Modifier
                                            .width(54.dp)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF3B4054) else Color(0xFF1E2129))
                                            .border(1.dp, if (isSel) Color(0xFF7580A5) else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable { guitarConfigStr = strConf },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(strConf, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val guitarTuningList = listOf("Standard (EADGBE)", "Drop D (DADGBE)", "Half-Step Down (Eb)", "DADGAD Celtic", "Open G")
                            M3DropdownMenu(
                                label = "Tuning",
                                selectedText = tuningOption,
                                items = guitarTuningList,
                                onItemSelect = { tuningOption = guitarTuningList[it] }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Capo Fret", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                (0..3).forEach { fret ->
                                    val label = if (fret == 0) "Open" else "$fret"
                                    val isSel = capoFretSelected == fret
                                    Box(
                                        modifier = Modifier
                                            .weight(if (fret == 0) 1.4f else 1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) orangeColor else Color(0xFF1E2129))
                                            .clickable { capoFretSelected = fret },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                (4..7).forEach { fret ->
                                    val isSel = capoFretSelected == fret
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) orangeColor else Color(0xFF1E2129))
                                            .clickable { capoFretSelected = fret },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$fret", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Fret Neck Zoom", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("5f", "8f", "12f", "15f").forEach { z ->
                                    val isSel = fretNeckZoom == z
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF3B4054) else Color(0xFF1E2129))
                                            .border(1.dp, if (isSel) Color(0xFF7580A5) else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable { fretNeckZoom = z },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(z, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    } else if (activeLabTab == "Uke") {
                        val ukeTuningList = listOf("Standard (GCEA)", "Low G (gCEA)", "Baritone (DGBE)")
                        M3DropdownMenu(
                            label = "Ukulele Tuning",
                            selectedText = tuningOption,
                            items = ukeTuningList,
                            onItemSelect = { tuningOption = ukeTuningList[it] }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fretboard View", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        if (activeLabTab != "Piano") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Left-Handed", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = isLeftHanded,
                                    onCheckedChange = { isLeftHanded = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = orangeColor)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Inlay Dots", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = showInlayDots,
                                onCheckedChange = { showInlayDots = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = orangeColor)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Label Overlay", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Notes", "Intervals", "Fingers").forEach { l ->
                                val isSel = labelOverlay == l
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF3B4054) else Color(0xFF1E2129))
                                        .border(1.dp, if (isSel) Color(0xFF7580A5) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { labelOverlay = l },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(l, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    val namingList = listOf("Western (C, D, E)", "Sargam (Sa, Re, Ga)", "Solfege (Do, Re, Mi)")
                    M3DropdownMenu(
                        label = "Note Naming System",
                        selectedText = noteNamingSystem,
                        items = namingList,
                        onItemSelect = { noteNamingSystem = namingList[it] }
                    )

                    val soundGenList = listOf("Acoustic Piano", "Electric Piano", "Acoustic Guitar", "Electric Guitar", "Classical Guitar", "Harmonium", "Sitar", "Violin", "Flute")
                    M3DropdownMenu(
                        label = "Sound Generator",
                        selectedText = synthSound,
                        items = soundGenList,
                        onItemSelect = { idx ->
                            synthSound = soundGenList[idx]
                            val engineName = when (synthSound) {
                                "Electric Piano" -> "PIANO_ELECTRIC"
                                "Acoustic Guitar" -> "GUITAR_ACOUSTIC"
                                "Electric Guitar" -> "GUITAR_ELECTRIC"
                                "Classical Guitar" -> "GUITAR_CLASSICAL"
                                "Harmonium" -> "HARMONIUM"
                                "Sitar" -> "SITAR"
                                "Violin" -> "VIOLIN"
                                "Flute" -> "FLUTE"
                                else -> "PIANO_ACOUSTIC"
                            }
                            runCatching { viewModel.audioEngine.setInstrument(engineName) }
                        }
                    )
                }

                // 2. MAIN WORKSTATION AREA
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // TOP BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Column {
                                Text(
                                    text = "$currentRootName $currentSuffixName Workstation",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Speed: $selectedSpeed • Playback: $playbackMode • Overlay: $labelOverlay",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(0.6f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("0.5x", "1.0x", "1.5x", "2.0x").forEach { spd ->
                                val isSel = selectedSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) orangeColor else Color(0xFF1E2129))
                                        .clickable { selectedSpeed = spd }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(spd, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2129))
                                    .clickable {
                                        playbackMode = when (playbackMode) {
                                            "Sequential" -> "Block Chord"
                                            "Block Chord" -> "Roundtrip Arp"
                                            else -> "Sequential"
                                        }
                                    }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (playbackMode) {
                                        "Sequential" -> "Seq Strum"
                                        "Block Chord" -> "Block (All)"
                                        "Roundtrip Arp" -> "Arp (L⇄R)"
                                        else -> "Seq Strum"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = { isPlaying = !isPlaying },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color(0xFFD32F2F) else orangeColor),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isPlaying) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "Stop" else "Play",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPlaying) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    // Active Note / Sargam / Solfege Telemetry Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeChordPitches.forEachIndexed { idx, midi ->
                            val semi = (midi % 12 + 12) % 12
                            val relSemi = ((semi - labRootIdx) % 12 + 12) % 12
                            val displayNote = when {
                                noteNamingSystem.startsWith("Sargam") -> SARGAM_SYLLABLES[relSemi]
                                noteNamingSystem.startsWith("Solfege") -> SOLFEGE_SYLLABLES[relSemi]
                                else -> STUDIO_ROOT_NAMES[semi]
                            }
                            val fingerNum = labPianoFingering.fingers.getOrNull(idx) ?: (idx + 1)
                            val isCurrentlySounding = isPlaying && currentAuditionIdx == idx

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrentlySounding) orangeColor else Color(0xFF171A24),
                                border = BorderStroke(1.dp, if (isCurrentlySounding) Color.White else orangeColor.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    runCatching { viewModel.audioEngine.noteOn(midi, 0.88f) }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = displayNote,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isCurrentlySounding) Color.Black else Color.White
                                    )
                                    Text(
                                        text = if (labelOverlay == "Fingers") "F$fingerNum" else DEGREE_LABELS[relSemi],
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentlySounding) Color.Black.copy(alpha = 0.8f) else orangeColor
                                    )
                                }
                            }
                        }
                    }

                    // CENTER: INSTRUMENT CANVAS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val activeLabPlayingPitch = if (isPlaying) currentAuditionIdx?.let { activeChordPitches.getOrNull(it) } else null

                        when (activeLabTab) {
                            "Piano" -> {
                                UnifiedPianoKeyboard(
                                    activeMidiNotes = activeMidiMap,
                                    accentColor = orangeColor,
                                    fingering = labPianoFingering,
                                    activeCoordinate = activeLabPlayingPitch,
                                    startNote = 48,
                                    endNote = 84,
                                    fillWidth = true,
                                    showNoteNames = showNoteNamesInLab,
                                    isPlaying = isPlaying,
                                    onKeyTapped = { midi ->
                                        runCatching {
                                            viewModel.audioEngine.setInstrument("PIANO")
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            "Guitar" -> {
                                val numF = when (fretNeckZoom) {
                                    "5f" -> 5
                                    "8f" -> 8
                                    "15f" -> 15
                                    else -> 12
                                }
                                UnifiedGuitarFretboard(
                                    numFrets = numF,
                                    activeNotes = activeChordPitches,
                                    activeMidiNote = activeLabPlayingPitch,
                                    rootNote = labRootIdx,
                                    accentColor = orangeColor,
                                    showNoteNames = showNoteNamesInLab,
                                    capoFret = capoFretSelected,
                                    baseFret = 1,
                                    stringLabels = labStringLabels,
                                    openStringsSemis = labOpenStringsSemis,
                                    fretboardBrush = rosewoodBrush,
                                    showInlayDots = showInlayDots,
                                    onFretTapped = { _, _, midi ->
                                        runCatching {
                                            viewModel.audioEngine.setInstrument("GUITAR")
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            "Uke" -> {
                                UnifiedGuitarFretboard(
                                    numFrets = 9,
                                    activeNotes = activeChordPitches,
                                    activeMidiNote = activeLabPlayingPitch,
                                    rootNote = labRootIdx,
                                    accentColor = orangeColor,
                                    showNoteNames = showNoteNamesInLab,
                                    stringLabels = labStringLabels,
                                    openStringsSemis = labOpenStringsSemis,
                                    fretboardBrush = rosewoodBrush,
                                    showInlayDots = showInlayDots,
                                    onFretTapped = { _, _, midi ->
                                        runCatching {
                                            viewModel.audioEngine.setInstrument("GUITAR")
                                            viewModel.audioEngine.noteOn(midi, 0.85f)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            else -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    UnifiedPianoKeyboard(
                                        activeMidiNotes = activeMidiMap,
                                        accentColor = orangeColor,
                                        fingering = labPianoFingering,
                                        activeCoordinate = activeLabPlayingPitch,
                                        startNote = 48,
                                        endNote = 84,
                                        fillWidth = true,
                                        showNoteNames = showNoteNamesInLab,
                                        isPlaying = isPlaying,
                                        onKeyTapped = { midi ->
                                            runCatching {
                                                viewModel.audioEngine.setInstrument("PIANO")
                                                viewModel.audioEngine.noteOn(midi, 0.85f)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    UnifiedGuitarFretboard(
                                        numFrets = 12,
                                        activeNotes = activeChordPitches,
                                        activeMidiNote = activeLabPlayingPitch,
                                        rootNote = labRootIdx,
                                        accentColor = orangeColor,
                                        showNoteNames = showNoteNamesInLab,
                                        capoFret = capoFretSelected,
                                        stringLabels = labStringLabels,
                                        openStringsSemis = labOpenStringsSemis,
                                        fretboardBrush = rosewoodBrush,
                                        showInlayDots = showInlayDots,
                                        onFretTapped = { _, _, midi ->
                                            runCatching {
                                                viewModel.audioEngine.setInstrument("GUITAR")
                                                viewModel.audioEngine.noteOn(midi, 0.85f)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }

                    // BOTTOM BAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1117), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E212B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Mode:", fontSize = 11.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1B1E28))
                                    .padding(2.dp)
                            ) {
                                listOf("Chords", "Scales").forEachIndexed { idx, m ->
                                    val isSel = selectedMode == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF383E54) else Color.Transparent)
                                            .clickable { selectedMode = idx }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(m, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else Color.White.copy(0.6f))
                                    }
                                }
                            }
                        }

                        var rootExpanded by remember { mutableStateOf(false) }
                        Box {
                            Column(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13161F))
                                    .border(1.dp, Color(0xFF282E40), RoundedCornerShape(8.dp))
                                    .clickable { rootExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Root", fontSize = 9.sp, color = Color.White.copy(0.5f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentRootName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = rootExpanded, onDismissRequest = { rootExpanded = false }) {
                                roots.forEachIndexed { idx, r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = { labRootIdx = idx; rootExpanded = false }
                                    )
                                }
                            }
                        }

                        var suffixExpanded by remember { mutableStateOf(false) }
                        Box {
                            Column(
                                modifier = Modifier
                                    .width(105.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13161F))
                                    .border(1.dp, Color(0xFF282E40), RoundedCornerShape(8.dp))
                                    .clickable { suffixExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Chord Suffix", fontSize = 9.sp, color = Color.White.copy(0.5f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentSuffixName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = suffixExpanded, onDismissRequest = { suffixExpanded = false }) {
                                chordSuffixes.forEachIndexed { idx, s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = { labSuffixIdx = idx; suffixExpanded = false }
                                    )
                                }
                            }
                        }

                        var inversionExpanded by remember { mutableStateOf(false) }
                        Box {
                            Column(
                                modifier = Modifier
                                    .width(115.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13161F))
                                    .border(1.dp, Color(0xFF282E40), RoundedCornerShape(8.dp))
                                    .clickable { inversionExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Inversion", fontSize = 9.sp, color = Color.White.copy(0.5f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(labInversion, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = inversionExpanded, onDismissRequest = { inversionExpanded = false }) {
                                inversions.forEach { inv ->
                                    DropdownMenuItem(
                                        text = { Text(inv) },
                                        onClick = { labInversion = inv; inversionExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. SONGWRITING TIMELINE & STRUCTURE SCRUBBER (With Density-Accurate Downbeats & Section Loop)
// ============================================================================

@Composable
fun SongWritingTimelineScrubber(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val analysis = viewModel.songStructureAnalysis.value
    val currentPosMs = viewModel.currentPosition.longValue
    val totalDurationMs = viewModel.currentDuration.value
    val isPlaying = viewModel.isPlaying.value || viewModel.isStemsPlaying.value
    val currentTrack = viewModel.selectedMixerItem.value ?: viewModel.currentMediaItem.value

    var loopCurrentSection by remember { mutableStateOf(false) }

    val currentSeconds = (currentPosMs / 1000f).coerceAtLeast(0f)
    val maxSeconds = if (totalDurationMs > 0) (totalDurationMs / 1000f) else (analysis?.durationSeconds ?: 180f).coerceAtLeast(1f)

    val activeSegment = analysis?.getSegmentAt(currentSeconds)
    val activeChord = analysis?.getChordAt(currentSeconds)
    val harmonic = analysis?.harmonicAnalysis

    // Automatic Section Loop Enforcement
    LaunchedEffect(currentSeconds, loopCurrentSection, activeSegment) {
        if (loopCurrentSection && activeSegment != null) {
            if (currentSeconds >= activeSegment.end - 0.18f) {
                viewModel.seekTo((activeSegment.start * 1000L).toLong())
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SONGWRITING TIMELINE & STRUCTURE SCRUBBER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentTrack?.displayName ?: "No Track Selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (harmonic != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "KEY: ${harmonic.scaleName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${(analysis?.bpm ?: 120f).toInt()} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val secColor = activeSegment?.let { AllInOneColorPalette.getColorForLabel(it.label) } ?: MaterialTheme.colorScheme.primary
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(secColor))
                    Text(
                        text = activeSegment?.label?.uppercase() ?: "INTRO / START",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activeChord != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CHORD:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeChord.chordName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (activeChord.romanNumeral.isNotEmpty()) {
                                Text(
                                    text = "(${activeChord.romanNumeral})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (harmonic != null) {
                        Text(
                            text = harmonic.primaryProgression,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            val segments = analysis?.segments?.ifEmpty { null } ?: listOf(
                MusicSegment(0f, 15f, "intro"),
                MusicSegment(15f, 45f, "verse 1"),
                MusicSegment(45f, 75f, "chorus 1"),
                MusicSegment(75f, 105f, "verse 2"),
                MusicSegment(105f, 135f, "chorus 2"),
                MusicSegment(135f, 155f, "bridge"),
                MusicSegment(155f, 180f, "outro")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                segments.forEach { segment ->
                    val segDuration = (segment.end - segment.start).coerceAtLeast(0.1f)
                    val weight = (segDuration / maxSeconds).coerceIn(0.015f, 1f)
                    val isCurrent = currentSeconds >= segment.start && currentSeconds < segment.end
                    val segColor = AllInOneColorPalette.getColorForLabel(segment.label)

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(if (isCurrent) segColor else segColor.copy(alpha = 0.6f))
                            .border(
                                width = if (isCurrent) 2.5.dp else 0.5.dp,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            )
                            .clickable {
                                viewModel.seekTo((segment.start * 1000L).toLong())
                            }
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = segment.label.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(6.dp))
                    .pointerInput(maxSeconds) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val targetMs = (fraction * maxSeconds * 1000L).toLong()
                            viewModel.seekTo(targetMs)
                        }
                    }
                    .pointerInput(maxSeconds) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val targetMs = (fraction * maxSeconds * 1000L).toLong()
                            viewModel.seekTo(targetMs)
                        }
                    }
            ) {
                val totalWidthPx = constraints.maxWidth.toFloat()
                val progressFraction = (currentSeconds / maxSeconds).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressFraction)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                )

                // FIXED: Exact density-converted Dp offset for downbeat ticks!
                analysis?.downbeats?.forEach { downbeatSec ->
                    val frac = (downbeatSec / maxSeconds).coerceIn(0f, 1f)
                    val tickX = with(density) { (totalWidthPx * frac).toDp() }
                    Box(
                        modifier = Modifier
                            .offset(x = tickX)
                            .width(1.dp)
                            .height(8.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressFraction)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatScrubberPlayerTime(currentPosMs),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (loopCurrentSection) "🔁 Looping Active Section" else "Drag to Scrub Timeline",
                        fontSize = 9.sp,
                        color = if (loopCurrentSection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Text(
                        text = formatScrubberPlayerTime((maxSeconds * 1000L).toLong()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        val prev = segments.lastOrNull { it.start < (currentSeconds - 1.0f) } ?: segments.firstOrNull()
                        if (prev != null) viewModel.seekTo((prev.start * 1000L).toLong())
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Section", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prev", fontSize = 11.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val newMs = (currentPosMs - 5000L).coerceAtLeast(0L)
                            viewModel.seekTo(newMs)
                        }
                    ) {
                        Icon(Icons.Default.Replay5, contentDescription = "-5s", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            if (viewModel.activeStemMap.isNotEmpty()) {
                                viewModel.toggleStemsPlayback()
                            } else {
                                viewModel.togglePlayback()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newMs = (currentPosMs + 5000L).coerceAtMost((maxSeconds * 1000L).toLong())
                            viewModel.seekTo(newMs)
                        }
                    ) {
                        Icon(Icons.Default.Forward5, contentDescription = "+5s", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { loopCurrentSection = !loopCurrentSection }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RepeatOne,
                            contentDescription = "Loop Current Section",
                            tint = if (loopCurrentSection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val next = segments.firstOrNull { it.start > (currentSeconds + 0.5f) }
                        if (next != null) viewModel.seekTo((next.start * 1000L).toLong())
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Next", fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Section", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun formatScrubberPlayerTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}