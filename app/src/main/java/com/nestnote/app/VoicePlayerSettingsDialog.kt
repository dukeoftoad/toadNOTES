package com.nestnote.app

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

// -------------------------------------------------------------------------------------------------
// Voice Player Settings Full Dialog (Platform compatibility)
// -------------------------------------------------------------------------------------------------

@Composable
internal fun VoicePlayerSettingsDialog(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var compactVoicePlayer by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(PREF_VOICE_PLAYER_COMPACT, true)
        )
    }
    var voicePlaybackSpeed by rememberSaveable {
        mutableFloatStateOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(PREF_VOICE_PLAYER_SPEED, 1f)
        )
    }
    val playerPreferences = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var playerStyle by rememberSaveable {
        mutableStateOf(
            playerPreferences.getString(PREF_VOICE_PLAYER_STYLE, null)
                ?.let { runCatching { VoicePlayerStyle.valueOf(it) }.getOrNull() }
                ?: if (compactVoicePlayer) VoicePlayerStyle.COMPACT else VoicePlayerStyle.FULL
        )
    }
    var playerSizePercent by rememberSaveable {
        mutableIntStateOf(playerPreferences.getInt(PREF_VOICE_PLAYER_SIZE_PERCENT, DEFAULT_VOICE_PLAYER_SIZE_PERCENT)
            .coerceIn(MIN_VOICE_PLAYER_SIZE_PERCENT, MAX_VOICE_PLAYER_SIZE_PERCENT))
    }

    fun choosePlayerStyle(style: VoicePlayerStyle) {
        playerStyle = style
        compactVoicePlayer = style == VoicePlayerStyle.COMPACT || style == VoicePlayerStyle.MINIMAL
        playerPreferences.edit()
            .putString(PREF_VOICE_PLAYER_STYLE, style.name)
            .putBoolean(PREF_VOICE_PLAYER_COMPACT, compactVoicePlayer)
            .apply()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(26.dp),
            color = colors.surface
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("voice_player_settings_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Voice player", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Choose how recordings appear in your notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = colors.outlineVariant)

                Text("Default playback speed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "New and previewed recordings use this speed. You can still seek normally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row(
                    Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoicePlayerSpeeds.forEach { speed ->
                        FilterChip(
                            selected = voicePlaybackSpeed == speed,
                            onClick = {
                                voicePlaybackSpeed = speed
                                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                    .putFloat(PREF_VOICE_PLAYER_SPEED, speed).apply()
                            },
                            label = { Text("${speed}×") },
                            modifier = Modifier.weight(1f).testTag("voice_player_speed_${speed}")
                        )
                    }
                }

                HorizontalDivider(color = colors.outlineVariant)

                Text("Player style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose a layout that suits each note. The size control scales the player without changing the audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Player size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${playerSizePercent}%", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                    }
                    TextButton(onClick = {
                        playerSizePercent = DEFAULT_VOICE_PLAYER_SIZE_PERCENT
                        playerPreferences.edit().putInt(PREF_VOICE_PLAYER_SIZE_PERCENT, playerSizePercent).apply()
                    }) { Text("Reset") }
                }
                Slider(
                    value = playerSizePercent.toFloat(),
                    onValueChange = { value ->
                        playerSizePercent = value.roundToInt().coerceIn(MIN_VOICE_PLAYER_SIZE_PERCENT, MAX_VOICE_PLAYER_SIZE_PERCENT)
                        playerPreferences.edit().putInt(PREF_VOICE_PLAYER_SIZE_PERCENT, playerSizePercent).apply()
                    },
                    valueRange = MIN_VOICE_PLAYER_SIZE_PERCENT.toFloat()..MAX_VOICE_PLAYER_SIZE_PERCENT.toFloat(),
                    steps = 14,
                    modifier = Modifier.fillMaxWidth().testTag("voice_player_size_slider")
                )
                Text(
                    "75% compact  ·  100% default  ·  150% large",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                HorizontalDivider(color = colors.outlineVariant)

                Text("Choose a player layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoicePlayerStyle.values().forEach { style ->
                        val selected = playerStyle == style
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { choosePlayerStyle(style) }
                                .testTag("voice_player_style_${style.name.lowercase()}"),
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
                            border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) colors.primary else colors.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (style) {
                                                VoicePlayerStyle.COMPACT -> Icons.Default.PlayArrow
                                                VoicePlayerStyle.FULL -> Icons.Default.Mic
                                                VoicePlayerStyle.WAVEFORM -> Icons.Default.GraphicEq
                                                VoicePlayerStyle.MINIMAL -> Icons.Default.PlayArrow
                                                VoicePlayerStyle.TRANSPORT -> Icons.Default.Forward10
                                            },
                                            contentDescription = null,
                                            tint = if (selected) colors.onPrimary else colors.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(style.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(style.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                                RadioButton(selected = selected, onClick = { choosePlayerStyle(style) })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    "Your choice is saved automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
