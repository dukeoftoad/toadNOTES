# NESTNOTE UI/UX Rules — Design Contract

**Status:** Mandatory for all UI work, reviews, and new features.  
**Product principle:** *Same app. Better experience. One design system.*

This document is the source of truth for UI/UX decisions. If a proposed implementation conflicts with these rules, follow this document or explicitly revise it through a reviewed design decision before coding.

## 1. Priority and sequence

1. **P0 — Note creation and editing:** redesign the core editor and replace the bottom colour/appearance picker with the animated right-side Appearance panel.
2. **P1 — App-wide maturity:** audit and align every tab, menu, dialog, icon, empty state, navigation transition, and responsive state.
3. **P1 — Accessibility and reliability:** validate contrast, touch targets, text scaling, keyboard/insets, focus, screen readers, and state handling.
4. **P2 — Backend and new features:** begin only when the relevant UI patterns and reusable components are stable; extend existing components rather than duplicate them.

Do not call the app UI/UX-mature based on a few polished screens. Maturity means the full end-to-end experience is coherent and validated.

## 2. Editor and Appearance interaction contract

- The note editor is the primary writing surface. Title, body, and formatting controls must have clear hierarchy, readable spacing, and minimal distraction.
- Appearance is opened from the editor toolbar and **slides in from the right**. Do not use a bottom sheet as the primary colour/background picker.
- On phones, the Appearance panel overlays the editor so it does not permanently squeeze the writing canvas. On wider screens, it may use a wider side panel.
- Use a polished, restrained slide/fade transition with sensible duration/easing. Respect system reduced-motion/accessibility settings where supported.
- Show background/wallpaper previews, a clear colour palette, selected-state indicators, and a visible close action. Make the currently selected option unambiguous.
- Preview selections immediately in the draft. Do not persist note changes just because a swatch was tapped; use the existing note save flow.
- Define and test dismissal semantics: close icon, outside tap, and system Back must behave consistently. If appearance edits are draft-only, offer predictable Apply/Cancel behavior and discard only the uncommitted appearance draft when cancelled.
- Keep the editor's existing save, cancel, unsaved-change confirmation, stale-draft protection, pin, reminder, archive, delete, share, and copy behavior intact.
- New-note and edit-note flows must reuse the same editor and Appearance components.

## 3. Reuse and single source of truth

- Before adding a component, search the codebase for an existing equivalent and extend it where possible.
- Use one reusable implementation for shared controls, colour/background selection, navigation destinations, empty states, dialogs, and settings patterns.
- Do not create duplicate pickers, parallel persistence paths, competing design tokens, or feature-specific copies of an existing component.
- UI state must flow through the existing note model and save/persistence boundary. Keep preview-only state separate from persisted note state until the user saves.
- Keep design tokens and styling aligned with Material 3 and the app theme; avoid hard-coded colours that break light/dark contrast unless they are intentional content artwork.

## 4. Whole-app consistency

- Every screen should have a clear title, primary action, secondary actions, and predictable back/dismiss behavior.
- Use consistent spacing, typography, shapes, icon size, elevation, motion, and component states.
- Icons must be recognizable and have meaningful accessibility descriptions; decorative icons should not create redundant announcements.
- Empty, loading, error, selected, disabled, and success states must be intentionally designed—not left as raw placeholders.
- Controls must work. Do not add decorative/inert actions; if functionality is deferred, label the limitation clearly.
- Preserve the current navigation architecture (Home, All notes, Categories, Reminders, More, Settings; Archive and Pinned under More unless an approved roadmap change says otherwise).

## 5. Responsive and accessible by default

- Support narrow phones, landscape, larger font/display scaling, and keyboard-visible editor states without clipped or unreachable actions.
- Respect window/system insets and keep important actions reachable while the keyboard is open.
- Maintain readable contrast in light and dark themes and in every note colour/background combination.
- Use adequate touch targets, semantic labels, selected/checked state semantics, and predictable focus order.
- Prefer reduced or non-essential motion to be optional when accessibility settings request it.
- Manual device/TalkBack checks must be recorded as manual checks; compilation is not proof of accessibility.

## 6. Required workflow for every UI change

1. **Audit:** identify the current component, its callers, state owner, persistence path, related settings, and existing tests.
2. **Reference:** compare the change against this contract and the approved prototype in ROADMAP.md. Use screenshots/design references where useful; do not copy visual assets without rights.
3. **Plan:** list affected screens, interactions, edge cases, and acceptance criteria before broad changes.
4. **Implement:** reuse existing components and tokens; keep the diff focused and avoid unrelated refactors.
5. **Verify:** run relevant unit tests and build checks; test create/edit, keyboard/insets, back/dismiss, save/cancel, and light/dark states as applicable.
6. **Review:** inspect the actual UI at narrow and larger sizes; validate semantics and touch targets. Record device checks only if actually performed.
7. **Document:** update ROADMAP.md for status/priority changes and WORKLOG.md with commit, tests, CI URL/result, and known limitations.
8. **Merge gate:** do not merge while required CI is pending/failing or while P0 acceptance criteria are unmet. Do not claim implementation, visual QA, or device testing that has not occurred.

## 7. P0 acceptance checklist

- [ ] Right-side Appearance panel replaces the bottom appearance picker as the primary interaction.
- [ ] Panel animates in/out and behaves correctly on phone-sized layouts.
- [ ] Colour and background previews, selected states, close and cancellation behavior are clear.
- [ ] Create and edit flows share the same polished editor/components.
- [ ] Title/body editing, formatting, category, pin, reminder, archive, share/copy, save and discard still work.
- [ ] Appearance is saved only through the intended note-save path; cancel does not accidentally persist a preview.
- [ ] Keyboard, insets, back handling, large text, light/dark theme and accessibility semantics are checked.
- [ ] Relevant automated tests and CI pass on the exact final commit.
- [ ] Manual/device checks and any untested cases are accurately documented.

## 8. Prototype reference

The approved concept is the editor with an **animated right-side Appearance panel**, wallpaper thumbnails, a selected colour palette, live draft preview, theme options, and a clear Apply/Cancel interaction. The panel overlays the editor on phones. Treat the visual concept as direction, not as evidence that the UI is already implemented or tested.

When in doubt: **polish the core flow, reuse the system, validate before expanding.**
