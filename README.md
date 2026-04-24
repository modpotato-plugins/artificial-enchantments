# Artificial Enchantments

A Paper 1.21+ library for creating custom enchantments with native registry integration, Folia support, and flexible effect handling.

## Features

- Native Paper 1.21+ registry integration (client-visible enchantments without lore hacks)
- Folia-safe region-thread scheduling
- Native-first storage (ItemMeta as source of truth)
- Dual effect dispatch: typed callbacks + event bus
- Built-in scaling formulas (linear, exponential, diminishing, stepped)
- Optional PacketEvents adapter boundary
- Thread-safe registry and item operations

## Requirements

- Paper 1.21+ (Folia supported)
- Java 21+

## Installation

### For Server Administrators

Install `artificial-enchantments-1.0.2.jar` into your server's `plugins/` folder. This is a **shared plugin library** — it must be installed as a plugin, not shaded into other plugins.

```bash
# Download the shaded JAR (includes all required dependencies)
wget https://github.com/modpotato-plugins/artificial-enchantments/releases/download/v1.0.1/artificial-enchantments-1.0.2.jar

# Place in plugins folder
cp artificial-enchantments-1.0.2.jar /path/to/server/plugins/
```

### For Plugin Developers

Add as a `compileOnly` dependency. **Do NOT shade this library** — see [Multi-Plugin Usage](#multi-plugin-usage) for why.

#### Quick Start: JitPack (Recommended)

The easiest way — no authentication required.

`build.gradle` / `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("com.github.modpotato-plugins:artificial-enchantments:v1.0.1")
}
```

That's it. JitPack builds automatically from GitHub tags.

#### Official: GitHub Packages

If you prefer the official GitHub Packages registry, it requires a Personal Access Token (PAT) even for public repositories.

**1. Create a PAT** with the `read:packages` scope:
GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)

**2. Configure credentials** in `~/.gradle/gradle.properties` (global) or project `gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PAT_TOKEN
```

Or use environment variables:

```bash
export GITHUB_ACTOR=YOUR_GITHUB_USERNAME
export GITHUB_TOKEN=YOUR_PAT_TOKEN
```

**3. Add the repository and dependency:**

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/modpotato-plugins/artificial-enchantments")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("io.artificial:artificial-enchantments:1.0.2")
}
```

`plugin.yml`:

```yaml
depend: [ArtificialEnchantments]
# or soft-depend if you want graceful degradation:
# softdepend: [ArtificialEnchantments]
```

## Multi-Plugin Usage

Artificial Enchantments is designed as a **shared plugin library**. It must be installed once as a server plugin, and all dependent plugins reference it via `compileOnly`.

### Why Not Shading?

**Do not shade this library into your plugin.** Doing so will cause serious problems:

| Problem | Cause |
|---------|-------|
| **Duplicate event listeners** | Each shaded copy registers its own Bukkit listeners, causing effects to fire multiple times |
| **Registry desync** | Each copy has its own `EnchantmentRegistryManager` singleton — enchantments registered in one copy are invisible to others |
| **Native registry corruption** | Paper's native enchantment registry receives duplicate/conflicting registrations |
| **Folia scheduler leaks** | Each copy spawns its own scheduler threads, never cleaned up |
| **Event bus isolation** | Subscribers in one shaded copy cannot see events from another |

### The Shared Plugin Model

```
Server
├── plugins/
│   ├── artificial-enchantments-1.0.2.jar  <-- Install once here
│   ├── my-enchants-plugin.jar              <-- compileOnly dependency
│   └── another-enchants-plugin.jar         <-- compileOnly dependency
```

Both `my-enchants-plugin` and `another-enchants-plugin` call `ArtificialEnchantmentsAPI.getInstance()` and receive the **same shared instance**. Their enchantments coexist in a single registry, effects dispatch through a single spine, and all events are visible to all listeners.

### Lifecycle Safety

The library initializes on first `create(plugin)` call and persists until the server shuts down. Individual plugins can safely call `create()` in their `onEnable()` — subsequent calls return the existing instance without side effects.

```java
@Override
public void onEnable() {
    // Safe to call even if another plugin already initialized
    ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.create(this);
    
    // Register your enchantments
    api.registerEnchantment(myEnchantment);
}

