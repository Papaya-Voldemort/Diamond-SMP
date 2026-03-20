# Config and Reload Model

All config-backed systems should:

- define a typed config model
- provide a `ConfigLoader<T>`
- expose a read-only `ConfigRepository<T>`
- implement reload only through an explicit reload path

Hot-reload behavior is not implemented yet, but the interfaces are reserved.

