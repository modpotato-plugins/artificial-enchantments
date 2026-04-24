# Draft: Feature Saturation / Pre-1.0 Expansion

## Requirements (confirmed)
- Current focus is **feature saturation and completeness**.
- **Stabilization and code cleanup are later-phase work**, closer to 1.0.0.
- Improve the **builder API** for custom enchantments.
- Improve **listener ergonomics** so checking enchantments on an item is easy and local.
- Add/define **enchantment table parity** behavior.
- Add/define **enchanted books + anvil behavior**.
- Define **loot table / loot injection ownership** and developer-facing API.

## Technical Decisions
- Builder improvements are scoped to ergonomics, validation, and failure policy; no DSL rewrite.
- Item enchant checks will be offered through a scoped facade, not `ItemStack` monkey-patching.
- Scaling customizations will favor named/registered algorithms with lambda-friendly inputs where appropriate.
- Vanilla parity is limited to hook-based table/book/anvil flows; no custom GUI rewrite.
- Loot hooks are explicit and opt-in, with block-break support as the anchor.

## Research Findings
- Builder and scaling foundations already exist, so expansion can stay additive.
- Event dispatch is already unified internally, but bus coverage is incomplete for some event types.
- Item storage already exposes has/get semantics, so ergonomics can be layered on top.
- Enchant table, books/anvils, and broader loot hooks are the main missing feature surfaces.

## Open Questions
- Builder API: should custom scaling support **lambda-based algorithms**, and should callbacks be wrapped in a **safe try/catch** layer?
- Item checks: should we expose **central service methods only**, or also a **local convenience style** like `item.hasArtificialEnchantment(...)`?
- Vanilla parity: how strict should parity be for **treasure enchants, restrictions, and level rules**?
- Books/anvils: should **enchanted books** be first-class, and should anvil merges/combinations follow vanilla-like rules?
- Loot tables: should loot injection be **automatic by default** or **explicitly opt-in** for developers?

## Scope Boundaries
- INCLUDE: new feature surfaces, ergonomics, parity systems, and integration ownership.
- EXCLUDE: stabilization-only cleanup, refactors with no user-facing gain, and 1.0.0 hardening work.
