package com.creativeblisslabs.peqlearn.app.ui.components
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings
import com.creativeblisslabs.peqlearn.app.util.TimeUtils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.data.MediaChapter

/**
 * pEQ CHAPTER CONSOLE: TEMPORAL NAVIGATOR Provides a tactile list of media segments for high-speed
 * seeking.
 */
@Composable
fun ChapterConsole(
        chapters: List<MediaChapter>,
        currentPositionMs: Long,
        onChapterClick: (MediaChapter) -> Unit
) {
    LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                    pEQStrings.SEGMENT_TIMELINE,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0, 229, 255),
                    modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(chapters) { chapter ->
            val isActive = currentPositionMs in chapter.startMs..chapter.endMs

            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { onChapterClick(chapter) }
                                    .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Badge
                Text(
                        text = TimeUtils.formatMs(chapter.startMs),
                        fontSize = 11.sp,
                        color = if (isActive) Color(0, 229, 255) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(60.dp)
                )

                // Chapter Title
                Text(
                        text = chapter.title,
                        fontSize = 15.sp,
                        color = if (isActive) Color.White else Color.Gray,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
