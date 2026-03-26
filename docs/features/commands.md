# Commands

This page documents the live command surface exposed by `plugin.yml` and the Paper command executors.

## Player Commands

### `/string`

- Fills every empty inventory slot with 64 string.
- Uses `cooldowns.string-command-seconds`.
- Intended as a utility shortcut for the cobweb recipe and general SMP setup.

### `/tpa <player>` and `/tpa spawn`

- Sends a teleport request to another player.
- `/tpa spawn` routes to `/spawn`.
- Blocked while the sender or target is combat tagged.
- Disabled when `systems.teleport` is false.

### `/tpaccept` and `/tpdeny`

- Accepts or denies the latest pending teleport request.
- Acceptance teleports the requester to the target.
- Requests expire after `cooldowns.tpa-seconds`.

### `/kit <name>`

- Gives a configured kit from `kits.yml`.
- Global access is controlled by `kits.allow-regular-users`.
- Individual kits can also require their own permission.

### `/trust <player>` and `/untrust <player>`

- Creates a mutual trust-hit relationship.
- Trusted pairs cannot damage each other in normal SMP combat.
- Trust protection is ignored inside managed PvP matches where the players are opposing teams.

### `/spawn`

- Teleports the player to the current world spawn.

### `/rules`

- Opens the rules GUI built from `rules.yml`.

### `/event`

- Shows the active event snapshot if one is running.
- Includes event name, remaining players, total participants, reward villager, and summary text.

### `/p`

Party management command for PvP.

This beta command is disabled until `config.yml -> pvp.enabled` is turned on.

- `/p <player>`: send invite.
- `/p accept [player]`: accept the latest invite or a specific inviter.
- `/p list`: show current members.
- `/p leave`: leave the party.
- `/p kick <player>`: leader-only removal.
- `/p disband`: leader-only full disband.

### `/pvp`

Now supports both GUI-first and direct-start usage.

This beta command is disabled until `config.yml -> pvp.enabled` is turned on.

- `/pvp`
- `/pvp menu`
- `/pvp status`
- `/pvp modes`
- `/pvp kits`
- `/pvp leave`
- `/pvp rematch`
- `/pvp start <mode> <kit>`
- `/pvp <mode>`
- `/pvp <mode> <kit>`

`/pvp` behavior:

- Opens a hub menu with mode cards, party status, compatible mode checks, rematch access, and arena availability.
- Selecting a mode opens a setup menu with party roster, launch checks, selected kit, previous/next kit cycling, and random kit selection.
- Direct start still validates party size, online roster, combat tags, arena availability, and allowed kits.

## Admin Commands

### `/godvillager`

- `/godvillager spawn <top|bottom|tools> [event]`
- `/godvillager reset`
- `/godvillager clear`

Use this to spawn reward villagers manually, clear active villagers, or reset retired stock.

### `/serverevent`

- `/serverevent status`
- `/serverevent stop`
- `/serverevent start <nametag|cat_hunt> <top|bottom|tools>`

Starts or stops the live event framework and selects the villager reward bucket.

### `/dsborder`

- `/dsborder size <diameter> [seconds]`
- `/dsborder center <x> <z>`
- `/dsborder warning <distance> <time>`
- `/dsborder reset`

### `/godtest`

- Gives one of every god item for validation and balancing.

### `/end`

- `/end open`
- `/end close`
- `/end status`

Controls End portal activation and active player access.

### `/beacon`

- `/beacon`
- `/beacon help`
- `/beacon stats`
- `/beacon setTier <tier>`
- `/beacon giveCore <type>`
- `/beacon debug`

Targets the beacon the player is looking at within 10 blocks, otherwise the nearest beacon within 10 blocks.

- `/beacon` shows tier, active state, selected effects, radius, bonuses, and next upgrade cost.
- `/beacon help` summarizes the tier path and upgrade cores.
- `/beacon stats` shows exact multipliers, linger, and passive bonuses.
- Admin subcommands allow direct tier mutation, core distribution, and runtime debug output.
