package io.artificial.enchantments.api;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ArtificialEnchantmentsAPI {

    static ArtificialEnchantmentsAPI create(@NotNull Plugin plugin) {
        throw new UnsupportedOperationException("Implementation not available in API module");
    }

    static ArtificialEnchantmentsAPI getInstance() {
        throw new UnsupportedOperationException("Implementation not available in API module");
    }

    @NotNull
    ArtificialEnchantmentsAPI registerEnchantment(@NotNull EnchantmentDefinition definition);

    boolean unregisterEnchantment(@NotNull NamespacedKey key);

    @NotNull
    Optional<EnchantmentDefinition> getEnchantment(@NotNull NamespacedKey key);

    @NotNull
    Collection<EnchantmentDefinition> getAllEnchantments();

    @NotNull
    Set<EnchantmentDefinition> getEnchantmentsFor(@NotNull org.bukkit.Material material);

    @NotNull
    ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment, int level);

    @NotNull
    ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key, int level);

    @NotNull
    ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    @NotNull
    ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key);

    @NotNull
    ItemStack removeAllEnchantments(@NotNull ItemStack item);

    int getEnchantmentLevel(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    int getEnchantmentLevel(@NotNull ItemStack item, @NotNull NamespacedKey key);

    @NotNull
    java.util.Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack item);

    boolean hasEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment);

    boolean hasEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key);

    @NotNull
    EnchantmentEventBus getEventBus();

    @NotNull
    Plugin getPlugin();

    boolean isFolia();

    @NotNull
    String getVersion();
}
