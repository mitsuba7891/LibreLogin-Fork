# Administrator migration guide

## Configuration migration

1. Back up the LibreLogin data folder and database before changing the jar.
2. On first startup, `config.conf` becomes `config.yml` and `messages.conf` becomes `messages.yml`.
3. The original files are preserved as `config.conf.pre-yaml.bak` and `messages.conf.pre-yaml.bak`.
4. After migration, edit only the `.yml` files. The old `.conf` files are not read while YAML exists.
5. Keep the existing database configuration and Protocolize installation unchanged.

## Artifact names

The build produces platform-labelled files in `Plugin/build/libs/platform/`:

- `LibreLogin-Paper-<version>.jar`
- `LibreLogin-Velocity-<version>.jar`

They are labelled outputs of the current shared plugin implementation. Install only the artifact
matching the platform, and remove an older LibreLogin jar so two copies are not loaded. BungeeCord
is no longer a supported LibreLogin platform.

## 2FA and Protocolize

The TOTP API remains behind LibreLogin's existing provider interface. No secret or database
migration is required. On Velocity, QR display remains conditional on Protocolize being present
and compatible. If Protocolize is unavailable, the plugin logs a warning and QR-based 2FA cannot
be enabled through the map projector.

## Platform removal

BungeeCord support was removed from the source tree, dependencies, metadata and build. Existing
BungeeCord installations must remain on the previous LibreLogin artifact or migrate to Velocity.
Paper and Velocity artifacts are the only supported outputs of this branch.
