# Changelog

## 1.0.6 - 2026-03-24

- Rebalanced new-chunk ore pressure so diamonds are toned down while iron, gold, and coal are boosted through configurable chunk rewrite settings
- Reworked managed reward villagers into persistent, killable fixtures that drop tagged relocation eggs instead of expiring after 30 minutes
- Added exact-stack prestige enchanted golden apple crafting and rare master-villager diamond sink trades with expensive perk books
- Increased god-item prices and documented the new economy and villager behavior for live-server rollout

## 1.0.5 - 2026-03-22

- Fixed restricted enchant handling so normal `Protection IV` and `Sharpness V` items downgrade to the allowed cap instead of being blocked outright
- Sanitized stored enchants on books, villager trade outputs, inventory items, loot results, and anvil outputs while preserving plugin god items
- Cleaned up restriction listener hot paths and removed redundant chunk processing overhead in the same subsystem

## 1.0.2 - 2026-03-20

- Added real web downloads for the branded companion dependencies: PlaceholderAPI `2.12.2`, TAB `5.5.0`, and Custom Join Messages `17.9.1`
- Exposed those dependency URLs and jar names in `config.yml` so operators can override them without editing code
- Added PlaceholderAPI branding sync so the test server's companion plugin stack is seeded end-to-end

## 1.0.1 - 2026-03-20

- Disabled the party PvP beta by default so servers must explicitly opt in through `config.yml`
- Added startup branding sync for the Diamond SMP test server MOTD, icon, and companion TAB or CustomJoinMessages configs
- Replaced the default DiamondSMP message, rules, and event copy with the exact branded text used on the Diamond SMP test server

## 1.0.0 - 2026-03-20

Initial release of Diamond SMP as a modular Paper plugin for Minecraft `1.21.11`.

- Added event-driven progression with Name Tag and Cat Hunt server events
- Added owner-bound reward villagers with limited stock and global purchase retirement
- Added event-exclusive god items and progression restrictions around netherite and enchants
- Added party PvP flows with kits, rematch support, dedicated arena handling, and player-state restore
- Added survival utility systems for teleport requests, kits, trust-hit, spawn, rules, and End access
- Added world border admin controls and PlaceholderAPI event placeholders
- Added config-driven bootstrap resources for gameplay, loot, crafting, integrations, villagers, and rules
