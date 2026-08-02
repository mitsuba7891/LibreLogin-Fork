# AuthLimbo changelog

This fork artifact was reviewed and updated with AI assistance (GPT Luna 5.6).

## 1.0.0

- Added a standalone Paper limbo companion for the Velocity architecture.
- Creates/uses the dedicated `auth_void` world.
- Keeps players at the limbo spawn and prevents movement, unsafe teleportation, interaction, damage, item drops and inventory actions.
- Uses a normal registered Paper backend instead of NanoLimbo.
- Does not access the authentication database and does not provide login commands.
- Requires modern Velocity forwarding on the backend and must not be installed alongside LibreLogin-Paper on the same auth server.

See the root [`CHANGELOG.md`](../CHANGELOG.md) for the complete fork history and [`README.md`](README.md) for installation.
