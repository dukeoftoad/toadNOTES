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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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


// Settings screen and its reusable setting controls.

/**
 * Keys for every item in the settings LazyColumn, in display order.
 * Declaring them in one place keeps programmatic scrolling stable when sections
 * are added, removed, or reordered.
 */
private val SettingsItemKeys = listOf(
    "settings_header",
    "settings_appearance",
    "settings_layout",
    "settings_behavior",
    "settings_editor",
    "settings_backup",
    "settings_reset"
)

/** Sort choices are immutable and never change, so keep them off the composition. */
private val SortOptions: List<Pair<String, String>> = listOf(
    "RECENT" to "Edited",
    "CREATED" to "Created",
    "TITLE" to "Title"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    dynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    amoled: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    dashboardLayout: DashboardLayout,
    onLayoutChange: (DashboardLayout) -> Unit,
    editorMode: EditorPresentationMode,
    onEditorModeChange: (EditorPresentationMode) -> Unit,
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
    onResetDefaults: () -> Unit,
    totalNotes: Int,
    archivedNotes: Int,
    modifier: Modifier = Modifier,
    onOpenBackupRestore: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var settingsSearch by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val settingsScope = rememberCoroutineScope()
    val settingsDestinations = listOf(
        "Appearance" to 1,
        "Layout" to 2,
        "Note behavior" to 3,
        "Note editor" to 4,
        "Data" to 5
    )

    // Resolve gracefully if a persisted sort key is no longer valid.
    val selectedSort = SortOptions.firstOrNull { it.first == sortKey } ?: SortOptions.first()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_scroll"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = SettingsItemKeys[0]) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Make NESTNOTE feel like yours.", color = colors.onSurfaceVariant)
            OutlinedTextField(
                value = settingsSearch,
                onValueChange = { settingsSearch = it },
                modifier = Modifier.fillMaxWidth().testTag("settings_search"),
                singleLine = true,
                label = { Text("Search settings") },
                placeholder = { Text("Try theme, sort, text size…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (settingsSearch.isNotBlank()) {
                        IconButton(onClick = { settingsSearch = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear settings search")
                        }
                    }
                }
            )
            if (settingsSearch.isBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Jump to", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settingsDestinations.forEach { (label, index) ->
                    AssistChip(
                        onClick = {
                            settingsScope.launch { listState.animateScrollToItem(index) }
                        },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (label) {
                                    "Appearance" -> Icons.Default.Palette
                                    "Layout" -> Icons.Default.GridView
                                    "Note behavior" -> Icons.Default.Settings
                                    "Note editor" -> Icons.Default.Edit
                                    else -> Icons.Default.Archive
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                }
            }
        }
        if (settingsSearch.isBlank() || listOf("appearance", "theme", "wallpaper", "color", "colour", "dark", "oled", "dynamic").any { settingsSearch.contains(it, ignoreCase = true) }) {
        item(key = SettingsItemKeys[1]) {
            SettingsSection(title = "Appearance", subtitle = "Colors that fit your device") {
                SettingLabel("Theme")
                SegmentedChoice(ThemeMode.entries, themeMode, { it.label }, onThemeChange)
                if (themeMode == ThemeMode.SYSTEM) {
                    Text(
                        "Follows your Android light/dark setting",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SwitchRow(
                    title = "Wallpaper colors",
                    summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "Use Android dynamic colors from your wallpaper"
                    } else {
                        "Requires Android 12 or newer; older devices use the app palette"
                    },
                    checked = dynamicColors,
                    onChange = onDynamicColorsChange
                )
                SwitchRow(
                    title = "Pure black in dark mode",
                    summary = "Saves battery on OLED screens. Applies when the theme is dark.",
                    checked = amoled,
                    onChange = onAmoledChange
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Note card colors")
                Column(Modifier.selectableGroup()) {
                    NoteColorStyle.entries.forEach { style ->
                        ChoiceCard(
                            title = style.label,
                            description = style.description,
                            selected = noteColorStyle == style,
                            onClick = { onNoteColorStyleChange(style) }
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Custom note wallpapers")
                Text(
                    "Open any note and select the palette tool. In the Colour tab, tap + to choose an image from your device. The wallpaper is saved with that note; use the Background tab to return to an illustrated background.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = colors.onSecondaryContainer)
                        Column(Modifier.weight(1f)) {
                            Text("Personalize each note", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
                            Text("Choose a color and background independently for each note.", style = MaterialTheme.typography.bodySmall, color = colors.onSecondaryContainer)
                        }
                    }
                }
            }
        }
        }
        if (settingsSearch.isBlank() || listOf("layout", "dashboard", "density", "grid", "list", "compact").any { settingsSearch.contains(it, ignoreCase = true) }) {
        item(key = SettingsItemKeys[2]) {
            SettingsSection(title = "Layout", subtitle = "Choose how your notes appear") {
                SettingLabel("Dashboard view")
                SegmentedChoice(DashboardLayout.entries, dashboardLayout, { it.label }, onLayoutChange)
                Text(
                    if (dashboardLayout == DashboardLayout.GRID) {
                        "Compact cards, side by side"
                    } else {
                        "Full-width notes with more preview space"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Dashboard density")
                SegmentedChoice(DashboardDensity.entries, dashboardDensity, { it.label }, onDensityChange)
                Text(dashboardDensity.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        }
        if (settingsSearch.isBlank() || listOf("behavior", "category", "sort", "pin", "preview", "date", "reminder", "notes").any { settingsSearch.contains(it, ignoreCase = true) }) {
        item(key = SettingsItemKeys[3]) {
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
                SegmentedChoice(SortOptions, selectedSort, { it.second }, { onSortChange(it.first) })
                Text("Pinned notes always stay on top.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SwitchRow(
                    title = "Show note previews",
                    summary = "Display the first lines of each note on its card",
                    checked = showNotePreviews,
                    onChange = onShowNotePreviewsChange
                )
                SwitchRow(
                    title = "Show dates and reminders",
                    summary = "Display the edit date or reminder on each card",
                    checked = showNoteTimestamps,
                    onChange = onShowNoteTimestampsChange
                )
            }
        }
        }
        if (settingsSearch.isBlank() || listOf("editor", "writing", "text", "size", "word", "reading", "count").any { settingsSearch.contains(it, ignoreCase = true) }) {
        item(key = SettingsItemKeys[4]) {
            SettingsSection(title = "Note editor", subtitle = "Choose how writing feels") {
                SettingLabel("Open editor as")
                Column(Modifier.selectableGroup()) {
                    EditorPresentationMode.entries.forEach { mode ->
                        ChoiceCard(
                            title = mode.label,
                            description = mode.description,
                            selected = editorMode == mode,
                            onClick = { onEditorModeChange(mode) }
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingLabel("Text size")
                SegmentedChoice(EditorTextSize.entries, editorTextSize, { it.label }, onEditorTextSizeChange)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Your notes will look like this.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = editorTextSize.bodySp.sp,
                        color = colors.onSurface
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SwitchRow(
                    title = "Word count and reading time",
                    summary = "Show a live count under the note while you write",
                    checked = showEditorCounts,
                    onChange = onShowEditorCountsChange
                )
            }
        }
        }
        if (settingsSearch.isBlank() || listOf("data", "backup", "restore", "export", "import", "archive").any { settingsSearch.contains(it, ignoreCase = true) }) {
        item(key = SettingsItemKeys[5]) {
            SettingsSection(title = "Data", subtitle = "Backup and manage your notes") {
                Text(
                    "$totalNotes notes stored on this device ($archivedNotes archived).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Keep a portable backup of your notes or restore from a backup file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onOpenBackupRestore,
                    modifier = Modifier.fillMaxWidth().testTag("settings_open_backup_restore")
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Backup & restore")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
        }
        item(key = SettingsItemKeys[6]) {
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth().testTag("settings_reset"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
            ) {
                Text("Reset settings to defaults")
            }
        }
        if (settingsSearch.isNotBlank() && listOf("appearance", "theme", "wallpaper", "color", "colour", "dark", "oled", "dynamic", "layout", "dashboard", "density", "grid", "list", "compact", "behavior", "category", "sort", "pin", "preview", "date", "reminder", "notes", "editor", "writing", "text", "size", "word", "reading", "count", "data", "backup", "restore", "export", "import", "archive").none { settingsSearch.contains(it, ignoreCase = true) }) {
            item(key = "settings_no_search_results") {
                Text("No matching settings", style = MaterialTheme.typography.titleMedium)
                Text("Try searching for theme, layout, category, text size, or backup.", color = colors.onSurfaceVariant)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = { Text("Theme, layout, editor and note defaults go back to their original values. Your notes are not changed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetDefaults()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
internal fun SettingLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(
                    text = label(option),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // The row owns the toggle semantics so tapping the switch itself cannot
        // invoke the same state callback twice through nested click handlers.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun ChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Radio-button semantics: screen readers announce selected state once,
            // and the card joins the surrounding selectableGroup().
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.secondaryContainer else colors.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.primary else colors.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (selected) {
                // Decorative: the selectable modifier already exposes selection state.
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
            }
        }
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
internal fun OverviewTile(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
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
/** Dedicated data-management destination, kept separate from general preferences. */
@Composable
internal fun BackupRestoreScreen(
    totalNotes: Int,
    archivedNotes: Int,
    pinnedNotes: Int,
    lastBackupAt: Long,
    backupStatus: String,
    automaticBackupEnabled: Boolean,
    onAutomaticBackupChange: (Boolean) -> Unit,
    onExportNotes: (Boolean) -> Unit,
    onImportNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var showBackupDetails by rememberSaveable { mutableStateOf(false) }
    var includeArchivedInBackup by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Your notes, in your control",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Keep a copy of your notes or bring notes back from a backup file.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackupStatTile(
                label = "Active",
                count = (totalNotes - archivedNotes).coerceAtLeast(0),
                icon = Icons.Default.Edit,
                modifier = Modifier.weight(1f)
            )
            BackupStatTile(
                label = "Archived",
                count = archivedNotes,
                icon = Icons.Default.Archive,
                modifier = Modifier.weight(1f)
            )
            BackupStatTile(
                label = "Pinned",
                count = pinnedNotes,
                icon = Icons.Default.PushPin,
                modifier = Modifier.weight(1f)
            )
        }

        SettingsSection(title = "Automatic backup", subtitle = "Scheduled copies stay on this device") {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onAutomaticBackupChange(!automaticBackupEnabled) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = automaticBackupEnabled, onCheckedChange = onAutomaticBackupChange)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable daily automatic backup")
                    Text("Saves the latest copy in NESTNOTE's private app storage.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Text(
                if (automaticBackupEnabled) "Last run: shown in Backup status. Android may delay background work; backups run only when the system permits. The private copy is not uploaded to cloud storage and may be removed if the app is uninstalled." else "Next run is not scheduled. Enable this option to create a daily local backup.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        SettingsSection(title = "Privacy and portability", subtitle = "Understand where your backup goes") {
            Text(
                "Manual exports are saved only to the location you choose in Android's save dialog. Exporting does not upload or sync a file to the cloud; a cloud provider is involved only if you explicitly choose its location.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "The JSON backup has a format identifier, schema version, creation timestamp, and named note fields so future versions can validate and migrate it. Password-protected export is not offered yet; keep exported files somewhere private.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        SettingsSection(title = "Backup contents", subtitle = "Know what travels with your backup") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                Text("Note titles, text, categories, and note settings")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                Text("Pinned and archived note state")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = colors.error)
                Text("Voice recordings and audio attachments are not included")
            }
        }

        SettingsSection(title = "Backup status", subtitle = "Know when your notes were last protected") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (backupStatus == "Success") Icons.Default.Check else Icons.Default.Archive, contentDescription = null, tint = if (backupStatus == "Success") colors.primary else colors.onSurfaceVariant)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (lastBackupAt > 0L) "Last backup: " + java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault()).format(java.util.Date(lastBackupAt)) else "No backup recorded yet", fontWeight = FontWeight.SemiBold)
                    Text(if (lastBackupAt > 0L) "$backupStatus · backup file saved to the chosen destination" else "Create a backup and save it somewhere safe.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }

        SettingsSection(title = "Back up notes", subtitle = "Save a portable copy") {
            Text(
                "Create a NESTNOTE backup file that you can keep or move to another device.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Includes note text and note settings. Voice recordings and audio attachments are not included.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().clickable { includeArchivedInBackup = !includeArchivedInBackup }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = includeArchivedInBackup, onCheckedChange = { includeArchivedInBackup = it })
                Column(modifier = Modifier.weight(1f)) {
                    Text("Include archived notes")
                    Text("Turn off to export active notes only", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            FilledTonalButton(
                onClick = { onExportNotes(includeArchivedInBackup) },
                modifier = Modifier.fillMaxWidth().testTag("settings_backup")
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create backup")
            }
        }

        SettingsSection(title = "Backup activity", subtitle = "Latest operation") {
            Text(
                if (lastBackupAt > 0L) "Most recent backup: " + java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault()).format(java.util.Date(lastBackupAt)) + " · " + backupStatus
                else "No completed backup operation has been recorded yet.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("A failed export does not update this timestamp. Restore results are reported after a restore is applied.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }

        SettingsSection(title = "Restore a backup", subtitle = "Review before applying changes") {
            Text(
                "Choose a NESTNOTE backup file to inspect and review its changes before restoring.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = onImportNotes,
                modifier = Modifier.fillMaxWidth().testTag("settings_restore")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose backup file")
            }
            TextButton(
                onClick = { showBackupDetails = !showBackupDetails },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showBackupDetails) "Hide restore rules" else "How restore works")
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (showBackupDetails) Icons.Default.Close else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = showBackupDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• Your existing notes are not cleared.\n• Notes in the backup are merged with local notes.\n• When the same note exists in both places, the most recently edited version is kept.\n• You can cancel at the review step before changes are applied.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun BackupStatTile(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary)
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}