@Override
public void onDisable() {
    // Optional: unregister your enchantments to clean up
    ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.getInstance();
    api.unregisterEnchantment(myEnchantment.getKey());
}
```

## Quick Start

### 1. Initialize the API

```java
public class MyPlugin extends JavaPlugin {
    private ArtificialEnchantmentsAPI api;
    
    @Override
    public void onEnable() {
        // Initialize the library
        this.api = ArtificialEnchantmentsAPI.create(this);
        
        // Or get the shared instance later
        this.api = ArtificialEnchantmentsAPI.getInstance();
    }
}
```

### 2. Define an Enchantment

```java
EnchantmentDefinition lifeSteal = EnchantmentDefinition.builder()
    .key(new NamespacedKey(plugin, "life_steal"))
    .displayName(Component.text("Life Steal", NamedTextColor.RED))
    .description(Component.text("Heals you when dealing damage"))
    .minLevel(1)
    .maxLevel(5)
    .scaling(LevelScaling.linear(0.1, 0.05)) // 10% at level 1, +5% per level
    .applicable(Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.NETHERITE_SWORD)
    .rarity(EnchantmentDefinition.Rarity.RARE)
    .effectHandler(new LifeStealHandler())
    .build();

api.registerEnchantment(lifeSteal);
```

### 3. Handle Effects (Typed Callbacks)

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

### 4. Handle Effects (Event Bus)

```java
api.getEventBus().register(
    plugin,
    CombatEvent.class,
    EventPriority.NORMAL,
    event -> {
        if (!event.getEnchantment().getKey().equals(MY_ENCHANT_KEY)) return;
        
        double multiplier = event.getScaledValue();
        event.setFinalDamage(event.getFinalDamage() * multiplier);
    }
);
```

### 5. Apply Enchantments

```java
ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
sword = api.applyEnchantment(sword, lifeSteal, 3);

// Or by key
sword = api.applyEnchantment(sword, new NamespacedKey(plugin, "life_steal"), 3);
```

### 6. Query Enchantments

```java
// Check if item has enchantment
if (api.hasEnchantment(item, lifeSteal)) {
    int level = api.getEnchantmentLevel(item, lifeSteal);
}

// Get all enchantments
Map<EnchantmentDefinition, Integer> enchantments = api.getEnchantments(item);

// Get applicable enchantments for a material
Set<EnchantmentDefinition> applicable = api.getEnchantmentsFor(Material.DIAMOND_SWORD);
```

## Architecture

### Native-First Storage Policy

This library uses Bukkit's native ItemMeta as the source of truth for enchantment state:

- Enchantments are stored in the native enchantment map (visible to clients)
- NBT is reserved for auxiliary metadata only (display flags, compatibility markers)
- No duplication between native and custom storage
- State cannot drift because there's only one source of truth

This design ensures:
- Client-visible enchantments without packet manipulation
- Compatibility with vanilla anvil mechanics
- Clean removal of enchantments (no orphaned NBT data)
- Predictable behavior with other plugins

### Effect Dispatch

Effects fire through a unified dispatch spine:

1. Typed callbacks fire first (EnchantmentEffectHandler methods)
2. Event bus listeners fire second
3. Cancellation in either path stops both
4. Data modifications sync back to the context

Choose your approach:
- **Typed callbacks**: Type-safe, compile-time checked, ideal for simple effects
- **Event bus**: Decoupled, multiple listeners, better for complex interactions

### Scaling Formulas

Built-in formulas cover common use cases:

```java
// Linear: base + (level - 1) * increment (like Sharpness)
LevelScaling linear = LevelScaling.linear(1.0, 0.5);
// Level 1: 1.0, Level 2: 1.5, Level 3: 2.0

// Exponential: base * multiplier^(level - 1)
LevelScaling exponential = LevelScaling.exponential(1.0, 1.2);
// Level 1: 1.0, Level 2: 1.2, Level 3: 1.44

