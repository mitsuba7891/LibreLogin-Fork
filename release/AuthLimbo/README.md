# AuthLimbo

Small standalone Paper plugin for the backend server used as a Velocity limbo.

AuthLimbo is a companion component of a fork/customized distribution based on [LibreLogin](https://github.com/kyngs/LibreLogin). The upstream license and attribution are retained in the release package.

AuthLimbo is intentionally separate from LibreLogin authentication. It keeps the backend safe and empty while `LibreLogin-Velocity` handles login and 2FA on the proxy.

## Install

1. Install `AuthLimbo-1.0.0.jar` in the Paper `auth/plugins/` directory.
2. Do not install `LibreLogin-Paper` on this backend when `LibreLogin-Velocity` is active.
3. Use a Paper backend with modern Velocity forwarding enabled and the same forwarding secret as Velocity.
4. Stop/start the server normally after changing the files. Do not use `/reload` for world-generation changes.

The plugin creates/uses the dedicated `auth_void` world and locks players at its spawn. It blocks movement, teleportation outside the limbo world, block interaction, damage, item drops and inventory actions. The player is held in adventure mode with flight enabled to avoid falling in the void.

## Required Paper configuration

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

The first generation of `auth_void` must occur with this generator mapping present. Do not reuse a normal terrain world as the void world. The previous `limbo` world may be preserved as a backup or removed only after an administrator confirms it is no longer needed.

## Velocity configuration

Register this backend as a normal server, for example:

```toml
[servers]
auth = "AUTH_HOST:AUTH_PORT"
```

Then list `auth` under LibreLogin-Velocity's `limbo` configuration. `AuthLimbo` does not communicate with the database and does not provide authentication commands.

## Dependencies

- Paper compatible with the API used to build this release (validated against Paper 1.21.4); test the selected Paper/Minecraft release before production.
- Java 21 or newer for this release build.
- No database.
- No NanoLimbo.
- No LibreLogin-Paper.
- No PacketEvents dependency.

AuthLimbo is a companion component of this fork and is not a replacement for LibreLogin-Velocity.
