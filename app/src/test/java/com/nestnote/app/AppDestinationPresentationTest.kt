package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationPresentationTest {
    @Test
    fun titleUsesSpecificLabelsForKnownDestinations() {
        assertEquals("Private Vault", AppDestinationPresentation.title("Private"))
        assertEquals("Private Vault settings", AppDestinationPresentation.title("PrivateVaultSettings"))
        assertEquals("Pinned notes", AppDestinationPresentation.title("Pinned"))
        assertEquals("Archived notes", AppDestinationPresentation.title("Archived"))
        assertEquals("NestNote", AppDestinationPresentation.title("Unknown"))
    }

    @Test
    fun subtitleUsesSpecificLabelsAndFallbackForDestinations() {
        assertEquals("Hidden notes · unlocked", AppDestinationPresentation.subtitle("Private"))
        assertEquals("Authentication and privacy controls", AppDestinationPresentation.subtitle("PrivateVaultSettings"))
        assertEquals("Make your notes feel like yours", AppDestinationPresentation.subtitle("Settings"))
        assertEquals("Your thoughts, organized.", AppDestinationPresentation.subtitle("Pinned"))
        assertEquals("Your thoughts, organized.", AppDestinationPresentation.subtitle("Unknown"))
    }
}
