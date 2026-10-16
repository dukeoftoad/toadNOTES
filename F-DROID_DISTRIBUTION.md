# F-Droid distribution readiness

Status: **audit in progress; not ready for submission or repository publication**.

## Intended outcome

Make NESTNOTE installable and updateable through an F-Droid-compatible source without replacing or weakening the existing GitHub Releases process.

- Upstream source: https://github.com/dukeoftoad/toadNOTES
- Upstream releases: https://github.com/dukeoftoad/toadNOTES/releases
- Android application ID: `com.nestnote.app`
- Namespace: `com.nestnote.app`
- Minimum SDK: API 26
- Target/compile SDK: API 36
- Java/Kotlin target: Java 17
- Android Gradle Plugin: 8.10.1
- Kotlin: 2.1.20
- CI Gradle version: 8.13
- Current Gradle dependencies use Google Maven and Maven Central; plugin resolution also uses Gradle Plugin Portal.

## Phase 1 — repository audit findings

### Confirmed from source

- `app/build.gradle.kts` defines package ID, SDK levels, Java 17, and app dependencies.
- `.github/workflows/android-ci.yml` runs JVM unit tests, builds debug and minified release APKs, and compiles instrumentation tests on PRs and pushes to `main`.
- `.github/workflows/android-release.yml` publishes release APKs for version tags matching `v*.*.*`; signing uses GitHub Actions secrets.
- `app/src/main/AndroidManifest.xml` declares a launcher activity and no explicit permissions.
- `android:allowBackup="false"` is set.
- README describes local encrypted note payloads using AES-GCM and Android Keystore, and states that the app has no account/cloud sync.
- README describes production APKs as signed by the permanent production key.

### Blocking questions / required fixes before submission

1. **License file added; rights review remains.** The owner selected GPL-3.0-only and the root `LICENSE` now contains the complete canonical license text. Before submission, confirm all copyright holders have authority to license the project under GPL-3.0-only. Do not treat third-party source headers as the app's license.
2. **Release build reproducibility is not yet demonstrated.** Run a clean build from the exact source tag using the build recipe selected for F-Droid and record the result.
3. **Dependency/source review is incomplete.** Audit all direct and transitive dependencies, embedded/vendored code, telemetry, network behavior, and any non-free build components against the current F-Droid inclusion policy.
4. **Versioning must be made deterministic for source builds.** The Gradle defaults are versionCode 1/versionName 1.0.0 unless properties are supplied. Ensure each tagged release maps to unique, correctly increasing values in the F-Droid build recipe.
5. **Signing model must remain separate.** F-Droid builds/signs its own distributed APKs. Do not expose the upstream production keystore or require upstream signing secrets for a clean source build. Do not reuse an upstream-signed APK as a substitute for the F-Droid build.
6. **Repository/listing status must be confirmed.** No existing official F-Droid listing or metadata entry was established by this repository-only audit.

## Phase 2 — metadata preparation checklist

A metadata draft now exists in `F-DROID_METADATA_PROPOSAL.md`. Do not submit it until the license-rights check, dependency review, and F-Droid build validation are complete. The likely metadata fields to prepare are:

- package ID: `com.nestnote.app`
- source code URL: `https://github.com/dukeoftoad/toadNOTES`
- issue tracker: `https://github.com/dukeoftoad/toadNOTES/issues`
- changelog/release information: GitHub Releases
- license: `GPL-3.0-only` (owner-selected; confirm contributor rights and include canonical license text before submission)
- build recipe: pin an upstream version tag and use the supported Gradle/Android SDK build requirements
- categories, summary, description, author/contact, and current version metadata: verify and populate from authoritative project details before submission

Official inclusion route: prepare and validate metadata in the F-Droid data repository, then follow its review process. A GitHub project or releases URL is not itself a compatible F-Droid repository.

## Phase 3 — validation plan and current evidence

Required before marking ready:

- [ ] Clean checkout of a pinned version tag.
- [ ] Build using the exact proposed F-Droid recipe, with no private signing secrets.
- [ ] `testDebugUnitTest` passes.
- [ ] `assembleDebug` passes.
- [ ] `assembleRelease` passes.
- [ ] `assembleDebugAndroidTest` compiles.
- [ ] Confirm APK application ID and version fields.
- [ ] Install the generated APK and verify launch.
- [ ] Compare the F-Droid-built artifact to upstream where reproducible-build comparison is available.
- [ ] Record the exact commit, commands, SDK/tool versions, and logs.

**Evidence limitation:** this audit was performed by reading repository files through GitHub. No local Android SDK build, APK install, device test, reproducibility comparison, or F-Droid server/index validation was executed during this work. These checks must not be reported as passed.

## Distribution decision

Prefer official F-Droid inclusion first. Consider a self-hosted repository only if early distribution is needed and someone can operate it securely. A self-hosted repository must publish a valid F-Droid index and metadata over HTTPS and protect its repository signing key. Never ask users to add the GitHub project URL as a repository URL.
