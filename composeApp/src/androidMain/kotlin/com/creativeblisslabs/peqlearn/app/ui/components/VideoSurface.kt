package com.creativeblisslabs.peqlearn.app.ui.components

import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.common.C
import com.creativeblisslabs.peqlearn.app.LocalAppViewModel
import com.creativeblisslabs.peqlearn.app.audio.AudioEngineProvider

@OptIn(UnstableApi::class)
@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale
) {
    val viewModel = LocalAppViewModel.current
    val zoom by viewModel.videoZoom

    var exoPlayer by remember {
        mutableStateOf<ExoPlayer?>(null)
    }

    AndroidView(
        modifier = modifier
            .onSizeChanged {
                // Prevents 0x0 layout race conditions
            },
        factory = { context ->
            exoPlayer = AudioEngineProvider.getExoPlayer(context)

            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                controllerAutoShow = false
                keepScreenOn = true
                setKeepContentOnPlayerReset(true)
                setBackgroundColor(Color.BLACK)
                setShutterBackgroundColor(Color.BLACK)
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                alpha = 0.99f
                clipToOutline = false
                clipChildren = false
                clipToPadding = false

                // 🎬 Cinema-Grade Subtitle Styling
                val captionStyle = androidx.media3.ui.CaptionStyleCompat(
                    Color.WHITE,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    Color.BLACK,
                    null
                )
                subtitleView?.setApplyEmbeddedStyles(false)
                subtitleView?.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                subtitleView?.setStyle(captionStyle)

                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (view !is PlayerView) return@AndroidView

            exoPlayer?.let { player ->
                if (view.player != player) {
                    view.player = player
                }
            }

            // Map ContentScale to ExoPlayer ResizeMode
            view.resizeMode = when (contentScale) {
                androidx.compose.ui.layout.ContentScale.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                androidx.compose.ui.layout.ContentScale.FillBounds -> AspectRatioFrameLayout.RESIZE_MODE_FILL // Stretch
                androidx.compose.ui.layout.ContentScale.Crop -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill/Crop
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }

            view.scaleX = zoom
            view.scaleY = zoom
        }
    )
}