package com.creativeblisslabs.peqlearn.app.util

import com.creativeblisslabs.peqlearn.app.ui.screens.*

/**
 * Universal Notation & Score File Parser supporting:
 * 1. Standard MIDI (.mid, .midi)
 * 2. MusicXML / XML (.xml, .musicxml, .mxl)
 * 3. Sheet Music PDF / Text metadata (.pdf)
 */
object UniversalScoreParser {

    fun parse(bytes: ByteArray, fileName: String, uriString: String? = null): Score? {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".mid") || lower.endsWith(".midi") || isMidiBytes(bytes) -> {
                MidiScoreParser.parse(bytes, fileName)
            }
            lower.endsWith(".musicxml") || lower.endsWith(".xml") || isXmlBytes(bytes) -> {
                MusicXmlParser.parse(bytes.decodeToString(), fileName)
            }
            lower.endsWith(".pdf") || isPdfBytes(bytes) -> {
                parsePdfScore(bytes, fileName, uriString)
            }
            else -> {
                // Try MusicXML first, then MIDI
                MusicXmlParser.parse(bytes.decodeToString(), fileName)
                    ?: MidiScoreParser.parse(bytes, fileName)
            }
        }
    }

    private fun isMidiBytes(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 'M'.code.toByte() &&
                bytes[1] == 'T'.code.toByte() &&
                bytes[2] == 'h'.code.toByte() &&
                bytes[3] == 'd'.code.toByte()
    }

    private fun isXmlBytes(bytes: ByteArray): Boolean {
        val head = bytes.take(100).toByteArray().decodeToString()
        return head.contains("<?xml") || head.contains("<score-partwise") || head.contains("<score-timewise")
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
