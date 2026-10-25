package com.nestnote.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** Dashboard heading and display controls; navigation, preferences, and note mutations stay in the app root. */
@Composable
internal fun AppDashboardHeader(
    selectedView: String,
    notes: List<NestNote>,
    filteredCount: Int,
    dark: Boolean,
    layout: DashboardLayout,
    sortByTitle: Boolean,
    sortByCreated: Boolean,
    canEncryptAll: Boolean,
    onCreateNote: () -> Unit,
    onImportMarkdown: () -> Unit,
    onToggleTheme: () -> Unit,
    onSetSort: (String) -> Unit,
    onEncryptAll: () -> Unit,
    onToggleLayout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedView == "Home") {
            Text(
                remember { NoteText.greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) },
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = colors.primaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface.copy(alpha = 0.72f)) {
                        Icon(Icons.Default.Edit, contentDescription = null,
                            modifier = Modifier.padding(12.dp).size(26.dp), tint = colors.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("A little space for big ideas", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(
                            "${notes.count { !it.privateNote && !it.archived }} active notes · ${notes.count { it.pinned && !it.privateNote && !it.archived }} pinned",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCreateNote) {
                        Icon(Icons.Default.Add, contentDescription = "Create a note", tint = colors.primary)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when (selectedView) {
                        "All notes" -> "All your notes"
                        "Pinned" -> "Pinned notes"
                        "Reminders" -> "Things to remember"
                        "Archived" -> "Archived notes"
                        else -> selectedView
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("$filteredCount notes", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (selectedView == "Home") "Your notes" else "Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (selectedView == "Home") {
                IconButton(onClick = onImportMarkdown, modifier = Modifier.testTag("import_markdown_button")) {
                    Icon(Icons.Default.FileOpen, contentDescription = "Import Markdown file")
                }
            }
            IconButton(onClick = onToggleTheme) {
                Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme")
            }
            Box {
                TextButton(onClick = { sortMenuExpanded = true }) {
                    Text(when { sortByTitle -> "Title A–Z"; sortByCreated -> "Recently created"; else -> "Recently edited" },
                        style = MaterialTheme.typography.labelLarge)
                }
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Recently edited") }, onClick = { onSetSort("RECENT"); sortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Recently created") }, onClick = { onSetSort("CREATED"); sortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Title A–Z") }, onClick = { onSetSort("TITLE"); sortMenuExpanded = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Encrypt all notes…") },
                        onClick = { sortMenuExpanded = false; onEncryptAll() }, enabled = canEncryptAll)
                }
            }
            IconButton(onClick = onToggleLayout) {
                Icon(if (layout == DashboardLayout.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                    contentDescription = "Switch dashboard layout")
            }
        }
    }
}
