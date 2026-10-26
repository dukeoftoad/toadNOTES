package com.nestnote.app

import android.graphics.Typeface as AndroidTypeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
internal fun SettingsEditorSection(
    defaultMarkdownZoomPercent: Int,
    onDefaultMarkdownZoomPercentChange: (Int) -> Unit,
    editorMode: EditorPresentationMode,
    onEditorModeChange: (EditorPresentationMode) -> Unit,
    reducedMotion: Boolean,
    editorFontStyle: EditorFontFamily,
    customFontPath: String?,
    onFontFamilySelected: (EditorFontFamily) -> Unit,
    onImportFont: () -> Unit,
    customTextSizeSp: Int,
    onCustomTextSizeChange: (Int) -> Unit,
    editorTextSize: EditorTextSize,
    onEditorTextSizePreset: (EditorTextSize) -> Unit,
    defaultNoteFormat: String,
    onDefaultNoteFormatChange: (String) -> Unit,
    showEditorCounts: Boolean,
    onShowEditorCountsChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SettingsSection(
        title = "Note editor",
        subtitle = "Markdown formatting, text size, and writing presentation",
        icon = Icons.Default.Code
    ) {
        SettingLabel("Default Markdown viewer zoom")
        Text(
            "$defaultMarkdownZoomPercent%",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = defaultMarkdownZoomPercent.toFloat(),
            onValueChange = { onDefaultMarkdownZoomPercentChange((it / 5f).toInt() * 5) },
            valueRange = 50f..200f,
            steps = 29,
            modifier = Modifier.fillMaxWidth().testTag("default_markdown_zoom_slider")
        )
        Text(
            "Used when opening Markdown files from Files. You can adjust each file from the viewer's More menu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingLabel("Open editor as")
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorPresentationMode.entries.forEach { mode ->
                ChoiceCard(
                    title = mode.label,
                    description = mode.description,
                    selected = editorMode == mode,
                    onClick = { onEditorModeChange(mode) },
                    icon = when (mode) {
                        EditorPresentationMode.SHEET -> Icons.Default.ViewAgenda
                        EditorPresentationMode.FULL_SCREEN -> Icons.Default.Smartphone
                        EditorPresentationMode.QUICK_DIALOG -> Icons.Default.Tune
                    },
                    reducedMotion = reducedMotion
                )
            }
        }
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SettingLabel("Font family")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorFontFamily.entries.filter { it != EditorFontFamily.CUSTOM || customFontPath != null }
                .forEach { family ->
                    FilterChip(
                        selected = editorFontStyle == family,
                        onClick = {
                            onFontFamilySelected(family)
                        },
                        label = { Text(family.label) }
                    )
                }
        }
        OutlinedButton(
            onClick = onImportFont,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (customFontPath == null) "Import TTF or OTF font" else "Replace imported font")
        }
        Text(
            "Imported fonts stay in this app's private storage.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
        SettingLabel("Body text size · $customTextSizeSp sp")
        Slider(
            value = customTextSizeSp.toFloat(),
            onValueChange = { value ->
                onCustomTextSizeChange(value.roundToInt().coerceIn(12, 32))
            },
            valueRange = 12f..32f,
            steps = 19,
            modifier = Modifier.testTag("editor_font_size_slider")
        )
        SegmentedChoice(
            EditorTextSize.entries,
            editorTextSize,
            { it.label },
            { preset ->
                onEditorTextSizePreset(preset)
            }
        )
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
        SettingLabel("Default format for new notes")
        SegmentedChoice(
            listOf("plain", "markdown"),
            defaultNoteFormat,
            { if (it == "markdown") "Markdown" else "Plain text" },
            { format ->
                onDefaultNoteFormatChange(format)
            }
        )
    
        // Live animated preview note card
        val animatedFontSize by animateFloatAsState(
            targetValue = customTextSizeSp.toFloat(),
            animationSpec = NestMotion.spatial(reducedMotion),
            label = "editor-font-size-preview"
        )
        val previewTypeface = remember(editorFontStyle, customFontPath) {
            when (editorFontStyle) {
                EditorFontFamily.SERIF -> AndroidTypeface.SERIF
                EditorFontFamily.MONOSPACE -> AndroidTypeface.MONOSPACE
                EditorFontFamily.CURSIVE -> AndroidTypeface.create("casual", AndroidTypeface.NORMAL)
                EditorFontFamily.CUSTOM -> customFontPath?.let { path ->
                    runCatching { AndroidTypeface.createFromFile(path) }.getOrNull()
                } ?: AndroidTypeface.SANS_SERIF
                EditorFontFamily.SANS -> AndroidTypeface.SANS_SERIF
            }
        }
        val previewFontFamily = remember(previewTypeface) {
            androidx.compose.ui.text.font.FontFamily(previewTypeface)
        }
    
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainerHighest,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Preview note",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${editorFontStyle.label} · $customTextSizeSp sp",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Text(
                    "Your notes will look like this.",
                    fontSize = animatedFontSize.sp,
                    lineHeight = (animatedFontSize * 1.45f).sp,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Normal,
                    fontFamily = previewFontFamily
                )
            }
        }
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SwitchRow(
            title = "Word count and reading time",
            summary = "Show a live count under the note while you write",
            checked = showEditorCounts,
            onChange = onShowEditorCountsChange
        )
    }
}
