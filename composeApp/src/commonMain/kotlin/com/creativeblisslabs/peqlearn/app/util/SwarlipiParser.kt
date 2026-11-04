package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.data.*

/**
 * Parser and Converter for Indian & Carnatic Swarlipi (Bhatkhande & Vishnu Digambar) notations.
 * Follows classical notation specifications used in Swarlipi and Swaranotebook.
 */
object SwarlipiParser {

    /**
     * Parses raw Bandish notation text formatted in either:
     * 1) Standard Bhatkhande grid format (vibhag separators '|', notes, lyrics, and taal marks)
     * 2) Sargam note lines with corresponding lyrics
     */
    fun parseMovement(
        movementTitle: String,
        notationText: String,
        lyricText: String = "",
        taalName: String = "Teentaal",
        beatsPerVibhag: List<Int> = listOf(4, 4, 4, 4),
        totalBeats: Int = 16
    ): SwarlipiMovement {
        if (notationText.isBlank()) {
            return generateDefaultMovement(movementTitle, lyricText, taalName, beatsPerVibhag, totalBeats)
        }

        val rawLines = notationText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val avartans = mutableListOf<SwarlipiAvartan>()
        var currentAvartanNumber = 1

        var idx = 0
        while (idx < rawLines.size) {
            val line = rawLines[idx]

            // Check if line contains vibhags '|'
            if (line.contains("|")) {
                // If next line exists and is a lyric line (does not contain note letters as primary tokens)
                val nextLineLyrics = if (idx + 1 < rawLines.size && !isSwaraLine(rawLines[idx + 1])) {
                    rawLines[idx + 1]
                } else null

                val avartan = parseVibhagLine(
                    swaraLine = line,
                    lyricLine = nextLineLyrics,
                    avartanIndex = currentAvartanNumber++,
                    taalName = taalName,
                    beatsPerVibhag = beatsPerVibhag
                )
                avartans.add(avartan)

                if (nextLineLyrics != null) {
                    idx += 2
                } else {
                    idx += 1
                }
            } else {
                // Space-separated sargam tokens
                val nextLineLyrics = if (idx + 1 < rawLines.size && !isSwaraLine(rawLines[idx + 1])) {
                    rawLines[idx + 1]
                } else null

                val avartan = parseSpaceSeparatedLine(
                    swaraLine = line,
                    lyricLine = nextLineLyrics,
                    avartanIndex = currentAvartanNumber++,
                    taalName = taalName,
                    beatsPerVibhag = beatsPerVibhag,
                    totalBeats = totalBeats
                )
                avartans.add(avartan)
                if (nextLineLyrics != null) {
                    idx += 2
                } else {
                    idx += 1
                }
            }
        }

        return SwarlipiMovement(
            title = movementTitle,
            avartans = avartans,
            rawNotationText = notationText
        )
    }

    private fun parseVibhagLine(
        swaraLine: String,
        lyricLine: String?,
        avartanIndex: Int,
        taalName: String,
        beatsPerVibhag: List<Int>
    ): SwarlipiAvartan {
        val swaraVibhagTokens = swaraLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val lyricVibhagTokens = lyricLine?.split("|")?.map { it.trim() } ?: emptyList()

        val parsedVibhags = mutableListOf<SwarlipiVibhag>()
        var globalBeatCounter = 1

        for (vIdx in swaraVibhagTokens.indices) {
            val vSwaraStr = swaraVibhagTokens[vIdx]
            val vLyricStr = lyricVibhagTokens.getOrNull(vIdx) ?: ""

            val swarTokens = vSwaraStr.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            val lyricTokens = if (vLyricStr.isNotBlank()) vLyricStr.split("\\s+".toRegex()) else emptyList()

            val expectedBeatsInThisVibhag = beatsPerVibhag.getOrElse(vIdx) { swarTokens.size.coerceAtLeast(1) }
            val matras = mutableListOf<SwarlipiMatra>()

            for (mIdx in 0 until expectedBeatsInThisVibhag) {
                val swarToken = swarTokens.getOrNull(mIdx) ?: "-"
                val lyricToken = lyricTokens.getOrNull(mIdx) ?: ""
                val matraNotes = parseMatraSwaras(swarToken, lyricToken)

                val taalSign = resolveTaalSign(taalName, globalBeatCounter, vIdx, mIdx)

                matras.add(
                    SwarlipiMatra(
                        notes = matraNotes,
                        matraNumber = globalBeatCounter,
                        taalMark = taalSign
                    )
                )
                globalBeatCounter++
            }

            parsedVibhags.add(
                SwarlipiVibhag(
                    matras = matras,
                    vibhagIndex = vIdx
                )
            )
        }

        return SwarlipiAvartan(
            vibhags = parsedVibhags,
            avartanNumber = avartanIndex
        )
    }

