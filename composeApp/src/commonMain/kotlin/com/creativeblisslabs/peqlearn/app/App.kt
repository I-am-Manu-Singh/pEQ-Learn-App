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
import androidx.compose.runtime.setValue
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
import com.creativeblisslabs.peqlearn.app.ui.sections.NowPlayingBar

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

    val isAnyFullscreen = viewModel.isVideoFullScreen.value ||
            viewModel.isFullscreenKeyboard.value ||
            viewModel.isNotationFullscreen.value ||
            viewModel.isImmersiveLabActive.value ||
            viewModel.isTanpuraFullscreen.value

    pEQTheme(theme = theme, accent = accent) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(if (isAnyFullscreen) Modifier else Modifier.statusBarsPadding())
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
        // SOFT AMBIENT LIGHT (Primary Glow)
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
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
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
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
                text = "pEQ Learn",
        color = MaterialTheme.colorScheme.primary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Interactive Music Learning",
                                    color = MaterialTheme.colorScheme.onSurface
,                fontSize = 13.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(42.dp))

            // =========================
            // PROGRESS BAR (Primary Gradient)
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
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
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
                                    color = MaterialTheme.colorScheme.onSurface
,    fontSize = 11.sp,
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
                                    color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp
        )
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



        val isAnyFullscreen = viewModel.isVideoFullScreen.value ||
                viewModel.isFullscreenKeyboard.value ||
                viewModel.isNotationFullscreen.value ||
                viewModel.isImmersiveLabActive.value ||
                viewModel.isTanpuraFullscreen.value

        Scaffold(

            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),

            containerColor = Color.Black,

            contentWindowInsets = if (isAnyFullscreen) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,

            topBar = {},

            snackbarHost = {

                SnackbarHost(
                    snackbarHostState
                )
            },

            bottomBar = {

                if (!isWideScreen && !isAnyFullscreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    ) {
                        if (viewModel.showMiniPlayer.value && viewModel.currentMediaItem.value != null && viewModel.currentTab.value != Tab.Player) {
                            ThinMiniPlayer(viewModel = viewModel)
                        }
                        BottomNavigationBar(
                            current = currentTab,
                            viewModel = viewModel,
                            onSelect = {
                                viewModel.currentTab.value = it
                            }
                        )
                    }
                }
            }

        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                    // =========================
                    // NORMAL APP LAYOUT
                    // =========================

                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        // =========================
                        // TABLET / LANDSCAPE NAV
                        // =========================
                        val isAnyFullscreen = viewModel.isVideoFullScreen.value ||
                                viewModel.isFullscreenKeyboard.value ||
                                viewModel.isNotationFullscreen.value ||
                                viewModel.isImmersiveLabActive.value ||
                                viewModel.isTanpuraFullscreen.value

                        if (
                            isWideScreen && !isAnyFullscreen
                        ) {

                            Column(

                                modifier = Modifier
                                    .width(240.dp)
                                    .fillMaxHeight()
                                    .background(
                                        MaterialTheme.colorScheme.surface
                                    )
                                    .padding(16.dp),

                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                BoxWithConstraints {
                                    val isLandscape = maxWidth > maxHeight

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NowPlayingBar(
                                            viewModel = viewModel,
                                            isLandscape = isLandscape,
                                            modifier = Modifier.weight(1f) // Now valid because it's inside a Row!
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
                                    if (!isWideScreen && !isAnyFullscreen) {
                                        Modifier.padding(paddingValues)
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
                                        DashboardHome(viewModel)
                                    }

                                    Tab.Player -> {
                                        DashboardHome(viewModel)
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
                // PLAYLIST PICKER
                // =========================
                if (viewModel.isPlaylistPickerShown.value) {
                    PlaylistPicker(
                        viewModel = viewModel,
                        onDismiss = {
                            viewModel.isPlaylistPickerShown.value = false
                        }
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
