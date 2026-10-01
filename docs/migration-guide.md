# Administrator migration guide

## Configuration migration

1. Back up the LibreLogin data folder and database before changing the jar.
2. On first startup, `config.conf` becomes `config.yml` and `messages.conf` becomes `messages.yml`.
3. The original files are preserved as `config.conf.pre-yaml.bak` and `messages.conf.pre-yaml.bak`.
4. After migration, edit only the `.yml` files. The old `.conf` files are not read while YAML exists.
5. Review the existing JDBC URL and TLS settings; explicit old values are preserved. See the [current configuration guide](wiki/Configuracion.md).

## Artifact names

The build produces platform-labelled files in `Plugin/build/libs/platform/`:

- `LibreLogin-Paper-<version>.jar`
- `LibreLogin-Velocity-<version>.jar`

They are labelled outputs of the current shared plugin implementation. Install only the artifact
matching the platform, and remove an older LibreLogin jar so two copies are not loaded. BungeeCord
is no longer a supported LibreLogin platform.

## 2FA and Protocolize

The TOTP API remains behind LibreLogin's existing provider interface. No secret or database
migration is required. In the current beta source, Paper and Velocity support manual secret/URI
setup without an image integration. Velocity prefers PacketEvents for optional QR delivery and
retains Protocolize as a compatible legacy fallback. Set `totp.qr-enabled: false` and restart to
use text-only setup. Disabling `totp.enabled` does not remove existing account factors or allow
their login to skip verification. See [2FA and recovery](wiki/Comandos-y-2FA.md).

## Platform removal

BungeeCord support was removed from the source tree, dependencies, metadata and build. Existing
BungeeCord installations must remain on the previous LibreLogin artifact or migrate to Velocity.
Paper and Velocity artifacts are the only supported outputs of this branch.
