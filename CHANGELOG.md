# Changelog

All notable changes in this fork are documented here. This release is based on [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin) and preserves its Mozilla Public License 2.0 notices.

## 0.24.6 — Fork documentation and release update

### Major architecture and platform changes

- Added separate, platform-labelled artifacts:
  - `LibreLogin-Velocity-0.24.6.jar` for proxy-side authentication.
  - `LibreLogin-Paper-0.24.6.jar` for standalone Paper authentication.
  - `AuthLimbo-1.0.0.jar` for the Paper backend used as a Velocity limbo.
- Added a release ZIP with one installation folder, README and changelog per component.
- Removed the obsolete BungeeCord platform, metadata and dependencies from the active build.
- Removed NanoLimbo from the supported release architecture. A normal registered Paper backend running AuthLimbo now provides the limbo world and movement lock.
- Added AuthLimbo as a small independent Paper plugin that creates/uses `auth_void`, keeps unauthenticated players in the void limbo and blocks movement, teleport abuse, interaction, damage, item drops and inventory actions.
- Kept the Java 21 bytecode baseline and documented that newer JVMs do not automatically certify every newer Minecraft or proxy release.
- Retained the source and executable distribution obligations of the upstream MPL-2.0 project and added fork attribution throughout the release documentation.

### Authentication, routing and 2FA

- Fixed the Velocity authorization crash caused by Adventure API incompatibility by replacing removed `Title.Times.of(...)` usage with `Title.Times.times(...)`.
- Added `/login <password> <2fa_code>` guidance so users can submit password and TOTP code together.
- Added the 2FA placeholder to login prompts/titles where applicable.
- Added manual TOTP secret/provisioning-URI output when QR projection is unavailable, so users are not blocked by a missing image integration.
- Reused the validated Protocolize projector during proxy 2FA setup and isolated the optional Protocolize implementation under the Velocity integration package.
- Added checks so `/2fa` and `/2faconfirm` are unavailable while premium/autologin is active; users must use `/cracked` first.
- Updated PacketEvents to 2.13.0 for newer Paper version strings and protocol mappings. Velocity uses PacketEvents as a separately installed external plugin; the Paper artifact loads its runtime library through Libby.
- Preserved the safe premium validation policy: Mojang, PlayerDB and MineTools are tried in sequence, while a complete API outage fails closed instead of allowing unsafe offline impersonation.
- Prevented players from carrying vehicles into Paper limbo during login, addressing the horse-death reproduction from upstream issue #402 pending live-server confirmation.

### Configuration and messages

- Migrated active configuration and messages to readable block-style YAML.
- Added automatic HOCON-to-YAML conversion with `.conf.pre-yaml.bak` backups.
- Added per-key guide comments to generated `config.yml`, including the mail section and configuration revision.
- Preserved legacy values during migration and added atomic config writes with validation reloads.
- Added a configurable literal message prefix from `messages.yml`:

  ```yaml
  prefix: "LibreLogin"
  ```

  Set `prefix: ""` to disable it. Prefixes remain excluded from titles, subtitles, action bars and email templates.
- Corrected `kick-premium-error-throttled` to a quoted single-line YAML scalar.
- Documented the database field order as database name, host, port, user and password.
- Added official MySQL Connector/J alongside MariaDB Connector/J. The connector selects MariaDB for `jdbc:mariadb://` and MySQL for `jdbc:mysql://`.
- Preserved separate username/password configuration fields rather than embedding credentials in JDBC URLs.

### Build, dependency and release tooling

- Replaced the unavailable Libby Gradle plugin with a self-contained `generateLibbyJson` task that emits repositories, relocations and SHA-256 Base64 checksums.
- Kept runtime libraries compile-only where they are downloaded by Libby at startup.
- Updated MariaDB Connector/J, MySQL Connector/J, HikariCP, SQLite JDBC and PostgreSQL JDBC versions.
- Retained the required LGPL/native-resource dependency notices in `docs/dependency-licenses.md`.
- Replaced legacy license-header and Blossom tooling with native Gradle tasks and generated Velocity sources.
- Upgraded the Gradle wrapper to 9.6.1 and Shadow to 8.3.11.
- Added `platformJars` and `releaseArchive` tasks with platform filtering and reproducible release layout.
- Removed the obsolete `mcupload` publication plugin and its credentials from CI; external publication is now explicit and platform-aware.
- Updated CI artifact verification to inspect the separate Paper and Velocity JARs without retired PaperMC download endpoints.
- Added regression tests for YAML block style, guide comments, message-key integrity, HOCON migration and legacy-value preservation.

### Upstream baseline retained in this fork

The fork carries the upstream LibreLogin functionality and history, including premium autologin, password registration/login, sessions, case-sensitive name validation, database migrations, player-data migration, Floodgate/Geyser support, TOTP 2FA, API providers, Paper and Velocity integrations, and the public API module. The full upstream history remains available in Git.

### Compatibility and limitations

- Build and release artifacts target Java 21 bytecode.
- PacketEvents 2.13.0+ must be installed separately where QR projection is required.
- Protocolize remains optional and is subject to its own protocol ceiling and compatibility matrix.
- Paper/Velocity/Minecraft combinations, especially future 26.x combinations, require live-server smoke testing.
- This changelog records fork changes; it is not a guarantee that every client protocol or platform build is compatible.

## 0.24.0 — Upstream baseline

- Added support for Minecraft 1.21.4.
- Added support for Java 23 build environments.
- Fixed the Paper “logged in from another location” issue from upstream issue #296.

For earlier upstream history, see the original project: <https://github.com/kyngs/LibreLogin/blob/master/CHANGELOG.md>.
