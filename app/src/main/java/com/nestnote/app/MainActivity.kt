package com.nestnote.app

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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

private val fallbackAccent = Color(0xFF625B71)
// Distinct, slightly richer swatches remain soft while being easy to tell apart.
private val noteAccents = listOf(
    Color(0xFFF4EEE3), Color(0xFFE88C98), Color(0xFFE98555),
    Color(0xFFD8C34D), Color(0xFFA6CC91), Color(0xFF69B9B3), Color(0xFF73ACD8)
)
private val noteTints = listOf(
    Color(0xFFFFFCF6), Color(0xFFFFE8E9), Color(0xFFFFE8DC),
    Color(0xFFFFF6CC), Color(0xFFEBF5E5), Color(0xFFE0F4F1), Color(0xFFE5F1FC)
)
private val categories = listOf("Work", "Personal", "Ideas", "Health", "Finance", "Education", "Travel", "Other")
private const val PREFS = "nestnote_preferences"
private const val PREF_THEME = "theme_mode"
private const val PREF_DYNAMIC = "dynamic_colors"
private const val PREF_LAYOUT = "dashboard_layout"
private const val PREF_EDITOR = "editor_mode"
private const val PREF_DENSITY = "dashboard_density"
private const val PREF_NOTE_STYLE = "note_color_style"
private const val PREF_DEFAULT_SORT = "default_sort"
private const val PREF_NOTE_PREVIEWS = "show_note_previews"
private const val PREF_NOTE_TIMESTAMPS = "show_note_timestamps"
private const val PREF_DEFAULT_CATEGORY = "default_note_category"
private const val PREF_TEXT_SIZE = "editor_text_size"
private const val PREF_EDITOR_COUNTS = "editor_show_counts"

