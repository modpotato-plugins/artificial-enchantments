package io.artificial.enchantments.api.enchanttable;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration API for customizing enchantment table behavior per-enchantment.
 *
 * <p>This interface allows enchantments to specify how they appear in the
 * enchanting table, including weight multipliers, cost adjustments, and
 * whether they should appear at all.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * EnchantmentDefinition myEnchant = EnchantmentDefinition.builder()
 *     .key(new NamespacedKey(plugin, "my_enchant"))
 *     .displayName(Component.text("My Enchant"))
 *     .tableConfiguration(EnchantTableConfiguration.builder()
 *         .tableWeight(5)           // Appears 5x more often than base
 *         .minBookshelves(5)        // Requires at least 5 bookshelves
 *         .maxBookshelves(15)       // Won't appear above 15 bookshelves
 *         .costMultiplier(0.8)      // 20% cheaper than normal
 *         .allowMultiple(false)     // Only one offer per table
 *         .build())
 *     .build();
 * }</pre>
 *
 * @since 0.3.0
 */
public interface EnchantTableConfiguration {

    /**
     * Gets the weight multiplier for this enchantment in the table.
     *
     * <p>Base weights are determined by rarity:
     * <ul>
     *   <li>COMMON: 10</li>
     *   <li>UNCOMMON: 5</li>
     *   <li>RARE: 2</li>
     *   <li>VERY_RARE: 1</li>
     * </ul>
     *
     * <p>This multiplier is applied to the base weight. A value of 2.0 makes
     * the enchantment twice as likely to appear.
     *
     * @return the weight multiplier (default: 1.0)
     * @since 0.3.0
     */
    double getWeightMultiplier();

    /**
     * Gets the minimum number of bookshelves required for this enchantment.
     *
     * <p>The enchantment will not appear in offers if the table has fewer
     * than this many bookshelves. Vanilla enchantments typically require
     * 0-15 bookshelves depending on the enchantment type.
     *
     * @return minimum bookshelves required (default: 0)
     * @since 0.3.0
     */
    int getMinBookshelves();

    /**
     * Gets the maximum number of bookshelves for this enchantment.
     *
     * <p>The enchantment will not appear in offers if the table has more
     * than this many bookshelves. Use this to restrict high-tier enchantments
     * to lower-power tables.
     *
     * @return maximum bookshelves allowed (default: 15)
     * @since 0.3.0
     */
    int getMaxBookshelves();

    /**
     * Gets the cost multiplier for enchanting with this enchantment.
     *
     * <p>This multiplier is applied to the base cost calculated from the
     * enchantment level and power. A value of 0.8 makes the enchantment 20%
     * cheaper. Values above 1.0 make it more expensive.
     *
     * @return the cost multiplier (default: 1.0)
     * @since 0.3.0
     */
    double getCostMultiplier();

    /**
     * Checks if this enchantment can appear multiple times in the same table.
     *
     * <p>If false, only one offer slot can contain this enchantment. If true,
     * multiple slots may offer the same enchantment at different levels.
     *
     * @return true if multiple offers allowed (default: false)
     * @since 0.3.0
     */
    boolean allowsMultipleOffers();

    /**
     * Gets the minimum enchantment level that can appear in the table.
     *
     * <p>This is independent of the definition's minLevel. Use this to restrict
     * table offers to higher levels while allowing lower levels through other
     * means (e.g., anvil combinations).
     *
     * @return minimum level for table offers (default: definition's minLevel)
     * @since 0.3.0
     */
    int getTableMinLevel();

    /**
     * Gets the maximum enchantment level that can appear in the table.
     *
     * <p>This is independent of the definition's maxLevel. Use this to restrict
     * table offers to lower levels while allowing higher levels through other means.
     *
     * @return maximum level for table offers (default: definition's maxLevel)
     * @since 0.3.0
     */
    int getTableMaxLevel();

    /**
     * Checks if this enchantment respects vanilla treasure/book restrictions.
     *
     * <p>If true, the enchantment will only appear in the table when the item
     * is placed directly (not via book). Treasure enchantments typically
     * only appear this way.
     *
     * @return true if treasure-like behavior (default: false)
     * @since 0.3.0
     */
    boolean isTreasure();

    /**
     * Creates a builder for constructing table configurations.
     *
     * @return a new builder instance
     * @since 0.3.0
     */
    @NotNull
    static Builder builder() {
        return new io.artificial.enchantments.internal.enchanttable.EnchantTableConfigurationBuilder();
    }

    /**
     * Gets the default configuration with all default values.
     *
     * @return the default configuration instance
     * @since 0.3.0
     */
    @NotNull
    static EnchantTableConfiguration defaults() {
        return new io.artificial.enchantments.internal.enchanttable.EnchantTableConfigurationImpl(
            1.0, 0, 15, 1.0, false, 1, Integer.MAX_VALUE, false
        );
    }

    /**
     * Builder for constructing table configurations.
     *
     * @since 0.3.0
     */
    interface Builder {

        /**
         * Sets the weight multiplier for this enchantment.
         *
         * @param multiplier the weight multiplier (must be > 0)
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder weightMultiplier(double multiplier);

        /**
         * Sets the minimum bookshelves required.
         *
         * @param min the minimum bookshelves (must be >= 0)
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder minBookshelves(int min);

        /**
         * Sets the maximum bookshelves allowed.
         *
         * @param max the maximum bookshelves (must be >= min)
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder maxBookshelves(int max);

        /**
         * Sets the cost multiplier.
         *
         * @param multiplier the cost multiplier (must be > 0)
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder costMultiplier(double multiplier);

        /**
         * Sets whether multiple offers are allowed.
         *
         * @param allow true to allow multiple offers
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder allowMultiple(boolean allow);

        /**
         * Sets the minimum enchantment level for table offers.
         *
         * @param level the minimum level (must be >= 1)
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder tableMinLevel(int level);

        /**
         * Sets the maximum enchantment level for table offers.
         *
         * @param level the maximum level (must be >= min)
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder tableMaxLevel(int level);

        /**
         * Sets whether this is a treasure enchantment.
         *
         * @param treasure true for treasure behavior
         * @return this builder for chaining
         * @since 0.3.0
         */
        @NotNull
        Builder treasure(boolean treasure);

        /**
         * Builds and returns the table configuration.
         *
         * @return the completed configuration
         * @since 0.3.0
         */
        @NotNull
        EnchantTableConfiguration build();
    }
}
