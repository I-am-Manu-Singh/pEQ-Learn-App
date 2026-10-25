package com.creativeblisslabs.peqlearn.app.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.text.style.TextAlign
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.creativeblisslabs.peqlearn.app.ui.screens.Score
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

private val scoreJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun OpenSheetMusicDisplayViewer(
    score: Score?,
    playheadMeasure: Int,
    playheadOffset: Float,
    isDarkMode: Boolean,
    zoom: Float,
    modifier: Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(if (isDarkMode) Color(0xFF0C0F12) else Color.White)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(if (isDarkMode) 0xFF0C0F12.toInt() else 0xFFFFFFFF.toInt())
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        displayZoomControls = false
                        builtInZoomControls = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoaded = true
                        }
                    }

                    loadUrl("file:///android_asset/osmd/osmd_viewer.html")
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            }
        )

        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.destroy()
                webViewRef = null
            }
        }

        // Render score whenever score changes or page finishes loading
        LaunchedEffect(score, isPageLoaded) {
            val wv = webViewRef
            if (wv != null && isPageLoaded) {
                if (score != null) {
                    try {
                        val encodedJson = scoreJson.encodeToString(score)
                        val base64 = Base64.encodeToString(
                            encodedJson.toByteArray(StandardCharsets.UTF_8),
                            Base64.NO_WRAP
                        )
                        wv.evaluateJavascript("window.drawScoreBase64 && window.drawScoreBase64('$base64');", null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    wv.evaluateJavascript("window.clearScore && window.clearScore();", null)
                }
            }
        }

        // Update playhead position during playback
        LaunchedEffect(playheadMeasure, playheadOffset, isPageLoaded) {
            val wv = webViewRef
            if (wv != null && isPageLoaded) {
                wv.evaluateJavascript("window.setPlayhead && window.setPlayhead($playheadMeasure, $playheadOffset);", null)
            }
        }

        // Update theme
        LaunchedEffect(isDarkMode, isPageLoaded) {
            val wv = webViewRef
            if (wv != null && isPageLoaded) {
                wv.evaluateJavascript("window.setTheme && window.setTheme($isDarkMode);", null)
            }
        }

        // Update zoom
        LaunchedEffect(zoom, isPageLoaded) {
            val wv = webViewRef
            if (wv != null && isPageLoaded) {
                wv.evaluateJavascript("window.setZoom && window.setZoom($zoom);", null)
            }
        }
    }
}
