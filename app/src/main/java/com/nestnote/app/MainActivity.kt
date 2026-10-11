package com.nestnote.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val fallbackAccent = Color(0xFF625B71)
private val noteAccents = listOf(
    Color(0xFF7567F8), Color(0xFF45A9E8), Color(0xFF58B990),
    Color(0xFFF2B341), Color(0xFFFF8D7D), Color(0xFFED6C9B)
)
private val noteTints = listOf(
    Color(0xFFF0EFFF), Color(0xFFE9F6FF), Color(0xFFEAF9F1),
    Color(0xFFFFF5E4), Color(0xFFFFF0ED), Color(0xFFFFF0F5)
)
private val categories = listOf("Work", "Personal", "Ideas", "Health", "Finance", "Education", "Travel", "Other")
private const val PREFS = "nestnote_preferences"
private const val PREF_THEME = "theme_mode"
private const val PREF_DYNAMIC = "dynamic_colors"
private const val PREF_LAYOUT = "dashboard_layout"
private const val PREF_EDITOR = "editor_mode"
private const val PREF_DENSITY = "dashboard_density"
private const val PREF_NOTE_STYLE = "note_color_style"

private enum class ThemeMode(val label: String) {
    SYSTEM("Follow device"), LIGHT("Light"), DARK("Dark")
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
    var notes by remember { mutableStateOf(store.load()) }
    var themeMode by remember { mutableStateOf(ThemeMode.values().firstOrNull { it.name == preferences.getString(PREF_THEME, "SYSTEM") } ?: ThemeMode.SYSTEM) }
    var dynamicColors by remember { mutableStateOf(preferences.getBoolean(PREF_DYNAMIC, true)) }
    var dashboardLayout by remember { mutableStateOf(DashboardLayout.values().firstOrNull { it.name == preferences.getString(PREF_LAYOUT, "GRID") } ?: DashboardLayout.GRID) }
    var editorMode by remember { mutableStateOf(EditorMode.values().firstOrNull { it.name == preferences.getString(PREF_EDITOR, "SHEET") } ?: EditorMode.SHEET) }
    var dashboardDensity by remember { mutableStateOf(DashboardDensity.values().firstOrNull { it.name == preferences.getString(PREF_DENSITY, "COMPACT") } ?: DashboardDensity.COMPACT) }
    var sortByTitle by remember { mutableStateOf(false) }
    var noteColorStyle by remember { mutableStateOf(NoteColorStyle.values().firstOrNull { it.name == preferences.getString(PREF_NOTE_STYLE, "PASTEL") } ?: NoteColorStyle.PASTEL) }
    var selectedView by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var editorNote by remember { mutableStateOf<NestNote?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var snackbarHost by remember { mutableStateOf<SnackbarHostState?>(null) }

    fun savePreference(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
    fun persist(next: List<NestNote>) {
        notes = next
        store.save(next)
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
        (selectedView == "Home" || selectedView == "All notes" || note.category == selectedView ||
            (selectedView == "Reminders" && note.reminder.isNotBlank())) &&
            (search.isBlank() || (note.title + " " + note.body + " " + note.category).contains(search, true))
    }.let { visible ->
        if (sortByTitle) visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenBy { it.title.lowercase(Locale.getDefault()) })
        else visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.updated })
    }

    MaterialTheme(colorScheme = scheme) {
        val colors = MaterialTheme.colorScheme
        Scaffold(
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
                    ExtendedFloatingActionButton(
                        onClick = { editorNote = null; editorVisible = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("New note") }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "All notes", "Categories", "Reminders", "Settings").forEach { item ->
                        NavigationBarItem(
                            selected = selectedView == item || (item == "All notes" && selectedView !in listOf("Home", "Categories", "Reminders", "Settings") && categories.contains(selectedView)),
                            onClick = { selectedView = item },
                            icon = {
                                Icon(when (item) {
                                    "Home" -> Icons.Default.Home
                                    "Settings" -> Icons.Default.Settings
                                    "Categories" -> Icons.Default.GridView
                                    "All notes" -> Icons.Default.List
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
                        onNoteColorStyleChange = { noteColorStyle = it; savePreference(PREF_NOTE_STYLE, it.name) }
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
                                    Text("Recently edited", style = MaterialTheme.typography.labelLarge)
                                }
                                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Recently edited") }, onClick = { sortByTitle = false; sortMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Title A–Z") }, onClick = { sortByTitle = true; sortMenuExpanded = false })
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
                                NoteCard(note, compact = dashboardDensity == DashboardDensity.COMPACT, colorStyle = noteColorStyle, onOpen = { editorNote = note; editorVisible = true },
                                    onPin = { persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it }) },
                                    onDelete = { persist(notes.filterNot { it.id == note.id }) })
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCard(note, compact = dashboardDensity == DashboardDensity.COMPACT, onOpen = { editorNote = note; editorVisible = true },
                                    onPin = { persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it }) },
                                    onDelete = { persist(notes.filterNot { it.id == note.id }) })
                            }
                        }
                    }
                }
            }
        }

        if (editorVisible) {
            val dismissEditor = { editorVisible = false }
            val saveNote: (NestNote) -> Unit = { updated ->
                val existing = editorNote
                val next = if (existing == null) notes + updated else notes.map { if (it.id == existing.id) updated.copy(id = existing.id) else it }
                persist(next)
                editorVisible = false
                toastMessage = "Note saved"
            }
            val deleteNote: (() -> Unit)? = if (editorNote != null) ({
                persist(notes.filterNot { it.id == editorNote?.id })
                editorVisible = false
            }) else null
            when (editorMode) {
                EditorMode.SHEET -> ModalBottomSheet(onDismissRequest = dismissEditor, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
                    NoteEditorContent(editorNote, onSave = saveNote, onDismiss = dismissEditor, onDelete = deleteNote, modifier = Modifier.fillMaxWidth().imePadding())
                }
                EditorMode.FULL_SCREEN -> Dialog(onDismissRequest = dismissEditor, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                    Surface(Modifier.fillMaxSize().imePadding(), color = colors.surface) {
                        NoteEditorContent(editorNote, onSave = saveNote, onDismiss = dismissEditor, onDelete = deleteNote, fullScreen = true, modifier = Modifier.fillMaxSize())
                    }
                }
                EditorMode.QUICK_DIALOG -> AlertDialog(
                    onDismissRequest = dismissEditor,
                    title = { Text(if (editorNote == null) "Quick note" else "Edit note") },
                    text = {
                        QuickNoteFields(
                            original = editorNote,
                            onSave = saveNote,
                            onDismiss = dismissEditor,
                            onDelete = deleteNote
                        )
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
        }
        if (toastMessage.isNotBlank()) {
            val host = remember { SnackbarHostState() }
            LaunchedEffect(toastMessage) {
                host.showSnackbar(toastMessage)
                toastMessage = ""
            }
            SnackbarHost(hostState = host, modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.BottomCenter))
        }
    }
}

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
    onNoteColorStyleChange: (NoteColorStyle) -> Unit
) {
    val colors = MaterialTheme.colorScheme
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
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                ThemeMode.values().forEach { mode ->
                    Row(Modifier.fillMaxWidth().clickable { onThemeChange(mode) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = themeMode == mode, onClick = { onThemeChange(mode) })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(mode.label)
                            if (mode == ThemeMode.SYSTEM) Text("Follow your Android light/dark setting", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Wallpaper colors", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Use Android dynamic colors from your wallpaper" else "Requires Android 12 or newer; older devices use the app palette",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
                        )
                    }
                    Switch(checked = dynamicColors, onCheckedChange = onDynamicColorsChange)
                }
            }
        }
        item {
            SettingsSection(title = "Dashboard", subtitle = "Choose how your notes appear") {
                DashboardLayout.values().forEach { layout ->
                    Row(Modifier.fillMaxWidth().clickable { onLayoutChange(layout) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = dashboardLayout == layout, onClick = { onLayoutChange(layout) })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(layout.label)
                            Text(if (layout == DashboardLayout.GRID) "Compact cards, side by side" else "Full-width notes with more preview space", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            SettingsSection(title = "Note card colors", subtitle = "Choose your preferred note-card look") {
                NoteColorStyle.values().forEach { style ->
                    Row(Modifier.fillMaxWidth().clickable { onNoteColorStyleChange(style) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = noteColorStyle == style, onClick = { onNoteColorStyleChange(style) })
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(style.label)
                            Text(style.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            SettingsSection(title = "Dashboard density", subtitle = "Choose how much space each note uses") {
                DashboardDensity.values().forEach { density ->
                    Row(Modifier.fillMaxWidth().clickable { onDensityChange(density) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = dashboardDensity == density, onClick = { onDensityChange(density) })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(density.label)
                            Text(density.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            SettingsSection(title = "Note creation", subtitle = "Choose how the editor opens") {
                EditorMode.values().forEach { mode ->
                    Row(Modifier.fillMaxWidth().clickable { onEditorModeChange(mode) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = editorMode == mode, onClick = { onEditorModeChange(mode) })
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(mode.label)
                            Text(mode.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
                Text("Default: expandable bottom sheet", color = colors.primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item {
            SettingsSection(title = "About", subtitle = "Your notes stay on this device") {
                Text("NESTNOTE stores notes locally. Wallpaper-based colors are provided by Android on supported devices.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
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
private fun NoteCard(note: NestNote, compact: Boolean, colorStyle: NoteColorStyle, onOpen: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    // Keep note surfaces and labels legible in wallpaper-derived and fallback schemes.
    val accent = noteAccents[note.color.coerceIn(0, noteAccents.lastIndex)]
    val tint = if (colorStyle == NoteColorStyle.PASTEL) noteTints[note.color.coerceIn(0, noteTints.lastIndex)] else colors.surfaceContainerLow
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tint),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(if (compact) 10.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tint, shape = RoundedCornerShape(8.dp)) {
                    Text(note.category, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = colors.onSurface, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                if (note.pinned) Icon(Icons.Default.PushPin, contentDescription = "Pinned note", tint = colors.primary, modifier = Modifier.size(18.dp))
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, contentDescription = "Actions for " + note.title) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (note.pinned) "Unpin note" else "Pin note") }, onClick = { menu = false; onPin() }, leadingIcon = { Icon(Icons.Default.PushPin, null) })
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onOpen() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Text(note.body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = if (compact) 2 else 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp).weight(1f, fill = false))
            Spacer(Modifier.height(if (compact) 6.dp else 14.dp))
            Text(
                if (note.reminder.isNotBlank()) "◷ ${note.reminder}" else SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updated)),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorContent(
    original: NestNote?,
    onSave: (NestNote) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    fullScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    var title by remember(original?.id) { mutableStateOf(original?.title.orEmpty()) }
    var body by remember(original?.id) { mutableStateOf(original?.body.orEmpty()) }
    var category by remember(original?.id) { mutableStateOf(original?.category ?: "Ideas") }
    var colorIndex by remember(original?.id) { mutableIntStateOf(original?.color ?: 0) }
    var reminder by remember(original?.id) { mutableStateOf(original?.reminder.orEmpty()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(
        modifier = modifier.then(if (fullScreen) Modifier.statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp) else Modifier.padding(horizontal = 20.dp).navigationBarsPadding()).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (original == null) "New note" else "Edit note", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Capture it before it slips away.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) { Text("✕") }
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = body, onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth().then(if (fullScreen) Modifier.weight(1f, fill = false) else Modifier),
            label = { Text("Your note") }, minLines = if (fullScreen) 8 else 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
            OutlinedTextField(
                value = category, onValueChange = {}, readOnly = true, label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryExpanded = false }) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Note color", style = MaterialTheme.typography.labelLarge)
            noteAccents.forEachIndexed { index, color ->
                Box(Modifier.size(28.dp).background(color, CircleShape).clickable { colorIndex = index }, contentAlignment = Alignment.Center) {
                    if (colorIndex == index) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
        }
        OutlinedButton(
            onClick = {
                val now = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day ->
                    TimePickerDialog(context, { _, hour, minute ->
                        reminder = String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d", year, month + 1, day, hour, minute)
                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (reminder.isBlank()) "Add reminder date & time" else "Reminder: $reminder") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(
                onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onSave(NestNote(
                            id = original?.id ?: System.currentTimeMillis(),
                            title = title.trim(), body = body.trim(), category = category, color = colorIndex,
                            pinned = original?.pinned ?: false, reminder = reminder, updated = System.currentTimeMillis()
                        ))
                    }
                },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) { Text("Save note") }
        }
    }
}

@Composable
private fun QuickNoteFields(
    original: NestNote?,
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
                            category = original?.category ?: "Ideas", color = original?.color ?: 0,
                            pinned = original?.pinned ?: false, reminder = original?.reminder.orEmpty(),
                            updated = System.currentTimeMillis()
                        )
                    )
                },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) { Text("Save") }
        }
    }
}
