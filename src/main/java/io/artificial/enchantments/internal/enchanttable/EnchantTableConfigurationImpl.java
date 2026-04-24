package io.artificial.enchantments.internal.enchanttable;

import io.artificial.enchantments.api.enchanttable.EnchantTableConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable implementation of {@link EnchantTableConfiguration}.
 * <p>
 * This class stores all configuration values for how an enchantment appears
 * and behaves in the enchantment table. All fields are final and set at
 * construction time via the builder.
 * <p>
 * <strong>Configuration Values:</strong>
 * <ul>
 *   <li>weightMultiplier - adjusts selection probability (default: 1.0)</li>
 *   <li>minBookshelves - minimum bookshelf count required (default: 0)</li>
 *   <li>maxBookshelves - maximum bookshelf count allowed (default: 15)</li>
 *   <li>costMultiplier - scales the lapis/level cost (default: 1.0)</li>
 *   <li>allowsMultipleOffers - can appear in multiple slots (default: false)</li>
 *   <li>tableMinLevel - minimum level offered (default: 1)</li>
 *   <li>tableMaxLevel - maximum level offered (default: Integer.MAX_VALUE)</li>
 *   <li>treasure - treasure-only enchantment (default: false)</li>
 * </ul>
 *
 * @since 0.2.0
 */
public final class EnchantTableConfigurationImpl implements EnchantTableConfiguration {

    private final double weightMultiplier;
    private final int minBookshelves;
    private final int maxBookshelves;
    private final double costMultiplier;
    private final boolean allowsMultipleOffers;
    private final int tableMinLevel;
    private final int tableMaxLevel;
    private final boolean treasure;

    /**
     * Creates a new immutable configuration.
     * <p>
     * This constructor is package-private. Use {@link EnchantTableConfigurationBuilder}
     * to create instances.
     *
     * @param weightMultiplier the weight multiplier for rarity adjustment
     * @param minBookshelves the minimum required bookshelves
     * @param maxBookshelves the maximum allowed bookshelves
     * @param costMultiplier the cost scaling multiplier
     * @param allowsMultipleOffers whether multiple offers are allowed
     * @param tableMinLevel the minimum level that can be offered
     * @param tableMaxLevel the maximum level that can be offered
     * @param treasure whether this is a treasure enchantment
     * @since 0.2.0
     */
    public EnchantTableConfigurationImpl(
        double weightMultiplier,
        int minBookshelves,
        int maxBookshelves,
        double costMultiplier,
        boolean allowsMultipleOffers,
        int tableMinLevel,
        int tableMaxLevel,
        boolean treasure
    ) {
        this.weightMultiplier = weightMultiplier;
        this.minBookshelves = minBookshelves;
        this.maxBookshelves = maxBookshelves;
        this.costMultiplier = costMultiplier;
        this.allowsMultipleOffers = allowsMultipleOffers;
        this.tableMinLevel = tableMinLevel;
        this.tableMaxLevel = tableMaxLevel;
        this.treasure = treasure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getWeightMultiplier() {
        return weightMultiplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinBookshelves() {
        return minBookshelves;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxBookshelves() {
        return maxBookshelves;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCostMultiplier() {
        return costMultiplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean allowsMultipleOffers() {
        return allowsMultipleOffers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTableMinLevel() {
        return tableMinLevel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTableMaxLevel() {
        return tableMaxLevel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTreasure() {
        return treasure;
    }
}
