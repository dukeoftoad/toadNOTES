package com.nestnote.app.voicenote

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.nestnote.app.VoiceAttachmentMetadata
import java.io.IOException
import java.util.Locale
import java.util.UUID

/**
 * Imports a user-selected audio document into voice-note storage.
 * Source bytes are streamed directly to the repository; plaintext is never staged on disk
 * and the complete file is never buffered in memory. Playback support remains codec/device-dependent.
 *
 * Files at or above [VoiceNoteRepository.PLAIN_STORAGE_MIN_BYTES] (30 MB) are stored WITHOUT
 * encryption ([VoiceNoteRepository.savePlain]) and played straight from disk like a normal
 * music player — decrypting-and-buffering a file that large peaked at ~2x its size on the
 * Java heap and stalled low-RAM devices. Smaller imports keep end-to-end AES-GCM encryption.
 */
class VoiceNoteImporter(
    context: Context,
    private val repository: VoiceNoteRepository = VoiceNoteRepository(context),
    private val resolver: ContentResolver = context.contentResolver,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    // Canonical dashed UUID form — matches the in-app recording path. (Older
    // imports stripped the dashes, which made their markers unparseable and
    // their chips render as "Voice note unavailable"; markers now accept both
    // forms, but new imports use the canonical one.)
    private val newAttachmentId: () -> String = { UUID.randomUUID().toString() }
) {
    private val appContext = context.applicationContext

    /**
     * Imports common audio documents including WAV, MP3, FLAC, AAC, M4A, OGG, Opus, AMR and 3GP.
     * Providers without a declared size are streamed and bounded by the encrypted store's size limit.
     */
    @Throws(IOException::class)
    fun importAudio(noteId: Long, uri: Uri): VoiceAttachmentMetadata {
        require(noteId > 0) { "Invalid note ID" }

        val mimeType = resolver.getType(uri)?.lowercase(Locale.ROOT)
        val providedName = queryDisplayName(uri)
        if (!VoiceAudioImportPolicy.isAudioDocument(providedName, mimeType)) throw IOException("Choose an audio file to import")
        val name = VoiceAudioImportPolicy.ensureAudioExtension(providedName, mimeType)

        val declaredSize = querySize(uri)
        if (declaredSize != null && declaredSize > VoiceNoteRepository.MAX_IMPORT_BYTES) {
            throw IOException("Audio file exceeds the ${VoiceNoteRepository.MAX_IMPORT_BYTES / (1024 * 1024)} MB limit")
        }

        val duration = readDurationMillis(uri)
        val input = resolver.openInputStream(uri)
            ?: throw IOException("Selected audio file could not be opened")
        input.use {
            val attachmentId = newAttachmentId()
            val createdAt = nowMillis()
            return if (VoiceAudioImportPolicy.storesPlain(declaredSize)) {
                repository.savePlain(
                    noteId = noteId,
                    attachmentId = attachmentId,
                    input = it,
                    createdAtMillis = createdAt,
                    durationMillis = duration,
                    displayName = name
                )
            } else {
                repository.save(
                    noteId = noteId,
                    attachmentId = attachmentId,
                    input = it,
                    createdAtMillis = createdAt,
                    durationMillis = duration,
                    displayName = name
                )
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index)?.trim()
                        ?.takeIf { it.isNotEmpty() }?.take(MAX_DISPLAY_NAME_LENGTH)
                    else null
                } else null
            } ?: "Imported audio"
        } catch (_: Exception) {
            "Imported audio"
        }
    }

    private fun querySize(uri: Uri): Long? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) cursor.getLong(index).takeIf { it >= 0L } else null
            } else null
        }
    }.getOrNull()

    private fun readDurationMillis(uri: Uri): Long = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        } finally {
            retriever.release()
        }
    }.getOrDefault(0L)

    private companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 160
    }
}
/** Pure import policy kept separate so extension/MIME behavior is covered by JVM tests. */
internal object VoiceAudioImportPolicy {
    private val audioExtensions = setOf(
        "aac", "aif", "aiff", "amr", "ape", "flac", "m4a", "m4b", "mka", "mp3",
        "mpc", "oga", "ogg", "opus", "wav", "weba", "webm", "wma", "3ga", "3gp"
    )

    /**
     * Files at or above 30 MB bypass encryption and play like a normal music player; unknown
     * sizes stay encrypted (providers that declare no size are rare and bounded by the limit).
     */
    fun storesPlain(declaredSizeBytes: Long?): Boolean =
        declaredSizeBytes != null && declaredSizeBytes >= VoiceNoteRepository.PLAIN_STORAGE_MIN_BYTES

    fun isAudioDocument(name: String, mimeType: String?): Boolean {
        if (mimeType?.startsWith("audio/") == true) return true
        return name.substringAfterLast('.', "").lowercase(Locale.ROOT) in audioExtensions
    }

    fun ensureAudioExtension(name: String, mimeType: String?): String {
        if (name.lastIndexOf('.') > 0 && name.substringAfterLast('.').isNotBlank()) return name
        val extension = when (mimeType) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/aac", "audio/aacp" -> "aac"
            "audio/ogg" -> "ogg"
            "audio/opus" -> "opus"
            "audio/amr", "audio/3gpp" -> "amr"
            "audio/flac" -> "flac"
            "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" -> "wav"
            "audio/webm" -> "webm"
            else -> null
        }
        return "$name." + (extension ?: "audio")
    }
}
