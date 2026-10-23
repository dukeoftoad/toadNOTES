package com.nestnote.app

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Base64
import java.util.Properties
import java.io.StringWriter
import javax.crypto.Cipher
import java.nio.ByteBuffer
import javax.crypto.spec.SecretKeySpec

class DraftStoreTest {

    private lateinit var root: File
    private lateinit var store: DraftStore

    @Before
    fun setUp() {
        root = Files.createTempDirectory("draftstore-test").toFile()
        store = DraftStore(root)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun sampleDraft(id: Long = 1L) = DraftStore.Draft(
        noteId = id,
        title = "Test title",
        body = "Test body",
        category = "Personal",
        color = 2,
        background = 1,
        pinned = true,
        reminder = "Oct 20, 9:00 AM",
        wallpaperUri = "",
        savedAtMillis = 123L
    )

    @Test
    fun `write then read round trips`() {
        val draft = sampleDraft()
        assertTrue(store.write(draft))
        val read = store.read(draft.noteId)
        assertNotNull(read)
        assertEquals(draft, read)
    }

    @Test
    fun `read missing returns null`() {
        assertNull(store.read(999L))
    }

    @Test
    fun `read negative ID returns null`() {
        assertNull(store.read(-1L))
    }

    @Test
    fun `write negative ID returns false`() {
        assertFalse(store.write(sampleDraft(id = -1L)))
    }

    @Test
    fun `write twice overwrites`() {
        val first = sampleDraft().copy(title = "First")
        val second = sampleDraft().copy(title = "Second")
        assertTrue(store.write(first))
        assertTrue(store.write(second))
        assertEquals("Second", store.read(first.noteId)?.title)
    }

    @Test
    fun `clear removes draft`() {
        val draft = sampleDraft()
        assertTrue(store.write(draft))
        assertTrue(store.clear(draft.noteId))
        assertNull(store.read(draft.noteId))
    }

    @Test
    fun `clear missing returns true`() {
        assertTrue(store.clear(999L))
    }

    @Test
    fun `listAll returns all drafts`() {
        store.write(sampleDraft(id = 1L))
        store.write(sampleDraft(id = 2L))
        store.write(sampleDraft(id = 3L))
        val all = store.listAll()
        assertEquals(3, all.size)
        assertTrue(all.map { it.noteId }.containsAll(listOf(1L, 2L, 3L)))
    }

    @Test
    fun `clearAll removes all drafts`() {
        store.write(sampleDraft(id = 1L))
        store.write(sampleDraft(id = 2L))
        assertEquals(2, store.clearAll())
        assertNull(store.read(1L))
        assertNull(store.read(2L))
    }

    @Test
    fun `corrupted content returns null and does not throw`() {
        val draft = sampleDraft()
        store.write(draft)
        File(root, "note-${draft.noteId}.json").writeText("not a properties file {{{", Charsets.UTF_8)
        assertNull(store.read(draft.noteId))
    }

    @Test
    fun `legacy plaintext draft files are rejected fail closed`() {
        File(root, "note-1.json").writeText(
            "schemaVersion=1\\nnoteId=1\\ntitle=Legacy secret\\nbody=legacy body\\n",
            Charsets.UTF_8
        )
        assertNull(store.read(1L))
        assertFalse(File(root, "note-1.json").exists())
        assertTrue(store.listAll().isEmpty())
    }

    @Test
    fun `oversized file returns null`() {
        val draft = sampleDraft()
        store.write(draft)
        File(root, "note-${draft.noteId}.json").writeText("x".repeat(3 * 1024 * 1024))
        assertNull(store.read(draft.noteId))
    }

    @Test
    fun `drafts remain isolated by note ID across store recreation`() {
        val firstStore = DraftStore(root)
        val noteOne = sampleDraft(id = 101L).copy(
            title = "Note one",
            body = "line one\n\n  indented\n[[voice:attachment-one]]"
        )
        val noteTwo = sampleDraft(id = 202L).copy(
            title = "Note two",
            body = "different content"
        )

        assertTrue(firstStore.write(noteOne))
        assertTrue(firstStore.write(noteTwo))

        // A new store instance models process recreation; each note must recover
        // only its own latest snapshot, including whitespace and attachment markers.
        val recreatedStore = DraftStore(root)
        assertEquals(noteOne, recreatedStore.read(101L))
        assertEquals(noteTwo, recreatedStore.read(202L))

        assertTrue(recreatedStore.clear(101L))
        assertNull(recreatedStore.read(101L))
        assertEquals(noteTwo, recreatedStore.read(202L))
    }

    @Test
    fun `draft timestamp and note ID round trip for stale draft comparison`() {
        val draft = sampleDraft(id = 42L).copy(savedAtMillis = 987654321L)
        assertTrue(store.write(draft))
        val restored = store.read(42L)
        assertEquals(42L, restored?.noteId)
        assertEquals(987654321L, restored?.savedAtMillis)
    }
    @Test
    fun latestDraftSnapshotCanBeFlushedImmediatelyBeforeEditorRemoval() {
        val older = sampleDraft(id = 404L).copy(title = "Earlier", body = "old", savedAtMillis = 1000L)
        val latest = older.copy(title = "Latest at stop", body = "final keystrokes", savedAtMillis = 1001L)
        assertTrue(store.write(older))
        assertTrue(DraftStore(root).write(latest))
        val recovered = DraftStore(root).read(404L)
        assertEquals("Latest at stop", recovered?.title)
        assertEquals("final keystrokes", recovered?.body)
        assertEquals(1001L, recovered?.savedAtMillis)
    }

    private val testKey = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
    private fun encryptedStore(key: SecretKeySpec = testKey) = DraftStore(root) { key }

    @Test
    fun `private draft contents are encrypted at rest and classification round trips`() {
        val draft = sampleDraft(77L).copy(title = "VAULT TITLE SECRET", body = "VAULT BODY SECRET", privateNote = true)
        val secureStore = encryptedStore()
        assertTrue(secureStore.write(draft))
        val disk = File(root, "note-77.json").readText(Charsets.UTF_8)
        assertTrue(disk.startsWith("NESTNOTE_DRAFT_PRIVATE_V2:"))
        assertFalse(disk.contains(draft.title))
        assertFalse(disk.contains(draft.body))
        assertEquals(draft, secureStore.read(77L))
        assertTrue(secureStore.listAll().isEmpty())
        assertEquals(listOf(draft), secureStore.listAll(includePrivate = true))
    }

    @Test
    fun `private draft wrong key and tampering are rejected`() {
        val draft = sampleDraft(78L).copy(title = "Secret", privateNote = true)
        assertTrue(encryptedStore().write(draft))
        assertNull(encryptedStore(SecretKeySpec(ByteArray(32) { (it + 9).toByte() }, "AES")).read(78L))
        val file = File(root, "note-78.json")
        val payload = Base64.getDecoder().decode(file.readText(Charsets.UTF_8).removePrefix("NESTNOTE_DRAFT_PRIVATE_V2:"))
        payload[payload.lastIndex] = (payload.last().toInt() xor 1).toByte()
        file.writeText("NESTNOTE_DRAFT_PRIVATE_V2:" + Base64.getEncoder().withoutPadding().encodeToString(payload), Charsets.UTF_8)
        assertNull(encryptedStore().read(78L))
    }

    @Test
    fun `missing privacy classification is rejected`() {
        val props = Properties().apply {
            setProperty("schemaVersion", "2"); setProperty("noteId", "79")
            setProperty("title", "Unclassified"); setProperty("body", "must not recover")
        }
        val raw = StringWriter().also { props.store(it, null) }.toString()
        val envelope = "NESTNOTE_DRAFT_PUBLIC_V2:" + Base64.getEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))
        File(root, "note-79.json").writeText(envelope, Charsets.UTF_8)
        assertNull(store.read(79L))
    }

    @Test
    fun `private classification inside public envelope is rejected`() {
        val props = Properties().apply {
            setProperty("schemaVersion", "2"); setProperty("noteId", "80")
            setProperty("title", "Secret"); setProperty("body", "private"); setProperty("privateNote", "true")
        }
        val raw = StringWriter().also { props.store(it, null) }.toString()
        val envelope = "NESTNOTE_DRAFT_PUBLIC_V2:" + Base64.getEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))
        File(root, "note-80.json").writeText(envelope, Charsets.UTF_8)
        assertNull(store.read(80L))
        assertTrue(store.listAll().isEmpty())
    }

    @Test
    fun `private draft is excluded from ordinary orphan listing`() {
        val draft = sampleDraft(82L).copy(privateNote = true)
        val secureStore = encryptedStore()
        assertTrue(secureStore.write(draft))
        assertTrue(secureStore.listAll().isEmpty())
        assertEquals(draft, secureStore.listAll(includePrivate = true).single())
    }


    @Test
    fun `public classification inside authenticated private envelope is rejected`() {
        val props = Properties().apply {
            setProperty("schemaVersion", "2"); setProperty("noteId", "83")
            setProperty("title", "Public marked private"); setProperty("body", "must reject")
            setProperty("privateNote", "false")
        }
        val plain = StringWriter().also { props.store(it, null) }.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, testKey)
        cipher.updateAAD("com.nestnote.app|drafts|private|v2".toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plain)
        val payload = ByteBuffer.allocate(cipher.iv.size + encrypted.size).put(cipher.iv).put(encrypted).array()
        val envelope = "NESTNOTE_DRAFT_PRIVATE_V2:" + Base64.getEncoder().withoutPadding().encodeToString(payload)
        File(root, "note-83.json").writeText(envelope, Charsets.UTF_8)
        assertNull(encryptedStore().read(83L))
    }

}
