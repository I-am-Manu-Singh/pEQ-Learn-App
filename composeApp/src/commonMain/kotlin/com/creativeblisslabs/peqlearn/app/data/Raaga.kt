package com.creativeblisslabs.peqlearn.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Full Raaga data class matching both Hindustani and Carnatic ragas.
 * 
 * Constructor parameter order (positional - 29 params total):
 *  1. name            - Raga name
 *  2. thaat           - Parent Thaat/Melakarta group ("N/A" for Carnatic)
 *  3. system          - "Hindustani" or "Carnatic"
 *  4. aroha           - Ascending scale pattern (sargam notation)
 *  5. avaroha         - Descending scale pattern
 *  6. vadi            - Primary resonant note (king swara)
 *  7. samvadi         - Secondary resonant note (minister swara)
 *  8. time            - Performance time / prahar
 *  9. mood            - Rasa / emotional quality
 * 10. semitones       - MIDI semitone offsets from tonic
 * 11. description     - Full description
 * 12. melakarta       - Melakarta number (null for Hindustani)
 * 13. chakra          - Chakra name (Carnatic), null for Hindustani
 * 14. pakad           - Characteristic phrase
 * 15. chalan          - Movement pattern
 * 16. jati            - Scale category (Sampoorna, Audava, etc.)
 * 17. alaap           - Sample alaap phrases
 * 18. bandish         - Composition name
 * 19. sthai           - First verse / sthai lyrics
 * 20. antara          - Second verse / antara lyrics
 * 21. taal            - Associated tala
 * 22. bolTaal         - Tala bol pattern
 * 23. taansPaltas     - Taan patterns
 * 24. jhala           - Jhala / tihai pattern
 * 25. meaning         - English meaning / translation
 * 26. transliteration - Transliteration of lyrics
 * 27. streamUrl       - Optional stream URL
 * 28. accentColor     - Optional accent color (Int?)
 * 29. videoUrl        - Optional video URL
 */
@Serializable
data class Raaga(
    val name: String,
    val thaat: String,
    val system: String = "Hindustani",
    val aroha: String = "",
    val avaroha: String = "",
    val vadi: String = "",
    val samvadi: String = "",
    val time: String = "",
    val mood: String = "",
    val semitones: List<Int> = emptyList(),
    val description: String = "",
    val melakarta: Int? = null,
    val chakra: String? = null,
    val pakad: String = "",
    val chalan: String = "",
    val jati: String = "",
    val alaap: List<String> = emptyList(),
    val bandish: String = "",
    val sthai: String = "",
    val antara: String = "",
    val taal: String = "",
    val bolTaal: String = "",
    val taansPaltas: List<String> = emptyList(),
    val jhala: String = "",
    val meaning: String = "",
    val transliteration: String = "",
    val streamUrl: String? = null,
    val accentColor: Int? = null,
    val videoUrl: String? = null
)