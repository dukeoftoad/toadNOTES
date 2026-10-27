package com.nestnote.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Animated title and supporting label for the currently selected app destination. */
@Composable
internal fun AppDestinationHeader(destination: String) {
    // Shared motion tokens instead of ad-hoc tweens; collapses to snap() under the
    // system "Remove animations" accessibility preference like every other transition.
    val reducedMotion = rememberSystemReducedMotion()
    Column {
        AnimatedContent(
            targetState = AppDestinationPresentation.title(destination),
            transitionSpec = {
                (fadeIn(NestMotion.fast(reducedMotion)) +
                    slideInVertically(NestMotion.spatial(reducedMotion), initialOffsetY = { it / 8 })) togetherWith
                    (fadeOut(NestMotion.fast(reducedMotion)) +
                        slideOutVertically(NestMotion.fast(reducedMotion), targetOffsetY = { -it / 10 }))
            },
            label = "tab-title-transition"
        ) { title ->
            Text(title, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
        }
        Text(
            AppDestinationPresentation.subtitle(destination),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
