package com.nestnote.app

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecordingLifecycleTest {
    @Test
    fun supportsPauseResumeAndStopInOrder() {
        val lifecycle = VoiceRecordingLifecycle()
        assertTrue(lifecycle.start())
        assertTrue(lifecycle.pause())
        assertEquals(VoiceRecordingLifecycle.State.PAUSED, lifecycle.state())
        assertTrue(lifecycle.resume())
        assertEquals(VoiceRecordingLifecycle.State.RECORDING, lifecycle.state())
        assertTrue(lifecycle.stop())
        assertEquals(VoiceRecordingLifecycle.State.STOPPED, lifecycle.state())
    }

    @Test
    fun rejectsRepeatedOrOutOfOrderTransitions() {
        val lifecycle = VoiceRecordingLifecycle()
        assertFalse(lifecycle.pause())
        assertFalse(lifecycle.resume())
        assertTrue(lifecycle.start())
        assertFalse(lifecycle.start())
        assertTrue(lifecycle.stop())
        assertFalse(lifecycle.start())
        assertFalse(lifecycle.resume())
        assertFalse(lifecycle.stop())
    }

    @Test
    fun discardIsTerminalAndCannotBeRevivedByLateWorkerTransitions() {
        val lifecycle = VoiceRecordingLifecycle()
        assertTrue(lifecycle.start())
        assertTrue(lifecycle.discard())
        assertFalse(lifecycle.pause())
        assertFalse(lifecycle.resume())
        assertFalse(lifecycle.stop())
        assertFalse(lifecycle.start())
        assertEquals(VoiceRecordingLifecycle.State.DISCARDED, lifecycle.state())
    }

    @Test
    fun fakeCapturePumpsValidPcmChunksInOrder() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(listOf(byteArrayOf(1, 2), byteArrayOf(3, 4, 5)))
        val result = PcmCapturePump.capture(fake, output, 10, { true }, { false }, {})
        assertEquals(5L, result)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), output.toByteArray())
        assertTrue(fake.released)
    }

    @Test
    fun fakeCaptureWithNoAudioReturnsZeroBytes() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(emptyList())
        assertEquals(0L, PcmCapturePump.capture(fake, output, 10, { true }, { false }, {}))
        assertEquals(0, output.size())
        assertTrue(fake.released)
    }

    @Test
    fun fakeCaptureReadFailurePropagatesAndReleasesCapture() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(emptyList(), readFailure = IOException("simulated microphone failure"))
        var reportedFailure: Exception? = null
        try {
            PcmCapturePump.capture(
                fake, output, 10, { true }, { false }, {},
                onCaptureFailure = { reportedFailure = it }
            )
            throw AssertionError("Expected read failure")
        } catch (expected: IOException) {
            assertEquals("simulated microphone failure", expected.message)
        }
        assertEquals("simulated microphone failure", reportedFailure?.message)
        assertTrue(fake.released)
        assertEquals(0, output.size())
    }

    @Test
    fun fakeCaptureOverflowFailsWithoutWritingTheOverflowingChunk() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)))
        try {
            PcmCapturePump.capture(fake, output, 4, { true }, { false }, {})
            throw AssertionError("Expected size limit failure")
        } catch (_: PcmCaptureSizeLimitException) {
            // The caller must not publish the partial bytes after this failure.
        }
        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertTrue(fake.released)
    }

    @Test
    fun overflowedPartialCaptureIsNeverCommittedAsAClip() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)))
        var committed: ByteArray? = null
        try {
            PcmCapturePump.capture(fake, output, 4, { true }, { false }, {})
            committed = output.toByteArray()
        } catch (_: PcmCaptureSizeLimitException) {
            // Match the worker contract: a capture error prevents publishing partial output.
        }
        assertEquals(null, committed)
        assertTrue(fake.released)
    }

    @Test
    fun repeatedStopIsIdempotentAndNeverRestartsSession() {
        val lifecycle = VoiceRecordingLifecycle()
        assertTrue(lifecycle.start())
        assertTrue(lifecycle.stop())
        assertFalse(lifecycle.stop())
        assertFalse(lifecycle.start())
        assertFalse(lifecycle.resume())
        assertEquals(VoiceRecordingLifecycle.State.STOPPED, lifecycle.state())
    }

    @Test
    fun concurrentStopAndDiscardNeverLeaveSessionActive() {
        repeat(100) {
            val lifecycle = VoiceRecordingLifecycle()
            assertTrue(lifecycle.start())
            val ready = CountDownLatch(2)
            val go = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            try {
                val stop = pool.submit<Boolean> {
                    ready.countDown()
                    go.await()
                    lifecycle.stop()
                }
                val discard = pool.submit<Boolean> {
                    ready.countDown()
                    go.await()
                    lifecycle.discard()
                }
                assertTrue(ready.await(2, TimeUnit.SECONDS))
                go.countDown()
                stop.get(2, TimeUnit.SECONDS)
                discard.get(2, TimeUnit.SECONDS)
                val state = lifecycle.state()
                assertTrue(
                    state == VoiceRecordingLifecycle.State.STOPPED ||
                        state == VoiceRecordingLifecycle.State.DISCARDED
                )
                assertFalse(lifecycle.start())
                assertFalse(lifecycle.resume())
            } finally {
                pool.shutdownNow()
            }
        }
    }

    @Test
    fun pauseResumeRacingWithStopNeverReactivatesSession() {
        repeat(100) {
            val lifecycle = VoiceRecordingLifecycle()
            assertTrue(lifecycle.start())
            val ready = CountDownLatch(3)
            val go = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(3)
            try {
                val pause = pool.submit<Boolean> {
                    ready.countDown()
                    go.await()
                    lifecycle.pause()
                }
                val resume = pool.submit<Boolean> {
                    ready.countDown()
                    go.await()
                    lifecycle.resume()
                }
                val stop = pool.submit<Boolean> {
                    ready.countDown()
                    go.await()
                    lifecycle.stop()
                }
                assertTrue(ready.await(2, TimeUnit.SECONDS))
                go.countDown()
                pause.get(2, TimeUnit.SECONDS)
                resume.get(2, TimeUnit.SECONDS)
                stop.get(2, TimeUnit.SECONDS)
                assertEquals(VoiceRecordingLifecycle.State.STOPPED, lifecycle.state())
                assertFalse(lifecycle.start())
                assertFalse(lifecycle.resume())
            } finally {
                pool.shutdownNow()
            }
        }
    }

    private class FakePcmCapture(
        chunks: List<ByteArray>,
        private val readFailure: IOException? = null
    ) : PcmCapture {
        private val chunks = ArrayDeque(chunks)
        var released = false
        override fun start() = Unit
        override fun pause() = Unit
        override fun resume() = Unit
        override fun read(buffer: ByteArray): Int {
            readFailure?.let { throw it }
            val next = if (chunks.isEmpty()) null else chunks.removeFirst()
            if (next == null) return -1
            next.copyInto(buffer)
            return next.size
        }
        override fun stop() = Unit
        override fun release() { released = true }
    }
}
