package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the static About & help content: the strings users see and the URLs the
 * app opens must not drift silently. Pure JVM — no Android framework types.
 */
class AboutInfoTest {

    @Test
    fun `version label matches the documented format`() {
        assertEquals(
            "Version 1.2.8 (3)",
            AboutInfo.versionLabel(AboutInfo.Version("1.2.8", 3L, true))
        )
    }

    @Test
    fun `fallback version matches the gradle defaults`() {
        val fallback = AboutInfo.fallbackVersion()
        assertEquals("1.4.6", fallback.name)
        assertEquals(10406L, fallback.code)
        assertFalse(fallback.fromPackage)
        assertEquals("Version 1.4.6 (10406)", AboutInfo.versionLabel(fallback))
    }

    @Test
    fun `all About links are https`() {
        listOf(
            AboutInfo.REPO_URL,
            AboutInfo.RELEASES_URL,
            AboutInfo.ISSUES_URL,
            AboutInfo.LICENCE_URL
        ).forEach { url ->
            assertTrue("Link must be https: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `repository links point at the NESTNOTE GitHub project`() {
        assertEquals("https://github.com/dukeoftoad/toadNOTES", AboutInfo.REPO_URL)
        assertTrue(AboutInfo.RELEASES_URL.startsWith(AboutInfo.REPO_URL))
        assertTrue(AboutInfo.ISSUES_URL.startsWith(AboutInfo.REPO_URL))
    }

    // Backtick JVM names must not contain dots, so "GPL-3.0-only" becomes "GPL-3-0-only" here.
    @Test
    fun `licence summary names GPL-3-0-only`() {
        val summary = AboutInfo.licenceSummary()
        assertTrue(summary.contains("GPL-3.0-only"))
        assertTrue(summary.contains("GNU General Public License"))
    }

    @Test
    fun `version details include version, api level and device`() {
        val details = AboutInfo.versionDetails(AboutInfo.Version("1.2.8", 3L, true), 36, "Google Pixel 8")
        assertTrue(details.contains("NESTNOTE Version 1.2.8 (3)"))
        assertTrue(details.contains("Android API 36"))
        assertTrue(details.contains("Google Pixel 8"))
        assertEquals(3, details.lineSequence().count())
    }

    @Test
    fun `version label survives odd version names`() {
        assertEquals("Version dev-abc (42)", AboutInfo.versionLabel(AboutInfo.Version("dev-abc", 42L, true)))
    }
}
