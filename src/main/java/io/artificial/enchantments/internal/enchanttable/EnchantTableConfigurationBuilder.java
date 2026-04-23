package io.artificial.enchantments.internal.enchanttable;

import io.artificial.enchantments.api.enchanttable.EnchantTableConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation for EnchantTableConfiguration.
 */
public final class EnchantTableConfigurationBuilder implements EnchantTableConfiguration.Builder {

    private double weightMultiplier = 1.0;
    private int minBookshelves = 0;
    private int maxBookshelves = 15;
    private double costMultiplier = 1.0;
    private boolean allowsMultipleOffers = false;
    private int tableMinLevel = 1;
    private int tableMaxLevel = Integer.MAX_VALUE;
    private boolean treasure = false;

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder weightMultiplier(double multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Weight multiplier must be positive");
        }
        this.weightMultiplier = multiplier;
        return this;
    }

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder minBookshelves(int min) {
        if (min < 0) {
            throw new IllegalArgumentException("Minimum bookshelves must be non-negative");
        }
        this.minBookshelves = min;
        return this;
    }

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder maxBookshelves(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Maximum bookshelves must be non-negative");
        }
        this.maxBookshelves = max;
        return this;
    }

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder costMultiplier(double multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Cost multiplier must be positive");
        }
        this.costMultiplier = multiplier;
        return this;
    }

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder allowMultiple(boolean allow) {
        this.allowsMultipleOffers = allow;
        return this;
    }

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder tableMinLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Table min level must be at least 1");
        }
        this.tableMinLevel = level;
        return this;
    }

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder tableMaxLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Table max level must be at least 1");
        }
        this.tableMaxLevel = level;
        return this;
    }

    @Override
    @NotNull
    public EnchantTableConfiguration.Builder treasure(boolean treasure) {
        this.treasure = treasure;
        return this;
    }

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
