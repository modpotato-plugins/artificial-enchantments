package io.artificial.enchantments.api;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Abstraction for storing and retrieving enchantment data on items.
 * <p>
 * Implementations must follow the native-first storage policy:
 * <ul>
 *   <li>Native ItemMeta is the source of truth for enchantment state</li>
 *   <li>NBT is used only for auxiliary metadata (display flags, compatibility markers)</li>
 *   <li>No duplication of enchant state between native and NBT stores</li>
 * </ul>
 *
 * <p>This interface provides atomic operations for enchantment mutation to prevent
 * state drift between the native ItemMeta and any auxiliary storage.
 *
 * @see EnchantmentDefinition
 * @since 0.1.0
 */
public interface ItemStorage {

    /**
     * Applies an enchantment to an item at the specified level.
     * <p>
     * The enchantment level is validated against the definition's bounds before
     * application. If the level is outside the valid range, the operation fails.
     * <p>
     * This operation is atomic - either the enchantment is fully applied with
     * all auxiliary metadata updated, or the item remains unchanged.
     *
     * @param item the item to enchant (must not be null or air)
     * @param enchantment the enchantment definition to apply (must not be null)
     * @param level the level to apply (must be within enchantment's min/max bounds)
     * @return the enchanted item (may be the same instance if item is mutable)
     * @throws IllegalArgumentException if item is null, air, or level is invalid
     * @throws IllegalStateException if the enchantment conflicts with existing ones
     *         and conflict resolution fails
     */
    @NotNull
    ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment, int level);

    /**
     * Applies an enchantment by its registry key.
     * <p>
     * This is a convenience method that looks up the definition in the registry
     * before delegating to {@link #applyEnchantment(ItemStack, EnchantmentDefinition, int)}.
     *
     * @param item the item to enchant (must not be null or air)
     * @param key the enchantment's namespaced key (must not be null)
     * @param level the level to apply
     * @return the enchanted item
     * @throws IllegalArgumentException if the enchantment is not registered
     * @see #applyEnchantment(ItemStack, EnchantmentDefinition, int)
     */
    @NotNull
    ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key, int level);

    /**
     * Removes an enchantment from an item.
     * <p>
     * If the item does not have the specified enchantment, this operation is a
     * no-op and returns the item unchanged.
     * <p>
     * Auxiliary metadata associated with this enchantment is also cleaned up.
     *
     * @param item the item to modify (must not be null)
     * @param enchantment the enchantment to remove (must not be null)
     * @return the modified item (may be the same instance if item is mutable)
     * @throws IllegalArgumentException if item or enchantment is null
     */
    @NotNull
    ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Removes an enchantment by its registry key.
     *
     * @param item the item to modify (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return the modified item
     * @throws IllegalArgumentException if the enchantment is not registered
     * @see #removeEnchantment(ItemStack, EnchantmentDefinition)
     */
    @NotNull
    ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key);

    /**
     * Removes all artificial enchantments from an item.
     * <p>
     * This removes all enchantments registered through this API while preserving
     * vanilla enchantments and the item's base properties (durability, lore, etc).
     *
     * @param item the item to clear (must not be null)
     * @return the modified item
     * @throws IllegalArgumentException if item is null
     */
    @NotNull
    ItemStack removeAllEnchantments(@NotNull ItemStack item);

    /**
     * Gets the level of a specific enchantment on an item.
     *
     * @param item the item to query (must not be null)
     * @param enchantment the enchantment to check (must not be null)
     * @return the enchantment level, or 0 if not present
     * @throws IllegalArgumentException if item or enchantment is null
     */
    int getEnchantmentLevel(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Gets the level of an enchantment by its registry key.
     *
     * @param item the item to query (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return the enchantment level, or 0 if not present or not registered
     * @throws IllegalArgumentException if item or key is null
     */
    int getEnchantmentLevel(@NotNull ItemStack item, @NotNull NamespacedKey key);

    /**
     * Gets all artificial enchantments on an item.
     * <p>
     * Returns a map of enchantment definitions to their levels. Only enchantments
     * registered through this API are included; vanilla enchantments are excluded.
     * <p>
     * The returned map is unmodifiable and represents a snapshot of the item's
     * state at the time of the call.
     *
     * @param item the item to query (must not be null)
     * @return an unmodifiable map of enchantments to levels (never null, may be empty)
     * @throws IllegalArgumentException if item is null
     */
    @NotNull
    Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack item);

    /**
     * Gets all artificial enchantment keys on an item as NamespacedKey to level mapping.
     * <p>
     * This is a lower-level alternative to {@link #getEnchantments(ItemStack)} that
     * returns raw keys rather than resolved definitions. Useful when definitions may
     * not be available (e.g., cross-plugin compatibility).
     *
     * @param item the item to query (must not be null)
     * @return an unmodifiable map of enchantment keys to levels
     * @throws IllegalArgumentException if item is null
     */
    @NotNull
    Map<NamespacedKey, Integer> getEnchantmentKeys(@NotNull ItemStack item);

    /**
     * Checks if an item has a specific enchantment.
     *
     * @param item the item to check (must not be null)
     * @param enchantment the enchantment to look for (must not be null)
     * @return true if the item has the enchantment at any level
     * @throws IllegalArgumentException if item or enchantment is null
     */
    boolean hasEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Checks if an item has an enchantment by its registry key.
     *
     * @param item the item to check (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return true if the item has the enchantment at any level
     * @throws IllegalArgumentException if item or key is null
     */
    boolean hasEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key);

    /**
     * Gets the names of all auxiliary metadata keys stored for this item.
     * <p>
     * Auxiliary metadata is stored in NBT and used for compatibility markers,
     * display flags, and other non-enchantment data.
     *
     * @param item the item to query (must not be null)
     * @return a set of auxiliary metadata keys (never null, may be empty)
     * @throws IllegalArgumentException if item is null
     */
    @NotNull
    Set<String> getAuxiliaryMetadataKeys(@NotNull ItemStack item);

    /**
     * Sets an auxiliary metadata string value.
     * <p>
     * This stores data in NBT that is not part of the core enchantment state.
     * Use this for compatibility markers, display configuration, etc.
     *
     * @param item the item to modify (must not be null)
     * @param key the metadata key (must not be null or empty)
     * @param value the value to store, or null to remove
     * @return the modified item
     * @throws IllegalArgumentException if item or key is null/invalid
     */
    @NotNull
    ItemStack setAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key, @Nullable String value);

    /**
     * Gets an auxiliary metadata string value.
     *
     * @param item the item to query (must not be null)
     * @param key the metadata key (must not be null)
     * @return the stored value, or null if not present
     * @throws IllegalArgumentException if item or key is null
     */
    @Nullable
    String getAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key);

    /**
     * Checks if auxiliary metadata with the given key exists.
     *
     * @param item the item to query (must not be null)
     * @param key the metadata key (must not be null)
     * @return true if metadata exists for the key
     * @throws IllegalArgumentException if item or key is null
     */
    boolean hasAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key);

    /**
     * Clears all auxiliary metadata from an item.
     * <p>
     * This does not affect native enchantment state, only the NBT auxiliary store.
     *
     * @param item the item to modify (must not be null)
     * @return the modified item
     * @throws IllegalArgumentException if item is null
     */
    @NotNull
    ItemStack clearAuxiliaryMetadata(@NotNull ItemStack item);
}
