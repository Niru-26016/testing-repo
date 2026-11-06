# Pocket PA — release runbook

## Build

The repository workflow builds:
- **PocketPA-v2-debug.apk:** for device testing; uses `com.niru.pocketpa.debug` so it can coexist with a later Play version.
- **PocketPA-v2-UNSIGNED-release.aab:** *not uploadable to Play* until signed. A successful build is not a Play Store approval.

## Create an upload key on your own trusted machine

`keytool -genkeypair -v -keystore pocketpa-upload.jks -alias pocketpa-upload -keyalg RSA -keysize 4096 -validity 10000`

Keep the `.jks` and all passwords in a secure private location. Never commit, post, or share them in chat. The repository ignores `.jks` files.

## Configure GitHub Actions (repository Secrets and variables → Actions)

- `POCKETPA_KEYSTORE_BASE64`: Base64-encoded binary contents of the key file, generated on your own machine.
- `POCKETPA_KEYSTORE_PASSWORD`: keystore password.
- `POCKETPA_KEY_ALIAS`: `pocketpa-upload` or your chosen alias.
- `POCKETPA_KEY_PASSWORD`: key password.

The workflow creates a signed AAB only if all four values are configured. Keep key ownership private. Prefer a dedicated repository before configuring secrets: secrets are repository-wide, including other workflows.

## In Google Play Console

1. Create an app with application ID `com.niru.pocketpa`.
2. Enroll in Play App Signing and create an internal testing release.
3. Upload the **signed** `PocketPA-v2-signed-release.aab` from the workflow.
4. Add screenshots from a real device, the included listing copy, 512×512 icon, 1024×500 feature graphic, and a public privacy-policy URL.
5. Fill out Data safety, app access, permissions, and content rating forms honestly.
6. Run testing and apply for production access when eligible.

Microphone use must be visible, user-initiated, necessary for live microphone functionality, and stopped on request. Test on Android 16 / API 36 before publication.
