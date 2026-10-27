package com.nestnote.app

import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.io.File

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
    onOpenCallRecordings: () -> Unit = {},
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

    val anySearchMatch = remember(settingsSearch) {
        settingsSearch.isBlank() || SettingsSearchSections.any { matchesSettingsSearch(settingsSearch, it) }
    }

    // Filtering hides whole LazyColumn items; keeping the previous scroll offset made the
    // screen appear blank when a query was typed after scrolling down. Reset to the top
    // whenever a search is active so the first match is always visible.
    LaunchedEffect(settingsSearch) {
        if (settingsSearch.isNotBlank()) listState.scrollToItem(0)
    }

    val matchedSectionCount = remember(settingsSearch) {
        if (settingsSearch.isBlank()) 0
        else SettingsSearchSections.count { matchesSettingsSearch(settingsSearch, it) }
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

                if (settingsSearch.isNotBlank()) {
                    Text(
                        when {
                            !anySearchMatch -> "No sections match \u201C$settingsSearch\u201D \u2014 try a shorter keyword"
                            matchedSectionCount == 1 -> "1 section matches \u201C$settingsSearch\u201D"
                            else -> "$matchedSectionCount sections match \u201C$settingsSearch\u201D"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

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
        if (matchesSettingsSearch(settingsSearch, "appearance")) {
            item(key = SettingsItemKeys[1]) {
                SettingsAppearanceSection(
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    reducedMotion = reducedMotion,
                    dynamicColors = dynamicColors,
                    onDynamicColorsChange = onDynamicColorsChange,
                    amoled = amoled,
                    onAmoledChange = onAmoledChange,
                    accentPalette = accentPalette,
                    onAccentPaletteChange = onAccentPaletteChange,
                    noteColorStyle = noteColorStyle,
                    onNoteColorStyleChange = onNoteColorStyleChange
                )
            }
        }

        // ---- 2. Layout Section ----
        if (matchesSettingsSearch(settingsSearch, "layout")) {
            item(key = SettingsItemKeys[2]) {
                SettingsLayoutSection(
                    dashboardLayout = dashboardLayout,
                    onLayoutChange = onLayoutChange,
                    reducedMotion = reducedMotion,
                    dashboardDensity = dashboardDensity,
                    onDensityChange = onDensityChange
                )
            }
        }

        // ---- 3. Notes & Search Section ----
        if (matchesSettingsSearch(settingsSearch, "behavior")) {
            item(key = SettingsItemKeys[3]) {
                SettingsBehaviorSection(
                    categoryOptions = categoryOptions,
                    defaultNoteCategory = defaultNoteCategory,
                    onDefaultNoteCategoryChange = onDefaultNoteCategoryChange,
                    categoryDraft = categoryDraft,
                    onCategoryDraftChange = { categoryDraft = it },
                    onCategoryOptionsChange = onCategoryOptionsChange,
                    selectedSort = selectedSort.first,
                    onSortChange = onSortChange,
                    showNotePreviews = showNotePreviews,
                    onShowNotePreviewsChange = onShowNotePreviewsChange,
                    showNoteTimestamps = showNoteTimestamps,
                    onShowNoteTimestampsChange = onShowNoteTimestampsChange
                )
            }
        }

        // ---- 4. Note Editor Section ----
        if (matchesSettingsSearch(settingsSearch, "editor")) {
            item(key = SettingsItemKeys[4]) {
                SettingsEditorSection(
                    defaultMarkdownZoomPercent = defaultMarkdownZoomPercent,
                    onDefaultMarkdownZoomPercentChange = onDefaultMarkdownZoomPercentChange,
                    editorMode = editorMode,
                    onEditorModeChange = onEditorModeChange,
                    reducedMotion = reducedMotion,
                    editorFontStyle = editorFontStyle,
                    customFontPath = customFontPath,
                    onFontFamilySelected = { family ->
                        editorFontStyle = family
                        editorPreferences.edit().putString(PREF_EDITOR_FONT, family.name).apply()
                    },
                    onImportFont = {
                        fontImportLauncher.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream"))
                    },
                    customTextSizeSp = customTextSizeSp,
                    onCustomTextSizeChange = { size ->
                        customTextSizeSp = size
                        editorPreferences.edit().putInt(PREF_TEXT_SIZE_SP, size).apply()
                    },
                    editorTextSize = editorTextSize,
                    onEditorTextSizePreset = { preset ->
                        onEditorTextSizeChange(preset)
                        customTextSizeSp = preset.bodySp
                        editorPreferences.edit().putInt(PREF_TEXT_SIZE_SP, preset.bodySp).apply()
                    },
                    defaultNoteFormat = defaultNoteFormat,
                    onDefaultNoteFormatChange = { format ->
                        defaultNoteFormat = format
                        editorPreferences.edit().putString(PREF_DEFAULT_NOTE_FORMAT, format).apply()
                    },
                    showEditorCounts = showEditorCounts,
                    onShowEditorCountsChange = onShowEditorCountsChange
                )
            }
        }

        // ---- 5. Voice Player Section (Inline Interactive Preview) ----
        if (matchesSettingsSearch(settingsSearch, "voice")) {
            item(key = "settings_voice_player_section_v2") {
                SettingsVoicePlayerSection(
                    reducedMotion = reducedMotion,
                    onOpenVoicePlayerSettings = { showVoicePlayerSettings = true }
                )
            }
        }

        // ---- 6. Privacy Section ----
        if (matchesSettingsSearch(settingsSearch, "privacy")) {
            item(key = SettingsItemKeys[6]) {
                SettingsPrivacySection(
                    screenCaptureProtectionEnabled = screenCaptureProtectionEnabled,
                    onScreenCaptureProtectionChange = onScreenCaptureProtectionChange,
                    onOpenPrivateVaultSettings = onOpenPrivateVaultSettings,
                    onOpenCallRecordings = onOpenCallRecordings
                )
            }
        }

        // ---- 7. Data & Encrypted Archive Section ----
        if (matchesSettingsSearch(settingsSearch, "data")) {
            item(key = SettingsItemKeys[7]) {
                SettingsDataSection(
                    totalNotes = totalNotes,
                    archivedNotes = archivedNotes,
                    onOpenBackupRestore = onOpenBackupRestore,
                    storageBackend = storageBackend,
                    onRequestStorageBackendChange = { pendingStorageBackend = it }
                )
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

        // ---- 9. Version footer ----
        item(key = "settings_version_footer") {
            val version = remember(context) { readAppVersion(context) }
            Text(
                "NESTNOTE ${version.name} (${version.code}) \u00B7 local-first encrypted notes",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
