# Diamond SMP

Diamond SMP is a modular Paper plugin for Minecraft Java `1.21.11` built around event-driven endgame progression. Instead of letting normal survival reach full netherite and top-tier enchants, the server pushes late-game power into admin-run events, reward villagers, custom god items, and a separate party PvP flow.

## What the Plugin Actually Does

- Replaces normal top-end progression with event-exclusive god gear.
- Spawns owner-bound reward villagers that sell limited stock and retire purchases globally.
- Blocks netherite progression, restricted enchants, and Strength II brewing/consumption.
- Rewrites diamond generation pressure with chunk-time ore amplification and extra drop multipliers.
- Adds party-based `/pvp` matches in a dedicated arena world with snapshot restore.
- Adds utility commands for teleport requests, kits, rules, trust-hit, parties, border control, and End access.
- Supports PlaceholderAPI event placeholders.

## Core Gameplay Loop

1. Players progress through a survival ruleset that deliberately blocks normal endgame.
2. Admins run a server event such as Name Tag or Cat Hunt.
3. The winner receives a reward villager tied to that player for a limited time.
4. The villager sells event-exclusive god items.
5. Purchased stock retires globally until an admin resets the purchase history.

## Major Systems

### Event-Exclusive God Items

- Helmet: Prot IV, Respiration III, Aqua Affinity, permanent Fire Resistance, Dolphin's Grace, and Water Breathing.
- Chestplate: Prot IV and a permanent health boost.
- Leggings: Prot IV and permanent Resistance II.
- Boots: Prot IV, Depth Strider V, Feather Falling IV, Soul Speed III, and permanent Speed II.
- Sword: Sharpness V, Fire Aspect II, Looting V, Sweeping Edge V, and Strength II while held.
- Axe: Efficiency X and extra armor durability damage in PvP.
- Pickaxe: Efficiency X, Fortune VII, vein mining, auto-smelting, and slow bedrock breaking.
- Bow: Power V, Unbreaking III, Infinity, left-click short-bow burst, right-click buffed normal shot, and no arrow consumption in either mode.
- Infinite Totem: reusable totem with a shared cooldown.
- Enchanted Gapple: event-exclusive consumable available from the tools villager pool.

### Progression Locks

- Netherite items and ingots are blocked from normal progression paths.
- Ancient debris is stripped from newly generated Nether chunks.
- Loot tables have ancient debris, netherite items, and restricted enchants removed.
- Protection IV and Sharpness V are blocked through enchanting, anvil output, smithing, crafting results, and villager trade generation.
- Strength II brewing and drinking are blocked outside the plugin's intended systems.

### Party PvP

- `/p` manages invitations, accepts, kicks, disbands, and party listings.
- `/pvp` opens a hub menu with compatible modes, rematch access, status info, and return options.
- `/pvp start <mode> <kit>` still supports direct launch for power users.
- Matches run in a dedicated flat PvP world with generated arenas and kit-based loadouts.
- Player inventories, armor, effects, XP, food, and location are snapshotted and restored when the player returns to the SMP.

## Player Commands

- `/string`
- `/tpa`, `/tpaccept`, `/tpdeny`
- `/kit <name>`
- `/trust <player>`, `/untrust <player>`
- `/spawn`
- `/rules`
- `/event`
- `/p <player|accept|leave|list|kick|disband>`
- `/pvp [menu|status|modes|kits|leave|rematch|start <mode> <kit>]`

## Admin Commands

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

Generated on first boot:

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

Operator documentation:

- [Configuration Guide](/Volumes/External%20Home/Kids%20Home/dev/Diamond-SMP/docs/guides/configuration-guide.md)
- [Commands](/Volumes/External%20Home/Kids%20Home/dev/Diamond-SMP/docs/features/commands.md)
- [Progression](/Volumes/External%20Home/Kids%20Home/dev/Diamond-SMP/docs/features/progression.md)
- [Items](/Volumes/External%20Home/Kids%20Home/dev/Diamond-SMP/docs/features/items.md)
- [Villagers](/Volumes/External%20Home/Kids%20Home/dev/Diamond-SMP/docs/features/villagers.md)
- [Combat and PvP](/Volumes/External%20Home/Kids%20Home/dev/Diamond-SMP/docs/features/combat.md)

## Release

```bash
./gradlew build
./gradlew :plugin-bootstrap:shadowJar
```

Current release:

- `1.0.0`
- Paper `1.21.11`
- Java `21`
- Deployable jar: `plugin-bootstrap/build/libs/Diamond-SMP-1.0.0.jar`

## Deploy

1. Copy `plugin-bootstrap/build/libs/Diamond-SMP-1.0.0.jar` into `plugins/`.
2. Start the server once to generate config and state files.
3. Edit the generated YAML files inside `plugins/DiamondSMP/`.
4. Restart the server.

GitHub release notes for `1.0.0` live in `docs/releases/1.0.0.md`.

## Validation Checklist

1. Confirm player commands register and respond.
2. Confirm `/serverevent start nametag tools` broadcasts and tracks participants.
3. Confirm the winning player receives a reward villager.
4. Purchase one villager item, then verify it disappears for everyone until reset.
5. Confirm End access is blocked while closed and restored when opened.
6. Confirm `/pvp` opens the hub and a match restores the original SMP state after return.
