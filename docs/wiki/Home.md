# Welcome to the LibreLogin Fork Wiki

**Authentication for standalone Paper servers and Velocity networks.**

[Download beta.5](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.25.0-beta.5) · [Installation](Installation.md) · [Configuration](Configuration.md) · [Troubleshooting](Troubleshooting.md)

[LibreLogin Fork](https://github.com/mitsuba7891/LibreLogin-Fork) is a maintained fork of LibreLogin. This wiki explains its features, setup and account-management commands in **English**.

## Start here

| Your setup | Install | Next step |
|---|---|---|
| **Standalone Paper** | `LibreLogin-Paper-0.25.0.jar` on Paper | [Standalone installation](Installation.md#standalone-paper) |
| **Velocity network** | `LibreLogin-Velocity-0.25.0.jar` on the proxy and `AuthLimbo-1.0.0.jar` on the Paper auth backend | [Velocity setup](Installation.md#velocity-network-with-authlimbo) |

> **Important:** AuthLimbo protects the waiting backend; it does not authenticate accounts. In a Velocity network, LibreLogin-Velocity handles authentication. Install the standalone Paper authentication plugin only for the standalone architecture.

## Features

- **[Premium autologin](Commands-and-2FA.md#premium-autologin)** — authenticate premium accounts through a valid Mojang session.
- **[Two-factor authentication](Commands-and-2FA.md#enable-2fa-without-a-map)** — set up an authenticator using a manual secret or provisioning URI; QR delivery is optional.
- **[IP-based sessions](Configuration.md#accounts-and-sessions)** — optional reconnection sessions for registered accounts, disabled by default.
- **[Email recovery](Commands-and-2FA.md#email-recovery)** — link and verify a recovery address when SMTP is configured.

## Commands

| Task | Guide |
|---|---|
| Register, log in or change a password | [Account commands](Commands-and-2FA.md#account-commands) |
| Enable or disable premium autologin | [Premium autologin](Commands-and-2FA.md#premium-autologin) |
| Enable 2FA or recover an account | [2FA setup and recovery](Commands-and-2FA.md#enable-2fa-without-a-map) |
| Manage accounts and permissions | [Staff commands](Commands-and-2FA.md#staff-commands-and-permissions) |

## Guides and configuration

- [Installation and forwarding](Installation.md)
- [Limbo and lobby routing](Configuration.md#limbo-and-lobby)
- [Databases and TLS modes](Configuration.md#database)
- [Messages and formatting](Configuration.md#messages)
- [Upgrade and migration](Troubleshooting.md#upgrading-an-existing-installation)
- [Common problems and known issues](Troubleshooting.md)

## API and development

- [Build requirements and verification](API-and-Build.md#verify-the-project)
- [Platform JARs and release bundle](API-and-Build.md#package-the-artifacts)
- [Integration interfaces](API-and-Build.md#integration-api)
- [Local Javadoc](API-and-Build.md#generate-local-javadoc)

## Important before installing

> **Java and compatibility:** Building and running Paper 26.2 require **Java 25**. LibreLogin keeps Java 21 bytecode. The advertised 1.13–26.2 range describes **client protocols**, not certification for every older Paper server; older clients need the appropriate protocol translation layer.

> **Release status:** These guides cover **v0.25.0-beta.5**, a **pre-release**. `/librelogin` provides clickable administrator help; initial configuration requests a restart, Paper uses the modern async spawn event, and SQLite no longer emits the Hikari keepalive warning. The [known issues](Troubleshooting.md#known-issues) include Paper tablist textures and live-server validation. The current distribution provides Paper, Velocity and AuthLimbo artifacts; BungeeCord and NanoLimbo are not supported outputs.

- [Stable release: v0.24.12](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.24.12).
- [Testing pre-release: v0.25.0-beta.5](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.25.0-beta.5).
- JAR names retain the base version `0.25.0`. Use the **release tag and checksums** to distinguish beta builds. Beta.1–beta.4 do not include the beta.5 interactive help.
- This wiki is maintained in English. Release archives preserve the documentation snapshot packaged with that release.

## Support and license

Report problems in [this fork's issue tracker](https://github.com/mitsuba7891/LibreLogin-Fork/issues), including the JAR, server/proxy, Java and client versions, translation plugins, and reproduction steps. Remove passwords, forwarding secrets, TOTP secrets and personal data before sharing logs or configuration.

LibreLogin retains **MPL-2.0** and its original notices. The MIT notices for FastLogin and TOTP apply to those dependencies. See [LICENSE](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/LICENSE) and the [dependency license report](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/dependency-licenses.md). The [cross-fork issue review](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/libreloginprod-issues.md) records the status of reports from LibreLoginProd.
