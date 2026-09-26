# DAST: pruebas dinámicas de seguridad

El análisis dinámico prueba la app **mientras corre** en un celular o emulador y revisa su
comportamiento real: qué guarda, qué manda por la red y qué expone a otras apps.

ShinyDex usa **MobSF** (Mobile Security Framework) como analizador dinámico, con una revisión
opcional del tráfico con **OWASP ZAP**.

---

## 1. MobSF

### Preparación (una sola vez)

```bash
docker pull opensecurity/mobile-security-framework-mobsf:latest
docker run -d --name mobsf -p 8000:8000 opensecurity/mobile-security-framework-mobsf:latest
```

Abrir MobSF en el navegador (`localhost`, puerto 8000), iniciar sesión y copiar la llave de la
sección **API Docs**. Guardarla como variable de entorno:

```bash
export MOBSF_API_KEY=<tu_llave>
```

### Escaneo

```bash
gradlew :app:assembleDebug
bash security/mobsf-scan.sh app/build/outputs/apk/debug/app-debug.apk
```

En Windows:

```bat
gradlew.bat :app:assembleDebug
powershell -File security\mobsf-scan.ps1 -Apk app\build\outputs\apk\debug\app-debug.apk
```

El script sube el APK, inicia el escaneo y guarda `security/reports/mobsf-report.json` y un PDF.
En Jenkins corre en la etapa **DAST** cuando se marca el parámetro `RUN_DAST`.

### Qué buscar en el reporte

| Hallazgo de MobSF | Resultado esperado en ShinyDex |
|---|---|
| Tráfico sin cifrar permitido | **No** en la versión final: lo bloquea la configuración de red |
| Actividades / servicios / receptores exportados | Solo `MainActivity` |
| Respaldo de datos permitido | **No**: `allowBackup="false"` |
| Versión final depurable | **No** |
| Secretos o llaves en el código | Ninguno: PokéAPI no requiere autenticación |
| Aleatoriedad insegura / criptografía débil | Ninguna: la app no hace criptografía |
| JavaScript en WebView | La app no tiene WebView |
| Permisos peligrosos | Ninguno: `INTERNET` es normal, no peligroso |

### Análisis dinámico (en ejecución)

El analizador dinámico de MobSF necesita su propia máquina virtual de Android. Con el APK
instalado ahí:

1. Recorrer una caza completa: crear una caza, sumar 100 encuentros y abrir la Pokédex.
2. Revisar el **tráfico HTTP(S)**: toda petición a PokéAPI debe ser `https://` a `pokeapi.co` o
   `raw.githubusercontent.com`.
3. Revisar los **archivos**: `shinydex.db` y las preferencias deben estar en la carpeta privada
   de la app (`/data/data/com.espinosa.shinydex/`), nunca en almacenamiento externo.
4. Revisar los **logs**: una versión final no debe imprimir URLs ni contenido de la base de
   datos.

---

## 2. OWASP ZAP (capa de red)

ZAP se usa como proxy intermedio para confirmar que la app no acepta conexiones interceptadas.

1. Levantar ZAP en modo daemon (por ejemplo, con su imagen oficial de Docker
   `ghcr.io/zaproxy/zaproxy:stable`).
2. Configurar el emulador para que use ZAP como proxy HTTP (opción `-http-proxy` del emulador,
   apuntando a la computadora donde corre ZAP).
3. Abrir la Pokédex en la app.

Resultado esperado: sin instalar el certificado de ZAP en el dispositivo, toda llamada a
PokéAPI falla con un error SSL y la Pokédex muestra su mensaje de error. Ese es el resultado
correcto: demuestra que la app sí valida TLS y no acepta un certificado de intercepción
cualquiera.

---

## Registrar los resultados

Guardar cada ejecución en `security/reports/` con la fecha, la versión de la app (`versionName`
en `app/build.gradle.kts`) y el hash del commit, para que la evidencia coincida con el caso de
aseguramiento en [`SAC.md`](SAC.md).

## Estado

Configurado y documentado. El escaneo con MobSF todavía no se ha ejecutado porque requiere un
servidor MobSF; queda pendiente en el registro de [`SAC.md`](SAC.md).
