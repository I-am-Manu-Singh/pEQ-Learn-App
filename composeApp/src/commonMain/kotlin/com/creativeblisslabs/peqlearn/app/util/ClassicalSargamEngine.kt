package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.data.Raaga
import com.creativeblisslabs.peqlearn.app.data.Taal
import com.creativeblisslabs.peqlearn.app.ui.screens.KeySignature
import com.creativeblisslabs.peqlearn.app.ui.screens.Measure
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import com.creativeblisslabs.peqlearn.app.ui.screens.ScoreNote

object ClassicalSargamEngine {

    data class SargamNoteEvent(
        val tokenIndex: Int,
        val displaySwara: String,
        val lyricSyllable: String = "",
        val midiNote: Int,
        val durationBeats: Float = 1.0f,
        val isSustain: Boolean = false
    )

    enum class RaagaConcertStage(val badge: String, val title: String) {
        IDLE("", "Ready"),
        ALAAP_NYAS("STAGE 1/6", "Shruti, Vadi–Samvadi & Nyas Alaap"),
        AROHA_AVAROHA("STAGE 2/6", "Aroha (Ascent) & Avaroha (Descent)"),
        PAKAD_CHALAN("STAGE 3/6", "Pakad & Chalan (Signature Phrase)"),
        BANDISH_STHAI("STAGE 4/6", "Bandish Sthai / Pallavi (with Taal Theka)"),
        BANDISH_ANTARA("STAGE 5/6", "Bandish Antara / Charanam (with Taal Theka)"),
        DRUT_TAAN("STAGE 6/6", "Vistaar & Taan Conclusion (Sam Resolution)")
    }

    data class ConcertSequenceStep(
        val stage: RaagaConcertStage,
        val swaraLabel: String,
        val lyricLabel: String,
        val midiNote: Int,
        val durationMs: Long,
        val tablaBol: String? = null,
        val matraIndex: Int = -1
    )

    /**
     * Repairs corrupted Vadi / Samvadi strings where "R" or "P" were replaced by MusicSymbols enum names.
     */
    fun sanitizeVadiSamvadi(raw: String, fallback: String = "Sa"): String {
        val t = raw.trim()
        if (t.isEmpty()) return fallback
        return when {
            t.equals("RELEASE", ignoreCase = true) || t.contains("RELEASE", ignoreCase = true) -> "R"
            t.equals("PULL_OFF", ignoreCase = true) || t.contains("PULL_OFF", ignoreCase = true) -> "P"
            t.equals("HAMMER_ON", ignoreCase = true) -> "M"
            else -> t
        }
    }

    /**
     * Tokenizes a Sargam string ("N. R G Ma P D N S'", "S R₁ G₃ M₁ P, D₁ N₃ S'") into UI display tokens.
     */
    fun tokenizeSargamPhrase(phrase: String): List<String> {
        val cleaned = phrase
            .replace(",", " , ")
            .replace("|", " | ")
            .replace("—", " ")
            .replace("–", " ")
            .trim()
        if (cleaned.isEmpty()) return emptyList()
        val rawTokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (rawTokens.size == 1 && cleaned.contains("-")) {
            cleaned.split("-").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            rawTokens
        }
    }

