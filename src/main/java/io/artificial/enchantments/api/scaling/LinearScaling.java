package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

public final class LinearScaling implements LevelScaling {

    private final double base;
    private final double increment;

    public LinearScaling(double base, double increment) {
        this.base = base;
        this.increment = increment;
    }

    @Override
    public double calculate(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be >= 1, got: " + level);
        }
        return base + ((level - 1) * increment);
    }

    public double getBase() {
        return base;
    }

    public double getIncrement() {
        return increment;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("LinearScaling[base=%.2f, increment=%.2f]", base, increment);
    }
}
