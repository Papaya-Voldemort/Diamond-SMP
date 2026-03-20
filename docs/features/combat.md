# Combat and PvP

Diamond SMP changes both normal SMP combat and the isolated party PvP system.

## SMP Combat Rules

### Combat Tagging

- Any player-vs-player hit tags both attacker and victim.
- Tag length uses `combat.tag-seconds`.
- Tagged players see an action bar countdown.
- Tagged players cannot use teleport requests.
- Logging out while tagged kills the player unless they are in a managed PvP session.

### Trust-Hit

- Trusted pairs cannot damage each other in normal SMP combat.
- This only bypasses damage outside the dedicated `/pvp` arena system.

### God Weapon Effects

- God sword grants Strength II while held.
- God axe applies extra armor durability loss in PvP.
- God bow uses left click for the short-bow burst and right click for a stronger normal bow shot, and neither mode consumes arrows.
- God pickaxe enables special mining behavior instead of direct combat bonuses.

## Party PvP System

### Match Flow

1. Create a party with `/p`.
2. Open `/pvp`.
3. Pick a configured mode.
4. Pick or cycle to a kit.
5. Launch if the party exactly matches the mode size.

### Validation

Before launch the service checks:

- the mode exists
- the kit exists and is allowed
- the party exists and has at least 2 players
- the party size exactly matches the mode total
- every party member is online
- no party member is already in a managed PvP match
- no party member is combat tagged
- an arena slot is free

### Arena Runtime

- A flat PvP world is created or loaded automatically.
- Arenas are generated on demand from config dimensions and materials.
- Friendly fire is disabled between teammates.
- Managed PvP deaths clear drops and suppress death messages.
- Losing players respawn into the arena result flow instead of returning to vanilla respawn behavior.

### Snapshot Restore

When a match starts the plugin captures:

- location
- game mode
- full inventory
- armor
- extra inventory contents
- potion effects
- XP, level, total experience
- food, saturation, exhaustion
- fire ticks
- health

Players can then return to the SMP with their previous state restored.

### New PvP Menu Options

- Hub menu with mode overview and compatible-mode guidance.
- Arena availability preview.
- Party roster preview.
- Rematch shortcut from both the hub and post-match screen.
- Setup menu with launch checks.
- Previous, next, and random kit selection.
- `/pvp status`, `/pvp modes`, and `/pvp kits` text commands for fast operator support.

### Post-Match

After the last team standing wins:

- the arena slot is freed
- winners are announced to match participants
- each player receives a personal stat summary
- players can choose Return to SMP, Rematch, or New PvP
