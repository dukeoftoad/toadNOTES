package com.nestnote.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsHeroCard(
    totalNotes: Int,
    archivedNotes: Int,
    themeLabel: String,
    darkTheme: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient Canvas graphic in top right
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
            ) {
                drawCircle(
                    color = colors.primary.copy(alpha = 0.08f),
                    radius = 90.dp.toPx(),
                    center = Offset(size.width, 0f)
                )
                drawCircle(
                    color = colors.tertiary.copy(alpha = 0.06f),
                    radius = 55.dp.toPx(),
                    center = Offset(size.width, 0f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Make NESTNOTE feel like yours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Quick metrics badge pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroBadgePill(
                        icon = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        text = themeLabel,
                        modifier = Modifier.weight(1f)
                    )
                    HeroBadgePill(
                        icon = Icons.Default.Edit,
                        text = "$totalNotes notes",
                        modifier = Modifier.weight(1f)
                    )
                    HeroBadgePill(
                        icon = Icons.Default.Archive,
                        text = "$archivedNotes archived",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun HeroBadgePill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Visual Theme Selector with Graphical Cards
// -------------------------------------------------------------------------------------------------

@Composable
internal fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    reducedMotion: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = selected == mode
            val colors = MaterialTheme.colorScheme
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.outlineVariant,
                animationSpec = NestMotion.fast(reducedMotion),
                label = "theme-card-border"
            )
            val borderWidth by animateDpAsState(
                targetValue = if (isSelected) 2.dp else 1.dp,
                animationSpec = NestMotion.fast(reducedMotion),
                label = "theme-card-border-width"
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) colors.primaryContainer.copy(alpha = 0.35f) else colors.surfaceContainerLow,
                animationSpec = NestMotion.fast(reducedMotion),
                label = "theme-card-container"
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) }
                    ),
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = BorderStroke(borderWidth, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mini artwork illustration for each theme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = when (mode) {
                            ThemeMode.SYSTEM -> Color(0xFF333333)
                            ThemeMode.LIGHT -> Color(0xFFFFF7ED)
                            ThemeMode.DARK -> Color(0xFF1E1E24)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (mode) {
                                ThemeMode.SYSTEM -> {
                                    // Split Day/Night graphic
                                    Row(Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(Color(0xFFFFF7ED)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.LightMode,
                                                contentDescription = null,
                                                tint = Color(0xFFC4552D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(Color(0xFF1A1A1A)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = Color(0xFFFF8A5C),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                ThemeMode.LIGHT -> {
                                    Icon(
                                        Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = Color(0xFFC4552D),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                ThemeMode.DARK -> {
                                    Icon(
                                        Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = Color(0xFFFF8A5C),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.primary else colors.onSurface
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Note Card Color Style with Live Swatches
// -------------------------------------------------------------------------------------------------

@Composable
internal fun NoteColorStyleCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    isPastel: Boolean,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surface,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "style-card-container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "style-card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "style-card-border-width"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(NestMotion.fast(reducedMotion)) + slideInHorizontally(
                        animationSpec = NestMotion.spatial(reducedMotion),
                        initialOffsetX = { it / 3 }
                    ),
                    exit = fadeOut(NestMotion.fast(reducedMotion)) + slideOutHorizontally(
                        animationSpec = NestMotion.fast(reducedMotion),
                        targetOffsetX = { it / 3 }
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
                }
            }

            // Live Swatch Palette Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPastel) {
                    // 6 representative pastel swatches
                    listOf(
                        noteTints[0], noteTints[1], noteTints[4],
                        noteTints[5], noteTints[7], noteTints[9]
                    ).forEach { swatch ->
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = swatch,
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f))
                        ) {}
                    }
                } else {
                    // Dynamic Material You swatches
                    listOf(
                        colors.primary,
                        colors.secondary,
                        colors.tertiary,
                        colors.primaryContainer,
                        colors.secondaryContainer
                    ).forEach { swatch ->
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = swatch,
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f))
                        ) {}
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Dashboard Layout Wireframe Selector (Grid vs List Mini-Graphics)
// -------------------------------------------------------------------------------------------------

@Composable
internal fun DashboardViewWireframeSelector(
    selected: DashboardLayout,
    onSelect: (DashboardLayout) -> Unit,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Grid Option Card
        LayoutWireframeCard(
            label = "Grid",
            isSelected = selected == DashboardLayout.GRID,
            onClick = { onSelect(DashboardLayout.GRID) },
            isGrid = true,
            modifier = Modifier.weight(1f),
            reducedMotion = reducedMotion
        )

        // List Option Card
        LayoutWireframeCard(
            label = "List",
            isSelected = selected == DashboardLayout.LIST,
            onClick = { onSelect(DashboardLayout.LIST) },
            isGrid = false,
            modifier = Modifier.weight(1f),
            reducedMotion = reducedMotion
        )
    }
}

@Composable
internal fun LayoutWireframeCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isGrid: Boolean,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "layout-card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "layout-card-border-width"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) colors.secondaryContainer.copy(alpha = 0.45f) else colors.surfaceContainerLow,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "layout-card-container"
    )

    Surface(
        modifier = modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick
        ),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Graphical mini-wireframe
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceContainerHighest.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGrid) {
                        // 2x2 grid mini-cards
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                                ) {}
                            }
                        }
                    } else {
                        // Stacked horizontal list cards
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                            ) {}
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.28f) else colors.outlineVariant.copy(alpha = 0.4f)
                            ) {}
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colors.primary else colors.onSurface
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Voice Player Style Option & Waveform Graphics
// -------------------------------------------------------------------------------------------------

