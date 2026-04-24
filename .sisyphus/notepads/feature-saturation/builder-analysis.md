# EnchantmentDefinition Builder Analysis

**Date:** 2026-04-19
**Task:** Wave 1, Task 1.1 - Builder Structure Analysis
**Files Analyzed:**
- `src/main/java/io/artificial/enchantments/api/EnchantmentDefinition.java`
- `src/main/java/io/artificial/enchantments/api/scaling/LevelScaling.java`
- `src/main/java/io/artificial/enchantments/internal/scaling/ScalingUtils.java`
- `src/main/java/io/artificial/enchantments/api/scaling/LinearScaling.java`
- `src/main/java/io/artificial/enchantments/api/scaling/ExponentialScaling.java`
- `src/main/java/io/artificial/enchantments/api/scaling/SteppedScaling.java`
- `src/main/java/io/artificial/enchantments/internal/scaling/DecayingScaling.java`
- Test files for usage patterns

---

## 1. Current Builder Architecture

### 1.1 Interface Design

The `EnchantmentDefinition` is an **interface** with a nested `Builder` interface. This is a deliberate design choice:

- **API Module**: Defines the contract (`EnchantmentDefinition` interface + nested `Builder` interface)
- **Implementation**: The actual builder implementation is NOT in the API module (throws `UnsupportedOperationException`)
- **Pattern**: Interface-based API with implementation deferred to consumers or other modules

### 1.2 Current Builder Methods

The `EnchantmentDefinition.Builder` interface defines the following methods:

#### Required Properties (must be set before build):
| Method | Parameter | Validation |
|--------|-----------|------------|
| `key(NamespacedKey)` | Non-null key | `@NotNull` annotation |
| `displayName(Component)` | Non-null component | `@NotNull` annotation |
| `minLevel(int)` | Level >= 1 | Javadoc states "must be >= 1" |
| `maxLevel(int)` | Level > minLevel | Javadoc states "must be > minLevel" |
| `scaling(LevelScaling)` | Scaling instance | `@NotNull` annotation |
| `scaling(Function<Integer, Double>)` | Lambda formula | `@NotNull` annotation |
| `applicable(Material...)` | Varargs materials | `@NotNull` annotation |
| `applicable(Set<Material>)` | Material set | `@NotNull` annotation |

#### Optional Properties (have defaults):
| Method | Default | Notes |
|--------|---------|-------|
| `description(Component)` | null | Lore display |
| `curse(boolean)` / `curse()` | false | Curse enchantment flag |
| `tradeable(boolean)` | true | Villager trading |
| `discoverable(boolean)` | true | Loot table appearance |
| `rarity(Rarity)` | COMMON | Enum: COMMON, UNCOMMON, RARE, VERY_RARE |
| `effectHandler(EnchantmentEffectHandler)` | null | Effect implementation |
| `conflictsWith(NamespacedKey)` | empty set | Can be called multiple times |
| `conflictsWith(NamespacedKey...)` | empty set | Varargs version |

#### Build Method:
```java
@NotNull
EnchantmentDefinition build(); // throws IllegalStateException if required properties missing
```

---

## 2. Validation Gaps (CRITICAL FINDINGS)

### 2.1 Missing Builder-Level Validation

**Current State**: The builder interface only has `@NotNull` annotations. There is **NO** validation of:

1. **Level Bounds Validation**:
   - `minLevel` is not validated to be >= 1 at build time
   - `maxLevel` is not validated to be > `minLevel` at build time
   - No validation that levels are within reasonable bounds (e.g., max level 255 for Minecraft)

2. **Empty Materials Set**:
   - `applicable()` with empty varargs or empty set is allowed
   - Creates enchantments that can't be applied to anything

3. **Key Uniqueness**:
   - No validation that the key is unique (duplicate keys allowed in builder)
   - Duplicate detection happens at registry time, not build time

4. **Circular Conflicts**:
   - No validation that an enchantment conflicts with itself
   - `conflictsWith(key)` where key equals the enchantment's own key is allowed

5. **Scaling Validity**:
   - No validation that scaling produces reasonable values
   - Could create scaling that returns NaN, Infinity, or negative values

### 2.2 Missing Cross-Property Validation

- **Level Consistency**: No check that minLevel <= maxLevel
- **Rarity + Curse**: Curses typically shouldn't be tradeable (vanilla behavior)
- **Effect Handler + Scaling**: No coupling validation (could have handler with constant 0 scaling)

---

## 3. Custom Scaling Integration Points

### 3.1 Current Scaling Architecture

The `LevelScaling` interface is a **@FunctionalInterface**:

