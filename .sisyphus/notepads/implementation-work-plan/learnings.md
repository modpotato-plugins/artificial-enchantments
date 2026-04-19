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
