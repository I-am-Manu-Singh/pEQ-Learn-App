package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Universal cross-platform PDF Sheet Music Document Viewer.
 * Renders actual pages of PDF files with smooth page-by-page scrolling,
 * pinch-to-zoom, and high-fidelity rendering.
 */
@Composable
expect fun PdfDocumentViewer(
    uriString: String,
    modifier: Modifier = Modifier
)
