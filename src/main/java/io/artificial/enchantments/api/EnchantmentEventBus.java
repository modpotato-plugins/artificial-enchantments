package io.artificial.enchantments.api;

import io.artificial.enchantments.api.event.EnchantEffectEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public interface EnchantmentEventBus {

    @NotNull
    <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    );

    @NotNull
    <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull EventPriority priority,
            @NotNull Consumer<T> handler
    );

    @NotNull
    <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull EventPriority priority,
            boolean ignoreCancelled,
            @NotNull Consumer<T> handler
    );

    void unregisterAll(@NotNull Plugin plugin);

    <T extends EnchantEffectEvent> void unregister(@NotNull EventSubscription<T> subscription);

    @NotNull
    <T extends EnchantEffectEvent> T dispatch(@NotNull T event);

    @NotNull
    <T extends EnchantEffectEvent> T dispatch(@NotNull Class<T> type, @NotNull java.util.function.Supplier<T> factory);

    interface EventSubscription<T extends EnchantEffectEvent> {
        @NotNull
        Class<T> getEventType();
        @NotNull
        EventPriority getPriority();
        boolean isIgnoreCancelled();
        void unsubscribe();
        boolean isActive();
    }
}