```java
@FunctionalInterface
public interface LevelScaling {
    double calculate(int level);
    
    // Static factory methods
    static LevelScaling of(Function<Integer, Double> formula)
    static LevelScaling linear(double base, double increment)
    static LevelScaling exponential(double base, double multiplier)
    static LevelScaling diminishing(double maxValue, double scalingFactor)
    static LevelScaling constant(double value)
    static LevelScaling stepped(Map<Integer, Double> steps)
}
```

### 3.2 Existing Scaling Implementations

| Implementation | Location | Formula | Validation |
|---------------|----------|---------|------------|
| `LinearScaling` | `api/scaling/` | base + (level-1)*increment | Level >= 1 |
| `ExponentialScaling` | `api/scaling/` | base * multiplier^(level-1) | multiplier > 0, level >= 1 |
| `DiminishingScaling` | `api/scaling/` | max * (level / (level + factor)) | max > 0, factor > 0, level >= 1 |
| `ConstantScaling` | `api/scaling/` | value | None |
| `SteppedScaling` | `api/scaling/` | Interpolated steps | Steps not empty, must include level 1 |
| `DecayingScaling` | `internal/scaling/` | max * (1 - decayFactor^level) | max > 0, 0 < decay < 1 |

### 3.3 Builder Hook Points for Custom Scaling

**Current Integration** (Already exists):
```java
// Direct LevelScaling instance
Builder scaling(@NotNull LevelScaling scaling);

// Lambda/function hook
Builder scaling(@NotNull Function<Integer, Double> formula);
```

**Extension Opportunities**:

1. **Named Scaling Presets**:
   ```java
   Builder scaling(String presetName, double... params);
   // Example: .scaling("sharpness", 1.0, 0.5)
   ```

2. **Validated Custom Scaling**:
   ```java
   Builder scaling(@NotNull LevelScaling scaling, double minValue, double maxValue);
   // Validates that scaling stays within bounds at all levels
   ```

3. **Per-Level Validation**:
   ```java
   Builder scaling(@NotNull LevelScaling scaling, IntPredicate levelValidator);
   ```

4. **Scaling Metadata**:
   ```java
   Builder scalingDescription(String description); // For display purposes
   Builder scalingDisplayFormat(String format); // e.g., "+%.1f damage"
   ```

---

## 4. Builder Ergonomics Improvements

### 4.1 Current Ergonomics Issues

1. **Conflicts API is Additive Only**:
   - Can only ADD conflicts, can't set a complete set at once
   - No `conflictsWith(Set<NamespacedKey>)` method

2. **No Batch Operations**:
   - Can't set multiple properties from a config map
   - No `fromConfig(ConfigurationSection)` method

3. **No Validation Pre-Build**:
   - Can't check if builder is valid before calling build()
   - No `validate()` or `isValid()` method

4. **No Copy/Template Support**:
   - Can't create builder from existing definition
   - No `toBuilder()` method on EnchantmentDefinition

5. **Type Safety Gaps**:
   - Materials can be mixed (e.g., swords + pickaxes) without warning
   - No validation that materials make sense for the enchantment type

### 4.2 Proposed Ergonomic Additions

#### Validation Builder Methods:
```java
// Pre-build validation
Builder validate(); // Throws IllegalStateException with detailed message
boolean isValid(); // Check without throwing
List<String> getValidationErrors(); // Get all validation errors
```

#### Batch Configuration:
```java
Builder fromMap(Map<String, Object> config); // Load from config
Builder defaults(EnchantmentDefinition template); // Copy defaults from another
```

#### Enhanced Conflicts API:
```java
Builder conflictsWith(Set<NamespacedKey> keys); // Batch set
Builder clearConflicts(); // Remove all conflicts
Builder conflictsWithAll(); // Conflicts with everything (curse behavior)
```

#### Material Validation:
```java
Builder applicable(@NotNull Material... materials);
Builder validateMaterials(MaterialTag tag); // e.g., WEAPONS, TOOLS
```

---

## 5. Backward Compatibility Considerations

### 5.1 Current Consumer Usage Pattern

From test files, current usage is:

```java
// Manual implementation (test pattern)
new TestEnchantmentDefinition(name, materials) {
    // Implements interface directly
}

// Expected production pattern (from README)
EnchantmentDefinition.builder()
    .key(new NamespacedKey(plugin, "life_steal"))
    .displayName(Component.text("Life Steal", NamedTextColor.RED))
    .description(Component.text("Heals you when dealing damage"))
    .minLevel(1)
    .maxLevel(5)
    .scaling(LevelScaling.linear(0.1, 0.05))
    .applicable(Material.DIAMOND_SWORD, Material.IRON_SWORD)
    .rarity(EnchantmentDefinition.Rarity.RARE)
    .effectHandler(new LifeStealHandler())
    .build();
```

### 5.2 Compatibility Preservation Strategy