    /**
     * Resolves a single Hindustani or Carnatic swara token to its semitone offset relative to Madhya Sa (0),
     * using the Raaga's parent `semitones` list to disambiguate Komal/Shuddha/Teevra when unmarked.
     */
    fun parseSingleSwaraSemitone(rawToken: String, ragaSemitones: List<Int>): Int? {
        var tok = rawToken.trim()
            .replace(",", "")
            .replace("(", "")
            .replace(")", "")
        if (tok.isEmpty() || tok == "|" || tok == "-" || tok == "ऽ" || tok == "S" && rawToken == "-") {
            return null
        }

        // Count dots for octave shift before stripping
        val isMandra = tok.startsWith(".") || tok.endsWith(".") || tok.contains("̣")
        val isTaar = tok.endsWith("'") || tok.endsWith("''") || tok.startsWith("'") ||
                tok.contains("̇") || tok.contains("Ṡ") || tok.contains("sa'")
        val isDoubleTaar = tok.endsWith("''")

        // Check explicit Teevra Madhyam marker M' BEFORE stripping single quotes!
        val isExplicitTeevraMa = tok.startsWith("M'") || tok.startsWith("m'") ||
                tok.contains("Ma(t)", ignoreCase = true) || tok.contains("M₂") || tok.contains("M2")

        val base = tok
            .replace(".", "")
            .replace("'", "")
            .replace("*", "")
            .replace("̣", "")
            .replace("̇", "")
            .replace("…", "")
            .trim()

        if (base.isEmpty()) return null

        val ragaSet = ragaSemitones.map { ((it % 12) + 12) % 12 }.toSet()

        val semiInOctave: Int = when {
            // Shadaj (Sa)
            base.equals("S", ignoreCase = true) || base.equals("Sa", ignoreCase = true) || base == "Ṡ" || base == "सा" || base == "सां" -> 0

            // Carnatic numbered Rishabham / Gandharam / Madhyamam / Dhaivatam / Nishadam
            base.startsWith("R₁") || base.startsWith("R1") -> 1
            base.startsWith("R₂") || base.startsWith("R2") || base.startsWith("G₁") || base.startsWith("G1") -> 2
            base.startsWith("R₃") || base.startsWith("R3") || base.startsWith("G₂") || base.startsWith("G2") -> 3
            base.startsWith("G₃") || base.startsWith("G3") -> 4
            base.startsWith("M₁") || base.startsWith("M1") -> 5
            base.startsWith("M₂") || base.startsWith("M2") -> 6
            base.startsWith("D₁") || base.startsWith("D1") -> 8
            base.startsWith("D₂") || base.startsWith("D2") || base.startsWith("N₁") || base.startsWith("N1") -> 9
            base.startsWith("D₃") || base.startsWith("D3") || base.startsWith("N₂") || base.startsWith("N2") -> 10
            base.startsWith("N₃") || base.startsWith("N3") -> 11

            // Hindustani Rishabh (re / r / r1 = Komal 1, Re / R / r2 = Shuddha 2, R3 = Shatshruti 3)
            base == "re" || base == "r" || base == "रे़" || base == "re(k)" || base == "r(k)" -> {
                if (1 in ragaSet) 1 else if (2 in ragaSet) 2 else 1
            }
            base.equals("Re", ignoreCase = true) || base.equals("R", ignoreCase = true) || base == "रे" || base == "re(s)" || base == "r(s)" -> {
                if (2 in ragaSet) 2 else if (1 in ragaSet) 1 else if (3 in ragaSet) 3 else 2
            }

            // Hindustani Gandhar (ga / g = Komal 3, Ga / G = Shuddha 4 or Raga-contextual)
            base == "ga" || base == "g" || base == "ग़" || base == "ga(k)" || base == "g(k)" -> {
                if (3 in ragaSet) 3 else if (4 in ragaSet) 4 else 3
            }
            base.equals("Ga", ignoreCase = true) || base.equals("G", ignoreCase = true) || base == "ग" || base == "ga(s)" || base == "g(s)" -> {
                if (4 in ragaSet) 4 else if (3 in ragaSet) 3 else if (2 in ragaSet) 2 else 4
            }

            // Hindustani Madhyam (M / m = Shuddha 5, Ma / M' = Teevra 6 or Raga-contextual)
            isExplicitTeevraMa || base == "m'" || base == "ma(t)" || base == "m(t)" -> 6
            base == "Ma" -> if (6 in ragaSet && 5 !in ragaSet) 6 else if (5 in ragaSet) 5 else 6
            base == "ma" -> if (6 in ragaSet && 5 !in ragaSet) 6 else if (5 in ragaSet) 5 else 6
            base.equals("M", ignoreCase = true) || base == "म" || base == "म तीव्र" -> {
                if (5 in ragaSet) 5 else if (6 in ragaSet) 6 else 5
            }

            // Pancham (Pa)
            base.equals("P", ignoreCase = true) || base.equals("Pa", ignoreCase = true) || base == "प" -> 7

            // Hindustani Dhaivat (dha / d = Komal 8, Dha / D = Shuddha 9 or Raga-contextual)
            base == "dha" || base == "d" || base == "ध़" || base == "dha(k)" || base == "d(k)" -> {
                if (8 in ragaSet) 8 else if (9 in ragaSet) 9 else 8
            }
            base.equals("Dha", ignoreCase = true) || base.equals("D", ignoreCase = true) || base == "ध" || base == "dha(s)" || base == "d(s)" -> {
                if (9 in ragaSet) 9 else if (8 in ragaSet) 8 else if (10 in ragaSet) 10 else 9
            }

            // Hindustani Nishad (ni / n = Komal 10, Ni / N = Shuddha 11 or Raga-contextual)
            base == "ni" || base == "n" || base == "नि़" || base == "ni(k)" || base == "n(k)" -> {
                if (10 in ragaSet) 10 else if (11 in ragaSet) 11 else 10
            }
            base.equals("Ni", ignoreCase = true) || base.equals("N", ignoreCase = true) || base == "नि" || base == "ni(s)" || base == "n(s)" -> {
                if (11 in ragaSet) 11 else if (10 in ragaSet) 10 else if (9 in ragaSet) 9 else 11
            }

            else -> return null
        }

        val octaveShift = when {
            isDoubleTaar -> 24
            // Do not add +12 if the only quote was M' (Teevra Ma)
            isTaar && !isExplicitTeevraMa -> 12
            isMandra -> -12
            // Automatic Mandra inference: in phrases like "N R G" (Yaman) or "n S ga M" (Bhimpalasi),
            // a leading Ni or Dha before Sa is in Mandra Saptak (-12)
            else -> 0
        }

        return semiInOctave + octaveShift
    }

