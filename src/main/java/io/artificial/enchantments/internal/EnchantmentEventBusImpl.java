package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.event.EnchantEffectEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Implementation of {@link EnchantmentEventBus}.
 *
 * <p>Provides thread-safe subscription management using {@link ConcurrentHashMap}
 * and {@link CopyOnWriteArrayList}. Subscriptions are sorted by {@link EventPriority}
 * ordinal during dispatch.
 *
 * @since 0.2.0
 */
public class EnchantmentEventBusImpl implements EnchantmentEventBus {

    /**
     * Creates a new enchantment event bus.
     *
     * @since 0.2.0
     */
    public EnchantmentEventBusImpl() {
    }

    private final Map<Class<? extends EnchantEffectEvent>, List<SubscriptionImpl<?>>> subscriptions = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    ) {
        return register(plugin, eventType, EventPriority.NORMAL, false, handler);
    }

    @Override
    @NotNull
    public <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull EventPriority priority,
            @NotNull Consumer<T> handler
    ) {
        return register(plugin, eventType, priority, false, handler);
    }

    @Override
    @NotNull
    public <T extends EnchantEffectEvent> EventSubscription<T> register(
            @NotNull Plugin plugin,
            @NotNull Class<T> eventType,
            @NotNull EventPriority priority,
            boolean ignoreCancelled,
            @NotNull Consumer<T> handler
    ) {
        SubscriptionImpl<T> subscription = new SubscriptionImpl<>(plugin, eventType, priority, ignoreCancelled, handler);
        subscriptions.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscription);
        return subscription;
    }

    @Override
    public void unregisterAll(@NotNull Plugin plugin) {
        for (List<SubscriptionImpl<?>> list : subscriptions.values()) {
            for (SubscriptionImpl<?> subscription : list) {
                if (subscription.getPlugin().equals(plugin)) {
                    subscription.unsubscribe();
                }
            }
            list.removeIf(sub -> !sub.isActive());
        }
        subscriptions.values().removeIf(List::isEmpty);
    }

    @Override
    public <T extends EnchantEffectEvent> void unregister(@NotNull EventSubscription<T> subscription) {
        if (subscription instanceof SubscriptionImpl) {
            subscription.unsubscribe();
            List<SubscriptionImpl<?>> list = subscriptions.get(subscription.getEventType());
            if (list != null) {
                list.removeIf(sub -> sub == subscription || !sub.isActive());
                if (list.isEmpty()) {
                    subscriptions.remove(subscription.getEventType());
                }
            }
        }
    }

    @Override
    @NotNull
    public <T extends EnchantEffectEvent> T dispatch(@NotNull T event) {
        @SuppressWarnings("unchecked")
        Class<T> eventType = (Class<T>) event.getClass();
        List<SubscriptionImpl<?>> list = subscriptions.get(eventType);
        if (list == null || list.isEmpty()) {
            return event;
        }

        List<SubscriptionImpl<?>> sorted = list.stream()
                .filter(SubscriptionImpl::isActive)
                .sorted((a, b) -> Integer.compare(a.getPriority().ordinal(), b.getPriority().ordinal()))
                .toList();

        for (SubscriptionImpl<?> sub : sorted) {
            if (!sub.isActive()) {
                continue;
            }
            if (sub.isIgnoreCancelled() && event.isCancellable() && event.checkCancelled()) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Consumer<T> handler = (Consumer<T>) sub.getHandler();
            try {
                handler.accept(event);
            } catch (Exception e) {
                sub.getPlugin().getLogger().log(Level.WARNING,
                        "Error handling event " + eventType.getName() + " in plugin " + sub.getPlugin().getName(), e);
            }
        }

        return event;
    }

    @Override
    @NotNull
    public <T extends EnchantEffectEvent> T dispatch(@NotNull Class<T> type, @NotNull java.util.function.Supplier<T> factory) {
        T event = factory.get();
        if (event == null) {
            throw new IllegalArgumentException("Factory must not return null");
        }
        return dispatch(event);
    }

    /**
     * Internal subscription implementation.
     *
     * @param <T> the event type
     */
    private static class SubscriptionImpl<T extends EnchantEffectEvent> implements EventSubscription<T> {
        private final Plugin plugin;
        private final Class<T> eventType;
        private final EventPriority priority;
        private final boolean ignoreCancelled;
        private final Consumer<T> handler;
        private volatile boolean active = true;

        SubscriptionImpl(
                @NotNull Plugin plugin,
                @NotNull Class<T> eventType,
                @NotNull EventPriority priority,
                boolean ignoreCancelled,
                @NotNull Consumer<T> handler
        ) {
            this.plugin = plugin;
            this.eventType = eventType;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.handler = handler;
        }

        @NotNull
        Plugin getPlugin() {
            return plugin;
        }

        @NotNull
        Consumer<T> getHandler() {
            return handler;
        }

        @Override
        @NotNull
        public Class<T> getEventType() {
            return eventType;
        }

        @Override
        @NotNull
        public EventPriority getPriority() {
            return priority;
        }

        @Override
        public boolean isIgnoreCancelled() {
            return ignoreCancelled;
        }

        @Override
        public void unsubscribe() {
            this.active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
