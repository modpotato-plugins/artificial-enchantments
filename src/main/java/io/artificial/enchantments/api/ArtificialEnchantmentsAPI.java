package io.artificial.enchantments.api;

import io.artificial.enchantments.api.loot.LootModifierRegistry;
import io.artificial.enchantments.api.query.ItemEnchantmentQuery;
import io.artificial.enchantments.api.scaling.ScalingAlgorithmRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * The primary entry point for the Artificial Enchantments library.
 * 
 * <p>This interface provides methods for registering custom enchantments,
 * applying them to items, and querying enchantment state.
 *
 * <p>Registry and event bus internals are safe for concurrent access, but item
 * mutation and effect execution should still be treated as normal server-side
 * work. The public scheduler abstraction helps dependent plugins centralize
 * their own scheduling decisions.
 *
 * <p><strong>Native-First Storage Policy:</strong><br>
 * This library uses Bukkit's native ItemMeta as the source of truth for
 * enchantment state. NBT storage is reserved for auxiliary metadata only.
 * This design prevents state drift and ensures compatibility with vanilla
 * enchantment handling.
 *
 * <p><strong>Getting Started:</strong>
 * <pre>{@code
 * // In your plugin's onEnable()
 * ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.create(this);
 * 
 * // Or access the shared instance after bootstrap
 * ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.getInstance();
 * }</pre>
 *
 * @see EnchantmentDefinition
 * @see EnchantmentEventBus
 * @see ItemStorage
 * @since 0.1.0
 */
public interface ArtificialEnchantmentsAPI {

    /**
     * Creates or returns the shared API instance.
     * 
     * <p>The first caller initializes the library. Later calls return the same
     * shared instance and do not rebind ownership to the newer plugin.
     *
     * @param plugin the plugin requesting the API (must not be null)
     * @return the shared API instance
     * @since 0.1.0
     */
    @NotNull
    static ArtificialEnchantmentsAPI create(@NotNull Plugin plugin) {
        return io.artificial.enchantments.internal.ArtificialEnchantmentsAPIImpl.create(plugin);
    }

    /**
     * Gets the shared API instance after the library has been initialized.
     * 
     * <p>Use this to access the API after the bootstrap phase is complete.
     * This returns the same instance for all callers.
     *
     * @return the shared API instance
     * @throws IllegalStateException if the API has not been initialized
     * @since 0.1.0
     */
    @NotNull
    static ArtificialEnchantmentsAPI getInstance() {
        return io.artificial.enchantments.internal.ArtificialEnchantmentsAPIImpl.getInstance();
    }

    /**
     * Queues an enchantment definition for Paper's native registry bootstrap.
     *
     * <p>Paper custom enchantments must be known during the registry compose
     * lifecycle, before ordinary {@code JavaPlugin#onEnable()} methods run.
     * Dependent plugins that need client-visible native enchantments should call
     * this from their own {@code PluginBootstrap#bootstrap(...)} method.
     *
     * @param definition the enchantment definition to register natively
     * @throws IllegalStateException if Paper's native registry compose window has closed
     * @since 1.0.3
     */
    static void registerBootstrapEnchantment(@NotNull EnchantmentDefinition definition) {
        io.artificial.enchantments.internal.EnchantmentRegistryManager.getInstance().register(definition);
    }

    /**
     * Registers a new enchantment definition with the library.
     * 
     * <p>The enchantment becomes available for application to items immediately.
     * On Paper 1.21+, native client visibility requires the definition to be
     * queued during bootstrap via {@link #registerBootstrapEnchantment(EnchantmentDefinition)}.
     * Calling this after Paper's registry compose event has closed fails fast
     * because the native registry can no longer accept new enchantments.
     *
     * <p><strong>Thread Safety:</strong> This operation is thread-safe.
     *
     * @param definition the enchantment definition to register (must not be null)
     * @return this API instance for chaining
     * @throws IllegalArgumentException if definition is null or a duplicate key exists
     * @throws IllegalStateException if Paper's native registry compose window has closed
     * @since 0.1.0
     */
    @NotNull
    ArtificialEnchantmentsAPI registerEnchantment(@NotNull EnchantmentDefinition definition);

