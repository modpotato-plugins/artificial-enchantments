package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

public final class DiminishingScaling implements LevelScaling {

    private final double maxValue;
    private final double scalingFactor;

    public DiminishingScaling(double maxValue, double scalingFactor) {
        if (maxValue <= 0) {
            throw new IllegalArgumentException("Max value must be positive");
        }
        if (scalingFactor <= 0) {
            throw new IllegalArgumentException("Scaling factor must be positive");
        }
        this.maxValue = maxValue;
        this.scalingFactor = scalingFactor;
    }

    @Override
    public double calculate(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be >= 1, got: " + level);
        }
        return maxValue * (level / (level + scalingFactor));
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("DiminishingScaling[max=%.2f, factor=%.2f]", maxValue, scalingFactor);
    }
}
