package com.nestnote.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Empty-state artwork: three stacked sheets of paper that gently float, with a badge
 * (search / archive / pin / reminder / add) that pops in with a spring. Everything is
 * drawn from the current Material 3 colour scheme, so it follows light/dark and wallpaper
 * palettes. With "Remove animations" enabled it is fully static.
 */
@Composable
internal fun NestEmptyIllustration(badge: ImageVector, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberSystemReducedMotion()

    // Only run the float animation when motion is allowed; the transition used to keep
    // ticking under "Remove animations" while its value was forced static.
    val phase = if (reducedMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "empty-float")
        val floatPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "empty-float-phase"
        )
        floatPhase
    }

    var badgeShown by remember(badge) { mutableStateOf(false) }
    LaunchedEffect(badge) { badgeShown = true }
    val badgeScale by animateFloatAsState(
        targetValue = if (badgeShown) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "empty-badge"
    )

    val back = colors.secondaryContainer
    val middle = colors.tertiaryContainer
    val front = colors.surfaceContainerHighest
    val outline = colors.outlineVariant
    val accent = colors.primary

    Box(modifier.size(156.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = (phase - 0.5f) * 10.dp.toPx() }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val paperW = size.width * 0.56f
            val paperH = size.height * 0.70f
            val corner = CornerRadius(14.dp.toPx())

            fun paper(angle: Float, color: Color, dx: Float) {
                rotate(angle, pivot = Offset(cx, cy)) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(cx - paperW / 2f + dx, cy - paperH / 2f),
                        size = Size(paperW, paperH),
                        cornerRadius = corner
                    )
                }
            }
            paper(-12f, back, -6.dp.toPx())
            paper(9f, middle, 6.dp.toPx())
            paper(0f, front, 0f)

            val left = cx - paperW / 2f
            val top = cy - paperH / 2f
            drawRoundRect(
                color = outline,
                topLeft = Offset(left, top),
                size = Size(paperW, paperH),
                cornerRadius = corner,
                style = Stroke(1.dp.toPx())
            )
            // Title bar + text lines.
            val pad = paperW * 0.16f
            val stroke = 5.dp.toPx()
            val lineStartX = left + pad
            var y = top + paperH * 0.24f
            drawLine(accent, Offset(lineStartX, y), Offset(lineStartX + paperW * 0.42f, y), stroke, StrokeCap.Round)
            val widths = floatArrayOf(0.68f, 0.56f, 0.34f)
            for (w in widths) {
                y += paperH * 0.17f
                drawLine(
                    outline,
                    Offset(lineStartX, y),
                    Offset(lineStartX + paperW * w, y),
                    stroke * 0.8f,
                    StrokeCap.Round
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-14).dp)
                .graphicsLayer {
                    scaleX = badgeScale
                    scaleY = badgeScale
                },
            shape = CircleShape,
            color = colors.primary,
            contentColor = colors.onPrimary,
            shadowElevation = 6.dp
        ) {
            Icon(badge, contentDescription = null, modifier = Modifier.padding(11.dp).size(24.dp))
        }
    }
}
