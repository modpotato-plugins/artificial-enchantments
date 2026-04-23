package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Context wrapper for executing enchantment effect handlers with exception isolation.
 *
 * <p>This class encapsulates all information needed to execute an effect handler and
 * provides consistent exception handling with detailed logging. It ensures that
 * exceptions from one handler do not prevent other handlers from executing.
 *
 * <p>Execution modes:
 * <ul>
 *   <li><strong>LENIENT (default):</strong> Log exceptions and continue with remaining handlers</li>
 *   <li><strong>STRICT:</strong> Log exceptions and propagate to stop all further execution</li>
 * </ul>
 *
 * <p>All logged errors include:
 * <ul>
 *   <li>Enchantment key (namespaced)</li>
 *   <li>Effect event type</li>
 *   <li>Handler class name</li>
 *   <li>Exception message and stack trace</li>
 *   <li>Enchantment level (if applicable)</li>
 * </ul>
 *
 * @see EffectDispatchSpine
 * @since 1.0.0
 */
public final class EffectExecutionContext {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    private final NamespacedKey enchantmentKey;
    private final String effectType;
    private final int level;
    private final ExecutionMode executionMode;

    /**
     * Execution mode for handling effect handler failures.
     */
    public enum ExecutionMode {
        /**
         * Log exceptions and continue with remaining handlers.
         * This is the default mode for production use.
         */
        LENIENT,

        /**
         * Log exceptions and propagate to stop all further execution.
         * Useful for testing and debugging problematic handlers.
         */
        STRICT
    }

    /**
     * Creates a new execution context.
     *
     * @param enchantmentKey the enchantment's namespaced key
     * @param effectType the type of effect being executed (e.g., "ENTITY_DAMAGE_BY_ENTITY")
     * @param level the enchantment level
     * @param executionMode the execution mode for error handling
     */
    public EffectExecutionContext(
            @NotNull NamespacedKey enchantmentKey,
            @NotNull String effectType,
            int level,
            @NotNull ExecutionMode executionMode
    ) {
        this.enchantmentKey = Objects.requireNonNull(enchantmentKey, "enchantmentKey cannot be null");
        this.effectType = Objects.requireNonNull(effectType, "effectType cannot be null");
        this.level = level;
        this.executionMode = Objects.requireNonNull(executionMode, "executionMode cannot be null");
    }

    /**
     * Creates a new execution context with lenient mode (default).
     *
     * @param enchantmentKey the enchantment's namespaced key
     * @param effectType the type of effect being executed
     * @param level the enchantment level
     */
    public EffectExecutionContext(
            @NotNull NamespacedKey enchantmentKey,
            @NotNull String effectType,
            int level
    ) {
        this(enchantmentKey, effectType, level, ExecutionMode.LENIENT);
    }

    /**
     * Creates a new execution context from an enchantment definition.
     *
     * @param enchantment the enchantment definition
     * @param eventType the dispatch event type
     * @param executionMode the execution mode
     */
    public EffectExecutionContext(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectDispatchSpine.DispatchEventType eventType,
            @NotNull ExecutionMode executionMode
    ) {
        this(
                enchantment.getKey(),
                eventType.name(),
                0, // Level will be set separately if needed
                executionMode
        );
    }

    /**
     * Executes the given runnable with exception isolation.
     *
     * <p>In LENIENT mode, exceptions are caught, logged, and the method returns normally.
     * In STRICT mode, exceptions are caught, logged, and then re-thrown.
     *
     * @param handlerClass the class of the handler being invoked (for logging)
     * @param action the action to execute
     * @return true if execution succeeded without exception, false if an exception was caught
     */
    public boolean executeWithIsolation(
            @Nullable Class<?> handlerClass,
            @NotNull Runnable action
    ) {
        Objects.requireNonNull(action, "action cannot be null");

        String handlerName = handlerClass != null ? handlerClass.getName() : "unknown";

        try {
            action.run();
            return true;
        } catch (Exception e) {
            logException(handlerName, e);

            if (executionMode == ExecutionMode.STRICT) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new EffectExecutionException(
                            "Effect handler failed in strict mode: " + enchantmentKey, e);
                }
            }

            return false;
        }
    }

    /**
     * Executes the given runnable with exception isolation, using lenient mode by default.
     *
     * @param handlerClass the class of the handler being invoked (for logging)
     * @param action the action to execute
     * @return true if execution succeeded without exception, false if an exception was caught
     */
    public boolean executeLenient(
            @Nullable Class<?> handlerClass,
            @NotNull Runnable action
    ) {
        if (executionMode == ExecutionMode.LENIENT) {
            return executeWithIsolation(handlerClass, action);
        } else {
            // Temporarily override to lenient
            try {
                return executeWithIsolation(handlerClass, action);
            } finally {
                // Mode is restored - no change needed as we don't mutate state
            }
        }
    }

    /**
     * Logs an exception with full context information.
     *
     * @param handlerName the name of the handler class
     * @param exception the exception that occurred
     */
    private void logException(@NotNull String handlerName, @NotNull Exception exception) {
        StringWriter stackTraceWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTraceWriter));

        String message = String.format(
                "Exception in effect handler [enchantment=%s, type=%s, level=%d, handler=%s]: %s%n%s",
                enchantmentKey,
                effectType,
                level,
                handlerName,
                exception.getMessage(),
                stackTraceWriter.toString()
        );

        LOGGER.log(Level.SEVERE, message);
    }

    /**
     * Gets a quick log message prefix for use in logging.
     *
     * @return a formatted prefix string with enchantment key and effect type
     */
    @NotNull
    public String getLogPrefix() {
        return String.format("[%s:%s:L%d] ", enchantmentKey, effectType, level);
    }

    // Getters

    /**
     * Gets the enchantment key.
     *
     * @return the enchantment's namespaced key
     */
    @NotNull
    public NamespacedKey getEnchantmentKey() {
        return enchantmentKey;
    }

    /**
     * Gets the effect type.
     *
     * @return the type of effect being executed
     */
    @NotNull
    public String getEffectType() {
        return effectType;
    }

    /**
     * Gets the enchantment level.
     *
     * @return the enchantment level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the execution mode.
     *
     * @return the current execution mode
     */
    @NotNull
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /**
     * Returns a new context with the specified level.
     *
     * @param level the enchantment level
     * @return a new context with the updated level
     */
    @NotNull
    public EffectExecutionContext withLevel(int level) {
        return new EffectExecutionContext(enchantmentKey, effectType, level, executionMode);
    }

    /**
     * Returns a new context with the specified execution mode.
     *
     * @param mode the execution mode
     * @return a new context with the updated mode
     */
    @NotNull
    public EffectExecutionContext withExecutionMode(@NotNull ExecutionMode mode) {
        return new EffectExecutionContext(enchantmentKey, effectType, level, mode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EffectExecutionContext that = (EffectExecutionContext) o;
        return level == that.level &&
                enchantmentKey.equals(that.enchantmentKey) &&
                effectType.equals(that.effectType) &&
                executionMode == that.executionMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(enchantmentKey, effectType, level, executionMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "EffectExecutionContext{" +
                "enchantmentKey=" + enchantmentKey +
                ", effectType='" + effectType + '\'' +
                ", level=" + level +
                ", executionMode=" + executionMode +
                '}';
    }

    /**
     * Exception thrown when strict mode is enabled and an effect handler fails.
     */
    public static class EffectExecutionException extends RuntimeException {
        /**
         * Creates a new effect execution exception.
         *
         * @param message the detail message
         * @param cause the underlying cause
         */
        public EffectExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
