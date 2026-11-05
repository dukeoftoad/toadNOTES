package com.nestnote.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reads the installed version at runtime (BuildConfig is not enabled in this project,
 * so PackageManager is the single honest source), falling back to [AboutInfo]'s
 * packaged defaults when the lookup fails.
 */
internal fun readAppVersion(context: Context): AboutInfo.Version {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        AboutInfo.Version(packageInfo.versionName ?: AboutInfo.FALLBACK_VERSION_NAME, code, true)
    } catch (_: Exception) {
        AboutInfo.fallbackVersion()
    }
}

/** Opens an external link; returns false when nothing on the device can handle it. */
internal fun openExternalLink(context: Context, url: String): Boolean {
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun launchLink(context: Context, url: String) {
    if (!openExternalLink(context, url)) {
        Toast.makeText(context, "No app found to open links", Toast.LENGTH_SHORT).show()
    }
}

private fun deviceModelLine(): String =
    "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()

/**
 * The About & help tab, reached from More > About & help.
 * Shows version, licence and F-Droid status, plus help facts about storage,
 * backups and current limitations.
 */
@Composable
internal fun AboutScreen(version: AboutInfo.Version) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var showVersionDetails by remember { mutableStateOf(false) }
    val versionLabel = AboutInfo.versionLabel(version)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "NESTNOTE",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your thoughts, organized.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.12f),
                    contentColor = colors.primary,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        versionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    "Local-first · encrypted · no account",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }

        MoreGroup("About NESTNOTE") {
            AboutInfoRow(
                Icons.Default.Lock,
                "Private by design",
                "Notes are stored encrypted on this device using AES-GCM with Android Keystore. " +
                    "There is no account, no cloud sync and no analytics.",
                false
            )
            AboutInfoRow(
                Icons.Default.Info,
                "What NESTNOTE does",
                "Notes, checklists and categories with markdown preview, voice-note attachments, " +
                    "reminder metadata and password-protected encrypted note archives — all on your device.",
                true
            )
        }

        MoreGroup("Under the hood") {
            AboutInfoRow(
                Icons.Default.Lock,
                "Encrypted note storage",
                "Note text is serialized locally and protected with AES-256-GCM. Encryption keys are generated and kept by Android Keystore; each save uses a fresh nonce and authenticated data.",
                false
            )
            AboutInfoRow(
                Icons.Default.Shield,
                "Voice attachments",
                "Audio is encrypted as it is written to app-private files. Attachment IDs are authenticated with the audio, so moving a file to another note will not decrypt it.",
                true
            )
            AboutInfoRow(
                Icons.Default.Description,
                "Encrypted archive format",
                "Portable .nestnote archives are protected with a password and AES-GCM. Private Vault notes and voice attachments are excluded.",
                true
            )
            AboutInfoRow(
                Icons.Default.Info,
                "Data portability",
                "Only password-protected encrypted archives are supported for note transfer. Voice recordings are excluded.",
                true
            )
        }

        MoreGroup("Links") {
            MoreRow(
                Icons.Default.Code,
                "Source code",
                "github.com/dukeoftoad/toadNOTES",
                false
            ) {
                launchLink(context, AboutInfo.REPO_URL)
            }
            MoreRow(
                Icons.Default.Download,
                "Releases",
                "Signed APKs from versioned GitHub releases",
                true
            ) {
                launchLink(context, AboutInfo.RELEASES_URL)
            }
            MoreRow(
                Icons.Default.BugReport,
                "Report an issue",
                "Tell us what broke or what to improve",
                true
            ) {
                launchLink(context, AboutInfo.ISSUES_URL)
            }
        }

        MoreGroup("Licence & credits") {
            MoreRow(
                Icons.Default.Gavel,
                "Licence",
                "GPL-3.0-only · read the full licence text",
                false
            ) {
                launchLink(context, AboutInfo.LICENCE_URL)
            }
            AboutInfoRow(
                Icons.Default.Description,
                "Third-party components",
                "Markdown preview uses Markwon (Apache-2.0). Mermaid diagrams render " +
                    "locally with mermaid.js (MIT) — no network access. " +
                    "The Fraunces display font is used under the SIL Open Font License.",
                true
            )
            AboutInfoRow(
                Icons.Default.Shop,
                "F-Droid",
                "Not yet listed on the official F-Droid index. " +
                    "GitHub Releases is the current install source; a repository listing is in preparation.",
                true
            )
        }

        MoreGroup("Good to know") {
            AboutInfoRow(
                Icons.Default.Shield,
                "Recovery first",
                "If stored notes can't be decrypted, NESTNOTE keeps the data untouched and shows a " +
                    "recovery path instead of replacing your notes.",
                false
            )
            AboutInfoRow(
                Icons.Default.Description,
                "Encrypted archives",
                "Create or restore a password-protected .nestnote archive from More > Backup & restore. " +
                    "Keep the archive password separately; forgotten passwords cannot be recovered.",
                true
            )
            AboutInfoRow(
                Icons.Default.NotificationsNone,
                "Reminders",
                "NESTNOTE schedules reminder alarms on-device. Android 13 and newer require notification permission; Android may delay inexact alarms when exact-alarm access is unavailable.",
                true
            )
        }

        MoreGroup("App info") {
            MoreRow(
                Icons.Default.Info,
                "Version details",
                "$versionLabel · tap for device info",
                false
            ) {
                showVersionDetails = true
            }
        }

        Text(
            "Built with Kotlin, Jetpack Compose and Material 3.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 4.dp)
        )
    }

    if (showVersionDetails) {
        AlertDialog(
            onDismissRequest = { showVersionDetails = false },
            title = { Text("About NESTNOTE") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        AboutInfo.versionLabel(version),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Android API ${Build.VERSION.SDK_INT}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        deviceModelLine(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    if (!version.fromPackage) {
                        Text(
                            "Installed version info is unavailable; showing packaged defaults.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        AboutInfo.licenceSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val details = AboutInfo.versionDetails(
                            version,
                            Build.VERSION.SDK_INT,
                            deviceModelLine()
                        )
                        clipboard.setText(AnnotatedString(details))
                    }
                ) {
                    Text("Copy details")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVersionDetails = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/** Non-clickable information row: icon tile, title and explanatory body text. */
@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    title: String,
    body: String,
    divider: Boolean
) {
    val colors = MaterialTheme.colorScheme

    if (divider) {
        HorizontalDivider(color = colors.outlineVariant)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = colors.primary.copy(alpha = 0.12f),
            contentColor = colors.primary
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(9.dp)
                    .size(22.dp)
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
