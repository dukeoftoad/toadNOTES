package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Test

class VoicePermissionPolicyTest {
    @Test
    fun grantedPermissionStartsRecordingOnlyForPendingRecordRequest() {
        assertEquals(
            VoicePermissionDecision.START_RECORDING,
            resolveVoicePermissionResult(granted = true, requestPending = true)
        )
    }

    @Test
    fun deniedPermissionShowsGuidanceAndDoesNotStartRecording() {
        assertEquals(
            VoicePermissionDecision.SHOW_PERMISSION_GUIDANCE,
            resolveVoicePermissionResult(granted = false, requestPending = true)
        )
    }

    @Test
    fun retryAfterDenialCanStartWhenTheNextRequestIsGranted() {
        // A denied result consumes the pending request. Tapping Record again creates
        // a fresh pending request; its granted result is accepted.
        assertEquals(
            VoicePermissionDecision.SHOW_PERMISSION_GUIDANCE,
            resolveVoicePermissionResult(granted = false, requestPending = true)
        )
        assertEquals(
            VoicePermissionDecision.START_RECORDING,
            resolveVoicePermissionResult(granted = true, requestPending = true)
        )
    }

    @Test
    fun staleCallbackCannotStartOrOverwritePermissionGuidance() {
        assertEquals(
            VoicePermissionDecision.IGNORE_STALE_RESULT,
            resolveVoicePermissionResult(granted = true, requestPending = false)
        )
        assertEquals(
            VoicePermissionDecision.IGNORE_STALE_RESULT,
            resolveVoicePermissionResult(granted = false, requestPending = false)
        )
    }
}
