# LibreLogin-Paper

Standalone Paper authentication based on [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin). This fork artifact was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6); see the repository `CHANGELOG.md` for the full change list.

## When to use it

Use this artifact when Paper itself handles authentication and no proxy-side LibreLogin is handling the same player path. For a Velocity network with `LibreLogin-Velocity`, install `AuthLimbo` on the auth backend instead.

## Install

Copy the JAR into Paper:

```text
Paper/plugins/LibreLogin-Paper-0.25.0.jar
```

Start once to generate `config.yml` and `messages.yml`, stop the server, configure them, then start again. Back up the database and plugin data before upgrades.

## Configuration

Important generated sections include:

- `database`: database name, host, port, user and password.
- `limbo`: unauthenticated-player worlds.
- `lobby`: post-authentication destination.
- `totp`: TOTP/2FA settings.
- `use-titles` and `use-action-bar`: login notifications.

Legacy `config.conf` and `messages.conf` files are converted to YAML and retained as `.conf.pre-yaml.bak` backups.

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

Premium/autologin accounts must run `/cracked` before configuring 2FA.

In beta.2, TOTP works through the manual secret/provisioning URI without a QR projector. Disabling `totp.enabled` does not bypass a saved second factor; recovery must be performed explicitly for the account. User guides are included in the bundle's `docs/wiki/` directory and [online at the release tag](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.2/docs/wiki/Home.md).

`login-log-alias: false` disables only `/log` after restarting, preserving `/login` and `/l`. Review existing database URLs because upgraded explicit values are preserved even though new defaults use verified TLS.

This is a pre-release. Tablist textures and the transition to the modern Paper spawn event remain pending live-server validation.

## Messages

```yaml
prefix: "LibreLogin"
```

Use `prefix: ""` to disable it. Reload messages with `/librelogin reload messages` where supported, or restart the server.

## Dependencies

- Java 25 for Paper 26.2.
- Compiled and tested against Paper API 26.2; the 1.13-26.2 range is a client/protocol range, not a Paper-server range.
- Runtime libraries and database drivers loaded through Libby.
- Optional LuckPerms and Floodgate integrations.
- PacketEvents 2.13.0+ is declared as a runtime library for this Paper artifact and is loaded through Libby; do not install a second copy unless your server setup explicitly requires it.

Older clients require the server/network's protocol translation layer. PacketEvents supplies packet compatibility to LibreLogin but does not translate a 1.13 client into the 26.2 server protocol by itself.

## License

This artifact is distributed under the upstream Mozilla Public License 2.0. See the repository `LICENSE`; do not replace it with MIT. The original project is <https://github.com/kyngs/LibreLogin>.
