# LibreLogin-Velocity

Proxy-side authentication plugin for Velocity.

This component is part of a fork/customized distribution based on [LibreLogin](https://github.com/kyngs/LibreLogin).

## Install

Copy `LibreLogin-Velocity-0.24.5.jar` into the Velocity `plugins/` directory. Keep only one active LibreLogin Velocity jar.

LibreLogin must be installed on the proxy for this architecture. Do not also install `LibreLogin-Paper` on the proxied `auth` backend: the backend is only a registered Paper server and should not initialize a second authentication/PacketEvents pipeline.

## Required backend registration

Register the backend names in `velocity.toml`:

```toml
[servers]
auth = "AUTH_HOST:AUTH_PORT"
lobby = "LOBBY_HOST:LOBBY_PORT"
try = ["auth"]
```

In `plugins/librelogin/config.yml`:

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
```

Use modern forwarding and configure the same forwarding secret in Velocity and the Paper backend.

## Dependencies

- Velocity API/runtime compatible with the selected proxy release.
- Java 21 or newer.
- PacketEvents 2.13.0+ is preferred for cross-version 2FA image projection.
- Protocolize is optional as a compatibility fallback where its supported protocol range is suitable.
- Optional integrations: LuckPerms, Floodgate and RedisBungee where used by the network.
- Database driver libraries are downloaded by Libby at runtime according to the selected database type.

NanoLimbo is not required or included.

## 2FA and premium login

Enable TOTP in `config.yml`. Login syntax is:

```text
/login <password> <2fa_code>
```

Users with premium/autologin enabled must run `/cracked` before `/2fa` or `/2faconfirm` is accepted.

## Prefix and messages

Edit `plugins/librelogin/messages.yml`:

```yaml
prefix: "LibreLogin"
```

The prefix is literal and applies to chat, command and kick messages. It is not applied to titles, subtitles, action bars or email templates. Set it empty to disable it:

```yaml
prefix: ""
```

Reload after editing:

```text
/librelogin reload messages
```
