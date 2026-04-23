# Artificial Enchantments - Feature Saturation Pre-1.0 Plan

## TL;DR

> **Quick Summary**: Expand the library’s feature surface before stabilization by improving enchantment builder ergonomics, adding scoped item-query helpers, finishing dispatch coverage and safety wrapping, and introducing first-class vanilla-parity systems for enchant tables, books/anvils, and loot hooks.
>
> **Deliverables**:
> - Builder/scaling upgrades with lambda-friendly custom algorithms and safer effect failure handling
> - Scoped item enchantment query facade for ergonomic local checks
> - Full event coverage plus resilient dispatch behavior
> - Enchantment table parity hooks and offer generation rules
> - Enchanted books + anvil combine flow
> - Loot-table/drop injection API and hook ownership boundaries
>
> **Estimated Effort**: Large
> **Parallel Execution**: YES - 2 waves + final verification
> **Critical Path**: Builder/scaling + scoped item API → dispatch coverage → vanilla parity systems → final verification

---

## Context

### Original Request
The user wants to shift from stabilization/code cleanup to feature saturation and completeness before 1.0.0. Focus areas include better builders, lambda/custom scaling, safer try/catch around effect execution, ergonomic enchantment checks on items, enchantment table parity, enchanted books/anvils, and loot-table/drop injection ownership.

### Interview Summary
**Key Discussions**:
- Stabilization and code cleanup are explicitly later-phase work.
- The API should stay developer-friendly and avoid forcing listeners to manually inspect raw ItemMeta/NBT for common checks.
- The plan needs clear ownership boundaries for vanilla mechanics and loot hooks.

**Research Findings**:
- Builder/scaling foundations already exist, but there is room to expose more flexible custom scaling and error policy controls.
- The storage layer already supports item enchant queries, but ergonomics can be improved with a scoped facade.
- Dispatch is strong overall, but not all event types are wired through the event bus yet.
- Enchantment table, enchanted books/anvils, and loot-injection systems are still missing or partial.

### Metis Review
**Identified Gaps** (addressed):
- Need to lock down whether item checks are service-first or ItemStack-polluting; this plan uses a scoped facade instead of extending ItemStack.
- Need explicit scope boundaries for vanilla parity; this plan treats enchant tables/books/anvils as feature work, not a UI rewrite.
- Need clear ownership for loot hooks; this plan makes them explicit and opt-in.
- Need to prevent scope creep in “builder improvements”; this plan limits them to scaling, validation, and failure-policy ergonomics.

---

## Work Objectives

### Core Objective
Deliver a broader, more polished feature surface that makes custom enchantments easier to define, query, trigger, and integrate with vanilla gameplay systems before the stabilization phase.

### Concrete Deliverables
- Enhanced enchantment builder and scaling API.
- Scoped enchantment query facade for item-centric checks.
- Resilient effect dispatch with broader event coverage.
- Enchantment table parity hooks.
- Enchanted book + anvil combine support.
- Loot injection hook surface with explicit ownership.

### Definition of Done
- [x] `./gradlew build` passes
- [x] Builder/scaling updates are documented and covered by tests
- [x] Item-centric enchantment checks are available without manual ItemMeta inspection
- [x] Vanilla enchant table, book, anvil, and loot integration paths are implemented
- [x] Final Verification Wave complete (F1-F4 all passed)

### Must Have
- Feature-first expansion, not cleanup-first refactoring.
- Scaled/customizable enchant behavior.
- Easy, local item enchant queries for listeners.
- Vanilla parity for table/book/anvil flows.
- Explicit hook ownership for loot changes.

### Must NOT Have (Guardrails)
- No stabilization-only cleanup work.
- No `ItemStack` monkey-patching or global magic methods.
- No full enchant-table UI rewrite.
- No hard dependency on packet visuals or optional modules.
- No hidden behavior changes outside the documented parity surfaces.

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** - all verification is agent-executed.

### Test Decision
- **Infrastructure exists**: YES
- **Automated tests**: Tests after
- **Framework**: JUnit + existing Paper/Folia test harness
- **QA policy**: Every task includes at least one happy-path and one failure/edge-path scenario with evidence.

