package com.nestnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The More tab: library, data and preferences in three calm groups. */
@Composable
internal fun MoreScreen(
    pinnedCount: Int,
    archivedCount: Int,
    categoryCount: Int,
    onOpen: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MoreGroup("Library") {
            MoreRow(Icons.Default.PushPin, "Pinned notes", "$pinnedCount important notes at a glance", false) { onOpen("Pinned") }
            MoreRow(Icons.Default.Archive, "Archived notes", "$archivedCount notes kept out of your main lists", true) { onOpen("Archived") }
            MoreRow(Icons.Default.Category, "Categories", "$categoryCount categories", true) { onOpen("Categories") }
        }
        MoreGroup("Data") {
            MoreRow(Icons.Default.Backup, "Backup & restore", "Save a JSON backup or merge-restore one", false) { onOpen("Settings") }
        }
        MoreGroup("Preferences") {
            MoreRow(Icons.Default.Settings, "Settings", "Appearance, layout, editor and note behaviour", false) { onOpen("Settings") }
        }
    }
}

@Composable
private fun MoreGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 4.dp)
    )
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) { Column(content = content) }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, divider: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (divider) HorizontalDivider(color = colors.outlineVariant)
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(shape = RoundedCornerShape(13.dp), color = colors.primary.copy(alpha = 0.12f), contentColor = colors.primary) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(9.dp).size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
