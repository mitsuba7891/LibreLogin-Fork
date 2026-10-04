# LibreLogin Fork 0.25.0-beta.4

## English

**Testing pre-release for Paper and Velocity.** Beta.4 removes the misleading SQLite Hikari warning and retains the beta.3 startup/spawn fixes.

### Changes since beta.3

- Disabled Hikari keepalive pings for the local SQLite connector. The database behavior is unchanged; the `keepaliveTime is greater than or equal to maxLifetime` warning is gone.
- Retained first-run restart behavior and `AsyncPlayerSpawnLocationEvent` handling from beta.3.
- The selected database remains controlled by `config.yml`; replacing the JAR does not change `database.type` or credentials.

### Downloads

| File | Install on |
|---|---|
| `LibreLogin-Paper-0.25.0.jar` | Standalone Paper server |
| `LibreLogin-Velocity-0.25.0.jar` | Velocity proxy |
| `AuthLimbo-1.0.0.jar` | Paper authentication backend for the Velocity architecture |
| `LibreLogin-0.25.0.zip` | Complete bundle and English guides |
| `SHA256SUMS.txt` | SHA-256 checksums |

JAR names retain the base version `0.25.0`; use the **beta.4 tag and checksums**. The release is a pre-release and still requires live Paper/Velocity/client testing.

[English wiki](https://github.com/mitsuba7891/LibreLogin-Fork/wiki) · [Versioned guides](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.4/docs/wiki/Home.md) · [Compare with beta.3](https://github.com/mitsuba7891/LibreLogin-Fork/compare/v0.25.0-beta.3...v0.25.0-beta.4)

## Español

Beta.4 elimina el warning engañoso de Hikari para SQLite desactivando los keepalive innecesarios. Mantiene los arreglos de beta.3: reinicio tras generar la configuración inicial y `AsyncPlayerSpawnLocationEvent` en Paper.

El JAR no cambia `database.type`, usuarios ni contraseñas del `config.yml`. Esta publicación es pre-release; usa el tag beta.4 y `SHA256SUMS.txt`.
