package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service for mutating item enchantment state.
 * <p>
 * This is the primary implementation of item mutation operations for the
 * Artificial Enchantments library. It coordinates between the native ItemMeta
 * (source of truth) and auxiliary NBT metadata storage.
 * <p>
 * All operations are atomic - either fully succeed or leave the item unchanged.
 * The native ItemMeta is always the source of truth for enchantment state;
 * NBT is used only for auxiliary metadata like display flags.
 *
 * @since 0.1.0
 */
public class ItemEnchantmentService {

    private final ItemStorage itemStorage;
    private final EnchantmentRegistryManager registryManager;

    public ItemEnchantmentService(
            @NotNull ItemStorage itemStorage,
            @NotNull EnchantmentRegistryManager registryManager) {
        this.itemStorage = Objects.requireNonNull(itemStorage, "itemStorage cannot be null");
        this.registryManager = Objects.requireNonNull(registryManager, "registryManager cannot be null");
    }

    /**
     * Applies an enchantment to an item at the specified level.
     * <p>
     * Validates the level against the enchantment's min/max bounds before
     * application. Returns the enchanted item (may be a new instance).
     *
     * @param item the item to enchant
     * @param enchantment the enchantment to apply
     * @param level the enchantment level
     * @return the enchanted item
     * @throws IllegalArgumentException if item is null, air, level invalid, or enchantment not applicable
     * @throws IllegalStateException if the enchantment is not registered
     */
    @NotNull
    public ItemStack applyEnchantment(
            @NotNull ItemStack item,
            @NotNull EnchantmentDefinition enchantment,
            int level) {
        return itemStorage.applyEnchantment(item, enchantment, level);
    }

    /**
     * Applies an enchantment by its registry key.
     *
     * @param item the item to enchant
     * @param key the enchantment's namespaced key
     * @param level the enchantment level
     * @return the enchanted item
     * @throws IllegalArgumentException if enchantment not registered or invalid parameters
     */
    @NotNull
    public ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key, int level) {
        return itemStorage.applyEnchantment(item, key, level);
    }

    /**
     * Removes an enchantment from an item.
     * <p>
     * If the item does not have the enchantment, returns the item unchanged.
     *
     * @param item the item to modify
     * @param enchantment the enchantment to remove
     * @return the modified item
     */
    @NotNull
    public ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        return itemStorage.removeEnchantment(item, enchantment);
    }

    /**
     * Removes an enchantment by its registry key.
     *
     * @param item the item to modify
     * @param key the enchantment's namespaced key
     * @return the modified item
     */
    @NotNull
    public ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return itemStorage.removeEnchantment(item, key);
    }

    /**
     * Removes all artificial enchantments from an item.
     * <p>
     * Preserves vanilla enchantments and the item's base properties.
     *
     * @param item the item to clear
     * @return the modified item
     */
    @NotNull
    public ItemStack removeAllEnchantments(@NotNull ItemStack item) {
        return itemStorage.removeAllEnchantments(item);
    }

    /**
     * Gets the level of a specific enchantment on an item.
     *
     * @param item the item to query
     * @param enchantment the enchantment to check
     * @return the enchantment level, or 0 if not present
     */
    public int getEnchantmentLevel(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        return itemStorage.getEnchantmentLevel(item, enchantment);
    }

    /**
     * Gets the level of an enchantment by its registry key.
     *
     * @param item the item to query
     * @param key the enchantment's namespaced key
     * @return the enchantment level, or 0 if not present or not registered
     */
    public int getEnchantmentLevel(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return itemStorage.getEnchantmentLevel(item, key);
    }

    /**
     * Gets all artificial enchantments on an item.
     * <p>
     * Returns a map of enchantment definitions to their levels.
     * Only enchantments registered through this API are included.
     *
     * @param item the item to query
     * @return unmodifiable map of enchantments to levels (never null)
     */
    @NotNull
    public Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack item) {
        return itemStorage.getEnchantments(item);
    }

    /**
     * Gets all artificial enchantment keys on an item.
     * <p>
     * Lower-level alternative that returns raw keys rather than definitions.
     *
     * @param item the item to query
     * @return unmodifiable map of keys to levels (never null)
     */
    @NotNull
    public Map<NamespacedKey, Integer> getEnchantmentKeys(@NotNull ItemStack item) {
        return itemStorage.getEnchantmentKeys(item);
    }

    /**
     * Checks if an item has a specific enchantment.
     *
     * @param item the item to check
     * @param enchantment the enchantment to look for
     * @return true if the item has the enchantment at any level
     */
    public boolean hasEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        return itemStorage.hasEnchantment(item, enchantment);
    }

    /**
     * Checks if an item has an enchantment by its registry key.
     *
     * @param item the item to check
     * @param key the enchantment's namespaced key
     * @return true if the item has the enchantment
     */
    public boolean hasEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return itemStorage.hasEnchantment(item, key);
    }

    /**
     * Gets all enchantments applicable to a material.
     *
     * @param material the material to check
     * @return unmodifiable set of applicable enchantment definitions
     */
    @NotNull
    public Set<EnchantmentDefinition> getEnchantmentsForMaterial(@NotNull Material material) {
        Objects.requireNonNull(material, "material cannot be null");
        return registryManager.getForMaterial(material);
    }

    /**
     * Gets all registered enchantment definitions.
     *
     * @return unmodifiable collection of all enchantments
     */
    @NotNull
    public Collection<EnchantmentDefinition> getAllEnchantments() {
        return registryManager.getAll();
    }

    /**
     * Gets an enchantment definition by key.
     *
     * @param key the enchantment's namespaced key
     * @return the definition, or empty if not registered
     */
    @NotNull
    public java.util.Optional<EnchantmentDefinition> getEnchantment(@NotNull NamespacedKey key) {
        return registryManager.get(key);
    }

    /**
     * Registers a new enchantment definition.
     *
     * @param definition the enchantment to register
     * @return the registered definition
     * @throws IllegalArgumentException if already registered or invalid
     * @throws IllegalStateException if registry is frozen
     */
    @NotNull
    public EnchantmentDefinition registerEnchantment(@NotNull EnchantmentDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");
        registryManager.register(definition);
        return definition;
    }

    /**
     * Unregisters an enchantment.
     *
     * @param key the enchantment's namespaced key
     * @return true if removed, false if not found
     * @throws IllegalStateException if registry is frozen
     */
    public boolean unregisterEnchantment(@NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        return registryManager.unregister(key);
    }

    /**
     * Sets auxiliary metadata on an item.
     * <p>
     * This stores non-enchantment data in NBT for compatibility or display.
     *
     * @param item the item to modify
     * @param key the metadata key
     * @param value the value to store
     * @return the modified item
     */
    @NotNull
    public ItemStack setAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key, String value) {
        return itemStorage.setAuxiliaryMetadata(item, key, value);
    }

    /**
     * Gets auxiliary metadata from an item.
     *
     * @param item the item to query
     * @param key the metadata key
     * @return the stored value, or null if not present
     */
    public String getAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key) {
        return itemStorage.getAuxiliaryMetadata(item, key);
    }
}
