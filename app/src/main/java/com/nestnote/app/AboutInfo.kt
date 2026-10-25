package com.nestnote.app

/**
 * Static "About & help" content, kept as a pure object so JVM unit tests can pin
 * the exact strings the app shows and links to. No Android framework types here.
 */
internal object AboutInfo {

    // Matches the build.gradle.kts defaults; used when PackageManager info is unavailable.
    const val FALLBACK_VERSION_NAME = "1.4.6"
    const val FALLBACK_VERSION_CODE = 10406L

    const val REPO_URL = "https://github.com/dukeoftoad/toadNOTES"
    const val RELEASES_URL = "https://github.com/dukeoftoad/toadNOTES/releases"
    const val ISSUES_URL = "https://github.com/dukeoftoad/toadNOTES/issues"
    const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.txt"
    const val LICENCE_NAME = "GPL-3.0-only"

    /**
     * [fromPackage] is false when the value came from the fallback constants
     * (for example in a JVM preview), so the UI can label it honestly.
     */
    data class Version(val name: String, val code: Long, val fromPackage: Boolean)

    fun fallbackVersion(): Version = Version(FALLBACK_VERSION_NAME, FALLBACK_VERSION_CODE, false)

    /** Compact one-line label, e.g. "Version 1.2.9 (3)". */
    fun versionLabel(version: Version): String = "Version ${version.name} (${version.code})"

    /** Multi-line copy-friendly summary used by the version-details dialog. */
    fun versionDetails(version: Version, apiLevel: Int, deviceModel: String): String =
        "NESTNOTE ${versionLabel(version)}\n" +
            "Android API $apiLevel\n" +
            deviceModel

    /** Honest one-line licence summary shown on the About screen. */
    fun licenceSummary(): String =
        "Distributed under the GNU General Public License v3.0 ($LICENCE_NAME)."
}
