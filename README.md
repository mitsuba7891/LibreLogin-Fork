<div align="center">

# LibreLogin Fork

**Open-source authentication for Paper servers and Velocity networks.**

[![Velocity](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/velocity_vector.svg)](https://github.com/mitsuba7891/LibreLogin-Fork/releases)
[![Paper](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/paper_vector.svg)](https://github.com/mitsuba7891/LibreLogin-Fork/releases)
[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/mitsuba7891/LibreLogin-Fork)

[//]: # ([![Modrinth]&#40;https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg&#41;]&#40;https://modrinth.com/plugin/librelogin-fork&#41;)

[![Gradle](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)
[![Java](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/java_vector.svg)](https://openjdk.org/projects/jdk/25/)

[![Documentation](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/generic_vector.svg)](https://github.com/mitsuba7891/LibreLogin-Fork/wiki)

[Downloads](https://github.com/mitsuba7891/LibreLogin-Fork/releases) · [Wiki / User guides](https://github.com/mitsuba7891/LibreLogin-Fork/wiki) · [API & Build](https://github.com/mitsuba7891/LibreLogin-Fork/wiki/API-and-Build) · [Issues](https://github.com/mitsuba7891/LibreLogin-Fork/issues)

</div>

<br>

A maintained fork and modernization of [LibreLogin](https://github.com/kyngs/LibreLogin), an open-source authentication platform for Minecraft networks.

> **Attribution and license:** This repository contains modifications of LibreLogin by kyngs and contributors. The upstream project is licensed under the **Mozilla Public License 2.0 (MPL-2.0)**; this fork retains that license and the original notices. The MIT license present under `licenses/FASTLOGIN_LICENSE` applies only to the relevant FastLogin-derived dependency, not to LibreLogin itself.
>
> **Historical AI-assisted update:** Release 0.24.10, including the message-formatting work introduced in 0.24.7, was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6). See the `CHANGELOG.md` 0.24.10 section for that release's changes.

## Release status

- **Stable:** [v0.24.12](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.24.12).
- **Pre-release:** [v0.25.0-beta.4](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.25.0-beta.4), with the SQLite pool warning fix and the beta.3 startup/spawn fixes.
- **Development:** the `beta` branch uses version `0.25.0`. The GitHub tag identifies the beta revision; JAR filenames retain the base version. Use the release tag and checksums to distinguish beta builds.

The [English wiki](https://github.com/mitsuba7891/LibreLogin-Fork/wiki) covers installation, database TLS, commands, manual 2FA and troubleshooting, with categorized navigation and prominent setup notes. The repository keeps its current [wiki sources](docs/wiki/Home.md); release ZIPs preserve the documentation snapshot packaged with each build. See the [cross-fork issue review](docs/libreloginprod-issues.md) for the status of reports from LibreLoginProd.

## Artifacts (0.25.0 beta)

This release provides three clearly separated artifacts:

| Artifact | Install on | Purpose |
|---|---|---|
| `LibreLogin-Velocity-0.25.0.jar` | Velocity proxy | Central authentication, sessions, premium login, commands and 2FA |
| `LibreLogin-Paper-0.25.0.jar` | Standalone Paper server | Authentication and manual 2FA when no proxy-side LibreLogin is used |
| `AuthLimbo-1.0.0.jar` | Paper `auth` backend | Empty-world limbo protection for the Velocity architecture |

For a Velocity network, install **LibreLogin-Velocity on the proxy** and **AuthLimbo on the Paper auth backend**. Do not install LibreLogin-Paper on that auth backend; it would create a second authentication pipeline.

At startup, LibreLogin checks the public [`librelogin-fork` Modrinth project](https://modrinth.com/plugin/librelogin-fork) for a newer stable release and prints the correct Paper or Velocity download link. If Modrinth is unavailable, it falls back exclusively to the fork's [GitHub releases](https://github.com/mitsuba7891/LibreLogin-Fork/releases). It only notifies: JAR downloads and replacement remain manual administrator actions.

## Requirements and compatibility

- JDK/Java 25 for builds and Paper 26.2 deployments.
- Paper API 26.2 is the server API used for compilation. Velocity remains a separate artifact.
- A database supported by the generated configuration when using persistent authentication data.
- On Velocity, install PacketEvents 2.13.0+ separately for cross-version QR projection; it is compile-only and is not bundled in the Velocity JAR. The Paper artifact loads its PacketEvents runtime dependency through Libby.
- Optional integrations: Protocolize, LuckPerms, Floodgate and RedisBungee, only when your network uses them.

The shared artifacts retain Java 21 bytecode while being compiled and tested with JDK 25. The **1.13-26.2 compatibility range refers to Minecraft client/protocol versions**, not to running this build on a Paper 1.13 server. A 26.2 server needs the network's ViaVersion/ViaBackwards-style translation layer to accept older clients; PacketEvents observes those protocols but does not replace protocol translation. Test the exact server, proxy and translation-plugin matrix before production.

## Installation: Velocity network

### 1. Install the artifacts

```text
Velocity/plugins/LibreLogin-Velocity-0.25.0.jar
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

Use `LibreLogin-Paper-0.25.0.jar` only when authentication is handled directly by Paper:

```text
Paper/plugins/LibreLogin-Paper-0.25.0.jar
```

Start the server once. LibreLogin generates `config.yml` and `messages.yml` and requests a restart instead of leaving a new unauthenticated instance running; fill in those files before the restarted process starts again. Do not run both the proxy and standalone Paper authentication flows for the same player path.

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

On the current `beta` source, Paper supports manual setup using the displayed secret or provisioning URI without Protocolize. On Velocity, QR delivery is optional. To avoid incompatible map packets while keeping TOTP enabled:

```yaml
totp:
  enabled: true
  qr-enabled: false
```

Restart after changing these settings. Disabling `totp.enabled` does not remove existing account secrets: accounts with 2FA must still verify their second factor. See [2FA and recovery](docs/wiki/Commands-and-2FA.md).

To release `/log` for Carpet or another plugin, set `login-log-alias: false` in `config.yml` and restart. `/login` and `/l` remain available.

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

New MariaDB and PostgreSQL configuration defaults use `verify-full` for TLS and certificate/hostname verification. Existing `jdbc-url` values are preserved when upgrading. The equivalent official MySQL mode is `VERIFY_IDENTITY`.

TLS is configured in `database.properties.mysql.jdbc-url` or `database.properties.postgresql.jdbc-url`; it is not a boolean toggle. The explicit non-TLS values are `sslMode=disable` (MariaDB), `sslMode=DISABLED` (MySQL) and `sslmode=disable` (PostgreSQL). Remote non-TLS connections produce a warning and remain allowed; credentials and password hashes then travel unencrypted. See [database examples](docs/wiki/Configuration.md).

## Upgrade and migration

Legacy HOCON files (`config.conf` and `messages.conf`) are converted to YAML automatically and retained as backup files. Review the generated YAML after migration. Do not delete database or world backups until login, premium mode, 2FA and lobby routing have been tested.

This fork removes the NanoLimbo integration from the supported release architecture. The replacement is a normal registered Paper backend running `AuthLimbo`.

## Building and release files

```bash
# JAVA_HOME must point to JDK 25.
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" \
  :API:test :Plugin:compileJava :Plugin:test :Plugin:platformJars \
  :Plugin:releaseArchive :Plugin:licenseCheck
```

Outputs:

```text
Plugin/build/libs/platform/LibreLogin-Velocity-0.25.0.jar
Plugin/build/libs/platform/LibreLogin-Paper-0.25.0.jar
Plugin/build/libs/platform/AuthLimbo-1.0.0.jar
Plugin/build/distributions/LibreLogin-0.25.0.zip
```

The ZIP contains one folder per component, a README and component changelog for each plugin, the root changelog and the MPL-2.0 license.

## License and attribution

LibreLogin Fork is distributed under the **Mozilla Public License 2.0**. See [`LICENSE`](LICENSE), [`HEADER.txt`](HEADER.txt), [`docs/dependency-licenses.md`](docs/dependency-licenses.md), and the component release directories for notices. This project is not affiliated with or endorsed by the upstream LibreLogin maintainers.

- Upstream project: <https://github.com/kyngs/LibreLogin>
- Fork repository: <https://github.com/mitsuba7891/LibreLogin-Fork>
- Release documentation: [`release/README.md`](release/README.md)
- Release changes: [`CHANGELOG.md`](CHANGELOG.md)
- User wiki: <https://github.com/mitsuba7891/LibreLogin-Fork/wiki>
- Wiki sources: [`docs/wiki/Home.md`](docs/wiki/Home.md)
- LibreLoginProd issue review: [`docs/libreloginprod-issues.md`](docs/libreloginprod-issues.md)
