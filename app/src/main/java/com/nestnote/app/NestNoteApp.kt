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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel


// App shell: theme, navigation, dashboard and screen routing.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun NestNoteApp(store: NoteStore) {
    val context = LocalContext.current
    // Read once per process; PackageManager is the honest version source (BuildConfig is off).
    val appVersion = remember { readAppVersion(context) }
    val windowSizeClass = (context as? ComponentActivity)?.let { calculateWindowSizeClass(it) }
    val isExpandedWidth = windowSizeClass?.widthSizeClass != null &&
        windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val preferences = remember { context.getSharedPreferences(PREFS, 0) }
    // Storage (crypto + JSON) stays off the first composition path. Notes start empty and
    // fill in from Dispatchers.IO; [storageLoaded] gates every write below so a slow or
    // failed load can never overwrite real data with an empty collection.
    var notes by remember { mutableStateOf<List<NestNote>>(emptyList()) }
    var storageLoaded by remember { mutableStateOf(false) }
    var storageRecoveryRequired by remember { mutableStateOf(false) }
    var showStorageRecoveryDialog by remember { mutableStateOf(false) }
    LaunchedEffect(store) {
        val result = withContext(Dispatchers.IO) { store.loadSafely() }
        notes = result.notes
        storageRecoveryRequired = result.recoveryRequired
        showStorageRecoveryDialog = result.recoveryRequired
        storageLoaded = true
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
    var defaultNoteCategory by remember { mutableStateOf(preferences.getString(PREF_DEFAULT_CATEGORY, "Ideas")?.takeIf { it in categories } ?: "Ideas") }
    var noteColorStyle by remember { mutableStateOf(NoteColorStyle.values().firstOrNull { it.name == preferences.getString(PREF_NOTE_STYLE, "PASTEL") } ?: NoteColorStyle.PASTEL) }
    var editorTextSize by remember { mutableStateOf(EditorTextSize.values().firstOrNull { it.name == preferences.getString(PREF_TEXT_SIZE, "MEDIUM") } ?: EditorTextSize.MEDIUM) }
    var showEditorCounts by remember { mutableStateOf(preferences.getBoolean(PREF_EDITOR_COUNTS, true)) }
    var selectedView by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var editorNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Reserve a collision-safe identity as soon as a new draft opens. Attachments can
    // then be staged against the same identity before the note is first saved.
    var editorDraftId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editorHasUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    var discardVoiceChanges by remember { mutableStateOf<() -> Unit>({}) }
    var confirmSystemDiscard by remember { mutableStateOf(false) }
    // Resolve the selected note from persisted notes after recreation.
    val editorNote = notes.firstOrNull { it.id == editorNoteId }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun savePreference(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
    fun persist(next: List<NestNote>, allowRecovery: Boolean = false): Boolean {
        if (!storageLoaded) {
            // Notes are still being decrypted; writing now could persist an empty collection.
            coroutineScope.launch { snackbarHostState.showSnackbar("Notes are still loading. Try again in a moment.") }
            return false
        }
        if (storageRecoveryRequired && !allowRecovery) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Restore a backup before changing notes. Existing local data was not overwritten.") }
            return false
        }
        val saved = store.save(next)
        if (saved) {
            notes = next
            if (allowRecovery) storageRecoveryRequired = false
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Save failed. Your previous notes are still available.") }
        }
        return saved
    }
    fun deleteNote(note: NestNote, closeEditor: Boolean = false) {
        // Do not dismiss the editor or offer Undo unless deletion was persisted.
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
                // Undo keeps the attachment directory intact. Restore the note before any cleanup.
                persist(notes + note)
            } else if (
                result != SnackbarResult.ActionPerformed &&
                !storageRecoveryRequired &&
                notes.none { it.id == note.id }
            ) {
                // The Undo window has expired and deletion remains persisted. Only now is it safe
                // to remove the deleted note's encrypted audio; never clean up during recovery.
                runCatching { VoiceAttachmentStore(context).deleteAll(note.id) }
                runCatching { VoiceAttachmentManifest(context).removeNote(note.id) }
            }
        }
    }
    fun setSort(key: String) {
        sortByTitle = key == "TITLE"
        sortByCreated = key == "CREATED"
        savePreference(PREF_DEFAULT_SORT, key)
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
        coroutineScope.launch { snackbarHostState.showSnackbar("Settings reset to defaults") }
    }
    val markdownImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val markdown = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            if (markdown == null) {
                coroutineScope.launch { snackbarHostState.showSnackbar("Couldn't read that Markdown file") }
            } else {
                val firstHeading = Regex("(?m)^#\\s+(.+?)\\s*$").find(markdown)
                val importedTitle = firstHeading?.groupValues?.get(1)?.trim().orEmpty()
                    .ifBlank { uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Imported note" } ?: "Imported note" }
                val importedBody = if (firstHeading != null) {
                    markdown.removeRange(firstHeading.range).trim()
                } else markdown.trim()
                val imported = NestNote(
                    id = System.currentTimeMillis(),
                    title = importedTitle,
                    body = importedBody,
                    category = defaultNoteCategory,
                    updated = System.currentTimeMillis(),
                    created = System.currentTimeMillis()
                )
                notes = listOf(imported) + notes
                selectedView = "Home"
                coroutineScope.launch { snackbarHostState.showSnackbar("Markdown file imported as a new note") }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(NoteStore.encodeBackup(notes).toByteArray(Charsets.UTF_8)) } != null
            }.getOrDefault(false)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(if (saved) "Notes backup saved (${notes.size} notes). Voice recordings are not included." else "Couldn't save the backup")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            val incoming = raw?.let { NoteStore.decodeBackup(it) }
            val message = if (incoming == null) {
                "That file isn't a valid NESTNOTE backup"
            } else {
                pendingRestore = incoming
                "Backup validated. Review the restore before applying it."
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

    // Recomputed only when an input changes, not on every recomposition of the shell.
    val filteredNotes = remember(notes, selectedView, search, sortByTitle, sortByCreated) {
        NoteFilters.visibleNotes(notes, selectedView, search, sortByTitle, sortByCreated)
    }

    // Complete navigation only after the predictive-back gesture is committed.
    // If the gesture is cancelled, the flow is cancelled and the current destination remains.
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
                        Column {
                            Text(
                                when (selectedView) { "Settings" -> "Settings"; "Backup" -> "Backup & restore"; "About" -> "About & help"; "More" -> "More"; "Pinned" -> "Pinned notes"; "Archived" -> "Archived notes"; "Categories" -> "Categories"; else -> "NestNote" },
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                            Text(
                                when (selectedView) { "Settings" -> "Make your notes feel like yours"; "Backup" -> "Protect and manage your notes"; "About" -> "Version, licence and how NESTNOTE works"; "More" -> "More ways to organize your notes"; else -> "Your thoughts, organized." },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (selectedView in moreSubViews) {
                            IconButton(onClick = { selectedView = "More" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to More")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
                )
            },
            bottomBar = {
                NestNoteNavBar(
                    selectedView = selectedView,
                    onSelect = { selectedView = it },
                    onNewNote = {
                        // Wait for the async load so a draft ID can never collide with a
                        // note that is still being decrypted.
                        if (!storageLoaded) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Notes are still loading. Try again in a moment.") }
                        } else {
                            editorNoteId = null
                            editorDraftId = NoteStore.allocateNoteId(notes)
                            editorVisible = true
                        }
                    }
                )
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
                    // Persistent, calm recovery state: visible after the recovery dialog is dismissed.
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
                        placeholder = { Text("Search your notes") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                // Keep common category filters one tap away on the dashboard.
                if (selectedView == "Home" || selectedView == "All notes" || categories.contains(selectedView)) {
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
                        categories.forEach { category ->
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
                        pinnedCount = notes.count { it.pinned && !it.archived },
                        archivedCount = notes.count { it.archived },
                        categoryCount = categories.size,
                        versionLabel = AboutInfo.versionLabel(appVersion),
                        onOpen = { selectedView = it }
                    )
                } else if (selectedView == "Backup") {
                    BackupRestoreScreen(
                        totalNotes = notes.size,
                        archivedNotes = notes.count { it.archived },
                        onExportNotes = { exportLauncher.launch("nestnote-backup.json") },
                        onImportNotes = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                    )
                } else if (selectedView == "Settings") {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it; savePreference(PREF_THEME, it.name) },
                        dynamicColors = dynamicColors,
                        onDynamicColorsChange = { dynamicColors = it; preferences.edit().putBoolean(PREF_DYNAMIC, it).apply() },
                        amoled = amoled,
                        onAmoledChange = { amoled = it; preferences.edit().putBoolean(PREF_AMOLED, it).apply() },
                        dashboardLayout = dashboardLayout,
                        onLayoutChange = { dashboardLayout = it; savePreference(PREF_LAYOUT, it.name) },
                        editorMode = editorMode,
                        onEditorModeChange = { editorMode = it; savePreference(PREF_EDITOR, it.name) },
                        dashboardDensity = dashboardDensity,
                        onDensityChange = { dashboardDensity = it; savePreference(PREF_DENSITY, it.name) },
                        noteColorStyle = noteColorStyle,
                        onNoteColorStyleChange = { noteColorStyle = it; savePreference(PREF_NOTE_STYLE, it.name) },
                        sortKey = when { sortByTitle -> "TITLE"; sortByCreated -> "CREATED"; else -> "RECENT" },
                        onSortChange = { setSort(it) },
                        showNotePreviews = showNotePreviews,
                        onShowNotePreviewsChange = { showNotePreviews = it; preferences.edit().putBoolean(PREF_NOTE_PREVIEWS, it).apply() },
                        showNoteTimestamps = showNoteTimestamps,
                        onShowNoteTimestampsChange = { showNoteTimestamps = it; preferences.edit().putBoolean(PREF_NOTE_TIMESTAMPS, it).apply() },
                        defaultNoteCategory = defaultNoteCategory,
                        onDefaultNoteCategoryChange = { defaultNoteCategory = it; savePreference(PREF_DEFAULT_CATEGORY, it) },
                        editorTextSize = editorTextSize,
                        onEditorTextSizeChange = { editorTextSize = it; savePreference(PREF_TEXT_SIZE, it.name) },
                        showEditorCounts = showEditorCounts,
                        onShowEditorCountsChange = { showEditorCounts = it; preferences.edit().putBoolean(PREF_EDITOR_COUNTS, it).apply() },
                        onResetDefaults = { resetPreferences() },
                        totalNotes = notes.size,
                        archivedNotes = notes.count { it.archived },
                        onOpenBackupRestore = { selectedView = "Backup" }
                    )
                } else if (selectedView == "About") {
                    AboutScreen(version = appVersion)
                } else if (selectedView == "Categories") {
                    val activeCategoryCounts = remember(notes) {
                        categories.associateWith { category -> notes.count { it.category == category && !it.archived } }
                    }
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Categories", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        categories.chunked(2).forEach { pair ->
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
                            // Prototype parity: calm time-of-day greeting above the notes heading.
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
                            // Prototype parity: direct theme toggle routed through the existing
                            // themeMode preference state — no second persistence path.
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
                                    savePreference(PREF_THEME, next.name)
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
                                    savePreference(PREF_LAYOUT, dashboardLayout.name)
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
                        // Calm loading state: never flashes the empty state before decryption finishes.
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
                            Surface(
                                shape = CircleShape,
                                color = colors.secondaryContainer,
                                contentColor = colors.onSecondaryContainer
                            ) {
                                Icon(
                                    when {
                                        search.isNotBlank() -> Icons.Default.Search
                                        selectedView == "Archived" -> Icons.Default.Archive
                                        selectedView == "Pinned" -> Icons.Default.PushPin
                                        selectedView == "Reminders" -> Icons.Default.NotificationsNone
                                        else -> Icons.Default.Add
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.padding(18.dp).size(30.dp)
                                )
                            }
                            Text(
                                when {
                                    search.isNotBlank() -> "No matching notes"
                                    selectedView == "Archived" -> "Nothing archived yet"
                                    selectedView == "Pinned" -> "No pinned notes yet"
                                    selectedView == "Reminders" -> "No reminders yet"
                                    categories.contains(selectedView) -> "No $selectedView notes yet"
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
                                    categories.contains(selectedView) -> "Notes assigned to this category will appear here."
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
                                NoteCard(note, compact = dashboardDensity == DashboardDensity.COMPACT, colorStyle = noteColorStyle, showPreview = showNotePreviews, showTimestamp = showNoteTimestamps, onOpen = { editorNoteId = note.id; editorVisible = true },
                                    onPin = { persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it }) },
                                    onDelete = { deleteNote(note) }, onArchive = { persist(notes.map { if (it.id == note.id) it.copy(archived = !it.archived, pinned = if (it.archived) it.pinned else false, updated = System.currentTimeMillis()) else it }) })
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCard(note, compact = dashboardDensity == DashboardDensity.COMPACT, colorStyle = noteColorStyle, showPreview = showNotePreviews, showTimestamp = showNoteTimestamps, onOpen = { editorNoteId = note.id; editorVisible = true },
                                    onPin = { persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it }) },
                                    onDelete = { deleteNote(note) }, onArchive = { persist(notes.map { if (it.id == note.id) it.copy(archived = !it.archived, pinned = if (it.archived) it.pinned else false, updated = System.currentTimeMillis()) else it }) })
                            }
                        }
                    }
                }
                }
            }
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

        // The editor waits for storage so a restored note is never mistaken for a new draft.
        if (editorVisible && storageLoaded) {
            val closeEditor = {
                editorHasUnsavedChanges = false
                confirmSystemDiscard = false
                editorVisible = false
                editorDraftId = null
                discardVoiceChanges = {}
            }
            val discardEditor: () -> Unit = {
                discardVoiceChanges()
                // New-note attachments are staged under the reserved draft ID. Discard
                // removes both payloads and index entries; saved-note attachments are untouched.
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
                // Compare the editor's baseline against the latest collection, not the
                // editor's own snapshot, which naturally remains unchanged while typing.
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
                    // Never silently change identity after audio is staged against this ID.
                    // Restore remaps colliding incoming drafts; if another writer still creates
                    // a collision, preserve this draft and ask the user to reopen safely.
                    if (existing == null && NoteStore.hasDraftIdCollision(notes, savedId)) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "This draft ID is now in use. Your draft and recordings are preserved; close and reopen a new note to continue."
                            )
                        }
                        return@saveNote
                    }
                    val updated = draft.copy(id = savedId, updated = System.currentTimeMillis())
                    val next = if (existing == null) notes + updated else notes.map {
                        if (it.id == existing.id) updated.copy(id = existing.id) else it
                    }
                    if (persist(next)) {
                        closeEditor()
                        coroutineScope.launch { snackbarHostState.showSnackbar("Note saved") }
                    }
                }
            }
            val editorDeleteAction: (() -> Unit)? = if (editorNote != null) ({
                editorNote?.let { deleteNote(it, closeEditor = true) }
            }) else null
            when (editorMode) {
                EditorPresentationMode.SHEET -> ModalBottomSheet(onDismissRequest = dismissEditor, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
                    NoteEditorContent(editorNote, draftNoteId = editorDraftId ?: NoteStore.allocateNoteId(notes), defaultNoteCategory = defaultNoteCategory, textSize = editorTextSize, showCounts = showEditorCounts, onSave = saveNote, onDismiss = discardEditor, onDelete = editorDeleteAction, onUnsavedChangesChange = { editorHasUnsavedChanges = it }, onRegisterVoiceDiscard = { discardVoiceChanges = it }, modifier = Modifier.fillMaxWidth().imePadding())
                }
                EditorPresentationMode.FULL_SCREEN -> Dialog(onDismissRequest = dismissEditor, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                    Surface(Modifier.fillMaxSize().imePadding(), color = colors.surface) {
                        NoteEditorContent(editorNote, draftNoteId = editorDraftId ?: NoteStore.allocateNoteId(notes), defaultNoteCategory = defaultNoteCategory, textSize = editorTextSize, showCounts = showEditorCounts, onSave = saveNote, onDismiss = discardEditor, onDelete = editorDeleteAction, onUnsavedChangesChange = { editorHasUnsavedChanges = it }, onRegisterVoiceDiscard = { discardVoiceChanges = it }, fullScreen = true, modifier = Modifier.fillMaxSize())
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
                            textSize = editorTextSize,
                            showCounts = showEditorCounts,
                            onSave = saveNote,
                            onDismiss = dismissEditor,
                            onDelete = editorDeleteAction,
                            onUnsavedChangesChange = { editorHasUnsavedChanges = it },
                            onRegisterVoiceDiscard = { discardVoiceChanges = it },
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
    }
}
