# Crafting and Loot

Diamond SMP modifies vanilla crafting, generated loot, and ore pressure.

## Crafting Overrides

### Golden Apples

- When a normal golden apple recipe is prepared, the output is replaced with 8 apples.

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

## Diamond Pressure

Diamond progression is intentionally accelerated, but in a controlled custom way.

### Chunk-Time Ore Rewrites

When a new Overworld chunk loads:

- diamond veins can be expanded based on `world.diamond-vein-size-multiplier`
- exposed diamond ore can cause nearby host blocks to be converted into more diamond ore based on `world.exposed-diamond-multiplier`

This happens once at chunk generation/load time, so operators should set these values before heavy world exploration.

### Break-Time Bonus Drops

- Breaking diamond ore without Silk Touch gives bonus diamonds based on `world.diamond-drop-multiplier`.
- God pickaxes bypass this bonus path because they have their own vein-mine and smelting logic.

## God Pickaxe Mining

When using the god pickaxe:

- ore veins are mined in batches
- drops are auto-smelted where applicable
- extra experience is collected and spawned
- bedrock can be broken after the configured delay
