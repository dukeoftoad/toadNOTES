package com.nestnote.app

import android.content.Context
import android.media.AudioManager
import java.util.UUID

/**
 * Starts a voice recording session and calls [onStarted] with the live session and the
 * audio-focus listener that must be released when playback or recording ends.
 *
 * The attachment id is registered in [addedIds] before background IO starts so a
 * discard triggered mid-recording can find and clean up the file once the session drains.
 */
internal fun startVoiceRecording(
    context: Context,
    audioManager: AudioManager,
    repository: com.nestnote.app.voicenote.VoiceNoteRepository,
    noteId: Long,
    addedIds: MutableSet<String>,
    onStarted: (EncryptedPcmRecording, AudioManager.OnAudioFocusChangeListener) -> Unit,
    onStatus: (String) -> Unit,
    onInterrupted: () -> Unit
) {
    val focusListener = requestVoiceAudioFocus(audioManager, onInterrupted)
    if (focusListener == null) {
        onStatus("Audio focus is unavailable. Stop other audio and try again.")
        return
    }
    val id = UUID.randomUUID().toString()
    addedIds.add(id)
    val session = EncryptedPcmRecording(context, repository, noteId, id) { result ->
        abandonVoiceAudioFocus(audioManager, focusListener)
        // Discard can finish before this main-thread callback is dispatched. Never let
        // a late successful worker callback recreate the manifest entry for discarded audio.
        if (id !in addedIds) {
            runCatching { repository.deletePayload(noteId, id) }
            return@EncryptedPcmRecording
        }
        when {
            result.isSuccess -> {
                // Save encrypted bytes only. Manifest entry is created after explicit Attach.
                onStatus("Recording ready for review: $id")
            }
            else -> onStatus(result.exceptionOrNull()?.message ?: "Couldn't save recording")
        }
    }
    try {
        session.start()
        onStarted(session, focusListener)
    } catch (error: Exception) {
        // A partially started session may already have a storage worker. Mark this
        // attachment discarded first, then wait for workers before deleting its payload.
        addedIds.remove(id)
        runCatching { session.discardAndWait() }
        runCatching { repository.deletePayload(noteId, id) }
        abandonVoiceAudioFocus(audioManager, focusListener)
        onStatus(error.message ?: "Couldn't start recording")
    }
}
