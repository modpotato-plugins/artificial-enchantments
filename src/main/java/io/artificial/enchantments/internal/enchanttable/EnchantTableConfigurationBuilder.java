package io.artificial.enchantments.internal.enchanttable;

import io.artificial.enchantments.api.enchanttable.EnchantTableConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation for {@link EnchantTableConfiguration}.
 * <p>
 * This builder allows configuring all aspects of how an enchantment appears
 * in the enchantment table:
 * <ul>
 *   <li>Weight multiplier - affects selection probability relative to rarity</li>
 *   <li>Bookshelf range - min/max bookshelves required to appear</li>
 *   <li>Cost multiplier - scales the lapis/level cost</li>
 *   <li>Multiple offers - whether this enchantment can appear in multiple slots</li>
 *   <li>Level clamping - min/max levels that can be offered</li>
 *   <li>Treasure flag - whether this is a treasure-only enchantment</li>
 * </ul>
 * <p>
 * Default values follow vanilla behavior:
 * <ul>
 *   <li>Weight multiplier: 1.0 (no adjustment)</li>
 *   <li>Bookshelves: 0-15 (available at all power levels)</li>
 *   <li>Cost multiplier: 1.0 (vanilla scaling)</li>
 *   <li>Single offer per enchantment</li>
 *   <li>Level range: 1 to enchantment max level</li>
 *   <li>Non-treasure (discoverable)</li>
 * </ul>
 *
 * @since 0.2.0
 */
public final class EnchantTableConfigurationBuilder implements EnchantTableConfiguration.Builder {

    /**
     * Creates a new enchantment table configuration builder.
     *
     * @since 0.2.0
     */
    public EnchantTableConfigurationBuilder() {
    }

    private double weightMultiplier = 1.0;
    private int minBookshelves = 0;
    private int maxBookshelves = 15;
    private double costMultiplier = 1.0;
    private boolean allowsMultipleOffers = false;
    private int tableMinLevel = 1;
    private int tableMaxLevel = Integer.MAX_VALUE;
    private boolean treasure = false;

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder weightMultiplier(double multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Weight multiplier must be positive");
        }
        this.weightMultiplier = multiplier;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder minBookshelves(int min) {
        if (min < 0) {
            throw new IllegalArgumentException("Minimum bookshelves must be non-negative");
        }
        this.minBookshelves = min;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder maxBookshelves(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Maximum bookshelves must be non-negative");
        }
        this.maxBookshelves = max;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder costMultiplier(double multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Cost multiplier must be positive");
        }
        this.costMultiplier = multiplier;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder allowMultiple(boolean allow) {
        this.allowsMultipleOffers = allow;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder tableMinLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Table min level must be at least 1");
        }
        this.tableMinLevel = level;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder tableMaxLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Table max level must be at least 1");
        }
        this.tableMaxLevel = level;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantTableConfiguration.Builder treasure(boolean treasure) {
        this.treasure = treasure;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Validates constraints before building:
     * <ul>
     *   <li>minBookshelves must not exceed maxBookshelves</li>
     *   <li>tableMinLevel must not exceed tableMaxLevel</li>
     * </ul>
     *
     * @throws IllegalStateException if constraints are violated
     */
    @Override
    @NotNull
    public EnchantTableConfiguration build() {
        if (minBookshelves > maxBookshelves) {
            throw new IllegalStateException("Min bookshelves cannot exceed max bookshelves");
        }
        if (tableMinLevel > tableMaxLevel) {
            throw new IllegalStateException("Table min level cannot exceed table max level");
        }
        return new EnchantTableConfigurationImpl(
            weightMultiplier,
            minBookshelves,
            maxBookshelves,
            costMultiplier,
            allowsMultipleOffers,
            tableMinLevel,
            tableMaxLevel,
            treasure
        );
    }
}
