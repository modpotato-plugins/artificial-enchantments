package io.artificial.enchantments.internal.scaling;

import io.artificial.enchantments.api.scaling.LevelScaling;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Internal scaling utilities for the effect dispatch spine.
 * 
 * <p>This class provides factory methods and wrappers for scaling formulas
 * used internally by the enchantment effect system. All scaling functions
 * are pure (no side effects, thread-safe).</p>
 * 
 * <p>Formula Reference:
 * <ul>
 *   <li><strong>Linear:</strong> result = base + (level - 1) * increment</li>
 *   <li><strong>Exponential:</strong> result = base * multiplier^(level - 1)</li>
 *   <li><strong>Diminishing:</strong> result = max * (level / (level + scalingFactor))</li>
 *   <li><strong>Constant:</strong> result = value (independent of level)</li>
 * </ul></p>
 * 
 * @see io.artificial.enchantments.api.scaling.LevelScaling
 */
public final class ScalingUtils {

    private ScalingUtils() {
        // Utility class - prevent instantiation
        throw new AssertionError("Utility class");
    }

    /**
     * Creates a linear scaling formula.
     * 
     * <p>Formula: {@code base + (level - 1) * increment}</p>
     * 
     * <p>Example: Sharpness-style damage increase
     * <pre>
     * base = 1.0, increment = 0.5
     * Level 1: 1.0 + (0) * 0.5 = 1.0
     * Level 5: 1.0 + (4) * 0.5 = 3.0
     * Level 10: 1.0 + (9) * 0.5 = 5.5
     * </pre></p>
     *
     * @param base the base value at level 1
     * @param increment the amount added per level above 1
     * @return a LevelScaling instance using linear progression
     */
    @NotNull
    public static LevelScaling linear(double base, double increment) {
        return new io.artificial.enchantments.api.scaling.LinearScaling(base, increment);
    }

    /**
     * Creates an exponential scaling formula.
     * 
     * <p>Formula: {@code base * multiplier^(level - 1)}</p>
     * 
     * <p><strong>Warning:</strong> Exponential scaling grows rapidly. Use with care
     * at high levels to avoid overpowered values.</p>
     * 
     * <p>Example:
     * <pre>
     * base = 2.0, multiplier = 1.5
     * Level 1: 2.0 * 1.5^0 = 2.0
     * Level 5: 2.0 * 1.5^4 = 10.125
     * Level 10: 2.0 * 1.5^9 = 76.27
     * </pre></p>
     *
     * @param base the base value at level 1
     * @param multiplier the factor multiplied per level (must be > 0)
     * @return a LevelScaling instance using exponential progression
     * @throws IllegalArgumentException if multiplier is not positive
     */
    @NotNull
    public static LevelScaling exponential(double base, double multiplier) {
        return new io.artificial.enchantments.api.scaling.ExponentialScaling(base, multiplier);
    }

    /**
     * Creates a diminishing returns scaling formula.
     * 
     * <p>Formula: {@code maxValue * (level / (level + scalingFactor))}</p>
     * 
     * <p>This formula approaches maxValue asymptotically as level increases,
     * preventing values from becoming overpowered at high enchantment levels.</p>
     * 
     * <p>Example:
     * <pre>
     * maxValue = 10.0, scalingFactor = 5.0
     * Level 1: 10.0 * (1 / 6) = 1.67
     * Level 5: 10.0 * (5 / 10) = 5.0
     * Level 10: 10.0 * (10 / 15) = 6.67
     * Level 100: 10.0 * (100 / 105) = 9.52
     * </pre></p>
     *
     * @param maxValue the asymptotic maximum value approached at high levels
     * @param scalingFactor controls how quickly the curve approaches maxValue (higher = slower)
     * @return a LevelScaling instance using diminishing returns
     * @throws IllegalArgumentException if maxValue or scalingFactor is not positive
     */
    @NotNull
    public static LevelScaling diminishing(double maxValue, double scalingFactor) {
        return new io.artificial.enchantments.api.scaling.DiminishingScaling(maxValue, scalingFactor);
    }

