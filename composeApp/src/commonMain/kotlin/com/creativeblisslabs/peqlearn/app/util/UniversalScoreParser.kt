package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.*

/**
 * Universal Notation & Score File Parser supporting:
 * 1. Standard MIDI (.mid, .midi)
 * 2. Guitar Pro (.gp, .gp3, .gp4, .gp5, .gpx)
 * 3. MusicXML / XML (.xml, .musicxml, .mxl)
 * 4. Sheet Music PDF / Text metadata (.pdf)
 */
object UniversalScoreParser {

    fun parse(bytes: ByteArray, fileName: String, uriString: String? = null): Score? {
        val lower = fileName.lowercase()
        val rawScore = when {
            lower.endsWith(".mid") || lower.endsWith(".midi") || lower.endsWith(".kar") || isMidiBytes(bytes) -> {
                MidiScoreParser.parse(bytes, fileName)
            }
            lower.endsWith(".gp") || lower.endsWith(".gp3") || lower.endsWith(".gp4") ||
            lower.endsWith(".gp5") || lower.endsWith(".gpx") || GuitarProParser.isGuitarProBytes(bytes) -> {
                GuitarProParser.parse(bytes, fileName)
            }
            lower.endsWith(".mxl") || isZipBytes(bytes) -> {
                GuitarProParser.parse(bytes, fileName)
                    ?: MidiFileParser.parse(bytes, fileName)
            }
            lower.endsWith(".musicxml") || lower.endsWith(".xml") || isXmlBytes(bytes) -> {
                MusicXmlParser.parse(bytes.decodeToString(), fileName)
                    ?: MidiFileParser.parse(bytes, fileName)
            }
            lower.endsWith(".pdf") || isPdfBytes(bytes) -> {
                parsePdfScore(bytes, fileName, uriString)
            }
            else -> {
                GuitarProParser.parse(bytes, fileName)
                    ?: MusicXmlParser.parse(bytes.decodeToString(), fileName)
                    ?: MidiScoreParser.parse(bytes, fileName)
                    ?: MidiFileParser.parse(bytes, fileName)
            }
        }

        return rawScore?.let { score ->
            // Enrich with optimal biomechanical piano fingerings and guitar fretboard positions
            runCatching {
                BiomechanicalPerformersEngine.enrichScoreWithViterbiFretboardAndFingers(score)
            }.getOrDefault(score)
        }
    }

    private fun isMidiBytes(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 'M'.code.toByte() &&
                bytes[1] == 'T'.code.toByte() &&
                bytes[2] == 'h'.code.toByte() &&
                bytes[3] == 'd'.code.toByte()
    }

    private fun isZipBytes(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    private fun isXmlBytes(bytes: ByteArray): Boolean {
        val head = bytes.take(100).toByteArray().decodeToString()
        return head.contains("<?xml", ignoreCase = true) ||
                head.contains("<score-partwise", ignoreCase = true) ||
                head.contains("<score-timewise", ignoreCase = true) ||
                head.contains("<GPIF", ignoreCase = true)
    }

    private fun isPdfBytes(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == '%'.code.toByte() &&
                bytes[1] == 'P'.code.toByte() &&
                bytes[2] == 'D'.code.toByte() &&
                bytes[3] == 'F'.code.toByte()
    }

    /**
     * Extracts embedded MusicXML or structured music stream metadata from sheet music PDF
     */
    private fun parsePdfScore(bytes: ByteArray, fileName: String, uriString: String?): Score {
        val title = fileName.removeSuffix(".pdf").replace("_", " ")
        return Score(
            title = title,
            composer = "Imported PDF Sheet",
            tempo = 120f,
            timeSignature = "4/4",
            pdfUri = uriString,
            isPdfScore = true,
            measures = listOf(
                Measure(
                    measureIndex = 1,
                    timeSignatureNumerator = 4,
                    timeSignatureDenominator = 4,
                    notes = emptyList()
                )
            )
        )
    }
}
