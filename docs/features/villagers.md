# Villagers

Reward villagers are the core reward-distribution mechanic.

## Villager Types

### `top`

- helmet
- chestplate

### `bottom`

- leggings
- boots
- infinite totem

### `tools`

- sword
- axe
- pickaxe
- bow
- enchanted golden apple

## Runtime Behavior

- Spawn at the winner or admin's location.
- Store villager type, source event, owner UUID, and owner name in persistent data.
- Follow their owner.
- Teleport closer if they drift too far away.
- Persist indefinitely by default instead of expiring after 30 minutes.
- Rebuild their trade list when opened so retired items disappear immediately.

## Relocation Eggs

- Only plugin-managed reward villagers can be converted into relocation eggs.
- When one dies through an allowed cause, it drops one tagged villager egg.
- Using that egg recreates the same managed villager type and preserves its owner/event identity.
- Normal villagers never drop relocation eggs.

This makes event villagers movable without deleting the content forever.

## Ownership Rules

- Owner can use the villager normally.
- Other players are blocked.
- Admins with `diamondsmp.admin.godvillager` bypass the ownership check.

## Global Retirement

When a trade is purchased:

- the item key is marked in `purchase-history.yml`
- that item disappears from future villager menus
- it stays retired until `/godvillager reset`

This applies across all managed reward villagers, not just the one that sold it.

## Normal Master Villagers

- Non-plugin master villagers can gain one extra high-cost diamond trade.
- This system is separate from reward villagers and is controlled through config-backed trade definitions.
