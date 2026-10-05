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

    // Premium High-Fidelity Studio Shell Finishes
    val mahoganyShellStart = Color(0xFF3A1A10)
    val mahoganyShellEnd = Color(0xFF1A0A08)
    val steelMetalStart = Color(0xFF7F8C8D)
    val drumHeadCoated = Color(0xFFFDFEFE)
    val chromeHardwareBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFE5E8E8), Color(0xFF95A5A6), Color(0xFF5D6D7E))
    )

    // Ultra-Premium Lathed B20 Bronze Alloy Fills
    val cymbalGoldCore = Color(0xFFF5B041)
    val cymbalGoldEdge = Color(0xFFF39C12)
    val cymbalGoldDarkShadow = Color(0xFF9A7D0A)
    val cymbalSpecularHighlight = Color(0xFFFDEBD0)

    val drumParts = remember {
        listOf(
            // TIER 1: Center Bass Drums
            Drum3DPart("KICK", "BASS DRUM 1", -26f, 18f, -2f, 22f, 26f, 2, mahoganyShellStart, 42f),
            Drum3DPart("KICK2", "BASS DRUM 2", 26f, 18f, -2f, 22f, 26f, 2, mahoganyShellStart, 42f),
            // TIER 2: Snare, Floor Tom, & Aux Workspace
            Drum3DPart("SNARE", "MAIN SNARE", -56f, 5f, -14f, 14f, 8.5f, 0, steelMetalStart, 38f),
            Drum3DPart("SNARE2", "SIDE SNARE", -86f, 7f, 14f, 11.5f, 8f, 0, mahoganyShellStart, 38f),
            Drum3DPart("FLOOR", "FLOOR TOM", 64f, 11f, -12f, 16.5f, 17f, 0, mahoganyShellStart, 40f),
            // TIER 3: Upper Mounted Rack Toms
            Drum3DPart("TOM1", "TOM 1", -26f, -11f, 16f, 10.5f, 10f, 0, mahoganyShellStart, 18f),
            Drum3DPart("TOM2", "TOM 2", 0f, -12f, 22f, 11.5f, 10.5f, 0, mahoganyShellStart, 18f),
            Drum3DPart("TOM", "TOM 3", 26f, -11f, 16f, 12.5f, 11f, 0, mahoganyShellStart, 18f),
            // TIER 4: High Elevated Outward-Reaching Cymbals
            Drum3DPart("HH", "CLOSED HATS", -72f, -5f, -32f, 11.5f, 2f, 1, cymbalGoldCore, 44f),
            Drum3DPart("OHAT", "OPEN HATS", -80f, -9f, -18f, 12f, 2f, 1, cymbalGoldCore, 44f),
            Drum3DPart("CRASH", "CRASH 1", -62f, -26f, 32f, 15f, 1.5f, 1, cymbalGoldCore, 46f),
            Drum3DPart("CRASH2", "CRASH 2", 8f, -28f, 36f, 14f, 1.5f, 1, cymbalGoldCore, 46f),
            Drum3DPart("RIDE", "RIDE CYMBAL", 72f, -20f, 30f, 19f, 1.5f, 1, cymbalGoldCore, 44f),
            // TIER 5: Deep Background Ambient Auxiliary Nodes
            Drum3DPart("CLAP", "CLAP BLOCK", -42f, 2f, -38f, 6f, 4f, 0, Color(0xFFE67E22), 36f),
            Drum3DPart("PERC", "COWBELL", 32f, -2f, -36f, 5.5f, 7.5f, 0, Color(0xFF7F8C8D), 34f),
            Drum3DPart("SHAKER", "SHAKER CHNL", 78f, 3f, -32f, 4f, 11f, 0, Color(0xFF34495E), 38f)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(380.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "3D STUDIO ARENA DRUM WORKSTATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        "Widescreen Space Balancing • Premium Lathed B20 Metal Alloys",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Yaw: ${yaw.toInt()}° • Pitch: ${pitch.toInt()}°",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSystemInDarkTheme()) Color(0xFF141413) else Color(0xFFECEAE2))
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
                            val factor = 280f / (270f + (y * sin(radP) + z1 * cos(radP)))
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

                    // --- 1. DOUBLE-BRACED CONICAL CHROME HARDWARE STANDS ---
                    drumParts.forEach { part ->
                        if (part.type != 2 && part.label != "TOM1" && part.label != "TOM2" && part.label != "TOM") {
                            val topMountOffset = project3D(part.x, part.y + part.height, part.z)
                            val floorBaseOffset = project3D(part.x, part.standFloorHeight, part.z)
                            drawLine(chromeHardwareBrush, topMountOffset, floorBaseOffset, strokeWidth = 3f)
                            val tightConicalSpread = 4.5f
                            val shortLegBaseY = part.standFloorHeight + 1.2f
                            val legLeft = project3D(part.x - tightConicalSpread, shortLegBaseY, part.z + tightConicalSpread / 2f)
                            val legRight = project3D(part.x + tightConicalSpread, shortLegBaseY, part.z + tightConicalSpread / 2f)
                            val legBack = project3D(part.x, shortLegBaseY - 0.5f, part.z - tightConicalSpread)
                            drawLine(chromeHardwareBrush, floorBaseOffset, legLeft, strokeWidth = 2.5f)
                            drawLine(chromeHardwareBrush, floorBaseOffset, legRight, strokeWidth = 2.5f)
                            drawLine(chromeHardwareBrush, floorBaseOffset, legBack, strokeWidth = 2.5f)
                        }
                    }

                    // --- 2. DEPTH SORTED COMPONENTS ENGINE ---
                    val sortedParts = drumParts.sortedByDescending { part ->
                        part.x * sin(radY) + part.z * cos(radY)
                    }

                    sortedParts.forEach { part ->
                        val isDirectActive = viewModel.sequencerTriggeredPads[part.label] == true
                        val isGeneralActive = viewModel.sequencerTriggeredPads.any { (k, v) ->
                            v && (k.contains(part.label, ignoreCase = true) ||
                                (part.label == "HH" && k.contains("HIHAT", ignoreCase = true)) ||
                                (part.label == "FLOOR" && k.contains("TOM_LO", ignoreCase = true)) ||
                                (part.label == "TOM1" && k.contains("TOM_HI", ignoreCase = true)) ||
                                (part.label == "TOM2" && k.contains("TOM_MID", ignoreCase = true)))
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
                            // ─── HORIZONTAL BASS DRUMS ───
                            val frontCenter = project3D(part.x, part.y, part.z - part.height / 2f)
                            drawOval(
                                color = Color.Black.copy(alpha = 0.22f),
                                topLeft = Offset(frontCenter.x - baseRad, frontCenter.y + baseRad * perspectiveAspect - 4f),
                                size = Size(baseRad * 2f, baseRad * 0.42f * perspectiveAspect)
                            )
                            val shellSteps = 24
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
                                drawPath(barrelPath, brush = Brush.linearGradient(colors = listOf(part.baseColor, Color.Black.copy(0.7f))))
                            }
                            val headPath = Path().apply {
                                frontRimVertices.forEachIndexed { idx, vertex ->
                                    if (idx == 0) moveTo(vertex.x, vertex.y) else lineTo(vertex.x, vertex.y)
                                }
                                close()
                            }
                            drawPath(headPath, color = if (isActive) Color.White else drumHeadCoated)
                            drawPath(headPath, brush = chromeHardwareBrush, style = Stroke(width = 3.5f))
                        } else if (part.type == 0) {
                            // ─── CYLINDRICAL DRUM SHELLS (TOMS/SNARES) ───
                            val shellSteps = 16
                            drawOval(
                                color = Color.Black.copy(alpha = 0.24f),
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
                                val litShellBrush = Brush.linearGradient(colors = listOf(part.baseColor, Color.Black.copy(0.75f)))
                                drawPath(path, brush = litShellBrush)
                            }
                            drawOval(chromeHardwareBrush, Offset(topCenter.x - baseRad, topCenter.y - ovalHeight), Size(baseRad * 2f, ovalHeight * 2f), style = Stroke(2.5f))
                            drawOval(if (isActive) Color.White else drumHeadCoated, Offset(topCenter.x - (baseRad * 0.95f), topCenter.y - (ovalHeight * 0.95f)), Size(baseRad * 1.9f, ovalHeight * 1.9f))
                        } else {
                            // ─── HIGH-END LATHED CYMBALS ───
                            val premiumCymbalBrush = Brush.radialGradient(
                                colors = listOf(
                                    if (isActive) Color.White else cymbalSpecularHighlight,
                                    cymbalGoldCore,
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
                            for (r in 1..5) {
                                drawOval(
                                    color = Color.Black.copy(0.08f),
                                    topLeft = Offset(topCenter.x - (baseRad * 0.18f * r), topCenter.y - (ovalHeight * 0.18f * r)),
                                    size = Size(baseRad * 0.36f * r, ovalHeight * 0.36f * r),
                                    style = Stroke(0.6f)
                                )
                            }
                            drawOval(
                                brush = Brush.linearGradient(colors = listOf(cymbalGoldEdge, cymbalGoldDarkShadow)),
                                topLeft = Offset(topCenter.x - (baseRad * 0.22f), topCenter.y - (ovalHeight * 0.22f)),
                                size = Size(baseRad * 0.44f, ovalHeight * 0.44f)
                            )
                            drawOval(
                                color = cymbalSpecularHighlight.copy(alpha = 0.6f),
                                topLeft = Offset(topCenter.x - (baseRad * 0.08f), topCenter.y - (ovalHeight * 0.08f)),
                                size = Size(baseRad * 0.16f, ovalHeight * 0.16f)
                            )
                        }
                    }
                }
            }
        }
    }
}
