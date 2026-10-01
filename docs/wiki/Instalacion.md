# Instalación

[Inicio](Home.md) · [Configuración](Configuracion.md) · [Problemas frecuentes](Solucion-de-problemas.md)

## Antes de instalar

1. Elige Paper independiente o autenticación en Velocity.
2. Descarga el artefacto de la revisión que vas a usar desde [Releases](https://github.com/mitsuba7891/LibreLogin-Fork/releases), o [compila `beta`](API-y-compilacion.md).
3. Para Paper 26.2, usa Java 25. Conserva una copia de la base de datos, las carpetas de plugins y los mundos antes de actualizar.
4. Mantén un solo JAR LibreLogin activo por instancia. Las bibliotecas de BD y otras dependencias se descargan mediante Libby cuando corresponda.

## Red Velocity + AuthLimbo

### 1. Distribuye los componentes

```text
Velocity/plugins/LibreLogin-Velocity-0.25.0.jar
Paper-auth/plugins/AuthLimbo-1.0.0.jar
```

No instales LibreLogin-Paper en el backend de esta misma ruta de autenticación. Para mapas QR en Velocity, instala PacketEvents 2.13.0+ como plugin separado. Para [2FA manual](Comandos-y-2FA.md), no hace falta un proyector de mapas.

### 2. Registra los backends

Ejemplo de `velocity.toml`; combina estos valores con tu configuración existente, sin duplicar secciones:

```toml
online-mode = false
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"

[servers]
auth = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["auth"]
```

Sustituye las direcciones por las reales. Aunque el proxy admita cuentas offline, las cuentas con autologin premium deben superar la autenticación online de Mojang.

### 3. Configura forwarding en cada Paper backend

En `server.properties`, establece `online-mode=false`. Para Paper 26.2, en `config/paper-global.yml`:

```yaml
proxies:
  velocity:
    enabled: true
    online-mode: false
    secret: "COPIA_EL_CONTENIDO_REAL_DE_FORWARDING_SECRET"
```

El secreto debe coincidir con el del proxy, y `proxies.velocity.online-mode` con el modo online del proxy. Restringe los puertos de los backends para que sólo acepte conexiones el proxy; un puerto offline público permitiría saltarse la autenticación central.

### 4. Prepara el mundo AuthLimbo

Antes de crear el mundo del backend auth, configura:

`server.properties`:

```properties
level-name=auth_void
allow-flight=true
```

`bukkit.yml`:

```yaml
worlds:
  auth_void:
    generator: AuthLimbo:void
```

Arranca el backend para crear el mundo vacío. AuthLimbo no convierte automáticamente un mundo de terreno existente en vacío.

### 5. Configura el plugin del proxy

Tras el primer arranque, detén Velocity y edita `plugins/librelogin/config.yml`:

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
```

Los nombres deben coincidir exactamente con `[servers]`. Arranca los backends y después el proxy. Comprueba que una cuenta nueva permanece en auth hasta registrarse y que una cuenta offline existente sólo entra al lobby después del login.

## Paper independiente

1. Instala `LibreLogin-Paper-0.25.0.jar` en `Paper/plugins/`.
2. Configura `online-mode=false` en `server.properties`. Esta arquitectura no debe tener activado forwarding de Bungee/Velocity.
3. Arranca una vez, detén el servidor y edita `plugins/LibreLogin/config.yml` y `messages.yml`.
4. Configura los mundos reales, por ejemplo:

```yaml
limbo:
  - auth_void
lobby:
  root:
    - world
```

`world` debe existir. El limbo de Paper se crea como mundo vacío si no existe. Todos los destinos de Paper van bajo `lobby.root`.

5. Reinicia. Prueba registro, login, salida/reconexión y [2FA manual](Comandos-y-2FA.md).

En Paper, las bibliotecas PacketEvents se cargan a través de Libby; el JAR de Velocity usa el plugin PacketEvents externo. Evita instalar copias adicionales sin comprobar qué componente las proporciona.
