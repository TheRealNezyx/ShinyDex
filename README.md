# ShinyDex

A shiny-hunt tracker for Android covering **Generations II, III and IV**, written in
Kotlin with Jetpack Compose. Black, gold and white, with a Poké Ball for a logo.

> **Status: version 0.1.0 — usable, not final.** Builds, installs and runs; the whole flow
> has been exercised on an API 36 emulator. See [Roadmap](#roadmap) for what is still open.

---

## One generation at a time

Picking a generation scopes the **whole app** to that region — its species, its games, its
methods and its sprite era. Pick Hoenn and you see Hoenn: #252–386 in Emerald sprites, with
Ruby / Sapphire / Emerald and only the methods those games have.

| Generation | Region | Dex | Games | Sprites |
|---|---|---|---|---|
| **II** | Johto | #152–251 (100) | Gold · Silver · Crystal | Crystal |
| **III** | Hoenn | #252–386 (135) | Ruby · Sapphire · Emerald | Emerald |
| **IV** | Sinnoh | #387–493 (107) | Diamond · Pearl · Platinum | Platinum |

**Generation I is locked on purpose:** shiny Pokémon did not exist in Red, Blue and Yellow,
so there is nothing to hunt there.

Only games whose dex matches their own region are listed. FireRed, LeafGreen, HeartGold and
SoulSilver are remakes carrying an earlier region's dex, so including them would break the
one-generation-one-dex rule. Adding a generation means one new entry in
`data/model/Generation.kt`, its games in `GameVersion.kt` and its methods in
`HuntMethod.kt`.

## Screenshots

| Generation select | My hunts | Hunt detail |
|---|---|---|
| ![](docs/screenshots/01-generation-select.png) | ![](docs/screenshots/02-hunts.png) | ![](docs/screenshots/03-hunt-detail.png) |

| Milestone message | Pokedex (shiny on) | New hunt |
|---|---|---|
| ![](docs/screenshots/04-milestone.png) | ![](docs/screenshots/05-pokedex-shiny.png) | ![](docs/screenshots/06-new-hunt.png) |

*Captured on an API 36 emulator, version 0.1.0.*

## Features

- **Generation select** — Gen II, III or IV; Gen I stays locked. Your choice scopes the whole
  app and is remembered; "CHANGE" in the top bar brings the screen back.
- **My hunts** — every hunt is a card with the shiny sprite, the game, the method, a live
  counter and **+ / −** buttons right on the card. A summary strip totals active hunts,
  found shinies and lifetime encounters.
- **Hunt detail** — a large counter with −10 / −1 / +1 / +10, the method's real odds, the
  cumulative "chance by now" probability, a progress bar towards one full-odds cycle, and
  an explanation of how that hunting method actually works in GSC.
- **Motivational messages** — a pop-up every 100 encounters, with special messages for the
  numbers that matter (1,000 · 4,096 · **8,192** · 10,000 …).
- **Pokédex** — the selected generation's own species, pulled from [PokéAPI](https://pokeapi.co), with a
  **shiny toggle** that swaps every sprite between normal and shiny. Tap an entry for its
  types, height, weight, base stats, and the normal and shiny artwork side by side.
- **Face-off** — hunt with friends in real time: a **Battle** (same Pokémon, first shiny
  wins) or a **Lounge** (everyone hunts their own). See [Face-off](#face-off-multiplayer).
- **Works offline** — the dex is cached in Room after the first load; solo hunts never need
  the network at all.

## Face-off (multiplayer)

One player creates a room and shares its six-character code (or the `shinydex://join/CODE`
link from the share button). Anyone with the code joins from the **Face-off** tab. Rooms hold
up to 8 hunters and stay in one generation.

| Mode | Everyone hunts | Ends when | What you see |
|---|---|---|---|
| **Battle** | The Pokémon the host picked | The first player taps **Found it!** — they win, everyone else's counter locks | Standings, and the chance that *someone* has hit it: every encounter from every player pooled into `1 − (1 − 1/d)^(n₁+n₂+…)` |
| **Lounge** | Their own pick from the room's dex | Every player has found theirs | Standings, found count, combined encounters |

Counters sync every two seconds. Milestone messages still fire every 100 of *your* encounters.

### Running it

The face-off service is the `:server` module — a small Kotlin/Ktor app in this same project.

```bash
gradlew :server:run
```

It listens on port 8080 (`PORT` overrides it). Then, in the app's Face-off tab, the **Server**
setting must point at it:

| Where the app runs | Server address |
|---|---|
| Android emulator on the same laptop | `10.0.2.2:8080` (the default) |
| A phone on the same Wi-Fi | the laptop's LAN IP, e.g. `192.168.1.20:8080` — allow Java through Windows Firewall |
| Anywhere (deployed) | an `https://` host — release builds refuse plain HTTP |

Everyone in a room must use the same server. Rooms live in memory: restarting the server ends
the face-offs in progress, and idle rooms expire after 12 hours.

### Rules the server enforces

- Each player gets a random secret token on join; only that token can change their counter,
  mark their shiny or leave. Tokens are never included in room views.
- One winner per battle — the room lock makes a simultaneous second "found" impossible.
- Pokémon must belong to the room's generation; names are sanitised and unique per room;
  counters move by at most ±10 per call and never go negative.
- Codes use an alphabet without 0/O or 1/I so they survive being read aloud.

## Hunting methods

Base odds are 1 in 8192 across all three generations. What changes is what beats them.

**Generation II** — Random Encounter, Surfing, Fishing, Headbutt Trees, Soft Reset,
Roaming Beast, Bug-Catching Contest, Game Corner Prize, plus:

| Method | Odds | Notes |
|---|---|---|
| Breeding (shiny parent) | 1 / 64 | Shininess comes from DVs, and DVs are inherited |
| Odd Egg | ~14% | **Crystal only** — the option disappears on Gold and Silver |
| Red Gyarados | Guaranteed | Scripted shiny at the Lake of Rage |

**Generation III** — Random Encounter, Surfing, Fishing, Rock Smash, Safari Zone, Soft
Reset, Roaming Latias / Latios and Breeding. Every one of them is full odds: Gen III
dropped DV inheritance and the Masuda Method did not exist yet, so there is no shortcut.

**Generation IV** — Random Encounter, Surfing, Fishing, Soft Reset, Honey Tree, Great
Marsh, Roaming, plus:

| Method | Odds | Notes |
|---|---|---|
| Poké Radar chain | ~1 / 200 | Best odds in Gen IV; caps around a chain of 40 |
| Masuda Method | 1 / 1638 | Breed parents from games of different languages |

## Tech stack

| Layer | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Architecture | MVVM — `ViewModel` + `StateFlow`, unidirectional data flow |
| Dependency injection | Hand-rolled `AppContainer` (four screens do not need Hilt) |
| Persistence | Room (hunts + Pokédex cache), SharedPreferences (generation, face-off session) |
| Networking | Retrofit + Gson over a shared, timeout-bounded OkHttp client |
| Images | Coil 3, reusing the same OkHttp client |
| Navigation | Navigation Compose, `shinydex://join/CODE` deep link |
| Face-off server | Ktor 3 (CIO engine) + Gson, in-memory rooms, `:server` module |
| Build | Gradle 9.5, AGP 9.3.2, Kotlin 2.4.10, KSP, version catalogue |

## Project layout

```
app/src/main/java/com/espinosa/shinydex/
├── AppContainer.kt              dependency graph
├── ShinyDexApp.kt               Application + Coil image loader
├── data/
│   ├── local/                   Room entities, DAOs, database, SharedPreferences
│   ├── model/                   Generation, GameVersion, HuntMethod, Hunt, FaceOff...
│   ├── remote/                  Retrofit services (PokeAPI + face-off), OkHttp, sprites
│   └── repo/                    HuntRepository, PokedexRepository, FaceOffRepository
├── util/                        Odds maths, motivational messages, room codes
└── ui/
    ├── MainActivity.kt          single activity
    ├── ShinyDexRoot.kt          Scaffold + NavHost + bottom bar
    ├── components/              Poké Ball logo, chips, sprites, milestone dialog
    ├── screens/                 Generation, Hunts, HuntDetail, AddHunt, Pokedex, FaceOff...
    ├── theme/                   black / gold / white palette
    └── viewmodel/               Generation, Hunts, Pokedex and FaceOff ViewModels

server/src/main/kotlin/com/espinosa/shinydex/server/
├── Application.kt               Ktor routes and error mapping
├── RoomStore.kt                 every face-off rule, testable without HTTP
└── Models.kt                    rooms, players and the wire format
```

---

## Build and run

1. Open the `ShinyDex` folder in Android Studio (**File → Open**, pick the folder itself).
2. Let Gradle sync. It uses Android Studio's bundled JDK — nothing to configure.
3. Pick an emulator or device on API 26+ and press **Run**.

From a terminal:

```bash
gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

### Requirements

- Android Studio with **SDK Platform 37** and an API 26+ emulator or device
- A JDK 21 installation for the `detektSast` task only (see below)

---

## Tests

```bash
gradlew :app:testDebugUnitTest :server:test
```

**54 tests.** The server's 20 cover the face-off rules (one winner per battle, lounge closes
only when everyone is done, tokens, generation checks, caps, expiry) and the HTTP API end to
end. The app's 34 cover the shiny probability maths, the milestone logic, the dex ranges and
sprite eras, and the method/generation rules — including that the Odd Egg is Crystal-only,
that no method leaks into another generation, and that Gen III really has nothing that
beats full odds — plus room codes, invite links and the pooled battle probability.

## Security tooling

Full write-ups live in [`security/`](security/).

| Practice | Tool | Command | Docs |
|---|---|---|---|
| **SAST** | detekt + Android Lint | `gradlew :app:detektSast :app:lintDebug` | [SAST.md](security/SAST.md) |
| **DAST** | MobSF (+ OWASP ZAP) | `bash security/mobsf-scan.sh <apk>` | [DAST.md](security/DAST.md) |
| **SCA** | OWASP Dependency-Check | `gradlew -PenableSca=true -PnvdApiKey=<key> :app:dependencyCheckAnalyze` | [SCA.md](security/SCA.md) |
| **SAC** | Security Assurance Case | — | [SAC.md](security/SAC.md) |

> **Why `detektSast` and not the detekt Gradle plugin:** the plugin runs detekt inside the
> Gradle daemon, and detekt 1.23.x cannot parse a Java 25 version string — which is what
> this project's daemon runs on (`gradle/gradle-daemon-jvm.properties`). The `detektSast`
> task forks the detekt CLI into its own JDK 21 process instead, so it behaves the same
> from Android Studio, the terminal and Jenkins. It needs a JDK 21 that Gradle can find.

### Security controls built into the app

- HTTPS only — cleartext traffic denied in the manifest *and* the network security config
- Hunt database and preferences excluded from cloud backup and device transfer
- Exactly two permissions, both `normal` protection level, no dangerous permissions
- One exported component (the launcher activity)
- Network logging compiled into debug builds only
- Release builds minified, resource-shrunk and obfuscated by R8
- Bounded network timeouts; every API call wrapped in `runCatching`

## CI — Jenkins

[`Jenkinsfile`](Jenkinsfile) defines a declarative pipeline:

```
Checkout → Build → Unit tests → SAST → SCA → DAST → Package
```

SAST runs on every build and publishes detekt + Lint SARIF through the Warnings-NG plugin.
SCA and DAST are gated behind the `RUN_SCA` and `RUN_DAST` job parameters because they need
an NVD API key and a MobSF server respectively. The agent needs JDK 17 or 21, an Android
SDK with platform 37, and accepted SDK licences.

---

## Roadmap

- [ ] Chain counter for the Poké Radar, separate from the encounter counter
- [ ] Generation V and beyond
- [ ] Per-hunt notes and a "caught on" date
- [ ] Export / import hunts as JSON
- [ ] Instrumented Compose UI tests in CI
- [ ] Automate the MobSF dynamic scan in the pipeline

## Credits

Pokémon data and sprites come from [PokéAPI](https://pokeapi.co), which is free and
unauthenticated. Pokémon is a trademark of Nintendo / Game Freak / The Pokémon Company;
this is a non-commercial student project and is not affiliated with them.
