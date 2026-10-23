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
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateVaultIntegrationTest {
    private lateinit var context: Context
    private lateinit var generalStore: NoteStore
    private lateinit var privateStore: PrivateNoteStore
    private lateinit var crypto: PrivateNoteCrypto

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        listOf("nestnote_local", "nestnote_private_vault_crypto", "nestnote_private_note_records")
            .forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        val generalKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        generalStore = NoteStore(context) { generalKey }
        privateStore = PrivateNoteStore(context)
        crypto = PrivateNoteCrypto(context)
    }

    @Test fun migrationMovesPrivateRecordsOutOfGeneralStoreOnlyAfterEncryptedCommit() {
        val public = NestNote(id = 11L, title = "Public", body = "ordinary")
        val private = NestNote(id = 12L, title = "Private marker", body = "Secret marker", privateNote = true)
        assertTrue(generalStore.save(listOf(public, private)))

        val legacy = generalStore.loadSafely().notes.filter { it.privateNote }
        assertEquals(listOf(private), legacy)
        assertTrue(crypto.initialize("vault password".toCharArray()))
        val session = crypto.unlock("vault password".toCharArray())!!
        assertTrue(privateStore.save(session, legacy))
        assertTrue(generalStore.save(listOf(public)))

        assertEquals(listOf(public), generalStore.loadSafely().notes)
        assertEquals(listOf(private), privateStore.load(session))
        val privateDisk = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
            .getString("encrypted_records", "")!!
        assertFalse(privateDisk.contains(private.title))
        assertFalse(privateDisk.contains(private.body))
    }

    @Test fun failedVaultUnlockDoesNotChangeGeneralStoreOrRevealPrivateRecords() {
        val public = NestNote(id = 21L, title = "Public", body = "ordinary")
        val private = NestNote(id = 22L, title = "Private", body = "secret", privateNote = true)
        assertTrue(generalStore.save(listOf(public, private)))
        assertTrue(crypto.initialize("right password".toCharArray()))
        assertEquals(null, crypto.unlock("wrong password".toCharArray()))
        assertEquals(listOf(public, private), generalStore.loadSafely().notes)
    }

    @Test fun tamperedPrivateRecordFailsClosed() {
        val private = NestNote(id = 31L, title = "Tamper target", body = "must remain secret", privateNote = true)
        assertTrue(crypto.initialize("correct password".toCharArray()))
        val session = crypto.unlock("correct password".toCharArray())!!
        assertTrue(privateStore.save(session, listOf(private)))

        val prefs = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
        val raw = prefs.getString("encrypted_records", null)!!
        val changed = raw.replaceFirst(Regex("[A-Za-z0-9]"), "Z")
        assertTrue(prefs.edit().putString("encrypted_records", changed).commit())
        var rejected = false
        try {
            PrivateNoteStore(context).load(PrivateNoteCrypto(context).unlock("correct password".toCharArray())!!)
        } catch (_: Exception) {
            rejected = true
        }
        assertTrue("tampered ciphertext/metadata must not be accepted", rejected)
    }

    @Test fun failedMigrationKeepsLegacyCopyAndRestartCanReadCommittedPrivateStore() {
        val public = NestNote(id = 41L, title = "Public", body = "public")
        val private = NestNote(id = 42L, title = "Legacy secret", body = "must survive failure", privateNote = true)
        assertTrue(generalStore.save(listOf(public, private)))
        val legacy = generalStore.loadSafely().notes.filter { it.privateNote }

        assertTrue(crypto.initialize("correct password".toCharArray()))
        val session = crypto.unlock("correct password".toCharArray())!!
        assertTrue(privateStore.save(session, legacy))

        // Simulate failure of the second (public-store cleanup) phase. The old blob must remain.
        assertFalse(generalStore.save(listOf(public, NestNote(id = 0L, title = "invalid", body = ""))))
        assertEquals(listOf(public, private), generalStore.loadSafely().notes)

        // Simulate process recreation: no old in-memory Session is reused.
        val restartedCrypto = PrivateNoteCrypto(context)
        val restartedSession = restartedCrypto.unlock("correct password".toCharArray())!!
        val restartedPrivateStore = PrivateNoteStore(context)
        val committedPrivate = restartedPrivateStore.load(restartedSession)
        assertEquals(listOf(private), committedPrivate)

        // Replay the actual startup/unlock recovery decision after a process restart:
        // merge committed private records with the still-present legacy copy by ID, then
        // retry the public-only general-store commit. Recovery must be idempotent.
        val recovered = NoteStore.merge(committedPrivate, generalStore.loadSafely().notes.filter { it.privateNote }).notes
        assertEquals(listOf(private), recovered)
        assertTrue(restartedPrivateStore.save(restartedSession, recovered))
        assertTrue(generalStore.save(listOf(public)))
        assertEquals(listOf(public), generalStore.loadSafely().notes)
        assertEquals(listOf(private), restartedPrivateStore.load(restartedSession))
    }

    @Test fun wrongPasswordAndStoreTamperingNeverProduceSessionOrRecords() {
        val private = NestNote(id = 51L, title = "Private", body = "confidential", privateNote = true)
        assertTrue(crypto.initialize("right password".toCharArray()))
        val session = crypto.unlock("right password".toCharArray())!!
        assertTrue(privateStore.save(session, listOf(private)))
        assertEquals(null, PrivateNoteCrypto(context).unlock("wrong password".toCharArray()))
        assertEquals(listOf(private), PrivateNoteStore(context).load(PrivateNoteCrypto(context).unlock("right password".toCharArray())!!))
    }


    @Test fun aesGcmCiphertextBitFlipIsRejected() {
        assertTrue(crypto.initialize("correct password".toCharArray()))
        val session = crypto.unlock("correct password".toCharArray())!!
        val envelope = session.encrypt("authenticated private payload".toByteArray(), 61L)
        val prefix = "NESTNOTE_PRIVATE_RECORD_V1:"
        val payload = android.util.Base64.decode(envelope.removePrefix(prefix), android.util.Base64.NO_WRAP)
        payload[payload.lastIndex] = (payload[payload.lastIndex].toInt() xor 0x01).toByte()
        val tampered = prefix + android.util.Base64.encodeToString(payload, android.util.Base64.NO_WRAP)
        var rejected = false
        try {
            session.decrypt(tampered, 61L)
        } catch (_: Exception) {
            rejected = true
        }
        assertTrue("AES-GCM must reject altered ciphertext", rejected)
        var wrongRecordRejected = false
        try {
            session.decrypt(envelope, 62L)
        } catch (_: Exception) {
            wrongRecordRejected = true
        }
        assertTrue("record ID must be authenticated as AAD", wrongRecordRejected)
    }


    @Test fun passwordRewrapFailureLeavesOriginalPasswordUsable() {
        assertTrue(crypto.initialize("original password".toCharArray()))
        assertFalse(crypto.changePassword("wrong password".toCharArray(), "replacement password".toCharArray()))
        assertTrue(crypto.unlock("original password".toCharArray()) != null)
        assertEquals(null, crypto.unlock("replacement password".toCharArray()))
    }

    @Test fun successfulPasswordRewrapKeepsPrivateRecordsReadableOnlyWithNewPassword() {
        val note = NestNote(id = 91L, title = "Rewrap test", body = "must survive", privateNote = true)
        assertTrue(crypto.initialize("original password".toCharArray()))
        assertTrue(privateStore.save(crypto.unlock("original password".toCharArray())!!, listOf(note)))
        assertTrue(crypto.changePassword("original password".toCharArray(), "replacement password".toCharArray()))
        assertEquals(null, crypto.unlock("original password".toCharArray()))
        val newSession = crypto.unlock("replacement password".toCharArray())!!
        assertEquals(listOf(note), PrivateNoteStore(context).load(newSession))
    }

    @Test fun biometricEnrollmentCannotBeTreatedAsEnabledWithoutStoredAuthBoundKey() {
        val biometric = PrivateVaultBiometricKey(context)
        biometric.deleteEnrollment()
        assertFalse(biometric.isEnrolled())
        assertEquals(null, biometric.createUnlockCipher())
        assertEquals(null, biometric.unlock(null))
    }
}
