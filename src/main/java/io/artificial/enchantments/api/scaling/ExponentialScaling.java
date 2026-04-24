package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

/**
 * Exponential scaling algorithm: base * multiplier^(level - 1).
 *
 * <p>This scaling pattern produces values that grow by a percentage each level,
 * rather than by a constant amount. Use with caution at high levels as values
 * can grow rapidly and may become unbalanced.
 *
 * <p><strong>Formula:</strong> {@code value = base * multiplier^(level - 1)}
 *
 * <p><strong>Example:</strong> A multiplier of 1.2 means each level is 20% stronger
 * than the previous: base=1.0, multiplier=1.2 produces values 1.0, 1.2, 1.44, 1.73, etc.
 *
 * @since 0.1.0
 */
public final class ExponentialScaling implements LevelScaling {

    private final double base;
    private final double multiplier;

    /**
     * Creates a new exponential scaling with the specified base and multiplier.
     *
     * @param base the value at level 1
     * @param multiplier the growth factor per level (must be positive, 1.0 = no growth)
     * @throws IllegalArgumentException if multiplier is not positive
     */
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

    /**
     * Gets the base value at level 1.
     *
     * @return the base value
     * @since 0.1.0
     */
    public double getBase() {
        return base;
    }

    /**
     * Gets the multiplier applied per level.
     *
     * @return the multiplier value
     * @since 0.1.0
     */
    public double getMultiplier() {
        return multiplier;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("ExponentialScaling[base=%.2f, multiplier=%.2f]", base, multiplier);
    }
}
