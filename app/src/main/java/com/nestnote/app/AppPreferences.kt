package com.nestnote.app

// Preference keys, enums and shared lists.

internal val categories = listOf("Work", "Personal", "Ideas", "Health", "Finance", "Education", "Travel", "Other")
internal const val PREFS = "nestnote_preferences"
internal const val PREF_THEME = "theme_mode"
internal const val PREF_DYNAMIC = "dynamic_colors"
internal const val PREF_AMOLED = "amoled_black"
internal const val PREF_LAYOUT = "dashboard_layout"
internal const val PREF_EDITOR = "editor_mode"
internal const val PREF_DENSITY = "dashboard_density"
internal const val PREF_NOTE_STYLE = "note_color_style"
internal const val PREF_DEFAULT_SORT = "default_sort"
internal const val PREF_SAVED_SEARCHES = "saved_searches"
internal const val PREF_NOTE_PREVIEWS = "show_note_previews"
internal const val PREF_NOTE_TIMESTAMPS = "show_note_timestamps"
internal const val PREF_DEFAULT_CATEGORY = "default_note_category"
internal const val PREF_CUSTOM_CATEGORIES = "custom_note_categories"
internal const val PREF_TEXT_SIZE = "editor_text_size"
internal const val PREF_TEXT_SIZE_SP = "editor_text_size_sp"
internal const val PREF_EDITOR_FONT = "editor_font_family"
internal const val PREF_EDITOR_FONT_FILE = "editor_font_file"
internal const val PREF_DEFAULT_NOTE_FORMAT = "default_note_format"
internal const val PREF_EDITOR_COUNTS = "editor_show_counts"
internal const val PREF_VOICE_PLAYER_COMPACT = "voice_player_compact"
internal const val PREF_VOICE_PLAYER_SPEED = "voice_player_speed"
internal val VoicePlayerSpeeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

// Backup keys. The string values match what NestNoteApp already writes, so existing
// user preferences keep working. The worker reads these same names — sharing them
// here is what prevents the UI and the worker from drifting apart again.
internal const val PREF_BACKUP_AUTOMATIC_ENABLED = "backup_automatic_enabled"
internal const val PREF_BACKUP_AUTOMATIC_LAST_RUN = "backup_automatic_last_run"
internal const val PREF_BACKUP_AUTOMATIC_STATUS = "backup_automatic_status"
internal const val PREF_BACKUP_AUTOMATIC_LAST_COUNT = "backup_automatic_last_count"
internal const val PREF_BACKUP_LAST_SUCCESS_AT = "backup_last_success_at"
internal const val PREF_BACKUP_LAST_STATUS = "backup_last_status"

internal enum class ThemeMode(val label: String) {
    SYSTEM("Device"), LIGHT("Light"), DARK("Dark")
}
internal enum class EditorTextSize(val label: String, val titleSp: Int, val bodySp: Int) {
    SMALL("Small", 24, 14), MEDIUM("Medium", 27, 16), LARGE("Large", 31, 19)
}
internal enum class EditorFontFamily(val label: String) {
    SANS("Sans"), SERIF("Serif"), MONOSPACE("Monospace"), CURSIVE("Cursive"), CUSTOM("Imported")
}
internal enum class DashboardLayout(val label: String) {
    GRID("Grid"), LIST("List")
}
internal enum class DashboardDensity(val label: String, val description: String) {
    COMPACT("Compact", "Tighter cards to see more notes"),
    COMFORTABLE("Comfortable", "More spacing and breathing room")
}
internal enum class NoteColorStyle(val label: String, val description: String) {
    PASTEL("Soft pastel cards", "Gentle note colors like the design reference"),
    ADAPTIVE("Match wallpaper", "Use your current Material You theme colors")
}
internal enum class EditorPresentationMode(val label: String, val description: String) {
    SHEET("Expandable bottom sheet", "A compact editor that expands as you write"),
    FULL_SCREEN("Full-screen editor", "A distraction-free writing space"),
    QUICK_DIALOG("Quick dialog", "A small dialog for short notes")
}