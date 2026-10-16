# NESTNOTE F-Droid repository

This directory contains the self-hosted F-Droid repository configuration and metadata. The repository signing key must be separate from the production APK signing key.

> **Status:** Do not publish until the required Actions secrets are configured and the workflow has passed validation. This workflow republishes the repository to the `gh-pages` branch.

## 1. Generate the repository signing key once

**Important:** The keystore workflow can be run from the Actions UI only after `.github/workflows/fdroid-keystore-init.yml` is merged into the repository's default branch.

1. Open the repository's **Actions** tab.
2. Select **Generate F-Droid Repo Keystore (Run Once)** and choose **Run workflow**.
3. Download the single artifact named `fdroid-keystore-secrets`. It contains `keystore.p12`, `keystore.b64`, and `secrets.txt`. The artifact expires after one day.
4. Store a secure offline backup of the keystore and credentials. Never commit them or share them publicly.
5. Add the contents of `keystore.b64` as the repository Actions secret `FDROID_KEYSTORE_BASE64`.
6. Add the three values from `secrets.txt` as repository Actions secrets: `FDROID_KEYSTORE_PASS`, `FDROID_KEY_PASS`, and `FDROID_KEY_ALIAS`.
7. Keep this repository key stable. Do not rerun key generation for later releases.

The key-generation workflow does not print the Base64 keystore or passwords to the Actions log. The artifact is sensitive because it contains the private key and credentials; restrict access to it.

## 2. Pin the production APK signing certificate

The publishing workflow verifies the downloaded release APK against a pinned SHA-256 certificate fingerprint before creating repository metadata. This is the **production APK signing certificate**, not the F-Droid repository signing key.

1. Download a known-good APK from the official [NESTNOTE GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases) page.
2. Verify that release independently, then inspect its certificate with Android SDK Build Tools:

   ```sh
   apksigner verify --verbose --print-certs NESTNOTE-v1.2.4.apk
   ```

3. Confirm the certificate belongs to NESTNOTE's permanent production signing key using your trusted release records. Do not blindly trust a fingerprint from an unverified download.
4. Add the certificate's **SHA-256 digest** as the repository Actions secret `FDROID_ALLOWED_APK_CERT_SHA256`. The workflow accepts the 64 hexadecimal digits with or without colons or hyphens.
5. The workflow checks each release APK against this secret. Only after the check succeeds does it inject that fingerprint into the generated metadata's `AllowedAPKSigningKeys` field. The committed metadata intentionally does not contain a guessed or unverified fingerprint.

If the fingerprint is missing or does not match, publishing fails closed.

## 3. Required GitHub Actions secrets

Configure all five repository Actions secrets:

- `FDROID_KEYSTORE_BASE64`: Base64-encoded dedicated F-Droid repository keystore
- `FDROID_KEYSTORE_PASS`: repository keystore password
- `FDROID_KEY_ALIAS`: repository signing key alias
- `FDROID_KEY_PASS`: repository signing key password
- `FDROID_ALLOWED_APK_CERT_SHA256`: independently verified SHA-256 fingerprint of the production APK signing certificate

The publishing workflow decodes the repository keystore into `fdroid/keystore.p12`, substitutes repository-key credentials into `fdroid/config.yml` at runtime, and removes the keystore from the working tree before preparing public files. Private credentials and the keystore must never be committed or published.

## 4. What the publishing workflow validates

For a release such as `v1.2.4`, the workflow:

1. Downloads the release asset named `NESTNOTE-v1.2.4.apk`.
2. Verifies the APK signature and checks that the application ID is `com.nestnote.app`.
3. Reads the APK's actual numeric `versionCode` and checks that `versionName` matches the release tag.
4. Compares the APK signing certificate fingerprint with `FDROID_ALLOWED_APK_CERT_SHA256`.
5. Names the repository APK using the actual manifest version code: `com.nestnote.app_<versionCode>.apk`.
6. Generates the F-Droid index from the `fdroid/` directory, where `config.yml`, `metadata/`, and `repo/` are located, and checks that an index and the APK exist before publication.
7. Publishes generated public repository files only; the config, metadata, and private keystore are not copied to the published directory.

## 5. Run and verify

After the workflow file is merged into the default branch and all five secrets are configured, publish a new GitHub Release to trigger the workflow. A manual run uses the workflow's current ref/tag, so ensure it points to a valid published release.

After a successful run, verify the public repository index and APK URLs from a clean client/device before sharing the repository URL. A successful GitHub Actions run alone is not proof that an F-Droid client can add the repository or install and update the app.

**Important limitation:** this is a self-hosted repository of upstream-signed release APKs, not an official F-Droid listing. Official F-Droid inclusion uses its own source-build and review process.
