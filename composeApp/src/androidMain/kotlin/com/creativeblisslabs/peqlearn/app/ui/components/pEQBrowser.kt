package com.creativeblisslabs.peqlearn.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun pEQBrowser(
    url: String,
    onClose: () -> Unit,
    onStreamCaptured: (String) -> Unit
) {
    var currentUrl by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var webView: WebView? by remember { mutableStateOf(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Browser Toolbar
            TopAppBar(
                title = { 
                    Text(
                        text = currentUrl.substringAfter("://").substringBefore("/"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                actions = {
                    if (canGoBack) {
                        IconButton(onClick = { webView?.goBack() }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    }
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    canGoBack = view?.canGoBack() ?: false
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val urlString = request?.url?.toString() ?: ""
                                    // Capture video streams (mp4, m3u8, etc.)
                                    if (urlString.contains(".mp4") || urlString.contains(".m3u8") || urlString.contains("videoplayback")) {
                                        onStreamCaptured(urlString)
                                        // Kill browser playback instantly
                                        view?.post {
                                            view.evaluateJavascript("(function(){ var medias = document.querySelectorAll('video, audio'); for(var i=0; i<medias.length; i++){ medias[i].pause(); } })();", null)
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    if (newProgress == 100) isLoading = false
                                }
                            }

                            loadUrl(url)
                            webView = this
                        }
                    }
                )
            }
        }
    }
}
