package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
expect fun pEQImage(
    data: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    mediaType: com.creativeblisslabs.peqlearn.app.data.MediaType? = null,
    fallbackPath: String? = null
)