**MUST Maintain**:
1. All existing builder method signatures
2. Default values for optional properties
3. `IllegalStateException` on `build()` for missing required properties
4. Interface-based design (no concrete classes in API)

**Can Add (Backward Compatible)**:
1. New methods with default implementations in interface (Java 8+)
2. Overloaded methods with additional parameters
3. New optional properties
4. Validation methods that don't change existing behavior

**Cannot Change**:
1. Existing method return types
2. Required/optional status of current properties
3. Exception types thrown

---

## 6. Integration with Validation System

### 6.1 Validation Timing Options

| Timing | Pros | Cons |
|--------|------|------|
| **Build-time** (current) | Fails fast, clear error location | Can't fix incrementally |
| **Per-method** | Immediate feedback | Expensive, verbose |
| **Lazy** (on first use) | Flexible | Late failure, hard to debug |
| **Explicit** (`validate()` call) | User controlled | May be forgotten |

**Recommendation**: Add explicit `validate()` method while keeping build-time validation as safety net.

### 6.2 Validation Severity Levels

```java
enum ValidationSeverity {
    ERROR,    // Must fix - build() will fail
    WARNING,  // Should fix - logged but build() succeeds
    INFO      // FYI - logged only
}
```

Example validations:
- **ERROR**: minLevel > maxLevel, empty materials, null key
- **WARNING**: curse + tradeable=true, maxLevel > 10 without explicit opt-in
- **INFO**: no description provided, only 1 applicable material

---

## 7. Effect Execution Safety Analysis

### 7.1 Current Effect Handler Interface

```java
public interface EnchantmentEffectHandler {
    void onEntityDamageByEntity(@NotNull CombatContext context);
    void onBlockBreak(@NotNull ToolContext context);
    // ... 15+ more event methods
}
```

### 7.2 Safety Gaps

1. **No Pre-Execution Validation**:
   - Effect handlers receive context but no validation that enchantment is valid
   - Could execute with invalid level (e.g., level 0 or negative)

2. **No Post-Execution Validation**:
   - Effect handlers can modify context in unsafe ways
   - No validation of returned values (NaN, Infinity)

3. **No Effect Isolation**:
   - One effect handler failure can break entire dispatch chain
   - No try-catch wrapper around individual handlers

4. **Thread Safety**:
   - Effect handlers may not be thread-safe
   - No validation that handler is stateless/immutable

### 7.3 Proposed Safety Additions

```java
// Builder-level safety configuration
Builder effectHandler(@Nullable EnchantmentEffectHandler handler);
Builder effectHandler(@Nullable EnchantmentEffectHandler handler, EffectSafetyMode mode);

enum EffectSafetyMode {
    LENIENT,    // Current behavior - trust the handler
    STRICT,     // Wrap with validation and try-catch
    SANDBOXED   // Run in isolated context with validation
}

// Validation at execution time
void onEntityDamageByEntity(@NotNull CombatContext context) {
    // Auto-validate level before execution
    // Auto-validate scaled value is finite
    // Wrap in try-catch with graceful degradation
}
```

---

## 8. Summary: Extension Points for Wave 1 Tasks

### Task 1.2: Validation Constraints
**Location**: Extend `EnchantmentDefinition.Builder` interface
**Additions**:
- `validate()` method
- `isValid()` method  
- `getValidationErrors()` method
- Enhanced `build()` with detailed error messages

### Task 1.3: Custom Scaling Integration
**Location**: Already supported via `scaling(Function<Integer, Double>)`
**Additions**:
- `scaling(String preset, double... params)` for named presets
- `scaling(LevelScaling, double minBound, double maxBound)` for bounded scaling
- `scalingDescription(String)` for metadata

### Task 1.4: Safer Effect Execution
**Location**: `EnchantmentEffectHandler` interface + builder
**Additions**:
- `effectHandler(handler, EffectSafetyMode)` builder method
- Default try-catch wrappers in dispatch spine
- Validation of context values before/after execution

---

## 9. Key Architectural Decisions

1. **Keep Interface-Based Design**: Don't create concrete builder classes in API
2. **Default Method Additions**: Use Java 8 default methods for new validation methods
3. **No Breaking Changes**: All additions must be backward compatible
4. **Validation at Build Time**: Primary validation happens at build(), with optional earlier validation
5. **Scaling Flexibility**: Preserve functional interface design for custom formulas
6. **Effect Safety Opt-In**: Safety modes should be configurable, not forced

---

## 10. Next Steps

1. **Task 1.2**: Implement validation methods in builder interface (default implementations)
2. **Task 1.3**: Add scaling preset methods and bounded scaling variants
3. **Task 1.4**: Design effect safety modes and execution wrappers
4. **Documentation**: Update README with new builder capabilities
