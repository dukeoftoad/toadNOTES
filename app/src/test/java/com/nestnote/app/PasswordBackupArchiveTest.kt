package com.nestnote.app

import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PasswordBackupArchiveTest {
    private fun validArchivePayload(): String = NoteStore.encodeArchivePayload(
        listOf(NestNote(id = 123, title = "Archive test", body = "private payload"))
    )

    @Test
    fun passwordArchiveRoundTripsAndRejectsWrongPassword() {
        val json = validArchivePayload()
        val archive = PasswordBackupArchive.encryptArchive(json, "correct horse battery".toCharArray())

        assertNotNull(PasswordBackupArchive.decryptArchive(archive, "correct horse battery".toCharArray()))
        assertNull(PasswordBackupArchive.decryptArchive(archive, "wrong password!".toCharArray()))
    }

    @Test
    fun tamperedArchiveAndShortPasswordAreRejected() {
        val archive = PasswordBackupArchive.encryptArchive(validArchivePayload(), "long enough password".toCharArray())
        archive[archive.lastIndex] = (archive.last().toInt() xor 0x01).toByte()

        assertNull(PasswordBackupArchive.decryptArchive(archive, "long enough password".toCharArray()))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            PasswordBackupArchive.encryptArchive(validArchivePayload(), "short".toCharArray())
        }
    }

    @Test
    fun randomBytesCannotBeImportedAsPasswordArchive() {
        assertNull(PasswordBackupArchive.decryptArchive(ByteArray(80) { it.toByte() }, "password long enough".toCharArray()))
    }
}
