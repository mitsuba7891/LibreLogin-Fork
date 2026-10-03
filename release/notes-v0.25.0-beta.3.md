# LibreLogin Fork 0.25.0-beta.3

## English

**Testing pre-release for Paper and Velocity.** This release contains the startup and Paper spawn fixes after beta.2. It is not a stable certification of every server/client combination.

### Changes since beta.2

- First-run configuration generation now requests a Paper restart or shuts down the Velocity proxy instead of leaving an empty authentication configuration active.
- Paper no longer registers the deprecated `PlayerSpawnLocationEvent`; beta.3 uses `AsyncPlayerSpawnLocationEvent`.
- Paper chooses the initial destination from the pre-login profile UUID and stores the post-login location cache by UUID.
- English wiki and release documentation now explain the first-start restart behavior and the host restart-supervisor requirement.
- Verification: compilation, 73 tests, `licenseCheck`, platform JARs and release archive all pass.

### Downloads and installation

| File | Install on |
|---|---|
| `LibreLogin-Paper-0.25.0.jar` | Standalone Paper server |
| `LibreLogin-Velocity-0.25.0.jar` | Velocity proxy |
| `AuthLimbo-1.0.0.jar` | Paper authentication backend for the Velocity architecture |
| `LibreLogin-0.25.0.zip` | Complete bundle, English guides and licence notices |
| `SHA256SUMS.txt` | SHA-256 checksums of the four files above |

JAR names and embedded LibreLogin version retain the base `0.25.0`. Use the **beta.3 tag and checksums** to distinguish this build from beta.1 and beta.2. Do not install LibreLogin-Paper on the same auth backend served by LibreLogin-Velocity.

Paper 26.2 and the build use Java 25; LibreLogin bytecode remains Java 21. A restart supervisor must relaunch a process after Paper `Server.restart()` or Velocity shutdown. Without one, start the process manually after filling the generated configuration.

### Upgrade notes and limitations

- Back up the database, plugin directory and worlds before upgrading.
- Existing configuration values are preserved. Review old JDBC URLs: new MariaDB/PostgreSQL defaults use `verify-full`; official MySQL uses `VERIFY_IDENTITY`.
- Saved 2FA factors still require verification. If the provider is disabled, enable it and restart or use account-specific administrator recovery.
- Tablist textures (#73) remain pending. Live Paper/Velocity/client smoke testing is still required; unit tests do not start Minecraft.
- The TOTP 1.7.1 licence is verified as MIT and bundled in the JARs and ZIP. The dependency report remains a partial direct-dependency inventory.

[English wiki](https://github.com/mitsuba7891/LibreLogin-Fork/wiki) · [Versioned guides](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.3/docs/wiki/Home.md) · [Issue review](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.3/docs/libreloginprod-issues.md) · [Compare with beta.2](https://github.com/mitsuba7891/LibreLogin-Fork/compare/v0.25.0-beta.2...v0.25.0-beta.3)

---

## Español

**Pre-release de pruebas para Paper y Velocity.** Incluye los arreglos del primer arranque y del spawn de Paper posteriores a beta.2.

- Al generar la configuración inicial, Paper solicita reinicio y Velocity se apaga para que el supervisor lo vuelva a levantar; ya no queda una instancia con autenticación vacía activa.
- Paper usa `AsyncPlayerSpawnLocationEvent` en lugar del evento obsoleto y selecciona el destino usando el UUID del perfil pre-login.
- La caché de ubicación post-login usa UUID.
- Wiki y documentación en inglés actualizadas con el comportamiento del primer arranque.
- Compilación, 73 tests, `licenseCheck`, JARs por plataforma y ZIP correctos.

Los nombres de los JAR mantienen la versión base `0.25.0`; usa el **tag beta.3 y sus checksums**. La estable continúa siendo v0.24.12. Siguen pendientes las texturas de tablist y las pruebas reales de la matriz Paper/Velocity/clientes.

[Wiki en inglés](https://github.com/mitsuba7891/LibreLogin-Fork/wiki) · [Guías de beta.3](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.3/docs/wiki/Home.md) · [Comparar beta.2](https://github.com/mitsuba7891/LibreLogin-Fork/compare/v0.25.0-beta.2...v0.25.0-beta.3)
