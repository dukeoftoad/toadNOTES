package com.nestnote.app

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsVoicePlayerSection(
    reducedMotion: Boolean,
    onOpenVoicePlayerSettings: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val playerPrefs = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var compactVoicePlayer by remember {
        mutableStateOf(playerPrefs.getBoolean(PREF_VOICE_PLAYER_COMPACT, true))
    }
    SettingsSection(
        title = "Voice player",
        subtitle = "Playback controls, waveform and appearance",
        icon = Icons.Default.Mic
    ) {
        SettingLabel("Player style")
    
        // Interactive style selector with live graphics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VoicePlayerStyleOption(
                label = "Compact",
                description = "Minimal circular pill with waveform dots",
                isCompact = true,
                selected = compactVoicePlayer,
                onClick = {
                    compactVoicePlayer = true
                    playerPrefs.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, true).putString(PREF_VOICE_PLAYER_STYLE, VoicePlayerStyle.COMPACT.name).apply()
                },
                modifier = Modifier.weight(1f),
                reducedMotion = reducedMotion
            )
    
            VoicePlayerStyleOption(
                label = "Full",
                description = "Expanded player with scrubber and seek buttons",
                isCompact = false,
                selected = !compactVoicePlayer,
                onClick = {
                    compactVoicePlayer = false
                    playerPrefs.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, false).putString(PREF_VOICE_PLAYER_STYLE, VoicePlayerStyle.FULL.name).apply()
                },
                modifier = Modifier.weight(1f),
                reducedMotion = reducedMotion
            )
        }
    
        // Interactive Live Waveform Preview Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenVoicePlayerSettings() }
                .testTag("settings_voice_player_open"),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Interactive waveform preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (compactVoicePlayer) "Compact mode active" else "Full player active",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open voice player settings",
                        tint = colors.onSurfaceVariant
                    )
                }
    
                // Dynamic animated waveform bars
                AnimatedWaveformPreview(
                    active = true,
                    reducedMotion = reducedMotion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                )
            }
        }
    }
}
