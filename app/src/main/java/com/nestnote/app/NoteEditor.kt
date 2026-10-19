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


// Unified create/edit note editor.

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
    var title by rememberSaveable(original?.id) { mutableStateOf(original?.title.orEmpty()) }
    var bodyValue by rememberSaveable(original?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(original?.body.orEmpty()))
    }
    val body = bodyValue.text
    val markdownBodySize = textSize.bodySp.toFloat()
    var category by rememberSaveable(original?.id) { mutableStateOf(original?.category ?: defaultNoteCategory) }
    var colorIndex by rememberSaveable(original?.id) { mutableIntStateOf(original?.color?.coerceIn(0, noteAccents.lastIndex) ?: 0) }
    var backgroundIndex by rememberSaveable(original?.id) { mutableIntStateOf(original?.background?.coerceIn(0, 5) ?: 0) }
    var reminder by rememberSaveable(original?.id) { mutableStateOf(original?.reminder.orEmpty()) }
    var pinned by rememberSaveable(original?.id) { mutableStateOf(original?.pinned ?: false) }
    var appearanceVisible by remember { mutableStateOf(false) }
    val appearanceRailState = rememberSlimRailState()
    val formattingRailState = rememberSlimRailState()
    var formattingVisible by remember { mutableStateOf(false) }
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
    val editorModeViewModel: EditorModeViewModel = viewModel(key = "editor-mode-${original?.id ?: draftNoteId}")
    val editorMode = editorModeViewModel.mode
    var voiceAttachmentCount by rememberSaveable(original?.id, draftNoteId) { mutableIntStateOf(0) }
    var voiceChangesPending by rememberSaveable(original?.id, draftNoteId) { mutableStateOf(false) }
    val voiceNoteId = original?.id ?: draftNoteId
    val colors = MaterialTheme.colorScheme
    val darkEditor = colors.surface.red < 0.5f
    val ink = if (darkEditor) colors.onSurface else Color(0xFF292633)
    val muted = if (darkEditor) colors.onSurfaceVariant else Color(0xFF66616D)
    val accent = if (darkEditor) colors.primary else Color(0xFF514783)
    val editorCanvas = if (backgroundIndex == 0) {
        if (darkEditor) colors.surface else noteTints[colorIndex]
    } else {
        colors.surface
    }
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
            // Carry the version opened by this editor; the parent checks it before writing.
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
        bodyValue = if (continued != null) TextFieldValue(continued.first, TextRange(continued.second)) else next
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
        clipboard.setPrimaryClip(ClipData.newPlainText("NESTNOTE note", NoteText.shareText(title, body)))
        android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(hasUnsavedChanges) { onUnsavedChangesChange(hasUnsavedChanges) }
    // A brand-new note opens ready to type.
    LaunchedEffect(Unit) { if (original == null) runCatching { titleFocus.requestFocus() } }

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
            .then(if (fullScreen) Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding() else Modifier.fillMaxWidth().navigationBarsPadding())
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
                    title = { Text(if (title.isBlank()) "New note" else title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { requestDismiss() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close editor")
                        }
                    },
                    actions = {
                        if (editorMode == EditorMode.Editing) {
                                IconButton(onClick = { pinned = !pinned }) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = if (pinned) "Unpin note" else "Pin note",
                                        tint = if (pinned) accent else if (darkEditor) colors.onSurfaceVariant else Color(0xFF55515C)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { reminderMenu = true }) {
                                        Icon(
                                            Icons.Default.NotificationsNone,
                                            contentDescription = "Choose reminder time",
                                            tint = if (reminder.isNotBlank()) accent else colors.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(expanded = reminderMenu, onDismissRequest = { reminderMenu = false }) {
                                        // Hide this preset when three hours from now would cross into tomorrow.
                                        val laterToday = NoteText.laterTodayOrNull(Calendar.getInstance())
                                        if (laterToday != null) {
                                            DropdownMenuItem(text = { Text("Later today") }, onClick = {
                                                reminderMenu = false
                                                NoteText.laterTodayOrNull(Calendar.getInstance())?.let {
                                                    reminder = NoteText.formatReminder(it)
                                                }
                                            })
                                        }
                                        DropdownMenuItem(text = { Text("Tomorrow morning") }, onClick = {
                                            reminderMenu = false; reminder = NoteText.formatReminder(NoteText.tomorrowMorning(Calendar.getInstance()))
                                        })
                                        DropdownMenuItem(text = { Text("Next Monday") }, onClick = {
                                            reminderMenu = false; reminder = NoteText.formatReminder(NoteText.nextMonday(Calendar.getInstance()))
                                        })
                                        DropdownMenuItem(text = { Text("Pick date and time…") }, onClick = { reminderMenu = false; pickReminder() })
                                        if (reminder.isNotBlank()) {
                                            DropdownMenuItem(text = { Text("Remove reminder") }, onClick = { reminderMenu = false; reminder = "" })
                                        }
                                    }
                                }
                                if (original != null) {
                                    IconButton(onClick = { onSave(buildNote(archived = true)) }) {
                                        Icon(Icons.Default.Archive, contentDescription = "Archive note", tint = colors.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = {
                                    appearanceStartColor = colorIndex
                                    appearanceStartBackground = backgroundIndex
                                    appearanceVisible = true
                                }) {
                                    Icon(Icons.Default.Palette, contentDescription = "Choose note appearance", tint = accent)
                                }
                                Box {
                                    IconButton(onClick = { moreMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More editor actions", tint = colors.onSurfaceVariant)
                                    }
                                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Formatting") },
                                            onClick = { moreMenu = false; formattingVisible = true },
                                            leadingIcon = { Icon(Icons.Default.FormatSize, null) }
                                        )
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
                                    }
                                }
                        }
                        IconButton(
                            onClick = {
                                if (editorMode == EditorMode.Editing) {
                                    keyboardController?.hide()
                                    editorModeViewModel.lock()
                                } else {
                                    editorModeViewModel.edit()
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = if (editorMode == EditorMode.Editing) "Lock note editing" else "Edit note"
                            }
                        ) {
                            AnimatedContent(targetState = editorMode, transitionSpec = { (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it / 3 }) togetherWith (fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + slideOutHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { -it / 3 }) }, label = "editor-lock-icon") { mode ->
                                Icon(
                                    imageVector = if (mode == EditorMode.Editing) Icons.Default.Lock else Icons.Default.Edit,
                                    contentDescription = null
                                )
                            }
                        }
                        AnimatedContent(targetState = editorMode, transitionSpec = { (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it / 4 }) togetherWith (fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + slideOutVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { -it / 4 }) }, label = "editor-save-done") { mode ->
                            TextButton(
                                onClick = {
                                    if (mode == EditorMode.Editing) {
                                        if (title.isNotBlank()) onSave(buildNote())
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
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                Column(Modifier.imePadding()) {
                            if (showCounts) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val words = NoteText.wordCount(body)
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
                    BasicTextField(
                    value = title,
                    onValueChange = { if (editorMode == EditorMode.Editing) title = it },
                    readOnly = editorMode == EditorMode.Locked,
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus).padding(vertical = 8.dp),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = textSize.titleSp.sp, lineHeight = (textSize.titleSp * 1.26f).sp,
                        fontWeight = FontWeight.Normal, color = ink
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineSmall, color = muted)
                            innerTextField()
                        }
                    }
                )
                    VoiceNoteControls(
                        noteId = voiceNoteId,
                        onAttachmentCountChanged = { voiceAttachmentCount = it },
                        onVoiceAttachmentAdded = { voiceChangesPending = true },
                        onDiscardHandlerReady = onRegisterVoiceDiscard
                    )
                    val checklistItems = NoteText.checklistItems(bodyValue.text)
                    if (checklistItems.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = accent.copy(alpha = 0.07f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("Checklist", style = MaterialTheme.typography.titleSmall, color = ink)
                                checklistItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.checked,
                                            modifier = Modifier.semantics {
                                                contentDescription = "${item.text.ifBlank { "Empty checklist item" }}, ${if (item.checked) "completed" else "not completed"}"
                                            },
                                            enabled = editorMode == EditorMode.Editing,
                                            onCheckedChange = { checked ->
                                                bodyValue = bodyValue.copy(text = NoteText.setChecklistItemChecked(bodyValue.text, item.lineIndex, checked))
                                            }
                                        )
                                        Text(
                                            item.text.ifBlank { "(empty item)" },
                                            modifier = Modifier.weight(1f).clickable(enabled = editorMode == EditorMode.Editing) {
                                                bodyValue = bodyValue.copy(text = NoteText.setChecklistItemChecked(bodyValue.text, item.lineIndex, !item.checked))
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (item.checked) muted else ink
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { previewVisible = !previewVisible }) {
                            Text(if (previewVisible) "Edit Markdown" else "Preview Markdown")
                        }
                    }
                    if (previewVisible) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                factory = { viewContext ->
                                    TextView(viewContext).apply {
                                        setTextColor(ink.toArgb())
                                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, markdownBodySize)
                                        setTextIsSelectable(true)
                                        movementMethod = LinkMovementMethod.getInstance()
                                    }
                                },
                                update = { textView ->
                                    textView.setTextColor(ink.toArgb())
                                    textView.textSize = markdownBodySize
                                    textView.text = MarkdownPreview.toSpanned(body)
                                }
                            )
                        }
                        Text(
                            "Preview supports basic headings, emphasis, inline code, links and simple lists.",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    } else {
                        BasicTextField(
                        value = bodyValue,
                        onValueChange = { if (editorMode == EditorMode.Editing) onBodyChange(it) },
                        readOnly = editorMode == EditorMode.Locked,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 3.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = textSize.bodySp.sp, lineHeight = (textSize.bodySp * 1.45f).sp,
                            color = ink
                        ),
                        minLines = 1,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxWidth()) {
                                if (bodyValue.text.isEmpty()) Text("Note", style = MaterialTheme.typography.bodyLarge, color = muted)
                                innerTextField()
                            }
                        }
                    )
                    }
    
                    Spacer(Modifier.height(if (compactDialog) 6.dp else 12.dp))
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = muted, modifier = Modifier.padding(horizontal = 12.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { item ->
                            FilterChip(
                                selected = category == item,
                                enabled = editorMode == EditorMode.Editing,
                                onClick = { category = item },
                                label = { Text(item) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = colors.surfaceVariant.copy(alpha = if (darkEditor) 0.72f else 0.55f),
                                    labelColor = ink,
                                    selectedContainerColor = accent,
                                    selectedLabelColor = colors.onPrimary
                                ),
                                border = BorderStroke(1.dp, colors.outlineVariant)
                            )
                        }
                    }
                    if (reminder.isNotBlank()) {
                        Surface(
                            onClick = { pickReminder() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceVariant.copy(alpha = if (darkEditor) 0.72f else 0.6f)
                        ) {
                            Row(
                                Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = accent)
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text("Reminder · $reminder", style = MaterialTheme.typography.bodyMedium, color = ink)
                                    Text(
                                        "Saved as a reminder time only — notifications aren’t enabled yet.",
                                        style = MaterialTheme.typography.bodySmall, color = muted
                                    )
                                }
                                IconButton(onClick = { reminder = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear reminder", tint = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (original != null) {
                        val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        Text(
                            "Edited ${format.format(Date(original.updated))} · Created ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(original.created))}",
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

    fun dismissAppearance(discardDraft: Boolean) {
        if (discardDraft) {
            colorIndex = appearanceStartColor
            backgroundIndex = appearanceStartBackground
        }
        appearanceVisible = false
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Keep the writing canvas dominant on phones, while giving the appearance
        // controls a little more room on tablets and other wide editor windows.
        val appearancePanelMaxWidth = if (maxWidth >= 600.dp) 480.dp else 360.dp
        AnimatedVisibility(visible = appearanceVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(colors.scrim.copy(alpha = 0.40f)).clickable { dismissAppearance(discardDraft = true) }.testTag("appearance_scrim"))
        }
        LaunchedEffect(appearanceVisible) {
            if (appearanceVisible) {
                appearanceRailState.open()
                snapshotFlow { appearanceRailState.targetValue }.collect { target ->
                    if (target == RailValue.Closed && appearanceVisible) dismissAppearance(discardDraft = true)
                }
            } else {
                appearanceRailState.close()
            }
        }
        if (appearanceVisible) {
            SlimRailPanel(
                state = appearanceRailState,
                panelWidth = appearancePanelMaxWidth,
                contentDescription = "Note appearance",
                modifier = Modifier.testTag("appearance_side_panel")
            ) {
                AppearanceSidePanel(
                    colorIndex = colorIndex,
                    backgroundIndex = backgroundIndex,
                    onColorSelected = { colorIndex = it },
                    onBackgroundSelected = { backgroundIndex = it },
                    onDismiss = { dismissAppearance(discardDraft = true) },
                    onApply = { dismissAppearance(discardDraft = false) },
                    modifier = Modifier.fillMaxHeight().width(appearancePanelMaxWidth)
                        .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }

    LaunchedEffect(formattingVisible) {
        if (formattingVisible) {
            formattingRailState.open()
            snapshotFlow { formattingRailState.targetValue }.collect { target ->
                if (target == RailValue.Closed && formattingVisible) formattingVisible = false
            }
        } else {
            formattingRailState.close()
        }
    }

    if (formattingVisible) {
        SlimRailPanel(
            state = formattingRailState,
            panelWidth = 304.dp,
            contentDescription = "Text formatting",
            modifier = Modifier.testTag("formatting_side_panel")
        ) {
            Column(
                Modifier.fillMaxHeight().width(304.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Formatting", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text("Format the selected text or current line", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    IconButton(onClick = { formattingVisible = false }, modifier = Modifier.testTag("formatting_close")) {
                        Icon(Icons.Default.Close, contentDescription = "Close formatting panel")
                    }
                }
                Text("Paragraph", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                TextButton(onClick = { applyLineEdit { text, cursor -> NoteText.cycleChecklist(text, cursor) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CheckBox, null); Spacer(Modifier.width(12.dp)); Text("Checklist") }
                TextButton(onClick = { applyLineEdit { text, cursor -> NoteText.toggleBullet(text, cursor) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null); Spacer(Modifier.width(12.dp)); Text("Bullet list") }
                TextButton(onClick = { applyLineEdit { text, cursor -> NoteText.toggleNumberedList(text, cursor) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FormatListNumbered, null); Spacer(Modifier.width(12.dp)); Text("Numbered list") }
                TextButton(onClick = { applyLineEdit { text, cursor -> NoteText.toggleBlockquote(text, cursor) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FormatQuote, null); Spacer(Modifier.width(12.dp)); Text("Blockquote") }
                HorizontalDivider()
                Text("Text style", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                TextButton(onClick = { applyInlineEdit("**") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FormatBold, null); Spacer(Modifier.width(12.dp)); Text("Bold") }
                TextButton(onClick = { applyInlineEdit("_") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FormatItalic, null); Spacer(Modifier.width(12.dp)); Text("Italic") }
                TextButton(onClick = { applyInlineEdit("~~") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FormatStrikethrough, null); Spacer(Modifier.width(12.dp)); Text("Strikethrough") }
                TextButton(onClick = { applyInlineEdit("`") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Code, null); Spacer(Modifier.width(12.dp)); Text("Inline code") }
                TextButton(onClick = { applyMarkdownLink() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(12.dp)); Text("Insert link") }
                HorizontalDivider()
                Text("Headings", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                TextButton(onClick = { applyLineEdit { text, cursor -> NoteText.toggleHeadingLevel(text, cursor, 1) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FormatSize, null); Spacer(Modifier.width(12.dp)); Text("Heading 1") }
                TextButton(onClick = { applyLineEdit { text, cursor -> NoteText.toggleHeadingLevel(text, cursor, 2) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.TextFields, null); Spacer(Modifier.width(12.dp)); Text("Heading 2") }
                TextButton(onClick = { applyLineEdit { text, cursor -> NoteText.toggleHeadingLevel(text, cursor, 3) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Title, null); Spacer(Modifier.width(12.dp)); Text("Heading 3") }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { formattingVisible = false }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }

    if (deleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmation = false },
            title = { Text("Delete this note?") },
            text = {
                Text("The note will be removed. You can undo deletion during the brief Undo window. Voice recordings are removed only after that window expires.")
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
}
