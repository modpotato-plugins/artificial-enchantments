package io.artificial.enchantments.api.query;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * A scoped, developer-friendly API for querying enchantments on items.
 * 
 * <p>This facade provides convenient methods for checking enchantments on items
 * without manual ItemMeta inspection. It wraps {@link io.artificial.enchantments.api.ItemStorage}
 * and provides a more ergonomic API that feels local to the item.
 * 
 * <p><strong>Null Safety:</strong> All methods in this interface are null-safe.
 * Passing a null {@link ItemStack} will return false (for boolean methods),
 * 0 (for level methods), or empty collections (for collection-returning methods).
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Single enchantment check
 * if (api.query().hasEnchantment(item, lifeSteal)) {
 *     int level = api.query().getLevel(item, lifeSteal);
 * }
 * 
 * // Check by key
 * if (api.query().hasEnchantment(item, new NamespacedKey(plugin, "life_steal"))) {
 *     // Enchantment is present
 * }
 * 
 * // Get all enchantments
 * Map<EnchantmentDefinition, Integer> enchantments = api.query().getAllEnchantments(item);
 * 
 * // Check if any enchantment exists
 * if (api.query().isEnchanted(item)) {
 *     // Item has at least one custom enchantment
 * }
 * 
 * // Get applicable enchantments for a material
 * Set<EnchantmentDefinition> applicable = api.query().getEnchantmentsFor(Material.DIAMOND_SWORD);
 * }</pre>
 * 
 * @see io.artificial.enchantments.api.ArtificialEnchantmentsAPI#query()
 * @since 0.2.0
 */
public interface ItemEnchantmentQuery {

    /**
     * Checks if an item has a specific enchantment.
     * 
     * <p>Returns false if the item is null or the enchantment is not present.
     *
     * @param item the item to check (null-safe)
     * @param enchantment the enchantment to look for (must not be null)
     * @return true if the item has the enchantment at any level
     * @throws IllegalArgumentException if enchantment is null
     * @since 0.2.0
     */
    boolean hasEnchantment(@Nullable ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Checks if an item has an enchantment by its registry key.
     * 
     * <p>Returns false if the item is null, the key is not found in the registry,
     * or the enchantment is not present on the item.
     *
     * @param item the item to check (null-safe)
     * @param key the enchantment's namespaced key (must not be null)
     * @return true if the item has the enchantment at any level
     * @throws IllegalArgumentException if key is null
     * @since 0.2.0
     */
    boolean hasEnchantment(@Nullable ItemStack item, @NotNull NamespacedKey key);

    /**
     * Gets the level of a specific enchantment on an item.
     * 
     * <p>Returns 0 if the item is null, doesn't have the enchantment, or the
     * enchantment is not registered.
     *
     * @param item the item to query (null-safe)
     * @param enchantment the enchantment to check (must not be null)
     * @return the enchantment level, or 0 if not present
     * @throws IllegalArgumentException if enchantment is null
     * @since 0.2.0
     */
    int getLevel(@Nullable ItemStack item, @NotNull EnchantmentDefinition enchantment);

    /**
     * Gets the level of an enchantment by its registry key.
     * 
     * <p>Returns 0 if the item is null, the enchantment is not found in the
     * registry, or the enchantment is not present on the item.
     *
     * @param item the item to query (null-safe)
     * @param key the enchantment's namespaced key (must not be null)
     * @return the enchantment level, or 0 if not present
     * @throws IllegalArgumentException if key is null
     * @since 0.2.0
     */
    int getLevel(@Nullable ItemStack item, @NotNull NamespacedKey key);

    /**
     * Gets all artificial enchantments on an item.
     * 
     * <p>Returns a map of enchantment definitions to their levels. Only
     * enchantments registered through this API are included; vanilla
     * enchantments are excluded.
     * 
     * <p>The returned map is unmodifiable and represents a snapshot of the
     * item's state at the time of the call. Returns an empty map if the item
     * is null.
     *
     * @param item the item to query (null-safe)
     * @return an unmodifiable map of enchantments to levels (never null)
     * @since 0.2.0
     */
    @NotNull
    Map<EnchantmentDefinition, Integer> getAllEnchantments(@Nullable ItemStack item);

    /**
     * Checks if an item has any custom enchantment.
     * 
     * <p>Returns true if the item has at least one artificial enchantment
     * registered through this API. Returns false if the item is null or has
     * no custom enchantments.
     *
     * @param item the item to check (null-safe)
     * @return true if the item has any custom enchantment
     * @since 0.2.0
     */
    boolean isEnchanted(@Nullable ItemStack item);

    /**
     * Gets all enchantments that can be applied to a specific material.
     * 
     * <p>Returns a set of all registered enchantments that are applicable to
     * the given material. This is useful for enchanting interfaces or checking
     * what enchantments are available for a tool type.
     * 
     * <p>The returned set is unmodifiable. Returns an empty set if the material
     * is null or no enchantments are applicable.
     *
     * @param material the material to check (null-safe)
     * @return an unmodifiable set of applicable enchantments (never null)
     * @since 0.2.0
     */
    @NotNull
    Set<EnchantmentDefinition> getEnchantmentsFor(@Nullable Material material);

    /**
     * Checks if multiple enchantments are present on an item.
     * 
     * <p>Returns true only if ALL specified enchantments are present on the item.
     * Returns false if the item is null, the enchantments array is empty, or
     * any enchantment is missing.
     *
     * @param item the item to check (null-safe)
     * @param enchantments the enchantments to look for (must not be null or empty)
     * @return true if all enchantments are present
     * @throws IllegalArgumentException if enchantments is null or empty
     * @since 0.2.0
     */
    boolean hasAllEnchantments(@Nullable ItemStack item, @NotNull EnchantmentDefinition... enchantments);

    /**
     * Checks if any of the specified enchantments are present on an item.
     * 
     * <p>Returns true if AT LEAST ONE of the specified enchantments is present
     * on the item. Returns false if the item is null, the enchantments array
     * is empty, or none of the enchantments are present.
     *
     * @param item the item to check (null-safe)
     * @param enchantments the enchantments to look for (must not be null or empty)
     * @return true if any enchantment is present
     * @throws IllegalArgumentException if enchantments is null or empty
     * @since 0.2.0
     */
    boolean hasAnyEnchantment(@Nullable ItemStack item, @NotNull EnchantmentDefinition... enchantments);

    /**
     * Gets the total level of all enchantments on an item.
     * 
     * <p>Returns the sum of all enchantment levels. Returns 0 if the item is null
     * or has no enchantments.
     *
     * @param item the item to query (null-safe)
     * @return the sum of all enchantment levels
     * @since 0.2.0
     */
    int getTotalEnchantmentLevel(@Nullable ItemStack item);

    /**
     * Gets the highest enchantment level on an item.
     * 
     * <p>Returns the maximum level among all enchantments on the item.
     * Returns 0 if the item is null or has no enchantments.
     *
     * @param item the item to query (null-safe)
     * @return the highest enchantment level, or 0 if none
     * @since 0.2.0
     */
    int getHighestLevel(@Nullable ItemStack item);
}
