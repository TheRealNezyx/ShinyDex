# Cómo jugar ShinyDex con tus amigos

Para cazar en grupo (modo **Face-off**) necesitan dos cosas: la app instalada en cada
teléfono, y **una computadora corriendo el servidor** en la misma red Wi-Fi.

---

## 1. Instalar la app

1. Descarga el archivo `app-debug.apk` desde la pestaña **Releases** de este repositorio.
2. Ábrelo en tu Android. Te va a decir que la instalación está bloqueada: toca
   **Configuración** y activa *Permitir de esta fuente*.
3. Instala y abre ShinyDex.

> Necesitas Android 8.0 o más nuevo.

## 2. Levantar el servidor (solo una persona)

En la computadora que va a hacer de servidor, dentro de la carpeta del proyecto:

```bash
gradlew :server:run
```

Cuando veas `ShinyDex face-off server listening on http://0.0.0.0:8080`, ya está corriendo.
Déjalo abierto mientras juegan.

Para saber qué dirección compartir, ejecuta `server/mostrar-ip.bat` (o `ipconfig`). Te va a
dar algo como:

```
http://192.168.1.20:8080
```

**La primera vez, Windows va a preguntar si permites que Java acepte conexiones.** Di que sí,
en redes privadas. Si no aparece el aviso, abre PowerShell **como administrador** y corre:

```powershell
New-NetFirewallRule -DisplayName "ShinyDex 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

## 3. Conectar los teléfonos

1. Todos en **la misma red Wi-Fi** que la computadora del servidor.
2. En la app: pestaña **Face-off** → escribe tu nombre.
3. Toca el renglón **Server** (abajo) y escribe la dirección que te pasaron, por ejemplo
   `192.168.1.20:8080`. Todos tienen que poner exactamente la misma.

## 4. Jugar

**Quien crea la sala:**
- Elige la generación arriba (Gen II, III o IV).
- En Face-off toca **Battle** o **Lounge**, elige el Pokémon y el método, y crea la sala.
- Toca el botón de compartir y manda el código (o el enlace) al grupo.

**Quien se une:**
- Escribe el código de 6 caracteres y toca **JOIN**.
- Si es un Lounge, elige qué Pokémon vas a cazar tú.

**Los dos modos:**

| | Battle | Lounge |
|---|---|---|
| Qué cazan | Todos el **mismo** Pokémon | Cada quien **el suyo** |
| Cómo termina | Gana el primero que toque *Found it!* | Cuando todos encontraron el suyo |

Cada quien lleva su propio contador con **−1**, **+1** y **+10**. Los números de todos se
actualizan solos cada 2 segundos. Cuando de verdad te salga el shiny, toca **Found it!**.

---

## Si algo no funciona

| Problema | Qué revisar |
|---|---|
| "Can't reach the face-off server" | ¿El servidor sigue corriendo? ¿Escribiste bien la dirección y el `:8080`? ¿Están en la misma Wi-Fi? |
| El teléfono no conecta pero la computadora sí | Es el firewall de Windows: corre el comando de arriba como administrador |
| Siguen sin conectar en Wi-Fi de escuela | Muchas redes escolares bloquean que los dispositivos se vean entre sí. Solución: que alguien prenda el **hotspot** de su celular y todos se conecten ahí, incluida la computadora |
| "No room with code..." | El código está mal escrito, o la sala ya se cerró |
| "That face-off is already over" | Alguien ya ganó esa batalla; creen una sala nueva |

Las salas viven en la memoria del servidor: si lo cierras y lo vuelves a abrir, las salas
abiertas se pierden.