    /**
     * Parses any Sargam string (Aroha, Avaroha, Pakad, Chalan, or Swarlipi line) into playable MIDI events
     * with exact token-index mapping and contextual octave continuity!
     */
    fun parseSargamToMidiEvents(
        phrase: String,
        ragaSemitones: List<Int>,
        baseSaMidi: Int = 60,
        isDescendingHint: Boolean = false
    ): List<SargamNoteEvent> {
        val tokens = tokenizeSargamPhrase(phrase)
        if (tokens.isEmpty()) return emptyList()

        val events = ArrayList<SargamNoteEvent>(tokens.size)
        var lastMidi = if (isDescendingHint) baseSaMidi + 12 else baseSaMidi
        var seenSaYet = false

        for (i in tokens.indices) {
            val tok = tokens[i]
            if (tok == "|" || tok == ",") continue

            // Sustain / Avagraha ('-', 'ऽ', '...') extends or repeats the previous note smoothly
            if (tok == "-" || tok == "ऽ") {
                events.add(
                    SargamNoteEvent(
                        tokenIndex = i,
                        displaySwara = tok,
                        midiNote = lastMidi,
                        durationBeats = 0.5f,
                        isSustain = true
                    )
                )
                continue
            }

            val hasTrailingDots = tok.endsWith("...") || tok.endsWith("..")
            val rawSemi = parseSingleSwaraSemitone(tok, ragaSemitones) ?: continue

            val hasExplicitOctave = tok.contains(".") || tok.contains("̣") ||
                    (tok.contains("'") && !tok.startsWith("M'") && !tok.startsWith("m'")) ||
                    tok.contains("Ṡ")

            var resolvedSemi = rawSemi
            if (!hasExplicitOctave) {
                val basePc = ((rawSemi % 12) + 12) % 12
                if (basePc == 0) {
                    // If we are at the end of an ascending phrase or start of a descending phrase, top Sa is +12
                    if ((isDescendingHint && i == 0) || (!isDescendingHint && i == tokens.lastIndex && seenSaYet)) {
                        resolvedSemi = 12
                    }
                    seenSaYet = true
                } else if (!seenSaYet && i == 0 && basePc >= 8 && !isDescendingHint) {
                    // Leading Ni or Dha before Sa in Aroha/Pakad (e.g., "N R G Ma P" in Yaman) is Mandra Saptak!
                    resolvedSemi = basePc - 12
                }
            } else if (((rawSemi % 12) + 12) % 12 == 0) {
                seenSaYet = true
            }

            val midi = (baseSaMidi + resolvedSemi).coerceIn(36, 96)
            lastMidi = midi
            events.add(
                SargamNoteEvent(
                    tokenIndex = i,
                    displaySwara = tok,
                    midiNote = midi,
                    durationBeats = if (hasTrailingDots) 1.5f else 1.0f,
                    isSustain = false
                )
            )
        }

        return events
    }

