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
            .remove("private_vault_retry_boot_count")
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
        assertEquals(secondLockout, PrivateVaultSecurity.retryAtMillis(context, firstLockout))

        PrivateVaultSecurity.clearFailedAttempts(context)
        assertEquals(0L, PrivateVaultSecurity.retryAtMillis(context, now))
        assertFalse(PrivateVaultSecurity.isLockedOut(context, now))
    }

    @Test
    fun lockoutEscalationPersistsAndCapsAtFifteenMinutes() {
        assertTrue(PrivateVaultSecurity.configure(context, "correct horse"))
        var now = 5_000_000L
        repeat(4) { assertEquals(0L, PrivateVaultSecurity.recordFailedAttempt(context, now)) }

        val expectedDelays = listOf(30_000L, 60_000L, 120_000L, 240_000L, 480_000L, 900_000L, 900_000L)
        expectedDelays.forEach { delay ->
            val deadline = PrivateVaultSecurity.recordFailedAttempt(context, now)
            assertEquals(now + delay, deadline)
            assertTrue(PrivateVaultSecurity.isLockedOut(context, now))
            assertFalse(PrivateVaultSecurity.isLockedOut(context, deadline))
            // Simulate a fresh read of persisted state after process recreation.
            assertEquals(deadline, PrivateVaultSecurity.retryAtMillis(context, now))
            now = deadline
        }
    }

    @Test
    fun attemptsDuringActiveCooldownDoNotExtendOrBypassIt() {
        assertTrue(PrivateVaultSecurity.configure(context, "correct horse"))
        val now = 8_000_000L
        repeat(4) { PrivateVaultSecurity.recordFailedAttempt(context, now) }
        val deadline = PrivateVaultSecurity.recordFailedAttempt(context, now)

        assertEquals(deadline, PrivateVaultSecurity.recordFailedAttempt(context, now + 1L))
        assertEquals(deadline, PrivateVaultSecurity.retryAtMillis(context, now))
        assertTrue(PrivateVaultSecurity.isLockedOut(context, now))
        assertFalse(PrivateVaultSecurity.isLockedOut(context, deadline))
    }

    @Test
    fun mixedBackupRoundTripPreservesPrivateFlagAndOrdinaryViewsExcludePrivateNotes() {
        val public = NestNote(id = 101L, title = "Public", body = "visible", pinned = true)
        val private = NestNote(id = 102L, title = "Secret", body = "hidden searchable text",
            pinned = true, archived = false, privateNote = true, reminder = "2030-01-01")
        val archivedPrivate = NestNote(id = 103L, title = "Archived secret", body = "hidden",
            archived = true, privateNote = true)
        val restored = NoteStore.decodeBackup(NoteStore.encodeBackup(listOf(public, private, archivedPrivate)))
        assertEquals(listOf(public, private, archivedPrivate), restored)

        val notes = requireNotNull(restored)
        listOf("Home", "All notes", "Pinned", "Archived", "Reminders", "Ideas", "Work", "Personal").forEach { view ->
            assertFalse("private note leaked into $view",
                NoteFilters.visibleNotes(notes, view, "", false, false).any { it.privateNote })
        }
        listOf(
            "hidden searchable text", "is:pinned", "is:archived", "has:reminder",
            "category:Ideas", "in:Ideas"
        ).forEach { query ->
            assertFalse("private note leaked into query: $query",
                NoteFilters.visibleNotes(notes, "Home", query, false, false).any { it.privateNote })
            assertFalse("private note leaked into archived query: $query",
                NoteFilters.visibleNotes(notes, "Archived", query, false, false).any { it.privateNote })
        }
        assertEquals(listOf(private), NoteFilters.visibleNotes(notes, "Private", "", false, false))
    }


    @Test
    fun lockoutUsesMonotonicTimeAndRebootCannotBypassCooldown() {
        assertTrue(PrivateVaultSecurity.configure(context, "correct horse"))
        val beforeReboot = 50_000L
        repeat(4) { PrivateVaultSecurity.recordFailedAttempt(context, beforeReboot, 10) }
        val deadline = PrivateVaultSecurity.recordFailedAttempt(context, beforeReboot, 10)
        assertEquals(beforeReboot + 30_000L, deadline)
        assertTrue(PrivateVaultSecurity.isLockedOut(context, beforeReboot + 1L, 10))

        // A wall-clock adjustment cannot affect elapsed-realtime deadlines. After a
        // reboot, replace the old boot's time-base with a bounded cooldown.
        val afterReboot = 1_000L
        val rebootDeadline = PrivateVaultSecurity.retryAtMillis(context, afterReboot, 11)
        assertEquals(afterReboot + 15 * 60_000L, rebootDeadline)
        assertTrue(PrivateVaultSecurity.isLockedOut(context, afterReboot, 11))
        assertFalse(PrivateVaultSecurity.isLockedOut(context, rebootDeadline, 11))
    }

    @Test
    fun passwordSetupRejectsShortPasswords() {
        assertFalse(PrivateVaultSecurity.configure(context, "12345"))
        assertFalse(PrivateVaultSecurity.isConfigured(context))
    }
}
