package com.nestnote.app

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun BootLandingScreen(preferences: android.content.SharedPreferences) {
    val colors = MaterialTheme.colorScheme
    var soundEnabled by remember {
        mutableStateOf(preferences.getBoolean(PREF_BOOT_SOUND_ENABLED, true))
    }
    var progress by remember { mutableFloatStateOf(0.12f) }
    val pulse = remember { Animatable(0.86f) }
    LaunchedEffect(Unit) {
        // Keep startup audio short, quiet, and user-controllable. Release the audio generator
        // immediately; no audio resource or looping playback is retained.
        if (soundEnabled) {
            runCatching {
                ToneGenerator(AudioManager.STREAM_SYSTEM, 28).let { tone ->
                    tone.startTone(ToneGenerator.TONE_PROP_ACK, 110)
                    delay(140)
                    tone.release()
                }
            }
        }
        while (progress < 0.9f) {
            delay(180)
            progress = (progress + 0.075f).coerceAtMost(0.9f)
        }
    }
    LaunchedEffect(Unit) {
        pulse.animateTo(1.04f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(950), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ))
    }
    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(108.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                    .clip(RoundedCornerShape(32.dp)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("NESTNOTE", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Text("Preparing your workspace", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(30.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.68f).height(4.dp).clip(CircleShape))
            Spacer(Modifier.height(12.dp))
            Text("Loading notes and getting things ready…", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(30.dp))
            TextButton(onClick = {
                soundEnabled = !soundEnabled
                preferences.edit().putBoolean(PREF_BOOT_SOUND_ENABLED, soundEnabled).apply()
            }) {
                Icon(if (soundEnabled) Icons.Default.NotificationsNone else Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (soundEnabled) "Boot sound on" else "Boot sound off")
            }
        }
    }
}

