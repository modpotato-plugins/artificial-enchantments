package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;
import java.util.function.Function;

/**
 * Functional interface for calculating scaled values from enchantment levels.
 * 
 * <p>Scaling formulas determine how enchantment effects grow with level.
 * Common patterns include linear growth (like Sharpness), exponential growth,
 * or diminishing returns for high-level enchantments.
 *
 * <p><strong>Built-in Formulas:</strong>
 * <ul>
 *   <li>{@link #linear(double, double)} - Constant increment per level (Sharpness-style)</li>
 *   <li>{@link #exponential(double, double)} - Percentage growth per level</li>
 *   <li>{@link #diminishing(double, double)} - Approach a maximum asymptotically</li>
 *   <li>{@link #constant(double)} - Same value at all levels</li>
 *   <li>{@link #stepped(java.util.Map)} - Custom values at specific levels with interpolation</li>
 * </ul>
 *
 * <p><strong>Custom Formulas:</strong>
 * <pre>{@code
 * LevelScaling custom = LevelScaling.of(level -> level * level * 0.5);
 * }</pre>
 *
 * @see LinearScaling
 * @see ExponentialScaling
 * @see DiminishingScaling
 * @since 0.1.0
 */
@FunctionalInterface
public interface LevelScaling {

    /**
     * Calculates the scaled value for a given enchantment level.
     *
     * @param level the enchantment level (>= 1)
     * @return the calculated value
     * @throws IllegalArgumentException if level is invalid
     * @since 0.1.0
     */
    double calculate(int level);

    /**
     * Creates a scaling from a custom function.
     *
     * @param formula the formula function (level -> value)
     * @return a LevelScaling wrapping the function
     * @since 0.1.0
     */
    @NotNull
    static LevelScaling of(@NotNull Function<Integer, Double> formula) {
        return formula::apply;
    }

    /**
     * Creates linear scaling: base + (level - 1) * increment.
     * 
     * <p>Example: Sharpness-style where each level adds constant damage.
     *
     * @param base the value at level 1
     * @param increment the amount added per level
     * @return a linear scaling instance
     * @since 0.1.0
     */
    @NotNull
    static LevelScaling linear(double base, double increment) {
        return new LinearScaling(base, increment);
    }

    /**
     * Creates exponential scaling: base * multiplier^(level - 1).
     * 
     * <p>Use with caution at high levels as values grow rapidly.
     *
     * @param base the value at level 1
     * @param multiplier the growth factor per level (1.0 = no growth)
     * @return an exponential scaling instance
     * @since 0.1.0
     */
    @NotNull
    static LevelScaling exponential(double base, double multiplier) {
        return new ExponentialScaling(base, multiplier);
    }

    /**
     * Creates diminishing scaling: maxValue * (level / (level + factor)).
     * 
     * <p>Values approach maxValue asymptotically. Higher factor means
     * slower approach to the maximum.
     *
     * @param maxValue the asymptotic maximum value
     * @param scalingFactor controls how quickly values approach max
     * @return a diminishing scaling instance
     * @since 0.1.0
     */
    @NotNull
    static LevelScaling diminishing(double maxValue, double scalingFactor) {
        return new DiminishingScaling(maxValue, scalingFactor);
    }

    /**
     * Creates constant scaling: same value at all levels.
     *
     * @param value the constant value
     * @return a constant scaling instance
     * @since 0.1.0
     */
    @NotNull
    static LevelScaling constant(double value) {
        return new ConstantScaling(value);
    }

    /**
     * Creates stepped scaling with interpolation between defined levels.
     * 
     * <p>Define exact values at specific levels. Values between steps are
     * linearly interpolated. Values above the highest step use the max step value.
     *
     * @param steps map of level -> value pairs (must include level 1)
     * @return a stepped scaling instance
     * @since 0.1.0
     */
    @NotNull
    static LevelScaling stepped(@NotNull java.util.Map<Integer, Double> steps) {
        return new SteppedScaling(steps);
    }
}
