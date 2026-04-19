package io.artificial.enchantments.api;

import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Set;
import java.util.function.Function;

public interface EnchantmentDefinition {

    @NotNull
    NamespacedKey getKey();

    @NotNull
    Component getDisplayName();

    @Nullable
    Component getDescription();

    int getMinLevel();

    int getMaxLevel();

    @NotNull
    LevelScaling getScaling();

    @NotNull
    Set<Material> getApplicableMaterials();

    boolean isApplicableTo(@NotNull Material material);

    boolean isApplicableTo(@NotNull ItemStack item);

    boolean isCurse();

    boolean isTradeable();

    boolean isDiscoverable();

    @NotNull
    Rarity getRarity();

    @Nullable
    EnchantmentEffectHandler getEffectHandler();

    double calculateScaledValue(int level);

    boolean conflictsWith(@NotNull EnchantmentDefinition other);

    @NotNull
    Set<NamespacedKey> getConflictingEnchantments();

    enum Rarity {
        COMMON,
        UNCOMMON,
        RARE,
        VERY_RARE
    }

    interface Builder {
        @NotNull
        Builder key(@NotNull NamespacedKey key);
        @NotNull
        Builder displayName(@NotNull Component name);
        @NotNull
        Builder description(@Nullable Component description);
        @NotNull
        Builder minLevel(int minLevel);
        @NotNull
        Builder maxLevel(int maxLevel);
        @NotNull
        Builder scaling(@NotNull LevelScaling scaling);
        @NotNull
        Builder scaling(@NotNull Function<Integer, Double> formula);
        @NotNull
        Builder applicable(@NotNull Material... materials);
        @NotNull
        Builder applicable(@NotNull Set<Material> materials);
        @NotNull
        Builder curse(boolean curse);
        @NotNull
        Builder curse();
        @NotNull
        Builder tradeable(boolean tradeable);
        @NotNull
        Builder discoverable(boolean discoverable);
        @NotNull
        Builder rarity(@NotNull Rarity rarity);
        @NotNull
        Builder effectHandler(@Nullable EnchantmentEffectHandler handler);
        @NotNull
        Builder conflictsWith(@NotNull NamespacedKey key);
        @NotNull
        Builder conflictsWith(@NotNull NamespacedKey... keys);
        @NotNull
        EnchantmentDefinition build();
    }

    @NotNull
    static Builder builder() {
        throw new UnsupportedOperationException("Implementation not available in API module");
    }
}
