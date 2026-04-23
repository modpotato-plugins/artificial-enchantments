package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.scaling.LevelScaling;
import io.artificial.enchantments.api.scaling.ScalingAlgorithmRegistry;
import io.artificial.enchantments.internal.scaling.ScalingRegistryHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Internal implementation of the EnchantmentDefinition.Builder interface.
 *
 * <p>This builder provides comprehensive validation for enchantment definitions
 * including level bounds, material sets, conflicts, and cross-property validation.
 * It creates immutable EnchantmentDefinitionImpl instances.
 *
 * @since 0.2.0
 */
public final class EnchantmentDefinitionBuilder implements EnchantmentDefinition.Builder {

    private NamespacedKey key;
    private Component displayName;
    private LevelScaling scaling;
    private final Set<Material> applicableMaterials = new HashSet<>();

    private Component description;
    private int minLevel = 1;
    private int maxLevel = 5;
    private boolean curse = false;
    private boolean tradeable = true;
    private boolean discoverable = true;
    private EnchantmentDefinition.Rarity rarity = EnchantmentDefinition.Rarity.COMMON;
    private EnchantmentEffectHandler effectHandler;
    private final Set<NamespacedKey> conflicts = new HashSet<>();

    @Override
    @NotNull
    public EnchantmentDefinition.Builder key(@NotNull NamespacedKey key) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder displayName(@NotNull Component name) {
        this.displayName = Objects.requireNonNull(name, "displayName must not be null");
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder description(@Nullable Component description) {
        this.description = description;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder minLevel(int minLevel) {
        this.minLevel = minLevel;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder maxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder scaling(@NotNull LevelScaling scaling) {
        this.scaling = Objects.requireNonNull(scaling, "scaling must not be null");
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder scaling(@NotNull Function<Integer, Double> formula) {
        Objects.requireNonNull(formula, "formula must not be null");
        this.scaling = LevelScaling.of(formula);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder scaling(@NotNull String algorithmName, double... params) {
        Objects.requireNonNull(algorithmName, "algorithmName must not be null");
        this.scaling = ScalingRegistryHolder.getRegistry().get(algorithmName, params);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder applicable(@NotNull Material... materials) {
        Objects.requireNonNull(materials, "materials must not be null");
        for (Material material : materials) {
            if (material != null) {
                this.applicableMaterials.add(material);
            }
        }
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder applicable(@NotNull Set<Material> materials) {
        Objects.requireNonNull(materials, "materials must not be null");
        this.applicableMaterials.addAll(materials);
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder curse(boolean curse) {
        this.curse = curse;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder curse() {
        this.curse = true;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder tradeable(boolean tradeable) {
        this.tradeable = tradeable;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder discoverable(boolean discoverable) {
        this.discoverable = discoverable;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder rarity(@NotNull EnchantmentDefinition.Rarity rarity) {
        this.rarity = Objects.requireNonNull(rarity, "rarity must not be null");
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder effectHandler(@Nullable EnchantmentEffectHandler handler) {
        this.effectHandler = handler;
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder conflictsWith(@NotNull NamespacedKey key) {
        this.conflicts.add(Objects.requireNonNull(key, "conflict key must not be null"));
        return this;
    }

    @Override
    @NotNull
    public EnchantmentDefinition.Builder conflictsWith(@NotNull NamespacedKey... keys) {
        Objects.requireNonNull(keys, "keys must not be null");
        for (NamespacedKey key : keys) {
            if (key != null) {
                this.conflicts.add(key);
            }
        }
        return this;
    }

    @Override
    @NotNull
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        // Required field validation
        validateRequiredFields(errors);

        // Level bounds validation
        validateLevelBounds(errors);

        // Material set validation
        validateMaterials(errors);

        // Conflict validation
        validateConflicts(errors);

        // Cross-property validation
        validateCrossProperties(errors);

        return Collections.unmodifiableList(errors);
    }

    private void validateRequiredFields(List<String> errors) {
        if (key == null) {
            errors.add("Required field 'key' is not set");
        }
        if (displayName == null) {
            errors.add("Required field 'displayName' is not set");
        }
        if (scaling == null) {
            errors.add("Required field 'scaling' is not set");
        }
    }

    private void validateLevelBounds(List<String> errors) {
        if (minLevel < 1) {
            errors.add("minLevel must be >= 1, but was: " + minLevel);
        }
        if (maxLevel <= 0) {
            errors.add("maxLevel must be > 0, but was: " + maxLevel);
        } else if (minLevel >= 1 && maxLevel <= minLevel) {
            errors.add("maxLevel (" + maxLevel + ") must be > minLevel (" + minLevel + ")");
        }
        if (maxLevel > 255) {
            errors.add("maxLevel (" + maxLevel + ") exceeds Minecraft's maximum enchantment level of 255");
        }
    }

    private void validateMaterials(List<String> errors) {
        if (applicableMaterials.isEmpty()) {
            errors.add("At least one applicable material must be specified");
            return;
        }

        int nonItemCount = 0;
        for (Material material : applicableMaterials) {
            if (!material.isItem()) {
                nonItemCount++;
            }
        }
        if (nonItemCount > 0) {
            errors.add(nonItemCount + " material(s) are not items and cannot hold enchantments");
        }
    }

    private void validateConflicts(List<String> errors) {
        if (key != null && conflicts.contains(key)) {
            errors.add("Enchantment cannot conflict with itself (key: " + key + ")");
        }
    }

    private void validateCrossProperties(List<String> errors) {
        if (curse && tradeable) {
            errors.add("WARNING: Curse enchantment is marked as tradeable (vanilla curses are typically not tradeable)");
        }

        if (maxLevel > 10 && maxLevel <= 255) {
            errors.add("WARNING: maxLevel (" + maxLevel + ") is very high; ensure this is intentional");
        }

        if (scaling != null && minLevel >= 1) {
            try {
                double minValue = scaling.calculate(minLevel);
                double maxValue = scaling.calculate(maxLevel);

                if (!Double.isFinite(minValue)) {
                    errors.add("scaling produces non-finite value at minLevel (" + minLevel + "): " + minValue);
                }
                if (!Double.isFinite(maxValue)) {
                    errors.add("scaling produces non-finite value at maxLevel (" + maxLevel + "): " + maxValue);
                }
                if (Double.isNaN(minValue) || Double.isNaN(maxValue)) {
                    errors.add("scaling produces NaN values");
                }
            } catch (Exception e) {
                errors.add("scaling threw exception during validation: " + e.getMessage());
            }
        }
    }

    @Override
    @NotNull
    public EnchantmentDefinition build() {
        List<String> errors = getValidationErrors();
        List<String> criticalErrors = new ArrayList<>();

        for (String error : errors) {
            if (!error.startsWith("WARNING:")) {
                criticalErrors.add(error);
            }
        }

        if (!criticalErrors.isEmpty()) {
            throw new IllegalStateException(
                "Cannot build enchantment definition: " + criticalErrors.size() + " error(s) found:\n  - " +
                String.join("\n  - ", criticalErrors)
            );
        }

        return new EnchantmentDefinitionImpl(
            key,
            displayName,
            description,
            minLevel,
            maxLevel,
            scaling,
            Collections.unmodifiableSet(new HashSet<>(applicableMaterials)),
            curse,
            tradeable,
            discoverable,
            rarity,
            effectHandler,
            Collections.unmodifiableSet(new HashSet<>(conflicts))
        );
    }
}
