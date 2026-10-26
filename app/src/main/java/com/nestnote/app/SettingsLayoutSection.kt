package com.nestnote.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsLayoutSection(
    dashboardLayout: DashboardLayout,
    onLayoutChange: (DashboardLayout) -> Unit,
    reducedMotion: Boolean,
    dashboardDensity: DashboardDensity,
    onDensityChange: (DashboardDensity) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SettingsSection(
        title = "Layout",
        subtitle = "Choose how your notes appear",
        icon = Icons.Default.GridView
    ) {
        SettingLabel("Dashboard view")
    
        // Visual mini-wireframe cards for Grid vs List
        DashboardViewWireframeSelector(
            selected = dashboardLayout,
            onSelect = onLayoutChange,
            reducedMotion = reducedMotion
        )
    
        Text(
            if (dashboardLayout == DashboardLayout.GRID) {
                "Compact cards, side by side"
            } else {
                "Full-width notes with more preview space"
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SettingLabel("Dashboard density")
        SegmentedChoice(DashboardDensity.entries, dashboardDensity, { it.label }, onDensityChange)
        Text(
            dashboardDensity.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
