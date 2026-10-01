# LibreLogin-Velocity changelog

This fork artifact was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6).

## 0.25.0-beta.2

- Restricted unauthenticated and pending-2FA players to configured limbo backends, including effective redirected destinations.
- Preserved backend connection failure reasons and handled missing connection results.
- Added optional QR delivery and text-only 2FA setup; disabled providers no longer bypass account factors.
- Hardened registered-account IP sessions and made `/log` optional.
- Updated user guides and bundled the verified MIT notice for TOTP; retains the beta.1 hardening.

## 0.24.10

- Added a Velocity-asset-aware fallback to the fork's GitHub releases when the primary Modrinth update check fails.
- The checker remains notification-only and never contacts the official LibreLogin release feed.

## 0.24.9

- Update notices now query `librelogin-fork` on Modrinth instead of the official LibreLogin GitHub repository.
- The checker recommends the Velocity-specific stable file and never downloads or replaces the installed JAR.

## 0.24.8

- Added shared session/password/attempt services, legacy-hash upgrades and account cooldowns that survive reconnects.
- Replaced deprecated event ordering and moved signed-chat blocking to the required AuthLimbo backend to avoid modern client kicks.
- Hardened premium/session validation and removed sensitive authentication data from logs.
- Maintained client/protocol handling from 1.13 through 26.2; protocol translation remains the network's responsibility.

## 0.24.7

- Every message value in the generated `messages.yml` is written between double quotes; messages may use `\n` line breaks, `[center]` centering and YAML list syntax for multi-line messages.

## 0.24.6

- Proxy-side authentication artifact separated from the Paper artifact.
- Fixed the Adventure `Title.Times.of(...)` runtime crash by using the compatible `Title.Times.times(...)` API.
- Added combined `/login <password> <2fa_code>` guidance and 2FA title/subtitle improvements.
- Added manual TOTP provisioning output when QR projection is unavailable.
- Added premium/autologin protection: `/2fa` requires `/cracked` first.
- Updated PacketEvents integration to 2.13.0 for newer Paper/Minecraft protocol handling. Install PacketEvents separately; it is not bundled.
- Kept Protocolize optional and isolated under the Velocity integration package.
- Added readable YAML migration, guide comments, configurable prefix and quoted premium throttling message.
- Added MySQL/MariaDB URL driver selection and updated runtime database libraries.
- Removed NanoLimbo and BungeeCord integration paths from the supported architecture.

See the root [`CHANGELOG.md`](../CHANGELOG.md) for the complete fork history and [`README.md`](README.md) for installation.
