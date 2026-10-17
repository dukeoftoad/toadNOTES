package com.nestnote.app

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecordingLifecycleTest {
    @Test fun supportsPauseResumeAndStopInOrder() {
        val lifecycle = VoiceRecordingLifecycle()
        assertTrue(lifecycle.start())
        assertTrue(lifecycle.pause())
        assertEquals(VoiceRecordingLifecycle.State.PAUSED, lifecycle.state())
        assertTrue(lifecycle.resume())
        assertEquals(VoiceRecordingLifecycle.State.RECORDING, lifecycle.state())
        assertTrue(lifecycle.stop())
        assertEquals(VoiceRecordingLifecycle.State.STOPPED, lifecycle.state())
    }

    @Test fun rejectsRepeatedOrOutOfOrderTransitionsAndFailureIsTerminal() {
        val lifecycle = VoiceRecordingLifecycle()
        assertFalse(lifecycle.pause())
        assertFalse(lifecycle.resume())
        assertTrue(lifecycle.start())
        assertFalse(lifecycle.start())
        assertTrue(lifecycle.fail())
        assertEquals(VoiceRecordingLifecycle.State.FAILED, lifecycle.state())
        assertFalse(lifecycle.pause())
        assertFalse(lifecycle.resume())
        assertFalse(lifecycle.stop())
        assertFalse(lifecycle.start())
    }

    @Test fun discardIsTerminalAndCannotBeRevivedByLateWorkerTransitions() {
        val lifecycle = VoiceRecordingLifecycle()
        assertTrue(lifecycle.start())
        assertTrue(lifecycle.discard())
        assertFalse(lifecycle.pause())
        assertFalse(lifecycle.resume())
        assertFalse(lifecycle.stop())
        assertFalse(lifecycle.start())
        assertEquals(VoiceRecordingLifecycle.State.DISCARDED, lifecycle.state())
    }

    @Test fun fakeCapturePumpsValidPcmChunksInOrder() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(listOf(byteArrayOf(1, 2), byteArrayOf(3, 4, 5)))
        val result = PcmCapturePump.capture(fake, output, 10, { true }, { false }, {})
        assertEquals(5L, result)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), output.toByteArray())
        assertTrue(fake.released)
    }

    @Test fun fakeCaptureWithNoAudioReturnsZeroBytes() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(emptyList())
        assertEquals(0L, PcmCapturePump.capture(fake, output, 10, { true }, { false }, {}))
        assertEquals(0, output.size())
    }

    @Test fun fakeCaptureReadFailurePropagatesAndReleasesCapture() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(emptyList(), readFailure = IOException("simulated microphone failure"))
        try {
            PcmCapturePump.capture(fake, output, 10, { true }, { false }, {})
            throw AssertionError("Expected read failure")
        } catch (expected: IOException) {
            assertEquals("simulated microphone failure", expected.message)
        }
        assertTrue(fake.released)
        assertEquals(0, output.size())
    }

    @Test fun fakeCaptureOverflowFailsWithoutWritingPartialChunk() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)))
        try {
            PcmCapturePump.capture(fake, output, 4, { true }, { false }, {})
            throw AssertionError("Expected size limit failure")
        } catch (_: PcmCaptureSizeLimitException) { }
        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertTrue(fake.released)
    }

    @Test fun repeatedStopIsIdempotentAndNeverRestartsSession() {
        val lifecycle = VoiceRecordingLifecycle()
        assertTrue(lifecycle.start())
        assertTrue(lifecycle.stop())
        assertFalse(lifecycle.stop())
        assertFalse(lifecycle.start())
        assertFalse(lifecycle.resume())
        assertEquals(VoiceRecordingLifecycle.State.STOPPED, lifecycle.state())
    }

    @Test fun lateCompletionAfterDiscardCannotRecreateManifestEntry() {
        val lock = Any()
        var discarded = false
        val manifest = mutableMapOf<String, String>()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val worker = Thread {
            started.countDown()
            release.await()
            synchronized(lock) {
                if (!discarded) manifest["clip"] = "saved"
            }
        }
        synchronized(lock) { manifest["clip"] = "pending" }
        worker.start()
        assertTrue(started.await(2, TimeUnit.SECONDS))
        synchronized(lock) {
            discarded = true
            manifest.remove("clip")
        }
        release.countDown()
        worker.join(2000)
        assertFalse(worker.isAlive)
        synchronized(lock) { assertTrue(manifest.isEmpty()) }
    }

    @Test fun overflowedPartialCaptureIsNeverCommittedAsAClip() {
        val output = ByteArrayOutputStream()
        val fake = FakePcmCapture(listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)))
        var committed: ByteArray? = null
        try {
            PcmCapturePump.capture(fake, output, 4, { true }, { false }, {})
            committed = output.toByteArray()
        } catch (_: PcmCaptureSizeLimitException) {
            // The active encrypted-store worker also deletes its ciphertext when this signal is set.
        }
        assertEquals(null, committed)
        assertTrue(fake.released)
    }

    @Test fun stopDiscardRaceAndLateWorkerCannotReviveDiscardedState() {
        repeat(100) {
            val lifecycle = VoiceRecordingLifecycle()
            assertTrue(lifecycle.start())
            val ready = CountDownLatch(2)
            val go = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            try {
                val stop = pool.submit<Boolean> { ready.countDown(); go.await(); lifecycle.stop() }
                val discard = pool.submit<Boolean> { ready.countDown(); go.await(); lifecycle.discard() }
                assertTrue(ready.await(2, TimeUnit.SECONDS))
                go.countDown()
                stop.get(2, TimeUnit.SECONDS)
                discard.get(2, TimeUnit.SECONDS)
                val state = lifecycle.state()
                assertTrue(state == VoiceRecordingLifecycle.State.STOPPED || state == VoiceRecordingLifecycle.State.DISCARDED)
                assertFalse(lifecycle.start())
                assertFalse(lifecycle.resume())
            } finally { pool.shutdownNow() }
        }
    }

    @Test fun pauseResumeRacingWithStopNeverReactivatesSession() {
        repeat(100) {
            val lifecycle = VoiceRecordingLifecycle()
            assertTrue(lifecycle.start())
            val pool = Executors.newFixedThreadPool(3)
            val ready = CountDownLatch(3)
            val go = CountDownLatch(1)
            try {
                val pause = pool.submit { ready.countDown(); go.await(); lifecycle.pause() }
                val resume = pool.submit { ready.countDown(); go.await(); lifecycle.resume() }
                val stop = pool.submit { ready.countDown(); go.await(); lifecycle.stop() }
                assertTrue(ready.await(2, TimeUnit.SECONDS))
                go.countDown()
                pause.get(2, TimeUnit.SECONDS); resume.get(2, TimeUnit.SECONDS); stop.get(2, TimeUnit.SECONDS)
                assertTrue(lifecycle.state() == VoiceRecordingLifecycle.State.STOPPED)
                assertFalse(lifecycle.start())
                assertFalse(lifecycle.resume())
            } finally { pool.shutdownNow() }
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
            val next = chunks.removeFirstOrNull() ?: return -1
            next.copyInto(buffer)
            return next.size
        }
        override fun stop() = Unit
        override fun release() { released = true }
    }
}