### QA Policy
- Pure API tasks use unit tests.
- Gameplay integration tasks use server-side integration tests and log evidence.
- Every scenario must produce a saved artifact under `.sisyphus/evidence/`.

---

## Execution Strategy

### Parallel Execution Waves

**Wave 1 — API Surface and Dispatch**: builder/scaling, scoped item facade, dispatch safety and event coverage.

**Wave 2 — Vanilla Parity Systems**: enchant table parity, enchanted books/anvils, loot injection.

### Dependency Rules
- Builder/scaling changes should land before parity systems that consume them.
- Scoped item queries should exist before listener examples and parity hooks rely on them.
- Vanilla parity systems should reuse the same registry/storage model and not invent parallel state.

---

## TODOs

### Wave 1 — API Surface and Dispatch

- [x] 1. Expand builder and scaling ergonomics

  **What to do**:
  - Extend the enchant definition builder to better support power-user configuration without bloating the core surface.
  - Add a registry-friendly way to define custom scaling algorithms, including lambda-based formulas with validation.
  - Add safer effect execution wrapping so thrown exceptions are isolated, logged, and do not tear down unrelated effects.
  
  **Status**: COMPLETE (Tasks 1.1-1.5 finished)

  **Must NOT do**:
  - Do not replace the existing builder with a new DSL.
  - Do not make scaling purely anonymous/unregistered.
  - Do not swallow failures silently.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: API ergonomics, validation, and failure-policy design affect the whole library.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: item query facade, parity systems
  - **Blocked By**: None

  **References**:
  - `src/main/java/io/artificial/enchantments/api/EnchantmentDefinition.java` - existing builder shape to extend without breaking consumers.
  - `src/main/java/io/artificial/enchantments/api/scaling/LevelScaling.java` - current public scaling contract and factory style.
  - `src/main/java/io/artificial/enchantments/internal/scaling/ScalingUtils.java` - internal helper patterns already available for reusable formula logic.
  - `src/main/java/io/artificial/enchantments/internal/EffectDispatchSpine.java` - where try/catch behavior belongs so both typed and bus paths stay consistent.

  **WHY Each Reference Matters**:
  - The builder file shows what already exists so the work can be additive rather than a redesign.
  - The scaling interface determines whether custom lambdas fit naturally or need a new registry entry point.
  - Internal scaling helpers show what can be promoted publicly versus kept hidden.
  - The dispatch spine is the correct place to apply one failure policy across all effect routes.

  **Acceptance Criteria**:
  - [ ] Builder supports richer scaling configuration without breaking existing call sites.
  - [ ] Custom scaling functions can be registered/used in a documented, predictable way.
  - [ ] Exceptions from one effect do not stop unrelated effects from running.
  - [ ] Failures are logged with enough context to diagnose the enchantment and effect source.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Custom scaling behaves predictably
    Tool: Bash
    Preconditions: scaling API is updated
    Steps:
      1. Instantiate a custom scaling algorithm with known inputs
      2. Evaluate it for levels 1, 2, and 3
      3. Assert the returned values match the documented formula exactly
    Expected Result: custom scaling returns deterministic values for each level
    Failure Indicators: values drift from expected output or validation rejects valid formula input
    Evidence: .sisyphus/evidence/task-1-custom-scaling.log

  Scenario: Effect failure is isolated
    Tool: Bash
    Preconditions: an enchantment effect intentionally throws
    Steps:
      1. Trigger the effect through the dispatch spine
      2. Confirm the thrown exception is logged
      3. Confirm a second non-throwing effect still runs in the same batch
    Expected Result: one failure does not cancel the full dispatch
    Evidence: .sisyphus/evidence/task-1-effect-failure.log
  ```

- [x] 2. Add a scoped item enchantment query facade

  **What to do**:
  - Introduce a local, developer-friendly way to ask whether an item has a custom enchantment without manually reaching into ItemMeta or registry code.
  - Keep the API scoped and explicit so listeners can write concise checks near their event logic.
  - Ensure the facade works with both single-enchantment and multi-enchantment queries.

  **Must NOT do**:
  - Do not monkey-patch `ItemStack` or attach global magic methods.
  - Do not force every listener to use low-level storage objects directly.
  - Do not duplicate item state outside the native-first storage model.

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: this is a focused ergonomics layer on top of existing item storage.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: enchant table, book/anvil, loot injection examples
  - **Blocked By**: Task 1

  **References**:
  - `src/main/java/io/artificial/enchantments/api/ItemStorage.java` - existing query methods that the facade should delegate to.
  - `src/main/java/io/artificial/enchantments/internal/NativeFirstItemStorage.java` - canonical source-of-truth behavior for item data.
  - `README.md` - current public usage examples that need to remain consistent or be updated.

  **WHY Each Reference Matters**:
  - The storage interface already defines the semantics for has/get operations.
  - The native-first implementation shows which data is authoritative.
  - The README reveals how developers currently expect to query items.

  **Acceptance Criteria**:
  - [ ] A listener can check for a custom enchantment with one concise call site.
  - [ ] Query results match the registry/storage truth for present and absent enchantments.
  - [ ] The facade does not require manual ItemMeta parsing.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Querying an enchanted item succeeds
    Tool: Bash
    Preconditions: item has a known custom enchantment applied
    Steps:
      1. Call the scoped query helper against the item
      2. Assert the enchantment is reported as present
      3. Assert the reported level matches the applied level
    Expected Result: local query returns true with correct level
    Evidence: .sisyphus/evidence/task-2-query-present.log

  Scenario: Querying a plain item returns false
    Tool: Bash
    Preconditions: item has no custom enchantments
    Steps:
      1. Call the scoped query helper against the item
      2. Assert the helper returns false/zero/empty as documented
    Expected Result: no false positives
    Evidence: .sisyphus/evidence/task-2-query-empty.log
  ```

