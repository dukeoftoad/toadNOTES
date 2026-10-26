package com.nestnote.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

private const val PREF_PRIVATE_SCREEN_PROTECTION = "private_screen_protection_enabled"
internal const val PREF_BOOT_SOUND_ENABLED = "boot_sound_enabled"

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
    var filteredNotes by remember { mutableStateOf<List<NestNote>>(emptyList()) }
    // Private records are held in the general encrypted blob only until the first authenticated
    // vault unlock can migrate them. Never expose them to ordinary views or overwrite them while
    // migration is pending.
    var legacyPrivateNotes by remember { mutableStateOf<List<NestNote>>(emptyList()) }
    val privateNoteCrypto = remember(context) { PrivateNoteCrypto(context) }
    val privateNoteStore = remember(context) { PrivateNoteStore(context) }
    val ordinaryNoteCrypto = remember(context) { OrdinaryNoteCrypto(context) }
    var privateVaultSession by remember { mutableStateOf<PrivateNoteCrypto.Session?>(null) }
    var storageLoaded by remember { mutableStateOf(false) }
    var lastBackupAt by remember { mutableLongStateOf(preferences.getLong(PREF_BACKUP_LAST_SUCCESS_AT, 0L)) }
    var backupStatus by remember { mutableStateOf(preferences.getString(PREF_BACKUP_LAST_STATUS, "Not yet backed up") ?: "Not yet backed up") }
    var storageRecoveryRequired by remember { mutableStateOf(false) }
    var showStorageRecoveryDialog by remember { mutableStateOf(false) }
    var openedExternalMarkdownTitle by remember { mutableStateOf("") }
    var openedExternalMarkdownContent by remember { mutableStateOf("") }
    var defaultMarkdownZoomPercent by remember { mutableIntStateOf(preferences.getLong(PREF_MARKDOWN_ZOOM, 100L).toInt().coerceIn(50, 200)) }
    var externalMarkdownZoomPercent by remember { mutableIntStateOf(defaultMarkdownZoomPercent) }
    var showExternalMarkdownZoomControls by remember { mutableStateOf(false) }
    var externalMarkdownMoreMenuExpanded by remember { mutableStateOf(false) }
    var showExternalMarkdownDialog by remember { mutableStateOf(false) }
    LaunchedEffect(store) {
        val result = withContext(Dispatchers.IO) { store.loadSafely() }
        // Legacy private notes stay encrypted by the old store key and are not rendered in any
        // ordinary list. They are migrated only after successful vault-password verification.
        notes = result.notes.filterNot { it.privateNote }
        legacyPrivateNotes = result.notes.filter { it.privateNote }
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
    var accentPalette by remember { mutableStateOf(AccentPalette.values().firstOrNull { it.name == preferences.getString("PREF_ACCENT_PALETTE", "DEFAULT") } ?: AccentPalette.DEFAULT) }
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
    val tabContentAlpha = remember { Animatable(1f) }
    val tabContentOffsetY = remember { Animatable(0f) }
    var hasShownInitialTab by remember { mutableStateOf(false) }
    LaunchedEffect(selectedView) {
        if (hasShownInitialTab) {
            tabContentAlpha.snapTo(0.88f)
            tabContentOffsetY.snapTo(7f)
            launch {
                tabContentAlpha.animateTo(1f, animationSpec = tween(durationMillis = 190))
            }
            tabContentOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 190))
        } else {
            hasShownInitialTab = true
        }
    }
    var showPrivateVaultGate by remember { mutableStateOf(false) }
    var privateVaultUnlocked by remember { mutableStateOf(false) }
    fun lockPrivateVault() {
        privateVaultSession = null
        privateVaultUnlocked = false
        notes = notes.filterNot { it.privateNote }
        if (selectedView == "Private") selectedView = "Home"
    }
    var privateVaultBiometricEnabled by remember { mutableStateOf(preferences.getBoolean("private_vault_biometric_enabled", false)) }
    var screenCaptureProtectionEnabled by remember {
        mutableStateOf(preferences.getBoolean(PREF_PRIVATE_SCREEN_PROTECTION, true))
    }
    var pendingBiometricEnrollment by remember { mutableStateOf(false) }
    var showBiometricEnrollment by remember { mutableStateOf(false) }
    val privateVaultBiometricAvailable = remember(context) {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
    fun requestPrivateVault() { showPrivateVaultGate = true }
    LaunchedEffect(selectedView) {
        if (selectedView != "Private") lockPrivateVault()
    }
    var search by remember { mutableStateOf("") }
    var selectedNoteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var confirmEncryptAll by remember { mutableStateOf(false) }
    var savedSearches by remember {
        mutableStateOf<List<String>>(preferences.getStringSet(PREF_SAVED_SEARCHES, emptySet())?.toList()?.sorted() ?: emptyList())
    }
    var searchVisible by remember { mutableStateOf(false) }
    var editorNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorDraftId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingLinkedNoteAfterSave by remember { mutableStateOf<Long?>(null) }
    var editorTemplate by remember { mutableStateOf<NoteTemplate?>(null) }
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editorHasUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    var discardVoiceChanges by remember { mutableStateOf<() -> Unit>({}) }
    var commitVoiceChanges by remember { mutableStateOf<() -> Unit>({}) }
    var confirmSystemDiscard by remember { mutableStateOf(false) }
    val editorNote = notes.firstOrNull { it.id == editorNoteId }
    // Protect private content even when a private note is opened from Home.
    // Track ownership across recompositions so changing between protected routes
    // cannot accidentally leave FLAG_SECURE set or clear a flag owned elsewhere.
    // Apply FLAG_SECURE across the app whenever the privacy setting is enabled.
    // This protects private content consistently, without relying on the current route.
    val protectPrivateContent = screenCaptureProtectionEnabled
    var privateVaultAddedSecureFlag by remember { mutableStateOf(false) }
    val secureWindow = remember(context) { context.findActivityForSecureWindow()?.window }
    SideEffect {
        val currentlySecure = (secureWindow?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE != 0
        if (protectPrivateContent && !currentlySecure) {
            secureWindow?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            privateVaultAddedSecureFlag = true
        } else if (!protectPrivateContent && privateVaultAddedSecureFlag) {
            secureWindow?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            privateVaultAddedSecureFlag = false
        }
    }
    DisposableEffect(secureWindow) {
        onDispose {
            if (privateVaultAddedSecureFlag) {
                secureWindow?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                privateVaultAddedSecureFlag = false
            }
        }
    }
    // Relock and tear down editor UI when backgrounding while either the vault or
    // a private note editor is visible. Editor state is saveable independently of
    // the vault route, so changing selectedView alone is not a sufficient boundary.
    DisposableEffect(context, selectedView, editorVisible, editorNote?.privateNote) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP &&
                (selectedView == "Private" || (editorVisible && editorNote?.privateNote == true))
            ) {
                // Drop the cryptographic session and remove decrypted private records
                // immediately; do not depend on a later route effect to perform the lock.
                lockPrivateVault()
                // Hide the editor without clearing its saveable IDs or dirty state.
                // NoteEditor autosaves drafts, and retaining state avoids discarding
                // a recent edit while still preventing the private UI from reappearing.
                editorVisible = false
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun saveString(key: String, value: String) = preferences.edit().putString(key, value).apply()
    fun saveBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
    fun saveLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()
    var persistenceBusy by remember { mutableStateOf(false) }
    suspend fun persist(next: List<NestNote>, allowRecovery: Boolean = false, claimHeld: Boolean = false): Boolean {
        if (!claimHeld) {
            if (persistenceBusy) {
                snackbarHostState.showSnackbar("A save is already in progress. Please try again.")
                return false
            }
            persistenceBusy = true
        } else if (!persistenceBusy) {
            return false
        }
        try {
            if (!storageLoaded) {
                snackbarHostState.showSnackbar("Notes are still loading. Try again in a moment.")
                return false
            }
            if (storageRecoveryRequired && !allowRecovery) {
                snackbarHostState.showSnackbar("Restore a backup before changing notes. Existing local data was not overwritten.")
                return false
            }
            val session = privateVaultSession
            val publicNext = next.filterNot { it.privateNote }
            val privateNext = next.filter { it.privateNote }
            if (privateNext.isNotEmpty() && session == null) {
                snackbarHostState.showSnackbar("Unlock Private Vault before saving private notes.")
                return false
            }
            val generalNext = if (session == null) publicNext + legacyPrivateNotes else publicNext
            val oldPrivate = notes.filter { it.privateNote }
            val (privateSaved, saved, rollbackSucceeded) = withContext(Dispatchers.IO) {
                android.os.Trace.beginSection("NestNote.persist.privateStoreSave")
                val privateOk = try {
                    if (session == null) true else privateNoteStore.save(session, privateNext)
                } finally {
                    android.os.Trace.endSection()
                }
                if (!privateOk) return@withContext Triple(false, false, true)
                android.os.Trace.beginSection("NestNote.persist.publicStoreSave")
                val publicOk = try {
                    store.save(generalNext)
                } finally {
                    android.os.Trace.endSection()
                }
                val rollbackOk = if (!publicOk && session != null) privateNoteStore.save(session, oldPrivate) else true
                Triple(privateOk, publicOk, rollbackOk)
            }
            if (!privateSaved) {
                snackbarHostState.showSnackbar("Private note encryption failed. No plaintext fallback was used.")
                return false
            }
            if (saved) {
                val previousIds = notes.map { it.id }.toSet()
                val nextIds = next.map { it.id }.toSet()
                val removedIds = previousIds - nextIds
                val notesForAlarms = next
                coroutineScope.launch(Dispatchers.IO) {
                    removedIds.forEach { ReminderScheduler.cancel(context, it) }
                    ReminderScheduler.rescheduleAll(context, notesForAlarms)
                }
                notes = if (session == null) publicNext else next
                if (allowRecovery) storageRecoveryRequired = false
            } else if (!rollbackSucceeded) {
                storageRecoveryRequired = true
                snackbarHostState.showSnackbar("Storage recovery is required after a failed save. Do not make further changes until recovery is complete.")
            } else {
                snackbarHostState.showSnackbar("Save failed. Your previous notes are still available.")
            }
            return saved
        } finally {
            persistenceBusy = false
        }
    }
    fun requestPersist(
        next: List<NestNote>,
        allowRecovery: Boolean = false,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        if (persistenceBusy) {
            onFailure()
            coroutineScope.launch { snackbarHostState.showSnackbar("A save is already in progress. Please try again.") }
            return
        }
        // Claim on the UI thread before launching so two rapid taps cannot queue stale snapshots.
        persistenceBusy = true
        coroutineScope.launch {
            try {
                if (persist(next, allowRecovery, claimHeld = true)) onSuccess() else onFailure()
            } finally {
                // Also release the claim if this coroutine is cancelled before persist starts.
                if (persistenceBusy) persistenceBusy = false
            }
        }
    }
    fun encryptOrdinaryNote(note: NestNote): NestNote {
        require(!note.privateNote && !note.encrypted) { "Only unencrypted ordinary notes can be encrypted here." }
        val payload = ordinaryNoteCrypto.encrypt(note)
        val locked = OrdinaryNoteRecordTransform.lockedRecord(note, payload)
        // Verify authenticated decryption and identity before the source record can be replaced.
        val verified = ordinaryNoteCrypto.decrypt(locked)
        check(verified == note) { "Encrypted note verification failed." }
        return locked
    }
    fun decryptOrdinaryNote(note: NestNote): NestNote {
        require(note.encrypted && !note.privateNote) { "Only encrypted ordinary notes can be decrypted here." }
        return ordinaryNoteCrypto.decrypt(note)
    }
    fun toggleNoteEncryption(noteId: Long) {
        val note = notes.firstOrNull { it.id == noteId } ?: return
        if (note.privateNote) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Private Vault notes use their own independent encryption.") }
            return
        }
        val replacement = runCatching {
            if (note.encrypted) decryptOrdinaryNote(note) else encryptOrdinaryNote(note)
        }.getOrElse { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(error.message ?: "Encryption failed; the original note was preserved.")
            }
            return
        }
        requestPersist(notes.map { if (it.id == noteId) replacement else it }) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(if (note.encrypted) "Note decrypted" else "Note encrypted and verified")
            }
        }
    }
    fun encryptSelectedNotes() {
        val selected = OrdinaryNoteSelection.forEncryption(notes, selectedNoteIds)
        if (selected.isEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("No unencrypted ordinary notes are selected.") }
            return
        }
        val replacements = runCatching { selected.associate { it.id to encryptOrdinaryNote(it) } }.getOrElse { error ->
            coroutineScope.launch { snackbarHostState.showSnackbar(error.message ?: "Bulk encryption failed; no notes were changed.") }
            return
        }
        requestPersist(OrdinaryNoteSelection.applyReplacements(notes, replacements)) {
            selectedNoteIds = emptySet()
            coroutineScope.launch { snackbarHostState.showSnackbar("Encrypted and verified ${replacements.size} notes") }
        }
    }
    fun decryptSelectedNotes() {
        val selected = OrdinaryNoteSelection.forDecryption(notes, selectedNoteIds)
        if (selected.isEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("No encrypted ordinary notes are selected.") }
            return
        }
        val replacements = runCatching { selected.associate { it.id to decryptOrdinaryNote(it) } }.getOrElse { error ->
            coroutineScope.launch { snackbarHostState.showSnackbar(error.message ?: "Bulk decryption failed; no notes were changed.") }
            return
        }
        requestPersist(OrdinaryNoteSelection.applyReplacements(notes, replacements)) {
            selectedNoteIds = emptySet()
            coroutineScope.launch { snackbarHostState.showSnackbar("Decrypted and verified ${replacements.size} notes") }
        }
    }
    fun deleteNote(note: NestNote, closeEditor: Boolean = false) {
        requestPersist(notes.filterNot { it.id == note.id }) {
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
                    withContext(Dispatchers.IO) {
                        runCatching { com.nestnote.app.voicenote.VoiceNoteRepository(context).deleteAllForNote(note.id) }
                    }
                }
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
        accentPalette = AccentPalette.DEFAULT
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
        defaultMarkdownZoomPercent = 100
        showEditorCounts = true
        categoryOptions = categories
        defaultNoteCategory = "Ideas"
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
                    MarkdownNoteParser.parse(markdown, fileName)
                }.getOrNull()
            }
        }
        if (parsed.any { it == null }) {
            snackbarHostState.showSnackbar("Couldn't read one or more Markdown files. Nothing was imported.")
            return
        }
        val imported = MarkdownNoteFactory.create(
            parsedNotes = parsed.filterNotNull(),
            existingNotes = notes,
            category = defaultNoteCategory,
            now = System.currentTimeMillis()
        )
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
            val openedDocument = withContext(Dispatchers.IO) {
                runCatching {
                    val markdown = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Unable to read Markdown file")
                    val displayName = context.contentResolver.query(
                        uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
                    }.orEmpty()
                    val title = displayName.ifBlank {
                        uri.lastPathSegment?.substringAfterLast('/') ?: "Markdown document"
                    }
                    title to markdown
                }
            }
            openedDocument.onSuccess { (title, markdown) ->
                openedExternalMarkdownTitle = title
                openedExternalMarkdownContent = markdown
                externalMarkdownZoomPercent = defaultMarkdownZoomPercent
                showExternalMarkdownZoomControls = false
                externalMarkdownMoreMenuExpanded = false
                showExternalMarkdownDialog = true
            }.onFailure {
                snackbarHostState.showSnackbar("Couldn't open that Markdown file")
            }
            onExternalMarkdownHandled()
        }
    }
    var backupFolderConfigured by remember { mutableStateOf(BackupDirectory.configuredTreeUri(context) != null) }
    val backupFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null && BackupDirectory.persistTreeGrant(context, treeUri)) {
            backupFolderConfigured = true
            coroutineScope.launch { snackbarHostState.showSnackbar("Encrypted archive folder access saved.") }
        } else if (treeUri != null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Folder access could not be saved. Choose a folder again.") }
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

    // Keep query parsing, full-body matching, and sorting off the UI thread. A short debounce
    // prevents rebuilding the result list for every intermediate keystroke while preserving
    // immediate text-field feedback.
    LaunchedEffect(notes, selectedView, search, sortByTitle, sortByCreated, privateVaultUnlocked) {
        delay(100)
        val view = if (selectedView == "Private" && privateVaultUnlocked) "Private" else selectedView
        val result = withContext(Dispatchers.Default) {
            NoteFilters.visibleNotes(notes, view, search, sortByTitle, sortByCreated)
        }
        filteredNotes = result
    }

    PredictiveBackHandler(enabled = !editorVisible && selectedView != "Home") { progress ->
        progress.collect { /* Reserve the progress stream for future destination previews. */ }
        selectedView = if (selectedView in moreSubViews) "More" else "Home"
        search = ""
        searchVisible = false
    }

    val expressiveScheme = (if (accentPalette == AccentPalette.DEFAULT) scheme else accentPalette.applyTo(scheme, dark))
        .let { if (dark && amoled) it.asAmoled() else it }

    MaterialTheme(
        colorScheme = expressiveScheme,
        typography = nestNoteTypography,
        shapes = nestNoteShapes
    ) {
        val colors = MaterialTheme.colorScheme
        if (!storageLoaded) {
            BootLandingScreen(preferences = preferences)
        } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier
                        ) {
                            AppDestinationHeader(selectedView)
                        }
                    },
                    navigationIcon = {
                        AppDestinationNavigationIcon(
                            destination = selectedView,
                            isMoreSubDestination = selectedView in moreSubViews,
                            onLockPrivateVault = { lockPrivateVault() },
                            onBackToMore = { selectedView = "More" }
                        )
                    },
                    actions = {
                        if (selectedView == "Home" || selectedView == "All notes") {
                            AppDestinationSearchAction(
                                searchVisible = searchVisible,
                                onToggle = {
                                    searchVisible = !searchVisible
                                    if (!searchVisible) search = ""
                                }
                            )
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
                            requestPersist(notes.map { if (it.id in selectedNoteIds) it.copy(category = category) else it }) { selectedNoteIds = emptySet() }
                        },
                        onTogglePin = {
                            val pin = !(selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned })
                            requestPersist(notes.map { if (it.id in selectedNoteIds) it.copy(pinned = pin, updated = System.currentTimeMillis()) else it }) { selectedNoteIds = emptySet() }
                        },
                        onToggleArchive = {
                            val restore = selectedNotes.isNotEmpty() && selectedNotes.all { it.archived }
                            requestPersist(notes.map { if (it.id in selectedNoteIds) it.copy(archived = !restore, pinned = if (restore) it.pinned else false, updated = System.currentTimeMillis()) else it }) { selectedNoteIds = emptySet() }
                        },
                        onDelete = { confirmBulkDelete = true },
                        onClear = { selectedNoteIds = emptySet() },
                        onSelectAll = { selectedNoteIds = filteredNotes.filterNot { it.privateNote }.map { it.id }.toSet() },
                        onEncrypt = if (selectedNotes.any { !it.encrypted && !it.privateNote }) ({ encryptSelectedNotes() }) else null,
                        onDecrypt = if (selectedNotes.any { it.encrypted && !it.privateNote }) ({ decryptSelectedNotes() }) else null
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
                        .graphicsLayer {
                            alpha = tabContentAlpha.value
                            translationY = tabContentOffsetY.value
                        }
                        .animateContentSize(animationSpec = tween(durationMillis = 200))
                ) {
                if (storageRecoveryRequired) {
                    AppStorageRecoveryNotice(onOpenSettings = { selectedView = "Settings" })
                }
                AppDashboardFilters(
                    searchVisible = searchVisible,
                    search = search,
                    savedSearches = savedSearches,
                    selectedView = selectedView,
                    categories = categoryOptions,
                    onSearchChange = { search = it },
                    onSaveSearch = { query ->
                        if (query !in savedSearches && savedSearches.size < 12) {
                            savedSearches = savedSearches + query
                            preferences.edit().putStringSet(PREF_SAVED_SEARCHES, savedSearches.toMutableSet()).apply()
                        }
                    },
                    onCloseSearch = { searchVisible = false; search = "" },
                    onRemoveSavedSearch = { query ->
                        savedSearches = savedSearches - query
                        preferences.edit().putStringSet(PREF_SAVED_SEARCHES, savedSearches.toMutableSet()).apply()
                    },
                    onSelectView = { selectedView = it }
                )
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
                } else if (selectedView == "TextToolbox") {
                    TextToolboxScreen()
                } else if (selectedView == "Shortcuts") {
                    KeyboardShortcutsScreen()
                } else if (selectedView == "Backup") {
                    BackupRestoreScreen(
                        totalNotes = notes.count { !it.privateNote },
                        archivedNotes = notes.count { it.archived && !it.privateNote },
                        pinnedNotes = notes.count { it.pinned && !it.archived && !it.privateNote },
                        lastBackupAt = lastBackupAt,
                        backupStatus = backupStatus,
                        backupFolderConfigured = backupFolderConfigured,
                        onChooseBackupFolder = { backupFolderLauncher.launch(null) },
                        onExportEncryptedArchive = { includeArchived, password ->
                            val encryptedExcluded = notes.count { it.encrypted && !it.privateNote }
                            val archiveNotes = NoteStore.notesForArchive(notes, includeArchived = includeArchived)
                            val encrypted = runCatching {
                                PasswordBackupArchive.encryptArchive(
                                    NoteStore.encodeArchivePayload(archiveNotes),
                                    password.toCharArray()
                                )
                            }.getOrNull()
                            val savedTree = BackupDirectory.configuredTreeUri(context)
                            val saved = encrypted != null && savedTree != null &&
                                BackupDirectory.writeEncryptedLatest(context, savedTree, encrypted)
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
                                snackbarHostState.showSnackbar(
                                    when {
                                        encrypted == null -> "Encrypted archive could not be created. Check the password and archive size."
                                        saved -> if (encryptedExcluded > 0) "Password-protected archive saved. $encryptedExcluded device-encrypted notes were excluded; decrypt them before backing them up." else "Password-protected archive saved to Documents/NESTNOTE/NESTNOTE-backup.nestnote."
                                        else -> "Encrypted archive could not be saved. Your notes were not changed."
                                    }
                                )
                            }
                        },
                        onImportEncryptedArchive = { password ->
                            val savedTree = BackupDirectory.configuredTreeUri(context)
                            val archive = savedTree?.let { BackupDirectory.readEncryptedLatest(context, it) }
                            val raw = archive?.let { PasswordBackupArchive.decryptArchive(it, password.toCharArray()) }
                            val incoming = raw?.let(NoteStore::decodeArchivePayload)
                            if (incoming != null && incoming.isNotEmpty()) {
                                pendingRestore = incoming
                                coroutineScope.launch { snackbarHostState.showSnackbar("Encrypted archive unlocked. Review the restore before applying it.") }
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Could not unlock the encrypted archive. Check the password or archive file; no notes were changed.") }
                            }
                        }
                    )
                } else if (selectedView == "CallRecordings") {
                    CallRecordingsScreen(onBack = { selectedView = "Settings" })
                } else if (selectedView == "PrivateVaultSettings") {
                    PrivateVaultSettingsScreen(
                        passwordConfigured = PrivateVaultSecurity.isConfigured(context),
                        biometricAvailable = privateVaultBiometricAvailable,
                        biometricEnabled = privateVaultBiometricEnabled && PrivateVaultBiometricKey(context).isEnrolled(),
                        privateNoteCount = notes.count { it.privateNote },
                        onBiometricEnabledChange = { enabled ->
                            if (enabled) {
                                pendingBiometricEnrollment = true
                                requestPrivateVault()
                            } else {
                                PrivateVaultBiometricKey(context).deleteEnrollment()
                                privateVaultBiometricEnabled = false
                                preferences.edit().putBoolean("private_vault_biometric_enabled", false).commit()
                            }
                        },
                        onSetUpPassword = { requestPrivateVault() },
                        onOpenVault = { requestPrivateVault() },
                        onChangePassword = { current, new, confirmation ->
                            val validRequest = new.length >= PrivateVaultSecurity.MIN_PASSWORD_LENGTH &&
                                new == confirmation && PrivateVaultSecurity.verify(context, current) &&
                                !PrivateVaultSecurity.isLockedOut(context)
                            val cryptoInitialized = privateNoteCrypto.isInitialized()
                            val accepted = if (!validRequest) {
                                false
                            } else if (!cryptoInitialized) {
                                PrivateVaultSecurity.changePassword(context, current, new, confirmation)
                            } else {
                                // These preference namespaces cannot share an atomic commit.
                                // Keep the compensating rollback explicit and testable.
                                val result = PrivateVaultPasswordChange.execute(
                                    current = current,
                                    replacement = new,
                                    rewrapKey = { from, to ->
                                        privateNoteCrypto.changePassword(from.toCharArray(), to.toCharArray())
                                    },
                                    updateVerifier = {
                                        PrivateVaultSecurity.changePassword(context, current, new, confirmation)
                                    },
                                    markRecoveryRequired = {
                                        storageRecoveryRequired = true
                                        lockPrivateVault()
                                    }
                                )
                                if (result.recoveryRequired) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Password change recovery is required. Private Vault was locked to protect your notes.")
                                    }
                                }
                                result.accepted
                            }
                            if (accepted) coroutineScope.launch { snackbarHostState.showSnackbar("Private Vault password updated") }
                            accepted
                        }
                    )
                } else if (selectedView == "Settings") {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it; saveString(PREF_THEME, it.name) },
                        dynamicColors = dynamicColors,
                        onDynamicColorsChange = { enabled ->
                            dynamicColors = enabled
                            saveBoolean(PREF_DYNAMIC, enabled)
                            if (enabled) {
                                accentPalette = AccentPalette.DEFAULT
                                saveString("PREF_ACCENT_PALETTE", AccentPalette.DEFAULT.name)
                            }
                        },
                        amoled = amoled,
                        onAmoledChange = { amoled = it; saveBoolean(PREF_AMOLED, it) },
                        accentPalette = accentPalette,
                        onAccentPaletteChange = { accentPalette = it; saveString("PREF_ACCENT_PALETTE", it.name); saveBoolean(PREF_DYNAMIC, false); dynamicColors = false },
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
                            fun applyCategoryUpdate() {
                                if (selectedView in removed) selectedView = "Home"
                                categoryOptions = updated
                                preferences.edit().putStringSet(PREF_CUSTOM_CATEGORIES, updated.toMutableSet()).apply()
                                if (defaultNoteCategory !in updated) {
                                    defaultNoteCategory = updated.firstOrNull() ?: "Ideas"
                                    saveString(PREF_DEFAULT_CATEGORY, defaultNoteCategory)
                                }
                            }
                            if (removed.isEmpty()) applyCategoryUpdate()
                            else requestPersist(notes.map { if (it.category in removed) it.copy(category = fallback) else it }) {
                                applyCategoryUpdate()
                            }
                        },
                        editorTextSize = editorTextSize,
                        onEditorTextSizeChange = { editorTextSize = it; saveString(PREF_TEXT_SIZE, it.name) },
                        defaultMarkdownZoomPercent = defaultMarkdownZoomPercent,
                        onDefaultMarkdownZoomPercentChange = { value ->
                            defaultMarkdownZoomPercent = value.coerceIn(50, 200)
                            saveLong(PREF_MARKDOWN_ZOOM, defaultMarkdownZoomPercent.toLong())
                        },
                        showEditorCounts = showEditorCounts,
                        onShowEditorCountsChange = { showEditorCounts = it; saveBoolean(PREF_EDITOR_COUNTS, it) },
                        onResetDefaults = { resetPreferences() },
                        totalNotes = notes.count { !it.privateNote },
                        archivedNotes = notes.count { it.archived && !it.privateNote },
                        onOpenBackupRestore = { selectedView = "Backup" },
                        onOpenPrivateVaultSettings = { selectedView = "PrivateVaultSettings" },
                        onOpenCallRecordings = { selectedView = "CallRecordings" },
                        storageBackend = store.currentBackend(),
                        screenCaptureProtectionEnabled = screenCaptureProtectionEnabled,
                        onScreenCaptureProtectionChange = { enabled ->
                            screenCaptureProtectionEnabled = enabled
                            preferences.edit().putBoolean(PREF_PRIVATE_SCREEN_PROTECTION, enabled).apply()
                        },
                        onStorageBackendChange = { target ->
                            if (!storageLoaded || storageRecoveryRequired) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Resolve storage recovery before scheduling a storage change.") }
                            } else if (target == store.currentBackend()) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("That storage backend is already active.") }
                            } else {
                                runCatching { StorageBackendSwitchWorker.schedule(context, target) }
                                    .onSuccess {
                                        val backendName = if (target == NoteStorageBackend.SQLCIPHER) "SQLCipher" else "AES-256 Android Keystore"
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Change to $backendName scheduled for 5 hours from now. Current storage remains active until then.")
                                        }
                                    }
                                    .onFailure { error ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(error.message ?: "Could not schedule storage change.") }
                                    }
                            }
                        }
                    )
                } else if (selectedView == "About") {
                    AboutScreen(version = appVersion)
                } else if (selectedView == "Categories") {
                    AppCategoriesScreen(
                        notes = notes,
                        categories = categoryOptions,
                        onSelectCategory = { selectedView = it }
                    )
                } else {
                    AppDashboardHeader(
                        selectedView = selectedView,
                        notes = notes,
                        filteredCount = filteredNotes.size,
                        dark = dark,
                        layout = dashboardLayout,
                        sortByTitle = sortByTitle,
                        sortByCreated = sortByCreated,
                        canEncryptAll = notes.any { !it.privateNote && !it.encrypted },
                        onCreateNote = {
                            editorNoteId = null
                            editorDraftId = NoteStore.allocateNoteId(notes)
                            editorTemplate = null
                            editorVisible = true
                        },
                        onImportMarkdown = {
                            markdownImportLauncher.launch(arrayOf("text/markdown", "text/plain", "text/x-markdown", "application/octet-stream"))
                        },
                        onToggleTheme = {
                            val next = if (dark) ThemeMode.LIGHT else ThemeMode.DARK
                            themeMode = next
                            saveString(PREF_THEME, next.name)
                        },
                        onSetSort = { setSort(it) },
                        onEncryptAll = { confirmEncryptAll = true },
                        onToggleLayout = {
                            dashboardLayout = if (dashboardLayout == DashboardLayout.GRID) DashboardLayout.LIST else DashboardLayout.GRID
                            saveString(PREF_LAYOUT, dashboardLayout.name)
                        }
                    )
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
                        AppNotesEmptyState(
                            search = search,
                            selectedView = selectedView,
                            categoryOptions = categoryOptions,
                            onClearSearch = { search = "" }
                        )
                    } else {
                        AppNoteCollection(
                            notes = filteredNotes,
                            layout = dashboardLayout,
                            compact = dashboardDensity == DashboardDensity.COMPACT,
                            colorStyle = noteColorStyle,
                            showPreview = showNotePreviews,
                            showTimestamp = showNoteTimestamps,
                            selectedNoteIds = selectedNoteIds,
                            onOpen = { note ->
                                if (selectedNoteIds.isNotEmpty()) {
                                    selectedNoteIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id
                                } else if (note.encrypted) {
                                    toggleNoteEncryption(note.id)
                                } else {
                                    editorNoteId = note.id
                                    editorDraftId = note.id
                                    editorTemplate = null
                                    editorVisible = true
                                }
                            },
                            onSelect = { note ->
                                selectedNoteIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id
                            },
                            onPin = { note ->
                                requestPersist(notes.map {
                                    if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it
                                })
                            },
                            onDelete = { note -> deleteNote(note) },
                            onArchive = { note ->
                                if (!note.encrypted) requestPersist(notes.map {
                                    if (it.id == note.id) it.copy(
                                        archived = !it.archived,
                                        pinned = if (it.archived) it.pinned else false,
                                        updated = System.currentTimeMillis()
                                    ) else it
                                })
                            },
                            onToggleEncryption = { note -> toggleNoteEncryption(note.id) }
                        )
                    }
                }
                }
            }
        }

        if (showOrphanDraftDialog && pendingOrphanDrafts.isNotEmpty()) {
            val newest = pendingOrphanDrafts.first()
            val draftDateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
            val preview = newest.title.ifBlank {
                newest.body.lineSequence().firstOrNull { it.isNotBlank() }?.take(40).orEmpty()
            }.ifBlank { "Untitled" }
            OrphanDraftRecoveryDialog(
                count = pendingOrphanDrafts.size,
                preview = preview,
                savedAtLabel = draftDateFormatter.format(Date(newest.savedAtMillis)),
                onDismiss = { showOrphanDraftDialog = false },
                onRecover = {
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
                    requestPersist(notes + recovered) {
                        coroutineScope.launch(Dispatchers.IO) { runCatching { DraftStore(context).clear(newest.noteId) } }
                        pendingOrphanDrafts = pendingOrphanDrafts.drop(1)
                        if (pendingOrphanDrafts.isEmpty()) showOrphanDraftDialog = false
                        coroutineScope.launch { snackbarHostState.showSnackbar("Draft recovered") }
                    }
                },
                onDiscardAll = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            pendingOrphanDrafts.forEach { runCatching { DraftStore(context).clear(it.noteId) } }
                        }
                    }
                    pendingOrphanDrafts = emptyList()
                    showOrphanDraftDialog = false
                }
            )
        }

        pendingRestore?.let { incoming ->
            // Defense in depth: even archive restores must never introduce Private Vault records.
            val publicIncoming = incoming.filterNot { it.privateNote }
            val restoreIncoming = NoteStore.remapDraftIdCollisionForRestore(notes, publicIncoming, editorDraftId)
            val preview = NoteStore.merge(notes, restoreIncoming)
            val withinNoteLimit = NoteStore.isValidNoteCollection(preview.notes)
            BackupRestoreReviewDialog(
                addedCount = preview.added,
                updatedCount = preview.updated,
                withinNoteLimit = withinNoteLimit,
                maxNotes = NoteStore.MAX_NOTES,
                onDismiss = { pendingRestore = null },
                onRestore = {
                    requestPersist(preview.notes, allowRecovery = true) {
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
            )
        }

        ExternalMarkdownPreviewDialog(
            visible = showExternalMarkdownDialog,
            onVisibleChange = { showExternalMarkdownDialog = it },
            title = openedExternalMarkdownTitle,
            markdown = openedExternalMarkdownContent,
            zoomPercent = externalMarkdownZoomPercent,
            onZoomPercentChange = { externalMarkdownZoomPercent = it },
            showZoomControls = showExternalMarkdownZoomControls,
            onShowZoomControlsChange = { showExternalMarkdownZoomControls = it },
            moreMenuExpanded = externalMarkdownMoreMenuExpanded,
            onMoreMenuExpandedChange = { externalMarkdownMoreMenuExpanded = it },
            defaultZoomPercent = defaultMarkdownZoomPercent,
            context = context
        )

        if (showPrivateVaultGate) {
            PrivateVaultGateDialog(
                passwordConfigured = PrivateVaultSecurity.isConfigured(context),
                biometricEnabled = privateVaultBiometricEnabled && PrivateVaultBiometricKey(context).isEnrolled(),
                onDismiss = {
                    showPrivateVaultGate = false
                    pendingBiometricEnrollment = false
                },
                onSubmit = { password, confirmation, onResult ->
                    // First-time setup should offer biometric enrollment immediately after the
                    // password has successfully created and unlocked the vault.
                    val wasConfiguredBeforeSubmit = PrivateVaultSecurity.isConfigured(context)
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val configured = PrivateVaultSecurity.isConfigured(context)
                            val passwordAccepted = if (configured) {
                                when {
                                    PrivateVaultSecurity.isLockedOut(context) -> false
                                    PrivateVaultSecurity.verify(context, password) -> {
                                        PrivateVaultSecurity.clearFailedAttempts(context)
                                        true
                                    }
                                    else -> {
                                        PrivateVaultSecurity.recordFailedAttempt(context)
                                        false
                                    }
                                }
                            } else {
                                password.length >= PrivateVaultSecurity.MIN_PASSWORD_LENGTH &&
                                    password == confirmation && PrivateVaultSecurity.configure(context, password)
                            }
                            val session = if (passwordAccepted) {
                                val keyReady = if (privateNoteCrypto.isInitialized()) true
                                    else privateNoteCrypto.initialize(password.toCharArray())
                                if (keyReady) privateNoteCrypto.unlock(password.toCharArray()) else null
                            } else null
                            val unlocked = if (session == null) null else runCatching {
                                val encryptedExisting = if (privateNoteStore.hasStoredRecords()) {
                                    privateNoteStore.load(session)
                                } else emptyList()
                                val migrated = NoteStore.merge(encryptedExisting, legacyPrivateNotes).notes
                                check(privateNoteStore.save(session, migrated)) { "Private vault encryption failed" }
                                check(store.save(notes.filterNot { it.privateNote })) { "Public note store commit failed" }
                                session to migrated
                            }.getOrNull()
                            Triple(unlocked, passwordAccepted, session != null)
                        }
                        val unlocked = result.first
                        val accepted = unlocked != null
                        if (unlocked != null) {
                            privateVaultSession = unlocked.first
                            notes = notes.filterNot { it.privateNote } + unlocked.second
                            legacyPrivateNotes = emptyList()
                        }
                        onResult(accepted)
                        if (accepted) {
                            if (!wasConfiguredBeforeSubmit && privateVaultBiometricAvailable) {
                                pendingBiometricEnrollment = true
                            } else if (!wasConfiguredBeforeSubmit) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Vault created. Strong fingerprint/biometric unlock is not available on this device; you can still use your password.")
                                }
                            }
                            privateVaultUnlocked = true
                            selectedView = "Private"
                            search = ""
                            searchVisible = false
                            selectedNoteIds = emptySet()
                            showPrivateVaultGate = false
                            if (pendingBiometricEnrollment) {
                                pendingBiometricEnrollment = false
                                showBiometricEnrollment = true
                            }
                        } else if (result.second) {
                            snackbarHostState.showSnackbar("Vault could not be safely unlocked or migrated. Existing data was preserved; try again.")
                        }
                    }
                },
                onBiometricUnlock = { authenticatedCipher, onBiometricResult ->
                    coroutineScope.launch {
                        val unlocked = withContext(Dispatchers.IO) {
                            val session = PrivateVaultBiometricKey(context).unlock(authenticatedCipher)
                            if (session == null) null else runCatching {
                                val encryptedExisting = if (privateNoteStore.hasStoredRecords()) privateNoteStore.load(session) else emptyList()
                                val migrated = NoteStore.merge(encryptedExisting, legacyPrivateNotes).notes
                                check(privateNoteStore.save(session, migrated)) { "Private vault encryption failed" }
                                check(store.save(notes.filterNot { it.privateNote })) { "Public note store commit failed" }
                                session to migrated
                            }.getOrNull()
                        }
                        if (unlocked != null) {
                            onBiometricResult(true)
                            privateVaultSession = unlocked.first
                            notes = notes.filterNot { it.privateNote } + unlocked.second
                            legacyPrivateNotes = emptyList()
                            privateVaultUnlocked = true
                            selectedView = "Private"
                            search = ""
                            searchVisible = false
                            selectedNoteIds = emptySet()
                            showPrivateVaultGate = false
                        } else {
                            onBiometricResult(false)
                            snackbarHostState.showSnackbar("Biometric unlock failed safely. Use your vault password.")
                        }
                    }
                }
            )
        }

        if (showBiometricEnrollment) {
            val session = privateVaultSession
            if (session == null) {
                showBiometricEnrollment = false
            } else {
                PrivateVaultBiometricEnrollmentDialog(
                    session = session,
                    onComplete = { enrolled ->
                        if (enrolled) {
                            privateVaultBiometricEnabled = true
                            preferences.edit().putBoolean("private_vault_biometric_enabled", true).commit()
                            coroutineScope.launch { snackbarHostState.showSnackbar("Biometric unlock enabled. Your password remains available as recovery.") }
                        }
                        showBiometricEnrollment = false
                    },
                    onDismiss = {
                        PrivateVaultBiometricKey(context).deleteEnrollment()
                        privateVaultBiometricEnabled = false
                        preferences.edit().putBoolean("private_vault_biometric_enabled", false).commit()
                        showBiometricEnrollment = false
                    }
                )
            }
        }

        if (showStorageRecoveryDialog) {
            AppStorageRecoveryDialog(
                onDismiss = { showStorageRecoveryDialog = false },
                onOpenSettings = { showStorageRecoveryDialog = false; selectedView = "Settings" }
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
                        runCatching { com.nestnote.app.voicenote.VoiceNoteRepository(context).deleteAllForNote(draftId) }
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
                    pendingLinkedNoteAfterSave = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("This note changed elsewhere. Reopen it before saving to avoid overwriting newer content.")
                    }
                } else {
                    val savedId = existing?.id ?: editorDraftId ?: NoteStore.allocateNoteId(notes)
                    if (existing == null && NoteStore.hasDraftIdCollision(notes, savedId)) {
                        pendingLinkedNoteAfterSave = null
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
                    requestPersist(
                        next,
                        onSuccess = {
                            // Clear the recovery snapshot only after durable note persistence.
                            coroutineScope.launch(Dispatchers.IO) { runCatching { DraftStore(context).clear(savedId) } }
                            commitVoiceChanges()
                            closeEditor()
                            val linkedId = pendingLinkedNoteAfterSave
                            if (linkedId != null) {
                                pendingLinkedNoteAfterSave = null
                                editorNoteId = linkedId
                                editorDraftId = linkedId
                                editorTemplate = null
                                editorVisible = true
                            }
                            coroutineScope.launch { snackbarHostState.showSnackbar("Note saved") }
                        },
                        onFailure = { pendingLinkedNoteAfterSave = null }
                    )
                }
            }
            val openLinkedNote: (NestNote, NestNote) -> Unit = { current, target ->
                // A link embedded in an ordinary note must not bypass the vault gate.
                // Private notes are intentionally excluded from ordinary lists/search,
                // but linked-note navigation receives the full note collection.
                if (target.privateNote && !privateVaultUnlocked) {
                    requestPrivateVault()
                } else if (current != editorNote) {
                    pendingLinkedNoteAfterSave = target.id
                    saveNote(current)
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
                    requestPersist(notes + duplicate) {
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
                ConfirmSystemDiscardDialog(
                    isNewNote = editorNote == null,
                    onDiscard = discardEditor,
                    onKeepEditing = { confirmSystemDiscard = false }
                )
            }
        }
        if (confirmEncryptAll) {
            val eligible = OrdinaryNoteSelection.eligibleForEncryption(notes)
            ConfirmEncryptAllDialog(
                eligibleCount = eligible.size,
                onDismiss = { confirmEncryptAll = false },
                onConfirm = {
                    confirmEncryptAll = false
                    val replacements = runCatching { eligible.associate { it.id to encryptOrdinaryNote(it) } }.getOrElse { error ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(error.message ?: "Encrypt all failed; original notes were preserved.") }
                        emptyMap()
                    }
                    if (replacements.isNotEmpty()) {
                        requestPersist(OrdinaryNoteSelection.applyReplacements(notes, replacements)) {
                            selectedNoteIds = emptySet()
                            coroutineScope.launch { snackbarHostState.showSnackbar("Encrypted and verified ${replacements.size} ordinary notes") }
                        }
                    } else if (eligible.isEmpty()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("There are no unencrypted ordinary notes.") }
                    }
                }
            )
        }
        if (confirmBulkDelete) {
            val deleting = notes.filter { it.id in selectedNoteIds }
            ConfirmBulkDeleteDialog(
                count = deleting.size,
                onDismiss = { confirmBulkDelete = false },
                onConfirm = {
                    confirmBulkDelete = false
                    requestPersist(notes.filterNot { it.id in selectedNoteIds }) {
                        coroutineScope.launch(Dispatchers.IO) {
                            deleting.forEach { note ->
                                runCatching { com.nestnote.app.voicenote.VoiceNoteRepository(context).deleteAllForNote(note.id) }
                            }
                        }
                        selectedNoteIds = emptySet()
                        coroutineScope.launch { snackbarHostState.showSnackbar("Selected notes deleted") }
                    }
                }
            )
        }
        }

    }
}


private tailrec fun Context.findActivityForSecureWindow(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityForSecureWindow()
    else -> null
}
