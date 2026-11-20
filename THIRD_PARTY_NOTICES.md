# Third-party notices

## Markleaf Android

- Project: https://github.com/jeiel85/markleaf-android
- Source revision consulted: `1bda1f6a875832088120c64fc404ed2bee844357`
- License: Apache License 2.0
- Upstream license text: [licenses/Apache-2.0-Markleaf.txt](licenses/Apache-2.0-Markleaf.txt)

NESTNOTE adapts the private-screen screenshot shielding (`FLAG_SECURE`) and persisted password-attempt backoff patterns from Markleaf's locked-notes implementation. NESTNOTE's code has been adapted to its own Compose screen, SharedPreferences-based vault preferences, and tests; Markleaf's app-specific UI and data layer were not copied wholesale.

The upstream repository's license file is preserved in this repository for attribution and license compliance.

## mermaid.js

- Bundled artifact: `app/src/main/assets/mermaid/mermaid.min.js` (version 9.4.3)
- Project: https://github.com/mermaid-js/mermaid
- License: MIT
- Upstream license text: [licenses/MIT-mermaid.txt](licenses/MIT-mermaid.txt)

The mermaid runtime is bundled into the APK so diagram rendering works fully offline. The
WebView that runs it has JavaScript enabled only for this local asset, no content or
universal file access, no JavaScript bridge beyond a height reporter, and mermaid runs with
`securityLevel: 'strict'`; note content cannot reach the network or execute anything outside
the diagram surface.
