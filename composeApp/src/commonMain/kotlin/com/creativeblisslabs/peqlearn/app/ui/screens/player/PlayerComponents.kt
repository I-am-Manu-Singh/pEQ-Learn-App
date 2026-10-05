package com.creativeblisslabs.peqlearn.app.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import com.creativeblisslabs.peqlearn.app.ui.components.pEQDesign
import com.creativeblisslabs.peqlearn.app.ui.components.InfoChip
import androidx.compose.ui.text.style.TextAlign
import com.creativeblisslabs.peqlearn.app.data.MediaType
import com.creativeblisslabs.peqlearn.app.data.pEQMediaItem
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.CircleShape
import kotlinx.datetime.toLocalDateTime

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    active: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (active) activeColor.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = if (active) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun MiniControlButton(
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
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
fun DualPointControlConsole(
    title: String,
    startLabel: String,
    endLabel: String,
    startTime: Long,
    endTime: Long,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onCloseClick: () -> Unit,
    mainActionIcon: ImageVector,
    mainActionLabel: String,
    mainActionEnabled: Boolean,
    onMainActionClick: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                FilledTonalIconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PointButton(
                    label = startLabel,
                    time = startTime,
                    modifier = Modifier.weight(1f),
                    onClick = onStartClick
                )

                PointButton(
                    label = endLabel,
                    time = endTime,
                    modifier = Modifier.weight(1f),
                    onClick = onEndClick
                )

                Button(
                    onClick = onMainActionClick,
                    enabled = mainActionEnabled,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(mainActionIcon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(mainActionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PointButton(
    label: String,
    time: Long,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isActive = time != -1L
    val containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(0.9f) else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = contentColor)
            Text(if (isActive) formatTime(time) else "--:--", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (isActive) contentColor else MaterialTheme.colorScheme.onSurface)
        }
    }
}

fun formatTime(ms: Long?, forceHours: Boolean = false): String {
    if (ms == null || ms < 0) return "--:--"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0 || forceHours) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

fun formatDate(timestamp: Long): String {
    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth}/${dateTime.monthNumber}/${dateTime.year}"
}

@Composable
fun pEQGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(pEQDesign.GlassColor, RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun pEQEditorContainer(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.42f)
    ) {
        Surface(
            modifier = modifier.fillMaxWidth().fillMaxHeight(0.75f),
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp), content = content)
        }
    }
}

@Composable
private fun DiagnosticItem(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun VolumeControlSlider(
    viewModel: AppViewModel
) {
    val volume = viewModel.masterVolume.value
    val volumeIcon = when {
        volume > 0.5f -> Icons.AutoMirrored.Filled.VolumeUp
        volume > 0f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeMute
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = volumeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        Slider(
            value = volume,
            onValueChange = { viewModel.setMasterVolume(it) },
            valueRange = 0f..2.0f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
            )
        )
    }
}

@Composable
fun DiagnosticsOverlay(viewModel: AppViewModel) {
    AnimatedVisibility(
        visible = viewModel.showDiagnostics.value,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = Modifier.fillMaxSize().padding(24.dp).zIndex(100f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.92f),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Troubleshoot, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "pEQ NEURAL DIAGNOSTICS",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                DiagnosticItemDetailed("SIGNAL CHAIN", if (viewModel.isMasterBypassActive.value) "BYPASSED" else "ACTIVE", if (viewModel.isMasterBypassActive.value) MaterialTheme.colorScheme.error else Color.Green)
                DiagnosticItemDetailed("SAMPLE RATE", "${viewModel.currentMediaItem.value?.sampleRate ?: 44100} Hz", MaterialTheme.colorScheme.primary)
                DiagnosticItemDetailed("BIT DEPTH", "${viewModel.currentMediaItem.value?.bitDepth ?: 16} bit", MaterialTheme.colorScheme.primary)
                DiagnosticItemDetailed("LATENCY", "24.2 ms", Color.Cyan)
                DiagnosticItemDetailed("BUFFER SIZE", "4096 samples", MaterialTheme.colorScheme.onSurface)
                DiagnosticItemDetailed("ENGINE STATE", if (viewModel.isPlaying.value) "STREAMING" else "IDLE", if (viewModel.isPlaying.value) Color.Green else Color.Yellow)
                DiagnosticItemDetailed("OUTPUT DEVICE", viewModel.currentDevice.value.deviceName, MaterialTheme.colorScheme.onSurface)
                DiagnosticItemDetailed("PROCESSING LOAD", "8.4%", Color.Magenta)

                Spacer(Modifier.weight(1f))
                
                Button(
                    onClick = { viewModel.showDiagnostics.value = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("EXIT TELEMETRY CONSOLE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItemDetailed(label: String, value: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransmissionTelemetry(viewModel: AppViewModel, item: pEQMediaItem) {
    val sampleRate = if (viewModel.currentSampleRate.value > 0) "${viewModel.currentSampleRate.value / 1000}kHz" else null
    val bitDepth = if (viewModel.currentBitDepth.value > 0) "${viewModel.currentBitDepth.value}bit" else null
    val codec = viewModel.currentCodec.value.takeIf { it.isNotBlank() }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (item.type == MediaType.AUDIO) {
            if (item.isHiRes || viewModel.currentSampleRate.value >= 88200) InfoChip("HI-RES", MaterialTheme.colorScheme.primary)
            if (item.isLossless) InfoChip("LOSSLESS", MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            codec?.let { InfoChip(it, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) }
            listOfNotNull(sampleRate, bitDepth).takeIf { it.isNotEmpty() }?.let { InfoChip(it.joinToString(" • "), MaterialTheme.colorScheme.surfaceContainerHighest) }
            if (item.channels > 0) InfoChip(when (item.channels) { 1 -> "MONO"; 2 -> "STEREO"; else -> "${item.channels} CH" }, MaterialTheme.colorScheme.surfaceContainerHighest)
        } else {
            item.resolution?.takeIf { it.isNotBlank() }?.let { InfoChip(it, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) }
            codec?.let { InfoChip(it, MaterialTheme.colorScheme.surfaceContainerHighest) }
            if (item.frameRate > 0f) InfoChip("${item.frameRate.toInt()} FPS", MaterialTheme.colorScheme.surfaceContainerHighest)
        }
        if (item.bitrate > 0) InfoChip("${item.bitrate} kbps", MaterialTheme.colorScheme.surfaceContainerHighest)
    }
}
