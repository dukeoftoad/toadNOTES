package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
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
class PrivateNoteCryptoTest {
    private lateinit var context: Context
    private lateinit var crypto: PrivateNoteCrypto

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nestnote_private_vault_crypto", Context.MODE_PRIVATE)
            .edit().clear().commit()
        crypto = PrivateNoteCrypto(context)
    }

    @Test fun passwordWrapCreatesIndependentKeyAndRoundTripsCiphertext() {
        val password = "correct horse".toCharArray()
        assertTrue(crypto.initialize(password))
        val session = crypto.unlock(password)
        assertTrue(session != null)
        val raw = "private title and body".toByteArray()
        val ciphertext = session!!.encrypt(raw, 91L)
        assertFalse(ciphertext.contains("private title"))
        assertArrayEquals(raw, session.decrypt(ciphertext, 91L))
    }

    @Test fun wrongPasswordAndTamperedKeyWrapperFailClosed() {
        assertTrue(crypto.initialize("correct password".toCharArray()))
        assertNull(crypto.unlock("wrong password".toCharArray()))
        val prefs = context.getSharedPreferences("nestnote_private_vault_crypto", Context.MODE_PRIVATE)
        val wrapped = prefs.getString("wrapped_vault_key", null)!!
        prefs.edit().putString("wrapped_vault_key", wrapped.dropLast(2) + "AA").commit()
        assertNull(crypto.unlock("correct password".toCharArray()))
    }

    @Test fun ciphertextTamperingAndRecordSubstitutionFailAuthentication() {
        assertTrue(crypto.initialize("correct password".toCharArray()))
        val session = crypto.unlock("correct password".toCharArray())!!
        val encrypted = session.encrypt("secret".toByteArray(), 1L)
        assertFalse(runCatching { session.decrypt(encrypted, 2L) }.isSuccess)
        val prefix = "NESTNOTE_PRIVATE_RECORD_V1:"
        val payload = android.util.Base64.decode(encrypted.removePrefix(prefix), android.util.Base64.NO_WRAP)
        payload[payload.lastIndex] = (payload.last().toInt() xor 1).toByte()
        val tampered = prefix + android.util.Base64.encodeToString(payload, android.util.Base64.NO_WRAP)
        assertFalse(runCatching { session.decrypt(tampered, 1L) }.isSuccess)
    }

    @Test fun passwordChangeRewrapsSameDataKey() {
        assertTrue(crypto.initialize("old password".toCharArray()))
        val before = crypto.unlock("old password".toCharArray())!!
        val encrypted = before.encrypt("preserve data".toByteArray(), 5L)
        assertTrue(crypto.changePassword("old password".toCharArray(), "new password".toCharArray()))
        assertNull(crypto.unlock("old password".toCharArray()))
        val after = crypto.unlock("new password".toCharArray())!!
        assertArrayEquals("preserve data".toByteArray(), after.decrypt(encrypted, 5L))
    }

    @Test fun initializeDoesNotReplaceExistingVaultKey() {
        assertTrue(crypto.initialize("first password".toCharArray()))
        assertFalse(crypto.initialize("second password".toCharArray()))
        assertTrue(crypto.unlock("first password".toCharArray()) != null)
        assertNull(crypto.unlock("second password".toCharArray()))
    }
}
