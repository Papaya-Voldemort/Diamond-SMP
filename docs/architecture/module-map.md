# Module Map

| Module | Purpose | Notes |
| --- | --- | --- |
| `plugin-bootstrap` | Runnable Paper plugin | Keep thin |
| `platform-paper` | Bukkit/Paper integration seams | Event/listener registration lives here |
| `core-api` | Shared interfaces and registries | Stable boundary |
| `core-domain` | Domain records | No platform logic |
| `core-service` | Wiring and orchestration | Service graph home |
| `infra-config` | Config contracts and reload flows | YAML-backed now |
| `infra-persistence` | Persistence abstractions | DB/files later |
| `infra-command` | Command contracts | Registration adapters later |
| `infra-events` | Custom plugin event contracts | Internal event surface |
| `test-fixtures` | Shared fixtures | Test-only helpers |

