package com.nestnote.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
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
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
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

// Formatter shared by all visible cards; constructing SimpleDateFormat does locale-data
// lookup and allocation, so doing it inside composition ran dozens of times per frame.
private val cardDateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

@Composable
internal fun NoteCard(note: NestNote, compact: Boolean, colorStyle: NoteColorStyle, showPreview: Boolean, showTimestamp: Boolean, onOpen: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit, onArchive: () -> Unit, onSelect: () -> Unit = {}, isSelected: Boolean = false, onToggleEncryption: () -> Unit = {}, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val markdownLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write("# " + note.title.trim() + "\n\n" + note.body)
                } ?: error("Unable to open destination")
                android.widget.Toast.makeText(context, "Markdown note saved", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "Could not save Markdown file", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    // Dark-mode detection from the actual scheme so AMOLED and wallpaper palettes work too.
    val darkTheme = colors.background.luminance() < 0.5f
    // Wallpaper palettes can produce very bright onSurface/primary colors that
    // disappear on the fixed pastel note backgrounds. Use a stable high-contrast
    // foreground for pastel cards; adaptive cards continue to follow Material 3.
    val tint = if (colorStyle == NoteColorStyle.PASTEL) noteTint(note.color, darkTheme) else colors.surfaceContainerLow
    val tintIsDark = tint.luminance() < 0.5f
    val cardText = if (colorStyle == NoteColorStyle.PASTEL) {
        if (tintIsDark) Color(0xFFEAE3DA) else Color(0xFF292633)
    } else colors.onSurface
    val cardSecondary = if (colorStyle == NoteColorStyle.PASTEL) {
        if (tintIsDark) Color(0xFFB4AAA0) else Color(0xFF625D6B)
    } else colors.onSurfaceVariant
    val cardAccent = if (colorStyle == NoteColorStyle.PASTEL) {
        if (tintIsDark) colors.primary else Color(0xFF51466B)
    } else colors.primary
    val hasWallpaper = note.background > 0 || note.wallpaperUri.isNotBlank()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onOpen,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); menu = true }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (hasWallpaper) Color.Transparent else tint, contentColor = cardText),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.primary else colors.outlineVariant)
    ) {
        Box {
            if (note.wallpaperUri.isNotBlank()) {
                NoteCustomWallpaper(note.wallpaperUri, Modifier.matchParentSize())
            } else if (note.background > 0) {
                NoteWallpaperArtwork(note.background, Modifier.matchParentSize())
            }
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        if (hasWallpaper) {
                            // Gradient scrim keeps the top of the artwork visible while
                            // guaranteeing text contrast toward the bottom of the card.
                            Brush.verticalGradient(listOf(tint.copy(alpha = if (tintIsDark) 0.78f else 0.62f), tint.copy(alpha = if (tintIsDark) 0.92f else 0.82f)))
                        } else {
                            SolidColor(Color.Transparent)
                        }
                    )
                    .padding(if (compact) 10.dp else 16.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = colors.primary, modifier = Modifier.padding(end = 6.dp).size(20.dp))
                }
                // Tonal chip keyed to the card ink so it reads on every pastel + wallpaper.
                Surface(color = cardAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                    Text(note.category, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = cardText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(visible = note.pinned, enter = scaleIn(NestMotion.spatial()) + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned note", tint = cardAccent, modifier = Modifier.size(18.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, contentDescription = "Actions for " + note.title, tint = cardText) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (note.pinned) "Unpin note" else "Pin note") }, onClick = { menu = false; haptics.performHapticFeedback(HapticFeedbackType.LongPress); onPin() }, leadingIcon = { Icon(if (note.pinned) Icons.Outlined.PushPin else Icons.Default.PushPin, null) })
                        DropdownMenuItem(
                            text = { Text("Download as Markdown") },
                            onClick = {
                                menu = false
                                val safeTitle = note.title.trim().ifBlank { "note" }.replace(Regex("""[\\/:*?"<>|]"""), "_")
                                markdownLauncher.launch("$safeTitle.md")
                            },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            modifier = Modifier.testTag("note_export_markdown")
                        )
                        DropdownMenuItem(text = { Text(if (isSelected) "Remove from selection" else "Select note") }, onClick = { menu = false; onSelect() }, leadingIcon = { Icon(Icons.Default.CheckCircleOutline, null) })
                        DropdownMenuItem(text = { Text(if (note.encrypted) "Decrypt note" else "Encrypt note") }, onClick = { menu = false; onToggleEncryption() }, leadingIcon = { Icon(if (note.encrypted) Icons.Default.LockOpen else Icons.Default.Lock, null) }, modifier = Modifier.testTag("note_toggle_encryption"))
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; if (!note.encrypted) onOpen() }, enabled = !note.encrypted, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text(if (note.archived) "Restore from archive" else "Archive") }, onClick = { menu = false; onArchive() }, leadingIcon = { Icon(if (note.archived) Icons.Default.Unarchive else Icons.Default.Archive, null) })
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                            colors = MenuDefaults.itemColors(textColor = colors.error, leadingIconColor = colors.error)
                        )
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
            // Parsed once per note body, not on every dashboard recomposition.
            val (checklistTotal, checklistDone) = remember(note.body) { NoteText.checklistProgress(note.body) }
            if (checklistTotal > 0) {
                val reducedMotion = rememberSystemReducedMotion()
                val progress by animateFloatAsState(
                    checklistDone.toFloat() / checklistTotal,
                    NestMotion.spatial(reducedMotion),
                    label = "checklist-progress"
                )
                Column(
                    Modifier.padding(top = 8.dp).semantics {
                        contentDescription = "Checklist progress: $checklistDone of $checklistTotal items complete"
                    },
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            if (checklistDone == checklistTotal) Icons.Default.Check else Icons.Default.CheckBox,
                            contentDescription = null, tint = cardAccent, modifier = Modifier.size(14.dp)
                        )
                        Text("$checklistDone/$checklistTotal", style = MaterialTheme.typography.labelSmall, color = cardAccent, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(cardAccent.copy(alpha = 0.18f))) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(cardAccent))
                    }
                }
            }
            if (showTimestamp) {
            Spacer(Modifier.height(if (compact) 6.dp else 14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (note.reminder.isNotBlank()) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = cardAccent, modifier = Modifier.size(13.dp))
                }
                Text(
                    if (note.reminder.isNotBlank()) note.reminder else cardDateFormatter.format(Date(note.updated)),
                    style = MaterialTheme.typography.labelSmall, color = if (note.reminder.isNotBlank()) cardAccent else cardSecondary
                )
            }
            }
            }
        }
    }
}
