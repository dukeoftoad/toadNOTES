package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
}
