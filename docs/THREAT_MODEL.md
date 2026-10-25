# NESTNOTE Threat Model

Last updated: 2026-10-24

> Implementation status (source audit, 2026-10-24): `DraftStore` uses schema-v2 envelopes with explicit `privateNote` classification. Private drafts use AES-GCM with a dedicated Android Keystore key and purpose-bound AAD; missing keys, malformed/authentication-failed envelopes, and classification mismatches are rejected without plaintext fallback. Legacy plaintext/unknown envelopes are rejected and may be deleted when discovered. This source-level assessment is not a substitute for exact-head CI and device/process-death validation.

## Scope and assets

This threat model covers local note confidentiality, the Private Vault UI gate, private-note filtering, screenshot protection, lifecycle behavior, and password-protected encrypted archives.

Assets include note titles, bodies and metadata; encrypted local note payloads and the Android Keystore-managed storage key; vault password-verification data and persisted backoff state; password-protected encrypted archives; and private content rendered in the editor, lists, search results, or Recents/screenshot surfaces.

## Critical distinction: UI lock vs cryptographic isolation

**Private Vault has a separate encrypted private-record store in the current source, in addition to its UI access gate.**

The Private Vault uses a random vault data key for private records. The vault password derives a wrapping key with PBKDF2-HMAC-SHA256 and AES-GCM unwraps that data key; the ordinary `NoteStore` key cannot decrypt private records. The biometric path separately wraps the existing vault data key using an authentication-bound Android Keystore key and a successful `BiometricPrompt.CryptoObject` cipher. These are source-level findings; exact-head CI and device validation are still required.

| Property | Current Private Vault | What separate cryptographic isolation would require |
| --- | --- | --- |
| Restricts normal in-app navigation | Password / optional biometric UI gate | May also need authenticated UI |
| Relocks and hides protected UI | Intended lifecycle behavior; must be tested | Still needed, but is not encryption |
| Independent encryption key for private notes | **Yes, a separate private data key and encrypted record store are implemented; lifecycle/recovery still require validation** | Continue reviewing authorization, rotation, recovery, and all access pathways |
| Vault password is an encryption key | **Not directly; it participates in the private-key wrapping/unlock protocol** | Review KDF parameters, wrapping, rotation, and recovery; never store the raw password |

| Portable archive encryption | **Yes** | AES-GCM authenticated archive encryption; Private Vault notes are excluded |


## Portability policy

Plaintext JSON export/import and scheduled plaintext JSON backups are not supported user-facing features. JSON remains an internal serialization format for local persistence and the payload inside password-protected encrypted archives. Portable archives use AES-GCM authentication and exclude Private Vault notes and voice attachments. Historical JSON files users created with older app versions are not automatically located or deleted by this change.

## Controls and residual risks

- **Local storage:** AES-GCM with an Android Keystore-managed key protects encrypted payloads at rest while the key remains available and protected. It does not protect content after decryption in memory.
- **UI authentication and cryptographic authorization:** password unlock unwraps the independent vault data key; enrolled biometric unlock is intended to unwrap the same key only through the authenticated CryptoObject path. UI bypass can still expose content while an authorized session is live, and process compromise remains outside the app's guarantees.
- **Filtering:** the private flag is application metadata. Ordinary-view filtering must cover Home, categories, pinned, archived, search, insights, counts, links, and restored data. Filtering is not cryptographic access control.
- **Screenshot protection:** `FLAG_SECURE` requests capture protection for the protected window. It does not guarantee protection against external cameras, a compromised OS, every OEM path, or content already exported.
- **Lifecycle:** backgrounding should relock and hide private editor UI. Activity/process recreation and saveable editor state require dedicated tests.

- **Archives:** portable archives are password-protected and AES-GCM encrypted. Archive selection excludes Private Vault notes, including archived private notes. The serialized payload is only intended to be consumed inside the encrypted archive flow.
- **Drafts:** schema-v2 envelopes persist an explicit privacy classification. Private draft payloads are AES-GCM encrypted with a dedicated Android Keystore key and purpose-bound AAD; missing keys, invalid tags, malformed envelopes, and classification mismatches fail closed. Legacy plaintext/unknown envelopes are rejected rather than reclassified. Exact-head regression and device/process-death validation remain required.
- **Lockout:** persisted cooldown/backoff slows repeated attempts through the app flow. Restart, boundary, maximum delay, and clock-change behavior require tests.
- **Biometrics:** cancellation, failure, unavailable hardware, enrollment changes, and fallback must never grant access except after explicit successful authentication.
- **Key loss:** invalidation or loss of the local-storage key may make encrypted data unrecoverable. Recovery behavior must preserve original bytes and be tested.

## Trust boundaries

- **App UI boundary:** authentication, navigation, editor visibility and filtering. It is not a cryptographic boundary.
- **Local-storage boundary:** encrypted payloads protected by Android Keystore while the key remains protected.
- **Archive boundary:** archive files remain encrypted at rest outside NESTNOTE, but password disclosure or weak passwords can compromise confidentiality. Voice attachments and Private Vault notes are not included.
- **OS boundary:** Android lifecycle, biometric APIs, screenshot restrictions and Keystore are trusted platform services. A compromised OS is outside these controls' guarantees.

