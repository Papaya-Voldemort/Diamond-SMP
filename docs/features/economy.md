# Economy

Diamond SMP does not run a full currency plugin economy. Its economy is a progression economy centered on scarcity, event access, and convenience utilities.

## Main Economic Loops

### Reward Villager Economy

- Reward villagers sell god items.
- Prices combine item-specific currency and optional emerald cost.
- Stock retires globally after purchase.
- God-item prices are intentionally much higher than the original defaults so late-game purchases feel prestigious again.

### Diamond Sinks

- Diamonds are still valuable, but the plugin now pushes rich players toward expensive late-game sinks instead of letting ores flood the economy.
- Main sinks:
  - prestige enchanted golden apples
  - expensive perk books
  - high-cost god items
  - rare bonus trades on normal master villagers

### Master Villager Bonus Trades

- Normal non-plugin villagers can gain one extra diamond-heavy trade at master level.
- The default chance is `20%`.
- Reward pools are configurable and intentionally expensive.
- Plugin-managed reward villagers do not receive these extra vanilla-master trades.

### Utility Economy

- `/string` converts command cooldown into immediate crafting throughput.
- Easy golden apples and cobwebs reduce grind in support of PvP-heavy gameplay.

## What Is Not Implemented

- no player balance ledger
- no shop GUI
- no chest shops
- no tax or auction system
- no vault-based money integration

The plugin instead treats progression access itself as the scarce resource.
