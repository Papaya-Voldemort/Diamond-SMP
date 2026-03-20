# Events

Server events are the central progression trigger in Diamond SMP.

## How Events Work

- Only one event can run at a time.
- All online players are enrolled when the event starts.
- Each event type controls how eliminations or win conditions work.
- The winner receives a reward villager of the admin-selected type.
- If the winner is offline when reward delivery happens, the villager is queued and spawned when they next join.

## Implemented Event Types

### Name Tag

- All players start as active green participants.
- When a green participant dies to another green participant, the victim turns red and is eliminated.
- Last green player remaining wins.

### Cat Hunt

- All online players are enrolled.
- First participating player to kill a cat wins.
- Useful as a fast test event or lightweight live event.

## Admin Control

- `/serverevent start <nametag|cat_hunt> <top|bottom|tools>`
- `/serverevent status`
- `/serverevent stop`

## Player Visibility

- `/event` reports the active snapshot.
- joining players receive current event status if an event is live
- PlaceholderAPI placeholders expose event status to scoreboards or chat formats

## PlaceholderAPI

If PlaceholderAPI is installed, the plugin registers:

- `%diamondsmp_current_event%`
- `%diamondsmp_current_event_name%`
- `%diamondsmp_event_status%`
- `%diamondsmp_event_reward%`
- `%diamondsmp_event_participants%`
- `%diamondsmp_event_remaining%`
