# Task 1.2: Builder Validation Constraints - Implementation Notes

## Date: 2026-04-19

## Summary

Successfully extended the `EnchantmentDefinition.Builder` interface with validation constraints as specified in Task 1.2.

## Changes Made

### 1. API Interface (`EnchantmentDefinition.java`)
- Added `validate()` method - throws `IllegalStateException` with detailed error messages
- Added `isValid()` method - returns boolean without throwing
- Added `getValidationErrors()` method - returns `List<String>` of all validation errors
- All methods use default implementations for backward compatibility
- Updated `builder()` factory method to return the internal implementation

### 2. Internal Builder Implementation (`EnchantmentDefinitionBuilder.java`)
- Created comprehensive builder implementation with validation logic
- Validation covers:
  - **Required fields**: key, displayName, scaling
  - **Level bounds**: minLevel >= 1, maxLevel > minLevel, maxLevel <= 255
  - **Materials**: non-empty set, all must be items (not blocks)
  - **Conflicts**: no self-conflicts allowed
  - **Cross-property**: curse + tradeable warning, high maxLevel warning
  - **Scaling validation**: NaN and Infinity detection

### 3. Immutable Implementation (`EnchantmentDefinitionImpl.java`)
- Created immutable implementation of `EnchantmentDefinition` interface
- Thread-safe design with unmodifiable collections
- Proper `equals()`, `hashCode()`, and `toString()` implementations

### 4. Unit Tests (`EnchantmentDefinitionBuilderValidationTest.java`)
- 27 comprehensive tests covering all validation scenarios
- Tests for required fields, level bounds, materials, conflicts, cross-properties
- Tests for scaling validation (NaN, Infinity)
- Backward compatibility tests

## Key Design Decisions

1. **Default maxLevel = 5**: Changed from 1 to 5 to ensure valid default state (maxLevel must be > minLevel)

2. **Warning vs Error separation**: 
   - Errors (missing fields, invalid bounds, self-conflicts) prevent `build()`
   - Warnings (curse+tradeable, high level) don't prevent `build()`

3. **Material validation**: Uses `Material.isItem()` to ensure only items (not blocks) can hold enchantments

4. **Scaling validation**: Calculates values at min/max levels to detect NaN/Infinity

## Test Results

```
EnchantmentDefinitionBuilder Validation Tests > All 27 tests PASSED
- Required field validation: 4 tests
- Level bounds validation: 6 tests  
- Material validation: 4 tests
- Conflict validation: 2 tests
- Cross-property validation: 3 tests
- Scaling validation: 4 tests
- Build behavior: 4 tests
- Backward compatibility: 2 tests
```

## Backward Compatibility

✓ All existing builder method signatures unchanged
✓ All existing builder calls continue to work
✓ Default implementations in interface ensure no breaking changes
✓ New validation is additive - doesn't change existing behavior

## Files Modified/Created

1. `src/main/java/io/artificial/enchantments/api/EnchantmentDefinition.java` - Added validation methods
2. `src/main/java/io/artificial/enchantments/internal/EnchantmentDefinitionBuilder.java` - NEW
3. `src/main/java/io/artificial/enchantments/internal/EnchantmentDefinitionImpl.java` - NEW
4. `src/test/java/io/artificial/enchantments/internal/EnchantmentDefinitionBuilderValidationTest.java` - NEW

## Validation Rules Reference

| Rule | Severity | Details |
|------|----------|---------|
| Missing key | ERROR | Required field |
| Missing displayName | ERROR | Required field |
| Missing scaling | ERROR | Required field |
| Empty materials | ERROR | At least one material required |
| minLevel < 1 | ERROR | Must be >= 1 |
| maxLevel <= minLevel | ERROR | Must be > minLevel |
| maxLevel > 255 | ERROR | Minecraft limit |
| Self-conflict | ERROR | Cannot conflict with itself |
| Non-item materials | ERROR | Materials must be items |
| Scaling NaN/Infinity | ERROR | Values must be finite |
| Curse + tradeable | WARNING | Vanilla behavior mismatch |
| maxLevel > 10 | WARNING | High level warning |

## Integration with Task 1.4

The validation foundation established here (particularly the `validate()` and `isValid()` methods) provides the infrastructure needed for lambda-friendly builder methods that can validate before building.
