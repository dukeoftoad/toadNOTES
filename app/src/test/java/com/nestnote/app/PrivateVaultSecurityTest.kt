package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
    fun passwordSetupRejectsShortPasswords() {
        assertFalse(PrivateVaultSecurity.configure(context, "12345"))
        assertFalse(PrivateVaultSecurity.isConfigured(context))
    }
}
