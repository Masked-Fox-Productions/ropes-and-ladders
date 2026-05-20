# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Minecraft mod targeting both Bedrock Edition (Script API + JSON definitions) and Java Edition (Fabric). The repo ships:

- `ropes_bp/` — behavior pack: block JSONs, items, and the script bundle entered at `scripts/main.js`.
- `ropes_rp/` — resource pack: lang strings, geometry, and textures.
- `java-ropes/` — Fabric mod: Gradle-based Java project with the same game logic.

The Bedrock Script API target is `@minecraft/server` 2.7.0 against `min_engine_version` 1.26.0. Scripts are ESM (`"type": "module"` at the repo root).

## Commands

```bash
# Bedrock tests
npm test                                                              # run all unit tests
node --import ./tests/register-hooks.mjs --test tests/RopeChain.test.mjs     # single file
node --import ./tests/register-hooks.mjs --test --test-name-pattern "pattern" tests/*.test.mjs  # filter by name

# Java tests
cd java-ropes && ./gradlew test                                       # run all Java tests
cd java-ropes && ./gradlew build                                      # build mod JAR
cd java-ropes && ./gradlew deployToMods                               # build and deploy to .minecraft/mods
```

The `--import ./tests/register-hooks.mjs` flag is required for Bedrock tests: it installs a Node loader hook that redirects `@minecraft/server` to `tests/stubs/minecraft-server.mjs`. Without it, importing any production module fails because the Bedrock module doesn't exist in Node.

There is no build step, no linter, and no package install for the Bedrock side — it has zero runtime dependencies. To install the add-on in Bedrock, zip the two pack directories as `*.mcpack`.

## Architecture

The codebase is split into three layers. Keep new code on the correct side of these seams.

**Domain layer (`scripts/domain/`)** — pure JavaScript, no `@minecraft/server` imports allowed. Domain classes define game logic, data models, and JSON serialization without ever touching a Bedrock API. This is what makes the domain testable under `node:test` with only a noop stub.

**Subsystem layer (`scripts/subsystem/`)** — each subsystem owns one Bedrock event subscription or interval. Constructors take the manager as their sole dependency; `.register()` wires up the Bedrock hook. They query the manager but never mutate other subsystems.

**Block handler layer (`scripts/handler/`)** — thin glue between `world.afterEvents.playerPlaceBlock` / `playerBreakBlock` and the domain. Each handler matches by `typeId`, then delegates to the manager.

The `RopeManager` is the single source of truth. No subsystem keeps its own state — they all query through the manager. When you mutate state, call `ropeManager.save()` so persistence stays in sync.

### Startup order (in `main.js`)

The order in `main.js` is load-bearing: the manager exists before any subsystem so its reference can be shared, and persistence is hydrated both on `worldLoad` and via a one-shot `system.run(...)` fallback because some Bedrock builds don't fire `worldLoad` on script reload.

### Persistence

State serializes to a single world dynamic property keyed by `ROPES_PERSISTENCE_KEY` in `util/RopeConstants.js`. The payload is the JSON form of all managed rope chains.

### Java parity

The Java edition follows the same architecture: domain classes have no Minecraft imports (testable with plain JUnit), subsystems handle event registration, and the mod entrypoint initializes everything in the correct order. See `docs/parity-checklist.md` for feature parity tracking.

## Conventions

- **All magic numbers go in `scripts/util/RopeConstants.js`.** Ranges, intervals, block IDs, climb speeds. Nothing hardcoded in handlers or domain classes.
- **Block identifiers are namespaced with `ropes:`** (e.g. `ropes:rope`, `ropes:rope_ladder`).
- **Domain code never imports `@minecraft/server`.** If you need the API in a domain class, that logic belongs in the subsystem or handler layer instead.
- **Mutations to state must call `ropeManager.save()`** so the world dynamic property stays current.

## Tests

Tests live in `tests/*.test.mjs` and use `node:test` + `node:assert/strict`. The loader hook in `tests/hooks.mjs` rewrites `@minecraft/server` to the stub at `tests/stubs/minecraft-server.mjs`. The stub exports inert `world` and `system` objects plus `__setWorld(w)` / `__setSystem(s)` / `__reset()` for tests that need to observe API calls.

When adding a test for code that imports `@minecraft/server`, install fakes via `__setWorld` **before** importing the module under test, or rely on the default noop stub.

Java tests live in `java-ropes/src/test/` and use JUnit 5. Domain tests need no Minecraft server — they test pure Java classes directly.
