@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nestnote.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopBar(
    title: String,
    editorMode: EditorMode,
    pinned: Boolean,
    privateNote: Boolean,
    reminder: String,
    noteFormat: String,
    hasContent: Boolean,
    isExisting: Boolean,
    canDelete: Boolean,
    canDuplicate: Boolean,
    accent: Color,
    onClose: () -> Unit,
    onOpenToolRail: (ToolRailTab) -> Unit,
    onVoiceNote: () -> Unit,
    onTogglePinned: () -> Unit,
    onTogglePrivate: () -> Unit,
    onReminderChange: (String) -> Unit,
    onPickReminder: () -> Unit,
    onLock: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onShowQr: () -> Unit,
    onToggleFormat: () -> Unit,
    onExportMarkdown: (String) -> Unit,
    onCopy: () -> Unit,
    onRequestDelete: () -> Unit,
    onSave: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberSystemReducedMotion()
    val keyboardController = LocalSoftwareKeyboardController.current
    var moreMenu by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                if (title.isBlank()) "New note" else title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = { onClose() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close editor"
                )
            }
        },
        actions = {
            if (editorMode == EditorMode.Editing) {
                IconButton(
                    onClick = { onOpenToolRail(ToolRailTab.Colour) },
                    modifier = Modifier.testTag("editor_appearance_button")
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Choose note appearance",
                        tint = accent
                    )
                }
                IconButton(
                    onClick = { onOpenToolRail(ToolRailTab.Format) },
                    modifier = Modifier.testTag("editor_format_button")
                ) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = "Formatting options",
                        tint = colors.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { moreMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More editor actions",
                        tint = colors.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = moreMenu,
                    onDismissRequest = { moreMenu = false }
                ) {
                    if (editorMode == EditorMode.Editing) {
                        DropdownMenuItem(
                            text = { Text("Voice note") },
                            onClick = {
                                moreMenu = false
                                onVoiceNote()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Mic, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (privateNote) "Move out of Private Vault" else "Hide in Private Vault") },
                            onClick = { moreMenu = false; onTogglePrivate() },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (pinned) "Unpin note" else "Pin note") },
                            onClick = { moreMenu = false; onTogglePinned() },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PushPin,
                                    null,
                                    tint = if (pinned) accent else colors.onSurfaceVariant
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (reminder.isNotBlank()) "Change reminder"
                                    else "Add reminder"
                                )
                            },
                            onClick = { moreMenu = false; reminderMenu = true },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    null,
                                    tint = if (reminder.isNotBlank()) accent
                                    else colors.onSurfaceVariant
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (editorMode == EditorMode.Editing) "Read mode"
                                else "Edit note"
                            )
                        },
                        onClick = {
                            moreMenu = false
                            if (editorMode == EditorMode.Editing) {
                                keyboardController?.hide()
                                onLock()
                            } else {
                                onEdit()
                            }
                        },
                        leadingIcon = {
                            Icon(
                                if (editorMode == EditorMode.Editing) Icons.Default.Lock
                                else Icons.Default.Edit,
                                null
                            )
                        }
                    )
                    if (editorMode == EditorMode.Editing && isExisting) {
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = {
                                moreMenu = false
                                onArchive()
                            },
                            leadingIcon = { Icon(Icons.Default.Archive, null) }
                        )
                        // Duplicate — captures the current editor state,
                        // including unsaved edits, as a new note.
                        if (canDuplicate) {
                            DropdownMenuItem(
                                text = { Text("Duplicate note") },
                                onClick = {
                                    moreMenu = false
                                    onDuplicate()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                            )
                        }
                    }
                    DropdownMenuItem(
                        text = { Text("Share note") },
                        enabled = hasContent,
                        onClick = { moreMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Show QR code") },
                        enabled = hasContent,
                        onClick = { moreMenu = false; onShowQr() },
                        leadingIcon = { Icon(Icons.Default.QrCode2, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Note format · " + if (noteFormat == "markdown") "Markdown" else "Plain text") },
                        leadingIcon = { Icon(Icons.Default.FormatSize, contentDescription = null) },
                        onClick = {
                            moreMenu = false
                            onToggleFormat()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Convert to Markdown file") },
                        enabled = hasContent,
                        onClick = {
                            moreMenu = false
                            val safeTitle = title.trim().ifBlank { "note" }.replace(Regex("""[\\/:*?"<>|]"""), "_")
                            onExportMarkdown("$safeTitle.md")
                        },
                        leadingIcon = { Icon(Icons.Default.Download, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy text") },
                        enabled = hasContent,
                        onClick = { moreMenu = false; onCopy() },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                    )
                    if (editorMode == EditorMode.Editing && canDelete) {
                        DropdownMenuItem(
                            text = { Text("Delete note", color = colors.error) },
                            onClick = { moreMenu = false; onRequestDelete() },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = colors.error)
                            }
                        )
                    }
                }
                DropdownMenu(
                    expanded = reminderMenu,
                    onDismissRequest = { reminderMenu = false }
                ) {
                    val laterToday = NoteText.laterTodayOrNull(Calendar.getInstance())
                    if (laterToday != null) {
                        DropdownMenuItem(
                            text = { Text("Later today") },
                            onClick = {
                                reminderMenu = false
                                NoteText.laterTodayOrNull(Calendar.getInstance())?.let {
                                    onReminderChange(NoteText.formatReminder(it))
                                }
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Tomorrow morning") },
                        onClick = {
                            reminderMenu = false
                            onReminderChange(
                                    NoteText.formatReminder(NoteText.tomorrowMorning(Calendar.getInstance()))
                                )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Next Monday") },
                        onClick = {
                            reminderMenu = false
                            onReminderChange(
                                    NoteText.formatReminder(NoteText.nextMonday(Calendar.getInstance()))
                                )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Pick date and time…") },
                        onClick = { reminderMenu = false; onPickReminder() }
                    )
                    if (reminder.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("Remove reminder") },
                            onClick = { reminderMenu = false; onReminderChange("") }
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = editorMode,
                transitionSpec = {
                    if (reducedMotion) {
                        (fadeIn(snap()) + slideInVertically(snap()) { it / 4 }) togetherWith
                            (fadeOut(snap()) + slideOutVertically(snap()) { -it / 4 })
                    } else {
                        (fadeIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) { it / 4 }) togetherWith
                            (fadeOut(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + slideOutVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { -it / 4 })
                    }
                },
                label = "editor-save-done"
            ) { mode ->
                TextButton(
                    onClick = {
                        if (mode == EditorMode.Editing) {
                            if (hasContent) onSave()
                        } else {
                            onClose()
                        }
                    },
                    enabled = mode == EditorMode.Locked || hasContent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (mode == EditorMode.Editing) "Save" else "Done")
                    }
                }
            }
        }
    )
}

@Composable
internal fun EditorBottomBar(
    visible: Boolean,
    showCounts: Boolean,
    body: String,
    canUndo: Boolean,
    canRedo: Boolean,
    muted: Color,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val reducedMotion = rememberSystemReducedMotion()
    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) {
            fadeIn(snap()) + slideInVertically(snap()) { it }
        } else {
            fadeIn() + slideInVertically { it }
        },
        exit = if (reducedMotion) {
            fadeOut(snap()) + slideOutVertically(snap()) { it }
        } else {
            fadeOut() + slideOutVertically { it }
        }
    ) {
        Column(Modifier.imePadding()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showCounts) {
                    val words = remember(body) { NoteText.wordCount(body) }
                    val minutes = NoteText.readingMinutes(words)
                    Text(
                        buildString {
                            append(if (words == 1) "1 word" else "$words words")
                            append(" · ${body.length} characters")
                            if (minutes > 0) append(" · $minutes min read")
                        },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }
            }
        }
    }
}

