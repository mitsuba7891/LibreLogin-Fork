# LibreLogin-Paper changelog

This fork artifact was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6).

## 0.24.10

- Added a Paper-asset-aware fallback to the fork's GitHub releases when the primary Modrinth update check fails.
- The checker remains notification-only and never contacts the official LibreLogin release feed.

## 0.24.9

- Update notices now query `librelogin-fork` on Modrinth instead of the official LibreLogin GitHub repository.
- The checker recommends the Paper-specific stable file and never downloads or replaces the installed JAR.

## 0.24.8

- Added centralized Argon2id/legacy password handling, reconnect-resistant attempt cooldowns and session caching.
- Moved recurring network/database work off the Paper thread and closed JDBC resources deterministically.
- Added complete unauthenticated chat, movement, interaction, combat, inventory and item blocking.
- Compiled/tested against Paper API 26.2 with JDK 25; 1.13-26.2 denotes client protocols, not Paper server versions.
- Hardened Mojang session verification, login nonces, session IP checks and sensitive logging.

## 0.24.7

- Every message value in the generated `messages.yml` is written between double quotes; messages may use `\n` line breaks, `[center]` centering and YAML list syntax for multi-line messages.

## 0.24.6

- Added a platform-filtered standalone Paper artifact.
- Hardened Paper startup for the modern Paper plugin lifecycle, including null-safe shutdown handling.
- Added vehicle protection while players are in the login limbo.
- Updated PacketEvents support for newer Paper version strings and protocol mappings; install it separately where required.
- Added readable YAML configuration/messages, automatic HOCON migration backups and per-key guide comments.
- Added configurable message prefix, combined password/TOTP login guidance and premium/autologin 2FA safeguards.
- Updated database drivers, Libby metadata generation and MySQL/MariaDB URL selection.
- Removed obsolete BungeeCord and NanoLimbo paths from the supported build.

See the root [`CHANGELOG.md`](../CHANGELOG.md) for the complete fork history and [`README.md`](README.md) for standalone installation.
