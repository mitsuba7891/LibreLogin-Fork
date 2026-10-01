# Solución de problemas

[Inicio](Home.md) · [Instalación](Instalacion.md) · [Configuración](Configuracion.md)

## «Invalid session» / cuenta premium y cliente offline

Una cuenta con autologin premium exige una sesión Mojang válida. Conectar un cliente offline con el mismo nombre no acredita la propiedad de esa cuenta.

- Comprueba con qué cuenta iniciará sesión el launcher y si el nombre coincide.
- Revisa `auto-register` y la ficha del usuario mediante `/librelogin user info <nombre>`.
- Si el dueño quiere cambiar a contraseña, el administrador puede usar `/librelogin user cracked <nombre>` tras verificar la titularidad y revisar que exista una contraseña válida.
- No habilites fallback offline automático ni borres la cuenta para ocultar el mensaje.

En el código actual de Paper, un HTTP 200 del servidor de sesiones también debe contener el nombre y UUID premium esperados. El UUID local de LibreLogin puede ser distinto del premium; no los intercambies.

## Nuevos jugadores fuera de limbo / cambios de servidor

Revisa que `limbo` y `[servers]` coincidan, que el backend auth use AuthLimbo y que ningún puerto offline permita acceso directo desde Internet.

En esta revisión de Velocity, antes de completar login/registro o mientras se configura 2FA, sólo se permite un destino incluido en `limbo`, incluso cuando otro plugin redirige una solicitud. Las cuentas offline sin registro no reutilizan una sesión por IP aunque conserven metadatos antiguos.

Si un plugin de routing intenta enviar prematuramente al jugador al lobby, esa conexión será denegada. Prueba también sin ese plugin de routing para localizar la causa.

## QR ausente o error de protocolo al ejecutar `/2fa`

Usa la ruta manual sin paquetes de mapa:

```yaml
totp:
  enabled: true
  qr-enabled: false
```

Reinicia, ejecuta `/2fa`, introduce la clave en tu autenticador y confirma con `/2faconfirm <código>`.

Para QR en Velocity, PacketEvents 2.13.0+ es la ruta preferida. Protocolize es un fallback con un rango más limitado; su integración no debe anunciarse como compatible con clientes posteriores a 1.21.3. Un cliente antiguo conectado a un backend moderno también necesita la traducción de protocolo apropiada. Paper no proporciona un proyector QR integrado.

## 2FA configurado pero ahora aparece un error al hacer login

Comprueba `totp.enabled`. Desactivarlo globalmente no elimina la clave de las cuentas ni permite omitir su segundo factor. Vuelve a habilitarlo y reinicia; si se necesita recuperar una cuenta, sigue la [recuperación administrativa de 2FA](Comandos-y-2FA.md).

Comprueba también que el reloj del servidor y el del teléfono sean correctos. No compartas la clave/URI para pedir ayuda.

## «Unable to connect», limbo no disponible o kick del backend

- Arranca primero el backend auth y confirma su dirección/puerto en Velocity.
- Comprueba el secreto y modo de forwarding en todos los backends.
- En esta revisión se conserva el motivo de desconexión recibido por Velocity; úsalo para distinguir un servidor apagado, un rechazo del backend o un problema de forwarding.
- `fallback` no sustituye a un limbo operativo para usuarios pendientes de autenticación.

## Jugadores en el lobby equivocado

Revisa `lobby.root`, los grupos de hosts forzados, `remember-last-server` y `fallback`. Los nombres se refieren a los backends registrados, no a modos de juego. En Paper son mundos y deben configurarse bajo `root`.

## Fallo de certificados de la base de datos

Las configuraciones nuevas usan verificación TLS completa. El hostname de `host`/`jdbc-url` debe coincidir con el certificado, y la JVM/driver debe confiar en la CA. Para certificados privados, configura el certificado/CA indicado por tu driver; consulta [TLS](Configuracion.md).

Los valores para desactivar explícitamente TLS difieren por driver. Hacerlo permite conectar sin cifrado, pero no resuelve la identidad del servidor de BD y deja credenciales/hashes en claro en una conexión remota.

## Conflicto con Carpet `/log`

En `config.yml`, establece `login-log-alias: false` y reinicia. LibreLogin conservará `/login` y `/l`. La opción afecta al registro del alias, no se aplica sólo recargando mensajes.

## Migrar una instalación antigua

1. Copia la BD, mundos y carpeta del plugin.
2. Sustituye el JAR por el de la plataforma correcta, quitando la copia anterior.
3. En el primer arranque, `config.conf` y `messages.conf` se convierten en YAML; los originales se conservan como `.conf.pre-yaml.bak`.
4. Revisa la URL JDBC y los ajustes conservados: los defaults nuevos no sustituyen valores explícitos existentes.
5. Prueba una cuenta nueva, una offline existente, una premium y una con 2FA antes de reabrir la red.

## Problemas conocidos que siguen pendientes

- **Skins en tablist (#73 de LibreLoginProd):** el perfil nuevo de Paper no conserva texturas. Sigue pendiente; validar la respuesta Mojang no añade las texturas.
- **Spawn en Paper 1.21.9+ (#52/#68):** se conserva la caché y restauración de ubicación, pero el listener usa el evento antiguo de spawn. Se necesita una prueba de esas versiones y, si procede, la adaptación al evento moderno.
- **Skull cache (#58):** el log enlazado en el reporte original ya no existe; hace falta un log actual y pasos de reproducción.

El detalle y estado de los 22 reportes está en la [revisión de issues](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/libreloginprod-issues.md).

## Desinstalar

Detén la instancia, conserva una copia de la base de datos y sustituye la autenticación o migra a un entorno que la proporcione antes de reabrir un servidor offline. Retirar LibreLogin sin otro control dejaría libres los nombres de los jugadores. AuthLimbo por sí solo no sustituye la autenticación del proxy.
