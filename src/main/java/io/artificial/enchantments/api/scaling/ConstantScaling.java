package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

/**
 * A scaling algorithm that returns a constant value regardless of level.
 *
 * @since 0.1.0
 */
public final class ConstantScaling implements LevelScaling {

    private final double value;

    /**
     * Creates a new constant scaling with the specified value.
     *
     * @param value the constant value to return for all levels
     */
    public ConstantScaling(double value) {
        this.value = value;
    }

    @Override
    public double calculate(int level) {
        return value;
    }

    /**
     * Gets the constant value.
     *
     * @return the value
     */
    public double getValue() {
        return value;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("ConstantScaling[value=%.2f]", value);
    }
}
