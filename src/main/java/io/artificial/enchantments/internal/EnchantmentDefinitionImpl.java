package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable implementation of EnchantmentDefinition.
 *
 * <p>This class is instantiated by EnchantmentDefinitionBuilder and provides
 * thread-safe, immutable access to all enchantment properties.
 *
 * @since 0.2.0
 */
public final class EnchantmentDefinitionImpl implements EnchantmentDefinition {

    private final NamespacedKey key;
    private final Component displayName;
    private final Component description;
    private final int minLevel;
    private final int maxLevel;
    private final LevelScaling scaling;
    private final Set<Material> applicableMaterials;
    private final boolean curse;
    private final boolean tradeable;
    private final boolean discoverable;
    private final Rarity rarity;
    private final EnchantmentEffectHandler effectHandler;
    private final Set<NamespacedKey> conflicts;

    EnchantmentDefinitionImpl(
        @NotNull NamespacedKey key,
        @NotNull Component displayName,
        @Nullable Component description,
        int minLevel,
        int maxLevel,
        @NotNull LevelScaling scaling,
        @NotNull Set<Material> applicableMaterials,
        boolean curse,
        boolean tradeable,
        boolean discoverable,
        @NotNull Rarity rarity,
        @Nullable EnchantmentEffectHandler effectHandler,
        @NotNull Set<NamespacedKey> conflicts
    ) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.scaling = scaling;
        this.applicableMaterials = applicableMaterials;
        this.curse = curse;
        this.tradeable = tradeable;
        this.discoverable = discoverable;
        this.rarity = rarity;
        this.effectHandler = effectHandler;
        this.conflicts = conflicts;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return key;
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    @Nullable
    public Component getDescription() {
        return description;
    }

    @Override
    public int getMinLevel() {
        return minLevel;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    @NotNull
    public LevelScaling getScaling() {
        return scaling;
    }

    @Override
    @NotNull
    public Set<Material> getApplicableMaterials() {
        return applicableMaterials;
    }

    @Override
    public boolean isApplicableTo(@NotNull Material material) {
        return applicableMaterials.contains(material);
    }

    @Override
    public boolean isApplicableTo(@NotNull ItemStack item) {
        return applicableMaterials.contains(item.getType());
    }

    @Override
    public boolean isCurse() {
        return curse;
    }

    @Override
    public boolean isTradeable() {
        return tradeable;
    }

    @Override
    public boolean isDiscoverable() {
        return discoverable;
    }

    @Override
    @NotNull
    public Rarity getRarity() {
        return rarity;
    }

    @Override
    @Nullable
    public EnchantmentEffectHandler getEffectHandler() {
        return effectHandler;
    }

    @Override
    public double calculateScaledValue(int level) {
        if (level < minLevel || level > maxLevel) {
            throw new IllegalArgumentException(
                "Level " + level + " is out of bounds [" + minLevel + ", " + maxLevel + "]"
            );
        }
        return scaling.calculate(level);
    }

    @Override
    public boolean conflictsWith(@NotNull EnchantmentDefinition other) {
        return conflicts.contains(other.getKey());
    }

    @Override
    @NotNull
    public Set<NamespacedKey> getConflictingEnchantments() {
        return conflicts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnchantmentDefinition)) return false;
        EnchantmentDefinition that = (EnchantmentDefinition) o;
        return Objects.equals(key, that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "EnchantmentDefinitionImpl{key=" + key + ", displayName=" + displayName + "}";
    }
}
