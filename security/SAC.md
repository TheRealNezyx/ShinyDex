# SAC: caso de aseguramiento de seguridad

Un *caso de aseguramiento de seguridad* es un argumento ordenado de que una aplicación es
suficientemente segura para su propósito. Se arma con **afirmaciones**, el **argumento** de cada
una y la **evidencia** que la respalda. Es lo que une los resultados de SAST, DAST y SCA en algo
que alguien puede revisar.

> Si en tu materia “SAC” significa **Software Composition Analysis**, eso está en
> [`SCA.md`](SCA.md). Se entregan los dos.

---

## Descripción del sistema

| | |
|---|---|
| Aplicación | ShinyDex 0.1.0 (`com.espinosa.shinydex`) |
| Plataforma | Android 8.0 (API 26) o superior |
| Propósito | Llevar cazas shiny de las generaciones II a IV, consultar la Pokédex de cada generación y cazar en grupo en salas de Face-off |
| Usuarios | Un usuario local; en las salas de Face-off, hasta 8 jugadores identificados solo por el nombre que eligen. Sin cuentas ni inicio de sesión |
| Servicios externos | PokéAPI y su repositorio de sprites (solo lectura, públicos) y el servidor de Face-off de ShinyDex (módulo `server`, propio) |
| Datos guardados | Cazas (Pokémon, juego, método, contador) en una base Room privada; la generación elegida y la sala activa en SharedPreferences privadas |
| Datos enviados | Cazas individuales: nada. Face-off: nombre, Pokémon y método elegidos y los encuentros, solo al servidor de Face-off configurado |

## Activos y qué puede salir mal

| Activo | Amenaza | Impacto |
|---|---|---|
| Contadores de cazas | Pérdida o corrupción | Bajo: molesto, no dañino |
| Contadores de cazas | Que otra app los lea | Bajo: no son sensibles, pero son privados del usuario |
| Tráfico de red | Intercepción o alteración (MITM) | Medio: datos de Pokédex o sprites alterados |
| El dispositivo | Código malicioso a través de una dependencia comprometida | Alto |
| La app | Ingeniería inversa o reempaquetado | Bajo: no hay nada que robar ni secretos |

Lo que *no* está: no hay credenciales, pagos, información personal, ubicación, cámara,
contactos, servicios en segundo plano ni contenido del usuario compartido con otros. La mayor
parte de la superficie de ataque de una app móvil no existe por diseño, y eso es lo más fuerte
de este caso.

---

## Afirmación 1: el tráfico de red no se puede interceptar ni degradar

**Argumento.** Todo el tráfico a PokéAPI usa HTTPS con los certificados del sistema y la
validación normal. El HTTP sin cifrar está bloqueado a nivel de plataforma, así que un cambio de
código no puede reactivarlo sin cambiar también el manifiesto y la configuración de red.

**Evidencia.**
- `AndroidManifest.xml`: `android:usesCleartextTraffic="false"`
- `res/xml/network_security_config.xml`: `cleartextTrafficPermitted="false"` en la configuración
  base y en la de dominios
- `data/remote/ApiClient.kt`: la dirección base está fija en `https://pokeapi.co/api/v2/`
- Coil usa el mismo cliente OkHttp, así que la carga de sprites hereda la política
  (`ShinyDexApp.newImageLoader`)
- Procedimiento de verificación con ZAP: [`DAST.md`](DAST.md), sección 2

## Afirmación 2: los datos locales se quedan en el dispositivo

**Argumento.** Room escribe en la carpeta privada de la app, SharedPreferences usa
`MODE_PRIVATE`, y ambos están excluidos de los respaldos en la nube y de la transferencia entre
dispositivos. No hay proveedor de contenido, servicio exportado ni permiso de almacenamiento
externo por donde sacarlos.

**Evidencia.**
- `AndroidManifest.xml`: `android:allowBackup="false"`, sin permisos de almacenamiento
- `res/xml/data_extraction_rules.xml`: los dominios `database` y `sharedpref` excluidos de
  `cloud-backup` y `device-transfer`
- `data/local/SettingsStore.kt`: `Context.MODE_PRIVATE`
- Solo existe un componente exportado (`MainActivity`, la pantalla de inicio)

## Afirmación 3: la app pide solo los privilegios que necesita

**Argumento.** Se declaran dos permisos, ambos de nivel `normal`, y los dos se necesitan para
la Pokédex y el Face-off. No se pide ningún permiso peligroso, así que no existe ningún aviso de
permisos que se pueda abusar.

**Evidencia.** `AndroidManifest.xml` declara exactamente `INTERNET` y `ACCESS_NETWORK_STATE`.

## Afirmación 4: la versión final no filtra nada por logs ni símbolos

**Argumento.** El registro de tráfico HTTP solo se compila en las versiones de prueba, y la
versión final se comprime, se reduce y se ofusca con R8.

**Evidencia.**
- `data/remote/ApiClient.kt`: el interceptor de registro está dentro de un
  `if (BuildConfig.DEBUG)`, así que se elimina del código de la versión final
- `app/build.gradle.kts`: `isMinifyEnabled = true`, `isShrinkResources = true` en release
- `app/proguard-rules.pro` conserva solo lo que la reflexión realmente necesita

## Afirmación 5: la información externa no puede corromper la app

