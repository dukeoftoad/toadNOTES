package com.nestnote.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun BulkSelectionBar(
    selectedCount: Int,
    categories: List<String>,
    allPinned: Boolean,
    allArchived: Boolean,
    onMoveCategory: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onEncrypt: (() -> Unit)?,
    onDecrypt: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    var categoryMenu by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("$selectedCount selected", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, contentDescription = "Select all notes") }
            if (onEncrypt != null) IconButton(onClick = onEncrypt) { Icon(Icons.Default.Lock, contentDescription = "Encrypt selected notes") }
            if (onDecrypt != null) IconButton(onClick = onDecrypt) { Icon(Icons.Default.LockOpen, contentDescription = "Decrypt selected notes") }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { categoryMenu = true }) {
                    Icon(Icons.Default.Category, contentDescription = "Move selected notes to category")
                }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = { categoryMenu = false; onMoveCategory(category) }
                        )
                    }
                }
            }
            IconButton(onClick = onTogglePin) {
                Icon(Icons.Default.PushPin, contentDescription = if (allPinned) "Unpin selected notes" else "Pin selected notes")
            }
            IconButton(onClick = onToggleArchive) {
                Icon(if (allArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = if (allArchived) "Restore selected notes" else "Archive selected notes")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete selected notes", tint = colors.error)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear selection")
            }
        }
    }
}
