package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateVaultPasswordChangeTest {
    @Test fun keyRewrapFailureDoesNotUpdateVerifier() {
        var verifierUpdated = false
        var recoveryRequired = false
        val result = PrivateVaultPasswordChange.execute(
            current = "old-password",
            replacement = "new-password",
            rewrapKey = { _, _ -> false },
            updateVerifier = { verifierUpdated = true; true },
            markRecoveryRequired = { recoveryRequired = true }
        )
        assertFalse(result.accepted)
        assertEquals(PrivateVaultPasswordChange.Failure.KEY_REWRAP, result.failure)
        assertFalse(verifierUpdated)
        assertFalse(recoveryRequired)
    }

    @Test fun verifierFailureRollsKeyBackToOriginalPassword() {
        val attempts = mutableListOf<Pair<String, String>>()
        var recoveryRequired = false
        val result = PrivateVaultPasswordChange.execute(
            current = "old-password",
            replacement = "new-password",
            rewrapKey = { from, to -> attempts += from to to; true },
            updateVerifier = { false },
            markRecoveryRequired = { recoveryRequired = true }
        )
        assertFalse(result.accepted)
        assertEquals(PrivateVaultPasswordChange.Failure.VERIFIER_UPDATE, result.failure)
        assertEquals(listOf("old-password" to "new-password", "new-password" to "old-password"), attempts)
        assertFalse(result.recoveryRequired)
        assertFalse(recoveryRequired)
    }

    @Test fun rollbackFailureLocksIntoRecoveryRequiredState() {
        var recoveryRequired = false
        var calls = 0
        val result = PrivateVaultPasswordChange.execute(
            current = "old-password",
            replacement = "new-password",
            rewrapKey = { _, _ -> ++calls == 1 },
            updateVerifier = { false },
            markRecoveryRequired = { recoveryRequired = true }
        )
        assertFalse(result.accepted)
        assertTrue(result.recoveryRequired)
        assertEquals(PrivateVaultPasswordChange.Failure.ROLLBACK, result.failure)
        assertTrue(recoveryRequired)
    }

    @Test fun successfulTwoPhaseChangeDoesNotAttemptRollback() {
        var calls = 0
        val result = PrivateVaultPasswordChange.execute(
            current = "old-password",
            replacement = "new-password",
            rewrapKey = { _, _ -> calls++; true },
            updateVerifier = { true },
            markRecoveryRequired = { throw AssertionError("not expected") }
        )
        assertTrue(result.accepted)
        assertFalse(result.recoveryRequired)
        assertEquals(1, calls)
    }
}
