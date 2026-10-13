# Raspored časova

Android aplikacija za praćenje rasporeda časova, školskog kalendara (praznici i raspusti) i roditeljskog
obaveštenja kada se časovi za taj dan završe. Podržava prepodnevnu i popodnevnu smenu.

## Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew bundleRelease     # release AAB (nepotpisan bez podešenog ključa)
```

Potrebni su JDK 17 i Android SDK (API 36).

## Automatski release

Svaki push/merge na `main` pokreće `.github/workflows/build-release.yml`, koji:

1. pravi debug APK i potpisan release AAB,
2. objavljuje GitHub Release `v1.0.<broj builda>` sa ta dva fajla,
3. briše starije release-ove (i njihove tagove) tako da ostanu samo poslednja 3.

Pull request-ovi prolaze isti build (bez potpisivanja i bez objavljivanja). Build fajlovi se nikad ne čuvaju u
repozitorijumu niti kao Actions artefakti.

### Ključ za potpisivanje

U **Settings → Secrets and variables → Actions** dodati:

| Secret | Vrednost |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | keystore fajl kodiran u base64 (`base64 -w0 upload.jks`) |
| `SIGNING_STORE_PASSWORD` | lozinka keystore-a |
| `SIGNING_KEY_ALIAS` | alias ključa |
| `SIGNING_KEY_PASSWORD` | lozinka ključa |

Novi ključ za otpremanje se pravi ovako:

```bash
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
