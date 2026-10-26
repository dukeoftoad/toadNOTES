package com.nestnote.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Displays the same note cards in grid or list mode; note mutations remain parent-owned. */
@Composable
internal fun AppNoteCollection(
    notes: List<NestNote>,
    layout: DashboardLayout,
    compact: Boolean,
    colorStyle: NoteColorStyle,
    showPreview: Boolean,
    showTimestamp: Boolean,
    selectedNoteIds: Set<Long>,
    onOpen: (NestNote) -> Unit,
    onSelect: (NestNote) -> Unit,
    onPin: (NestNote) -> Unit,
    onDelete: (NestNote) -> Unit,
    onArchive: (NestNote) -> Unit,
    onToggleEncryption: (NestNote) -> Unit
) {
    if (layout == DashboardLayout.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 155.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            gridItems(notes, key = { it.id }) { note ->
                NoteCard(
                    note,
                    compact = compact,
                    colorStyle = colorStyle,
                    showPreview = showPreview,
                    showTimestamp = showTimestamp,
                    onOpen = { onOpen(note) },
                    onSelect = { onSelect(note) },
                    isSelected = note.id in selectedNoteIds,
                    onPin = { if (!note.encrypted) onPin(note) },
                    onDelete = { onDelete(note) },
                    onArchive = { onArchive(note) },
                    onToggleEncryption = { onToggleEncryption(note) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note,
                    compact = compact,
                    colorStyle = colorStyle,
                    showPreview = showPreview,
                    showTimestamp = showTimestamp,
                    onOpen = { onOpen(note) },
                    onSelect = { onSelect(note) },
                    isSelected = note.id in selectedNoteIds,
                    onPin = { onPin(note) },
                    onDelete = { onDelete(note) },
                    onArchive = { onArchive(note) },
                    onToggleEncryption = { onToggleEncryption(note) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}
