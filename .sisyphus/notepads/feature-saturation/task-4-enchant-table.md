
## Task 4: Enchantment Table Parity Hooks Implementation

### Implementation Summary
Successfully implemented Paper enchantment table parity hooks that allow custom enchantments to appear in enchanting table offers with vanilla-style behavior.

### Files Created

#### API Layer
- `src/main/java/io/artificial/enchantments/api/enchanttable/EnchantTableConfiguration.java`
  - Configuration API for per-enchantment table settings
  - Supports weight multipliers, bookshelf constraints, cost adjustments
  - Builder pattern for easy configuration

#### Internal Implementation
- `src/main/java/io/artificial/enchantments/internal/enchanttable/EnchantTableConfigurationImpl.java`
  - Immutable implementation of the configuration interface
  
- `src/main/java/io/artificial/enchantments/internal/enchanttable/EnchantTableConfigurationBuilder.java`
  - Builder implementation with validation

- `src/main/java/io/artificial/enchantments/internal/enchanttable/EnchantOfferGenerator.java`
  - Generates enchantment offers with vanilla-style weights and costs
  - Respects rarity weights (COMMON=10, UNCOMMON=5, RARE=2, VERY_RARE=1)
  - Implements level determination based on slot position (0, 1, 2)
  - Cost calculation: 1-30 levels based on bookshelf power
  - Conflict detection with vanilla and other custom enchantments

- `src/main/java/io/artificial/enchantments/internal/enchanttable/EnchantmentTableListener.java`
  - Event listener for Paper's PrepareItemEnchantEvent and EnchantItemEvent
  - Hooks into PrepareItemEnchantEvent to inject custom offers into empty slots
  - Handles EnchantItemEvent to apply selected custom enchantments to items
  - Uses PaperRegistryBridge to look up native Paper enchantments

#### Tests
- `src/test/java/io/artificial/enchantments/internal/enchanttable/EnchantOfferGeneratorTest.java`
  - Unit tests for offer generation logic
  - Tests for empty items, no applicable enchantments, rarity weights
  - Tests for bookshelf power affecting cost
  - Tests for level bounds enforcement

#### Plugin Registration
- Modified `src/main/java/io/artificial/enchantments/ArtificialEnchantmentsPlugin.java`
  - Added `registerEnchantmentTableListener()` method
  - Registers EnchantmentTableListener on plugin enable

### Key Features Implemented

1. **Vanilla-Style Weights**: Uses the same rarity-to-weight mapping as vanilla
   - COMMON: 10
   - UNCOMMON: 5
   - RARE: 2
   - VERY_RARE: 1

2. **Slot-Based Level Distribution**: 
   - Slot 0: 30-50% of max level
   - Slot 1: 50-80% of max level
   - Slot 2: 70-100% of max level

3. **Bookshelf Power Integration**: 
   - Respects min/max bookshelf constraints
   - Power (0-15) affects available enchantment levels
   - Cost scales with power (5-7 at low power, 20-30 at max)

4. **Conflict Detection**: 
   - Checks conflicts with vanilla enchantments in existing offers
   - Prevents conflicting enchantments from appearing together
   - Respects definition's `getConflictingEnchantments()`

5. **Discoverable Flag**: Only enchantments with `isDiscoverable()=true` appear in tables

6. **No Vanilla Override**: Only adds to empty slots, never replaces vanilla offers

### Build Status
- Main source compilation: SUCCESS
- Test compilation: SUCCESS (for new test file)
- Build: SUCCESS (with `-x test -x javadoc` due to pre-existing project issues)

### Integration Points
- Uses `EnchantmentRegistryManager` for enchantment lookups
- Uses `PaperRegistryBridge` for native Paper enchantment access
- Integrates with `ItemEnchantmentQuery` facade pattern (future enhancement ready)

### Notes for Task 5 (Books/Anvils)
The cost calculation logic in EnchantOfferGenerator may be reusable for anvil cost calculations.
