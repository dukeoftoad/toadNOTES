package com.nestnote.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsBehaviorSection(
    categoryOptions: List<String>,
    defaultNoteCategory: String,
    onDefaultNoteCategoryChange: (String) -> Unit,
    categoryDraft: String,
    onCategoryDraftChange: (String) -> Unit,
    onCategoryOptionsChange: (List<String>) -> Unit,
    selectedSort: String,
    onSortChange: (String) -> Unit,
    showNotePreviews: Boolean,
    onShowNotePreviewsChange: (Boolean) -> Unit,
    showNoteTimestamps: Boolean,
    onShowNoteTimestampsChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SettingsSection(
        title = "Notes & search",
        subtitle = "Defaults for finding and organizing notes",
        icon = Icons.Default.Search
    ) {
        SettingLabel("Default category for new notes")
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryOptions.forEach { category ->
                val selected = defaultNoteCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onDefaultNoteCategoryChange(category) },
                    label = { Text(category) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    
        SettingLabel("Manage categories")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = categoryDraft,
                onValueChange = { onCategoryDraftChange(it.take(24)) },
                modifier = Modifier.weight(1f).testTag("new_category_field"),
                label = { Text("New category") },
                singleLine = true
            )
            FilledIconButton(
                onClick = {
                    val clean = categoryDraft.trim()
                    if (clean.isNotBlank() && categoryOptions.none { it.equals(clean, ignoreCase = true) } && categoryOptions.size < 24) {
                        onCategoryOptionsChange(categoryOptions + clean)
                        onCategoryDraftChange("")
                    }
                },
                enabled = categoryDraft.isNotBlank() && categoryOptions.size < 24,
                modifier = Modifier.testTag("add_category_button")
            ) { Icon(Icons.Default.Check, contentDescription = "Add category") }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryOptions.forEach { category ->
                AssistChip(
                    onClick = { onDefaultNoteCategoryChange(category) },
                    label = { Text(category) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (categoryOptions.size > 1) onCategoryOptionsChange(categoryOptions - category)
                            },
                            enabled = categoryOptions.size > 1,
                            modifier = Modifier.size(28.dp).testTag("remove_category_${category.lowercase().replace(' ', '_')}")
                        ) { Icon(Icons.Default.Close, contentDescription = "Remove $category", modifier = Modifier.size(16.dp)) }
                    }
                )
            }
        }
        Text("Removing a category moves its notes to the first remaining category.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SettingLabel("Sort notes by")
        SegmentedChoice(
            SortOptions.map { it.first },
            selectedSort,
            { key -> SortOptions.first { it.first == key }.second },
            onSortChange
        )
        Text(
            "Pinned notes always stay on top.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SwitchRow(
            title = "Show note previews",
            summary = "Display the first lines of each note on its card",
            checked = showNotePreviews,
            onChange = onShowNotePreviewsChange
        )
    
        SwitchRow(
            title = "Show dates and reminders",
            summary = "Display the edit date or reminder on each card",
            checked = showNoteTimestamps,
            onChange = onShowNoteTimestampsChange
        )
    }
}
