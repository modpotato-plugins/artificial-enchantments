package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Scheduled task that scans all online players' held items and armor,
 * dispatching {@link EffectDispatchSpine.DispatchEventType#HELD_TICK}
 * and {@link EffectDispatchSpine.DispatchEventType#ARMOR_TICK} effects.
 *
 * <p>Runs once per second (every 20 ticks).</p>
 */
public class EnchantmentTickTask implements Runnable {

    private final Plugin plugin;
    private final EffectDispatchSpine spine;
    private final ItemEnchantmentService itemService;
    private BukkitTask task;

    public EnchantmentTickTask(
            @NotNull Plugin plugin,
            @NotNull EffectDispatchSpine spine,
            @NotNull ItemEnchantmentService itemService
    ) {
        this.plugin = plugin;
        this.spine = spine;
        this.itemService = itemService;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkSlot(player, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.HELD_TICK);
            checkSlot(player, EquipmentSlot.OFF_HAND, EffectDispatchSpine.DispatchEventType.HELD_TICK);
            checkSlot(player, EquipmentSlot.HEAD, EffectDispatchSpine.DispatchEventType.ARMOR_TICK);
            checkSlot(player, EquipmentSlot.CHEST, EffectDispatchSpine.DispatchEventType.ARMOR_TICK);
            checkSlot(player, EquipmentSlot.LEGS, EffectDispatchSpine.DispatchEventType.ARMOR_TICK);
            checkSlot(player, EquipmentSlot.FEET, EffectDispatchSpine.DispatchEventType.ARMOR_TICK);
        }
    }

    private void checkSlot(
            @NotNull Player player,
            @NotNull EquipmentSlot slot,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }

        Map<EnchantmentDefinition, Integer> enchantments = itemService.getEnchantments(item);
        for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
            spine.dispatch(entry.getKey(), entry.getValue(), new TickEventPlaceholder(), slot, eventType);
        }
    }

    public void start() {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static class TickEventPlaceholder extends Event {
        private static final HandlerList handlers = new HandlerList();

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }
}
