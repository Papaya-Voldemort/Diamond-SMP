# Configuration Guide

Diamond SMP uses several YAML files because operators are expected to tune gameplay without code changes.

## Boot Process

On first startup the plugin writes its config bundle into the plugin data folder. Edit those generated files there, not the copies under `src/main/resources`.

## Main Files

### `config.yml`

Primary runtime switches.

- `cooldowns`
  - `infinite-totem-seconds`
  - `string-command-seconds`
  - `tpa-seconds`
- `combat`
  - `god-bow-base-damage`
  - `god-bow-headshot-bonus`
  - `god-bow-velocity`
  - `god-bow-range`
  - `axe-armor-durability-multiplier`
  - `bedrock-break-seconds`
  - `tag-seconds`
- `world`
  - `diamond-drop-multiplier`
  - `exposed-diamond-multiplier`
  - `diamond-vein-size-multiplier`
  - `remove-ancient-debris-from-new-chunks`
  - `disable-netherite-progression`
  - `disable-restricted-enchants`
  - `disable-strength-ii`
- `villagers`
  - `despawn-minutes`
  - `costs.<item>`
- `kits.allow-regular-users`
- `world-border`
  - `default-size`
  - `default-center.x`
  - `default-center.z`
  - `warning-distance`
  - `warning-time`
- `systems`
  - `god-items`
  - `teleport`
  - `events`
  - `rules-gui`
  - `border-tools`
- `pvp`
  - `enabled`
  - `beta`
  - `world-name`
  - `party-invite-timeout-seconds`
  - `arena-count`
  - `arena.y-level`
  - `arena.half-size`
  - `arena.height`
  - `arena.spacing`
  - `arena.spawn-spread`
  - `arena.floor-material`
  - `arena.wall-material`
  - `kits`
  - `gui.hub-title`
  - `gui.setup-title`
  - `gui.result-title`
  - `gui.*-size`
  - `gui.*-filler`
  - `gui.*-icon`
  - `modes.<mode>.display-name`
  - `modes.<mode>.team-sizes`
  - `modes.<mode>.icon`
- `branding`
  - `enabled`
  - `sync-server-icon`
  - `sync-companion-plugin-configs`
  - `motd.line-1`
  - `motd.line-2`
  - `companion-downloads.enabled`
  - `companion-downloads.placeholderapi.*`
  - `companion-downloads.tab.*`
  - `companion-downloads.custom-join-messages.*`

### `messages.yml`

Every major player-facing message lives here, including command responses, party flow, villagers, events, End access, and PvP summaries. The default copy now matches the branded Diamond SMP test server wording.

### `rules.yml`

Controls the `/rules` GUI title and section cards. The bundled defaults match the test server's Field Manual branding.

### `villagers.yml`

Defines the stock for `top`, `bottom`, and `tools` reward villagers. Each trade can set:

- `currency`
- `currency-amount`
- `emerald-cost`

### `events.yml`

Documents which event types should be considered enabled and provides short descriptions. Current code ships Name Tag and Cat Hunt with the same branded descriptions used on the test server.

### `kits.yml`

Defines named kits for `/kit` and `/pvp`. Supports:

- `permission`
- item `material`
- `amount`
- `name`
- `lore`
- `enchants`
- `damage`
- `unbreakable`
- `custom-model-data`
- `item-flags`
- `leather-color`
- `skull-owner`
- book metadata
- potion metadata and custom potion effects

### `crafting.yml`

- `recipes.easy-gaps`
- `recipes.easy-cobs`

The current code uses the custom recipes documented in the crafting feature page.

### `loot.yml`

- `remove-ancient-debris`
- `remove-netherite-items`

### `items.yml`

Currently a metadata placeholder because god items are built in code and tagged through persistent data.

### `integrations.yml`

Reserved for third-party integration settings. PlaceholderAPI is the only implemented optional integration and does not require extra configuration here right now.

### `world-rules.yml`

Reserved companion file. The actual world and progression switches are currently in `config.yml`.

## State Files

- `purchase-history.yml`: global retired villager items.
- `state.yml`: whether the End is open or closed.

## Recommended Operator Changes

1. Verify villager prices before your first live season.
2. Decide whether to opt into the PvP beta by turning `pvp.enabled` on.
3. Adjust diamond multipliers before generating large amounts of map terrain.
4. Decide whether `/kit` should be player-facing or admin-only.
5. Review all messages and rules text so they match your server language.
6. Decide whether to keep the bundled Diamond SMP branding sync for the MOTD, icon, companion configs, and companion plugin downloads.
7. Override any companion download URL in `branding.companion-downloads` if you need a different source or version.
8. Review `pvp.gui` materials and titles if you want a different PvP menu look after enabling the beta.
