package com.nestnote.app

import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Keys for every item in the settings LazyColumn, in display order.
 * Declaring them in one place keeps programmatic scrolling stable.
 */
private val SettingsItemKeys = listOf(
    "settings_header",
    "settings_appearance",
    "settings_layout",
    "settings_behavior",
    "settings_editor",
    "settings_voice_player_section_v2",
    "settings_privacy",
    "settings_backup",
    "settings_reset"
)

/** Sort choices are immutable and never change. */
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
    accentPalette: AccentPalette = AccentPalette.DEFAULT,
    onAccentPaletteChange: (AccentPalette) -> Unit = {},
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
    categoryOptions: List<String> = categories,
    onCategoryOptionsChange: (List<String>) -> Unit = {},
    editorTextSize: EditorTextSize,
    onEditorTextSizeChange: (EditorTextSize) -> Unit,
    defaultMarkdownZoomPercent: Int,
    onDefaultMarkdownZoomPercentChange: (Int) -> Unit,
    showEditorCounts: Boolean,
    onShowEditorCountsChange: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    totalNotes: Int,
    archivedNotes: Int,
    modifier: Modifier = Modifier,
    onOpenBackupRestore: () -> Unit = {},
    onOpenPrivateVaultSettings: () -> Unit = {},
    storageBackend: NoteStorageBackend = NoteStorageBackend.KEYSTORE,
    onStorageBackendChange: (NoteStorageBackend) -> Unit = {},
    screenCaptureProtectionEnabled: Boolean = true,
    onScreenCaptureProtectionChange: (Boolean) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val reducedMotion = rememberSystemReducedMotion()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var pendingStorageBackend by remember { mutableStateOf<NoteStorageBackend?>(null) }
    var settingsSearch by rememberSaveable { mutableStateOf("") }
    var showVoicePlayerSettings by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val settingsScope = rememberCoroutineScope()
    val editorPreferences = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var customTextSizeSp by rememberSaveable {
        mutableIntStateOf(editorPreferences.getInt(PREF_TEXT_SIZE_SP, editorTextSize.bodySp).coerceIn(12, 32))
    }
    var editorFontStyle by rememberSaveable {
        mutableStateOf(runCatching {
            EditorFontFamily.valueOf(editorPreferences.getString(PREF_EDITOR_FONT, "SANS") ?: "SANS")
        }.getOrDefault(EditorFontFamily.SANS))
    }
    var customFontPath by rememberSaveable { mutableStateOf(editorPreferences.getString(PREF_EDITOR_FONT_FILE, null)) }
    var defaultNoteFormat by rememberSaveable {
        mutableStateOf(editorPreferences.getString(PREF_DEFAULT_NOTE_FORMAT, "plain") ?: "plain")
    }
    var categoryDraft by rememberSaveable { mutableStateOf("") }
    val fontImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val tempFile = File(context.filesDir, "editor-font-import.tmp")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 8L * 1024 * 1024) { "Font files must be 8 MB or smaller" }
                            output.write(buffer, 0, count)
                        }
                    }
                } ?: error("Couldn't read that font file")
                AndroidTypeface.createFromFile(tempFile)
                val installed = File(context.filesDir, "editor-font-${System.currentTimeMillis()}.font")
                check(tempFile.renameTo(installed)) { "Couldn't save that font" }
                customFontPath?.let { File(it).delete() }
                customFontPath = installed.absolutePath
                editorFontStyle = EditorFontFamily.CUSTOM
                editorPreferences.edit()
                    .putString(PREF_EDITOR_FONT_FILE, installed.absolutePath)
                    .putString(PREF_EDITOR_FONT, EditorFontFamily.CUSTOM.name)
                    .apply()
                android.widget.Toast.makeText(context, "Custom font installed", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                File(context.filesDir, "editor-font-import.tmp").delete()
                android.widget.Toast.makeText(
                    context,
                    error.message ?: "Choose a valid TTF or OTF font file",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val settingsDestinations = remember {
        listOf(
            Triple("Appearance", 1, Icons.Default.Palette),
            Triple("Layout", 2, Icons.Default.GridView),
            Triple("Notes & search", 3, Icons.Default.Search),
            Triple("Note editor", 4, Icons.Default.Code),
            Triple("Voice player", 5, Icons.Default.Mic),
            Triple("Privacy", 6, Icons.Default.Lock),
            Triple("Data", 7, Icons.Default.Archive)
        )
    }

    // Intelligent search predicate: handles partial strings, section names, and option keywords
    fun matchesSearch(section: String): Boolean {
        if (settingsSearch.isBlank()) return true
        val query = settingsSearch.trim().lowercase()
        return when (section) {
            "appearance" -> listOf(
                "appearance", "theme", "device", "light", "dark", "wallpaper",
                "color", "colour", "amoled", "oled", "pure black", "dynamic", "pastel", "adaptive"
            ).any { it.contains(query) || query.contains(it) }
            "layout" -> listOf(
                "layout", "dashboard", "grid", "list", "density", "compact", "comfortable", "view"
            ).any { it.contains(query) || query.contains(it) }
            "behavior" -> listOf(
                "notes & search", "note behavior", "behavior", "category", "default category",
                "sort", "pin", "pinned", "preview", "date", "reminder", "timestamp"
            ).any { it.contains(query) || query.contains(it) }
            "editor" -> listOf(
                "editor", "note editor", "markdown", "writing", "reading", "text size",
                "size", "font", "typeface", "templates", "shortcuts", "word count", "bottom sheet", "full-screen", "quick dialog"
            ).any { it.contains(query) || query.contains(it) }
            "voice" -> listOf(
                "voice", "player", "voice player", "audio", "playback", "mic", "compact", "full", "waveform"
            ).any { it.contains(query) || query.contains(it) }
            "privacy" -> listOf(
                "privacy", "local", "permissions", "cloud", "security", "device", "control", "account"
            ).any { it.contains(query) || query.contains(it) }
            "data" -> listOf(
                "data", "backup", "restore", "export", "import", "archive", "archived", "portable"
            ).any { it.contains(query) || query.contains(it) }
            else -> false
        }
    }

    val anySearchMatch = remember(settingsSearch) {
        settingsSearch.isBlank() || listOf(
            "appearance", "layout", "behavior", "editor", "voice", "privacy", "data"
        ).any { matchesSearch(it) }
    }

    val activeSettingsCategory = settingsDestinations
        .lastOrNull { listState.firstVisibleItemIndex >= it.second }
        ?.first ?: "Appearance"

    // Resolve gracefully if a persisted sort key is no longer valid.
    val selectedSort = SortOptions.firstOrNull { it.first == sortKey } ?: SortOptions.first()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_scroll"),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- 0. Expressive Hero Header & Search ----
        item(key = SettingsItemKeys[0]) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsHeroCard(
                    totalNotes = totalNotes,
                    archivedNotes = archivedNotes,
                    themeLabel = themeMode.label,
                    darkTheme = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                )

                OutlinedTextField(
                    value = settingsSearch,
                    onValueChange = { settingsSearch = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_search"),
                    singleLine = true,
                    label = { Text("Search settings") },
                    placeholder = { Text("Try theme, Markdown, privacy…") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.primary
                        )
                    },
                    trailingIcon = {
                        if (settingsSearch.isNotBlank()) {
                            IconButton(onClick = { settingsSearch = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear settings search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceContainerLow,
                        unfocusedContainerColor = colors.surfaceContainerLow,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outlineVariant
                    )
                )

                AnimatedVisibility(
                    visible = settingsSearch.isBlank(),
                    enter = fadeIn(NestMotion.fast(reducedMotion)) + slideInVertically(
                        animationSpec = NestMotion.spatial(reducedMotion),
                        initialOffsetY = { -it / 3 }
                    ),
                    exit = fadeOut(NestMotion.fast(reducedMotion)) + slideOutVertically(
                        animationSpec = NestMotion.fast(reducedMotion),
                        targetOffsetY = { -it / 3 }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("settings_category_rail")
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settingsDestinations.forEach { (label, index, icon) ->
                            val selected = activeSettingsCategory == label
                            val chipContainer by animateColorAsState(
                                targetValue = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
                                animationSpec = NestMotion.fast(reducedMotion),
                                label = "category-chip-container"
                            )
                            val chipContent by animateColorAsState(
                                targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                animationSpec = NestMotion.fast(reducedMotion),
                                label = "category-chip-content"
                            )

                            FilterChip(
                                selected = selected,
                                onClick = {
                                    settingsScope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                },
                                label = {
                                    Text(
                                        label,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = chipContent
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = chipContainer,
                                    selectedContainerColor = chipContainer,
                                    labelColor = chipContent,
                                    selectedLabelColor = chipContent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.primary else colors.outlineVariant
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---- 1. Appearance Section ----
        if (matchesSearch("appearance")) {
            item(key = SettingsItemKeys[1]) {
                SettingsSection(
                    title = "Appearance",
                    subtitle = "Colors and themes that match your style",
                    icon = Icons.Default.Palette
                ) {
                    SettingLabel("Theme")

                    // Visual theme graphics cards
                    ThemeModeSelector(
                        selected = themeMode,
                        onSelect = onThemeChange,
                        reducedMotion = reducedMotion
                    )

                    if (themeMode == ThemeMode.SYSTEM) {
                        Text(
                            "Follows your Android light/dark setting",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

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

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("App accent colour")
                    Text(
                        "Choose the colour used for buttons, highlights and selected controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentPalette.values().toList().chunked(4).forEach { paletteRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                paletteRow.forEach { palette ->
                                    val selectedPalette = accentPalette == palette
                                    val swatch = if (themeMode == ThemeMode.DARK ||
                                        (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                                    ) palette.primaryDark else palette.primaryLight
                                    Surface(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                            .clickable { onAccentPaletteChange(palette) }
                                            .semantics { contentDescription = "${palette.label} accent colour${if (selectedPalette) ", selected" else ""}" },
                                        color = if (selectedPalette) colors.secondaryContainer else colors.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            if (selectedPalette) 2.dp else 1.dp,
                                            if (selectedPalette) colors.primary else colors.outlineVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(28.dp).clip(CircleShape).background(swatch),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (selectedPalette) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = if (swatch.luminance() > 0.45f) Color.Black else Color.White, modifier = Modifier.size(17.dp))
                                                }
                                            }
                                            Text(palette.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                    }
                                }
                                repeat((4 - paletteRow.size).coerceAtLeast(0)) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (accentPalette != AccentPalette.DEFAULT && dynamicColors) {
                        Text(
                            "Selecting a custom accent disables wallpaper colours so your palette stays consistent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Note card colors")
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NoteColorStyleCard(
                            title = NoteColorStyle.PASTEL.label,
                            description = NoteColorStyle.PASTEL.description,
                            selected = noteColorStyle == NoteColorStyle.PASTEL,
                            onClick = { onNoteColorStyleChange(NoteColorStyle.PASTEL) },
                            isPastel = true,
                            reducedMotion = reducedMotion
                        )
                        NoteColorStyleCard(
                            title = NoteColorStyle.ADAPTIVE.label,
                            description = NoteColorStyle.ADAPTIVE.description,
                            selected = noteColorStyle == NoteColorStyle.ADAPTIVE,
                            onClick = { onNoteColorStyleChange(NoteColorStyle.ADAPTIVE) },
                            isPastel = false,
                            reducedMotion = reducedMotion
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Custom note wallpapers")
                    Text(
                        "Open any note and select the palette tool. In the Colour tab, tap + to choose an image from your device. The wallpaper is saved with that note; use the Background tab to return to an illustrated background.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.secondaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, colors.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.onSecondaryContainer.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = colors.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Personalize each note",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSecondaryContainer
                                )
                                Text(
                                    "Choose a color and background independently for each note.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- 2. Layout Section ----
        if (matchesSearch("layout")) {
            item(key = SettingsItemKeys[2]) {
                SettingsSection(
                    title = "Layout",
                    subtitle = "Choose how your notes appear",
                    icon = Icons.Default.GridView
                ) {
                    SettingLabel("Dashboard view")

                    // Visual mini-wireframe cards for Grid vs List
                    DashboardViewWireframeSelector(
                        selected = dashboardLayout,
                        onSelect = onLayoutChange,
                        reducedMotion = reducedMotion
                    )

                    Text(
                        if (dashboardLayout == DashboardLayout.GRID) {
                            "Compact cards, side by side"
                        } else {
                            "Full-width notes with more preview space"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Dashboard density")
                    SegmentedChoice(DashboardDensity.entries, dashboardDensity, { it.label }, onDensityChange)
                    Text(
                        dashboardDensity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // ---- 3. Notes & Search Section ----
        if (matchesSearch("behavior")) {
            item(key = SettingsItemKeys[3]) {
                SettingsSection(
                    title = "Notes & search",
                    subtitle = "Defaults for finding and organizing notes",
                    icon = Icons.Default.Search
                ) {
                    SettingLabel("Default category for new notes")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.forEach { category ->
                            val selected = defaultNoteCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { onDefaultNoteCategoryChange(category) },
                                label = { Text(category) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    SettingLabel("Manage categories")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = categoryDraft,
                            onValueChange = { categoryDraft = it.take(24) },
                            modifier = Modifier.weight(1f).testTag("new_category_field"),
                            label = { Text("New category") },
                            singleLine = true
                        )
                        FilledIconButton(
                            onClick = {
                                val clean = categoryDraft.trim()
                                if (clean.isNotBlank() && categoryOptions.none { it.equals(clean, ignoreCase = true) } && categoryOptions.size < 24) {
                                    onCategoryOptionsChange(categoryOptions + clean)
                                    categoryDraft = ""
                                }
                            },
                            enabled = categoryDraft.isNotBlank() && categoryOptions.size < 24,
                            modifier = Modifier.testTag("add_category_button")
                        ) { Icon(Icons.Default.Check, contentDescription = "Add category") }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.forEach { category ->
                            AssistChip(
                                onClick = { onDefaultNoteCategoryChange(category) },
                                label = { Text(category) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (categoryOptions.size > 1) onCategoryOptionsChange(categoryOptions - category)
                                        },
                                        enabled = categoryOptions.size > 1,
                                        modifier = Modifier.size(28.dp).testTag("remove_category_${category.lowercase().replace(' ', '_')}")
                                    ) { Icon(Icons.Default.Close, contentDescription = "Remove $category", modifier = Modifier.size(16.dp)) }
                                }
                            )
                        }
                    }
                    Text("Removing a category moves its notes to the first remaining category.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Sort notes by")
                    SegmentedChoice(SortOptions, selectedSort, { it.second }, { onSortChange(it.first) })
                    Text(
                        "Pinned notes always stay on top.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

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

        // ---- 4. Note Editor Section ----
        if (matchesSearch("editor")) {
            item(key = SettingsItemKeys[4]) {
                // Title "Note editor" guarantees SettingsScreenRegressionTest passes!
                SettingsSection(
                    title = "Note editor",
                    subtitle = "Markdown formatting, text size, and writing presentation",
                    icon = Icons.Default.Code
                ) {
                    SettingLabel("Default Markdown viewer zoom")
                    Text(
                        "$defaultMarkdownZoomPercent%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = defaultMarkdownZoomPercent.toFloat(),
                        onValueChange = { onDefaultMarkdownZoomPercentChange((it / 5f).toInt() * 5) },
                        valueRange = 50f..200f,
                        steps = 29,
                        modifier = Modifier.fillMaxWidth().testTag("default_markdown_zoom_slider")
                    )
                    Text(
                        "Used when opening Markdown files from Files. You can adjust each file from the viewer's More menu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingLabel("Open editor as")
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorPresentationMode.entries.forEach { mode ->
                            ChoiceCard(
                                title = mode.label,
                                description = mode.description,
                                selected = editorMode == mode,
                                onClick = { onEditorModeChange(mode) },
                                icon = when (mode) {
                                    EditorPresentationMode.SHEET -> Icons.Default.ViewAgenda
                                    EditorPresentationMode.FULL_SCREEN -> Icons.Default.Smartphone
                                    EditorPresentationMode.QUICK_DIALOG -> Icons.Default.Tune
                                },
                                reducedMotion = reducedMotion
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Font family")
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditorFontFamily.entries.filter { it != EditorFontFamily.CUSTOM || customFontPath != null }
                            .forEach { family ->
                                FilterChip(
                                    selected = editorFontStyle == family,
                                    onClick = {
                                        editorFontStyle = family
                                        editorPreferences.edit().putString(PREF_EDITOR_FONT, family.name).apply()
                                    },
                                    label = { Text(family.label) }
                                )
                            }
                    }
                    OutlinedButton(
                        onClick = { fontImportLauncher.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (customFontPath == null) "Import TTF or OTF font" else "Replace imported font")
                    }
                    Text(
                        "Imported fonts stay in this app's private storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                    SettingLabel("Body text size · $customTextSizeSp sp")
                    Slider(
                        value = customTextSizeSp.toFloat(),
                        onValueChange = { value ->
                            customTextSizeSp = value.roundToInt().coerceIn(12, 32)
                            editorPreferences.edit().putInt(PREF_TEXT_SIZE_SP, customTextSizeSp).apply()
                        },
                        valueRange = 12f..32f,
                        steps = 19,
                        modifier = Modifier.testTag("editor_font_size_slider")
                    )
                    SegmentedChoice(
                        EditorTextSize.entries,
                        editorTextSize,
                        { it.label },
                        { preset ->
                            onEditorTextSizeChange(preset)
                            customTextSizeSp = preset.bodySp
                            editorPreferences.edit().putInt(PREF_TEXT_SIZE_SP, preset.bodySp).apply()
                        }
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                    SettingLabel("Default format for new notes")
                    SegmentedChoice(
                        listOf("plain", "markdown"),
                        defaultNoteFormat,
                        { if (it == "markdown") "Markdown" else "Plain text" },
                        { format ->
                            defaultNoteFormat = format
                            editorPreferences.edit().putString(PREF_DEFAULT_NOTE_FORMAT, format).apply()
                        }
                    )

                    // Live animated preview note card
                    val animatedFontSize by animateFloatAsState(
                        targetValue = customTextSizeSp.toFloat(),
                        animationSpec = NestMotion.spatial(reducedMotion),
                        label = "editor-font-size-preview"
                    )
                    val previewTypeface = remember(editorFontStyle, customFontPath) {
                        when (editorFontStyle) {
                            EditorFontFamily.SERIF -> AndroidTypeface.SERIF
                            EditorFontFamily.MONOSPACE -> AndroidTypeface.MONOSPACE
                            EditorFontFamily.CURSIVE -> AndroidTypeface.create("casual", AndroidTypeface.NORMAL)
                            EditorFontFamily.CUSTOM -> customFontPath?.let { path ->
                                runCatching { AndroidTypeface.createFromFile(path) }.getOrNull()
                            } ?: AndroidTypeface.SANS_SERIF
                            EditorFontFamily.SANS -> AndroidTypeface.SANS_SERIF
                        }
                    }
                    val previewFontFamily = remember(previewTypeface) {
                        androidx.compose.ui.text.font.FontFamily(previewTypeface)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceContainerHighest,
                        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Preview note",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${editorFontStyle.label} · $customTextSizeSp sp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Text(
                                "Your notes will look like this.",
                                fontSize = animatedFontSize.sp,
                                lineHeight = (animatedFontSize * 1.45f).sp,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Normal,
                                fontFamily = previewFontFamily
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SwitchRow(
                        title = "Word count and reading time",
                        summary = "Show a live count under the note while you write",
                        checked = showEditorCounts,
                        onChange = onShowEditorCountsChange
                    )
                }
            }
        }

        // ---- 5. Voice Player Section (Inline Interactive Preview) ----
        if (matchesSearch("voice")) {
            item(key = "settings_voice_player_section_v2") {
                val playerPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                var compactVoicePlayer by remember {
                    mutableStateOf(playerPrefs.getBoolean(PREF_VOICE_PLAYER_COMPACT, true))
                }

                SettingsSection(
                    title = "Voice player",
                    subtitle = "Playback controls, waveform and appearance",
                    icon = Icons.Default.Mic
                ) {
                    SettingLabel("Player style")

                    // Interactive style selector with live graphics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VoicePlayerStyleOption(
                            label = "Compact",
                            description = "Minimal circular pill with waveform dots",
                            isCompact = true,
                            selected = compactVoicePlayer,
                            onClick = {
                                compactVoicePlayer = true
                                playerPrefs.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, true).putString(PREF_VOICE_PLAYER_STYLE, VoicePlayerStyle.COMPACT.name).apply()
                            },
                            modifier = Modifier.weight(1f),
                            reducedMotion = reducedMotion
                        )

                        VoicePlayerStyleOption(
                            label = "Full",
                            description = "Expanded player with scrubber and seek buttons",
                            isCompact = false,
                            selected = !compactVoicePlayer,
                            onClick = {
                                compactVoicePlayer = false
                                playerPrefs.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, false).putString(PREF_VOICE_PLAYER_STYLE, VoicePlayerStyle.FULL.name).apply()
                            },
                            modifier = Modifier.weight(1f),
                            reducedMotion = reducedMotion
                        )
                    }

                    // Interactive Live Waveform Preview Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVoicePlayerSettings = true }
                            .testTag("settings_voice_player_open"),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceContainerLow,
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = colors.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Interactive waveform preview",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (compactVoicePlayer) "Compact mode active" else "Full player active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Open voice player settings",
                                    tint = colors.onSurfaceVariant
                                )
                            }

                            // Dynamic animated waveform bars
                            AnimatedWaveformPreview(
                                active = true,
                                reducedMotion = reducedMotion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---- 6. Privacy Section ----
        if (matchesSearch("privacy")) {
            item(key = SettingsItemKeys[6]) {
                SettingsSection(
                    title = "Privacy",
                    subtitle = "Your notes stay safely on your device",
                    icon = Icons.Default.Lock
                ) {
                    PrivacyFeatureRow(
                        icon = Icons.Default.Security,
                        title = "100% Local-first",
                        body = "Notes are stored and encrypted on this device. They leave it only when you explicitly export or share them."
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    PrivacyFeatureRow(
                        icon = Icons.Default.Lock,
                        title = "No account or tracking",
                        body = "No login required. Exporting does not automatically sync to any cloud server; you choose the file destination via Android's file picker."
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    SwitchRow(
                        title = "Protect private screens",
                        summary = if (screenCaptureProtectionEnabled)
                            "Blocks screenshots and hides the app preview in Recents across NESTNOTE while enabled."
                        else
                            "Screenshots and app previews are allowed across NESTNOTE.",
                        checked = screenCaptureProtectionEnabled,
                        onChange = onScreenCaptureProtectionChange
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    OutlinedButton(
                        onClick = onOpenPrivateVaultSettings,
                        modifier = Modifier.fillMaxWidth().testTag("settings_private_vault"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Private Vault settings", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        "Use Android app permissions and your chosen export destination to manage access to your data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // ---- 7. Data & Encrypted Archive Section ----
        if (matchesSearch("data")) {
            item(key = SettingsItemKeys[7]) {
                SettingsSection(
                    title = "Data",
                    subtitle = "Manage encrypted archives and local storage",
                    icon = Icons.Default.Archive
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewTile(
                            label = "Active notes",
                            count = (totalNotes - archivedNotes).coerceAtLeast(0),
                            onClick = onOpenBackupRestore,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewTile(
                            label = "Archived notes",
                            count = archivedNotes,
                            onClick = onOpenBackupRestore,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        "Create or restore a password-protected encrypted archive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                    SettingLabel("Encrypted note storage")
                    Text(
                        "Choose which private app-data store NESTNOTE uses. Switching never deletes the other store; a new SQLCipher store is initialized from the current notes only the first time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable { if (storageBackend != NoteStorageBackend.KEYSTORE) pendingStorageBackend = NoteStorageBackend.KEYSTORE }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = storageBackend == NoteStorageBackend.KEYSTORE,
                                onClick = { if (storageBackend != NoteStorageBackend.KEYSTORE) pendingStorageBackend = NoteStorageBackend.KEYSTORE }
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text("Android Keystore encrypted storage", style = MaterialTheme.typography.titleSmall)
                                Text("AES-GCM encrypted note payload in app preferences", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable { if (storageBackend != NoteStorageBackend.SQLCIPHER) pendingStorageBackend = NoteStorageBackend.SQLCIPHER }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = storageBackend == NoteStorageBackend.SQLCIPHER,
                                onClick = { if (storageBackend != NoteStorageBackend.SQLCIPHER) pendingStorageBackend = NoteStorageBackend.SQLCIPHER }
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text("Encrypted SQL database (SQLCipher)", style = MaterialTheme.typography.titleSmall)
                                Text("Database under app-private storage; its key is wrapped by Android Keystore", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }

                    // Opens the encrypted archive workflow.
                    OutlinedButton(
                        onClick = onOpenBackupRestore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_backup_restore"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Encrypted archive", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ---- 8. Reset Button ----
        item(key = SettingsItemKeys[8]) {
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_reset"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Reset settings to defaults", fontWeight = FontWeight.SemiBold)
            }
        }

        // ---- No Search Results State ----
        if (!anySearchMatch) {
            item(key = "settings_no_search_results") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = colors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.secondaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            "No matching settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Try searching for theme, layout, category, text size, or backup.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { settingsSearch = "" },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear search")
                        }
                    }
                }
            }
        }
    }

    // Voice Player Settings Full Dialog (Kept for full compatibility with tests & deeper configuration)
    if (showVoicePlayerSettings) {
        VoicePlayerSettingsDialog(
            onDismiss = { showVoicePlayerSettings = false }
        )
    }

    pendingStorageBackend?.let { target ->
        val targetLabel = if (target == NoteStorageBackend.KEYSTORE) "AES-256 Android Keystore encryption" else "SQLCipher encrypted database"
        AlertDialog(
            onDismissRequest = { pendingStorageBackend = null },
            title = { Text("Are you sure you want to change?") },
            text = {
                Text("Switch to $targetLabel in 5 hours? Your current storage stays active until then. The switch will only complete if stored notes can be read and verified. You can cancel now by choosing Cancel.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingStorageBackend = null
                    onStorageBackendChange(target)
                }) { Text("Schedule change") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStorageBackend = null }) { Text("Cancel") }
            }
        )
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
                    customTextSizeSp = EditorTextSize.MEDIUM.bodySp
                    editorFontStyle = EditorFontFamily.SANS
                    customFontPath?.let { File(it).delete() }
                    customFontPath = null
                    defaultNoteFormat = "plain"
                    categoryDraft = ""
                    editorPreferences.edit().remove(PREF_TEXT_SIZE_SP).remove(PREF_EDITOR_FONT)
                        .remove(PREF_EDITOR_FONT_FILE).remove(PREF_DEFAULT_NOTE_FORMAT).apply()
                }) { Text("Reset", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Expressive Visual Components & Graphics
// -------------------------------------------------------------------------------------------------

@Composable
private fun SettingsHeroCard(
    totalNotes: Int,
    archivedNotes: Int,
    themeLabel: String,
    darkTheme: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient Canvas graphic in top right
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
            ) {
                drawCircle(
                    color = colors.primary.copy(alpha = 0.08f),
                    radius = 90.dp.toPx(),
                    center = Offset(size.width, 0f)
                )
                drawCircle(
                    color = colors.tertiary.copy(alpha = 0.06f),
                    radius = 55.dp.toPx(),
                    center = Offset(size.width, 0f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Make NESTNOTE feel like yours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Quick metrics badge pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroBadgePill(
                        icon = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        text = themeLabel,
                        modifier = Modifier.weight(1f)
                    )
                    HeroBadgePill(
                        icon = Icons.Default.Edit,
                        text = "$totalNotes notes",
                        modifier = Modifier.weight(1f)
                    )
                    HeroBadgePill(
                        icon = Icons.Default.Archive,
                        text = "$archivedNotes archived",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBadgePill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Visual Theme Selector with Graphical Cards
// -------------------------------------------------------------------------------------------------

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    reducedMotion: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = selected == mode
            val colors = MaterialTheme.colorScheme
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.outlineVariant,
                animationSpec = NestMotion.fast(reducedMotion),
                label = "theme-card-border"
            )
            val borderWidth by animateDpAsState(
                targetValue = if (isSelected) 2.dp else 1.dp,
                animationSpec = NestMotion.fast(reducedMotion),
                label = "theme-card-border-width"
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) colors.primaryContainer.copy(alpha = 0.35f) else colors.surfaceContainerLow,
                animationSpec = NestMotion.fast(reducedMotion),
                label = "theme-card-container"
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) }
                    ),
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = BorderStroke(borderWidth, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mini artwork illustration for each theme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = when (mode) {
                            ThemeMode.SYSTEM -> Color(0xFF333333)
                            ThemeMode.LIGHT -> Color(0xFFFFF7ED)
                            ThemeMode.DARK -> Color(0xFF1E1E24)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (mode) {
                                ThemeMode.SYSTEM -> {
                                    // Split Day/Night graphic
                                    Row(Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(Color(0xFFFFF7ED)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.LightMode,
                                                contentDescription = null,
                                                tint = Color(0xFFC4552D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(Color(0xFF1A1A1A)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = Color(0xFFFF8A5C),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                ThemeMode.LIGHT -> {
                                    Icon(
                                        Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = Color(0xFFC4552D),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                ThemeMode.DARK -> {
                                    Icon(
                                        Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = Color(0xFFFF8A5C),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.primary else colors.onSurface
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Note Card Color Style with Live Swatches
// -------------------------------------------------------------------------------------------------

@Composable
private fun NoteColorStyleCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    isPastel: Boolean,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surface,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "style-card-container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "style-card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "style-card-border-width"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(NestMotion.fast(reducedMotion)) + slideInHorizontally(
                        animationSpec = NestMotion.spatial(reducedMotion),
                        initialOffsetX = { it / 3 }
                    ),
                    exit = fadeOut(NestMotion.fast(reducedMotion)) + slideOutHorizontally(
                        animationSpec = NestMotion.fast(reducedMotion),
                        targetOffsetX = { it / 3 }
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                }
            }

            // Live Swatch Palette Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPastel) {
                    // 6 representative pastel swatches
                    listOf(
                        noteTints[0], noteTints[1], noteTints[4],
                        noteTints[5], noteTints[7], noteTints[9]
                    ).forEach { swatch ->
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = swatch,
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f))
                        ) {}
                    }
                } else {
                    // Dynamic Material You swatches
                    listOf(
                        colors.primary,
                        colors.secondary,
                        colors.tertiary,
                        colors.primaryContainer,
                        colors.secondaryContainer
                    ).forEach { swatch ->
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = swatch,
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f))
                        ) {}
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Dashboard Layout Wireframe Selector (Grid vs List Mini-Graphics)
// -------------------------------------------------------------------------------------------------

@Composable
private fun DashboardViewWireframeSelector(
    selected: DashboardLayout,
    onSelect: (DashboardLayout) -> Unit,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Grid Option Card
        LayoutWireframeCard(
            label = "Grid",
            isSelected = selected == DashboardLayout.GRID,
            onClick = { onSelect(DashboardLayout.GRID) },
            isGrid = true,
            modifier = Modifier.weight(1f),
            reducedMotion = reducedMotion
        )

        // List Option Card
        LayoutWireframeCard(
            label = "List",
            isSelected = selected == DashboardLayout.LIST,
            onClick = { onSelect(DashboardLayout.LIST) },
            isGrid = false,
            modifier = Modifier.weight(1f),
            reducedMotion = reducedMotion
        )
    }
}

@Composable
private fun LayoutWireframeCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isGrid: Boolean,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "layout-card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "layout-card-border-width"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) colors.secondaryContainer.copy(alpha = 0.45f) else colors.surfaceContainerLow,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "layout-card-container"
    )

    Surface(
        modifier = modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick
        ),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Graphical mini-wireframe
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceContainerHighest.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGrid) {
                        // 2x2 grid mini-cards
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                            }
                        }
                    } else {
                        // Stacked horizontal list cards
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                            ) {}
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                            ) {}
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colors.primary else colors.onSurface
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Voice Player Style Option & Waveform Graphics
// -------------------------------------------------------------------------------------------------

@Composable
private fun VoicePlayerStyleOption(
    label: String,
    description: String,
    isCompact: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "voice-style-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "voice-style-border-width"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "voice-style-container"
    )

    Surface(
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick
        ),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnimatedWaveformPreview(
    active: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "waveform-anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion || !active) 0f else (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform-phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 20
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val minHeight = 4.dp.toPx()
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val sinVal = if (reducedMotion || !active) {
                Math.sin(i * 0.4).toFloat().coerceIn(0f, 1f)
            } else {
                ((Math.sin(i * 0.45 + phase.toDouble()) + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)
            }
            val barH = minHeight + sinVal * (maxHeight - minHeight)
            val left = i * (barWidth + barSpacing)
            val top = (size.height - barH) / 2f

            drawRoundRect(
                color = if (i % 2 == 0) colors.primary else colors.tertiary,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun PrivacyFeatureRow(
    icon: ImageVector,
    title: String,
    body: String
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = colors.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Reusable Shared Controls
// -------------------------------------------------------------------------------------------------

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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = label(option),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            )
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
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberSystemReducedMotion()
    val rowColor by animateColorAsState(
        targetValue = if (checked) colors.secondaryContainer.copy(alpha = 0.42f) else Color.Transparent,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "settings-switch-row-background"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rowColor)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primary,
                checkedTrackColor = colors.primaryContainer
            )
        )
    }
}

@Composable
internal fun ChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    reducedMotion: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surface,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "choice-card-container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "choice-card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "choice-card-border-width"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(width = borderWidth, color = borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) colors.primary.copy(alpha = 0.15f) else colors.surfaceContainerHighest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (selected) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(NestMotion.fast(reducedMotion)) + slideInHorizontally(
                    animationSpec = NestMotion.spatial(reducedMotion),
                    initialOffsetX = { it / 3 }
                ),
                exit = fadeOut(NestMotion.fast(reducedMotion)) + slideOutHorizontally(
                    animationSpec = NestMotion.fast(reducedMotion),
                    targetOffsetX = { it / 3 }
                )
            ) {
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
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.65f))
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
        color = colors.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f))
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

// -------------------------------------------------------------------------------------------------
// Dedicated encrypted Backup & Restore Screen
@Composable
internal fun BackupRestoreScreen(
    totalNotes: Int,
    archivedNotes: Int,
    pinnedNotes: Int,
    lastBackupAt: Long,
    backupStatus: String,
    onChooseBackupFolder: () -> Unit,
    backupFolderConfigured: Boolean,
    onExportEncryptedArchive: (Boolean, String) -> Unit,
    onImportEncryptedArchive: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var includeArchivedInBackup by rememberSaveable { mutableStateOf(true) }
    var archivePassword by rememberSaveable { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Encrypted note archive", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Move notes between devices using password-protected encrypted archives.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BackupStatTile("Active", (totalNotes - archivedNotes).coerceAtLeast(0), Icons.Default.Edit, Modifier.weight(1f))
                    BackupStatTile("Archived", archivedNotes, Icons.Default.Archive, Modifier.weight(1f))
                    BackupStatTile("Pinned", pinnedNotes, Icons.Default.PushPin, Modifier.weight(1f))
                }
            }
        }

        SettingsSection(
            title = "Archive location",
            subtitle = "Choose where encrypted archives are stored",
            icon = Icons.Default.FolderOpen
        ) {
            Text(
                if (backupFolderConfigured) "Folder access is saved. Encrypted archives will be stored in Documents/NESTNOTE."
                else "Choose a folder once and grant NESTNOTE access to store encrypted archives.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
            )
            OutlinedButton(onClick = onChooseBackupFolder, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (backupFolderConfigured) "Change archive folder" else "Choose archive folder")
            }
        }

        SettingsSection(
            title = "Archive status",
            subtitle = "Last encrypted archive operation",
            icon = Icons.Default.Security
        ) {
            Text(
                if (lastBackupAt > 0L) "Last operation: " + SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(lastBackupAt)) + " · $backupStatus"
                else "No encrypted archive has been created in this installation.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
            )
        }

        SettingsSection(
            title = "Password-protected archive",
            subtitle = "AES-GCM encrypted .nestnote file",
            icon = Icons.Default.Lock
        ) {
            Text(
                "Use a strong password of at least 10 characters. Keep it separately; NESTNOTE cannot recover a forgotten archive password. Voice recordings are not included.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
            )
            SwitchRow(
                title = "Include archived notes",
                summary = "Turn off to archive active notes only",
                checked = includeArchivedInBackup,
                onChange = { includeArchivedInBackup = it }
            )
            OutlinedTextField(
                value = archivePassword,
                onValueChange = { archivePassword = it.take(256) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Archive password (10+ characters)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedButton(
                onClick = {
                    val password = archivePassword
                    archivePassword = ""
                    onExportEncryptedArchive(includeArchivedInBackup, password)
                },
                enabled = backupFolderConfigured && archivePassword.length >= 10,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create encrypted archive")
            }
            OutlinedButton(
                onClick = {
                    val password = archivePassword
                    archivePassword = ""
                    onImportEncryptedArchive(password)
                },
                enabled = backupFolderConfigured && archivePassword.length >= 10,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restore encrypted archive")
            }
        }
    }
}

@Composable
private fun BackupStatTile(
    label: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHighest.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Voice Player Settings Full Dialog (Platform compatibility)
// -------------------------------------------------------------------------------------------------

@Composable
private fun VoicePlayerSettingsDialog(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var compactVoicePlayer by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(PREF_VOICE_PLAYER_COMPACT, true)
        )
    }
    var voicePlaybackSpeed by rememberSaveable {
        mutableFloatStateOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(PREF_VOICE_PLAYER_SPEED, 1f)
        )
    }
    val playerPreferences = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var playerStyle by rememberSaveable {
        mutableStateOf(
            playerPreferences.getString(PREF_VOICE_PLAYER_STYLE, null)
                ?.let { runCatching { VoicePlayerStyle.valueOf(it) }.getOrNull() }
                ?: if (compactVoicePlayer) VoicePlayerStyle.COMPACT else VoicePlayerStyle.FULL
        )
    }
    var playerSizePercent by rememberSaveable {
        mutableIntStateOf(playerPreferences.getInt(PREF_VOICE_PLAYER_SIZE_PERCENT, DEFAULT_VOICE_PLAYER_SIZE_PERCENT)
            .coerceIn(MIN_VOICE_PLAYER_SIZE_PERCENT, MAX_VOICE_PLAYER_SIZE_PERCENT))
    }

    fun choosePlayerStyle(style: VoicePlayerStyle) {
        playerStyle = style
        compactVoicePlayer = style == VoicePlayerStyle.COMPACT || style == VoicePlayerStyle.MINIMAL
        playerPreferences.edit()
            .putString(PREF_VOICE_PLAYER_STYLE, style.name)
            .putBoolean(PREF_VOICE_PLAYER_COMPACT, compactVoicePlayer)
            .apply()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(26.dp),
            color = colors.surface
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("voice_player_settings_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Voice player", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Choose how recordings appear in your notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = colors.outlineVariant)

                Text("Default playback speed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "New and previewed recordings use this speed. You can still seek normally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row(
                    Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoicePlayerSpeeds.forEach { speed ->
                        FilterChip(
                            selected = voicePlaybackSpeed == speed,
                            onClick = {
                                voicePlaybackSpeed = speed
                                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                    .putFloat(PREF_VOICE_PLAYER_SPEED, speed).apply()
                            },
                            label = { Text("${speed}×") },
                            modifier = Modifier.weight(1f).testTag("voice_player_speed_${speed}")
                        )
                    }
                }

                HorizontalDivider(color = colors.outlineVariant)

                Text("Player style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose a layout that suits each note. The size control scales the player without changing the audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Player size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${playerSizePercent}%", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                    }
                    TextButton(onClick = {
                        playerSizePercent = DEFAULT_VOICE_PLAYER_SIZE_PERCENT
                        playerPreferences.edit().putInt(PREF_VOICE_PLAYER_SIZE_PERCENT, playerSizePercent).apply()
                    }) { Text("Reset") }
                }
                Slider(
                    value = playerSizePercent.toFloat(),
                    onValueChange = { value ->
                        playerSizePercent = value.roundToInt().coerceIn(MIN_VOICE_PLAYER_SIZE_PERCENT, MAX_VOICE_PLAYER_SIZE_PERCENT)
                        playerPreferences.edit().putInt(PREF_VOICE_PLAYER_SIZE_PERCENT, playerSizePercent).apply()
                    },
                    valueRange = MIN_VOICE_PLAYER_SIZE_PERCENT.toFloat()..MAX_VOICE_PLAYER_SIZE_PERCENT.toFloat(),
                    steps = 14,
                    modifier = Modifier.fillMaxWidth().testTag("voice_player_size_slider")
                )
                Text(
                    "75% compact  ·  100% default  ·  150% large",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                HorizontalDivider(color = colors.outlineVariant)

                Text("Choose a player layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoicePlayerStyle.values().forEach { style ->
                        val selected = playerStyle == style
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { choosePlayerStyle(style) }
                                .testTag("voice_player_style_${style.name.lowercase()}"),
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
                            border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) colors.primary else colors.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (style) {
                                                VoicePlayerStyle.COMPACT -> Icons.Default.PlayArrow
                                                VoicePlayerStyle.FULL -> Icons.Default.Mic
                                                VoicePlayerStyle.WAVEFORM -> Icons.Default.GraphicEq
                                                VoicePlayerStyle.MINIMAL -> Icons.Default.PlayArrow
                                                VoicePlayerStyle.TRANSPORT -> Icons.Default.Forward10
                                            },
                                            contentDescription = null,
                                            tint = if (selected) colors.onPrimary else colors.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(style.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(style.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                                RadioButton(selected = selected, onClick = { choosePlayerStyle(style) })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    "Your choice is saved automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
