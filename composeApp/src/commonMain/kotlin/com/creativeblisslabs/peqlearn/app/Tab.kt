package com.creativeblisslabs.peqlearn.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.creativeblisslabs.peqlearn.app.ui.theme.pEQStrings

enum class Tab(val label: String, val icon: ImageVector) {
    Dashboard("DASHBOARD", Icons.Default.Dashboard),
    Learn("LEARN", Icons.Default.School),
    Mixer(pEQStrings.NEURAL_MIXER, Icons.Default.Tune),
    Settings(pEQStrings.SETTINGS_LABEL, Icons.Default.Settings),
    Player("NOW PLAYING", Icons.Default.GraphicEq)
}
