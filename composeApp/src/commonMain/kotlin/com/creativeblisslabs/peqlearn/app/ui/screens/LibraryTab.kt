package com.creativeblisslabs.peqlearn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.peqlearn.app.data.MediaType
import com.creativeblisslabs.peqlearn.app.data.pEQMediaItem
import com.creativeblisslabs.peqlearn.app.ui.components.*
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.*

@Composable
private fun ModernLibraryChip(
    selected: Boolean, 
    icon: ImageVector, 
    text: String, 
    showDropdown: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            if (showDropdown) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun pEQDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
    ) {
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
            content = content
        )
    }
}

@Composable
private fun pEQDropdownItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { 
            Text(
                text, 
                fontSize = 12.sp, 
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ) 
        },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
        trailingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun EmptyLibraryState(isInsideFolder: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = if (isInsideFolder) Icons.Default.FolderOff else Icons.Default.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("NO MEDIA DETECTED", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(if (isInsideFolder) "This directory appears to be empty." else "Check your archive paths in settings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactMediaCard(item: pEQMediaItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(modifier = Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                pEQImage(
                    data = item.thumbnail,
                    fallbackPath = item.path,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    mediaType = item.type
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
