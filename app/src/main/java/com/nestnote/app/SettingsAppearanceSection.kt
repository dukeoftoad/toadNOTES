package com.nestnote.app

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsAppearanceSection(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    reducedMotion: Boolean,
    dynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    amoled: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    accentPalette: AccentPalette,
    onAccentPaletteChange: (AccentPalette) -> Unit,
    noteColorStyle: NoteColorStyle,
    onNoteColorStyleChange: (NoteColorStyle) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SettingsSection(
        title = "Appearance",
        subtitle = "Colors and themes that match your style",
        icon = Icons.Default.Palette
    ) {
        SettingLabel("Theme")
    
        // Visual theme graphics cards
        ThemeModeSelector(
            selected = themeMode,
            onSelect = onThemeChange,
            reducedMotion = reducedMotion
        )
    
        if (themeMode == ThemeMode.SYSTEM) {
            Text(
                "Follows your Android light/dark setting",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            )
        }
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SwitchRow(
            title = "Wallpaper colors",
            summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "Use Android dynamic colors from your wallpaper"
            } else {
                "Requires Android 12 or newer; older devices use the app palette"
            },
            checked = dynamicColors,
            onChange = onDynamicColorsChange
        )
    
        SwitchRow(
            title = "Pure black in dark mode",
            summary = "Saves battery on OLED screens. Applies when the theme is dark.",
            checked = amoled,
            onChange = onAmoledChange
        )
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SettingLabel("App accent colour")
        Text(
            "Choose the colour used for buttons, highlights and selected controls.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPalette.values().toList().chunked(4).forEach { paletteRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paletteRow.forEach { palette ->
                        val selectedPalette = accentPalette == palette
                        val swatch = if (themeMode == ThemeMode.DARK ||
                            (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                        ) palette.primaryDark else palette.primaryLight
                        Surface(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .clickable { onAccentPaletteChange(palette) }
                                .semantics { contentDescription = "${palette.label} accent colour${if (selectedPalette) ", selected" else ""}" },
                            color = if (selectedPalette) colors.secondaryContainer else colors.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                if (selectedPalette) 2.dp else 1.dp,
                                if (selectedPalette) colors.primary else colors.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(swatch),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedPalette) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = if (swatch.luminance() > 0.45f) Color.Black else Color.White, modifier = Modifier.size(17.dp))
                                    }
                                }
                                Text(palette.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                    repeat((4 - paletteRow.size).coerceAtLeast(0)) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (accentPalette != AccentPalette.DEFAULT && dynamicColors) {
            Text(
                "Selecting a custom accent disables wallpaper colours so your palette stays consistent.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SettingLabel("Note card colors")
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteColorStyleCard(
                title = NoteColorStyle.PASTEL.label,
                description = NoteColorStyle.PASTEL.description,
                selected = noteColorStyle == NoteColorStyle.PASTEL,
                onClick = { onNoteColorStyleChange(NoteColorStyle.PASTEL) },
                isPastel = true,
                reducedMotion = reducedMotion
            )
            NoteColorStyleCard(
                title = NoteColorStyle.ADAPTIVE.label,
                description = NoteColorStyle.ADAPTIVE.description,
                selected = noteColorStyle == NoteColorStyle.ADAPTIVE,
                onClick = { onNoteColorStyleChange(NoteColorStyle.ADAPTIVE) },
                isPastel = false,
                reducedMotion = reducedMotion
            )
        }
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SettingLabel("Custom note wallpapers")
        Text(
            "Open any note and select the palette tool. In the Colour tab, tap + to choose an image from your device. The wallpaper is saved with that note; use the Background tab to return to an illustrated background.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.secondaryContainer.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, colors.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.onSecondaryContainer.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = colors.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Personalize each note",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSecondaryContainer
                    )
                    Text(
                        "Choose a color and background independently for each note.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSecondaryContainer
                    )
                }
            }
        }
    }
}