    private fun parseSpaceSeparatedLine(
        swaraLine: String,
        lyricLine: String?,
        avartanIndex: Int,
        taalName: String,
        beatsPerVibhag: List<Int>,
        totalBeats: Int
    ): SwarlipiAvartan {
        val swarTokens = swaraLine.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val lyricTokens = lyricLine?.split("\\s+".toRegex())?.filter { it.isNotEmpty() } ?: emptyList()

        val parsedVibhags = mutableListOf<SwarlipiVibhag>()
        var tokenIdx = 0
        var globalBeat = 1

        for (vIdx in beatsPerVibhag.indices) {
            val numBeats = beatsPerVibhag[vIdx]
            val matras = mutableListOf<SwarlipiMatra>()

            for (b in 0 until numBeats) {
                val sToken = swarTokens.getOrNull(tokenIdx) ?: "-"
                val lToken = lyricTokens.getOrNull(tokenIdx) ?: ""
                val notes = parseMatraSwaras(sToken, lToken)
                val taalSign = resolveTaalSign(taalName, globalBeat, vIdx, b)

                matras.add(
                    SwarlipiMatra(
                        notes = notes,
                        matraNumber = globalBeat,
                        taalMark = taalSign
                    )
                )
                tokenIdx++
                globalBeat++
            }

            parsedVibhags.add(
                SwarlipiVibhag(
                    matras = matras,
                    vibhagIndex = vIdx
                )
            )
        }

        return SwarlipiAvartan(
            vibhags = parsedVibhags,
            avartanNumber = avartanIndex
        )
    }

    /**
     * Parses individual or compound notes within a single Matra (e.g., "(P)", "S-R", "SR", "m(P)", "S'")
     */
    fun parseMatraSwaras(token: String, lyricToken: String): List<SwarlipiNote> {
        val trimmed = token.trim()
        if (trimmed == "-" || trimmed == "S" || trimmed == "ऽ" || trimmed == ".") {
            return listOf(
                SwarlipiNote(
                    swar = "-",
                    lyric = lyricToken,
                    isAvagraha = true
                )
            )
        }

        // Check if enclosed in parenthesis: Khatka / Krintan / Andolan, e.g. "(P)" or "(PDMP)"
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            val inside = trimmed.substring(1, trimmed.length - 1)
            return listOf(
                SwarlipiNote(
                    swar = inside,
                    lyric = lyricToken,
                    isKhatka = true
                )
            )
        }

        // Check for compound notes joined by hyphen, tilde or sub-beats, e.g. "S~R" (meend) or "S-R"
        if (trimmed.contains("~")) {
            val parts = trimmed.split("~")
            val first = parseSingleSwar(parts[0], lyricToken)
            return listOf(first.copy(meendEnd = parts.getOrNull(1) ?: ""))
        }

        // If multiple uppercase/lowercase swara letters are glued together without spaces, e.g. "SRGM" (1/4 matra each)
        val subTokens = splitCompoundSwars(trimmed)
        if (subTokens.size > 1) {
            val lyricChars = lyricToken.toCharArray()
            return subTokens.mapIndexed { idx, sub ->
                val subLyric = lyricChars.getOrNull(idx)?.toString() ?: (if (idx == 0) lyricToken else "")
                parseSingleSwar(sub, subLyric)
            }
        }

