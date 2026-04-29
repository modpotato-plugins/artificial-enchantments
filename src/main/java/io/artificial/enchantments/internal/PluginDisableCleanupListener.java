package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentEventBus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Cleans up event bus subscriptions when dependent plugins disable.
 */
public final class PluginDisableCleanupListener implements Listener {

    private final EnchantmentEventBus eventBus;

    /**
     * Creates a new cleanup listener for the shared enchantment event bus.
     *
     * @param eventBus event bus instance to clean when plugins disable
     */
    public PluginDisableCleanupListener(@NotNull EnchantmentEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus cannot be null");
    }

    /**
     * Removes all subscriptions owned by the plugin that just disabled.
     *
     * @param event the disable event emitted by Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
        eventBus.unregisterAll(event.getPlugin());
    }
}
