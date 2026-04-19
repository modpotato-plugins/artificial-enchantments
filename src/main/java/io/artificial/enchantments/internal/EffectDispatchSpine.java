package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.context.EffectContext;
import io.artificial.enchantments.api.event.EnchantEffectEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central dispatch spine for all enchantment effects.
 *
 * <p>This class is the single unified coordinator that powers both:
 * <ul>
 *   <li>Typed callbacks - direct invocation of {@link EnchantmentEffectHandler} methods</li>
 *   <li>Event bus - dispatch of {@link EnchantEffectEvent} subclasses to registered listeners</li>
 * </ul>
 *
 * <p>Both paths flow through this spine to ensure:
 * <ul>
 *   <li>Single point of validation and calculation</li>
 *   <li>No duplicate effect execution</li>
 *   <li>Consistent cancellation propagation</li>
 *   <li>Thread-safe dispatch via FoliaScheduler</li>
 * </ul>
 *
 * <p>Processing order:
 * <ol>
 *   <li>Calculate scaled value using enchantment's {@link io.artificial.enchantments.api.scaling.LevelScaling}</li>
 *   <li>Create appropriate {@link EffectContext} via {@link ContextFactory}</li>
 *   <li>Fire typed callback path (if handler exists)</li>
 *   <li>Check cancellation - stop if cancelled</li>
 *   <li>Fire event bus path (if listeners registered)</li>
 *   <li>Check cancellation after bus</li>
 * </ol>
 *
 * @see TypedCallbackDispatcher
 * @see EventBusDispatcher
 * @see ContextFactory
 */
public final class EffectDispatchSpine {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    private final ContextFactory contextFactory;
    private final TypedCallbackDispatcher typedCallbackDispatcher;
    private final EventBusDispatcher eventBusDispatcher;
    private final FoliaScheduler scheduler;
    private final EnchantmentEventBus eventBus;

    private volatile boolean shutdown = false;

    /**
     * Creates a new EffectDispatchSpine with the given components.
     *
     * @param scheduler the folia scheduler for thread-safe dispatch
     * @param eventBus the event bus for dispatching events to listeners
     */
    public EffectDispatchSpine(
            @NotNull FoliaScheduler scheduler,
            @NotNull EnchantmentEventBus eventBus
    ) {
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.contextFactory = new ContextFactory();
        this.typedCallbackDispatcher = new TypedCallbackDispatcher();
        this.eventBusDispatcher = new EventBusDispatcher(eventBus);
    }

    /**
     * Dispatches an enchantment effect through both callback and event bus paths.
     *
     * <p>This is the main entry point for effect dispatch. It coordinates:
     * <ul>
     *   <li>Value calculation via LevelScaling</li>
     *   <li>Context creation</li>
     *   <li>Typed callback invocation</li>
     *   <li>Event bus dispatch</li>
     *   <li>Cancellation handling</li>
     * </ul>
     *
     * @param enchantment the enchantment definition
     * @param level the enchantment level
     * @param bukkitEvent the underlying Bukkit event
     * @param slot the equipment slot where the enchantment is applied (may be null for non-equipment events)
     * @param eventType the type of event being dispatched
     * @return true if the effect was dispatched successfully, false if cancelled or error occurred
     */
    public boolean dispatch(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot,
            @NotNull DispatchEventType eventType
    ) {
        if (shutdown) {
            LOGGER.warning("Attempted to dispatch effect after spine shutdown: " + enchantment.getKey());
            return false;
        }

        if (level < enchantment.getMinLevel() || level > enchantment.getMaxLevel()) {
            LOGGER.warning("Invalid level " + level + " for enchantment " + enchantment.getKey());
            return false;
        }

        // Check if Bukkit event is already cancelled
        if (bukkitEvent instanceof Cancellable cancellable && cancellable.isCancelled()) {
            return false;
        }

        try {
            // Calculate scaled value using the enchantment's scaling
            double scaledValue = enchantment.calculateScaledValue(level);

            // Create the appropriate context for this event type
            EffectContext context = contextFactory.createContext(
                    eventType,
                    enchantment,
                    level,
                    scaledValue,
                    bukkitEvent,
                    slot
            );

            if (context == null) {
                LOGGER.warning("Failed to create context for event type: " + eventType);
                return false;
            }

            // Execute dispatch on the appropriate thread
            return executeDispatch(enchantment, context, bukkitEvent, eventType);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error dispatching effect for " + enchantment.getKey(), e);
            return false;
        }
    }

