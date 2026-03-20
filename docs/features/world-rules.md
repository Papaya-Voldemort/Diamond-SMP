# World Rules

Diamond SMP changes several world-level behaviors to support its progression model.

## Overworld

- Diamond ore generation pressure is increased through chunk-time ore rewriting.
- Diamond drops are multiplied at break time.
- World border defaults are configurable and editable through `/dsborder`.

## Nether

- Ancient debris is stripped from newly generated chunks when enabled.
- Netherite progression is blocked regardless of how the item would otherwise be obtained.

## The End

- End access is controlled by `/end`.
- When the End is closed:
  - portal activation is blocked
  - portal travel is cancelled
  - players found in The End are returned to the Overworld
- The open/closed state persists in `state.yml`.

## Dedicated PvP World

- Separate world name comes from `config.yml -> pvp.world-name`.
- Flat world generation is used.
- Structures are disabled.
- Mob spawning, day cycle, and weather cycle are disabled.
- Keep inventory and immediate respawn are enabled in that world for match control.