@Composable
internal fun VoicePlayerStyleOption(
    label: String,
    description: String,
    isCompact: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "voice-style-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "voice-style-border-width"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "voice-style-container"
    )

    Surface(
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick
        ),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun AnimatedWaveformPreview(
    active: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    // Only run the infinite transition while the preview is actually animating; it used to
    // tick at frame rate on every idle settings row with its value pinned to 0.
    val phase = if (reducedMotion || !active) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "waveform-anim")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1400),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveform-phase"
        )
        animatedPhase
    }

    Canvas(modifier = modifier) {
        val barCount = 20
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val minHeight = 4.dp.toPx()
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val sinVal = if (reducedMotion || !active) {
                Math.sin(i * 0.4).toFloat().coerceIn(0f, 1f)
            } else {
                ((Math.sin(i * 0.45 + phase.toDouble()) + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)
            }
            val barH = minHeight + sinVal * (maxHeight - minHeight)
            val left = i * (barWidth + barSpacing)
            val top = (size.height - barH) / 2f

            drawRoundRect(
                color = if (i % 2 == 0) colors.primary else colors.tertiary,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
internal fun PrivacyFeatureRow(
    icon: ImageVector,
    title: String,
    body: String
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = colors.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Reusable Shared Controls
// -------------------------------------------------------------------------------------------------

@Composable
internal fun SettingLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = label(option),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberSystemReducedMotion()
    val rowColor by animateColorAsState(
        targetValue = if (checked) colors.secondaryContainer.copy(alpha = 0.42f) else Color.Transparent,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "settings-switch-row-background"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rowColor)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primary,
                checkedTrackColor = colors.primaryContainer
            )
        )
    }
}

@Composable
internal fun ChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    reducedMotion: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surface,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "choice-card-container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "choice-card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = NestMotion.fast(reducedMotion),
        label = "choice-card-border-width"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(width = borderWidth, color = borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) colors.primary.copy(alpha = 0.15f) else colors.surfaceContainerHighest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (selected) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(NestMotion.fast(reducedMotion)) + slideInHorizontally(
                    animationSpec = NestMotion.spatial(reducedMotion),
                    initialOffsetX = { it / 3 }
                ),
                exit = fadeOut(NestMotion.fast(reducedMotion)) + slideOutHorizontally(
                    animationSpec = NestMotion.fast(reducedMotion),
                    targetOffsetX = { it / 3 }
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary)
            }
        }
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.65f))
            content()
        }
    }
}

@Composable
internal fun OverviewTile(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Dedicated encrypted Backup & Restore Screen
