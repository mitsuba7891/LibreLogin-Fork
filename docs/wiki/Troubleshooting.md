# Troubleshooting

[Home](Home.md) · [Installation](Installation.md) · [Configuration](Configuration.md)

**Common problems:** [Invalid session](#invalid-session-and-premium-accounts) · [Limbo](#players-outside-limbo-or-changing-servers) · [QR errors](#missing-qr-or-protocol-errors) · [TLS](#database-certificate-errors) · [Known issues](#known-issues)

## Invalid session and premium accounts

A premium-autologin account requires a valid Mojang session. An offline client using the same name does not prove ownership.

- Check the launcher's signed-in account and whether the nickname matches.
- Review `auto-register` and `/librelogin user info <name>`.
- If the owner wants password login, an administrator can use `/librelogin user cracked <name>` after verifying ownership and checking that a valid password exists.
- Do not add automatic offline fallback or delete the account just to hide the message.

Paper also requires a successful session-server response to contain the expected username and premium UUID. The LibreLogin local UUID can differ from the premium UUID; do not interchange them.

## Players outside limbo or changing servers

Check that `limbo` names match `[servers]`, the auth backend uses AuthLimbo, and offline backend ports are not publicly accessible.

Before login/registration completes, or during 2FA setup, this Velocity revision permits only configured limbo destinations, including redirects made by other plugins. Unregistered offline accounts cannot reuse IP sessions through stale metadata.

If a routing plugin sends the player to the lobby too early, the request is denied. Test without that routing plugin to identify the cause.

## Missing QR or protocol errors

### Use manual setup

```yaml
totp:
  enabled: true
  qr-enabled: false
```

Restart, run `/2fa`, enter the secret in your authenticator, and confirm with `/2faconfirm <code>`.

### Check the QR integration

PacketEvents 2.13.0+ is the preferred Velocity QR path. Protocolize is a fallback with a more limited range; its integration must not be advertised as supporting clients newer than 1.21.3. Older clients on a modern backend also need appropriate protocol translation. Paper does not provide a built-in QR projector.

## Existing 2FA account cannot log in

Check `totp.enabled`. Disabling the provider does not remove account secrets or bypass a second factor. Enable it again and restart, or follow [account-specific recovery](Commands-and-2FA.md#two-factor-recovery).

Check the server and phone clocks as well. Do not share the secret or URI when requesting support.

## Backend connection failures

- Start auth first and verify its address and port in Velocity.
- Check forwarding mode and matching secrets on every backend.
- Beta.2 preserves Velocity's received disconnect reason; use it to distinguish a stopped server, backend rejection or forwarding problem.
- `fallback` cannot replace an operational limbo for unauthenticated players.

## Wrong lobby destination

Review `lobby.root`, forced-host groups, `remember-last-server` and `fallback`. Names refer to registered backends, not game modes. On Paper they are worlds and belong under `root`.

## Database certificate errors

New URLs use full TLS verification. The hostname in `host`/`jdbc-url` must match the certificate, and the JVM or driver must trust its CA. For private certificates, configure the driver's certificate/CA settings; see [TLS modes](Configuration.md#tls-modes).

Non-TLS values vary by driver. Disabling TLS allows an unencrypted connection, but does not establish the database server's identity and exposes credentials/hashes in clear text on a remote connection.

## Carpet conflicts with /log

Set `login-log-alias: false` in `config.yml` and restart. `/login` and `/l` remain available. This controls alias registration and is not applied by reloading messages alone.

## Upgrading an existing installation

1. Back up the database, worlds and plugin folder.
2. Replace the JAR with the correct platform artifact and remove the older copy.
3. First startup converts `config.conf` and `messages.conf` to YAML, preserving originals as `.conf.pre-yaml.bak`.
4. Review the JDBC URL and preserved settings; new defaults do not replace explicit existing values.
5. Test a new, existing offline, premium and 2FA account before reopening the network.

## Known issues

The issue numbers below belong to **LibreLoginProd**.

| Report | Current status |
|---|---|
| **Tablist skins (#73)** | Paper's replacement profile does not preserve textures. Session validation does not add them; the visual issue remains pending. |
| **Spawn on Paper 1.21.9+ (#52/#68)** | Beta.3 uses `AsyncPlayerSpawnLocationEvent` and UUID-based location caching. Live testing is still required for each Paper/client matrix. |
| **Skull cache (#58)** | The original log is no longer accessible. A current log and reproduction steps are needed. |

The [cross-fork review](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/libreloginprod-issues.md) records all 22 reports.

## Uninstalling

Stop the instance and keep a database backup. Replace authentication or migrate to an environment that provides it before reopening an offline server. Removing LibreLogin without another authentication mechanism leaves player names unprotected. AuthLimbo alone does not replace proxy authentication.
