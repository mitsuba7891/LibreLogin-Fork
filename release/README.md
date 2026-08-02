# LibreLogin Fork — Release package

This is a maintained, customized distribution based on [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin). It separates proxy authentication, standalone Paper authentication and the Paper limbo backend into three clearly named artifacts.

> **AI-assisted update:** This release package and the fork changes it ships (release 0.24.7 and the message-formatting upgrade) were reviewed and updated with AI assistance (GPT Luna 5.6). See `CHANGELOG.md` for the complete list of changes.

## Package contents

```text
LibreLogin-Velocity/
  LibreLogin-Velocity-0.24.7.jar
  README.md
  CHANGELOG.md
LibreLogin-Paper/
  LibreLogin-Paper-0.24.7.jar
  README.md
  CHANGELOG.md
AuthLimbo/
  AuthLimbo-1.0.0.jar
  README.md
  CHANGELOG.md
README.md
CHANGELOG.md
LICENSE
```

## Which plugin should I install?

### Velocity network

Install:

- `LibreLogin-Velocity-0.24.7.jar` on the Velocity proxy.
- `AuthLimbo-1.0.0.jar` on the Paper backend named `auth`.

Do **not** install `LibreLogin-Paper` on that auth backend. LibreLogin-Velocity owns authentication and AuthLimbo only supplies the protected empty limbo world.

### Standalone Paper server

Install only `LibreLogin-Paper-0.24.7.jar` on the Paper server. Do not install LibreLogin-Velocity unless the server is also part of a proxy architecture.

## Dependencies

- Java 21 or newer.
- Paper/Velocity versions compatible with your selected Minecraft release.
- On Velocity, install PacketEvents 2.13.0+ separately for cross-version QR projection; it is not bundled in the Velocity JAR. The Paper artifact loads its PacketEvents runtime dependency through Libby.
- Optional Protocolize, LuckPerms, Floodgate and RedisBungee integrations when used by your network.
- A supported database for persistent authentication data. Drivers are loaded by Libby at runtime.

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

## Messages and prefix

In `plugins/librelogin/messages.yml` every message value is written between double quotes and supports the fork formatting syntax:

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

## Build the package

```bash
./gradlew :API:test :Plugin:test :Plugin:platformJars :Plugin:releaseArchive --no-daemon -PnoBump
```

## Publish the GitHub Release

The repository already contains the source commit and tag. To authenticate the GitHub CLI on the VPS without placing a token in shell history:

```bash
gh auth login --hostname github.com --git-protocol ssh --web
```

Alternatively, use a fine-grained token with repository **Contents: Read and write** permission without echoing it:

```bash
read -rsp 'GitHub token: ' GH_TOKEN; export GH_TOKEN; echo
gh auth status
```

Then create the release from the repository root:

```bash
gh release create v0.24.6 \
  Plugin/build/distributions/LibreLogin-0.24.6.zip \
  Plugin/build/libs/platform/LibreLogin-Paper-0.24.6.jar \
  Plugin/build/libs/platform/LibreLogin-Velocity-0.24.6.jar \
  Plugin/build/libs/platform/AuthLimbo-1.0.0.jar \
  --repo mitsuba7891/LibreLogin-Fork \
  --title "LibreLogin Fork 0.24.6" \
  --notes-file release/CHANGELOG.md
```

Do not paste the token into Git, a README, a shell script or this chat. SSH authenticates Git operations; GitHub Releases use the API and therefore require `gh auth` or `GH_TOKEN`.
