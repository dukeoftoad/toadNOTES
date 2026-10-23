# NESTNOTE Threat Model

Last updated: 2026-10-23

> Audit note: ordinary-view filters and the user-facing export selector exclude private notes, but private editor drafts are currently persisted separately by `DraftStore` as plaintext UTF-8/Properties files. Those draft files are inside app-private storage, but they do not inherit `NoteStore`'s AES-GCM encryption. Private draft encryption/classification and safe orphan recovery remain a release-blocking follow-up.

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

- **Backups:** the current user-facing plaintext JSON export selector excludes private notes (including archived private notes). The backup serializer itself is not a security boundary: callers that pass private notes directly to `encodeBackup` can produce readable private-note titles and bodies. The flag is not encryption; anyone who obtains such a file can read it.
- **Drafts:** `DraftStore` persists editor draft title/body and metadata in plaintext files. The files live in app-private storage, but are not encrypted by `NoteStore`; a private-note draft may therefore remain readable to an attacker with access to app-private files or a device/runtime compromise. Drafts currently do not persist a private/public classification, so orphan recovery cannot reliably apply the vault boundary. Encrypt and classify private drafts, or prevent them from being written in plaintext, before release.
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

The isolated-store integration is now wired into the app's password unlock, load, save, migration, and lock paths on branch `feat/private-vault-crypto-isolation`. While locked, the app's active note list contains only public notes; existing private records remain in the old general encrypted blob until a successful password unlock migrates them. Migration commits the isolated encrypted private store before replacing the general store with public notes only. If either step fails, unlock is denied and the legacy encrypted blob is retained where possible.

Private records use a separate random vault data key wrapped by a PBKDF2-HMAC-SHA256-derived AES key, and record ciphertext is authenticated with record-ID-bound AES-GCM AAD. The general NoteStore key cannot directly decrypt the isolated record payloads. This is cryptographic isolation at the record-store layer, not a claim that every app pathway has been fully audited. Plaintext backup export excludes private notes. Linked-note chips and backlinks are filtered to the same privacy class so public notes do not reveal private-note titles while the vault is open. Migration keeps the original encrypted general-store copy until encrypted private records are committed and the public-only general store is successfully committed; a failed second phase leaves the legacy copy for retry. Cross-store crash recovery and password-change failure atomicity still require dedicated fault-injection and device testing.

Biometric unlock remains deliberately disabled in the gate: no authentication-bound Android Keystore key plus BiometricPrompt CryptoObject flow is implemented yet. Do not re-enable the biometric setting or claim biometric support until the cryptographic unwrap path is implemented and tested. A successful biometric UI callback must never be treated as cryptographic authorization. Do not merge/release until exact-head CI and security review pass.


## Draft confidentiality boundary

Drafts are stored separately from encrypted note records. Schema-v2 draft envelopes persist an explicit `privateNote` classification. Private drafts use AES-GCM with a dedicated Android Keystore key and purpose-bound AAD; a missing key, invalid tag, malformed envelope, or classification/envelope mismatch rejects recovery without a plaintext fallback. Ordinary orphan discovery excludes private drafts. Legacy unversioned/schema-v1 plaintext draft files are not migrated or assumed public; they are rejected and deleted when discovered. Deletion on flash storage is not a guaranteed forensic secure erase, so this cleanup is risk reduction rather than a secure-erasure claim.

A private draft is still protected by the app/device security boundary, not a separate vault password-derived key. Access to an existing private draft must occur only through the authenticated Private Vault editor path. Device-level compromise remains out of scope.


## 2026-10-23 — Biometric key binding and remaining validation

The optional biometric wrapping primitive now uses a dedicated Android Keystore AES-GCM key with per-use authentication required and invalidation on biometric enrollment changes. The existing random vault data key is wrapped only after an authenticated enrollment `Cipher` is supplied; unlocking requires the `Cipher` returned by a successful `BiometricPrompt.CryptoObject` result. A plain biometric-success callback cannot unwrap the vault key.

Biometric unlock remains disabled in settings and in the vault gate. Enrollment UX, key invalidation recovery, and physical-device/emulator validation have not been completed. Do not claim the biometric feature is enabled or release-ready.

Password-change recovery is explicitly coordinated and unit-tested for key-rewrap failure, verifier-update failure with successful rollback, and rollback failure that locks the vault and marks recovery required. Migration tests simulate the store-commit/legacy-cleanup boundary with new store instances, but do not yet kill and restart the Android process at that exact point. Draft tests simulate store recreation, not an OS process kill. True process termination, activity recreation, backup restoration plus private-draft recovery, and biometric hardware/key invalidation must be validated on an Android emulator/device before release.
