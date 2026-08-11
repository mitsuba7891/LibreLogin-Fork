# Changelog

## 0.24.10 — GitHub update-check fallback

- Modrinth remains the primary update source; failures now fall back to public releases from `mitsuba7891/LibreLogin-Fork` on GitHub.
- The fallback never queries the official LibreLogin repository and recommends only the matching Paper or Velocity stable artifact.
- GitHub is not queried when Modrinth responds successfully, and neither source can download or replace the installed JAR.
- Added tests for fallback activation, primary short-circuiting, dual failures and GitHub asset selection.

## 0.24.9 — Modrinth update notifications

- Replaced the official LibreLogin GitHub update source with the public `librelogin-fork` project on Modrinth.
- Stable semantic releases are compared against the installed version and the Paper/Velocity-specific file is recommended.
- Console notices include the direct download and project URLs; no file is downloaded or replaced automatically.
- Added Modrinth API timeouts, an identifying `User-Agent` and regression tests for release/file selection.

## 0.24.8 — Security and Paper 26.2 update

### Authentication and security

- Added dedicated in-memory session caching, account-based brute-force cooldowns and centralized password verification with transparent legacy-hash upgrades.
- New configurations default to Argon2id, five attempts, a 60-second cooldown, eight-character passwords and a 120-second authentication deadline; existing values are retained.
- Fixed session IP validation, fail-open Mojang session responses, nonce replay paths, malformed TOTP attempt accounting and sensitive command logging.
- Expanded Paper/AuthLimbo protection to block chat, movement, commands, interactions, damage/projectiles and inventory/item actions until authentication.

### Performance and compatibility

- Moved network and database work away from the Paper main thread and cached profiles throughout the login pipeline.
- Closed JDBC resources deterministically and optimized per-IP account counting with database-side `COUNT(*)` queries.
- Compiled and tested with JDK 25 and Paper API 26.2 while retaining Java 21 bytecode for shared artifacts.
- Maintained client/protocol handling from 1.13 through 26.2; older clients require a protocol translation layer.
- Updated PacketEvents to 2.13.0, removed dead code and added cryptography/logging regression tests.

## 0.24.7 — Message formatting upgrade (AI-assisted)

This release was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6) and applies to the LibreLogin fork.

### Message formatting (messages.yml)

- Every scalar message value in the generated `messages.yml` is now written between double quotes (`"..."`), including multiline values, list entries and single-quoted values the YAML serializer previously emitted unquoted or with single quotes.
- Message values may be written as YAML lists; every list entry becomes one chat line:

  ```yaml
  prompt-login:
    - "Line one"
    - "[center]&e&lLine two"
  ```

- `\n` inside a value creates a line break (e.g. `"Line one\nLine two"`).
- `[center]` at the start of a line centers that line using pixel-based measurement.
- MiniMessage and legacy `&` colour codes keep working inside the quoted values.
- Added a new header to the generated `messages.yml` documenting the new syntax and the AI-assisted review.

### Configuration and messages (continued)

- Configurable literal chat prefix from `messages.yml` (`prefix: "LibreLogin"`, empty to disable), excluded from titles, subtitles, action bars and emails.
- `/login <password> <2fa_code>` guidance for users with TOTP enabled.
- Premium/autologin accounts cannot enable 2FA until they switch back to cracked with `/cracked`.

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
