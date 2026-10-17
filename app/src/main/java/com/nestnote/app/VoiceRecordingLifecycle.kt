package com.nestnote.app

/**
 * Thread-safe, Android-independent state gate for a single recording session.
 * STOPPED and DISCARDED are terminal; a late worker cannot revive either state.
 */
internal class VoiceRecordingLifecycle {
    internal enum class State { NEW, RECORDING, PAUSED, STOPPED, DISCARDED }

    private var current = State.NEW

    @Synchronized
    fun start(): Boolean {
        if (current != State.NEW) return false
        current = State.RECORDING
        return true
    }

    @Synchronized
    fun pause(): Boolean {
        if (current != State.RECORDING) return false
        current = State.PAUSED
        return true
    }

    @Synchronized
    fun resume(): Boolean {
        if (current != State.PAUSED) return false
        current = State.RECORDING
        return true
    }

    @Synchronized
    fun stop(): Boolean {
        if (current == State.STOPPED || current == State.DISCARDED) return false
        current = State.STOPPED
        return true
    }

    @Synchronized
    fun discard(): Boolean {
        if (current == State.DISCARDED) return false
        current = State.DISCARDED
        return true
    }

    @Synchronized
    fun state(): State = current
}
