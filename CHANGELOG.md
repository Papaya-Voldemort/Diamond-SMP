# Changelog

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
