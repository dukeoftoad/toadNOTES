package com.nestnote.app

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Parsing of the real GitHub releases/latest payload shape with org.json (provided by
 * Robolectric on the JVM — the same implementation Android ships). Ported from
 * toadFILES (UpdateCheckerParsingTest), adapted to this repo's single-APK release
 * convention (`NESTNOTE-<tag>.apk`, produced by android-release.yml). Runs against a
 * realistic raw string, no network involved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateCheckerParsingTest {

    private val payload = """
        {
          "url": "https://api.github.com/repos/dukeoftoad/toadNOTES/releases/18122461",
          "html_url": "https://github.com/dukeoftoad/toadNOTES/releases/tag/v1.4.10",
          "tag_name": "v1.4.10",
          "target_commitish": "main",
          "name": "NESTNOTE v1.4.10",
          "body": "### Added\r\n- In-app update check (ported from toadFILES)\r\n- Settings polish\r\n",
          "draft": false,
          "prerelease": false,
          "created_at": "2026-03-01T10:00:00Z",
          "published_at": "2026-03-01T10:12:00Z",
          "assets": [
            {
              "name": "NESTNOTE-v1.4.10-sources.zip",
              "content_type": "application/zip",
              "browser_download_url": "https://github.com/dukeoftoad/toadNOTES/releases/download/v1.4.10/NESTNOTE-v1.4.10-sources.zip",
              "size": 204800
            },
            {
              "name": "NESTNOTE-v1.4.10.apk",
              "content_type": "application/vnd.android.package-archive",
              "browser_download_url": "https://github.com/dukeoftoad/toadNOTES/releases/download/v1.4.10/NESTNOTE-v1.4.10.apk",
              "size": 7340032
            },
            {
              "name": "NESTNOTE-v1.4.10-sha256.txt",
              "content_type": "text/plain",
              "browser_download_url": "https://github.com/dukeoftoad/toadNOTES/releases/download/v1.4.10/NESTNOTE-v1.4.10-sha256.txt",
              "size": 142
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parseRelease picks the first apk asset and the release fields`() {
        val release = GithubReleaseChecker.parseRelease(payload)

        assertEquals("v1.4.10", release.tagName)
        assertEquals("https://github.com/dukeoftoad/toadNOTES/releases/tag/v1.4.10", release.htmlUrl)
        // The zip asset must be skipped in favor of the .apk one.
        assertEquals("NESTNOTE-v1.4.10.apk", release.apkName)
        assertEquals(
            "https://github.com/dukeoftoad/toadNOTES/releases/download/v1.4.10/NESTNOTE-v1.4.10.apk",
            release.apkUrl,
        )
        assertEquals(7340032L, release.apkSizeBytes)
        // Notes keep the body text (CR-LF normalized to LF), markdown rendered as plain text.
        assertTrue(release.notes!!.startsWith("### Added"))
        assertTrue(release.notes!!.contains("ported from toadFILES"))
        assertTrue(!release.notes!!.contains("\r"))
    }

    @Test
    fun `parseRelease tolerates a release with no apk asset`() {
        val noApk = """
            {
              "tag_name": "v1.5.0",
              "html_url": "https://github.com/dukeoftoad/toadNOTES/releases/tag/v1.5.0",
              "body": "Sources only.",
              "assets": [
                { "name": "NESTNOTE-v1.5.0-sources.zip", "browser_download_url": "https://x/s.zip", "size": 1 }
              ]
            }
        """.trimIndent()

        val release = GithubReleaseChecker.parseRelease(noApk)

        assertEquals("v1.5.0", release.tagName)
        assertNull(release.apkName)
        assertNull(release.apkUrl)
        assertEquals(0L, release.apkSizeBytes)
    }

    @Test
    fun `parseRelease treats a missing assets array and empty body honestly`() {
        val minimal = """{"tag_name": "v1.6.0"}"""

        val release = GithubReleaseChecker.parseRelease(minimal)

        assertEquals("v1.6.0", release.tagName)
        assertNull(release.apkUrl)
        assertNull(release.notes)
        // html_url falls back to the repo releases page so "View on GitHub" always works.
        assertEquals("https://github.com/dukeoftoad/toadNOTES/releases", release.htmlUrl)
    }

    @Test
    fun `parseRelease refuses a payload without tag_name`() {
        try {
            GithubReleaseChecker.parseRelease("""{"html_url": "https://github.com/x"}""")
            fail("expected JSONException for a payload without tag_name")
        } catch (_: JSONException) {
            // expected — the ViewModel surfaces this as an honest Error state
        }
    }
}
