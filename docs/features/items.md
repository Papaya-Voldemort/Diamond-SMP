# Items

Diamond SMP's signature items are the event-exclusive god items built in `GodItemRegistry`.

## Item Model

- God items are created in code, not from `items.yml`.
- Each item is tagged with persistent data so the plugin can recognize it reliably.
- Restriction checks use those tags to distinguish legal plugin items from blocked vanilla progression items.

## Full God Item Reference

### Helmet

- Base material: `DIAMOND_HELMET`
- Enchants: Protection IV, Respiration III, Aqua Affinity
- Passive effects while worn: Fire Resistance, Dolphin's Grace, Water Breathing

### Chestplate

- Base material: `DIAMOND_CHESTPLATE`
- Enchants: Protection IV
- Passive effect while worn: strong health boost

### Leggings

- Base material: `DIAMOND_LEGGINGS`
- Enchants: Protection IV
- Passive effect while worn: Resistance II

### Boots

- Base material: `DIAMOND_BOOTS`
- Enchants: Protection IV, Depth Strider V, Feather Falling IV, Soul Speed III
- Passive effect while worn: Speed II

### Sword

- Base material: `DIAMOND_SWORD`
- Enchants: Sharpness V, Fire Aspect II, Looting V, Sweeping Edge V
- Passive effect while held: Strength II

### Axe

- Base material: `DIAMOND_AXE`
- Enchants: Efficiency X
- Special behavior: damages enemy armor durability harder than normal

### Pickaxe

- Base material: `DIAMOND_PICKAXE`
- Enchants: Efficiency X, Fortune VII
- Special behavior: vein mining, auto-smelting, bedrock breaking

### Bow

- Base material: `BOW`
- Enchants: Power V, Unbreaking III, Infinity
- Special behavior:
  - left click fires the instant short-bow shot
  - short-bow shots deal extra damage to players and even more to non-players
  - right click fires a stronger normal-bow shot, roughly in the Power VII range
  - neither mode consumes arrows

### Infinite Totem

- Reusable custom totem recognized through god-item metadata.
- Resupplies itself after use if the cooldown is ready.
- Applies a normal client cooldown to the totem slot.

### Enchanted Gapple

- Event-exclusive consumable sold through villager stock.

## Acquisition

- Primary path: reward villagers.
- Test path: `/godtest` for admins.
