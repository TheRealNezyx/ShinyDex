# SAC — Security Assurance Case

A *security assurance case* is a structured argument that an application is acceptably
secure for its purpose, built out of **claims**, the **argument** for each claim, and the
**evidence** that supports it. It is what ties the SAST, DAST and SCA results together
into a statement someone can actually review.

> If "SAC" in your brief means **Software Composition Analysis**, that is covered
> separately in [`SCA.md`](SCA.md). Both are delivered.

---

## System description

| | |
|---|---|
| Application | ShinyDex 0.1.0 (`com.espinosa.shinydex`) |
| Platform | Android 8.0 (API 26) and above |
| Purpose | Track shiny hunts for Generations II–IV, browse each generation's Pokedex, and hunt together in face-off rooms |
| Users | A local user; in face-off rooms, up to 8 players identified only by a chosen display name. No accounts, no sign-in |
| External services | PokeAPI and its sprite CDN (read-only, public), and the ShinyDex face-off server (`:server` module, self-hosted) |
| Data stored | Hunt records (species, game, method, counter) in a private Room database; the selected generation in private SharedPreferences |
| Data transmitted | Solo hunts: nothing. Face-off: display name, chosen Pokemon and method, and encounter counts — sent only to the configured face-off server |

## Assets and what could go wrong

| Asset | Threat | Impact |
|---|---|---|
| Hunt counters | Loss or corruption | Low — annoying, not harmful |
| Hunt counters | Read by another app | Low — not sensitive, but still private to the user |
| Network traffic | Interception / tampering (MITM) | Medium — poisoned Pokedex data or sprite payloads |
| The device | Malicious payload delivered through a compromised dependency | High |
| The app itself | Reverse engineering / repackaging | Low — nothing worth stealing, no secrets |

Note what is *absent*: no credentials, no payment data, no personal information, no
location, no camera, no contacts, no background services, no user-generated content shared
with anyone. Most of the mobile attack surface is missing by design, and that is itself the
strongest part of this assurance case.

---

## Claim 1 — Network traffic cannot be intercepted or downgraded

**Argument.** All traffic uses HTTPS with the system trust store and default certificate
validation. Cleartext HTTP is denied at the platform level, so it cannot be reintroduced by
a code change without also changing the manifest and the network config.

**Evidence.**
- `AndroidManifest.xml`: `android:usesCleartextTraffic="false"`
- `res/xml/network_security_config.xml`: `cleartextTrafficPermitted="false"` in the base
  config and in the per-domain config
- `data/remote/ApiClient.kt`: the base URL is hard-coded to `https://pokeapi.co/api/v2/`
- Coil is constructed with the same OkHttp client, so sprite loading inherits the policy
  (`ShinyDexApp.newImageLoader`)
- DAST: with a ZAP proxy in front of the app and its CA *not* trusted, every request fails
  — see [`DAST.md`](DAST.md) §2

## Claim 2 — Local data stays local

**Argument.** Room writes to the app's private data directory, SharedPreferences uses
`MODE_PRIVATE`, and both are excluded from cloud backup and device transfer. No content
provider, no exported service, and no external-storage permission exists to read them out.

**Evidence.**
- `AndroidManifest.xml`: `android:allowBackup="false"`, no storage permissions
- `res/xml/data_extraction_rules.xml`: `database` and `sharedpref` domains excluded from
  both `cloud-backup` and `device-transfer`
- `data/local/SettingsStore.kt`: `Context.MODE_PRIVATE`
- Only one exported component exists (`MainActivity`, the launcher)
- DAST: MobSF file analysis confirms the database path is under
  `/data/data/com.espinosa.shinydex/`

## Claim 3 — The app requests only the privileges it needs

**Argument.** Two permissions are declared, both in the `normal` protection level, and both
are required for the Pokedex to function. No dangerous permission is requested, so no
runtime permission prompt exists to be abused.

**Evidence.** `AndroidManifest.xml` declares exactly `INTERNET` and `ACCESS_NETWORK_STATE`.

## Claim 4 — Release builds leak nothing through logs or symbols

**Argument.** HTTP logging is compiled in only for debug builds, and release builds are
minified, resource-shrunk and obfuscated by R8.

**Evidence.**
- `data/remote/ApiClient.kt`: the logging interceptor is inside an `if (BuildConfig.DEBUG)`
  guard, so it is removed from release bytecode
