package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.data.SyncedLine

/**
 * pEQ LYRICS CONSOLE: SYNCED SCROLLING UI Implements high-fidelity temporal tracking with smooth
 * transitions.
 */
@Composable
fun SyncedLyricsView(
        lyrics: List<SyncedLine>,
        currentPositionMs: Long,
        onLineClick: (SyncedLine) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val activeIndex = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }

    // Auto-scroll logic: Keep the active line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -200)
        }
    }

    LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val alpha by animateFloatAsState(if (isActive) 1f else 0.4f)
            val color by animateColorAsState(if (isActive) Color(0, 229, 255) else Color.White)
            val fontSize by animateFloatAsState(if (isActive) 22f else 18f)

            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 24.dp)
                                    .clickable {
                                        onLineClick(line)
                                    }, // T-05: Lyric-to-Seek Handshake
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                        text = line.text,
                        fontSize = fontSize.sp,
                        fontWeight = if (isActive) FontWeight.Black else FontWeight.Medium,
                        color = color,
                        modifier = Modifier.alpha(alpha),
                        lineHeight = 28.sp
                )
            }
        }
    }
}
