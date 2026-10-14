# Raspored časova (Class Schedule)

An Android app for following a child's class schedule, the school calendar (holidays and breaks) and a parent
notification when classes end for the day. It supports separate morning and afternoon shifts. The app's UI is in
Serbian.

## Features

- **Porodica (Family):** shown only with two or more children, and then the first tab. Every child's day on one
  screen: shift, current class or break, the day's classes as small dots, when classes end and a switch for the
  parent alarm. Tapping a child opens their Today tab.
- **Danas (Today):** a big status card (current class with a countdown ring, break, when school starts, or "done for
  today"), the day's classes as stops on a line, and the "Sutra u torbu" packing list for the next school day.
  Tapping a stop changes or cancels that class for that day only (e.g. Serbian instead of English, or no PE), and a
  class can be added for that day only. Changes for past days are removed automatically.
- **Nedelja (Week):** the regular Monday–Friday timetable, kept separately for the morning and afternoon shift, shown
  one day at a time or as a whole-week table. "Uredi" switches to editing, where each period opens a sheet of subject
  tiles.
- **Raspust (Breaks):** the official Serbian primary-school calendar (breaks, days off, swapped days) with a big
  countdown to the next break, plus the school's own events (e.g. Dan škole, school slava) added per child. Days
  without classes show no lessons on the Today tab.
- **Settings** (the round button at the top right, meant for parents): theme, children, QR import and a test
  notification; per child: name and school, shift rotation (alternating weekly or monthly, or always the same, with
  a choice of which shift is current), parent alarm, packing reminder, calendar region (Central Serbia or Vojvodina) and 8th grade
  (earlier end of classes), bell times for each shift, and the subject list with what to bring for each subject.

The official calendar lives in `app/src/main/java/com/bane23/classschedule/data/SchoolCalendar.kt` (`SCHOOL_YEARS`).
Add a new `SchoolYear` there each summer once the ministry publishes the next year's calendar.

- **Subjects:** each child has an editable subject list (Serbian primary-school subjects by default); the timetable
  and one-day changes pick subjects from a sheet of tiles, and a subject typed by hand is added to the list. Each
  subject can have a note on what to bring (e.g. "patike i majica" for PE), which fills the packing list.
- **Pre-class and optional classes:** each shift can have a "pretčas" before the 1st class, and any period can be
  marked as optional remedial ("Dopunska") or advanced ("Dodatna") teaching for a chosen subject, e.g. Tuesday morning
  "Dopunska: Srpski jezik", Tuesday afternoon "Dopunska: Matematika".
- **QR sharing:** a child's settings show their timetable, bell times, shifts, calendar events and subjects as a
  QR code; another phone scans it and adds it as a new child or replaces an existing one. One-day changes and alarm
  settings are not shared. The code is deflate-compressed JSON (`data/ShareCodec.kt`).
- **Parent alarm:** an optional notification when the day's last class ends (or 5/10/15 minutes before), following
  the shift, the school calendar and one-day changes. Alarms fire with the app closed and in Doze, and are rescheduled after a reboot, app update or clock change; no exact-alarm permission is needed. When battery optimization is on, the child's settings show a shortcut to turn it off. By default it plays a short school-bell chime
  (`res/raw/school_bell.wav`, generated for this app); the "Zvuk" choice "Alarm" makes it ring with the phone's alarm
  tone at alarm volume, repeating until the notification is opened or dismissed, and "Bez zvuka" posts a plain
  notification without any sound.
- **Packing reminder:** an optional per-child notification for the student, at a chosen time (19:00 by default) on
  each evening before a school day, listing the next day's subjects not yet ticked off in "Sutra u torbu" with what
  to bring for each; it is skipped when everything is packed. It is scheduled and rescheduled like the parent alarm
  and has the same sound choices, on its own notification channels.
- Tapping either notification opens the app on the Today tab of that child.

**Design ("Stanice"):** a cool blue-white base (deep night blue in the dark theme) with one tangerine accent, the
rounded [Baloo 2](https://github.com/EkType/Baloo2) font (SIL Open Font License 1.1, bundled in `res/font`, cut down
to Latin characters) and a color and emoji per subject (`ui/Components.kt`), so younger children can recognize
classes without reading. The light theme is the default; Settings also offers dark and "follow the phone".

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
