package com.creativeblisslabs.peqlearn.app.util

data class ProgressionDef(
    val id: String,
    val name: String,
    val category: String,
    val romanDisplay: String,
    val description: String,
    val examples: String,
    val degrees: List<String>,
    val mode: String,
    val recommendedScale: String
)

object RealProgressionsDb {
    val list: List<ProgressionDef> = listOf(
        ProgressionDef(
            id = "pop-standard",
            name = "Pop Axis",
            category = "Pop",
            romanDisplay = "I – V7 – vi – IV",
            description = "The most widely used progression in modern pop",
            examples = "Let It Be, No Woman No Cry, With or Without You",
            degrees = listOf("I", "V", "vi", "IV"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "sensitive-pop",
            name = "Relative Minor Loop",
            category = "Pop",
            romanDisplay = "vi – IV – I – V7",
            description = "Same chords as the Pop Axis, starting on the minor",
            examples = "Africa (Toto), Decode (Paramore)",
            degrees = listOf("vi", "IV", "I", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "folk-rock",
            name = "Three-Chord Pop",
            category = "Pop",
            romanDisplay = "I – IV – V7",
            description = "The simplest complete progression; backbone of early rock 'n' roll",
            examples = "La Bamba, Twist and Shout, Johnny B. Goode",
            degrees = listOf("I", "IV", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "doo-wop",
            name = "50s Progression",
            category = "Pop",
            romanDisplay = "I – vi – IV – V7",
            description = "Also called the Doo-Wop progression; ubiquitous in 1950s pop",
            examples = "Stand By Me, Blue Moon, Heart and Soul",
            degrees = listOf("I", "vi", "IV", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "ii-v-i-pop",
            name = "ii–V–I (Pop)",
            category = "Pop",
            romanDisplay = "ii – V7 – I",
            description = "Borrowed from jazz; adds harmonic sophistication",
            examples = "Common in pop bridges and pre-choruses",
            degrees = listOf("ii", "V", "I"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "12-bar-blues",
            name = "12-Bar Blues",
            category = "Blues",
            romanDisplay = "I7 – I7 – I7 – I7 / IV7 – IV7 – I7 – I7 / V7 – IV7 – I7 – V7",
            description = "The foundation of the blues",
            examples = "Sweet Home Chicago, Pride and Joy, La Grange",
            degrees = listOf("I", "I", "I", "I", "IV", "IV", "I", "I", "V", "IV", "I", "V"),
            mode = "mixolydian",
            recommendedScale = "Blues Scale"
        ),
        ProgressionDef(
            id = "quick-change-blues",
            name = "Quick-Change Blues",
            category = "Blues",
            romanDisplay = "I7 – IV7 – I7 – I7 / IV7 – IV7 – I7 – I7 / V7 – IV7 – I7 – V7",
            description = "12-bar with IV in bar 2 for added early tension",
            examples = "Pride and Joy (SRV), Sweet Little Angel",
            degrees = listOf("I", "IV", "I", "I", "IV", "IV", "I", "I", "V", "IV", "I", "V"),
            mode = "mixolydian",
            recommendedScale = "Blues Scale"
        ),
        ProgressionDef(
            id = "8-bar-blues",
            name = "8-Bar Blues",
            category = "Blues",
            romanDisplay = "I7 – V – IV – IV / I7 – V – I7 – V",
            description = "Compressed form; feels more urgent and stripped-back",
            examples = "Key to the Highway, Worried Life Blues",
            degrees = listOf("I", "V", "IV", "IV", "I", "V", "I", "V"),
            mode = "mixolydian",
            recommendedScale = "Blues Scale"
        ),
        ProgressionDef(
            id = "minor-blues",
            name = "Minor Blues",
            category = "Blues",
            romanDisplay = "i – i – i – i / iv – iv – i – i / V – iv – i – V",
            description = "12-bar in minor; more mournful and tense than the major form",
            examples = "The Thrill Is Gone (BB King), All Your Love",
            degrees = listOf("i", "i", "i", "i", "iv", "iv", "i", "i", "V", "iv", "i", "V"),
            mode = "aeolian",
            recommendedScale = "Blues Scale"
        ),
        ProgressionDef(
            id = "ii-v-i-major",
            name = "ii–V–I Major",
            category = "Jazz",
            romanDisplay = "ii7 – V7 – Imaj7",
            description = "The core jazz cadence",
            examples = "Autumn Leaves, All The Things You Are",
            degrees = listOf("ii", "V", "I"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "ii-v-i-minor",
            name = "ii–V–i Minor",
            category = "Jazz",
            romanDisplay = "iiø7 – V7 – im7",
            description = "Half-diminished ii gives a darker, minor flavour",
            examples = "Autumn Leaves (minor sections), Alone Together",
            degrees = listOf("ii", "V", "i"),
            mode = "aeolian",
            recommendedScale = "Natural Minor Scale"
        ),
        ProgressionDef(
            id = "jazz-turnaround",
            name = "Jazz Turnaround",
            category = "Jazz",
            romanDisplay = "Imaj7 – VI7 – II7 – V7",
            description = "Secondary dominants create harmonic momentum",
            examples = "Rhythm Changes endings, many standard codas",
            degrees = listOf("I", "VI", "II", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "rhythm-changes",
            name = "Rhythm Changes",
            category = "Jazz",
            romanDisplay = "I–vi–ii–V7 (A) / III–VI–II–V7 (B)",
            description = "32-bar AABA form; a rite of passage for jazz players",
            examples = "I Got Rhythm (Gershwin), Oleo, Anthropology",
            degrees = listOf("I", "vi", "ii", "V", "III", "VI", "II", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "tritone-sub",
            name = "Tritone Substitution",
            category = "Jazz",
            romanDisplay = "♭II7 – Imaj7",
            description = "♭II7 replaces V7 for chromatic descending bass motion",
            examples = "Bebop standards, Charlie Parker compositions",
            degrees = listOf("♭II", "I"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "three-chord-rock",
            name = "Three-Chord Rock",
            category = "Rock",
            romanDisplay = "I – IV – V7",
            description = "Same numerals as Three-Chord Pop but played as power chords",
            examples = "Rock Around the Clock, many punk/rock songs",
            degrees = listOf("I", "IV", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "blues-rock",
            name = "Mixolydian Vamp",
            category = "Rock",
            romanDisplay = "I7 – ♭VII – IV",
            description = "♭VII borrowed from the Mixolydian mode; the quintessential rock flavour",
            examples = "Sweet Home Alabama, Knockin' on Heaven's Door, Hey Joe",
            degrees = listOf("I", "♭VII", "IV"),
            mode = "mixolydian",
            recommendedScale = "Mixolydian Scale"
        ),
        ProgressionDef(
            id = "aeolian-vamp",
            name = "Aeolian Vamp",
            category = "Rock",
            romanDisplay = "i – ♭VII7 – ♭VI – ♭VII7",
            description = "Natural minor rocking between ♭VI and ♭VII; dark and anthemic",
            examples = "Stairway to Heaven, All Along the Watchtower",
            degrees = listOf("i", "♭VII", "♭VI", "♭VII"),
            mode = "aeolian",
            recommendedScale = "Natural Minor Scale"
        ),
        ProgressionDef(
            id = "dark-ballad",
            name = "Minor Rock Loop",
            category = "Rock",
            romanDisplay = "i – ♭VI – ♭III – ♭VII7",
            description = "Aeolian loop with a resolving quality; huge in stadium rock",
            examples = "Stairway to Heaven (chorus), many alt-rock anthems",
            degrees = listOf("i", "♭VI", "♭III", "♭VII"),
            mode = "aeolian",
            recommendedScale = "Natural Minor Scale"
        ),
        ProgressionDef(
            id = "folk-three-chord",
            name = "Folk Three-Chord",
            category = "Folk / Country",
            romanDisplay = "I – IV – V7 – I",
            description = "Open-position acoustic shapes; the most natural fit for guitar",
            examples = "Wagon Wheel, Take Me Home Country Roads, Blowin' in the Wind",
            degrees = listOf("I", "IV", "V", "I"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "two-chord-vamp",
            name = "Two-Chord Vamp",
            category = "Folk / Country",
            romanDisplay = "I – V7",
            description = "Simplest vehicle for a melody; works well with fingerpicking",
            examples = "Many traditional folk and children's songs",
            degrees = listOf("I", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "bluegrass-loop",
            name = "Bluegrass Loop",
            category = "Folk / Country",
            romanDisplay = "I – IV – I – V7",
            description = "Common in bluegrass and old-time; pairs well with alternating bass",
            examples = "Deep River Blues, many bluegrass standards",
            degrees = listOf("I", "IV", "I", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "pachelbel",
            name = "Pachelbel Canon",
            category = "Folk / Country",
            romanDisplay = "I – V7 – vi – iii – IV – I – IV – V7",
            description = "Stepwise descending bass; especially rich fingerpicked",
            examples = "Pachelbel's Canon, many folk-inspired songs",
            degrees = listOf("I", "V", "vi", "iii", "IV", "I", "IV", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "andalusian",
            name = "Andalusian Cadence",
            category = "Classical / Modal",
            romanDisplay = "i – ♭VII7 – ♭VI – V",
            description = "Descending bass line; V is major for Phrygian tension",
            examples = "Flamenco tradition, Sultans of Swing, Hit the Road Jack",
            degrees = listOf("i", "♭VII", "♭VI", "V"),
            mode = "aeolian",
            recommendedScale = "Natural Minor Scale"
        ),
        ProgressionDef(
            id = "lamento-bass",
            name = "Lamento Bass",
            category = "Classical / Modal",
            romanDisplay = "I – VII – VI – V7",
            description = "Stepwise descending bass in the root position; elegant for fingerstyle",
            examples = "Air on G String, many Baroque pieces",
            degrees = listOf("I", "vii", "vi", "V"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "plagal-cadence",
            name = "Plagal Cadence",
            category = "Classical / Modal",
            romanDisplay = "IV – I",
            description = "The \"Amen\" cadence; restful and conclusive",
            examples = "Hymns, gospel, many rock outros",
            degrees = listOf("IV", "I"),
            mode = "ionian",
            recommendedScale = "Major Scale"
        ),
        ProgressionDef(
            id = "phrygian-vamp",
            name = "Phrygian Vamp",
            category = "Classical / Modal",
            romanDisplay = "i – ♭II (repeat)",
            description = "The ♭II (Neapolitan) chord creates an exotic, suspended tension",
            examples = "Flamenco, Middle Eastern-influenced passages",
            degrees = listOf("i", "♭II", "i", "♭II"),
            mode = "phrygian",
            recommendedScale = "Phrygian Scale"
        ),
    )
}
