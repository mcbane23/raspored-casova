# Raspored časova (Class Schedule)

An Android app for following a child's class schedule, the school calendar (holidays and breaks) and a parent
notification when classes end for the day. It supports separate morning and afternoon shifts. The app's UI is in
Serbian.

## Features

- **Danas (Today):** the selected day's classes with bell times and the current shift. Tapping a class changes or
  cancels it for that day only (e.g. Serbian instead of English, or no PE), and a class can be added for that day
  only. Changes for past days are removed automatically.
- **Nedelja (Week):** the regular Monday–Friday timetable, kept separately for the morning and afternoon shift.
- **Podešavanja (Settings):** multiple children (one by default), school, shift rotation (always morning, always
  afternoon, alternating weekly or monthly, with a choice of which shift is current) and bell times for each shift.

Data is stored locally on the phone (`data.json` in the app's private storage).

## Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew bundleRelease     # release AAB (unsigned unless a signing key is configured)
```

Requires JDK 17 and the Android SDK (API 36).

## Automatic releases

Every push/merge to `main` runs `.github/workflows/build-release.yml`, which:

1. builds a debug APK and a signed release AAB (the AAB only when the signing secrets are set, see below),
2. publishes a GitHub Release `v1.0.<build number>` with those files,
3. deletes older releases (and their tags) so only the latest 3 remain.

Pull requests run the same build without signing or publishing. Build outputs are never stored in the repository or
as Actions artifacts.

### Signing keys

The **debug APK** is signed with `app/debug.keystore`, which is committed to the repository (it is not secret; the
password is `android`). This keeps the signature stable, so each new debug APK installs over the previous one. The
debug build has the ID `com.bane23.classschedule.debug`, so it can be installed next to the release version.

The **release AAB** is signed with your upload key, which is kept only in GitHub secrets.

1. Create a keystore (once, on your computer; requires a JDK):

   ```bash
   keytool -genkeypair -v -keystore upload.jks -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

   Remember the keystore password and the key password (they can be the same). Keep `upload.jks` somewhere safe
   outside the repository; without it you cannot upload new versions until Google resets the upload key.

2. Encode the keystore as base64 text:

   ```bash
   # Linux
   base64 -w0 upload.jks > upload.jks.b64
   # macOS
   base64 -i upload.jks -o upload.jks.b64
   # Windows (PowerShell)
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("upload.jks")) | Out-File upload.jks.b64 -NoNewline
   ```

3. On GitHub, open the repository → **Settings → Secrets and variables → Actions → New repository secret** and add
   four secrets:

   | Secret | Value |
   | --- | --- |
   | `SIGNING_KEYSTORE_BASE64` | the full contents of `upload.jks.b64` |
   | `SIGNING_STORE_PASSWORD` | the keystore password |
   | `SIGNING_KEY_ALIAS` | the key alias (`upload` in the command above) |
   | `SIGNING_KEY_PASSWORD` | the key password |

4. Delete `upload.jks.b64` from your computer.

When publishing to Google Play for the first time, enable **Play App Signing**; Google then holds the app signing
key and this key is used only for uploads.
