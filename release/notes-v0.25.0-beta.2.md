# LibreLogin Fork 0.25.0-beta.2

## English

**Testing pre-release for Paper and Velocity.** This release contains the fixes added after beta.1; it is not a stable-release certification of every server/client combination.

### Changes since beta.1

- Configurable `/log` alias: set `login-log-alias: false` and restart to keep `/login` and `/l` while releasing `/log` for Carpet or another plugin.
- Velocity forwards the actual connection failure reason and handles failed requests without a result.
- Paper supports manual 2FA setup with a secret/provisioning URI without a map projector.
- Optional QR delivery: set `totp.qr-enabled: false` for text-only setup. Keep `totp.enabled: true` for TOTP verification.
- Accounts with a saved 2FA factor cannot bypass it when the TOTP provider is disabled. Account recovery remains an explicit administrator action.
- Unauthenticated and pending-2FA Velocity players cannot enter non-limbo backends, including redirected destinations.
- Unregistered offline accounts cannot reuse stale IP sessions; incomplete, expired and future-dated session metadata is rejected.
- Paper validates both username and expected premium UUID in the Mojang session response. Login nonces use constant-time comparison.
- README badges, Spanish user guides and a review of all 22 LibreLoginProd issues are included.
- TOTP 1.7.1's MIT license was verified in its parent POM and exact source tag; its original notice is bundled in the LibreLogin JARs and release ZIP.

### Downloads and installation

| File | Install on |
|---|---|
| `LibreLogin-Paper-0.25.0.jar` | Standalone Paper server |
| `LibreLogin-Velocity-0.25.0.jar` | Velocity proxy |
| `AuthLimbo-1.0.0.jar` | Paper authentication backend for the Velocity architecture |
| `LibreLogin-0.25.0.zip` | Complete bundle, installation guides and licence notices |
| `SHA256SUMS.txt` | SHA-256 checksums of the four files above |

JAR names and embedded LibreLogin version retain the base `0.25.0`. Use this release's **beta.2 tag and checksums** to distinguish them from beta.1. Do not install LibreLogin-Paper on the same auth backend served by LibreLogin-Velocity.

Build/Paper 26.2 use Java 25; LibreLogin bytecode remains Java 21. Velocity's optional QR path uses a separately installed PacketEvents 2.13.0+ plugin; Paper uses its Libby runtime dependency. The advertised 1.13–26.2 range refers to client protocols and still requires appropriate protocol translation.

### Upgrade notes and limitations

- Back up the database, plugin directory and worlds. Restart after replacing JARs or changing structural settings.
- Existing configuration values are preserved. Check old JDBC URLs: new MariaDB/PostgreSQL defaults use `verify-full`; official MySQL uses `VERIFY_IDENTITY`. Explicit non-TLS URLs remain allowed with a remote-connection warning.
- Saved 2FA factors still require verification. If the provider is disabled, enable it and restart or use the administrator's account-specific recovery command.
- Paper tablist textures (#73) and the transition to the modern Paper spawn event (#52/#68) remain pending. The original skull-cache report (#58) lacks an accessible log.
- Verification: compilation, 73 unit tests and `licenseCheck` pass. Unit tests do not start a Minecraft server/proxy; live-server smoke testing is still needed.
- The specific TOTP licence uncertainty is resolved. The dependency report remains a partial direct-dependency inventory, not a complete transitive audit.

[Wiki](https://github.com/mitsuba7891/LibreLogin-Fork/wiki) · [Versioned user guides](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.2/docs/wiki/Home.md) · [Issue review](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.2/docs/libreloginprod-issues.md) · [Compare with beta.1](https://github.com/mitsuba7891/LibreLogin-Fork/compare/v0.25.0-beta.1...v0.25.0-beta.2)

---

## Español

**Pre-release de pruebas para Paper y Velocity.** Incluye los arreglos posteriores a beta.1; todavía no certifica como estable todas las combinaciones de servidor y cliente.

### Novedades desde beta.1

- Alias `/log` configurable: `login-log-alias: false` y reinicio conservan `/login` y `/l` y liberan `/log` para Carpet u otro plugin.
- Velocity conserva el motivo real de desconexión y comprueba los fallos que no producen resultado.
- 2FA manual en Paper mediante clave/URI, sin proyector de mapas.
- QR opcional: `totp.qr-enabled: false` selecciona el flujo de texto. Mantén `totp.enabled: true` para verificar TOTP.
- Desactivar el proveedor TOTP no permite omitir el segundo factor guardado de una cuenta. La recuperación administrativa es explícita y por cuenta.
- Los usuarios sin login/registro completo o pendientes de 2FA no pueden entrar a otros backends desde Velocity, incluidas las redirecciones de otros plugins.
- Una cuenta offline sin registrar no reutiliza sesiones por IP antiguas. Se rechazan sesiones incompletas, expiradas o con fecha futura.
- Paper verifica nombre y UUID premium de la respuesta de Mojang. Los nonces usan comparación constante.
- README con badges, guías en español y revisión de los 22 issues de LibreLoginProd.
- Licencia MIT de TOTP 1.7.1 comprobada en el POM padre y tag exacto; aviso original incluido en JAR y ZIP.

### Instalación y actualización

Los nombres de los JAR y su versión interna siguen siendo `0.25.0`. Usa el **tag beta.2 y sus checksums** para distinguir esta compilación de beta.1.

- Paper independiente: `LibreLogin-Paper-0.25.0.jar`.
- Red Velocity: `LibreLogin-Velocity-0.25.0.jar` en el proxy y `AuthLimbo-1.0.0.jar` en el backend Paper auth.
- No instales LibreLogin-Paper en ese mismo backend auth.
- JDK 25 para compilar y ejecutar Paper 26.2; bytecode LibreLogin Java 21.
- Conserva copias de BD, configuración y mundos. Revisa las URL JDBC antiguas: los valores explícitos se conservan. Reinicia tras cambios estructurales.
- Para alta 2FA sólo de texto: `totp.enabled: true` y `totp.qr-enabled: false`.

### Estado de pruebas y pendientes

Compilación, **73 tests** y `licenseCheck` correctos. Faltan pruebas reales de la matriz de servidor/proxy/clientes. Siguen pendientes las texturas en tablist (#73), la transición del evento de spawn de Paper (#52/#68) y un log accesible para el reporte skull-cache (#58).

P-05, la incertidumbre de licencia TOTP, queda resuelto; esto no convierte el inventario parcial de dependencias en una auditoría transitiva completa.

Las guías están publicadas en la [wiki en español](https://github.com/mitsuba7891/LibreLogin-Fork/wiki), incluidas en `docs/wiki/` dentro del ZIP y conservadas en la [documentación versionada](https://github.com/mitsuba7891/LibreLogin-Fork/blob/v0.25.0-beta.2/docs/wiki/Home.md). Los assets de beta.1 se conservan. Esta publicación sigue marcada como **pre-release**; la estable continúa siendo v0.24.12.
