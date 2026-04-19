# Task 3: Registry and Storage Model - Learnings

## Date: 2026-04-19

## Patterns and Conventions

### Thread Safety
- Use `ConcurrentHashMap` for registry storage to ensure thread-safe operations
- All public methods on `DefaultEnchantmentRegistry` are thread-safe via concurrent map operations
- `putIfAbsent` pattern prevents duplicate registrations in concurrent scenarios

### Key Validation
- Namespace and value must match pattern `^[a-z0-9_./-]+$` (lowercase, digits, underscore, dot, slash, hyphen)
- Maximum key length of 256 characters
- Keys must follow `namespace:value` format with net.kyori.adventure.key.Key
- Validation happens at entry points (register, get, unregister, isRegistered)

### Level Bounds Validation
- `minLevel >= 1` (enforced at registration)
- `maxLevel > minLevel` (must be strictly greater)
- `maxLevel <= Short.MAX_VALUE` for safety
- Validation prevents state drift at registration time

### Native-First Storage Model
- `NativeFirstItemStorage` uses ItemMeta as the source of truth
- Native enchantments are read/written via ItemMeta.getEnchants() / addEnchant() / removeEnchant()
- No duplication of enchantment state in NBT - native components are authoritative
- This prevents data drift between multiple storage locations

### NBT Metadata Separation
- `NbtMetadataStorage` is strictly for auxiliary data (display markers, compatibility flags)
- Uses Item-NBT-API for safe compound manipulation
- Root compound key isolates metadata (`artificial_enchantments`)
- Methods: setString, getString, setInt, getInt, setBoolean, getBoolean, remove, hasKey, clear, getKeys

### Architecture Decisions
1. **Single Source of Truth**: Native ItemMeta holds all enchantment state
2. **Separation of Concerns**: Registry manages definitions, Storage manages item state
3. **Validation Early**: Invalid keys/levels rejected at registration, not application
4. **Null Safety**: All public methods validate null inputs with Objects.requireNonNull
5. **Immutable Returns**: Collections returned as unmodifiable to prevent external modification

## Integration Notes

### Dependencies
- `net.kyori:adventure-api` for Key types
- `de.tr7zw:item-nbt-api` for NBT manipulation
- `org.bukkit:bukkit` for ItemStack/ItemMeta
- `org.jetbrains:annotations` for @NotNull/@Nullable

### Testing Strategy
- Unit tests for registry operations (register, unregister, get, thread safety)
- Unit tests for validation (key format, level bounds)
- Integration tests require Paper server for item storage
- Thread safety test uses concurrent registration attempts

## Files Created

### API Contracts
- `api/enchant/EnchantmentDefinition.java` - Core enchantment contract
- `api/enchant/EnchantmentRegistry.java` - Registry interface
- `api/enchant/ItemStorage.java` - Storage abstraction

### Internal Implementations
- `internal/DefaultEnchantmentRegistry.java` - Thread-safe registry with validation
- `internal/NativeFirstItemStorage.java` - Native-first item storage
- `internal/NbtMetadataStorage.java` - Auxiliary NBT metadata handler

### Unit Tests
- `test/internal/DefaultEnchantmentRegistryTest.java` - Registry operations
- `test/internal/EnchantmentValidationTest.java` - Validation logic

---

# Task 8: Scaling and Attribute Helpers - Learnings

## Date: 2026-04-19

## Patterns and Conventions

### Scaling Formula Implementations
- Linear: `base + (level - 1) * increment` - Sharpness-style constant increase
- Exponential: `base * multiplier^(level - 1)` - Rapid growth, use with care at high levels
- Diminishing: `max * (level / (level + scalingFactor))` - Approaches max asymptotically
- Decaying: `max * (1 - decayFactor^level)` - Alternative diminishing curve
- All formulas validate level >= 1 and throw IllegalArgumentException for invalid inputs

### Pure Function Design
- All scaling calculations are pure functions (no side effects, deterministic)
- Thread-safe by design - no shared mutable state
- Immutable parameters, consistent return values
- Rounding helpers: `calculateRounded`, `calculateFloored`, `calculateCeiled`

### Attribute Modifier Management
- UUID-based tracking for modifier identification and removal
- Deterministic UUID generation: `UUID.nameUUIDFromBytes(enchantmentKey + ":" + target)`
- Consistent naming convention: `artificial_enchantment_` prefix
- Support for both entity and item stack modifiers
- Operations: ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1

### Damage Calculation Pipeline
- Order of operations: Additive -> Multiplicative -> Critical -> Clamping
- Helper methods for damage type classification (physical, magical, environmental)
- Support for life steal, recoil/thorns, armor penetration calculations
- Caps and minimums supported via `clampDamage()`

### Utility Class Pattern
- Private constructor with AssertionError to prevent instantiation
- Static methods only
- No state, no side effects
- Documented mathematical formulas in JavaDoc

## Architecture Decisions

