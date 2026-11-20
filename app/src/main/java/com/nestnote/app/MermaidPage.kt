package com.nestnote.app

/**
 * Builds the self-contained HTML document the mermaid WebView renders.
 *
 * The diagram source is embedded as a JavaScript string literal, so every backslash,
 * backtick, `${` sequence and `</script` occurrence must be escaped before embedding;
 * the escaping order below matters (backslashes first, the script-tag breaker last so
 * it does not get double-escaped). The page never touches the network: mermaid.min.js
 * is loaded from the app's own assets and `securityLevel: 'strict'` keeps diagram
 * labels inert, so note content cannot execute anything in the WebView.
 *
 * Pure JVM: escaping rules are regression-tested without an emulator.
 */
internal object MermaidPage {

    fun buildHtml(source: String, dark: Boolean): String {
        val theme = if (dark) "dark" else "neutral"
        val embed = source
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\\$")
            .replace("</script", "<\\/script")

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<style>
  html, body { margin: 0; padding: 0; background: transparent; }
  #diagram { padding: 2px 0; }
  #diagram svg { max-width: 100%; height: auto; }
  #fallback { display: none; white-space: pre-wrap; overflow-wrap: anywhere;
              font: 12px/1.5 monospace; opacity: 0.75; padding: 8px; margin: 0; }
</style>
</head>
<body>
<div id="diagram"></div>
<pre id="fallback"></pre>
<script src="mermaid.min.js"></script>
<script>
var source = `$embed`;
var reported = false;
function reportHeight() {
  if (reported) return;
  reported = true;
  var height = Math.ceil(document.body.getBoundingClientRect().height);
  if (window.MermaidBridge && window.MermaidBridge.reportHeight) {
    window.MermaidBridge.reportHeight(height);
  }
}
function showFallback(message) {
  document.getElementById('diagram').style.display = 'none';
  var fallback = document.getElementById('fallback');
  fallback.style.display = 'block';
  fallback.textContent = 'Diagram could not be rendered:\n' + message + '\n\n' + source;
  reportHeight();
}
try {
  mermaid.initialize({
    startOnLoad: false,
    theme: '$theme',
    securityLevel: 'strict',
    fontFamily: 'sans-serif',
    flowchart: { useMaxWidth: true },
    sequence: { useMaxWidth: true },
    gantt: { useMaxWidth: true }
  });
  mermaid.render('nestnoteDiagram', source).then(function (result) {
    document.getElementById('diagram').innerHTML = result.svg;
    reportHeight();
  }).catch(function (error) {
    showFallback(String(error && error.message ? error.message : error));
  });
} catch (error) {
  showFallback(String(error && error.message ? error.message : error));
}
</script>
</body>
</html>
""".trimIndent()
    }
}
