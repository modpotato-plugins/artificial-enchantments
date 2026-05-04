package io.artificial.enchantments.internal;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * FoliaLib-backed implementation of {@link FoliaScheduler}.
 *
 * <p>On Folia this delegates to region/entity/global schedulers through
 * FoliaLib. On Paper/Purpur it resolves to the normal main-thread scheduler.
 */
public final class BukkitFoliaScheduler implements FoliaScheduler {

    private final FoliaLib foliaLib;

    /**
     * Creates a new Bukkit folia scheduler.
     *
     * @param plugin the owning plugin
     */
    public BukkitFoliaScheduler(@NotNull Plugin plugin) {
        this.foliaLib = new FoliaLib(plugin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runGlobal(@NotNull Plugin plugin, @NotNull Runnable task) {
        scheduler().runNextTick(ignored -> task.run());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runAtLocation(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task) {
        scheduler().runAtLocation(location, ignored -> task.run());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runAtEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        scheduler().runAtEntity(entity, ignored -> task.run());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ScheduledTask runGlobalDelayed(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks) {
        return new WrappedScheduledTask(scheduler().runLater(task, delayTicks));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ScheduledTask runGlobalTimer(@NotNull Plugin plugin, @NotNull Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        final WrappedScheduledTask[] handle = new WrappedScheduledTask[1];
        WrappedTask wrappedTask = scheduler().runTimer(() -> {
            if (handle[0] != null) {
                task.accept(handle[0]);
            }
        }, delayTicks, periodTicks);
        handle[0] = new WrappedScheduledTask(wrappedTask);
        return handle[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread() || scheduler().isGlobalTickThread();
    }

    @NotNull
    private PlatformScheduler scheduler() {
        return foliaLib.getScheduler();
    }

    private record WrappedScheduledTask(WrappedTask task) implements ScheduledTask {

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public @NotNull Plugin getOwningPlugin() {
            return task.getOwningPlugin();
        }
    }
}
