package com.nestnote.app

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Creates a QR image locally. The payload is encoded as-is, so callers must warn
 * users when the QR contains readable note text rather than an encrypted link.
 */
internal object NoteQrCode {
    /** Builds a readable share payload without exposing internal voice attachment IDs. */
    fun sharePayload(title: String, body: String): String =
        NoteText.shareText(title, body).replace(Regex("""\[\[voice:[^\]\n]+\]\]"""), "[Voice note]")

    fun createBitmap(payload: String, size: Int = 768): Bitmap {
        require(payload.isNotBlank()) { "QR payload must not be blank" }
        require(size > 0) { "QR image size must be positive" }

        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val matrix = QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
