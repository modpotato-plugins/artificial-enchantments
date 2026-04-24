# Artificial Enchantments API Guide

Version 1.0.0 | [GitHub](https://github.com/modpotato-plugins/artificial-enchantments)

This guide covers every public API surface in Artificial Enchantments. It's written for plugin developers who want to build custom enchantments on Paper 1.21+ with full Folia support.

All examples assume you have a `plugin` variable (your `JavaPlugin` instance) and an `api` variable (your `ArtificialEnchantmentsAPI` instance).

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Defining Enchantments](#defining-enchantments)
3. [Scaling Formulas](#scaling-formulas)
4. [Effect Handlers](#effect-handlers)
5. [Event Bus](#event-bus)
6. [Item Operations](#item-operations)
7. [Enchanted Books](#enchanted-books)
8. [Anvil Operations](#anvil-operations)
9. [Loot Modifiers](#loot-modifiers)
10. [Thread Safety and Folia](#thread-safety-and-folia)
11. [PacketEvents Integration](#packetevents-integration)
12. [Common Patterns and Best Practices](#common-patterns-and-best-practices)

---

## Getting Started

Initialize the API once during `onEnable()`. The library is a shared plugin, so multiple plugins can call `create()` safely. The first call initializes the library. Subsequent calls return the existing instance.

```java
public class MyPlugin extends JavaPlugin {
    private ArtificialEnchantmentsAPI api;

    @Override
    public void onEnable() {
        // Initialize (safe even if another plugin already did this)
        this.api = ArtificialEnchantmentsAPI.create(this);
    }

    @Override
    public void onDisable() {
        // Optional: clean up your enchantments
        ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.getInstance();
        api.unregisterEnchantment(myEnchantment.getKey());
    }
}
```

After bootstrap, grab the shared instance from anywhere:

```java
ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.getInstance();
```

Calling `getInstance()` before any plugin has called `create()` throws `IllegalStateException`.

---

## Defining Enchantments

Enchantments are immutable definitions built with the builder pattern. Every enchantment needs a unique `NamespacedKey`, display metadata, level bounds, a scaling formula, and a set of applicable materials.

### Minimal Example

```java
EnchantmentDefinition lifeSteal = EnchantmentDefinition.builder()
    .key(new NamespacedKey(plugin, "life_steal"))
    .displayName(Component.text("Life Steal", NamedTextColor.RED))
    .description(Component.text("Heals you when dealing damage"))
    .minLevel(1)
    .maxLevel(5)
    .scaling(LevelScaling.linear(0.1, 0.05))
    .applicable(Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.NETHERITE_SWORD)
    .build();

api.registerEnchantment(lifeSteal);
```

### Builder Options

| Method | Required | Description |
|--------|----------|-------------|
| `key(NamespacedKey)` | Yes | Unique identifier. Format: `namespace:value`. |
| `displayName(Component)` | Yes | Name shown to players. |
| `description(Component)` | No | Lore description. Null by default. |
| `minLevel(int)` | Yes | Minimum valid level (>= 1). |
| `maxLevel(int)` | Yes | Maximum valid level (> minLevel). |
| `scaling(LevelScaling)` | Yes | Formula for level-to-value conversion. |
| `scaling(Function<Integer, Double>)` | Yes | Lambda shortcut for custom formulas. |
| `scaling(String, double...)` | Yes | Named algorithm shortcut (e.g., `"LINEAR"`, 1.0, 0.5). |
| `applicable(Material...)` | Yes | Materials this enchantment can apply to. |
| `applicable(Set<Material>)` | Yes | Set-based alternative. |
| `rarity(Rarity)` | No | `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`. Default: `COMMON`. |
| `curse(boolean)` / `curse()` | No | Marks as a curse. Default: false. |
| `tradeable(boolean)` | No | Available from villager trades. Default: true. |
| `discoverable(boolean)` | No | Appears in loot. Default: true. |
| `effectHandler(EnchantmentEffectHandler)` | No | Typed callback handler. Null means no effects. |
| `conflictsWith(NamespacedKey)` | No | Adds a conflicting enchantment key. |
| `conflictsWith(NamespacedKey...)` | No | Adds multiple conflicting keys. |

### Validation

The builder supports pre-build validation so you can catch configuration errors early:

```java
EnchantmentDefinition.Builder builder = EnchantmentDefinition.builder()
    .key(new NamespacedKey(plugin, "my_enchant"))
    .displayName(Component.text("My Enchant"))
    .minLevel(1)
    .maxLevel(5);

// Check without throwing
if (!builder.isValid()) {
    List<String> errors = builder.getValidationErrors();
    for (String error : errors) {
        getLogger().warning("Validation error: " + error);
    }
}

// Or validate and throw if invalid
builder.validate();
EnchantmentDefinition def = builder.build();
```

Validation rules include: required fields, level bounds, non-empty material sets, and self-conflict prevention.

### Behavior Flags

```java
EnchantmentDefinition cursed = EnchantmentDefinition.builder()
    .key(new NamespacedKey(plugin, "fragility"))
    .displayName(Component.text("Fragility", NamedTextColor.RED))
    .minLevel(1)
    .maxLevel(3)
    .scaling(LevelScaling.constant(1.0))
    .applicable(Material.DIAMOND_SWORD)
    .curse()                          // Mark as curse
    .tradeable(false)                 // Villagers won't sell it
    .discoverable(false)              // Won't appear in loot
    .rarity(EnchantmentDefinition.Rarity.VERY_RARE)
    .build();
```

### Conflicts

Prevent incompatible enchantments from coexisting:

```java
EnchantmentDefinition fireAspect = ...;
EnchantmentDefinition iceAspect = EnchantmentDefinition.builder()
    .key(new NamespacedKey(plugin, "ice_aspect"))
    .displayName(Component.text("Ice Aspect"))
    // ... other fields ...
    .conflictsWith(fireAspect.getKey())
    .build();
```

---

## Scaling Formulas

Scaling formulas convert enchantment levels into effect values. The library includes five built-in types and full custom formula support.

### Built-in Types

```java
// Linear: base + (level - 1) * increment
// Sharpness-style. Level 1 = 1.0, Level 2 = 1.5, Level 3 = 2.0
LevelScaling linear = LevelScaling.linear(1.0, 0.5);

// Exponential: base * multiplier^(level - 1)
// Grows fast. Level 1 = 1.0, Level 2 = 1.2, Level 3 = 1.44
LevelScaling exponential = LevelScaling.exponential(1.0, 1.2);

// Diminishing: max * (level / (level + factor))
// Approaches max asymptotically. Level 1 = 20.0, Level 5 = 55.6, Level 10 = 71.4
LevelScaling diminishing = LevelScaling.diminishing(100.0, 4.0);

// Constant: same value at every level
LevelScaling constant = LevelScaling.constant(5.0);

// Stepped: custom values with linear interpolation
Map<Integer, Double> steps = Map.of(1, 5.0, 5, 10.0, 10, 15.0);
LevelScaling stepped = LevelScaling.stepped(steps);
```

### Named Algorithms via Registry

The builder accepts algorithm names for dynamic scaling selection:

```java
EnchantmentDefinition def = EnchantmentDefinition.builder()
    .key(key)
    .displayName(name)
    .minLevel(1)
    .maxLevel(5)
    .scaling("LINEAR", 1.0, 0.5)        // base, increment
    .applicable(Material.DIAMOND_SWORD)
    .build();
```

Available names: `LINEAR`, `EXPONENTIAL`, `DIMINISHING`, `CONSTANT`, `STEPPED`, `DECAYING`.

### Custom Formulas

Pass a lambda directly:

```java
LevelScaling custom = LevelScaling.of(level -> {
    return 10.0 * Math.log(level + 1);
});
```

Or register a reusable named algorithm:

```java
ScalingAlgorithmRegistry registry = api.getScalingRegistry();

registry.register("LOGARITHMIC", new ScalingAlgorithm() {
    @Override
    public LevelScaling create(double... params) {
        return level -> params[0] * Math.log(level + params[1]);
    }

    @Override
    public String getDescription() { return "Logarithmic scaling"; }

    @Override
    public int getParameterCount() { return 2; }

    @Override
    public String[] getParameterNames() { return new String[]{"coefficient", "offset"}; }
});

// Use it
EnchantmentDefinition def = EnchantmentDefinition.builder()
    .scaling("LOGARITHMIC", 10.0, 1.0)
    // ...
    .build();
```

### Querying the Registry

```java
ScalingAlgorithmRegistry registry = api.getScalingRegistry();

if (registry.hasAlgorithm("LINEAR")) {
    Optional<ScalingAlgorithmMetadata> meta = registry.getMetadata("LINEAR");
    meta.ifPresent(m -> getLogger().info("Algorithm: " + m.getDescription()));
}

Set<String> names = registry.getRegisteredNames();
```

---

## Effect Handlers

Effect handlers are typed callbacks that fire when game events occur. Implement `EnchantmentEffectHandler` and override the methods you care about.

### Complete Handler Example

```java
public class LifeStealHandler implements EnchantmentEffectHandler {

    @Override
    public void onEntityDamageByEntity(@NotNull CombatContext context) {
        if (!context.isAttack()) return;

        Player attacker = (Player) context.getAttacker();
        double healPercent = context.getScaledValue();
        double damage = context.getFinalDamage();
        double healAmount = damage * healPercent;

        attacker.heal(healAmount);
        attacker.sendActionBar(Component.text("+" + String.format("%.1f", healAmount) + " hearts"));
    }
}
```

### All Event Types

| Method | Context | Triggered By |
|--------|---------|--------------|
| `onEntityDamageByEntity(CombatContext)` | Attacker, victim, damage | Entity damages another |
| `onEntityDamage(CombatContext)` | Victim, damage | Entity takes any damage |
| `onShieldBlock(CombatContext)` | Defender, damage | Shield blocks an attack |
| `onBlockBreak(ToolContext)` | Player, tool, block, drops | Block broken |
| `onBlockBreakPre(ToolContext)` | Player, tool, block | Before block break (can cancel) |
| `onBlockPlace(ToolContext)` | Player, tool, block | Block placed |
| `onBlockInteract(InteractionContext)` | Player, block, hand | Right/left click block |
| `onEntityInteract(InteractionContext)` | Player, entity, hand | Right click entity |
| `onProjectileLaunch(ProjectileContext)` | Projectile, shooter, velocity | Arrow/trident thrown |
| `onProjectileHit(ProjectileContext)` | Projectile, hit data | Projectile hits block/entity |
| `onFishingAction(FishingContext)` | Player, hook | Cast, bite, reel |
| `onHeldTick(TickContext)` | Player, item, slot | Periodic while held |
| `onArmorTick(TickContext)` | Player, item, slot | Periodic while equipped |
| `onItemUsed(ItemContext)` | Item, slot | Item interaction |
| `onDurabilityDamage(ItemContext)` | Item, durability | Item takes durability loss |
| `onItemConsume(ConsumableContext)` | Food, saturation, health | Eating/drinking |
| `onBowShoot(WeaponContext)` | Force, critical | Bow/crossbow fired |
| `onTridentThrow(WeaponContext)` | Force, pierce | Trident thrown |
| `onItemDrop(ItemContext)` | Item, location | Item dropped |
| `onItemPickup(ItemContext)` | Item, location | Item picked up |

### Cancellation

Contexts that wrap cancellable Bukkit events can be cancelled:

```java
@Override
public void onBlockBreakPre(@NotNull ToolContext context) {
    // Prevent breaking obsidian with this tool
    if (context.getBlock().getType() == Material.OBSIDIAN) {
        tryCancel(context);
    }
}
```

Use the convenience methods from the interface:

```java
tryCancel(context);      // Cancel if cancellable
isCancelled(context);    // Check cancellation state
```

Cancellation propagates back to the Bukkit event and stops both typed callbacks and event bus listeners.

### Attaching a Handler

```java
EnchantmentDefinition myEnchant = EnchantmentDefinition.builder()
    // ... other fields ...
    .effectHandler(new LifeStealHandler())
    .build();
```

Handlers can be stateful if you need per-enchantment configuration. The library creates one handler instance per enchantment definition.

---

## Event Bus

The event bus provides a publish-subscribe alternative to typed callbacks. Use it when you need multiple listeners, cross-plugin reactions, or decoupled effect handling.

### Subscribing

```java
EnchantmentEventBus bus = api.getEventBus();

// Simple subscription
EventSubscription<CombatEvent> sub = bus.register(
    plugin,
    CombatEvent.class,
    event -> {
        if (!event.getEnchantment().getKey().equals(MY_ENCHANT_KEY)) return;
        double multiplier = event.getScaledValue();
        event.setFinalDamage(event.getFinalDamage() * multiplier);
    }
);

// With priority
bus.register(
    plugin,
    CombatEvent.class,
    EventPriority.HIGH,
    event -> { /* ... */ }
);

// Full control: priority + ignore cancelled
bus.register(
    plugin,
    CombatEvent.class,
    EventPriority.NORMAL,
    true,  // ignoreCancelled
    event -> { /* ... */ }
);
```

### Event Types

All events extend `EnchantEffectEvent`. Available event classes:

- `CombatEvent` - damage and combat
- `ToolEvent` - block break/place
- `InteractionEvent` - block/entity interaction
- `ProjectileEvent` - projectile launch/hit
- `FishingEvent` - fishing actions
- `TickEvent` - held/armor ticks
- `ItemEvent` - item usage/durability/drop/pickup
- `ConsumableEvent` - eating/drinking
- `WeaponEvent` - bow/trident use

### Subscription Management

```java
// Unsubscribe a single listener
sub.unsubscribe();

// Unsubscribe all listeners for your plugin
bus.unregisterAll(plugin);

// Check subscription state
if (sub.isActive()) {
    sub.unsubscribe();
}
```

### Dispatch Order and Cancellation

Effects flow through a unified spine:

1. Typed callbacks fire first (`EnchantmentEffectHandler` methods)
2. Event bus listeners fire second (sorted by `EventPriority`)
3. Cancellation stops both paths
4. Data modifications sync back to Bukkit events

Event bus listeners receive the same context data as typed callbacks. Both approaches modify the same underlying event.

---

## Item Operations

The API provides two ways to work with item enchantments: direct API methods and the `ItemStorage` interface. Both are fully thread-safe and Folia-compatible.

### Applying Enchantments

```java
ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);

// By definition
sword = api.applyEnchantment(sword, lifeSteal, 3);

// By key
sword = api.applyEnchantment(sword, new NamespacedKey(plugin, "life_steal"), 3);
```

Levels are validated against the enchantment's min/max bounds. Conflicting enchantments are resolved automatically (or throw `IllegalStateException` if resolution fails).

### Removing Enchantments

```java
// Remove one enchantment
sword = api.removeEnchantment(sword, lifeSteal);
sword = api.removeEnchantment(sword, new NamespacedKey(plugin, "life_steal"));

// Remove all artificial enchantments (vanilla enchants preserved)
sword = api.removeAllEnchantments(sword);
```

### Querying Enchantments

```java
// Direct API methods
if (api.hasEnchantment(sword, lifeSteal)) {
    int level = api.getEnchantmentLevel(sword, lifeSteal);
}

Map<EnchantmentDefinition, Integer> enchantments = api.getEnchantments(sword);
Set<EnchantmentDefinition> forSwords = api.getEnchantmentsFor(Material.DIAMOND_SWORD);
```

### Query Facade

The query facade provides null-safe, ergonomic lookups:

```java
ItemEnchantmentQuery query = api.query();

// Null-safe checks (returns false/0/empty for null items)
if (query.hasEnchantment(item, lifeSteal)) {
    int level = query.getLevel(item, lifeSteal);
}

// Aggregate checks
if (query.isEnchanted(item)) {
    int totalLevel = query.getTotalEnchantmentLevel(item);
    int highest = query.getHighestLevel(item);
}

// Multi-enchant checks
if (query.hasAllEnchantments(item, lifeSteal, fireAspect)) {
    // Both present
}

if (query.hasAnyEnchantment(item, lifeSteal, fireAspect)) {
    // At least one present
}

Map<EnchantmentDefinition, Integer> all = query.getAllEnchantments(item);
```

### Direct ItemStorage Access

For batch operations or advanced use cases:

```java
ItemStorage storage = api.getItemStorage();

ItemStack result = storage.applyEnchantment(item, enchantment, level);
Map<NamespacedKey, Integer> keys = storage.getEnchantmentKeys(item);
```

`ItemStorage` also manages auxiliary metadata (NBT storage for compatibility markers and display flags):

```java
// Store compatibility marker
item = storage.setAuxiliaryMetadata(item, "migrated_from_v1", "true");

// Read it back
String marker = storage.getAuxiliaryMetadata(item, "migrated_from_v1");

// Clean up
item = storage.clearAuxiliaryMetadata(item);
```

---

## Enchanted Books

Artificial enchantments work seamlessly with enchanted books. You can apply custom enchantments to books using the same methods as regular items. The anvil system handles book-to-item and book-to-book combinations automatically.

### Creating Books

```java
// Apply enchantments to an enchanted book
ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
book = api.applyEnchantment(book, lifeSteal, 3);
book = api.applyEnchantment(book, fireAspect, 2);

// Query book contents
Map<EnchantmentDefinition, Integer> enchants = api.getEnchantments(book);
int level = api.getEnchantmentLevel(book, lifeSteal);
```

### Combining Books

When two books are combined in an anvil, the library follows vanilla behavior: enchantments stack if compatible, and the higher level is kept for duplicates. Conflicting enchantments are excluded based on the enchantment's conflict rules.

---

## Anvil Operations

Anvil handling is fully automatic. The library hooks into `PrepareAnvilEvent` to calculate results for custom enchantments without any code on your part.

### What Works Automatically

- **Item + Book**: Custom enchantments from the book transfer to the item
- **Book + Book**: Combines enchantments into a single book
- **Item + Item**: Merges enchantments between two items
- **Renaming**: Prior work penalty is tracked and applied

### Prior Work Tracking

The library tracks anvil use history via persistent data on items. Each anvil operation increases the prior work penalty, which affects cost calculation. This is handled internally and requires no API calls.

### Treasure Books

Books marked as treasure (via auxiliary metadata or by containing undiscoverable vanilla enchantments) cost more to apply. The library detects this automatically during anvil calculations.

```java
// Mark a book as treasure (affects anvil cost)
ItemStorage storage = api.getItemStorage();
book = storage.setAuxiliaryMetadata(book, "treasure_book", "true");
```

### Customizing Anvil Behavior

The library's anvil integration is listener-based and does not expose a direct public API for manual combination. If you need to override anvil behavior, use Bukkit's `PrepareAnvilEvent` directly and modify the result before the library's listener processes it.

---

## Loot Modifiers

Loot modifiers let enchantments change block break drops. Registration is explicit: only enchantments with registered modifiers can affect loot.

### Creating a Modifier

```java
LootModifier doubleDrops = context -> {
    int level = context.getLevel();
    int multiplier = 1 + level;

    for (ItemStack drop : context.getDrops()) {
        drop.setAmount(drop.getAmount() * multiplier);
    }
};
```

### Registering

```java
LootModifierRegistry registry = api.getLootModifierRegistry();
registry.register(myEnchantment, doubleDrops);
```

Multiple modifiers can be registered for the same enchantment. They're invoked in registration order.

### LootContext API

```java
// Access data
ItemStack tool = context.getTool();
Block block = context.getBlock();
Player player = context.getPlayer();
Location dropLoc = context.getLocation();
int level = context.getLevel();
double scaled = context.getScaledValue();
int xp = context.getExpToDrop();

// Modify drops
context.addDrop(new ItemStack(Material.DIAMOND));
context.removeDrop(someItem);
context.clearDrops();
context.setDrops(newDropsList);

// Modify XP
context.setExpToDrop(xp * 2);

// Check for other enchantments on the tool
if (context.hasEnchantment("minecraft:silk_touch")) {
    int silkLevel = context.getEnchantmentLevel("minecraft:silk_touch");
}
```

### Management

```java
// Check if enchantment has modifiers
if (registry.hasModifier(myEnchantment)) {
    List<LootModifier> mods = registry.getModifiers(myEnchantment);
}

// Unregister specific modifier
registry.unregister(myEnchantment, doubleDrops);

// Unregister all for an enchantment
registry.unregisterAll(myEnchantment);

// Check registration count
int count = registry.getModifierCount(myEnchantment);
```

---

## Thread Safety and Folia

All public API methods are thread-safe. Registry operations use `ConcurrentHashMap`. Item operations validate thread context and reschedule to the correct region thread when needed.

### Scheduler

Use the provided scheduler for Folia-safe task execution:

```java
FoliaScheduler scheduler = api.getScheduler();

// Run on global region thread
scheduler.runGlobal(plugin, () -> {
    // Non-location-dependent work
});

// Run at a specific location
scheduler.runAtLocation(plugin, player.getLocation(), () -> {
    // Location-dependent work
});

// Run scoped to an entity
scheduler.runAtEntity(plugin, player, () -> {
    // Entity-specific work
});

// Delayed tasks
ScheduledTask task = scheduler.runGlobalDelayed(plugin, () -> {
    // Run after 20 ticks
}, 20L);

// Repeating tasks
ScheduledTask timer = scheduler.runGlobalTimer(plugin, t -> {
    // Run every 40 ticks
}, 20L, 40L);

// Cancel when done
task.cancel();
```

### Keep Handlers Lightweight

Effect handlers run on the server thread (or region thread on Folia). Reschedule heavy work:

```java
@Override
public void onBlockBreak(@NotNull ToolContext context) {
    // Light work: modify drops inline
    context.addDrop(new ItemStack(Material.DIAMOND));

    // Heavy work: reschedule
    api.getScheduler().runGlobal(plugin, () -> {
        // Database writes, file I/O, expensive calculations
    });
}
```

### Thread Checks

```java
if (api.isFolia()) {
    getLogger().info("Running on Folia with region threading");
}

if (api.getScheduler().isPrimaryThread()) {
    getLogger().info("On main thread");
}
```

---

## PacketEvents Integration

The optional PacketEvents adapter provides per-player visual customization. It's completely isolated from core functionality and disabled by default.

### Enabling

```java
// Check if PacketEvents is available
PacketEventsAdapter adapter = // obtained from plugin initialization
if (adapter.enable()) {
    getLogger().info("PacketEvents adapter enabled");
}
```

### Per-Player Preferences

```java
PacketEventsAdapter.PlayerVisualPreferences prefs =
    new PacketEventsAdapter.PlayerVisualPreferences()
        .setEnabled(true)
        .setModifyLore(true)
        .setShowEnchantmentGlint(true)
        .setHideVanillaEnchantments(false)
        .setLorePrefix("Custom: ");

adapter.setPlayerPreferences(player.getUniqueId(), prefs);
```

### Custom Lore Formatting

```java
prefs.setCustomLoreFormatter(context -> {
    String name = context.getEnchantmentName();
    int level = context.getLevel();
    boolean isArtificial = context.isArtificial();

    // Return custom lore formatting
    return Component.text(name + " " + level);
});
```

### Safety

- Soft dependency: no `ClassNotFoundException` if PacketEvents is absent
- Graceful degradation: falls back to native display
- Display-only: never modifies item state or enchantment logic

---

## Common Patterns and Best Practices

### Shared Plugin Model

**Never shade this library.** Install `artificial-enchantments-1.0.1.jar` once in `plugins/`. All dependent plugins use `compileOnly`.

```kotlin
dependencies {
    compileOnly("io.artificial:artificial-enchantments:1.0.1")
}
```

Shading causes duplicate listeners, registry desync, and scheduler leaks.

### Version Checking

```java
String version = api.getVersion();
// Returns "1.0.0"
```

### Effect Handler Patterns

**Pattern 1: Simple Typed Callback**

Best for enchantments with one clear effect.

```java
public class SimpleHandler implements EnchantmentEffectHandler {
    @Override
    public void onEntityDamageByEntity(@NotNull CombatContext ctx) {
        double bonus = ctx.getScaledValue();
        ctx.setFinalDamage(ctx.getFinalDamage() + bonus);
    }
}
```

**Pattern 2: Event Bus for Cross-Plugin Interaction**

Best when multiple plugins need to react to the same enchantment.

```java
api.getEventBus().register(plugin, CombatEvent.class, EventPriority.NORMAL, event -> {
    // React to any combat enchantment
    EnchantmentDefinition enchant = event.getEnchantment();
    // Cross-plugin logic here
});
```

**Pattern 3: Hybrid Approach**

Use typed callbacks for the primary effect and event bus for secondary reactions.

```java
// Primary effect in handler
public class PrimaryHandler implements EnchantmentEffectHandler {
    @Override
    public void onEntityDamageByEntity(@NotNull CombatContext ctx) {
        ctx.setFinalDamage(ctx.getFinalDamage() * 1.5);
    }
}

// Secondary reaction via event bus
api.getEventBus().register(plugin, CombatEvent.class, EventPriority.HIGH, event -> {
    if (event.getEnchantment().getKey().equals(MY_KEY)) {
        // Play sound, spawn particles, etc.
    }
});
```

### Scaling Caching

Scaling formulas are pure functions. Cache results if you call them frequently:

```java
private final Map<Integer, Double> cache = new HashMap<>();

private double getCachedValue(EnchantmentDefinition enchant, int level) {
    return cache.computeIfAbsent(level, enchant::calculateScaledValue);
}
```

### Conflict Management

Design conflict sets carefully. Conflicts are checked at application time (anvil, command, direct API), not at registration time.

```java
// A conflicts with B
EnchantmentDefinition a = EnchantmentDefinition.builder()
    .conflictsWith(b.getKey())
    .build();

// B should probably conflict back
EnchantmentDefinition b = EnchantmentDefinition.builder()
    .conflictsWith(a.getKey())
    .build();
```

### Null Safety

The query facade handles nulls gracefully. Direct API methods throw `IllegalArgumentException` for null required parameters.

```java
// Safe: returns false for null item
boolean hasIt = api.query().hasEnchantment(null, lifeSteal);

// Throws: item parameter is required
boolean hasIt = api.hasEnchantment(null, lifeSteal);
```

### Cleanup on Disable

Unregister your enchantments to keep the server clean:

```java
@Override
public void onDisable() {
    ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.getInstance();
    for (EnchantmentDefinition def : myEnchantments) {
        api.unregisterEnchantment(def.getKey());
    }
    api.getEventBus().unregisterAll(this);
    api.getLootModifierRegistry().clear();
}
```

---

## Quick Reference

### ArtificialEnchantmentsAPI

| Method | Description |
|--------|-------------|
| `create(Plugin)` | Initialize and bind to plugin |
| `getInstance()` | Get shared instance |
| `registerEnchantment(EnchantmentDefinition)` | Register custom enchantment |
| `unregisterEnchantment(NamespacedKey)` | Remove from registry |
| `getEnchantment(NamespacedKey)` | Lookup by key |
| `getAllEnchantments()` | List all registered |
| `getEnchantmentsFor(Material)` | Get applicable enchantments |
| `applyEnchantment(ItemStack, EnchantmentDefinition, int)` | Apply to item |
| `removeEnchantment(ItemStack, EnchantmentDefinition)` | Remove from item |
| `removeAllEnchantments(ItemStack)` | Clear all artificial enchants |
| `getEnchantmentLevel(ItemStack, EnchantmentDefinition)` | Get level (0 if absent) |
| `getEnchantments(ItemStack)` | Map of all enchants on item |
| `hasEnchantment(ItemStack, EnchantmentDefinition)` | Check presence |
| `getEventBus()` | Access event bus |
| `getItemStorage()` | Direct storage access |
| `query()` | Null-safe query facade |
| `getScalingRegistry()` | Scaling algorithm registry |
| `getLootModifierRegistry()` | Loot modifier registry |
| `getScheduler()` | Folia-safe scheduler |
| `isFolia()` | Check Folia presence |
| `getVersion()` | Get library version |
| `getPlugin()` | Get owning plugin |

### EnchantmentEventBus

| Method | Description |
|--------|-------------|
| `register(Plugin, Class<T>, Consumer<T>)` | Simple subscription |
| `register(Plugin, Class<T>, EventPriority, Consumer<T>)` | With priority |
| `register(Plugin, Class<T>, EventPriority, boolean, Consumer<T>)` | Full control |
| `unregisterAll(Plugin)` | Remove all plugin subscriptions |
| `unregister(EventSubscription)` | Remove specific subscription |
| `dispatch(T)` | Dispatch event manually |
| `dispatch(Class<T>, Supplier<T>)` | Create and dispatch |

### ItemEnchantmentQuery

| Method | Description |
|--------|-------------|
| `hasEnchantment(ItemStack, EnchantmentDefinition)` | Null-safe presence check |
| `hasEnchantment(ItemStack, NamespacedKey)` | Check by key |
| `getLevel(ItemStack, EnchantmentDefinition)` | Get level (0 if absent) |
| `getAllEnchantments(ItemStack)` | Map of enchantments |
| `isEnchanted(ItemStack)` | Has any custom enchantment |
| `hasAllEnchantments(ItemStack, EnchantmentDefinition...)` | All present |
| `hasAnyEnchantment(ItemStack, EnchantmentDefinition...)` | Any present |
| `getTotalEnchantmentLevel(ItemStack)` | Sum of all levels |
| `getHighestLevel(ItemStack)` | Max level |
| `getEnchantmentsFor(Material)` | Applicable to material |

---

*For installation and architecture details, see [README.md](../README.md) and [ARCHITECTURE.md](ARCHITECTURE.md).*
