package com.nestnote.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class ToolRailTab(val label: String, val icon: ImageVector) {
    Colour("Colour", Icons.Default.Palette),
    Background("Background", Icons.Default.Image),
    Format("Format", Icons.Default.TextFields)
}

internal enum class FormatAction(val label: String) {
    Checklist("Checklist"), Bullet("Bullet list"), Numbered("Numbered list"), Quote("Blockquote"),
    Bold("Bold"), Italic("Italic"), Strike("Strikethrough"), Code("Inline code"), Link("Insert link"),
    Heading1("Heading 1"), Heading2("Heading 2"), Heading3("Heading 3")
}

private val backgroundNames = listOf("Plain colour", "Botanical", "Sunset", "Waves", "Soft shapes", "Paper")

/**
 * Slim vertical tool rail for the editor: Colour, Background and Format tabs in a ~58dp pill on the
 * right edge. Replaces the two wide side panels. Colour/Background edits are a draft until
 * [onApply]; [onCancel] (close button, outside tap on appearance tabs, or system Back) reverts them.
 * Format actions apply immediately to the note text.
 */
@Composable
internal fun EditorToolRail(
    visible: Boolean,
    tab: ToolRailTab,
    onTabChange: (ToolRailTab) -> Unit,
    colorIndex: Int,
    backgroundIndex: Int,
    onColorSelected: (Int) -> Unit,
    onBackgroundSelected: (Int) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onFormat: (FormatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    BackHandler(enabled = visible, onBack = onCancel)
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Outside tap cancels appearance drafts. Format keeps the text field reachable instead.
        if (visible && tab != ToolRailTab.Format) {
            Box(
                Modifier.fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
                    .testTag("appearance_scrim")
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            enter = slideInHorizontally(NestMotion.spatial()) { it + 48 } + fadeIn(NestMotion.fast()),
            exit = slideOutHorizontally(NestMotion.fast()) { it + 48 } + fadeOut(NestMotion.fast())
        ) {
            Surface(
                modifier = Modifier.width(58.dp).heightIn(max = maxHeight * 0.86f).testTag("tool_rail"),
                shape = RoundedCornerShape(29.dp),
                color = colors.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Column(
                    Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ToolRailTab.values().forEach { t ->
                        RailSelectable(t.label, tab == t) { onTabChange(t) }.let { sel ->
                            Box(sel, contentAlignment = Alignment.Center) {
                                Icon(t.icon, contentDescription = t.label, tint = if (tab == t) colors.primary else colors.onSurfaceVariant)
                            }
                        }
                    }
                    RailDivider()
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (tab) {
                            ToolRailTab.Colour -> noteAccents.forEachIndexed { index, tint ->
                                val isSelected = index == colorIndex
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(tint)
                                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF383044) else Color(0xFF77734F), CircleShape)
                                        .clickable { onColorSelected(index) }
                                        .semantics { contentDescription = "Note colour ${index + 1}" + (if (isSelected) ", selected" else ""); this.selected = isSelected }
                                        .testTag("note_color_$index"),
                                    contentAlignment = Alignment.Center
                                ) { if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF383044)) }
                            }
                            ToolRailTab.Background -> backgroundNames.forEachIndexed { index, name ->
                                val isSelected = index == backgroundIndex
                                Box(
                                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF383044) else Color(0xFF77734F), RoundedCornerShape(12.dp))
                                        .clickable { onBackgroundSelected(index) }
                                        .semantics { contentDescription = name + (if (isSelected) ", selected" else ""); this.selected = isSelected }
                                        .testTag("note_background_$index")
                                ) {
                                    if (index == 0) Box(Modifier.fillMaxSize().background(noteTints[colorIndex]))
                                    else NoteWallpaperArtwork(index, Modifier.fillMaxSize())
                                }
                            }
                            ToolRailTab.Format -> FormatAction.values().forEach { action ->
                                if (action == FormatAction.Bold || action == FormatAction.Heading1) RailDivider()
                                Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                                        .clickable(role = Role.Button) { onFormat(action) }
                                        .semantics { contentDescription = action.label }
                                        .testTag("format_${action.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val tint = colors.onSurface
                                    when (action) {
                                        FormatAction.Checklist -> Icon(Icons.Default.CheckBox, null, tint = tint)
                                        FormatAction.Bullet -> Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null, tint = tint)
                                        FormatAction.Numbered -> Icon(Icons.Default.FormatListNumbered, null, tint = tint)
                                        FormatAction.Quote -> Icon(Icons.Default.FormatQuote, null, tint = tint)
                                        FormatAction.Bold -> Icon(Icons.Default.FormatBold, null, tint = tint)
                                        FormatAction.Italic -> Icon(Icons.Default.FormatItalic, null, tint = tint)
                                        FormatAction.Strike -> Icon(Icons.Default.FormatStrikethrough, null, tint = tint)
                                        FormatAction.Code -> Icon(Icons.Default.Code, null, tint = tint)
                                        FormatAction.Link -> Icon(Icons.Default.Link, null, tint = tint)
                                        FormatAction.Heading1 -> Text("H1", fontWeight = FontWeight.Bold, color = tint)
                                        FormatAction.Heading2 -> Text("H2", fontWeight = FontWeight.Bold, color = tint)
                                        FormatAction.Heading3 -> Text("H3", fontWeight = FontWeight.Bold, color = tint)
                                    }
                                }
                            }
                        }
                    }
                    RailDivider()
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).clickable(role = Role.Button, onClick = onCancel)
                            .semantics { contentDescription = "Cancel and close tools" }.testTag("appearance_cancel"),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Close, contentDescription = null, tint = colors.onSurfaceVariant) }
                    FilledIconButton(onClick = onApply, modifier = Modifier.size(44.dp).testTag("appearance_apply")) {
                        Icon(Icons.Default.Check, contentDescription = "Apply and close tools")
                    }
                }
            }
        }
    }
}

@Composable
private fun RailSelectable(label: String, selected: Boolean, onClick: () -> Unit): Modifier {
    val colors = MaterialTheme.colorScheme
    return Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
        .background(if (selected) colors.primary.copy(alpha = 0.16f) else Color.Transparent)
        .selectable(selected = selected, role = Role.Tab, onClick = onClick)
}

@Composable
private fun RailDivider() {
    HorizontalDivider(Modifier.width(28.dp).padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
