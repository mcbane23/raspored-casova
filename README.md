# Raspored časova (Class Schedule)

An Android app for following a child's class schedule, the school calendar (holidays and breaks) and a parent
notification when classes end for the day. It supports separate morning and afternoon shifts. The app's UI is in
Serbian.

## Features

- **Danas (Today):** the selected day's classes with bell times and the current shift. Tapping a class changes or
  cancels it for that day only (e.g. Serbian instead of English, or no PE), and a class can be added for that day
  only. Changes for past days are removed automatically.
- **Nedelja (Week):** the regular Monday–Friday timetable, kept separately for the morning and afternoon shift.
- **Kalendar (Calendar):** the official Serbian primary-school calendar (breaks, days off, swapped days) with a
  countdown to the next break, plus the school's own events (e.g. Dan škole, school slava) added per child. Days
  without classes show no lessons on the Today tab.
- **Podešavanja (Settings):** multiple children (one by default), school, shift rotation (always morning, always
  afternoon, alternating weekly or monthly, with a choice of which shift is current), bell times for each shift,
  calendar region (Central Serbia or Vojvodina) and whether the child is in 8th grade (earlier end of classes).

The official calendar lives in `app/src/main/java/com/bane23/classschedule/data/SchoolCalendar.kt` (`SCHOOL_YEARS`).
Add a new `SchoolYear` there each summer once the ministry publishes the next year's calendar.

- **Subjects:** each child has an editable subject list (Serbian primary-school subjects by default); the timetable
  and one-day changes pick subjects from a drop-down list, and a subject typed by hand is added to the list.
- **Pre-class and optional classes:** each shift can have a "pretčas" before the 1st class, and any period can be
  marked as optional remedial ("Dopunska") or advanced ("Dodatna") teaching for a chosen subject, e.g. Tuesday morning
  "Dopunska: Srpski jezik", Tuesday afternoon "Dopunska: Matematika".
- **QR sharing:** Settings shows the selected child's timetable, bell times, shifts, calendar events and subjects as a
  QR code; another phone scans it and adds it as a new child or replaces an existing one. One-day changes and alarm
  settings are not shared. The code is deflate-compressed JSON (`data/ShareCodec.kt`).
- **Parent alarm:** an optional notification when the day's last class ends (or 5/10/15 minutes before), following
  the shift, the school calendar and one-day changes. It uses inexact alarms, so no exact-alarm permission is needed. By default it plays a short school-bell chime
  (`res/raw/school_bell.wav`, generated for this app); the "Zvuk alarma" switch makes it ring with the phone's alarm
  tone at alarm volume, repeating until the notification is opened or dismissed.

**Design:** pastel palettes based on [Catppuccin](https://catppuccin.com) (Latte for light, Mocha for dark), rounded cards and a colored emoji badge per subject (`ui/Components.kt`) so younger children can recognize
classes without reading. The Today tab opens with a status card (current class with a progress bar, break, or
"classes are over"), the Week tab shows one weekday at a time, and the Calendar tab leads with a countdown to the
next break. Settings has a theme choice (System, Light, Dark; System by default) and a button that sends a
test parent notification.

Data is stored locally on the phone (`data.json` in the app's private storage).

See [docs/google-play.md](docs/google-play.md) for the Play Console checklist and listing texts, and
[docs/privacy-policy.md](docs/privacy-policy.md) for the privacy policy.

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
