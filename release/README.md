# LibreLogin Fork — Release package

This is a maintained, customized distribution based on [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin). It separates proxy authentication, standalone Paper authentication and the Paper limbo backend into three clearly named artifacts.

> **Release:** [v0.25.0](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.25.0), with Minecraft 26.3 support, complete paginated help and the `librelogin-fork` identity. The GitHub release includes Paper, Velocity, AuthLimbo, the ZIP and `SHA256SUMS.txt`; record the source commit and verify artifact checksums when deploying. Historical release 0.24.10 work used AI assistance (Freebuff assistant using GPT Luna 5.6); see `CHANGELOG.md` for the change history.

## Package contents

```text
LibreLogin-Velocity/
  LibreLogin-Velocity-0.25.0.jar
  README.md
  CHANGELOG.md
LibreLogin-Paper/
  LibreLogin-Paper-0.25.0.jar
  README.md
  CHANGELOG.md
AuthLimbo/
  AuthLimbo-1.0.0.jar
  README.md
  CHANGELOG.md
README.md
CHANGELOG.md
LICENSE
licenses/
docs/
  dependency-licenses.md
  wiki/
```

## Which plugin should I install?

### Velocity network

Install:

- `LibreLogin-Velocity-0.25.0.jar` on the Velocity proxy.
- `AuthLimbo-1.0.0.jar` on the Paper backend named `auth`.

Do **not** install `LibreLogin-Paper` on that auth backend. LibreLogin-Velocity owns authentication and AuthLimbo only supplies the protected empty limbo world.

### Standalone Paper server

Install only `LibreLogin-Paper-0.25.0.jar` on the Paper server. Do not install LibreLogin-Velocity unless the server is also part of a proxy architecture.

## Dependencies and compatibility

- Java 25 for Paper 26.3 deployments and release builds. Shared bytecode remains Java 21.
- Paper API 26.3 and a compatible Velocity proxy.
- On Velocity, install PacketEvents 2.14.0+ separately for cross-version QR projection; it is not bundled in the Velocity JAR. The Paper artifact loads its PacketEvents runtime dependency through Libby.
- Optional Protocolize, LuckPerms, Floodgate and RedisBungee integrations when used by your network.
- A supported database for persistent authentication data. Drivers are loaded by Libby at runtime.

The maintained 1.13-26.3 range is for client protocols. It does not mean the same JAR is certified as a Paper 1.13 server plugin. Older clients connecting to a newer backend require the appropriate ViaVersion/ViaBackwards-style translation layer. Test the exact live-server matrix before production.

The internal Paper/Velocity plugin ID/name is `librelogin-fork`. New installations use `plugins/librelogin-fork/`; existing legacy data directories are reused without moving or merging files. `/librelogin` and permission nodes remain unchanged. Paper retains the `LibreLogin` provider alias; Velocity dependency declarations must use `librelogin-fork`.

## License and attribution

The original LibreLogin project and this fork are licensed under the **Mozilla Public License 2.0 (MPL-2.0)**. The upstream project is not MIT-licensed. The `MIT` notice in `licenses/FASTLOGIN_LICENSE` applies to a separate FastLogin-derived dependency only. Do not replace `LICENSE` with MIT: retain MPL-2.0 and all upstream notices.

Upstream: <https://github.com/kyngs/LibreLogin>
Fork: <https://github.com/mitsuba7891/LibreLogin-Fork>

## Configuration quick start

1. Start the selected server once to generate `config.yml` and `messages.yml`.
2. Stop the server before changing structural settings.
3. Set the database values in this order: database name, host, port, user, password.
4. For Velocity, set the registered backend name under `limbo` and the post-login destination under `lobby.root`.
5. For AuthLimbo, configure `level-name=auth_void` and the `AuthLimbo:void` generator before first world creation.
6. Back up the database, plugin directory and worlds before migrating from HOCON.

Generated legacy HOCON files are converted automatically to YAML and retained as `.conf.pre-yaml.bak` backups.

