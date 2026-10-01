# LibreLogin Fork — Wiki

Guías en español para [mitsuba7891/LibreLogin-Fork](https://github.com/mitsuba7891/LibreLogin-Fork), un fork de LibreLogin con autenticación para **Paper** y **Velocity**.

## Empieza aquí

| Guía | Contenido |
|---|---|
| [Instalación](Instalacion.md) | Elegir el JAR, montar Velocity + AuthLimbo o instalar Paper independiente. |
| [Configuración](Configuracion.md) | Limbo/lobby, SQLite, MariaDB/MySQL/PostgreSQL, TLS, sesiones y mensajes. |
| [Comandos y 2FA](Comandos-y-2FA.md) | Registro, login, premium, clave manual, correo y permisos administrativos. |
| [Solución de problemas](Solucion-de-problemas.md) | Sesión inválida, QR, routing, TLS, migración y problemas conocidos. |
| [API y compilación](API-y-compilacion.md) | JDK 25, Gradle, artefactos y documentación de la API. |

## Qué instalar

| Entorno | Componentes |
|---|---|
| Paper independiente | `LibreLogin-Paper-0.25.0.jar` en Paper. |
| Red Velocity | `LibreLogin-Velocity-0.25.0.jar` en el proxy y `AuthLimbo-1.0.0.jar` en el backend Paper de autenticación. |

**AuthLimbo no autentica cuentas por sí solo:** protege el backend de espera; LibreLogin-Velocity decide cuándo un jugador puede entrar al lobby.

## Versiones y estado

- [Estable publicada: v0.24.12](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.24.12).
- [Pre-release: v0.25.0-beta.2](https://github.com/mitsuba7891/LibreLogin-Fork/releases/tag/v0.25.0-beta.2).
- Estas guías describen **`v0.25.0-beta.2`, versión base 0.25.0**. Incluye `login-log-alias`, 2FA manual en Paper y `totp.qr-enabled`. Los JAR conservan `0.25.0` en sus nombres; usa el tag de la release y sus checksums para distinguir revisiones. Los assets antiguos de `beta.1` no incluyen esos cambios.
- Paper 26.2 y la compilación necesitan **Java 25**. El bytecode de LibreLogin se mantiene en Java 21. El rango 1.13–26.2 anunciado corresponde a **clientes/protocolos**, no a una certificación del JAR para todos los servidores Paper antiguos.

Descargas: [GitHub Releases](https://github.com/mitsuba7891/LibreLogin-Fork/releases). Los problemas confirmados y pendientes del fork vecino se registran en la [revisión de issues](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/libreloginprod-issues.md).

## Ayuda y licencia

Reporta problemas en [los issues de este fork](https://github.com/mitsuba7891/LibreLogin-Fork/issues), incluyendo versión del JAR, servidor/proxy, Java, cliente, plugins de traducción y pasos para reproducir. Retira contraseñas, secretos de forwarding, claves TOTP y datos personales antes de compartir logs/configuración.

LibreLogin conserva **MPL-2.0** y los avisos de sus autores originales. El aviso MIT de FastLogin corresponde a esa dependencia. Consulta [LICENSE](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/LICENSE) y el [inventario de licencias](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/dependency-licenses.md).
