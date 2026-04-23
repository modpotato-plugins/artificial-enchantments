package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

/**
 * A scaling algorithm that approaches a maximum value with diminishing returns.
 *
 * <p>The formula is {@code max * (level / (level + factor))}, meaning each
 * additional level provides a smaller increase than the previous one.
 *
 * @since 0.1.0
 */
public final class DiminishingScaling implements LevelScaling {

    private final double maxValue;
    private final double scalingFactor;

    /**
     * Creates a new diminishing scaling.
     *
     * @param maxValue the asymptotic maximum value (must be positive)
     * @param scalingFactor the factor controlling return diminishment (must be positive)
     * @throws IllegalArgumentException if either parameter is not positive
     */
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

    /**
     * Gets the maximum asymptotic value.
     *
     * @return the max value
     */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * Gets the scaling factor.
     *
     * @return the scaling factor
     */
    public double getScalingFactor() {
        return scalingFactor;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("DiminishingScaling[max=%.2f, factor=%.2f]", maxValue, scalingFactor);
    }
}
