package io.artificial.enchantments.api;

import io.artificial.enchantments.api.event.EnchantEffectEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/**
 * Event bus for subscribing to enchantment-triggered effects.
 * 
 * <p>This interface provides a publish-subscribe mechanism for listening to
 * enchantment effects. Events are dispatched through the same spine as typed
 * callbacks, ensuring consistent behavior regardless of which approach you use.
 *
 * <p><strong>Dispatch Rules:</strong>
 * <ol>
 *   <li>Typed callbacks fire first (from {@link EnchantmentEffectHandler})</li>
 *   <li>Event bus listeners fire second</li>
 *   <li>Cancellation stops both paths when detected</li>
 *   <li>Listeners can modify event data that syncs back to the context</li>
 * </ol>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Register a listener for combat events
 * EventSubscription<CombatEvent> sub = api.getEventBus().register(
 *     plugin,
 *     CombatEvent.class,
 *     EventPriority.NORMAL,
 *     event -> {
 *         // Only handle our custom enchant
 *         if (!event.getEnchantment().getKey().equals(MY_ENCHANT_KEY)) return;
 *         
 *         // Modify the final damage
 *         double multiplier = event.getScaledValue();
 *         event.setFinalDamage(event.getFinalDamage() * multiplier);
 *     }
 * );
 * 
 * // Unsubscribe later
 * sub.unsubscribe();
 * }</pre>
 *
 * @see EnchantEffectEvent
 * @see EnchantmentEffectHandler
 * @since 0.1.0
 */
public interface EnchantmentEventBus {

    /**
     * Registers a simple event handler with default priority and cancellation handling.
     *
     * @param plugin the plugin registering the handler (must not be null)
     * @param eventType the event class to listen for (must not be null)
     * @param handler the handler to invoke (must not be null)
     * @param <T> the event type
     * @return a subscription object for later unsubscription
     * @since 0.1.0
     */
    @NotNull
    <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    );

    /**
     * Registers an event handler with specific priority.
     *
     * @param plugin the plugin registering the handler (must not be null)
     * @param eventType the event class to listen for (must not be null)
     * @param priority the Bukkit event priority (must not be null)
     * @param handler the handler to invoke (must not be null)
     * @param <T> the event type
     * @return a subscription object for later unsubscription
     * @since 0.1.0
     */
    @NotNull
    <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull EventPriority priority,
            @NotNull Consumer<T> handler
    );

    /**
     * Registers an event handler with full control over priority and cancellation.
     *
     * @param plugin the plugin registering the handler (must not be null)
     * @param eventType the event class to listen for (must not be null)
     * @param priority the Bukkit event priority (must not be null)
     * @param ignoreCancelled if true, handler won't fire for cancelled events
     * @param handler the handler to invoke (must not be null)
     * @param <T> the event type
     * @return a subscription object for later unsubscription
     * @since 0.1.0
     */
    @NotNull
    <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull EventPriority priority,
            boolean ignoreCancelled,
            @NotNull Consumer<T> handler
    );

    /**
     * Unregisters all handlers for a plugin.
     *
     * @param plugin the plugin to unregister (must not be null)
     * @since 0.1.0
     */
    void unregisterAll(@NotNull Plugin plugin);

    /**
     * Unregisters a specific subscription.
     *
     * @param subscription the subscription to cancel (must not be null)
     * @param <T> the event type
     * @since 0.1.0
     */
    <T extends EnchantEffectEvent> void unregister(@NotNull EventSubscription<T> subscription);

    /**
     * Dispatches an event to all registered handlers.
     * 
     * <p>Typically called internally by the library. Plugins can use this
     * for testing or custom event scenarios.
     *
     * @param event the event to dispatch (must not be null)
     * @param <T> the event type
     * @return the dispatched event (may be modified by handlers)
     * @since 0.1.0
     */
    @NotNull
    <T extends EnchantEffectEvent> T dispatch(@NotNull T event);

    /**
     * Dispatches an event created by a factory function.
     * 
     * <p>Convenience method that creates and dispatches in one call.
     *
     * @param type the event class (must not be null)
     * @param factory the factory to create the event (must not be null)
     * @param <T> the event type
     * @return the dispatched event
     * @since 0.1.0
     */
    @NotNull
    <T extends EnchantEffectEvent> T dispatch(@NotNull Class<T> type, @NotNull java.util.function.Supplier<T> factory);

    /**
     * Represents a subscription to an event type.
     * 
     * <p>Use {@link #unsubscribe()} to cancel the subscription and stop receiving events.
     *
     * @param <T> the event type
     * @since 0.1.0
     */
    interface EventSubscription<T extends EnchantEffectEvent> {
        /**
         * Gets the subscribed event type.
         *
         * @return the event class
         * @since 0.1.0
         */
        @NotNull
        Class<T> getEventType();

        /**
         * Gets the listener priority.
         *
         * @return the event priority
         * @since 0.1.0
         */
        @NotNull
        EventPriority getPriority();

        /**
         * Checks if this subscription ignores cancelled events.
         *
         * @return true if ignoring cancelled events
         * @since 0.1.0
         */
        boolean isIgnoreCancelled();

        /**
         * Unsubscribes this listener from receiving further events.
         *
         * @since 0.1.0
         */
        void unsubscribe();

        /**
         * Checks if this subscription is still active.
         *
         * @return true if still receiving events
         * @since 0.1.0
         */
        boolean isActive();
    }
}
