package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun pEQWebView(
    url: String,
    modifier: Modifier = Modifier
)
