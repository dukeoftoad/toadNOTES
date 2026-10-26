package com.nestnote.app

import androidx.compose.ui.graphics.Color

internal enum class AccentPalette(val label: String, val primaryLight: Color, val primaryDark: Color, val secondaryLight: Color, val secondaryDark: Color) {
    DEFAULT("Default", Color(0xFF526B55), Color(0xFFB4D0B0), Color(0xFF526477), Color(0xFFB8C8E2)),
    OCEAN("Ocean", Color(0xFF006A9C), Color(0xFF8CCFFF), Color(0xFF41617D), Color(0xFFB5D2F0)),
    LAVENDER("Lavender", Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFF625B71), Color(0xFFCCC2DC)),
    ROSE("Rose", Color(0xFFB3265E), Color(0xFFFFB1C8), Color(0xFF76565F), Color(0xFFE8BFC9)),
    FOREST("Forest", Color(0xFF386A20), Color(0xFFA4D68B), Color(0xFF52634A), Color(0xFFC0D9B2)),
    SUNSET("Sunset", Color(0xFF9C4328), Color(0xFFFFB59D), Color(0xFF77574D), Color(0xFFE8BFB2)),
    AMBER("Amber", Color(0xFF805600), Color(0xFFFFD27A), Color(0xFF6D5D3F), Color(0xFFE4C99A));

    fun applyTo(scheme: androidx.compose.material3.ColorScheme, dark: Boolean) = scheme.copy(
        primary = if (dark) primaryDark else primaryLight,
        onPrimary = if (dark) Color(0xFF19351B) else Color.White,
        primaryContainer = if (dark) primaryDark.copy(alpha = 0.24f) else primaryLight.copy(alpha = 0.12f),
        onPrimaryContainer = if (dark) Color(0xFFE0EBDD) else primaryLight,
        secondary = if (dark) secondaryDark else secondaryLight,
        tertiary = if (dark) secondaryDark else secondaryLight
    )
}
