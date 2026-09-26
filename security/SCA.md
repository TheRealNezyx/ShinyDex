# SCA: análisis de composición de software

SAST revisa el código que *yo* escribí. SCA revisa el código que *importé*: las librerías de
terceros, sus dependencias y las vulnerabilidades conocidas (CVE) publicadas contra ellas. En
una app de Android el árbol de dependencias es mucho más grande que la app misma, así que esta
revisión no es opcional.

## Herramienta: OWASP Dependency-Check

| | |
|---|---|
| Configuración | [`security/sca.gradle.kts`](sca.gradle.kts) |
| Comando | `gradlew -PenableSca=true -PnvdApiKey=<llave> :app:dependencyCheckAnalyze` |
| Reportes | `app/build/reports/dependency-check-report.{html,json}` |
| Criterio | `failBuildOnCVSS = 7.0`: cualquier CVE alto o crítico hace fallar el build |

Dependency-Check se **activa a propósito** (`-PenableSca=true`). La primera vez descarga una
copia local de la National Vulnerability Database, lo que tarda varios minutos y requiere una
llave gratuita de <https://nvd.nist.gov/developers/request-an-api-key>. Dejarlo fuera del
camino normal hace que un build o una sincronización del IDE nunca tengan que esperarlo.

En Jenkins corre en la etapa **SCA** cuando se marca el parámetro `RUN_SCA`, y la llave sale de
la credencial `NVD_API_KEY` (nunca se guarda en el repositorio).

## Revisar el árbol de dependencias a mano

```bash
gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

## Las dependencias de ShinyDex

La app usa a propósito pocas librerías y todas muy conocidas: cada librería extra es más
superficie de ataque y una cosa más que actualizar.

| Dependencia | Para qué está | Notas de riesgo |
|---|---|---|
| Jetpack Compose + Material 3 | Interfaz | AndroidX oficial; versiones alineadas por el BOM de Compose |
| AndroidX Room | Base de datos local de cazas | AndroidX oficial; sin exposición a la red |
| AndroidX Navigation Compose | Navegación entre pantallas | AndroidX oficial |
| Retrofit + convertidor Gson | Cliente de PokéAPI y de Face-off | Muy auditada; Gson solo lee respuestas de servidores fijos |
| OkHttp logging-interceptor | Registro de peticiones | **Solo en versiones de prueba**, protegido con `BuildConfig.DEBUG` |
| Coil 3 | Carga de sprites | Usa el mismo cliente OkHttp seguro en lugar de crear el suyo |

Notas para la revisión:

- No hay inicio de sesión, pagos ni cuentas de usuario, así que una dependencia comprometida no
  puede filtrar credenciales: no hay ninguna.
- Gson solo convierte a las clases de datos simples de `data/remote/dto/` y
  `data/remote/faceoff/`. No hay resolución polimórfica ni por reflexión de tipos.
- El BOM de Compose fija todas las librerías de Compose en un solo conjunto de versiones
  probado, lo que elimina los errores por mezclar versiones.

## Mantener las dependencias al día

Las versiones viven en un solo lugar, [`gradle/libs.versions.toml`](../gradle/libs.versions.toml),
así que actualizar es un cambio en un solo archivo.

## Estado

Configurado y documentado. Todavía no se ha ejecutado contra la NVD porque requiere la llave;
queda pendiente en el registro de [`SAC.md`](SAC.md).