    /**
     * Unregisters an enchantment by its key.
     * 
     * <p>Removes the enchantment from the registry. Items already enchanted
     * with this enchantment retain their effects until the items are modified.
     *
     * <p><strong>Thread Safety:</strong> This operation is thread-safe.
     *
     * @param key the enchantment's namespaced key (must not be null)
     * @return true if an enchantment was removed, false if not found
     * @since 0.1.0
     */
    boolean unregisterEnchantment(@NotNull NamespacedKey key);

    /**
     * Gets an enchantment definition by its key.
     *
     * @param key the enchantment's namespaced key (must not be null)
     * @return the definition if found, empty otherwise
     * @since 0.1.0
     */
    @NotNull
    Optional<EnchantmentDefinition> getEnchantment(@NotNull NamespacedKey key);

    /**
     * Gets all registered enchantment definitions.
     *
     * @return an unmodifiable collection of all enchantments
     * @since 0.1.0
     */
    @NotNull
    Collection<EnchantmentDefinition> getAllEnchantments();

    /**
     * Gets all enchantments applicable to a specific material.
     *
     * @param material the material to check (must not be null)
     * @return a set of enchantments that can be applied to this material
     * @since 0.1.0
     */
    @NotNull
    Set<EnchantmentDefinition> getEnchantmentsFor(@NotNull org.bukkit.Material material);

