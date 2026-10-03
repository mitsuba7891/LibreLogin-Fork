# Revisión de issues de LibreLoginProd

Revisado el **1 de octubre de 2026** contra el código de `mitsuba7891/LibreLogin-Fork`, rama `beta`.

Fuente: los 22 issues abiertos y cerrados de [Navio1430/LibreLoginProd](https://github.com/Navio1430/LibreLoginProd/issues). Los números de esta página pertenecen a ese fork. Su numeración de versiones y sus cambios no identifican automáticamente un fallo en nuestro código.

## Estado por issue

| Issue | Resultado en nuestro fork |
|---|---|
| [#40](https://github.com/Navio1430/LibreLoginProd/issues/40), conflicto `/log` | Corregido en `db29623`: `login-log-alias: false` libera `/log`; requiere reinicio. |
| [#48](https://github.com/Navio1430/LibreLoginProd/issues/48), motivo de desconexión | Corregido en `7a366ae`: se conserva el motivo de Velocity y se comprueba si falta el resultado. |
| [#41](https://github.com/Navio1430/LibreLoginProd/issues/41), 2FA en Paper | Corregido en `ab2b95a`: el proveedor TOTP no depende del proyector QR; se ofrece clave/URI manual. |
| [#67](https://github.com/Navio1430/LibreLoginProd/issues/67), error de protocolo con el QR | Mitigación adicional: `totp.qr-enabled: false` evita inicializar/enviar mapas, manteniendo la verificación TOTP. No se ha reproducido el error original del cliente ni se declara reparada toda combinación de paquetes/protocolos. |
| [#61](https://github.com/Navio1430/LibreLoginProd/issues/61), nuevos usuarios fuera de limbo | No se reproduce la regresión específica de su versión 0.25.8 en nuestra selección inicial: las cuentas nuevas sin autenticación van a limbo. Se ha reforzado esa política contra metadatos de sesión antiguos y se ha corregido un hueco propio de Velocity que permitía solicitar otro backend antes de autenticarse. |
| [#53](https://github.com/Navio1430/LibreLoginProd/issues/53), sesión inválida para usuarios offline | Los comentarios confirman que una cuenta configurada como premium exige una sesión Mojang válida. No se añade fallback offline a una cuenta premium. Además, se refuerza la comprobación de nombre y UUID de la respuesta Mojang en Paper. |
| [#72](https://github.com/Navio1430/LibreLoginProd/issues/72), conflicto premium/offline y mayúsculas | Comportamiento esperado cuando el perfil exige premium o no coincide el nombre registrado. Revisar `auto-register` y la identidad de la cuenta; no desactivar comprobaciones de propiedad para eliminar el mensaje. |
| [#73](https://github.com/Navio1430/LibreLoginProd/issues/73), skins en tablist | Pendiente. `PaperListeners.onPreLogin` crea un perfil sin conservar texturas. La validación adicional de Mojang no corrige este problema visual. |
| [#52](https://github.com/Navio1430/LibreLoginProd/issues/52), spawn en Paper 1.21.9–1.21.10 | Nuestro código conserva la caché de ubicación y la restauración después del login, pero sigue usando el evento antiguo `PlayerSpawnLocationEvent`. La advertencia sobre la API moderna también es relevante aquí; falta comprobarlo en esas versiones y adaptar el evento si se reproduce. No se marca como corregido. |
| [#68](https://github.com/Navio1430/LibreLoginProd/issues/68), regresión de spawn en 0.25.9 | No se han importado los cambios causantes de esa regresión. La restauración existente no basta para certificar todas las versiones: comparte la comprobación pendiente de #52. |
| [#50](https://github.com/Navio1430/LibreLoginProd/issues/50), soporte 1.21.9 | PacketEvents 2.13.0 y Paper API 26.2 ya están declarados. La transición del evento de spawn señalada en los comentarios queda registrada con #52; compilar no sustituye una prueba del servidor. |
| [#35](https://github.com/Navio1430/LibreLoginProd/issues/35), destino Creative y fallback | No hay reproducción con configuración de nuestro fork. El código usa `lobby.root`, hosts forzados, `remember-last-server` y `fallback`; esos valores pueden explicar el destino. Documentados en la guía. |
| [#58](https://github.com/Navio1430/LibreLoginProd/issues/58), skull cache | No confirmable: el informe no da pasos y su enlace de Pastebin devuelve 404. Hace falta un log actual para atribuirlo a LibreLogin. |
| [#59](https://github.com/Navio1430/LibreLoginProd/issues/59), desinstalación | Consulta operativa. La guía explica cómo sustituir la autenticación antes de reabrir un servidor offline. |
| [#70](https://github.com/Navio1430/LibreLoginProd/issues/70), BungeeCord/Waterfall | Petición de plataforma. Nuestra distribución documentada ofrece Paper y Velocity; no se añade otra plataforma como parche de seguridad. |
| [#42](https://github.com/Navio1430/LibreLoginProd/issues/42), PlaceholderAPI | Petición de integración. No se presenta como una función disponible. |
| [#39](https://github.com/Navio1430/LibreLoginProd/issues/39), autenticación por mod/cookies | Petición de una nueva autenticación. No se añade un mecanismo de confianza de cliente sin un diseño y revisión propios. |
| [#38](https://github.com/Navio1430/LibreLoginProd/issues/38), prefijo offline | Petición de espacios de nombres. No se cambian nombres/UUID de cuentas existentes como arreglo de seguridad. |
| [#34](https://github.com/Navio1430/LibreLoginProd/issues/34), lista de cambios | README actualizado y esta revisión separan cambios confirmados de problemas pendientes. |
| [#33](https://github.com/Navio1430/LibreLoginProd/issues/33), README | README actualizado con badges y enlaces reales del fork. |
| [#30](https://github.com/Navio1430/LibreLoginProd/issues/30), CD Modrinth | Publicar en Modrinth requiere credenciales de su cuenta. No se ha publicado ni configurado CD en esta revisión. |
| [#29](https://github.com/Navio1430/LibreLoginProd/issues/29), JAR de 500 KB | Ya existen tareas de JAR por plataforma. No se elimina una dependencia necesaria para perseguir un tamaño arbitrario. |

## Refuerzos adicionales de autenticación

- `velocity/Blockers.java`: usuarios pendientes de login/registro y de 2FA sólo pueden conectarse a un limbo configurado. Se comprueba el **destino efectivo**, incluyendo redirecciones de otros plugins.
- `common/session/SessionPolicy.java`: una cuenta offline sin contraseña registrada no reutiliza una sesión por IP. Se rechazan metadatos incompletos, sesiones expiradas, otra IP y fechas de autenticación futuras. `session-timeout` sigue desactivado por defecto.
- `LoginCommand.java`: un secreto 2FA existente exige un proveedor operativo; `totp.enabled: false` ya no omite silenciosamente el segundo factor. La recuperación administrativa explícita es `/librelogin user 2faoff <nombre>`.
- `paper/protocol/MojangSessionResponse.java`: HTTP 200 no basta; se requieren nombre y UUID premium esperados en un JSON válido. El UUID premium se conserva desde el inicio del intercambio cifrado y no se confunde con el UUID local configurable.
- `EncryptionUtil.verifyNonce`: comparación del nonce mediante `MessageDigest.isEqual`.

## Verificación

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths=/home/ubuntu/.jdk25 \
  :Plugin:compileJava :Plugin:test :Plugin:licenseCheck
```

Resultado: **BUILD SUCCESSFUL**, 73 tests sin fallos. Se añadieron 35 casos para selección/restricción de backend, sesiones offline, respuesta de Mojang y nonce. Los tests no arrancan un proxy o servidor Minecraft ni acreditan la reproducción de los problemas de spawn/skins.

Los tres primeros arreglos, los refuerzos y la corrección de arranque/spawn se incluyen en `v0.25.0-beta.3`, junto con la documentación. Los assets de beta.1 y beta.2 se conservan. La incertidumbre P-05 sobre TOTP se resolvió comprobando la licencia MIT heredada del POM padre y el aviso del tag exacto; véase [dependency-licenses.md](dependency-licenses.md).