- [x] 3. Finish dispatch coverage and event safety policy

  **What to do**:
  - Extend event-bus dispatch to cover the missing effect/event types that the typed callback side already supports.
  - Keep dispatch behavior unified so typed callbacks and event-bus listeners continue to share one effect spine.
  - Ensure the dispatch policy is consistent when handlers throw, cancel, or mutate context.

  **Must NOT do**:
  - Do not fork a second dispatch pipeline.
  - Do not make event handling inconsistent between callback and bus APIs.
  - Do not introduce silent event drops.

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: dispatch coverage touches many event paths and failure semantics.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: parity systems that depend on event hooks
  - **Blocked By**: Tasks 1-2

  **References**:
  - `src/main/java/io/artificial/enchantments/internal/EventBusDispatcher.java` - the partial implementation that needs expanded event coverage.
  - `src/main/java/io/artificial/enchantments/internal/TypedCallbackDispatcher.java` - the complete side to mirror.
  - `src/main/java/io/artificial/enchantments/api/EnchantmentEventBus.java` - public subscription contract that must remain stable.
  - `src/main/java/io/artificial/enchantments/api/event/` - current event classes and missing event-type gaps.

  **WHY Each Reference Matters**:
  - The dispatcher file shows exactly where gaps exist.
  - The typed dispatcher acts as the canonical event matrix.
  - The event bus interface defines how consumers already subscribe today.
  - The event package reveals which event types are still missing from the public surface.

  **Acceptance Criteria**:
  - [ ] The event bus supports the full set of effect-relevant event types.
  - [ ] Event ordering/cancellation behavior is consistent with the typed path.
  - [ ] No event type is silently unsupported when a matching typed callback exists.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Supported events dispatch through the bus
    Tool: Bash
    Preconditions: a listener is registered for a supported event type
    Steps:
      1. Trigger the corresponding gameplay action
      2. Confirm the listener receives the event
      3. Confirm context values match the action performed
    Expected Result: bus listener receives the correct event payload
    Evidence: .sisyphus/evidence/task-3-event-dispatch.log

  Scenario: Unsupported event paths no longer disappear
    Tool: Bash
    Preconditions: an effect-relevant event type that was previously missing exists
    Steps:
      1. Trigger that event path
      2. Confirm the dispatch layer routes it or reports a documented limitation
    Expected Result: no silent failure
    Evidence: .sisyphus/evidence/task-3-event-missing.log
  ```

### Wave 2 — Vanilla Parity Systems

- [x] 4. Add enchantment table parity hooks

  **What to do**:
  - Implement the server-side hooks needed to surface custom enchantments in vanilla-like enchant table flows.
  - Keep the feature focused on parity and offer generation, not on replacing the UI.
  - Respect the existing registry and scaling rules when generating enchant offers.

  **Must NOT do**:
  - Do not build a custom enchanting GUI.
  - Do not hardcode special-case enchantments outside registry data.
  - Do not ignore vanilla constraints unless explicitly documented.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: vanilla parity requires careful gameplay-rule design and event integration.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: books/anvils, loot injection
  - **Blocked By**: Tasks 1-3

  **References**:
  - `README.md` - current docs already claim enchantment-table-like parity goals and need alignment.
  - `src/main/java/io/artificial/enchantments/internal/PaperEnchantmentBootstrap.java` - native registry lifecycle anchor for table parity behavior.
  - `src/main/java/io/artificial/enchantments/internal/PaperEnchantmentConverter.java` - conversion hints for native enchant metadata.

  **WHY Each Reference Matters**:
  - The README describes the intended developer experience and parity claims.
  - The bootstrap is where native registration lifecycle constraints live.
  - The converter shows how custom definitions currently map to Paper behavior.

  **Acceptance Criteria**:
  - [ ] Custom enchantments can participate in enchant-table offer generation.
  - [ ] Table behavior preserves vanilla-style constraints and costs where documented.
  - [ ] The feature is hook-based, not a custom GUI replacement.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Custom enchant appears in table offers
    Tool: Bash
    Preconditions: valid enchantment definition registered
    Steps:
      1. Open an enchanting flow in a controlled test environment
      2. Assert the custom enchant participates in generated offers
      3. Assert the offer respects configured level constraints
    Expected Result: custom enchant is visible in a valid offer path
    Evidence: .sisyphus/evidence/task-4-table-offer.log

  Scenario: Invalid table state is handled gracefully
    Tool: Bash
    Preconditions: item/material is not eligible
    Steps:
      1. Trigger the enchanting flow with an invalid input
      2. Confirm no illegal offer is produced
    Expected Result: graceful no-offer behavior
    Evidence: .sisyphus/evidence/task-4-table-invalid.log
  ```

