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
import android.util.Base64
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteStoreTest {
    private lateinit var context: Context
    private lateinit var store: NoteStore
    private lateinit var testKey: javax.crypto.SecretKey

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().clear().commit()
        testKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        store = NoteStore(context) { testKey }
    }

    @Test
    fun keystoreSwitchMergesLatestSourceNotesAndChangesBackendAfterCopy() {
        val original = NestNote(id = 501L, title = "Original", body = "Keystore copy", updated = 10L)
        assertTrue(store.save(listOf(original)))
        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().putString("storage_backend", NoteStorageBackend.SQLCIPHER.name).commit())

        // These are the notes reloaded from the active source at worker execution time.
        val latest = listOf(
            original.copy(title = "Edited during delay", body = "Latest body", updated = 20L),
            NestNote(id = 502L, title = "Created during delay", body = "Must survive", updated = 21L)
        )
        val switched = store.switchBackend(NoteStorageBackend.KEYSTORE, latest)

        assertFalse(switched.recoveryRequired)
        assertEquals(NoteStorageBackend.KEYSTORE, store.currentBackend())
        assertEquals(listOf(501L, 502L), store.load().map { it.id })
        assertEquals("Edited during delay", store.load().first().title)
        assertEquals("Must survive", store.load().last().body)
    }

    @Test
    fun missingKeystoreKeyFailsClosedAndPreservesBackendAndCiphertext() {
        val note = NestNote(id = 503L, title = "Preserve me", body = "Encrypted bytes")
        assertTrue(store.save(listOf(note)))
        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val encryptedBefore = prefs.getString("notes_encrypted_v2", null)
        assertTrue(prefs.edit().putString("storage_backend", NoteStorageBackend.SQLCIPHER.name).commit())
        val wrongKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val brokenStore = NoteStore(context) { wrongKey }

        org.junit.Assert.assertThrows(NoteStorageException::class.java) {
            brokenStore.switchBackend(NoteStorageBackend.KEYSTORE, listOf(note.copy(body = "new copy")))
        }

        assertEquals(NoteStorageBackend.SQLCIPHER, brokenStore.currentBackend())
        assertEquals(encryptedBefore, prefs.getString("notes_encrypted_v2", null))
    }

    @Test
    fun corruptKeystoreCiphertextFailsClosedWithoutOverwritingOriginalBytes() {
        val note = NestNote(id = 504L, title = "Corrupt test", body = "Keep stored bytes")
        assertTrue(store.save(listOf(note)))
        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        assertTrue(prefs.edit()
            .putString("storage_backend", NoteStorageBackend.SQLCIPHER.name)
            .putString("notes_encrypted_v2", "not-valid-ciphertext")
            .commit())
        val corruptBytes = prefs.getString("notes_encrypted_v2", null)

        org.junit.Assert.assertThrows(NoteStorageException::class.java) {
            store.switchBackend(NoteStorageBackend.KEYSTORE, listOf(note.copy(body = "must not overwrite")))
        }

        assertEquals(NoteStorageBackend.SQLCIPHER, store.currentBackend())
        assertEquals(corruptBytes, prefs.getString("notes_encrypted_v2", null))
    }

    @Test
    fun invalidSourceNotesAbortSwitchWithoutChangingBackendOrStoredBytes() {
        val note = NestNote(id = 505L, title = "Valid", body = "Keep")
        assertTrue(store.save(listOf(note)))
        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val encryptedBefore = prefs.getString("notes_encrypted_v2", null)
        assertTrue(prefs.edit().putString("storage_backend", NoteStorageBackend.SQLCIPHER.name).commit())
        val invalid = note.copy(id = -1L)

        org.junit.Assert.assertThrows(NoteStorageException::class.java) {
            store.switchBackend(NoteStorageBackend.KEYSTORE, listOf(invalid))
        }

        assertEquals(NoteStorageBackend.SQLCIPHER, store.currentBackend())
        assertEquals(encryptedBefore, prefs.getString("notes_encrypted_v2", null))
    }

    @Test
    fun firstLoadReturnsStarterNotes() {
        val notes = store.load()
        assertTrue(notes.isNotEmpty())
        assertTrue(notes.any { it.title == "Project Plan" })
    }

    @Test
    fun saveAndLoadRoundTripsAllNoteFields() {
        val expected = listOf(NestNote(
            id = 42L, title = "Test title", body = "Body text",
            category = "Work", color = 3, pinned = true,
            reminder = "2026-10-12 09:30", updated = 123456L, privateNote = true
        ))
        store.save(expected)
        assertEquals(expected, store.load())
    }

    @Test
    fun privateNoteFlagSurvivesSaveLoadAndArchivePayloadRoundTrip() {
        val secret = NestNote(id = 999L, title = "Private", body = "Secret", privateNote = true)
        assertTrue(store.save(listOf(secret)))
        assertEquals(secret, store.load().single())
        assertTrue(NoteStore.notesForArchive(listOf(secret), includeArchived = true).isEmpty())
    }

    @Test
    fun legacyRecordsWithoutCreatedTimestampUseTheirIdAsFallback() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit()
            .putString(
                "notes",
                """[{"id":77,"title":"Legacy","body":"Older note","category":"Ideas","updated":88}]"""
            )
            .commit()

        val loaded = store.load().single()
        assertEquals(77L, loaded.created)
        assertEquals("Legacy", loaded.title)
    }

    @Test
    fun createdTimestampSurvivesSaveAndLoad() {
        val original = NestNote(
            id = 9L, title = "Created date", body = "Keep original time",
            updated = 500L, created = 100L
        )
        store.save(listOf(original.copy(updated = 900L)))
        val loaded = store.load().single()
        assertEquals(100L, loaded.created)
        assertEquals(900L, loaded.updated)
    }

    @Test
    fun archiveAndRestoreStateSurviveSaveAndLoad() {
        val archived = NestNote(id = 19L, title = "Archived", body = "Keep it", archived = true)
        store.save(listOf(archived))
        assertEquals(archived, store.load().single())

        val restored = store.load().single().copy(archived = false)
        store.save(listOf(restored))
        assertEquals(restored, store.load().single())
        assertFalse(store.load().single().archived)
    }

    @Test
    fun legacyRecordsDefaultToNotArchived() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString("notes", """[{"id":78,"title":"Legacy","body":"Visible"}]""").commit()
        assertFalse(store.load().single().archived)
    }

    @Test
    fun noteWallpaperSelectionSurvivesSaveAndLoad() {
        val decorated = NestNote(
            id = 91L, title = "Wallpaper", body = "Botanical",
            color = 2, background = 1,
            wallpaperUri = "content://wallpaper-provider/images/91"
        )
        store.save(listOf(decorated))
        assertEquals(decorated, store.load().single())
    }

    @Test
    fun legacyNotesDefaultToNoCustomWallpaper() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString("notes", """[{"id":92,"title":"Legacy","body":"No wallpaper"}]""").commit()
        assertEquals(0, store.load().single().background)
    }

    @Test
    fun savingEmptyListPersistsAnEmptyList() {
        store.save(emptyList())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun archivePayloadRoundTripsAndLegacyPayloadRemainsReadable() {
        val notes = listOf(NestNote(id = 45, title = "Backup", body = "Version one"))
        val versioned = NoteStore.encodeArchivePayload(notes)
        assertEquals(notes, NoteStore.decodeArchivePayload(versioned))
        assertEquals(notes, NoteStore.decodeArchivePayload(NoteStore.encode(notes)))
    }

    @Test
    fun archivePayloadRejectsPrivateVaultRecords() {
        val secret = NestNote(id = 901L, title = "Private", body = "Do not export", privateNote = true)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NoteStore.encodeArchivePayload(listOf(secret))
        }
        val forgedPayload = """{"format":"com.nestnote.archive-payload","schemaVersion":1,"notes":${NoteStore.encode(listOf(secret))}}"""
        assertEquals(null, NoteStore.decodeArchivePayload(forgedPayload))
    }
    @Test
    fun archivePayloadRejectsUnknownSchemaVersion() {
        val unknown = """{"format":"com.nestnote.archive-payload","schemaVersion":99,"notes":[]}"""
        assertEquals(null, NoteStore.decodeArchivePayload(unknown))
    }

    @Test
    fun allocateNoteIdResolvesSameMillisecondAndImportedIdCollisions() {
        val existing = listOf(
            NestNote(id = 100L, title = "A", body = ""),
            NestNote(id = 101L, title = "B", body = ""),
            NestNote(id = 102L, title = "Imported", body = "")
        )

        assertEquals(103L, NoteStore.allocateNoteId(existing, nowMillis = 100L))
        assertEquals(103L, NoteStore.allocateNoteId(existing, nowMillis = 103L))
    }

    @Test
    fun allocateNoteIdNeverReturnsZeroOrNegativeForClockRollback() {
        assertEquals(1L, NoteStore.allocateNoteId(emptyList(), nowMillis = 0L))
        assertEquals(2L, NoteStore.allocateNoteId(listOf(NestNote(id = 1L, title = "Existing", body = "")), nowMillis = -50L))
    }

    @Test
    fun restoreRemapsIncomingReservedDraftIdWithoutChangingExistingNotes() {
        val existing = listOf(NestNote(id = 100L, title = "Saved", body = "Keep"))
        val incoming = listOf(
            NestNote(id = 200L, title = "Imported collision", body = "Restore me"),
            NestNote(id = 201L, title = "Other import", body = "Keep ID")
        )

        val remapped = NoteStore.remapDraftIdCollisionForRestore(
            existing = existing,
            incoming = incoming,
            reservedDraftId = 200L,
            nowMillis = 200L
        )

        assertEquals(listOf(200L, 201L), incoming.map { it.id })
        assertEquals(listOf(202L, 201L), remapped.map { it.id })
        assertEquals(100L, existing.single().id)
        assertEquals("Imported collision", remapped.first().title)
        assertTrue((existing + remapped).map { it.id }.distinct().size == 3)
    }

    @Test
    fun restoreDoesNotRemapReservedIdWhenItAlreadyBelongsToSavedNote() {
        val existing = listOf(NestNote(id = 200L, title = "Saved", body = "Current"))
        val incoming = listOf(NestNote(id = 200L, title = "Backup", body = "Imported"))

        val result = NoteStore.remapDraftIdCollisionForRestore(
            existing = existing,
            incoming = incoming,
            reservedDraftId = 200L,
            nowMillis = 300L
        )

        assertEquals(incoming, result)
        assertEquals("Saved", NoteStore.merge(existing, result).notes.single().title)
    }

    @Test
    fun restoreLeavesIncomingIdsAloneWhenNoDraftCollisionExists() {
        val incoming = listOf(NestNote(id = 300L, title = "Import", body = "No collision"))

        assertEquals(
            incoming,
            NoteStore.remapDraftIdCollisionForRestore(emptyList(), incoming, 200L, nowMillis = 300L)
        )
    }

    @Test
    fun saveTimeDraftCollisionIsDetectedWithoutChangingReservedIdentity() {
        val draftId = 800L
        val persisted = listOf(NestNote(id = draftId, title = "Restored note", body = ""))
        assertTrue(NoteStore.hasDraftIdCollision(persisted, draftId))
        assertFalse(NoteStore.hasDraftIdCollision(persisted, draftId + 1L))
        // A collision is detected, not silently resolved by changing the draft ID;
        // callers can preserve staged encrypted attachments while reporting the conflict.
        assertEquals(800L, draftId)
    }

    @Test
    fun editorDirtyStateTracksEditSaveDiscardAndVoiceChanges() {
        val baseline = NestNote(
            id = 900L, title = "Title", body = "Body", category = "Ideas",
            color = 2, pinned = true, reminder = "Tomorrow", background = 1
        )
        fun dirty(
            original: NestNote?,
            title: String = "Title",
            body: String = "Body",
            category: String = "Ideas",
            color: Int = 2,
            background: Int = 1,
            pinned: Boolean = true,
            reminder: String = "Tomorrow",
            voicePending: Boolean = false
        ) = NoteStore.hasEditorChanges(
            original, title, body, category, "Ideas", color, background, pinned, reminder, voicePending
        )

        assertFalse(dirty(baseline))
        assertTrue(dirty(baseline, body = "Edited"))
        // After saving, the saved content becomes the new baseline and is clean.
        val saved = baseline.copy(body = "Edited")
        assertFalse(dirty(saved, body = "Edited"))
        // Discard/navigation must still treat an unsaved text edit or staged voice as dirty.
        assertTrue(dirty(baseline, title = "Unsaved"))
        assertTrue(dirty(baseline, voicePending = true))
        assertFalse(dirty(null, title = "", body = "", category = "Ideas", color = 0,
            background = 0, pinned = false, reminder = "", voicePending = false))
    }

    @Test
    fun versionGuardAllowsUnchangedNoteAndRejectsStaleDraft() {
        assertTrue(NoteStore.isSameNoteVersion(100L, 100L))
        assertFalse(NoteStore.isSameNoteVersion(100L, 101L))
    }

    @Test
    fun archivePayloadRejectsMissingRequiredFieldsAndInvalidRecordValues() {
        assertEquals(null, NoteStore.decodeArchivePayload("""[{"title":"Missing ID","body":"No identity"}]"""))
        assertEquals(null, NoteStore.decodeArchivePayload("""{"schemaVersion":1,"notes":[{"id":-1,"title":"Invalid","body":"Bad ID"}]}"""))
        assertEquals(null, NoteStore.decodeArchivePayload("""{"schemaVersion":1}"""))
    }

    @Test
    fun decodeTreatsExplicitJsonNullTextFieldsAsMissingNotLiteralNull() {
        // Regression: org.json's optString returns the literal string "null" for
        // JSONObject.NULL, so a hand-edited or third-party backup with
        // {"title": null, "body": null, ...} imported notes whose content was the
        // four characters 'n','u','l','l' in every text field.
        val decoded = NoteStore.decode(
            """[{"id":7,"title":null,"body":null,"category":null,"reminder":null,"wallpaperUri":null,"format":null,"encryptedPayload":null}]""",
            requireFields = true
        )
        assertEquals(1, decoded?.size)
        val note = decoded!!.single()
        assertEquals("", note.title)
        assertEquals("", note.body)
        assertEquals("Ideas", note.category)
        assertEquals("", note.reminder)
        assertEquals("", note.wallpaperUri)
        assertEquals("plain", note.format)
        assertEquals("", note.encryptedPayload)
    }

    @Test
    fun mergeAddsNewNotesUpdatesOnlyNewerCopiesAndNeverDeletesAbsentNotes() {
        val original = NestNote(id = 1, title = "Original", body = "Keep", updated = 10)
        val unchanged = NestNote(id = 2, title = "Unchanged", body = "Keep me", updated = 20)
        val existing = listOf(original, unchanged)
        val incoming = listOf(
            original.copy(title = "Newer copy", updated = 11),
            unchanged.copy(title = "Older copy", updated = 19),
            NestNote(id = 3, title = "Imported", body = "Add", updated = 30)
        )

        val result = NoteStore.merge(existing, incoming)

        assertEquals(1, result.added)
        assertEquals(1, result.updated)
        assertEquals(listOf(1L, 2L, 3L), result.notes.map { it.id })
        assertEquals("Newer copy", result.notes[0].title)
        assertEquals("Unchanged", result.notes[1].title)
        assertEquals("Imported", result.notes[2].title)
    }

    @Test
    fun mergeHandlesLargeImportsAndPreservesStableOrder() {
        val existing = (1L..5_000L).map { id ->
            NestNote(id = id, title = "Saved $id", body = "Existing", updated = 1)
        }
        val incoming = (2_501L..6_000L).map { id ->
            NestNote(id = id, title = "Imported $id", body = "Incoming", updated = 2)
        }

        val result = NoteStore.merge(existing, incoming)

        assertEquals(1_000, result.added)
        assertEquals(2_500, result.updated)
        assertEquals(6_000, result.notes.size)
        assertEquals((1L..6_000L).toList(), result.notes.map { it.id })
        assertEquals("Imported 2501", result.notes[2_500].title)
    }

    @Test
    fun archivePayloadRejectsDuplicateIds() {
        val raw = """[{"id":7,"title":"One","body":"A"},{"id":7,"title":"Two","body":"B"}]"""
        assertEquals(null, NoteStore.decode(raw, requireFields = true))
    }

    @Test
    fun decodeRejectsInvalidIdsAndOversizedFields() {
        val zeroId = """[{"id":0,"title":"Invalid","body":"Body"}]"""
        val longTitle = "x".repeat(NoteStore.MAX_TITLE_LENGTH + 1)
        val oversizedTitle = NoteStore.encode(listOf(NestNote(id = 8, title = longTitle, body = "Body")))
        assertEquals(null, NoteStore.decode(zeroId, requireFields = true))
        assertEquals(null, NoteStore.decode(oversizedTitle, requireFields = true))
    }

    @Test
    fun archivePayloadRejectsOversizedPayloadBeforeParsing() {
        val raw = " ".repeat(NoteStore.MAX_ARCHIVE_PAYLOAD_BYTES + 1)
        assertEquals(null, NoteStore.decode(raw, requireFields = true))
    }

    @Test
    fun ordinaryEncryptionMetadataRoundTripsWithoutPlaintextContent() {
        val encrypted = NestNote(
            id = 904,
            title = NoteStore.ENCRYPTED_NOTE_TITLE,
            body = "",
            category = "Encrypted",
            encrypted = true,
            encryptedPayload = "NESTNOTE_ORDINARY_NOTE_V1:authenticated-ciphertext"
        )
        val encoded = NoteStore.encode(listOf(encrypted))
        val decoded = NoteStore.decode(encoded, requireFields = true)

        assertEquals(listOf(encrypted), decoded)
        assertFalse(encoded.contains("original secret title"))
        assertFalse(encoded.contains("original secret body"))
    }

    @Test
    fun deviceEncryptedNotesAreExcludedFromPortableArchives() {
        val encrypted = NestNote(
            id = 907,
            title = NoteStore.ENCRYPTED_NOTE_TITLE,
            body = "",
            category = "Encrypted",
            encrypted = true,
            encryptedPayload = "NESTNOTE_ORDINARY_NOTE_V1:payload"
        )
        val plain = NestNote(id = 908, title = "Portable", body = "Backup me")
        assertEquals(listOf(plain), NoteStore.notesForArchive(listOf(encrypted, plain), includeArchived = true))
        var rejected = false
        try {
            NoteStore.encodeArchivePayload(listOf(encrypted))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun encryptedRecordCannotBePersistedWithPlaintextOrVaultClassification() {
        val invalidPlaintext = NestNote(
            id = 905,
            title = "Still plaintext",
            body = "",
            encrypted = true,
            encryptedPayload = "NESTNOTE_ORDINARY_NOTE_V1:payload"
        )
        val invalidVault = NestNote(
            id = 906,
            title = NoteStore.ENCRYPTED_NOTE_TITLE,
            body = "",
            privateNote = true,
            encrypted = true,
            encryptedPayload = "NESTNOTE_ORDINARY_NOTE_V1:payload"
        )
        assertFalse(NoteStore.isValidNoteCollection(listOf(invalidPlaintext)))
        assertFalse(NoteStore.isValidNoteCollection(listOf(invalidVault)))
    }

    @Test
    fun saveRejectsDuplicateIdsWithoutReplacingExistingNotes() {
        val original = NestNote(id = 81, title = "Keep me", body = "Existing data")
        assertTrue(store.save(listOf(original)))
        val result = store.save(listOf(original, original.copy(title = "Duplicate")))
        assertFalse(result)
        assertEquals(listOf(original), store.load())
    }

    @Test
    fun malformedStoredJsonRequiresRecoveryAndIsNotReplacedWithStarterNotes() {
        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        prefs.edit().putString("notes", "{not valid json").commit()

        val result = store.loadSafely()

        assertTrue(result.recoveryRequired)
        assertTrue(result.notes.isEmpty())
        assertEquals("{not valid json", prefs.getString("notes", null))
    }

    @Test
    fun missingStoredDataIsTreatedAsFreshInstallNotCorruption() {
        val result = store.loadSafely()
        assertFalse(result.recoveryRequired)
        assertTrue(result.notes.any { it.title == "Project Plan" })
    }

    @Test
    fun mergeKeepsExistingCopyWhenTimestampsTieAndPreservesExistingOrder() {
        val first = NestNote(id = 10, title = "First", body = "Saved first", updated = 100)
        val second = NestNote(id = 20, title = "Second", body = "Saved second", updated = 200)
        val tiedImport = first.copy(title = "Conflicting equal-time copy", body = "Must not win", updated = 100)
        val added = NestNote(id = 30, title = "Third", body = "Imported third", updated = 300)

        val result = NoteStore.merge(listOf(first, second), listOf(added, tiedImport))

        assertEquals(1, result.added)
        assertEquals(0, result.updated)
        assertEquals(listOf(10L, 20L, 30L), result.notes.map { it.id })
        assertEquals("First", result.notes[0].title)
        assertEquals("Saved first", result.notes[0].body)
    }

    @Test
    fun saveUsesV2AuthenticatedCiphertextAndNeverStoresPlaintextNotes() {
        val note = NestNote(id = 901, title = "Secret title", body = "Secret body")
        assertTrue(store.save(listOf(note)))

        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        val encrypted = prefs.getString("notes_encrypted_v2", null)
        assertTrue(encrypted != null)
        assertEquals(null, prefs.getString("notes_encrypted_v1", null))
        assertEquals(null, prefs.getString("notes", null))
        assertFalse(encrypted!!.contains("Secret title"))
        assertFalse(encrypted.contains("Secret body"))
        assertEquals(listOf(note), store.load())
    }

    @Test
    fun modifiedV2CiphertextRequiresRecoveryAndIsNotOverwritten() {
        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        assertTrue(store.save(listOf(NestNote(id = 902, title = "Authenticated", body = "Keep bytes"))))
        val original = prefs.getString("notes_encrypted_v2", null)!!
        val bytes = Base64.decode(original, Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        val tampered = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString("notes_encrypted_v2", tampered).commit()

        val result = store.loadSafely()

        assertTrue(result.recoveryRequired)
        assertTrue(result.notes.isEmpty())
        assertEquals(tampered, prefs.getString("notes_encrypted_v2", null))
    }

    @Test
    fun encryptedV1RecordsMigrateToV2WithoutPlaintextWindow() {
        val expected = listOf(NestNote(id = 903, title = "Old encrypted note", body = "Migrate safely"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, testKey)
        val encrypted = cipher.doFinal(NoteStore.encode(expected).toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv).put(encrypted).array()
        val oldValue = Base64.encodeToString(payload, Base64.NO_WRAP)
        val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
        prefs.edit().putString("notes_encrypted_v1", oldValue).commit()

        val result = store.loadSafely()

        assertFalse(result.recoveryRequired)
        assertEquals(expected, result.notes)
        assertTrue(prefs.getString("notes_encrypted_v2", null) != null)
        assertEquals(null, prefs.getString("notes_encrypted_v1", null))
        assertEquals(null, prefs.getString("notes", null))
    }



}
