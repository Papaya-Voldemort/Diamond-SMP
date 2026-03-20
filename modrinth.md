# Diamond SMP Modrinth Draft

## Suggested Metadata

- Title: `Diamond SMP`
- Slug: `diamond-smp`
- Project type: `plugin`
- Visibility: `unlisted`
- Supported loaders: `paper`
- Supported game versions: `1.21.11`
- Environment: server-side required, client-side unsupported
- License: `All Rights Reserved` unless you want to publish under a different explicit license

## Short Description

Event-driven Paper plugin that replaces normal endgame with admin-run events, reward villagers, god items, and party PvP.

## Full Description

Diamond SMP is a modular Paper plugin for Minecraft `1.21.11` designed around a controlled endgame loop. Instead of letting vanilla progression run straight into normal netherite and unrestricted enchants, the server shifts late-game power into scheduled events, owner-bound reward villagers, curated god items, and a dedicated party PvP system.

This makes progression more social and more intentional:

- admins run events to create server-wide moments
- winners unlock limited reward villagers tied to their account
- villagers sell powerful items that are not part of normal survival progression
- purchased reward stock retires globally until an admin resets it
- players can jump into organized party PvP without risking their normal SMP inventory

## Highlights

- Event-exclusive god gear instead of standard netherite progression
- Reward villagers with owner checks, limited-time access, and configurable stock
- Global retirement of purchased villager items until reset
- Name Tag and Cat Hunt event flows
- Dedicated `/pvp` arena flow with kits, rematches, and full player-state restore
- Survival utility commands including `/tpa`, `/spawn`, `/rules`, `/event`, `/string`, and kits
- Trust-hit system to stop friendly SMP damage
- Configurable End access controls and world border administration
- PlaceholderAPI expansion for event status
- Progression locks for netherite, restricted enchants, and Strength II usage
- Diamond ore and drop multipliers to reshape resource pressure
- Config-driven crafting, loot, kits, rules, villagers, integrations, and world rules

## Best Fit

Diamond SMP is intended for private or semi-private survival servers that want:

- a curated late game instead of open vanilla escalation
- admin-led seasonal events
- rare rewards with strong social visibility
- a safer way to run PvP side content inside the same server

## Installation

1. Build the release jar with `./gradlew build`.
2. Copy `plugin-bootstrap/build/libs/Diamond-SMP-1.0.0.jar` into the server `plugins/` directory.
3. Start the Paper server once to generate config files.
4. Review and edit the generated files inside `plugins/DiamondSMP/`.
5. Restart the server after configuration changes.

## Core Commands

Player commands:

- `/string`
- `/tpa`, `/tpaccept`, `/tpdeny`
- `/kit <name>`
- `/trust <player>`, `/untrust <player>`
- `/spawn`
- `/rules`
- `/event`
- `/p <player|accept|leave|list|kick|disband>`
- `/pvp [menu|status|modes|kits|leave|rematch|start <mode> <kit>]`

Admin commands:

- `/godvillager spawn <top|bottom|tools> [event]`
- `/godvillager reset`
- `/godvillager clear`
- `/serverevent status`
- `/serverevent stop`
- `/serverevent start <nametag|cat_hunt> <top|bottom|tools>`
- `/dsborder <size|center|warning|reset>`
- `/godtest`
- `/end <open|close|status>`

## Configuration Files

Generated on first startup:

- `config.yml`
- `messages.yml`
- `rules.yml`
- `villagers.yml`
- `events.yml`
- `kits.yml`
- `crafting.yml`
- `loot.yml`
- `items.yml`
- `integrations.yml`
- `world-rules.yml`

## Release Notes Text

Version `1.0.0` ships the first full public release of the Diamond SMP Paper plugin. It includes event-driven reward villagers, god items, survival progression locks, party-based PvP with state restore, utility player commands, admin event controls, world border tools, End access management, PlaceholderAPI support, and config-backed gameplay systems across items, kits, villagers, loot, and world rules.
