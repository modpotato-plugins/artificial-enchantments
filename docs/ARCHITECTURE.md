# Artificial Enchantments Architecture

This document explains the internal design decisions and architecture of the Artificial Enchantments library.

## Table of Contents

1. [Core Principles](#core-principles)
2. [Storage Model](#storage-model)
3. [Registry Design](#registry-design)
4. [Effect Dispatch](#effect-dispatch)
5. [Thread Safety](#thread-safety)
6. [Paper Integration](#paper-integration)
7. [Optional Modules](#optional-modules)

## Core Principles

### 1. Native-First Storage

The library treats Bukkit's native ItemMeta as the authoritative source of truth for enchantment state. This decision was made to:

- **Prevent state drift**: With one source of truth, there's no risk of synchronization issues between multiple storage systems
- **Ensure vanilla compatibility**: Native enchantments work with anvils, grindstones, and other vanilla mechanics
- **Avoid NBT complexity**: NBT is only used for auxiliary metadata, not core enchantment data
- **Support client visibility**: Paper 1.21+ shows native enchantments to clients without packet manipulation

### 2. Dual-Path Effects

The library supports two ways to handle enchantment effects:

- **Typed callbacks**: Direct method calls on `EnchantmentEffectHandler` implementations
- **Event bus**: Publish-subscribe pattern through `EnchantmentEventBus`

Both paths flow through a single dispatch spine to ensure consistent behavior and avoid double-firing.

### 3. Scheduler-Abstraction Design

The library exposes a `FoliaScheduler` abstraction so dependent plugins can centralize how they schedule follow-up work. The current in-tree implementation is `BukkitFoliaScheduler`, so item mutation and heavy work should still be treated conservatively as server-side operations and validated on Folia before production use.

## Storage Model

### ItemStorage Abstraction

The `ItemStorage` interface provides atomic operations for enchantment mutation:

```
ItemStack applyEnchantment(ItemStack, EnchantmentDefinition, int)
ItemStack removeEnchantment(ItemStack, EnchantmentDefinition)
Map<EnchantmentDefinition, Integer> getEnchantments(ItemStack)
```

All operations:
- Validate inputs at the entry point
- Update native ItemMeta atomically
- Clean up auxiliary NBT metadata if needed
- Return the modified ItemStack (may be same instance for mutable items)

### NativeFirstItemStorage Implementation

The default implementation follows the native-first policy:

1. **Read path**: Queries ItemMeta.getEnchants() for enchantment state
2. **Write path**: Uses ItemMeta.addEnchant() / removeEnchant()
3. **Auxiliary data**: Stored in NBT under `artificial_enchantments` compound

### NbtMetadataStorage

Separate from enchantment storage, this handles:

- Compatibility markers (e.g., "migrated_from_v1")
- Display configuration flags
- Cross-plugin compatibility data

Methods: `setString()`, `getString()`, `setInt()`, `getInt()`, `hasKey()`, `clear()`

## Registry Design

### EnchantmentRegistryManager

Central singleton that manages enchantment lifecycle:

```
register(EnchantmentDefinition) -> queues for native registration
get(NamespacedKey) -> returns definition if registered
getAll() -> returns all registered enchantments
getForMaterial(Material) -> index lookup for applicable enchantments
```

### Key Validation

All enchantment keys must follow the pattern:
- Format: `namespace:value`
- Characters: lowercase letters, digits, underscore, dot, slash, hyphen
- Max length: 256 characters
- Namespace and value cannot be empty

Validation happens at registration time, not application time.

### Native Bridge

On Paper 1.21+, enchantments are registered with the native registry:

1. PluginBootstrap registers a handler for RegistryEvents.ENCHANTMENT.freeze()
2. During the freeze event, queued enchantments are registered
3. PaperRegistryBridge provides bidirectional lookup between library definitions and native enchantments
4. Enchantments become visible to clients without lore hacks

## Effect Dispatch

### EffectDispatchSpine

The central coordinator for all effect execution:

```
Dispatch Flow:
1. Calculate scaled value (LevelScaling.apply(level))
2. Create EffectContext via ContextFactory
3. Fire typed callback (if handler exists)
4. Check cancellation - stop if cancelled
5. Fire event bus (if listeners registered)
6. Check final cancellation
7. Sync modifications back to Bukkit event
```

### ContextFactory

Creates appropriate context implementations for each event type:

- `CombatContextImpl` - wraps EntityDamageEvent
- `ToolContextImpl` - wraps BlockBreakEvent/BlockPlaceEvent
- `ProjectileContextImpl` - wraps ProjectileLaunchEvent/ProjectileHitEvent
- `FishingContextImpl` - wraps PlayerFishEvent
- ... and 6 more

All contexts implement `Cancellable` to support cancellation propagation.

### Cancellation Rules

1. Handlers can cancel via `context.tryCancel()`
2. Cancellation is detected by `EffectDispatchSpine` via `context.isCancelled()`
3. Spine propagates to Bukkit event via `((Cancellable)bukkitEvent).setCancelled(true)`
4. Event bus listeners check `event.isCancelled()` to respect prior cancellation

### Event Bus Implementation

The event bus uses a subscription model:

```java
EventSubscription<T> register(
    Plugin plugin,
    Class<T> eventType,
    EventPriority priority,
    boolean ignoreCancelled,
    Consumer<T> handler
)
```

Subscriptions are:
- Stored strongly in a concurrent map keyed by event type
- Automatically cleaned up on plugin disable via a lifecycle listener
- Type-safe through generics

## Thread Safety

### Concurrent Registry Access

The registry uses `ConcurrentHashMap` for thread-safe storage:

- `putIfAbsent()` pattern prevents duplicate registrations
- `computeIfAbsent()` for material-based index updates
- Read operations are lock-free

### FoliaScheduler Abstraction

Provides a single scheduling interface across runtime environments:

```java
// Global region execution
scheduler.runGlobal(Runnable)

// Entity-scoped execution
scheduler.runAtEntity(Entity, Runnable)

// Location-scoped execution
scheduler.runAtLocation(Location, Runnable)
```

The abstraction:
- Detects Folia classes at runtime
- Gives dependent plugins one scheduler API to target
- Uses the in-tree `BukkitFoliaScheduler` implementation today
- Leaves final region-thread validation to the deploying plugin/server stack

### Item Operations

Item enchantment operations are designed around normal server-thread mutation:

- ItemStack creation/manipulation is single-threaded work
- Registry lookups and event-bus bookkeeping are concurrent-safe
- Callers should explicitly reschedule heavy or thread-sensitive follow-up work

## Paper Integration

### Bootstrap Phase

Paper 1.21+ uses a bootstrap system for registry registration:

```yaml
# plugin.yml
load: STARTUP
```

```java
public class PaperEnchantmentBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(
            RegistryEvents.ENCHANTMENT.freeze(),
            event -> {
                // Register native enchantments here
            }
        );
    }
}
```

### Type Conversion

Mapping between library types and Paper types:

| Library Type | Paper Type | Conversion |
|--------------|------------|------------|
| NamespacedKey | Key | `Key.key(nsKey.getNamespace(), nsKey.getKey())` |
| Material | ItemType | `Registry.ITEM.get(Key)` |
| Rarity | weight | COMMON(10), UNCOMMON(5), RARE(2), VERY_RARE(1) |

### Bidirectional Lookup

PaperRegistryBridge maintains:

- `definition -> native Enchantment` mapping for effect dispatch
- `native Enchantment -> definition` mapping for reverse lookups
- This enables seamless translation between library and native representations

## Optional Modules

### PacketEvents Adapter

The optional module provides advanced visual customization:

**Design Principles:**
- Completely optional - core library works without it
- Isolated boundary - no core code depends on it
- Disabled by default - must be explicitly enabled
- Display-only - never alters core state semantics

**Use Cases:**
- Per-player enchantment visibility
- Custom enchantment display formatting
- Client-side packet rewriting

**Activation:**
```java
PacketEventsAdapter adapter = new PacketEventsAdapter(plugin);
if (adapter.enable()) {
    // PacketEvents-backed visuals are now active
}
```

**Safety:**
- Soft dependency - no ClassNotFoundException if absent
- Feature detection at runtime
- Graceful degradation to native display

## Extension Points

### Custom Scaling

Implement `LevelScaling` for custom formulas:

```java
LevelScaling custom = LevelScaling.of(level -> {
    // Your formula here
    return base * Math.pow(level, exponent);
});
```

### Custom Effect Handlers

Implement `EnchantmentEffectHandler` and override relevant methods:

```java
public class MyHandler implements EnchantmentEffectHandler {
    @Override
    public void onEntityDamageByEntity(CombatContext ctx) {
        // Handle damage
    }
    
    @Override
    public void onBlockBreak(ToolContext ctx) {
        // Handle block breaking
    }
}
```

### Event Bus Listeners

Subscribe to events for cross-plugin interaction:

```java
api.getEventBus().register(
    plugin,
    CombatEvent.class,
    EventPriority.NORMAL,
    event -> {
        // React to any combat enchantment effect
    }
);
```

## Performance Considerations

### Scaling Calculations

Scaling formulas are pure functions - cache results if needed:

```java
// Cache scaled values per level to avoid recalculation
Map<Integer, Double> cachedValues = new HashMap<>();
```

### Effect Dispatch

Keep handlers lightweight:

- Heavy work should be rescheduled
- Avoid synchronous database queries
- Cache frequently accessed data

### Registry Lookups

Material-to-enchantment index provides O(1) lookup:

```java
// Fast: material index lookup
Set<EnchantmentDefinition> applicable = api.getEnchantmentsFor(material);

// Slower: full registry scan
api.getAllEnchantments().stream()
    .filter(e -> e.isApplicableTo(material));
```

## Testing Strategy

### Unit Tests

Test pure functions and logic:

- Scaling formula calculations
- Enchantment definition validation
- Registry key validation

### Integration Tests

Test with Paper test server:

- Native registration succeeds
- Item enchantment roundtrips correctly
- Effects fire on game events
- Tick scans and scheduler behavior work as expected on the target server

### QA Scenarios

Each feature has defined QA scenarios:

```
Scenario: Register and apply enchantment
  Steps:
    1. Register enchant definition
    2. Apply to item
    3. Read back enchantment data
  Expected: State is preserved
```

## Migration Notes

### From Other Enchantment Libraries

Migrating from lore-based or NBT-heavy libraries:

1. Convert enchantment definitions to new API
2. Use `ItemStorage.removeAllEnchantments()` to clean old data
3. Re-apply with `ItemStorage.applyEnchantment()`
4. Old NBT data can be cleaned up via auxiliary metadata methods

### Version Compatibility

- Paper 1.21+ only (uses native registry features)
- No legacy Paper/Spigot support planned
- Semantic versioning for API stability
