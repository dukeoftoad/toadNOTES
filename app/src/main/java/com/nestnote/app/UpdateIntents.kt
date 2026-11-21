package com.nestnote.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Installer and browser handoff for the self-update flow. Ported from toadFILES'
 * com.toadfiles.app.util.Intents (the install section); every function returns an
 * honest result so callers can tell the user exactly what happened — nothing
 * pretends to succeed.
 *
 * INSTALL gates on canRequestPackageInstalls() (API 26+, which is this app's
 * minSdk) and walks the user to the exact Android settings toggle when it is off
 * (several OEM installers otherwise fail silently or auto-deny).
 * Permission-missing, settings-open-failure and installer-launch-failure are
 * reported as distinct outcomes.
 */
object UpdateIntents {

    /** Outcome of handing an APK to the system installer. */
    enum class InstallResult {
        /** The package installer was launched. */
        STARTED,

        /** "Install unknown apps" was off; the settings toggle was opened — a retry is valid. */
        NEEDS_PERMISSION,

        /** The permission was missing AND the settings screen could not be opened — no retry path. */
        SETTINGS_FAILED,

        /** The handoff itself failed (URI trouble, or a throwing launch after permission was granted). */
        FAILED,
    }

    /**
     * Hands an APK already available as a content URI to the system installer.
     * Used by the self-update flow, whose APK comes out of a completed
     * DownloadManager job as content://downloads/… — such a URI is served to the
     * installer by the download system itself, so no FileProvider round-trip is
     * needed.
     *
     * On Android 8+ (minSdk 26 here) the app must hold the "Install unknown apps"
     * grant for this app (REQUEST_INSTALL_PACKAGES + a user toggle). Many OEM
     * installers fail silently or auto-deny without it, so we check first and walk
     * the user straight to the toggle instead of firing an intent into the void.
     * Returns [InstallResult.STARTED] when the installer was launched,
     * [InstallResult.NEEDS_PERMISSION] when the user was taken to the settings
     * toggle (the pending APK should be kept — a retry is valid),
     * [InstallResult.SETTINGS_FAILED] when even the settings screen could not be
     * opened (no retry path — the pending APK should be dropped),
     * [InstallResult.FAILED] when the handoff itself failed.
     */
    fun installApkFromUri(context: Context, uri: Uri): InstallResult {
        if (!canRequestPackageInstalls(context)) {
            // Distinct outcome: "go flip the toggle in Settings" is only honest if we
            // actually took the user there. If both settings intents fail, say so.
            return if (openInstallPermissionSettings(context)) {
                InstallResult.NEEDS_PERMISSION
            } else {
                InstallResult.SETTINGS_FAILED
            }
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(uri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // The self-update handoff can run from the application context after the
            // download finishes; NEW_TASK keeps that legal, and is a no-op difference
            // when launched from an activity.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent) // no chooser: this must reach the installer
            InstallResult.STARTED
        } catch (_: Exception) {
            // Permission is already granted here; this is a real handoff failure.
            InstallResult.FAILED
        }
    }

    /**
     * Opens an https URL in the user's browser (used by "View on GitHub"). No
     * Custom Tabs dependency — the plain ACTION_VIEW launch resolves whatever
     * browser the user has. User-initiated only; refuses everything that is not
     * https so this helper can never be talked into launching file/content
     * intents or shipping the user to a cleartext http endpoint. Returns an
     * honest false when the URL is not an https URL or nothing can handle it.
     */
    fun openUrl(context: Context, url: String): Boolean {
        val scheme = try {
            Uri.parse(url).scheme?.lowercase()
        } catch (_: Exception) {
            null
        }
        if (scheme != "https") return false
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun canRequestPackageInstalls(context: Context): Boolean = try {
        context.packageManager.canRequestPackageInstalls()
    } catch (_: Exception) {
        false
    }

    /** Opens the "Install unknown apps" screen for this app; false when both attempts fail. */
    private fun openInstallPermissionSettings(context: Context): Boolean = try {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
