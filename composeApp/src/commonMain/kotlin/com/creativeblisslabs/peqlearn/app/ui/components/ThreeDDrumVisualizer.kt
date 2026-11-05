package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import kotlin.math.pow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Drum3DPart(
    val label: String,
    val displayName: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val height: Float,
    val type: Int, // 0 = Standard Drum (Snare/Tom), 1 = Cymbal, 2 = Horizontal Bass Drum, 3 = Aux Percussion
    val baseColor: Color,
    val standFloorHeight: Float,
    val tiltXDeg: Float = 0f,
    val tiltZDeg: Float = 0f
)

private data class ProjectedPoint3D(
    val x: Float,
    val y: Float,
    val zCam: Float,
    val scale: Float
) {
    val offset: Offset get() = Offset(x, y)
}

private data class Quad3D(
    val p0: ProjectedPoint3D,
    val p1: ProjectedPoint3D,
    val p2: ProjectedPoint3D,
    val p3: ProjectedPoint3D,
    val avgZ: Float,
    val normalX: Float,
    val normalY: Float,
    val normalZ: Float,
    val index: Int
)

@Composable
fun ThreeDDrumVisualizer(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    currentKitLabels: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var yaw by remember { mutableFloatStateOf(16f) }
    var pitch by remember { mutableFloatStateOf(32f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var lastHitDisplay by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // Local hit timestamps for realistic spring vibration & head indentation even when sequencer isn't driving
    val localStrikeTimes = remember { mutableStateMapOf<String, Long>() }
    var frameTick by remember { mutableLongStateOf(0L) }

    // High-speed 60fps physics ticker when any pad or ripple is active
    val anyPadTriggered = viewModel.sequencerTriggeredPads.values.any { it } ||
        viewModel.drumKitRippleProgress.values.any { it > 0f } ||
        localStrikeTimes.isNotEmpty()

    LaunchedEffect(anyPadTriggered) {
        while (anyPadTriggered) {
            val now = System.currentTimeMillis()
            val expired = localStrikeTimes.filterValues { now - it > 550L }.keys.toList()
            expired.forEach { localStrikeTimes.remove(it) }
            frameTick = now
            delay(16L)
        }
    }

    // Hoisted Studio Finishes & Hardware Gradients (Zero per-frame allocation)
    val mahoganyWoodLight = Color(0xFF8C3B2B)
    val mahoganyWoodMid = Color(0xFF582116)
    val mahoganyWoodDark = Color(0xFF240B06)
    val snareSteelLight = Color(0xFFE6E9EF)
    val snareSteelMid = Color(0xFF9AA5B4)
    val snareSteelDark = Color(0xFF3B4351)
    val drumHeadCoated = Color(0xFFFAF8F2)
    val drumHeadClearRing = Color(0xFFDCD8CE)

    val chromeHardwareBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFD6DCE2), Color(0xFF7D8A97), Color(0xFF2B343D))
        )
    }
    val blackHardwareBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF484D54), Color(0xFF202328), Color(0xFF0C0E11))
        )
    }

    // B20 Hand-Hammered Bronze Cymbal Palette
    val cymbalGoldCore = Color(0xFFFFF3B8)
    val cymbalGoldMid = Color(0xFFEBA832)
    val cymbalGoldEdge = Color(0xFFC27D1A)
    val cymbalGoldDarkShadow = Color(0xFF5E3A08)
    val cymbalSpecularHighlight = Color(0xFFFFFEF7)

    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(215, 240, 244, 250)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }

    val drumParts = remember {
        listOf(
            // TIER 1: Center Double Bass Drums (Y=18, Floor=40)
            Drum3DPart("KICK", "BASS DRUM 1", -27f, 18f, 2f, 21f, 26f, 2, mahoganyWoodDark, 40f),
            Drum3DPart("KICK2", "BASS DRUM 2", 27f, 18f, 2f, 21f, 26f, 2, mahoganyWoodDark, 40f),
            // TIER 2: Main Snare, Popcorn Side Snare, & Floor Tom
            Drum3DPart("SNARE", "MAIN SNARE", -54f, 8f, -15f, 14f, 8.5f, 0, snareSteelLight, 40f, tiltXDeg = 6f),
            Drum3DPart("SNARE2", "SIDE SNARE", -85f, 10f, 10f, 11.5f, 7.5f, 0, mahoganyWoodDark, 40f, tiltXDeg = 5f),
            Drum3DPart("FLOOR", "FLOOR TOM", 62f, 13f, -12f, 16.5f, 16.5f, 0, mahoganyWoodDark, 40f, tiltXDeg = 4f),
            // TIER 3: Upper Rack Toms (Rack-mounted with ergonomic drummer tilt)
            Drum3DPart("TOM_HI", "HIGH TOM", -25f, -9f, 14f, 10.5f, 9.5f, 0, mahoganyWoodDark, 18f, tiltXDeg = 14f),
            Drum3DPart("TOM_MID", "MID TOM", 0f, -10f, 19f, 11.5f, 10f, 0, mahoganyWoodDark, 18f, tiltXDeg = 14f),
            Drum3DPart("TOM_LO", "LOW TOM", 25f, -9f, 14f, 12.5f, 10.5f, 0, mahoganyWoodDark, 18f, tiltXDeg = 14f),
            // TIER 4: Cymbals (HH_CLOSED & HH_OPEN share the physical Hi-Hat stand assembly cleanly)
            Drum3DPart("HH_CLOSED", "CLOSED HI-HAT", -75f, -5f, -20f, 13f, 2f, 1, cymbalGoldMid, 40f, tiltXDeg = 4f),
            Drum3DPart("HH_OPEN", "OPEN HI-HAT", -75f, -8.5f, -20f, 13f, 2f, 1, cymbalGoldMid, 40f, tiltXDeg = 4f),
            Drum3DPart("CRASH", "CRASH 1", -62f, -24f, 26f, 15.5f, 1.5f, 1, cymbalGoldMid, 40f, tiltXDeg = 12f, tiltZDeg = 6f),
            Drum3DPart("CRASH2", "CRASH 2", 8f, -26f, 31f, 15f, 1.5f, 1, cymbalGoldMid, 40f, tiltXDeg = 12f, tiltZDeg = -4f),
            Drum3DPart("RIDE", "RIDE CYMBAL", 72f, -17f, 24f, 19f, 1.5f, 1, cymbalGoldMid, 40f, tiltXDeg = 10f, tiltZDeg = -8f),
            // TIER 5: True 3D Auxiliary Percussion (Clap Stack, Steel Cowbell, Studio Shaker)
            Drum3DPart("CLAP", "CLAP STACK", -42f, 3f, -33f, 7f, 3.5f, 3, Color(0xFFE67E22), 40f),
            Drum3DPart("PERC", "COWBELL", 33f, 0f, -31f, 6.2f, 4.8f, 3, Color(0xFF95A5A6), 40f),
            Drum3DPart("SHAKER", "STUDIO SHAKER", 78f, 5f, -29f, 4.8f, 6.5f, 3, Color(0xFF34495E), 40f)
        )
    }

    fun isPartTriggered(partLabel: String): Boolean {
        val cleanPart = partLabel.uppercase()
        if (localStrikeTimes.containsKey(cleanPart)) return true
        if (viewModel.sequencerTriggeredPads[partLabel] == true) return true
        return viewModel.sequencerTriggeredPads.any { (k, v) ->
            if (!v) return@any false
            val cleanKey = k.uppercase().substringAfter("-")
            when {
                cleanKey == cleanPart -> true
                cleanPart == "HH_CLOSED" && (cleanKey == "HH" || cleanKey.contains("CLOSED") || cleanKey.contains("CHAT")) -> true
                cleanPart == "HH_OPEN" && (cleanKey == "OHAT" || cleanKey.contains("OPEN") || cleanKey.contains("OH")) -> true
                cleanPart == "CRASH" && cleanKey == "CRASH" -> true
                cleanPart == "CRASH2" && cleanKey == "CRASH2" -> true
                cleanPart == "RIDE" && cleanKey.contains("RIDE") -> true
                cleanPart == "FLOOR" && cleanKey.contains("FLOOR") -> true
                cleanPart == "TOM_HI" && (cleanKey == "TOM_HI" || cleanKey == "TOM1") -> true
                cleanPart == "TOM_MID" && (cleanKey == "TOM_MID" || cleanKey == "TOM2") -> true
                cleanPart == "TOM_LO" && (cleanKey == "TOM_LO" || cleanKey == "TOM3") -> true
                cleanPart == "KICK" && (cleanKey == "KICK" || cleanKey.contains("BASS")) -> true
                cleanPart == "KICK2" && cleanKey == "KICK2" -> true
                cleanPart == "SNARE" && cleanKey == "SNARE" -> true
                cleanPart == "SNARE2" && cleanKey == "SNARE2" -> true
                cleanPart == "CLAP" && cleanKey.contains("CLAP") -> true
                cleanPart == "PERC" && (cleanKey.contains("PERC") || cleanKey.contains("COWBELL")) -> true
                cleanPart == "SHAKER" && cleanKey.contains("SHAKER") -> true
                else -> false
            }
        }
    }

    fun getStrikeOscillation(partLabel: String): Float {
        // Read frameTick so Compose invalidates draw scope smoothly during decay
        val now = if (frameTick > 0L) frameTick else System.currentTimeMillis()
        val strikeTime = localStrikeTimes[partLabel.uppercase()]
        if (strikeTime != null) {
            val elapsed = (now - strikeTime).coerceAtLeast(0L) / 1000f
            if (elapsed < 0.55f) {
                val decay = (1f - elapsed / 0.55f).pow(2f)
                return sin(elapsed * 38f) * decay
            }
        }
        return if (isPartTriggered(partLabel)) 0.85f else 0f
    }

    fun triggerDrumHit(part: Drum3DPart) {
        localStrikeTimes[part.label.uppercase()] = System.currentTimeMillis()
        lastHitDisplay = part.displayName

        val targetIndex = currentKitLabels.indexOfFirst {
            it.substringAfter("-").equals(part.label, ignoreCase = true)
        }
        val playIndex = if (targetIndex != -1) targetIndex else 0
        val actualLabel = if (targetIndex != -1) currentKitLabels[targetIndex] else part.label

        viewModel.audioEngine.playSample(label = actualLabel, index = playIndex, string = "DRUM")
        scope.launch {
            viewModel.triggerThreeDDrumVisualizer(part.label)
        }
    }

    // Root container respects external `modifier` and preserves aspect ratio without clipping
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF12151C), Color(0xFF090B0E), Color(0xFF050608))
                    } else {
                        listOf(Color(0xFF232832), Color(0xFF161920), Color(0xFF0E1015))
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
    ) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Aspect-ratio-safe uniform scale: guarantees the 260x145 virtual 3D stage fits inside any Modifier box
        val baseSceneScale = min(w / 295f, h / 170f).coerceAtLeast(0.35f) * zoom
        val centerX = w * 0.5f
        val centerY = h * 0.54f
        val cameraDist = 340f

        fun project3DPoint(x: Float, y: Float, z: Float, curYaw: Float = yaw, curPitch: Float = pitch): ProjectedPoint3D {
            val radP = curPitch * (PI.toFloat() / 180f)
            val radY = curYaw * (PI.toFloat() / 180f)
            val x1 = x * cos(radY) - z * sin(radY)
            val z1 = x * sin(radY) + z * cos(radY)
            val y2 = y * cos(radP) - z1 * sin(radP)
            val z2 = y * sin(radP) + z1 * cos(radP)
            val factor = cameraDist / (cameraDist + z2).coerceAtLeast(45f)
            val finalScale = factor * baseSceneScale
            return ProjectedPoint3D(
                x = centerX + x1 * finalScale,
                y = centerY + y2 * finalScale,
                zCam = z2,
                scale = finalScale
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(w, h, baseSceneScale) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yaw = (yaw + dragAmount.x * 0.35f).coerceIn(-85f, 85f)
                        pitch = (pitch - dragAmount.y * 0.35f).coerceIn(16f, 68f)
                    }
                }
                .pointerInput(w, h, baseSceneScale, currentKitLabels) {
                    awaitPointerEventScope {
                        var downPos: Offset? = null
                        var downTime = 0L
                        var dragged = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed && downPos == null) {
                                downPos = change.position
                                downTime = System.currentTimeMillis()
                                dragged = false
                            } else if (change.pressed && downPos != null) {
                                if ((change.position - downPos).getDistance() > 12f) {
                                    dragged = true
                                }
                            } else if (!change.pressed && downPos != null) {
                                val tapOffset = downPos
                                val elapsed = System.currentTimeMillis() - downTime
                                downPos = null

                                if (!dragged && elapsed < 500L) {
                                    val radP = pitch * (PI.toFloat() / 180f)
                                    val aspect = sin(radP).coerceAtLeast(0.32f)

                                    // Sort front-to-back (smallest zCam first) so front drums win hit tests
                                    val candidates = drumParts.map { part ->
                                        val proj = if (part.type == 2) {
                                            project3DPoint(part.x, part.y, part.z - part.height * 0.25f)
                                        } else {
                                            project3DPoint(part.x, part.y, part.z)
                                        }
                                        part to proj
                                    }.sortedBy { it.second.zCam }

                                    var matchedPart: Drum3DPart? = null
                                    for ((part, proj) in candidates) {
                                        if (part.label == "HH_OPEN") continue // Handled via smart Hi-Hat zone check below
                                        val rx = (part.radius * proj.scale * 1.22f).coerceAtLeast(14f)
                                        val ry = if (part.type == 2) {
                                            rx
                                        } else {
                                            (rx * aspect * 1.35f).coerceAtLeast(12f)
                                        }
                                        val dx = (tapOffset.x - proj.x) / rx
                                        val dy = (tapOffset.y - proj.y) / ry
                                        val normDist = sqrt(dx * dx + dy * dy)

                                        if (normDist <= 1.08f) {
                                            matchedPart = if (part.label == "HH_CLOSED" && normDist > 0.65f) {
                                                // Outer edge of Hi-Hat triggers Open Hi-Hat, inner bell/bow triggers Closed Hi-Hat
                                                drumParts.first { it.label == "HH_OPEN" }
                                            } else {
                                                part
                                            }
                                            break
                                        }
                                    }

                                    matchedPart?.let { triggerDrumHit(it) }
                                }
                            }
                        }
                    }
                }
        ) {
            val radP = pitch * (PI.toFloat() / 180f)
            val radY = yaw * (PI.toFloat() / 180f)
            val perspectiveAspect = sin(radP).coerceAtLeast(0.26f)
            val floorLevel = 40f

            // Ambient Studio Stage Backlight Glow
            val stageCenter = project3DPoint(0f, 0f, 10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.14f),
                        Color(0xFFFF9800).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = stageCenter.offset,
                    radius = min(w, h) * 0.72f
                ),
                radius = min(w, h) * 0.72f,
                center = stageCenter.offset
            )

            // ═══════════════════════════════════════════════════════════════════
            // 1. 3D ACOUSTIC HARDWOOD RISER PLATFORM & DRUM RUG
            // ═══════════════════════════════════════════════════════════════════
            val riserHalfW = 116f
            val riserFrontZ = -50f
            val riserBackZ = 56f
            val riserTopY = floorLevel
            val riserThickness = 4.2f

            val pFL = project3DPoint(-riserHalfW, riserTopY, riserFrontZ)
            val pFR = project3DPoint(riserHalfW, riserTopY, riserFrontZ)
            val pBR = project3DPoint(riserHalfW, riserTopY, riserBackZ)
            val pBL = project3DPoint(-riserHalfW, riserTopY, riserBackZ)

            val pFL_b = project3DPoint(-riserHalfW, riserTopY + riserThickness, riserFrontZ)
            val pFR_b = project3DPoint(riserHalfW, riserTopY + riserThickness, riserFrontZ)
            val pBR_b = project3DPoint(riserHalfW, riserTopY + riserThickness, riserBackZ)
            val pBL_b = project3DPoint(-riserHalfW, riserTopY + riserThickness, riserBackZ)

            // Riser drop shadow on studio floor
            val shadowPad = 7f
            val sFL = project3DPoint(-riserHalfW - shadowPad, riserTopY + riserThickness + 1f, riserFrontZ - shadowPad)
            val sFR = project3DPoint(riserHalfW + shadowPad, riserTopY + riserThickness + 1f, riserFrontZ - shadowPad)
            val sBR = project3DPoint(riserHalfW + shadowPad, riserTopY + riserThickness + 1f, riserBackZ + shadowPad)
            val sBL = project3DPoint(-riserHalfW - shadowPad, riserTopY + riserThickness + 1f, riserBackZ + shadowPad)
            drawPath(
                path = Path().apply {
                    moveTo(sFL.x, sFL.y); lineTo(sFR.x, sFR.y); lineTo(sBR.x, sBR.y); lineTo(sBL.x, sBL.y); close()
                },
                color = Color.Black.copy(alpha = 0.45f)
            )

            // Hardwood Deck Top Surface
            val floorPath = Path().apply {
                moveTo(pFL.x, pFL.y)
                lineTo(pFR.x, pFR.y)
                lineTo(pBR.x, pBR.y)
                lineTo(pBL.x, pBL.y)
                close()
            }
            val floorStartY = minOf(pBL.y, pBR.y, pFL.y, pFR.y)
            val floorEndY = maxOf(pBL.y, pBR.y, pFL.y, pFR.y) + 1f
            drawPath(
                path = floorPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF3B2418), Color(0xFF26160E), Color(0xFF1A0F0A)),
                    startY = floorStartY,
                    endY = floorEndY
                )
            )

            // Perspective Hardwood Plank Seams
            val plankCount = 14
            for (plank in 1 until plankCount) {
                val px = -riserHalfW + (2f * riserHalfW * (plank / plankCount.toFloat()))
                val startPt = project3DPoint(px, riserTopY, riserBackZ)
                val endPt = project3DPoint(px, riserTopY, riserFrontZ)
                drawLine(
                    color = Color(0xFF0E0805).copy(alpha = 0.55f),
                    start = startPt.offset,
                    end = endPt.offset,
                    strokeWidth = (1.1f * baseSceneScale).coerceAtLeast(0.7f)
                )
            }

            // Persian/Studio Acoustic Drum Rug under Center Kit
            val rugW = 78f
            val rugFrontZ = -34f
            val rugBackZ = 34f
            val rFL = project3DPoint(-rugW, riserTopY - 0.2f, rugFrontZ)
            val rFR = project3DPoint(rugW, riserTopY - 0.2f, rugFrontZ)
            val rBR = project3DPoint(rugW, riserTopY - 0.2f, rugBackZ)
            val rBL = project3DPoint(-rugW, riserTopY - 0.2f, rugBackZ)
            val rugPath = Path().apply {
                moveTo(rFL.x, rFL.y); lineTo(rFR.x, rFR.y); lineTo(rBR.x, rBR.y); lineTo(rBL.x, rBL.y); close()
            }
            drawPath(
                path = rugPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3E1919).copy(alpha = 0.72f), Color(0xFF241218).copy(alpha = 0.78f)),
                    start = rFL.offset,
                    end = rBR.offset
                )
            )
            drawPath(
                path = rugPath,
                color = Color(0xFFD4AC6E).copy(alpha = 0.35f),
                style = Stroke(width = (1.4f * baseSceneScale).coerceAtLeast(0.8f))
            )

            // Front & Side Bevel Edges of Riser (Handles both positive and negative camera yaw)
            val frontRiserPath = Path().apply {
                moveTo(pFL.x, pFL.y); lineTo(pFR.x, pFR.y); lineTo(pFR_b.x, pFR_b.y); lineTo(pFL_b.x, pFL_b.y); close()
            }
            drawPath(
                path = frontRiserPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF24150E), Color(0xFF0E0805)),
                    startY = minOf(pFL.y, pFL_b.y),
                    endY = maxOf(pFR.y, pFR_b.y) + 1f
                )
            )
            if (yaw < -2f) {
                val rightRiserPath = Path().apply {
                    moveTo(pFR.x, pFR.y); lineTo(pBR.x, pBR.y); lineTo(pBR_b.x, pBR_b.y); lineTo(pFR_b.x, pFR_b.y); close()
                }
                drawPath(rightRiserPath, color = Color(0xFF160D08))
            } else if (yaw > 2f) {
                val leftRiserPath = Path().apply {
                    moveTo(pFL.x, pFL.y); lineTo(pBL.x, pBL.y); lineTo(pBL_b.x, pBL_b.y); lineTo(pFL_b.x, pFL_b.y); close()
                }
                drawPath(leftRiserPath, color = Color(0xFF160D08))
            }
            // Top metallic stage trim
            drawPath(
                path = floorPath,
                color = Color(0xFF8D6E63).copy(alpha = 0.38f),
                style = Stroke(width = (1.2f * baseSceneScale).coerceAtLeast(0.8f))
            )

            // ═══════════════════════════════════════════════════════════════════
            // 2. AMBIENT OCCLUSION FLOOR SHADOWS
            // ═══════════════════════════════════════════════════════════════════
            drumParts.forEach { part ->
                if (part.label == "HH_OPEN") return@forEach
                val shadowCenter = project3DPoint(part.x, floorLevel - 0.1f, part.z)
                val sRad = part.radius * shadowCenter.scale * (if (part.type == 2) 1.25f else 1.05f)
                val sH = sRad * perspectiveAspect * 0.55f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent),
                        center = shadowCenter.offset,
                        radius = sRad.coerceAtLeast(1f)
                    ),
                    topLeft = Offset(shadowCenter.x - sRad, shadowCenter.y - sH),
                    size = Size(sRad * 2f, sH * 2f)
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // 3. CHROME RACK BRIDGE FOR UPPER RACK TOMS
            // ═══════════════════════════════════════════════════════════════════
            val rackLeftPostTop = project3DPoint(-38f, 2f, 17f)
            val rackLeftPostBot = project3DPoint(-38f, floorLevel, 17f)
            val rackRightPostTop = project3DPoint(38f, 2f, 17f)
            val rackRightPostBot = project3DPoint(38f, floorLevel, 17f)
            val rackCenterCurved = project3DPoint(0f, 1f, 22f)

            val tubeStroke = (3.4f * baseSceneScale).coerceAtLeast(1.6f)
            drawLine(chromeHardwareBrush, rackLeftPostTop.offset, rackLeftPostBot.offset, strokeWidth = tubeStroke)
            drawLine(chromeHardwareBrush, rackRightPostTop.offset, rackRightPostBot.offset, strokeWidth = tubeStroke)
            drawLine(chromeHardwareBrush, rackLeftPostTop.offset, rackCenterCurved.offset, strokeWidth = tubeStroke)
            drawLine(chromeHardwareBrush, rackCenterCurved.offset, rackRightPostTop.offset, strokeWidth = tubeStroke)

            // ═══════════════════════════════════════════════════════════════════
            // 4. HI-HAT & DUAL BASS DRUM PEDALS (DYNAMIC KINEMATICS)
            // ═══════════════════════════════════════════════════════════════════
            val isHhOpenTriggered = isPartTriggered("HH_OPEN")
            val isHhClosedTriggered = isPartTriggered("HH_CLOSED")
            val isHhActive = isHhOpenTriggered || isHhClosedTriggered
            val isKick1Active = isPartTriggered("KICK")
            val isKick2Active = isPartTriggered("KICK2")

            // Hi-Hat Foot Pedal
            val hhX = -75f
            val hhZ = -20f
            val hhBaseFront = project3DPoint(hhX, floorLevel, hhZ - 11f)
            val hhBaseRear = project3DPoint(hhX, floorLevel, hhZ + 1f)
            val hhPedalAngleY = when {
                isHhClosedTriggered -> floorLevel - 0.8f
                isHhOpenTriggered -> floorLevel - 4.2f
                else -> floorLevel - 2.6f
            }
            val hhFootboardTip = project3DPoint(hhX, hhPedalAngleY, hhZ - 9f)
            drawLine(blackHardwareBrush, hhBaseRear.offset, hhBaseFront.offset, strokeWidth = 5.5f * baseSceneScale)
            drawLine(chromeHardwareBrush, hhBaseRear.offset, hhFootboardTip.offset, strokeWidth = 4.2f * baseSceneScale)

            // Dual Bass Drum Pedals & Animated Felt Beaters (Drummer side at front -Z)
            listOf(
                Triple("KICK", -27f, isKick1Active),
                Triple("KICK2", 27f, isKick2Active)
            ).forEach { (label, kx, active) ->
                val osc = getStrikeOscillation(label)
                val frontHeadZ = 2f - 13f // Z = -11f
                val pedalRear = project3DPoint(kx, floorLevel, frontHeadZ - 13f)
                val pedalFront = project3DPoint(kx, floorLevel, frontHeadZ - 1f)
                val boardTopY = floorLevel - 3.2f + (if (active) 2.2f else 0f)
                val boardTip = project3DPoint(kx, boardTopY, frontHeadZ - 3.5f)

                drawLine(blackHardwareBrush, pedalRear.offset, pedalFront.offset, strokeWidth = 6.5f * baseSceneScale)
                drawLine(chromeHardwareBrush, pedalRear.offset, boardTip.offset, strokeWidth = 4.8f * baseSceneScale)

                val camCenter = project3DPoint(kx, floorLevel - 6.5f, frontHeadZ - 2.5f)
                val beaterStrikeZ = if (active || abs(osc) > 0.1f) frontHeadZ + 0.4f else frontHeadZ - 4.8f
                val beaterTip = project3DPoint(kx, 18f, beaterStrikeZ)

                drawLine(chromeHardwareBrush, camCenter.offset, beaterTip.offset, strokeWidth = 2.2f * baseSceneScale)
                drawCircle(
                    color = if (active) Color(0xFFFF5252) else Color(0xFFF5F5F5),
                    radius = (3.6f * baseSceneScale).coerceAtLeast(2f),
                    center = beaterTip.offset
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // 5. TRUE 3D CAMERA-SPACE DEPTH SORTING (PAINTER'S ALGORITHM)
            // ═══════════════════════════════════════════════════════════════════
            // Filter out HH_OPEN as a separate physical object since HH_CLOSED renders the unified 2-cymbal Hi-Hat pair
            val physicalParts = drumParts.filter { it.label != "HH_OPEN" }

            val sortedParts = physicalParts.sortedByDescending { part ->
                project3DPoint(part.x, part.y + part.height * 0.5f, part.z).zCam
            }

            sortedParts.forEach { part ->
                // --- A. RENDER ASSOCIATED HARDWARE STAND FIRST (OCCLUDED BY DRUM BODY) ---
                val isRackTom = part.label.startsWith("TOM_")
                if (isRackTom) {
                    // Connect Rack Tom to the Chrome Rack Bar via Ball L-Rod Clamp
                    val tomBottom = project3DPoint(part.x, part.y + part.height, part.z)
                    val clampPoint = project3DPoint(part.x, 1.5f, 18.5f)
                    drawLine(chromeHardwareBrush, tomBottom.offset, clampPoint.offset, strokeWidth = 3.0f * baseSceneScale)
                    drawCircle(Color(0xFF1F2937), radius = 3.5f * baseSceneScale, center = clampPoint.offset)
                    drawCircle(Color(0xFFE5E7EB), radius = 2.0f * baseSceneScale, center = clampPoint.offset)
                } else if (part.type != 2) {
                    val standTop = project3DPoint(part.x, part.y + part.height, part.z)
                    val collarY = floorLevel - 3.8f
                    val floorCollar = project3DPoint(part.x, collarY, part.z)
                    val floorBase = project3DPoint(part.x, floorLevel, part.z)

                    drawLine(
                        brush = chromeHardwareBrush,
                        start = standTop.offset,
                        end = floorBase.offset,
                        strokeWidth = (2.8f * standTop.scale).coerceAtLeast(1.4f)
                    )

                    // Double-Braced Tripod Legs
                    val legSpread = if (part.type == 3) 8.5f else 12.5f
                    for (legIdx in 0..2) {
                        val legAngle = legIdx * (2.0 * PI / 3.0) + 0.4
                        val fx = part.x + (cos(legAngle) * legSpread).toFloat()
                        val fz = part.z + (sin(legAngle) * legSpread).toFloat()
                        val footPt = project3DPoint(fx, floorLevel, fz)
                        val midBraceCollar = project3DPoint(part.x, floorLevel - 1.8f, part.z)
                        val midFoot = Offset((floorCollar.x + footPt.x) * 0.5f, (floorCollar.y + footPt.y) * 0.5f)

                        drawLine(chromeHardwareBrush, floorCollar.offset, footPt.offset, strokeWidth = (2.1f * footPt.scale).coerceAtLeast(1f))
                        drawLine(chromeHardwareBrush, midBraceCollar.offset, midFoot, strokeWidth = (1.3f * footPt.scale).coerceAtLeast(0.8f))
                        drawCircle(Color(0xFF111827), radius = (2.6f * footPt.scale).coerceAtLeast(1.4f), center = footPt.offset)
                    }
                }

                // --- B. COMPUTE STRIKE ANIMATION & RIPPLES ---
                val isActive = if (part.label == "HH_CLOSED") {
                    isHhClosedTriggered || isHhOpenTriggered
                } else {
                    isPartTriggered(part.label)
                }
                val osc = if (part.label == "HH_CLOSED") {
                    val o1 = getStrikeOscillation("HH_CLOSED")
                    val o2 = getStrikeOscillation("HH_OPEN")
                    if (abs(o1) > abs(o2)) o1 else o2
                } else {
                    getStrikeOscillation(part.label)
                }

                val hitBounceY = if (part.type == 0 && (isActive || abs(osc) > 0.05f)) abs(osc) * 1.1f else 0f
                val velocityScale = when {
                    !isActive && abs(osc) < 0.05f -> 1f
                    part.type == 2 -> 1f + 0.055f * max(0.5f, abs(osc))
                    part.type == 1 -> 1f + 0.035f * max(0.5f, abs(osc))
                    else -> 1f + 0.045f * max(0.5f, abs(osc))
                }

                val topCenter = project3DPoint(part.x, part.y + hitBounceY, part.z)
                val bottomCenter = project3DPoint(part.x, part.y + part.height + hitBounceY, part.z)
                val baseRad = part.radius * velocityScale * topCenter.scale
                val bottomRad = part.radius * velocityScale * bottomCenter.scale

                // Tilt-adjusted oval aspect ratio for drums and cymbals
                val effectiveTiltRad = ((pitch + part.tiltXDeg * cos(radY)) * (PI / 180.0)).toFloat().coerceIn(0.22f, 1.45f)
                val localAspect = sin(effectiveTiltRad).coerceAtLeast(0.25f)
                val ovalHeight = baseRad * localAspect
                val bottomOvalHeight = bottomRad * localAspect

                // Expanding Acoustic Strike Wave Ripple
                val ripple = viewModel.drumKitRippleProgress[part.label]
                    ?: if (part.label == "HH_CLOSED") viewModel.drumKitRippleProgress["HH_OPEN"] else null
                if (ripple != null && ripple > 0f) {
                    val rScale = 1f + ripple * 0.85f
                    drawOval(
                        color = primaryColor.copy(alpha = (1f - ripple).coerceIn(0f, 1f) * 0.65f),
                        topLeft = Offset(topCenter.x - baseRad * rScale, topCenter.y - ovalHeight * rScale),
                        size = Size(baseRad * 2f * rScale, ovalHeight * 2f * rScale),
                        style = Stroke(width = (2.2f * topCenter.scale).coerceAtLeast(1f))
                    )
                }

                // --- C. RENDER 3D GEOMETRY BY INSTRUMENT TYPE ---
                when (part.type) {
                    2 -> {
                        // ═══════════════════════════════════════════════════════════════
                        // TYPE 2: TRUE 3D HORIZONTAL BASS DRUMS (DEPTH-SORTED SLATS & HOOPS)
                        // ═══════════════════════════════════════════════════════════════
                        val shellSteps = 28
                        val r = part.radius * velocityScale
                        val frontZ = part.z - part.height * 0.5f
                        val backZ = part.z + part.height * 0.5f

                        val frontRing = Array(shellSteps) { i ->
                            val a = (i.toFloat() / shellSteps) * 2f * PI.toFloat()
                            project3DPoint(part.x + sin(a) * r, part.y + cos(a) * r, frontZ)
                        }
                        val backRing = Array(shellSteps) { i ->
                            val a = (i.toFloat() / shellSteps) * 2f * PI.toFloat()
                            project3DPoint(part.x + sin(a) * r, part.y + cos(a) * r, backZ)
                        }

                        // Build 3D Quads and depth-sort back-to-front so rear slats never paint over front slats
                        val quads = ArrayList<Quad3D>(shellSteps)
                        for (i in 0 until shellSteps) {
                            val next = (i + 1) % shellSteps
                            val midAngle = ((i + 0.5f) / shellSteps) * 2f * PI.toFloat()
                            val p0 = frontRing[i]
                            val p1 = frontRing[next]
                            val p2 = backRing[next]
                            val p3 = backRing[i]
                            val avgZ = (p0.zCam + p1.zCam + p2.zCam + p3.zCam) * 0.25f
                            quads.add(
                                Quad3D(
                                    p0 = p0, p1 = p1, p2 = p2, p3 = p3,
                                    avgZ = avgZ,
                                    normalX = sin(midAngle),
                                    normalY = cos(midAngle),
                                    normalZ = 0f,
                                    index = i
                                )
                            )
                        }
                        quads.sortByDescending { it.avgZ }

                        val frontCenter = project3DPoint(part.x, part.y, frontZ)
                        val backCenter = project3DPoint(part.x, part.y, backZ)

                        fun drawBassDrumHead(ring: Array<ProjectedPoint3D>, center: ProjectedPoint3D, isFrontResonant: Boolean) {
                            val headPath = Path().apply {
                                ring.forEachIndexed { idx, pt ->
                                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                }
                                close()
                            }
                            drawPath(
                                path = headPath,
                                brush = Brush.radialGradient(
                                    colors = if (isActive) {
                                        listOf(Color.White, Color(0xFFFFF59D), drumHeadCoated)
                                    } else if (isFrontResonant) {
                                        listOf(Color(0xFF22252A), Color(0xFF141619), Color(0xFF0B0C0E))
                                    } else {
                                        listOf(drumHeadCoated, drumHeadClearRing)
                                    },
                                    center = center.offset,
                                    radius = (r * center.scale).coerceAtLeast(10f)
                                )
                            )
                            // Perimeter Muffle Ring + Maple Counter-Hoop + Chrome Claws
                            drawPath(headPath, color = drumHeadClearRing.copy(alpha = 0.35f), style = Stroke(width = 3.2f * center.scale))
                            drawPath(
                                path = headPath,
                                brush = Brush.linearGradient(listOf(mahoganyWoodLight, mahoganyWoodDark)),
                                style = Stroke(width = 4.2f * center.scale)
                            )
                            drawPath(headPath, brush = chromeHardwareBrush, style = Stroke(width = 1.6f * center.scale))

                            if (isFrontResonant) {
                                // Acoustic Mic Port Hole & Studio Crest Logo on Front Head
                                val portOffset = project3DPoint(part.x + r * 0.38f, part.y + r * 0.28f, frontZ)
                                val portR = r * 0.19f * portOffset.scale
                                drawCircle(Color(0xFF070809), radius = portR, center = portOffset.offset)
                                drawCircle(Color(0xFFD4AC6E), radius = portR, center = portOffset.offset, style = Stroke(width = 1.4f * portOffset.scale))

                                val logoPt = project3DPoint(part.x, part.y - r * 0.38f, frontZ)
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.75f),
                                    topLeft = Offset(logoPt.x - r * 0.28f * logoPt.scale, logoPt.y - r * 0.08f * logoPt.scale),
                                    size = Size(r * 0.56f * logoPt.scale, r * 0.16f * logoPt.scale),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }

                        // Draw whichever head is farther from the camera first
                        if (frontCenter.zCam > backCenter.zCam) {
                            drawBassDrumHead(frontRing, frontCenter, isFrontResonant = true)
                        } else {
                            drawBassDrumHead(backRing, backCenter, isFrontResonant = false)
                        }

                        // Draw 3D Cylindrical Maple Shell Quads with Continuous Phong-Style Specular Shading
                        quads.forEach { q ->
                            val quadPath = Path().apply {
                                moveTo(q.p0.x, q.p0.y)
                                lineTo(q.p1.x, q.p1.y)
                                lineTo(q.p2.x, q.p2.y)
                                lineTo(q.p3.x, q.p3.y)
                                close()
                            }
                            // Overhead stage light vector (-0.35, -0.82, -0.45)
                            val lightDot = (-0.35f * q.normalX - 0.82f * q.normalY).coerceIn(-1f, 1f)
                            val brightness = ((lightDot + 1f) * 0.5f).coerceIn(0.08f, 1f)
                            val slatColor = lerp(mahoganyWoodDark, mahoganyWoodLight, brightness)
                            val specularBoost = if (brightness > 0.78f) {
                                lerp(slatColor, Color(0xFFFFCCBC), (brightness - 0.78f) * 2.2f)
                            } else slatColor

                            drawPath(quadPath, color = specularBoost)

                            // Chrome Long Tension Rods every 3rd slat
                            if (q.index % 3 == 0 && q.avgZ <= (frontCenter.zCam + backCenter.zCam) * 0.5f + 6f) {
                                val rodStart = Offset((q.p0.x + q.p1.x) * 0.5f, (q.p0.y + q.p1.y) * 0.5f)
                                val rodEnd = Offset((q.p2.x + q.p3.x) * 0.5f, (q.p2.y + q.p3.y) * 0.5f)
                                drawLine(chromeHardwareBrush, rodStart, rodEnd, strokeWidth = (1.5f * q.p0.scale).coerceAtLeast(0.8f))
                            }
                        }

                        // Draw the closer drumhead on top
                        if (frontCenter.zCam <= backCenter.zCam) {
                            drawBassDrumHead(frontRing, frontCenter, isFrontResonant = true)
                        } else {
                            drawBassDrumHead(backRing, backCenter, isFrontResonant = false)
                        }

                        // Telescopic Chrome Bass Drum Spurs
                        val spurL_Mount = project3DPoint(part.x - r * 0.88f, part.y + r * 0.55f, part.z - part.height * 0.2f)
                        val spurR_Mount = project3DPoint(part.x + r * 0.88f, part.y + r * 0.55f, part.z - part.height * 0.2f)
                        val spurL_Foot = project3DPoint(part.x - r * 1.38f, floorLevel, part.z - part.height * 0.42f)
                        val spurR_Foot = project3DPoint(part.x + r * 1.38f, floorLevel, part.z - part.height * 0.42f)

                        drawLine(chromeHardwareBrush, spurL_Mount.offset, spurL_Foot.offset, strokeWidth = 2.8f * spurL_Mount.scale)
                        drawLine(chromeHardwareBrush, spurR_Mount.offset, spurR_Foot.offset, strokeWidth = 2.8f * spurR_Mount.scale)
                        drawCircle(Color(0xFF111827), radius = 3.2f * spurL_Foot.scale, center = spurL_Foot.offset)
                        drawCircle(Color(0xFF111827), radius = 3.2f * spurR_Foot.scale, center = spurR_Foot.offset)
                    }

                    0 -> {
                        // ═══════════════════════════════════════════════════════════════
                        // TYPE 0: CYLINDRICAL SNARES & TOMS (CONTINUOUS PHONG SHADING & LUGS)
                        // ═══════════════════════════════════════════════════════════════
                        val isSnare = part.label.contains("SNARE")

                        // Bottom resonant head & counter-hoop
                        drawOval(
                            color = Color(0xFF1A0D0A),
                            topLeft = Offset(bottomCenter.x - bottomRad, bottomCenter.y - bottomOvalHeight),
                            size = Size(bottomRad * 2f, bottomOvalHeight * 2f)
                        )

                        // Continuous Cylindrical Shell Gradient (No striped polygon seams)
                        val shellBodyPath = Path().apply {
                            moveTo(topCenter.x - baseRad, topCenter.y)
                            lineTo(topCenter.x + baseRad, topCenter.y)
                            lineTo(bottomCenter.x + bottomRad, bottomCenter.y)
                            lineTo(bottomCenter.x - bottomRad, bottomCenter.y)
                            close()
                        }
                        val shellGradient = Brush.horizontalGradient(
                            0.0f to (if (isSnare) snareSteelDark else mahoganyWoodDark),
                            0.28f to (if (isSnare) snareSteelLight else mahoganyWoodLight),
                            0.42f to (if (isSnare) Color.White else Color(0xFFB85A44)),
                            0.68f to (if (isSnare) snareSteelMid else mahoganyWoodMid),
                            1.0f to (if (isSnare) Color(0xFF1D222B) else Color(0xFF140603)),
                            startX = topCenter.x - baseRad,
                            endX = topCenter.x + baseRad
                        )
                        drawPath(shellBodyPath, brush = shellGradient)
                        drawOval(
                            brush = shellGradient,
                            topLeft = Offset(bottomCenter.x - bottomRad, bottomCenter.y - bottomOvalHeight),
                            size = Size(bottomRad * 2f, bottomOvalHeight * 2f)
                        )

                        // Bottom Chrome Die-Cast Hoop
                        drawOval(
                            brush = chromeHardwareBrush,
                            topLeft = Offset(bottomCenter.x - bottomRad, bottomCenter.y - bottomOvalHeight),
                            size = Size(bottomRad * 2f, bottomOvalHeight * 2f),
                            style = Stroke(width = (2.2f * bottomCenter.scale).coerceAtLeast(1.1f))
                        )

                        // Front-facing Chrome Tube Lugs & Tension Rods
                        val lugCount = 8
                        for (lugIdx in 0 until lugCount) {
                            // Angles in [-PI/2 .. +PI/2] face the front/viewer
                            val angle = -PI.toFloat() * 0.44f + (lugIdx / (lugCount - 1f)) * PI.toFloat() * 0.88f
                            val lxTop = topCenter.x + sin(angle) * baseRad * 0.98f
                            val lyTop = topCenter.y + cos(angle) * ovalHeight * 0.98f
                            val lxBot = bottomCenter.x + sin(angle) * bottomRad * 0.98f
                            val lyBot = bottomCenter.y + cos(angle) * bottomOvalHeight * 0.98f

                            drawLine(
                                brush = chromeHardwareBrush,
                                start = Offset(lxTop, lyTop + 1.5f),
                                end = Offset(lxBot, lyBot - 1.5f),
                                strokeWidth = (2.0f * topCenter.scale).coerceAtLeast(1f)
                            )
                            val midLug = Offset((lxTop + lxBot) * 0.5f, (lyTop + lyBot) * 0.5f)
                            drawCircle(
                                color = Color(0xFFECF0F1),
                                radius = (1.7f * topCenter.scale).coerceAtLeast(0.9f),
                                center = midLug
                            )
                        }

                        // Coated Batter Drumhead with 3D Recessed Collar & Strike Glow
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = if (isActive) {
                                    listOf(Color.White, Color(0xFFFFF59D), primaryColor.copy(alpha = 0.55f), drumHeadCoated)
                                } else {
                                    listOf(Color.White, drumHeadCoated, drumHeadClearRing)
                                },
                                center = Offset(topCenter.x - baseRad * 0.15f, topCenter.y - ovalHeight * 0.15f),
                                radius = (baseRad * 1.15f).coerceAtLeast(4f)
                            ),
                            topLeft = Offset(topCenter.x - baseRad * 0.96f, topCenter.y - ovalHeight * 0.96f),
                            size = Size(baseRad * 1.92f, ovalHeight * 1.92f)
                        )

                        // Overtone Control Ring & Center Reverse Power Dot
                        drawOval(
                            color = drumHeadClearRing,
                            topLeft = Offset(topCenter.x - baseRad * 0.84f, topCenter.y - ovalHeight * 0.84f),
                            size = Size(baseRad * 1.68f, ovalHeight * 1.68f),
                            style = Stroke(width = (1.2f * topCenter.scale).coerceAtLeast(0.7f))
                        )
                        drawOval(
                            color = if (isSnare) Color(0xFFD0CCC0).copy(alpha = 0.7f) else Color(0xFFE5E2D9).copy(alpha = 0.45f),
                            topLeft = Offset(topCenter.x - baseRad * 0.26f, topCenter.y - ovalHeight * 0.26f),
                            size = Size(baseRad * 0.52f, ovalHeight * 0.52f)
                        )

                        // Top Triple-Flanged / Die-Cast Chrome Counter-Hoop
                        drawOval(
                            brush = chromeHardwareBrush,
                            topLeft = Offset(topCenter.x - baseRad, topCenter.y - ovalHeight),
                            size = Size(baseRad * 2f, ovalHeight * 2f),
                            style = Stroke(width = (3.0f * topCenter.scale).coerceAtLeast(1.4f))
                        )
                    }

                    3 -> {
                        // ═══════════════════════════════════════════════════════════════
                        // TYPE 3: TRUE 3D AUXILIARY PERCUSSION (COWBELL, CLAP STACK, SHAKER)
                        // ═══════════════════════════════════════════════════════════════
                        when (part.label) {
                            "PERC" -> {
                                // 3D Trapezoidal Brushed-Steel Latin/Rock Cowbell
                                val wFront = baseRad * 1.15f
                                val wBack = baseRad * 0.68f
                                val hBell = (bottomCenter.y - topCenter.y).coerceAtLeast(baseRad * 0.85f)
                                val bellPath = Path().apply {
                                    moveTo(topCenter.x - wBack, topCenter.y - ovalHeight * 0.4f)
                                    lineTo(topCenter.x + wBack, topCenter.y - ovalHeight * 0.4f)
                                    lineTo(topCenter.x + wFront, topCenter.y + hBell)
                                    lineTo(topCenter.x - wFront, topCenter.y + hBell)
                                    close()
                                }
                                drawPath(
                                    path = bellPath,
                                    brush = Brush.horizontalGradient(
                                        colors = if (isActive) {
                                            listOf(Color.White, primaryColor, Color(0xFF5D6D7E))
                                        } else {
                                            listOf(Color(0xFF2C3E50), Color(0xFFBDC3C7), Color(0xFF566573), Color(0xFF1C2833))
                                        },
                                        startX = topCenter.x - wFront,
                                        endX = topCenter.x + wFront
                                    )
                                )
                                drawPath(bellPath, brush = chromeHardwareBrush, style = Stroke(width = 1.6f * topCenter.scale))
                                // Open Mouth of Cowbell at Front Bottom
                                drawOval(
                                    color = Color(0xFF0B0E11),
                                    topLeft = Offset(topCenter.x - wFront * 0.92f, topCenter.y + hBell - ovalHeight * 0.35f),
                                    size = Size(wFront * 1.84f, ovalHeight * 0.7f)
                                )
                            }

                            "CLAP" -> {
                                // 3-Tiered Bent Bronze Hand-Clap Stack
                                for (stackIdx in 2 downTo 0) {
                                    val tierOffset = stackIdx * (2.6f * topCenter.scale)
                                    val tierScale = 1f - stackIdx * 0.11f
                                    val rx = baseRad * tierScale * 1.25f
                                    val ry = ovalHeight * tierScale * 1.25f
                                    val cy = topCenter.y + tierOffset
                                    drawOval(
                                        brush = Brush.radialGradient(
                                            colors = if (isActive && stackIdx == 0) {
                                                listOf(Color.White, Color(0xFFFFB74D), Color(0xFFE65100))
                                            } else {
                                                listOf(Color(0xFFFFCC80), Color(0xFFE67E22), Color(0xFF6E2C00))
                                            },
                                            center = Offset(topCenter.x, cy),
                                            radius = rx.coerceAtLeast(4f)
                                        ),
                                        topLeft = Offset(topCenter.x - rx, cy - ry),
                                        size = Size(rx * 2f, ry * 2f)
                                    )
                                    drawOval(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        topLeft = Offset(topCenter.x - rx, cy - ry),
                                        size = Size(rx * 2f, ry * 2f),
                                        style = Stroke(width = 1f * topCenter.scale)
                                    )
                                }
                            }

                            else -> {
                                // Metallic Knurled Cylindrical Studio Shaker
                                val shakerW = baseRad * 0.95f
                                val shakerH = (bottomCenter.y - topCenter.y).coerceAtLeast(baseRad * 1.4f)
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isActive) {
                                            listOf(Color.White, primaryColor, Color(0xFF1F2937))
                                        } else {
                                            listOf(Color(0xFF1F2937), Color(0xFF9CA3AF), Color(0xFF374151), Color(0xFF111827))
                                        },
                                        startX = topCenter.x - shakerW,
                                        endX = topCenter.x + shakerW
                                    ),
                                    topLeft = Offset(topCenter.x - shakerW, topCenter.y - ovalHeight * 0.5f),
                                    size = Size(shakerW * 2f, shakerH + ovalHeight),
                                    cornerRadius = CornerRadius(shakerW * 0.45f, shakerW * 0.45f)
                                )
                                // Knurled grip bands
                                for (band in 1..3) {
                                    val by = topCenter.y + (shakerH * 0.22f * band)
                                    drawLine(
                                        color = Color(0xFFD1D5DB).copy(alpha = 0.5f),
                                        start = Offset(topCenter.x - shakerW * 0.85f, by),
                                        end = Offset(topCenter.x + shakerW * 0.85f, by),
                                        strokeWidth = 1.4f * topCenter.scale
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // ═══════════════════════════════════════════════════════════════
                        // TYPE 1: B20 BRONZE CYMBALS (ANISOTROPIC SHEEN & HI-HAT CLUTCH)
                        // ═══════════════════════════════════════════════════════════════
                        fun drawLathedCymbalDisc(
                            center: Offset,
                            rx: Float,
                            ry: Float,
                            scaleFactor: Float,
                            highlighted: Boolean,
                            isBottomHat: Boolean = false
                        ) {
                            // Outer metallic rim thickness shadow
                            drawOval(
                                color = Color(0xFF3E2504),
                                topLeft = Offset(center.x - rx, center.y - ry + 1.8f * scaleFactor),
                                size = Size(rx * 2f, ry * 2f)
                            )

                            // B20 Bronze Radial + Anisotropic Specular Bow Fill
                            val cymbalBrush = Brush.radialGradient(
                                colors = if (isBottomHat) {
                                    listOf(cymbalGoldEdge, cymbalGoldDarkShadow, Color(0xFF3B2304))
                                } else if (highlighted) {
                                    listOf(Color.White, cymbalSpecularHighlight, cymbalGoldCore, cymbalGoldMid, cymbalGoldEdge)
                                } else {
                                    listOf(cymbalSpecularHighlight, cymbalGoldCore, cymbalGoldMid, cymbalGoldEdge, cymbalGoldDarkShadow)
                                },
                                center = Offset(center.x - rx * 0.18f, center.y - ry * 0.18f),
                                radius = (rx * 1.18f).coerceAtLeast(4f)
                            )
                            drawOval(
                                brush = cymbalBrush,
                                topLeft = Offset(center.x - rx, center.y - ry),
                                size = Size(rx * 2f, ry * 2f)
                            )

                            if (!isBottomHat) {
                                // Anisotropic bow sheen wedges (signature metallic X-reflection on lathed bronze)
                                val sheenPath = Path().apply {
                                    moveTo(center.x, center.y)
                                    lineTo(center.x - rx * 0.82f, center.y - ry * 0.45f)
                                    lineTo(center.x - rx * 0.45f, center.y - ry * 0.82f)
                                    close()
                                    moveTo(center.x, center.y)
                                    lineTo(center.x + rx * 0.82f, center.y + ry * 0.45f)
                                    lineTo(center.x + rx * 0.45f, center.y + ry * 0.82f)
                                    close()
                                }
                                drawPath(sheenPath, color = Color.White.copy(alpha = if (highlighted) 0.45f else 0.22f))

                                // Fine concentric lathing grooves
                                for (ring in 1..6) {
                                    val frac = 0.24f + ring * 0.12f
                                    drawOval(
                                        color = Color(0xFF3E2723).copy(alpha = 0.14f),
                                        topLeft = Offset(center.x - rx * frac, center.y - ry * frac),
                                        size = Size(rx * 2f * frac, ry * 2f * frac),
                                        style = Stroke(width = (0.7f * scaleFactor).coerceAtLeast(0.5f))
                                    )
                                }

                                // Hand-hammered dimples
                                for (step in 0 until 12) {
                                    val a = (step * 2.0 * PI / 12.0).toFloat()
                                    val hx = center.x + cos(a) * rx * 0.64f
                                    val hy = center.y + sin(a) * ry * 0.64f
                                    drawCircle(
                                        color = Color.Black.copy(alpha = 0.08f),
                                        radius = (1.8f * scaleFactor).coerceAtLeast(0.8f),
                                        center = Offset(hx, hy)
                                    )
                                }

                                // Raised Acoustic Bell (Cup)
                                val bellRx = rx * 0.24f
                                val bellRy = ry * 0.24f
                                drawOval(
                                    brush = Brush.radialGradient(
                                        colors = listOf(cymbalSpecularHighlight, cymbalGoldMid, cymbalGoldDarkShadow),
                                        center = Offset(center.x - bellRx * 0.25f, center.y - bellRy * 0.25f),
                                        radius = bellRx.coerceAtLeast(2f)
                                    ),
                                    topLeft = Offset(center.x - bellRx, center.y - bellRy * 1.15f),
                                    size = Size(bellRx * 2f, bellRy * 2f)
                                )
                                // Chrome Felt & Wingnut
                                drawCircle(Color(0xFF1F2937), radius = (3.2f * scaleFactor).coerceAtLeast(1.5f), center = center)
                                drawCircle(chromeHardwareBrush, radius = (1.8f * scaleFactor).coerceAtLeast(1f), center = center)
                            }
                        }

                        if (part.label == "HH_CLOSED") {
                            // Unified Dual-Cymbal Hi-Hat Pair:
                            // Bottom hat stays anchored; top hat lifts up on HH_OPEN and clamps down on HH_CLOSED
                            val bottomHhPt = project3DPoint(part.x, part.y + 1.4f, part.z)
                            val gapWorldY = when {
                                isHhOpenTriggered -> -3.8f
                                isHhClosedTriggered -> -0.7f
                                else -> -2.1f
                            }
                            val wobbleY = osc * 1.1f
                            val topHhPt = project3DPoint(part.x, part.y + 1.4f + gapWorldY + wobbleY, part.z)

                            // Central Chrome Pull Rod extending above clutch
                            val rodTip = project3DPoint(part.x, part.y - 6.5f, part.z)
                            drawLine(
                                brush = chromeHardwareBrush,
                                start = bottomHhPt.offset,
                                end = rodTip.offset,
                                strokeWidth = (1.8f * topHhPt.scale).coerceAtLeast(1f)
                            )

                            // 1. Bottom Hi-Hat Cymbal
                            drawLathedCymbalDisc(
                                center = bottomHhPt.offset,
                                rx = baseRad,
                                ry = ovalHeight,
                                scaleFactor = bottomHhPt.scale,
                                highlighted = isHhClosedTriggered,
                                isBottomHat = true
                            )
                            // Dark air gap shadow between cymbals when open
                            if (!isHhClosedTriggered) {
                                drawOval(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    topLeft = Offset(bottomHhPt.x - baseRad * 0.92f, bottomHhPt.y - ovalHeight * 0.92f),
                                    size = Size(baseRad * 1.84f, ovalHeight * 1.84f)
                                )
                            }
                            // 2. Top Hi-Hat Cymbal
                            drawLathedCymbalDisc(
                                center = topHhPt.offset,
                                rx = baseRad,
                                ry = ovalHeight * (1f + osc * 0.08f).coerceIn(0.82f, 1.18f),
                                scaleFactor = topHhPt.scale,
                                highlighted = isHhOpenTriggered || isHhClosedTriggered,
                                isBottomHat = false
                            )
                        } else {
                            // Crash 1, Crash 2, & Ride Cymbals with 3D strike wobble
                            val wobbleRy = (ovalHeight * (1f + osc * 0.14f)).coerceAtLeast(baseRad * 0.18f)
                            val wobbleCenter = Offset(topCenter.x, topCenter.y + osc * 2.2f * topCenter.scale)
                            drawLathedCymbalDisc(
                                center = wobbleCenter,
                                rx = baseRad,
                                ry = wobbleRy,
                                scaleFactor = topCenter.scale,
                                highlighted = isActive
                            )
                        }
                    }
                }

                // --- D. SUBTLE ON-INSTRUMENT CALLOUT LABEL WHEN ACTIVE ---
                if (isActive) {
                    labelPaint.textSize = (8.5f * topCenter.scale).coerceIn(16f, 28f)
                    val activeText = if (part.label == "HH_CLOSED" && isHhOpenTriggered) "OPEN HAT" else part.displayName
                    drawContext.canvas.nativeCanvas.drawText(
                        activeText,
                        topCenter.x,
                        topCenter.y - ovalHeight - 6f * topCenter.scale,
                        labelPaint
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 6. COMPACT STUDIO HUD OVERLAY (CAMERA READOUT, ACTIVE HIT & RESET)
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Badge: Active Drum Hit Readout or Drag Hint
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.52f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (anyPadTriggered) Color(0xFF4ADE80) else primaryColor)
                    )
                    Text(
                        text = lastHitDisplay ?: "3D STUDIO KIT • DRAG TO ORBIT",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Controls: Zoom & Camera Reset
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.52f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.clickable {
                        zoom = when {
                            zoom < 0.95f -> 1.0f
                            zoom < 1.12f -> 1.18f
                            else -> 0.86f
                        }
                    }
                ) {
                    Text(
                        text = "${(zoom * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (abs(yaw - 16f) > 1f || abs(pitch - 32f) > 1f || abs(zoom - 1f) > 0.05f) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryColor.copy(alpha = 0.28f),
                        border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.65f)),
                        modifier = Modifier.clickable {
                            yaw = 16f
                            pitch = 32f
                            zoom = 1f
                        }
                    ) {
                        Text(
                            text = "RESET VIEW",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}