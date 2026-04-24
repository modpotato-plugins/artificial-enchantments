package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Context for periodic tick-based enchantment effects.
 *
 * <p>Provides access to player, item, slot, and timing data for
 * enchantments that apply effects while an item is held or worn.
 * Called periodically for held items and equipped armor.
 *
 * @since 0.1.0
 */
public interface TickContext extends EffectContext {

    /**
     * Gets the player holding or wearing the item.
     *
     * @return the player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the item being held or worn.
     *
     * @return the item
     */
    @NotNull
    ItemStack getItem();

    /**
     * Gets the equipment slot where the item is located.
     *
     * @return the equipment slot
     */
    @NotNull
    EquipmentSlot getSlot();

    /**
     * Gets the player's current location.
     *
     * @return the location
     */
    @NotNull
    Location getLocation();

    /**
     * Checks if the item is being held in main or off hand.
     *
     * @return true if held
     */
    boolean isHeld();

    /**
     * Checks if the item is equipped as armor.
     *
     * @return true if armor
     */
    boolean isArmor();

    /**
     * Gets the number of ticks this item has been held/worn.
     *
     * @return the tick count
     */
    int getTickCount();

    /**
     * Gets the duration the item has been held in milliseconds.
     *
     * @return the held duration
     */
    long getHeldDuration();
}
