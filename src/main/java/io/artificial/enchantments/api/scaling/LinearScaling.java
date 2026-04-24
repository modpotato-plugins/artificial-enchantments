package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

/**
 * Linear scaling algorithm: base + (level - 1) * increment.
 *
 * <p>This is the most common scaling pattern, used by vanilla enchantments
 * like Sharpness. Each level adds a constant amount to the base value.
 *
 * <p><strong>Formula:</strong> {@code value = base + (level - 1) * increment}
 *
 * <p><strong>Example:</strong> Sharpness adds 0.5 damage per level, starting at 1.0
 * for level 1: base=1.0, increment=0.5 produces values 1.0, 1.5, 2.0, 2.5, etc.
 *
 * @since 0.1.0
 */
public final class LinearScaling implements LevelScaling {

    private final double base;
    private final double increment;

    /**
     * Creates a new linear scaling with the specified base and increment.
     *
     * @param base the value at level 1
     * @param increment the amount added per additional level
     */
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
     * Gets the increment added per level.
     *
     * @return the increment value
     * @since 0.1.0
     */
    public double getIncrement() {
        return increment;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("LinearScaling[base=%.2f, increment=%.2f]", base, increment);
    }
}
