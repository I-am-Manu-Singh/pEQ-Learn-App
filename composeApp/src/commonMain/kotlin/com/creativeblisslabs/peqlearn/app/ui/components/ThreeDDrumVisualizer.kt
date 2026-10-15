package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
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
    val type: Int, // 0 = Standard Drum (Snare/Tom), 1 = Cymbal, 2 = Horizontal Bass Drum
    val baseColor: Color,
    val standFloorHeight: Float
)

@Composable
fun ThreeDDrumVisualizer(
    viewModel: AppViewModel,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    currentKitLabels: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var yaw by remember { mutableStateOf(18f) }
    var pitch by remember { mutableStateOf(34f) }
    val scope = rememberCoroutineScope()
    var canvasWidth by remember { mutableStateOf(400f) }
    var canvasHeight by remember { mutableStateOf(200f) }

    // Premium High-Fidelity Studio Shell Finishes & Textures
    val mahoganyWoodLight = Color(0xFF6E3622)
    val mahoganyWoodDark = Color(0xFF2E130B)
    val steelMetalLight = Color(0xFFBDC3C7)
    val steelMetalDark = Color(0xFF4A5568)
    val drumHeadCoated = Color(0xFFFAF9F6)
    val drumHeadSubtleRing = Color(0xFFE2E0D8)
    val chromeHardwareBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFD5D8DC), Color(0xFF7F8C8D), Color(0xFF34495E))
    )
    val blackHardwareBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF4A4A4A), Color(0xFF1F1F1F), Color(0xFF0F0F0F))
    )

    // Ultra-Premium Lathed B20 Bronze Alloy Fills
    val cymbalGoldCore = Color(0xFFF9E79F)
    val cymbalGoldMid = Color(0xFFF39C12)
    val cymbalGoldEdge = Color(0xFFD68910)
    val cymbalGoldDarkShadow = Color(0xFF7D6608)
    val cymbalSpecularHighlight = Color(0xFFFFFDF0)

    val drumParts = remember {
        listOf(
            // TIER 1: Center Double Bass Drums (Y=18, Floor=40)
            Drum3DPart("KICK", "BASS DRUM 1", -26f, 18f, -2f, 22f, 26f, 2, mahoganyWoodDark, 40f),
            Drum3DPart("KICK2", "BASS DRUM 2", 26f, 18f, -2f, 22f, 26f, 2, mahoganyWoodDark, 40f),
            // TIER 2: Snare, Floor Tom, & Aux Workspace
            Drum3DPart("SNARE", "MAIN SNARE", -56f, 7f, -14f, 14f, 9f, 0, steelMetalLight, 40f),
            Drum3DPart("SNARE2", "SIDE SNARE", -86f, 9f, 14f, 11.5f, 8f, 0, mahoganyWoodDark, 40f),
            Drum3DPart("FLOOR", "FLOOR TOM", 64f, 13f, -12f, 17f, 17f, 0, mahoganyWoodDark, 40f),
            // TIER 3: Upper Mounted Rack Toms (Mounted on Bass Drum / Hardware Rack)
            Drum3DPart("TOM1", "TOM 1", -26f, -10f, 16f, 10.5f, 10f, 0, mahoganyWoodDark, 18f),
            Drum3DPart("TOM2", "TOM 2", 0f, -11f, 22f, 11.5f, 10.5f, 0, mahoganyWoodDark, 18f),
            Drum3DPart("TOM", "TOM 3", 26f, -10f, 16f, 12.5f, 11f, 0, mahoganyWoodDark, 18f),
            // TIER 4: High Elevated Cymbals (HH is stacked: bottom at Y=-5, top moving)
            Drum3DPart("HH", "HI-HAT", -74f, -5f, -22f, 12.5f, 2f, 1, cymbalGoldMid, 40f),
            Drum3DPart("CRASH", "CRASH 1", -64f, -24f, 30f, 15f, 1.5f, 1, cymbalGoldMid, 40f),
            Drum3DPart("CRASH2", "CRASH 2", 8f, -26f, 34f, 14.5f, 1.5f, 1, cymbalGoldMid, 40f),
            Drum3DPart("RIDE", "RIDE CYMBAL", 74f, -18f, 28f, 19.5f, 1.5f, 1, cymbalGoldMid, 40f),
            // TIER 5: Auxiliary Nodes
            Drum3DPart("CLAP", "CLAP BLOCK", -42f, 4f, -34f, 6f, 4.5f, 0, Color(0xFFE67E22), 40f),
            Drum3DPart("PERC", "COWBELL", 32f, 0f, -32f, 5.5f, 7.5f, 0, Color(0xFF7F8C8D), 40f),
            Drum3DPart("SHAKER", "SHAKER CHNL", 78f, 5f, -30f, 4f, 11f, 0, Color(0xFF34495E), 40f)
        )
    }

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSystemInDarkTheme()) Color(0xFF090A0C) else Color(0xFFE5E7EB))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            yaw = (yaw + dragAmount.x * 0.4f).coerceIn(-90f, 90f)
                            pitch = (pitch - dragAmount.y * 0.4f).coerceIn(20f, 65f)
                        }
                    }
                    .pointerInput(Unit) {
                        fun project3DLocal(x: Float, y: Float, z: Float): Offset {
                            val radP = pitch * (PI.toFloat() / 180f)
                            val radY = yaw * (PI.toFloat() / 180f)
                            val x1 = x * cos(radY) - z * sin(radY)
                            val z1 = x * sin(radY) + z * cos(radY)
                            val factor = 280f / (280f + (y * sin(radP) + z1 * cos(radP)))
                            return Offset(
                                canvasWidth / 2f + x1 * factor * 2.9f,
                                canvasHeight / 2f + (y * cos(radP) - z1 * sin(radP)) * factor * 2.9f
                            )
                        }

                        detectTapGestures { tapOffset ->
                            var bestPart: Drum3DPart? = null
                            var bestDist = Float.MAX_VALUE
                            drumParts.forEach { part ->
                                val center = project3DLocal(part.x, part.y, part.z)
                                val dist = sqrt((tapOffset.x - center.x) * (tapOffset.x - center.x) + (tapOffset.y - center.y) * (tapOffset.y - center.y))
                                if (dist < part.radius * 3.8f && dist < bestDist) {
                                    bestPart = part
                                    bestDist = dist
                                }
                            }

                            bestPart?.let { clicked ->
                                val targetIndex = currentKitLabels.indexOfFirst {
                                    it.substringAfter("-").equals(clicked.label, ignoreCase = true)
                                }
                                val playIndex = if (targetIndex != -1) targetIndex else 0
                                val actualLabel = if (targetIndex != -1) currentKitLabels[targetIndex] else clicked.label

                                viewModel.audioEngine.playSample(label = actualLabel, index = playIndex, string = "DRUM")

                                scope.launch {
                                    viewModel.triggerThreeDDrumVisualizer(clicked.label)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    canvasWidth = w
                    canvasHeight = h

                    fun project3D(x: Float, y: Float, z: Float): Offset {
                        val radP = pitch * (PI.toFloat() / 180f)
                        val radY = yaw * (PI.toFloat() / 180f)
                        val x1 = x * cos(radY) - z * sin(radY)
                        val z1 = x * sin(radY) + z * cos(radY)
                        val y2 = y * cos(radP) - z1 * sin(radP)
                        val z2 = y * sin(radP) + z1 * cos(radP)
                        val factor = 280f / (280f + z2)
                        return Offset(w / 2f + x1 * factor * 2.9f, h / 2f + y2 * factor * 2.9f)
                    }

                    val radP = pitch * (PI.toFloat() / 180f)
                    val radY = yaw * (PI.toFloat() / 180f)
                    val perspectiveAspect = sin(radP)
                    val floorLevel = 40f

                    // ═══════════════════════════════════════════════════════════════
                    // 1. ACOUSTIC HARDWOOD STUDIO RISER / FLOOR BASE
                    // ═══════════════════════════════════════════════════════════════
                    val riserHalfW = 125f
                    val riserFrontZ = -55f
                    val riserBackZ = 65f
                    val riserTopY = floorLevel
                    val riserThickness = 3.5f

                    val pFL = project3D(-riserHalfW, riserTopY, riserFrontZ)
                    val pFR = project3D(riserHalfW, riserTopY, riserFrontZ)
                    val pBR = project3D(riserHalfW, riserTopY, riserBackZ)
                    val pBL = project3D(-riserHalfW, riserTopY, riserBackZ)

                    val pFL_b = project3D(-riserHalfW, riserTopY + riserThickness, riserFrontZ)
                    val pFR_b = project3D(riserHalfW, riserTopY + riserThickness, riserFrontZ)
                    val pBR_b = project3D(riserHalfW, riserTopY + riserThickness, riserBackZ)

                    // Front bevel edge of riser
                    val frontRiserPath = Path().apply {
                        moveTo(pFL.x, pFL.y)
                        lineTo(pFR.x, pFR.y)
                        lineTo(pFR_b.x, pFR_b.y)
                        lineTo(pFL_b.x, pFL_b.y)
                        close()
                    }
                    val frontStartY = minOf(pFL.y, pFL_b.y)
                    val frontEndY = maxOf(pFL.y, pFL_b.y) + 1f
                    drawPath(
                        frontRiserPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF1C130D), Color(0xFF0D0805)),
                            startY = frontStartY,
                            endY = frontEndY
                        )
                    )

                    // Right bevel edge of riser if visible
                    if (yaw < 0f) {
                        val sideRiserPath = Path().apply {
                            moveTo(pFR.x, pFR.y)
                            lineTo(pBR.x, pBR.y)
                            lineTo(pBR_b.x, pBR_b.y)
                            lineTo(pFR_b.x, pFR_b.y)
                            close()
                        }
                        drawPath(sideRiserPath, color = Color(0xFF140D09))
                    }

                    // Wood floor surface
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
                        floorPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF332017), Color(0xFF1F130E)),
                            startY = floorStartY,
                            endY = floorEndY
                        )
                    )

                    // Hardwood studio floor planks
                    val plankCount = 14
                    for (plank in 0..plankCount) {
                        val px = -riserHalfW + (2 * riserHalfW * (plank / plankCount.toFloat()))
                        val startPt = project3D(px, riserTopY, riserBackZ)
                        val endPt = project3D(px, riserTopY, riserFrontZ)
                        drawLine(
                            color = Color(0xFF0F0906).copy(alpha = 0.55f),
                            start = startPt,
                            end = endPt,
                            strokeWidth = 1.2f
                        )
                    }

                    // Stage lighting highlight on floor
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD700).copy(alpha = 0.08f), Color.Transparent),
                            center = project3D(0f, riserTopY, 0f),
                            radius = w * 0.45f
                        ),
                        topLeft = Offset(w * 0.15f, h * 0.35f),
                        size = Size(w * 0.7f, h * 0.55f)
                    )

                    // ═══════════════════════════════════════════════════════════════
                    // 2. FLOOR CONTACT SHADOWS FOR ALL STANDS & DRUMS
                    // ═══════════════════════════════════════════════════════════════
                    drumParts.forEach { part ->
                        val shadowCenter = project3D(part.x, floorLevel, part.z)
                        val sRad = part.radius * 2.6f
                        val sH = sRad * perspectiveAspect * 0.38f
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                                center = shadowCenter,
                                radius = sRad
                            ),
                            topLeft = Offset(shadowCenter.x - sRad, shadowCenter.y - sH),
                            size = Size(sRad * 2f, sH * 2f)
                        )
                    }

                    // ═══════════════════════════════════════════════════════════════
                    // 3. HARDWARE STANDS & MOUNTING SYSTEMS (CHROME TRIPODS & BOOMS)
                    // ═══════════════════════════════════════════════════════════════
                    drumParts.forEach { part ->
                        if (part.type != 2 && part.label != "TOM1" && part.label != "TOM2" && part.label != "TOM") {
                            val topMountOffset = project3D(part.x, part.y + part.height, part.z)
                            val floorCollarOffset = project3D(part.x, floorLevel - 3f, part.z)
                            val floorBaseOffset = project3D(part.x, floorLevel, part.z)

                            // Main vertical tube
                            drawLine(chromeHardwareBrush, topMountOffset, floorBaseOffset, strokeWidth = 3.5f)

                            // Chrome adjustment memory lock collar
                            val collarP = project3D(part.x, part.y + part.height + 6f, part.z)
                            drawCircle(Color(0xFF2C3E50), radius = 3.5f, center = collarP)
                            drawCircle(Color(0xFFECF0F1), radius = 2.5f, center = collarP)

                            // Double-braced tripod base legs
                            val legRadius = 14f
                            val legY = floorLevel
                            val legAngle1 = 0.0
                            val legAngle2 = (2.0 * PI) / 3.0
                            val legAngle3 = (4.0 * PI) / 3.0

                            val foot1 = project3D(part.x + (cos(legAngle1) * legRadius).toFloat(), legY, part.z + (sin(legAngle1) * legRadius).toFloat())
                            val foot2 = project3D(part.x + (cos(legAngle2) * legRadius).toFloat(), legY, part.z + (sin(legAngle2) * legRadius).toFloat())
                            val foot3 = project3D(part.x + (cos(legAngle3) * legRadius).toFloat(), legY, part.z + (sin(legAngle3) * legRadius).toFloat())

                            // Primary leg struts
                            drawLine(chromeHardwareBrush, floorCollarOffset, foot1, strokeWidth = 2.8f)
                            drawLine(chromeHardwareBrush, floorCollarOffset, foot2, strokeWidth = 2.8f)
                            drawLine(chromeHardwareBrush, floorCollarOffset, foot3, strokeWidth = 2.8f)

                            // Secondary cross braces for double-braced realism
                            val midCollarOffset = project3D(part.x, floorLevel - 1.5f, part.z)
                            val midFoot1 = Offset((floorCollarOffset.x + foot1.x) / 2f, (floorCollarOffset.y + foot1.y) / 2f)
                            val midFoot2 = Offset((floorCollarOffset.x + foot2.x) / 2f, (floorCollarOffset.y + foot2.y) / 2f)
                            val midFoot3 = Offset((floorCollarOffset.x + foot3.x) / 2f, (floorCollarOffset.y + foot3.y) / 2f)
                            drawLine(chromeHardwareBrush, midCollarOffset, midFoot1, strokeWidth = 1.6f)
                            drawLine(chromeHardwareBrush, midCollarOffset, midFoot2, strokeWidth = 1.6f)
                            drawLine(chromeHardwareBrush, midCollarOffset, midFoot3, strokeWidth = 1.6f)

                            // Rubber foot tips
                            drawCircle(Color(0xFF17202A), radius = 3.5f, center = foot1)
                            drawCircle(Color(0xFF17202A), radius = 3.5f, center = foot2)
                            drawCircle(Color(0xFF17202A), radius = 3.5f, center = foot3)
                        } else if (part.label == "TOM1" || part.label == "TOM2" || part.label == "TOM") {
                            // Bass Drum to Tom Mount Hardware Pipe
                            val tomBottom = project3D(part.x, part.y + part.height, part.z)
                            val mountAnchor = project3D(part.x * 0.7f, 15f, 0f)
                            drawLine(chromeHardwareBrush, tomBottom, mountAnchor, strokeWidth = 4f)
                            drawCircle(Color(0xFF34495E), radius = 4f, center = mountAnchor)
                            drawCircle(Color(0xFFECF0F1), radius = 2.5f, center = mountAnchor)
                        }
                    }

                    // ═══════════════════════════════════════════════════════════════
                    // 4. HI-HAT PEDAL & KICK DRUM PEDALS (MOVING KINEMATICS)
                    // ═══════════════════════════════════════════════════════════════
                    val isHhActive = (viewModel.sequencerTriggeredPads["HH"] == true ||
                                      viewModel.sequencerTriggeredPads["OHAT"] == true ||
                                      viewModel.sequencerTriggeredPads.any { it.value && it.key.uppercase().contains("HAT") })

                    val isKick1Active = (viewModel.sequencerTriggeredPads["KICK"] == true ||
                                        viewModel.sequencerTriggeredPads.any { it.value && it.key.uppercase() == "KICK" })

                    val isKick2Active = (viewModel.sequencerTriggeredPads["KICK2"] == true ||
                                        viewModel.sequencerTriggeredPads.any { it.value && it.key.uppercase() == "KICK2" })

                    // --- HI-HAT FOOT PEDAL ---
                    val hhPedalX = -74f
                    val hhPedalZ = -14f
                    val hhBaseFront = project3D(hhPedalX, floorLevel, hhPedalZ - 10f)
                    val hhBaseRear = project3D(hhPedalX, floorLevel, hhPedalZ)
                    val hhPedalTopY = if (isHhActive) floorLevel - 0.5f else floorLevel - 2.8f
                    val hhFootboardTip = project3D(hhPedalX, hhPedalTopY, hhPedalZ - 8.5f)

                    // Pedal Base plate
                    drawLine(blackHardwareBrush, hhBaseRear, hhBaseFront, strokeWidth = 7f)
                    // Pedal Hinged Footboard
                    drawLine(chromeHardwareBrush, hhBaseRear, hhFootboardTip, strokeWidth = 5.5f)
                    // Pull rod connecting footboard to hi-hat cymbal rod
                    val hhRodBottom = project3D(hhPedalX, floorLevel, hhPedalZ - 8.5f)
                    drawLine(chromeHardwareBrush, hhFootboardTip, hhRodBottom, strokeWidth = 2.2f)

                    // --- DUAL BASS DRUM PEDALS & STRIKING BEATERS ---
                    listOf(
                        Triple("KICK", -26f, isKick1Active),
                        Triple("KICK2", 26f, isKick2Active)
                    ).forEach { (_, kx, active) ->
                        val pedalBaseFront = project3D(kx, floorLevel, -14f)
                        val pedalBaseRear = project3D(kx, floorLevel, -2f)
                        val pedalBoardY = if (active) floorLevel - 0.6f else floorLevel - 3.2f
                        val pedalBoardTip = project3D(kx, pedalBoardY, -11f)

                        // Base plate & side pillars
                        drawLine(blackHardwareBrush, pedalBaseRear, pedalBaseFront, strokeWidth = 8f)
                        val camPillarLeft = project3D(kx - 3f, floorLevel - 6f, -13f)
                        val camPillarRight = project3D(kx + 3f, floorLevel - 6f, -13f)
                        val pillarBaseLeft = project3D(kx - 3f, floorLevel, -13f)
                        val pillarBaseRight = project3D(kx + 3f, floorLevel, -13f)
                        drawLine(chromeHardwareBrush, pillarBaseLeft, camPillarLeft, strokeWidth = 2.5f)
                        drawLine(chromeHardwareBrush, pillarBaseRight, camPillarRight, strokeWidth = 2.5f)

                        // Cam shaft across pillars
                        drawLine(chromeHardwareBrush, camPillarLeft, camPillarRight, strokeWidth = 3f)

                        // Moving Footboard
                        drawLine(chromeHardwareBrush, pedalBaseRear, pedalBoardTip, strokeWidth = 6f)

                        // Double chain linkage from footboard to cam
                        val camCenter = project3D(kx, floorLevel - 6f, -13f)
                        drawLine(blackHardwareBrush, pedalBoardTip, camCenter, strokeWidth = 2.5f)

                        // STRIKING BEATER SHAFT & FELT HEAD
                        // When active, beater strikes forward (+Z toward drum head at Z=-15)
                        val beaterShaftAngleZ = if (active) -14.5f else -8.5f
                        val beaterShaftAngleY = if (active) 18f else 14f
                        val beaterTip = project3D(kx, beaterShaftAngleY, beaterShaftAngleZ)

                        drawLine(chromeHardwareBrush, camCenter, beaterTip, strokeWidth = 2.8f)
                        // Felt / Rubber beater head
                        drawCircle(if (active) Color(0xFFE74C3C) else Color(0xFFECF0F1), radius = 4f, center = beaterTip)
                        drawCircle(Color(0xFF2C3E50), radius = 2f, center = beaterTip)
                    }

                    // ═══════════════════════════════════════════════════════════════
                    // 5. DEPTH-SORTED COMPONENT RENDERING ENGINE
                    // ═══════════════════════════════════════════════════════════════
                    val sortedParts = drumParts.sortedByDescending { part ->
                        part.x * sin(radY) + part.z * cos(radY)
                    }

                    sortedParts.forEach { part ->
                        val isDirectActive = viewModel.sequencerTriggeredPads[part.label] == true
                        val isGeneralActive = viewModel.sequencerTriggeredPads.any { (k, v) ->
                            if (!v) return@any false
                            val cleanKey = k.uppercase()
                            val cleanPart = part.label.uppercase()

                            when {
                                cleanKey.contains(cleanPart) -> true
                                cleanPart == "HH" && (cleanKey.contains("HIHAT") || cleanKey.contains("HAT")) -> true
                                cleanPart == "CRASH" && cleanKey.contains("CRASH") -> true
                                cleanPart == "RIDE" && cleanKey.contains("RIDE") -> true
                                cleanPart == "FLOOR" && (cleanKey.contains("LO") || cleanKey.contains("FLOOR")) -> true
                                cleanPart.startsWith("TOM") && cleanKey.contains("TOM") -> true
                                cleanPart.contains("KICK") && cleanKey.contains("KICK") -> true
                                else -> false
                            }
                        }
                        val isActive = isDirectActive || isGeneralActive

                        val velocityScale = when {
                            !isActive -> 1f
                            part.type == 2 -> 1.09f
                            part.type == 1 -> 1.04f
                            else -> 1.06f
                        }
                        val baseRad = part.radius * velocityScale * 2.9f
                        val ovalHeight = baseRad * perspectiveAspect
                        val topCenter = project3D(part.x, part.y, part.z)
                        val bottomCenter = project3D(part.x, part.y + part.height, part.z)

                        val strikeRipple = viewModel.drumKitRippleProgress[part.label]
                        if (strikeRipple != null && strikeRipple > 0f) {
                            drawOval(
                                color = primaryColor.copy(alpha = (1f - strikeRipple) * 0.5f),
                                topLeft = Offset(topCenter.x - (baseRad * (1f + strikeRipple)), topCenter.y - (ovalHeight * (1f + strikeRipple))),
                                size = Size(baseRad * 2f * (1f + strikeRipple), ovalHeight * 2f * (1f + strikeRipple)),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        if (part.type == 2) {
                            // ─── HORIZONTAL BASS DRUMS (MAPLE WOOD GRAIN & DUAL METAL HOOPS) ───
                            val frontCenter = project3D(part.x, part.y, part.z - part.height / 2f)
                            drawOval(
                                color = Color.Black.copy(alpha = 0.28f),
                                topLeft = Offset(frontCenter.x - baseRad, frontCenter.y + baseRad * perspectiveAspect - 4f),
                                size = Size(baseRad * 2f, baseRad * 0.45f * perspectiveAspect)
                            )
                            val shellSteps = 28
                            val frontRimVertices = ArrayList<Offset>(shellSteps)
                            for (i in 0 until shellSteps) {
                                val ratio = i / shellSteps.toFloat()
                                val nextRatio = (i + 1) / shellSteps.toFloat()
                                val slice = ratio * 2 * PI.toFloat()
                                val sliceNext = nextRatio * 2 * PI.toFloat()
                                val tLeft = project3D(part.x + sin(slice) * part.radius, part.y + cos(slice) * part.radius, part.z - part.height / 2f)
                                val tRight = project3D(part.x + sin(sliceNext) * part.radius, part.y + cos(sliceNext) * part.radius, part.z - part.height / 2f)
                                val bLeft = project3D(part.x + sin(slice) * part.radius, part.y + cos(slice) * part.radius, part.z + part.height / 2f)
                                val bRight = project3D(part.x + sin(sliceNext) * part.radius, part.y + cos(sliceNext) * part.radius, part.z + part.height / 2f)
                                frontRimVertices.add(tLeft)
                                val barrelPath = Path().apply {
                                    moveTo(tLeft.x, tLeft.y)
                                    lineTo(tRight.x, tRight.y)
                                    lineTo(bRight.x, bRight.y)
                                    lineTo(bLeft.x, bLeft.y)
                                    close()
                                }
                                val slatColor = if (i % 2 == 0) mahoganyWoodDark else mahoganyWoodLight
                                drawPath(barrelPath, brush = Brush.linearGradient(colors = listOf(slatColor, Color(0xFF150805))))
                            }

                            // Bass Drum Front Resonant Head
                            val headPath = Path().apply {
                                frontRimVertices.forEachIndexed { idx, vertex ->
                                    if (idx == 0) moveTo(vertex.x, vertex.y) else lineTo(vertex.x, vertex.y)
                                }
                                close()
                            }
                            drawPath(headPath, color = if (isActive) Color.White else drumHeadCoated)

                            // Coated outer perimeter ring
                            drawPath(headPath, color = drumHeadSubtleRing, style = Stroke(width = 6f))
                            // Wooden drum counter-hoop
                            drawPath(headPath, brush = Brush.linearGradient(listOf(mahoganyWoodLight, mahoganyWoodDark)), style = Stroke(width = 5.5f))
                            // Chrome tension rods & die-cast claws
                            drawPath(headPath, brush = chromeHardwareBrush, style = Stroke(width = 2.5f))

                            // Bass Drum Sound Hole / Mic Port (Off-center)
                            val portCenter = Offset(frontCenter.x + baseRad * 0.4f, frontCenter.y + ovalHeight * 0.25f)
                            drawCircle(Color(0xFF1A1A1A), radius = baseRad * 0.24f, center = portCenter)
                            drawCircle(chromeHardwareBrush, radius = baseRad * 0.24f, center = portCenter, style = Stroke(1.8f))

                            // Heavy-Duty Chrome Telescopic Bass Drum Spurs
                            val spurLeftMount = project3D(part.x - part.radius * 0.9f, part.y + part.radius * 0.6f, part.z - part.height / 4f)
                            val spurRightMount = project3D(part.x + part.radius * 0.9f, part.y + part.radius * 0.6f, part.z - part.height / 4f)
                            val spurLeftFoot = project3D(part.x - part.radius * 1.55f, floorLevel, part.z - part.height / 2f)
                            val spurRightFoot = project3D(part.x + part.radius * 1.55f, floorLevel, part.z - part.height / 2f)

                            drawLine(chromeHardwareBrush, spurLeftMount, spurLeftFoot, strokeWidth = 4f)
                            drawLine(chromeHardwareBrush, spurRightMount, spurRightFoot, strokeWidth = 4f)
                            drawCircle(Color(0xFF17202A), radius = 4f, center = spurLeftFoot)
                            drawCircle(Color(0xFF17202A), radius = 4f, center = spurRightFoot)

                        } else if (part.type == 0) {
                            // ─── CYLINDRICAL DRUM SHELLS (SNARES / TOMS WITH LUGS & DIE-CAST HOOPS) ───
                            val shellSteps = 20
                            drawOval(
                                color = Color.Black.copy(alpha = 0.28f),
                                topLeft = Offset(bottomCenter.x - baseRad, bottomCenter.y - ovalHeight),
                                size = Size(baseRad * 2f, ovalHeight * 2f)
                            )
                            for (i in 0 until shellSteps) {
                                val ratio = i / shellSteps.toFloat()
                                val nextRatio = (i + 1) / shellSteps.toFloat()
                                val angleCurrent = ratio * PI.toFloat() - (PI.toFloat() / 2f)
                                val angleNext = nextRatio * PI.toFloat() - (PI.toFloat() / 2f)
                                val tLeft = Offset(topCenter.x + sin(angleCurrent) * baseRad, topCenter.y + cos(angleCurrent) * ovalHeight)
                                val tRight = Offset(topCenter.x + sin(angleNext) * baseRad, topCenter.y + cos(angleNext) * ovalHeight)
                                val bLeft = Offset(bottomCenter.x + sin(angleCurrent) * baseRad, bottomCenter.y + cos(angleCurrent) * ovalHeight)
                                val bRight = Offset(bottomCenter.x + sin(angleNext) * baseRad, bottomCenter.y + cos(angleNext) * ovalHeight)
                                val path = Path().apply {
                                    moveTo(tLeft.x, tLeft.y)
                                    lineTo(tRight.x, tRight.y)
                                    lineTo(bRight.x, bRight.y)
                                    lineTo(bLeft.x, bLeft.y)
                                    close()
                                }
                                val slatColor = if (part.label.contains("SNARE")) {
                                    if (i % 2 == 0) steelMetalLight else steelMetalDark
                                } else {
                                    if (i % 2 == 0) mahoganyWoodDark else mahoganyWoodLight
                                }
                                drawPath(path, brush = Brush.linearGradient(colors = listOf(slatColor, Color.Black.copy(0.7f))))

                                // Chrome tension lugs along shell
                                if (i % 3 == 0) {
                                    val midLugTop = Offset((tLeft.x + tRight.x) / 2f, (tLeft.y + tRight.y) / 2f + 2f)
                                    val midLugBottom = Offset((bLeft.x + bRight.x) / 2f, (bLeft.y + bRight.y) / 2f - 2f)
                                    drawLine(chromeHardwareBrush, midLugTop, midLugBottom, strokeWidth = 2.4f)
                                }
                            }

                            // Bottom counter hoop
                            drawOval(chromeHardwareBrush, Offset(bottomCenter.x - baseRad, bottomCenter.y - ovalHeight), Size(baseRad * 2f, ovalHeight * 2f), style = Stroke(2.2f))

                            // Top Die-Cast Chrome Hoop
                            drawOval(chromeHardwareBrush, Offset(topCenter.x - baseRad, topCenter.y - ovalHeight), Size(baseRad * 2f, ovalHeight * 2f), style = Stroke(3.5f))

                            // Batter Drumhead Surface
                            drawOval(if (isActive) Color.White else drumHeadCoated, Offset(topCenter.x - (baseRad * 0.95f), topCenter.y - (ovalHeight * 0.95f)), Size(baseRad * 1.9f, ovalHeight * 1.9f))
                            // Subtle perimeter acoustic overtone ring
                            drawOval(drumHeadSubtleRing, Offset(topCenter.x - (baseRad * 0.88f), topCenter.y - (ovalHeight * 0.88f)), Size(baseRad * 1.76f, ovalHeight * 1.76f), style = Stroke(1.5f))

                            // Snare center dot or Tom badge
                            drawCircle(Color(0xFFE2E0D8).copy(alpha = 0.5f), radius = baseRad * 0.22f, center = topCenter)

                        } else {
                            // ─── HIGH-END LATHED CYMBALS (WITH DUAL STACKED HI-HAT DYNAMICS) ───
                            val isHiHat = part.label == "HH"

                            if (isHiHat) {
                                // ═══════════════════════════════════════════════════════════
                                // DUAL STACKED HI-HAT CYMBALS (TOP & BOTTOM PAIR)
                                // ═══════════════════════════════════════════════════════════
                                // Bottom Cymbal (Fixed at base mount)
                                val bottomY = part.y
                                val bottomCenterHh = project3D(part.x, bottomY, part.z)
                                val bottomOvalH = baseRad * perspectiveAspect

                                val bottomCymbalBrush = Brush.radialGradient(
                                    colors = listOf(cymbalGoldDarkShadow, cymbalGoldEdge, Color(0xFF4E3606)),
                                    center = bottomCenterHh,
                                    radius = baseRad * 1.1f
                                )
                                drawOval(
                                    brush = bottomCymbalBrush,
                                    topLeft = Offset(bottomCenterHh.x - baseRad, bottomCenterHh.y - bottomOvalH),
                                    size = Size(baseRad * 2f, bottomOvalH * 2f)
                                )

                                // Top Cymbal (Moves up when Open / down tightly clamped when closed/struck)
                                val topGapY = if (isHhActive) -0.5f else -2.6f
                                val topY = bottomY + topGapY
                                val topCenterHh = project3D(part.x, topY, part.z)
                                val topOvalH = baseRad * perspectiveAspect

                                val topHhBrush = Brush.radialGradient(
                                    colors = listOf(
                                        if (isActive) Color.White else cymbalSpecularHighlight,
                                        cymbalGoldCore,
                                        cymbalGoldMid,
                                        cymbalGoldDarkShadow
                                    ),
                                    center = topCenterHh,
                                    radius = baseRad * 1.15f
                                )
                                drawOval(
                                    brush = topHhBrush,
                                    topLeft = Offset(topCenterHh.x - baseRad, topCenterHh.y - topOvalH),
                                    size = Size(baseRad * 2f, topOvalH * 2f)
                                )

                                // Lathing tonal grooves on top hat
                                for (r in 1..5) {
                                    drawOval(
                                        color = Color.Black.copy(0.09f),
                                        topLeft = Offset(topCenterHh.x - (baseRad * 0.18f * r), topCenterHh.y - (topOvalH * 0.18f * r)),
                                        size = Size(baseRad * 0.36f * r, topOvalH * 0.36f * r),
                                        style = Stroke(0.6f)
                                    )
                                }

                                // Hi-Hat Center Bell & Clutch Assembly
                                drawOval(
                                    brush = Brush.linearGradient(colors = listOf(cymbalGoldEdge, cymbalGoldDarkShadow)),
                                    topLeft = Offset(topCenterHh.x - (baseRad * 0.22f), topCenterHh.y - (topOvalH * 0.22f)),
                                    size = Size(baseRad * 0.44f, topOvalH * 0.44f)
                                )
                                // Chrome Hi-Hat Clutch nut & felt
                                drawCircle(Color(0xFF34495E), radius = 3.5f, center = topCenterHh)
                                drawCircle(chromeHardwareBrush, radius = 2f, center = topCenterHh)

                            } else {
                                // ─── CRASH & RIDE CYMBALS (B20 BRONZE WITH BELL & HAMMER MARKS) ───
                                val premiumCymbalBrush = Brush.radialGradient(
                                    colors = listOf(
                                        if (isActive) Color.White else cymbalSpecularHighlight,
                                        cymbalGoldCore,
                                        cymbalGoldMid,
                                        cymbalGoldEdge,
                                        cymbalGoldDarkShadow
                                    ),
                                    center = topCenter,
                                    radius = baseRad * 1.15f
                                )
                                drawOval(
                                    brush = premiumCymbalBrush,
                                    topLeft = Offset(topCenter.x - baseRad, topCenter.y - ovalHeight),
                                    size = Size(baseRad * 2f, ovalHeight * 2f)
                                )

                                // Realistic concentric lathing grooves
                                for (r in 1..6) {
                                    drawOval(
                                        color = Color.Black.copy(0.08f),
                                        topLeft = Offset(topCenter.x - (baseRad * 0.16f * r), topCenter.y - (ovalHeight * 0.16f * r)),
                                        size = Size(baseRad * 0.32f * r, ovalHeight * 0.32f * r),
                                        style = Stroke(0.6f)
                                    )
                                }

                                // Hand-hammered texture dimples
                                for (step in 0..11) {
                                    val angle = (step * 2 * PI / 12).toFloat()
                                    val hx = topCenter.x + cos(angle) * (baseRad * 0.65f)
                                    val hy = topCenter.y + sin(angle) * (ovalHeight * 0.65f)
                                    drawCircle(Color.Black.copy(alpha = 0.07f), radius = 2.2f, center = Offset(hx, hy))
                                }

                                // Raised Cymbal Bell (Cup)
                                drawOval(
                                    brush = Brush.linearGradient(colors = listOf(cymbalGoldEdge, cymbalGoldDarkShadow)),
                                    topLeft = Offset(topCenter.x - (baseRad * 0.24f), topCenter.y - (ovalHeight * 0.24f)),
                                    size = Size(baseRad * 0.48f, ovalHeight * 0.48f)
                                )
                                // Specular peak on bell
                                drawOval(
                                    color = cymbalSpecularHighlight.copy(alpha = 0.7f),
                                    topLeft = Offset(topCenter.x - (baseRad * 0.10f), topCenter.y - (ovalHeight * 0.10f)),
                                    size = Size(baseRad * 0.20f, ovalHeight * 0.20f)
                                )
                                // Chrome cymbal felt & wingnut
                                drawCircle(Color(0xFF2C3E50), radius = 3.5f, center = topCenter)
                                drawCircle(chromeHardwareBrush, radius = 2f, center = topCenter)
                            }
                        }
                    }
                }
            
        }
    }
}