1. **Wrapper Pattern**: `ScalingUtils` delegates to existing API scaling classes
2. **Deterministic UUIDs**: Ensures consistent IDs across server restarts
3. **Pure Functions**: All calculations are side-effect free for testability
4. **Validation at Boundaries**: Invalid levels rejected immediately with exceptions
5. **No Circular Dependencies**: Internal scaling depends only on API contracts

## Files Created

### Internal Scaling
- `internal/scaling/ScalingUtils.java` - Factory methods and scaling utilities
- `internal/scaling/DecayingScaling.java` - Exponential decay formula

### Attribute Helpers
- `internal/AttributeModifierHelper.java` - Entity/item attribute modification
- `internal/DamageModifierHelper.java` - Damage calculation utilities

### Unit Tests
- `test/internal/scaling/ScalingUtilsTest.java` - Formula verification tests
- `test/internal/DamageModifierHelperTest.java` - Damage calculation tests

## Testing Approach

- Parameterized tests for formula verification with known inputs/outputs
- Edge case testing: level 1, high levels (100), invalid levels
- Mathematical correctness verified for all scaling types
- Damage calculation order of operations tested explicitly

---

# Task 5: Native Paper Bridge - Learnings

## Date: 2026-04-19

## Patterns and Conventions

### Paper 1.21+ Registry Bootstrap API
- Use `PluginBootstrap` interface with `load: STARTUP` in plugin.yml
- Register event handler via `context.getLifecycleManager().registerEventHandler()`
- Use `RegistryEvents.ENCHANTMENT.freeze()` (not `compose()` - compose is newer)
- Event handler receives `RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder>`
- Register enchantments via `event.registry().register(TypedKey, BuilderConsumer)`

### Enchantment Registry Entry Construction
- Create keys using `EnchantmentKeys.create(Key.key(namespace, key))` 
- Builder methods: `description()`, `maxLevel()`, `weight()`, `anvilCost()`, `supportedItems()`, `primaryItems()`, `minimumCost()`, `maximumCost()`, `activeSlots()`, `exclusiveWith()`
- Costs use `EnchantmentRegistryEntry.EnchantmentCost.of(base, increment)`
- Item types use `RegistrySet.keySet(RegistryKey.ITEM, ...)` for supported/primary items
- Conflicts use `TypedKey<Enchantment>` with `RegistrySet.keySet(RegistryKey.ENCHANTMENT, typedKeys)`

### Library to Native Type Conversion
- NamespacedKey -> Key: `Key.key(nsKey.getNamespace(), nsKey.getKey())`
- Material -> ItemType: Look up via `org.bukkit.Registry.ITEM.get(Key)`
- EnchantmentDefinition.Rarity -> weight: COMMON(10), UNCOMMON(5), RARE(2), VERY_RARE(1)

### Registry Manager Pattern
- `EnchantmentRegistryManager` as singleton with double-checked locking
- Track pending vs native-registered enchantments separately
- ConcurrentHashMap for thread-safe storage
- Material-based index for fast lookup of applicable enchantments

### Abstraction Layer Design
- Core API (EnchantmentDefinition) remains Paper-agnostic
- Bridge classes (PaperRegistryBridge, PaperEnchantmentConverter) handle all Paper interactions
- No Paper types exposed in public API
- Registry manager tracks both internal definitions and native registration state

## Architecture Decisions

1. **Bootstrap Phase Registration**: All native registrations happen during Paper's freeze event
2. **Definition Queue**: Enchantments registered via API before/during bootstrap are queued for native registration
3. **Bidirectional Mapping**: PaperRegistryBridge provides lookup in both directions (definition <-> native enchantment)
4. **No Legacy Support**: Paper 1.21+ only - no Spigot or older Paper compatibility
5. **Type Safety**: Use Paper's TypedKey<T> for all registry references instead of raw NamespacedKey

## Files Created

### Paper Bridge
- `internal/PaperEnchantmentBootstrap.java` - PluginBootstrap implementation
- `internal/PaperRegistryBridge.java` - Bidirectional registry lookup
- `internal/PaperEnchantmentConverter.java` - Definition to Paper entry conversion
- `internal/EnchantmentRegistryManager.java` - Internal tracking and indexing

### Key Integration Points
- `plugin.yml` requires `load: STARTUP` for bootstrap phase execution
- Bootstrap registers handler for RegistryEvents.ENCHANTMENT.freeze()
- Converter maps: displayName, maxLevel, rarity->weight, applicableMaterials->supportedItems, conflicts->exclusiveWith
- Native registration marks enchantments for client visibility without lore hacks

## Verification

- Paper bridge files compile without errors against Paper 1.21.4 API
- All other compilation errors are from Wave 2/3 tasks (NBT storage, effects, etc.)
- Implementation follows PaperMC documentation for registry bootstrap
- No coupling between public API and Paper implementation types
# Task 6: Item Mutation Service - Learnings
