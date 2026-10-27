package com.nestnote.app

private val noteLinkPattern = Regex("""\[\[([^\]\r\n]{1,160})\]\]""")

// Voice attachments are anchored as [[voice:<uuid>]] and are not note links. Without
// this guard the marker's inner text leaked into the "Missing notes" diagnostic and
// (hypothetically) into backlink matching. The id accepts both the dashed canonical
// UUID and the dash-less 32-hex form written by older imports.
private val voiceMarkerLabelPattern = Regex("""(?i)voice:[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}""")

internal fun isVoiceMarkerLabel(label: String): Boolean = voiceMarkerLabelPattern.matches(label.trim())

internal fun noteLinkTitles(body: String): List<String> = noteLinkPattern.findAll(body)
    .map { it.groupValues[1].trim() }
    .filter { it.isNotBlank() }
    .filterNot(::isVoiceMarkerLabel)
    .distinctBy { it.lowercase() }
    .toList()

internal data class ResolvedNoteLink(val label: String, val note: NestNote)

internal fun resolveNoteLinks(body: String, notes: List<NestNote>, excludingId: Long? = null): List<ResolvedNoteLink> =
    resolveNoteLinks(body, notesByTitle(notes, excludingId))

/**
 * Title index over [notes]. Building it is O(N) with thousands of string allocations, so
 * callers that resolve links on every keystroke should cache the map (keyed on the note
 * list) and reuse [resolveNoteLinks] below with only the body changing.
 */
internal fun notesByTitle(notes: List<NestNote>, excludingId: Long? = null): Map<String, NestNote> =
    notes.filter { it.id != excludingId }.associateBy { it.title.trim().lowercase() }

internal fun resolveNoteLinks(body: String, byTitle: Map<String, NestNote>): List<ResolvedNoteLink> =
    noteLinkPattern.findAll(body).mapNotNull { match ->
        val label = match.groupValues[1].trim()
        if (isVoiceMarkerLabel(label)) return@mapNotNull null
        val target = byTitle[label.lowercase()] ?: return@mapNotNull null
        ResolvedNoteLink(label, target)
    }.distinctBy { it.note.id }.toList()

internal fun notesLinkingTo(title: String, notes: List<NestNote>, excludingId: Long? = null): List<NestNote> {
    val key = title.trim().lowercase()
    if (key.isBlank()) return emptyList()
    return notes.filter { note ->
        note.id != excludingId && noteLinkPattern.findAll(note.body)
            .filterNot { isVoiceMarkerLabel(it.groupValues[1]) }
            .any { it.groupValues[1].trim().lowercase() == key }
    }
}

internal fun wrapAsNoteLink(text: String, start: Int, end: Int): Triple<String, Int, Int> {
    val safeStart = start.coerceIn(0, text.length)
    val safeEnd = end.coerceIn(safeStart, text.length)
    val selection = text.substring(safeStart, safeEnd)
    val result = text.replaceRange(safeStart, safeEnd, "[[${selection}]]")
    val innerStart = safeStart + 2
    return Triple(result, innerStart, innerStart + selection.length)
}
