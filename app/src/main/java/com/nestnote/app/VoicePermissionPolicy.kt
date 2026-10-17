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
