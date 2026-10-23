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
import androidx.biometric.BiometricManager
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
import androidx.compose.material.icons.filled.BookmarkAdd
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
import androidx.compose.material.icons.filled.FileOpen
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun NestNoteApp(
    store: NoteStore,
    externalMarkdownUri: android.net.Uri? = null,
    onExternalMarkdownHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val appVersion = remember { readAppVersion(context) }
    val windowSizeClass = (context as? ComponentActivity)?.let { calculateWindowSizeClass(it) }
    val isExpandedWidth = windowSizeClass?.widthSizeClass != null &&
        windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val preferences = remember { context.getSharedPreferences(PREFS, 0) }
    var notes by remember { mutableStateOf<List<NestNote>>(emptyList()) }
    var storageLoaded by remember { mutableStateOf(false) }
    var lastBackupAt by remember { mutableLongStateOf(preferences.getLong(PREF_BACKUP_LAST_SUCCESS_AT, 0L)) }
    var backupStatus by remember { mutableStateOf(preferences.getString(PREF_BACKUP_LAST_STATUS, "Not yet backed up") ?: "Not yet backed up") }
    var automaticBackupEnabled by remember { mutableStateOf(preferences.getBoolean(PREF_BACKUP_AUTOMATIC_ENABLED, false)) }
    var automaticBackupLastRun by remember { mutableLongStateOf(preferences.getLong(PREF_BACKUP_AUTOMATIC_LAST_RUN, 0L)) }
    var automaticBackupStatus by remember { mutableStateOf(preferences.getString(PREF_BACKUP_AUTOMATIC_STATUS, "Not run yet") ?: "Not run yet") }
    var storageRecoveryRequired by remember { mutableStateOf(false) }
    var showStorageRecoveryDialog by remember { mutableStateOf(false) }
    LaunchedEffect(store) {
        val result = withContext(Dispatchers.IO) { store.loadSafely() }
        notes = result.notes
        storageRecoveryRequired = result.recoveryRequired
        showStorageRecoveryDialog = result.recoveryRequired
        storageLoaded = true
    }
    LaunchedEffect(storageLoaded) {
        if (!storageLoaded) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching { ReminderScheduler.rescheduleAll(context, notes) }
        }
    }
    // Phase 7b: orphaned new-note draft recovery.
    // orphanDraftCheckCompleted is `remember` (not `rememberSaveable`) so that a
    // device rotation re-runs the check and re-shows the dialog if the user was
    // mid-recovery. Losing this once per rotation is better than silently
    // dropping orphan drafts entirely.
    var pendingOrphanDrafts by remember { mutableStateOf<List<DraftStore.Draft>>(emptyList()) }
    var showOrphanDraftDialog by remember { mutableStateOf(false) }
    var orphanDraftCheckCompleted by remember { mutableStateOf(false) }
    LaunchedEffect(storageLoaded) {
        if (!storageLoaded || orphanDraftCheckCompleted) return@LaunchedEffect
        orphanDraftCheckCompleted = true
        val drafts = withContext(Dispatchers.IO) { DraftStore(context).listAll() }
        val noteIds = notes.map { it.id }.toSet()
        val orphans = drafts
            .filter { it.noteId !in noteIds && (it.title.isNotBlank() || it.body.isNotBlank()) }
            .sortedByDescending { it.savedAtMillis }
        if (orphans.isNotEmpty()) {
            pendingOrphanDrafts = orphans
            showOrphanDraftDialog = true
        }
    }
    var pendingRestore by remember { mutableStateOf<List<NestNote>?>(null) }
    var themeMode by remember { mutableStateOf(ThemeMode.values().firstOrNull { it.name == preferences.getString(PREF_THEME, "SYSTEM") } ?: ThemeMode.SYSTEM) }
    var dynamicColors by remember { mutableStateOf(preferences.getBoolean(PREF_DYNAMIC, true)) }
    var amoled by remember { mutableStateOf(preferences.getBoolean(PREF_AMOLED, false)) }
    var dashboardLayout by remember { mutableStateOf(DashboardLayout.values().firstOrNull { it.name == preferences.getString(PREF_LAYOUT, "GRID") } ?: DashboardLayout.GRID) }
    var editorMode by remember { mutableStateOf(EditorPresentationMode.values().firstOrNull { it.name == preferences.getString(PREF_EDITOR, "FULL_SCREEN") } ?: EditorPresentationMode.FULL_SCREEN) }
    var dashboardDensity by remember { mutableStateOf(DashboardDensity.values().firstOrNull { it.name == preferences.getString(PREF_DENSITY, "COMPACT") } ?: DashboardDensity.COMPACT) }
    var sortByTitle by remember { mutableStateOf(preferences.getString(PREF_DEFAULT_SORT, "RECENT") == "TITLE") }
    var sortByCreated by remember { mutableStateOf(preferences.getString(PREF_DEFAULT_SORT, "RECENT") == "CREATED") }
    var showNotePreviews by remember { mutableStateOf(preferences.getBoolean(PREF_NOTE_PREVIEWS, true)) }
    var showNoteTimestamps by remember { mutableStateOf(preferences.getBoolean(PREF_NOTE_TIMESTAMPS, true)) }
    var categoryOptions by remember {
        mutableStateOf(preferences.getStringSet(PREF_CUSTOM_CATEGORIES, null)?.toList()?.sorted() ?: categories)
    }
    var defaultNoteCategory by remember {
        mutableStateOf(preferences.getString(PREF_DEFAULT_CATEGORY, "Ideas")?.takeIf { it in categoryOptions } ?: "Ideas")
    }
    var noteColorStyle by remember { mutableStateOf(NoteColorStyle.values().firstOrNull { it.name == preferences.getString(PREF_NOTE_STYLE, "PASTEL") } ?: NoteColorStyle.PASTEL) }
    var editorTextSize by remember { mutableStateOf(EditorTextSize.values().firstOrNull { it.name == preferences.getString(PREF_TEXT_SIZE, "MEDIUM") } ?: EditorTextSize.MEDIUM) }
    var showEditorCounts by remember { mutableStateOf(preferences.getBoolean(PREF_EDITOR_COUNTS, true)) }
    var selectedView by remember { mutableStateOf("Home") }
    var showPrivateVaultGate by remember { mutableStateOf(false) }
    var privateVaultUnlocked by remember { mutableStateOf(false) }
    var privateVaultBiometricEnabled by remember { mutableStateOf(preferences.getBoolean(PREF_PRIVATE_VAULT_BIOMETRIC_ENABLED, true)) }
    val privateVaultBiometricAvailable = remember(context) {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
    fun requestPrivateVault() { showPrivateVaultGate = true }
    DisposableEffect(context, selectedView) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && selectedView == "Private") {
                privateVaultUnlocked = false
                selectedView = "Home"
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(selectedView) {
        if (selectedView != "Private") privateVaultUnlocked = false
        if (selectedView == "Backup") {
            automaticBackupLastRun = preferences.getLong(PREF_BACKUP_AUTOMATIC_LAST_RUN, 0L)
            automaticBackupStatus = preferences.getString(PREF_BACKUP_AUTOMATIC_STATUS, "Not run yet") ?: "Not run yet"
        }
    }
    var search by remember { mutableStateOf("") }
    var selectedNoteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var savedSearches by remember {
        mutableStateOf<List<String>>(preferences.getStringSet(PREF_SAVED_SEARCHES, emptySet())?.toList()?.sorted() ?: emptyList())
    }
    var searchVisible by remember { mutableStateOf(false) }
    var editorNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorDraftId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorTemplate by remember { mutableStateOf<NoteTemplate?>(null) }
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editorHasUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    var discardVoiceChanges by remember { mutableStateOf<() -> Unit>({}) }
    var commitVoiceChanges by remember { mutableStateOf<() -> Unit>({}) }
    var confirmSystemDiscard by remember { mutableStateOf(false) }
    val editorNote = notes.firstOrNull { it.id == editorNoteId }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun saveString(key: String, value: String) = preferences.edit().putString(key, value).apply()
    fun saveBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
    fun saveLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()
    fun persist(next: List<NestNote>, allowRecovery: Boolean = false): Boolean {
        if (!storageLoaded) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Notes are still loading. Try again in a moment.") }
            return false
        }
        if (storageRecoveryRequired && !allowRecovery) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Restore a backup before changing notes. Existing local data was not overwritten.") }
            return false
        }
        val saved = store.save(next)
        if (saved) {
            val previousIds = notes.map { it.id }.toSet()
            val nextIds = next.map { it.id }.toSet()
            val removedIds = previousIds - nextIds
            val notesForAlarms = next
            coroutineScope.launch(Dispatchers.IO) {
                removedIds.forEach { ReminderScheduler.cancel(context, it) }
                ReminderScheduler.rescheduleAll(context, notesForAlarms)
            }
            notes = next
            if (allowRecovery) storageRecoveryRequired = false
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Save failed. Your previous notes are still available.") }
        }
        return saved
    }
    fun deleteNote(note: NestNote, closeEditor: Boolean = false) {
        if (!persist(notes.filterNot { it.id == note.id })) return
        if (closeEditor) editorVisible = false
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Note deleted",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && notes.none { it.id == note.id }) {
                persist(notes + note)
            } else if (
                result != SnackbarResult.ActionPerformed &&
                !storageRecoveryRequired &&
                notes.none { it.id == note.id }
            ) {
                runCatching { VoiceAttachmentStore(context).deleteAll(note.id) }
                runCatching { VoiceAttachmentManifest(context).removeNote(note.id) }
            }
        }
    }
    fun setSort(key: String) {
        sortByTitle = key == "TITLE"
        sortByCreated = key == "CREATED"
        saveString(PREF_DEFAULT_SORT, key)
    }
    fun resetPreferences() {
        preferences.edit().clear().apply()
        themeMode = ThemeMode.SYSTEM
        dynamicColors = true
        amoled = false
        dashboardLayout = DashboardLayout.GRID
        editorMode = EditorPresentationMode.FULL_SCREEN
        dashboardDensity = DashboardDensity.COMPACT
        sortByTitle = false
        sortByCreated = false
        showNotePreviews = true
        showNoteTimestamps = true
        defaultNoteCategory = "Ideas"
        noteColorStyle = NoteColorStyle.PASTEL
        editorTextSize = EditorTextSize.MEDIUM
        showEditorCounts = true
        categoryOptions = categories
        defaultNoteCategory = "Ideas"
        automaticBackupEnabled = false
        automaticBackupLastRun = 0L
        automaticBackupStatus = "Not run yet"
        AutomaticBackupScheduler.cancel(context)
        coroutineScope.launch { snackbarHostState.showSnackbar("Settings reset to defaults") }
    }
    suspend fun importMarkdownUris(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        // Read and parse documents off the main thread; don't freeze the UI for large batches.
        val parsed = withContext(Dispatchers.IO) {
            uris.map { uri ->
                runCatching {
                    val markdown = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Unable to read Markdown file")
                    val displayName = runCatching {
                        context.contentResolver.query(
                            uri,
                            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
                        }
                    }.getOrNull().orEmpty()
                    val fileName = displayName.ifBlank {
                        uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""
                    }
                    val firstHeading = Regex("(?m)^#\\s+(.+?)\\s*$").find(markdown)
                    val title = firstHeading?.groupValues?.get(1)?.trim().orEmpty()
                        .ifBlank { fileName.ifBlank { "Imported note" } }
                    val body = if (firstHeading != null) markdown.removeRange(firstHeading.range).trim()
                        else markdown.trim()
                    title to body
                }.getOrNull()
            }
        }
        if (parsed.any { it == null }) {
            snackbarHostState.showSnackbar("Couldn't read one or more Markdown files. Nothing was imported.")
            return
        }
        val now = System.currentTimeMillis()
        val usedNotes = notes.toMutableList()
        val imported = parsed.filterNotNull().mapIndexed { index, (title, body) ->
            val timestamp = now + index
            val note = NestNote(
                id = NoteStore.allocateNoteId(usedNotes, timestamp),
                title = title,
                body = body,
                category = defaultNoteCategory,
                updated = timestamp,
                created = timestamp
            )
            usedNotes += note
            note
        }
        if (persist(imported + notes)) {
            selectedView = "Home"
            snackbarHostState.showSnackbar(
                if (imported.size == 1) "Markdown file imported as a new note"
                else "${imported.size} Markdown files imported as new notes"
            )
        }
    }
    val markdownImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        coroutineScope.launch { importMarkdownUris(uris) }
    }
    LaunchedEffect(externalMarkdownUri, storageLoaded) {
        val uri = externalMarkdownUri
        if (storageLoaded && uri != null) {
            importMarkdownUris(listOf(uri))
            onExternalMarkdownHandled()
        }
    }
    var exportArchivedNotes by remember { mutableStateOf(true) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val exportNotes = if (exportArchivedNotes) notes else notes.filterNot { it.archived }
            val saved = runCatching {
                val payload = NoteStore.encodeBackup(exportNotes).toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri)?.use { it.write(payload) } != null
            }.getOrDefault(false)
            if (saved) {
                lastBackupAt = System.currentTimeMillis()
                backupStatus = "Success"
                preferences.edit()
                    .putLong(PREF_BACKUP_LAST_SUCCESS_AT, lastBackupAt)
                    .putString(PREF_BACKUP_LAST_STATUS, backupStatus)
                    .apply()
            } else {
                backupStatus = "Failed"
                saveString(PREF_BACKUP_LAST_STATUS, backupStatus)
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(if (saved) "Backup saved (" + exportNotes.size + " notes). Voice recordings are not included." else "Backup could not be saved. Your notes were not changed.")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            // Validate every selected file before opening the restore preview; never apply a partial batch.
            val batches = uris.map { uri ->
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?.let { NoteStore.decodeBackup(it) }
                }.getOrNull()
            }
            val incoming = if (batches.all { it != null }) {
                batches.filterNotNull().flatten().takeIf { it.isNotEmpty() }
            } else null
            val message = if (incoming == null) {
                "One or more selected files aren't valid NESTNOTE backups. Nothing was imported."
            } else {
                // Deduplicate IDs across files using the same newest-update-wins rule as restore.
                pendingRestore = incoming.fold(emptyList()) { accumulated, batchNote ->
                    NoteStore.merge(accumulated, listOf(batchNote)).notes
                }
                "Validated ${uris.size} backup file(s). Review the combined restore before applying it."
            }
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        }
    }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) nestNoteDarkScheme
    else nestNoteLightScheme

    val filteredNotes = remember(notes, selectedView, search, sortByTitle, sortByCreated, privateVaultUnlocked) {
        NoteFilters.visibleNotes(notes, if (selectedView == "Private" && privateVaultUnlocked) "Private" else selectedView, search, sortByTitle, sortByCreated)
    }

    PredictiveBackHandler(enabled = !editorVisible && selectedView != "Home") { progress ->
        progress.collect { /* Reserve the progress stream for future destination previews. */ }
        selectedView = if (selectedView in moreSubViews) "More" else "Home"
        search = ""
        searchVisible = false
    }

    val expressiveScheme = scheme.copy(
        tertiary = if (dark) Color(0xFF9CCBC1) else Color(0xFF426F67)
    ).let { if (dark && amoled) it.asAmoled() else it }

    MaterialTheme(
        colorScheme = expressiveScheme,
        typography = nestNoteTypography,
        shapes = nestNoteShapes
    ) {
        val colors = MaterialTheme.colorScheme
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.pointerInput(selectedView) {
                                var totalDrag = 0f
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        if (dragAmount > 0f) {
                                            totalDrag += dragAmount
                                            if (totalDrag > 72f && selectedView == "Home") {
                                                totalDrag = 0f
                                                requestPrivateVault()
                                            }
                                        } else if (dragAmount < 0f) totalDrag = 0f
                                    },
                                    onDragEnd = { totalDrag = 0f },
                                    onDragCancel = { totalDrag = 0f }
                                )
                            }
                        ) {
                            Text(
                                when (selectedView) { "Private" -> "Private Vault"; "PrivateVaultSettings" -> "Private Vault settings"; "Settings" -> "Settings"; "Backup" -> "Backup & restore"; "About" -> "About & help"; "Templates" -> "Note templates"; "Insights" -> "Note insights"; "Shortcuts" -> "Keyboard shortcuts"; "More" -> "More"; "Pinned" -> "Pinned notes"; "Archived" -> "Archived notes"; "Categories" -> "Categories"; else -> "NestNote" },
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                            Text(
                                when (selectedView) { "Private" -> "Hidden notes · unlocked"; "PrivateVaultSettings" -> "Authentication and privacy controls"; "Settings" -> "Make your notes feel like yours"; "Backup" -> "Protect and manage your notes"; "About" -> "Version, licence and how NESTNOTE works"; "Templates" -> "A faster start for your next note"; "Insights" -> "Private patterns from your own notes"; "Shortcuts" -> "Hardware keyboard controls"; "More" -> "More ways to organize your notes"; else -> "Your thoughts, organized. · Swipe down to open Private Vault" },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (selectedView == "Private") {
                            IconButton(onClick = { privateVaultUnlocked = false; selectedView = "Home" }) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Lock Private Vault")
                            }
                        } else if (selectedView in moreSubViews) {
                            IconButton(onClick = { selectedView = "More" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to More")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
                )
            },
            bottomBar = {
                if (selectedNoteIds.isNotEmpty()) {
                    val selectedNotes = notes.filter { it.id in selectedNoteIds }
                    BulkSelectionBar(
                        selectedCount = selectedNotes.size,
                        categories = categoryOptions,
                        allPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned },
                        allArchived = selectedNotes.isNotEmpty() && selectedNotes.all { it.archived },
                        onMoveCategory = { category ->
                            if (persist(notes.map { if (it.id in selectedNoteIds) it.copy(category = category) else it })) selectedNoteIds = emptySet()
                        },
                        onTogglePin = {
                            val pin = !(selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned })
                            if (persist(notes.map { if (it.id in selectedNoteIds) it.copy(pinned = pin, updated = System.currentTimeMillis()) else it })) selectedNoteIds = emptySet()
                        },
                        onToggleArchive = {
                            val restore = selectedNotes.isNotEmpty() && selectedNotes.all { it.archived }
                            if (persist(notes.map { if (it.id in selectedNoteIds) it.copy(archived = !restore, pinned = if (restore) it.pinned else false, updated = System.currentTimeMillis()) else it })) selectedNoteIds = emptySet()
                        },
                        onDelete = { confirmBulkDelete = true },
                        onClear = { selectedNoteIds = emptySet() }
                    )
                } else {
                    NestNoteNavBar(
                        selectedView = selectedView,
                        onSelect = { selectedView = it; selectedNoteIds = emptySet() },
                        onNewNote = {
                            if (!storageLoaded) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Notes are still loading. Try again in a moment.") }
                            } else {
                                editorNoteId = null
                                editorDraftId = NoteStore.allocateNoteId(notes)
                                editorTemplate = null
                                editorVisible = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    (if (isExpandedWidth) Modifier.widthIn(max = 840.dp) else Modifier)
                        .fillMaxSize()
                ) {
                if (storageRecoveryRequired) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.errorContainer,
                        contentColor = colors.onErrorContainer
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(22.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("Notes need recovery", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Stored notes couldn't be read. Nothing was overwritten. Restore a backup before creating, editing, or deleting notes.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { selectedView = "Settings" }) { Text("Open") }
                        }
                    }
                }
                if (selectedView == "Home" || selectedView == "All notes" || searchVisible) {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag("note_search_field"),
                        placeholder = { Text("Search · is:pinned · has:voice · after:YYYY-MM-DD") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (search.isNotBlank()) IconButton(
                                onClick = {
                                    val query = search.trim()
                                    if (query !in savedSearches && savedSearches.size < 12) {
                                        savedSearches = savedSearches + query
                                        preferences.edit().putStringSet(PREF_SAVED_SEARCHES, savedSearches.toMutableSet()).apply()
                                    }
                                },
                                enabled = search.trim() in savedSearches || savedSearches.size < 12
                            ) { Icon(Icons.Default.BookmarkAdd, contentDescription = "Save search") }
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
                                onClick = { search = query },
                                label = { Text(query, maxLines = 1) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            savedSearches = savedSearches - query
                                            preferences.edit().putStringSet(PREF_SAVED_SEARCHES, savedSearches.toMutableSet()).apply()
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) { Icon(Icons.Default.Close, contentDescription = "Remove saved search", modifier = Modifier.size(16.dp)) }
                                }
                            )
                        }
                    }
                }
                if (selectedView == "Home" || selectedView == "All notes" || categoryOptions.contains(selectedView)) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 18.dp, end = 18.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedView == "Home" || selectedView == "All notes",
                            onClick = { selectedView = "Home" },
                            label = { Text("All notes") },
                            leadingIcon = if (selectedView == "Home" || selectedView == "All notes") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        categoryOptions.forEach { category ->
                            FilterChip(
                                selected = selectedView == category,
                                onClick = { selectedView = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
                if (selectedView == "More") {
                    MoreScreen(
                        pinnedCount = notes.count { it.pinned && !it.archived && !it.privateNote },
                        archivedCount = notes.count { it.archived && !it.privateNote },
                        categoryCount = categoryOptions.size,
                        versionLabel = AboutInfo.versionLabel(appVersion),
                        onOpen = { selectedView = it; selectedNoteIds = emptySet() }
                    )
                } else if (selectedView == "Templates") {
                    NoteTemplatesScreen { template ->
                        editorTemplate = template
                        editorNoteId = null
                        editorDraftId = NoteStore.allocateNoteId(notes)
                        selectedView = "Home"
                        editorVisible = true
                    }
                } else if (selectedView == "Insights") {
                    NoteInsightsScreen(notes.filterNot { it.privateNote }, onOpenCategory = { selectedView = it }) { noteId ->
                        editorNoteId = noteId
                        editorDraftId = noteId
                        selectedView = "Home"
                        editorVisible = true
                    }
                } else if (selectedView == "Shortcuts") {
                    KeyboardShortcutsScreen()
                } else if (selectedView == "Backup") {
                    BackupRestoreScreen(
                        totalNotes = notes.count { !it.privateNote },
                        archivedNotes = notes.count { it.archived && !it.privateNote },
                        pinnedNotes = notes.count { it.pinned && !it.archived && !it.privateNote },
                        lastBackupAt = lastBackupAt,
                        backupStatus = backupStatus,
                        automaticBackupEnabled = automaticBackupEnabled,
                        automaticBackupLastRun = automaticBackupLastRun,
                        automaticBackupStatus = automaticBackupStatus,
                        onAutomaticBackupChange = { enabled ->
                            automaticBackupEnabled = enabled
                            saveBoolean(PREF_BACKUP_AUTOMATIC_ENABLED, enabled)
                            if (enabled) AutomaticBackupScheduler.schedule(context)
                            else AutomaticBackupScheduler.cancel(context)
                        },
                        onExportNotes = { includeArchived ->
                            exportArchivedNotes = includeArchived
                            exportLauncher.launch("nestnote-backup.json")
                        },
                        onImportNotes = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                    )
                } else if (selectedView == "PrivateVaultSettings") {
                    PrivateVaultSettingsScreen(
                        passwordConfigured = PrivateVaultSecurity.isConfigured(context),
                        biometricAvailable = privateVaultBiometricAvailable,
                        biometricEnabled = privateVaultBiometricEnabled,
                        privateNoteCount = notes.count { it.privateNote },
                        onBiometricEnabledChange = { enabled ->
                            privateVaultBiometricEnabled = enabled
                            preferences.edit().putBoolean(PREF_PRIVATE_VAULT_BIOMETRIC_ENABLED, enabled).apply()
                        },
                        onSetUpPassword = { requestPrivateVault() },
                        onOpenVault = { requestPrivateVault() },
                        onChangePassword = { current, new, confirmation ->
                            val accepted = PrivateVaultSecurity.changePassword(context, current, new, confirmation)
                            if (accepted) coroutineScope.launch { snackbarHostState.showSnackbar("Private Vault password updated") }
                            accepted
                        }
                    )
                } else if (selectedView == "Settings") {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it; saveString(PREF_THEME, it.name) },
                        dynamicColors = dynamicColors,
                        onDynamicColorsChange = { dynamicColors = it; saveBoolean(PREF_DYNAMIC, it) },
                        amoled = amoled,
                        onAmoledChange = { amoled = it; saveBoolean(PREF_AMOLED, it) },
                        dashboardLayout = dashboardLayout,
                        onLayoutChange = { dashboardLayout = it; saveString(PREF_LAYOUT, it.name) },
                        editorMode = editorMode,
                        onEditorModeChange = { editorMode = it; saveString(PREF_EDITOR, it.name) },
                        dashboardDensity = dashboardDensity,
                        onDensityChange = { dashboardDensity = it; saveString(PREF_DENSITY, it.name) },
                        noteColorStyle = noteColorStyle,
                        onNoteColorStyleChange = { noteColorStyle = it; saveString(PREF_NOTE_STYLE, it.name) },
                        sortKey = when { sortByTitle -> "TITLE"; sortByCreated -> "CREATED"; else -> "RECENT" },
                        onSortChange = { setSort(it) },
                        showNotePreviews = showNotePreviews,
                        onShowNotePreviewsChange = { showNotePreviews = it; saveBoolean(PREF_NOTE_PREVIEWS, it) },
                        showNoteTimestamps = showNoteTimestamps,
                        onShowNoteTimestampsChange = { showNoteTimestamps = it; saveBoolean(PREF_NOTE_TIMESTAMPS, it) },
                        defaultNoteCategory = defaultNoteCategory,
                        onDefaultNoteCategoryChange = { defaultNoteCategory = it; saveString(PREF_DEFAULT_CATEGORY, it) },
                        categoryOptions = categoryOptions,
                        onCategoryOptionsChange = { updated ->
                            val removed = categoryOptions.toSet() - updated.toSet()
                            val fallback = updated.firstOrNull() ?: "Other"
                            val notesMoved = removed.isEmpty() || persist(
                                notes.map { if (it.category in removed) it.copy(category = fallback) else it }
                            )
                            if (notesMoved) {
                                if (selectedView in removed) selectedView = "Home"
                                categoryOptions = updated
                                preferences.edit().putStringSet(PREF_CUSTOM_CATEGORIES, updated.toMutableSet()).apply()
                                if (defaultNoteCategory !in updated) {
                                    defaultNoteCategory = updated.firstOrNull() ?: "Ideas"
                                    saveString(PREF_DEFAULT_CATEGORY, defaultNoteCategory)
                                }
                            }
                        },
                        editorTextSize = editorTextSize,
                        onEditorTextSizeChange = { editorTextSize = it; saveString(PREF_TEXT_SIZE, it.name) },
                        showEditorCounts = showEditorCounts,
                        onShowEditorCountsChange = { showEditorCounts = it; saveBoolean(PREF_EDITOR_COUNTS, it) },
                        onResetDefaults = { resetPreferences() },
                        totalNotes = notes.count { !it.privateNote },
                        archivedNotes = notes.count { it.archived },
                        onOpenBackupRestore = { selectedView = "Backup" },
                        onOpenPrivateVaultSettings = { selectedView = "PrivateVaultSettings" }
                    )
                } else if (selectedView == "About") {
                    AboutScreen(version = appVersion)
                } else if (selectedView == "Categories") {
                    val activeCategoryCounts = remember(notes) {
                        categoryOptions.associateWith { category -> notes.count { it.category == category && !it.archived && !it.privateNote } }
                    }
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Categories", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        categoryOptions.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { category ->
                                    Card(
                                        modifier = Modifier.weight(1f).clickable { selectedView = category },
                                        border = BorderStroke(1.dp, colors.outlineVariant)
                                    ) {
                                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                            Text(category, style = MaterialTheme.typography.titleMedium)
                                            Text("${activeCategoryCounts[category] ?: 0} active notes", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedView == "Home") {
                            Text(
                                remember { NoteText.greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) },
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    when (selectedView) {
                                        "All notes" -> "All your notes"
                                        "Pinned" -> "Pinned notes"
                                        "Reminders" -> "Things to remember"
                                        "Archived" -> "Archived notes"
                                        else -> selectedView
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${filteredNotes.size} notes",
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (selectedView == "Home") "Your notes" else "Notes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedView == "Home") {
                                IconButton(
                                    onClick = { markdownImportLauncher.launch(arrayOf("text/markdown", "text/plain", "text/x-markdown", "application/octet-stream")) },
                                    modifier = Modifier.testTag("import_markdown_button")
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = "Import Markdown file")
                                }
                            }
                            IconButton(
                                onClick = {
                                    val next = if (dark) ThemeMode.LIGHT else ThemeMode.DARK
                                    themeMode = next
                                    saveString(PREF_THEME, next.name)
                                }
                            ) {
                                Icon(
                                    if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme"
                                )
                            }
                            var sortMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { sortMenuExpanded = true }) {
                                    Text(when { sortByTitle -> "Title A–Z"; sortByCreated -> "Recently created"; else -> "Recently edited" }, style = MaterialTheme.typography.labelLarge)
                                }
                                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Recently edited") }, onClick = { setSort("RECENT"); sortMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Recently created") }, onClick = { setSort("CREATED"); sortMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Title A–Z") }, onClick = { setSort("TITLE"); sortMenuExpanded = false })
                                }
                            }
                            IconButton(
                                onClick = {
                                    dashboardLayout = if (dashboardLayout == DashboardLayout.GRID) DashboardLayout.LIST else DashboardLayout.GRID
                                    saveString(PREF_LAYOUT, dashboardLayout.name)
                                }
                            ) {
                                Icon(
                                    if (dashboardLayout == DashboardLayout.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                    contentDescription = "Switch dashboard layout"
                                )
                            }
                        }
                    }
                    if (!storageLoaded) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 44.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text("Loading your notes…", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (filteredNotes.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 44.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NestEmptyIllustration(
                                when {
                                    search.isNotBlank() -> Icons.Default.Search
                                    selectedView == "Archived" -> Icons.Default.Archive
                                    selectedView == "Pinned" -> Icons.Default.PushPin
                                    selectedView == "Reminders" -> Icons.Default.NotificationsNone
                                    else -> Icons.Default.Add
                                }
                            )
                            Text(
                                when {
                                    search.isNotBlank() -> "No matching notes"
                                    selectedView == "Archived" -> "Nothing archived yet"
                                    selectedView == "Pinned" -> "No pinned notes yet"
                                    selectedView == "Reminders" -> "No reminders yet"
                                    categoryOptions.contains(selectedView) -> "No $selectedView notes yet"
                                    else -> "A fresh page awaits"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when {
                                    search.isNotBlank() -> "Try another search, or clear it to see all your notes."
                                    selectedView == "Archived" -> "Notes you archive will be kept here, away from your main lists."
                                    selectedView == "Pinned" -> "Pin important notes to keep them easy to find."
                                    selectedView == "Reminders" -> "Add a reminder to a note and it will show up here."
                                    categoryOptions.contains(selectedView) -> "Notes assigned to this category will appear here."
                                    else -> "Create a note and let ideas flow."
                                },
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (search.isNotBlank()) {
                                TextButton(onClick = { search = "" }) { Text("Clear search") }
                            }
                        }
                    } else if (dashboardLayout == DashboardLayout.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 155.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            gridItems(filteredNotes, key = { it.id }) { note ->
                                NoteCard(note, compact = dashboardDensity == DashboardDensity.COMPACT, colorStyle = noteColorStyle, showPreview = showNotePreviews, showTimestamp = showNoteTimestamps,
                                    onOpen = {
                                        if (selectedNoteIds.isNotEmpty()) {
                                            selectedNoteIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id
                                        } else { editorNoteId = note.id; editorDraftId = note.id; editorTemplate = null; editorVisible = true }
                                    },
                                    onSelect = { selectedNoteIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id },
                                    isSelected = note.id in selectedNoteIds,
                                    onPin = { persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it }) },
                                    onDelete = { deleteNote(note) }, onArchive = { persist(notes.map { if (it.id == note.id) it.copy(archived = !it.archived, pinned = if (it.archived) it.pinned else false, updated = System.currentTimeMillis()) else it }) },
                                    modifier = Modifier.animateItem())
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCard(note, compact = dashboardDensity == DashboardDensity.COMPACT, colorStyle = noteColorStyle, showPreview = showNotePreviews, showTimestamp = showNoteTimestamps,
                                    onOpen = {
                                        if (selectedNoteIds.isNotEmpty()) {
                                            selectedNoteIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id
                                        } else { editorNoteId = note.id; editorDraftId = note.id; editorTemplate = null; editorVisible = true }
                                    },
                                    onSelect = { selectedNoteIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id },
                                    isSelected = note.id in selectedNoteIds,
                                    onPin = { persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it }) },
                                    onDelete = { deleteNote(note) }, onArchive = { persist(notes.map { if (it.id == note.id) it.copy(archived = !it.archived, pinned = if (it.archived) it.pinned else false, updated = System.currentTimeMillis()) else it }) },
                                    modifier = Modifier.animateItem())
                            }
                        }
                    }
                }
                }
            }
        }

        if (showOrphanDraftDialog && pendingOrphanDrafts.isNotEmpty()) {
            val newest = pendingOrphanDrafts.first()
            val count = pendingOrphanDrafts.size
            val draftDateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
            val preview = newest.title.ifBlank {
                newest.body.lineSequence().firstOrNull { it.isNotBlank() }?.take(40).orEmpty()
            }.ifBlank { "Untitled" }
            AlertDialog(
                onDismissRequest = { showOrphanDraftDialog = false },
                title = {
                    Text(if (count == 1) "Recover unsaved note?" else "Recover unsaved notes?")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (count == 1) {
                                "A draft from ${draftDateFormatter.format(Date(newest.savedAtMillis))} was found."
                            } else {
                                "$count drafts from previous sessions were found. The most recent is:"
                            }
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceContainerHighest
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(preview, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    draftDateFormatter.format(Date(newest.savedAtMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val recovered = NestNote(
                            id = newest.noteId,
                            title = newest.title,
                            body = newest.body,
                            category = newest.category,
                            color = newest.color,
                            background = newest.background,
                            pinned = newest.pinned,
                            reminder = newest.reminder,
                            wallpaperUri = newest.wallpaperUri,
                            updated = newest.savedAtMillis,
                            created = newest.savedAtMillis
                        )
                        if (persist(notes + recovered)) {
                            runCatching { DraftStore(context).clear(newest.noteId) }
                            pendingOrphanDrafts = pendingOrphanDrafts.drop(1)
                            if (pendingOrphanDrafts.isEmpty()) {
                                showOrphanDraftDialog = false
                            }
                            coroutineScope.launch { snackbarHostState.showSnackbar("Draft recovered") }
                        }
                    }) { Text("Recover") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    pendingOrphanDrafts.forEach {
                                        runCatching { DraftStore(context).clear(it.noteId) }
                                    }
                                }
                            }
                            pendingOrphanDrafts = emptyList()
                            showOrphanDraftDialog = false
                        }) { Text("Discard all") }
                        TextButton(onClick = { showOrphanDraftDialog = false }) { Text("Later") }
                    }
                }
            )
        }

        pendingRestore?.let { incoming ->
            val restoreIncoming = NoteStore.remapDraftIdCollisionForRestore(notes, incoming, editorDraftId)
            val preview = NoteStore.merge(notes, restoreIncoming)
            val withinNoteLimit = NoteStore.isValidNoteCollection(preview.notes)
            AlertDialog(
                onDismissRequest = { pendingRestore = null },
                title = { Text("Review backup restore") },
                text = {
                    Text(
                        if (withinNoteLimit) {
                            "This backup will add ${preview.added} notes and update ${preview.updated} existing notes. Voice recordings are not included. Notes not present in the backup will not be deleted."
                        } else {
                            "This restore would exceed NESTNOTE's ${NoteStore.MAX_NOTES}-note limit. Remove notes or choose a smaller backup before restoring."
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = withinNoteLimit,
                        onClick = {
                            if (persist(preview.notes, allowRecovery = true)) {
                                val restoredCategories = (categoryOptions + preview.notes.map { it.category })
                                    .distinct().filter { it.isNotBlank() }
                                if (restoredCategories != categoryOptions) {
                                    categoryOptions = restoredCategories
                                    preferences.edit().putStringSet(PREF_CUSTOM_CATEGORIES, restoredCategories.toMutableSet()).apply()
                                }
                                pendingRestore = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Restored ${preview.added} new and ${preview.updated} updated notes")
                                }
                            }
                        }
                    ) { Text("Restore notes") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestore = null }) { Text("Cancel") }
                }
            )
        }

        if (showPrivateVaultGate) {
            PrivateVaultGateDialog(
                passwordConfigured = PrivateVaultSecurity.isConfigured(context),
                biometricEnabled = privateVaultBiometricEnabled,
                onDismiss = { showPrivateVaultGate = false },
                onSubmit = { password, confirmation ->
                    val configured = PrivateVaultSecurity.isConfigured(context)
                    val accepted = if (configured) PrivateVaultSecurity.verify(context, password)
                    else password.length >= PrivateVaultSecurity.MIN_PASSWORD_LENGTH && password == confirmation && PrivateVaultSecurity.configure(context, password)
                    if (accepted) {
                        privateVaultUnlocked = true
                        selectedView = "Private"
                        search = ""
                        searchVisible = false
                        selectedNoteIds = emptySet()
                        showPrivateVaultGate = false
                    }
                    accepted
                },
                onBiometricUnlock = {
                    privateVaultUnlocked = true
                    selectedView = "Private"
                    search = ""
                    searchVisible = false
                    selectedNoteIds = emptySet()
                    showPrivateVaultGate = false
                }
            )
        }

        if (showStorageRecoveryDialog) {
            AlertDialog(
                onDismissRequest = { showStorageRecoveryDialog = false },
                title = { Text("Local notes need recovery") },
                text = { Text("NESTNOTE couldn't read the saved notes. The stored data has been left untouched. Open Settings and restore a backup before creating, editing, or deleting notes.") },
                confirmButton = {
                    TextButton(onClick = { showStorageRecoveryDialog = false; selectedView = "Settings" }) {
                        Text("Open backup settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStorageRecoveryDialog = false }) { Text("Later") }
                }
            )
        }

        if (editorVisible && storageLoaded) {
            val closeEditor = {
                editorHasUnsavedChanges = false
                confirmSystemDiscard = false
                editorVisible = false
                editorDraftId = null
                editorTemplate = null
                discardVoiceChanges = {}
                commitVoiceChanges = {}
            }
            val discardEditor: () -> Unit = {
                discardVoiceChanges()
                if (editorNote == null) {
                    editorDraftId?.let { draftId ->
                        runCatching { VoiceAttachmentStore(context).deleteAll(draftId) }
                        runCatching { VoiceAttachmentManifest(context).removeNote(draftId) }
                    }
                }
                closeEditor()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (editorNote == null) {
                            "Draft discarded, including its unsaved voice recordings."
                        } else {
                            "Unsaved changes discarded. Previously saved voice recordings were kept."
                        }
                    )
                }
            }
            val dismissEditor: () -> Unit = { if (editorHasUnsavedChanges) confirmSystemDiscard = true else discardEditor() }
            val saveNote: (NestNote) -> Unit = saveNote@{ draft ->
                val existing = editorNote
                val currentSaved = existing?.let { opened -> notes.firstOrNull { it.id == opened.id } }
                val isStale = existing != null && (
                    currentSaved == null ||
                        !NoteStore.isSameNoteVersion(draft.updated, currentSaved.updated)
                    )
                if (isStale) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("This note changed elsewhere. Reopen it before saving to avoid overwriting newer content.")
                    }
                } else {
                    val savedId = existing?.id ?: editorDraftId ?: NoteStore.allocateNoteId(notes)
                    if (existing == null && NoteStore.hasDraftIdCollision(notes, savedId)) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "This draft ID is now in use. Your draft and recordings are preserved; close and reopen a new note to continue."
                            )
                        }
                        return@saveNote
                    }
                    val updated = draft.copy(
                        id = savedId,
                        updated = System.currentTimeMillis(),
                        privateNote = if (existing == null && selectedView == "Private") true else draft.privateNote
                    )
                    val next = if (existing == null) notes + updated else notes.map {
                        if (it.id == existing.id) updated.copy(id = existing.id) else it
                    }
                    if (persist(next)) {
                        commitVoiceChanges()
                        closeEditor()
                        coroutineScope.launch { snackbarHostState.showSnackbar("Note saved") }
                    }
                }
            }
            val openLinkedNote: (NestNote, NestNote) -> Unit = { current, target ->
                if (current != editorNote) {
                    saveNote(current)
                    if (!editorVisible) {
                        editorNoteId = target.id
                        editorDraftId = target.id
                        editorTemplate = null
                        editorVisible = true
                    }
                } else {
                    editorNoteId = target.id
                    editorDraftId = target.id
                    editorTemplate = null
                    selectedNoteIds = emptySet()
                }
            }
            val duplicateAction: ((NestNote) -> Unit)? = if (editorNote != null) {
                { noteState ->
                    val duplicate = NoteDuplicate.duplicate(
                        source = noteState,
                        existing = notes,
                        keepReminder = false,
                        keepPinned = false
                    )
                    if (persist(notes + duplicate)) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Note duplicated") }
                    }
                }
            } else null
            val editorDeleteAction: (() -> Unit)? = if (editorNote != null) ({
                editorNote?.let { deleteNote(it, closeEditor = true) }
            }) else null
            when (editorMode) {
                EditorPresentationMode.SHEET -> ModalBottomSheet(onDismissRequest = dismissEditor, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
                    NoteEditorContent(editorNote, draftNoteId = editorDraftId ?: NoteStore.allocateNoteId(notes), defaultNoteCategory = defaultNoteCategory, categoryOptions = categoryOptions, defaultNoteFormat = preferences.getString(PREF_DEFAULT_NOTE_FORMAT, "plain") ?: "plain", template = editorTemplate, allNotes = notes, onOpenLinkedNote = openLinkedNote, textSize = editorTextSize, showCounts = showEditorCounts, onSave = saveNote, onDismiss = discardEditor, onDelete = editorDeleteAction, onDuplicate = duplicateAction, onUnsavedChangesChange = { editorHasUnsavedChanges = it }, onRegisterVoiceDiscard = { discardVoiceChanges = it }, onRegisterVoiceCommit = { commitVoiceChanges = it }, modifier = Modifier.fillMaxWidth().imePadding())
                }
                EditorPresentationMode.FULL_SCREEN -> Dialog(onDismissRequest = dismissEditor, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                    Surface(Modifier.fillMaxSize().imePadding(), color = colors.surface) {
                        NoteEditorContent(editorNote, draftNoteId = editorDraftId ?: NoteStore.allocateNoteId(notes), defaultNoteCategory = defaultNoteCategory, categoryOptions = categoryOptions, defaultNoteFormat = preferences.getString(PREF_DEFAULT_NOTE_FORMAT, "plain") ?: "plain", template = editorTemplate, allNotes = notes, onOpenLinkedNote = openLinkedNote, textSize = editorTextSize, showCounts = showEditorCounts, onSave = saveNote, onDismiss = discardEditor, onDelete = editorDeleteAction, onDuplicate = duplicateAction, onUnsavedChangesChange = { editorHasUnsavedChanges = it }, onRegisterVoiceDiscard = { discardVoiceChanges = it }, onRegisterVoiceCommit = { commitVoiceChanges = it }, fullScreen = true, modifier = Modifier.fillMaxSize())
                    }
                }
                EditorPresentationMode.QUICK_DIALOG -> Dialog(
                    onDismissRequest = dismissEditor,
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .heightIn(max = 760.dp)
                            .padding(horizontal = 18.dp, vertical = 24.dp)
                            .imePadding(),
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surface,
                        tonalElevation = 6.dp
                    ) {
                        NoteEditorContent(
                            original = editorNote,
                            draftNoteId = editorDraftId ?: NoteStore.allocateNoteId(notes),
                            defaultNoteCategory = defaultNoteCategory,
                            categoryOptions = categoryOptions,
                            defaultNoteFormat = preferences.getString(PREF_DEFAULT_NOTE_FORMAT, "plain") ?: "plain",
                            template = editorTemplate,
                            allNotes = notes,
                            onOpenLinkedNote = openLinkedNote,
                            textSize = editorTextSize,
                            showCounts = showEditorCounts,
                            onSave = saveNote,
                            onDismiss = discardEditor,
                            onDelete = editorDeleteAction,
                            onDuplicate = duplicateAction,
                            onUnsavedChangesChange = { editorHasUnsavedChanges = it },
                            onRegisterVoiceDiscard = { discardVoiceChanges = it }, onRegisterVoiceCommit = { commitVoiceChanges = it },
                            compactDialog = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (confirmSystemDiscard) {
                AlertDialog(
                    onDismissRequest = { confirmSystemDiscard = false },
                    title = { Text("Discard changes?") },
                    text = {
                        Text(
                            if (editorNote == null) {
                                "Your unsaved note and any voice recordings attached to this new draft will be discarded."
                            } else {
                                "Your unsaved changes and newly staged voice recordings will be discarded. Voice recordings already saved to this note will be kept."
                            }
                        )
                    },
                    confirmButton = { TextButton(onClick = discardEditor) { Text("Discard") } },
                    dismissButton = { TextButton(onClick = { confirmSystemDiscard = false }) { Text("Keep editing") } }
                )
            }
        }
        if (confirmBulkDelete) {
            val deleting = notes.filter { it.id in selectedNoteIds }
            AlertDialog(
                onDismissRequest = { confirmBulkDelete = false },
                title = { Text("Delete ${deleting.size} notes?") },
                text = { Text("This permanently removes the selected notes and their voice recordings.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmBulkDelete = false
                        if (persist(notes.filterNot { it.id in selectedNoteIds })) {
                            deleting.forEach { note ->
                                runCatching { VoiceAttachmentStore(context).deleteAll(note.id) }
                                runCatching { VoiceAttachmentManifest(context).removeNote(note.id) }
                            }
                            selectedNoteIds = emptySet()
                            coroutineScope.launch { snackbarHostState.showSnackbar("Selected notes deleted") }
                        }
                    }) { Text("Delete", color = colors.error) }
                },
                dismissButton = { TextButton(onClick = { confirmBulkDelete = false }) { Text("Cancel") } }
            )
        }

    }
}