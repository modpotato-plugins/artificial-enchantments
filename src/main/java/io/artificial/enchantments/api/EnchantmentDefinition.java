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

/**
 * Defines a custom enchantment that can be registered with the library.
 * 
 * <p>An enchantment definition includes all metadata needed for registration,
 * application to items, and effect scaling. Definitions are immutable after
 * creation and can be built using the {@link Builder}.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * EnchantmentDefinition myEnchant = EnchantmentDefinition.builder()
 *     .key(new NamespacedKey(plugin, "my_enchant"))
 *     .displayName(Component.text("My Enchant"))
 *     .description(Component.text("Does cool stuff"))
 *     .minLevel(1)
 *     .maxLevel(5)
 *     .scaling(LevelScaling.linear(1.0, 0.5))
 *     .applicable(Material.DIAMOND_SWORD, Material.IRON_SWORD)
 *     .rarity(EnchantmentDefinition.Rarity.RARE)
 *     .effectHandler(new MyEffectHandler())
 *     .build();
 * 
 * api.registerEnchantment(myEnchant);
 * }</pre>
 *
 * @see Builder
 * @see LevelScaling
 * @see EnchantmentEffectHandler
 * @since 0.1.0
 */
public interface EnchantmentDefinition {

    /**
     * Gets the unique namespaced key for this enchantment.
     *
     * @return the enchantment's key (never null)
     * @since 0.1.0
     */
    @NotNull
    NamespacedKey getKey();

    /**
     * Gets the display name shown to players.
     *
     * @return the display name component (never null)
     * @since 0.1.0
     */
    @NotNull
    Component getDisplayName();

    /**
     * Gets the optional description shown in lore.
     *
     * @return the description component, or null if none
     * @since 0.1.0
     */
    @Nullable
    Component getDescription();

    /**
     * Gets the minimum valid level for this enchantment.
     *
     * @return the minimum level (always >= 1)
     * @since 0.1.0
     */
    int getMinLevel();

    /**
     * Gets the maximum valid level for this enchantment.
     *
     * @return the maximum level (> minLevel)
     * @since 0.1.0
     */
    int getMaxLevel();

    /**
     * Gets the scaling formula for calculating effect values.
     *
     * @return the level scaling instance (never null)
     * @since 0.1.0
     */
    @NotNull
    LevelScaling getScaling();

    /**
     * Gets all materials this enchantment can be applied to.
     *
     * @return set of applicable materials (never null)
     * @since 0.1.0
     */
    @NotNull
    Set<Material> getApplicableMaterials();

    /**
     * Checks if this enchantment can be applied to a specific material.
     *
     * @param material the material to check (must not be null)
     * @return true if applicable
     * @since 0.1.0
     */
    boolean isApplicableTo(@NotNull Material material);

    /**
     * Checks if this enchantment can be applied to an item.
     *
     * @param item the item to check (must not be null)
     * @return true if applicable to the item's material
     * @since 0.1.0
     */
    boolean isApplicableTo(@NotNull ItemStack item);

    /**
     * Checks if this is a curse enchantment.
     *
     * @return true if the enchantment is a curse
     * @since 0.1.0
     */
    boolean isCurse();

    /**
     * Checks if this enchantment can be traded with villagers.
     *
     * @return true if tradeable
     * @since 0.1.0
     */
    boolean isTradeable();

    /**
     * Checks if this enchantment can be found in loot.
     *
     * @return true if discoverable
     * @since 0.1.0
     */
    boolean isDiscoverable();

    /**
     * Gets the rarity tier of this enchantment.
     *
     * @return the rarity (never null)
     * @since 0.1.0
     */
    @NotNull
    Rarity getRarity();

    /**
     * Gets the effect handler for this enchantment, if any.
     *
     * @return the effect handler, or null if this enchantment has no effects
     * @since 0.1.0
     */
    @Nullable
    EnchantmentEffectHandler getEffectHandler();

    /**
     * Calculates the scaled value for a given level.
     * 
     * <p>Uses the configured {@link LevelScaling} formula.
     *
     * @param level the enchantment level to calculate for
     * @return the scaled value
     * @throws IllegalArgumentException if level is invalid
     * @since 0.1.0
     */
    double calculateScaledValue(int level);

    /**
     * Checks if this enchantment conflicts with another.
     *
     * @param other the other enchantment to check (must not be null)
     * @return true if the enchantments cannot coexist on the same item
     * @since 0.1.0
     */
    boolean conflictsWith(@NotNull EnchantmentDefinition other);

