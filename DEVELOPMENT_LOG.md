# NESTNOTE Development Decisions

## 2026-10-11 — Phase 1 and delivery
- API 36 is the required Android CI target.
- Emulator UI workflow is manual-only because emulator boot/UI automation has been unreliable. It is intentionally not a required CI gate.
- Phase 1 dashboard and personalization work was merged in PR #1.
- Compact grid, pastel note cards, adaptive color choices, search/filtering, and layout/sort options are part of the product direction.

## 2026-10-11 — Phase 2 editor safety
- Keep existing NoteStore and local persistence format unchanged.
- Use saveable Compose state for editor fields so drafts can survive activity recreation where Compose state restoration applies.
- Explicit close and Cancel confirm before discarding meaningful unsaved changes.
- This first increment does not prove the entire editor surface restores or that every system/sheet dismissal route is guarded. These are tracked in [ROADMAP.md](ROADMAP.md).
- Do not call Phase 2 complete until those paths are addressed and normal Android CI is green on the current head.

## Ongoing rules
- Prefer small, testable changes over broad rewrites.
- Preserve local user data and design future migrations explicitly.
- Keep the work log factual and distinguish planned, implemented, and verified behavior.
- Do not merge PRs without explicit approval from the repository owner.
