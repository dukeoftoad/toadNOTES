package com.nestnote.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Destinations that live under the More tab; back from these returns to More. */
internal val moreSubViews = listOf("Settings", "Pinned", "Archived", "Categories")

/**
 * Floating four-tab bar with a centred New note button: Home, Notes, [New], Reminders, More.
 * [selectedView] uses the existing destination names so routing in [NestNoteApp] is unchanged.
 */
@Composable
internal fun NestNoteNavBar(
    selectedView: String,
    onSelect: (String) -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val current = when {
        selectedView == "Home" -> "Home"
        selectedView == "Reminders" -> "Reminders"
        selectedView == "More" || selectedView in moreSubViews -> "More"
        else -> "All notes" // "All notes" and every category filter
    }
    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem("Home", Icons.Default.Home, current == "Home") { onSelect("Home") }
            NavItem("Notes", Icons.AutoMirrored.Filled.List, current == "All notes") { onSelect("All notes") }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = onNewNote,
                    modifier = Modifier.size(52.dp).semantics { contentDescription = "Create note" },
                    shape = RoundedCornerShape(18.dp),
                    color = colors.primary,
                    contentColor = colors.onPrimary,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            }
            NavItem("Reminders", Icons.Default.NotificationsNone, current == "Reminders") { onSelect("Reminders") }
            NavItem("More", Icons.Default.MoreHoriz, current == "More") { onSelect("More") }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        if (selected) colors.primary else colors.onSurfaceVariant, NestMotion.fast(), label = "nav-tint"
    )
    val pill by animateColorAsState(
        if (selected) colors.primary.copy(alpha = 0.14f) else Color.Transparent, NestMotion.fast(), label = "nav-pill"
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(pill).padding(horizontal = 16.dp, vertical = 4.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
