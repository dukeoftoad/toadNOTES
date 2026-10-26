package com.nestnote.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Empty-state copy and illustration selection for the current dashboard destination. */
@Composable
internal fun AppNotesEmptyState(
    search: String,
    selectedView: String,
    categoryOptions: List<String>,
    onClearSearch: () -> Unit
) {
    val categorySelected = categoryOptions.contains(selectedView)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NestEmptyIllustration(
            when {
                search.isNotBlank() -> Icons.Default.Search
                selectedView == "Archived" -> Icons.Default.Archive
                selectedView == "Pinned" -> Icons.Default.PushPin
                selectedView == "Reminders" -> Icons.Default.NotificationsNone
                else -> Icons.Default.Add
            }
        )
        Text(
            when {
                search.isNotBlank() -> "No matching notes"
                selectedView == "Archived" -> "Nothing archived yet"
                selectedView == "Pinned" -> "No pinned notes yet"
                selectedView == "Reminders" -> "No reminders yet"
                categorySelected -> "No $selectedView notes yet"
                else -> "A fresh page awaits"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            when {
                search.isNotBlank() -> "Try another search, or clear it to see all your notes."
                selectedView == "Archived" -> "Notes you archive will be kept here, away from your main lists."
                selectedView == "Pinned" -> "Pin important notes to keep them easy to find."
                selectedView == "Reminders" -> "Add a reminder to a note and it will show up here."
                categorySelected -> "Notes assigned to this category will appear here."
                else -> "Create a note and let ideas flow."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        if (search.isNotBlank()) TextButton(onClick = onClearSearch) { Text("Clear search") }
    }
}
