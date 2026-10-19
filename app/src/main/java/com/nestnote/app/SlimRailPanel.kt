package com.nestnote.app

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** The stable, public destinations for the slim edge rail. */
enum class RailValue {
    Closed,
    Open
}

/**
 * Hoisted state for [SlimRailPanel]. Keep one instance per rail and pass it back
 * to the panel so callers can open/close the panel from other UI.
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class SlimRailState internal constructor(
    internal val draggableState: AnchoredDraggableState<RailValue>
) {
    val currentValue: RailValue get() = draggableState.currentValue
    val targetValue: RailValue get() = draggableState.targetValue
    val isOpen: Boolean get() = currentValue == RailValue.Open

    suspend fun open() { draggableState.settle(-Float.MAX_VALUE) }
    suspend fun close() { draggableState.settle(Float.MAX_VALUE) }
    suspend fun toggle() {
        if (targetValue == RailValue.Open) close() else open()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberSlimRailState(): SlimRailState {
    val density = LocalDensity.current
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val state = remember(density, decayAnimationSpec) {
        SlimRailState(
            AnchoredDraggableState(
                initialValue = RailValue.Closed,
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = { with(density) { 125.dp.toPx() } },
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                decayAnimationSpec = decayAnimationSpec
            )
        )
    }
    return state
}

/**
 * Measures the panel and refreshes drag anchors from the measured width, so
 * density, window-size, and panel-width changes cannot leave stale anchors.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.slimRailLayout(
    state: SlimRailState,
    railWidthPx: Float
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val closedOffset = (placeable.width.toFloat() - railWidthPx).coerceAtLeast(0f)
    state.draggableState.updateAnchors(
        DraggableAnchors {
            RailValue.Closed at closedOffset
            RailValue.Open at 0f
        }
    )
    val currentOffset = state.draggableState.offset
        .takeIf { it.isFinite() }
        ?.roundToInt() ?: closedOffset.roundToInt()
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(x = currentOffset, y = 0)
    }
}

/**
 * Right-edge, custom M3-style rail/panel. The visible edge affordance is 24 dp
 * wide; its hit target is at least 48 dp. Place this inside a full-screen Box.
 *
 * Content and state are hoisted: this component does not own application data.
 * The panel applies safe-drawing insets so content remains clear of system bars.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlimRailPanel(
    state: SlimRailState,
    modifier: Modifier = Modifier,
    panelWidth: androidx.compose.ui.unit.Dp = 304.dp,
    railWidth: androidx.compose.ui.unit.Dp = 24.dp,
    contentDescription: String = "Side panel",
    panelContent: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val panelWidthPx = with(density) { panelWidth.toPx() }
    // Keep the visible rail inside the measured panel even if callers customize widths.
    val effectiveRailWidthPx = with(density) { railWidth.toPx() }.coerceIn(0f, panelWidthPx)

    PredictiveBackHandler(enabled = state.currentValue == RailValue.Open) { events ->
        var previousProgress = 0f
        try {
            events.collect { event ->
                // Apply only the progress delta; dispatchRawDelta is incremental.
                val progressDelta = event.progress - previousProgress
                state.draggableState.dispatchRawDelta((panelWidthPx - effectiveRailWidthPx) * progressDelta)
                previousProgress = event.progress
            }
            state.close()
        } catch (cancelled: CancellationException) {
            // Cancellation also cancels this handler's coroutine; restore the
            // open state outside that cancelled context.
            withContext(NonCancellable) { state.open() }
            throw cancelled
        }
    }

    Box(modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(panelWidth)
                .anchoredDraggable(
                    state = state.draggableState,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state = state.draggableState)
                )
                .slimRailLayout(state, effectiveRailWidthPx)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (state.isOpen) 3.dp else 0.dp,
            shadowElevation = if (state.isOpen) 6.dp else 0.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                val ink = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxSize()) {
                    // Quiet generative geometry stays behind content at very low alpha.
                    val unit = size.minDimension
                    drawCircle(color = ink.copy(alpha = 0.035f), radius = unit * 0.34f, center = Offset(size.width * 0.78f, size.height * 0.18f))
                    drawCircle(color = ink.copy(alpha = 0.045f), radius = unit * 0.22f, center = Offset(size.width * 0.18f, size.height * 0.82f), style = Stroke(width = 1.dp.toPx()))
                    drawLine(color = ink.copy(alpha = 0.035f), start = Offset(size.width * 0.52f, 0f), end = Offset(size.width, size.height * 0.24f), strokeWidth = 1.dp.toPx())
                }
                panelContent()
                // The visual affordance is slim; the semantics and clickable
                // target remain large enough for touch and assistive technology.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .width(48.dp)
                        .clickable {
                            scope.launch {
                                if (state.targetValue == RailValue.Open) state.close() else state.open()
                            }
                        }
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = if (state.isOpen) "Close $contentDescription" else "Open $contentDescription"
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        Modifier
                            .width(24.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f))
                    )
                    val ink = MaterialTheme.colorScheme.onSurfaceVariant
                    Canvas(Modifier.width(24.dp).fillMaxHeight(0.12f).align(Alignment.CenterStart)) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        drawLine(
                            color = ink.copy(alpha = 0.88f),
                            start = Offset(centerX - size.width * 0.12f, centerY - size.height * 0.30f),
                            end = Offset(centerX + size.width * 0.12f, centerY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = ink.copy(alpha = 0.88f),
                            start = Offset(centerX + size.width * 0.12f, centerY),
                            end = Offset(centerX - size.width * 0.12f, centerY + size.height * 0.30f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Low-alpha generative speckles keep the edge surface
                        // visually alive without competing with panel content.
                        drawCircle(
                            color = ink.copy(alpha = 0.1f),
                            radius = size.minDimension * 0.42f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
