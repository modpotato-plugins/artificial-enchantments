# Artificial Enchantments - Paper 1.21+ API Plan

## TL;DR

> **Quick Summary**: Build a Paper 1.21+ enchantment library with a native-registry core, Folia-safe runtime, and a small public API for registering custom enchantments, applying/removing them on items, and dispatching effects.
>
> **Deliverables**:
> - Core registry + item enchant APIs
> - Paper 1.21+ native bridge
> - FoliaLib-backed thread safety
> - Effect dispatch with both typed callbacks and an event bus
> - Built-in scaling helpers plus custom scaling hooks
> - Optional PacketEvents adapter boundary (isolated, disabled by default)
> - Tests, docs, and release hardening
>
> **Estimated Effort**: Large
> **Parallel Execution**: YES - 3 waves + final verification
> **Critical Path**: Scaffold -> registry/contracts -> native bridge -> item mutation -> effects -> tests

---

## Context

### Original Request
Create a new `artificial enchantments` API for Paper/Folia that uses FoliaLib and Item-NBT-API, centralizes custom plugin-level enchantments, supports standardized item add/remove operations, effect multipliers, event injection, and attribute changes, without rolling custom NMS.

### Interview Summary
**Key Discussions**:
- Paper 1.21+ native custom enchantments are useful; they are the core runtime, not just a registry toy.
- PacketEvents can help with advanced visuals, but it is risky; keep it optional and isolated.
- Developer ergonomics are a top priority, so the API should expose both typed callbacks and an event bus, with one shared dispatch spine.
- The library should be native-first: Paper data components are the source of truth; NBT is only for auxiliary metadata/markers, not duplicate enchant state.
- Tests are after implementation.

**Research Findings**:
- FoliaLib provides region-safe scheduling and ownership checks.
- Item-NBT-API is good for safe item metadata mutation and compound storage.
- Paper 1.21+ registry integration gives real client-visible enchant behavior.
- PacketEvents can rewrite item packets, but it adds edge cases and should not be core.

### Metis Review
**Identified Gaps** (addressed):
- Clarified that the native registry needs a source-of-truth role to avoid data drift.
- Locked down event dispatch so typed callbacks and event bus share one runtime path.
- Explicitly excluded legacy support and runtime NMS.
- Deferred packet-level visuals to an optional adapter boundary.

---

## Work Objectives

### Core Objective
Deliver a clean, high-reliability Paper 1.21+ enchantment library that plugin developers can use without touching NMS or thread-safety internals.

### Concrete Deliverables
- Public API for enchant definitions, registry access, scaling, and effect contexts.
- Paper runtime that registers custom enchantments natively and mutates items safely.
- Folia-safe scheduling adapter hidden behind the API.
- Optional PacketEvents adapter boundary (disabled by default).
- Documentation and test coverage for registry, item mutation, and effect dispatch.

### Definition of Done
- [ ] `./gradlew build` passes
- [ ] `./gradlew test` passes
- [ ] Native enchant registration works on Paper 1.21+
- [ ] Item add/remove/query APIs work without duplicate state drift
- [ ] Effect dispatch works with both typed callbacks and the bus

### Must Have
- Namespaced custom enchant registry.
- Add/remove/list enchantments on items.
- Native-first state model.
- Folia-safe runtime.
- Built-in scaling helpers plus consumer-defined scaling.

### Must NOT Have (Guardrails)
- No custom NMS.
- No legacy Paper/Spigot support in the initial release.
- No hot runtime native-enchant unregister as a primary workflow.
- No hard dependency on PacketEvents.
- No command/admin UI, economy, permissions, or resource-pack work.

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** - verification is agent-executed only.

### Test Decision
- **Infrastructure exists**: NO
- **Automated tests**: Tests after
- **Framework**: JUnit 5 + Paper/Folia integration harness
- **QA policy**: Every task includes an agent-run happy path and failure/edge scenario with saved evidence.

### QA Policy
- Unit tests cover registry, scaling, validation, and pure helpers.
- Integration tests cover Paper native registration, item mutation, and effect dispatch.
- Folia safety checks must be exercised in-region and off-region.
- Evidence goes under `.sisyphus/evidence/`.

---

## Execution Strategy

### Parallel Execution Waves

**Wave 1 — Foundation**: build system, API contracts, registry model, storage model, Folia adapter.

**Wave 2 — Core Runtime**: native Paper bridge, item mutation service, typed callback path, event bus, scaling/attribute helpers.

**Wave 3 — Extensions & Quality**: optional PacketEvents boundary, display policy, docs, tests, perf hardening.

