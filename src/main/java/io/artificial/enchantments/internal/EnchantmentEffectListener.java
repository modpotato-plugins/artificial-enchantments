package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bukkit event listener that bridges gameplay events into the {@link EffectDispatchSpine}.
 *
 * <p>For each supported Bukkit event, this listener extracts the relevant item,
 * queries custom enchantments via {@link ItemEnchantmentService}, and dispatches
 * effects through the spine so that typed callbacks and event bus listeners execute.
 */
public final class EnchantmentEffectListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    private final Plugin plugin;
    private final EffectDispatchSpine spine;
    private final ItemEnchantmentService itemService;

    public EnchantmentEffectListener(
            @NotNull Plugin plugin,
            @NotNull EffectDispatchSpine spine,
            @NotNull ItemEnchantmentService itemService
    ) {
        this.plugin = plugin;
        this.spine = spine;
        this.itemService = itemService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.BLOCK_BREAK);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.BLOCK_PLACE);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand() != null ? event.getHand() : EquipmentSlot.HAND;
        dispatchForItem(item, event, hand, EffectDispatchSpine.DispatchEventType.BLOCK_INTERACT);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        dispatchForItem(item, event, event.getHand(), EffectDispatchSpine.DispatchEventType.ENTITY_INTERACT);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        EquipmentSlot slot = EquipmentSlot.HAND;
        if (item.getType().isAir()) {
            item = player.getInventory().getItemInOffHand();
            slot = EquipmentSlot.OFF_HAND;
        }
        dispatchForItem(item, event, slot, EffectDispatchSpine.DispatchEventType.PROJECTILE_LAUNCH);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        EquipmentSlot slot = EquipmentSlot.HAND;
        if (item.getType().isAir()) {
            item = player.getInventory().getItemInOffHand();
            slot = EquipmentSlot.OFF_HAND;
        }
        dispatchForItem(item, event, slot, EffectDispatchSpine.DispatchEventType.PROJECTILE_HIT);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.FISHING_ACTION);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        EquipmentSlot slot = EquipmentSlot.HAND;
        if (bow != null && !bow.equals(player.getInventory().getItemInMainHand())) {
            if (bow.equals(player.getInventory().getItemInOffHand())) {
                slot = EquipmentSlot.OFF_HAND;
            }
        }
        dispatchForItem(bow, event, slot, EffectDispatchSpine.DispatchEventType.BOW_SHOOT);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.ITEM_CONSUME);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        dispatchForItem(item, event, null, EffectDispatchSpine.DispatchEventType.DURABILITY_DAMAGE);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.ITEM_DROP);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        dispatchForItem(item, event, null, EffectDispatchSpine.DispatchEventType.ITEM_PICKUP);
    }

    private void dispatchForItem(
            ItemStack item,
            org.bukkit.event.Event bukkitEvent,
            EquipmentSlot slot,
            EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        try {
            Map<EnchantmentDefinition, Integer> enchantments = itemService.getEnchantments(item);
            for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
                spine.dispatch(entry.getKey(), entry.getValue(), bukkitEvent, slot, eventType);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error dispatching enchantment effects for " + eventType, e);
        }
    }
}