    /**
     * Detects placeholder (", - - - | - - - -") or lyric-mismatched Bandish Swarlipi notations and
     * dynamically synthesizes an authentic, melodically faithful Sthai & Antara Swarlipi grid
     * matching the Raaga's scale, Pakad, Vadi/Samvadi, and actual Devanagari lyrics!
     */
    fun resolveAuthenticRaaga(raga: Raaga): Raaga {
        val cleanVadi = sanitizeVadiSamvadi(raga.vadi, "Sa")
        val cleanSamvadi = sanitizeVadiSamvadi(raga.samvadi, "Pa")

        val isPlaceholderNotation = raga.sthaiNotation.contains(", - - - | - - - -") ||
                raga.sthaiNotation.isBlank() ||
                (raga.system.equals("Carnatic", true) && raga.name == "Todi" && raga.sthaiNotation.contains("लं ग र"))

        // Also check if the Swarlipi lyrics don't match the Sthai Devanagari verse (e.g. Darbari, Bhupali, Hamsadhwani, Marwa, Bihag)
        val firstSthaiWord = raga.sthai.trim().split(Regex("\\s+")).firstOrNull()?.take(2).orEmpty()
        val isLyricMismatch = firstSthaiWord.length >= 2 &&
                raga.sthaiNotation.isNotBlank() &&
                !isPlaceholderNotation &&
                !raga.sthaiNotation.contains(firstSthaiWord.take(1))

        if (!isPlaceholderNotation && !isLyricMismatch && cleanVadi == raga.vadi && cleanSamvadi == raga.samvadi) {
            return raga
        }

        val synthesized = synthesizeSwarlipiFromRaagaAndLyrics(raga)
        return raga.copy(
            vadi = cleanVadi,
            samvadi = cleanSamvadi,
            sthaiNotation = synthesized.first,
            antaraNotation = synthesized.second,
            bandishNotation = "${synthesized.first}\n\n${synthesized.second}"
        )
    }

    private fun synthesizeSwarlipiFromRaagaAndLyrics(raga: Raaga): Pair<String, String> {
        val arohaTokens = tokenizeSargamPhrase(raga.aroha)
            .filter { it !in setOf("|", ",", "-") }
            .ifEmpty { listOf("S", "R", "G", "M", "P", "D", "N", "S'") }
        val avarohaTokens = tokenizeSargamPhrase(raga.avaroha)
            .filter { it !in setOf("|", ",", "-") }
            .ifEmpty { arohaTokens.reversed() }
        val pakadTokens = tokenizeSargamPhrase(raga.pakad)
            .filter { it !in setOf("|", ",", "-") }
            .map { it.replace("...", "").replace("..", "") }
            .ifEmpty { arohaTokens.take(4) + avarohaTokens.takeLast(4) }

        val n = arohaTokens.size.coerceAtLeast(5)
        fun sw(idx: Int, fromAsc: Boolean = true): String {
            val list = if (fromAsc) arohaTokens else avarohaTokens
            return list[idx.coerceIn(0, list.lastIndex)]
        }
        fun pk(idx: Int): String = pakadTokens[idx % pakadTokens.size]

        // Sthai melodic contour (16 beats = 4 vibhags of 4 beats):
        // Line 1 centers on Pakad & Madhya Saptak Vadi; Line 2 rises to Uttaranga and resolves to Sam (Sa)
        val sthaiSwarasLine1 = listOf(
            pk(0), pk(1), pk(2), pk(3),
            sw(1), sw(2), sw(3), "-",
            sw(4 % n), sw(3 % n), sw(2 % n), sw(1 % n),
            pk(0), "-", sw(0), "-"
        )
        val sthaiSwarasLine2 = listOf(
            sw(2 % n), sw(3 % n), sw(4 % n), sw(5 % n),
            sw(n - 1), "-", sw(1, false), sw(2, false),
            sw(3, false), sw(4 % avarohaTokens.size, false), sw(2 % n), sw(1 % n),
            sw(0), "-", "-", "-"
        )

        // Antara melodic contour (16 beats = 4 vibhags of 4 beats):
        // Line 1 climbs to Taar Saptak (S' R' S'); Line 2 cascades down the full Avaroha back to Sa
        val topSa = arohaTokens.lastOrNull() ?: "S'"
        val secondSwara = arohaTokens.getOrElse(1) { "R" }.replace("'", "") + "'"
        val antaraSwarasLine1 = listOf(
            sw((n / 2).coerceIn(1, n - 2)), sw((n / 2 + 1).coerceIn(1, n - 1)), topSa, topSa,
            topSa, "-", secondSwara, topSa,
            sw(1, false), sw(2, false), sw(3, false), "-",
            sw(2, false), sw(1, false), topSa, "-"
        )
        val antaraSwarasLine2 = listOf(
            topSa, sw(1, false), sw(2, false), sw(3, false),
            sw(4 % avarohaTokens.size, false), "-", sw(2 % n), sw(3 % n),
            pk(0), pk(1), sw(1 % n), sw(0),
            sw(0), "-", "-", "-"
        )

        val sthaiLines = raga.sthai.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val antaraLines = raga.antara.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        val sthaiFormatted = buildTwoLineSwarlipiBlock(
            swaras1 = sthaiSwarasLine1,
            lyricLine1 = sthaiLines.getOrElse(0) { raga.bandish.ifBlank { raga.name } },
            swaras2 = sthaiSwarasLine2,
            lyricLine2 = sthaiLines.getOrElse(1) { sthaiLines.firstOrNull().orEmpty() }
        )

        val antaraFormatted = buildTwoLineSwarlipiBlock(
            swaras1 = antaraSwarasLine1,
            lyricLine1 = antaraLines.getOrElse(0) { "अंतरा चरण" },
            swaras2 = antaraSwarasLine2,
            lyricLine2 = antaraLines.getOrElse(1) { antaraLines.firstOrNull().orEmpty() }
        )

        return sthaiFormatted to antaraFormatted
    }

