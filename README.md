# Raspored časova

Android aplikacija za praćenje rasporeda časova, školskog kalendara (praznici i raspusti) i roditeljskog
obaveštenja kada se časovi za taj dan završe. Podržava prepodnevnu i popodnevnu smenu.

## Funkcije

- **Danas:** časovi za izabrani dan sa vremenom zvona i smenom. Dodirom na čas on se menja ili otkazuje samo za taj
  dan (npr. Srpski umesto Engleskog, ili nema Fizičkog), a dodaje se i čas samo za taj dan. Izmene za prošle dane se
  same brišu.
- **Nedelja:** redovni raspored od ponedeljka do petka, posebno za prepodnevnu i popodnevnu smenu.
- **Podešavanja:** više dece (jedno po defaultu), škola, smena (uvek prepodne, uvek popodne, menja se nedeljno ili
  mesečno, uz izbor koja je smena ove nedelje/meseca) i vreme zvona za svaku smenu.

Podaci se čuvaju lokalno na telefonu (`data.json` u privatnom prostoru aplikacije).

## Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew bundleRelease     # release AAB (nepotpisan bez podešenog ključa)
```

Potrebni su JDK 17 i Android SDK (API 36).

## Automatski release

Svaki push/merge na `main` pokreće `.github/workflows/build-release.yml`, koji:

1. pravi debug APK i potpisan release AAB (AAB samo ako su podešeni secrets za potpisivanje, vidi dole),
2. objavljuje GitHub Release `v1.0.<broj builda>` sa ta dva fajla,
3. briše starije release-ove (i njihove tagove) tako da ostanu samo poslednja 3.

Pull request-ovi prolaze isti build (bez potpisivanja i bez objavljivanja). Build fajlovi se nikad ne čuvaju u
repozitorijumu niti kao Actions artefakti.

### Ključevi za potpisivanje

**Debug APK** se potpisuje ključem `app/debug.keystore` koji je u repozitorijumu (nije tajna, lozinka `android`).
Zato svaki novi debug APK može da se instalira preko prethodnog. Debug verzija ima ID
`com.bane23.classschedule.debug`, pa može da stoji na telefonu pored prave verzije.

**Release AAB** se potpisuje tvojim ključem za otpremanje (upload key), koji se čuva samo u GitHub secrets.

1. Napravi keystore (jednom, na svom računaru; potreban je JDK):

   ```bash
   keytool -genkeypair -v -keystore upload.jks -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

   Zapamti lozinku keystore-a i lozinku ključa (mogu biti iste). Fajl `upload.jks` sačuvaj na sigurnom mestu
   van repozitorijuma; bez njega ne možeš da otpremiš nove verzije dok ga Google ne zameni.

2. Pretvori keystore u base64 tekst:

   ```bash
   # Linux
   base64 -w0 upload.jks > upload.jks.b64
   # macOS
   base64 -i upload.jks -o upload.jks.b64
   # Windows (PowerShell)
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("upload.jks")) | Out-File upload.jks.b64 -NoNewline
   ```

3. Na GitHub-u otvori repozitorijum → **Settings → Secrets and variables → Actions → New repository secret**
   i dodaj četiri secrets:

   | Secret | Vrednost |
   | --- | --- |
   | `SIGNING_KEYSTORE_BASE64` | ceo sadržaj fajla `upload.jks.b64` |
   | `SIGNING_STORE_PASSWORD` | lozinka keystore-a |
   | `SIGNING_KEY_ALIAS` | alias ključa (`upload` iz komande gore) |
   | `SIGNING_KEY_PASSWORD` | lozinka ključa |

4. Obriši `upload.jks.b64` sa računara.

Pri prvoj objavi na Google Play uključi **Play App Signing**; Google tada čuva konačni ključ aplikacije, a ovaj
ključ služi samo za otpremanje.
