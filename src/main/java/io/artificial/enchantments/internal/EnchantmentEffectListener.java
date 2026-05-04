package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Creates a new enchantment effect listener.
     *
     * @param plugin      the owning plugin for registration context
     * @param spine       the dispatch spine for firing enchantment effects
     * @param itemService the item enchantment service for querying item enchantments
     * @since 1.0.0
     */
    public EnchantmentEffectListener(
            @NotNull Plugin plugin,
            @NotNull EffectDispatchSpine spine,
            @NotNull ItemEnchantmentService itemService
    ) {
        this.plugin = plugin;
        this.spine = spine;
        this.itemService = itemService;
    }

    /**
     * Handles entity damage by entity events, dispatching enchantment effects
     * for weapons held by the attacking player.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#ENTITY_DAMAGE_BY_ENTITY}
     * context for enchantments on the main hand weapon.
     *
     * @param event the damage event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE_BY_ENTITY);
        }

        if (event.getEntity() instanceof Player victim) {
            dispatchShieldBlock(victim, event);
            dispatchArmor(victim, event, EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE);
        }
    }

    /**
     * Handles general entity damage events for armor and shield enchantments.
     *
     * @param event the damage event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            dispatchShieldBlock(player, event);
            dispatchArmor(player, event, EffectDispatchSpine.DispatchEventType.ENTITY_DAMAGE);
        }
    }

    /**
     * Handles block break events, dispatching enchantment effects for the
     * tool used to break the block.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#BLOCK_BREAK}
     * context for enchantments on the main hand tool.
     *
     * @param event the block break event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.BLOCK_BREAK);
    }

    /**
     * Handles block place events, dispatching enchantment effects for the
     * item used to place the block.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#BLOCK_PLACE}
     * context for enchantments on the main hand item.
     *
     * @param event the block place event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.BLOCK_PLACE);
    }

    /**
     * Handles player interact events with blocks, dispatching enchantment effects
     * for the item in the interacting hand.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#BLOCK_INTERACT}
     * context for enchantments on the interacting hand. Only processes left and right
     * click block actions.
     *
     * @param event the player interact event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand() != null ? event.getHand() : EquipmentSlot.HAND;
        boolean blockInteraction = event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

        if (blockInteraction) {
            dispatchForItem(item, event, hand, EffectDispatchSpine.DispatchEventType.BLOCK_INTERACT);
        }
        dispatchForItem(item, event, hand, EffectDispatchSpine.DispatchEventType.ITEM_USED);
    }

    /**
     * Handles player interact entity events, dispatching enchantment effects
     * for the item in the interacting hand.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#ENTITY_INTERACT}
     * context for enchantments on the hand used to interact with the entity.
     *
     * @param event the player interact entity event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        dispatchForItem(item, event, event.getHand(), EffectDispatchSpine.DispatchEventType.ENTITY_INTERACT);
    }

    /**
     * Handles projectile launch events, dispatching enchantment effects for the
     * item used to launch the projectile (e.g., bow, crossbow, snowball).
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#PROJECTILE_LAUNCH}
     * context for enchantments on the main or off hand item. Only processes projectiles
     * launched by players.
     *
     * @param event the projectile launch event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        if (event.getEntity() instanceof Trident) {
            HeldItem trident = findHeldItem(player, Material.TRIDENT);
            if (trident != null) {
                dispatchForItem(trident.item(), event, trident.slot(), EffectDispatchSpine.DispatchEventType.PROJECTILE_LAUNCH);
                dispatchForItem(trident.item(), event, trident.slot(), EffectDispatchSpine.DispatchEventType.TRIDENT_THROW);
            }
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

    /**
     * Handles projectile hit events, dispatching enchantment effects for the
     * item used to launch the projectile.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#PROJECTILE_HIT}
     * context for enchantments on the main or off hand item. Only processes projectiles
     * shot by players.
     *
     * @param event the projectile hit event
     * @since 1.0.0
     */
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

    /**
     * Handles player fishing events, dispatching enchantment effects for the
     * fishing rod in the main hand.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#FISHING_ACTION}
     * context for enchantments on the fishing rod.
     *
     * @param event the player fish event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.FISHING_ACTION);
    }

    /**
     * Handles entity shoot bow events, dispatching enchantment effects for the
     * bow used to shoot the arrow.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#BOW_SHOOT}
     * context for enchantments on the bow. Detects whether the bow was in the main
     * or off hand and dispatches accordingly. Only processes bows shot by players.
     *
     * @param event the entity shoot bow event
     * @since 1.0.0
     */
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

    /**
     * Handles player item consume events, dispatching enchantment effects for the
     * item being consumed.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#ITEM_CONSUME}
     * context for enchantments on the consumed item.
     *
     * @param event the player item consume event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.ITEM_CONSUME);
    }

    /**
     * Handles player item damage events, dispatching enchantment effects for the
     * item taking durability damage.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#DURABILITY_DAMAGE}
     * context for enchantments on the damaged item.
     *
     * @param event the player item damage event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        dispatchForItem(item, event, null, EffectDispatchSpine.DispatchEventType.DURABILITY_DAMAGE);
    }

    /**
     * Handles player drop item events, dispatching enchantment effects for the
     * item being dropped.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#ITEM_DROP}
     * context for enchantments on the dropped item.
     *
     * @param event the player drop item event
     * @since 1.0.0
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        dispatchForItem(item, event, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.ITEM_DROP);
    }

    /**
     * Handles entity pickup item events, dispatching enchantment effects for the
     * item being picked up.
     *
     * <p>Creates and dispatches a {@link EffectDispatchSpine.DispatchEventType#ITEM_PICKUP}
     * context for enchantments on the picked up item. Only processes pickups by players.
     *
     * @param event the entity pickup item event
     * @since 1.0.0
     */
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

    private void dispatchArmor(
            @NotNull Player player,
            @NotNull org.bukkit.event.Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        PlayerInventory inventory = player.getInventory();
        dispatchForItem(inventory.getHelmet(), bukkitEvent, EquipmentSlot.HEAD, eventType);
        dispatchForItem(inventory.getChestplate(), bukkitEvent, EquipmentSlot.CHEST, eventType);
        dispatchForItem(inventory.getLeggings(), bukkitEvent, EquipmentSlot.LEGS, eventType);
        dispatchForItem(inventory.getBoots(), bukkitEvent, EquipmentSlot.FEET, eventType);
    }

    private void dispatchShieldBlock(@NotNull Player player, @NotNull org.bukkit.event.Event bukkitEvent) {
        if (!player.isBlocking()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        if (mainHand.getType() == Material.SHIELD) {
            dispatchForItem(mainHand, bukkitEvent, EquipmentSlot.HAND, EffectDispatchSpine.DispatchEventType.SHIELD_BLOCK);
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand.getType() == Material.SHIELD) {
            dispatchForItem(offHand, bukkitEvent, EquipmentSlot.OFF_HAND, EffectDispatchSpine.DispatchEventType.SHIELD_BLOCK);
        }
    }

    @Nullable
    private HeldItem findHeldItem(@NotNull Player player, @NotNull Material material) {
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        if (mainHand.getType() == material) {
            return new HeldItem(mainHand, EquipmentSlot.HAND);
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand.getType() == material) {
            return new HeldItem(offHand, EquipmentSlot.OFF_HAND);
        }

        return null;
    }

    private record HeldItem(@NotNull ItemStack item, @NotNull EquipmentSlot slot) {
    }
}
