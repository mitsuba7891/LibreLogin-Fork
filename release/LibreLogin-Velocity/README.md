# LibreLogin-Velocity

Proxy-side authentication for Velocity, based on [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin).

## Install

Copy the JAR into the Velocity proxy:

```text
Velocity/plugins/LibreLogin-Velocity-0.24.6.jar
```

Install `PacketEvents 2.13.0+` separately when QR projection is required. It is compile-only and is not bundled. Keep only one active LibreLogin Velocity JAR.

For the split architecture, install `AuthLimbo-1.0.0.jar` on the Paper server registered as `auth`. Do not install `LibreLogin-Paper` on that backend.

## Velocity registration

`velocity.toml`:

```toml
[servers]
auth = "127.0.0.1:25566"
lobby = "127.0.0.1:25567"
try = ["auth"]
```

Use the real backend addresses. Configure modern forwarding and the same forwarding secret on Velocity and every Paper backend.

## LibreLogin configuration

After first startup, edit `plugins/librelogin/config.yml`:

```yaml
limbo:
  - auth
lobby:
  root:
    - lobby
```

Use the generated comments as the source of truth for your revision. Keep the auth backend private behind a firewall where possible.

## Commands and 2FA

```text
/register <password> <password>
/login <password>
/login <password> <2fa_code>
/2fa
/2faconfirm <code>
/cracked
/premium
```

If premium/autologin is active, run `/cracked` before `/2fa`. Never share a TOTP URI, secret or recovery code.

## Messages and prefix

Edit `plugins/librelogin/messages.yml`:

```yaml
prefix: "LibreLogin"
```

The value is literal and can be disabled:

```yaml
prefix: ""
```

Reload safely with:

```text
/librelogin reload messages
```

The prefix is excluded from titles, subtitles, action bars and email templates.

## Dependencies

- Java 21+.
- Compatible Velocity proxy.
- PacketEvents 2.13.0+ as an external plugin for the preferred QR path.
- Protocolize optional where its supported protocol range is appropriate.
- Optional LuckPerms, Floodgate and RedisBungee integrations.
- Database driver libraries loaded at runtime through Libby.

## Troubleshooting

- **Limbo not running:** confirm the `auth` Paper backend is online, registered under the same name in `velocity.toml` and listed under `limbo`.
- **Backend closes immediately:** verify modern forwarding and matching secrets; do not run LibreLogin-Paper on the auth backend.
- **QR unavailable:** install a compatible external PacketEvents/Protocolize path, or use the manual provisioning URI output.
- **Messages unchanged:** run `/librelogin reload messages` or restart the proxy.

## License

This artifact is distributed under the upstream Mozilla Public License 2.0. See the repository `LICENSE`; do not replace it with MIT. The original project is <https://github.com/kyngs/LibreLogin>.
