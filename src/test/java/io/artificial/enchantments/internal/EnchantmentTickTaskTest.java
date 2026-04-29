package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("EnchantmentTickTask Tests")
class EnchantmentTickTaskTest {

    private static final String TEST_NS = "test";

    private Plugin plugin;
    private Server server;
    private FoliaScheduler scheduler;
    private EffectDispatchSpine spine;
    private ItemEnchantmentService itemService;
    private EnchantmentTickTask task;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        server = mock(Server.class);
        scheduler = mock(FoliaScheduler.class);
        spine = mock(EffectDispatchSpine.class);
        itemService = mock(ItemEnchantmentService.class);

        when(plugin.getServer()).thenReturn(server);
        when(spine.dispatch(any(), anyInt(), any(), any(), any())).thenReturn(true);

        task = new EnchantmentTickTask(plugin, scheduler, spine, itemService);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Creates a mock ItemStack where clone() returns a separate stable snapshot. */
    private ItemStack mockItem(Material material) {
        ItemStack item = mock(ItemStack.class);
        ItemStack clone = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(clone.getType()).thenReturn(material);
        when(item.clone()).thenReturn(clone);
        // Snapshot is similar to the original item (item hasn't changed)
        when(clone.isSimilar(item)).thenReturn(true);
        when(clone.isSimilar(clone)).thenReturn(true);
        return item;
    }