### Dependency Rules
- Public API types should land before runtime wiring.
- Native Paper bridge depends on the registry/item model being defined.
- Typed callbacks and event bus must share one dispatcher; do not implement them as separate pipelines.
- PacketEvents module depends only on the core API and the display abstraction.

---

## TODOs

### Wave 1 — Foundation

- [x] 1. Project scaffold and build wiring

  **What to do**:
  - Create the library module structure and Gradle setup for a Paper 1.21+ API library.
  - Add dependencies for Paper API, FoliaLib, Item-NBT-API, test libraries, and optional PacketEvents boundary support.
  - Configure plugin metadata for Folia support and startup loading requirements.

  **Must NOT do**:
  - Do not add legacy Paper/Spigot support.
  - Do not add commands, config files, or demo content.

  **Recommended Agent Profile**:
  > Build-system and library scaffolding.
  - **Category**: `quick`
    - Reason: small but foundational wiring work.
  - **Skills**: `[]`
    - No special domain skill required.

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: all remaining tasks
  - **Blocked By**: None

  **References**:
  - `folialib-research/build.gradle.kts` - dependency and relocation patterns to mirror.
  - `folialib-research/README.md` - Folia support and usage expectations.

  **Acceptance Criteria**:
  - [ ] Project builds cleanly with dependency resolution.
  - [ ] Plugin metadata marks Folia compatibility.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Build resolves dependencies
    Tool: Bash
    Preconditions: project scaffold exists
    Steps:
      1. Run the build command
      2. Confirm dependency resolution completes without error
    Expected Result: build succeeds
    Evidence: .sisyphus/evidence/task-1-build.log

  Scenario: Metadata includes Folia support
    Tool: Bash
    Preconditions: plugin metadata file exists
    Steps:
      1. Inspect plugin metadata
      2. Confirm folia support/load-startup flags are present
    Expected Result: metadata contains expected flags
    Evidence: .sisyphus/evidence/task-1-metadata.txt
  ```

- [x] 2. Public API contracts

  **What to do**:
  - Define the core public API surface for registry, enchant definitions, contexts, and item mutation entry points.
  - Add the effect contract hierarchy for typed callbacks plus event bus integration.
  - Define scaling interfaces and built-in formulas.

  **Must NOT do**:
  - Do not expose internal runtime classes.
  - Do not implement behavior here; keep this as contracts only.

  **Recommended Agent Profile**:
  > API design and type modeling.
  - **Category**: `unspecified-high`
    - Reason: this is foundational library API design.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: runtime, tests
  - **Blocked By**: Task 1

  **References**:
  - `IMPLEMENTATION PLAN` section above - source of truth for scope.
  - `folialib-research/common/src/main/java/com/tcoded/folialib/FoliaLib.java` - scheduler API shape to hide behind the library.

  **Acceptance Criteria**:
  - [ ] Public API compiles without runtime implementations.
  - [ ] API names are stable, namespaced, and documented.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: API compiles as contracts only
    Tool: Bash
    Preconditions: scaffold exists
    Steps:
      1. Run compilation
      2. Confirm public API classes are present
    Expected Result: compile passes
    Evidence: .sisyphus/evidence/task-2-compile.log

  Scenario: Internal classes remain hidden
    Tool: Bash
    Preconditions: code exists
    Steps:
      1. Search public package exports
      2. Confirm internal runtime packages are not exported
    Expected Result: only intended API surfaces are public
    Evidence: .sisyphus/evidence/task-2-api-scan.txt
  ```

