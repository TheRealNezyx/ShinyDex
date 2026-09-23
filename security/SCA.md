# SCA — Software Composition Analysis

SAST looks at code *you* wrote. SCA looks at the code you *imported*: third-party
libraries, their transitive dependencies, and the known vulnerabilities (CVEs) published
against them. For an Android app the dependency tree is far larger than the app itself, so
this is not an optional check.

## Tool: OWASP Dependency-Check

| | |
|---|---|
| Config | [`security/sca.gradle.kts`](sca.gradle.kts) |
| Command | `gradlew -PenableSca=true -PnvdApiKey=<key> :app:dependencyCheckAnalyze` |
| Reports | `app/build/reports/dependency-check-report.{html,json}` |
| Gate | `failBuildOnCVSS = 7.0` — any High or Critical CVE fails the build |

Dependency-Check is **opt-in** (`-PenableSca=true`). It downloads a local mirror of the
National Vulnerability Database on first run, which takes several minutes and now needs a
free API key from <https://nvd.nist.gov/developers/request-an-api-key>. Keeping it off the
default path means a normal build or an IDE sync never waits on it.

In Jenkins it runs in the **SCA** stage when the `RUN_SCA` job parameter is checked, with
the key supplied by the `NVD_API_KEY` credential.

## Reviewing the dependency tree by hand

```bash
gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

## ShinyDex's dependency surface

The app deliberately keeps a small, mainstream dependency set — every extra library is
extra attack surface and one more thing to patch.

| Dependency | Why it is here | Risk notes |
|---|---|---|
| Jetpack Compose + Material 3 | UI toolkit | First-party AndroidX; version-aligned by the Compose BOM |
| AndroidX Room | Local hunt database | First-party AndroidX; no network exposure |
| AndroidX Navigation Compose | Screen routing | First-party AndroidX |
| Retrofit + Gson converter | PokeAPI client | Widely audited; Gson only parses responses from a fixed HTTPS host |
| OkHttp logging-interceptor | Request logging | **Debug builds only** — guarded by `BuildConfig.DEBUG` |
| Coil 3 | Sprite loading | Shares the hardened OkHttp client rather than building its own |

Notes for the review:

- There is no authentication, no payment path and no user account, so a compromised
  dependency cannot leak credentials — there are none to leak.
- Gson deserialises only into the plain data classes in `data/remote/dto/`, and only from
  `https://pokeapi.co`. No polymorphic or reflective type resolution is enabled.
- The Compose BOM pins every Compose artifact to one tested set of versions, which removes
  a whole class of "mixed versions" bugs.

## Keeping dependencies current

Versions live in one place, [`gradle/libs.versions.toml`](../gradle/libs.versions.toml), so
patching is a single-file change. Check for updates with:

```bash
gradlew dependencyUpdates
```

(that task needs the Ben Manes versions plugin; it is not applied by default for the same
build-speed reason as Dependency-Check.)
