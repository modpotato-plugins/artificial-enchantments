package io.artificial.enchantments.internal.enchanttable;

import io.artificial.enchantments.api.enchanttable.EnchantTableConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable implementation of EnchantTableConfiguration.
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

    @Override
    public double getWeightMultiplier() {
        return weightMultiplier;
    }

    @Override
    public int getMinBookshelves() {
        return minBookshelves;
    }

    @Override
    public int getMaxBookshelves() {
        return maxBookshelves;
    }

    @Override
    public double getCostMultiplier() {
        return costMultiplier;
    }

    @Override
    public boolean allowsMultipleOffers() {
        return allowsMultipleOffers;
    }

    @Override
    public int getTableMinLevel() {
        return tableMinLevel;
    }

    @Override
    public int getTableMaxLevel() {
        return tableMaxLevel;
    }

    @Override
    public boolean isTreasure() {
        return treasure;
    }
}
