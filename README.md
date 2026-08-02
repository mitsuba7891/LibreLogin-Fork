# LibreLogin Fork

A maintained fork and modernization of [LibreLogin](https://github.com/kyngs/LibreLogin), an open-source authentication platform for Minecraft networks.

> **Attribution and license:** This repository contains modifications of LibreLogin by kyngs and contributors. The upstream project is licensed under the **Mozilla Public License 2.0 (MPL-2.0)**; this fork retains that license and the original notices. The MIT license present under `licenses/FASTLOGIN_LICENSE` applies only to the relevant FastLogin-derived dependency, not to LibreLogin itself.
>
> **AI-assisted update:** The 0.24.7 release (and the message-formatting changes it ships) was reviewed and updated with AI assistance (GPT Luna 5.6). See the `CHANGELOG.md` 0.24.7 section for the complete list of changes.

## Release 0.24.6

This release provides three clearly separated artifacts:

| Artifact | Install on | Purpose |
|---|---|---|
| `LibreLogin-Velocity-0.24.6.jar` | Velocity proxy | Central authentication, sessions, premium login, commands and proxy-side 2FA |
| `LibreLogin-Paper-0.24.6.jar` | Standalone Paper server | Authentication when no proxy-side LibreLogin is used |
| `AuthLimbo-1.0.0.jar` | Paper `auth` backend | Empty-world limbo protection for the Velocity architecture |

For a Velocity network, install **LibreLogin-Velocity on the proxy** and **AuthLimbo on the Paper auth backend**. Do not install LibreLogin-Paper on that auth backend; it would create a second authentication pipeline.

## Requirements

- Java 21 or newer.
- A supported Paper or Velocity build compatible with the selected Minecraft version.
- A database supported by the generated configuration when using persistent authentication data.
- On Velocity, install PacketEvents 2.13.0+ separately for cross-version QR projection; it is compile-only and is not bundled in the Velocity JAR. The Paper artifact loads its PacketEvents runtime dependency through Libby.
- Optional integrations: Protocolize, LuckPerms, Floodgate and RedisBungee, only when your network uses them.

The build targets Java 21 bytecode. Running on a newer Java runtime does not automatically certify every newer Minecraft or proxy release; test the exact server/client matrix before production.

## Installation: Velocity network

### 1. Install the artifacts

```text
Velocity/plugins/LibreLogin-Velocity-0.24.6.jar
Paper-auth/plugins/AuthLimbo-1.0.0.jar
```

Install PacketEvents 2.13.0+ separately if QR projection is needed. Do not install `LibreLogin-Paper` on the proxied auth server.

### 2. Register backend servers in `velocity.toml`

```toml
[servers]
auth = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["auth"]
```

Use your actual bind addresses and ports. Keep the auth backend inaccessible from the public internet where possible.

### 3. Configure forwarding

Enable modern Velocity forwarding and use the same forwarding secret in Velocity and every Paper backend. Do not expose backend ports publicly without firewall protection.

### 4. Configure AuthLimbo before first startup

In the auth Paper server:

`server.properties`

```properties
level-name=auth_void
allow-flight=true
```

`bukkit.yml`

```yaml
worlds:
  auth_void:
    generator: AuthLimbo:void
```

Start the auth server once to generate the dedicated void world. Do not reuse a normal terrain world as the limbo world.

### 5. Configure LibreLogin

In the proxy plugin data directory, edit the generated `plugins/librelogin/config.yml` and set the backend names used by your network. The relevant shape is:

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
```

The exact generated keys and comments are authoritative for your installed revision. Back up `config.yml`, `messages.yml`, the database and worlds before upgrades.

## Standalone Paper installation

Use `LibreLogin-Paper-0.24.6.jar` only when authentication is handled directly by Paper:

```text
Paper/plugins/LibreLogin-Paper-0.24.6.jar
```

Start the server, configure the generated `config.yml` and `messages.yml`, then restart after structural configuration changes. Do not run both the proxy and standalone Paper authentication flows for the same player path.

## Login and 2FA commands

The login form accepts the password and, when TOTP is enabled, the one-time code together:

```text
/login <password> <2fa_code>
```

Typical account flow:

```text
/register <password> <password>
/login <password>
/2fa
/2faconfirm <code>
```

If a premium/autologin account is active, disable it first:

```text
/cracked
/2fa
```

The QR/provisioning output must be treated as a secret. Never post a TOTP URI or recovery data publicly.

## Messages and prefix

Edit `plugins/librelogin/messages.yml`. Every message value is written between double quotes and supports the fork formatting syntax:

```yaml
prefix: "LibreLogin"
```

The value is literal. To disable the prefix completely:

```yaml
prefix: ""
```

**Line breaks** — use `\n` inside a value to create a line break:

```yaml
info-user: "UUID: %uuid%\nJoined: %joined%"
```

**Centering** — start a line with `[center]` to center it in chat using pixel-based measurement:

```yaml
sub-title-login: "[center]&e/login &b<password>"
```

**Multi-line messages** — a message may be a YAML list; every entry becomes one line (combine with `[center]` and `\n` freely):

```yaml
prompt-login:
  - "Line one"
  - "[center]&e&lLine two"
```

Legacy `&` colour codes and MiniMessage syntax (`<bold>`, `<gradient:red:blue>`, `<size:20>`) keep working inside quoted values.

After editing messages:

```text
/librelogin reload messages
```

or restart the proxy. The prefix is not added to titles, subtitles, action bars or email templates.

## Database configuration

Use the generated configuration comments. The documented order is:

```text
database name → host → port → user → password
```

MariaDB URLs use `jdbc:mariadb://`; official MySQL URLs use `jdbc:mysql://`. Keep credentials in dedicated fields and never commit active passwords. MariaDB, MySQL, SQLite and PostgreSQL drivers are loaded at runtime through Libby.

## Upgrade and migration

Legacy HOCON files (`config.conf` and `messages.conf`) are converted to YAML automatically and retained as backup files. Review the generated YAML after migration. Do not delete database or world backups until login, premium mode, 2FA and lobby routing have been tested.

This fork removes the NanoLimbo integration from the supported release architecture. The replacement is a normal registered Paper backend running `AuthLimbo`.

## Building and release files

```bash
./gradlew :API:test :Plugin:test :Plugin:platformJars :Plugin:releaseArchive --no-daemon -PnoBump
```

Outputs:

```text
Plugin/build/libs/platform/LibreLogin-Velocity-0.24.6.jar
Plugin/build/libs/platform/LibreLogin-Paper-0.24.6.jar
Plugin/build/libs/platform/AuthLimbo-1.0.0.jar
Plugin/build/distributions/LibreLogin-0.24.6.zip
```

The ZIP contains one folder per component, a README and component changelog for each plugin, the root changelog and the MPL-2.0 license.

## License and attribution

LibreLogin Fork is distributed under the **Mozilla Public License 2.0**. See [`LICENSE`](LICENSE), [`HEADER.txt`](HEADER.txt), [`docs/dependency-licenses.md`](docs/dependency-licenses.md), and the component release directories for notices. This project is not affiliated with or endorsed by the upstream LibreLogin maintainers.

- Upstream project: <https://github.com/kyngs/LibreLogin>
- Fork repository: <https://github.com/mitsuba7891/LibreLogin-Fork>
- Release documentation: [`release/README.md`](release/README.md)
- Release changes: [`CHANGELOG.md`](CHANGELOG.md)
