package com.nestnote.app

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteStoreMigrationTest {
    private lateinit var context: Context
    private lateinit var key: SecretKey

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().clear().commit()
        key = javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    @Test
    fun legacyPlaintextMigratesBeforePlaintextKeyIsRemoved() {
        val preferences = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val expected = listOf(NestNote(id = 71L, title = "Legacy", body = "Keep this note"))
        preferences.edit().putString("notes", NoteStore.encode(expected)).commit()

        val loaded = NoteStore(context) { key }.loadSafely()

        assertFalse(loaded.recoveryRequired)
        assertEquals(expected, loaded.notes)
        assertNull(preferences.getString("notes", null))
        assertTrue(preferences.getString("notes_encrypted_v2", null).orEmpty().isNotBlank())
        assertNull(preferences.getString("notes_encrypted_v1", null))
        assertEquals(expected, NoteStore(context) { key }.loadSafely().notes)
    }

    @Test
    fun authenticatedV1PayloadMigratesToV2AndRemainsReadable() {
        val preferences = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val expected = listOf(NestNote(id = 72L, title = "V1 note", body = "Migrate once"))
        preferences.edit()
            .putString("notes_encrypted_v1", encryptV1(NoteStore.encode(expected)))
            .commit()

        val loaded = NoteStore(context) { key }.loadSafely()

        assertFalse(loaded.recoveryRequired)
        assertEquals(expected, loaded.notes)
        assertNull(preferences.getString("notes_encrypted_v1", null))
        assertNull(preferences.getString("notes", null))
        assertTrue(preferences.getString("notes_encrypted_v2", null).orEmpty().isNotBlank())
        assertEquals(expected, NoteStore(context) { key }.loadSafely().notes)
    }

    @Test
    fun malformedLegacyPayloadRequiresRecoveryAndPreservesOriginalBytes() {
        val preferences = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val malformed = """[{"id":1,"title":""".trim()
        preferences.edit().putString("notes", malformed).commit()

        val result = NoteStore(context) { key }.loadSafely()

        assertTrue(result.recoveryRequired)
        assertTrue(result.notes.isEmpty())
        assertEquals(malformed, preferences.getString("notes", null))
        assertNull(preferences.getString("notes_encrypted_v2", null))
    }

    @Test
    fun unreadableV1PayloadRequiresRecoveryWithoutDeletingLegacyCiphertext() {
        val preferences = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val invalidPayload = Base64.encodeToString(ByteArray(4) { 7 }, Base64.NO_WRAP)
        preferences.edit().putString("notes_encrypted_v1", invalidPayload).commit()

        val result = NoteStore(context) { key }.loadSafely()

        assertTrue(result.recoveryRequired)
        assertTrue(result.notes.isEmpty())
        assertEquals(invalidPayload, preferences.getString("notes_encrypted_v1", null))
        assertNull(preferences.getString("notes_encrypted_v2", null))
    }

    @Test
    fun v2CiphertextTamperingBlocksLoadAndPreservesStoredPayload() {
        val preferences = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val store = NoteStore(context) { key }
        val expected = listOf(NestNote(id = 73L, title = "Protected", body = "Authenticated"))
        assertTrue(store.save(expected))
        val encrypted = preferences.getString("notes_encrypted_v2", null)!!
        val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        val tampered = Base64.encodeToString(bytes, Base64.NO_WRAP)
        preferences.edit().putString("notes_encrypted_v2", tampered).commit()

        val result = NoteStore(context) { key }.loadSafely()

        assertTrue(result.recoveryRequired)
        assertTrue(result.notes.isEmpty())
        assertEquals(tampered, preferences.getString("notes_encrypted_v2", null))
    }

    private fun encryptV1(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(
            ByteBuffer.allocate(cipher.iv.size + encrypted.size).put(cipher.iv).put(encrypted).array(),
            Base64.NO_WRAP
        )
    }
}
