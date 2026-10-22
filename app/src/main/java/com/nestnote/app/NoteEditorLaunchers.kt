package com.nestnote.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Picks, validates and persists access to an imported wallpaper image. */
@Composable
internal fun rememberWallpaperImportLauncher(
    onImported: (uriString: String) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val importResult = runCatching {
                val resolver = context.contentResolver
                // Some providers report no MIME type; the decode check below is the real gate.
                val mimeType = resolver.getType(uri)
                require(mimeType == null || mimeType.startsWith("image/")) {
                    "Please choose an image file"
                }
                // Verify the provider can actually read and decode this image before
                // changing the draft; otherwise the picker appears successful but the
                // wallpaper can disappear after the document provider/app restarts.
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val imageStream = resolver.openInputStream(uri) ?: error("Unable to open selected image")
                imageStream.use { stream ->
                    // With inJustDecodeBounds enabled, decodeStream intentionally returns null;
                    // the dimensions are the validation result, not the decoded bitmap.
                    android.graphics.BitmapFactory.decodeStream(stream, null, bounds)
                }
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected file is not a readable image" }
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            importResult.onSuccess {
                onImported(uri.toString())
                android.widget.Toast.makeText(context, "Wallpaper imported", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                android.widget.Toast.makeText(
                    context,
                    error.message?.takeIf { it.isNotBlank() } ?: "Could not import wallpaper. Please choose another image.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/** Saves the note as a Markdown document chosen by the user. */
@Composable
internal fun rememberMarkdownExportLauncher(
    markdown: () -> String
): ManagedActivityResultLauncher<String, Uri?> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(markdown())
                } ?: error("Unable to open destination")
                android.widget.Toast.makeText(context, "Markdown file saved", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "Could not save Markdown file", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

/** Writes the pending QR payload to a PNG chosen by the user. */
@Composable
internal fun rememberQrImageExportLauncher(
    pendingPayload: () -> String?,
    onFinished: () -> Unit
): ManagedActivityResultLauncher<String, Uri?> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val payload = pendingPayload()
        if (uri != null && !payload.isNullOrBlank()) {
            runCatching {
                val bitmap = NoteQrCode.createBitmap(payload)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        "Could not encode QR image"
                    }
                } ?: error("Unable to open destination")
                android.widget.Toast.makeText(context, "QR image saved", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "Could not save QR image", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        onFinished()
    }
}
