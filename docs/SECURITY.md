# NESTNOTE Security Model: Private Vault Boundaries

Last updated: 2026-10-23

## Summary

NESTNOTE uses two distinct controls that must not be confused:

- **Local-storage encryption** protects stored note payloads at rest using AES-GCM and an Android Keystore-managed key.
- **Private Vault locking** is an application-level UI access gate. The vault password is verified using a salted password hash; supported devices may offer biometric authentication. This gates the intended in-app UI flow. It does **not** derive, wrap, or unlock a separate encryption key for private notes.

Therefore, the current Private Vault is a privacy/navigation boundary, **not a cryptographically isolated vault**. Private notes use the same general local-storage encryption system as ordinary notes.

## What each control does and does not guarantee

| Control | Intended protection | Not a guarantee of |
| --- | --- | --- |
| AES-GCM + Android Keystore for local note payloads | Confidentiality and integrity of encrypted local payloads while the key remains protected | Protection after decryption, from a compromised runtime, or from plaintext exports |
| Vault password / biometric gate | Restricts access through the intended Private Vault UI flow | A second encryption layer, a separate key, or protection from every UI/control-flow bypass |
| `FLAG_SECURE` | Requests Android to block screenshots and non-secure display/Recents capture for the protected window | Universal protection against external cameras, compromised OS, all OEM capture paths, or content already copied/exported |
| Private-note filtering | Keeps flagged notes out of supported ordinary app views/searches | Cryptographic access control over raw storage, backups, logs, or unreviewed note-derived surfaces |
| Password-attempt backoff | Slows repeated attempts through the app's vault password flow | Device-wide rate limiting or resistance to a compromised process/device |

## Backup and data movement

Portable note transfer uses password-protected `.nestnote` archives with AES-GCM authenticated encryption. Private Vault notes and voice attachments are excluded from these archives. The archive password is independent of the vault password; losing it makes that archive unrecoverable.

## Threat-model assumptions and residual risks

The UI gate is intended to protect against casual access through normal app navigation. It is not a defense against an attacker who modifies the app, instruments its process, exploits the OS, or directly obtains decrypted/exported content. Lifecycle handling, `FLAG_SECURE`, and filtering reduce specific risks but require regression and device validation.

Relevant threats include:
- Casual navigation into private notes.
- A private editor remaining visible after backgrounding or activity recreation.
- Screenshots and Recents previews exposing content.
- Private notes leaking into ordinary lists, search, pinned/archived views, counts, links, or insights.
- Encrypted archive password handling and archive restore validation.
- Repeated password guessing, including lockout persistence and clock changes.
- Biometric cancellation/failure/unavailability incorrectly granting access.
- Loss or invalidation of the local-storage Keystore key.

## Required user-facing wording

Prefer:

> Private Vault adds a password/biometric gate to viewing private notes inside NESTNOTE. Private notes use NESTNOTE's general local-storage encryption, not a separate vault key. Portable encrypted archives exclude private notes.

Avoid claims such as “separately encrypted vault,” “vault-password-encrypted notes,” or “private notes remain encrypted in exported backups.”

## Future cryptographic-isolation work

Separate cryptographic isolation is a distinct security project, not a documentation toggle. At minimum, it requires a reviewed design for independent vault-key generation and protected key storage/wrapping; authorization by password/biometrics without storing the raw password; authenticated encryption; key rotation, password changes, biometric enrollment changes, and recovery; safe migration of existing private notes; explicit encrypted/plaintext backup policy; failure-safe rollback; and dedicated tests.

Until that work is implemented and verified, product copy must describe Private Vault as a UI gate over notes protected by the app's general local-storage encryption.


## Portable backup policy (2026-10-24)

Plaintext JSON export/import and scheduled plaintext JSON backups are removed. The only user-facing portable note backup is a password-protected AES-GCM `.nestnote` archive. Its scope excludes Private Vault notes and voice attachments. JSON is still used internally for local persistence and the serialized payload inside the encrypted archive; this does not make a plaintext JSON backup available. Existing JSON files created by earlier versions are not automatically found or deleted.
