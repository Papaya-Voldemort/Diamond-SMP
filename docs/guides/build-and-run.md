# Build and Run

This repo targets Java 21 and Paper API metadata version 1.21.11.

## Build

Use the wrapper from the repo root:

```bash
./gradlew build
```

To build only the deployable plugin jar:

```bash
./gradlew :plugin-bootstrap:shadowJar
```

The deployable artifact is:

- `plugin-bootstrap/build/libs/Diamond-SMP-1.0.0.jar`

## Local Runtime Expectations

- Run on Paper `1.21.11`
- Run with Java `21`
- First startup writes config files into the plugin data folder
- Purchase retirement is stored in `purchase-history.yml`

## Deployment Flow

1. Build the plugin.
2. Copy the shadow jar into your Paper server `plugins/` directory.
3. Start the server once.
4. Review generated config files.
5. Restart the server after edits.

## Quick Verification

1. Check console for `Diamond SMP enabled`.
2. Run `/rules`.
3. Run `/string`.
4. Run `/godvillager spawn top testevent`.
5. Run `/serverevent start cat_hunt tools`.
6. Confirm `config.yml` values affect cooldowns and trade costs.

## Current Build Status

- `./gradlew build` passes
- Javadoc warnings exist in scaffold modules
- Some Paper world-border APIs used here are deprecated but still compile on 1.21.11
