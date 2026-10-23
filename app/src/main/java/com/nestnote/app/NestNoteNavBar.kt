package com.nestnote.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
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
internal val moreSubViews = listOf("Settings", "PrivateVaultSettings", "Backup", "Pinned", "Archived", "Categories", "About", "Templates", "Insights", "Shortcuts")

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
    val reducedMotion = rememberSystemReducedMotion()
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
            NavItem("Home", Icons.Outlined.Home, Icons.Default.Home, current == "Home", reducedMotion) { onSelect("Home") }
            NavItem("Notes", Icons.Outlined.Description, Icons.Default.Description, current == "All notes", reducedMotion) { onSelect("All notes") }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val fabSource = remember { MutableInteractionSource() }
                val fabPressed by fabSource.collectIsPressedAsState()
                val fabRotation by animateFloatAsState(
                    if (fabPressed) 90f else 0f, NestMotion.fast(reducedMotion), label = "fab-rotation"
                )
                Surface(
                    onClick = onNewNote,
                    interactionSource = fabSource,
                    modifier = Modifier
                        .size(52.dp)
                        .pressScale(fabSource, pressedScale = 0.92f)
                        .semantics { contentDescription = "Create note" },
                    shape = RoundedCornerShape(18.dp),
                    color = colors.primary,
                    contentColor = colors.onPrimary,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = fabRotation }
                        )
                    }
                }
            }
            NavItem("Reminders", Icons.Outlined.NotificationsNone, Icons.Default.Notifications, current == "Reminders", reducedMotion) { onSelect("Reminders") }
            NavItem("More", Icons.Outlined.MoreHoriz, Icons.Default.MoreHoriz, current == "More", reducedMotion) { onSelect("More") }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        if (selected) colors.primary else colors.onSurfaceVariant, NestMotion.fast(reducedMotion), label = "nav-tint"
    )
    // The pill grows out of the icon's centre instead of fading in as a fixed block.
    val pillScale by animateFloatAsState(
        if (selected) 1f else 0.4f,
        if (reducedMotion) snap() else spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "nav-pill-scale"
    )
    val pillAlpha by animateFloatAsState(
        if (selected) 1f else 0f, NestMotion.fast(reducedMotion), label = "nav-pill-alpha"
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.1f else 1f,
        if (reducedMotion) snap() else spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "nav-icon-scale"
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(width = 60.dp, height = 32.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = pillScale
                        scaleY = pillScale
                        alpha = pillAlpha
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primary.copy(alpha = 0.16f))
            )
            Icon(
                if (selected) selectedIcon else icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp).graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
            )
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
