# Cross-Platform Parity Checklist

Tracks feature parity between Bedrock and Java implementations.

## Phase 1

Java parity is built in layers: the **domain logic** (pure Java, JUnit-tested) lands
first, then the **Fabric runtime** (block/item registration, event handlers, rendering,
persistence wiring). "Domain only" below means the shared logic is ported and tested but
the Fabric glue that drives it in-game is still pending.

| Feature | Bedrock Status | Java Status | Notes |
|---|---|---|---|
| Domain model (RopeChain, RopeManager, indices, serialization) | Done | Done | `com.ropes.domain`, 32 JUnit tests porting the Bedrock domain suite |
| Rope block (place, extend, retract, recoil) | Done | Domain only | Chain logic ported; needs Fabric block + place/interact event wiring |
| Rope ladder block (place, extend, full recoil) | Done | Domain only | Chain logic ported; needs Fabric block + place/interact event wiring |
| Whip item (deploy, auto-retract, return to inventory) | Done | Not started | Needs Fabric item + use/attack handlers |
| Climbing mechanics (slow falling, jump-to-climb) | Done | Not started | Needs server tick + player input handlers |
| Rope cascade (ledge coils, multi-drop) | Done | Domain only | `createLedgeCoil` / `retractOneFromBottom` ported + tested |
| Support block break detection | Done | Domain only | Support index ported + tested; needs break-event wiring |
| Persistence (save/load rope chains) | Done | Domain only | `exportState` / `load` ported + tested; needs SavedData wrapper |

## Platform Differences (Acceptable)

| Difference | Bedrock | Java | Rationale |
|---|---|---|---|
