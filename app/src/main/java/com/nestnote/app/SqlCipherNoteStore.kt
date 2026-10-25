package com.nestnote.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A SQLCipher-encrypted database in the app's private database directory.
 * The random SQLCipher passphrase is wrapped by a non-exportable Android Keystore key.
 * If key access or authentication fails, this class never opens an unencrypted database.
 */
internal class SqlCipherNoteStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init { SQLiteDatabase.loadLibs(appContext) }

    /**
     * Reports whether either half of this backend's persisted state exists. A database file
     * without its wrapped passphrase (or vice versa) is not a fresh backend: it is an
     * interrupted/corrupt state and must not be silently seeded over.
     */
    @Synchronized
    fun hasStorageArtifacts(): Boolean {
        val file = appContext.getDatabasePath(DATABASE_NAME)
        return file.exists() || prefs.contains(WRAPPED_PASSPHRASE)
    }

    @Synchronized
    fun hasStoredNotes(): Boolean {
        val file = appContext.getDatabasePath(DATABASE_NAME)
        if (!file.exists() || file.length() == 0L || !prefs.contains(WRAPPED_PASSPHRASE)) return false
        return runCatching {
            val encoded = prefs.getString(WRAPPED_PASSPHRASE, null) ?: return false
            val passphrase = unwrapPassphrase(encoded)
            val helper = openHelper()
            try {
                helper.getReadableDatabase(passphrase).rawQuery(
                    "SELECT 1 FROM notes_payload WHERE id = 1 LIMIT 1", null
                ).use { it.moveToFirst() }
            } finally {
                helper.close()
            }
        }.getOrDefault(false)
    }

    @Synchronized
    fun loadSafely(): NoteLoadResult {
        return try {
            val encoded = prefs.getString(WRAPPED_PASSPHRASE, null)
                ?: return NoteLoadResult(emptyList(), true)
            val passphrase = unwrapPassphrase(encoded)
            val helper = openHelper()
            val database = helper.getReadableDatabase(passphrase)
            try {
                database.rawQuery("SELECT payload FROM notes_payload WHERE id = 1", null).use { cursor ->
                    if (!cursor.moveToFirst()) NoteLoadResult(emptyList(), true)
                    else {
                        val notes = NoteStore.decode(cursor.getString(0))
                        if (notes == null) NoteLoadResult(emptyList(), true)
                        else NoteLoadResult(notes, false)
                    }
                }
            } finally {
                database.close()
                helper.close()
            }
        } catch (_: Throwable) {
            NoteLoadResult(emptyList(), true)
        }
    }

    @Synchronized
    fun save(notes: List<NestNote>): Boolean {
        if (!NoteStore.isValidNoteCollection(notes)) return false
        return try {
            val passphrase = getOrCreatePassphrase()
            val helper = openHelper()
            val database = helper.getWritableDatabase(passphrase)
            try {
                database.beginTransaction()
                database.execSQL(
                    "INSERT OR REPLACE INTO notes_payload (id, payload) VALUES (1, ?)",
                    arrayOf(NoteStore.encode(notes))
                )
                database.setTransactionSuccessful()
                true
            } finally {
                if (database.inTransaction()) database.endTransaction()
                database.close()
                helper.close()
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun openHelper() = object : SQLiteOpenHelper(
        appContext, DATABASE_NAME, null, DATABASE_VERSION
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE notes_payload (id INTEGER PRIMARY KEY NOT NULL, payload TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Future schema upgrades must preserve encrypted payload data.
            if (oldVersion < 1) onCreate(db)
        }
    }

    private fun getOrCreatePassphrase(): String {
        prefs.getString(WRAPPED_PASSPHRASE, null)?.let { return unwrapPassphrase(it) }
        val raw = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val wrapped = wrapPassphrase(raw)
        check(prefs.edit().putString(WRAPPED_PASSPHRASE, wrapped).commit()) {
            "Could not persist the wrapped SQLCipher key"
        }
        return Base64.encodeToString(raw, Base64.NO_WRAP).also { raw.fill(0) }
    }

    private fun wrapPassphrase(raw: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(raw)
        return Base64.encodeToString(
            ByteBuffer.allocate(cipher.iv.size + encrypted.size).put(cipher.iv).put(encrypted).array(),
            Base64.NO_WRAP
        )
    }

    private fun unwrapPassphrase(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size >= IV_BYTES + TAG_BYTES) { "Wrapped SQLCipher key is truncated" }
        val buffer = ByteBuffer.wrap(payload)
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getExistingWrappingKey(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(AAD)
        val raw = cipher.doFinal(ciphertext)
        return try {
            Base64.encodeToString(raw, Base64.NO_WRAP)
        } finally {
            raw.fill(0)
        }
    }

    private fun getExistingWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("SQLCipher key wrapper is missing")
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val DATABASE_NAME = "nestnote_encrypted.db"
        private const val DATABASE_VERSION = 1
        private const val PREFS_NAME = "nestnote_local"
        private const val WRAPPED_PASSPHRASE = "sqlcipher_wrapped_passphrase_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nestnote_sqlcipher_key_wrapper_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val TAG_BITS = 128
        private val AAD = "com.nestnote.app|sqlcipher-passphrase|v1".toByteArray(Charsets.UTF_8)
    }
}
