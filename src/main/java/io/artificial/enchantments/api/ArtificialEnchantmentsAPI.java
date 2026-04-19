package io.artificial.enchantments.api;

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
 * applying them to items, and querying enchantment state. All operations are
 * thread-safe and Folia-compatible.
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
     * Creates a new API instance bound to the specified plugin.
     * 
     * <p>This method initializes the library for use by your plugin. Each
     * plugin should call this once during {@code onEnable()}.
     *
     * <p><strong>Implementation Note:</strong> This method throws in the API
     * module because the actual implementation is provided by the core
     * library at runtime.
     *
     * @param plugin the plugin requesting the API (must not be null)
     * @return a new API instance bound to the plugin
     * @throws UnsupportedOperationException in the API module
     * @since 0.1.0
     */
    static ArtificialEnchantmentsAPI create(@NotNull Plugin plugin) {
        throw new UnsupportedOperationException("Implementation not available in API module");
    }

    /**
     * Gets the shared API instance after the library has been initialized.
     * 
     * <p>Use this to access the API after the bootstrap phase is complete.
     * This returns the same instance for all callers.
     *
     * <p><strong>Implementation Note:</strong> This method throws in the API
     * module because the actual implementation is provided by the core
     * library at runtime.
     *
     * @return the shared API instance
     * @throws UnsupportedOperationException in the API module
     * @since 0.1.0
     */
    static ArtificialEnchantmentsAPI getInstance() {
        throw new UnsupportedOperationException("Implementation not available in API module");
    }

    /**
     * Registers a new enchantment definition with the library.
     * 
     * <p>The enchantment becomes available for application to items immediately.
     * On Paper 1.21+, the enchantment is also registered with the native
     * registry for client visibility.
     *
     * <p><strong>Thread Safety:</strong> This operation is thread-safe.
     *
     * @param definition the enchantment definition to register (must not be null)
     * @return this API instance for chaining
     * @throws IllegalArgumentException if definition is null or a duplicate key exists
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
}
