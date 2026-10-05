@file:OptIn(ExperimentalMaterial3Api::class)

package com.creativeblisslabs.peqlearn.app
import kotlin.IllegalStateException
import kotlin.collections.listOf
import kotlin.collections.forEach
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import com.creativeblisslabs.peqlearn.app.ui.components.*
import peq_learn.composeapp.generated.resources.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import com.creativeblisslabs.peqlearn.app.ui.screens.*
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.theme.*
import com.creativeblisslabs.peqlearn.app.ui.sections.HeaderSection
import com.creativeblisslabs.peqlearn.app.Tab
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import com.creativeblisslabs.peqlearn.app.data.MediaType
import androidx.compose.ui.zIndex
import com.creativeblisslabs.peqlearn.app.data.Device
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import peq_learn.composeapp.generated.resources.Res
import peq_learn.composeapp.generated.resources.app_icon
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> { throw IllegalStateException("No AppViewModel provided") }
@Composable
fun App() {

    val viewModel =
        LocalAppViewModel.current

    val theme by viewModel.selectedTheme
    val accent by viewModel.selectedAccent

    val showSplash =
        !viewModel.splashCompleted

LaunchedEffect(Unit) {

    if (!viewModel.splashCompleted) {

        delay(2400)

        viewModel.completeSplash()
    }
}

    pEQTheme(theme = theme, accent = accent) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
        ) {

            AnimatedContent(
    targetState = showSplash,

    transitionSpec = {

        fadeIn(
            animationSpec =
                tween(700)
        ).togetherWith(

            fadeOut(
                animationSpec =
                    tween(500)
            )
        )
    },

    label = "app_root_transition"
) { splash ->

    if (splash) {

        pEQSplashScreen()

    } else {

        MainScreen(viewModel)
    }
}

// 🧠 NEURAL FOCUS OVERLAY (Lock Screen / Floating Lyrics)
AnimatedVisibility(
    visible = viewModel.isLockScreenMode.value && !viewModel.isInPipMode.value,
    enter = fadeIn(tween(400)) + slideInVertically { it },
    exit = fadeOut(tween(400)) + slideOutVertically { it }
) {
    NeuralLockScreenOverlay(viewModel)
}

        }
    }
}
@Composable
fun pEQSplashScreen() {
    val infinite = rememberInfiniteTransition(label = "pEQ_splash")

    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val glow by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Use DynamicBackground instead of hardcoded gradient
        DynamicBackground(  baseColor = MaterialTheme.colorScheme.primary,
            transientImpact = 0.2f)

        // =========================
        // SOFT AMBIENT LIGHT (Yellow Glow)
        // =========================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-120).dp)
                .size(420.dp)
                .graphicsLayer {
                    scaleX = glow
                    scaleY = glow
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD600).copy(alpha = 0.15f), // Yellow accent
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(80.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // =========================
            // LOGO CONTAINER
            // =========================
            Surface(
                modifier = Modifier.size(124.dp),
                shape = RoundedCornerShape(34.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFFFD600).copy(alpha = 0.2f) // Yellow border tint
                ),
                tonalElevation = 10.dp,
                shadowElevation = 20.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color(0xFFFFD600).copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier.clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(Modifier.height(34.dp))

            // =========================
            // TITLE
            // =========================
            Text(
                text = "pEQ",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Media Player",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(42.dp))

            // =========================
            // PROGRESS BAR (Yellow Gradient)
            // =========================
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD600), // Vibrant Yellow
                                    Color(0xFFFFA000)  // Amber/Orange
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(18.dp))
           var loadingText by remember {
        mutableStateOf("Loading assets...")
}

LaunchedEffect(Unit) {
    delay(1000)
    loadingText = "audio components..."
}
LaunchedEffect(Unit) {
    delay(1000)
    loadingText = "video components..."
}

LaunchedEffect(Unit) {
    delay(1000)
    loadingText = "neural engine..."
}
Text(
    text = loadingText,
    color = Color.White.copy(alpha = 0.42f),
    fontSize = 11.sp,
    letterSpacing = 0.5.sp
)
        }

        // =========================
        // VERSION
        // =========================
        Text(
            text = "v1.0",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            color = Color.White.copy(alpha = 0.18f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun NeuralLockScreenOverlay(
    viewModel: AppViewModel
) {

    val currentItem = viewModel.currentMediaItem.value ?: return
    val isFocusMode by viewModel.isFocusMode
    val isLockScreenMode by viewModel.isLockScreenMode

    val isPlaying = viewModel.isPlaying.value
    val currentPosition = viewModel.currentPosition.value

    val lyrics = viewModel.syncedLyrics

    val currentLineIndex by remember(
        lyrics,
        currentPosition
    ) {

        derivedStateOf {

            lyrics.indexOfLast {
                it.timestampMs <= currentPosition
            }.coerceAtLeast(0)
        }
    }

    val currentLyric =
        lyrics.getOrNull(currentLineIndex)

    val nextLyric =
        lyrics.getOrNull(currentLineIndex + 1)

    val progressToNext by remember(
        currentPosition,
        currentLyric,
        nextLyric
    ) {

        derivedStateOf {

            if (
                currentLyric == null ||
                nextLyric == null
            ) {
                0f
            } else {

                (
                    (currentPosition -
                        currentLyric.timestampMs)
                        .toFloat() /

                        (
                            nextLyric.timestampMs -
                                currentLyric.timestampMs
                            )
                            .toFloat()
                            .coerceAtLeast(1f)

                    ).coerceIn(0f, 1f)
            }
        }
    }

    var controlsVisible by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(
        controlsVisible,
        isPlaying
    ) {

        if (controlsVisible && isPlaying) {

            delay(4000)
            controlsVisible = false
        }
    }

    // =========================================================
    // ROOT
    // =========================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFocusMode) Color.Black else Color.Black.copy(alpha = 0.97f))
            .pointerInput(Unit) {
                detectTapGestures {
                    if (!isFocusMode) controlsVisible = !controlsVisible
                }
            }
    ) {

        // =====================================================
        // BACKGROUND
        // =====================================================
        if (!isFocusMode) {
            DynamicBackground(
                baseColor = MaterialTheme.colorScheme.primary,
                transientImpact = 0.16f
            )
        }

        // =====================================================
        // TOP GLOW
        // =====================================================
        if (!isFocusMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.14f),

                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(
                    horizontal = 24.dp,
                    vertical = 20.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            // =============================================
            // STATUS
            // =============================================

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() +
                    slideInVertically {
                        -it / 2
                    },
                exit = fadeOut()
            ) {

                Surface(
                    shape = RoundedCornerShape(24.dp),

                    color =
                        MaterialTheme.colorScheme.surface
                            .copy(alpha = 0.14f),

                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                            .copy(alpha = 0.08f)
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlaying) {
                                        Color(0xFF00E676)
                                    } else {
                                        Color.Gray
                                    }
                                )
                        )

                        Spacer(
                            Modifier.width(10.dp)
                        )

                        Text(
                            text =
                                if (isPlaying) {
                                    "NEURAL FOCUS ACTIVE"
                                } else {
                                    "PLAYBACK PAUSED"
                                },

                            color = Color.White,

                            fontWeight = FontWeight.Bold,

                            fontSize = 12.sp,

                            letterSpacing = 1.1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // =============================================
            // ARTWORK
            // =============================================

            Surface(
                modifier = Modifier.size(220.dp),

                shape = RoundedCornerShape(42.dp),

                color =
                    MaterialTheme.colorScheme.surface
                        .copy(alpha = 0.12f),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.08f)
                ),

                shadowElevation = 18.dp
            ) {

                if (currentItem.thumbnail != null || currentItem.path.isNotEmpty()) {

                    pEQImage(
                        data = currentItem.thumbnail,
                        fallbackPath = currentItem.path,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        mediaType = currentItem.type
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF161616),
                                        Color(0xFF0A0A0A)
                                    )
                                )
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "pEQ",

                            color =
                                MaterialTheme.colorScheme.primary,

                            fontSize = 54.sp,

                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // =============================================
            // TRACK INFO
            // =============================================

            Column(
                modifier = Modifier.fillMaxWidth(),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        currentItem.title
                            .ifBlank { "Unknown" },

                    style =
                        MaterialTheme.typography
                            .headlineMedium,

                    maxLines = 2,

                    textAlign = TextAlign.Center,

                    color =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text =
                        currentItem.artist
                            ?: "Unknown Artist",

                    style =
                        MaterialTheme.typography
                            .titleMedium,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(28.dp))

            // =============================================
            // LYRICS CARD
            // =============================================

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),

                shape = RoundedCornerShape(34.dp),

                color =
                    MaterialTheme.colorScheme.surface
                        .copy(alpha = 0.12f),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.06f)
                )
            ) {

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 28.dp,
                                vertical = 34.dp
                            ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        AnimatedContent(
                            targetState = currentLyric,

                            transitionSpec = {

                                fadeIn(
                                    tween(320)
                                ) togetherWith

                                    fadeOut(
                                        tween(220)
                                    )
                            },

                            label = "lyrics_focus"
                        ) { lyric ->

                            Text(
                                text =
                                    lyric?.text
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?: "♪",

                                style = TextStyle(
                                    color = Color.White,

                                    fontSize = 32.sp,

                                    fontWeight =
                                        FontWeight.Black,

                                    textAlign =
                                        TextAlign.Center,

                                    lineHeight = 44.sp
                                )
                            )
                        }

                        Spacer(
                            Modifier.height(26.dp)
                        )

                        // =====================================
                        // LYRIC PROGRESS
                        // =====================================

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(
                                    Color.White.copy(
                                        alpha = 0.08f
                                    )
                                )
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(
                                        progressToNext
                                    )
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // =====================================
                    // WATERMARK
                    // =====================================

                    Text(
                        text = "pEQ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 14.dp, end = 18.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // =============================================
            // TRANSPORT CONTROLS
            // =============================================

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {

                TransportControlsRestored(
                    viewModel = viewModel
                )
            }

            Spacer(Modifier.height(18.dp))

            // =============================================
            // EXIT
            // =============================================

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {

                OutlinedButton(
                    onClick = {
                        viewModel.isLockScreenMode.value =
                            false
                    },

                    shape = RoundedCornerShape(20.dp),

                    border = BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.08f)
                    ),

                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor =
                                Color.White.copy(alpha = 0.05f)
                        )
                ) {

                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "EXIT FOCUS",

                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Focus Mode / Lock Controls
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            if (isFocusMode) {
                // Top Left: Unlock
                IconButton(
                    onClick = { viewModel.toggleFocusMode() },
                    modifier = Modifier.align(Alignment.TopStart).size(56.dp)
                ) {
                    Icon(Icons.Default.LockOpen, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                }
            }
            
            // Top Right: Lock/Unlock Button (Always visible on Lock Screen mode)
            IconButton(
                onClick = { 
                    if (isFocusMode) viewModel.toggleFocusMode() 
                    else viewModel.toggleLockScreenMode()
                },
                modifier = Modifier.align(Alignment.TopEnd).size(56.dp)
            ) {
                Icon(
                    imageVector = if (isFocusMode || isLockScreenMode) Icons.Default.VpnKey else Icons.Default.VpnLock,
                    contentDescription = null,
                    tint = if (isFocusMode || isLockScreenMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun SmallTransportButton(
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun TransportControlsRestored(viewModel: AppViewModel) {
    val item = viewModel.currentMediaItem.value ?: return
    val isFavorite = viewModel.favoriteItems.any { it.path == item.path }
    val _repeatMode by viewModel.repeatMode
    var showVolumePopup by remember { mutableStateOf(false) }
    val volumeIcon = when {
        viewModel.masterVolume.value > 0.5f -> Icons.AutoMirrored.Filled.VolumeUp
        viewModel.masterVolume.value > 0f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeMute
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SmallTransportButton(icon = volumeIcon, active = showVolumePopup) { showVolumePopup = !showVolumePopup }
        if (showVolumePopup) {
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -280), onDismissRequest = { showVolumePopup = false }) {
                Surface(modifier = Modifier.padding(horizontal = 16.dp).width(280.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, tonalElevation = 12.dp) {
                    Box(Modifier.padding(24.dp)) { VolumeControlSlider(viewModel = viewModel) }
                }
            }
        }
        FilledTonalIconButton(onClick = { viewModel.previousTrack() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(30.dp)) }
        Surface(onClick = { viewModel.togglePlayback() }, modifier = Modifier.size(78.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp) {
            Box(contentAlignment = Alignment.Center) { Icon(if (viewModel.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        }
        FilledTonalIconButton(onClick = { viewModel.nextTrack() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(30.dp)) }
        SmallTransportButton(icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, active = isFavorite) { viewModel.toggleFavorite(item) }
        SmallTransportButton(icon = Icons.Default.AutoAwesome, active = viewModel.isAiUpscalingEnabled.value) { viewModel.toggleAiUpscaling() }
    }
}
@Composable
fun MainScreen(
    viewModel: AppViewModel
) {

    val snackbarHostState =
        remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab


    LaunchedEffect(
        viewModel.toastMessage.value
    ) {

        viewModel.toastMessage.value?.let { msg ->

            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )

            viewModel.toastMessage.value = null
        }
    }

    // =========================
    // INITIALIZATION
    // =========================

    LaunchedEffect(Unit) {
        viewModel.start()
        viewModel.setMasterVolume(getSystemVolume())
        
        // 🔥 FIRST INSTALL ONBOARDING
        val folders = viewModel.persistenceManager.loadScanFolders()
        if (folders.isEmpty()) {
            delay(400) // Let the app breathe
            viewModel.showFolderManager.value = true
        }
    }

    // =========================
    // GLOBAL BACK
    // =========================

    pEQBackHandler(

        enabled =
            currentTab != Tab.Dashboard 

    ) {

        viewModel.currentTab.value =
            Tab.Dashboard
    }

    // =========================
    // ROOT
    // =========================

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        val isWideScreen =
            maxWidth > 840.dp || maxWidth > maxHeight



        Scaffold(

            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),

            containerColor = Color.Black,

            contentWindowInsets =

             
                    WindowInsets.safeDrawing,

            topBar = {

                if (
                    !isWideScreen
                ) {

                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                    ) {

                        HeaderSection(viewModel)
                    }
                }
            },

            snackbarHost = {

                SnackbarHost(
                    snackbarHostState
                )
            },

            bottomBar = {

                if (
                    !isWideScreen) {

                   BottomNavigationBar(
                    current = currentTab,
                    viewModel = viewModel,
                    onSelect = {
                        viewModel.currentTab.value = it
                    }
                )
                }
            }

        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                // ALWAYS RENDER BACKGROUND FIRST (Optimized for Focus Modes)
                if (!viewModel.isLockScreenMode.value || viewModel.isInPipMode.value) {
                    DynamicBackground(
                        baseColor = MaterialTheme.colorScheme.primary,
                        transientImpact = 0.2f
                    )
                }



                    // =========================
                    // NORMAL APP LAYOUT
                    // =========================

                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        // =========================
                        // TABLET / LANDSCAPE NAV
                        // =========================

                        if (
                            isWideScreen 
                        ) {

                            Column(

                                modifier = Modifier
                                    .width(240.dp)
                                    .fillMaxHeight()
                                    .statusBarsPadding()
                                    .background(
                                        MaterialTheme.colorScheme.surface
                                    )
                                    .padding(16.dp),

                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.currentTab.value = Tab.Player }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                     Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(Res.drawable.app_icon),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(), 
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "pEQ",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "TAP TO OPEN PLAYER",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 7.sp,
                                            letterSpacing = 0.5.sp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(
                                    Modifier.height(24.dp)
                                )

                                NavigationRailSection(
                                    viewModel
                                )
                            }
                        }

                        // =========================
                        // MAIN CONTENT
                        // =========================

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(

                                    if (
                                        !isWideScreen 
                                    ) {

                                        Modifier.padding(
                                            paddingValues
                                        )

                                    } else {

                                        Modifier
                                    }
                                )
                        ) {

                            AnimatedContent(
                                targetState = currentTab,

                                transitionSpec = {

                                    fadeIn(
                                        tween(220)
                                    ).togetherWith(

                                        fadeOut(
                                            tween(160)
                                        )
                                    )
                                },

                                label = "main_content"
                            ) { tab ->

                                when (tab) {

                                    Tab.Dashboard -> {
                                        pEQWorkstationDashboard(viewModel)
                                    }

                                    Tab.Player -> {
                                        pEQWorkstationDashboard(viewModel)
                                    }

                                    Tab.Mixer -> {
                                        pEQMixerScreen(viewModel)
                                    }

                                    Tab.Learn -> {
                                        pEQLearnScreen(viewModel)
                                    }

                                    Tab.Settings -> {
                                        SettingsPanel(viewModel)
                                    }
                                }
                            }
                        }
                    }
                

                // =========================
                // FOLDER MANAGER
                // =========================

                if (viewModel.showFolderManager.value && viewModel.currentTab.value != Tab.Player) {
                    PEQArchiveRegistryDialog {
                        viewModel.showFolderManager.value = false
                    }
                }
                
                // =========================
                // DEVICE SWITCHER
                // =========================

                if (viewModel.showDeviceSwitcher.value) {

                    DeviceSwitcherDialog(
                        viewModel
                    )
                }
            }
        }
    }

}

@Composable
fun Modifier.auroraBorder(): Modifier {

    val infinite = rememberInfiniteTransition()

    val alpha by infinite.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    return this.border(
        BorderStroke(
            if (isLight) 0.5.dp else 1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(if (isLight) alpha / 2 else alpha),
                    Color.Transparent,
                    MaterialTheme.colorScheme.secondary.copy(if (isLight) alpha / 2 else alpha)
                )
            )
        ),
        RoundedCornerShape(22.dp)
    )
}


