package io.artificial.enchantments.internal.query;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.query.ItemEnchantmentQuery;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of {@link ItemEnchantmentQuery} that delegates to
 * {@link ItemStorage} and {@link EnchantmentRegistryManager}.
 * 
 * <p>This implementation follows the facade pattern - it provides a more
 * ergonomic API while delegating all actual operations to existing storage
 * and registry components.
 * 
 * <p><strong>Null Safety:</strong> All methods handle null inputs gracefully
 * as specified by the interface contract.
 *
 * @since 0.2.0
 */
public class ItemEnchantmentQueryImpl implements ItemEnchantmentQuery {

    private final ItemStorage itemStorage;
    private final EnchantmentRegistryManager registryManager;

    /**
     * Creates a new query facade.
     *
     * @param itemStorage the item storage to delegate to (must not be null)
     * @param registryManager the registry manager for enchantment lookups (must not be null)
     */
    public ItemEnchantmentQueryImpl(
            @NotNull ItemStorage itemStorage,
            @NotNull EnchantmentRegistryManager registryManager) {
        this.itemStorage = Objects.requireNonNull(itemStorage, "itemStorage cannot be null");
        this.registryManager = Objects.requireNonNull(registryManager, "registryManager cannot be null");
    }

    @Override
    public boolean hasEnchantment(@Nullable ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");
        if (item == null) {
            return false;
        }
        return itemStorage.hasEnchantment(item, enchantment);
    }

    @Override
    public boolean hasEnchantment(@Nullable ItemStack item, @NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        if (item == null) {
            return false;
        }
        return itemStorage.hasEnchantment(item, key);
    }

    @Override
    public int getLevel(@Nullable ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");
        if (item == null) {
            return 0;
        }
        return itemStorage.getEnchantmentLevel(item, enchantment);
    }

    @Override
    public int getLevel(@Nullable ItemStack item, @NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        if (item == null) {
            return 0;
        }
        return itemStorage.getEnchantmentLevel(item, key);
    }

    @Override
    @NotNull
    public Map<EnchantmentDefinition, Integer> getAllEnchantments(@Nullable ItemStack item) {
        if (item == null) {
            return Collections.emptyMap();
        }
        return itemStorage.getEnchantments(item);
    }

    @Override
    public boolean isEnchanted(@Nullable ItemStack item) {
        if (item == null) {
            return false;
        }
        return !itemStorage.getEnchantments(item).isEmpty();
    }

    @Override
    @NotNull
    public Set<EnchantmentDefinition> getEnchantmentsFor(@Nullable Material material) {
        if (material == null) {
            return Collections.emptySet();
        }
        return registryManager.getForMaterial(material);
    }

    @Override
    public boolean hasAllEnchantments(@Nullable ItemStack item, @NotNull EnchantmentDefinition... enchantments) {
        Objects.requireNonNull(enchantments, "enchantments cannot be null");
        if (enchantments.length == 0) {
            throw new IllegalArgumentException("enchantments array cannot be empty");
        }
        if (item == null) {
            return false;
        }

        for (EnchantmentDefinition enchantment : enchantments) {
            if (!itemStorage.hasEnchantment(item, enchantment)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasAnyEnchantment(@Nullable ItemStack item, @NotNull EnchantmentDefinition... enchantments) {
        Objects.requireNonNull(enchantments, "enchantments cannot be null");
        if (enchantments.length == 0) {
            throw new IllegalArgumentException("enchantments array cannot be empty");
        }
        if (item == null) {
            return false;
        }

        for (EnchantmentDefinition enchantment : enchantments) {
            if (itemStorage.hasEnchantment(item, enchantment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTotalEnchantmentLevel(@Nullable ItemStack item) {
        if (item == null) {
            return 0;
        }

        int total = 0;
        for (int level : itemStorage.getEnchantments(item).values()) {
            total += level;
        }
        return total;
    }

    @Override
    public int getHighestLevel(@Nullable ItemStack item) {
        if (item == null) {
            return 0;
        }

        int highest = 0;
        for (int level : itemStorage.getEnchantments(item).values()) {
            if (level > highest) {
                highest = level;
            }
        }
        return highest;
    }
}
