# LibreLogin-Velocity

Proxy-side authentication for Velocity, based on [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin). This fork artifact was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6); see the repository `CHANGELOG.md` for the full change list.

## Install

Copy the JAR into the Velocity proxy:

```text
Velocity/plugins/LibreLogin-Velocity-0.25.0.jar
```

Install `PacketEvents 2.14.0+` separately when QR projection is required, including Minecraft 26.3 clients. It is compile-only and is not bundled. Keep only one active LibreLogin Velocity JAR.

The internal ID is `librelogin-fork`. Fresh installations use `plugins/librelogin-fork/`; existing `librelogin`/`LibreLogin`/LibrePremium data directories are reused. The startup log identifies the legacy path. Update plugin dependencies referring to the old Velocity ID, but keep `/librelogin` and existing permission nodes.

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

After first startup, edit `plugins/librelogin-fork/config.yml` (or the reused legacy directory):

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

`/librelogin [page]` and `/librelogin help [page]` show all permitted commands in six-command pages, with clickable Previous/Next controls. Clicking a staff/account command suggests it without executing it.

For text-only setup, set `totp.qr-enabled: false`, keep `totp.enabled: true`, and restart. Beta.2 refuses skipping a saved second factor when its provider is disabled. It also restricts unauthenticated players to configured limbos, including effective destinations changed by other plugins.

Set `login-log-alias: false` and restart to release `/log` for another plugin while keeping `/login` and `/l`. On a first startup, LibreLogin requests a proxy restart after generating configuration; configure a process supervisor to relaunch Velocity. This expected setup step is informational, not a fatal error. User guides are included in the bundle's `docs/wiki/` directory and [in the current source](https://github.com/mitsuba7891/LibreLogin-Fork/blob/master/docs/wiki/Home.md).

## Messages and prefix

Edit `plugins/librelogin-fork/messages.yml` (or the reused legacy directory):

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

- Java 25 is the tested runtime for this release; the artifact retains Java 21 bytecode.
- Compatible Velocity proxy.
- PacketEvents 2.14.0+ as an external plugin for the preferred QR path.
- Protocolize optional where its supported protocol range is appropriate.
- Optional LuckPerms, Floodgate and RedisBungee integrations.
- Database driver libraries loaded at runtime through Libby.

Client/protocol handling is maintained from 1.13 through 26.3. Install the appropriate ViaVersion/ViaBackwards-style translation components for the actual backend versions; PacketEvents does not perform protocol translation by itself. AuthLimbo is required on the configured Paper authentication backend and performs the safe chat/interaction lock for modern signed-chat clients.

## Troubleshooting

- **Limbo not running:** confirm the `auth` Paper backend is online, registered under the same name in `velocity.toml` and listed under `limbo`.
- **Backend closes immediately:** verify modern forwarding and matching secrets; do not run LibreLogin-Paper on the auth backend.
- **QR unavailable:** install PacketEvents 2.14.0+ for 1.13–26.3, or use the manual provisioning URI output. Missing optional image integrations are reported at INFO and do not disable 2FA. Protocolize's fallback only covers clients through 1.21.3.
- **Messages unchanged:** run `/librelogin reload messages` or restart the proxy.

## License

This artifact is distributed under the upstream Mozilla Public License 2.0. See the repository `LICENSE`; do not replace it with MIT. The original project is <https://github.com/kyngs/LibreLogin>.
