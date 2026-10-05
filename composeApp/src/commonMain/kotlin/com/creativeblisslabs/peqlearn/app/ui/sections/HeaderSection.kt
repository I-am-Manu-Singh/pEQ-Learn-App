@file:OptIn(ExperimentalMaterial3Api::class)

package com.creativeblisslabs.peqlearn.app.ui.sections

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.creativeblisslabs.peqlearn.app.data.Device
import com.creativeblisslabs.peqlearn.app.data.MediaType
import com.creativeblisslabs.peqlearn.app.Tab
import com.creativeblisslabs.peqlearn.app.auroraBorder
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.theme.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.painterResource
import peq_learn.composeapp.generated.resources.Res
import peq_learn.composeapp.generated.resources.app_icon

@Composable
fun HeaderSection(
    viewModel: AppViewModel
) {

    var showAudioPanel by remember {
        mutableStateOf(false)
    }

    var showDeviceSheet by remember {
        mutableStateOf(false)
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val nowPlaying =
        viewModel.currentMediaItem.value

    BoxWithConstraints {

        val isLandscape =
            maxWidth > maxHeight

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        if (isLandscape) 10.dp
                        else 8.dp,

                    vertical =
                        if (isLandscape) 4.dp
                        else 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(
                    if (isLandscape) 6.dp
                    else 10.dp
                )
        ) {
            IconButton(
                onClick = { viewModel.showMiniPlayer.value = !viewModel.showMiniPlayer.value },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AppShortcut,
                    contentDescription = "Widget Command",
                    tint = if (viewModel.showMiniPlayer.value) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // =========================
            // PLAYER CARD
            // =========================

            Surface(
                onClick = {

                    if (nowPlaying != null) {

                        viewModel.currentTab.value =
                            Tab.Player

                        if (
                            nowPlaying.type ==
                            MediaType.VIDEO
                        ) {

                            viewModel.isVideoFullScreen.value =
                                true
                        }
                    }
                },

                modifier = Modifier
                    .weight(1f)
                    .heightIn(
                        min =
                            if (isLandscape)
                                54.dp
                            else
                                64.dp,

                        max =
                            if (isLandscape)
                                62.dp
                            else
                                74.dp
                    )
                    .auroraBorder(),

                shape = RoundedCornerShape(
                    if (isLandscape)
                        22.dp
                    else
                        28.dp
                ),

                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerHighest
                        .copy(alpha = 0.45f)
            ) {

                if (nowPlaying != null) {

                    Column {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        if (isLandscape)
                                            10.dp
                                        else
                                            12.dp,

                                    vertical =
                                        if (isLandscape)
                                            6.dp
                                        else
                                            10.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            // =========================
                            // THUMBNAIL
                            // =========================

                            Box(
                                modifier = Modifier
                                    .size(
                                        if (isLandscape)
                                            38.dp
                                        else
                                            46.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            if (isLandscape)
                                                10.dp
                                            else
                                                14.dp
                                        )
                                    )
                                    .background(Color.Black)
                            ) {

                                pEQImage(
                                    data =
                                        nowPlaying.thumbnail,
                                    fallbackPath =
                                        nowPlaying.path,

                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(0.75f),

                                    contentScale =
                                        ContentScale.Crop,
                                    
                                    mediaType = nowPlaying.type
                                )
                            }

                            Spacer(
                                Modifier.width(
                                    if (isLandscape)
                                        8.dp
                                    else
                                        10.dp
                                )
                            )

                            // =========================
                            // INFO
                            // =========================

                            Column(
                                modifier =
                                    Modifier.weight(1f),

                                verticalArrangement =
                                    Arrangement.spacedBy(1.dp)
                            ) {

                                Text(
                                    text =
                                        nowPlaying.title,

                                    fontSize =
                                        if (isLandscape)
                                            10.sp
                                        else
                                            11.sp,

                                    lineHeight =
                                        if (isLandscape)
                                            12.sp
                                        else
                                            14.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    maxLines = 1,

                                    overflow =
                                        TextOverflow.Ellipsis
                                )

                                Text(
                                    text =
                                        nowPlaying.subtitle,

                                    fontSize =
                                        if (isLandscape)
                                            8.sp
                                        else
                                            9.sp,

                                    lineHeight =
                                        if (isLandscape)
                                            9.sp
                                        else
                                            10.sp,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,

                                    maxLines = 1,

                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }

                            Spacer(
                                Modifier.width(
                                    if (isLandscape)
                                        2.dp
                                    else
                                        6.dp
                                )
                            )

                            // =========================
                            // CONTROLS
                            // =========================

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        if (isLandscape)
                                            1.dp
                                        else
                                            3.dp
                                    )
                            ) {

                                FilledTonalIconButton(
                                    onClick = {
                                        viewModel.previousTrack()
                                    },

                                    modifier = Modifier.size(
                                        if (isLandscape)
                                            20.dp
                                        else
                                            24.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.SkipPrevious,

                                        contentDescription = null,

                                        modifier = Modifier.size(
                                            if (isLandscape)
                                                13.dp
                                            else
                                                16.dp
                                        )
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        viewModel.togglePlayback()
                                    },

                                    modifier = Modifier.size(
                                        if (isLandscape)
                                            28.dp
                                        else
                                            34.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector =
                                            if (viewModel.isPlaying.value)
                                                Icons.Default.Pause
                                            else
                                                Icons.Default.PlayArrow,

                                        contentDescription = null,

                                        modifier = Modifier.size(
                                            if (isLandscape)
                                                15.dp
                                            else
                                                18.dp
                                        )
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        viewModel.nextTrack()
                                    },

                                    modifier = Modifier.size(
                                        if (isLandscape)
                                            20.dp
                                        else
                                            24.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.SkipNext,

                                        contentDescription = null,

                                        modifier = Modifier.size(
                                            if (isLandscape)
                                                13.dp
                                            else
                                                16.dp
                                        )
                                    )
                                }
                            }
                        }

                        val progress =
                            (
                                    viewModel.currentPosition.value
                                        .toFloat() /

                                            viewModel.currentDuration.value
                                                .coerceAtLeast(1L)
                                                .toFloat()

                                    ).coerceIn(0f, 1f)

                        LinearProgressIndicator(
                            progress = { progress },

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    if (isLandscape)
                                        2.dp
                                    else
                                        3.dp
                                ),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            trackColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(alpha = 0.10f)
                        )
                    }

                } else {

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal =
                                    if (isLandscape)
                                        10.dp
                                    else
                                        14.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Surface(
    shape = RoundedCornerShape(if (isLandscape) 10.dp else 14.dp),
    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
    modifier = Modifier.size(if (isLandscape) 38.dp else 46.dp)
) {
    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = null,
            // Change size to fillMaxSize to stretch to the Surface boundaries
            modifier = Modifier.fillMaxSize(), 
            contentScale = ContentScale.Crop
        )
    }
}


                        Spacer(
                            Modifier.width(
                                if (isLandscape)
                                    8.dp
                                else
                                    12.dp
                            )
                        )

                        Column {

                            Text(
                                text = "pEQ Media Player",

                                fontSize =
                                    if (isLandscape)
                                        11.sp
                                    else
                                        13.sp,

                                fontWeight =
                                    FontWeight.Black,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )

                            Text(
                                text =
                                    "No active playback",

                                fontSize =
                                    if (isLandscape)
                                        8.sp
                                    else
                                        9.sp,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // =========================
            // RIGHT CONTROLS
            // =========================

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        if (isLandscape)
                            6.dp
                        else
                            10.dp
                    ),

                verticalAlignment =
                    Alignment.Top,

                modifier = Modifier.padding(end = 2.dp)
            ) {

                // =========================
                // DEVICE
                // =========================

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.spacedBy(3.dp),

                    modifier = Modifier.widthIn(
                        min =
                            if (isLandscape)
                                36.dp
                            else
                                44.dp,

                        max =
                            if (isLandscape)
                                48.dp
                            else
                                58.dp
                    )
                ) {

                    FilledTonalIconButton(
                        onClick = {
                            showDeviceSheet = true
                        },

                        modifier = Modifier.size(
                            if (isLandscape)
                                30.dp
                            else
                                36.dp
                        )
                    ) {

                        Icon(
                            imageVector =
                                viewModel.currentDevice.value.icon,

                            contentDescription = null,

                            tint =
                                viewModel.currentDevice.value.color,

                            modifier = Modifier.size(
                                if (isLandscape)
                                    15.dp
                                else
                                    18.dp
                            )
                        )
                    }

                    Text(
                        text =
                            viewModel.currentDevice.value.deviceName,

                        fontSize =
                            if (isLandscape)
                                6.sp
                            else
                                7.sp,

                        lineHeight =
                            if (isLandscape)
                                7.sp
                            else
                                8.sp,

                        fontWeight =
                            FontWeight.Bold,

                        textAlign =
                            TextAlign.Center,

                        maxLines = 2,

                        overflow =
                            TextOverflow.Ellipsis,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
// =========================
// VOLUME
// =========================

                Box {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {

        val isMuted =
            viewModel.masterVolume.value <= 0f

        FilledTonalIconButton(
            onClick = {
                showAudioPanel = !showAudioPanel
            },

            modifier = Modifier.size(
                if (isLandscape)
                    30.dp
                else
                    36.dp
            ),

            colors =
    IconButtonDefaults.filledTonalIconButtonColors(

        containerColor =
            if (isMuted)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.secondaryContainer,

        contentColor =
            if (isMuted)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSecondaryContainer
    )
        ) {

            Icon(
                imageVector =
                    when {

                        viewModel.masterVolume.value > 0.5f ->
                            Icons.AutoMirrored.Filled.VolumeUp

                        viewModel.masterVolume.value > 0f ->
                            Icons.AutoMirrored.Filled.VolumeDown

                        else ->
                            Icons.AutoMirrored.Filled.VolumeOff
                    },

                contentDescription = null,

                modifier = Modifier.size(
                    if (isLandscape)
                        15.dp
                    else
                        18.dp
                )
            )
        }

        Text(
            text =
                "${(viewModel.masterVolume.value * 100).toInt()}%",

            modifier = Modifier.padding(4.dp),

            fontSize =
                if (isLandscape)
                    9.sp
                else
                    9.sp,

            lineHeight =
                if (isLandscape)
                    5.sp
                else
                    6.sp,

            fontWeight = FontWeight.Bold,

            color =
                if (isMuted)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
        )
    }

    // =========================
    // FLOATING PANEL
    // =========================

    Popup(
        alignment = Alignment.TopEnd,

        offset = IntOffset(
            x =
                if (isLandscape)
                    -120
                else
                    -160,

            y =
                if (isLandscape)
                    55
                else
                    70
        ),

        onDismissRequest = {
            showAudioPanel = false
        }
    ) {

        androidx.compose.animation.AnimatedVisibility(
            visible = showAudioPanel,

            enter =
                fadeIn() + scaleIn(
                    initialScale = 0.92f
                ),

            exit =
                fadeOut() + scaleOut(
                    targetScale = 0.92f
                )
        ) {

            Surface(
                shape = RoundedCornerShape(26.dp),

                tonalElevation = 20.dp,

                shadowElevation = 30.dp,

                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerHighest
                        .copy(alpha = 0.96f),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme
                        .colorScheme
                        .outline
                        .copy(alpha = 0.08f)
                )
            ) {

                VolumeControlSlider(
                    viewModel = viewModel,

                    modifier = Modifier
                        .width(
                            if (isLandscape)
                                200.dp
                            else
                                250.dp
                        )
                        .padding(10.dp)
                )
                
            }
        }
    }
}
            }
        }

        // =========================
        // DEVICE SHEET
        // =========================

        if (showDeviceSheet) {

            ModalBottomSheet(
                onDismissRequest = {

                    showDeviceSheet = false

                },

                sheetState = sheetState
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp,0.dp,20.dp,8.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = pEQStrings.SELECT_DEVICE,

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Bold
                    )

                    Device.entries.forEach { dev ->

                        val selected =
                            dev == viewModel.currentDevice.value

                        val isConnected =
                            viewModel.connectedDevices.contains(dev)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(
                                    if (isConnected)
                                        1f
                                    else
                                        0.45f
                                )
                                .clickable(
                                    enabled = isConnected
                                ) {

                                    viewModel.updateDevice(dev)

                                    showDeviceSheet = false
                                },

                            shape = RoundedCornerShape(18.dp),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceContainerHighest
                                    .copy(alpha = 0.45f),

                            border = BorderStroke(
                                1.dp,

                                if (selected)
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                        .copy(alpha = 0.25f)

                                else
                                    MaterialTheme
                                        .colorScheme
                                        .outline
                                        .copy(alpha = 0.08f)
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Surface(
                                    shape =
                                        RoundedCornerShape(14.dp),

                                    color =
                                        if (selected)
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                                .copy(alpha = 0.15f)

                                        else
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant,

                                    modifier = Modifier.size(42.dp)
                                ) {

                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector = dev.icon,

                                            contentDescription = null,

                                            tint =

                                                if (selected)
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary

                                                else if (isConnected)
                                                    dev.color

                                                else
                                                    Color.Gray,

                                            modifier =
                                                Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(
                                    Modifier.width(10.dp)
                                )

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = dev.deviceName,

                                        fontSize = 13.sp,

                                        fontWeight = FontWeight.Bold,

                                        color =

                                            if (isConnected)
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface

                                            else
                                                Color.Gray
                                    )

                                    Spacer(
                                        Modifier.height(2.dp)
                                    )

                                    Text(
                                        text =

                                            if (isConnected)
                                                "${dev.chip} • ${dev.mode}"

                                            else
                                                "Device not detected",

                                        fontSize = 10.sp,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                if (selected) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.CheckCircle,

                                        contentDescription = null,

                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================
        // ARCHIVE SHEET
        // =========================

        if (viewModel.showFolderManager.value) {

            PEQArchiveRegistryDialog(
                onDismiss = {

                    viewModel.showFolderManager.value =
                        false
                }
            )
        }
    }
}