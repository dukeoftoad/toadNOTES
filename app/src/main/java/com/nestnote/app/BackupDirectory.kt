package com.nestnote.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

/**
 * Uses a persisted Storage Access Framework tree grant so encrypted archives can be written to
 * Documents/NESTNOTE without showing a picker each time.
 *
 * Android intentionally requires a one-time user-granted directory permission. This
 * helper never attempts broad storage permission or direct filesystem access to Documents.
 */
internal object BackupDirectory {
    const val PREFERENCE_KEY = "backup_documents_tree_uri"
    private const val FOLDER_NAME = "NESTNOTE"
    private const val ENCRYPTED_BACKUP_NAME = "NESTNOTE-backup.nestnote"
    private const val MIME_ENCRYPTED = "application/octet-stream"

    fun persistTreeGrant(context: Context, treeUri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString(PREFERENCE_KEY, treeUri.toString()).commit()
    }.getOrDefault(false)

    fun configuredTreeUri(context: Context): Uri? =
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .getString(PREFERENCE_KEY, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun writeEncryptedLatest(context: Context, treeUri: Uri, bytes: ByteArray): Boolean = runCatching {
        require(bytes.isNotEmpty() && bytes.size <= NoteStore.MAX_ARCHIVE_PAYLOAD_BYTES)
        val folder = nestNoteFolder(context, treeUri, create = true) ?: error("Cannot access Documents/NESTNOTE")
        writeSafely(context, folder, ENCRYPTED_BACKUP_NAME, MIME_ENCRYPTED, bytes)
    }.getOrDefault(false)

    fun readEncryptedLatest(context: Context, treeUri: Uri): ByteArray? = runCatching {
        val folder = nestNoteFolder(context, treeUri, create = false) ?: return null
        val file = folder.findFile(ENCRYPTED_BACKUP_NAME) ?: return null
        val output = java.io.ByteArrayOutputStream()
        context.contentResolver.openInputStream(file.uri)?.use { input ->
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > NoteStore.MAX_ARCHIVE_PAYLOAD_BYTES) throw IOException("Encrypted backup is too large")
                output.write(buffer, 0, count)
            }
        } ?: return null
        output.toByteArray()
    }.getOrNull()


    /**
     * Stage a complete replacement before touching the last good backup. SAF providers do not
     * promise atomic output streams, so write to a sibling temporary document, then rotate names.
     * If the final rename fails, attempt to restore the previous document.
     */
    private fun writeSafely(
        context: Context,
        folder: DocumentFile,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        val temporaryName = "$fileName.tmp"
        val previousName = "$fileName.previous"
        var temporary: DocumentFile? = null
        var previous: DocumentFile? = null
        var oldMoved = false
        try {
            folder.findFile(temporaryName)?.let {
                check(it.delete()) { "Cannot clear stale temporary backup" }
            }
            temporary = folder.createFile(mimeType, temporaryName)
                ?: error("Cannot create temporary backup")
            context.contentResolver.openOutputStream(temporary.uri, "wt")?.use { output ->
                output.write(bytes)
                output.flush()
            } ?: error("Cannot open temporary backup")
            val verified = context.contentResolver.openInputStream(temporary.uri)?.use { input ->
                // SAF providers are external and may return more data than was written. Bound
                // verification reads to the expected payload instead of readBytes(), which could
                // allocate an unbounded array if a provider returns a corrupt/hostile stream.
                val output = java.io.ByteArrayOutputStream(bytes.size)
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= bytes.size) { "Temporary backup is larger than expected" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: error("Cannot verify temporary backup")
            check(verified.size == bytes.size && verified.contentEquals(bytes)) {
                "Temporary backup verification failed"
            }

            previous = folder.findFile(previousName)
            if (previous != null) check(previous.delete()) { "Cannot clear previous backup snapshot" }
            val existing = folder.findFile(fileName)
            if (existing != null) {
                check(existing.renameTo(previousName)) { "Cannot preserve previous backup" }
                oldMoved = true
                previous = folder.findFile(previousName)
            }

            check(temporary.renameTo(fileName)) { "Cannot publish completed backup" }
            temporary = null
            // The newly published file is now the good copy; old cleanup is best effort.
            previous?.delete()
            previous = null
            return true
        } catch (failure: Exception) {
            temporary?.delete()
            if (oldMoved && folder.findFile(fileName) == null) {
                previous?.renameTo(fileName)
            }
            throw failure
        }
    }

    fun encryptedBackupName(): String = ENCRYPTED_BACKUP_NAME

    fun folderName(): String = FOLDER_NAME

    private fun nestNoteFolder(context: Context, treeUri: Uri, create: Boolean): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val existing = root.findFile(FOLDER_NAME)
        if (existing != null) return existing.takeIf { it.isDirectory }
        if (!create) return null
        return root.createDirectory(FOLDER_NAME)
    }
}
