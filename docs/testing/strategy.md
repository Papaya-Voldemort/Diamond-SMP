# Testing Strategy

Planned testing layers:

- Unit tests for domain records, registries, and config loaders
- Integration tests for startup wiring and command/event registration
- Smoke tests against a Paper server for plugin enable/disable lifecycle
- Regression coverage for version-sensitive mechanics

The scaffold only provides test boundaries, not full test suites yet.