private enum class ThemeMode(val label: String) {
    SYSTEM("Device"), LIGHT("Light"), DARK("Dark")
}
private enum class EditorTextSize(val label: String, val titleSp: Int, val bodySp: Int) {
    SMALL("Small", 24, 14), MEDIUM("Medium", 27, 16), LARGE("Large", 31, 19)
}
private enum class DashboardLayout(val label: String) {
    GRID("Grid"), LIST("List")
}
private enum class DashboardDensity(val label: String, val description: String) {
    COMPACT("Compact", "Tighter cards to see more notes"),
    COMFORTABLE("Comfortable", "More spacing and breathing room")
}
private enum class NoteColorStyle(val label: String, val description: String) {
    PASTEL("Soft pastel cards", "Gentle note colors like the design reference"),
    ADAPTIVE("Match wallpaper", "Use your current Material You theme colors")
}
private enum class EditorMode(val label: String, val description: String) {
    SHEET("Expandable bottom sheet", "A compact editor that expands as you write"),
    FULL_SCREEN("Full-screen editor", "A distraction-free writing space"),
    QUICK_DIALOG("Quick dialog", "A small dialog for short notes")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = NoteStore(this)
        setContent { NestNoteApp(store) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NestNoteApp(store: NoteStore) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFS, 0) }
    val initialNoteLoad = remember { store.loadSafely() }
    var notes by remember { mutableStateOf(initialNoteLoad.notes) }
    var storageRecoveryRequired by remember { mutableStateOf(initialNoteLoad.recoveryRequired) }
    var showStorageRecoveryDialog by remember { mutableStateOf(initialNoteLoad.recoveryRequired) }
    var pendingRestore by remember { mutableStateOf<List<NestNote>?>(null) }
    var themeMode by remember { mutableStateOf(ThemeMode.values().firstOrNull { it.name == preferences.getString(PREF_THEME, "SYSTEM") } ?: ThemeMode.SYSTEM) }
    var dynamicColors by remember { mutableStateOf(preferences.getBoolean(PREF_DYNAMIC, true)) }
    var dashboardLayout by remember { mutableStateOf(DashboardLayout.values().firstOrNull { it.name == preferences.getString(PREF_LAYOUT, "GRID") } ?: DashboardLayout.GRID) }
    var editorMode by remember { mutableStateOf(EditorMode.values().firstOrNull { it.name == preferences.getString(PREF_EDITOR, "FULL_SCREEN") } ?: EditorMode.FULL_SCREEN) }
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
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editorHasUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    var confirmSystemDiscard by remember { mutableStateOf(false) }
    // Resolve the selected note from persisted notes after recreation.
    val editorNote = notes.firstOrNull { it.id == editorNoteId }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun savePreference(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
    fun persist(next: List<NestNote>, allowRecovery: Boolean = false): Boolean {
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
        persist(notes.filterNot { it.id == note.id })
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
        dashboardLayout = DashboardLayout.GRID
        editorMode = EditorMode.FULL_SCREEN
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
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(NoteStore.encodeBackup(notes).toByteArray(Charsets.UTF_8)) } != null
            }.getOrDefault(false)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(if (saved) "Backup saved (${notes.size} notes)" else "Couldn't save the backup")
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
    } else if (dark) darkColorScheme(primary = Color(0xFFD0BCFF), secondary = Color(0xFFCCC2DC))
    else lightColorScheme(primary = fallbackAccent, secondary = Color(0xFF625B71))

    val filteredNotes = notes.filter { note ->
        ((selectedView == "Archived" && note.archived) ||
            (selectedView != "Archived" && !note.archived && (selectedView == "Home" || selectedView == "All notes" || note.category == selectedView ||
            (selectedView == "Reminders" && note.reminder.isNotBlank())))) &&
            (search.isBlank() || (note.title + " " + note.body + " " + note.category).contains(search, true))
    }.let { visible ->
        when {
            sortByTitle -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenBy { it.title.lowercase(Locale.getDefault()) })
            sortByCreated -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.created })
            else -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.updated })
        }
    }

    MaterialTheme(colorScheme = scheme) {
        val colors = MaterialTheme.colorScheme
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("NestNote", fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
                            Text("Your thoughts, organized.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectedView = "Settings" }) {
                            Icon(Icons.Default.Settings, contentDescription = "Open settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
                )
            },
            floatingActionButton = {
                if (selectedView != "Settings" && selectedView != "Categories") {
                    FloatingActionButton(
                        onClick = { editorNoteId = null; editorVisible = true },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create note", modifier = Modifier.size(28.dp))
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "All notes", "Categories", "Reminders", "Archived", "Settings").forEach { item ->
                        NavigationBarItem(
                            selected = selectedView == item || (item == "All notes" && selectedView !in listOf("Home", "Categories", "Reminders", "Archived", "Settings") && categories.contains(selectedView)),
                            onClick = { selectedView = item },
                            icon = {
                                Icon(when (item) {
                                    "Home" -> Icons.Default.Home
                                    "Settings" -> Icons.Default.Settings
                                    "Categories" -> Icons.Default.GridView
                                    "All notes" -> Icons.Default.List
                                    "Archived" -> Icons.Default.Delete
                                    else -> Icons.Default.Check
                                }, contentDescription = item)
                            },
                            label = { Text(item, maxLines = 1, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (selectedView == "Home" || selectedView == "All notes" || searchVisible) {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
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
                if (selectedView == "Settings") {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it; savePreference(PREF_THEME, it.name) },
                        dynamicColors = dynamicColors,
                        onDynamicColorsChange = { dynamicColors = it; preferences.edit().putBoolean(PREF_DYNAMIC, it).apply() },
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
                        totalNotes = notes.size,
                        archivedNotes = notes.count { it.archived },
                        onExportNotes = { exportLauncher.launch("nestnote-backup.json") },
                        onImportNotes = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        onResetDefaults = { resetPreferences() }
                    )
                } else if (selectedView == "Categories") {
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
                                            Text("${notes.count { it.category == category }} notes", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
                            Spacer(Modifier.height(2.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    when (selectedView) {
                                        "All notes" -> "All your notes"
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
                                    if (dashboardLayout == DashboardLayout.GRID) Icons.Default.List else Icons.Default.GridView,
                                    contentDescription = "Switch dashboard layout"
                                )
                            }
                        }
                    }
                    if (filteredNotes.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("A fresh page awaits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("Create a note and let ideas flow.", color = colors.onSurfaceVariant)
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

        pendingRestore?.let { incoming ->
            val preview = NoteStore.merge(notes, incoming)
            AlertDialog(
                onDismissRequest = { pendingRestore = null },
                title = { Text("Review backup restore") },
                text = {
                    Text("This backup will add ${preview.added} notes and update ${preview.updated} existing notes. Notes not present in the backup will not be deleted.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (persist(preview.notes, allowRecovery = true)) {
                            pendingRestore = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Restored ${preview.added} new and ${preview.updated} updated notes")
                            }
                        }
                    }) { Text("Restore notes") }
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

        if (editorVisible) {
            val closeEditor = { editorHasUnsavedChanges = false; confirmSystemDiscard = false; editorVisible = false }
            val dismissEditor = { if (editorHasUnsavedChanges) confirmSystemDiscard = true else closeEditor() }
            val saveNote: (NestNote) -> Unit = { draft ->
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
                    val updated = draft.copy(updated = System.currentTimeMillis())
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
                EditorMode.SHEET -> ModalBottomSheet(onDismissRequest = dismissEditor, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
                    NoteEditorContent(editorNote, defaultNoteCategory = defaultNoteCategory, textSize = editorTextSize, showCounts = showEditorCounts, onSave = saveNote, onDismiss = closeEditor, onDelete = editorDeleteAction, onUnsavedChangesChange = { editorHasUnsavedChanges = it }, modifier = Modifier.fillMaxWidth().imePadding())
                }
                EditorMode.FULL_SCREEN -> Dialog(onDismissRequest = dismissEditor, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                    Surface(Modifier.fillMaxSize().imePadding(), color = colors.surface) {
                        NoteEditorContent(editorNote, defaultNoteCategory = defaultNoteCategory, textSize = editorTextSize, showCounts = showEditorCounts, onSave = saveNote, onDismiss = closeEditor, onDelete = editorDeleteAction, onUnsavedChangesChange = { editorHasUnsavedChanges = it }, fullScreen = true, modifier = Modifier.fillMaxSize())
                    }
                }
                EditorMode.QUICK_DIALOG -> AlertDialog(
                    onDismissRequest = dismissEditor,
                    title = { Text(if (editorNote == null) "Quick note" else "Edit note") },
                    text = {
                        QuickNoteFields(
                            original = editorNote,
                            defaultCategory = defaultNoteCategory,
                            onSave = saveNote,
                            onDismiss = dismissEditor,
                            onDelete = editorDeleteAction
                        )
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
            if (confirmSystemDiscard) {
                AlertDialog(
                    onDismissRequest = { confirmSystemDiscard = false },
                    title = { Text("Discard changes?") },
                    text = { Text("Your unsaved changes will be lost.") },
                    confirmButton = { TextButton(onClick = closeEditor) { Text("Discard") } },
                    dismissButton = { TextButton(onClick = { confirmSystemDiscard = false }) { Text("Keep editing") } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    dynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    dashboardLayout: DashboardLayout,
    onLayoutChange: (DashboardLayout) -> Unit,
    editorMode: EditorMode,
    onEditorModeChange: (EditorMode) -> Unit,
    dashboardDensity: DashboardDensity,
    onDensityChange: (DashboardDensity) -> Unit,
    noteColorStyle: NoteColorStyle,
    onNoteColorStyleChange: (NoteColorStyle) -> Unit,
    sortKey: String,
    onSortChange: (String) -> Unit,
    showNotePreviews: Boolean,
    onShowNotePreviewsChange: (Boolean) -> Unit,
    showNoteTimestamps: Boolean,
    onShowNoteTimestampsChange: (Boolean) -> Unit,
    defaultNoteCategory: String,
    onDefaultNoteCategoryChange: (String) -> Unit,
    editorTextSize: EditorTextSize,
    onEditorTextSizeChange: (EditorTextSize) -> Unit,
    showEditorCounts: Boolean,
    onShowEditorCountsChange: (Boolean) -> Unit,
    totalNotes: Int,
    archivedNotes: Int,
    onExportNotes: () -> Unit,
    onImportNotes: () -> Unit,
    onResetDefaults: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var confirmReset by remember { mutableStateOf(false) }
    val sortOptions = listOf("RECENT" to "Edited", "CREATED" to "Created", "TITLE" to "Title")
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_scroll"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Make NESTNOTE feel like yours.", color = colors.onSurfaceVariant)
        }
        item {
            SettingsSection(title = "Appearance", subtitle = "Colors that fit your device") {
                SettingLabel("Theme")
                SegmentedChoice(ThemeMode.values().toList(), themeMode, { it.label }, onThemeChange)
                if (themeMode == ThemeMode.SYSTEM) {
                    Text("Follows your Android light/dark setting", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SwitchRow(
                    title = "Wallpaper colors",
                    summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Use Android dynamic colors from your wallpaper" else "Requires Android 12 or newer; older devices use the app palette",
                    checked = dynamicColors,
                    onChange = onDynamicColorsChange
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Note card colors")
                NoteColorStyle.values().forEach { style ->
                    ChoiceCard(style.label, style.description, noteColorStyle == style) { onNoteColorStyleChange(style) }
                }
            }
        }
        item {
            SettingsSection(title = "Layout", subtitle = "Choose how your notes appear") {
                SettingLabel("Dashboard view")
                SegmentedChoice(DashboardLayout.values().toList(), dashboardLayout, { it.label }, onLayoutChange)
                Text(
                    if (dashboardLayout == DashboardLayout.GRID) "Compact cards, side by side" else "Full-width notes with more preview space",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Dashboard density")
                SegmentedChoice(DashboardDensity.values().toList(), dashboardDensity, { it.label }, onDensityChange)
                Text(dashboardDensity.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        item {
            SettingsSection(title = "Note behavior", subtitle = "Defaults for new and existing notes") {
                SettingLabel("Default category for new notes")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = defaultNoteCategory == category,
                            onClick = { onDefaultNoteCategoryChange(category) },
                            label = { Text(category) }
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Sort notes by")
                SegmentedChoice(sortOptions, sortOptions.first { it.first == sortKey }, { it.second }, { onSortChange(it.first) })
                Text("Pinned notes always stay on top.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SwitchRow("Show note previews", "Display the first lines of each note on its card", showNotePreviews, onShowNotePreviewsChange)
                SwitchRow("Show dates and reminders", "Display the edit date or reminder on each card", showNoteTimestamps, onShowNoteTimestampsChange)
            }
        }
        item {
            SettingsSection(title = "Note editor", subtitle = "Choose how writing feels") {
                SettingLabel("Open editor as")
                EditorMode.values().forEach { mode ->
                    ChoiceCard(mode.label, mode.description, editorMode == mode) { onEditorModeChange(mode) }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Text size")
                SegmentedChoice(EditorTextSize.values().toList(), editorTextSize, { it.label }, onEditorTextSizeChange)
                Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Your notes will look like this.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = editorTextSize.bodySp.sp,
                        color = colors.onSurface
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SwitchRow("Word count and reading time", "Show a live count under the note while you write", showEditorCounts, onShowEditorCountsChange)
            }
        }
        item {
            SettingsSection(title = "Backup & privacy", subtitle = "Your notes stay on this device") {
                Text(
                    "$totalNotes notes stored on this device ($archivedNotes archived).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "NESTNOTE does not sync to a cloud account. Back up your notes to a file you choose, and restore it any time. Restoring adds missing notes and keeps the most recently edited copy of notes that already exist; nothing is deleted.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onExportNotes, modifier = Modifier.weight(1f)) { Text("Back up notes") }
                    OutlinedButton(onClick = onImportNotes, modifier = Modifier.weight(1f)) { Text("Restore backup") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset settings to defaults")
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = { Text("Theme, layout, editor and note defaults go back to their original values. Your notes are not changed.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onResetDefaults() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label(option), maxLines = 1)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, summary: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.secondaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = colors.primary)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun OverviewTile(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoteCard(note: NestNote, compact: Boolean, colorStyle: NoteColorStyle, showPreview: Boolean, showTimestamp: Boolean, onOpen: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit, onArchive: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorContent(
    original: NestNote?,
    defaultNoteCategory: String,
    textSize: EditorTextSize,
    showCounts: Boolean,
    onSave: (NestNote) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onUnsavedChangesChange: (Boolean) -> Unit = {},
    fullScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    var title by rememberSaveable(original?.id) { mutableStateOf(original?.title.orEmpty()) }
    var bodyValue by rememberSaveable(original?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(original?.body.orEmpty()))
    }
    val body = bodyValue.text
    var category by rememberSaveable(original?.id) { mutableStateOf(original?.category ?: defaultNoteCategory) }
    var colorIndex by rememberSaveable(original?.id) { mutableIntStateOf(original?.color?.coerceIn(0, noteAccents.lastIndex) ?: 0) }
    var backgroundIndex by rememberSaveable(original?.id) { mutableIntStateOf(original?.background?.coerceIn(0, 5) ?: 0) }
    var reminder by rememberSaveable(original?.id) { mutableStateOf(original?.reminder.orEmpty()) }
    var pinned by rememberSaveable(original?.id) { mutableStateOf(original?.pinned ?: false) }
    var appearanceVisible by remember { mutableStateOf(false) }
    var discardConfirmation by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val ink = Color(0xFF292633)
    val muted = Color(0xFF66616D)
    val accent = Color(0xFF514783)
    val hasUnsavedChanges = title != original?.title.orEmpty() ||
        body != original?.body.orEmpty() ||
        category != (original?.category ?: defaultNoteCategory) ||
        colorIndex != (original?.color ?: 0) ||
        backgroundIndex != (original?.background ?: 0) ||
        pinned != (original?.pinned ?: false) ||
        reminder != original?.reminder.orEmpty()

    fun requestDismiss() {
        if (hasUnsavedChanges) discardConfirmation = true else onDismiss()
    }
    fun buildNote(archived: Boolean = original?.archived ?: false): NestNote {
        val now = System.currentTimeMillis()
        return NestNote(
            id = original?.id ?: now,
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
            .background(if (backgroundIndex == 0) noteTints[colorIndex] else Color.White)
    ) {
        if (backgroundIndex > 0) {
            NoteWallpaperArtwork(backgroundIndex, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(noteTints[colorIndex].copy(alpha = 0.58f)))
        }
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { requestDismiss() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close editor", tint = Color(0xFF45434A))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { pinned = !pinned }) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = if (pinned) "Unpin note" else "Pin note",
                        tint = if (pinned) accent else Color(0xFF55515C)
                    )
                }
                Box {
                    IconButton(onClick = { reminderMenu = true }) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = "Choose reminder time",
                            tint = if (reminder.isNotBlank()) accent else Color(0xFF55515C)
                        )
                    }
                    DropdownMenu(expanded = reminderMenu, onDismissRequest = { reminderMenu = false }) {
                        DropdownMenuItem(text = { Text("Later today") }, onClick = {
                            reminderMenu = false; reminder = NoteText.formatReminder(NoteText.laterToday(Calendar.getInstance()))
                        })
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
                        Icon(Icons.Default.Archive, contentDescription = "Archive note", tint = Color(0xFF55515C))
                    }
                }
                IconButton(onClick = { appearanceVisible = true }) {
                    Icon(Icons.Default.Palette, contentDescription = "Choose note appearance", tint = accent)
                }
                Box {
                    IconButton(onClick = { moreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More editor actions", tint = Color(0xFF55515C))
                    }
                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
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

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                    placeholder = { Text("Title") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = textSize.titleSp.sp, lineHeight = (textSize.titleSp * 1.26f).sp, fontWeight = FontWeight.Normal
                    ),
                    singleLine = false,
                    shape = RoundedCornerShape(0.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
                                        onCheckedChange = { checked ->
                                            bodyValue = bodyValue.copy(text = NoteText.setChecklistItemChecked(bodyValue.text, item.lineIndex, checked))
                                        }
                                    )
                                    Text(
                                        item.text.ifBlank { "(empty item)" },
                                        modifier = Modifier.weight(1f).clickable {
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
                OutlinedTextField(
                    value = bodyValue,
                    onValueChange = { onBodyChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    placeholder = { Text("Note") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = textSize.bodySp.sp, lineHeight = (textSize.bodySp * 1.45f).sp
                    ),
                    minLines = if (fullScreen) 10 else 5,
                    shape = RoundedCornerShape(0.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Spacer(Modifier.height(12.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium, color = muted, modifier = Modifier.padding(horizontal = 12.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.55f),
                                labelColor = ink,
                                selectedContainerColor = accent,
                                selectedLabelColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0x33514783))
                        )
                    }
                }
                if (reminder.isNotBlank()) {
                    Surface(
                        onClick = { pickReminder() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.6f)
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
                                Icon(Icons.Default.Close, contentDescription = "Clear reminder", tint = Color(0xFF55515C))
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
                Spacer(Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { applyLineEdit { text, cursor -> NoteText.cycleChecklist(text, cursor) } }) {
                    Icon(Icons.Default.CheckBox, contentDescription = "Toggle checklist item", tint = Color(0xFF45434A))
                }
                IconButton(onClick = { applyLineEdit { text, cursor -> NoteText.toggleBullet(text, cursor) } }) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Toggle bullet point", tint = Color(0xFF45434A))
                }
                Spacer(Modifier.weight(1f))
                if (showCounts) {
                    val words = NoteText.wordCount(body)
                    val minutes = NoteText.readingMinutes(words)
                    Text(
                        buildString {
                            append(if (words == 1) "1 word" else "$words words")
                            append(" · ${body.length} characters")
                            if (minutes > 0) append(" · $minutes min read")
                        },
                        modifier = Modifier.padding(end = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = muted
                    )
                }
            }
            HorizontalDivider(color = Color(0x1F6F6878))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { requestDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text("Cancel") }
                Button(
                    onClick = { if (title.isNotBlank()) onSave(buildNote()) },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2DFE9),
                        disabledContentColor = accent
                    )
                ) { Text("Save note") }
            }
        }
    }

    if (appearanceVisible) {
        AppearancePickerSheet(
            colorIndex = colorIndex,
            backgroundIndex = backgroundIndex,
            onColorSelected = { colorIndex = it },
            onBackgroundSelected = { backgroundIndex = it },
            onDismiss = { appearanceVisible = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearancePickerSheet(
    colorIndex: Int,
    backgroundIndex: Int,
    onColorSelected: (Int) -> Unit,
    onBackgroundSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFBF4),
        contentColor = Color(0xFF211F26),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFBDB5A6)) }
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Box(Modifier.fillMaxWidth().height(258.dp)) {
                if (backgroundIndex == 0) {
                    Box(Modifier.fillMaxSize().background(Color(0xFFF9F4EA)))
                } else {
                    NoteWallpaperArtwork(backgroundIndex, Modifier.fillMaxSize())
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Colour", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    noteAccents.forEachIndexed { index, accent ->
                        Box(
                            Modifier.size(52.dp).clip(CircleShape)
                                .testTag("note_color_$index")
                                .background(accent)
                                .border(
                                    width = if (index == colorIndex) 2.dp else 1.dp,
                                    color = if (index == colorIndex) Color(0xFF514783) else Color(0xFFB8B1A7),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == colorIndex) {
                                Icon(Icons.Default.Check, contentDescription = "Selected note color", tint = Color(0xFF514783), modifier = Modifier.size(25.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))
                Text("Background", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0..5).forEach { index ->
                        Box(
                            Modifier.size(70.dp).clip(CircleShape)
                                .testTag("note_background_$index")
                                .border(
                                    width = if (index == backgroundIndex) 2.dp else 1.dp,
                                    color = if (index == backgroundIndex) Color(0xFF5A538A) else Color(0x66807870),
                                    shape = CircleShape
                                )
                                .clickable { onBackgroundSelected(index) }
                        ) {
                            if (index == 0) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawRect(Color(0xFFF9F4EA))
                                    drawLine(Color(0xFF77716A), Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.72f, size.height * 0.72f), strokeWidth = 3.dp.toPx())
                                }
                            } else {
                                NoteWallpaperArtwork(index, Modifier.fillMaxSize())
                            }
                            if (index == backgroundIndex) {
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd).size(23.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF514783),
                                    contentColor = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFFFF6E5))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected background", modifier = Modifier.padding(3.dp))
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun NoteWallpaperArtwork(index: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (index) {
            1 -> {
                drawRect(Color(0xFFE7E6D4))
                drawCircle(Color(0xFFF3D6A4), radius = w * 0.16f, center = Offset(w * 0.78f, h * 0.24f))
                drawOval(Color(0xFF7E8E6C), Offset(w * -0.12f, h * 0.66f), Size(w * 0.70f, h * 0.45f))
                drawOval(Color(0xFF586E51), Offset(w * 0.22f, h * 0.53f), Size(w * 0.48f, h * 0.55f))
                drawOval(Color(0xFF8C9A72), Offset(w * 0.56f, h * 0.60f), Size(w * 0.50f, h * 0.46f))
                for (i in 0..8) {
                    val x = w * (0.02f + i * 0.12f)
                    val y = h * (0.56f - (i % 3) * 0.12f)
                    drawLine(Color(0xFF52694D), Offset(x, h), Offset(x + w * 0.02f, y + h * 0.20f), strokeWidth = w * 0.008f)
                    drawOval(if (i % 2 == 0) Color(0xFF718560) else Color(0xFF9EAA81), Offset(x - w * 0.08f, y), Size(w * 0.18f, h * 0.13f))
                }
            }
            2 -> {
                drawRect(Color(0xFFF2D7D0))
                for (i in -4..12) {
                    val x = w * (i / 12f)
                    drawLine(Color(0xFFFFF2E5), Offset(x, h * 0.08f), Offset(x + w * 0.72f, h * 0.88f), strokeWidth = w * 0.07f)
                    drawLine(Color(0xFFEABAB1), Offset(x + w * 0.08f, h * 0.08f), Offset(x + w * 0.80f, h * 0.88f), strokeWidth = w * 0.012f)
                }
            }
            3 -> {
                drawRect(Color(0xFFB7DDE6))
                drawCircle(Color(0xFFF3B27F), radius = w * 0.16f, center = Offset(w * 0.68f, h * 0.34f))
                val farWave = Path().apply {
                    moveTo(0f, h * 0.58f); cubicTo(w * 0.32f, h * 0.45f, w * 0.61f, h * 0.68f, w, h * 0.52f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(farWave, Color(0xFF87BACD))
                val nearWave = Path().apply {
                    moveTo(0f, h * 0.74f); cubicTo(w * 0.33f, h * 0.60f, w * 0.71f, h * 0.85f, w, h * 0.71f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(nearWave, Color(0xFF6296AE))
                drawOval(Color(0xFFECCCA4), Offset(w * 0.04f, h * 0.79f), Size(w * 0.46f, h * 0.13f))
            }
            4 -> {
                drawRect(Color(0xFFF8E6C4))
                drawCircle(Color(0xFFE9A77F), radius = w * 0.18f, center = Offset(w * 0.76f, h * 0.27f))
                val backDune = Path().apply {
                    moveTo(0f, h * 0.68f); cubicTo(w * 0.30f, h * 0.40f, w * 0.48f, h * 0.77f, w, h * 0.48f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(backDune, Color(0xFFE7B78B))
                val frontDune = Path().apply {
                    moveTo(0f, h * 0.82f); cubicTo(w * 0.36f, h * 0.67f, w * 0.56f, h * 0.92f, w, h * 0.73f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(frontDune, Color(0xFFD48E69))
            }
            else -> {
                drawRect(Color(0xFFC7E0E3))
                drawCircle(Color(0xFFF2B7A0), radius = w * 0.18f, center = Offset(w * 0.26f, h * 0.28f))
                val hill = Path().apply {
                    moveTo(0f, h * 0.60f); cubicTo(w * 0.25f, h * 0.72f, w * 0.48f, h * 0.49f, w, h * 0.65f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(hill, Color(0xFF70A9B4))
                val foreground = Path().apply {
                    moveTo(0f, h * 0.82f); cubicTo(w * 0.35f, h * 0.68f, w * 0.7f, h * 0.94f, w, h * 0.76f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(foreground, Color(0xFF568A96))
            }
        }
    }
}

@Composable
private fun QuickNoteFields(
    original: NestNote?,
    defaultCategory: String,
    onSave: (NestNote) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember(original?.id) { mutableStateOf(original?.title.orEmpty()) }
    var body by remember(original?.id) { mutableStateOf(original?.body.orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
        OutlinedTextField(body, { body = it }, label = { Text("Note") }, minLines = 3)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(
                onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) onSave(
                        NestNote(
                            id = original?.id ?: System.currentTimeMillis(), title = title.trim(), body = body.trim(),
                            category = original?.category ?: defaultCategory, color = original?.color ?: 0,
                            pinned = original?.pinned ?: false, reminder = original?.reminder.orEmpty(),
                            updated = original?.updated ?: 0L, created = original?.created ?: System.currentTimeMillis(),
                            archived = original?.archived ?: false, background = original?.background ?: 0
                        )
                    )
                },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) { Text("Save") }
        }
    }
}
