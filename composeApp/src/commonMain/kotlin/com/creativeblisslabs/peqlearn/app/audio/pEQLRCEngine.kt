package com.creativeblisslabs.peqlearn.app.audio

import com.creativeblisslabs.peqlearn.app.data.SyncedLine

/**
 * pEQ LRC ENGINE: MULTI-FORMAT SYNCED LYRICS PARSER
 * Supports: LRC, SRT, VTT, Enhanced LRC, and plain text
 */
object pEQLRCEngine {

    // Standard LRC: [mm:ss.xx] or [mm:ss.xxx] or [mm:ss:xx]
    private val LRC_REGEX = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})\]\s*(.*)""")
    
    // Enhanced LRC with word-level: <mm:ss.xx>
    private val ENHANCED_LRC = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})\]\s*(.*)""")
    
    // SRT timestamp: 00:01:23,456 --> 00:01:25,789
    private val SRT_TIMESTAMP = Regex("""(\d{2}):(\d{2}):(\d{2}),(\d{3})\s*-->.*""")
    
    // VTT timestamp: 00:01:23.456 --> 00:01:25.789
    private val VTT_TIMESTAMP = Regex("""(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s*-->.*""")
    
    // Simple timestamp: 1:23 or 01:23
    private val SIMPLE_TIMESTAMP = Regex("""(\d{1,2}):(\d{2})\s+(.+)""")

    fun parse(content: String): List<SyncedLine> {
        val trimmed = content.trim()
        
        // Detect format
        return when {
            trimmed.contains("-->") && (trimmed.contains("WEBVTT") || trimmed.contains(".")) -> parseSrtVtt(trimmed)
            trimmed.contains("-->") -> parseSrtVtt(trimmed)
            trimmed.contains("[") && LRC_REGEX.containsMatchIn(trimmed) -> parseLrc(trimmed)
            else -> emptyList() // Return empty for plain text — caller handles conversion
        }
    }
    
    /**
     * Parse standard LRC and Enhanced LRC formats
     */
    private fun parseLrc(lrcContent: String): List<SyncedLine> {
        val lines = mutableListOf<SyncedLine>()

        lrcContent.lineSequence().forEach { rawLine ->
            // Handle multiple timestamps per line: [00:05.00][00:10.00] text
            val timestamps = mutableListOf<Long>()
            var remaining = rawLine.trim()
            
            while (remaining.startsWith("[")) {
                val match = LRC_REGEX.find(remaining)
                if (match != null) {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val ms = match.groupValues[3].toLong()
                    val timestamp = (min * 60 * 1000) + (sec * 1000) + (if (ms < 100) ms * 10 else ms)
                    timestamps.add(timestamp)
                    
                    val text = match.groupValues[4].trim()
                    remaining = text
                    
                    // Check if there's another timestamp after
                    if (remaining.startsWith("[")) continue
                    
                    // Add line for each timestamp (handles multi-timestamp lines)
                    timestamps.forEach { ts ->
                        if (remaining.isNotEmpty()) {
                            lines.add(
                                SyncedLine(
                                    timestampMs = ts,
                                    text = remaining
                                )
                            )
                        }
                    }
                    break
                } else if (remaining.startsWith("[ti:") || remaining.startsWith("[ar:") || 
                           remaining.startsWith("[al:") || remaining.startsWith("[by:") ||
                           remaining.startsWith("[offset:") || remaining.startsWith("[length:")) {
                    // Metadata tag — skip
                    println("pEQControl: [LRC] Metadata: $remaining")
                    break
                } else {
                    break
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
    
    /**
     * Parse SRT and VTT subtitle formats
     */
    private fun parseSrtVtt(content: String): List<SyncedLine> {
        val lines = mutableListOf<SyncedLine>()
        val blocks = content.split(Regex("""\n\s*\n"""))
        
        for (block in blocks) {
            val blockLines = block.trim().lines().filter { it.isNotBlank() }
            if (blockLines.isEmpty()) continue
            
            // Skip WEBVTT header
            if (blockLines[0].contains("WEBVTT")) continue
            
            var timestampMs: Long? = null
            val textLines = mutableListOf<String>()
            
            for (line in blockLines) {
                // Try SRT timestamp
                val srtMatch = SRT_TIMESTAMP.find(line)
                if (srtMatch != null) {
                    val h = srtMatch.groupValues[1].toLong()
                    val m = srtMatch.groupValues[2].toLong()
                    val s = srtMatch.groupValues[3].toLong()
                    val ms = srtMatch.groupValues[4].toLong()
                    timestampMs = (h * 3600000) + (m * 60000) + (s * 1000) + ms
                    continue
                }
                
                // Try VTT timestamp
                val vttMatch = VTT_TIMESTAMP.find(line)
                if (vttMatch != null) {
                    val h = vttMatch.groupValues[1].toLong()
                    val m = vttMatch.groupValues[2].toLong()
                    val s = vttMatch.groupValues[3].toLong()
                    val ms = vttMatch.groupValues[4].toLong()
                    timestampMs = (h * 3600000) + (m * 60000) + (s * 1000) + ms
                    continue
                }
                
                // Skip sequence numbers (pure digits)
                if (line.trim().all { it.isDigit() }) continue
                
                // Strip HTML tags from VTT
                val cleaned = line.replace(Regex("<[^>]*>"), "").trim()
                if (cleaned.isNotEmpty()) {
                    textLines.add(cleaned)
                }
            }
            
            if (timestampMs != null && textLines.isNotEmpty()) {
                lines.add(
                    SyncedLine(
                        timestampMs = timestampMs,
                        text = textLines.joinToString(" ")
                    )
                )
            }
        }
        
        return lines.sortedBy { it.timestampMs }
    }

    /**
     * Finds the active line based on current playback position.
     */
    fun getActiveLineIndex(lyrics: List<SyncedLine>, currentMs: Long): Int {
        return lyrics.indexOfLast { it.timestampMs <= currentMs }
    }
}
