# NESTNOTE test strategy

- Unit tests in app/src/test verify local note persistence and JSON recovery with Robolectric.
- End-to-end UI tests in app/src/androidTest launch the real MainActivity on an Android emulator and exercise dashboard, create/save, search, and cancel flows.
- CI in .github/workflows/android-ci.yml runs unit tests, builds a debug APK, and runs instrumentation tests on API 36.
- CD in .github/workflows/android-release.yml runs unit tests, builds a release APK on a vX.Y.Z tag, and attaches the unsigned APK to a GitHub Release.

The release workflow intentionally produces an unsigned APK. Configure Android signing secrets and a signing Gradle task before distributing a production APK through Play Store or other trusted channels.