    /**
     * Creates a constant scaling formula (value independent of level).
     * 
     * <p>Formula: {@code value}</p>
     *
     * @param value the constant value returned for all levels
     * @return a LevelScaling instance returning a fixed value
     */
    @NotNull
    public static LevelScaling constant(double value) {
        return new io.artificial.enchantments.api.scaling.ConstantScaling(value);
    }

    /**
     * Creates a custom scaling formula from a user-defined function.
     * 
     * <p>This allows consumers to define arbitrary scaling formulas for
     * specialized use cases not covered by the built-in formulas.</p>
     * 
     * <p>The function must be pure (no side effects) and thread-safe.</p>
     * 
     * <p>Example:
     * <pre>
     * // Quadratic scaling: value = level^2
     * LevelScaling quadratic = ScalingUtils.custom(level -&gt; (double) level * level);
     * 
     * // Logarithmic scaling: value = log(level + 1)
     * LevelScaling logScale = ScalingUtils.custom(level -&gt; Math.log(level + 1));
     * </pre></p>
     *
     * @param formula the custom formula as a Function&lt;Integer, Double&gt;
     * @return a LevelScaling instance using the custom formula
     * @throws IllegalArgumentException if formula is null
     */
    @NotNull
    public static LevelScaling custom(@NotNull Function<Integer, Double> formula) {
        return LevelScaling.of(formula);
    }

    /**
     * Creates an alternative diminishing returns formula using exponential decay.
     * 
     * <p>Formula: {@code maxValue * (1 - decayFactor^level)}</p>
     * 
     * <p>This variant approaches maxValue from below using exponential decay,
     * providing a different curve shape than the standard diminishing formula.</p>
     * 
     * <p>Example:
     * <pre>
     * maxValue = 10.0, decayFactor = 0.9
     * Level 1: 10.0 * (1 - 0.9) = 1.0
     * Level 5: 10.0 * (1 - 0.9^5) = 4.1
     * Level 10: 10.0 * (1 - 0.9^10) = 6.51
     * Level 100: 10.0 * (1 - 0.9^100) ≈ 10.0
     * </pre></p>
     *
     * @param maxValue the maximum value approached at infinite level
     * @param decayFactor the decay rate per level (0 &lt; decayFactor &lt; 1)
     * @return a LevelScaling instance using exponential decay
     * @throws IllegalArgumentException if maxValue is not positive or decayFactor not in (0, 1)
     */
    @NotNull
    public static LevelScaling decaying(double maxValue, double decayFactor) {
        if (maxValue <= 0) {
            throw new IllegalArgumentException("Max value must be positive");
        }
        if (decayFactor <= 0 || decayFactor >= 1) {
            throw new IllegalArgumentException("Decay factor must be in range (0, 1)");
        }
        return new DecayingScaling(maxValue, decayFactor);
    }

    /**
     * Validates that a level is valid (>= 1).
     *
     * @param level the level to validate
     * @throws IllegalArgumentException if level < 1
     */
    public static void validateLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be >= 1, got: " + level);
        }
    }

    /**
     * Calculates a value and rounds to the nearest integer.
     * Useful for discrete values like damage amounts.
     *
     * @param scaling the scaling formula
     * @param level the enchantment level
     * @return the rounded value
     */
    public static int calculateRounded(@NotNull LevelScaling scaling, int level) {
        return (int) Math.round(scaling.calculate(level));
    }

    /**
     * Calculates a value and floors to the nearest integer.
     * Useful for ensuring minimum thresholds.
     *
     * @param scaling the scaling formula
     * @param level the enchantment level
     * @return the floored value
     */
    public static int calculateFloored(@NotNull LevelScaling scaling, int level) {
        return (int) Math.floor(scaling.calculate(level));
    }

    /**
     * Calculates a value and ceils to the nearest integer.
     * Useful for ensuring at least a certain amount.
     *
     * @param scaling the scaling formula
     * @param level the enchantment level
     * @return the ceiled value
     */
    public static int calculateCeiled(@NotNull LevelScaling scaling, int level) {
        return (int) Math.ceil(scaling.calculate(level));
    }
}
