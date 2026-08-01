# LibreLogin-Velocity changelog

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