- [x] 3. Registry and storage model

  **What to do**:
  - Implement the enchant registry model and validation rules.
  - Define the item storage abstraction with native-first state semantics.
  - Add auxiliary Item-NBT-API metadata handling for compatibility markers and display support.

  **Must NOT do**:
  - Do not make NBT the source of truth when native components are available.
  - Do not add legacy-server storage paths.

  **Recommended Agent Profile**:
  > Core model and persistence boundaries.
  - **Category**: `deep`
    - Reason: this task defines state ownership and drift avoidance.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: native bridge, effect runtime
  - **Blocked By**: Tasks 1-2

  **References**:
  - `folialib-research/common/src/main/java/com/tcoded/folialib/type/Ref.java` - safe state wrapper patterns.
  - Context summary: Item-NBT-API compound list and native-first storage policy.

  **Acceptance Criteria**:
  - [ ] Registry validates namespaced keys and level bounds.
  - [ ] Item state can be read and written through a single abstraction.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Register and retrieve enchant definition
    Tool: Bash
    Preconditions: registry initialized
    Steps:
      1. Register a test enchant
      2. Retrieve it by key
    Expected Result: retrieved definition matches registered data
    Evidence: .sisyphus/evidence/task-3-registry.log

  Scenario: Native-first item state roundtrip
    Tool: Bash
    Preconditions: native components available
    Steps:
      1. Apply enchant to an item
      2. Read it back through storage API
    Expected Result: level and metadata match without drift
    Evidence: .sisyphus/evidence/task-3-roundtrip.log
  ```

- [x] 4. Folia safety layer

  **What to do**:
  - Wrap FoliaLib scheduling and region ownership checks behind internal helpers.
  - Add safe execution helpers for entity and location operations.
  - Ensure consumers never interact with FoliaLib directly.

  **Must NOT do**:
  - Do not expose scheduler types in the public API.
  - Do not assume main-thread execution.

  **Recommended Agent Profile**:
  > Threading boundary and safety helpers.
  - **Category**: `unspecified-high`
    - Reason: region safety is critical but constrained.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: runtime event handlers and item mutation
  - **Blocked By**: Tasks 1-3

  **References**:
  - `folialib-research/platform/paper/src/main/java/com/tcoded/folialib/impl/PaperImplementation.java` - platform-specific implementation shape.
  - Research lines 17-31 - region ownership checks and runAtEntity/runAtLocation behavior.

  **Acceptance Criteria**:
  - [ ] Async and region-safe entry points are separated internally.
  - [ ] Unsafe calls are rejected or rescheduled consistently.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Region-owned operation executes safely
    Tool: Bash
    Preconditions: Folia-capable test environment
    Steps:
      1. Trigger an entity-scoped operation from the correct region
      2. Confirm it executes without rescheduling loops
    Expected Result: operation succeeds on the owned region
    Evidence: .sisyphus/evidence/task-4-owned.log

  Scenario: Off-region operation is redirected
    Tool: Bash
    Preconditions: Folia-capable test environment
    Steps:
      1. Trigger an entity-scoped operation from the wrong thread/region
      2. Confirm it is rescheduled or rejected safely
    Expected Result: no unsafe world access occurs
    Evidence: .sisyphus/evidence/task-4-reschedule.log
  ```

---

### Wave 2 — Core Runtime

- [ ] 5. Native Paper bridge

  **What to do**:
  - Implement the Paper 1.21+ bootstrap path and registry bridge.
  - Convert library enchant definitions into native Paper enchant registrations.
  - Wire native display and behavior hooks through the core abstraction.

  **Must NOT do**:
  - Do not couple the core API to Paper implementation types.
  - Do not add legacy server branches here.

  **Recommended Agent Profile**:
  > Paper registry integration.
  - **Category**: `deep`
    - Reason: high-impact platform bridge.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: display-specific adapters, tests
  - **Blocked By**: Tasks 1-4

  **References**:
  - Context summary: Paper registry integration and related details.
  - `folialib-research/platform/paper/src/main/java/com/tcoded/folialib/impl/PaperImplementation.java` - platform detection pattern.

  **Acceptance Criteria**:
  - [ ] Custom enchant registers on Paper 1.21+.
  - [ ] Native client-visible behavior is preserved.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Native registration succeeds
    Tool: Bash
    Preconditions: Paper 1.21+ test server
    Steps:
      1. Start the plugin
      2. Register a custom enchant
      3. Query the registry bridge
    Expected Result: enchant is present in native registry
    Evidence: .sisyphus/evidence/task-5-native-register.log

  Scenario: Native behavior remains visible
    Tool: Bash
    Preconditions: Paper 1.21+ test server
    Steps:
      1. Apply the enchant to an item
      2. Inspect client-visible representation
    Expected Result: native enchant display is present without lore hacks
    Evidence: .sisyphus/evidence/task-5-native-display.log
  ```

- [ ] 6. Item mutation service

  **What to do**:
  - Implement add/remove/query methods for enchantments on items.
  - Make native data components the source of truth where available.
  - Use Item-NBT-API only for auxiliary metadata and compatibility markers.

  **Must NOT do**:
  - Do not make lore the canonical state.
  - Do not duplicate enchant state across multiple stores without a clear reason.

  **Recommended Agent Profile**:
  > Item state and serialization.
  - **Category**: `deep`
    - Reason: state drift avoidance is central.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: effect dispatch and display policy
  - **Blocked By**: Tasks 3-5

  **References**:
  - Context summary: NBT compound-list pattern and safe meta update pattern.
  - Context summary: native-first storage policy.

  **Acceptance Criteria**:
  - [ ] Add/remove/query operations are deterministic.
  - [ ] Item reads do not drift between native and auxiliary stores.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Add and remove enchant from item
    Tool: Bash
    Preconditions: test item exists
    Steps:
      1. Apply an enchant to the item
      2. Read enchant data back
      3. Remove the enchant
      4. Read back again
    Expected Result: state transitions are correct and stable
    Evidence: .sisyphus/evidence/task-6-roundtrip.log

  Scenario: Auxiliary metadata does not drift state
    Tool: Bash
    Preconditions: item with auxiliary metadata exists
    Steps:
      1. Modify auxiliary metadata
      2. Re-read item state
    Expected Result: enchant levels remain unchanged
    Evidence: .sisyphus/evidence/task-6-meta.log
  ```

