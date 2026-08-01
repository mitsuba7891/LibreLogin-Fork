# LibreLogin fork release package

This package contains the three plugins required for the split proxy/backend setup:

- `LibreLogin-Velocity/` — authentication, sessions, premium login, commands and 2FA on the Velocity proxy.
- `LibreLogin-Paper/` — the standalone Paper implementation for networks that use LibreLogin directly on Paper.
- `AuthLimbo/` — a small independent Paper limbo-lockdown plugin for a Paper backend used as the Velocity limbo. It does **not** contain LibreLogin authentication or PacketEvents.

This project is a fork/customized distribution based on [LibreLogin](https://github.com/kyngs/LibreLogin).
Please keep the upstream license and attribution files included with the release.

## Recommended proxy installation

1. Install only `LibreLogin-Velocity-<version>.jar` in the Velocity `plugins/` directory.
2. Register the backend servers in `velocity.toml`, for example:

   ```toml
   [servers]
   auth = "AUTH_HOST:AUTH_PORT"
   lobby = "LOBBY_HOST:LOBBY_PORT"
   try = ["auth"]
   ```

3. Configure the Velocity plugin's `limbo` list with `auth` and its `lobby.root` list with `lobby`.
4. Configure modern forwarding in Velocity and Paper with the same forwarding secret.
5. Do not install `LibreLogin-Paper` on the proxied `auth` backend when `LibreLogin-Velocity` is handling authentication.
6. Install `AuthLimbo` on the `auth` Paper backend and configure that backend as documented in `AuthLimbo/README.md`.

## 2FA

Enable TOTP in the Velocity `plugins/librelogin/config.yml`. The proxy needs a compatible image projector:

- PacketEvents 2.13.0+ is the preferred cross-version path in this fork.
- Protocolize remains an optional compatibility path where its supported protocol range is appropriate.

The login prompt accepts:

```text
/login <password> <2fa_code>
```

Accounts with premium/autologin enabled must first run `/cracked` before starting `/2fa`.

## Configuration files

LibreLogin generates readable YAML files on first startup:

- `config.yml` — database, limbo/lobby, TOTP, login and platform settings.
- `messages.yml` — all user-facing messages.

The message prefix is user-controlled:

```yaml
prefix: "LibreLogin"
```

Use an empty value to disable it:

```yaml
prefix: ""
```

After editing messages, use `/librelogin reload messages` or restart the proxy.

## NanoLimbo

NanoLimbo is not part of this release. There are no NanoLimbo dependencies, integrations or runtime lookup paths in the packaged artifacts. The limbo backend is a normal registered Paper server, with `AuthLimbo` providing the empty-world and movement-lock behavior.

## Java and support notes

The LibreLogin artifacts target Java 21 bytecode. Java 21 or newer is required. Paper and Velocity themselves may impose additional Java and Minecraft-version requirements. Test the selected Paper, Velocity, ViaVersion/ViaBackwards and client-version combination before production rollout.

See the component READMEs and `CHANGELOG.md` for the complete change list.
