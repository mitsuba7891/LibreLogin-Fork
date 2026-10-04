# AuthLimbo changelog

This fork artifact was reviewed and updated with AI assistance (Freebuff assistant using GPT Luna 5.6).

## LibreLogin bundle 0.25.0-beta.5

- Documentation/release bundle update; the `AuthLimbo-1.0.0.jar` companion behavior is unchanged.

## LibreLogin bundle 0.25.0-beta.4

- Documentation/release bundle update; the `AuthLimbo-1.0.0.jar` companion behavior is unchanged.

## LibreLogin bundle 0.25.0-beta.3

- Documentation/release bundle update; the `AuthLimbo-1.0.0.jar` companion behavior is unchanged.

## LibreLogin bundle 0.25.0-beta.2

- Included as the Paper limbo companion for the updated Velocity authentication flow.
- The companion version remains `1.0.0`; backend configuration and forwarding requirements are unchanged.

## LibreLogin bundle 0.24.8

- Expanded the unauthenticated lock to chat, commands, block/entity interaction, direct/projectile damage, inventory operations, pickup/drop, hand swaps and consumption.
- Added Paper 26.2-compatible gamerule, PvP and spawn handling while preserving the existing `AuthLimbo-1.0.0.jar` companion version.

## 1.0.0

- Added a standalone Paper limbo companion for the Velocity architecture.
- Creates/uses the dedicated `auth_void` world.
- Keeps players at the limbo spawn and prevents movement, unsafe teleportation, interaction, damage, item drops and inventory actions.
- Uses a normal registered Paper backend instead of NanoLimbo.
- Does not access the authentication database and does not provide login commands.
- Requires modern Velocity forwarding on the backend and must not be installed alongside LibreLogin-Paper on the same auth server.

See the root [`CHANGELOG.md`](../CHANGELOG.md) for the complete fork history and [`README.md`](README.md) for installation.
