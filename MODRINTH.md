# LibreLogin Fork — Authentication for your Minecraft network

**LibreLogin Fork** is a maintained fork of [LibreLogin](https://github.com/kyngs/LibreLogin), an open-source authentication plugin for Minecraft servers and networks. It protects your server with **registration, login, premium (autologin) support, sessions and 2FA (TOTP)** — and it is easy to install.

> **AI-assisted update:** This plugin (release 0.24.7 and the message-formatting upgrade) was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6). See the [CHANGELOG](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.24.7/CHANGELOG.md) for the complete list of changes.

---

## Which file do you need?

Download the **3-in-1 ZIP** (`LibreLogin-0.24.7.zip`) from the [Releases](https://github.com/mitsuba7891/LibreLogin-Fork/releases) page and pick the files for your setup:

| Your setup | Files you need |
|---|---|
| 🟣 **Velocity network** (proxy + servers) | `LibreLogin-Velocity.jar` **+** `AuthLimbo.jar` |
| 🟢 **Single Paper server** (no proxy) | `LibreLogin-Paper.jar` |
| 🔵 **Paper limbo backend** (for Velocity) | `AuthLimbo.jar` alone |

> ⚠️ **Important:** On a Velocity network, the **limbo is required**. Without `AuthLimbo` on the `auth` backend, players cannot connect — you will see "Limbo not running". Install **both** `LibreLogin-Velocity.jar` (proxy) **and** `AuthLimbo.jar` (auth server).

**Requirements**

- Java **21** or newer.
- A supported **Paper** or **Velocity** build for your Minecraft version.
- A database (MariaDB/MySQL, SQLite or PostgreSQL) configured in `config.yml`.
- On Velocity, also install **PacketEvents 2.13.0+** if you want in-game QR codes for 2FA.

---

## 🟣 Option 1 — Velocity network (recommended)

The proxy handles login for all servers. Structure:

```text
Velocity/plugins/LibreLogin-Velocity.jar      ← on the proxy
Paper-auth/plugins/AuthLimbo.jar             ← on the "auth" server (the limbo)
Paper-lobby/plugins/                         ← your normal servers (nothing extra)
```

### Step 1 — Install the files

1. Put `LibreLogin-Velocity.jar` in the **Velocity** `plugins/` folder.
2. Put `AuthLimbo.jar` in the **auth Paper server's** `plugins/` folder.
3. Do **not** install `LibreLogin-Paper.jar` on the auth server — it would create a second login system and break everything.

### Step 2 — Register your servers in `velocity.toml`

```toml
[servers]
auth = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["auth"]
```

Use your real IPs/ports. Keep the `auth` server private (not reachable from the internet).

### Step 3 — Enable modern forwarding

Turn on **modern forwarding** in Velocity and set the **same forwarding secret** in Velocity and on every Paper server. This is required or backends will disconnect players.

### Step 4 — Prepare the limbo world (auth server)

In the auth server's `server.properties`:

```properties
level-name=auth_void
allow-flight=true
```

In `bukkit.yml`:

```yaml
worlds:
  auth_void:
    generator: AuthLimbo:void
```

Start the auth server once so it creates the empty limbo world.

### Step 5 — Configure the plugin

Start everything, then edit the generated `plugins/librelogin/config.yml` on the proxy:

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
```

- `limbo` = where unauthenticated players wait (the `auth` server).
- `lobby.root` = where players go after login.

Done! Players now register/login at the proxy, get sent to the limbo until authorized, then to the lobby.

---

## 🟢 Option 2 — Single Paper server

Use this when Paper itself handles authentication (no proxy).

1. Put `LibreLogin-Paper.jar` in the Paper server's `plugins/` folder.
2. Start the server once — it generates `config.yml` and `messages.yml`.
3. Stop, edit `config.yml` (database, limbo world, lobby), then start again.

```text
Paper/plugins/LibreLogin-Paper.jar
```

---

## 🔵 Option 3 — AuthLimbo alone

`AuthLimbo` is a tiny companion that **protects the limbo world**. It is not a login plugin — it only:

- creates/uses the `auth_void` world,
- keeps players at the spawn point,
- blocks movement, teleport abuse, interaction, damage, drops and inventory actions.

Install it on the auth Paper backend for the Velocity architecture (see Option 1). It requires modern Velocity forwarding on the backend.

---

## ⚙️ Configuration

### Database

In `config.yml`, set your database in this order (the generated file has comments):

```text
database name → host → port → user → password
```

MariaDB uses `jdbc:mariadb://`, MySQL uses `jdbc:mysql://`. Drivers are loaded automatically at startup.

### Messages (`messages.yml`)

Every message is written between double quotes and supports:

- **`\n`** — line break: `"Line one\nLine two"`
- **`[center]`** — center a line in chat: `"[center]&e&lLibreLogin"`
- **Lists** — one line per entry:

  ```yaml
  prompt-login:
    - "Line one"
    - "[center]&e&lLine two"
  ```

- **Prefix** — shown before chat messages:

  ```yaml
  prefix: "LibreLogin"   # or "" to disable
  ```

Reload messages with `/librelogin reload messages`.

---

## ⌨️ Commands

```text
/register <password> <password>     Create your account
/login <password>                   Log in
/login <password> <2fa_code>        Log in with 2FA
/2fa                               Enable two-factor authentication
/2faconfirm <code>                  Confirm 2FA setup
/premium                           Enable premium (autologin)
/cracked                           Disable premium
```

If an account has premium/autologin active, run `/cracked` first before enabling 2FA. Never share TOTP secrets or QR codes — treat them like passwords.

---

## ❓ Troubleshooting

| Problem | Fix |
|---|---|
| "Limbo not running" | The `auth` server is offline, not registered in `velocity.toml`, or not listed under `limbo`. |
| Backend disconnects instantly | Enable modern forwarding and check that the forwarding secret matches on all servers. |
| Player can move in the limbo | AuthLimbo is missing/disabled on the auth server. |
| QR code not shown | Install PacketEvents 2.13.0+ on Velocity, or use the manual TOTP secret shown in chat. |
| Messages not changing | Run `/librelogin reload messages` or restart the proxy. |

---

## 📦 Building from source (optional)

```bash
./gradlew :API:test :Plugin:test :Plugin:platformJars :Plugin:releaseArchive --no-daemon -PnoBump
```

Artifacts are generated in `Plugin/build/libs/platform/` and the release ZIP in `Plugin/build/distributions/`.

---

## 📄 License

Distributed under the **Mozilla Public License 2.0 (MPL-2.0)**, the same license as the upstream project. This fork is **not** affiliated with or endorsed by the upstream LibreLogin maintainers.

- Upstream: <https://github.com/kyngs/LibreLogin>
- Fork: <https://github.com/mitsuba7891/LibreLogin-Fork>

---

# 🇪🇸 Español

# LibreLogin Fork — Autenticación para tu red de Minecraft

**LibreLogin Fork** es un fork mantenido de [LibreLogin](https://github.com/kyngs/LibreLogin), un plugin de autenticación de código abierto para servidores y redes de Minecraft. Protege tu servidor con **registro, inicio de sesión, soporte premium (autologin), sesiones y 2FA (TOTP)** — y es fácil de instalar.

> **Actualización con IA:** Este plugin (release 0.24.7 y la mejora de formato de mensajes) fue revisado y actualizado con asistencia de IA (Freebuff assistant usando GPT Luna 5.6). Consulta el [CHANGELOG](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.24.7/CHANGELOG.md) para la lista completa de cambios.

---

## ¿Qué archivo necesitas?

Descarga el **ZIP 3-en-1** (`LibreLogin-0.24.7.zip`) desde la página de [Releases](https://github.com/mitsuba7891/LibreLogin-Fork/releases) y elige los archivos según tu caso:

| Tu configuración | Archivos que necesitas |
|---|---|
| 🟣 **Red Velocity** (proxy + servidores) | `LibreLogin-Velocity.jar` **+** `AuthLimbo.jar` |
| 🟢 **Servidor Paper único** (sin proxy) | `LibreLogin-Paper.jar` |
| 🔵 **Backend Paper limbo** (para Velocity) | Solo `AuthLimbo.jar` |

> ⚠️ **Importante:** En una red Velocity, **el limbo es obligatorio**. Sin `AuthLimbo` en el servidor `auth`, los jugadores no pueden entrar — verás "Limbo not running". Instala **ambos**: `LibreLogin-Velocity.jar` (en el proxy) **y** `AuthLimbo.jar` (en el servidor auth).

**Requisitos**

- Java **21** o superior.
- Una build compatible de **Paper** o **Velocity** para tu versión de Minecraft.
- Una base de datos (MariaDB/MySQL, SQLite o PostgreSQL) configurada en `config.yml`.
- En Velocity, instala también **PacketEvents 2.13.0+** si quieres códigos QR en el juego para el 2FA.

---

## 🟣 Opción 1 — Red Velocity (recomendada)

El proxy gestiona el login de todos los servidores. Estructura:

```text
Velocity/plugins/LibreLogin-Velocity.jar      ← en el proxy
Paper-auth/plugins/AuthLimbo.jar             ← en el servidor "auth" (el limbo)
Paper-lobby/plugins/                         ← tus servidores normales (nada extra)
```

### Paso 1 — Instala los archivos

1. Pon `LibreLogin-Velocity.jar` en la carpeta `plugins/` de **Velocity**.
2. Pon `AuthLimbo.jar` en la carpeta `plugins/` del **servidor Paper auth**.
3. **No** instales `LibreLogin-Paper.jar` en el servidor auth — crearía un segundo sistema de login y rompería todo.

### Paso 2 — Registra tus servidores en `velocity.toml`

```toml
[servers]
auth = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["auth"]
```

Usa tus IPs/puertos reales. Mantén el servidor `auth` privado (sin acceso desde internet).

### Paso 3 — Activa el forwarding moderno

Activa **modern forwarding** en Velocity y pon la **misma forwarding secret** en Velocity y en cada servidor Paper. Es obligatorio o los backends expulsarán a los jugadores.

### Paso 4 — Prepara el mundo limbo (servidor auth)

En `server.properties` del servidor auth:

```properties
level-name=auth_void
allow-flight=true
```

En `bukkit.yml`:

```yaml
worlds:
  auth_void:
    generator: AuthLimbo:void
```

Inicia una vez el servidor auth para que cree el mundo vacío del limbo.

### Paso 5 — Configura el plugin

Inicia todo y luego edita el `plugins/librelogin/config.yml` generado en el proxy:

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
```

- `limbo` = dónde esperan los jugadores sin autenticar (el servidor `auth`).
- `lobby.root` = dónde van los jugadores tras iniciar sesión.

¡Listo! Los jugadores se registran/inician sesión en el proxy, van al limbo hasta autenticarse y luego al lobby.

---

## 🟢 Opción 2 — Servidor Paper único

Usa esto cuando Paper gestiona la autenticación (sin proxy).

1. Pon `LibreLogin-Paper.jar` en la carpeta `plugins/` del servidor Paper.
2. Inicia el servidor una vez — genera `config.yml` y `messages.yml`.
3. Detén, edita `config.yml` (base de datos, mundo limbo, lobby) y vuelve a iniciar.

```text
Paper/plugins/LibreLogin-Paper.jar
```

---

## 🔵 Opción 3 — Solo AuthLimbo

`AuthLimbo` es un pequeño acompañante que **protege el mundo limbo**. No es un plugin de login — solo:

- crea/usa el mundo `auth_void`,
- mantiene a los jugadores en el punto de aparición,
- bloquea movimiento, abuso de teletransporte, interacción, daño, drops y acciones de inventario.

Instálalo en el backend Paper auth para la arquitectura Velocity (ver Opción 1). Requiere modern forwarding de Velocity en el backend.

---

## ⚙️ Configuración

### Base de datos

En `config.yml`, configura tu base de datos en este orden (el archivo generado tiene comentarios):

```text
nombre de la db → host → puerto → user → contraseña
```

MariaDB usa `jdbc:mariadb://`, MySQL usa `jdbc:mysql://`. Los drivers se cargan automáticamente al iniciar.

### Mensajes (`messages.yml`)

Cada mensaje va entre comillas dobles y soporta:

- **`\n`** — salto de línea: `"Linea uno\nLinea dos"`
- **`[center]`** — centrar una línea en el chat: `"[center]&e&lLibreLogin"`
- **Listas** — una línea por entrada:

  ```yaml
  prompt-login:
    - "Línea uno"
    - "[center]&e&lLínea dos"
  ```

- **Prefijo** — se muestra antes de los mensajes de chat:

  ```yaml
  prefix: "LibreLogin"   # o "" para desactivarlo
  ```

Recarga los mensajes con `/librelogin reload messages`.

---

## ⌨️ Comandos

```text
/register <contraseña> <contraseña>    Crear tu cuenta
/login <contraseña>                    Iniciar sesión
/login <contraseña> <codigo_2fa>       Iniciar sesión con 2FA
/2fa                                  Activar verificación en dos pasos
/2faconfirm <codigo>                   Confirmar la configuración de 2FA
/premium                              Activar premium (autologin)
/cracked                              Desactivar premium
```

Si una cuenta tiene premium/autologin activo, ejecuta `/cracked` antes de activar el 2FA. Nunca compartas secretos TOTP ni códigos QR — trátalos como contraseñas.

---

## ❓ Solución de problemas

| Problema | Solución |
|---|---|
| "Limbo not running" | El servidor `auth` está apagado, no registrado en `velocity.toml`, o no está en `limbo`. |
| El backend expulsa al instante | Activa el modern forwarding y comprueba que la forwarding secret coincide en todos los servidores. |
| El jugador se mueve en el limbo | Falta AuthLimbo o está desactivado en el servidor auth. |
| No aparece el QR | Instala PacketEvents 2.13.0+ en Velocity, o usa el secreto TOTP manual que sale en el chat. |
| Los mensajes no cambian | Ejecuta `/librelogin reload messages` o reinicia el proxy. |

---

## 📦 Compilar desde el código (opcional)

```bash
./gradlew :API:test :Plugin:test :Plugin:platformJars :Plugin:releaseArchive --no-daemon -PnoBump
```

Los artefactos se generan en `Plugin/build/libs/platform/` y el ZIP de release en `Plugin/build/distributions/`.

---

## 📄 Licencia

Distribuido bajo la **Mozilla Public License 2.0 (MPL-2.0)**, la misma licencia del proyecto original. Este fork **no** está afiliado ni respaldado por los mantenedores del LibreLogin original.

- Proyecto original: <https://github.com/kyngs/LibreLogin>
- Fork: <https://github.com/mitsuba7891/LibreLogin-Fork>
