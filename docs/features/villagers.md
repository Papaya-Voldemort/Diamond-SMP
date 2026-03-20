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
- Store villager type, source event, owner UUID, owner name, and expiry in persistent data.
- Follow their owner.
- Teleport closer if they drift too far away.
- Expire automatically after `villagers.despawn-minutes`.
- Rebuild their trade list when opened so retired items disappear immediately.

## Ownership Rules

- Owner can use the villager normally.
- Other players are blocked.
- Admins with `diamondsmp.admin.godvillager` bypass the ownership check.

## Global Retirement

When a trade is purchased:

- the item key is marked in `purchase-history.yml`
- that item disappears from future villager menus
- it stays retired until `/godvillager reset`

This applies across all villagers, not just the one that sold it.