// Diminishing: max * (level / (level + factor))
LevelScaling diminishing = LevelScaling.diminishing(100.0, 4.0);
// Level 1: 20.0, Level 5: 55.6, Level 10: 71.4, approaches 100.0

// Stepped: custom values with interpolation
Map<Integer, Double> steps = Map.of(1, 5.0, 5, 10.0, 10, 15.0);
LevelScaling stepped = LevelScaling.stepped(steps);

// Custom function
LevelScaling custom = LevelScaling.of(level -> level * level * 0.5);
```

### Optional PacketEvents Module

The library includes an optional PacketEvents adapter for advanced visuals:

- Disabled by default
- Fully isolated from core functionality
- Used only for display concerns (packet rewriting)
- Core library works fine without it

## Context Types

Effect handlers receive context objects with relevant data:

| Context | Triggered By | Key Data |
|---------|--------------|----------|
| `CombatContext` | Damage events | Attacker, victim, damage, weapon |
| `ToolContext` | Block break/place | Player, tool, block, drops |
| `InteractionContext` | Player interact | Block/entity, hand, click type |
| `ProjectileContext` | Projectile launch/hit | Projectile, shooter, velocity |
| `FishingContext` | Fishing actions | Hook, caught item/entity |
| `TickContext` | Held/armor ticks | Player, item, slot, tick count |
| `ItemContext` | Item usage/durability | Item, slot, durability |
| `ConsumableContext` | Item consumption | Food level, saturation, health |
| `WeaponContext` | Bow/trident use | Force, critical, pierce level |

## Thread Safety

All public API methods are thread-safe:

- Registry operations use `ConcurrentHashMap`
- Item operations are atomic
- Folia region checks happen automatically

Keep effect handlers lightweight. Reschedule heavy work:

```java
@Override
public void onBlockBreak(@NotNull ToolContext context) {
    // Light work: modify drops
    context.addDrop(new ItemStack(Material.DIAMOND));
    
    // Heavy work: reschedule
    api.getScheduler().runLater(plugin, () -> {
        // Expensive operation here
    }, 1);
}
```

## API Reference

### ArtificialEnchantmentsAPI

Main entry point for all operations:

- `create(Plugin)` / `getInstance()` - Get API instance
- `registerEnchantment(EnchantmentDefinition)` - Register custom enchant
- `applyEnchantment(ItemStack, EnchantmentDefinition, int)` - Apply to item
- `removeEnchantment(ItemStack, EnchantmentDefinition)` - Remove from item
- `getEnchantments(ItemStack)` - Get all enchantments on item
- `getEventBus()` - Access the event bus

### EnchantmentDefinition

Immutable definition created via builder:

- `getKey()` - Unique identifier
- `getDisplayName()` / `getDescription()` - User-facing text
- `getMinLevel()` / `getMaxLevel()` - Valid level range
- `getScaling()` - Level-to-value formula
- `getApplicableMaterials()` - Where it can be applied
- `isCurse()` / `isTradeable()` / `isDiscoverable()` - Behavior flags
- `getEffectHandler()` - Effect implementation

### EnchantmentEffectHandler

Interface for typed callback effects. Override methods for events you care about:

- `onEntityDamageByEntity(CombatContext)` - Attack damage
- `onBlockBreak(ToolContext)` - Block breaking
- `onProjectileLaunch(ProjectileContext)` - Projectiles
- `onHeldTick(TickContext)` - Periodic held item effects
- ... and 15 more event types

### EnchantmentEventBus

Subscribe to enchantment events:

- `register(Plugin, Class<T>, Consumer<T>)` - Simple subscription
- `register(Plugin, Class<T>, EventPriority, Consumer<T>)` - With priority
- `register(Plugin, Class<T>, EventPriority, boolean, Consumer<T>)` - Full control
- `unregisterAll(Plugin)` - Remove all plugin subscriptions

## License

MIT License - See LICENSE file for details.

## Contributing

Issues and PRs welcome. Please follow the existing code style and include tests for new features.