    /**
     * Gets all enchantment keys that conflict with this one.
     *
     * @return set of conflicting enchantment keys (never null)
     * @since 0.1.0
     */
    @NotNull
    Set<NamespacedKey> getConflictingEnchantments();

    /**
     * Rarity tiers for enchantments, affecting trading and loot weights.
     *
     * @since 0.1.0
     */
    enum Rarity {
        /** Common enchantments appear frequently in trading and loot */
        COMMON,
        /** Uncommon enchantments appear less frequently */
        UNCOMMON,
        /** Rare enchantments appear infrequently */
        RARE,
        /** Very rare enchantments are hard to find */
        VERY_RARE
    }

    /**
     * Builder for creating enchantment definitions.
     * 
     * <p>All properties must be set before calling {@link #build()}, except
     * for optional properties which have sensible defaults.
     *
     * <p><strong>Required Properties:</strong>
     * <ul>
     *   <li>{@link #key(NamespacedKey)} - the unique identifier</li>
     *   <li>{@link #displayName(Component)} - the display name</li>
     *   <li>{@link #minLevel(int)} and {@link #maxLevel(int)} - level bounds</li>
     *   <li>{@link #scaling(LevelScaling)} - the scaling formula</li>
     *   <li>{@link #applicable(Material...)} - valid materials</li>
     * </ul>
     *
     * @since 0.1.0
     */
    interface Builder {
        /**
         * Sets the unique namespaced key for this enchantment.
         *
         * @param key the enchantment key (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder key(@NotNull NamespacedKey key);

        /**
         * Sets the display name shown to players.
         *
         * @param name the display name (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder displayName(@NotNull Component name);

        /**
         * Sets the optional description for lore display.
         *
         * @param description the description, or null for none
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder description(@Nullable Component description);

        /**
         * Sets the minimum valid level.
         *
         * @param minLevel the minimum level (must be >= 1)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder minLevel(int minLevel);

        /**
         * Sets the maximum valid level.
         *
         * @param maxLevel the maximum level (must be > minLevel)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder maxLevel(int maxLevel);

        /**
         * Sets the scaling formula using a {@link LevelScaling} instance.
         *
         * @param scaling the scaling formula (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder scaling(@NotNull LevelScaling scaling);

        /**
         * Sets a custom scaling formula using a lambda.
         *
         * @param formula the formula function (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder scaling(@NotNull Function<Integer, Double> formula);

        /**
         * Sets applicable materials from a varargs list.
         *
         * @param materials the materials (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder applicable(@NotNull Material... materials);

        /**
         * Sets applicable materials from a set.
         *
         * @param materials the material set (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder applicable(@NotNull Set<Material> materials);

        /**
         * Sets whether this is a curse enchantment.
         *
         * @param curse true for curse enchantments
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder curse(boolean curse);

        /**
         * Marks this enchantment as a curse (convenience method).
         *
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder curse();

        /**
         * Sets whether this enchantment is tradeable with villagers.
         *
         * @param tradeable true if tradeable
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder tradeable(boolean tradeable);

        /**
         * Sets whether this enchantment appears in loot tables.
         *
         * @param discoverable true if discoverable
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder discoverable(boolean discoverable);

        /**
         * Sets the rarity tier.
         *
         * @param rarity the rarity (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder rarity(@NotNull Rarity rarity);

        /**
         * Sets the effect handler for this enchantment.
         *
         * @param handler the effect handler, or null for no effects
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder effectHandler(@Nullable EnchantmentEffectHandler handler);

        /**
         * Adds a conflicting enchantment key.
         *
         * @param key the conflicting enchantment key (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder conflictsWith(@NotNull NamespacedKey key);

        /**
         * Adds multiple conflicting enchantment keys.
         *
         * @param keys the conflicting keys (must not be null)
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        Builder conflictsWith(@NotNull NamespacedKey... keys);

        /**
         * Builds and returns the enchantment definition.
         *
         * @return the completed definition
         * @throws IllegalStateException if required properties are not set
         * @since 0.1.0
         */
        @NotNull
        EnchantmentDefinition build();
    }

    /**
     * Creates a new builder for constructing enchantment definitions.
     *
     * @return a new builder instance
     * @throws UnsupportedOperationException in the API module
     * @since 0.1.0
     */
    @NotNull
    static Builder builder() {
        throw new UnsupportedOperationException("Implementation not available in API module");
    }
}
