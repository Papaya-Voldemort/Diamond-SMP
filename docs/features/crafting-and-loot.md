# Crafting and Loot

Diamond SMP modifies vanilla crafting, generated loot, and ore pressure.

## Crafting Overrides

### Golden Apples

- When the easy-gap recipe is enabled, a normal golden apple recipe is replaced with 8 apples.

### Prestige God Apple

- The plugin adds a custom enchanted golden apple craft in a normal crafting table.
- Recipe:
  - center: 1 golden apple
  - every outer slot: 64 diamond blocks
- The recipe is validated in plugin code, so partial stacks do not work.
- Shift-click and other bulk-craft shortcuts are blocked for this recipe to prevent underpay or dupe behavior.
- Operators can tune or disable the recipe in `crafting.yml`.

### Cobwebs

- The configured string-to-cobweb recipe yields 5 cobwebs.
- `/string` exists largely to support this utility economy.

### Restricted Outputs

The plugin blocks crafting results that would produce:

- restricted netherite items
- restricted high-tier enchant outputs

The same policy is also enforced in anvils and smithing tables.

## Loot Cleanup

On loot generation the plugin removes:

- ancient debris
- restricted netherite items
- restricted enchanted items

This keeps generated structures aligned with the event-based progression model.

## Ore Rebalance

Ore pressure is handled during new Overworld chunk generation/load.

### Chunk-Time Ore Rewrites

When a new Overworld chunk loads:

- diamond ore is only lightly expanded compared with the old inflated defaults
- exposed diamond chain growth is capped
- iron veins are boosted the most
- coal is boosted strongly enough to feel like a reliable bulk resource
- gold is boosted moderately

This happens once at chunk generation/load time, so operators should set these values before heavy world exploration. Existing explored chunks are not retroactively changed by the plugin.

### Break-Time Diamond Drops

- Breaking diamond ore without Silk Touch can still award bonus diamonds, but the default multiplier is now `1.0`.
- God pickaxes bypass this bonus path because they have their own vein-mine and smelting logic.

## God Pickaxe Mining

When using the god pickaxe:

- ore veins are mined in batches
- drops are auto-smelted where applicable
- extra experience is collected and spawned
- bedrock can be broken after the configured delay

## Diamond Perk Books

Late-game diamond sink enchants are sold as expensive perk books through villager trades:

- `Prospector` applies to pickaxes and can occasionally grant an extra ore drop
- `Bulwark` applies to chestplates and reduces incoming damage
- `Momentum` applies to boots and grants movement utility

These are plugin-tagged items, not random enchanting-table outputs.