## Out of scope / not guaranteed

No claim is made to protect against a rooted or otherwise compromised device, process instrumentation, a maliciously modified app, external-camera capture, content deliberately shared/copied by the user, or irreversible loss of the Keystore key.

## Release decision and required wording

Use this statement:


> Private Vault uses a password gate and a separate encrypted private-record store; biometric enrollment and automatic unlock are wired in source but remain pending exact-head CI and device validation. Private-note drafts are separately encrypted and classified. Portable archives are password-protected and exclude Private Vault notes.


Do not claim that portable archives include Private Vault notes or voice attachments.

Before release, verify lifecycle and activity/process recreation, biometric outcomes, screenshot/Recents behavior, encrypted archive round trips, all ordinary note-derived surfaces, lockout boundaries and clock changes. Source inspection or a successful build alone does not establish device behavior.

## Future cryptographic-isolation review questions

1. What independent key encrypts private-note payloads, and how is it separated from the ordinary storage key?
2. How is key use authorized after password or biometric authentication without storing the raw password?
3. How do password changes, biometric enrollment changes, key rotation, and recovery work?
4. What happens on migration failure or key loss, and how is existing data preserved?
5. Are portable archives encrypted and is their content scope clearly disclosed?
6. Which automated and device tests demonstrate each security claim?

The isolated-store integration is now wired into the app's password unlock, load, save, migration, and lock paths on branch `feat/private-vault-crypto-isolation`. While locked, the app's active note list contains only public notes; existing private records remain in the old general encrypted blob until a successful password unlock migrates them. Migration commits the isolated encrypted private store before replacing the general store with public notes only. If either step fails, unlock is denied and the legacy encrypted blob is retained where possible.

Private records use a separate random vault data key wrapped by a PBKDF2-HMAC-SHA256-derived AES key, and record ciphertext is authenticated with record-ID-bound AES-GCM AAD. The general NoteStore key cannot directly decrypt the isolated record payloads. This is cryptographic isolation at the record-store layer, not a claim that every app pathway has been fully audited. Portable encrypted archive selection excludes private notes. Linked-note chips and backlinks are filtered to the same privacy class so public notes do not reveal private-note titles while the vault is open. Migration keeps the original encrypted general-store copy until encrypted private records are committed and the public-only general store is successfully committed; a failed second phase leaves the legacy copy for retry. Cross-store crash recovery and password-change failure atomicity still require dedicated fault-injection and device testing.

Biometric key wrapping and the BiometricPrompt CryptoObject path are present in source, but the setting/gate remains subject to device-level validation. Do not claim biometric unlock is release-ready until enrollment, unlock, cancellation, failure, enrollment-change/key-invalidation, and fallback tests pass. A successful biometric UI callback must never be treated as cryptographic authorization. Do not merge/release until exact-head CI and security review pass.


## Draft confidentiality boundary

Drafts are stored separately from encrypted note records. Schema-v2 draft envelopes persist an explicit `privateNote` classification. Private drafts use AES-GCM with a dedicated Android Keystore key and purpose-bound AAD; a missing key, invalid tag, malformed envelope, or classification/envelope mismatch rejects recovery without a plaintext fallback. Ordinary orphan discovery excludes private drafts. Legacy unversioned/schema-v1 plaintext draft files are not migrated or assumed public; they are rejected and deleted when discovered. Deletion on flash storage is not a guaranteed forensic secure erase, so this cleanup is risk reduction rather than a secure-erasure claim.

A private draft is still protected by the app/device security boundary, not a separate vault password-derived key. Access to an existing private draft must occur only through the authenticated Private Vault editor path. Device-level compromise remains out of scope.


## 2026-10-23 — Biometric key binding and remaining validation

The optional biometric wrapping primitive now uses a dedicated Android Keystore AES-GCM key with per-use authentication required and invalidation on biometric enrollment changes. The existing random vault data key is wrapped only after an authenticated enrollment `Cipher` is supplied; unlocking requires the `Cipher` returned by a successful `BiometricPrompt.CryptoObject` result. A plain biometric-success callback cannot unwrap the vault key.

Biometric enrollment and automatic unlock are wired in the current PR source. This does not establish device-level correctness: enrollment, successful unlock, cancellation-to-password fallback, biometric enrollment changes/key invalidation, and physical-device/emulator validation remain release gates. Do not claim release readiness until those checks and exact-head CI pass.

Password-change recovery is explicitly coordinated and unit-tested for key-rewrap failure, verifier-update failure with successful rollback, and rollback failure that locks the vault and marks recovery required. Migration tests simulate the store-commit/legacy-cleanup boundary with new store instances, but do not yet kill and restart the Android process at that exact point. Draft tests simulate store recreation, not an OS process kill. True process termination, activity recreation, backup restoration plus private-draft recovery, and biometric hardware/key invalidation must be validated on an Android emulator/device before release.
