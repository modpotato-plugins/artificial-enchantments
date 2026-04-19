package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;
import java.util.function.Function;

@FunctionalInterface
public interface LevelScaling {

    double calculate(int level);

    @NotNull
    static LevelScaling of(@NotNull Function<Integer, Double> formula) {
        return formula::apply;
    }

    @NotNull
    static LevelScaling linear(double base, double increment) {
        return new LinearScaling(base, increment);
    }

    @NotNull
    static LevelScaling exponential(double base, double multiplier) {
        return new ExponentialScaling(base, multiplier);
    }

    @NotNull
    static LevelScaling diminishing(double maxValue, double scalingFactor) {
        return new DiminishingScaling(maxValue, scalingFactor);
    }

    @NotNull
    static LevelScaling constant(double value) {
        return new ConstantScaling(value);
    }

    @NotNull
    static LevelScaling stepped(@NotNull java.util.Map<Integer, Double> steps) {
        return new SteppedScaling(steps);
    }
}
