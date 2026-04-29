package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Scheduled task that scans all online players' held items and armor,
 * dispatching {@link EffectDispatchSpine.DispatchEventType#HELD_TICK}
 * and {@link EffectDispatchSpine.DispatchEventType#ARMOR_TICK} effects.
 *
 * <p>Runs once per second (every 20 ticks).</p>
 */
public class EnchantmentTickTask implements Runnable {

    private final Plugin plugin;
    private final FoliaScheduler scheduler;
    private final EffectDispatchSpine spine;
    private final ItemEnchantmentService itemService;
    private final Map<TickSlotKey, TickState> tickStates = new HashMap<>();
    private FoliaScheduler.ScheduledTask task;

    /**
     * Creates a new tick task for scanning player equipment.
     *
     * @param plugin the plugin instance for scheduling
     * @param scheduler scheduler abstraction used for the repeating scan task
     * @param spine the effect dispatch spine for triggering enchantment effects
     * @param itemService the item service for querying enchantments on items
     * @since 0.1.0
     */
    public EnchantmentTickTask(
            @NotNull Plugin plugin,
            @NotNull FoliaScheduler scheduler,
            @NotNull EffectDispatchSpine spine,
            @NotNull ItemEnchantmentService itemService
    ) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.spine = spine;
        this.itemService = itemService;
    }

    @Override
    public void run() {
        Set<TickSlotKey> activeKeys = new HashSet<>();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            checkSlot(player, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.HELD_TICK, activeKeys);
            checkSlot(player, EquipmentSlot.OFF_HAND, EffectDispatchSpine.DispatchEventType.HELD_TICK, activeKeys);
            checkSlot(player, EquipmentSlot.HEAD, EffectDispatchSpine.DispatchEventType.ARMOR_TICK, activeKeys);
            checkSlot(player, EquipmentSlot.CHEST, EffectDispatchSpine.DispatchEventType.ARMOR_TICK, activeKeys);
            checkSlot(player, EquipmentSlot.LEGS, EffectDispatchSpine.DispatchEventType.ARMOR_TICK, activeKeys);
            checkSlot(player, EquipmentSlot.FEET, EffectDispatchSpine.DispatchEventType.ARMOR_TICK, activeKeys);
        }

        tickStates.keySet().removeIf(key -> !activeKeys.contains(key));
    }

    private void checkSlot(
            @NotNull Player player,
            @NotNull EquipmentSlot slot,
            @NotNull EffectDispatchSpine.DispatchEventType eventType,
            @NotNull Set<TickSlotKey> activeKeys
    ) {
        TickSlotKey key = new TickSlotKey(player.getUniqueId(), slot);
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            tickStates.remove(key);
            return;
        }

        activeKeys.add(key);
        TickState state = nextTickState(key, item);
        TickDispatchEvent tickEvent = new TickDispatchEvent(
                player,
                item,
                slot,
                eventType == EffectDispatchSpine.DispatchEventType.HELD_TICK,
                state.tickCount(),
                state.heldDurationMillis()
        );

        Map<EnchantmentDefinition, Integer> enchantments = itemService.getEnchantments(item);
        for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
            spine.dispatch(entry.getKey(), entry.getValue(), tickEvent, slot, eventType);
        }
    }

    @NotNull
    private TickState nextTickState(@NotNull TickSlotKey key, @NotNull ItemStack item) {
        TickState previous = tickStates.get(key);
        TickState current;

        if (previous == null || !previous.matches(item)) {
            current = new TickState(item.clone(), 20, 1_000L);
        } else {
            current = previous.advance(item);
        }

        tickStates.put(key, current);
        return current;
    }

    /**
     * Starts the tick task, scheduling it to run every 20 ticks (1 second).
     *
     * @since 0.1.0
     */
    public void start() {
        this.task = scheduler.runGlobalTimer(plugin, ignored -> run(), 20L, 20L);
    }

    /**
     * Stops the tick task, cancelling any pending executions.
     *
     * @since 0.1.0
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private record TickSlotKey(@NotNull UUID playerId, @NotNull EquipmentSlot slot) {
    }

    private record TickState(@NotNull ItemStack snapshot, int tickCount, long heldDurationMillis) {
        private boolean matches(@NotNull ItemStack item) {
            return snapshot.isSimilar(item);
        }

        @NotNull
        private TickState advance(@NotNull ItemStack item) {
            return new TickState(item.clone(), tickCount + 20, heldDurationMillis + 1_000L);
        }
    }
}
