# Chat and Tags

There is no custom chat formatter or rank-tag system implemented in this plugin today.

## What Is Implemented

### Name Tag Event Logic

The Name Tag server event uses color-state language internally:

- players start green
- eliminated players become red
- last green player wins

This is event logic, not a permanent chat/tag framework.

### Rules and Event Messaging

- rich clickable messages are used for trust, teleport, and party acceptance
- `/event` and PlaceholderAPI expose current event state

## What To Expect

If you need full chat channels, prefixes, suffixes, or rank styling, that still needs a separate plugin or a future feature expansion.
