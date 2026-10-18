# NESTNOTE F-Droid repository

This directory contains the self-hosted F-Droid repository configuration and metadata. The repository signing key must be separate from the production APK signing key.

## Publishing policy

The F-Droid publishing workflow runs automatically for **every published stable SemVer GitHub Release** tagged `vX.Y.Z`, including patch and minor releases such as `v1.2.5`. Prereleases (tags containing a hyphen) are rejected by validation. To publish or republish an already-existing release, open **Actions → NESTNOTE F-Droid Repository → Run workflow**, select the default branch, and enter the exact release tag (for example `v1.2.5`). Manual runs validate the release APK and fail closed if required signing secrets are missing or checks fail.

## 1. Generate the repository signing key once

1. Open the repository's **Actions** tab.
2. Select **Generate F-Droid Repo Keystore (Run Once)** and choose **Run workflow**.
3. Download the artifact named `fdroid-keystore-secrets`. It contains the repository keystore and credentials and expires after one day.
4. Store a secure offline backup. Never commit the keystore or share its credentials publicly.
5. Add the Base64 keystore as the repository Actions secret `FDROID_KEYSTORE_BASE64`.
6. Add the credentials as repository Actions secrets `FDROID_KEYSTORE_PASS`, `FDROID_KEY_ALIAS`, and `FDROID_KEY_PASS`.
7. Keep this repository key stable. Do not regenerate it for later releases.

## 2. Pin the production APK signing certificate

The publishing workflow verifies the downloaded release APK against a pinned SHA-256 certificate fingerprint before creating repository metadata. This is the **production APK signing certificate**, not the F-Droid repository signing key.

1. Download a known-good APK from the official [NESTNOTE GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases) page.
2. Verify the release independently and inspect its certificate using Android SDK Build Tools:

   ```sh
   apksigner verify --verbose --print-certs NESTNOTE-v1.2.4.apk
   ```

3. Confirm that certificate belongs to NESTNOTE's permanent production signing key using trusted release records.
4. Add the certificate's SHA-256 digest as the repository Actions secret `FDROID_ALLOWED_APK_CERT_SHA256`.

If the fingerprint is missing or does not match, publishing fails closed.

## 3. What publishing validates

For a qualifying release, the workflow downloads `NESTNOTE-vX.Y.Z.apk`, verifies the APK signature, checks application ID `com.nestnote.app`, confirms `versionName` matches the release tag, and checks the pinned production certificate. It then generates the F-Droid index and publishes only generated public repository files to `gh-pages`; private signing credentials and the keystore are not published.

After a successful run, verify the index and APK URLs from a clean client/device. A successful GitHub Actions run alone does not prove the F-Droid client can add the repository or install/update the app.

**Note:** This is a self-hosted F-Droid repository of upstream-signed release APKs, not an official F-Droid listing. Official F-Droid inclusion uses its own source-build and review process.


## Branding assets

The repository includes a reusable NESTNOTE icon at [`icon.svg`](icon.svg). The website uses shared SVG assets for its favicon, hero illustration, and social preview, plus a web app manifest. The F-Droid repository configuration references the matching SVG repository icon so the generated index can display the project branding without storing secrets or release credentials in the public tree.
