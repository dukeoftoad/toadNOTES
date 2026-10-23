# NESTNOTE Threat Model

Last updated: 2026-10-23

## Scope and assets

This threat model covers local note confidentiality, the Private Vault UI gate, private-note filtering, screenshot protection, lifecycle behavior, and user-exported backups.

Assets include note titles, bodies and metadata; encrypted local note payloads and the Android Keystore-managed storage key; vault password-verification data and persisted backoff state; plaintext JSON backups; and private content rendered in the editor, lists, search results, or Recents/screenshot surfaces.

## Critical distinction: UI lock vs cryptographic isolation

**Private Vault is currently a UI access gate, not a separately encrypted vault.**

The vault password does not derive, wrap, or unlock an independent key for private notes. The password hash authenticates the user through the intended app UI flow; it is not the key encrypting note payloads. Private notes use the same general Android Keystore-backed local-storage encryption as ordinary notes.

| Property | Current Private Vault | What separate cryptographic isolation would require |
| --- | --- | --- |
| Restricts normal in-app navigation | Password / optional biometric UI gate | May also need authenticated UI |
| Relocks and hides protected UI | Intended lifecycle behavior; must be tested | Still needed, but is not encryption |
| Independent encryption key for private notes | **No** | Yes, with reviewed generation, protection/wrapping, authorization, rotation and recovery |
| Vault password is an encryption key | **No** | A reviewed password/key protocol; never store the raw password |
| Private content encrypted in exported JSON | **No** | A separately designed and verified encrypted-export format |

## Controls and residual risks

- **Local storage:** AES-GCM with an Android Keystore-managed key protects encrypted payloads at rest while the key remains available and protected. It does not protect content after decryption in memory.
- **UI authentication:** password verification and optional biometrics gate the intended navigation path. UI bypass or process compromise does not encounter a second vault-specific encryption layer.
- **Filtering:** the private flag is application metadata. Ordinary-view filtering must cover Home, categories, pinned, archived, search, insights, counts, links, and restored data. Filtering is not cryptographic access control.
- **Screenshot protection:** `FLAG_SECURE` requests capture protection for the protected window. It does not guarantee protection against external cameras, a compromised OS, every OEM path, or content already exported.
- **Lifecycle:** backgrounding should relock and hide private editor UI. Activity/process recreation and saveable editor state require dedicated tests.
- **Backups:** user-exported JSON may contain readable private-note titles and bodies. The flag is not encryption; anyone who obtains the file can read it.
- **Lockout:** persisted cooldown/backoff slows repeated attempts through the app flow. Restart, boundary, maximum delay, and clock-change behavior require tests.
- **Biometrics:** cancellation, failure, unavailable hardware, enrollment changes, and fallback must never grant access except after explicit successful authentication.
- **Key loss:** invalidation or loss of the local-storage key may make encrypted data unrecoverable. Recovery behavior must preserve original bytes and be tested.

## Trust boundaries

- **App UI boundary:** authentication, navigation, editor visibility and filtering. It is not a cryptographic boundary.
- **Local-storage boundary:** encrypted payloads protected by Android Keystore while the key remains protected.
- **Export boundary:** JSON backups leave the encrypted-storage boundary as plaintext; subsequent copies and recipients are outside the app's control.
- **OS boundary:** Android lifecycle, biometric APIs, screenshot restrictions and Keystore are trusted platform services. A compromised OS is outside these controls' guarantees.

## Out of scope / not guaranteed

No claim is made to protect against a rooted or otherwise compromised device, process instrumentation, a maliciously modified app, external-camera capture, plaintext backups, content deliberately shared/copied by the user, or irreversible loss of the Keystore key.

## Release decision and required wording

Use this statement:

> Private Vault adds a password/biometric gate to viewing private notes inside NESTNOTE. Private notes use NESTNOTE's general local-storage encryption, not a separate vault key. Exported JSON backups are plaintext and may include private notes.

Do not claim a separately encrypted vault, vault-password-encrypted notes, or encrypted JSON backups.

Before release, verify lifecycle and activity/process recreation, biometric outcomes, screenshot/Recents behavior, backup round trips, all ordinary note-derived surfaces, lockout boundaries and clock changes. Source inspection or a successful build alone does not establish device behavior.

## Future cryptographic-isolation review questions

1. What independent key encrypts private-note payloads, and how is it separated from the ordinary storage key?
2. How is key use authorized after password or biometric authentication without storing the raw password?
3. How do password changes, biometric enrollment changes, key rotation, and recovery work?
4. What happens on migration failure or key loss, and how is existing data preserved?
5. Are backups explicitly encrypted, or clearly disclosed as plaintext?
6. Which automated and device tests demonstrate each security claim?

Until a separately reviewed design is implemented and verified, Private Vault must be described as a UI privacy boundary layered over the app's general local-storage encryption.
