# Lifecycle

Expected startup phases:

1. Bootstrap plugin context
2. Load configs
3. Register services
4. Register listeners
5. Register commands
6. Mark post-enable

Future work should map all major subsystems onto these phases instead of doing ad hoc startup work.

