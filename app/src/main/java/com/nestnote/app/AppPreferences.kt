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
internal const val PREF_MARKDOWN_ZOOM = "markdown_viewer_zoom_percent"
internal const val PREF_EDITOR_COUNTS = "editor_show_counts"
internal const val PREF_VOICE_PLAYER_COMPACT = "voice_player_compact"
internal const val PREF_VOICE_PLAYER_SPEED = "voice_player_speed"
internal const val PREF_VOICE_PLAYER_STYLE = "voice_player_style"
internal const val PREF_VOICE_PLAYER_SIZE_PERCENT = "voice_player_size_percent"
internal const val PREF_VOICE_NOTIFICATION_PERMISSION_ASKED = "voice_notification_permission_asked"
internal val VoicePlayerSpeeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

internal enum class VoicePlayerStyle(val label: String, val description: String) {
    COMPACT("Compact", "Small pill with play control and short waveform"),
    FULL("Full", "Expanded card with title, waveform and seek bar"),
    WAVEFORM("Waveform", "Prominent waveform with recording name and duration"),
    MINIMAL("Minimal", "Small play control and duration with no waveform"),
    TRANSPORT("Transport", "Dedicated rewind, play/pause, forward and seek controls")
}

internal const val DEFAULT_VOICE_PLAYER_SIZE_PERCENT = 100
internal const val MIN_VOICE_PLAYER_SIZE_PERCENT = 75
internal const val MAX_VOICE_PLAYER_SIZE_PERCENT = 150

// Encrypted archive status keys.
internal const val PREF_BACKUP_LAST_SUCCESS_AT = "backup_last_success_at"
internal const val PREF_BACKUP_LAST_STATUS = "backup_last_status"
internal const val PREF_PRIVATE_VAULT_SALT = "private_vault_password_salt"
internal const val PREF_PRIVATE_VAULT_HASH = "private_vault_password_hash"
internal const val PREF_PRIVATE_VAULT_BIOMETRIC_ENABLED = "private_vault_biometric_enabled"

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
