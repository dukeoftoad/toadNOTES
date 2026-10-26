package com.nestnote.app

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

// Keep each request paired with its listener so every lifecycle path releases the
// exact request that was granted.
private val voiceAudioFocusRequests =
    java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<AudioManager.OnAudioFocusChangeListener, AudioFocusRequest>()
    )

internal fun requestVoiceAudioFocus(
    audioManager: AudioManager,
    onInterrupted: () -> Unit
): AudioManager.OnAudioFocusChangeListener? {
    val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) onInterrupted()
    }
    val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setOnAudioFocusChangeListener(listener)
        .build()
    if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        return null
    }
    voiceAudioFocusRequests[listener] = request
    return listener
}

internal fun abandonVoiceAudioFocus(
    audioManager: AudioManager,
    listener: AudioManager.OnAudioFocusChangeListener?
) {
    if (listener == null) return
    val request = voiceAudioFocusRequests.remove(listener) ?: return
    audioManager.abandonAudioFocusRequest(request)
}
