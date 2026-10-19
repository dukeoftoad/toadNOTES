# Ultra-slim right-side rail panel

`SlimRailPanel` is a reusable Jetpack Compose right-edge panel with a 24 dp visible edge affordance and a minimum 48 dp accessible hit target. It is intentionally state-hoisted: callers own a `SlimRailState` and supply the panel content.

## Usage

```kotlin
val railState = rememberSlimRailState()

Box(Modifier.fillMaxSize()) {
    AppContent()

    SlimRailPanel(
        state = railState,
        contentDescription = "Appearance panel"
    ) {
        AppearanceTools(
            onClose = { scope.launch { railState.close() } }
        )
    }
}
```

## Behavior and implementation

- `SlimRailState` exposes open, close, and toggle operations and the current/target rail values.
- `AnchoredDraggableState` snaps between `RailValue.Closed` and `RailValue.Open`; the velocity threshold and positional threshold provide fling- and distance-based settling.
- `Modifier.slimRailLayout` measures the actual panel width and recalculates anchors from that measurement, so changed window sizes and densities do not leave stale offsets.
- `PredictiveBackHandler` maps system-back progress to panel movement and restores the open state if the gesture is cancelled.
- The panel applies `WindowInsets.safeDrawing` and uses a spring with medium-bouncy damping for expressive motion.
- The edge grip is visually 24 dp wide while the semantic/clickable target is at least 48 dp and merges descendants for TalkBack.
- A low-alpha canvas detail adds subtle texture without requiring image assets.

## Manual verification checklist

- [ ] Confirm the visible rail is 24 dp and the interactive target is at least 48 dp.
- [ ] Swipe to open and close; release around the midpoint and verify it snaps to the nearest anchor.
- [ ] Fling the rail in both directions and verify velocity-based settling.
- [ ] On Android 13+, test predictive back while open, including cancelling the gesture.
- [ ] Enable TalkBack and confirm the handle is announced as one actionable control.
- [ ] Check edge-to-edge behavior, rotation, split-screen, large fonts, and light/dark themes.
- [ ] Run the Android unit and instrumentation test suites and verify the app builds.

The component is additive and is not wired into a specific screen by default; consumers opt in by placing it in a full-screen `Box`.
