package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.peqlearn.app.util.BiomechanicalPerformersEngine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Photorealistic Anatomical Guitar Hands & Pick Overlay:
 * 1. Left Hand (Fretting Hand):
 *    - Curled organic fingers (1=Index, 2=Middle, 3=Ring, 4=Pinky, 5=Thumb over top for bass muting/wrapping)
 *    - Frets right behind the fret wire (biomechanically authentic fret touchpoint)
 *    - Natural skin gradients, knuckle creases, fingernails, and glowing finger badges
 *    - Thumb positioned behind or resting over the neck top edge
 * 2. Right Hand (Strumming/Picking Hand):
 *    - Tear-drop celluloid guitar pick with tortoiseshell/pearl gradient, bevelled edges & grip texture
 *    - Dynamic pick angle & pluck dip when striking strings (downstroke / upstroke pick deflection)
 *    - Natural resting curled fingers (middle, ring, pinky) floating above soundboard
 * 3. Physical String Oscillation:
 *    - Realistic physics-based string vibration with standing wave decay when plucked
 */
@Composable
fun GuitarHumanHandsOverlay(
    activeMidiNotes: Map<Int, Boolean>,
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    numStrings: Int = 6,
    numFrets: Int = 24,
    baseFret: Int = 1,
    openStringsMidi: List<Int> = listOf(64, 59, 55, 50, 45, 40), // e4, B3, G3, D3, A2, E2
    showFrettingHand: Boolean = true,
    showPickingHand: Boolean = true,
    handAccentColor: Color = Color(0xFF6C63FF),
    pickColor: Color = Color(0xFFFFB347),
    modifier: Modifier = Modifier
) {
    if (!showFrettingHand && !showPickingHand) return

    val soundingPitches = remember(activeMidiNotes) {
        activeMidiNotes.entries
            .filter { it.value && it.key in 24..100 }
            .map { it.key }
            .distinct()
            .sorted()
    }

    var lastBoxCenter by remember { mutableFloatStateOf(3.0f) }

    // Solve optimal Viterbi string, fret, and finger placement
    val viterbiSolution = remember(soundingPitches, openStringsMidi, numFrets, baseFret) {
        BiomechanicalPerformersEngine.solveFrettedSliceBiomechanical(
            pitches = soundingPitches,
            openStringsMidiHighToLow = openStringsMidi,
            maxFret = numFrets,
            minFret = 0,
            lastHandBoxCenterFret = lastBoxCenter
        )
    }

    val placementMap = viterbiSolution.first
    val currentBoxCenter = viterbiSolution.second

    LaunchedEffect(currentBoxCenter) {
        if (placementMap.values.any { it.fret > 0 }) {
            lastBoxCenter = currentBoxCenter
        }
    }

    // Smooth gliding hand box center animation
    val animatedBoxCenter = remember { Animatable(lastBoxCenter) }
    LaunchedEffect(currentBoxCenter) {
        animatedBoxCenter.animateTo(
            targetValue = currentBoxCenter,
            animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
        )
    }

    // Pick strike animation & string pluck impact
    val pickPluckAnim = remember { Animatable(0f) }
    var lastPluckedString by remember { mutableIntStateOf(-1) }

    LaunchedEffect(soundingPitches) {
        if (soundingPitches.isNotEmpty()) {
            val lowestStr = placementMap.values.map { it.stringIndex }.minOrNull() ?: 0
            lastPluckedString = lowestStr
            pickPluckAnim.snapTo(1f)
            pickPluckAnim.animateTo(0f, animationSpec = tween(120, easing = LinearEasing))
        }
    }

    // String vibration phase generator
    val infiniteTransition = rememberInfiniteTransition(label = "string_oscillation")
    val stringPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "string_phase"
    )

    val fingerBadgePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val totalW = size.width
        val totalH = size.height

        // Match UnifiedGuitarFretboard viewport geometry
        val s = (totalH / 165f).coerceIn(0.5f, 2.5f)
        val leftPad = (44f * s).coerceIn(28f, totalW * 0.16f)
        val rightPad = (16f * s).coerceIn(10f, totalW * 0.06f)
        val topPad = (12f * s).coerceAtLeast(6f)
        val bottomPad = (22f * s).coerceAtLeast(14f)

        val playableW = (totalW - leftPad - rightPad).coerceAtLeast(1f)
        val playableH = (totalH - topPad - bottomPad).coerceAtLeast(1f)

        val r = 0.056125687f
        var scaleSum = 0f
        for (f in 1..numFrets) {
            val absoluteFret = baseFret + f - 1
            scaleSum += (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
        }
        val safeScaleSum = scaleSum.coerceAtLeast(0.001f)

        fun getFretWireX(fret: Int): Float {
            if (fret <= 0) return leftPad
            var accumulated = 0f
            for (f in 1..fret.coerceAtMost(numFrets)) {
                val absoluteFret = baseFret + f - 1
                val lengthFraction = (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
                accumulated += (lengthFraction / safeScaleSum) * playableW
            }
            return leftPad + accumulated
        }

        fun getFretTouchpointX(fret: Int): Float {
            if (fret <= 0) return leftPad - 12f * s
            val fretWire = getFretWireX(fret)
            val prevFretWire = getFretWireX(fret - 1)
            val fretWidth = fretWire - prevFretWire
            // Biomechanically accurate: fingers press down ~80% towards the fret wire (not the middle!)
            return fretWire - fretWidth * 0.22f
        }

        val nutH = playableH * 0.88f
        val bridgeH = playableH * 0.98f
        val midY = topPad + playableH / 2f
        val nutTop = midY - nutH / 2f
        val nutBottom = midY + nutH / 2f
        val bridgeTop = midY - bridgeH / 2f
        val bridgeBottom = midY + bridgeH / 2f

        fun getStringYAtX(strIdx: Int, x: Float): Float {
            val t = ((x - leftPad) / playableW).coerceIn(0f, 1f)
            val curNutTop = midY - nutH / 2f
            val curNutBottom = midY + nutH / 2f
            val curTop = curNutTop + (bridgeTop - curNutTop) * t
            val curBottom = curNutBottom + (bridgeBottom - curNutBottom) * t
            val currentH = curBottom - curTop
            val curStringSpacing = currentH / (numStrings - 1).coerceAtLeast(1).toFloat()
            return if (numStrings <= 1) midY else curTop + strIdx * curStringSpacing
        }

        // =====================================================================
        // 1. PHYSICAL STRING OSCILLATION (VIBRATING STRINGS WHEN PLUCKED)
        // =====================================================================
        val activeStrings = placementMap.values.map { it.stringIndex }.toSet()
        for (strIdx in 0 until numStrings) {
            val isActive = strIdx in activeStrings
            if (isActive) {
                val stringPath = Path()
                val steps = 30
                val decayAmp = (3.5f * s) * (0.3f + 0.7f * pickPluckAnim.value)
                for (step in 0..steps) {
                    val t = step / steps.toFloat()
                    val x = leftPad + t * playableW
                    val baseY = getStringYAtX(strIdx, x)
                    // Standing wave fundamental: sin(pi * x / L) * sin(omega * t)
                    val standingWave = sin(t * PI.toFloat()) * sin(stringPhase + strIdx * 0.7f) * decayAmp
                    val y = baseY + standingWave
                    if (step == 0) stringPath.moveTo(x, y) else stringPath.lineTo(x, y)
                }

                // Vibrant neon oscillation blur
                drawPath(
                    path = stringPath,
                    color = handAccentColor.copy(alpha = 0.55f * (0.4f + 0.6f * pickPluckAnim.value)),
                    style = Stroke(width = (4f * s).coerceIn(2.5f, 7f), cap = StrokeCap.Round)
                )
                drawPath(
                    path = stringPath,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = (1.5f * s).coerceIn(1f, 3f), cap = StrokeCap.Round)
                )
            }
        }

        // =====================================================================
        // 2. LEFT HAND: FRETTING HAND (CURLED ORGANIC HUMAN FINGERS)
        // =====================================================================
        if (showFrettingHand) {
            val handBoxCenter = animatedBoxCenter.value
            val handPalmX = getFretTouchpointX(handBoxCenter.toInt()).coerceIn(leftPad, totalW - rightPad - 40f)
            val handPalmY = nutBottom + 26.dp.toPx() * s

            // Group fretted notes by finger (1=Index, 2=Middle, 3=Ring, 4=Pinky)
            val frettedAssignments = placementMap.values.filter { it.fret > 0 }

            val skinLight = Color(0xFFFFDFC4)
            val skinMid = Color(0xFFE5B590)
            val skinShadow = Color(0xFFA6714E)

            // Draw thumb wrapping over or anchoring behind the neck
            val thumbTipX = handPalmX - 22f * s
            val thumbTipY = nutTop - (8f * s)
            drawRoundRect(
                color = skinMid,
                topLeft = Offset(thumbTipX - 10f * s, thumbTipY - 6f * s),
                size = Size(20f * s, 26f * s),
                cornerRadius = CornerRadius(10f * s, 10f * s)
            )
            drawRoundRect(
                color = Color(0xFFFFF0E6).copy(alpha = 0.75f),
                topLeft = Offset(thumbTipX - 7f * s, thumbTipY - 4f * s),
                size = Size(14f * s, 10f * s),
                cornerRadius = CornerRadius(5f * s, 5f * s)
            )

            // Draw palm fleshy base
            val palmPath = Path().apply {
                moveTo(handPalmX - 35f * s, handPalmY + 30f * s)
                quadraticBezierTo(handPalmX - 42f * s, handPalmY - 5f * s, handPalmX - 24f * s, handPalmY - 14f * s)
                quadraticBezierTo(handPalmX, handPalmY - 18f * s, handPalmX + 32f * s, handPalmY - 10f * s)
                quadraticBezierTo(handPalmX + 44f * s, handPalmY + 18f * s, handPalmX + 30f * s, handPalmY + 36f * s)
                close()
            }
            drawPath(
                path = palmPath,
                brush = Brush.radialGradient(
                    colors = listOf(skinLight, skinMid, skinShadow),
                    center = Offset(handPalmX, handPalmY),
                    radius = 45f * s
                )
            )
            drawPath(
                path = palmPath,
                color = Color.Black.copy(alpha = 0.25f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Draw articulated curled fingers (Fingers 1..4)
            for (fNum in 1..4) {
                val placement = frettedAssignments.firstOrNull { it.finger == fNum }
                val isPressing = placement != null

                val targetX: Float
                val targetY: Float
                if (isPressing) {
                    targetX = getFretTouchpointX(placement!!.fret)
                    targetY = getStringYAtX(placement.stringIndex, targetX)
                } else {
                    // Resting finger naturally curled above the strings near the hand box
                    val fretOffset = when (fNum) {
                        1 -> -1.2f
                        2 -> -0.4f
                        3 -> 0.4f
                        else -> 1.2f
                    }
                    targetX = getFretTouchpointX((handBoxCenter + fretOffset).toInt().coerceIn(1, numFrets))
                    targetY = getStringYAtX(fNum.coerceIn(0, numStrings - 1), targetX) + (14f * s)
                }

                val fingerBaseX = handPalmX + (fNum - 2.5f) * 16f * s
                val fingerBaseY = handPalmY - 12f * s

                val knuckleX = fingerBaseX * 0.45f + targetX * 0.55f
                val knuckleY = fingerBaseY * 0.4f + targetY * 0.6f + (if (isPressing) 18f * s else 26f * s)

                val fingerHalfW = (7.5f * s).coerceIn(4f, 14f)

                // Draw finger tube segment
                val fingerPath = Path().apply {
                    moveTo(fingerBaseX - fingerHalfW, fingerBaseY)
                    quadraticBezierTo(knuckleX - fingerHalfW * 1.15f, knuckleY, targetX - fingerHalfW * 0.85f, targetY)
                    cubicTo(
                        targetX - fingerHalfW * 0.85f, targetY - fingerHalfW * 0.9f,
                        targetX + fingerHalfW * 0.85f, targetY - fingerHalfW * 0.9f,
                        targetX + fingerHalfW * 0.85f, targetY
                    )
                    quadraticBezierTo(knuckleX + fingerHalfW * 1.15f, knuckleY, fingerBaseX + fingerHalfW, fingerBaseY)
                    close()
                }

                // 3D Cylinder shading for human finger
                drawPath(
                    path = fingerPath,
                    brush = Brush.linearGradient(
                        colors = listOf(skinLight, skinMid, skinShadow),
                        start = Offset(fingerBaseX - fingerHalfW, fingerBaseY),
                        end = Offset(fingerBaseX + fingerHalfW, fingerBaseY)
                    )
                )
                drawPath(
                    path = fingerPath,
                    color = if (isPressing) handAccentColor.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.28f),
                    style = Stroke(width = if (isPressing) 1.8.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round)
                )

                // Fingernail on distal phalanx
                val nailW = fingerHalfW * 1.3f
                val nailH = fingerHalfW * 1.4f
                drawRoundRect(
                    color = if (isPressing) Color(0xFFFFF9C4) else Color(0xFFFFF0E6).copy(alpha = 0.82f),
                    topLeft = Offset(targetX - nailW * 0.5f, targetY - nailH * 0.6f),
                    size = Size(nailW, nailH),
                    cornerRadius = CornerRadius(nailW * 0.45f, nailW * 0.45f)
                )

                // Glowing circular finger badge with digit (1..4)
                val badgeY = targetY + fingerHalfW * 1.6f
                val badgeRad = (fingerHalfW * 1.1f).coerceAtLeast(8.dp.toPx())

                if (isPressing) {
                    drawCircle(
                        color = handAccentColor.copy(alpha = 0.45f),
                        radius = badgeRad * 1.45f,
                        center = Offset(targetX, badgeY)
                    )
                }

                drawCircle(
                    color = if (isPressing) handAccentColor else Color(0xFF161A22).copy(alpha = 0.88f),
                    radius = badgeRad,
                    center = Offset(targetX, badgeY)
                )
                drawCircle(
                    color = if (isPressing) Color.White else handAccentColor.copy(alpha = 0.65f),
                    radius = badgeRad,
                    center = Offset(targetX, badgeY),
                    style = Stroke(width = if (isPressing) 2.dp.toPx() else 1.dp.toPx())
                )

                fingerBadgePaint.textSize = (badgeRad * 1.15f).coerceIn(9.dp.toPx(), 14.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    fNum.toString(),
                    targetX,
                    badgeY + fingerBadgePaint.textSize * 0.36f,
                    fingerBadgePaint
                )
            }
        }

        // =====================================================================
        // 3. RIGHT HAND: PICKING / STRUMMING HAND WITH TEAR-DROP GUITAR PICK
        // =====================================================================
        if (showPickingHand && soundingPitches.isNotEmpty()) {
            val pluckedStr = if (lastPluckedString >= 0) lastPluckedString else 0
            val pickX = totalW - rightPad - (48f * s)
            val pluckDipY = sin(pickPluckAnim.value * PI.toFloat()) * (10f * s)
            val pickY = getStringYAtX(pluckedStr, pickX) + pluckDipY

            // Draw curled fingers of strumming hand resting above pick
            val rhPalmX = pickX + (28f * s)
            val rhPalmY = pickY - (24f * s)

            val rhSkinLight = Color(0xFFFFDFC4)
            val rhSkinMid = Color(0xFFE5B590)
            val rhSkinShadow = Color(0xFFA6714E)

            // Right hand wrist and curled knuckles
            val rhWristPath = Path().apply {
                moveTo(rhPalmX + 42f * s, rhPalmY + 36f * s)
                cubicTo(rhPalmX + 32f * s, rhPalmY, rhPalmX + 16f * s, rhPalmY - 14f * s, rhPalmX - 8f * s, rhPalmY - 8f * s)
                quadraticBezierTo(rhPalmX - 22f * s, rhPalmY + 12f * s, rhPalmX - 14f * s, rhPalmY + 28f * s)
                close()
            }
            drawPath(
                path = rhWristPath,
                brush = Brush.linearGradient(
                    colors = listOf(rhSkinLight, rhSkinMid, rhSkinShadow),
                    start = Offset(rhPalmX - 20f * s, rhPalmY),
                    end = Offset(rhPalmX + 30f * s, rhPalmY + 30f * s)
                )
            )

            // Thumb holding the pick from top
            drawRoundRect(
                color = rhSkinMid,
                topLeft = Offset(pickX - 4f * s, pickY - 26f * s),
                size = Size(24f * s, 18f * s),
                cornerRadius = CornerRadius(9f * s, 9f * s)
            )

            // Tear-drop Celluloid Guitar Pick
            val pickW = (24f * s).coerceIn(16f, 38f)
            val pickH = (28f * s).coerceIn(18f, 44f)
            val pickTipX = pickX - pickW * 0.42f
            val pickTipY = pickY

            val pickPath = Path().apply {
                moveTo(pickTipX, pickTipY) // Sharp pointed pick tip striking string
                cubicTo(
                    pickTipX + pickW * 0.25f, pickTipY - pickH * 0.48f,
                    pickTipX + pickW * 0.85f, pickTipY - pickH * 0.44f,
                    pickTipX + pickW, pickTipY - pickH * 0.15f
                )
                cubicTo(
                    pickTipX + pickW * 1.05f, pickTipY + pickH * 0.05f,
                    pickTipX + pickW * 0.85f, pickTipY + pickH * 0.44f,
                    pickTipX + pickW * 0.25f, pickTipY + pickH * 0.48f
                )
                close()
            }

            // Tortoiseshell / Pearloid 3D celluloid gradient
            drawPath(
                path = pickPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF3E0), pickColor, Color(0xFFE65100), Color(0xFF3E2723)),
                    center = Offset(pickTipX + pickW * 0.5f, pickTipY),
                    radius = pickW * 0.85f
                )
            )
            // Bevelled pick rim & edge gloss
            drawPath(
                path = pickPath,
                color = Color.White.copy(alpha = 0.82f),
                style = Stroke(width = (1.6f * s).coerceIn(1.2f, 3f))
            )
            // Grip logo mark on center of pick
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = pickW * 0.16f,
                center = Offset(pickTipX + pickW * 0.55f, pickTipY)
            )

            // Pluck spark/impact flash when striking string
            if (pickPluckAnim.value > 0.1f) {
                drawCircle(
                    color = Color(0xFFFFEB3B).copy(alpha = 0.75f * pickPluckAnim.value),
                    radius = (10f * s) * pickPluckAnim.value,
                    center = Offset(pickTipX, pickTipY)
                )
            }
        }
    }
}

