package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateVaultSecurityTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(PREF_PRIVATE_VAULT_SALT)
            .remove(PREF_PRIVATE_VAULT_HASH)
            .remove("private_vault_failed_attempts")
            .remove("private_vault_retry_at_millis")
            .commit()
    }

    @Test
    fun passwordIsHashedAndVerificationRejectsWrongPassword() {
        assertFalse(PrivateVaultSecurity.isConfigured(context))
        assertTrue(PrivateVaultSecurity.configure(context, "correct horse"))
        assertTrue(PrivateVaultSecurity.isConfigured(context))
        assertTrue(PrivateVaultSecurity.verify(context, "correct horse"))
        assertFalse(PrivateVaultSecurity.verify(context, "incorrect horse"))
        assertFalse(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PREF_PRIVATE_VAULT_HASH, "").orEmpty().contains("correct horse"))
    }

    @Test
    fun passwordCanBeChangedOnlyAfterCurrentPasswordVerification() {
        assertTrue(PrivateVaultSecurity.configure(context, "original password"))
        assertFalse(PrivateVaultSecurity.changePassword(context, "wrong password", "updated password", "updated password"))
        assertFalse(PrivateVaultSecurity.changePassword(context, "original password", "short", "short"))
        assertFalse(PrivateVaultSecurity.changePassword(context, "original password", "updated password", "does not match"))
        assertTrue(PrivateVaultSecurity.changePassword(context, "original password", "updated password", "updated password"))
        assertFalse(PrivateVaultSecurity.verify(context, "original password"))
        assertTrue(PrivateVaultSecurity.verify(context, "updated password"))
    }

    @Test
    fun failedPasswordAttemptsPersistBackoffAndSuccessfulAuthenticationCanResetIt() {
        assertTrue(PrivateVaultSecurity.configure(context, "correct horse"))
        val now = 1_000_000L
        repeat(4) {
            assertEquals(0L, PrivateVaultSecurity.recordFailedAttempt(context, now))
        }
        val firstLockout = PrivateVaultSecurity.recordFailedAttempt(context, now)
        assertEquals(now + 30_000L, firstLockout)
        assertTrue(PrivateVaultSecurity.isLockedOut(context, now))
        assertFalse(PrivateVaultSecurity.isLockedOut(context, firstLockout))

        val secondLockout = PrivateVaultSecurity.recordFailedAttempt(context, firstLockout)
        assertEquals(firstLockout + 60_000L, secondLockout)
        assertTrue(PrivateVaultSecurity.isLockedOut(context, firstLockout))
        assertEquals(secondLockout, PrivateVaultSecurity.retryAtMillis(context))

        PrivateVaultSecurity.clearFailedAttempts(context)
        assertEquals(0L, PrivateVaultSecurity.retryAtMillis(context))
        assertFalse(PrivateVaultSecurity.isLockedOut(context, now))
    }

    @Test
    fun passwordSetupRejectsShortPasswords() {
        assertFalse(PrivateVaultSecurity.configure(context, "12345"))
        assertFalse(PrivateVaultSecurity.isConfigured(context))
    }
}
