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
import androidx.compose.ui.text.style.TextOverflow
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
        Tab.Library,
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
    val isMiniPlayerVisible = viewModel.showMiniPlayer.value
    val currentItem = viewModel.currentMediaItem.value

    val leftTabs = listOf(
        Tab.Dashboard,
        Tab.Learn
    )

    val rightTabs = listOf(
        Tab.Mixer,
        Tab.Settings
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 25.dp)
    ) {

        // =====================================
        // MAIN NAVIGATION BAR
        // =====================================

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),

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

                verticalAlignment = Alignment.CenterVertically
            ) {

                // LEFT TABS

                leftTabs.forEach { tab ->

                    val selected =
                        current == tab ||
                                (current == Tab.Player && tab == Tab.Dashboard)

                    BottomNavItem(
                        tab = tab,
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(tab) }
                    )
                }

                // CENTER LIBRARY TAB

                BottomNavItem(
                    tab = Tab.Library,
                    selected = current == Tab.Library,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(Tab.Library) }
                )

                // RIGHT TABS

                rightTabs.forEach { tab ->

                    BottomNavItem(
                        tab = tab,
                        selected = current == tab,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(tab) }
                    )
                }
            }
        }



        // SMALL UP / DOWN ARROW
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-9).dp)
                .size(20.dp)
                .clickable {
                    viewModel.showMiniPlayer.value =
                        !viewModel.showMiniPlayer.value
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMiniPlayerVisible)
                    Icons.Default.KeyboardArrowDown
                else
                    Icons.Default.KeyboardArrowUp,
                contentDescription = "Toggle Mini Player",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }


    }
}


// =========================================
// REUSABLE NAVIGATION ITEM
// =========================================

@Composable
private fun BottomNavItem(
    tab: Tab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val iconColor =
        if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = getTabIcon(tab),
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier.size(23.dp)
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected)
                FontWeight.Bold
            else
                FontWeight.Medium,
            color = iconColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 3.dp)
                .clip(CircleShape)
                .background(
                    if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Transparent
                )
        )
    }
}
private fun getTabIcon(tab: Tab): ImageVector = when (tab) {
    Tab.Dashboard -> Icons.Default.Dashboard
    Tab.Learn -> Icons.Default.School
    Tab.Mixer -> Icons.Default.Tune
    Tab.Settings -> Icons.Default.Settings
    Tab.Player -> Icons.Default.GraphicEq
    Tab.Library -> Icons.Default.LibraryMusic
}
