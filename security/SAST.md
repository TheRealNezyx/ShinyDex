# SAST — Static Application Security Testing

Static analysis inspects ShinyDex's source and configuration **without running it**. Two
tools run in this project, and both are wired into the Jenkins pipeline.

## 1. detekt (Kotlin static analysis)

| | |
|---|---|
| Config | [`config/detekt/detekt.yml`](../config/detekt/detekt.yml) |
| Command | `gradlew :app:detektSast` |
| Reports | `app/build/reports/detekt/detekt.{html,xml,sarif}` |
| Gate | `build.maxIssues: 0` — any finding fails the build |

Security-relevant rules that are switched on:

- `exceptions.SwallowedException` / `TooGenericExceptionCaught` — an exception that is
  caught and dropped hides failures, including failed network validation.
- `style.ForbiddenComment` — `FIXME:` markers cannot reach a release build.
- `style.UnusedPrivateMember` — dead code is unreviewed code.
- `complexity.LongMethod` / `LongParameterList` — long, complex functions are where logic
  bugs (and therefore security bugs) hide. Compose functions are exempt: they are
  declarative UI trees, not procedural logic.

> **JDK note.** detekt 1.23.x cannot run on JDK 23 or newer, and this project pins the
> Gradle daemon to Java 25 (`gradle/gradle-daemon-jvm.properties`). The `detektSast` task
> therefore forks the detekt CLI into its own JDK 21 process rather than running inside the
> daemon, so it works identically from Android Studio, a terminal and Jenkins. The only
> requirement is a JDK 21 that Gradle's toolchain detection can find.

## 2. Android Lint

| | |
|---|---|
| Config | `android.lint { }` in [`app/build.gradle.kts`](../app/build.gradle.kts) |
| Command | `gradlew :app:lintDebug` |
| Reports | `app/build/reports/lint-results-debug.{html,sarif}` |
| Gate | `abortOnError = true` |

Lint is the tool that understands Android-specific weaknesses: exported components without
permissions, cleartext traffic, unsafe `Intent` handling, world-readable storage, hardcoded
secrets, outdated dependencies with known advisories, and insecure `WebView` settings.
`checkDependencies = true` makes it analyse library code as well as app code.

The SARIF output is consumed by the Jenkins Warnings-NG plugin so findings show up as
build annotations rather than buried log lines.

## Static controls these tools verify

| Control | Where it lives |
|---|---|
| Cleartext HTTP denied app-wide | `res/xml/network_security_config.xml`, `usesCleartextTraffic="false"` |
| App data excluded from cloud/device-transfer backups | `res/xml/data_extraction_rules.xml`, `allowBackup="false"` |
| Release builds minified and obfuscated | `isMinifyEnabled` / `isShrinkResources` in `app/build.gradle.kts` |
| No network logging in release | `BuildConfig.DEBUG` guard in `data/remote/ApiClient.kt` |
| Only two permissions requested | `AndroidManifest.xml` (`INTERNET`, `ACCESS_NETWORK_STATE`) |
| Single exported component | `MainActivity` is the only `android:exported="true"` entry |