    /**
     * Applies an enchantment to an item at the specified level.
     * 
     * <p>The item's ItemMeta is updated with the enchantment. The level must
     * be within the enchantment's defined min/max bounds.
     *
     * <p><strong>Native-First Policy:</strong> The enchantment is stored in
     * native ItemMeta, not duplicated in NBT.
     *
     * @param item the item to enchant (must not be null or air)
     * @param enchantment the enchantment to apply (must not be null)
     * @param level the level to apply (must be within bounds)
     * @return the enchanted item (may be same instance)
     * @throws IllegalArgumentException if parameters are invalid
     * @since 0.1.0
     */
    @NotNull
    ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment, int level);

    /**
     * Applies an enchantment by its registry key.
     * 
     * <p>Convenience method that looks up the definition before application.
     *
     * @param item the item to enchant (must not be null or air)
     * @param key the enchantment's namespaced key (must not be null)
     * @param level the level to apply
     * @return the enchanted item
     * @throws IllegalArgumentException if enchantment is not registered
     * @since 0.1.0
     */
    @NotNull
    ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key, int level);

    /**
     * Removes an enchantment from an item.
     *
     * @param item the item to modify (must not be null)
     * @param enchantment the enchantment to remove (must not be null)
     * @return the modified item
     * @since 0.1.0
     */
    @NotNull
    ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Removes an enchantment by its key.
     *
     * @param item the item to modify (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return the modified item
     * @since 0.1.0
     */
    @NotNull
    ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key);

    /**
     * Removes all artificial enchantments from an item.
     * 
     * <p>Vanilla enchantments are preserved. Only enchantments registered
     * through this API are removed.
     *
     * @param item the item to clear (must not be null)
     * @return the modified item
     * @since 0.1.0
     */
    @NotNull
    ItemStack removeAllEnchantments(@NotNull ItemStack item);

    /**
     * Gets the level of an enchantment on an item.
     *
     * @param item the item to check (must not be null)
     * @param enchantment the enchantment to look for (must not be null)
     * @return the enchantment level, or 0 if not present
     * @since 0.1.0
     */
    int getEnchantmentLevel(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Gets the level of an enchantment by its key.
     *
     * @param item the item to check (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return the enchantment level, or 0 if not present
     * @since 0.1.0
     */
    int getEnchantmentLevel(@NotNull ItemStack item, @NotNull NamespacedKey key);

    /**
     * Gets all artificial enchantments on an item.
     * 
     * <p>Returns a map of enchantment definitions to their levels. The map
     * is unmodifiable and represents a snapshot of the item's state.
     *
     * @param item the item to query (must not be null)
     * @return map of enchantments to levels (never null, may be empty)
     * @since 0.1.0
     */
    @NotNull
    java.util.Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack item);

    /**
     * Checks if an item has a specific enchantment.
     *
     * @param item the item to check (must not be null)
     * @param enchantment the enchantment to look for (must not be null)
     * @return true if the enchantment is present at any level
     * @since 0.1.0
     */
    boolean hasEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Checks if an item has an enchantment by its key.
     *
     * @param item the item to check (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return true if the enchantment is present at any level
     * @since 0.1.0
     */
    boolean hasEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key);

    /**
     * Gets the event bus for subscribing to enchantment effects.
     *
     * <p>Use this to listen for enchantment-triggered events without
     * implementing a full effect handler.
     *
     * @return the event bus instance
     * @since 0.1.0
     */
    @NotNull
    EnchantmentEventBus getEventBus();

    /**
     * Gets the item storage for direct access to item enchantment operations.
     *
     * <p>Use this for advanced item manipulation or when you need to perform
     * batch operations on items.
     *
     * @return the item storage instance
     * @since 0.1.0
     */
    @NotNull
    ItemStorage getItemStorage();

    /**
     * Gets the query facade for convenient enchantment lookups on items.
     *
     * <p>This provides a scoped, developer-friendly API for checking enchantments
     * without manual ItemMeta inspection. All methods are null-safe and return
     * sensible defaults for null inputs.
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * // Check if item has an enchantment
     * if (api.query().hasEnchantment(item, lifeSteal)) {
     *     int level = api.query().getLevel(item, lifeSteal);
     * }
     *
     * // Get all enchantments
     * Map<EnchantmentDefinition, Integer> enchantments = api.query().getAllEnchantments(item);
     *
     * // Check material applicability
     * Set<EnchantmentDefinition> applicable = api.query().getEnchantmentsFor(Material.DIAMOND_SWORD);
     * }</pre>
     *
     * @return the query facade instance
     * @since 0.2.0
     */
    @NotNull
    ItemEnchantmentQuery query();

    /**
     * Gets the plugin that owns this API instance.
     *
     * @return the owning plugin
     * @since 0.1.0
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Checks if running on a Folia server.
     *
     * @return true if Folia region threading is detected
     * @since 0.1.0
     */
    boolean isFolia();

    /**
     * Gets the library version string.
     *
     * @return the version in semantic format (e.g., "1.0.0")
     * @since 0.1.0
     */
    @NotNull
    String getVersion();

    /**
     * Gets the scaling algorithm registry.
     *
     * <p>Use this to register custom scaling algorithms or retrieve built-in ones.
     * The registry is pre-populated with all built-in algorithms on startup.
     *
     * @return the scaling algorithm registry instance
     * @since 0.2.0
     */
    @NotNull
    ScalingAlgorithmRegistry getScalingRegistry();

    /**
     * Gets the loot modifier registry for block-break loot modifications.
     *
     * <p>Use this registry to register {@link io.artificial.enchantments.api.loot.LootModifier}
     * instances that can modify drops when blocks are broken with enchanted tools.
     *
     * <p><strong>Explicit Ownership:</strong>
     * Only enchantments with explicitly registered modifiers affect loot drops.
     * Non-targeted loot remains untouched unless opted in.
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * LootModifierRegistry registry = api.getLootModifierRegistry();
     *
     * LootModifier myModifier = context -> {
     *     // Double drops based on level
     *     int multiplier = 1 + context.getLevel();
     *     for (ItemStack drop : context.getDrops()) {
     *         drop.setAmount(drop.getAmount() * multiplier);
     *     }
     * };
     *
     * registry.register(myEnchantment, myModifier);
     * }</pre>
     *
     * @return the loot modifier registry instance
     * @since 0.4.0
     */
    @NotNull
    LootModifierRegistry getLootModifierRegistry();

    /**
     * Gets the Folia-compatible scheduler for thread-safe task execution.
     *
     * <p>Use this scheduler to run tasks on the appropriate threads:
     * <ul>
     *   <li>Global region thread - for non-location-dependent operations</li>
     *   <li>Region thread - for location-dependent operations</li>
     *   <li>Entity scheduler - for entity-specific operations</li>
     * </ul>
     *
     * <p><strong>Usage Example:</strong>
     * <pre>{@code
     * // Run later on global region thread
     * api.getScheduler().runGlobalDelayed(plugin, () -> {
     *     // Task here
     * }, 20L);
     *
     * // Run at a specific location's region thread
     * api.getScheduler().runAtLocation(plugin, location, () -> {
     *     // Location-dependent task
     * });
     * }</pre>
     *
     * @return the scheduler instance
     * @since 0.2.0
     */
    @NotNull
    io.artificial.enchantments.internal.FoliaScheduler getScheduler();
}
