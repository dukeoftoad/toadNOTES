package com.nestnote.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal fun collapsibleSlotHeight(measuredHeight: Int, fraction: Float): Int =
    (measuredHeight * fraction).roundToInt().coerceAtLeast(0)

/**
 * Unified create/edit note editor.
 *
 * Voice notes are anchored inline via `[[voice:<attachmentId>]]` markers in the
 * body text (see NoteText.voiceMarkers). In edit mode the marker is visible as
 * plain text at the cursor position where the user recorded. In preview mode
 * each marker is rendered as an [InlineVoicePlayer] card between text blocks.
 *
 * Deletion policy: when the user removes a voice note from the inline preview,
 * the marker is stripped from the body immediately but the encrypted file and
 * manifest entry are queued for deletion. The queue is flushed only when the
 * user saves, so discarding after a delete leaves the original note fully intact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteEditorContent(
    original: NestNote?,
    draftNoteId: Long,
    defaultNoteCategory: String,
    textSize: EditorTextSize,
    showCounts: Boolean,
    onSave: (NestNote) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onUnsavedChangesChange: (Boolean) -> Unit = {},
    onRegisterVoiceDiscard: (() -> Unit) -> Unit = {},
    fullScreen: Boolean = false,
    compactDialog: Boolean = false,
    modifier: Modifier = Modifier
) {
    // ---- Core editor state ----
    var title by rememberSaveable(original?.id) { mutableStateOf(original?.title.orEmpty()) }
    var bodyValue by rememberSaveable(original?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(original?.body.orEmpty()))
    }
    val body = bodyValue.text
    val markdownBodySize = textSize.bodySp.toFloat()
    var category by rememberSaveable(original?.id) {
        mutableStateOf(original?.category ?: defaultNoteCategory)
    }
    var colorIndex by rememberSaveable(original?.id) {
        mutableIntStateOf(original?.color?.coerceIn(0, noteAccents.lastIndex) ?: 0)
    }
    var backgroundIndex by rememberSaveable(original?.id) {
        mutableIntStateOf(original?.background?.coerceIn(0, 5) ?: 0)
    }
    var reminder by rememberSaveable(original?.id) { mutableStateOf(original?.reminder.orEmpty()) }
    var pinned by rememberSaveable(original?.id) { mutableStateOf(original?.pinned ?: false) }
    var toolRailVisible by remember { mutableStateOf(false) }
    var toolRailTab by remember { mutableStateOf(ToolRailTab.Colour) }
    var categoryMenu by remember { mutableStateOf(false) }
    var previewVisible by rememberSaveable(original?.id) { mutableStateOf(false) }
    var appearanceStartColor by remember { mutableIntStateOf(colorIndex) }
    var appearanceStartBackground by remember { mutableIntStateOf(backgroundIndex) }
    var discardConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val editorModeViewModel: EditorModeViewModel =
        viewModel(key = "editor-mode-${original?.id ?: draftNoteId}")
    val editorMode = editorModeViewModel.mode
    var voiceChangesPending by rememberSaveable(original?.id, draftNoteId) {
        mutableStateOf(false)
    }
    val voiceNoteId = original?.id ?: draftNoteId
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberSystemReducedMotion()
    val darkEditor = colors.surface.red < 0.5f
    val ink = if (darkEditor) colors.onSurface else Color(0xFF292633)
    val muted = if (darkEditor) colors.onSurfaceVariant else Color(0xFF66616D)
    val accent = if (darkEditor) colors.primary else Color(0xFF514783)
    val editorCanvas = if (backgroundIndex == 0) {
        if (darkEditor) colors.surface else noteTints[colorIndex]
    } else {
        colors.surface
    }

    // ---- Voice playback + deletion coordination ----
    var activeVoicePlayerId by remember { mutableStateOf<String?>(null) }
    // Pending deletion queue: marker removed immediately, file flushed on save.
    var pendingVoiceDeletion by remember { mutableStateOf<String?>(null) }
    var pendingVoiceDeletions by remember { mutableStateOf<Set<String>>(emptySet()) }

    val hasUnsavedChanges = title != original?.title.orEmpty() ||
        body != original?.body.orEmpty() ||
        category != (original?.category ?: defaultNoteCategory) ||
        colorIndex != (original?.color ?: 0) ||
        backgroundIndex != (original?.background ?: 0) ||
        pinned != (original?.pinned ?: false) ||
        reminder != original?.reminder.orEmpty() ||
        voiceChangesPending

    fun requestDismiss() {
        if (hasUnsavedChanges) discardConfirmation = true else onDismiss()
    }

    fun buildNote(archived: Boolean = original?.archived ?: false): NestNote {
        val now = System.currentTimeMillis()
        return NestNote(
            id = original?.id ?: draftNoteId,
            title = title.trim(),
            body = body.trim(),
            category = category,
            color = colorIndex,
            pinned = if (archived) false else pinned,
            reminder = reminder,
            updated = original?.updated ?: 0L,
            created = original?.created ?: now,
            archived = archived,
            background = backgroundIndex
        )
    }

    fun pickReminder() {
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val picked = Calendar.getInstance().apply { set(year, month, day, hour, minute, 0) }
                reminder = NoteText.formatReminder(picked)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun onBodyChange(next: TextFieldValue) {
        val continued = if (next.selection.collapsed) {
            NoteText.continueList(bodyValue.text, next.text, next.selection.start)
        } else null
        bodyValue = if (continued != null) {
            TextFieldValue(continued.first, TextRange(continued.second))
        } else next
    }

    fun applyLineEdit(transform: (String, Int) -> Pair<String, Int>) {
        val oldValue = bodyValue
        val (text, cursor) = transform(oldValue.text, oldValue.selection.start)
        val (selectionStart, selectionEnd) = NoteText.selectionAfterLineEdit(
            oldText = oldValue.text,
            newText = text,
            selectionStart = oldValue.selection.start,
            selectionEnd = oldValue.selection.end,
            newCursor = cursor
        )
        bodyValue = TextFieldValue(text, TextRange(selectionStart, selectionEnd))
    }

    fun applyMarkdownLink() {
        val oldValue = bodyValue
        val (text, selectionStart, selectionEnd) = NoteText.insertMarkdownLink(
            oldValue.text, oldValue.selection.start, oldValue.selection.end
        )
        bodyValue = TextFieldValue(text, TextRange(selectionStart, selectionEnd))
    }

    fun applyInlineEdit(marker: String) {
        val oldValue = bodyValue
        val (text, selectionStart, selectionEnd) = NoteText.toggleInlineFormat(
            oldValue.text, oldValue.selection.start, oldValue.selection.end, marker
        )
        bodyValue = TextFieldValue(text, TextRange(selectionStart, selectionEnd))
    }

    fun shareNote() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title.trim())
            putExtra(Intent.EXTRA_TEXT, NoteText.shareText(title, body))
        }
        context.startActivity(Intent.createChooser(send, "Share note"))
    }

    fun copyNote() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("NESTNOTE note", NoteText.shareText(title, body))
        )
        android.widget.Toast.makeText(
            context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Removes the marker from the body right away and queues the file + manifest
     * entry for deletion on the next save. If the user discards instead of saving,
     * the original body (with marker) is preserved and the file is untouched.
     */
    fun deleteInlineVoice(attachmentId: String) {
        val newBody = NoteText.removeVoiceMarker(bodyValue.text, attachmentId)
        if (newBody != bodyValue.text) {
            bodyValue = bodyValue.copy(
                text = newBody,
                selection = TextRange(
                    bodyValue.selection.start.coerceIn(0, newBody.length),
                    bodyValue.selection.end.coerceIn(0, newBody.length)
                )
            )
            pendingVoiceDeletions = pendingVoiceDeletions + attachmentId
        }
        if (activeVoicePlayerId == attachmentId) activeVoicePlayerId = null
    }

    /**
     * Flushes deferred voice deletions and hands the note off to the parent. Deletions
     * run before [onSave] so the composable is still alive and can execute them; if
     * [onSave] throws, the caller can reopen the note and see missing-attachment cards
     * for cleanup, which is preferable to silently orphaning files.
     */
    fun performSave(note: NestNote) {
        if (pendingVoiceDeletions.isNotEmpty()) {
            val store = VoiceAttachmentStore(context)
            val manifest = VoiceAttachmentManifest(context)
            pendingVoiceDeletions.forEach { id ->
                runCatching { store.delete(voiceNoteId, id) }
                runCatching { manifest.remove(voiceNoteId, id) }
            }
            pendingVoiceDeletions = emptySet()
        }
        voiceChangesPending = false
        onSave(note)
    }

    LaunchedEffect(hasUnsavedChanges) { onUnsavedChangesChange(hasUnsavedChanges) }
    // A brand-new note opens ready to type.
    LaunchedEffect(Unit) { if (original == null) runCatching { titleFocus.requestFocus() } }

    // One-time migration: any voice attachment on disk without a marker in the body
    // is anchored at the end of the note. Idempotent — re-runs are no-ops.
    LaunchedEffect(voiceNoteId) {
        val store = VoiceAttachmentStore(context)
        val existingIds = runCatching { store.list(voiceNoteId).map { it.attachmentId } }
            .getOrDefault(emptyList())
        if (existingIds.isNotEmpty()) {
            val migrated = NoteText.migrateOrphanAttachments(bodyValue.text, existingIds)
            if (migrated != bodyValue.text) {
                bodyValue = bodyValue.copy(text = migrated)
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedTextColor = ink,
        focusedTextColor = ink,
        cursorColor = accent,
        unfocusedPlaceholderColor = muted,
        focusedPlaceholderColor = muted
    )

    Box(
        modifier = modifier
            .then(
                if (fullScreen) {
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                } else {
                    Modifier.fillMaxWidth().navigationBarsPadding()
                }
            )
            .background(editorCanvas)
    ) {
        if (backgroundIndex > 0) {
            NoteWallpaperArtwork(backgroundIndex, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(noteTints[colorIndex].copy(alpha = 0.58f)))
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (title.isBlank()) "New note" else title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { requestDismiss() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close editor"
                            )
                        }
                    },
                    actions = {
                        if (editorMode == EditorMode.Editing) {
                            IconButton(
                                onClick = {
                                    appearanceStartColor = colorIndex
                                    appearanceStartBackground = backgroundIndex
                                    toolRailTab = ToolRailTab.Colour
                                    toolRailVisible = true
                                },
                                modifier = Modifier.testTag("editor_appearance_button")
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Choose note appearance",
                                    tint = accent
                                )
                            }
                            IconButton(
                                onClick = {
                                    appearanceStartColor = colorIndex
                                    appearanceStartBackground = backgroundIndex
                                    toolRailTab = ToolRailTab.Format
                                    toolRailVisible = true
                                },
                                modifier = Modifier.testTag("editor_format_button")
                            ) {
                                Icon(
                                    Icons.Default.FormatSize,
                                    contentDescription = "Formatting",
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
                                        text = { Text(if (pinned) "Unpin note" else "Pin note") },
                                        onClick = { moreMenu = false; pinned = !pinned },
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
                                            editorModeViewModel.lock()
                                        } else {
                                            editorModeViewModel.edit()
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
                                if (editorMode == EditorMode.Editing && original != null) {
                                    DropdownMenuItem(
                                        text = { Text("Archive") },
                                        onClick = {
                                            moreMenu = false
                                            performSave(buildNote(archived = true))
                                        },
                                        leadingIcon = { Icon(Icons.Default.Archive, null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Share note") },
                                    enabled = title.isNotBlank() || body.isNotBlank(),
                                    onClick = { moreMenu = false; shareNote() },
                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy text") },
                                    enabled = title.isNotBlank() || body.isNotBlank(),
                                    onClick = { moreMenu = false; copyNote() },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                )
                                if (editorMode == EditorMode.Editing &&
                                    onDelete != null && original != null
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete note", color = colors.error) },
                                        onClick = { moreMenu = false; deleteConfirmation = true },
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
                                                reminder = NoteText.formatReminder(it)
                                            }
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Tomorrow morning") },
                                    onClick = {
                                        reminderMenu = false
                                        reminder = NoteText.formatReminder(
                                            NoteText.tomorrowMorning(Calendar.getInstance())
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Next Monday") },
                                    onClick = {
                                        reminderMenu = false
                                        reminder = NoteText.formatReminder(
                                            NoteText.nextMonday(Calendar.getInstance())
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pick date and time…") },
                                    onClick = { reminderMenu = false; pickReminder() }
                                )
                                if (reminder.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = { Text("Remove reminder") },
                                        onClick = { reminderMenu = false; reminder = "" }
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
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { it / 4 }) togetherWith
                                        (fadeOut(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + slideOutVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) { -it / 4 })
                                }
                            },
                            label = "editor-save-done"
                        ) { mode ->
                            TextButton(
                                onClick = {
                                    if (mode == EditorMode.Editing) {
                                        if (title.isNotBlank()) performSave(buildNote())
                                    } else {
                                        requestDismiss()
                                    }
                                },
                                enabled = mode == EditorMode.Locked || title.isNotBlank()
                            ) { Text(if (mode == EditorMode.Editing) "Save" else "Done") }
                        }
                    }
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = editorMode == EditorMode.Editing,
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
                        if (showCounts) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val words = remember(body) { NoteText.wordCount(body) }
                                val minutes = NoteText.readingMinutes(words)
                                Text(
                                    buildString {
                                        append(if (words == 1) "1 word" else "$words words")
                                        append(" · ${body.length} characters")
                                        if (minutes > 0) append(" · $minutes min read")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        ) { editorPadding ->
            Column(
                Modifier.fillMaxSize().padding(editorPadding).padding(horizontal = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(if (compactDialog) 8.dp else 18.dp))

                    // ---- Title ----
                    BasicTextField(
                        value = title,
                        onValueChange = { if (editorMode == EditorMode.Editing) title = it },
                        readOnly = editorMode == EditorMode.Locked,
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(titleFocus)
                            .padding(vertical = 8.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = textSize.titleSp.sp,
                            lineHeight = (textSize.titleSp * 1.26f).sp,
                            fontWeight = FontWeight.Normal,
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
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = muted
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (pinned) {
                        AssistChip(
                            onClick = { if (editorMode == EditorMode.Editing) pinned = false },
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

                    // ---- Checklist helpers ----
                    val checklistItems = remember(bodyValue.text) {
                        NoteText.checklistItems(bodyValue.text)
                    }
                    if (checklistItems.isNotEmpty()) {
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
                                checklistItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.checked,
                                            modifier = Modifier.semantics {
                                                contentDescription = buildString {
                                                    append(
                                                        item.text.ifBlank { "Empty checklist item" }
                                                    )
                                                    append(", ")
                                                    append(if (item.checked) "completed" else "not completed")
                                                }
                                            },
                                            enabled = editorMode == EditorMode.Editing,
                                            onCheckedChange = { checked ->
                                                bodyValue = bodyValue.copy(
                                                    text = NoteText.setChecklistItemChecked(
                                                        bodyValue.text, item.lineIndex, checked
                                                    )
                                                )
                                            }
                                        )
                                        Text(
                                            item.text.ifBlank { "(empty item)" },
                                            modifier = Modifier.weight(1f).clickable(
                                                enabled = editorMode == EditorMode.Editing
                                            ) {
                                                bodyValue = bodyValue.copy(
                                                    text = NoteText.setChecklistItemChecked(
                                                        bodyValue.text,
                                                        item.lineIndex,
                                                        !item.checked
                                                    )
                                                )
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

                    // ---- Preview toggle ----
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { previewVisible = !previewVisible }) {
                            Text(if (previewVisible) "Edit Markdown" else "Preview Markdown")
                        }
                    }

                    // ---- Body: preview or edit ----
                    if (previewVisible) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp).weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            PreviewWithInlineVoice(
                                body = body,
                                noteId = voiceNoteId,
                                textSize = textSize,
                                ink = ink,
                                activePlayerId = activeVoicePlayerId,
                                onPlayRequested = { id -> activeVoicePlayerId = id },
                                onStopRequested = { activeVoicePlayerId = null },
                                onDeleteRequested = { id -> pendingVoiceDeletion = id },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                        Text(
                            "Preview supports basic headings, emphasis, inline code, links, " +
                                "simple lists, and inline voice notes.",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    } else {
                        BasicTextField(
                            value = bodyValue,
                            onValueChange = {
                                if (editorMode == EditorMode.Editing) onBodyChange(it)
                            },
                            readOnly = editorMode == EditorMode.Locked,
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 3.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textSize.bodySp.sp,
                                lineHeight = (textSize.bodySp * 1.45f).sp,
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
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = muted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    // ---- Voice recording surface (edit mode only) ----
                    // Always visible so the user can anchor a new recording at the
                    // cursor without hunting through a menu. Playback of existing
                    // recordings happens in Preview mode via InlineVoicePlayer.
                    if (editorMode == EditorMode.Editing) {
                        Spacer(Modifier.height(8.dp))
                        VoiceNoteControls(
                            noteId = voiceNoteId,
                            cursorAtRecordStart = { bodyValue.selection.start },
                            onRecordingAttached = { attachmentId, cursor ->
                                val (newText, newCursor) = NoteText.insertVoiceMarker(
                                    bodyValue.text, cursor, attachmentId
                                )
                                bodyValue = TextFieldValue(newText, TextRange(newCursor))
                                voiceChangesPending = true
                            },
                            onDiscardHandlerReady = onRegisterVoiceDiscard
                        )
                    }

                    Spacer(Modifier.height(if (compactDialog) 6.dp else 12.dp))

                    // ---- Category chip ----
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        AssistChip(
                            onClick = { if (editorMode == EditorMode.Editing) categoryMenu = true },
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
                            categories.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = { category = item; categoryMenu = false },
                                    trailingIcon = {
                                        if (category == item) {
                                            Icon(Icons.Default.Check, null, tint = accent)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // ---- Reminder chip ----
                    if (reminder.isNotBlank()) {
                        Surface(
                            onClick = { pickReminder() },
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
                                        "Saved as a reminder time only — notifications aren't enabled yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = muted
                                    )
                                }
                                IconButton(onClick = { reminder = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear reminder",
                                        tint = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ---- Edit timestamps ----
                    if (original != null) {
                        val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        Text(
                            "Edited ${format.format(Date(original.updated))} · Created " +
                                SimpleDateFormat("MMM d", Locale.getDefault())
                                    .format(Date(original.created)),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = muted
                        )
                    }

                    Spacer(Modifier.height(if (compactDialog) 4.dp else 8.dp))
                }
            }
        }
    }

    fun dismissToolRail(discardDraft: Boolean) {
        if (discardDraft) {
            colorIndex = appearanceStartColor
            backgroundIndex = appearanceStartBackground
        }
        toolRailVisible = false
    }

    EditorToolRail(
        visible = toolRailVisible,
        tab = toolRailTab,
        onTabChange = { toolRailTab = it },
        colorIndex = colorIndex,
        backgroundIndex = backgroundIndex,
        onColorSelected = { colorIndex = it },
        onBackgroundSelected = { backgroundIndex = it },
        onApply = { dismissToolRail(discardDraft = false) },
        onCancel = { dismissToolRail(discardDraft = true) },
        onFormat = { action ->
            when (action) {
                FormatAction.Checklist -> applyLineEdit { text, cursor ->
                    NoteText.cycleChecklist(text, cursor)
                }
                FormatAction.Bullet -> applyLineEdit { text, cursor ->
                    NoteText.toggleBullet(text, cursor)
                }
                FormatAction.Numbered -> applyLineEdit { text, cursor ->
                    NoteText.toggleNumberedList(text, cursor)
                }
                FormatAction.Quote -> applyLineEdit { text, cursor ->
                    NoteText.toggleBlockquote(text, cursor)
                }
                FormatAction.Bold -> applyInlineEdit("**")
                FormatAction.Italic -> applyInlineEdit("_")
                FormatAction.Strike -> applyInlineEdit("~~")
                FormatAction.Code -> applyInlineEdit("`")
                FormatAction.Link -> applyMarkdownLink()
                FormatAction.Heading1 -> applyLineEdit { text, cursor ->
                    NoteText.toggleHeadingLevel(text, cursor, 1)
                }
                FormatAction.Heading2 -> applyLineEdit { text, cursor ->
                    NoteText.toggleHeadingLevel(text, cursor, 2)
                }
                FormatAction.Heading3 -> applyLineEdit { text, cursor ->
                    NoteText.toggleHeadingLevel(text, cursor, 3)
                }
            }
        }
    )

    // ---- Delete note dialog ----
    if (deleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmation = false },
            title = { Text("Delete this note?") },
            text = {
                Text(
                    "The note will be removed. You can undo deletion during the brief Undo " +
                        "window. Voice recordings are removed only after that window expires."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmation = false
                    onDelete()
                }) { Text("Delete note") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmation = false }) { Text("Keep note") }
            }
        )
    }

    // ---- Discard note dialog ----
    if (discardConfirmation) {
        AlertDialog(
            onDismissRequest = { discardConfirmation = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    discardConfirmation = false
                    onDismiss()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { discardConfirmation = false }) { Text("Keep editing") }
            }
        )
    }

    // ---- Delete voice note dialog ----
    if (pendingVoiceDeletion != null) {
        AlertDialog(
            onDismissRequest = { pendingVoiceDeletion = null },
            title = { Text("Delete voice note?") },
            text = {
                Text(
                    "The voice note will be removed from the note and its encrypted audio " +
                        "will be permanently deleted when you save. If you discard your " +
                        "changes instead, the voice note will be preserved."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingVoiceDeletion?.let { deleteInlineVoice(it) }
                    pendingVoiceDeletion = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingVoiceDeletion = null }) { Text("Keep") }
            }
        )
    }
}

/**
 * Renders the note body as alternating text and inline voice players.
 * Text segments use the existing MarkdownPreview renderer; voice segments
 * use [InlineVoicePlayer] at each marker's position.
 */
@Composable
private fun PreviewWithInlineVoice(
    body: String,
    noteId: Long,
    textSize: EditorTextSize,
    ink: Color,
    activePlayerId: String?,
    onPlayRequested: (String) -> Unit,
    onStopRequested: () -> Unit,
    onDeleteRequested: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(body) { NoteText.voiceSegments(body) }
    val bodySp = textSize.bodySp.toFloat()
    Column(modifier.verticalScroll(rememberScrollState())) {
        segments.forEach { segment ->
            if (segment.attachmentId == null) {
                if (segment.text.isNotBlank()) {
                    val spanned = remember(segment.text, bodySp, ink) {
                        MarkdownPreview.toSpanned(segment.text)
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        factory = { viewContext ->
                            TextView(viewContext).apply {
                                setTextColor(ink.toArgb())
                                setTextSize(
                                    android.util.TypedValue.COMPLEX_UNIT_SP,
                                    bodySp
                                )
                                setTextIsSelectable(true)
                                movementMethod = LinkMovementMethod.getInstance()
                            }
                        },
                        update = { textView ->
                            textView.setTextColor(ink.toArgb())
                            textView.textSize = bodySp
                            textView.text = spanned
                        }
                    )
                }
            } else {
                InlineVoicePlayer(
                    noteId = noteId,
                    attachmentId = segment.attachmentId!!,
                    activePlayerId = activePlayerId,
                    onPlayRequested = onPlayRequested,
                    onStopRequested = onStopRequested,
                    onDeleteRequested = onDeleteRequested,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}