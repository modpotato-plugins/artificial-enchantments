package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stepped scaling algorithm with linear interpolation between defined levels.
 *
 * <p>This scaling pattern allows defining exact values at specific levels,
 * with values between steps being linearly interpolated. This is useful for
 * enchantments that need specific breakpoints or custom progression curves.
 *
 * <p><strong>Behavior:</strong>
 * <ul>
 *   <li>If level is exactly defined in steps, return that value</li>
 *   <li>If level is between two defined steps, linearly interpolate</li>
 *   <li>If level exceeds the highest step, use the maximum step value</li>
 * </ul>
 *
 * <p><strong>Example:</strong> Mapping level 1->5.0, level 5->10.0, level 10->15.0
 * would produce: 5.0, 6.25, 7.5, 8.75, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 15.0, ...
 *
 * @since 0.1.0
 */
public final class SteppedScaling implements LevelScaling {

    private final TreeMap<Integer, Double> steps;

    /**
     * Creates a new stepped scaling from a map of level-value pairs.
     *
     * <p>The map must not be empty and must contain a value for level 1.
     * The steps are sorted by level for efficient lookup.
     *
     * @param steps map of level -> value pairs (must not be empty, must include level 1)
     * @throws IllegalArgumentException if steps is empty or missing level 1
     */
    public SteppedScaling(@NotNull Map<Integer, Double> steps) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Steps map must not be empty");
        }
        if (!steps.containsKey(1)) {
            throw new IllegalArgumentException("Steps must define a value for level 1");
        }
        this.steps = new TreeMap<>(steps);
    }

    @Override
    public double calculate(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be >= 1, got: " + level);
        }

        Double exact = steps.get(level);
        if (exact != null) {
            return exact;
        }

        Map.Entry<Integer, Double> lower = steps.floorEntry(level);
        Map.Entry<Integer, Double> higher = steps.ceilingEntry(level);

        if (higher == null) {
            return steps.lastEntry().getValue();
        }

        if (lower == null) {
            return higher.getValue();
        }

        if (lower.getKey().equals(higher.getKey())) {
            return lower.getValue();
        }

        double t = (double) (level - lower.getKey()) / (higher.getKey() - lower.getKey());
        return lower.getValue() + t * (higher.getValue() - lower.getValue());
    }

    /**
     * Gets an unmodifiable view of the defined steps.
     *
     * @return map of level -> value pairs (unmodifiable)
     * @since 0.1.0
     */
    @NotNull
    public Map<Integer, Double> getSteps() {
        return java.util.Collections.unmodifiableMap(steps);
    }

    @Override
    @NotNull
    public String toString() {
        return "SteppedScaling[steps=" + steps + "]";
    }
}