    /**
     * Splits a Devanagari or Latin lyric line into 16 rhythmic akshara/matra slots (4 vibhags of 4 beats)
     * aligned 1:1 under the 16 swara tokens.
     */
    private fun splitLyricsInto16Matras(lyricLine: String): List<String> {
        val words = lyricLine.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return List(16) { "-" }

        // Group Devanagari consonant + combining vowel matras into natural syllables (aksharas)
        val syllables = mutableListOf<String>()
        for (word in words) {
            var cur = StringBuilder()
            for (ch in word) {
                val type = Character.getType(ch)
                val isCombining = type == Character.NON_SPACING_MARK.toInt() ||
                        type == Character.COMBINING_SPACING_MARK.toInt() ||
                        ch in '\u0900'..'\u0903' || ch in '\u093A'..'\u094F' || ch in '\u0951'..'\u0957' || ch in '\u0962'..'\u0963'
                if (cur.isNotEmpty() && !isCombining) {
                    syllables.add(cur.toString())
                    cur = StringBuilder()
                }
                cur.append(ch)
            }
            if (cur.isNotEmpty()) syllables.add(cur.toString())
        }

        val slots = MutableList(16) { "-" }
        if (syllables.size >= 16) {
            for (i in 0 until 16) slots[i] = syllables[i]
        } else {
            for (i in syllables.indices) {
                val targetIdx = ((i * 15) / syllables.size.coerceAtLeast(1)).coerceIn(0, 15)
                if (slots[targetIdx] == "-") {
                    slots[targetIdx] = syllables[i]
                } else {
                    val nextFree = (targetIdx until 16).firstOrNull { slots[it] == "-" } ?: targetIdx
                    slots[nextFree] = syllables[i]
                }
            }
        }
        return slots
    }

    private fun buildTwoLineSwarlipiBlock(
        swaras1: List<String>,
        lyricLine1: String,
        swaras2: List<String>,
        lyricLine2: String
    ): String {
        fun format16(swaras: List<String>, lyrics: List<String>): String {
            val swVibhags = (0 until 4).joinToString(" | ") { v ->
                (0 until 4).joinToString(" ") { b -> swaras.getOrElse(v * 4 + b) { "-" } }
            }
            val lyrVibhags = (0 until 4).joinToString(" | ") { v ->
                (0 until 4).joinToString(" ") { b -> lyrics.getOrElse(v * 4 + b) { "-" } }
            }
            return "$swVibhags\n$lyrVibhags"
        }

        val lyr1 = splitLyricsInto16Matras(lyricLine1)
        val lyr2 = splitLyricsInto16Matras(lyricLine2)
        return "${format16(swaras1, lyr1)}\n\n${format16(swaras2, lyr2)}"
    }