- [ ] 7. Effect runtime and dispatch spine

  **What to do**:
  - Implement one dispatcher that powers both typed callbacks and the event bus.
  - Add event-context objects for combat, tools, utility, and attribute effects.
  - Ensure cancellation and priority rules are deterministic.

  **Must NOT do**:
  - Do not create separate execution pipelines for callbacks and the bus.
  - Do not make event ordering ambiguous.

  **Recommended Agent Profile**:
  > Runtime behavior and event orchestration.
  - **Category**: `ultrabrain`
    - Reason: this is the hardest logic in the library.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: scaling helpers, tests
  - **Blocked By**: Tasks 2-6

  **References**:
  - Context summary: supported event types and high-priority dispatch pattern.
  - Context summary: interface-based handler and scaling concept.

  **Acceptance Criteria**:
  - [ ] Typed callbacks and event bus both fire through the same core path.
  - [ ] Event cancellation rules are documented and enforced.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Typed callback dispatch fires once
    Tool: Bash
    Preconditions: enchant with typed callback exists
    Steps:
      1. Trigger the matching game event
      2. Count callback invocations
    Expected Result: callback fires exactly once
    Evidence: .sisyphus/evidence/task-7-callback.log

  Scenario: Event bus cancellation is respected
    Tool: Bash
    Preconditions: event bus subscriber exists
    Steps:
      1. Register a cancelling subscriber
      2. Trigger the event
    Expected Result: dispatch stops according to contract
    Evidence: .sisyphus/evidence/task-7-bus-cancel.log
  ```

- [ ] 8. Scaling and attribute helpers

  **What to do**:
  - Implement built-in scaling formulas and custom scaling function support.
  - Add helpers for simple attribute changes and common effect math.
  - Keep the helpers pure and reusable.

  **Must NOT do**:
  - Do not bury scaling logic inside unrelated listeners.
  - Do not make formulas stateful.

  **Recommended Agent Profile**:
  > Math/helpers and small APIs.
  - **Category**: `quick`
    - Reason: discrete utility layer.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: tests and docs only
  - **Blocked By**: Tasks 2, 7

  **References**:
  - Context summary: scaling formulas and custom function support.

  **Acceptance Criteria**:
  - [ ] Built-in formulas return expected values.
  - [ ] Custom scaling function can be injected and used.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Built-in linear scaling works
    Tool: Bash
    Preconditions: scaling helper compiled
    Steps:
      1. Evaluate a linear formula at level 3
      2. Compare output to expected value
    Expected Result: formula output is correct
    Evidence: .sisyphus/evidence/task-8-linear.log

  Scenario: Custom scaling function is honored
    Tool: Bash
    Preconditions: custom function path available
    Steps:
      1. Provide a custom lambda/function
      2. Evaluate at a test level
    Expected Result: helper returns custom result
    Evidence: .sisyphus/evidence/task-8-custom.log
  ```

---

### Wave 3 — Extensions & Quality

