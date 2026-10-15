package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.Tab
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

/**
 * High-fidelity Navigation components for pEQ Learn.
 */

@Composable
fun NavigationRailSection(viewModel: AppViewModel) {
    val navTabs = listOf(
        Tab.Dashboard,
        Tab.Learn,
        Tab.Mixer,
        Tab.Settings
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        navTabs.forEach { tab ->
            NavRailItem(
                tab = tab,
                isSelected = viewModel.currentTab.value == tab,
                onClick = { viewModel.currentTab.value = tab }
            )
        }                                        
    }
}

@Composable
private fun NavRailItem(tab: Tab, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else Color.Transparent,
        animationSpec = tween(300)
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(300)
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = getTabIcon(tab),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    current: Tab,
    viewModel: AppViewModel,
    onSelect: (Tab) -> Unit
) {
    val leftTabs = listOf(Tab.Dashboard, Tab.Learn)
    val rightTabs = listOf(Tab.Mixer, Tab.Settings)
    val isMiniPlayerVisible = viewModel.showMiniPlayer.value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Tabs (Dashboard, Learn)
            leftTabs.forEach { tab ->
                val isSelected = current == tab || (current == Tab.Player && tab == Tab.Dashboard)
                val iconColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getTabIcon(tab),
                        contentDescription = tab.label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = iconColor,
                        maxLines = 1
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(7.dp))
                    }
                }
            }

           // Center Cutout Button: Miniplayer Toggle
Column(
    modifier = Modifier
        .weight(0.9f)
        .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    Surface(
        onClick = { viewModel.showMiniPlayer.value = !viewModel.showMiniPlayer.value },
        shape = CircleShape,
        color = if (isMiniPlayerVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
        shadowElevation = 6.dp,
        modifier = Modifier.size(44.dp)
    ) {
        val currentItem = viewModel.currentMediaItem.value
        Box(contentAlignment = Alignment.Center) {
            if (currentItem != null) {
                pEQImage(
                    data = currentItem.thumbnail,
                    fallbackPath = currentItem.path,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isMiniPlayerVisible) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

            // Right Tabs (Mixer, Settings)
            rightTabs.forEach { tab ->
                val isSelected = current == tab
                val iconColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getTabIcon(tab),
                        contentDescription = tab.label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = iconColor,
                        maxLines = 1
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(7.dp))
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: Tab): ImageVector = when (tab) {
    Tab.Dashboard -> Icons.Default.Dashboard
    Tab.Learn -> Icons.Default.School
    Tab.Mixer -> Icons.Default.Tune
    Tab.Settings -> Icons.Default.Settings
    Tab.Player -> Icons.Default.GraphicEq
}
