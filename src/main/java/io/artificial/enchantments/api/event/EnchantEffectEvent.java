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

    /**
     * Creates a new enchant effect event.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     */
    protected EnchantEffectEvent(@NotNull EnchantmentDefinition enchantment, int level, double scaledValue) {
        this.enchantment = enchantment;
        this.level = level;
        this.scaledValue = scaledValue;
    }

    /**
     * Creates a new enchant effect event with async flag.
     *
     * @param isAsync whether the event is async
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     */
    protected EnchantEffectEvent(boolean isAsync, @NotNull EnchantmentDefinition enchantment, int level, double scaledValue) {
        super(isAsync);
        this.enchantment = enchantment;
        this.level = level;
        this.scaledValue = scaledValue;
    }

    /**
     * Gets the enchantment definition that triggered this event.
     *
     * @return the enchantment definition
     */
    @NotNull
    public EnchantmentDefinition getEnchantment() {
        return enchantment;
    }

    /**
     * Gets the level of the triggering enchantment.
     *
     * @return the enchantment level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the scaled value from the enchantment's scaling algorithm.
     *
     * @return the scaled value
     */
    public double getScaledValue() {
        return scaledValue;
    }

    /**
     * Checks if this event type is cancellable.
     *
     * @return true if this event implements {@link org.bukkit.event.Cancellable}
     */
    public boolean isCancellable() {
        return this instanceof org.bukkit.event.Cancellable;
    }

    /**
     * Checks if this event has been cancelled.
     *
     * @return true if cancelled
     * @throws IllegalStateException if this event type is not cancellable
     */
    public boolean checkCancelled() {
        if (!isCancellable()) {
            throw new IllegalStateException("This event type is not cancellable");
        }
        return ((org.bukkit.event.Cancellable) this).isCancelled();
    }

    @NotNull
    @Override
    public abstract HandlerList getHandlers();

    /**
     * Gets the handler list for this event type.
     *
     * @return the handler list
     * @throws UnsupportedOperationException if called on the base class
     */
    @NotNull
    public static HandlerList getHandlerList() {
        throw new UnsupportedOperationException("Subclasses must implement getHandlerList()");
    }
}
