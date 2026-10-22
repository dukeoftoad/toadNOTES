import com.nestnote.app.*
import java.io.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.content.ContextWrapper

var failures = 0; var passes = 0
fun check(name: String, ok: Boolean, detail: String = "") {
    if (ok) passes++ else { failures++; println("FAIL $name $detail") }
}
fun tmpRoot(name: String): File = File("/tmp/nn_test/${name}_${System.nanoTime()}").apply { mkdirs() }
fun fakeContext(dir: File) = object : ContextWrapper(null) {
    override fun getFilesDir(): File = dir
}
val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

fun main() {
    // ---------- NoteText ----------
    check("unwrap whole **bold**", NoteText.toggleInlineFormat("**bold**", 0, 8, "**").first == "bold",
        NoteText.toggleInlineFormat("**bold**", 0, 8, "**").first)
    val unwrapSel = NoteText.toggleInlineFormat("**bold**", 0, 8, "**")
    check("unwrap selection range", unwrapSel.second == 0 && unwrapSel.third == 4, "${unwrapSel.second},${unwrapSel.third}")
    check("wrap plain word bold", NoteText.toggleInlineFormat("a b", 2, 3, "**").first == "a **b**")
    check("wrap then toggle back", NoteText.toggleInlineFormat("a **b** c", 4, 5, "**").first == "a b c")
    check("italic inside bold keeps bold", NoteText.toggleInlineFormat("**bold**", 2, 6, "_").first == "**_bold_**",
        NoteText.toggleInlineFormat("**bold**", 2, 6, "_").first)
    check("collapsed bold", NoteText.toggleInlineFormat("ab", 1, 1, "**").first == "a****b")
    val link = NoteText.insertMarkdownLink("hello", 0, 5)
    check("link text", link.first == "[hello](https://)", link.first)
    check("link selects URL", link.second == 8 && link.third == 16, "${link.second},${link.third}")
    val multi = NoteText.insertMarkdownLink("a\nb", 0, 3).first
    check("link multiline label", multi == "[a b](https://)", multi)
    check("link escapes brackets", NoteText.insertMarkdownLink("a]b", 0, 3).first == "[a\\]b](https://)")

    // ---------- DraftStore ----------
    val dir = tmpRoot("draft")
    val store = DraftStore(fakeContext(dir))
    val d = DraftStore.Draft(7, "T", "line1\nline2 é", "Ideas", 1, 0, true, "2026-01-01 09:00", "", 123L, "markdown")
    check("draft write", store.write(d))
    val r = store.read(7)
    check("draft roundtrip", r == d, "$r")
    check("draft tmp removed", File(dir, "drafts").listFiles()?.none { it.name.endsWith(".tmp") } ?: true)
    // Old-format draft (no format key) must decode as plain.
    val old = File(dir, "drafts/note-8.json")
    old.writeText("#\nschemaVersion=1\nnoteId=8\ntitle=x\nbody=y\ncategory=Ideas\ncolor=0\nbackground=0\npinned=false\nreminder=\nwallpaperUri=\nsavedAtMillis=5\n")
    check("legacy draft decodes", store.read(8)?.format == "plain", "${store.read(8)}")
    // Oversized draft is refused on write, not silently written then unreadable.
    val big = d.copy(noteId = 9, body = "x".repeat(9 * 1024 * 1024))
    check("oversize refused", !store.write(big))
    check("clear", store.clear(7) && store.read(7) == null)
    check("corrupt draft yields null", DraftStore(fakeContext(dir)).also {
        File(dir, "drafts/note-10.json").writeText("schemaVersion=99\nnoteId=10\n")
    }.read(10) == null)

    // ---------- VoiceAttachmentStore ----------
    val vroot = tmpRoot("voice")
    val vs = VoiceAttachmentStore(fakeContext(vroot), { key }, File(vroot, "v"))
    val audio = ByteArray(50_000) { (it * 31).toByte() }
    val id = "11111111-2222-3333-4444-555555555555"
    vs.save(1, id, ByteArrayInputStream(audio))
    check("voice roundtrip", vs.read(1, id).contentEquals(audio))
    // AAD binding: copying the file under another note must not decrypt.
    val src = File(vroot, "v/1/$id.nva"); File(vroot, "v/2").mkdirs()
    src.copyTo(File(vroot, "v/2/$id.nva"))
    var threw = false
    try { vs.read(2, id) } catch (e: IOException) { threw = true }
    check("moved file fails auth", threw)
    // copyAll re-encrypts under the new note.
    val ids = vs.copyAll(1, 3)
    check("copyAll ids", ids == listOf(id), "$ids")
    check("copyAll readable", vs.read(3, id).contentEquals(audio))
    check("copyAll source intact", vs.read(1, id).contentEquals(audio))
    // Rollback: destination already has the attachment -> copyAll throws and leaves note 4 empty.
    vs.save(4, id, ByteArrayInputStream(audio))
    var copyFailed = false
    try { vs.copyAll(1, 4) } catch (e: IOException) { copyFailed = true }
    check("copyAll conflict throws", copyFailed)
    check("copyAll leaves dest intact", vs.read(4, id).contentEquals(audio))
    // read/delete must not create directories for unknown notes.
    try { vs.read(99, id) } catch (e: IOException) {}
    check("read does not mkdir", !File(vroot, "v/99").exists())
    check("delete unknown ok", vs.delete(98, id))
    check("delete not mkdir", !File(vroot, "v/98").exists())
    // Stale temp removed on construction.
    File(vroot, "v").mkdirs()
    val stale = File(vroot, "v/.1_x.tmp"); stale.writeText("junk")
    VoiceAttachmentStore(fakeContext(vroot), { key }, File(vroot, "v"))
    check("stale temp cleaned", !stale.exists())
    // Shared lock: concurrent saves from separate instances all succeed and all readable.
    val threads = (1..20).map { i -> Thread {
        VoiceAttachmentStore(fakeContext(vroot), { key }, File(vroot, "v")).save(50, "a$i", ByteArrayInputStream(audio))
    } }
    threads.forEach { it.start() }; threads.forEach { it.join() }
    check("concurrent saves", VoiceAttachmentStore(fakeContext(vroot), { key }, File(vroot, "v")).list(50).size == 20)

    println("PASSED $passes  FAILED $failures")
}
