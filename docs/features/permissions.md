# Permissions

Permissions are defined in `plugin.yml`.

## Player Defaults

- `diamondsmp.command.string`
- `diamondsmp.command.tpa`
- `diamondsmp.command.kit`
- `diamondsmp.command.trust`
- `diamondsmp.command.spawn`
- `diamondsmp.command.rules`
- `diamondsmp.command.event`
- `diamondsmp.command.party`
- `diamondsmp.command.pvp`

These default to `true`.

## Admin Defaults

- `diamondsmp.admin.godvillager`
- `diamondsmp.admin.events`
- `diamondsmp.admin.border`
- `diamondsmp.admin.kit`
- `diamondsmp.admin.godtest`
- `diamondsmp.admin.end`

These default to `op`.

## Special Cases

- `diamondsmp.admin.godtest` also gates the `godtest` kit in `kits.yml`.
- `diamondsmp.admin.godvillager` bypasses reward villager ownership checks.