    /** Creates a Player mock whose only non-null slot is HAND. */
    private Player playerWithHandItem(UUID id, ItemStack handItem) {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(handItem);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.HEAD)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.CHEST)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.LEGS)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.FEET)).thenReturn(null);
        return player;
    }

    private EnchantmentDefinition testEnchantment(String name) {
        return new MinimalEnchantmentDefinition(new NamespacedKey(TEST_NS, name));
    }

    // -----------------------------------------------------------------------
    // Enchantment caching tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Enchantments are queried only once when item does not change between ticks")
    void enchantmentsQueriedOnceForUnchangedItem() {
        ItemStack sword = mockItem(Material.DIAMOND_SWORD);
        EnchantmentDefinition enchant = testEnchantment("sharp");
        when(itemService.getEnchantments(sword)).thenReturn(Map.of(enchant, 1));

        Player player = playerWithHandItem(UUID.randomUUID(), sword);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        task.run();
        task.run();
        task.run();

        // getEnchantments should have been called exactly once (first tick); subsequent
        // ticks should reuse the cached result.
        verify(itemService, times(1)).getEnchantments(sword);
    }

    @Test
    @DisplayName("Enchantments are re-queried when item is replaced in the slot")
    void enchantmentsRefreshedWhenItemChanges() {
        UUID playerId = UUID.randomUUID();
        ItemStack sword1 = mockItem(Material.DIAMOND_SWORD);
        ItemStack sword2 = mockItem(Material.IRON_SWORD);

        // The stored snapshot of sword1 should NOT be similar to sword2
        when(sword1.clone().isSimilar(sword2)).thenReturn(false);

        EnchantmentDefinition enchant1 = testEnchantment("sharp1");
        EnchantmentDefinition enchant2 = testEnchantment("sharp2");
        when(itemService.getEnchantments(sword1)).thenReturn(Map.of(enchant1, 1));
        when(itemService.getEnchantments(sword2)).thenReturn(Map.of(enchant2, 2));

        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.HEAD)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.CHEST)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.LEGS)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.FEET)).thenReturn(null);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        // First tick: sword1 in hand
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(sword1);
        task.run();

        // Second tick: sword2 in hand (item changed)
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(sword2);
        task.run();

        verify(itemService, times(1)).getEnchantments(sword1);
        verify(itemService, times(1)).getEnchantments(sword2);
    }

    @Test
    @DisplayName("No dispatch is performed when item has no enchantments")
    void noDispatchForUnenchantedItem() {
        ItemStack plainSword = mockItem(Material.DIAMOND_SWORD);
        when(itemService.getEnchantments(plainSword)).thenReturn(Collections.emptyMap());

        Player player = playerWithHandItem(UUID.randomUUID(), plainSword);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        task.run();

        verifyNoInteractions(spine);
    }

    @Test
    @DisplayName("Dispatch is called for each enchantment on the item")
    void dispatchCalledForEachEnchantment() {
        ItemStack sword = mockItem(Material.DIAMOND_SWORD);
        EnchantmentDefinition e1 = testEnchantment("e1");
        EnchantmentDefinition e2 = testEnchantment("e2");
        when(itemService.getEnchantments(sword)).thenReturn(Map.of(e1, 1, e2, 3));

        Player player = playerWithHandItem(UUID.randomUUID(), sword);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        task.run();

        verify(spine, times(2)).dispatch(any(), anyInt(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // Tick-state advance tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Tick count starts at 20 and advances by 20 each run")
    void tickCountAdvancesEachRun() {
        ItemStack sword = mockItem(Material.DIAMOND_SWORD);
        EnchantmentDefinition enchant = testEnchantment("counter");
        when(itemService.getEnchantments(sword)).thenReturn(Map.of(enchant, 1));

        java.util.concurrent.atomic.AtomicInteger lastTickCount = new java.util.concurrent.atomic.AtomicInteger(-1);
        doAnswer(inv -> {
            TickDispatchEvent event = inv.getArgument(2);
            lastTickCount.set(event.getTickCount());
            return true;
        }).when(spine).dispatch(any(), anyInt(), any(TickDispatchEvent.class), any(), any());

        Player player = playerWithHandItem(UUID.randomUUID(), sword);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        task.run();
        assertEquals(20, lastTickCount.get(), "Initial tickCount should be 20");

        task.run();
        assertEquals(40, lastTickCount.get(), "After second run tickCount should be 40");

        task.run();
        assertEquals(60, lastTickCount.get(), "After third run tickCount should be 60");
    }

    @Test
    @DisplayName("Held duration starts at 1000ms and increases by 1000ms each run")
    void heldDurationAdvancesEachRun() {
        ItemStack sword = mockItem(Material.DIAMOND_SWORD);
        EnchantmentDefinition enchant = testEnchantment("duration");
        when(itemService.getEnchantments(sword)).thenReturn(Map.of(enchant, 1));

        java.util.concurrent.atomic.AtomicLong lastDuration = new java.util.concurrent.atomic.AtomicLong(-1L);
        doAnswer(inv -> {
            TickDispatchEvent event = inv.getArgument(2);
            lastDuration.set(event.getHeldDuration());
            return true;
        }).when(spine).dispatch(any(), anyInt(), any(TickDispatchEvent.class), any(), any());

        Player player = playerWithHandItem(UUID.randomUUID(), sword);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        task.run();
        assertEquals(1_000L, lastDuration.get());

        task.run();
        assertEquals(2_000L, lastDuration.get());
    }

    @Test
    @DisplayName("Tick count resets when item is replaced in the slot")
    void tickCountResetsOnItemChange() {
        UUID playerId = UUID.randomUUID();
        ItemStack sword1 = mockItem(Material.DIAMOND_SWORD);
        ItemStack sword2 = mockItem(Material.IRON_SWORD);
        when(sword1.clone().isSimilar(sword2)).thenReturn(false);

        EnchantmentDefinition enchant = testEnchantment("reset");
        when(itemService.getEnchantments(sword1)).thenReturn(Map.of(enchant, 1));
        when(itemService.getEnchantments(sword2)).thenReturn(Map.of(enchant, 1));

        java.util.concurrent.atomic.AtomicInteger lastTickCount = new java.util.concurrent.atomic.AtomicInteger(-1);
        doAnswer(inv -> {
            TickDispatchEvent event = inv.getArgument(2);
            lastTickCount.set(event.getTickCount());
            return true;
        }).when(spine).dispatch(any(), anyInt(), any(TickDispatchEvent.class), any(), any());

        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.HEAD)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.CHEST)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.LEGS)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.FEET)).thenReturn(null);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        // Two runs with sword1 - tick advances
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(sword1);
        task.run();
        task.run();
        assertEquals(40, lastTickCount.get());

        // Item replaced - tick count resets to 20
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(sword2);
        task.run();
        assertEquals(20, lastTickCount.get(), "Tick count should reset when item changes");
    }

    // -----------------------------------------------------------------------
    // State pruning tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Null item slot removes state and causes no dispatch")
    void nullItemSlotRemovesState() {
        UUID playerId = UUID.randomUUID();
        ItemStack sword = mockItem(Material.DIAMOND_SWORD);
        EnchantmentDefinition enchant = testEnchantment("prune");
        when(itemService.getEnchantments(sword)).thenReturn(Map.of(enchant, 1));

        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.HEAD)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.CHEST)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.LEGS)).thenReturn(null);
        when(inventory.getItem(EquipmentSlot.FEET)).thenReturn(null);
        doReturn(List.of(player)).when(server).getOnlinePlayers();

        // Tick with sword in hand
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(sword);
        task.run();
        verify(spine, atLeastOnce()).dispatch(any(), anyInt(), any(), any(), any());
        clearInvocations(spine);

        // Slot becomes empty
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(null);
        task.run();
        verifyNoInteractions(spine);
    }

    @Test
    @DisplayName("Offline player state is pruned on next run")
    void offlinePlayerStateIsPruned() {
        ItemStack sword = mockItem(Material.DIAMOND_SWORD);
        EnchantmentDefinition enchant = testEnchantment("offline");
        when(itemService.getEnchantments(sword)).thenReturn(Map.of(enchant, 1));

        Player player = playerWithHandItem(UUID.randomUUID(), sword);

        // Player online during first run
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        task.run();
        verify(spine, atLeastOnce()).dispatch(any(), anyInt(), any(), any(), any());
        clearInvocations(spine, itemService);

        // Player goes offline
        doReturn(Collections.emptyList()).when(server).getOnlinePlayers();
        task.run();
        verifyNoInteractions(spine);

        // Player comes back online; state was pruned so enchantments must be re-queried
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        task.run();
        verify(itemService, times(1)).getEnchantments(sword);
    }

    // -----------------------------------------------------------------------
    // start / stop tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("start() registers a global timer with the scheduler")
    void startRegistersGlobalTimer() {
        FoliaScheduler.ScheduledTask mockScheduledTask = mock(FoliaScheduler.ScheduledTask.class);
        when(scheduler.runGlobalTimer(any(), any(), anyLong(), anyLong())).thenReturn(mockScheduledTask);

        task.start();

        verify(scheduler).runGlobalTimer(eq(plugin), any(), eq(20L), eq(20L));
    }

    @Test
    @DisplayName("stop() cancels the scheduled task")
    void stopCancelsScheduledTask() {
        FoliaScheduler.ScheduledTask mockScheduledTask = mock(FoliaScheduler.ScheduledTask.class);
        when(scheduler.runGlobalTimer(any(), any(), anyLong(), anyLong())).thenReturn(mockScheduledTask);

        task.start();
        task.stop();

        verify(mockScheduledTask).cancel();
    }

    @Test
    @DisplayName("stop() is idempotent when called without start()")
    void stopWithoutStartIsIdempotent() {
        assertDoesNotThrow(() -> task.stop());
    }

    // -----------------------------------------------------------------------
    // Inner test helper
    // -----------------------------------------------------------------------

    private static class MinimalEnchantmentDefinition implements EnchantmentDefinition {
        private final NamespacedKey key;

        MinimalEnchantmentDefinition(NamespacedKey key) {
            this.key = key;
        }

        @Override public NamespacedKey getKey() { return key; }
        @Override public Component getDisplayName() { return Component.text(key.getKey()); }
        @Override public Component getDescription() { return null; }
        @Override public int getMinLevel() { return 1; }
        @Override public int getMaxLevel() { return 5; }
        @Override public LevelScaling getScaling() { return LevelScaling.linear(1.0, 0.5); }
        @Override public Set<Material> getApplicableMaterials() { return Set.of(Material.DIAMOND_SWORD); }
        @Override public boolean isApplicableTo(Material material) { return material == Material.DIAMOND_SWORD; }
        @Override public boolean isApplicableTo(ItemStack item) { return item.getType() == Material.DIAMOND_SWORD; }
        @Override public boolean isCurse() { return false; }
        @Override public boolean isTradeable() { return true; }
        @Override public boolean isDiscoverable() { return true; }
        @Override public Rarity getRarity() { return Rarity.COMMON; }
        @Override public EnchantmentEffectHandler getEffectHandler() { return null; }
        @Override public double calculateScaledValue(int level) { return getScaling().calculate(level); }
        @Override public boolean conflictsWith(EnchantmentDefinition other) { return false; }
        @Override public Set<NamespacedKey> getConflictingEnchantments() { return Set.of(); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EnchantmentDefinition that)) return false;
            return key.equals(that.getKey());
        }

        @Override
        public int hashCode() { return key.hashCode(); }
    }
}