    /**
     * Parses a 2-line or 4-line Swarlipi notation block (where even lines are Swaras and odd lines are Lyrics)
     * into playable [SargamNoteEvent]s carrying both the Swara and its aligned Lyric syllable!
     */
    fun parseBandishSwarlipiToEvents(
        notationBlock: String,
        ragaSemitones: List<Int>,
        baseSaMidi: Int = 60
    ): List<SargamNoteEvent> {
        val lines = notationBlock.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val events = mutableListOf<SargamNoteEvent>()
        var globalTokenIdx = 0
        var lastMidi = baseSaMidi

        var i = 0
        while (i < lines.size) {
            val swaraLine = lines[i]
            val lyricLine = lines.getOrNull(i + 1).orEmpty()

            val swTokens = swaraLine.split("|", " ").map { it.trim() }.filter { it.isNotEmpty() }
            val lyrTokens = lyricLine.split("|", " ").map { it.trim() }.filter { it.isNotEmpty() }

            for (idx in swTokens.indices) {
                val sw = swTokens[idx]
                val lyr = lyrTokens.getOrElse(idx) { "" }
                if (sw == "-" || sw == "ऽ" || sw == ",") {
                    events.add(
                        SargamNoteEvent(
                            tokenIndex = globalTokenIdx++,
                            displaySwara = sw,
                            lyricSyllable = if (lyr == "-") "" else lyr,
                            midiNote = lastMidi,
                            durationBeats = 0.5f,
                            isSustain = true
                        )
                    )
                } else {
                    val semi = parseSingleSwaraSemitone(sw, ragaSemitones)
                    if (semi != null) {
                        val midi = (baseSaMidi + semi).coerceIn(48, 84)
                        lastMidi = midi
                        events.add(
                            SargamNoteEvent(
                                tokenIndex = globalTokenIdx++,
                                displaySwara = sw,
                                lyricSyllable = if (lyr == "-") "" else lyr,
                                midiNote = midi,
                                durationBeats = 0.5f,
                                isSustain = false
                            )
                        )
                    }
                }
            }
            i += 2
        }
        return events
    }

