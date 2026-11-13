package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.peqlearn.app.util.BiomechanicalPerformersEngine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

private val GUITAR_NOTE_NAMES = arrayOf("C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")

private fun formatGuitarPitchLabel(midiNote: Int): String {
    val octave = (midiNote / 12) - 1
    return "${GUITAR_NOTE_NAMES[(midiNote % 12 + 12) % 12]}$octave"
}

private data class GuitarViewportFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

private fun computeGuitarOverlayViewport(
    canvasWidth: Float,
    canvasHeight: Float,
    minAspect: Float = 2.0f,
    maxAspect: Float = 11.5f,
    referenceHeight: Float = 165f,
    horizontalPaddingFrac: Float = 0.01f,
    verticalPaddingFrac: Float = 0.02f
): GuitarViewportFrame {
    val safeW = (canvasWidth * (1f - horizontalPaddingFrac * 2f)).coerceAtLeast(1f)
    val safeH = (canvasHeight * (1f - verticalPaddingFrac * 2f)).coerceAtLeast(1f)
    val currentAspect = safeW / safeH

    val fittedW: Float
    val fittedH: Float
    when {
        currentAspect > maxAspect -> {
            fittedH = safeH
            fittedW = (safeH * maxAspect).coerceAtMost(safeW)
        }
        currentAspect < minAspect -> {
            fittedW = safeW
            fittedH = (safeW / minAspect).coerceAtMost(safeH)
        }
        else -> {
            fittedW = safeW
            fittedH = safeH
        }
    }

    val left = (canvasWidth - fittedW) * 0.5f
    val top = (canvasHeight - fittedH) * 0.5f
    val scale = (fittedH / referenceHeight).coerceIn(0.42f, 2.5f)
    return GuitarViewportFrame(left, top, fittedW, fittedH, scale)
}

// Anatomical relative finger widths for Guitar Fretting Fingers 1..4 (Index, Middle, Ring, Pinky)
private val FRET_FINGER_WIDTH_MULT = floatArrayOf(0f, 0.96f, 1.02f, 0.95f, 0.82f)

