package com.nestnote.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders one mermaid diagram with the bundled offline mermaid.js runtime.
 *
 * The WebView is created only when a note actually contains a ```mermaid block (the preview
 * splits blocks first), keeps no JS bridge beyond a height reporter, runs with file access
 * to content URLs disabled, and loads everything from app assets — diagrams never touch the
 * network. The page reports its rendered height through [MermaidBridge] so the Compose
 * layout can wrap the diagram without a fixed-height guess; the report is clamped so a
 * pathological diagram can never push the preview taller than the screen.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun MermaidDiagramView(
    source: String,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val html = remember(source, dark) { MermaidPage.buildHtml(source, dark) }
    // One height holder for the WebView's lifetime: the bridge is created once inside the
    // remembered WebView and must keep writing to the state the layout actually reads.
    val height = remember { mutableIntStateOf(160) }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.databaseEnabled = false
            settings.allowContentAccess = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.loadsImagesAutomatically = true
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun reportHeight(cssHeight: Int) {
                        post {
                            height.intValue = cssHeight.coerceIn(120, 720)
                        }
                    }
                },
                "MermaidBridge"
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    AndroidView(
        factory = { webView },
        update = { view ->
            // loadDataWithBaseURL re-parses the page; skip it when the diagram did not change.
            if (view.tag != html) {
                view.tag = html
                // Reset while the next diagram measures itself, so stale height never lingers.
                height.intValue = 160
                view.loadDataWithBaseURL(
                    "file:///android_asset/mermaid/",
                    html,
                    "text/html",
                    "utf-8",
                    null
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height.intValue.dp)
            .testTag("preview_mermaid_diagram")
    )
}
