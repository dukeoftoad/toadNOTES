package com.nestnote.app

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.viewmodel.compose.viewModel


// Dashboard note card.

@Composable
internal fun NoteCard(note: NestNote, compact: Boolean, colorStyle: NoteColorStyle, showPreview: Boolean, showTimestamp: Boolean, onOpen: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit, onArchive: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    // Wallpaper palettes can produce very bright onSurface/primary colors that
    // disappear on the fixed pastel note backgrounds. Use a stable high-contrast
    // foreground for pastel cards; adaptive cards continue to follow Material 3.
    val tint = if (colorStyle == NoteColorStyle.PASTEL) noteTints[note.color.coerceIn(0, noteTints.lastIndex)] else colors.surfaceContainerLow
    val cardText = if (colorStyle == NoteColorStyle.PASTEL) Color(0xFF292633) else colors.onSurface
    val cardSecondary = if (colorStyle == NoteColorStyle.PASTEL) Color(0xFF625D6B) else colors.onSurfaceVariant
    val cardAccent = if (colorStyle == NoteColorStyle.PASTEL) Color(0xFF51466B) else colors.primary
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (note.background > 0) Color.Transparent else tint, contentColor = cardText),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box {
            if (note.background > 0) {
                NoteWallpaperArtwork(note.background, Modifier.matchParentSize())
            }
            Column(
                Modifier.fillMaxWidth()
                    .background(if (note.background > 0) noteTints[note.color.coerceIn(0, noteTints.lastIndex)].copy(alpha = 0.68f) else Color.Transparent)
                    .padding(if (compact) 10.dp else 16.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tint, shape = RoundedCornerShape(8.dp)) {
                    Text(note.category, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = cardText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                if (note.pinned) Icon(Icons.Default.PushPin, contentDescription = "Pinned note", tint = cardAccent, modifier = Modifier.size(18.dp))
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, contentDescription = "Actions for " + note.title, tint = cardText) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (note.pinned) "Unpin note" else "Pin note") }, onClick = { menu = false; onPin() }, leadingIcon = { Icon(Icons.Default.PushPin, null) })
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onOpen() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text(if (note.archived) "Restore from archive" else "Archive") }, onClick = { menu = false; onArchive() }, leadingIcon = { Icon(if (note.archived) Icons.Default.Home else Icons.Default.Delete, null) })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            if (showPreview && note.body.isNotBlank()) {
                Text(
                    note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardSecondary,
                    maxLines = if (compact) 2 else 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 7.dp).weight(1f, fill = false)
                )
            }
            // Progress remains visible even when note previews are disabled.
            val (checklistTotal, checklistDone) = NoteText.checklistProgress(note.body)
            if (checklistTotal > 0) {
                Text(
                    "Checklist · $checklistDone/$checklistTotal complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = cardAccent,
                    modifier = Modifier.padding(top = 6.dp).semantics {
                        contentDescription = "Checklist progress: $checklistDone of $checklistTotal items complete"
                    }
                )
            }
            if (showTimestamp) {
            Spacer(Modifier.height(if (compact) 6.dp else 14.dp))
            Text(
                if (note.reminder.isNotBlank()) "◷ ${note.reminder}" else SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updated)),
                style = MaterialTheme.typography.labelSmall, color = cardSecondary
            )
            }
            }
        }
    }
}
