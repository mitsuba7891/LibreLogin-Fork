# LibreLogin Fork 0.25.0 — Master source update

This update promotes the beta work to `master`. A GitHub release/tag and public artifact upload are separate publication steps.

## Included changes

- Complete `/librelogin [page]` and `/librelogin help [page]` help from the live command registry: six commands per page, visible descriptions, aliases, permissions and clickable Previous/Next navigation. Account/staff command clicks are suggestions, never automatic actions.
- Internal Paper/Velocity plugin identity `librelogin-fork`. Existing authentication directories are reused without moving, merging or replacing user data. Commands, permission nodes and database provider IDs are unchanged. Paper retains the `LibreLogin` provider alias; Velocity integration dependencies must use the new ID.
- First-run configuration generation is an informational setup event, without duplicate fatal/refusal announcements. Actual startup failures still refuse authentication.
- Paper API `26.3.build.206-stable` and PacketEvents `2.14.0`. The QR projector accepts Minecraft client protocol 777 (26.3) and checks that the installed PacketEvents actually contains those mappings.
- Missing optional QR integrations produce an INFO notice explaining manual secret/URI setup. Install PacketEvents 2.14.0+ on Velocity for QR maps through 26.3, or use `totp.qr-enabled: false` while keeping TOTP verification enabled. Protocolize remains a legacy fallback through client 1.21.3.
- Retains the beta's session, premium-profile, login-nonce, routing, TLS, password-policy, SQLite and async Paper spawn fixes.

PacketEvents' upstream [2.14.0 release notes](https://github.com/retrooper/packetevents/releases/tag/v2.14.0) confirm the 26.3 mappings. The 1.13–26.3 range describes client protocols, not certification for all historical Paper server versions; protocol translation remains the network's responsibility.

## Upgrade and verify

Back up the database, plugin folders and worlds. Keep one authentication JAR per instance and do not install LibreLogin-Paper on a proxied AuthLimbo backend. Check the selected data-directory path at startup before editing configuration.

Build with JDK 25, retaining the `0.25.0` artifact version:

```bash
./gradlew -PnoBump test checkLicenses :Plugin:platformJars :Plugin:releaseArchive
```

The Paper/Velocity JARs and AuthLimbo companion are in `Plugin/build/libs/platform/`; the bundle is `Plugin/build/distributions/LibreLogin-0.25.0.zip`.

Automated tests and successful compilation do not replace a live Paper/Velocity/client matrix test. Tablist textures and the exact deployed protocol-translation stack still require verification before production.
