package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import peq_learn.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import com.creativeblisslabs.peqlearn.app.audio.getAudioEngine
import com.creativeblisslabs.peqlearn.app.data.*
import com.creativeblisslabs.peqlearn.app.util.SwarlipiParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Classical Indian & Carnatic Swarlipi (Bhatkhande & Vishnu Digambar) Notation Viewer.
 * Renders authentic Bandish sheet music for Sthai and Antara with:
 * - Saptak indicators: Taar (dot above), Mandra (dot below), Madhya
 * - Swara variations: Komal (horizontal line below), Teevra (vertical stroke above M')
 * - Matra timing cup ties (arc underneath multi-swar beats, e.g. 1/2, 1/4 matra)
 * - Kan swaras (grace notes in superscript)
 * - Meend (glide curve arcs over swaras)
 * - Khatka (parenthesized note ornaments)
 * - Geet Uccharan (aligned lyric syllables under each matra)
 * - Taal Lipi: Sam (✕), Khali (०), and Taali numbers (२, ३, ४) below each vibhag
 * - Interactive Audio Auditioning with visual playhead tracking
 */
@Composable
fun SwarlipiNotationViewer(
    raaga: Raaga,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    playingBeat: Int? = null,
    onPlayNote: ((midi: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedMovement by remember { mutableStateOf("Sthai") } // "Sthai" or "Antara"
    var isPlayingBandish by remember { mutableStateOf(false) }
    var currentMatraIndex by remember { mutableIntStateOf(-1) }
    var notationScript by remember { mutableStateOf("Bhatkhande English") } // "Bhatkhande English", "Devanagari", "Latin"
    val scope = rememberCoroutineScope()
    val swarlipiFont = Font(Res.font.ome_swarlipi)
    val bhatkhandeHindiFont = Font(Res.font.ome_bhatkhande_hindi)
    val bhatkhandeEnglishFont = Font(Res.font.ome_bhatkhande_english)
    val swarlipiFontFamily = remember(swarlipiFont) { FontFamily(swarlipiFont) }
    val bhatkhandeHindiFontFamily = remember(bhatkhandeHindiFont) { FontFamily(bhatkhandeHindiFont) }
    val bhatkhandeEnglishFontFamily = remember(bhatkhandeEnglishFont) { FontFamily(bhatkhandeEnglishFont) }

    // Resolve taal vibhag structure
    val taalName = raaga.taal.ifEmpty { "Teentaal" }
    val (beatsPerVibhag, totalBeats) = remember(taalName) {
        val nameLower = taalName.lowercase()
        when {
            nameLower.contains("teen") -> listOf(4, 4, 4, 4) to 16
            nameLower.contains("jhap") -> listOf(2, 3, 2, 3) to 10
            nameLower.contains("ektaal") -> listOf(2, 2, 2, 2, 2, 2) to 12
            nameLower.contains("rupak") -> listOf(3, 2, 2) to 7
            nameLower.contains("keherwa") -> listOf(4, 4) to 8
            nameLower.contains("adi") -> listOf(4, 2, 2) to 8
            else -> listOf(4, 4, 4, 4) to 16
        }
    }

    // Parse Sthai and Antara into Swarlipi movements
    val sthaiMovement = remember(raaga.name, raaga.sthaiNotation, raaga.sthai) {
        SwarlipiParser.parseMovement(
            movementTitle = "Sthai",
            notationText = raaga.sthaiNotation.ifEmpty { raaga.bandishNotation },
            lyricText = raaga.sthai,
            taalName = taalName,
            beatsPerVibhag = beatsPerVibhag,
            totalBeats = totalBeats
        )
    }

    val antaraMovement = remember(raaga.name, raaga.antaraNotation, raaga.antara) {
        SwarlipiParser.parseMovement(
            movementTitle = "Antara",
            notationText = raaga.antaraNotation,
            lyricText = raaga.antara,
            taalName = taalName,
            beatsPerVibhag = beatsPerVibhag,
            totalBeats = totalBeats
        )
    }

    val activeMovement = if (selectedMovement == "Sthai") sthaiMovement else antaraMovement

    pEQGlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title, Movement Selector (Sthai / Antara), Script Switcher, Play Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SWARLIPI PADDHATHI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = accentColor
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Bhatkhande System",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${raaga.name} • ${raaga.bandish.ifEmpty { "Traditional Bandish" }} (${taalName})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Script & Font Switcher (Bhatkhande English / Devanagari / Latin / Ome Swarlipi)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clickable {
                            notationScript = when (notationScript) {
                                "Bhatkhande English" -> "Devanagari"
                                "Devanagari" -> "Latin"
                                "Latin" -> "Bhatkhande Hindi"
                                "Bhatkhande Hindi" -> "Ome Swarlipi"
                                else -> "Bhatkhande English"
                            }
                        }
                    ) {
                        Text(
                            text = when (notationScript) {
                                "Bhatkhande English" -> "🔤 Bhatkhande (Standard)"
                                "Devanagari" -> "अ Devanagari"
                                "Latin" -> "A Latin"
                                "Bhatkhande Hindi" -> "🎼 Bhatkhande (Hindi)"
                                else -> "🎵 Ome Swarlipi"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Play Bandish Button
                    InstrumentPlayButton(
                        accentColor = accentColor,
                        label = if (isPlayingBandish) "Stop" else "Sing Bandish",
                        isPlaying = isPlayingBandish,
                        onPlayClick = {
                            if (isPlayingBandish) {
                                isPlayingBandish = false
                                currentMatraIndex = -1
                            } else {
                                isPlayingBandish = true
                                val engine = getAudioEngine()
                                engine.setInstrument(ChordInstrumentType.HARMONIUM.soundEngineName)
                                scope.launch {
                                    try {
                                        val allMatras = activeMovement.avartans.flatMap { it.vibhags.flatMap { v -> v.matras } }
                                        allMatras.forEachIndexed { idx, matra ->
                                            if (!isPlayingBandish) return@forEachIndexed
                                            currentMatraIndex = idx
                                            val note = matra.notes.firstOrNull()
                                            if (note != null && !note.isAvagraha) {
                                                val midi = swarToMidi(note.swar, note.saptak, note.variation, 60)
                                                engine.noteOn(midi, 0.85f)
                                                onPlayNote?.invoke(midi)
                                                delay(280L)
                                                engine.noteOff(midi)
                                            } else {
                                                delay(280L)
                                            }
                                        }
                                    } finally {
                                        currentMatraIndex = -1
                                        isPlayingBandish = false
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Movement Selection Tabs (Sthai / Pallavi vs Antara / Charanam)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Sthai", "Antara").forEach { mov ->
                    val isSelected = selectedMovement == mov
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedMovement = mov
                                currentMatraIndex = -1
                            },
                        color = if (isSelected) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (isSelected) accentColor else Color.Transparent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (mov == "Sthai") "स्थायी • Sthai (Pallavi)" else "अंतरा • Antara (Charanam)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Swarlipi Grid: Avartans -> Vibhags -> Matras
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var globalMatraCounter = 0

                activeMovement.avartans.forEachIndexed { aIdx, avartan ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        // Avartan header label
                        Text(
                            text = "आवर्तन ${aIdx + 1} (Avartan ${aIdx + 1})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )

                        // Rhythmic Vibhags Row with authentic division bars ('|')
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF14171E).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            // Left boundary bar
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(72.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            )

                            avartan.vibhags.forEachIndexed { vIdx, vibhag ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    vibhag.matras.forEach { matra ->
                                        val isCurrentActiveMatra = currentMatraIndex == globalMatraCounter
                                        globalMatraCounter++

                                        SwarlipiMatraCell(
                                            matra = matra,
                                            script = notationScript,
                                            accentColor = accentColor,
                                            isHighlighted = isCurrentActiveMatra,
                                            swarlipiFontFamily = swarlipiFontFamily,
                                            bhatkhandeHindiFontFamily = bhatkhandeHindiFontFamily,
                                            bhatkhandeEnglishFontFamily = bhatkhandeEnglishFontFamily
                                        )
                                    }
                                }

                                // Vibhag vertical division bar '|'
                                Box(
                                    modifier = Modifier
                                        .width(if (vIdx == avartan.vibhags.lastIndex) 2.5.dp else 1.5.dp)
                                        .height(72.dp)
                                        .background(
                                            if (vIdx == avartan.vibhags.lastIndex) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Bhatkhande Notation Legend & Guide
            SwarlipiLegendFooter(accentColor = accentColor)
        }
    }
}

/**
 * Individual Matra cell rendering:
 * 1. Grace notes / Kan in superscript
 * 2. Swara note with Taar dot (above), Mandra dot (below), and Komal underline
 * 3. Timing cup-tie arc for multi-note matras (1/2, 1/4 matra)
 * 4. Lyric syllable (Geet Uccharan)
 * 5. Taal Sign: Sam (✕), Khali (०), or Taali Number (२, ३, ४)
 */
@Composable
private fun SwarlipiMatraCell(
    matra: SwarlipiMatra,
    script: String,
    accentColor: Color,
    isHighlighted: Boolean,
    swarlipiFontFamily: FontFamily? = null,
    bhatkhandeHindiFontFamily: FontFamily? = null,
    bhatkhandeEnglishFontFamily: FontFamily? = null
) {
    val count = matra.notes.size.coerceAtLeast(1)
    val cellWidth = (count * 24 + 16).dp

    Column(
        modifier = Modifier
            .width(cellWidth)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isHighlighted) Color(0xFFFFEB3B).copy(alpha = 0.25f) else Color.Transparent)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Matra Number (small subtle beat number)
        Text(
            text = "${matra.matraNumber}",
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            textAlign = TextAlign.Center
        )

        // Swara Notes Row
        Box(contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                matra.notes.forEach { note ->
                    SwaraGlyph(
                        note = note,
                        script = script,
                        accentColor = if (isHighlighted) Color(0xFFFFEB3B) else MaterialTheme.colorScheme.onSurface,
                        swarlipiFontFamily = swarlipiFontFamily,
                        bhatkhandeHindiFontFamily = bhatkhandeHindiFontFamily,
                        bhatkhandeEnglishFontFamily = bhatkhandeEnglishFontFamily
                    )
                }
            }

            // Draw Matra tie cup curve underneath when multiple notes share 1 beat (1/2, 1/4 matra)
            if (matra.notes.size > 1) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 18.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(2f, h - 2f)
                        quadraticTo(w / 2f, h + 4.dp.toPx(), w - 2f, h - 2f)
                    }
                    drawPath(
                        path = path,
                        color = accentColor.copy(alpha = 0.7f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
        }

        // Stroke Row (Mizrab / Instrumental Strokes) when present
        val strokeText = matra.notes.mapNotNull { it.stroke.ifEmpty { null } }.joinToString(" ")
        if (strokeText.isNotEmpty()) {
            Text(
                text = strokeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }

        // Lyric Syllable (Geet Uccharan)
        val lyricCombined = matra.notes.joinToString("") { it.lyric }.ifEmpty { "-" }
        Text(
            text = lyricCombined,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isHighlighted) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        // Taal Sign: Sam (✕), Khali (०), or Taali (२, ३, ४)
        Text(
            text = matra.taalMark.ifEmpty { " " },
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = when (matra.taalMark) {
                "✕" -> Color(0xFFFF5252)
                "०" -> Color(0xFF64B5F6)
                else -> Color(0xFFFFB74D)
            },
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Authentic Swara Glyph representation:
 * - Upper dot for Taar Saptak (Ṡ, Ṙ, Ġ)
 * - Lower dot for Mandra Saptak (Ṇ, Ḍ, Ṗ)
 * - Underline for Komal Swara (R̲, G̲, D̲, N̲)
 * - Vertical stroke for Teevra Madhyam (M' or Ḿ)
 * - Kan swar superscript before note
 */
@Composable
private fun SwaraGlyph(
    note: SwarlipiNote,
    script: String,
    accentColor: Color,
    swarlipiFontFamily: FontFamily? = null,
    bhatkhandeHindiFontFamily: FontFamily? = null,
    bhatkhandeEnglishFontFamily: FontFamily? = null
) {
    if (script == "Ome Swarlipi") {
        // Authentic Ome Swarlipi notation format (https://omenad.github.io/fonts/ome-swarlipi/)
        // Lowercase for Shuddha notes: s, r, g, m, p, d, n
        // Uppercase for Vikrit notes: R, G, D, N (Komal), M (Teevra)
        // Octaves: 'u' suffix for Tar Saptak, 'l' suffix for Mandra Saptak
        // Khatka: enclosed in parentheses e.g. (p)
        val baseNote = when (note.swar.take(1).uppercase()) {
            "S" -> "s"
            "R" -> if (note.variation == SwaraVariation.KOMAL) "R" else "r"
            "G" -> if (note.variation == SwaraVariation.KOMAL) "G" else "g"
            "M" -> if (note.variation == SwaraVariation.TEEVRA) "M" else "m"
            "P" -> "p"
            "D" -> if (note.variation == SwaraVariation.KOMAL) "D" else "d"
            "N" -> if (note.variation == SwaraVariation.KOMAL) "N" else "n"
            "-" -> "-"
            "," -> "-"
            else -> note.swar.lowercase()
        }

        val octaveIndicator = when (note.saptak) {
            SwaraSaptak.TAAR -> "u"
            SwaraSaptak.MANDRA -> "l"
            SwaraSaptak.MADHYA -> ""
        }

        val glyphText = when {
            note.isKhatka -> "(${baseNote}${octaveIndicator})"
            note.kan != null -> "<sup>${note.kan}</sup>${baseNote}${octaveIndicator}"
            else -> "${baseNote}${octaveIndicator}"
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = glyphText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = swarlipiFontFamily ?: FontFamily.Monospace,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val devanagariMap = mapOf(
        "S" to "सा", "R" to "रे", "G" to "ग", "M" to "म", "P" to "प", "D" to "ध", "N" to "नि", "-" to "ऽ"
    )

    val baseLabel = if (script == "Devanagari") {
        devanagariMap[note.swar] ?: note.swar
    } else {
        note.swar
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Taar Saptak dot (dot above)
        Box(modifier = Modifier.height(5.dp), contentAlignment = Alignment.Center) {
            if (note.saptak == SwaraSaptak.TAAR) {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }

        // 2. Main note with optional Kan swar or Khatka
        Row(verticalAlignment = Alignment.Top) {
            if (!note.kan.isNullOrEmpty()) {
                val kanDisplay = if (script == "Devanagari") (devanagariMap[note.kan] ?: note.kan) else note.kan
                Text(
                    text = kanDisplay,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 1.dp)
                )
            }

            val displayText = when {
                note.isKhatka -> "(${if (script == "Devanagari") (devanagariMap[note.swar] ?: note.swar) else note.swar})"
                note.variation == SwaraVariation.TEEVRA -> if (script == "Devanagari") "म॑" else "M'"
                note.variation == SwaraVariation.KOMAL && script != "Devanagari" -> note.swar.lowercase()
                else -> baseLabel
            }

            Text(
                text = displayText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }

        // 3. Mandra Saptak dot (dot below) or Komal underline (horizontal bar)
        Box(modifier = Modifier.height(5.dp), contentAlignment = Alignment.Center) {
            when {
                note.variation == SwaraVariation.KOMAL -> {
                    // Underline below note
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(1.8.dp)
                            .background(accentColor)
                    )
                }
                note.saptak == SwaraSaptak.MANDRA -> {
                    // Dot below note
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
        }
    }
}

/**
 * Compact Legend Explaining Bhatkhande and Vishnu Digambar Notations
 */
@Composable
private fun SwarlipiLegendFooter(accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• Taar: Ṡ / su (upper dot)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("• Mandra: Ṇ / nl (lower dot)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("• Komal: R̲ / R, G, D, N (flat)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("• Teevra: M' / M (sharp)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✕ = Sam", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                    Text("० = Khali", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("• Strokes (Mizrab): ; = Da, ' = Ra, \\ = Dir", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.secondary)
                Text("• Khatka: (p)", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.secondary)
                Text("• Kan: ^m p", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

/**
 * Maps Swara name, Saptak, and Variation to MIDI pitch.
 */
private fun swarToMidi(swar: String, saptak: SwaraSaptak, variation: SwaraVariation, rootMidi: Int = 60): Int {
    val clean = swar.take(1).uppercase()
    val baseOffset = when (clean) {
        "S" -> 0
        "R" -> if (variation == SwaraVariation.KOMAL) 1 else 2
        "G" -> if (variation == SwaraVariation.KOMAL) 3 else 4
        "M" -> if (variation == SwaraVariation.TEEVRA) 6 else 5
        "P" -> 7
        "D" -> if (variation == SwaraVariation.KOMAL) 8 else 9
        "N" -> if (variation == SwaraVariation.KOMAL) 10 else 11
        else -> 0
    }
    val octaveOffset = when (saptak) {
        SwaraSaptak.MANDRA -> -12
        SwaraSaptak.MADHYA -> 0
        SwaraSaptak.TAAR -> 12
    }
    return rootMidi + baseOffset + octaveOffset
}