**Argumento.** La información externa es JSON de PokéAPI y del servidor de Face-off. Gson la
convierte en clases de datos fijas con valores por defecto, así que una respuesta mal formada u
hostil produce valores vacíos en lugar de un cierre o una inyección. Cada llamada de red está
envuelta en `runCatching` y se convierte en un mensaje de error en pantalla.

**Evidencia.**
- `data/remote/dto/PokeApiDtos.kt`: clases de datos simples, todos los campos con valor por
  defecto
- `data/repo/PokedexRepository.kt` y `FaceOffRepository.kt`: `runCatching` alrededor de las
  llamadas
- `data/model/FaceOff.kt`: una sala con modo o generación desconocidos se rechaza
- Room usa consultas parametrizadas en los DAO; no se arma SQL con texto
- El contador está limitado (`coerceAtLeast(0)`) en `HuntRepository.adjustEncounters`

## Afirmación 6: las dependencias vulnerables se detectan antes de publicar

**Argumento.** Las dependencias están fijadas en un solo catálogo de versiones y se revisan
contra la NVD con OWASP Dependency-Check, con un corte en CVSS 7.0.

**Evidencia.** [`SCA.md`](SCA.md), `security/sca.gradle.kts` y la etapa **SCA** de Jenkins.

## Afirmación 7: las revisiones de seguridad corren en cada build, no cuando alguien se acuerda

**Argumento.** El pipeline de Jenkins corre SAST en cada commit y SCA/DAST bajo demanda, y cada
etapa guarda su reporte como artefacto del build.

**Evidencia.** [`../Jenkinsfile`](../Jenkinsfile), [`SAST.md`](SAST.md).

## Afirmación 8: en un Face-off nadie puede jugar por otro

**Argumento.** Al entrar a una sala, cada jugador recibe una llave aleatoria de 72 caracteres
que solo existe en su dispositivo y en la memoria del servidor. Cada cambio (encuentros,
“found”, salir) tiene que traerla, y el servidor identifica al jugador por la llave, nunca por
un identificador que mande el cliente. Lo que ven los demás jugadores nunca incluye llaves. El
servidor valida toda la información: modo, generación, rango de la Pokédex, caracteres y largo
del nombre, largo del método, rango de probabilidad y un tope de ±10 por cambio de contador.

**Evidencia.**
- `server/.../RoomStore.kt`: `playerOrFail`, `cleanName`, `cleanPick`, `MAX_STEP`
- `RoomStoreTest`: `a player can only change their own counter`, `views never contain tokens`,
  `encounter steps are bounded...`, `names are sanitised...`, `invalid creation input is rejected`
- `ApplicationTest`: `mutations without a token are refused`, `malformed JSON is a clean 400
  with no internals`
- Enlaces de invitación: `FaceOffCodes.codeFromLink` solo acepta un código bien formado del
  esquema `shinydex://join/` (`FaceOffTest`)

## Riesgos residuales (aceptados para esta versión)

| Riesgo | Por qué se acepta |
|---|---|
| Sin certificate pinning | La app habla con una API pública, sin autenticación y de solo lectura. El pinning agregaría riesgo de fallas (rotación de certificados) sin beneficio de confidencialidad: una respuesta de la Pokédex no tiene nada secreto. |
| Base de datos sin cifrar | Los contadores no son sensibles, y Android ya cifra la partición de datos del usuario. SQLCipher agregaría una dependencia nativa sin ganancia real. |
| El APK de prueba es depurable | Es lo esperado y necesario para desarrollar. La versión final no es depurable. |
| Face-off por HTTP sin cifrar en la versión de prueba | La versión de prueba permite HTTP (`src/debug/res/xml/network_security_config.xml`) para que funcione un servidor en red local. La versión final no incluye ese archivo y exige un servidor con HTTPS. |
| Face-off sin límite de peticiones ni cuentas | Las salas duran poco, tienen tope de 8 jugadores y 500 salas, y se necesita el código para verlas. Las cuentas serían el siguiente paso para publicarlo. |
| El análisis dinámico es manual | El análisis dinámico de MobSF necesita una máquina virtual instrumentada; automatizarlo queda pendiente. |

## Registro de verificación

| Fecha | Versión | Revisión | Resultado |
|---|---|---|---|
| 2026-08-27 | 0.1.0 | `gradlew :app:detekt` | Pasa: 0 hallazgos |
| 2026-08-27 | 0.1.0 | `gradlew :app:lintDebug` | Pasa: sin errores |
| 2026-08-27 | 0.1.0 | `gradlew :app:testDebugUnitTest` | Pasa: 17 pruebas |
| 2026-08-27 | 0.1.0 | Prueba manual en emulador API 36 | Pasa: flujo completo de caza, Pokédex y Milestone |
| 2026-09-23 | 0.1.0 | `gradlew :app:detektSast` (app y servidor) | Pasa: 0 hallazgos (16 corregidos en el código de Face-off) |
| 2026-09-23 | 0.1.0 | `gradlew :app:lintDebug` | Pasa: sin errores |
| 2026-09-23 | 0.1.0 | Prueba manual en emulador API 36 | Pasa: entrada de Pokédex en el detalle |
| 2026-09-25 | 0.1.0 | `gradlew :app:testDebugUnitTest :server:test` | Pasa: 60 pruebas |
| _pendiente_ | 0.1.0 | MobSF estático y dinámico | Requiere un servidor MobSF |
| _pendiente_ | 0.1.0 | OWASP Dependency-Check | Requiere una llave de la NVD |