    /**
     * Builds the complete 6-stage authentic concert demonstration of a Raaga:
     *  1. Shruti & Vadi-Samvadi Nyas Alaap
     *  2. Aroha & Avaroha
     *  3. Pakad & Chalan (Signature Catch Phrase)
     *  4. Bandish Sthai / Pallavi (accompanied by Taal Theka Bols)
     *  5. Bandish Antara / Charanam (accompanied by Taal Theka Bols)
     *  6. Drut Taan / Vistaar ending on Sam (Sa)
     */
    fun buildAuthenticRaagaConcertSequence(
        rawRaga: Raaga,
        taal: Taal,
        baseSaMidi: Int = 60
    ): List<ConcertSequenceStep> {
        val raga = resolveAuthenticRaaga(rawRaga)
        val steps = mutableListOf<ConcertSequenceStep>()
        val semitones = raga.semitones.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11) }

        // STAGE 1: Shruti Sa-Pa-Sa & Vadi-Samvadi Nyas Alaap
        val vadiSemi = parseSingleSwaraSemitone(raga.vadi, semitones) ?: semitones.getOrElse(2) { 4 }
        val samvadiSemi = parseSingleSwaraSemitone(raga.samvadi, semitones) ?: semitones.getOrElse(5) { 9 }
        val alaapNotes = listOf(
            Triple("Sa", "षड्ज (Tonic Drone)", baseSaMidi to 550L),
            Triple(raga.vadi.ifBlank { "Ga" }, "वादी स्वर (Vadi Nyas)", (baseSaMidi + vadiSemi) to 600L),
            Triple(raga.samvadi.ifBlank { "Ni" }, "संवादी स्वर (Samvadi)", (baseSaMidi + samvadiSemi) to 600L),
            Triple("Sa", "न्यास (Resolution)", baseSaMidi to 550L)
        )
        for ((sw, desc, pair) in alaapNotes) {
            steps.add(
                ConcertSequenceStep(
                    stage = RaagaConcertStage.ALAAP_NYAS,
                    swaraLabel = sw,
                    lyricLabel = desc,
                    midiNote = pair.first.coerceIn(48, 84),
                    durationMs = pair.second
                )
            )
        }

        // STAGE 2: Aroha & Avaroha
        val arohaEvents = parseSargamToMidiEvents(raga.aroha, semitones, baseSaMidi, isDescendingHint = false)
        arohaEvents.forEach { ev ->
            steps.add(
                ConcertSequenceStep(
                    stage = RaagaConcertStage.AROHA_AVAROHA,
                    swaraLabel = ev.displaySwara,
                    lyricLabel = "Aroha (Ascent)",
                    midiNote = ev.midiNote,
                    durationMs = 340L
                )
            )
        }
        val avarohaEvents = parseSargamToMidiEvents(raga.avaroha, semitones, baseSaMidi, isDescendingHint = true)
        avarohaEvents.forEach { ev ->
            steps.add(
                ConcertSequenceStep(
                    stage = RaagaConcertStage.AROHA_AVAROHA,
                    swaraLabel = ev.displaySwara,
                    lyricLabel = "Avaroha (Descent)",
                    midiNote = ev.midiNote,
                    durationMs = 340L
                )
            )
        }

        // STAGE 3: Pakad & Chalan
        val pakadPhrase = "${raga.pakad} , ${raga.chalan}".trim().trim(',')
        val pakadEvents = parseSargamToMidiEvents(pakadPhrase, semitones, baseSaMidi, isDescendingHint = false)
        pakadEvents.forEach { ev ->
            steps.add(
                ConcertSequenceStep(
                    stage = RaagaConcertStage.PAKAD_CHALAN,
                    swaraLabel = ev.displaySwara,
                    lyricLabel = "Pakad / Chalan",
                    midiNote = ev.midiNote,
                    durationMs = if (ev.isSustain) 240L else 380L
                )
            )
        }

        // STAGE 4: Bandish Sthai / Pallavi with synchronized Tabla Theka Bols!
        val bolSeq = taal.bolSequence.ifEmpty { listOf("Dha", "Dhin", "Dhin", "Dha") }
        var matraCounter = 0
        val sthaiEvents = parseBandishSwarlipiToEvents(raga.sthaiNotation, semitones, baseSaMidi)
        sthaiEvents.forEach { ev ->
            val mIdx = matraCounter % bolSeq.size
            val bol = if (!ev.isSustain || mIdx == 0) bolSeq[mIdx] else null
            matraCounter++
            steps.add(
                ConcertSequenceStep(
                    stage = RaagaConcertStage.BANDISH_STHAI,
                    swaraLabel = ev.displaySwara,
                    lyricLabel = ev.lyricSyllable.ifBlank { "Sthai" },
                    midiNote = ev.midiNote,
                    durationMs = 320L,
                    tablaBol = bol,
                    matraIndex = mIdx
                )
            )
        }

        // STAGE 5: Bandish Antara / Charanam with synchronized Tabla Theka Bols!
        val antaraEvents = parseBandishSwarlipiToEvents(raga.antaraNotation, semitones, baseSaMidi)
        antaraEvents.forEach { ev ->
            val mIdx = matraCounter % bolSeq.size
            val bol = if (!ev.isSustain || mIdx == 0) bolSeq[mIdx] else null
            matraCounter++
            steps.add(
                ConcertSequenceStep(
                    stage = RaagaConcertStage.BANDISH_ANTARA,
                    swaraLabel = ev.displaySwara,
                    lyricLabel = ev.lyricSyllable.ifBlank { "Antara" },
                    midiNote = ev.midiNote,
                    durationMs = 320L,
                    tablaBol = bol,
                    matraIndex = mIdx
                )
            )
        }

        // STAGE 6: Fast Taan / Vistaar concluding on Sam (Sa) with Sam Tabla Dha!
        val taanString = raga.taansPaltas.firstOrNull().orEmpty().ifBlank { "${raga.aroha} ${raga.avaroha}" }
        val taanEvents = parseSargamToMidiEvents(taanString, semitones, baseSaMidi, isDescendingHint = false)
        taanEvents.forEachIndexed { idx, ev ->
            val isFinalSam = idx == taanEvents.lastIndex
            steps.add(
                ConcertSequenceStep(
                    stage = RaagaConcertStage.DRUT_TAAN,
                    swaraLabel = ev.displaySwara,
                    lyricLabel = if (isFinalSam) "सम (Sam Resolution)" else "Drut Taan",
                    midiNote = ev.midiNote,
                    durationMs = if (isFinalSam) 650L else 185L,
                    tablaBol = if (isFinalSam || idx % 4 == 0) bolSeq[0] else null,
                    matraIndex = if (isFinalSam) 0 else idx % bolSeq.size
                )
            )
        }

        return steps
    }

    /**
     * Converts the full Raaga (Aroha, Avaroha, Pakad, Sthai, Antara) into a multi-measure [Score]
     * enriched with Viterbi guitar `(string, fret)` and RoboPianist piano fingerings (1..5)
     * so `StaffAndTabNotationViewer` displays the complete Raaga composition with lyrics & sargam!
     */
    fun buildCompleteRaagaScore(
        rawRaga: Raaga,
        taal: Taal,
        baseSaMidi: Int = 60
    ): Score {
        val raga = resolveAuthenticRaaga(rawRaga)
        val semitones = raga.semitones.ifEmpty { listOf(0, 2, 4, 5, 7, 9, 11) }

        val arohaEv = parseSargamToMidiEvents(raga.aroha, semitones, baseSaMidi, false)
        val avarohaEv = parseSargamToMidiEvents(raga.avaroha, semitones, baseSaMidi, true)
        val pakadEv = parseSargamToMidiEvents(raga.pakad, semitones, baseSaMidi, false)
        val sthaiEv = parseBandishSwarlipiToEvents(raga.sthaiNotation, semitones, baseSaMidi)
            .filter { !it.isSustain }
        val antaraEv = parseBandishSwarlipiToEvents(raga.antaraNotation, semitones, baseSaMidi)
            .filter { !it.isSustain }

        val allEvents = (arohaEv + avarohaEv + pakadEv + sthaiEv + antaraEv).ifEmpty {
            semitones.mapIndexed { idx, s -> SargamNoteEvent(idx, "S", "", baseSaMidi + s, 0.5f) }
        }

        val measures = mutableListOf<Measure>()
        val notesPerMeasure = 8 // 8 eighth-notes per 4/4 measure
        var noteId = 1

        allEvents.chunked(notesPerMeasure).forEachIndexed { mIdx, chunk ->
            val mNotes = chunk.mapIndexed { idxInBar, ev ->
                val lyr = if (ev.lyricSyllable.isNotBlank()) "${ev.lyricSyllable} (${ev.displaySwara})" else ev.displaySwara
                ScoreNote(
                    id = noteId++,
                    midiNote = ev.midiNote,
                    position = idxInBar * 0.5f,
                    duration = 0.5f,
                    voice = 0,
                    lyric = lyr
                )
            }
            val rehearsal = when (mIdx) {
                0 -> "Aroha / Avaroha"
                2 -> "Pakad & Sthai"
                else -> ""
            }
            measures.add(
                Measure(
                    measureIndex = mIdx + 1,
                    timeSignatureNumerator = 4,
                    timeSignatureDenominator = 4,
                    keySignature = KeySignature.C_MAJOR,
                    notes = mNotes,
                    rehearsalMark = rehearsal,
                    barline = if (mIdx == (allEvents.size - 1) / notesPerMeasure) "final" else "single"
                )
            )
        }

        val rawScore = Score(
            title = "${raga.name} — ${raga.bandish.ifBlank { "Classical Composition" }}",
            composer = "${raga.system} • ${raga.thaat.takeIf { it != "N/A" } ?: (raga.chakra ?: "")} • ${taal.name}",
            tempo = 108f,
            timeSignature = "4/4",
            keySignature = KeySignature.C_MAJOR,
            measures = measures
        )

        return BiomechanicalPerformersEngine.enrichScoreWithViterbiFretboardAndFingers(rawScore)
    }
}