/**
 * Ultrarealistic Anatomical Guitar Hands, Barre Articulation & Pick Overlay:
 * 1. Exact Aspect-Safe Viewport Synchronization with `UnifiedGuitarFretboard`.
 * 2. Left Hand (Fretting Hand):
 *    - 4-Channel independent spring kinematics for Index(1), Middle(2), Ring(3), and Pinky(4).
 *    - Automatic Index Finger Barre synthesis when Finger 1 frets multiple strings at the same fret.
 *    - 3-joint articulated phalanges (MCP -> PIP -> DIP -> Distal Pad), extensor tendons, PIP/DIP skin creases,
 *      translucent fingernails with lunula & free-edge highlights, and floating glassmorphic HUD telemetry.
 * 3. Right Hand (Picking / Strumming Hand):
 *    - Persistent anatomical right hand with thumb-index plectrum pinch, curled ulnar fingers (M, R, P),
 *      upstroke/downstroke wrist rotation, and bevelled tortoiseshell/pearloid celluloid pick.
 * 4. Fret-Terminated Standing Wave String Oscillation:
 *    - Strings vibrate realistically between the exact fretted touchpoint and the bridge saddle.
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

    val safeNumFrets = numFrets.coerceIn(4, 24)
    val safeBaseFret = baseFret.coerceIn(1, 24)

    val soundingPitches = remember(activeMidiNotes) {
        activeMidiNotes.entries
            .filter { it.value && it.key in 24..100 }
            .map { it.key }
            .distinct()
            .sorted()
    }

    var lastBoxCenter by remember { mutableFloatStateOf(3.0f) }

    // Solve optimal Viterbi string, fret, and finger placement
    val viterbiSolution = remember(soundingPitches, openStringsMidi, safeNumFrets, safeBaseFret) {
        BiomechanicalPerformersEngine.solveFrettedSliceBiomechanical(
            pitches = soundingPitches,
            openStringsMidiHighToLow = openStringsMidi,
            maxFret = safeNumFrets,
            minFret = 0,
            lastHandBoxCenterFret = lastBoxCenter
        )
    }

    val placementMap = viterbiSolution.first
    val currentBoxCenter = viterbiSolution.second.coerceIn(1.5f, (safeNumFrets - 1).toFloat().coerceAtLeast(2f))

    LaunchedEffect(currentBoxCenter, placementMap) {
        if (placementMap.values.any { it.fret > 0 }) {
            lastBoxCenter = currentBoxCenter
        }
    }

    // Smooth critically-damped hand box center glide
    val animatedBoxCenter by animateFloatAsState(
        targetValue = if (placementMap.values.any { it.fret > 0 }) currentBoxCenter else lastBoxCenter,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 320f),
        label = "guitar_lh_box_center"
    )

    // Group active fretted placements by finger (1 = Index, 2 = Middle, 3 = Ring, 4 = Pinky)
    val frettedByFinger = remember(placementMap) {
        placementMap.values.filter { it.fret > 0 }.groupBy { it.finger.coerceIn(1, 4) }
    }

    // Per-finger target fret and string index (for smooth 2D spring tracking)
    val f1TargetFret = frettedByFinger[1]?.firstOrNull()?.fret?.toFloat() ?: (animatedBoxCenter - 1.2f).coerceIn(1f, safeNumFrets.toFloat())
    val f2TargetFret = frettedByFinger[2]?.firstOrNull()?.fret?.toFloat() ?: (animatedBoxCenter - 0.4f).coerceIn(1f, safeNumFrets.toFloat())
    val f3TargetFret = frettedByFinger[3]?.firstOrNull()?.fret?.toFloat() ?: (animatedBoxCenter + 0.4f).coerceIn(1f, safeNumFrets.toFloat())
    val f4TargetFret = frettedByFinger[4]?.firstOrNull()?.fret?.toFloat() ?: (animatedBoxCenter + 1.2f).coerceIn(1f, safeNumFrets.toFloat())

    val f1TargetStr = frettedByFinger[1]?.map { it.stringIndex }?.average()?.toFloat() ?: 2.8f
    val f2TargetStr = frettedByFinger[2]?.map { it.stringIndex }?.average()?.toFloat() ?: 2.4f
    val f3TargetStr = frettedByFinger[3]?.map { it.stringIndex }?.average()?.toFloat() ?: 1.9f
    val f4TargetStr = frettedByFinger[4]?.map { it.stringIndex }?.average()?.toFloat() ?: 1.4f

    val animF1Fret by animateFloatAsState(f1TargetFret, spring(0.90f, 480f), label = "gF1Fret")
    val animF2Fret by animateFloatAsState(f2TargetFret, spring(0.90f, 480f), label = "gF2Fret")
    val animF3Fret by animateFloatAsState(f3TargetFret, spring(0.90f, 480f), label = "gF3Fret")
    val animF4Fret by animateFloatAsState(f4TargetFret, spring(0.90f, 480f), label = "gF4Fret")

    val animF1Str by animateFloatAsState(f1TargetStr, spring(0.90f, 460f), label = "gF1Str")
    val animF2Str by animateFloatAsState(f2TargetStr, spring(0.90f, 460f), label = "gF2Str")
    val animF3Str by animateFloatAsState(f3TargetStr, spring(0.90f, 460f), label = "gF3Str")
    val animF4Str by animateFloatAsState(f4TargetStr, spring(0.90f, 460f), label = "gF4Str")

    val animF1Press by animateFloatAsState(if (!frettedByFinger[1].isNullOrEmpty()) 1f else 0f, spring(0.85f, 650f), label = "gF1Press")
    val animF2Press by animateFloatAsState(if (!frettedByFinger[2].isNullOrEmpty()) 1f else 0f, spring(0.85f, 650f), label = "gF2Press")
    val animF3Press by animateFloatAsState(if (!frettedByFinger[3].isNullOrEmpty()) 1f else 0f, spring(0.85f, 650f), label = "gF3Press")
    val animF4Press by animateFloatAsState(if (!frettedByFinger[4].isNullOrEmpty()) 1f else 0f, spring(0.85f, 650f), label = "gF4Press")

    // Pick strike animation, upstroke/downstroke alternation & target string tracking
    val pickPluckAnim = remember { Animatable(0f) }
    var lastPluckedString by remember { mutableIntStateOf(2) }
    var pickStrokeDirection by remember { mutableFloatStateOf(1f) } // +1f = Downstroke, -1f = Upstroke

    LaunchedEffect(soundingPitches) {
        if (soundingPitches.isNotEmpty()) {
            val activeStrs = placementMap.values.map { it.stringIndex }
            val targetStr = if (pickStrokeDirection > 0f) {
                activeStrs.minOrNull() ?: 2
            } else {
                activeStrs.maxOrNull() ?: 2
            }
            lastPluckedString = targetStr
            pickStrokeDirection = -pickStrokeDirection
            pickPluckAnim.snapTo(1f)
            pickPluckAnim.animateTo(0f, animationSpec = tween(145, easing = FastOutSlowInEasing))
        }
    }

    val animatedPickString by animateFloatAsState(
        targetValue = if (soundingPitches.isNotEmpty()) lastPluckedString.toFloat() else 2.5f,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 460f),
        label = "guitar_pick_string"
    )

    // High-frequency standing wave phase generator
    val infiniteTransition = rememberInfiniteTransition(label = "string_oscillation")
    val stringPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 68, easing = LinearEasing),
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
            setShadowLayer(3.5f, 0f, 1.2f, android.graphics.Color.argb(210, 0, 0, 0))
        }
    }
    val noteCalloutPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(2.5f, 0f, 1f, android.graphics.Color.argb(210, 0, 0, 0))
        }
    }
    val wristCuffPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(190, 0, 0, 0))
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val totalW = size.width
        val totalH = size.height

        // Synchronized 1:1 with UnifiedGuitarFretboard's computeAspectSafeViewport
        val vp = computeGuitarOverlayViewport(
            canvasWidth = totalW,
            canvasHeight = totalH,
            minAspect = 2.0f,
            maxAspect = 11.5f,
            referenceHeight = 165f,
            horizontalPaddingFrac = 0.01f,
            verticalPaddingFrac = 0.02f
        )

        val s = vp.scale
        val leftPad = vp.left + (44f * s).coerceIn(28f, vp.width * 0.16f)
        val rightPad = (totalW - vp.right) + (16f * s).coerceIn(10f, vp.width * 0.06f)
        val topPad = vp.top + (12f * s).coerceAtLeast(6f)
        val bottomPad = (totalH - vp.bottom) + (22f * s).coerceAtLeast(14f)

        val playableW = (totalW - leftPad - rightPad).coerceAtLeast(1f)
        val playableH = (totalH - topPad - bottomPad).coerceAtLeast(1f)

        val r = 0.056125687f
        var scaleSum = 0f
        for (f in 1..safeNumFrets) {
            val absoluteFret = safeBaseFret + f - 1
            scaleSum += (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
        }
        val safeScaleSum = scaleSum.coerceAtLeast(0.001f)

        fun getRelativeFretWireX(relFret: Int): Float {
            if (relFret <= 0) return leftPad
            var accumulated = 0f
            for (f in 1..relFret.coerceAtMost(safeNumFrets)) {
                val absoluteFret = safeBaseFret + f - 1
                val lengthFraction = (1f - r).toDouble().pow(absoluteFret.toDouble()).toFloat() * r
                accumulated += (lengthFraction / safeScaleSum) * playableW
            }
            return leftPad + accumulated
        }

        fun getFretTouchpointX(absoluteFret: Int): Float {
            if (absoluteFret <= 0) return leftPad - 12f * s
            val relFret = (absoluteFret - safeBaseFret + 1).coerceIn(1, safeNumFrets)
            val fretWire = getRelativeFretWireX(relFret)
            val prevFretWire = getRelativeFretWireX(relFret - 1)
            val fretWidth = fretWire - prevFretWire
            // Biomechanically authentic: guitarist presses just behind the fret wire (~78% of fret span)
            return fretWire - fretWidth * 0.22f
        }

        fun interpolateFretTouchpointX(fractionalFret: Float): Float {
            val fl = fractionalFret.toInt().coerceIn(0, safeBaseFret + safeNumFrets)
            val cl = (fl + 1).coerceIn(0, safeBaseFret + safeNumFrets)
            val frac = (fractionalFret - fl).coerceIn(0f, 1f)
            return getFretTouchpointX(fl) * (1f - frac) + getFretTouchpointX(cl) * frac
        }

        val nutH = playableH * 0.88f
        val bridgeH = playableH * 0.98f
        val midY = topPad + playableH / 2f
        val nutTop = midY - nutH / 2f
        val nutBottom = midY + nutH / 2f
        val bridgeTop = midY - bridgeH / 2f
        val bridgeBottom = midY + bridgeH / 2f
        val bridgeX = totalW - rightPad

        fun getStringYAtX(fractionalStrIdx: Float, x: Float): Float {
            val t = ((x - leftPad) / playableW).coerceIn(0f, 1f)
            val currentH = nutH + (bridgeH - nutH) * t
            val currentTop = midY - currentH / 2f
            val curStringSpacing = currentH / (numStrings - 1).coerceAtLeast(1).toFloat()
            return if (numStrings <= 1) midY else currentTop + fractionalStrIdx.coerceIn(0f, (numStrings - 1).toFloat()) * curStringSpacing
        }

        val stringSpacingAtNut = (nutH / (numStrings - 1).coerceAtLeast(1)).coerceAtLeast(8f)

        // =====================================================================
        // 1. FRET-STOPPED STANDING-WAVE STRING OSCILLATION
        // =====================================================================
        val activeByString = placementMap.values.associateBy { it.stringIndex }
        for ((strIdx, placement) in activeByString) {
            if (strIdx !in 0 until numStrings) continue
            val startX = if (placement.fret <= 0) {
                leftPad
            } else {
                val relFret = (placement.fret - safeBaseFret + 1).coerceIn(1, safeNumFrets)
                getRelativeFretWireX(relFret)
            }
            val vibratingLen = (bridgeX - startX).coerceAtLeast(12f)
            val stringPath = Path()
            val harmonicPath = Path()
            val steps = 32
            val baseAmp = (3.4f * s) * (0.38f + 0.62f * pickPluckAnim.value)

            for (step in 0..steps) {
                val u = step / steps.toFloat()
                val x = startX + u * vibratingLen
                val baseY = getStringYAtX(strIdx.toFloat(), x)
                // Fundamental standing wave node-to-node between fretted wire and bridge
                val fundamental = sin(u * PI.toFloat()) * sin(stringPhase + strIdx * 0.85f) * baseAmp
                // Subtle 2nd harmonic overtone shimmer
                val overtone = sin(u * 2f * PI.toFloat()) * sin(stringPhase * 1.7f + strIdx) * (baseAmp * 0.28f)
                val y1 = baseY + fundamental
                val y2 = baseY - fundamental * 0.65f + overtone
                if (step == 0) {
                    stringPath.moveTo(x, y1)
                    harmonicPath.moveTo(x, y2)
                } else {
                    stringPath.lineTo(x, y1)
                    harmonicPath.lineTo(x, y2)
                }
            }

            drawPath(
                path = stringPath,
                color = handAccentColor.copy(alpha = 0.58f * (0.45f + 0.55f * pickPluckAnim.value)),
                style = Stroke(width = (4.2f * s).coerceIn(2.2f, 7.5f), cap = StrokeCap.Round)
            )
            drawPath(
                path = harmonicPath,
                color = pickColor.copy(alpha = 0.36f * (0.4f + 0.6f * pickPluckAnim.value)),
                style = Stroke(width = (2.4f * s).coerceIn(1.2f, 4.5f), cap = StrokeCap.Round)
            )
            drawPath(
                path = stringPath,
                color = Color.White.copy(alpha = 0.90f),
                style = Stroke(width = (1.4f * s).coerceIn(0.9f, 2.8f), cap = StrokeCap.Round)
            )
        }

        // Shared Realistic Subsurface-Scattered Skin Palette
        val hasActiveNotes = soundingPitches.isNotEmpty()
        val activeAlpha = if (hasActiveNotes) 0.95f else 0.72f
        val skinHighlight = Color(
            red = (0.98f * 0.86f + handAccentColor.red * 0.14f).coerceIn(0f, 1f),
            green = (0.85f * 0.86f + handAccentColor.green * 0.14f).coerceIn(0f, 1f),
            blue = (0.75f * 0.86f + handAccentColor.blue * 0.14f).coerceIn(0f, 1f),
            alpha = activeAlpha
        )
        val skinMidtone = Color(
            red = (0.90f * 0.82f + handAccentColor.red * 0.18f).coerceIn(0f, 1f),
            green = (0.72f * 0.82f + handAccentColor.green * 0.18f).coerceIn(0f, 1f),
            blue = (0.61f * 0.82f + handAccentColor.blue * 0.18f).coerceIn(0f, 1f),
            alpha = activeAlpha * 0.96f
        )
        val skinSubsurface = Color(
            red = (0.81f * 0.78f + handAccentColor.red * 0.22f).coerceIn(0f, 1f),
            green = (0.56f * 0.78f + handAccentColor.green * 0.22f).coerceIn(0f, 1f),
            blue = (0.48f * 0.78f + handAccentColor.blue * 0.22f).coerceIn(0f, 1f),
            alpha = activeAlpha * 0.92f
        )
        val skinDeepShadow = Color(
            red = (0.55f * 0.75f + handAccentColor.red * 0.25f).coerceIn(0f, 1f),
            green = (0.38f * 0.75f + handAccentColor.green * 0.25f).coerceIn(0f, 1f),
            blue = (0.33f * 0.75f + handAccentColor.blue * 0.25f).coerceIn(0f, 1f),
            alpha = activeAlpha * 0.88f
        )

        // =====================================================================
        // 2. LEFT HAND: ULTRAREALISTIC BIOMECHANICAL FRETTING HAND
        // =====================================================================
        if (showFrettingHand) {
            val palmCenterX = interpolateFretTouchpointX(animatedBoxCenter)
                .coerceIn(leftPad + 16f * s, bridgeX - 36f * s)
            val palmBaseY = (nutBottom + 24.dp.toPx() * s).coerceAtMost(totalH + 10.dp.toPx())

            // A. Dynamic Thumb Anchor Behind Top Neck Edge (Peeking over near index/middle finger)
            val lowestStrPressed = placementMap.values.maxOfOrNull { it.stringIndex } ?: 3
            val thumbWrapBiasY = if (lowestStrPressed >= 4) 5f * s else 0f
            val thumbCenterX = (palmCenterX - 14f * s).coerceIn(leftPad + 8f * s, bridgeX - 24f * s)
            val thumbTopY = (nutTop - 13f * s + thumbWrapBiasY).coerceAtLeast(2f)
            val thumbW = (23f * s).coerceIn(15f, 36f)
            val thumbH = (26f * s).coerceIn(17f, 40f)

            // Thumb drop shadow on top edge of guitar neck
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.42f),
                topLeft = Offset(thumbCenterX - thumbW * 0.5f + 1.5f, thumbTopY + 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW * 0.48f, thumbW * 0.48f)
            )
            // Distal thumb phalanx peeking over top edge of neck
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(skinHighlight, skinMidtone, skinDeepShadow),
                    center = Offset(thumbCenterX - thumbW * 0.15f, thumbTopY + thumbH * 0.35f),
                    radius = thumbW * 0.9f
                ),
                topLeft = Offset(thumbCenterX - thumbW * 0.5f, thumbTopY),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW * 0.48f, thumbW * 0.48f)
            )
            drawRoundRect(
                color = skinDeepShadow.copy(alpha = 0.65f),
                topLeft = Offset(thumbCenterX - thumbW * 0.5f, thumbTopY),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW * 0.48f, thumbW * 0.48f),
                style = Stroke(width = 1.dp.toPx())
            )
            // Realistic thumb nail plate & lunula
            val tNailW = thumbW * 0.68f
            val tNailH = thumbH * 0.44f
            drawRoundRect(
                color = Color(0xFFFFF0E8).copy(alpha = 0.84f),
                topLeft = Offset(thumbCenterX - tNailW * 0.5f, thumbTopY + 2.5f * s),
                size = Size(tNailW, tNailH),
                cornerRadius = CornerRadius(tNailW * 0.4f, tNailW * 0.4f)
            )

            // B. Dorsal / Palmar Base Arch & Wrist Silhouette below Bottom Neck Edge
            val palmSpreadW = (44f * s).coerceIn(28f, 76f)
            val palmPath = Path().apply {
                moveTo(palmCenterX - palmSpreadW * 0.82f, palmBaseY + 34f * s)
                cubicTo(
                    palmCenterX - palmSpreadW * 1.05f, palmBaseY + 4f * s,
                    palmCenterX - palmSpreadW * 0.78f, palmBaseY - 12f * s,
                    palmCenterX - palmSpreadW * 0.46f, palmBaseY - 15f * s
                )
                quadraticBezierTo(
                    palmCenterX, palmBaseY - 19f * s,
                    palmCenterX + palmSpreadW * 0.54f, palmBaseY - 12f * s
                )
                cubicTo(
                    palmCenterX + palmSpreadW * 0.92f, palmBaseY - 6f * s,
                    palmCenterX + palmSpreadW * 1.02f, palmBaseY + 14f * s,
                    palmCenterX + palmSpreadW * 0.78f, palmBaseY + 36f * s
                )
                close()
            }

            drawPath(
                path = palmPath,
                color = Color.Black.copy(alpha = 0.35f),
                style = Stroke(width = 3.5.dp.toPx())
            )
            drawPath(
                path = palmPath,
                brush = Brush.radialGradient(
                    colors = listOf(skinHighlight, skinMidtone, skinSubsurface, skinDeepShadow),
                    center = Offset(palmCenterX - 6f * s, palmBaseY - 2f * s),
                    radius = palmSpreadW * 1.25f
                )
            )
            drawPath(
                path = palmPath,
                color = handAccentColor.copy(alpha = 0.72f),
                style = Stroke(width = 1.3.dp.toPx())
            )

            // C. Articulated 3-Joint Fretting Fingers 1..4 (with Automatic Index Barre Support)
            val animFrets = floatArrayOf(0f, animF1Fret, animF2Fret, animF3Fret, animF4Fret)
            val animStrs = floatArrayOf(0f, animF1Str, animF2Str, animF3Str, animF4Str)
            val animPresses = floatArrayOf(0f, animF1Press, animF2Press, animF3Press, animF4Press)

            val baseFingerHalfW = (stringSpacingAtNut * 0.44f).coerceIn(5.8f, 13.5f)
            fingerBadgePaint.textSize = (baseFingerHalfW * 1.28f).coerceIn(8.5.dp.toPx(), 13.dp.toPx())
            noteCalloutPaint.textSize = (baseFingerHalfW * 0.94f).coerceIn(6.5.dp.toPx(), 9.5.dp.toPx())

            for (fNum in 1..4) {
                val fingerPlacements = frettedByFinger[fNum].orEmpty()
                val isPressing = fingerPlacements.isNotEmpty()
                val press = animPresses[fNum]
                val fHalfW = baseFingerHalfW * FRET_FINGER_WIDTH_MULT[fNum]

                // Detect Index Finger Barre (Finger 1 pressing 2+ strings on the same fret)
                val isBarreFinger = fNum == 1 && fingerPlacements.size >= 2 &&
                    fingerPlacements.map { it.fret }.distinct().size == 1

                val tipX = interpolateFretTouchpointX(animFrets[fNum])
                    .coerceIn(leftPad + fHalfW, bridgeX - fHalfW)

                val tipY = if (isBarreFinger) {
                    val highestFrettedStr = fingerPlacements.minOf { it.stringIndex }
                    getStringYAtX(highestFrettedStr.toFloat(), tipX)
                } else {
                    val rawStrY = getStringYAtX(animStrs[fNum], tipX)
                    // Unpressed fingers hover slightly lower/curled off the string plane
                    rawStrY + (1f - press) * (9.5f * s)
                }

                // Metacarpophalangeal (MCP) knuckle anchor along palm arch
                val slotOffset = (fNum - 2.5f)
                val mcpX = palmCenterX + slotOffset * (14.5f * s) + (tipX - palmCenterX) * 0.28f
                val mcpY = palmBaseY - (11f * s) + abs(slotOffset) * (2.5f * s)

                // Proximal (PIP) and Distal (DIP) interphalangeal joints bow outward when arched onto a string
                val lateralBowX = (slotOffset * 3.8f * s) * (0.7f + 0.3f * press)
                val archDropY = (16f * s) * (1.15f - 0.30f * press)

                val pipX = mcpX * 0.54f + tipX * 0.46f + lateralBowX
                val pipY = maxOf(mcpY * 0.56f + tipY * 0.44f + archDropY, tipY + 10f * s)

                val dipX = mcpX * 0.24f + tipX * 0.76f + lateralBowX * 0.45f
                val dipY = mcpY * 0.24f + tipY * 0.76f + archDropY * 0.38f

                val bw = fHalfW * 1.08f
                val pw = fHalfW * 0.96f
                val dw = fHalfW * 0.88f
                val tw = fHalfW * (if (isBarreFinger) 0.94f else 0.82f)

                // String Contact Glow & Force Ripple under fretted string(s)
                if (isPressing) {
                    fingerPlacements.forEach { fp ->
                        val contactY = getStringYAtX(fp.stringIndex.toFloat(), tipX)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    handAccentColor.copy(alpha = 0.62f * press),
                                    handAccentColor.copy(alpha = 0.18f * press),
                                    Color.Transparent
                                ),
                                center = Offset(tipX, contactY),
                                radius = tw * 1.95f
                            ),
                            radius = tw * 1.95f,
                            center = Offset(tipX, contactY)
                        )
                    }
                }

                val fingerPath = Path().apply {
                    moveTo(mcpX - bw, mcpY)
                    quadraticBezierTo(
                        (mcpX - bw + pipX - pw) * 0.5f,
                        (mcpY + pipY) * 0.5f,
                        pipX - pw,
                        pipY
                    )
                    quadraticBezierTo(
                        (pipX - pw + dipX - dw) * 0.5f,
                        (pipY + dipY) * 0.5f,
                        dipX - dw,
                        dipY
                    )
                    quadraticBezierTo(
                        dipX - dw,
                        (dipY + tipY) * 0.5f,
                        tipX - tw,
                        tipY
                    )
                    // Rounded anatomical distal fingertip dome
                    cubicTo(
                        tipX - tw, tipY - tw * 1.25f,
                        tipX + tw, tipY - tw * 1.25f,
                        tipX + tw, tipY
                    )
                    quadraticBezierTo(
                        dipX + dw,
                        (dipY + tipY) * 0.5f,
                        dipX + dw,
                        dipY
                    )
                    quadraticBezierTo(
                        (pipX + pw + dipX + dw) * 0.5f,
                        (pipY + dipY) * 0.5f,
                        pipX + pw,
                        pipY
                    )
                    quadraticBezierTo(
                        (mcpX + bw + pipX + pw) * 0.5f,
                        (mcpY + pipY) * 0.5f,
                        mcpX + bw,
                        mcpY
                    )
                    close()
                }

                // Soft finger shadow cast onto the fretboard
                val shadowOffsetLeft = with(this) { 2.dp.toPx() }
                val shadowOffsetTop = with(this) { 3.dp.toPx() }
                withTransform({
                    translate(left = shadowOffsetLeft, top = shadowOffsetTop)
                }) {
                    drawPath(
                        path = fingerPath,
                        color = Color.Black.copy(alpha = if (isPressing) 0.42f else 0.24f)
                    )
                }

                // 3D Cylindrical Subsurface-Scattered Finger Shading
                drawPath(
                    path = fingerPath,
                    brush = Brush.linearGradient(
                        colors = listOf(skinHighlight, skinMidtone, skinSubsurface, skinDeepShadow),
                        start = Offset(pipX - pw * 1.1f, pipY),
                        end = Offset(pipX + pw * 1.1f, pipY)
                    )
                )

                // Central phalanx specular highlight ridge
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            skinHighlight.copy(alpha = 0.48f),
                            skinHighlight.copy(alpha = 0.18f)
                        ),
                        start = Offset(pipX, pipY),
                        end = Offset(tipX, tipY)
                    ),
                    start = Offset(pipX, pipY),
                    end = Offset(tipX, tipY),
                    strokeWidth = pw * 0.45f,
                    cap = StrokeCap.Round
                )

                // PIP & DIP Joint Skin Crease Folds
                val pipCreaseW = pw * 0.65f
                drawLine(
                    color = skinDeepShadow.copy(alpha = 0.52f),
                    start = Offset(pipX - pipCreaseW, pipY - 0.8.dp.toPx()),
                    end = Offset(pipX + pipCreaseW, pipY - 0.8.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = skinDeepShadow.copy(alpha = 0.36f),
                    start = Offset(pipX - pipCreaseW * 0.8f, pipY + 1.2.dp.toPx()),
                    end = Offset(pipX + pipCreaseW * 0.8f, pipY + 1.2.dp.toPx()),
                    strokeWidth = 0.85.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val dipCreaseW = dw * 0.58f
                drawLine(
                    color = skinDeepShadow.copy(alpha = 0.46f),
                    start = Offset(dipX - dipCreaseW, dipY),
                    end = Offset(dipX + dipCreaseW, dipY),
                    strokeWidth = 0.9.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Finger perimeter contour
                drawPath(
                    path = fingerPath,
                    color = if (isPressing) handAccentColor.copy(alpha = 0.92f) else skinDeepShadow.copy(alpha = 0.55f),
                    style = Stroke(
                        width = if (isPressing) 1.6.dp.toPx() else 1.0.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Realistic Fingernail Plate (Nail Bed + Lunula + Free Edge + Specular Glint)
                val nailW = tw * 1.24f
                val nailH = tw * (1.28f - 0.14f * press)
                val nailTopY = tipY - tw * 0.72f
                val nailLeftX = tipX - nailW * 0.5f
                val nailCorner = CornerRadius(nailW * 0.42f, nailW * 0.42f)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (isPressing) {
                            listOf(Color(0xFFFFF6F0), Color(0xFFFFCDD2))
                        } else {
                            listOf(Color(0xFFFFEDE4).copy(alpha = 0.84f), Color(0xFFF5C6CB).copy(alpha = 0.78f))
                        },
                        startY = nailTopY,
                        endY = nailTopY + nailH
                    ),
                    topLeft = Offset(nailLeftX, nailTopY),
                    size = Size(nailW, nailH),
                    cornerRadius = nailCorner
                )
                // Distal white free edge
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isPressing) 0.92f else 0.72f),
                    topLeft = Offset(nailLeftX + nailW * 0.08f, nailTopY),
                    size = Size(nailW * 0.84f, (nailH * 0.22f).coerceAtLeast(1.4.dp.toPx())),
                    cornerRadius = nailCorner
                )
                // Proximal half-moon lunula
                val lunulaW = nailW * 0.54f
                val lunulaH = nailH * 0.24f
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isPressing) 0.65f else 0.42f),
                    topLeft = Offset(tipX - lunulaW * 0.5f, nailTopY + nailH - lunulaH * 1.15f),
                    size = Size(lunulaW, lunulaH),
                    cornerRadius = CornerRadius(lunulaW * 0.5f, lunulaW * 0.5f)
                )
                drawRoundRect(
                    color = if (isPressing) handAccentColor else skinDeepShadow.copy(alpha = 0.55f),
                    topLeft = Offset(nailLeftX, nailTopY),
                    size = Size(nailW, nailH),
                    cornerRadius = nailCorner,
                    style = Stroke(width = if (isPressing) 1.4.dp.toPx() else 0.8.dp.toPx())
                )

                // Glassmorphic Finger Number Badge (1..4)
                val badgeY = tipY + tw * 1.45f
                val badgeRad = (tw * (if (isPressing) 1.15f else 0.90f)).coerceAtLeast(7.dp.toPx())

                if (isPressing) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(handAccentColor.copy(alpha = 0.65f), Color.Transparent),
                            center = Offset(tipX, badgeY),
                            radius = badgeRad * 1.75f
                        ),
                        radius = badgeRad * 1.75f,
                        center = Offset(tipX, badgeY)
                    )
                }

                drawCircle(
                    color = if (isPressing) handAccentColor else Color(0xFF141822).copy(alpha = 0.86f),
                    radius = badgeRad,
                    center = Offset(tipX, badgeY)
                )
                drawCircle(
                    color = if (isPressing) Color.White else handAccentColor.copy(alpha = 0.72f),
                    radius = badgeRad,
                    center = Offset(tipX, badgeY),
                    style = Stroke(width = if (isPressing) 1.8.dp.toPx() else 1.0.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.drawText(
                    if (isBarreFinger) "1B" else fNum.toString(),
                    tipX,
                    badgeY + fingerBadgePaint.textSize * 0.36f,
                    fingerBadgePaint
                )

                // Floating Note Name Callout above pressed fingertip
                if (isPressing) {
                    val topPlacement = fingerPlacements.minByOrNull { it.stringIndex }!!
                    val openSemi = openStringsMidi.getOrNull(topPlacement.stringIndex) ?: 40
                    val computedMidi = openSemi + topPlacement.fret
                    val noteLabel = formatGuitarPitchLabel(computedMidi)
                    val pillW = (badgeRad * 2.35f).coerceAtLeast(22.dp.toPx())
                    val pillH = (badgeRad * 1.25f).coerceAtLeast(12.dp.toPx())
                    val pillY = (nailTopY - pillH * 0.72f).coerceAtLeast(pillH * 0.6f)

                    drawRoundRect(
                        color = Color(0xFF0E131F).copy(alpha = 0.88f),
                        topLeft = Offset(tipX - pillW * 0.5f, pillY - pillH * 0.5f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH * 0.5f, pillH * 0.5f)
                    )
                    drawRoundRect(
                        color = handAccentColor.copy(alpha = 0.92f),
                        topLeft = Offset(tipX - pillW * 0.5f, pillY - pillH * 0.5f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH * 0.5f, pillH * 0.5f),
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        noteLabel,
                        tipX,
                        pillY + noteCalloutPaint.textSize * 0.35f,
                        noteCalloutPaint
                    )
                }
            }

            // D. Glassmorphic Left-Hand Position Telemetry Wrist Cuff
            val cuffW = (64f * s).coerceIn(48.dp.toPx(), 92.dp.toPx())
            val cuffH = (16f * s).coerceIn(13.dp.toPx(), 20.dp.toPx())
            val cuffY = (palmBaseY + 2f * s).coerceAtMost(totalH - cuffH * 0.6f - 2f)
            val posFretInt = animatedBoxCenter.toInt().coerceAtLeast(1)
            val cuffText = if (frettedByFinger.isNotEmpty()) {
                "LH • POS $posFretInt"
            } else if (soundingPitches.isNotEmpty()) {
                "LH • OPEN"
            } else {
                "LH • FRET $posFretInt"
            }

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF121726).copy(alpha = 0.92f),
                        handAccentColor.copy(alpha = 0.88f),
                        Color(0xFF121726).copy(alpha = 0.92f)
                    ),
                    startX = palmCenterX - cuffW * 0.5f,
                    endX = palmCenterX + cuffW * 0.5f
                ),
                topLeft = Offset(palmCenterX - cuffW * 0.5f, cuffY - cuffH * 0.5f),
                size = Size(cuffW, cuffH),
                cornerRadius = CornerRadius(cuffH * 0.5f, cuffH * 0.5f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(palmCenterX - cuffW * 0.5f, cuffY - cuffH * 0.5f),
                size = Size(cuffW, cuffH),
                cornerRadius = CornerRadius(cuffH * 0.5f, cuffH * 0.5f),
                style = Stroke(width = 1.1.dp.toPx())
            )
            wristCuffPaint.textSize = (cuffH * 0.54f).coerceIn(7.5.dp.toPx(), 10.5.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                cuffText,
                palmCenterX,
                cuffY + wristCuffPaint.textSize * 0.36f,
                wristCuffPaint
            )
        }

        // =====================================================================
        // 3. RIGHT HAND: PERSISTENT PICKING HAND & BEVELLED CELLULOID PLECTRUM
        // =====================================================================
        if (showPickingHand) {
            val pickX = (bridgeX - 20f * s).coerceIn(leftPad + playableW * 0.72f, totalW - 14f)
            val pluckImpulse = sin(pickPluckAnim.value * PI.toFloat())
            val pluckDipY = pluckImpulse * (8.5f * s) * pickStrokeDirection
            val pickY = getStringYAtX(animatedPickString, pickX) + pluckDipY
            val pickTiltDeg = (pluckImpulse * 14f * pickStrokeDirection).coerceIn(-16f, 16f)

            val pickW = (25f * s).coerceIn(16f, 38f)
            val pickH = (28f * s).coerceIn(18f, 42f)
            val pickTipX = pickX - pickW * 0.44f
            val pickTipY = pickY

            withTransform({
                rotate(degrees = pickTiltDeg, pivot = Offset(pickX + 10f * s, pickY))
            }) {
                val rhPalmX = pickX + 28f * s
                val rhPalmY = pickY - 18f * s

                // 1. Curled Ulnar Fingers (Middle 2, Ring 3, Pinky 4) tucked gracefully beneath the index finger
                for (fIdx in 4 downTo 2) {
                    val curlStep = (fIdx - 1)
                    val fCurledX = pickX + (curlStep * 5.5f * s)
                    val fCurledY = pickY + (curlStep * 8.5f * s)
                    val fCurledW = (22f * s) - (curlStep * 1.8f * s)
                    val fCurledH = (10.5f * s).coerceAtLeast(6.5f)

                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(skinMidtone, skinSubsurface, skinDeepShadow),
                            start = Offset(fCurledX, fCurledY - fCurledH * 0.5f),
                            end = Offset(fCurledX + fCurledW, fCurledY + fCurledH * 0.5f)
                        ),
                        topLeft = Offset(fCurledX, fCurledY - fCurledH * 0.5f),
                        size = Size(fCurledW, fCurledH),
                        cornerRadius = CornerRadius(fCurledH * 0.5f, fCurledH * 0.5f)
                    )
                    drawRoundRect(
                        color = skinDeepShadow.copy(alpha = 0.55f),
                        topLeft = Offset(fCurledX, fCurledY - fCurledH * 0.5f),
                        size = Size(fCurledW, fCurledH),
                        cornerRadius = CornerRadius(fCurledH * 0.5f, fCurledH * 0.5f),
                        style = Stroke(width = 0.9.dp.toPx())
                    )
                }

                // 2. Index Finger Distal & Middle Phalanx Curling UNDER the Pick to Support It
                val indexSupportPath = Path().apply {
                    moveTo(pickX - 3f * s, pickY + 5f * s)
                    quadraticBezierTo(
                        pickX + 16f * s, pickY + 15f * s,
                        rhPalmX + 4f * s, rhPalmY + 14f * s
                    )
                    quadraticBezierTo(
                        pickX + 16f * s, pickY - 2f * s,
                        pickX - 3f * s, pickY + 5f * s
                    )
                    close()
                }
                drawPath(
                    path = indexSupportPath,
                    brush = Brush.linearGradient(
                        colors = listOf(skinHighlight, skinMidtone, skinDeepShadow),
                        start = Offset(pickX, pickY),
                        end = Offset(rhPalmX, rhPalmY + 14f * s)
                    )
                )

                // 3. Tear-Drop 3D Tortoiseshell / Pearloid Celluloid Guitar Pick
                val pickPath = Path().apply {
                    moveTo(pickTipX, pickTipY)
                    cubicTo(
                        pickTipX + pickW * 0.24f, pickTipY - pickH * 0.48f,
                        pickTipX + pickW * 0.86f, pickTipY - pickH * 0.44f,
                        pickTipX + pickW, pickTipY - pickH * 0.14f
                    )
                    cubicTo(
                        pickTipX + pickW * 1.06f, pickTipY + pickH * 0.05f,
                        pickTipX + pickW * 0.86f, pickTipY + pickH * 0.44f,
                        pickTipX + pickW * 0.24f, pickTipY + pickH * 0.48f
                    )
                    close()
                }

                // Pick shadow on strings
                val pickShadowLeft = with(this) { 1.5.dp.toPx() }
                val pickShadowTop = with(this) { 2.5.dp.toPx() }
                withTransform({
                    translate(left = pickShadowLeft, top = pickShadowTop)
                }) {
                    drawPath(path = pickPath, color = Color.Black.copy(alpha = 0.42f))
                }

                drawPath(
                    path = pickPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF8E1),
                            pickColor,
                            Color(0xFFE65100),
                            Color(0xFF3E2723)
                        ),
                        center = Offset(pickTipX + pickW * 0.46f, pickTipY - pickH * 0.08f),
                        radius = pickW * 0.88f
                    )
                )
                // Bevelled celluloid specular edge
                drawPath(
                    path = pickPath,
                    color = Color.White.copy(alpha = 0.86f),
                    style = Stroke(width = (1.5f * s).coerceIn(1.1f, 2.6f))
                )
                // Embossed concentric grip rings on pick
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = pickW * 0.16f,
                    center = Offset(pickTipX + pickW * 0.54f, pickTipY),
                    style = Stroke(width = 1.dp.toPx())
                )

                // 4. Right Hand Dorsum & Opposed Thumb Pinching Top of Pick
                val rhThumbPath = Path().apply {
                    moveTo(pickX - 4f * s, pickY - 3f * s)
                    cubicTo(
                        pickX - 6f * s, pickY - 16f * s,
                        pickX + 12f * s, pickY - 22f * s,
                        rhPalmX + 10f * s, rhPalmY - 4f * s
                    )
                    quadraticBezierTo(
                        rhPalmX + 22f * s, rhPalmY + 12f * s,
                        rhPalmX + 4f * s, rhPalmY + 18f * s
                    )
                    quadraticBezierTo(
                        pickX + 12f * s, pickY + 4f * s,
                        pickX - 4f * s, pickY - 3f * s
                    )
                    close()
                }
                drawPath(
                    path = rhThumbPath,
                    brush = Brush.linearGradient(
                        colors = listOf(skinHighlight, skinMidtone, skinSubsurface, skinDeepShadow),
                        start = Offset(pickX, pickY - 18f * s),
                        end = Offset(rhPalmX + 16f * s, rhPalmY + 18f * s)
                    )
                )
                drawPath(
                    path = rhThumbPath,
                    color = skinDeepShadow.copy(alpha = 0.58f),
                    style = Stroke(width = 1.1.dp.toPx())
                )

                // Right Thumb Nail Plate (viewed in lateral profile gripping pick)
                val rhNailW = (11f * s).coerceAtLeast(7f)
                val rhNailH = (7.5f * s).coerceAtLeast(5f)
                drawRoundRect(
                    color = Color(0xFFFFF2EA).copy(alpha = 0.88f),
                    topLeft = Offset(pickX - 1f * s, pickY - 13f * s),
                    size = Size(rhNailW, rhNailH),
                    cornerRadius = CornerRadius(rhNailH * 0.45f, rhNailH * 0.45f)
                )
                drawRoundRect(
                    color = skinDeepShadow.copy(alpha = 0.5f),
                    topLeft = Offset(pickX - 1f * s, pickY - 13f * s),
                    size = Size(rhNailW, rhNailH),
                    cornerRadius = CornerRadius(rhNailH * 0.45f, rhNailH * 0.45f),
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // Thumb Interphalangeal (IP) knuckle crease lines
                drawLine(
                    color = skinDeepShadow.copy(alpha = 0.48f),
                    start = Offset(pickX + 13f * s, pickY - 14f * s),
                    end = Offset(pickX + 16f * s, pickY - 5f * s),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. Acoustic Pluck Spark & String Attack Halo at Pick Tip
            if (pickPluckAnim.value > 0.08f) {
                val sparkAlpha = pickPluckAnim.value
                val sparkRad = (13f * s) * (0.5f + 0.6f * sparkAlpha)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f * sparkAlpha),
                             Color(0xFFFFEB3B).copy(alpha = 0.72f * sparkAlpha),
                            pickColor.copy(alpha = 0.28f * sparkAlpha),
                            Color.Transparent
                        ),
                        center = Offset(pickTipX, pickTipY),
                        radius = sparkRad
                    ),
                    radius = sparkRad,
                    center = Offset(pickTipX, pickTipY)
                )
            }
        }
    }
}