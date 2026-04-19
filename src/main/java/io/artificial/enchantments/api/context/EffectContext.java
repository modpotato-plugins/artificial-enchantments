package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all effect contexts.
 * 
 * <p>Context objects are passed to {@link io.artificial.enchantments.api.EnchantmentEffectHandler}
 * methods and contain all data relevant to the triggering game event. They provide:
 * <ul>
 *   <li>Enchantment definition and level</li>
 *   <li>Scaled value calculated by the enchantment's formula</li>
 *   <li>Cancellation support (for cancellable events)</li>
 * </ul>
 *
 * <p><strong>Cancellation:</strong><br>
 * Contexts that wrap cancellable Bukkit events implement {@link Cancellable}.
 * Use {@link #tryCancel()} to attempt cancellation, or {@link #isCancelled()}
 * to check status. Cancellation propagates back to the original Bukkit event.
 *
 * @see io.artificial.enchantments.api.EnchantmentEffectHandler
 * @since 0.1.0
 */
public interface EffectContext {

    /**
     * Gets the enchantment definition that triggered this effect.
     *
     * @return the enchantment definition (never null)
     * @since 0.1.0
     */
    @NotNull
    EnchantmentDefinition getEnchantment();

    /**
     * Gets the enchantment level applied to the item.
     *
     * @return the enchantment level (>= 1)
     * @since 0.1.0
     */
    int getLevel();

    /**
     * Gets the scaled value calculated from the enchantment level.
     * 
     * <p>This is the result of applying the enchantment's {@link io.artificial.enchantments.api.scaling.LevelScaling}
     * formula to the current level.
     *
     * @return the scaled value
     * @since 0.1.0
     */
    double getScaledValue();

    /**
     * Checks if this context supports cancellation.
     *
     * @return true if this context implements {@link Cancellable}
     * @since 0.1.0
     */
    boolean isCancellable();

    /**
     * Attempts to cancel the underlying event.
     * 
     * <p>Returns false if the event is not cancellable. Check {@link #isCancellable()}
     * before calling if cancellation status is uncertain.
     *
     * @return true if cancellation was successful
     * @since 0.1.0
     */
    default boolean tryCancel() {
        if (this instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
            return true;
        }
        return false;
    }

    /**
     * Checks if the underlying event has been cancelled.
     *
     * @return true if cancellable and cancelled
     * @since 0.1.0
     */
    default boolean isCancelled() {
        return this instanceof Cancellable cancellable && cancellable.isCancelled();
    }
}
