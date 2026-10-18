package com.nestnote.app

/**
 * Interprets a permission callback only when it belongs to an outstanding Record tap.
 * Keeping stale callbacks inert prevents an old result from starting an unexpected recording.
 */
internal enum class VoicePermissionDecision {
    START_RECORDING,
    SHOW_PERMISSION_GUIDANCE,
    IGNORE_STALE_RESULT
}

internal fun resolveVoicePermissionResult(
    granted: Boolean,
    requestPending: Boolean
): VoicePermissionDecision = when {
    !requestPending -> VoicePermissionDecision.IGNORE_STALE_RESULT
    granted -> VoicePermissionDecision.START_RECORDING
    else -> VoicePermissionDecision.SHOW_PERMISSION_GUIDANCE
}

/** Prevents overlapping Record requests while permission, capture startup, or playback is active. */
internal fun canStartVoiceRecording(
    recording: Boolean,
    starting: Boolean,
    permissionRequestPending: Boolean,
    playbackActive: Boolean
): Boolean = !recording && !starting && !permissionRequestPending && !playbackActive


/** Formats PCM16 mono 16 kHz duration from the encrypted envelope size without decrypting audio. */
internal fun formatVoiceDuration(encryptedSizeBytes: Long): String {
    // Envelope: 4-byte magic + 12-byte IV + 16-byte GCM tag.
    val audioBytes = (encryptedSizeBytes - 32L).coerceAtLeast(0L)
    if (audioBytes == 0L) return "Duration unavailable"
    val totalSeconds = audioBytes / (16_000L * 2L)
    return "Duration %d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
