package com.nestnote.app

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation

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


// Theme tokens: shapes, typography and note colour swatches.

internal val fallbackAccent = Color(0xFF625B71)

// Stable Material 3 expressive styling: generous, varied corner geometry and a
// typography hierarchy, without opting into the experimental M3 Expressive APIs.
internal val nestNoteShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// Display face: Fraunces (variable, SIL OFL). Body text stays on the system font to keep the APK small.
@OptIn(ExperimentalTextApi::class)
internal val nestNoteDisplayFont = FontFamily(
    listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            R.font.fraunces_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.Setting("opsz", 36f)
            )
        )
    }
)

// Derive from the Material defaults so sizes and line heights are preserved.
// (Passing a bare TextStyle replaces the default style, dropping its font size.)
private val baseTypography = Typography()
internal val nestNoteTypography = baseTypography.copy(
    displaySmall = baseTypography.displaySmall.copy(fontFamily = nestNoteDisplayFont, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = nestNoteDisplayFont, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = nestNoteDisplayFont, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = nestNoteDisplayFont, fontWeight = FontWeight.Bold, letterSpacing = (-0.15).sp),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = baseTypography.bodyLarge.copy(letterSpacing = 0.15.sp, lineHeight = 26.sp),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
)

// Warm "paper and terracotta" palette used when wallpaper colours are off or unavailable.
internal val nestNoteLightScheme = lightColorScheme(
    primary = Color(0xFFC4552D), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0), onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF77574E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD0), onSecondaryContainer = Color(0xFF2C150F),
    background = Color(0xFFF6F1EA), onBackground = Color(0xFF26221F),
    surface = Color(0xFFFFFBF7), onSurface = Color(0xFF26221F),
    surfaceVariant = Color(0xFFEFE5DD), onSurfaceVariant = Color(0xFF6B625B),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBF5EF),
    surfaceContainer = Color(0xFFF6EFE8), surfaceContainerHigh = Color(0xFFF0E8E0),
    outline = Color(0xFFA89E94), outlineVariant = Color(0xFFE7DFD5)
)
internal val nestNoteDarkScheme = darkColorScheme(
    primary = Color(0xFFFF8A5C), onPrimary = Color(0xFF2A140B),
    primaryContainer = Color(0xFF5A2A18), onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE7BDB1), onSecondary = Color(0xFF442A22),
    secondaryContainer = Color(0xFF5D4037), onSecondaryContainer = Color(0xFFFFDBD0),
    background = Color(0xFF171412), onBackground = Color(0xFFF2EBE3),
    surface = Color(0xFF1B1714), onSurface = Color(0xFFF2EBE3),
    surfaceVariant = Color(0xFF342E29), onSurfaceVariant = Color(0xFFC3B8AD),
    surfaceContainerLowest = Color(0xFF110E0C), surfaceContainerLow = Color(0xFF221E1B),
    surfaceContainer = Color(0xFF27221F), surfaceContainerHigh = Color(0xFF302A26),
    outline = Color(0xFF8F857B), outlineVariant = Color(0xFF3F3832)
)

/** Pure-black surfaces for OLED screens. Applied on top of any dark scheme. */
internal fun ColorScheme.asAmoled(): ColorScheme = copy(
    background = Color.Black, surface = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212), surfaceContainerHigh = Color(0xFF1A1A1A)
)

/** Shared motion tokens so every screen animates with the same feel. */
internal object NestMotion {
    fun <T> spatial() = spring<T>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    fun <T> fast() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}

// Distinct, slightly richer swatches remain soft while being easy to tell apart.
internal val noteAccents = listOf(
    Color(0xFFF4EEE3), Color(0xFFE88C98), Color(0xFFE98555),
    Color(0xFFD8C34D), Color(0xFFA6CC91), Color(0xFF69B9B3), Color(0xFF73ACD8)
)
internal val noteTints = listOf(
    Color(0xFFFFFCF6), Color(0xFFFFE8E9), Color(0xFFFFE8DC),
    Color(0xFFFFF6CC), Color(0xFFEBF5E5), Color(0xFFE0F4F1), Color(0xFFE5F1FC)
)
