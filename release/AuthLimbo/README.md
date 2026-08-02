# AuthLimbo

A small standalone Paper companion for the Velocity architecture, based on the LibreLogin fork and replacing the NanoLimbo integration. This artifact was reviewed and updated with AI assistance (Freebuff assistant, deepseek-v4-flash); see the repository `CHANGELOG.md` for the full change list.

## Responsibility

AuthLimbo is not an authentication plugin. It does not access the database or provide `/login`, `/register` or `/2fa`. `LibreLogin-Velocity` remains responsible for authentication; AuthLimbo only protects the backend limbo world.

## Install

Copy the JAR into the Paper auth backend:

```text
Paper-auth/plugins/AuthLimbo-1.0.0.jar
```

Do not install `LibreLogin-Paper` on this backend when `LibreLogin-Velocity` is active. Enable modern Velocity forwarding and use the same forwarding secret as the proxy.

## Void-world configuration

Before the first generation:

`server.properties`

```properties
level-name=auth_void
allow-flight=true
```

`bukkit.yml`

```yaml
worlds:
  auth_void:
    generator: AuthLimbo:void
```

Start the server once, confirm `auth_void` is created as the dedicated limbo world, then check the console. Do not reuse a normal terrain world. Do not use `/reload` for world-generation changes.

## Velocity registration

`velocity.toml`:

```toml
[servers]
auth = "127.0.0.1:25566"
```

Then list `auth` under LibreLogin-Velocity's `limbo` setting. The backend should be private and reachable only by the proxy.

## Behavior

AuthLimbo keeps players at the limbo spawn and blocks movement, unsafe teleportation, block interaction, damage, item drops and inventory actions. It uses adventure mode and flight to avoid falling while the proxy completes authentication.

## Dependencies and limitations

- Java 21+ for this release build.
- Paper compatible with the API used to build this release; validated against Paper 1.21.4.
- No database, PacketEvents, NanoLimbo or LibreLogin-Paper dependency.
- Test the exact Paper/Minecraft version before production.

## Troubleshooting

- **Limbo not running:** ensure the Paper process is online and `auth` in `velocity.toml` resolves to the correct address.
- **Terrain appears:** stop the server, verify `level-name=auth_void` and the `AuthLimbo:void` generator entry, then recreate only the intended limbo world after taking a backup.
- **Player can move:** confirm AuthLimbo is enabled and no other plugin overrides movement, teleport, game mode or flight events.
- **Backend disconnects:** verify modern forwarding and the matching forwarding secret.

## License

This component is distributed under the upstream Mozilla Public License 2.0. See the repository `LICENSE`; do not replace it with MIT. Upstream: <https://github.com/kyngs/LibreLogin>.
