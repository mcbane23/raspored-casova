# Publishing on Google Play

Checklist and texts for the Google Play Console listing.

## Before the first upload

1. Add the four signing secrets (see README → Signing keys) so releases include a signed AAB.
2. Create the app in Play Console: default language **Serbian (Latin) – sr**, app (not game), free.
3. Enable **Play App Signing** and upload the AAB from the latest GitHub release.
4. Paste the privacy policy URL in Play Console: https://mcbane23.github.io/privacy/raspored-casova/ (published
   from the public `mcbane23/privacy` repository; keep it in sync with `docs/privacy-policy.md`).
5. Store graphics (kept outside the repository, in the project's shared files under `play-store/v2/`): the 512×512
   "calendar and bell" icon (same artwork as the launcher icon), six 1080×1920 phone screenshots with Serbian
   captions, and two 1024×500 feature graphic variants.

## Release notes (first release)

Paste into Play Console → the release → **Release notes** (max 500 characters per language; these are 396 and
403). Later releases can reuse the `SR:`/`EN:` lines from the GitHub release, see [releases.md](releases.md).

```
<sr>
Prva verzija aplikacije Raspored časova!
• Današnji časovi i nedeljni raspored, posebno za prepodnevnu i popodnevnu smenu
• Školski kalendar Srbije 2026/27 sa raspustima i odbrojavanjem
• Roditeljski alarm kad se završi poslednji čas
• Večernji podsetnik za pakovanje torbe
• Više dece, deljenje rasporeda QR kodom i widget za početni ekran
Bez naloga, bez reklama, svi podaci ostaju na telefonu.
</sr>
<en-US>
First release of Raspored časova (School Timetable)!
• Today's classes and the weekly timetable, separate for morning and afternoon shifts
• Serbian school calendar 2026/27 with breaks and a countdown
• Parent alarm when the last class ends
• Evening reminder to pack the school bag
• Several children, QR code timetable sharing and a home-screen widget
No account, no ads, all data stays on your phone.
</en-US>
```

## App content answers

| Section | Answer |
| --- | --- |
| Privacy policy | https://mcbane23.github.io/privacy/raspored-casova/ |
| Ads | No ads |
| App access | All functionality available without login |
| Content rating | Reference/utility app, no objectionable content |
| Target audience | Parents (18+); not designed primarily for children, so the Families policy does not apply |
| Data safety | No data collected, no data shared; data is not encrypted in transit because nothing is transmitted |
| Government app | No |
| Financial features | None |
| Health | No |

## Permissions declared

- `POST_NOTIFICATIONS` – the optional "classes are over" and "pack the bag" reminders.
- `RECEIVE_BOOT_COMPLETED` – reschedules that reminder after a restart.
- `SCHEDULE_EXACT_ALARM` – lets both reminders fire at the exact minute set. On Android 12+ the user grants it under "Alarms & reminders"; the app links there and warns while it is missing.
- `CAMERA` (from the zxing-android-embedded scanner) – scanning a shared timetable QR code.

With exact alarms allowed the app uses `AlarmManager.setAlarmClock`, which fires on time even in Doze; without the permission it falls back to `setAndAllowWhileIdle`, which Android may delay by 10-15 minutes. It uses the user-granted `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM` (reserved by Play policy for alarm-clock and calendar apps), so no exact-alarm declaration form is needed. It does not request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; Settings only links to the system battery optimization list.

## Store listing (Serbian)

**App name** (max 30): Raspored časova

**Short description** (max 80):
Raspored časova, smene, raspusti i podsetnik kad se nastava završi.

**Full description** (max 4000):

Raspored časova je jednostavna aplikacija za roditelje i decu osnovnoškolskog uzrasta. Na jednom mestu vidite
današnje časove, nedeljni raspored i školski kalendar.

• Danas – časovi za dan sa tačnim vremenom zvona. Zamena predmeta ili otkazan čas? Izmenite samo taj dan, a
raspored se sutra sam vraća na redovan.
• Prepodnevna i popodnevna smena – poseban raspored i zvona za svaku smenu; smena se menja nedeljno, mesečno ili je
stalna.
• Školski kalendar Srbije – raspusti, neradni dani i dani kada se radi po drugom rasporedu, uz odbrojavanje do
sledećeg raspusta. Podržani su centralna Srbija i Vojvodina, kao i raniji kraj nastave za osmi razred.
• Događaji škole – dodajte Dan škole, školsku slavu ili druge slobodne dane.
• Roditeljski alarm – obaveštenje kad se završi poslednji čas (ili nekoliko minuta ranije).
• Podsetnik za torbu – uveče pred školski dan podseća dete šta da spakuje za sutra.
• Više dece – svako dete ima svoj raspored, smenu i kalendar.
• Spisak predmeta – izaberite predmet iz liste umesto kucanja, uz dopunsku i dodatnu nastavu iz bilo kog predmeta.
• Pretčas – za smene koje počinju pretčasom.
• Deljenje QR kodom – pošaljite popunjen raspored detetu ili drugom roditelju skeniranjem QR koda.

Bez naloga, bez reklama i bez interneta: svi podaci ostaju samo na vašem telefonu.

**Category:** Education
**Tags:** school, timetable, calendar
