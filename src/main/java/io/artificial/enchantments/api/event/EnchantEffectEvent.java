package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class EnchantEffectEvent extends Event {

    private final EnchantmentDefinition enchantment;
    private final int level;
    private final double scaledValue;

    protected EnchantEffectEvent(@NotNull EnchantmentDefinition enchantment, int level, double scaledValue) {
        this.enchantment = enchantment;
        this.level = level;
        this.scaledValue = scaledValue;
    }

    protected EnchantEffectEvent(boolean isAsync, @NotNull EnchantmentDefinition enchantment, int level, double scaledValue) {
        super(isAsync);
        this.enchantment = enchantment;
        this.level = level;
        this.scaledValue = scaledValue;
    }

    @NotNull
    public EnchantmentDefinition getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    public double getScaledValue() {
        return scaledValue;
    }

    public boolean isCancellable() {
        return this instanceof org.bukkit.event.Cancellable;
    }

    public boolean checkCancelled() {
        if (!isCancellable()) {
            throw new IllegalStateException("This event type is not cancellable");
        }
        return ((org.bukkit.event.Cancellable) this).isCancelled();
    }

    @NotNull
    @Override
    public abstract HandlerList getHandlers();

    @NotNull
    public static HandlerList getHandlerList() {
        throw new UnsupportedOperationException("Subclasses must implement getHandlerList()");
    }
}
