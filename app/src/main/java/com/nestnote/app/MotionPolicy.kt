package com.nestnote.app

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user disabled system animations (the "Remove animations" accessibility
 * setting, or a developer animator duration scale of 0). Compose spring animations ignore
 * the system animator duration scale, so NESTNOTE honors the preference itself: every
 * decorative transition (rail, nav pill, editor bars, collapsible slots) settles instantly
 * while layout and behavior stay unchanged.
 *
 * Observed live, so toggling the accessibility setting takes effect without restarting.
 */
@Composable
internal fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val scaleUri = remember { Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE) }

    fun animatorScale(): Float = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)

    var reduced by remember { mutableStateOf(animatorScale() == 0f) }

    DisposableEffect(resolver, scaleUri) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                reduced = animatorScale() == 0f
            }
        }
        resolver.registerContentObserver(scaleUri, false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