@Composable
fun DynamicBackground(
    baseColor: Color,
    transientImpact: Float
) {

    val scheme = MaterialTheme.colorScheme

    val isLight =
        scheme.background.luminance() > 0.5f

    val infinite =
        rememberInfiniteTransition()

    val drift by infinite.animateFloat(
        initialValue = -120f,
        targetValue = 120f,

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18000,
                easing = LinearEasing
            ),

            repeatMode = RepeatMode.Reverse
        )
    )

    val pulse by infinite.animateFloat(
        initialValue =
            if (isLight) 0.08f else 0.16f,

        targetValue =
            if (isLight) 0.14f else 0.28f,

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000,
                easing = EaseInOutSine
            ),

            repeatMode = RepeatMode.Reverse
        )
    )

    val reactiveAlpha by animateFloatAsState(
        targetValue =
            (0.03f + transientImpact * 0.08f)
                .coerceAtMost(0.18f),

        animationSpec = tween(400),

        label = "reactive"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors =

                        if (isLight) {

                            listOf(
                                Color(0xFFF7F8FC),
                                Color(0xFFEFF2F9),
                                Color(0xFFF9FAFD)
                            )

                        } else {

                            listOf(
                                Color(0xFF05070B),
                                Color(0xFF0A0F18),
                                Color(0xFF05070B)
                            )
                        }
                )
            )
    ) {

        // =========================
        // MAIN LIGHT ORB
        // =========================

        Box(
            modifier = Modifier
                .size(700.dp)
                .offset(
                    x = drift.dp,
                    y = (-120).dp
                )
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            baseColor.copy(alpha = pulse),
                            baseColor.copy(alpha = reactiveAlpha),
                            Color.Transparent
                        )
                    ),

                    shape = CircleShape
                )
        )

        // =========================
        // SECONDARY ORB
        // =========================

        Box(
            modifier = Modifier
                .size(520.dp)
                .offset(
                    x = (-drift * 0.6f).dp,
                    y = 220.dp
                )
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.secondary.copy(
                                alpha =
                                    if (isLight) 0.05f else 0.09f
                            ),
                            Color.Transparent
                        )
                    ),

                    shape = CircleShape
                )
        )

        // =========================
        // TOP SHINE
        // =========================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isLight)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            else
                                Color.White.copy(alpha = 0.03f),

                            Color.Transparent
                        )
                    )
                )
        )

        // =========================
        // EDGE VIGNETTE
        // =========================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,

                            if (isLight)
                                Color.Black.copy(alpha = 0.05f)
                            else
                                Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
        )
    }
}


