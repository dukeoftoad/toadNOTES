package com.nestnote.app

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.viewmodel.compose.viewModel


// Appearance side panel and wallpaper artwork.

@Composable
internal fun AppearanceSidePanel(
    colorIndex: Int,
    backgroundIndex: Int,
    onColorSelected: (Int) -> Unit,
    onBackgroundSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxHeight().verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text("Make this note feel like yours", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("appearance_close")) {
                Icon(Icons.Default.Close, contentDescription = "Close appearance panel")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().height(150.dp).testTag("appearance_preview"),
            shape = RoundedCornerShape(18.dp),
            color = noteTints[colorIndex],
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Box {
                if (backgroundIndex > 0) NoteWallpaperArtwork(backgroundIndex, Modifier.fillMaxSize())
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your note", style = MaterialTheme.typography.titleMedium, color = Color(0xFF292633))
                    Text("A little space for your thoughts…", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF514D57))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Colour palette", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text("Choose a soft tint", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            // Stack colour swatches into short rows to avoid horizontal scrolling.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                noteAccents.chunked(4).forEach { swatchRow ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        swatchRow.forEach { tint ->
                            val index = noteAccents.indexOf(tint)
                            val isSelected = index == colorIndex
                            Box(
                                Modifier.size(48.dp).clip(CircleShape)
                                    .background(tint)
                                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF383044) else Color(0xFF77734F), CircleShape)
                                    .clickable { onColorSelected(index) }
                                    .semantics { contentDescription = "Note colour " + (index + 1) + (if (isSelected) ", selected" else ""); this.selected = isSelected }
                                    .testTag("note_color_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF383044))
                            }
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Backgrounds", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text("Add a little atmosphere", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            (0..5).forEach { index ->
                val isSelected = index == backgroundIndex
                Surface(
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                        .testTag("note_background_$index")
                        .clickable { onBackgroundSelected(index) }
                        .semantics { contentDescription = (if (index == 0) "Plain colour" else listOf("", "Botanical", "Sunset", "Waves", "Soft shapes", "Paper")[index]) + (if (isSelected) ", selected" else ""); this.selected = isSelected },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF383044) else Color(0xFF77734F)),
                    color = noteTints[colorIndex]
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(9.dp))) {
                            if (index == 0) Box(Modifier.fillMaxSize().background(noteTints[colorIndex]))
                            else NoteWallpaperArtwork(index, Modifier.fillMaxSize())
                        }
                        Text(
                            if (index == 0) "Plain colour" else listOf("", "Botanical", "Sunset", "Waves", "Soft shapes", "Paper")[index],
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF292633)
                        )
                        if (isSelected) Icon(Icons.Default.Check, contentDescription = "Selected background", tint = Color(0xFF383044))
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).testTag("appearance_cancel")) {
                Text("Cancel")
            }
            Button(onClick = onApply, modifier = Modifier.weight(1f)) {
                Text("Apply")
            }
        }
        Text("Your choice is saved with the note when you tap Save.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun NoteWallpaperArtwork(index: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (index) {
            1 -> {
                drawRect(Color(0xFFE7E6D4))
                drawCircle(Color(0xFFF3D6A4), radius = w * 0.16f, center = Offset(w * 0.78f, h * 0.24f))
                drawOval(Color(0xFF7E8E6C), Offset(w * -0.12f, h * 0.66f), Size(w * 0.70f, h * 0.45f))
                drawOval(Color(0xFF586E51), Offset(w * 0.22f, h * 0.53f), Size(w * 0.48f, h * 0.55f))
                drawOval(Color(0xFF8C9A72), Offset(w * 0.56f, h * 0.60f), Size(w * 0.50f, h * 0.46f))
                for (i in 0..8) {
                    val x = w * (0.02f + i * 0.12f)
                    val y = h * (0.56f - (i % 3) * 0.12f)
                    drawLine(Color(0xFF52694D), Offset(x, h), Offset(x + w * 0.02f, y + h * 0.20f), strokeWidth = w * 0.008f)
                    drawOval(if (i % 2 == 0) Color(0xFF718560) else Color(0xFF9EAA81), Offset(x - w * 0.08f, y), Size(w * 0.18f, h * 0.13f))
                }
            }
            2 -> {
                drawRect(Color(0xFFF2D7D0))
                for (i in -4..12) {
                    val x = w * (i / 12f)
                    drawLine(Color(0xFFFFF2E5), Offset(x, h * 0.08f), Offset(x + w * 0.72f, h * 0.88f), strokeWidth = w * 0.07f)
                    drawLine(Color(0xFFEABAB1), Offset(x + w * 0.08f, h * 0.08f), Offset(x + w * 0.80f, h * 0.88f), strokeWidth = w * 0.012f)
                }
            }
            3 -> {
                drawRect(Color(0xFFB7DDE6))
                drawCircle(Color(0xFFF3B27F), radius = w * 0.16f, center = Offset(w * 0.68f, h * 0.34f))
                val farWave = Path().apply {
                    moveTo(0f, h * 0.58f); cubicTo(w * 0.32f, h * 0.45f, w * 0.61f, h * 0.68f, w, h * 0.52f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(farWave, Color(0xFF87BACD))
                val nearWave = Path().apply {
                    moveTo(0f, h * 0.74f); cubicTo(w * 0.33f, h * 0.60f, w * 0.71f, h * 0.85f, w, h * 0.71f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(nearWave, Color(0xFF6296AE))
                drawOval(Color(0xFFECCCA4), Offset(w * 0.04f, h * 0.79f), Size(w * 0.46f, h * 0.13f))
            }
            4 -> {
                drawRect(Color(0xFFF8E6C4))
                drawCircle(Color(0xFFE9A77F), radius = w * 0.18f, center = Offset(w * 0.76f, h * 0.27f))
                val backDune = Path().apply {
                    moveTo(0f, h * 0.68f); cubicTo(w * 0.30f, h * 0.40f, w * 0.48f, h * 0.77f, w, h * 0.48f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(backDune, Color(0xFFE7B78B))
                val frontDune = Path().apply {
                    moveTo(0f, h * 0.82f); cubicTo(w * 0.36f, h * 0.67f, w * 0.56f, h * 0.92f, w, h * 0.73f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(frontDune, Color(0xFFD48E69))
            }
            else -> {
                drawRect(Color(0xFFC7E0E3))
                drawCircle(Color(0xFFF2B7A0), radius = w * 0.18f, center = Offset(w * 0.26f, h * 0.28f))
                val hill = Path().apply {
                    moveTo(0f, h * 0.60f); cubicTo(w * 0.25f, h * 0.72f, w * 0.48f, h * 0.49f, w, h * 0.65f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(hill, Color(0xFF70A9B4))
                val foreground = Path().apply {
                    moveTo(0f, h * 0.82f); cubicTo(w * 0.35f, h * 0.68f, w * 0.7f, h * 0.94f, w, h * 0.76f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(foreground, Color(0xFF568A96))
            }
        }
    }
}
