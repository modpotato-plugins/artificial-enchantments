package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.context.CombatContext;
import io.artificial.enchantments.api.context.EffectContext;
import io.artificial.enchantments.api.context.ItemContext;
import io.artificial.enchantments.api.event.CombatEvent;
import io.artificial.enchantments.api.event.EnchantEffectEvent;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.bukkit.damage.DamageSource;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EffectDispatchSpine Tests")
class EffectDispatchSpineTest {

    private EffectDispatchSpine spine;
    private MockFoliaScheduler scheduler;
    private MockEnchantmentEventBus eventBus;
    private static final String TEST_NS = "test";

    @BeforeEach
    void setUp() {
        scheduler = new MockFoliaScheduler();
        eventBus = new MockEnchantmentEventBus();
        spine = new EffectDispatchSpine(scheduler, eventBus);
    }

    @Test
    @DisplayName("Spine creation with valid dependencies succeeds")
    void spineCreationSucceeds() {
        assertNotNull(spine);
        assertNotNull(spine.getScheduler());
        assertNotNull(spine.getContextFactory());
    }

    @Test
    @DisplayName("Dispatch with invalid level returns false")
    void dispatchWithInvalidLevelReturnsFalse() {
        EnchantmentDefinition enchantment = createTestEnchantment("test", 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        boolean result = spine.dispatch(enchantment, 0, bukkitEvent, EquipmentSlot.HAND, 
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Dispatch with level below min returns false")
    void dispatchWithLevelBelowMinReturnsFalse() {
        EnchantmentDefinition enchantment = createTestEnchantment("test", 3, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        boolean result = spine.dispatch(enchantment, 1, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Dispatch with level above max returns false")
    void dispatchWithLevelAboveMaxReturnsFalse() {
        EnchantmentDefinition enchantment = createTestEnchantment("test", 1, 3);
        Event bukkitEvent = createMockCombatEvent();
        
        boolean result = spine.dispatch(enchantment, 5, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Dispatch with cancelled bukkit event returns false")
    void dispatchWithCancelledEventReturnsFalse() {
        EnchantmentDefinition enchantment = createTestEnchantment("test", 1, 5);
        MockCancellableEvent bukkitEvent = new MockCancellableEvent(false);
        bukkitEvent.setCancelled(true);
        
        boolean result = spine.dispatch(enchantment, 3, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Dispatch with shutdown spine returns false")
    void dispatchWithShutdownSpineReturnsFalse() {
        EnchantmentDefinition enchantment = createTestEnchantment("test", 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        spine.shutdown();
        boolean result = spine.dispatch(enchantment, 3, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertFalse(result);
        assertTrue(spine.isShutdown());
    }

    @Test
    @DisplayName("Dispatch fires typed callback handler")
    void dispatchFiresTypedCallback() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        EnchantmentEffectHandler handler = new EnchantmentEffectHandler() {
            @Override
            public void onEntityDamageByEntity(CombatContext context) {
                handlerCalled.set(true);
            }
        };
        
        EnchantmentDefinition enchantment = createTestEnchantmentWithHandler("callback_test", handler, 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        spine.dispatch(enchantment, 3, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertTrue(handlerCalled.get());
    }

    @Test
    @DisplayName("Dispatch fires event bus")
    void dispatchFiresEventBus() {
        AtomicBoolean eventFired = new AtomicBoolean(false);
        eventBus.setListener(event -> {
            if (event instanceof CombatEvent) {
                eventFired.set(true);
            }
        });
        
        EnchantmentDefinition enchantment = createTestEnchantment("bus_test", 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        spine.dispatch(enchantment, 3, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertTrue(eventFired.get());
    }

    @Test
    @DisplayName("Dispatch returns false when cancelled via callback")
    void dispatchReturnsFalseWhenCancelledViaCallback() {
        EnchantmentEffectHandler handler = new EnchantmentEffectHandler() {
            @Override
            public void onEntityDamageByEntity(CombatContext context) {
                context.tryCancel();
            }
        };
        
        EnchantmentDefinition enchantment = createTestEnchantmentWithHandler("cancel_test", handler, 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        boolean result = spine.dispatch(enchantment, 3, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Dispatch with null handler skips callback path")
    void dispatchWithNullHandlerSkipsCallback() {
        EnchantmentDefinition enchantment = createTestEnchantment("no_handler", 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        boolean result = spine.dispatch(enchantment, 3, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("ContextFactory is available from spine")
    void contextFactoryIsAvailable() {
        assertNotNull(spine.getContextFactory());
    }

    @Test
    @DisplayName("Scheduler is available from spine")
    void schedulerIsAvailable() {
        assertSame(scheduler, spine.getScheduler());
    }

    @Test
    @DisplayName("Scaled value is correctly calculated in context")
    void scaledValueIsCorrectlyCalculated() {
        AtomicReference<Double> capturedValue = new AtomicReference<>();
        EnchantmentEffectHandler handler = new EnchantmentEffectHandler() {
            @Override
            public void onEntityDamageByEntity(CombatContext context) {
                capturedValue.set(context.getScaledValue());
            }
        };
        
        EnchantmentDefinition enchantment = createTestEnchantmentWithHandler("scaling_test", handler, 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        spine.dispatch(enchantment, 3, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        double expected = enchantment.calculateScaledValue(3);
        assertEquals(expected, capturedValue.get(), 0.001);
    }

    @Test
    @DisplayName("Context level is correctly set")
    void contextLevelIsCorrectlySet() {
        AtomicReference<Integer> capturedLevel = new AtomicReference<>();
        EnchantmentEffectHandler handler = new EnchantmentEffectHandler() {
            @Override
            public void onEntityDamageByEntity(CombatContext context) {
                capturedLevel.set(context.getLevel());
            }
        };
        
        EnchantmentDefinition enchantment = createTestEnchantmentWithHandler("level_test", handler, 1, 5);
        Event bukkitEvent = createMockCombatEvent();
        
        spine.dispatch(enchantment, 4, bukkitEvent, EquipmentSlot.HAND,
            EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        
        assertEquals(4, capturedLevel.get());
    }

    @Test
    @DisplayName("Tick dispatch populates typed callback context")
    void tickDispatchPopulatesTypedCallbackContext() {
        AtomicReference<Player> capturedPlayer = new AtomicReference<>();
        AtomicReference<ItemStack> capturedItem = new AtomicReference<>();
        AtomicReference<EquipmentSlot> capturedSlot = new AtomicReference<>();
        AtomicReference<Integer> capturedTickCount = new AtomicReference<>();
        AtomicReference<Long> capturedHeldDuration = new AtomicReference<>();

        EnchantmentEffectHandler handler = new EnchantmentEffectHandler() {
            @Override
            public void onHeldTick(io.artificial.enchantments.api.context.TickContext context) {
                capturedPlayer.set(context.getPlayer());
                capturedItem.set(context.getItem());
                capturedSlot.set(context.getSlot());
                capturedTickCount.set(context.getTickCount());
                capturedHeldDuration.set(context.getHeldDuration());
            }
        };

        EnchantmentDefinition enchantment = createTestEnchantmentWithHandler("tick_test", handler, 1, 5);
        Player player = mock(Player.class);
        ItemStack sword = mock(ItemStack.class);
        when(player.getLocation()).thenReturn(new Location(null, 12, 64, 12));
        when(sword.clone()).thenReturn(sword);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        TickDispatchEvent tickEvent = new TickDispatchEvent(
                player,
                sword,
                EquipmentSlot.HAND,
                true,
                40,
                2_000L
        );

        boolean result = spine.dispatch(
                enchantment,
                2,
                tickEvent,
                EquipmentSlot.HAND,
                EffectDispatchSpine.DispatchEventType.HELD_TICK
        );

        assertTrue(result);
        assertSame(player, capturedPlayer.get());
        assertEquals(Material.DIAMOND_SWORD, capturedItem.get().getType());
        assertEquals(EquipmentSlot.HAND, capturedSlot.get());
        assertEquals(40, capturedTickCount.get());
        assertEquals(2_000L, capturedHeldDuration.get());
    }

    @Test
    @DisplayName("Tick dispatch populates event bus payload")
    void tickDispatchPopulatesEventBusPayload() {
        AtomicReference<io.artificial.enchantments.api.event.TickEvent> capturedEvent = new AtomicReference<>();
        eventBus.setListener(event -> {
            if (event instanceof io.artificial.enchantments.api.event.TickEvent tickEvent) {
                capturedEvent.set(tickEvent);
            }
        });

        EnchantmentDefinition enchantment = createTestEnchantment("tick_bus_test", 1, 5);
        Player player = mock(Player.class);
        ItemStack helmet = mock(ItemStack.class);
        when(player.getLocation()).thenReturn(new Location(null, 0, 64, 0));
        when(helmet.clone()).thenReturn(helmet);
        when(helmet.getType()).thenReturn(Material.DIAMOND_HELMET);
        TickDispatchEvent tickEvent = new TickDispatchEvent(
                player,
                helmet,
                EquipmentSlot.HEAD,
                false,
                60,
                3_000L
        );

        boolean result = spine.dispatch(
                enchantment,
                3,
                tickEvent,
                EquipmentSlot.HEAD,
                EffectDispatchSpine.DispatchEventType.ARMOR_TICK
        );

        assertTrue(result);
        assertNotNull(capturedEvent.get());
        assertSame(player, capturedEvent.get().getPlayer());
        assertEquals(Material.DIAMOND_HELMET, capturedEvent.get().getItem().getType());
        assertEquals(EquipmentSlot.HEAD, capturedEvent.get().getSlot());
        assertEquals(60, capturedEvent.get().getTickCount());
        assertEquals(3_000L, capturedEvent.get().getHeldDuration());
        assertTrue(capturedEvent.get().isArmor());
        assertFalse(capturedEvent.get().isHeld());
    }

    @Test
    @DisplayName("Durability dispatch populates item context and syncs damage")
    void durabilityDispatchPopulatesItemContextAndSyncsDamage() {
        AtomicReference<Player> capturedPlayer = new AtomicReference<>();
        AtomicReference<ItemStack> capturedItem = new AtomicReference<>();
        AtomicReference<Integer> capturedDamage = new AtomicReference<>();

        EnchantmentEffectHandler handler = new EnchantmentEffectHandler() {
            @Override
            public void onDurabilityDamage(ItemContext context) {
                capturedPlayer.set(context.getPlayer());
                capturedItem.set(context.getItem());
                capturedDamage.set(context.getDamageTaken());
                context.reduceDamage(3);
            }
        };

        EnchantmentDefinition enchantment = createTestEnchantmentWithHandler("durability_test", handler, 1, 5);
        Player player = mock(Player.class);
        ItemStack sword = mock(ItemStack.class);
        PlayerItemDamageEvent damageEvent = mock(PlayerItemDamageEvent.class);
        when(player.getLocation()).thenReturn(new Location(null, 3, 64, 3));
        when(sword.clone()).thenReturn(sword);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.getItemMeta()).thenReturn(null);
        when(damageEvent.isCancelled()).thenReturn(false);
        when(damageEvent.getPlayer()).thenReturn(player);
        when(damageEvent.getItem()).thenReturn(sword);
        when(damageEvent.getDamage()).thenReturn(7);

        boolean result = spine.dispatch(
                enchantment,
                2,
                damageEvent,
                EquipmentSlot.HAND,
                EffectDispatchSpine.DispatchEventType.DURABILITY_DAMAGE
        );

        assertTrue(result);
        assertSame(player, capturedPlayer.get());
        assertSame(sword, capturedItem.get());
        assertEquals(7, capturedDamage.get());
        verify(damageEvent).setDamage(4);
    }

    private EnchantmentDefinition createTestEnchantment(String name, int minLevel, int maxLevel) {
        return createTestEnchantmentWithHandler(name, null, minLevel, maxLevel);
    }

    private EnchantmentDefinition createTestEnchantmentWithHandler(String name, EnchantmentEffectHandler handler, int minLevel, int maxLevel) {
        return new TestEnchantmentDefinition(name, handler, minLevel, maxLevel);
    }

    private Event createMockCombatEvent() {
        EntityDamageByEntityEvent mockEvent = mock(EntityDamageByEntityEvent.class);
        LivingEntity mockVictim = mock(LivingEntity.class);
        DamageSource mockSource = mock(DamageSource.class);

        when(mockEvent.isAsynchronous()).thenReturn(false);
        when(mockEvent.isCancelled()).thenReturn(false);
        when(mockEvent.getDamage()).thenReturn(5.0);
        when(mockEvent.getEntity()).thenReturn(mockVictim);
        when(mockEvent.getDamageSource()).thenReturn(mockSource);
        when(mockEvent.getCause()).thenReturn(EntityDamageEvent.DamageCause.CUSTOM);

        return mockEvent;
    }

    private static class MockCancellableEvent extends Event implements Cancellable {
        private boolean cancelled = false;

        MockCancellableEvent(boolean async) {
            super(async);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }

        @Override
        public HandlerList getHandlers() {
            return new HandlerList();
        }
    }

    private static class MockFoliaScheduler implements FoliaScheduler {
        @Override
        public void runGlobal(Plugin plugin, Runnable task) {
            task.run();
        }

        @Override
        public void runAtLocation(Plugin plugin, org.bukkit.Location location, Runnable task) {
            task.run();
        }

        @Override
        public void runAtEntity(Plugin plugin, org.bukkit.entity.Entity entity, Runnable task) {
            task.run();
        }

        @Override
        public ScheduledTask runGlobalDelayed(Plugin plugin, Runnable task, long delayTicks) {
            task.run();
            return new MockScheduledTask(plugin);
        }

        @Override
        public ScheduledTask runGlobalTimer(Plugin plugin, java.util.function.Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
            return new MockScheduledTask(plugin);
        }

        @Override
        public boolean isPrimaryThread() {
            return true;
        }
    }

    private static class MockScheduledTask implements FoliaScheduler.ScheduledTask {
        private final Plugin plugin;
        private boolean cancelled = false;

        MockScheduledTask(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public Plugin getOwningPlugin() {
            return plugin;
        }
    }

    private static class MockEnchantmentEventBus implements EnchantmentEventBus {
        private Consumer<io.artificial.enchantments.api.event.EnchantEffectEvent> listener;

        void setListener(Consumer<io.artificial.enchantments.api.event.EnchantEffectEvent> listener) {
            this.listener = listener;
        }

        @Override
        public <T extends io.artificial.enchantments.api.event.EnchantEffectEvent> EventSubscription<T> register(Plugin plugin, Class<T> eventType, java.util.function.Consumer<T> handler) {
            return new MockEventSubscription<>(eventType);
        }

        @Override
        public <T extends io.artificial.enchantments.api.event.EnchantEffectEvent> EventSubscription<T> register(Plugin plugin, Class<T> eventType, org.bukkit.event.EventPriority priority, java.util.function.Consumer<T> handler) {
            return new MockEventSubscription<>(eventType);
        }

        @Override
        public <T extends io.artificial.enchantments.api.event.EnchantEffectEvent> EventSubscription<T> register(Plugin plugin, Class<T> eventType, org.bukkit.event.EventPriority priority, boolean ignoreCancelled, java.util.function.Consumer<T> handler) {
            return new MockEventSubscription<>(eventType);
        }

        @Override
        public void unregisterAll(Plugin plugin) {}

        @Override
        public <T extends io.artificial.enchantments.api.event.EnchantEffectEvent> void unregister(EventSubscription<T> subscription) {}

        @Override
        public <T extends io.artificial.enchantments.api.event.EnchantEffectEvent> T dispatch(T event) {
            if (listener != null) {
                listener.accept(event);
            }
            return event;
        }

        @Override
        public <T extends io.artificial.enchantments.api.event.EnchantEffectEvent> T dispatch(Class<T> type, java.util.function.Supplier<T> factory) {
            T event = factory.get();
            return dispatch(event);
        }
    }

    private static class MockEventSubscription<T extends io.artificial.enchantments.api.event.EnchantEffectEvent> implements EnchantmentEventBus.EventSubscription<T> {
        private final Class<T> eventType;
        private boolean active = true;

        MockEventSubscription(Class<T> eventType) {
            this.eventType = eventType;
        }

        @Override
        public Class<T> getEventType() {
            return eventType;
        }

        @Override
        public org.bukkit.event.EventPriority getPriority() {
            return org.bukkit.event.EventPriority.NORMAL;
        }

        @Override
        public boolean isIgnoreCancelled() {
            return false;
        }

        @Override
        public void unsubscribe() {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    private static class TestEnchantmentDefinition implements EnchantmentDefinition {
        private final NamespacedKey key;
        private final EnchantmentEffectHandler handler;
        private final int minLevel;
        private final int maxLevel;

        TestEnchantmentDefinition(String name, EnchantmentEffectHandler handler, int minLevel, int maxLevel) {
            this.key = new NamespacedKey("test", name);
            this.handler = handler;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        @Override
        public NamespacedKey getKey() { return key; }

        @Override
        public Component getDisplayName() { return Component.text(key.getKey()); }

        @Override
        public Component getDescription() { return null; }

        @Override
        public int getMinLevel() { return minLevel; }

        @Override
        public int getMaxLevel() { return maxLevel; }

        @Override
        public LevelScaling getScaling() { return LevelScaling.linear(1.0, 0.5); }

        @Override
        public Set<Material> getApplicableMaterials() { return Set.of(Material.DIAMOND_SWORD); }

        @Override
        public boolean isApplicableTo(Material material) { return material == Material.DIAMOND_SWORD; }

        @Override
        public boolean isApplicableTo(org.bukkit.inventory.ItemStack item) { return item.getType() == Material.DIAMOND_SWORD; }

        @Override
        public boolean isCurse() { return false; }

        @Override
        public boolean isTradeable() { return true; }

        @Override
        public boolean isDiscoverable() { return true; }

        @Override
        public Rarity getRarity() { return Rarity.COMMON; }

        @Override
        public EnchantmentEffectHandler getEffectHandler() { return handler; }

        @Override
        public double calculateScaledValue(int level) { return getScaling().calculate(level); }

        @Override
        public boolean conflictsWith(EnchantmentDefinition other) { return false; }

        @Override
        public Set<NamespacedKey> getConflictingEnchantments() { return Set.of(); }
    }
}