- [ ] 9. Optional PacketEvents adapter boundary

  **What to do**:
  - Define an isolated optional adapter layer for advanced visuals/per-player rewriting.
  - Keep it disabled by default and fully decoupled from the core runtime.
  - Use it only for display concerns, not core enchant storage.

  **Must NOT do**:
  - Do not require PacketEvents for core functionality.
  - Do not let this module alter core state semantics.

  **Recommended Agent Profile**:
  > Optional integration boundary.
  - **Category**: `unspecified-high`
    - Reason: integration code with a risky third-party dependency.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: docs for optional module only
  - **Blocked By**: Tasks 2, 6

  **References**:
  - Context summary: PacketEvents capability and risk summary.

  **Acceptance Criteria**:
  - [ ] Module compiles as optional.
  - [ ] Core library works when the module is absent.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Optional module loads when present
    Tool: Bash
    Preconditions: PacketEvents installed in test server
    Steps:
      1. Enable optional module path
      2. Open an inventory with an enchanted item
    Expected Result: adapter executes without breaking core behavior
    Evidence: .sisyphus/evidence/task-9-present.log

  Scenario: Core library works without optional module
    Tool: Bash
    Preconditions: PacketEvents absent
    Steps:
      1. Start the core library only
      2. Apply and read enchant data
    Expected Result: no dependency failure occurs
    Evidence: .sisyphus/evidence/task-9-absent.log
  ```

- [ ] 10. Documentation and public ergonomics

  **What to do**:
  - Write JavaDoc for all public API types.
  - Document the native-first storage policy and dispatch rules.
  - Provide minimal usage examples for registration, application, and effect handling.

  **Must NOT do**:
  - Do not over-document internals.
  - Do not add tutorial-style filler.

  **Recommended Agent Profile**:
  > Technical writing and API clarity.
  - **Category**: `writing`
    - Reason: public API docs and examples.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: release only
  - **Blocked By**: Tasks 2-8

  **References**:
  - All public API classes from Tasks 2, 7, and 8.

  **Acceptance Criteria**:
  - [ ] Public API has JavaDoc.
  - [ ] README explains native-first behavior and optional module boundaries.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: README documents the main workflow
    Tool: Bash
    Preconditions: documentation written
    Steps:
      1. Read the usage section
      2. Confirm registration and application are explained
    Expected Result: docs are complete and aligned with API
    Evidence: .sisyphus/evidence/task-10-readme.log

  Scenario: Public API JavaDoc exists
    Tool: Bash
    Preconditions: source files exist
    Steps:
      1. Scan public classes
      2. Confirm JavaDoc presence
    Expected Result: public API is documented
    Evidence: .sisyphus/evidence/task-10-javadoc.log
  ```

- [ ] 11. Tests and hardening

  **What to do**:
  - Add unit and integration tests for registry, scaling, item mutation, and effect dispatch.
  - Add failure cases for invalid keys, bounds, and thread-safety violations.
  - Cover the no-module path and optional-module path.

  **Must NOT do**:
  - Do not add tests that depend on manual verification.
  - Do not leave thread-safety or drift behavior untested.

  **Recommended Agent Profile**:
  > Verification and hardening.
  - **Category**: `deep`
    - Reason: the release gate for this library.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: final verification
  - **Blocked By**: Tasks 1-10

  **References**:
  - All runtime tasks above.

  **Acceptance Criteria**:
  - [ ] `./gradlew test` passes.
  - [ ] Edge cases are covered for invalid input and thread safety.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Test suite passes
    Tool: Bash
    Preconditions: tests written
    Steps:
      1. Run the test task
      2. Confirm zero failures
    Expected Result: suite passes
    Evidence: .sisyphus/evidence/task-11-tests.log

  Scenario: Failure cases are covered
    Tool: Bash
    Preconditions: tests written
    Steps:
      1. Run invalid-input tests
      2. Run off-thread safety tests
    Expected Result: failures are handled predictably
    Evidence: .sisyphus/evidence/task-11-edgecases.log
  ```

---

## Final Verification Wave

- [ ] F1. Plan compliance audit

  **What to do**:
  - Verify every must-have is implemented.
  - Verify every must-not-have is absent.
  - Verify evidence exists for each task’s QA scenarios.

  **Acceptance Criteria**:
  - [ ] Must-have list is complete.
  - [ ] No scope violations are present.

- [ ] F2. Code quality review

  **What to do**:
  - Run build/test/lint and inspect changed files for slop patterns.

  **Acceptance Criteria**:
  - [ ] Build and tests pass.
  - [ ] No obvious code-quality regressions.

- [ ] F3. End-to-end QA

  **What to do**:
  - Execute the scenarios from all tasks in an integrated environment.

  **Acceptance Criteria**:
  - [ ] Scenarios pass in combination.

- [ ] F4. Scope fidelity check

  **What to do**:
  - Confirm implementation matches the locked scope and nothing more.

  **Acceptance Criteria**:
  - [ ] No contamination or unaccounted changes.

---

## Commit Strategy

- Commit once per wave after the wave’s verification passes.
- Use small, descriptive messages that match the delivered surface.

---

## Success Criteria

### Verification Commands
```bash
./gradlew build
./gradlew test
```

### Final Checklist
- [ ] Registry API is clean and namespaced
- [ ] Item mutation has one source of truth
- [ ] Folia-safe scheduling is hidden from consumers
- [ ] Paper 1.21+ native bridge works
- [ ] PacketEvents is optional and isolated
- [ ] Tests pass
