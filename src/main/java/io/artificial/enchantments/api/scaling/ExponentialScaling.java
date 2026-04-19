package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

public final class ExponentialScaling implements LevelScaling {

    private final double base;
    private final double multiplier;

    public ExponentialScaling(double base, double multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive");
        }
        this.base = base;
        this.multiplier = multiplier;
    }

    @Override
    public double calculate(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be >= 1, got: " + level);
        }
        return base * Math.pow(multiplier, level - 1);
    }

    public double getBase() {
        return base;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("ExponentialScaling[base=%.2f, multiplier=%.2f]", base, multiplier);
    }
}
