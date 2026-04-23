package io.artificial.enchantments.internal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Bukkit implementation of {@link FoliaScheduler}.
 *
 * <p>Wraps the standard Bukkit scheduler to provide the FoliaScheduler
 * abstraction. On traditional single-threaded Bukkit servers, all tasks
 * run on the main server thread regardless of location or entity.
 */
public final class BukkitFoliaScheduler implements FoliaScheduler {

    @Override
    public void runGlobal(@NotNull Plugin plugin, @NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAtLocation(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAtEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public @NotNull ScheduledTask runGlobalDelayed(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        return new BukkitScheduledTask(bukkitTask);
    }

    @Override
    public @NotNull ScheduledTask runGlobalTimer(@NotNull Plugin plugin, @NotNull Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        ScheduledTask[] wrapper = new ScheduledTask[1];
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (wrapper[0] != null) {
                task.accept(wrapper[0]);
            }
        }, delayTicks, periodTicks);
        wrapper[0] = new BukkitScheduledTask(bukkitTask);
        return wrapper[0];
    }

    @Override
    public boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread();
    }

    private record BukkitScheduledTask(BukkitTask bukkitTask) implements ScheduledTask {

        @Override
        public void cancel() {
            bukkitTask.cancel();
        }

        @Override
        public boolean isCancelled() {
            return !Bukkit.getScheduler().isCurrentlyRunning(bukkitTask.getTaskId())
                    && !Bukkit.getScheduler().isQueued(bukkitTask.getTaskId());
        }

        @Override
        public @NotNull Plugin getOwningPlugin() {
            return bukkitTask.getOwner();
        }
    }
}
