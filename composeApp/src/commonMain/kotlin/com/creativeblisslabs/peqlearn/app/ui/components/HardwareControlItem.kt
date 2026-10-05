package com.creativeblisslabs.peqlearn.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HardwareControlItem(
    label: String,
    value: String = "",
    desc: String = "",
    enabled: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onToggle != null) {
            Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors =
                            SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor =
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
            )
        } else {
            Text(
                    value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
