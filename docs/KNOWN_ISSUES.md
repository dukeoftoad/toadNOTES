# NESTNOTE — Known Issues

Living document. New issues are appended as they're discovered; when fixed,
either strike through in place or move to the "Resolved" section at the bottom
with the commit/PR reference.

**Last updated:** following Phase 1 bug-fix batch.

**Current test status:** 207 unit tests, all passing.

---

## Open issues

### OI-1 — Notification permission denial is silent

**Severity:** low (UX gap)  
**File:** `app/src/main/java/com/nestnote/app/NoteEditor.kt`, `pickReminder()`

**Symptom:** When the user first opens the reminder picker on Android 13+, the
app requests `POST_NOTIFICATIONS`. If the user denies, reminders still save and
`AlarmManager` still fires at the scheduled time — but `NotificationManagerCompat.notify()`
silently no-ops (caught by the `SecurityException` handler in
`ReminderNotifications.show`). The user never learns their reminders won't be
delivered.

**Fix (future):** After the permission request completes, if the permission is
denied, show a snackbar or inline banner:

> "Reminders are saved, but notifications are off. Enable them in system settings
> to be notified."

Requires storing permission state or re-checking on editor open. Not urgent —
the OS prompt itself is visible on first open, and users who deny are making an
informed choice. Deferred to a polish pass.

---

### OI-2 — `LocalClipboardManager` deprecation

**Severity:** trivial  
**File:** `app/src/main/java/com/nestnote/app/AboutScreen.kt:104`

**Symptom:** Compile warning:
