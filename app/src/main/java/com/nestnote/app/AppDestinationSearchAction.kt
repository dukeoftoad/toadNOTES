package com.nestnote.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Search affordance for the primary note destinations. Search state remains parent-owned. */
@Composable
internal fun AppDestinationSearchAction(searchVisible: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onToggle,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(if (searchVisible) colors.primaryContainer else colors.surfaceContainerHigh)
            .testTag("home_search_icon")
    ) {
        Icon(
            if (searchVisible) Icons.Default.Close else Icons.Default.Search,
            contentDescription = if (searchVisible) "Close search" else "Search notes",
            tint = if (searchVisible) colors.onPrimaryContainer else colors.onSurface
        )
    }
}
