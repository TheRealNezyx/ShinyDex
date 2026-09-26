# SAST: pruebas estáticas de seguridad

El análisis estático revisa el código y la configuración de ShinyDex **sin ejecutarlos**. En
este proyecto corren dos herramientas, y las dos están conectadas al pipeline de Jenkins.

## 1. detekt (análisis estático de Kotlin)

| | |
|---|---|
| Configuración | [`config/detekt/detekt.yml`](../config/detekt/detekt.yml) |
| Comando | `gradlew :app:detektSast` |
| Reportes | `app/build/reports/detekt/detekt.{html,xml,sarif}` |
| Criterio | `build.maxIssues: 0`: cualquier hallazgo hace fallar el build |

Reglas relevantes para seguridad que están activadas:

- `exceptions.SwallowedException` / `TooGenericExceptionCaught`: una excepción que se atrapa y
  se ignora esconde fallas, incluidas las validaciones de red que fallan.
- `style.ForbiddenComment`: las marcas `FIXME:` no pueden llegar a una versión final.
- `style.UnusedPrivateMember`: el código muerto es código que nadie revisa.
- `complexity.LongMethod` / `LongParameterList`: las funciones largas y complejas son donde se
  esconden los errores de lógica (y por lo tanto de seguridad). Las funciones de Compose están
  exentas: son árboles de interfaz declarativos, no lógica.

> **Nota sobre el JDK.** detekt 1.23.x no puede correr en JDK 23 o superior, y este proyecto
> fija el daemon de Gradle en Java 25 (`gradle/gradle-daemon-jvm.properties`). Por eso la tarea
> `detektSast` corre detekt en su propio proceso con JDK 21 en lugar de dentro del daemon, y
> funciona igual desde Android Studio, una terminal y Jenkins. Solo necesita un JDK 21 que
> Gradle pueda encontrar.

## 2. Android Lint

| | |
|---|---|
| Configuración | `android.lint { }` en [`app/build.gradle.kts`](../app/build.gradle.kts) |
| Comando | `gradlew :app:lintDebug` |
| Reportes | `app/build/reports/lint-results-debug.{html,sarif}` |
| Criterio | `abortOnError = true` |

Lint entiende las debilidades propias de Android: componentes exportados sin permisos, tráfico
sin cifrar, manejo inseguro de `Intent`, almacenamiento legible por otras apps, secretos
escritos en el código, dependencias con avisos de seguridad y configuraciones inseguras de
`WebView`. `checkDependencies = true` hace que también analice el código de las librerías.

El resultado en SARIF lo lee el plugin Warnings-NG de Jenkins, así que los hallazgos aparecen
como anotaciones del build y no escondidos en el log.

## Controles estáticos que revisan estas herramientas

| Control | Dónde está |
|---|---|
| HTTP sin cifrar bloqueado en toda la app | `res/xml/network_security_config.xml`, `usesCleartextTraffic="false"` |
| Datos de la app fuera de respaldos y transferencias | `res/xml/data_extraction_rules.xml`, `allowBackup="false"` |
| Versión final comprimida y ofuscada | `isMinifyEnabled` / `isShrinkResources` en `app/build.gradle.kts` |
| Sin registro de red en la versión final | Condición `BuildConfig.DEBUG` en `data/remote/ApiClient.kt` |
| Solo dos permisos | `AndroidManifest.xml` (`INTERNET`, `ACCESS_NETWORK_STATE`) |
| Un solo componente exportado | `MainActivity` es la única entrada con `android:exported="true"` |

## Resultados

| Fecha | Herramienta | Resultado |
|---|---|---|
| 2026-09-23 | `gradlew :app:detektSast` | 0 hallazgos (antes: 16 en el código de Face-off, corregidos) |
| 2026-09-23 | `gradlew :app:lintDebug` | Sin errores |
