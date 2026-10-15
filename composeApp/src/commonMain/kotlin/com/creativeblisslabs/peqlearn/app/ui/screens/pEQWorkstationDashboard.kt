package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.LocalAppViewModel
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

@Composable
fun pEQWorkstationDashboard(viewModel: AppViewModel = LocalAppViewModel.current) {
    val selectedTab by viewModel.dashboardSubTab
    val tabs = listOf("HOME")

    Column(modifier = Modifier.fillMaxSize()) {
        // Integrated Workstation Tab Navigation
        pEQTabs(
            tabs = tabs,
            selectedTab = selectedTab,
            viewModel = viewModel,
            onTabSelected = { viewModel.dashboardSubTab.value = it }
        )

        // Global Search Bar (Visible in certain tabs)
        if (selectedTab in listOf(1, 2, 3)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = viewModel.globalSearchQuery.value,
                        onValueChange = { viewModel.globalSearchQuery.value = it },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true
                    )
                    if (viewModel.globalSearchQuery.value.isNotEmpty()) {
                        IconButton(onClick = { viewModel.globalSearchQuery.value = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Tab Content with Animated Transitions
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            modifier = Modifier.fillMaxSize(),
            label = "dashboard_tabs"
        ) { target ->
            when (target) {
                0 -> DashboardHome(viewModel)
            }
        }

   
    }
}
