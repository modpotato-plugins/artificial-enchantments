package io.artificial.enchantments.internal.scaling;

import io.artificial.enchantments.api.scaling.LevelScaling;
import org.jetbrains.annotations.NotNull;

/**
 * Exponential decay scaling formula.
 * 
 * <p>Formula: maxValue * (1 - decayFactor^level)
 * 
 * <p>This formula approaches maxValue asymptotically from below as level increases,
 * using exponential decay. The decayFactor controls how quickly the value approaches max.
 *
 * @since 0.2.0
 */
final class DecayingScaling implements LevelScaling {

    private final double maxValue;
    private final double decayFactor;

    DecayingScaling(double maxValue, double decayFactor) {
        if (maxValue <= 0) {
            throw new IllegalArgumentException("Max value must be positive");
        }
        if (decayFactor <= 0 || decayFactor >= 1) {
            throw new IllegalArgumentException("Decay factor must be in range (0, 1)");
        }
        this.maxValue = maxValue;
        this.decayFactor = decayFactor;
    }

    @Override
    public double calculate(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be >= 1, got: " + level);
        }
        return maxValue * (1 - Math.pow(decayFactor, level));
    }

    double getMaxValue() {
        return maxValue;
    }

    double getDecayFactor() {
        return decayFactor;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("DecayingScaling[max=%.2f, decay=%.2f]", maxValue, decayFactor);
    }
}
