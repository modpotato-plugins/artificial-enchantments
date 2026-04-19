package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.TreeMap;

public final class SteppedScaling implements LevelScaling {

    private final TreeMap<Integer, Double> steps;

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
