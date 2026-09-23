# DAST — Dynamic Application Security Testing

Dynamic analysis exercises the **running** app on a device or emulator and inspects its
real behaviour: what it writes to storage, what it sends over the network, and what it
exposes to other apps.

ShinyDex uses **MobSF** (Mobile Security Framework) as its dynamic analyser, with an
optional **OWASP ZAP** pass over the PokeAPI traffic.

---

## 1. MobSF

### One-time setup

```bash
docker pull opensecurity/mobile-security-framework-mobsf:latest
docker run -d --name mobsf -p 8000:8000 opensecurity/mobile-security-framework-mobsf:latest
```

Open <http://localhost:8000>, sign in, and copy the API key from **API Docs**. Export it:

```bash
export MOBSF_API_KEY=your_api_key_here
```

### Scan

```bash
gradlew :app:assembleDebug
bash security/mobsf-scan.sh app/build/outputs/apk/debug/app-debug.apk
```

On Windows:

```bat
gradlew.bat :app:assembleDebug
powershell -File security\mobsf-scan.ps1 -Apk app\build\outputs\apk\debug\app-debug.apk
```

The script uploads the APK, starts the scan and writes `security/reports/mobsf-report.json`
plus a PDF. In Jenkins this runs in the **DAST** stage when the `RUN_DAST` parameter is set.

### What to look for in the report

| MobSF finding | Expected result for ShinyDex |
|---|---|
| Cleartext traffic permitted | **No** — blocked by the network security config |
| Exported activities / services / receivers | Only `MainActivity` |
| App data backup allowed | **No** — `allowBackup="false"` |
| Debuggable release build | **No** |
| Hardcoded secrets / API keys | None — PokeAPI needs no authentication |
| Insecure random / weak crypto | None — the app performs no cryptography |
| WebView JavaScript enabled | No WebView in the app |
| Dangerous permissions | None — `INTERNET` is normal, not dangerous |

### Dynamic (runtime) analysis

MobSF's dynamic analyser needs its own Android VM. Once the APK is installed there:

1. Run the app through a full hunt: create a hunt, add 100 encounters, open the Pokedex.
2. Check **HTTP(S) traffic** — every request must be `https://` to `pokeapi.co` or
   `raw.githubusercontent.com`.
3. Check **Files** — `shinydex.db` and the shared-preferences XML must live under the app's
   private `/data/data/com.espinosa.shinydex/` directory, never on external storage.
4. Check **Logs** — a release build must not print request URLs or database contents.

---

## 2. OWASP ZAP (network layer)

ZAP is used as an intercepting proxy to confirm the app refuses to talk over plain HTTP.

```bash
docker run -u zap -p 8080:8080 -i ghcr.io/zaproxy/zaproxy:stable \
  zap.sh -daemon -host 0.0.0.0 -port 8080 -config api.disablekey=true
```

Point the emulator at the proxy:

```bash
emulator -avd <your_avd> -http-proxy http://10.0.2.2:8080
```

Expected outcome: with ZAP's CA certificate **not** installed on the device, every PokeAPI
call fails with an SSL error and the Pokedex shows its error banner. That is the correct
result — it proves TLS is actually being validated and the app is not accepting an
arbitrary interception certificate.

---

## Recording results

Save each run under `security/reports/` and note the date, the app version
(`versionName` in `app/build.gradle.kts`) and the commit hash, so the evidence lines up
with the assurance case in [`SAC.md`](SAC.md).
