# Publishing on Google Play

Checklist and texts for the Google Play Console listing.

## Before the first upload

1. Add the four signing secrets (see README → Signing keys) so releases include a signed AAB.
2. Create the app in Play Console: default language **Serbian (Latin) – sr**, app (not game), free.
3. Enable **Play App Signing** and upload the AAB from the latest GitHub release.
4. Host `docs/privacy-policy.md` at a public URL (the repository is private, so e.g. a public GitHub Gist,
   Google Sites page, or GitHub Pages from a separate public repository) and paste that URL in Play Console.
5. Store graphics: a 512×512 icon and a 1024×500 feature graphic were generated for the first listing (kept outside
   the repository); phone screenshots must be taken from the app (at least 2).

## App content answers

| Section | Answer |
| --- | --- |
| Privacy policy | URL of the hosted `docs/privacy-policy.md` |
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
