package com.nestnote.app

import android.media.MediaDataSource
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CallRecordingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val playbackScope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences(CallRecordingImport.PREFS, 0) }
    var enabled by remember { mutableStateOf(CallRecordingImport.enabled(context)) }
    var sourceUri by remember { mutableStateOf(CallRecordingImport.sourceTree(context)) }
    var status by remember { mutableStateOf(CallRecordingImport.status(context)) }
    var recordings by remember { mutableStateOf(CallRecordingImport.items(context)) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var pendingAction by remember { mutableStateOf<String?>("open") }
    var authenticated by remember { mutableStateOf(false) }
    var selectedPlayback by remember { mutableStateOf<String?>(null) }
    var playbackPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                authenticated = false
                pendingAction = "open"
                runCatching { playbackPlayer?.stop() }
                runCatching { playbackPlayer?.release() }
                playbackPlayer = null
                selectedPlayback = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var playbackError by remember { mutableStateOf("") }
    var folderPickerEnables by remember { mutableStateOf(true) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val chosenFolder = runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull()
            if (chosenFolder == null || !chosenFolder.isDirectory || !chosenFolder.canRead()) {
                status = CallRecordingImport.STATUS_SOURCE_UNAVAILABLE
            } else if (CallRecordingImport.verifyEncryptionReady(context) &&
                CallRecordingImport.persistTreeGrant(context, uri)
            ) {
                sourceUri = uri
                if (folderPickerEnables) {
                    prefs.edit().putBoolean(CallRecordingImport.KEY_ENABLED, true).commit()
                    enabled = true
                }
                CallRecordingImport.setStatus(context, CallRecordingImport.STATUS_READY)
                if (enabled) CallRecordingImport.schedule(context)
                status = CallRecordingImport.status(context)
            } else {
                status = "Folder permission or encrypted storage verification failed — nothing was enabled"
                prefs.edit().putBoolean(CallRecordingImport.KEY_ENABLED, false).apply()
                enabled = false
            }
        }
    }

    LaunchedEffect(enabled, sourceUri) {
        if (enabled && sourceUri != null) {
            CallRecordingImport.schedule(context)
            delay(350)
            status = CallRecordingImport.status(context)
            recordings = CallRecordingImport.items(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { playbackPlayer?.stop() }
            runCatching { playbackPlayer?.release() }
            playbackPlayer = null
        }
    }

    val visibleItems = recordings.filter { item ->
        val matchesQuery = query.isBlank() || item.displayName.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            "Imported" -> item.status == CallRecordingImport.ITEM_IMPORTED
            "Pending" -> item.status == CallRecordingImport.ITEM_PENDING
            "Failed" -> item.status == CallRecordingImport.ITEM_FAILED
            else -> true
        }
        matchesQuery && matchesFilter
    }

    if (authenticated) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Call recordings") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
    }) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Text("Protected by encrypted storage", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "NESTNOTE does not record calls. It imports recordings that your dialer has already created and that Android lets you access."
                        )
                        Text(
                            "Imported copies are encrypted on this device. Deleting a copy here never deletes the original recording.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Automatically import recordings", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (enabled) "On — eligible files are checked in the selected folder"
                                    else "Off by default. Turn on to begin setup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = enabled, onCheckedChange = { turnOn ->
                                pendingAction = if (turnOn) "enable" else "disable"
                                password = ""
                                passwordError = ""
                            })
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Source, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("Recording source", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    sourceUri?.let { runCatching { DocumentFile.fromTreeUri(context, it)?.name }.getOrNull() }
                                        ?: "No folder selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = {
                                folderPickerEnables = enabled
                                pendingAction = "change_folder"
                                password = ""
                                passwordError = ""
                            }) { Text(if (sourceUri == null) "Choose" else "Change") }
                        }
                        Text("Status: $status", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            onClick = {
                                CallRecordingImport.setStatus(context, CallRecordingImport.STATUS_READY)
                                CallRecordingImport.schedule(context)
                                status = CallRecordingImport.status(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Check folder now")
                        }
                    }
                }
            }
            item {
                Text("Private recordings", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Search is limited to recordings imported into this encrypted list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(status, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        if (enabled && sourceUri != null) {
                            CallRecordingImport.schedule(context)
                            status = "Checking source folder…"
                            playbackScope.launch {
                                delay(700)
                                status = CallRecordingImport.status(context)
                                recordings = CallRecordingImport.items(context)
                            }
                        } else {
                            status = if (sourceUri == null) CallRecordingImport.STATUS_NEEDS_FOLDER
                            else CallRecordingImport.STATUS_DISABLED
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Scan now")
                    }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Search recordings") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Imported", "Pending", "Failed").forEach { value ->
                        FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
                    }
                }
            }
            if (visibleItems.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Text(
                                when {
                                    filter == "Failed" && status.contains("failed", ignoreCase = true) -> status
                                    filter == "Pending" && status.contains("waiting", ignoreCase = true) -> status
                                    recordings.isEmpty() -> "No imported recordings yet"
                                    else -> "No matching recordings"
                                }
                            )
                            Text(
                                "Choose a folder that contains audio files created by your dialer, then run a folder check.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            items(visibleItems, key = { it.sourceUri }) { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(item.importedAtMillis))} · " +
                                when (item.status) {
                                    CallRecordingImport.ITEM_IMPORTED ->
                                        if (item.durationMillis > 0) "${item.durationMillis / 1000}s · Encrypted" else "Encrypted"
                                    CallRecordingImport.ITEM_PENDING -> "Pending import"
                                    else -> "Import failed"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.status != CallRecordingImport.ITEM_IMPORTED) {
                            if (item.error.isNotBlank()) {
                                Text(item.error, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    CallRecordingImport.retry(context, item.sourceUri)
                                    status = "Retry queued for ${item.displayName}"
                                    playbackScope.launch {
                                        delay(500)
                                        status = CallRecordingImport.status(context)
                                        recordings = CallRecordingImport.items(context)
                                    }
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Retry import")
                                }
                            }
                        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (selectedPlayback == item.attachmentId) {
                                    runCatching { playbackPlayer?.stop() }
                                    runCatching { playbackPlayer?.release() }
                                    playbackPlayer = null
                                    selectedPlayback = null
                                } else {
                                    runCatching { playbackPlayer?.release() }
                                    playbackPlayer = null
                                    playbackError = ""
                                    selectedPlayback = item.attachmentId
                                    playbackScope.launch {
                                        var bytes: ByteArray? = null
                                        var player: MediaPlayer? = null
                                        try {
                                            bytes = withContext(Dispatchers.IO) {
                                                VoiceAttachmentStore(context).read(
                                                    CallRecordingImport.RECORDINGS_NOTE_ID, item.attachmentId
                                                )
                                            }
                                            // The screen may have relocked while encrypted bytes were being read.
                                            val payload = requireNotNull(bytes)
                                            if (!authenticated || selectedPlayback != item.attachmentId) {
                                                payload.fill(0)
                                                return@launch
                                            }
                                            player = MediaPlayer()
                                            val activePlayer = requireNotNull(player)
                                            activePlayer.setDataSource(object : MediaDataSource() {
                                                override fun getSize(): Long = payload.size.toLong()
                                                override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
                                                    if (position < 0L || position >= payload.size) return -1
                                                    val count = minOf(size, payload.size - position.toInt())
                                                    System.arraycopy(payload, position.toInt(), buffer, offset, count)
                                                    return count
                                                }
                                                override fun close() { payload.fill(0) }
                                            })
                                            activePlayer.setOnCompletionListener {
                                                runCatching { it.release() }
                                                payload.fill(0)
                                                if (playbackPlayer === it) playbackPlayer = null
                                                selectedPlayback = null
                                            }
                                            activePlayer.setOnErrorListener { mp, _, _ ->
                                                runCatching { mp.release() }
                                                payload.fill(0)
                                                if (playbackPlayer === mp) playbackPlayer = null
                                                selectedPlayback = null
                                                playbackError = "Could not play this recording"
                                                true
                                            }
                                            activePlayer.setOnPreparedListener {
                                                if (authenticated && selectedPlayback == item.attachmentId) {
                                                    it.start()
                                                } else {
                                                    payload.fill(0)
                                                    runCatching { it.release() }
                                                }
                                            }
                                            playbackPlayer = activePlayer
                                            activePlayer.prepareAsync()
                                        } catch (error: Exception) {
                                            bytes?.fill(0)
                                            runCatching { player?.release() }
                                            playbackError = error.message ?: "Could not play this recording"
                                            selectedPlayback = null
                                        }
                                    }
                                }
                            }) {
                                Icon(if (selectedPlayback == item.attachmentId) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(if (selectedPlayback == item.attachmentId) "Stop" else "Play")
                            }
                            OutlinedButton(onClick = {
                                runCatching { playbackPlayer?.stop() }
                                runCatching { playbackPlayer?.release() }
                                playbackPlayer = null
                                selectedPlayback = null
                                if (CallRecordingImport.removeImportedCopy(context, item.attachmentId)) {
                                    recordings = CallRecordingImport.items(context)
                                } else playbackError = "Encrypted copy could not be deleted"
                            }) { Text("Delete copy") }
                        }
                    }
                }
            }
            if (playbackError.isNotBlank()) item {
                Text(playbackError, color = MaterialTheme.colorScheme.error)
            }
            item { Spacer(Modifier.size(16.dp)) }
        }
    }

    } else {
        Scaffold(topBar = {
            TopAppBar(title = { Text("Call recordings") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            })
        }) { insets ->
            Column(
                Modifier.fillMaxSize().padding(insets).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Text("Private recordings are locked", style = MaterialTheme.typography.titleLarge)
                Text("Authenticate with your Private Vault password to view or manage imported recordings.")
                Button(onClick = {
                    pendingAction = "open"
                    password = ""
                    passwordError = ""
                }) { Text("Unlock with password") }
                val host = remember(context) { context.findCallRecordingsActivity() }
                val biometricAvailable = remember(host) {
                    host != null && BiometricManager.from(host).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                    ) == BiometricManager.BIOMETRIC_SUCCESS &&
                        PrivateVaultBiometricKey(context).isEnrolled()
                }
                if (biometricAvailable) {
                    OutlinedButton(onClick = {
                        val activity = host ?: return@OutlinedButton
                        val biometricKey = PrivateVaultBiometricKey(context)
                        val cipher = biometricKey.createUnlockCipher()
                        if (cipher == null) {
                            passwordError = "Biometric key unavailable. Use your vault password."
                            pendingAction = "open"
                            return@OutlinedButton
                        }
                        val prompt = BiometricPrompt(
                            activity,
                            ContextCompat.getMainExecutor(activity),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    val session = biometricKey.unlock(result.cryptoObject?.cipher)
                                    if (session != null) {
                                        authenticated = true
                                        pendingAction = null
                                        passwordError = ""
                                    } else {
                                        passwordError = "Biometric vault unlock failed safely. Use your password."
                                        pendingAction = "open"
                                    }
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                                        errorCode != BiometricPrompt.ERROR_CANCELED
                                    ) passwordError = errString.toString()
                                    pendingAction = "open"
                                }
                            }
                        )
                        val info = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Unlock Private Vault")
                            .setSubtitle("Authenticate to view private call recordings")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .setNegativeButtonText("Use password")
                            .build()
                        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
                    }) { Text("Unlock with fingerprint") }
                }
                if (passwordError.isNotBlank()) {
                    Text(passwordError, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null; password = "" },
            title = { Text("Confirm Private Vault access") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your vault password to continue. Cancel leaves the setting and source unchanged.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = "" },
                        label = { Text("Vault password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    if (passwordError.isNotBlank()) Text(passwordError, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!PrivateVaultSecurity.isConfigured(context)) {
                        passwordError = "Set up a Private Vault password first."
                    } else if (PrivateVaultSecurity.isLockedOut(context)) {
                        passwordError = "Vault access is temporarily locked. Try again after the cooldown."
                    } else if (!PrivateVaultSecurity.verify(context, password)) {
                        PrivateVaultSecurity.recordFailedAttempt(context)
                        passwordError = "Password not accepted. No changes were made."
                    } else {
                        PrivateVaultSecurity.clearFailedAttempts(context)
                        val action = pendingAction
                        authenticated = true
                        pendingAction = null
                        password = ""
                        when (action) {
                            "open" -> Unit
                            "enable" -> if (sourceUri == null) folderPicker.launch(null) else {
                                prefs.edit().putBoolean(CallRecordingImport.KEY_ENABLED, true).commit()
                                enabled = true
                                CallRecordingImport.schedule(context)
                            }
                            "change_folder" -> folderPicker.launch(sourceUri)
                            "disable" -> {
                                prefs.edit().putBoolean(CallRecordingImport.KEY_ENABLED, false).commit()
                                enabled = false
                                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("call_recording_import_scan")
                                CallRecordingImport.setStatus(context, CallRecordingImport.STATUS_DISABLED)
                                status = CallRecordingImport.status(context)
                            }
                        }
                    }
                }) { Text("Authenticate") }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null; password = "" }) { Text("Cancel") } }
        )
    }
}


private tailrec fun Context.findCallRecordingsActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findCallRecordingsActivity()
    else -> null
}