        return listOf(parseSingleSwar(trimmed, lyricToken))
    }

    private fun splitCompoundSwars(text: String): List<String> {
        val regex = "([A-Za-z]['`.]*|_?[A-Za-z]['`.]*)".toRegex()
        val matches = regex.findAll(text).map { it.value }.toList()
        return if (matches.size > 1) matches else listOf(text)
    }

    /**
     * Parse single Swara token into SwarlipiNote with Saptak and Variation (Shuddha, Komal, Teevra).
     */
    fun parseSingleSwar(raw: String, lyric: String): SwarlipiNote {
        var str = raw.trim()

        // Check Mizrab Stroke: ";" (Da), "'" (Ra), "\" (Dir)
        var stroke = ""
        if (str.endsWith(";")) {
            stroke = "Da"
            str = str.removeSuffix(";").trim()
        } else if (str.endsWith("\\")) {
            stroke = "Dir"
            str = str.removeSuffix("\\").trim()
        }

        // Check Kan (grace note superscript before swara), e.g. "^M R" or "M_R" or "MR"
        var kanNote: String? = null
        if (str.startsWith("^") && str.length > 2) {
            kanNote = str.substring(1, 2)
            str = str.substring(2)
        }

        // Check Khatka in parentheses, e.g. "(P)" or "(M)"
        val isKhatka = str.startsWith("(") && str.endsWith(")")
        if (isKhatka) {
            str = str.removeSurrounding("(", ")").trim()
        }

        // Saptak detection:
        // Mandra (lower): lowercase in some traditions, or followed by '.', or prefixed by '.' (e.g. "N.", ".N", "n.", or Ome Swarlipi "Nl")
        // Taar (upper): followed by `'` or dot above (e.g. "S'", "S*", "s'", or Ome Swarlipi "Su")
        var saptak = SwaraSaptak.MADHYA
        if (str.endsWith("u") && str.length > 1 && str.first().isLetter()) {
            saptak = SwaraSaptak.TAAR
            str = str.removeSuffix("u")
        } else if (str.endsWith("l") && str.length > 1 && str.first().isLetter()) {
            saptak = SwaraSaptak.MANDRA
            str = str.removeSuffix("l")
        } else if (str.endsWith("'") || str.endsWith("’") || str.endsWith("*") || str.startsWith("˙") || str.startsWith("•")) {
            saptak = SwaraSaptak.TAAR
            str = str.replace("'", "").replace("’", "").replace("*", "").replace("˙", "").replace("•", "")
        } else if (str.endsWith(".") || str.startsWith(".") || str.endsWith("_") || str.startsWith("_")) {
            saptak = SwaraSaptak.MANDRA
            str = str.replace(".", "").replace("_", "")
        }

        // Variation detection:
        // Komal: lowercase note "r", "g", "d", "n", or "re", "ga", "dha", "ni", or with underline or 'b'
        // Teevra: "M'", "Ma'", "M#", "tM"
        var variation = SwaraVariation.SHUDDHA
        val upper = str.uppercase()

        if (raw.contains("'") && upper.startsWith("M")) {
            // "M'" was explicitly marked as Teevra Madhyam!
            variation = SwaraVariation.TEEVRA
        } else if (str.contains("#") || upper == "MA(T)" || str == "Ma") {
            variation = SwaraVariation.TEEVRA
        } else if (str == "r" || str == "g" || str == "d" || str == "n" ||
            str == "re" || str == "ga" || str == "dha" || str == "ni" ||
            str.startsWith("k") || str.contains("b")
        ) {
            variation = SwaraVariation.KOMAL
        }

        val cleanSwarName = when {
            upper.startsWith("S") -> "S"
            upper.startsWith("R") -> "R"
            upper.startsWith("G") -> "G"
            upper.startsWith("M") -> "M"
            upper.startsWith("P") -> "P"
            upper.startsWith("D") -> "D"
            upper.startsWith("N") -> "N"
            else -> str.take(2).uppercase()
        }

        return SwarlipiNote(
            swar = cleanSwarName,
            saptak = saptak,
            variation = variation,
            kan = kanNote,
            stroke = stroke,
            isKhatka = isKhatka,
            lyric = lyric
        )
    }

    private fun resolveTaalSign(taalName: String, globalBeat: Int, vibhagIdx: Int, beatInVibhag: Int): String {
        if (beatInVibhag != 0) return "" // Taal markers in Bhatkhande appear on the first beat of each vibhag
        val nameLower = taalName.lowercase()
        return when {
            nameLower.contains("teen") -> { // 16 beats: X, 2, 0, 3 (on beats 1, 5, 9, 13)
                when (vibhagIdx) {
                    0 -> "✕"
                    1 -> "२"
                    2 -> "०"
                    3 -> "३"
                    else -> ""
                }
            }
            nameLower.contains("jhap") -> { // 10 beats (2+3+2+3): X, 2, 0, 3
                when (vibhagIdx) {
                    0 -> "✕"
                    1 -> "२"
                    2 -> "०"
                    3 -> "३"
                    else -> ""
                }
            }
            nameLower.contains("ektaal") -> { // 12 beats (2x6): X, 0, 2, 0, 3, 4
                when (vibhagIdx) {
                    0 -> "✕"
                    1 -> "०"
                    2 -> "२"
                    3 -> "०"
                    4 -> "३"
                    5 -> "४"
                    else -> ""
                }
            }
            nameLower.contains("rupak") -> { // 7 beats (3+2+2): 0, 1, 2
                when (vibhagIdx) {
                    0 -> "०"
                    1 -> "१"
                    2 -> "२"
                    else -> ""
                }
            }
            nameLower.contains("keherwa") -> { // 8 beats (4+4): X, 0
                when (vibhagIdx) {
                    0 -> "✕"
                    1 -> "०"
                    else -> ""
                }
            }
            nameLower.contains("adi") -> { // Carnatic Adi Tala (4+2+2): 1, 5, 7
                when (vibhagIdx) {
                    0 -> "१"
                    1 -> "५"
                    2 -> "७"
                    else -> ""
                }
            }
            else -> {
                if (vibhagIdx == 0) "✕" else "${vibhagIdx + 1}"
            }
        }
    }

    /**
     * Determines whether a line contains primarily Swara notation tokens
     * or lyric syllables (Devanagari or text).
     */
    fun isSwaraLine(line: String): Boolean {
        val tokens = line.split("|", " ")
            .map { it.trim().trim(';', '\'', '\\', '.', ',', '-', '~', '(', ')') }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return true
        
        val swaraCount = tokens.count { token ->
            val upper = token.uppercase()
            upper in listOf("S", "R", "G", "M", "P", "D", "N", "SA", "RE", "GA", "MA", "PA", "DHA", "NI") ||
            token in listOf("s", "r", "g", "m", "p", "d", "n", "re", "ga", "ma", "pa", "dha", "ni", "dha", "ga", "ni", "ra") ||
            token.startsWith("R₁") || token.startsWith("R₂") || token.startsWith("R₃") ||
            token.startsWith("G₁") || token.startsWith("G₂") || token.startsWith("G₃") ||
            token.startsWith("M₁") || token.startsWith("M₂") ||
            token.startsWith("D₁") || token.startsWith("D₂") || token.startsWith("D₃") ||
            token.startsWith("N₁") || token.startsWith("N₂") || token.startsWith("N₃")
        }
        return swaraCount >= (tokens.size * 0.4)
    }

    private fun generateDefaultMovement(
        movementTitle: String,
        lyrics: String,
        taalName: String,
        beatsPerVibhag: List<Int>,
        totalBeats: Int
    ): SwarlipiMovement {
        val lyricLines = lyrics.lines().filter { it.isNotBlank() }
        val sampleSwars = listOf("S", "R", "G", "M", "P", "D", "N", "S'")

        val avartans = lyricLines.mapIndexed { aIdx, lLine ->
            val words = lLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val vibhags = beatsPerVibhag.mapIndexed { vIdx, count ->
                val matras = (0 until count).mapIndexed { mIdx, _ ->
                    val globalBeat = (vIdx * count) + mIdx + 1
                    val swar = sampleSwars.getOrElse((globalBeat + aIdx * 2) % sampleSwars.size) { "S" }
                    val word = words.getOrNull(mIdx % words.size.coerceAtLeast(1)) ?: "-"
                    SwarlipiMatra(
                        notes = listOf(parseSingleSwar(swar, word)),
                        matraNumber = globalBeat,
                        taalMark = resolveTaalSign(taalName, globalBeat, vIdx, mIdx)
                    )
                }
                SwarlipiVibhag(matras = matras, vibhagIndex = vIdx)
            }
            SwarlipiAvartan(vibhags = vibhags, avartanNumber = aIdx + 1)
        }

        return SwarlipiMovement(
            title = movementTitle,
            avartans = avartans
        )
    }
}