The archive includes the current user guides under `docs/wiki/`; start with `docs/wiki/Home.md`. Current source documentation is available [on master](https://github.com/mitsuba7891/LibreLogin-Fork/blob/master/docs/wiki/Home.md); documentation packaged with older releases remains specific to those releases.

First-run file generation is an INFO setup notice, not a fatal authentication failure. Missing optional QR providers also produce an INFO notice: manual 2FA remains available. Use `/librelogin [page]` or the clickable Previous/Next controls to browse all permitted commands.

New database URLs default to verified TLS. Existing URLs are preserved; review their parameters when upgrading. `login-log-alias: false` releases `/log` after a restart.

## Messages and prefix

In `plugins/librelogin-fork/messages.yml` (or the legacy directory reported at startup), every message value is written between double quotes and supports the fork formatting syntax:

```yaml
prefix: "LibreLogin"
```

The value is literal; use an empty value to disable it:

```yaml
prefix: ""
```

**Line breaks** — use `\n` inside a value:

```yaml
info-user: "UUID: %uuid%\nJoined: %joined%"
```

**Centering** — start a line with `[center]` to center it in chat:

```yaml
sub-title-login: "[center]&e/login &b<password>"
```

**Multi-line messages** — a message may be a YAML list; every entry becomes one line:

```yaml
prompt-login:
  - "Line one"
  - "[center]&e&lLine two"
```

Legacy `&` colour codes and MiniMessage syntax (`<bold>`, `<gradient:red:blue>`, `<size:20>`) keep working inside quoted values.

Reload messages with:

```text
/librelogin reload messages
```

## Commands

```text
/register <password> <password>
/login <password>
/login <password> <2fa_code>
/2fa
/2faconfirm <code>
/cracked
/premium
```

Premium/autologin accounts must use `/cracked` before configuring 2FA. Treat QR URLs, TOTP secrets and recovery codes as passwords.

Paper supports manual secret/URI setup. On either platform, `totp.qr-enabled: false` selects text-only setup while `totp.enabled: true` keeps verification enabled. Disabling the provider no longer bypasses an account's saved second factor.

## Known limitations

Tablist textures remain pending. Unit tests do not certify a live Paper/Velocity/client matrix. See the [0.25.0 source notes](notes-v0.25.0.md) and packaged troubleshooting guide for details.

## Build the package

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" \
  :Plugin:compileJava :Plugin:test :Plugin:platformJars :Plugin:releaseArchive :Plugin:licenseCheck
```

## Publish the GitHub Release

Publishing the source branch does not create the `v0.25.0` tag or a GitHub Release. Create and review the release tag separately after validation; the example below requires that tag and creates only a draft. Neither the build nor the CI pipeline publishes it automatically. To authenticate the GitHub CLI without placing a token in shell history:

```bash
gh auth login --hostname github.com --git-protocol ssh --web
```

Alternatively, use a fine-grained token with repository **Contents: Read and write** permission without echoing it:

```bash
read -rsp 'GitHub token: ' GH_TOKEN; export GH_TOKEN; echo
gh auth status
```

After the tag exists, a maintainer can create a draft from the repository root:

```bash
gh release create v0.25.0 \
  Plugin/build/distributions/LibreLogin-0.25.0.zip \
  Plugin/build/libs/platform/LibreLogin-Paper-0.25.0.jar \
  Plugin/build/libs/platform/LibreLogin-Velocity-0.25.0.jar \
  Plugin/build/libs/platform/AuthLimbo-1.0.0.jar \
  --repo mitsuba7891/LibreLogin-Fork \
  --title "LibreLogin Fork 0.25.0" \
  --draft --verify-tag \
  --notes-file release/notes-v0.25.0.md
```

Do not paste the token into Git, a README, a shell script or this chat. SSH authenticates Git operations; GitHub Releases use the API and therefore require `gh auth` or `GH_TOKEN`.
