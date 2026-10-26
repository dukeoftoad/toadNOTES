package com.nestnote.app

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

@Composable
internal fun ExternalMarkdownPreviewDialog(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    title: String,
    markdown: String,
    zoomPercent: Int,
    onZoomPercentChange: (Int) -> Unit,
    showZoomControls: Boolean,
    onShowZoomControlsChange: (Boolean) -> Unit,
    moreMenuExpanded: Boolean,
    onMoreMenuExpandedChange: (Boolean) -> Unit,
    defaultZoomPercent: Int,
    context: Context
) {
    if (!visible) return
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = { onVisibleChange(false) },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box {
                    IconButton(onClick = { onMoreMenuExpandedChange(true) }, modifier = Modifier.testTag("external_markdown_more_button")) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Markdown viewer options")
                    }
                    DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { onMoreMenuExpandedChange(false) }) {
                        DropdownMenuItem(
                            text = { Text(if (showZoomControls) "Hide text size controls" else "Text size") },
                            leadingIcon = { Icon(Icons.Default.FormatSize, contentDescription = null) },
                            onClick = {
                                onShowZoomControlsChange(!showZoomControls)
                                onMoreMenuExpandedChange(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Use default size (${defaultZoomPercent}%)") },
                            onClick = {
                                onZoomPercentChange(defaultZoomPercent)
                                onMoreMenuExpandedChange(false)
                            }
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                AnimatedVisibility(visible = showZoomControls) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(enabled = zoomPercent > 50, onClick = { onZoomPercentChange((zoomPercent - 5).coerceAtLeast(50)) }) {
                                Text("−", style = MaterialTheme.typography.titleLarge)
                            }
                            Text(text = "${zoomPercent}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 52.dp), textAlign = TextAlign.Center)
                            IconButton(enabled = zoomPercent < 200, onClick = { onZoomPercentChange((zoomPercent + 5).coerceAtMost(200)) }) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Slider(
                            value = zoomPercent.toFloat(),
                            onValueChange = { onZoomPercentChange((it / 5f).toInt() * 5) },
                            valueRange = 50f..200f,
                            steps = 29,
                            modifier = Modifier.fillMaxWidth().testTag("external_markdown_zoom_slider")
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())
                ) {
                    AndroidView(
                        factory = { viewContext ->
                            TextView(viewContext).apply {
                                movementMethod = LinkMovementMethod.getInstance()
                                setTextColor(colors.onSurface.toArgb())
                                textSize = 16f * zoomPercent / 100f
                                setPadding(4, 4, 4, 4)
                            }
                        },
                        update = { textView ->
                            textView.text = MarkdownPreview.toSpanned(context, markdown, colors.onSurface.toArgb())
                            textView.textSize = 16f * zoomPercent / 100f
                            textView.movementMethod = LinkMovementMethod.getInstance()
                        },
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onVisibleChange(false) }) { Text("Close") } }
    )
}
