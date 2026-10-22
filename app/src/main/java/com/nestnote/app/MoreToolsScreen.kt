package com.nestnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class NoteTemplate(val title: String, val description: String, val body: String, val format: String = "markdown", val icon: ImageVector)

internal fun builtInNoteTemplates(): List<NoteTemplate> {
    val today = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    return listOf(
        NoteTemplate("Quick capture", "A clean inbox for an idea", "", "plain", Icons.Default.Lightbulb),
        NoteTemplate("Daily journal", "A gentle structure for today", "# $today\n\n## What matters today\n\n## Notes\n\n## One thing to remember\n", icon = Icons.Default.EventNote),
        NoteTemplate("Meeting notes", "People, decisions and next steps", "# Meeting\n\n**Date:** $today\n**People:** \n\n## Agenda\n- \n\n## Notes\n\n## Decisions\n- \n\n## Actions\n- [ ] \n", icon = Icons.Default.MeetingRoom),
        NoteTemplate("Project brief", "Turn a rough idea into a plan", "# Project\n\n## Goal\n\n## Why it matters\n\n## Next steps\n- [ ] \n\n## Open questions\n", icon = Icons.Default.Workspaces),
        NoteTemplate("Checklist", "A ready-to-fill task list", "# Checklist\n\n- [ ] First item\n- [ ] Second item\n- [ ] Third item\n", icon = Icons.Default.Checklist)
    )
}

@Composable
internal fun NoteTemplatesScreen(onUseTemplate: (NoteTemplate) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Start with a shape", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("A template is a starting point. Every section stays editable, and nothing is sent off-device.", color = colors.onSurfaceVariant)
        builtInNoteTemplates().forEach { template ->
            Card(onClick = { onUseTemplate(template) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow), border = BorderStroke(1.dp, colors.outlineVariant)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                        Icon(template.icon, null, Modifier.padding(11.dp).size(23.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(template.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Use ${template.title}", tint = colors.primary)
                }
            }
        }
    }
}

@Composable
internal fun NoteInsightsScreen(notes: List<NestNote>, onOpenCategory: (String) -> Unit, onOpenNote: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = remember(notes) { notes.filterNot { it.archived } }
    val pinned = remember(active) { active.count { it.pinned } }
    val withReminders = remember(active) { active.count { it.reminder.isNotBlank() } }
    val words = remember(notes) { notes.sumOf { NoteText.wordCount(it.body) } }
    val longest = remember(active) { active.maxByOrNull { NoteText.wordCount(it.body) } }
    val recentCutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val recentlyEdited = remember(notes) { notes.count { it.updated >= recentCutoff } }
    val rediscovery = remember(notes) { notes.filterNot { it.archived }.minByOrNull { it.updated } }
    val categoryCounts = remember(active) { active.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your notes, at a glance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("These insights are calculated on this device from your notes.", color = colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightTile("Active", active.size.toString(), Modifier.weight(1f))
            InsightTile("Archived", notes.count { it.archived }.toString(), Modifier.weight(1f))
            InsightTile("Pinned", pinned.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightTile("Words", words.toString(), Modifier.weight(1f))
            InsightTile("Reminders", withReminders.toString(), Modifier.weight(1f))
            InsightTile("Edited this week", recentlyEdited.toString(), Modifier.weight(1f))
        }
        MoreGroup("By category") {
            if (categoryCounts.isEmpty()) Text("Add notes to see your category mix.", Modifier.padding(16.dp), color = colors.onSurfaceVariant)
            else categoryCounts.forEachIndexed { index, entry -> MoreRow(Icons.Default.EditNote, entry.key, "${entry.value} active ${if (entry.value == 1) "note" else "notes"}", index > 0) {} }
        }
        longest?.let { note -> MoreGroup("Deepest note") { MoreRow(Icons.Default.Workspaces, note.title.ifBlank { "Untitled note" }, "${NoteText.wordCount(note.body)} words · tap to continue writing", false) { onOpenNote(note.id) } } }
        rediscovery?.let { note -> MoreGroup("Rediscover") { MoreRow(Icons.Default.Lightbulb, note.title.ifBlank { "Untitled note" }, "A note from ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.updated))}", false) { onOpenNote(note.id) } } }
    }
}

@Composable
private fun InsightTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = colors.surfaceContainerLow, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun KeyboardShortcutsScreen() {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Write at keyboard speed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Connect a hardware keyboard and use these shortcuts while editing a note.", color = colors.onSurfaceVariant)
        MoreGroup("Editing") {
            ShortcutRow("Undo", "Ctrl + Z")
            ShortcutRow("Redo", "Ctrl + Y")
            ShortcutRow("Redo alternative", "Ctrl + Shift + Z")
        }
        MoreGroup("Formatting") {
            Text("Open the formatting rail from the editor toolbar for headings, checklists, lists, quotes, emphasis, code and links. Formatting applies to the current line or selection.", Modifier.padding(16.dp), color = colors.onSurfaceVariant)
        }
        Text("Shortcut keys follow the Android hardware keyboard layout.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun ShortcutRow(action: String, shortcut: String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(action, style = MaterialTheme.typography.bodyLarge)
        Surface(shape = RoundedCornerShape(9.dp), color = colors.secondaryContainer) {
            Text(shortcut, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = colors.onSecondaryContainer, fontWeight = FontWeight.Medium)
        }
    }
}
