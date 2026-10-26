package com.nestnote.app

/**
 * Pure presentation labels for app destinations.
 *
 * Keeps destination naming consistent without moving route state or navigation ownership out of
 * NestNoteApp.
 */
internal object AppDestinationPresentation {
    fun title(destination: String): String = when (destination) {
        "Private" -> "Private Vault"
        "PrivateVaultSettings" -> "Private Vault settings"
        "Settings" -> "Settings"
        "Backup" -> "Encrypted archives"
        "About" -> "About & help"
        "Templates" -> "Note templates"
        "Insights" -> "Note insights"
        "Shortcuts" -> "Keyboard shortcuts"
        "More" -> "More"
        "Pinned" -> "Pinned notes"
        "Archived" -> "Archived notes"
        "Categories" -> "Categories"
        else -> "NestNote"
    }

    fun subtitle(destination: String): String = when (destination) {
        "Private" -> "Hidden notes · unlocked"
        "PrivateVaultSettings" -> "Authentication and privacy controls"
        "Settings" -> "Make your notes feel like yours"
        "Backup" -> "Protect and manage your notes"
        "About" -> "Version, licence and how NESTNOTE works"
        "Templates" -> "A faster start for your next note"
        "Insights" -> "Private patterns from your own notes"
        "Shortcuts" -> "Hardware keyboard controls"
        "More" -> "More ways to organize your notes"
        else -> "Your thoughts, organized."
    }
}
