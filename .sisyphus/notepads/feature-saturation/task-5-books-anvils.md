# Task 5: Enchanted Books and Anvils - Implementation Notes

## Created Files

### API (Public Interface)
- `src/main/java/io/artificial/enchantments/api/book/EnchantedBook.java`
  - Interface for creating/querying enchanted books
  - Builder pattern for book creation
  - Native-first storage following project conventions

### Internal Implementation
- `src/main/java/io/artificial/enchantments/internal/book/EnchantedBookImpl.java`
  - Implementation using ItemStorage
  - Delegates to NativeFirstItemStorage for enchantment handling

- `src/main/java/io/artificial/enchantments/internal/book/EnchantedBookBuilderImpl.java`
  - Builder implementation for fluent API
  - Supports custom display names, lore, treasure flag

- `src/main/java/io/artificial/enchantments/internal/book/BookFactory.java`
  - Factory for creating EnchantedBook instances

### Anvil System
- `src/main/java/io/artificial/enchantments/internal/anvil/AnvilCostCalculator.java`
  - Vanilla-like cost calculation
  - Supports rarity-based costs (Common=1, Uncommon=2, Rare=4, VeryRare=8)
  - Treasure book multiplier (2x cost)
  - Prior work penalty handling
  - Max cost limit (39 levels)

- `src/main/java/io/artificial/enchantments/internal/anvil/AnvilCombinationLogic.java`
  - Book+item combination
  - Book+book combination
  - Conflict detection between enchantments
  - Level merging (same level upgrades by 1, different takes max)

- `src/main/java/io/artificial/enchantments/internal/anvil/AnvilListener.java`
  - Hooks PrepareAnvilEvent
  - Calculates result preview
  - Handles rename-only operations
  - Sets repair cost for valid combinations

### Tests
- `src/test/java/io/artificial/enchantments/internal/anvil/AnvilCostCalculatorTest.java`
- `src/test/java/io/artificial/enchantments/internal/anvil/AnvilCombinationLogicTest.java`

## Key Design Decisions

1. **Native-First Storage**: Books use the same storage model as items via ItemStorage
2. **Conflict Detection**: Uses EnchantmentDefinition.conflictsWith() and getConflictingEnchantments()
3. **Cost Calculation**: Follows vanilla patterns with custom rarity multipliers
4. **Too Expensive**: Respects vanilla max cost of 39 levels
5. **Prior Work**: Tracks penalty in item PDC (PersistentDataContainer)

## Implementation Notes

- Uses Paper's deprecated AnvilInventory methods (getRenameText, setRepairCost)
  - These are marked for removal but still functional in 1.21
  - Will need migration when Paper provides replacements

- Conflict detection supports bidirectional conflicts:
  - If enchantment A conflicts with B
  - Either A→B or B→A will block the combination

- Book+Book combinations allow incompatible enchantments (like vanilla)
  - Conflicts are tracked but don't block the operation
  - Item+Book respects material applicability

## Integration

Plugin bootstrap updated to register AnvilListener:
- Created in `ArtificialEnchantmentsPlugin.onEnable()`
- Uses ItemStorage from API instance
- Logs registration for debugging

## Build Status

- Main Java source: ✅ Compiles successfully
- Test source: Pre-existing failures in ItemEnchantmentQueryTest.java
- Anvil tests: Created, pending compilation verification

## References

- Uses ItemStorage from Wave 1 foundation
- Shares cost calculation patterns with Task 4
- Follows enchantment table listener pattern from existing code
