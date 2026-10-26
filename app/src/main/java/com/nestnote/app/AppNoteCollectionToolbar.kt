package com.nestnote.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag

/** Presentation of note-collection actions; app preferences and navigation stay parent-owned. */
@Composable
internal fun AppNoteCollectionToolbar(
    selectedView: String,
    dark: Boolean,
    sortByTitle: Boolean,
    sortByCreated: Boolean,
    dashboardLayout: DashboardLayout,
    canEncryptAll: Boolean,
    onImportMarkdown: () -> Unit,
    onThemeToggle: () -> Unit,
    onSortChange: (String) -> Unit,
    onEncryptAll: () -> Unit,
    onLayoutToggle: () -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (selectedView == "Home") "Your notes" else "Notes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (selectedView == "Home") {
            IconButton(
                onClick = onImportMarkdown,
                modifier = Modifier.testTag("import_markdown_button")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = "Import Markdown file")
            }
        }
        IconButton(onClick = onThemeToggle) {
            Icon(
                if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme"
            )
        }
        androidx.compose.foundation.layout.Box {
            TextButton(onClick = { sortMenuExpanded = true }) {
                Text(
                    when {
                        sortByTitle -> "Title A–Z"
                        sortByCreated -> "Recently created"
                        else -> "Recently edited"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Recently edited") },
                    onClick = { onSortChange("RECENT"); sortMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Recently created") },
                    onClick = { onSortChange("CREATED"); sortMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Title A–Z") },
                    onClick = { onSortChange("TITLE"); sortMenuExpanded = false }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Encrypt all notes…") },
                    onClick = { sortMenuExpanded = false; onEncryptAll() },
                    enabled = canEncryptAll
                )
            }
        }
        IconButton(onClick = onLayoutToggle) {
            Icon(
                if (dashboardLayout == DashboardLayout.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = "Switch dashboard layout"
            )
        }
    }
}
