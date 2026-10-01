# Configuración

[Inicio](Home.md) · [Instalación](Instalacion.md) · [Comandos y 2FA](Comandos-y-2FA.md)

## Archivos

- Velocity: `plugins/librelogin/config.yml` y `messages.yml`.
- Paper: normalmente `plugins/LibreLogin/config.yml` y `messages.yml`.

Los comentarios generados por la revisión instalada son la referencia. Conserva la indentación YAML; los comentarios usan `#`, no `//`. Los ejemplos siguientes son fragmentos: intégralos en las secciones existentes.

Detén la instancia antes de cambiar BD, mundos/backends, alias o integraciones y reinicia después. Recargar la configuración no reconstruye necesariamente esos componentes.

## Limbo y lobby

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
remember-last-server: false
fallback: false
```

En Velocity son nombres de `[servers]`; en Paper son mundos, y todos los destinos van bajo `root`. Los hosts forzados pueden tener su propio grupo de lobby; usa `§` en lugar de puntos en esas claves, como indican los comentarios generados.

`remember-last-server: true` permite reutilizar el último destino cuando siga disponible. `fallback: true` permite al proxy intentar otro lobby si cae un backend; no autoriza a usuarios que aún deben completar login/registro o 2FA.

## Base de datos

El valor inicial de `database.type` es `librelogin-sqlite`. Para una red con varios proxies, configura una base de datos compartida en lugar de archivos SQLite independientes.

Ejemplo MariaDB, dentro de `config.yml`:

```yaml
database:
  type: librelogin-mysql
  properties:
    mysql:
      database: librelogin
      host: db.example.net
      port: 3306
      user: librelogin
      password: "SUSTITUYE_POR_TU_PASSWORD"
      jdbc-url: "jdbc:mariadb://%host%:%port%/%database%?autoReconnect=true&zeroDateTimeBehavior=convertToNull&sslMode=verify-full"
```

El mismo tipo `librelogin-mysql` y la sección `mysql` sirven para MySQL oficial; selecciona su driver cambiando la URL:

```yaml
jdbc-url: "jdbc:mysql://%host%:%port%/%database%?sslMode=VERIFY_IDENTITY"
```

Para PostgreSQL:

```yaml
database:
  type: librelogin-postgresql
  properties:
    postgresql:
      database: librelogin
      host: db.example.net
      port: 5432
      user: librelogin
      password: "SUSTITUYE_POR_TU_PASSWORD"
      jdbc-url: "jdbc:postgresql://%host%:%port%/%database%?sslmode=verify-full"
```

Mantén usuario y contraseña en sus campos separados. Al actualizar, se conserva una `jdbc-url` existente: revisa su modo TLS expresamente.

### TLS: valores exactos

| Driver / URL | Verificación completa | Sin TLS |
|---|---|---|
| MariaDB, `jdbc:mariadb://` | `sslMode=verify-full` | `sslMode=disable` |
| MySQL, `jdbc:mysql://` | `sslMode=VERIFY_IDENTITY` | `sslMode=DISABLED` |
| PostgreSQL, `jdbc:postgresql://` | `sslmode=verify-full` | `sslmode=disable` |

`sslMode` es un parámetro de la URL, no una clave YAML booleana. Usa los valores de la tabla, no `off`/`false`. Para una CA privada, MariaDB permite `serverSslCert` y PostgreSQL `sslrootcert`; configura el certificado según el driver en vez de desactivar la verificación para resolver un error de certificados.

El plugin permite URLs sin TLS. Si el destino no es loopback, registra un aviso; no impide esa conexión. **Sin cifrado, credenciales y hashes de contraseñas viajan en claro.** `localhost`, `127.0.0.1` y `::1` sólo evitan ese aviso; no cambian automáticamente el modo TLS de la URL.

## Cuentas y sesiones

```yaml
auto-register: false
profile-conflict-resolution-strategy: BLOCK
session-timeout: 0
minimum-password-length: 8
max-login-attempts: 5
milliseconds-to-refresh-login-attempts: 60000
seconds-to-authorize: 120
login-log-alias: true
```

- `auto-register: false`: un nombre premium no convierte por sí solo una nueva cuenta offline en una cuenta con autologin. Las cuentas que ya tienen modo premium siguen exigiendo sesión Mojang válida.
- `session-timeout: 0`: desactiva las sesiones por IP. Con un valor positivo, una cuenta registrada puede reutilizar una autenticación reciente desde la misma IP; compartir IP no demuestra por sí solo la identidad de una persona.
- `login-log-alias: false`: libera `/log` para Carpet u otro plugin, conservando `/login` y `/l`. Requiere reiniciar.
- Mantén `BLOCK` cuando no hayas decidido cómo resolver un conflicto de identidades. `OVERWRITE` elimina datos del perfil offline implicado.

No cambies `new-uuid-creator` para intentar corregir un login: afecta a la identidad con la que se crean cuentas nuevas y debe planificarse con los datos de jugadores y mundos.

## 2FA

```yaml
totp:
  enabled: true
  qr-enabled: false
  label: "Mi servidor"
  delay: 1000
```

`qr-enabled: false` usa sólo clave/URI manual. Si vale `true`, Velocity intenta entregar también el mapa con una integración compatible; Paper utiliza la ruta manual. `enabled: false` no elimina secretos ya guardados y esas cuentas no pueden entrar omitiendo el segundo factor. Consulta [activación y recuperación](Comandos-y-2FA.md).

## Mensajes

En `messages.yml`:

```yaml
prefix: "&6[Mi servidor] &r"
prompt-login:
  - "[center]&eInicia sesión"
  - "&f/login <contraseña>"
```

Se admite `prefix: ""` para quitar el prefijo, listas de líneas, `\n`, colores `&` y MiniMessage. El prefijo no se añade a títulos, action bars o plantillas de correo.

Después de editar mensajes, ejecuta `/librelogin reload messages` o reinicia. Las opciones de títulos/action bars/bossbar se controlan desde `config.yml`.
