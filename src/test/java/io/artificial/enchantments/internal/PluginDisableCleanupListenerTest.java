package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.event.CombatEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PluginDisableCleanupListener Tests")
class PluginDisableCleanupListenerTest {

    @Test
    @DisplayName("Plugin disable unsubscribes that plugin's event bus listeners")
    void pluginDisableUnsubscribesPluginListeners() {
        EnchantmentEventBus eventBus = new EnchantmentEventBusImpl();
        Plugin plugin = mock(Plugin.class);
        PluginDisableCleanupListener listener = new PluginDisableCleanupListener(eventBus);
        PluginDisableEvent disableEvent = mock(PluginDisableEvent.class);
        when(disableEvent.getPlugin()).thenReturn(plugin);

        EnchantmentEventBus.EventSubscription<CombatEvent> subscription = eventBus.register(
                plugin,
                CombatEvent.class,
                EventPriority.NORMAL,
                combatEvent -> {
                }
        );

        assertTrue(subscription.isActive());

        listener.onPluginDisable(disableEvent);

        assertFalse(subscription.isActive());
    }
}
