package com.nestnote.app

/**
 * Stable item keys for the settings LazyColumn, in display order.
 *
 * Keep these keys stable: settings deep-links and programmatic scrolling depend on them.
 */
internal val SettingsItemKeys = listOf(
    "settings_header",
    "settings_appearance",
    "settings_layout",
    "settings_behavior",
    "settings_editor",
    "settings_voice_player_section_v2",
    "settings_privacy",
    "settings_backup",
    "settings_reset"
)

/** Stable sort choices shared by the settings UI. */
internal val SortOptions: List<Pair<String, String>> = listOf(
    "RECENT" to "Edited",
    "CREATED" to "Created",
    "TITLE" to "Title"
)

/** Settings sections searched by the empty-state visibility check. */
internal val SettingsSearchSections = listOf(
    "appearance", "layout", "behavior", "editor", "voice", "privacy", "data"
)

/**
 * Match a settings query against the existing section keywords.
 *
 * Keep this predicate pure so search behavior can be tested without composing the screen.
 */
internal fun matchesSettingsSearch(searchText: String, section: String): Boolean {
    if (searchText.isBlank()) return true
    val query = searchText.trim().lowercase()
    val keywords = when (section) {
        "appearance" -> listOf(
            "appearance", "theme", "device", "light", "dark", "wallpaper",
            "color", "colour", "amoled", "oled", "pure black", "dynamic", "pastel", "adaptive"
        )
        "layout" -> listOf(
            "layout", "dashboard", "grid", "list", "density", "compact", "comfortable", "view"
        )
        "behavior" -> listOf(
            "notes & search", "note behavior", "behavior", "category", "default category",
            "sort", "pin", "pinned", "preview", "date", "reminder", "timestamp"
        )
        "editor" -> listOf(
            "editor", "note editor", "markdown", "writing", "reading", "text size",
            "size", "font", "typeface", "templates", "shortcuts", "word count",
            "bottom sheet", "full-screen", "quick dialog"
        )
        "voice" -> listOf(
            "voice", "player", "voice player", "audio", "playback", "mic", "compact", "full", "waveform"
        )
        "privacy" -> listOf(
            "privacy", "local", "permissions", "cloud", "security", "device", "control", "account"
        )
        "data" -> listOf(
            "data", "backup", "restore", "export", "import", "archive", "archived", "portable"
        )
        else -> return false
    }
    return keywords.any { it.contains(query) || query.contains(it) }
}

