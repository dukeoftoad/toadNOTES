package com.nestnote.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface as AndroidTypeface
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
    categoryOptions: List<String> = categories,
    defaultNoteFormat: String = "plain",
    template: NoteTemplate? = null,
    allNotes: List<NestNote> = emptyList(),
    onOpenLinkedNote: ((NestNote, NestNote) -> Unit)? = null,
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
    var title by rememberSaveable(original?.id, draftNoteId) {
        mutableStateOf(original?.title ?: template?.title.orEmpty())
    }
    var bodyValue by rememberSaveable(original?.id, draftNoteId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(original?.body ?: template?.body.orEmpty()))
    }
    val body = bodyValue.text

    // ---- Undo/redo ----
    val undoManager = remember(original?.id, draftNoteId) { TextUndoManager() }
    var canUndo by remember(original?.id, draftNoteId) { mutableStateOf(false) }
    var canRedo by remember(original?.id, draftNoteId) { mutableStateOf(false) }

    var category by rememberSaveable(original?.id, draftNoteId) {
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
    var privateNote by rememberSaveable(original?.id) { mutableStateOf(original?.privateNote ?: false) }
    var toolRailVisible by remember { mutableStateOf(false) }
    var toolRailTab by remember { mutableStateOf(ToolRailTab.Colour) }
    var previewVisible by rememberSaveable(original?.id) { mutableStateOf(false) }
    var editVoiceText by rememberSaveable(original?.id, draftNoteId) { mutableStateOf(false) }
    var noteFormat by rememberSaveable(original?.id, draftNoteId) {
        mutableStateOf(original?.format ?: template?.format ?: if (defaultNoteFormat == "markdown") "markdown" else "plain")
    }
    var appearanceStartColor by remember { mutableIntStateOf(colorIndex) }
    var appearanceStartBackground by remember { mutableIntStateOf(backgroundIndex) }
    var appearanceStartWallpaperUri by remember { mutableStateOf(original?.wallpaperUri.orEmpty()) }
    var discardConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var voiceNoteDialog by rememberSaveable(original?.id, draftNoteId) { mutableStateOf(false) }
    var qrCodeDialog by remember { mutableStateOf(false) }
    var qrPayloadToSave by remember { mutableStateOf<String?>(null) }
    val titleFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val editorPreferences = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val customTextSizeSp = remember(original?.id) {
        editorPreferences.getInt(PREF_TEXT_SIZE_SP, textSize.bodySp).coerceIn(12, 32)
    }
    val editorFontStyle = remember(original?.id) {
        runCatching { EditorFontFamily.valueOf(editorPreferences.getString(PREF_EDITOR_FONT, "SANS") ?: "SANS") }
            .getOrDefault(EditorFontFamily.SANS)
    }
    val customFontPath = remember(original?.id) { editorPreferences.getString(PREF_EDITOR_FONT_FILE, null) }
    val editorTypeface = remember(editorFontStyle, customFontPath) {
        when (editorFontStyle) {
            EditorFontFamily.SERIF -> AndroidTypeface.SERIF
            EditorFontFamily.MONOSPACE -> AndroidTypeface.MONOSPACE
            EditorFontFamily.CURSIVE -> AndroidTypeface.create("casual", AndroidTypeface.NORMAL)
            EditorFontFamily.CUSTOM -> customFontPath?.let { path ->
                runCatching { AndroidTypeface.createFromFile(path) }.getOrNull()
            } ?: AndroidTypeface.DEFAULT
            EditorFontFamily.SANS -> AndroidTypeface.SANS_SERIF
        }
    }
    val editorFontFamily = remember(editorTypeface) { FontFamily(editorTypeface) }
    var customWallpaperUri by rememberSaveable(original?.id, draftNoteId) { mutableStateOf(original?.wallpaperUri.orEmpty()) }
    val wallpaperImportLauncher = rememberWallpaperImportLauncher { imported ->
        customWallpaperUri = imported
        backgroundIndex = 0
    }
    val markdownExportLauncher = rememberMarkdownExportLauncher { NoteText.toMarkdown(title, body) }
    val qrImageExportLauncher = rememberQrImageExportLauncher(
        pendingPayload = { qrPayloadToSave },
        onFinished = { qrPayloadToSave = null }
    )
    // Permission request is a no-op on Android < 13 (permission doesn't exist).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result ignored — reminders still save, they just won't fire until granted. */ }

    val editorModeViewModel: EditorModeViewModel =
        viewModel(key = "editor-mode-${original?.id ?: draftNoteId}")
    val editorMode = editorModeViewModel.mode
    var voiceChangesPending by rememberSaveable(original?.id, draftNoteId) {
        mutableStateOf(false)
    }
    val voiceNoteId = original?.id ?: draftNoteId
    val colors = MaterialTheme.colorScheme
    // Use perceived surface brightness, not only the red channel, to classify dark themes.
    // This keeps editor ink and secondary text readable with tinted/dynamic color schemes.
    val darkEditor = colors.surface.luminance() < 0.5f
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

    fun buildNote(
        archived: Boolean = original?.archived ?: false,
        keepReminder: Boolean = true
    ): NestNote {
        val now = System.currentTimeMillis()
        return NestNote(
            id = original?.id ?: draftNoteId,
            title = title.trim(),
            body = body.trim(),
            category = category,
            color = colorIndex,
            pinned = if (archived) false else pinned,
            reminder = if (keepReminder) reminder else "",
            updated = original?.updated ?: 0L,
            created = original?.created ?: now,
            archived = archived,
            background = backgroundIndex,
            wallpaperUri = customWallpaperUri,
            format = if (noteFormat == "markdown") "markdown" else "plain",
            privateNote = privateNote
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
                if (picked.timeInMillis <= System.currentTimeMillis()) {
                    android.widget.Toast.makeText(
                        context, "Pick a time in the future", android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    reminder = NoteText.formatReminder(picked)
                }
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

    // Markdown-only markers would render as literal characters in a plain-text note,
    // so the first Bold/Italic/Link on a plain note switches the note to Markdown.
    fun switchToMarkdownIfPlain() {
        if (noteFormat == "markdown") return
        noteFormat = "markdown"
        previewVisible = false
        android.widget.Toast.makeText(
            context, "Note switched to Markdown", android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun applyMarkdownLink() {
        switchToMarkdownIfPlain()
        val oldValue = bodyValue
        val (text, selectionStart, selectionEnd) = NoteText.insertMarkdownLink(
            oldValue.text, oldValue.selection.start, oldValue.selection.end
        )
        setBody(TextFieldValue(text, TextRange(selectionStart, selectionEnd)))
    }

    fun applyInlineEdit(marker: String, switchToMarkdown: Boolean = false) {
        if (switchToMarkdown) switchToMarkdownIfPlain()
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
    suspend fun writeDraftNow() {
        if (!currentHasUnsavedChanges) return
        if (original == null && title.isBlank() && bodyValue.text.isBlank()) return
        // Read Compose state here (main thread), then do file I/O off the main thread.
        val draft = DraftStore.Draft(
            noteId = voiceNoteId,
            title = title,
            body = bodyValue.text,
            category = category,
            color = colorIndex,
            background = backgroundIndex,
            pinned = pinned,
            reminder = reminder,
            wallpaperUri = customWallpaperUri,
            savedAtMillis = System.currentTimeMillis(),
            format = noteFormat
        )
        withContext(Dispatchers.IO) { runCatching { DraftStore(context).write(draft) } }
    }

    LaunchedEffect(
        title, bodyValue.text, category, colorIndex, backgroundIndex,
        pinned, reminder, customWallpaperUri, noteFormat
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

    // Stable instance: a fresh transformation on every recomposition forces the text field
    // to re-run layout for the whole body even when nothing about the chips changed.
    val voiceChipTransformation = remember(
        colors.primaryContainer, colors.onPrimaryContainer, voiceNames
    ) {
        VoiceMarkerVisualTransformation(
            colors.primaryContainer, colors.onPrimaryContainer, voiceNames
        )
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
                EditorTopBar(
                    title = title,
                    editorMode = editorMode,
                    pinned = pinned,
                    privateNote = privateNote,
                    reminder = reminder,
                    noteFormat = noteFormat,
                    hasContent = title.isNotBlank() || body.isNotBlank(),
                    isExisting = original != null,
                    canDelete = onDelete != null && original != null,
                    canDuplicate = onDuplicate != null,
                    accent = accent,
                    onClose = ::requestDismiss,
                    onOpenToolRail = { tab ->
                        appearanceStartColor = colorIndex
                        appearanceStartBackground = backgroundIndex
                        appearanceStartWallpaperUri = customWallpaperUri
                        toolRailTab = tab
                        toolRailVisible = true
                    },
                    onVoiceNote = { voiceNoteDialog = true },
                    onTogglePinned = { pinned = !pinned },
                    onTogglePrivate = { privateNote = !privateNote },
                    onReminderChange = { reminder = it },
                    onPickReminder = ::pickReminder,
                    onLock = { editorModeViewModel.lock() },
                    onEdit = { editorModeViewModel.edit() },
                    onArchive = { performSave(buildNote(archived = true)) },
                    onDuplicate = { onDuplicate?.invoke(buildNote(keepReminder = false)) },
                    onShare = ::shareNote,
                    onShowQr = { qrCodeDialog = true },
                    onToggleFormat = {
                        noteFormat = if (noteFormat == "markdown") "plain" else "markdown"
                        previewVisible = false
                    },
                    onExportMarkdown = { fileName -> markdownExportLauncher.launch(fileName) },
                    onCopy = ::copyNote,
                    onRequestDelete = { deleteConfirmation = true },
                    onSave = { performSave(buildNote()) }
                )
            },
            bottomBar = {
                EditorBottomBar(
                    visible = editorMode == EditorMode.Editing,
                    showCounts = showCounts,
                    body = body,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    muted = muted,
                    onUndo = ::performUndo,
                    onRedo = ::performRedo
                )
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

                    val editable = editorMode == EditorMode.Editing

                    EditorTitleField(
                        title = title,
                        onTitleChange = { title = it },
                        editable = editable,
                        focusRequester = titleFocus,
                        textSize = textSize,
                        bodyTextSp = customTextSizeSp,
                        fontFamily = editorFontFamily,
                        ink = ink,
                        muted = muted
                    )

                    if (pinned) {
                        EditorPinnedChip(accent, editable) { pinned = false }
                    }

                    val checklistItems = remember(bodyValue.text) {
                        NoteText.checklistItems(bodyValue.text)
                    }
                    if (checklistItems.isNotEmpty()) {
                        EditorChecklistCard(
                            items = checklistItems,
                            editable = editable,
                            accent = accent,
                            ink = ink,
                            muted = muted,
                            onItemChecked = { item, checked ->
                                setBody(
                                    bodyValue.copy(
                                        text = NoteText.setChecklistItemChecked(
                                            bodyValue.text, item.lineIndex, checked
                                        )
                                    )
                                )
                            }
                        )
                    }

                    EditorBodySection(
                        bodyValue = bodyValue,
                        onBodyChange = ::onBodyChange,
                        editable = editable,
                        noteFormat = noteFormat,
                        previewVisible = previewVisible,
                        onPreviewVisibleChange = { previewVisible = it },
                        editVoiceText = editVoiceText,
                        onEditVoiceTextChange = { editVoiceText = it },
                        voiceNoteId = voiceNoteId,
                        textSize = textSize,
                        bodyTextSp = customTextSizeSp,
                        fontFamily = editorFontFamily,
                        typeface = editorTypeface,
                        ink = ink,
                        muted = muted,
                        visualTransformation = voiceChipTransformation,
                        activePlayerId = activeVoicePlayerId,
                        onPlayRequested = { id -> activeVoicePlayerId = id },
                        onStopRequested = { activeVoicePlayerId = null },
                        onDeleteRequested = { id -> pendingVoiceDeletion = id }
                    )
                    if (allNotes.isNotEmpty()) {
                        EditorRelatedNotes(title, body, voiceNoteId, allNotes) { target ->
                            onOpenLinkedNote?.invoke(buildNote(), target)
                        }
                    }

                    Spacer(Modifier.height(if (compactDialog) 6.dp else 12.dp))

                    EditorFooter(
                        editable = editable,
                        category = category,
                        categoryOptions = categoryOptions,
                        onCategoryChange = { category = it },
                        reminder = reminder,
                        onPickReminder = ::pickReminder,
                        onClearReminder = { reminder = "" },
                        original = original,
                        darkEditor = darkEditor,
                        accent = accent,
                        ink = ink,
                        muted = muted
                    )

                    Spacer(Modifier.height(if (compactDialog) 4.dp else 8.dp))
                }
            }
        }
    }

    fun dismissToolRail(discardDraft: Boolean) {
        if (discardDraft) {
            colorIndex = appearanceStartColor
            backgroundIndex = appearanceStartBackground
            customWallpaperUri = appearanceStartWallpaperUri
        }
        toolRailVisible = false
    }

    EditorToolRail(
        visible = toolRailVisible,
        tab = toolRailTab,
        onTabChange = { toolRailTab = it },
        colorIndex = colorIndex,
        backgroundIndex = backgroundIndex,
        customWallpaperUri = customWallpaperUri,
        onColorSelected = { colorIndex = it },
        onImportWallpaper = { wallpaperImportLauncher.launch(arrayOf("image/*")) },
        // A selected preset must replace the imported image, which otherwise takes precedence.
        onBackgroundSelected = {
            backgroundIndex = it
            customWallpaperUri = ""
        },
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
                FormatAction.Bold -> applyInlineEdit("**", switchToMarkdown = true)
                FormatAction.Italic -> applyInlineEdit("_", switchToMarkdown = true)
                FormatAction.Strike -> applyInlineEdit("~~")
                FormatAction.Code -> applyInlineEdit("`")
                FormatAction.Link -> applyMarkdownLink()
                FormatAction.NoteLink -> {
                    val wrapped = wrapAsNoteLink(bodyValue.text, bodyValue.selection.min, bodyValue.selection.max)
                    noteFormat = "markdown"
                    setBody(TextFieldValue(wrapped.first, TextRange(wrapped.second, wrapped.third)))
                }
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

    if (qrCodeDialog) {
        NoteQrDialog(
            title = title,
            body = body,
            onDismiss = { qrCodeDialog = false },
            onSaveImage = { payload ->
                qrPayloadToSave = payload
                qrImageExportLauncher.launch("nestnote-qr.png")
            }
        )
    }

    if (deleteConfirmation && onDelete != null) {
        DeleteNoteDialog(
            onConfirm = {
                deleteConfirmation = false
                onDelete()
            },
            onDismiss = { deleteConfirmation = false }
        )
    }

    if (discardConfirmation) {
        DiscardChangesDialog(
            onConfirm = {
                discardConfirmation = false
                onDismiss()
            },
            onDismiss = { discardConfirmation = false }
        )
    }

    // Existing-note draft recovery dialog (Phase 7a). Orphaned new-note drafts are
    // handled by NestNoteApp on app start (Phase 7b).
    pendingDraft?.takeIf { showDraftRecoveryDialog }?.let { draft ->
        DraftRecoveryDialog(
            draft = draft,
            timeFormatter = draftTimeFormatter,
            onRecover = {
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
                noteFormat = draft.format
                undoManager.clear()
                canUndo = false
                canRedo = false
                showDraftRecoveryDialog = false
                pendingDraft = null
            },
            onDiscard = {
                runCatching { DraftStore(context).clear(voiceNoteId) }
                showDraftRecoveryDialog = false
                pendingDraft = null
            },
            onDismiss = {
                showDraftRecoveryDialog = false
                pendingDraft = null
            }
        )
    }

    if (voiceNoteDialog && editorMode == EditorMode.Editing) {
        VoiceNoteSheet(
            noteId = voiceNoteId,
            cursorAtRecordStart = { bodyValue.selection.start },
            addedThisSession = sessionIdSet,
            onDismiss = { voiceNoteDialog = false },
            onRecordingAttached = { attachmentId, cursor ->
                val (newText, newCursor) = NoteText.insertVoiceMarker(
                    bodyValue.text, cursor, attachmentId
                )
                setBody(TextFieldValue(newText, TextRange(newCursor)))
                voiceChangesPending = true
                voiceNoteDialog = false
            }
        )
    }

    pendingVoiceDeletion?.let { id ->
        DeleteVoiceNoteDialog(
            onConfirm = {
                deleteInlineVoice(id)
                pendingVoiceDeletion = null
            },
            onDismiss = { pendingVoiceDeletion = null }
        )
    }
}
