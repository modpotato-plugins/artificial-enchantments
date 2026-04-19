package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all enchantment effect events dispatched through the event bus.
 * 
 * <p>Events extend this class to provide type-specific data for different game
 * scenarios (combat, tool usage, etc.). Each event carries the enchantment
 * definition, level, and scaled value that triggered it.
 *
 * <p><strong>Dispatch Order:</strong><br>
 * Events are dispatched after typed callbacks from {@link io.artificial.enchantments.api.EnchantmentEffectHandler}.
 * Both paths use the same dispatch spine for consistency.
 *
 * <p><strong>Cancellation:</strong><br>
 * Subclasses that represent cancellable game events implement {@link org.bukkit.event.Cancellable}.
 * Check {@link #isCancellable()} before calling {@link #checkCancelled()}.
 *
 * @see io.artificial.enchantments.api.EnchantmentEventBus
 * @see CombatEvent
 * @see ToolEvent
 * @since 0.1.0
 */
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