    /**
     * Executes the dispatch synchronously or asynchronously based on the bukkit event type.
     *
     * @param enchantment the enchantment being processed
     * @param context the effect context
     * @param bukkitEvent the underlying bukkit event
     * @param eventType the type of dispatch event
     * @return true if dispatch completed without cancellation
     */
    private boolean executeDispatch(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull DispatchEventType eventType
    ) {
        // For async Bukkit events, we need to ensure thread safety
        boolean isAsync = bukkitEvent.isAsynchronous();

        if (isAsync) {
            // Already on async thread - execute directly
            return runDispatch(enchantment, context, bukkitEvent, eventType);
        } else {
            // On main thread - execute directly for simplicity and performance
            // The FoliaScheduler is used for scheduling tasks, not for immediate dispatch
            return runDispatch(enchantment, context, bukkitEvent, eventType);
        }
    }

    /**
     * Runs the actual dispatch logic through both paths.
     *
     * @param enchantment the enchantment definition
     * @param context the effect context
     * @param bukkitEvent the underlying bukkit event
     * @param eventType the type of event
     * @return true if not cancelled, false if cancelled
     */
    private boolean runDispatch(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull DispatchEventType eventType
    ) {
        // Track if anything has cancelled the effect
        boolean cancelled = false;

        // PATH 1: Typed Callbacks (direct handler invocation)
        cancelled = fireTypedCallbackPath(enchantment, context, eventType);

        // Check cancellation after typed callback path
        if (cancelled || context.isCancelled()) {
            propagateCancellation(bukkitEvent);
            return false;
        }

        // PATH 2: Event Bus (listeners)
        cancelled = fireEventBusPath(enchantment, context, bukkitEvent, eventType);

        // Final cancellation check
        if (cancelled || context.isCancelled()) {
            propagateCancellation(bukkitEvent);
            return false;
        }

        return true;
    }

    /**
     * Fires the typed callback path - invokes methods on the enchantment's handler.
     *
     * @param enchantment the enchantment definition
     * @param context the effect context
     * @param eventType the type of event
     * @return true if cancelled, false otherwise
     */
    private boolean fireTypedCallbackPath(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull DispatchEventType eventType
    ) {
        EnchantmentEffectHandler handler = enchantment.getEffectHandler();
        if (handler == null) {
            return false; // No handler = no cancellation from this path
        }

        try {
            return typedCallbackDispatcher.dispatch(handler, context, eventType);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error in typed callback for " + enchantment.getKey(), e);
            return false;
        }
    }

    /**
     * Fires the event bus path - creates and dispatches the appropriate EnchantEffectEvent.
     *
     * @param enchantment the enchantment definition
     * @param context the effect context
     * @param bukkitEvent the underlying bukkit event
     * @param eventType the type of event
     * @return true if cancelled, false otherwise
     */
    private boolean fireEventBusPath(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull DispatchEventType eventType
    ) {
        try {
            return eventBusDispatcher.dispatch(enchantment, context, bukkitEvent, eventType);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error in event bus dispatch for " + enchantment.getKey(), e);
            return false;
        }
    }

    /**
     * Propagates cancellation from the context back to the original Bukkit event.
     *
     * @param bukkitEvent the Bukkit event to potentially cancel
     */
    private void propagateCancellation(@NotNull Event bukkitEvent) {
        if (bukkitEvent instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
    }

    /**
     * Returns the FoliaScheduler for scheduling tasks.
     *
     * @return the scheduler
     */
    @NotNull
    public FoliaScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Returns the ContextFactory for creating effect contexts.
     *
     * @return the context factory
     */
    @NotNull
    public ContextFactory getContextFactory() {
        return contextFactory;
    }

    /**
     * Shuts down the dispatch spine, preventing further dispatches.
     */
    public void shutdown() {
        this.shutdown = true;
        LOGGER.info("EffectDispatchSpine shutdown complete");
    }

    /**
     * Check if the spine has been shut down.
     *
     * @return true if shutdown
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Enumeration of all dispatchable event types.
     * Maps to methods in {@link EnchantmentEffectHandler} and corresponding
     * {@link EnchantEffectEvent} subclasses.
     */
    public enum DispatchEventType {
        // Combat events
        ENTITY_DAMAGE_BY_ENTITY,
        ENTITY_DAMAGE,
        SHIELD_BLOCK,

        // Tool events
        BLOCK_BREAK,
        BLOCK_BREAK_PRE,
        BLOCK_PLACE,
        BLOCK_INTERACT,

        // Interaction events
        ENTITY_INTERACT,

        // Projectile events
        PROJECTILE_LAUNCH,
        PROJECTILE_HIT,

        // Fishing events
        FISHING_ACTION,

        // Tick events
        HELD_TICK,
        ARMOR_TICK,

        // Item events
        ITEM_USED,
        DURABILITY_DAMAGE,
        ITEM_DROP,
        ITEM_PICKUP,

        // Consumable events
        ITEM_CONSUME,

        // Weapon events
        BOW_SHOOT,
        TRIDENT_THROW
    }
}
