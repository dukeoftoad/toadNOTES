package com.nestnote.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Search input, saved-query chips, and category filters; search and navigation state stay parent-owned. */
@Composable
internal fun AppDashboardFilters(
    searchVisible: Boolean,
    search: String,
    savedSearches: List<String>,
    selectedView: String,
    categories: List<String>,
    onSearchChange: (String) -> Unit,
    onSaveSearch: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onRemoveSavedSearch: (String) -> Unit,
    onSelectView: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    if (searchVisible) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag("note_search_field"),
            placeholder = { Text("Search · is:pinned · has:voice · after:YYYY-MM-DD") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (search.isNotBlank()) {
                        val query = search.trim()
                        IconButton(
                            onClick = { onSaveSearch(query) },
                            enabled = query in savedSearches || savedSearches.size < 12
                        ) { Icon(Icons.Default.BookmarkAdd, contentDescription = "Save search") }
                    }
                    IconButton(onClick = onCloseSearch) {
                        Icon(Icons.Default.Close, contentDescription = "Close search field")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = colors.surfaceContainerLow,
                focusedContainerColor = colors.surfaceContainerLow,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = colors.primary
            )
        )
    }
    if (savedSearches.isNotEmpty() && (selectedView == "Home" || selectedView == "All notes")) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            savedSearches.forEach { query ->
                FilterChip(
                    selected = search == query,
                    onClick = { onSearchChange(query) },
                    label = { Text(query, maxLines = 1) },
                    trailingIcon = {
                        IconButton(onClick = { onRemoveSavedSearch(query) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove saved search", modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
    if (selectedView == "Home" || selectedView == "All notes" || categories.contains(selectedView)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedView == "Home" || selectedView == "All notes",
                onClick = { onSelectView("Home") },
                label = { Text("All notes") },
                leadingIcon = if (selectedView == "Home" || selectedView == "All notes") {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
            categories.forEach { category ->
                FilterChip(selected = selectedView == category, onClick = { onSelectView(category) }, label = { Text(category) })
            }
        }
    }
}
