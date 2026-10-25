package com.nestnote.app

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateNoteStoreTest {
    private lateinit var context: Context
    private lateinit var crypto: PrivateNoteCrypto
    private lateinit var store: PrivateNoteStore
    private lateinit var session: PrivateNoteCrypto.Session

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nestnote_private_vault_crypto", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE).edit().clear().commit()
        crypto = PrivateNoteCrypto(context)
        assertTrue(crypto.initialize("vault password".toCharArray()))
        session = crypto.unlock("vault password".toCharArray())!!
        store = PrivateNoteStore(context)
    }

    @Test fun privateRecordsRoundTripAndDiskDoesNotContainPlaintext() {
        val note = NestNote(id = 1001L, title = "Private title marker", body = "Private body marker", privateNote = true)
        assertTrue(store.save(session, listOf(note)))
        val disk = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
            .getString("encrypted_records", "")!!
        assertFalse(disk.contains(note.title))
        assertFalse(disk.contains(note.body))
        assertEquals(listOf(note), store.load(session))
    }

    @Test fun publicNotesCannotBeWrittenIntoPrivateStore() {
        assertFalse(store.save(session, listOf(NestNote(id = 1002L, title = "public", body = "body"))))
    }

    @Test fun wrongSessionCannotReadPrivateRecords() {
        val note = NestNote(id = 1003L, title = "Private", body = "secret", privateNote = true)
        assertTrue(store.save(session, listOf(note)))
        // Construct an independent session key. A second crypto instance shares the same
        // persisted vault metadata and must not initialize/replace the existing vault key.
        val wrong = PrivateNoteCrypto.Session(
            SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
        )
        assertFalse(runCatching { store.load(wrong) }.isSuccess)
    }
    @Test fun tamperedPrivateRecordCiphertextFailsClosed() {
        val note = NestNote(id = 1004L, title = "Private tamper", body = "must not load", privateNote = true)
        assertTrue(store.save(session, listOf(note)))
        val prefs = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
        val records = JSONArray(prefs.getString("encrypted_records", null))
        val item = records.getJSONObject(0)
        val prefix = "NESTNOTE_PRIVATE_RECORD_V1:"
        val envelope = item.getString("ciphertext")
        val payload = Base64.decode(envelope.removePrefix(prefix), Base64.NO_WRAP)
        payload[payload.lastIndex] = (payload.last().toInt() xor 1).toByte()
        item.put("ciphertext", prefix + Base64.encodeToString(payload, Base64.NO_WRAP))
        assertTrue(prefs.edit().putString("encrypted_records", records.toString()).commit())

        assertFalse(runCatching { store.load(session) }.isSuccess)
    }

    @Test fun recordIdSubstitutionFailsAuthenticatedRead() {
        val note = NestNote(id = 1005L, title = "Bound identity", body = "authenticated id", privateNote = true)
        assertTrue(store.save(session, listOf(note)))
        val prefs = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
        val records = JSONArray(prefs.getString("encrypted_records", null))
        records.getJSONObject(0).put("id", 1006L)
        assertTrue(prefs.edit().putString("encrypted_records", records.toString()).commit())

        assertFalse(runCatching { store.load(session) }.isSuccess)
    }

    @Test fun unsupportedPrivateStoreSchemaFailsClosedWithoutClearingBytes() {
        val note = NestNote(id = 1007L, title = "Preserve ciphertext", body = "do not overwrite", privateNote = true)
        assertTrue(store.save(session, listOf(note)))
        val prefs = context.getSharedPreferences("nestnote_private_note_records", Context.MODE_PRIVATE)
        val originalRecords = prefs.getString("encrypted_records", null)
        assertTrue(prefs.edit().putInt("schema_version", 999).commit())

        assertFalse(runCatching { store.load(session) }.isSuccess)
        assertEquals(originalRecords, prefs.getString("encrypted_records", null))
        assertEquals(999, prefs.getInt("schema_version", -1))
    }

}
