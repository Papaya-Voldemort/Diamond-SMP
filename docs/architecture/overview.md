# Architecture Overview

Diamond SMP is structured as a layered multi-module Paper plugin.

## Layers

- `plugin-bootstrap`: only plugin startup and lifecycle entrypoint
- `platform-paper`: server-facing adapters
- `core-api`: stable cross-module contracts
- `core-domain`: domain records and identifiers
- `core-service`: service wiring and orchestration
- `infra-*`: supporting technical capabilities

## Dependency Rule

The bootstrap module may depend on everything required to assemble the plugin. Lower layers must not depend on bootstrap. Feature implementation should prefer contracts in `core-api` over direct module coupling.