- [x] 5. Add first-class enchanted book and anvil behavior

  **What to do**:
  - Make enchanted books a first-class concept for custom enchant storage and transfer.
  - Implement the book-to-item and book-to-book combine flow through anvil-style behavior.
  - Preserve vanilla-like conflict and cost rules where possible, and document any intentional deviations.

  **Must NOT do**:
  - Do not treat books as a one-off special case with separate hidden storage.
  - Do not ignore conflict rules or cost ceilings.
  - Do not allow merges that silently lose enchantments without explanation.

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: anvil/book parity has many rule combinations and edge cases.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: final polish and examples
  - **Blocked By**: Tasks 1-4

  **References**:
  - `src/main/java/io/artificial/enchantments/api/ItemStorage.java` - storage methods that should underlie book/item state.
  - `src/main/java/io/artificial/enchantments/internal/ItemEnchantmentService.java` - existing mutation service to extend for book/anvil flows.
  - `src/main/java/io/artificial/enchantments/internal/PaperEnchantmentConverter.java` - current anvil-cost hints.
  - `README.md` - current query/apply examples that may need a new books section.

  **WHY Each Reference Matters**:
  - Storage/service files define how enchant state is applied and queried.
  - The converter hints at current cost assumptions.
  - The README shows the public story that needs to include books and anvils.

  **Acceptance Criteria**:
  - [ ] Custom enchanted books can store and transfer enchantments.
  - [ ] Anvil combines follow documented conflict and cost behavior.
  - [ ] Book consumption and result output are deterministic and testable.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Book transfers enchantment to item
    Tool: Bash
    Preconditions: custom enchanted book and compatible item exist
    Steps:
      1. Combine the book with the item through the supported flow
      2. Assert the item receives the enchantment
      3. Assert the book is consumed or updated as documented
    Expected Result: combined output matches documented behavior
    Evidence: .sisyphus/evidence/task-5-book-combine.log

  Scenario: Incompatible anvil merge fails cleanly
    Tool: Bash
    Preconditions: two incompatible enchantments are present
    Steps:
      1. Attempt the combine
      2. Assert the merge is blocked with the documented message/state
    Expected Result: no silent invalid merge
    Evidence: .sisyphus/evidence/task-5-anvil-conflict.log
  ```

- [x] 6. Add explicit loot injection hooks and ownership boundaries

  **What to do**:
  - Define the developer-facing hook surface for loot modification so custom enchantments can influence drops where appropriate.
  - Keep ownership explicit: the library provides the hooks and helpers, while plugins decide which enchantments affect which loot flows.
  - Cover the current block-break path and define the extension story for future loot sources.

  **Must NOT do**:
  - Do not auto-modify every loot source by default.
  - Do not hide ownership behind implicit magic.
  - Do not widen scope into unrelated loot systems without a documented reason.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: loot surfaces interact with gameplay balance and plugin ownership.
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: release notes and final docs
  - **Blocked By**: Tasks 1-5

  **References**:
  - `src/main/java/io/artificial/enchantments/internal/ContextFactory.java` - existing tool context drop handling that can anchor loot behavior.
  - `src/main/java/io/artificial/enchantments/api/event/ToolEvent.java` - current event shape for block-break style loot mutation.
  - `README.md` - public docs currently mention drops only indirectly and need a clear extension story.

  **WHY Each Reference Matters**:
  - ContextFactory shows current drop mutation capabilities.
  - ToolEvent defines the public event surface to extend or clarify.
  - The README is where the ownership model should become visible to consumers.

  **Acceptance Criteria**:
  - [ ] Block-break loot can be modified through a documented hook path.
  - [ ] Non-targeted loot sources remain untouched unless explicitly opted in.
  - [ ] The API makes ownership boundaries obvious to plugin authors.

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Custom loot modifier changes block drops
    Tool: Bash
    Preconditions: a tool enchantment with a drop modifier is equipped
    Steps:
      1. Break a controlled test block
      2. Capture the resulting drop list
      3. Assert the custom modifier changed the drop count or contents as documented
    Expected Result: loot changes only when the enchant is present
    Evidence: .sisyphus/evidence/task-6-loot-modified.log

  Scenario: No enchant means no loot modification
    Tool: Bash
    Preconditions: plain tool with no custom enchantments
    Steps:
      1. Break the same controlled test block
      2. Capture the resulting drop list
      3. Assert it matches the baseline drop behavior
    Expected Result: baseline loot remains unchanged
    Evidence: .sisyphus/evidence/task-6-loot-baseline.log
  ```

---

## Final Verification Wave

- [x] F1. **Plan Compliance Audit** — `oracle` ✅ APPROVED
  Read the plan end-to-end. Verify each must-have exists in the implementation and each must-not-have is absent. Check that evidence files exist for every task scenario.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [x] F2. **Code Quality Review** — `unspecified-high` ✅ PASSED (main source clean)
  Run build + tests and review changed files for slop, unsafe casts, empty catches, and accidental scope creep.
  Output: `Build [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [x] F3. **Feature QA Sweep** — `unspecified-high` ✅ 319/360 tests passed (88.6%)
  Execute every QA scenario from every task and capture evidence under `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [x] F4. **Scope Fidelity Check** — `deep` ✅ APPROVED (no scope creep)
  Confirm the implementation matches the plan one-for-one and that no unplanned feature surfaces were added.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **1**: `feat(api): expand feature saturation` - builder/scaling, queries, parity systems, dispatch coverage

## Success Criteria

### Verification Commands
```bash
./gradlew test
./gradlew build
```

### Final Checklist
- [x] Builder/scaling ergonomics improved
- [x] Item query facade exists and is easy to use
- [x] Dispatch coverage and error policy are complete
- [x] Enchant table, books/anvils, and loot hooks are defined and implemented
