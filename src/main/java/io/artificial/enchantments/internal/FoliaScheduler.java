package io.artificial.enchantments.internal;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Thread-safe scheduler abstraction for Folia and non-Folia servers.
 *
 * <p>This interface provides methods to execute tasks on the appropriate threads:
 * <ul>
 *   <li>Global region thread - for non-location-dependent operations</li>
 *   <li>Region thread - for location-dependent operations</li>
 *   <li>Entity scheduler - for entity-specific operations</li>
 * </ul>
 *
 * <p>Implementations handle the differences between Folia's threaded regions
 * and traditional single-threaded Bukkit servers.
 *
 * @see EffectDispatchSpine
 */
public interface FoliaScheduler {

    /**
     * Executes a task on the global region thread.
     *
     * @param plugin the plugin scheduling the task
     * @param task the task to execute
     */
    void runGlobal(@NotNull Plugin plugin, @NotNull Runnable task);

    /**
     * Executes a task on the region thread for the given location.
     *
     * @param plugin the plugin scheduling the task
     * @param location the location determining which region thread
     * @param task the task to execute
     */
    void runAtLocation(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task);

    /**
     * Executes a task on the entity's scheduler.
     *
     * @param plugin the plugin scheduling the task
     * @param entity the entity to schedule the task for
     * @param task the task to execute
     */
    void runAtEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task);

    /**
     * Schedules a delayed task on the global region thread.
     *
     * @param plugin the plugin scheduling the task
     * @param task the task to execute
     * @param delayTicks the delay in ticks
     * @return a task handle that can be used to cancel the task
     */
    @NotNull
    ScheduledTask runGlobalDelayed(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks);

    /**
     * Schedules a repeating task on the global region thread.
     *
     * @param plugin the plugin scheduling the task
     * @param task the task to execute
     * @param delayTicks the initial delay in ticks
     * @param periodTicks the period between executions in ticks
     * @return a task handle that can be used to cancel the task
     */
    @NotNull
    ScheduledTask runGlobalTimer(@NotNull Plugin plugin, @NotNull Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    /**
     * Returns true if the current thread is the primary server thread.
     *
     * @return true if on main thread
     */
    boolean isPrimaryThread();

    /**
     * Represents a scheduled task that can be cancelled.
     */
    interface ScheduledTask {
        /**
         * Cancels this task.
         */
        void cancel();

        /**
         * Returns true if this task is cancelled.
         *
         * @return true if cancelled
         */
        boolean isCancelled();

        /**
         * Returns the plugin that owns this task.
         *
         * @return the owning plugin
         */
        @NotNull
        Plugin getOwningPlugin();
    }
}