- `app/build.gradle.kts`: `isMinifyEnabled = true`, `isShrinkResources = true` on release
- `app/proguard-rules.pro` keeps only what reflection genuinely needs

## Claim 5 — Untrusted input cannot corrupt the app

**Argument.** The only external input is PokeAPI JSON. It is parsed by Gson into fixed,
non-polymorphic data classes with defaulted fields, so a malformed or hostile response
produces empty values rather than a crash or an injection. Every network call is wrapped in
`runCatching` and surfaces as a UI error state.

**Evidence.**
- `data/remote/dto/PokeApiDtos.kt`: plain data classes, every field defaulted
- `data/repo/PokedexRepository.kt`: `runCatching` around both API calls
- `ui/viewmodel/PokedexViewModel.kt`: failures become `DetailState.Failed` / an error banner
- Room writes go through parameterised DAO queries; there is no raw SQL string building
- Counter arithmetic is clamped (`coerceAtLeast(0)`) in `HuntRepository.adjustEncounters`

## Claim 6 — Known-vulnerable dependencies are detected before release

**Argument.** Dependencies are pinned in one version catalogue and scanned against the NVD
by OWASP Dependency-Check, gated at CVSS 7.0.

**Evidence.** [`SCA.md`](SCA.md), `security/sca.gradle.kts`, and the Jenkins **SCA** stage.

## Claim 7 — Security checks run on every build, not just when someone remembers

**Argument.** The Jenkins pipeline runs SAST on every commit and SCA/DAST on demand, and
each stage archives its report as a build artifact.

**Evidence.** [`../Jenkinsfile`](../Jenkinsfile), [`SAST.md`](SAST.md).

---

## Claim 8 — In a face-off, nobody can play for someone else

**Argument.** Joining a room returns a random 72-character token that exists only on that
device and in server memory. Every write (encounters, found, leave) must carry it, and the
server resolves the player from the token, never from a client-supplied id. Room views
returned to other players contain no tokens. All input is validated server-side: mode,
generation, dex range, name characters and length, method length, odds range, and a ±10 cap
per counter change.

**Evidence.**
- `server/.../RoomStore.kt`: `playerOrFail`, `cleanName`, `cleanPick`, `MAX_STEP`
- `RoomStoreTest`: `a player can only change their own counter`, `views never contain tokens`,
  `encounter steps are bounded...`, `names are sanitised...`, `invalid creation input is rejected`
- `ApplicationTest`: `mutations without a token are refused`, `malformed JSON is a clean 400
  with no internals`
- Invite links: `FaceOffCodes.codeFromLink` accepts only a well-formed code from the
  `shinydex://join/` scheme (`FaceOffTest`)

## Residual risks (accepted for this release)

| Risk | Why it is accepted |
|---|---|
| No certificate pinning | The app talks to a public, unauthenticated, read-only API. Pinning would add breakage risk (certificate rotation) with no confidentiality benefit — there is nothing secret in a Pokedex response. |
| Database not encrypted at rest | Hunt counters are not sensitive, and Android already encrypts the device's user data partition. SQLCipher would add a native dependency for no meaningful gain. |
| Debug APK is debuggable | Expected and required for development. Release builds are not debuggable. |
| Face-off over plain HTTP in debug | Debug builds allow cleartext (`src/debug/res/xml/network_security_config.xml`) so a laptop server works during development. Release builds do not include that file and require an HTTPS server. |
| No face-off rate limiting or accounts | Rooms are short-lived, capped at 8 players and 500 rooms, and a code is required to see one. Accounts would be the next step for a public deployment. |
| Dynamic analysis is manual | MobSF dynamic analysis needs an instrumented VM; automating it is future work. Static MobSF analysis is scripted. |

## Verification log

| Date | Version | Check | Result |
|---|---|---|---|
| 2026-08-27 | 0.1.0 | `gradlew :app:detekt` | Pass — 0 findings |
| 2026-08-27 | 0.1.0 | `gradlew :app:lintDebug` | Pass — no errors |
| 2026-08-27 | 0.1.0 | `gradlew :app:testDebugUnitTest` | Pass — 17 tests |
| 2026-08-27 | 0.1.0 | Manual run on API 36 emulator | Pass — full hunt flow, Pokedex, milestone |
| _pending_ | 0.1.0 | MobSF static + dynamic | Run before the final submission |
| _pending_ | 0.1.0 | OWASP Dependency-Check | Run once an NVD API key is available |
