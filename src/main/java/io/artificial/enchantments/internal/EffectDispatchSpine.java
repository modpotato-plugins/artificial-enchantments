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

import java.util.concurrent.atomic.AtomicReference;
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
    private final AtomicReference<EffectExecutionContext.ExecutionMode> executionMode;

    private volatile boolean shutdown = false;

    /**
     * Creates a new EffectDispatchSpine with the given components.
     *
     * <p>Defaults to LENIENT execution mode where exceptions in effect handlers
     * are logged but do not prevent other handlers from executing.
     *
     * @param scheduler the folia scheduler for thread-safe dispatch
     * @param eventBus the event bus for dispatching events to listeners
     */
    public EffectDispatchSpine(
            @NotNull FoliaScheduler scheduler,
            @NotNull EnchantmentEventBus eventBus
    ) {
        this(scheduler, eventBus, EffectExecutionContext.ExecutionMode.LENIENT);
    }

    /**
     * Creates a new EffectDispatchSpine with the given components and execution mode.
     *
     * @param scheduler the folia scheduler for thread-safe dispatch
     * @param eventBus the event bus for dispatching events to listeners
     * @param executionMode the execution mode (LENIENT or STRICT)
     */
    public EffectDispatchSpine(
            @NotNull FoliaScheduler scheduler,
            @NotNull EnchantmentEventBus eventBus,
            @NotNull EffectExecutionContext.ExecutionMode executionMode
    ) {
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.executionMode = new AtomicReference<>(executionMode);
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

        EffectExecutionContext.ExecutionMode currentMode = executionMode.get();

        try {
            double scaledValue = enchantment.calculateScaledValue(level);

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

            EffectExecutionContext executionContext = new EffectExecutionContext(
                    enchantment.getKey(),
                    eventType.name(),
                    level,
                    currentMode
            );

            return executeDispatch(enchantment, context, bukkitEvent, eventType, executionContext);

        } catch (Exception e) {
            if (currentMode == EffectExecutionContext.ExecutionMode.STRICT) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("Error in strict mode dispatch for " + enchantment.getKey(), e);
                }
            }
            LOGGER.log(Level.SEVERE, "Error dispatching effect for " + enchantment.getKey(), e);
            return false;
        }
    }

    private boolean executeDispatch(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull DispatchEventType eventType,
            @NotNull EffectExecutionContext executionContext
    ) {
        boolean isAsync = bukkitEvent.isAsynchronous();

        if (isAsync) {
            return runDispatch(enchantment, context, bukkitEvent, eventType, executionContext);
        } else {
            return runDispatch(enchantment, context, bukkitEvent, eventType, executionContext);
        }
    }

    private boolean runDispatch(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull DispatchEventType eventType,
            @NotNull EffectExecutionContext executionContext
    ) {
        boolean cancelled = false;

        cancelled = fireTypedCallbackPath(enchantment, context, eventType, executionContext);

        if (cancelled || context.isCancelled()) {
            propagateCancellation(bukkitEvent);
            return false;
        }

        cancelled = fireEventBusPath(enchantment, context, bukkitEvent, eventType, executionContext);

        if (cancelled || context.isCancelled()) {
            propagateCancellation(bukkitEvent);
            return false;
        }

        return true;
    }

    private boolean fireTypedCallbackPath(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull DispatchEventType eventType,
            @NotNull EffectExecutionContext executionContext
    ) {
        EnchantmentEffectHandler handler = enchantment.getEffectHandler();
        if (handler == null) {
            return false;
        }

        AtomicReference<Boolean> cancelled = new AtomicReference<>(false);

        executionContext.executeWithIsolation(
                handler.getClass(),
                () -> {
                    boolean wasCancelled = typedCallbackDispatcher.dispatch(handler, context, eventType);
                    cancelled.set(wasCancelled);
                }
        );

        return cancelled.get();
    }

    private boolean fireEventBusPath(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull DispatchEventType eventType,
            @NotNull EffectExecutionContext executionContext
    ) {
        AtomicReference<Boolean> cancelled = new AtomicReference<>(false);

        executionContext.executeWithIsolation(
                EventBusDispatcher.class,
                () -> {
                    boolean wasCancelled = eventBusDispatcher.dispatch(enchantment, context, bukkitEvent, eventType);
                    cancelled.set(wasCancelled);
                }
        );

        return cancelled.get();
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
     * Gets the current execution mode.
     *
     * @return the current execution mode (LENIENT or STRICT)
     */
    @NotNull
    public EffectExecutionContext.ExecutionMode getExecutionMode() {
        return executionMode.get();
    }

    /**
     * Sets the execution mode for effect handler failures.
     *
     * <p>In LENIENT mode (default), exceptions from effect handlers are logged
     * but do not prevent other handlers from executing.
     *
     * <p>In STRICT mode, exceptions from effect handlers are logged and
     * propagated to stop all further execution. This is useful for debugging.
     *
     * @param mode the execution mode to set
     */
    public void setExecutionMode(@NotNull EffectExecutionContext.ExecutionMode mode) {
        this.executionMode.set(mode);
        LOGGER.info("EffectDispatchSpine execution mode set to: " + mode);
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
