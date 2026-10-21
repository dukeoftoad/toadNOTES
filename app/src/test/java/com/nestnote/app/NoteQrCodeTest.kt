package com.nestnote.app

import android.graphics.Color
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.WriterException
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteQrCodeTest {
    @Test
    fun createsSquareQrBitmapWithBlackAndWhiteModules() {
        val bitmap = NoteQrCode.createBitmap("NESTNOTE QR test", size = 128)

        assertEquals(128, bitmap.width)
        assertEquals(128, bitmap.height)

        var blackPixels = 0
        var whitePixels = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                when (bitmap.getPixel(x, y)) {
                    Color.BLACK -> blackPixels++
                    Color.WHITE -> whitePixels++
                }
            }
        }
        assertTrue(blackPixels > 0)
        assertTrue(whitePixels > 0)
    }

    @Test
    fun generatedQrCanBeDecodedBackToExactPayload() {
        val payload = "NESTNOTE QR round-trip: title\n\nA note with punctuation: !? & symbols."
        val bitmap = NoteQrCode.createBitmap(payload, size = 512)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))

        assertEquals(payload, decoded.text)
    }

    @Test(expected = WriterException::class)
    fun rejectsPayloadThatExceedsQrCapacity() {
        NoteQrCode.createBitmap("x".repeat(5000))
    }

    @Test
    fun generatedQrRoundTripsUnicodeAndEmojiPayload() {
        val payload = "नमस्ते NESTNOTE 🌿\nNotes stay on your device."
        val bitmap = NoteQrCode.createBitmap(payload, size = 512)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))

        assertEquals(payload, decoded.text)
    }

    @Test
    fun sharePayloadSanitizesEveryVoiceMarkerAndNeverIncludesAttachmentIds() {
        val firstId = "12345678-1234-4234-8234-123456789abc"
        val secondId = "abcdefab-cdef-4abc-8def-abcdefabcdef"
        val payload = NoteQrCode.sharePayload(
            "Voice notes",
            "First [[voice:$firstId]] then [[voice:$secondId]]"
        )

        assertEquals("Voice notes\n\nFirst [Voice note] then [Voice note]", payload)
        assertTrue(!payload.contains(firstId))
        assertTrue(!payload.contains(secondId))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveBitmapSize() {
        NoteQrCode.createBitmap("valid payload", size = 0)
    }

    @Test
    fun sharePayloadReplacesVoiceAttachmentIdsWithReadableLabel() {
        val id = "12345678-1234-4234-8234-123456789abc"

        assertEquals(
            "Title\n\nBefore [Voice note] after",
            NoteQrCode.sharePayload("Title", "Before [[voice:$id]] after")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankPayload() {
        NoteQrCode.createBitmap("  ")
    }
}
