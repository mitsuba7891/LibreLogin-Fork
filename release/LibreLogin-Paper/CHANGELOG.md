# LibreLogin-Paper changelog

This fork artifact was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6).

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
