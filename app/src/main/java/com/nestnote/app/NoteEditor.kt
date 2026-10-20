package com.nestnote.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal fun collapsibleSlotHeight(measuredHeight: Int, fraction: Float): Int =
    (measuredHeight * fraction).roundToInt().coerceAtLeast(0)


/**
 * Hides internal attachment IDs while editing without changing the saved note body.
 * Each marker is displayed as a compact, styled voice-note label. Offset mapping
 * keeps text selection and cursor movement anchored to the underlying marker.
 */
private class VoiceMarkerVisualTransformation(
    private val chipBackground: Color,
    private val chipForeground: Color,
    private val names: Map<String, String> = emptyMap()
) : VisualTransformation {
    private val markerRegex = Regex("""\[\[voice:([^\]\n]+)\]\]""")
    private val validAttachmentId = Regex("""(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")

    override fun filter(text: AnnotatedString): TransformedText {
        val matches = markerRegex.findAll(text.text).toList()
        if (matches.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val output = AnnotatedString.Builder()
        val ranges = mutableListOf<MarkerOffset>()
        var sourceCursor = 0
        var outputCursor = 0

        matches.forEach { match ->
            val prefix = text.text.substring(sourceCursor, match.range.first)
            output.append(prefix)
            outputCursor += prefix.length
            val attachmentId = match.groupValues[1]
            val displayName = if (validAttachmentId.matches(attachmentId)) names[attachmentId] ?: "Voice note" else "Voice note unavailable"
            val label = "  🎙 " + displayName.take(28) + "  "
            val transformedStart = outputCursor
            output.pushStyle(
                SpanStyle(
                    color = chipForeground,
                    background = chipBackground,
                    fontWeight = FontWeight.SemiBold
                )
            )
            output.append(label)
            output.pop()
            outputCursor += label.length
            ranges += MarkerOffset(match.range.first, match.range.last + 1, transformedStart, outputCursor)
            sourceCursor = match.range.last + 1
        }
        output.append(text.text.substring(sourceCursor))

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, text.length)
                var delta = 0
                for (range in ranges) {
                    if (safe < range.originalStart) break
                    if (safe <= range.originalEnd) {
                        return if (safe == range.originalEnd) range.transformedEnd
                        else range.transformedStart
                    }
                    delta += (range.transformedEnd - range.transformedStart) -
                        (range.originalEnd - range.originalStart)
                }
                return (safe + delta).coerceIn(0, output.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, output.length)
                var delta = 0
                for (range in ranges) {
                    if (safe < range.transformedStart) break
                    if (safe <= range.transformedEnd) {
                        return if (safe == range.transformedEnd) range.originalEnd
                        else range.originalStart
                    }
                    delta += (range.originalEnd - range.originalStart) -
                        (range.transformedEnd - range.transformedStart)
                }
                return (safe + delta).coerceIn(0, text.length)
            }
        }
        return TransformedText(output.toAnnotatedString(), mapping)
    }

    private data class MarkerOffset(
        val originalStart: Int,
        val originalEnd: Int,
        val transformedStart: Int,
        val transformedEnd: Int
    )
}

/**
 * Unified create/edit note editor.
 *
 * Undo/redo covers body text edits only.
 *
 * Autosave writes a draft (via [DraftStore]) whenever a field changes, debounced
 * 2 seconds, plus a periodic safety-net write every 15 seconds.
 *
 * Reminders: the reminder picker requests POST_NOTIFICATIONS on Android 13+ the
 * first time it is opened, and seeds itself from the note's existing reminder if
 * one is set (via [NoteTextParsing.parseReminderOrNull]). The parent schedules
 * the actual alarm via [ReminderScheduler] when the note is saved.
 *
 * Duplicate: the More menu offers "Duplicate note" for saved notes; the parent
 * allocates a fresh ID and saves a copy (current editor state, including
 * unsaved changes) as a new note. The duplicate never carries a reminder, so it
 * cannot double-fire an alarm.
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
    onRegisterVoiceCommit: (() -> Unit) -> Unit = {},
    onDuplicate: ((NestNote) -> Unit)? = null,
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

    // ---- Undo/redo ----
    val undoManager = remember(original?.id, draftNoteId) { TextUndoManager() }
    var canUndo by remember(original?.id, draftNoteId) { mutableStateOf(false) }
    var canRedo by remember(original?.id, draftNoteId) { mutableStateOf(false) }

    var category by rememberSaveable(original?.id) {
        mutableStateOf(original?.category ?: defaultNoteCategory)
    }
    var colorIndex by rememberSaveable(original?.id) {
        mutableIntStateOf(original?.color?.coerceIn(0, noteAccents.lastIndex) ?: 0)
    }
    var backgroundIndex by rememberSaveable(original?.id) {
        mutableIntStateOf(original?.background?.coerceIn(0, 9) ?: 0)
    }
    var reminder by rememberSaveable(original?.id) { mutableStateOf(original?.reminder.orEmpty()) }
    var pinned by rememberSaveable(original?.id) { mutableStateOf(original?.pinned ?: false) }
    var toolRailVisible by remember { mutableStateOf(false) }
    var toolRailTab by remember { mutableStateOf(ToolRailTab.Colour) }
    var categoryMenu by remember { mutableStateOf(false) }
    var previewVisible by rememberSaveable(original?.id) { mutableStateOf(false) }
    var noteFormat by rememberSaveable(original?.id) { mutableStateOf(original?.format ?: "plain") }
    var appearanceStartColor by remember { mutableIntStateOf(colorIndex) }
    var appearanceStartBackground by remember { mutableIntStateOf(backgroundIndex) }
    var discardConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    var voiceNoteDialog by rememberSaveable(original?.id, draftNoteId) { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val context = LocalContext.current
    var customWallpaperUri by rememberSaveable(original?.id, draftNoteId) { mutableStateOf(original?.wallpaperUri.orEmpty()) }
    val wallpaperImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            customWallpaperUri = uri.toString()
            backgroundIndex = 0
            android.widget.Toast.makeText(context, "Wallpaper imported", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val markdownExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(NoteText.toMarkdown(title, body))
                } ?: error("Unable to open destination")
                android.widget.Toast.makeText(context, "Markdown file saved", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "Could not save Markdown file", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    // Permission request is a no-op on Android < 13 (permission doesn't exist).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result ignored — reminders still save, they just won't fire until granted. */ }

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
    var pendingVoiceDeletion by remember { mutableStateOf<String?>(null) }
    var pendingVoiceDeletions by rememberSaveable(
        original?.id, draftNoteId,
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })
    ) { mutableStateOf<Set<String>>(emptySet()) }
    var sessionVoiceIds by rememberSaveable(
        original?.id, draftNoteId,
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })
    ) { mutableStateOf<Set<String>>(emptySet()) }
    val sessionIdSet = remember(sessionVoiceIds) {
        object : AbstractMutableSet<String>() {
            override val size get() = sessionVoiceIds.size
            override fun iterator(): MutableIterator<String> =
                sessionVoiceIds.toMutableList().iterator()
            override fun add(element: String): Boolean {
                if (element in sessionVoiceIds) return false
                sessionVoiceIds = sessionVoiceIds + element; return true
            }
            override fun remove(element: String): Boolean {
                if (element !in sessionVoiceIds) return false
                sessionVoiceIds = sessionVoiceIds - element; return true
            }
            override fun clear() { sessionVoiceIds = emptySet() }
        }
    }
    var voiceNames by remember(voiceNoteId) { mutableStateOf<Map<String, String>>(emptyMap()) }

    val hasUnsavedChanges = title != original?.title.orEmpty() ||
        body != original?.body.orEmpty() ||
        category != (original?.category ?: defaultNoteCategory) ||
        colorIndex != (original?.color ?: 0) ||
        backgroundIndex != (original?.background ?: 0) ||
        customWallpaperUri != original?.wallpaperUri.orEmpty() ||
        pinned != (original?.pinned ?: false) ||
        reminder != original?.reminder.orEmpty() ||
        noteFormat != (original?.format ?: "plain") ||
        voiceChangesPending

    // ---- Undo/redo plumbing ----
    fun snapshotOf(value: TextFieldValue) = TextUndoManager.Snapshot(
        text = value.text,
        selectionStart = value.selection.start,
        selectionEnd = value.selection.end
    )

    fun setBody(newValue: TextFieldValue, undoable: Boolean = true) {
        if (undoable) undoManager.push(snapshotOf(bodyValue))
        bodyValue = newValue
        canUndo = undoManager.canUndo()
        canRedo = undoManager.canRedo()
    }

    fun performUndo() {
        val restored = undoManager.undo(snapshotOf(bodyValue)) ?: return
        bodyValue = TextFieldValue(
            text = restored.text,
            selection = TextRange(restored.selectionStart, restored.selectionEnd)
        )
        canUndo = undoManager.canUndo()
        canRedo = undoManager.canRedo()
    }

    fun performRedo() {
        val restored = undoManager.redo(snapshotOf(bodyValue)) ?: return
        bodyValue = TextFieldValue(
            text = restored.text,
            selection = TextRange(restored.selectionStart, restored.selectionEnd)
        )
        canUndo = undoManager.canUndo()
        canRedo = undoManager.canRedo()
    }

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
            background = backgroundIndex,
            wallpaperUri = customWallpaperUri,
            format = if (noteFormat == "markdown") "markdown" else "plain"
        )
    }

    fun pickReminder() {
        // Android 13+ needs explicit POST_NOTIFICATIONS permission. Request it here
        // rather than on app launch — this is the moment the user signals they want
        // a reminder, so the prompt is contextual and likely to be accepted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Seed the picker from the existing reminder when there is one. Users
        // expecting "edit reminder" should not have to re-scroll to the same date.
        val seed = NoteTextParsing.parseReminderOrNull(reminder) ?: Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val picked = Calendar.getInstance().apply { set(year, month, day, hour, minute, 0) }
                reminder = NoteText.formatReminder(picked)
            }, seed.get(Calendar.HOUR_OF_DAY), seed.get(Calendar.MINUTE), true).show()
        }, seed.get(Calendar.YEAR), seed.get(Calendar.MONTH), seed.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun onBodyChange(next: TextFieldValue) {
        val continued = if (next.selection.collapsed) {
            NoteText.continueList(bodyValue.text, next.text, next.selection.start)
        } else null
        setBody(
            if (continued != null) TextFieldValue(continued.first, TextRange(continued.second))
            else next
        )
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
        setBody(TextFieldValue(text, TextRange(selectionStart, selectionEnd)))
    }

    fun applyMarkdownLink() {
        val oldValue = bodyValue
        val (text, selectionStart, selectionEnd) = NoteText.insertMarkdownLink(
            oldValue.text, oldValue.selection.start, oldValue.selection.end
        )
        setBody(TextFieldValue(text, TextRange(selectionStart, selectionEnd)))
    }

    fun applyInlineEdit(marker: String) {
        val oldValue = bodyValue
        val (text, selectionStart, selectionEnd) = NoteText.toggleInlineFormat(
            oldValue.text, oldValue.selection.start, oldValue.selection.end, marker
        )
        setBody(TextFieldValue(text, TextRange(selectionStart, selectionEnd)))
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

    fun deleteInlineVoice(attachmentId: String) {
        val newBody = NoteText.removeVoiceMarker(bodyValue.text, attachmentId)
        if (newBody != bodyValue.text) {
            setBody(
                bodyValue.copy(
                    text = newBody,
                    selection = TextRange(
                        bodyValue.selection.start.coerceIn(0, newBody.length),
                        bodyValue.selection.end.coerceIn(0, newBody.length)
                    )
                )
            )
            pendingVoiceDeletions = pendingVoiceDeletions + attachmentId
        }
        if (activeVoicePlayerId == attachmentId) activeVoicePlayerId = null
    }

    fun performSave(note: NestNote) {
        onSave(note)
    }

    // ---- Autosave ----
    val currentHasUnsavedChanges by rememberUpdatedState(hasUnsavedChanges)
    fun writeDraftNow() {
        if (!currentHasUnsavedChanges) return
        if (original == null && title.isBlank() && bodyValue.text.isBlank()) return
        runCatching {
            DraftStore(context).write(
                DraftStore.Draft(
                    noteId = voiceNoteId,
                    title = title,
                    body = bodyValue.text,
                    category = category,
                    color = colorIndex,
                    background = backgroundIndex,
                    pinned = pinned,
                    reminder = reminder,
                    wallpaperUri = customWallpaperUri,
                    savedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    LaunchedEffect(
        title, bodyValue.text, category, colorIndex, backgroundIndex,
        pinned, reminder, customWallpaperUri
    ) {
        delay(2_000L)
        writeDraftNow()
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(15_000L)
            writeDraftNow()
        }
    }

    // ---- Draft recovery (for existing notes — new-note orphans are Phase 7b) ----
    var pendingDraft by remember { mutableStateOf<DraftStore.Draft?>(null) }
    var showDraftRecoveryDialog by remember { mutableStateOf(false) }
    val draftTimeFormatter = remember {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    }

    LaunchedEffect(voiceNoteId) {
        val existing = original ?: return@LaunchedEffect
        val draft = withContext(Dispatchers.IO) { DraftStore(context).read(existing.id) }
        if (draft != null && draft.savedAtMillis > existing.updated) {
            pendingDraft = draft
            showDraftRecoveryDialog = true
        }
    }

    DisposableEffect(voiceNoteId) {
        onRegisterVoiceCommit {
            val store = VoiceAttachmentStore(context)
            val manifest = VoiceAttachmentManifest(context)
            val anchored = NoteText.voiceMarkers(bodyValue.text).map { it.attachmentId }.toSet()
            (pendingVoiceDeletions + sessionVoiceIds).filter { it !in anchored }.forEach { id ->
                runCatching { store.delete(voiceNoteId, id) }
                runCatching { manifest.remove(voiceNoteId, id) }
            }
            pendingVoiceDeletions = emptySet()
            sessionVoiceIds = emptySet()
            voiceChangesPending = false
            runCatching { DraftStore(context).clear(voiceNoteId) }
        }
        onRegisterVoiceDiscard {
            val store = VoiceAttachmentStore(context)
            val manifest = VoiceAttachmentManifest(context)
            val ids = sessionVoiceIds
            sessionVoiceIds = emptySet()
            pendingVoiceDeletions = emptySet()
            ids.forEach { id ->
                runCatching { store.delete(voiceNoteId, id) }
                runCatching { manifest.remove(voiceNoteId, id) }
            }
            runCatching { DraftStore(context).clear(voiceNoteId) }
        }
        onDispose { }
    }

    val markerIds = remember(body) { NoteText.voiceMarkers(body).map { it.attachmentId }.distinct() }
    LaunchedEffect(voiceNoteId, markerIds) {
        if (markerIds.isNotEmpty()) {
            voiceNames = withContext(Dispatchers.IO) {
                runCatching {
                    VoiceAttachmentManifest(context).list(voiceNoteId, VoiceAttachmentStore(context))
                        .associate { it.attachmentId to it.displayName }
                }.getOrDefault(emptyMap())
            }
        }
    }

    LaunchedEffect(hasUnsavedChanges) { onUnsavedChangesChange(hasUnsavedChanges) }
    LaunchedEffect(Unit) { if (original == null) runCatching { titleFocus.requestFocus() } }

    LaunchedEffect(voiceNoteId) {
        val store = VoiceAttachmentStore(context)
        val existingIds = runCatching { store.list(voiceNoteId).map { it.attachmentId } }
            .getOrDefault(emptyList())
            .filter { it !in pendingVoiceDeletions && it !in sessionVoiceIds }
        if (existingIds.isNotEmpty()) {
            val migrated = NoteText.migrateOrphanAttachments(bodyValue.text, existingIds)
            if (migrated != bodyValue.text) {
                setBody(
                    bodyValue.copy(
                        text = migrated,
                        selection = TextRange(
                            bodyValue.selection.start.coerceIn(0, migrated.length),
                            bodyValue.selection.end.coerceIn(0, migrated.length)
                        )
                    ),
                    undoable = false
                )
            }
        }
    }

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
            .onPreviewKeyEvent { event ->
                if (editorMode != EditorMode.Editing) return@onPreviewKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (!event.isCtrlPressed) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Z -> {
                        if (event.isShiftPressed) performRedo() else performUndo()
                        true
                    }
                    Key.Y -> {
                        performRedo()
                        true
                    }
                    else -> false
                }
            }
    ) {
        if (customWallpaperUri.isNotBlank()) {
            NoteCustomWallpaper(customWallpaperUri, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(noteTints[colorIndex].copy(alpha = 0.58f)))
        } else if (backgroundIndex > 0) {
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
                                        text = { Text("Voice note") },
                                        onClick = {
                                            moreMenu = false
                                            voiceNoteDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Mic, contentDescription = null)
                                        }
                                    )
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
                                    // Duplicate — captures the current editor state,
                                    // including unsaved edits, as a new note.
                                    if (onDuplicate != null) {
                                        DropdownMenuItem(
                                            text = { Text("Duplicate note") },
                                            onClick = {
                                                moreMenu = false
                                                onDuplicate.invoke(buildNote())
                                            },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                        )
                                    }
                                }
                                DropdownMenuItem(
                                    text = { Text("Share note") },
                                    enabled = title.isNotBlank() || body.isNotBlank(),
                                    onClick = { moreMenu = false; shareNote() },
                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Note format · " + if (noteFormat == "markdown") "Markdown" else "Plain text") },
                                    onClick = {
                                        moreMenu = false
                                        noteFormat = if (noteFormat == "markdown") "plain" else "markdown"
                                        previewVisible = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.TextFields, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Convert to Markdown file") },
                                    enabled = title.isNotBlank() || body.isNotBlank(),
                                    onClick = {
                                        moreMenu = false
                                        val safeTitle = title.trim().ifBlank { "note" }.replace(Regex("""[\\/:*?"<>|]"""), "_")
                                        markdownExportLauncher.launch("$safeTitle.md")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Download, null) }
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
                                        if (title.isNotBlank() || body.isNotBlank()) {
                                            performSave(buildNote())
                                        }
                                    } else {
                                        requestDismiss()
                                    }
                                },
                                enabled = mode == EditorMode.Locked || title.isNotBlank() || body.isNotBlank()
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
                            if (canUndo) {
                                IconButton(onClick = { performUndo() }) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                                }
                            }
                            if (canRedo) {
                                IconButton(onClick = { performRedo() }) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                                }
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
                                                    append(item.text.ifBlank { "Empty checklist item" })
                                                    append(", ")
                                                    append(if (item.checked) "completed" else "not completed")
                                                }
                                            },
                                            enabled = editorMode == EditorMode.Editing,
                                            onCheckedChange = { checked ->
                                                setBody(
                                                    bodyValue.copy(
                                                        text = NoteText.setChecklistItemChecked(
                                                            bodyValue.text, item.lineIndex, checked
                                                        )
                                                    )
                                                )
                                            }
                                        )
                                        Text(
                                            item.text.ifBlank { "(empty item)" },
                                            modifier = Modifier.weight(1f).clickable(
                                                enabled = editorMode == EditorMode.Editing
                                            ) {
                                                setBody(
                                                    bodyValue.copy(
                                                        text = NoteText.setChecklistItemChecked(
                                                            bodyValue.text,
                                                            item.lineIndex,
                                                            !item.checked
                                                        )
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

                    // Reading mode renders the real inline voice players by default.
                    // Markdown preview remains an optional editing aid, not a prerequisite
                    // for playing attachments from a saved note.
                    if (editorMode == EditorMode.Editing && noteFormat == "markdown") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { previewVisible = !previewVisible }) {
                                Text(if (previewVisible) "Edit Markdown" else "Preview Markdown")
                            }
                        }
                    }

                    if ((noteFormat == "markdown" && (previewVisible || editorMode == EditorMode.Locked)) ||
                        (editorMode == EditorMode.Locked && noteFormat != "markdown")) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp).weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface.copy(alpha = 0.82f),
                            border = if (editorMode == EditorMode.Editing) {
                                BorderStroke(1.dp, colors.outlineVariant)
                            } else null
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
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                renderMarkdown = noteFormat == "markdown"
                            )
                        }
                        if (editorMode == EditorMode.Editing && noteFormat == "markdown") {
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
                                if (editorMode == EditorMode.Editing) onBodyChange(it)
                            },
                            readOnly = editorMode == EditorMode.Locked,
                            // Keep attachment IDs out of both editable and read-only
                            // views. The stored body remains the canonical marker text.
                            visualTransformation = if (noteFormat == "markdown") VoiceMarkerVisualTransformation(
                                colors.primaryContainer,
                                colors.onPrimaryContainer,
                                voiceNames
                            ) else VisualTransformation.None,
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

                    Spacer(Modifier.height(if (compactDialog) 6.dp else 12.dp))

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
                                        "You'll be notified at this time.",
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
        onImportWallpaper = { wallpaperImportLauncher.launch(arrayOf("image/*")) },
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

    // Existing-note draft recovery dialog (Phase 7a). Orphaned new-note drafts are
    // handled by NestNoteApp on app start (Phase 7b).
    if (showDraftRecoveryDialog && pendingDraft != null) {
        val draft = pendingDraft!!
        AlertDialog(
            onDismissRequest = {
                showDraftRecoveryDialog = false
                pendingDraft = null
            },
            title = { Text("Recover unsaved changes?") },
            text = {
                Text(
                    "A draft from ${draftTimeFormatter.format(Date(draft.savedAtMillis))} " +
                        "was found. Recover it?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    title = draft.title
                    bodyValue = TextFieldValue(
                        text = draft.body,
                        selection = TextRange(draft.body.length)
                    )
                    category = draft.category
                    colorIndex = draft.color.coerceIn(0, noteAccents.lastIndex)
                    backgroundIndex = draft.background.coerceIn(0, 9)
                    pinned = draft.pinned
                    reminder = draft.reminder
                    customWallpaperUri = draft.wallpaperUri
                    undoManager.clear()
                    canUndo = false
                    canRedo = false
                    showDraftRecoveryDialog = false
                    pendingDraft = null
                }) { Text("Recover") }
            },
            dismissButton = {
                TextButton(onClick = {
                    runCatching { DraftStore(context).clear(voiceNoteId) }
                    showDraftRecoveryDialog = false
                    pendingDraft = null
                }) { Text("Discard draft") }
            }
        )
    }

    if (voiceNoteDialog && editorMode == EditorMode.Editing) {
        ModalBottomSheet(
            onDismissRequest = { voiceNoteDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Text("Voice note", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Recorded and stored encrypted on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )
                VoiceNoteControls(
                    noteId = voiceNoteId,
                    cursorAtRecordStart = { bodyValue.selection.start },
                    onRecordingAttached = { attachmentId, cursor ->
                        val (newText, newCursor) = NoteText.insertVoiceMarker(
                            bodyValue.text, cursor, attachmentId
                        )
                        setBody(TextFieldValue(newText, TextRange(newCursor)))
                        voiceChangesPending = true
                        voiceNoteDialog = false
                    },
                    addedThisSession = sessionIdSet
                )
            }
        }
    }

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

@Composable
internal fun PreviewWithInlineVoice(
    body: String,
    noteId: Long,
    textSize: EditorTextSize,
    ink: Color,
    activePlayerId: String?,
    onPlayRequested: (String) -> Unit,
    onStopRequested: () -> Unit,
    onDeleteRequested: (String) -> Unit,
    modifier: Modifier = Modifier,
    renderMarkdown: Boolean = true
) {
    val segments = remember(body) { NoteText.voiceSegments(body) }
    val bodySp = textSize.bodySp.toFloat()
    Column(modifier.verticalScroll(rememberScrollState())) {
        segments.forEachIndexed { segmentIndex, segment ->
            if (segment.attachmentId == null) {
                if (segment.text.isNotEmpty()) {
                    val spanned = remember(segment.text, bodySp, ink, renderMarkdown) {
                        if (renderMarkdown) MarkdownPreview.toSpanned(segment.text)
                        else android.text.SpannableString(segment.text)
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .testTag("voice_preview_text_$segmentIndex"),
                        factory = { viewContext ->
                            TextView(viewContext).apply {
                                setTextColor(ink.toArgb())
                                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, bodySp)
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
                        .testTag("voice_preview_player")
                )
            }
        }
    }
}