# Kits

Kits are used both as normal command kits and as the loadout source for party PvP.

## Current Included Kits

- `starter`
- `godtest`
- `archer`
- `tank`
- `example_advanced`

## PvP Kit Pool

By default PvP only allows the kits listed in `config.yml -> pvp.kits`:

- `starter`
- `archer`
- `tank`

## Supported Kit Metadata

Each item entry in `kits.yml` can define:

- `material`
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
- potion base type, color, and custom effects
- written book title, author, and pages

## Access Control

- Global player kit access: `config.yml -> kits.allow-regular-users`
- Per-kit access: `permission`

## Good Operator Practice

- Keep PvP kits small and predictable.
- Use `example_advanced` as a metadata reference, not as a balance target.
- Do not expose `godtest` outside admin validation.
