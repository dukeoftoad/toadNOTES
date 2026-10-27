package com.nestnote.app

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal enum class AccentPalette(val label: String, val primaryLight: Color, val primaryDark: Color, val secondaryLight: Color, val secondaryDark: Color) {
    DEFAULT("Default", Color(0xFF526B55), Color(0xFFB4D0B0), Color(0xFF526477), Color(0xFFB8C8E2)),
    OCEAN("Ocean", Color(0xFF006A9C), Color(0xFF8CCFFF), Color(0xFF41617D), Color(0xFFB5D2F0)),
    LAVENDER("Lavender", Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFF625B71), Color(0xFFCCC2DC)),
    ROSE("Rose", Color(0xFFB3265E), Color(0xFFFFB1C8), Color(0xFF76565F), Color(0xFFE8BFC9)),
    FOREST("Forest", Color(0xFF386A20), Color(0xFFA4D68B), Color(0xFF52634A), Color(0xFFC0D9B2)),
    SUNSET("Sunset", Color(0xFF9C4328), Color(0xFFFFB59D), Color(0xFF77574D), Color(0xFFE8BFB2)),
    AMBER("Amber", Color(0xFF805600), Color(0xFFFFD27A), Color(0xFF6D5D3F), Color(0xFFE4C99A));

    /**
     * Opaque tonal blend of [fg] into [bg]. Blending against the scheme's own container
     * surface (instead of alpha-compositing) keeps containers readable on any base —
     * including AMOLED black — and avoids translucent-overlap artifacts.
     */
    private fun blend(fg: Color, bg: Color, fraction: Float): Color = Color(
        red = fg.red * fraction + bg.red * (1 - fraction),
        green = fg.green * fraction + bg.green * (1 - fraction),
        blue = fg.blue * fraction + bg.blue * (1 - fraction),
        alpha = 1f
    )

    fun applyTo(scheme: ColorScheme, dark: Boolean): ColorScheme {
        val primary = if (dark) primaryDark else primaryLight
        val secondary = if (dark) secondaryDark else secondaryLight
        val base = scheme.surfaceContainerHigh
        return scheme.copy(
            primary = primary,
            onPrimary = if (dark) blend(primaryDark, Color.Black, 0.82f) else Color.White,
            primaryContainer = blend(primary, base, if (dark) 0.32f else 0.15f),
            onPrimaryContainer = if (dark) blend(primaryDark, Color.White, 0.55f) else primaryLight,
            secondary = secondary,
            onSecondary = if (dark) blend(secondaryDark, Color.Black, 0.82f) else Color.White,
            secondaryContainer = blend(secondary, base, if (dark) 0.28f else 0.14f),
            onSecondaryContainer = if (dark) blend(secondaryDark, Color.White, 0.55f) else secondaryLight,
            tertiary = secondary,
            onTertiary = if (dark) blend(secondaryDark, Color.Black, 0.82f) else Color.White,
            tertiaryContainer = blend(secondary, base, if (dark) 0.28f else 0.14f),
            onTertiaryContainer = if (dark) blend(secondaryDark, Color.White, 0.55f) else secondaryLight
        )
    }
}
