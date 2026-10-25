package com.nestnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Category browsing presentation; destination navigation remains owned by NestNoteApp. */
@Composable
internal fun AppCategoriesScreen(
    notes: List<NestNote>,
    categories: List<String>,
    onSelectCategory: (String) -> Unit
) {
    val activeCategoryCounts = remember(notes, categories) {
        categories.associateWith { category ->
            notes.count { it.category == category && !it.archived && !it.privateNote }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Categories", style = MaterialTheme.typography.headlineSmall)
        categories.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { category ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelectCategory(category) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(category, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${activeCategoryCounts[category] ?: 0} active notes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