@Composable
internal fun EditorTitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    editable: Boolean,
    focusRequester: FocusRequester,
    textSize: EditorTextSize,
    bodyTextSp: Int = textSize.bodySp,
    fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
    ink: Color,
    muted: Color
) {
    BasicTextField(
        value = title,
        onValueChange = { if (editable) onTitleChange(it) },
        readOnly = !editable,
        modifier = Modifier.fillMaxWidth()
            .focusRequester(focusRequester)
            .padding(vertical = 8.dp)
            .testTag("editor_title_field"),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontSize = (bodyTextSp * 1.65f).sp,
            lineHeight = (bodyTextSp * 2.05f).sp,
            fontWeight = FontWeight.Normal,
            fontFamily = fontFamily,
            color = ink
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        decorationBox = { innerTextField ->
            Box {
                if (title.isEmpty()) {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = (bodyTextSp * 1.65f).sp,
                            lineHeight = (bodyTextSp * 2.05f).sp,
                            fontFamily = fontFamily
                        ),
                        color = muted
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
internal fun EditorPinnedChip(accent: Color, editable: Boolean, onUnpin: () -> Unit) {
    AssistChip(
        onClick = { if (editable) onUnpin() },
        label = { Text("Pinned") },
        leadingIcon = {
            Icon(
                Icons.Default.PushPin, null, tint = accent,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = Modifier.padding(top = 4.dp).semantics {
            contentDescription = "Pinned. Tap to unpin"
        }
    )
}

@Composable
internal fun EditorChecklistCard(
    items: List<NoteText.ChecklistItem>,
    editable: Boolean,
    accent: Color,
    ink: Color,
    muted: Color,
    onItemChecked: (NoteText.ChecklistItem, Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "Checklist",
                style = MaterialTheme.typography.titleSmall,
                color = ink
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.checked,
                        modifier = Modifier.semantics {
                            contentDescription = buildString {
                                append(item.text.ifBlank { "Empty checklist item" })
                                append(", ")
                                append(if (item.checked) "completed" else "not completed")
                            }
                        },
                        enabled = editable,
                        onCheckedChange = { checked -> onItemChecked(item, checked) }
                    )
                    Text(
                        item.text.ifBlank { "(empty item)" },
                        modifier = Modifier.weight(1f).clickable(enabled = editable) {
                            onItemChecked(item, !item.checked)
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (item.checked) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            }
                        ),
                        color = if (item.checked) muted else ink
                    )
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.EditorBodySection(
    bodyValue: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    editable: Boolean,
    noteFormat: String,
    previewVisible: Boolean,
    onPreviewVisibleChange: (Boolean) -> Unit,
    editVoiceText: Boolean,
    onEditVoiceTextChange: (Boolean) -> Unit,
    voiceNoteId: Long,
    textSize: EditorTextSize,
    bodyTextSp: Int = textSize.bodySp,
    fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
    typeface: android.graphics.Typeface = android.graphics.Typeface.DEFAULT,
    ink: Color,
    muted: Color,
    visualTransformation: VisualTransformation,
    activePlayerId: String?,
    onPlayRequested: (String) -> Unit,
    onStopRequested: () -> Unit,
    onDeleteRequested: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val body = bodyValue.text
    // Reading mode renders the real inline voice players by default.
    // Markdown preview remains an optional editing aid, not a prerequisite
    // for playing attachments from a saved note.
    val hasInlineVoiceNotes = remember(body) {
        NoteText.voiceSegments(body).any { it.attachmentId != null }
    }
    if (editable && (noteFormat == "markdown" || hasInlineVoiceNotes)) {
        val reducedMotion = rememberSystemReducedMotion()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (hasInlineVoiceNotes) {
                FilterChip(
                    selected = editVoiceText,
                    onClick = { onEditVoiceTextChange(!editVoiceText) },
                    leadingIcon = {
                        Icon(
                            if (editVoiceText) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        AnimatedContent(
                            targetState = editVoiceText,
                            transitionSpec = {
                                if (reducedMotion) fadeIn(snap()) togetherWith fadeOut(snap())
                                else fadeIn() togetherWith fadeOut()
                            },
                            label = "voice-text-mode"
                        ) { editingText ->
                            Text(if (editingText) "Edit text" else "Play notes")
                        }
                    }
                )
            }
            if (noteFormat == "markdown") {
                FilterChip(
                    selected = previewVisible,
                    onClick = { onPreviewVisibleChange(!previewVisible) },
                    leadingIcon = {
                        Icon(
                            if (previewVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        AnimatedContent(
                            targetState = previewVisible,
                            transitionSpec = {
                                if (reducedMotion) fadeIn(snap()) togetherWith fadeOut(snap())
                                else fadeIn() togetherWith fadeOut()
                            },
                            label = "markdown-preview-mode"
                        ) { showingPreview ->
                            Text(if (showingPreview) "Edit Markdown" else "Preview")
                        }
                    }
                )
            }
        }
    }

    val showInlineVoicePreview = hasInlineVoiceNotes &&
        (!editable || !editVoiceText)
    if (showInlineVoicePreview ||
        (noteFormat == "markdown" && (previewVisible || !editable)) ||
        (!editable && noteFormat != "markdown")) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp).weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = colors.surface.copy(alpha = 0.82f),
            border = if (editable) {
                BorderStroke(1.dp, colors.outlineVariant)
            } else null
        ) {
            PreviewWithInlineVoice(
                body = body,
                noteId = voiceNoteId,
                textSize = textSize,
                bodyTextSp = bodyTextSp,
                typeface = typeface,
                ink = ink,
                activePlayerId = activePlayerId,
                onPlayRequested = onPlayRequested,
                onStopRequested = onStopRequested,
                onDeleteRequested = onDeleteRequested,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                renderMarkdown = noteFormat == "markdown"
            )
        }
        if (editable && noteFormat == "markdown") {
            Text(
                "Preview supports basic headings, emphasis, inline code, links, " +
                    "simple lists, and inline voice notes.",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
    } else {
        BasicTextField(
            value = bodyValue,
            onValueChange = {
                if (editable) onBodyChange(it)
            },
            readOnly = !editable,
            // Keep attachment IDs out of both editable and read-only
            // views. The stored body remains the canonical marker text.
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
                .padding(top = 3.dp)
                .testTag("editor_body_field"),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = bodyTextSp.sp,
                lineHeight = (bodyTextSp * 1.45f).sp,
                fontFamily = fontFamily,
                color = ink
            ),
            minLines = 1,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    if (bodyValue.text.isEmpty()) {
                        Text(
                            "Note",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textSize.bodySp.sp,
                                lineHeight = (textSize.bodySp * 1.45f).sp
                            ),
                            color = muted
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
internal fun EditorRelatedNotes(
    title: String,
    body: String,
    currentId: Long,
    notes: List<NestNote>,
    onOpen: (NestNote) -> Unit
) {
    val links = remember(body, notes, currentId) { resolveNoteLinks(body, notes, currentId) }
    val backlinks = remember(title, notes, currentId) { notesLinkingTo(title, notes, currentId) }
    val resolvedTitles = remember(links) { links.map { it.label.lowercase() }.toSet() }
    val missing = remember(body, resolvedTitles) { noteLinkTitles(body).filterNot { it.lowercase() in resolvedTitles } }
    if (links.isNotEmpty() || backlinks.isNotEmpty() || missing.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (links.isNotEmpty()) {
                Text("Linked notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    links.forEach { link ->
                        FilterChip(selected = false, onClick = { onOpen(link.note) }, label = { Text(link.label) }, leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        })
                    }
                }
            }
            if (backlinks.isNotEmpty()) {
                Text("Linked from", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    backlinks.forEach { source ->
                        FilterChip(selected = false, onClick = { onOpen(source) }, label = { Text(source.title.ifBlank { "Untitled note" }) })
                    }
                }
            }
            if (missing.isNotEmpty()) {
                Text("Missing notes: ${missing.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
internal fun EditorFooter(
    editable: Boolean,
    category: String,
    categoryOptions: List<String> = categories,
    onCategoryChange: (String) -> Unit,
    reminder: String,
    onPickReminder: () -> Unit,
    onClearReminder: () -> Unit,
    original: NestNote?,
    darkEditor: Boolean,
    accent: Color,
    ink: Color,
    muted: Color
) {
    val colors = MaterialTheme.colorScheme
    var categoryMenu by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        AssistChip(
            onClick = { if (editable) categoryMenu = true },
            label = { Text("Category · $category") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.testTag("category_chip")
        )
        DropdownMenu(
            expanded = categoryMenu,
            onDismissRequest = { categoryMenu = false }
        ) {
            categoryOptions.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = { onCategoryChange(item); categoryMenu = false },
                    trailingIcon = {
                        if (category == item) {
                            Icon(Icons.Default.Check, null, tint = accent)
                        }
                    }
                )
            }
        }
    }

    if (reminder.isNotBlank()) {
        Surface(
            onClick = onPickReminder,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceVariant.copy(
                alpha = if (darkEditor) 0.72f else 0.6f
            )
        ) {
            Row(
                Modifier.padding(
                    start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = accent)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        "Reminder · $reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink
                    )
                    Text(
                        "You'll be notified at this time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
                IconButton(onClick = onClearReminder) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear reminder",
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (original != null) {
        val editedLabel = remember(original.updated, original.created) {
            "Edited ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(original.updated))}" +
                " · Created " +
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(original.created))
        }
        Text(
            editedLabel,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = muted
        )
    }
}
