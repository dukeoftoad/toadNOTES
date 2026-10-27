package com.nestnote.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
    // Header statistics are derived once per note-list change; counting inline recomputed
    // two full-list scans on every header recomposition (sort toggles, menu state, theme).
    val activeNotes = remember(notes) { notes.count { !it.privateNote && !it.archived } }
    val pinnedNotes = remember(notes) { notes.count { it.pinned && !it.privateNote && !it.archived } }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedView == "Home") {
            Text(
                remember { NoteText.greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) },
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = colors.primaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.surface.copy(alpha = 0.72f)) {
                        Icon(Icons.Default.Edit, contentDescription = null,
                            modifier = Modifier.padding(12.dp).size(26.dp), tint = colors.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("A little space for big ideas", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatPill(label = "$activeNotes active")
                            StatPill(icon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(11.dp), tint = colors.onPrimaryContainer) }, label = "$pinnedNotes pinned")
                        }
                    }
                    FilledTonalIconButton(
                        onClick = onCreateNote,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create a note")
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
                FilledTonalIconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.surfaceContainerHigh,
                        contentColor = colors.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Sort and organise"
                    )
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

/** Small rounded pill used for hero-card statistics. */
@Composable
private fun StatPill(label: String, icon: (@Composable () -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface.copy(alpha = 0.42f)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) icon()
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
