package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

public final class ConstantScaling implements LevelScaling {

    private final double value;

    public ConstantScaling(double value) {
        this.value = value;
    }

    @Override
    public double calculate(int level) {
        return value;
    }

    public double getValue() {
        return value;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("ConstantScaling[value=%.2f]", value);
    }
}
