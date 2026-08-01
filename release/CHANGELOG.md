# Changelog

## 0.24.6 fork release

### Major changes

- Added three release artifacts with separate installation responsibilities:
  - `LibreLogin-Velocity-0.24.6.jar` for proxy-side authentication.
  - `LibreLogin-Paper-0.24.6.jar` for standalone Paper authentication.
  - `AuthLimbo-1.0.0.jar` for a registered Paper limbo backend used with Velocity.
- Removed NanoLimbo from the supported architecture and release dependencies. The limbo backend is now a regular registered Paper server.
- Added `AuthLimbo`, a small independent Paper plugin that creates/uses `auth_void`, keeps players in a void world and blocks movement/interactions while they are in the proxy limbo.
- Kept authentication on Velocity for the proxy deployment. `LibreLogin-Paper` must not be installed on the proxied `auth` backend in that architecture.
- Added the modern Paper/Velocity forwarding setup and documented the required matching forwarding secret.
- Added a release archive with three clearly named folders, one README per component, and this changelog.

### Authentication and 2FA

- Added `/login <password> <2fa_code>` guidance to prompts and titles.
- Added the 2FA placeholder to the login subtitle.
- Added manual TOTP secret/provisioning-URI output when QR projection is unavailable.
- Added PacketEvents 2.13.0 as the preferred cross-version QR projection path; Protocolize remains optional where compatible.
- Premium/autologin users cannot start `/2fa` or `/2faconfirm` until they disable autologin with `/cracked`.
- Premium API lookups try Mojang, PlayerDB and MineTools in sequence; a complete API outage still fails closed instead of allowing an unsafe offline premium bypass.

### Configuration and messages

- Migrated active configuration and messages to readable YAML with automatic legacy HOCON conversion and backups.
- Added a configurable literal message prefix:

  ```yaml
  prefix: "LibreLogin"
  ```

  Set `prefix: ""` to disable it. Prefixes do not apply to titles, subtitles, action bars or email templates.
- Corrected the rate-limit message to a quoted, single-line YAML scalar:

  ```yaml
  kick-premium-error-throttled: "The Mojang API is rate limiting our server, please try joining again in a while!"
  ```
- Generated YAML includes per-key guide comments and preserves legacy values during migration.
- Database configuration is documented in the order database name, host, port, user and password.

### Platform and dependency changes

- Paper and Velocity artifacts are built separately and filtered by platform.
- Runtime libraries are described by generated `libby.json`; database drivers are loaded at runtime.
- Updated MariaDB, MySQL, HikariCP, SQLite and PostgreSQL runtime dependencies.
- Removed obsolete BungeeCord platform files and dependencies.
- Removed the obsolete NanoLimbo integration and dependency paths.
- Retained Java 21 bytecode as the release baseline.

### Upgrade notes

- Back up the database, plugin data folder and server worlds before upgrading.
- On a Velocity network, install only `LibreLogin-Velocity` on the proxy and install `AuthLimbo` on the Paper `auth` backend.
- Set the backend `level-name=auth_void`, `allow-flight=true`, and declare `AuthLimbo:void` in `bukkit.yml` before the first generation of `auth_void`.
- Configure `limbo: [auth]` and `lobby.root: [lobby]` in the Velocity LibreLogin configuration.
- Reload messages after editing with `/librelogin reload messages`, or restart the proxy for jar changes.
