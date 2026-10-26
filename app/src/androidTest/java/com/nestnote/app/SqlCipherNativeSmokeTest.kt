package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.sqlcipher.database.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for the SQLCipher JNI field lookup crash.
 *
 * This intentionally does not write to the application's note database or preferences.
 */
@RunWith(AndroidJUnit4::class)
class SqlCipherNativeSmokeTest {
    @Test
    fun nativeLibraryLoadsAndJniHandleFieldKeepsExpectedDescriptor() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // SqlCipherNoteStore initialization invokes SQLiteDatabase.loadLibs(context).
        // The reported crash occurs during this native initialization path.
        SqlCipherNoteStore(context)

        val handle = SQLiteDatabase::class.java.getDeclaredField("mNativeHandle")
        assertNotNull("SQLCipher JNI handle field must be present", handle)
        assertEquals("SQLCipher JNI expects a Java long (descriptor J)", java.lang.Long.TYPE, handle.type)
    }
}
