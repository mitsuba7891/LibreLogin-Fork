# Installation

[Home](Home.md) · [Configuration](Configuration.md) · [Troubleshooting](Troubleshooting.md)

**Choose your architecture first:** [Velocity network](#velocity-network-with-authlimbo) or [standalone Paper](#standalone-paper).

## Before you start

1. Choose standalone Paper or proxy-side authentication on Velocity.
2. Download your revision from [Releases](https://github.com/mitsuba7891/LibreLogin-Fork/releases), or [build `beta`](API-and-Build.md).
3. Use **Java 25** for Paper 26.2. Back up the database, plugin folders and worlds before upgrading.
4. Keep one active LibreLogin JAR per instance. Libby downloads database libraries and other runtime dependencies where required.

> **First startup:** LibreLogin generates `config.yml` and `messages.yml`, then requests a server/proxy restart instead of leaving an unauthenticated instance running. Fill in the generated files before the restarted process starts again. A restart supervisor is required if your host does not automatically relaunch the process.

## Velocity network with AuthLimbo

### Step 1 — Install the components

```text
Velocity/plugins/LibreLogin-Velocity-0.25.0.jar
Paper-auth/plugins/AuthLimbo-1.0.0.jar
```

> **Important:** Do not install LibreLogin-Paper on this auth backend. LibreLogin-Velocity owns authentication; AuthLimbo protects the waiting world.

For QR maps on Velocity, install PacketEvents 2.13.0+ separately. [Manual 2FA](Commands-and-2FA.md#enable-2fa-without-a-map) does not require a map projector.

### Step 2 — Register the backends

Merge this example into `velocity.toml` without duplicating existing sections:

```toml
online-mode = false
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"

[servers]
auth = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["auth"]
```

Replace the addresses with your actual backend addresses. Even when the proxy accepts offline accounts, premium-autologin accounts must authenticate through a valid Mojang session.

### Step 3 — Configure forwarding on every Paper backend

Set `online-mode=false` in `server.properties`. For Paper 26.2, configure `config/paper-global.yml`:

```yaml
proxies:
  velocity:
    enabled: true
    online-mode: false
    secret: "COPY_THE_PROXY_FORWARDING_SECRET"
```

The secret must match the proxy's secret, and `proxies.velocity.online-mode` must match the proxy's online-mode setting.

> **Backend access:** Allow backend connections only from the proxy. A publicly accessible offline backend would let players bypass central authentication.

### Step 4 — Prepare the AuthLimbo world

Before creating the auth backend's world, configure:

**`server.properties`**

```properties
level-name=auth_void
allow-flight=true
```

**`bukkit.yml`**

```yaml
worlds:
  auth_void:
    generator: AuthLimbo:void
```

Start the backend to create the void world. AuthLimbo does not automatically convert an existing terrain world into a void world.

### Step 5 — Configure LibreLogin on the proxy

After the first startup, stop Velocity and edit `plugins/librelogin/config.yml`:

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
```

Names must match `[servers]` exactly. Start the backends, then the proxy. Verify that a new account stays in auth until registration and an existing offline account enters the lobby only after login.

## Standalone Paper

1. Install `LibreLogin-Paper-0.25.0.jar` in `Paper/plugins/`.
2. Set `online-mode=false` in `server.properties`. Bungee/Velocity forwarding must be disabled for this architecture.
3. Start once, stop the server, and edit `plugins/LibreLogin/config.yml` and `messages.yml`.
4. Configure the actual worlds, for example:

```yaml
limbo:
  - auth_void
lobby:
  root:
    - world
```

The lobby world `world` must exist. Paper creates a missing limbo as a void world. All Paper destinations belong under `lobby.root`.

5. Restart and test registration, login, reconnection and [manual 2FA](Commands-and-2FA.md#enable-2fa-without-a-map).

On Paper, Libby loads the PacketEvents runtime library. Velocity uses the external PacketEvents plugin. Check which component provides it before installing additional copies.
